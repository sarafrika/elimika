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

# Use existing user with UID 1000 or create elimika user
RUN existing_user=$(getent passwd 1000 | cut -d: -f1) && \
    if [ -n "$existing_user" ]; then \
        echo "Using existing user: $existing_user"; \
        user_name=$existing_user; \
    else \
        groupadd -r elimika && useradd -r -g elimika -u 1000 elimika; \
        user_name=elimika; \
    fi

# Set the working directory
WORKDIR /app

# Create necessary directories and set ownership to UID 1000
RUN mkdir -p /app/storage /app/logs && \
    chown -R 1000:1000 /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership of the JAR file to UID 1000
RUN chown 1000:1000 app.jar

# Switch to non-root user (UID 1000)
USER 1000

# Expose the port the application will run on
EXPOSE 8080

# Metadata labels
LABEL version="0.0.1"
LABEL maintainer="Wilfred Njuguna"

# Run the Spring Boot application with optimized JVM options
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]