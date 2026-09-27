# Tutor study/review HTTP and SSE contract (v1)

All routes start with `/api/ai-learning/tutor` and require the learner's bearer access token through Gateway.
Gateway replaces it with an internal JWT before forwarding to AI Learning. Session, turn, and question ids are
server-generated UUIDs. The active learning path is selected by the server.

| Method | Route | Request | Response |
| --- | --- | --- | --- |
| `POST` | `/sessions` | Optional `{ "title": "Study" }` | `201` session summary: `sessionId`, `pathId`, `title`, `createdAt`, `updatedAt` |
| `GET` | `/sessions?limit=50` | Limit 1–100 | `200` array of newest unarchived summaries |
| `GET` | `/sessions/{sessionId}` | None | `200` summary plus latest 200 `messages` and `pendingQuestion` |
| `DELETE` | `/sessions/{sessionId}` | None | `204` |
| `POST` | `/sessions/{sessionId}/turns` | Exactly one of `message` or `answer` | `200 text/event-stream` |

`message` must be 1–4,000 characters. `answer` is `{ "questionId": "uuid", "text": "..." }` with 1–2,000
characters. An invalid body returns JSON `422` before streaming. Missing or invalid authentication returns `401`;
an unowned, missing, or archived session returns `404`; another running turn in that session returns `409`.
Creating a session without an active learning goal also returns `409`.

Each SSE block has `event: <name>`, `data: <JSON object>` and a blank line. JSON keys are camelCase. While no event
is ready, the service sends `: keep-alive` comments about every 15 seconds. Responses set `Cache-Control: no-cache`
and `X-Accel-Buffering: no`.

| Event | Payload |
| --- | --- |
| `turn.started` | `{ "turnId": "uuid", "sessionId": "uuid" }` |
| `assistant.message` | `{ "text": "..." }` |
| `tool.called` | `{ "name": "mastery_status\|mastery_quiz\|mastery_grade\|mastery_assess\|path_outline\|path_reorder\|learner_profile" }` |
| `question` | Public question: `questionId`, `knowledgePointId`, `prompt`, `questionType`, `options`, and other public question fields when present. |
| `grading` | For a quiz: `questionId`, `knowledgePointId`, `isCorrect`, `mastery`, `mastered`, `explanation`. For a qualitative assessment: `knowledgePointId`, `passed`, `mastered`, `mastery`. |
| `path.reordered` | `{ "moduleCount": 2, "knowledgePointCount": 4 }` after an actual order change. |
| `profile.updated` | `{ "fields": ["time_budget"] }` when at least one profile field changed. |
| `turn.completed` | `{ "turnId": "uuid" }`, with `questionId` when the turn placed a question. |
| `turn.failed` | `{ "turnId": "uuid", "failureCode": "..." }` (for example `llm_error`, `llm_not_configured`, `too_many_rounds`, `internal_error`). |

Example question turn (illustrative ids and content):

```text
event: turn.started
data: {"turnId":"00000000-0000-4000-8000-000000000001","sessionId":"00000000-0000-4000-8000-000000000002"}

event: question
data: {"questionId":"00000000-0000-4000-8000-000000000003","knowledgePointId":"00000000-0000-4000-8000-000000000004","prompt":"Which option is correct?","questionType":"choice","options":[{"id":"A","label":"A","body":"Option A"},{"id":"B","label":"B","body":"Option B"}]}

event: turn.completed
data: {"turnId":"00000000-0000-4000-8000-000000000001","questionId":"00000000-0000-4000-8000-000000000003"}

```

The `expectedAnswer` field, system prompt, provider key, and raw tool arguments never appear in SSE. The open
question in `GET /sessions/{id}` belongs to that session; its `pendingQuestion` contains the public question and
its status, without the expected answer. A submitted answer is stored before the model runs. Grading updates the
same path and its revision, including when a finalized assessment arrived between tutor turns. If the client
disconnects, the turn continues on the server and the learner can fetch the session again.
