---
phase: 4
title: "Minimal compose and end-to-end verification"
status: pending
priority: P1
dependencies: [1, 2, 3]
effort: "~5h"
---

# Phase 4: Minimal compose and end-to-end verification

## Overview
Thêm vào compose phần tối thiểu để AI Learning chạy thật: DB riêng, Flyway chạy một lần, API và consumer. Sau đó chạy toàn bộ test cần Docker và kiểm chứng toàn luồng bằng HTTP thật qua Gateway: learner làm bài → grader finalize → outbox → RabbitMQ → consumer → DeepTutor → `/status`.

Pha này không thêm code nghiệp vụ. Lỗi phát hiện ở đây được sửa trong đúng module của pha 1–3, và phải có test tái hiện lỗi trước khi sửa.

## Context
- Compose hiện có:
  - `learning-support-db` (5433), `community-db` (5434), `game-db` (5435), `rabbitmq` (5672/15672), `game-service`.
  - Không có Config, Eureka, Gateway, User, Content, Assessment: các service Java chạy local/IDE (quyết định đã chốt).
- Theo config-repo, User, Content và Assessment mặc định dùng PostgreSQL local `localhost:5432` (`user_db`, `content_db`, `assessment_db`).
- Gateway route `/api/ai-learning/**` tới `${AI_LEARNING_SERVICE_URI:http://localhost:8000}`. Đây là URL tĩnh, không qua Eureka.
- `services/ai-learning-service/Dockerfile` đã có:
  - Build context là root repo; Dockerfile chạy `pip install ./third_party/deeptutor`, rồi chạy `uvicorn main:app` ở cổng 8000.
  - Version của DeepTutor lấy từ attribute, không cần `.git`.
- AI Learning API chuyển tiếp internal JWT của learner thẳng tới User (`/api/users/me/learning-goals/active`) và Content (`/api/content/topics`, `/api/content/knowledge-points`). Vì vậy base URL phải trỏ thẳng vào service (User 8085, Content 8082), không đi qua Gateway.
- Env mà AI Learning cần:
  - `ConsumerSettings` chỉ cần `AI_LEARNING_DATABASE_URL` và `AI_LEARNING_AMQP_URL`.
  - `Settings` của API cần thêm `AI_LEARNING_INTERNAL_JWT_SECRET` (Base64, giải mã ra ít nhất 32 byte) và hai base URL.
- Test cần Docker, hiện đang skip:
  - Assessment: `AssessmentSchemaValidationTest` (1) và `AssessmentOutboxIntegrationTest` (3, cộng 1 test chuỗi grader của pha 3).
  - Content: `KnowledgePointLearningTypeMigrationTest` (1).
  - Python: `tests/test_assessment_rabbitmq.py` (3).
- Máy dev có Docker CLI 29.1.3 nhưng Docker Desktop chưa chạy. Testcontainers đang dùng bản 1.21.4.
- Tạo tài khoản có quyền:
  - Không có seed ADMIN.
  - `UserManagementController` (`hasRole('ADMIN')`) đã có `PUT /api/users/{id}/roles`.
  - Vì vậy chỉ cần tạo ADMIN đầu tiên bằng SQL (chỉ dev). Các role khác cấp qua API có sẵn.
- Start attempt chỉ gọi User (goal đang active) và Content (mapping KP theo `questionVersionId`). `packageVersionId` và `contentSectionId` không được đối chiếu với Content.
- Content `learningType` nhận `MEMORY`, `CONCEPT`, `PROCEDURE`, `DESIGN`. Với DeepTutor, `CONCEPT` và `DESIGN` là KP định tính (`QUALITATIVE_TYPES`).

## Requirements
- Functional:
  - Compose thêm các service sau (theo quyết định "thêm tối thiểu"):
    - `ai-learning-db`: postgres:15-alpine, `127.0.0.1:5436`.
    - `ai-learning-migrate`: Flyway chạy một lần.
    - `ai-learning-api`.
    - `ai-learning-consumer`.
  - Không thêm `assessment-db` (đã chốt). Assessment dùng `assessment_db` trên PG local như User/Content, theo đúng mặc định trong config.
  - Trên máy sạch (volume mới), lệnh `docker compose up -d rabbitmq ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer` phải chạy được.
  - Flyway thật áp `0.1 → 1 → 2 → 3` trên DB rỗng; chạy lại thì không làm gì.
  - Consumer tự khai báo topology (queue chính, retry, DLQ, binding) khi khởi động.
  - 9 test cần Docker chạy thật và PASS.
  - E2E qua Gateway pass: có path trước, chưa có path (hộp chờ), gọi lặp (idempotency), chấm lại.
