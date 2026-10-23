# Content package authoring v1

All routes below require authenticated `CONTENT_AUTHOR`. They extend existing package creation,
preview and publication APIs without replacing `/api/content/packages` routes. Only drafts can
be edited through these routes. The old add-section route now enforces the same draft invariant.

## Errors and ownership

Errors use the existing Content `ErrorResponse` envelope (`timestamp`, `status`, `error`,
`message`, `path`, `details`). Machine-readable codes are in `details.code`.

- Missing package, section or referenced question version: `404`.
- Version absent from the requested package, or section belonging to a different version: `400`.
- Editing a non-`DRAFT` version: `400 VERSION_NOT_DRAFT`, including the legacy add-section endpoint.
- Existing draft: `409 DRAFT_EXISTS`. A duplicate question version anywhere in the same package
  version: `409 QUESTION_ALREADY_IN_VERSION`.
- Invalid fields, malformed JSON and incomplete/duplicate/foreign ordering IDs: `400`.
- Authenticated `CUSTOMER`, `EXAMINER` and `ADMIN` without `CONTENT_AUTHOR`: `403`.

## Implementation status (2026-10-23)

Implemented: author detail (section 1, partial), create draft (section 2), section create/update/delete and
ordering (section 3). Not yet implemented: the list route `GET /api/content/admin/packages`, section questions
(section 4), passage and audio (section 5), audio upload (section 6), and package creation for
`TOPIC_TEST`/`COURSE_TEST` (plan phase 01, section 7). Until those land, the author detail response omits
`questionId`, `stem`, `status`, `purpose`, `passage` and `audio`; questions appear only as
`{id, questionVersionId, sortOrder, maxScore}`.

The detail response has the shape `{id, code, title, packageType, accessLevel, status,
currentPublishedVersionId, lessonId, courseId, sourceCode, createdAt, updatedAt, versions[]}`. Each version
has `id, versionNumber, status, rulesJson, publishedAt, publishAt, lastPublishError, sections[]`, and each
section has `id, title, skill, sortOrder, timeLimitSeconds, partNo, coverAssetId, instructions, questions[]`.

Section sort order is unique within a version. Creating or updating a section with a sort order that another
section of the same version holds returns `400`. Reordering is one transaction: the sort-order uniqueness check
is deferred to commit (migration V74), so the whole sequence is written together.

Using a section from another version of the same package returns `400`; a section that does not exist in the
package returns `404`. Every write checks that the target version is a draft before it looks at the section, so
a published version returns `400 VERSION_NOT_DRAFT` regardless of section IDs.

## 1. Author detail and catalog

`GET /api/content/admin/packages/{packageId}` returns:

```json
{
  "id": "<uuid>", "code": "READING-PRACTICE-1", "title": "Reading practice",
  "packageType": "PRACTICE_SET", "accessLevel": "FREE", "status": "PUBLISHED",
  "currentPublishedVersionId": "<uuid>", "lessonId": "<uuid or null>",
  "sourceCode": null, "createdAt": "2026-10-23T00:00:00Z", "updatedAt": "2026-10-23T00:00:00Z",
  "versions": [{
    "id": "<uuid>", "versionNumber": 2, "status": "DRAFT", "rulesJson": "{}",
    "publishedAt": null, "publishAt": null, "lastPublishError": null,
    "sections": [{
      "id": "<uuid>", "title": "Reading Passage 1", "skill": "READING", "sortOrder": 1,
      "timeLimitSeconds": 1200, "partNo": 1, "coverAssetId": null, "instructions": "Read and answer.",
      "passage": "Passage text",
      "audio": {
        "assetId": "<uuid>", "mediaReference": "listening/demo/ls1.mp3",
        "mediaUrl": "https://media.example/listening/demo/ls1.mp3",
        "durationSeconds": 45, "transcript": null
      },
      "questions": [{
        "id": "<section-question uuid>", "questionVersionId": "<uuid>", "questionId": "<uuid>",
        "sortOrder": 1, "maxScore": 1.0, "stem": "Question text", "status": "PUBLISHED", "purpose": "LEARNING"
      }]
    }]
  }]
}
```

Missing passage/audio are `null`; collection fields are arrays, including when empty. Versions
are ordered by increasing `versionNumber`, sections/questions by increasing `sortOrder` with
ID as tie-breaker. Question stems contain at most 200 Unicode characters; status comes from the
question version and purpose from its question. Answers are available only through author preview.
Audio URLs use the same `MediaReferencePolicy` as preview; raw audio keys and optional transcripts
remain author-only. Asset selection uses the first link by `sort_order` and ID for each asset type.
The reader loads relations in bounded bulk queries, never one query per version/section/question.

`GET /api/content/admin/packages?type=&status=&lessonId=&q=` returns an array of existing
`ContentPackageResponse` fields plus `latestVersionNumber` (null if no version) and `hasDraft`
(boolean). Optional `type` is a `PackageType`; `status` is a publication status; `lessonId` is a UUID.
`q` matches code or title case-insensitively as a literal substring. Results are limited to 500,
ordered by decreasing `updatedAt` then ID. There is no pagination in v1.

## 2. Create a draft version

`POST /api/content/admin/packages/{packageId}/versions`, body `{ "rulesJson": "{}" }`.

Returns `201` with the version shape above and `sections: []`. The server chooses maximum existing
version number plus one (one for an empty package). At most one draft may be created per package
through this endpoint; an existing draft returns `409 DRAFT_EXISTS`. `rulesJson` defaults to `{}`
and must contain valid JSON. Draft creation is serialized per package.

The legacy `POST /api/content/packages/{id}/versions` keeps its existing explicit-number behavior.

