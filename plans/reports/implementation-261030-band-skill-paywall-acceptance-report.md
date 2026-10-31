# Báo cáo nghiệm thu — Band-skill paywall

Ngày cập nhật: 2026-10-30
Nhánh: `feat/band-skill-paywall` ở backend và frontend.

## Tiến độ

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Graphify backend/frontend | DONE | AST cục bộ; backend sau phase C 22,750 nút/91,898 cạnh, frontend sau A.3 8,267 nút/20,766 cạnh. Graph query xác nhận các node course, `course_skill_prices`, reader, `LearningContentClient` và lesson. Output nằm trong `graphify-out/` và không được đưa vào commit. |
| A.1 Cover content-service | DONE | V86 và API/content mapping đã commit; test controller và draft controller chạy xanh. TDD red-first bị lệch: dependency download bị chặn nên test chỉ chạy sau khi triển khai. |
| A.2 Cover learning-service passthrough | DONE | Commit `7ce7556b`; DTO/client/content response mapping và V86 reader integration xanh. |
| A.3 Cover frontend | DONE | Admin forms, learner course/topic adapters, banner/cards and placement summary updated; focused tests, typecheck and lint xanh. Commit `cdcad2e`. |
| B. Access unlock debit | DONE | V5 ledger check plus `COURSE_UNLOCK_DEBIT` mapping; focused tests + Testcontainers integration xanh. |
| C. Content unlock prices/facts | DONE | V87 prices, V88 clear topic gates, one-skill authoring checks, learning payload fields and SQL ordering facts. Data audit identifies the existing `TREES_MULTI_SKILL` violation; content owner fixes remain a ship prerequisite. |
| D. Per-skill progression | DONE | `TopicStatusDeriver` now opens one topic per course skill. Focused tests 33/33; full learning-service reactor 1,002/1,002. Commit `215ee045`. |
| E. Learning purchase | DONE | Durable V38 unlock purchase, replay/reconciliation and `/courses` ownership/pricing fields. Focused unit/API + 3 PostgreSQL integration tests passed; full learning-service reactor: 1,019 tests passed. Commit `6ec31d7a`. |
| F. Paywall gate | DONE | Shared request-scoped gate, V39 grandfathering, lesson-list flags, free-practice and tree enforcement. Focused 84/84; full learning-service reactor 1,035/1,035. |
| G. Frontend paywall/admin prices | DONE_WITH_LIMITS | Touched tests, typecheck, lint and production build pass; the full suite has one unrelated flashcard copy assertion failure. Browser verification against a running authenticated API remains unrun. |
| H. Audit/contracts/scenarios | PARTIAL | Learning and Access contract docs plus lesson-count audit SQL updated. Existing seed audit violations remain; no dev database is running, and source has no monetary prices for key products. The Assessment V2 seed sets AI Writing grading at 3 Points; the monetary price table still needs product-owner inputs. |

## Môi trường

- Java: 21.0.6.
- Node.js: 24.21.0.
- Docker Desktop/daemon: chạy được trong test nâng quyền; A.2 reader integration với Testcontainers đã xanh. Cần xác nhận lại trạng thái khi chạy các integration suite cuối và audit DB.
- `npm --version` hiện lỗi EPERM khi Node cố truy cập `C:\Users\pduy8`; sẽ thử lệnh trực tiếp trong workspace và ghi rõ kết quả.
- Chưa đọc hoặc sửa tệp `.env`/secret.

## Commits

- Backend: `db9439df docs(plans): finalize course cover and skill paywall plans`.
- Backend: `820e15dc feat(content): add course covers and descriptions`.
- Backend: `3534de4f test(content): assert topic descriptions pass through requests`.
- Backend: `7ce7556b feat(learning): pass course cover and descriptions`.
- Frontend: `cdcad2e feat(frontend): show course covers and descriptions`.

## A.1 — Cover content-service

