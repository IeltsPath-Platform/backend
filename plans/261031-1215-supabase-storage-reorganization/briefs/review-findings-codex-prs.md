# Kết quả review PR của Codex (2026-10-31)

Người review: Claude. Cách kiểm: đọc diff, chạy lại test trên worktree sạch, viết test tạm để chứng minh lỗi nghi ngờ, theo `verification-upload-endpoints.md`.

## Tóm tắt

| PR | Nội dung | Kết quả |
| --- | --- | --- |
| backend #125 | Tài liệu bucket | Đạt, sửa nhỏ 2 trạng thái |
| frontend #53 | Upload ảnh bìa khóa học (frontend) | Đạt, chưa chạy thử với API thật |
| frontend #54 | Upload ghi âm Speaking (frontend) | Đạt, chưa chạy thử với API thật |
| backend #126 | Upload ghi âm Speaking (backend) | **Chưa đạt: 2 lỗi phải sửa, 1 điểm yếu cần sửa** |

Chưa PR nào được đề nghị merge.

## backend #126: phải sửa trước khi merge

### F1. Lỗi lưu trữ và file quá lớn trả 500 thay vì 503 và 413

- `GlobalExceptionHandler` của assessment-service không có handler cho `StorageException` và `MaxUploadSizeExceededException`. Hai loại lỗi này rơi vào `Exception.class` và trả **500**.
- library-service đã xử lý đúng (`StorageException` → 503, `MaxUploadSizeExceededException` → 413). Dùng làm mẫu.
- Ảnh hưởng: upload (`put`), nộp bài (`storage.exists`), lấy URL nghe lại (`signedDownloadUrl`). Checklist yêu cầu 503 khi Supabase lỗi và 413 khi file quá giới hạn.
- Sửa: thêm hai handler vào `services/assessment-service/src/main/java/com/ieltspath/assessment/api/exception/GlobalExceptionHandler.java`. Log chỉ ghi `operation` và `reason`, không ghi khóa hay tên bucket. Thông báo cố định.
- Test: controller trả 503 khi `ObjectStorage.put` ném `StorageException`; trả 413 khi file vượt `max-file-size`; không tạo bản ghi DB khi `put` lỗi.
- **Phạm vi đã được duyệt (2026-10-31):** được sửa `GlobalExceptionHandler.java` của assessment-service, chỉ thêm hai handler trên. Không đổi handler nào khác.

### F2. Bài Speaking có `textPayload` và không có `audioReference` gây NullPointerException (500)

- `CreateLearnerSubmissionUseCase.execute`: sau kiểm "đúng một payload", nhánh `SPEAKING` gọi `reference.startsWith(...)` trên `audioReference`. Nếu client gửi `skill = SPEAKING`, `textPayload` có giá trị, `audioReference = null`, vượt qua kiểm "đúng một payload" rồi gặp NPE.
- Đã chứng minh bằng test tạm: `Cannot invoke "String.startsWith(String)" because "reference" is null`.
- Sửa: với `SPEAKING`, nếu `audioReference == null` hoặc có `textPayload` thì ném `InvalidAssessmentStateException` (400).
- Test: Speaking + text + không audio → 400.

### F3. Cấu hình hạn lưu và TTL không được kiểm tra

- `SpeakingRecordingProperties`: `retentionDays` và `recordingUrlTtlSeconds` không có ràng buộc.
- `ASSESSMENT_SPEAKING_RETENTION_DAYS=0` hoặc âm sẽ làm job xóa **mọi** ghi âm ngay lập tức (cutoff = hiện tại hoặc tương lai).
- TTL lớn hơn 900 giây làm `signedDownloadUrl` từ chối (giới hạn `MAX_TTL` của `common-storage`).
- Sửa: `@Validated` với `retentionDays >= 1` và `recordingUrlTtlSeconds` trong khoảng 1 đến 900. Khởi động thất bại khi sai.
- Test: giá trị sai làm context không khởi động (hoặc test validator).

### Điểm yếu về test

- `SpeakingRecordingControllerTest` chỉ có 1 test. Chưa phủ: 401, 403 (đúng role nhưng sai người), 400 (file rỗng, SVG, byte giả), 413, 503. Cần bổ sung các ca này khi sửa F1.

## Đã kiểm và đạt (backend #126)

- Phạm vi file: đúng brief (cộng `docker-compose.yml`, chỉ khối assessment).
- Quyền: upload chỉ `CUSTOMER`; nghe lại chỉ chủ bài hoặc examiner được giao (`grading_jobs.assigned_examiner_id`, HUMAN, SPEAKING, không phải `QA_SAMPLE`). Admin không nghe được.
- Khóa: `ObjectKeys.forUser("speaking", userId, ext)`; kiểm `requireOwnedBy` khi nộp bài và khi lấy URL; từ chối `local-recording:`; kiểm `exists`.
- URL ký: TTL 900 giây, dùng `signedDownloadUrl`. Không trả URL ở upload.
- Bài đã xóa trả 404.
- Job xóa: dùng retention; ghi log không lộ khóa; lỗi một bản ghi không dừng cả lô.
- Migration `V20` đúng số kế tiếp (V19 là mới nhất).
- Cấu hình multipart `max-file-size: 5MB`, `max-request-size: 6MB`: đúng.
- Kiểu khai báo `audio/webm;codecs=opus` của Chrome được chấp nhận, vì `ContentTypes.normalize` bỏ phần sau dấu `;`.
- Test chạy lại trên worktree sạch: 13 test (5 lớp) 0 lỗi 0 bỏ qua. Số của Codex (450 và 536 toàn bộ) chưa chạy lại.

## frontend #53 và #54

- Đúng phạm vi file. Đã đọc `mediaUploadApi.ts`, `speakingRecordingApi.ts`, diff `SpeakingSectionRunner.tsx`.
- Kiểm client: kích thước, MIME, file rỗng. Không còn chuỗi `local-recording:`.
- Luồng nộp bài giữ blob và khóa đã upload trong bộ nhớ để thử lại, không phải ghi lại.
- Chưa kiểm: chạy thử trên trình duyệt thật với file thật, và hiển thị lỗi 413 và 503 (phụ thuộc F1 của backend).
- Gợi ý không bắt buộc: dòng thông báo "bản ghi sẽ được gửi lên khi bạn nộp bài" nên nêu thời hạn lưu 90 ngày (quyết định sản phẩm).

## backend #125

- Bảng bucket khớp `shared/common-storage/README.md`. Link đúng.
- Sửa nhỏ: `media-premium` đã tạo (ghi "đã tạo, riêng tư"); `speaking-recordings` ghi lại sau khi #126 merge.

## Việc cần Codex làm

1. PR #126: sửa F1, F2, F3, bổ sung test controller. Phạm vi `GlobalExceptionHandler.java` đã được duyệt cho F1.
2. PR #125: sửa hai dòng trạng thái.
3. Báo cáo theo định dạng chuẩn, kèm số test thật sau khi sửa.
