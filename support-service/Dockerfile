FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY security-hardening-starter ./security-hardening-starter
RUN mvn -q -f security-hardening-starter/pom.xml -DskipTests install
COPY support-service ./support-service
RUN mvn -q -f support-service/pom.xml -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/support-service/target/*.jar app.jar
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring
USER spring
EXPOSE 8114
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8114}/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
