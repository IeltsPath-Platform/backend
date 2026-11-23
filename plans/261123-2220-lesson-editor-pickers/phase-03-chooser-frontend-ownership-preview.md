# Phase 03: Frontend, dùng trạng thái "đã có chủ", đề rút gọn và Knowledge point

Plan: [plan.md](plan.md) · Trạng thái: done · Phụ thuộc: phase 02 (contract), nên làm sau phase 01

Việc: **1** (làm mờ và công tắc lọc) và **3** (đề và Knowledge point), phía giao diện.

## Context

- `frontend/src/features/admin/lessons/components/QuestionChooser.tsx` (sau phase 01) và `BlockEditor.tsx`
- `frontend/src/features/admin/questions/types.ts` (`questionSchema`) và `questions/api/questionApi.ts` (`listQuestions`)
- `frontend/src/features/admin/lessons/types.ts` (`LessonBlock['questions']`)
- `backend/docs/contracts/question-publication-v1.md` sau phase 02
- `frontend/src/features/admin/packages/components/SectionQuestionsEditor.tsx`: nơi dùng thứ hai, **không đổi hành vi**
- `frontend/.sdd/constraints/admin-ui.md`, quy ước đã lưu: không thêm chữ hướng dẫn dưới ô; trạng thái dữ liệu (nhãn "Đã dùng...") thì được.

## Yêu cầu

1. **Câu đã có chủ.** Chooser nhận prop tùy chọn `blockId` (block đang sửa) và bật `include=ownership`. Dòng có `ownedElsewhere` thì mờ, checkbox bị vô hiệu hóa, kèm nhãn "Đã dùng ở nơi khác" (chung, vì câu có thể nằm ở block bài học khác hoặc ở phần thi trong gói đề), **trừ** khi `versionId` đang nằm trong `value` (câu đã chọn cho block này, kể cả khi vừa tick trong lúc chưa lưu). Backend vẫn là nơi quyết định cuối cùng khi Lưu.
1b. **Công tắc "Chỉ hiện câu còn trống".** Dùng `AdminSwitch` (quy ước: một lựa chọn bật/tắt thì dùng công tắc, không dùng checkbox), nhãn có sẵn trên công tắc, không thêm chữ giải thích. Bật thì gửi `onlyFree=true` (kèm `exceptBlockId`), về trang 0, và nhớ lựa chọn trong lúc form còn mở. Mặc định **tắt** để admin thấy cả câu đã dùng (đang mờ). Chỉ hiện công tắc ở nơi bật `include=ownership` (tức form block bài tập, không phải `SectionQuestionsEditor`).
2. **Đề rút gọn và Knowledge point.** Bật `include=preview`. Mỗi dòng hiện `stemExcerpt` (tối đa hai dòng, cắt gọn bằng CSS) và các mã Knowledge point dạng nhãn nhỏ, tên hiện trong `title`; nếu có `knowledgePointTotal`, thêm "+n". Thiếu đề thì không hiện dòng đề.
3. **"Xem nội dung".** Giữ nút để xem đề đầy đủ (`getQuestion` như hiện nay); đổi chữ thành "Xem đủ" hoặc giữ nguyên, theo `admin-ui.md`.
4. **Nhãn câu đã chọn.** Mục "Câu hỏi đã chọn" dùng mã và đoạn đầu của đề nếu đã tải; bảng nhãn cộng dồn của phase 01 lưu thêm `stemExcerpt`.
5. **Hai nơi dùng giữ hành vi.** `BlockEditor` truyền `blockId={block?.id}` và bật cả hai `include`. `SectionQuestionsEditor` không truyền gì, nên **không** gửi `include`, không mờ câu nào, không đổi gì.
6. Cập nhật `questionSchema` bằng các trường mới dạng `nullish`/mảng mặc định rỗng để `QuestionBankPage` và test cũ không vỡ.

## Files sửa

- `QuestionChooser.tsx`, `BlockEditor.tsx`, `lessons.css` (nhãn Knowledge point, dòng mờ), `questions/types.ts`, `questions/api/questionApi.ts` (kiểu `QuestionFilters` thêm `include`, `exceptBlockId`; không đụng `knowledgePointId` cũ).
- Test: `QuestionChooser.test.tsx`, `BlockEditor` (nếu có), `SectionQuestionsEditor.test`, `questionApi.test`.

## Test

- Dòng `ownedElsewhere=true` bị vô hiệu hóa và có nhãn; nếu `versionId` nằm trong `value` thì không bị vô hiệu hóa.
- Khi sửa block có sẵn câu A: câu A không mờ dù backend trả `ownedElsewhere` cho phần còn lại (kiểm `exceptBlockId` được gửi).
- Công tắc bật: request có `onlyFree=true` và `page=0`; tắt: không có `onlyFree`. `SectionQuestionsEditor` không có công tắc.
- Dòng hiện `stemExcerpt` và mã Knowledge point; "+n" khi có `knowledgePointTotal`.
- `SectionQuestionsEditor`: request **không** có `include`; chọn một câu như cũ.
- `QuestionBankPage` vẫn parse được phản hồi không có trường mới.
- Chạy `npm test -- src/features/admin`, `npm run lint`, `npm run typecheck`, `npm run build`. Nếu vitest còn hỏng ở `setup.ts` thì báo rõ.
- Thử tay: bài Listening có câu đã dùng ở bài khác; sửa block có sẵn câu; thêm block mới; mở phần thi trong gói đề.

## Rủi ro

- Phase 02 chưa lên môi trường thì `include` bị bỏ qua và giao diện phải xử lý trường thiếu một cách êm (dòng không mờ, không có đề), không báo lỗi.
- Mờ dòng chỉ là gợi ý; không được thay cho kiểm tra ở backend.
