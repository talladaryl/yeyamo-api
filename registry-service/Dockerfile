FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget curl

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring target/*.jar app.jar

USER spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
