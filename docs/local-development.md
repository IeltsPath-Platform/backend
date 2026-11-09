# Chạy và cấu hình local

Hướng dẫn build, chạy bằng Docker, biến môi trường, tài khoản demo và Swagger. Tổng quan hệ thống và các pattern ở [`README.md`](../README.md); kiến trúc chi tiết ở [`system-architecture.md`](system-architecture.md).

## 1. Biên dịch dự án

Mở terminal tại thư mục gốc của dự án và chạy:

```bash
mvn clean compile -DskipTests
```

Kiểm thử riêng tên event, payload và helper retry:

```bash
mvn -q -pl shared/common-events test
```

*(Nếu hiển thị `BUILD SUCCESS` là toàn bộ cấu trúc dự án và các module con đã hợp lệ).*

## 2. Khởi chạy backend bằng Docker

Compose khởi chạy PostgreSQL (mỗi service vẫn có database riêng), RabbitMQ, Config Server, Eureka, Gateway và các
service đã triển khai. Các database schema và dữ liệu demo được nạp bởi Flyway; tài khoản và điểm demo bật mặc định
trong môi trường local. Đặt biến môi trường của mục 4 trong `.env` ở root, kiểm cấu hình rồi khởi chạy:

```powershell
docker compose config --quiet
docker compose up -d --build
```

PostgreSQL được publish ở host port `5440` và chứa database riêng cho user, content, assessment, access, learning,
library, game, community và notification. `docker compose down` giữ dữ liệu; thêm `-v` để xóa database và khởi tạo lại toàn bộ seed.

**Cạm bẫy khi dùng volume PostgreSQL cũ:** `infra/postgres/init-databases.sql` chỉ chạy khi volume `postgres-data` được khởi tạo lần đầu. Nếu đã có volume trước khi thêm Notification Service, cần tạo database một lần thủ công nếu chưa có: `docker compose exec postgres psql -U postgres -d postgres -c "CREATE DATABASE notification_db;"`. Không chạy lệnh nếu database đã tồn tại.

Sau khi tạo lại container Gateway, chờ khoảng 1–2 phút để Eureka cập nhật định tuyến trước khi kiểm tra các route qua Gateway.

**Stack quan sát (tùy chọn):** log tập trung và trace (Loki, Tempo, Alloy, Grafana) chạy bằng profile riêng, mặc định tắt. Cần `GRAFANA_ADMIN_PASSWORD` trong `.env`, và bật thêm `TRACING_ENABLED=true` nếu muốn gửi trace:

```powershell
docker compose --profile observability -f docker-compose.yml -f docker-compose.observability.yml up -d
```

Grafana chỉ mở trên `127.0.0.1:3000`. Chi tiết ở [`system-architecture.md`](system-architecture.md) mục "Tracing và log tập trung".

## 3. Quản lý schema bằng Flyway

Với service sử dụng Flyway, migration tự chạy khi service khởi động. Khi thay đổi schema:

1. Thêm file SQL vào `services/<service-name>/src/main/resources/db/migration/`.
2. Đặt tên theo mẫu `V<version>__<short_description>.sql`: version tăng dần, mô tả viết thường và dùng dấu gạch dưới; ví dụ `V1__create_initial_schema.sql`.
3. Không sửa migration đã được áp dụng; tạo migration mới với version tiếp theo cho mọi thay đổi schema.

Khi đổi nhánh mà gặp checksum mismatch hoặc migration đã chạy nhưng thiếu file local, đối chiếu tên file và checksum
với lịch sử Git trước. Giữ nguyên migration đã chạy từ nhánh chính và đánh số tiếp theo cho migration mới của nhánh
tính năng nếu trùng version. Không dùng `repair` để thay checksum của hai migration có nội dung khác nhau: thao tác
này chỉ đổi lịch sử Flyway, không chạy SQL còn thiếu.

## 4. Chạy luồng chính local (học bài → đề cuối → Learning Service)

Luồng: học viên gọi `GET /api/learning/topics` → học từng bài, nộp khối bài tập (`/api/learning/lessons/**`) → bài ôn khi
KP yếu (`/api/learning/reviews/**`) → nhận mã đề (`POST /api/learning/topics/{id}/test-assignments`) → làm đề ở
`/api/assessments/**` → outbox → RabbitMQ → consumer của Learning Service ghi bằng chứng và mở topic kế. Mọi service
Java chạy trên host (IDE hoặc `java -jar`); Compose chỉ chạy DB và RabbitMQ. Contract: `docs/contracts/lesson-learning-v1.md`,
`lesson-writing-v1.md`, `assessment-completed-v2.md`.

**Biến môi trường** (đặt trong `.env` ở root, file này đã được gitignore; không commit giá trị):

