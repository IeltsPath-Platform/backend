# Internal learning content API v1

## Course practice-test milestones

`GET /internal/learning-content/courses/{id}/practice-test-milestones` returns up to 100 milestones for an ACTIVE
course. Unknown or inactive courses return `404`. Only PUBLISHED `PRACTICE_TEST` packages whose current version is
PUBLISHED and belongs to that package are included. Results are ordered by `afterTopicSequence`, `code`, and package id:

```json
[{"packageId":"6b2bf327-9455-4be7-942c-f24c5669f422","packageVersionId":"adff6074-cfd2-4320-b8d6-684dece01862","code":"CHECKPOINT-A","title":"Course checkpoint","afterTopicSequence":2}]
```

`afterTopicSequence` is a one-based position **inside the course**, using Learning's topic order (free topics first,
then topic `sortOrder`, then id). Only ACTIVE topics with a PUBLISHED lesson count. The current package version is
resolved on each read, so republishing does not require replacing the milestone.

CONTENT_AUTHOR manages the projection through `GET` and `PUT
/api/content/admin/courses/{id}/practice-test-milestones`. PUT atomically replaces the course's milestones and takes
`{"milestones":[{"packageId":"6b2bf327-9455-4be7-942c-f24c5669f422","afterTopicSequence":2}]}`. An empty array clears them.
Unknown courses return `404`; duplicate packages, nonpositive or unreachable positions, more than 100 entries, unpublished
versions, and packages of another type return `400`. A package can belong to only one course's milestones, matching
Learning's single first-score assignment per learner/package. Reads for management also show configured packages that
have since been archived; their `packageVersionId` may be null. Learners cannot call the management routes.

A practice test without a `sourceCode` is internal: it is reached through its course (a checkpoint) and is never listed
in the practice library. The library catalog lists only tests that have a supported `sourceCode` and are not a course
milestone. `POST /api/content/packages` therefore accepts a `PRACTICE_TEST` with no `sourceCode`, and publishing
checks the source only when one is present (an unknown one is refused). A `sourceCode` on any other package type is
refused with `400`.

The seed supplies one midpoint checkpoint per course 5.5 and 6.5. Both contain original short Reading/Writing material;
they are not full-length IELTS mocks. Practice-test questions are reserved against reuse in lessons or review sets.
Publishing a `PRACTICE_TEST` locks its question rows, checks ownership and `EXAM` purpose, and requires gradable
Reading/Listening questions or Writing essays with a numeric `passBand`, as for course-test grading support.

**Status: approved 2026-10-01. Updated 2026-10-16: active status on topic sequence entries and filtering in learner refresh; bounded progress metadata for dashboard statistics. Earlier updates added course sequence metadata and course final-test packages, derived `skills`, and mixed-skill course tests.**

Content Service owns these routes under `/internal/learning-content` (three practice routes added 2026-10-03; course test packages added 2026-10-07; progress metadata added 2026-10-16). They require a verified internal JWT; callers forward the request bearer and `X-Correlation-Id`. Gateway must explicitly deny client access to `/internal/**`. Except for the metadata-only route below, responses here can contain `answerSpec` and `explanation` and must never be proxied to a learner unchanged. This contract uses Content's camelCase JSON names. Vocabulary/video catalog data belongs to Library Service; Content V7 removed those tables. The `VOCABULARY` block contains logical sense IDs only, with no Content-owned vocabulary entity or cross-database lookup.

## `POST /progress-metadata`

Learning uses this single batch read to obtain the current published lesson denominator for the learner's started courses.
The request contains 1–100 non-null UUIDs, counted before deduplication:

```json
{"courseIds":["30000000-0000-4000-8000-000000000001"]}
```

HTTP 200 response:

```json
{
  "revision":"a2da7a64-bc31-31cb-8d23-dd7464456473",
  "complete":true,
  "lessons":[{
    "courseId":"30000000-0000-4000-8000-000000000001",
    "courseName":"IELTS 5.5",
    "bandLevel":5.5,
    "topicId":"10000000-0000-4000-8000-000000000001",
    "lessonId":"20000000-0000-4000-8000-000000000101",
    "skills":["READING"]
  }]
}
```

The example revision is illustrative. `revision` is an opaque fingerprint of the returned metadata, including lesson
membership, skills and course labels; additions, removals or changes may change it. Only ACTIVE courses, ACTIVE topics
and PUBLISHED lessons qualify. Unknown/inactive IDs contribute no rows; they do not cause 404. Lessons are ordered by
course ID, topic order/ID and lesson order/ID. Skills are distinct and alphabetically ordered. This response contains no
questions, answers, `answerSpec`, `chartFacts`, explanations, transcript or learner data.

The query probes at most 10,001 rows. Up to 10,000 lessons returns `complete=true`; overflow returns HTTP 200 with
`complete=false` and `lessons=[]`. Callers must reject an incomplete denominator, never show a percentage from a
truncated set. No matching lessons returns `complete=true, lessons=[]`; the revision remains opaque.

