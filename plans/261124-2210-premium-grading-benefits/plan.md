# Premium: free AI grading, higher daily limits, unlimited examiner requests

Status: ready to implement. Decisions confirmed by the product owner on 2026-11-24. Branch: `feat/premium-grading-benefits`
(from `main`). Do not push or open a PR; the owner reviews first.

## 1. Goal

Premium differs from a normal account only in grading. Premium does **not** open courses: a Premium learner pays points
to unlock a course skill like anyone else.

| Benefit | Normal | Premium |
| --- | --- | --- |
| AI grading of Writing (lesson, Practice, free writing) | 20 a day, 3 points each | 20 a day, **0 points** |
| AI grading of Speaking | 5 a day, 5 points each | **10 a day, 0 points** |
| AI grading of essays in topic/course tests | free, 20 a day | free, **40 a day** |
| Ask an examiner to grade | not possible (`402`) | **unlimited requests**, 0 points |
| Hearts | standard maximum | 7 hearts, unchanged |
| Unlock a course skill, refill hearts, streak freeze | points | points, unchanged |

Essay grading in topic/course tests stays free for everyone: it sits on the progress path, so charging would block
learners (see `docs/system-architecture.md`, section on Writing fee policies).

Out of scope: learner and admin screens for plans, key activation and point history; payment by money; changing the
products' prices or the key-batch flow.

## 2. Definitions

- **Premium** = an `ACTIVE` subscription whose `endsAt` is null or in the future (`Subscription.isActive()`). No new
  concept: the same test Access already uses for `GET /api/access/me/subscription` (`200` when active, `204` otherwise).
- **Unlimited examiner requests** = a Premium subscription never needs a human-grading credit.
- Daily limits are configuration. Premium limits default to the values in the table.

## 3. Current state (verified in code)

Learning (`services/learning-service/src/main/java/com/ieltspath/learning`):
- `application/port/AccessClient` has `balance()`, `premium()` (learner bearer, `GET /api/access/me/subscription`) and
  `debit(...)`. `infrastructure/client/RestAccessClient` implements it with `ResilientCall`; it needs the learner's
  bearer, so it cannot be used from background jobs.
- `application/service/EssaySubmissionFlow` (shared by `SubmitLessonEssayUseCase`, `SubmitPracticeEssayUseCase`,
  `SubmitFreeWritingUseCase`): `requireBalance()`, `grade()` takes the daily quota
  (`settings.dailyGradingLimit()`), `charge()` debits `submission.pointCost()` then calls `finish`. The use cases create
  the submission with `flow.pointCost()`.
- `WritingSubmission.markGraded(UUID ledgerEntryId)` requires a non-null ledger id. Table
  `lesson_writing_submissions`: `point_cost >= 0` (V37) but
  `CHECK (status <> 'GRADED' OR (... AND debit_ledger_entry_id IS NOT NULL))` (V2).
- Speaking: `CreateSpeakingSubmissionUseCase.requireBalance()` (called at lines ~59 and ~75) and
  `SpeakingSubmission.start(..., settings.pointCost(), ...)`; `SettleSpeakingSubmissionUseCase.execute` debits with
  `referenceType "SPEAKING_AI"` and calls `SpeakingSubmission.graded(ledgerEntryId, now)`. Table `speaking_submissions`:
  `CHECK (point_cost > 0)` (V43) and `CHECK (status <> 'GRADED' OR (result IS NOT NULL AND debit_ledger_entry_id IS NOT
  NULL))` (V44). The daily quota is taken in the background `infrastructure/scheduler/SpeakingGradingJob.quota(userId)`
  (`settings.dailyGradingLimit()`), which has no learner bearer (`LearningWorkerTokenProvider` signs worker tokens).
- `application/service/PaywallGate.Request.lessonOpen(...)` and `knowledgePointOpen(...)` pass `premium()` into
  `PaywallPolicy.isOpen(...)`, which opens every lesson for Premium. `premiumActive()` is also used by
  `StartFreePracticeUseCase` (sets that need a plan feature) and must stay.
- `HeartsService.maxFor` gives Premium `premiumMax` (7): keep.

Assessment (`services/assessment-service/src/main/java/com/ieltspath/assessment`):
- `application/usecase/GradeGateEssayJobUseCase.gradeOne` takes
  `quota.tryConsume(job.userId(), day, dailyLimit)` with `@Value("${assessment.llm-grading.daily-limit:20}")`. It runs
  in a scheduler, no learner bearer. `infrastructure/client/AssessmentServiceTokenProvider` signs a worker token with
  role `CUSTOMER`.
