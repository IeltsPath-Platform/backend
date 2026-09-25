---
phase: 1
title: "Gemini through DeepTutor's LLM layer"
status: pending
priority: P1
dependencies: []
effort: "~3h"
---

# Phase 1: Gemini through DeepTutor's LLM layer

## Overview
AI Learning gọi Gemini **đúng như DeepTutor đang gọi LLM**:
- cấu hình nằm trong model catalog của DeepTutor;
- lời gọi đi qua `deeptutor.services.llm.complete()`.

Không viết client riêng, không thêm thư viện, không có biến môi trường cho key. Pha này chỉ dựng phần gọi LLM; việc
sắp thứ tự nằm ở pha 2 và 3.

## Context
Các điểm dưới đây đã kiểm chứng trên DeepTutor v1.6.9 (submodule `third_party/deeptutor`).

**Cấu hình**
- Cấu hình LLM của DeepTutor chỉ đến từ model catalog: `$DEEPTUTOR_HOME/data/user/settings/model_catalog.json`.
  - Không đặt `DEEPTUTOR_HOME` thì DeepTutor dùng thư mục đang chạy.
  - DeepTutor cố ý không lấy key từ `.env` hay biến môi trường (test `tests/services/config/test_catalog_env_overlay.py`).
  - Compose của DeepTutor gắn thư mục này qua volume (`./data:/app/data`) và hướng dẫn "Configure providers in
    data/user/settings/model_catalog.json or the UI".
- Gemini là provider có sẵn trong `deeptutor/services/provider_registry.py`:
  - `name="gemini"`, backend `openai_compat`;
  - endpoint mặc định `https://generativelanguage.googleapis.com/v1beta/openai/`.
  - Tức là DeepTutor gọi Gemini qua endpoint tương thích OpenAI của Google, bằng thư viện `openai`, vốn đã là
    dependency của DeepTutor.
- Catalog có hai service dạng LLM là `llm` và `task`. Plan dùng `llm`, vì:
  - `task` chỉ dùng được qua `task_llm_scope(TaskKind...)`, mà `TaskKind` là enum đóng trong submodule, không sửa được;
  - trong DeepTutor, lộ trình do model `llm` thiết kế (chế độ outline), không phải model `task`.

**Cách gọi**
- Mẫu lời gọi một lần, không hội thoại, là `deeptutor/services/mastery_hints.py`:
  - import `complete` ngay trong hàm;
  - `max_retries=0`;
  - bọc bằng `asyncio.wait_for(..., timeout=_LLM_TIMEOUT)`;
  - lỗi thì trả kết quả rỗng, không làm hỏng việc chính.
- Lời gọi xin nguyên một JSON trong một lần có quy tắc chung của DeepTutor: `json_with_reasoning_retry` trong
  `deeptutor/services/llm/structured_retry.py`. Nếu câu trả lời không dùng được, nó hỏi lại một lần với mức suy luận
  thấp.
- Test của DeepTutor thay hàm gọi LLM của call site bằng hàm giả (ví dụ `tests/reading/test_reading_hints.py` thay
  `_call_llm`).

**Chạy thử ngày 2026-09-25** (server giả kiểu OpenAI, catalog tạm, qua lớp LLM thật của DeepTutor)
- Profile `binding: gemini` tạo request `POST {base_url}/chat/completions`:
  - header `Authorization: Bearer <key>`;
  - body giữ `response_format: {"type": "json_object"}`.
- Lỗi:

  | Tình huống | DeepTutor trả về |
  | --- | --- |
  | HTTP 500 hoặc 429 | `LLMAPIError` |
  | Catalog không có model đang chọn | `LLMConfigError` |
  | Quá hạn `wait_for` | `asyncio.TimeoutError` |
  | Câu trả lời không phải JSON | `{}`, sau 2 lần gọi |

  Message lỗi không chứa key.
- Import lớp LLM có tác dụng phụ:
  - đọc catalog;
  - đặt `OPENAI_API_KEY` và `OPENAI_BASE_URL` trong process;
  - ghi `settings/system.json` và sổ token `usage.sqlite3` dưới `data/user/`.
- DeepTutor ghi file catalog với quyền `0600`. `get_llm_config()` cache cấu hình suốt đời process.

