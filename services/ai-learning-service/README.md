# `ai-learning-service`

Service chuyên trách **Adaptive Learning** cho nền tảng IELTSPath.

## Tech Stack
- Python 3.11+
- FastAPI
- Mastery engine in `app/mastery` (ported from DeepTutor v1.6.9, see below)
- PostgreSQL (`ai_learning_db`)

## Phase 1 scope
- Goal-bound Mastery Path bootstrap from User Service and Content Service.
- Learner adaptive state persisted in PostgreSQL `mastery_paths.state_json`.
- Safe progress, status, and path-map reads derived through the mastery engine's policy.
- Internal JWT authentication using only the validated Authorization bearer token.
- Formal Assessment feedback: an `AssessmentCompleted.v2` RabbitMQ consumer applies
  finalized, pre-graded results through the mastery engine (see
  `docs/contracts/assessment-completed-v2.md`).

Tutor sessions, question-level review APIs, and mistake practice are outside this phase.

## Mastery engine

`app/mastery` is the adaptive engine: models, mastery scoring, gates, `next_objective()`,
spaced repetition, grading, and the path operations of `LearningService`. It is a port of
DeepTutor v1.6.9's `learning` package (Apache-2.0, commit `da856ad`) with the same
behavior, thresholds, and stored `state_json` shape, trimmed to what the service uses.
Each ported file names its source; see `NOTICE` and `licenses/DeepTutor-LICENSE.txt`.

The service does **not** depend on DeepTutor at runtime, build, or test time. The
DeepTutor clone in this repository is a reference to read while porting (see the root
README); `tests/test_no_deeptutor_dependency.py` fails if anything imports `deeptutor` or if the
image, requirements, or Compose file refer to the clone. The engine tests
(`tests/test_mastery_*.py`) are DeepTutor's own tests adapted to `app.mastery`, with
their expected values kept verbatim.

## Build the service image

Build from the repository root:

```powershell
docker build -f services/ai-learning-service/Dockerfile -t ieltspath-ai-learning .
```

