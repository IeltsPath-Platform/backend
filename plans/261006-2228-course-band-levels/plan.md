---
title: "Course theo band, gợi ý course từ placement và thi cuối course"
description: "Thêm Course (bậc band) nhóm topic; mọi course mở, mỗi course là một chuỗi riêng; placement chỉ gợi ý course; thi cuối course là mốc không chặn."
status: in-progress
priority: P2
branch: "feat/course-band-levels"
tags: [content, learning, assessment, tdd]
blockedBy: []
blocks: [261006-2309-multi-skill-lessons]
created: "2026-10-06T15:42:49.726Z"
createdBy: "ck:plan"
source: skill
---

# Course theo band, gợi ý course từ placement và thi cuối course

## Overview

Mentor (họp 2026-10-06): **course theo band, lesson không gắn band**. Chủ dự án chốt thêm (validate 2026-10-06):
target band, band ước tính từ placement và course đang học là ba thứ độc lập; học viên **tự vào course nào cũng được**.
Repo có `topics.band_min/max` nhưng `topic-sequence` không dùng band; placement event có `overall_band` nhưng learning
bỏ qua.

Nguồn: [brainstorm report](../reports/brainstorm-261006-2228-meeting-ba-analysis-roadmap-report.md) §4.
Chế độ: `--tdd`, mỗi phase viết test đỏ trước, code cho xanh sau.

## Giao cho người implement (Codex)

Người implement: Codex. Người viết plan: Claude. Base: `main` @ `6351f3a`.

1. Đọc theo thứ tự: `AGENTS.md`, `CLAUDE.md`, `plan.md` này (đặc biệt bảng quyết định và Validation Log), rồi từng
   phase trước khi làm phase đó. Bảng quyết định là chốt của chủ dự án: **không tự đổi**.
2. Tạo nhánh `feat/course-band-levels` từ base. Mỗi phase một commit conventional (`feat(content): ...`, `feat(learning): ...`). Không
   ghi mã plan, số phase hay mã quyết định (D1, M3…) vào code, comment, tên test, migration hay commit.
3. TDD: viết test đỏ của phase, chạy để thấy đỏ, rồi code cho xanh. Lệnh hẹp nhất trước:
   `mvn -q -pl services/<service> -am test`. Testcontainers cần Docker; thiếu Docker thì ghi rõ test nào bị skip,
   không tuyên bố đã chạy.
4. Chỉ được sửa kỳ vọng của **các test cũ liệt kê bên dưới**. Test cũ nào khác đỏ: dừng, ghi `BLOCKED` vào
   Verification kèm tên test và lý do, không tự sửa kỳ vọng.
5. Dừng và ghi `BLOCKED` khi: quyết định trong plan mâu thuẫn với code thật, số migration đã bị dùng, hoặc phải đổi
   contract ngoài phạm vi plan.
6. Nội dung seed (passage, câu hỏi, đáp án, giải thích) do Codex viết theo mẫu seed hiện có; chép toàn văn câu mới vào
   Verification để chủ dự án duyệt trong PR.
7. Không sửa `third_party/`, `.env`, không commit `graphify-out/`. Chạy `graphify update .` sau khi xong nếu máy có
   graphify.

### Test cũ được phép sửa kỳ vọng (đổi luật có chủ ý)

| Test | Vì sao đổi | Sửa thế nào |
| --- | --- | --- |
| learning `domain/service/TopicStatusDeriverTest.opensAnIndependentTopicForEachSkillIncludingUnspecified` | Chuỗi đổi từ theo skill sang theo course (D5) | Viết lại thành "mỗi course một chuỗi độc lập"; giữ các test còn lại của lớp |
| learning `infrastructure/persistence/ReviewAndTestAssignmentIntegrationTest.skillTracksKeepReviewsLocalAndPassWritingWithoutATest` (~dòng 267) | Stub topic-sequence không có `course` nên mọi topic rơi vào một chuỗi | Gán `course` khác nhau cho topic Reading và Writing trong stub để giữ ý định: review chỉ khóa cùng skill, Writing PASS không cần test |
| content `infrastructure/persistence/LessonPipelineSeedTest.topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints` (~dòng 369) | Sequence giờ chỉ gồm topic có course, thêm field `course` | Cập nhật kỳ vọng thứ tự và field theo backfill `IELTS_5_5` |
| content `src/test/resources/seed/lesson-pipeline-demo-expected.json` | Chỉ khi snapshot chứa output topic-sequence | Cập nhật snapshot, ghi lý do trong commit |
| learning `infrastructure/client/RestLearningContentClientTest` | Field `course` mới | Thêm case; case cũ (không có `course`) phải vẫn xanh |
| content `LessonPipelineSeedTest.lessonPracticeAndTestQuestionsAreReservedForLearning` (~dòng 858) | V21 thêm 24 question version LEARNING có owner (lesson, practice, topic test, course test) | **Chỉ** đổi `hasSize(147)` thành `hasSize(171)`; không đổi query hay assertion khác. *(Thêm 2026-10-07, chủ dự án duyệt sau khi Codex báo BLOCKED)* |
| content `LessonPipelineSeedTest.practiceSetsBelongToTheEarliestLessonTeachingTheirKnowledgePoint` (~dòng 523) | V21 thêm 2 practice set gắn lesson | **Chỉ** thêm 2 entry `R65-PS-INFERENCE → R65-I1`, `R65-PS-PARAPHRASE → R65-I1`; giữ nguyên 28 entry cũ. *(Thêm 2026-10-07, chủ dự án duyệt sau khi Codex báo BLOCKED)* |
| learning `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` (~dòng 60–61) | Mỗi migration learning mới đổi phiên bản Flyway hiện tại và số bảng | **Chỉ** cập nhật hai số: version = migration learning cao nhất sau phase đó, số bảng = 14 + số bảng mới tạo bởi các migration của plan (đếm từ file SQL, ghi phép đếm vào Verification); giữ nguyên kiểm tra health. *(Thêm 2026-10-07, chủ dự án duyệt sau khi Codex báo BLOCKED)* |

### Quyền tự quyết khi chạy không có người giám sát (chủ dự án duyệt 2026-10-07)

Áp dụng cho các phase còn lại (5, 6, 4, 7). Codex **được tự sửa kỳ vọng của test cũ** mà không cần hỏi, nếu thỏa **cả ba**:
1. Test đỏ chỉ vì dữ liệu hoặc số đếm do chính plan này thêm (migration, seed, bảng, field mới), **hoặc** vì luật D5
   (chuỗi theo course thay cho chuỗi theo skill) đúng như phase 6 mô tả.
2. Chỉ sửa con số, danh sách hoặc kỳ vọng trạng thái cho khớp luật mới; không xóa test, không nới assertion, không đổi
   ý định của test (ví dụ: review vẫn chỉ khóa cùng skill).
3. Ghi vào Verification: tên test, kỳ vọng cũ → mới, lý do gắn với D-nào.

Vẫn phải dừng và ghi `BLOCKED` khi: test đỏ vì hành vi nghiệp vụ khác plan, phải đổi contract hoặc route ngoài plan, phải
đổi bảng quyết định, migration xung đột, đụng `common-security`/Gateway/JWT, hoặc Docker không chạy được (khi đó không
được commit phase có migration). Không push, không tạo PR, không merge.

## Quyết định đã chốt (implementer không được tự đổi)

