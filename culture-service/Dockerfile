FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY domain-foundation ./domain-foundation
RUN mvn -q -f domain-foundation/pom.xml -DskipTests install
COPY event-contracts ./event-contracts
RUN mvn -q -f event-contracts/pom.xml -DskipTests install
COPY security-hardening-starter ./security-hardening-starter
RUN mvn -q -f security-hardening-starter/pom.xml -DskipTests install
COPY culture-service ./culture-service
RUN mvn -q -f culture-service/pom.xml -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/culture-service/target/*.jar app.jar
RUN addgroup -S spring && adduser -S -u 10001 -G spring spring && chown -R spring:spring /app
USER spring
EXPOSE 8115
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD wget -qO- http://localhost:${SERVER_PORT:-8115}/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
