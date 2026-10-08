# Lesson learning API v1 — proposed contract

**Status: approved 2026-10-01.** This document describes the intended learner HTTP API; it does not claim it is deployed. Examples use actual lesson/question/package codes and text from [`seed-content.md`](../../plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md). The existing V4 UUIDs for DEMO_READING and KP1 are real; other UUIDs and timestamps illustrate relationships because the V9 seed leaves their assignment to the migration.

**Implementation status (2026-10-07):** Java Learning Service implements every route below, including course listing, placement-based course recommendations, course-final-test assignment and the `COURSE_GATE` consumer path. Learning Service consumes `AssessmentCompleted.v2` to apply assignments and pass topics/courses; Assessment auto-grades objective final tests.

**Reading hints (2026-10-02):** lesson exercise questions and submission results add an always-present nullable `hint` field, with the policy below. Review set and quick-check questions carry the question's `hint` (2026-10-03); review results keep `hint: null`; final assessments are unchanged.

The MVP app displays course, topic and lesson status. The Java Learning Service owns learner placements, course/topic progress, mastery evidence, reviews and test assignments. The placement test is optional: every course is open with or without it, and a placement only recommends a course without changing topic state. Topics form one sequence per course, shared across skills. Reviews still gate work by skill. A course-final-test assignment uses a package code once before rotating; passing at 70% marks the course complete without blocking any topic. Topic order, course id and skill are stored in `topic_progress`; topic and course pass timestamps are one-way. Tutor, practice notebook, learner memory, learning goals and LLM ordering are removed.

## Learner routes

All routes in this section have prefix `/api/learning`, require the learner bearer token through Gateway, and are scoped to the verified user id. Response JSON uses camelCase. Content reads forward the bearer and `X-Correlation-Id`; a Content `404` becomes `404 NOT_FOUND`, and other dependency failures remain `502`/`503`. Lists are ordered by the Content `sortOrder` and stable ids. Learners cannot call Content's internal API or list packages/questions through public `/api/content` routes.

### `GET /topics`

Refreshes the shared `knowledge_point_catalog` and the user's topic order from one Content `topic-sequence` read. Returns `[{topicId, code, name, sequenceOrder, status, completedLessonCount, accessLevel, skill, hasTopicTest, course}]`, where `course` is `{courseId, code, name, bandLevel}`. `accessLevel` is `FREE`, or `PREMIUM` when Content requires a paid feature for the topic (`requiredFeatureKey`); Learning does not check the learner's plan yet. Topics are ordered by course band, free before premium, topic `sortOrder`, then `topicId`. Within each course, a topic is `PASSED` when `passed_at` exists; otherwise the first unpassed topic is `IN_PROGRESS` and later topics are `LOCKED`, regardless of skill. Topics whose course is not yet known share a separate null-course group. Removed topics have no sequence order and are omitted.

```json
[
  {"topicId":"10000000-0000-4000-8000-000000000001","code":"DEMO_READING","name":"Demo IELTS Reading","sequenceOrder":1,"status":"IN_PROGRESS","completedLessonCount":0,"accessLevel":"FREE","skill":"READING","hasTopicTest":true,"course":{"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5}},
  {"topicId":"20000000-0000-4000-8000-000000000002","code":"TFNG_SKILLS","name":"True / False / Not Given","sequenceOrder":2,"status":"LOCKED","completedLessonCount":0,"accessLevel":"FREE","skill":"READING","hasTopicTest":true,"course":{"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5}},
  {"topicId":"24000000-0000-4000-8000-010000000001","code":"PREMIUM_MATCHING_INFO","name":"Matching Information","sequenceOrder":4,"status":"LOCKED","completedLessonCount":0,"accessLevel":"PREMIUM","skill":"READING","hasTopicTest":true,"course":{"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5}}
]
```

### `GET /courses`

Available only to role `CUSTOMER`. Returns courses ordered by `bandLevel` as `[{courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt}]`. A recommendation uses the learner's latest non-null `overall_band` from a `PLACEMENT` result: the lowest course band at or above it, or the highest course if no band is high enough. Without a placement, no course is recommended. Placement never changes topic status. `testStatus` is `NONE` when the course has no published final test, `LOCKED` while a course topic remains unpassed, `AVAILABLE` once all topics pass, and `PASSED` after the course test reaches 70%.

### `GET /placement-test`

Available only to role `CUSTOMER`. Returns `{packageId, packageVersionId}` of the published `PLACEMENT_TEST` package. The learner then starts an Assessment attempt with that `packageVersionId` (`POST /api/assessments/attempts`, type `PLACEMENT`), sends the Writing essay and the Speaking recording through `POST /api/assessments/submissions`, and submits the attempt. A learner can sit the test once: when a placement exists the call returns `409 PLACEMENT_ALREADY_DONE`; without a published package it returns `404 NO_PLACEMENT_TEST`.

No learning route requires a placement. The placement is stored when Assessment completes the attempt; poll `GET /api/assessments/attempts/{id}/placement-result` until it stops returning `404`, then `GET /courses` marks the recommended course.

### `POST /courses/{id}/test-assignments`

