---
title: KP con theo mã lỗi để cá nhân hóa bài ôn
description: >-
  Mỗi mã lỗi có một KP con (kỹ năng tránh bẫy) dưới KP cha; KP con nhận bằng
  chứng theo đáp án đã chọn; bài ôn luôn là của KP cha và chọn bộ theo bẫy của
  KP con yếu nhất; KP con yếu khi KP cha chưa có bài ôn tạo bài ôn tùy chọn cho
  KP cha; phản hồi fixHint khi chọn sai.
status: completed
priority: P2
effort: 11d
branch: feat/content-llm-shares-learning-config
tags:
  - content-service
  - learning-service
  - frontend
  - mastery
  - review
  - error-taxonomy
  - tdd
blockedBy: []
blocks: []
created: '2026-10-24T10:20:59.756Z'
createdBy: 'ck:plan'
source: skill
---

# KP con theo mã lỗi để cá nhân hóa bài ôn

## Overview

Hai học viên cùng yếu một KP hiện nhận cùng kiểu bài ôn dù sai theo hai kiểu khác nhau; mã lỗi chỉ được ghi lại và hiển
thị. Plan này cho mã lỗi chảy vào KP: mỗi mã lỗi có một **KP con** (kỹ năng tránh bẫy đó) dưới KP cha, nhận bằng chứng
theo **đáp án học viên đã chọn**, và dùng lại toàn bộ mastery MP-1.0, giỏ ôn, bộ luyện hiện có. Làm một lần; phase chia
theo phụ thuộc kỹ thuật, không chia tính năng.

Nguồn: [báo cáo brainstorm](../reports/brainstorm-261024-0135-error-code-personalized-review-report.md) (đã validate).

## Quyết định đã chốt

| # | Quyết định |
| --- | --- |
| D1 | KP con theo mã lỗi: mỗi mã ACTIVE (kể cả `DRAFT`, đánh dấu chờ duyệt) có KP cha → một KP con. Không gắn câu vào KP con. |
| D2 | Bằng chứng KP con, với mỗi bẫy X trong câu: chọn đúng → ✓; chọn bẫy X → ✗; chọn bẫy khác/đáp án sai không mã → không ghi. Trọng số = một nửa bằng chứng luyện tập. KP cha ghi như cũ. |
| D3 | Lỗi chính của KP cha = KP con yếu nhất đã đủ dữ liệu MP-1.0. Không có ngưỡng tỷ lệ riêng. |
| D4 | Chọn bộ: mọi bài ôn KP cha (chặn, giỏ ôn, `ERROR_PATTERN`) xếp số câu bẫy lỗi chính → tỷ lệ → thứ tự hiện tại, chỉ khi có bộ ≥ 2 câu bẫy. Luyện tự do giữ nguyên. |
| D5 | **Đơn vị ôn luôn là KP cha.** KP con không có bài ôn riêng, không vào giỏ ôn; chỉ nhận bằng chứng, quyết định lỗi chính (chọn bộ) và hiển thị lồng. |
| D6 | Trigger mới `ERROR_PATTERN` (tùy chọn, không chặn) tạo lúc chấm một bài ôn **cho KP cha** khi: KP con vừa nhận bằng chứng **đang** `PRIORITY`; KP cha chưa có bài ôn chờ; KP cha còn bộ ≥ 2 câu chứa bẫy đó; không vượt giới hạn mỗi lần nộp. Các trigger bài ôn KP cha hiện có giữ nguyên. |
| D7 | Lesson của bài ôn `ERROR_PATTERN`: lesson của bài vừa làm, hoặc lesson đã hoàn thành dạy KP cha; không có thì không tạo. Bài ôn KP cha có lỗi chính trả tên lỗi + `fixHint` khi mở bài và khi quay lại lý thuyết. |
| D8 | Phản hồi tên lỗi + `fixHint` khi chọn đáp án sai có mã. |
| D9 | Không áp cho bài thi (consumer RabbitMQ); không đổi `AssessmentCompleted.v2`. KP con không có tiên quyết, không vào chẩn đoán dò ngược. Câu đúng `TRUE`/`YES` chưa có mã. |
| D10 | Frontend nằm trong phạm vi. |

