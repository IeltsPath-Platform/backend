---
phase: 3
title: "API reliability"
status: pending
priority: P1
effort: "M"
dependencies: [1, 2]
---

# Phase 3: API reliability

## Overview

Rate limit dùng chung giữa các instance ở gateway (Redis), chống dò mật khẩu ở login, thay hai bộ đếm in-memory, thống nhất định dạng lỗi và rà validation.

## Requirements

- Gateway dùng `RedisRateLimiter` bọc trong filter riêng `ClientRateLimit` (xem khung code; filter `RequestRateLimiter` có sẵn không trả `Retry-After`), khóa = userId (đã xác thực) hoặc IP client (anonymous).
- Mức mặc định (cấu hình được trong `api-gateway.yaml`, chỉnh sau khi xem metric phase 2):

  | Nhóm | Route | replenish/giây | burst |
  |---|---|---|---|
  | auth | `/api/auth/login`, `/api/auth/refresh`, `/api/auth/forgot-password`, `/api/users/register` | 1 | 5 |
  | upload | các route upload ảnh, audio, ghi âm | 1 | 5 |
  | ai | route sinh nội dung AI, chấm bằng LLM | 1 | 3 |
  | default | còn lại | 20 | 40 |

- Vượt giới hạn: 429, header `Retry-After`, body theo định dạng lỗi chung, metric `rate_limit_rejected_total{route}`.
- Redis lỗi: gateway **cho qua** (hành vi mặc định của `RedisRateLimiter` khi Redis lỗi; có test khẳng định) và ghi metric lỗi.
- Login trong user-service (`AuthController.java:62`, `AuditedLoginUseCase`) — <!-- Updated: Red Team Review - tránh để kẻ tấn công khóa tài khoản người khác --> hai bộ đếm, cửa sổ 15 phút:
  - Theo cặp `email đã chuẩn hóa + IP`: lần sai thứ 6 trở đi → 429 + `Retry-After` đến hết cửa sổ. Chặn dò mật khẩu từ một nguồn mà không ảnh hưởng chủ tài khoản ở IP khác.
  - Theo `email` (mọi IP): vượt 20 lần sai → không khóa cứng 15 phút; mỗi lần sai sau đó mở một "cửa sổ chờ" ngắn tăng dần (1 s, 2 s, 4 s, tối đa 8 s); trong cửa sổ đó trả 429 + `Retry-After` = số giây còn lại; ghi cảnh báo + metric. <!-- Updated: khung Codex - không ngủ luồng (Thread.sleep) vì kẻ tấn công có thể dùng nó làm cạn luồng servlet --> Không dùng `Thread.sleep`. Chủ tài khoản chỉ phải chờ vài giây, không bị khóa hẳn.
  - Đăng nhập đúng xóa bộ đếm của cặp `email + IP` đó.
  - Redis lỗi → **chặn login** (503 `LOGIN_TEMPORARILY_UNAVAILABLE`).
- Thay `InMemoryAvatarUploadRateLimiter` (user) và `InMemoryMediaRateLimiter` (community) bằng bản Redis cùng port (`AvatarUploadRateLimiter`, `MediaRateLimiter`).
- Định dạng lỗi: 9 service có `ErrorResponse` riêng; khẳng định cùng tập field (`timestamp,status,error,message,path,details,code` như assessment) và sửa chỗ lệch.
- Validation: rà controller community (1 file `@Valid`) và notification (1 file) cho request body không có ràng buộc.

## Architecture

- Gateway là WebFlux → `spring-boot-starter-data-redis-reactive`. `KeyResolver` bean: lấy `sub` từ JWT đã kiểm (filter hiện có) hoặc IP từ `X-Forwarded-For` đầu tiên chỉ khi request đến từ proxy tin cậy (cấu hình), ngược lại địa chỉ remote.
- Login limiter là port ở user-service (`LoginAttemptLimiter`), adapter Redis (`INCR` + `EXPIRE`), gọi trong `AuthController`/use case login trước khi kiểm mật khẩu.
- Không đếm sai mật khẩu cho tài khoản không tồn tại theo cách lộ thông tin: đếm theo email chuẩn hóa dù tồn tại hay không.

## Related Code Files

