# Submission history v1

The learner's own past submissions, for the page "Lịch sử nộp bài", and what the learner practised after an essay. Read only;
nothing is stored for it.

## `GET /api/learning/submission-history`

Role `CUSTOMER`; the user comes from the token. Through the Gateway as `/api/learning/**`.

| Parameter | Meaning |
|---|---|
| `scope` (required) | `COURSE`: what was submitted inside a course. `FREE`: free practice of the practice bank. `KNOWLEDGE`: free practice on one knowledge point. |
| `skill` | `WRITING` or `SPEAKING`. Required for `scope=FREE`; ignored otherwise. |
| `page` | From 0. Default 0. |
| `size` | 1 to 50, default 20. Values outside the range are clamped. |

A missing or unknown `scope`, or a `FREE` request whose `skill` is not `WRITING` or `SPEAKING`, answers `422` with the common
`Invalid request` body.

Response: `{items, page, size, hasNext}`. There is no total: the service reads one row more than a page to know whether another
page exists. Items are newest first (`submittedAt` descending, then `id`).

Item: `{kind, id, submittedAt, skill, task, promptPreview, band, percent, correctCount, totalCount, passed, status, lessonId,
topicId, courseId, attemptId, packageVersionId, revisionNo, knowledgePointId, knowledgePointCode, knowledgePointName}`. A field
that does not apply to the `kind` is `null`.

| `kind` | Where it comes from | Fields it fills |
|---|---|---|
| `PRACTICE` | A submitted lesson Practice attempt (`practice_attempts`, origin `LESSON`) | `id` and `attemptId` (the attempt), `skill`, `correctCount`, `totalCount`, `passed`, `lessonId`, `packageVersionId` |
| `WRITING` | An essay of a lesson block or a Practice item (`COURSE`), or a free practice essay (`FREE`) | `id` (the essay), `task`, `promptPreview` (at most 160 characters of the question), `band`, `passed`, `status`, `lessonId` (null when free), `revisionNo` |
| `SPEAKING` | A speaking submission of a lesson (`COURSE`) or of the practice bank (`FREE`) | `id`, `band`, `status`, `lessonId` (null when free), `packageVersionId` |
| `TOPIC_TEST` | A topic test that was taken (`topic_test_assignments`, consumed) | `id` (the assignment), `attemptId`, `percent`, `topicId`, `packageVersionId` |
| `COURSE_TEST` | A course test that was taken (`course_test_assignments`, consumed) | `id`, `attemptId`, `percent`, `courseId`, `packageVersionId` |
| `PRACTICE_TEST` | A practice test milestone of a course that was taken (`practice_test_assignments`, consumed) | `id`, `attemptId`, `percent`, `courseId`, `packageVersionId` |
| `KP_PRACTICE` | A submitted free practice set on one knowledge point (`practice_attempts`, origin `FREE`; `KNOWLEDGE`) | `id` and `attemptId` (the attempt), `skill`, `correctCount`, `totalCount`, `passed`, `knowledgePointId`, `knowledgePointCode`, `knowledgePointName` |

`submittedAt` is the submission time of the attempt or essay, the creation time of a speaking submission, and the time a test
was taken.

Left out: an essay whose grading failed (`FAILED`, the points went back) and a speaking submission that is `UPLOADING` or
`ABANDONED`. An essay still being graded, waiting for payment or waiting for an examiner is listed with its `status`.
A lesson Practice set that holds a Writing or Speaking item shows both the Practice attempt (`PRACTICE`) and the essay or
speaking submission (`WRITING`, `SPEAKING`), as two records.

For `PRACTICE`, `WRITING` and `SPEAKING` of a lesson, `topicId` is the topic of the lesson when the learner's progress holds it
(`lesson_progress`), otherwise null. Names of lessons, topics, courses and tests are not part of the item; the client resolves
topics and courses from the ids (one call each) and does not open a lesson for its title, because opening a lesson counts as
studying it.

A `KP_PRACTICE` line is named by the service: the knowledge point catalog of Content is read once, only for a page that has such
a line. When Content does not answer, the page is still returned and `knowledgePointCode` and `knowledgePointName` are null.

## `GET /api/learning/submission-history/practice-since-essay`

What the learner practised on some knowledge points since one of their essays was submitted; the Writing result page uses it to
show, for each thing it suggests studying, whether the learner has practised it since. Role `CUSTOMER`.

| Parameter | Meaning |
|---|---|
| `submissionId` (required) | The essay. Another learner's essay, or one that does not exist, answers `404 NOT_FOUND`. |
| `knowledgePointIds` | The points to look at, repeated or comma separated; duplicates count once. At most 20, otherwise `422`. None answers `[]`. |

Response: an array, one entry per point practised since the essay's `submitted_at`; a point not practised since is absent.
Entry: `{knowledgePointId, attempts, lastAttemptId, lastSubmittedAt, lastCorrectCount, lastTotalCount, lastPassed}`, where
`attempts` counts the submitted free practice sets (`practice_attempts`, origin `FREE`) of the point since the essay and the
`last*` fields describe the latest of them. `lastAttemptId` opens at `/learn/practice/{attemptId}`.

## Not in this list

- Free practice Listening and Reading attempts: `GET /api/assessments/practice/history?skill=LISTENING|READING`.
- Essays sent to an examiner: `GET /api/learning/examiner-reviews` with `GET /api/assessments/grading-jobs/mine`.
- Lesson exercise blocks and quick checks.

## Reading it

Learning reads its own tables only (`practice_attempts`, `lesson_writing_submissions`, `speaking_submissions`,
`topic_test_assignments`, `course_test_assignments`, `practice_test_assignments`). The `COURSE` scope is one `UNION ALL` of
those, so a page costs one query. Indexes `idx_lesson_writing_user_submitted` and `idx_practice_attempts_user_submitted` (V54)
serve the per-user timeline, including `KNOWLEDGE` and `practice-since-essay`; `speaking_submissions` already has one.