| # | Quyết định |
| --- | --- |
| D1 | **Course = bậc band dùng chung mọi skill**, một `band_level` (5.5, 6.5…), unique, sắp theo `band_level`. Không có `sort_order`. |
| D2 | `topics.course_id` nullable FK. `topic-sequence` **chỉ trả topic có course ACTIVE**. Seed cũ được backfill vào course `IELTS_5_5`. |
| D3 | Giữ `topics.band_min/max` (không xóa); lộ trình chỉ dùng course. |
| D4 | **Mọi course đều mở.** Không có "course đang học", không API chọn course. Học viên vào course nào thì học course đó. |
| D5 | **Chuỗi theo course** (không tách skill): trong mỗi course, topic chưa PASS đầu tiên là `IN_PROGRESS`, còn lại `LOCKED`, đã pass là `PASSED`. Topic một skill cũ cùng course cũng học lần lượt (chuẩn bị cho lesson nhiều skill). Không thêm giá trị `TopicStatus` mới. Topic chưa có thông tin course (dữ liệu trước refresh) gom vào nhóm `course = null`. |
| D6 | Review chờ vẫn chặn lesson, Practice, thi topic **cùng skill ở mọi course** (giữ luật hiện tại). |
| D7 | Thứ tự chuỗi và hiển thị: `course.band_level → miễn phí/premium → sortOrder → topicId`. |
| D8 | **Thi cuối course là mốc không chặn**: package `COURSE_TEST` (chỉ seed, gắn `course_id`, **chỉ câu Reading tự chấm**). Mở khi **mọi topic của course PASSED**. Đạt ≥ 70% (`PassMark`) thì course `PASSED`. Không chặn topic nào. |
| D9 | Assessment ánh xạ `COURSE_TEST → COURSE_GATE`: `assessment_type` mới trong `AssessmentCompleted.v2` (thêm giá trị có tài liệu, không đổi version). Learning phải nhận được `COURSE_GATE` trước khi assessment phát; vì một PR nên commit phase 6 đứng trước phase 4. |
| D10 | **Placement chỉ gợi ý**: event `PLACEMENT` có `overall_band` khác null thì lưu band ước tính mới nhất; `GET /api/learning/courses` đánh dấu `recommended=true` cho course thấp nhất có `band_level` ≥ band đó (không có thì course cao nhất). Không đổi trạng thái topic nào. Target band ở user-service không dùng. |
| D11 | API mới của learning (`GET /courses`, `POST /courses/{id}/test-assignments`) chỉ cho role `CUSTOMER` (`@PreAuthorize`, method security đã bật trong common-security). Đây là pattern mới trong learning; API cũ giữ nguyên. |
| D12 | Một nhánh `feat/course-band-levels`, một PR, mỗi phase một commit. |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Content course model](./phase-01-content-course-model.md) | Completed |
| 2 | [Content sequence and course test packages](./phase-02-content-sequence-and-course-test-packages.md) | Completed |
| 3 | [Content seed courses](./phase-03-content-seed-courses.md) | Completed |
| 4 | [Assessment course gate](./phase-04-assessment-course-gate.md) | Completed |
| 5 | [Learning placement recommendation](./phase-05-learning-learner-level.md) | Completed |
| 6 | [Learning course path and course test](./phase-06-learning-course-path-and-course-test.md) | Completed |
| 7 | [Docs and verification](./phase-07-docs-and-verification.md) | Pending |

Thứ tự commit: 1 → 2 → 3 → 5 → 6 → 4 → 7.

## Acceptance criteria

Dữ liệu ví dụ: course 5.5 có R1 → R2 → L1 (theo `sortOrder`; R là Reading, L là Listening); course 6.5 có R3 → R4.

- Học viên mới, `GET /api/learning/topics`:
  - R1, R3 là `IN_PROGRESS`; R2, L1, R4 là `LOCKED`;
  - mỗi topic có `course {courseId, code, bandLevel}`.
- PASS R3 thì R4 thành `IN_PROGRESS`, và trạng thái của R1/R2/L1 không đổi. PASS R1, R2 thì L1 thành `IN_PROGRESS`.
- Có review chờ ở skill Reading thì lesson R1 và R3 đều bị chặn; học viên đã PASS R1, R2 vẫn học được L1.
- Event `PLACEMENT` với `overall_band=6.0`:
  - `GET /api/learning/courses` có course 6.5 `recommended=true`;
  - trạng thái topic không đổi.
- `POST /api/learning/courses/{id}/test-assignments`:
  - 403 `COURSE_TEST_LOCKED` khi còn topic chưa PASS; đủ điều kiện thì giao mã đề;
  - event `COURSE_GATE` ≥ 70% làm course `PASSED`;
  - role khác `CUSTOMER` bị 403.
- Mọi test learning, content và assessment hiện có vẫn xanh.
- `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test` xanh.

## Ngoài phạm vi

Assessment tự quy điểm placement ra band · seed bài placement · course theo skill · ép học tăng dần theo band · course
"đang học" · API admin tạo package `COURSE_TEST` · UI.

## Câu hỏi mở (không chặn implement)

1. Hai course cùng band (Academic/General) có cần không? D1 đang ép `band_level` unique.
2. Nội dung thật cho course 6.5 và COURSE_TEST do ai viết? Seed phase 3 chỉ là demo tối thiểu.
3. Khi trình bày cho hội đồng: mentor muốn học "tăng dần", còn D4 mở mọi course. Cần một câu giải thích (gợi ý course
   từ placement thay cho việc ép thứ tự).

## Dependencies

- Không có plan nào đang mở (mọi plan trong `plans/` đã `completed`). Dựa trên kết quả của
  `261002-1600-skill-tracks-practice-remediation` (chuỗi theo skill, `topics.skill`).

## Validation Log

### Session 1 — 2026-10-06

**Verification Results**
- Claims checked: 34 · Verified: 31 · Failed: 3 · Unverified: 0 · Tier: Full
- Failures:
  - F1: learning-service không có `@PreAuthorize` ở controller nào (`services/learning-service/src/main/java/com/group01/learning/api/controller/*`); phase 5 cũ giả định role CUSTOMER → chốt D11.
  - F2: `LearningGateException` luôn map 403 (`api/exception/GlobalExceptionHandler.java:30-35`), không ra 409 được → lỗi không còn liên quan vì API tự khai level đã bị bỏ (D4, D10).
  - F3: content không có validator `answer-spec-v1` (validator chỉ có trong grader của assessment/learning) → phase 3 dùng test cấu trúc.

**Questions asked: 9** (gồm 2 câu làm rõ mô hình)

| Chủ đề | Quyết định |
| --- | --- |
| Phân quyền API mới | Kiểm `CUSTOMER` (D11) |
| Level / band học viên | Target, placement, course độc lập; học viên tự vào course (D4) |
| Topic course khác | Mở hết (D4) |
| Chưa chọn course | Không có khái niệm chọn course (D4) |
| Đổi course | Tự do (hệ quả của D4) |
| Placement | Chỉ gợi ý course (D10) |
| Mô hình chuỗi | Chuỗi theo (course, skill), không `OPTIONAL` (D5; **đổi thành chuỗi theo course ở Session 2**) |
| Seed | `COURSE_TEST` chỉ Reading, kiểm tra cấu trúc (D8, phase 3) |
| Nhánh/PR | Một nhánh, một PR (D12) |

**Thay đổi so với bản trước:**
- Bỏ level học viên (tự khai, placement ghi đè), bỏ trạng thái `OPTIONAL`, bỏ "course bắt đầu theo level".
- Thêm gợi ý course từ placement; chuỗi theo (course, skill); kiểm role CUSTOMER cho API mới.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-07
- Decision deltas checked: 6 (bỏ level/tự khai, bỏ OPTIONAL, chuỗi (course, skill), placement chỉ gợi ý, kiểm role CUSTOMER, một PR)
- Reconciled stale references: 9 (phase 3 seed Reading-only + test cấu trúc; phase 4 thứ tự commit/DLQ; phase 5 viết lại; phase 6 viết lại; phase 7 docs/AGENTS §3.8)
- Unresolved contradictions: 0
- Ghi chú: brainstorm report §4 (course bắt đầu theo placement) đã lỗi thời; plan này là nguồn đúng.

