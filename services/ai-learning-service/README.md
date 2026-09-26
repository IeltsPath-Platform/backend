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

The Content fallback order follows sibling `sortOrder` in topic-tree preorder.
Knowledge Points are ordered by `createdAt` ascending, with UUID as a stable
tie-breaker; Content Service does not currently expose an editorial learning
order for Knowledge Points. A newly created path can instead use a validated LLM
ordering, as described below.

`POST /api/ai-learning/paths` also refreshes an existing path from Content: names and metadata are refreshed, while
existing module and Knowledge Point order is preserved. New points in scope are appended to the end of their module
in Content order; new modules are appended after existing modules. Refresh does not call the LLM. The response's
`addedKnowledgePointCount` counts points added by this call (all of them when the path is created).
A refresh only adds, because DeepTutor's `replace_modules` deletes the state of any point
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
Both containers use one image; the build installs DeepTutor with its full dependency stack.
Only the API mounts `./services/ai-learning-service/deeptutor-data` at `/app/data`.
`DEEPTUTOR_HOME=/app` makes DeepTutor use that directory for configuration and usage.
The entrypoint prepares the directory and starts the API or consumer as `appuser`
(uid 10001); the consumer neither mounts the catalog nor imports the LLM layer.

Check the migration with `docker compose run --rm ai-learning-migrate info`; running
`migrate` again reports that the schema is up to date.

## LLM path ordering

When an active goal has no path yet, `POST /api/ai-learning/paths` or the first
`GET /progress` or `GET /status` can create it. After scoping the Content curriculum
to the target band, the API asks Gemini for an ordering through DeepTutor's
`deeptutor.services.llm.complete()`. The response must contain exactly the same
modules and exactly the same Knowledge Points in each module: it cannot add,
remove, duplicate, or move a point between modules. Names and learning types remain
those supplied by Content. DeepTutor still computes mastery, gates, test-out,
review scheduling, and `next_objective()`.

Ordering happens only at creation, outside the database transaction, with a total
LLM timeout of 20 seconds. DeepTutor may make one additional call to repair an
unusable JSON response within that same timeout; API-error retries are disabled.
Concurrent creation requests can each make a call, but only one path and its
ordering are committed. Later assessment results update learning state without
reordering. Placement arriving after creation still applies test-out, but does
not trigger another ordering call. Refresh preserves existing order and appends
new points as described above.

### Data sent to Gemini

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

### Configure Gemini locally

The catalog is
`services/ai-learning-service/deeptutor-data/user/settings/model_catalog.json`
on the host and `/app/data/user/settings/model_catalog.json` in the API container.
`deeptutor-data/` is git-ignored. DeepTutor writes the catalog with mode `0600`;
the bind mount's host permissions also apply. Key, model, and endpoint are configured
only in this catalog, with no additional environment variables.

From the repository root, create the catalog only if it does not already exist:

```powershell
$catalogDirectory = 'services/ai-learning-service/deeptutor-data/user/settings'
$catalogPath = Join-Path $catalogDirectory 'model_catalog.json'
New-Item -ItemType Directory -Force -Path $catalogDirectory | Out-Null
if (Test-Path -LiteralPath $catalogPath) {
    throw 'Catalog already exists. Edit the existing private catalog instead of replacing it.'
}
Copy-Item -LiteralPath 'services/ai-learning-service/model_catalog.example.json' -Destination $catalogPath
```

Edit that private file locally: replace the example `api_key`, select the Gemini
model available to the team, keep `binding: "gemini"`, and leave `base_url` empty
for DeepTutor's default Gemini endpoint. Keep `extra_headers` empty for this setup.
The model in the example file is a starting value, not a requirement.

The example sets `"reasoning_effort": "low"` on the model. Without it, DeepTutor
v1.6.9 sends `minimal` to every `gemini-3*` model, and `gemini-3.8-flash` answers
`400 Thinking level MINIMAL is not supported`. Keep the field unless the chosen
model accepts `minimal`. Google also rejects `gemini-2.5-flash` for new API keys
with a 404.

DeepTutor caches configuration for the process lifetime. After any catalog change:

```powershell
docker compose restart ai-learning-api
docker compose exec -u appuser ai-learning-api deeptutor config show
```

Always run DeepTutor commands in the container as `appuser` (`-u appuser`). A plain
`docker compose exec` runs as root. DeepTutor rewrites the catalog as a root-owned
`0600` file, which the API process (uid 10001) cannot read. The API then treats the
catalog as empty, overwrites it with an empty catalog, and reports
`llm_not_configured`: the key and model are lost from the file. Root also leaves
other files behind, such as `user/logs/deeptutor.jsonl`, and after that even
`-u appuser` commands fail with `PermissionError`. To recover:

