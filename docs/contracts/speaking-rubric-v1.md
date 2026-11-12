# Speaking rubric contract v1

Content Service owns versioned Speaking rubrics. Authors edit only `DRAFT` versions; activating a version retires the
previous active version for `SPEAKING`. Active versions are immutable.

Authoring endpoints use the existing rubric API:

- `GET /api/content/admin/rubrics?task=SPEAKING`
- `GET /api/content/admin/rubrics/versions?code=IELTS_SPEAKING`
- `POST /api/content/admin/rubrics`
- `PUT /api/content/admin/rubrics/{code}/{version}`
- `POST /api/content/admin/rubrics/{code}/{version}/activate`

The request and response fields are the existing rubric fields plus:

```json
{
  "systemPrompt": "General assessor guidance, up to 4000 characters",
  "criteria": [
    {
      "code": "FC",
      "name": "Fluency and coherence",
      "description": "Criterion overview",
      "sortOrder": 1,
      "bandDescriptors": [
        { "band": 4.0, "descriptor": "..." },
        { "band": 4.5, "descriptor": "..." }
      ]
    }
  ]
}
```

Speaking activation requires exactly FC, LR and GRA, with optional PR, and one descriptor for every half band from 0.0
through 9.0 for each criterion. PR is stored for learner feedback but is not an LLM grading criterion. The internal
`GET /internal/learning-content/rubrics/active?task=SPEAKING` and
`GET /internal/learning-content/rubrics/{code}/{version}` responses include `systemPrompt` and the mapped
`knowledgePointId` on each criterion. Learning uses the immutable version selected when grading begins.

Migration V116 creates `IELTS_SPEAKING@1` as a starter `DRAFT`. Its prompt and paraphrased descriptors are authoring
notes, not approved IELTS guidance; an IELTS-qualified reviewer must revise and activate it before AI grading can use
it.

Later versions are added by migration, never by editing an applied one: V117 (`@2`, first active), V122 (`@3`, deeper
feedback), V123 (`@4`, stricter marking: band 5 as the starting point, the lower band wins when two are close, concrete
limits for bands 4 to 9 of FC, LR and GRA) and V125 (`@5`, FC only: coherence is read sentence by sentence and sentences
that wander away from the question cap the band at 4.5 for three or more, 5.5 for one or two). Each new version retires the
previous one; grades keep the version they were marked with. The thresholds in `@4` and `@5` are proposals and must be
re-checked against the examiner calibration set.