### Session 2 — 2026-10-06 (từ brainstorm lesson nhiều skill)
- Nguồn: [brainstorm multi-skill](../reports/brainstorm-261006-2309-multi-skill-lessons-report.md) M3.
- D5 đổi: chuỗi theo (course, skill) → **chuỗi theo course**. D7 bỏ `skill` khỏi thứ tự. Acceptance criteria và ví dụ cập nhật.
- D6 (review chặn theo skill) giữ nguyên ở plan này; plan multi-skill đổi sang "lesson chứa skill".

### Whole-Plan Consistency Sweep (Session 2)
- Files reread: plan.md, phase-03, phase-06, phase-07
- Decision deltas checked: 2 (khóa chuỗi, thứ tự sắp)
- Reconciled stale references: xem phase 6, phase 7
- Unresolved contradictions: 0

## Verification

(Codex điền sau mỗi phase: commit, lệnh đã chạy và kết quả, test bị skip, test cũ đã sửa kỳ vọng, nội dung seed mới,
mọi `BLOCKED`.)

### Phase 1 Verification — 2026-10-07

- Status: `DONE_WITH_CONCERNS` — implementation and available tests complete; PostgreSQL checks skipped because the Docker daemon is unavailable. No `BLOCKED` condition.
- Commit: `d86775862fa1664a0a162688d6e382885121fe69` — `feat(content): add band-level courses and topic membership`.
- Scope: additive `V19__courses.sql` (no seeds); framework-free `Course`, repository, MapStruct/JPA adapter, course author/list APIs, scoped 409 course conflicts, and nullable `courseId` through topic domain, persistence, commands/results, request DTOs, and detail/tree responses. Existing topic duplicate-code 400 behavior and band metadata preserved. Update null retains course membership; create without a course remains valid. Content README updated for these APIs.
- TDD red: wrote all initial phase tests before production code. The sandbox attempt could not resolve Maven dependencies due to cache/network permissions and is not counted as the red proof. Elevated runs of the following command exited 1 at `testCompile` because `Course`, `CourseRepository`, course use cases/controller, and persistence types did not yet exist:

  ```powershell
  mvn -q -pl services/content-service -am test '-Dtest=CourseTest,CourseControllerTest,ContentAuthorizationWebMvcTest,UpdateTopicUseCaseTest,CreateTopicCourseUseCaseTest,TopicControllerTest,CourseMigrationTest,CoursePersistenceTest' '-Dsurefire.failIfNoSpecifiedTests=false'
  ```

- TDD green: same focused command after implementation exited 0 — 41 discovered, 39 passed, 2 skipped, zero failures/errors.
- Regression: elevated `mvn -q -pl services/content-service -am test` exited 0 — content 199 discovered, 158 passed, 41 skipped, zero failures/errors; shared common-security 8 passed. No old test expectations changed.
- Review corrections: new migration test scopes the ordering assertion to its own LOW/HIGH fixtures and asserts the result ends; Flyway target is pinned to 19 so future course seed migrations cannot conflict with fixture bands. Added adapter tests for concurrent code/band unique violations and propagation of unrelated integrity failures; no production changes after full-suite green.
- Final narrow check (run after these test corrections):

  ```powershell
  mvn -q -pl services/content-service -am test '-Dtest=CoursePersistenceTest,CourseMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'
  ```

  Exited 0 — `CoursePersistenceTest` 5 passed; `CourseMigrationTest` 2 skipped; zero failures/errors. Current combined XML evidence contains 201 content tests (160 passed, 41 skipped), reflecting the full run plus this narrow recheck; a full 201-test run was not performed.
- New skipped tests: `CourseMigrationTest.coursesHaveUniqueCodesAndHalfBandsAndDefaultActiveStatus` and `CourseMigrationTest.topicCourseMembershipIsOptionalButMustReferenceAnExistingCourse`. Schema creation, PostgreSQL constraints, topic FK/index, and actual PostgreSQL ordering were not exercised. Existing skipped classes: `BandRangeMigrationTest` (4), `CatalogRemovalMigrationTest` (1), `DemoReadingPassageSeedTest` (1), `KnowledgePointLearningTypeMigrationTest` (1), `LessonPipelineSeedTest` (32). Exact existing names appear in [baseline skip evidence](./reports/baseline-skipped-tests.json).
- Review: [phase review](./reports/phase-01-review.md); adapter tests cover named unique-constraint error translation while preserving other failures. `git diff --check` passed; source check confirmed no Spring/JPA/Hibernate imports in `Course`.
- Graph: `graphify update .` exited 0, 7864 nodes / 26885 edges; SQL extraction warning reports missing `tree_sitter_sql`. No dependency installed; generated graph artifacts remain uncommitted.
- Seed text: none added in this phase. Old expectations changed: none.

### Phase 2 Verification — 2026-10-07

- Status: `DONE_WITH_CONCERNS` — implementation and available tests complete; Docker-dependent PostgreSQL cases skipped. No `BLOCKED` condition. The approved gap before course seed/backfill remains: old seeded topics have no course and are excluded from the sequence until phase 3; the approved old sequence test expectation is deferred to that phase.
- Commit: `5926d93cda8a6b6c2e29c990ea851ff7e3aa8bce` — `feat(content): expose course sequences and final test packages`.
- Scope: new `V20__course_test_packages.sql`, `COURSE_TEST` enum with required course membership in aggregate/database, package course ID persistence, seeded-only API creation rejection, course metadata joined into topic sequence, internal course test-packages endpoint, and course-test inclusion in all three question exclusion/ownership/reservation type lists. Course tests use the existing ordered question locks, ownership and LEARNING-purpose checks, then require nonempty Reading objective CHOICE/FILL specifications before publication. Published version reader already accepts every package type; no existing package payload shape changed.
- TDD red: new tests were written before production edits. Elevated command below exited 1 at `testCompile` because `GetCourseTestPackagesUseCase` and `PackageQuestionSpec` did not yet exist:

  ```powershell
  mvn -q -pl services/content-service -am test '-Dtest=CoursePackagesUseCaseTest,ContentPackageCourseMappingTest,CourseSequenceIntegrationTest,InternalLearningContentControllerTest,ContentPackageControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false'
  ```

