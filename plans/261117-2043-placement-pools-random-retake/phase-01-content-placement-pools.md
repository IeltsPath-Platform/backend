---
phase: 1
title: "Content placement pools"
status: pending
priority: P1
dependencies: []
effort: "M"
---

# Phase 1: Content placement pools

## Overview
A `PLACEMENT_TEST` package belongs to one of three pools. Content stores and validates it, lets authors set it through the admin create endpoint, lists placement exams per pool for Learning, and tells Assessment the pool of an exam version.

## Requirements
- Functional: package `placementPool` in `BAND_0_3 | BAND_3_5 | BAND_5_9`, required for `PLACEMENT_TEST`, null for every other type; a placement package cannot be published without a pool; the internal listing filters by pool and returns the pool per item in a dedicated response; the internal package-version response carries the pool.
- Non-functional: additive contracts only; the existing published placement exam is backfilled to `BAND_5_9` (ceiling 9.0, so its behaviour does not change); no change to already applied migrations; the shared `TopicTestPackageResponse` stays as is.

## Architecture
- New `PlacementPool` enum in `content/domain/vo` with `ceiling()` (3.0 / 5.0 / 9.0); Content does not apply the ceiling, Assessment does, but the enum is the single list of pools for this service.
- `ContentPackage` aggregate gets `placementPool` (set at creation, immutable); persisted by `ContentPackageJpaEntity`, mapped by `ContentPackagePersistenceMapper` (repository `ContentPackageJpaRepository`), all under `infrastructure/persistence`.
- **Create endpoints.** The admin form posts to `/api/content/admin/packages` (`ContentPackageAuthoringController`, request `AuthorPackageRequests.CreatePackage`). That record and the legacy `CreateContentPackageRequest` both gain `placementPool`, validated the same way (required for `PLACEMENT_TEST`, rejected for other types). Both build `CreateContentPackageCommand`, which gains the field. Its other creators pass null: `PracticeSetMaterializer:82`, `CourseMaterializer:235`, `CourseImportUseCases:421`.
- Read side stays JDBC: `JdbcLearningContentReader.placementTestPackages()` selects the pool and accepts an optional pool filter; a new result record `PlacementPackageResult(packageId, packageVersionId, code, pool)` and a new response `PlacementPackageResponse` (`api/dto/internal`) serve it. `TopicTestPackageResult` / `TopicTestPackageResponse` stay for the topic and course test listings.
- Internal controller: `GET /internal/learning-content/placement-packages?pool=` (param optional; absent returns all pools, each with its `pool`).
- **Author list.** `GET /api/content/admin/packages` (`ContentPackageAuthoringController.list`, `ListAuthorPackagesUseCase`, port `AuthorPackageReader`, adapter `JdbcAuthorPackageReader`) gains an optional `placementPool` query parameter (`AuthorPackageReader.Filter` gets the field, the SQL adds `AND p.placement_pool = :placementPool`), and `AuthorPackageSummary` / `AuthorPackageSummaryResponse` carry `placementPool` (null for other types). This lets the admin Packages page show and filter the pool; per-pool counts are computed in the frontend from the placement list (a handful of rows, well under `LIST_LIMIT`).
- Package version internal response (`PackageVersionContentResponse` in `api/dto/internal`, served by `GET /internal/learning-content/package-versions/{id}`) and the result record behind it gain `placementPool` (null for other types) so Assessment can snapshot it when an attempt starts.

