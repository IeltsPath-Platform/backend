# Phase 03: API soạn lesson (backend, content-service)

Phụ thuộc: phase 02 (xuất bản câu hỏi).

## Hiện trạng cần biết (đã khảo sát)

- Bảng V8: `lessons` (status DRAFT/PUBLISHED/ARCHIVED, `code` UNIQUE toàn cục, `UNIQUE(topic_id, sort_order)`), `lesson_blocks` (`block_type` TEXT/ASSET/VOCABULARY/EXERCISE, `UNIQUE(lesson_id, sort_order)`), `lesson_block_questions`, `lesson_block_vocabulary` (sense id không FK, nằm ở library), `lesson_knowledge_points`, `lesson_block_knowledge_points` (chỉ block TEXT). `summary_markdown` (V56), `estimated_minutes` (V58). Không có aggregate/entity/repository Java cho lesson; đọc bằng SQL thuần ở `JdbcLearningContentReader` (`publishedLessons` :215, `publishedLesson` :249, `lessonExists` :384).
- Hai ràng buộc `sort_order` không deferrable và V72 dùng `sort_order` âm cho block từ vựng: sắp xếp lại phải làm hai bước (đẩy tạm sang dải offset lớn rồi đặt giá trị cuối), không cần migration.
- Block essay = block EXERCISE có đúng một câu `answer_spec.type = ESSAY`, `task` TASK_1/TASK_2, `passBand` 4.0-9.0, TASK_1 cần `chartFacts` 1-2000 ký tự và ảnh IMAGE gắn vào question version (`LessonBlockKind.classify`). Block EXERCISE không được trộn essay với câu khác.
- learning-service đọc nội dung live mỗi request và lưu tiến độ theo `block_id` (`passed_block_ids`). ID block phải ổn định khi sửa; thay cả bộ block làm học viên mất tín dụng block đã qua. Sắp xếp lại lesson an toàn (`LessonProgress.place()` tự đồng bộ).
- Chưa có kiểm tra sở hữu câu hỏi (một câu thuộc đúng một chủ: lesson hoặc gói) ở phía lesson; `questionsUsedElsewhere` (`JdbcLearningContentReader.java:516`) đã tính block lesson là chủ, dùng lại. Cũng chưa có nơi nào kiểm tra `purpose = LEARNING` ở phía lesson.
- `InvalidLessonBlockException` hiện là lỗi dữ liệu seed (HTTP 500, mã `INVALID_LESSON_BLOCK`); lỗi do client nhập phải là 4xx với mã khác.

## Thiết kế

- Domain: `Lesson` aggregate + `LessonBlock` entity (không import Spring/JPA), repository contract ở `domain/repository`, adapter JDBC (`NamedParameterJdbcTemplate`) ở `infrastructure/persistence/adapter` vì cần upsert theo `block_id` ổn định và sắp xếp hai bước. Mẫu cấu trúc: `LessonResource` + `LessonResourceAdminController` và luồng publish của `ContentPackage`. Mọi use case ghi khóa dòng lesson `SELECT ... FOR UPDATE`. Chuyển lỗi unique-constraint trong adapter thành domain exception (mẫu `LessonResourceRepositoryAdapter.save`).
- Endpoint (tất cả `/api/content/admin`, quyền `ADMIN`, `CONTENT_AUTHOR`):

