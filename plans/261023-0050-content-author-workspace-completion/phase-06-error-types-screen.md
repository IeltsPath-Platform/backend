# Phase 06: Màn "Mã lỗi"

Người làm: **Codex** (đổi từ Claude Code ngày 2026-10-23). Nhánh: `feat/content-author-screens` (frontend, worktree `../frontend-claude`).
Trạng thái: In progress — giao diện đã triển khai, lượt này giữ nguyên source; toàn bộ suite còn 2 lỗi flashcard ngoài phạm vi, checklist thật còn chờ gateway.

Repo: `frontend`. Phụ thuộc: không (API đã có). Làm cuối vì ít tác động nhất.

## Mục tiêu

Tác giả quản lý danh mục mã lỗi (lỗi học viên hay mắc, gắn với KP và gợi ý sửa) dùng cho chấm và chẩn đoán: tạo, sửa, duyệt, ngừng dùng.

## API (đã có, `ErrorTypeController`)

- `GET /api/content/error-types?activeOnly=false` → `[{ id, code, name, category, severity, knowledgePointId, fixHint, status, reviewStatus }]`
- `POST /api/content/error-types` body `{ code, name, category, severity (1..3), knowledgePointId?, fixHint }` → `201`
- `PUT /api/content/error-types/{id}` body `{ name, category, severity, knowledgePointId?, fixHint, status?: ACTIVE|INACTIVE, reviewStatus?: DRAFT|APPROVED }` (không đổi được `code`)
- Enum: `category` = `GRAMMAR | VOCABULARY | COHERENCE | TASK | READING_STRATEGY | LISTENING_STRATEGY | SPELLING_NUMBER` (đã kiểm migration: seed có `SPELLING`, `NUMBER_FORMAT`, `PLURAL_FORM` thuộc `SPELLING_NUMBER`; thiếu giá trị này thì schema Zod sẽ từ chối cả danh sách); `status` = `ACTIVE | INACTIVE`; `reviewStatus` = `DRAFT | APPROVED`.

Đọc `CreateErrorTypeUseCase`/`UpdateErrorTypeUseCase` để biết định dạng `code` hợp lệ và lỗi trùng mã trả gì; hiển thị đúng.

## Tạo

```text
src/features/admin/error-types/
  types.ts, api/errorTypeApi.ts, lib/errorTypeLabels.ts
  pages/ErrorTypesPage.tsx, components/ErrorTypeForm.tsx
  pages/ErrorTypesPage.test.tsx
```

- Menu: `{ path: '/content-author/error-types', label: 'Mã lỗi', icon: Bug, roles: ['CONTENT_AUTHOR'] }`. Route trong nhánh `isAuthorArea`.
- Bảng: Mã, Tên, Nhóm (nhãn tiếng Việt), Mức (1 nhẹ / 2 vừa / 3 nặng, có chữ, không chỉ màu), KP gắn kèm (tên KP), Duyệt (badge), Trạng thái. Lọc theo nhóm, trạng thái duyệt, ô tìm mã/tên (lọc phía client vì API trả cả danh sách).
- `ErrorTypeForm` dùng cho tạo và sửa: chọn KP bằng `questions/components/KnowledgePointPicker.tsx` (dùng lại, không viết mới); `code` khóa khi sửa. Nút nhanh trên dòng: "Duyệt" (gửi `reviewStatus: APPROVED` kèm các trường hiện tại), "Ngừng dùng"/"Dùng lại".
- Nhãn nhóm: Ngữ pháp, Từ vựng, Mạch lạc, Đáp ứng đề, Chiến lược đọc, Chiến lược nghe, Chính tả & số.

## Test

Danh sách và lọc client; tạo gửi đúng body; sửa không gửi `code`; "Duyệt" gửi đủ trường + `reviewStatus: 'APPROVED'`; lỗi trùng mã hiện rõ; menu chỉ CONTENT_AUTHOR.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh; `frontend/README.md` cập nhật danh sách màn khu `/content-author` sau khi phase cuối cùng xong.

## Kết quả

