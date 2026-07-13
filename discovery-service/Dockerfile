FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/discovery-service-0.0.1-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8093
EXPOSE 8093
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8093/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