### Chỉnh so với báo cáo khi lập plan (người dùng xác nhận cả bốn, 2026-10-24)

| # | Báo cáo ghi | Plan làm | Lý do |
| --- | --- | --- | --- |
| P1 | Trỏ `error_types.knowledge_point_id` về KP con; thêm cột KP cha vào `knowledge_points` | Giữ `error_types.knowledge_point_id` là **KP cha**; thêm `error_types.trap_knowledge_point_id` (KP con, unique). KP cha của KP con suy ra từ dòng mã lỗi | Cột cũ đang được Writing dùng (`LINKER_MISUSE` → `DEMO_WRITING_LINKING`, `EssayKnowledgeJudge`, plan 261021-2016) và màn mã lỗi của author (plan 261023-0050); trỏ lại sẽ làm hỏng cả hai. Bớt một thay đổi schema |
| P2 | Luật T/F/NG trong `ErrorTagger` (learning) | Ghi bẫy T/F/NG thẳng vào `answer_spec.distractors` ở content (migration cho câu cũ, tự điền khi lưu câu mới); `ErrorTagger` không đổi | Một nguồn cho cả learning (gắn mã, bằng chứng) và content (SQL đếm câu bẫy); tránh viết luật hai lần bằng Java và SQL |
| P3 | Không nói KP con nào được tạo | Tạo KP con cho mọi mã ACTIVE có KP cha; báo cáo/cây chỉ trả KP con đã có bằng chứng | Không phải đồng bộ lại khi câu hỏi đổi `distractors` |
| P4 | "Một nửa bằng chứng chính cùng nguồn" | Một nguồn mới `error_trap`, trọng số mặc định 0.3 (= 0.5 × luyện tập 0.6), không tính vào trần 3 bài luyện | MP-1.0 gán trọng số theo nguồn; toàn bộ bằng chứng KP con cùng một nguồn nên trần luyện tập sẽ bóp KP con còn ≤ 3 bằng chứng |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Baseline data and characterization tests](./phase-01-baseline-data-and-characterization-tests.md) | Completed |
| 2 | [Content schema and trap knowledge points](./phase-02-content-schema-and-trap-knowledge-points.md) | Completed |
| 3 | [Content trap logic and set search](./phase-03-content-trap-logic-and-set-search.md) | Completed |
| 4 | [Learning trap evidence](./phase-04-learning-trap-evidence.md) | Completed |
| 5 | [Learning review selection and trap reviews](./phase-05-learning-review-selection-and-trap-reviews.md) | Completed |
| 6 | [Learning feedback and report](./phase-06-learning-feedback-and-report.md) | Completed |
| 7 | [Frontend](./phase-07-frontend.md) | Completed |
| 8 | [Docs and full verification](./phase-08-docs-and-full-verification.md) | Completed |
| 9 | [Seed trap practice data](./phase-09-seed-trap-practice-data.md) | Completed |

Thứ tự: 1 → 2 → 3 → **9** → 4 → 5 → 6 → 7 → 8. Phase 9 (dữ liệu mẫu cho mọi mã lỗi, không riêng T/F/NG) đánh số sau cùng
để không đổi tên các phase khác, nhưng làm ngay sau phase 3. TDD: mỗi phase viết test đỏ trước, test cũ liên quan phải còn xanh.
Codex làm từng phase; Claude nghiệm thu sau mỗi phase (mục "Nghiệm thu").

## Dependencies