Errors use Content's existing `ErrorResponse`: HTTP 400 for missing/empty/over-100 `courseIds`, null elements or malformed
UUID/JSON; HTTP 401 for missing/invalid internal authentication. This internal route is not exposed in public Swagger or
through Gateway. The public consumer and its 422/503 behavior are documented in
[dashboard statistics](dashboard-statistics-v1.md).

Examples use real codes, stems, answers, and passages from [`seed-content.md`](../../plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md). The existing V4 UUIDs for DEMO_READING and KP1 are real; other example UUIDs illustrate relationships rather than fixing V9 migration IDs. A `404` means the id is unknown or unpublished. Collections are sorted as described below.

## Derived skills

Added 2026-10-07. A lesson teaches several skills. Lesson `skills` is the set of the skills of the questions in its
`EXERCISE` blocks plus the skills of the KPs mapped to its `TEXT` blocks. Topic `skills` is the union over its PUBLISHED
lessons. Practice set `skills` is the set of the skills of the questions in its current PUBLISHED version. Every array
is distinct, follows the enum order `LISTENING, READING, WRITING, SPEAKING` and never contains `ALL`. Each response
that has `skills` keeps the older `skill` field for compatibility: it is the only element when `skills` has exactly one,
otherwise `null`. The arrays are computed inside the listing queries, never with one query per lesson or set. The stored
topic `skill` label no longer decides lesson skills and may change after lessons are published.

## `GET /topic-sequence`

Returns an array of ACTIVE topics that belong to an ACTIVE course and have at least one PUBLISHED lesson (since 2026-10-07 a topic no longer needs a stored `skill`), ordered by the stored topic `skill` label (topics without one last), then `course.bandLevel`, then `sortOrder`, then `topicId` (course filtering/order added 2026-10-07; a PUBLISHED `TOPIC_TEST` has not been required since 2026-10-03). Topics without a course or in an inactive course are excluded. The additive `active` field on each topic and course reports the corresponding status; the current sequence query returns only entries where both are `true`. Learning also filters inactive entries defensively before refreshing a learner's path. Each topic carries `course: {courseId, code, name, bandLevel, hasCourseTest, active}`; `hasCourseTest` is true when that course has a PUBLISHED `COURSE_TEST` package with a non-null current version. Course metadata is joined in the topic query. `skills` is the union of its PUBLISHED lessons' skills and `skill` follows the compatibility rule in *Derived skills*. `hasTopicTest` is true when the topic has a PUBLISHED `TOPIC_TEST` package with a current version; a topic without one (the demo `DEMO_WRITING`) is passed by completing its lessons. This is the single curriculum read used to build and refresh the MVP path. Each topic includes its ACTIVE knowledge points, ordered by `created_at`, then `id`; no topic band range or answer key is returned. `hasPracticeSet` is true exactly when at least one package matches `POST /practice-sets/search` for that KP with `excludePackageIds=[]` and default `minQuestions=3`. Since content V16 every KP of the free and premium Reading and Listening topics has one; the Writing KPs have none.

```json
[
  {
    "topicId": "10000000-0000-4000-8000-000000000001",
    "code": "DEMO_READING", "name": "Demo IELTS Reading", "sortOrder": 900, "requiredFeatureKey": null,
    "skill": "READING", "hasTopicTest": true, "active": true,
    "course": {"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5,"hasCourseTest":true,"active":true},
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000003","code":"DR_TOPIC_SENTENCE","name":"Câu chủ đề","learningType":"PROCEDURE","skill":"READING","description":null,"cefrLevel":null,"canDo":null,"scopeIn":null,"scopeOut":null,"hasPracticeSet":true}
    ]
  },
  {
    "topicId": "20000000-0000-4000-8000-000000000002",
    "code": "TFNG_SKILLS", "name": "True / False / Not Given", "sortOrder": 910, "requiredFeatureKey": null,
    "skill": "READING", "hasTopicTest": true, "active": true,
    "course": {"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5,"hasCourseTest":true,"active":true},
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000005","code":"TFNG_FALSE_VS_NOT_GIVEN","name":"False hay Not Given","learningType":"PROCEDURE","skill":"READING","description":null,"cefrLevel":null,"canDo":null,"scopeIn":null,"scopeOut":null,"hasPracticeSet":true}
    ]
  }
]
```

