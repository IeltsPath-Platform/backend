# Content Service

Content Service owns the IELTS curriculum: topics, knowledge points (KPs), the question bank, content packages,
reading content and assets. It is a Spring MVC downstream service on port `8082`, reached through the Gateway at
`/api/content/**` except the vocabulary and video paths routed to Library Service. Schema migrations live in
`src/main/resources/db/migration`.

Library Service owns the five vocabulary/video catalog tables. Content V7 drops those tables; run that destructive
migration only in Testcontainers until its use on a shared `content_db` is approved. Library checks a video's topic
through `GET /api/content/topics/{id}`. Content implements `POST /internal/game-content/snapshots` only for `GRAMMAR`;
Library implements the same [snapshot contract](../../docs/contracts/game-content-snapshot-v1.md) for `VOCABULARY`.

## Courses

Courses group topics by an IELTS band level shared across skills. `bandLevel` is unique and must be between 0 and 9
in half-band steps. `code` is unique and remains unchanged on update. Courses have `ACTIVE` or `INACTIVE` status.

- `GET /api/content/courses`: `ADMIN`, `CONTENT_AUTHOR`, `CUSTOMER`, and `EXAMINER`; returns all courses ordered by
  `bandLevel` in the database.
- `POST /api/content/admin/courses`: `ADMIN` and `CONTENT_AUTHOR`; accepts `code`, `name`, and `bandLevel` and creates
  an active course.
- `PUT /api/content/admin/courses/{id}`: the same author roles; accepts `name`, `bandLevel`, and optional `status`.
  Duplicate course codes or band levels return 409; invalid bands return 400; unknown course IDs return 404.

Topic create/update requests and topic detail/tree responses include nullable `courseId`. A supplied ID must reference
an existing course. An omitted or null course ID on update preserves membership; creating a topic without a course
remains valid. Topic `bandMin`/`bandMax` metadata remains available. V19 adds the course table and nullable topic foreign key.

V21 seeds active `IELTS_5_5` (band 5.5) and `IELTS_6_5` (band 6.5) courses. Every existing topic with a skill is
assigned to IELTS 5.5. IELTS 6.5 includes the Reading topic `READING_6_5_INFERENCE`, with inference and paraphrase
knowledge points, a lesson, a passage, practice for each knowledge point, and a topic test. Each course has a
six-question Reading `COURSE_TEST`; these packages are seed-managed and exposed through the course sequence's
`hasCourseTest` flag and `GET /internal/learning-content/courses/{id}/test-packages`. The seed and representative
6.5 sequence response are covered in the [internal learning content contract](../../docs/contracts/learning-content-internal-v1.md).

V45 seeds 13 Grammar and Vocabulary knowledge points from the approved proposal. They retain the proposal's CEFR,
can-do and scope metadata, start as `INACTIVE`, and point to their related active strategy knowledge points as
prerequisites. Author tree reads can include these nodes with `includeInactive=true`; Learning's internal prerequisite
read filters inactive points until they are activated. See the [knowledge tree contract](../../docs/contracts/knowledge-tree-v1.md).

## Package preview and scheduled publication

Authors can preview any package version, including a `DRAFT`, at
`GET /api/content/admin/packages/{packageId}/versions/{versionId}/preview`. The `sections` payload omits answer
specifications and audio transcripts; a separate `answers` array contains them for author review. Schedule a draft
with `PUT .../schedule` and `{ "publishAt": "<future ISO-8601 instant>" }`, cancel with `DELETE .../schedule`, and
read pending or failed schedules at `GET /api/content/admin/packages/{packageId}/publication-schedules`. All routes
require `ADMIN` or `CONTENT_AUTHOR`. Scheduling runs the same checks as an immediate publish. V43 stores the due
instant and a safe error code; a once-per-minute job uses a PostgreSQL advisory lock and the existing publish use case.
A `PRACTICE_TEST` created without `sourceCode` is an internal course checkpoint: the practice library lists only
tests with a supported source that are not course milestones. See the [preview and scheduled publication contract](../../docs/contracts/content-scheduled-publish-v1.md) for response
shapes and retry behavior.

