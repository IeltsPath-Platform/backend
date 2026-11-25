# Phase 04: Chuyển các màn đang tải cả mảng

Plan: [plan.md](plan.md) · Trạng thái: done · Frontend pagination callers updated.

## Context

- Kết quả kiểm kê và API trang mới ở [phase 03](phase-03-backend-paging-for-array-lists.md)
- `frontend/src/features/admin/users/pages/UsersPage.tsx` (tải `GET /api/users` cả mảng, `PAGE_SIZE = 25`, `slice`, bộ lọc client, thanh nút)
- `frontend/src/features/admin/work-queue/pages/WorkQueuePage.tsx` và `work-queue/api/workQueueApi.ts`
- Các màn khác theo kết quả kiểm kê (từ vựng quản trị nếu thuộc nhóm lớn)
- Hook và thành phần dùng chung từ phase 01

## Yêu cầu

1. `userAdminApi.listUsers(filter, page, size, signal)` gọi API trang mới (đã chốt phân trang ở server); schema `pageSchema(userSchema)`; các bộ lọc đang áp ở client (vai trò, trạng thái, tìm kiếm) thành tham số server do phase 03 thêm; không lọc client trên dữ liệu chưa đủ.
2. `UsersPage`, `WorkQueuePage` và các màn được duyệt: dùng `useInfiniteList` + `InfiniteSentinel` như phase 02, STT nối tiếp, thao tác ghi gọi `refresh()`.
3. Màn **không** thuộc nhóm lớn (khóa học, chủ đề, rubric, lỗi nhỏ, cây tri thức) giữ nguyên.
4. Mọi nơi khác đang dùng hàm `list*` đã đổi hình dạng (ví dụ danh sách chọn người dùng ở màn khác) phải đổi theo hoặc dùng đường danh sách nhỏ riêng.

## Test

- Cập nhật test các màn đã đổi theo khuôn phase 02: mock API trả trang, kích hoạt sentinel để tải thêm, đổi bộ lọc về đầu, STT nối tiếp.
- Bộ lọc người dùng: gửi đúng tham số, không còn lọc client trên dữ liệu thiếu.
- Chạy bằng PowerShell: `npx.cmd vitest run src/features/admin`, `npm run lint`, `npm run typecheck`, `npm run build`. Thử tay: màn người dùng với nhiều dòng, đổi bộ lọc giữa chừng.

## Rủi ro

- Bộ lọc/tìm kiếm hiện làm ở client có thể không có tương đương phía server; phải thêm vào phase 03 hoặc bỏ có chủ ý (cần chủ dự án quyết).
- Thứ tự mặc định của API mới phải khớp thứ tự đang thấy trên màn (ví dụ người dùng sắp theo tên hoặc ngày tạo), nếu không người dùng thấy danh sách "đổi chỗ".
