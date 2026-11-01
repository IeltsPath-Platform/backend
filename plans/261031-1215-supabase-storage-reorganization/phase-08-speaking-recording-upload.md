---
phase: 8
title: "Speaking recording upload"
status: pending
priority: P2
effort: "2d"
dependencies: [1]
---

# Phase 8: Speaking recording upload

## Phân công

- **Codex**, theo `briefs/codex-phase-08-speaking-upload.md` (backend và frontend). **Claude review** trước khi merge vì liên quan dữ liệu giọng nói và quyền nghe lại.
- Đã có quyết định: 90 ngày, chủ bài và examiner được giao nghe lại, TTL 900 giây.
- Điều kiện: Codex phải tìm được bảng gán examiner trước khi viết endpoint nghe lại; nếu không tìm thấy thì dừng.

## Overview

Ghi âm Speaking hiện không được lưu: `SpeakingSectionRunner.tsx:82` gửi `audioReference: local-recording:{attemptId}:{itemId}` (chuỗi giả), cột `audio_reference` của `learner_submissions` (và `video_practice_attempts`) chỉ giữ chuỗi đó. Phase này làm upload thật vào bucket riêng tư `speaking-recordings`, nghe lại qua URL ký, và xóa theo thời hạn lưu. Ghi âm là dữ liệu cá nhân (giọng nói).

## Requirements

- Functional: học viên ghi âm → file lên `speaking-recordings` (≤5 MB; webm, mp4, ogg, mpeg, wav) qua backend; bài nộp mang khóa thật; chủ bài và người chấm được phân công nghe lại qua URL ký ngắn hạn; file quá hạn lưu bị xóa.
- Non-functional: bucket riêng tư, khóa thuộc đúng người dùng (`speaking/{userId}/{uuid}.{ext}`), kiểm byte file; không log khóa/URL; có thời hạn lưu cấu hình; xóa khi tài khoản bị xóa nếu hệ thống có luồng đó.

## Architecture

- assessment-service thêm phụ thuộc `shared/common-storage` (đã có `ObjectStorage`, `ObjectKeys.forUser`, `UploadPolicy`, `signedDownloadUrl`), theo mẫu `UploadContentAudioUseCase` / `ContentAudioStorageAdapter`.
- Luồng: FE ghi âm blob → `POST` upload (multipart) → nhận `key` → gửi `CreateLearnerSubmissionRequest.audioReference = key`. Backend kiểm khóa bắt đầu bằng `speaking/{currentUserId}/` và `ObjectStorage.exists`; từ chối `local-recording:` cho bài nộp mới (bài cũ giữ nguyên, không backfill).
- Nghe lại: endpoint trả URL ký TTL 15 phút **chỉ cho chủ bài và examiner được giao** (liên quan hàng đợi chấm của plan `261022-1150-examiner-selection-and-queue`); admin không có quyền nghe (đã chốt ở phiên validate 1).
- Thời hạn lưu: job `@Scheduled` xóa object quá `assessment.speaking.retention-days` (**90**, đã chốt) và đánh dấu bài nộp là đã hết hạn (không để khóa trỏ vào object không còn).
  <!-- Updated: Validation Session 1 - hạn lưu 90 ngày; quyền nghe lại chủ bài + examiner được giao -->
## Related Code Files

