# Learning Service

Spring Boot service on port 8086, registered in Eureka as `learning-service`.
Gateway forwards `/api/learning/**` and supplies the internal JWT validated by
`common-security`. The verified JWT subject scopes all learner progress and mastery.
The module implements placement-based course recommendations, independent topic paths per course, course final-test
assignments and the `COURSE_GATE` result path, lessons (a Markdown summary, slides and documents read
from Content, completed when the lesson's Practice passes or, without required Practice, by the learner's
confirmation), practice sets, Writing essay grading in practice (Task 1 and
Task 2, LLM), mastery reads, review sets, final-test assignment and the `AssessmentCompleted.v2` consumer.

## Local runtime

Runtime configuration is in
[`learning-service.yaml`](../../infra/config-server/config-repo/learning-service.yaml).
The root Compose stack runs PostgreSQL at host port `5440`; Learning owns the separate `learning_db` database.
For container execution, run `docker compose up -d --build`. To run Learning from the host, start the dependencies with
`docker compose up -d postgres rabbitmq`, set `LEARNING_DB_URL=jdbc:postgresql://localhost:5440/learning_db`,
`LEARNING_DB_PASSWORD` to the same local value as `POSTGRES_PASSWORD`, and provide `GATEWAY_INTERNAL_JWT_SECRET` plus
RabbitMQ credentials.
Content defaults to `http://localhost:8082`; override it with `CONTENT_SERVICE_URL`.
Access (points) defaults to `http://localhost:8084`; override it with `ACCESS_SERVICE_URL`.
Essay grading needs an OpenAI-compatible endpoint: `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`
(secret, `.env` only), `LEARNING_LLM_MODEL`, optional `LEARNING_LLM_REASONING_EFFORT`. Without them essay
submissions return `503 GRADING_UNAVAILABLE`; everything else works. `LEARNING_WRITING_POINT_COST` (3) and
`LEARNING_WRITING_DAILY_GRADING_LIMIT` (10) tune the price and the daily limit.
When running from the host, start Config Server, Eureka, Gateway, User and Content (and Access for essays) before using
the learner APIs. The full Compose stack includes these services.

From the repository root:

```powershell
mvn -q -pl services/learning-service -am test
mvn -pl services/learning-service spring-boot:run
```

The context test uses Testcontainers PostgreSQL and is skipped when Docker is unavailable.
Tests supply their own configuration and do not import local environment files.

## Implemented learner routes

