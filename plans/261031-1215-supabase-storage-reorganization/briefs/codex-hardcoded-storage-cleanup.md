# Brief Codex: bỏ các chỗ hard-code lưu file (làm sau phase 3 và phase 4)

Người làm: **Codex**. Claude review và kiểm theo `verification-upload-endpoints.md`. Không ghi Supabase, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Điều kiện bắt đầu

- Phase 3 đã merge (cùng vùng `ContentMediaController`, `Course`).
- Phase 4 đã merge (V90 đã áp, ảnh đã đổi sang khóa). Mục B chỉ làm sau đó.
- Mục A và C độc lập, làm được ngay sau phase 3.

## Nhánh và PR

- `feat/storage-hardcode-cleanup` từ `feat/supabase-storage-reorganization`, PR vào đó (draft). Có thể tách thành ba PR nếu muốn.

## A. Chỉ cho phép host project của mình (quyết định đã chốt)

Hiện tại:
- `frontend/src/features/community/components/ComposerMediaPanel.tsx:17`: `ALLOWED_IMAGE_HOSTS = ['images.unsplash.com', 'i.imgur.com', 'supabase.co']`. Cho phép mọi project Supabase.
- `frontend/src/features/community/components/PostCard.tsx:28`: cho phép mọi `*.supabase.co` cho video.

Yêu cầu:
- Host Supabase lấy từ một biến build của frontend (ví dụ `VITE_SUPABASE_MEDIA_HOST`), giá trị `kndrsdqzbzsaiuuruqno.supabase.co`. Đặt vào file env mẫu của frontend, không commit khóa.
- So khớp host đầy đủ, không so khớp đuôi. Không chấp nhận `evil.supabase.co` hay `kndrsdqzbzsaiuuruqno.supabase.co.evil.com`.
- Backend `AllowedMediaPolicy` đã kiểm host theo cấu hình; đối chiếu để hai bên cùng một quy tắc.
- Test: host đúng được nhận; host project khác bị từ chối; đuôi giả bị từ chối.

## B. Bỏ ảnh `data:` base64 ở backend (sau phase 4)

- `services/content-service/src/main/java/com/ieltspath/content/domain/vo/MediaReferencePolicy.java`: bỏ nhánh `IMAGE_DATA_URI`. Chỉ còn khóa tương đối và `https://`.
- Test: `data:image/...` bị từ chối với `InvalidMediaReferenceException`; khóa và `https://` vẫn chạy.
- Kiểm trước: truy vấn `content_assets` không còn dòng `data:` (đã đúng sau V90). Nếu còn, dừng và báo.

## C. Ảnh thẻ học của trang luyện tập lên server (quyết định đã chốt)

Hiện tại:
- `frontend/src/features/practice/lib/flashcardStorage.ts`: lưu thẻ, kể cả ảnh base64 (`image: data:image/...`), vào `localStorage`. Giới hạn khoảng 5 MB.

Yêu cầu:
- Trước khi làm: xác định trang luyện tập có dùng cùng dữ liệu với `learning-support/flashcards` (library-service) không. Nếu hai thứ là hai tính năng khác nhau, **dừng và báo `NEEDS_CONTEXT`**.
- Nếu cùng dữ liệu: ảnh đi qua `POST /api/learning-support/flashcards/images` (đã có, bucket `flashcards`), lưu khóa vào thẻ, không lưu base64.
- Dữ liệu `localStorage` cũ (đã có ảnh base64): đọc được, và khi người dùng lưu lại thì chuyển sang khóa. Không mất thẻ.
- Test: thêm thẻ có ảnh gọi upload và lưu khóa; thẻ cũ base64 vẫn hiển thị; ảnh lỗi (quá lớn, sai loại) báo lỗi, không mất thẻ.

## Không làm

- Không sửa bước ghi âm Speaking (đã nằm trong brief phase 8).
- Không đổi `AvatarReferences` và `AllowedMediaPolicy` (tiền tố Supabase là chấp nhận được hiện tại; ghi chú trong tài liệu).
- Không xóa dữ liệu localStorage cũ tự động.

## Lệnh kiểm chứng

- Backend: `mvn -B -ntp -pl services/content-service -am test`, và module community/library nếu chạm.
- Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run src/features/community src/features/practice`.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Trang luyện tập và `learning-support/flashcards` không rõ có chung dữ liệu.
- `content_assets` còn `data:` sau V90.
- Thiếu biến `VITE_SUPABASE_MEDIA_HOST` trong môi trường build.
