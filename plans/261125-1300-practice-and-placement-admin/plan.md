# Admin screens for the practice test library and the placement tests

Status: ready to implement. Decisions confirmed by the owner on 2026-11-25 (section 9). Do not push or open PRs; the
owner reviews first.

| Repo | Branch | From |
| --- | --- | --- |
| backend | `feat/practice-placement-admin` | `main` |
| frontend | `feat/practice-placement-admin` | `main` |

Run after `plans/261125-1230-premium-practice-tests` is merged, or rebase on it: both touch the Premium tag of practice
tests (this plan only edits the tag; that plan enforces it).

## 1. Goal

Today every package type is managed in one screen, "Đề & gói câu hỏi" (`/content-author/packages`), which mixes 7
types. Two of them are what learners see directly and need their own screens:

- **Thư viện luyện đề**: `PRACTICE_TEST`, the 4-skill free practice library, Free or Premium.
- **Đề đầu vào**: `PLACEMENT_TEST`, the placement tests split into the pools `BAND_0_3`, `BAND_3_5`, `BAND_5_9`.

Both new screens are views over the same content package model and reuse the existing editor (`PackageEditorPage`:
versions, sections, questions, preview, publish, schedule). No second authoring model.

## 2. Current state (verified on `main`)

Backend (content-service):
- `GET /api/content/admin/packages` (`ContentPackageAuthoringController.list`, `ListAuthorPackagesUseCase`,
  `AuthorPackageReader.Filter`) filters by `type`, `status`, `lessonId`, `topicId`, `courseId`, `q`, `placementPool`.
  No filter by access level or skill; not paginated.
- `AuthorPackageSummaryResponse` returns `id, code, title, packageType, accessLevel (FREE|PREMIUM), status,
  currentPublishedVersionId, lessonId, topicId, courseId, sourceCode, createdAt, updatedAt` and `placementPool`.
  It has no skill and no part list.
- `POST /api/content/admin/packages` creates a package with `accessLevel` (mapped to `required_feature_key` by
  `AccessLevelCompatibility`), `sourceCode` (practice tests) and `placementPool` (placement tests).
- **There is no route to change a package after creation** (title, access level, source, pool) and **no route to
  archive** one, although `PublicationStatus` has `ARCHIVED`.
- The learner catalog `POST /api/content/practice-tests/search` lists published `PRACTICE_TEST` packages that are not a
  course milestone. Learning reads placement packages from `GET /internal/learning-content/placement-packages`
  (published, by pool).
- Assessment has no per-package statistics route (`/api/assessments/admin` has examiner and evaluation routes only).

Frontend (`src/features/admin/packages`):
- `PackagesPage` lists all types with a type filter; for `PLACEMENT_TEST` it shows `PlacementPoolSummary` (published
  count per pool).
- `PackageCreateForm` has the type select, "Truy cập FREE/PREMIUM", "Nguồn đề" for practice tests and "Kho đề đầu vào"
  for placement tests (`placementPool` required).
- Menu (`adminMenu.ts`): "Đề & gói câu hỏi" in group `questions`.

## 3. Backend

### 3.1 Edit a package's information

- `PATCH /api/content/admin/packages/{packageId}` (`CONTENT_AUTHOR`), body with optional fields:
  `title`, `accessLevel` (`FREE|PREMIUM`), `sourceCode` (practice tests only), `placementPool` (placement tests only).
  The code and the type cannot change.
- Use case `UpdatePackageInfoUseCase` in `application/usecase`; the rules live in the `ContentPackage` aggregate:
  - `sourceCode` only for `PRACTICE_TEST`, `placementPool` only for `PLACEMENT_TEST`, otherwise `400` with a
    Vietnamese message naming the field.
  - `sourceCode` must be an existing source (same check as create); `placementPool` must be a valid pool.
  - The pool of a placement test can change only while the package has never been published (status `DRAFT` and no
    published version). Otherwise `409`, `details.code = "PLACEMENT_POOL_LOCKED"`, message
    "Đề đã xuất bản nên không đổi được kho band. Gỡ đề này và tạo đề mới cho kho khác.".
  - Access level change applies at once to the learner catalog and the Premium checks; no version is created.