Each knowledge point now also contains nullable `cefrLevel` (`A1`–`C2`), `canDo`, `scopeIn`, and `scopeOut`. The
example shows representative KPs; the real response includes **all** ACTIVE KPs of each returned topic (KP1–KP4 for
`DEMO_READING`, KP5 for `TFNG_SKILLS`). `hasPracticeSet` uses the same eligible-package predicate as search,
including the no-overlap rule below. It is computed in a batched query, not by one query per KP. The topic object is
`{topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course, skills, active}`;
`skill` and `hasTopicTest` were added 2026-10-03, `course` and `skills` on 2026-10-07, and `active` on 2026-10-16.
Existing readers that ignore unknown fields keep working. Content V15 moved the Writing knowledge points
`DEMO_READING_W1_CHART` and `DEMO_READING_W2_OPINION`, with their essay blocks (same block ids), from Reading lessons
L3/L4 to `DEMO_WRITING` lessons W1/W2 (sort 950, no final test). `requiredFeatureKey` (added 2026-10-02) is the
Access feature needed to learn the topic, `null` when free; the seed marks `PREMIUM_MATCHING_INFO` and
`PREMIUM_SENTENCE_COMPLETION` (sort 930, 940) with `PREMIUM_CONTENT`.

## `GET /topics/{id}/lessons`

Returns PUBLISHED lessons of the ACTIVE topic in `sortOrder` order, with IDs needed to persist progress. Each entry is `{lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, skills}`; both ID arrays contain UUIDs (`skills` added 2026-10-07). Example for `DEMO_READING`:

```json
[
  {"lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001","code":"L1","title":"Câu chủ đề nằm ở đâu","summary":null,"sortOrder":1,"knowledgePointIds":["20000000-0000-4000-8000-000000000003"],"exerciseBlockIds":["20000000-0000-4000-8000-000000000202","20000000-0000-4000-8000-000000000205"]},
  {"lessonId":"20000000-0000-4000-8000-000000000102","topicId":"10000000-0000-4000-8000-000000000001","code":"L2","title":"Ý chính của cả bài","summary":null,"sortOrder":2,"knowledgePointIds":["10000000-0000-4000-8000-000000000002"],"exerciseBlockIds":["20000000-0000-4000-8000-000000000203"]}
]
```

The example includes two representative entries; the actual seed has L1–L4.

V21 seed example for the independent 6.5 Reading sequence (KPs abbreviated to show the course and per-KP practice flag):

```json
{
  "topicId":"28000000-0000-4000-8000-010000000001",
  "code":"READING_6_5_INFERENCE",
  "name":"Reading 6.5: Inference and paraphrase",
  "sortOrder":960,
  "requiredFeatureKey":null,
  "skill":"READING",
  "hasTopicTest":true,
  "course":{"courseId":"30000000-0000-4000-8000-000000000002","code":"IELTS_6_5","name":"IELTS 6.5","bandLevel":6.5,"hasCourseTest":true},
  "knowledgePoints":[
    {"id":"28000000-0000-4000-8000-020000000001","code":"R65_EVIDENCE_INFERENCE","name":"Supported inference","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":true},
    {"id":"28000000-0000-4000-8000-020000000002","code":"R65_PARAPHRASE","name":"Paraphrase across sentences","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":true}
  ]
}
```

The seed also creates `COURSE_TEST` packages for each seeded course: one in V21 and a second in V22, so assignments
can rotate. For example, the 6.5 course package route returns:

```json
[{"packageId":"28000000-0000-4000-8000-080000000005","packageVersionId":"28000000-0000-4000-8000-090000000005","code":"COURSE-6_5-READING"},
 {"packageId":"29000000-0000-4000-8000-080000000005","packageVersionId":"29000000-0000-4000-8000-090000000005","code":"COURSE-6_5-READING-B"}]
```

## `GET /lessons/{id}`

Returns `{lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, blocks, skill, skills}` for a PUBLISHED lesson; `skills` (added 2026-10-07) and `skill` follow *Derived skills*. Blocks are ordered by `sortOrder` and share `{blockId, blockType, sortOrder, knowledgePointIds}`. Block `knowledgePointIds` (added 2026-10-03, never null) are the KPs a `TEXT` block teaches (table `lesson_block_knowledge_points`, possibly empty), the distinct KPs of an `EXERCISE` block's questions, and `[]` for `ASSET` and `VOCABULARY` blocks. A review uses the `TEXT` blocks of its KP as theory; an `ASSET` block (passage, audio) is never theory.

| `blockType` | Additional fields | Rule |
| --- | --- | --- |
| `TEXT` | `textContent: string` | Lesson prose. |
| `ASSET` | `asset: {id, assetType, textContent, mediaReference, durationSeconds, mediaUrl}` | `PASSAGE` uses `textContent`; media uses `mediaReference`. Unused values are `null`. For `AUDIO` (added 2026-10-01) `mediaUrl` is the playable URL resolved by Content (an `https://` reference unchanged, or an object key joined to `content.media.base-url`; anything else fails with `INVALID_MEDIA_REFERENCE`) and `textContent` is the transcript, which is an answer: learners see it only after passing. |
| `VOCABULARY` | `vocabularySenseIds: UUID[]` | Ordered logical IDs in Library Service. Content V72 seeds one leading block with eight senses on each `DEMO_READING` lesson L1–L4, referencing Library V15; Learning does not treat this block as an exercise for completion. |
| `EXERCISE` | `blockKind: "EXERCISE" \| "ESSAY"`, `questions: Question[]` | Ordered by `sortOrder`. `blockKind` (added 2026-10-01): `ESSAY` when the block holds exactly one question whose `answerSpec.type` is `ESSAY`, otherwise `EXERCISE` (only auto-gradable questions). Content rejects any other mix. Consumers trust `blockKind` and do not classify again. |

