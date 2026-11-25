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

Everything in sections 1 to 7 below is implemented. The detail response has the shape `{id, code, title,
packageType, accessLevel, status, currentPublishedVersionId, lessonId, topicId, courseId, sourceCode, placementPool, createdAt,
updatedAt, versions[]}`. Each version has `id, versionNumber, status, rulesJson, publishedAt, publishAt,
lastPublishError, sections[]`, and each section has `id, title, skill, sortOrder, timeLimitSeconds, partNo,
coverAssetId, instructions, passage, audio, questions[]`. Each question has `id, questionVersionId, questionId,
sortOrder, maxScore, stem, status, purpose`.

Two responses carry less than the detail: the response of `POST /versions` (a version with `sections: []`) and the
responses of the section routes, which return a section without `passage`, `audio` and question details (they are
`null`). Read the detail again to get them.

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
  "topicId": null, "courseId": null, "placementPool": null,
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

`GET /api/content/admin/packages?type=&status=&lessonId=&topicId=&courseId=&q=&placementPool=&page=&size=` returns one page,
`{ "content": [...], "page", "size", "totalElements", "totalPages" }`. `page` is zero-based (default `0`) and `size` is `1`
to `100` (default `25`); a value outside those ranges returns `400`. `totalElements` counts every package that matches the
filters. **Changed from the earlier version of this route:** the response was a bare array limited to the 500 most
recently updated packages, so older packages could not be listed at all. Each item has the
`ContentPackageResponse` fields plus `topicId`, `courseId`, `latestVersionNumber` (null if no version) and
`hasDraft` (boolean) and `placementPool` (the pool enum for placement packages, null for others). Optional `placementPool` filters by `BAND_0_3`, `BAND_3_5` or `BAND_5_9` (added 2026-11-17). Optional `type` is a `PackageType`; `status` is a publication status; `lessonId`,
`topicId` and `courseId` are UUIDs. `q` matches code or title case-insensitively as a literal substring (`%`
and `_` are not wildcards). Results are ordered by decreasing `updatedAt` then ID, so pages do not overlap. An unknown `type`, `status` or `placementPool` value returns `400`.

## 2. Create a draft version

`POST /api/content/admin/packages/{packageId}/versions`, body `{ "rulesJson": "..." }` (optional).

Returns `201` with the version shape above and `sections: []`; the response does not list the copied
sections, so a client reads the package again. The server chooses maximum existing version number plus
one (one for an empty package). At most one draft may be created per package through this endpoint; an
existing draft returns `409 DRAFT_EXISTS`. `rulesJson` must contain valid JSON when sent. Draft creation
is serialized per package.

**Change from the previous version of this contract: the draft is no longer empty.** It starts as a copy
of the version learners currently get (`currentPublishedVersionId`); a package that was never published
uses the highest numbered non-draft version; a package with no earlier version gets an empty draft. The
copy has new ids for sections, question groups, question links and passage/audio links, in the same
transaction as the draft. The questions and assets themselves are shared with the source version, not
duplicated. When `rulesJson` is omitted or blank the draft takes the source version's rules (`{}` when
there is no source); an explicit value is kept. A scheduled publication time is not copied.

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
audio and questions as in detail. A conflicting section sort order returns `400`. A `partNo` can be used once per skill
in a version (Listening Part 1 and Reading Passage 1 may coexist); sections with no skill share one group. Before
2026-11-24 it was unique across the whole version.

The legacy `POST /api/content/packages/versions/{versionId}/sections` also rejects non-drafts.

## 4. Section questions

Prefix: `/api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}`.

- `POST /questions`: `{ "questionVersionId": "<uuid>", "maxScore": 1.0 }`, `201` with the
  question row from detail. Adds after the highest existing sort order; `maxScore` defaults to
  1.0 and must be positive. A question version already in any section of this version returns
  `409 QUESTION_ALREADY_IN_VERSION`; missing question version returns `404`. A section whose
  skill is `WRITING` holds one question: adding a second returns `400` (added 2026-11-24).
