---
phase: 2
title: "Đổi code Gateway và User Service"
status: completed
priority: P1
dependencies: [1]
effort: "medium"
---

# Phase 2: Đổi code Gateway và User Service

## Overview

Đổi encoder/decoder JWT trong gateway và user-service từ RSA key sang HMAC
secret. Giữ nguyên các validator issuer/audience/type/sub/roles và luồng strip
header hiện tại.

## Requirements

- Functional: token mới ký bằng `HS256`, gateway và user-service vẫn reject
  token sai issuer/audience/type/sub/role.
- Non-functional: code đơn giản, không introduce custom crypto; dùng Spring
  Security/Nimbus API có sẵn.

## Architecture

Spring Security path sau khi đổi:

```text
String secret -> SecretKeySpec("HmacSHA256")
              -> NimbusJwtEncoder / NimbusJwtDecoder / NimbusReactiveJwtDecoder
              -> validators hiện tại
```

Không còn cần `RsaKeyLoader`.

## Related Code Files

- Delete: `services/user-service/src/main/java/com/group01/user/config/RsaKeyLoader.java`
- Delete: `infra/api-gateway/src/main/java/com/group01/apigateway/security/RsaKeyLoader.java`
- Modify: `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java`
- Modify: `services/user-service/src/main/java/com/group01/user/application/usecase/JwtTokenService.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtService.java`

## Implementation Steps

1. Tạo helper nhỏ nếu cần, ví dụ `HmacKeyFactory` hoặc private method:

   ```java
   SecretKey secretKey(String secret) {
       byte[] key = decodeBase64OrUtf8(secret);
       return new SecretKeySpec(key, "HmacSHA256");
   }
   ```

   Ưu tiên đặt helper package-private trong từng module hoặc shared local
   method, không tạo abstraction lớn.

2. user-service external JWT signer:
   - `JwtEncoder jwtEncoder(...)` dùng `externalJwtSecret`.
   - Thay `JwsHeader.with(SignatureAlgorithm.RS256)` trong `JwtTokenService`
     bằng `JwsHeader.with(MacAlgorithm.HS256)`.

3. user-service internal JWT verifier:
   - `JwtDecoder` dùng:

     ```java
     NimbusJwtDecoder.withSecretKey(internalSecretKey)
         .macAlgorithm(MacAlgorithm.HS256)
         .build()
     ```

   - Giữ nguyên validators:
     issuer `urn:code-base:api-gateway`, audience `user-service`,
     `type=gateway-internal`, UUID subject, role `ADMIN|LEARNER`.

4. gateway external JWT verifier:
   - `ReactiveJwtDecoder` dùng:

     ```java
     NimbusReactiveJwtDecoder.withSecretKey(externalSecretKey)
         .macAlgorithm(MacAlgorithm.HS256)
         .build()
     ```

   - Giữ nguyên validators external: issuer, audience `api-gateway`,
     `type=access`, subject UUID, roles canonical.

5. gateway internal JWT signer:
   - `JwtEncoder internalJwtEncoder(...)` dùng `internalJwtSecret`.
   - `InternalJwtService` đổi header sang `MacAlgorithm.HS256`.
   - Giữ `kid` nếu muốn logging/rotation label, nhưng không dùng `kid` để chọn
     RSA key nữa.

6. Xóa imports RSA/Nimbus JWK không còn dùng:
   - `RSAKey`, `JWKSet`, `ImmutableJWKSet`, `SecurityContext`
   - `RSAPublicKey`, `RSAPrivateKey`, `KeyFactory`, `X509EncodedKeySpec`,
     `PKCS8EncodedKeySpec`.

7. Không tạo file test mới. Nếu implementation khiến test hiện có không compile,
   chỉ cập nhật fixture/property trong file test hiện có hoặc chạy compile với
   `-DskipTests` theo acceptance của plan.

## Success Criteria

- [x] Main code không còn `RsaKeyLoader`.
- [x] Main code không còn JWT `SignatureAlgorithm.RS256`.
- [x] Gateway vẫn strip `Authorization`/`X-User-*` và chỉ gắn internal JWT cho
  route/audience đã allowlist.
- [x] user-service vẫn bật resource server security để verify internal HMAC JWT.
- [x] Không tạo file test mới.

## Outcome

- `NimbusJwtDecoder` và `NimbusReactiveJwtDecoder` chuyển sang `withSecretKey`
  + `MacAlgorithm.HS256`.
- Token signer trong user-service và gateway chuyển sang `MacAlgorithm.HS256`.
- Xóa `RsaKeyLoader` ở cả hai module.

## Risk Assessment

Nguy cơ lớn nhất là vô tình dùng cùng secret cho external và internal JWT. Khi
cook, phải grep lại property/env để bảo đảm hai secret riêng. Nguy cơ thứ hai là
secret quá ngắn; nên fail-fast hoặc ít nhất document yêu cầu 32 bytes random.