`Question` is `{questionVersionId, sortOrder, stem, options, answerSpec, explanation, hint, knowledgePointIds}`. `hint` (added 2026-10-01, string or `null`, at most 500 characters) points the learner back to where to reread; it never states the answer. Learning Service decides when to show it: only for a `FILL` question or a `CHOICE` question with at least three options (True/False/Not Given included), after a wrong answer in a block not yet passed. `GET /package-versions/{id}` items also carry `hint` (added 2026-10-03; the V16 practice questions have one, older items are `null`); it is for practice and review sets, and a final-test consumer must not show it. `options` is the Content option array `{optionKey, content, sortOrder}[]`, or **`null` for a fill answer**. A non-null array represents a choice question, including True/False/Not Given (all three keys appear). `answerSpec` is a JSON object conforming to [answer spec v1](answer-spec-v1.md), not a JSON-encoded string.

Questions in an `ESSAY` block also carry `assets: [{assetId, assetType, mediaUrl, altText, sortOrder}]` (added 2026-10-01), the images attached to the question version in `sortOrder`; questions in `EXERCISE` blocks do not have the key. `mediaUrl` is resolved by Content from the stored media reference (for `IMAGE`: an `https://` URL or a `data:image/png|jpeg|svg+xml;base64,` URI, returned unchanged; any other value fails with `INVALID_MEDIA_REFERENCE`). `altText` is the asset's text. A `TASK_1` essay must have `chartFacts` and at least one `IMAGE`, otherwise the lesson fails with `INVALID_LESSON_BLOCK`.

L1 example, using its Q12 fill block and roof passage:

```json
{
  "lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001",
  "code":"L1","title":"Câu chủ đề nằm ở đâu","summary":null,"sortOrder":1,
  "knowledgePointIds":["20000000-0000-4000-8000-000000000003"],
  "blocks":[
    {"blockId":"20000000-0000-4000-8000-000000000201","blockType":"TEXT","sortOrder":1,"textContent":"Câu chủ đề (topic sentence) nêu ý mà cả đoạn triển khai. Trong bài IELTS nó thường là câu đầu đoạn, đôi khi là câu thứ hai sau một câu dẫn. Mẹo: thử bỏ câu đó đi. Nếu đoạn văn mất ý chung thì đó là câu chủ đề."},
    {"blockId":"20000000-0000-4000-8000-000000000204","blockType":"ASSET","sortOrder":2,"asset":{"id":"20000000-0000-4000-8000-000000000801","assetType":"PASSAGE","textContent":"A. Across Europe and North America, city planners are turning to green roofs to cope with hotter summers. A green roof is a layer of soil and plants laid over a waterproof membrane on top of a building.\n\nB. The most important benefit of green roofs is that they keep buildings cool. On a summer afternoon, a conventional black roof can reach 80°C, while a planted roof nearby rarely rises above 30°C. As a result, the floors below need far less air conditioning.\n\nC. Green roofs also manage rainwater. The soil soaks up much of a heavy shower and releases it slowly, which takes pressure off city drains. In Copenhagen, new flat roofs must now be planted for this reason.\n\nD. Not everyone is convinced. Critics point out that green roofs are expensive to install and need regular care, and that many older buildings are not strong enough to carry the extra weight.","mediaReference":null,"durationSeconds":null}},
    {"blockId":"20000000-0000-4000-8000-000000000202","blockType":"EXERCISE","blockKind":"EXERCISE","sortOrder":3,"questions":[{"questionVersionId":"20000000-0000-4000-8000-000000000013","sortOrder":1,"stem":"Which sentence is the topic sentence of paragraph B?","options":[{"optionKey":"A","content":"The most important benefit of green roofs is that they keep buildings cool.","sortOrder":1},{"optionKey":"B","content":"On a summer afternoon, a conventional black roof can reach 80°C…","sortOrder":2},{"optionKey":"C","content":"As a result, the floors below need far less air conditioning.","sortOrder":3}],"answerSpec":{"type":"CHOICE","correct":"A"},"explanation":"Câu A nêu ý của cả đoạn (làm mát). Hai câu sau là số liệu và hệ quả để chứng minh.","knowledgePointIds":["20000000-0000-4000-8000-000000000003"]}]},
    {"blockId":"20000000-0000-4000-8000-000000000206","blockType":"TEXT","sortOrder":4,"textContent":"Luyện thêm với đoạn C và D."},
    {"blockId":"20000000-0000-4000-8000-000000000205","blockType":"EXERCISE","blockKind":"EXERCISE","sortOrder":5,"questions":[{"questionVersionId":"20000000-0000-4000-8000-000000000012","sortOrder":3,"stem":"Complete with ONE WORD from paragraph D: people who doubt green roofs are called ______.","options":null,"answerSpec":{"type":"FILL","accepted":["critics"]},"explanation":"Đoạn D: \"Critics point out that…\". Không phân biệt hoa thường.","knowledgePointIds":["20000000-0000-4000-8000-000000000003"]}]}
  ]
}
```

