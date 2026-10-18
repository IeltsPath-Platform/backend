# Learning changes review: 2026-10-17

## Scope

Read-only review of Learning Service changes in `4a0b56e..9cf6d74`, compared with the related contracts under
`docs/contracts/`. Gateway route/security configuration was checked separately for the requested new paths. No
Learning Service files were modified.

## Findings

### Medium — essay revision does not enforce its documented CUSTOMER role

[`WritingController.java:47`](../../services/learning-service/src/main/java/com/ieltspath/learning/api/controller/WritingController.java)
and [`WritingController.java:54`](../../services/learning-service/src/main/java/com/ieltspath/learning/api/controller/WritingController.java)
expose revision and history handlers without a controller- or method-level role check. The revision contract explicitly
requires `CUSTOMER`; an authenticated `ADMIN`, `EXAMINER`, or `CONTENT_AUTHOR` token therefore reaches the handler as
well. Ownership checks prevent access to another user's submission, but do not enforce the endpoint's documented role.

### Low — passed course score can pair a frozen total with current weights

[`GetCourseScoreUseCase.java:47`](../../services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetCourseScoreUseCase.java)
uses the stored total and components for a passed course, while
[`GetCourseScoreUseCase.java:54`](../../services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetCourseScoreUseCase.java)
returns weights from the current configuration. After an admin changes the score configuration, the response may show a
passed total/components that were computed under the earlier configuration alongside new weights; those fields no
longer describe one consistent calculation. The course score contract says a passed course keeps its decision while
configuration changes recalculate undecided courses, so the response should either preserve the applied weights or
clearly separate the historical pass calculation from current weights.

## Gateway verification

The requested Learning paths match the existing `learning-service` route (`Path=/api/learning/**`), and the requested
Assessment paths match `assessment-service` (`Path=/api/assessments/**`). Both prefixes are configured for internal JWT
signing. A focused Gateway test was added to pin these route and JWT-prefix assumptions for representative paths across
every requested path group.

## Verification

`mvn -Dmaven.repo.local=C:\Users\pduy8\.m2\repository -f infra/api-gateway/pom.xml -Dtest=LearningRouteConfigTest test` — 2 tests,
0 failures, 0 errors, 0 skipped.
