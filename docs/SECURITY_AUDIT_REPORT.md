# Rapport d'audit de securite offensif YeYamo

Date : 15 juillet 2026  
Branche : `security/hardening-20260715`  
Referentiels : STRIDE, DREAD (1 a 10), OWASP ASVS 4.x et CWE.

## Perimetre et limites de preuve

L'analyse couvre les 26 modules actifs du reacteur Maven : infrastructure
(`config-server`, `registry-service`, `api-gateway`), identite, utilisateur,
partenaire/admin, catalogue/ingestion/media/content/interaction/moderation,
feed/discovery/notification/recommandation, gamification/missions/parrainage,
booking/payment, event/analytics/place et messaging. Les squelettes
`graph-service`, `search-service` et `social-service`, exclus de l'architecture V2
et du build, ne sont pas des cibles deployables.

L'audit a combine lecture du code et des configurations, tests automatises de
non-regression et analyse des dependances. Aucune pile YeYamo complete n'etait
active : k6 n'etait pas installe et les seuls conteneurs presents appartenaient a
un autre projet; ils n'ont pas ete touches. Par consequent, les campagnes DAST,
Kafka/Redis/Cassandra/OpenSearch et les mesures de charge sont marquees
**Non execute**. Aucun resultat ni chiffre de capacite n'a ete simule.
OWASP Dependency-Check 12.2.2 a ete lance, mais la synchronisation NVD a depasse
la fenetre de 240 secondes et n'a produit aucun rapport; l'etat CVE reste donc
non mesure dans cette session et doit etre traite par la CI avec `NVD_API_KEY`.

## Frontieres de confiance et acteurs

Le flux principal est : client web/mobile -> ingress TLS -> API Gateway ->
Resource Servers -> PostgreSQL/Redis/Kafka/Cassandra/OpenSearch/stockage objet.
Config Server et Eureka forment une frontiere d'administration distincte. Kafka
et l'Outbox franchissent une frontiere asynchrone : les producteurs ne font pas
confiance au consommateur et l'identifiant d'evenement doit rester idempotent.
Les acteurs sont Client, Partenaire, Administrateur, Moderateur, service interne
et fournisseur externe (OAuth, paiement, email/push, ingestion).

Les actifs les plus sensibles sont les secrets et tokens, PII/KYC, moyens et
operations de paiement, disponibilites de reservation, droits admin/moderation,
messages prives, ledger XP/recompenses et preuves de check-in.

## Methode de score

La moyenne DREAD est `(Damage + Reproductibilite + Exploitabilite + Utilisateurs
affectes + Decouvrabilite) / 5`. Toute menace confirmee a 7 ou plus a ete
corrigee. `N/E` signifie que l'absence de cible locale empeche un score
experimental.

## Matrice d'attaques et de menaces

