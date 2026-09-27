---
phase: 4
title: Slim CLAUDE files
status: completed
priority: P1
dependencies:
  - 1
  - 3
effort: ~0.5d
---

# Phase 4: Slim CLAUDE files

## Overview
Viết lại `CLAUDE.md` root thành bản gọn (≤ 150 dòng) nạp `AGENTS.md`, tạo `services/ai-learning-service/CLAUDE.md`
(≤ 100 dòng), rồi kiểm chứng toàn bộ bộ tài liệu bằng lệnh thật và một smoke test.

## Requirements
- Root CLAUDE.md chứa thứ dùng hằng ngày; không lặp quy tắc của AGENTS.md (import bằng `@AGENTS.md`).
- Giữ tiêu đề `## graphify` (tooling graphify tìm mục này).
- Nested CLAUDE.md chỉ chứa phần riêng của ai-learning-service.

## Architecture
**`CLAUDE.md` root** (thứ tự mục):
1. Đầu file: tên dự án, "Cập nhật lần cuối: <ngày>, commit <sha>", dòng `@AGENTS.md`.
2. Hệ thống trong 5 dòng + con trỏ `docs/system-architecture.md`.
3. Bảng module (1 dòng/module): ngôn ngữ, cổng, DB (local 5432 / compose + cổng), route Gateway; `third_party/deeptutor` = tham khảo.
4. Chạy local: phần nào trên host, phần nào compose (`docker compose up -d rabbitmq ai-learning-db …`); Postgres 5432
   cần `user_db`, `content_db`, `assessment_db`, `access_db`, `notification_db` (lệnh `CREATE DATABASE`, không mật khẩu);
   `.env` root được compose và Spring (`optional:file:../../.env[.properties]`) nạp; thứ tự khởi động.
5. Lệnh hay dùng (Java, compose); Python → `services/ai-learning-service/CLAUDE.md`.
6. Cạm bẫy đã biết (5–8 dòng, từ fact sheet).
7. Tài liệu tra cứu: `docs/system-architecture.md`, `docs/contracts/`, `.sdd/database/DATABASE_V5.md`, README service, `plans/`.
8. `## graphify`: dùng `explain`/`path`/`query` (graph đã loại `third_party/`), `graphify update .` sau khi sửa code.

**`services/ai-learning-service/CLAUDE.md`**:
1. Stack và cách chạy (uvicorn, compose services, cổng 8000/5436).
2. Package map `app/` 1 dòng/package; `mastery/` là bản port DeepTutor v1.6.9 (giữ hành vi, test giữ giá trị gốc).
3. Bất biến riêng: lượt tutor luôn đóng; hạn mức ngày (`app/usage`); không import `deeptutor`; không log nội dung; test
   không gọi LLM thật.
4. Test: venv 3.11, cài, biến `AI_LEARNING_TEST_DATABASE_URL`/`AI_LEARNING_TEST_AMQP_URL`, `PYTHONDONTWRITEBYTECODE=1`,
   lệnh `python -m pytest tests`.
5. Migration: thêm `migrations/V<n>__*.sql`, test dựng schema từ cùng chuỗi; bảng README.
6. Contract: `docs/contracts/tutor-sse-v1.md`, `practice-v1.md`, `assessment-completed-v2.md`.

## Related Code Files
- Modify: `CLAUDE.md`
- Create: `services/ai-learning-service/CLAUDE.md`
- Keep: `.claude/CLAUDE.md` (3 dòng, skill graphify)

## Implementation Steps
1. Viết root CLAUDE.md theo Architecture; mọi dữ kiện từ fact sheet.
2. Viết nested CLAUDE.md.
3. Kiểm chứng:
   - Đếm dòng: root ≤ 150, nested ≤ 100.
   - Secret scan trên 4 file (root/nested CLAUDE.md, AGENTS.md, `docs/system-architecture.md`): chuỗi base64 ≥ 40 ký tự,
     `password`/`secret`/`key` có giá trị, URL có `user:pass@`.
   - Chạy lại từng lệnh ghi trong 2 file CLAUDE.md (hoặc ghi rõ điều kiện Docker).
   - Đối chiếu bảng module/cổng/route với fact sheet.
4. Smoke test: spawn một Explore agent **chỉ** đọc `CLAUDE.md` (kèm `@AGENTS.md`) và, khi cần, nested CLAUDE.md; hỏi 6 câu:
   (a) chạy test Python thế nào; (b) content-service cổng nào, DB nào; (c) assessment báo cho ai-learning bằng cách nào;
   (d) contract SSE tutor ở đâu; (e) quy tắc nào cấm khi viết test ai-learning; (f) service nào chạy trong compose.
   So với fact sheet; sai câu nào → sửa tài liệu → hỏi lại.
5. Cập nhật mục "Tài liệu và file quan trọng" nếu README root còn trỏ tới mục cũ của CLAUDE.md.
6. Commit: `docs: slim CLAUDE.md and add ai-learning-service agent context`.

## Success Criteria
- [x] Root ≤ 150 dòng có `@AGENTS.md` và `## graphify`; nested ≤ 100 dòng.
- [x] Secret scan sạch; lệnh chạy được; dữ kiện khớp fact sheet.
- [x] Smoke test 6/6 câu đúng.

## Kết quả (2026-09-27)
- Root `CLAUDE.md` 104 dòng (1 import `@AGENTS.md`, có `## graphify`); `services/ai-learning-service/CLAUDE.md` 77 dòng;
  `AGENTS.md` 299 dòng (bằng bản gốc, do khôi phục quy tắc graphify + thêm hướng dẫn Git Bash/venv).
- Commit nội dung đầu `253775d` (theo yêu cầu commit + push của người dùng, trước khi có kết quả review).
- Smoke test lần 1 (chỉ đọc CLAUDE.md + import): 5/6 — thiếu exchange/routing key; nêu 7 điểm khó hiểu.
- Code review: [report](./reports/code-reviewer-260927-2201-agent-context-docs-review-report.md) — 0 High, 7 Medium,
  13 Low, 0 secret, 25/28 dữ kiện đúng. Đã sửa: M1 (thứ tự ưu tiên, `.sdd` baseline cũ — người dùng chọn constitution
  đứng trên, không sửa `.sdd`), M2 (`-p no:cacheprovider`; người dùng chọn xóa `pyproject.toml`/`uv.lock` root →
  commit `1703853`, thêm `.pytest_cache/` vào `.gitignore`), M3–M7, L1–L13.
- Smoke test lần 2: 6/6 đúng (a–f) + (g) thứ tự ưu tiên đúng; 8/9 điểm nhỏ còn lại đã sửa. Giữ lặp công thức test giữa
  `AGENTS.md` và CLAUDE.md của service (AGENTS phải tự đứng được cho Codex).
- Kiểm lại: link tương đối OK, `docker compose config --quiet` OK, `llm-stub` qua profile OK, pytest thu 432 test không
  sinh file trong repo sau khi xóa pyproject root.

## Risk Assessment
- `@AGENTS.md` không được nạp (sai đường dẫn): kiểm bằng smoke test câu (e) (quy tắc chỉ có trong AGENTS.md).
- Nested CLAUDE.md chỉ nạp khi Claude đọc file trong service: root CLAUDE.md phải có con trỏ tới nó.
