# Knowledge tree API v1

The content service owns knowledge points and prerequisite edges. A prerequisite edge is directed from the prerequisite
to the dependent knowledge point.

## Manage a knowledge point

The following routes require `CONTENT_AUTHOR`:

| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/content/knowledge-points` | Create a knowledge point. |
| `PUT` | `/api/content/knowledge-points/{id}` | Replace its topic, code, name, kind, learning type, skill, description, CEFR level, can-do statement, and scope. |
| `DELETE` | `/api/content/knowledge-points/{id}` | Set status to `INACTIVE`; repeated calls return `204`. |
| `POST` | `/api/content/knowledge-points/{id}/activate` | Set status to `ACTIVE` and return the knowledge point. |
| `GET` | `/api/content/knowledge-points/{id}/prerequisites` | Return direct prerequisites, including inactive points and their status. |
| `PUT` | `/api/content/knowledge-points/{id}/prerequisites` | Replace the full prerequisite set (up to 300 IDs); duplicate IDs are collapsed and an empty array clears the set. |

New or changed knowledge point codes must match `^[RLWGVS](55|60|65|70)_[A-Z0-9_]{2,60}$`. `G` codes are for
`GRAMMAR` points with `skill=ALL`; `V` codes are for `VOCABULARY` points with `skill=ALL`; `S` codes are for points
with `skill=SPEAKING`. `R`, `L`, and `W` retain their existing format checks. An unchanged legacy code is accepted by
`PUT`. An active point must have a `learningType`, a `cefrLevel` and a `canDo`; activation without any of them returns
`400`.

Create and update requests accept `cefrLevel` (`A1` through `C2`), `canDo`, `scopeIn`, and `scopeOut`. Creation
requires a non-blank `canDo` (`400` otherwise). On creation, a missing `cefrLevel` is inferred from the owning course
band when available (up to 4.5 is `B1`, up to 6.0 is `B2`, up to 7.5 is `C1`); when the topic has no course and no level
is sent, creation returns `400`. `scopeIn` and `scopeOut` default to `null`. Updating an active point with a null or
blank `cefrLevel` or `canDo` returns `400`; `scopeIn` and `scopeOut` can still be cleared. Inactive draft points may
carry any of these fields as `null`. These fields are also included in the knowledge point response.

## CEFR order of prerequisites

A prerequisite never has a higher CEFR level than the point that depends on it; the same level is allowed. A point or
prerequisite without a level is ignored by the rule. Replacing prerequisites with a point above the dependent returns
`422` with `details.code = "PREREQUISITE_LEVEL_INVALID"` and an `invalidIds` array of the offending prerequisites. The
same code is returned when `PUT /api/content/knowledge-points/{id}` changes `cefrLevel` so that an existing edge would
run from a higher level to a lower one; `invalidIds` then lists the neighbours that conflict.

## Check the graph

`GET /api/content/knowledge-tree/health` requires `CONTENT_AUTHOR` or `EXAMINER`. It checks the active
points and the edges between active points and returns:

```json
{
  "isolatedIds": ["<knowledgePointId>"],
  "missingMetadataIds": ["<knowledgePointId>"],
  "levelViolations": [{ "from": "<prerequisiteId>", "to": "<knowledgePointId>" }]
}
```

`isolatedIds` lists active points that are neither a prerequisite nor a dependent of another active point.
`missingMetadataIds` lists active points without a `cefrLevel` or a `canDo`. `levelViolations` lists edges whose
prerequisite has a higher level than its dependent. A healthy graph returns three empty arrays.

Changing `topicId` returns `409 KNOWLEDGE_POINT_IN_USE` when the point is linked to a published question version or a
lesson. Other fields, including `code`, can still be updated.

Invalid prerequisite sets return `422` with `details.code = "PREREQUISITE_INVALID"` and an `invalidIds` array when an ID
is missing, inactive, or is the target itself. A cycle returns `422` with `details.code = "PREREQUISITE_CYCLE"` and a
`cycle` array containing the IDs along the cycle, including the repeated start/end ID. Writes to the tree are serialized
with a transaction-scoped advisory lock so concurrent replacements cannot introduce a cycle.

## Read the knowledge tree

`GET /api/content/knowledge-tree` requires `CONTENT_AUTHOR` or `EXAMINER`.

Optional query parameters:

| Parameter | Meaning |
| --- | --- |
| `courseId` | Restrict nodes to topics in this course. Edges crossing the filter boundary are omitted. |
| `skill` | Restrict nodes to one skill. |
| `includeInactive` | Include inactive points. Honored for `CONTENT_AUTHOR`; ignored for `EXAMINER`. |

The response is `{ "nodes": [...], "edges": [...] }`. Each node contains `id`, `code`, `name`, `skill`, `kind`,
`learningType`, `band`, `courseId`, `topicId`, `lessonIds`, `status`, `cefrLevel`, `canDo`, `scopeIn`, and `scopeOut`.
The four metadata fields are nullable. `band` and `courseId` are `null` when the point's topic is not attached to a
course. Each edge is `{ "from": "<prerequisiteId>", "to": "<knowledgePointId>" }`. An edge is included only when
both endpoint nodes are present in the response.

## Internal learning read

`POST /internal/learning-content/knowledge-points/prerequisites` accepts:

```json
{ "ids": ["<knowledgePointId>"] }
```

It returns an object keyed by active requested knowledge point IDs. Each value lists active direct prerequisite IDs. An
active point with no prerequisites maps to an empty array. Unknown or inactive requested IDs are omitted. Inactive
prerequisites are filtered out. The gateway blocks `/internal/**` from public clients.

## Learner tree trap children (Learning API)

The learner-facing tree contract is in [free-practice-v1](free-practice-v1.md#free-practice-and-the-learner-tree-v1).
Learning reads Content's topic sequence with `includeErrorTraps=true` and nests a child's mastery under its parent only
after the learner has evidence for that child. Trap children are not independent prerequisite nodes and are not offered
free-practice routes.
