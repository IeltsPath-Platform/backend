# Phase 02: Frontend thanh phân trang

Plan: [plan.md](plan.md) · Trạng thái: done · Phụ thuộc: phase 01 (hình dạng phản hồi)

## Context

- `frontend/src/features/admin/packages/pages/PackagesPage.tsx` (đã bỏ ô "Mở trực tiếp bằng ID")
- `frontend/src/features/admin/packages/api/packageApi.ts` (`listPackages`) và `types.ts` (`packageFiltersSchema`, `packageListItemSchema`)
- `frontend/src/features/admin/packages/components/PlacementPoolSummary.tsx`
- Mẫu phân trang admin: `frontend/src/features/admin/questions/pages/QuestionBankPage.tsx` và `ai-drafts/pages/AiDraftsPage.tsx` (dùng khuôn này, không tạo component phân trang dùng chung mới)
- `frontend/src/features/admin/questions/types.ts` (`questionPageSchema`) làm mẫu schema trang
- `frontend/.sdd/constraints/admin-ui.md`; quy ước đã lưu: không thêm chữ giải thích dưới ô

## Yêu cầu

1. `listPackages(filters, signal)` gửi thêm `page`, `size`; parse phản hồi mới thành `{ content, page, size, totalElements, totalPages }` bằng schema zod mới (`packagePageSchema`).
2. `PackagesPage` giữ `page` trong state, cỡ trang 25; hiện thanh "Trước / Trang n / tổng · x gói / Sau" theo khuôn `QuestionBankPage`; đổi bất kỳ bộ lọc nào hoặc ô tìm (sau debounce) thì về trang 0; cột STT tính theo `page * size + index + 1`.
3. Hủy yêu cầu cũ bằng `AbortSignal` khi đổi trang hoặc lọc (đã có qua `useAdminResource`); khóa cache có `page`.
4. Nếu trang hiện tại vượt `totalPages` sau khi dữ liệu đổi (ví dụ xóa gói), lùi về trang cuối.
5. `PlacementPoolSummary`: đếm số gói placement đã xuất bản theo từng kho bằng `totalElements` (`type=PLACEMENT_TEST`, `status=PUBLISHED`, `placementPool=...`, `size=1`); giữ nguyên chữ và hành vi hiển thị hiện có ("Cần thêm đề: lượt thi lại sẽ lặp đề"...). Không tải toàn bộ danh sách để đếm.

## Files sửa

- `packageApi.ts`, `types.ts`, `PackagesPage.tsx`, `PlacementPoolSummary.tsx`, `packages.css` nếu cần (token có sẵn, không màu cứng).
- Test: `PackagesPage.test.tsx`, test của `PlacementPoolSummary` (nếu có), `packageApi.test.ts`, fixture trong `testFixtures` nếu mock `listPackages` trả mảng.

## Test

- Mock `listPackages` trả đối tượng trang; kiểm bấm "Sau" gọi `page=1`, đổi loại gói về `page=0`, STT đúng ở trang 2.
- Mọi test đang `mockResolvedValue([...])` cho `listPackages` phải đổi sang hình dạng trang; tìm bằng `grep -rn "listPackages" frontend/src`.
- `PlacementPoolSummary` vẫn hiện đúng số đã xuất bản từng kho và cảnh báo thiếu đề.
- Chạy bằng PowerShell: `npx.cmd vitest run src/features/admin/packages`, rồi `npm run lint`, `npm run typecheck`, `npm run build`. (Chạy bằng Git Bash từng lỗi `Vitest failed to find the current suite`; đó là cách chạy, không sửa cấu hình.)
- Thử tay: tạo hoặc dùng dữ liệu demo hơn 25 gói; chuyển trang; lọc loại gói Placement thấy số kho đúng; tìm theo mã.

## Rủi ro

- Phải lên cùng backend phase 01; nếu lên lệch thì Zod ném lỗi parse và trang hiện lỗi.
- Không thêm dependency, không sửa `package.json`, `vite.config.ts`, `tsconfig`, `eslint.config.js`.
