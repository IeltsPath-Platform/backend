---
phase: 2
title: "API upload ở content-service (sau MVP)"
status: pending
priority: P3
---

# Phase 2: API upload ở content-service (sau MVP)

**Cần người dùng duyệt trước khi làm** (thêm tích hợp dịch vụ ngoài và biến bí mật mới; AGENTS.md §2, §5).

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
- `infrastructure/client`: adapter gọi Supabase Storage bằng `RestClient` sẵn có (không thêm SDK), header khóa lấy từ `SUPABASE_SECRET_KEY`.
- `application/usecase`: `UploadMediaAssetUseCase`: kiểm quyền (`ADMIN`, `CONTENT_AUTHOR`), kiểm MIME và kích thước theo `CONTENT_MEDIA_ALLOWED_TYPES`, `CONTENT_MEDIA_MAX_BYTES`, sinh key an toàn (không dùng tên file do client gửi), rồi lưu key vào `content_assets.media_reference`.
- `api`: controller gọi use case; lỗi qua `GlobalExceptionHandler`/`ErrorResponse`. Forward `X-Correlation-Id`, không log khóa hay nội dung file.
- Route mới ở Gateway chỉ khi cần; không đổi route hiện có.

## Biến

Xem bảng trong `plan.md`: `SUPABASE_URL`, `SUPABASE_STORAGE_BUCKET`, `SUPABASE_SECRET_KEY` (chỉ `.env`), `CONTENT_MEDIA_MAX_BYTES`, `CONTENT_MEDIA_ALLOWED_TYPES`, và `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` nếu chọn cách B.

## Kiểm tra

- Mockito cho use case (từ chối MIME sai, quá kích thước, sai quyền).
- `@WebMvcTest` cho controller và security (CUSTOMER bị 403).
- Adapter test bằng stub HTTP cục bộ, không gọi Supabase thật.
- Không log hay trả về khóa; `git grep` sạch.
