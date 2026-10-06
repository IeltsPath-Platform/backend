---
title: "Course theo band, gợi ý course từ placement và thi cuối course"
description: "Thêm Course (bậc band) nhóm topic; mọi course mở, mỗi course là một chuỗi riêng; placement chỉ gợi ý course; thi cuối course là mốc không chặn."
status: pending
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
| 3 | [Content seed courses](./phase-03-content-seed-courses.md) | Pending |
| 4 | [Assessment course gate](./phase-04-assessment-course-gate.md) | Pending |
| 5 | [Learning placement recommendation](./phase-05-learning-learner-level.md) | Pending |
| 6 | [Learning course path and course test](./phase-06-learning-course-path-and-course-test.md) | Pending |
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
- Commit subject: `feat(content): expose course sequences and final test packages` (hash recorded in next verification entry).
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
