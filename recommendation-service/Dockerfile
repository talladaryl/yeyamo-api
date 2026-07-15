FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --chown=appuser:appuser target/recommendation-service-0.0.1-SNAPSHOT.jar app.jar
USER appuser
ENV SERVER_PORT=8095
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:8095/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
