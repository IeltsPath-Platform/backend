# Nghiệm thu E2E MVP Reading, Writing, Listening — 2026-10-02

Status: PASS (95/95 kiểm tra live) sau khi sửa một bug ở access-service phát hiện trong lần chạy này.

Nhánh `feat/main-follow`, HEAD `2226a25` cộng thay đổi chưa commit của lần chạy này (sửa access, xem dưới).
Phạm vi theo [phase 8](../phase-08-nghiem-thu-e2e-mvp.md); không lặp lại kiểm chứng nền đã có ở
[e2e-261002-foundation-learning-pipeline.md](../../260929-1640-lesson-learning-pipeline-mvp/reports/e2e-261002-foundation-learning-pipeline.md)
(DLQ replay, nội dung event).

## Môi trường

- Java 21, boot jar build bằng `mvn clean package` từ source hiện tại. Eureka, Gateway, User, Content, Access, Assessment,
  Learning chạy thật trên host; không chạy process Config Server, YAML lấy trực tiếp từ `infra/config-server/config-repo/`.
- PostgreSQL 15 và RabbitMQ 3.13 trong container tạm (cổng riêng), năm database mới. Không dùng database hay `.env` của
  lập trình viên. Mật khẩu DB/RabbitMQ và hai khóa JWT sinh ngẫu nhiên cho lần chạy; không ghi vào report.
- LLM: stub OpenAI-compatible cục bộ (band 7.0 khi bài đủ số từ tối thiểu, 5.0 khi thiếu; có điều khiển lỗi/độ trễ, đếm số
  lần gọi). Không gọi model thật.
- Media: `CONTENT_MEDIA_BASE_URL=https://media.e2e.invalid/` (giả). Chỉ kiểm URL đúng dạng `base + key`, **không kiểm phát
  được** vì chưa có bucket và 8 file mp3.
- Học viên đăng ký và đăng nhập thật qua Gateway. ADMIN: user đăng ký thường rồi được gán role ADMIN bằng SQL trong DB tạm
  (user-service không có tài khoản ADMIN seed), sau đó đăng nhập thật.
- Script điều phối, stub và kịch bản nằm ngoài repo (scratchpad phiên làm việc), không commit. Không log token, mật khẩu,
  nội dung bài viết, prompt hay output LLM.

## Kết quả live E2E

| Bước | Kết quả | Quan sát chính |
| --- | --- | --- |
| 1. Tài khoản | 5/5 | Register công khai với `role=ADMIN` → 400; CUSTOMER gọi `points/adjust` → 403; ADMIN nạp 30 point, học viên thấy số dư. |
| 2. Reading (`DEMO_READING`) | 25/25 | Topic/bài khóa đúng thứ tự (`LESSON_LOCKED`, topic khóa → 403). Kịch bản Lan: L1-B5 sai Q11 rồi đạt, replay cùng `requestId` trả response cũ; L2 sai Q5 → L3 bị `REVIEW_REQUIRED`, bài ôn KP1 4/4 → `DONE`. L3/L4 hoàn thành không cần essay. Mastery KP3 = 0.729 (4 bằng chứng), KP1 = 0.875 (5), khớp seed. Giao mã đề lặp trả cùng assignment; đề 4/4 → event → `DEMO_READING` PASSED, `TFNG_SKILLS` IN_PROGRESS. |
| 2b. TFNG | 15/15 | TF1 sai rồi đạt; KP5 không có gói luyện nên không chèn bài ôn. X5 2/3 (66.67%) → không có `solutions`/`sectionSolutions`, topic không qua; assignment mới được cấp; làm lại đạt → PASSED. |
| 3. Gợi ý Reading | 6/6 | Câu sai (Q11, Q5, QT1 TFNG) có `hint`; câu đúng `hint: null`; GET bài chỉ hiện gợi ý cho câu đã sai; khối đạt thì mọi `hint` null và có `solutions`; bài ôn luôn `hint: null`. |
| 4. Writing | 14/14 | Chi tiết bên dưới. |
| 5. Listening | 24/24 | LS1/LS2: `mediaUrl` = base + `listening/demo/ls{1,2}.mp3`, có `durationSeconds`, không có key `transcript`; hoàn thành bài thì có transcript. Đề cuối: structure có `audio.url` https, không transcript. Lần 1/4 (25%) → không lời giải; câu sai chèn 1 bài ôn, bài ôn chặn giao mã đề tới khi `DONE` (3/3). Lần 2 đạt 100% → `sectionSolutions` có transcript → `DEMO_LISTENING` PASSED. |
| 6. Chặn truy cập | 6/6 | CUSTOMER gọi `/api/content/questions`, `/packages`, `/assets/{id}` → 403; Gateway chặn `/internal/access/points/debit`, `/internal/content/lessons` → 403; không token → 401. |

Quét khóa lộ chạy trên mọi response học viên ở các bước trên: không có `answerSpec`, `chartFacts`, `answerSnapshot`;
`explanation`/`correctAnswer`/`solutions`/`sampleAnswer`/transcript chỉ xuất hiện sau khi đạt.

### Writing

