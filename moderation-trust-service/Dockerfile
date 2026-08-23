FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --chown=spring:spring target/moderation-trust-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8100
EXPOSE 8100
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:8100/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
