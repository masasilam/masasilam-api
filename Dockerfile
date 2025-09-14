# Multi-stage build untuk build dan runtime

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory untuk build
WORKDIR /app

# Copy file Maven wrapper dan pom.xml terlebih dahulu (untuk layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (akan di-cache jika pom.xml tidak berubah)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build aplikasi (skip tests untuk deployment yang lebih cepat)
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install tzdata untuk timezone support (opsional)
RUN apk add --no-cache tzdata

# Create non-root user untuk security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy jar dari build stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership ke non-root user
RUN chown appuser:appgroup app.jar

# Switch ke non-root user
USER appuser

# Expose port aplikasi
EXPOSE 8080

# Health check (opsional - untuk monitoring)
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options yang dioptimalkan untuk container
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]