# Multi-stage build using Amazon Corretto 23
FROM amazoncorretto:23-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apk add --no-cache maven

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM amazoncorretto:23-alpine AS production

# Install necessary packages
RUN apk add --no-cache \
    curl \
    bind-tools \
    && rm -rf /var/cache/apk/*

# Create app user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy configuration files
COPY dns-records.txt .
COPY src/main/resources/application.properties .

# Create logs directory
RUN mkdir -p logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports
# 8080 for REST API
# 5353 for DNS server
EXPOSE 8080 5353/udp

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+UseCGroupMemoryLimitForHeap \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="JavaDNS Team" \
      version="1.0.0" \
      description="High-performance DNS server with plugin support" \
      org.opencontainers.image.source="https://github.com/example/javadns" \
      org.opencontainers.image.description="DNS server built with Spring Boot and dnsjava"