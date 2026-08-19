# Stage 1: Build the application using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and src directly without relying on Maven wrapper
COPY pom.xml ./
COPY src ./src

# Build the package skipping tests
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime environment
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default port
EXPOSE 8081

# Set default env values (PORT defaults to 8081; MONGODB_URI for database overrides)
ENV PORT=8081
ENV MONGODB_URI=mongodb://localhost:27017/job_ingestion

# Run the Spring Boot application, mapping server port and mongo uri dynamically
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT} --spring.mongodb.uri=${MONGODB_URI}"]
