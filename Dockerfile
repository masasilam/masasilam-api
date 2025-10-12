# ===================================
# ⚙️ STAGE 1: BUILD
# ===================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Salin pom.xml dan download dependencies
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B --no-transfer-progress

# Salin source code dan build
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress -T 1C

# ===================================
# 🚀 STAGE 2: RUNTIME
# ===================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Salin jar
COPY --from=builder /app/target/*.jar app.jar

# Railway port
EXPOSE 8080

# Jalankan dengan memory minimal
CMD ["java", "-Xms64m", "-Xmx256m", "-jar", "app.jar"]