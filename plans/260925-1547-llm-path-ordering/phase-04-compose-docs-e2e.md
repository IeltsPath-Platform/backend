---
phase: 4
title: "Compose, docs and E2E"
status: completed
priority: P1
dependencies: [1, 2, 3]
effort: "~4h"
---

# Phase 4: Compose, docs and E2E

## Overview
Đưa catalog của DeepTutor vào compose theo đúng cách DeepTutor triển khai: gắn thư mục `data` qua volume. Viết tài
liệu, rồi kiểm chứng E2E qua Gateway với một server giả kiểu OpenAI, để không phụ thuộc key thật. Key thật do nhóm
tự chạy thử trên máy.

## Đọc trước khi code
| File | Để làm gì |
| --- | --- |
| `docker-compose.yml` (`ai-learning-api`, `ai-learning-consumer`) | Hai service dùng chung một image (`&ai-learning-build`); consumer đổi `command`. |
| `services/ai-learning-service/Dockerfile` | Đang chạy bằng `USER appuser` (uid 10001), `WORKDIR /app`. |
| `third_party/deeptutor/docker-compose.ghcr.yml` | DeepTutor gắn `./data:/app/data`, cấu hình provider trong catalog. |
| `third_party/deeptutor/Dockerfile` (entrypoint, đoạn `chown -R deeptutor:deeptutor /app/data`) | Cách DeepTutor làm thư mục data ghi được. |
| `third_party/deeptutor/deeptutor_cli/config_cmd.py` (`config show`) | Lệnh kiểm tra cấu hình LLM, che key. |

## Context
- DeepTutor ghi vào `data/user/` khi dùng lớp LLM: catalog đã chuẩn hóa, `settings/system.json`, `usage.sqlite3`.
  Thư mục gắn từ host phải ghi được bởi user trong container.
- Nhóm chạy trên Windows (README dùng PowerShell) với Docker Desktop: thư mục gắn từ host ghi được với mọi uid.
  Trên Linux thì cần `chown` như entrypoint của DeepTutor.
- `ai-learning-api` và `ai-learning-consumer` dùng chung image. Entrypoint mới phải chạy đúng cả `CMD` mặc định
  (uvicorn) lẫn `command` của consumer.
- DeepTutor gọi Gemini qua endpoint tương thích OpenAI, nên server giả là server kiểu OpenAI
  (`POST .../chat/completions`), không phải `generateContent`.

## Requirements
- Functional:
  - Dockerfile:
    - `ENV DEEPTUTOR_HOME=/app`; tạo sẵn `/app/data` thuộc `appuser`.
    - Thêm `services/ai-learning-service/docker-entrypoint.sh`, chạy bằng root:
      `chown -R appuser:appuser /app/data 2>/dev/null || true`, rồi
      `exec setpriv --reuid=appuser --regid=appuser --init-groups "$@"`. Bỏ `USER appuser`, thêm
      `ENTRYPOINT ["/app/docker-entrypoint.sh"]`, giữ `CMD` uvicorn. Kiểm tra `setpriv` có trong `python:3.11-slim`;
      nếu không có thì cài `util-linux` hoặc dùng `gosu`.
    - Kiểm chứng: process uvicorn và consumer đều chạy bằng uid 10001 (`docker compose exec ... id`).
  - Compose:
    - `ai-learning-api` thêm volume `./services/ai-learning-service/deeptutor-data:/app/data`, giống `./data:/app/data`
      của DeepTutor. Không thêm biến môi trường nào cho key hay model.
    - Chưa có catalog: DeepTutor tự tạo catalog rỗng, lời gọi trả `llm_not_configured`, path theo thứ tự Content.
      Compose vẫn chạy khi chưa có key.
    - `ai-learning-consumer` **không** gắn thư mục này.
    - Service `llm-stub`, chỉ bật khi dùng `--profile llm-stub`:
      - image `python:3.11-slim`, chạy `services/ai-learning-service/tests/e2e/llm_stub.py` (gắn bằng volume),
        cổng nội bộ 8090, trong `codebase-network`;
      - server kiểu OpenAI: đọc payload trong message `user`, trả `{"modules": [...], "rationale": "..."}`;
      - chế độ qua biến `LLM_STUB_MODE`: `reverse` (đảo thứ tự module và KP, mặc định), `unknown_kp` (thêm một KP
        lạ), `slow` (ngủ 30 giây, lâu hơn timeout 20 giây), `error` (trả 500).
    - Commit catalog cho E2E: `services/ai-learning-service/tests/e2e/model_catalog.stub.json`, profile `gemini`, key
      giả `stub-key`, `base_url: http://llm-stub:8090/v1beta/openai/`.
  - README AI Learning, mục "LLM path ordering":
    - Dữ liệu nào được gửi và không được gửi (theo pha 2).
    - Key nằm ở đâu: `deeptutor-data/user/settings/model_catalog.json`, bị git-ignore; DeepTutor ghi với quyền `0600`.
    - Cách cấu hình (PowerShell):
      1. Tạo `deeptutor-data/user/settings/`, chép `model_catalog.example.json` vào đó với tên `model_catalog.json`.
      2. Điền key và model Gemini, để `base_url` trống.
      3. `docker compose restart ai-learning-api` (DeepTutor cache cấu hình).
      4. `docker compose exec ai-learning-api deeptutor config show` → provider `gemini`, key hiện `***`.
    - Hành vi khi lỗi: bảng các `reason` (pha 1 và 3) và thứ tự Content.
    - Cách tắt: xóa profile trong service `llm` của catalog, hoặc xóa file catalog, rồi restart.
    - Sổ token của DeepTutor: `deeptutor-data/user/usage.sqlite3`.
  - README root: mục chạy local ghi bước cấu hình Gemini (đường dẫn catalog, không ghi key) và `requirements-test.txt`.