| Attaque / controle | Services cibles | Resultat prouve | DREAD | Statut |
|---|---|---:|---:|---|
| Reutilisation d'un refresh token apres rotation, avec course concurrente | auth | **Vulnerable puis resistant** par test automatise | 7,8 | Corrige (`9940e81`), CWE-294/CWE-362 |
| Token OAuth signe pour une autre audience; email non verifie | auth | **Vulnerable puis resistant** par test automatise | 8,4 | Corrige (`8a31877`, config `03bbcd0`), CWE-287/CWE-345 |
| JWT `alg:none`, algorithme inattendu, secret trop court | gateway et Resource Servers | Resistant par test automatise | 8,0 potentiel | Corrige dans le socle; test renforce pendant cet audit, CWE-345 |
| JWT mauvais `iss`/`aud`, expire ou signe avec ancienne cle autorisee | gateway et Resource Servers | Resistant par test automatise | 7,6 potentiel | Rotation courante + anciennes cles; JWKS possible |
| Brute force login et OTP | auth, gateway | Controle present; attaque live **Non executee** | N/E | Rate limit gateway/service + verrou Redis; DAST a rejouer, CWE-307 |
| Credential stuffing, spraying et enumeration de compte | auth | Analyse statique favorable; campagne **Non executee** | N/E | Script local fourni; alerting SOC a valider |
| Fixation de session | tous | Non applicable : API Bearer stateless sans session HTTP | 0 | Accepte |
| Rejeu d'access token sur paiement/remboursement | payment, booking | JWT seul rejouable; operation protegee par idempotency key/receipt | 5,8 | Accepte; TTL de receipt a superviser |
| IDOR/BOLA/BFLA | admin, partner, payment, booking, messaging, media | Controles role/proprietaire trouves; attaque live **Non executee** | N/E | Tests contractuels multi-acteurs a planifier |
| Injection SQL/JPQL/HQL | services PostgreSQL | Requetes trouvees parametrees; DAST **Non execute** | N/E | Resistant statiquement; SAST/DAST CI a ajouter |
| Injection NoSQL | messaging, analytics, Redis consumers | Pas de requete construite depuis une expression cliente trouvee; live **Non execute** | N/E | A valider avec Cassandra/OpenSearch reels |
| SSRF ingestion | ingestion | Resistant statiquement : HTTPS, allowlist, DNS/IP privees, redirects et taille bornes | 6,4 potentiel | Controle conserve; tests DNS rebinding a planifier |
| XXE/deserialisation dangereuse | tous | Resistant statiquement : pas de parser XML ni Java native/default typing trouves | 6,0 potentiel | Test SAST continu recommande |
| Path traversal | tous, surtout media | Resistant par test filtre; confinement et normalisation du stockage inspectes | 7,2 potentiel | Corrige dans le socle, CWE-22 |
| Upload SVG/polyglot/double extension/ZIP bomb | media/content | MIME, magic bytes, tailles; SVG/ZIP refuses. Corpus live **Non execute** | N/E | Antivirus/CDR objet requis avant production |
| Mass assignment et champs JSON inconnus | API JSON | Bean Validation et rejet global des champs inconnus inspectes | 5,6 potentiel | Resistant statiquement |
| Double reservation | booking | Verrou pessimiste + receipt idempotent inspectes; concurrence live **Non executee** | N/E | Integration PostgreSQL concurrente a ajouter |
| Double paiement/remboursement | payment | Inbox/idempotence, webhook signe et remboursement borne inspectes; live **Non execute** | N/E | Adapter fournisseur reel a certifier |
| Double gain XP / recompense / parrainage | gamification, mission, referral | Ledger/evenement et contraintes uniques inspectes; live **Non execute** | N/E | Concurrence Kafka/PostgreSQL a valider |
| Faux check-in GPS | interaction, gamification | **Vulnerable fonctionnellement** : coordonnees syntaxiques seulement | 6,8 | A planifier : preuve serveur/catalogue et anti-spoofing; ne pas augmenter XP sur position seule |
| Inflation likes/favoris/partages | interaction | Unicite par acteur/cible et commandes idempotentes inspectees | 5,8 | Detection comportementale reste a ajouter |
| Kafka replay/duplication/poison message | consommateurs | Inbox/idempotence et DLT presentes selon services; broker **Non execute** | N/E | ACL/TLS, quotas et tests Testcontainers a planifier |
| Cache poisoning/stampede/exposition Redis | gateway/feed/reco/gamification/referral/auth | Clefs serveur et TTL inspectes; Redis **Non execute** | N/E | TLS/ACL/reseau, verrous distribues et tests a valider |
| HTTP/login/WebSocket flood | gateway/auth/messaging | Tests de charge **Non executes** | N/E | Scenarios locaux fournis; capacite inconnue |

## Vulnerabilites corrigees

1. **Famille de refresh tokens non revoquee apres reutilisation** : verrou
   pessimiste sur le token, rotation transactionnelle et revocation de tous les
   refresh tokens de l'utilisateur a la detection. Preuve :
   `RefreshTokenServiceSecurityTests`. Commit `9940e81`.
2. **Validation OAuth fail-open** : les client IDs Google/Apple sont maintenant
   obligatoires, `aud` est toujours valide, `azp` est requis en presence de
   plusieurs audiences et l'email doit etre verifie. Preuve :
   `OAuthTokenVerifierSecurityTests`. Commits `8a31877` et `03bbcd0` dans le depot
   de configuration.
3. **Regressions JWT et quotas** : tests explicites de `alg:none`, HS512 inattendu,
   secret court, configuration de confiance incomplete et reponse `429` sans
   fuite d'adresse IP. Le socle de correction provient du commit `ad76278` et les
   nouveaux tests du commit `b8237dd`.

Verification finale : les 16 tests de `security-hardening-starter` et
`auth-service` passent sans echec, puis les 26 modules actifs du reacteur Maven
compilent et sont packages avec Java 21 (`-DskipTests package`).

