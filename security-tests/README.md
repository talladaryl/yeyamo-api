# Tests de securite et de charge

Ces scenarios sont strictement reserves a une pile YeYamo locale ou de test autorisee.
Les scripts refusent toute URL qui ne pointe pas vers `localhost`, `127.0.0.1` ou
`host.docker.internal`. Ils utilisent uniquement des identifiants factices.

## Prerequis

- demarrer la pile YeYamo de test et ses dependances isolees ;
- installer k6 ;
- fournir les secrets et donnees de test par variables d'environnement ;
- verifier que la cible ne contient aucune donnee de production.

## Executions

```powershell
$env:BASE_URL = "http://localhost:8080"
k6 run security-tests/k6/auth-abuse.js
k6 run security-tests/k6/critical-services-load.js
```

`auth-abuse.js` verifie surtout le rejet des identifiants invalides et le `429`.
`critical-services-load.js` effectue une montee progressive puis un pic sur des
routes GET configurees par `READ_PATHS` (liste separee par des virgules). Aucune
mutation metier n'est realisee par defaut. Les resultats doivent etre archives
avec la version du code, la configuration, les ressources machine et les seuils.

Ces scripts n'ont pas ete executes pendant l'audit du 15 juillet 2026 : aucune
pile YeYamo complete n'etait active et k6 n'etait pas installe. Aucun chiffre de
capacite ne doit donc etre deduit de leur presence dans le depot.
