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

# Package the application. This stage deliberately does NOT run the test suite, and must not:
# the suite is Testcontainers-based (build.gradle pulls org.testcontainers:junit-jupiter and
# spring-boot-testcontainers) and needs a Docker daemon, which a `docker build` stage has no
# access to. `bootJar` is used rather than the older `build -x test` so the command states what
# it actually does - package - instead of naming the verifying task and then opting out of the
# verification, which reads like an oversight and invites someone to "fix" it in either direction.
#
# The verification gate lives in .github/workflows/build-push.yml: the "Build and test with Gradle"
# step runs `./gradlew clean build --no-daemon` (which runs `check`/`test`) and must pass before the
# `docker build` step in that same job runs, so no image reaches Docker Hub from an unverified tree.
# Building this Dockerfile by hand bypasses that gate - run `./gradlew build` yourself first.
#
# `bootJar` also leaves exactly one artefact in build/libs. `build` additionally emits
# elimika-<version>-plain.jar, so the COPY below used to glob two files into one destination and
# survived only because "-plain.jar" happens to sort before ".jar" and is therefore overwritten.
RUN ./gradlew clean bootJar --no-daemon

# Use a smaller JRE image for the runtime stage
FROM eclipse-temurin:21-jre

# Pin the container (and therefore the JVM default) time zone to UTC. Elimika
# stores every instant in UTC and relies on this for correct timestamp handling.
ENV TZ=UTC

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

# Run the Spring Boot application.
#
# Heap is expressed as a percentage of the CONTAINER limit, not a fixed -Xmx, so the same image is
# correct on any node — but that only works if the container actually has a limit. Without one the
# JVM sizes itself against the whole host and every container competes for the same pool with the
# kernel OOM killer as the only arbiter. The limit lives in docker/compose.yaml; keep the two together.
#
# 70% rather than the often-quoted 75%: the remainder is not spare, it is metaspace, thread stacks
# (~1MB each), code cache, direct buffers and GC structures, none of which are bounded by heap flags.
# This app carries ~18 Spring Modulith modules, so metaspace runs large — hence the explicit cap,
# which turns a slow unbounded leak into a fast, legible failure.
#
# G1 is selected explicitly, NOT because it is the JDK 21 default. The default only applies on a
# "server class" machine, which requires >=1792MB visible memory — below that the JVM silently picks
# SerialGC, and our 1536m limit is below it. Verified: at --memory=1536m the JVM reports
# UseSerialGC=true, at 1792m it reports UseG1GC=true. Naming G1 makes the collector independent of
# the limit, so lowering the limit later cannot quietly downgrade it.
#
# Deliberately absent: -XX:+UseContainerSupport (default since JDK 10) and -Djava.security.egd
# (a workaround for a blocking /dev/random that modern JDKs on Linux no longer need).
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=UTC", \
  "-jar", "app.jar"]