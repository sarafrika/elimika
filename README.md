# Elimika

## Overview
Elimika is Sarafrika's modular learning platform backend. It orchestrates courses, classes, organizations, and learner experiences while handling authentication, scheduling, and commerce in one Spring Boot 3.5 service targeting Java 21. Publishing a course or class automatically syncs it to the public catalogue and Medusa commerce stack, keeping storefront data consistent without manual API calls.

## Core Capabilities
- Domain-driven modules for courses, class definitions, timetabling, availability, and assessments
- Organization, instructor, and student domains with Keycloak-backed identity and role management
- Commerce workflows that generate paywalls, track purchases, and push catalogue data to Medusa
- Notification and email services for enrollment, invitations, and progress updates
- Flyway-managed PostgreSQL schema migrations and storage helpers for media assets

## Architecture & Stack
The service runs on Spring Boot with Spring Modulith, Jakarta Validation, and JPA/Hibernate. Authentication uses Keycloak 26+, while payments and catalogue operations integrate with Medusa. Persistent data lives in PostgreSQL; Flyway migrations under `src/main/resources/db/migration` execute on startup. Semantic-release (pnpm) automates versioning and changelog updates.

## Prerequisites
- Java 21 runtime (the Gradle wrapper manages builds)
- Docker & Docker Compose for local infrastructure (PostgreSQL, Keycloak, Medusa)
- pnpm 10+ for release automation tasks
- Node 20+/pnpm only required if you plan to run `pnpm release` locally

## Local Development
1. Clone the repository and create a `.env` file with database, Keycloak, encryption, and Medusa credentials. Use `install.md` as a reference template.
2. Provision dependencies with Docker (PostgreSQL + Keycloak + Elimika) or reuse existing infrastructure. The guide in `install.md` includes ready-to-run `docker-compose` instructions.
3. Generate the external Docker network used by the stack if it does not already exist: `docker network create sarafrika`.
4. Start the backing services: `docker compose -f docker/compose.yaml up -d` (or the extended compose file from `install.md`).
5. Launch the API locally: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`. Override ports and secrets through environment variables as needed.

## Running Tests & Tooling
- `./gradlew test` runs the JUnit 5 suite.
- `./gradlew clean build` compiles, tests, and packages the application jar.
- `./gradlew flywayMigrate` (optional) applies migrations to the configured datasource.
- `pnpm release` triggers semantic-release; keep this for CI pipelines to avoid double-tagging.

## Deployment Notes
The production container image is published as `sarafrika/elimika`. The provided `docker/compose.yaml` mounts persistent volumes for storage and logs, exposes port `30000`, and expects configuration via `.env`. Health checks rely on the Spring Boot actuator endpoint.

## Project Structure
```
src/main/java/apps/sarafrika/elimika/    # Domain modules (course, commerce, classes, tenancy, etc.)
src/main/resources/                      # Spring configuration, templates, Flyway migrations
src/test/java/                           # Mirrored test packages
docker/                                   # Deployment compose file
docs/                                     # Domain and feature guides
AGENTS.md                                 # Contributor and workflow guidelines
```

## Further Reading
- `AGENTS.md` for contributor guidelines and workflow expectations
- `install.md` for the full Docker-based setup walkthrough
- `docs/guides/` for deep dives into each domain and feature area
