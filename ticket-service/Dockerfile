FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1001 yeyamo && \
    adduser -D -u 1001 -G yeyamo yeyamo

WORKDIR /app

# Le JAR est construit par le reactor avant le build Docker. Cette image ne
# dépend donc pas d'un cache Maven implicite ni de sources hors du contexte.
COPY --chown=yeyamo:yeyamo target/ticket-service-0.0.1-SNAPSHOT.jar app.jar

USER yeyamo

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8112/actuator/health || exit 1

EXPOSE 8112

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
