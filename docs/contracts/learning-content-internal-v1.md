# Internal learning content API v1

**Status: approved 2026-10-01.**

Content Service owns these six routes under `/internal/learning-content`. They require a verified internal JWT; callers forward the request bearer and `X-Correlation-Id`. Gateway must explicitly deny client access to `/internal/**`. Responses here can contain `answerSpec` and `explanation` and must never be proxied to a learner unchanged. This contract uses Content's camelCase JSON names. Vocabulary/video catalog data belongs to Library Service; Content V7 removed those tables. The `VOCABULARY` block contains logical sense IDs only, with no Content-owned vocabulary entity or cross-database lookup.

Examples use real codes, stems, answers, and passages from [`seed-content.md`](../../plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md). The existing V4 UUIDs for DEMO_READING and KP1 are real; other example UUIDs illustrate relationships rather than fixing V9 migration IDs. A `404` means the id is unknown or unpublished. Collections are sorted as described below.

## `GET /topic-sequence`

Returns an array of ACTIVE topics with at least one PUBLISHED lesson **and** one PUBLISHED `TOPIC_TEST` package, ordered by `sortOrder`, then `topicId`. This is the single curriculum read used to build and refresh the MVP path. Each topic includes its ACTIVE knowledge points, ordered by `created_at`, then `id`; no band or answer key is returned. `hasPracticeSet` is true exactly when at least one package matches `POST /practice-sets/search` for that KP with `excludePackageIds=[]` and default `minQuestions=3`. KP5 has no eligible practice package, so it is false and cannot create a review, although the TFNG test still measures it.

```json
[
  {
    "topicId": "10000000-0000-4000-8000-000000000001",
    "code": "DEMO_READING", "name": "Demo IELTS Reading", "sortOrder": 900,
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000003","code":"DR_TOPIC_SENTENCE","name":"Câu chủ đề","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":true}
    ]
  },
  {
    "topicId": "20000000-0000-4000-8000-000000000002",
    "code": "TFNG_SKILLS", "name": "True / False / Not Given", "sortOrder": 910,
    "knowledgePoints": [
      {"id":"20000000-0000-4000-8000-000000000005","code":"TFNG_FALSE_VS_NOT_GIVEN","name":"False hay Not Given","learningType":"PROCEDURE","skill":"READING","description":null,"hasPracticeSet":false}
    ]
  }
]
```

The example shows representative KPs; the real response includes **all** ACTIVE KPs of each returned topic (KP1–KP4 for `DEMO_READING`, KP5 for `TFNG_SKILLS`). `hasPracticeSet` uses the same eligible-package predicate as search, including the no-overlap rule below. It is computed in a batched query, not by one query per KP. The topic object is `{topicId, code, name, sortOrder, knowledgePoints}`; `skill` belongs only to each KP.

## `GET /topics/{id}/lessons`

Returns PUBLISHED lessons of the ACTIVE topic in `sortOrder` order, with IDs needed to persist progress. Each entry is `{lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds}`; both ID arrays contain UUIDs. Example for `DEMO_READING`:

```json
[
  {"lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001","code":"L1","title":"Câu chủ đề nằm ở đâu","summary":null,"sortOrder":1,"knowledgePointIds":["20000000-0000-4000-8000-000000000003"],"exerciseBlockIds":["20000000-0000-4000-8000-000000000202","20000000-0000-4000-8000-000000000205"]},
  {"lessonId":"20000000-0000-4000-8000-000000000102","topicId":"10000000-0000-4000-8000-000000000001","code":"L2","title":"Ý chính của cả bài","summary":null,"sortOrder":2,"knowledgePointIds":["10000000-0000-4000-8000-000000000002"],"exerciseBlockIds":["20000000-0000-4000-8000-000000000203"]}
]
```

The example includes two representative entries; the actual seed has L1–L4.

## `GET /lessons/{id}`

Returns `{lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, blocks}` for a PUBLISHED lesson. Blocks are ordered by `sortOrder` and share `{blockId, blockType, sortOrder}`:

