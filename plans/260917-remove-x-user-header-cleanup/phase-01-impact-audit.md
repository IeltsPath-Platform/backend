---
phase: 1
title: "Impact audit"
status: completed
priority: P1
dependencies: []
---

# Phase 1: Impact audit

## Overview

Confirm that `X-User-*` is no longer part of any trusted runtime path before
removing the cleanup and shared identity-header code.

## Requirements

- Functional: identify every producer, consumer, test, and doc reference to
  `X-User-Id`, `X-User-Email`, `X-User-Roles`, `SecurityHeaders.USER_*`,
  `CurrentUserHeaderFilter`, `CurrentUserHolder`, and `CurrentUser`.
- Non-functional: keep `X-Correlation-Id` untouched and preserve internal JWT
  auth behavior.

## Architecture

The target architecture is JWT-only identity. Gateway forwards
`Authorization: Bearer <internal-jwt>` to downstream services; no unsigned
identity headers participate in authentication.

## Related Code Files

- Inspect: `infra/api-gateway/src/main/java/com/group01/apigateway/filter/InternalJwtGatewayFilter.java`
- Inspect: `shared/common-security/src/main/java/com/group01/commonsecurity/header/SecurityHeaders.java`
- Inspect: `shared/common-security/src/main/java/com/group01/commonsecurity/filter/CurrentUserHeaderFilter.java`
- Inspect: `shared/common-security/src/main/java/com/group01/commonsecurity/currentuser/*`
- Inspect: `shared/common-security/src/main/java/com/group01/commonsecurity/config/CommonSecurityAutoConfiguration.java`
- Inspect: `services/user-service/src/test/java/com/group01/user/api/controller/UserControllerSecurityTest.java`
- Inspect: `README.md`

## Implementation Steps

1. Run repo-wide search for `X-User-`, `SecurityHeaders.USER_`,
   `CurrentUserHeaderFilter`, `CurrentUserHolder`, and `CurrentUser`.
2. Classify each reference as runtime code, test-only assertion, docs, or old
   plan history.
3. Confirm there is no runtime producer setting `X-User-*` anymore.
4. Confirm current service identity comes from Spring Security JWT principal,
   not `CurrentUserHolder`.
5. Decide final deletion list and note anything that must stay for
   compatibility.

## Success Criteria

- [x] Audit proves no runtime path depends on `X-User-*` for identity.
- [x] Deletion list is explicit and limited.
- [x] `X-Correlation-Id` remains in scope as retained infrastructure header.

## Risk Assessment

Risk: deleting the shared current-user holder could break a hidden service added
later. Mitigation: use repo-wide search and Maven module compile before
deleting; if an external consumer exists outside this repo, publish this as a
breaking cleanup.