## Authentification et mots de passe

Les mots de passe utilisent BCrypt avec un cout de 12. Ce n'est pas un BCrypt
faible, mais Argon2id avec parametres calibres sur le materiel de production est
la cible recommandee. Une migration transparente peut reencoder le hash lors
d'une connexion reussie, sans invalider les comptes existants. Les OTP ont une
duree et un nombre de tentatives bornes. Le binding IP/User-Agent des refresh
tokens n'est pas impose pour eviter des deconnexions mobiles abusives; la rotation
et la detection de reutilisation constituent le controle principal.

## Tests de charge

| Cible | Capacite maximale | P95/P99 | Point de rupture |
|---|---:|---:|---|
| api-gateway | Non mesuree | Non mesures | Pile absente, k6 absent |
| auth-service | Non mesuree | Non mesures | Pile absente, k6 absent |
| booking-service | Non mesuree | Non mesures | PostgreSQL/Kafka de test absents |
| payment-service | Non mesuree | Non mesures | Fournisseur et dependances de test absents |
| messaging WebSocket | Non mesuree | Non mesures | Cassandra/Kafka/ingress absents |

Les scripts `security-tests/k6` sont verrouilles sur une cible locale. Ils
realiseront une montee progressive jusqu'a 1000 VU et un login flood avec comptes
fictifs. Avant toute conclusion de capacite, collecter saturation Hikari,
latences/lag Kafka, evictions et hit ratio Redis, connexions WebSocket, CPU/memoire,
P95/P99 et taux de 429/5xx. Tester aussi le retour a l'etat nominal apres le pic.

Les priorites de robustesse sont un compose/test environment global jetable,
timeouts explicites, circuit breakers et bulkheads sur chaque adaptateur distant,
backpressure/quotas Kafka, et cache stampede control. Les retries doivent rester
bornes, exponentiels avec jitter et DLT; ils ne doivent jamais envelopper une
operation non idempotente sans cle metier.

## Posture OWASP ASVS et score

Score de posture code/configuration : **76/100**. Detail : architecture et
frontieres 8/10, authentification/session 17/20, controle d'acces 12/15,
validation/fichiers 13/15, donnees/secrets 10/15, journalisation/dependances 8/10,
resilience et validation dynamique 8/15. Cette note mesure les preuves presentes
dans le depot, pas la securite d'une production non observee.

Le mapping ASVS detaille et les parametres d'exploitation sont dans
[`SECURITY_HARDENING.md`](SECURITY_HARDENING.md).

## Risques residuels priorises

1. **P0 avant production** : lancer DAST et charge sur une pile jetable complete;
   la capacite et le comportement sous panne restent inconnus.
2. **P0 infrastructure** : TLS/mTLS, ACL et segmentation pour Kafka, Redis,
   PostgreSQL, Cassandra, OpenSearch, Config/Eureka et stockage objet; secrets
   racines dans Vault/KMS, jamais dans Git.
3. **P1 metier** : faire valider le check-in par une source catalogue serveur et
   un signal de confiance anti-spoofing avant toute attribution XP/recompense.
4. **P1 fichiers** : antivirus/CDR, quarantaine et URLs signees dans l'adaptateur
   objet; tester un corpus malveillant controle.
5. **P1 supply chain** : SBOM CycloneDX, signature d'images, SAST/secret scanning,
   relancer Dependency-Check avec cle NVD et politique de suppression expiree.
6. **P2 detection** : centraliser les evenements d'authentification, alertes de
   spraying/rejeu et traces d'audit immuables; definir les seuils SOC.

## Diagnostic final

Le socle transversal fournit des controles coherents pour les headers, CORS,
JWT, validation, rate limiting et journalisation. Deux failles d'authentification
exploitables et severes ont ete corrigees avec tests de non-regression. Les
transactions metier sensibles possedent globalement idempotence, Inbox/Outbox et
contraintes d'unicite. La posture reste toutefois non certifiable en production
sans infrastructure securisee et sans tests dynamiques bout en bout. La capacite
sous charge est totalement inconnue et aucun chiffre ne doit etre annonce. Le
risque produit le plus visible demeure le check-in GPS insuffisamment atteste.
La prochaine etape doit etre une pile de test globale jetable, instrumentee, puis
l'execution reproductible des campagnes DAST, concurrence, Kafka et k6.