Available only to role `CUSTOMER`; request body is empty. Requires all topics in the course to be `PASSED`, else `403 COURSE_TEST_LOCKED`. A course without a published final test returns `409 NO_COURSE_TEST`; an already passed course returns `409 COURSE_ALREADY_PASSED`; a course id not in the active sequence returns `404 COURSE_NOT_FOUND`. A pending assignment is returned unchanged; otherwise Learning assigns the least recently used published package. No package available returns `409 TEST_UNAVAILABLE`. Response: `{assignmentId, packageId, packageVersionId}`. Start an Assessment attempt with the returned package version; Assessment emits `COURSE_GATE`, and a result at or above 70% passes the course without changing topic states.

### `GET /mastery`

Returns `[{knowledgePointId, topicId, skill, mastery, evidenceCount}]` for KPs in `knowledge_point_catalog`, using only the verified user's `kp_evidence`. `mastery` is a number from 0 to 1; `evidenceCount` counts the user's stored evidence for that KP. No learning goal or aggregate learning state is required.

Example after Lan completes L1:

```json
[{"knowledgePointId":"20000000-0000-4000-8000-020000000003","topicId":"10000000-0000-4000-8000-000000000001","skill":"READING","mastery":0.729,"evidenceCount":4}]
```

### `GET /topics/{id}/lessons`

The topic must be `IN_PROGRESS` or `PASSED`; otherwise `403 TOPIC_LOCKED`. Returns `{topicId, skill, hasTopicTest, lessons, testStatus}`. Lesson statuses are `LOCKED`, `AVAILABLE`, or `COMPLETED`; each lesson also has `practiceStatus` (`LOCKED`, `REQUIRED`, `PASSED`) and nullable `practicePassReason` (`FIRST_SUBMISSION`, `REVIEW_FINISHED`, `ALL_SETS_ATTEMPTED`, `NO_PRACTICE`). `testStatus` is `LOCKED`, `AVAILABLE`, `PASSED`, or `NONE` when `hasTopicTest=false`. A test is available only after all topic lessons are completed, all their practice statuses are `PASSED`, and no review of the same skill (or unassigned skill) is pending. Completing every published lesson passes a topic without a test.

```json
{
  "topicId":"10000000-0000-4000-8000-000000000001","skill":"READING","hasTopicTest":true,"testStatus":"LOCKED",
  "lessons":[
    {"lessonId":"20000000-0000-4000-8000-000000000101","code":"L1","title":"Câu chủ đề nằm ở đâu","sortOrder":1,"status":"AVAILABLE","practiceStatus":"LOCKED","practicePassReason":null},
    {"lessonId":"20000000-0000-4000-8000-000000000102","code":"L2","title":"Ý chính của cả bài","sortOrder":2,"status":"LOCKED","practiceStatus":"LOCKED","practicePassReason":null},
    {"lessonId":"20000000-0000-4000-8000-000000000103","code":"L3","title":"Ý chính hay chi tiết?","sortOrder":3,"status":"LOCKED","practiceStatus":"LOCKED","practicePassReason":null},
    {"lessonId":"20000000-0000-4000-8000-000000000104","code":"L4","title":"Dạng Matching Headings","sortOrder":4,"status":"LOCKED","practiceStatus":"LOCKED","practicePassReason":null}
  ]
}
```

### `GET /lessons/{id}`

Lesson metadata adds `skill` from Content. A pending review blocks only lessons of the same skill; a legacy review with `skill: null` blocks every skill. The existing gate error precedence and HTTP 403 remain unchanged.

The same lesson gate applies to this GET, submissions, and completion. Error precedence: pending review (`REVIEW_REQUIRED`), topic not current (`TOPIC_LOCKED`), then earlier lesson incomplete (`LESSON_LOCKED`). Returns metadata and ordered `blocks`. A question object has **only** `questionVersionId`, `sortOrder`, `stem`, `options`, `hint`; an asset can provide passage text alongside it. Exercise blocks carry `blockKind`: `EXERCISE` for auto-graded questions, `ESSAY` for a Writing essay block shaped as in [`lesson-writing-v1`](lesson-writing-v1.md) (an essay block never counts toward lesson completion, and exercise submissions to it return `409 ESSAY_BLOCK`). An `ASSET` block gives `{id, assetType, textContent?, mediaUrl?, durationSeconds?, transcript?}`: a `PASSAGE` has `textContent`; media has `mediaUrl` (never the stored reference); an `AUDIO` asset gains `transcript` only once the lesson is completed, and before that the key is absent because the transcript gives the answers away. Do not pass through Content's `answerSpec`, `explanation`, KP mappings, or solution fields. `options=null` means a fill response; a non-null array means choose an `optionKey`, including the three TFNG keys. A passed exercise block gains a separate `solutions` array of `{questionVersionId, correctAnswer, explanation}`; an unpassed block omits the `solutions` key entirely.

`hint` is always present, including when `null`, so the key does not disclose which questions have author-written hints. Learning Service reveals Content's hint only for a gradable `FILL`, or a gradable `CHOICE` with at least three options (including TFNG). Empty/absent CHOICE options are treated as the fixed three-choice TFNG fallback; one or two options and unsupported/ungradable specs are ineligible. Legacy gradable specs without `type` follow the grader's CHOICE default. A question must have been answered incorrectly in **any** saved submission for the same verified user, lesson and block, and the block must not have passed. Once opened, its hint remains even after a later correct answer while the block still fails. Before the first wrong answer, without a Content hint, or after the block has passed, `hint=null`. The runtime rule does not filter by skill; current Content seed supplies Reading hints and leaves Listening hints null. Essay prompts do not use this field.