V22 is mock demo content so each Academic course covers Reading, Listening and Writing and can rotate final tests.
IELTS 6.5 gains `LISTENING_6_5_DETAIL` (two knowledge points, one lesson, a practice set per knowledge point and a
topic test) and `WRITING_6_5_DISCUSSION` (one Task 2 essay lesson with `passBand` 6.5, no topic test). The Listening
packages reuse the V12 audio (`ls2.mp3`, `trapM.mp3`, `museum.mp3`, `hotel.mp3`), so no new media upload is needed.
Each course gains a second six-question Reading `COURSE_TEST` (`COURSE-5_5-READING-B`, `COURSE-6_5-READING-B`).

V23 seeds `TREES_MULTI_SKILL` ("Cây cối", IELTS 5.5, sort 955, no topic `skill`): lessons T1 and T2 each teach Reading,
Listening and Writing (theory, passage + Reading exercise, audio + Listening exercise, Task 2 essay with `passBand`
5.5). T1 has a Reading set, a Listening set, a one-essay Writing set and a mixed Reading + Listening set; T2 has a
Reading set. The topic test `TR-TOPIC-TEST` has a Reading, a Listening and a Writing (essay) section. Listening reuses
the V12 audio (`tour.mp3`, `ls1.mp3`, `numM.mp3`, `spellM.mp3`, `ls2.mp3`); no new media upload is needed.

## Lessons and curriculum order

V8 implements `lessons`, `lesson_blocks`, `lesson_block_vocabulary`, `lesson_block_questions` and
`lesson_knowledge_points`. `content_packages.topic_id` links each `TOPIC_TEST` to its topic; a topic can have
multiple test codes. `LESSON` remains a valid package type for existing reading packages. V9 seeds the Reading
lesson pipeline. These are implemented migrations; their presence does not confirm they ran on a shared database.

Learning reads the `/internal/learning-content/**` routes in the
[internal contract](../../docs/contracts/learning-content-internal-v1.md). `topic-sequence` returns active topics
that have a skill and published lessons and an active course, ordered by skill, course band and topic order, with
course metadata, `hasTopicTest`, active KPs and `hasPracticeSet`. Learning owns learner progress, gates and mastery;
Assessment reads package snapshots when it creates an attempt.

Each topic that teaches lessons has one `skill` (`LISTENING`, `READING`, `WRITING`, `SPEAKING`; V15, never `ALL`),
and its lessons inherit it. Topic create/update accept an optional `skill`; update keeps the current skill when it is
omitted and returns `409` (`details.code = TOPIC_SKILL_LOCKED`) when a topic with published lessons would change
skill. Lesson authoring is available through the admin API described in
[`lesson-authoring-v1.md`](../../docs/contracts/lesson-authoring-v1.md); publication keeps lesson content aligned with
the topic's skill. V15 moved the demo Writing
essays from Reading lessons L3/L4 into topic `DEMO_WRITING` (lessons W1, W2; no final test), keeping their block ids.

V16 adds `lesson_block_knowledge_points` (the KPs each TEXT block teaches) and `content_packages.lesson_id`, which makes a `PRACTICE_SET` part of a lesson's Practice. Package creation accepts an optional `lessonId` (practice sets only, the lesson must exist, else `422` with `details.code = INVALID_PACKAGE_LESSON`); publishing such a package fails the same way when a question's skill differs from the lesson's topic. V16 seeds `PS-KP1-C`, `PS-KP1-D` and one set for each of TF1, PM1, PM2, PS1 and PS2, and refuses a published lesson of a topic with a final test that has no Practice.

