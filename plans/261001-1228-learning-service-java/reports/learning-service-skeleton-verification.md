# Kiểm chứng khung Learning Service

- Ngày: 2026-10-01
- Nhánh: `feat/learning-service-skeleton`
- Phạm vi: [phase 1](../phase-01-xoa-python-dung-khung.md) hoàn tất phần code; [plan](../plan.md) đang thực hiện,
  1/5 phase hoàn tất (20%). Phase 2–5 vẫn pending.
- Review: hoàn tất, không còn finding cần xử lý.

## Kết quả triển khai

| Hạng mục | Dữ kiện đã kiểm |
| --- | --- |
| Gỡ Python | Xóa 135 file được Git quản lý dưới `services/ai-learning-service/`; không còn code service Python được track. |
| Gỡ tài liệu cũ | Xóa `docs/ai-learning-database.md`, `docs/contracts/practice-v1.md`, `docs/contracts/tutor-sse-v1.md`. |
| Khung Java | `services/learning-service`, package `com.group01.learning`, Spring Boot, cổng mặc định 8086, cấu hình Eureka và `common-security`; health qua Actuator. Chưa có nghiệp vụ. |
| Reactor | POM gốc có 13 module: ba infra, một shared, chín business service. |
| Persistence | Flyway `V1__learning_schema.sql` tạo chín bảng: `knowledge_point_catalog`, `topic_progress`, `lesson_progress`, `lesson_exercise_submissions`, `review_items`, `review_sets`, `topic_test_assignments`, `kp_evidence`, `assessment_result_versions`. |
| Cấu hình | `learning-service.yaml`: DB `learning_db` ở host 5436, `ddl-auto: validate`; `LEARNING_DB_PASSWORD` không có fallback. |
| Compose | Thêm `learning-db` và `learning-db-data`, host `127.0.0.1:5436`, yêu cầu `LEARNING_DB_PASSWORD`; gỡ các service/volume `ai-learning-*` và `llm-stub`. Learning Service chạy trên host. |
| Gateway | Route `lb://learning-service`, predicate `/api/learning/**`, prefix internal JWT `/api/learning`; route và prefix AI Learning cũ đã gỡ. |
| Contract | `lesson-learning-v1.md` đổi prefix và mô tả `GET /mastery`; `assessment-completed-v2.md` đổi consumer/topology sang Learning Service. Các API và consumer này sẽ triển khai ở phase 2–4. |

Nguồn: [module README](../../../services/learning-service/README.md),
[migration](../../../services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql),
[runtime config](../../../infra/config-server/config-repo/learning-service.yaml),
[lesson contract](../../../docs/contracts/lesson-learning-v1.md),
[event contract](../../../docs/contracts/assessment-completed-v2.md).

## Gate kiểm chứng

| Gate | Kết quả |
| --- | --- |
| `mvn -q -pl services/learning-service -am test` | Exit 0. `common-security`: 8 run, 8 pass, 0 failure, 0 error, 0 skip. Learning Service: 3 run, 3 pass, 0 failure, 0 error, 0 skip. |
| `mvn -q -pl infra/api-gateway test` | Exit 0. 8 run, 8 pass, 0 failure, 0 error, 0 skip. |
| `mvn -q compile -DskipTests` | Exit 0 cho cả reactor; đây là compile, không phải chạy test toàn bộ reactor. |
| `docker compose config --quiet` | Fail vì thiếu `LEARNING_DB_PASSWORD`. Người dùng cần tự thêm biến vào `.env`, rồi chạy lại; chưa ghi nhận pass. |
| JSON trong contract | 17 fenced JSON block của lesson contract và một block của event contract đều parse hợp lệ. |
| Reference cũ trong file được track | `git grep` chỉ còn kết quả trong bốn tài liệu liệt kê bên dưới khi loại `plans/`, `docs/journals/`, `.sdd/`, `third_party/`. |

Tổng hai lệnh test: **19 run, 19 pass, 0 failure, 0 error, 0 skip**. Báo cáo Surefire hiện tại khớp các số trên.
Sau khi sửa logging lỗi 500 để chỉ ghi loại exception và correlation id, learning-service cùng common-security đã chạy
lại: exit 0, vẫn 11 test pass và không skip.
Test context dùng Testcontainers PostgreSQL đã chạy, xác nhận Flyway version 1, chín bảng và health 200.
Test MVC xác nhận thiếu token trả 401; route được xác thực trong test trả 200. Test Gateway kiểm route discovery và
prefix internal JWT. Các test không chứng minh việc đăng ký Eureka trên môi trường local đang chạy.

## Reference Python còn lại theo phạm vi phase 5

Kết quả `git grep -n -i -E 'ai-learning|ai_learning|AI_LEARNING'` trên file được track được đối chiếu với các mục sau.
Các nội dung này được giữ theo phạm vi PR1; [phase 5](../phase-05-tai-lieu-va-plan-con.md) sẽ đồng bộ với Java.

| File | Các mục còn reference cũ |
| --- | --- |
| [AGENTS.md](../../../AGENTS.md) | §2 tech stack; §3.4 security; §3.5 giao tiếp service; §3.6 runtime; §3.8 AI Learning Python; §4 cấu trúc; §6 kiểm chứng; §7 điểm chưa nhất quán. |
| [CLAUDE.md](../../../CLAUDE.md) | §1 topology assessment → consumer; §3 môi trường/chạy local. |
| [README.md](../../../README.md) | Cấu trúc dự án; Getting Started §1 prerequisites và §6 Assessment → AI Learning, gồm cấu hình LLM, nguồn DeepTutor, tutor E2E và test Python. |
| [system-architecture.md](../../../docs/system-architecture.md) | §1 tổng quan; §2 service/dữ liệu; §3 HTTP/RabbitMQ; §5 AI Learning Python; §6 tạo path, kết quả → mastery, tutor; §7 local; §9 consumer pattern; §11 vấn đề tài liệu cũ. |

Các cập nhật tài liệu trong PR1 giới hạn ở hai contract, thông tin module Java và lệnh test hiện tại.
Không cần mở rộng sang nội dung đã giao cho phase 5. Baseline `.sdd/` và tài liệu lịch sử ngoài phạm vi tìm kiếm này.

## Điều kiện còn lại

- Người dùng tự cấu hình `LEARNING_DB_PASSWORD` và chạy lại Compose gate. Không đọc hoặc chỉnh `.env` trong công việc này.
- Các phase 2–4 bổ sung nghiệp vụ học và consumer; hiện assessment event chưa có consumer Learning Service.
- Dữ liệu runtime local `deeptutor-data` bị ignore được giữ nguyên và không đọc; nó không phải code service được Git quản lý.
- Tiếp theo thực hiện phase 2 theo acceptance criteria của plan.