- Migration: `V86__course_cover_and_descriptions.sql`.
- Thay đổi: course `coverImageUrl`/`description`, topic `description`; request limits, URL scheme validation, blank-to-null normalization; giữ constructor cũ và mapping persistence/API.
- Test viết trước implementation: `CourseControllerTest`, `TopicControllerTest`, `CoursePersistenceTest` được thêm/cập nhật. Do Maven Central chưa tải dependency được trong sandbox, red run không thể thực hiện trước implementation.
- Lệnh xanh: Maven `-pl services/content-service -am -Dtest=CourseControllerTest,TopicControllerTest,CourseDraftControllerTest,CoursePersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test` — 33 test, 0 failure/error. Build biên dịch 553 source và 178 test source.
- TDD deviation: lần chạy đầu sau commit cho thấy test `TopicControllerTest` khẳng định sai rằng controller trim command; sửa test để kiểm tra request được chuyển nguyên vẹn, còn aggregate chuẩn hóa nội dung. Lần chạy clean từ đúng working directory xanh.
- `CourseMaterializer`/import/draft vẫn dùng được nhờ constructor tương thích; `CourseDraftControllerTest` 11/11 và `CoursePersistenceTest` 5/5 xanh. Chưa chạy `ApproveCourseDraftIntegrationTest` (Docker/Testcontainers không khả dụng).
- Kiểm tra `git diff --check`: xanh.

## A.2 — Cover learning-service passthrough

- Learning content client và response mapping chuyển `coverImageUrl`, course description và topic description; tương thích payload cũ thiếu field; API contract được cập nhật.
- Test xanh: learning client 33/33, learning response mapping 3/3, content response mapping 2/2, Testcontainers reader integration 1/1; cả hai service qua `test-compile`.
- Commit: `7ce7556b1ea3178f632ea3628f30f56b7266361e`.

## A.3 — Cover frontend

- Admin course drawer gửi cover URL/mô tả dạng nullable và có preview; topic drawer gửi mô tả. Learner adapter truyền fields tới course hero, spotlight, tiles, topic hero và placement cards; ảnh lỗi/quên URL dùng band art hiện có.
- Focused Vitest: 45 test, 0 failure/error. `tsc -b --pretty false` và ESLint cho file đã sửa xanh.
- Graphify frontend được cập nhật bằng AST cục bộ sau thay đổi.

## B — Access unlock debit

- Thêm `COURSE_UNLOCK_DEBIT` và ánh xạ reference `COURSE_UNLOCK` trong writer và idempotent replay; V5 mở rộng transaction-type check mà giữ nguyên các giá trị cũ.
- Tests trước implementation đỏ do enum chưa có giá trị mới (test compile); sau implementation, focused Access tests xanh: DebitPointsUseCase, PointDebitWriter và `PointCreditIntegrationTest`, tổng 16 test, 0 lỗi. Testcontainers xác nhận V5 chạy và debit trả đúng ledger type.

## C — Content prices and paywall facts

- Added `course_skill_prices` and `courses.combo_unlock_points` in V87, plus V88 clearing `required_feature_key` from every topic. Prices are not seeded; the course create/update API replaces the skill-price map, preserves omitted legacy fields, and validates positive prices and combo boundaries.
- Course topics now require one non-`ALL` skill; course topic knowledge points must use that skill or `ALL`, and lesson knowledge point assignments reject mismatches. The audit is diagnostic only and does not mutate seed content.
- Content reader returns course prices, available skills, topic skill, 1-based skill position and earliest lesson position. Position ranking uses active course topics and published lessons ordered by topic order/id then lesson order/id. Legacy schema targets keep passing through nullable projection fallbacks.
- Learning-content client, learner topic result and lesson response now carry course prices and lesson course/skill/position facts.
- Added audit SQL and report. Latest test database reports one unskilled course topic (`TREES_MULTI_SKILL`), five mismatched knowledge points (`TRAP_KEYWORD_MATCH_ONLY`, `TR_L_DETAIL`, `TR_R_SCANNING`, `TR_R_WORD_IN_CONTEXT`, `TR_W_OPINION`), and two multi-skill lessons (`T1`, `T2`). These seed violations remain a content-owner prerequisite before enabling the paywall.
- Focused clean Maven run: 91 tests, 0 failures/errors, including course pricing/controller/persistence, topic/KP/lesson validation, course sequence on the old v52 schema, lesson pipeline on v79, and V87/V88 Testcontainers migration/reader/audit checks.
- Follow-up `TopicSequenceCourseMetadataIntegrationTest`: 4/4 passed after adding cross-topic skill ranking edge cases (inactive topics and draft lessons are excluded; knowledge-point earliest position is skill-local).
- Graphify refreshed after phase C: backend graph reports 22,750 nodes and 91,898 edges.

## D — Per-skill topic progression

