
<!-- markdownlint-disable line-length -->

# Contributing to Qlarr Backend

Thank you for your interest in contributing to Qlarr Backend!

## Development Setup

### Prerequisites

- **Java** 19 or later
- **Docker** (for PostgreSQL and Testcontainers)
- **PostgreSQL** (optional, can use Docker)

### Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/qlarr-surveys/backend
   cd backend
   ```

2. Start PostgreSQL:

   ```bash
   docker-compose up -d postgres-db
   ```

3. Build the project:

   ```bash
   ./gradlew build
   ```

4. Run the application:

   ```bash
   ./gradlew bootRun
   ```

   The API will be available at `http://localhost:8080`

## Available Commands

| Command | Purpose |
| -------- | --------- |
| `./gradlew build` | Build the project |
| `./gradlew bootRun` | Run the application (port 8080) |
| `./gradlew test` | Run all tests (requires Docker) |
| `./gradlew test --tests "ClassName"` | Run a specific test class |
| `./gradlew test --tests "ClassName.methodName"` | Run a specific test method |

## Architecture

The backend follows a **layered architecture**:

```text
Controllers → Services → Repositories → PostgreSQL
```

| Layer | Package | Purpose |
| -------- | --------- | --------- |
| API DTOs | `api/` | Request/ response models |
| Controllers | `controllers/` | REST endpoints |
| Services | `services/` | Business logic |
| Persistence | `persistence/` | JPA entities and repositories |
| Mappers | `mappers/` | Entity ↔ DTO conversion |

## Database

- **PostgreSQL 15.1** with **Liquibase** migrations
- Migrations are in `src/main/resources/db/changelog/`
- All schema changes go through Liquibase migration files
- Response values stored as JSONB

## Authentication & Authorization

- Stateless JWT authentication (access token: 1h, refresh token: 1y)
- Roles: `super_admin`, `survey_admin`, `analyst`, `surveyor`, `respondent`
- Use `@PreAuthorize("hasAnyAuthority(...)")` for endpoint-level authorization

## PR Title Format

PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `fix:` - Bug fix
- `feat:` - New feature
- `refactor:` - Code refactoring
- `chore:` - Maintenance
- `docs:` - Documentation
- `build:` - Build system
- `ci:` - CI/CD
- `revert:` - Revert changes

Example: `fix: resolve 401 on token refresh` or `feat: add new survey export endpoint`

## Testing

Testing requires Docker to be running (for Testcontainers):

- **E2E tests**: `@SpringBootTest` with Testcontainers PostgreSQL
- **Controller tests**: `@WebMvcTest` with MockMvc
- **Unit tests**: MockK for mocking

Run tests:

```bash
./gradlew test
```

## Database Migrations

When adding database changes:

1. Create a new Liquibase changelog file in `src/main/resources/db/changelog/`
2. Add the changelog reference to `db.changelog-master.yaml`
3. Include rollback instructions

## Need Help?

Join our [Discord server](https://discord.gg/3exUNKwsET) to discuss with the team.
