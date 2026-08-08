FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S -G spring -u 10001 spring
WORKDIR /app
COPY --chown=spring:spring target/graph-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=8116
EXPOSE 8116
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD wget -q --spider http://localhost:8116/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
