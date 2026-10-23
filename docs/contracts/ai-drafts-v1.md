# AI drafts v1

An AI draft is a request to the language model and its outcome. A draft never reaches learners. An author approves it,
and approval creates ordinary `DRAFT` content that still needs its own publication step.

All routes require `CONTENT_AUTHOR`. An author sees only drafts they requested. Another author's draft returns
`404`, so the response does not reveal that it exists.

## Lifecycle

`QUEUED` → `GENERATING` → `READY` → `APPROVED` or `REJECTED`. A generation that fails ends in `FAILED`.
A `GENERATING` draft that has been running longer than `content.ai.stale-after` returns to `QUEUED`.

- Only `READY` drafts can be approved or rejected. Otherwise `400`.
- `FAILED` drafts are not retried in place. The author makes a new request.

## Routes

| Method | Path | Behaviour |
| --- | --- | --- |
| GET | `/api/content/admin/ai-drafts?kind=&status=&page=&size=` | The caller's drafts, newest first. `size` is 1..100 (default 20). Filters are optional. |
| GET | `/api/content/admin/ai-drafts/{id}` | One draft. `404` if missing or not the caller's. |
| POST | `/api/content/admin/ai-drafts/{id}/reject` | Body `{ "note": "…" }` (up to 1000 characters). Only from `READY`. Returns the draft. |

A draft response has: `id`, `kind` (`QUESTIONS` or `COURSE`), `status`, `request` and `result` as JSON objects
(`result` is `null` until generation finishes), `errorCode`, `model`, `promptVersion`, `createdAt`, `finishedAt`,
`reviewedAt` and `reviewNote`.

Creating a request and approving a draft are added with the kind-specific features (question drafts and course
drafts). This version provides only the queue, the job and the review routes above.

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
| `FAILED` | The model call failed or timed out. |
| `INVALID_REPLY` | The model answered without usable text: none, not JSON, or (for a course plan, after a second try) nothing that fits the candidate set. |
| `NO_CANDIDATES` | A course request matched no knowledge point, so there was nothing to plan from. |
| `UNSUPPORTED_KIND` | No generator is registered for the draft's kind. |
| `INTERNAL` | An unexpected error in the job. |

## Configuration

| Key | Default | Meaning |
| --- | --- | --- |
| `content.ai.batch-size` | 2 | Drafts claimed per run. |
| `content.ai.stale-after` | PT10M | Generating longer than this is requeued. |
| `content.ai.daily-limit` | 20 | Uses per author per day. |
| `content.ai.max-items-per-request` | 20 | Upper bound on items in one request. |
| `content.ai.quota-zone` | Asia/Ho_Chi_Minh | Day boundary for allowances. |
| `content.llm.base-url` | (empty) | OpenAI-compatible base URL. Read from `CONTENT_LLM_BASE_URL`. |
| `content.llm.api-key` | (empty) | Read from `CONTENT_LLM_API_KEY`. Keep it in `.env` only. |
| `content.llm.model` | (empty) | Read from `CONTENT_LLM_MODEL`. |
| `content.llm.timeout` | PT60S | Connect and read timeout for the model call. |

Several instances may run the job. Claiming uses row locks that skip rows another instance holds, so no draft is
generated twice.
