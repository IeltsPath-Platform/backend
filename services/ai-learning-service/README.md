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

Curriculum topics follow Content Service's sibling `sortOrder` in tree preorder.
Knowledge Points are ordered by `createdAt` ascending, with UUID as a stable
tie-breaker; Content Service does not currently expose an editorial learning
order for Knowledge Points.

The public Phase 1 routes are:

- `POST /api/ai-learning/paths`
- `GET /api/ai-learning/progress`
- `GET /api/ai-learning/status`
- `GET /api/ai-learning/paths/{pathId}/map`

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