| Biến                                                                        | Dùng cho                                                                                                                                                                                                                                                                                                              |
| --------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POSTGRES_PASSWORD`                                                         | Mật khẩu PostgreSQL dùng bởi các database riêng của từng service; host port mặc định `5440`                                                                                                                                                                                                                           |
| `REDIS_PASSWORD`                                                            | Mật khẩu Redis dùng cho rate limit ở Gateway và giới hạn đăng nhập ở User Service; compose yêu cầu biến này                                                                                                                                                                                                           |
| `GAME_WEBSOCKET_ALLOWED_ORIGINS`                                            | Trang web được mở WebSocket game, phân tách bằng dấu phẩy, `*` thay cho phần bất kỳ (mặc định `http://localhost:5173`). Production đặt origin của web, ví dụ `https://app.example.com`; Expo bản web cũng cần origin của nó ở đây. App native không cần: nó gửi Origin trùng địa chỉ API nên được nhận như cùng nguồn |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`                                    | Broker trong compose; assessment và Learning Service trên host phải dùng đúng cặp này (mặc định `guest` sẽ fail)                                                                                                                                                                                                      |
| `GATEWAY_INTERNAL_JWT_SECRET`                                               | Gateway và toàn bộ downstream service phải dùng cùng giá trị, lệch sẽ trả 401                                                                                                                                                                                                                                         |
| `EXTERNAL_JWT_SECRET`                                                       | User Service ký token, Gateway xác thực                                                                                                                                                                                                                                                                               |
| `CONTENT_MEDIA_BASE_URL`                                                    | Prefix https của bucket công khai `media` cho asset PUBLIC; thiếu thì key PUBLIC trả 500 `INVALID_MEDIA_REFERENCE`                                                                                                                                                                                                    |
| `CONTENT_PREMIUM_MEDIA_BUCKET`                                              | Bucket riêng tư cho audio lesson PREMIUM và PDF tài liệu bài học; mặc định `media-premium`, URL ký có hạn 900 giây. Bucket phải cho phép `application/pdf`                                                                                                                                                                                                                            |
| `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`, `LEARNING_LLM_MODEL`       | Endpoint OpenAI-compatible để Learning Service chấm bài luận Writing; thiếu thì nộp bài luận trả 503 `GRADING_UNAVAILABLE`. API key là secret. Gemini: base URL `https://generativelanguage.googleapis.com/v1beta/openai`, model `gemini-3.8-flash` (đã thử 2026-10-02; `gemini-2.5-flash` không còn cấp cho key mới) |
| `LEARNING_LLM_REASONING_EFFORT`                                             | Mức suy luận gửi cho model. Với `gemini-3.8-flash` đặt `low`: mặc định của client cho `gemini-3*` là `minimal`, model này trả 400                                                                                                                                                                                     |
| `LEARNING_SPEECH_KEY`, `LEARNING_SPEECH_REGION`, `LEARNING_SPEECH_LOCALE` | Azure Speech cho luyện tập Speaking chấm bằng AI (Learning Service): khóa và vùng của tài nguyên Speech (ví dụ `eastasia`), giọng chuẩn để chấm phát âm (mặc định `en-US`, chỉ `en-US` có điểm ngữ điệu). Khóa là secret, chỉ để trong `.env`. Thiếu khóa hoặc vùng thì phân tích giọng nói bị từ chối (`SPEECH_NOT_CONFIGURED`), phần khác vẫn chạy. Bản ghi gửi lên phải là WAV 16 kHz mono 16-bit |
| `LEARNING_SPEAKING_ENABLED`, `LEARNING_SPEAKING_DAILY_LIMIT`, `LEARNING_SPEAKING_HISTORY_EXTRA_KP_CODES` | Bật chấm Speaking bằng AI (mặc định `false` vì rubric Speaking và bảng band phát âm chưa được giám khảo duyệt; đặt `true` để thử. Cần đặt `LEARNING_SPEECH_KEY`, `LEARNING_SPEECH_REGION`, `LEARNING_LLM_*` thì học viên mới được mời chấm, thiếu một trong ba thì `/terms` trả `enabled=false`), số bài chấm tối đa mỗi học viên mỗi ngày (mặc định `5`), và danh sách mã KP từ Writing (cách nhau dấu phẩy) được phép đưa vào phần lịch sử học viên cho LLM (mặc định rỗng) |
| `LEARNING_SPEAKING_POINT_COST`, `LEARNING_SPEAKING_BUCKET`, `LEARNING_SPEAKING_RETENTION_DAYS` | Luyện tập Speaking chấm bằng AI của Learning Service: giá điểm một bài (mặc định `5`, tạm thời), bucket (mặc định `speaking-recordings`, ghi dưới tiền tố `practice/`), số ngày giữ ghi âm (mặc định `90`). Lưu file cần `SUPABASE_URL` và `SUPABASE_SECRET_KEY`; thiếu thì upload ghi âm trả 503 `STORAGE_UNAVAILABLE`, phần khác vẫn chạy |
| `ASSESSMENT_LLM_BASE_URL`, `ASSESSMENT_LLM_API_KEY`, `ASSESSMENT_LLM_MODEL` | Endpoint OpenAI-compatible để Assessment Service chấm essay trong thi topic/course. Thiếu cấu hình, lỗi LLM hoặc hết hạn mức ngày thì chuyển bài sang EXAMINER; bài thi MOCK vẫn do EXAMINER chấm. API key là secret.                                                                                                 |
| `ASSESSMENT_LLM_DAILY_LIMIT`                                                | Số essay tối đa mỗi học viên mỗi ngày mà Assessment Service gửi chấm AI (mặc định `20`; tính theo ngày `Asia/Ho_Chi_Minh`)                                                                                                                                                                                            |
| `GRAFANA_ADMIN_PASSWORD` | Mật khẩu admin Grafana, bắt buộc khi chạy stack observability (profile `observability`) |

