# Vocabulary study backend handoff

Implementation date: 2026-11-07. Public URLs, enum values, request/response examples, coded errors and payload limits remain in [the API contract](../api-contract.md). Frontend authoring and mobile integration are outside this backend delivery.

## Acceptance evidence

| Contract requirement | Automated evidence |
|---|---|
| Curated seed, ordering, correct counts, nonempty valid content | StudyUnitSeedMigrationTest checks both topics, every unit and publication metadata against the real generator |
| Distinct sense IDs, all four exercise types, safe options and MATCH groups | ExerciseGeneratorTest, AnswerGraderTest, StudyUnitJdbcIntegrationTest |
| Prerequisites, unlocks, cross-device resume and durable retries | StudyAttemptJdbcIntegrationTest.resumeIdempotencyOwnershipAndUnlocking and resumedStartRequestRemainsIdempotentAfterCompletionAndCannotBeReusedForAnotherUnitOrMode |
| Wrong-answer feedback, permanent first-try accuracy, invalid-answer no-write | StudyAttemptJdbcIntegrationTest.invalidAnswerDoesNotPersistWrongFirstTryIsPermanentAndSnapshotsSurviveRevision |
| Concurrent completion and one reward | StudyAttemptJdbcIntegrationTest.simultaneousCompleteReturnsOneStoredCompletion |
| Replay zero XP, first-only perfect bonus, revision-safe reward history | StudyGamificationJdbcIntegrationTest.xpGrantedOncePerUnitLocalDatesStayFixedAndReplayStillContinuesStreak |
| Timezone boundaries and immutable ledger local dates | StudyStreakRulesTest, StudyGamificationJdbcIntegrationTest with a mutable Clock at 17:30 UTC |
| Zero hearts, allowed reads/resume/WORD/replay, five-hour regeneration | StudyHeartsTest and StudyGamificationJdbcIntegrationTest |
| New correct/incorrect flashcard reviews maintain streak without profile/XP; duplicate does nothing | ReviewFlashcardUseCaseTest and PracticeQuestionFlashcardPersistenceTest |
| Premium 7, normal 5, upgrade/downgrade, TTL, failure fallback, outside-transaction HTTP | StudyPlanRefresherTest and StudyGamificationJdbcIntegrationTest |
| Public two reads and protected study/authoring routes | StudyUnitSecurityTest, StudyAttemptSecurityTest, StudyMeSecurityTest, Gateway InternalPathSecurityTest |
| Durable purchases, insufficient Points, retained uncertain intent, saved results, same-key concurrency | StudyGamificationJdbcIntegrationTest and RestStudyAccessClientTest; actual Access wallet race in PointDebitConcurrencyTest |
| Author-only lifecycle, private drafts, immutable snapshots and archive-safe attempts | StudyUnitJdbcIntegrationTest, StudyUnitListQueryTest, StudyAttemptJdbcIntegrationTest |
| Nullable media, unit completion independent of flashcards | StudyUnitJdbcIntegrationTest and actual unit-only attempt integration fixtures |
| Metrics follow durable transitions and terminal Access availability failures | StudyGamificationJdbcIntegrationTest.countersTrackCommittedLogicalOperationsRatherThanRetriedRequests and StudyAccessMetricsTest |
| Independent ownership in source and persisted data | StudyIndependenceTest; actual web objective and vocabulary completion with all-column raw database equality in both directions in live acceptance |

## Boundaries and operational limits

- No Learning, Content, frontend or mobile implementation is changed. The required Access idempotency bug fix is separately committed and documented in [its review](access-idempotency-review.md).
- Published kind is retained on the revision to keep draft edits invisible. Durable start-request aliases preserve resume idempotency after completion. These persistence supplements satisfy existing invariants without changing the public API.
- V42 is conditional on existing catalog validity: author changes can skip a whole demo topic. Valid seeds append after existing author units and advance its curriculum version once; no author data is reactivated or overwritten. StudyUnitSeedUpgradeTest covers these upgrade cases.
- No migration of old local mobile progress, offline rewards, streak milestone rewards or pending-purchase reconciliation job is included.
- Different concurrent purchase request IDs can each debit before either effect. Clients must retry the same logical request ID; operator guidance and the old-pending query are in Library README.
- Demo curriculum requires content-author review before production. Physical-device media rendering and client fallback UI remain client acceptance work; backend nullable fields and HTTPS URLs are checked independently.
- Local latency measurements are development observations, not a production SLA.

## Final verification

Required clean suites passed: Library 407, Gateway 58 and Access 112 tests, with zero failures/errors/skips. Final fresh live acceptance passed 404 checks with exit 0: baseline 5, learner runner 239, public/OpenAPI 58, timezone 14 and supplemental 88. See [implementation progress](implementation-progress.md), [live acceptance](vocabulary-study-e2e-verification.md) and [final review](final-review.md). The live report preserves earlier harness failures instead of treating them as passing runs. Unexecuted client media checks are not represented as backend passes.
