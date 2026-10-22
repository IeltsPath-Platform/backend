# Phase 02: Xuất bản và lưu trữ phiên bản câu hỏi (backend, content-service)

Mục tiêu: lesson gắn được câu hỏi mới. Hiện không có đường nào xuất bản phiên bản câu hỏi; `QuestionVersion.publish()` và `Question.publishVersion()` không có nơi gọi trong production. Dùng lại hai phương thức đó và `PublicationStatus` (DRAFT, PUBLISHED, ARCHIVED).

## Endpoint (quyền `ADMIN`, `CONTENT_AUTHOR`)

- `POST /api/content/admin/questions/{questionId}/versions/{versionId}/publish`: DRAFT sang PUBLISHED; cập nhật `questions.current_published_version_id`. Kiểm tra trước khi xuất bản: `answer_spec` hợp lệ theo `docs/contracts/answer-spec-v1.md`, bắt buộc `difficulty` (đã có mã `DIFFICULTY_REQUIRED`), câu essay phải có `task` và `passBand` hợp lệ. Lỗi `422` kèm `details.code`.
- `POST /api/content/admin/questions/{questionId}/versions/{versionId}/archive`: PUBLISHED sang ARCHIVED. Từ chối `409 QUESTION_VERSION_IN_USE` nếu phiên bản đang gắn vào lesson đã PUBLISHED hoặc gói đề đã PUBLISHED.
- Đọc `QuestionController` (`/api/content/questions`, `/api/content/admin/questions`) để đặt đúng chỗ. Nếu chưa có, thêm danh sách có lọc `status`, `purpose`, `skill` và tìm theo KP (màn ngân hàng câu hỏi ở phase 07 cần).

## Triển khai

- Use case `@Transactional`; khóa dòng `SELECT ... FOR UPDATE` (mẫu `JdbcRubricRepository.updateDraft`, service không có `@Version`).
- Mã lỗi mới thêm vào `GlobalExceptionHandler` theo mẫu các mã 422/409 hiện có.
- Contract mới `docs/contracts/question-publication-v1.md` (mẫu `content-scheduled-publish-v1.md`: tiêu đề, dòng quyền, mỗi endpoint một mục, ví dụ JSON, trạng thái lỗi).

## Test

- Unit (Mockito) cho use case.
- `@WebMvcTest` kiểm quyền (mẫu `LessonResourceAuthorizationWebMvcTest`: CUSTOMER 403, ẩn danh 401, CONTENT_AUTHOR được phép) và thêm vào `ContentAuthorizationWebMvcTest`.
- Testcontainers cho `QUESTION_VERSION_IN_USE` (fixture dọn bằng tiền tố `code` riêng).

## Tiêu chí hoàn thành

Tạo câu hỏi DRAFT, xuất bản được, câu PUBLISHED xuất hiện trong lesson; lưu trữ bị từ chối khi đang được dùng. Contract và test xanh.
