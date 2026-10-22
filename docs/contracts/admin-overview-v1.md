# Admin overview API v1

`GET /api/content/admin/overview` returns the counts of the content this service owns, for the overview page of the
authoring area. Roles: `CONTENT_AUTHOR`, `EXAMINER` (read only). Anyone else gets `403`.

```json
{
  "knowledgePoints": {
    "total": 70,
    "byStatus": { "ACTIVE": 57, "INACTIVE": 13 },
    "bySkill": { "READING": 12, "WRITING": 9 },
    "byCefr": { "B1": 20, "NONE": 3 }
  },
  "activeKnowledgePointsWithoutPublishedQuestion": 12,
  "draftQuestionVersions": 4,
  "lessons": { "DRAFT": 2, "PUBLISHED": 9, "ARCHIVED": 0 }
}
```

- `byStatus` counts every knowledge point. `bySkill` and `byCefr` count the `ACTIVE` points only; a point without a skill
  or a CEFR level is counted under `NONE`.
- `activeKnowledgePointsWithoutPublishedQuestion`: `ACTIVE` points that no `PUBLISHED` question version tests.
- `draftQuestionVersions`: question versions still in `DRAFT`, waiting to be published.
- `lessons` always lists the three statuses, with `0` for an empty one.

The problems of the knowledge graph (isolated points, missing level or can-do, edges that invert levels) are not here:
read them from `GET /api/content/knowledge-tree/health` ([knowledge-tree-v1](knowledge-tree-v1.md)). Work items and
question statistics come from Learning Service ([learning-staff-area-v1](learning-staff-area-v1.md)); the frontend
combines the sources, there is no aggregating endpoint.
