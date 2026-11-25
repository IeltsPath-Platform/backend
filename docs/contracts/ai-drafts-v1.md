# AI drafts v1

An AI draft is a request to the language model and its outcome. A draft never reaches learners. An author approves it,
and approval creates ordinary `DRAFT` content that still needs its own publication step.

All routes require `CONTENT_AUTHOR`. An author sees only drafts they requested. Another author's draft returns
`404`, so the response does not reveal that it exists.

## Lifecycle

`QUEUED` → `GENERATING` → `READY` → `APPROVED` or `REJECTED`. A generation that fails ends in `FAILED`.
A stale `GENERATING` draft returns to `QUEUED` until it reaches `content.ai.max-attempts`; then it fails and returns its use.
Question drafts associated with a lesson, and section question drafts in `READY`, expire to `EXPIRED` after 14 days; this does not refund a use. Older bank question drafts without `lessonId` keep their existing lifecycle.

- Only `READY` drafts can be approved or rejected. Otherwise `400`.
- `FAILED` drafts are not retried in place. The author makes a new request.

## Routes

| Method | Path | Behaviour |
| --- | --- | --- |
| GET | `/api/content/admin/ai-drafts?kind=&status=&page=&size=` | The caller's drafts, newest first. `kind` may repeat. Each summary includes an `origin` and omits `request.source.text`. `size` is 1..100 (default 20). |
| GET | `/api/content/admin/ai-drafts/{id}` | One draft. `404` if missing or not the caller's. |
| POST | `/api/content/admin/ai-drafts/{id}/reject` | Body `{ "note": "…" }` (up to 1000 characters). Only from `READY`. Returns the draft. |
| POST | `/api/content/admin/ai-drafts/questions` | `202`; body `{ knowledgePointId, questionType, count, instructions, lessonId?, blockId? }`. Difficulty is assigned as `MEDIUM`. Lesson requests require a draft lesson and one of its active knowledge points. |
| GET | `/api/content/admin/ai-drafts/questions/open?lessonId=` | The caller's open question draft for the lesson; missing draft returns `404`. One open draft per author and lesson. |
| PATCH | `/api/content/admin/ai-drafts/{id}/items/{itemId}?revision=` | Edits a question draft item by stable id. A stale revision returns `409`. |
| DELETE | `/api/content/admin/ai-drafts/{id}/items/{itemId}?revision=` | Removes an item by stable id and increments the revision. |
| POST | `/api/content/admin/ai-drafts/{id}/approve` | `{ itemIds, revision, createNewBlock? }`; lesson requests attach selected questions to their exercise block (or create one) in the same transaction. Set `createNewBlock: true` to attach to a new exercise block after the requested block was removed. Bank requests keep the existing bank behavior. |
| POST | `/api/content/admin/ai-drafts/section-questions` | `202`; asks for questions from one Reading passage or Listening transcript. Body `{ packageId, versionId, sectionId, questionType, count, instructions }`. |
| GET | `/api/content/admin/ai-drafts/section-questions/open?versionId=` | Open section drafts owned by the caller in this package version. List summaries omit `request.source.text`. |
| GET | `/api/content/admin/ai-drafts/allowance` | `{ limit, used, remaining }` for the caller's daily AI allowance. |
| GET | `/api/content/admin/ai-drafts/section-questions/{draftId}` | Draft plus `sourceChanged` and `sectionState` (`OK`, `MISSING`, `VERSION_NOT_DRAFT`, `SKILL_CHANGED`). Full source is included for the owner. |
| PATCH | `/api/content/admin/ai-drafts/section-questions/{draftId}/items/{itemId}` | Edits one item with a `revision`; every edit re-runs the source and duplicate checks. |
| DELETE | `/api/content/admin/ai-drafts/section-questions/{draftId}/items/{itemId}?revision=` | Drops one item and increments the revision. |
| POST | `/api/content/admin/ai-drafts/section-questions/{draftId}/approve` | `{ itemIds, revision, acceptSourceChange }`; creates selected questions as `DRAFT` and attaches them to the section atomically. |
| POST | `/api/content/admin/ai-drafts/section-questions/{draftId}/reject` | Rejects an owned section draft. |

A list summary includes `origin { type, title, packageId?, versionId?, sectionId?, topicId?, lessonId?, exists, knowledgePointName? }` and `itemCount`; origin is resolved for the page in a batched query. A missing source remains visible with `exists: false`. A draft response has: `id`, `kind` (`QUESTIONS`, `COURSE`, `PRACTICE_SET` or `SECTION_QUESTIONS`), `status`, `request` and `result` as JSON objects
(`result` is `null` until generation finishes), `origin` on list summaries, `errorCode`, `model`, `promptVersion`, `createdAt`, `finishedAt`,
`reviewedAt` and `reviewNote`.

Creating a request and approving a draft are added with the kind-specific features (question drafts and course
drafts). This version provides only the queue, the job and the review routes above.

The model no longer writes `COURSE` drafts: authors write them by hand ([course-drafts-v1](course-drafts-v1.md)) or
import them from Excel. `COURSE` drafts that the model made earlier stay in the table and are read, edited and
approved as before. A `COURSE` draft that was still `QUEUED` when the generator was removed ends in `FAILED` with
`UNSUPPORTED_KIND`, and its use is given back.

