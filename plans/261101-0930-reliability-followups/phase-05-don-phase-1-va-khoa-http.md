---
phase: 5
title: "Dọn phase 1 cũ và khóa lúc gọi HTTP"
status: pending
priority: P2
effort: "S"
dependencies: []
---

# Phase 5: Dọn phase 1 cũ và khóa lúc gọi HTTP

Người làm: Claude. Làm ngay được, không đụng file của Codex ngoài ba file plan cũ.

## Overview

Phase 1 cũ đã **đo** rằng Rocket và Solo không cần khóa thêm (row lock cộng kiểm trạng thái, có test đồng thời), nhưng văn bản vẫn còn tám chỗ yêu cầu điều ngược lại. Phase này làm văn bản khớp quyết định đã đo, ghi lại cơ chế của từng scheduler, và xử lý một job giữ khóa trong lúc gọi HTTP.

## Hiện trạng (đã kiểm)

21 `@Scheduled` trong 20 job. Cơ chế chống xử lý trùng:

| Service | Job | Cơ chế | Gọi HTTP/LLM khi giữ khóa? |
|---|---|---|---|
| game | `RocketRoundScheduler` | `FOR UPDATE` trận + kiểm trạng thái vòng; test `concurrentSchedulesCloseAnExpiredRoundOnlyOnce` | không |
| game | `SoloTimeoutScheduler` | `FOR UPDATE OF m,p` + kiểm hạn; test `concurrentTimeoutSchedulesCostExactlyOneLife` | không |
| game | `OutboxRetentionJob` | xóa theo ngưỡng, chạy trùng vô hại | không |
| game, notification | `RoomEventChannelSupervisor`, `NoticePushChannelConfig` | việc riêng từng instance (đăng ký lại kênh Redis) | không |
| assessment, community, access | 3 relay outbox | `FOR UPDATE SKIP LOCKED` | gọi broker trong transaction, ngắn |
| assessment | `GradingDeadlineScheduler` | `FOR UPDATE OF j SKIP LOCKED` | cần kiểm (bước 4) |
| assessment | `GateEssayGradingScheduler` | claim `SKIP LOCKED` | **cần kiểm: gọi LLM sau khi đã nhả khóa?** |
| assessment | `SpeakingRecordingRetentionScheduler` | xóa theo ngưỡng | không |
| content | `ScheduledPublishJob` | advisory lock | không |
| content | `AiDraftScheduler` | `FOR UPDATE SKIP LOCKED` | **cần kiểm: gọi LLM sau khi đã nhả khóa?** |
| learning | `LeagueCloseJob`, `InactivityFlagJob`, `SnapshotJob` (×2) | advisory xact lock | không |
| learning | `RewardGrantRelayJob` | advisory xact lock + `SKIP LOCKED` | gọi broker, ngắn |
| learning | `MasteryDecayJob` | advisory lock mức session | cần kiểm không có HTTP |
| learning | `WorkItemSyncJob` | advisory xact lock | **có**: `WorkItemSync` gọi `LearningContentClient` trong transaction |
| user | `LoginHistoryPurgeScheduler` | xóa theo ngưỡng | không |

`WorkItemSyncJob.runOnce` (dòng 50–56) mở transaction, lấy `pg_try_advisory_xact_lock`, rồi `WorkItemSync.sync()` gọi HTTP sang content: giữ một kết nối DB và khóa suốt thời gian chờ content (read 5 s mỗi lời gọi, cộng thêm khi phase 3 thêm retry). Chạy mỗi 3 giờ nên chưa gây sự cố, nhưng là đúng kiểu giữ khóa qua HTTP mà plan cấm.

## Requirements

- Văn bản phase 1 cũ nhất quán với quyết định đã đo.
- Không job nào giữ khóa DB hoặc transaction mở trong lúc gọi HTTP hoặc LLM.
- Các relay gửi message (hậu quả trùng lớn nhất) có test chạy song song; không thêm test song song cho job xóa theo ngưỡng.

## Related Code Files

- Modify: `plans/261031-2145-backend-reliability-patterns/phase-01-nen-nhieu-instance.md`, `phase-02-observability.md`, `phase-03-api-reliability.md`, `phase-05-resilience-va-goi-service.md`, `phase-06-idempotency-va-saga.md`, `phase-08-test-loi-tong.md` (xem bảng "Cập nhật plan cũ" trong [plan.md](./plan.md))
- Modify: `services/learning-service/.../scheduler/WorkItemSyncJob.java`, `.../application/service/WorkItemSync.java`
- Create: test song song cho `OutboxRelay` (assessment) và `CommunityOutboxRelay`
- Modify: `docs/system-architecture.md` (bảng cơ chế ở trên, rút gọn)

