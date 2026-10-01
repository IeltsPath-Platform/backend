# Learning Service

Spring Boot service on port 8086, registered in Eureka as `learning-service`.
Gateway forwards `/api/learning/**` and supplies the internal JWT validated by
`common-security`. The verified JWT subject scopes all learner progress and mastery.
The module implements topic ordering, lessons (exercise and essay blocks, audio),
exercise submissions, Writing essay grading (Task 1 and Task 2, LLM), mastery reads,
review sets, final-test assignment and the `AssessmentCompleted.v2` consumer.

## Local runtime

Runtime configuration is in
[`learning-service.yaml`](../../infra/config-server/config-repo/learning-service.yaml).
PostgreSQL runs as Compose `learning-db` on `127.0.0.1:5436`, database `learning_db`.
Set `LEARNING_DB_PASSWORD`, `GATEWAY_INTERNAL_JWT_SECRET` and RabbitMQ credentials
in the local environment. The database password has no fallback.
Content defaults to `http://localhost:8082`; override it with `CONTENT_SERVICE_URL`.
Access (points) defaults to `http://localhost:8084`; override it with `ACCESS_SERVICE_URL`.
Essay grading needs an OpenAI-compatible endpoint: `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`
(secret, `.env` only), `LEARNING_LLM_MODEL`, optional `LEARNING_LLM_REASONING_EFFORT`. Without them essay
submissions return `503 GRADING_UNAVAILABLE`; everything else works. `LEARNING_WRITING_POINT_COST` (3) and
`LEARNING_WRITING_DAILY_GRADING_LIMIT` (10) tune the price and the daily limit.
Start Config Server, Eureka, Gateway, User and Content (and Access for essays) before using the learner APIs.

From the repository root:

```powershell
mvn -q -pl services/learning-service -am test
mvn -pl services/learning-service spring-boot:run
```

The context test uses Testcontainers PostgreSQL and is skipped when Docker is unavailable.
Tests supply their own configuration and do not import local environment files.

## Implemented learner routes

