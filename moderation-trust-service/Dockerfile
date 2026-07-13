FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/moderation-trust-service-0.0.1-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8100
EXPOSE 8100
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8100/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
