---
title: "Backend reliability patterns và nền nhiều instance"
description: "Áp dụng 10 bước reliability (B1–B10) theo thứ tự rủi ro, trên nền chạy nhiều instance mỗi service: Redis, scheduler không chạy trùng, WebSocket qua broker chung, correlation ID và metric, rate limit, outbox relay, circuit breaker, idempotency, test lỗi."
status: pending
priority: P2
branch: "main"
tags: [reliability, observability, resilience, messaging, multi-instance, tdd]
blockedBy: []
blocks: []
related: [261019-0025-common-events, 261016-1045-community-realtime-feed]
created: "2026-10-31T14:52:33.252Z"
createdBy: "ck:plan"
source: skill
mode: tdd
---

# Backend reliability patterns và nền nhiều instance

## Overview

Đầu vào: `plans/reports/brainstorm-261031-2137-backend-reliability-patterns-report.md` (hiện trạng B1–B10, quyết định đã chốt).

Mục tiêu kép: sửa rủi ro thật **và** mỗi bước B1–B10 có ít nhất một chỗ áp dụng thật kèm test minh họa. Hệ thống sẽ chạy nhiều instance mỗi service, nên phase 1 dựng nền đó trước.

Chế độ TDD: mỗi phase viết test trước (khóa hành vi hiện có, rồi test cho hành vi mới), sau đó mới sửa code. Bộ test hiện có (179 lớp Testcontainers) phải giữ xanh.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Nen nhieu instance](./phase-01-nen-nhieu-instance.md) | Pending |
| 2 | [Observability](./phase-02-observability.md) | Pending |
| 3 | [API reliability](./phase-03-api-reliability.md) | Pending |
| 4 | [Outbox relay](./phase-04-outbox-relay.md) | Pending |
| 5 | [Resilience va goi service](./phase-05-resilience-va-goi-service.md) | Pending |
| 6 | [Idempotency va saga](./phase-06-idempotency-va-saga.md) | Pending |
| 7 | [Ra messaging va concurrency](./phase-07-ra-messaging-va-concurrency.md) | Pending |
| 8 | [Test loi tong](./phase-08-test-loi-tong.md) | Pending |

Chia việc, thứ tự, prompt dán sẵn cho Codex và bảng tiến độ: [work-split-claude-codex.md](./work-split-claude-codex.md).

## Khung kiểm soát Codex (phase 3–8)

Phase file **là** brief của Codex; không có brief riêng. Mỗi phase 3–8 có mục `## Khung code cho Codex` gồm: hợp đồng code (package, tên lớp, chữ ký method, property, migration, tên test), bảng file được sửa, lệnh kiểm chứng, điều kiện dừng. Mục đó là hợp đồng: làm đúng, không sáng tạo thêm.

**Luật cứng**

1. **Hợp đồng code:** tên package, lớp, method, property, migration, test đúng như khung. Muốn khác (kể cả vì thấy hợp lý hơn) → `NEEDS_CONTEXT`, nêu lý do và đề xuất, không tự đổi.
2. **Phạm vi file:** chỉ tạo/sửa file trong bảng "File được sửa" của phase. Cần file khác → dừng hỏi. Ngoại lệ duy nhất: test fixture cần thêm bean giả (`@MockitoBean`) vì lớp trong bảng có phụ thuộc mới — được làm, phải ghi trong báo cáo.
3. **Cấm:** đọc `.env`; sửa `docker-compose.yml`, `infra/`, `pom.xml` ngoài bảng; thêm thư viện ngoài danh sách của phase; sửa, xóa, `@Disabled` test cũ để cho xanh; đổi hợp đồng API công khai ngoài khung; ghi Supabase, RabbitMQ, Redis thật; push lên `main`, mở PR, merge; ghi mã plan, số phase, nhãn B1–B10 trong code, tên test, migration, commit.
4. **TDD bắt buộc:** commit đầu chỉ có test mới (phải đỏ; ghi tên test đỏ và lý do đỏ vào báo cáo). Commit sau là code. Kết thúc phase: mọi test xanh.
5. **Nhánh:** commit thẳng lên `feat/reliability-patterns` (Claude tạo khi làm phase 1). Mỗi phase một nhóm commit, commit message theo conventional commits.
6. **Lệnh kiểm chứng:** chạy đúng lệnh trong mục "Lệnh kiểm chứng" của phase; dán số thật (chạy, lỗi, bỏ qua). Không ghi "đã chạy" cho lệnh chưa chạy.
7. **Dừng ngay** (`NEEDS_CONTEXT` hoặc `BLOCKED`) khi: khung không khớp code thật (lớp, method, đường dẫn không tồn tại hoặc khác chữ ký), test cũ mâu thuẫn với hành vi mới, cần quyết định sản phẩm, hoặc gặp mục "Dừng và hỏi" của phase.

