# Qlarr Backend
**Qlarr Surveys** is a framework to create and run customizable, scientific & offline-first **[surveys as code](https://github.com/qlarr-surveys/survey-engine)** on all platforms. Surveys are defined using JSON to represent ui agnostic survey components and [Javascript](https://github.com/qlarr-surveys/survey-engine-script) instructions to represent complex survey logic. 

This is the backend application for qlarr, built using Spring Boot to 
 - expose the main usescase of Qlarr surveys of creating and executing surveys for web application
 - support caching surveys for offline use and syncing the responses from such offline surveys
 - support operations for Survey management and administrive functionalities like login, adding using, cloning surveys....et 



## Operations
If you wish to run this project for a production environment, we recommend you to use our [docker-compose](https://github.com/qlarr-surveys/backend/blob/main/docker-compose.yml) file that deploys both frontend and backend

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


## Contributing
We welcome contributors, the easiest way to contribute to this project is to join our [Discored server](https://discord.gg/3exUNKwsET) and talk to us directly


## Support
If you are interested in a new feature, please start a Discussion/ Idea
To report a bug, please raise an issue with clear steps to reproduce... Export the survey with the issue and import it as part of your bug report
