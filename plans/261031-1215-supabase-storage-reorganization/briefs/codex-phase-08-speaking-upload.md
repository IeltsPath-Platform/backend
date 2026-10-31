# Brief Codex: phase 8, ghi âm Speaking

Người làm: **Codex**. Claude review trước khi merge (dữ liệu giọng nói, quyền truy cập). Không ghi Supabase, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Nhánh và PR

- Backend: `feat/storage-p8-speaking-upload` từ `feat/supabase-storage-reorganization`, PR vào đó.
- Frontend: `feat/storage-p8-speaking-upload` từ `feat/supabase-storage-reorganization` (frontend).
- Không sửa `docker-compose.yml` ngoài khối `assessment-service`.

## Quyết định đã chốt

- Bucket `speaking-recordings`: riêng tư, 5 MB, `webm`, `mp4`, `ogg`, `mpeg`, `wav` (đã có trên Supabase).
- Khóa: `speaking/{userId}/{uuid}.{ext}`.
- Lưu **90 ngày**. Quá hạn thì xóa object và đánh dấu bài nộp.
- Nghe lại: chỉ **chủ bài** và **examiner được giao chấm** bài đó. Admin không nghe được.
- URL nghe lại TTL **900 giây** (15 phút), đúng giới hạn `MAX_TTL` của `common-storage`.
- Upload đi qua backend (file nhỏ, kiểm nội dung).

## Điều kiện

- Trước khi viết endpoint nghe lại, Codex phải tìm bảng gán examiner. Nếu không tìm thấy, dừng và báo `NEEDS_CONTEXT`. Gợi ý: `grep -rn "examiner" services/assessment-service/src/main/resources/db/migration/*.sql`.

## Đọc trước (đường dẫn đã kiểm)

- `services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/LearnerSubmissionController.java`: base `/api/assessments/submissions`; lấy user bằng `currentUser.requireUserId()`.
- `.../api/dto/request/CreateLearnerSubmissionRequest.java`: `audioReference` là `String`.
- `.../application/usecase/CreateLearnerSubmissionUseCase.java`: đang kiểm "đúng một payload text hoặc audio", chưa kiểm khóa audio.
- `.../infrastructure/persistence/entity/LearnerSubmissionJpaEntity.java`: cột `audio_reference`, `user_id`, `submitted_at` (từ mapper).
- `services/assessment-service/src/main/resources/db/migration/`: bảng `learner_submissions` (V11 có insert vào đây). Số migration mới nhất là **V19**, nên file mới là **V20**.
- `.../infrastructure/scheduler/GradingDeadlineScheduler.java`: mẫu `@Scheduled` đã dùng trong service này.
- `services/assessment-service/pom.xml`: **chưa có** `common-storage`. Thêm theo mẫu `services/content-service/pom.xml` (dòng 26–30).
- `infra/config-server/config-repo/assessment-service.yaml`: thêm khối cấu hình (xem bên dưới).
- Thư viện: `shared/common-storage` (README đã mô tả `ObjectStorage`, `ObjectKeys.forUser`, `ObjectKeys.requireOwnedBy`, `UploadPolicy`, `ContentTypes`).
- Frontend: `frontend/src/features/learning-path/placement/SpeakingSectionRunner.tsx` (dòng ~82 đang gửi `local-recording:{attemptId}:{itemId}`), `useMicRecorder.ts`.

## Backend: cấu hình

`assessment-service.yaml`:

```yaml
app:
  storage:
    enabled: true
    supabase:
      url: ${SUPABASE_URL:}
      secret-key: ${SUPABASE_SECRET_KEY:}
assessment:
  speaking:
    bucket: ${ASSESSMENT_SPEAKING_BUCKET:speaking-recordings}
    retention-days: ${ASSESSMENT_SPEAKING_RETENTION_DAYS:90}
    recording-url-ttl-seconds: 900
```

`docker-compose.yml`: thêm `SUPABASE_URL` và `SUPABASE_SECRET_KEY` vào khối `assessment-service` (theo cùng mẫu các service khác, giá trị `"${SUPABASE_URL:-}"`).

## Backend: file tạo và sửa

Dưới `services/assessment-service/src/main/java/com/ieltspath/assessment/`:

