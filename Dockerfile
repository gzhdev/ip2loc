# Multi-stage build for optimal image size
# Changed base image to non-alpine variant for multi-arch (amd64/arm64) support
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds, tests run in CI)
RUN mvn clean package -DskipTests -B

# Runtime stage with minimal JRE (non-alpine for multi-arch)
FROM eclipse-temurin:17-jre

# Add metadata
LABEL maintainer="your-email@example.com"
LABEL description="IP2Location Geolocation Service"
LABEL version="0.0.1-SNAPSHOT"

# Install curl for health checks (Ubuntu/Debian base)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser

# Set working directory
WORKDIR /app

# Create data directory for IP2Location database
RUN mkdir -p /app/data && \
    chown -R appuser:appuser /app

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose default Spring Boot port
EXPOSE 8080

# Health check using curl (installed)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 CMD curl -fs http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
