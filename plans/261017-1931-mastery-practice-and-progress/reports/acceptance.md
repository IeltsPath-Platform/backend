# Acceptance: mastery, practice and progress (plan 261017-1931)

Updated 2026-10-18. All seven demos ran against a real Compose stack through the Gateway (gateway, user, content, assessment,
access, learning, library, PostgreSQL, RabbitMQ). Docker's disk image was moved from C: to E: first. Running them found
three defects that no test caught; they are fixed (see "Defects found while running the demos").

## Verification run so far

| Check | Result |
|---|---|
| `mvn -B -ntp -pl services/access-service test` | 65 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL 15 Testcontainers ran (2026-10-18) |
| Content V45 migration test | `GrammarVocabularySeedMigrationTest`: 2 tests, 0 failures/errors/skips, PostgreSQL 15.19 Testcontainers applied V45 (included in full verify) |
| `mvn -B -ntp verify` whole reactor | BUILD SUCCESS, 24 min 27 s, 1,751 tests, 0 failures, 0 errors, 0 skipped; Testcontainers ran (2026-10-18, code changes later committed without further source edits). Per module: common-security 8, api-gateway 9, config-server 1, eureka-server 1, user-service 115, access-service 65, content-service 421, library-service 67, learning-service 714, assessment-service 309, game-service 15, notification-service 1, community-service 25 |
| `docker compose config --quiet` | exit 0 (2026-10-18) |
| First Compose attempt | Failed: BuildKit `rpc error ... EOF`, C: had no free space. Fixed by moving Docker's data disk to E: (C: from 2.6 GB to 28.2 GB free) |
| Compose stack after the move | Build and start succeeded; every service `healthy` and `UP` in Eureka |
| Learning tests after the snapshot fix | `SnapshotIntegrationTest` 7, `LessonSubmissionIntegrationTest` 16, plus report and inactivity tests: 33 tests, 0 failures |

## Defects found while running the demos

1. **`access-service` had no RabbitMQ settings** (`access-service.yaml` and `docker-compose.yml`): it connected to `localhost`
   and its health was DOWN. Added the `rabbitmq` block and the `RABBITMQ_*` variables with `depends_on: rabbitmq`.
2. **`learning-service` had no `USER_SERVICE_URL` in Compose**, and its default pointed at port 8081 (library). The
   learning goal (target band, gap) and the league display names could not be read. Compose now passes
   `http://user-service:8085` and the default is 8085.
3. **The snapshot job failed on every run.** It needed the skill and band of each knowledge point and read them from Content
   with the caller's gateway token, which a scheduled job does not have (`Authenticated gateway JWT required`). The
   `knowledge_point_catalog` table (already refreshed from Content whenever a learner opens the course list) now also keeps
   the course band (migration V31), and the job reads skill and band from it. Limit: the catalog is filled the first time a
   learner opens the course list after the migration; a run before that writes snapshots with no skill or band, and the
   next run replaces them.

## Demos

