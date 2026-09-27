# Brainstorm: gỡ blocker luồng chính (hoãn cấu hình LLM)

- Ngày: 2026-09-24 · Branch: `main` · Chế độ: markdown (không `--html`, không `--wiki`)
- Luồng chính: Assessment finalize → outbox → RabbitMQ → AI Learning consumer → DeepTutor → PostgreSQL → `next_objective()` → `GET /api/ai-learning/status`

## 1. Câu hỏi và kết luận

Câu hỏi: task "CONFIG LLM / MODEL API KEY" có cần để chạy luồng chính không?

Kết luận: **không cần**. Bằng chứng từ source local:

- Nạp toàn bộ `main.py` và `app.messaging.assessment_consumer`: không có module LLM hay provider nào được nạp. Chỉ có `deeptutor.learning.{models, service, storage, policy, scheduler, mastery, grading, pending}`, `services.file_io`, `services.path_service` và `utils`.
- Môi trường test không cài `openai`/`anthropic`, nhưng cả 57 test đã qua, kể cả E2E trên PostgreSQL thật.
- Trong `deeptutor/learning/`, chỉ `topic_generation.py` và `topic_naming.py` gọi LLM. IELTSPath không dùng hai file này vì curriculum lấy từ Content.
- Plan Phase 1 đã hoãn Tutor Chat, session runtime và RAG (`.sdd/specs/ai-learning-phase1-plan.md`, mục *Explicitly deferred*).
- Contract LLM thật của DeepTutor là `data/user/settings/model_catalog.json` cộng key pool; runtime settings dùng env làm override và tự export `OPENAI_API_KEY`/`OPENAI_BASE_URL`. Làm bây giờ thì sẽ phải tự đặt tên biến, và thành cấu hình chết (YAGNI).

Phát hiện phụ, **không thuộc phạm vi đợt này**:
- 8/9 file config-repo có giá trị mặc định đã commit cho `GATEWAY_INTERNAL_JWT_SECRET`. Người dùng quyết định chưa xử lý.
- `CLAUDE.md` còn ghi role ADMIN/LEARNER, trong khi thực tế đã là CUSTOMER, EXAMINER, … Validator Python đã đúng, không chặn luồng.

## 2. Quyết định đã chốt

| Hạng mục | Quyết định |
|---|---|
| Task LLM | Hoãn sang khi có component dùng model (Tutor Chat, chấm AI). Khi làm thì theo contract `model_catalog`/runtime settings của DeepTutor. Key chấm AI Writing/Speaking thuộc Assessment, là secret riêng. |
| Secret JWT mặc định | Không làm đợt này |
| Learner chưa có path khi event tới | **Hộp chờ trong `ai_learning_db`** |
| Trigger finalize | **Endpoint cho EXAMINER/ADMIN** trước; tự chấm câu khách quan để sau |
| Migration `ai_learning_db` | **Baseline DDL V5 + Flyway one-shot container** |
| Compose | **Thêm tối thiểu**; service Java chạy local/IDE |

## 3. Thiết kế từng hạng mục

### A. Môi trường chạy 8 test chưa thực thi (không cần code)
- Bật Docker Desktop, rồi chạy `mvn -pl services/assessment-service,services/content-service -am test` (Testcontainers).
- Chạy test RabbitMQ Python với `AI_LEARNING_TEST_AMQP_URL` trỏ vào RabbitMQ trong compose.
- Đạt khi: 4 test Java Assessment, 1 test migration Content và 3 test RabbitMQ Python đều PASS.

### B. Baseline `ai_learning_db` + Flyway
- File mới: `services/ai-learning-service/migrations/V0_1__create_v5_mastery_tables.sql`.
  - Version nhỏ hơn 1 để giữ V1/V2. **Cần xác minh** Flyway sắp thứ tự đúng.
  - Chỉ tạo bảng store thực dùng: `mastery_paths`, `mastery_interactions`, `mastery_events`. `mastery_learning_evidence` đã có trong V2.
  - Không tạo `mastery_path_sessions`/`mastery_path_leases` (YAGNI).
- `mastery_interactions.status`: DeepTutor ghi chữ thường (`registered`, …), còn V5 ghi chữ hoa. DDL phải khớp DeepTutor. Giữ partial unique index "một interaction active cho mỗi path".
- Flyway: image `flyway/flyway`, mount thư mục `migrations`, chạy `migrate` một lần trước API và consumer.
- Test Python dùng chính file baseline thay cho DDL chép tay trong `tests/postgres_schema_support.py`, để chỉ có một nguồn sự thật.
- Rủi ro: môi trường đã áp V1 bằng tay thì cần `baselineOnMigrate`/`baselineVersion`. Ghi rõ trong README.
- Đạt khi: DB rỗng → `flyway migrate` → `POST /paths` và `/status` chạy được; test PG Python pass khi dùng file baseline.

### C. Endpoint grader (assessment-service)
- Controller mới `/api/assessments/grading/**` với `@PreAuthorize("hasAnyRole('EXAMINER','ADMIN')")`, theo convention `AdminAccessController`/`TopicController`:
  - `POST /attempts/{attemptId}/results`: mở result version mới (regrade).
  - `PUT /results/{resultId}/details`: lưu điểm kỹ năng, kết quả từng item (`score`, `max_score`, `is_correct`), lỗi, judgment PASS/FAIL/NOT_ASSESSED.
  - `POST /results/{resultId}/finalize`: gọi `FinalizeAssessmentResultUseCase`.
