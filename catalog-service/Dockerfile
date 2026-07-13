FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/catalog-service-0.0.1-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8088
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8088/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
