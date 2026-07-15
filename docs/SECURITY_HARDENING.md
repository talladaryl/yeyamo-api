# Durcissement de sécurité YeYamo

Dernier audit : 15 juillet 2026. Périmètre : les 26 modules actifs du réacteur Maven. Les squelettes `graph-service`, `search-service` et `social-service` sont hors V2, exclus du build et ne doivent pas être déployés.

## Mesures appliquées

| Domaine | Implémentation |
|---|---|
| En-têtes HTTP | HSTS sur HTTPS, CSP restrictive, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, Referrer/Permissions/Cross-Origin policies. CSP adaptée uniquement à Swagger. |
| CORS | Origines exactes en liste blanche, méthodes et en-têtes bornés, credentials configurables, refus des origines inconnues avant le contrôleur. Aucun joker avec credentials. |
| JWT/OAuth2 | Signature obligatoire, durée de vie, `iss` et `aud` obligatoires, dérive d'horloge de 60 s, clés HMAC de 256 bits minimum. Rotation par clé courante + anciennes clés de vérification, ou JWKS asymétrique. |
| Entrées | Bean Validation, rejet des champs JSON inconnus, tailles URI/query/payload, méthodes et types de contenu autorisés, contrôles métier bornés. Les requêtes SQL trouvées sont paramétrées via JPA. |
| XSS/CSRF | API JSON avec CSP et `nosniff`; les valeurs restent à échapper dans les clients. CSRF désactivé pour les API stateless Bearer. Config Server et Eureka utilisent HTTP Basic stateless; l'exception CSRF est limitée à leurs endpoints machine. |
| Traversée de chemin | Détection HTTP des séquences encodées; stockage média confiné dans une racine normalisée, sans liens symboliques; noms de fichiers neutralisés et clés aléatoires. |
| Uploads | Tailles multipart bornées, MIME en liste blanche et signature binaire vérifiée pour médias et documents KYC. FFmpeg reçoit des arguments séparés et des fichiers temporaires générés côté serveur. |
| Désérialisation | Pas de désérialisation Java native ni de polymorphisme Jackson automatique. JSON d'ingestion limité en volume, nombre de lignes/champs, taille des valeurs et profondeur fonctionnelle. |
| SSRF | Ingestion API en HTTPS par défaut, hôtes explicitement autorisés, redirects interdits, timeouts, réponse bornée et adresses privées/locales refusées. |
| Rate limiting | Token bucket local défensif dans chaque service; limite Redis distribuée au gateway, fail-closed, quota auth séparé, adresse cliente hachée. Verrouillage de connexion Redis par identifiant haché. |
| Secrets | Aucun secret applicatif connu par défaut. Variables sensibles obligatoires; chiffrement asymétrique Spring Cloud Config disponible en profil `prod`. |
| Logs | Identifiant de corrélation nettoyé, refus structurés, adresse cliente hachée, aucune valeur de token/mot de passe/payload journalisée. Réponses d'erreur sans stack trace ni valeur rejetée. |
| Supply chain | Réacteur Maven commun, JJWT 0.13.0 et profil OWASP Dependency-Check avec seuil CVSS 7, rapports HTML/JSON/SARIF et workflow CI. |

Le module partagé `security-hardening-starter` applique les contrôles transverses. Il ne remplace pas les autorisations métier de chaque `SecurityFilterChain`.

## Configuration obligatoire

En production, fournir au minimum :

```text
CONFIG_SERVER_USERNAME
CONFIG_SERVER_PASSWORD
EUREKA_USERNAME
EUREKA_PASSWORD
EUREKA_SERVER_URL=https://<user>:<password>@registry.internal:8761/eureka/
JWT_SECRET=<au moins 32 octets aléatoires>
SPRING_DATASOURCE_PASSWORD
PAYMENT_WEBHOOK_SECRET
```

Configurer `CORS_ALLOWED_ORIGINS` avec les seules origines frontales HTTPS. Ne conserver les valeurs localhost que pour le développement. Le proxy de confiance doit supprimer tout `X-Forwarded-Proto` reçu d'Internet avant d'ajouter le sien; sinon définir `TRUST_FORWARDED_PROTO=false`.

### Rotation JWT

