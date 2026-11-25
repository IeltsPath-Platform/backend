# Phase 02: Form admin thêm slide bằng PDF

Plan: [plan.md](plan.md) · Trạng thái: done · Phụ thuộc: phase 01

## Context

- `frontend/src/features/admin/lessons/components/LessonResourcesEditor.tsx` (đọc cả file trước khi sửa)
- `frontend/src/features/admin/media/api/mediaUploadApi.ts` (`uploadAdminDocument`, `documentFileProblem`, `documentErrorMessage`)
- `frontend/.sdd/constraints/admin-ui.md`: quy ước UI admin (đọc trước khi sửa)
- Quy ước đã lưu: không thêm chữ hướng dẫn dưới ô; lỗi nhập là chữ đỏ dưới ô bằng zod, không toast; lỗi backend mới hiện toast.

## Yêu cầu

- "Thêm slide" giống "Thêm tài liệu": ô "Tệp PDF" bắt buộc (dấu `*` đỏ), tiêu đề mặc định lấy từ tên tệp.
- Không còn ô "URL slide".
- Sửa slide chỉ đổi tiêu đề (tệp giữ nguyên), như tài liệu đã có tệp.
- Bảng slide hiển thị link "Tệp PDF" như hàng tài liệu có tệp; cột đổi tên thành "Tệp".

## Thay đổi trong `LessonResourcesEditor.tsx`

- `ResourceForm`: `needsFile = !item` (mọi loại); `needsUrl = type === 'DOCUMENT' && Boolean(item) && !item.fileKey` (chỉ tài liệu link cũ khi sửa); `storedFile` giữ nguyên.
- `label`/`resourceSchema`: không đổi cấu trúc; schema slide không còn `url`.
- Tiêu đề cột bảng: `type === 'DOCUMENT' ? 'Tệp / đường dẫn' : 'Tệp'`.
- Nhánh hiển thị hàng: bỏ nhánh link cho slide không cần sửa riêng (slide luôn có `fileKey`); giữ nhánh link cho tài liệu cũ.
- `onSave` thêm: đã đúng (nhánh `file` đã gọi `uploadAdminDocument` rồi `createLessonResource` với `fileKey`, không phụ thuộc loại).
- Cập nhật comment đầu `ResourceForm`: "A slide and a new document are PDFs the author uploads".
- `frontend/src/features/admin/lessons/types.ts`: kiểm kiểu `LessonResource` (`url` có thể rỗng khi có `fileKey`) và `lessonApi.ts` `CreateLessonResourceInput` đã nhận `fileKey` cho `SLIDE` chưa; chỉ sửa nếu kiểu đang chặn.

## Test

- Sửa `LessonResourcesEditor.test.tsx` và `LessonResourcesEditor.reorder.test.tsx`: dữ liệu slide dùng `fileKey`; thêm ca "thêm slide: chọn PDF → upload → tạo", "không chọn tệp → lỗi đỏ dưới ô", "tệp không phải PDF hoặc quá 20 MB → lỗi đỏ".
- Kiểm `LessonEditorDrawer.test.tsx`, `TopicLessonsDrawer.test.tsx` nếu dữ liệu mẫu có slide link.
- Chạy `npx vitest run src/features/admin/lessons`, rồi `npm run lint`, `npm run typecheck`.

## Rủi ro

- Slide cũ dạng link không còn sau V148 nên không cần nhánh tương thích lâu dài; nhưng giữa lúc frontend mới và backend cũ (chưa chạy V148) form vẫn đúng vì backend cũ từ chối slide có `fileKey`. Triển khai backend trước hoặc cùng lúc.