- Modify: `infra/api-gateway/pom.xml`, `infra/config-server/config-repo/api-gateway.yaml` (filter `ClientRateLimit` theo nhóm route)
- Create: `infra/api-gateway/src/main/java/com/ieltspath/apigateway/ratelimit/{ClientKeyResolver,RateLimitRejectionHandler}.java`
- Create: `services/user-service/.../application/port/LoginAttemptLimiter.java`, `.../infrastructure/adapter/RedisLoginAttemptLimiter.java`
- Modify: `services/user-service/src/main/java/com/ieltspath/user/api/controller/AuthController.java` (hoặc use case login)
- Replace: `services/user-service/.../infrastructure/adapter/InMemoryAvatarUploadRateLimiter.java`, `services/community-service/.../infrastructure/adapter/InMemoryMediaRateLimiter.java`
- Modify: `services/*/src/main/java/.../api/exception/ErrorResponse.java` và `GlobalExceptionHandler.java` (chỉ chỗ lệch)
- Modify: `infra/config-server/config-repo/{user-service,community-service}.yaml`, `docker-compose.yml` (Redis env cho gateway, user, community)

## Implementation Steps

### Test viết trước

1. Gateway (Testcontainers Redis): 2 context gateway cùng Redis; gửi `burst+1` request auth cùng IP chia đều 2 context → request cuối 429 có `Retry-After`.
2. Gateway: userId khác nhau không chia sẻ giới hạn; anonymous theo IP.
3. Gateway: dừng container Redis → request vẫn qua (200 từ downstream giả), metric lỗi tăng.
4. user-service: cùng email + IP sai 5 lần → lần 6 trả 429; **cùng email từ IP khác vẫn đăng nhập được** (chủ tài khoản không bị khóa); 21 lần sai từ nhiều IP → lần sau trả 429 với `Retry-After` 1–8 giây, hết cửa sổ thì đăng nhập đúng được; đăng nhập đúng xóa bộ đếm cặp; email khác hoa thường dùng chung bộ đếm; Redis dừng → 503.
5. Limiter Redis cho avatar và media: test hành vi giống test in-memory hiện có (chạy lại test cũ trên adapter mới).
6. Test hợp đồng lỗi: mỗi service một test gọi endpoint sinh 400 và 404, so tập field JSON với danh sách chuẩn.

### Code

7. Thêm phụ thuộc và cấu hình gateway; `KeyResolver`; handler 429 trả body chuẩn.
8. Login limiter.
9. Thay hai limiter in-memory; xóa lớp cũ.
10. Sửa lệch định dạng lỗi; bổ sung `@Valid` và ràng buộc thiếu.
11. Tài liệu: bảng mức rate limit và hành vi khi Redis lỗi trong `docs/system-architecture.md`.

## Success Criteria

- [ ] Test 1–6 xanh.
- [ ] Thử tay: login sai 6 lần qua gateway → 429 có `Retry-After`.
- [ ] Không còn lớp `InMemory*RateLimiter`.
- [ ] Mọi service trả cùng tập field lỗi.

## Risk Assessment

- **Mức quá chặt chặn người dùng thật (nhiều người chung NAT):** khóa ưu tiên userId; mức default rộng; chỉnh theo metric.
- **IP giả qua `X-Forwarded-For`:** chỉ tin header khi đi qua proxy cấu hình; mặc định dùng remote address.
- **Redis chết làm sập login:** chấp nhận (quyết định); có metric và log cảnh báo.
- **Đổi field lỗi làm vỡ frontend:** chỉ thêm field thiếu, không đổi tên field hiện có.

## Khung code cho Codex

Điều kiện bắt đầu: phase 1 (Redis trong compose) và phase 2 (`common-observability`, metric) đã có trên `feat/reliability-patterns`.

### Gateway (`infra/api-gateway`, Spring Cloud Gateway WebFlux, Spring Cloud 2025.0.0)

Không dùng filter `RequestRateLimiter` có sẵn (nó trả 429 rỗng, không có `Retry-After`). Dùng `RedisRateLimiter` có sẵn **bên trong** một filter riêng để tự viết response.

