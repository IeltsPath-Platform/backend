---
phase: 2
title: System architecture doc
status: completed
priority: P2
dependencies:
  - 1
effort: ~0.5d
---

# Phase 2: System architecture doc

## Overview
Tạo `docs/system-architecture.md`: nơi đọc-khi-cần cho kiến trúc toàn hệ thống, nhận nội dung mô tả từ CLAUDE.md
(§4–12, §14) và AGENTS.md (§1, §3.1–3.2 phần mô tả), cập nhật đủ 9 service theo fact sheet.

## Requirements
- Đủ 9 business service + 3 infra + `common-security`; AI Learning (Python) và `third_party/deeptutor` (chỉ tham khảo).
- Mọi dữ kiện lấy từ fact sheet; ≤ 800 dòng; tiếng Việt.
- Không chứa quy tắc bắt buộc (quy tắc ở AGENTS.md) — chỉ mô tả "hệ thống là gì, chạy thế nào, vì sao".

## Architecture
Mục lục đề xuất:
1. Tổng quan và sơ đồ (text/mermaid): Client → Gateway → service; Config Server, Eureka; RabbitMQ; DB từng service.
2. Bảng service: bounded context, DB, cổng, route Gateway, trạng thái (đang chạy / skeleton).
3. Giao tiếp: HTTP nội bộ (bảng caller → callee → endpoint), async (`AssessmentCompleted.v2`: outbox → relay → exchange →
   consumer, retry/DLQ); outbox chưa relay ở access/content/game. Link `docs/contracts/`.
4. Security model: external JWT, Gateway ký internal JWT, `common-security`, bản Python `app/security/internal_jwt.py`.
5. Kiến trúc trong service: Clean/DDD layer của service Java (biến thể `application/port`); cấu trúc `app/` của ai-learning.
6. Flow chính: đăng nhập/refresh; request protected; assessment → mastery path; tutor turn SSE (+ hạn mức ngày).
7. Data & persistence: DB ownership, Flyway (Java trong service, ai-learning chạy Flyway container), link `.sdd/database/DATABASE_V5.md`.
8. Quyết định kiến trúc (từ §11 cũ + mới: AI Learning viết Python ngoài Maven/Eureka; port mastery từ DeepTutor thay vì
   phụ thuộc; outbox + RabbitMQ cho assessment).
9. Pattern đang dùng (§12 cũ, cập nhật) và bài học kỹ thuật (§14 cũ).
10. Điểm chưa nhất quán đã biết (từ fact sheet).

## Related Code Files
- Create: `docs/system-architecture.md`
- Read: fact sheet, `CLAUDE.md`, `AGENTS.md`, `README.md`, `services/*/README.md`, `docs/contracts/*`

## Implementation Steps
1. Dựng mục lục theo Architecture; chép nội dung còn đúng từ CLAUDE.md §4–12, §14 và AGENTS.md phần mô tả.
2. Sửa/bổ sung theo fact sheet (service mới, messaging, AI Learning).
3. Ghi đầu file: "Cập nhật lần cuối: <ngày>, commit <sha>".
4. Tự kiểm: mỗi cổng/route/DB/tên biến trong doc có trong fact sheet; `grep` không có giá trị secret.
5. Commit: `docs: describe the current system architecture across all services`.

## Success Criteria
- [x] Doc phủ đủ service và 10 mục; ≤ 800 dòng.
- [x] Mọi dữ kiện khớp fact sheet; 0 secret.
- [x] Mọi nội dung mô tả sẽ bị cắt khỏi CLAUDE.md/AGENTS.md ở phase 3–4 đã có mặt ở đây.

## Kết quả (2026-09-27)
- `docs/system-architecture.md`: 231 dòng, 11 mục (thêm mục "Chạy local" tóm tắt); secret scan sạch; đủ 16 cổng.
- Kiểm thêm ngoài fact sheet (đã ghi vào fact sheet §10): role chuẩn `CUSTOMER` (V3 đổi từ `LEARNER`), assessment/game
  forward bearer + correlation id, WebSocket dùng ticket.
- Phát hiện: `services/ai-learning-service/README.md` tự mâu thuẫn về event chưa có path (dòng ~472 đúng, ~500 sai) —
  chỉ ghi vào mục "Điểm chưa nhất quán", không sửa README (ngoài phạm vi).

## Risk Assessment
- Doc dài và trùng README root: chấp nhận ở đợt này (README ngoài phạm vi); doc tham chiếu README cho hướng dẫn chạy chi tiết.