The example shows Q12 within L1-B5; the actual block also has Q1 and Q11. Any implementation must return the complete block and lesson, rather than the representative subset shown above.

## `GET /topics/{id}/test-packages`

Returns PUBLISHED `TOPIC_TEST` packages for the topic with their current PUBLISHED version, sorted by `packageId`. The learner assignment service selects a package that has not been consumed, or the least recently consumed package after exhaustion.

```json
[
  {"packageId":"20000000-0000-4000-8000-000000000301","packageVersionId":"20000000-0000-4000-8000-000000000401","code":"X1"},
  {"packageId":"20000000-0000-4000-8000-000000000302","packageVersionId":"20000000-0000-4000-8000-000000000402","code":"X2"}
]
```

## `GET /courses/{id}/test-packages`

Added 2026-10-07. Returns PUBLISHED `COURSE_TEST` packages belonging to the ACTIVE course, with a current version that belongs to the same package and is PUBLISHED, sorted by `packageId`. Shape is identical to `GET /topics/{id}/test-packages`: `{packageId, packageVersionId, code}[]`. An unknown or inactive course returns `404`; a course without a published final test returns `[]`. Course tests are created by seed only. Learning owns eligibility for taking the course test and rotates the returned packages.

`GET /package-versions/{id}` accepts published `COURSE_TEST` versions using the existing package payload, with `packageType="COURSE_TEST"` and nullable `topicId`.

## `POST /practice-sets/search`

Request is `{knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId}`. Defaults are `[]`, `3`, `1` and none; `limit` must be 1–10. Return PUBLISHED `PRACTICE_SET` packages with at least `minQuestions` total questions and at least one question mapped to the requested KP. Exclude packages in `excludePackageIds` and packages containing any question version also used in a lesson or `TOPIC_TEST` of **any** topic. Sort the packages of `preferredLessonId` (added 2026-10-03, optional) first, then by matching-question count descending, then `packageId`; apply `limit` in the database. Learning passes every package already revealed to the learner in `excludePackageIds`. The `hasPracticeSet` predicate above is this eligibility rule before exclusion/limit.

```json
{"knowledgePointId":"10000000-0000-4000-8000-000000000002","excludePackageIds":[],"minQuestions":3,"limit":1}
```

Each result also includes `band` and `requiredFeatureKey`. `band` comes from the package's owning course when present,
otherwise from the owning topic's upper range (or lower bound when it has no upper bound); it is `null` when no band is
assigned. `requiredFeatureKey` is the package's Access feature, or `null` when free. These fields are additive; readers
must tolerate `null` values.

```json
[{"packageId":"20000000-0000-4000-8000-000000000501","packageVersionId":"20000000-0000-4000-8000-000000000601","code":"PS-KP1-A","questionCount":4,"matchedQuestionCount":4,"band":5.5,"requiredFeatureKey":null}]
```

Practice search eligibility also excludes question versions used by `COURSE_TEST`, `MOCK_TEST` or `PLACEMENT_TEST` packages. The same exclusion applies to availability counts and `hasPracticeSet`. Reservation checks also include `COURSE_TEST` questions, so they cannot be reused as practice material.

The request/response illustrates KP1 and its `PS-KP1-A` package. The V4 one-question package is ineligible with the default `minQuestions=3`.

## `GET /lessons/{id}/practice-sets`

Added 2026-10-03. Returns the lesson's Practice: PUBLISHED `PRACTICE_SET` packages with a current version whose `lessonId` is this lesson, ordered by `code`. Each item is `{packageId, packageVersionId, code, title, questionCount, knowledgePointIds, requiredFeatureKey, skills, band}`; `band` and `requiredFeatureKey` may be `null`. `band` follows the owning course or topic range described above. No question, answer or explanation. Optional `?skill=X` (added 2026-10-07) returns only sets whose `skills` is exactly `[X]`, so mixed sets are excluded; an unknown value or `ALL` returns `400`. `404` when the lesson is unknown or unpublished; `[]` when it has no Practice (the Writing lessons).

