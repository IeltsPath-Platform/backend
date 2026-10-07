# Learning Implementation Context

## Summary

Read-only preparation for placement recommendation and course paths/tests. Read phase 5/6, learning README/runtime config (secret values excluded), then used Graphify explain/query for `AssessmentCompletedParser`, `RefreshLearningTopicsUseCase`, `ApplyAssessmentResultUseCase` and `TopicStatusDeriver` before reading relevant source. No code, tests, plans or configuration changed; no Maven run.

The approved work fits the current boundaries. Migration inventory is V1–V5: planned V6 and V7 are unused. No verified plan/code contradiction was found. Commit order remains content 1 → 2 → 3 → learning 5 → 6 → assessment 4 → docs 7.

Paths below are relative to `services/learning-service/src/main/java/com/group01/learning/` unless noted.

## Placement event path

| Existing file | Current behavior / required change |
| --- | --- |
| `application/command/AssessmentResult.java` | Record currently ends with `completedAt, items`; add optional `BigDecimal overallBand`. Preserve the old constructor overload for existing event fixtures. |
| `infrastructure/messaging/AssessmentCompletedParser.java` | Validates numeric scores, UUIDs, versions and allowed assessment types. Currently ignores `overall_band`. Add optional numeric parsing plus domain band validation, wrapping invalid values as `ContractViolationException`; null/absent must be accepted. |
| `application/usecase/ApplyAssessmentResultUseCase.java` | Single `@Transactional` method starts with learner lock, ignores equal/older version, removes earlier evidence on regrade, records version, then returns immediately for PLACEMENT. Insert placement recording after `recordVersion` and before return; keep no placement evidence/review insertion. |
| `infrastructure/messaging/AssessmentCompletedListener.java` | Calls the proxied use case and ACKs afterwards; parser violations go to DLQ. No new HTTP calls or listener transaction logic needed. |

Create V6 `learner_placements`, `domain/vo/BandLevel`, `domain/aggregate/LearnerPlacement`, its repository contract and `JdbcLearnerPlacementRepository`. Aggregate owns completed-at freshness/same-attempt regrade rule; JDBC only persists the resulting state. If a placement with null band arrives, version handling still happens and no placement row is created/changed.

Constructor impact: the only production `new AssessmentResult(...)` is the parser. The existing integration helper at `src/test/java/.../infrastructure/persistence/AssessmentResultIntegrationTest.java:105` uses the old constructor; retain its old behavior. Apply-use-case constructor is wired by Spring in integration tests and mocked in listener tests, rather than directly instantiated by old test code.

## Curriculum refresh and gate path

| Existing file | Current behavior / required change |
| --- | --- |
| `application/port/LearningContentClient.java` | Topic has overloaded legacy constructors; append nullable Course metadata and retain overloads. Add `getCourseTestPackages(UUID)` using existing TestPackage shape. Only concrete implementation is RestLearningContentClient; old tests mock the interface. |
| `infrastructure/client/RestLearningContentClient.java` | Jackson binds topic records directly; missing added course property naturally becomes null. New course package GET should reuse `get` and existing bearer/correlation forwarding/error mapping. |
| `application/usecase/RefreshLearningTopicsUseCase.java` | Holds learner lock; makes one sequence HTTP read, sorts by skill/free/sort/id, updates curriculum/catalog, backfills review skills, uses batched completed counts, maps TopicResult. Replace only ordering key with course band/free/sort/id and carry course metadata in results and placements. |
| `domain/aggregate/LearnerCurriculum.java` | `reorder` preserves passes, gives 1..n sequence orders, clears order for removed topics. TopicPlacement currently has topicId/skill/hasTopicTest; append courseId without changing pass/removal behavior. |
| `domain/entity/TopicProgress.java` | Legacy 3- and 5-argument constructors, `place(order, skill, hasTopicTest)`, one-way pass. Append courseId, preserve constructor compatibility and propagate it through place. |
| `domain/service/TopicStatusDeriver.java` | Current first-unpassed grouping is HashSet of skill. Replace grouping key with nullable courseId; null remains one group; removed topics excluded as before. |
| `infrastructure/persistence/JdbcLearnerCurriculumRepository.java` | Explicit SELECT and batched INSERT/upsert of every topic; add course_id to both mapping directions and SQL parameters. Existing pass timestamp uses COALESCE and must remain one-way. |
| `application/result/TopicResult.java`, `api/dto/response/TopicResponse.java` | Add course projection `{courseId, code, bandLevel}`; existing field names and accessLevel behavior stay intact. Preserve old TopicResult constructor used by MVC fixture. |

Keep skill review behavior in `domain/service/LessonAccessGate`, `application/service/LessonAccess`, `application/usecase/GetTopicLessonsUseCase`, `AssignTopicTestUseCase` and PracticeProgress. Skill comparisons already ignore course, so they naturally block the same skill across courses. Do not replace those comparisons with course IDs.

Existing refresh triggers in LessonAccess and AssignTopicTestUseCase are topic missing / skill missing; they do not inspect courseId. Approved legacy records remain in the null-course group until refresh. Do not silently add new access rules while threading metadata.

## Assignment, course list and consumer