- Initial implementation recheck exposed a new-test route mistake: `ContentPackageControllerTest.creatingACourseTestThroughTheAuthorApiReturns400` targeted nonexistent `/api/content/admin/packages`. Corrected only this new test to the existing author-protected `/api/content/packages` route; the same focused command then exited 0 (27 discovered, 21 passed, 6 skipped). No old expectation or route changed.
- Review identified FILL Unicode-whitespace divergence from Assessment. Added `CoursePackagesUseCaseTest.fillAlternativesUseTheGradersUnicodeWhitespaceRule` before the fix; elevated `mvn -q -pl services/content-service -am test '-Dtest=CoursePackagesUseCaseTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 1 (7 tests, one failure): an NBSP-only accepted answer incorrectly published. FILL now uses the grader's Unicode whitespace replacement followed by trim; CHOICE keeps the grader's existing nonblank-string rule. Regression covers both NBSP-only rejection and valid text surrounded by NBSP. Empty course tests are also rejected.
- Final focused green: first command above exited 0 — 29 discovered, 23 passed, 6 skipped, zero failures/errors.
- Full regression: elevated `mvn -q -pl services/content-service -am test` exited 0 — content 217 discovered, 170 passed, 47 skipped, zero failures/errors; common-security 8 passed. Available old tests remained green. No old test expectations changed; `LessonPipelineSeedTest.topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints` remains untouched for the phase-3 backfill.
- New skipped PostgreSQL tests (Docker daemon unavailable): `CourseSequenceIntegrationTest.sequenceExcludesUnassignedInactiveAndLessonlessTopicsAndUsesCourseBandOrder`, `.courseTestFlagAndPackageQueryRequirePublishedPackagesAndCurrentVersions`, `.courseQuestionsCannotBeExposedAsPracticeAndAreReservedAcrossQuestionVersions`, `.questionSpecProjectionIncludesEssayAndMalformedObjectiveShapesForPublishValidation`, `.courseTestsRequireAnExistingCourseAndCannotOmitCourseMembership`, `.courseTestForeignKeyRejectsUnknownCourses`. Fixtures pin Flyway target 20 and rollback each test; future V21 seeds cannot conflict. Actual migration, SQL filtering/order, FK guards, package eligibility and cross-version ownership queries were not exercised.
- Existing skips: `BandRangeMigrationTest` (4), `CatalogRemovalMigrationTest` (1), `CourseMigrationTest` (2), `DemoReadingPassageSeedTest` (1), `KnowledgePointLearningTypeMigrationTest` (1), `LessonPipelineSeedTest` (32); see phase 1 and [baseline skip evidence](./reports/baseline-skipped-tests.json).
- Contract: [internal learning content API](../../docs/contracts/learning-content-internal-v1.md) dated 2026-10-07, ten routes, additive `course` metadata, active-course filtering/order, endpoint shape/errors, Reading objective publishing and practice/reservation exclusions. Application parses JSON; domain has no JSON/framework dependency. Fixed number of reader queries, with course metadata from the topic JOIN and no per-topic/per-question repository queries.
- Review: [phase review](./reports/phase-02-review.md); concrete FILL mismatch resolved and regression proved red/green. `git diff --check` passed. `graphify update .` exited 0 — 7920 nodes / 27077 edges; SQL parser still unavailable, no dependency installed, graph artifacts not staged.
- Seed text added: none. Existing migration files rewritten: none. Unrelated user document edits preserved.

### Phase 3 Verification — 2026-10-07

- Status: `DONE`; Docker-backed PostgreSQL tests executed successfully. The two expectation conflicts identified by source review were resolved exactly as newly authorized in the decision table. No `BLOCKED` condition remains.
- Commit subject: `feat(content): seed courses and higher-band reading`.
- Scope: additive `V21__seed_courses.sql`, two ACTIVE courses (`IELTS_5_5`, `IELTS_6_5`), all existing skilled topics backfilled to 5.5, higher Reading inference/paraphrase topic with two ACTIVE KPs, one published lesson with TEXT/KP mappings plus passage and EXERCISE blocks, two three-question lesson-attached practice sets, one four-question topic test, and two six-question Reading course tests. All 24 new questions are PUBLISHED, purpose LEARNING, have unique question/version IDs and one owner, and contain CHOICE/FILL answer specs. Six original fictional Reading passages are included. Earlier CourseMigrationTest and CourseSequenceIntegrationTest remain pinned to migrations 19 and 20; CourseSeedTest targets 21 without mutable fixtures. The internal Content README and learning-content contract document the seed and representative 6.5 responses.
- TDD red, before V21 existed: `mvn -q -pl services/content-service -am test '-Dtest=CourseSeedResourceTest,CourseSeedTest' '-Dsurefire.failIfNoSpecifiedTests=false'` — exit 1; 7 discovered, 3 failures, 0 errors, 4 skips. All runnable resource tests failed because the V21 resource was absent; PostgreSQL integration tests skipped, so there is no executed PostgreSQL red evidence.
- Focused pre-Docker green: the focused seed/legacy command exited 0; 40 discovered, 3 passed, 37 skipped, 0 failures/errors. It is superseded for execution evidence by the Docker-backed full run below.
- Docker check: `docker ps` succeeded; PostgreSQL/Testcontainers cases were enabled.
- Full command: `mvn -q -pl services/content-service -am test` — exit 0. Content Service: 224 tests, 224 passed, 0 failures, 0 errors, 0 skipped. This includes `CourseSeedTest` (4), `CourseMigrationTest` (2), `CourseSequenceIntegrationTest` (6), `LessonPipelineSeedTest` (32), and `CourseSeedResourceTest` (3); all executed. Shared common-security: 8 tests, 8 passed, 0 failures, 0 errors, 0 skipped. V19–V21 migration-backed tests ran against PostgreSQL/Testcontainers.
- Authorized old expectation changes: in `LessonPipelineSeedTest.lessonPracticeAndTestQuestionsAreReservedForLearning`, changed only `hasSize(147)` to `hasSize(171)`; in `LessonPipelineSeedTest.practiceSetsBelongToTheEarliestLessonTeachingTheirKnowledgePoint`, added exactly `R65-PS-INFERENCE -> R65-I1` and `R65-PS-PARAPHRASE -> R65-I1`. The separately allowlisted sequence expectation changes remain limited to `topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints`. No other old expectations or snapshots changed.
- New seed content: the complete SQL below is verbatim from `V21__seed_courses.sql`; no previously applied migration was rewritten. `git diff --check` passed. Review report: [phase 3 seed review](./reports/phase-03-review.md).

#### Full new seed content (verbatim V21 source)

The source below contains every new passage, theory block, stem, option, accepted answer and explanation exactly as preserved for review.

```sql
-- Original demo Reading materials: interpreting evidence and preserving meaning when paraphrasing.
INSERT INTO courses (id, code, name, band_level, status) VALUES ('30000000-0000-4000-8000-000000000001', 'IELTS_5_5', 'IELTS 5.5', 5.5, 'ACTIVE');
INSERT INTO courses (id, code, name, band_level, status) VALUES ('30000000-0000-4000-8000-000000000002', 'IELTS_6_5', 'IELTS 6.5', 6.5, 'ACTIVE');
UPDATE topics SET course_id = '30000000-0000-4000-8000-000000000001' WHERE skill IS NOT NULL;
INSERT INTO topics (id, code, name, sort_order, status, skill, course_id) VALUES ('28000000-0000-4000-8000-010000000001', 'READING_6_5_INFERENCE', 'Supported inference and paraphrase', 960, 'ACTIVE', 'READING', '30000000-0000-4000-8000-000000000002');
INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES ('28000000-0000-4000-8000-020000000001', '28000000-0000-4000-8000-010000000001', 'R65_EVIDENCE_INFERENCE', 'Supported inference', 'STRATEGY', 'PROCEDURE', 'READING', 'Distinguish conclusions supported by the text from plausible assumptions; preserve qualifications and limits.', 'ACTIVE');
INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES ('28000000-0000-4000-8000-020000000002', '28000000-0000-4000-8000-010000000001', 'R65_PARAPHRASE', 'Meaning-preserving paraphrase', 'STRATEGY', 'PROCEDURE', 'READING', 'Match restatements that preserve comparison, scope and certainty rather than merely repeating keywords.', 'ACTIVE');
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('28000000-0000-4000-8000-030000000001', '28000000-0000-4000-8000-010000000001', 'R65-I1', 'Drawing supported inferences', 1, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('28000000-0000-4000-8000-030000000001', '28000000-0000-4000-8000-020000000001');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('28000000-0000-4000-8000-030000000001', '28000000-0000-4000-8000-020000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('28000000-0000-4000-8000-040000000001', '28000000-0000-4000-8000-030000000001', 1, 'TEXT', 'A supported inference joins clues in the passage without adding outside facts. First identify the observation, then ask what it permits you to conclude. A rise after a change does not by itself prove that the change caused the rise. Preserve limits such as some, may, during the trial and among respondents. To check a paraphrase, compare who did what, the direction of any comparison and the strength of the claim. Similar words can hide a changed meaning; different words can express the same relationship. Reject an answer that turns a possibility into a certainty or a small sample into a universal rule.');
INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id) VALUES ('28000000-0000-4000-8000-040000000001', '28000000-0000-4000-8000-020000000001');
INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id) VALUES ('28000000-0000-4000-8000-040000000001', '28000000-0000-4000-8000-020000000002');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000001', 'PASSAGE', 'A. In a fictional town, a library extended its closing time from six to eight for a six-week trial. Evening visits increased, but total weekly visits changed little. Staff noticed that several regular afternoon visitors now came after work.

