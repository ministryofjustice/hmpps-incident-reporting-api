# Project Guidelines: HMPPS Incident Reporting API

## Project Overview
This application is a REST API and database that serves as the source of truth for incident report data within prisons. It is part of the HMPPS (Her Majesty's Prison and Probation Service) Digital Prison Services (DPS) ecosystem.

## Tech Stack
- **Language:** Kotlin (JVM 25)
- **Framework:** Spring Boot (with Spring Security, Spring Data JPA, WebClient, and WebFlux)
- **Database:** PostgreSQL (managed by Flyway migrations)
- **Messaging:** Amazon SQS (via `hmpps-sqs-spring-boot-starter`)
- **Observability:** OpenTelemetry and Application Insights
- **Build System:** Gradle
- **API Documentation:** OpenAPI/Swagger (SpringDoc)

## Project Structure
The source code is located in `src/main/kotlin/uk/gov/justice/digital/hmpps/incidentreporting/`:
- `resource/`: REST controllers (API endpoints), heavily annotated with OpenAPI schemas.
- `service/`: Business logic layer, coordinating between repositories and external services.
- `jpa/`: Data access layer, including:
    - `repository/`: Spring Data repositories and custom implementations.
    - `id/`: Composite keys or ID-related logic.
- `dto/`: Data Transfer Objects for API requests and responses.
- `config/`: Spring Boot configuration classes.
- `constants/`: Enums and constant values used throughout the application.

Other key directories:
- `src/main/resources/db/migration/`: Flyway SQL migration files.
- `src/main/resources/reports/`: JSON and other report-related assets.
- `src/test/kotlin/`: Unit and integration tests, often using Testcontainers (Postgres, Localstack) and WireMock.
- `helm_deploy/`: Kubernetes deployment configurations.

## Development Workflow
- **Build:** `./gradlew build`
- **Run Tests:** `./gradlew test` (unit and integration tests)
- **Linting:**
    - Check style: `./gradlew ktlintCheck`
    - Auto-fix style: `./gradlew ktlintformat`
- **Local DB:** Use `docker compose -f docker-compose-local.yml up` to start a local PostgreSQL instance.

## Coding Standards
- **Layered Architecture:** Follow the Resource -> Service -> Repository pattern.
- **Null Safety:** Leverage Kotlin's null-safety features consistently.
- **API Documentation:** Ensure all new or modified endpoints have clear OpenAPI annotations (descriptions, examples, schemas).
- **Validation:** Use standard Spring validation annotations (`@Valid`, `@RequestBody`, etc.) in resources and DTOs.
- **Database Migrations:** All schema changes must be implemented as Flyway migrations in `src/main/resources/db/migration/`.
- **Testing:**
    - Write unit tests for business logic in services.
    - Write integration tests for resource endpoints using `WebTestClient`.
    - Use Testcontainers for any tests requiring a real database or SQS.
