# 🧪 Tests d'Intégration Ads - Guide Manuel

## Prérequis

- Services déployés : discovery-service, config-server, auth-service, campaign-service, ads-delivery-service, feed-service
- Token JWT valide
- Campagnes actives dans campaign-service

---

## Test 1 : Vérification Ads Désactivé (Default Safe)

### Objectif
Confirmer que par défaut, aucune publicité n'est injectée.

### Procédure

```bash
# 1. Vérifier configuration
curl http://config-server:8080/feed-service/default | jq '.propertySources[].source | select(.["yeyamo.ads.enabled"])'

# Expected:
# {
#   "yeyamo.ads.enabled": "false"
# }

# 2. Requête feed
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | .itemType'

# Expected: Tous les items doivent être "ORGANIC"
# Output:
# "ORGANIC"
# "ORGANIC"
# ...

# 3. Vérifier logs
docker logs feed-service | grep "AdInjectionService"

# Expected:
# "AdInjectionService initialized: enabled=false"
# "Ads delivery DISABLED - using NoOp client"
```

### Critères de Succès
- [x] Feature flag `yeyamo.ads.enabled=false`
- [x] Aucun item avec `itemType=SPONSORED`
- [x] Log confirme "enabled=false"
- [x] Aucune erreur dans les logs

---

## Test 2 : Activation Ads avec Service Down

### Objectif
Confirmer que le feed fonctionne même si ads-delivery-service est indisponible (fallback graceful).

### Procédure

```bash
# 1. Activer feature flag
# Modifier cloud-conf-yeyamo/feed-service.properties
yeyamo.ads.enabled=true
yeyamo.ads.feed.enabled=true

# Restart feed-service
docker-compose restart feed-service

# 2. Vérifier activation
docker logs feed-service | grep "AdInjectionService"
# Expected: "AdInjectionService initialized: enabled=true"

# 3. Arrêter ads-delivery-service
docker-compose stop ads-delivery-service

# 4. Requête feed
time curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | .itemType'

# Expected:
# - Tous "ORGANIC"
# - Latency < 1s
# - Pas d'erreur HTTP

# 5. Vérifier logs
docker logs feed-service | grep -A2 "Ads request failed"

# Expected:
# "Ads request failed after XXXms: Connection refused"
# "Circuit breaker: CLOSED -> OPEN (failures=5)"

# 6. Nouvelle requête (circuit breaker OPEN)
time curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Expected: Latency < 200ms (pas d'attente, fallback immédiat)
```

### Critères de Succès
- [x] Feed retourne 200 OK
- [x] Items organiques présents
- [x] Pas d'erreur 500/503
- [x] Log warning "Connection refused"
- [x] Circuit breaker passe en OPEN après 5 échecs
- [x] Latency < 1s même avec service down

---

## Test 3 : Injection Normale (Happy Path)

### Objectif
Confirmer que les publicités sont correctement injectées quand tout fonctionne.

### Procédure

```bash
# 1. S'assurer qu'il y a des campagnes actives
curl http://campaign-service:8080/api/v1/campaigns \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  | jq '.[] | {id, status, startAt, endAt}'

# Expected: Au moins 1 campagne avec status=ACTIVE

# 2. Démarrer ads-delivery-service
docker-compose start ads-delivery-service

# Attendre 35s (circuit breaker timeout)
sleep 35

# 3. Requête feed avec ads
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | {itemType, postId, deliveryId, campaignId}'

# Expected:
# [
#   { "itemType": "ORGANIC", "postId": "abc-123", "deliveryId": null, "campaignId": null },
#   ...
#   { "itemType": "SPONSORED", "postId": null, "deliveryId": "del-456", "campaignId": "camp-789" },
#   ...
# ]

# 4. Compter les types
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items | group_by(.itemType) | map({type: .[0].itemType, count: length})'

# Expected:
# [
#   { "type": "ORGANIC", "count": 18-20 },
#   { "type": "SPONSORED", "count": 0-3 }
# ]

# 5. Vérifier espacement (aucune pub consécutive)
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq -r '.items | to_entries | .[] | "\(.key): \(.value.itemType)"' \
  | grep -A1 "SPONSORED"

# Expected: Jamais deux "SPONSORED" consécutives

# 6. Vérifier logs injection
docker logs feed-service | grep "Injecting"

# Expected:
# "Injecting X sponsored placements into Y organic items"
# "Feed injection complete: Y organic + X sponsored = Z total items"
```

### Critères de Succès
- [x] Response contient à la fois ORGANIC et SPONSORED
- [x] Max 3 ads par page
- [x] Jamais deux ads consécutives
- [x] Chaque SPONSORED a deliveryId et campaignId
- [x] Log "Injecting X sponsored placements"
- [x] Circuit breaker revenu en CLOSED

---

## Test 4 : Pagination & Cohérence

### Objectif
Vérifier que la pagination fonctionne correctement avec ads.

### Procédure

```bash
# 1. Page 0
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | .postId' > page0_posts.txt

# 2. Page 1
curl -X GET "http://feed-service:8080/api/v1/feed?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | .postId' > page1_posts.txt

# 3. Vérifier aucun doublon organique
comm -12 <(sort page0_posts.txt | grep -v null) <(sort page1_posts.txt | grep -v null)

# Expected: Aucune ligne (pas de doublon)

# 4. Vérifier ads différentes
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | select(.itemType=="SPONSORED") | .campaignId' > page0_ads.txt

curl -X GET "http://feed-service:8080/api/v1/feed?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | select(.itemType=="SPONSORED") | .campaignId' > page1_ads.txt

# Note: Peut avoir doublons (limitation connue MVP)
cat page0_ads.txt page1_ads.txt
```