- Non-functional:
  - Compose không có secret viết thẳng, chỉ dùng `${VAR:?Set VAR}` như convention hiện có. Không commit `.env`.
  - Container consumer chỉ nhận env DB và AMQP, không nhận JWT secret.
  - Chỉ publish port trên `127.0.0.1`.
  - Không thêm thư viện. Không sửa Dockerfile nếu build được (xem rủi ro image nặng).
  - Tài liệu và report không chứa giá trị secret, token hay email thật.

## Architecture
```text
Host (IDE / mvn spring-boot:run)             Docker compose (codebase-network)
  config-server :8888                          rabbitmq :5672 / :15672
  eureka        :8761                          ai-learning-db :5436 ◄── ai-learning-migrate (Flyway, 1 lần)
  gateway       :8080 ─ /api/ai-learning/** ─► ai-learning-api :8000 ──┐ internal JWT của learner
  user    :8085 ◄──────── host.docker.internal ─────────────────────────┤
  content :8082 ◄──────── host.docker.internal ─────────────────────────┘
  assessment :8083 ─ outbox relay ─► rabbitmq ─► ai-learning-consumer ─► ai-learning-db
  user_db, content_db, assessment_db: PostgreSQL local :5432
```
Phác thảo từng service (YAML chính xác viết lúc cook):
- `ai-learning-db`:
  - `POSTGRES_DB: ai_learning_db`, `POSTGRES_PASSWORD: "${AI_LEARNING_DB_PASSWORD:?Set AI_LEARNING_DB_PASSWORD}"`.
  - Volume `ai-learning-db-data`, healthcheck `pg_isready` như các DB khác.
- `ai-learning-migrate`:
  - Image `flyway/flyway` cùng major với Flyway phía Java (11).
  - Env: `FLYWAY_URL=jdbc:postgresql://ai-learning-db:5432/ai_learning_db`, `FLYWAY_USER`, `FLYWAY_PASSWORD` (từ `AI_LEARNING_DB_PASSWORD`), `FLYWAY_LOCATIONS=filesystem:/flyway/sql`.
  - Mount `./services/ai-learning-service/migrations:/flyway/sql:ro`, `command: migrate`, chờ DB healthy.
- `ai-learning-api`:
  - Build context `.`, dockerfile `services/ai-learning-service/Dockerfile`, port `127.0.0.1:8000:8000`.
  - Env:
    - `AI_LEARNING_INTERNAL_JWT_SECRET: "${GATEWAY_INTERNAL_JWT_SECRET:?…}"`: phải cùng giá trị mà Gateway dùng.
    - `AI_LEARNING_DATABASE_URL: postgresql://postgres:${AI_LEARNING_DB_PASSWORD:?…}@ai-learning-db:5432/ai_learning_db`.
    - `AI_LEARNING_USER_SERVICE_BASE_URL: ${AI_LEARNING_USER_SERVICE_BASE_URL:-http://host.docker.internal:8085}`.
    - `AI_LEARNING_CONTENT_SERVICE_BASE_URL: ${AI_LEARNING_CONTENT_SERVICE_BASE_URL:-http://host.docker.internal:8082}`.
  - `extra_hosts: ["host.docker.internal:host-gateway"]`.
  - `depends_on` migrate với điều kiện `service_completed_successfully`.
- `ai-learning-consumer`:
  - Dùng chung block build với API (YAML anchor, lần build thứ hai trúng cache).
  - `command: ["python", "-m", "app.messaging.assessment_consumer"]`.
  - Env chỉ gồm:
    - `AI_LEARNING_DATABASE_URL`.
    - `AI_LEARNING_AMQP_URL: amqp://${RABBITMQ_USERNAME:?…}:${RABBITMQ_PASSWORD:?…}@rabbitmq:5672/%2F`.
  - `depends_on`: rabbitmq healthy và migrate hoàn tất.
  - `restart: unless-stopped`, vì process pika thoát khi mất kết nối broker.

