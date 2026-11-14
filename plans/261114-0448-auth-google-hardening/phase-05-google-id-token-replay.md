---
phase: 5
title: "Chống phát lại ID token Google (G1)"
status: pending
priority: P1
dependencies: [1]
---

# Phase 5: ID token Google: tuổi token và nonce

## Vấn đề
`NimbusGoogleIdTokenVerifier` kiểm `iss`, `aud`, `azp`, `exp`, chữ ký và `email_verified`, nhưng không kiểm `iat` hay
`nonce`. ID token Google sống khoảng một giờ; ai có được token (log client, tiện ích trình duyệt, proxy) đều dùng lại
được tại `/auth/google` đến khi hết hạn.

## Thiết kế

### 1. Giới hạn tuổi token (không cần sửa client)
- `GoogleAuthProperties`: `maxTokenAgeSeconds` (`app.auth.google.max-token-age-seconds`, mặc định 300; `0` = tắt).
- `validateGoogleClaims`: bắt buộc có `iat`; từ chối nếu `now - iat > maxTokenAge + 60` (dung sai đồng hồ 60 giây) hoặc
  `iat` ở tương lai quá 60 giây. Lỗi trả `AuthenticationFailedException` (401) như token sai, không phân biệt lý do với
  client; metric `auth.google.signin{result=stale_token}`.
- Client phải gửi token ngay sau khi nhận (One Tap, Credential Manager đều cấp token mới mỗi lần).

### 2. Nonce do server cấp, dùng một lần (triển khai theo cờ)
- Route công khai mới `POST /auth/google/nonce` (**cần duyệt**): trả `{ "nonce": "<43 ký tự base64url>",
  "expiresInSeconds": 300 }` kèm `Cache-Control: no-store`. Sinh bằng `SecureRandom` 32 byte.
- Lưu Redis: khóa `auth:google:nonce:{SHA-256(nonce) dạng hex}` giá trị `1`, TTL 300 giây (chỉ lưu hash, không lưu nonce thô).
- Client đưa nonce vào yêu cầu Google (Web: tham số `nonce` của Google Identity Services; Android: `setNonce` của
  Credential Manager); Google ghi lại vào claim `nonce` của ID token.
- `GoogleIdentity` thêm trường `nonce`. Trong `GoogleSignInUseCase`, **sau khi** chữ ký hợp lệ (không để người chưa có token
  Google đốt nonce): nếu token có `nonce` → phải `consume` thành công (`GETDEL` nguyên tử), không thì 401
  (`result=replay`); nếu token không có `nonce` → chỉ được phép khi `app.auth.google.require-nonce=false`.
- Cờ `app.auth.google.require-nonce` (mặc định `false`); bật `true` sau khi cả web và mobile đã gửi nonce.
- Cổng `application/port/GoogleNonceStore` (`issue()`, `consume(nonce)`), adapter Redis ở `infrastructure`. Redis lỗi khi
  `consume`: **đóng** (từ chối, 503 `GOOGLE_SIGNIN_UNAVAILABLE`) nếu `require-nonce=true`, **mở** (bỏ qua) nếu `false`.

### Gateway
Thêm `/auth/google/nonce` vào `gateway.security.public-endpoints` (method `POST`) và vào danh sách route tier `auth`
trong `config-repo/api-gateway.yaml`; cập nhật `AuthRouteConsistencyTest` theo AGENTS §3.9.

### Triển khai theo bước
1. Phát hành kiểm `iat` (không đổi client).
2. Phát hành endpoint nonce, `require-nonce=false`; client web và mobile bắt đầu gửi nonce.
3. Khi số lần đăng nhập không nonce về 0 (metric), đặt `require-nonce=true`.

## Files
- user-service: `config/GoogleAuthProperties.java`, `infrastructure/adapter/NimbusGoogleIdTokenVerifier.java`,
  `application/result/GoogleIdentity.java`, `application/usecase/GoogleSignInUseCase.java`,
  `application/port/GoogleNonceStore.java`, `infrastructure/adapter/RedisGoogleNonceStore.java`,
  `api/controller/AuthController.java` (endpoint nonce), `api/dto/response/GoogleNonceResponse.java`.
- api-gateway: `config-repo/api-gateway.yaml`, `AuthRouteConsistencyTest`.
- Tài liệu: `docs/contracts/google-sign-in-v1.md` (endpoint nonce, mã lỗi, hướng dẫn client web và Android).

## Tests
- `NimbusGoogleIdTokenVerifierTest`: `iat` quá cũ bị từ chối; trong dung sai được nhận; `iat` tương lai bị từ chối; thiếu
  `iat` bị từ chối; `maxTokenAge=0` tắt kiểm tra.
- `GoogleSignInUseCaseTest`: token có nonce hợp lệ dùng được đúng một lần, lần hai 401; nonce lạ 401; thiếu nonce khi
  `require-nonce=true` 401, khi `false` được nhận; nonce không bị đốt khi chữ ký sai.
- Redis (Testcontainers hoặc cách `RedisLoginAttemptLimiter` đang test): TTL, `GETDEL` nguyên tử khi hai request đồng thời.
- `@WebMvcTest`: endpoint nonce trả đúng định dạng, `Cache-Control: no-store`, không cần xác thực.
- `AuthRouteConsistencyTest` xanh sau khi thêm route.

## Success Criteria
- [ ] ID token cũ hơn 5 phút bị 401; token mới vẫn đăng nhập được.
- [ ] Với `require-nonce=true`, phát lại cùng một ID token lần hai bị 401.
- [ ] Không log ID token hay nonce; chỉ lưu hash nonce trong Redis.
- [ ] Contract và hướng dẫn client có trong cùng PR; `mvn -q -pl services/user-service -am test` và
  `mvn -q -pl infra/api-gateway test` xanh.
