# Internal learning content API v1

**Status: approved 2026-10-01. Updated 2026-10-07: course sequence metadata and course final-test packages; derived `skills` on topics, lessons and practice sets; mixed-skill course tests.**

Content Service owns these ten routes under `/internal/learning-content` (three practice routes added 2026-10-03; course test packages added 2026-10-07). They require a verified internal JWT; callers forward the request bearer and `X-Correlation-Id`. Gateway must explicitly deny client access to `/internal/**`. Responses here can contain `answerSpec` and `explanation` and must never be proxied to a learner unchanged. This contract uses Content's camelCase JSON names. Vocabulary/video catalog data belongs to Library Service; Content V7 removed those tables. The `VOCABULARY` block contains logical sense IDs only, with no Content-owned vocabulary entity or cross-database lookup.

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

Returns an array of ACTIVE topics that belong to an ACTIVE course and have at least one PUBLISHED lesson (since 2026-10-07 a topic no longer needs a stored `skill`), ordered by the stored topic `skill` label (topics without one last), then `course.bandLevel`, then `sortOrder`, then `topicId` (course filtering/order added 2026-10-07; a PUBLISHED `TOPIC_TEST` has not been required since 2026-10-03). Topics without a course or in an inactive course are excluded. Each topic carries `course: {courseId, code, name, bandLevel, hasCourseTest}`; `hasCourseTest` is true when that course has a PUBLISHED `COURSE_TEST` package with a non-null current version. Course metadata is joined in the topic query. `skills` is the union of its PUBLISHED lessons' skills and `skill` follows the compatibility rule in *Derived skills*. `hasTopicTest` is true when the topic has a PUBLISHED `TOPIC_TEST` package with a current version; a topic without one (the demo `DEMO_WRITING`) is passed by completing its lessons. This is the single curriculum read used to build and refresh the MVP path. Each topic includes its ACTIVE knowledge points, ordered by `created_at`, then `id`; no topic band range or answer key is returned. `hasPracticeSet` is true exactly when at least one package matches `POST /practice-sets/search` for that KP with `excludePackageIds=[]` and default `minQuestions=3`. Since content V16 every KP of the free and premium Reading and Listening topics has one; the Writing KPs have none.

```json
[
  {
    "topicId": "10000000-0000-4000-8000-000000000001",
    "code": "DEMO_READING", "name": "Demo IELTS Reading", "sortOrder": 900, "requiredFeatureKey": null,
    "skill": "READING", "hasTopicTest": true,
    "course": {"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5,"hasCourseTest":true},
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000003","code":"DR_TOPIC_SENTENCE","name":"Câu chủ đề","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":true}
    ]
  },
  {
    "topicId": "20000000-0000-4000-8000-000000000002",
    "code": "TFNG_SKILLS", "name": "True / False / Not Given", "sortOrder": 910, "requiredFeatureKey": null,
    "skill": "READING", "hasTopicTest": true,
    "course": {"courseId":"30000000-0000-4000-8000-000000000001","code":"IELTS_5_5","name":"IELTS 5.5","bandLevel":5.5,"hasCourseTest":true},
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000005","code":"TFNG_FALSE_VS_NOT_GIVEN","name":"False hay Not Given","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":true}
    ]
  }
]
```

The example shows representative KPs; the real response includes **all** ACTIVE KPs of each returned topic (KP1–KP4 for `DEMO_READING`, KP5 for `TFNG_SKILLS`). `hasPracticeSet` uses the same eligible-package predicate as search, including the no-overlap rule below. It is computed in a batched query, not by one query per KP. The topic object is `{topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course, skills}`; `skill` and `hasTopicTest` were added 2026-10-03, `course` and `skills` on 2026-10-07. Existing readers that ignore unknown fields keep working. Content V15 moved the Writing knowledge points `DEMO_READING_W1_CHART` and `DEMO_READING_W2_OPINION`, with their essay blocks (same block ids), from Reading lessons L3/L4 to `DEMO_WRITING` lessons W1/W2 (sort 950, no final test). `requiredFeatureKey` (added 2026-10-02) is the Access feature needed to learn the topic, `null` when free; the seed marks `PREMIUM_MATCHING_INFO` and `PREMIUM_SENTENCE_COMPLETION` (sort 930, 940) with `PREMIUM_CONTENT`.

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
| `VOCABULARY` | `vocabularySenseIds: UUID[]` | Ordered logical IDs in Library Service; MVP seed has no such block. |
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

```json
[{"packageId":"20000000-0000-4000-8000-000000000501","packageVersionId":"20000000-0000-4000-8000-000000000601","code":"PS-KP1-A","questionCount":4,"matchedQuestionCount":4}]
```

Practice search eligibility also excludes question versions used by `COURSE_TEST`, `MOCK_TEST` or `PLACEMENT_TEST` packages. The same exclusion applies to availability counts and `hasPracticeSet`. Reservation checks also include `COURSE_TEST` questions, so they cannot be reused as practice material.

The request/response illustrates KP1 and its `PS-KP1-A` package. The V4 one-question package is ineligible with the default `minQuestions=3`.

## `GET /lessons/{id}/practice-sets`

