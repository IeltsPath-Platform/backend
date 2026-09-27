---
phase: 3
title: "API tutor HTTP + SSE"
status: complete
priority: P1
dependencies: [2]
effort: "~1.5d"
---

# Phase 3: API tutor HTTP + SSE

## Overview
Mở tutor ra ngoài bằng REST, và một endpoint `POST` trả `text/event-stream` cho mỗi lượt học. Xác thực như mọi API khác
của AI Learning: Gateway kiểm external JWT, ký internal JWT, service đọc bằng `require_current_user`. Path luôn là path
active của học viên, do server xác định.

## Đọc trước
- `main.py`: mẫu endpoint, `require_current_user`, `get_path_service`, exception handler (**lưu ý**: `ValueError` đang
  được map thành 502, nên lỗi đầu vào của tutor phải đi qua model Pydantic (422), không ném `ValueError`).
- `app/application/path_service.py`: lấy path active theo goal active (`ActiveGoalRequired` → 409).
- `app/api/dto/responses.py`: kiểu response (alias camelCase).

## Requirements
- Endpoint (prefix `/api/ai-learning/tutor`), mọi endpoint yêu cầu học viên đã xác thực:

  | Method, path | Hành vi |
  | --- | --- |
  | `POST /sessions` | Tạo session trên path active (tạo path nếu chưa có, như `POST /paths`). Không có goal active → 409 như `/progress` |
  | `GET /sessions` | Session chưa archive của học viên, mới nhất trước |
  | `GET /sessions/{id}` | Message và câu hỏi đang chờ (bản công khai). Không thuộc học viên → 404 |
  | `DELETE /sessions/{id}` | Archive (soft delete) → 204 |
  | `POST /sessions/{id}/turns` | Body `{ "message"?: string ≤ 4000, "answer"?: { "questionId": uuid, "text": string ≤ 2000 } }`, cần đúng một trong hai. Trả `text/event-stream` |

- **Kiểm tra trước khi stream** (lỗi trả JSON với status code bình thường): session không thuộc học viên → 404; turn
  khác đang chạy → 409 (`ActiveTurnConflict`); body sai → 422.
- **SSE** (`app/api/tutor_sse.py`):
  - Mỗi `TutorEvent` thành `event: <type>` và `data: <json camelCase>`, cách nhau bằng dòng trống.
  - Comment `: keep-alive` mỗi 15 giây khi chưa có sự kiện.
  - Header `Cache-Control: no-cache`, `X-Accel-Buffering: no`.
- **Client ngắt kết nối**: turn vẫn chạy tới cuối phía server để state nhất quán; client đọc lại kết quả bằng
  `GET /sessions/{id}`.
- Khởi động API (`lifespan`): gọi `recover_interrupted_turns()` (pha 1). Consumer không gọi.
- Hợp đồng sự kiện ghi ở `docs/contracts/tutor-sse-v1.md` (pha 4 hoàn thiện ví dụ).
- Gateway: route `ai-learning-service` (`Path=/api/ai-learning/**`) đã phủ. Không đổi cơ chế xác thực. Pha 7 kiểm SSE đi
  qua Gateway không bị gom buffer và không bị timeout giữa chừng.

## Implementation Steps
### Tests Before (`TestClient`, internal JWT như các test API hiện có, stub script)
1. Thiếu hoặc sai token → 401; session của học viên khác → 404 ở mọi endpoint.
2. `POST /sessions` không có goal active → 409.
3. Một chu kỳ qua HTTP: turn 1 stream có `turn.started` → `question` → `turn.completed`; `GET /sessions/{id}` có câu hỏi
   chờ; turn 2 với `answer` stream có `grading`; `GET /api/ai-learning/status` đổi.
4. Hai turn đồng thời trên một session → một cái 409 trước khi stream.
5. Body có cả `message` và `answer`, hoặc không có cái nào → 422.
6. Stream không chứa `expectedAnswer`, prompt hay key; log không chứa nội dung message.
### Refactor
7. DTO, endpoint, SSE, lifespan.
### Tests After
8. Gate đầy đủ.

## Success Criteria
- [x] Học viên học một KP qua HTTP + SSE, chỉ với internal JWT như mọi API khác.
- [x] Không endpoint nào nhận path id từ client.
