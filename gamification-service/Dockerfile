FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/gamification-service-0.0.1-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8097
EXPOSE 8097
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8097/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
