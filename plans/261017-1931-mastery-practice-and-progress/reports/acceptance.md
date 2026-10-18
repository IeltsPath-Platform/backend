# Acceptance: mastery, practice and progress (plan 261017-1931)

Updated 2026-10-18. Only what was run is marked as run. "Compose" columns are empty until someone runs the demo on a clean
Compose stack; nothing below claims they were.

## Verification run so far

| Check | Result |
|---|---|
| `mvn -B -ntp -o -pl services/learning-service test` | 712 tests, 0 failures, 0 skipped, PostgreSQL 15 Testcontainers ran (2026-10-18) |
| Content suite | Codex reports 419 tests, 0 failures (V44 included); not rerun by me |
| Library suite | Codex reports 67 tests, 0 failures (V4 vocab seed); not rerun by me |
| `mvn -B -ntp verify` whole reactor | not run |
| `docker compose config --quiet` | not run |

## Demos

| # | Demo | Learning-side tests | Compose run |
|---|---|---|---|
| 1 | Two pieces of evidence show "not enough data"; more evidence on another day gives a real tier | `Mp1MasteryIntegrationTest.aPointWithTooLittleEvidenceIsInsufficientAndStaysOutOfTheBasketButKeepsItsBlockingReview`, `evidenceOverTwoDaysIsJudgedAndAnOldPointFadesBackToInsufficient`. Only under `learning.mastery.policy=MP1`; the default stays `DEEPTUTOR` | |
| 2 | Free practice on a point, fail, no lesson locked; only 2 sets left → `FREE_PRACTICE_EXHAUSTED` | `FreePracticeIntegrationTest` (16 tests): reserve, no blocking review, band and plan filters, course score untouched (`aFreeSetNeverEntersTheProcessScoreOfTheCourse`) | |
| 3 | Report shows 6 skills, estimated band, gap to target | `GetLearnerReportUseCaseTest` (skills, focus, band, target), `ReportControllerTest`, `BandEstimatorTest`, `BandProjectionTest` | |
| 4 | Run the snapshot job, read the weekly report and the trend | `SnapshotIntegrationTest` (run, idempotence, weekly change, history), `ReportControllerTest` | |
| 5 | Review 5 flashcards, a correct card moves to box 2 | Library (Codex): `FlashcardLeitnerReviewTest`, `FlashcardLeitnerControllerTest` in library-service (not rerun by me) | |
| 6 | A learner away 3 days gets an `INACTIVE` flag; a submission closes it | `InactivityAndActivityIntegrationTest.aLearnerWhoStoppedWhileOnACourseGetsOneFlagAndTheirNextAnswerClosesIt`, `anInactiveFlagBecomesOneItemInTheExaminerQueue` | |
| 7 | Schedule a package, it publishes itself | Content (Codex): V43 scheduler integration, 2 tests | |

## Open

- Phase 1: the Grammar/Vocab seed was approved on 2026-10-18; Codex still has to add it (migration number V45, because V43 and
  V44 are taken) with the new points `INACTIVE`. The tree criterion "Grammar/Vocab points linked to strategy points, no cycle"
  stays open until then.
- Phase 3 is closed with a known limit: the tree's `practiceAvailable` is an upper bound (it does not subtract sets the band,
  plan or essay filter removes) and only 10 candidates are fetched before filtering. Exact figures need Content to filter by
  band and feature; the start route answers `FREE_PRACTICE_EXHAUSTED` when nothing is left.
- Codex: contracts `flashcard-srs-v1.md`, `content-scheduled-publish-v1.md`, `user-internal-v1.md` (learning goal), READMEs of
  library and content.
- Whoever runs Compose: the seven demos, `mvn verify` over the whole reactor, `docker compose config --quiet`.
- `docs/system-architecture.md` does not exist in the repo.
