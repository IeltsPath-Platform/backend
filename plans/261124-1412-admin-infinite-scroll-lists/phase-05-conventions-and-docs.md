# Phase 05: Quy ước UI, tài liệu và dọn dẹp

Plan: [plan.md](plan.md) · Trạng thái: done · Admin UI guidance, project decision memory, and obsolete pager CSS updated.

## Context

- `frontend/.sdd/constraints/admin-ui.md` mục 3 "Bảng" (hiện ghi: "Mọi bảng có cột STT... bảng phân trang đánh số nối tiếp (`page * size + index + 1`)")
- Bộ nhớ dự án (`MEMORY.md` và các file `admin-ui-*.md`) ghi các quy ước admin đã chốt
- CSS cũ của thanh nút phân trang rải ở `questions.css`, `packages.css`, `lessons.css`...

## Yêu cầu

1. Sửa `admin-ui.md` mục 3: thay luật phân trang đánh số bằng luật cuộn vô hạn: "Danh sách có thể lớn tải theo trang ở server và tự tải thêm khi cuộn gần đáy (`useInfiniteList` + `InfiniteSentinel`); không có nút Trước/Sau/Trang n; cột STT nối tiếp; đầu bảng dính; dòng 'Đang hiện n / tổng' và 'Đã hiện tất cả'; danh sách nhỏ cố định và cây không dùng cách này". Ghi ngày quyết định 2026-11-24 và rằng đây là yêu cầu của chủ dự án.
2. Ghi rõ **ngoại lệ**: ngôn ngữ nút "Thử lại" ở đáy khi tải thêm lỗi; trạng thái trong `role="status"`, không toast.
3. Cập nhật mô tả phân trang trong contract các API đã đổi (phase 03) nếu chưa làm, và mục "Pagination" trong `frontend/README.md` hoặc tài liệu liên quan nếu có.
4. Xóa CSS và hằng số của thanh nút không còn dùng (`.qb-pager`, `.adm-package-pager`, `.ls-questions__pagination`, ...), kiểm bằng `grep` rằng không còn tham chiếu.
5. Lưu quyết định vào bộ nhớ dự án (loại feedback, một file mới) để các phiên sau không đề xuất lại nút phân trang.
6. Nếu còn danh sách admin nào dùng phân trang bằng nút do được loại khỏi phạm vi, liệt kê trong mục "Ngoại lệ" của `admin-ui.md` để không bị coi là bỏ sót.

## Test và kiểm tra

- `grep -rn "Trang trước\|Trang sau" frontend/src/features/admin` chỉ còn trong danh sách ngoại lệ đã ghi.
- `npm run lint`, `typecheck`, `build`, `npx.cmd vitest run src/features/admin` xanh.
- Thử tay một lượt toàn bộ khu admin: mỗi màn trong phạm vi cuộn tải thêm được.

## Rủi ro

- Sửa tài liệu quy ước là thay đổi cấp dự án: chỉ làm sau khi chủ dự án xác nhận phạm vi ở câu hỏi mở 1 của plan.
