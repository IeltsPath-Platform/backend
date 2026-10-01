# Kiểm chứng nền luồng học Java — 2026-10-02

Status: PASS

Nhánh: `feat/lesson-pipeline-docs`, source runtime từ `feat/main-follow` commit `896fa2c`.
Thay đổi của công việc này chỉ là tài liệu; không sửa code, config, contract hay migration.

## Môi trường và phạm vi

- Java 21, boot jar build từ source hiện tại; Eureka, Gateway, Content, Assessment và Learning chạy thật trên host.
- PostgreSQL 15 và RabbitMQ 3.13 chạy trong container tạm riêng, ba database mới: `content_db`, `assessment_db`, `learning_db`. Không dùng database của lập trình viên.
- Cấu hình từ YAML hiện có trong `infra/config-server/config-repo/`, với port và credential tạm qua environment/command line; không chạy process Config Server, không đọc `.env`.
- JWT external hợp lệ của fixture CUSTOMER dùng UUID mới; Gateway xác thực và ký internal JWT, service dùng security thật. Không kiểm luồng đăng ký/đăng nhập User ở lần chạy này.
- Credential và khóa ký tạo ngẫu nhiên trong bộ nhớ; không ghi vào report. Script điều phối và helper replay chỉ nằm trong `target/` bị ignore, không thuộc commit. Không log câu trả lời hay event body.
- Kịch bản Lan Reading với ngưỡng review `0.6` từ [seed-content.md](../../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md#kịch-bản-kiểm-thử). Bỏ qua khối `ESSAY` theo contract vì essay không chặn hoàn thành bài; không gọi LLM.

## Kết quả live E2E

| # | Kiểm tra | Kết quả quan sát |
| --- | --- | --- |
| 1 | CUSTOMER qua Gateway → Learning → Content | `GET /api/learning/topics` refresh sequence; `DEMO_READING` là `IN_PROGRESS`. |
| 2 | Ranh giới đọc nội dung | Gateway chặn `/internal/**` bằng 403; CUSTOMER đọc question bank Content bị 403. Nội dung bài và cấu trúc attempt không có answer spec, explanation, solution hoặc transcript trước khi đạt. |
| 3 | Bốn bài Reading và bài ôn KP1 | Nộp sai đầu tiên rồi nộp lại đúng ở L1/L2; replay cùng request trả response cũ; request mới không tăng lesson evidence. L3 bị `403 REVIEW_REQUIRED`; review KP1 đúng 4/4 → `DONE`, mở L3. Tổng 7 evidence `lesson_exercise` và 4 `review_set`. |
| 4 | Giao X1 → tạo attempt → answers → submit | Assignment POST lặp trả cùng assignment; attempt lấy structure allowlist; X1 đạt 75%. Submit lặp không tạo thêm result/outbox. Learner result có 4 item solutions, `sectionSolutions: []` cho đề Reading này. |
| 5 | Outbox → RabbitMQ → consumer | Assignment được consume bằng attempt vừa nộp; `DEMO_READING` → `PASSED`, `TFNG_SKILLS` → `IN_PROGRESS`; thêm 4 assessment evidence và 1 pending review KP2. Mastery KP2 làm tròn ba chữ số = **0.487**, giữ nguyên số kỳ vọng seed. |
| 6 | Event và exposure | Một `AssessmentCompleted.v2` đã publish; `learning_goal_id = null`, package version đúng assignment, 4 item results. Event không chứa answer spec, solution, correct answer hay transcript. |
| 7 | DLQ replay bằng body/property gốc | Đưa một bản sao hợp lệ của event vừa publish vào DLQ. Helper đọc manual-ACK, kiểm SHA-256 body và message id, publish lại nguyên body/properties với `mandatory` và publisher confirm, sau đó mới ACK DLQ. Consumer nhận duplicate; evidence và result version không đổi; main/retry/DLQ đều rỗng. |

**7/7 kiểm tra pass.** Process và container tạm đã được dừng/xóa sau khi kiểm xong.
Flyway thực tế trên database tạm: Content V1–V13, Assessment V1–V4, Learning V1–V2.

DLQ được dựng bằng một duplicate hợp lệ để kiểm runbook replay và idempotency; đây không phải bằng chứng đã sửa được một event sai contract. Coverage automated ack/nack vẫn dùng channel giả; lần này bổ sung kiểm chứng live broker, không thêm automated RabbitMQ test vào source.

## Regression và kiểm tài liệu

| Lệnh | Kết quả thực tế |
| --- | --- |
| `mvn -q compile -DskipTests` | Exit 0, toàn reactor compile. |
| `mvn -q test` | **693 test, 0 failure, 0 error, 0 skip**. Docker/Testcontainers khả dụng và các test integration đã chạy. |
| `mvn -q -pl infra/eureka-server,infra/api-gateway,services/content-service,services/assessment-service,services/learning-service -am package -DskipTests` | Exit 0, tạo boot jar cho live E2E. |
| `docker compose config --quiet` | Exit 0. |
| `graphify update .` | Exit 0; không có thay đổi topology code graph, output ignored. |
| `node C:\Users\pduy8\.claude\scripts\validate-docs.cjs docs/` | Exit 0; scanner kiểm 1 file, 6 internal links hợp lệ, **44 warning** (15 code references, 29 config keys). Các tên Java nằm trong Maven submodule/config-repo; scanner không nhận đầy đủ layout này và coi cả enum như config key. Đây không phải một lần kiểm sạch warning. |
| Kiểm path Markdown, UTF-8/BOM và `git diff --check` | Các file đổi không có link tới file bị thiếu, BOM hoặc whitespace error. |

Số test theo module: common-security 8; Gateway 8; Config Server 1; Eureka 1; User 106; Content 138; Assessment 117; Access 45; Learning 175; Library 53; Game 15; Community 25; Notification 1.

## Điều chỉnh so với phase cũ và giới hạn

- Ánh xạ Python → Java theo [python-to-java-mapping.md](../../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); không chạy pytest của service đã xóa. Full reactor test bao gồm các gate Content/Assessment/Gateway/Learning.
- Không tái tạo `docs/ai-learning-database.md`; schema hiện tại ở [DATABASE_V5 §7](../../../.sdd/database/DATABASE_V5.md#7-learning-service--tiến-độ-học-bằng-chứng-mastery-bài-ôn-mã-đề) và [Learning README](../../../services/learning-service/README.md).
- Các lần chạy chuẩn bị được sửa ở script kiểm chứng: chờ discovery đủ service, dùng UUID thực tế V9 thay UUID minh họa contract, gate 403 theo contract, bỏ khối ESSAY khỏi luồng tự chấm khách quan. Không sửa production để làm test pass; không đổi số mastery.
- Đây là nền topic → bài → review → đề cuối → event → topic kế. E2E toàn MVP của roadmap 2057 (đăng nhập, Writing/point/quota, Listening/audio và các case khác) còn là công việc riêng; learner Reading hints còn pending. Không tuyên bố MVP đầy đủ đã hoàn tất.
- Review source phát hiện mô tả khóa mọi lượt ghi quá rộng: một số cập nhật Writing trung gian/quota hiện dùng SQL nguyên tử ngoài transaction advisory lock. Đã ghi đúng dữ kiện vào database docs và kiến trúc §11; không đổi quy tắc AGENTS.md và không sửa code Writing trong công việc tài liệu này.
