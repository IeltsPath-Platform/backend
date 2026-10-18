# Acceptance: mastery, practice and progress (plan 261017-1931)

Updated 2026-10-18. Demo cells stay blank until each API scenario has run. The clean-stack attempt is recorded below; Compose
startup failed, so none of the demos ran.

## Verification run so far

| Check | Result |
|---|---|
| `mvn -B -ntp -pl services/access-service test` | 65 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL 15 Testcontainers ran (2026-10-18) |
| Content V45 migration test | `GrammarVocabularySeedMigrationTest`: 2 tests, 0 failures/errors/skips, PostgreSQL 15.19 Testcontainers applied V45 (included in full verify) |
| `mvn -B -ntp verify` whole reactor | BUILD SUCCESS, 24 min 27 s, 1,751 tests, 0 failures, 0 errors, 0 skipped; Testcontainers ran (2026-10-18, code changes later committed without further source edits). Per module: common-security 8, api-gateway 9, config-server 1, eureka-server 1, user-service 115, access-service 65, content-service 421, library-service 67, learning-service 714, assessment-service 309, game-service 15, notification-service 1, community-service 25 |
| `docker compose config --quiet` | exit 0 (2026-10-18) |
| Clean Compose stack | `down -v` succeeded. `docker compose --parallel 1 up -d --build` failed before services started: BuildKit returned `rpc error ... EOF`, then Docker Engine became unavailable. Engine recovered, but Windows C: still reports 0 GB free after pruning 4.78 GB of inactive BuildKit cache. No demo API calls were made. |

The seven Compose cells below are intentionally blank because none of the scenarios ran.

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

- Phase 1: the approved Grammar/Vocab proposal is seeded by V45 as 13 `INACTIVE` points, linked as prerequisites to strategy
  points. The PostgreSQL migration test confirms the new graph is acyclic; this tree criterion is closed.
- Phase 3 is closed with a known limit: the tree's `practiceAvailable` is an upper bound (it does not subtract sets the band,
  plan or essay filter removes) and only 10 candidates are fetched before filtering. Exact figures need Content to filter by
  band and feature; the start route answers `FREE_PRACTICE_EXHAUSTED` when nothing is left.
- Compose acceptance remains open: free/compact Docker's C: storage, start the clean stack, then run demos 1–7 through Gateway.
- Contract review found `knowledge-tree-v1.md` consistent with controller behavior; the Access, user, SRS, and scheduled-publish
  contracts and Content/Library READMEs were updated and committed.
- `docs/system-architecture.md` does not exist in the repo.