1. Générer une nouvelle clé aléatoire d'au moins 32 octets et un nouveau `JWT_KEY_ID`.
2. Déplacer l'ancienne valeur dans `JWT_PREVIOUS_SECRETS` et déployer tous les Resource Servers.
3. Déployer `auth-service` avec la nouvelle `JWT_SECRET`; les nouveaux tokens portent le nouveau `kid`.
4. Après la durée maximale de tous les access tokens plus la marge d'horloge, retirer l'ancienne clé.

Pour une production multi-équipe, préférer une paire asymétrique et `JWT_JWK_SET_URI`; la clé privée reste alors uniquement dans l'émetteur.

### Chiffrement Spring Cloud Config

Activer le profil `prod` du Config Server et fournir :

```text
CONFIG_ENCRYPT_KEYSTORE_LOCATION
CONFIG_ENCRYPT_KEYSTORE_PASSWORD
CONFIG_ENCRYPT_KEYSTORE_ALIAS
CONFIG_ENCRYPT_KEYSTORE_SECRET
```

Chiffrer une valeur via l'endpoint `/encrypt` authentifié, puis stocker uniquement le résultat `{cipher}...` dans le dépôt de configuration. Restreindre `/encrypt` et `/decrypt` au réseau d'administration et sauvegarder le keystore hors du dépôt Git. Les variables d'environnement ou un gestionnaire de secrets restent préférables pour les clés racines.

## Vérification

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
./analytics-service/mvnw.cmd -pl security-hardening-starter test
./analytics-service/mvnw.cmd test
./analytics-service/mvnw.cmd -Psecurity -DskipTests verify
```

Le dernier test nécessite l'accès NVD; définir `NVD_API_KEY` en CI. Une suppression Dependency-Check doit être documentée avec justification et date d'expiration dans `security/dependency-check-suppressions.xml`.

Tests de sécurité couverts : HSTS, CSP/CORS, traversée encodée, origine refusée, validation `iss`/`aud`/expiration et rotation JWT. À ajouter avant production : intégration Redis distribuée, proxy TLS, Kafka ACL/TLS, PostgreSQL TLS, stockage objet, DAST et charge adversariale.

## Correspondance OWASP ASVS 4.x

| Chapitre ASVS | État et preuve principale |
|---|---|
| V1 Architecture | Config centralisée, starter partagé, services stateless et ports/adapters existants. |
| V2 Authentication | BCrypt coût 12, anti-brute-force, OTP borné, JWT strict et rotation. |
| V3 Session | Pas de session HTTP; refresh tokens rotatifs/révocables côté auth. |
| V4 Access Control | Resource Server et rôles par service; contrôles propriétaire/admin métier. |
| V5 Validation | Bean Validation, limites globales, SQL paramétré, ingestion et uploads durcis. |
| V7 Error/Logging | erreurs génériques, pas de données rejetées, logs nettoyés et corrélés. |
| V8 Data Protection | secrets externes/chiffrables; `no-store` sur auth/paiement/admin. TLS infrastructure reste à déployer. |
| V9 Communications | HSTS et hypothèse TLS ingress; certificats à fournir par l'environnement. |
| V10 Malicious Code | Dependency-Check CI et désérialisation native absente. SAST/SBOM/signature restent à intégrer. |
| V11 Business Logic | idempotency keys, Inbox/Outbox, limites de tentatives, webhooks anti-rejeu. |
| V12 Files | MIME + magic bytes + tailles + confinement. Antivirus/CDR reste requis pour les KYC réels. |
| V13 API | JWT, CORS, quotas, pagination/tailles bornées et OpenAPI. |
| V14 Configuration | erreurs masquées, Actuator limité, Config/Eureka authentifiés, secrets sans fallback connu. |

## Risques résiduels avant production

- Déployer Redis, PostgreSQL, Kafka, Eureka et Config Server avec TLS/mTLS et ACL réseau; le code ne peut pas fournir les certificats de l'environnement.
- Remplacer le stockage local par l'adaptateur objet de production, avec chiffrement KMS, antivirus/CDR et URLs signées.
- Le rate limiting local est par instance; le contrôle distribué de référence est celui du gateway. Les appels internes doivent être protégés par réseau/mTLS.
- Exécuter SAST, DAST, SBOM/signature des images et tests de pénétration. Dependency-Check seul ne prouve pas l'absence de vulnérabilités.
- Désactiver ou authentifier Swagger en production selon la politique d'exposition choisie.
