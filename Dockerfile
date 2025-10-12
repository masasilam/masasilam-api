# ================================
# 🏗️ STAGE 1: BUILD (Maven + Java 21)
# ================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Salin file pom.xml terlebih dahulu untuk caching dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B -T 2C --no-transfer-progress

# Salin source code dan build
COPY src ./src
RUN mvn clean package -DskipTests -B -T 2C --no-transfer-progress

# ================================
# 🚀 STAGE 2: RUNTIME (Java 21 JRE)
# ================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Salin hasil build dari stage builder
COPY --from=builder /app/target/*.jar app.jar

# Railway akan menetapkan PORT secara dinamis
EXPOSE 8080

# Gunakan environment variable PORT dari Railway (default 8080)
CMD ["sh", "-c", "java -Xms64m -Xmx256m -jar app.jar --server.port=${PORT:-8080}"]
