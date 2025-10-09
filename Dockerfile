# Build stage - super optimized dengan Java 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Verify Java version
RUN java -version && mvn -version

# Copy pom.xml dan download dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B -T 2C --no-transfer-progress

# Copy source dan build dengan Java 21
COPY src ./src
RUN mvn package -DskipTests -B -T 2C -o --no-transfer-progress -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

# Runtime stage - minimal
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p storage/covers storage/books storage/authors && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Memory optimized untuk hosting gratis (256MB-512MB RAM)
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]