| Hành động | File | Nội dung |
| --- | --- | --- |
| Tạo | `application/usecase/UploadSpeakingRecordingUseCase.java` | `UploadPolicy.of(5 MB, "audio/webm", "audio/mp4", "audio/ogg", "audio/mpeg", "audio/wav")`; `ContentTypes.extensionFor`; `ObjectKeys.forUser("speaking", userId, ext)`; `ObjectStorage.put(bucket, key, bytes, ct)`. Trả khóa và kích thước. |
| Tạo | `api/controller/SpeakingRecordingController.java` | `POST /api/assessments/speaking-recordings`, multipart `file`, trả **201** `{mediaReference, contentType, sizeBytes}`. Không trả URL. Quyền giống `LearnerSubmissionController`. |
| Tạo | `application/usecase/GetSpeakingRecordingUrlUseCase.java` | `GET /api/assessments/submissions/{submissionId}/recording-url`. Kiểm: người gọi là chủ bài, **hoặc** examiner được giao bài đó; sai thì 403. Bài đã xóa (`recording_deleted_at` không null) thì 404. Trả `{url, expiresInSeconds: 900}` qua `ObjectStorage.signedDownloadUrl`. |
| Sửa | `application/usecase/CreateLearnerSubmissionUseCase.java` | với `skill = SPEAKING` và `audioReference` có giá trị: từ chối tiền tố `local-recording:`; `ObjectKeys.requireOwnedBy(key, "speaking", currentUserId)`; `ObjectStorage.exists(bucket, key)`. Lỗi trả 400 với thông báo cố định. |
| Tạo | `application/usecase/PurgeExpiredSpeakingRecordingsUseCase.java` | chọn tối đa 100 bài `SPEAKING` có `submitted_at < now - retention-days`, `recording_deleted_at IS NULL`, `audio_reference LIKE 'speaking/%'`; xóa object; đặt `recording_deleted_at = now`. Lỗi một bài thì ghi log và đi tiếp. |
| Tạo | `infrastructure/scheduler/SpeakingRecordingRetentionScheduler.java` | `@Scheduled` mỗi giờ, gọi use case trên |
| Tạo | `infrastructure/persistence/...` (theo mẫu repository hiện có) | truy vấn cho purge và lấy bài theo id |
| Tạo | `src/main/resources/db/migration/V20__speaking_recording_retention.sql` | `ALTER TABLE learner_submissions ADD COLUMN recording_deleted_at TIMESTAMPTZ;` và chỉ mục cho `(skill, submitted_at)` |

Frontend:
- `SpeakingSectionRunner.tsx`: thay `local-recording:...` bằng quy trình: ghi âm → upload `POST /api/assessments/speaking-recordings` → nhận `mediaReference` → gửi bài nộp với `audioReference = mediaReference`.
- Dùng `MediaRecorder.isTypeSupported` để chọn MIME; nếu không nằm trong danh sách server thì báo lỗi rõ ràng.
- Lỗi mạng khi upload: cho thử lại, không mất bản ghi đang thu.
- Tạo `frontend/src/features/learning-path/placement/speakingRecordingApi.ts` cho hàm upload (không nhét vào runner).

## Test viết trước (TDD)

Backend:
- `UploadSpeakingRecordingUseCaseTest`: từ chối >5 MB, rỗng, byte không phải audio; chấp nhận webm và wav thật; khóa `speaking/{userId}/`.
- `SpeakingRecordingControllerTest`: 201 đúng hình; response không chứa `url`.
- `CreateLearnerSubmissionUseCaseTest` (mở rộng): từ chối `local-recording:`; từ chối khóa `speaking/{người khác}/`; từ chối khóa không tồn tại (`exists` false); chấp nhận khóa của chính người dùng.
- `GetSpeakingRecordingUrlUseCaseTest`: chủ bài có URL; examiner được giao có URL; examiner khác 403; học viên khác 403; admin không có quyền (403); bài đã xóa 404; TTL trả 900.
- `PurgeExpiredSpeakingRecordingsUseCaseTest`: xóa đúng bài quá hạn; không đụng bài còn hạn; bài đã có `recording_deleted_at` không xử lý lại; lỗi một object không dừng cả lô.
- Test migration V20 (theo mẫu test migration có sẵn của assessment, nếu có).

Frontend:
- `speakingRecordingApi`: upload đúng endpoint; từ chối MIME lạ ở client.
- `SpeakingSectionRunner`: không còn chuỗi `local-recording:`; lỗi mạng cho thử lại; gửi khóa thật.

## Không làm

- Không đổi `video_practice_attempts` (luyện theo video). Nếu thấy cùng lỗi chuỗi giả ở đó, **báo trong `Concerns`**, không sửa.
- Không hỗ trợ admin nghe ghi âm.
- Không ghi URL ký vào log hoặc DB.
- Không trả tên bucket hay khóa thô cho client.
- Không đổi `common-storage`.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/assessment-service -am test
```
Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run src/features/learning-path/placement`.

Thử tay trên trình duyệt (Claude hoặc bạn làm, không tự báo "đã chạy" nếu chưa chạy).

## Tiêu chí xong

- Học viên ghi âm và nộp: object nằm dưới `speaking/{userId}/` trong `speaking-recordings`; `audio_reference` là khóa thật.
- Chủ bài và examiner được giao nghe được; người khác không.
- Bài quá 90 ngày: object bị xóa, `recording_deleted_at` có giá trị, nghe lại trả 404.
- Test xanh, diff trong danh sách file trên.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Không tìm thấy bảng gán examiner cho bài nộp.
- Trình duyệt tạo MIME nằm ngoài danh sách server.
- `video_practice_attempts` có cùng lỗi: báo, không tự mở rộng phạm vi.
