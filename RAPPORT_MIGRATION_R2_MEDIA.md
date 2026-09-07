# Migration Cloudflare R2 — media-service

## Statut

**RÉSOLU.** Le stockage physique des nouveaux médias est désormais Cloudflare R2 via son API S3-compatible. Le contrat mobile `GET /api/v1/media/{id}/content` reste un streaming direct, inchangé.

## Partie A — Audit du stockage existant

### A.1 Mécanisme constaté

`MediaApplicationService` utilisait déjà le port `ObjectStoragePort` :

```java
public interface ObjectStoragePort{
 String store(String key,InputStream content,long length,String contentType);
 StoredObject open(String key);void delete(String key);
 record StoredObject(InputStream content,long length,String contentType){}
}
```

Les clés étaient déjà persistées dans `media_assets.storage_key` (`V1__create_media_schema.sql`) et le chemin local configuré était `media.storage.local.root=${MEDIA_STORAGE_ROOT:./storage/media}` dans `cloud-conf-yeyamo/media-service.properties`.

Aucune implémentation de production de `ObjectStoragePort` n’était versionnée dans le module ; seules des implémentations mémoire de tests existaient. Le nouvel adaptateur local couvre donc les clés historiques non préfixées et rend cette compatibilité explicite.

### A.2 Contrat de lecture actuel

Le contrôleur conserve le streaming de bytes :

```java
@GetMapping("/{id}/content")
public ResponseEntity<InputStreamResource> content(...)
```

et :

```java
return ResponseEntity.ok()
 .contentType(MediaType.parseMediaType(o.contentType()))
 .contentLength(o.length())
 .body(new InputStreamResource(o.content()));
```

**Verdict : aucune redirection 302 n’est introduite.** Le mobile continue d’appeler exactement `GET /api/v1/media/{id}/content` et reçoit des bytes avec leur `Content-Type`.

### A.3 Configuration et dépendances

La présence du `.env` réel a été contrôlée sans lecture de son contenu ni de valeurs secrètes. Les noms suivants sont désormais documentés, vides, dans `.env.example` :

```text
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=
R2_PUBLIC_BASE_URL=
```

La recherche ne trouvait aucun AWS SDK/S3 déjà déclaré. Dépendances ajoutées à `media-service/pom.xml` :

| Dépendance | Version | Justification |
| --- | --- | --- |
| `software.amazon.awssdk:s3` | `2.25.63` | Client S3 synchrone compatible Cloudflare R2. |
| `org.wiremock:wiremock-standalone` (test) | `3.6.0` | Stub HTTP S3 local, sans réseau Cloudflare. |
| `spring-boot-starter-security-oauth2-resource-server-test` (test) | gérée par Spring Boot | Authentification MockMvc des uploads de test. |

## Partie B — Adaptateur R2

### B.1 Configuration — RÉSOLU

`R2StorageProperties` est lié via `@ConfigurationProperties(prefix = "r2")`, activé par `@EnableConfigurationProperties`. En production, l’endpoint est construit exactement depuis le compte :

```java
if(endpoint==null||endpoint.isBlank())
 endpoint="https://"+properties.getAccountId()+".r2.cloudflarestorage.com";
```

`forcePathStyle` est activé :

```java
.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
```

Le fichier de configuration centralisé `cloud-conf-yeyamo/media-service.properties` mappe explicitement `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME` et `R2_PUBLIC_BASE_URL` vers ces propriétés. Aucune valeur réelle n’est stockée dans ce fichier.

`r2.endpoint-override` n’est utilisé que par le test WireMock ; il n’est pas documenté dans `.env.example` et l’endpoint de production reste dérivé de `R2_ACCOUNT_ID`.

### B.2 Routage nouveau / historique — RÉSOLU

Fichiers ajoutés sous `media-service/.../infrastructure/storage/` :

- `R2StorageAdapter.java` : `PutObject`, `GetObject`, `DeleteObject` via AWS SDK v2 ; toute indisponibilité est normalisée en `STORAGE_UNAVAILABLE`.
- `LocalObjectStorageAdapter.java` : lecture/suppression de compatibilité des clés historiques locales, avec contrôle anti-traversée de chemin.
- `RoutedObjectStorageAdapter.java` : implémentation primaire de `ObjectStoragePort`.
- `R2StorageConfiguration.java` et `R2StorageProperties.java` : configuration R2.

