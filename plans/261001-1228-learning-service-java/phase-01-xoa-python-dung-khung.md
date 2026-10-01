---
phase: 1
title: "Xóa Python, dựng khung learning-service"
status: completed
priority: P1
dependencies: []
effort: "1 ngày"
---

# Phase 1: Xóa Python, dựng khung learning-service

## Overview

Một PR: gỡ toàn bộ ai-learning Python và dựng service Java rỗng nhưng chạy được (khởi động, Flyway, security, health),
có route Gateway và DB compose. Chưa có nghiệp vụ.

## Kết quả ngày 2026-10-01

Hoàn tất phạm vi code trên nhánh `feat/learning-service-skeleton`: xóa 135 file được Git quản lý của service Python và
ba tài liệu cũ; thêm khung Java, Flyway chín bảng, cấu hình và route `/api/learning/**`; đồng bộ hai contract.
Chưa triển khai API bài học/mastery hoặc consumer.

Maven learning-service cùng common-security: 11 test pass; Gateway: 8 test pass; không failure, error hay skip.
Compile cả reactor pass. `docker compose config --quiet` chưa pass vì thiếu `LEARNING_DB_PASSWORD`; người dùng tự thêm
biến vào `.env`. Đây là điều kiện cấu hình local còn lại, không ghi giá trị thay người dùng.

[`Báo cáo kiểm chứng`](./reports/learning-service-skeleton-verification.md) ghi các gate và reference Python còn chờ
phase 5. Review đã hoàn tất, không còn finding cần xử lý; test learning-service cùng common-security đã chạy lại và pass.

## Requirements

**Xóa:**
- `services/ai-learning-service/` (toàn bộ).
- `docker-compose.yml`: `ai-learning-db`, `ai-learning-migrate`, `ai-learning-api`, `ai-learning-consumer`, `llm-stub`, volume
  `ai-learning-db-data`.
- `infra/config-server/config-repo/api-gateway.yaml`: route `ai-learning-service` và `/api/ai-learning` trong
  `internal-jwt-paths`.
- `.gitignore`, `.dockerignore`, `services/README.md`: dòng riêng của ai-learning.
- `docs/contracts/tutor-sse-v1.md`, `docs/contracts/practice-v1.md`, `docs/ai-learning-database.md`.

**Tạo `services/learning-service`** theo template AGENTS §3.1.1 (copy cấu trúc của `library-service`, service mới nhất):
- `pom.xml` (thêm vào reactor gốc), `application.yml` (`SERVER_PORT:8086`, tên `learning-service`, import `.env` như service khác).
- `config-repo/learning-service.yaml`: datasource `jdbc:postgresql://localhost:5436/learning_db`, user `postgres`, mật khẩu
  `${LEARNING_DB_PASSWORD}` (không fallback), `ddl-auto: validate`, `CONTENT_SERVICE_URL` mặc định `http://localhost:8082`,
  RabbitMQ như assessment, `learning.review-mastery-threshold: 0.6`.
- `LearningServiceApplication`, `GlobalExceptionHandler` + `ErrorResponse` (mở rộng `{detail, code, ...}` cho lỗi cổng ở phase 2).
- Flyway `V1__learning_schema.sql`: toàn bộ bảng ở mục "Mô hình dữ liệu" của `plan.md` (tạo một lần cho đỡ đổi schema
  giữa các PR); cột và index của `topic_progress`…`topic_test_assignments` theo V11 trong `260929-1640/phase-06`.
- Test: context load (Testcontainers), `@WebMvcTest` một route giả để chứng minh thiếu token → 401.

**Gateway:** route `learning-service` → `lb://learning-service`, `Path=/api/learning/**`; thêm `/api/learning` vào
`internal-jwt-paths`. Test Gateway hiện có pass.

**Compose:** service `learning-db` (`postgres:15-alpine`, host `127.0.0.1:5436`, DB `learning_db`,
`LEARNING_DB_PASSWORD:?Set LEARNING_DB_PASSWORD`), volume `learning-db-data`. Không có container cho learning-service
(chạy trên host như service Java khác).

**Contract:**
- `lesson-learning-v1.md`: prefix `/api/ai-learning` → `/api/learning`; thêm `GET /mastery` (phase 2 định nghĩa shape).
- `assessment-completed-v2.md`: consumer là Learning Service; queue `learning.assessment-completed.v2`, retry
  `learning.assessment-completed.retry` / `.v2.retry`, DLQ `learning.assessment-completed.dlx` / `.v2.dlq`, header
  `x-learning-failure`, env `LEARNING_RETRY_DELAY_MS`, `LEARNING_MAX_DELIVERY_ATTEMPTS`; bỏ mục "Results that arrive before
  the learning path" và mọi chữ DeepTutor/path; idempotency theo `(user_id, attempt_id, result_version)`.

## Tests

- `mvn -q -pl services/learning-service -am test`, `mvn -q -pl infra/api-gateway test`, `mvn -q compile -DskipTests`,
  `docker compose config --quiet` (cần `LEARNING_DB_PASSWORD` trong `.env`; thiếu thì báo, không tự ghi giá trị).
- `grep -ri "ai-learning\|ai_learning\|AI_LEARNING"` ngoài `plans/`, `docs/journals/`, `.sdd/`: chỉ còn trong tài liệu
  sẽ sửa ở phase 5 (liệt kê ra trong báo cáo).

## Rủi ro

- Event assessment không còn queue nhận tới hết phase 4 (chấp nhận).
- `AGENTS.md`/`CLAUDE.md` còn nói về Python tới phase 5: phase này chỉ sửa bảng module và lệnh test bị gãy, phần còn lại ở phase 5.
