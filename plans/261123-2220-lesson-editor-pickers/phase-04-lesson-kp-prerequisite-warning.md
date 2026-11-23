# Phase 04: Cảnh báo KP tiên quyết còn thiếu ở ô Knowledge point của bài

Plan: [plan.md](plan.md) · Trạng thái: done · Chỉ frontend, độc lập với phase 01-03

Việc **6**. Quyết định 2026-11-23: cảnh báo kèm nút "Thêm tiên quyết", **không** tự chọn và **không** chặn Lưu.

## Context

- `frontend/src/features/admin/lessons/components/LessonKnowledgeField.tsx`: tải `fetchKnowledgeTree({})` rồi dựng `offered`; dùng ở `LessonEditorPanel.tsx` dòng ~116 (cấp bài) và `BlockEditor.tsx` dòng ~61 (cấp block Văn bản)
- `frontend/src/features/admin/components/KnowledgeSelectField.tsx`: bảng KP đã chọn, giới hạn mặc định `limit = 50`
- `frontend/src/features/admin/knowledge-graph/types.ts`: `knowledgeTreeSchema` = `{ nodes, edges }`; cạnh `{ from, to }` với `from` là KP tiên quyết, `to` là KP phụ thuộc
- `frontend/src/features/admin/lessons/components/LessonEditorPanel.tsx`: mẫu thông báo trạng thái `adm-alert` (`EXERCISE_LOCK_NOTE`, dòng ~180)
- `frontend/.sdd/constraints/admin-ui.md` và quy ước đã lưu: không thêm chữ hướng dẫn dưới ô; **trạng thái dữ liệu** như cảnh báo này thì được; công tắc cho một lựa chọn, nút cho hành động
- `backend/docs/contracts/knowledge-tree-v1.md`: cạnh chỉ nối KP đang hoạt động; tiên quyết không bao giờ có cấp CEFR cao hơn KP phụ thuộc

## Yêu cầu

1. **Tính thiếu.** Với mỗi KP trong `value` (cấp bài), lấy các cạnh có `to` là KP đó; tiên quyết nào (`from`) chưa nằm trong `value` thì tính là thiếu. Chỉ xét tiên quyết **trực tiếp**, một tầng. Kết quả: danh sách `{ point, missing: [point] }`.
2. **Hiện cảnh báo** ngay dưới bảng KP đã chọn, theo mẫu `adm-alert` (vai trò `note`, không dùng toast; không phải lỗi nhập nên không dùng chữ đỏ dưới ô). Nội dung nêu rõ KP nào thiếu tiên quyết nào, dùng mã KP (tên đầy đủ trong `title`), tối đa vài dòng rồi "và n KP khác". Không hiện khi không thiếu gì.
3. **Nút "Thêm tiên quyết".** Thêm toàn bộ tiên quyết còn thiếu vào `value` (đã loại trùng). Nếu thêm hết sẽ vượt giới hạn 50 KP thì thêm đến khi đủ 50, và cảnh báo vẫn hiện phần còn thiếu; không im lặng cắt.
4. **Không chặn Lưu.** Cảnh báo chỉ để admin cân nhắc; một KP tiên quyết có thể đã được dạy ở bài trước.
5. **Chỉ cấp bài.** `LessonKnowledgeField` nhận prop tùy chọn bật cảnh báo (ví dụ `warnMissingPrerequisites`); `LessonEditorPanel` bật, `BlockEditor` **không** bật (tiên quyết có thể nằm ở block khác của cùng bài).
6. **Không thêm lời gọi mạng.** Dùng chính `knowledge.data.edges` mà `LessonKnowledgeField` đã tải. Đang tải hoặc lỗi tải cây thì không hiện cảnh báo.
7. **Trạng thái cũ của dữ liệu.** KP trong `value` không còn trong cây (ngừng hoạt động) không có cạnh nên không phát cảnh báo; tiên quyết đang ngừng hoạt động không có trong cạnh nên không gợi ý. Chấp nhận, ghi chú trong báo cáo.

## Thiết kế

- Hàm thuần `missingPrerequisites(value, edges)` đặt trong `lessons/lib/` (ví dụ `knowledge-prerequisites.ts`) để test gọn; trả mảng theo thứ tự `value`, tiên quyết sắp theo mã A→Z. Độ phức tạp O(số cạnh + KP đã chọn): dựng `Map<to, from[]>` một lần, không lặp lồng nhau.
- Thành phần trình bày nhỏ `KnowledgePrerequisiteNote` cạnh `LessonKnowledgeField`; chỉ tách file nếu `LessonKnowledgeField.tsx` phình ra.
- `KnowledgeSelectField` **không đổi** (dùng ở cả hộp thoại khác như dựng course); cảnh báo vẽ ở `LessonKnowledgeField`, bên ngoài bảng.
- Cập nhật `value` qua `onChange` có sẵn, không thêm trạng thái ngoài.

## Files sửa / tạo

- Sửa `LessonKnowledgeField.tsx` (prop mới, vẽ cảnh báo), `LessonEditorPanel.tsx` (bật prop), `lessons.css` nếu cần chút khoảng cách (dùng token có sẵn, không mã màu cứng).
- Tạo `lessons/lib/knowledge-prerequisites.ts` và `knowledge-prerequisites.test.ts`.
- Không đổi backend, không đổi `knowledge-graph/`.

## Test

- Hàm thuần: không cạnh thì rỗng; KP có tiên quyết đã chọn đủ thì rỗng; thiếu một hoặc nhiều; hai KP cùng cần một tiên quyết thì tiên quyết chỉ hiện một lần ở danh sách thêm; tiên quyết nằm ngoài `value` nhưng KP phụ thuộc cũng ngoài `value` thì không báo (chỉ xét `to` thuộc `value`).
- `LessonEditorDrawer.test.tsx` (hoặc test của `LessonKnowledgeField`): chọn KP có tiên quyết thì hiện cảnh báo đúng mã; bấm "Thêm tiên quyết" thì tiên quyết vào bảng và cảnh báo biến mất; vượt 50 thì chỉ thêm đủ 50 và còn cảnh báo; block Văn bản **không** hiện cảnh báo.
- Cảnh báo không làm đổi payload Lưu khi admin bỏ qua nó.
- Chạy `npm test -- src/features/admin/lessons`, `npm run lint`, `npm run typecheck`, `npm run build`. Ngày 2026-11-23 vitest trong máy này từng hỏng ở `src/test/setup.ts` cho mọi test; nếu còn thì báo rõ, không tuyên bố đã chạy.
- Thử tay: bài mới, chọn một KP có tiên quyết (dùng `L80_DENSE_LECTURE` hoặc KP bất kỳ có cạnh trong cây), bấm "Thêm tiên quyết", lưu, mở lại bài.

## Rủi ro

- Cảnh báo nhiễu: KP có nhiều tiên quyết có thể làm cảnh báo dài. Giới hạn số dòng hiển thị và gộp "và n KP khác".
- Cây tri thức lớn: `edges` có thể nhiều nghìn cạnh; vẫn xử lý một lần theo `Map`, không ảnh hưởng hiệu năng đáng kể.
- Hiểu lầm "thiếu = sai": nút và cảnh báo chỉ gợi ý; không chặn Lưu và không đổi dữ liệu nếu admin không bấm.
- Rollback: gỡ prop và hàm mới; không dữ liệu hay API nào bị đổi.