| # | Demo | Learning-side tests | Compose run |
|---|---|---|---|
| 1 | Two pieces of evidence show "not enough data"; more evidence on another day gives a real tier | `Mp1MasteryIntegrationTest.aPointWithTooLittleEvidenceIsInsufficientAndStaysOutOfTheBasketButKeepsItsBlockingReview`, `evidenceOverTwoDaysIsJudgedAndAnOldPointFadesBackToInsufficient`. Only under `learning.mastery.policy=MP1`; the default stays `DEEPTUTOR` | Done with `LEARNING_MASTERY_POLICY=MP1` set through a temporary Compose override (not committed), then `POST /admin/rule-configs/recompute`. A point with 8 pieces of evidence all on one day: mastery 0.333, tier `INSUFFICIENT` (under the default formula the same point is 0.538, `PRIORITY`). With half of that evidence moved back one day (edited in the database) and recomputed: 0.338, `PRIORITY`. Only the "one day" side of the rule was run; the "fewer than 3 pieces" side is held by the learning tests. The service was put back on `DEEPTUTOR` afterwards |
| 2 | Free practice on a point, fail, no lesson locked; only 2 sets left → `FREE_PRACTICE_EXHAUSTED` | `FreePracticeIntegrationTest` (16 tests): reserve, no blocking review, band and plan filters, course score untouched (`aFreeSetNeverEntersTheProcessScoreOfTheCourse`) | Done on `DR_TOPIC_SENTENCE` (4 sets, 2 kept back). The start route reopens an unfinished set instead of taking a new one. One failed set (all wrong): `passed` false, hearts cost, no blocking review created (`GET /reviews` empty), lesson statuses unchanged. The next start: `409 FREE_PRACTICE_EXHAUSTED`. The course score was not read on Compose |
| 3 | Report shows 6 skills, estimated band, gap to target | `GetLearnerReportUseCaseTest` (skills, focus, band, target), `ReportControllerTest`, `BandEstimatorTest`, `BandProjectionTest` | Done. `GET /report`: 6 skills (`READING` mastery 1.0 with data, the other five empty), estimated band 5.5, target 7.0 and gap 1.5 (goal row written in User Service's database for the run), projection `NOT_ENOUGH_DATA` because there are no four weeks of history. A learner whose only point is below 60% gets no band (`value` null) |
| 4 | Run the snapshot job, read the weekly report and the trend | `SnapshotIntegrationTest` (run, idempotence, weekly change, history), `ReportControllerTest` | Done after fixing the job (defect 3), with the weekly cron set to every minute through a temporary override. The snapshot of the week of 2026-10-05 was written for both learners (events were moved into that week in the database); `GET /report/weekly`: 10 answered, 10 correct, accuracy 1.0, 100 minutes, `READING` 1.0 with direction `NEW`; `GET /report/history`: one week, band 5.5. A later run left 2 rows, no duplicates, no error in the log. Only one week of data, so "up/down against the previous week" was not seen on Compose |
| 5 | Review 5 flashcards, a correct card moves to box 2 | Library (Codex): `FlashcardLeitnerReviewTest`, `FlashcardLeitnerControllerTest` in library-service (not rerun by me) | Done through the Gateway on a freshly built library-service. Five `MANUAL` cards: all box 1 and due. Four correct reviews: box 2, next review in 2 days. One wrong review: box 1, next review in 1 day. Replaying the first `requestId`: `200`, still box 2, `reviewCount` 1. Nothing due afterwards. A second correct review of a card made due again: box 3, next review in 4 days. The intervals match `[1,2,4,8,16]` |
| 6 | A learner away 3 days gets an `INACTIVE` flag; a submission closes it | `InactivityAndActivityIntegrationTest.aLearnerWhoStoppedWhileOnACourseGetsOneFlagAndTheirNextAnswerClosesIt`, `anInactiveFlagBecomesOneItemInTheExaminerQueue` | Done. A learner's answers were moved 4 days back in the database and the job's first run set to 15 seconds through a temporary override. Only that learner got an `INACTIVE` flag (the other had answered today); `GET /admin/work-items` showed one `EXAMINER` item of kind `LEARNER_FLAG` with `status` OPEN. After that learner answered again the flag became `DISMISSED` with the note "learner answered again" |
| 7 | Schedule a package, it publishes itself | Content (Codex): V43 scheduler integration, 2 tests | Done. The seed has no `DRAFT` version, so a draft package with no sections was created in the database; `PUT .../schedule` with a time 2 minutes ahead returned `200`. About 40 seconds after the due time the version was `PUBLISHED`, `publish_at` cleared, no `lastPublishError`, and the package `PUBLISHED`. The package was deleted afterwards. A publishable package with real questions, and the failure path (`lastPublishError`), were not run on Compose; the existing integration test mocks the publishing step |

## Open

- Phase 1: the approved Grammar/Vocab proposal is seeded by V45 as 13 `INACTIVE` points, linked as prerequisites to strategy
  points. The PostgreSQL migration test confirms the new graph is acyclic; this tree criterion is closed.
- Phase 3 is closed with a known limit: the tree's `practiceAvailable` is an upper bound (it does not subtract sets the band,
  plan or essay filter removes) and only 10 candidates are fetched before filtering. Exact figures need Content to filter by
  band and feature; the start route answers `FREE_PRACTICE_EXHAUSTED` when nothing is left.
- Compose acceptance is done for demos 1–7 (see above). The data edits made to set the scenarios up (events moved in time,
  a draft package, a target band, five questions tagged HARD and put back) were made in the local Compose database only.
- Not exercised on Compose: the failure path of a scheduled publication, a published package with real questions, and the
  week-over-week change in the weekly report (only one week of snapshots existed).
- Contract review found `knowledge-tree-v1.md` consistent with controller behavior; the Access, user, SRS, and scheduled-publish
  contracts and Content/Library READMEs were updated and committed.
- `docs/system-architecture.md` does not exist in the repo.
