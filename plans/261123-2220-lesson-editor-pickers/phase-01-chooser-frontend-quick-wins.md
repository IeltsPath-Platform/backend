# Phase 01: Frontend, ba việc không cần backend

Plan: [plan.md](plan.md) · Trạng thái: done · Làm trước, giao độc lập

Việc: **2** lọc kỹ năng mặc định, **4** tìm theo mã và phân trang, **5** bố cục. Cộng thêm: hiện `code` thay cho 8 ký tự ID.

## Context

- `frontend/src/features/admin/lessons/components/QuestionChooser.tsx` (đọc cả file; test `QuestionChooser.test.tsx`)
- `frontend/src/features/admin/lessons/components/BlockEditor.tsx` dòng 80: nơi dùng cho bài học, đã có `skill` của chủ đề
- `frontend/src/features/admin/packages/components/SectionQuestionsEditor.tsx` dòng 90: nơi dùng thứ hai, chọn một câu, `purpose` `EXAM`/`PLACEMENT`
- `frontend/src/features/admin/questions/api/questionApi.ts` (`listQuestions` đã nhận `code`, `page`, `size`) và `questions/types.ts` (`QuestionFilters`, `questionPageSchema` có `totalElements`, `totalPages`)
- `frontend/src/features/admin/lessons/lessons.css` dòng 48-52 (`.ls-questions*`) và mẫu bố cục `.ls-vocab` (cột flex, `min-height: 0`, vùng cuộn)
- `frontend/.sdd/constraints/admin-ui.md`: quy ước UI admin, đọc trước khi sửa. Quy ước đã lưu: không thêm chữ hướng dẫn dưới ô; tìm kiếm/lọc là ô nhập có nhãn.

## Yêu cầu

1. **Kỹ năng mặc định.** `QuestionChooser` nhận prop tùy chọn `defaultSkill`; trạng thái `skill` khởi tạo bằng nó nếu thuộc `SKILLS`, ngược lại rỗng. `BlockEditor` truyền `skill` của chủ đề. `SectionQuestionsEditor` không truyền, nên giữ "Tất cả" như trước. Ô lọc vẫn đổi được (một bài có nhiều kỹ năng).
2. **Hiện mã.** Dòng hiện `code` của câu hỏi thay cho `id.slice(0, 8)`; nhãn trong mục "Câu hỏi đã chọn" cũng dùng `code`.
3. **Tìm theo mã.** Ô "Tìm theo mã câu hỏi" gửi tham số `code` (debounce khoảng 300 ms, quay về trang đầu khi đổi). Khớp một phần, không phân biệt hoa thường (theo contract).
4. **Phân trang.** Cỡ trang 25. Hiện "Trang n / tổng" với nút Trước, Sau và tổng số câu. Dùng component phân trang admin có sẵn nếu có (xem `QuestionBankPage.tsx`); nếu chưa có thì làm gọn trong chooser, không tạo component dùng chung mới.
5. **Nhớ nhãn câu đã chọn.** Câu chọn ở trang khác, hoặc bị lọc mất khỏi danh sách hiện tại, vẫn phải hiện đúng mã trong "Câu hỏi đã chọn": giữ một bảng `versionId → nhãn` cộng dồn mỗi khi tải trang mới (hiện chỉ dựng từ trang đang xem nên rơi về "Phiên bản abcd1234").
6. **Bố cục.** Bỏ `max-height: 280px` cố định của `.ls-questions__list`; vùng danh sách chiếm phần chiều cao còn lại của panel và tự cuộn (theo mẫu `.ls-vocab`). Phần "Câu hỏi đã chọn" có chiều cao giới hạn và cuộn riêng để không đẩy danh sách xuống. Kiểm cả panel hẹp và cao thấp.

## Files sửa

- `QuestionChooser.tsx`, `lessons.css`, `BlockEditor.tsx` (truyền `defaultSkill`).
- `QuestionChooser.test.tsx`: cập nhật.
- Không đổi `SectionQuestionsEditor.tsx`; chỉ kiểm tra hành vi giữ nguyên.
- Không đổi `questionApi.ts` (đã đủ tham số).

## Test

- Mặc định lọc theo `defaultSkill`; không có `defaultSkill` thì "Tất cả".
- Gõ mã gửi đúng `code` và về trang 0; đổi trang gửi đúng `page`.
- Chọn câu ở trang 1, sang trang 2: câu đó vẫn nằm trong mục "đã chọn" với mã đúng.
- `SectionQuestionsEditor.test`: vẫn chọn được đúng một câu, không bị lọc kỹ năng.
- Test hồi quy cho bài đã khóa (không viết code mới): ở bài không còn `DRAFT`, `LessonEditorPanel` hiện ghi chú `EXERCISE_LOCK_NOTE`, nút "Bài tập" bị vô hiệu hóa kèm `title`, và block bài tập có sẵn không sửa hay xóa được (`LessonEditorPanel.tsx` dòng 175-202, 257). Nếu đã có test đúng ý ở `LessonEditorDrawer.test.tsx` thì chỉ ghi nhận, không thêm trùng.
- Chạy `npm test -- src/features/admin/lessons src/features/admin/packages`, `npm run lint`, `npm run typecheck`, `npm run build`. Ghi chú: ngày 2026-11-23 vitest trong máy này từng hỏng ở `src/test/setup.ts` cho mọi test; nếu còn thì báo rõ, không tuyên bố đã chạy.
- Thử tay trên trình duyệt: bài Listening, bài có hơn 25 câu xuất bản, panel hẹp.

## Rủi ro

- Đổi tính năng dùng chung: giữ prop mới tùy chọn để nơi dùng thứ hai không đổi.
- Debounce và hủy yêu cầu cũ (`AbortSignal`) để kết quả cũ không ghi đè kết quả mới; `useAdminResource` đã nhận `signal`.
