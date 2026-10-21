# syntax=docker/dockerfile:1.7

ARG SERVICE_MODULE

FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG SERVICE_MODULE
WORKDIR /workspace

# Keep dependency resolution independent from application source changes. The
# explicit copies preserve the reactor layout needed by -am.
COPY pom.xml ./
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY event-contracts/pom.xml event-contracts/pom.xml
COPY domain-foundation/pom.xml domain-foundation/pom.xml
COPY security-hardening-starter/pom.xml security-hardening-starter/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY registry-service/pom.xml registry-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY partner-service/pom.xml partner-service/pom.xml
COPY campaign-service/pom.xml campaign-service/pom.xml
COPY ads-delivery-service/pom.xml ads-delivery-service/pom.xml
COPY admin-service/pom.xml admin-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY culture-service/pom.xml culture-service/pom.xml
COPY country-config-service/pom.xml country-config-service/pom.xml
COPY graph-service/pom.xml graph-service/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY media-service/pom.xml media-service/pom.xml
COPY content-service/pom.xml content-service/pom.xml
COPY interaction-service/pom.xml interaction-service/pom.xml
COPY moderation-trust-service/pom.xml moderation-trust-service/pom.xml
COPY feed-service/pom.xml feed-service/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY recommendation-service/pom.xml recommendation-service/pom.xml
COPY gamification-service/pom.xml gamification-service/pom.xml
COPY mission-reward-service/pom.xml mission-reward-service/pom.xml
COPY referral-service/pom.xml referral-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY commerce-service/pom.xml commerce-service/pom.xml
COPY ticket-service/pom.xml ticket-service/pom.xml
COPY event-service/pom.xml event-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY place-service/pom.xml place-service/pom.xml
COPY messaging-service/pom.xml messaging-service/pom.xml
COPY support-service/pom.xml support-service/pom.xml
COPY search-service/pom.xml search-service/pom.xml
COPY social-service/pom.xml social-service/pom.xml

RUN --mount=type=cache,id=yeyamo-maven-repository,sharing=locked,target=/root/.m2 \
    mvn -B -ntp -pl "${SERVICE_MODULE}" -am -DskipTests dependency:go-offline

COPY . .
RUN --mount=type=cache,id=yeyamo-maven-repository,sharing=locked,target=/root/.m2 \
    mvn -B -ntp -pl "${SERVICE_MODULE}" -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
ARG SERVICE_MODULE
ARG SERVICE_PORT=8080

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring

WORKDIR /app
COPY --from=build --chown=spring:spring "/workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.0.1-SNAPSHOT.jar" app.jar
USER spring
ENV SERVER_PORT=${SERVICE_PORT}
EXPOSE ${SERVICE_PORT}
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 CMD-SHELL curl --fail --silent "http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness" >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:ActiveProcessorCount=1", "-jar", "app.jar"]
