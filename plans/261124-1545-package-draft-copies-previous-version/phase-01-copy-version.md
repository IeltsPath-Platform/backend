# Phase 01: Sao chép bản nguồn khi tạo bản nháp

Plan: [plan.md](plan.md) · Trạng thái: todo

## Context

- `content-service/.../application/usecase/CreateDraftPackageVersionUseCase.java` (khóa gói bằng `findByIdForUpdate`, gọi `createDraftVersion`).
- `domain/aggregate/ContentPackage.createDraftVersion`, `domain/entity/ContentPackageVersion`.
- Bảng: `content_package_versions`, `content_sections` (V1, có `package_version_id`), `section_question_groups` (V28),
  `section_questions` (V1, có `group_id` nối bằng khóa kép `(group_id, section_id)`). Đọc tất cả migration chạm các bảng này
  để liệt kê **đủ cột** (passage, audio, cover, `part_no`, `time_limit_seconds`, ...), vì sao chép thiếu cột là mất dữ liệu.
- Controller `ContentPackageAuthoringController` (route tạo bản nháp), `AuthorPackageRequests`, contract
  `docs/contracts/content-package-authoring-v1.md`.
- Frontend: `src/features/admin/packages/api/packageApi.ts` (`createDraftVersion`), `PackageEditorPage.tsx` (thông báo "Đã tạo bản nháp trống").

## Yêu cầu

1. Sau khi chọn bản nguồn (xem bảng quyết định trong plan.md), sao chép trong **cùng transaction** và cùng khóa gói:
   dòng version mới (số kế tiếp, `DRAFT`, `rules` lấy của nguồn, không `publishAt`), các phần, nhóm câu hỏi, liên kết câu hỏi.
   Sinh UUID mới cho mọi dòng; ánh xạ `old section id → new`, `old group id → new` để `section_questions.group_id` và khóa kép đúng.
2. Làm ở database bằng `INSERT ... SELECT` qua một adapter persistence (không nạp cả cây vào bộ nhớ rồi lặp gọi repository,
   không N+1; AGENTS §3.7). Aggregate vẫn giữ luật "tối đa một bản nháp"; use case gọi một port mới (ví dụ
   `PackageVersionCopier`) sau khi aggregate tạo bản nháp, hoặc để use case tạo bản nháp rồi gọi copier với id nguồn và đích.
3. Request tạo bản nháp giữ hình dạng hiện có; `rules` trống thì lấy của bản nguồn. Không thêm tham số chọn nguồn.
4. Contract: cập nhật `content-package-authoring-v1.md` ("tạo bản nháp: sao chép từ bản hiện hành; trống nếu gói chưa có bản").
5. Frontend: `createDraftVersion` không còn gửi `'{}'` cố định nếu không cần; thông báo thành công "Đã tạo bản nháp từ bản N."
   (lấy N từ phản hồi hoặc từ phiên bản nguồn đang chọn) và "Đã tạo bản nháp trống." khi gói chưa có phần.
6. Không migration. Nếu thiếu chỉ mục khiến `INSERT ... SELECT` chậm, DỪNG và hỏi.

## Test

- Testcontainers (content-service): gói có hai nhóm và nhiều câu; tạo nháp thì số phần, nhóm, liên kết bằng nguồn;
  id đều mới; `group_id` của liên kết mới trỏ nhóm mới; sửa/xóa bản nháp không đổi bản nguồn; gói không có bản thì tạo trống;
  có sẵn một bản nháp thì vẫn trả `409` tiếng Việt như cũ; xuất bản bản nháp vẫn chạy.
- Use case (Mockito) cho chọn bản nguồn (hiện hành, bản lớn nhất, không có bản).
- Frontend: test `PackageEditorPage` kiểm thông báo mới và gọi API đúng.
- Chạy hẹp trước (`ContentPackageAuthoringTest`, test mới), rồi cả service và `ArchitectureTest`.

## Rủi ro

- Thiếu một cột khi sao chép (cột thêm ở migration sau V28) là mất dữ liệu âm thầm: đối chiếu `information_schema` hoặc
  migration của từng bảng, và thêm test so sánh toàn bộ cột (trừ id, khóa ngoại, thời gian) giữa nguồn và bản sao.
- Bản sao dùng chung tài nguyên (câu hỏi, audio): xóa tài nguyên gốc sau này ảnh hưởng cả hai bản. Chấp nhận, ghi trong PR.
- Gói lớn (hàng trăm liên kết): `INSERT ... SELECT` một lượt, trong khóa gói; không gọi HTTP trong lúc khóa.