## 3. Sections

Prefix: `/api/content/admin/packages/{packageId}/versions/{versionId}`.

- `POST /sections`: `201`, returns the new section.
- `PUT /sections/{sectionId}`: `200`, returns the updated section.
- `DELETE /sections/{sectionId}`: `204`. Removes its question rows and asset links, retaining assets.
- `PUT /sections/order`: body `{ "sectionIds": ["<uuid>"] }`, `204`. IDs must be the exact section
  set, each once. Assigns contiguous sort orders `1..n` atomically.

Create/update use the existing `AddContentSectionRequest` fields: required `title` (up to 255
characters), optional `skill`, integer `sortOrder`, optional `timeLimitSeconds`, `instructions`,
`partNo` (1..4) and `coverAssetId`. Section responses include these fields plus `id`, passage,
audio and questions as in detail. A conflicting section sort order returns `400`.

The legacy `POST /api/content/packages/versions/{versionId}/sections` also rejects non-drafts.

## 4. Section questions

Prefix: `/api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}`.

- `POST /questions`: `{ "questionVersionId": "<uuid>", "maxScore": 1.0 }`, `201` with the
  question row from detail. Adds after the highest existing sort order; `maxScore` defaults to
  1.0 and must be positive. A question version already in any section of this version returns
  `409 QUESTION_ALREADY_IN_VERSION`; missing question version returns `404`.
- `DELETE /questions/{sectionQuestionId}`: `204`, reassigns remaining sort orders `1..n`.
- `PUT /questions/order`: `{ "sectionQuestionIds": ["<uuid>"] }`, `204`; requires exactly all
  current rows, each once, and assigns sort orders `1..n` atomically.

New rows have `group_id = null`. Adding a question does not check its publication state, purpose
or use in another published package: existing publication validation remains authoritative.
It continues returning `422 QUESTION_ALREADY_USED`, `QUESTION_PURPOSE_MISMATCH` and
`INVALID_PACKAGE_LESSON` according to the existing publish contract.

## 5. Section passage and audio

Use the section prefix above; all operations require a draft and are transactional.

- `PUT /passage`: `{ "text": "Passage text" }`, required nonblank text, maximum 50,000
  characters. Creates a new `PASSAGE` asset, removes every old passage link for this section
  and links the new asset at sort order zero. `200 { "passage": "Passage text" }`.
- `DELETE /passage`: `204`; removes passage links, retaining all assets.
- `PUT /audio`: `{ "mediaReference": "listening/demo/ls1.mp3", "durationSeconds": 45,
  "transcript": null }`. Validates an object key using `ObjectKeys.requireSafe` in the application
  layer, duration `1..7200` seconds. Both uploaded keys and existing seed keys are accepted.
  Creates a new `AUDIO` asset (`text_content` stores the transcript), replaces every old audio
  link, links at order zero and returns `200` with the detail audio object (including resolved URL).
- `DELETE /audio`: `204`; removes audio links, retaining assets and stored files.

No replaced asset is deleted: another published version may still reference it. Invalid audio
keys (including `..`) return `400`. Media URL configuration errors retain the existing media
policy behavior; file keys require `CONTENT_MEDIA_BASE_URL` to resolve.

## 6. Audio upload

`POST /api/content/admin/media/audio`, multipart field `file`, returns `201`:

```json
{
  "mediaReference": "content-audio/<user uuid>/<random uuid>.mp3",
  "mediaUrl": "https://storage.example/storage/v1/object/public/media/content-audio/<user uuid>/<random uuid>.mp3",
  "contentType": "audio/mpeg", "sizeBytes": 2483021
}
```

The upload stores the file only; the author subsequently calls `PUT /audio` to attach its key.
`UploadPolicy` checks magic bytes and MIME, allowing `audio/mpeg`, `audio/mp4`, `audio/wav`,
`audio/ogg`, with maximum size 25 MiB (25 × 1024 × 1024 bytes). A renamed image is rejected.
The server does not extract duration or add audio-processing dependencies; the frontend reads
`loadedmetadata` and supplies duration when attaching the audio.

Keys are generated with `ObjectKeys.forUser("content-audio", userId, ext)` using authenticated
`CurrentUserProvider`, with random filenames, in `content.media.bucket` (default `media`).
Audio binaries, secrets and user filenames are never committed or logged.

| Failure | HTTP | `details.code` |
| --- | --- | --- |
| Empty file | 400 | optional |
| Disallowed MIME or mismatched magic bytes | 400 | `UNSUPPORTED_AUDIO` |
| Larger than 25 MiB, including multipart rejection | 413 | optional |
| Unconfigured or failed storage | 503 | `STORAGE_UNAVAILABLE` |

Storage logs contain only operation and reason, never a filename, object key or storage credential.
No test contacts live Supabase or a live LLM.

### Configuration and compatibility

Content depends on the existing `common-storage` module. Runtime config enables `app.storage`,
uses `${SUPABASE_URL:}` and `${SUPABASE_SECRET_KEY:}` and exposes bucket `${CONTENT_MEDIA_BUCKET:media}`.
Both Compose variables default to empty (`${SUPABASE_URL:-}`, `${SUPABASE_SECRET_KEY:-}`): when
both are missing, `UnavailableObjectStorage` lets the service start and uploads return `503`.
Providing only one is an invalid storage configuration. Multipart limits are `25MB` per file,
`26MB` per request. Reads still use the configured `CONTENT_MEDIA_BASE_URL` public prefix.

Backlog: unreferenced upload cleanup, section-question-group editing, bulk import, package
deletion and cloning old versions. None is implemented by this contract.