## Allowances

Each request takes one use from the author's daily allowance (`content.ai.daily-limit`, default 20). The day is
counted in `content.ai.quota-zone` (default `Asia/Ho_Chi_Minh`). Exhausting the allowance returns `429` with
`details.code = "AI_QUOTA_EXCEEDED"`, and no draft is stored.

A draft that fails for any reason gives its use back. The author received nothing usable for it.

## Failure codes

Stored in `errorCode`, safe to show. They never include the prompt or the model's reply.

| Code | Meaning |
| --- | --- |
| `NOT_CONFIGURED` | `CONTENT_LLM_BASE_URL` or `CONTENT_LLM_MODEL` is empty. |
| `TIMEOUT` | The model did not answer within `content.llm.timeout` (default 180 seconds). |
| `HTTP_<status>` | The model endpoint answered with an error status, for example `HTTP_429` (rate limit) or `HTTP_503` (overloaded). Only the status is kept, never the body. Content calls the LLM as a non-retryable write, so a busy answer fails this draft and returns its use. |
| `FAILED` | The model call failed in another way, such as the endpoint not being reachable. |
| `INVALID_REPLY` | The model answered without usable text: none, or not JSON. |
| `NO_CANDIDATES` | Only on `COURSE` drafts the model made before it stopped writing them: the request matched no knowledge point. |
| `UNSUPPORTED_KIND` | No generator is registered for the draft's kind. |
| `INTERNAL` | An unexpected error in the job. |
| `NO_USABLE_ITEMS` | No returned section question passed validation. |

Section requests support `MOCK_TEST`, `PLACEMENT_TEST`, `PRACTICE_SET`, `TOPIC_TEST` and `COURSE_TEST`, and Listening/Reading only.
Listening reads only its transcript; Reading reads only its passage. The normalized source snapshot and SHA-256 are stored in the request.
The source must contain at least 80 words and at most 20,000 characters. An author may have one open request for a section; a duplicate returns
`409` with `details.code = "SECTION_DRAFT_OPEN"` and `details.draftId`. A request with an unsupported section or type returns `400`;
missing/short/long sources return `422` with `SECTION_SOURCE_REQUIRED`, `SECTION_SOURCE_TOO_SHORT` or `SECTION_SOURCE_TOO_LONG`.
Approving after a source change requires `acceptSourceChange: true`; deleted sources are always refused. Revision conflicts return `409 DRAFT_CHANGED`.
Review validation errors return `422 INVALID_DRAFT_ITEM`; collisions with questions added after generation return `409 DRAFT_ITEMS_DUPLICATED`.

## Configuration

| Key | Default | Meaning |
| --- | --- | --- |
| `content.ai.batch-size` | 2 | Drafts claimed per run. |
| `content.ai.stale-after` | PT10M | A claim older than this is requeued until `content.ai.max-attempts` is reached, then it fails and returns its use. This exceeds the 180-second read timeout plus the 5-second connection timeout for one LLM attempt. |
| `content.ai.max-attempts` | 3 | Maximum number of claims before an interrupted draft fails and its use is returned. |
| `content.ai.daily-limit` | 20 | Uses per author per day. |
| `content.ai.max-items-per-request` | 20 | Upper bound on items in one request. |
| `content.ai.quota-zone` | Asia/Ho_Chi_Minh | Day boundary for allowances. |
| `content.ai.section-questions.min-source-words` | 80 | Minimum source length in whitespace-separated words. |
| `content.ai.section-questions.max-source-chars` | 20000 | Maximum source length; no silent truncation. |
| `content.ai.section-questions.max-count` | 10 | Maximum questions requested for one section. |
| `content.ai.section-questions.duplicate-threshold` | 0.8 | Stem Jaccard threshold. |
| `content.ai.section-questions.same-answer-threshold` | 0.5 | Similarity threshold for questions sharing an answer. |
| `content.ai.section-questions.max-answer-words` | 4 | Maximum words in an accepted typed answer. |
| `content.ai.section-questions.max-copied-words` | 12 | Maximum consecutive source words allowed in a stem. |
| `content.ai.section-questions.max-stem-chars` | 400 | Maximum section question stem length. |
| `content.ai.section-questions.ready-ttl` | P14D | Time a ready section draft remains open before expiry. |
| `content.llm.base-url` | (empty) | OpenAI-compatible base URL. Read from `CONTENT_LLM_BASE_URL`, else `LEARNING_LLM_BASE_URL`. |
| `content.llm.api-key` | (empty) | Read from `CONTENT_LLM_API_KEY`, else `LEARNING_LLM_API_KEY`. Keep it in `.env` only. |
| `content.llm.model` | (empty) | Read from `CONTENT_LLM_MODEL`, else `LEARNING_LLM_MODEL`. |
| `content.llm.timeout` | PT180S | How long to wait for the model's answer (read timeout). Connecting is capped at 10 seconds. |

Several instances may run the job. Each claim increments `attempt_count` and receives a `claim_token` under
`FOR UPDATE SKIP LOCKED`. Completion and failure update only the matching `GENERATING` claim; an older worker cannot
overwrite a replacement worker, and failure returns the allowance only when its conditional update changes one row.