```powershell
docker compose stop ai-learning-api
Remove-Item -Recurse -Force services/ai-learning-service/deeptutor-data
# create the catalog again as above, fill in the key and model, then:
docker compose up -d ai-learning-api
docker compose exec -u appuser ai-learning-api deeptutor config show
```

Check `llm.provider` is `gemini`, `llm.model` matches the chosen model, and
`llm.api_key` is `***`. Use only those fields when recording configuration evidence;
`extra_headers` is displayed without masking. If the CLI fails because another
DeepTutor service is unconfigured, verify the new path's `path.ordered` event
instead. A new goal/path is required to exercise ordering after configuration
changes; existing paths keep their stored order.

To disable LLM ordering, remove the `llm` profiles and clear its active profile/model
selection, or remove the catalog file, then restart `ai-learning-api`. With no
configured LLM, DeepTutor can create an empty catalog and path creation uses
Content order. Existing path order remains unchanged. The catalog is shared with
DeepTutor's `llm` service; there is no separate ordering feature flag.

DeepTutor records token usage in
`services/ai-learning-service/deeptutor-data/user/usage.sqlite3`. This ledger records
usage; it does not impose a per-learner or daily quota. Logs and ordering events
exclude keys, full prompts, and request payloads.

### Fallback and diagnostics

An LLM failure does not prevent path creation: the entire path uses Content order,
with a `path.ordered` event whose `source` is `content`.

| `reason` | Meaning |
| --- | --- |
| `llm_not_configured` | Catalog has no usable active LLM configuration |
| `llm_timeout` | The overall LLM request/JSON-repair time exceeded 20 seconds |
| `llm_error` | Provider/API failure or another LLM-layer error, including data-directory I/O |
| `llm_unusable_response` | DeepTutor could not obtain usable JSON |
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

The stub uses a dummy catalog and never calls Gemini. From the repository root,
copy it only when no catalog exists; this deliberately refuses to overwrite a
private Gemini catalog:

```powershell
$catalogDirectory = 'services/ai-learning-service/deeptutor-data/user/settings'
$catalogPath = Join-Path $catalogDirectory 'model_catalog.json'
New-Item -ItemType Directory -Force -Path $catalogDirectory | Out-Null
if (Test-Path -LiteralPath $catalogPath) {
    throw 'Catalog already exists. Use a disposable checkout/data directory for this recipe.'
}
Copy-Item -LiteralPath 'services/ai-learning-service/tests/e2e/model_catalog.stub.json' -Destination $catalogPath
$env:LLM_STUB_MODE = 'reverse'
docker compose --profile llm-stub up -d --build llm-stub ai-learning-api ai-learning-consumer
docker compose restart ai-learning-api
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
| No catalog | Path created in Content order after API restart | `source=content`, `reason=llm_not_configured` |

For the fifth case, remove only the disposable stub catalog copied above and
restart `ai-learning-api` before creating another fresh goal's path. Do not delete
an existing private catalog as part of this recipe. The API may recreate an empty
catalog during the call. Afterwards, stop the test-only stub and remove the shell
mode variable:

```powershell
docker compose --profile llm-stub stop llm-stub
Remove-Item Env:LLM_STUB_MODE -ErrorAction SilentlyContinue
```

To check runtime identity, inspect the `Uid:` and `Gid:` lines of `/proc/1/status`
in both API and consumer containers: the application process should use 10001.
An ordinary `docker compose exec ... id` checks the exec process, which can be root,
so it does not establish the application's UID. Also verify the API's application
user can write `/app/data`. Record case outcomes and aggregate test results under
`plans/260925-1547-llm-path-ordering/reports/`, without real keys, bearer tokens,
full prompts, or full payloads. This recipe documents expected behavior; execution
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

## Run tests

Use a Python virtual environment and run from `services/ai-learning-service` with
the pinned submodule on the import path:

```powershell
python -m pip install pytest -r requirements-test.txt
$env:PYTHONPATH = "../../third_party/deeptutor;."
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests
```

`requirements-test.txt` includes the service requirements plus the four packages
needed when importing DeepTutor's LLM layer from the local submodule. LLM tests
use temporary catalogs and a local OpenAI-compatible stub; they require no real
Gemini key and send no requests to Gemini.

`AI_LEARNING_TEST_DATABASE_URL` enables the PostgreSQL integration and end-to-end
tests, which each run in their own throwaway schema. `AI_LEARNING_TEST_AMQP_URL`
enables the RabbitMQ tests.

