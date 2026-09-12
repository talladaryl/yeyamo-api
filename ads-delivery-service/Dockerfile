# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl ads-delivery-service -am -DskipTests package
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/ads-delivery-service/target/ads-delivery-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8111
EXPOSE 8111
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:8111/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
