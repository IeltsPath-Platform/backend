# Brainstorm: band → skill → lesson, 3 free lessons, unlock per skill or combo

Date: 2026-10-30. Status: design approved by user, not planned/implemented.

## Problem

Monetise courses: let learners try the first 3 lessons of each skill free, then buy a single skill or the whole band (combo). Underlying need: a trial that shows value + a purchase path that fits existing sales (activation keys made by Sale staff).

## Current state (scouted)

- `courses` (1 per band, `band_level` unique) → `topics` (`course_id`, `skill`) → `lessons`. Topic layer sits between skill and lesson; a skill can hold several topics.
- Access is binary + per user: `plans` FREE/PREMIUM, `subscriptions` (user, plan, `ends_at`), feature `PREMIUM_CONTENT`. Key products: `PREMIUM_30D/90D`, `POINT_50/100`. No course/skill scope.
- Gating via `required_feature_key` on topics/packages/questions. Lessons have no gate.
- FE `mapTopicSummary` always LOCKs premium topics ("chưa mở trong giai đoạn này").
- Point wallet/ledger/debit (idempotent) exists; precedent for spending Points on a non-grading purpose: `HEART_REFILL`.
- No Speaking topics seeded (L/R/W only). Active courses: 5.5, 6.5; 6.0/7.0 inactive.

## Decisions (user)

| Topic | Decision |
|---|---|
| Duration | Lifetime purchase |
| Currency | Points (existing wallet). Premium 30/90d opens everything while active |
| Scope of a skill purchase | Lessons of that skill (+ its topic tests) only; AI/human grading still use Points/credits |
| "3 first lessons" unit | Count lessons in learning order within (band, skill), across topics; keep topic layer |
| Upgrade skill → combo | Combo price minus Points already spent on that band's skills (min 0) |
| Combo meaning | All skills of the band, including skills added later (e.g. Speaking) |
| Sequential order | Keep existing "pass previous to continue"; UI distinguishes paywall lock vs progress lock |

## Approaches considered

- **A (chosen)** Scoped unlock rows in learning-service + Point debit via Access + rule-based "first 3". Least new surface; Access only gains a transaction type.
- B. Per-lesson `is_free_preview` flag: flexible, more author toil, drift from "3". Rejected (YAGNI).
- C. Per (band, skill) feature keys/plans: plan explosion, still time-based subscription. Rejected.

## Design

Open a lesson if any: Premium active; lesson position ≤ 3 in (course, skill); learner owns (course, skill); learner owns combo (course, all). Premium-only topics (`required_feature_key`) stay Premium-only.

- content-service `courses`: add `skill_unlock_points`, `combo_unlock_points` (null = not for sale); passed through internal DTO to learning-service.
- learning-service: table `learner_course_unlocks(user_id, course_id, skill NULL=combo, point_cost, ledger_entry_id, created_at)`, unique per (user, course, skill). `POST /api/learning/courses/{id}/unlocks` with idempotency key; price computed server-side; debit then insert, retry-safe; per-user lock. Enforce rule on lesson open / practice start / submit endpoints, not only UI.
- access-service: add `COURSE_UNLOCK_DEBIT` transaction type (migration like V4 + `DebitPointsUseCase` and `PointDebitWriter` switch).
- FE: paywall dialog (skill price, combo price after deduction, balance, top-up via `KeyActivationDialog`, Premium hint); remove permanent premium LOCK in adapter; two price fields in admin `CourseForm`; Speaking shows "sắp ra mắt".

## Out of scope

Keys that unlock a course directly, refunds of unlocks, gating practice sets / mock tests / grading by skill, Speaking content, per-lesson free flags, mobile.

## Acceptance criteria

- Free learner reaches only the first 3 lessons of each (band, skill).
- Skill or combo purchase opens the rest; insufficient Points rejected; Premium opens all.
- Same idempotency key never debits twice.
- Calling lesson/practice APIs directly for a locked lesson is rejected.
- Upgrade price follows the deduction rule.

## Risks

- **Content volume**: a (band, skill) with ≤ 3 lessons is fully free, nothing to sell. Count lessons per (band, skill) before pricing; 6.5 Reading looks thin.
- **One currency, two uses**: unlock price must sit on the same scale as AI-grading cost, or learners starve grading.
- **Two-step purchase**: Sale sells Point key → learner redeems → spends. Direct course keys can come later.
- **Premium expiry**: Premium-only access closes, Point-bought stays; UI must show the difference.
- **Position calc**: depends on stable topic order and lesson `sort_order`; inactive topics/lessons must not shift positions of free lessons.

## Unresolved questions

- Point prices per skill and per combo, and the Point cost of one AI grading, to set a coherent scale.
- Should existing Premium-only topics (e.g. `PREMIUM_MATCHING_INFO`) count toward the "3 free" position or be skipped? (Default: skipped, stay Premium-only.)
- Exact lesson counts per (band, skill) today.