- `PUT /questions/{sectionQuestionId}/newer-version` (added 2026-11-24): no body, `200` with the question row.
  Moves the row to the newest `PUBLISHED` version of the same question, keeping its order, score and group; a
  draft package version only. A question with no newer published version returns `400`. Each question row in the
  detail carries `newerVersionNumber`, the number of that newer version or `null`.
- `POST /api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}/questions/publish-drafts`: publishes all attached question versions that are currently `DRAFT`, after locking the package and question rows in question-ID order. Returns `{ "publishedCount": n }`; no draft questions returns zero. Validation is completed for the full set before any version changes. A validation failure returns `422 QUESTION_VERSION_NOT_PUBLISHABLE` and no version is published.
- `DELETE /questions/{sectionQuestionId}`: `204`, reassigns remaining sort orders `1..n`. The only question of a
  `WRITING` section cannot be removed (`400`, added 2026-11-24): the prompt is edited, not swapped.
- `PUT /questions/order`: `{ "sectionQuestionIds": ["<uuid>"] }`, `204`; requires exactly all
  current rows, each once, and assigns sort orders `1..n` atomically.

New rows have `group_id = null`. Adding a question does not check its publication state, purpose
or use in another published package: existing publication validation remains authoritative.
It continues returning `422 QUESTION_ALREADY_USED`, `QUESTION_PURPOSE_MISMATCH` and
`INVALID_PACKAGE_LESSON` according to the existing publish contract.

Package publication returns `422 QUESTION_NOT_PUBLISHED` if any question version used by the target version is not `PUBLISHED`; the error identifies affected question versions. This applies to immediate and scheduled publication.

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
| Empty file | 400 | `EMPTY_FILE` |
| Disallowed MIME or mismatched magic bytes | 400 | `UNSUPPORTED_AUDIO` |
| Larger than 25 MiB, including multipart rejection | 413 | `AUDIO_TOO_LARGE` |
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

## 7. Create a package, including topic and course final tests

`POST /api/content/admin/packages`, body:

```json
{ "code": "X5", "title": "Final test", "packageType": "TOPIC_TEST", "accessLevel": "FREE",
  "lessonId": null, "sourceCode": null, "topicId": "<uuid>", "courseId": null }
```

Returns `201` with the detail shape of section 1 and `versions: []`. The package is created as `DRAFT` and has no
version; call `POST /versions` next. `title` (up to 255) is required. `code` may be omitted or blank while package code
generation is enabled; the service generates it from the associated course and package type, or uses the fixed
`PKG_{TYPE}{SEQ:3}` fallback when no course can be inferred. When generation is disabled, `code` (up to 100 characters)
is required. An enabled locked setting rejects a manually supplied code that differs from the current suggestion with
`409 CODE_LOCKED`.

- `packageType` is any of `PRACTICE_SET`, `PRACTICE_TEST`, `MOCK_TEST`, `PLACEMENT_TEST`, `QUIZ`, `TOPIC_TEST`,
  `COURSE_TEST`. `LESSON` returns `400`.
- `TOPIC_TEST`: `topicId` is required and the topic must exist; `lessonId`, `courseId` and `sourceCode` must be
  null. Otherwise `400 INVALID_PACKAGE_TOPIC`.
- `COURSE_TEST`: `courseId` is required and the course must exist; `lessonId`, `topicId` and `sourceCode` must be
  null. Otherwise `400 INVALID_PACKAGE_COURSE`.
- Every other type: `topicId` and `courseId` must be null (`400`). `lessonId` is only for `PRACTICE_SET`
  (`422 INVALID_PACKAGE_LESSON` when the lesson does not exist). `sourceCode` is only for `PRACTICE_TEST`, is
  optional (none means an internal course checkpoint, not listed in the library) and must be a supported test
  source.
