# Bản nháp mới của gói được sao chép sẵn từ bản trước

Trạng thái: done (code và test xong; chờ chủ dự án thử tay) · content-service + frontend · Có thể không cần migration · Một phase: [phase-01-copy-version.md](phase-01-copy-version.md)

## Vấn đề

"Tạo bản nháp mới" hiện tạo bản **trống**: `CreateDraftPackageVersionUseCase` gọi `ContentPackage.createDraftVersion(rules)`,
chỉ ghi một dòng `content_package_versions` với số bản kế tiếp và `rules`, không có phần, câu hỏi, đoạn văn, audio. Frontend
gọi `createDraftVersion(packageId, '{}')` và báo "Đã tạo bản nháp trống". Muốn sửa một câu trong đề đã xuất bản, tác giả phải
dựng lại cả đề. Với bản đã xuất bản (chỉ xem), cách duy nhất để sửa là đi qua bản nháp, nên bản nháp phải bắt đầu từ nội dung hiện có.

## Quyết định cần chủ dự án xác nhận (đề xuất sẵn)

| Chủ đề | Đề xuất |
|---|---|
| Bản nguồn | Bản **đang hiện hành** (`currentPublishedVersionId`); gói chưa xuất bản lần nào thì bản có số lớn nhất; gói chưa có bản nào thì tạo trống như cũ |
| Có cho chọn bản nguồn không | Không (YAGNI). Muốn đi từ bản cũ hơn thì chưa hỗ trợ |
| Sao chép gì | Phần (tiêu đề, kỹ năng, phần thi, thời gian, hướng dẫn, đoạn văn, ảnh bìa, audio), nhóm câu hỏi, liên kết câu hỏi (cùng `question_version_id`, điểm tối đa, thứ tự), `rules` của bản nguồn |
| Câu hỏi có sao chép nội dung không | Không. Chỉ liên kết lại cùng phiên bản câu hỏi; câu hỏi và audio là tài nguyên dùng chung |
| Hành vi khi bản nguồn trống | Tạo bản trống, như cũ |

## Tiêu chí nghiệm thu

- Gói có bản 1 (7 phần, câu hỏi, audio): tạo bản nháp thì bản 2 có đủ 7 phần với nội dung y hệt, id mới, thứ tự giữ nguyên.
- Sửa hoặc xóa trong bản 2 không ảnh hưởng bản 1.
- Nhóm câu hỏi được ánh xạ đúng: liên kết câu hỏi của bản 2 trỏ nhóm của bản 2, không trỏ nhóm của bản 1.
- Một gói chỉ có tối đa một bản nháp (luật cũ, giữ nguyên); sao chép chạy trong cùng transaction với khóa gói đang có.
- Frontend: nút tạo bản nháp báo "Đã tạo bản nháp từ bản N." (hoặc "bản nháp trống" khi không có nguồn); không đổi luồng khác.
- `mvn -q -pl services/content-service -am test` và `ArchitectureTest` xanh (Testcontainers cần Docker; bị bỏ qua phải nói rõ);
  frontend: `npx.cmd vitest run src/features/admin/packages`, lint, typecheck.

## Câu hỏi còn mở

- Bản nháp có nên giữ `publishAt` (lịch xuất bản) của bản nguồn không: đề xuất **không**, bản nháp luôn bắt đầu chưa có lịch.
