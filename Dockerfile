# Use Eclipse Temurin JDK 21 for the build stage
FROM eclipse-temurin:21-jdk AS build

# Set the working directory
WORKDIR /app

# Copy Gradle configuration files and wrapper scripts first
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Make the Gradle wrapper executable
RUN chmod +x gradlew

# Download dependencies to cache them in a separate layer
RUN ./gradlew dependencies --no-daemon

# Copy the entire source code
COPY src ./src

# Build the application and skip tests for a faster build
RUN ./gradlew clean build -x test --no-daemon

# Use a smaller JRE image for the runtime stage
FROM eclipse-temurin:21-jre

# Create a non-root user
RUN groupadd -r elimika && useradd -r -g elimika -u 1000 elimika

# Set the working directory
WORKDIR /app

# Create necessary directories and set ownership
RUN mkdir -p /app/storage /app/logs && \
    chown -R elimika:elimika /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership of the JAR file
RUN chown elimika:elimika app.jar

# Switch to non-root user
USER elimika

# Expose the port the application will run on
EXPOSE 8080

# Metadata labels
LABEL version="0.0.1"
LABEL maintainer="Wilfred Njuguna"

# Run the Spring Boot application with optimized JVM options
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]