## Related Code Files
- Create: migration `V137__placement_pool.sql` in `services/content-service/src/main/resources/db/migration/`; `PlacementPool.java`; `PlacementPackageResult.java`; `PlacementPackageResponse.java`.
- Modify: `ContentPackage.java`, `ContentPackageJpaEntity.java`, `ContentPackagePersistenceMapper.java`; `AuthorPackageRequests.java` (`CreatePackage`), `CreateContentPackageRequest.java`, `CreateContentPackageCommand` and its five creators (`ContentPackageAuthoringController:76`, `ContentPackageController:66`, `PracticeSetMaterializer:82`, `CourseMaterializer:235`, `CourseImportUseCases:421`); package detail responses; the author list (`AuthorPackageReader`, `JdbcAuthorPackageReader`, `ListAuthorPackagesUseCase`, `AuthorPackageSummary`, `AuthorPackageSummaryResponse`); `PublishContentPackageUseCase.java` (placement branch near line 101-109); `LearningContentReader.java`, `JdbcLearningContentReader.java` (`placementTestPackages`, package version reader); `GetPlacementTestPackagesUseCase.java`; `InternalLearningContentController.java` (`/placement-packages`, version response DTO).
- Tests: package create/publish unit tests, `ContentPackageAuthoringController` and `ContentPackageController` tests (including the list filter and `placementPool` in the summary), `JdbcAuthorPackageReaderTest` (existing; add the pool filter), `JdbcLearningContentReader` Testcontainers test, migration test. Existing tests touching the placement listing and to update: `InternalLearningContentControllerTest` (4 references) and `PlacementTestSeedTest` (3 references; must keep passing after the backfill). `GetTopicTestPackagesUseCase`, `GetCourseTestPackagesUseCase` and `CoursePackagesUseCaseTest` are untouched.

## Implementation Steps (tests first)
1. **Red:** migration test: column exists, CHECK rejects an unknown value and a pool on a non-placement type, the existing `PLACEMENT_TEST` row is `BAND_5_9` after migrating.
2. **Red:** create-package tests on **both** controllers: placement without pool is `400`; pool on `PRACTICE_SET` is `400`; valid placement stores and returns the pool; the other creators of the command still compile and create packages with a null pool.
3. **Red:** publish test: placement package without a pool is refused with a clear code (`PLACEMENT_POOL_REQUIRED`).
4. **Red:** reader IT: three exams in three pools; no filter returns all with their pool; filter returns only that pool; unpublished or archived exams excluded (same rule as today).
5. **Red:** author list: `JdbcAuthorPackageReaderTest` with packages in two pools plus a non-placement package: `placementPool` filter returns only that pool; no filter returns all and each summary carries its pool (null for non-placement); controller test passes the `placementPool` query parameter through and an unknown value is `400`.
6. **Red:** internal controller JSON test for `placement-packages?pool=` (new `PlacementPackageResponse`) and for `placementPool` in the package-version response; update `InternalLearningContentControllerTest` and `PlacementTestSeedTest` for the new field.
7. **Green:** migration, enum, aggregate/entity/mapper, both request records, command and its creators, publish rule, reader and controller changes, author list filter and summary field.
8. Update `docs/contracts/learning-content-internal-v1.md` and `docs/contracts/content-package-authoring-v1.md` (create field, list parameter, summary field) in the same change.
9. Run `mvn -q -pl services/content-service -am test` and the service `ArchitectureTest`.

## Success Criteria
- [ ] All new tests failed before and pass after; full content-service suite and ArchitectureTest pass.
- [ ] A package created through `/api/content/admin/packages` with a pool keeps it; Learning can list exams per pool; Assessment can read `placementPool` of a version.
- [ ] The admin list returns `placementPool` per package and filters by it.
- [ ] The seeded exam is still listed and unchanged in behaviour (`BAND_5_9`).
- [ ] Contracts updated in the same PR.

## Risk Assessment
- Pools `BAND_0_3` and `BAND_3_5` have no exam until phase 6 seeds them (each exam needs its own `PLACEMENT`-purpose questions; a question can belong to only one published package, see `questionsUsedElsewhere` in `PublishContentPackageUseCase`). The picker disables empty pools; release Learning and the frontend only after phase 6's exams and audio are in place.
- Pool cannot be edited after creation by design; a mistaken pool means creating a new package. Revisit only if operations ask.
- Overlap with the in-progress author-workspace plan on the same controllers and DTOs: keep the change small and rebase.