### Critères de Succès
- [x] Aucun doublon de postId organique entre pages
- [x] Chaque page a ~10 items organic + 0-2 ads
- [x] Ads peuvent être répétées (acceptable MVP)

---

## Test 5 : Performance & Timeout

### Objectif
Vérifier que le timeout est respecté et que le fallback est rapide.

### Procédure

```bash
# 1. Mesurer latency normale
for i in {1..10}; do
  time curl -s -o /dev/null -w "%{time_total}\n" \
    -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
    -H "Authorization: Bearer $TOKEN"
done | awk '{sum+=$1; n++} END {print "Average: " sum/n "s"}'

# Expected: < 1s en moyenne

# 2. Simuler slow ads-delivery-service
# (Ajouter artificiellement sleep dans ads-delivery-service ou utiliser proxy)

# 3. Vérifier timeout
curl -w "@curl-format.txt" -o /dev/null -s \
  -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# curl-format.txt:
# time_total: %{time_total}s

# Expected: < 2s (même si ads timeout)

# 4. Vérifier métriques circuit breaker
curl http://feed-service:8080/actuator/prometheus | grep ads_

# Expected:
# ads_requests_total{status="success"} X
# ads_requests_total{status="timeout"} Y
# ads_circuit_breaker_state{state="closed"} 1
```

### Critères de Succès
- [x] Latency P95 < 1s
- [x] Timeout ads n'empêche pas feed
- [x] Métriques Prometheus disponibles

---

## Test 6 : Rétrocompatibilité Client

### Objectif
Confirmer qu'un ancien client mobile peut toujours lire le feed.

### Procédure

```bash
# 1. Ancien format (ignore nouveaux champs)
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | {postId, authorId, caption, likes}'

# Expected: Fonctionne, champs présents pour ORGANIC, null pour SPONSORED

# 2. Parser strict JSON
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | python -m json.tool > /dev/null

# Expected: Pas d'erreur JSON parsing

# 3. Simuler ancien client (ignore itemType)
curl -X GET "http://feed-service:8080/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | del(.itemType, .deliveryId, .campaignId, .trackingToken)'

# Expected: Ancien format fonctionne toujours
```

### Critères de Succès
- [x] JSON valide
- [x] Champs organiques présents pour items ORGANIC
- [x] Ancien client peut ignorer nouveaux champs

---

## Test 7 : Discovery Service

### Objectif
Vérifier que discovery-service fonctionne de manière similaire.

### Procédure

```bash
# 1. Activer ads discovery
# cloud-conf-yeyamo/discovery-service.properties
yeyamo.ads.enabled=true
yeyamo.ads.discovery.enabled=true

docker-compose restart discovery-service

# 2. Requête discovery
curl -X GET "http://discovery-service:8081/api/v1/discovery/search?query=concert&type=EVENT&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.items[] | {itemType, title, deliveryId}'

# Expected: Mix ORGANIC et SPONSORED

# 3. Vérifier logs
docker logs discovery-service | grep "AdInjectionService"

# Expected:
# "AdInjectionService initialized: enabled=true"
# "Injecting X sponsored placements into Y organic items"
```

### Critères de Succès
- [x] Discovery retourne items mixtes
- [x] Même logique que feed-service
- [x] Pas d'erreur

---

## Rapport de Test

### Template

```markdown
# Test Run Report

**Date**: 2026-08-02  
**Environment**: Staging/Production  
**Tester**: [Nom]  

## Results

| Test | Status | Notes |
|------|--------|-------|
| 1. Ads Désactivé | ✅ PASS | Default safe confirmé |
| 2. Service Down | ✅ PASS | Fallback graceful OK |
| 3. Injection Normale | ✅ PASS | 2 ads injectées sur 20 items |
| 4. Pagination | ✅ PASS | Pas de doublons organiques |
| 5. Performance | ✅ PASS | P95=650ms |
| 6. Rétrocompatibilité | ✅ PASS | Ancien client fonctionne |
| 7. Discovery | ✅ PASS | Comportement identique |

## Issues Found

- Aucune issue critique
- Note: Campagnes peuvent se répéter cross-page (limitation connue)

## Recommendation

✅ **APPROVED FOR PRODUCTION**

**Signature**: _____________  
**Date**: __/__/____
```

---

## Métriques à Surveiller Post-Déploiement

```bash
# 1. Grafana Dashboard
# - ads_requests_total (rate)
# - ads_request_duration_seconds (histogram)
# - ads_circuit_breaker_state
# - ads_injected_total

# 2. Logs critiques
docker logs feed-service | grep -E "ERROR|WARN" | grep -i ads

# 3. Business metrics
# - Impressions totales
# - CTR (click-through rate)
# - Revenue par jour

# 4. User feedback
# - App store reviews
# - Support tickets mentioning "ads" or "sponsored"
```

---

## Rollback Checklist

Si problème détecté en production :

1. [ ] Désactiver feature flag immédiatement
   ```bash
   yeyamo.ads.enabled=false
   docker-compose restart feed-service
   ```

2. [ ] Vérifier fallback fonctionne
   ```bash
   curl http://feed-service:8080/api/v1/feed?page=0&size=20
   # Expected: Organic only
   ```

3. [ ] Collecter logs pour analyse
   ```bash
   docker logs feed-service > feed-service-incident.log
   ```

4. [ ] Post-mortem
   - Cause racine
   - Fix
   - Re-test
   - Re-deploy

---

**Document préparé par**: Équipe Backend  
**Dernière mise à jour**: 2026-08-02