## Implementation Steps

### 1. Dọn phase 1 cũ (chỉ tài liệu)

Sửa đúng các chỗ sau trong `phase-01-nen-nhieu-instance.md`:

| Dòng | Hiện nay | Sửa thành |
|---|---|---|
| 12 | "khóa scheduler game theo trận" | bỏ cụm này |
| 30 | Rocket/Solo "đọc không claim → 2 instance cùng xử lý một trận" | "đọc không claim nhưng thao tác ghi khóa dòng và kiểm trạng thái, nên chạy trùng không làm sai dữ liệu" |
| 44 | Solo "chưa có test đồng thời" | trỏ tới `concurrentTimeoutSchedulesCostExactlyOneLife` |
| 56, 59 | `RoomEventBus`, `RedisRoomEventBus`, `PushBus`, `RedisPushBus` | tên đã triển khai: `RoomEventBridge`, `RoomEventCodec`, `NoticePushBridge` |
| 58 | liệt kê `RocketMatchStoreAdapter`, `SoloGameplayRepositoryAdapter`, hai scheduler cần sửa | xóa dòng; không sửa các file này |
| 61 | "Modify (nếu thiếu claim): …assessment, content, user" | xóa |
| 68 | "9 job chưa có khóa… Rocket và Solo phải đỏ trước khi sửa" | "Rocket và Solo đã có test đồng thời xanh (bằng chứng không cần khóa); các relay: xem phase 5 mới" |
| 77 | "game bắt buộc claim theo trận" | xóa |
| 83 | "Test song song của 9 job xanh" | "test đồng thời Rocket, Solo và hai relay xanh" |
| 93 | rủi ro advisory lock giữ quá lâu | giữ, đổi thành "chỉ dùng cho job không gọi HTTP; xem bảng cơ chế" |

Cộng thêm bảng cơ chế ở trên vào phần "Architecture / Scheduler". Đổi `status` của phase 1 cũ khi các tiêu chí còn lại đạt.

### 2. Test viết trước (phải đỏ)

- `WorkItemSyncJobTest`: `doesNotHoldTheAdvisoryLockWhileCallingContent` (client content giả chặn bằng latch; trong lúc chặn, một kết nối khác `pg_try_advisory_lock(hashtext('learning:work-item-sync'))` trả `true`); `appliesOnlyOneRoundWhenTwoInstancesRace` (hai luồng, mỗi mục thêm đúng một lần).
- Assessment `OutboxRelayConcurrencyTest` và community `CommunityOutboxRelayConcurrencyTest`: 20 hàng chờ, hai luồng gọi relay đồng thời, mỗi hàng gửi đúng một lần (theo mẫu `twoRelaysRunningAtOnceSendEachEventOnce` của access).

### 3. Code

- Tách `WorkItemSync.sync()` thành hai bước: `loadFromContent()` (HTTP, chỉ đọc, ngoài transaction) và `apply(loaded)` (ghi DB). `WorkItemSyncJob.runOnce` gọi bước đọc trước, rồi mở transaction, lấy `pg_try_advisory_xact_lock`, chạy bước ghi. Hai instance cùng đọc content là vô hại (chỉ đọc); chỉ một instance ghi.

### 4. Kiểm các ô "cần kiểm"

- Đọc `GateEssayGradingScheduler` + `JdbcGateEssayJobStore`, `AiDraftScheduler` + `JdbcAiDraftRepository`, `GradingDeadlineScheduler`, `MasteryDecayJob`. Với mỗi cái ghi vào bảng: có gọi HTTP/LLM khi còn giữ khóa hoặc transaction không. Chỗ nào có: ghi báo cáo và tách như bước 3 nếu rủi ro thật; chỗ không: điền ô "không".

## Success Criteria

- [ ] Phase 1 cũ không còn câu mâu thuẫn (xem lại toàn file, `grep "claim\|đỏ trước\|9 job"`).
- [ ] `WorkItemSyncJobTest` xanh, test cũ của learning xanh.
- [ ] Hai test relay song song xanh.
- [ ] Bảng cơ chế không còn ô "cần kiểm".

## Risk Assessment

- **Tách bước đọc làm hai instance cùng gọi content:** chấp nhận, tải thấp (3 giờ/lần).
- **`WorkItemSync` đọc dữ liệu nhiều lần giữa đọc và ghi:** kết quả đọc dùng để thêm mục còn thiếu; nếu mục đã được thêm giữa chừng, câu ghi phải bỏ qua mục đã có (đã có ràng buộc duy nhất hoặc `ON CONFLICT` — kiểm trước khi tách, thêm nếu thiếu).
- **Rollback:** khôi phục `runOnce` về một transaction; thay đổi chỉ trong hai file.
