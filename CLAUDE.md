# CLAUDE.md — IELTSPath backend

- Cập nhật lần cuối: 2026-10-01; dữ kiện đã kiểm với code sau khi learning-service có consumer. Về dữ kiện, code là nguồn đúng khi tài
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
| `services/user-service` | Java | 8085 | `user_db` (Postgres local 5432) | `/auth/**`, `/api/users/**`, `/api/learning-support/{activities,streak}/**` |
| `services/content-service` | Java | 8082 | `content_db` (local 5432) | `/api/content/**` trừ ba nhóm catalog chuyển tới library |
| `services/library-service` | Java | 8081 | `library_db` (compose host 5437) | `/api/content/{videos,vocabulary}/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `services/assessment-service` | Java | 8083 | `assessment_db` (local 5432) | `/api/assessments/**` |
| `services/access-service` | Java | 8084 | `access_db` (local 5432) | `/api/access/**` |
| `services/game-service` | Java | 8087 | `game_db` (compose 5435) | `/api/games/**`, ws `/ws/games/**` |
| `services/notification-service` | Java (khung) | 8088 | `notification_db` (local 5432) | `/api/notifications/**` |
| `services/community-service` | Java | 8089 | `community_db` (compose 5434, cần `COMMUNITY_DB_URL`, xem §5) | `/api/community/**` |
| `services/learning-service` | Java | 8086 | `learning_db` (compose 5436) | `/api/learning/**` |
| `third_party/deeptutor` | Python (chỉ đọc) | — | — | Nguồn của công thức mastery đã port; không build, không import |

Nguồn: `application.yml` từng module (`SERVER_PORT`), `infra/config-server/config-repo/*.yaml`, `docker-compose.yml`.

## 3. Chạy local

- Postgres local cổng 5432 phải có `user_db`, `content_db`, `assessment_db`, và khi chạy access/notification thì thêm
  `access_db`, `notification_db` (`CREATE DATABASE access_db;` …). Flyway của từng service tạo bảng khi khởi động.
- `.env` ở root (gitignored) chứa mật khẩu và secret: compose nội suy nó; Gateway và mọi service Java nghiệp vụ import nó
  (`optional:file:../../.env[.properties]`; config-server, eureka-server thì không). Chỉ ghi tên biến, không ghi giá trị. Learning Service
  cần `LEARNING_DB_PASSWORD`, `RABBITMQ_PASSWORD`; content cần `CONTENT_MEDIA_BASE_URL` (https của bucket mp3) cho bài Listening.
- Compose yêu cầu `LIBRARY_DB_PASSWORD` trong `.env` dù chỉ bật một phần stack. Chỉ chạy các container cần dùng:
  `docker compose up -d rabbitmq learning-db`
  (+ `library-db`, `community-db`, `game-db` khi cần).
- Service Java chạy trên host (IDE hoặc `java -jar`) theo thứ tự: config-server → eureka → api-gateway → user → content →
  library → assessment → các service còn lại (kể cả game-service, xem §5).
- RabbitMQ: AMQP `127.0.0.1:5672`, UI `127.0.0.1:15672`. Learning Service tự khai báo queue, retry queue và DLQ khi khởi động.
- Hướng dẫn luồng chính và cấu hình Compose: `README.md` §6.

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
- Compose yêu cầu `LIBRARY_DB_PASSWORD` ngay cả khi chưa bật `library-db`, vì Compose nội suy toàn bộ file.
- Container `game-service` trong compose trỏ `http://config-server:8888` mà compose không có config-server: đừng dùng
  container này, chạy game-service trên host.
- community-service mặc định `localhost:5432/community_db`; dùng DB compose thì đặt
  `COMMUNITY_DB_URL=jdbc:postgresql://localhost:5434/community_db` (và mật khẩu) trong `.env`.
- assessment dùng RabbitMQ với `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` từ `.env`; mặc định `guest` sẽ bị broker từ chối.
- `GATEWAY_INTERNAL_JWT_SECRET` phải giống nhau ở Gateway, mọi service Java, lệch là 401. Config-repo có giá
  trị fallback cho secret (thiếu biến env thì chạy bằng secret công khai trong repo): không dựa vào, không tự sửa, báo người dùng.
- Outbox của access/content/game chỉ ghi, chưa có relay: event của các service này không tới consumer.
- notification-service chỉ là khung package.
- Role chuẩn là `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF` (`LEARNER` cũ đã đổi thành `CUSTOMER`).
- `graphify-out/` không được commit.
- Bài Listening (content V12) trả 500 `INVALID_MEDIA_REFERENCE` khi thiếu `CONTENT_MEDIA_BASE_URL`; 8 file mp3 phải upload
  đúng key (bảng trong `services/content-service/README.md`).

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