- `infrastructure/client/AccessHumanGradingCreditClient` calls Access with the learner's own bearer.

Access (`services/access-service/src/main/java/com/ieltspath/access`):
- `application/usecase/ConsumeHumanGradingCreditUseCase`: `debit` → `Subscription.consumeHumanGradingCredit()` (throws
  `InsufficientCreditsException` at zero; no subscription also throws it); `consumeOnce` stores a
  `HumanGradingCreditConsumption(submissionId, userId, subscriptionId, remainingCredits, refundedAt)`; `refund`
  calls `Subscription.refundHumanGradingCredit()` which fails when `humanGradingCreditsUsed <= 0`.
- `api/controller/InternalHumanGradingController` (`/internal/access/human-grading`, `CUSTOMER`) and
  `InternalPointsController` (`/internal/access/points/debit`, caller must equal `userId`). The entitlement route
  `/api/access/users/{userId}/entitlement` is `ADMIN` only.
- Latest migrations: access V12, learning V58.

## 4. Phase 1 — Access: Premium lookup for services and unlimited examiner requests

### 1.1 Internal Premium lookup

- New `GET /internal/access/users/{userId}/premium` → `{"premium": true|false}` in a new
  `InternalEntitlementController` (`api/controller`), `@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")`. Worker tokens
  carry role `CUSTOMER` and a service subject, so this route must **not** require the caller to equal `userId` (unlike
  debit). It returns only the boolean. The Gateway already blocks `/internal/**` from public clients.
- A use case `IsUserPremiumUseCase` (`application/usecase`, `@Transactional(readOnly = true)`) returns
  `subscriptionRepository.findActiveByUserId(userId).map(Subscription::isActive).orElse(false)`.
- Learning request paths keep using `AccessClient.premium()` (bearer). Only background jobs use the new route.

### 1.2 Unlimited examiner requests

- Migration `V13__unlimited_human_grading_feature.sql`: insert plan feature `HUMAN_GRADING_UNLIMITED` enabled for the
  PREMIUM plan (`22222222-2222-2222-2222-222222222222`, table `plan_features`, same columns as the V1 seed), and add
  `credit_spent boolean NOT NULL DEFAULT true` to `human_grading_credit_consumptions`. Never edit V1 to V12.
- `ConsumeHumanGradingCreditUseCase.debit(userId)`: find the active subscription (none → keep throwing
  `InsufficientCreditsException`, still `402 HUMAN_GRADING_CREDIT_REQUIRED`). If its plan has the enabled feature
  `HUMAN_GRADING_UNLIMITED`, do **not** call `consumeHumanGradingCredit()`; return it as "unlimited". Read the plan
  features through `PlanRepository` the same way `GetUserEntitlementUseCase` does.
- Idempotency stays by submission id: `consumeOnce` stores the consumption with `credit_spent = false` for the unlimited
  case. Update `HumanGradingCreditConsumption` (add `creditSpent`), the repository interface and
  `JdbcHumanGradingCreditConsumptionRepository` (insert and read the column).
- `refund`: when the stored consumption has `creditSpent = false`, do not call `refundHumanGradingCredit()`; mark it
  refunded and return the current remaining credits. This prevents a refund giving back a credit that was never spent.
- `markReconsumed` (a refunded consumption asked again) must also set `credit_spent` for the new attempt.
- Response `HumanGradingCreditResponse` keeps `charged` meaning "a credit was spent" (false for unlimited). Learning and
  Assessment only read it; check `RestHumanReviewClient` and `AccessHumanGradingCreditClient.consume` still behave.
- Events `HumanGradingCreditConsumed/Refunded` are still written to the outbox; keep `remainingCredits` as is.
- Do not change the `PREMIUM_30D`/`PREMIUM_90D` credit numbers (they become informational).

### 1.3 Tests and docs

- Unit tests for `ConsumeHumanGradingCreditUseCase`: unlimited plan does not spend a credit, repeat call is idempotent,
  refund of an unlimited consumption leaves `humanGradingCreditsUsed` untouched, no subscription → insufficient, plan
  without the feature keeps the old credit counting.
- `IsUserPremiumUseCase` test and a controller test (`@WebMvcTest`) for the new route (auth, boolean body).
- Testcontainers test for the migration and the repository column, following the existing access persistence tests.
- Contracts: `docs/contracts/access-points-v1.md` (new internal route, section on human-grading credits),
  `docs/contracts/human-review-v1.md` (Premium means unlimited requests; `402` only without an active Premium
  subscription).

