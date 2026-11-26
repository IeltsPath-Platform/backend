# Codex prompts for the data integrity phases

Paste one prompt per Codex session. Each phase has its own worktree and branch, already created from `main` (830023b4).

| Phase | Worktree | Branch | Start |
| --- | --- | --- | --- |
| 1 | `C:\Users\Admin\wt\backend-one-active-subscription` | `fix/one-active-subscription` | wave 1 |
| 2 | `C:\Users\Admin\wt\backend-email-lower-unique` | `fix/email-lower-unique` | wave 1 |
| 3 | `C:\Users\Admin\wt\backend-wallet-reconciliation` | `feat/wallet-ledger-reconciliation` | wave 1 |
| 4 | `C:\Users\Admin\wt\backend-lock-published` | `fix/lock-published-versions` | wave 1 |
| 6 | `C:\Users\Admin\wt\backend-retention` | `feat/retention-purge` | wave 1 |
| 5 | `C:\Users\Admin\wt\backend-status-transitions` | `refactor/status-transitions` | wave 2, after phase 1 is merged: run `git merge origin/main` in its worktree first |

Wave 1 phases touch different services and files, so they can run at the same time. Phase 5 changes the enums that phase 1
also touches, so it starts after phase 1 is on `main`. Plan files are in
`C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\`.

## Common rules (already included in each prompt below)

```
Working rules
- Work only in the worktree named above, on its branch. Do not touch other worktrees or the main checkout.
- Read the plan file first: it lists context, files, steps and tests. The code is the source of truth: if the plan and the code disagree, trust the code, say so in your report and adjust.
- Smallest change that meets the requirements. Follow existing patterns, naming and error handling. Error messages shown to users stay in Vietnamese with diacritics.
- Take the next free Flyway version in the service (list db/migration first). Never edit an existing migration.
- Do not put plan names, phase numbers or audit labels in code, comments, migration names, test names or commit messages.
- Run the narrow tests first, then the service module tests: mvn -o -q -pl <module> -am test. Testcontainers tests need Docker; if Docker is off, say so and list which tests you could not run. Do not hide or weaken failing tests.
- Commit locally with a conventional message (feat/fix/refactor). Do NOT push and do NOT open a PR.
- Finish with: what changed, files, tests run and their result, anything you could not verify, and open questions.
```

## Phase 1 - one Active subscription (access-service)

```
Worktree: C:\Users\Admin\wt\backend-one-active-subscription   Branch: fix/one-active-subscription
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-01-one-active-subscription.md
(and plan.md in the same folder for the background)

Task: make the database guarantee at most one ACTIVE subscription per user, and make lapsed subscriptions really EXPIRED.
Facts already checked: ActivateKeyUseCase and GrantSubscriptionUseCase both look up findActiveByUserId then extend or create. findActiveByUserId hides rows with ends_at in the past, but nothing ever sets them to EXPIRED, so a plain unique index would block the next purchase after a lapse.

Do:
1. Migration: set lapsed ACTIVE rows to EXPIRED; for users with several ACTIVE rows keep the latest ends_at and CANCEL the others; then CREATE UNIQUE INDEX on subscriptions (user_id) WHERE status = 'ACTIVE'.
2. Add expireLapsed(userId, now) and expireAllLapsed(now) to the repository port and adapter; add Subscription.expire().
3. Call expireLapsed first, in the same transaction, in ActivateKeyUseCase and GrantSubscriptionUseCase. On a unique violation, reload the active row and extend it once.
4. Add SubscriptionLapseScheduler (same pattern as SubscriptionExpiryNoticeScheduler, property access.subscription-lapse.scan-interval, default PT15M).
Tests: lapsed subscription then activation creates a new Active row and expires the old; activation on an Active row extends it; two parallel activations leave one ACTIVE row (Testcontainers); migration test with two ACTIVE rows and one lapsed row; scheduler test.
Put the pre-check queries from the plan in your final report with the commands to run them, but do not run them on any real database.
Common working rules: see the block above (no push, no PR, local commit).
```

## Phase 2 - e-mail unique ignoring letter case (user-service)

```
Worktree: C:\Users\Admin\wt\backend-email-lower-unique   Branch: fix/email-lower-unique
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-02-email-case-insensitive.md

Task: add a case-insensitive unique index on users e-mail and confirm every write path normalises.
Facts already checked: the Email value object trims and lower-cases, and registration, login, forgot password, verification and admin create use it.

Do:
1. Read GoogleSignInUseCase, UserManagementController and every repository method that reads or writes email; list any path that does not build Email and fix it.
2. Migration: lower-case existing e-mails, then CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email)). The migration must fail with a clear message if two rows differ only by case, and must not merge accounts. Keep the old constraint.
3. Check the migrations and seed data for mixed-case e-mails (admin and demo users) and report them.
Tests: registering A@x.com after a@x.com is refused with the existing duplicate message; migration lower-cases a mixed-case row; migration fails clearly on a case-only duplicate; Google sign-in finds an existing lower-case account.
Common working rules: see the block above (no push, no PR, local commit).
```

## Phase 3 - wallet and ledger reconciliation (access-service)

```
Worktree: C:\Users\Admin\wt\backend-wallet-reconciliation   Branch: feat/wallet-ledger-reconciliation
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-03-wallet-ledger-reconciliation.md

