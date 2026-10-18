# Kết quả sửa lỗi sau kiểm thử độc lập

Ngày: 2026-10-15

## Đã sửa

1. `BandStatisticsController` khai báo tên cho toàn bộ query parameters. Test MVC hiện kiểm tra ngày sai trả 400 mà không gọi use case, đồng thời kiểm tra request hợp lệ trả 200 và use case nhận đúng ngày, giá trị mặc định.
2. `VocabularyReviewController` khai báo `@PathVariable("id")`. Test MVC mới gửi câu trả lời hợp lệ, kiểm tra 200 và xác nhận đúng `sessionId` được chuyển cho use case.
3. Chuẩn hóa thời điểm tại độ chính xác microsecond của PostgreSQL trước khi đưa vào domain/persist hoặc trả từ use case. Áp dụng cho `VocabularyReviewUseCase`, `StudySession`, `StartStudySessionUseCase` và `RecordStudyHeartbeatUseCase`. Test domain mới khởi tạo thời gian có nanosecond, mô phỏng giá trị đọc lại từ `timestamptz`, rồi xác nhận interval hợp lệ đúng biên 30 giây vẫn được chấp nhận.
4. `LearningServiceApplicationTests` kiểm tra Flyway version `11` và 18 bảng; giữ nguyên assertion schema.
5. `StudyActivityTracker` tự mở session mới với `clientSessionId` mới và `takeover=false` khi lease hết hạn/session đã đóng hoặc tab hiện lại. `SESSION_IN_USE` được retry tối đa ba lần với khoảng nghỉ 250 ms và 750 ms trước khi hiện nút takeover. Heartbeat đang chờ ACK vẫn giữ nguyên payload/sequence khi gặp lỗi mạng; lỗi telemetry không ngăn người học làm bài.
6. FE hiển thị band/delta một chữ số thập phân và khai báo `CourseCompletion.bandLevel` nullable.

Đã rà các controller mới của user, learning, content, assessment và library: mọi `@RequestParam`/`@PathVariable` đều có tên rõ ràng. Không sửa controller cũ, không bật `-parameters`, không sửa migration, không đổi AnswerSpecGraderTest/fixture, không thêm dependency hoặc thay đổi config/shared security/Gateway.

## Kiểm chứng

| Lệnh | Kết quả |
|---|---|
| `mvn -B -ntp -fae -pl services/user-service,services/content-service,services/library-service,services/assessment-service,services/learning-service -am test '-Ddebug=false'` | Lượt đầy đủ kết thúc exit 1. `content-service`: 255 test, 0 lỗi/skip; `library-service`: 70 test, 0 lỗi/skip; `learning-service`: 248 test, 1 error/0 skip; `assessment-service`: 207 test, 1 error/0 skip. `user-service` không compile được trong lượt này vì thiếu import `StudySession`; import đã được thêm sau khi phát hiện lỗi. Hai error còn lại đúng lỗi B1 đã có từ trước: `AnswerSpecGraderTest.gradesEverySharedContractVector` không tìm thấy `answer-spec-v1-vectors.json` ở Learning và Assessment. Không thay fixture theo yêu cầu trong prompt. Testcontainers PostgreSQL đã khởi chạy thật; không có test nào bị skip do thiếu Docker. |
| `mvn -B -ntp -pl services/user-service -am test '-Ddebug=false'` | Exit 0 sau khi sửa import. `user-service`: 132 test, 0 failure/error/skip; `common-security`: 8 test, 0 failure/error/skip. `StudySessionTest` 7/7 và `StudyTimePersistenceTest` 3/3 pass trên PostgreSQL Testcontainers. |
| `git diff --check` (BE) | Exit 0; chỉ cảnh báo Git sẽ chuẩn hóa LF/CRLF ở một số file đã thay đổi trong workspace. |
| `npm run lint` (FE) | Exit 0. |
| `npm run typecheck` (FE) | Exit 0. |
| `npm run build` (FE) | Exit 0; Vite báo bundle JavaScript lớn hơn 500 kB sau minify. |
| `node --test src/features/study-time/activeIntervals.test.mjs` (FE) | Exit 0; 4 pass, 0 fail/skip. |

Các test MVC mới `BandStatisticsControllerTest` (4/4) và `VocabularyReviewControllerTest` (4/4) đều pass. `LearningServiceApplicationTests` cũng pass với version 11 và 18 bảng.

Trong test Learning có log thử kết nối RabbitMQ tại `localhost:5672` bị từ chối; các test vẫn hoàn tất theo kết quả nêu trên. Đây không phải lỗi Testcontainers PostgreSQL.

## Còn lại / giới hạn

- Hai lỗi B1 nói trên còn tồn tại do thiếu fixture dùng chung; không sửa theo giới hạn được giao.
- Chưa chạy browser/E2E, full stack HTTP, kịch bản nhiều tab, offline, logout, audio, keyboard hoặc mobile.
- Các P3 chưa sửa: ngày lọc lịch sử band đang theo UTC trong khi FE gửi ngày trình duyệt; tuần/tháng FE theo đồng hồ trình duyệt thay vì timezone hồ sơ; `StudyContext` của Assessment chưa cung cấp `courseId`; card reload khi focus và trạng thái loading bị nháy; giới hạn trên 100 khóa làm progress API trả 503; tài liệu còn lệch về URL chạy host và trạng thái phase trong plan.
- Không đọc `.env`; không checkout/reset/clean, commit hoặc push.
