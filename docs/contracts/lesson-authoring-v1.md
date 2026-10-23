# Lesson authoring API v1

Lesson authoring routes require `CONTENT_AUTHOR`. They operate on the content service's lesson records. The
learner-facing `/internal/learning-content/lessons/**` response remains unchanged.

## List lessons for a topic

`GET /api/content/admin/topics/{topicId}/lessons` returns all lesson states ordered by `sortOrder`, with `id`, `code`,
`title`, `summary`, `summaryMarkdown`, `status`, `sortOrder`, `estimatedMinutes`, `blockCount`, and
`knowledgePointIds`. An unknown topic returns `404`.

## Create and edit lessons

`POST /api/content/admin/topics/{topicId}/lessons` creates a `DRAFT` lesson and returns its authoring detail. It accepts:

```json
{
  "code": "READING-INFERENCE-1",
  "title": "Finding an inference",
  "summary": "Read for implied meaning.",
  "summaryMarkdown": "Optional longer lesson summary.",
  "estimatedMinutes": 12,
  "sortOrder": 1
}
```

`sortOrder` defaults to the last lesson position for the topic. Duplicate codes and topic/order conflicts return `409`.

`GET /api/content/admin/lessons/{lessonId}` returns the full authoring detail, including all blocks and question
versions in any publication state. The detail includes IDs for existing blocks so clients can keep them stable.

`PUT /api/content/admin/lessons/{lessonId}` replaces the lesson's `title`, `summary`, `summaryMarkdown`, and
`estimatedMinutes`. It is allowed in any lesson state and returns the updated detail.

`PUT /api/content/admin/topics/{topicId}/lessons/order` accepts `{ "lessonIds": ["<id>", "<id>"] }`, which must
contain every lesson of the topic exactly once. It returns `204`; unknown, duplicate, or incomplete IDs return `409`
with `details.code = LESSON_SORT_CONFLICT`.

## Knowledge points

`PUT /api/content/admin/lessons/{lessonId}/knowledge-points` accepts
`{ "knowledgePointIds": ["<active-knowledge-point-id>"] }` and replaces the lesson's KP set. Every ID must exist and
be `ACTIVE`; an invalid set returns `422` with `details.code = LESSON_BLOCK_INVALID`.

## Blocks

`POST /api/content/admin/lessons/{lessonId}/blocks` creates a block and returns it, including the generated `id`.
`PUT /api/content/admin/lessons/{lessonId}/blocks/{blockId}` updates the block in place and always returns the same
`id`. `DELETE` at that path returns `204`. `PUT /api/content/admin/lessons/{lessonId}/blocks/order` accepts
`{ "blockIds": ["<id>", "<id>"] }`, must include each block exactly once, and returns `204`.

Block request fields are selected by `blockType`:

```json
{
  "blockType": "TEXT",
  "textContent": "Read the passage and identify the implied claim.",
  "knowledgePointIds": ["<active-knowledge-point-id>"],
  "sortOrder": 1
}
```

`ASSET` uses `assetId`; `VOCABULARY` uses `vocabularySenseIds`; `EXERCISE` uses `questionVersionIds`. Only `TEXT`
accepts `knowledgePointIds`. A block must supply the field matching its type and no incompatible type-specific fields.
Invalid client block data returns `422` with `details.code = LESSON_BLOCK_INVALID`.

`TEXT`, `ASSET`, and `VOCABULARY` blocks may be edited or removed in any lesson state. For an `EXERCISE` block,
changing its questions or removing the block is allowed only while the lesson is `DRAFT`; otherwise the API returns
`409` with `details.code = LESSON_EXERCISE_LOCKED`. Block order may be changed in any state. Reordering uses a
temporary sort range inside the same transaction so existing negative vocabulary sort positions cannot collide.

## Publish and archive

`POST /api/content/admin/lessons/{lessonId}/publish` validates the full lesson and returns the updated detail when
successful. A lesson must have at least one active KP and one block; every exercise must be valid, each attached
question version must be `PUBLISHED` with `purpose = LEARNING` and not owned by another lesson or package, each TEXT
block KP must be active, and each vocabulary sense must exist in Library. Invalid lessons return `422` with
`details.code = LESSON_NOT_PUBLISHABLE` and an `issues` array of safe reason codes. An unavailable Library Service
returns `503`.

`POST /api/content/admin/lessons/{lessonId}/archive` archives the lesson and returns its updated detail. Learning
then omits the lesson from learner-facing reads.

## Reorder slides or documents

`PUT /api/content/admin/lessons/{lessonId}/resources/order` with body `{ "type": "SLIDE" | "DOCUMENT", "resourceIds": ["<uuid>"] }`
returns `204`. `resourceIds` must be exactly the lesson's resources of that type, each once; anything else returns `400`.
The resources of that type get orders `0..n-1` in one transaction. Resources of the other type are not touched.

All mutations serialize on the lesson row using `SELECT ... FOR UPDATE`. Replacing lesson or block order is atomic.

## Topic sort order

A topic's `sortOrder` is its position among siblings: topics with the same parent (roots share one group) must have
different orders. Creating a topic, or changing its parent or order, to a value another sibling already holds returns
`409` with `details.code = "SORT_ORDER_TAKEN"`. The other topic is never moved. Saving a topic without changing its parent
or order is not checked, so existing data is not blocked by this rule.

### Reordering topics

`PUT /api/content/admin/topics/order` (also under `/api/content/topics/order`), `CONTENT_AUTHOR` only, puts all topics
of one parent in a new order in a single transaction. A swap done with two single saves would be refused by the rule
above, so a client moves a topic with this call.

```json
{ "parentTopicId": null, "topicIds": ["<uuid>", "<uuid>", "<uuid>"] }
```

- `parentTopicId` is the parent whose children are ordered; `null` (or left out) means the top-level topics, which are
  one group across all courses.
- `topicIds` must name **every** topic of that group exactly once and nothing else, in the wanted order. A missing,
  extra, repeated or foreign id, or an empty list, returns `400` and changes nothing. An unknown parent returns `404`.
- The group keeps the sort orders it already uses and hands them out in the new sequence, so other groups and the gaps
  between numbers stay as they were. A group whose sort orders are not all distinct (older data has ties, mostly among
  top-level topics) is numbered one after another from its smallest order, which also removes the tie.
- Returns `204`. Read the tree again for the stored orders. Topics whose position did not change are not written.
- It does not change a topic's parent. Moving a topic to another parent is still a `PUT /{id}` with the new
  `parentTopicId` and a free `sortOrder`.