```json
[{"packageId":"26000000-0000-4000-8000-080000000003","packageVersionId":"26000000-0000-4000-8000-090000000003","code":"PS-TF-A","title":"Luyện thêm PS-TF-A: False hay Not Given","questionCount":3,"knowledgePointIds":["20000000-0000-4000-8000-020000000005"],"requiredFeatureKey":null,"skills":["READING"],"band":5.5}]
```

Content V16 links each existing practice set of at least three questions to the earliest published lesson, of the same skill, that teaches one of its KPs, and seeds sets for lessons that had none, so every published lesson of a topic with a final test has at least one. A practice set and a review set come from the same pool of packages.

## `GET /topics/{id}/practice-sets`

Added 2026-10-03. `{lessons: [{lessonId, practiceSets: [...]}]}`: every PUBLISHED lesson of the ACTIVE topic in `sortOrder`, each with the items of the route above, including `skills` (possibly `[]`), read in a fixed number of queries. `404` for an unknown or inactive topic.

## `POST /practice-sets/availability`

Added 2026-10-03. Request `{knowledgePointIds, excludePackageIds, minQuestions}`: 1–50 ids (else `400`), defaults `[]` and `3`. Response `{counts: {knowledgePointId: n}}` where `n` is the number of packages that `POST /practice-sets/search` could return for that KP with the same exclusions, in one query for all KPs; an unknown KP counts `0`.

```json
{"knowledgePointIds":["10000000-0000-4000-8000-000000000002"],"excludePackageIds":["20000000-0000-4000-8000-080000000004"]}
{"counts":{"10000000-0000-4000-8000-000000000002":3}}
```

## `POST /questions/difficulties`

Returns the stored difficulty tag for question versions in one database query. This endpoint requires an authenticated internal JWT like the other `/internal/learning-content/**` routes.

Request: 1–500 question-version UUIDs. The list must not be empty and must not contain `null`.

```json
{"questionVersionIds":["20000000-0000-4000-8000-000000000201","20000000-0000-4000-8000-000000000202"]}
```

Response:

```json
{"difficulties":{"20000000-0000-4000-8000-000000000201":"EASY","20000000-0000-4000-8000-000000000202":"HARD"}}
```

Difficulty values are `EASY`, `MEDIUM`, or `HARD`. Unknown IDs and versions whose difficulty is still `NULL` are omitted from `difficulties`. Difficulty is required for
questions that belong to a course: the authoring API refuses a new version without it (`400 DIFFICULTY_REQUIRED`) and the
backfill sets it from the course. Questions used only in library practice tests belong to no course, so they may stay
`NULL`, and nothing guesses a value for them. Learning treats a missing tag as untagged: an untagged correct answer earns
the unknown-difficulty XP, the too-easy/too-hard check uses the general limits, and staff statistics leave the field out. Invalid or oversized input returns `400`.

## `POST /suggestions/candidates`

Returns published content candidates for Learning's optional suggestions in one batched request. Like the other
`/internal/learning-content/**` routes, this requires a verified internal JWT and is not exposed by the Gateway.

Request (1–50 distinct or repeated UUIDs; `maxBand` is a number from 0 through 9):

```json
{"knowledgePointIds":["20000000-0000-4000-8000-000000000003"],"maxBand":5.5}
```

Response (illustrative IDs):

```json
{
  "packages":[{"packageId":"20000000-0000-4000-8000-000000000301","packageVersionId":"20000000-0000-4000-8000-000000000401","type":"PRACTICE_SET","title":"Topic sentence practice","knowledgePointIds":["20000000-0000-4000-8000-000000000003"],"band":5.5,"skill":"READING"}],
  "lessons":[{"lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001","title":"Câu chủ đề nằm ở đâu","knowledgePointIds":["20000000-0000-4000-8000-000000000003"],"band":5.5,"skill":"READING","sortOrder":1}]
}
```

`packages` includes `PRACTICE_SET` and `PRACTICE_TEST` only. A package and its current version must both be
`PUBLISHED`; a lesson must be `PUBLISHED`. The mapped KP, its topic and course must be active, and content tied to a
lesson, topic or course must have an active published owner. Each item includes all its active target KP IDs so the
caller can match several requested KPs without further Content calls. Unknown or inactive KP IDs simply have no
matches. Package `band` is the highest of its owner course band and the bands of the KPs measured by its current
version; lesson `band` comes from its course. Both are at most `maxBand`. `skill` is the single skill of the active
mapped KPs, or `null` for a mixed-skill item. Packages are ordered by band, type, then ID; lessons by band, topic
order, lesson order, then ID. Invalid input returns `400`. Learning chooses at most five, removes already attempted
content, and builds learner-facing paths; this internal endpoint makes no learner-specific selection.

## `GET /package-versions/{id}`

