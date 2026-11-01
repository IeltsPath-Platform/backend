---
phase: 2
title: "Chỉnh phase 3: route auth và X-Forwarded-For"
status: pending
priority: P1
effort: "S"
dependencies: [old-3]
---

# Phase 2: Chỉnh phase 3 — route auth và X-Forwarded-For

Người làm: Codex, một nhóm commit sửa trên `feat/reliability-patterns` sau commit `e5161ba3`. Luật cứng, mẫu báo cáo và cổng duyệt: mục "Khung kiểm soát Codex" trong [plan.md cũ](../261031-2145-backend-reliability-patterns/plan.md). Mục này là phần bổ sung, không thay luật.

## Overview

Cổng duyệt phase 3 cũ tìm ra hai lỗi chặn và vài điểm cần ghi nhận. Cả hai lỗi chặn nằm ở cấu hình gateway và bộ phân giải khóa, đều nhỏ.

## Lỗi chặn

**A. Route auth sai path và thiếu endpoint.**

- Endpoint thật nằm dưới `/auth/*` (`AuthController`: `login`, `refresh`, `forgot-password`, `reset-password`, `logout`, `me`) và `POST /api/users/register`. Gateway không rewrite path này. `/api/auth/*` không tồn tại ở bất kỳ service nào.
- `api-gateway.yaml:60`, `ClientRateLimitGatewayFilterFactory.resolveTier` (dòng 65–67) và test (`/api/auth/login`) dùng `/api/auth/*`; plan cũ dòng 23 và 146 cũng vậy.
- `/auth/reset-password` thiếu ở cả route lẫn `resolveTier`, nên đoán token đặt lại mật khẩu chỉ bị tier `default` (20 req/s).

Sửa: tập endpoint tier `auth` là đúng năm path `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, `/api/users/register`. Xóa mọi `/api/auth/*`. `logout` giữ tier `default`.

**B. Đọc `X-Forwarded-For` từ bên trái.**

- `ClientKeyResolver` lấy phần tử đầu của `X-Forwarded-For`. Proxy tin cậy thường **thêm** địa chỉ client vào bên phải (nginx `$proxy_add_x_forwarded_for`); phần tử bên trái do client tự gửi nên giả mạo được, làm mỗi lần gọi một khóa IP mới và vô hiệu rate limit.
- Mặc định `trusted-proxies: []` nên chưa bị khai thác, nhưng lỗi hiện ra ngay khi bật proxy.

Sửa: duyệt `X-Forwarded-For` từ phải sang trái, bỏ các hop nằm trong `trusted-proxies`, lấy địa chỉ đầu tiên không tin cậy làm client; nếu tất cả đều tin cậy hoặc header rỗng thì dùng địa chỉ remote.

## Ghi nhận (không chặn)

- `failsClosedWhenRedisIsDown` đã bị sửa sau commit đỏ: từ dừng container Redis thật sang mock `getExpire` ném `RedisConnectionFailureException`. Đủ để chứng minh ánh xạ lỗi; báo cáo phải ghi đây là Deviation. Nếu `RedisLoginAttemptLimiter` gọi phương thức Redis khác trước `getExpire`, mock trả `null` và test có thể qua vì lý do sai: thêm một ca dùng `LettuceConnectionFactory` trỏ tới cổng đóng.
- `sharesLimitAcrossTwoGatewayInstances` được viết lại với Redis thật cho hai limiter: mạnh hơn bản đỏ, giữ nguyên.
- Báo cáo ghi "Deviations" thiếu hai thay đổi test trên.

## Related Code Files

| Hành động | File |
|---|---|
| Sửa | `infra/config-server/config-repo/api-gateway.yaml` (route `user-auth-limited`) |
| Sửa | `infra/api-gateway/.../ratelimit/{ClientRateLimitGatewayFilterFactory,ClientKeyResolver}.java` |
| Sửa | `infra/api-gateway/src/test/.../ratelimit/ClientRateLimitGatewayFilterFactoryTest.java` (đổi path; thêm ca mới) |
| Tạo | `infra/api-gateway/src/test/.../ratelimit/AuthRouteConsistencyTest.java` |
| Sửa | `docs/system-architecture.md` (bảng rate limit: danh sách path auth, cách đặt `trusted-proxies`) |
| Sửa | `plans/261031-2145-backend-reliability-patterns/phase-03-api-reliability.md` dòng 23, 41, 99, 146 |

## Implementation Steps

### Test viết trước (phải đỏ)

1. `ClientRateLimitGatewayFilterFactoryTest`: đổi `/api/auth/login` thành `/auth/login`; thêm `appliesAuthTierToEveryPublicAuthEndpoint` (năm path → tier `auth`; `/auth/logout` → `default`).
2. `ClientKeyResolver` test: `usesFirstUntrustedAddressFromTheRight` (`X-Forwarded-For: 6.6.6.6, 198.51.100.7` với remote tin cậy → `198.51.100.7`), `skipsTrustedHopsFromTheRight` (`198.51.100.7, 10.0.0.2` với `10.0.0.2` tin cậy), `fallsBackToRemoteWhenEveryHopIsTrusted`, `ignoresSpoofedLeftmostEntry`.
3. `AuthRouteConsistencyTest`: đọc `api-gateway.yaml` bằng SnakeYAML, lấy predicate `Path` của route `user-auth-limited`, khẳng định bằng đúng tập năm path; và mọi path của `gateway.security.public-endpoints` bắt đầu bằng `/auth/` (trừ `logout`) cùng `/api/users/register` đều có mặt. Test này chặn việc cấu hình và code lệch nhau lần nữa.

### Code

4. Sửa yaml, `resolveTier`, `ClientKeyResolver` theo mục "Lỗi chặn".
5. Sửa tài liệu và dòng plan cũ.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl infra/api-gateway -am verify
```

## Success Criteria

- [ ] Test 1–3 đỏ trước, xanh sau; toàn bộ test gateway cũ xanh.
- [ ] Không còn chuỗi `/api/auth` trong repo (`git grep`), ngoài file lịch sử.
- [ ] Kiểm tay qua gateway thật (Claude): 6 lần `POST /auth/reset-password` sai từ một IP → lần thứ 6 nhận 429 có `Retry-After`.

## Dừng và hỏi

- Hạ tầng triển khai cần CIDR thay cho địa chỉ đơn trong `trusted-proxies`: báo, không tự thêm tính năng khớp CIDR.
- Phát hiện endpoint nhạy cảm khác ngoài năm path trên: báo kèm đường dẫn, không tự thêm.
