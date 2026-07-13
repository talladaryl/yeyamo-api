FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8087
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
 CMD wget -qO- http://localhost:8087/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
