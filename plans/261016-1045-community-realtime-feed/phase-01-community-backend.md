# Community backend

Context: [plan](plan.md), [architecture](../../docs/system-architecture.md).

- [x] Add V2 preserving legacy categories and archiving merged reactions; media, follow, bookmark indexes.
- [x] Implement validated media/target band, one reaction per user, author/admin ownership, two-level replies.
- [x] Add keyset feed and paginated profile/follow/bookmark APIs with batched enrichment; preserve legacy APIs.
- [x] Add public and internal batch profiles without private fields or N+1.
- [x] Write events transactionally, suppress self notifications, relay with confirms/retries.
- [x] Verify unit/security/PostgreSQL tests and reactor compile.

Files: `services/community-service/**`, `services/user-service/**`, Community runtime config, Compose wiring, event/API contracts.
Validation: `mvn -q -pl services/community-service,services/user-service -am test`; `mvn -q compile -DskipTests`.
Risk: migration merges legacy reactions; archive first and use a deterministic winner. Never modify V1 or run destructive rollback; revert application changes and use a forward migration if needed.