The following is the L1-B5 portion of L1. The full response also contains L1's text, roof passage, and L1-B2 exercise; it contains all Q1, Q11, Q12 in L1-B5.

```json
{
  "lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001","code":"L1","title":"Câu chủ đề nằm ở đâu","sortOrder":1,"status":"AVAILABLE","skill":"READING",
  "blocks":[
    {"blockId":"20000000-0000-4000-8000-000000000205","blockType":"EXERCISE","sortOrder":5,"passed":false,"questions":[
      {"questionVersionId":"20000000-0000-4000-8000-000000000001","sortOrder":1,"stem":"Which sentence is the topic sentence of paragraph C?","options":[{"optionKey":"A","content":"Green roofs also manage rainwater.","sortOrder":1},{"optionKey":"B","content":"The soil soaks up much of a heavy shower and releases it slowly…","sortOrder":2},{"optionKey":"C","content":"In Copenhagen, new flat roofs must now be planted for this reason.","sortOrder":3}],"hint":null},
      {"questionVersionId":"20000000-0000-4000-8000-000000000011","sortOrder":2,"stem":"Which sentence tells you what paragraph D is about?","options":[{"optionKey":"A","content":"Not everyone is convinced.","sortOrder":1},{"optionKey":"B","content":"Critics point out that green roofs are expensive to install and need regular care…","sortOrder":2},{"optionKey":"C","content":"…many older buildings are not strong enough to carry the extra weight.","sortOrder":3}],"hint":null},
      {"questionVersionId":"20000000-0000-4000-8000-000000000012","sortOrder":3,"stem":"Complete with ONE WORD from paragraph D: people who doubt green roofs are called ______.","options":null,"hint":null}
    ]}
  ]
}
```

### `POST /lessons/{id}/exercises/{blockId}/submissions`

Request: `{requestId, answers:[{questionVersionId, answer}]}`. `requestId` is a UUID; one answer for **every** question in the block is required (`422` if missing/duplicate/foreign). Reuse of a request id for the same user, lesson, and block returns the saved response; reuse in a different scope is `409 REQUEST_CONFLICT`. Reattempts submit the whole same block with a new request id. The pass threshold is at least 70% of possible score; for L1-B5's three one-point questions, 2/3 fails and 3/3 passes. Only the **first submission** for a user and block contributes mastery evidence, whether it passed or failed. Later attempts can complete the block but never add evidence. If the block completes the lesson, mark the lesson complete in the same PostgreSQL transaction, serialized by a transaction-scoped advisory lock for the user. Lesson completion does not create a review.

Every result carries `hint`. On failure, it is `{questionVersionId, correct, hint}`: apply the GET policy to the union of previously wrong questions and this submission's wrong answers, including previously wrong questions now answered correctly. On passing, it is `{questionVersionId, correct, correctAnswer, explanation, hint:null}`. A block that passed earlier keeps hints null even if a later attempt fails. Replaying a `requestId` returns its original saved response, including hints, even after passing or Content edits. Hint history uses one extra query per GET or new submission; replay needs no Content read or hint query. Mastery evidence and the formula do not change, and no `hints_used` is recorded.

```json
{"requestId":"20000000-0000-4000-8000-000000000801","answers":[{"questionVersionId":"20000000-0000-4000-8000-000000000001","answer":"A"},{"questionVersionId":"20000000-0000-4000-8000-000000000011","answer":"B"},{"questionVersionId":"20000000-0000-4000-8000-000000000012","answer":"critics"}]}
```

The first response, 2/3, is:

```json
{"blockPassed":false,"lessonCompleted":false,"results":[{"questionVersionId":"20000000-0000-4000-8000-000000000001","correct":true,"hint":null},{"questionVersionId":"20000000-0000-4000-8000-000000000011","correct":false,"hint":"Câu báo trước nội dung cả đoạn D thường ngắn và khái quát. Câu nêu một lý do cụ thể chỉ là chi tiết."},{"questionVersionId":"20000000-0000-4000-8000-000000000012","correct":true,"hint":null}]}
```

A new request with Q11=`A` passes 3/3; each result then adds the solution. Q12's `correctAnswer` is `critics` (the first accepted answer):

```json
{"blockPassed":true,"lessonCompleted":true,"results":[{"questionVersionId":"20000000-0000-4000-8000-000000000001","correct":true,"hint":null,"correctAnswer":"A","explanation":"\"Green roofs also manage rainwater\" nêu chủ đề. Câu B giải thích cách làm, câu C là ví dụ."},{"questionVersionId":"20000000-0000-4000-8000-000000000011","correct":true,"hint":null,"correctAnswer":"A","explanation":"\"Not everyone is convinced\" báo trước cả đoạn nói về ý kiến phản đối. Câu B chỉ là một lý do cụ thể."},{"questionVersionId":"20000000-0000-4000-8000-000000000012","correct":true,"hint":null,"correctAnswer":"critics","explanation":"Đoạn D: \"Critics point out that…\". Không phân biệt hoa thường."}]}
```

