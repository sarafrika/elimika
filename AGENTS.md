# Repository Guidelines

## Project Structure & Module Organization
Elimika is a Spring Boot 3.5 service on Java 21. Feature modules live in `src/main/java/apps/sarafrika/elimika/**` (commerce, availability, notifications, and others). Shared configuration, templates, and static assets stay in `src/main/resources`; Flyway migrations sit in `db/migration`, while profile overrides belong in `application-*.yaml`. Tests mirror the source tree under `src/test/java`. Deployment files reside in `docker/compose.yaml`, and reference notes remain in `docs/`.

## Build, Test, and Development Commands
- `./gradlew clean build`: compile, run unit tests, and assemble the bootable jar.
- `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`: start the API with the dev profile and devtools reload.
- `docker compose -f docker/compose.yaml up`: launch backing services needed for integration work.
- `pnpm release`: trigger semantic-release; reserve for CI pipelines so tags stay consistent.

## Coding Style & Naming Conventions
Use 4-space indentation and follow the existing Lombok patterns. Classes, controllers, and configuration beans use PascalCase, while Spring beans and packages stay lowercase. Keep controllers thin, move domain logic into the relevant module, and keep request and response DTOs alongside feature code. Configuration files should reference secrets via environment variables, never literals.

- When moderating organizations, rely on the consolidated endpoint `POST /api/v1/admin/organizations/{uuid}/moderate?action=approve|reject|revoke` instead of bespoke verify/unverify routes.
- Order dashboard activity feeds from `GET /api/v1/admin/dashboard/activity-feed`; resist duplicating audit lookups in feature code.
- Entity classes must keep `@Column` usage to `@Column(name = "...")` only—nullability, defaults, and constraints belong exclusively in Flyway migrations.

### Flyway Migration Naming
- Always name migration files `Vyyyymmddhhmm__migration_name.sql` using the current UTC timestamp. Run `date -u +"%Y%m%d%H%M"` in the terminal to generate the prefix and avoid manual guessing.
- Treat UTC as the source of truth for all timestamps (migrations, auditing, cross-service messages). Convert external inputs to UTC immediately and only localize at the presentation layer.
- After the version prefix, keep filenames lowercase with underscores (e.g., `__rename_enrollments_to_class_enrollments.sql`) so Flyway ordering remains predictable across filesystems.

## Testing Guidelines
JUnit 5 powers testing through `spring-boot-starter-test`. Name new classes with the `*Test` suffix so Gradle picks them up. Place integration scenarios under `src/test/java/.../integration` and prefer Testcontainers for external dependencies. Run `./gradlew test` before any pull request and document notable new assertions, especially around Flyway migrations and critical business rules. Keep the Spring Modulith architecture tests active and ensure the project stays fully compliant with Spring Modulith role rules; reintroduce other unit tests only once they align with the refined module boundaries.

## Commit & Pull Request Guidelines
Follow Conventional Commits (`feat:`, `fix:`, `chore:`) as seen in `feat: enforce commerce paywall before class enrollment`. Commit messages must be fully descriptive—state the intent and scope without relying on terse phrasing or abbreviations. Keep each commit focused and in the imperative. Pull requests should include a brief summary, linked issue, test evidence (`./gradlew test` output or UI screenshots), and call out configuration or secret changes so reviewers can coordinate updates.

## Environment & Configuration Tips
Store local secrets in `.env` files consumed by Docker Compose and exclude them from version control. When adding a profile, derive it from `application.yaml`, create `application-<profile>.yaml`, and list required variables in `docs/`. Update `docker/compose.yaml` whenever new services or ports are introduced to keep environments reproducible.
