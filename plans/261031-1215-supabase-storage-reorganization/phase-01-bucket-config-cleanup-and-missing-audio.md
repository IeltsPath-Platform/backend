---
phase: 1
title: "Bucket config cleanup and missing audio"
status: pending
priority: P1
effort: "3h"
dependencies: []
---

# Phase 1: Bucket config cleanup and missing audio

## Overview

Không đổi code. Chỉnh cấu hình bucket cho khớp code, nạp 20 file mp3 Listening còn thiếu, đặt `CONTENT_MEDIA_BASE_URL`, dọn file thử. Làm trước vì các phase sau (ảnh bìa, khóa Writing) cần base URL đúng và bucket `media` cấu hình đủ.

## Requirements

- Functional: 24/24 mp3 theo `services/content-service/README.md` truy cập được; `community` và `media` đúng cấu hình đích.
- Non-functional: mọi lệnh ghi lên Supabase thật chỉ chạy sau khi người dùng đồng ý ngay lúc đó; không lộ `SUPABASE_SECRET_KEY`.

## Architecture

Không đổi. Khóa trong DB vẫn là khóa tương đối (`listening/demo/ls1.mp3`); content-service ghép `CONTENT_MEDIA_BASE_URL` qua `MediaReferencePolicy.resolve`. Nếu base URL trống, khóa tương đối ném `InvalidMediaReferenceException` (audio) hoặc bị bỏ lặng lẽ thành `null` (bìa đề luyện tập, `SearchPracticeTestsUseCase.coverUrl`).

## Related Code Files

- Modify (cấu hình, không phải code): `.env` (gitignored), `docker-compose.yml`, `infra/config-server/config-repo/content-service.yaml` (`content.media.base-url`, kiểm tra đã có chưa).
- Tham chiếu: `services/content-service/README.md` (bảng 24 file), `LessonPipelineSeedTest`, `ListeningPracticeAudioSeedTest`.

## Implementation Steps

1. **Đọc chỉ-đọc trước khi sửa** (project `kndrsdqzbzsaiuuruqno`):
   - `get_advisors` loại `security`.
   - `select policyname, cmd, roles, qual, with_check from pg_policies where schemaname = 'storage';` để biết ai ghi được vào `storage.objects` (báo cáo chưa kiểm). Bucket công khai cho đọc theo URL bất kể policy; điều cần chốt là `anon`/`authenticated` **không** ghi/xóa được.
2. **Kiểm tham chiếu file thử** trước khi xóa: trong `user_db`, `library_db`, `community_db` tìm khóa của 4 object demo (`images/0000…0001/baa097c1-….webp` ở `avatars`, `images/0000…0001/5e1de849-….jpg` ở `flashcards`, `community/post/0000…0001/5862d15a-….png` và `ceb05e02-….jpg` ở `community`). Chỉ xóa nếu không có dòng nào tham chiếu; nếu có, giữ lại và ghi lý do.
3. **Cấu hình bucket** (hỏi người dùng rồi mới chạy; Dashboard hoặc SQL):
   ```sql
   update storage.buckets set allowed_mime_types = array['image/png','image/jpeg','image/webp','image/gif'] where id = 'community';
   update storage.buckets set file_size_limit = 26214400,
     allowed_mime_types = array['audio/mpeg','audio/mp4','audio/wav','audio/ogg','image/png','image/jpeg','image/webp']
   where id = 'media';
   ```
   Trước khi bỏ `image/svg+xml`: xác nhận `storage.objects` của `media` không có `.svg` (hiện không có) và không khóa nào trong DB trỏ tới `.svg` (data URI SVG nằm trong DB nên không bị ảnh hưởng, xem phase 4).
4. **Nạp 20 mp3.** Nguồn: `services/content-service/media/` trên máy tác giả (không commit). Nếu thiếu, tạo lại bản đọc máy từ transcript theo README (file `practice`). Giữ **đúng khóa** trong README:
   - `listening/demo/`: `ls1`, `ls2`, `numM`, `spellM`, `museum`, `trapM`, `hotel`, `tour` (`.mp3`).
   - `listening/practice/`: `pt-listening-01-s1` … `s4`, `pt-listening-02-s1` … `s4`, `pt-listening-03-s1` … `s4`.
   Upload bằng Dashboard (giữ cấu trúc thư mục) hoặc script chạy trên máy người dùng với khóa server lấy từ `.env`; không chạy qua phiên này.
5. **Đặt `CONTENT_MEDIA_BASE_URL`** = `https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/` (có `/` cuối) trong `.env`; xác nhận `docker-compose.yml` và `content-service.yaml` đọc biến này. Chưa kiểm giá trị hiện tại: đọc `.env` hỏi người dùng, không in ra khóa bí mật.
   <!-- Updated: Validation Session 1 - đổi mặc định compose sang URL Supabase thật -->
   Đổi luôn mặc định trong `docker-compose.yml` (dòng `CONTENT_MEDIA_BASE_URL`, hiện là `https://media.example.invalid/`) thành URL trên, để môi trường quên đặt biến không resolve ra URL hỏng lặng lẽ. Mặc định trong `config-repo/content-service.yaml` (`base-url: ${CONTENT_MEDIA_BASE_URL:}`) giữ rỗng. URL công khai của bucket không phải bí mật.
6. **Xác minh:** `select name from storage.objects where bucket_id = 'media' and name like 'listening/%' order by name;` phải ra đủ 24; `curl -sI <base>listening/demo/ls1.mp3` trả `200` và `content-type: audio/mpeg` cho từng khóa.
7. **Dọn:** xóa object demo đã xác nhận không tham chiếu (bước 2); xóa `.emptyFolderPlaceholder` ở `listening/demo/` và `writtings/leassons/PS-W1-WR-2/` (thư mục đã có file thật). Placeholder của thư mục rỗng khác thì để.

## Success Criteria

- [ ] `pg_policies` của `storage` đã xem; `anon`/`authenticated` không ghi được `storage.objects` (hoặc đã ghi nhận ngoại lệ cho người dùng).
- [ ] `community` có `allowed_mime_types` 4 loại ảnh; `media` 25 MB, 4 audio + 3 ảnh, không còn svg.
- [ ] 24 khóa mp3 tồn tại và trả `200 audio/mpeg`.
- [ ] Mở bài Listening seed (LS1, LS2) và PT-LISTENING-01 không còn `INVALID_MEDIA_REFERENCE`.
- [ ] Object demo được xóa hoặc có ghi chú vì sao giữ.

## Risk Assessment

- Bỏ `image/svg+xml` mà vẫn có seed/bìa trỏ file SVG trong bucket: giảm bằng kiểm tra bước 3 và phase 4 xuất WebP.
- Hạ giới hạn bucket sau khi đã có file lớn hơn: ở đây chỉ nâng, an toàn.
- Upload nhầm khóa (sai hoa thường `numM`, `spellM`, `trapM`): so từng khóa với README bằng truy vấn bước 6.
- Rollback: cấu hình bucket đổi lại bằng `update` ngược; file mp3 xóa được. Ghi lại giá trị cũ (đã có trong báo cáo: `community` null; `media` 20971520 và `{audio/mpeg,image/png,image/jpeg,image/svg+xml,image/webp}`).