**Mẫu báo cáo (bắt buộc)**

```
Phase: <số>
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
Commits: <hash> <message> (mỗi dòng một commit)
Files: <danh sách file tạo/sửa/xóa>
Red tests (commit test): <tên test> — <vì sao đỏ>
Verification: <lệnh> → <chạy/lỗi/bỏ qua>
Deviations from skeleton: none | <mục, lý do>
Concerns: <nếu có>
```

**Cổng duyệt của Claude (trước khi `ck plan check`)**

1. Đối chiếu diff với khung: tên, chữ ký, file ngoài bảng → trả lại nếu lệch.
2. Kiểm commit test đỏ có thật (checkout commit test, chạy test đó).
3. Chạy lại lệnh kiểm chứng của phase.
4. Làm mục "Kiểm tay" của phase trên stack thật.
5. Đạt cả 4 mới đánh dấu phase xong.

## Đối chiếu B1–B10

| Bước | Phase | Chỗ minh họa chính |
|---|---|---|
| B1 Idempotency | 6 | `Idempotency-Key` cho cấp gói admin; rà khóa tự nhiên của trừ điểm, tín dụng chấm |
| B2 Transactional Outbox | 4 | Relay access → `SubscriptionGrantedV1` → notification |
| B3 Reliable Messaging | 4, 7 | Consumer mới dùng retry/DLQ chung; quy tắc version event; đưa message từ DLQ về xử lý lại |
| B4 Saga | 6 | Máy trạng thái + test cửa sổ lỗi: mở khóa khóa học, tín dụng chấm người thật |
| B5 Resilience | 5 | Resilience4j: breaker, timeout, retry GET, bulkhead LLM; timeout ở gateway |
| B6 Concurrency | 1, 7 | Scheduler khóa advisory khi nhiều instance; rà `@Version` |
| B7 Service-to-service | 2, 5 | Chuyển tiếp correlation ID; ánh xạ lỗi downstream thống nhất; contract test cho 3 API nội bộ |
| B8 API Reliability | 3 | Rate limit gateway (Redis), chống dò mật khẩu, định dạng lỗi chung |
| B9 Observability | 2 | MDC, log JSON, Micrometer Prometheus |
| B10 Failure Testing | mọi phase + 8 | Bộ test lỗi tổng trong CI |

## Quyết định đã chốt (từ brainstorm)

- Nhiều instance; thêm Redis vào compose. Không Redis HA.
- Thư viện: Resilience4j, Micrometer Prometheus, Logstash JSON encoder. Rate limit ở gateway bằng `RedisRateLimiter` (bọc trong filter riêng để trả `Retry-After`); không Bucket4j.
- Redis chết: rate limit cho qua (có metric), riêng login chặn.
- Outbox chỉ ghi: access relay + consumer thật; game giữ làm nhật ký + dọn; content bảng không ai ghi → xóa code chết.
- WebSocket (đổi sau red-team): game và notification phát qua **Redis pub/sub**, mỗi instance gửi cho session cục bộ; notification giữ simple broker; vé game lưu Redis. Bỏ sticky ở gateway và STOMP relay.
- Ngoài phạm vi: Kubernetes, tracing phân tán, Redis HA, đồng bộ trạng thái game giữa instance, schema registry.

## Điều chỉnh so với brainstorm (kiểm lại khi lập plan)

- Brainstorm đề xuất thêm `Idempotency-Key` cho kích hoạt key và trừ điểm nội bộ: **đã có sẵn** (`idempotencyKey`/`referenceId` ở access). Chỗ thiếu thật duy nhất là cấp gói admin (phase 6).
- Outbox content: không chỗ nào ghi → xóa code chết, không cần rà thêm (phase 4).
- 16 scheduler: 2 relay đã claim bằng `SKIP LOCKED`; 7 job còn lại cần kiểm store trước khi bọc khóa (phase 1).

## Dependencies

- Phase 1 trước mọi phase (Redis, scale). Phase 2 trước 3–8 (cần log và metric để kiểm chứng).
- Phase 3 dùng Redis của phase 1. Phase 4 dùng metric của phase 2. Phase 8 tổng hợp sau 1–7.
- Liên quan, không chặn: `261019-0025-common-events` (helper retry/DLQ phase 4 và 7 dùng lại), `261016-1045-community-realtime-feed` (WebSocket notification).

## Quy tắc chung

