# Premium-only practice tests in the 4-skill free practice library

Status: ready to implement. Product decisions confirmed by the owner on 2026-11-25. Fixes defect DEF-ASM-03.
Do not push or open PRs; the owner reviews first.

| Repo | Branch | From |
| --- | --- | --- |
| backend | `feat/premium-practice-tests` | `feat/premium-grading-benefits` (PR #197, not merged; it touches the same Learning use cases and adds the per-request Premium lookup reused here). If #197 is merged first, rebase onto `main`. |
| frontend | `feat/premium-practice-tests` | `main` |

## 1. Goal and decisions

A practice test tagged Premium (`content_packages.required_feature_key = 'PREMIUM_CONTENT'`) can be opened and taken only by
a learner with an active Premium subscription. Untagged tests stay free for everyone. Authors and examiners are not
affected.

| Question | Decision |
| --- | --- |
| What a normal learner sees | The card is shown with a "Premium" badge and a lock; clicking it opens the upgrade dialog. |
| Preview | None. The card shows title, skill, question count and time only. |
| Premium ends mid-attempt | Checked when the test is started; an attempt already started can be answered and submitted. |
| Scope | All 4 skills of the free practice library. |

Defect DEF-ASM-03: a learner without Premium started Premium test `PT-LISTENING-03` by calling the API directly and read
all 20 questions. No layer checks the tag today. The fix must hold for direct API calls, not only in the UI.

"Premium" means an `ACTIVE` subscription whose `endsAt` is null or in the future: the rule behind
`GET /api/access/me/subscription` (`200` active, `204` none).

## 2. Where a learner can reach a test's content today (verified on `main`)

| Skill | How the learner opens the test | Route | Check today |
| --- | --- | --- | --- |
| Reading, Listening | Start an attempt; the attempt holds a snapshot of the test | `POST /api/assessments/attempts` → `StartAssessmentAttemptUseCase` | none |
| Reading, Listening | After start, everything is read from the attempt | `/api/assessments/attempts/{id}/…` (owner only) | owner check |
| Reading | Passage by section id (not used by the frontend) | `GET /api/content/reading/sections/{sectionId}` (`ReadingController`, role `CUSTOMER` allowed) | none |
| Writing | The workspace page loads the task | `GET /api/content/practice-tests/{versionId}/sections/{sectionId}/writing-task` (`PracticeTestController`, used by `WritingWorkspacePage`) | none |
| Writing | Submitting the essay | `SubmitFreeWritingUseCase` (Learning) | none |
| Speaking | The workspace page loads the task | `GET /api/content/practice-tests/{versionId}/sections/{sectionId}/speaking-task` (used by `SpeakingWorkspacePage`) | none |
| Speaking | Creating a submission (before recording) | `CreateSpeakingSubmissionUseCase` (Learning) | none |

The tag already exists end to end for authoring and listing: authors set "Truy cập FREE/PREMIUM" (`PackageCreateForm`,
`AccessLevelCompatibility`), seeds tag `PT-READING-ISP-03`, `PT-LISTENING-03`, `PT-WRITING-03`, `PT-WRITING-06`, and the
catalog `POST /api/content/practice-tests/search` returns `requiredFeatureKey` per card. The frontend receives it
(`readingPracticeApi.ts`) but no component uses it. The internal version response used by Assessment and Learning
(`PackageVersionContentResponse`) does not carry it.

## 3. Where the rule is enforced

The "start" of a test is the first server call that gives its content:

| Skill | Start (checked) | Not checked again (an attempt in progress can finish) |
| --- | --- | --- |
| Reading, Listening | Assessment: starting the attempt | answers, sections, submit, result of that attempt |
| Writing | Content: loading the writing task | — see note |
| Speaking | Content: loading the speaking task; Learning: creating the submission | uploads, grading and settling of that submission |

Note on Writing: there is no server-side attempt for free writing; the essay is sent once. Learning also checks when the
essay is submitted, so the API cannot be used to submit to a Premium prompt without Premium. Consequence: a learner whose
Premium ends between opening the task and submitting is refused (`403`) and keeps the text in the page. This is the only
gap against the "can finish" decision, and it is rare (one essay, minutes). Confirmed by the owner on 2026-11-25: check
on submit too, no grace period. The reading passage route keeps learner access under the Premium rule (also confirmed).

Common errors (all services, Vietnamese message, stable `details.code`):

| Case | Status | `details.code` | Message |
| --- | --- | --- | --- |
| Tagged test, learner not Premium | `403` | `PREMIUM_REQUIRED` | "Đề này dành cho tài khoản Premium. Nâng cấp để luyện đề này." |
| Premium lookup failed | `503` | `ENTITLEMENT_UNAVAILABLE` | "Chưa kiểm tra được gói Premium. Vui lòng thử lại sau." |

A failed lookup never opens a tagged test. An untagged test never calls Access.

## 4. Backend

### 4.1 Content

**Expose the tag to services.** Add nullable `requiredFeatureKey` to `PackageVersionContentResult` and
`PackageVersionContentResponse` (`GET /internal/learning-content/package-versions/{id}`), read from
`content_packages.required_feature_key` in the query behind `GetPackageVersionContentUseCase`. Additive only.

**Guard the three learner routes.**
- `GET /api/content/reading/sections/{sectionId}`, `…/writing-task`, `…/speaking-task`: when the section's package has a
  `requiredFeatureKey` and the caller is a `CUSTOMER` without `CONTENT_AUTHOR` or `EXAMINER`, require Premium. Authors and
  examiners keep today's behaviour and never call Access. Put the rule in the use cases (`GetReadingPassageUseCase` and
  the writing/speaking task use cases), not in the controller; the controller passes whether the caller is a learner
  only.
