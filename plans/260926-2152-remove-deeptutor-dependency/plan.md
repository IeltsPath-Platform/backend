---
title: "Gỡ phụ thuộc DeepTutor khỏi ai-learning-service"
description: "Service không import, không build, không test dựa vào DeepTutor. Port đúng hành vi lõi mastery của DeepTutor v1.6.9 vào app/mastery, tự viết LLM client cấu hình bằng biến môi trường, gỡ third_party khỏi build. third_party/deeptutor chỉ để đọc tham khảo."
status: completed
priority: P1
branch: "feat/ai-learning-service"
tags: [refactor, backend, ai]
created: "2026-09-26T21:52:00.000Z"
completed: "2026-09-26"
source: conversation
---

# Gỡ phụ thuộc DeepTutor khỏi ai-learning-service

## Overview

Yêu cầu (người dùng, 2026-09-26): **code của `ai-learning-service` không import và không phụ thuộc code trong
`third_party/deeptutor`**; dựa trên ý tưởng của DeepTutor nhưng tự viết, tự cấu hình. Trước refactor:
- 14 trong 32 file của `app/` import `deeptutor.*`; 11 file trong `tests/` cũng vậy.
- `Dockerfile` cài `pip install ./third_party/deeptutor`; test chạy với `PYTHONPATH=../../third_party/deeptutor`.
- Cấu hình LLM nằm trong `model_catalog.json` của DeepTutor; compose mount `deeptutor-data`.

Plan này:
1. **Port** phần lõi mastery mà app dùng vào `app/mastery/`, giữ **đúng hành vi** của DeepTutor v1.6.9.
2. **Tự viết** LLM client (API chuẩn OpenAI), cấu hình bằng `AI_LEARNING_LLM_*`.
3. Gỡ DeepTutor khỏi build, test, compose; thêm test chặn để không ai import lại.

Tutor study/review (tính năng mới) nằm ở plan riêng [260926-2249-tutor-study-review](../260926-2249-tutor-study-review/plan.md).

## Quyết định đã chốt (hội thoại 2026-09-26)

| # | Quyết định |
| --- | --- |
| P1 | **Không import, không phụ thuộc DeepTutor**, kể cả script dev hay sinh fixture. Được đọc code DeepTutor để chuyển thể. |
| P2 | **Port đúng hành vi, không tự thiết kế lại**: cổng đạt, công thức mastery, lịch ôn, `next_objective`, cách chấm giữ nguyên như v1.6.9 (commit `da856ad`). |
| P3 | **Kiểm hành vi bằng test chuyển thể từ test của DeepTutor** (`deeptutor/learning/tests/`): giữ nguyên ca kiểm tra và giá trị kỳ vọng, chỉ đổi import. |
| P4 | **Giấy phép Apache-2.0**: file port có header ghi nguồn và "modified"; `licenses/DeepTutor-LICENSE.txt` và `NOTICE` ở gốc service. |

## Bảng port

| Nguồn DeepTutor (`third_party/deeptutor/deeptutor/`) | Đích |
| --- | --- |
| `learning/models.py`, `learning/pending.py`, `utils/text_display.py` (một hàm) | `app/mastery/models.py`, `pending.py` |
| `learning/policy.py`, `scheduler.py`, `mastery.py`, `grading.py` | `app/mastery/policy.py`, `scheduler.py`, `mastery.py`, `grading.py` |
| `learning/service.py` (chỉ các method app gọi) | `app/mastery/service.py` |
| `learning/storage.py` (lỗi và `LearningTransaction`) | `app/mastery/store.py` |
| `services/llm` (quy tắc reasoning, temperature, retry JSON), `utils/json_parser.py` | `app/llm/client.py`, `app/llm/json_payload.py` (tự viết theo cùng quy tắc) |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Test chặn và engine thuần (models, policy, scheduler, grading)](./phase-01-guard-and-pure-engine.md) | Completed (2026-09-26) |
| 2 | [Port service và hợp đồng store, chuyển toàn bộ app sang `app.mastery`](./phase-02-service-and-switch.md) | Completed (2026-09-26) |
| 3 | [LLM client riêng và gỡ DeepTutor khỏi build](./phase-03-llm-client-and-unplug.md) | Completed (2026-09-26) |

## Kết quả (2026-09-26, commit `dbc0cdd`)

