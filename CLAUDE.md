# CLAUDE.md — IELTSPath backend

- Cập nhật lần cuối: 2026-09-27, commit `889d5dc`. Code là nguồn đúng khi tài liệu lệch.
- Quy tắc bắt buộc (stack, layer, bảo mật, điều cấm, quy trình) nằm trong `AGENTS.md`, được nạp ngay dưới đây.
- Kiến trúc, flow, quyết định: `docs/system-architecture.md`. Làm việc trong `services/ai-learning-service/` thì đọc thêm
  `services/ai-learning-service/CLAUDE.md` (tự nạp khi mở file trong service đó).

@AGENTS.md

## 1. Hệ thống

Microservices cho nền tảng học IELTS. Client → API Gateway (8080) → service; Gateway xác thực external JWT rồi ký
internal JWT cho downstream. 8 service Java (Spring Boot, Maven reactor, Eureka `lb://`) + 1 service Python (AI Learning,
FastAPI, ngoài Maven, Gateway gọi bằng URI cố định). Assessment báo kết quả thi cho AI Learning qua outbox + RabbitMQ
(`AssessmentCompleted.v2`). Mỗi service sở hữu một PostgreSQL DB. Nhánh đang phát triển nhiều nhất: AI Learning.

## 2. Module

| Module | Ngôn ngữ | Cổng | DB (nơi chạy) | Route Gateway |
| --- | --- | --- | --- | --- |
| `infra/config-server` | Java | 8888 | — | — |
| `infra/eureka-server` | Java | 8761 | — | — |
| `infra/api-gateway` | Java (WebFlux) | 8080 | — | — |
| `shared/common-security` | Java lib | — | — | — |
| `services/user-service` | Java | 8085 | `user_db` (Postgres local 5432) | `/auth/**`, `/api/users/**` |
| `services/content-service` | Java | 8082 | `content_db` (local 5432) | `/api/content/**` |
| `services/assessment-service` | Java | 8083 | `assessment_db` (local 5432) | `/api/assessments/**` |
| `services/access-service` | Java | 8084 | `access_db` (local 5432) | `/api/access/**` |
| `services/learning-support-service` | Java | 8086 | `learning_support_db` (compose 5433) | `/api/learning-support/**` |
| `services/game-service` | Java | 8087 | `game_db` (compose 5435) | `/api/games/**`, ws `/ws/games/**` |
| `services/notification-service` | Java (khung) | 8088 | `notification_db` (local 5432) | `/api/notifications/**` |
| `services/community-service` | Java | 8089 | `community_db` (compose 5434) | `/api/community/**` |
| `services/ai-learning-service` | Python 3.11 | 8000 | `ai_learning_db` (compose 5436) | `/api/ai-learning/**` |
| `third_party/deeptutor` | Python | — | — | Chỉ để đọc khi port; không import |

Nguồn: `application.yml` từng module (`SERVER_PORT`), `infra/config-server/config-repo/*.yaml`, `docker-compose.yml`.

## 3. Chạy local

- Postgres local cổng 5432 phải có `user_db`, `content_db`, `assessment_db`, và khi chạy access/notification thì thêm
  `access_db`, `notification_db` (`CREATE DATABASE access_db;` …). Flyway của từng service tạo bảng khi khởi động.
- `.env` ở root (gitignored) chứa mật khẩu và secret: compose nội suy nó, mọi module Spring import nó
  (`optional:file:../../.env[.properties]`). Chỉ ghi tên biến vào tài liệu, không ghi giá trị.
- Compose chỉ chạy một phần stack:
  `docker compose up -d --build rabbitmq ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer`
  (+ `learning-support-db`, `community-db`, `game-db` khi cần các service đó).
- Service Java chạy trên host (IDE hoặc `java -jar`) theo thứ tự: config-server → eureka → api-gateway → user → content →
  assessment → các service còn lại.
- AI Learning trong container gọi User/Content trên host qua `host.docker.internal`; RabbitMQ UI ở `127.0.0.1:15672`.
- Hướng dẫn đầy đủ: `README.md` §4–6.

## 4. Lệnh hay dùng

```powershell
mvn -q -pl services/<name>-service -am test     # test một service (Testcontainers cần Docker)
mvn -q -pl shared/common-security test
mvn -q compile -DskipTests                       # cả reactor; tránh `clean` khi service đang chạy từ IDE
docker compose config --quiet                    # kiểm compose + .env
docker compose ps
```

Test Python và lệnh riêng của AI Learning: `services/ai-learning-service/CLAUDE.md`.

## 5. Cạm bẫy đã biết

- `CONTENT_SERVICE_URL` của game-service trong config-repo mặc định `http://content-service:8082` (tên trong Docker) và ghi
  đè default local → khi chạy game trên host, đặt `CONTENT_SERVICE_URL=http://localhost:8082`.
- Container `game-service` trong compose trỏ `http://config-server:8888`, nhưng compose không có config-server.
- assessment dùng RabbitMQ với `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` từ `.env`; mặc định `guest` sẽ bị broker từ chối.
- `GATEWAY_INTERNAL_JWT_SECRET` phải giống nhau ở Gateway, mọi service Java và AI Learning, lệch là 401. Config-repo có giá
  trị fallback cho secret: thiếu biến env thì service chạy bằng secret công khai trong repo.
- Outbox của access/content/game chỉ ghi, chưa có relay: event của các service này không tới consumer.
- notification-service chỉ là khung package; README user-service ghi `PATCH` cho đổi trạng thái user, controller là `PUT`.
- Role chuẩn là `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF` (`LEARNER` cũ đã đổi thành `CUSTOMER`).
- `.pyc` và `graphify-out/` không được commit; VS Code có thể tự chạy pytest discovery và sinh bytecode khi sửa file test.

## 6. Tài liệu tra cứu

| Cần gì | Đọc |
| --- | --- |
| Kiến trúc, flow, giao tiếp, điểm chưa nhất quán | `docs/system-architecture.md` |
| Contract HTTP/SSE/event | `docs/contracts/` (`tutor-sse-v1`, `practice-v1`, `assessment-completed-v2`) |
| Schema toàn bộ database | `.sdd/database/DATABASE_V5.md` |
| Chạy local, biến môi trường | `README.md`, `infra/README.md`, README từng service |
| Cấu hình runtime | `infra/config-server/config-repo/<service>.yaml` |
| Kế hoạch và quyết định đang làm | `plans/` (mỗi plan có `plan.md` + phase), `plans/reports/` |

## graphify

Graph ở `graphify-out/` chỉ chứa code first-party (đã loại `third_party/` và `*.md` trong `.graphifyignore`).

- Câu hỏi về codebase: chạy `graphify query "<câu hỏi>"`, `graphify explain "<Symbol>"` hoặc
  `graphify path "<A>" "<B>"` trước khi grep/đọc nhiều file. Tên trùng thì dùng `path::Symbol`.
- Chỉ đọc `graphify-out/GRAPH_REPORT.md` khi cần review kiến trúc rộng.
- Sau khi sửa code: `graphify update .` (AST, không tốn API); thêm `--force` khi số node giảm có chủ đích.
