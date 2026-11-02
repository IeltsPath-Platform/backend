# Codex handoff: first-attempt suggested review

Implementer: **Codex**. Reviewer: Claude. Every decision is already made in the phase files. When something in the code
does not match what a phase file says (a method moved, a test name differs), stop and report `NEEDS_CONTEXT` with the
file and line; do not choose a different design.

## Branches and PRs

- Base: `feat/course-skill-board` in both repos (it holds the unmerged course-page work this plan builds on:
  `GetTopicLessonsUseCase`, the skill board, the unlock dialog). Do not branch from `main`.
- Backend repo (`backend/`): create `feat/first-attempt-suggested-review` from `feat/course-skill-board`.
- Frontend repo (`frontend/`): create `feat/first-attempt-suggested-review` from `feat/course-skill-board`.
- Do not push and do not open PRs; the user reviews first and opens them (this branch stacks on `feat/course-skill-board`).
- Commit per work package (below), conventional commits, no plan ids or phase numbers in messages, code or test names.
- Leave these alone and never stage them: tracked deletions and untracked files under
  `services/content-service/src/main/resources/db/migration/` (a local migration rename owned by someone else),
  and untracked files under `plans/` and `docs/demo/`.

## Work packages, in order

| Package | Phases | Repo | Done when |
|---------|--------|------|-----------|
| A | 1, 2 | backend | `PracticeReviewRuleTest`, `ReviewItemTest`, `JdbcReviewItemRepositoryIntegrationTest` green; whole learning-service suite green (behavior for learners unchanged yet) |
| B | 3, 4 | backend | every test listed in phases 3 and 4 green; whole learning-service suite green |
| C | 5 | frontend | the tests listed in phase 5 green; `npx tsc -b`, `npx eslint src/features/learning-path`, `npx vitest run src/features/learning-path`, `node --test tests/learning-path.test.mjs` green |
| D | 6 | backend | contract doc updated as written in phase 6 |

Package A must not change learner-visible behavior: until package B, `SubmitPracticeAttemptUseCase` still inserts with
`blocking = true` (phase 2 says how).

## Boundaries (do not touch)

- No Flyway migration. No change to `lesson_practice_passes`, `docker-compose.yml`, `infra/`, config-repo YAML.
- No change to `ReviewReevaluation`, `ErrorTrapReviews`, `DiagnosticFlow`, `GetReviewBasketUseCase`, `StartBasketReviewUseCase`,
  `CriterionReviews`, free practice classes.
- Do not weaken or delete an existing assertion except the ones phase 3 lists by line; those are replaced exactly as written.
- Do not read `.env`; do not call external services.

## Commands (run from `backend/`; Docker must be running for Testcontainers)

```bash
mvn -q -pl services/learning-service -am test -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl services/learning-service -am test          # whole service; confirm Testcontainers tests ran (count > 0)
```

Frontend (from `frontend/`): `npx tsc -b --pretty false`, `npx eslint src/features/learning-path`,
`npx vitest run src/features/learning-path`, `node --test tests/learning-path.test.mjs`.

## Report format (end of each package)

```text
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
Summary: one or two sentences
Tests: command run and pass/fail counts
Concerns/Blockers: optional, with file:line
```
