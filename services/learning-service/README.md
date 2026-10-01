# Learning Service

Spring Boot service on port 8086, registered in Eureka as `learning-service`.
Gateway forwards `/api/learning/**` and supplies the internal JWT validated by
`common-security`. The verified JWT subject scopes all learner progress and mastery.
The module implements topic ordering, lesson reads, exercise submissions, lesson
completion and mastery reads. Review submission, test assignment and the
`AssessmentCompleted.v2` consumer are not implemented yet.

## Local runtime

Runtime configuration is in
[`learning-service.yaml`](../../infra/config-server/config-repo/learning-service.yaml).
PostgreSQL runs as Compose `learning-db` on `127.0.0.1:5436`, database `learning_db`.
Set `LEARNING_DB_PASSWORD`, `GATEWAY_INTERNAL_JWT_SECRET` and RabbitMQ credentials
in the local environment. The database password has no fallback.
Content defaults to `http://localhost:8082`; override it with `CONTENT_SERVICE_URL`.
Start Config Server, Eureka, Gateway, User and Content before using the learner APIs.

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
| POST | `/lessons/{id}/complete` | Idempotently completes a lesson with no exercise blocks; lessons with exercises return `409 LESSON_HAS_EXERCISES`. |
| GET | `/mastery` | Returns `{knowledgePointId, topicId, mastery, evidenceCount}` for catalog KPs using only the current user's evidence; makes no Content request. |

Topics derive `PASSED` from `passed_at`; the first unpassed topic is `IN_PROGRESS`
and later topics are `LOCKED`. Removed topics lose their sequence order and leave
the topic list. Lessons become available in order after earlier lessons complete.
Lesson GET, submission and completion apply errors in this order:
`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`. Topic lesson lists expose test
availability, but no route currently creates a test assignment or passes a topic.

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
further lesson access; completing it requires the review API planned for the next PR.

Learning errors use `{detail, code}`; `REVIEW_REQUIRED` additionally includes
`reviews` with `reviewId`, `lessonId`, `knowledgePointId`. Malformed requests return
422. Content reads forward the authenticated internal bearer and `X-Correlation-Id`;
missing content maps to `404 NOT_FOUND`, transport/unavailability errors to
`503 CONTENT_UNAVAILABLE`, and other Content failures to `502 CONTENT_FAILURE`.

## Structure and references

Layers follow `api → application → domain`, with HTTP and JDBC adapters under
`infrastructure`. The five domain services contain no Spring or persistence imports.
The service owns its nine tables and does not read other services' databases.

- [Internal Content contract](../../docs/contracts/learning-content-internal-v1.md)
- [Answer grading contract](../../docs/contracts/answer-spec-v1.md)
- [Assessment event contract](../../docs/contracts/assessment-completed-v2.md)
