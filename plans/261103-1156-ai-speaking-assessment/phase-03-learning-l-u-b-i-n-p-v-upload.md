---
phase: 3
title: "Learning lưu bài nộp và upload"
status: completed
priority: P2
effort: "2d"
dependencies: [1]
---

# Phase 3: Learning lưu bài nộp và upload

## Overview

Learning-service nhận file ghi âm (kiểm tra số dư điểm trước khi nhận), nhận bài nộp idempotent, lưu trạng thái và kết quả từng bước. Chưa gọi Azure hay LLM (phase 4 và 5).

## Requirements

- Functional: upload từng file; nộp bài (cả đề hoặc part lẻ); xem trạng thái; mỗi học viên một bài đang chờ; nộp trùng cùng `requestId` không tạo thêm bài và không trừ thêm điểm.
- Non-functional: không log audio, transcript, khóa; thời hạn lưu cấu hình được (90 ngày); chỉ role `CUSTOMER`.

## Architecture

Endpoint (Gateway đã chuyển `/api/learning/**` về learning-service, không cần sửa Gateway):

| Endpoint | Việc |
|---|---|
| `GET /api/learning/speaking-practice/terms` | Giá điểm, giới hạn dung lượng, phiên bản văn bản đồng ý |
| `POST /api/learning/speaking-practice/submissions` | Nhận `requestId`, `scope` (PART/FULL), `packageVersionId`, danh sách part, `consent`. Kiểm tra số dư (đọc, chưa trừ), consent, không có bài khác đang chờ; đủ thì tạo bài `UPLOADING`, trả 201 cùng `submissionId` và danh sách câu cần ghi âm (`questionVersionId`, part, thứ tự). Thiếu điểm: 402, chưa có file nào lên Storage. Cùng `requestId` trả lại đúng bài cũ |
| `PUT /api/learning/speaking-practice/submissions/{id}/recordings/{questionVersionId}` (multipart) | Kiểm tra bài thuộc học viên và đang `UPLOADING`, câu thuộc bài, định dạng và dung lượng; ghi lên bucket theo khóa ở dưới; ghi lại cùng câu thì dùng khóa mới (`_r2`) và xóa file cũ |
| `POST /api/learning/speaking-practice/submissions/{id}/complete` | Kiểm tra **đủ ghi âm của đề hoặc part** (số câu lấy từ content-service); đủ thì chuyển `QUEUED`, trả 202 |
| `GET /api/learning/speaking-practice/submissions/{id}` | Trạng thái, và kết quả khi `GRADED` |

Khóa object (D12), bucket `speaking-recordings`, tiền tố `practice/`:

```
practice/
  {userId}/                                  học viên (đúng một thư mục mỗi người)
    2026-11-04_{submissionId}/               ngày nộp và mã bài nộp
      part1_q01.wav                          part 1, câu 1
      part1_q02.wav
      part2_q01.wav                          part 2 (cue card)
      part3_q01_r2.wav                       câu đã ghi lại sau lỗi upload (lần 2)
```

Mọi đoạn khóa là ASCII, chữ thường, không dấu cách; khóa dựng trong learning-service rồi qua `ObjectKeys.requireSafe`. Quyền sở hữu kiểm bằng tiền tố `practice/{userId}/` và bản ghi `speaking_recordings`, không dùng `ObjectKeys.requireOwnedBy` (hàm đó chỉ nhận khóa dạng `{prefix}/{userId}/{uuid}.{ext}`). `x-upsert` của `put` là `false`: ghi trùng khóa bị 409, nên không ghi đè mà đổi hậu tố. Số `n` của part là `partNo` của đề, `nn` là thứ tự câu trong part (từ 01).

Ghi âm được giữ ở trình duyệt đến lúc học viên bấm "Chấm bài" (D5); từ lúc đó mới có `POST submissions`. Kiểm số dư chỉ một lần ở bước tạo bài. Số dư có thể đổi giữa hai bước (tiêu hết ở nơi khác); điểm chỉ trừ khi chấm xong (D4), nên hết điểm lúc đó thì bài vào `PAYMENT_PENDING`, không mất kết quả.

Nộp lại cùng `requestId` khi `PAYMENT_PENDING` thì chạy lại bước trừ điểm (giống Writing).

