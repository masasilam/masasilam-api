# ===================================
# ⚙️ STAGE 1: DEPENDENCY CACHE
# ===================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS deps
WORKDIR /app

# COPY HANYA pom.xml untuk caching dependency layer
COPY pom.xml .

# Download dependencies dengan optimasi
RUN mvn dependency:go-offline -B --no-transfer-progress || true

# ===================================
# ⚙️ STAGE 2: BUILD
# ===================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy dari stage deps (reuse cache)
COPY --from=deps /root/.m2 /root/.m2
COPY pom.xml .

# Copy source dan build
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress -T 1C

# ===================================
# 🚀 STAGE 3: RUNTIME (MINIMAL)
# ===================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install only required runtime dependencies
RUN apk add --no-cache tzdata

# Copy HANYA jar file
COPY --from=builder /app/target/*.jar app.jar

# Railway port
EXPOSE 8080

# Health check (optional tapi bagus)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run dengan memory optimal untuk Railway
ENTRYPOINT ["java", \
    "-Xms128m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]