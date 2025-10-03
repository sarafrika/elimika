# elimika Project Overview

This project, "elimika," is a Spring Boot application built with Gradle and Java 21. It leverages Spring Modulith for modularity, Spring Data JPA for data persistence, and Flyway for database migrations. Keycloak is integrated for authentication, and PostgreSQL is used as the database. The application is containerized using Docker and orchestrated with Docker Compose. It also uses `semantic-release` for automated versioning and releases.

## Building and Running

### Building the Application

The application can be built using Gradle. From the project root, execute:

```bash
./gradlew clean build -x test
```

This command will compile the Java code, run tests (unless `-x test` is used), and package the application into a JAR file located in `build/libs`.

### Running with Docker Compose

The application can be run as a Docker service using `docker-compose`. Ensure you have Docker and Docker Compose installed.

1.  **Environment Variables:** Create a `.env` file in the `docker/` directory, based on `docker/.env.sample`, and fill in the necessary environment variables.
2.  **Start Services:** From the project root, navigate to the `docker` directory and run:

    ```bash
    docker compose up -d
    ```

    This will build the Docker image (if not already built) and start the `elimika` service, along with any other services defined in `docker/compose.yaml`.

The application will be accessible on port `30000` (as configured in `docker/compose.yaml`).

## Development Conventions

*   **Language:** Java 21
*   **Build Tool:** Gradle
*   **Frameworks:** Spring Boot, Spring Modulith
*   **Database:** PostgreSQL (managed with Flyway for schema migrations)
*   **Authentication:** Keycloak
*   **Containerization:** Docker, Docker Compose
*   **Release Management:** `semantic-release` is used for automated versioning and releases based on commit messages.