## Related Code Files
- Modify:
  - `docker-compose.yml`: các service ở trên và volume `ai-learning-db-data`.
  - `README.md` (root): mục "Chạy luồng chính local". Gồm tên biến môi trường (không ghi giá trị), thứ tự khởi động, bước tạo ADMIN chỉ dùng cho dev.
  - `services/ai-learning-service/README.md`: chạy API, consumer và migrate bằng compose; env cho từng process.
  - Chỉ khi test Docker lộ lỗi: sửa file của pha 1–3 trong đúng module, kèm test tái hiện.
- Create: `plans/260924-2135-main-flow-blockers/reports/e2e-verification-<YYMMDD-HHMM>-main-flow-report.md` (bằng chứng, đã che token và secret).
- Không tạo:
  - `.env` được commit.
  - Script seed hay code mới cho role.
  - Service Java trong compose.

## Implementation Steps
### Tests Before (ghi mốc, xác nhận đỏ)
1. Bật Docker Desktop; `docker info` chạy được. Ghi version Engine vào report.
2. Chạy gate Java khi đã có Docker, trước khi sửa compose:
   ```powershell
   mvn -o -pl services/assessment-service,services/content-service -am test
   ```
   - 6 test Testcontainers (5 Assessment + 1 Content) giờ phải chạy, không còn skip.
   - Nếu fail thì dừng. Sửa ở module gốc, viết test tái hiện trước, rồi mới đi tiếp.
3. Chạy test RabbitMQ Python:
   - `docker compose up -d rabbitmq`.
   - Đặt `AI_LEARNING_TEST_AMQP_URL=amqp://<user>:<pass>@127.0.0.1:5672/%2F` và `AI_LEARNING_TEST_DATABASE_URL` (DB tạm), rồi chạy suite Python. Kỳ vọng 3 test RabbitMQ chạy và pass.
4. Probe đỏ, trước khi thêm service:
   - `docker compose config --services` chưa có `ai-learning-*`.
   - `curl http://127.0.0.1:8000/health` thất bại.
   - Queue `ai-learning.assessment-completed.v2` chưa tồn tại (xem qua management UI hoặc `rabbitmqctl list_queues`).
### Implement
5. Thêm các service vào compose. Chạy `docker compose config --quiet` với `.env` local, rồi rà lại để chắc chắn không có password hay secret nào viết thẳng.
6. Chạy migrate:
   - `docker compose up -d ai-learning-db ai-learning-migrate`: migrate kết thúc với exit 0.
   - `docker compose run --rm ai-learning-migrate info`: `0.1`, `1`, `2`, `3` đều Success.
   - `migrate` lần hai báo schema đã cập nhật.
7. Chạy API và consumer:
   - `docker compose up -d --build ai-learning-api ai-learning-consumer`.
   - `/health` trả 200.
   - Log consumer cho thấy đã khai báo topology. RabbitMQ có queue chính, retry, DLQ và binding `assessment.completed.v2`.
### Tests After: E2E thật (runbook)
Điều kiện trước:
- PostgreSQL local có `user_db`, `content_db`, `assessment_db`. Flyway của từng service tự áp khi khởi động.
- Chạy local theo thứ tự: config-server → eureka → gateway → user → content → assessment. Env cho các service này:
  - `RABBITMQ_USERNAME` và `RABBITMQ_PASSWORD` giống compose.
  - `GATEWAY_INTERNAL_JWT_SECRET` giống `.env`.
- Mọi request đi qua Gateway `http://localhost:8080`.

8. Tài khoản:
   - Đăng ký `learner-a`, `learner-b`, `examiner` và `admin` qua `POST /api/users/register`. Tất cả nhận role CUSTOMER.
   - **Chỉ dev, chỉ trên DB local**: gán ADMIN cho `admin` trong `user_db`:
     ```sql
     INSERT INTO user_roles (user_id, role_id)
     SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ADMIN'
     WHERE u.email = '<email của admin>'
     ON CONFLICT DO NOTHING;
     ```
   - `admin` đăng nhập lại (`POST /auth/login`) để token có role mới.
   - `admin` cấp `EXAMINER` cho `examiner` qua `PUT /api/users/{id}/roles` (API có sẵn, không dùng SQL). `examiner` đăng nhập lại.
