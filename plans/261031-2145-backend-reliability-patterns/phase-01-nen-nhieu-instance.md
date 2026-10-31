---
phase: 1
title: "Nen nhieu instance"
status: pending
priority: P1
effort: "L"
dependencies: []
---

# Phase 1: Nền nhiều instance

<!-- Updated: Red Team Review - bỏ sticky và STOMP relay; game và notification phát qua Redis pub/sub; khóa scheduler game theo trận; không bọc khóa job đã claim theo hàng -->

## Overview

Cho phép chạy 2+ instance mỗi service mà không chạy job trùng, không mất push thông báo, không vỡ phòng game. Thêm Redis làm store chung (vé game, kênh phát chung, rate limit ở phase 3).

## Requirements

- Redis 7 trong `docker-compose.yml`, healthcheck, cổng host chỉ `127.0.0.1` (nếu mở).
- 16 `@Scheduled` job: mỗi đơn vị việc chỉ một instance xử lý.
- notification-service: push STOMP tới người dùng dù họ nối vào instance khác.
- game-service: mọi người trong phòng nhận event phòng dù nối vào instance khác, kể cả event do scheduler sinh ra.
- Không đổi hợp đồng WebSocket phía frontend (`/ws/notifications`, `/ws/games`, destination `/user/queue/notifications`, định dạng message game).

## Hiện trạng (đã kiểm)

- Game: một endpoint `/ws/games` (`GameWebSocketConfig.java:28`); phòng gắn qua vé (`InMemoryWebSocketTicketStore.issue(roomId, userId)`); session giữ trong `sessionsByRoom` (`GameWebSocketHandler.java:262`); event phòng là `@TransactionalEventListener` trong tiến trình (`GameWebSocketHandler.java:66` và 11 handler khác), phát từ REST use case và từ scheduler.
- Notification: `enableSimpleBroker("/queue")` (`NotificationMessagingConfig.java:50`); listener RabbitMQ gọi `convertAndSendToUser` trên instance đang consume.
- Scheduler game: `RocketMatchStoreAdapter.dueMatches` (`:130`) và `SoloGameplayRepositoryAdapter.due` (`:531`) đọc không claim → 2 instance cùng xử lý một trận.
- Relay assessment, community và store chấm LLM (`JdbcGateEssayJobStore`) đã claim bằng `FOR UPDATE SKIP LOCKED`.

## Architecture

- **Kênh phát chung (Redis pub/sub)** cho WebSocket:
  - Game: các `@TransactionalEventListener` hiện tại không gửi thẳng tới session nữa mà publish một message (`roomId`, loại, payload JSON đã dựng sẵn, danh sách người nhận nếu là gửi riêng) lên kênh `game.room-events`. Mỗi instance subscribe kênh, gửi cho session cục bộ trong `sessionsByRoom`. Logic game không đổi.
  - Notification: sau khi lưu thông báo, publish (`recipientId`, payload) lên kênh `notification.push`; mỗi instance subscribe và gọi `convertAndSendToUser` cục bộ (simple broker giữ nguyên; người không nối vào instance đó thì bỏ qua).
  - Pub/sub không bền: instance rớt thì mất push lúc đó; chấp nhận vì REST là nguồn sự thật (đã có đồng bộ lại). Ghi rõ trong tài liệu.
  - Lớp nhỏ dùng chung giữa hai service (publisher + listener container + mã hóa JSON): đặt trong mỗi service (2 chỗ, mã ngắn); chỉ đưa vào `shared/` nếu service thứ ba cần.
- **Vé game:** `InMemoryWebSocketTicketStore` → `RedisWebSocketTicketStore` (TTL, `GETDEL` để dùng một lần), vì vé phát qua REST ở instance A có thể được dùng ở instance B.
- **Scheduler:**
  - Job có store claim theo hàng (`SKIP LOCKED`): không bọc gì, chỉ thêm test song song. Áp cho 2 relay và `GateEssayGradingScheduler` (gọi LLM, không được giữ khóa qua HTTP).
  - Game: claim theo trận bắt buộc: `dueMatches`/`due` chọn bằng `FOR UPDATE SKIP LOCKED` trong transaction của lần xử lý, hoặc `pg_try_advisory_xact_lock(hash(matchId))` trước khi `advance`/`timeout`; trận đang bị instance khác xử lý thì bỏ qua lượt này.
  - Job còn lại (`GradingDeadlineScheduler`, `AiDraftScheduler`, `SpeakingRecordingRetentionScheduler`, `LoginHistoryPurgeScheduler`): kiểm store; nếu không claim theo hàng và job không gọi HTTP trong lúc giữ khóa → bọc toàn job bằng `pg_try_advisory_xact_lock` (mẫu `content/infrastructure/scheduling/PostgresAdvisoryLock.java`); nếu có gọi HTTP → chuyển sang claim theo hàng.
  - Không thêm ShedLock.
