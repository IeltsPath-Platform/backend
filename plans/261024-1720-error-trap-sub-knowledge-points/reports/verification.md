# Phase 8 verification

Date: 2026-10-25

## Commands and results

| Command | Result |
| --- | --- |
| `mvn -q -pl services/content-service -am package` | Exit 0. Surefire XML: 168 suites, 958 tests, 0 failures/errors, 1 skipped. Docker/Testcontainers was available. The one skipped test is the opt-in import-template file writer (`writeImportTemplate` not set). |
| `mvn -q -pl services/learning-service -am test` | Final run after acceptance fixes: exit 0. Surefire XML: 129 suites, 941 tests, 0 failures/errors/skips. Docker/Testcontainers was available. |
| `docker compose config --quiet` | Exit 0. Docker CLI printed warnings that `C:\Users\Admin\.docker\config.json` could not be read in this sandbox; Compose configuration validation completed successfully. |
| `npm.cmd test` in `frontend/` | Exit 1. 131 files: 130 passed, 1 failed; 1,063 tests passed and 2 failed. Both failures are existing copy assertions in `src/features/flashcards/lib/owl-reactions.test.ts` (confused-pose text expected “conservation”; tired-pose text expected one of four phrases). The Claude phase-7 report records a baseline failure in the same file and an intermittent tired-pose failure; phase 7 was not changed in this run. |
| `npm.cmd run build` in `frontend/` | Exit 0 (`tsc -b && vite build`). Vite emitted the existing chunk-size warning for assets above 500 kB. |

## Compose walkthrough and end-to-end verification

The Compose HTTP walkthrough was not run. `docker compose ps --format json` returned no running services; the test run used
Testcontainers for PostgreSQL and did not start the full Compose stack or call the learner HTTP API.

The replacement is the real Testcontainers application-flow test
`FreePracticeIntegrationTest.freePracticeSubmissionCreatesAnErrorPatternForItsCompletedParentLesson`. It supplies a
Content stub with distractors and a child-KP catalog, calls the actual free-practice start/submit use cases, and verifies
PostgreSQL `kp_evidence` (`error_trap`), the child `kp_mastery_profiles` row/tier, and a pending optional `ERROR_PATTERN`
review on the parent. This proves the application/database path, not the Compose HTTP path. Separate Testcontainers tests
exercise lesson, counted Practice, review, diagnostic and Content-label-failure submissions.

No HTTP request was made by code while recording evidence. The no-HTTP rule applies to production evidence-writing logic;
manual HTTP verification is allowed and simply was not performed in this run.

## Documentation updated

- `AGENTS.md`: `learning.error-trap.*`, source `error_trap` weight 0.3, practice cap exclusion, local catalog mapping, and
  parent-only aggregates.
- `docs/system-architecture.md`: Learning and Content responsibilities and rollout behavior.
- `services/content-service/README.md` and `services/learning-service/README.md`: child KP lifecycle, local evidence mapping,
  feedback, selection, and report/tree behavior.
- `plans/reports/brainstorm-261024-0135-error-code-personalized-review-report.md`: adopted P1–P4 plan decisions and final
  review-unit behavior.
- Phase 6 learner API contracts: lesson learning, free practice, diagnostics, learner report, and knowledge tree.

## Self-check

- [x] Content package and full learning test commands exited 0; Testcontainers was not skipped for Docker absence.
- [x] Compose configuration validated successfully.
- [x] Frontend production build succeeded.
- [ ] Frontend tests are not fully green: two pre-existing/intermittent flashcard copy assertions remain failing (phase 7
  was intentionally left untouched).
- [x] An end-to-end Testcontainers application-flow test replaced the Compose walkthrough; Compose HTTP itself remains unrun.

Status: DONE_WITH_CONCERNS — backend packaging and documentation are complete; frontend test suite retains the two known
unrelated copy assertion failures documented above.
