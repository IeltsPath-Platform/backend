---
phase: 1
title: Fact sheet and graphify scope
status: completed
priority: P1
dependencies: []
effort: ~0.5d
---

# Phase 1: Fact sheet and graphify scope

## Overview
Lập một fact sheet đã kiểm chứng tại HEAD làm nguồn duy nhất cho các phase sau, và loại `third_party/` khỏi graphify để
graph phản ánh code first-party.

## Requirements
- Fact sheet chỉ chứa dữ kiện đọc từ file nguồn hoặc kiểm bằng lệnh, mỗi mục ghi file nguồn.
- Không ghi giá trị secret; biến có default nhạy cảm chỉ ghi "có default trong <file>".
- Graph sau rebuild không còn node từ `third_party/`.

## Architecture
Output: `plans/260927-2201-agent-context-docs-restructure/reports/fact-sheet-<sha HEAD>.md`, các bảng:

| Bảng | Nguồn |
| --- | --- |
| Module (tên, loại, ngôn ngữ, có trong Maven reactor?) | `pom.xml` (modules), cây `infra/ services/ shared/` |
| Cổng service | `SERVER_PORT` default trong `*/src/main/resources/application.yml`, `config-repo/*.yaml`; ai-learning 8000 (`main.py`, compose) |
| DB mỗi service (tên, host:port mặc định, local hay compose) | `config-repo/<svc>.yaml` (`*_DB_URL`), `docker-compose.yml` |
| Route Gateway | `config-repo/api-gateway.yaml` (id, uri, Path) |
| Giao tiếp HTTP nội bộ | `*/infrastructure/client/*.java`, `services/ai-learning-service/app/clients/*.py`, `Internal*Controller` |
| Messaging | `RabbitOutboxEventPublisher`, `OutboxRelay*`, `app/messaging/*`, bảng `outbox_events` của từng service (relay hay không) |
| Biến env (tên, bắt buộc/có default, dùng ở đâu) | compose, `config-repo`, `app/config.py` |
| Thứ tự khởi động và phần nào chạy host/compose | `docker-compose.yml`, `README.md` §4–6 |
| Lệnh build/test đã chạy thử | kết quả thực tế (ghi exit code, thời gian) |
| Bất biến có test/tooling cưỡng chế | `tests/test_no_deeptutor_dependency.py`, plan rules, `.gitignore` |
| Điểm chưa nhất quán còn hiệu lực | kiểm lại từng mục §13 CLAUDE.md cũ + phát hiện trong phiên 2026-09-27 |

## Related Code Files
- Create: `plans/260927-2201-agent-context-docs-restructure/reports/fact-sheet-<sha>.md`
- Modify: `.graphifyignore`
- Regenerate (gitignored): `graphify-out/`

## Implementation Steps
1. Ghi `git rev-parse --short HEAD` vào đầu fact sheet.
2. Điền từng bảng theo cột "Nguồn". Dùng `graphify explain "<Class>"` để định vị client/publisher, rồi đọc file.
3. Kiểm lệnh (ghi kết quả):
   - `mvn -q -pl shared/common-security test` và một business service nhỏ (ví dụ `mvn -q -pl services/community-service test`).
   - `mvn -q clean compile -DskipTests` (toàn reactor).
   - `docker compose config --quiet`.
   - Python: venv 3.11, `python -m pip install pytest -r requirements-test.txt`, `python -m pytest tests -q` với
     `AI_LEARNING_TEST_DATABASE_URL` (+ `AI_LEARNING_TEST_AMQP_URL` khi RabbitMQ chạy), `PYTHONDONTWRITEBYTECODE=1`.
4. Kiểm lại từng "điểm chưa nhất quán" §13 cũ (còn/không còn) và thêm mục mới: `CONTENT_SERVICE_URL` game mặc định
   `content-service:8082`; outbox access/content/game không relay; notification-service skeleton; secret fallback trong
   `api-gateway.yaml`; Postgres local cần `access_db`, `notification_db`.
5. `.graphifyignore`: thêm dòng `third_party/` (kèm comment lý do). Chạy `graphify update . --force`.
6. Kiểm graph: script đếm node có `source_file` bắt đầu `third_party/` = 0; `graphify explain "run_turn"` và
   `graphify query "how does assessment notify ai learning"` trả node first-party.
7. Commit: `chore(graphify): keep vendored reference code out of the code graph` (+ fact sheet).

## Success Criteria
- [x] Fact sheet đủ các bảng, mỗi dòng có nguồn; không có giá trị secret.
- [x] Mọi lệnh ở bước 3 có kết quả ghi lại (pass hoặc điều kiện cần).
- [x] 0 node `third_party/` trong graph; query mẫu ra node first-party.

## Kết quả (2026-09-27)
- Fact sheet: [reports/fact-sheet-79f9fd6.md](./reports/fact-sheet-79f9fd6.md).
- Lệnh Maven dùng `mvn -q compile -DskipTests` thay `clean compile` (không xóa `target/` của service đang chạy trong IDE);
  test community-service cần `-am` và Docker (Testcontainers).
- Graph: 46.578 → 7.429 node, 0 node `third_party/`; `graphify query` về assessment → ai-learning ra node first-party.
  `graphify explain "run_turn"` báo trùng tên giữa `app/api/tutor.py` và test → dùng dạng `path::symbol`.
- Mục §13 cũ "graph tham chiếu SecurityConfig đã xóa" hết hiệu lực (0 node trỏ file không tồn tại).

## Risk Assessment
- `mvn` toàn reactor chậm hoặc cần Docker (Testcontainers): chỉ compile toàn reactor, test module nhỏ; ghi điều kiện.
- `graphify update --force` lỗi: chạy `graphify .` (full build) như AGENTS.md §6 cho phép khi graph lỗi.