- Liên quan, không chặn: `261021-2016-writing-criterion-sub-kp-judgments` (dùng `error_types.knowledge_point_id`; P1 giữ
  nguyên cột này), `261023-0050-content-author-workspace-completion` (màn mã lỗi; KP con tự đồng bộ ở backend),
  `261016-0131-knowledge-tree-assessment-journey` (giỏ ôn, bài ôn).
- Triển khai: content chạy migration trước learning. Content chỉ trả KP con khi learning gửi `includeErrorTraps=true`, nên
  learning bản cũ không thấy KP con (không lệch band, báo cáo). Learning bản mới với content cũ: danh mục không có KP con →
  không có bằng chứng KP con, hành vi cũ.

## Tiêu chí nghiệm thu

- Lan (hay chọn False ở câu đúng Not Given) và Minh (hay chọn Not Given ở câu đúng False) cùng yếu KP T/F/NG nhận bộ ôn
  khác nhau theo bẫy của KP con yếu nhất (test tích hợp tái hiện ví dụ PS-A/PS-B/PS-C).
- Chọn bẫy Y không ghi gì cho KP con X; chọn đúng ghi ✓ cho mọi KP con của câu (test domain).
- KP con `PRIORITY` khi KP cha không có bài ôn → có đúng một bài ôn tùy chọn `ERROR_PATTERN` **của KP cha**, bộ đầu nhắm
  bẫy; KP cha có bài ôn → không có (test tích hợp, gồm kịch bản "KP cha ôn xong, lần nộp sau mới tạo").
- Không có bài ôn nào mang KP là KP con; giỏ ôn không có KP con.
- Writing (`EssayKnowledgeJudge`, `LINKER_MISUSE`) và màn mã lỗi không đổi hành vi.
- Band ước tính, `skills`/`focus` của báo cáo, bản chụp, nhiệm vụ ngày không đổi khi có KP con (cùng hồ sơ có/không KP con).
- Content lỗi hoặc chậm không làm hỏng lần nộp bài (ghi bằng chứng KP con không gọi HTTP).
- Dữ liệu mẫu: mọi mã lỗi trong phạm vi (có KP cha có câu trắc nghiệm) có ≥ 2 bộ của KP cha, mỗi bộ ≥ 2 câu chứa bẫy;
  T/F/NG đạt riêng cho từng bẫy (phase 9).
- `mvn -q -pl services/content-service -am package` và `mvn -q -pl services/learning-service -am package` xanh; frontend
  test + build xanh.

## Dữ kiện đã kiểm chứng (2026-10-24, nhánh `feat/content-llm-shares-learning-config`)

| Dữ kiện | Nguồn |
| --- | --- |
| Bộ luyện thuộc KP khi tổng ≥ `minQuestions` câu **và có ít nhất 1 câu** gắn KP; sắp: lesson ưu tiên → `matched_question_count` → `p.id` | `JdbcLearningContentReader.java:88-103`, `:640-672` |
| `topic-sequence` trả mọi KP ACTIVE của topic | `JdbcLearningContentReader.java:176-182` |
| Lưu câu từ chối mã `distractors` không ACTIVE (`DRAFT` mà ACTIVE vẫn được) | `AddQuestionVersionUseCase.java:80-105` |
| `knowledge_points` có `kind`, `learning_type`, `skill`, `description`, `status` | seed `INSERT INTO knowledge_points (...)` |
| `question_versions.options` JSONB, `answer_spec` JSONB; `questions.question_type` có `TRUE_FALSE_NOT_GIVEN` | content V1 |
| Trọng số MP-1.0 theo nguồn qua `learning.mastery.mp1.source-weights.*`; `isPractice` = lesson/Practice/luyện tự do | `MasteryFormulaSettings.java:30-50`, `Mp1Params.java` |
| Practice: `firstAnswers` → `evidence.append` → `recordEvidence(userId, firstAnswers, criterionOnly)` → `criterionReviews.create` | `SubmitPracticeAttemptUseCase.java:129,162,216-221` |
| Bài ôn tùy chọn sinh lúc chấm: `insertPending(userId, candidates, false, "WRITING_CRITERION")` | `WritingCriterionReviews.java:58` |
| `review_items.trigger_kind` có `CHECK`, `VARCHAR(20)`; danh sách mới nhất ở V35 | learning V4, V35 |
| `kp_evidence.source` có `CHECK`, đổi bằng drop/add | learning V24 |
| Giỏ ôn: KP `PRIORITY` chỉ hiện khi đã có bài ôn tùy chọn (nhóm `weak`, `reason = triggerKind`) | `GetReviewBasketUseCase.java:66-106` |
| Ước tính band cần ≥ 80% KP của band ở `MAINTAIN` | `BandEstimator.java:24,52` |
| Test độ phủ đòi mỗi KP Reading/Writing course 5.5/6.5 có ≥ 3 bộ | `PracticeSetCoverageMigrationTest.java:51-56` |

