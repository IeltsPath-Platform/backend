# Content placement pools review

Status: DONE_WITH_CONCERNS

## Scope

Pending changes against `7866f176` in `services/content-service/**`, including the six new Java/SQL/test files, and the two Content contracts. Read the approved plan and phase 1, backend AGENTS.md, CLAUDE.md, README.md, constitution and relevant architecture/contract sections. `docs/code-standards.md` does not exist. Queried the existing Graphify graph for Content controller, reader and mapper nodes before targeted repository searches. The graph predates this diff; implementation claims below are verified against source.

## Stage 1: specification compliance

Implementation requirements pass. Final runnable acceptance remains owned by the controller's in-progress test run; this review does not claim fresh full-suite success.

| Requirement | Evidence / result |
| --- | --- |
| Three pool values and ceilings | `domain/vo/PlacementPool.java`: 3.0, 5.0, 9.0. |
| Required exactly for placement at both create endpoints | `AuthorPackageRequests.CreatePackage` and `CreateContentPackageRequest` use bean validation; both controllers retain `@Valid`. Both usecase entry points call the shared creation guard in `CreateContentPackageUseCase:48`. Controller tests assert missing/inapplicable pool rejection, forwarding and response serialization; direct-usecase tests cover all pools and rejection outside HTTP. |
| Immutable aggregate field and JPA persistence mapping | `ContentPackage:31` final field, creation passes it, no mutation method; entity string enum column is non-updatable; mapper reads and writes it. New `ContentPackagePlacementMappingTest` exercises domain/entity/domain roundtrip for every enum. This is a mapper test, not a database flush/reload test; reviewed entity metadata and migration agree. |
| Legacy seed backfill without old-migration edits | Only new `V137__placement_pool.sql`: all existing placement rows backfilled to BAND_5_9, enum/type CHECK added. Existing migrations and archunit stores absent from changed files. `PlacementTestSeedTest` still resolves original exam and asserts the pool. |
| Publish guard, including scheduling | `PublishContentPackageUseCase:80` rejects null pool with PLACEMENT_POOL_REQUIRED before content validation; normal publish and `validateForPublish` use it. `SchedulePackagePublicationUseCase` delegates validation to that method. Error handler exposes the code in details. |
| Optional pool-filtered internal listing | `JdbcLearningContentReader:751` binds the enum name, joins current version to its package, requires both statuses PUBLISHED, orders by package ID. Dedicated result/response retain original JSON fields and add pool; absent filter still returns all. New PostgreSQL migration/reader test covers all pools, draft/archived packages, and draft version exclusion. |
| Version pool survives media transformation | Reader selects package placement_pool into `PackageVersionContentResult`, `withSections` retains it, internal DTO serializes placementPool. Mapper/media replacement test and controller JSON test cover it. |
| Author list filter and summary | Controller passes typed placementPool into filter; JDBC binds it; selected field reaches summary DTO. Tests cover filter forwarding, unknown enum 400, two pools and null non-placement summary. |
| Other creators / shared listings | Three non-placement command creators explicitly pass null; TopicTestPackageResult/Response and topic/course listing usecases unchanged. |
| Additive wire contracts | Existing response fields preserved; new fields and optional parameters documented in the same diff. Internal Java interface signature intentionally changes with updated local callers. No cross-service model imports or new architecture pattern introduced. |
| Required tests and ArchitectureTest | Fresh results pending from controller; do not infer success from old target reports. Historical red evidence cannot be established solely from the final diff. |

## Stage 2: quality and edge cases

No blocking source defect found in this scope.

Scouted both request paths, constructor/factory chains, mapper and repository save path, version/media projections, scheduled publication, admin query parameter binding, existing placement consumers and legacy seed checks. Pool is immutable, so this change introduces no mutable pool/version snapshot race. New queries remain bulk reads; pool SQL values are bound parameters. Existing content-author authorization remains on both create paths and the admin list. No new secret/PII fields, external calls, schema removal, error swallowing, or archunit baseline changes were introduced.

## Informational finding

Low: `docs/contracts/learning-content-internal-v1.md:397` currently says placementPool is retained when "building a preview", but `GetPackageVersionPreviewUseCase` constructs a separate `PackageVersionPreviewResult` without that field. Phase 1 does not require adding pool to the preview API. Remove the preview claim and keep the verified media-resolution claim. Sent to controller for correction; no source/API expansion recommended.

## Verification and limits

- `git diff --check`: exit 0, no whitespace errors.
- Reviewed current test assertions and source metadata; did not run a competing Maven suite while controller's run was active.
- Full test/build/ArchitectureTest status must be taken from controller's fresh run. Existing placement Surefire reports were stale and were not treated as evidence.
- Type/test coverage percentages and lint count unavailable; repository has no configured coverage gate or general linter.
- Plan files were not edited. No source files were changed by this review.

Summary: Implementation matches the Content phase and no blocking production defect was found. Final acceptance requires the controller's fresh verification results and removal of one inaccurate preview documentation clause.
Concerns/Blockers: No source blocker. Minor documentation correction and fresh suite evidence pending externally.

## Addendum: historical Content seed fixture compatibility

Status: DONE_WITH_CONCERNS

Reviewed the follow-up diff against `7866f176` in `CourseDemoContentSeedTest`, `CourseSeedTest`, `CourseSequenceIntegrationTest`, and `MultiSkillSeedTest` only. This addresses the controller-traced full-suite failure: current `JdbcLearningContentReader.publishedPackageVersion` selects `p.placement_pool`, while these tests intentionally migrate historical snapshots that predate that column.

No blocking defect found in the fix:

- Historical Flyway targets remain 58, 58, 52 and 92 respectively. No seed assertions, test bodies, skips, historical data cleanup, or historical migration resources changed in these four diffs.
- Each setup applies the actual classpath resource `db/migration/V137__placement_pool.sql` via `ResourceDatabasePopulator` immediately after the pinned Flyway migration and before constructing/using the current reader. There is no duplicated pool DDL, reader fallback, mocked result, or swallowed database exception.
- The actual V137 resource only adds the pool column, backfills placement packages and adds its value/type CHECK. It requires only content_packages and package_type, already present at all four pinned targets. It does not advance the fixtures through unrelated later seed migrations.
- All four tests use their own Testcontainers PostgreSQL datasource. The new execution is confined to test setup; it does not alter real runtime configuration or migration behavior. These setups do not subsequently run Flyway again, so applying the resource outside Flyway history does not produce a second V137 application within these fixtures.
- All four call the affected publishedPackageVersion path (`CourseDemoContentSeedTest:58`, `CourseSeedTest:89`, `CourseSequenceIntegrationTest:101`, `MultiSkillSeedTest:90` at review time). The unchanged assertions continue to inspect actual reader results and historical seed content.
- Focused `git diff --check` returned exit 0. No Maven command was run by this review.

Summary: The fixture change restores compatibility with the current reader without weakening the historical seed checks or changing runtime behavior.
Concerns/Blockers: No review blocker. Required fresh full Content suite and ArchitectureTest rerun remain pending; no passing result is claimed here.

