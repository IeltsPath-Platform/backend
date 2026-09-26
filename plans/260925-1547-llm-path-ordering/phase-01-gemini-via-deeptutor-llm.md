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

Không viết client riêng, không thêm thư viện runtime, không có biến môi trường cho key. Pha này chỉ dựng phần gọi
LLM; việc sắp thứ tự nằm ở pha 2 và 3.

## Đọc trước khi code (DeepTutor, submodule `third_party/deeptutor`, v1.6.9)
| File | Để làm gì |
| --- | --- |
| `deeptutor/services/mastery_hints.py` (hàm `_generate`) | Mẫu lời gọi một lần, không hội thoại. Làm theo đúng mẫu này. |
| `deeptutor/services/llm/factory.py` (`complete`) | Tham số của lời gọi. |
| `deeptutor/services/llm/structured_retry.py` (`json_with_reasoning_retry`) | Đọc JSON từ LLM, hỏi lại một lần nếu câu trả lời không dùng được. |
| `deeptutor/services/llm/exceptions.py` | Các loại lỗi; tất cả kế thừa `LLMError`. |
| `deeptutor/services/llm/config.py` (`get_llm_config`, `clear_llm_config_cache`) | Đọc cấu hình, cache. |
| `deeptutor/services/provider_registry.py` (provider `gemini`) | Endpoint mặc định của Gemini. |
| `deeptutor/services/config/model_catalog.py` | Cấu trúc và vị trí file catalog. |
| `tests/services/config/test_provider_runtime.py` | Mẫu catalog hợp lệ trong test của DeepTutor. |

## Context
Các điểm dưới đây đã kiểm chứng bằng cách đọc code và chạy thử lớp LLM thật của DeepTutor với một server giả kiểu
OpenAI (2026-09-25 và 2026-09-26).

**Cấu hình**
- Cấu hình LLM của DeepTutor chỉ đến từ model catalog: `$DEEPTUTOR_HOME/data/user/settings/model_catalog.json`.
  - Không đặt `DEEPTUTOR_HOME` thì DeepTutor dùng thư mục đang chạy (cwd).
  - DeepTutor cố ý không lấy key từ `.env` hay biến môi trường (test `tests/services/config/test_catalog_env_overlay.py`).
  - Compose của DeepTutor gắn thư mục này qua volume (`./data:/app/data`).
- Gemini là provider có sẵn: `name="gemini"`, backend `openai_compat`, endpoint mặc định
  `https://generativelanguage.googleapis.com/v1beta/openai/`. DeepTutor gọi Gemini qua endpoint tương thích OpenAI
  của Google, bằng thư viện `openai`.
- Catalog có hai service dạng LLM là `llm` và `task`. Dùng `llm`, vì:
  - `task` chỉ dùng được qua `task_llm_scope(TaskKind...)`, mà `TaskKind` là enum đóng trong submodule, không sửa được;
  - trong DeepTutor, lộ trình do model `llm` thiết kế (chế độ outline).

**Hành vi đã chạy thử**
- Profile `binding: gemini` tạo request `POST {base_url}/chat/completions`, header `Authorization: Bearer <key>`,
  body giữ `response_format: {"type": "json_object"}`.
- Lỗi:

  | Tình huống | DeepTutor trả về |
  | --- | --- |
  | HTTP 500 hoặc 429 | `LLMAPIError` |
  | Catalog không có model đang chọn | `LLMConfigError` (ném ngay từ `get_llm_config()`, chưa gọi mạng) |
  | Quá hạn `asyncio.wait_for` | `asyncio.TimeoutError` |
  | Câu trả lời không phải JSON | `json_with_reasoning_retry` trả `{}` sau 2 lần gọi |
  | Thư mục data không ghi được | `OSError` thường, **không** phải `LLMError` |

  Message lỗi không chứa key.
- Import lớp LLM có tác dụng phụ:
  - đọc catalog, tạo catalog rỗng nếu chưa có;
  - đặt `OPENAI_API_KEY` và `OPENAI_BASE_URL` trong process;
  - ghi `data/user/settings/system.json` và sổ token `data/user/usage.sqlite3`.
- DeepTutor ghi file catalog với quyền `0600`. `get_llm_config()` cache cấu hình suốt đời process; lỗi
  `LLMConfigError` thì không bị cache.

