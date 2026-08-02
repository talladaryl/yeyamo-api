# 🔨 Build & Test - Ads Integration

## Compilation

### Feed Service

```bash
cd feed-service
mvn clean compile

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Compiling 38 source files
```

### Discovery Service

```bash
cd discovery-service
mvn clean compile

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Compiling 32 source files
```

### Ads Delivery Service

```bash
cd ads-delivery-service
mvn clean compile

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Compiling 36 source files
```

---

## Tests

### Feed Service (20 tests)

```bash
cd feed-service
mvn test

# Expected output:
# Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

**Tests inclus**:
- ✅ AdInjectionServiceTest (9 tests)
- ✅ FeedQueryServiceTests (1 test)
- ✅ FeedProjectionCommandServiceTests (1 test)
- ✅ FeedDomainTests (2 tests)
- ✅ FeedServiceApplicationTests (1 test)
- ✅ RedisFeedCacheTests (2 tests)
- ✅ FeedEventConsumerTests (2 tests)
- ✅ FeedOutboxTests (1 test)
- ✅ SecurityConfigTests (1 test)

### Ads Delivery Service (15 tests)

```bash
cd ads-delivery-service
mvn test

# Expected output:
# Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

---

## Build Docker Images

### Feed Service

```bash
cd feed-service
mvn clean package -DskipTests
docker build -t yeyamo/feed-service:ads-integration .
```

### Discovery Service

```bash
cd discovery-service
mvn clean package -DskipTests
docker build -t yeyamo/discovery-service:ads-integration .
```

### Ads Delivery Service

```bash
cd ads-delivery-service
mvn clean package -DskipTests
docker build -t yeyamo/ads-delivery-service:latest .
```

---

## Vérification Complète

### Script de Vérification Totale

```bash
#!/bin/bash

echo "=== YEYAMO ADS INTEGRATION - BUILD & TEST ==="
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Fonction de test
test_service() {
    SERVICE=$1
    echo "Testing $SERVICE..."
    cd $SERVICE
    mvn clean test -q
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ $SERVICE tests PASSED${NC}"
        cd ..
        return 0
    else
        echo -e "${RED}✗ $SERVICE tests FAILED${NC}"
        cd ..
        return 1
    fi
}

# Test ads-delivery-service
test_service "ads-delivery-service"
ADS_RESULT=$?

# Test feed-service
test_service "feed-service"
FEED_RESULT=$?

# Test discovery-service (compilation uniquement, pas de tests unitaires ads encore)
echo "Compiling discovery-service..."
cd discovery-service
mvn clean compile -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ discovery-service compilation PASSED${NC}"
    DISCOVERY_RESULT=0
else
    echo -e "${RED}✗ discovery-service compilation FAILED${NC}"
    DISCOVERY_RESULT=1
fi
cd ..

# Résumé
echo ""
echo "=== SUMMARY ==="
echo ""

TOTAL_FAILED=0

if [ $ADS_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ ads-delivery-service: 15/15 tests passed${NC}"
else
    echo -e "${RED}✗ ads-delivery-service: FAILED${NC}"
    ((TOTAL_FAILED++))
fi

if [ $FEED_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ feed-service: 20/20 tests passed${NC}"
else
    echo -e "${RED}✗ feed-service: FAILED${NC}"
    ((TOTAL_FAILED++))
fi

if [ $DISCOVERY_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ discovery-service: Compilation passed${NC}"
else
    echo -e "${RED}✗ discovery-service: FAILED${NC}"
    ((TOTAL_FAILED++))
fi

echo ""
if [ $TOTAL_FAILED -eq 0 ]; then
    echo -e "${GREEN}=== ALL CHECKS PASSED ✓ ===${NC}"
    echo ""
    echo "Ready for deployment!"
    exit 0
else
    echo -e "${RED}=== $TOTAL_FAILED SERVICE(S) FAILED ✗ ===${NC}"
    echo ""
    echo "Please fix the failures before deploying."
    exit 1
fi
```

Sauvegarder comme `verify-ads-integration.sh` et exécuter:

```bash
chmod +x verify-ads-integration.sh
./verify-ads-integration.sh
```

---

## Déploiement Docker Compose

### docker-compose.yml (extrait)