9. Nội dung (dùng tài khoản `admin`):
   - Topic ACTIVE.
   - KP-A `learningType=CONCEPT` (thứ tự 1) và KP-B `learningType=PROCEDURE` (thứ tự 2).
   - Q1 có version map tới KP-A; Q2 có version map tới KP-B, tạo qua `QuestionController`.
10. Kịch bản A: path có trước.
    - `learner-a` tạo goal qua `POST /api/users/me/learning-goals`.
    - Gọi `GET /api/ai-learning/status`: path được tạo. Ghi lại `revision` và objective (kỳ vọng KP-A).
    - Start attempt qua `POST /api/assessments/attempts` với 2 item (version của Q1 và Q2), rồi submit.
    - Kiểm tra DB `assessment_db`:
      - `assessment_attempts.learning_goal_id` bằng goal vừa tạo.
      - `attempt_item_knowledge_points` có snapshot của 2 item.
    - `examiner` chấm:
      - Mở version kết quả: `POST /api/assessments/grading/attempts/{attemptId}/results`.
      - Lưu chi tiết: `PUT .../results/{resultId}/details` với `overallBand`, Q1 có judgment PASS cho KP-A, Q2 có `score`/`maxScore`.
      - Finalize: `POST .../results/{resultId}/finalize`. Result COMPLETED mang đúng band grader đã gửi.
    - Outbox có đúng 1 dòng `AssessmentCompleted.v2` cho result, và `published_at` được set trong vài giây.
    - Log consumer ghi outcome `applied`; queue chính và DLQ đều rỗng.
    - Gọi lại `GET /api/ai-learning/status`:
      - `revision` tăng đúng 1.
      - Objective chuyển sang KP-B, vì KP-A là KP định tính đã PASS còn KP-B mới có 1 attempt nên còn dưới gate.
      - Kết quả phải khớp với `next_objective()` của DeepTutor trên state đã lưu.
    - Kiểm tra DB `ai_learning_db`:
      - `mastery_learning_evidence` có dòng `source='assessment_service'` cho các item.
      - `formal_assessment_result_versions` ghi version 1.
11. Idempotency:
    - Gọi finalize lần nữa: trả 200, không thêm dòng outbox.
    - Gửi lại đúng event: chỉ trên DB dev, đặt `published_at = NULL` cho dòng outbox đó để relay publish lại. Consumer phải ghi outcome `duplicate` và `revision` không đổi.
12. Chấm lại:
    - `examiner` mở version 2, lưu chi tiết khác (ví dụ Q2 sai), rồi finalize.
    - Consumer ghi `applied` cho v2; ledger lên version 2.
    - Evidence chỉ còn dữ liệu của v2, không đếm đôi: số attempt của KP-B vẫn là 1.
13. Kịch bản B: chưa có path (hộp chờ).
    - `learner-b` tạo goal nhưng không gọi `/status`. Làm bài, `examiner` chấm và finalize.
    - Consumer ghi outcome `pending`:
      - `pending_formal_assessment_results` có 1 dòng.
      - Queue chính và DLQ rỗng, không có retry.
    - `learner-b` gọi `GET /api/ai-learning/status` lần đầu:
      - Path được tạo và đã phản ánh kết quả; objective giống kịch bản A.
      - Bảng chờ rỗng.
14. Phân quyền qua Gateway: token của learner gọi 3 endpoint grading đều nhận 403.
15. Ghi bằng chứng vào report: status code, số dòng DB, log consumer rút gọn. Che token, Authorization header, mật khẩu và email thật.
### Regression Gate
16. Chạy lại `mvn -o -pl services/assessment-service,services/content-service -am test`: 0 fail, 0 test skip vì thiếu Docker.
17. Suite Python với `AI_LEARNING_TEST_DATABASE_URL` và `AI_LEARNING_TEST_AMQP_URL`: 0 fail, 0 skip.
18. `git status` không có `.env`; chạy `git diff --check` và `graphify update .`.

