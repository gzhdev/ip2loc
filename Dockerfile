# Multi-stage build for optimal image size
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

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

# Runtime stage with minimal JRE
FROM eclipse-temurin:17-jre-alpine

# Add metadata
LABEL maintainer="your-email@example.com"
LABEL description="IP2Location Geolocation Service"
LABEL version="0.0.1-SNAPSHOT"

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

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

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
