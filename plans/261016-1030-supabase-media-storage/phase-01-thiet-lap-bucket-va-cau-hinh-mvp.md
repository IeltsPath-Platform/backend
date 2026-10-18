---
phase: 1
title: "Thiết lập bucket và cấu hình MVP"
status: pending
priority: P2
---

# Phase 1: Thiết lập bucket và cấu hình MVP

## Mục tiêu

Có bucket Supabase public chứa mp3 và ảnh; content-service ghép được `mediaUrl` từ key. Không code mới ngoài phần ghép URL đã có trong plan Listening phase 2.

## Việc cần làm

1. Tạo project Supabase; Storage → New bucket, tên ví dụ `ieltspath-media`, bật **Public bucket**.
2. Giới hạn kích thước file và loại file (MIME) của bucket cho khớp `audio/mpeg` và các ảnh dùng trong seed.
3. Upload file theo key trong `seed-content.md` (ví dụ `listening/demo/ls1.mp3`). Tên key chữ thường, không dấu cách, không `..`, không bắt đầu bằng `/`.
4. Lấy URL cơ sở: `https://<project-ref>.supabase.co/storage/v1/object/public/<bucket>/`.
5. Đặt `CONTENT_MEDIA_BASE_URL` trong `.env` gốc; config-repo content đọc qua `content.media.base-url` (plan Listening phase 2). Không thêm giá trị mặc định chứa URL thật.
6. README content-service ghi danh sách key và biến cấu hình (chỉ tên biến).

## Kiểm tra

- `curl -I <base>listening/demo/ls1.mp3` trả `200`, `Content-Type: audio/mpeg`, có `Accept-Ranges: bytes`.
- Gửi `Range: bytes=0-99` nhận `206`.
- Content trả `mediaUrl` đúng bằng base + key; base rỗng mà gặp key thì `INVALID_MEDIA_REFERENCE` (hành vi đã có trong plan Listening).
- Đổi `CONTENT_MEDIA_BASE_URL` thì URL trả ra đổi theo, không cần migration.
- Không file nào chứa khóa Supabase; `git grep` không thấy giá trị bí mật.

## Ngoài phạm vi

Upload API, signed URL, giới hạn lượt nghe, RLS policy cho ghi (chỉ người có khóa server hoặc Dashboard mới ghi được vào bucket).
