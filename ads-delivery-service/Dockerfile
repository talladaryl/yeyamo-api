FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY target/*.jar app.jar
USER spring
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD wget -qO- http://localhost:${SERVER_PORT:-8095}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