```java
package com.ieltspath.apigateway.ratelimit;

/** "user:{sub}" khi có JwtAuthenticationToken; ngược lại "ip:{địa chỉ}" (X-Forwarded-For chỉ khi remote nằm trong trustedProxies). */
public class ClientKeyResolver implements KeyResolver {
    public ClientKeyResolver(RateLimitProperties properties) { }
    @Override public Mono<String> resolve(ServerWebExchange exchange) { }
}

@ConfigurationProperties("gateway.rate-limit")
public record RateLimitProperties(List<String> trustedProxies, Map<String, Tier> tiers) {
    public record Tier(int replenishRate, int burstCapacity) { }
}

/** Filter dùng trong route: `- name: ClientRateLimit` với `args.tier: auth|upload|ai|default`. */
@Component
public class ClientRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ClientRateLimitGatewayFilterFactory.Config> {
    public ClientRateLimitGatewayFilterFactory(RedisRateLimiter limiter, ClientKeyResolver keys,
                                               RateLimitProperties properties, MeterRegistry meters) { }
    // Bị chặn: 429, header Retry-After = ceil(1 / replenishRate) giây (tối thiểu 1),
    // body JSON {"timestamp","status":429,"error":"Too Many Requests","message","path","code":"RATE_LIMITED"},
    // tăng counter rate_limit_rejected_total{route=<route id>, tier=<tier>}.
    // Redis lỗi: RedisRateLimiter đã cho qua; thêm counter rate_limit_backend_errors_total.
    public static class Config { private String tier = "default"; /* getter, setter */ }
}

@Configuration @EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig { /* bean RedisRateLimiter (mặc định), bean ClientKeyResolver */ }
```

Cấu hình `infra/config-server/config-repo/api-gateway.yaml` (thêm, không đổi route cũ):

```yaml
gateway:
  rate-limit:
    trusted-proxies: []          # để trống: luôn dùng remote address
    tiers:
      auth:    { replenish-rate: 1,  burst-capacity: 5 }
      upload:  { replenish-rate: 1,  burst-capacity: 5 }
      ai:      { replenish-rate: 1,  burst-capacity: 3 }
      default: { replenish-rate: 20, burst-capacity: 40 }
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

- Route `auth`: thêm route mới id `user-auth-limited` **trước** route `user-service`, cùng `uri: lb://USER-SERVICE`, `Path=/api/auth/login,/api/auth/refresh,/api/auth/forgot-password,/api/users/register`, filter `ClientRateLimit` tier `auth`.
- Route `upload` và `ai`: liệt kê đường dẫn upload (ảnh, audio, ghi âm) và AI (sinh nội dung, chấm LLM) bằng cách grep `@PostMapping` có `consumes = MULTIPART` và controller AI trong `services/`; thêm route riêng trước route service tương ứng. Danh sách đường dẫn ghi vào báo cáo.
- Mọi route còn lại: thêm `ClientRateLimit` tier `default` qua `default-filters`.
- Thêm `spring-boot-starter-data-redis-reactive` vào `infra/api-gateway/pom.xml`.

### user-service

```java
package com.ieltspath.user.application.port;
public interface LoginAttemptLimiter {
    /** Ném LoginRateLimitedException(retryAfter) nếu cặp email+ip quá ngưỡng hoặc email đang trong cửa sổ chờ. */
    void checkAllowed(String normalizedEmail, String clientIp);
    /** Tăng 2 bộ đếm (cặp, email); mở cửa sổ chờ khi bộ đếm email > 20. */
    void recordFailure(String normalizedEmail, String clientIp);
    /** Xóa bộ đếm cặp. */
    void recordSuccess(String normalizedEmail, String clientIp);
}

package com.ieltspath.user.application.exception;
public class LoginRateLimitedException extends RuntimeException { public Duration retryAfter() { } }
public class LoginLimiterUnavailableException extends RuntimeException { }

package com.ieltspath.user.infrastructure.adapter;
/** Khóa Redis: login:pair:{sha256(email)}:{ip}, login:email:{sha256(email)}, login:wait:{sha256(email)}; TTL cửa sổ 15 phút. Lỗi Redis → LoginLimiterUnavailableException. */
@Component public class RedisLoginAttemptLimiter implements LoginAttemptLimiter { }
```

- Gọi trong `AuditedLoginUseCase.execute(String email, String password, LoginContext context)`: chuẩn hóa email (`trim().toLowerCase(Locale.ROOT)`), `checkAllowed` trước; bắt exception sai thông tin đăng nhập mà `LoginUseCase` đang ném → `recordFailure` rồi ném lại nguyên exception; thành công → `recordSuccess`. IP lấy từ `LoginContext` (đã có `LoginContextResolver.from(httpRequest)`); nếu `LoginContext` chưa có IP → `NEEDS_CONTEXT`.
- `GlobalExceptionHandler` user-service: `LoginRateLimitedException` → 429 + header `Retry-After`, code `LOGIN_RATE_LIMITED`; `LoginLimiterUnavailableException` → 503, code `LOGIN_TEMPORARILY_UNAVAILABLE`.
- `RedisAvatarUploadRateLimiter implements AvatarUploadRateLimiter` (user) và `RedisMediaRateLimiter implements MediaRateLimiter` (community): **cùng con số giới hạn** với bản in-memory hiện có (đọc hằng trong lớp cũ, giữ nguyên); Redis lỗi → cho qua + log warn (upload không phải đường bảo mật).
- Thêm `spring-boot-starter-data-redis` vào `services/user-service/pom.xml`, `services/community-service/pom.xml`; cấu hình Redis vào `config-repo/{user-service,community-service}.yaml` giống khối gateway.

