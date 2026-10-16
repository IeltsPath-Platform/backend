# Knowledge tree API v1

The content service owns knowledge points and prerequisite edges. A prerequisite edge is directed from the prerequisite
to the dependent knowledge point.

## Manage a knowledge point

The following routes require `ADMIN` or `CONTENT_AUTHOR`:

| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/content/knowledge-points` | Create a knowledge point. |
| `PUT` | `/api/content/knowledge-points/{id}` | Replace its topic, code, name, kind, learning type, skill, and description. |
| `DELETE` | `/api/content/knowledge-points/{id}` | Set status to `INACTIVE`; repeated calls return `204`. |
| `POST` | `/api/content/knowledge-points/{id}/activate` | Set status to `ACTIVE` and return the knowledge point. |
| `GET` | `/api/content/knowledge-points/{id}/prerequisites` | Return direct prerequisites, including inactive points and their status. |
| `PUT` | `/api/content/knowledge-points/{id}/prerequisites` | Replace the full prerequisite set (up to 300 IDs); duplicate IDs are collapsed and an empty array clears the set. |

New or changed knowledge point codes must match `^[RLW](55|60|65|70)_[A-Z0-9_]{2,60}$`. An unchanged legacy code is
accepted by `PUT`. Codes are checked for format only; the prefix is not compared with the point's skill or course band.
An active point must have a `learningType`; activation without one returns `400`.

Changing `topicId` returns `409 KNOWLEDGE_POINT_IN_USE` when the point is linked to a published question version or a
lesson. Other fields, including `code`, can still be updated.

Invalid prerequisite sets return `422` with `details.code = "PREREQUISITE_INVALID"` and an `invalidIds` array when an ID
is missing, inactive, or is the target itself. A cycle returns `422` with `details.code = "PREREQUISITE_CYCLE"` and a
`cycle` array containing the IDs along the cycle, including the repeated start/end ID. Writes to the tree are serialized
with a transaction-scoped advisory lock so concurrent replacements cannot introduce a cycle.

## Read the knowledge tree

`GET /api/content/knowledge-tree` requires `ADMIN`, `CONTENT_AUTHOR`, or `EXAMINER`.

Optional query parameters:

| Parameter | Meaning |
| --- | --- |
| `courseId` | Restrict nodes to topics in this course. Edges crossing the filter boundary are omitted. |
| `skill` | Restrict nodes to one skill. |
| `includeInactive` | Include inactive points. Honored for `ADMIN` and `CONTENT_AUTHOR`; ignored for `EXAMINER`. |

The response is `{ "nodes": [...], "edges": [...] }`. Each node contains `id`, `code`, `name`, `skill`, `kind`,
`learningType`, `band`, `courseId`, `topicId`, `lessonIds`, and `status`. `band` and `courseId` are `null` when the
point's topic is not attached to a course. Each edge is `{ "from": "<prerequisiteId>", "to": "<knowledgePointId>" }`.
An edge is included only when both endpoint nodes are present in the response.

## Internal learning read

`POST /internal/learning-content/knowledge-points/prerequisites` accepts:

```json
{ "ids": ["<knowledgePointId>"] }
```

It returns an object keyed by active requested knowledge point IDs. Each value lists active direct prerequisite IDs. An
active point with no prerequisites maps to an empty array. Unknown or inactive requested IDs are omitted. Inactive
prerequisites are filtered out. The gateway blocks `/internal/**` from public clients.
