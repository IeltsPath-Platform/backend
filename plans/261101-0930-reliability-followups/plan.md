---
title: "Reliability follow-ups: correlation qua outbox, auth rate limit, resilience, draining"
description: "Xử lý 5 lỗi thiết kế phát hiện khi rà plan reliability: correlation id đứt ở outbox, route rate limit sai path và XFF, thứ tự exception/retry/breaker của Resilience4j, phase 1 tự mâu thuẫn về khóa scheduler, thiếu graceful shutdown và draining."
status: completed
priority: P2
branch: "feat/reliability-patterns"
tags: [reliability, observability, resilience, shutdown, follow-up]
blockedBy: []
blocks: []
related: [261031-2145-backend-reliability-patterns]
created: "2026-11-01T02:30:00.000Z"
createdBy: "ck:plan"
source: skill
mode: tdd
---

# Reliability follow-ups

## Overview

Plan cũ [261031-2145-backend-reliability-patterns](../261031-2145-backend-reliability-patterns/plan.md) đã làm xong nền multi-instance, observability, outbox access. Khi rà lại bằng code thật, 5 điểm thiết kế còn sai hoặc thiếu. Plan này sửa đúng 5 điểm đó, **không thêm hạ tầng mới** (không ShedLock, không service mesh, không Kubernetes).

Nguyên tắc để kế hoạch gọn và đúng chỗ:

1. **Sửa tại một điểm duy nhất** thay vì sửa từng nơi ghi (ví dụ lấy correlation id ở adapter lưu outbox, không ở từng use case).
2. **Giữ nguyên chỗ đang đúng.** Resilience bọc lời gọi thô và để khối `catch` hiện có của client tiếp tục dịch lỗi, thay vì viết lại mọi client.
3. **Chỉ hứa điều Docker Compose bảo đảm được.** Draining giảm cửa sổ lỗi, không hứa zero-downtime.
4. **Đo trước khi thêm khóa hay `@Version`.** Phase cũ đã làm vậy cho game; phase 5 mới hoàn tất việc đó cho văn bản plan.

## Từ lỗi đến phase

| # | Lỗi phát hiện | Phase mới | Phase cũ bị ảnh hưởng |
|---|---|---|---|
| 1 | Correlation id đứt ở outbox: không lưu, relay chạy thread khác không có MDC | [Phase 1](./phase-01-correlation-qua-outbox.md) | 2 (tiêu chí event→listener), 4 |
| 2 | Plan và code dùng `/api/auth/*` (không tồn tại), thiếu `reset-password`, XFF đọc từ trái | [Phase 2](./phase-02-chinh-phase-3-auth-va-xff.md) | 3 |
| 3 | Resilience bọc sai chỗ so với `catch` của client, retry nằm trong breaker, POST truy vấn bị coi là ghi, timeout gateway va chạm client | [Phase 3](./phase-03-thiet-ke-lai-resilience.md) | **5 được thay thế**, 6 |
| 4 | Phase 1 cũ tự mâu thuẫn về khóa scheduler; một job giữ khóa lúc gọi HTTP | [Phase 5](./phase-05-don-phase-1-va-khoa-http.md) | 1, 8 |
| 5 | Chưa cấu hình rõ graceful shutdown, `stop_grace_period`, độ trễ Eureka; không có test dừng một replica | [Phase 4](./phase-04-graceful-shutdown-va-draining.md) | 8 |

## Phases

| Phase | Tên | Người làm | Bắt đầu khi | Status |
|---|---|---|---|---|
| 1 | [Correlation qua outbox](./phase-01-correlation-qua-outbox.md) | Claude | Ngay (không đụng file của Codex) | Pending |
| 2 | [Chỉnh phase 3: auth route và XFF](./phase-02-chinh-phase-3-auth-va-xff.md) | Codex (commit sửa sau `e5161ba3`) | Ngay; phase 3 cũ chưa qua cổng cho đến khi xong | Pending |
| 3 | [Thiết kế lại resilience](./phase-03-thiet-ke-lai-resilience.md) | Codex | Phase 3 cũ qua cổng duyệt | Pending |
| 4 | [Graceful shutdown và draining](./phase-04-graceful-shutdown-va-draining.md) | Claude | Phase 3 cũ qua cổng (tránh xung đột compose, config) | Pending |
| 5 | [Dọn phase 1 cũ và khóa lúc gọi HTTP](./phase-05-don-phase-1-va-khoa-http.md) | Claude | Ngay | Pending |