- Topic ordering now groups by `(courseId, skill)`, so a learner can have one `IN_PROGRESS` topic in each course skill. Topics without a course retain a shared sequence; completed and passed topics keep their prior status meaning. No schema migration is needed, and current progress remains derived from stored `passedAt` / `testedAt` values.
- Updated the `LearnerCurriculum` rule comment and learning-service README. Lesson access, course aggregation and suggestions already consume derived statuses and aggregate by course, so they needed no behavior changes.
- Focused regression tests for progression and lesson gates: 33 passed, 0 failures/errors.
- Full learning-service reactor: 1,002 tests passed, 0 failures/errors/skips, including Testcontainers integrations. Surefire waited 30 seconds for broker-listener shutdown after the suite, then reported BUILD SUCCESS.
- Commit: `215ee045`.

## E — Learning unlock purchase

- Added V38 learner unlock persistence with one non-cancelled row per `(user, course, skill-or-combo)` and unique request ids. Pending intents and insufficient-balance cancellations commit in `REQUIRES_NEW` transactions.
- `POST /courses/{id}/unlocks` validates target/availability/ownership, prices skill purchases from Content, calculates combo price using actual paid ACTIVE skill costs, records a pending intent before the Access debit, and replays with the stored request id and amount.
- `/courses` now reconciles pending purchases first and returns prices, available/unlocked skills, combo payable points and `premium`. Access outage leaves pending rows for retry and degrades the display-only premium field to false.
- Focused unit/API tests: 16 passed. V38/PostgreSQL persistence integration tests: 3 passed. Combined focused coverage: 21 passed, 0 failures/errors/skips.
- Full learning-service reactor: 1,019 tests passed, 0 failures/errors/skips. Surefire waited 30 seconds for broker-listener shutdown, then reported BUILD SUCCESS.
- Implementation commit: `6ec31d7a`.

## F — Lesson paywall gate

- `PaywallPolicy` and a request-scoped `PaywallGate` enforce free-first-three, ACTIVE skill/combo ownership, grandfathered progress and Premium in that order. Premium is fetched once only when earlier rules do not open the content.
- Lesson reads, resources, exercise/essay submissions and completion share `LessonAccess`; topic lesson summaries expose `paywall` and `freePreview` independently of progress `LOCKED`. Practice-set listing was not gated through `LessonAccess` at this phase; it was added in the fix round below. The listing gate preserves completed-lesson review catalogs after Premium expires, while regular lesson reading remains subject to the paywall.
- V39 marks pre-existing lesson progress grandfathered and leaves new rows false. Free practice checks the point's earliest lesson position before hearts or attempts; the tree adds `locked` and suppresses `practiceAvailable`, degrading Access outages to locked only for points that need Premium.
- Focused Phase F tests: 84 passed, 0 failures/errors/skips, including migration, Premium expiry, no-side-effect and tree outage scenarios.
- Full learning-service reactor at the end of Phase F: 1,035 tests passed, 0 failures/errors/skips. The latest full reactor after this fix round is recorded in the acceptance update below.
- Implementation commit is recorded in Git history.

## G — Frontend paywall and admin prices

- Learner course summaries now carry skill/combo prices, ownership, availability and Premium state. Lesson rows distinguish paywall from progress locks and mark free previews; topic mapping preserves server status.
- Added an accessible unlock dialog on course cards, paywalled lesson rows, and locked tree points. It displays server skill/combo net price, Points balance and shortfall; reuses the existing Points key activation dialog. The client sends UUID `Idempotency-Key` and keeps the key after an uncertain response while it refreshes/reconciles course state.
- Topic test copy explains the unlock requirement. Speaking has no purchase CTA and is shown as coming soon. A tree `PAYWALL` response opens the unlock dialog.
- Admin course edit saves the per-skill price map and nullable combo price with positive integer and combo-range validation. Course topic creation requires a skill. Existing affected admin fixtures/assertions were updated to the new request shape.
- Focused Phase G suites passed, including 26 earlier learner/admin tests, 43 affected admin/adapter tests, and 16 tree/lesson/unlock tests; added retry-key and unlock HTTP contract tests pass (3 tests).
- Final full frontend run at Phase G completed 202 files / 1,696 tests: 1,695 passed and one unrelated mixed-up-answer copy assertion failed in `flashcards/lib/owl-reactions.test.ts`. In the fix round below, the full frontend suite was rerun once; the same untouched file had two copy assertions fail while its isolated 21-test run passed. The admin/package fixtures affected by this feature were corrected and their focused suites passed.
- `npm run typecheck`, ESLint, and production build passed. Browser verification against a running authenticated API was unavailable.

