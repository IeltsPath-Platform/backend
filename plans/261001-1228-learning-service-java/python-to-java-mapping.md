# Đọc plan con viết cho ai-learning Python khi code learning-service Java

Các plan con (1640 phase 8, 0737, 0812, 0851, 1006) viết trước ngày 2026-10-01 nên còn tên file và API của service Python.
**Luật nghiệp vụ, mã lỗi, response, test case giữ nguyên**; chỉ đổi nơi đặt code theo bảng dưới. Agent code đọc file này
trước phase con nào nhắc `services/ai-learning-service`. Gặp chỗ không ánh xạ được thì dừng, báo `NEEDS_DECISION`.

## Ánh xạ

| Plan con viết (Python) | Làm ở learning-service (Java) |
| --- | --- |
| `services/ai-learning-service/app/...` | `services/learning-service/src/main/java/com/group01/learning/{domain,application,api,infrastructure}` theo AGENTS §3.1 |
| `migrations/V<n>__x.sql` | `src/main/resources/db/migration/V<n>__x.sql`; số theo bảng "Migration" bên dưới |
| Route `/api/ai-learning/...` | `/api/learning/...` |
| `require_current_user`, `AuthenticatedUser` | `CurrentUserProvider` (`common-security`) |
| Lấy bearer qua `Depends(bearer_scheme)` | Header `Authorization` của request hiện tại, forward cùng `X-Correlation-Id` |
| `PostgresLearningStore.transaction(path)`, "path transaction" | `@Transactional` (hoặc `TransactionTemplate` khi cần nhiều transaction ngắn) + `pg_advisory_xact_lock` theo user ở đầu transaction |
| `app/lessons/gates.py`, `authorize_lesson_access` | `LessonAccessGate` (phase 2 của `261001-1228`) |
| `app/lessons/review_rule.py`, `reevaluate_reviews` | `ReviewRule` |
| `app/learning/practice_evidence.py`, `source = X` | insert `kp_evidence` với `source = X`; UQ(`user_id`, `source`, `source_reference_id`) thay index `mastery_learning_evidence` |
| `formal_provenance.py` thêm source | thêm giá trị vào CHECK/enum `source` của `kp_evidence` (migration mới) |
| `path_review_items`, `path_review_sets` | `review_items`, `review_sets` |
| `LearningGateError(code, status, extra)` | exception nghiệp vụ map trong `GlobalExceptionHandler` → `{detail, code, ...}` |
| `app/clients/content_service.py` | port `LearningContentClient` + adapter `RestClient` |
| Client httpx mới (access, …) | port trong `application/port`, adapter `RestClient` ở `infrastructure/client` |
| `app/llm/client.py`, `ChatCompletionsClient`, `json_with_reasoning_retry` | port `LlmClient` + adapter `RestClient` gọi `/chat/completions` OpenAI-compatible; JSON hỏng thì thử lại một lần; không thêm Spring AI |
| Setting `AI_LEARNING_X` (`app/config.py`) | `learning.x` trong `config-repo/learning-service.yaml`, env `LEARNING_X`; validate bằng `@ConfigurationProperties` + `@Validated` |
| Biến env của container `ai-learning-api` trong compose | Không có container: service chạy trên host, cấu hình ở config-repo |
| DTO pydantic `extra="forbid"`, allowlist | Java record chỉ có trường cho phép; map từ payload content, không trả thẳng payload |
| `pytest`, `httpx.MockTransport`, `OpenAiStub` | JUnit + Mockito, `MockRestServiceServer`; test DB bằng Testcontainers |
| Lệnh `python -m pytest tests -p no:cacheprovider` | `mvn -q -pl services/learning-service -am test` |
| `mastery_learning_evidence.hints_used` | Không có cột này; bỏ mọi chỗ nhắc |

## Migration learning-service

| Version | Plan |
| --- | --- |
| V1 | `261001-1228` (toàn bộ schema MVP lõi) |
| V2 | 0737: `lesson_writing_submissions`, `llm_daily_usage` (hạn mức chấm Writing), thêm `lesson_writing` vào `kp_evidence.source` |
| — | 0812, 0851, 1006: không migration learning-service (1006 dùng index có sẵn ở V1) |

## Đổi quyết định của 0737 (người dùng chốt 2026-10-01)

- **Có hạn mức chấm Writing theo ngày** (trước đây "không tính `llm_daily_usage`"): bảng `llm_daily_usage(user_id, usage_date,
  kind, count)` PK(`user_id`, `usage_date`, `kind`); ngày theo `learning.quota-timezone` (mặc định `Asia/Ho_Chi_Minh`);
  `learning.writing.daily-grading-limit` mặc định 10. Tăng đếm (có điều kiện `count < limit`) ngay trước khi gọi LLM, trong
  transaction ngắn riêng; hết lượt → 429 `DAILY_LIMIT_REACHED`, không gọi LLM, không trừ point. LLM lỗi không trả lại lượt.
  Áp cho cả Task 1 (0812).
