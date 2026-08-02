# 🚀 Ads Integration - Guide de Démarrage Rapide

## Configuration Minimale

### 1. Variables d'Environnement

```bash
# Dans docker-compose.yml ou .env

# === GLOBAL ===
ADS_ENABLED=true                           # Master switch

# === FEED SERVICE ===
ADS_FEED_ENABLED=true                      # Activer ads dans feed
ADS_MAX_PER_PAGE=3                         # Max 3 ads par page
ADS_ORGANIC_PER_AD=8                       # 1 ad tous les 8 contenus
ADS_TIMEOUT_MS=500                         # Timeout 500ms
ADS_DELIVERY_SERVICE_URL=http://ads-delivery-service:8080

# === DISCOVERY SERVICE ===
ADS_DISCOVERY_ENABLED=true                 # Activer ads dans discovery
ADS_DISCOVERY_MAX_PER_PAGE=3
ADS_DISCOVERY_ORGANIC_PER_AD=8

# === CIRCUIT BREAKER ===
ADS_CIRCUIT_BREAKER_FAILURE_THRESHOLD=5    # 5 échecs → OPEN
ADS_CIRCUIT_BREAKER_TIMEOUT_MS=30000       # 30s avant retry
```

### 2. Démarrage des Services

```bash
# Ordre recommandé
docker-compose up -d postgres redis kafka
docker-compose up -d discovery-service config-server
docker-compose up -d auth-service
docker-compose up -d campaign-service
docker-compose up -d ads-delivery-service  # ← Requis!
docker-compose up -d feed-service
docker-compose up -d discovery-service
```

### 3. Vérification

```bash
# Vérifier health ads-delivery-service
curl http://localhost:8083/actuator/health

# Tester feed SANS ads (cold start)
curl http://localhost:8080/api/v1/feed?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN"

# Tester feed AVEC ads (si activé)
curl http://localhost:8080/api/v1/feed?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN" | jq '.items[] | .itemType'
# Expected: Mix de "ORGANIC" et "SPONSORED"
```

---

## Rollout Progressif

### Étape 1 : Beta Interne (Staff Only)

```bash
# Activer uniquement pour staff
ADS_ENABLED=true
ADS_FEED_ENABLED=true
ADS_MAX_PER_PAGE=1              # ← Conservateur
ADS_ORGANIC_PER_AD=10           # ← Très espacé
```

**Durée**: 1 semaine  
**Métriques**: Latency, circuit breaker state, user feedback

### Étape 2 : 10% Utilisateurs

```bash
# Même config, augmenter trafic via load balancer
ADS_MAX_PER_PAGE=2
ADS_ORGANIC_PER_AD=8
```

**Durée**: 1 semaine  
**Métriques**: CTR, impressions, revenue

### Étape 3 : 50% Utilisateurs

```bash
ADS_MAX_PER_PAGE=3
ADS_ORGANIC_PER_AD=6            # ← Plus agressif
```

### Étape 4 : 100% + Discovery

```bash
ADS_FEED_ENABLED=true
ADS_DISCOVERY_ENABLED=true      # ← Nouveau
```

---

## Troubleshooting

### Problème : Aucune pub affichée

**Diagnostic**:
```bash
# 1. Vérifier feature flag
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("ads"))'

# 2. Vérifier ads-delivery-service
curl http://localhost:8083/actuator/health

# 3. Vérifier logs
docker logs feed-service | grep -i "ads"
# Expected: "Ads delivery ENABLED"
```

**Solutions**:
- Feature flag désactivé → Activer dans cloud-conf
- Service down → Vérifier ads-delivery-service logs
- Circuit breaker OPEN → Attendre 30s ou restart service

### Problème : Feed lent

**Diagnostic**:
```bash
# Vérifier latency ads
docker logs feed-service | grep "Received.*placements"
# Look for: "Received 2 sponsored placements in XXXms"
```

**Solutions**:
- Si >500ms: Réduire `ADS_TIMEOUT_MS=300`
- Si circuit breaker: Augmenter `ADS_CIRCUIT_BREAKER_FAILURE_THRESHOLD=10`
- Si trop de retry: Vérifier ads-delivery-service performance

### Problème : Pubs consécutives

**Impossible**: Le code garantit `itemsSinceLastAd >= organicPerAd`

Si observé, c'est un bug critique → Ouvrir issue avec:
- Requête exacte
- Réponse complète
- Logs service

---

## Monitoring Dashboard (Grafana)

```promql
# Requests par seconde
rate(ads_requests_total[5m])

# Success rate
rate(ads_requests_total{status="success"}[5m]) 
  / rate(ads_requests_total[5m])

# P95 latency
histogram_quantile(0.95, rate(ads_request_duration_seconds_bucket[5m]))

# Circuit breaker state
ads_circuit_breaker_state{service="feed"}

# Ads injectées
rate(ads_injected_total[5m])
```

---

## FAQ

**Q: Peut-on désactiver ads uniquement pour certains utilisateurs?**  
A: Oui, modifier le code pour vérifier un flag user-level avant `adInjectionService.injectAds()`.

**Q: Comment ajouter plus de 3 ads par page?**  
A: Modifier `ADS_MAX_PER_PAGE=5`, mais attention à l'expérience utilisateur.

**Q: Les ads sont-elles cachées?**  
A: Non! Uniquement le feed organique est caché. Ads = fresh data à chaque requête.

**Q: Que se passe-t-il si ads-delivery-service crash?**  
A: Circuit breaker OPEN après 5 échecs → feed organique uniquement (graceful degradation).

---

## Support

- **Logs**: `docker logs feed-service`
- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`
- **Documentation**: `docs/ADS_INTEGRATION_REPORT.md`
