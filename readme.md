# Qlarr Backend
A Spring Boot backend that uses Qlarr Survey Engnine, a UI-agnostic tool that lets you create and run customizable, scientific & offline-first surveys as code on all platforms.

In addition to exposing the main usescase of Qlarr survey engine of creating and executing surveys for web application this backend also
 - supports caching surveys for offline use and syncing the responses from such offline surveys
 - supports operations for Survey management and administrive functionalities like login, adding using, cloning surveys....et 

## Contributing
Wanis


## Support
Wanis

## Operations
If you wish to run this project for a production environment, we recommend you to use our docker repo that packages both frontend and backend

To run this locally, follow these steps:

### Prerequisites

- **Java**: Install [Java 19](https://jdk.java.net/19/) or later.
- **Docker**: For local database setup, install [Docker](https://www.docker.com/) and Docker Compose.
- **PostgreSQL**: If not using Docker, ensure PostgreSQL is installed locally.

### Local Development Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/qlarr-surveys/backend.git
   cd backend
   ```

3. **Database Setup**:
   - **With Docker**: Run PostgreSQL in a container with:
     ```bash
     docker-compose  up -d postgres-db
     ```
   - **Without Docker**: Set up a local PostgreSQL instance and create the database `qlarr_db`.

4. **Build the Application**:
   ```bash
   ./gradlew build
   ```

5. **Run the Application**:
   - Start the backend service with:
     ```bash
     ./gradlew bootRun
     ```
   - The application should now be accessible at `http://localhost:8080`.

6. **Testing**:
   - Run unit and integration tests:
     ```bash
     ./gradlew test
     ```
