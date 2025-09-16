# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Elimika is a Spring Boot 3.5.5 application built with Java 21 that provides an educational platform for learning management. It uses Spring Security with OAuth2 + JWT, PostgreSQL database, and Keycloak for authentication. The platform supports multi-tenancy with organizations and different user roles (Student, Instructor, Admin).

## Core Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.5 with Java 21
- **Security**: Spring Security + OAuth2 Resource Server with JWT
- **Database**: PostgreSQL with Flyway migrations
- **Authentication**: Keycloak 26.0.6 for identity management
- **ORM**: Spring Data JPA with Hibernate
- **Build Tool**: Gradle with Gradle Wrapper
- **Containerization**: Docker support
- **Documentation**: SpringDoc OpenAPI

### Package Structure
The main codebase follows Spring Boot conventions under `src/main/java/apps/sarafrika/elimika/`:

- `authentication/` - Keycloak integration and authentication services
- `course/` - Course management, lessons, assessments, certificates
- `instructor/` - Instructor profiles, experience, education, skills
- `student/` - Student management and enrollment
- `shared/` - Common DTOs, utilities, storage, and cross-cutting concerns
- `tenancy/` - Multi-tenant organization and user domain management

Each module typically contains:
- `model/` - JPA entities
- `dto/` - Data Transfer Objects
- `repository/` - Spring Data JPA repositories
- `service/` - Business logic services
- `factory/` - Entity factories and builders
- `config/` - Module-specific configuration

## Development Commands

### Build and Run
```bash
# Build the application
./gradlew build

# Run the application (development mode)
./gradlew bootRun

# Run tests
./gradlew test

# Clean and rebuild
./gradlew clean build
```

### Database
- Database migrations are located in `src/main/resources/db/migration/`
- Migration files follow Flyway naming convention: `V{timestamp}__{description}.sql`
- The application uses PostgreSQL and automatically runs migrations on startup

### Docker Development
- Use the setup instructions in `install.md` for local Docker environment
- The application expects PostgreSQL and Keycloak to be running
- Default development database: `jdbc:postgresql://localhost:5432/elimika`

## Configuration Profiles

The application supports multiple profiles:
- `dev` - Development profile (defined in `application-dev.yaml`)
- `staging` - Staging environment (defined in `application-staging.yaml`)
- Default profile configuration in `application.yaml`

Key configuration areas:
- Database connection via `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`
- Keycloak integration via `app.keycloak.*` properties
- JWT configuration via `spring.security.oauth2.resourceserver.jwt.*`
- Encryption keys for sensitive data

## Key Design Patterns

### Spring Modulith Architecture
The application uses Spring Modulith for modular monolith architecture with:
- Event publication for cross-module communication
- Module boundaries enforced at package level
- Integration testing support for modules

### Multi-tenancy
- Organizations represent tenants in the system
- User-domain mapping for role-based access control
- Domain-specific repositories and services

### Security Model
- JWT-based authentication via Keycloak
- Role-based authorization with multiple domains (Student, Instructor, Admin)
- Resource server configuration for OAuth2

### Data Patterns
- Factory pattern for entity creation
- Repository pattern with Spring Data JPA
- DTO pattern for API responses
- Specification pattern for dynamic queries (`SpecificationHelper`)

## Testing

- Uses JUnit 5 and Spring Boot Test framework
- Spring Modulith test support for integration testing
- Currently no test directory exists - tests should be created in `src/test/java/`

## API Documentation

- SpringDoc OpenAPI integration for automatic API documentation
- Swagger UI available at `/swagger-ui.html` when running
- API follows RESTful conventions with proper HTTP status codes

## Domain-Specific Notes

### Course Management
- Complex hierarchy: Programs → Courses → Lessons → Content
- Support for quizzes, assignments, and assessments
- Progress tracking and certificate generation
- Rubric-based grading system

### User Management
- Three main user types: Student, Instructor, Admin
- User-domain mappings for multi-role support
- Keycloak integration for identity management

### File Storage
- File upload support with configurable limits (100MB max)
- Storage service abstraction in `shared/storage/`
- Support for various content types (documents, images, videos)

## Release Process

- Semantic versioning with semantic-release
- Automated changelog generation
- Release via GitHub Actions
- Uses conventional commits for automatic version bumping
- All claude related files for its use should be added to git ignore
- Always use git conventions for commits and checkouts
- The author of most of code is Wilfred Njuguna
- Never mention of coauthors or author when doing commits
- All dtos should have the @JsonPropery that defines the json fields in snake case