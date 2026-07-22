FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --chown=spring:spring target/analytics-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8097
EXPOSE 8097
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:8097/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
