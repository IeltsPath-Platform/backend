# Account deletion and anonymization design

Status: design proposal. No deletion endpoint or event consumer is implemented by the retention phase.

## Decision summary

Account deletion is an irreversible anonymization workflow. User Service is the authority for the request: it disables the account and revokes credentials before publishing one `AccountDeleted.v1` event through its transactional outbox. Every consumer applies the event idempotently using a durable event ID. The event contains the stable user ID and deletion time, but no email, name, phone, or other personal data.

The stable user ID remains as a pseudonymous link where records must stay consistent, including learning history and the Access point ledger. Account-specific content is deleted where it is private. Community content remains visible with a “Deleted user” author. The deleted identity cannot sign in or be restored.

Self-service deletion initially applies to `CUSTOMER` accounts. Staff and administrator accounts need an operator workflow and last-administrator safeguards; they must not use learner self-service deletion.

## Data inventory and proposed action

The inventory below is based on the service migration schemas. It must be checked against deployed schema versions before implementation.

| Service | Data | Proposed action |
|---|---|---|
| User | `users.email`, `full_name`, `phone_number`, `password_hash`, status | Mark the account deleted/inactive, replace direct identifiers with a non-reversible tombstone value, and prevent authentication before emitting the event. Do not retain the password hash. |
| User | `learner_profiles.display_name`, `avatar_reference`, `bio` | Remove the avatar object, then clear these fields or replace the display name with “Deleted user”. Keep no personal profile text. |
| User | `oauth_identities`, `refresh_tokens`, `account_action_tokens` | Delete provider links and outstanding credentials. The deleted account must fail refresh, login, verification, and password-reset flows. |
| User | `login_history` | Apply the existing 180-day retention independently. Delete the account's remaining history during deletion. |
| User | `auth_audit_events` | Existing separate purge defaults to 365 days in 500-row batches. Keep that policy independent from login history; confirm audit/legal retention and user-ID handling before changing it. This phase does not alter it. |
| User | `examiner_profiles` | For learner self-service, none should exist. Staff deletion requires a separate workflow that disables examiner availability and handles assignments before anonymizing profile data. |
| Assessment | `attempt_responses.response_payload`, `attempt_sections.section_snapshot`, `attempt_items.question_snapshot` and `answer_snapshot` | Delete learner answer payloads and any user-specific snapshot data. Question/section snapshots are generally content copies, not personal data, but inspect imported/free-text content before retaining them. Foreign keys currently restrict deleting attempts/items, so prefer clearing/redacting content in place unless a retention-approved row-level cascade is designed. |
| Assessment | `learner_submissions`, `human_review_submissions`, essays and feedback | Delete private answer/essay content, or replace payloads with a deletion marker if foreign-key or grading-history requirements prevent row deletion. Remove linked audio objects. Preserve only explicitly required result aggregates without personal content. |
| Assessment | `video_practice_attempts.response_text`, `audio_reference`, `result_payload`, `reference_text_snapshot` | Delete learner response text and result payload, remove any linked audio object, and retain only a non-identifying score/status if product and legal owners approve. The reference text is a content snapshot and should be retained only if it contains no learner-authored material. |
| Assessment | `placement_speaking_grades` | Keep a minimal result only if the product/legal retention policy requires it; remove feedback that can identify the learner. The account link remains pseudonymous until that policy is settled. |
| Learning | `lesson_writing_submissions`, `speaking_submissions`, `speaking_recordings` | Delete learner-authored text and storage objects. Keep non-identifying outcome aggregates only when needed for product records. |
| Learning | attempts, progress, streaks, goals, badges, rewards, placement state | Retain only if required for aggregate reporting or financial/reward integrity; detach direct identifiers and preserve the stable pseudonymous user key. Remove free-text fields that can contain personal data. |
| Library | `notes`, `saved_video_segments`, `flashcards`, `flashcard_decks`, vocabulary study submissions | Delete user-created text and cards, including deck relationships. Retain only non-content aggregate counters if needed. |
| Community | `posts`, `comments`, reactions, follows and bookmarks | Preserve posts/comments only after removing authored text on request if it contains personal data; detach author IDs and display them as “Deleted user”. Delete reactions, follows, bookmarks, and device-like activity. A schema change is needed because author IDs are currently non-null. |
| Access | wallets, point ledger, subscriptions, key and purchase history | Keep the ledger and subscription/key history to preserve DR-41 accounting and support dispute handling. Remove no ledger rows; retain only the pseudonymous user ID and never copy profile PII into the ledger. |
| Notification | `user_contacts`, mail preferences, examiner digests, push devices | Delete email/name contact snapshots, preferences, and device tokens. `mail_deliveries` stores event IDs and sent times only, so it has no direct email to erase. Review event payload retention in notification inbox/outbox tables. |
| Object storage | profile images, Speaking recordings and any user uploads | Delete by owner-scoped object key and record idempotent completion. A failed storage deletion must be retried and surfaced as pending work; it must not make the account sign-in capable again. |

## Event and processing flow

1. User Service authenticates the owner, locks the account row, verifies that deletion is allowed, and marks it `DELETION_PENDING` or the existing non-authenticating status.
2. In the same database transaction, revoke refresh tokens, invalidate account-action tokens, remove OAuth links, anonymize profile fields, and insert `AccountDeleted.v1` into the outbox. The payload is `{eventId, userId, deletedAt, schemaVersion}`. No identifying values are copied into the event.
3. The outbox relay publishes the event after commit. A retry republishes the same event ID.
4. Assessment, Learning, Library, Community, Access, and Notification consumers each write the event ID to an inbox table in the same transaction as their local changes. A unique event-ID constraint makes repeated deliveries harmless.
5. Each service deletes its private content and object-storage files, or anonymizes required history as listed above. Consumers do not call User Service while holding their database transaction.
6. A deletion status endpoint reports `pending`, `completed`, or `needs_attention` without exposing deleted profile values. Failed storage or consumer work is retried from durable local state.

## User-visible and security behavior

- On request, immediately revoke sessions and block new login, refresh, OAuth login, password reset, and email verification for the account.
- Return a deletion request receipt and an estimated completion state. Once asynchronous work finishes, the former owner cannot retrieve records or recreate the account with the same email until the identifier reuse policy permits it.
- Show “Deleted user” for retained community contributions. Delete private notes, flashcards, recordings, and authored learning responses.
- Anonymization is not reversible. Support staff cannot restore the former identity from a tombstone.
- Keep only stable, access-controlled pseudonymous IDs where ledger, aggregate, moderation, or audit integrity requires them. Document each retention purpose and time limit before implementation.

## Open decisions before implementation

1. Confirm legal retention periods and whether `auth_audit_events` can retain a user ID or must be detached. The sign-in-history scheduler must never delete this table by implication.
2. Decide whether assessment bands and placement outcomes are retained, and for how long, after deleting essays and feedback.
3. Decide whether community posts/comments are deleted on request or retained after author detachment; the proposal above preserves them only after removing personal content.
4. Confirm notification inbox and outbox payload retention, because event payloads can contain display names even though `mail_deliveries` does not contain the email address.
5. Confirm policy for duplicate email reuse after tombstoning and for accounts with staff roles.

## Rollback and operational notes

The workflow is irreversible after a service deletes content or storage objects. Before enabling it, deploy consumers and inbox tables, test replay and partial failure, measure backlog, and retain the request status for support. Rollback may pause event publication or consumers, but it cannot restore erased personal data. Do not run a destructive production migration before legal, product, and operations owners approve the open decisions.