### `POST /lessons/{id}/complete`

Only a lesson with **no** `EXERCISE` block can be completed this way; a lesson with exercises returns `409`. The same lesson gate applies. Empty request body. Idempotent completion returns `{lessonId, status:"COMPLETED"}`. Every V9 seed lesson has an exercise, so there is no valid seed success example. Calling this route for L1 returns an error:

```json
{"detail":"L1 contains exercise blocks","code":"LESSON_HAS_EXERCISES"}
```

### Lesson practice

`GET /lessons/{id}/practice-sets` returns `{lessonId, skill, lessonCompleted, practiceStatus, practicePassReason, items}`. Each item has `{packageId, code, title, questionCount, accessLevel, status, bestPercent, lastAttemptId, revealed}`. `accessLevel` is `FREE` or `PREMIUM`; Learning does not check entitlement. Item `status` is `LOCKED` before lesson completion or during a pending review of that skill, otherwise `AVAILABLE`, `IN_PROGRESS`, `PASSED`, or `ATTEMPTED`. `revealed` means the learner has submitted either a practice attempt or a review set for that package. The catalog remains readable while locked.

`POST /lessons/{id}/practice-attempts` takes `{packageId}` and returns HTTP 201 with `{attemptId, packageId, packageVersionId, passage?, audio?, questions}`. Starting the same package while an attempt is open returns that attempt and keeps its pinned package version. The package must belong to the lesson. `GET /practice-attempts/{id}` returns the owned attempt's question view, or its saved submission response after submission; another learner's attempt returns 404. Questions expose only `questionVersionId`, `sortOrder`, `stem`, `options`, and `hint`; audio exposes `mediaUrl` and `durationSeconds`. Neither route exposes `answerSpec`, `explanation`, solutions, or transcript before submission.

`POST /practice-attempts/{id}/submissions` takes `{requestId, answers:[{questionVersionId, answer}]}` with exactly one answer per question. It returns `{attemptId, correct, total, percent, passed, countedAsEvidence, results, transcript?, reviewsCreated}`; each result contains `questionVersionId`, `correct`, `correctAnswer`, `explanation`, and `hint:null`. A Listening transcript appears after submission. A repeated `requestId` returns the saved result; another request for a submitted attempt returns `409 ATTEMPT_ALREADY_SUBMITTED`. Passing is at least 70%. Only the first submission of an unrevealed package writes `practice_set` evidence and can give `FIRST_SUBMISSION`; subsequent practice of that package, including after a submitted review set, is graded and revealed but writes no evidence or review. A counted submission below 70% can create a `PRACTICE` review for each KP below 70% if that KP has no pending review and an unrevealed eligible set remains. Lesson completion itself creates no review.

Practice clearance is one-way: after the lesson completes, it passes with the first applicable reason in this order: counted first practice submission at 70% or above; a completed or skipped practice-triggered review; all currently published lesson packages submitted in practice or review with no pending practice review; or no practice packages. The first passing reason is stored and later content changes do not revoke it. Reads calculate status without writing a pass row. A pending same-skill review (or legacy null-skill review) blocks practice; `REVIEW_REQUIRED` remains HTTP 403. An incomplete lesson gives `409 PRACTICE_LOCKED` on start.

### `GET /reviews`

Lists the verified learner's reviews oldest first, without a Content request. Query parameters: `status` defaults to `PENDING`; `skill` optionally selects that skill and legacy null-skill reviews; `limit` defaults to 20, accepts 1–100, and returns HTTP 400 above 100. Each item is `{reviewId, knowledgePointId, lessonId, skill, stage, createdAt}`; `stage` is `PRACTICE` or `THEORY`. Review skill is filled from the teaching lesson's topic on curriculum refresh when possible.

### `GET /reviews/{reviewId}`

`reviewId` is a UUID. An unknown review or another learner's review returns `404 NOT_FOUND`. Besides `reviewId`, `reviewStatus`, `lessonId`, `theory` and `set`, the response has `knowledgePointId`, `skill`, `stage` (`PRACTICE` or `THEORY`), nullable `theoryReason` (`SECOND_FAIL`, `LOW_SCORE`, `WRONG_IN_LESSON`), `theoryScope`, `failedSets`, `maxFailedSets` (2) and `quickCheck`.

- `theory` is the text of the lesson's TEXT blocks that teach the review's knowledge point (`theoryScope: KNOWLEDGE_POINT`), or every TEXT block when the lesson tags none (`LESSON_FALLBACK`). Audio transcripts are never part of it.
- At `PRACTICE`, the response has the one open review set, preferring its lesson and excluding every package the learner was given in a review or whose answers were seen in submitted practice. Repeated GETs return the same open set. If no such package remains, the review becomes `SKIPPED` with no set. Set questions use the lesson allowlist with their `hint`; a Listening set adds `audio: {mediaUrl, durationSeconds}` next to `passage` (no transcript). `quickCheck` is empty.
- At `THEORY`, `set` is null and `quickCheck` lists up to 3 auto-graded questions of that lesson tagged with the knowledge point (lesson order, same allowlist with `hint`, no answers). The learner answers them through `theory-check`.
- A review starts at `THEORY` when the knowledge point scored below 40% in the practice attempt that created it (`LOW_SCORE`), or when the learner answered it wrong on the first submission of the lesson's exercises (`WRONG_IN_LESSON`). Practice reviews decide this when created; reviews from assessment results decide it the first time they are opened.

