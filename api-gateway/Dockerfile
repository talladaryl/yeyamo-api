# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8083
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:8083/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
