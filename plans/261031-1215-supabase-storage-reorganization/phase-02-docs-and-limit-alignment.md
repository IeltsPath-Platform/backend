---
phase: 2
title: "Docs and limit alignment"
status: pending
priority: P3
effort: "1.5h"
dependencies: [1]
---

# Phase 2: Docs and limit alignment

## Phân công

- **Codex**, theo `briefs/codex-phase-02-docs.md`. Claude review.
- Đã làm trước: README content (`de1bec41`), README common-storage (`1aca55e1`).

## Overview

Làm tài liệu và giới hạn khớp thực tế: sửa câu cũ trong README content-service, thêm bảng bucket vào tài liệu kiến trúc, và chốt một nguồn chuẩn cho giới hạn upload để code và cấu hình bucket không lệch lại.

## Requirements

- Functional: tài liệu mô tả đúng bucket, chủ, công khai, giới hạn, tiền tố khóa; có truy vấn kiểm tra bucket khớp cấu hình.
- Non-functional: chỉ sửa tài liệu, không đổi hành vi. Phase 3, 5, 7, 8 bổ sung dòng tương ứng khi hoàn thành (bảng ở đây là nguồn duy nhất).

## Architecture

Nguồn chuẩn giới hạn: **hằng số trong code của use case upload** (ví dụ `UploadContentAudioUseCase.MAX_AUDIO_BYTES = 25 MB`, `AllowedMediaPolicy` của community, `UploadPolicy` của avatar/flashcard). Cấu hình bucket phải **lớn hơn hoặc bằng** giới hạn code và cho phép đúng các loại mà code nhận; bucket là chặn thứ hai, không phải nơi quyết định. Ghi nguyên tắc này trong tài liệu.

## Related Code Files

- Modify: `services/content-service/README.md` (câu "backend không có upload API, signed URL": nay có `UploadContentAudioUseCase` và `ContentMediaController`).
- Modify: `docs/system-architecture.md` (thêm mục "Lưu trữ file / Supabase Storage").
- Kiểm tra và sửa nếu lệch: `infra/config-server/config-repo/{content,user,library,community}-service.yaml`.
- Tham khảo: `services/{user,library,community}-service/README.md`.

## Implementation Steps

1. Đọc nguyên văn `services/content-service/README.md`, `docs/system-architecture.md` trước khi sửa (rule tài liệu: đọc rồi mới sửa, sửa xong đối chiếu ngày/liên kết).
2. Sửa câu cũ trong README content-service; mô tả luồng upload audio hiện có (`POST /api/content/admin/media/audio`, khóa `content-audio/{userId}/{uuid}.{ext}`) và nhắc khóa seed theo bảng 24 file.
3. Thêm bảng bucket vào `docs/system-architecture.md` theo bảng đích ở `plan.md` (chỉ ghi trạng thái đã có hiệu lực; mục `media-premium` ghi "chưa tạo" cho tới phase 7). Cột: bucket, service chủ, công khai, giới hạn code, giới hạn bucket, loại file, tiền tố khóa, biến cấu hình.
4. Thêm đoạn quy tắc chia bucket (công khai/riêng tư, ai ghi, loại file/dung lượng, service chủ) và nguyên tắc "code là nguồn chuẩn, bucket ≥ code".
5. Thêm truy vấn kiểm tra khớp vào cùng tài liệu:
   ```sql
   select id, public, file_size_limit, allowed_mime_types from storage.buckets order by id;
   ```
   kèm bảng giá trị mong đợi để so bằng mắt (chưa tự động hóa; xem Risk).
6. Đối chiếu biến `USER_AVATAR_BUCKET`, `LIBRARY_FLASHCARD_BUCKET`, `COMMUNITY_MEDIA_BUCKET`, `CONTENT_MEDIA_BUCKET` giữa config-repo, `docker-compose.yml` và tài liệu; mặc định phải trùng tên bucket thật.

## Success Criteria

- [ ] README content-service không còn khẳng định "không có upload API".
- [ ] `docs/system-architecture.md` có bảng bucket và đoạn quy tắc; ngày và liên kết đúng.
- [ ] Truy vấn kiểm tra chạy được trên project thật và kết quả khớp bảng (sau phase 1).
- [ ] Tên biến bucket nhất quán ở config-repo, compose, tài liệu.

## Risk Assessment

- Tài liệu lệch lại theo thời gian: chấp nhận ở mức này (YAGNI). Nếu lệch lặp lại, thêm kiểm tra tự động (contract test chạy khi đặt `STORAGE_CONTRACT_*`) ở plan khác; không làm ở đây.
- Không để lộ giá trị bí mật khi ghi biến: chỉ ghi tên biến.