- `PLACEMENT_TEST`: `placementPool` is required and must be `BAND_0_3`, `BAND_3_5` or `BAND_5_9`.
  Any other package type requires a null/omitted pool. Missing, unknown or inapplicable pools return `400`.
  The same validation applies to the legacy `POST /api/content/packages`. Create, list and detail responses
  carry `placementPool`; the pool changes only before the first publication (section 8).
- Publishing a placement package without a pool returns `400` with `details.code = PLACEMENT_POOL_REQUIRED`.
  This also applies to scheduled publishing and validation before scheduling.
- A duplicate `code` returns `400` like the original route.
- The topic or course of a final test cannot be changed after creation.
- A `DRAFT` final test is invisible to learners: learning reads only `PUBLISHED` packages with a published
  version. Publishing keeps its existing rules.

The original `POST /api/content/packages` still refuses `TOPIC_TEST` and `COURSE_TEST`.

## 8. Change a package's information

`PATCH /api/content/admin/packages/{packageId}` (`CONTENT_AUTHOR`). Every field is optional and a missing field is left
as it is:

```json
{ "title": "Reading Practice Test 6", "accessLevel": "PREMIUM", "sourceCode": "IELTS_SPACE_PRO", "placementPool": "BAND_3_5" }
```

Returns `200` with the detail shape of section 1. The code and the package type never change.

- `title`: up to 255 characters, not blank.
- `accessLevel` `FREE` or `PREMIUM`: takes effect for learners at once (the learner library and the Premium checks read it).
- `sourceCode`: only for `PRACTICE_TEST`, a supported test source. A blank value clears it, which takes the test out of
  the learner library (it becomes an internal checkpoint). Another type answers `400`.
- `placementPool`: only for `PLACEMENT_TEST`. It can change only while the package has never been published (status
  `DRAFT` and no published or archived version); otherwise `409` with `details.code = PLACEMENT_POOL_LOCKED`
  ("Đề đã xuất bản nên không đổi được kho band. Gỡ đề này và tạo đề mới cho kho khác."). Another type answers `400`.
- An unknown package answers `404`.

## 9. Hide and restore a test

`POST /api/content/admin/packages/{packageId}/archive` and `POST /api/content/admin/packages/{packageId}/restore`
(`CONTENT_AUTHOR`) return `200` with the detail shape of section 1. Only `PRACTICE_TEST` and `PLACEMENT_TEST` can be
hidden; any other type answers `409` with `details.code = PACKAGE_ARCHIVE_NOT_SUPPORTED`.

- Archive sets the status to `ARCHIVED`. Learners no longer see the test: the library and the placement selection read
  `PUBLISHED` packages only. Attempts already started keep their snapshot and can still be finished.
- The last published placement test of a band pool cannot be archived: `409` with
  `details.code = PLACEMENT_POOL_LAST_PUBLISHED` ("Kho đề này chỉ còn một đề đã xuất bản. Thêm đề khác trước khi gỡ đề
  này."). An unpublished placement test can always be archived.
- Restore puts the package back to `PUBLISHED` when it has a published version, otherwise to `DRAFT`.
- Archive and restore are idempotent: repeating one returns the package as it is.

## 10. The author list

`GET /api/content/admin/packages` accepts `type`, `status`, `lessonId`, `topicId`, `courseId`, `q`, `placementPool` and,
for the practice library and placement screens, `accessLevel` (`FREE` or `PREMIUM`) and `skill` (`READING`, `LISTENING`,
`WRITING`, `SPEAKING`). The list is bounded (500 rows, most recently updated first). Each row also carries, for the
version a learner would see (the published one, otherwise the latest):

| Field | Meaning |
| --- | --- |
| `skill` | The one skill of its sections; null when they mix skills or there are none. |
| `partNos` | The distinct, sorted `partNo` of its sections. |
| `sectionCount`, `questionCount` | Number of sections and of section questions. |

Attempt counts and the average band of a test are read from Assessment
(`GET /api/assessments/admin/package-stats`, see the Assessment contract), not from this route.

Backlog: unreferenced upload cleanup, section-question-group editing, bulk import, package
deletion and cloning old versions. None is implemented by this contract.
