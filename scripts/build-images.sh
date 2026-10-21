#!/usr/bin/env bash
set -euo pipefail

maven_threads="${MAVEN_THREADS:-1}"
image_prefix="${DOCKER_IMAGE_PREFIX:-yeyamo}"

services=(
  "config-server:8080"
  "registry-service:8761"
  "api-gateway:8083"
  "auth-service:8082"
  "user-service:8086"
  "partner-service:8087"
  "campaign-service:8110"
  "ads-delivery-service:8111"
  "ticket-service:8112"
  "commerce-service:8113"
  "admin-service:8096"
  "catalog-service:8088"
  "place-service:8084"
  "content-service:8090"
  "interaction-service:8091"
  "feed-service:8092"
  "media-service:8101"
  "moderation-trust-service:8100"
  "event-service:8085"
  "booking-service:8102"
  "payment-service:8103"
  "messaging-service:8104"
  "notification-service:8094"
  "support-service:8114"
  "culture-service:8115"
  "gamification-service:8105"
  "mission-reward-service:8098"
  "recommendation-service:8095"
  "referral-service:8099"
  "analytics-service:8097"
  "ingestion-service:8089"
  "discovery-service:8093"
  "graph-service:8116"
)

if command -v mvn >/dev/null 2>&1; then
  mvn -B -ntp -T "$maven_threads" install -DskipTests
elif [[ -x auth-service/mvnw ]]; then
  auth-service/mvnw -f pom.xml -B -ntp -T "$maven_threads" install -DskipTests
else
  echo "Maven or auth-service/mvnw is required" >&2
  exit 1
fi

for service_spec in "${services[@]}"; do
  service="${service_spec%%:*}"
  port="${service_spec##*:}"
  docker build \
    --file deploy/runtime.Dockerfile \
    --build-arg "SERVICE_MODULE=$service" \
    --build-arg "SERVICE_PORT=$port" \
    --tag "$image_prefix/$service:local" \
    "$service"
done
