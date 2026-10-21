#!/bin/bash

echo "========================================"
echo "Building all microservices..."
echo "========================================"

echo ""
echo "Checking for Maven or Maven Wrapper..."

MAVEN_THREADS="${MAVEN_THREADS:-1}"
MAVEN_ARGS=(-B -ntp -T "${MAVEN_THREADS}" -DskipTests install)

# Check if Maven is installed
if command -v mvn &> /dev/null; then
    echo "Maven found in PATH, using mvn command"
    mvn "${MAVEN_ARGS[@]}"
    BUILD_RESULT=$?
elif [ -f "user-service/mvnw" ]; then
    echo "Using Maven Wrapper from user-service"
    user-service/mvnw -f pom.xml "${MAVEN_ARGS[@]}"
    BUILD_RESULT=$?
elif [ -f "auth-service/mvnw" ]; then
    echo "Using Maven Wrapper from auth-service"
    auth-service/mvnw -f pom.xml "${MAVEN_ARGS[@]}"
    BUILD_RESULT=$?
elif [ -f "catalog-service/mvnw" ]; then
    echo "Using Maven Wrapper from catalog-service"
    catalog-service/mvnw -f pom.xml "${MAVEN_ARGS[@]}"
    BUILD_RESULT=$?
else
    echo ""
    echo "ERROR: Neither Maven nor Maven Wrapper found"
    echo ""
    echo "Please either:"
    echo "  1. Install Maven from https://maven.apache.org/download.cgi"
    echo "  2. Or build each service individually"
    echo ""
    exit 1
fi

if [ $BUILD_RESULT -ne 0 ]; then
    echo ""
    echo "ERROR: Build failed"
    echo "Check the error messages above"
    exit 1
fi

echo ""
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo "All JAR files have been created in target/ directories"
echo ""
echo "You can now run: docker compose up -d --build"