## 5. Phase 2 — Learning: Premium benefits in grading, no course unlock

### 2.1 Client

- `AccessClient`: add `boolean premiumOf(UUID userId)`. `RestAccessClient` implements it with
  `GET /internal/access/users/{userId}/premium` using `LearningWorkerTokenProvider.token()`, `CallKind.READ` through the
  existing `ResilientCall`, outside any transaction or lock. On any failure the caller treats the learner as **not**
  Premium for limits (a failed lookup never grants the bigger allowance).
- Document the new HTTP edge in `docs/system-architecture.md` (Learning → Access already exists; add the route).

### 2.2 Writing (lesson, Practice, free writing): no points for Premium

- Add `learning.writing.premium-daily-grading-limit` (default 20, `@Min(1)`) to `WritingSettings`. The record has
  secondary constructors and `defaults()`; add the new component to all of them and fix test fixtures that build it.
- `EssaySubmissionFlow`: add `boolean premiumLearner()` that calls `AccessClient.premium()` and returns `false` on
  `AccessUnavailableException`. Change `requireBalance()` to skip the check when the learner is Premium, `pointCost()`
  to return `0` for Premium, `grade()` to pick `premiumDailyGradingLimit` or `dailyGradingLimit`, and `charge()` to skip
  the debit when `submission.pointCost() == 0` (call `finish` with no ledger entry). Each use case reads
  `premiumLearner()` once at the top of its request and passes it down; do not store per-request state in the singleton.
- `WritingSubmission.markGraded`: accept a null ledger id only when `pointCost() == 0`; keep requiring it otherwise.
  Update the three use cases' `finish` callbacks and `WritingSubmissionViewAssembler` so a null ledger entry is valid.
- Migration `V59__premium_free_grading.sql` (learning): drop and recreate the V2 check on
  `lesson_writing_submissions` so `GRADED` needs `debit_ledger_entry_id IS NOT NULL OR point_cost = 0`.
- A `PAYMENT_PENDING` replay of a Premium submission must not stay stuck: with cost 0 it goes straight to `GRADED`.

### 2.3 Speaking: no points for Premium, 10 a day

- Add `learning.speaking.premium-daily-grading-limit` (default 10, `@Min(1)`) to `SpeakingPracticeSettings`. This record
  has a single constructor on purpose; keep it that way and update the test fixture.
- `CreateSpeakingSubmissionUseCase`: look up `access.premium()` once (false when Access is unavailable); skip
  `requireBalance()` for Premium and store `pointCost = 0` in `SpeakingSubmission.start(...)`.
- `SettleSpeakingSubmissionUseCase`: when `submission.pointCost() == 0`, skip `points.debit` and call
  `current.graded(null, now)`. `SpeakingSubmission.graded` accepts a null ledger id only when `pointCost() == 0`.
- `SpeakingGradingJob.quota(userId)`: choose the limit with `AccessClient.premiumOf(userId)` (false on failure). One
  lookup per claimed submission, never inside a transaction or lock.
- Same migration `V59`: relax `chk_speaking_submissions_cost` to `point_cost >= 0` and the `GRADED` check
  (`chk_speaking_submission_grade`) to accept a null ledger id when `point_cost = 0`.
- Speaking stays off by default (`learning.speaking.enabled`); do not change that.

### 2.4 Remove the course-unlock benefit

- `PaywallGate.Request.lessonOpen(...)` and `knowledgePointOpen(...)`: stop passing `premium()` to
  `PaywallPolicy.isOpen`; pass `false`. Keep `grandfathered` (a lesson already started stays open), the free-course
  rule, the free preview and purchased skills. Keep `premiumActive()` for `StartFreePracticeUseCase`.
- Keep the `premium` flag in `CourseResponse` and `ListCoursesUseCase` (it describes the learner's plan, used by
  clients); only the access rule changes.
- `PaywallPolicy`: remove or ignore the `premium` parameter consistently; update its callers and tests.

### 2.5 Tests

- `PaywallGateTest`, `PaywallPolicyTest`, `ListCoursesUnlockFieldsTest`: Premium without a purchase no longer opens a
  paid lesson; a started lesson stays open; free course and free preview unchanged; purchase unchanged.
- `EssaySubmissionFlow`: Premium writes without balance check or debit and reaches `GRADED`; normal learner is checked
  and charged exactly as before; Premium quota 20; Access unavailable → not Premium and the existing
  `503 PAYMENT_UNAVAILABLE` for the points path.
