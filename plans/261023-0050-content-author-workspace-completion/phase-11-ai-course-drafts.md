# Phase 11: AI sinh course từ yêu cầu

Người làm: **Claude Code**. Nhánh: `feat/content-ai-course-drafts` (backend), tạo từ `origin/main` sau khi phase 10 đã merge.
Trạng thái: Done (2026-10-23, chưa commit/merge; xem mục "Kết quả")

Repo: `backend`, service `content-service`. Phụ thuộc: phase 08 (hạ tầng), phase 09 (sinh câu, để bù KP thiếu), phase 10 (cấu trúc, kiểm phủ, duyệt).

## Mục tiêu

Mentor: nhập "6.5, luyện chủ đề A, B, C" thì hệ thống sinh một course phù hợp, rồi người duyệt. AI **chỉ chọn từ kho**, không bịa nội dung. Việc chọn ứng viên làm bằng luật xác định trước; LLM chỉ được nhóm, sắp xếp và chọn **trong tập ứng viên đó**, và mọi mã nó trả phải thuộc tập.

## Yêu cầu

- **Yêu cầu sinh:** `POST /api/content/admin/ai-drafts/courses` (chỉ `CONTENT_AUTHOR`):

  ```json
  { "bandLevel": 6.5, "skills": ["READING"], "topics": ["Paraphrase", "True/False/Not Given"],
    "knowledgePointIds": [], "lessonCount": 8, "instructions": "tùy chọn" }
  ```

  - `topics` là chủ đề dạng chữ; hệ thống ánh xạ sang KP. Cũng nhận trực tiếp `knowledgePointIds` (đường chính, chính xác hơn). Phải có ít nhất một trong hai.
  - Trừ một lượt hạn mức `AI`, lưu `ai_drafts` (`QUEUED`, `kind = COURSE`), trả `202` kèm id.
- **Bước 1, chọn ứng viên (luật, không LLM)** `CourseCandidateSelector`:
  - KP `ACTIVE` đúng band và kỹ năng; thêm KP tiên quyết band thấp hơn, đánh dấu "nền" (không bắt buộc vào lesson);
  - ánh xạ chủ đề chữ sang KP bằng so khớp tên, mã, mô tả đã chuẩn hóa; không khớp → `unmatchedTopics`, không bịa KP;
  - mỗi KP: gói `PRACTICE_SET` và câu `PUBLISHED` chưa gắn nơi khác;
  - sắp KP theo thứ tự tô-pô của cây tiên quyết;
  - tối đa 40 KP mỗi yêu cầu (cấu hình được) để prompt không quá dài.
- **Bước 2, LLM** nhận tập ứng viên (mã và mô tả ngắn, không có nội dung câu đầy đủ) và được yêu cầu: nhóm KP thành `lessonCount` lesson, đặt tên và tóm tắt; chọn gói và câu cho từng lesson **chỉ từ mã trong tập**; gợi ý topic và thứ tự, mã và tiêu đề đề cuối của từng topic và course; không tạo khối `TEXT` có nội dung.
- **Bước 3, kiểm tra** `CourseDraftValidator`:
  - mọi mã KP, gói, câu phải thuộc tập ứng viên; mã lạ → loại và cảnh báo `HALLUCINATED_REFERENCE`;
  - không mã nào dùng hai chỗ; chạy `CourseCoverageAnalyzer` (phase 10) và lưu `analysis` kèm bản nháp;
  - LLM trả hỏng định dạng → thử lại tối đa 1 lần, vẫn hỏng → `FAILED` `INVALID_REPLY` (hoàn hạn mức).
- **Thiếu kho đề:** KP không đủ gói hoặc câu → `gaps`, **không** sinh thêm trong luồng này. Mỗi mục `gaps` kèm gợi ý gọi yêu cầu sinh câu của phase 09 cho KP đó.
- **Dùng lại phase 10:** kết quả đúng cấu trúc của phase 10, nên xem, sửa tay (`PUT`), phân tích và duyệt qua `course-drafts`. Bản nháp AI có `model` và `prompt_version`. Duyệt tạo course `INACTIVE` và đề cuối rỗng `DRAFT` như phase 10.
- Bổ sung vào `ai-drafts-v1.md` / `course-drafts-v1.md` trong cùng commit. Prompt không có dữ liệu học viên.

## Thiết kế phía code

```text
RequestCourseDraftUseCase ─ trừ quota, lưu ai_drafts(QUEUED, kind=COURSE, request)
RunAiDraftJobUseCase ─ CourseDraftGenerator:
    CourseCandidateSelector (luật) → CoursePromptBuilder → ContentGenerationPort
        → CourseDraftValidator (tập ứng viên + CourseCoverageAnalyzer) → result + analysis
xem/sửa/phân tích/duyệt: CourseDraftController của phase 10
```

