# Multi-stage build for YeYamo Ticket Service
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent POM and security-hardening-starter first for better caching
COPY pom.xml ./
COPY security-hardening-starter ./security-hardening-starter

# Copy ticket-service
COPY ticket-service/pom.xml ./ticket-service/
COPY ticket-service/src ./ticket-service/src

# Build the application
WORKDIR /app/ticket-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 yeyamo && \
    adduser -D -u 1001 -G yeyamo yeyamo

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/ticket-service/target/*.jar app.jar

# Change ownership
RUN chown -R yeyamo:yeyamo /app

# Switch to non-root user
USER yeyamo

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8093/actuator/health || exit 1

EXPOSE 8093

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
