# Phase 2 - e-mail unique ignoring letter case (user-service)

Rule: SRS DR-40. Size S. Status: proposed.

## Context
- `users.email` has a plain `UNIQUE`, which is case sensitive in PostgreSQL.
- The `Email` value object (`user/domain/vo/Email.java`) trims and lower-cases, and registration, login, forgot
  password, verification and admin create all build it, so the application already stores lower-case.
- Risks left: rows written by migrations or seed scripts, manual SQL, and Google sign-in or any path that does not
  build `Email`.

## Requirements
1. The database refuses two users whose e-mail differs only by letter case.
2. Existing data is checked first; no automatic merge of accounts.

## Files
- New migration `services/user-service/src/main/resources/db/migration/V20__users_email_unique_lower.sql` (next free version).
- Review `GoogleSignInUseCase`, `UserManagementController` and any repository method that writes or reads `email`
  (`UserRepository.findByEmail`, `existsByEmail`) to confirm they use the normalised value.
- Optionally make `UserRepository.findByEmail` compare `lower(email)` so old mixed-case rows are still found.

## Steps
1. Pre-check on production data:
   `SELECT lower(email), count(*) FROM users GROUP BY 1 HAVING count(*) > 1;`
   and `SELECT count(*) FROM users WHERE email <> lower(email);`.
   If the first returns rows, stop and report; the owner decides which account stays.
2. Migration: `UPDATE users SET email = lower(email) WHERE email <> lower(email);` (only if the first check is empty),
   then `CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));`. Keep the old constraint.
3. Grep every insert and update of `email` in the service, and fix any that skips `Email`.

## Tests
- Registering `A@x.com` after `a@x.com` is refused with the existing duplicate-e-mail message.
- Migration test: a mixed-case row is lower-cased; two rows that differ by case make the migration fail with a clear message.
- Google sign-in with `Name@Gmail.com` finds the account created as `name@gmail.com`.

## Risks and rollback
- Mixed-case rows from seeds (check the admin and demo users in migrations) would be changed; check logins still work.
- Rollback: `DROP INDEX uq_users_email_lower;`.