Nếu mật khẩu có ký tự đặc biệt, hãy percent-encode hoặc chọn giá trị an toàn cho URL, vì nó nằm trong URL DB/AMQP.

**Thứ tự khởi động:**

1. Chạy toàn stack trong container: `docker compose up -d --build`. Init script tạo database riêng cho từng service;
   Flyway áp schema/seed. User và Access ghi tài khoản/điểm demo khi `DEMO_DATA_ENABLED=true` (mặc định local bật).
2. Hoặc chạy Java từ IDE: `docker compose up -d postgres rabbitmq`, đặt từng `*_DB_URL` về
   `jdbc:postgresql://localhost:5440/<service>_db` và password bằng `POSTGRES_PASSWORD`; sau đó chạy Config Server →
   Eureka → Gateway → các business service. Các Spring module tự nạp `.env` ở root repository.
3. Mở Swagger tại Gateway `http://localhost:8080/swagger-ui.html`.

Gateway giữ path công khai: `/api/content/videos/**`, `/api/content/vocabulary/**` và
`/api/content/admin/vocabulary/**` tới library; `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**`
tới library; `/api/learning/**` tới learning, cùng goal (`/api/users/me/learning-goals/**`), study-time
(`/api/users/me/study-sessions/**`, `/api/users/me/statistics/study-time`) và activity (`/api/learning-support/activities/**`)
mà Gateway viết lại sang learning. `POST /api/learning/practice-sessions` đi thẳng tới learning-service và dùng tier rate limit `default` từ `default-filters`. Gateway chặn
`/internal/**`. Game chọn snapshot `VOCABULARY` từ library (`LIBRARY_SERVICE_URL` mặc định `http://localhost:8081`),
`GRAMMAR` từ content. Khi game chạy trên host, đặt `CONTENT_SERVICE_URL=http://localhost:8082` vì Config Server vẫn
mặc định địa chỉ `http://content-service:8082`. Library không gọi Content; video chỉ lưu `topicId` như tham chiếu. Content V7 xóa năm bảng catalog;
chỉ cho Flyway chạy migration phá hủy này trên Testcontainers cho tới khi có duyệt riêng đối với `content_db` dùng chung.

Nội dung demo Reading, Writing, Listening và lesson nhiều skill nằm trong các migration Content V9–V23. V13 thêm gợi ý
Reading ở Content; Learning Service trả `hint` cho câu từng sai trong cùng user/bài/khối chưa đạt (câu điền hoặc chọn ≥3 phương án,
TFNG hỗ trợ options thiếu/rỗng), giữ tới khi khối đạt. Contract: [lesson-learning-v1](contracts/lesson-learning-v1.md).
Listening cần 8 file mp3 upload
đúng key dưới `CONTENT_MEDIA_BASE_URL` (danh sách trong `services/content-service/README.md`). Thư mục
`third_party/deeptutor` chỉ là bản clone để **đọc**: công thức mastery của Learning Service được port từ đó, không
service nào build hay import thư mục này.
**Tài khoản demo cho frontend (chỉ dev/demo, mật khẩu công khai):** bật `DEMO_DATA_ENABLED=true` thì user-service và
access-service tự ghi tài khoản demo sau mỗi lần Flyway chạy (callback `db/callback/afterMigrate__demo_data.sql`). Mặc
định là `false`; Compose local bật sẵn (đặt `DEMO_DATA_ENABLED=false` để không ghi tài khoản/điểm demo); chạy service từ IDE thì
thêm `DEMO_DATA_ENABLED=true` vào `.env`. Ghi một lần, khởi động lại không ghi trùng và không nạp lại point đã dùng.