Call through Gateway with the learner bearer token. All paths below have prefix
`/api/learning`; request and response JSON use camelCase. Start with `GET /courses` to
show available bands and the optional placement recommendation, then group topics from
`GET /topics` by each topic's `course.courseId`. See the
[approved learner contract](../../docs/contracts/lesson-learning-v1.md) for payloads.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/placement-test` | CUSTOMER only; returns `{packageId, packageVersionId}` of the published placement test, `409 PLACEMENT_ALREADY_DONE` once a placement exists, `404 NO_PLACEMENT_TEST` without a package. The only route open before a placement. |
| GET | `/courses` | CUSTOMER only; returns courses by band with placement-based `recommended`, topic counts and course-test `testStatus`. The placement test is optional: every course is open with or without a placement, which only sets `recommended`. |
| GET | `/topics` | One Content `topic-sequence` read refreshes the shared KP catalog and the user's topic order; returns `skill`, `course`, `hasTopicTest`, statuses and completed lesson counts. Topic status sequences are independent per course and span skills. |
| POST | `/courses/{id}/test-assignments` | CUSTOMER only; assigns/returns a course final-test package after every topic in that course passes. `COURSE_GATE` at 70% passes the course without blocking another course. |
| GET | `/topics/{id}/lessons` | Lists lessons with `practiceStatus` and `practicePassReason`, plus topic `skill`, `hasTopicTest`, and `testStatus`. |
| GET | `/lessons/{id}` | Applies the lesson gate and returns the lesson: `{lessonId, topicId, code, title, summary, sortOrder, status, skill, skills}` with `status` `AVAILABLE` or `COMPLETED`. Its content is read from `/lessons/{id}/resources`. |
| GET | `/lessons/{id}/resources` | Applies the same lesson gate and returns `{lessonId, summaryMarkdown, slides[], documents[]}` from Content; `summaryMarkdown` is `null` when the lesson has none, each link is `{id, title, url, sortOrder}`. Does not start or change lesson progress. |
| GET | `/writing-submissions/{id}` | Owner only; the grade appears only when `GRADED` (`PAYMENT_PENDING` shows `code`, `GRADING`/`FAILED` show `failureCode`). Reads practice essays and essays graded while lessons still had essay blocks. |
| POST | `/lessons/{id}/complete` | Idempotently completes a lesson that has no required Practice (`NO_PRACTICE`), which opens the next lesson. A lesson with required Practice completes only when its Practice passes, so confirming it earlier returns `409 PRACTICE_REQUIRED` with `lessonIds: [lessonId]`. |
| GET | `/lessons/{id}/practice-sets` | Lists lesson practice packages with access level, attempt and clearance status, and whether a package's answers were revealed. |
| POST | `/lessons/{id}/practice-attempts` | Starts or returns the open attempt for a lesson package. The Practice opens with its lesson and follows the lesson gate. |
| GET | `/practice-attempts/{id}` | Returns an owned attempt's questions, or its saved result after submission. Owner check only, so a learner can always read their own attempts. |
| POST | `/practice-attempts/{id}/submissions` | Grades every question once per request id, reveals solutions, and may record evidence and a practice review. |
| GET | `/reviews/{id}` | Owned review only (else 404): the theory of the review is the Markdown summary of its lesson (`theory`, scope `LESSON`), then by `stage`: `PRACTICE` gives one open set of a package never given or revealed (with hints), preferring the teaching lesson, none left → `SKIPPED`; `THEORY` gives no set and no quick check (`quickCheck` is always empty). |
| GET | `/reviews` | Lists owned reviews oldest first; `status` defaults to `PENDING`, optional `skill` filters by skill (and includes legacy unassigned reviews), `limit` defaults to 20 and is capped at 100 (`400` above that). Each review carries `sourcePackageId`, the Practice set whose attempt created it (`null` for reviews from assessments). |
| POST | `/reviews/{id}/submissions` | Submits the open set once (`REVIEW_SET_CLOSED` otherwise, `THEORY_REQUIRED` at the theory stage); writes `review_set` evidence; always returns solutions and the audio transcript; 70% → `DONE`; the first failed set → `THEORY`, later failed sets go straight to a new set (`PRACTICE`), until one passes or no unrevealed package is left. |
| POST | `/reviews/{id}/theory-check` | Confirms the theory was read: `answers` must be empty (`422 INVALID_ANSWERS` otherwise, `409 THEORY_NOT_REQUIRED` outside the theory stage). Writes no evidence and moves the review back to `PRACTICE`; a replayed `requestId` returns the stored result. |
| POST | `/topics/{id}/test-assignments` | `REVIEW_REQUIRED`, `PRACTICE_REQUIRED`, and `TEST_LOCKED` gates, then returns the open assignment or assigns an unused test code (the least recently used one when all were used); none → `409 TEST_UNAVAILABLE`. |
| GET | `/mastery` | Returns `{knowledgePointId, topicId, skill, mastery, evidenceCount}` for catalog KPs using only the current user's evidence; makes no Content request. |

Topics derive `PASSED` from `passed_at`; the first unpassed topic in each skill is `IN_PROGRESS`
and later topics of that skill are `LOCKED`. Topics without a skill share a separate order.
Free topics precede premium topics within each skill. A topic without a final test has
`testStatus: NONE` and passes when all published lessons are completed; requesting its
test assignment returns `409 NO_TOPIC_TEST`. Pending reviews gate only their own skill;
legacy reviews with no skill gate every skill. Removed topics lose their sequence order and leave
the topic list. Lessons become available in order after earlier lessons complete. A lesson with required
Practice (every published set of Reading or Listening, or without a skill; Writing sets and sets that need a paid
feature are optional) completes when that Practice passes, so the next
lesson opens only after the Practice of the lesson before it. A lesson without required Practice completes by
confirmation.
Lesson GET, resources, completion and Practice apply errors in this order:
`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`.

Lessons no longer have exercise or essay blocks (Content V30). Tables written while they did stay as history and are
no longer read or written: `lesson_exercise_submissions`, `lesson_progress.passed_block_ids`, and the
`LESSON_BLOCK` rows of `lesson_writing_submissions`, which `GET /writing-submissions/{id}` still returns.
`kp_evidence` rows from lesson exercises (`lesson_exercise`, `lesson_writing`) also stay and still count toward mastery.

Question objects contain only `questionVersionId`, `sortOrder`, `stem`, `options`, `hint`.
`options: null` represents a fill answer. `answerSpec` and KP mappings are never
returned to learners; solutions and explanations appear only after a set is submitted.
Review set and practice attempt questions carry Content hints; their results keep `hint: null`.
Final assessments do not expose hints.

## Submission, mastery and review state

Practice and review submissions require a UUID `requestId` and exactly one answer per question. Missing, duplicate or
foreign question ids return `422 INVALID_ANSWERS`; unsupported answer specs return `422 UNGRADABLE_EXERCISE`. Reuse of
a request id within the same user and attempt (or review set) returns its saved response; reuse in another scope
returns `409 REQUEST_CONFLICT`. Progress, evidence, submission response and practice clearance are committed in one
transaction, serialized by a PostgreSQL transaction-scoped advisory lock for the user.

Mastery uses the latest five outcomes ordered by `kp_evidence.ordinal`, with recency
weights `0.5, 0.7, 0.85, 0.95, 1.0`. One and two outcomes cap mastery at `0.5` and
`0.8`; no evidence yields `0`. `evidenceCount` counts all stored outcomes for the KP.

Completing a lesson creates no review and writes no evidence. The first submission of an unrevealed
practice package writes `practice_set` evidence. A counted practice submission below
70% can create a `PRACTICE` review for KPs below 70% in that attempt, if the KP has
no pending review and an unrevealed eligible set remains. A package is revealed after
either a submitted practice attempt or a submitted review set; later practice of it
still returns solutions but writes no evidence or review. Assessment results retain
the mastery-based review rule. A pending review blocks the lessons of its skill and their Practice until
`DONE` or `SKIPPED`; a legacy review without a skill blocks every skill. The lesson that created the review stays
open to read (lesson GET, resources, completion), but its own Practice also waits for the review. In
`GET /topics/{id}/lessons` such a lesson stays `AVAILABLE`, while the lessons held back by it are `LOCKED` with
`blockingReviewId` set.

A pending review is at stage `PRACTICE` (one set) or `THEORY` (read the Markdown summary of the lesson that teaches
the KP, then confirm through `theory-check`). It starts at `THEORY` when the KP scored below 40% in the practice
attempt that created it; reviews from assessment results start at `PRACTICE`. The first failed set moves it to
`THEORY`; the confirmation (no evidence) moves it back to `PRACTICE`; later failed sets take a new set without
reading the theory again, with no limit on failures. A set never reuses a package the learner was given in a review
or submitted in practice, so a review ends as `SKIPPED` only when no such package is left. `maxFailedSets` in a review
response is `null`.
Reviews created before Content V30 may carry the theory reason `WRONG_IN_LESSON`; it is no longer assigned.

The final topic test needs every lesson completed and practice-cleared. Clearance is
stored on write paths and never revoked by later package publication. Reasons, in
priority order, are `FIRST_SUBMISSION`, `REVIEW_FINISHED`, `ALL_SETS_ATTEMPTED`, and
`NO_PRACTICE`. The Practice is cleared once **every required set has been submitted at least once** (a package a review
used also counts as seen) and **no review of the lesson is pending**: `FIRST_SUBMISSION` when every required set passed
at its first submission, `REVIEW_FINISHED` when reviews were needed and all are `DONE` or `SKIPPED`, else
`ALL_SETS_ATTEMPTED`. Passing one set of several does not clear it. A lesson with no required set is `NO_PRACTICE` even
before it opens; otherwise its Practice is `LOCKED` until the lesson opens, then `REQUIRED` until cleared. Clearing it
completes the lesson. Catalog and topic reads calculate status
without writing pass rows.

## Writing essays

Practice sets can hold `TASK_1` (Academic chart description) and `TASK_2` essay items; an essay is submitted through
its practice attempt and graded within the request. Package items carry no chart images, so Task 1 prompts have an
empty `images` list. Learners never receive `chartFacts` or `answerSpec`. Submission validation accepts 50–1,000 words
for either task; `minWords` of the item only guides the learner.

Contract: [`lesson-writing-v1`](../../docs/contracts/lesson-writing-v1.md). Order: validate (50–1,000 words,
≤ 10,000 characters) → look up `requestId` → load the practice attempt → check the balance (Access
`GET /api/access/me/points`) → transaction: lesson gate and a `GRADING` row → daily limit (`llm_daily_usage`, 10 per day
in `Asia/Ho_Chi_Minh`) → LLM, outside any transaction → grade stored as `PAYMENT_PENDING` → debit 3 points
(`POST /internal/access/points/debit`) → transaction: `GRADED`. Calls to Access use the learner's own bearer. A resend
with the same `requestId` continues where the last one stopped, so the LLM is not called twice for one grade and points
are charged once. LLM failure charges nothing; a grading older than 120 s no longer blocks the item.

`EssayGrader` sends the essay and Task 1 `chartFacts` as tagged text. The criteria are exactly `TA`, `CC`, `LR`, `GRA`
for `TASK_1` (Task Achievement against the chart facts), and `TR`, `CC`, `LR`, `GRA` for `TASK_2` (Task Response). It
accepts 0–9 half bands, retries one malformed reply with low reasoning effort, trims free text, and computes the overall
band itself. Essays, prompts and LLM output are never logged.

The stored `EssayPrompt` snapshot contains `questionVersionId`, `stem`, `task`, `minWords`, `passBand`, `chartFacts`,
`sampleAnswer`, `images` and `knowledgePointIds`, so retries keep the original prompt.
Writing in final topic tests remains outside the MVP pending the fee policy.

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
`reviews` with `reviewId`, `lessonId`, `knowledgePointId`, and `skill` when known. Malformed requests return
422. `REVIEW_REQUIRED` remains HTTP 403; `PRACTICE_REQUIRED`
(with `lessonIds`) and `ATTEMPT_ALREADY_SUBMITTED` return HTTP 409. Content reads forward the authenticated internal bearer and `X-Correlation-Id`;
missing content maps to `404 NOT_FOUND`, transport/unavailability errors to
`503 CONTENT_UNAVAILABLE`, and other Content failures to `502 CONTENT_FAILURE`.

## Structure and references

Layers follow `api → application → domain`, with HTTP, RabbitMQ and JDBC adapters under
`infrastructure`. The domain has no Spring or persistence imports.
The service owns its eleven tables (Flyway V1–V2) and does not read other services' databases.

| Layer | Contents |
| --- | --- |
| `domain/aggregate` | `LessonProgress` (placement and completion, once), `LearnerCurriculum` (topic order, one-way pass; entity `TopicProgress`), `ReviewItem` (open set, DONE / SKIPPED after three failed sets; entity `ReviewSet`), `TopicTestAssignment` (consumed once, 70% passes), `WritingSubmission` (GRADING → PAYMENT_PENDING → GRADED, GRADING → FAILED → GRADING) |
| `domain/repository` | One repository per aggregate, plus `KnowledgeEvidenceRepository` (evidence and mastery history) and `KnowledgePointCatalogRepository` |
| `domain/service`, `domain/vo` | Pure rules (`MasteryCalculator`, `ReviewRule`, `AnswerSpecGrader`, `PassMark`, `PackageRotation`, …) and value objects (statuses, `KnowledgeEvidence`, `EssayPrompt`, `WritingGrade`) |
| `application/port` | Content, Access and LLM clients; `LearnerLock`, the submission logs replayed by `requestId`, `AssessmentResultLog`, `LlmUsageQuota` |
| `application/service`, `application/usecase` | Lesson gate (`LessonAccess`), review insertion, essay grading, the learner's essay view (`WritingSubmissionViewAssembler`); one use case per action, invoked via `execute`; controllers map application results to response DTOs |

State changes go through aggregate methods; adapters only read and write rows. Writes of one learner run under
`LearnerLock`. Writing moves out of GRADING without that lock, so `WritingSubmissionRepository.save` writes only if
the row still has the status it was loaded with and returns false otherwise.

- [Internal Content contract](../../docs/contracts/learning-content-internal-v1.md)
- [Answer grading contract](../../docs/contracts/answer-spec-v1.md)
- [Assessment event contract](../../docs/contracts/assessment-completed-v2.md)