```yaml
services:
  ads-delivery-service:
    image: yeyamo/ads-delivery-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - CONFIG_SERVER_URL=http://config-server:8080
      - EUREKA_SERVER_URL=http://discovery-service:8761/eureka/
    depends_on:
      - postgres
      - redis
      - kafka
    networks:
      - yeyamo-network

  feed-service:
    image: yeyamo/feed-service:ads-integration
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ADS_ENABLED=false                    # ← Safe default
      - ADS_FEED_ENABLED=false
      - ADS_DELIVERY_SERVICE_URL=http://ads-delivery-service:8080
    depends_on:
      - ads-delivery-service
    networks:
      - yeyamo-network

  discovery-service:
    image: yeyamo/discovery-service:ads-integration
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ADS_ENABLED=false                    # ← Safe default
      - ADS_DISCOVERY_ENABLED=false
      - ADS_DELIVERY_SERVICE_URL=http://ads-delivery-service:8080
    depends_on:
      - ads-delivery-service
    networks:
      - yeyamo-network
```

### Commandes de Déploiement

```bash
# 1. Build all services
docker-compose build ads-delivery-service feed-service discovery-service

# 2. Start dependencies first
docker-compose up -d postgres redis kafka config-server eureka-server

# Wait for services to be healthy
sleep 30

# 3. Start ads-delivery-service
docker-compose up -d ads-delivery-service

# Wait for health check
sleep 15

# 4. Start feed and discovery
docker-compose up -d feed-service discovery-service

# 5. Check logs
docker-compose logs -f feed-service | grep "AdInjectionService"
# Expected: "AdInjectionService initialized: enabled=false"

# 6. Health check
curl http://localhost:8080/actuator/health
curl http://localhost:8083/actuator/health
```

---

## Métriques de Build

### Temps de Compilation

| Service | Temps Moyen | Taille JAR |
|---------|-------------|------------|
| ads-delivery-service | ~12s | 85 MB |
| feed-service | ~14s | 92 MB |
| discovery-service | ~15s | 95 MB |

### Temps de Tests

| Service | Tests | Temps |
|---------|-------|-------|
| ads-delivery-service | 15 | ~30s |
| feed-service | 20 | ~45s |
| discovery-service | 0 (compilation only) | ~15s |

**Total Build + Test Time**: ~2 minutes

---

## Troubleshooting Build

### Problème: Tests Échouent

```bash
# Nettoyer Maven cache
mvn clean
rm -rf ~/.m2/repository/com/yeyamo_mobile

# Rebuild
mvn clean install -U
```

### Problème: Dépendances Manquantes

```bash
# Vérifier parent pom
cd yeyamo-api
mvn clean install -DskipTests

# Puis rebuild services
cd feed-service
mvn clean compile
```

### Problème: Docker Build Échoue

```bash
# Vérifier Dockerfile existe
ls feed-service/Dockerfile

# Build avec logs
docker build --no-cache --progress=plain -t yeyamo/feed-service:debug feed-service/
```

---

## CI/CD Pipeline

### GitHub Actions (exemple)

```yaml
name: Ads Integration CI

on:
  push:
    branches: [ main, feature/ads-integration ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build ads-delivery-service
      run: |
        cd ads-delivery-service
        mvn clean test
    
    - name: Build feed-service
      run: |
        cd feed-service
        mvn clean test
    
    - name: Build discovery-service
      run: |
        cd discovery-service
        mvn clean compile
    
    - name: Publish Test Results
      uses: EnricoMi/publish-unit-test-result-action@v2
      if: always()
      with:
        files: |
          **/target/surefire-reports/*.xml
```

---

## Checklist Pré-Déploiement

### Build & Test

- [x] ads-delivery-service compile ✅
- [x] ads-delivery-service tests pass (15/15) ✅
- [x] feed-service compile ✅
- [x] feed-service tests pass (20/20) ✅
- [x] discovery-service compile ✅
- [x] No compilation warnings (acceptables uniquement)

### Docker Images

- [ ] ads-delivery-service image built
- [ ] feed-service image built
- [ ] discovery-service image built
- [ ] Images pushed to registry

### Configuration

- [ ] cloud-conf-yeyamo/feed-service.properties created
- [ ] cloud-conf-yeyamo/discovery-service.properties created
- [ ] Feature flags set to false (safe default)
- [ ] Environment variables documented

### Documentation

- [x] ADS_INTEGRATION_REPORT.md ✅
- [x] ADS_QUICKSTART.md ✅
- [x] ADS_INTEGRATION_TESTING.md ✅
- [x] ADS_INTEGRATION_SUMMARY.md ✅
- [x] BUILD_AND_TEST_ADS.md ✅

---

## Support

En cas de problème lors du build:

1. **Vérifier logs Maven**: `mvn -X test` (debug mode)
2. **Nettoyer**: `mvn clean install -U`
3. **Vérifier JDK**: `java -version` (doit être 21)
4. **Consulter**: [docs/ADS_INTEGRATION_TESTING.md](docs/ADS_INTEGRATION_TESTING.md)

**Contact Équipe Backend**: backend-team@yeyamo.com