- Đọc `backend/AGENTS.md` trước khi code.
- **Nhánh và PR (Validation Session 1):** một nhánh tích hợp `feat/reliability-patterns` từ `main` cho backend. Frontend không cần nhánh: đã kiểm, không màn hình nào bị ảnh hưởng (cấp gói admin không có UI; WebSocket giữ nguyên hợp đồng). Mỗi phase là một nhóm commit trên nhánh này; **một PR duy nhất vào `main`** mỗi repo khi xong cả plan. Không PR xếp chồng.
- **Phân công (Validation Session 1):** Claude làm phase 1 và 2 (hạ tầng, module dùng chung). Codex làm phase 3–8 theo mục `## Khung code cho Codex` trong từng phase file (xem "Khung kiểm soát Codex" bên dưới); Claude review diff và kiểm chứng thật trước khi đánh dấu xong. Codex không đọc `.env`, không đổi hạ tầng ngoài phạm vi brief.
- Không đặt mã plan, số phase, nhãn B1–B10 vào comment, tên test, migration, commit. Mô tả hành vi.
- Không chép khóa, mật khẩu Redis hay token vào log, test, tài liệu.
- Test lỗi dùng Testcontainers thật (Postgres, RabbitMQ, Redis); HTTP downstream giả bằng `MockRestServiceServer` (đã dùng trong repo). Thêm Toxiproxy chỉ ở phase 8 nếu cần cắt mạng thật.

## Red Team Review

Ngày 2026-10-31. Bốn góc nhìn (bảo mật, giả định sai, chế độ lỗi, phạm vi), chạy trong phiên, không spawn reviewer. 15 nghi vấn → 7 phát hiện có bằng chứng `file:dòng`; 7 áp dụng (2 trong đó đổi quyết định cũ, người dùng đã chốt lại), 4 bác bỏ.

| # | Mức | Phát hiện | Bằng chứng | Xử lý | Phase |
|---|---|---|---|---|---|
| 1 | Critical | Sticky theo phòng không làm được (một endpoint `/ws/games`, không `roomId`) và không phủ event do scheduler sinh | `GameWebSocketConfig.java:28`, `GameWebSocketHandler.java:66`, `:262`, `RocketRoundScheduler.java:27` | Đổi sang Redis pub/sub cho event phòng (người dùng chốt) | 1 |
| 2 | ~~High~~ → **Đã hạ/bác** (kiểm khi làm phase 1) | Scheduler Rocket/Solo đọc trận không claim → 2 instance xử lý trùng | `RocketMatchStoreAdapter.java:40` (`FOR UPDATE`), `RocketGameplayUseCase.java:66-69,249-251` | Trùng chỉ tốn tải; test đồng thời xanh (`concurrentSchedulesCloseAnExpiredRoundOnlyOnce`). Không thêm khóa | 1 |
| 3 | High | Khóa login theo email cho phép khóa tài khoản người khác | `AuthController.java:62` | Chặn theo email+IP; theo email chỉ trễ tăng dần | 3 |
| 4 | High | `/actuator/prometheus` bị security chặn; exposure ghi đè ở từng service | `CommonSecurityAutoConfiguration.java:56`, `learning-service.yaml:155` | Sửa rule + mọi file config-repo khai báo exposure (hiện 12) + test 200 | 2 |
| 5 | Medium | Có metric nhưng không ai thu thập | `config-repo/application.yaml:12` | Prometheus trong compose, profile `observability`, Eureka SD | 2 |
| 6 | Medium | STOMP relay mở một kết nối RabbitMQ cho mỗi WebSocket | `NotificationMessagingConfig.java:50` | Notification dùng cùng kênh Redis với game (người dùng chốt) | 1 |
| 7 | Medium | Bọc toàn job bằng advisory lock mâu thuẫn với "không giữ khóa qua HTTP" (job chấm LLM) | `GateEssayGradingScheduler.java:22` | Job đã claim theo hàng thì không bọc | 1 |

Bác bỏ: Redis chết làm sập login (quyết định người dùng); bọc cả 22 client là thừa (helper chung, cần minh họa); đổi listener đang chạy tốt (đã có điều kiện); xóa `GatewayRequestContext` (đã có điều kiện).

### Whole-Plan Consistency Sweep

- Decision delta: bỏ sticky + load balancer tùy chỉnh; bỏ STOMP relay và plugin `rabbitmq_stomp`; thêm Redis cho notification; game scheduler claim bắt buộc; login hai bộ đếm; actuator security + Prometheus.
- Đã rà mọi file plan tìm `sticky`, `STOMP relay`, `rabbitmq_stomp`, `RoomAffinity`, `enableStompBrokerRelay`: chỉ còn trong mục Red Team Review và ghi chú "đã bỏ".
- Phase 8 thêm kỳ vọng push khi Redis chết.
- Không còn mâu thuẫn chưa giải quyết.

## Validation Log

### Session 1 (2026-10-31)

Verification: guard áp dụng (đã có Red Team Review kèm bằng chứng); chỉ kiểm các tuyên bố thêm sau red-team.

