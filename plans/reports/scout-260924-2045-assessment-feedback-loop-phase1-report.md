# Scout Report — Phase 1 Assessment → DeepTutor Feedback Loop

Date: 2026-09-24 · Branch: `main` @ `352d62b` · Mode: read-only scout (no code changed)

Target flow: Assessment finalize → outbox → RabbitMQ → AI Learning consumer → DeepTutor LearningService → `mastery_paths.state_json` → `policy.next_objective()` → `GET /api/ai-learning/status`.

## 0. Hard blockers (must resolve before any integration code)

| # | Blocker | Evidence |
|---|---|---|
| B1 | **DeepTutor source absent.** Submodule not initialized (`git submodule status` → `-da856ad…`), `third_party/deeptutor/` empty. Every DeepTutor API below is UNVERIFIED. | `.gitmodules`, `git ls-tree HEAD third_party/deeptutor` → `da856ad67075d49b483150ac44ebca710cb5f266` |
| B2 | **Submodule remote = upstream `HKUDS/DeepTutor`**, not a team fork. Plan L159 expects a team fork. Any `record_external_assessment` patch cannot be committed reproducibly → likely **DEEPTUTOR FORK PATCH REQUIRED**. | `.gitmodules`, plan L159/198-208/423/485-489 |
| B3 | **No local runtime for validation**: Docker daemon not running; Python not installed on host; `java -version` printed nothing (Maven 3.9.11 present). Testcontainers, RabbitMQ, all Python tests currently not executable. | tool probes |
| B4 | **AGENTS.md forbids adding messaging without explicit approval** (L26, L40, L143, L298). Spec says RabbitMQ is final (plan L16/239/460) but exchange/queue/DLQ/client lib are TO DEFINE. | `AGENTS.md`, plan |

## 1. Relevant files

### Assessment Service (`services/assessment-service/`, prefix `src/main/java/com/group01/assessment` = `…`)
- `…/application/usecase/CreateAssessmentResultUseCase.java:28-35` — `@Transactional`; requires SUBMITTED attempt; `version = latest+1` (unlocked read, race only caught by UNIQUE); saves result **directly as `COMPLETED`**. Called by learner via `POST /api/assessments/attempts/{attemptId}/result` (`api/controller/AssessmentResultController.java:29`).
- `…/application/usecase/SaveAssessmentResultDetailsUseCase.java:78-169` — `@Transactional`, `PESSIMISTIC_WRITE` on latest result (`…/infrastructure/persistence/repository/AssessmentResultJpaRepository.java:20-24`); upserts skill_scores / item_results / error_analysis_items. **No controller calls it.**
- `…/application/usecase/SubmitAssessmentAttemptUseCase.java:9`, `…/domain/aggregate/AssessmentAttempt.java:50-71` (submit/expire), `…/domain/vo/AttemptStatus.java`.
- `…/application/usecase/StartAssessmentAttemptUseCase.java:120` — copies client-supplied opaque `knowledge_snapshot` JSON onto `attempt_items`.
- `…/domain/entity/AssessmentResult.java` (record; `status` is String), `…/domain/entity/ItemResult.java`, `ItemResultInput.java`.
- `src/main/resources/db/migration/V1__create_assessment_tables.sql` — `attempt_items` (L33 `question_version_id`, L37 `knowledge_snapshot JSONB`), `assessment_results` (L68-76, `UNIQUE(attempt_id,result_version)`), `skill_scores` (L78-87), `item_results` (L89-98, **no `max_score`**), `error_analysis_items` (L100-106), **`outbox_events` (L164-174) + `idx_outbox_pending` (L186)** — table exists, no Java code uses it. Next migration = **V4**.
- `pom.xml` — no AMQP/messaging deps; Testcontainers postgres present.
- Tests: `…/infrastructure/persistence/AssessmentSchemaValidationTest.java` (Testcontainers `postgres:15-alpine`, Flyway + Hibernate validate — new entities must match DDL), `AssessmentPersistenceMapperTest`, `AssessmentAttemptTest`, `SaveAttemptResponseUseCaseTest`. No tests for result creation/details.
- Config: `infra/config-server/config-repo/assessment-service.yaml` (no broker/scheduling props).

