---
phase: 3
title: "AGENTS rules source"
status: pending
priority: P1
dependencies: [1, 2]
effort: "~0.5d"
---

# Phase 3: AGENTS rules source

## Overview
Biến `AGENTS.md` thành nguồn quy tắc duy nhất, đúng với HEAD và tự đứng được (Codex/tool khác chỉ đọc file này).
Sửa các khẳng định sai, thêm quy tắc cho AI Learning và messaging, cắt phần mô tả đã chuyển sang `docs/system-architecture.md`.

## Requirements
- Không còn câu gây hiểu sai: stack Java-only, "không có message broker", "OpenAPI/Swagger definition" (FastAPI tự sinh
  `/openapi.json`; chỉ đúng là "không có file OpenAPI được track").
- Quy tắc đủ cho cả service Java và Python.
- ~200–230 dòng; giữ cấu trúc mục §1–§7 để người quen file dễ tìm.

## Architecture
Thay đổi theo mục:

| Mục | Thay đổi |
| --- | --- |
| §1 Tổng quan | 2–3 câu: 9 service + infra + lib; con trỏ `docs/system-architecture.md` |
| §2 Tech stack | Thêm: Python 3.11, FastAPI, pydantic-settings, psycopg2, httpx, python-jose, pika (ai-learning); RabbitMQ 3.13 + Spring AMQP (assessment); Spring WebSocket (game); Flyway CLI container cho ai-learning. Sửa câu "không có…" thành danh sách còn thiếu đúng (frontend, cache, object storage, CI) |
| §3.1 | Từ "User Service" thành mọi service Java; ghi biến thể `application/port` (assessment, game) |
| §3.x mới | "AI Learning (Python)": layer `app/api → application → mastery/tutor/practice/usage → persistence/clients`; store mở connection mỗi lần gọi; DTO camelCase alias; lượt tutor luôn đóng |
| §3.4 Security | Thêm: bản Python của internal JWT phải khớp issuer/claim/role với `common-security` |
| §3.5 Microservice | Thêm: giao tiếp async qua outbox + RabbitMQ; event contract ở `docs/contracts/` |
| §5 Cấm | Thêm: import `deeptutor`; test gọi LLM thật; log nội dung hội thoại/prompt/key; sửa migration đã áp dụng; commit `.env`/bytecode |
| §6 Quy trình | Lệnh `mvn -pl <module> test` cho mọi module; lệnh test Python; graphify: `update . --force` khi node giảm có chủ đích |
| §7 Chưa nhất quán | Làm mới theo fact sheet; bỏ mục đã hết hiệu lực |
| Mô tả DDD/kiến trúc dài | Chuyển/cắt, thay bằng con trỏ `docs/system-architecture.md` |

## Related Code Files
- Modify: `AGENTS.md`
- Read: fact sheet, `docs/system-architecture.md`, `services/ai-learning-service/README.md`, `tests/test_no_deeptutor_dependency.py`

## Implementation Steps
1. Sửa §2 và câu "không có…" trước (lỗi nghiêm trọng nhất).
2. Tổng quát hóa §3.1, thêm §3.x AI Learning, bổ sung §3.4/§3.5.
3. Bổ sung §5, §6 (mọi lệnh lấy từ fact sheet, đã chạy thử).
4. Cắt phần mô tả đã có trong `docs/system-architecture.md`, để lại con trỏ.
5. Làm mới §7; ghi đầu file "Cập nhật lần cuối: <ngày>, commit <sha>".
6. Tự kiểm: `grep -ci` Python/RabbitMQ/ai-learning > 0; không còn "không có message broker"; 0 secret; ≤ ~230 dòng.
7. Commit: `docs: make AGENTS.md the single source of agent rules for all services`.

## Success Criteria
- [ ] Không còn khẳng định Java-only / không broker / không OpenAPI sai.
- [ ] Có quy tắc AI Learning và các mục cấm mới; lệnh khớp fact sheet.
- [ ] ~200–230 dòng; đọc độc lập vẫn đủ quy tắc (không phụ thuộc CLAUDE.md).

## Risk Assessment
- Cắt quá tay làm Codex thiếu ngữ cảnh: giữ đủ quy tắc + con trỏ; chỉ cắt phần *mô tả* đã chuyển sang doc.