- New port `application/port/LearnerEntitlement` (`boolean premium()`, throws a dedicated exception when unavailable) and
  adapter `infrastructure/client/AccessLearnerEntitlementClient`: `GET /api/access/me/subscription` with the current
  bearer and `X-Correlation-Id`, through `ResilientCall.call("access", CallKind.READ, …)`, outside any transaction.
- New edge Content → Access: declare `ACCESS_SERVICE_URL` (default `http://localhost:8084`) in
  `infra/config-server/config-repo/content-service.yaml`, add the row to the HTTP table in `docs/system-architecture.md`.
  Access calls no service, so there is no cycle; confirm with `ServiceDependencyGraphTest`.
- Map the two exceptions in Content's `GlobalExceptionHandler` (through `VietnameseErrorMessages`).

**Tests.** Internal version response carries the key (tagged and free). For each of the three routes: tagged + non-Premium
learner → `403 PREMIUM_REQUIRED`; tagged + Premium → `200`; tagged + author/examiner → `200` without calling Access; free →
`200` without calling Access; lookup failure → `503`. Client test with `MockRestServiceServer` (`200`, `204`, `5xx`).

### 4.2 Assessment

- `ContentPackageProvider.PackageVersion`: add `requiredFeatureKey`; map it in `ContentPackageClient`.
- Port `application/port/LearnerEntitlement` and adapter `infrastructure/client/AccessLearnerEntitlementClient` as in 4.1,
  using the learner's bearer from the security context the way `AccessHumanGradingCreditClient` does (base URL
  `ACCESS_SERVICE_URL`, already used by Assessment).
- `StartAssessmentAttemptUseCase.execute`: after reading the version and before `creator.create`, when
  `type == OFFICIAL_PRACTICE` and `requiredFeatureKey != null`, require Premium. The use case is not transactional, so
  the call is outside any transaction. Other attempt types (placement, topic and course gates, mock, quiz) are unchanged.
- No check on later calls of an existing attempt.
- Map the exceptions in Assessment's `GlobalExceptionHandler`.
- Tests: free practice test never calls Access; tagged + Premium starts; tagged + non-Premium → `PREMIUM_REQUIRED` and no
  attempt is created; lookup failure → `ENTITLEMENT_UNAVAILABLE`; other types unaffected; client test; controller test for
  the `403` body. Add a test that reproduces DEF-ASM-03: starting the tagged `PT-LISTENING-03` version without Premium is
  refused.

### 4.3 Learning

- `LearningContentClient` package version record: add `requiredFeatureKey`; map it in its REST client.
- `SubmitFreeWritingUseCase`: when the prompt's package version is tagged, require `AccessClient.premium()` before any
  quota, balance or grading step and outside the learner lock and transaction. Reuse the Premium value the request
  already reads for grading costs (one Access call per request). A replay of an existing `requestId` answers as today
  without a new check.
- `CreateSpeakingSubmissionUseCase` (`execute` and `executeForAttempt`): same rule when the package version is tagged.
  Uploads, grading and settling of an existing submission are not checked again.
- Lesson practice sets and Practice inside a course are out of scope (they follow the course paywall).
- `AccessUnavailableException` during this check → `503 ENTITLEMENT_UNAVAILABLE`; the existing points path keeps its own
  `503 PAYMENT_UNAVAILABLE`.
- Tests: free prompt never checks; tagged + Premium passes; tagged + non-Premium → `PREMIUM_REQUIRED` with nothing stored;
  Access down → `ENTITLEMENT_UNAVAILABLE`; replay of a stored submission passes without a lookup.