- Response: the updated `AuthorPackageResponse`.
- Tests: aggregate rules, use case, `@WebMvcTest` for the route (validation, `403` for other roles), and a
  Testcontainers test that the change is stored.

### 3.2 Archive and restore

- `POST /api/content/admin/packages/{packageId}/archive` and `POST …/restore` (`CONTENT_AUTHOR`). Archive sets
  `ARCHIVED`; restore puts the package back to `PUBLISHED` if it has a published version, otherwise `DRAFT`.
- An archived practice test disappears from the learner catalog; an archived placement test is no longer offered to
  learners. Check the catalog query (`JdbcPracticeTestCatalogReader`) and the placement query already filter by
  `PUBLISHED`; add the filter where it is missing.
- Attempts already started on an archived test can still be finished and read (Assessment keeps its snapshot).
- Applies only to `PRACTICE_TEST` and `PLACEMENT_TEST` in this plan; other types answer `409` with a Vietnamese message.
- Tests: archive hides from the catalog and from placement selection; restore shows again; started attempts unaffected.

### 3.3 Placement pool safety

- Refuse (`409`, `details.code = "PLACEMENT_POOL_LAST_PUBLISHED"`, message
  "Kho đề này chỉ còn một đề đã xuất bản. Thêm đề khác trước khi gỡ đề này.") an archive that would leave a pool with
  no published placement test. Learning needs at least one published test per pool. (A pool change cannot cause this:
  only unpublished tests can change pool, see 3.1.)
- Tests: archive of the last published test of a pool is refused; archive when another published test exists passes;
  pool change of a published test answers `PLACEMENT_POOL_LOCKED`.

### 3.4 List filters for the new screens

- Extend `AuthorPackageReader.Filter` and the list route with `accessLevel` (`FREE|PREMIUM`) and `skill`
  (`READING|LISTENING|WRITING|SPEAKING`, from the sections of the current or latest version).
- Add to `AuthorPackageSummaryResponse`: `skill` (null when the sections mix skills or have none), `partNos` (sorted
  list from the sections), `sectionCount`, `questionCount` of the latest version, and `archived` follows from `status`.
  Load them in the same query (no per-row query, AGENTS.md §3.7).
- The list stays unpaginated only if the result is bounded; add `page`/`size` (default size 50) because practice tests
  grow. The old screen keeps working with the default page.
- Tests: Testcontainers query test for the new filters and fields.

### 3.5 Attempt statistics

- `GET /api/assessments/admin/package-stats?packageIds=…` (`CONTENT_AUTHOR`): per package, number of attempts started,
  submitted, and the average score of submitted attempts, all time. One grouped query, at most 100 ids.
- The frontend calls Content and Assessment separately; no new service-to-service call.
- Gateway: the route sits under `/api/assessments/**`; check the rate-limit tier in
  `infra/config-server/config-repo/api-gateway.yaml` covers it.

### 3.6 Contracts

- Document the new and changed routes in the content contract for authoring packages (or a new
  `docs/contracts/content-package-admin-v1.md` if none exists) and the Assessment stats route.

## 4. Frontend

### 4.1 Menu

- Group `questions` ("Câu hỏi & đề") gets two entries: "Thư viện luyện đề" (`/content-author/practice-tests`) and
  "Đề đầu vào" (`/content-author/placement-tests`).
- "Đề & gói câu hỏi" no longer lists or creates `PRACTICE_TEST` and `PLACEMENT_TEST`: remove both from its type filter
  and from its create form, move `PlacementPoolSummary` out of it, and show a short note with links to the two new
  screens. Each type is managed in one place.
- Update `adminMenu.ts`, `AdminRoutes.tsx` and the menu tests.

### 4.2 Thư viện luyện đề (`/content-author/practice-tests`)

- Tabs: Reading, Listening, Writing, Speaking (URL keeps the tab, `?skill=`).
- Header per tab: "N đề · M Premium · K đang ẩn".
- Filters: access (Tất cả / Miễn phí / Premium), status (Nháp / Đã xuất bản / Đã ẩn), source, search by code or title.
- Table columns: STT, Mã, Tiêu đề, Part, Số câu, Nguồn, Truy cập (badge "Premium"), Trạng thái, Lượt làm, Điểm TB,
  Hành động. Statistics come from 3.5 in one call for the rows of the page; if that call fails the table still shows
  with "—" in the two columns.
