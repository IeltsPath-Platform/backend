# Library Service

`library-service` (Spring Boot, Eureka name `LIBRARY-SERVICE`) runs on port `8081` and owns `library_db`. The root
Compose stack runs Library with the other deployed services; PostgreSQL publishes at host port `5440`.

## Data and routes

Flyway V1 creates the five catalog tables: `vocabulary_items`, `vocabulary_senses`, `learning_videos`,
`video_segments`, `video_segment_lexical_entries`. V2 creates six personal-library tables: `flashcard_decks`,
`flashcards`, `flashcard_deck_items`, `notes`, `video_learning_progress`, `saved_video_segments`. Four V2 foreign keys
to catalog tables use `ON DELETE RESTRICT`; there is no `outbox_events` table. Data is not copied from the former
service/database.

Gateway routes `/api/content/videos/**`, `/api/content/vocabulary/**` and `/api/content/admin/vocabulary/**` here.
It also routes `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` here; the public
paths and request/response shapes remain the same. Catalog writes require `ADMIN` or `CONTENT_AUTHOR`; personal
library writes are owner-scoped.

When saving a video, Library verifies its topic through Content's `GET /api/content/topics/{id}`, forwarding the
authenticated bearer token and `X-Correlation-Id`. The HTTP client uses a 2-second connection and 5-second read
timeout; Content failures return 503. `POST /internal/game-content/snapshots` serves `VOCABULARY` using the shared
[game snapshot contract](../../docs/contracts/game-content-snapshot-v1.md); Content serves `GRAMMAR`.

## Local setup

For the full container stack, run `docker compose up -d --build`. To run Library from the host, start PostgreSQL with
`docker compose up -d postgres` and set `LIBRARY_DB_URL=jdbc:postgresql://localhost:5440/library_db` and
`LIBRARY_DB_PASSWORD` to the same local value as `POSTGRES_PASSWORD`. Runtime config is in
`infra/config-server/config-repo/library-service.yaml`:

| Variable | Default or purpose |
| --- | --- |
| `LIBRARY_DB_URL` | `jdbc:postgresql://localhost:5440/library_db` |
| `LIBRARY_DB_USERNAME` | `postgres` |
| `LIBRARY_DB_PASSWORD` | Required database password; no fallback in service config |
| `CONTENT_SERVICE_URL` | `http://localhost:8082` for topic lookup |
| `GATEWAY_INTERNAL_JWT_SECRET` | Verifies Gateway's internal JWT; use the same value as Gateway |

Verify with `mvn -q -pl services/library-service -am test`. Migration tests use Testcontainers and require Docker.
