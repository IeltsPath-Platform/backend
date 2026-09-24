---
date: 2026-09-24
kind: verification-report
phase: 4
scope: main flow Assessment finalize → RabbitMQ → DeepTutor → /status (no LLM)
---

# Kiểm chứng E2E luồng chính

Chạy trong container Linux (cloud session), không phải máy Windows của nhóm. Token, Authorization
header, mật khẩu và email đều không được ghi; UUID trong log đã được thay bằng `<uuid>`.

## Môi trường

| Thành phần | Phiên bản / cách chạy |
| --- | --- |
| Docker Engine | 29.3.1 |
| Testcontainers | 1.21.4, chạy được với Engine 29, không cần chỉnh `api.version` |
| PostgreSQL local | 16 (`user_db`, `content_db`, `assessment_db`), tạo mới trước lần chạy cuối |
| Compose | `rabbitmq`, `ai-learning-db`, `ai-learning-migrate`, `ai-learning-api`, `ai-learning-consumer` trên volume mới (`docker compose down -v` trước khi chạy) |
| Service Java | `java -jar` trên host: config-server → eureka → api-gateway → user → content → assessment |

Khác biệt chỉ do sandbox:

- Proxy HTTPS của sandbox dùng CA riêng, nên `pip install` trong lúc build image bị lỗi `CERTIFICATE_VERIFY_FAILED`.
  Image được build bằng một bản sao tạm của `services/ai-learning-service/Dockerfile` có thêm CA đó (không commit),
  rồi tag đúng tên compose dùng (`backend-ai-learning-api`, `backend-ai-learning-consumer`) và chạy `up --no-build`.
  Dockerfile và compose trong repo giữ nguyên.
- `host.docker.internal:host-gateway` chạy được trên Linux; rủi ro firewall Windows chưa được kiểm chứng ở đây.

## Tests Before

- Python (PostgreSQL, chưa có RabbitMQ): 57 pass, 3 skip, đúng mốc của plan.
- Maven `assessment-service` + `content-service` khi đã có Docker: **1 lỗi có sẵn**.
  `AssessmentOutboxIntegrationTest.relayPublishesOnlyCommittedOutboxRows` báo
  `404 NOT_FOUND - no queue 'test.assessment-completed.…'`. Xem mục "Lỗi phát hiện và đã sửa".
- Probe đỏ trước khi sửa compose: `docker compose config --services` chưa có `ai-learning-*`;
  `curl http://127.0.0.1:8000/health` → không kết nối được (`000`).

## Compose và migrate

- `docker compose config --quiet` pass. Mọi password và secret trong compose đều là `${VAR:?}`; không có giá trị viết thẳng.
- `ai-learning-migrate` trên DB rỗng: `Successfully applied 4 migrations … now at version v3`, exit 0.
- `docker compose run --rm ai-learning-migrate info`: `0.1`, `1`, `2`, `3` đều ở trạng thái Success.
- Chạy `migrate` lần hai: `Schema "public" is up to date. No migration necessary.`
- `/health` → 200 `{"status":"UP","service":"ai-learning-service"}`.
- Consumer log `Consuming ai-learning.assessment-completed.v2`. RabbitMQ có queue chính, `.retry`, `.dlq`
  và binding `assessment.events` → `ai-learning.assessment-completed.v2` (`assessment.completed.v2`).
- Image AI Learning: khoảng 1.6 GB, build khoảng 3 phút (stack đầy đủ của DeepTutor), không lỗi wheel.

## E2E qua Gateway (`http://localhost:8080`)

Script chạy các bước 8–14 của runbook. Kết quả của lần chạy cuối trên stack sạch:

| Bước | Kiểm tra | Kết quả |
| --- | --- | --- |
| 8 | 4 tài khoản đăng ký, tất cả nhận role `CUSTOMER` | ✅ |
| 8 | SQL cấp ADMIN (chỉ dev) cập nhật 1 dòng; admin cấp `EXAMINER` qua `PUT /api/users/{id}/roles` | ✅ 200 |
| 9 | Topic ACTIVE, KP-A `CONCEPT`, KP-B `PROCEDURE`, Q1→KP-A và Q2→KP-B qua `QuestionController` | ✅ |
| 10 | `/status` trước khi làm bài: `probe` KP-A, revision 1 | ✅ |
| 10 | `learning_goal_id` của attempt bằng goal vừa tạo; có 2 dòng snapshot KP | ✅ |
| 10 | Finalize: COMPLETED, band 6.5 (band của grader); 1 dòng outbox, `published_at` được set | ✅ |
| 10 | Consumer `applied`; queue chính, retry và DLQ đều 0 message | ✅ |
| 10 | `/status` sau khi chấm: `practice` **KP-B**, revision 2 (+1) | ✅ |
| 10 | `mastery_learning_evidence` có 2 dòng `assessment_service`; ledger ở version 1 | ✅ |
| 10 | `next_objective()` của DeepTutor trên `state_json` đã lưu trùng với `/status` | ✅ |
| 11 | Finalize lần 2 → 200, vẫn 1 dòng outbox | ✅ |
| 11 | Đặt lại `published_at = NULL` (DB dev) → consumer `duplicate`, revision không đổi | ✅ |
| 12 | Chấm lại version 2 (Q2 sai) → consumer `applied`; ledger ở version 2 | ✅ |
| 12 | KP-B chỉ còn 1 evidence (`incorrect`), không đếm đôi; revision +1 | ✅ |
| 13 | learner-b: consumer `pending`, 1 dòng trong hộp chờ, chưa có path, queue chính/DLQ = 0 | ✅ |
| 13 | `/status` đầu tiên của learner-b: revision 1, objective trùng kịch bản A, hộp chờ còn 0 dòng | ✅ |
| 14 | Token learner gọi 3 endpoint grading qua Gateway → `403, 403, 403` | ✅ |

