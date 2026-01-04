# Dockerfile for Product Service
# Multi-stage build for optimized image size

# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy parent POM and module POMs first (for dependency caching)
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
COPY admin-dashboard/pom.xml admin-dashboard/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY common-lib/src common-lib/src
COPY product-service/src product-service/src

# Build the application
RUN mvn clean package -pl common-lib,product-service -am -DskipTests

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S cloudshop && adduser -S cloudshop -G cloudshop

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/product-service/target/*.jar app.jar

# Set ownership
RUN chown -R cloudshop:cloudshop /app

# Switch to non-root user
USER cloudshop

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