## Requirements
- Functional:
  - Cấu hình theo cách DeepTutor:
    - Một profile Gemini trong service `llm` của catalog:
      - `binding: "gemini"`;
      - `base_url` để trống, để DeepTutor dùng endpoint mặc định của provider;
      - `api_key`;
      - một model Gemini đang được chọn (`active_profile_id`, `active_model_id`).
    - Commit file mẫu `services/ai-learning-service/model_catalog.example.json`, với key giả.
    - File thật đặt ở `services/ai-learning-service/deeptutor-data/user/settings/model_catalog.json`. Thư mục
      `deeptutor-data/` bị git-ignore.
    - Không có biến môi trường cho key, model hay endpoint.
  - Lời gọi `DeepTutorOrderingLlm.propose(system_prompt, payload) -> LlmProposal`:
    - Import `complete` và `json_with_reasoning_retry` ngay trong hàm, như `mastery_hints.py`.
    - Gọi `complete(prompt=payload, system_prompt=..., response_format={"type": "json_object"}, max_retries=0,
      reasoning_effort=...)`, bọc bằng `json_with_reasoning_retry(..., expected_key="modules")`.
    - Toàn bộ nằm trong `asyncio.wait_for` với hằng timeout, mặc định 20 giây, giống `_LLM_TIMEOUT` của DeepTutor.
      Constructor nhận timeout để test đặt ngắn hơn.
    - `max_retries=0`: không retry khi lỗi, giống `mastery_hints.py`. Chiến lược retry thuộc phần "làm sau" (L4).
  - `LlmProposal` gồm `payload: dict | None`, `reason: str | None` và `model: str | None`:

    | Kết quả | `payload` | `reason` |
    | --- | --- | --- |
    | Thành công | dict trả về | `None` |
    | `LLMConfigError` | `None` | `llm_not_configured` |
    | `asyncio.TimeoutError` | `None` | `llm_timeout` |
    | `LLMError` khác | `None` | `llm_error` |
    | `json_with_reasoning_retry` trả `{}` | `None` | `llm_unusable_response` |

    `model` lấy từ `get_llm_config().model` của DeepTutor, chỉ dùng cho log và event.
- Non-functional:
  - Key chỉ nằm trong file catalog. Không bao giờ xuất hiện trong log, exception, event hay report. Log chỉ ghi tên
    model, reason và thời gian.
  - Không thêm thư viện vào `requirements.txt`. `openai`, `loguru`, `json-repair` và các gói khác là dependency của
    DeepTutor, đã có trong image qua `pip install ./third_party/deeptutor`. Môi trường test cần cài giống vậy.
  - Consumer không import lớp LLM của DeepTutor.

## Related Code Files
- Create: `services/ai-learning-service/app/learning/deeptutor_llm.py` (`DeepTutorOrderingLlm`, `LlmProposal`)
- Create: `services/ai-learning-service/model_catalog.example.json`
- Modify: `.gitignore` (thêm `services/ai-learning-service/deeptutor-data/`)
- Tests: `tests/test_deeptutor_llm.py`

## Implementation Steps
### Tests Before
1. Chạy suite Python, ghi mốc.
2. Viết test **fail trước**, loại unit. Thay `complete` bằng hàm giả, như test của DeepTutor:
   - JSON hợp lệ → `payload`. Lời gọi có `response_format`, `max_retries=0`, cùng system prompt và payload.
   - `LLMConfigError`, `LLMAPIError`, hàm giả chạy quá timeout, hoặc câu trả lời không phải JSON → đúng `reason`.
     Không message nào chứa key.
3. Viết test **fail trước**, loại tích hợp, đi qua lớp LLM thật của DeepTutor:
   - Dựng một server giả kiểu OpenAI chạy trong process test.
   - Tạo `DEEPTUTOR_HOME` tạm, với profile `gemini` có `base_url` trỏ tới server giả.
   - Kiểm tra: request đi tới `/chat/completions` với `Authorization: Bearer`, có `response_format`, và payload được
     đọc đúng.
   - Server trả 500 → `llm_error`. Catalog không có model → `llm_not_configured`.
   - Xóa cache của DeepTutor giữa các case, như test của DeepTutor: `clear_llm_config_cache()`,
     `ModelCatalogService._instances.clear()`, `PathService.reset_instance()`.
   - Đặt `DEEPTUTOR_HOME` trước khi import, để DeepTutor không ghi file vào repo.
### Refactor
4. Viết `deeptutor_llm.py`, file catalog mẫu và dòng `.gitignore`.
### Tests After / Regression Gate
5. Suite pass, 0 skip; `compileall`; `git diff --check`; `graphify update .`.
6. `git status` sạch, ngoài các file của pha: không có `data/` hay `usage.sqlite3` bị tạo trong repo.

## Success Criteria
- [ ] Gọi được Gemini qua `deeptutor.services.llm.complete()` với cấu hình trong catalog của DeepTutor.
- [ ] Không có client, thư viện hay biến môi trường nào cho key được thêm vào.
- [ ] Mọi kiểu lỗi quy về một `reason`; key không lộ ở đâu.
- [ ] Catalog chưa cấu hình LLM → `llm_not_configured`, hành vi giữ như hiện nay.

## Risk Assessment
- **DeepTutor đổi API của lớp LLM khi nâng submodule:** mọi chỗ gọi DeepTutor gom trong `deeptutor_llm.py`. Test tích
  hợp khóa hành vi.
- **Tác dụng phụ khi import** (đặt `OPENAI_*` trong process, ghi `data/user/`): chỉ import trong process API, ngay
  lúc gọi. Test đặt `DEEPTUTOR_HOME` tạm.
- **Sửa catalog khi đang chạy không có tác dụng**, vì DeepTutor cache cấu hình: tài liệu ghi rõ phải khởi động lại
  container API (pha 4).
