# Code review — tái cấu trúc tài liệu ngữ cảnh agent

- Reviewer: code-reviewer subagent, 2026-09-27. Phạm vi: `CLAUDE.md` (95 dòng), `services/ai-learning-service/CLAUDE.md` (67),
  `AGENTS.md` (277), `docs/system-architecture.md` (231); so với `9a1f5a8:AGENTS.md`, `9a1f5a8:CLAUDE.md`, plan, fact sheet.
- Nội dung phase 4 được commit (`253775d`) trong lúc review; reviewer xác nhận bằng md5 là giống bản đã review.
- Kết luận: không High, không secret; 25/28 dữ kiện kiểm với code đúng. Status: DONE_WITH_CONCERNS.

## Medium

| # | Vấn đề | Bằng chứng | Hướng sửa |
| --- | --- | --- | --- |
| M1 | `.sdd/` có rulebook tự nhận thẩm quyền mà tài liệu mới không nhắc; `.sdd/global/system-architecture.md` (ACTIVE) và `.sdd/constraints/global.md` ghi không broker/outbox; constitution LOCKED đứng đầu thứ tự ưu tiên | `.sdd/global/system-architecture.md:46,130,204,242`; `.sdd/constraints/global.md:43,71` | AGENTS §1 + docs §11 ghi rõ baseline lỗi thời và thứ tự ưu tiên (cần người dùng quyết) |
| M2 | Lệnh pytest trong tài liệu tạo `.pytest_cache/` không bị ignore ở root: `pyproject.toml` root (`cf62f7e`, Python ≥3.14) thành configfile/rootdir | `pytest --co` in `rootdir: backend`, `configfile: pyproject.toml` | Thêm `-p no:cacheprovider`; ghi pyproject root vào §7/§11 |
| M3 | community DB ghi "compose 5434" nhưng default code là local 5432; override `COMMUNITY_DB_URL` không được ghi | `config-repo/community-service.yaml:3`; compose `community-db` 5434 | Thêm cạm bẫy |
| M4 | AGENTS nói ai-learning forward `X-Correlation-Id` — sai | `app/clients/*.py` chỉ gửi `Authorization` | Bỏ ai-learning khỏi ví dụ; ghi vào §7/§11 |
| M5 | AGENTS §6 làm rơi quy tắc graphify: `affected`, full build chỉ khi cần, không tự cài/commit `.graphify-tools/`, GRAPH_REPORT chỉ khi cần | old `AGENTS.md` 253, 256, 264 | Khôi phục 2–3 dòng |
| M6 | Cấm "tham chiếu `deeptutor`" mâu thuẫn header ghi nguồn Apache-2.0 bắt buộc; test chỉ kiểm import + Dockerfile/entrypoint/requirements/compose/README | `app/mastery/grading.py:1`; `tests/test_no_deeptutor_dependency.py` | Viết lại: cấm import và đưa vào build/runtime; comment ghi nguồn được phép |
| M7 | CLAUDE.md trỏ README §4–6 là hướng dẫn đầy đủ; §4 mâu thuẫn (full `compose up`, liệt kê config/eureka/gateway, community 5433) | `README.md:208-219` | Chỉ trỏ §6; thêm dòng §11 |

## Low

- L1 `.env` không được import bởi config-server/eureka-server (chỉ Gateway + service Java nghiệp vụ).
- L2 `llm-stub` nằm sau profile `llm-stub` trong compose.
- L3 docs §11 thiếu: CORS không expose `Retry-After`, `sqlalchemy` không dùng, `outbox_events` không dùng ở community/learning-support.
- L4 Hướng dẫn venv thiếu: cách tạo (`uv venv --seed --python 3.11 <dir>`), có `services/ai-learning-service/.venv`, root `.venv`/pyproject.
- L5 Dữ kiện dễ lỗi thời: "432 passed" (không DB: 323 passed/109 skipped), "hiện tới V9", "nhánh phát triển nhiều nhất".
- L6 AGENTS nói Gateway route "qua Eureka", thiếu ngoại lệ ai-learning (URI cố định `AI_LEARNING_SERVICE_URI`).
- L7 AGENTS nói mọi tác vụ LLM cho học viên qua hạn mức; path ordering không bị giới hạn (quyết định Q7 plan `260927-1453`).
- L8 `com/group01/<name>` — learning-support dùng package `learningsupport`; user-service có package `config` ở root.
- L9 Mất lệnh `mvn -pl infra/api-gateway test`.
- L10 CLAUDE.md của service không nói route path/status/progress/health nằm ở `main.py`.
- L11 Snippet test chỉ PowerShell; Git Bash cần cú pháp khác.
- L12 Hash commit ở header mơ hồ; dùng hash fact sheet `79f9fd6` và định nghĩa rõ.
- L13 Chưa nói DB test nên là DB dùng một lần.

## Verdict

(a) Accuracy PASS WITH CONCERNS (M3, M4, L1). (b) Nothing lost PARTIAL (M5, L9, L3, M3). (c) Consistency PASS WITH CONCERNS
(L6, L3, L2). (d) Security PASS. (e) Instruction quality CONCERNS (M1, M2, M6, M7, L7, L11). (f) Acceptance PARTIAL (phase 4
checkbox chưa đánh, smoke test chưa ghi, `uvicorn` chưa kiểm).

## Câu hỏi cho người dùng

1. Constitution `.sdd/global/constitution.md` (LOCKED, ưu tiên cao nhất) và AGENTS.md "nguồn quy tắc duy nhất": cái nào thắng?
2. `pyproject.toml` / `uv.lock` ở root (Python ≥3.14) cố ý hay thừa?
3. LLM sắp thứ tự path có tính vào hạn mức ngày không? → Đã có quyết định: không (Q7 của plan `260927-1453`).