B. A questionnaire was offered only during the new evening hours. Most respondents praised the later closing time. The librarian described the replies as useful evidence about evening users, rather than a measure of every resident''s preferences.

C. The council agreed to continue the trial for another month before deciding whether to fund it permanently. It wanted attendance records from a period without school holidays, because the first trial overlapped with a holiday.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000002', 'PASSAGE', 'A. A fictional neighbourhood workshop lets residents bring broken household items to volunteer repairers. Last spring, the workshop added a booking system. Queues became shorter, although the number of completed repairs remained almost unchanged. Organisers suggested that appointments spread arrivals across the day.

B. Repairers recorded why some items could not be fixed. Missing replacement parts were the most frequent obstacle; lack of tools was mentioned less often. These records covered items brought to the workshop, not all broken items in the neighbourhood.

C. Organisers plan to contact suppliers about a small stock of common parts. They have not promised that this will increase successful repairs, since some products cannot be opened without damage.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000003', 'PASSAGE', 'A. In a fictional housing estate, residents converted an unused courtyard into a shared garden. Anyone living on the estate could join, but plots were allocated by lottery when applications exceeded the available space. A successful applicant received a plot for one season, not permanent ownership.

B. Members agreed to collect rainwater where possible. During a dry month, however, the tanks did not provide enough water, so members used the estate water supply. The rule was intended to reduce demand on that supply, rather than to prohibit its use.

C. At the end of the season, members reported that they had met neighbours they previously knew only by sight. The coordinator called this an unexpected benefit: the project had originally been proposed to make productive use of the courtyard.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000004', 'PASSAGE', 'A. A fictional local archive scanned a selection of fragile letters and offered digital copies to visitors. Requests to handle those originals declined during the pilot, while requests to see other documents stayed steady. The archivist thought the copies might reduce wear on the selected letters, but said a longer observation period was needed.

B. Volunteers chose letters whose handwriting was relatively clear. They transcribed the text and left uncertain words marked with a question symbol. The archive preferred visible uncertainty to a confident guess that could mislead future readers.

C. Online users could search the transcriptions, but they were reminded to consult the images before quoting a difficult phrase. The pilot made the collection easier to explore; it did not remove the need for careful interpretation. Funding for a wider project had not yet been approved.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000005', 'PASSAGE', 'A. A fictional community centre lends tools to local residents. Members pay a small annual fee and may borrow one tool at a time. The centre started the service because many household jobs need equipment that is used only occasionally.

B. Volunteers check every tool when it is returned. A damaged tool is removed from the shelves until it has been repaired. Members also receive a short safety guide with each loan.

