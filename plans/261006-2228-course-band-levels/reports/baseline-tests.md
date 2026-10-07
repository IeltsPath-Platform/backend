# Baseline Test Report

## Summary

Status: DONE_WITH_CONCERNS. Baseline at commit `6351f3af5a81322ab52255c8996b2479c76df983` passed with exit code 0. No tracked code changes were present before the run.

513 discovered tests: 426 passed, 87 skipped, 0 failures, 0 errors. Skipped tests were not executed; database migrations, persistence behavior and real broker integration remain unverified in this baseline.

## Execution

Command: `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test`

Started UTC: 2026-10-06T17:24:55.5271701Z
Completed UTC: 2026-10-06T17:25:44.4256378Z

The first sandbox run stopped before test execution because Maven cache/network access was denied. The authorized elevated retry completed successfully. Elevated `docker info --format '{{.ServerVersion}}'` also failed because the Docker Desktop Linux engine named pipe does not exist. Testcontainers therefore skipped its Docker-dependent cases.

Counts below were read from Surefire XML files written after the recorded baseline start time; stale report files were excluded.

## Results

| Module | Suites | Discovered | Passed | Skipped | Failures | Errors |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| shared/common-security | 4 | 8 | 8 | 0 | 0 | 0 |
| services/content-service | 30 | 177 | 138 | 39 | 0 | 0 |
| services/assessment-service | 17 | 116 | 106 | 10 | 0 | 0 |
| services/learning-service | 31 | 212 | 174 | 38 | 0 | 0 |

## Skipped suites

Every skipped testcase is enumerated in [baseline-skipped-tests.json](baseline-skipped-tests.json).

| Suite | Skipped cases |
| --- | ---: |
| `com.group01.content.infrastructure.persistence.BandRangeMigrationTest` | 4 |
| `com.group01.content.infrastructure.persistence.CatalogRemovalMigrationTest` | 1 |
| `com.group01.content.infrastructure.persistence.DemoReadingPassageSeedTest` | 1 |
| `com.group01.content.infrastructure.persistence.KnowledgePointLearningTypeMigrationTest` | 1 |
| `com.group01.content.infrastructure.persistence.LessonPipelineSeedTest` | 32 |
| `com.group01.assessment.application.usecase.AutoGradingIntegrationTest` | 5 |
| `com.group01.assessment.infrastructure.messaging.AssessmentOutboxIntegrationTest` | 4 |
| `com.group01.assessment.infrastructure.persistence.AssessmentSchemaValidationTest` | 1 |
| `com.group01.learning.infrastructure.persistence.AssessmentResultIntegrationTest` | 5 |
| `com.group01.learning.infrastructure.persistence.LessonSubmissionIntegrationTest` | 12 |
| `com.group01.learning.infrastructure.persistence.LessonWritingIntegrationTest` | 10 |
| `com.group01.learning.infrastructure.persistence.PracticeIntegrationTest` | 1 |
| `com.group01.learning.infrastructure.persistence.RemediationLadderIntegrationTest` | 2 |
| `com.group01.learning.infrastructure.persistence.ReviewAndTestAssignmentIntegrationTest` | 7 |
| `com.group01.learning.LearningServiceApplicationTests` | 1 |

## Artifacts

- [Elevated Maven output](baseline-maven.log)
- [Sandbox attempt output](baseline-maven-sandbox.log)
- [Machine-readable counts](baseline-counts.json)
- [All skipped test identities and reasons](baseline-skipped-tests.json)

## Recommendations

Proceed with the planned phase tests. Run the full combined suite with Docker available before claiming migrations and broker integration have been verified. No old-test expectations were changed. No code or plan files were edited by this tester.

## Unresolved questions

None. Docker availability is the only baseline verification limitation.
