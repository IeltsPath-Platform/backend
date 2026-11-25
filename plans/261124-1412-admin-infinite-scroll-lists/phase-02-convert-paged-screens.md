# Phase 02: Chuyển các màn đang phân trang bằng nút

Plan: [plan.md](plan.md) · Trạng thái: done · Chỉ frontend · Phụ thuộc: phase 01

Chuyển 8 màn nhóm A sang `useInfiniteList` và `InfiniteSentinel`. API và hình dạng phản hồi **không đổi**.

## Context

Mỗi màn dưới `frontend/src/features/admin/`:

| Màn | File | Ghi chú riêng |
|---|---|---|
| Bộ chọn câu hỏi | `lessons/components/QuestionChooser.tsx` | Làm **đầu tiên**: đang bị phản ánh. Nằm trong hộp thoại `wide` (cuộn thân hộp thoại) và trong ngăn kéo phần thi; danh sách `.ls-questions__list` có `max-height` riêng (từ `.adm-modal .ls-questions__list`) nên vùng cuộn là chính danh sách. Giữ bảng nhãn cộng dồn của câu đã chọn |
| Ngân hàng câu hỏi | `questions/pages/QuestionBankPage.tsx` | Ô tìm kiếm hiện lọc client trên trang hiện tại. **Đã chốt: chuyển sang tìm ở server** bằng tham số `code` (debounce 300 ms, đổi thì về đầu danh sách); bỏ lọc client `text`. Chỉ tìm theo mã |
| Đề & gói | `packages/pages/PackagesPage.tsx` | Khóa tải gồm bộ lọc; bỏ `pageAt`/`adm-package-pager` |
| Bản nháp AI | `ai-drafts/pages/AiDraftsPage.tsx` | |
| Đánh giá AI | `ai-evaluation/pages/AiEvaluationPage.tsx` | Hai danh sách (bài mẫu, lượt đánh giá), mỗi cái một hook riêng |
| Chấm bài | `examiner-review/pages/ExaminerReviewPage.tsx` | Hai danh sách (hàng chờ, của tôi), `size` theo hằng `PAGE_SIZE` |
| Lịch sử đăng nhập | `users/components/LoginHistoryModal.tsx` | Danh sách trong hộp thoại |

Thêm: `frontend/.sdd/constraints/admin-ui.md` mục 3 (bảng), phase 05 mới sửa; phase này không động vào tài liệu.

## Yêu cầu

1. Mỗi danh sách dùng `useInfiniteList(key, (page, signal) => api(..., page, size, signal))`, `key` chứa mọi bộ lọc. Đổi bộ lọc thì danh sách về đầu và cuộn lại lên trên (đặt `scrollTop = 0` của vùng cuộn).
2. Thay thanh nút bằng `InfiniteSentinel` ở cuối bảng/danh sách; dòng tóm tắt "Đang hiện n / tổng …" thay "Trang n / tổng".
3. Cột STT nối tiếp (`index + 1` trên danh sách đã gộp).
4. Thao tác ghi (tạo, sửa, xóa, xuất bản, nhận bài) xong gọi `refresh()` để tải lại từ đầu mà không nháy trắng.
5. Đầu bảng dính (`adm-table--sticky`) khi bảng nằm trong vùng cuộn của chính trang.
6. Bỏ code và CSS của thanh nút trong mỗi màn (`qb-pager`, `adm-package-pager`, `ls-questions__pagination`, state `page`).
7. `QuestionChooser`: giữ nguyên `purpose`, `defaultSkill`, `includeOwnershipPreview`, công tắc "Chỉ hiện câu còn trống" (đổi công tắc thì về đầu), ô tìm theo mã (debounce 300 ms). `SectionQuestionsEditor` vẫn không gửi `include`.

## Test

- Mỗi màn: thay các bước bấm "Trang sau" bằng kích hoạt sentinel (`intersect`) và kiểm yêu cầu có `page` kế tiếp; đổi bộ lọc thì yêu cầu mới về `page=0`; hết dữ liệu thì hiện dòng "Đã hiện tất cả".
- `QuestionChooser`: kéo xuống tải thêm; câu đã chọn ở trang đầu vẫn hiện đúng mã sau khi tải thêm; công tắc đổi thì về đầu.
- `PackagesPage`: STT nối tiếp qua các lần tải; "Chi tiết"/"Chỉnh sửa" vẫn đúng với dòng sau khi tải thêm.
- Chạy bằng PowerShell: `npx.cmd vitest run src/features/admin`, `npm run lint`, `npm run typecheck`, `npm run build`.
- **Thử tay trên trình duyệt** (bắt buộc, jsdom không thử được cuộn thật): bộ chọn câu hỏi trong hộp thoại Thêm block, trong ngăn kéo phần thi; Ngân hàng câu hỏi với hơn 100 câu; bộ lọc đổi giữa chừng; mạng chậm (DevTools throttling) và lỗi mạng khi tải thêm.

## Rủi ro

- Ô tìm của Ngân hàng câu hỏi thu hẹp về tìm theo mã (trước đó lọc trên các trường hiển thị của trang); ghi trong báo cáo bàn giao.
- Danh sách có nội dung cao bất thường (câu hỏi có đề dài) làm số dòng thấy được ít, tải thêm chạy thường xuyên: chấp nhận.
- `ExaminerReviewPage` có thể có làm mới theo thời gian (hàng chờ đổi liên tục); kiểm không làm danh sách nhảy khi `refresh` chạy.