Returns one PUBLISHED version with `{packageVersionId, packageId, packageType, topicId, rules, sections}`. `topicId` is required for `TOPIC_TEST` and nullable for other types. `rules` is the parsed version rules object; the V9 seed versions use `{}`. MVP final tests have no time limit: when `rules` has no time key, Assessment sets `expiresAt=null`. Defining a timed package rule is deferred beyond MVP. Sections have `{sectionId, title, skill, instructions, sortOrder, passage, audio?, items}`; `passage` is the full text of the attached `PASSAGE` asset or `null`. `audio` (added 2026-10-01, absent when the section has no `AUDIO` asset) is `{assetId, mediaUrl, durationSeconds, transcript}`, resolved like a lesson audio block; the transcript must not reach a learner before the result allows it. Items have `{questionVersionId, sortOrder, stem, options, answerSpec, explanation, maxScore, knowledgePointMappings}`. Each mapping has `{knowledgePointId, weight}`; weight does not affect grading or mastery.

Essay package items additionally carry `guide` (learner-facing Markdown or null) and
`images: [{mediaUrl, altText}]` (added 2026-10-20). Images come from validated `IMAGE` assets linked to the question or
its section, ordered by asset-link `sortOrder` and asset ID. Content resolves every image reference through the same
media policy as lesson images; the stored reference is not included in the internal response. Both images and guides
are read in bulk for a package version. Auto-graded items have `guide: null` and `images: []`. Learning forwards only
these teaching fields for course practice essays; answer specs, grading explanations and chart facts remain internal.

X1 example (one representative item shown; the actual version contains Q2, Q14, Q15, Q16):

```json
{
  "packageVersionId":"20000000-0000-4000-8000-000000000401","packageId":"20000000-0000-4000-8000-000000000301","packageType":"TOPIC_TEST","topicId":"10000000-0000-4000-8000-000000000001","rules":{},
  "sections":[{"sectionId":"20000000-0000-4000-8000-000000000701","title":"Street trees","skill":"READING","instructions":null,"sortOrder":1,"passage":"A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.\n\nB. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.\n\nC. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away.","items":[{"questionVersionId":"20000000-0000-4000-8000-000000000002","sortOrder":1,"stem":"What is the passage mainly about?","options":[{"optionKey":"A","content":"How oak trees grow in cities","sortOrder":1},{"optionKey":"B","content":"The ways street trees improve city life","sortOrder":2},{"optionKey":"C","content":"A 2019 study of stress in Toronto","sortOrder":3}],"answerSpec":{"type":"CHOICE","correct":"B"},"explanation":"Cả ba đoạn cùng nói lợi ích của cây đường phố.","maxScore":1,"knowledgePointMappings":[{"knowledgePointId":"10000000-0000-4000-8000-000000000002","weight":1.0}]}]}]
}
```

These are new internal DTOs. Existing public Content DTO names such as `answerSpecJson` and `rulesJson` are unchanged by this contract. Error responses follow Content Service's existing `ErrorResponse` shape; a missing/unpublished resource is `404` and invalid search input is `400`.

Package publishing enforces one owner per `question_id` across its versions: any lesson or a published version of one `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST`, `MOCK_TEST` or `PLACEMENT_TEST` package; versions of the same package may share questions and drafts do not reserve them. Conflicts return `422` with `details.code = QUESTION_ALREADY_USED`. The question bank's immutable `purpose` (V17) is `LEARNING` for lessons, practice, topic tests and course tests, and `EXAM` for mock/placement tests; publishing the wrong purpose returns `422` with `details.code = QUESTION_PURPOSE_MISMATCH`. Course tests use the same ordered question locks and ownership/purpose checks before publishing. Since 2026-10-07 they may contain Reading or Listening questions with auto-gradable CHOICE or FILL answer specs (including the legacy CHOICE shape) and Writing essays with `answerSpec.passBand`; Speaking questions, missing/unsupported specs, blank or non-text CHOICE answers, empty/invalid FILL alternatives and essays without a valid `passBand` return `400` without publishing. An essay in a `PRACTICE_SET`, `TOPIC_TEST` or `COURSE_TEST` must have a numeric `passBand` from 0 to 9 in steps of 0.5, else `400`. A lesson's `PRACTICE_SET` may only contain questions whose skill is in the lesson's `skills`, else `422` with `details.code = INVALID_PACKAGE_LESSON`. Public creation of `COURSE_TEST` returns `400`, like `TOPIC_TEST`; both are seeded only. Public question creation accepts optional `purpose` (default `LEARNING`), public question responses include it, and `GET /api/content/questions` accepts a purpose filter; the internal lesson/package payloads remain as defined above. `QUIZ` and legacy `LESSON` packages are outside these publishing checks.

## Error types (phase 3a)

