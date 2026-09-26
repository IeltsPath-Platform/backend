---
phase: 7
title: "Compose, E2E qua Gateway, tài liệu và spec"
status: pending
priority: P1
dependencies: [6]
effort: "~1.5d"
---

# Phase 7: Compose, E2E qua Gateway, tài liệu và spec

## Overview
Chạy toàn bộ luồng qua Gateway bằng compose với LLM giả; viết tài liệu; soạn bản sửa spec của nhóm để duyệt.

## Requirements
- **Compose**:
  - `ai-learning-api` có biến `AI_LEARNING_LLM_*` (pha 3), không volume dữ liệu.
  - Service `llm-stub` (profile `llm-stub`) hỗ trợ mode `script` qua biến `LLM_STUB_SCRIPT` và file kịch bản mount chỉ đọc.
- **E2E qua Gateway** (`tests/e2e/tutor_e2e.py`, chạy tay theo README, dùng `httpx` stream với external JWT thật từ
  `/auth/login`):
  1. Học viên có goal active → `POST /tutor/sessions` → turn 1 nhận `question` qua SSE.
  2. Turn 2 trả lời → `grading`; `GET /api/ai-learning/status` đổi.
  3. **SSE không bị gom**: sự kiện `turn.started` tới client trước `turn.completed` ít nhất một khoảng thời gian (stub
     có độ trễ giữa các phản hồi). Keep-alive giữ kết nối qua một turn dài hơn 30 giây.
  4. Một kết quả thi chính thức (RabbitMQ) áp vào path giữa hai turn → không mất evidence.
  5. Học viên B không đọc được session của A qua Gateway.
  6. Không file nào được ghi dưới thư mục service trong container (`docker compose exec ai-learning-api find /app -newer …`
     chỉ ra file log nếu có).
  - Nếu Gateway gom buffer hoặc cắt kết nối: sửa cấu hình route (ví dụ `response-timeout`) trong
    `infra/config-server/config-repo/api-gateway.yaml`, kèm test Java nếu đổi code; không đổi cơ chế xác thực.
- **Tài liệu**:
  - README service, mục "Tutor": endpoint, sự kiện SSE, cách đọc stream bằng `fetch` (vì `EventSource` không gửi được
    header `Authorization`), cấu hình LLM, dữ liệu gửi tới LLM, giới hạn (một instance).
  - `docs/contracts/tutor-sse-v1.md`: sự kiện và payload, có ví dụ.
  - README root: cách chạy E2E tutor; ghi rõ `third_party/deeptutor` **chỉ để tham khảo**, build và test không dùng.
- **Spec (cần nhóm duyệt, commit riêng, không tự merge)**:
  - `.sdd/specs/ai-learning-phase1-plan.md` (dòng 18, 28, 35–36, 133–136), `SERVICE_ARCHITECTURE_V2.md` (78, 332),
    `FEATURE_TREE_V2.md` (11): đổi "DeepTutor là engine / cài từ submodule" thành "engine mastery là bản port của DeepTutor
    v1.6.9 trong `app/mastery`, không có logic adaptive nào khác; service không phụ thuộc DeepTutor lúc chạy".
  - `.sdd/database/DATABASE_V5.md` §7: `sessions`, `messages`, `turns` theo pha 4; đánh dấu `turn_events`,
    `mastery_path_sessions`, `mastery_path_leases`, `notebook_entries`, `practice_review_*` là chưa dùng.

## Implementation Steps
1. Compose và stub script.
2. Script E2E; chạy đủ 6 kịch bản qua Gateway; lưu kết quả vào `reports/` (không token, không nội dung hội thoại đầy đủ).
3. Tài liệu, hợp đồng SSE.
4. Bản sửa spec (commit riêng).

## Success Criteria
- [ ] 6 kịch bản E2E pass qua Gateway với LLM giả.
- [ ] Gate Python 0 fail, 0 skip, không có `third_party` trên `PYTHONPATH`.
- [ ] Nhóm chạy được tutor với Gemini thật chỉ bằng biến `AI_LEARNING_LLM_*` theo README.
- [ ] Bản sửa spec đã gửi nhóm duyệt.
