---
phase: 3
title: "Trace xuyên outbox, RabbitMQ, DLQ"
status: pending
priority: P2
dependencies: [2]
---

# Phase 3: trace xuyên outbox, RabbitMQ, DLQ

## Overview

Event đi HTTP → outbox → relay → RabbitMQ → consumer giữ `traceId` dù đổi thread, retry hay replay DLQ. Người làm: Codex (outbox/DLQ vốn thuộc phạm vi Codex); Claude duyệt và kiểm tay.

Thread relay không có context của request gốc, nên (như `correlation_id` đã làm) phải lưu bền cùng hàng outbox.

## Thiết kế

1. **Ghi:** cột `trace_context` (text, nullable) cạnh `correlation_id` ở 4 bảng outbox (assessment, community, access, `reward_grants` của learning). Chụp W3C `traceparent` (và `tracestate` nếu có) từ span hiện tại tại điểm ghi duy nhất của mỗi service, cùng transaction nghiệp vụ. Dùng `io.micrometer.tracing.propagation.Propagator`; helper dùng chung đặt trong `shared/common-observability`.
2. **Relay:** khôi phục context từ cột (`Propagator.extract`), mở span `outbox.relay` là **con** của span ghi, để nó là span hiện tại khi gọi `RabbitTemplate`. Không đặt MDC tay để giả `traceId`. Đóng span và xóa scope trong `finally`.
3. **Publish/consume:** bật observation của Spring AMQP (`RabbitTemplate.setObservationEnabled(true)` và listener container tương ứng) để `traceparent` đi theo header và consumer tạo span con. ACK, retry, DLQ, idempotency giữ nguyên; `messageId` vẫn là event ID.
4. **Retry và DLQ replay:** republish giữ nguyên `traceparent` và `x-correlation-id` (`DeadLetterReplayer` đang giữ thuộc tính message; test khẳng định, nếu thiếu thì sửa).
5. **Không đổi** payload hay version của event nghiệp vụ.

## Files

- Create: 4 migration thêm `trace_context` (access V9, assessment V22, community V10, learning V42; kiểm số trống trước khi tạo, access V7 dành cho idempotency); helper chụp/khôi phục context trong `shared/common-observability`.
- Modify: điểm ghi outbox và publisher/relay của assessment, community, access, learning; cấu hình RabbitTemplate và listener container (observation); `DeadLetterReplayer` chỉ khi test chứng minh header bị mất.

## Test viết trước

1. HTTP → ghi outbox → commit → relay `@Scheduled` → RabbitMQ thật → consumer: span consumer cùng `traceId` với span request gốc.
2. Như trên nhưng consumer lỗi một lần rồi thành công (retry): vẫn cùng `traceId`.
3. Message vào DLQ rồi replay: header `traceparent` và `x-correlation-id` còn nguyên.
4. Hàng outbox cũ không có `trace_context`: relay vẫn gửi, không lỗi, tạo trace mới.
5. Tracing tắt: cột để NULL, hành vi publish không đổi.

## Success Criteria

- [ ] Các test trên xanh với Testcontainers (Postgres + RabbitMQ thật).
- [ ] Payload và version event nghiệp vụ không đổi.
- [ ] Kiểm tay: một event thật hiện đúng một trace xuyên request → consumer trong Tempo.
- [ ] Chưa đạt thì ghi **PARTIAL**, không ghi DONE.

## Rủi ro

| Rủi ro | Biện pháp |
|---|---|
| Trace mất qua scheduler | Lưu context bền; test xuyên thread |
| Scope/MDC rò sang message kế tiếp | Đóng span và xóa scope trong `finally`; test |
| Migration trùng số với phase idempotency | Kiểm số trống trước khi tạo |