After Lan misses L2 Q5, she can receive `PS-KP1-A` and L2 theory:

Review questions and results reuse the lesson DTOs. Set and quick-check questions carry `hint`; results carry `hint:null`. Final assessments and game snapshots do not expose hints.

```json
{
  "reviewId":"20000000-0000-4000-8000-000000000901","reviewStatus":"PENDING","lessonId":"20000000-0000-4000-8000-000000000102",
  "theory":["Ý chính của cả bài là điều mọi đoạn cùng góp vào. Đọc câu chủ đề của từng đoạn rồi tìm điểm chung. Mẹo: Đáp án đúng thường khái quát; đáp án bẫy chỉ đúng với một đoạn."],
  "set":{"reviewSetId":"20000000-0000-4000-8000-000000000902","packageId":"20000000-0000-4000-8000-000000000501","packageVersionId":"20000000-0000-4000-8000-000000000601","passage":"A. Beekeeping is no longer only a country pursuit. In London, Paris and New York, thousands of hives now sit on rooftops and in backyards, kept by office workers, schools and even hotels.\n\nB. Supporters say city bees do well because parks and gardens offer a wide variety of flowers throughout the year. Honey from urban hives often wins prizes for its complex flavour.\n\nC. However, scientists warn that too many hives can harm wild bees. When honeybees are crowded into a small area, they compete with native species for the same limited flowers.","questions":[{"questionVersionId":"20000000-0000-4000-8000-000000000051","sortOrder":1,"stem":"What is the passage mainly about?","options":[{"optionKey":"A","content":"City honey tastes better than country honey","sortOrder":1},{"optionKey":"B","content":"Urban beekeeping is growing, with benefits and risks","sortOrder":2},{"optionKey":"C","content":"Wild bees are disappearing from London","sortOrder":3}],"hint":null}]}
}
```

The example shows BE1; the actual set includes BE1–BE4. `PS-KP1-A` is the seed package code corresponding to the example `packageId`.

Review states (`maxFailedSets = 2`; Practice and review sets share the lesson's `PRACTICE_SET` packages, so practice
uses up review sets):

```text
                  review created (practice < 70%, or assessment mastery < 0.6)
                     |
        +------------+-----------------------------+
        | PRACTICE                                 | THEORY (KP < 40% in the attempt, or wrong in the lesson)
        v                                          v
   GET: one unrevealed set with hints         GET: KP theory blocks + quick check
   submit set -- >= 70% --> DONE              POST theory-check --> PRACTICE
        `-- fail -- 2nd failed set --> SKIPPED
                 `-- otherwise --> THEORY
   no unrevealed package left --> SKIPPED
```

### `POST /reviews/{reviewId}/submissions`

Request: `{reviewSetId, requestId, answers}` with the same answer array shape as a lesson submission and all set questions required. A review at `THEORY` returns `409 THEORY_REQUIRED`. Only the owned, currently open set may be submitted; a different or closed set gives `409 REVIEW_SET_CLOSED`. A set is submitted once, and its first answers write `review_set` mastery evidence. At least 70% makes the review `DONE`. A failed set moves the review to `THEORY` (`LOW_SCORE` below 40%, otherwise `SECOND_FAIL`); the second failed set makes it `SKIPPED`. The response has `reviewStatus`, `stage`, `failedSets`, `results` with `correctAnswer` and `explanation` for every set, passed or not, and the section `transcript` for a Listening set.

```json
{"reviewSetId":"20000000-0000-4000-8000-000000000902","requestId":"20000000-0000-4000-8000-000000000803","answers":[{"questionVersionId":"20000000-0000-4000-8000-000000000051","answer":"B"},{"questionVersionId":"20000000-0000-4000-8000-000000000052","answer":"A"},{"questionVersionId":"20000000-0000-4000-8000-000000000053","answer":"A"},{"questionVersionId":"20000000-0000-4000-8000-000000000054","answer":"A"}]}
```

```json
{"reviewStatus":"DONE","results":[{"questionVersionId":"20000000-0000-4000-8000-000000000051","correct":true,"hint":null,"correctAnswer":"B","explanation":"Đoạn A: đang phát triển; B: lợi ích; C: rủi ro."},{"questionVersionId":"20000000-0000-4000-8000-000000000052","correct":true,"hint":null,"correctAnswer":"A","explanation":"Cả đoạn giải thích vì sao ong thành phố phát triển tốt."},{"questionVersionId":"20000000-0000-4000-8000-000000000053","correct":true,"hint":null,"correctAnswer":"A","explanation":"\"However… can harm wild bees\" là mặt trái."},{"questionVersionId":"20000000-0000-4000-8000-000000000054","correct":true,"hint":null,"correctAnswer":"A","explanation":"Tiêu đề phải bao cả lợi ích lẫn rủi ro."}]}
```