Added 2026-10-03. Returns the lesson's Practice: PUBLISHED `PRACTICE_SET` packages with a current version whose `lessonId` is this lesson, ordered by `code`. Each item is `{packageId, packageVersionId, code, title, questionCount, knowledgePointIds, requiredFeatureKey, skills}`; no question, answer or explanation. Optional `?skill=X` (added 2026-10-07) returns only sets whose `skills` is exactly `[X]`, so mixed sets are excluded; an unknown value or `ALL` returns `400`. `404` when the lesson is unknown or unpublished; `[]` when it has no Practice (the Writing lessons).

```json
[{"packageId":"26000000-0000-4000-8000-080000000003","packageVersionId":"26000000-0000-4000-8000-090000000003","code":"PS-TF-A","title":"Luyện thêm PS-TF-A: False hay Not Given","questionCount":3,"knowledgePointIds":["20000000-0000-4000-8000-020000000005"],"requiredFeatureKey":null,"skills":["READING"]}]
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

## `GET /package-versions/{id}`

Returns one PUBLISHED version with `{packageVersionId, packageId, packageType, topicId, rules, sections}`. `topicId` is required for `TOPIC_TEST` and nullable for other types. `rules` is the parsed version rules object; the V9 seed versions use `{}`. MVP final tests have no time limit: when `rules` has no time key, Assessment sets `expiresAt=null`. Defining a timed package rule is deferred beyond MVP. Sections have `{sectionId, title, skill, instructions, sortOrder, passage, audio?, items}`; `passage` is the full text of the attached `PASSAGE` asset or `null`. `audio` (added 2026-10-01, absent when the section has no `AUDIO` asset) is `{assetId, mediaUrl, durationSeconds, transcript}`, resolved like a lesson audio block; the transcript must not reach a learner before the result allows it. Items have `{questionVersionId, sortOrder, stem, options, answerSpec, explanation, maxScore, knowledgePointMappings}`. Each mapping has `{knowledgePointId, weight}`; weight does not affect grading or mastery.

X1 example (one representative item shown; the actual version contains Q2, Q14, Q15, Q16):

```json
{
  "packageVersionId":"20000000-0000-4000-8000-000000000401","packageId":"20000000-0000-4000-8000-000000000301","packageType":"TOPIC_TEST","topicId":"10000000-0000-4000-8000-000000000001","rules":{},
  "sections":[{"sectionId":"20000000-0000-4000-8000-000000000701","title":"Street trees","skill":"READING","instructions":null,"sortOrder":1,"passage":"A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.\n\nB. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.\n\nC. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away.","items":[{"questionVersionId":"20000000-0000-4000-8000-000000000002","sortOrder":1,"stem":"What is the passage mainly about?","options":[{"optionKey":"A","content":"How oak trees grow in cities","sortOrder":1},{"optionKey":"B","content":"The ways street trees improve city life","sortOrder":2},{"optionKey":"C","content":"A 2019 study of stress in Toronto","sortOrder":3}],"answerSpec":{"type":"CHOICE","correct":"B"},"explanation":"Cả ba đoạn cùng nói lợi ích của cây đường phố.","maxScore":1,"knowledgePointMappings":[{"knowledgePointId":"10000000-0000-4000-8000-000000000002","weight":1.0}]}]}]
}
```

These are new internal DTOs. Existing public Content DTO names such as `answerSpecJson` and `rulesJson` are unchanged by this contract. Error responses follow Content Service's existing `ErrorResponse` shape; a missing/unpublished resource is `404` and invalid search input is `400`.

Package publishing enforces one owner per `question_id` across its versions: any lesson or a published version of one `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST`, `MOCK_TEST` or `PLACEMENT_TEST` package; versions of the same package may share questions and drafts do not reserve them. Conflicts return `422` with `details.code = QUESTION_ALREADY_USED`. The question bank's immutable `purpose` (V17) is `LEARNING` for lessons, practice, topic tests and course tests, and `EXAM` for mock/placement tests; publishing the wrong purpose returns `422` with `details.code = QUESTION_PURPOSE_MISMATCH`. Course tests use the same ordered question locks and ownership/purpose checks before publishing. Since 2026-10-07 they may contain Reading or Listening questions with auto-gradable CHOICE or FILL answer specs (including the legacy CHOICE shape) and Writing essays with `answerSpec.passBand`; Speaking questions, missing/unsupported specs, blank or non-text CHOICE answers, empty/invalid FILL alternatives and essays without a valid `passBand` return `400` without publishing. An essay in a `PRACTICE_SET`, `TOPIC_TEST` or `COURSE_TEST` must have a numeric `passBand` from 0 to 9 in steps of 0.5, else `400`. A lesson's `PRACTICE_SET` may only contain questions whose skill is in the lesson's `skills`, else `422` with `details.code = INVALID_PACKAGE_LESSON`. Public creation of `COURSE_TEST` returns `400`, like `TOPIC_TEST`; both are seeded only. Public question creation accepts optional `purpose` (default `LEARNING`), public question responses include it, and `GET /api/content/questions` accepts a purpose filter; the internal lesson/package payloads remain as defined above. `QUIZ` and legacy `LESSON` packages are outside these publishing checks.
