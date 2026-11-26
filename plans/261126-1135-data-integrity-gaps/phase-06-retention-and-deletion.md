# Phase 6 - retention and account deletion

Rules: SRS DR-44 to DR-47. Size S for 6a and 6b; 6c is a design deliverable, implemented by a later plan.

## Decisions
- Speaking recordings, placement included: 90 days.
- Essay text: kept until the account is deleted.
- Sign-in history: 180 days.
- Account deletion anonymises: remove name, e-mail, phone, avatar, bio and recording files; keep learning records without a person attached; keep the point ledger consistent.

## 6a - placement recordings fall under the 90-day purge (assessment-service)
Context: placement Speaking recordings are rows of `learner_submissions` with `skill = 'SPEAKING'`; the purge
(`PurgeExpiredSpeakingRecordingsUseCase`, `SpeakingRecordingRetentionScheduler`, `SpeakingRecordingProperties.retentionDays = 90`)
selects `skill='SPEAKING' AND submitted_at < cutoff AND recording_deleted_at IS NULL`, so they are already covered.
Steps: add a test that a placement attempt's recording older than 90 days is deleted from storage and marked
`recording_deleted_at`, and that the placement result keeps its band and feedback (those are stored separately in
`placement_speaking_grades`). Confirm the grading job in learning has finished with the file long before day 90 (it does: timeout is 10 minutes).
No production code change expected; if the test fails, fix the query.

## 6b - sign-in history older than 180 days (user-service)
Context: table `login_history` (user, success, reason, address, device, time), written by `AuditedLoginUseCase`.
Files: `LoginHistoryRetentionUseCase`, a port method `deleteOlderThan(cutoff, limit)`, scheduler
`LoginHistoryRetentionScheduler`, properties `user.login-history.retention-days=180`, `.batch-size=1000`, `.interval=PT24H`.
Steps: delete in batches so the table is never locked for long; log only the count. Open question: whether
`auth_audit_events` follows the same limit (ask the owner; leave it alone until answered).
Tests: rows older than 180 days are deleted, newer ones kept, batches respect the limit (Testcontainers).

## 6c - account anonymisation (design only in this plan)
Deliverable: a document in `docs/` that lists, service by service, the personal data and what happens on deletion, and
the event flow. Starting inventory to confirm by reading the tables:
- user: `users` (name, e-mail, phone, password hash), `learner_profiles`, `oauth_identities`, `login_history`, `refresh_tokens`, `account_action_tokens`, `auth_audit_events`, `examiner_profiles`.
- assessment: `learner_submissions` (essay text, audio), recordings in storage, `human_review_submissions` (essay text).
- learning: essays in `lesson_writing_submissions`, speaking submissions and recordings; learner id columns elsewhere stay as an anonymous id.
- library: notes, flashcards, decks (user content; decide delete or keep anonymous). community: posts and comments (author shown as a deleted user).
- access: wallet and ledger stay (ledger consistency, DR-41); subscriptions stay; activation key history stays.
- notification: mail delivery records that hold an address, if any.
Design questions to answer: one `AccountDeleted.v1` event through the outbox, per-service handlers that are idempotent,
what the user sees afterwards, how sign-in is blocked, and whether anonymisation is reversible (recommended: no).
Output: the design document and a follow-up plan. No code in this plan.

## Tests and acceptance
- 6a and 6b tests above pass; no change to public contracts.
- SRS rules DR-44 to DR-46 change from Planned to enforced when 6a and 6b merge; DR-47 stays Planned until the follow-up plan ships.

## Risks and rollback
- 6b deletes data: run first with a dry-run flag that only logs the count (default on for the first release).
- Rollback: turn the scheduler off by property; deleted history cannot be restored, which is the intended policy.