V18 thêm 11 đề Practice Reading (mỗi bài Reading của topic có thi cuối có ít nhất hai đề; mỗi KP đủ đề cho một lần Practice và hai set ôn) và đề thi cuối thứ hai X8, X9, X10 cho `TFNG_SKILLS`, `PREMIUM_MATCHING_INFO`, `PREMIUM_SENTENCE_COMPLETION`, để thi lại gặp đề khác. Toàn văn câu hỏi để duyệt: `plans/261002-1600-skill-tracks-practice-remediation/reports/v18-new-questions.md`.

Còn thiếu: mỗi KP Listening (`LS_NUM`, `LS_SPELL`, `LS_PARA`, `LS_TRAP`) chỉ có một đề Practice, vì chưa có file mp3 mới. Practice Listening trượt nên không tạo được bài ôn (hết đề chưa lộ lời giải). Cần thêm audio để thang ôn tập của Listening đủ bước.

Each question (`question_id`, across all its versions) has at most one owner: any lesson, or one `PRACTICE_SET`, `TOPIC_TEST`, `MOCK_TEST` or `PLACEMENT_TEST` package. Publishing checks every lesson and PUBLISHED versions of other covered packages; drafts do not reserve questions, and versions of the same package may share them. A conflict returns `422` with `details.code = QUESTION_ALREADY_USED`, listing at most 10 conflicts, without saving or publishing. `QUIZ` and legacy `LESSON` packages are outside this rule.

V24 adds purpose `PLACEMENT` and V25 seeds the published `PLACEMENT-4SKILLS` package (4 Listening, 6 Reading, 1 Writing and 1 Speaking question, all `PLACEMENT`); Learning reads it from `GET /internal/learning-content/placement-packages`. Placement tests accept only `PLACEMENT` questions and mock tests only `EXAM`.
V17 adds immutable `questions.purpose`: `LEARNING` for lessons, practice and topic tests; `EXAM` for mock tests (placement tests moved to `PLACEMENT` in V24). Existing questions default to `LEARNING`; those already attached to any mock or placement test are backfilled to `EXAM`. Question creation accepts optional `purpose` (default `LEARNING`), question responses include it, and the catalog accepts `?purpose=LEARNING|EXAM` with optional `skill`, filtering in the database. Adding a version preserves purpose. Publishing locks question rows in id order until commit before checking ownership and purpose; the wrong purpose returns `422` with `details.code = QUESTION_PURPOSE_MISMATCH` without saving. Invalid purpose input returns `400`.

Topics retain optional `bandMin`/`bandMax` metadata (0–9, half-band steps). V8 removes KP band columns and the KP
API no longer accepts or returns own/effective band ranges. The current learning sequence uses topic `sort_order`,
without goal-band filtering or placement test-out. Topic create/update still validate bands; omitting them on update
clears the range.

## Media

- `GET /api/content/assets/{id}` is for `ADMIN` and `CONTENT_AUTHOR` only: asset text can be a transcript. Learners
  receive media through the internal lesson and package payloads that Learning Service and Assessment relay.
- Content is the only place that builds media URLs. Images use an `https://` URL or a
  `data:image/png|jpeg|svg+xml;base64,` URI. `PUBLIC` audio uses an `https://` URL or an object key joined to
  `content.media.base-url` (env `CONTENT_MEDIA_BASE_URL`, an https prefix; blank means keys cannot resolve and the
  read fails with `INVALID_MEDIA_REFERENCE`). `PREMIUM` lesson audio is signed from the private
  `content.media.premium-bucket` (`CONTENT_PREMIUM_MEDIA_BUCKET`, default `media-premium`) with a 900-second TTL.
  Learning requests signed URLs only after its lesson gate allows access; assessment tests, package previews and author
  reads keep the public media behavior.
- Audio keys contain ASCII letters/digits, `.`, `_`, `-` and `/` between path segments; they cannot start with `/`
  or contain `..`. A full `https://` reference works without a base URL. Learning Service and Assessment forward
  the resolved URL; they do not build it themselves.
