# ===================================
# ⚙️ STAGE 1: BUILD
# ===================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Salin hanya file pom.xml dulu (untuk caching dependencies)
COPY pom.xml .

# Gunakan cache Maven dengan ID unik sesuai format Railway
RUN --mount=type=cache,id=railway-maven-cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress

# Salin source code dan build jar
COPY src ./src
RUN --mount=type=cache,id=railway-maven-cache,target=/root/.m2 \
    mvn clean package -DskipTests -B --no-transfer-progress

# ===================================
# 🚀 STAGE 2: RUNTIME
# ===================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Salin hasil build ke runtime container
COPY --from=builder /app/target/*.jar app.jar

# Railway menggunakan port dinamis
EXPOSE 8080

# Jalankan aplikasi Spring Boot
CMD ["sh", "-c", "java -Xms64m -Xmx256m -jar app.jar --server.port=${PORT:-8080}"]
