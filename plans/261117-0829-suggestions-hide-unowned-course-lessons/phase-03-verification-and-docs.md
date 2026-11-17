---
phase: 3
title: Verification and docs
status: completed
priority: P2
dependencies:
  - 1
  - 2
effort: S
---

# Phase 3: Verification and docs

## Overview
Update the internal contract, run the affected suites, and check the scenario end to end.

## Requirements
- `docs/contracts/learning-content-internal-v1.md`, section `POST /suggestions/candidates`: document `lessons[].courseId` (UUID) and `lessons[].skillPosition` (int, same definition as lesson summaries), additive, older Learning ignores them.
- No public API, event or schema change; no migration.

## Related Code Files
- Modify: `docs/contracts/learning-content-internal-v1.md`
- Read-only check: `docs/system-architecture.md` (no new HTTP edge: Learning → Content already exists, so the dependency table is unchanged)

## Implementation Steps
1. Edit the contract section; keep the example JSON in sync with the DTO.
2. Run:
   - `mvn -q -pl services/content-service -am test`
   - `mvn -q -pl services/learning-service -am test`
   - `mvn -q -pl services/learning-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`
   Report skipped Testcontainers tests if Docker is unavailable; do not call them passed.
3. Manual check (local stack): learner with combo unlock on the 6.5 course only → `GET /api/learning/suggestions` contains no 5.0 lesson beyond position 3 per skill; grant Premium → they can appear.
4. `graphify update .` in `backend/`.

## Success Criteria
- [ ] Contract updated in the same PR as code.
- [ ] Both service suites and ArchitectureTest pass (or skips are reported).
- [ ] Manual scenario matches the plan's acceptance criteria.

## Risk Assessment
- CI runs Testcontainers that local runs may skip; rely on CI for the repository IT if Docker is missing locally.
