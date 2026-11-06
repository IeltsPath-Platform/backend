# Comment reactions verification

Date: 2026-11-07 (Asia/Bangkok). Branch: `feat/comment-reactions`. Plan status: `completed` (backend implementation; live gateway E2E remains unverified).

## Summary

Backend implementation and source review are complete, with no design deviation. The final clean module run passed 244 tests with zero failures, errors, or skips (exit 0, 2 minutes 07 seconds). This includes all 13 tests in `CommunityOutboxUseCaseIntegrationTest` and the V10 upgrade test in `CommentReactionMigrationTest`. Live gateway runtime verification has not been run; this report does not claim E2E completion.

The clean run includes the final timestamp, repository-result, and zero/null response assertions. Moderation response enrichment follows the requirement that every `CommentResponse` includes counts and the viewer's reaction.

## Evidence checklist

- [x] TDD sequence confirmed by the tester: new tests first produced compile RED because the new commands, repository, and use cases were absent; implementation and affected caller updates followed.
- [x] `AddCommentReactionUseCase` and `RemoveCommentReactionUseCase` use `CommunityApplicationSupport.requireVisibleComment`, validate the containing post, and apply the existing administrator visibility exception. `CommentReactionUseCaseTest` covers all four types, own comments/replies, missing/hidden/deleted content, and administrator behavior.
- [x] `CommunityController` exposes authenticated PUT/DELETE comment reaction endpoints with 204 and empty bodies. `CommunitySecurityTest.commentReactionEndpointsRequireAuthenticationAndRejectUnknownTypes` and `commentReactionsForwardAuthenticatedActorAndReturnNoContent` cover 401, 400, identity forwarding, and success responses.
- [x] `CommentInteractions` zero-fills four reaction keys; `CommunityQueriesUseCase.enrichComments` uses the current viewer. `CommunityQueriesUseCaseTest.commentsBatchInteractionsOnceAndUseCurrentViewerWithFourZeroDefaults`, `emptyCommentsAvoidAllEnrichmentQueries`, and `threadedRootsAndRepliesBothEnrichUsingViewer` cover enrichment and empty pages.
- [x] `CommunitySecurityTest.everyCommentResponsePathIncludesCountsAndViewerReaction` covers flat lists, threaded lists, reply lists, create, edit, and moderation responses.
- [x] `CommunityOutboxUseCaseIntegrationTest.commentReactionReplaceRepeatAndWrongTypeRemovePreserveOneRowAndNoOpTimestamp` passed: replace keeps one row, repeating a type preserves the timestamp, and missing/different-type removal is a no-op.
- [x] `CommunityOutboxUseCaseIntegrationTest.twentyCommentsUseOneSqlForCountsAndViewerReaction` passed: Hibernate statistics recorded exactly one prepared statement for 20 comment IDs; counts and each viewer's reaction are checked together. The adapter calls the grouped `CommentReactionJpaRepository.interactions` SQL once.
- [x] `CommunityOutboxUseCaseIntegrationTest.concurrentDifferentReactionsBySameViewerLeaveExactlyOneRow` passed with simultaneous transactions for the same comment/user and different types. The composite primary key and `ON CONFLICT` upsert enforce one row.
- [x] `CommentReactionMigrationTest.upgradesExistingV10PostsCommentsAndReactionsWithoutChangingRows` passed: V11 preserves existing posts, comments/replies and post reactions, accepts the four types, rejects invalid types/missing comments, and creates the comment/type index.
- [x] `CommunityOutboxUseCaseIntegrationTest.softModerationKeepsCommentReactionsAndPhysicalDeleteCascades` passed: soft status changes preserve reactions; physical comment deletion cascades them.
- [x] Source review found no concrete blockers or stale constructor/signature callers. `git diff --check` passed. Comment reaction use cases do not use the event outbox; author notifications remain outside scope, with no shared-events or notification-service changes.

## Final verification

| Check | Result |
| --- | --- |
| Final clean community-service test command | `$env:JAVA_HOME='D:/jdk-21'; mvn.cmd -pl services/community-service -o clean test` |
| Total tests | **244** |
| Failures / errors / skipped | **0 / 0 / 0** |
| Exit code / duration | **0 / 2 minutes 07 seconds** |
| Runtime PUT/DELETE through gateway and subsequent response reads | **Not run** — existing `/api/community/**` route unchanged; no E2E claim |

Final clean-run log: `C:/Users/Admin/AppData/Local/Temp/community-comment-reactions-clean-test.log` (local verification artifact, not committed). Any later gateway runtime verification should be recorded separately. FE/mobile integration remains the follow-up described in the original plan.

## References

- [Implementation plan](../plan.md)
- [API contract](../../../docs/contracts/comment-reactions-v1.md)
