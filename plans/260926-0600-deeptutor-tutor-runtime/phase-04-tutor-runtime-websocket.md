---
phase: 4
title: "Tutor runtime và WebSocket trong AI Learning"
status: pending
priority: P1
dependencies: [3]
effort: "~2d"
---

# Phase 4: Tutor runtime và WebSocket trong AI Learning

## Overview
Dựng `ApplicationContainer` của DeepTutor bên trong FastAPI của AI Learning. Container chỉ bật các capability của plan.
Mở endpoint WebSocket nói **đúng giao thức `unified_ws` của DeepTutor**, với xác thực bằng internal JWT của IELTSPath.
Kèm một số REST tối thiểu để liệt kê và xem session.

## Đọc trước khi code
| File (trong `third_party/deeptutor/deeptutor`) | Để làm gì |
| --- | --- |
| `api/routers/unified_ws.py` | Luồng: auth → accept → container → subscribe turn → commands |
| `api/routers/auth.py` (`ws_require_auth`) | DeepTutor xác thực WS thế nào; cần thay bằng internal JWT |
| `api/contracts/turn_protocol.py` | Message client ↔ server (`start_turn`, `answer`, `cancel`, `subscribe`…) |
| `app/container.py` (`ApplicationContainer.build`, `start`) | Cách chọn capability và coordination |
| `runtime/capability_catalog.py`, `runtime/registry` | Bật hoặc tắt capability, tool (web search, code exec, KB) |
| `api/routers/sessions.py` | REST session mà frontend DeepTutor dùng |

## Requirements
- Functional:
  - `app/deeptutor_runtime/container.py`: dựng container khi app khởi động (`lifespan`), gán vào
    `app.state.application_container`.
    - Loop capability giữ lại (B4): `mastery`, `ask_questions`, `immersive_reading`. Các capability khác trong
      `BUILTIN_LOOP_CAPABILITY_SPECS` đều bị loại, kể cả `explore_context` (T4).
    - Turn capability: chỉ những gì mastery, question và reading cần (pha 1 liệt kê). Không `deep_research`,
      `deep_solve`, `math_animator`.
    - Tool của chat: tắt web search, code execution, knowledge base, MCP. Kiểm tra settings `tools` của DeepTutor
      (`deeptutor config show` đang in `"tools": []`) và test rằng không tool nào trong số này được đưa cho LLM.
    - Danh sách bật/tắt là **allowlist** trong code, có test.
  - Endpoint `GET /api/ai-learning/tutor/ws` (WebSocket):
    - Endpoint gọi thẳng `deeptutor.api.routers.unified_ws.unified_websocket(ws)`. Xác thực nằm trong
      `ws_require_auth` đã được thay (B3): đọc internal JWT từ header `Authorization` của handshake; không hợp lệ thì
      đóng 4001 **trước** `accept`; hợp lệ thì cài `CurrentUser` role `user`.
    - **Test bắt buộc:** trong turn, `get_current_user().is_admin` là `False`, và `id` là UUID của học viên. Không bao
      giờ là `local-admin`.
  - REST (qua `api/routers/sessions.py` của DeepTutor nếu dùng được với adapter, hoặc bọc mỏng):
    - `GET /api/ai-learning/tutor/sessions`: session của học viên;
    - `GET /api/ai-learning/tutor/sessions/{id}`: message và turn;
    - `DELETE /api/ai-learning/tutor/sessions/{id}`: soft delete.
  - Session mới luôn được gắn vào **path active** của học viên (`bind_session`). Học viên không có goal active → lỗi
    409, như `/progress`.
  - Consumer không dựng container và không mở WebSocket.
- Non-functional:
  - Một instance: coordination `local` của DeepTutor.
  - Timeout idle và giới hạn kích thước message (theo `ws_max_size` của DeepTutor).
  - Log chỉ ghi `session_id`, `turn_id`, loại lệnh và mã lỗi.

## Implementation Steps
### Tests Before
1. Test WebSocket bằng `TestClient.websocket_connect`, LLM giả:
   - thiếu hoặc sai token → 4401;
   - `start_turn` capability `mastery` mode `study` → nhận chuỗi event, rồi câu hỏi (`mastery_quiz`);
   - gửi câu trả lời → nhận kết quả chấm; `/status` đổi.
2. Allowlist: gọi tool bị tắt (web search) → DeepTutor báo tool không có; không có request mạng nào ra ngoài LLM giả.
3. Học viên A không subscribe được turn của học viên B.
### Refactor
4. Container, endpoint, REST.
### Tests After
5. Gate đầy đủ.

## Success Criteria
- [ ] Một buổi học study hoàn chỉnh chạy qua WebSocket, bằng giao thức gốc của DeepTutor.
- [ ] Chỉ capability trong allowlist chạy được.
