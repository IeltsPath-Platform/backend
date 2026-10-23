# Question drafts v1

An author asks the AI for questions on one knowledge point. The result is a draft. Approving the chosen items creates
ordinary DRAFT questions that still need their own publication. Learners never see a draft.

All routes require `CONTENT_AUTHOR`. The request and approval use the queue and review rules in `ai-drafts-v1.md`.

## Request

`POST /api/content/admin/ai-drafts/questions` with body:

```json
{ "knowledgePointId": "<uuid>", "questionType": "MULTIPLE_CHOICE", "count": 5,
  "difficulty": "MEDIUM", "instructions": "optional, up to 500 characters" }
```

- `questionType` is one of `MULTIPLE_CHOICE`, `TRUE_FALSE_NOT_GIVEN`, `FILL_IN_BLANK`, `SHORT_ANSWER`.
  Other types, including `ESSAY` and `SPEAKING`, are not generated and return `400`.
- `count` is 1 up to `content.ai.max-items-per-request`. `difficulty` is required.
- The knowledge point must exist and be active (`404` or `400` otherwise).
- Returns `202` with the queued draft. An exhausted allowance returns `429 AI_QUOTA_EXCEEDED`.

The prompt contains only the knowledge point's name and description, the request fields and the author's
instructions. It contains no learner data and no existing question text.

## What a ready draft holds

`result.items` lists the usable items. Each has `stem`, `options` (`optionKey`, `content`, `sortOrder`),
`answerSpec` built by the server (`{"type":"CHOICE","correct":…,"distractors":…}` or `{"type":"FILL","accepted":[…]}`),
`explanation` and `difficulty` (always the requested one).

`result.rejected` lists the items that were dropped, each with `index` and `reason`:

| Reason | Meaning |
| --- | --- |
| `NOT_AN_OBJECT` | The item is not a JSON object. |
| `BAD_STEM` | Stem missing, blank or longer than 4000 characters. |
| `NO_EXPLANATION` | Explanation missing. |
| `BAD_OPTIONS` | Choice item with fewer than 2 or more than 6 options, or repeated/blank keys. |
| `CORRECT_NOT_AN_OPTION` | The correct key is not one of the options. |
| `DISTRACTOR_NOT_A_WRONG_OPTION` | A distractor names the correct key or an unknown key. |
| `UNKNOWN_ERROR_CODE` | A distractor code is not an active, approved error type. |
| `NO_ACCEPTED_ANSWER` | Typed item without a non-blank accepted answer. |
| `DUPLICATE_STEM` | The stem equals, ignoring case and spacing, a stem the knowledge point already has or one kept earlier in the same reply. |

If no item is usable, the draft fails with `INVALID_REPLY`.

## Edit and remove

Only a `READY` question draft of its author can change; otherwise `400` (not ready) or `404` (not found or not theirs).
Items are addressed by their position in `result.items`, counted from 0. A position that does not exist returns `404`.

`PATCH /api/content/admin/ai-drafts/{id}/items/{index}` changes one item. Every field is optional and a field left out
stays as it is:

```json
{ "stem": "…", "explanation": "…",
  "options": [{ "optionKey": "A", "content": "…" }, { "optionKey": "B", "content": "…" }],
  "correct": "B", "distractors": { "A": "PARAPHRASE_TRAP" }, "accepted": ["colour", "color"] }
```

`options` and `distractors` replace the whole list or map (`"distractors": {}` clears the error codes); sort orders are
numbered again from 1. `difficulty` and the question type stay those of the request. The edited item must pass the
same rules as a generated one, with the reasons above (`DUPLICATE_STEM` also counts the other items of the draft, but
not the edited item's own stem). A broken edit returns `422` with `details.code = INVALID_DRAFT_ITEM` and
`details.reason`, and the draft is unchanged. Oversize text (a stem or explanation over 4000 characters, more than 6
options) returns `400`. Returns `200` with the draft.

`DELETE /api/content/admin/ai-drafts/{id}/items/{index}` drops one item and returns `200` with the draft. The items
behind it move up by one position, so a client uses the returned `result.items` for the next call. `result.rejected` is
left as it was, because its `index` refers to the model's original reply.

## Approve

`POST /api/content/admin/ai-drafts/{id}/approve` with body `{ "itemIndexes": [0, 2] }`.

- Only a `READY` question draft can be approved; otherwise `400`. Indexes must refer to existing items; otherwise `400`.
- Each chosen item becomes a question in `DRAFT` with `importRef = "ai-<draftId>-<index>"`, through the same use cases
  as every other question. The answer spec is checked again there, so a rule that changed since generation still applies.
- All or nothing: if one item fails, nothing is created and the draft stays `READY`.
- The draft becomes `APPROVED` and records the reviewer. Returns `200` with the draft.
