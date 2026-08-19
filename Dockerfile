# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Grant execution rights on the Maven wrapper
RUN chmod +x mvnw

# Resolve dependencies (caches them in Docker layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and package the application (skipping test execution during build)
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Create the runtime container
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Define default environment variables
ENV PORT=8081
ENV MONGODB_URI=mongodb://localhost:27017/job_ingestion

# Expose the application port
EXPOSE 8081

# Run the application, ensuring shell expands environment variables properly
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT} --spring.mongodb.uri=${MONGODB_URI}"]
