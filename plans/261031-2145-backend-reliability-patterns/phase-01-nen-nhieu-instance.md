---
phase: 1
title: "Nen nhieu instance"
status: pending
priority: P1
effort: "L"
dependencies: []
---

# Phase 1: Nền nhiều instance

<!-- Các cơ chế triển khai được đối chiếu lại trong bảng scheduler bên dưới; kiểm tay hai instance vẫn do Claude thực hiện. -->

## Overview

Cho phép chạy 2+ instance mỗi service mà không chạy job trùng, không mất push thông báo, không vỡ phòng game. Thêm Redis làm store chung (vé game, kênh phát chung, rate limit ở phase 3).

## Requirements

- Redis 7 trong `docker-compose.yml`, healthcheck, cổng host chỉ `127.0.0.1` (nếu mở).
- 21 phương thức `@Scheduled` trong 20 job: cơ chế chống xử lý trùng được ghi rõ theo từng job.
- notification-service: push STOMP tới người dùng dù họ nối vào instance khác.
- game-service: mọi người trong phòng nhận event phòng dù nối vào instance khác, kể cả event do scheduler sinh ra.
- Không đổi hợp đồng WebSocket phía frontend (`/ws/notifications`, `/ws/games`, destination `/user/queue/notifications`, định dạng message game).

## Hiện trạng (đã kiểm)

- Game: một endpoint `/ws/games` (`GameWebSocketConfig.java:28`); phòng gắn qua vé (`InMemoryWebSocketTicketStore.issue(roomId, userId)`); session giữ trong `sessionsByRoom` (`GameWebSocketHandler.java:262`); event phòng là `@TransactionalEventListener` trong tiến trình (`GameWebSocketHandler.java:66` và 11 handler khác), phát từ REST use case và từ scheduler.
- Notification: `enableSimpleBroker("/queue")` (`NotificationMessagingConfig.java:50`); listener RabbitMQ gọi `convertAndSendToUser` trên instance đang consume.
- Scheduler game đọc mục đến hạn không claim; ghi được khóa theo trận và kiểm tra trạng thái nên lần chạy trùng không áp dụng cùng thay đổi hai lần.
- Relay assessment, community và store chấm LLM (`JdbcGateEssayJobStore`) đã claim bằng `FOR UPDATE SKIP LOCKED`.

## Architecture

- **Kênh phát chung (Redis pub/sub)** cho WebSocket:
  - Game: các `@TransactionalEventListener` hiện tại không gửi thẳng tới session nữa mà publish một message (`roomId`, loại, payload JSON đã dựng sẵn, danh sách người nhận nếu là gửi riêng) lên kênh `game.room-events`. Mỗi instance subscribe kênh, gửi cho session cục bộ trong `sessionsByRoom`. Logic game không đổi.
  - Notification: sau khi lưu thông báo, publish (`recipientId`, payload) lên kênh `notification.push`; mỗi instance subscribe và gọi `convertAndSendToUser` cục bộ (simple broker giữ nguyên; người không nối vào instance đó thì bỏ qua).
  - Pub/sub không bền: instance rớt thì mất push lúc đó; chấp nhận vì REST là nguồn sự thật (đã có đồng bộ lại). Ghi rõ trong tài liệu.
  - Lớp nhỏ dùng chung giữa hai service (publisher + listener container + mã hóa JSON): đặt trong mỗi service (2 chỗ, mã ngắn); chỉ đưa vào `shared/` nếu service thứ ba cần.
- **Vé game:** `InMemoryWebSocketTicketStore` → `RedisWebSocketTicketStore` (TTL, `GETDEL` để dùng một lần), vì vé phát qua REST ở instance A có thể được dùng ở instance B.
- **Scheduler:** 21 lịch chạy trong 20 job; không thêm ShedLock hay advisory lock khi không cần.

| Service | Job | Cơ chế chống trùng | HTTP/LLM trong transaction/khóa |
|---|---|---|---|
| game | `RocketRoundScheduler` | `FOR UPDATE` trận + kiểm trạng thái vòng; lịch trùng chỉ settle một lần | Không |
| game | `SoloTimeoutScheduler` | `FOR UPDATE OF m,p` + kiểm deadline; chỉ trừ mạng một lần | Không |
| game | `OutboxRetentionJob` | Xóa theo cutoff, lặp an toàn | Không |
| game | `RoomEventChannelSupervisor` | Mỗi instance tự subscribe Redis pub/sub | Không |
| notification | `NoticePushChannelConfig` | Mỗi instance tự subscribe Redis pub/sub | Không |
| assessment | `OutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED` | Broker trong transaction ngắn |
| access | `AccessOutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED` | Broker trong transaction ngắn |
| community | `CommunityOutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED` | Broker trong transaction ngắn |
| assessment | `GateEssayGradingScheduler` | Claim bằng `FOR UPDATE SKIP LOCKED`; hoàn tất trong transaction riêng | LLM ngoài transaction |
| assessment | `GradingDeadlineScheduler` | Store claim hàng bằng `FOR UPDATE SKIP LOCKED` | Không |
| assessment | `SpeakingRecordingRetentionScheduler` | Xóa theo cutoff, lặp an toàn | Không |
| content | `ScheduledPublishJob` | Advisory lock theo đợt publish | Không |
| content | `AiDraftScheduler` | Claim bằng `FOR UPDATE SKIP LOCKED`, commit trước khi gọi generator | Generator ngoài transaction |
| learning | `LeagueCloseJob` | Advisory transaction lock | Không |
| learning | `InactivityFlagJob` | Advisory transaction lock | Không |
| learning | `SnapshotJob` (weekly) | Advisory transaction lock theo kỳ | Không |
| learning | `SnapshotJob` (monthly) | Advisory transaction lock theo kỳ | Không |
| learning | `RewardGrantRelayJob` | Advisory lock + `SKIP LOCKED` cho batch | Broker trong transaction ngắn |
| learning | `MasteryDecayJob` | Session advisory lock; batch ghi riêng | Không |
| learning | `WorkItemSyncJob` | Đọc nguồn/Content trước; transaction ngắn khóa advisory rồi áp dụng snapshot | Content HTTP ngoài transaction |
| user | `LoginHistoryPurgeScheduler` | Xóa theo cutoff, lặp an toàn | Không |