- Frontend implementation commit: `08c0813` (`feat(frontend): add band skill paywall UI`). The pre-existing frontend `package-lock.json` modification was left uncommitted.

## H — Content audit, contracts, and scenarios

- Updated `lesson-learning-v1.md` with per-skill topic progression, course unlock fields, `POST /courses/{id}/unlocks`, idempotency and lesson/free-practice paywall behavior. Updated `access-points-v1.md` with the service-to-service debit contract and `COURSE_UNLOCK_DEBIT` ledger mapping.
- Extended `course-skill-content-audit.sql` with published lesson counts and counts beyond the 3-lesson free preview for every `(course, skill)`, including zero-content skills.
- Before the Phase 6 repair, the content audit found one unskilled tree topic, five mismatched knowledge points and two multi-skill tree lessons. V89 repairs these seed facts; the migrated-database audit now returns no violations in all three checks.
- A persistent development database was not run or inspected. Access source contains key product entitlements but no monetary prices for Point/Premium products. Assessment V2 seeds Writing AI grading at 3 Points. The money-to-Points conversion and product-approved price table still require monetary price inputs and owner sign-off. No price was entered in a persistent database.
- Contract/audit documentation commit: `89cd9f97`. Course counts and the clean-audit results are recorded in Phase 6 closeout below; approved prices and live-stack scenarios are handled separately.

## Fix round acceptance — 2026-10-31

- Before this fix round, the full content-service suite had not been run. Its first full verification failed once at `MultiSkillContentIntegrationTest.singleSkillCompatibilityComesFromExercisesRatherThanTopicMetadata`: it expected `LISTENING` but the reader correctly returned topic skill `READING`. The test now asserts singular lesson/topic skill `READING` and separately asserts plural `skills` remains derived from exercises/knowledge points (`LISTENING`). A later run exposed a stale IDE-generated test class with an unresolved-import stub; cleaning generated Maven targets and recompiling fixed that separate build artifact issue, and the latest full suite then passed.
- Knowledge-point gating now uses `topic.skill()` when point skill is null, blank, `ALL`, or unparsable. Free practice applies the same fallback. Tests cover WRITING ownership opening an `ALL` point at earliest position 5, READING ownership not opening it, explicit point skill precedence, tree state, and free practice.
- Practice-set listing now goes through `LessonAccess.authorizePracticeSets` and the shared `PaywallGate`. Tests cover free preview, unowned paywall (`403 PAYWALL`), completed lessons after Premium expiry, owned skill, grandfathered progress, Premium, and Access outage: free/owned/grandfathered/completed still list, while a paywalled lesson fails with Access `503` and is not mislabeled `PAYWALL`. Normal lesson reading still uses the regular paywall path.
- Latest full backend runs (target module totals): content-service **1,077 tests, 0 failures, 0 errors, 1 skipped**; learning-service **1,043 tests, 0 failures, 0 errors, 0 skipped**; access-service **77 tests, 0 failures, 0 errors, 0 skipped**. The `-am` content run also ran common-security (9, all passed) and common-storage (60 total, 57 passed and 3 skipped); learning/access runs also ran common-events (17) and common-security (9), all passed. Aggregate reactor totals: content **1,146 run / 0 failed / 0 errors / 4 skipped**; learning **1,069 / 0 / 0 / 0**; access **103 / 0 / 0 / 0**. The focused learning tests passed **45/45** with no skips.
- Latest frontend checks: `npm run typecheck` passed (exit 0); `npm run lint` passed (exit 0); requested learning-path/admin-courses Vitest run passed **234/234 across 37 files**. One full `npm run test` rerun reported **202 files, 1,697 tests: 1,695 passed, 2 failed**. Both failures were unrelated copy assertions in the unchanged `src/features/flashcards/lib/owl-reactions.test.ts`; isolated rerun passed **21/21**, so this is treated as flaky and remains unresolved.
- On Windows, javac previously hit `AccessDeniedException` while resolving jars under `backend/.m2`. Tests were run with process-only `MAVEN_OPTS` setting the local Maven repository to `backend/.m2/repository` and `java.io.tmpdir` to `backend/target`; no shared Maven or project build configuration was changed.
- Hygiene: `backend/.m2/` is locally excluded through `.git/info/exclude`, not shared `.gitignore`. The service-boundary cleanup plan was left untouched.
- Acceptance: backend paywall/fallback/listing fixes and regression coverage are complete; all three backend full suites passed. Frontend checks passed except the two unrelated flaky owl-reaction copy assertions in the single required full run. Live browser scenarios and monetary prices were not verified and remain out of scope.

