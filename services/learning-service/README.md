# Learning Service

Spring Boot service on port 8086, registered in Eureka as `learning-service`.
Gateway forwards `/api/learning/**` and supplies the internal JWT validated by
`common-security`. The verified JWT subject scopes all learner progress and mastery.
The module implements placement-based course recommendations, independent topic paths per course, course final-test
assignments and the `COURSE_GATE` result path, lessons (exercise and essay blocks, audio),
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
`/api/learning`; request and response JSON use camelCase. Start with `GET /courses` to
show available bands and the optional placement recommendation, then group topics from
`GET /topics` by each topic's `course.courseId`. See the
[approved learner contract](../../docs/contracts/lesson-learning-v1.md) for payloads.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/courses` | CUSTOMER only; returns courses by band with placement-based `recommended`, topic counts and course-test `testStatus`. Every course is open. |
| GET | `/topics` | One Content `topic-sequence` read refreshes the shared KP catalog and the user's topic order; returns `skill`, `course`, `hasTopicTest`, statuses and completed lesson counts. Topic status sequences are independent per course and span skills. |
| POST | `/courses/{id}/test-assignments` | CUSTOMER only; assigns/returns a course final-test package after every topic in that course passes. `COURSE_GATE` at 70% passes the course without blocking another course. |
| GET | `/topics/{id}/lessons` | Lists lessons with `practiceStatus` and `practicePassReason`, plus topic `skill`, `hasTopicTest`, and `testStatus`. |
| GET | `/lessons/{id}` | Applies the lesson gate and returns ordered blocks; a passed exercise block includes its solutions. |
| POST | `/lessons/{id}/exercises/{blockId}/submissions` | Grades every question in the block, saves an idempotent response and completes the lesson when all exercise blocks have passed. |
| POST | `/lessons/{id}/essays/{blockId}/submissions` | `{requestId, essayText}`: grades the essay within the request (see below) and returns the band, four criteria, corrections and summary once points are charged. |
| GET | `/writing-submissions/{id}` | Owner only; the grade appears only when `GRADED` (`PAYMENT_PENDING` shows `code`, `GRADING`/`FAILED` show `failureCode`). |
| POST | `/lessons/{id}/complete` | Idempotently completes a lesson with no exercise blocks; lessons with exercises return `409 LESSON_HAS_EXERCISES`. |
| GET | `/lessons/{id}/practice-sets` | Lists lesson practice packages with access level, attempt and clearance status, and whether a package's answers were revealed. |
| POST | `/lessons/{id}/practice-attempts` | Starts or returns the open attempt for a lesson package after lesson completion. |
| GET | `/practice-attempts/{id}` | Returns an owned attempt's questions, or its saved result after submission. |
| POST | `/practice-attempts/{id}/submissions` | Grades every question once per request id, reveals solutions, and may record evidence and a practice review. |
| GET | `/reviews/{id}` | Owned review only (else 404): the theory blocks of its KP, then by `stage`: `PRACTICE` gives one open set of a package never given or revealed (with hints), preferring the teaching lesson, none left → `SKIPPED`; `THEORY` gives up to 3 quick-check questions instead. |
| GET | `/reviews` | Lists owned reviews oldest first; `status` defaults to `PENDING`, optional `skill` filters by skill (and includes legacy unassigned reviews), `limit` defaults to 20 and is capped at 100 (`400` above that). |
| POST | `/reviews/{id}/submissions` | Submits the open set once (`REVIEW_SET_CLOSED` otherwise, `THEORY_REQUIRED` at the theory stage); writes `review_set` evidence; always returns solutions and the audio transcript; 70% → `DONE`, a failure → `THEORY`, the second failed set → `SKIPPED`. |
| POST | `/reviews/{id}/theory-check` | Grades the quick check (`THEORY_NOT_REQUIRED` outside the theory stage), returns solutions, writes no evidence, and moves the review back to `PRACTICE`. |
| POST | `/topics/{id}/test-assignments` | `REVIEW_REQUIRED`, `PRACTICE_REQUIRED`, and `TEST_LOCKED` gates, then returns the open assignment or assigns an unused test code (the least recently used one when all were used); none → `409 TEST_UNAVAILABLE`. |
| GET | `/mastery` | Returns `{knowledgePointId, topicId, skill, mastery, evidenceCount}` for catalog KPs using only the current user's evidence; makes no Content request. |

Topics derive `PASSED` from `passed_at`; the first unpassed topic in each skill is `IN_PROGRESS`
and later topics of that skill are `LOCKED`. Topics without a skill share a separate order.
Free topics precede premium topics within each skill. A topic without a final test has
`testStatus: NONE` and passes when all published lessons are completed; requesting its
test assignment returns `409 NO_TOPIC_TEST`. Pending reviews gate only their own skill;
legacy reviews with no skill gate every skill. Removed topics lose their sequence order and leave
the topic list. Lessons become available in order after earlier lessons complete.
Lesson GET, submission and completion apply errors in this order:
`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`. Exercise blocks carry `blockKind`: essay blocks show only the prompt, task,
minimum words, pass band and images, never count toward completion and reject exercise
submissions with `409 ESSAY_BLOCK`. Media assets expose `mediaUrl`; an audio transcript
appears only once the lesson is completed.

The Writing demo data move in `V3__skill_tracks.sql` reassigns only the known W1/W2 essay submissions and KP6/KP7 reviews using their seed IDs; it is not a general-purpose migration pattern.

Question objects contain only `questionVersionId`, `sortOrder`, `stem`, `options`, `hint`.
`options: null` represents a fill answer. `answerSpec` and KP mappings are never
returned to learners; solutions and explanations appear only after a block passes.

Content V13 supplies nullable Reading hints through the internal lesson response.
Learning opens a hint only for a valid auto-gradable `FILL`, or a valid `CHOICE` with at least
three options; TFNG also works with absent/empty options. Two-choice questions and
essays receive no hint. A question that was wrong in any submission for the same
user, lesson and block keeps its hint while that block has not passed, including a
later correct answer in a failed block. New responses for a passed block have
`hint: null` for every question. Lesson GET questions and exercise POST results
always include the `hint` key, with `null` when hidden or unavailable. Review set,
quick-check and practice attempt questions carry Content hints; their results keep
`hint: null`. Final assessments do not expose hints.

## Submission, mastery and review state

Submissions require a UUID `requestId` and exactly one answer per block question.
Missing, duplicate or foreign question ids return `422 INVALID_ANSWERS`; unsupported
answer specs return `422 UNGRADABLE_EXERCISE`. A block passes at 70% correct or more.
Reuse of a request id within the same user, lesson and block returns its saved
response, including its original hints even if the block has since passed; reuse
in another scope returns `409 REQUEST_CONFLICT`. Reattempts use a new request id
and resubmit the whole block.

Only the first submission for a user and block writes `lesson_exercise` evidence,
including wrong answers. Later attempts can pass the block without adding evidence.
Hint history separately reads wrong entries from every saved `response.results`,
scoped by user and lesson and grouped by block; it needs no new Learning migration
or `hints_used` field and does not change mastery.
Progress, evidence, submission response and practice clearance are committed in one transaction,
serialized by a PostgreSQL transaction-scoped advisory lock for the user.

Mastery uses the latest five outcomes ordered by `kp_evidence.ordinal`, with recency
weights `0.5, 0.7, 0.85, 0.95, 1.0`. One and two outcomes cap mastery at `0.5` and
`0.8`; no evidence yields `0`. `evidenceCount` counts all stored outcomes for the KP.

Lesson completion no longer creates reviews. The first submission of an unrevealed
practice package writes `practice_set` evidence. A counted practice submission below
70% can create a `PRACTICE` review for KPs below 70% in that attempt, if the KP has
no pending review and an unrevealed eligible set remains. A package is revealed after
either a submitted practice attempt or a submitted review set; later practice of it
still returns solutions but writes no evidence or review. Assessment results retain
the mastery-based review rule. A pending review blocks lesson and practice access in
its skill until `DONE` or `SKIPPED`; a legacy review without a skill blocks every skill.

A pending review is at stage `PRACTICE` (one set) or `THEORY` (read the KP's TEXT
blocks, tagged in Content, then a quick check of up to 3 lesson questions). It starts
at `THEORY` when the KP scored below 40% in the practice attempt that created it or
was wrong in the first submission of the lesson's exercises; assessment reviews make
that lesson check on first open, because the event consumer does not call Content. A
failed set moves it to `THEORY`; the quick check (right or wrong, no evidence) moves it
back to `PRACTICE`; the second failed set skips it. A set never reuses a package the
learner was given in a review or submitted in practice (`ReviewRule.MAX_FAILED_REVIEW_SETS`).

The final topic test needs every lesson completed and practice-cleared. Clearance is
stored on write paths and never revoked by later package publication. Reasons, in
priority order, are `FIRST_SUBMISSION`, `REVIEW_FINISHED`, `ALL_SETS_ATTEMPTED`, and
`NO_PRACTICE`. Catalog and topic reads calculate status without writing pass rows.

## Writing essays

Lessons support `TASK_1` (Academic chart description) and `TASK_2`. Task 1 prompts include
`images[{mediaUrl, altText}]` in Content order; image URLs may be `https://` or Base64 data URIs
(`image/png`, `image/jpeg`, `image/svg+xml`). Clients render them through an image element.
Content requires exactly one essay question per essay block, nonblank `chartFacts` of 1–2,000 characters
and at least one attached `IMAGE` for Task 1; invalid blocks return `INVALID_LESSON_BLOCK`, invalid
media references return `INVALID_MEDIA_REFERENCE`. These are Content validation errors; Learning maps
Content failures as described below. Learners never receive `chartFacts` or `answerSpec`.
The seeded prompts set `minWords` to 150 for Task 1 and 250 for Task 2; submission validation accepts
50–1,000 words for either task.