### 4.4 Other routes

Search Content, Assessment and Learning for any other route reachable by a `CUSTOMER` that returns questions, passages,
audio, transcripts or task text by package, version or section id. Apply the same rule, or list each one with the reason
it is safe (for example owner-only attempt routes).

### 4.5 Contracts and docs

- `docs/contracts/learning-content-internal-v1.md`: `requiredFeatureKey` in the version response.
- The Assessment attempt contract, `lesson-writing-v1.md` (free writing) and `speaking-practice-grading-v1.md`: the
  Premium rule and the two errors.
- The content practice-test routes (reading passage, writing task, speaking task) in the matching contract, or a new
  short section if they have none.
- `docs/system-architecture.md`: Content → Access edge and the Assessment → Access subscription read.

## 5. Frontend

- **Card.** `features/practice/components/PracticeTestCard.tsx`: when `requiredFeatureKey` is set, show a "Premium" badge
  with a lucide icon (`Crown`) and text, using existing tokens with WCAG AA contrast. When `session.tier !== 'PREMIUM'`,
  also show a lock (`Lock`) and make the start action open the upgrade dialog instead of starting; the card stays visible.
  Premium learners see the badge and start normally. Apply to all 4 skills' catalog pages
  (`pages/PracticeCatalogPage.tsx`, `listening/ListeningPracticePage.tsx`, writing and speaking catalog pages).
- **Dialog.** Move `features/practice/speaking/PremiumPlansDialog.tsx` to `features/practice/components/` unchanged in
  behaviour and update its imports, so every skill uses it.
- **Server answer.** Starting an attempt, loading a writing or speaking task, submitting an essay and creating a Speaking
  submission: on `403` with `details.code === 'PREMIUM_REQUIRED'` open the dialog (covers a stale `session.tier` and
  direct links); on `503 ENTITLEMENT_UNAVAILABLE` show the backend message as a toast. On the writing page a refused
  submit keeps the essay text in the editor.
- **Types.** The catalog types of all 4 skills include `requiredFeatureKey`.
- **Tests** (Vitest, `@/test/render`): tagged card shows badge and lock; a non-Premium click opens the dialog and sends no
  start request; a Premium learner starts; `403 PREMIUM_REQUIRED` from start, task load or submit opens the dialog; a
  refused essay submit keeps the text.
- **Checks:** `npm run lint`, `npm run typecheck`, the practice tests. If Vitest reports "failed to find the current
  suite", delete `node_modules/.vite/vitest` and run again.

## 6. Patterns applied (backend AGENTS.md §3.9)

- New HTTP calls to Access go through `ResilientCall` as `READ` with the learner's bearer and `X-Correlation-Id`, outside
  transactions and locks; a failed lookup refuses only tagged tests.
- New service edge Content → Access declared in config-repo and the architecture table; no cycle.
- Additive response fields; errors in Vietnamese with `details.code`; contracts updated in the same change.
- No migration, no event, no scheduler, no new dependency.

## 7. Verification

1. Backend, narrowest first: the new tests in content, assessment and learning.
2. `ArchitectureTest` of content, assessment and learning, in a clean worktree (for example
   `git worktree add --detach C:/w/be <branch>`) or after a full `javac` compile: an open IDE rewrites `target/classes`
   and makes the result unreliable. Do not update `archunit_store`.
3. `infra/api-gateway`: `ServiceDependencyGraphTest`.
4. Frontend: lint, typecheck, practice tests.
5. Report results as they are, and say which tests did not run and why.

## 8. Acceptance

- DEF-ASM-03 is closed: starting `PT-LISTENING-03` by a direct API call without Premium returns `403 PREMIUM_REQUIRED`
  and creates no attempt.
- With a non-Premium learner token, the reading passage, writing task and speaking task routes of a tagged test return
  `403`; free tests return `200`.
- A non-Premium learner sees tagged tests with a badge and lock and gets the upgrade dialog instead of starting.
- A Premium learner opens and finishes tagged tests in all 4 skills.
- An attempt or Speaking submission started while Premium can be finished after Premium ends (Writing: see the note in 3).
- Access down: tagged tests answer `503`; free tests work.
- Authors, examiners and untagged tests behave exactly as before.

## 9. Risks

- Seeds already tag some tests as Premium; after release normal learners see them locked. Review the tagged list before
  release.
- `session.tier` can be stale after an upgrade until the session refreshes; the server is the authority and the `403`
  handler covers it.
- Content gains a runtime dependency on Access for tagged tests only; Access downtime affects only those tests.