### Outbox conventions to reuse (all write-only; **no relay/publisher anywhere in repo**)
- **content-service** (same DDL shape as assessment: `published_at` NULL = pending, `retry_count`, `last_error`) — best to copy:
  `services/content-service/src/main/java/com/group01/content/domain/aggregate/OutboxEvent.java` (`create` :32, `markPublished` :37, `recordError` :41), `domain/repository/OutboxEventRepository.java`, `infrastructure/persistence/{entity/OutboxEventJpaEntity.java, adapter/OutboxEventRepositoryAdapter.java, mapper/OutboxEventPersistenceMapper.java, repository/OutboxEventJpaRepository.java:15}` (JPQL pending query).
- **game-service** writer port: `services/game-service/src/main/java/com/group01/game/application/port/OutboxWriter.java:6` (`append(aggregateType, aggregateId, eventType, Map payload)`), `infrastructure/persistence/adapter/OutboxWriterAdapter.java:21` — avoids hand-built JSON.
- access-service uses a different status-based schema + `String.format` JSON (don't copy).
- Event naming convention in repo: PascalCase, no version (`PointDebited`, `GameMatchCompleted`); spec introduces `AssessmentCompleted.v2`.

### AI Learning Service (`services/ai-learning-service/`, Python 3.11, FastAPI, psycopg2)
- `app/persistence/postgres_learning_store.py` — imports `LearningProgress`, `LearningConflictError`, `LearningStoreError`, `LearningTransaction` (L16-21). `SELECT … FOR UPDATE` (L153-157); create path (L166-177); CAS commit `revision = base+1` (L185-200); inserts `tx.events` into `mastery_events` (L201-207); rewrites DeepTutor qmark SQL for `mastery_interactions` (L42-61). ContextVar-joined nested transactions (L141-147). **Never writes `mastery_learning_evidence`.** Docstring claims "DeepTutor v1.6.9".
- `app/application/path_service.py` — imports `map_summary, next_objective`, `LearningService` (L9-10). `ensure_active_path` (L58-87) always resolves goal via User Service active goal → **no `ensure_path(user_id, learning_goal_id)` variant** for the consumer. `_create_path` (L89-97). `next_objective(progress)` called only in `active_status` (L112).
- `app/adapters/curriculum_adapter.py` — builds DeepTutor `KnowledgePoint/LearningModule` from Content KPs (identity = Content `knowledge_points.id`).
- `main.py` — routes: `POST /api/ai-learning/paths` (L90), `/progress` (L105), `/status` (L115), `/paths/{path_id}/map` (L125).
- `app/config.py` — `pydantic_settings`, prefix `AI_LEARNING_`, `lru_cache`.
- `app/clients/{content_service.py, user_service.py}`, `app/security/internal_jwt.py`, `app/api/dto/responses.py:20-52`.
- `migrations/V1__one_mastery_path_per_learning_goal.sql` — only migration (partial unique `(user_id, learning_goal_id)`); **no DDL for base V5 tables, no migration runner**.
- `requirements.txt` — no broker client; `sqlalchemy` listed but unused. `Dockerfile:11-12` — `pip install ./third_party/deeptutor` (build ctx = repo root).
- Tests (unittest style, no conftest): `tests/test_curriculum_adapter.py` (imports DeepTutor → fails while B1), `test_mastery_path_goal_uniqueness.py` (needs `AI_LEARNING_TEST_DATABASE_URL`), `test_api_response_dto.py`, `test_internal_jwt.py`. No store/path-service tests.

### Content Service (KP identity + mapping source)
- `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` — `knowledge_points` (L18-30, `id UUID`, `code UNIQUE`), `question_knowledge_points` (L153-158, PK `(question_version_id, knowledge_point_id)`, `weight NUMERIC(5,2) DEFAULT 1.00`).
- `V2__add_knowledge_point_learning_type.sql` — `learning_type` MEMORY/CONCEPT/PROCEDURE/DESIGN.
- ⚠ **Duplicate Flyway version**: `V2__add_knowledge_point_learning_type.sql` + `V2__align_content_entitlements.sql` (verified) — Flyway will refuse to migrate.
- KP mappings exposed only via public `QuestionVersionResponse.knowledgePoints` (`QuestionKnowledgePointResponse(questionVersionId, knowledgePointId, weight)`); **no internal mapping endpoint**. `api/dto/internal/` only has game snapshot DTOs.

### Specs / docs
- `.sdd/specs/ai-learning-phase1-plan.md` — Stage A done (L16); v2 envelope L287-296 (`learning_goal_id?` optional + active-goal fallback L241-249 — **contradicts task: goal required, no fallback**); weight = metadata (L302); UUIDv5 inputs (L306, namespace undefined); regrade/replay (L310, TO VERIFY); adapter/ingestion responsibilities (L71-78, L215-222); DeepTutor extension gated on approval (L198-208, L485-489); DoD all unchecked (L442-452).
- `.sdd/database/DATABASE_V5.md` — `mastery_paths` (L1132-1157), `mastery_interactions` (L1176-1200), `mastery_events` (L1204-1223), `mastery_learning_evidence` (L1227-1258; index `(path_id, source, source_reference_id)` at L1255 is **non-unique**), ingest rules (L1529-1565), `outbox_events` (L2290-2337), `question_knowledge_points` (L675-686). **No inbox/processed-events table.**
- `.sdd/specs/SERVICE_ARCHITECTURE_V2.md` §14.2 (L636-655), §16.2 (L718-746) — names event, no definition, no RabbitMQ.
- `AGENTS.md` — L26 approval for messaging; L109 no empty packages; L196/L220 Flyway naming, never edit applied migrations; L195/L266 test conventions; L258-264 graphify update.
- `docs/journals/2026-09-24-ai-learning-stage-a.md` — tests never run; fork remote unresolved.

### Infra
- `docker-compose.yml` — only `learning-support-db`, `community-db`, `game-db`, `game-service`; network `codebase-network`; env pattern `${X:?Set X}` / `${X:-default}`, ports `127.0.0.1:…`; healthcheck + `condition: service_healthy`. **No rabbitmq, assessment-service, ai-learning-service.**
- `pom.xml` — Spring Boot 3.5.14 / Cloud 2025.0.0 BOMs (`spring-boot-starter-amqp` version would come from BOM).
- `infra/config-server/config-repo/*.yaml` — no messaging config anywhere.

## 2. Gap summary vs. required flow

| Step | Status |
|---|---|
| Formal "finalize" transition | Missing — result born `COMPLETED` before details; details use case unreachable |
| `learning_goal_id` on attempt/result | Missing everywhere in assessment-service |
| Q→KP snapshot with weights | Missing — only opaque client `knowledge_snapshot`; no weights; no `max_score` |
| Outbox write in finalize tx | Table exists; no Java entity/writer |
| Outbox relay → RabbitMQ | Nothing in repo (no relay in any service) |
| RabbitMQ infra/config | Nothing (compose, config-repo, deps) |
| Python consumer | Nothing; no broker lib |
| Explicit-goal `ensure_path` | Missing |
| Evidence idempotency constraint | Missing (V5 index non-unique; no migration) |
| `mastery_learning_evidence` projection | Never written |
| Base V5 DDL for ai_learning_db | Not in repo (assumed pre-created) |
| DeepTutor external-assessment seam | Unknown until B1 resolved; proposed only |
| Tests for store/ingestion/E2E | None |

## 3. Unresolved questions (need user decision)
1. Init submodule at pinned `da856ad` (`git submodule update --init third_party/deeptutor`) — and is there a team fork remote to switch `.gitmodules` to? (B1/B2)
2. Explicit approval to add RabbitMQ (compose service, `spring-boot-starter-amqp`, Python client — `pika` vs `aio-pika`) per AGENTS.md L26. (B4)
3. What is "finalize" in Assessment: new explicit finalize use case (DRAFT/PROCESSING → COMPLETED) emitting the event, vs. emit on `CreateAssessmentResultUseCase`? Current create marks COMPLETED immediately.
4. Source of `learning_goal_id`: new column on `assessment_attempts` set at start (client-supplied? validated?).
5. Source of KP snapshot + weights + `max_score`: snapshot at attempt start from `question_knowledge_points` (needs Content internal endpoint or client-supplied structured snapshot), persisted in V4 table/columns?
6. Event naming: `AssessmentCompleted.v2` (spec) vs. repo PascalCase-no-version convention.
7. UUIDv5 namespace value + canonical serialization; exchange/queue/routing/DLQ names.
8. Who owns base V5 DDL + migration runner for `ai_learning_db` (needed for evidence UNIQUE constraint)?
9. Content-service duplicate `V2__` migrations — fix separately (out of scope but will break any Testcontainers run of content-service).
10. Environment: start Docker Desktop, install Python 3.11, fix `java` on PATH to allow validation.
