---
phase: 2
title: "API upload ở content-service (sau MVP)"
status: pending
priority: P3
---

# Phase 2: API upload ở content-service (sau MVP)

**Hướng đi đã được chốt ngày 2026-10-19: không upload tay nữa, mọi file phải qua API** (xem mục "Cập nhật" trong `plan.md`).
Phần client Supabase dùng chung nằm ở `shared/common-storage`. Endpoint của content-service vẫn cần plan riêng và người dùng duyệt (AGENTS.md §2, §5).

## Mục tiêu

Admin hoặc `CONTENT_AUTHOR` tải mp3/ảnh lên bucket qua hệ thống thay vì Dashboard. Thuộc `content-service`, không tạo service mới.

## Chọn cách upload (quyết định cần chốt)

| | A. Qua backend | B. Signed upload URL |
| --- | --- | --- |
| Luồng | Trình duyệt → content-service → Supabase | Trình duyệt xin URL → tải thẳng lên Supabase → báo content-service |
| Ưu | Kiểm tra file tại chỗ, khóa không rời server | Không tốn băng thông server, hợp file lớn |
| Nhược | Tốn băng thông/RAM, bị giới hạn kích thước request của Gateway và Spring (phải stream) | Phải xác nhận sau khi tải (tồn tại, đúng loại, kích thước); có thể sinh file mồ côi |

Khuyến nghị: A cho file nhỏ do admin tải; B khi file lớn hoặc nhiều người tải.

## Thiết kế (theo layer của AGENTS.md)

- `application/port`: `MediaStoragePort` (ví dụ `store(key, contentType, stream)`, hoặc `createUploadTarget(key, contentType)` cho cách B).
- Không viết adapter Supabase riêng: dùng `ObjectStorage` của `shared/common-storage` (bật `app.storage.enabled`, `SUPABASE_URL`, `SUPABASE_SECRET_KEY`). Content-service chỉ giữ `MediaStoragePort` mỏng nếu cần tách domain khỏi thư viện.
- `application/usecase`: `UploadMediaAssetUseCase`: kiểm quyền (`ADMIN`, `CONTENT_AUTHOR`), kiểm MIME và kích thước theo `CONTENT_MEDIA_ALLOWED_TYPES`, `CONTENT_MEDIA_MAX_BYTES`, sinh key an toàn (không dùng tên file do client gửi), rồi lưu key vào `content_assets.media_reference`.
- `api`: controller gọi use case; lỗi qua `GlobalExceptionHandler`/`ErrorResponse`. Forward `X-Correlation-Id`, không log khóa hay nội dung file.
- Route mới ở Gateway chỉ khi cần; không đổi route hiện có.

## Biến

Xem bảng trong `plan.md` và mục "Cập nhật 2026-10-19": `SUPABASE_URL`, `SUPABASE_SECRET_KEY` (chỉ `.env`), tên bucket theo từng service (thay cho `SUPABASE_STORAGE_BUCKET`), `CONTENT_MEDIA_MAX_BYTES`, `CONTENT_MEDIA_ALLOWED_TYPES`. Không có `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` (Supabase không cho đặt TTL cho signed upload URL).

## Kiểm tra

- Mockito cho use case (từ chối MIME sai, quá kích thước, sai quyền).
- `@WebMvcTest` cho controller và security (CUSTOMER bị 403).
- Adapter test bằng stub HTTP cục bộ, không gọi Supabase thật.
- Không log hay trả về khóa; `git grep` sạch.