`GET /internal/learning-content/error-types` returns error types with both `status=ACTIVE` and `review_status=APPROVED` as a JSON array of `{"code","name","category","severity","fixHint","knowledgePointId"}`, ordered by code. `knowledgePointId` is nullable and identifies the knowledge point linked to the error type when one is configured. Assessment reads it to refuse any error analysis code that is not in the reviewed catalogue (`422`, `details.code = UNKNOWN_ERROR_CODE`). `category` is one of `GRAMMAR`, `VOCABULARY`, `COHERENCE`, `TASK`, `READING_STRATEGY`, `LISTENING_STRATEGY`, `SPELLING_NUMBER`. `severity` is 1 to 3. The public management endpoints are `GET`, `POST` and `PUT` under `/api/content/error-types`; the code cannot change after creation.

## Essay rubrics (phase 11)

Content owns versioned IELTS Writing rubrics. Each rubric has a `code`, `task` (`TASK_1` or `TASK_2`), positive
`version`, `status` (`DRAFT`, `ACTIVE`, `RETIRED`), `sourceNote`, ordered `criteria`, band descriptors and optional
original examples. Each criterion is `{code,name,description,sortOrder,bandDescriptors:[{band,descriptor}]}`; each
example is `{band,prompt,essay,commentary}`. Bands are numeric half-band values from 0 through 9. The schema allows
only one `ACTIVE` rubric per task. Seed descriptors for bands 4–9 are original paraphrases of the public IELTS Writing
band descriptors with the source recorded; seed essays and prompts are original material.
Migration `V50__rubrics.sql` seeds both `TASK_1` and `TASK_2` as `ACTIVE` for bootstrap. Their `sourceNote` explicitly
identifies the material as an AI draft that has not been reviewed by an IELTS expert; `ACTIVE` is a runtime status.

The authoring routes require `CONTENT_AUTHOR`:

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/content/admin/rubrics?task=TASK_1\|TASK_2` | List draft versions for the task. |
| POST | `/api/content/admin/rubrics` | Create a `DRAFT` (`201`). Body contains `code`, `task`, `version`, `sourceNote`, `criteria`, and optional `examples`. |
| PUT | `/api/content/admin/rubrics/{code}/{version}` | Replace a draft's task, source note, criteria and examples. Active or retired versions cannot be edited. |
| POST | `/api/content/admin/rubrics/{code}/{version}/activate` | Activate a complete draft and retire the existing active rubric for that task. |

`GET /api/content/rubrics?task=TASK_1|TASK_2` is readable by `CONTENT_AUTHOR` and `EXAMINER` and returns a
zero-or-one-element array containing the active rubric for that task. Invalid task or rubric data returns `400`;
attempting to edit an active or retired version returns `409 RUBRIC_NOT_EDITABLE`. A missing active rubric has no
public list entry; internal active and historical lookups return `404` when no rubric is available.

The internal prompt projection is available to service callers using their forwarded internal JWT and correlation id:

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/internal/learning-content/rubrics/active?task=TASK_1\|TASK_2` | Return the active rubric as one object with `code`, `task`, `version`, `sourceNote`, `criteria`, and `examples`. |
| GET | `/internal/learning-content/rubrics/{code}/{version}` | Return that saved active or retired version, never a draft. Used by both Learning and Assessment to keep grading on the activated prompt version. |

Each internal criterion also carries `knowledgePointId` (UUID, nullable, added 2026-10-21): the knowledge point that the
criterion is evidence for, from `writing_criterion_knowledge_points` keyed by task and criterion code
(`V69__writing_criterion_knowledge_points.sql`). It does not depend on the rubric version. The additive
`subKnowledgePoints` field is an ordered array of `{id,code,name,description}` entries from
`writing_criterion_sub_knowledge_points` (`V71__writing_criterion_sub_knowledge_points.sql`), filtered to active
knowledge points; it is `[]` when the criterion has none. For example, Task 2's CC criterion returns
`[{"id":"…","code":"DEMO_WRITING_TOPIC_SENTENCE","name":"…","description":"…"},{"id":"…","code":"DEMO_WRITING_LINKING","name":"…","description":"…"}]`.
Admin and public rubric responses do not include criterion knowledge point mappings or sub-knowledge points.

These projections are internal only. Gateway denies browser/client access to `/internal/**`; Learning and Assessment must
not read Content's database directly. Rubric data is input to the grader and does not include answer keys.

For each task, both graders use the `ACTIVE` projection only before its first Assessment `ai_prompt_activations` row.
Learning checks `GET /internal/assessments/ai-prompt-activations?task=TASK_1` using an authenticated internal JWT and
`learning.assessment.base-url` (default `http://localhost:8083`). A successful `{"task":"TASK_1","promptVersion":null}`
response permits bootstrap. A version string requires the historical rubric lookup above, including `RETIRED` versions;
subsequent Content activation does not switch the runtime prompt. Activation or rubric outages never permit fallback to
the active projection. Evaluation candidates continue to use Content's current `ACTIVE` rubric.
See [Assessment evaluation and activation](./assessment-grading-v1.md#ai-rubric-evaluation-and-activation-phase-11).
