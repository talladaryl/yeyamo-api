FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --chown=spring:spring target/*.jar app.jar
USER spring
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8083/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