Contract: [`lesson-writing-v1`](../../docs/contracts/lesson-writing-v1.md). Order: validate (50–1,000 words,
≤ 10,000 characters) → look up `requestId` → load the lesson → check the balance (Access `GET /api/access/me/points`)
→ transaction: lesson gate and a `GRADING` row → daily limit (`llm_daily_usage`, 10 per day in `Asia/Ho_Chi_Minh`)
→ LLM, outside any transaction → grade stored as `PAYMENT_PENDING` → debit 3 points (`POST /internal/access/points/debit`,
key `lesson-writing:{userId}:{requestId}`) → transaction: `GRADED` and `lesson_writing` evidence. Calls to Access use
the learner's own bearer. A resend with the same `requestId` continues where the last one stopped, so the LLM is not
called twice for one grade and points are charged once. LLM failure charges nothing; a grading older than 120 s no
longer blocks the block.

`EssayGrader` sends the essay and Task 1 `chartFacts` as tagged text; it does not send images to the LLM.
The criteria are exactly `TA`, `CC`, `LR`, `GRA` for `TASK_1` (Task Achievement against the chart facts),
and `TR`, `CC`, `LR`, `GRA` for `TASK_2` (Task Response). It accepts 0–9 half bands,
retries one malformed reply with low reasoning effort, trims free
text, and computes the overall band itself. Essays, prompts and LLM output are never logged. The essay block in
`GET /lessons/{id}` shows `latestSubmission` (grade only once `GRADED`) and `sampleAnswer` after the block is passed.