- Row actions: Soạn đề (opens `PackageEditorPage`), Sửa thông tin (drawer: title, FREE/PREMIUM, source; 3.1),
  Ẩn khỏi thư viện / Hiện lại (3.2, with a confirm dialog), Xem trước.
- "Tạo đề luyện" opens `PackageCreateForm` with the type fixed to `PRACTICE_TEST` and the skill tab preselected for the
  first section.

### 4.3 Đề đầu vào (`/content-author/placement-tests`)

- Three cards on top, one per pool (`BAND_0_3` 0–3.0, `BAND_3_5` 3.0–5.0, `BAND_5_9` 5.0–9.0): published count, draft
  count, and a warning when a pool has no published test. Reuse and move `PlacementPoolSummary`.
- Table filtered by pool (tabs or select): STT, Mã, Tiêu đề, Kho đề, Số phần, Số câu, Trạng thái, Lượt làm, Điểm TB,
  Hành động (statistics as in 4.2).
- Row actions: Soạn đề, Sửa thông tin (title; pool only for a test never published, the field is disabled with a
  short reason otherwise; 3.1), Gỡ / Khôi phục (3.2). The `409` answers of 3.1 and 3.3 show their message.
- "Tạo đề đầu vào" opens `PackageCreateForm` with the type fixed to `PLACEMENT_TEST` and the pool preselected.

### 4.4 Shared

- Follow `.sdd/constraints/admin-ui.md` and the `.adm-*` design system; drawers through `AdminDrawer`, confirms through
  `useAdminConfirm`, errors through `useAdminError`, loading, empty and error states on every list.
- `PackageCreateForm` gains an optional fixed type (and preset skill or pool); the old screen keeps the free choice.
- Types and API in `features/admin/packages/api` (zod schemas for the new fields).
- Tests (Vitest): each screen lists and filters; tabs; the edit drawer sends `PATCH` and refreshes; archive asks for
  confirmation and refreshes; the `409` of 3.3 shows the message; menu entries and routes.

## 5. Patterns applied (backend AGENTS.md §3.9)

- No new service-to-service call; the frontend reads Content and Assessment separately.
- Business rules in the aggregate and use cases; Vietnamese errors with `details.code`; contracts updated in the same
  change.
- Queries return the new list fields in one query, filtered and paginated in the database.
- No new migration unless a column is needed (none expected: `status`, `required_feature_key`, `source_code`,
  `placement_pool` exist).

## 6. Verification

- Backend: new tests in content (and assessment if 3.5), `ArchitectureTest` in a clean worktree or after a full `javac`
  compile (an open IDE rewrites `target/classes`), no `archunit_store` update.
- Frontend: `npm run lint`, `npm run typecheck`, the admin tests touched. If Vitest reports "failed to find the current
  suite", delete `node_modules/.vite/vitest` and run again.

## 7. Acceptance

- An author manages the practice library by skill: lists, filters Free/Premium, edits title, access and source, hides
  and restores a test, and opens the editor, without using "Đề & gói câu hỏi".
- An author manages placement tests by pool, sees which pool lacks a published test, edits title and pool, and cannot
  leave a pool without a published test.
- Changing a test to Premium locks it for normal learners at once (through the Premium practice plan).
- Archived tests are hidden from learners; attempts already started still finish.
- Both screens show attempts and average score per test; "Đề & gói câu hỏi" no longer shows these two types.
- A published placement test cannot change pool; a pool never loses its last published test.

## 8. Risks

- Changing access or pool of a published test affects learners immediately; the edit drawer states it.
- Pagination of the shared list route must not break "Đề & gói câu hỏi"; keep the default page large enough or page there
  too.

## 9. Decisions (confirmed 2026-11-25)

1. Statistics "Lượt làm" and "Điểm TB" are in scope (3.5).
2. `PRACTICE_TEST` and `PLACEMENT_TEST` are removed from "Đề & gói câu hỏi"; each type has one screen (4.1).
3. The pool of a placement test changes only before its first publication (3.1).
4. Restore puts a test back to published when it has a published version, otherwise to draft (3.2).