Call through Gateway with the learner bearer token. All paths below have prefix
`/api/learning`; request and response JSON use camelCase. Start with `GET /topics`
to refresh the user's curriculum order before opening lessons. See the
[approved learner contract](../../docs/contracts/lesson-learning-v1.md) for payloads.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/topics` | One Content `topic-sequence` read refreshes the shared KP catalog and the user's topic order; returns statuses and completed lesson counts. |
| GET | `/topics/{id}/lessons` | Lists lessons and `testStatus` for an `IN_PROGRESS` or `PASSED` topic. |
| GET | `/lessons/{id}` | Applies the lesson gate and returns ordered blocks; a passed exercise block includes its solutions. |
| POST | `/lessons/{id}/exercises/{blockId}/submissions` | Grades every question in the block, saves an idempotent response and completes the lesson when all exercise blocks have passed. |
| POST | `/lessons/{id}/essays/{blockId}/submissions` | `{requestId, essayText}`: grades the essay within the request (see below) and returns the band, four criteria, corrections and summary once points are charged. |
| GET | `/writing-submissions/{id}` | Owner only; the grade appears only when `GRADED` (`PAYMENT_PENDING` shows `code`, `GRADING`/`FAILED` show `failureCode`). |
| POST | `/lessons/{id}/complete` | Idempotently completes a lesson with no exercise blocks; lessons with exercises return `409 LESSON_HAS_EXERCISES`. |
| GET | `/reviews/{id}` | Owned review only (else 404): theory of the teaching lesson and one open practice set (unused package first, otherwise the one given longest ago). No package left: `SKIPPED`. |
| POST | `/reviews/{id}/submissions` | Submits the open set once (`REVIEW_SET_CLOSED` otherwise); writes `review_set` evidence; 70% → `DONE` with solutions (and the audio transcript), third failed set → `SKIPPED`. |
| POST | `/topics/{id}/test-assignments` | `REVIEW_REQUIRED` / `TEST_LOCKED` gates, then returns the open assignment or assigns an unused test code (the least recently used one when all were used); none → `409 TEST_UNAVAILABLE`. |
| GET | `/mastery` | Returns `{knowledgePointId, topicId, mastery, evidenceCount}` for catalog KPs using only the current user's evidence; makes no Content request. |

Topics derive `PASSED` from `passed_at`; the first unpassed topic is `IN_PROGRESS`
and later topics are `LOCKED`. Removed topics lose their sequence order and leave
the topic list. Lessons become available in order after earlier lessons complete.
Lesson GET, submission and completion apply errors in this order:
`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`. Exercise blocks carry `blockKind`: essay blocks show only the prompt, task,
minimum words, pass band and images, never count toward completion and reject exercise
submissions with `409 ESSAY_BLOCK`. Media assets expose `mediaUrl`; an audio transcript
appears only once the lesson is completed.

Question objects contain only `questionVersionId`, `sortOrder`, `stem`, `options`.
`options: null` represents a fill answer. `answerSpec` and KP mappings are never
returned to learners; solutions and explanations appear only after a block passes.

## Submission, mastery and review state

Submissions require a UUID `requestId` and exactly one answer per block question.
Missing, duplicate or foreign question ids return `422 INVALID_ANSWERS`; unsupported
answer specs return `422 UNGRADABLE_EXERCISE`. A block passes at 70% correct or more.
Reuse of a request id within the same user, lesson and block returns its saved
response; reuse in another scope returns `409 REQUEST_CONFLICT`. Reattempts use a
new request id and resubmit the whole block.

Only the first submission for a user and block writes `lesson_exercise` evidence,
including wrong answers. Later attempts can pass the block without adding evidence.
Progress, evidence, submission response and reviews are committed in one transaction,
serialized by a PostgreSQL transaction-scoped advisory lock for the user.

Mastery uses the latest five outcomes ordered by `kp_evidence.ordinal`, with recency
weights `0.5, 0.7, 0.85, 0.95, 1.0`. One and two outcomes cap mastery at `0.5` and
`0.8`; no evidence yields `0`. `evidenceCount` counts all stored outcomes for the KP.

Lesson completion reevaluates reviews from the first submission of each block. A review
requires all four: mastery below `learning.review-mastery-threshold` (default `0.6`),
a wrong answer for that KP, a completed teaching lesson, and an eligible practice set
in the catalog. Existing pending reviews are not duplicated. A pending review blocks
further lesson access until it is `DONE` or `SKIPPED`.

## Writing essays

Contract: [`lesson-writing-v1`](../../docs/contracts/lesson-writing-v1.md). Order: validate (50–1,000 words,
≤ 10,000 characters) → look up `requestId` → load the lesson → check the balance (Access `GET /api/access/me/points`)
→ transaction: lesson gate and a `GRADING` row → daily limit (`llm_daily_usage`, 10 per day in `Asia/Ho_Chi_Minh`)
→ LLM, outside any transaction → grade stored as `PAYMENT_PENDING` → debit 3 points (`POST /internal/access/points/debit`,
key `lesson-writing:{userId}:{requestId}`) → transaction: `GRADED` and `lesson_writing` evidence. Calls to Access use
the learner's own bearer. A resend with the same `requestId` continues where the last one stopped, so the LLM is not
called twice for one grade and points are charged once. LLM failure charges nothing; a grading older than 120 s no
longer blocks the block.

`EssayGrader` sends the essay (and Task 1 chart facts) as tagged data, accepts exactly the task's criteria
(`TR`/`TA`, `CC`, `LR`, `GRA`) in 0–9 half bands, retries one malformed reply with low reasoning effort, trims free
text, and computes the overall band itself. Essays, prompts and LLM output are never logged. The essay block in
`GET /lessons/{id}` shows `latestSubmission` (grade only once `GRADED`) and `sampleAnswer` after the block is passed.

## Assessment results

The consumer reads queue `learning.assessment-completed.v2` (bound to `assessment.events` /
`assessment.completed.v2`), with a TTL retry queue and a DLQ. Each result version is applied in one transaction
under the user lock and acknowledged after commit: versions are idempotent per attempt and a higher version replaces
the attempt's `assessment` evidence. Equal or older versions are ignored. No learning goal or existing learning
progress is required, and the consumer makes no HTTP calls. `TOPIC_GATE` selects the latest assignment for the same
user/package version with `consumed_at IS NULL` and `assigned_at <= completed_at`, consumes it even on failure and
passes the topic at 70% (one way); `TOPIC_GATE`, `MOCK`, `OFFICIAL_PRACTICE` and `QUIZ`
reevaluate reviews; `PLACEMENT` only records its version. Contract violations and the last failed attempt go to the
DLQ with header `x-learning-failure`. Settings: `LEARNING_RETRY_DELAY_MS` (30 s), `LEARNING_MAX_DELIVERY_ATTEMPTS`
(5), `LEARNING_CONSUMER_ENABLED` (true).

Learning errors use `{detail, code}`; `REVIEW_REQUIRED` additionally includes
`reviews` with `reviewId`, `lessonId`, `knowledgePointId`. Malformed requests return
422. Content reads forward the authenticated internal bearer and `X-Correlation-Id`;
missing content maps to `404 NOT_FOUND`, transport/unavailability errors to
`503 CONTENT_UNAVAILABLE`, and other Content failures to `502 CONTENT_FAILURE`.

## Structure and references

Layers follow `api → application → domain`, with HTTP and JDBC adapters under
`infrastructure`. The domain services contain no Spring or persistence imports.
The service owns its eleven tables (Flyway V1–V2) and does not read other services' databases.

- [Internal Content contract](../../docs/contracts/learning-content-internal-v1.md)
- [Answer grading contract](../../docs/contracts/answer-spec-v1.md)
- [Assessment event contract](../../docs/contracts/assessment-completed-v2.md)
