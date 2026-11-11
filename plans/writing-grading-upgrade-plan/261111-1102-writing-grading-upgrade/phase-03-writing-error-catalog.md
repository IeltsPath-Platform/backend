---
phase: 3
title: "Writing error catalog"
status: pending
priority: P1
dependencies: []
---

# Phase 3: Danh mục lỗi Writing (content, learning)

## Overview
Prompt chấm đang nhận mọi mã lỗi active, kể cả của Reading, Listening và Speaking, trong khi mã ngữ pháp cho Writing chỉ có
bốn. Phase này thêm cột kỹ năng, bổ sung mã còn thiếu và mã dự phòng. Phần mã mới **chờ chuyên gia duyệt**; phần code không chờ.

## Requirements
- Content migration `V125__error_type_skill.sql` (V122 đã dùng cho mô tả band thấp; kiểm lại số lúc code; không sửa V45):
  - `error_types.skills VARCHAR(20)[] NOT NULL DEFAULT '{}'`, giá trị trong `READING`, `LISTENING`, `WRITING`, `SPEAKING`.
    Dùng mảng vì một mã ngữ pháp có thể dùng cho cả Writing và Speaking.
  - Backfill theo dữ liệu hiện có: `READING_STRATEGY` → `READING`; `LISTENING_STRATEGY` → `LISTENING`; mã `SPK_*` →
    `SPEAKING`; `T1_*`, `OFF_TOPIC`, `UNDERDEVELOPED_IDEA`, `NO_CLEAR_POSITION`, `LINKER_MISUSE`, và các mã `GRAMMAR`,
    `VOCABULARY` không có tiền tố `SPK_` → `WRITING`. `SPELLING_NUMBER`: rà từng mã (`PLURAL_FORM`, `SPELLING` dùng cho
    Writing; `WORD_LIMIT_EXCEEDED`, `NUMBER_FORMAT` thì không). Mã không xếp được để mảng rỗng và liệt kê trong mô tả PR.
- `ErrorType` (domain Content) thêm `skills`; màn admin `ErrorTypeController` đọc và sửa được.
- `GET /internal/learning-content/error-types` nhận `?skill=WRITING` (tùy chọn; không truyền thì trả như cũ, để consumer khác không vỡ).
  Learning thêm `LearningContentClient.activeErrorTypes(skill)` (giữ bản không tham số); `EssayGrader` gọi với `WRITING`.
- **Assessment lọc giống Learning (quyết định 8):** `ErrorTypeCatalog`/`ContentErrorTypeClient` thêm bản có `skill`;
  `OpenAiCompatibleEssayGrader` gọi với `WRITING`, để eval chạy đúng danh mục học viên nhận.
- Các chỗ gọi khác giữ bản không lọc: `ErrorTypeTexts` (tra tên và `fixHint`), `ApplyHumanReviewResultUseCase` (kiểm mã
  Mentor chọn). `SpeakingGradingContextCapture` vẫn lọc tiền tố `SPK_` trong đợt này; chuyển sang `skill=SPEAKING` là việc
  riêng của Speaking.
- **Mã dự phòng** (seed cùng migration, `review_status = APPROVED`, `knowledge_point_id = NULL`): `GRAMMAR_OTHER`,
  `VOCABULARY_OTHER`, `COHERENCE_OTHER`, `TASK_OTHER`. Thêm cột `fallback BOOLEAN NOT NULL DEFAULT false` để code nhận biết,
  không dựa vào hậu tố tên.
- **Mã mới (chờ duyệt):** chuyên gia đối chiếu 24 lỗi của examiner và đề xuất mã, tên, `fixHint`, KP gắn kèm. Ứng viên:
  dạng động từ sau modal, thiếu động từ chính, giới từ, danh từ đếm được/không đếm được, cấu trúc so sánh, ý kiến cá
  nhân trong Task 1. Seed bằng migration riêng **sau khi duyệt**; trước đó các lỗi này rơi vào mã dự phòng.
- **Mã Task 2 (chờ duyệt, phục vụ phase 8):** `T2_PARTIAL_ANSWER` (trả lời thiếu phần đề), `T2_NO_CONCLUSION` (thiếu kết
  bài), `T2_IRRELEVANT_EXAMPLE` (ví dụ không liên quan hoặc khái quát quá mức); gắn KP cho `NO_CLEAR_POSITION` (hiện
  `NULL`) để nút luyện dùng được. Chuyên gia chọn mã, tên, `fixHint`, KP; cùng migration với nhóm trên.
- Learning: correction mang mã dự phòng được lưu và trả cho học viên, nhưng `WritingEvidenceRule`, `EssayKnowledgeJudge`
  và luật tạo bài ôn bỏ qua nó (không có KP nên đường hiện tại đã bỏ qua; thêm test khóa hành vi này).

## Related Code Files
- Content: `domain/aggregate/ErrorType.java`, JPA entity và mapper của error type, `api/controller/InternalLearningContentController.java`,
  `api/controller/ErrorTypeController.java`, use case liệt kê error type, migration mới
- Learning: `application/port/LearningContentClient.java`, `infrastructure/client/RestLearningContentClient.java`,
  `application/service/EssayGrader.java`
- Assessment: `application/port/ErrorTypeCatalog.java`, `infrastructure/client/ContentErrorTypeClient.java`,
  `infrastructure/client/OpenAiCompatibleEssayGrader.java`
- Docs: `docs/contracts/learning-content-internal-v1.md` (tham số `skill`, trường `skills`, `fallback`)
- Tests: Testcontainers cho migration và query lọc; `EssayGraderTest` (prompt không còn mã `SPK_*`); test evidence bỏ qua mã dự phòng

## Implementation Steps
1. Baseline test content và learning.
2. Liệt kê toàn bộ mã active hiện có (mọi migration, không chỉ V45) và bảng xếp `skills` đề xuất; gửi người dùng xem trước khi viết migration.
3. Test đỏ → migration và code Content → test đỏ → Learning.
4. Chạy eval (phase 1) lần nữa với prompt v1 nhưng danh mục đã lọc: so `error_recall` và `error_code_false_positive` với số nền.

## Success Criteria
- [ ] Prompt Writing ở cả Learning và Assessment chỉ chứa mã có `WRITING`.
- [ ] Mã dự phòng không sinh evidence hay bài ôn (có test).
- [ ] Consumer khác của `/error-types` không đổi hành vi.

## Risk Assessment
- Danh mục gửi cho LLM đổi mà `promptVersion` không đổi (nó chỉ gồm rubric và template): hành vi chấm đổi âm thầm.
  Bắt buộc chạy eval ở bước 4 và ghi kết quả; nếu within-half-band hoặc QWK tụt dưới ngưỡng thì dừng lại hỏi người dùng.
- `activeErrorTypes()` có năm chỗ gọi (bốn ở Learning, một ở Assessment): thêm bản có tham số, không đổi bản cũ, để
  các chỗ không liên quan giữ nguyên hành vi.
- Chấm gate của Assessment cũng nhận danh mục đã lọc: hành vi chấm gate đổi cùng lúc với Learning, cùng bước eval ở bước 4.
