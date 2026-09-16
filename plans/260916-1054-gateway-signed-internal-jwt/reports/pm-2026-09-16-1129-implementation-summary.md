---
title: "Gateway-signed internal JWT implementation summary"
created: "2026-09-16T11:29:46+07:00"
status: "done-with-follow-ups"
---

# Implementation Summary

## Completed

| Area | Result |
|------|--------|
| Gateway external JWT | Verifies RS256 external access tokens with issuer/audience/type/subject/role validation. |
| Gateway internal JWT | Signs short-lived RS256 internal JWTs for configured downstream audiences. |
| Header trust boundary | Removes client `Authorization` and `X-User-*` before forwarding. |
| User service auth | Verifies gateway-signed internal JWTs only for protected APIs. |
| Common security | Removed `common-security` runtime identity dependency from user-service. |
| Config/compose | Removed shared `JWT_SECRET`; removed direct user-service host port; disabled broad gateway discovery locator. |
| Docs/tests | README updated; gateway/user-service tests updated. |

## Verification

Command:

```powershell
mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am test
```

Result:

- Build: success
- Tests: 23
- Failures: 0
- Errors: 0
- Skipped: 1 (`RefreshTokenPostgresIntegrationTest`, no valid Docker/Testcontainers environment)

## Follow-ups

- Add key rotation runbook/config for current, previous, and retired public keys.
- Add deeper gateway end-to-end filter tests for header stripping and arbitrary route non-signing.
- Consider gateway rate limiting for `/auth/login`, `/auth/refresh`, `/auth/logout`, and `/api/users/register`.
