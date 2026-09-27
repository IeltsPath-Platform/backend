# CLAUDE.md — ai-learning-service

- Cập nhật lần cuối: 2026-09-27, commit `889d5dc`. Quy tắc chung: `AGENTS.md` (§3.8 riêng cho service này).
- Python 3.11, FastAPI + uvicorn, pydantic-settings (tiền tố `AI_LEARNING_`), psycopg2, httpx, pika. Không thuộc Maven.
- Mô tả đầy đủ và cấu hình: `README.md` trong thư mục này.

## Chạy

- Compose (từ root): `docker compose up -d --build rabbitmq ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer`.
  API ở `127.0.0.1:8000`, DB ở `127.0.0.1:5436`; `ai-learning-migrate` chạy Flyway một lần rồi thoát 0.
- Trên host (trong thư mục này): `uvicorn main:app --reload --port 8000`. `Settings` đọc `.env` của thư mục đang chạy
  (service có `.env` riêng); container thì nhận biến từ `docker-compose.yml`, không từ `.env` của service.
- Khởi động sẽ đánh dấu lượt tutor bị gián đoạn là failed, và dừng nếu `AI_LEARNING_QUOTA_TIMEZONE` không phải tên IANA
  PostgreSQL biết hoặc thiếu bảng `llm_daily_usage`.

## Package `app/`

| Package | Vai trò |
| --- | --- |
| `api/` | Router FastAPI (`tutor.py`, `practice.py`), SSE (`tutor_sse.py`), DTO camelCase (`dto/`), dependency wiring |
| `application/` | `PathService`, sắp thứ tự path (`path_orderer.py`), nhận kết quả thi (`formal_assessment_ingestion.py`) |
| `adapters/` | Map dữ liệu Content/Assessment vào model mastery |
| `clients/` | httpx tới Content và User (chuyển tiếp internal JWT của learner) |
| `mastery/` | Engine mastery port từ DeepTutor v1.6.9: model, chấm, policy, scheduler, `LearningService` |
| `tutor/` | `TutorEngine` (vòng tool-calling ≤ 6 vòng), tools, prompts, session store, learner memory, bài đọc |
| `practice/` | Practice notebook và lịch ôn theo câu |
| `usage/` | Hạn mức lượt tutor/tóm tắt memory theo ngày (`DailyQuotaStore`) |
| `learning/` | LLM sắp thứ tự, provenance, placement test-out |
| `llm/` | Client OpenAI-compatible (`AI_LEARNING_LLM_*`) |
| `messaging/` | Consumer `AssessmentCompleted.v2` và topology retry/DLQ |
| `persistence/`, `security/` | `PostgresLearningStore`; kiểm internal JWT |

## Bất biến

- Không import `deeptutor` và không tham chiếu `third_party/` (`tests/test_no_deeptutor_dependency.py`). Muốn tham khảo
  thì đọc `third_party/deeptutor` rồi port, ghi nguồn ở đầu file.
- `app/mastery`: giữ hành vi, ngưỡng và dạng `state_json`; test `tests/test_mastery_*.py` giữ giá trị gốc của DeepTutor.
- Một lượt tutor luôn kết thúc `completed` hoặc `failed` (có `failure_code`), kể cả khi lỗi, hết hạn mức hay client ngắt.
- Không log nội dung hội thoại, câu trả lời, prompt hay key; log chỉ có id, loại event, mã lỗi, `error_type`.
- Test không gọi LLM thật: dùng `ScriptedChat` (`tests/test_tutor_engine_postgres.py`) hoặc stub cục bộ.
- Store mở một kết nối psycopg2 mỗi lần gọi (`closing(psycopg2.connect(url))`); gọi từ route qua `asyncio.to_thread`.
- DTO trả ra dùng alias camelCase (`ApiResponse`); kiểm internal JWT phải khớp `common-security` (issuer, subject, role).

## Test

```powershell
# venv Python 3.11 (Dockerfile dùng python:3.11-slim); .venv ở root repo là của công cụ khác, không có pytest
python -m pip install pytest -r requirements-test.txt
$env:PYTHONDONTWRITEBYTECODE = "1"
$env:AI_LEARNING_TEST_DATABASE_URL = "<URL PostgreSQL dùng để test>"   # thiếu thì test PostgreSQL bị skip
$env:AI_LEARNING_TEST_AMQP_URL = "<URL RabbitMQ dùng để test>"          # thiếu thì test RabbitMQ bị skip
python -m pytest tests
```

- Mỗi lớp test PostgreSQL tạo schema riêng từ `migrations/V*.sql` (`tests/postgres_schema_support.py`) rồi xóa đi; không
  có DDL viết tay trong test. Mốc 2026-09-27: 432 passed, 0 skip.
- Test API ghi đè dependency của FastAPI (`main.app.dependency_overrides`) và xóa cache `get_settings` khi đổi env.
- E2E tutor qua Gateway và `llm-stub` (`LLM_STUB_MODE=script`, tài khoản `TUTOR_E2E_*` hoặc `--register-disposable`):
  `tests/e2e/tutor_e2e.py`, hướng dẫn ở `README.md` root mục "Kiểm chứng tutor study/review qua Gateway". E2E sắp thứ
  tự path bằng LLM: `README.md` của service, mục "Five-case stub E2E recipe".

## Migration và contract

- Thêm `migrations/V<n+1>__<mô_tả>.sql` (hiện tới `V9__llm_daily_usage.sql`), cập nhật bảng migration trong `README.md`,
  `tests/test_migrations_postgres.py` và `.sdd/database/DATABASE_V5.md` mục AI Learning.
- Contract: `docs/contracts/tutor-sse-v1.md`, `docs/contracts/practice-v1.md`, `docs/contracts/assessment-completed-v2.md`.
  Đổi route, payload SSE hoặc event thì cập nhật contract trong cùng commit.