Thứ tự thực tế: phase 1, 2, 5 chạy ngay song song (file không chồng nhau); phase 3, 4 sau khi phase 3 cũ của Codex qua cổng; phase 6 và 8 cũ chạy cuối.

## Quyết định đã chốt

- Phase 5 cũ **không giao cho Codex nữa**; phase 3 mới thay thế. Mục "Contract test" của phase 5 cũ giữ nguyên và được phase 3 mới tham chiếu.
- Số lần thử: READ và ghi idempotent tối đa **2 lần** (1 lần retry, 200 ms), thay cho 3 lần của bản cũ, để `lần thử × read timeout + backoff` luôn nhỏ hơn timeout gateway.
- Compose giữ healthcheck `/actuator/health` (kiểm DB) để chặn thứ tự khởi động; không đổi sang readiness.
- Migration: access dùng **V8** cho cột correlation vì V7 đã dành cho idempotency của phase 6 cũ.

## Điều kiện nhánh (cần bạn quyết khi đến bước tích hợp)

Các sửa lỗi sau đang nằm ở nhánh phụ, chưa vào `feat/reliability-patterns`: fix listener correlation `5221e81c`, phase 4 (`6b539bf4`…`e30674e0`), phase 7 (`9e8aa3d4`…`32d3886d`). Phase 1 mới của plan này cần `5221e81c`.

## Cập nhật plan cũ (làm trong phase 5 mới)

| File cũ | Sửa |
|---|---|
| `phase-01-nen-nhieu-instance.md` | Bỏ 8 câu mâu thuẫn, thêm bảng cơ chế của 21 `@Scheduled` (chi tiết ở phase 5) |
| `phase-02-observability.md` | Tiêu chí "event assessment → learning listener" trỏ sang phase 1 mới; bước 3 test Rabbit ghi rõ chỉ phủ publish trực tiếp |
| `phase-03-api-reliability.md` | Dòng 23, 41, 99, 146: `/api/auth/*` → `/auth/*`, thêm `reset-password`, sửa mô tả XFF |
| `phase-05-resilience-va-goi-service.md` | Đánh dấu `status: superseded`, trỏ sang phase 3 mới; giữ mục Contract test |
| `phase-06-idempotency-va-saga.md` | `dependencies: [2, 3]` theo phase 3 mới |
| `phase-08-test-loi-tong.md` | Thêm kịch bản: correlation HTTP→outbox→consumer, dừng một replica, danh sách endpoint auth |

## Tiêu chí hoàn thành toàn plan

- [ ] Một request có `X-Correlation-Id: t-1` đi qua gateway, ghi outbox, relay gửi, consumer xử lý: log consumer có `t-1`.
- [ ] Mọi endpoint auth công khai (`login`, `refresh`, `forgot-password`, `reset-password`, `register`) rơi vào tier `auth`; test chặn lệch giữa cấu hình và code.
- [ ] Breaker mở khi downstream 5xx hoặc timeout, không mở vì 404; client vẫn ánh xạ 404 thành `NOT_FOUND`.
- [ ] Văn bản phase 1 cũ không còn câu mâu thuẫn; không job nào giữ khóa DB lúc gọi HTTP.
- [ ] Dừng một replica khi replica khác chạy: request đang xử lý hoàn tất, cửa sổ lỗi do định tuyến cũ ≤ 15 giây, đã đo và ghi lại.

## Câu hỏi còn mở

- Timeout route của các endpoint chấm LLM (learning 20 s, assessment 45 s, content có retry bận 5 s + 15 s) cần đo thật ở phase 3 trước khi chốt số 60/120/180 giây.
- Có đưa phase 1 mới thành điều kiện bắt buộc trước khi gộp PR không? Đề xuất: có, vì nếu không phase 2 cũ hứa điều không đạt được.

## Cách thực hiện (cập nhật 2026-11-01)

Plan này là **spec chi tiết đã đính chính** cho các gói A–E trong [work-split của plan 261031-2145](../261031-2145-backend-reliability-patterns/work-split-claude-codex.md#chạy-một-mạch-các-gói-còn-lại-cho-codex). Codex làm các gói A→B→C→D→E một mạch; Claude duyệt hàng loạt và làm kiểm tay. Ánh xạ: phase 1 → gói B2, phase 2 → gói A, phase 3 → gói C, phase 4 → gói E, phase 5 → gói B1. Cột "Người làm" ở bảng trên là phân công ban đầu, nay thay bằng ánh xạ này.

## Đóng plan (2026-11-02)

Trạng thái: **đã implement vào các gói A–E của plan 2145; còn tồn đọng đã biết** giống plan 2145 (kiểm tay trên stack thật và hai giới hạn của gói E).