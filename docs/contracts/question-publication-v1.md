# Question publication API v1

Question authoring routes require `CONTENT_AUTHOR`.

## List questions

`GET /api/content/admin/questions` returns one page of question catalog records, newest first (`createdAt` descending,
then `id`). The equivalent `/api/content/questions` alias behaves the same. Both require `CONTENT_AUTHOR`.

| Query parameter | Meaning |
| --- | --- |
| `skill` | Optional. `LISTENING`, `READING`, `WRITING`, `SPEAKING` or `ALL`. |
| `purpose` | Optional. `LEARNING`, `EXAM` or `PLACEMENT`. |
| `status` | Optional. `DRAFT`, `PUBLISHED` or `ARCHIVED` (the question's own status). |
| `page` | Zero-based page index. Default `0`. |
| `size` | Page size, `1` to `100`. Default `25`. |

Filters combine with AND and run in the database. An unknown enum value, a negative `page` or a `size` outside `1..100`
returns `400`.

```json
{
  "content": [ { "id": "…", "questionType": "MULTIPLE_CHOICE", "skill": "READING", "status": "PUBLISHED", "…": "…" } ],
  "page": 0,
  "size": 25,
  "totalElements": 57,
  "totalPages": 3
}
```

Each item has the same fields the route returned before (`id`, `questionType`, `skill`, `accessLevel`, `status`,
`currentPublishedVersionId`, `createdAt`, `updatedAt`, `purpose`, `importRef`). `totalPages` is `0` when nothing
matches. A page beyond the end returns an empty `content` with the real `totalElements`.

**Changed from the earlier version of this route:** the response was a bare JSON array and ignored `page`, `size` and
`status`. Clients must read `content`. The only caller in this repository is the `/content-author` question bank.

### Import reference

Questions may carry an optional `importRef`: a stable key used by spreadsheet imports and AI drafts to refer to a
question. It is trimmed; blank means none. Allowed characters are letters, digits, dot, underscore and dash, up to 100
characters; anything else returns `400`. It is unique across questions (`409` with `details.code = "IMPORT_REF_CONFLICT"`
when another question has it). Learners never see it.

- `POST /api/content/admin/questions` accepts `importRef` and returns it.
- The list, create, new-version and archive responses include `importRef`, `null` when unset. The detail response
  (`GET /api/content/admin/questions/{id}`) does not include it yet.
- `GET /api/content/admin/questions?importRef=<ref>` returns the question with exactly that reference in the same page
  shape, with at most one item in `content` (`page` `0`, `size` `100`). When this parameter is present, `skill`,
  `purpose`, `status`, `page` and `size` are ignored.

## Publish a question version

`POST /api/content/admin/questions/{questionId}/versions/{versionId}/publish` publishes a `DRAFT` version and sets it
as the question's `currentPublishedVersionId`. It returns `200` with the updated question detail, including the new
version status. Publication requires a valid `answerSpec` as defined by [answer-spec-v1](answer-spec-v1.md), a
non-null difficulty, and, for `ESSAY`, a valid `task` and `passBand` (4.0–9.0 in half-band steps). `TASK_1` also
requires `chartFacts` with 1–2,000 characters. Invalid content returns `422` with `details.code` set to
`QUESTION_VERSION_NOT_PUBLISHABLE`; missing difficulty returns `422` with `details.code = DIFFICULTY_REQUIRED`.
Unknown questions return `404`; a version that is unknown or belongs to another question returns `404`; attempting to
publish a version outside `DRAFT` returns `409` with `details.code = QUESTION_VERSION_STATE_INVALID`.

```json
{
  "id": "<question-id>",
  "questionType": "MULTIPLE_CHOICE",
  "skill": "READING",
  "accessLevel": "FREE",
  "status": "PUBLISHED",
  "currentPublishedVersionId": "<version-id>",
  "createdAt": "2026-10-22T00:00:00Z",
  "updatedAt": "2026-10-22T00:00:00Z",
  "versions": [
    { "id": "<version-id>", "questionId": "<question-id>", "versionNumber": 1, "status": "PUBLISHED" }
  ],
  "purpose": "LEARNING"
}
```

## Archive a question version

`POST /api/content/admin/questions/{questionId}/versions/{versionId}/archive` archives a `PUBLISHED` version and
returns `200` with the updated question detail. A version linked to a `PUBLISHED` lesson or a `PUBLISHED` content
package cannot be archived and returns `409` with `details.code = QUESTION_VERSION_IN_USE`. Unknown questions or
versions return `404`; a version that belongs to another question returns `404`; a non-published version returns
`409` with `details.code = QUESTION_VERSION_STATE_INVALID`.

The question's current published pointer is cleared when it points to the archived version. The question becomes
`DRAFT` when no other published version remains; otherwise its pointer is moved to the most recently published
remaining version.

```json
{
  "id": "<question-id>",
  "questionType": "MULTIPLE_CHOICE",
  "skill": "READING",
  "accessLevel": "FREE",
  "status": "DRAFT",
  "currentPublishedVersionId": null,
  "createdAt": "2026-10-22T00:00:00Z",
  "updatedAt": "2026-10-22T00:00:00Z",
  "versions": [
    { "id": "<version-id>", "questionId": "<question-id>", "versionNumber": 1, "status": "ARCHIVED" }
  ],
  "purpose": "LEARNING"
}
```
