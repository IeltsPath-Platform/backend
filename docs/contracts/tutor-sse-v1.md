# Tutor study/review HTTP and SSE contract (v1)

All routes start with `/api/ai-learning/tutor` and require the learner's bearer access token through Gateway.
Gateway replaces it with an internal JWT before forwarding to AI Learning. Session, turn, and question ids are
server-generated UUIDs. The active learning path is selected by the server.

| Method | Route | Request | Response |
| --- | --- | --- | --- |
| `POST` | `/sessions` | Optional `{ "title": "Study", "readingSectionId": "uuid" }` | `201` session summary: `sessionId`, `pathId`, `title`, `createdAt`, `updatedAt`, `material` (`null`, or `{ "type": "READING", "sectionId", "packageId", "title" }`); `404` when Content has no readable passage for the section, `503` when Content is unreachable. Neither creates a session. |
| `GET` | `/sessions?limit=50` | Limit 1–100 | `200` array of newest unarchived summaries |
| `GET` | `/sessions/{sessionId}` | None | `200` summary plus latest 200 `messages` and `pendingQuestion` |
| `DELETE` | `/sessions/{sessionId}` | None | `204` |
| `POST` | `/sessions/{sessionId}/turns` | Exactly one of `message` or `answer` | `200 text/event-stream`; `429` JSON when the daily turn limit is reached |
| `GET` | `/memory` | None | `200` `{ "content": "...", "updatedAt": "..." }`; no stored memory returns empty content and `null` timestamp |
| `DELETE` | `/memory` | None | `204`; clears the learner's memory and skips all messages that exist at deletion time |
| `GET` | `/usage` | None | `200` `{ "timezone": "Asia/Ho_Chi_Minh", "resetsAt": "...", "tutorTurns": { "used": 3, "limit": 50 }, "memorySummaries": { "used": 0, "limit": 10 } }`; `limit` `0` means unlimited |

`message` must be 1–4,000 characters. `answer` is `{ "questionId": "uuid", "text": "..." }` with 1–2,000
characters. An invalid body returns JSON `422` before streaming. Missing or invalid authentication returns `401`;
an unowned, missing, or archived session returns `404`; another running turn in that session returns `409`.
Creating a session without an active learning goal also returns `409`.

Each learner has a daily limit of tutor turns (default 50) and of memory summaries (default 10). The day ends at
midnight in the service's configured time zone (default `Asia/Ho_Chi_Minh`). A turn over the limit returns `429`
before any SSE and before the model is called:
`{ "detail": "Daily tutor turn limit reached", "limit": 50, "resetsAt": "2026-09-27T17:00:00Z" }`, with a
`Retry-After` header in seconds until `resetsAt`. Validation `422`, `404` and `409` do not count. A turn that ends in
`turn.failed` with `llm_not_configured`, or with `llm_error` before any `tool.called` or `assistant.message` event, is not
counted. Once the model has answered, the turn counts even if a later model call fails; other failures always count. A
memory summary over its limit is skipped silently and retried on a later turn or day.

Each SSE block has `event: <name>`, `data: <JSON object>` and a blank line. JSON keys are camelCase. While no event
is ready, the service sends `: keep-alive` comments about every 15 seconds. Responses set `Cache-Control: no-cache`
and `X-Accel-Buffering: no`.

| Event | Payload |
| --- | --- |
| `turn.started` | `{ "turnId": "uuid", "sessionId": "uuid" }` |
| `assistant.message` | `{ "text": "..." }` |
| `tool.called` | `{ "name": "mastery_status\|mastery_quiz\|mastery_grade\|mastery_assess\|path_outline\|path_reorder\|learner_profile\|knowledge_point_details\|practice_questions\|reading_questions\|save_note" }` |
| `question` | Public question: `questionId`, `knowledgePointId`, `prompt`, `questionType`, `options`, and other public question fields when present. |
| `practice.questions` | `{ "knowledgePointId": "uuid", "questions": [{ "entryId": 1, "prompt": "...", "questionType": "short", "options": [], "difficulty": "easy" }] }`; the event poses saved practice cards and ends the turn. Questions on a reading passage have `knowledgePointId: null` and a `materialId` (the section id). |
| `grading` | For a quiz: `questionId`, `knowledgePointId`, `isCorrect`, `mastery`, `mastered`, `explanation`. For a qualitative assessment: `knowledgePointId`, `passed`, `mastered`, `mastery`. |
| `path.reordered` | `{ "moduleCount": 2, "knowledgePointCount": 4 }` after an actual order change. |
| `profile.updated` | `{ "fields": ["time_budget"] }` when at least one profile field changed. |
| `note.draft` | `{ "title": "...", "body": "...", "sourceType": "KNOWLEDGE_POINT|READING|TUTOR_SESSION", "sourceReferenceId": "uuid" }` (`READING` carries the section id); emitted only when the learner asks to save a note. |
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

A reading session (`readingSectionId`) copies the Content passage once, at creation, with the learner's token;
`GET /sessions/{id}` returns it as `material.instructions` and `material.paragraphs` (`[{ "label": "A", "text": "..." }]`,
at most 20,000 characters, ending in a `[passage truncated]` paragraph when cut). Turns never call Content. Only
Reading sections of published practice sets and lessons can be opened.

Practice tool results sent to the model contain only status, entry ids, and a note. `practice.questions` contains the
knowledge point id and public card fields; neither it nor the tool result contains `expectedAnswer`, `correctAnswer`, or
`explanation`. The `expectedAnswer` field, system prompt, provider key, and raw tool arguments never appear in SSE. The open
question in `GET /sessions/{id}` belongs to that session; its `pendingQuestion` contains the public question and
its status, without the expected answer. A submitted answer is stored before the model runs. Grading updates the
same path and its revision, including when a finalized assessment arrived between tutor turns. If the client
disconnects, the turn continues on the server and the learner can fetch the session again.

When the frontend receives `note.draft`, it immediately sends `POST /api/learning-support/notes` with the event's
`title`, `body`, `sourceType`, and `sourceReferenceId`, using the learner's own valid bearer token. On success it tells
the learner “Đã lưu” and links to the note. On failure it reports the error and offers a retry. The tutor event is a
draft only; AI Learning does not persist it or call Learning Support. A lost draft can be requested again.
