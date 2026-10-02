# Reading Hints Follow History, Not Mastery

**Date**: 2026-10-02
**Severity**: Low
**Component**: Learning Service
**Status**: Resolved

## What Happened

Content V13 already supplied hints; Learning lacked the policy and history-based disclosure. Implementation now exposes eligible hints after wrong answers.

## State Boundaries

Hint history and mastery evidence have different scopes. Hint history includes every wrong retry; mastery evidence remains limited to the first submission. Keeping those queries separate preserves the existing learning behavior.

## Technical Details

Eligibility is pure: valid FILL or CHOICE with ≥3 options; zero options uses the TFNG fallback. A missing answer-spec type follows the grader's legacy CHOICE default. Wrong answers come from all saved responses, scoped by user/lesson/block. Hints survive later correct answers until the block has ever passed.

GET/POST always include `hint`, including null, preventing key presence from revealing eligibility. Reviews always return null. Replay returns the exact stored response after passing or content changes, without querying Content.

## What We Tried

TDD RED exposed the missing policy. Baseline: 175 Learning +8 shared tests. Focused: 76. Regression: 183+8, zero failures/errors/skips with Docker. Final narrow check: one passed. No full reactor, live E2E, or real LLM verification.

## Root Cause Analysis

Authoring data existed, but Learning had no disclosure rule. First-only evidence cannot represent every wrong retry.

## Lessons Learned

Reuse saved history instead of adding hint flags or a Learning migration. Preserve first-attempt evidence and the mastery formula; keep replay independent of current content.

## Next Steps

Implementation review completed with no defects found. Plan statuses and local commits record the completed work; deployment is outside this task.
