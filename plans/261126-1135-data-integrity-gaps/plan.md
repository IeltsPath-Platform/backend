# Data integrity gaps found while writing SRS section 3.3

Status: proposed. Nothing is implemented. Six phases, each its own small PR. Order: 1, 2, 5, 3, 4, 6.

## Why

Writing SRS section 3.3 (Data Business Rules), each rule was checked against the migrations and the code. Six rules hold
today only because the application behaves, not because the database or the domain enforces them. A race, a new code
path or a manual fix can break them silently. The SRS lists them as "Planned" rules DR-39 to DR-48 and each phase
below turns one or more of them into an enforced rule.

## Phases

| # | Phase | Rule in SRS | Service | Size | File |
| --- | --- | --- | --- | --- | --- |
| 1 | One Active subscription per user (and make lapsed ones Expired) | DR-39, DR-48 | access | M | [phase-01](phase-01-one-active-subscription.md) |
| 2 | E-mail unique ignoring letter case | DR-40 | user | S | [phase-02](phase-02-email-case-insensitive.md) |
| 3 | Wallet balance checked against the ledger | DR-41 | access | S | [phase-03](phase-03-wallet-ledger-reconciliation.md) |
| 4 | Published content locked by the database | DR-42 | content | M | [phase-04](phase-04-published-content-lock.md) |
| 5 | Status changes follow the lifecycle | DR-43 | access, assessment | M | [phase-05](phase-05-status-transitions.md) |
| 6 | Retention and account deletion | DR-44 to DR-47 | user, assessment, design | S + design | [phase-06](phase-06-retention-and-deletion.md) |

## What the code check found (corrects earlier assumptions)

- E-mail: the `Email` value object already trims and lower-cases, and every use case builds it, so the application side
  is safe. Only the database lacks a case-insensitive unique index (phase 2 shrinks to index and data check).
- Subscriptions: both `ActivateKeyUseCase` and `GrantSubscriptionUseCase` already extend an active subscription, so the
  owner's decision "admin grant adds days" is already the behaviour. The gap is a race plus a real problem: **nothing
  ever sets a lapsed subscription to EXPIRED**. `findActiveByUserId` hides it with `ends_at > now`, but the row stays
  `ACTIVE`. A plain unique index would block the next purchase after a lapse, so phase 1 has to expire lapsed rows too.
- Recordings: placement Speaking recordings are stored in `learner_submissions` with `skill = 'SPEAKING'`, and the existing
  purge selects exactly that, so they are already purged after 90 days. Phase 6 only adds a test.

## Decisions (owner, 2026-11-26)

1. An Administrator grant on a user who already has an Active subscription adds days to it, like a key activation.
2. Retention: Speaking recordings (placement included) 90 days; essay text until the account is deleted; sign-in history 180 days.
3. Account deletion anonymises: remove name, e-mail, phone, avatar, bio and recording files; keep learning records without a person attached and keep the point ledger consistent.
4. The wallet reconciliation job only reports; it never changes a balance.

## Dependencies and order

- Phases 1, 2 and 5 touch different services and can run in parallel.
- Phase 5 first adds `canMoveTo`; phase 1 uses it for Subscription, so land phase 5's Subscription part before or with phase 1.
- Phase 3 is independent. Phase 4 needs its audit step done before the trigger.
- Phase 6 design (account anonymisation) is a separate follow-up plan after phases 1 and 2 ship.

## Rules for every phase

- Migration versions at the time of writing: access V14, user V19, content V151, assessment V25. Always take the next free version on `main` that day and ask the owner to check `flyway_schema_history` before merging.
- Run the pre-check query of the phase on a copy of production data before merging a migration that adds a constraint.
- No change to public API contracts. Error messages shown to users stay in Vietnamese.
- Commit messages and tests do not mention plan or phase numbers.
- When a phase merges, change its rule in SRS section 3.3 from "Planned" to enforced.

## Acceptance

- Each phase ships migration or code, tests and a rollback note.
- `mvn` tests of the touched services pass; Testcontainers tests need Docker.
- SRS section 3.3 matches the merged behaviour.

## Unresolved questions

1. Should account anonymisation (phase 6 design) become its own plan right after phases 1 and 2? Recommended: yes.
2. Does `auth_audit_events` count as "sign-in history" for the 180-day rule, or only `login_history`?