- Create V7 with `topic_progress.course_id`, `course_progress` and separate `course_test_assignments`. Existing topic assignment schema is in V1: partial unique `(user_id, topic_id) WHERE consumed_at IS NULL`, index `(user_id, package_version_id, assigned_at)`. Mirror with course_id; no cross-service FK or merged nullable-scope aggregate.
- Mirror `TopicTestAssignment`/`TopicTestAssignmentRepository`/`JdbcTopicTestAssignmentRepository` into course equivalents. Current findOpenForAttempt scopes by user/version, filters open and assigned_at ≤ completed_at, orders newest first, `LIMIT 1 FOR UPDATE`. Save only consumes open rows; package rotation reads grouped latest consumption times. Preserve those invariants.
- Create CourseProgress and JDBC repository; pass stays one-way and learner scoped. Read course progress in one batch for listing rather than per course.
- ListCoursesUseCase can group a single refresh result in maps, then combine one course-progress read and one placement read. At most one sequence HTTP request; no course HTTP request inside grouping. Recommendation chooses lowest band ≥ placement, else highest; no placement means all false.
- AssignCourseTestUseCase follows existing lock/transaction pattern and PackageRotation. Validate course/topic completion and test availability, then reuse open assignment before selecting a new code. Reuse TestAssignmentResult/TestAssignmentResponse. Use LearningRequestException for the approved 403/409 statuses.
- Add COURSE_GATE to parser TYPES and use-case REVIEWED_TYPES. It should append evidence, consume matching course assignment even on failure, pass course at 70%, reevaluate reviews, and retain equal/older-version replay handling. No learner curriculum pass is performed by course gates; course tests never lock topics.

## MVC and configuration

New CourseController alone receives CUSTOMER `@PreAuthorize` on list and assignment routes, reads identity through CurrentUserProvider and maps response DTOs. Old controllers currently have no role annotations and must stay unchanged. CommonSecurityAutoConfiguration already enables method security; Gateway `/api/learning/**` and springdoc paths already cover the new routes.

`api/exception/GlobalExceptionHandler` already maps LearningRequestException to its supplied status and `{detail, code}`; LearningGateException always maps 403 with optional review details. Existing TestAssignmentResponse is `{assignmentId, packageId, packageVersionId}`.

LearningSecurityWebMvcTest currently targets a synthetic protected controller. Add the real CourseController and mock its dependencies for new role tests while keeping existing authentication tests. LessonLearningWebMvcTest currently slices LessonLearningController, ReviewController and PracticeController with mocked use cases; add new controller mocks only if extending that slice. Do not disable shared security.

## Old-test compatibility and risk map

| Test area | Evidence / treatment |
| --- | --- |
| TopicStatusDeriverTest | Only `opensAnIndependentTopicForEachSkillIncludingUnspecified` is allowed to change expectations. Other methods already describe a single/null group and should keep their assertions. |
| ReviewAndTestAssignmentIntegrationTest | Only `skillTracksKeepReviewsLocalAndPassWritingWithoutATest` can adjust course fixtures. It currently expects Reading, Listening and Writing initially open; use separate course IDs for those intended independent tracks, retaining skill-local review/no-test Writing assertions. |
| RestLearningContentClientTest | Add course cases; missing course old payloads must still parse and old assertions stay green. |
| AssessmentResultIntegrationTest | Existing null-band placement case asserts no evidence; retain it. New placement tests need to clear the new placement table between cases because USER is shared. Same for new course/progress/assignment fixtures. |
| LessonSubmissionIntegrationTest | Existing two sequence topics are both READING with no course, so one null group preserves current order/status expectations. |
| LessonWritingIntegrationTest, PracticeIntegrationTest, RemediationLadderIntegrationTest | Sequence stubs each contain one topic; null-course constructor compatibility should preserve behavior. Keep old expectations unchanged. |
| ProgressAggregatesTest, ReviewRuleTest | Current curriculum fixtures are null-course/single sequence. Preserve overloads and existing one-way pass/status assertions. |
| LessonLearningWebMvcTest | Old TopicResult constructors are used around line 354; retain overloads. Additive nullable course may be omitted under existing non_null Jackson configuration; old JSON assertions need no rewrite. |

No other old expectation change is authorized. If an unallowlisted old test actually fails, report its exact name and stop rather than broadening this list. Docker was unavailable at baseline: new unit/parser/MVC/client cases execute, but Testcontainers cases require explicit skip names/counts until Docker exists.

## Risks to resolve through implementation tests

- Regrade replay vs placement freshness: same attempt overwrites even at equal/older completedAt; another older attempt does not. Newer result version still governs event replay.
- Course gate with null packageVersionId or no matching assignment must not pass a course; evidence/review handling remains consistent with topic gates.
- Missing course payloads and old persisted course_id null all belong to a single null group by the approved decision; retain constructor compatibility without recreating skill grouping.
- Avoid repeated refresh calls to obtain metadata already returned by the first refresh and avoid repeated filtering/counting of every topic for every course.
- Tests that introduce new state tables need isolated new-state fixtures without changing existing assertions or using destructive application migrations.

## Next steps

Use this map when learning work begins after content seed completion. No implementation or verification was performed during preparation; all statuses above describe current code and required work.