| `blockType` | Additional fields | Rule |
| --- | --- | --- |
| `TEXT` | `textContent: string` | Lesson prose. |
| `ASSET` | `asset: {id, assetType, textContent, mediaReference, durationSeconds}` | `PASSAGE` uses `textContent`; media uses `mediaReference`. Unused values are `null`. |
| `VOCABULARY` | `vocabularySenseIds: UUID[]` | Ordered logical IDs in Library Service; MVP seed has no such block. |
| `EXERCISE` | `questions: Question[]` | Ordered by `sortOrder`. |

`Question` is `{questionVersionId, sortOrder, stem, options, answerSpec, explanation, knowledgePointIds}`. `options` is the Content option array `{optionKey, content, sortOrder}[]`, or **`null` for a fill answer**. A non-null array represents a choice question, including True/False/Not Given (all three keys appear). `answerSpec` is a JSON object conforming to [answer spec v1](answer-spec-v1.md), not a JSON-encoded string.

L1 example, using its Q12 fill block and roof passage:

```json
{
  "lessonId":"20000000-0000-4000-8000-000000000101","topicId":"10000000-0000-4000-8000-000000000001",
  "code":"L1","title":"Câu chủ đề nằm ở đâu","summary":null,"sortOrder":1,
  "knowledgePointIds":["20000000-0000-4000-8000-000000000003"],
  "blocks":[
    {"blockId":"20000000-0000-4000-8000-000000000201","blockType":"TEXT","sortOrder":1,"textContent":"Câu chủ đề (topic sentence) nêu ý mà cả đoạn triển khai. Trong bài IELTS nó thường là câu đầu đoạn, đôi khi là câu thứ hai sau một câu dẫn. Mẹo: thử bỏ câu đó đi. Nếu đoạn văn mất ý chung thì đó là câu chủ đề."},
    {"blockId":"20000000-0000-4000-8000-000000000204","blockType":"ASSET","sortOrder":2,"asset":{"id":"20000000-0000-4000-8000-000000000801","assetType":"PASSAGE","textContent":"A. Across Europe and North America, city planners are turning to green roofs to cope with hotter summers. A green roof is a layer of soil and plants laid over a waterproof membrane on top of a building.\n\nB. The most important benefit of green roofs is that they keep buildings cool. On a summer afternoon, a conventional black roof can reach 80°C, while a planted roof nearby rarely rises above 30°C. As a result, the floors below need far less air conditioning.\n\nC. Green roofs also manage rainwater. The soil soaks up much of a heavy shower and releases it slowly, which takes pressure off city drains. In Copenhagen, new flat roofs must now be planted for this reason.\n\nD. Not everyone is convinced. Critics point out that green roofs are expensive to install and need regular care, and that many older buildings are not strong enough to carry the extra weight.","mediaReference":null,"durationSeconds":null}},
    {"blockId":"20000000-0000-4000-8000-000000000202","blockType":"EXERCISE","sortOrder":3,"questions":[{"questionVersionId":"20000000-0000-4000-8000-000000000013","sortOrder":1,"stem":"Which sentence is the topic sentence of paragraph B?","options":[{"optionKey":"A","content":"The most important benefit of green roofs is that they keep buildings cool.","sortOrder":1},{"optionKey":"B","content":"On a summer afternoon, a conventional black roof can reach 80°C…","sortOrder":2},{"optionKey":"C","content":"As a result, the floors below need far less air conditioning.","sortOrder":3}],"answerSpec":{"type":"CHOICE","correct":"A"},"explanation":"Câu A nêu ý của cả đoạn (làm mát). Hai câu sau là số liệu và hệ quả để chứng minh.","knowledgePointIds":["20000000-0000-4000-8000-000000000003"]}]},
    {"blockId":"20000000-0000-4000-8000-000000000206","blockType":"TEXT","sortOrder":4,"textContent":"Luyện thêm với đoạn C và D."},
    {"blockId":"20000000-0000-4000-8000-000000000205","blockType":"EXERCISE","sortOrder":5,"questions":[{"questionVersionId":"20000000-0000-4000-8000-000000000012","sortOrder":3,"stem":"Complete with ONE WORD from paragraph D: people who doubt green roofs are called ______.","options":null,"answerSpec":{"type":"FILL","accepted":["critics"]},"explanation":"Đoạn D: \"Critics point out that…\". Không phân biệt hoa thường.","knowledgePointIds":["20000000-0000-4000-8000-000000000003"]}]}
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

## `POST /practice-sets/search`

Request is `{knowledgePointId, excludePackageIds, minQuestions, limit}`. Defaults are `[]`, `3`, and `1`; `limit` must be 1–10. Return PUBLISHED `PRACTICE_SET` packages with at least `minQuestions` total questions and at least one question mapped to the requested KP. Exclude packages in `excludePackageIds` and packages containing any question version also used in a lesson or `TOPIC_TEST` of **any** topic. Sort by matching-question count descending, then `packageId`; apply `limit` in the database. The `hasPracticeSet` predicate above is this eligibility rule before exclusion/limit.

```json
{"knowledgePointId":"10000000-0000-4000-8000-000000000002","excludePackageIds":[],"minQuestions":3,"limit":1}
```

```json
[{"packageId":"20000000-0000-4000-8000-000000000501","packageVersionId":"20000000-0000-4000-8000-000000000601","code":"PS-KP1-A","questionCount":4,"matchedQuestionCount":4}]
```

The request/response illustrates KP1 and its `PS-KP1-A` package. The V4 one-question package is ineligible with the default `minQuestions=3`.

## `GET /package-versions/{id}`

Returns one PUBLISHED version with `{packageVersionId, packageId, packageType, topicId, rules, sections}`. `topicId` is required for `TOPIC_TEST` and nullable for other types. `rules` is the parsed version rules object; the V9 seed versions use `{}`. MVP final tests have no time limit: when `rules` has no time key, Assessment sets `expiresAt=null`. Defining a timed package rule is deferred beyond MVP. Sections have `{sectionId, title, skill, instructions, sortOrder, passage, items}`; `passage` is the full text of the attached `PASSAGE` asset or `null`. Items have `{questionVersionId, sortOrder, stem, options, answerSpec, explanation, maxScore, knowledgePointMappings}`. Each mapping has `{knowledgePointId, weight}`; weight does not affect grading or mastery.

X1 example (one representative item shown; the actual version contains Q2, Q14, Q15, Q16):

```json
{
  "packageVersionId":"20000000-0000-4000-8000-000000000401","packageId":"20000000-0000-4000-8000-000000000301","packageType":"TOPIC_TEST","topicId":"10000000-0000-4000-8000-000000000001","rules":{},
  "sections":[{"sectionId":"20000000-0000-4000-8000-000000000701","title":"Street trees","skill":"READING","instructions":null,"sortOrder":1,"passage":"A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.\n\nB. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.\n\nC. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away.","items":[{"questionVersionId":"20000000-0000-4000-8000-000000000002","sortOrder":1,"stem":"What is the passage mainly about?","options":[{"optionKey":"A","content":"How oak trees grow in cities","sortOrder":1},{"optionKey":"B","content":"The ways street trees improve city life","sortOrder":2},{"optionKey":"C","content":"A 2019 study of stress in Toronto","sortOrder":3}],"answerSpec":{"type":"CHOICE","correct":"B"},"explanation":"Cả ba đoạn cùng nói lợi ích của cây đường phố.","maxScore":1,"knowledgePointMappings":[{"knowledgePointId":"10000000-0000-4000-8000-000000000002","weight":1.0}]}]}]
}
```

These are new internal DTOs. Existing public Content DTO names such as `answerSpecJson` and `rulesJson` are unchanged by this contract. Error responses follow Content Service's existing `ErrorResponse` shape; a missing/unpublished resource is `404` and invalid search input is `400`.
