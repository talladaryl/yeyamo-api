# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8094
EXPOSE 8094
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:8094/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