## Success Criteria
- [ ] `docker compose config --quiet` pass; compose không có secret viết thẳng.
- [ ] Flyway thật áp `0.1 → 1 → 2 → 3` trên DB rỗng; chạy lại không làm gì.
- [ ] 9 test cần Docker đều PASS (5 Assessment, 1 Content, 3 Python RabbitMQ); không còn test NOT EXECUTED.
- [ ] Kịch bản A: 1 lần finalize tạo 1 dòng outbox, được áp dụng đúng 1 lần; `/status` đổi objective đúng theo DeepTutor và `revision` tăng 1.
- [ ] Finalize lặp và message lặp không đổi learning state.
- [ ] Chấm lại: chỉ version mới nhất có hiệu lực, không đếm đôi.
- [ ] Kịch bản B: event nằm trong hộp chờ, không vào DLQ; `/status` đầu tiên áp dụng nó.
- [ ] Learner nhận 403 ở endpoint grading khi đi qua Gateway.
- [ ] README root và README của AI Learning có cách chạy, danh sách tên biến môi trường và bước tạo ADMIN chỉ dùng cho dev.
- [ ] Report E2E trong `reports/` có bằng chứng thật.

## Risk Assessment
- **Image AI Learning nặng.** `pip install ./third_party/deeptutor` kéo cả stack LLM/RAG (openai, anthropic, llama-index, faiss-cpu, PyMuPDF…), dù luồng chính không nạp module nào trong số đó.
  - Hậu quả: build lâu, có thể lỗi wheel.
  - Xử lý: build được thì giữ nguyên, vì phạm vi đang HOLD. Nếu build lỗi thì dừng và hỏi. Phương án thay thế là cài DeepTutor không kèm deps và dùng `PYTHONPATH` như test local; không tự đổi.
- **`host.docker.internal` trên Windows.** Firewall có thể chặn container gọi service Java trên host.
  - Xử lý: cho Java đi qua firewall (mạng private), hoặc chạy API AI Learning bằng uvicorn trên host (Gateway vẫn trỏ `localhost:8000`). Ghi cách đã dùng vào report.
- **Docker Engine 29 với Testcontainers 1.21.4.** Có thể báo lỗi client API quá cũ.
  - Xử lý: đặt `api.version` trong `~/.docker-java.properties`. Nếu cần nâng version Testcontainers thì hỏi trước.
- **Lịch sử Flyway cũ trên PG local.** `content_db` nào từng áp `V2__add_knowledge_point_learning_type` (trước khi đổi tên thành V3) sẽ fail khi Flyway validate.
  - Xử lý (chỉ dev): tạo lại DB local. Không chạy `repair` trên DB dùng chung.
- **Secret JWT lệch** giữa Gateway (IDE) và AI Learning (compose) sẽ gây 401. Hai phía phải dùng cùng một giá trị từ `.env`.
- **User RabbitMQ.** Compose tạo user theo `RABBITMQ_USERNAME`, không có `guest`. Assessment trong IDE phải dùng đúng credential này; mặc định `guest` trong config sẽ fail.
- **Compose nội suy toàn bộ file.** Mọi biến `:?`, kể cả `POSTGRES_PASSWORD` và `GAME_DB_PASSWORD`, phải có trong `.env`, dù chỉ chạy một phần service.
- **Mật khẩu có ký tự đặc biệt** trong URL DB hoặc AMQP: dùng giá trị an toàn cho URL, hoặc percent-encode.

## Security Considerations
- Secret chỉ đi qua `.env` (đã gitignore) hoặc env. Compose chỉ tham chiếu `${VAR:?}`; tài liệu chỉ ghi tên biến.
- Consumer không nhận `AI_LEARNING_INTERNAL_JWT_SECRET`, vì không cần và để giảm phạm vi lộ.
- Tạo ADMIN bằng SQL chỉ dành cho DB local. README ghi cảnh báo rõ; không thêm seed hay endpoint mới.
- Port chỉ bind `127.0.0.1`.
- Report E2E không chứa token, Authorization header, mật khẩu hay email thật.

## Câu hỏi mở
Không còn. `assessment-db` đã chốt bỏ khỏi compose: config Assessment mặc định trỏ tới PG local, giống User/Content, nên thêm DB trong compose chỉ tạo thêm bước override env mà không tách biệt được luồng.
