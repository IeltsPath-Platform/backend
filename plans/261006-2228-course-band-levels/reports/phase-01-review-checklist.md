# Content Course Model Review Checklist

Prepared from `phase-01-content-course-model.md` and the approved decisions in `plan.md`. Implementation has not been reviewed; await controller handoff before reading partial edits or running Maven.

## Specification checks

| Requirement | Evidence to inspect | Status |
| --- | --- | --- |
| New additive V19 migration; no old migration rewritten | Migration diff and version inventory | Pending |
| Course columns, exact lengths/defaults/status constraints and timestamps | SQL and JPA mappings | Pending |
| Unique course code and band; half-band range 0–9 enforced in SQL/domain | SQL, Course invariant and tests | Pending |
| Nullable indexed topic course FK | SQL, Topic aggregate and JPA mapping | Pending |
| Course list ordered by band in database | Repository query and adapter | Pending |
| GET courses allows ADMIN, CONTENT_AUTHOR, CUSTOMER, EXAMINER | Controller/security configuration and authorization tests | Pending |
| POST/PUT course authoring only ADMIN and CONTENT_AUTHOR | Controller/security configuration and authorization tests | Pending |
| Create returns 201; duplicate code/band returns 409; invalid bands return 400 | Controller tests and existing exception handler | Pending |
| Update modifies name, bandLevel, status while retaining immutable identity/code | Update command/use case/domain method | Pending |
| Topic create/update carry courseId through request, commands, aggregate, JPA, mapper and response | End-to-end mapping diff | Pending |
| Unknown topic course fails through existing 404/400 convention | Use cases and focused tests | Pending |
| Null course on update retains assignment; create without course remains valid | Update/create tests and implementation | Pending |
| Domain has no Spring/JPA/application/infrastructure imports | Course and Topic imports | Pending |
| Aggregate separate from JPA; MapStruct and repository adapter follow local boundaries | Repository/mapper/use-case dependencies | Pending |
| No gateway/shared/config/Docker changes; no seed yet | Complete scoped diff | Pending |
| Fresh red then green evidence; old tests retain expectations outside allowlist | Saved logs, test diff and Surefire results | Pending |

## Quality and edge cases

- Validate null/blank/oversized names and codes, band boundaries 0 and 9, half bands, 5.25 and 9.5, status input and unknown course IDs.
- Check duplicate checks under concurrent writes: database uniqueness must produce the agreed 409 and rollback the transaction.
- Verify read/write permissions for each canonical role and unauthenticated callers without weakening JWT validation.
- Inspect topic compatibility, constructors, equality and mapper behavior, including omitted/null course updates.
- Confirm bounded course listing uses its approved natural band limit, with database ordering and no repeated repository/client calls inside loops.
- Check error responses follow the existing public body/status contracts and do not expose credentials or private data.
- Verify tests exercise invariants and behavior; mocks may not conceal persistence mapping, FK, migration or uniqueness limitations.

## Verification

Use `mvn -q -pl services/content-service -am test` after handoff and only when no other Maven phase is running. Docker is unavailable in the baseline, so explicitly count and name skipped Testcontainers cases. Do not claim migrations or persistence constraints executed when their tests skip.

No source changes are owned by this reviewer; send findings to the controller and implementation worker. Any unauthorized old-test failure is a stop condition.
