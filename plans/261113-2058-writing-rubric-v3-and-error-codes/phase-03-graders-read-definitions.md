# Phase 3: Grader đọc định nghĩa, task của mã và system prompt của rubric

## Context

- Phụ thuộc phase 1 (hai cột mới). Nên release cùng phase 2.
- Hiện tại cả hai grader in dòng mã là `code — name: fixHint` và gửi mọi mã Writing cho cả hai task:
  - learning: `EssayGrader.systemPrompt` (dùng `LearningContentClient.ErrorType`);
  - assessment: `RubricPromptBuilder.build` (dùng `ErrorTypeCatalog.ErrorType`, cho essay gate và đợt AI eval).
- `rubrics.system_prompt` của Writing hiện không được grader nào dùng.

## File

**content-service**
- `domain/aggregate/ErrorType.java`, `infrastructure/persistence/adapter/JdbcErrorTypeRepository.java`: đọc
  `model_definition`, `writing_tasks` (chỉ đọc; admin chưa sửa được hai cột này).
- `api/controller/InternalLearningContentController.ActiveErrorTypeResponse`: thêm `modelDefinition`, `writingTasks`
  (thêm trường, không đổi trường cũ).
- `docs/contracts/learning-content-internal-v1.md`: ghi hai trường mới.

**learning-service**
- `application/port/LearningContentClient.ErrorType`: thêm hai trường, giữ constructor cũ cho test.
- `application/service/EssayGrader.java`:
  - lọc `errorTypes` theo task trước khi dựng prompt **và** trước khi dựng `allowedCodes`;
  - dòng mã: `code — name: modelDefinition`, nếu trống thì dùng `fixHint`;
  - in `rubric.systemPrompt()` (nếu có) ngay trước `{{criteria}}`.
- Mapping của client content (adapter HTTP) nếu record không tự map.

**assessment-service**
- `application/port/ErrorTypeCatalog.ErrorType` và `ContentErrorTypeClient`: thêm hai trường.
- `infrastructure/client/RubricPromptBuilder.java`: cùng ba thay đổi như `EssayGrader`, để đợt AI eval chấm giống production.
- `ActiveRubricProvider.Rubric`: thêm `systemPrompt` nếu chưa có.

Tách hàm lọc theo task và hàm in dòng mã thành helper nhỏ trong mỗi service. Không đưa sang `shared/`: hai service có
record riêng, và helper chỉ vài dòng.

## Bước làm

1. content: đọc và trả hai cột; test `@WebMvcTest` hoặc repository test cho response mới.
2. learning: record, lọc theo task, dòng mã, system prompt. Test trong `EssayGraderTaskChecksTest` hoặc test mới:
   - prompt Task 1 không có `T2_*`, `OFF_TOPIC`, `UNDERDEVELOPED_IDEA`, `NO_CLEAR_POSITION`;
   - prompt Task 2 không có `T1_*`, `DATA_LANGUAGE`;
   - mã có `modelDefinition` thì dòng mã không chứa `fixHint`;
   - mã trả về thuộc task kia bị xử lý như mã không được phép (giống hành vi hiện có với mã lạ);
   - rubric không có `systemPrompt` thì prompt giống hệt trước khi sửa (giữ nguyên hành vi với @2).
3. assessment: cùng các test cho `RubricPromptBuilder`.
4. Chạy `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test`.

## Không làm trong phase này (D1)

- Backend tự tạo bằng chứng `T1_NO_OVERVIEW`, `NO_CLEAR_POSITION`, `T2_PARTIAL_ANSWER` từ `taskChecks`. Ba mã này
  vẫn gửi cho model (D5).
- `{{word_count}}`, `{{paragraph_count}}`.
- `withNextBands` bỏ qua band .5.
- Admin sửa `model_definition`/`writing_tasks`.

## Rủi ro

- `promptVersion` vẫn là `code@version+tplv2`. Đổi dòng mã làm prompt khác đi nhưng tên version không đổi. Phân biệt
  được nhờ rubric @3 (version tăng); nếu chạy @2 với code mới thì so sánh eval cũ và mới không còn sạch. Ghi vào PR.
- Content trả thêm trường: client cũ dùng `@JsonIgnoreProperties(ignoreUnknown)` nên không vỡ. Cần kiểm learning
  client có cấu hình tương tự.
