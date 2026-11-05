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

Migration V104 creates `IELTS_SPEAKING@1` as a starter `DRAFT`. Its prompt and paraphrased descriptors are authoring
notes, not approved IELTS guidance; an IELTS-qualified reviewer must revise and activate it before AI grading can use
it.