- Non-functional:
  - Không commit key hay file catalog thật. Report không chứa key hay nội dung prompt đầy đủ.

## Implementation Steps
1. Chạy E2E qua Gateway, 2 learner, `--profile llm-stub`, chép `model_catalog.stub.json` vào `deeptutor-data`:
   - `reverse`, learner có placement:
     - thứ tự module trong `/map` ngược với thứ tự Content; `/status` theo thứ tự mới;
     - test-out vẫn đúng; event `path.ordered` có `source=llm`.
   - `unknown_kp` → path theo thứ tự Content; event `source=content`, reason `invalid_ordering`,
     detail `unknown_knowledge_point`.
   - `slow` → path vẫn được tạo sau khoảng 20 giây, thứ tự Content, reason `llm_timeout`.
   - `error` → thứ tự Content, reason `llm_error`.
   - Xóa catalog, restart → thứ tự Content, reason `llm_not_configured`.
   - Mỗi kịch bản dùng một learner/goal mới, vì LLM chỉ được gọi khi tạo path.
2. Chạy lại toàn bộ gate Java và Python: 0 fail, 0 skip.
3. Ghi report vào `plans/260925-1547-llm-path-ordering/reports/`, không có key, token hay nội dung prompt đầy đủ.
4. Viết hướng dẫn để nhóm chạy thử với key Gemini thật (trong README): điền catalog, restart, `deeptutor config show`,
   tạo path, xem event `path.ordered` có `source=llm`.

## Success Criteria
- [ ] Compose chạy được cả khi có và khi không có catalog.
- [ ] Container API ghi được `/app/data`; API và consumer chạy bằng `appuser`.
- [ ] E2E với server giả đạt 5 kịch bản trên.
- [ ] Tài liệu ghi rõ dữ liệu gửi ra ngoài, key nằm ở đâu, và cách tắt tính năng.

## Risk Assessment
- **Quyền ghi thư mục gắn từ host khác nhau giữa Linux và Docker Desktop:** entrypoint `chown` như DeepTutor; kiểm
  chứng trên Linux trong E2E.
- **`deeptutor config show` cần thêm cấu hình khác của DeepTutor và báo lỗi:** nếu vậy, README dùng event
  `path.ordered` để kiểm tra thay cho lệnh này.
- **Catalog của DeepTutor sau này dùng chung cho Tutor:** đúng chủ đích. Muốn tắt riêng việc sắp xếp thì cần một cờ
  mới; chưa làm.
