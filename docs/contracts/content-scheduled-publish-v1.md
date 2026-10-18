# Content package preview and scheduled publication v1

The author routes require `ADMIN` or `CONTENT_AUTHOR`.

## Preview a package version

`GET /api/content/admin/packages/{packageId}/versions/{versionId}/preview` returns author review data for a package
version, including drafts. Learner-facing section data is separated from answers and transcripts:

```json
{
  "packageVersionId": "<version-id>",
  "packageId": "<package-id>",
  "packageType": "PRACTICE_SET",
  "topicId": "<topic-id>",
  "rules": {},
  "sections": [
    {
      "sectionId": "<section-id>",
      "title": "Reading",
      "skill": "READING",
      "instructions": "Choose the best answer.",
      "sortOrder": 1,
      "passage": "<passage text or null>",
      "audio": {
        "assetId": "<asset-id>",
        "mediaUrl": "https://media.example/audio.mp3",
        "durationSeconds": 45
      },
      "items": [
        {
          "questionVersionId": "<question-version-id>",
          "sortOrder": 1,
          "stem": "<question stem>",
          "options": { "A": "<option>" },
          "hint": null,
          "maxScore": 1.0,
          "knowledgePointMappings": [
            { "knowledgePointId": "<knowledge-point-id>", "weight": 1.0 }
          ]
        }
      ]
    }
  ],
  "answers": [
    {
      "sectionId": "<section-id>",
      "audioTranscript": null,
      "items": [
        {
          "questionVersionId": "<question-version-id>",
          "answerSpec": { "type": "CHOICE", "correct": "A" },
          "explanation": "<explanation or null>",
          "hint": null
        }
      ]
    }
  ]
}
```

`sections` excludes answer specifications and audio transcripts. `answers` contains those author-only values. Audio
references in `sections` contain a resolved media URL. An unknown package or a package version with no content record
returns `404`; an unknown version ID or a version ID that does not belong to the supplied package returns `400`.

## Schedule, cancel, and read publication

`PUT /api/content/admin/packages/{packageId}/versions/{versionId}/schedule` accepts a future ISO-8601 instant:

```json
{ "publishAt": "2026-10-19T09:00:00Z" }
```

It returns `200` with the schedule state:

```json
{
  "packageId": "<package-id>",
  "versionId": "<version-id>",
  "publishAt": "2026-10-19T09:00:00Z",
  "lastPublishError": null
}
```

Only `DRAFT` versions can be scheduled. Scheduling validates the package with the same publication rules used for
immediate publication. A missing or non-future `publishAt`, or a non-draft version, returns `400`; publication
validation errors return `422` with the corresponding `details.code` (for example `QUESTION_ALREADY_USED`,
`QUESTION_PURPOSE_MISMATCH`, or `INVALID_PACKAGE_LESSON`).

`DELETE /api/content/admin/packages/{packageId}/versions/{versionId}/schedule` cancels a draft schedule and returns
`204`. `GET /api/content/admin/packages/{packageId}/publication-schedules` returns an array of versions that have a
scheduled publication time or a recorded publication error, with the same four fields as the schedule response; no
current schedule or recorded failure returns an empty array. Unknown packages return `404`; an unknown version ID or
a version ID that does not belong to the package returns `400` for the schedule and cancel routes.

The publisher checks due drafts once per minute by default and serializes workers with a PostgreSQL advisory lock.
Failures leave the version in `DRAFT` and store a safe code in `lastPublishError`; rescheduling or successful
publication clears that error. A failure that breaks a publishing rule (`QUESTION_ALREADY_USED`,
`QUESTION_PURPOSE_MISMATCH`, `INVALID_PACKAGE_LESSON`, `INVALID_CONTENT_STATE`) also clears `publishAt`: the same
attempt would fail again until an author changes the package, and retrying would publish it unannounced weeks
later. The author reads the code in the publication schedules, fixes the package and schedules it again. Only
`PUBLISH_FAILED` (a cause the job cannot tell, possibly temporary) keeps `publishAt`, so the next minute retries it.
