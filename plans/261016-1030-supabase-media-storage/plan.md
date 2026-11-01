---
title: "Lưu mp3 và ảnh bài học trên Supabase Storage"
description: "Chọn Supabase Storage làm bucket public cho mp3 Listening và ảnh biểu đồ Writing. MVP chỉ cần một biến CONTENT_MEDIA_BASE_URL, team upload tay, backend không nhận file. Phase sau MVP (cần duyệt): API upload ở content-service."
status: pending
priority: P2
branch: "feat/main-follow"
tags: [content, media, supabase, storage, listening, writing]
blockedBy: [261019-0813-common-storage-module]
blocks: []
created: "2026-10-16T03:30:00.000Z"
createdBy: "claude"
source: conversation
---

# Lưu mp3 và ảnh bài học trên Supabase Storage

> **Tiếp nối 2026-10-31:** phần cấu hình bucket còn lại, nạp file thiếu, ảnh bìa, bucket `media-premium` và upload Speaking được lập kế hoạch ở `261031-1215-supabase-storage-reorganization`.

## Cập nhật 2026-10-19 (thay các mục "upload tay" và "sau MVP" bên dưới)

Người dùng chốt **không upload tay nữa: mọi file phải đi qua API**. Phạm vi mở rộng và cách làm thay đổi:

- **Ba bucket** thay vì một: `media` (public, chỉ backend ghi), `avatars` (public, học viên upload), `speaking-recordings` (**private**, ghi âm Speaking của học viên).
- **Phần dùng chung** nằm ở `shared/common-storage` (plan `261019-0813-common-storage-module`): client Supabase, key an toàn, kiểm tra nội dung file. Adapter riêng trong content-service ở phase 2 bên dưới **không còn cần viết**; content-service dùng `ObjectStorage` của module đó.
- **Biến cấu hình:** `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` **bỏ** cho luồng signed upload, vì Supabase không cho truyền TTL khi tạo signed upload URL (chỉ `signedDownloadUrl` có TTL, dùng cho bucket private). `SUPABASE_STORAGE_BUCKET` (một biến) thay bằng tên bucket **theo từng service**. `SUPABASE_URL` và `SUPABASE_SECRET_KEY` ánh xạ vào `app.storage.supabase.*`.
- **Cách upload:** avatar và Speaking đi **qua backend** (file nhỏ, cần kiểm tra nội dung); mp3/ảnh nội dung của admin dùng **signed upload URL** (file lớn).
- Phase 2 vẫn **cần plan riêng cho từng endpoint** (user-service avatar, assessment-service Speaking, content-service nội dung), chưa có.
- Phase 1 (bucket và `CONTENT_MEDIA_BASE_URL`) vẫn làm được độc lập, không chờ module.

## Overview

Plan Listening (`260930-0851`) đã chốt: mp3 nằm trên bucket cloud có URL công khai; DB lưu **key**, content ghép `CONTENT_MEDIA_BASE_URL` rồi trả `mediaUrl`.
Plan Writing Task 1 (`260930-0812`) cho ảnh biểu đồ dùng URL `https://` hoặc data URI. Plan này chọn nhà cung cấp là **Supabase Storage** và liệt kê cấu hình.
Hai plan nói trên nằm trên nhánh `feat/main-follow`.

## Quyết định (chốt 2026-10-16, theo hội thoại)

| Chủ đề | Quyết định |
| --- | --- |
| Nhà cung cấp | Supabase Storage, một bucket **public**. Không dùng Google Drive (không phải CDN, link tải không ổn định, không đảm bảo range request) |
| Upload trong MVP | Team upload tay bằng Dashboard, đặt tên đúng key trong seed. **Không có upload API, không có file service**, backend không nhận file |
| Giá trị trong DB | Giữ nguyên như plan Listening: `content_assets.media_reference` là key (ví dụ `listening/demo/ls1.mp3`), hoặc URL `https://` đầy đủ |
| Nơi ghép URL | Chỉ content-service (một chỗ duy nhất, `MediaReferencePolicy`); ai-learning và assessment nhận URL đầy đủ |
| API upload | **Ngoài MVP**, phase 2 bên dưới, cần duyệt trước khi làm (thêm tích hợp dịch vụ ngoài, AGENTS.md §2) |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Thiết lập bucket và cấu hình MVP](./phase-01-thiet-lap-bucket-va-cau-hinh-mvp.md) | Pending |
| 2 | [API upload ở content-service (sau MVP)](./phase-02-api-upload-sau-mvp.md) | Pending, cần duyệt |

## Biến cấu hình

| Biến | Phase | Nơi đặt | Bí mật? | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `CONTENT_MEDIA_BASE_URL` | 1 (MVP, bắt buộc) | `.env` gốc + `config-repo/content-service.yaml` (`content.media.base-url`) | Không | `https://<project-ref>.supabase.co/storage/v1/object/public/<bucket>/`, có dấu `/` cuối |
| `SUPABASE_URL` | 2 | `.env` + config-repo content | Không | `https://<project-ref>.supabase.co` |
| `SUPABASE_STORAGE_BUCKET` | 2 | `.env` + config-repo content | Không | Tên bucket (khớp với bucket trong `CONTENT_MEDIA_BASE_URL`) |
| `SUPABASE_SECRET_KEY` | 2 | **chỉ `.env`** | **Có** | Khóa server của Supabase (secret key, hoặc `service_role` ở project cũ). Chỉ content-service đọc |
| `CONTENT_MEDIA_MAX_BYTES` | 2 | config-repo content | Không | Giới hạn kích thước mỗi file khi upload |
| `CONTENT_MEDIA_ALLOWED_TYPES` | 2 | config-repo content | Không | Ví dụ `audio/mpeg,image/png,image/jpeg,image/svg+xml` |
| `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` | 2 (chỉ nếu dùng signed upload URL) | config-repo content | Không | Thời hạn của URL upload |

Không cần: `SUPABASE_ANON_KEY` (không có client trực tiếp), mật khẩu Postgres của Supabase (không dùng DB của Supabase), biến cho ai-learning/assessment/Gateway.

Quy tắc: khóa chỉ ở `.env` (gitignored), **không** ghi giá trị vào code, tài liệu, test, log, commit, và **không** đặt giá trị fallback trong config-repo (AGENTS.md §3.4, §5). Chỉ ghi tên biến.

## Rủi ro

- Bucket public: ai có URL đều tải được, không phù hợp cho nội dung trả phí hay riêng tư. MVP chấp nhận (giống plan Listening); transcript và đáp án không nằm trong file media.
- Gói miễn phí giới hạn dung lượng và kích thước mỗi file; kiểm tra trên trang giá hiện tại trước khi upload mp3 dài.
- Khóa server lộ ra frontend hoặc repo là sự cố bảo mật; phase 1 không cần khóa nào nên rủi ro này chỉ có từ phase 2.
- Đổi nhà cung cấp sau này chỉ cần đổi `CONTENT_MEDIA_BASE_URL` và copy file cùng key, không cần migration.
