---
phase: 6
title: "Content audit and verification"
status: pending
priority: P2
effort: "3h"
dependencies: [1, 2, 3, 4, 5]
---

# Phase 6: Content audit and verification

## Overview

Check there is enough content to sell, set a coherent price scale, run the end-to-end scenarios and update the contracts.

## Requirements

- Functional: a count of published lessons per (course, skill) (by primary skill, Premium-only topics excluded); a price proposal per course; scenario run; contract docs.
- Non-functional: no data fixes hidden in this phase; gaps are reported, not patched.

## Tests first (red)

- Write the end-to-end scenario checklist before running anything (see steps). Any scenario that cannot be expressed as an automated learning-service integration test stays manual and is listed as such.

## Related Code Files

- Create: audit SQL under the report, not in migrations: `plans/reports/` (counts per (course, skill)).
- Modify: `backend/docs/contracts/` for `POST /api/learning/courses/{id}/unlocks`, new course-list fields, `PAYWALL` and 409 codes; `docs/system-architecture.md` only if the access/learning flow description changes. Do not re-run the capstone doc build scripts.

## Implementation Steps

1. Run the count query on the dev DB; flag every (course, skill) with <= 3 lessons (nothing to sell) and those with 0 (Speaking).
2. Pick Point prices with the product owner using the Point cost of one AI grading as the unit; set them via the admin form or a seed migration agreed with the owner.
3. Scenarios: free learner lessons 1-3 open and 4 refused; buy a skill with enough Points, then thiếu Point; buy 2 skills then combo (net price); combo then skill refused; retry with same key; kill learning-service between debit and activation then reload the course list (Points taken once, unlock active); Access stopped: free and owned lessons still open, a paywalled lesson returns 503; learner with progress before go-live keeps those lessons (count `grandfathered = true` rows after V39 and check one real learner); Premium learner opens all; Premium expiry; Premium-only topic without Premium; Speaking not purchasable; topic without course not gated.
4. Update contract docs.
5. Run all three backend modules' tests and the FE suite.

## Success Criteria

- [ ] Audit report written with counts and price proposal.
- [ ] Every scenario passes (automated or recorded manual result).
- [ ] Contracts updated; full test suites green.

## Risk Assessment

- Thin content (for example 6.5 Reading): the paywall would sell nothing. Decide per course whether to add content before enabling prices (null price keeps a course free).
