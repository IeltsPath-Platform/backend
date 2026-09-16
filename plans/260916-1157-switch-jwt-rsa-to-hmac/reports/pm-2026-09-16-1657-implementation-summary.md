---
title: "Switch JWT RSA to HMAC implementation summary"
created: "2026-09-16T16:57:00+07:00"
status: "completed"
---

# Implementation Summary

## Completed

| Area | Result |
|------|--------|
| User-service external JWT | Signs access tokens with `EXTERNAL_JWT_SECRET` and HS256. |
| User-service internal verifier | Verifies gateway internal JWTs with `GATEWAY_INTERNAL_JWT_SECRET` and HS256. |
| API Gateway external verifier | Verifies client access tokens with `EXTERNAL_JWT_SECRET` and HS256. |
| API Gateway internal signer | Signs downstream internal JWTs with `GATEWAY_INTERNAL_JWT_SECRET` and HS256. |
| RSA cleanup | Removed `RsaKeyLoader` from gateway and user-service main code. |
| Config/docs | Config server, compose, and README now use HMAC secret env names. |

## Verification

Command:

```powershell
mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -DskipTests compile
mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -q test
```

Result:

- Build: success
- Tests: 26 discovered, 25 passed, 1 skipped
  (`RefreshTokenPostgresIntegrationTest` skipped because no Docker/Testcontainers
  environment was available), 0 failures, 0 errors
- New test files: none

## Notes

- HMAC requires keeping `EXTERNAL_JWT_SECRET` and `GATEWAY_INTERNAL_JWT_SECRET`
  separate.
- Secret values must be at least 32 bytes. Raw secret strings are accepted;
  base64 values must use the `base64:` prefix.