### `POST /topics/{id}/test-assignments`

Only pending reviews of the topic's skill or a legacy null skill block assignment. A topic with `hasTopicTest=false` returns `409 NO_TOPIC_TEST` and passes when all its published lessons are completed.

Requires the topic to be `IN_PROGRESS`, all lessons completed, all lessons practice-cleared, and no pending review of its skill or a legacy null skill. A pending review returns `403 REVIEW_REQUIRED` first; incomplete practice returns `409 PRACTICE_REQUIRED` with `lessonIds`; other locked conditions return `403 TEST_LOCKED`. Empty body. Repeated POSTs return the same unconsumed assignment. After consumption, choose a `packageId` not yet used by this learner/topic; when exhausted, reuse the least recently consumed package with its currently PUBLISHED version. With no available code, return `409 TEST_UNAVAILABLE`. Assignment alone does not submit an assessment or pass the topic.

For `DEMO_READING` the first assignment can be seed code `X1`:

```json
{"assignmentId":"20000000-0000-4000-8000-000000000951","packageId":"20000000-0000-4000-8000-000000000301","packageVersionId":"20000000-0000-4000-8000-000000000401"}
```

**One-use rule for MVP:** Assessment start accepts `packageVersionId` without `assignmentId`. Only the **first completed submission** for `(user, packageVersionId)` after the current assignment's `assignedAt` consumes that assignment and may open the topic. Other attempts of the same version for that assignment do not open the topic, but their results still contribute mastery evidence. After every code has been used, assignment chooses the least recently used package again; a new `assignedAt` starts a new assignment window, and the learner may already know its answers. That repeat-code risk is an accepted MVP limit.

### `POST /reviews/{reviewId}/theory-check`

Request: `{requestId, answers}` with one answer for every `quickCheck` question (`[]` when there is none). Allowed only at `THEORY`, otherwise `409 THEORY_NOT_REQUIRED`. Right or wrong, the review returns to `PRACTICE` and the next GET gives a new set. No mastery evidence is written. A repeated `requestId` returns the saved response; reuse on another review gives `409 REQUEST_CONFLICT`. Response: `{reviewId, correct, total, results:[{questionVersionId, correct, correctAnswer, explanation, hint:null}], stage:"PRACTICE"}`.

## Error body and review rule

New learning-gate errors use `{ "detail": string, "code": string, "reviews"?: array, "lessonIds"?: array }`. `reviews` is present only for `REVIEW_REQUIRED`, with each entry `{reviewId, lessonId, knowledgePointId, skill?}`; a legacy null skill is omitted. `lessonIds` is present for `PRACTICE_REQUIRED`. Do not return an answer, prompt, or token in an error. Ordinary malformed requests use HTTP 422; dependency errors retain HTTP 502/503. Examples for each new code:

| HTTP | Code | JSON example |
| --- | --- | --- |
| 403 | `REVIEW_REQUIRED` | `{"detail":"Complete the pending review first","code":"REVIEW_REQUIRED","reviews":[{"reviewId":"20000000-0000-4000-8000-000000000901","lessonId":"20000000-0000-4000-8000-000000000102","knowledgePointId":"10000000-0000-4000-8000-000000000002"}]}` |
| 409 | `PLACEMENT_ALREADY_DONE` | `{"detail":"The placement test was already taken","code":"PLACEMENT_ALREADY_DONE"}` |
| 404 | `NO_PLACEMENT_TEST` | `{"detail":"No placement test is published","code":"NO_PLACEMENT_TEST"}` |
| 403 | `TOPIC_LOCKED` | `{"detail":"Topic is locked","code":"TOPIC_LOCKED"}` |
| 403 | `LESSON_LOCKED` | `{"detail":"Complete L1 before L2","code":"LESSON_LOCKED"}` |
| 403 | `TEST_LOCKED` | `{"detail":"Complete the topic lessons first","code":"TEST_LOCKED"}` |
| 409 | `TEST_UNAVAILABLE` | `{"detail":"No published test code is available","code":"TEST_UNAVAILABLE"}` |
| 409 | `NO_TOPIC_TEST` | `{"detail":"This topic has no final test","code":"NO_TOPIC_TEST"}` |
| 409 | `PRACTICE_LOCKED` | `{"detail":"Complete the lesson before practice","code":"PRACTICE_LOCKED"}` |
| 409 | `PRACTICE_REQUIRED` | `{"detail":"Complete practice for these lessons","code":"PRACTICE_REQUIRED","lessonIds":["20000000-0000-4000-8000-000000000101"]}` |
| 409 | `ATTEMPT_ALREADY_SUBMITTED` | `{"detail":"Practice attempt was already submitted with a different requestId","code":"ATTEMPT_ALREADY_SUBMITTED"}` |
| 409 | `REQUEST_CONFLICT` | `{"detail":"requestId belongs to another submission","code":"REQUEST_CONFLICT"}` |
| 409 | `LESSON_HAS_EXERCISES` | `{"detail":"L1 contains exercise blocks","code":"LESSON_HAS_EXERCISES"}` |
| 409 | `REVIEW_SET_CLOSED` | `{"detail":"Review set is closed","code":"REVIEW_SET_CLOSED"}` |
| 409 | `THEORY_REQUIRED` | `{"detail":"Read the theory before the next set","code":"THEORY_REQUIRED"}` |
| 409 | `THEORY_NOT_REQUIRED` | `{"detail":"This review is not at the theory stage","code":"THEORY_NOT_REQUIRED"}` |
| 409 | `ESSAY_BLOCK` | `{"detail":"Essay blocks are submitted as essays","code":"ESSAY_BLOCK"}` |
| 404 | `NOT_FOUND` | `{"detail":"Lesson was not found","code":"NOT_FOUND"}` |