The image installs only `requirements.txt` and the service code. The service
configuration uses `AI_LEARNING_INTERNAL_JWT_SECRET`,
`AI_LEARNING_DATABASE_URL`, `AI_LEARNING_USER_SERVICE_BASE_URL`, and
`AI_LEARNING_CONTENT_SERVICE_BASE_URL`, plus the optional `AI_LEARNING_LLM_*`
settings described under [LLM path ordering](#llm-path-ordering). The JWT secret must
be Base64 encoded and decode to at least 32 bytes. Supply secret values through
runtime configuration; do not put them in this file or the image.

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

`mastery_interactions.status` accepts the lowercase values the engine writes
(`registered`, `awaiting_input`, `answered`, `graded`, `abandoned`), not the uppercase
names in `DATABASE_V5.md`. `interaction_id` is a UUID as in V5; Phase 1 never inserts
interactions, so the question-id format is checked when Tutor Chat is built.

A database where `V1` and `V2` were applied by hand has no Flyway history table, so
`flyway migrate` reports a non-empty schema. Baseline it once at the last applied
version, then migrate normally:

```powershell
flyway -baselineOnMigrate=true -baselineVersion=2 migrate
```

The PostgreSQL tests build each schema by running this same migration chain
(`tests/postgres_schema_support.py`); there is no hand-written DDL in the tests.
`AI_LEARNING_TEST_DATABASE_URL` points them at a disposable database.

The engine uses a synchronous `LearningStore` contract (`app/mastery/store.py`). Its
PostgreSQL adapter locks one aggregate row with `SELECT ... FOR UPDATE`; nested
engine transactions for bootstrap join one PostgreSQL transaction so path
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
is still decided only by the engine's `next_objective()`. An existing path is returned as is, without reading
Content again.

A finalized `PLACEMENT` result tests out the points the learner already masters, in the same transaction as its
evidence: points whose effective `bandMax` is not above the placement's `overall_band`, and points every placement
item answered correctly (or judged PASS). Test-out is a learner mastery override, so `next_objective()`
skips the point; the placement evidence is still recorded and no mastery score, gate, policy or scheduler changes.
The override model keeps DeepTutor v1.6.9's shape so stored paths stay readable, so provenance is kept in the override note
(`placement:{attemptId}:v{version}`) and `/progress` and `/map` report `masterySource: "placement"` for it
(`system` = cleared by evidence, `learner` = the learner's own claim). A regraded placement replaces that attempt's
test-out; a learner's own override is never replaced or cleared. Tested-out points have no repetition state, so
they are not scheduled for review.

The Content fallback order follows sibling `sortOrder` in topic-tree preorder.
Knowledge Points are ordered by `createdAt` ascending, with UUID as a stable
tie-breaker; Content Service does not currently expose an editorial learning
order for Knowledge Points. A newly created path can instead use a validated LLM
ordering, as described below.

`POST /api/ai-learning/paths` also refreshes an existing path from Content: names and metadata are refreshed, while
existing module and Knowledge Point order is preserved. New points in scope are appended to the end of their module
in Content order; new modules are appended after existing modules. Refresh does not call the LLM. The response's
`addedKnowledgePointCount` counts points added by this call (all of them when the path is created).
A refresh only adds, because the engine's `replace_modules` deletes the state of any point
missing from the new module set. A point that left the curriculum (inactive, deleted, or now above the target band)
stays in the path with its history and is retired with a `retired:content` override, so `next_objective()` skips it
and `masterySource` reports `retired`; it is restored if it comes back in scope. The engine's summary counts a retired
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
| `ai-learning-api` | `uvicorn main:app` on `127.0.0.1:8000`, starts after the migration | `AI_LEARNING_INTERNAL_JWT_SECRET` (from `GATEWAY_INTERNAL_JWT_SECRET`), `AI_LEARNING_DATABASE_URL`, `AI_LEARNING_USER_SERVICE_BASE_URL`, `AI_LEARNING_CONTENT_SERVICE_BASE_URL`, `AI_LEARNING_LLM_*` |
| `ai-learning-consumer` | `python -m app.messaging.assessment_consumer`, restarted if it exits | `AI_LEARNING_DATABASE_URL`, `AI_LEARNING_AMQP_URL` only (no JWT secret) |

The API forwards the learner's internal JWT straight to User (`8085`) and Content (`8082`),
not through the Gateway, so both base URLs default to `http://host.docker.internal:<port>`.
Both containers use one image and mount no volumes. The entrypoint starts the API or
consumer as `appuser` (uid 10001); the consumer receives no LLM settings.

Check the migration with `docker compose run --rm ai-learning-migrate info`; running
`migrate` again reports that the schema is up to date.

## LLM path ordering

When an active goal has no path yet, `POST /api/ai-learning/paths` or the first
`GET /progress` or `GET /status` can create it. After scoping the Content curriculum
to the target band, the API asks the configured LLM (Gemini by default) for an
ordering through an OpenAI-compatible chat completions call (`app/llm/client.py`).
The response must contain exactly the same modules and exactly the same Knowledge
Points in each module: it cannot add, remove, duplicate, or move a point between
modules. Names and learning types remain those supplied by Content. The mastery
engine still computes mastery, gates, test-out, review scheduling, and
`next_objective()`.

Ordering happens only at creation, outside the database transaction, with a total
LLM timeout of 20 seconds. If the first response is not usable JSON, the client asks
once more with reasoning effort `low` within that same timeout; HTTP errors are not
retried. Concurrent creation requests can each make a call, but only one path and its
ordering are committed. Later assessment results update learning state without
reordering. Placement arriving after creation still applies test-out, but does
not trigger another ordering call. Refresh preserves existing order and appends
new points as described above.

### Data sent to the LLM

| Sent | Detail |
| --- | --- |
| Goal context | Target band, days until the exam (not the exam date), available minutes per day |
| Placement summary | Latest available overall placement band and each in-scope point's `correct`, `incorrect`, or `not_tested` result |
| Content metadata | Module/topic UUID, name and parent UUID; Knowledge Point UUID, name, learning type, skill, description limited to 200 characters, effective band minimum/maximum |

Learner name, email, user UUID, goal UUID, authentication tokens, assessment
answers, and the full assessment payload are not sent. Topic and Knowledge Point
UUIDs identify curriculum entries, not the learner. If no placement is available
at creation, ordering uses the goal and curriculum alone. Requests above 300
Knowledge Points or 60,000 payload characters use Content order without calling
the LLM.

### Configure the LLM

The API reads these environment variables on every ordering call, so a changed key
or model applies after `docker compose up -d ai-learning-api` without other steps.
Put the key in the root `.env` (git-ignored), never in a tracked file.

| Variable | Default (Compose) | Meaning |
| --- | --- | --- |
| `AI_LEARNING_LLM_API_KEY` | empty | Provider key. Empty disables LLM ordering |
| `AI_LEARNING_LLM_MODEL` | `gemini-3.8-flash` | Model id sent to the provider |
| `AI_LEARNING_LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai/` | OpenAI-compatible endpoint; `/chat/completions` is appended |
| `AI_LEARNING_LLM_REASONING_EFFORT` | `low` | Sent as `reasoning_effort`. Empty uses the model family default |
| `AI_LEARNING_LLM_TIMEOUT_SECONDS` | `20` | Total budget for both attempts |

With `AI_LEARNING_LLM_REASONING_EFFORT` empty, the client follows DeepTutor v1.6.9's
rule: Gemini 2.5 models get `none` and Gemini 3 models get `minimal`, to stop them
spending the whole token budget on thinking. `gemini-3.8-flash` answers
`400 Thinking level MINIMAL is not supported`, which is why Compose defaults to `low`;
keep it unless the chosen model accepts `minimal`. Google also rejects
`gemini-2.5-flash` for new API keys with a 404.

A new goal/path is required to exercise ordering after configuration changes;
existing paths keep their stored order. To disable LLM ordering, leave
`AI_LEARNING_LLM_API_KEY` empty and recreate `ai-learning-api`: path creation then uses
Content order with `reason=llm_not_configured`. There is no separate ordering feature
flag, and no per-learner or daily quota. Logs and ordering events exclude keys, full
prompts, and request payloads.

### Fallback and diagnostics

An LLM failure does not prevent path creation: the entire path uses Content order,
with a `path.ordered` event whose `source` is `content`.

| `reason` | Meaning |
| --- | --- |
| `llm_not_configured` | No model or no API key is set |
| `llm_timeout` | Both attempts together exceeded the 20-second budget |
| `llm_error` | Provider/API failure, network failure, or invalid LLM settings |
| `llm_unusable_response` | Neither attempt returned usable JSON |
| `payload_too_large` | More than 300 points or 60,000 payload characters; LLM skipped |
| `invalid_ordering` | JSON did not preserve the required module/point permutation |

For `invalid_ordering`, `detail` is one of `malformed`, `missing_module`,
`unknown_module`, `duplicate_module`, `missing_knowledge_point`,
`unknown_knowledge_point`, `duplicate_knowledge_point`, or `moved_knowledge_point`.
Success records `source=llm` with no reason. Events also contain model, module and
point counts, and a rationale limited to 500 characters; rationale is not a learner
response field. Database or upstream Content/User failures retain their existing
error handling.

Inspect a new path's diagnostic fields in the local AI Learning database, replacing
the path placeholder with the UUID returned by `POST /api/ai-learning/paths`:

```sql
SELECT payload_json ->> 'source' AS source,
       payload_json ->> 'reason' AS reason,
       payload_json ->> 'detail' AS detail,
       payload_json ->> 'model' AS model
FROM mastery_events
WHERE path_id = '<path UUID>'::uuid AND event_type = 'path.ordered'
ORDER BY id DESC;
```

### Five-case stub E2E recipe

Use a disposable local setup with the root README's Gateway, User, Content,
Assessment, PostgreSQL and RabbitMQ prerequisites running. Prepare at least two
learners, an active goal for each scenario, and scoped Content with at least two
modules and multiple points. For `reverse`, finalize a placement result before
creating the path so the result is parked for that goal. Each scenario needs a
fresh learner/goal pair with no existing path; even `GET /status` or `/progress`
can create one. Calls below go through the Gateway at `http://localhost:8080` using
the scenario learner's normal authentication flow.

The stub never calls Gemini. Point the API at it with a dummy key, from the
repository root:

```powershell
$env:AI_LEARNING_LLM_BASE_URL = 'http://llm-stub:8090/v1beta/openai/'
$env:AI_LEARNING_LLM_MODEL = 'stub-model'
$env:AI_LEARNING_LLM_API_KEY = 'stub-key'
$env:LLM_STUB_MODE = 'reverse'
docker compose --profile llm-stub up -d --build llm-stub ai-learning-api ai-learning-consumer
```

For each of the first four cases, set the mode and recreate the stub so Compose
passes the new environment value:

```powershell
$env:LLM_STUB_MODE = 'unknown_kp' # Use reverse, unknown_kp, slow, or error for the current case.
docker compose --profile llm-stub up -d --force-recreate llm-stub
```

Create the fresh goal's path with `POST /api/ai-learning/paths`, then read
`GET /api/ai-learning/paths/{pathId}/map` and `GET /api/ai-learning/status`. Check
`path.ordered` in the database using the query above.

| Case | Expected path and status | Expected event |
| --- | --- | --- |
| `reverse`, with placement | Module and point order reversed from Content; status follows the first eligible point in that order; placement test-out still skips mastered points | `source=llm` |
| `unknown_kp` | Path created in Content order | `source=content`, `reason=invalid_ordering`, `detail=unknown_knowledge_point` |
| `slow` | Path created in Content order after about 20 seconds of LLM wait (stub waits 30 seconds) | `source=content`, `reason=llm_timeout` |
| `error` | Path created in Content order after the stub's HTTP 500 response | `source=content`, `reason=llm_error` |
| No key | Path created in Content order after recreating the API without a key | `source=content`, `reason=llm_not_configured` |

For the fifth case, clear the key and recreate the API before creating another
fresh goal's path. Afterwards, stop the test-only stub and remove the shell
variables:

```powershell
$env:AI_LEARNING_LLM_API_KEY = ''
docker compose up -d ai-learning-api
# ...run the fifth case, then:
docker compose --profile llm-stub stop llm-stub
Remove-Item Env:LLM_STUB_MODE, Env:AI_LEARNING_LLM_BASE_URL, Env:AI_LEARNING_LLM_MODEL, Env:AI_LEARNING_LLM_API_KEY -ErrorAction SilentlyContinue
```

To check runtime identity, inspect the `Uid:` and `Gid:` lines of `/proc/1/status`
in both API and consumer containers: the application process should use 10001.
An ordinary `docker compose exec ... id` checks the exec process, which can be root,
so it does not establish the application's UID. Record case outcomes and aggregate
test results in the plan's `reports/` folder, without real keys, bearer tokens, full
prompts, or full payloads. This recipe documents expected behavior; execution
results belong in the verification report.

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

The engine, like DeepTutor v1.6.9, has no entry point for already-graded results.
`app/learning/external_assessment.py` subclasses `LearningService` and replays the
post-grade steps DeepTutor runs after grading, calling the engine's own methods for
every adaptive computation:

- `record_quiz_attempt`
- `_record_quiz_evidence`
- `calculate_mastery` and `update_mastery`
- `SpacedRepetitionScheduler`
- `record_qualitative_in_memory`
- `scheduler.replay` for regrades

The consumer never creates a learning path, because creating one needs the
learner's curriculum token. An event for a goal that has no path yet is retried,
then parked in the dead-letter queue.

## Run tests

Use a Python virtual environment and run from `services/ai-learning-service`:

```powershell
python -m pip install pytest -r requirements-test.txt
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests
```

`python -m pytest` puts the service directory on the import path; no other path is
needed. LLM tests set `AI_LEARNING_LLM_*` for the duration of each test and use a
local OpenAI-compatible stub; they require no real Gemini key and send no requests
to Gemini.

`AI_LEARNING_TEST_DATABASE_URL` enables the PostgreSQL integration and end-to-end
tests, which each run in their own throwaway schema. `AI_LEARNING_TEST_AMQP_URL`
enables the RabbitMQ tests.

