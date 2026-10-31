# Brief Codex: phase 2, tài liệu bucket

Người làm: **Codex**. Claude review. Chỉ sửa tài liệu, không đổi code.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Nhánh và PR

- `feat/storage-p2-docs` từ `feat/supabase-storage-reorganization`, PR vào đó.

## Đã làm trước (không làm lại)

- `services/content-service/README.md`: đã sửa đoạn "không có upload API" (commit `de1bec41`).
- `shared/common-storage/README.md`: đã có bảng bucket, URL mẫu và quy tắc khóa (commit `1aca55e1`).

## Việc cần làm

1. `backend/docs/system-architecture.md`: thêm mục "Lưu trữ file (Supabase Storage)". Nội dung:
   - Bảng bucket giống bảng trong `shared/common-storage/README.md` (cùng cột, cùng trạng thái). Copy đúng, không viết lại khác.
   - Một đoạn nguyên tắc: cùng chủ và cùng chính sách thì chung bucket, phân biệt bằng tiền tố khóa; khác chính sách thì bucket mới; khác service thì không chung bucket.
   - Link tới `shared/common-storage/README.md` cho URL mẫu.
2. Kiểm nhất quán tên biến bucket giữa `infra/config-server/config-repo/*.yaml`, `docker-compose.yml` và tài liệu: `USER_AVATAR_BUCKET`, `LIBRARY_FLASHCARD_BUCKET`, `COMMUNITY_MEDIA_BUCKET`, `CONTENT_MEDIA_BUCKET`. Nếu lệch, **báo trong `Concerns`**, không sửa config.
3. `services/content-service/README.md`: đọc lại đoạn bảng 24 file. Nếu còn câu nào nói file "chưa có" trong khi phase 1 đã xong thì ghi lại; không đổi số liệu.

## Không làm

- Không sửa code, config, compose, migration.
- Không ghi Supabase. Không chạy truy vấn Supabase. Mọi số liệu bucket lấy từ bảng đã có, không tự kiểm.
- Không ghi mã plan, số phase trong tài liệu sản phẩm (chỉ ghi trong thư mục plan).

## Kiểm chứng

- Mở từng link trong mục mới và xác nhận đường dẫn tồn tại (`ls` hoặc `Glob`).
- Không có lệnh build. Ghi trong `Summary` những gì đã đối chiếu.

## Tiêu chí xong

- `docs/system-architecture.md` có mục bucket, nội dung khớp `common-storage/README.md`.
- Mọi link hợp lệ; mục bucket không nói bucket nào đã có khi thực tế chưa có (`media-premium` ghi "kế hoạch").

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Tên biến bucket lệch giữa các nguồn: báo bảng so sánh, không tự chọn.
