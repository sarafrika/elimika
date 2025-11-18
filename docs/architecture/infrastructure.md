# Spring Modulith Infrastructure Overview

This doc captures the module boundaries and dependency rules enforced by Spring Modulith in Elimika. The `ModulithArchitectureTest` in `src/test/java/apps/sarafrika/elimika/ModulithArchitectureTest.java` must stay green; add new dependencies by updating the relevant `package-info.java` file with a named interface and an explicit `allowedDependencies` entry.

## Verification
- Run `./gradlew test` to execute the architecture check alongside the rest of the suite.
- Every cross-module call should target a named interface (SPI package) to keep the graph acyclic; avoid reaching into `internal` packages.

## Module Map
| Module | Named interface(s) | Allowed dependencies (enforced) | Notes |
| --- | --- | --- | --- |
| `course` | `course-spi` | `shared`, `coursecreator :: coursecreator-spi`, `tenancy :: tenancy-spi` | Provides course info via `CourseInfoService`; consumes tenancy/user lookups through the `tenancy-spi`. |
| `tenancy` | `tenancy-spi` | `shared`, `authentication :: keycloak-integration`, `notifications :: preferences-spi`, `notifications :: events-api`, `notifications :: notifications-spi`, `instructor :: instructor-spi`, `course :: course-spi`, `coursecreator :: coursecreator-spi`, `timetabling :: timetabling-spi`, `commerce.purchase :: commerce-purchase-spi` | Anchor for user/org context; exposes user lookup and domain mapping. |
| `instructor` | `instructor-spi` | `shared` | Exposes instructor lookup/verification; must not depend directly on timetabling internals (use shared enrollment lookup instead). |
| `timetabling` | `timetabling-spi` | `shared`, `availability :: availability-spi`, `classes :: classes-spi`, `course :: course-spi`, `commerce :: commerce-paywall`, `student`, `tenancy` | Drives scheduling and enrollment; enforces paywall via the `commerce-paywall` named interface and course logic via `course-spi`. |
| `commerce` | `commerce-purchase-spi`, `commerce-paywall` | `shared`, `tenancy`, `systemconfig`, `commerce.purchase :: commerce-purchase-spi` | Hosts the paywall SPI; keep external consumers on `commerce-paywall` (not purchase internals). |
| `commerce.purchase` | `commerce-purchase-spi` | `shared`, `commerce :: commerce-paywall`, `tenancy :: tenancy-spi`, `student :: student-spi` | Persists purchase snapshots; consumes the paywall SPI instead of direct commerce internals. |

## Adding or Adjusting Dependencies
- Define a new named interface in the providing module’s `spi` (or similarly scoped) package with `@NamedInterface("<interface-name>")`.
- Update the consuming module’s `@ApplicationModule` declaration to include `"<provider> :: <interface-name>"`.
- Prefer expanding shared abstractions (e.g., `shared.spi.enrollment.EnrollmentLookupService`) when multiple modules need the same read-only surface to avoid cycles.

## Quick Health Checks
- If the architecture test reports a cycle, refactor the consumer to call a named interface or shift the contract into `shared`. Avoid adding broad module-to-module dependencies that bypass SPIs.
- Keep module documentation in sync with boundary changes so future integrations don’t reintroduce violations.