Practice-triggered review insertion uses the attempt's own score, not mastery: the counted first submission must be below 70%, a KP must be below 70% in that attempt, no review for that KP may be pending, and another unrevealed eligible package must exist. Assessment-triggered review insertion retains the mastery threshold (`learning.review-mastery-threshold`, default `0.6`), wrong-answer, completed teaching lesson, and eligible-practice checks. Lesson completion creates no review. A KP with no remaining eligible package cannot create a practice review. Topic `PASSED` is one-way.

## Assessment changes requiring approval

**Implemented 2026-10-01** as described below. Errors use Assessment's `ErrorResponse`
(`timestamp,status,error,message,path,details`) plus a `code` key on these cases: `409 ATTEMPT_EXPIRED` (submit at or
after `expiresAt`; the attempt is left `EXPIRED`), `422 PACKAGE_NOT_ATTEMPTABLE` (`PRACTICE_SET`, `LESSON`, or a version
with an empty section), `503 CONTENT_UNAVAILABLE` (Content failed or returned an unusable version). An unknown or
unpublished `packageVersionId` is `404`; a result not yet `COMPLETED` is `404` on `GET /attempts/{attemptId}/result`.
`knowledgeSnapshot` is `[{knowledgePointId, weight}]`. A stored `answer` that is not a JSON string scores zero.

These are proposed changes to existing `/api/assessments` routes. The current controller/DTO names are preserved where behavior is unchanged; grader DTOs and `GradingController` stay as they are.

| Route | Contract |
| --- | --- |
| `POST /attempts` | Request becomes `{packageVersionId, mode, channel}`. `mode` remains `STANDARD|TIMED`; `channel` remains `WEB|MOBILE|API`. Assessment fetches the Content package version before writing. It derives `attemptType`: `TOPIC_TEST→TOPIC_GATE`, `COURSE_TEST→COURSE_GATE`, `MOCK_TEST→MOCK`, `PLACEMENT_TEST→PLACEMENT`, `QUIZ→QUIZ`; `PRACTICE_SET` and `LESSON` return 422. MVP final tests have no time limit, so `expiresAt=null` when version `rules` has no time key. Old `attemptType`, `expiresAt`, `sections` are ignored if sent (Jackson unknown-field behavior), never trusted. The existing `AssessmentAttemptResponse` field names remain `id,userId,packageVersionId,attemptType,mode,channel,status,startedAt,submittedAt,expiresAt,rowVersion,createdAt,updatedAt`. |
| `GET /attempts/{id}/structure` | Existing `sections[].{id,contentSectionId,sortOrder,snapshot,items[]}` and item `{id,questionVersionId,sortOrder,questionSnapshot,knowledgeSnapshot}` names remain. **Remove `answerSnapshot`** from the learner DTO. `snapshot` is now an allowlist **object** `{title,skill,instructions,passage?,audio?}`; `audio` is `{url,durationSeconds}` using Content's resolved `audio.mediaUrl`. `solution` and `transcript` are never exposed here. Missing `skill` in an older JSON snapshot defaults to `READING`; an unparseable snapshot returns null fields without a server error. `questionSnapshot` remains a string containing stem/options only. |
| `PUT /attempts/{id}/items/{itemId}/response` | Existing request remains `{payload,schemaVersion,expectedRevision}`. `payload` remains a string containing JSON `{"answer":"…"}`; see [answer spec](answer-spec-v1.md). Existing `AttemptResponseResponse` field names remain unchanged. |
| `POST /attempts/{id}/submit` | If every item is objectively gradable, save result version 1 as `COMPLETED`, item scores, and outbox in the same transaction. If any item is ungradable, retain the human grading flow. Submitting after expiry persists `EXPIRED` and responds 409 with an expiry code. |
| `GET /attempts/{attemptId}/result` | Learner-only DTO for latest **COMPLETED** version, ignoring a newer DRAFT. It has `id,attemptId,resultVersion,status,completedAt,score,maxScore,percent,items[{attemptItemId,questionVersionId,correct}]`. `solutions[]` with `{attemptItemId,questionVersionId,correctAnswer,explanation}` exists only when `percent >= 70`; below 70 the key is absent. A separate `sectionSolutions[]` with `{attemptSectionId,transcript}` is also present only when `percent >= 70`, for sections whose stored `solution.transcript` is a string; it is `[]` when no section has a transcript and absent below 70. The existing `solutions[]` array is unchanged. Examiner DTO `AssessmentResultResponse` remains `id,attemptId,resultVersion,status,overallBand,completedAt`. |
| `POST /attempts/{attemptId}/result` | Remove the learner route and its learner use-case path. Grader routes under `/api/assessments/grading/**` remain. |

