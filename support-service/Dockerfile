FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY security-hardening-starter ./security-hardening-starter
RUN mvn -q -f security-hardening-starter/pom.xml -DskipTests install
COPY support-service ./support-service
RUN mvn -q -f support-service/pom.xml -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/support-service/target/*.jar app.jar
RUN addgroup -S spring \
    && adduser -S -u 10001 -G spring spring \
    && chown -R spring:spring /app
USER spring
EXPOSE 8114
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8114}/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