Le routeur applique ce contrat :

```java
private static final String R2_PREFIX="r2/";
public String store(String key,InputStream content,long length,String contentType){
 return R2_PREFIX+r2.store(key,content,length,contentType);
}
public StoredObject open(String key){
 return key.startsWith(R2_PREFIX)
  ? r2.open(key.substring(R2_PREFIX.length()))
  : local.open(key);
}
```

Ainsi, tout nouvel upload enregistre une clé `r2/...` et est servi depuis R2. Une clé historique sans ce préfixe reste servie depuis le répertoire local. Aucune ligne existante ni aucun objet existant n’est modifié automatiquement.

`GlobalExceptionHandler` mappe `STORAGE_UNAVAILABLE` vers HTTP `503 Service Unavailable`, avec le format standard `{ code, message, timestamp }`.

### B.3 Contrat mobile — RÉSOLU ET INCHANGÉ

| Élément | Avant | Après |
| --- | --- | --- |
| Route | `GET /api/v1/media/{id}/content` | identique |
| Réponse | flux de bytes + `Content-Type` | identique |
| Redirection | non | non |
| Résolution mobile Phase 5/7 | URL interne média | identique |

**Aucun prompt frontend n’est nécessaire.**

### B.4 Procédure manuelle de migration des anciens fichiers

Une migration automatique n’a volontairement pas été exécutée. Pour chaque ligne `media_assets` dont `storage_key` ne commence pas par `r2/` :

1. sauvegarder le répertoire `MEDIA_STORAGE_ROOT` et la base ;
2. copier le fichier local vers le bucket R2 sous la même clé non préfixée ;
3. contrôler taille et checksum SHA-256 contre `media_assets.size_bytes` et `checksum` ;
4. seulement après validation, mettre à jour la clé en `r2/{ancienne-clé}` ;
5. conserver la copie locale jusqu’à validation fonctionnelle complète ;
6. ne supprimer les fichiers locaux qu’après sauvegarde validée et décision explicite.

Cette procédure est séparée pour éviter toute perte de données lors de ce lot.

## Partie C — Tests sans appel Cloudflare

### Test d’intégration ajouté

Fichier : `media-service/src/test/java/com/yeyamo_mobile/api/media_service/R2StorageIntegrationTest.java`.

Il démarre un `WireMockServer` local, injecte uniquement pour le test son URL sous `r2.endpoint-override`, et couvre :

- `shouldUploadMediaToR2AndReturnId` ;
- `shouldServeMediaContentFromR2` ;
- `shouldRejectUploadOnR2Failure`.

Le test vérifie une réponse upload `201`, un streaming `200 image/png` avec les bytes attendus, et le format standard `503` / `STORAGE_UNAVAILABLE` lors d’un `PutObject` simulé en erreur.

### Fichier de test intégral

