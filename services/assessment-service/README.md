# Assessment Service

Assessment Service owns assessment attempts and their local assessment history. It is a Spring MVC downstream service on port `8083` and uses the shared internal JWT security configuration.

## Scope

The service currently supports:

- creating, reading, submitting, expiring, and saving responses for assessment attempts;
- materializing section and item snapshots supplied by the caller;
- creating and reading local assessment results;
- creating learner submissions and local grading job state;
- creating video practice attempts.

The service does not call Content, Access, User, AI, or human grading services. IDs such as package, question, video, and knowledge point references are stored as local logical references. Point debit, entitlement checks, provider execution, event publishing, and broker integration remain follow-up work.

## HTTP API

All assessment routes require an authenticated internal JWT. The authenticated subject is obtained from `CurrentUserProvider`; ownership is checked in the application layer.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/assessments/attempts` | Create an attempt from section/item snapshots |
| `GET` | `/api/assessments/attempts/{id}` | Read an owned attempt |
| `GET` | `/api/assessments/attempts/{id}/structure` | Read the owned attempt structure |
| `PUT` | `/api/assessments/attempts/{id}/items/{itemId}/response` | Save a response with an expected revision |
| `POST` | `/api/assessments/attempts/{id}/submit` | Submit an attempt |
| `POST` | `/api/assessments/attempts/{id}/expire` | Expire an attempt |
| `POST` | `/api/assessments/attempts/{id}/result` | Create a local result |
| `GET` | `/api/assessments/attempts/{id}/result` | Read an owned result |
| `POST` | `/api/assessments/submissions` | Create a Writing or Speaking submission |
| `POST` | `/api/assessments/grading-jobs` | Create local grading job state |
| `GET` | `/api/assessments/grading-jobs/{id}` | Read an owned grading job |
| `POST` | `/api/assessments/video-practice` | Create a video practice attempt |

Validation and domain errors use the module's `ErrorResponse` handler. Response saves use optimistic revision checks and return a conflict when the expected revision is stale.

## Persistence and configuration

The service owns the `assessment_db` PostgreSQL database. Flyway migrations are under `src/main/resources/db/migration` and Hibernate runs with `ddl-auto=validate` through centralized configuration. Set these runtime variables when running against a local database:

- `ASSESSMENT_DB_URL`
- `ASSESSMENT_DB_USERNAME`
- `ASSESSMENT_DB_PASSWORD`
- `GATEWAY_INTERNAL_JWT_SECRET`
- `INTERNAL_JWT_ISSUER`

The service imports configuration from Config Server and registers with Eureka using the shared project runtime configuration. Do not put credentials or JWT secret values in source or documentation.

## Architecture

The module follows the project layering convention:

```text
api -> application -> domain
infrastructure -> domain
```

Controllers map validated HTTP requests to application commands. Use cases coordinate domain objects and repository contracts. JPA entities, Spring Data repositories, mappers, and adapters stay in infrastructure. No cross-service client, message publisher, or provider adapter is part of the current module.

## Verification

From the repository root:

```powershell
mvn -pl services/assessment-service -am test
mvn -pl services/assessment-service -am compile -DskipTests
```

The module currently includes application/domain tests and an application-context test. PostgreSQL migration verification requires a PostgreSQL runtime or Testcontainers support when those tests are added.