**Môi trường test hiện tại**
- README hướng dẫn chạy test bằng PowerShell với `PYTHONPATH = "../../third_party/deeptutor;."`, không cài package
  DeepTutor. `requirements.txt` chỉ đủ cho `deeptutor.learning`.
- Trong venv sạch chỉ có `requirements.txt`, lớp LLM của DeepTutor cần thêm đúng 4 gói: `openai`, `loguru`,
  `json-repair`, `aiohttp`. Đã chạy đủ các case trên với 4 gói này.

## Requirements
- Functional:
  - Cấu hình:
    - Commit file mẫu `services/ai-learning-service/model_catalog.example.json`, đúng dạng dưới đây, key giả:
      ```json
      {
        "version": 1,
        "services": {
          "llm": {
            "active_profile_id": "llm-profile-gemini",
            "active_model_id": "llm-model-gemini",
            "profiles": [
              {
                "id": "llm-profile-gemini",
                "name": "Gemini",
                "binding": "gemini",
                "base_url": "",
                "api_key": "REPLACE_WITH_GEMINI_API_KEY",
                "api_version": "",
                "extra_headers": {},
                "models": [
                  {"id": "llm-model-gemini", "name": "Gemini Flash", "model": "gemini-2.5-flash"}
                ]
              }
            ]
          }
        }
      }
      ```
      `base_url` để trống để DeepTutor dùng endpoint mặc định của provider. Tên model là ví dụ; nhóm chọn model
      Gemini hiện hành khi điền.
    - File thật: `services/ai-learning-service/deeptutor-data/user/settings/model_catalog.json`. Thêm
      `services/ai-learning-service/deeptutor-data/` vào `.gitignore` gốc.
    - Không có biến môi trường cho key, model hay endpoint.
  - Module `app/learning/deeptutor_llm.py`:
    ```python
    @dataclass(frozen=True)
    class LlmProposal:
        payload: dict[str, Any] | None
        reason: str | None      # None khi thành công
        model: str | None       # tên model từ catalog, chỉ để log/event

    class DeepTutorOrderingLlm:
        def __init__(self, *, timeout_seconds: float = 20.0, complete: Callable[..., Awaitable[str]] | None = None): ...
        async def propose(self, system_prompt: str, payload: str) -> LlmProposal: ...
    ```
    - Import `complete`, `get_llm_config`, `json_with_reasoning_retry` và các exception **ngay trong hàm**, như
      `mastery_hints.py`, để consumer và các module khác không kéo lớp LLM vào.
    - Bước 1: `get_llm_config()`. Lỗi `LLMConfigError` → trả `llm_not_configured`, không gọi mạng. Lấy `model`.
    - Bước 2: gọi, toàn bộ trong `asyncio.wait_for(..., timeout_seconds)`:
      ```python
      async def run(reasoning_effort):
          return await complete(prompt=payload, system_prompt=system_prompt,
                                response_format={"type": "json_object"},
                                temperature=0.2, max_tokens=8192, max_retries=0,
                                reasoning_effort=reasoning_effort)
      data = await json_with_reasoning_retry(run, expected_key="modules", logger_instance=logger)
      ```
    - `complete` truyền vào constructor chỉ dùng cho test; mặc định là `deeptutor.services.llm.complete`.
    - `max_retries=0`: không retry khi lỗi, giống `mastery_hints.py`. Chiến lược retry thuộc phần "làm sau" (L4).
  - Bảng kết quả:

    | Kết quả | `payload` | `reason` |
    | --- | --- | --- |
    | Thành công | dict trả về | `None` |
    | `LLMConfigError` | `None` | `llm_not_configured` |
    | `asyncio.TimeoutError` | `None` | `llm_timeout` |
    | `LLMError` khác | `None` | `llm_error` |
    | `json_with_reasoning_retry` trả `{}` | `None` | `llm_unusable_response` |
    | Mọi exception khác (ví dụ `OSError` khi ghi `data/`) | `None` | `llm_error` |

    `propose` **không bao giờ ném lỗi**: tạo path không được hỏng vì LLM (L4). Mỗi lỗi ghi một dòng log
    `warning` gồm reason, model, thời gian và tên class lỗi. Không log message gốc của lỗi LLM, không log payload.
