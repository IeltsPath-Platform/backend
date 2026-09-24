# Assessment Service

Assessment Service owns assessment attempts and their local assessment history. It is a Spring MVC downstream service on port `8083` and uses the shared internal JWT security configuration.

## Scope

The service currently supports:

- creating, reading, submitting, expiring, and saving responses for assessment attempts;
- materializing section and item snapshots supplied by the caller;
- creating and reading local assessment results;
- creating learner submissions and local grading job state;
- creating video practice attempts.

When an attempt starts, the service captures two things by forwarding the caller's gateway JWT:

- the learner's active learning goal, from User Service `GET /api/users/me/learning-goals/active`;
- the question to knowledge-point mapping, from Content Service `POST /internal/assessment-content/knowledge-point-mappings`.

Both are stored with the attempt (`assessment_attempts.learning_goal_id`, `attempt_item_knowledge_points`) and are never re-resolved later.

A created result is a DRAFT. Grading saves item results, including `max_score` and optional per-knowledge-point `PASS`/`FAIL`/`NOT_ASSESSED` judgments. `FinalizeAssessmentResultUseCase` then moves the result to COMPLETED and writes `AssessmentCompleted.v2` to `outbox_events` in the same transaction. `OutboxRelay` publishes committed rows to the RabbitMQ exchange `assessment.events` with routing key `assessment.completed.v2`, using publisher confirms. See `docs/contracts/assessment-completed-v2.md`.

IDs such as package, question and video references stay local logical references. Point debit, entitlement checks, provider execution, and an HTTP surface for graders to save details and finalize are still follow-up work.

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
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `USER_SERVICE_URL`, `CONTENT_SERVICE_URL`
- `ASSESSMENT_OUTBOX_RELAY_ENABLED` (default `true`)

The service imports configuration from Config Server and registers with Eureka using the shared project runtime configuration. Do not put credentials or JWT secret values in source or documentation.

## Architecture

The module follows the project layering convention:

```text
api -> application -> domain
infrastructure -> domain
```

Controllers map validated HTTP requests to application commands. Use cases coordinate domain objects, repository contracts, and the `application/port` interfaces for User and Content lookups. JPA entities, Spring Data repositories, mappers, adapters, the HTTP clients (`infrastructure/client`), and the outbox relay (`infrastructure/messaging`) stay in infrastructure.

## Verification

From the repository root:

```powershell
mvn -pl services/assessment-service -am test
mvn -pl services/assessment-service -am compile -DskipTests
```

The module includes application/domain tests, an application-context test, and a Testcontainers PostgreSQL schema test.
The schema test applies Flyway, validates JPA mappings with Hibernate, and checks assessment result and video-practice
persistence against the V5 schema; it is skipped when Docker is unavailable. Creating video practice requires `videoId`,
`segmentId`, `practiceType`, and `referenceTextSnapshot` so the attempt retains its segment and transcript snapshot.