- Speaking: create and settle for Premium (no debit, cost 0, graded without ledger id), normal learner unchanged,
  job limit 5 versus 10, lookup failure → 5.
- Testcontainers: the V59 constraints accept a Premium row and still reject a paid row without a ledger entry
  (extend the existing writing and speaking persistence tests or add one).

## 6. Phase 3 — Assessment: 40 essays a day for Premium

- Add `assessment.llm-grading.premium-daily-limit` (default 40) beside `daily-limit` in
  `infra/config-server/config-repo/assessment-service.yaml` and inject it into `GradeGateEssayJobUseCase` (adjust its
  constructors).
- New port `application/port/PremiumLookup` (`boolean isPremium(UUID userId)`) and adapter
  `infrastructure/client/AccessPremiumClient` calling `GET /internal/access/users/{userId}/premium` with
  `AssessmentServiceTokenProvider.authorizationHeader()`, `ResilientCall.call("access", CallKind.READ, ...)`, base URL
  from `ACCESS_SERVICE_URL` (already used by `AccessHumanGradingCreditClient`). Any failure returns `false`.
- `gradeOne`: do the lookup before the quota call, outside any transaction or lock, then
  `quota.tryConsume(userId, day, premium ? premiumDailyLimit : dailyLimit)`. A failed lookup uses the normal limit.
- Tests: job unit tests for limit 20 and 40 and for a failed lookup (mock `PremiumLookup`, no real HTTP); a client test
  with `MockRestServiceServer` for the route and the failure case.
- Docs: add the edge to the synchronous HTTP table in `docs/system-architecture.md`; check `ServiceDependencyGraphTest`
  (`infra/api-gateway`): Access must not call Assessment.

## 7. Phase 4 — Config, docs, verification

- `infra/config-server/config-repo`: the three new keys with the defaults above; no secrets.
- `docs/contracts/`: update `access-points-v1.md`, `human-review-v1.md`; add a short section to the Writing and
  Speaking contracts (`lesson-writing-v1.md`, `speaking-practice-grading-v1.md`) saying Premium grades with
  `pointCost 0` and a larger daily limit. Update `AGENTS.md` §3.8 (the "Tác vụ LLM mới cho học viên" and paywall text)
  only where it states Premium opens lessons.
- Verification, narrowest first, then the whole services:
  - `mvn -q -B -ntp -pl services/access-service -am test`
  - `mvn -q -B -ntp -pl services/learning-service -am test`
  - `mvn -q -B -ntp -pl services/assessment-service -am test`
  - `mvn -q -B -ntp -pl infra/api-gateway test` (dependency graph test)
  - `ArchitectureTest` of each touched service with `-Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`.
  Do not update `archunit_store`. `content-service` currently has a known `ArchitectureTest` failure from another
  branch; report it, do not fix it here.
- Report test results as they really are; do not claim a test passed that was skipped (Testcontainers needs Docker).

## 8. Patterns applied (AGENTS.md §3.9)

- New HTTP calls go through `ResilientCall` as `READ`, outside transactions and locks; failure degrades to "not Premium".
- Human-grading consume and refund stay idempotent by submission id; no new money or points are created or granted.
- No new event, no new scheduler; no change to `ResilientCall`, outbox or retry helpers.
- Error messages added or changed are Vietnamese; migrations are new files only; no secrets in config or logs; no essay
  text or prompt in logs.

## 9. Acceptance

- A normal learner is checked, graded and charged exactly as before (Writing 3, Speaking 5, same daily limits).
- A Premium learner: Writing and Speaking grade with no balance check and no debit (`pointCost 0`), Speaking allows 10
  a day, test essays allow 40 a day, examiner requests are never refused for credit and refunds do not mint credits.
- A Premium learner still pays points to unlock a course skill; lessons already started remain open.
- Access down or the Premium lookup failing: nobody gets the larger allowance; the points path answers `503
  PAYMENT_UNAVAILABLE` as before; a Premium submission is not left in `PAYMENT_PENDING`.
- No new `ArchitectureTest` violations in access, learning, assessment; `ServiceDependencyGraphTest` passes.

## 10. Risks and rollback

- Removing the Premium paywall branch locks unstarted lessons for current Premium learners. Started lessons stay open
  (`grandfathered`). Announce before release.
- Unlimited examiner requests move cost to examiners; their own daily capacity is the only brake. Watch the queue.
- The relaxed database checks only accept `point_cost = 0` rows without a ledger entry; paid rows keep the old rule.
- Rollback: revert the code. The new migrations are additive (feature row, nullable-friendly checks, one defaulted
  column), so older code keeps working with them.
