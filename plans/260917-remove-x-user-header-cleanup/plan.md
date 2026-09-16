---
title: "Remove X-User header cleanup"
description: "Remove obsolete X-User identity header cleanup now that downstream services trust only gateway-signed internal JWTs."
status: in-progress
priority: P2
branch: "main"
tags: [auth, security, api-gateway, common-security, cleanup]
blockedBy: []
blocks: [260916-1054-gateway-signed-internal-jwt]
created: "2026-09-16T18:26:56.434Z"
createdBy: "ck:plan"
source: skill
---

# Remove X-User header cleanup

## Overview

Remove the obsolete `X-User-*` trust-boundary remnants from the codebase.

Current auth direction is already JWT-only:

```text
Client external JWT -> API Gateway verifies token
                    -> Gateway signs short-lived internal JWT
                    -> user-service verifies internal JWT
```

Because the gateway no longer sets `X-User-Id`, `X-User-Email`, or
`X-User-Roles`, the cleanup should remove the defensive strip lines and the
dead shared header identity reader. Keep `common-security` only for still-used
infrastructure concerns such as `X-Correlation-Id`.

## Implementation Status

- `InternalJwtGatewayFilter` now removes only the client `Authorization`
  header before adding a gateway-signed internal JWT for protected paths.
- `SecurityHeaders` now keeps only `CORRELATION_ID`.
- `CurrentUserHeaderFilter`, `CurrentUserHolder`, and `CurrentUser` were
  removed from `shared/common-security`.
- `CommonSecurityAutoConfiguration` no longer registers a header-identity
  filter.
- `user-service` tests still send fake raw `X-User-*` headers to verify they
  cannot authenticate a request; tests were intentionally not modified in this
  pass.
- README no longer describes active gateway `X-User-*` stripping.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Impact audit](./phase-01-impact-audit.md) | Complete |
| 2 | [Code cleanup](./phase-02-code-cleanup.md) | Complete |
| 3 | [Docs and verification](./phase-03-docs-and-verification.md) | Partial |

## Dependencies

- Blocks/supersedes part of
  [260916-1054-gateway-signed-internal-jwt](../260916-1054-gateway-signed-internal-jwt/plan.md)
  because that older plan still treats `X-User-*` stripping as an explicit
  gateway invariant.

## Acceptance Criteria

- [x] `InternalJwtGatewayFilter` no longer imports `SecurityHeaders` solely to
  remove `X-User-*`.
- [x] `SecurityHeaders` keeps `CORRELATION_ID` but no longer defines
  `USER_ID`, `USER_EMAIL`, or `USER_ROLES`.
- [x] `CurrentUserHeaderFilter`, `CurrentUserHolder`, and `CurrentUser` are
  removed or made unreachable if no runtime caller remains.
- [x] `CommonSecurityAutoConfiguration` no longer registers a header-identity
  filter.
- [ ] user-service remains protected by internal JWT validation; raw
  `X-User-*` headers still cannot authenticate a request.
- [x] README/docs no longer say gateway actively strips `X-User-*` as a current
  requirement.
- [x] Focused compile passes for `shared/common-security`, `api-gateway`, and
  `user-service`.
- [ ] Focused tests pass for `api-gateway` and `user-service`.

## Out Of Scope

- Do not remove `X-Correlation-Id`.
- Do not remove the `shared/common-security` module unless a separate audit
  proves it has no remaining purpose.
- Do not change external/internal JWT payload shape.
- Do not change gateway route matching or internal JWT signing.
