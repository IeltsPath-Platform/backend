# Content placement-pool contract review

Date: 2026-11-17
Scope: phase 1 Content only. Static verification of `content-package-authoring-v1.md` and `learning-content-internal-v1.md` against final controllers, request/response records, use cases, readers and persistence mapping. No Assessment/Learning implementation or preview contract claim is included.

## Result

No concrete drift found in the placement-pool contract changes reviewed. Both contracts describe the implemented field names, optional filters, validation, publication visibility and nullable package-version pool accurately.

## Acceptance mapping

Source paths below are relative to `services/content-service/src/main/java/com/ieltspath/content/` unless otherwise stated.

| Acceptance | Verified evidence | Result |
| --- | --- | --- |
| Admin create requires a placement pool; other types reject it; valid value is returned and saved | `api/dto/request/AuthorPackageRequests.java` CreatePackage `@AssertTrue`; `api/controller/ContentPackageAuthoringController.java:81` forwards the pool; `application/usecase/CreateContentPackageUseCase.java:54` validates it and passes it to aggregate creation; AuthorPackageResponse maps it | Matches authoring contract section 7 |
| Legacy create has the same pool rules | `api/dto/request/CreateContentPackageRequest.java` has enum field and `@AssertTrue`; `api/controller/ContentPackageController.java:66` forwards it; the shared use case and ContentPackageResponse retain it | Matches authoring contract section 7 |
| Missing/inapplicable/unknown pool returns 400 | Both create requests use Bean Validation and enum binding; `api/exception/GlobalExceptionHandler.java:273` maps validation failures to 400; malformed JSON/unknown enum maps to 400 at line 317; illegal argument maps to 400 at line 61 | Matches both contracts |
| Stored pool is retained and cannot be edited through the API | Aggregate has final placementPool; `infrastructure/persistence/mapper/ContentPackagePersistenceMapper.java` maps both directions; JPA enum column has `updatable = false`; no pool mutation endpoint | Matches immutable-pool claim |
| Placement publication requires a pool, including scheduling | `application/usecase/PublishContentPackageUseCase.java:80` throws PLACEMENT_POOL_REQUIRED; exception handler exposes details.code with 400; `SchedulePackagePublicationUseCase.java:37` calls validateForPublish | Matches authoring contract section 7 |
| Admin list accepts optional placementPool and returns each package pool | `ContentPackageAuthoringController.java:63` uses optional enum query parameter; ListAuthorPackagesUseCase forwards Filter; `JdbcAuthorPackageReader.java:41` adds SQL pool predicate only when provided; selection and AuthorPackageSummaryResponse retain nullable placementPool | Matches authoring contract section 1; unknown enum returns 400 via parameter mismatch handler |
| Internal listing has optional pool, absent means all, per-item pool and published filtering | `api/controller/InternalLearningContentController.java:84`; GetPlacementTestPackagesUseCase forwards optional pool; `JdbcLearningContentReader.java:752` conditionally filters by pool, joins current version to the same package, requires both statuses PUBLISHED, orders by package ID; PlacementPackageResponse has packageId/packageVersionId/code/pool | Matches internal placement-packages contract, including empty array behavior |
| Internal package-version response carries nullable placementPool and preserves it through media resolution | `JdbcLearningContentReader.java:867` selects nullable package pool; `application/result/PackageVersionContentResult.java:30` withSections retains it; GetPackageVersionContentUseCase uses withSections for audio/image transformations; `api/dto/internal/PackageVersionContentResponse.java:67` maps it | Matches internal package-versions contract |
| Existing placement seed is backfilled without changing ceiling | `src/main/resources/db/migration/V137__placement_pool.sql` backfills existing PLACEMENT_TEST rows to BAND_5_9; PlacementPool.ceiling() is 9.0 for that value | Matches both contracts |
| Shared topic/course response remains unchanged | Placement listing uses dedicated PlacementPackageResult/PlacementPackageResponse; topic/course response remains separate | Matches phase 1 contract boundary |

## Verification evidence and limits

Graph-first navigation was attempted with `graphify query "PlacementPool" --limit 15`; it returned no matching nodes. Focused source reads and searches then supplied the evidence above.

Existing test source was inspected for both create routes, admin list filter and summary, internal filter/unknown pool, nullable/present package-version pool, mapper round-trip, section replacement, migration/reader visibility and seed backfill. Relevant classes include ContentPackageAuthoringErrorTest, ContentPackageControllerTest, InternalLearningContentControllerTest, ContentPackagePlacementMappingTest, PlacementPoolMigrationTest and PlacementTestSeedTest. Inspection is not a claim that these tests passed. No Maven commands were run by this reviewer; execution results belong to the controller's phase 1 validation report.

No source, contract document, rule file, other service or generated graph was modified. This report is the only owned output.

Status: DONE
Summary: Phase 1 placement-pool contract changes match the verified Content implementation; no concrete contract drift found.
Concerns/Blockers: None in reviewed contract scope. Runtime/build results were not independently rerun.