| Tài khoản            | userId                                 | Mật khẩu   | Ghi chú                                                     |
| -------------------- | -------------------------------------- | ---------- | ----------------------------------------------------------- |
| `learner@ielts.demo` | `00000000-0000-0000-0000-000000000001` | `Demo@123` | CUSTOMER, ví 30 point (10 lần chấm Writing)                 |
| `admin@ielts.demo`   | `00000000-0000-0000-0000-000000000002` | `Demo@123` | ADMIN, nạp point qua `POST /api/access/admin/points/adjust` |

Content seed các bài Reading, Listening, Writing, topic nhiều skill và đề placement `PLACEMENT-4SKILLS` qua các Flyway
migration (V9–V25). Placement test (`GET /api/learning/placement-test`) không bắt buộc: học viên học được mọi course khi chưa làm,
placement chỉ dùng để gợi ý course và chỉ làm được một lần. Band placement lấy từ
Listening/Reading quy đổi theo số câu đúng trên 40 (bảng IELTS, không đúng câu nào là band 0), Writing do LLM chấm
(Task 2 nặng gấp đôi Task 1; band mặc định `assessment.placement.default-writing-band` = 5.5 khi LLM không chấm được)
và Speaking cố định `assessment.placement.speaking-band` (mặc định 0 cho tới khi chấm được ghi âm). Learning tự
tạo lộ trình khi học viên gọi `GET /api/learning/topics` lần đầu, nên không cần seed thêm. Không bật cờ demo trên
database dùng chung hoặc production.

**Tài khoản có quyền (chỉ dev, chỉ trên DB local):**

1. Đăng ký tài khoản qua `POST /api/users/register`. Mọi tài khoản mới đều nhận role `CUSTOMER`.
2. Hệ thống không seed ADMIN. Gán ADMIN cho tài khoản đầu tiên bằng SQL trên `user_db` local:
   ```sql
   INSERT INTO user_roles (user_id, role_id)
   SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ADMIN'
   WHERE u.email = '<email của admin>'
   ON CONFLICT DO NOTHING;
   ```
   > ⚠️ Không bao giờ chạy lệnh này trên database dùng chung hoặc production.
3. Admin đăng nhập lại (`POST /auth/login`) để token mang role mới, rồi cấp role khác (ví dụ `EXAMINER`) qua `PUT /api/users/{id}/roles`. Người được cấp cũng phải đăng nhập lại.

Grader (`EXAMINER`/`ADMIN`) chấm qua `/api/assessments/grading/**`, xem `services/assessment-service/README.md`.
Bằng chứng E2E của lần kiểm chứng gần nhất:
`plans/260930-2057-mvp-reading-writing-listening-roadmap/reports/e2e-261002-mvp-reading-writing-listening.md`.

**Chạy backend bằng Docker** (không cần IDE, không cần Postgres local): file `docker-compose.yml` dựng config-server,
Eureka, Gateway, user, content, library, access, assessment, learning, game, community, một PostgreSQL có database riêng
cho từng service, và RabbitMQ. Community ghi transactional outbox; Notification lưu event vào `notification_db` và push qua
`/ws/notifications` sau commit. Gateway xác thực JWT trong WebSocket subprotocol.

```bash
docker compose up -d --build  # lần đầu build lâu; sau đó bỏ --build
docker compose down           # thêm -v để xóa dữ liệu và nạp lại seed
```

Cần trong `.env`: `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `EXTERNAL_JWT_SECRET`,
`GATEWAY_INTERNAL_JWT_SECRET`; tùy chọn `LEARNING_LLM_*`, `ASSESSMENT_LLM_*` (chấm Writing) và
`CONTENT_MEDIA_BASE_URL` (mặc định là URL giữ chỗ, audio chưa phát được). Cổng host: Gateway 8080, Eureka 8761,
Postgres 5440, RabbitMQ 5672/UI 15673; đổi bằng `MVP_GATEWAY_PORT`, `MVP_EUREKA_PORT`, `MVP_POSTGRES_PORT`,
`RABBITMQ_HOST_PORT`, `MVP_RABBITMQ_UI_PORT` nếu cổng đã được dùng.
Config-repo được mount chỉ đọc, sửa YAML xong chỉ cần restart service.

**Swagger UI của luồng chính:** khi Gateway và các service đã chạy, mở `http://localhost:8080/swagger-ui.html`, chọn
tài liệu (user, access, assessment, learning) ở ô "Select a definition". Đăng nhập bằng `POST /auth/login`, bấm
**Authorize** và dán `accessToken` (không kèm chữ `Bearer`); "Try it out" đi qua Gateway như client thật. Tài liệu chỉ
gồm các route của luồng chính (auth, `/api/learning/**`, `/api/assessments/attempts/**`, point của access) và không có
`/internal/**`. Schema sinh từ code, mã lỗi nghiệp vụ xem contract. Tắt bằng `API_DOCS_ENABLED=false` ngoài môi trường dev.