Log consumer (rút gọn):

```text
INFO __main__ AssessmentCompleted.v2 <uuid> applied on path <uuid> (version 1, 2 evidence)
INFO __main__ AssessmentCompleted.v2 <uuid> duplicate on path <uuid> (version 1, 0 evidence)
INFO __main__ AssessmentCompleted.v2 <uuid> applied on path <uuid> (version 2, 2 evidence)
INFO app.application.formal_assessment_ingestion Parked AssessmentCompleted.v2 <uuid>: no path yet for goal <uuid>
INFO __main__ AssessmentCompleted.v2 <uuid> pending until the path of goal <uuid> exists (version 1)
```

Lần chạy đầu (trước khi reset DB) cho objective trỏ vào KP của một topic còn sót lại từ lần chạy lỗi trước đó.
Curriculum lấy toàn bộ topic ACTIVE, nên DeepTutor chọn KP chưa được đánh giá là đúng. Lần chạy cuối dùng DB sạch.

## Lỗi phát hiện và đã sửa

Mỗi lỗi đều có test tái hiện chạy đỏ trước khi sửa.

1. **Assessment, test outbox (có sẵn, chỉ lộ ra khi có Docker).** Queue test khai báo `autoDelete=true`.
   `receive(queue, timeout)` tạo consumer rồi hủy nó, nên RabbitMQ xóa queue sau lần receive đầu và lần sau gặp 404.
   Đã sửa bằng queue không auto-delete, xóa trong `finally` (commit pha 3).
2. **Content, `POST /api/content/questions/{id}/versions` (chặn bước 9).** Mapping KP lấy `questionVersionId` từ
   client, trong khi id của version do server sinh ra, nên API trả 500 hoặc map sai version. Giờ mapping luôn gắn
   vào version đang tạo; `questionVersionId` client gửi bị bỏ qua. Test: `AddQuestionVersionUseCaseTest`.
3. **User, `POST /api/users/me/learning-goals` (chặn bước 10).** Use case tự gán `UUID.randomUUID()` cho entity có
   `@GeneratedValue`, nên Spring Data `merge` một entity chưa tồn tại và Hibernate 6.6 ném
   `StaleObjectStateException` → 500. Giờ để Hibernate sinh id như các entity khác.
   Test: `LearningGoalCreationPersistenceTest` (Testcontainers).

## Regression Gate

- `mvn -o -pl services/assessment-service,services/content-service,services/user-service -am test`:
  common-security 8, user 93, content 38, assessment 56; **0 fail, 0 skip**. Các test Testcontainers đều chạy thật:
  `AssessmentSchemaValidationTest` (1), `AssessmentOutboxIntegrationTest` (4), `KnowledgePointLearningTypeMigrationTest` (1),
  `LearningGoalConstraintMigrationTest` (2), `LearningGoalCreationPersistenceTest` (1).
- Python với `AI_LEARNING_TEST_DATABASE_URL` và `AI_LEARNING_TEST_AMQP_URL` (RabbitMQ của compose):
  **80 pass, 0 skip** (4 test RabbitMQ).
- `git status` không có `.env`; `git diff --check` sạch.
- `graphify update .`: **chưa chạy**, vì `graphify` không được cài trong môi trường này. Cần chạy trên máy dev.

## Còn mở

- `ForgotPasswordUseCase` cũng tự gán `UUID.randomUUID()` cho `AccountActionTokenJpaEntity` (có `@GeneratedValue`).
  Nhiều khả năng dính cùng lỗi với mục 3; nằm ngoài phạm vi plan này nên chưa sửa.
- Rủi ro firewall Windows với `host.docker.internal`, và `content_db` từng áp `V2__add_knowledge_point_learning_type`
  cũ, chưa được kiểm chứng ở đây (xem phần rủi ro của pha 4).
