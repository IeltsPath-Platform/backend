# Course practice-test milestones — implementation report

Date: 2026-10-16 (Asia/Ho_Chi_Minh)
Status: DONE_WITH_CONCERNS

## Summary

Course milestones support published PRACTICE_TEST packages, learner assignment, and an immutable first consumed percentage. Free practice writes evidence/events without topic progression or blocking reviews. No weighted course scoring or new topic-opening policy was implemented.

## Validation

Command: `mvn -q -B -ntp -pl services/assessment-service,services/content-service,services/learning-service -am test`
Exit: 0. Docker/Testcontainers ran. No skipped tests.

| Module / suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| content-service (59 suites) | 377 | 0 | 0 | 0 |
| learning-service (46 suites) | 305 | 0 | 0 | 0 |
| assessment-service (33 suites) | 251 | 0 | 0 | 0 |
| PracticeTestMilestoneIntegrationTest | 31 | 0 | 0 | 0 |
| PracticeTestMilestoneControllerTest | 23 | 0 | 0 | 0 |
| PublishContentPackageUseCaseTest | 12 | 0 | 0 | 0 |
| PracticeTestAssignmentIntegrationTest | 7 | 0 | 0 | 0 |
| PracticeTestControllerTest | 7 | 0 | 0 | 0 |
| AttemptTypeTest | 1 | 0 | 0 | 0 |
| EnqueueGateEssayGradingServiceTest | 6 | 0 | 0 | 0 |
| GateEssayGradingIntegrationTest | 5 | 0 | 0 | 0 |

Suite rows are included in module totals, not additional tests.
`git diff --check`: exit 0. Read-only independent review found no remaining P1/P2 blockers after fixes.

Red checks initially proved the missing content table and attempt mapping. Learning's initial red run was interrupted by incomplete concurrent event-log compilation. Additional regression tests proved that practice-test essays were queued but unclaimed before fixing the worker. Stale IDE-generated classes in target caused discovery failures; only the affected modules' build artifacts were removed and rebuilt.

## Behavior and contracts

- Content V38: milestone table; one published original short Reading/Writing checkpoint per active 5.5/6.5 course, placed at its midpoint.
- Admin GET/PUT atomically list/replace up to 100 milestones; validate publication, package type, uniqueness and reachable course-local topic position.
- Internal GET resolves current published versions and excludes inactive courses/unpublished packages.
- Learning V12: one assignment per learner/package; user lock serializes assignment and event application.
- GET shows LOCKED/AVAILABLE/DONE; POST returns the assessment package version, preserves open assignments and permits retakes.
- First matching completed result applied to an open assignment freezes its percentage. Replays/regrades/retakes cannot overwrite it.
- Previously applied free attempts cannot consume a newly created assignment on regrade.
- Free OFFICIAL_PRACTICE still records normal evidence/events and does not generate blocking reviews.
- PRACTICE_TEST maps to OFFICIAL_PRACTICE. Its submitted essays use the existing AI quota, worker and EXAMINER fallback.
- Practice questions cannot be reused as review material; publication validates question ownership, EXAM purpose and gradable essay/objective specs.

## Coordination and scope

User explicitly approved expanding ownership to EnqueueGateEssayGradingService and its tests, then PackageType, JdbcGateEssayJobStore, PublishContentPackageUseCase and related tests.
No edits were made by this implementation to AutoGradeAttemptService, AssessmentCompletedEventFactory, AssessmentCompletedV2, SaveAttemptResponseRequest, SubmitExerciseCommand, QuestionEventLog or AssessmentCompletedParser.
Only the practice assignment dependency/branch/helper from the shared ApplyAssessmentResultUseCase is staged for this work. Concurrent event-log changes belong to Claude and remain outside this commit. The full verification ran against the combined current working tree.

## Concerns / limits

- Seed checkpoints are short Reading/Writing practice tests, not full-length IELTS exams; no new audio or Speaking grading is supplied.
- Nonblocking review generation waits for review tiers; current OFFICIAL_PRACTICE processing deliberately avoids the blocking generator.
- Consumption uses first matching applied completion, like the existing topic assignment. It does not reconcile chronological completion order across independently delayed events.
- Matching uses learner/version and completedAt >= assignedAt, as specified; the event contract does not supply an attempt-start association.
- No push or deployment performed.

## Files changed

- `docs/contracts/learning-content-internal-v1.md`
- `docs/contracts/lesson-learning-v1.md`
- `plans/261016-0131-knowledge-tree-assessment-journey/phase-08-practice-test-milestones.md`
- `plans/261016-0131-knowledge-tree-assessment-journey/plan.md`
- `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/vo/AttemptType.java`
- `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/EnqueueGateEssayGradingService.java`
- `services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcGateEssayJobStore.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/domain/vo/AttemptTypeTest.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/EnqueueGateEssayGradingServiceTest.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GateEssayGradingIntegrationTest.java`
- `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalPracticeTestMilestoneController.java`
- `services/content-service/src/main/java/com/ieltspath/content/api/controller/PracticeTestMilestoneController.java`
- `services/content-service/src/main/java/com/ieltspath/content/application/usecase/GetPracticeTestMilestonesUseCase.java`
- `services/content-service/src/main/java/com/ieltspath/content/application/usecase/ReplacePracticeTestMilestonesUseCase.java`
- `services/content-service/src/main/java/com/ieltspath/content/domain/repository/PracticeTestMilestoneRepository.java`
- `services/content-service/src/main/java/com/ieltspath/content/domain/vo/PracticeTestMilestone.java`
- `services/content-service/src/main/java/com/ieltspath/content/domain/vo/PackageType.java`
- `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcPracticeTestMilestoneRepository.java`
- `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcLearningContentReader.java`
- `services/content-service/src/main/java/com/ieltspath/content/application/usecase/PublishContentPackageUseCase.java`
- `services/content-service/src/main/resources/db/migration/V38__practice_test_milestones.sql`
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeTestMilestoneIntegrationTest.java`
- `services/content-service/src/test/java/com/ieltspath/content/api/controller/PracticeTestMilestoneControllerTest.java`
- `services/content-service/src/test/java/com/ieltspath/content/application/usecase/PublishContentPackageUseCaseTest.java`
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/LessonPipelineSeedTest.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/controller/PracticeTestController.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/result/PracticeTestMilestoneResult.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ListPracticeTestsUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/AssignPracticeTestUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ApplyAssessmentResultUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/domain/aggregate/PracticeTestAssignment.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/domain/repository/PracticeTestAssignmentRepository.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/persistence/JdbcPracticeTestAssignmentRepository.java`
- `services/learning-service/src/main/resources/db/migration/V12__practice_test_assignments.sql`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestLearningContentClient.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/PracticeTestAssignmentIntegrationTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/api/controller/PracticeTestControllerTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/client/RestLearningContentClientTest.java`
- `plans/reports/pm-261016-2250-practice-test-milestones.md`

## Unresolved questions

None.
