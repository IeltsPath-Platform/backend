# Content red-stage verification

Date: 2026-10-07 (Asia/Saigon)
Status: EXPECTED RED

Command: `mvn -q -pl services/content-service -am test`

Maven exited with code 1. Docker-backed integration tests ran. All failures/errors are the new or explicitly approved cases; no unrelated old test failed. No fixture/setup failure occurred.

| Module | Suites | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| services/content-service | 43 | 235 | 5 | 3 | 0 |
| shared/common-security | 4 | 8 | 0 | 0 | 0 |

Failed/error cases:

- Failure - com.group01.content.api.controller.InternalLearningContentControllerTest.invalidPracticeSkillFilterIsRejected: Status expected:<400> but was:<200>
- Error - com.group01.content.application.usecase.MultiSkillPackagePublishingTest.learningPackagesAcceptEssayThresholdsAtBothBounds: COURSE_TEST requires Reading questions with gradable CHOICE or FILL answer specs
- Error - com.group01.content.application.usecase.MultiSkillPackagePublishingTest.courseTestsAcceptListeningAndWritingWithHalfBandThresholds: COURSE_TEST requires Reading questions with gradable CHOICE or FILL answer specs
- Failure - com.group01.content.application.usecase.MultiSkillPackagePublishingTest.learningPackagesRejectMissingOutOfRangeAndNonHalfBandEssayThresholds:  Expecting code to raise a throwable.
- Error - com.group01.content.application.usecase.UpdateTopicUseCaseTest.changingTheSkillOfATopicWithPublishedLessonsIsAllowed: Topic 'DEMO_READING' has published lessons; its skill cannot change
- Failure - com.group01.content.infrastructure.persistence.MultiSkillContentIntegrationTest.sequenceDerivesSkillsFromPublishedLessonsEvenWhenTopicSkillIsNull:  Expecting Optional to contain a value but it was empty.
- Failure - com.group01.content.infrastructure.persistence.MultiSkillContentIntegrationTest.singleSkillCompatibilityComesFromExercisesRatherThanTopicMetadata:  expected: "LISTENING" but was: "READING"
- Failure - com.group01.content.infrastructure.persistence.MultiSkillContentIntegrationTest.lessonAndSummaryIncludeTextKnowledgePointSkillsAndDeduplicateExerciseSkills:  expected: "["READING","WRITING"]" but was: ""

Skipped test names: none.

The publishing errors reflect the current Reading-only COURSE_TEST rule. The topic update error reflects the current published-lesson skill lock. The remaining failures assert behavior still missing before implementation.

Only this report was written by the tester; no source changes, test expectation changes, .env access, or real LLM calls.
