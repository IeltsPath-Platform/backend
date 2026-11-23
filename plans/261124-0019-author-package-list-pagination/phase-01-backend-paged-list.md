# Phase 01: Backend phân trang danh sách gói

Plan: [plan.md](plan.md) · Trạng thái: done

## Context

- `services/content-service/src/main/java/com/ieltspath/content/api/controller/ContentPackageAuthoringController.java` (`list`, dòng ~65)
- `.../application/usecase/ListAuthorPackagesUseCase.java`, `.../application/port/AuthorPackageReader.java` (`LIST_LIMIT = 500`, `Filter`)
- `.../infrastructure/persistence/adapter/JdbcAuthorPackageReader.java` (`list`, dòng ~34)
- Mẫu phân trang đã có: `QuestionController.listQuestions`, `ListQuestionsUseCase.Page`, `QuestionPageResponse`
- `.../api/dto/response/AuthorPackageSummaryResponse.java`
- `backend/docs/contracts/content-package-authoring-v1.md` dòng ~75-90 (mục danh sách)
- `backend/AGENTS.md` §3.3 (lỗi tiếng Việt), §3.7 (phân trang, không `findAll`), §5

## Yêu cầu

1. `GET /api/content/admin/packages` nhận thêm `page` (mặc định 0) và `size` (mặc định 25, từ 1 đến 100). Giá trị ngoài khoảng trả `400` tiếng Việt, không dùng mã lỗi làm message.
2. Phản hồi đổi thành `{ content: [...], page, size, totalElements, totalPages }`; mỗi phần tử giữ nguyên các trường hiện có.
3. Bỏ `LIST_LIMIT`. Giữ các bộ lọc hiện có (`type`, `status`, `lessonId`, `topicId`, `courseId`, `q`, `placementPool`) và thứ tự `updated_at DESC, id`.
4. `totalElements` đếm với **đúng cùng điều kiện lọc**.

## Thiết kế

- `AuthorPackageReader.list(Filter, page, size)` trả một `Page` (`items` và `totalElements`), theo khuôn `QuestionRepository.Page`. Dựng `WHERE` một lần, dùng cho cả truy vấn dữ liệu (`LIMIT :size OFFSET :offset`) và `SELECT COUNT(*)`.
- Truy vấn dữ liệu giữ hai truy vấn con (`latest_version_number`, `has_draft`); chỉ chạy cho 25 dòng của trang nên rẻ hơn hiện tại (500 dòng).
- Use case kiểm `page`/`size` và ném `IllegalArgumentException` tiếng Việt giống `ListQuestionsUseCase`; `GlobalExceptionHandler` đã chuyển thành `400`.
- Thêm `AuthorPackagePageResponse` cạnh `AuthorPackageSummaryResponse`, theo `QuestionPageResponse`.
- Không dùng `findAll()`, không lọc hay đếm trong bộ nhớ.

## Files sửa / tạo

- Sửa `AuthorPackageReader.java`, `JdbcAuthorPackageReader.java`, `ListAuthorPackagesUseCase.java`, `ContentPackageAuthoringController.java`.
- Tạo `AuthorPackagePageResponse.java`.
- Sửa `content-package-authoring-v1.md`: bỏ câu "Results are limited to 500 … no pagination in v1", ghi `page`, `size`, hình dạng mới, và ghi chú đây là thay đổi hình dạng phản hồi so với bản trước (như cách `question-publication-v1.md` đã ghi "Changed from the earlier version of this route").

## Test

- Cập nhật test đang giả định trả mảng: `ContentPackageAuthoringControllerTest`, `ContentPackageAuthoringErrorTest`, `ContentAuthorizationWebMvcTest`, `JdbcAuthorPackageReaderTest`.
- Mới: trang 0 và trang 1 không trùng dòng và nối liền thứ tự; `totalElements` đúng khi có lọc (`type`, `q`, `placementPool`); `size=101`, `size=0`, `page=-1` trả `400` tiếng Việt; tổng vượt 500 vẫn truy cập được hàng thứ 501 (seed 501 dòng trong test Testcontainers, hoặc kiểm `OFFSET` bằng cách tương đương).
- ArchUnit: `mvn -q -pl services/content-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Chạy hẹp trước: các lớp test trên; sau đó `mvn -q -pl services/content-service -am test` **một lần** (bộ này rất nặng, nhiều lớp tự dựng PostgreSQL).

## Rủi ro và rollback

- Đổi hình dạng phản hồi: frontend phải lên cùng lúc (phase 02). Lùi bằng cách khôi phục mảng và `LIST_LIMIT`.
- Kiểm chỉ mục cho `ORDER BY updated_at DESC, id` nếu bảng lớn; không cần migration với vài trăm hàng. Nếu `EXPLAIN` cho thấy quét tuần tự đáng kể, ghi vào báo cáo và hỏi trước khi thêm migration.
