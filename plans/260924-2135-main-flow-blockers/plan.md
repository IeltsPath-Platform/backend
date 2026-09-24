---
title: "Gỡ blocker luồng chính Assessment → DeepTutor (không LLM)"
description: "Gỡ các blocker để luồng Assessment finalize → RabbitMQ → DeepTutor → /status chạy thật: baseline schema ai_learning_db + Flyway, hộp chờ khi chưa có path, endpoint grader, compose tối thiểu và kiểm chứng E2E."
status: completed
priority: P1
branch: "main"
tags: [feature, backend, database, api, auth, infra]
blockedBy: []
blocks: []
created: "2026-09-24T14:53:44.813Z"
createdBy: "ck:plan"
source: skill
---

# Gỡ blocker luồng chính Assessment → DeepTutor (không LLM)

## Overview

Code của luồng chính đã có, và test Python E2E trên PostgreSQL đã pass. Luồng vẫn chưa chạy được thật vì bốn lý do:

- `ai_learning_db` không có DDL gốc và không có runner migration.
- Nếu event tới khi learner chưa có path, event bị retry rồi rơi vào DLQ.
- Không có gì gọi `FinalizeAssessmentResultUseCase`.
- Compose thiếu các service và database cần thiết. Cộng thêm 8 test cần Docker chưa được chạy.

Plan này **không** làm cấu hình LLM, vì luồng chính không gọi model nào (đã kiểm chứng).

- Nguồn quyết định: `plans/reports/brainstorm-260924-2135-main-flow-blockers-without-llm-report.md`
- Contract event: `docs/contracts/assessment-completed-v2.md`
- Chế độ: `--fast --tdd`, phạm vi HOLD. Mỗi pha có Tests Before, Refactor, Tests After và Regression Gate.
- Đường dẫn trong các pha tính từ root repo `C:\Users\Admin\OneDrive\Desktop\capstone-fall26\backend`.

Quyết định đã chốt:
- Baseline V5 `V0_1__` chạy qua Flyway. Đã xác minh Flyway 11 áp theo thứ tự `[0.1, 1, 2]`.
- Hộp chờ dùng advisory lock theo `(user, goal)`. Kết quả chờ được áp dụng trong transaction tạo path.
- Endpoint `/api/assessments/grading/**` dùng `hasAnyRole('EXAMINER','ADMIN')`. Gateway đã route `/api/assessments/**`.
- EXAMINER không bị giới hạn theo phân công trong đợt này.
- Giữ endpoint learner tạo result. Band của result luôn do grader ghi đè khi lưu chi tiết.
- Kết quả trong hộp chờ được giữ đến khi path được tạo; chưa dọn.
- Compose tối thiểu, chỉ thêm phần AI Learning (không có `assessment-db`). Service Java chạy local/IDE; `user_db`, `content_db`, `assessment_db` nằm trên PostgreSQL local.

Ngoài phạm vi:
- Cấu hình LLM và dọn giá trị mặc định của `GATEWAY_INTERNAL_JWT_SECRET`.
- Tự chấm câu khách quan, phân công EXAMINER (`human_reviews`), dọn hộp chờ, compose full stack.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [AI Learning baseline schema and Flyway](./phase-01-ai-learning-baseline-schema-and-flyway.md) | Completed |
| 2 | [Pending formal result inbox](./phase-02-pending-formal-result-inbox.md) | Completed |
| 3 | [Assessment grader endpoints](./phase-03-assessment-grader-endpoints.md) | Completed |
| 4 | [Minimal compose and end-to-end verification](./phase-04-minimal-compose-and-end-to-end-verification.md) | Completed |

## Dependencies

- Không phụ thuộc plan khác. Plan `260924-chinh-sua-kien-truc-service` (mục 02–05) đã completed.
- Phụ thuộc giữa các pha trong plan:
  - Pha 2 cần pha 1, vì dùng chuỗi migration và schema test dùng chung.
  - Pha 3 độc lập, có thể làm song song với pha 1–2.
  - Pha 4 cần cả 1, 2 và 3.
- Môi trường cho pha 4:
  - Docker Desktop (CLI 29.1.3 đã cài, daemon chưa bật).
  - PostgreSQL local (đã có, bản 17).
  - RabbitMQ chạy bằng compose.
- Sau mỗi pha có sửa code: chạy `graphify update .` (theo `CLAUDE.md`).
- Gate chung sau mỗi pha:
  ```powershell
  mvn -o -pl services/assessment-service,services/content-service -am test
  ```
  ```bash
  # từ services/ai-learning-service
  PYTHONPATH="../../third_party/deeptutor;." AI_LEARNING_TEST_DATABASE_URL=<disposable db> python -m pytest tests
  ```