C. The centre opens on Wednesday evenings and Saturday mornings. It does not offer home delivery. The coordinator hopes to add more gardening tools next year, but the decision depends on the budget.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('28000000-0000-4000-8000-050000000006', 'PASSAGE', 'A. A fictional college introduced a shuttle between its campus and the station for an eight-week experiment. The shuttle ran only on weekdays. A count at the campus gate showed fewer cars on the days measured than in the previous term, but the previous count had been made during a different teaching timetable. The transport team warned against attributing the whole difference to the shuttle.

B. A survey invited shuttle passengers to describe their journeys. Several said they had previously walked from the station; others had driven all the way to campus. The team therefore distinguished between passengers who changed their route and those who changed their main mode of travel.

C. Morning services were usually full, whereas the last afternoon service often had spare seats. The college proposed moving one afternoon departure to the morning, keeping the total number of daily trips unchanged. This adjustment was intended to better match demand, rather than expand capacity overall.

D. The trial report recommended another term of monitoring before a permanent contract. It argued that the service appeared promising, while recognising that passenger responses could not reveal the preferences of people who had never used it.', 'VALID');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('28000000-0000-4000-8000-040000000002', '28000000-0000-4000-8000-030000000001', 2, 'ASSET', '28000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('28000000-0000-4000-8000-040000000003', '28000000-0000-4000-8000-030000000001', 3, 'EXERCISE');
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000001', '28000000-0000-4000-8000-060000000001', 1, 'What can reasonably be inferred from paragraph A of The library trial?', '[{"optionKey": "A", "content": "The trial attracted many new weekly visitors.", "sortOrder": 1}, {"optionKey": "B", "content": "Some visits shifted from afternoon to evening.", "sortOrder": 2}, {"optionKey": "C", "content": "Every visitor preferred the later hours.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'Total weekly visits changed little and regular afternoon users appeared in the evening, supporting a shift in timing rather than a large rise in users.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000001' WHERE id = '28000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000001', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('28000000-0000-4000-8000-040000000003', '28000000-0000-4000-8000-070000000001', 1);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000002', '28000000-0000-4000-8000-060000000002', 1, 'Which restatement best preserves the meaning of the librarian''s view in paragraph B?', '[{"optionKey": "A", "content": "The questionnaire established the preferences of the whole town.", "sortOrder": 1}, {"optionKey": "B", "content": "The replies were too unreliable to be useful.", "sortOrder": 2}, {"optionKey": "C", "content": "The replies helped describe evening users but did not represent every resident.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'The librarian valued the replies while limiting the group they described. C preserves both usefulness and scope.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000002' WHERE id = '28000000-0000-4000-8000-060000000002';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000002', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('28000000-0000-4000-8000-040000000003', '28000000-0000-4000-8000-070000000002', 2);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000003', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000003', '28000000-0000-4000-8000-060000000003', 1, 'Why might shorter queues in The repair workshop not mean that more items were repaired?', '[{"optionKey": "A", "content": "Appointments could spread arrivals without changing the number completed.", "sortOrder": 1}, {"optionKey": "B", "content": "The passage says volunteers stopped repairing items.", "sortOrder": 2}, {"optionKey": "C", "content": "Short queues prove that residents no longer needed repairs.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'The repair count remained almost unchanged; spreading arrivals explains reduced queues without assuming increased output.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000003' WHERE id = '28000000-0000-4000-8000-060000000003';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000003', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000004', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000004', '28000000-0000-4000-8000-060000000004', 1, 'What is the strongest conclusion supported by the records in paragraph B?', '[{"optionKey": "A", "content": "Every broken item in the neighbourhood lacks replacement parts.", "sortOrder": 1}, {"optionKey": "B", "content": "For items brought in, missing parts were a more common obstacle than missing tools.", "sortOrder": 2}, {"optionKey": "C", "content": "Tools were never a problem for any repairer.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'The comparison applies to recorded workshop items. It neither covers every local item nor says that tool problems never occurred.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000004' WHERE id = '28000000-0000-4000-8000-060000000004';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000004', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000005', 'FILL_IN_BLANK', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000005', '28000000-0000-4000-8000-060000000005', 1, 'Complete with ONE WORD from paragraph C: some products cannot be opened without ______.', NULL, '{"type": "FILL", "accepted": ["damage"]}', 1, 'The last sentence explicitly limits repair prospects because opening some products causes damage.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000005' WHERE id = '28000000-0000-4000-8000-060000000005';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000005', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000006', '28000000-0000-4000-8000-060000000006', 1, 'Which paraphrase matches the allocation rule in The shared garden?', '[{"optionKey": "A", "content": "Applicants were given plots permanently in application order.", "sortOrder": 1}, {"optionKey": "B", "content": "Every resident was guaranteed a plot whenever they applied.", "sortOrder": 2}, {"optionKey": "C", "content": "When demand exceeded space, chance determined who received a plot for a season.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'Lottery means selection by chance; the original rule applies when oversubscribed and grants only one season.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000006' WHERE id = '28000000-0000-4000-8000-060000000006';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000006', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000007', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000007', '28000000-0000-4000-8000-060000000007', 1, 'Which statement accurately restates the rainwater agreement?', '[{"optionKey": "A", "content": "Members aimed to use less estate water but could use it when necessary.", "sortOrder": 1}, {"optionKey": "B", "content": "Members were forbidden from using estate water during dry weather.", "sortOrder": 2}, {"optionKey": "C", "content": "Rainwater always met the entire garden demand.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'The agreement sought reduction, not prohibition. The dry-month example confirms that the estate supply remained available.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000007' WHERE id = '28000000-0000-4000-8000-060000000007';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000007', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000008', 'FILL_IN_BLANK', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000008', '28000000-0000-4000-8000-060000000008', 1, 'Complete with ONE WORD from paragraph C: meeting neighbours was an ______ benefit.', NULL, '{"type": "FILL", "accepted": ["unexpected"]}', 1, 'The coordinator describes this benefit as unexpected because the original purpose concerned the unused courtyard.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000008' WHERE id = '28000000-0000-4000-8000-060000000008';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000008', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000009', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000009', '28000000-0000-4000-8000-060000000009', 1, 'What inference is supported by the change in requests during The archive pilot?', '[{"optionKey": "A", "content": "All visitors stopped consulting original documents.", "sortOrder": 1}, {"optionKey": "B", "content": "Digital copies may have reduced handling of the selected letters.", "sortOrder": 2}, {"optionKey": "C", "content": "Scanning proved that no letters would ever deteriorate.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'Requests for the selected originals declined. The text supports a possible reduction in handling, with uncertainty and limited scope.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000009' WHERE id = '28000000-0000-4000-8000-060000000009';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000009', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000010', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000010', '28000000-0000-4000-8000-060000000010', 1, 'Why did volunteers mark uncertain words?', '[{"optionKey": "A", "content": "To make the letters impossible to search.", "sortOrder": 1}, {"optionKey": "B", "content": "Because the archive banned all transcriptions.", "sortOrder": 2}, {"optionKey": "C", "content": "To show the limits of their interpretation rather than conceal a guess.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'Visible uncertainty avoids presenting an unsupported reading as certain; it does not imply that transcription or search was abandoned.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000010' WHERE id = '28000000-0000-4000-8000-060000000010';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000010', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000011', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000011', '28000000-0000-4000-8000-060000000011', 1, 'Which paraphrase matches the advice to online users in paragraph C?', '[{"optionKey": "A", "content": "Searchable text helps exploration, but difficult quotations should be checked against the images.", "sortOrder": 1}, {"optionKey": "B", "content": "The transcription is guaranteed to be correct and needs no checking.", "sortOrder": 2}, {"optionKey": "C", "content": "Only physical visitors can inspect the collection.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'A retains both improved access and the requirement to verify a difficult phrase using the images.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000011' WHERE id = '28000000-0000-4000-8000-060000000011';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000011', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000012', 'FILL_IN_BLANK', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000012', '28000000-0000-4000-8000-060000000012', 1, 'Complete with ONE WORD from paragraph C: wider project funding had not yet been ______.', NULL, '{"type": "FILL", "accepted": ["approved"]}', 1, 'The final sentence states that approval was pending; easier access did not establish that expansion had been funded.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000012' WHERE id = '28000000-0000-4000-8000-060000000012';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000012', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000013', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000013', '28000000-0000-4000-8000-060000000013', 1, 'What is the main idea of A community tool library?', '[{"optionKey": "A", "content": "Residents can borrow occasionally needed tools through a managed local service.", "sortOrder": 1}, {"optionKey": "B", "content": "The centre delivers tools to every home for free.", "sortOrder": 2}, {"optionKey": "C", "content": "Gardening tools are the only equipment that can be borrowed.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'The passage explains the service, its borrowing rules and checks. B contradicts the fee and delivery details; C is unsupported.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000013' WHERE id = '28000000-0000-4000-8000-060000000013';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000013', '10000000-0000-4000-8000-000000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000014', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000014', '28000000-0000-4000-8000-060000000014', 1, 'Which sentence states the main idea of paragraph B?', '[{"optionKey": "A", "content": "Members also receive a short safety guide with each loan.", "sortOrder": 1}, {"optionKey": "B", "content": "Volunteers check every tool when it is returned.", "sortOrder": 2}, {"optionKey": "C", "content": "A damaged tool is removed from the shelves until it has been repaired.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'The first sentence establishes the return-check procedure; removal of damaged tools is one consequence and the guide is an additional safety measure.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000014' WHERE id = '28000000-0000-4000-8000-060000000014';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000014', '20000000-0000-4000-8000-020000000003', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000015', 'FILL_IN_BLANK', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000015', '28000000-0000-4000-8000-060000000015', 1, 'Complete with ONE WORD from paragraph A: members pay a small ______ fee.', NULL, '{"type": "FILL", "accepted": ["annual"]}', 1, 'Paragraph A states that the membership fee is annual, so it is paid yearly.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000015' WHERE id = '28000000-0000-4000-8000-060000000015';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000015', '10000000-0000-4000-8000-000000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000016', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000016', '28000000-0000-4000-8000-060000000016', 1, 'According to the passage, when is the centre open?', '[{"optionKey": "A", "content": "Every weekday morning.", "sortOrder": 1}, {"optionKey": "B", "content": "Wednesday mornings and Sunday evenings.", "sortOrder": 2}, {"optionKey": "C", "content": "Wednesday evenings and Saturday mornings.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'Paragraph C gives precisely Wednesday evenings and Saturday mornings.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000016' WHERE id = '28000000-0000-4000-8000-060000000016';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000016', '20000000-0000-4000-8000-020000000003', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000017', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000017', '28000000-0000-4000-8000-060000000017', 1, 'Which statement is supported by paragraph B?', '[{"optionKey": "A", "content": "Damaged tools remain unavailable until repaired.", "sortOrder": 1}, {"optionKey": "B", "content": "Damaged tools are lent at a reduced fee.", "sortOrder": 2}, {"optionKey": "C", "content": "Members repair every damaged tool themselves.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'A directly restates removal from the shelves until repair. The passage does not describe discounted loans or member repairs.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000017' WHERE id = '28000000-0000-4000-8000-060000000017';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000017', '20000000-0000-4000-8000-020000000005', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000018', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000018', '28000000-0000-4000-8000-060000000018', 1, 'What does the passage say about more gardening tools?', '[{"optionKey": "A", "content": "Their purchase has already been completed.", "sortOrder": 1}, {"optionKey": "B", "content": "Adding them is a hope that depends on the budget.", "sortOrder": 2}, {"optionKey": "C", "content": "They will definitely be added regardless of cost.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'The coordinator hopes to expand the stock, but the budget condition means that expansion is not certain.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000018' WHERE id = '28000000-0000-4000-8000-060000000018';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000018', '20000000-0000-4000-8000-020000000005', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000019', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000019', '28000000-0000-4000-8000-060000000019', 1, 'Why is the lower car count insufficient to prove the shuttle caused the whole decrease?', '[{"optionKey": "A", "content": "The shuttle did not run during the experiment.", "sortOrder": 1}, {"optionKey": "B", "content": "The team measured only shuttle passengers and never cars.", "sortOrder": 2}, {"optionKey": "C", "content": "The earlier count used a different teaching timetable, so the comparison has a limitation.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'Paragraph A identifies a timetable difference that could affect the counts. It therefore limits a causal conclusion rather than disproving any benefit.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000019' WHERE id = '28000000-0000-4000-8000-060000000019';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000019', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000020', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000020', '28000000-0000-4000-8000-060000000020', 1, 'What can be inferred about the shuttle passengers surveyed?', '[{"optionKey": "A", "content": "Not all of them had switched from driving to campus.", "sortOrder": 1}, {"optionKey": "B", "content": "Every one of them had previously driven.", "sortOrder": 2}, {"optionKey": "C", "content": "None of them used the station.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'Some previously walked from the station, while others drove all the way. The sample includes different previous modes.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000020' WHERE id = '28000000-0000-4000-8000-060000000020';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000020', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000021', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000021', '28000000-0000-4000-8000-060000000021', 1, 'Which restatement matches the proposed timetable adjustment?', '[{"optionKey": "A", "content": "The college would increase the total number of daily trips.", "sortOrder": 1}, {"optionKey": "B", "content": "One departure would move to a busier period without adding daily trips.", "sortOrder": 2}, {"optionKey": "C", "content": "All afternoon departures would be cancelled.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "B"}', 1, 'Moving one trip changes its timing while maintaining the total. B preserves both parts of the proposal.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000021' WHERE id = '28000000-0000-4000-8000-060000000021';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000021', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000022', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000022', '28000000-0000-4000-8000-060000000022', 1, 'Which phrase best captures the report''s overall conclusion?', '[{"optionKey": "A", "content": "The shuttle has conclusively solved college traffic.", "sortOrder": 1}, {"optionKey": "B", "content": "The experiment should end because no passenger benefited.", "sortOrder": 2}, {"optionKey": "C", "content": "Early signs are encouraging, but further evidence is needed before a permanent commitment.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "C"}', 1, 'The report calls the service promising and recommends another monitoring term, combining cautious optimism with a request for more evidence.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000022' WHERE id = '28000000-0000-4000-8000-060000000022';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000022', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000023', 'FILL_IN_BLANK', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000023', '28000000-0000-4000-8000-060000000023', 1, 'Complete with ONE WORD from paragraph C: the last afternoon service often had spare ______.', NULL, '{"type": "FILL", "accepted": ["seats"]}', 1, 'Spare seats are the stated sign of lower demand on the last afternoon service.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000023' WHERE id = '28000000-0000-4000-8000-060000000023';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000023', '28000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES ('28000000-0000-4000-8000-060000000024', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES ('28000000-0000-4000-8000-070000000024', '28000000-0000-4000-8000-060000000024', 1, 'Which claim would go beyond the survey evidence described in paragraph D?', '[{"optionKey": "A", "content": "The responses show what every college traveller wants.", "sortOrder": 1}, {"optionKey": "B", "content": "The responses describe the people who used the shuttle.", "sortOrder": 2}, {"optionKey": "C", "content": "People who never used it may have preferences the survey cannot reveal.", "sortOrder": 3}]', '{"type": "CHOICE", "correct": "A"}', 1, 'The report explicitly says passenger replies cannot reveal non-user preferences. A incorrectly generalises from users to every traveller.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '28000000-0000-4000-8000-070000000024' WHERE id = '28000000-0000-4000-8000-060000000024';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('28000000-0000-4000-8000-070000000024', '28000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key) VALUES ('28000000-0000-4000-8000-080000000001', 'R65-PS-INFERENCE', 'Repair workshop: supported inference', 'PRACTICE_SET', NULL, '28000000-0000-4000-8000-030000000001', NULL, 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version) VALUES ('28000000-0000-4000-8000-090000000001', '28000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}', 1);
UPDATE content_package_versions SET published_at = CURRENT_TIMESTAMP WHERE id = '28000000-0000-4000-8000-090000000001';
UPDATE content_packages SET current_published_version_id = '28000000-0000-4000-8000-090000000001' WHERE id = '28000000-0000-4000-8000-080000000001';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('28000000-0000-4000-8000-0a0000000001', '28000000-0000-4000-8000-090000000001', 'The repair workshop', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('28000000-0000-4000-8000-0c0000000001', '28000000-0000-4000-8000-050000000002', '28000000-0000-4000-8000-0a0000000001', 0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000003', '28000000-0000-4000-8000-0a0000000001', '28000000-0000-4000-8000-070000000003', 1, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000004', '28000000-0000-4000-8000-0a0000000001', '28000000-0000-4000-8000-070000000004', 2, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000005', '28000000-0000-4000-8000-0a0000000001', '28000000-0000-4000-8000-070000000005', 3, 1.0);
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key) VALUES ('28000000-0000-4000-8000-080000000002', 'R65-PS-PARAPHRASE', 'Shared garden: paraphrase', 'PRACTICE_SET', NULL, '28000000-0000-4000-8000-030000000001', NULL, 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version) VALUES ('28000000-0000-4000-8000-090000000002', '28000000-0000-4000-8000-080000000002', 1, 'PUBLISHED', '{}', 1);
UPDATE content_package_versions SET published_at = CURRENT_TIMESTAMP WHERE id = '28000000-0000-4000-8000-090000000002';
UPDATE content_packages SET current_published_version_id = '28000000-0000-4000-8000-090000000002' WHERE id = '28000000-0000-4000-8000-080000000002';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('28000000-0000-4000-8000-0a0000000002', '28000000-0000-4000-8000-090000000002', 'The shared garden', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('28000000-0000-4000-8000-0c0000000002', '28000000-0000-4000-8000-050000000003', '28000000-0000-4000-8000-0a0000000002', 0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000006', '28000000-0000-4000-8000-0a0000000002', '28000000-0000-4000-8000-070000000006', 1, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000007', '28000000-0000-4000-8000-0a0000000002', '28000000-0000-4000-8000-070000000007', 2, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000008', '28000000-0000-4000-8000-0a0000000002', '28000000-0000-4000-8000-070000000008', 3, 1.0);
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key) VALUES ('28000000-0000-4000-8000-080000000003', 'R65-TOPIC-TEST', 'Archive pilot: inference and paraphrase', 'TOPIC_TEST', '28000000-0000-4000-8000-010000000001', NULL, NULL, 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version) VALUES ('28000000-0000-4000-8000-090000000003', '28000000-0000-4000-8000-080000000003', 1, 'PUBLISHED', '{}', 1);
UPDATE content_package_versions SET published_at = CURRENT_TIMESTAMP WHERE id = '28000000-0000-4000-8000-090000000003';
UPDATE content_packages SET current_published_version_id = '28000000-0000-4000-8000-090000000003' WHERE id = '28000000-0000-4000-8000-080000000003';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('28000000-0000-4000-8000-0a0000000003', '28000000-0000-4000-8000-090000000003', 'The archive pilot', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('28000000-0000-4000-8000-0c0000000003', '28000000-0000-4000-8000-050000000004', '28000000-0000-4000-8000-0a0000000003', 0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000009', '28000000-0000-4000-8000-0a0000000003', '28000000-0000-4000-8000-070000000009', 1, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000010', '28000000-0000-4000-8000-0a0000000003', '28000000-0000-4000-8000-070000000010', 2, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000011', '28000000-0000-4000-8000-0a0000000003', '28000000-0000-4000-8000-070000000011', 3, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000012', '28000000-0000-4000-8000-0a0000000003', '28000000-0000-4000-8000-070000000012', 4, 1.0);
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key) VALUES ('28000000-0000-4000-8000-080000000004', 'COURSE-5_5-READING', 'IELTS 5.5 Reading final', 'COURSE_TEST', NULL, NULL, '30000000-0000-4000-8000-000000000001', 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version) VALUES ('28000000-0000-4000-8000-090000000004', '28000000-0000-4000-8000-080000000004', 1, 'PUBLISHED', '{}', 1);
UPDATE content_package_versions SET published_at = CURRENT_TIMESTAMP WHERE id = '28000000-0000-4000-8000-090000000004';
UPDATE content_packages SET current_published_version_id = '28000000-0000-4000-8000-090000000004' WHERE id = '28000000-0000-4000-8000-080000000004';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-090000000004', 'A community tool library', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('28000000-0000-4000-8000-0c0000000004', '28000000-0000-4000-8000-050000000005', '28000000-0000-4000-8000-0a0000000004', 0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000013', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000013', 1, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000014', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000014', 2, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000015', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000015', 3, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000016', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000016', 4, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000017', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000017', 5, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000018', '28000000-0000-4000-8000-0a0000000004', '28000000-0000-4000-8000-070000000018', 6, 1.0);
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key) VALUES ('28000000-0000-4000-8000-080000000005', 'COURSE-6_5-READING', 'IELTS 6.5 Reading final', 'COURSE_TEST', NULL, NULL, '30000000-0000-4000-8000-000000000002', 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version) VALUES ('28000000-0000-4000-8000-090000000005', '28000000-0000-4000-8000-080000000005', 1, 'PUBLISHED', '{}', 1);
UPDATE content_package_versions SET published_at = CURRENT_TIMESTAMP WHERE id = '28000000-0000-4000-8000-090000000005';
UPDATE content_packages SET current_published_version_id = '28000000-0000-4000-8000-090000000005' WHERE id = '28000000-0000-4000-8000-080000000005';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-090000000005', 'The shuttle experiment', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('28000000-0000-4000-8000-0c0000000005', '28000000-0000-4000-8000-050000000006', '28000000-0000-4000-8000-0a0000000005', 0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000019', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000019', 1, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000020', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000020', 2, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000021', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000021', 3, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000022', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000022', 4, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000023', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000023', 5, 1.0);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('28000000-0000-4000-8000-0b0000000024', '28000000-0000-4000-8000-0a0000000005', '28000000-0000-4000-8000-070000000024', 6, 1.0);
```

### Phase 5 Verification — 2026-10-07 (partial, held)

- Status: `BLOCKED` by the phase-3 allowlist conflicts above. Placement work began independently while the content seed was authored; the required commit order was preserved. No phase-3 or phase-5 commit was created.
- TDD red: before production implementation, `mvn -q -pl services/learning-service -am test '-Dtest=BandLevelTest,LearnerPlacementTest,AssessmentCompletedParserTest,AssessmentResultIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 1 at `testCompile` because the new `BandLevel` type did not exist. No learning tests executed in this run.
- Partial implementation: V6 learner placements, pure band value object and placement aggregate, repository/JDBC adapter, optional band parsing, and placement recording within the existing consumer transaction. Old `AssessmentResult` constructor compatibility is retained. Source and new tests remain uncommitted and unverified after implementation.
- Held checks: no green run, post-implementation compile, full learning suite, persistence execution, or phase-5 code review. All implementation/testing stopped when the two phase-3 conflicts were confirmed. Do not reuse baseline Surefire reports as evidence for the partial implementation.
- Old expectations changed: none. Seed content: none in learning. No real LLM calls.
- Detailed evidence and file inventory: [placement verification draft](./reports/phase-05-verification-draft.md).

### Phase 5 Verification — 2026-10-07

- Status: `DONE`; phase 5 is committed after the full Docker-backed suite passed.
- Commit subject: `feat(learning): record placement band recommendations`.
- TDD red before production implementation: the focused compile failed because `BandLevel` did not exist yet; 0 tests executed, 0 skipped. This is expected red evidence.
- Focused green: `mvn -q -pl services/learning-service -am test '-Dtest=BandLevelTest,LearnerPlacementTest,ApplyPlacementResultUseCaseTest,AssessmentCompletedParserTest,AssessmentResultIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` — exit 0; 19 selected Learning tests passed, 0 failures, 0 errors, 0 skipped. PostgreSQL/Testcontainers executed `AssessmentResultIntegrationTest` and applied Flyway V1–V6.
- Full command: `mvn -q -pl services/learning-service -am test` — exit 0. Learning Service: 228 tests, 228 passed, 0 failures, 0 errors, 0 skipped. Shared `common-security`: 8 tests, 8 passed, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers ran; the application context test executed successfully.
- Authorized legacy expectation change in `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth`: Flyway version `5 → 6`; table count `14 → 15`; health check unchanged. SQL count: `14` baseline tables + `1` table created by this plan's learning migration V6 (`learner_placements`) = `15` tables.
- No other expectation was changed. No real LLM calls. Test contexts logged RabbitMQ connection retries because no broker was running; no test was skipped for that reason.

### Plan status reconciliation — 2026-10-07

- Phases 1, 2, 3 and 5 are complete. Phases 6, 4 and 7 remain pending in the requested commit order. Overall plan status remains `in-progress`.
- Confirmed commit order on `feat/course-band-levels`: `d867758`, `5926d93`, `6935369`, followed by phase 5. Phase 6 is next; phase 4 must follow phase 6.
- The authorized phase-3 seed expectation changes and phase-5 migration-count update pass against PostgreSQL/Testcontainers. The phase-5 health check is unchanged.

### Phase 6 Verification — 2026-10-07

- Status: `DONE`; phase 6 is verified and committed after the full Docker-backed learning suite passed.
- TDD red: the focused compile failed before implementation because the new course-domain and result types did not exist; 0 tests executed, 0 skipped. The first Docker-backed focused run then exposed fixture cleanup and recommendation-index mistakes in the new integration test; both were corrected. The specifically allowlisted `ReviewAndTestAssignmentIntegrationTest.skillTracksKeepReviewsLocalAndPassWritingWithoutATest` stub was updated with distinct course metadata so the old assertions continued to verify skill-local reviews.
- Full command: `mvn -q -pl services/learning-service -am test` exited 0. Learning Service: 238 executed, 238 passed, 0 failures, 0 errors, 0 skipped. Shared `common-security`: 8 executed, 8 passed, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers executed PostgreSQL integration cases, including `CoursePathIntegrationTest`, `ReviewAndTestAssignmentIntegrationTest`, and `LearningServiceApplicationTests`.
- Final focused recheck after the course metadata aggregation cleanup: `CoursePathIntegrationTest` ran 3 tests, 3 passed, 0 failures, 0 errors, 0 skipped.
- Authorized `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` change: Flyway version `6 -> 7`; table count `15 -> 17`; public health assertion unchanged. SQL count: baseline `14` + V6 `learner_placements` (1 table) + V7 `course_progress` and `course_test_assignments` (2 tables) = `17`.
- No other old expectation changed. `git diff --check` passed. No real LLM calls. Commit subject: `feat(learning): add course learning paths and tests`.

### Phase 4 Verification — 2026-10-07

- Status: `DONE`; phase 4 is verified and committed after the full Docker-backed assessment suite passed.
- TDD red: the initial compile failed because `AttemptType.COURSE_GATE` did not exist; 0 tests executed, 0 skipped. After adding the enum mapping but before V5, PostgreSQL rejected `COURSE_GATE` under the original generated CHECK constraint as expected (10 selected tests, 9 passed, 1 expected error, 0 skipped).
- Focused green: `mvn -q -pl services/assessment-service -am test '-Dtest=AssessmentAttemptTest,AutoGradingIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 0: 10 tests executed and passed, 0 failures, 0 errors, 0 skipped. Testcontainers exercised the new migration and persisted attempt; the test asserted `assessment_type=COURSE_GATE` in the emitted event JSON.
- Full command: `mvn -q -pl services/assessment-service -am test` exited 0. Assessment Service: 118 executed, 118 passed, 0 failures, 0 errors, 0 skipped. Shared `common-security`: 8 executed, 8 passed, 0 failures, 0 errors, 0 skipped. PostgreSQL/Testcontainers schema and outbox tests executed.
- Updated `docs/contracts/assessment-completed-v2.md` and the Assessment README to define the new event type and deploy order. No existing test expectation changed. `git diff --check` passed. Commit subject: `feat(assessment): support course gate attempts`.