X1/X2/X5 seed versions use `rules={}`, so their `expiresAt` is `null`. The time-limit calculation rule is deferred beyond MVP; a missing time key in `rules` always yields `expiresAt=null`.

Example creation from assigned seed code `X1`:

```json
{"packageVersionId":"20000000-0000-4000-8000-000000000401","mode":"STANDARD","channel":"WEB"}
```

```json
{"id":"20000000-0000-4000-8000-000000000971","userId":"20000000-0000-4000-8000-000000000981","packageVersionId":"20000000-0000-4000-8000-000000000401","attemptType":"TOPIC_GATE","mode":"STANDARD","channel":"WEB","status":"IN_PROGRESS","startedAt":"2026-10-01T09:00:00Z","submittedAt":null,"expiresAt":null,"rowVersion":0,"createdAt":"2026-10-01T09:00:00Z","updatedAt":"2026-10-01T09:00:00Z"}
```

Structure excerpt for X1's Q2 (the full version includes Q14–Q16):

```json
{"sections":[{"id":"20000000-0000-4000-8000-000000000972","contentSectionId":"20000000-0000-4000-8000-000000000701","sortOrder":1,"snapshot":{"title":"Street trees","skill":"READING","instructions":null,"passage":"A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.\n\nB. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.\n\nC. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away."},"items":[{"id":"20000000-0000-4000-8000-000000000973","questionVersionId":"20000000-0000-4000-8000-000000000002","sortOrder":1,"questionSnapshot":"{\"stem\":\"What is the passage mainly about?\",\"options\":[{\"optionKey\":\"A\",\"content\":\"How oak trees grow in cities\",\"sortOrder\":1},{\"optionKey\":\"B\",\"content\":\"The ways street trees improve city life\",\"sortOrder\":2},{\"optionKey\":\"C\",\"content\":\"A 2019 study of stress in Toronto\",\"sortOrder\":3}]}","knowledgeSnapshot":"[{\"knowledgePointId\":\"10000000-0000-4000-8000-000000000002\",\"weight\":1.0}]"}]}]}
```

Saving Q2's answer uses the existing request schema:

```json
{"payload":"{\"answer\":\"B\"}","schemaVersion":1,"expectedRevision":0}
```

At 3/4 on X1 (Q15 wrong), `percent=75`, so solutions appear while each item still reports correctness:

```json
{"id":"20000000-0000-4000-8000-000000000975","attemptId":"20000000-0000-4000-8000-000000000971","resultVersion":1,"status":"COMPLETED","completedAt":"2026-10-01T09:10:00Z","score":3,"maxScore":4,"percent":75,"items":[{"attemptItemId":"20000000-0000-4000-8000-000000000973","questionVersionId":"20000000-0000-4000-8000-000000000002","correct":true},{"attemptItemId":"20000000-0000-4000-8000-000000000974","questionVersionId":"20000000-0000-4000-8000-000000000015","correct":false}],"solutions":[{"attemptItemId":"20000000-0000-4000-8000-000000000974","questionVersionId":"20000000-0000-4000-8000-000000000015","correctAnswer":"DETAIL","explanation":"Kết quả nghiên cứu ở Toronto là bằng chứng cho ý \"Trees also calm people\"."}],"sectionSolutions":[]}
```

The item/solution arrays above are excerpts; the actual response includes all four X1 items and, on a passing result, all four solutions. On a 2/4 result, `percent=50`, correctness remains available for every item and both `solutions` and `sectionSolutions` are absent.

Listening sections are stored server-side as `{title,skill,instructions,audio:{url,durationSeconds},solution:{transcript}}`; sections without audio keep `{title,skill,instructions,passage?}`. The learner structure exposes only the audio URL and duration, for example:

```json
{"snapshot":{"title":"Listening","skill":"LISTENING","instructions":"Listen and answer","audio":{"url":"https://cdn.example.com/listening/demo/ls1.mp3","durationSeconds":95}}}
```

On a passing Listening result, section solutions are separate from item solutions:

```json
{"sectionSolutions":[{"attemptSectionId":"20000000-0000-4000-8000-000000000972","transcript":"The library closes at six."}]}
```

These excerpts illustrate the new section fields. Auto-grading, attempt types, outbox and `AssessmentCompleted.v2` are unchanged; the event never includes a transcript.

## Public Content knowledge point API change

`GET /api/content/knowledge-points` removes `bandMin`, `bandMax`, `effectiveBandMin`, `effectiveBandMax` from each KP response. `POST /api/content/knowledge-points` no longer accepts `bandMin` or `bandMax`; old inputs are ignored by the current Jackson unknown-field behavior. Existing keys such as `id`, `topicId`, `code`, `name`, `kind`, `learningType`, `skill`, `description`, and `status` keep their names. Topic band fields stay. This is a public response change for consumers to review before phase 3.

## Delivery order

Deploy the Java Learning Service consumer accepting a nullable `learning_goal_id` and missing `package_version_id` before Assessment starts emitting goal-less events. See [AssessmentCompleted.v2](assessment-completed-v2.md) for retry, idempotency and DLQ replay. Learner access restrictions and internal Content endpoints precede opening this learner API.
