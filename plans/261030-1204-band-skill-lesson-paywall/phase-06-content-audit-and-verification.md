---
phase: 6
title: "Content audit and verification"
status: pending
priority: P2
effort: "3h"
dependencies: [1, 2, 3, 4, 5, 7]
---

# Phase 6: Content audit and verification

## Overview

Check there is enough content to sell, set a coherent price scale, run the end-to-end scenarios and update the contracts.

## Requirements

- Functional: a count of published lessons per (course, skill) (by topic skill); the one-skill-per-topic audit from phase 2 (course topics with no skill, lessons whose skill differs from their topic); a price proposal per (course, skill) and per combo that follows the pricing guide below; scenario run; contract docs.
- Non-functional: no data fixes hidden in this phase; gaps are reported, not patched.

## Tests first (red)

- Write the end-to-end scenario checklist before running anything (see steps). Any scenario that cannot be expressed as an automated learning-service integration test stays manual and is listed as such.

## Related Code Files

- Create: audit SQL under the report, not in migrations: `plans/reports/` (counts per (course, skill)).
- Modify: `backend/docs/contracts/` for `POST /api/learning/courses/{id}/unlocks`, new course-list fields, `PAYWALL` and 409 codes; `docs/system-architecture.md` only if the access/learning flow description changes. Do not re-run the capstone doc build scripts.

## Implementation Steps

1. Run the count query on the dev DB; flag every (course, skill) with <= 3 lessons (nothing to sell) and those with 0 (Speaking).
2. Build the price table of the pricing guide, agree it with the product owner, and have R&D enter the prices in the admin course drawer (no seed migration, so R&D keeps ownership and can adjust later).
3. Scenarios: free learner lessons 1-3 open and 4 refused; buy a skill with enough Points, then thiếu Point; buy 2 skills then combo (net price); combo then skill refused; retry with same key; kill learning-service between debit and activation then reload the course list (Points taken once, unlock active); Access stopped: free and owned lessons still open, a paywalled lesson returns 503; learner with progress before go-live keeps those lessons (count `grandfathered = true` rows after V39 and check one real learner); Premium learner opens all; Premium expiry; no topic in a course keeps a Premium gate (`required_feature_key` null for all topics); Speaking not purchasable; topic without course not gated; per-skill progression (one learner with all four skills open at once, finishing a Listening topic is not needed to open Reading, a mid-course learner keeps every current in-progress topic); free practice on a point of a locked lesson is refused with no hearts spent and opens after unlock; tree shows `locked` and clears it after unlock; placement or mock evidence on a locked point still counts; the audit shows no topic without a skill and no multi-skill lesson before go-live; a free learner cannot start the topic test of a topic with locked lessons and the UI says why.
4. Update contract docs.
5. Run all three backend modules' tests and the FE suite.

## Pricing guide

Prices are per skill and per course and R&D can change them later in admin; this guide is how the first set is chosen. The code only rejects nonsense (phase 2: positive integers, combo between the highest skill price and the sum of skill prices). Everything else is a proposal.

1. Common unit: money. Convert Points with the cheapest per-Point rate among the Point keys, and take the Premium 30-day and 90-day key prices, all read from access-service data, not assumed.
2. Each skill price (money) stays below the Premium 30-day price, and the combo (money) sits between the Premium 30-day and 90-day prices. Otherwise one of the two ways to buy is never chosen.
3. The combo is cheaper than buying every skill separately by a stated discount greater than zero, decided with the product owner.
4. Locked lessons per (course, skill) = published lessons minus 3, from the audit. Show price per locked lesson and keep similar rates across skills and courses; a skill with 3 lessons or fewer has nothing to sell and gets no price.
5. State each skill price as a number of AI gradings (the Point cost of one grading) so a learner who unlocks a skill is not left unable to afford grading.
6. Output: one table per course (skill, published and locked lessons, Points, money, price per locked lesson, comparison with Premium) in `plans/reports/`, signed off by the owner before R&D enters it.

## Success Criteria

- [ ] Audit report written with counts and the price table following the pricing guide.
- [ ] R&D changes a skill price and the combo price in admin; the next purchase uses the new price, owned unlocks are untouched, invalid combinations are refused.
- [ ] Every scenario passes (automated or recorded manual result).
- [ ] Contracts updated; full test suites green.

## Risk Assessment

- Thin content (for example 6.5 Reading): the paywall would sell nothing. Decide per course whether to add content before enabling prices (null price keeps a course free).
