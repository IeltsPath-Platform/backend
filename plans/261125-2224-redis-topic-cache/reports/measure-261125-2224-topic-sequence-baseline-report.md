# Phase 01 baseline: Learning topic sequence

Date: 2026-11-26 (Asia/Saigon)
Branch: `feat/redis-content-cache`
Commit under test: `cd7bbdd7a864c753312bbd13569bed595d52a4e7`
Working-tree-only migration adjustment: `V148__course_import_drafts.sql` renamed to `V152__course_import_drafts.sql`; SQL unchanged.

## Environment

- Windows host running Docker Desktop Linux containers; Docker Engine 29.6.1, 12 CPUs, 7,825 MiB RAM.
- Java 21.0.6; PostgreSQL `15-alpine`; Redis `7-alpine`.
- Before measurement, the existing 11-container app stack was healthy and each container was below 1% CPU in the `docker stats --no-stream` snapshot.
- Testcontainers started an isolated PostgreSQL and Redis. All 152 Content migrations applied successfully, then PostgreSQL `ANALYZE` ran.
- Hikari pool size 4. Per variant: 100 warm-ups then 200 sequential samples.
- A is `JdbcLearningContentReader.topicSequence(3, includeErrorTraps)` + `TopicSequenceResponse.from` + Jackson serialization. B is Redis GET + Jackson parse to `List<TopicSequenceResponse>`.

## Results

| Variant | Topics | Knowledge points | JSON bytes | A: DB + map + JSON p50 / p95 / max | B: Redis GET + parse p50 / p95 / max |
| --- | ---: | ---: | ---: | --- | --- |
| `base` | 48 | 109 | 106,354 | 175.144 / 237.252 / 286.435 ms | 2.919 / 4.026 / 5.395 ms |
| `error-traps` | 48 | 133 | 118,019 | 990.154 / 1,215.887 / 1,509.105 ms | 2.811 / 4.328 / 7.650 ms |

The focused Maven run passed. It validated and applied all migrations through V152. The full measurement test took 405 seconds. During the error-trap samples the isolated PostgreSQL container reached about 97% CPU; results are single-client measurements on this machine and should be read with that load in mind.

## HTTP path and request call counts

The live Learning `/actuator/prometheus` scrape exposed `http_client_requests_seconds` samples for Eureka only; there was no `client_name="content"` series to measure topic-sequence latency. No topic-sequence HTTP request was made during the observed window. Therefore the required comparison of Redis p95 against half the actual Learning-to-Content HTTP p95 is unavailable and cannot yet be marked passed.

Static call-site count per request flow, based on current code:

| Flow | Topic-sequence HTTP calls before cache | Conditions |
| --- | ---: | --- |
| Learner report | 1 | 2 only if the error-trap catalog is empty and the fallback runs |
| Review basket | 1 | When the request has review candidates to describe; otherwise 0 |
| Suggestions | 2 | One via `KnowledgePointDirectory`, one in `currentBand` |
| Quests / weakness target | 1 | When `QuestService.weaknessTarget` is evaluated |
| Submission history | 1 | Non-empty history; 0 for an empty result |

These are source-level call counts, not request-volume measurements from production.

## AOF write estimate

At one refresh per minute for both keys:

- `(106,354 + 118,019) × 1,440 = 323,097,120 bytes/day` (about 308 MiB/day).
- This is the uncompressed value-byte estimate only; it excludes Redis AOF framing and rewrite effects.

## Gate status

- Neither response exceeds 1 MB.
- A p95 is not below 20 ms for either variant (237 ms and 1,216 ms).
- Redis p95 is much lower than A p95, but the plan compares Redis with the actual HTTP path; that HTTP latency was not available from the live metrics. The 2× HTTP comparison remains unverified.
- Per plan, stop after Phase 01 and get approval before Phase 02. No production cache code has been added.
