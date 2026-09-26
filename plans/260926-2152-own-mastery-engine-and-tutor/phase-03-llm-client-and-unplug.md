---
phase: 3
title: "LLM client riêng và gỡ DeepTutor khỏi build"
status: completed
priority: P1
dependencies: [2]
effort: "~1.5d"
---

# Phase 3: LLM client riêng và gỡ DeepTutor khỏi build

## Overview
Thay lớp LLM của DeepTutor (dùng cho việc Gemini sắp thứ tự path) bằng một client nhỏ gọi API chuẩn OpenAI. Cấu hình
bằng biến môi trường thay cho `model_catalog.json`. Sau đó gỡ DeepTutor khỏi `Dockerfile`, requirements, compose, README
và test. Kết thúc pha: **không còn gì trong service tham chiếu `third_party` hay `deeptutor`**; test chặn chạy ở chế độ cuối.

## Đọc trước
- Code của mình: `app/learning/deeptutor_llm.py` (hành vi và các mã lý do: `llm_not_configured`, `llm_timeout`,
  `llm_unusable_response`, `llm_error`), `tests/test_deeptutor_llm.py`, `tests/deeptutor_llm_support.py`,
  `tests/e2e/llm_stub.py` (endpoint `/v1beta/openai/chat/completions`), `main.py`.
- Chỉ đọc, trong `third_party/deeptutor/deeptutor/`: `services/llm/structured_retry.py` (`payload_with_reasoning_retry`:
  lần 1 dùng reasoning mặc định, JSON không dùng được thì thử lại một lần với `RETRY_REASONING_EFFORT` trong
  `services/llm/reasoning_params.py`), `utils/json_parser.py` (`parse_json_response`).

## Requirements
- `app/llm/client.py`, class `ChatCompletionsClient`:
  - `httpx.AsyncClient`, `POST {base_url}/chat/completions`, header `Authorization: Bearer <key>`.
  - Tham số: `messages`, `temperature`, `max_tokens`, `response_format`, `reasoning_effort` (bỏ khỏi body khi `None`),
    `tools` (dùng ở pha 5).
  - Trả về nội dung text và `tool_calls` đã parse. Lỗi HTTP hoặc mạng → exception riêng có mã, **không** kèm nội dung
    response.
  - `json_with_reasoning_retry(run, expected_key)`: chuyển thể đúng quy tắc một lần thử lại của DeepTutor; parse JSON bỏ
    rào ```` ```json ````, như `parse_json_response` ở mức app cần.
- Cấu hình (`app/config.py`, class riêng để consumer không cần): `AI_LEARNING_LLM_BASE_URL` (mặc định
  `https://generativelanguage.googleapis.com/v1beta/openai/`), `AI_LEARNING_LLM_MODEL`, `AI_LEARNING_LLM_API_KEY`
  (`SecretStr`, thiếu → `llm_not_configured`), `AI_LEARNING_LLM_REASONING_EFFORT` (tùy chọn),
  `AI_LEARNING_LLM_TIMEOUT_SECONDS` (mặc định 20).
- `app/learning/ordering_llm.py`, class `OrderingLlm` thay `DeepTutorOrderingLlm`: **giữ nguyên** các mã lý do, event
  `path.ordered`, log (không nội dung, không key), timeout. Xóa `app/learning/deeptutor_llm.py`.
- Test: đổi `test_deeptutor_llm.py` thành `test_ordering_llm.py`, giữ mọi ca và kỳ vọng. Cấu hình trỏ tới stub bằng biến
  môi trường thay cho catalog tạm. Xóa `tests/deeptutor_llm_support.py`; bỏ `isolate_deeptutor_home()` khỏi
  `tests/conftest.py`. Test "consumer không nạp lớp LLM" viết lại cho `app.llm`.
- **Gỡ khỏi build và cấu hình**:
  - `Dockerfile`: bỏ `DEEPTUTOR_HOME`, `COPY third_party/deeptutor`, `pip install ./third_party/deeptutor`.
  - `requirements-test.txt`: bỏ ghi chú và các gói chỉ DeepTutor cần (`openai`, `loguru`, `json-repair`, `aiohttp`), trừ
    khi app còn dùng.
  - `docker-compose.yml`: bỏ volume `deeptutor-data` của `ai-learning-api`; thêm biến `AI_LEARNING_LLM_*` (key lấy từ
    `.env`, không hard-code).
  - Xóa `model_catalog.example.json`, `tests/e2e/model_catalog.stub.json`; E2E ordering (README) trỏ stub bằng
    `AI_LEARNING_LLM_BASE_URL=http://llm-stub:8090/v1beta/openai/`.
  - `README.md` của service và root: bỏ mục catalog, `deeptutor-data`, usage ledger, `PYTHONPATH` tới `third_party`;
    thêm mục cấu hình LLM bằng biến môi trường. `.gitignore`: bỏ dòng `deeptutor-data/`.
- Test chặn: allowlist rỗng; `BUILD_REFERENCES_THIRD_PARTY = False`.

## Implementation Steps
1. Viết test cho `ChatCompletionsClient` với stub (request đúng shape, không log nội dung, lỗi HTTP có mã, retry JSON).
2. Chuyển thể `test_deeptutor_llm.py`; code client, `OrderingLlm`, config; đổi `main.py`.
3. Gỡ build, compose, README; allowlist rỗng.
4. Gate **không có `PYTHONPATH` tới `third_party`**. Build image: `docker compose build ai-learning-api ai-learning-consumer`
   và chạy lại E2E ordering trong README với stub (mode `reverse`, `unknown_kp`, `slow`, `error`).

## Success Criteria
- [x] `grep -rn "deeptutor\|third_party" services/ai-learning-service --exclude-dir=.venv` chỉ còn header nguồn, `NOTICE`,
      `licenses/`.
- [x] Image build không cần thư mục `third_party`.
- [ ] E2E ordering qua Gateway cho cùng kết quả như trước (chưa chạy, cần đủ stack).
- [x] Suite pass không có `PYTHONPATH` tới `third_party`, 0 skip (305 passed).