```java
package com.yeyamo_mobile.api.media_service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.yeyamo_mobile.api.media_service.application.thumbnail.ImageThumbnailStrategy;

@SpringBootTest
@AutoConfigureMockMvc
class R2StorageIntegrationTest {
 private static final WireMockServer r2=new WireMockServer(wireMockConfig().dynamicPort());
 static {r2.start();}
 @Autowired private MockMvc mockMvc;
 @Autowired private ObjectMapper objectMapper;

 @BeforeAll static void start(){}
 @AfterAll static void stop(){if(r2!=null)r2.stop();}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("r2.endpoint-override",r2::baseUrl);}
 @BeforeEach void reset() throws Exception {
  r2.resetAll();
  r2.stubFor(put(urlPathMatching("/yeyamo-test/thumbnails/.*")).atPriority(1).willReturn(aResponse().withStatus(200).withHeader("ETag","\""+md5(thumbnail())+"\"")));
  r2.stubFor(put(urlPathMatching("/yeyamo-test/.*")).atPriority(2).willReturn(aResponse().withStatus(200).withHeader("ETag","\""+md5(png())+"\"")));
 }

 @Test void shouldUploadMediaToR2AndReturnId() throws Exception {
  String body=mockMvc.perform(upload("r2-upload-user"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty()).andReturn().getResponse().getContentAsString();
  UUID id=UUID.fromString(objectMapper.readTree(body).path("id").asText());
  r2.verify(putRequestedFor(urlPathMatching("/yeyamo-test/.*")));
  org.junit.jupiter.api.Assertions.assertNotNull(id);
 }

 @Test void shouldServeMediaContentFromR2() throws Exception {
  String body=mockMvc.perform(upload("r2-content-user")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  UUID id=UUID.fromString(objectMapper.readTree(body).path("id").asText());
  byte[] bytes=png();
  r2.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/yeyamo-test/.*")).willReturn(aResponse().withStatus(200)
   .withHeader("Content-Type","image/png").withHeader("Content-Length",String.valueOf(bytes.length)).withBody(bytes)));
  mockMvc.perform(get("/api/v1/media/{id}/content",id))
   .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(bytes));
 }

 @Test void shouldRejectUploadOnR2Failure() throws Exception {
  r2.resetAll();
  r2.stubFor(put(urlPathMatching("/yeyamo-test/.*")).willReturn(aResponse().withStatus(503).withBody("R2 unavailable")));
  mockMvc.perform(upload("r2-failure-user"))
   .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
 }

 private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(String owner) throws Exception {
  return multipart("/api/v1/media").file(new MockMultipartFile("file","cover.png","image/png",png())).with(user(owner));
 }
 private static byte[] png() throws Exception {BufferedImage image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return output.toByteArray();}
 private static byte[] thumbnail() throws Exception{return new ImageThumbnailStrategy(480,480).generate(png(),"image/png").bytes();}
 private static String md5(byte[] bytes) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));}
}
```

### Sortie brute — tests module

Commande :

```text
mvn test -pl media-service
```

Fin de sortie brute :

```text
[INFO] Running com.yeyamo_mobile.api.media_service.R2StorageIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  03:23 min
[INFO] Finished at: 2026-09-07T14:07:46+01:00
```

### Sortie brute — compilation reactor

Commande :

```text
mvn compile -DskipTests
```

Fin de sortie brute :

```text
[INFO] Reactor Summary:
[INFO] media-service 0.0.1-SNAPSHOT ....................... SUCCESS [  2.017 s]
[INFO] YeYamo API Reactor 1.0.0-SNAPSHOT .................. SUCCESS [  0.001 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  03:19 min
[INFO] Finished at: 2026-09-07T14:11:41+01:00
```

Le reactor complet contient 41 modules ; tous ont terminé avec succès.

## Contrôles finaux

- Aucun Docker lancé.
- Aucun appel réseau vers Cloudflare R2 effectué.
- Aucun secret, clé R2 ou valeur du `.env` réel n’a été lu ou reporté.
- `git diff --check -- .env.example media-service` : code de sortie `0`.
- Le `.gitignore` de `media-service` a été ajusté de `storage/` vers `/storage/` pour éviter d’ignorer les adaptateurs Java sous `src/.../infrastructure/storage/`.

### Limite d’exécution rencontrée après la dernière modification de configuration

Après le dernier ajout, strictement déclaratif, des cinq mappings `R2_*` dans `cloud-conf-yeyamo/media-service.properties`, une revalidation identique a été demandée. L’environnement Codex l’a refusée avant exécution avec :

```text
Automatic approval review failed: You've hit your usage limit.
```

Cette limite d’environnement n’est ni une erreur Maven ni une erreur de test. Les dernières exécutions complètes vertes documentées ci-dessus portent sur le code R2, les tests WireMock et les 41 modules ; il reste à relancer localement les deux commandes ci-dessous après récupération du quota :

```text
mvn test -pl media-service
mvn compile -DskipTests
```

MIGRATION R2 IMPLÉMENTÉE — CONTRAT MÉDIA INCHANGÉ — REVALIDATION FINALE BLOQUÉE PAR QUOTA D’EXÉCUTION
