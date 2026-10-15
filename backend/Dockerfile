# ==============================================================================
# Multi-Stage Dockerfile for Spring Boot Backend
# ==============================================================================

# Stage 1: Build the Application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create unprivileged application user for security
RUN groupadd -r eventgroup && useradd -r -g eventgroup eventuser

# Copy built artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R eventuser:eventgroup /app

USER eventuser

# Expose backend port
EXPOSE 8085

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8085/api/events || exit 1

# Launch JVM with container-aware memory limits
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
