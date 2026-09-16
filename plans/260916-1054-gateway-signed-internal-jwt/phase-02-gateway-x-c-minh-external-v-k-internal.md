---
phase: 2
title: "Gateway xác minh external và ký internal"
status: completed
priority: P1
dependencies: [1]
---

# Phase 2: Gateway xác minh external và ký internal

## Overview

Gateway trở thành nơi duy nhất nhận external access token từ client. Sau khi
verify, gateway tạo internal JWT theo audience của downstream rồi forward.

## Requirements

- Functional: protected route phải có external token hợp lệ trước khi gateway ký
  internal token.
- Non-functional: không lấy downstream audience từ client-controlled header/path
  tùy ý; mapping phải rõ ràng theo route config.

## Architecture

Gateway filter chain:

```text
request -> strip Authorization/X-User-* -> verify external token
        -> resolve route audience -> sign internal JWT -> forward
```

Public route như login/register/refresh không cần internal JWT. Với public route,
gateway vẫn phải strip identity headers từ client để downstream không vô tình
đọc identity giả.

## Related Code Files

- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/JwtUserHeaderGatewayFilter.java`
- Create: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtService.java`
- Create: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtGatewayFilter.java`
- Create: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwksController.java` if JWKS endpoint is chosen
- Modify: `infra/config-server/config-repo/api-gateway.yaml`

## Implementation Steps

1. Tighten gateway external decoder validation: issuer, audience
   `api-gateway`, `type=access`, UUID subject, expiry, algorithm, and canonical
   roles only.
2. Replace header projection filter with internal JWT filter. The new filter
   must remove inbound `Authorization`, `X-User-Id`, `X-User-Email`,
   `X-User-Roles` before adding trusted downstream auth.
3. Add route-to-audience mapping, starting with `user-service`. Disable broad
   discovery locator signing or put it behind an explicit allowlist; unknown
   routes must never receive an internal JWT.
4. Sign internal JWT with gateway private key and `kid`; include minimal claims:
   `sub`, `roles`, `iss`, `aud`, `type`, `iat`, `exp`, `jti`. Omit email unless
   a concrete downstream endpoint still needs it.
5. Ensure public endpoints are passthrough without internal identity assertion.
6. Emit structured logs/metrics for rejected external tokens and internal token
   issuance without logging JWT bodies or secrets.

## Success Criteria

- [x] Gateway rejects wrong issuer/audience/type/role/sub external tokens.
- [x] Gateway signs internal JWT only after successful external authentication.
- [x] Forwarded protected requests contain exactly one trusted bearer token: the
  gateway-signed internal JWT.
- [x] Client-supplied identity headers are stripped on both public and protected routes.
- [x] Discovery locator is disabled for signed routes or constrained by an explicit
  service allowlist.
- [x] Route audience cannot be influenced directly by client input.

## Outcome

- Replaced `JwtUserHeaderGatewayFilter` with `InternalJwtGatewayFilter`.
- Gateway strips inbound `Authorization` and `X-User-*`, then signs internal JWTs
  only for configured downstream audiences.
- Added explicit `/api/users` and `/auth/me` audience mapping for `user-service`.

## Risk Assessment

Main risk is signing tokens for the wrong service because of dynamic discovery
or path rewriting. Mitigation: require explicit route audience config and test
that unknown routes do not get internal tokens.
