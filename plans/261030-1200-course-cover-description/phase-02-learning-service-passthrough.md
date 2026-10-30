# Phase 2 - Đường learner: content-service → learning-service

## Context

Learner không gọi content-service trực tiếp. Course/topic đi qua `GET /api/learning/...` của learning-service, lấy dữ liệu từ endpoint internal của content-service.

## Files

content-service (`backend/services/content-service/src/main/java/com/ieltspath/content/`):
- `api/dto/internal/TopicSequenceResponse.java` - record `Course` thêm `coverImageUrl`, `description`; thêm `description` ở cấp topic.
- `application/result/TopicSequenceResult.java` - `CourseEntry` và topic thêm các trường tương ứng.
- `infrastructure/persistence/adapter/JdbcLearningContentReader.java` - SQL quanh dòng 160-175 chọn thêm `c.cover_image_url`, `c.description AS course_description`, `t.description`; nếu có query khác dựng `CourseEntry` thì sửa theo.

learning-service (`backend/services/learning-service/src/main/java/com/ieltspath/learning/`):
- `application/port/LearningContentClient.java` - record `Course` (dòng ~216) và topic record thêm trường; giữ constructor phụ cũ truyền null.
- Client implementation đọc JSON internal (tìm class implements `LearningContentClient`).
- `application/result/CourseResult.java` + `api/dto/response/CourseResponse.java` - thêm `coverImageUrl`, `description`.
- `application/result/TopicResult.java` + response topic tương ứng - thêm `description`, và `coverImageUrl`/`description` trong course ref gắn vào topic.

## Steps

1. Mở rộng record ở content-service, sửa SQL reader.
2. Mở rộng record client ở learning-service; JSON deserialize phải chịu được thiếu trường (null) để deploy lệch version không vỡ.
3. Truyền xuống `CourseResult`/`TopicResult` và response API.
4. Cập nhật contract tài liệu nếu có mô tả response (`backend/docs/contracts/`), chỉ khi file đó mô tả course/topic summary.

## Tests

- Test của reader/client hiện có (`JdbcLearningContentReader*`, `LearningContentClient*`) cập nhật record mới.
- Test controller learner: response chứa 3 trường, null khi chưa set.

## Risks / rollback

- Nhiều record có nhiều constructor phụ: sửa dễ lỡ tay, chạy full test module learning-service.
- Rollback: bỏ trường khỏi record; FE phase 3 đọc optional nên không vỡ.
