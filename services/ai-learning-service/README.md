# `ai-learning-service`

Service chuyên trách **Adaptive Learning và DeepTutor AI Tutor Core** cho nền tảng IELTSPath.

## Tech Stack
- Python 3.11+
- FastAPI
- DeepTutor Engine
- PostgreSQL (`ai_learning_db`)

## Phase 1 scope
- Goal-bound DeepTutor Mastery Path bootstrap from User Service and Content Service.
- Learner adaptive state persisted in PostgreSQL `mastery_paths.state_json`.
- Safe progress, status, and path-map reads derived through DeepTutor v1.6.9 policy.
- Internal JWT authentication using only the validated Authorization bearer token.
- Formal Assessment feedback: an `AssessmentCompleted.v2` RabbitMQ consumer applies
  finalized, pre-graded results through DeepTutor (see
  `docs/contracts/assessment-completed-v2.md`).

Tutor sessions, question-level review APIs, and mistake practice are outside this phase.

## Build the service image

Build from the repository root so Docker can install the pinned local DeepTutor
submodule:

```powershell
docker build -f services/ai-learning-service/Dockerfile -t ieltspath-ai-learning .
```

The image installs the service requirements and then installs DeepTutor from
`third_party/deeptutor`; it does not fetch a separate DeepTutor release at
runtime. The service configuration uses `AI_LEARNING_INTERNAL_JWT_SECRET`,
`AI_LEARNING_DATABASE_URL`, `AI_LEARNING_USER_SERVICE_BASE_URL`, and
`AI_LEARNING_CONTENT_SERVICE_BASE_URL`. The JWT secret must be Base64 encoded
and decode to at least 32 bytes. Supply secret values through runtime
configuration; do not put them in this file or the image.

## Database migrations

`ai_learning_db` is migrated by Flyway from `migrations/`, in numeric version order:

| Version | File | Creates |
| --- | --- | --- |
| `0.1` | `V0_1__create_v5_mastery_tables.sql` | V5 `mastery_paths`, `mastery_interactions`, `mastery_events` |
| `1` | `V1__one_mastery_path_per_learning_goal.sql` | One path per `(user_id, learning_goal_id)` |
| `2` | `V2__formal_assessment_evidence.sql` | Evidence projection and the result-version ledger |
| `3` | `V3__pending_formal_assessment_results.sql` | Results parked until the goal's path exists |
| `4` | `V4__mastery_path_knowledge_point_bands.sql` | Band snapshot of each knowledge point in a path |

`V1` stops if the database already contains more than one path for a non-null
`(user_id, learning_goal_id)` pair; reconcile those rows before retrying.

`mastery_interactions.status` accepts the lowercase values DeepTutor writes
(`registered`, `awaiting_input`, `answered`, `graded`, `abandoned`), not the uppercase
names in `DATABASE_V5.md`. `interaction_id` is a UUID as in V5; Phase 1 never inserts
interactions, so DeepTutor's question-id format is checked when Tutor Chat is built.

A database where `V1` and `V2` were applied by hand has no Flyway history table, so
`flyway migrate` reports a non-empty schema. Baseline it once at the last applied
version, then migrate normally:

```powershell
flyway -baselineOnMigrate=true -baselineVersion=2 migrate
```

The PostgreSQL tests build each schema by running this same migration chain
(`tests/postgres_schema_support.py`); there is no hand-written DDL in the tests.
`AI_LEARNING_TEST_DATABASE_URL` points them at a disposable database.

The service uses DeepTutor's synchronous `LearningStore` interface. Its
PostgreSQL adapter locks one aggregate row with `SELECT ... FOR UPDATE`; nested
DeepTutor transactions for bootstrap join one PostgreSQL transaction so path
ownership, initial state, curriculum, revision, and events commit together.
Run the Flyway migrations before enabling the path endpoints.
The active-goal and curriculum contracts also depend on User Service
`V4__enforce_one_active_learning_goal_per_user.sql` and Content Service
`V3__add_knowledge_point_learning_type.sql` being applied to their own databases.

A new path contains only the knowledge points in scope for the active goal's `targetBand`: a point is kept when
its effective `bandMin` from Content Service is empty or not above the target band. There is no upper bound; easier
points stay in the path and placement test-out skips them. A topic whose points were all left out is dropped, and
if nothing is left the path API returns 409. The effective band of every kept point is stored with the path
(`mastery_path_knowledge_point_bands`) in the creating transaction, before parked results are applied. The applied
scope is recorded as a `path.scope_applied` event. Scoping decides what the path contains; which point to learn next
is still decided only by DeepTutor's `next_objective()`. An existing path is returned as is, without reading
Content again.