## Phase 6 closeout — 2026-10-31

Status: IN_PROGRESS. The seed repair, audit and proposed-price table are complete. Isolated live-stack scenarios remain.

### Seed repair and audit

- Added V89. Existing topic `2a000000-0000-4000-8000-010000000001` is now `TREES_READING`; created `TREES_LISTENING` (`...010000000002`) and `TREES_WRITING` (`...010000000003`). Existing Reading lessons `...030000000001/002` keep their identities. Added skill-specific lessons `...030000000003`–`...030000000006` (`T1_L`, `T2_L`, `T1_W`, `T2_W`).
- Moved `TR_L_DETAIL` to Listening, `TR_W_OPINION` to Writing, and the Reading trap point to Reading. Reassigned lesson blocks and Listening audio resource `31000000-0000-4000-8000-000000000007`; Reading resources remain attached to the original T1.
- Kept Reading practice packages in place, attached the existing Listening and Writing practice packages to `T1_L` and `T1_W`, and archived the mixed-skill practice package without deleting it. Created topic-test packages `...080000000007/008` and versions `...090000000007/008`; the existing Reading package/version remains in place. Published question and question-version IDs/content are preserved.
- Fresh-database and V88-upgrade migration tests pass. The three audit checks return zero unskilled topics, zero mismatched course knowledge points, and zero published course lessons with multiple or topic-mismatched skills.

| Course | Skill | Published lessons | Beyond first 3 |
|---|---|---:|---:|
| IELTS 5.5 | Listening | 4 | 1 |
| IELTS 5.5 | Reading | 11 | 8 |
| IELTS 5.5 | Speaking | 0 | 0 |
| IELTS 5.5 | Writing | 7 | 4 |
| IELTS 6.0 | Listening | 0 | 0 |
| IELTS 6.0 | Reading | 0 | 0 |
| IELTS 6.0 | Speaking | 0 | 0 |
| IELTS 6.0 | Writing | 0 | 0 |
| IELTS 6.5 | Listening | 1 | 0 |
| IELTS 6.5 | Reading | 1 | 0 |
| IELTS 6.5 | Speaking | 0 | 0 |
| IELTS 6.5 | Writing | 1 | 0 |
| IELTS 7.0 | Listening | 0 | 0 |
| IELTS 7.0 | Reading | 0 | 0 |
| IELTS 7.0 | Speaking | 0 | 0 |
| IELTS 7.0 | Writing | 0 | 0 |

- Counts and audit checks were verified on a fresh migrated Testcontainers PostgreSQL database. A persistent development `content_db` was not started or inspected.
- Full content-service reactor: **1,149 tests run, 0 failures, 0 errors, 4 skipped**; target content-service module: **1,080 run, 0 failures, 0 errors, 1 skipped**. common-security contributed 9 passing tests; common-storage contributed 60 run, 0 failures/errors, 3 skipped.
- Migration/test commit: `e901a459` (`fix(content): split tree course lessons by skill`).

### Proposed Points prices

Rule: `10 Points × lessons beyond the first 3`, clamped to 20–50 Points. Propose a combo only when at least three skills have prices: 70% of their sum, rounded up to a multiple of 5, bounded by the highest skill price and 100 Points.

| Course | Listening | Reading | Speaking | Writing | Combo |
|---|---:|---:|---:|---:|---:|
| IELTS 5.5 | **20 PROPOSED** | **50 PROPOSED** | — | **40 PROPOSED** | **80 PROPOSED** |
| IELTS 6.0 | — | — | — | — | — |
| IELTS 6.5 | — | — | — | — | — |
| IELTS 7.0 | — | — | — | — | — |

- IELTS 5.5 has three priced skills totaling 110 Points; 70% is 77, rounded up to 80. This is above the highest skill price (50), below the sum (110), and under the 100-Point cap.
- A dash means no proposed price because that skill has no lessons beyond the free preview; no combo is proposed for courses with fewer than three priced skills. Premium opens all lessons and has no Points price.
- These are Points amounts only, derived from the requested rule. Actual monetary prices for Point and Premium products are unknown. Product-owner approval is required before R&D enters any price. No price was written to a database.