- 2026-10-23, nhánh frontend `feat/content-author-screens`, worktree `frontend-claude`: thêm danh mục, lọc nhóm/duyệt/mã/tên phía client; tạo/sửa controlled, khóa code khi sửa; dùng lại KnowledgePointPicker với tối đa một KP, hiện tên KP, duyệt/ngừng dùng/dùng lại gửi đủ trường hiện tại. Schema có SPELLING_NUMBER. Menu/route chỉ CONTENT_AUTHOR.
- Đã đọc CreateErrorTypeUseCase/UpdateErrorTypeUseCase/ErrorType và controller/exception handler: mã `^[A-Z][A-Z0-9_]*$`, dài tối đa 60, name tối đa 255; severity 1–3, fixHint bắt buộc. Trùng mã trả 400 và message DuplicateCodeException, không có machine code; dịch đúng dạng response này thay vì đoán mã lỗi.
- 13 test mã lỗi/menu qua; typecheck, lint, build qua; npm test sau tích hợp: 95 file / 573 test qua. Build còn cảnh báo bundle lớn sẵn có; không thêm dependency hoặc đổi cấu hình build.
- Chưa chạy tạo/sửa/duyệt/ngừng dùng qua tài khoản content@gmail.com hoặc kiểm 390px: gateway 8080/18080 không kết nối được khi kiểm ngoài sandbox. Không tuyên bố đã kiểm thủ công.
- README của worktree sẽ cập nhật danh sách màn một lần khi nhánh frontend cuối cùng hoàn tất, theo plan mục Phân công và phối hợp/Việc cuối; phase 02/13/15 còn bị chặn. Không sửa clone frontend chính.
- Đã push commit riêng `08fcc09 feat(error-types): manage error taxonomy and reviews`; [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29). Reviewer không tìm thấy thêm lỗi quyền truy cập, payload hoặc response của màn này.
- Lượt kiểm cuối: typecheck/lint/build qua; npm test 577 qua / 1 lỗi flashcard tired ngẫu nhiên ngoài phạm vi (`owl-reactions.test.ts:109`). Đây là kết quả cuối thay cho lượt xanh 573 test trước đó; không sửa/bỏ test ngoài phạm vi, không tuyên bố hoàn tất mọi cổng kiểm chứng hoặc tự merge PR.
- Lượt mới nhất 2026-10-23: giữ nguyên toàn bộ code màn mã lỗi, chỉ kiểm lại. `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua; 78 test liên quan author/menu/route (gồm danh mục mã lỗi) qua. Toàn bộ `npm.cmd test`: 94/95 file qua, 580 test qua / 1 lỗi flashcard confused tại `owl-reactions.test.ts:87`, ngoài phạm vi; không sửa hoặc bỏ test.
- Checklist còn lại chưa chạy: tạo, sửa, duyệt, ngừng dùng/dùng lại bằng content@gmail.com với backend thật; kiểm nhãn KP/dữ liệu sau tải lại và màn 390px. Gateway 8080 và 18080 timeout khi kiểm ngoài sandbox, nên không tuyên bố đã thực hiện các bước này. README tiếp tục dành cho nhánh frontend cuối cùng theo plan; không sửa clone chính hoặc backend code.
- Lượt này không có commit sửa màn mã lỗi. [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) vẫn nháp; các cập nhật mới chỉ thuộc slide/tài liệu, thứ tự chủ đề và nhãn phiên bản rubric.

### Cập nhật hiện tại 2026-10-23 — giữ nguyên màn mã lỗi

- Lượt hiện tại không đổi source màn mã lỗi, không có commit mới riêng cho màn này. Các kết quả test/commit/push ở trên là lịch sử.
- Kiểm chứng cuối lượt: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0). Toàn bộ `npm.cmd test` exit 1: 102/103 file qua, 670/672 test qua; 2 lỗi ngẫu nhiên đã biết ở `flashcards/lib/owl-reactions.test.ts:87` (confused) và `:109` (tired), ngoài phạm vi. Không sửa/bỏ test, không chạy lại để đổi kết quả. Build còn cảnh báo kích thước chunk sẵn có; không tuyên bố tất cả cổng xanh hoặc phase hoàn thành.
- Gateway 8080/18080 timeout trong lượt này; tạo/sửa/duyệt/ngừng dùng/dùng lại, kiểm tên KP/dữ liệu sau tải lại và màn 390px vẫn chưa chạy. Không có API mã lỗi cần backend triển khai thêm theo phạm vi hiện tại.
- [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) vẫn nháp. Chỉ cho phép commit local các thay đổi mới trong lượt này; không push/merge. Các file phase được cập nhật local cho backend owner, không commit vào backend.
