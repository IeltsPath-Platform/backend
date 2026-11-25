# Phase 01: Backend cho slide là tệp PDF

Plan: [plan.md](plan.md) · Trạng thái: done

## Context

- `backend/services/content-service/src/main/java/com/ieltspath/content/domain/aggregate/LessonResource.java`
- `.../application/usecase/CreateLessonResourceUseCase.java`
- `.../resources/db/migration/V114__lesson_resource_files.sql` (ràng buộc cũ, **không sửa**)
- `backend/docs/contracts/lesson-authoring-v1.md` (mục "Documents uploaded as files", dòng 92-113)

## Yêu cầu

- Slide mới chỉ nhận `fileKey` (tiền tố `lesson-document/`, đúng chủ sở hữu, tệp thật sự tồn tại); body có `url` cho slide là `400`.
- Tài liệu giữ nguyên: link hoặc tệp.
- Mọi slide link hiện có bị xóa; DB không cho chèn lại.
- Lỗi trả về tiếng Việt có dấu, theo `AGENTS.md` §3.3.

## Files sửa / tạo

- Sửa `LessonResource.java`
  - Bỏ nhánh "Chỉ tài liệu mới là tệp tải lên được" ở constructor.
  - Thêm luật: `SLIDE` phải có tệp, báo "Slide phải là tệp PDF".
  - `createFile(...)` nhận thêm `LessonResourceType`.
  - Cập nhật Javadoc ở đầu lớp và ở `update` (slide cũng là tệp).
- Sửa `CreateLessonResourceUseCase.newFileResource`: bỏ chặn `!= DOCUMENT`, truyền loại vào `createFile`. Giữ kiểm tra "không được cả hai" và `requireOwnedBy`.
- Tạo `V148__lesson_slide_files.sql`, theo thứ tự:
  1. `DELETE FROM lesson_resources WHERE resource_type = 'SLIDE';` (mọi slide hiện tại đều là link, theo ràng buộc V114).
  2. `DROP CONSTRAINT ck_lesson_resources_target`, tạo lại cho phép tệp ở mọi loại: `(url IS NOT NULL AND file_key IS NULL AND url ~* '^https?://') OR (url IS NULL AND file_key IS NOT NULL AND file_key LIKE 'lesson-document/%')`.
  3. Thêm `ck_lesson_resources_slide_file CHECK (resource_type <> 'SLIDE' OR file_key IS NOT NULL)`.
  - Comment đầu file ghi rõ migration xóa slide link có chủ ý. Không sửa V57, V60, V114 (đã áp dụng).
- Không cần đổi controller, DTO, `LessonResourceResult` (đã ký URL theo `fileKey` không phân loại), `Delete/Update/Reorder`.
- Sửa `backend/docs/contracts/lesson-authoring-v1.md`: "A slide is always a link" thành "A slide is always an uploaded PDF"; dòng 103-104 đổi thành "A slide with a `url`, or a body with both, returns `400`"; thêm một dòng nói V148 đã xóa slide link cũ.
- Tiền tố `lesson-document/` và endpoint `/media/document` dùng chung cho slide. Giữ nguyên tên để khỏi đổi contract; ghi chú một câu trong contract.

## Test

Sửa các test đang giả định slide là link, rồi chạy `mvn -q -pl services/content-service -am test`:

- `LessonResourceTest`: slide có url ném lỗi; slide có `fileKey` hợp lệ tạo được; tài liệu link vẫn tạo được.
- `LessonResourceAdminControllerTest`: tạo slide bằng `fileKey` trả `201`; bằng `url` trả `400` tiếng Việt.
- `LessonResourceFileMigrationTest`: thêm ca cho V148 (slide link bị xóa, chèn slide link bị DB từ chối, chèn slide tệp được; tài liệu link giữ nguyên).
- `LessonResourcePersistenceTest`, `InternalLessonResourceControllerTest`, `LessonResourceWebMvcTest` (learning-service): đổi dữ liệu mẫu slide sang `fileKey`; kiểm URL ký xuất hiện ở `slides[0].url`.
- Luồng course draft cũng nhận slide bằng `fileKey`: `CourseDraftCodecTest` từ chối slide URL, `ApproveCourseDraftIntegrationTest` kiểm file được kiểm tra quyền/tồn tại và materialize thành slide.
- Test xóa slide gọi `LessonDocumentFiles.delete` sau commit.
- Test Testcontainers tự bỏ qua khi không có Docker; nếu bỏ qua thì ghi rõ khi báo cáo.

## Rủi ro và rollback

- Xóa dữ liệu không đảo ngược. Muốn lùi chỉ có cách khôi phục từ bản sao DB; không có migration đảo.
- Môi trường đã có slide link thật (ngoài seed) mất slide khi lên V148: thông báo cho admin trước khi triển khai.
- Chưa có phase 03 mà đã chạy V148 thì học viên thấy slide trống; xem thứ tự triển khai ở `plan.md`.
