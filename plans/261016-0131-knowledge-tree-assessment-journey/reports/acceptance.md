# Acceptance run

Date: 2026-10-17

Branch: `feat/knowledge-tree-assessment-journey`

## Contract audit

Compared the contracts against the current controllers and service behavior:

| Contract | Result |
| --- | --- |
| `assessment-completed-v2.md` | Item `error_code` and per-KP `error_type` scopes are documented separately; the first error-analysis code may appear in both. |
| `answer-spec-v1.md` and shared vectors | Request/response examples and shared answer vectors match objective grading and error-tagging inputs. |
| `assessment-grading-v1.md` | Queue fields and source/reason semantics match examiner and AI-evaluation flows; rubric snapshots apply to standalone human reviews. |
| `learning-content-internal-v1.md` | Internal routes, suggestion candidate fields, and the new question-difficulty batch request/response match Content controllers. |
| `admin-area-v1.md` | QA queue fields and Access consume/refund response and error behavior match the controllers. |
| `human-review-v1.md` | Request, idempotency, pending/done response, and blind-review fields match the internal Assessment endpoints. |
| Access contracts | No separate Access contract file exists; the human-grading credit API is covered by `admin-area-v1.md`. |

## Database reference

Added the current Assessment, Content, and Access table definitions to `.sdd/database/DATABASE_V5.md`. The migration scan covers 25 Assessment tables, 27 Content tables, and 10 Access tables.

## Reactor verification

The user reports that `mvn -B -ntp verify` excluding `library-service` passed on this tree: **1,419 tests, 0 failures, 0 errors, 0 skipped**. This full run was not repeated during this acceptance continuation.

The focused rerun performed earlier for Content and Assessment was `mvn -B -ntp -pl services/content-service,services/assessment-service -am test`: **714 tests, 0 failures, 0 errors, 0 skipped**. Testcontainers ran against Docker.

`library-service` is outside this acceptance scope. Its existing test-source compilation issue on main was not fixed, and no new library JAR was built.

## Compose and demo

`docker compose down -v` completed and removed the database and RabbitMQ volumes, including the previous demo account/data. The requested `docker compose up -d --build` was attempted, but the Dockerfile Maven builds received HTTP 429 from Maven Central while resolving Spring Boot 3.5.14 and Spring Cloud 2025.0.0 BOMs. Serialized retries also received 429.

To continue on the clean volumes, runtime images for the ten services with verified local JARs were rebuilt one at a time from those JARs; the existing `library-service` image was retained because the service is out of scope. The stack was started with `docker compose up -d --no-build`. All Compose containers reported healthy. This used the verified artifacts but did not complete the requested Compose Maven image build.

The following responses were observed through Gateway on the clean stack:

