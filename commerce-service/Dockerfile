FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY target/commerce-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD wget -qO- http://localhost:${SERVER_PORT:-8090}/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
