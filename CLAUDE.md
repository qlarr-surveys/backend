# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Qlarr Backend — a Spring Boot 3.0.2 REST API written in Kotlin 2.0.20, providing survey creation, execution, offline caching/syncing, and administration. Surveys are defined as JSON with JavaScript-based logic evaluated via GraalVM.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run (starts on http://localhost:8080)
./gradlew bootRun

# Run all tests (requires Docker for Testcontainers)
./gradlew test

# Run a single test class
./gradlew test --tests "com.qlarr.backend.end2end.RunSurveyE2ETest"

# Run a single test method
./gradlew test --tests "com.qlarr.backend.end2end.RunSurveyE2ETest.testMethodName"

# Start local PostgreSQL
docker-compose up -d postgres-db
```

## Local Development Prerequisites

- Java 19+
- Docker (for PostgreSQL and Testcontainers in tests)
- Local profile auto-configures: PostgreSQL at `localhost:5432/qlarrdb` (user: `qlarr`, pass: `password`), Mailhog at `localhost:1025`

## Architecture

**Layered structure:** Controllers → Services → Repositories → PostgreSQL

Source root: `src/main/kotlin/com/qlarr/backend/`

| Layer | Package | Purpose |
|-------|---------|---------|
| API DTOs | `api/` | Request/response models organized by domain (`design/`, `survey/`, `response/`, `runsurvey/`, `user/`, `offline/`, `surveyengine/`, `version/`) |
| Controllers | `controllers/` | REST endpoints, `@PreAuthorize` for role-based access |
| Services | `services/` | Business logic |
| Persistence | `persistence/entities/`, `persistence/repositories/` | JPA entities and Spring Data repositories |
| Mappers | `mappers/` | Entity ↔ DTO conversion |
| Security | `security/` | JWT auth filter chain, token generation/validation |
| Config | `configurations/`, `properties/` | Spring beans, externalized config (`JwtProperties`, `FileSystemProperties`, `EmailProperties`) |
| Expressions | `expressionmanager/` | GraalVM JavaScript evaluation for survey logic |
| Error handling | `error/`, `exceptions/` | Global exception handler + domain exceptions |

**Key external dependency:** `qlarr-surveys:survey-engine-kmp` (Kotlin Multiplatform survey engine, via JitPack).

## Database

- **PostgreSQL 15.1** with **Liquibase** migrations (YAML master changelog at `src/main/resources/db/changelog/db.changelog-master.yaml`, SQL files in `db/changelog/DDL/`)
- JPA DDL-auto is `none` — all schema changes go through Liquibase migration files
- Response values stored as JSONB for flexible schema
- Users use soft deletes (`deleted` flag)

## Authentication & Authorization

- Stateless JWT authentication (access token: 1h, refresh token: 1y)
- `JwtTokenFilter` validates on every request; `JwtService` handles token lifecycle
- Roles: `super_admin`, `survey_admin`, `analyst`, `surveyor`, `respondent`
- Controllers use `@PreAuthorize("hasAnyAuthority(...)")` for endpoint-level authorization
- Public endpoints: survey execution (`/survey/{id}/run/*`), resources, attachments, login, password reset

## Testing

- **E2E tests** (`end2end/`): `@SpringBootTest` with `WebTestClient` + Testcontainers PostgreSQL. Extend `E2ETestBase`.
- **Controller integration tests** (`controllers/`): `@WebMvcTest` with MockMvc + SpringMockK. Extend `IntegrationTestBase` which provides `withRole()`/`withRoles()` helpers.
- **Unit tests** (`services/`, `helpers/`): MockK for mocking.
- Docker must be running for any test that uses Testcontainers.

## PR & Commit Conventions

PR titles must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification (enforced by GitHub Actions via `amannn/action-semantic-pull-request`).

## API Structure

No `/api/` prefix — endpoints are at root level (e.g., `/user/login`, `/survey/{id}`, `/dashboard/surveys`). Response export supports CSV and XLSX formats.