| Demo step | Result |
| --- | --- |
| 1. Tree edit and cycle refusal | As ADMIN, tree GET returned 200. Adding `DR_TOPIC_SENTENCE` as a prerequisite of `DR_MATCHING_HEADINGS` returned 200 and the prerequisite GET showed it. The reverse edge returned 422 `PREREQUISITE_CYCLE`; the cycle was rejected. |
| 2. Practice review and diagnosis | Completed the Reading lessons and initial Practice sets, then failed the `PS-KP4-A` set (0/4). Learning created the `DR_MATCHING_HEADINGS` review. After its theory check and two failed review sets, the review entered `DIAGNOSING`. The `DR_TOPIC_SENTENCE` probe failed 0/3 and the diagnosis concluded that KP. The gap review was passed; the blocked B review resumed with the V40 `PS-KP4-D` set and was passed. The newly seeded review sets supported the scripted path. |
| 3. Learner report and recommendations | `GET /api/learning/report`, `GET /api/learning/review-basket`, and `GET /api/learning/suggestions` all returned 200. The report and basket included review/mastery state, and suggestions returned `PS-KP3-D`. |
| 4. Topic test and Examiner work | The initial run assigned version `20000000-0000-4000-8000-000000000001` and Assessment returned 404 `Package version not found`. On recheck, Learning and Content both reported the published X1 version `20000000-0000-4000-8000-090000000001`, and repeating the assignment matched it. The mismatch was **không tái hiện, chưa rõ nguồn**; no fix is recorded. The 50% submission and remediation work item could not be produced. The Examiner queue contained a `LEARNER_FLAG/BURST_SUBMISSIONS`, not a `MID_COURSE_REMEDIATION` item. |
| 5. Course test and remediation | `GET /api/learning/courses` returned courseId `30000000-0000-4000-8000-000000000001` for IELTS 5.5. Posting to `/api/learning/courses/30000000-0000-4000-8000-000000000001/test-assignments` returned 422 `{"detail":"Invalid request"}`, not the expected 403 `COURSE_TEST_LOCKED`. This is the exact courseId used. No course attempt, essay grading, remediation, or retest could be run. |
| 6. Score configuration | Admin GET and PUT returned 200. With default weights the learner's in-progress course total was 0.096; changing process/mid/final weights from 0.20/0.30/0.50 to 0.25/0.25/0.50 changed it to 0.120. Restoring the defaults returned the total to 0.096. |
| 7. Gold essay evaluation and activation | Added one demo-only manual sample (201), then started an evaluation (202). It finished DONE with n=1, within-half-band 1.0, MAE 0.5, QWK 0.0, and `passed=false`. Activation returned 409 with `EVALUATION_NOT_PASSED`. This single sample is synthetic and is not an expert-rated acceptance gold set. |
| 8. QA re-grade and Examiner quality | QA queue returned 200 with `totalElements=0`; examiner-quality returned 200 with an empty result. No sample was available to claim or re-grade. |

## Acceptance status

Closed with open items; not a full acceptance. Verification of every module except `library-service` is green (1,419
tests, 0 failures, 0 skipped), and the clean stack is healthy. Demo steps 1-3, 6 and the step 7 activation guard passed;
the V40 sets enabled the intended review-and-diagnosis flow.

Open items:

- **Step 5 (resolved in follow-up).** The old image returned 422 because `CourseController.assign` did not name the
  `@PathVariable`; after rebuilding Learning, `GET /api/learning/courses` returned courseId
  `30000000-0000-4000-8000-000000000001`, and the assignment request returned the expected 403 `COURSE_TEST_LOCKED`.
- **Step 4 (không tái hiện, chưa rõ nguồn).** The existing Learning assignment and the Content X1 package
  both use published version `20000000-0000-4000-8000-090000000001`. Repeating the assignment request through Gateway
  returned that same value. The previous mismatch could not be confirmed from current data or code; no fix is recorded.
- **Step 8** still has no QA sample to claim or re-grade: the queue returned `totalElements=0`. Examiner quality returned
  one aggregate entry.
- **Phase 11** has tooling but no accuracy evidence: no gold set rated by an IELTS expert exists. A run on 30 synthetic
  essays could not finish (`LLM_REQUEST_FAILED`); synthetic scores are never accuracy evidence.
- **`library-service`** test sources do not compile, unchanged from main; out of scope.

## Follow-up after Learning image rebuild

On 2026-10-17, `mvn -B -ntp -pl services/learning-service -DskipTests package` succeeded; Learning tests were not run.
The runtime image was rebuilt from that JAR, and `learning-service` returned healthy. The live step-5 response above came
from the rebuilt service. The step-8 QA queue and examiner-quality endpoints both returned 200; the queue had no sample.

The step-4 assignment row has packageId `20000000-0000-4000-8000-080000000001` and packageVersionId
`20000000-0000-4000-8000-090000000001`. Content's `X1` package has the same current published version ID. The repeated
Gateway assignment response matched both values. No current Learning/Content version mismatch was found.

The read-only Content database check returned **12** rows in `question_versions` where `difficulty IS NULL`.