- Seed audio keys are stored in `media` or `media-premium` according to the asset's `accessTier`; Placement keeps its
  public key `listening/placement/placement-ls1.mp3`. The public copy of `listening/demo/ls1.mp3` remains until its
  removal is approved. Authors uploading through `POST /api/content/admin/media/audio` (`CONTENT_AUTHOR`, 25 MB) may
  choose `tier=PREMIUM`; the generated key under `content-audio/{userId}/` is returned for creating a lesson asset with
  the matching `accessTier`. Keep audio binaries out of Git; the media host must serve `audio/mpeg` and support range
  requests for playback. Signed URLs expire after 900 seconds and must not be stored or logged.
- Audio `text_content` is the transcript. Learning reveals it only after lesson completion or a passed review set
  (≥ 70%); Assessment keeps it out of attempt structure and returns it in `sectionSolutions` only at ≥ 70%.
- The V12 Listening seed references 8 audio keys (recordings from the transcripts in the seed; mp3 files are not
  committed). `ls1.mp3` is a PREMIUM lesson asset; the other listed keys use the public `media` bucket:

  | Key | Seconds | Used by |
  | --- | --- | --- |
| `listening/demo/ls1.mp3` | 45 | lesson LS1 (private `media-premium`) |
  | `listening/demo/ls2.mp3` | 50 | lesson LS2 |
  | `listening/demo/numM.mp3` | 30 | practice set PS-NUM |
  | `listening/demo/spellM.mp3` | 35 | practice set PS-SPELL |
  | `listening/demo/museum.mp3` | 50 | practice set PS-PARA |
  | `listening/demo/trapM.mp3` | 30 | practice set PS-TRAP |
  | `listening/demo/hotel.mp3` | 45 | final test X3 |
  | `listening/demo/tour.mp3` | 40 | final test X4 |

- The V27 placement test (version 2 of `PLACEMENT-4SKILLS`: Listening Parts 1–4, Reading Passages 1–3, Writing Tasks
  1–2, Speaking Parts 1–3) adds four more files; the seconds are estimates from the transcripts:

  | Key | Seconds | Used by |
  | --- | --- | --- |
  | `listening/placement/part1.mp3` | 150 | placement Listening Part 1 |
  | `listening/placement/part2.mp3` | 180 | placement Listening Part 2 |
  | `listening/placement/part3.mp3` | 170 | placement Listening Part 3 |
  | `listening/placement/part4.mp3` | 190 | placement Listening Part 4 |

  The original shared V25 Placement sample uses the separate public key `listening/placement/placement-ls1.mp3`.

- The Listening practice tests (V32, durations from V35, keys kept by V39) reference 12 files. The recordings are
  text-to-speech renderings of the transcripts; keep a local copy in `services/content-service/media/` (ignored by
  Git) and upload them under the same keys. The database only ever holds the key, never the audio:

  | Key | Seconds | Used by |
  | --- | --- | --- |
  | `listening/practice/pt-listening-01-s1.mp3` … `s4.mp3` | 50, 48, 59, 39 | PT-LISTENING-01 sections 1–4 |
  | `listening/practice/pt-listening-02-s1.mp3` … `s4.mp3` | 41, 42, 48, 36 | PT-LISTENING-02 sections 1–4 |
  | `listening/practice/pt-listening-03-s1.mp3` … `s4.mp3` | 39, 42, 46, 35 | PT-LISTENING-03 sections 1–4 |

  Until the files are uploaded and `CONTENT_MEDIA_BASE_URL` is set, these tests answer `INVALID_MEDIA_REFERENCE` when
  the audio is requested. Once both are done no migration is needed: the URL is `CONTENT_MEDIA_BASE_URL` + key.

## Author workspace (CONTENT_AUTHOR)

These routes serve the author screens. Everything they create stays unpublished (`DRAFT`, or `INACTIVE` for a course and
its topics); publishing goes through the usual checks.

