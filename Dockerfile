# Stage 1: Build the JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /app/uploads
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx1g", "-Xms512m", "-XX:+UseG1GC", "-jar", "app.jar"]
