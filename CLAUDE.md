# CLAUDE.md — IELTSPath backend

- Cập nhật lần cuối: 2026-10-07; dữ kiện đã kiểm với code sau course path, placement recommendation và course gate. Về dữ kiện, code là nguồn đúng khi tài
  liệu lệch; về quy tắc, xem thứ tự ưu tiên đầu `AGENTS.md`.
- Quy tắc bắt buộc (stack, layer, bảo mật, điều cấm, quy trình) nằm trong `AGENTS.md`, được nạp ngay dưới đây.
- Kiến trúc, flow, quyết định: `docs/system-architecture.md`.

@AGENTS.md

## 1. Hệ thống

Microservices cho nền tảng học IELTS. Client → API Gateway (8080) → service; Gateway xác thực external JWT rồi ký
internal JWT cho downstream. 9 service Java nghiệp vụ (Spring Boot, Maven reactor, Eureka `lb://`). Assessment báo kết quả
thi cho Learning Service qua outbox + RabbitMQ: exchange `assessment.events`, routing key `assessment.completed.v2`,
queue `learning.assessment-completed.v2` (có retry
và DLQ). Mỗi service sở hữu một PostgreSQL DB.

## 2. Module

| Module | Ngôn ngữ | Cổng | DB (nơi chạy) | Route Gateway |
| --- | --- | --- | --- | --- |
| `infra/config-server` | Java | 8888 | — | — |
| `infra/eureka-server` | Java | 8761 | — | — |
| `infra/api-gateway` | Java (WebFlux) | 8080 | — | — |
| `shared/common-security` | Java lib | — | — | — |
| `services/user-service` | Java | 8085 | `user_db` (Compose PostgreSQL host 5440) | `/auth/**`, `/api/users/**`, `/api/learning-support/{activities,streak}/**` |
| `services/content-service` | Java | 8082 | `content_db` (Compose PostgreSQL host 5440) | `/api/content/**` trừ ba nhóm catalog chuyển tới library |
| `services/library-service` | Java | 8081 | `library_db` (Compose PostgreSQL host 5440) | `/api/content/{videos,vocabulary}/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `services/assessment-service` | Java | 8083 | `assessment_db` (Compose PostgreSQL host 5440) | `/api/assessments/**` |
| `services/access-service` | Java | 8084 | `access_db` (Compose PostgreSQL host 5440) | `/api/access/**` |
| `services/game-service` | Java | 8087 | `game_db` (Compose PostgreSQL host 5440) | `/api/games/**`, ws `/ws/games/**` |
| `services/notification-service` | Java (khung) | 8088 | Chưa có DB runtime | Chưa triển khai |
| `services/community-service` | Java | 8089 | `community_db` (Compose PostgreSQL host 5440) | `/api/community/**` |
| `services/learning-service` | Java | 8086 | `learning_db` (Compose PostgreSQL host 5440) | `/api/learning/**` |
| `third_party/deeptutor` | Python (chỉ đọc) | — | — | Nguồn của công thức mastery đã port; không build, không import |

Nguồn: `application.yml` từng module (`SERVER_PORT`), `infra/config-server/config-repo/*.yaml`, `docker-compose.yml`.

## 3. Chạy local

- Root `docker-compose.yml` là Compose duy nhất: một PostgreSQL server ở host port 5440 có database riêng cho từng
  service đã triển khai; init script tạo các database, Flyway của mỗi service tạo schema và seed.
- `.env` ở root (gitignored) chứa mật khẩu và secret: compose nội suy nó; Gateway và mọi service Java nghiệp vụ import nó
  (`optional:file:../../.env[.properties]`; config-server, eureka-server thì không). Chỉ ghi tên biến, không ghi giá trị.
  Compose cần `POSTGRES_PASSWORD`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `EXTERNAL_JWT_SECRET`,
  `GATEWAY_INTERNAL_JWT_SECRET`. Learning Service chấm bài luận cần `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`,
  `LEARNING_LLM_MODEL` (thiếu thì nộp bài luận trả 503, phần khác vẫn chạy); essay trong thi topic/course của Assessment
  dùng `ASSESSMENT_LLM_BASE_URL`, `ASSESSMENT_LLM_API_KEY`, `ASSESSMENT_LLM_MODEL`, `ASSESSMENT_LLM_DAILY_LIMIT`
  (mặc định 20; thiếu cấu hình thì gate essay chuyển EXAMINER) và access-service đang chạy
  (`ACCESS_SERVICE_URL`, mặc định `http://localhost:8084`); content cần `CONTENT_MEDIA_BASE_URL` (https của bucket mp3) cho bài Listening.
- `docker compose up -d --build` chạy toàn stack. Khi chạy service Java từ IDE, khởi động database và broker bằng
  `docker compose up -d postgres rabbitmq`, rồi đặt các `*_DB_URL` thành `jdbc:postgresql://localhost:5440/<service>_db`
  và password thành `POSTGRES_PASSWORD`.
- Service Java chạy trên host (IDE hoặc `java -jar`) theo thứ tự: config-server → eureka → api-gateway → user → content →
  library → assessment → các service còn lại (kể cả game-service, xem §5).
- RabbitMQ: AMQP `127.0.0.1:5672`, UI `127.0.0.1:15673`. Learning Service tự khai báo queue, retry queue và DLQ khi khởi động.
- Hướng dẫn luồng chính và cấu hình Compose: `README.md` §6.
- Full stack trong container: `docker compose up -d --build`. Cổng mặc định: Gateway 8080, Eureka 8761, PostgreSQL 5440,
  RabbitMQ 5672/UI 15673. Đổi cổng qua `MVP_GATEWAY_PORT`, `MVP_EUREKA_PORT`, `MVP_POSTGRES_PORT`,
  `RABBITMQ_HOST_PORT`, `MVP_RABBITMQ_UI_PORT`.
- Swagger UI luồng chính: `http://localhost:8080/swagger-ui.html` (Gateway proxy `/api-docs/<service>` → `/v3/api-docs`
  của user/access/assessment/learning). Thêm route vào tài liệu thì sửa `springdoc.paths-to-match` trong config-repo của
  service đó.

## 4. Lệnh hay dùng

```powershell
mvn -q -pl services/<name>-service -am test     # test một service (Testcontainers cần Docker)
mvn -q -pl shared/common-security test
mvn -q -pl infra/api-gateway test
mvn -q compile -DskipTests                       # cả reactor; tránh `clean` khi service đang chạy từ IDE
docker compose config --quiet                    # kiểm compose + .env
docker compose ps
```

Test service học: `mvn -q -pl services/learning-service -am test` (Testcontainers cần Docker).

## 5. Cạm bẫy đã biết

- `CONTENT_SERVICE_URL` của game-service trong config-repo mặc định `http://content-service:8082` (tên trong Docker) và ghi
  đè default local → khi chạy game trên host, đặt `CONTENT_SERVICE_URL=http://localhost:8082`.
- Game gọi library qua `LIBRARY_SERVICE_URL` (mặc định `http://localhost:8081`) cho `VOCABULARY`, gọi content qua
  `CONTENT_SERVICE_URL` cho `GRAMMAR`. Library gọi `GET /api/content/topics/{id}` để kiểm topic khi ghi video.
- PostgreSQL init script chỉ chạy khi volume mới được tạo. `docker compose down -v` xóa schema và toàn bộ seed để khởi tạo lại.
- Jar build bằng `package` không `clean` sau khi code bị xóa/chuyển service vẫn chứa class cũ trong `target/classes` (content
  từng chết khi khởi động vì `VocabularyRepositoryAdapter`): dùng `mvn clean package` khi không có service nào chạy từ IDE.
- Entity map cột `jsonb` từ `String` phải có `@JdbcTypeCode(SqlTypes.JSON)`; thiếu thì PostgreSQL từ chối khi ghi (outbox
  access từng làm mọi lần trừ point lỗi 500). Test Mockito không bắt được, cần test Testcontainers.
- Trong Compose, Game gọi Content và Library qua tên service; khi chạy Game trên host, đặt `CONTENT_SERVICE_URL=http://localhost:8082`.
- `community-service` mặc định `localhost:5432/community_db`; khi chạy trên host với DB Compose, đặt
  `COMMUNITY_DB_URL=jdbc:postgresql://localhost:5440/community_db` và `COMMUNITY_DB_PASSWORD` trong `.env`.
- assessment dùng RabbitMQ với `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` từ `.env`; mặc định `guest` sẽ bị broker từ chối.
- `GATEWAY_INTERNAL_JWT_SECRET` phải giống nhau ở Gateway, mọi service Java, lệch là 401. Config-repo có giá
  trị fallback cho secret (thiếu biến env thì chạy bằng secret công khai trong repo): không dựa vào, không tự sửa, báo người dùng.
- Assessment phát `COURSE_GATE` cho bài thi `COURSE_TEST`; Learning phải được cập nhật để nhận type này trước khi Assessment
  phát event. Nếu event đến Learning bản cũ, nó vào DLQ; restart Learning rồi replay theo quy trình trong contract.
- Essay đã nộp trong `TOPIC_GATE`/`COURSE_GATE` được chấm LLM miễn phí; thiếu cấu hình, LLM lỗi hoặc hết quota ngày thì
  chuyển sang hàng chờ EXAMINER. Essay không được nộp qua `/api/assessments/submissions` sẽ nhận 0 điểm và không tạo job.
  `MOCK` vẫn do EXAMINER chấm.
- Outbox của access/content/game chỉ ghi, chưa có relay: event của các service này không tới consumer.
- notification-service chỉ là khung package.
- Role chuẩn là `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF` (`LEARNER` cũ đã đổi thành `CUSTOMER`).
- `graphify-out/` không được commit.
- Bài Listening seed (content V12, reference dạng key) trả 500 `INVALID_MEDIA_REFERENCE` khi thiếu `CONTENT_MEDIA_BASE_URL`;
  URL `https://` đầy đủ không cần base. 8 file mp3 phải upload đúng key (bảng trong `services/content-service/README.md`).

## 6. Tài liệu tra cứu

| Cần gì | Đọc |
| --- | --- |
| Kiến trúc, flow, giao tiếp, điểm chưa nhất quán | `docs/system-architecture.md` |
| Invariant cấp dự án (ưu tiên cao nhất) | `.sdd/global/constitution.md`; các file khác trong `.sdd/global`, `.sdd/constraints` là baseline cũ |
| Contract HTTP/event | `docs/contracts/` (`lesson-learning-v1`, `lesson-writing-v1`, `learning-content-internal-v1`, `answer-spec-v1`, `assessment-completed-v2`) |
| Schema toàn bộ database | `.sdd/database/DATABASE_V5.md` |
| Chạy local, biến môi trường | `README.md`, `infra/README.md`, README từng service |
| Cấu hình runtime | `infra/config-server/config-repo/<service>.yaml` |
| Kế hoạch và quyết định đang làm | `plans/` (mỗi plan có `plan.md` + phase), `plans/reports/` |

## graphify

Graph ở `graphify-out/` chỉ chứa code first-party (đã loại `third_party/` và `*.md` trong `.graphifyignore`).

- Khi `graphify-out/graph.json` tồn tại, câu hỏi về codebase: chạy `graphify query "<câu hỏi>"`, `graphify explain "<Symbol>"` hoặc
  `graphify path "<A>" "<B>"` trước khi grep/đọc nhiều file. Tên trùng thì dùng `path::Symbol`.
- Chỉ đọc `graphify-out/GRAPH_REPORT.md` khi cần review kiến trúc rộng.
- Sau khi sửa code: `graphify update .` (AST, không tốn API); thêm `--force` khi số node giảm có chủ đích.