| Method | Path | Hành vi |
| --- | --- | --- |
| GET | `/topics/{topicId}/lessons` | Danh sách để soạn: mọi trạng thái, số block, KP, `estimatedMinutes`. |
| POST | `/topics/{topicId}/lessons` | Tạo lesson DRAFT: `code`, `title`, `summary`, `summaryMarkdown?`, `estimatedMinutes?`, `sortOrder?` (mặc định nối cuối). |
| GET | `/lessons/{id}` | Góc nhìn soạn: block đủ nội dung, câu hỏi **mọi trạng thái** (kèm trạng thái phiên bản), KP, tài nguyên. |
| PUT | `/lessons/{id}` | Sửa tiêu đề, tóm tắt, markdown, số phút, ở mọi trạng thái lesson. |
| PUT | `/topics/{topicId}/lessons/order` | Sắp xếp lesson theo danh sách id. |
| PUT | `/lessons/{id}/knowledge-points` | Thay bộ KP của lesson (KP phải ACTIVE). |
| POST | `/lessons/{id}/blocks` | Thêm block: `blockType`, `textContent` / `assetId` / `vocabularySenseIds` / `questionVersionIds`, `knowledgePointIds` (chỉ TEXT), `sortOrder?`. |
| PUT | `/lessons/{id}/blocks/{blockId}` | Sửa block, **giữ nguyên `blockId`**. Block TEXT/ASSET/VOCABULARY sửa được ở mọi trạng thái lesson. Block EXERCISE đổi bộ câu hỏi chỉ khi lesson DRAFT, ngược lại `409 LESSON_EXERCISE_LOCKED`. |
| DELETE | `/lessons/{id}/blocks/{blockId}` | Xóa block; block EXERCISE chỉ khi lesson DRAFT. |
| PUT | `/lessons/{id}/blocks/order` | Sắp xếp block theo danh sách id (hai bước). |
| POST | `/lessons/{id}/publish` | Kiểm tra rồi chuyển PUBLISHED. |
| POST | `/lessons/{id}/archive` | Chuyển ARCHIVED (học viên nhận 404 cho bài này). |

- Kiểm tra khi xuất bản, `422 LESSON_NOT_PUBLISHABLE` kèm danh sách lỗi trong `details`: có ít nhất một KP ACTIVE và một block; mỗi block EXERCISE hợp lệ theo `LessonBlockKind.classify`; mọi câu hỏi ở trạng thái PUBLISHED, `purpose = LEARNING`, mỗi câu thuộc đúng một chủ; KP của block TEXT là ACTIVE; sense từ vựng tồn tại.
- Kiểm tra sense từ vựng qua port `VocabularySenseLookup` gọi library `GET /api/content/vocabulary/senses?ids=` (tối đa 100 id), theo mẫu `ContentTopicClient`/`TopicLookup` của library-service (chuyển bearer và `X-Correlation-Id`, timeout ngắn); lỗi dịch vụ thành `503`.
- Mã lỗi mới trong `GlobalExceptionHandler`: `LESSON_BLOCK_INVALID` (422), `LESSON_NOT_PUBLISHABLE` (422), `LESSON_EXERCISE_LOCKED` (409), `LESSON_SORT_CONFLICT` (409). Trùng `code` lesson dùng `DuplicateCodeException` sẵn có.
- Không đổi hình dạng `/internal/learning-content/lessons/**` mà learning-service đang đọc.
- Cập nhật `services/content-service/README.md` (dòng nói chưa có API soạn lesson). Contract mới `docs/contracts/lesson-authoring-v1.md` (mẫu `content-scheduled-publish-v1.md`); ghi bổ sung vào `learning-content-internal-v1.md` nếu có lệch.

## Test

Aggregate (`LessonTest`), use case Mockito, `@WebMvcTest` kiểm quyền và thêm vào `ContentAuthorizationWebMvcTest`, Testcontainers: tạo; sắp xếp lại khi có block `sort_order` âm; sửa block đã xuất bản giữ nguyên id; xuất bản bị từ chối (câu DRAFT, câu thuộc chủ khác); khóa EXERCISE. Fixture dọn bằng tiền tố `code` riêng.

## Tiêu chí hoàn thành

R&D tạo, sửa, sắp xếp lesson và block, gắn KP và câu hỏi, xuất bản mà không cần SQL. Sửa block TEXT của lesson đã xuất bản không đổi `blockId`. learning-service vẫn đọc được lesson (test hợp đồng nội bộ xanh).

## Đối chiếu với frontend (phase 08)

Màn soạn bài đọc đúng contract `lesson-authoring-v1.md`. Frontend chấp nhận cả `id` và `lessonId`/`blockId`, cả `assetId` và `asset.id`, cả `knowledgePointIds` và `knowledgePoints[]`. Sau mỗi thao tác ghi, frontend tải lại bằng GET nên không phụ thuộc body trả về. Lỗi xuất bản đọc `details.issues` (mảng chuỗi mã); chuỗi nào frontend chưa có bản dịch thì hiện nguyên mã. Để câu trả lời của màn soạn có nghĩa, mỗi câu hỏi trong block EXERCISE của `GET /lessons/{id}` nên có `stem`, `status`, `versionNumber`.