### Verification Results
- Claims checked: 8 (5 listener notification, `SKIP LOCKED` ở `JdbcGateEssayJobStore`/relay assessment/relay community, `AuditedLoginUseCase`, số file exposure, số chỗ `convertAndSendToUser`)
- Verified: 7 | Failed: 1 | Unverified: 0
- Tier: Full (8 phase), giới hạn theo guard
- Failures: "10 file yaml" khai báo exposure → thực tế 12 file (`infra/config-server/config-repo/*.yaml`); đã sửa sau khi người dùng xác nhận.

| # | Câu hỏi | Quyết định | Áp vào |
|---|---|---|---|
| 1 | Chia PR | Một nhánh tích hợp `feat/reliability-patterns`, **1 PR vào main** mỗi repo | plan.md |
| 2 | Người làm | Claude phase 1–2; Codex phase 3–8 theo brief, Claude review | plan.md |
| 3 | Mức rate limit | Dùng bảng mặc định phase 3, chỉnh theo metric | phase 3 (không đổi) |
| 4 | Outbox `PENDING` cũ của access | Chuyển sang `NOT_RELAYED`, không gửi; event không relay cũng ghi `NOT_RELAYED` | phase 4 |
| 5 | Khóa cấp gói admin | Bắt buộc. (Sau đó kiểm: frontend không có UI cấp gói → không cần PR frontend; ghi header trong OpenAPI.) | phase 6 |
| 6 | Toxiproxy | Cho phép khi cần, chỉ scope test | phase 8 |
| 7 | Số file exposure | Ghi theo thực tế (12), ưu tiên xóa ghi đè | phase 2, Red Team table |

### Whole-Plan Consistency Sweep
- Rà mọi file tìm `mỗi phase một PR`, `10 file`, `plugin stomp`, `quyết định trong bước 1`: đã sửa hết; còn "quyết định khi làm" ở phase 8 (chọn vị trí module test) là chi tiết triển khai, không mâu thuẫn.
- Phase 4 trước đó để mở cách đánh dấu event không relay → đã chốt `NOT_RELAYED`.
- Không còn mâu thuẫn chưa giải quyết.

### Session 2 (2026-10-31): bổ sung thiếu sót và khung Codex

Người dùng hỏi plan đã đủ 10 bước chưa; đối chiếu từng ý con thấy thiếu 3, người dùng đồng ý thêm và yêu cầu khung code chặt để kiểm soát Codex.

| Thay đổi | Bằng chứng | Phase |
|---|---|---|
| Timeout gateway (connect 2 s, response 15 s, AI/upload 120 s) | `api-gateway.yaml` không có `connect-timeout`/`response-timeout` | 5 |
| Contract test 3 API nội bộ bằng JSON mẫu dùng chung | `docs/contracts/` chỉ là tài liệu; không có test provider/consumer cho API HTTP nội bộ | 5 |
| Công cụ đưa message từ DLQ về xử lý lại (notification, access, learning) | Chỉ có hướng dẫn tay ở `docs/contracts/assessment-completed-v2.md:172` | 7 |
| Mục "Khung code cho Codex" ở phase 3–8 + "Khung kiểm soát Codex" ở plan.md | Yêu cầu người dùng | 3–8 |
| Login: cửa sổ chờ trả 429 thay vì `Thread.sleep` | Ngủ luồng cho phép làm cạn luồng servlet | 3 |
| Rate limit: filter riêng bọc `RedisRateLimiter` | `RequestRateLimiter` có sẵn không trả `Retry-After` | 3 |
| Không cần PR frontend cho cấp gói admin | `frontend/src` không gọi `subscriptions/grant` | 6 |
| Content: giữ bảng outbox, chỉ xóa 6 file code chết | Tránh rủi ro migration | 4 |
| Phase 8: test đặt trong service sở hữu hành vi, không module mới | Giảm độ phức tạp | 8 |
| Saga tín dụng chấm: kỳ vọng theo thiết kế hiện có (refund lỗi → tín dụng vẫn tiêu, gửi lại không trừ lần hai) | `RequestHumanGradingUseCase.java:20-21,51,66-71` | 6 |

Whole-Plan Consistency Sweep: rà `relay phase 1`, `STOMP (relay`, `frontend`, `RequestRateLimiter`, `briefs/`, `tests/reliability`, `độ trễ tăng dần`; đã sửa phase 3 (file liên quan), phase 4 (push qua `PushBus`), plan.md (nhánh frontend, bảng Session 1). Không còn mâu thuẫn chưa giải quyết.

## Acceptance tổng

- Chạy `docker compose up --scale learning-service=2 --scale notification-service=2 --scale game-service=2 --scale api-gateway=1`: thông báo tới đúng người, mỗi job chạy 1 lần mỗi chu kỳ, phòng game chơi được.
- Mỗi B1–B10 có chỗ áp dụng và test theo bảng đối chiếu.
- `mvn -B -ntp verify` cả reactor xanh trong CI có Docker.
