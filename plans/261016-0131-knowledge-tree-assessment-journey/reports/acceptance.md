# Acceptance run

Date: 2026-10-17
Branch: feat/knowledge-tree-assessment-journey

## Contract audit

Compared the contracts against the current controllers and service behavior:

| Contract | Result |
| --- | --- |
| assessment-completed-v2.md | The item error_code and per-KP error_type scopes are documented separately; the first error-analysis code may appear in both. |
| answer-spec-v1.md and shared vectors | Request/response examples and shared answer vectors match the objective grading and error-tagging inputs. |
| assessment-grading-v1.md | Queue fields and source/reason semantics match the examiner and AI-evaluation flows; rubric snapshots apply to standalone human reviews. |
| learning-content-internal-v1.md | Internal routes and suggestion candidate response fields match the Content controllers. |
| admin-area-v1.md | QA queue fields and Access consume/refund response and error behavior match the controllers. |
| human-review-v1.md | Request, idempotency, pending/done response, and blinded-review fields match the internal Assessment endpoints. |
| Access contracts | No separate Access contract file exists; the human-grading credit API is covered by admin-area-v1.md. |

The current branch already contains the alignment corrections, so this run made no additional contract edits.

## Reactor verification

Command: mvn -B -ntp verify
Result: BUILD FAILURE; reactor verification incomplete.

| Completed module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| common-security | 8 | 0 | 0 | 0 |
| api-gateway | 9 | 0 | 0 | 0 |
| config-server | 1 | 0 | 0 | 0 |
| eureka-server | 1 | 0 | 0 | 0 |
| user-service | 101 | 0 | 0 | 0 |
| Total completed before failure | 120 | 0 | 0 | 0 |

The build stopped at Spring Boot repackage for user-service with "There is not enough space on the disk". The Maven log also reports that Testcontainers could not find a valid Docker environment. The reactor did not reach Content, Learning, or Assessment, so their Testcontainers suites were not verified. This is not a green reactor run; no Learning source files were changed.

## Compose and demo

docker compose down -v completed. docker compose up -d --build did not finish: BuildKit builds stalled and the Docker engine later stopped accepting requests through its named pipe. A subsequent docker compose up -d and docker ps also could not reach the engine. The stack was not available, so none of the demo actions below was executed.

| Demo step | Scenario | Result |
| --- | --- | --- |
| 1 | R&D edits the knowledge tree and a cycle is rejected | Not run - stack unavailable. |
| 2 | Wrong Practice creates review and diagnosis work | Not run - stack unavailable. |
| 3 | Learner report, review basket, and suggestions | Not run - stack unavailable. |
| 4 | Topic test opens the next topic and queues Examiner work | Not run - stack unavailable. |
| 5 | Course test, essay grading, remediation, retest, and pass | Not run - stack unavailable. |
| 6 | Admin changes course score weights | Not run - stack unavailable. |
| 7 | Gold essay evaluation and prompt activation | Not run - stack unavailable. |
| 8 | QA re-grade and Examiner quality report | Not run - stack unavailable. |

## Acceptance status

Not accepted yet. The contract audit is complete, but full reactor verification requires free disk space and working Docker Desktop access. The eight demo steps still need to be executed against a clean running stack.