- **Compose:** service ứng dụng không có `container_name` và không mở cổng host → `--scale` chạy được. Gateway giữ 1 bản (cổng 8080). Không cần load balancer tùy chỉnh ở gateway.

## Related Code Files

- Modify: `docker-compose.yml` (Redis; env Redis cho notification, game), `infra/config-server/config-repo/{notification-service,game-service}.yaml`
- Modify: `services/game-service/src/main/java/com/ieltspath/game/infrastructure/websocket/GameWebSocketHandler.java` (tách "phát" khỏi "gửi cục bộ")
- Create: `services/game-service/.../infrastructure/websocket/{RoomEventBus,RedisRoomEventBus}.java`, `.../websocket/RedisWebSocketTicketStore.java`
- Delete: `services/game-service/.../infrastructure/websocket/InMemoryWebSocketTicketStore.java`
- Modify: `services/game-service/.../persistence/adapter/{RocketMatchStoreAdapter,SoloGameplayRepositoryAdapter}.java`, `.../config/{RocketRoundScheduler,SoloTimeoutScheduler}.java`
- Modify: `services/notification-service/.../infrastructure/{GradingCompletedListener,GradingRequestedListener,GradingOverdueListener,GradingDueSoonListener,CommunityEventConsumer}.java` (push qua bus), Create: `.../infrastructure/{PushBus,RedisPushBus}.java`
- Modify: `services/{game,notification}-service/pom.xml` (`spring-boot-starter-data-redis`)
- Modify (nếu thiếu claim): scheduler và store của assessment, content, user liệt kê ở trên

## Implementation Steps

### Test viết trước

1. **Khóa hành vi hiện có:** chạy toàn bộ test notification-service và game-service; ghi số test làm mốc.
2. **Scheduler song song:** với mỗi job trong 9 job chưa có khóa trong file, test tích hợp (Testcontainers Postgres) tạo dữ liệu, chạy hàm batch từ 2 luồng cùng lúc, khẳng định mỗi đơn vị việc xử lý đúng 1 lần. Rocket và Solo phải đỏ trước khi sửa.
3. **Game qua 2 instance:** Testcontainers Redis + Postgres, 2 context game-service; người A nối instance 1, người B nối instance 2 cùng phòng; B gửi đáp án qua REST ở instance 2 → A nhận `RANKING_UPDATED`; scheduler ở instance 2 kết thúc vòng → A nhận event vòng mới.
4. **Notification qua 2 instance:** 2 context notification-service; client STOMP nối instance A; event RabbitMQ do instance B consume → client nhận push đúng một lần.
5. **Vé game:** vé phát ở context 1, dùng ở context 2; dùng lần hai bị từ chối; hết TTL bị từ chối.

### Code

6. Thêm Redis vào compose (`REDIS_HOST`, `REDIS_PORT`, mật khẩu qua `.env`, không commit giá trị).
7. Sửa scheduler theo kết quả bước 2 (game bắt buộc claim theo trận).
8. Game: `RoomEventBus` + chuyển các handler sang publish; listener Redis gửi cục bộ; vé Redis.
9. Notification: `PushBus` + chuyển các listener sang publish sau khi lưu.
10. Tài liệu: mục "Chạy nhiều instance" trong `docs/system-architecture.md` (kênh Redis, push không bền, cách scale bằng compose).

## Success Criteria

- [ ] Test song song của 9 job xanh; Rocket và Solo mỗi trận xử lý 1 lần mỗi chu kỳ.
- [ ] Test game và notification qua 2 instance xanh.
- [ ] `docker compose up --scale notification-service=2 --scale game-service=2 --scale learning-service=2`: thử tay nhận thông báo, chơi một phòng Wordly và một phòng Rocket với hai trình duyệt.
- [ ] Test hiện có của notification và game giữ nguyên số và xanh.

## Risk Assessment

- **Push không bền:** instance rớt giữa chừng thì mất push; REST đồng bộ lại. Chấp nhận, ghi tài liệu.
- **Thứ tự event game:** Redis pub/sub giữ thứ tự trong một kênh từ một publisher; event từ hai instance có thể xen kẽ. Payload đã mang trạng thái đầy đủ (ranking, vòng) nên client chỉ cần hiển thị bản mới nhất; kiểm trong test bước 3.
- **Redis chết:** game và notification mất push thời gian thực, REST vẫn chạy. Vé game không phát được → không vào phòng mới. Có metric cảnh báo (phase 2).
- **Advisory lock giữ quá lâu:** chỉ dùng cho job không gọi HTTP; `pg_try_` để không chờ.
