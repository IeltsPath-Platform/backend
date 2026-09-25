---
phase: 1
title: "Gemini client and configuration"
status: pending
priority: P1
dependencies: []
effort: "~2h"
---

# Phase 1: Gemini client and configuration

## Overview
Một client nhỏ gọi API `generateContent` của Gemini bằng `httpx` (đã là dependency), yêu cầu trả về JSON. Không
thêm thư viện mới. Không có key thì tính năng tắt.

## Context
- DeepTutor gọi LLM qua các endpoint kiểu OpenAI (`deeptutor/runtime/agentic/client.py`); client đó gắn với vòng
  agent và session. Plan này chỉ cần một lời gọi, nên không dùng lại client của DeepTutor.
- `app/config.py` có `Settings` (API) và `ConsumerSettings` (consumer). Chỉ API tạo path, nên chỉ API cần key;
  consumer không nhận key.

## Requirements
- Functional:
  - Biến môi trường:
    - `AI_LEARNING_GEMINI_API_KEY`: tùy chọn; trống thì tắt sắp xếp bằng LLM.
    - `AI_LEARNING_GEMINI_MODEL`: mặc định là một model Flash hiện hành, chốt khi cook.
    - `AI_LEARNING_GEMINI_TIMEOUT_SECONDS`: mặc định 15.
  - `GeminiClient.generate_json(system: str, user: str, schema: dict) -> dict`:
    - `POST .../models/{model}:generateContent`, với `responseMimeType: application/json` và `responseSchema`.
    - Key gửi qua header `x-goog-api-key`, không để trong URL.
    - Ném `LlmUnavailable` khi: timeout, lỗi mạng, HTTP 4xx/5xx, hoặc response không phải JSON hợp lệ.
- Non-functional:
  - Key không bao giờ xuất hiện trong log, exception hay event. Log chỉ ghi model, mã HTTP và thời gian.
  - Client dùng `httpx.AsyncClient` và timeout cứng.

## Related Code Files
- Create: `services/ai-learning-service/app/clients/gemini.py`
- Modify: `app/config.py` (các trường mới trong `Settings`; `SecretStr` cho key)
- Tests: `tests/test_gemini_client.py` (dùng `httpx.MockTransport`, không gọi mạng)

## Implementation Steps
### Tests Before
1. Chạy suite Python, ghi mốc.
2. Viết test **fail trước**:
   - Request đúng URL và model; key nằm trong header, không nằm trong URL; body có `responseMimeType` và schema.
   - Response hợp lệ → trả về dict đã parse từ JSON.
   - Timeout, 429, 500, JSON hỏng → `LlmUnavailable`; message không chứa key.
   - Không có key → `Settings.gemini_enabled` là False.
### Refactor
3. Client và cấu hình.
### Tests After / Regression Gate
4. Suite pass; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Gọi được Gemini qua `httpx`, trả JSON; không thêm thư viện.
- [ ] Mọi kiểu lỗi quy về `LlmUnavailable`; key không lộ ở đâu.
- [ ] Không có key thì tính năng tắt, hành vi giữ như hiện nay.

## Risk Assessment
- **API Gemini đổi:** gom URL và định dạng request vào một chỗ; test khóa định dạng.
- **Tên model đổi hoặc hết hạn:** để model là biến môi trường.
