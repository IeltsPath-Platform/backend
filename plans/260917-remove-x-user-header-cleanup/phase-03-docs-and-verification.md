---
phase: 3
title: "Docs and verification"
status: in-progress
priority: P2
dependencies: [2]
---

# Phase 3: Docs and verification

## Overview

Update documentation and run focused verification so the repo consistently
describes JWT-only identity without `X-User-*` cleanup leftovers.

## Requirements

- Functional: update README and overlapping plan notes that currently describe
  gateway `X-User-*` stripping as an active invariant.
- Non-functional: preserve historical context where useful, but mark it as
  superseded rather than current behavior.

## Architecture

Docs should say: client identity is carried by external JWT to gateway; gateway
issues internal JWT to services; downstream services verify internal JWT and do
not read raw identity headers.

## Related Code Files

- Modify: `README.md`
- Modify: `plans/260916-1054-gateway-signed-internal-jwt/plan.md`
- Optional modify: historical phase docs only if they are used as current
  handoff docs.

## Implementation Steps

1. Replace README statements such as "Gateway strips `Authorization` and
   `X-User-*`" with current wording: "Gateway replaces client
   `Authorization` with gateway-signed internal JWT; services ignore raw
   identity headers."
2. Update "add new microservice" guidance to say new services should use
   internal JWT, not `X-User-*`.
3. Update the older gateway-signed JWT plan frontmatter/body to reference this
   cleanup plan as superseding its old `X-User-*` strip invariant.
4. Run focused compile/tests:

   ```powershell
   mvn -q -pl shared/common-security,infra/api-gateway,services/user-service -DskipTests compile
   mvn -q -pl infra/api-gateway,services/user-service test
   ```

5. Re-run search:

   ```powershell
   rg -n "SecurityHeaders\\.USER_|CurrentUserHeaderFilter|CurrentUserHolder|X-User-" .
   ```

   Remaining matches should be docs/history only, not main runtime code.

## Success Criteria

- [x] README current behavior no longer claims `X-User-*` stripping is active.
- [x] Older plan dependency is documented as superseded by this cleanup.
- [x] Focused compile command passes with `-DskipTests`.
- [ ] Focused test command passes, aside from known Docker/Testcontainers skips
  if Docker is unavailable.
- [x] Final search output has no runtime `X-User-*` identity path.

## Risk Assessment

Risk: docs may still contain old "strip X-User" statements in completed plan
history. Mitigation: update active README and active/in-progress plans; leave
completed historical artifacts only when clearly historical.