## Hướng dẫn cho Codex (đọc trước khi làm)

- Đọc `backend/AGENTS.md` (§3, §5, §6) và `backend/CLAUDE.md` mục 3-4. Phase 7 đọc thêm `frontend/AGENTS.md`.
- Trước mỗi phase: `git status`, `git log -3`; kiểm số migration mới nhất; kiểm file được nhắc trong phase tồn tại và giống
  mô tả. Khác hẳn → **dừng và báo**, không đoán.
- Mỗi phase: (1) chạy baseline module, ghi test đang đỏ; (2) viết test mới, chạy, thấy đỏ **đúng lý do**; (3) code; (4) chạy
  lại toàn module. Lỗi nền có sẵn: ghi vào báo cáo, không sửa/vô hiệu test không liên quan.
- Testcontainers tự bỏ qua khi thiếu Docker: kiểm `docker ps`; bị bỏ qua thì ghi "chưa chạy", không coi là xanh.
- Chỉ sửa file liệt kê trong phase (file test mới thì được). Không commit, không push. Không sửa migration cũ. Không đưa mã
  plan/phase vào comment, tên test, migration, commit message. Không log nội dung câu trả lời.
- Ngoại lệ cho test cũ: nếu một test cũ đỏ **chỉ vì** dữ liệu mới làm đổi số đếm hợp lệ mà nó ghim (không phải vì hành vi sai),
  được ghim lại vào phiên bản nó viết cho (`Flyway.target`) hoặc giới hạn phạm vi đếm; giữ nguyên ý định và mọi assert; ghi từng
  test và lý do vào báo cáo. Mọi lỗi khác: dừng và báo.
- Dừng và hỏi nếu: cần đổi quyết định D1–D10/P1–P4; cần đổi `AssessmentCompleted.v2`; test nền đỏ khiến không phân biệt được
  lỗi mới. Thiếu bộ có câu bẫy (đã đo ở phase 1: gần như mọi mã đều thiếu) **không** là lý do dừng: phase 9 bổ sung.
