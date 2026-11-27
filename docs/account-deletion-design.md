# Account deletion and anonymization

Status: implemented. Self-service deletion is `DELETE /api/users/me` (user-service); the event is `user.deleted.v1`
(`docs/contracts/user-deleted-v1.md`). Last checked against the code on 2026-11-27.

## Decision summary

Account deletion is irreversible and happens at once, with no grace period. User Service is the authority: it checks the
password (or a fresh Google ID token for a Google-only account), marks the account `DELETED`, anonymises it, revokes every
credential, and writes one `user.deleted.v1` event to its outbox in the same transaction. The event carries only the
event ID, the user ID and the time.

Each consumer records the user in a `deleted_users` tombstone and a `user_deletion_jobs` row in one transaction, then a
`UserDeletionJobWorker` purges the data in batches. Jobs are claimed with a lease under `FOR UPDATE SKIP LOCKED`, tried up
to 10 times, then marked `FAILED` and logged. A job that crashed on its last attempt is marked `FAILED` once its lease
expires. Storage objects are deleted outside any transaction, and a row is removed only after its object is gone. Storage
also answers 404 for a wrong bucket or key, so a 404 counts as "gone" only when an existence check, which first
confirms the bucket is reachable, finds no object.

Only `CUSTOMER` accounts may delete themselves. Any staff role gets `STAFF_ACCOUNT_SELF_DELETION`; an administrator
closes staff accounts so that grading queues, sales keys and content can be handed over first. The admin delete
endpoint only deactivates an account; it does not erase data. When a staff member asks for their data to be erased:

1. An administrator hands over their open work (grading queue, sales keys, drafts).
2. The administrator sets their roles to `CUSTOMER` only (`PUT /api/users/{id}/roles`).
3. The staff member deletes their account from the app, with their password or Google account, like any learner.

Grading and QA they did as an examiner is kept without their name, as described below.

A `DELETED` account cannot change status, be edited, get roles, or be set back to another status, and its learner
profile and avatar cannot be changed by the user or an administrator (`ACCOUNT_DELETED`, HTTP 409). Nobody can set
`DELETED` through the status API (`DELETED_STATUS_RESERVED`); the admin delete endpoint leaves an already deleted
account as it is. Every write to an account takes the account lock before its first read, the same lock deletion
takes, so a request running at the same time as a deletion sees the deleted account instead of saving an older copy
over it. An avatar uploaded while the account is being deleted is queued for removal instead of being attached.

The same email or Google account can register again, as a new account with a new user ID. No sign-up bonus exists, so
this cannot be used to farm points.

## What happens to each kind of data

| Data | Action |
|---|---|
| Name, email, phone, password, Google link, profile text | Cleared or replaced at once in user-service (`deleted+<id>@deleted.local`, "Người dùng đã xóa"). |
| Avatar | Queued in `pending_avatar_deletions` in the same transaction; `AvatarDeletionJob` deletes the object and never gives up: a failure waits twice as long as the last (up to 6 hours) and is logged as an error from the 10th attempt. A 404 counts as deleted only when an existence check confirms the bucket is reachable, so a wrong bucket or key is not mistaken for success. |
| Examiner profile (band, certificates) | Cleared; `accepting_submissions = false`. |
| Notes, flashcards, decks, notifications, devices, mail contacts and preferences | Deleted, including storage objects. A phone that was signed in to another account before keeps that account's push history pointing at the device; those deliveries are deleted with the device. |
| Learning: essays, Speaking recordings, answers, practice, reviews, diagnostics, progress, XP, hearts, streaks, badges, leagues, flags, settings, LLM usage, work items about the learner, placement Speaking results in `placement_speaking_outbox` | Deleted. Every table with a `user_id` (or `subject_user_id`) column is covered, and every event table whose JSON `payload` names the learner; a test fails if a new one is not. |
| Assessment: attempts, answers, results, essays, Speaking recordings, grading jobs and reviews of the learner's work, comparisons, QA rows, gold-set copies of the learner's essays and their eval items | Deleted. |
| Assessment work the user did as an examiner | Finished reviews keep their grade without a grader; unfinished ones go back to the queue; QA rows keep a placeholder ID. |
| Game | Player rows, guesses, hints, receipts, sessions removed; a room they hosted keeps a placeholder host. |
| Community posts and comments | Kept with the author shown as "Người dùng đã xóa". Bookmarks, follows, reactions and blocks are deleted. Reports they filed stay with the reporter anonymised. |
| Point ledger, key activations, subscriptions | Kept for reconciliation. The active subscription is cancelled; the remaining balance is written off through one ledger entry (`ACCOUNT_DELETION`), so wallet and ledger agree. |
| Outbox events about the user | Deleted, published or not, so no mail or result is sent for a deleted account: user-service mail events (email, name, reset and verification tokens), access expiry notices, assessment results and Mentor reviews of the learner, library review reminders, community notifications addressed to them, Learning placement Speaking results. The `user.deleted.v1` event itself is kept until it is published. |
| Security logs | `login_history` deleted at once; `auth_audit_events` purged after 365 days by its own job. |
| `users` row | Kept as an anonymous tombstone with status `DELETED`. |

Data kept for a legitimate reason (ledger, audit log, anonymous community content) must be listed, with its reason and
time limit, in the privacy policy, as Google Play requires. The policy must say plainly that community posts and
comments stay after deletion with the author removed, and that their text may still contain personal details the
user wrote into them.

## Decisions confirmed (2026-11-27)

1. Only learners delete their own account. Staff (examiner, sales, content author, admin) ask an administrator, who
   hands over open work, sets their role to `CUSTOMER`, and the person then deletes the account themselves.
2. Gold-set copies of a learner's essays and their evaluation items are deleted; the smaller evaluation set is accepted.
3. Community posts and comments are kept and anonymised so discussions stay intact; the privacy policy says so.
4. The first version (#230) was never deployed, so no `FAILED` job needs to be reset.

## Events that arrive after the deletion

Every service that consumes other events checks the tombstone, so a late event cannot recreate the user's data: Learning
(assessment results, human review results, placement Speaking), Assessment (placement Speaking), Access (point grants,
email verification) and Notification (inbox rows, mail contacts, mail preferences, examiner digests). Community, Content,
Game and Library have no other consumer that writes per-user data; requests by the deleted user stop at the Gateway
because the access token is revoked at once.

## Operating it

- A job marked `FAILED` needs an operator: fix the cause, then reset it on that service's database:
  `UPDATE user_deletion_jobs SET status = 'PENDING', attempts = 0, claimed_at = NULL, completed_at = NULL, last_error = NULL WHERE status = 'FAILED';`
- Deploy consumers before user-service, so their queues exist before the first event.
- Deletion cannot be rolled back once a service has removed data or objects.

## Open decisions

1. Whether a deletion status endpoint (`pending` / `completed` / `needs_attention`) is still wanted; it is not built.