- Không file nào import `deeptutor`; `.venv` không cài `deeptutor`; image build từ Dockerfile mới không có `third_party`
  và import được API lẫn consumer. `.dockerignore` loại `third_party` khỏi build context.
- Gate: `pytest tests -rs` không có `PYTHONPATH` tới `third_party` → **306 passed, 0 skipped, 0 failed** (có PostgreSQL,
  RabbitMQ). Trước refactor: 179 passed; mọi test cũ chỉ đổi import, không đổi kỳ vọng.
- Engine: 96 test engine của DeepTutor chuyển thể (`tests/test_mastery_{models,policy,scheduler,grading}.py`) và 22 test
  service (`tests/test_mastery_service.py`, trên `tests/mastery_memory_store.py`); ca bị loại ghi ở đầu mỗi file.
- `state_json` do model DeepTutor ghi: 5/5 path trong DB dev đọc và ghi lại giống hệt bằng model port (kiểm một lần, không
  commit dữ liệu học viên).
- `tests/test_no_deeptutor_dependency.py` fail nếu có import `deeptutor` hoặc file build nhắc tới bản clone.
- Review độc lập: port trung thành; đã sửa quy tắc bỏ `temperature` khi có `reasoning_effort` và mô tả OpenAPI.
- Lệch so với file pha: test để phẳng `tests/test_mastery_*.py` theo quy ước repo; nhóm interaction của
  `LearningTransaction` port ngay ở pha 2 vì `replace_modules_for_path` cần; parse JSON không có bước json-repair (JSON
  hỏng → retry như cũ); `.gitignore` vẫn giữ `deeptutor-data/` vì thư mục cũ trên máy dev có thể chứa key.

## Việc còn lại

1. **E2E ordering qua Gateway với stub** (năm kịch bản trong README service): chưa chạy vì cần dựng đủ stack; đã thay bằng
   test HTTP thật với stub và smoke test trong image.
2. **Sửa spec của nhóm (cần nhóm duyệt, commit riêng):** `.sdd/specs/ai-learning-phase1-plan.md` (dòng 18, 28, 35–36,
   133–136), `SERVICE_ARCHITECTURE_V2.md` (78, 332), `FEATURE_TREE_V2.md` (11) còn ghi "DeepTutor là engine / cài từ
   submodule". Đổi thành "engine mastery là bản port của DeepTutor v1.6.9 trong `app/mastery`, không có logic adaptive nào
   khác; service không phụ thuộc DeepTutor lúc chạy".
3. **Máy dev:** chuyển key trong `services/ai-learning-service/deeptutor-data/` sang `.env` (`AI_LEARNING_LLM_API_KEY`), xóa
   thư mục đó, đổi key nếu từng chia sẻ.
4. Vệ sinh repo có từ trước: 25 file `.pyc` đang được track, `.gitignore` chưa có `__pycache__/`.

## Quy tắc (cho mọi thay đổi sau này ở engine)

1. **Không `import deeptutor`**, không thêm `third_party` vào `PYTHONPATH`, `sys.path`, `Dockerfile`, requirements hay compose.
2. **Port, không sáng tạo.** Logic và hằng số (ngưỡng, hệ số, tên event, chuỗi status) chép nguyên văn; mỗi hàm port có
   test chuyển thể tương ứng.
3. Mỗi file port bắt đầu bằng header ghi nguồn:
   ```python
   # Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/<file>.py @ da856ad.
   # Modified for IELTSPath: <thay đổi chính, một dòng>.
   ```
4. Không đổi hợp đồng công khai (`/status`, `/map`, `/progress`, `/paths`, schema DB, event RabbitMQ, event `path.*`)
   khi chỉ refactor.

## Lệnh gate

Từ `services/ai-learning-service`, cần PostgreSQL và RabbitMQ (`docker compose up -d ai-learning-db rabbitmq`):

```bash
export PYTHONDONTWRITEBYTECODE=1
export AI_LEARNING_TEST_DATABASE_URL=postgresql://<user>:<pass>@localhost:<port>/<db_test>
export AI_LEARNING_TEST_AMQP_URL=amqp://<user>:<pass>@localhost:5672/
python -m pytest tests -rs          # 0 fail, 0 skip
python -m compileall -q app main.py
git diff --check && graphify update .
```
