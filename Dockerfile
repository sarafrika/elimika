# Use the official Gradle image with Java 21 for the build stage
FROM gradle:8.5-jdk21 as build

# Set the working directory
WORKDIR /elimika

# Copy the entire project for build context
COPY . .

# Build the application using Gradle and skip tests for faster build
RUN gradle clean build -x test

# Use Eclipse Temurin JDK 21 for the runtime stage
FROM eclipse-temurin:21-jre

# Set the working directory for the application
WORKDIR /elimika

# Copy the built JAR from the build stage and rename it to infraops.jar
COPY --from=build /elimiks/build/libs/*.jar elimika.jar

# Expose the port the application will run on
EXPOSE 8080

# Tagging the image with version 0.0.1
LABEL version="0.0.1"
LABEL authors="Wilfred Njuguna"

# Run the Spring Boot application with the renamed JAR file
ENTRYPOINT ["java", "-jar", "elimika.jar"]
