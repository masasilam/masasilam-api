# Gunakan image Java LTS yang stabil untuk production
FROM eclipse-temurin:21-jre-alpine

# Atau pilihan lain:
# FROM openjdk:21-jdk-alpine
# FROM amazoncorretto:21-alpine

# Set working directory
WORKDIR /app

# Copy jar file (pastikan sudah dibuild dengan Maven/Gradle)
COPY target/*.jar app.jar

# Expose port aplikasi Anda
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Tambahkan JVM options untuk production
# ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]