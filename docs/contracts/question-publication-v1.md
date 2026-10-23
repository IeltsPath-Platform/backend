# Question publication API v1

Question authoring routes require `CONTENT_AUTHOR`.

## List questions

The existing `GET /api/content/admin/questions` route supports optional `skill` and `purpose` filters and returns an
array of question catalog records. The equivalent `/api/content/questions` alias remains available. Both require
`CONTENT_AUTHOR`.

### Import reference

Questions may carry an optional `importRef`: a stable key used by spreadsheet imports and AI drafts to refer to a
question. It is trimmed; blank means none. Allowed characters are letters, digits, dot, underscore and dash, up to 100
characters; anything else returns `400`. It is unique across questions (`409` with `details.code = "IMPORT_REF_CONFLICT"`
when another question has it). Learners never see it.

- `POST /api/content/admin/questions` accepts `importRef` and returns it.
- The list, create, new-version and archive responses include `importRef`, `null` when unset. The detail response
  (`GET /api/content/admin/questions/{id}`) does not include it yet.
- `GET /api/content/admin/questions?importRef=<ref>` returns the question with exactly that reference as a list of at
  most one item. When this parameter is present, `skill` and `purpose` are ignored.

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