Task: a scheduled, read-only check that compares each point wallet with its ledger. It only reports; it never changes a balance.

Do:
1. ReconcileWalletsUseCase with a port WalletReconciliationStore (JDBC). Find wallets where balance <> sum(delta), or balance <> the latest balance_after, or total_credited - total_debited <> balance. Limit rows (default 100).
2. Log one WARN per mismatch (user id and numbers only) and increment the Micrometer counter points.reconciliation.mismatch.
3. WalletReconciliationScheduler following the SubscriptionExpiryNoticeScheduler pattern. Properties access.points.reconciliation.enabled (default false), .interval (default PT24H), .max-rows. Add them to the access-service config in infra/config-server/config-repo and document the metric in docs/system-architecture.md.
Tests: consistent wallet reports nothing; a wallet 5 above its ledger is reported and unchanged; disabled by default does nothing; the SQL on real tables (Testcontainers).
Common working rules: see the block above (no push, no PR, local commit).
```

## Phase 4 - lock published content (content-service)

```
Worktree: C:\Users\Admin\wt\backend-lock-published   Branch: fix/lock-published-versions
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-04-published-content-lock.md

Task: the database must refuse changes to the content of a PUBLISHED question version or package version.
Do the audit FIRST and write it into your report: every save, saveAll and native UPDATE that touches question_versions or content_package_versions (repositories, import, AI draft approval, scheduled publish, seed code). If a path edits a PUBLISHED row, that is a bug: fix it so it creates a new version, and tell me.

Then:
1. Migration with a BEFORE UPDATE trigger function on both tables: if OLD.status = 'PUBLISHED' and any protected column changed (question_versions: stem, options, answer_spec, schema_version, explanation, difficulty, hint, guide, guide_steps; content_package_versions: rules, schema_version, version_number), raise an exception with a stable message. Allow status, published_at and updated_at to change (PUBLISHED to ARCHIVED must work). No delete trigger.
2. Map the exception to the existing conflict response (Vietnamese message) in GlobalExceptionHandler.
Tests: editing a published version is refused on both tables; DRAFT to PUBLISHED and PUBLISHED to ARCHIVED work; ScheduledPublishJob, course import and AI draft approval still pass. Run the whole content-service suite. PracticeSetMaterializerIntegrationTest already fails on main (missing bean CodeSettingsUseCases): compare against that baseline and say so.
Common working rules: see the block above (no push, no PR, local commit).
```

## Phase 6 - retention and account deletion design (user-service, assessment-service, docs)

```
Worktree: C:\Users\Admin\wt\backend-retention   Branch: feat/retention-purge
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-06-retention-and-deletion.md

Task, three parts:
6a (assessment-service): prove that placement Speaking recordings are covered by the 90-day purge (PurgeExpiredSpeakingRecordingsUseCase, SpeakingRecordingRetentionScheduler; recordings are learner_submissions rows with skill = 'SPEAKING'). Add a test: a placement attempt's recording older than 90 days is deleted from storage and marked recording_deleted_at, and the placement result keeps its band and feedback. Change production code only if the test shows a gap.
6b (user-service): delete login_history rows older than 180 days. LoginHistoryRetentionUseCase, port deleteOlderThan(cutoff, limit), scheduler, properties user.login-history.retention-days=180, .batch-size=1000, .interval=PT24H, and .dry-run (default true: only log the count). Delete in batches. Do not touch auth_audit_events.
6c (docs only): write docs/account-deletion-design.md: for every service the personal data tables and fields, what happens on deletion (remove, anonymise, keep), one AccountDeleted event through the outbox with idempotent handlers, how sign-in is blocked, and that anonymisation is not reversible. Start from the inventory in the plan and verify it against the migrations. No code for 6c.
Tests: rows older than 180 days deleted, newer kept, batch limit respected, dry run deletes nothing (Testcontainers).
Common working rules: see the block above (no push, no PR, local commit).
```

## Phase 5 - status transitions (access-service, assessment-service) - start after phase 1 is merged

```
Worktree: C:\Users\Admin\wt\backend-status-transitions   Branch: refactor/status-transitions
First run: git merge origin/main (phase 1 must be in it; Subscription.expire() should exist).
Plan: C:\Users\Admin\wt\backend-plans\plans\261126-1135-data-integrity-gaps\phase-05-status-transitions.md

Task: give each status enum canMoveTo(next) and make the domain methods use it, so a final state can never be left (for example EXPIRED back to ACTIVE).
Enums: access SubscriptionStatus and KeyStatus; assessment GradingJobStatus, the human review status and SubmissionStatus.
Do first: for each enum list every place that assigns the status (setStatus, status =, SQL UPDATE ... SET status) and every move the code really makes, including retries and re-assignment. Compare with the allowed moves in the plan. If the code makes a move the plan forbids, do not guess: list it in your report and keep that move allowed until I decide.
Then: add canMoveTo with the allowed moves in the enum, use it in the aggregates and throw the existing invalid-state exception; JDBC updates that bypass an aggregate get a WHERE status IN (allowed from) guard. No schema change.
Tests: a table-driven test per enum covering every pair of statuses; one test per refused move showing the invalid-state error; existing tests still pass.
Common working rules: see the block above (no push, no PR, local commit).
```

