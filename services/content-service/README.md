# Content Service

Content Service owns the IELTS curriculum: topics, knowledge points (KPs), the question bank, content packages,
reading content and assets. It is a Spring MVC downstream service on port `8082`, reached through the Gateway at
`/api/content/**` except the vocabulary and video paths routed to Library Service. Schema migrations live in
`src/main/resources/db/migration`.

Library Service owns the five vocabulary/video catalog tables. Content V7 drops those tables; run that destructive
migration only in Testcontainers until its use on a shared `content_db` is approved. Library checks a video's topic
through `GET /api/content/topics/{id}`. Content implements `POST /internal/game-content/snapshots` only for `GRAMMAR`;
Library implements the same [snapshot contract](../../docs/contracts/game-content-snapshot-v1.md) for `VOCABULARY`.

## Lessons and curriculum order

V8 implements `lessons`, `lesson_blocks`, `lesson_block_vocabulary`, `lesson_block_questions` and
`lesson_knowledge_points`. `content_packages.topic_id` links each `TOPIC_TEST` to its topic; a topic can have
multiple test codes. `LESSON` remains a valid package type for existing reading packages. V9 seeds the Reading
lesson pipeline. These are implemented migrations; their presence does not confirm they ran on a shared database.

Learning reads the six `/internal/learning-content/**` routes in the
[internal contract](../../docs/contracts/learning-content-internal-v1.md). `topic-sequence` returns active topics
that have a skill and published lessons, ordered by skill then `sort_order`, with `hasTopicTest`, active KPs and
`hasPracticeSet`. Learning owns learner progress, gates and mastery; Assessment reads package snapshots when it
creates an attempt.

Each topic that teaches lessons has one `skill` (`LISTENING`, `READING`, `WRITING`, `SPEAKING`; V15, never `ALL`),
and its lessons inherit it. Topic create/update accept an optional `skill`; update keeps the current skill when it is
omitted and returns `409` (`details.code = TOPIC_SKILL_LOCKED`) when a topic with published lessons would change
skill. There is no lesson authoring API, so seed migrations must keep every lesson question and lesson KP on its
topic's skill: `LessonPipelineSeedTest.everyLessonTeachesOnlyItsTopicsSkill` enforces it. V15 moved the demo Writing
essays from Reading lessons L3/L4 into topic `DEMO_WRITING` (lessons W1, W2; no final test), keeping their block ids.

Topics retain optional `bandMin`/`bandMax` metadata (0–9, half-band steps). V8 removes KP band columns and the KP
API no longer accepts or returns own/effective band ranges. The current learning sequence uses topic `sort_order`,
without goal-band filtering or placement test-out. Topic create/update still validate bands; omitting them on update
clears the range.

## Media

- `GET /api/content/assets/{id}` is for `ADMIN` and `CONTENT_AUTHOR` only: asset text can be a transcript. Learners
  receive media through the internal lesson and package payloads that Learning Service and Assessment relay.
- Content is the only place that builds media URLs (`MediaReferencePolicy`). Images use an `https://` URL or a
  `data:image/png|jpeg|svg+xml;base64,` URI; audio uses an `https://` URL or an object key joined to
  `content.media.base-url` (env `CONTENT_MEDIA_BASE_URL`, an https prefix; blank means keys cannot resolve and the
  read fails with `INVALID_MEDIA_REFERENCE`).
- Audio keys contain ASCII letters/digits, `.`, `_`, `-` and `/` between path segments; they cannot start with `/`
  or contain `..`. A full `https://` reference works without a base URL. Learning Service and Assessment forward
  the resolved URL; they do not build it themselves.
- The team uploads mp3 files to a public-read cloud bucket. The backend has no upload API, signed URLs or listen-count
  limit. Keep audio binaries out of Git; the media host must serve `audio/mpeg` and support range requests for playback.
- Audio `text_content` is the transcript. Learning reveals it only after lesson completion or a passed review set
  (≥ 70%); Assessment keeps it out of attempt structure and returns it in `sectionSolutions` only at ≥ 70%.
- The V12 Listening seed references 8 files to upload under the base URL (record them from the transcripts in the
  seed; mp3 files are not committed):

  | Key | Seconds | Used by |
  | --- | --- | --- |
  | `listening/demo/ls1.mp3` | 45 | lesson LS1 |
  | `listening/demo/ls2.mp3` | 50 | lesson LS2 |
  | `listening/demo/numM.mp3` | 30 | practice set PS-NUM |
  | `listening/demo/spellM.mp3` | 35 | practice set PS-SPELL |
  | `listening/demo/museum.mp3` | 50 | practice set PS-PARA |
  | `listening/demo/trapM.mp3` | 30 | practice set PS-TRAP |
  | `listening/demo/hotel.mp3` | 45 | final test X3 |
  | `listening/demo/tour.mp3` | 40 | final test X4 |

## Verification

```powershell
mvn -pl services/content-service -am test
```

The Testcontainers migration tests (including `CatalogRemovalMigrationTest`) are skipped
when Docker is unavailable.
