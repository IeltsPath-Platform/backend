# Content Service

Content Service owns the IELTS curriculum: topics, knowledge points (KPs), the question bank, content packages,
reading content and assets. It is a Spring MVC downstream service on port `8082`, reached through the Gateway at
`/api/content/**` except the vocabulary and video paths routed to Library Service. Schema migrations live in
`src/main/resources/db/migration`.

Library Service owns the five vocabulary/video catalog tables. Content V7 drops those tables; run that destructive
migration only in Testcontainers until its use on a shared `content_db` is approved. Library checks a video's topic
through `GET /api/content/topics/{id}`. Content implements `POST /internal/game-content/snapshots` only for `GRAMMAR`;
Library implements the same [snapshot contract](../../docs/contracts/game-content-snapshot-v1.md) for `VOCABULARY`.

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

## Media

- `GET /api/content/assets/{id}` is for `ADMIN` and `CONTENT_AUTHOR` only: asset text can be a transcript. Learners
  receive media through the internal lesson and package payloads that Learning Service and Assessment relay.
- Content is the only place that builds media URLs (`MediaReferencePolicy`). Images use an `https://` URL or a
  `data:image/png|jpeg|svg+xml;base64,` URI; audio uses an `https://` URL or an object key joined to
  `content.media.base-url` (env `CONTENT_MEDIA_BASE_URL`, an https prefix; blank means keys cannot resolve and the
  read fails with `INVALID_MEDIA_REFERENCE`).
- The V12 Listening seed references 8 files to upload under the base URL (record them from the transcripts in the
  seed; mp3 files are not committed):

  | Key | Seconds | Used by |
  | --- | --- | --- |
  | `listening/demo/ls1.mp3` | 45 | lesson LS1 |
  | `listening/demo/ls2.mp3` | 50 | lesson LS2 |
  | `listening/demo/numM.mp3` | 30 | practice set PS-NUM |
  | `listening/demo/spellM.mp3` | 35 | practice set PS-SPELL |
  | `listening/demo/museum.mp3` | 50 | practice set PS-PARA |
  | `listening/demo/trapM.mp3` | 30 | practice set PS-TRAP |
  | `listening/demo/hotel.mp3` | 45 | final test X3 |
  | `listening/demo/tour.mp3` | 40 | final test X4 |

## Verification

```powershell
mvn -pl services/content-service -am test
```

The Testcontainers migration tests (including `CatalogRemovalMigrationTest`) are skipped
when Docker is unavailable.