Dữ liệu (`learning_db`): `speaking_submissions` (trạng thái, phạm vi, `request_id` UNIQUE, phiên bản prompt, `rubric_id`, `rubric_version`, `prompt_checksum` (điền khi bắt đầu bước LLM), bảng ánh xạ phát âm, `consent_at`, giá điểm, band, JSON kết quả, mã lỗi, số lần thử, `ledger_entry_id`), `speaking_recordings` (mã, câu hỏi, thời lượng, thứ tự), `speaking_analyses` (transcript, mốc từng từ, nhịp nói, điểm phát âm, phiên bản nhà cung cấp). Bucket `speaking-recordings`, tiền tố `practice/` thuộc learning-service (D12); tiền tố `speaking/` vẫn của assessment-service.

## Related Code Files

- Create: `services/learning-service/src/main/resources/db/migration/V{next}__speaking_submissions.sql`
- Create: `domain/aggregate/SpeakingSubmission.java` (máy trạng thái), `domain/repository/SpeakingSubmissionRepository.java`, `infrastructure/persistence/JdbcSpeakingSubmissionRepository.java`
- Create: `application/usecase/UploadSpeakingRecordingUseCase.java`, `SubmitSpeakingPracticeUseCase.java`, `GetSpeakingSubmissionUseCase.java`
- Create: `api/controller/SpeakingPracticeController.java` + DTO request/response
- Create: `infrastructure/scheduler/SpeakingRetentionJob.java` (xóa file, transcript sau thời hạn)
- Modify: `services/learning-service/pom.xml` (thêm `shared/common-storage`), `infra/config-server/config-repo/learning-service.yaml` (bucket, retention, giá điểm, kiểu file), `docs/system-architecture.md` (bảng bucket)
- (Đường dẫn đầy đủ bên dưới `com/ieltspath/learning/`)

## Implementation Steps

1. Viết test máy trạng thái và test repository (Testcontainers) trước, rồi aggregate, migration, repository.
2. Use case tạo bài: kiểm tra số dư qua `AccessClient` (chỉ đọc), `consent`, idempotent theo `requestId`, chặn bài thứ hai đang chờ (`findActive` giống `findGradingFree`).
3. Use case upload theo `assessment-service` `UploadSpeakingRecordingUseCase` (cùng `UploadPolicy`: 10 MB cấu hình bằng `learning.speaking.max-recording-bytes`, chỉ `audio/wav` PCM 16 kHz mono do `WavFormat` kiểm tra) nhưng khóa do `SpeakingRecordingKeys` dựng theo D12 rồi qua `ObjectKeys.requireSafe`; use case hoàn tất kiểm tra đủ ghi âm.
4. Controller, test WebMvc: 402 khi thiếu điểm lúc tạo bài, 409 khi có bài đang chờ hoặc `requestId` xung đột, 400 khi thiếu `consent` hoặc hoàn tất thiếu ghi âm, 404 khi bài hay câu không thuộc học viên.
5. Job xóa theo thời hạn; cập nhật tài liệu bucket.

## Success Criteria

- [ ] Thiếu điểm: tạo bài bị 402 trước khi file lên Storage (test khẳng định `storage.put` không được gọi).
- [ ] Khóa object đúng dạng `practice/{userId}/{ngày}_{submissionId}/part{n}_q{nn}.wav`; ghi lại cùng câu cho khóa `_r2` và xóa file cũ; không tạo khóa ngoài tiền tố `practice/{userId}/`.
- [ ] Nộp trùng `requestId`: một bài, không trừ thêm; nộp khi đang có bài chờ: 409.
- [ ] Không thể hoàn tất thiếu ghi âm cho cả đề, hay ghi vào bài của người khác.
- [ ] Job dọn xóa file và transcript quá hạn; chạy lặp an toàn.
- [ ] Toàn bộ test learning-service (`mvn -pl services/learning-service test`) xanh, gồm test mới và cũ.

## Risk Assessment

- Thêm `common-storage` vào learning-service: kiểm tra không kéo cấu hình storage vào service khác; rollback bằng cách tắt endpoint (flag phase 7).
- F5 làm mất ghi âm phía trình duyệt: ngoài phạm vi phase này (phase 6, câu hỏi mở số 7).
