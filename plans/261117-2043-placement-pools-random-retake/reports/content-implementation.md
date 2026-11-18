# Content placement pools implementation

Date: 2026-11-17
Status: verification in progress

## Scope

Implemented phase 1 only in content-service and its two owned contracts. V137 adds the nullable pool column,
backfills all existing placement packages to BAND_5_9, and rejects unknown pools or pools on other package types.
The aggregate pool is final; JPA maps it as non-updatable. Both package creation APIs require a placement pool
exactly for PLACEMENT_TEST. Non-HTTP creation uses the same use-case validation; generated/imported non-placement
packages explicitly pass null. Legacy restored aggregates can still represent a missing pool, which publishing
refuses with HTTP 400 and details.code PLACEMENT_POOL_REQUIRED.

The author list filters by placementPool and carries it per summary. Internal placement reads use dedicated
PlacementPackageResult / PlacementPackageResponse records with pool and optional filtering. The package-version
payload carries placementPool and preserves it through media resolution. Topic/course test DTOs are unchanged.

## Tests-first evidence

Before production edits, added migration/reader IT, both creation API validation/response tests, missing-pool
publication refusal, author list filter IT, and internal pool/filter/version JSON tests. Ran:

```powershell
mvn -q -pl services/content-service -am -Dtest=PlacementPoolMigrationTest,ContentPackageControllerTest,ContentPackageAuthoringErrorTest,PublishContentPackageUseCaseTest,JdbcAuthorPackageReaderTest,InternalLearningContentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: failed during test compilation because PlacementPool did not yet exist. Raw output:
services/content-service/target/placement-pool-red.log. Tests did not reach runtime at this red gate.
Supplemental direct-usecase validation, persistence mapper/media replacement, and author-summary JSON coverage
was added during implementation review.

## Verification

Focused run compiles and executes real Docker/PostgreSQL Testcontainers with zero skips. Completed classes:
- ContentPackageAuthoringErrorTest: 9 tests, 0 failures/errors.
- ContentPackageControllerTest: 9 tests, 0 failures/errors.
- InternalLearningContentControllerTest: 24 tests, 0 failures/errors.
- JdbcAuthorPackageReaderTest: 7 tests, 0 failures/errors.
- PlacementTestSeedTest: 4 tests, 0 failures/errors.

A first implementation compile error (JsonRawValue on an unnecessary compatibility-constructor parameter) was
traced and removed. The focused run also caught the old placement purpose fixture with no pool; it now supplies
BAND_5_9 so it continues to exercise purpose validation. Existing source/binding fixtures likewise now provide
a valid placement pool, while the new missing-pool test deliberately retains null. A rerun and full suite are pending.

Contracts updated: docs/contracts/learning-content-internal-v1.md and docs/contracts/content-package-authoring-v1.md.
No dependency added, no applied migration or ArchUnit store edited, no LLM called, no commit created.
