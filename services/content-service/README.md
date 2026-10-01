# Content Service

Content Service owns the canonical IELTS curriculum: topics, knowledge points (KPs), the question bank, content
packages, vocabulary and learning videos. It is a Spring MVC downstream service on port `8082`, reached through the
Gateway at `/api/content/**`. Schema migrations live in `src/main/resources/db/migration`.

## Band ranges

Topics and knowledge points carry an optional IELTS band range (`bandMin`, `bandMax`), in 0.0–9.0 and half-band
steps. AI Learning uses it to build a learner's path:

- a KP is in the path when its effective `bandMin` is empty or not above the goal's target band;
- a KP whose effective `bandMax` is not above the learner's placement band is tested out.

Rules:

- Either end may be empty (open). Both empty means "every band", so content without a band reaches every learner.
- A KP's **effective** range is its own range when it has one, otherwise its topic's range. An own range replaces the
  topic's as a whole; the two are never combined end by end. Child topics do not inherit their parent's range.
- `GET /api/content/knowledge-points` returns both the own range (`bandMin`/`bandMax`) and the effective range
  (`effectiveBandMin`/`effectiveBandMax`). `GET /api/content/topics` returns each topic's range.
- `POST`/`PUT /api/content/topics` and `POST /api/content/knowledge-points` accept `bandMin`/`bandMax`. An invalid
  range returns 400. `PUT /topics` replaces the whole topic, so omitting the band clears it.

## Verification

```powershell
mvn -pl services/content-service -am test
```

The Testcontainers migration tests (`KnowledgePointLearningTypeMigrationTest`, `BandRangeMigrationTest`) are skipped
when Docker is unavailable.