| Kiểm tra | Quan sát |
| --- | --- |
| Task 2 bài yếu | 200, `TR/CC/LR/GRA`, band 5.0, `passed=false`, ví 30 → 27, không có `sampleAnswer`. |
| Gửi lại cùng `requestId` | Response giống hệt, stub không nhận thêm request, không trừ thêm. |
| LLM lỗi | 503 `GRADING_UNAVAILABLE`, không trừ point. Gửi lại cùng `requestId` sau khi stub hết lỗi → chấm xong Task 1 (`TA/CC/LR/GRA`, 7.0, đạt), −3, có `sampleAnswer`. GET bài: `latestSubmission` GRADED, không `chartFacts`. |
| Song song cùng khối | Hai `requestId` khác nhau (stub trễ 4 s): 200 và 409 `GRADING_IN_PROGRESS`; một lần gọi LLM, trừ 3 một lần. |
| Song song hai khối (Task 1 + Task 2) | Cả hai 200, ví −6; sổ cái có đúng 5 debit, mỗi debit −3. |
| Hạn mức ngày | `llm_daily_usage` = 6 sau 5 lần chấm thành công + 1 lần LLM lỗi (đếm khi gọi LLM, đúng contract). Chạy lại learning-service với hạn mức 6 → 429 `DAILY_LIMIT_REACHED`, không gọi LLM, không trừ point. |
| Hết point | ADMIN rút về 0 → 402 `INSUFFICIENT_POINTS`, không gọi LLM. Topic vẫn học tiếp và qua đề được (bước 2b, 5 chạy sau khi ví = 0). |
| Bài dưới 50 từ | 422 trước khi kiểm số dư/LLM. |
| Nộp exercise vào khối essay | 409 `ESSAY_BLOCK`. |

## Bug phát hiện và đã sửa

**Access không trừ được point — mọi lần chấm Writing kẹt ở `PAYMENT_PENDING`.** Lần chạy đầu: LLM chấm xong nhưng
`POST /internal/access/points/debit` trả 500
(`column "payload" is of type jsonb but expression is of type character varying`). `PointDebitWriter` ghi outbox
`PointDebited`; `OutboxEventJpaEntity` của access thiếu `@JdbcTypeCode(SqlTypes.JSON)` mà content/assessment có, nên
Hibernate gửi `payload` dạng varchar. Test hiện có của access chỉ là unit test với mock, không ghi outbox xuống
PostgreSQL thật nên không bắt được. Mọi use case ghi outbox của access đều chịu cùng lỗi trước bản sửa:
`PointDebitWriter`, `ActivateKeyUseCase` (kích hoạt key), `GrantSubscriptionUseCase`, `ConsumeHumanGradingCreditUseCase`.
Lần chạy này chỉ kiểm live đường trừ point; ba use case còn lại dùng cùng entity nên được sửa theo, chưa kiểm live.

Sửa:
- `services/access-service/.../persistence/entity/OutboxEventJpaEntity.java`: thêm `@JdbcTypeCode(SqlTypes.JSON)`.
- `services/access-service/pom.xml`: thêm `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` (scope test,
  như user/content).
- Test mới `PointDebitPersistenceTest` (Testcontainers): debit qua JPA thật ghi ví, sổ cái và outbox JSONB. Đã chạy
  **fail trước khi sửa** đúng lỗi jsonb, pass sau khi sửa. Access: 46 test, 0 fail.

Sau khi sửa, build lại jar access và chạy lại toàn bộ kịch bản với học viên mới: 95/95.

## Regression

| Lệnh | Kết quả |
| --- | --- |
| `mvn -q -pl services/access-service -am test` | 46 test, 0 failure, 0 error, 0 skip. |
| `mvn -q compile -DskipTests` | Exit 0. |
| `mvn -q test` (toàn reactor, Docker bật) | Exit 0. **703 test, 0 failure, 0 error, 0 skip** (Testcontainers chạy). Theo module: common-security 8, Gateway 8, Config Server 1, Eureka 1, User 106, Content 138, Access 46, Assessment 117, Learning 183, Library 53, Game 15, Community 26, Notification 1. (Bỏ qua surefire report cũ trong `target/` của `learning-support-service` đã xóa.) |
| `docker compose config --quiet` | Exit 1 với `.env` của máy: thiếu `LIBRARY_DB_PASSWORD`, `LEARNING_DB_PASSWORD` (biến mới từ chia service / learning-service). Đặt hai biến giá trị giả chỉ cho lệnh kiểm → exit 0, compose hợp lệ. Cần bổ sung hai biến vào `.env` khi chạy DB compose. |

Ghi chú build: lần build đầu không `clean` làm jar content còn class cũ `VocabularyRepositoryAdapter` (đã chuyển sang
library khi chia service) và content không khởi động. Không phải lỗi source; build từ `target/` sạch thì chạy. Ai build
jar trên máy đã từng build trước khi chia service cần `mvn clean package`.

## Tài liệu

`DATABASE_V5.md` §0 và `mvp-database.md` đã ghi V5.3, V5.4, Learning V1–V2, Writing là đã triển khai (PR #34–#36); bản sửa
access không đổi schema nên không cần sửa tài liệu database.

## Giới hạn

- Chưa kiểm audio phát được (không có bucket, 8 file mp3 chưa thu âm). Đây là việc nội dung/hạ tầng, không phải code.
- Không chạy model LLM thật; chất lượng chấm chưa được đánh giá.
- Kịch bản dùng đề X1 4/4 (không lặp ca 75% và mastery KP2 = 0.487 vì report nền đã kiểm).
- Outbox của access vẫn chỉ ghi, chưa có relay (đã biết, `AGENTS.md` §7); bản sửa chỉ làm việc ghi không lỗi.

## Câu hỏi mở

- Chọn bucket cloud và người thu âm 8 file mp3 seed (đã có trong roadmap).
- Có cần một lượt chạy với model thật trước demo để xem band có hợp lý không.
