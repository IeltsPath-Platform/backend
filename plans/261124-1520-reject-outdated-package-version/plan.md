# Chặn bắt đầu làm bài trên phiên bản gói đã bị thay thế

Trạng thái: todo · Backend (content-service, assessment-service) · Không migration · Không đổi frontend

## Vấn đề

Khi gói có bản 2 được xuất bản, bản 1 vẫn `status = 'PUBLISHED'` (code không có bước chuyển sang `ARCHIVED`).
`GET /internal/learning-content/package-versions/{id}` chỉ lọc `v.status = 'PUBLISHED'`
(`JdbcLearningContentReader.packageVersionContent`, `publishedOnly = true`), không so với
`content_packages.current_published_version_id`. Assessment gọi endpoint này khi học sinh bấm "Bắt đầu"
(`StartAssessmentAttemptUseCase`), nên ai còn giữ ID bản 1 (tab mở sẵn, đường dẫn đã lưu, gọi API trực tiếp) vẫn làm được
bản đã bị thay: bản sửa lỗi nội dung không tới họ, điểm hai học sinh cùng gói ra từ hai đề khác nhau, lượt placement bị tính.

## Đã chốt với chủ dự án

- Chỉ chặn lúc **bắt đầu** làm bài. Lượt đã bắt đầu thì nộp bình thường theo bản đã làm: nội dung được chụp vào lượt
  (`AttemptCreator.sectionSnapshot`), không đọc lại từ Content lúc nộp. Không buộc làm lại.
- Chặn ở content-service, một truy vấn; không thêm endpoint, không migration.
- Không đổi trạng thái bản cũ sang `ARCHIVED` trong phạm vi này.

## Phase

| # | File | Nội dung |
|---|---|---|
| 01 | [phase-01-reject-outdated-version.md](phase-01-reject-outdated-version.md) | Kiểm tra bên gọi giữ ID cũ, sửa truy vấn, đổi thông báo, test |

## Tiêu chí nghiệm thu

- Gói có bản 1 và bản 2 cùng `PUBLISHED`, hiện hành là bản 2: đọc bản 2 được, bản 1 trả rỗng.
- Gói chỉ một bản hiện hành: luồng cũ không đổi.
- Bắt đầu bằng ID bản cũ trả lỗi tiếng Việt, không tạo lượt làm bài.
- Lượt đã bắt đầu trên bản 1 vẫn nộp và chấm đúng sau khi bản 2 xuất bản.
- `mvn -q -pl services/content-service -am test` và `mvn -q -pl services/assessment-service -am test` xanh
  (Testcontainers cần Docker; nếu bị bỏ qua phải nói rõ), cùng `ArchitectureTest` của hai service.

- Không có thay đổi ở learning-service. Cửa sổ hẹp của placement (tối đa 15 phút sau khi giao) được chấp nhận, xem phase 01.

## Câu hỏi còn mở

- Không còn. Nếu Codex phát hiện bên gọi khác dùng endpoint, hoặc học viên có thể kẹt quá 15 phút, dừng và hỏi.