- Các use case hiện khóa theo learner sở hữu (`userId + attemptId`). Cần thêm lối vào theo `resultId`/`attemptId` cho grader, dùng lại phần validate.
- Kiểm tra route gateway có chuyển `/api/assessments/**` không.
- Đạt khi:
  - Token CUSTOMER bị 403.
  - EXAMINER finalize thành công, có đúng 1 dòng outbox.
  - Kết quả chưa chấm đủ trả 400.
  - Finalize lần 2 không tạo event mới.

### D. Hộp chờ (ai-learning-service)
- Migration mới: bảng `pending_formal_assessment_results` gồm `event_id` (PK), `user_id`, `learning_goal_id`, `attempt_id`, `result_version`, `payload` (jsonb), `received_at`. Index `(user_id, learning_goal_id)`.
- Consumer: khi `PathNotBootstrapped` thì ghi vào hộp chờ (`ON CONFLICT (event_id) DO NOTHING`) rồi ACK. Lỗi contract vẫn vào DLQ.
- Tạo path (`ensure_path`, dùng chung cho API và consumer): sau khi tạo path, lấy các kết quả đang chờ của `(user, goal)` theo thứ tự `(attempt_id, result_version)` và đưa qua `FormalAssessmentIngestionService`, trong cùng transaction của path; xóa dòng đã áp dụng.
- Chống race giữa consumer ghi hộp chờ và luồng tạo path: dùng `pg_advisory_xact_lock(hash(user_id, learning_goal_id))` ở cả hai phía. Ledger version đảm bảo không áp dụng hai lần.
- Đạt khi:
  - Event tới trước khi có path: được ACK và có một dòng trong hộp chờ.
  - `/status` đầu tiên tạo path, áp dụng kết quả chờ, và `next_objective` phản ánh đúng.
  - Redelivery không nhân đôi.
  - Consumer và bootstrap chạy song song vẫn chỉ áp dụng một lần.
  - Version cũ trong hộp chờ bị bỏ qua.

### E. Compose tối thiểu
- Thêm:
  - `assessment-db` và `ai-learning-db` (postgres:15-alpine, port `127.0.0.1` còn trống, sau 5433–5435).
  - `ai-learning-migrate` (Flyway, `service_completed_successfully`).
  - `ai-learning-api` (build `services/ai-learning-service/Dockerfile`, context repo root).
  - `ai-learning-consumer` (cùng image, command `python -m app.messaging.assessment_consumer`).
- Secret theo convention `${X:?Set X}`; không commit `.env`, vì `.gitignore` đã có `.env`.
- API trong container gọi User/Content chạy trên máy qua `host.docker.internal`. Cần xác minh trên Docker Desktop Windows.
- Đạt khi: các service healthy, consumer khai báo được queue, `/api/ai-learning/health` trả UP.

## 4. Thứ tự và phụ thuộc

```text
A (làm ngay khi bật Docker)
B baseline + Flyway ──► D hộp chờ (migration chạy qua Flyway) ──► E compose ──► chạy E2E toàn luồng
C endpoint grader (song song với B/D) ─────────────────────────────┘
```

## 5. Ngoài phạm vi
- Cấu hình LLM/model provider và dọn secret JWT mặc định.
- Tự chấm câu khách quan, tích hợp chấm AI, và phân công/queue chấm (`human_reviews`).
- Compose full stack (config, eureka, gateway, user, content, assessment).
- Tutor Chat, RAG, SessionStore.

## 6. Rủi ro
- Cách đánh version Flyway nhỏ hơn 1 chưa xác minh. Nếu không được thì phải đổi tên V1/V2, việc này chỉ an toàn khi chưa có môi trường nào đã áp chúng.
- Hộp chờ phình với những goal không bao giờ được mở. Tạm chấp nhận; dọn dẹp để sau.
- Mọi EXAMINER chấm được mọi result, vì chưa có mô hình phân công.
- Endpoint cũ cho learner tự tạo result (band tự khai) vẫn còn. Nay chỉ tạo DRAFT, không có quyền finalize.

## 7. Tiêu chí thành công E2E
- Learner bắt đầu bài (goal và snapshot KP được ghi lại) rồi submit.
- EXAMINER lưu chi tiết chấm và finalize.
- Outbox → RabbitMQ → consumer.
- `GET /api/ai-learning/status` đổi objective đúng theo `next_objective` của DeepTutor.
- Trường hợp chưa có path: event nằm trong hộp chờ, `/status` đầu tiên áp dụng nó.
- Toàn bộ test PASS, không còn test NOT EXECUTED nào khi có Docker.

## 8. Câu hỏi mở
- Có giới hạn EXAMINER chỉ chấm bài được giao (`human_reviews`) ngay đợt này không? Đề xuất hiện tại là không.
- Kết quả trong hộp chờ được giữ bao lâu?
- Có giữ endpoint learner `POST /api/assessments/attempts/{id}/result` không?
