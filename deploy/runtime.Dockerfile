# syntax=docker/dockerfile:1.7

ARG SERVICE_MODULE
ARG SERVICE_PORT=8080

FROM eclipse-temurin:21-jre-jammy
ARG SERVICE_MODULE
ARG SERVICE_PORT

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring

WORKDIR /app
COPY --chown=spring:spring target/${SERVICE_MODULE}-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=${SERVICE_PORT}
EXPOSE ${SERVICE_PORT}
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 CMD-SHELL curl --fail --silent "http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness" >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:ActiveProcessorCount=1", "-jar", "app.jar"]
