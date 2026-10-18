---
phase: 5
title: "AI sinh course từ yêu cầu"
status: pending
priority: P2
dependencies: [2, 3, 4]
---

# Phase 5: AI sinh course từ yêu cầu

## Overview

Mentor: nhập "6.5, luyện chủ đề A, B, C" thì hệ thống sinh ra một course phù hợp, rồi người duyệt. Theo quyết định D3, AI
**chỉ chọn từ kho**, không bịa nội dung. Để chắc điều đó, việc chọn ứng viên làm bằng luật xác định trước, LLM chỉ được
chọn và sắp xếp **trong tập ứng viên đó**, và mọi mã nó trả phải nằm trong tập.

## Requirements

- **Yêu cầu:** `POST /api/content/admin/ai-drafts/courses` (`CONTENT_AUTHOR`, `ADMIN`):

  ```json
  { "bandLevel": 6.5, "skills": ["READING"], "topics": ["Paraphrase", "True/False/Not Given"],
    "knowledgePointIds": [], "lessonCount": 8, "instructions": "tùy chọn" }
  ```

  - `topics` là chủ đề dạng chữ do người dùng nhập; hệ thống ánh xạ sang KP (bước chọn ứng viên). Cũng nhận trực tiếp
    `knowledgePointIds`. Phải có ít nhất một trong hai.
  - Trả `202` kèm id.
- **Bước 1, chọn ứng viên (luật, không LLM)** (`CourseCandidateSelector`):
  - lấy KP `ACTIVE` đúng band và kỹ năng; thêm các KP tiên quyết của chúng thuộc band thấp hơn (đưa vào nhưng đánh dấu
    "nền", không bắt buộc đưa vào lesson);
  - ánh xạ chủ đề chữ sang KP bằng so khớp tên, mã và mô tả KP chuẩn hóa; chủ đề không khớp KP nào → ghi `unmatchedTopics`
    trong kết quả, không tự bịa KP;
  - với mỗi KP, liệt kê gói `PRACTICE_SET` và câu `EXERCISE` `PUBLISHED` chưa gắn nơi khác (ứng viên kho đề);
  - sắp KP theo thứ tự tô-pô của cây tiên quyết (phase 1 của plan cây).
- **Bước 2, LLM** nhận tập ứng viên (mã và mô tả ngắn, không có nội dung câu hỏi đầy đủ) và được yêu cầu:
  - nhóm KP thành `lessonCount` lesson và đặt tên, tóm tắt từng lesson;
  - chọn gói và câu cho từng lesson **chỉ từ mã trong tập ứng viên**;
  - gợi ý topic (nhóm lesson) và thứ tự; không tạo khối `TEXT`.
- **Bước 3, kiểm tra kết quả** (`CourseDraftValidator`):
  - mọi mã KP, gói, câu trong kết quả phải thuộc tập ứng viên; mã lạ → loại và ghi cảnh báo `HALLUCINATED_REFERENCE`;
  - không có mã nào trùng hai chỗ; chạy `CourseCoverageAnalyzer` (phase 4) và lưu `analysis` kèm bản nháp.
  - LLM trả hỏng định dạng → thử lại tối đa 1 lần, vẫn hỏng → `FAILED` với `INVALID_REPLY`.
- **Thiếu kho đề (D3):** KP không đủ gói hoặc câu → vào `gaps`, **không** sinh thêm. Phản hồi bản nháp kèm đường dẫn gợi ý
  tới yêu cầu sinh câu hỏi (phase 3) cho từng KP thiếu.
- **Dùng lại bản nháp của phase 4:** kết quả có cấu trúc đúng như phase 4, nên xem, sửa tay (`PUT`), phân tích và duyệt
  qua cùng API `course-drafts`. Bản nháp do AI sinh có `model` và `prompt_version`.
- **Hạn mức và nền:** dùng hạ tầng phase 2. Một yêu cầu sinh course tính một lượt hạn mức.

## Architecture

```text
RequestCourseDraftUseCase ─ trừ quota, lưu ai_drafts(QUEUED, kind=COURSE, request)
RunAiDraftJobUseCase ─ CourseDraftGenerator:
    CourseCandidateSelector (luật) → CoursePromptBuilder → ContentGenerationPort
        → CourseDraftValidator (tập ứng viên + CourseCoverageAnalyzer) → result + analysis
duyệt/sửa/phân tích: dùng CourseDraftController của phase 4
```

## Related Code Files

(`C` = content package)

- Tạo: `C/application/service/{CourseCandidateSelector, CoursePromptBuilder, CourseDraftGenerator,
  CourseDraftValidator}.java`, `C/application/usecase/RequestCourseDraftUseCase.java`,
  `C/api/controller/AiCourseDraftController.java`, DTO
- Test:
  - `CourseCandidateSelectorTest` (thứ tự tô-pô, chủ đề không khớp, KP nền, gói đã gắn nơi khác bị loại);
  - `CourseDraftValidatorTest` (mã lạ bị loại và cảnh báo; gói trùng hai lesson bị loại);
  - `CoursePromptBuilderTest` (không chứa dữ liệu học viên, không chứa nội dung câu hỏi đầy đủ);
  - `AiCourseDraftIntegrationTest` (cổng giả trả cấu trúc mẫu có một mã bịa → bản nháp sạch, `gaps` đúng, duyệt tạo
    course `INACTIVE`).

## Implementation Steps

1. **Đỏ trước:** `CourseCandidateSelectorTest`, `CourseDraftValidatorTest`.
2. Selector và validator (hai phần quyết định chất lượng, làm trước).
3. Prompt builder và generator; nối vào hạ tầng phase 2.
4. API yêu cầu; integration test.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Mọi mã trong bản nháp đều thuộc tập ứng viên; mã LLM bịa không lọt vào.
- [ ] Yêu cầu có chủ đề không khớp KP nào → `unmatchedTopics`, không có KP bịa.
- [ ] Kho thiếu → `gaps`, không có câu nào được sinh thêm trong luồng này.
- [ ] Duyệt bản nháp AI tạo course `INACTIVE` như phase 4.

## Risk Assessment

- Ánh xạ chủ đề chữ sang KP bằng so khớp chuỗi có thể yếu. Nên cho người dùng chọn `knowledgePointIds` trực tiếp là đường
  chính; chủ đề chữ chỉ là tiện lợi, kèm `unmatchedTopics` để thấy rõ.
- Tập ứng viên lớn làm prompt dài. Giới hạn số KP mỗi yêu cầu (mặc định 40).

## Bàn giao FE

Form yêu cầu (band, kỹ năng, chủ đề hoặc chọn KP, số lesson), màn xem bản nháp kèm danh sách `gaps` và nút "sinh thêm câu
cho KP này".