The stored `EssayPrompt` snapshot contains `questionVersionId`, `stem`, `task`, `minWords`, `passBand`,
`chartFacts`, `sampleAnswer`, `images` and `knowledgePointIds`, so retries keep the original prompt.
Writing in final topic tests remains outside the MVP pending the fee policy; essay blocks do not block lesson completion.

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
422. `REVIEW_REQUIRED` remains HTTP 403; `PRACTICE_LOCKED`, `PRACTICE_REQUIRED`
(with `lessonIds`), and `ATTEMPT_ALREADY_SUBMITTED` return HTTP 409. Content reads forward the authenticated internal bearer and `X-Correlation-Id`;
missing content maps to `404 NOT_FOUND`, transport/unavailability errors to
`503 CONTENT_UNAVAILABLE`, and other Content failures to `502 CONTENT_FAILURE`.

## Structure and references

Layers follow `api → application → domain`, with HTTP, RabbitMQ and JDBC adapters under
`infrastructure`. The domain has no Spring or persistence imports.
The service owns its eleven tables (Flyway V1–V2) and does not read other services' databases.

| Layer | Contents |
| --- | --- |
| `domain/aggregate` | `LessonProgress` (blocks passed, completion once), `LearnerCurriculum` (topic order, one-way pass; entity `TopicProgress`), `ReviewItem` (open set, DONE / SKIPPED after three failed sets; entity `ReviewSet`), `TopicTestAssignment` (consumed once, 70% passes), `WritingSubmission` (GRADING → PAYMENT_PENDING → GRADED, GRADING → FAILED → GRADING) |
| `domain/repository` | One repository per aggregate, plus `KnowledgeEvidenceRepository` (evidence and mastery history) and `KnowledgePointCatalogRepository` |
| `domain/service`, `domain/vo` | Pure rules (`MasteryCalculator`, `ReviewRule`, `AnswerSpecGrader`, `PassMark`, `PackageRotation`, …) and value objects (statuses, `KnowledgeEvidence`, `EssayPrompt`, `WritingGrade`) |
| `application/port` | Content, Access and LLM clients; `LearnerLock`, the submission logs replayed by `requestId`, `AssessmentResultLog`, `LlmUsageQuota` |
| `application/service`, `application/usecase` | Lesson gate (`LessonAccess`), review insertion, essay grading, the learner's lesson view (`LessonViewAssembler`, `WritingSubmissionViewAssembler`); one use case per action, invoked via `execute`; controllers map application results to response DTOs |

State changes go through aggregate methods; adapters only read and write rows. Writes of one learner run under
`LearnerLock`. Writing moves out of GRADING without that lock, so `WritingSubmissionRepository.save` writes only if
the row still has the status it was loaded with and returns false otherwise.

- [Internal Content contract](../../docs/contracts/learning-content-internal-v1.md)
- [Answer grading contract](../../docs/contracts/answer-spec-v1.md)
- [Assessment event contract](../../docs/contracts/assessment-completed-v2.md)