Tạo: `C/application/service/{CourseCandidateSelector, CoursePromptBuilder, CourseDraftGenerator, CourseDraftValidator}.java`, `C/application/usecase/RequestCourseDraftUseCase.java`, `C/api/controller/AiCourseDraftController.java`, DTO.

## Test

- `CourseCandidateSelectorTest`: thứ tự tô-pô, chủ đề không khớp, KP nền, gói đã gắn nơi khác bị loại, giới hạn 40 KP.
- `CourseDraftValidatorTest`: mã lạ bị loại kèm cảnh báo; gói trùng hai lesson bị loại.
- `CoursePromptBuilderTest`: không có dữ liệu học viên, không có nội dung câu đầy đủ.
- `AiCourseDraftIntegrationTest` (port giả trả cấu trúc mẫu có một mã bịa): bản nháp sạch, `gaps` đúng, duyệt tạo course `INACTIVE`.

## Hoàn thành khi

`mvn -q -pl services/content-service -am test` xanh; mọi mã trong bản nháp thuộc tập ứng viên; chủ đề không khớp nằm ở `unmatchedTopics`; kho thiếu nằm ở `gaps`, không câu nào được sinh thêm trong luồng này.

## Kết quả

**2026-10-23 (Claude Code), chưa commit.** Trạng thái: Done (chờ merge).

- Contract: phần "AI course drafts" trong `course-drafts-v1.md`; mã lỗi `NO_CANDIDATES` thêm vào `ai-drafts-v1.md`.
- `POST /api/content/admin/ai-drafts/courses` (`AiCourseDraftController`, `RequestCourseDraftUseCase`): kiểm yêu cầu, trừ hạn mức `AI`, lưu `QUEUED`, `202`.
- `CourseCandidateSelector` (luật, không LLM): KP theo id hoặc khớp cụm chủ đề (chuẩn hóa chữ thường, bỏ dấu, mọi từ phải có trong tên/mã/mô tả), thêm tiên quyết ở mức CEFR thấp hơn làm "nền", xếp tô-pô (Kahn, hòa thì giữ thứ tự chọn), tối đa `content.ai.max-course-candidates` (40), kèm gói `PRACTICE_SET` chưa gắn và câu `LEARNING` đã xuất bản.
- `CoursePromptBuilder` (`COURSE-v1`): chỉ mã, tên, mô tả ngắn, mã gói, nhãn câu `Q1…`; không có id câu hỏi, nội dung câu, dữ liệu học viên.
- `CourseDraftValidator`: mã KP/gói/nhãn câu ngoài tập ứng viên bị loại kèm `HALLUCINATED_REFERENCE`; dùng hai chỗ → `DUPLICATE_REFERENCE`; lesson hết KP bị bỏ (`EMPTY_LESSON_DROPPED`), topic hết lesson bị bỏ (`EMPTY_TOPIC_DROPPED`); mã do model đặt được làm sạch và đảm bảo duy nhất; `sortOrder` topic nối tiếp sau topic gốc cao nhất đang có.
- `CourseDraftGenerator`: hai lần thử, vẫn hỏng → `INVALID_REPLY` (job hoàn hạn mức); không ứng viên → `NO_CANDIDATES` (không gọi model); kết quả = cấu trúc + `unmatchedTopics`, `warnings`, `analysis`, `gapHints` (`REQUEST_QUESTION_DRAFT`). Không sinh thêm câu nào để bù thiếu.
- Duyệt/sửa/phân tích dùng route của phase 10; trường phụ bị bỏ khi tác giả `PUT` cấu trúc.
- Test: `CourseCandidateSelectorTest`, `CourseDraftValidatorTest`, `CourseDraftGeneratorTest`, `RequestCourseDraftUseCaseTest`, `AiCourseDraftControllerTest`, `CourseDraftAuthorizationTest`, `AiCourseDraftIntegrationTest` (Testcontainers, model giả trả cấu trúc có một mã bịa: bản nháp sạch, gaps đúng, duyệt tạo course `INACTIVE`, lượt dùng được giữ khi thành công và hoàn khi lỗi, không có câu hỏi nào được tạo).
- Lưu ý: mã CEFR của band 6.5 là `C1` (tới 4.5 B1, 6.0 B2, 7.5 C1), nên KP của course 6.5 phải có `cefr_level = C1` mới vào tập ứng viên theo cụm chủ đề.