### Định dạng lỗi

Tập field chuẩn: `timestamp, status, error, message, path, details, code` (`code` bỏ khi null), theo `services/assessment-service/.../api/exception/ErrorResponse.java`. Chỉ **thêm** field thiếu; không đổi tên, không đổi kiểu field hiện có.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | `infra/api-gateway/src/main/java/com/ieltspath/apigateway/ratelimit/{ClientKeyResolver,RateLimitProperties,ClientRateLimitGatewayFilterFactory,RateLimitConfig}.java` + test cùng package |
| Sửa | `infra/api-gateway/pom.xml`, `infra/config-server/config-repo/{api-gateway,user-service,community-service}.yaml` |
| Tạo | user-service: `application/port/LoginAttemptLimiter.java`, `application/exception/{LoginRateLimitedException,LoginLimiterUnavailableException}.java`, `infrastructure/adapter/{RedisLoginAttemptLimiter,RedisAvatarUploadRateLimiter}.java` + test |
| Sửa | user-service: `application/usecase/AuditedLoginUseCase.java`, `api/exception/GlobalExceptionHandler.java`, `pom.xml` |
| Xóa | user-service `infrastructure/adapter/InMemoryAvatarUploadRateLimiter.java`; community `infrastructure/adapter/InMemoryMediaRateLimiter.java` |
| Tạo | community: `infrastructure/adapter/RedisMediaRateLimiter.java` + test; Sửa `services/community-service/pom.xml` |
| Sửa | `services/*/src/main/java/.../api/exception/{ErrorResponse,GlobalExceptionHandler}.java` (chỉ chỗ thiếu field) |
| Sửa | controller community/notification thiếu `@Valid` (liệt kê trong báo cáo) |
| Sửa | `docs/system-architecture.md` (bảng rate limit, hành vi khi Redis lỗi) |

### Tên test bắt buộc

- `ClientRateLimitGatewayFilterFactoryTest`: `rejectsWithRetryAfterWhenBurstExceeded`, `sharesLimitAcrossTwoGatewayInstances` (2 context, 1 Redis Testcontainers), `keysByUserWhenAuthenticated`, `keysByIpWhenAnonymous`, `ignoresForwardedForFromUntrustedProxy`, `letsRequestsThroughWhenRedisIsDown`.
- `RedisLoginAttemptLimiterTest`: `blocksSixthFailureFromSamePair`, `allowsSameEmailFromOtherIp`, `opensShortWaitWindowAfterTwentyFailuresAcrossIps`, `successClearsPairCounter`, `normalizesEmailCase`, `failsClosedWhenRedisIsDown`.
- `AuditedLoginUseCaseTest` (thêm ca): `recordsFailureAndRethrowsOnBadCredentials`, `recordsSuccess`.
- `RedisAvatarUploadRateLimiterTest`, `RedisMediaRateLimiterTest`: chạy lại đúng các ca của test in-memory hiện có.
- Mỗi service một `ErrorResponseContractTest`: 400 và 404 trả đủ tập field chuẩn.

### Lệnh kiểm chứng

```
mvn -B -ntp -pl infra/api-gateway,services/user-service,services/community-service -am verify
mvn -B -ntp verify
```

### Kiểm tay (Claude)

Qua gateway thật: 6 lần login sai cùng email từ một máy → lần 6 nhận 429 có `Retry-After`; login đúng từ máy khác (IP khác) vẫn được. Dừng Redis → `GET` API thường vẫn 200, login 503.

### Dừng và hỏi

- `LoginContext` không mang IP client, hoặc `LoginUseCase` không có exception riêng cho sai thông tin.
- Không tìm được danh sách route upload/AI rõ ràng.
- Hai service có `ErrorResponse` khác kiểu ở cùng tên field (ví dụ `timestamp` là chuỗi ở chỗ này, `LocalDateTime` ở chỗ khác).
