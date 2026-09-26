---
phase: 5
title: "Gateway WebSocket và xác thực"
status: pending
priority: P1
dependencies: [4]
effort: "~1d"
---

# Phase 5: Gateway WebSocket và xác thực

## Overview
Cho WebSocket của Tutor đi qua Gateway như mọi API khác:
1. Gateway xác thực external JWT.
2. Gateway ký internal JWT.
3. Gateway chuyển kết nối tới AI Learning.

Trình duyệt không đặt được header `Authorization` khi mở WebSocket, nên cần một cách truyền token khác.

## Đọc trước khi code
- `infra/api-gateway/src/main/java/com/group01/apigateway/filter/InternalJwtGatewayFilter.java` và security config của Gateway.
- `infra/config-server/config-repo/api-gateway.yaml` (route `ai-learning-service`, CORS, public endpoints).
- `shared/common-security` (claim của internal JWT) và `app/security/internal_jwt.py` của AI Learning.

## Requirements
- Functional:
  - Route WebSocket `/api/ai-learning/tutor/ws` → AI Learning. Spring Cloud Gateway hỗ trợ `ws://`; giữ URI theo cấu
    hình hiện có (`AI_LEARNING_SERVICE_URI`).
  - Client gửi external JWT qua **subprotocol**: `Sec-WebSocket-Protocol: ieltspath.bearer.<token>`. Không dùng query
    string, vì token trong URL dễ lọt vào log.
  - Gateway:
    1. Đọc token từ subprotocol và xác thực như header `Authorization`.
    2. Ký internal JWT.
    3. Gửi internal JWT sang AI Learning qua header `Authorization` của handshake phía sau.
    4. Bỏ token gốc khỏi subprotocol chuyển tiếp, và trả lại cho client subprotocol `ieltspath.bearer` (không kèm token).
  - AI Learning chỉ chấp nhận internal JWT, như mọi endpoint khác.
  - Log Gateway không ghi token hay subprotocol đầy đủ.
- Non-functional: không đổi hành vi các route HTTP hiện có.

## Implementation Steps
### Tests Before
1. Test Gateway (WebFlux):
   - handshake có subprotocol hợp lệ → upstream nhận `Authorization: Bearer <internal JWT>`;
   - không có hoặc sai token → 401 trước khi upgrade;
   - token không xuất hiện trong log.
2. Test MVC hiện có của các service không đổi.
### Refactor
3. Filter, route, cấu hình CORS/origin cho WebSocket.
### Tests After
4. Gate Java đầy đủ; kết nối thật qua Gateway trong E2E (pha 12).

## Success Criteria
- [ ] Trình duyệt mở được WebSocket Tutor qua Gateway chỉ với external JWT.
- [ ] AI Learning không bao giờ thấy external JWT.