- Create (assessment-service): use case upload (`UploadSpeakingRecordingUseCase`), controller/endpoint (đường dẫn theo quy ước hiện có của service, dự kiến `POST /api/assessment/speaking-recordings`), adapter lưu trữ, endpoint URL nghe lại, job dọn hết hạn, migration (dự kiến `V20`, cột đánh dấu hết hạn nếu cần), test.
- Modify (assessment-service): `pom.xml` (phụ thuộc `common-storage`), `CreateLearnerSubmissionUseCase` (kiểm khóa), `config-repo/assessment-service.yaml`, `docker-compose.yml` (`SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `ASSESSMENT_SPEAKING_BUCKET` mặc định `speaking-recordings`, thời hạn lưu).
- Modify (frontend): `features/learning-path/placement/SpeakingSectionRunner.tsx`, `useMicRecorder.ts` (kiểu MIME thực của `MediaRecorder`), API client assessment.
- Tham khảo: `LearnerSubmissionController`, `VideoPracticeAttempt`, `AssessmentPersistenceMapper`.

## Implementation Steps

1. **Nghiên cứu ngắn trước:** đọc `LearnerSubmissionController`, luồng chấm Speaking (AI/examiner dùng audio thế nào?), nơi `video_practice_attempts.audio_reference` được ghi (có cùng tình trạng chuỗi giả không?), MIME mà `MediaRecorder` tạo trên Chrome/Safari (`audio/webm;codecs=opus` hay `audio/mp4`), và `ContentTypes` của `common-storage` có nhận dạng được webm/ogg/mp4/wav bằng byte không. Kết quả có thể đổi phạm vi (ví dụ `VideoPracticeAttempt` đưa vào hay để sau); ghi lại trước khi viết code.
2. **Test trước:** policy (từ chối >5 MB, byte không phải audio, rỗng), khóa đúng tiền tố và thuộc user; bài nộp từ chối khóa của người khác, khóa không tồn tại, và `local-recording:`; quyền nghe lại (chủ bài được, người lạ 403, examiner không được giao 403); job dọn xóa đúng object quá hạn và không động bài chưa hết hạn.
3. Thêm cấu hình, use case, endpoint upload và kiểm khóa trong tạo bài nộp.
4. Endpoint nghe lại bằng URL ký; ghi nhận quy tắc phân quyền đã chốt.
5. Job hết hạn + migration đánh dấu; cấu hình thời hạn lưu.
6. Frontend: upload blob trước khi nộp, hiện tiến trình và lỗi, cho thử lại (không mất bản ghi khi mạng lỗi tạm thời); gửi khóa làm `audioReference`. Bỏ chuỗi `local-recording:`.
7. Cập nhật tài liệu: dòng `speaking-recordings` trong bảng bucket (phase 2), thời hạn lưu và quyền nghe lại; nêu đây là dữ liệu cá nhân ở mục bảo mật của `docs/system-architecture.md`.
8. Chạy `mvn -B -ntp -pl services/assessment-service -am test` và test FE placement Speaking; thử thủ công một lượt ghi âm → nộp → nghe lại bằng trình duyệt thật (không chỉ test đơn vị).

## Success Criteria

- [ ] Ghi âm xong có object trong `speaking-recordings` dưới `speaking/{userId}/`; `audio_reference` là khóa đó.
- [ ] Chủ bài và người chấm được giao nghe lại; người khác bị từ chối; bucket không đọc được bằng URL công khai.
- [ ] File quá hạn lưu bị xóa theo cấu hình; bài nộp không trỏ vào object đã xóa mà không có đánh dấu.
- [ ] FE không còn tạo `local-recording:`; mạng lỗi có đường thử lại.
- [ ] Test assessment-service và FE xanh; một lượt thủ công chạy được trên trình duyệt.

## Risk Assessment

- Kích thước: bản ghi dài vượt 5 MB bị từ chối; kiểm độ dài tối đa của Speaking Part 2 với bitrate `MediaRecorder` (đặt `audioBitsPerSecond` nếu cần); chỉnh giới hạn nếu thực đo vượt.
- Trình duyệt tạo MIME khác (Safari `audio/mp4`): policy phải nhận đủ loại bucket cho phép.
- Quyền riêng tư: giọng nói là dữ liệu cá nhân; thời hạn lưu và xóa là bắt buộc, không để mặc định "giữ mãi". Hạn lưu (90 ngày) và quyền nghe lại (chủ bài, examiner được giao) đã chốt ở phiên validate 1.
- Cần `SUPABASE_URL`/`SUPABASE_SECRET_KEY` cho assessment-service; thiếu thì upload thất bại rõ ràng (`UnavailableObjectStorage`) thay vì nuốt lỗi — kiểm FE hiển thị lỗi này.
- Rollback: tắt tính năng bằng cách giữ endpoint nhưng FE cũ vẫn gửi chuỗi giả sẽ bị từ chối; vì vậy triển khai FE và BE cùng lúc, hoặc giữ chấp nhận `local-recording:` trong một cửa sổ chuyển tiếp ngắn có thời hạn.
