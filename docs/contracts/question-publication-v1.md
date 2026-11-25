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
| `code` | Optional. Case-insensitive partial match against the stable question code. |
| `knowledgePointIds` | Repeatable, up to 50 UUIDs. Keeps questions whose current published version is related to at least one listed knowledge point. Filter runs in the database before pagination. |
| `page` | Zero-based page index. Default `0`. |
| `size` | Page size, `1` to `100`. Default `25`. |
| `include` | Optional, comma-separated `ownership` and/or `preview`. Unknown values return `400`. Without this parameter the response shape and query behavior are unchanged. |
| `exceptBlockId` | Optional lesson block UUID. With `include=ownership` or `onlyFree=true`, a question used only by this block is not treated as owned elsewhere. |
| `onlyFree` | Optional boolean, default `false`. When `true`, filters out versions owned by another lesson block or any package section before pagination. A version in `exceptBlockId` is allowed. |

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

Each item includes `code` along with (`id`, `questionType`, `skill`, `accessLevel`, `status`,
`currentPublishedVersionId`, `createdAt`, `updatedAt`, `purpose`, `importRef`). The detail response also includes `code`.
`totalPages` is `0` when nothing
matches. A page beyond the end returns an empty `content` with the real `totalElements`.

When requested with `include=ownership`, each item adds `ownedElsewhere`, which is `true` when its published version
belongs to another lesson block or a package section; an unpublished question or one used only by `exceptBlockId` is
`false`. When requested with `include=preview`, each item adds `stemExcerpt` (at most 160 characters, cut at a word
boundary and ending in `…` when shortened; null when there is no published version) and `knowledgePoints` (up to four
`{ "code", "name" }` objects ordered by weight descending, then code ascending). `knowledgePointTotal` is included
when the version has more than four knowledge points. These additional fields are omitted when their corresponding
include is not requested. `onlyFree` can be used independently of `include`.

**Changed from the earlier version of this route:** the response was a bare JSON array and ignored `page`, `size` and
`status`. Clients must read `content`. The only caller in this repository is the `/content-author` question bank.

### Import reference

Questions may carry an optional `importRef`: a stable key used by spreadsheet imports and AI drafts to refer to a
question. It is trimmed; blank means none. Allowed characters are letters, digits, dot, underscore and dash, up to 100
characters; anything else returns `400`. It is unique across questions (`409` with `details.code = "IMPORT_REF_CONFLICT"`
when another question has it). Learners never see it.

Every question has a stable generated `code`. Existing questions are numbered by `createdAt`, then `id`; new questions
use the configured `QUESTION` pattern (default `Q-{SEQ:6}`). Codes are included in create, list, detail, new-version and
archive responses. List searches can filter by code with the `code` query parameter.

- `POST /api/content/admin/questions` accepts `importRef` and returns it.
- The list, create, new-version and archive responses include `importRef`, `null` when unset. The detail response
  (`GET /api/content/admin/questions/{id}`) does not include it yet.
- `GET /api/content/admin/questions?importRef=<ref>` returns the question with exactly that reference in the same page
  shape, with at most one item in `content` (`page` `0`, `size` `100`). When this parameter is present, `skill`,
  `purpose`, `status`, `page` and `size` are ignored. `include`, `exceptBlockId` and `onlyFree` are also ignored for
  this exact-reference lookup.

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

## Passages and recordings of a question version

`GET /api/content/admin/questions/versions/{versionId}/passages` (`CONTENT_AUTHOR`) returns the reading passages and the recordings
that go with one version, so an author can see a question in its context; empty when it has none. A question holds no link to
them: an asset is found when it is linked to the version itself (`source` `DIRECT`), is the nearest `ASSET` block above the lesson
exercise block that holds the version (`LESSON`), or is linked to a section that holds the version (`SECTION`). Each asset appears
once, most direct source first, at most 5.

Each item is `{ assetId, text, source, sourceName, type, mediaUrl, durationSeconds }`. `type` is `PASSAGE` (`text` is the passage) or
`AUDIO` (`text` is the transcript and may be null; a recording without a stored file is left out). `mediaUrl` is a playable link for
an `AUDIO` item: public audio uses the public media policy, private (`PREMIUM`) audio gets a signed link that expires in the
configured premium window and is never stored. When a link cannot be made the item keeps its transcript and `mediaUrl` is null.
`type`, `mediaUrl` and `durationSeconds` were added on 2026-11-24; earlier responses carried only passages.

`type` can also be `IMAGE` (added 2026-11-24): the prompt image of a Writing question, linked directly to the version. `text` is its
description (may be null) and `mediaUrl` the link that shows it (null when it cannot be made). Clients must accept the new value.

## Prompt image of a Writing question version

`PUT /api/content/admin/questions/{questionId}/versions/{versionId}/image` (`CONTENT_AUTHOR`), body
`{ "mediaReference": "<key returned by POST /api/content/admin/media/image>", "altText": "..." }` (`altText` optional, at most 500
characters), sets the image and replaces any earlier one. `DELETE` on the same path removes it. Both return `204`.

Only a `DRAFT` or `PUBLISHED` version of a `WRITING` question changes (a published version too since 2026-11-24, because a Writing
task in an exam cannot swap its question): an archived version returns `409` and another skill returns `400`, each with a Vietnamese
message. The question is locked while the image changes, so it cannot race a publication or an archive. Replacing or removing
keeps the old asset row, because another version may still use it. A new version of a question starts with the image of the latest earlier
version that has one (since 2026-11-24); the asset is shared, so replacing the image on the new version leaves the old one untouched.

## Where a question is used

`GET /api/content/admin/questions/{questionId}/usages` (`CONTENT_AUTHOR`, added 2026-11-25) lists every place a version of the question is held: a section of a package version or an exercise block of a lesson. Each item is `{ kind, targetId, targetCode, targetTitle, location, targetVersionNumber, targetStatus, questionVersionId, questionVersionNumber }`. `kind` is `PACKAGE` (`location` is the section title, `targetVersionNumber` and `targetStatus` describe the package version) or `LESSON` (`location` is `Kh?i <n>`, `targetVersionNumber` is null, `targetStatus` is the lesson status). Packages come first, newest package version first. An unused question returns an empty array.

