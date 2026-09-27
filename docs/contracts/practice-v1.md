# Tutor question practice API (v1)

All routes start with `/api/ai-learning/practice` and require the learner's bearer access token through Gateway.
Gateway replaces it with an internal JWT before forwarding to AI Learning. Entry ids are server-generated integers.
Every read and write is scoped to `user_id` stored on the notebook entry.

| Method | Route | Request | Response |
| --- | --- | --- | --- |
| `GET` | `/notebook` | Optional `knowledgePointId`, `sessionId`, `status=open\|correct\|incorrect`; `limit` 1–100 (default 50) | `200` newest owned entries first |
| `POST` | `/entries/{entryId}/answer` | `{ "answer": "..." }`, 1–4,000 characters | `200` `{ entryId, questionId, isCorrect, correctAnswer, explanation, dueAt? }` |
| `GET` | `/due` | `limit` 1–100 (default 20) | `200` currently due mistake entries, earliest due first |
| `POST` | `/reviews` | `{ "requestId": "uuid", "entryId": 1, "answer": "...", "rating": "good" }`; `rating` is optional and defaults to `good` | `200` `{ entryId, questionId, isCorrect, rating, dueAt, resolved, correctAnswer, explanation }` |

Entry responses contain `entryId`, `questionId`, `sessionId`, `knowledgePointId`, `knowledgePointName`, `prompt`, `questionType`,
`options`, `difficulty`, `createdAt`, `answeredAt`, `userAnswer`, `isCorrect`, and `resolved`. `correctAnswer` and
`explanation` are omitted until the learner answers. Due cards also omit both answer fields while they are awaiting a
review answer.

The tutor stores one to five `short` or `choice` questions per call. Every question has an explanation and a server-side
answer key. Multiple choice labels must be complete and ordered from A; the saved answer is normalized to its label.
A choice answer may be the label (`B`), a labelled option (`B. went`) or the option body (`went`), as on the tutor's
question card; an answer that names no single option is graded as wrong.
The answer endpoint uses deterministic `grade_answer`; a correct first answer creates no review state, while a wrong
first answer schedules the entry ten minutes later. Practice never updates path mastery, evidence, or revision.

The notebook entry keeps the learner's first answer and result; reviews never change `userAnswer`, `isCorrect` or the
`status` filter, only `resolved`. Each review is recorded separately.

Review accepts `hard`, `good`, or `easy`; a wrong review is always recorded as `again`. The scheduler is per entry. Three
consecutive correct reviews set `resolved=true` and remove that entry from `/due`. A `requestId` replay for the same
learner and entry returns the original response without advancing the schedule again. Reusing it for another learner or
entry returns `409`.

| Status | Meaning |
| --- | --- |
| `401` | Missing or invalid authentication |
| `404` | Entry does not exist or belongs to another learner |
| `409` | Entry was already answered, is not in the review schedule, or a review `requestId` conflicts |
| `422` | Invalid query, path id, request body, answer length, rating, or limit |