| Area | Routes | Contract |
| --- | --- | --- |
| Packages | `GET`/`POST /api/content/admin/packages` (list with filters; create `TOPIC_TEST` and `COURSE_TEST` as well), package detail with stems, status, purpose, passage and audio | [content-package-authoring-v1](../../docs/contracts/content-package-authoring-v1.md) |
| Section media | passage and audio of a section; `POST /api/content/admin/media/audio` uploads an mp3 (25 MB) to the object store | same contract |
| AI question drafts | `POST /api/content/admin/ai-drafts/questions`, `PATCH`/`DELETE …/{id}/items/{index}`, `POST …/{id}/approve` | [ai-drafts-v1](../../docs/contracts/ai-drafts-v1.md), [question-drafts-v1](../../docs/contracts/question-drafts-v1.md) |
| Course drafts | `POST`/`PUT /api/content/admin/course-drafts`, `GET …/{id}/analysis`, `POST …/{id}/approve` (build a course from the knowledge tree) | [course-drafts-v1](../../docs/contracts/course-drafts-v1.md) |
| AI course drafts | `POST /api/content/admin/ai-drafts/courses` (rule-based candidates from the catalogue; the model only groups them) | [ai-drafts-v1](../../docs/contracts/ai-drafts-v1.md) |
| Topic order | `PUT /api/content/admin/topics/order` reorders all topics of one parent in one transaction (`204`) | [lesson-authoring-v1](../../docs/contracts/lesson-authoring-v1.md#reordering-topics) |
| Excel import | `GET …/course-import/template`, `POST …/course-import/validate`, `POST …/course-import` | [course-import-v1](../../docs/contracts/course-import-v1.md) |

Configuration (`infra/config-server/config-repo/content-service.yaml`, keys in the gitignored root `.env`):

- `CONTENT_LLM_BASE_URL`, `CONTENT_LLM_API_KEY`, `CONTENT_LLM_MODEL`: an OpenAI-compatible model for drafts. When one is not set the
  matching `LEARNING_LLM_*` value is used, so the model the learning service already uses also serves drafts. Blank on
  both sides means a draft fails with `NOT_CONFIGURED`; the service still starts and everything else works.
- `content.ai.*`: `batch-size`, `stale-after`, `daily-limit` (shared by AI requests and imports, counted separately per
  author and day in `ai_usage_daily`), `max-items-per-request`, `max-course-candidates`, `quota-zone`.
- `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `CONTENT_MEDIA_BUCKET`, `CONTENT_PREMIUM_MEDIA_BUCKET`: author audio upload;
  without storage configuration the upload answers `503`. Uploaded keys live under `content-audio/`; PUBLIC keys resolve
  through `CONTENT_MEDIA_BASE_URL`, while PREMIUM lesson audio is signed from the private premium bucket after access
  is authorized.

An AI draft never reaches learners: a queued draft is generated by a scheduled job, an author reviews it, and approval
creates ordinary `DRAFT` content. See [docs/ai-policy.md](../../docs/ai-policy.md#ai-sinh-nội-dung-khu-author-của-content).

The Excel template ships as `src/main/resources/import/course-import-template-v1.xlsx`. After changing the column
definition in `CourseImportTemplate`, rewrite it with
`mvn -pl services/content-service -am test -Dtest=ImportTemplateFileTest -DwriteImportTemplate=true`.

## Error trap knowledge points

Active error types with a parent knowledge point receive one synchronized child trap KP. The existing
`error_types.knowledge_point_id` remains the parent mapping used by Writing; `trap_knowledge_point_id` is the child.
Distractor codes on question answer specs are the source used to tag trap answers and count trap questions in practice
sets. Child KPs are returned only on the internal topic-sequence read when Learning requests `includeErrorTraps=true`;
the default sequence omits them so parent-only curriculum rollups keep their existing shape. Content owns the child
catalog and trap metadata synchronization.

## Verification

```powershell
mvn -pl services/content-service -am test
```

The Testcontainers migration tests (including `CatalogRemovalMigrationTest`) are skipped
when Docker is unavailable.
