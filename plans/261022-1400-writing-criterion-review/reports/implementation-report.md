# Writing criterion review implementation

Status: DONE_WITH_CONCERNS

## Workspace

- Worktree: backend-writing-criterion-review.
- Branch: feat/writing-criterion-review, based on local main at 05f08708.
- Original backend / feat/content-author worktree preserved. No commit or push.

## Behavior

- A counted lesson Practice essay or original lesson essay creates optional reviews for failed mapped rubric criteria only. Equality with passBand passes.
- Shared application service requests availability once for all eligible failed criterion KPs, with at least three questions and revealed/assigned packages excluded.
- Selection skips pending KPs, chooses lowest bands first, then canonical KP code, with configurable maximum learning.writing.criterion-review-limit (default 2).
- Inserts use the existing batch insert API and pending uniqueness constraint. Reviews retain the essay lesson ID, WRITING skill, blocking=false and WRITING_CRITERION trigger.
- Basket adds weak without changing existing groups. Pending optional KPs appear once, sorted by mastery then code; pending priority reviews open through existing endpoints.
- Review selection accepts PS-WCR packages outside lessons. Passing writes review_set evidence, updates mastery and marks DONE.
- Practice clearance still reads PRACTICE triggers only. Optional reviews do not block lessons, Practice or topic tests.
- Submission reviewsCreated remains scoped to existing blocking PRACTICE reviews; optional criterion reviews are exposed through GET /reviews and the basket.

## Deliberate limits

- Free writing, revisions and judged sub knowledge points create no criterion review.
- Existing weak profiles are not backfilled; creation occurs on a new counted submission.
- Confirm the default maximum of two with the user and mentor before release.

## Changes

### Production code

Paths below are relative to services/learning-service/src/main/java/com/ieltspath/learning/.

- domain/vo/FailedWritingCriterion.java (new)
- domain/service/WritingCriterionReviewRule.java (new)
- domain/service/WritingEvidenceRule.java
- domain/vo/PendingReview.java
- application/service/WritingCriterionReviews.java (new)
- application/usecase/SubmitPracticeAttemptUseCase.java
- application/usecase/SubmitLessonEssayUseCase.java
- application/usecase/GetReviewBasketUseCase.java
- application/result/ReviewBasketResult.java
- api/dto/response/ReviewBasketResponse.java (new)
- api/controller/ReviewBasketController.java
- infrastructure/persistence/JdbcReviewItemRepository.java
- infrastructure/client/OpenAiCompatibleLlmClient.java: request string rendering omits prompt content so Spring HTTP DEBUG cannot print it.

### Migration

- services/learning-service/src/main/resources/db/migration/V35__writing_criterion_reviews.sql (new): extends trigger CHECK constraint, retaining every existing value. No old migration or data changed.

### Tests

Paths below are relative to services/learning-service/src/test/java/com/ieltspath/learning/.

- domain/service/WritingCriterionReviewRuleTest.java (new)
- domain/service/WritingEvidenceRuleTest.java
- application/service/WritingCriterionReviewsTest.java (new)
- application/usecase/SubmitPracticeAttemptUseCaseTest.java (new)
- application/usecase/StudyContextTest.java: aligns existing mock with authorizeToRead.
- infrastructure/client/OpenAiCompatibleLlmClientTest.java: adds direct safe request rendering coverage.
- LearningServiceApplicationTests.java: expects Flyway V35, with the same table count.
- api/controller/ReviewBasketControllerTest.java
- infrastructure/persistence/PracticeEssayIntegrationTest.java
- infrastructure/persistence/LessonWritingIntegrationTest.java
- infrastructure/persistence/ReviewBasketIntegrationTest.java

### Contracts and maintainer docs

- docs/contracts/lesson-learning-v1.md
- docs/contracts/lesson-writing-v1.md
- services/learning-service/README.md
- AGENTS.md: replaces obsolete statement that Writing never creates reviews with the new scoped optional behavior.
- plans/261022-1400-writing-criterion-review/plan.md

## Verification

Full suite completed with Maven exit code 0: learning-service 890 tests in 117 suites; common-events 13 tests; common-security 8 tests. Total 911 tests, 0 failures, 0 errors, 0 skips. Testcontainers used PostgreSQL 15; Content and grading clients were stubbed. No real LLM.

Focused commands used Maven -B -ntp -q -pl services/learning-service -am with -Dsurefire.failIfNoSpecifiedTests=false:

1. -Dtest=WritingEvidenceRuleTest,WritingCriterionReviewRuleTest,WritingCriterionReviewsTest,ReviewBasketControllerTest test
2. -Dtest=WritingEvidenceRuleTest,WritingCriterionReviewRuleTest,WritingCriterionReviewsTest,SubmitPracticeAttemptUseCaseTest,ReviewBasketControllerTest,PracticeEssayIntegrationTest,LessonWritingIntegrationTest test
3. -Dtest=PracticeEssayIntegrationTest test
4. -Dtest=ReviewBasketControllerTest,ReviewBasketIntegrationTest,SubmitPracticeAttemptUseCaseTest test

Focused results across the eight classes: 69 tests, zero failures/errors/skips in the latest class reports before full regression. Initial failures were test-only: a wrong mastery column name and an outdated completed-lesson read assertion. The latter is now tested as readable, while completion still rejects blocking reviews with REVIEW_REQUIRED. No production gate changed.

Full command: mvn -B -ntp -q -pl services/learning-service -am test > writing-criterion-full.log 2>&1

Fix verification command: mvn -B -ntp -q -pl services/learning-service -am -Dtest=ReviewBasketControllerTest,StudyContextTest,OpenAiCompatibleLlmClientTest,LearningServiceApplicationTests -Dsurefire.failIfNoSpecifiedTests=false test > writing-criterion-regression-fixes.log 2>&1

First full run: learning-service 889 tests, 4 failures, 1 error, 0 skips; dependencies 21 tests, all green. The new schema version assertion still expected V34 and was updated to V35. This run also exposed one obsolete StudyContextTest mock (authorize vs authorizeToRead) and three existing prompt confidentiality assertions under Spring DEBUG. The client request record previously rendered prompt content through its auto-generated toString; the fix redacts only string rendering, preserving JSON and the original assertions. Focused verification after fixes: ReviewBasketControllerTest, StudyContextTest, OpenAiCompatibleLlmClientTest, LearningServiceApplicationTests; 24 tests, zero failures/errors/skips. Second full run passed all 911 tests.

Domain import scan for application/api/infrastructure/Spring/JPA has zero matches. git diff --check passes.

## Review

Edge-case and quality reviews found no actionable production defects. Optional future coverage: inject Content availability failure and retry to exercise transaction recovery explicitly; existing transaction/lock paths were inspected.

## Remaining concern

Both full runs produced Surefire fork shutdown dumps after the test results were written. In the final run Maven exited 0 and every test passed, but Surefire reported that its fork had not exited 30 seconds after System.exit(0). The thread dump shows SpringApplicationShutdownHook waiting in DefaultLifecycleProcessor.stop, with Rabbit listener consumers retrying connections. RabbitMQ lifecycle/configuration was not changed for this feature. This is a teardown concern, not a failed or skipped test, and remains unresolved.

Evidence files in the worktree: writing-criterion-full.log, writing-criterion-full-initial.log, writing-criterion-regression-fixes.log; final fork dump under services/learning-service/target/surefire-reports/2026-10-22T15-15-25_976-jvmRun1.dump. Logs and target artifacts are ignored by Git.