A finalized `PLACEMENT` result tests out the points the learner already masters, in the same transaction as its
evidence: points whose effective `bandMax` is not above the placement's `overall_band`, and points every placement
item answered correctly (or judged PASS). Test-out is a DeepTutor learner mastery override, so `next_objective()`
skips the point; the placement evidence is still recorded and no mastery score, gate, policy or scheduler changes.
The pinned DeepTutor submodule is the unmodified upstream release, so provenance is kept in the override note
(`placement:{attemptId}:v{version}`) and `/progress` and `/map` report `masterySource: "placement"` for it
(`system` = cleared by evidence, `learner` = the learner's own claim). A regraded placement replaces that attempt's
test-out; a learner's own override is never replaced or cleared. Tested-out points have no repetition state, so
they are not scheduled for review.

Curriculum topics follow Content Service's sibling `sortOrder` in tree preorder.
Knowledge Points are ordered by `createdAt` ascending, with UUID as a stable
tie-breaker; Content Service does not currently expose an editorial learning
order for Knowledge Points.

`POST /api/ai-learning/paths` also refreshes an existing path from Content: new points in scope are added, names and
order follow Content, and the response's `addedKnowledgePointCount` counts the points added by the call (all of them
when the path is created). A refresh only adds, because DeepTutor's `replace_modules` deletes the state of any point
missing from the new module set. A point that left the curriculum (inactive, deleted, or now above the target band)
stays in the path with its history and is retired with a `retired:content` override, so `next_objective()` skips it
and `masterySource` reports `retired`; it is restored if it comes back in scope. DeepTutor's summary counts a retired
point as cleared. An unchanged curriculum commits nothing. The GET routes never read Content for an existing path.

The public Phase 1 routes are:

- `POST /api/ai-learning/paths`
- `GET /api/ai-learning/progress`
- `GET /api/ai-learning/status`
- `GET /api/ai-learning/paths/{pathId}/map`

## Run with Docker Compose

From the repository root, with the variables listed in the root README in `.env`:

```bash
docker compose up -d --build rabbitmq ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer
```

| Service | What it does | Environment it receives |
| --- | --- | --- |
| `ai-learning-db` | PostgreSQL `ai_learning_db` on `127.0.0.1:5436` | `AI_LEARNING_DB_PASSWORD` |
| `ai-learning-migrate` | `flyway migrate` once over `migrations/`, then exits 0 | JDBC URL, `postgres`, `AI_LEARNING_DB_PASSWORD` |
| `ai-learning-api` | `uvicorn main:app` on `127.0.0.1:8000`, starts after the migration | `AI_LEARNING_INTERNAL_JWT_SECRET` (from `GATEWAY_INTERNAL_JWT_SECRET`), `AI_LEARNING_DATABASE_URL`, `AI_LEARNING_USER_SERVICE_BASE_URL`, `AI_LEARNING_CONTENT_SERVICE_BASE_URL` |
| `ai-learning-consumer` | `python -m app.messaging.assessment_consumer`, restarted if it exits | `AI_LEARNING_DATABASE_URL`, `AI_LEARNING_AMQP_URL` only (no JWT secret) |

The API forwards the learner's internal JWT straight to User (`8085`) and Content (`8082`),
not through the Gateway, so both base URLs default to `http://host.docker.internal:<port>`.
Both containers use one image; the build installs DeepTutor with its full dependency stack
(about 1.6 GB), although the main flow imports no LLM module.

Check the migration with `docker compose run --rm ai-learning-migrate info`; running
`migrate` again reports that the schema is up to date.

## Formal assessment consumer

Run the consumer as a separate process from the same image:

```powershell
python -m app.messaging.assessment_consumer
```

It needs `AI_LEARNING_DATABASE_URL` and `AI_LEARNING_AMQP_URL`. Optional settings are
`AI_LEARNING_ASSESSMENT_EXCHANGE` (default `assessment.events`),
`AI_LEARNING_RETRY_DELAY_MS` and `AI_LEARNING_MAX_DELIVERY_ATTEMPTS`. It declares its
own queue, retry queue and dead-letter queue.

A result whose goal has no path yet is parked in `pending_formal_assessment_results`
and ACKed (log outcome `pending`), not retried. The first `POST /paths`, `GET /progress`
or `GET /status` for that goal creates the path and applies the parked results in the
same transaction. Parked rows are kept until then; nothing expires them yet, so a goal
that is never opened keeps its rows. See `docs/contracts/assessment-completed-v2.md`.

Apply `migrations/V2__formal_assessment_evidence.sql` after V1. It adds:

- the `mastery_learning_evidence` projection;
- the `UNIQUE (path_id, source, source_reference_id)` evidence identity;
- the `formal_assessment_result_versions` ledger.

Every aggregate commit rebuilds the projection from `state_json` in the same
transaction.

DeepTutor v1.6.9 has no entry point for already-graded results.
`app/learning/external_assessment.py` subclasses `LearningService` and replays the
post-grade steps of `_apply_grade`, calling DeepTutor's own methods for every
adaptive computation:

- `record_quiz_attempt`
- `_record_quiz_evidence`
- `calculate_mastery` and `update_mastery`
- `SpacedRepetitionScheduler`
- `record_qualitative_in_memory`
- `scheduler.replay` for regrades

The consumer never creates a learning path, because creating one needs the
learner's curriculum token. An event for a goal that has no path yet is retried,
then parked in the dead-letter queue.

Tests run from this directory with the pinned submodule on the import path:

```powershell
$env:PYTHONPATH = "../../third_party/deeptutor;."
python -m pytest tests
```

`AI_LEARNING_TEST_DATABASE_URL` enables the PostgreSQL integration and end-to-end
tests, which each run in their own throwaway schema. `AI_LEARNING_TEST_AMQP_URL`
enables the RabbitMQ tests.

