# Qlarr Backend

[Discord 💬](https://discord.gg/9mbRh6SpGj) | [Demo 🖥️](https://console.qlarr.com/) | [Docs 📄](https://qlarr-surveys.github.io/docs/) | [Deploy Locally in 30 Sec! 🚀](https://qlarr-surveys.github.io/docs/guides/deployment/local)

**Qlarr Surveys** is a framework to create and run customizable, scientific & offline-first **[surveys as code](https://github.com/qlarr-surveys/survey-engine)** on all platforms. Surveys are defined using JSON to represent UI-agnostic survey components and [JavaScript](https://github.com/qlarr-surveys/survey-engine-script) instructions to represent complex survey logic.

This is the backend application for Qlarr, built using **Spring Boot 3.0.2** and **Kotlin 2.0.20** to:
- Expose the main use cases of Qlarr surveys: creating and executing surveys for web applications
- Support caching surveys for offline use and syncing responses from offline surveys
- Provide survey management and administrative functionalities like login, user management, cloning surveys, etc.

## Key Features

- 📄 **Survey As Code** — Write survey structure in JSON, and survey logic in JavaScript
- 📴 **Offline-First Design** — Collect data anywhere without internet connectivity
- ⍰ **Conditional Logic & Skip Logic** — Advanced branching based on user responses
- ✅ **Input Validation** — Ensure data quality with built-in validation checks
- 🎲 **Randomization & Sampling** — Randomize questions and options with weighted priorities
- 🌐 **Multilingual Surveys** — Support for multiple languages
- 🔗 **Piping** — Reference and display values from previous answers
- ⬅️➡️ **Flexible Navigation** — All questions, page-by-page, or question-by-question
- 🎨 **Conditional Formatting** — Dynamic styling based on responses
- ⏱️📊 **Time Limits & Scoring** (WIP) — Perfect for quizzes and timed assessments

## Tech Stack

- **Spring Boot**: 3.0.2
- **Kotlin**: 2.0.20
- **PostgreSQL**: 15.1 with Liquibase migrations
- **Authentication**: Stateless JWT (access token: 1h, refresh token: 1y)
- **Survey Engine**: [survey-engine-kmp](https://github.com/qlarr-surveys/survey-engine) (Kotlin Multiplatform)
- **JavaScript Evaluation**: GraalVM for survey logic expressions
- **Testing**: JUnit 5, MockK, Testcontainers

## Architecture

**Layered structure:** Controllers → Services → Repositories → PostgreSQL

| Layer | Package | Purpose |
|-------|---------|---------|
| Controllers | `controllers/` | REST endpoints with role-based authorization |
| Services | `services/` | Business logic |
| API DTOs | `api/` | Request/response models by domain |
| Persistence | `persistence/` | JPA entities and Spring Data repositories |
| Mappers | `mappers/` | Entity ↔ DTO conversion |
| Security | `security/` | JWT authentication filter chain |
| Expression Manager | `expressionmanager/` | GraalVM JavaScript evaluation for survey logic |

**API Structure**: No `/api/` prefix — endpoints are at root level (e.g., `/user/login`, `/survey/{id}`, `/dashboard/surveys`).

## Operations

If you wish to run this project for a production environment, we recommend using our [docker-compose](https://github.com/qlarr-surveys/backend/blob/main/docker-compose.yml) file that deploys both frontend and backend.

To run this locally, follow these steps:

### Prerequisites

- **Java**: Install [Java 19](https://jdk.java.net/19/) or later
- **Docker**: For local database setup and running tests, install [Docker](https://www.docker.com/) and Docker Compose
- **PostgreSQL**: If not using Docker, ensure PostgreSQL 15.1+ is installed locally

### Local Development Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/qlarr-surveys/backend.git
   cd backend
   ```

2. **Database Setup**:
   - **With Docker** (recommended):
     ```bash
     docker-compose up -d postgres-db
     ```
     This creates a PostgreSQL database at `localhost:5432/qlarrdb` with user `qlarr` and password `password`.

   - **Without Docker**: Set up a local PostgreSQL instance and create the database `qlarrdb` with the same credentials.

3. **Email Testing** (optional):
   - The local profile includes Mailhog for email testing at `localhost:1025`
   - Start with: `docker-compose up -d mailhog`

4. **Build the Application**:
   ```bash
   ./gradlew build
   ```

5. **Run the Application**:
   ```bash
   ./gradlew bootRun
   ```
   The application will be accessible at `http://localhost:8080`.

### Testing

**Important**: Docker must be running for tests that use Testcontainers.

```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.qlarr.backend.end2end.RunSurveyE2ETest"

# Run a single test method
./gradlew test --tests "com.qlarr.backend.end2end.RunSurveyE2ETest.testMethodName"
```

**Test Types**:
- **E2E tests** (`end2end/`): Full Spring Boot context with Testcontainers PostgreSQL
- **Controller integration tests** (`controllers/`): MockMvc with mocked services
- **Unit tests** (`services/`, `helpers/`): MockK for isolated testing

## Authentication & Authorization

- **JWT-based authentication**: Stateless tokens (access: 1h, refresh: 1y)
- **Roles**: `super_admin`, `survey_admin`, `analyst`, `surveyor`, `respondent`
- **Public endpoints**: Survey execution, resources, attachments, login, password reset
- **Protected endpoints**: Use `@PreAuthorize` annotations for role-based access control

## Database Migrations

All schema changes are managed through **Liquibase** migrations:
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- SQL files: `db/changelog/DDL/`
- JPA DDL-auto is set to `none`

## API Features

- Survey creation, execution, and management
- Offline caching and response syncing
- Response export in CSV and XLSX formats
- User management with soft deletes
- JavaScript-based survey logic evaluation
- File attachments and resources

## Contributing

We welcome contributors! The easiest way to contribute is to join our [Discord server](https://discord.gg/3exUNKwsET) and talk to us directly.

### Pull Request Conventions

PR titles must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This is enforced by GitHub Actions.

Examples:
- `feat: add bulk response export`
- `fix: resolve survey logic evaluation error`
- `docs: update API documentation`
- `refactor: simplify response mapper logic`

## Support

- **Feature requests**: Start a Discussion or Idea on GitHub
- **Bug reports**: Raise an issue with clear steps to reproduce. Export the survey with the issue and import it as part of your bug report.
- **Questions**: Join our [Discord server](https://discord.gg/3exUNKwsET)

## License

See the [LICENSE](LICENSE) file for details.