Chi tiết triển khai, pub/sub không bền, khóa vé và scale compose cũng có trong [mục scheduler và nhiều instance](../../docs/system-architecture.md#scheduler-va-nhieu-instance).
- **Compose:** service ứng dụng không có `container_name` và không mở cổng host → `--scale` chạy được. Gateway giữ 1 bản (cổng 8080). Không cần load balancer tùy chỉnh ở gateway.

## Related Code Files

- Modify: `docker-compose.yml` (Redis; env Redis cho notification, game), `infra/config-server/config-repo/{notification-service,game-service}.yaml`
- Modify: `services/game-service/src/main/java/com/ieltspath/game/infrastructure/websocket/GameWebSocketHandler.java` (tách "phát" khỏi "gửi cục bộ")
- Create: `services/game-service/.../infrastructure/websocket/{RoomEventBridge,RoomEventCodec,RoomEventDelivery,RoomEventChannelConfig,RoomEventChannelSupervisor,RedisWebSocketTicketStore}.java`
- Delete: `services/game-service/.../infrastructure/websocket/InMemoryWebSocketTicketStore.java`
- Rocket và Solo giữ cơ chế khóa hàng/trạng thái đã có; không cần thay `RocketMatchStoreAdapter`, `SoloGameplayRepositoryAdapter` hay hai scheduler để thêm distributed claim.
- Modify: notification listeners để đẩy qua `NoticePushBridge`; `services/notification-service/.../infrastructure/NoticePushChannelConfig.java` đăng ký nhận push cục bộ.
- Modify: `services/{game,notification}-service/pom.xml` (`spring-boot-starter-data-redis`)
- Không thêm claim tùy tiện cho scheduler assessment, content, user; xem bảng cơ chế trong `docs/system-architecture.md`.

## Implementation Steps

### Test viết trước

1. **Khóa hành vi hiện có:** chạy toàn bộ test notification-service và game-service; ghi số test làm mốc.
2. **Scheduler song song:** test tích hợp (Testcontainers Postgres) cho Rocket, Solo và các relay cần xác nhận; Rocket và Solo không cần thêm claim tường minh.
3. **Game qua 2 instance:** Testcontainers Redis + Postgres, 2 context game-service; người A nối instance 1, người B nối instance 2 cùng phòng; B gửi đáp án qua REST ở instance 2 → A nhận `RANKING_UPDATED`; scheduler ở instance 2 kết thúc vòng → A nhận event vòng mới.
4. **Notification qua 2 instance:** 2 context notification-service; client STOMP nối instance A; event RabbitMQ do instance B consume → client nhận push đúng một lần.
5. **Vé game:** vé phát ở context 1, dùng ở context 2; dùng lần hai bị từ chối; hết TTL bị từ chối.

### Code

6. Thêm Redis vào compose (`REDIS_HOST`, `REDIS_PORT`, mật khẩu qua `.env`, không commit giá trị).
7. Game: bridge sự kiện phòng và vé Redis; giữ cơ chế khóa theo trận đã có.
8. Notification: bridge push qua Redis sau khi lưu.
9. Tài liệu: mục "Chạy nhiều instance" trong `docs/system-architecture.md`.

## Success Criteria

- [ ] Test song song Rocket `concurrentSchedulesCloseAnExpiredRoundOnlyOnce`, Solo `concurrentTimeoutSchedulesCostExactlyOneLife`, relay assessment/community xanh; khóa DB không được giữ qua HTTP/LLM.
- [ ] Test game và notification qua 2 instance xanh.
- [ ] `docker compose up --scale notification-service=2 --scale game-service=2 --scale learning-service=2`: thử tay nhận thông báo, chơi một phòng Wordly và một phòng Rocket với hai trình duyệt.
- [ ] Test hiện có của notification và game giữ nguyên số và xanh.

## Risk Assessment

- **Push không bền:** instance rớt giữa chừng thì mất push; REST đồng bộ lại. Chấp nhận, ghi tài liệu.
- **Thứ tự event game:** Redis pub/sub giữ thứ tự trong một kênh từ một publisher; event từ hai instance có thể xen kẽ. Payload đã mang trạng thái đầy đủ (ranking, vòng) nên client chỉ cần hiển thị bản mới nhất; kiểm trong test bước 3.
- **Redis chết:** game và notification mất push thời gian thực, REST vẫn chạy. Vé game không phát được → không vào phòng mới. Có metric cảnh báo (phase 2).
- **Advisory lock giữ quá lâu:** chỉ dùng cho job không gọi HTTP; `pg_try_` để không chờ.

## Cơ chế scheduler đã rà

Bảng bên trên ghi cơ chế đã đối chiếu với code; phase vẫn chờ Claude kiểm tay đa instance theo tiêu chí phía trên.
