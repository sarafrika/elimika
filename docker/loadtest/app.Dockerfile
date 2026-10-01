# Load-test app image from a jar built on the host (scripts/loadtest/stack-up.sh runs ./gradlew bootJar first).
#
# Why not ./Dockerfile directly: its build stage re-downloads Gradle and every dependency whenever build.gradle
# changes, which on a slow link takes far longer than a host build that reuses ~/.gradle. The RUNTIME stage
# below is a copy of ./Dockerfile's (same base image, user, JVM flags), so the container behaves the same.
# KEEP THE ENTRYPOINT IN SYNC WITH ./Dockerfile. To use ./Dockerfile verbatim instead:
#   LOADTEST_APP_DOCKERFILE=Dockerfile scripts/loadtest/stack-up.sh
FROM eclipse-temurin:21-jre

ENV TZ=UTC

RUN existing_user=$(getent passwd 1000 | cut -d: -f1) && \
    if [ -z "$existing_user" ]; then \
        groupadd -r elimika && useradd -r -g elimika -u 1000 elimika; \
    fi

WORKDIR /app
RUN mkdir -p /app/storage /app/logs && chown -R 1000:1000 /app

ARG JAR=build/libs/elimika-0.0.1.jar
COPY --chown=1000:1000 ${JAR} app.jar

USER 1000
EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=UTC", \
  "-jar", "app.jar"]
