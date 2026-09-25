---
phase: 4
title: "Compose, docs and E2E"
status: pending
priority: P1
dependencies: [1, 2, 3]
effort: "~4h"
---

# Phase 4: Compose, docs and E2E

## Overview
Đưa catalog của DeepTutor vào compose theo đúng cách DeepTutor triển khai: gắn thư mục `data` qua volume. Viết tài
liệu, rồi kiểm chứng E2E qua Gateway với một server giả kiểu OpenAI, để không phụ thuộc key thật. Key thật do nhóm
tự chạy thử trên máy.

## Context
- Compose của DeepTutor gắn `./data:/app/data`. Cấu hình provider nằm trong `data/user/settings/model_catalog.json`.
- Entrypoint image DeepTutor `chown` `/app/data` cho user không đặc quyền, vì DeepTutor ghi vào đó: catalog đã chuẩn
  hóa, `system.json`, `usage.sqlite3`. Image AI Learning đang chạy bằng `appuser` (uid 10001).
- DeepTutor gọi Gemini qua endpoint tương thích OpenAI. Vì vậy server giả cho E2E là server kiểu OpenAI
  (`/chat/completions`), không phải `generateContent`.
- CLI `deeptutor` được cài cùng package. Lệnh `deeptutor config show` in provider, model và endpoint, và che key.

## Requirements
- Functional:
  - Compose, service `ai-learning-api`:
    - `DEEPTUTOR_HOME: /app`;
    - volume `./services/ai-learning-service/deeptutor-data:/app/data`, giống `./data:/app/data` của DeepTutor.
    - Chưa có catalog thì DeepTutor tự tạo catalog rỗng. Lời gọi trả `llm_not_configured` và path theo thứ tự
      Content, nên compose vẫn chạy khi chưa có key.
  - Dockerfile AI Learning: thư mục `/app/data` phải ghi được bởi user trong container. Làm như entrypoint của
    DeepTutor: `chown` `/app/data`, rồi chạy uvicorn bằng `appuser` (hạ quyền bằng `setpriv`). Kiểm chứng rằng sau
    khi container chạy, máy host vẫn sửa được file catalog.
  - `ai-learning-consumer` **không** gắn thư mục này và không có `DEEPTUTOR_HOME`.
  - Server giả `llm-stub` trong compose, chỉ bật khi dùng profile `llm-stub`:
    - Server kiểu OpenAI. Nó đọc payload, rồi đảo ngược thứ tự module và KP.
    - Có chế độ trả KP lạ, chế độ chậm hơn timeout, và chế độ trả 500.
    - Catalog dùng cho E2E có profile `gemini` với `base_url` trỏ tới server giả.
  - README AI Learning, mục "LLM path ordering":
    - Dữ liệu nào được gửi và không được gửi.
    - Key nằm ở đâu:
      - file catalog trong thư mục bị git-ignore;
      - DeepTutor ghi file với quyền `0600`.
    - Cách cấu hình:
      1. Chép `model_catalog.example.json` vào `deeptutor-data/user/settings/model_catalog.json`.
      2. Điền key và model Gemini.
      3. Khởi động lại container API, vì DeepTutor cache cấu hình.
      4. Kiểm tra bằng `deeptutor config show`.
    - Hành vi khi lỗi: bảng các `reason` và thứ tự Content.
    - Cách tắt: bỏ profile LLM khỏi catalog, hoặc không gắn catalog.
    - Sổ token của DeepTutor nằm ở `deeptutor-data/user/usage.sqlite3`.
  - README root:
    - Ghi đường dẫn catalog và bước cấu hình Gemini (không ghi key).
    - Không thêm biến môi trường nào cho key.
    - Nêu rõ môi trường test Python cần dependency của DeepTutor.
  - Postman:
    - Thêm kiểm tra thứ tự module trả về khi có server giả.
    - Thêm một kịch bản catalog chưa cấu hình LLM.
- Non-functional:
  - Không commit key hay file catalog thật. Report không chứa key hay nội dung prompt đầy đủ.

## Implementation Steps
1. Chạy E2E qua Gateway, 2 learner, catalog trỏ tới `llm-stub`:
   - Learner có placement:
     - thứ tự path khác thứ tự Content, đúng theo server giả;
     - test-out vẫn đúng;
     - `/status` theo thứ tự mới.
   - Server giả trả KP lạ → path theo thứ tự Content; event có `source=content`, lý do `invalid_ordering`.
   - Server giả chậm hơn timeout → path vẫn được tạo, theo thứ tự Content, lý do `llm_timeout`.
   - Catalog rỗng → path theo thứ tự Content, lý do `llm_not_configured`.
2. Chạy lại toàn bộ gate Java và Python: 0 fail, 0 skip.
3. Ghi report vào `reports/`.
4. Viết hướng dẫn để nhóm chạy thử với key Gemini thật:
   - Điền catalog, để `base_url` trống.
   - Khởi động lại API, rồi chạy `deeptutor config show`.
   - Tạo path, rồi xem event `path.ordered` có `source=llm`.

## Success Criteria
- [ ] Compose chạy được cả khi có và khi không có catalog.
- [ ] Container API ghi được `/app/data`; máy host vẫn sửa được catalog.
- [ ] E2E với server giả đạt 4 kịch bản trên.
- [ ] Tài liệu ghi rõ dữ liệu gửi ra ngoài, key nằm ở đâu, và cách tắt tính năng.

## Risk Assessment
- **Quyền ghi thư mục gắn từ host khác nhau giữa Linux và Docker Desktop:** kiểm chứng trên Linux trong E2E, và ghi
  cách xử lý vào README.
- **Catalog của DeepTutor sau này dùng chung cho Tutor:** đúng chủ đích, vì Tutor runtime cũng đọc catalog này. Muốn
  tắt riêng việc sắp xếp thì cần một cờ mới. Chưa làm.