- Báo cáo cuối mỗi phase lưu ở `reports/codex-phase-0N-<slug>-report.md`: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED`, file
  đã sửa, lệnh test đã chạy + kết quả thật, test chưa chạy và lý do, câu hỏi còn mở.

## Prompt giao việc cho Codex (dán nguyên văn, mỗi phase một lần)

`<PLAN_DIR>` = đường dẫn tuyệt đối tới thư mục chứa file này. Chỉ giao phase sau khi phase trước đã được nghiệm thu.

```text
Làm phase N của plan <PLAN_DIR>\plan.md, chi tiết ở <PLAN_DIR>\phase-0N-<tên>.md.
Đọc plan.md (các mục "Quyết định đã chốt", "Dữ kiện đã kiểm chứng", "Hướng dẫn cho Codex") và backend/AGENTS.md trước.
Làm đúng thứ tự TDD trong phase: baseline test, viết test đỏ, code, chạy lại toàn module. Chỉ sửa các file liệt kê trong phase.
Không commit, không sửa migration cũ, không đổi quyết định đã chốt. Gặp điều kiện "dừng và hỏi" thì dừng và báo.
Kết thúc bằng báo cáo ở <PLAN_DIR>\reports\codex-phase-0N-<tên>-report.md: Status, file đã sửa, lệnh test đã chạy và kết quả
thật, test chưa chạy và lý do, câu hỏi còn mở.
```

Phase 7 thêm dòng: `Repo là thư mục frontend/; dùng đúng "Hợp đồng JSON" ở phase-06.`

## Nghiệm thu (Claude) sau mỗi phase

1. Đọc báo cáo Codex; đối chiếu danh sách file đã sửa với "Related Code Files" của phase.
2. Đọc diff (`git diff`), soát theo `AGENTS.md`: layer, N+1/HTTP trong vòng lặp, migration mới không sửa cũ, không log nội dung.
3. Tự chạy lại lệnh test của phase; xác nhận test mới có trong lượt chạy (không bị bỏ qua vì thiếu Docker).
4. Kiểm từng mục "Success Criteria" của phase bằng bằng chứng (test, truy vấn, đoạn code).
5. Kết luận `ĐẠT` / `ĐẠT CÓ GHI CHÚ` / `CHƯA ĐẠT` kèm danh sách sửa; ghi `reports/acceptance-phase-0N-<slug>-report.md`;
   chỉ `ĐẠT` mới đánh dấu phase xong (`ck plan check`) và giao phase sau.

## Red Team Review

2026-10-24. Soát theo 4 góc (toàn vẹn dữ liệu, giả định sai, lỗi vận hành, phạm vi), không dùng agent con. 7 phát hiện,
người dùng duyệt từng mục: chấp nhận 6, bác 1.

| # | Mức | Phát hiện | Bằng chứng | Quyết định | Áp vào |
| --- | --- | --- | --- | --- | --- |
| PH1 | Nghiêm trọng | KP con lọt vào danh mục KP học viên qua `topic-sequence`, kéo sai band ước tính (0.8 `MAINTAIN`), báo cáo, bản chụp, nhiệm vụ | `JdbcLearningContentReader.java:180-181`, `BandEstimator.java:24,52` | Chấp nhận: cờ `includeErrorTraps`; danh mục learning lưu `parent_kp_id`/`error_code`; loại KP con khỏi mọi phép tính tổng hợp | phase 1, 3, 4, 6 |
| PH2 | Cao | Test độ phủ bộ luyện vỡ vì KP con | `PracticeSetCoverageMigrationTest.java:51-56` | Chấp nhận: test loại KP con; độ phủ KP con ghi báo cáo | phase 1, 2 |
| PH3 | Cao | Gọi HTTP lấy danh mục mã lỗi khi chấm, dưới khóa học viên, không có dự phòng | `SubmitPracticeAttemptUseCase.java:109-111`, `RestLearningContentClient.java:192-193` | Chấp nhận: bằng chứng, lỗi chính, luật nhóm, lesson đọc danh mục trong DB; chỉ phản hồi `fixHint` gọi HTTP, lỗi thì bỏ `error` | phase 4, 5, 6 |
| PH4 | Trung bình | Tự điền bẫy T/F/NG khi mã bị tắt chặn lưu mọi câu T/F/NG | `AddQuestionVersionUseCase.java:101-104` | Chấp nhận: chỉ tự điền khi hai mã ACTIVE | phase 3 |
| PH5 | Trung bình | Tạo bài ôn `ERROR_PATTERN` có thể đọc tier cũ | `WritingCriterionReviews.java:22` | Chấp nhận: thứ tự bằng chứng → `MasteryProfileUpdater` → `ErrorTrapReviews` | phase 5 |
| PH6 | Trung bình | V79 tạo KP con khi KP cha INACTIVE | `phase-02-content-schema-and-trap-knowledge-points.md:34-37` | Chấp nhận: chỉ khi KP cha ACTIVE; KP con theo trạng thái KP cha | phase 2, 3 |
| PH7 | Trung bình | `fixHint` mã `DRAFT` hiện cho học viên | `phase-06-learning-feedback-and-report.md:43` | Bác: người dùng đã quyết cho phép mã `DRAFT`, đánh dấu chờ duyệt | — |

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-08.
- Decision deltas checked: 6 (cờ `includeErrorTraps`; cột danh mục learning; bỏ `trapKnowledgePointId` khỏi API mã lỗi nội bộ;
  bỏ HTTP khi chấm; điều kiện tự điền T/F/NG; thứ tự tạo bài ôn).
- Reconciled stale references: 5 (phase 3 overview/yêu cầu/DTO/tiêu chí; phase 4 kiến trúc; `plan.md` mục triển khai).
  Tham số tìm bộ sau đó đổi thành `trapErrorCode` (xem mục dưới).
- Unresolved contradictions: 0.

### Đơn giản hóa sau red-team (người dùng đề xuất, 2026-10-24)

Đơn vị ôn luôn là KP cha; KP con chỉ quyết định chọn bộ. Bỏ: bài ôn của KP con, luật tìm bộ "≥ 3 câu bẫy" và
`hasPracticeSet` cho KP con, tra lesson KP con theo KP cha, luật "một bài ôn cho cả nhóm", KP con trong giỏ ôn. Trường hợp
"KP cha ổn, KP con yếu" giữ được bằng trigger `ERROR_PATTERN` tạo bài ôn tùy chọn cho KP cha. Sửa: `plan.md` (D4–D7, tiêu
chí), phase 1, 3, 4, 5 (viết lại), 7. Sweep lại: 0 mâu thuẫn.

### Sửa sau khi Codex dừng ở phase 2 (2026-10-24)

Phase 2 chạy toàn module đỏ 4 test cũ. Hai nguyên nhân, cả hai là lỗi của plan chứ không phải của Codex:
1. V79 (phase 2) tạo KP con nhưng bộ lọc ẩn KP con khỏi `topic-sequence` nằm ở phase 3 → `LessonPipelineSeedTest` (2 ca) thấy
   `TRAP_*`. Sửa: bộ lọc mặc định chuyển sang phase 2 (`JdbcLearningContentReader`), phase 3 chỉ thêm đường mở
   `includeErrorTraps=true`.
2. `DistractorSeedMigrationTest` (2 ca) ghim số liệu seed V46/V47 (20 câu, 31 đáp án sai), V79 thêm 43 câu T/F/NG. Sửa: ghim
   lớp test vào `Flyway.target("78")`; thêm ngoại lệ "test cũ ghim số đếm" vào "Hướng dẫn cho Codex"; nhắc lại ở phase 9.
V79 và hai test mới của Codex được giữ nguyên. Sweep: 0 mâu thuẫn.

### Chi tiết hóa cho Codex (2026-10-24)

Viết lại phase 2–7 ở mức code được: SQL đầy đủ (V79, tìm bộ bọc subquery vì Postgres không dùng alias trong biểu thức
`ORDER BY`, `topic-sequence`), chữ ký hàm, chỗ cắm theo dòng, bảng chỗ đọc cần loại KP con, giả mã `ErrorTrapReviews`, hợp
đồng JSON. Tham số tìm bộ đổi `trapKnowledgePointId` → `trapErrorCode` (SQL so mã trực tiếp, không cần join); kiểm còn bộ
cho `ERROR_PATTERN` dùng `searchPracticeSets` + `trapQuestionCount`, không đổi API availability. Thêm "Dữ kiện đã kiểm
chứng", "Hướng dẫn cho Codex", "Prompt giao việc", "Nghiệm thu". Sweep: 0 mâu thuẫn.