- Non-functional:
  - Key chỉ nằm trong file catalog. Không bao giờ xuất hiện trong log, exception, event hay report.
  - Không thêm gì vào `requirements.txt`: image đã cài đủ qua `pip install ./third_party/deeptutor`.
  - Thêm `services/ai-learning-service/requirements-test.txt` cho môi trường test, dùng đúng ràng buộc phiên bản
    của `third_party/deeptutor/pyproject.toml`:
    ```text
    -r requirements.txt
    # DeepTutor's LLM layer (deeptutor.services.llm); the image gets these from ./third_party/deeptutor.
    openai>=1.30.0
    loguru>=0.7.3,<1.0.0
    json-repair>=0.57.0,<1.0.0
    aiohttp>=3.9.4
    ```
  - Consumer không import lớp LLM của DeepTutor.

## Related Code Files
- Create: `services/ai-learning-service/app/learning/deeptutor_llm.py`
- Create: `services/ai-learning-service/model_catalog.example.json`
- Create: `services/ai-learning-service/requirements-test.txt`
- Create: `services/ai-learning-service/tests/deeptutor_llm_support.py`
  - đặt `DEEPTUTOR_HOME` tạm **trước** khi bất kỳ test nào import `deeptutor.services`;
  - dựng server giả kiểu OpenAI chạy trong thread (`http.server`), có các chế độ: trả JSON, trả 500, chậm, trả chữ;
  - ghi catalog tạm có profile `gemini` với `base_url` trỏ tới server giả;
  - hàm reset cache của DeepTutor giữa các case: `clear_llm_config_cache()`,
    `ModelCatalogService._instances.clear()`, `PathService.reset_instance()` (của DeepTutor).
- Modify: `.gitignore` (thêm `services/ai-learning-service/deeptutor-data/`)
- Modify: `services/ai-learning-service/README.md` (mục chạy test: `pip install -r requirements-test.txt`)
- Tests: `tests/test_deeptutor_llm.py`

## Implementation Steps
### Tests Before
1. Chạy suite Python, ghi mốc.
2. Test unit (thay `complete` bằng hàm giả, như test của DeepTutor thay `_call_llm`):
   - JSON hợp lệ → `payload`; hàm giả nhận `response_format`, `max_retries=0`, đúng system prompt và payload.
   - `LLMAPIError`, `OSError`, hàm giả ngủ lâu hơn timeout, câu trả lời không phải JSON → đúng `reason`, không ném lỗi.
   - Không dòng log nào chứa key (dùng `assertLogs`).
3. Test tích hợp đi qua lớp LLM thật của DeepTutor (dùng `deeptutor_llm_support.py`):
   - Request tới `/chat/completions`, có `Authorization: Bearer`, có `response_format`; payload đọc đúng.
   - Server trả 500 → `llm_error`. Catalog không có model → `llm_not_configured`, server giả không nhận request nào.
### Refactor
4. Viết `deeptutor_llm.py`, file mẫu, `requirements-test.txt`, `.gitignore`, README.
### Tests After / Regression Gate
5. Suite pass, 0 skip; `compileall`; `git diff --check`; `graphify update .`.
6. `git status` sạch ngoài các file của pha: không có `data/` hay `usage.sqlite3` bị tạo trong repo.

## Success Criteria
- [ ] Gọi được Gemini qua `deeptutor.services.llm.complete()` với cấu hình trong catalog của DeepTutor.
- [ ] Không có client, thư viện runtime hay biến môi trường nào cho key được thêm vào.
- [ ] Mọi kiểu lỗi quy về một `reason`, `propose` không bao giờ ném lỗi; key không lộ ở đâu.

## Risk Assessment
- **DeepTutor đổi API của lớp LLM khi nâng submodule:** mọi chỗ gọi DeepTutor gom trong `deeptutor_llm.py`; test
  tích hợp khóa hành vi.
- **Tác dụng phụ khi import:** chỉ import ngay lúc gọi, trong process API; test đặt `DEEPTUTOR_HOME` tạm.
- **Sửa catalog khi đang chạy không có tác dụng** vì DeepTutor cache cấu hình: tài liệu ghi rõ phải khởi động lại
  container API (pha 4).
