# Red-team review: scope and complexity critic, plus contract verifier

- Plan: `plans/260929-1640-lesson-learning-pipeline-mvp/` (plan.md, phase-01 to phase-08)
- Reviewer lens: YAGNI, over-engineering, redundant state, and missing consumers for every changed interface
- Fixed decisions not challenged: hide answers until a block is passed; reviews use new PRACTICE_SET packages; multiple TOPIC_TEST forms; one path per user with no LLM; premium and topic reorder deferred; content comes from seed only.
- All evidence was checked with grep/read against the working tree on 2026-09-29.

---

## Finding 1: Phase 5 deletes `curriculum_scope.py` while live code still imports it; the phase 5 gate cannot pass and the tutor loses KP details

- **Severity:** Critical
- **Location:** Phase 5, section "Code → Xóa" and "Related Code Files → Delete"
- **Flaw:** Phase 5 deletes `app/adapters/curriculum_scope.py` and names only `path_service.py`, `path_orderer.py` and `path_ordering.py` as its users. Four other importers are left out. Two of them are not touched until phase 7, and one is never scheduled. The module is also the only producer of `KnowledgePointDetails` (skill, description), which the tutor tool `knowledge_point_details` reads.
- **Failure scenario:** After phase 5, `import app.application.path_service` → `formal_result_applier` → `placement_test_out` → `curriculum_scope` raises `ModuleNotFoundError`. The whole API and consumer then fail to import, so the phase 5 regression gate ("python -m pytest tests") fails. If someone patches the imports, a new path no longer writes `mastery_path_knowledge_point_details`. The tutor's `knowledge_point_details` tool then returns `skill: None, description: ""` for every KP, while the system prompt tells the model to call that tool first.
- **Evidence:**
  - Importers not in the phase 5 plan: `app/adapters/formal_evidence_adapter.py:17` (`parse_band`, changed only in phase 7), `app/learning/placement_test_out.py:26` (deleted only in phase 7), `app/persistence/postgres_learning_store.py:406` and `:431` (lazy imports of `KnowledgePointBand` and `KnowledgePointDetails`), `main.py:15` and the handler at `main.py:67-72`
  - Only producer of details: `app/adapters/curriculum_scope.py:79-94`, written through `app/application/path_service.py:201-202` and `:243-245`
  - Consumer: `app/tutor/tools.py:337-358` (reads `knowledge_point_details` and `knowledge_point_bands`), `app/tutor/prompts.py:58`
  - Tests that break and are not listed: `tests/test_practice_postgres.py:117-121`, `tests/test_path_refresh.py:106-148`, `tests/test_goal_scoped_path_postgres.py:52-57`
- **Suggested fix:**
  - Delete `curriculum_scope.py` in phase 7, not phase 5, or move the parts that are still used first: `parse_band` → `formal_evidence_adapter.py`, and `KnowledgePointDetails` plus the non-band part of `CurriculumScope.select` → `curriculum_adapter.py`.
  - State explicitly that path creation keeps writing the details snapshot, and add `app/tutor/tools.py` (band fields become null) and the three tests above to phase 5.
  - Move the `placement_test_out` deletion into phase 5, or keep `curriculum_scope` alive until phase 7.

## Finding 2: The phase 5 caller list for `PathService` / `ensure_path` misses about 12 files, and the test-deletion list is wrong

- **Severity:** High
- **Location:** Phase 5, section "Code → Chỗ gọi" and "Related Code Files → Modify tests / Delete"
- **Flaw:** Phase 5 changes `ensure_path(user_id, learning_goal_id, ...)` and the `PathService(store, user_client, content_client, ...)` constructor, where the `user_client` parameter goes away. It lists only 5 test files. Most callers pass `GoalClient` as the second positional argument, so removing `user_client` silently shifts `content` into the wrong slot. The delete glob also removes tests for a class the plan says to keep, and it misses a test file for a module it deletes.
- **Failure scenario:** The phase 5 gate hits `TypeError` or `AttributeError` in every tutor and practice Postgres test, and none of those files is in scope for any phase. `tests/test_ordering_llm.py` imports the deleted `app.learning.ordering_llm` and fails at collection. Deleting `tests/test_path_ordering*.py` removes `OrderingValidatorTest`, even though the plan keeps `OrderingValidator` for the tutor's `path_reorder` tool. That tool is then left with no unit test.
- **Evidence:**
  - Constructor and signature: `app/application/path_service.py:38-45`, `:139-149`
  - Callers not in phase 5: `app/messaging/assessment_consumer.py:121`, `app/api/dependencies.py:22-27`, `tests/test_assessment_consumer.py:52-54`, `tests/test_assessment_rabbitmq.py:67-69`, `tests/test_formal_assessment_ingestion.py:28-31,175-187,265-269`, `tests/test_formal_assessment_postgres.py:59-62,75,199,222,229,262`, `tests/test_path_refresh.py:64-69,264`, `tests/test_practice_postgres.py:96-97`, `tests/test_tutor_api_postgres.py:84`, `tests/test_tutor_engine_postgres.py:106-107`, `tests/test_tutor_quota_api_postgres.py:86`, `tests/test_tutor_reading_postgres.py:82`. The four formal/consumer tests are listed only in phase 7, which runs after phase 5's gate.
  - Missing delete: `tests/test_ordering_llm.py:14`. Wrong delete: `tests/test_path_ordering.py:76` (`OrderingValidatorTest`); the kept user is `app/application/path_reorder.py:8,74`.
  - Cross-test imports: `tests/test_path_orderer.py:18` imports from `tests.test_path_ordering`, and `tests/test_path_ordering_postgres.py:20-21` imports from both.
- **Suggested fix:**
  - List every caller above in phase 5.
  - Make the constructor change keyword-only (`PathService(store, *, content_client=..., applier=...)`) so a positional mismatch fails loudly.
  - Delete `tests/test_ordering_llm.py`, and move `OrderingValidatorTest` into a kept file next to the new validator module.

## Finding 3: Assessment V5 adds columns the schema already has, contradicting the target spec ("không đổi bảng")

- **Severity:** High
- **Location:** Phase 4, section "Architecture → Migration `V5__auto_grading_support.sql`"; Phase 2, section "Architecture"
- **Flaw:**
  - `attempt_items.answer_snapshot JSONB` already exists and phase 2 explicitly keeps it. Its only writer is the request field that phase 4 removes. V5 then adds a second column, `solution_snapshot`, for the same data.
  - `assessment_results.score` and `max_score` duplicate `Σ item_results.score` and `Σ item_results.max_score` (both item columns already exist). Phase 7 recomputes percent from `item_results` anyway.
  - `attempt_items.max_score` can live inside the same answer snapshot JSON.
- **Failure scenario:**
  - `answer_snapshot` becomes a permanently NULL column next to `solution_snapshot`, and a future dev writes the answer key into the wrong one.
  - A regrade (new result version) updates `item_results` but not `assessment_results.score`, or the reverse. `GET /result` percent (from `assessment_results`) then disagrees with the consumer percent (from `item_results`, phase 7 "TOPIC_GATE"). The learner is shown "PASSED ≥70% + solutions" while ai-learning keeps the topic IN_PROGRESS.
- **Evidence:**
  - Existing columns: `assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql:36` (`answer_snapshot`), `:93` (`item_results.score`), `V4__add_assessment_completed_event_support.sql:18` (`item_results.max_score`)
  - The event already carries per-item `score` and `max_score`: `AssessmentCompletedV2.java:30-31`
  - Phase 2 keeps the column: `phase-02-chan-lo-dap-an.md:27`
  - The target spec says no table change: `plans/260928-2019-architecture-doc-service-split/plan.md:38`
- **Suggested fix:**
  - Drop V5. Write `{answerSpec, explanation, maxScore}` into the existing `answer_snapshot` (now hidden from the structure response by phase 2), and write per-item `max_score` into `item_results` at grade time (the column exists).
  - Compute `score`, `maxScore` and `percent` in `GetAssessmentResultUseCase` from `item_results`, the same way the consumer does.

## Finding 4: Phase 4 deletes one working content port but silently keeps the user-service goal call, which now serves no consumer

- **Severity:** High
- **Location:** Phase 4, sections "Architecture" (bullet "Bỏ `KnowledgeMappingProvider`") and "Related Code Files"; Phase 8, section "Delete / đánh dấu"
- **Flaw:** The plan is not consistent about which ports it removes.
  - It deletes `KnowledgeMappingProvider` and `ContentKnowledgeMappingClient` because the KP data "already comes in the payload".
  - It never mentions `LearningGoalProvider` / `UserLearningGoalClient`. Every attempt start still makes a synchronous call to user-service to resolve the active goal. The only use of that goal was event attribution, which becomes nullable, and ai-learning stops using it (phases 5 and 7).
  - The KP-mapping endpoint is left for phase 8 with "delete if grep confirms". Grep already confirms the only caller is the client being deleted.
- **Failure scenario:** user-service is down or slow → `findActiveGoalId` throws → learners cannot start a TOPIC_TEST attempt, even though no downstream consumer reads the goal. This adds a cross-service availability dependency to the gate that decides topic PASSED.
- **Evidence:**
  - The call: `StartAssessmentAttemptUseCase.java:24,37`
  - "Dependency failures are thrown so an attempt is never started with an unknown attribution": `LearningGoalProvider.java:9-12`
  - Adapter: `infrastructure/client/UserLearningGoalClient.java:17`
  - Test mock: `StartAssessmentAttemptUseCaseTest.java:32`
  - The only caller of the KP-mapping endpoint is `ContentKnowledgeMappingClient.java:29`. Its only test user is `StartAssessmentAttemptUseCaseTest.java:33,45`; there is no client test, despite phase 4 saying "và test của chúng".
  - The endpoint is also documented in `docs/system-architecture.md:59` and `services/assessment-service/README.md:18`.
- **Suggested fix:**
  - Choose one of two options: (a) delete `LearningGoalProvider`, `UserLearningGoalClient` and its config key, and always write `learning_goal_id = null`; or (b) keep it and write down why.
  - In phase 4, delete `InternalAssessmentContentController`, `GetQuestionKnowledgePointMappingsUseCase` and their tests (`GetQuestionKnowledgePointMappingsUseCaseTest`) in the same PR, and update the two docs above. Do not defer this to a conditional phase 8 step.

## Finding 5: The ai-learning V11 schema stores the same fact in three places and has columns no endpoint reads

- **Severity:** High
- **Location:** Phase 6, section "Architecture → Migration `V11__lesson_learning.sql`"; Phase 7, section "Requirements → TOPIC_GATE"
- **Flaw:**
  - "Block passed" is recorded three times: `lesson_progress.passed_block_ids TEXT[]`, `lesson_exercise_submissions.block_passed`, and `lesson_exercise_submissions.evidence_recorded`.
  - The replay response is stored twice: `response JSONB` next to `answers`, `correct_count`, `total_count`, `score_percent` in submissions, and again as `path_review_sets.request_id` + `response`.
  - "Best score" is stored three times, and nothing reads any of them: `lesson_progress.best_score_percent`, `topic_progress.best_test_score_percent`, `topic_test_assignments.best_percent`. `last_completed_at` has no reader either. None of the 8 endpoint responses exposes them. `GET /topics` returns `status` and `completedLessonCount`; `GET /topics/{id}/test` returns `packageId`, `packageVersionId`, `formNumber`, `attemptCount`; form choice uses `assigned_at`.
  - The original spec derives completion from submissions and has no `passed_block_ids`.
- **Failure scenario:** Phase 6's own risk section says the transaction boundary between the progress tables and the path transaction is still undecided ("Chốt cách làm ở bước store"). A retry after a crash between "insert submission (block_passed=true)" and "append passed_block_ids" leaves the block passed in one table and not in the other. The gate reads `passed_block_ids` and keeps the lesson LOCKED forever. Meanwhile `evidence_recorded=false` suppresses mastery on the next valid pass. Phase 7 step 3 ("2/4 → best = 50") tests a column with no reader, so it proves nothing.
- **Evidence:**
  - `phase-06-ai-learning-api-bai-hoc-va-bai-on.md:65-70` and `:143`
  - `phase-07-ai-learning-consumer-ket-qua-de.md:31-33`, `:61`
  - The original spec has no `passed_block_ids` ("Hoàn thành tính theo từng khối từ `lesson_exercise_submissions`"): `plans/260928-2019-architecture-doc-service-split/plan.md:141-142`
- **Suggested fix:**
  - Treat `lesson_exercise_submissions` as the only source of truth. "Block ever passed" = `EXISTS(... block_passed)`. Drop `passed_block_ids` and `evidence_recorded`, and derive the latter as "no earlier passing submission for this block".
  - Keep `response JSONB` and drop the redundant scalar columns, or keep the scalars and rebuild the response.
  - Drop every `best_*` column and `last_completed_at` until an endpoint needs them.
  - Split phase 6 into two PRs: 6a (lessons, submission, gates, review rule) and 6b (review sets, form assignment). The combined size is 8 endpoints, 6 tables, 7 new test files and 3-4 days, which is too large to review as one PR.

## Finding 6: Phase 3 removes `LESSON` and widens the `ContentPackage` aggregate for no MVP benefit, and misses the consumers it breaks

- **Severity:** High
- **Location:** Phase 3, section "Architecture → Migration" (bullet "CHECK `package_type`: thêm `TOPIC_TEST`, bỏ `LESSON`") and "Domain"
- **Flaw:**
  - Dropping `LESSON` is a destructive CHECK change that the lesson pipeline does not need; lessons are separate tables now. It breaks a compile-time consumer that is not listed.
  - Adding `topicId` to the `ContentPackage` aggregate, JPA entity and mapper is only needed if a write path uses it. Content is seed-only, and the new internal endpoints can read `topic_id` through a query.
  - The existing public create endpoint will accept `TOPIC_TEST` without a topic.
- **Failure scenario:**
  - `GetReadingPassageUseCase` no longer compiles. If "fixed" by deleting the enum, the tutor's `reading_questions` flow still behaves differently for any package that was typed LESSON.
  - `POST /api/content/packages` with `packageType=TOPIC_TEST` reaches the new CHECK `topic_id NOT NULL` and returns a 500 with a raw DB error instead of a 400.
- **Evidence:**
  - Unlisted consumer: `content-service/.../application/usecase/GetReadingPassageUseCase.java:35` (`Set.of(PackageType.PRACTICE_SET, PackageType.LESSON)`) and its test `GetReadingPassageUseCaseTest.java:86`
  - Factory and constructor callers not listed: `CreateContentPackageUseCase.java:26`, `ContentPackagePersistenceMapper.java:27`, `ContentPackageTest.java:19,33,47`, `PublishContentPackageUseCaseTest.java:48`, `GetReadingPassageUseCaseTest.java:54`
  - Enum: `domain/vo/PackageType.java:8`
  - Original CHECK: `V1__create_content_tables.sql:74`
- **Suggested fix:**
  - Keep `LESSON` and only add `TOPIC_TEST`, which is additive.
  - Add the `content_packages.topic_id` column and CHECK in V7, but do not add it to the aggregate. Read it in the internal query use cases through a projection.
  - Either reject `TOPIC_TEST` in `CreateContentPackageUseCase` (400), or list that file and add a test.

## Finding 7: Phase 2 "restricts" a learner endpoint that becomes unreachable for everyone, and its test proves nothing

- **Severity:** Medium
- **Location:** Phase 2, sections "Requirements" (last bullet), "Tests After" step 6, and "Refactor" step 9
- **Flaw:** `POST /api/assessments/attempts/{id}/result` is backed by the learner entry `CreateAssessmentResultUseCase.execute`, which looks up the attempt by `(attemptId, currentUserId)`. After adding `hasAnyRole('EXAMINER','ADMIN')`, an examiner calling it for a learner's attempt gets 404. The real grader path already exists at `/api/assessments/grading/attempts/{attemptId}/results`. So step 6 ("EXAMINER → keeps old behavior") asserts behavior that is useless in practice, and the endpoint stays as dead surface.
- **Failure scenario:** Someone reads the test as proof that examiners can open results through this route. They build tooling against it and get 404 on every learner attempt. Meanwhile two controllers keep two entry points for one operation.
- **Evidence:**
  - Learner-scoped lookup: `CreateAssessmentResultUseCase.java:22-27`
  - Existing grader route: `GradingController.java:30,38-46`
  - Endpoint: `AssessmentResultController.java:29-38`
- **Suggested fix:** Delete the `@PostMapping` in `AssessmentResultController` and the learner `execute` method (plus `CreateAssessmentResultCommand` if it becomes unused). Test: CUSTOMER `POST` returns 405 or 404, and the grading route is unchanged.

## Finding 8: Phase 4 changes `AssessmentResultResult` / `AssessmentResultResponse`, but half of their consumers are missing, including grader endpoints that would inherit learner-only fields

- **Severity:** Medium
- **Location:** Phase 4, section "Requirements" (bullet `GET /attempts/{id}/result`) and "Related Code Files → Modify"
- **Flaw:** Phase 4 adds `score`, `maxScore`, `percent`, `items[]` and `solutions[]` to the shared result record and response DTO. It lists only `GetAssessmentResultUseCase`, `AssessmentResultResult` and `AssessmentResultResponse`. The same record is built in two more use cases and returned by two grader endpoints. The rule "solutions only when percent ≥ 70" is a learner rule applied to a DTO that graders share.
- **Failure scenario:** The build breaks at every constructor call that is not listed. If they are patched with nulls, grader `finalize` returns `solutions: null, items: null`, which silently changes the grader response schema (AGENTS §5 forbids a silent contract change). If they are patched with real data, the grader response now includes the solution payload, and phase 1 did not document that contract change.
- **Evidence:**
  - Constructors: `CreateAssessmentResultUseCase.java:40`, `FinalizeAssessmentResultUseCase.java:124-126`, `GetAssessmentResultUseCase.java:3`
  - Response users: `AssessmentResultController.java:37,42`, `GradingController.java:45,59`
  - Tests: `GradingControllerTest.java:159,200`, plus `CreateAssessmentResultUseCaseTest` (not listed)
  - Record: `AssessmentResultResult.java:6-7`
  - DTO: `AssessmentResultResponse.java:3`
- **Suggested fix:** Add a learner-only `LearnerAssessmentResultResponse` (and result record) returned only by `AssessmentResultController.get`. Leave the grader DTO unchanged. List all the files above, plus `AssessmentPersistenceMapperTest` if the attempt-item mapping changes.

## Finding 9: Gold plating in the seed, endpoints and tests; one approved endpoint has no data and no test, and the TFNG topic has no final-test path

- **Severity:** Medium
- **Location:** Phase 3, sections "Seed V8", "Requirements → search limit", "Implementation Steps" step 7; Phase 6, section "Requirements" (`POST /lessons/{id}/complete`) and "Tests After"
- **Flaw:**
  - **8 PRACTICE_SET packages** (8 passages, 32 questions) plus a second form passage. The acceptance criteria need at most 2 sets for one KP ("trượt thì nhận gói khác", "hết gói → gói giao lâu nhất"). The ai-learning tests use fake content (phase 6 step 6, `dependency_overrides`), so seed volume serves only one manual E2E run.
  - **`TFNG_SKILLS` topic:** it is seeded with no TOPIC_TEST. The phase 1 contract and phase 6 do not define `GET /topics/{TFNG}/test` when zero forms exist; there is no error code for it among `TEST_LOCKED`, `REVIEW_REQUIRED` and the others. The topic can never become PASSED.
  - **`POST /lessons/{id}/complete`:** no seeded lesson lacks an exercise (L1-L4 and TF1 all get questions), and none of the phase 6 test steps 2-8 covers it. The endpoint ships untested and unused.
  - **`/practice-sets/search`:** it has `limit` with default 20 and max 50, plus a count-based sort, but the only caller takes the first candidate.
  - **Query-count test (step 7):** it relies on "Hibernate statistics or SQL counting". No such harness exists in the repo, and AGENTS §3.7 explicitly says there is no query-count gate yet and prescribes Testcontainers plus SQL review instead.
- **Failure scenario:**
  - About 2 days of phase 3 go into hand-writing SQL seed rows that no automated test reads.
  - A learner who finishes TF1 opens the final test and gets an unspecified 404 or 500.
  - The `/complete` endpoint regresses silently because nothing calls it.
  - A new test-infra dependency (datasource-proxy or statistics config) arrives through a feature plan.
- **Evidence:**
  - Seed scope: `phase-03-content-bai-hoc-va-goi.md:60-65`; limit: `:27`; query-count step: `:89`
  - Endpoint: `phase-06-ai-learning-api-bai-hoc-va-bai-on.md:39`; test list with no `/complete` case: `:99-123`
  - No query-count infra: repo-wide grep for `generate_statistics|getQueryExecutionCount|datasource-proxy|QueryCount` returns 0 hits
  - Only topic seeded today: `content-service/.../V4__seed_main_flow_content.sql:5-11`
- **Suggested fix:**
  - Seed 2 PRACTICE_SET packages for KP2 and 1 for KP1. Either give TFNG one TOPIC_TEST form or drop TFNG and test "next topic opens" with fake content.
  - Add a contract error `TEST_UNAVAILABLE` for a topic with zero forms.
  - Either seed one lesson without exercises and test `/complete`, or add a DB/seed invariant that every lesson has at least one EXERCISE block and defer the endpoint.
  - Make search `limit=1`, or return the first match only.
  - Replace step 7 with a Testcontainers test plus a written SQL review, as AGENTS §3.7 prescribes.

## Finding 10: Phase 8 rewrites the architecture doc that the architecture plan reserved for itself, and "unblocking" that plan is based on a false dependency

- **Severity:** Medium
- **Location:** Phase 8, section "Related Code Files → Modify" (bullets `docs/system-architecture.md` and `plans/260928-2019-.../plan.md`); plan.md, frontmatter `blocks` and section "Dependencies"
- **Flaw:**
  - The architecture plan's phase 2 says implementation plans must NOT edit `docs/system-architecture.md`, because that phase rewrites §1-§11. It records this in its risk table.
  - Phase 8 edits §3, §6 and §11 of the same file, plus `DATABASE_V5.md`, five `FEATURE_TREE_V2.md` anchors, `AGENTS.md`, two `CLAUDE.md` files and four READMEs.
  - Architecture phase 2 is gated on the library-service split and the removal of learning-support, which this plan puts out of scope. So "Phase 8: remove the block on phase 2" unblocks nothing, and the `blocks:` edge in plan.md is wrong.
- **Failure scenario:** Two plans edit the same doc sections in sequence, and the later rewrite discards or conflicts with phase 8's edits. A reviewer then treats arch phase 2 as runnable after this plan and runs it before library-service exists, which violates its own step 1.
- **Evidence:**
  - Reserved ownership: `plans/260928-2019-architecture-doc-service-split/phase-02-gop-kien-truc-dich-sau-khi-tach-xong.md:14` ("Plan triển khai sau này không cần sửa lại file này nữa") and the risk-table row "Plan triển khai sau này cũng sửa `docs/system-architecture.md` → trùng | Ghi trong plan triển khai: phần tài liệu kiến trúc do pha này làm"
  - Real gate: same file, step 1 ("Xác nhận code đã tách (`services/library-service` tồn tại…)")
  - This plan: `plan.md:9`, `:104`; `phase-08-tai-lieu-va-don-dep.md:24`, `:30`
- **Suggested fix:**
  - Limit phase 8 to fact corrections the code change forces: `AGENTS.md` §3.8 LLM-ordering sentence, `docs/contracts/*`, the service READMEs and migration lists, and a short §11 entry.
  - Leave the §2/§3/§6 rewrites to the architecture plan's phase 2.
  - Remove `blocks: [260928-2019-architecture-doc-service-split]` and the matching `blockedBy` in that plan, or reword it as "provides input to".
  - Merge phase 8 into phases 4-7: each phase updates its own README and contract in the same PR, as phase 1's risk note already requires.

---

## Other verified defects (below finding threshold; fix while editing)

- **Phase 6 claims an evidence source the recorder cannot write.** It says `source: lesson_exercise / review_set` through `record_external_quiz_outcome`, but that method hard-codes `evidence.source = FORMAL_EVIDENCE_SOURCE` ("assessment_service") at `app/learning/external_assessment.py:72` and has no `source` parameter. `app/learning/external_assessment.py` must be added to phase 6 Modify, or lesson evidence will be tagged as formal Assessment evidence (`app/learning/formal_provenance.py:22,82`).
- **Phase 5 misstates the existing index.** It says "index unique đổi sang (user_id, attempt_id, result_version)", but `idx_pending_formal_results_goal` is non-unique (`migrations/V3__pending_formal_assessment_results.sql:14-15`; PK is `event_id`). Adding UNIQUE is a new constraint, not a change to an existing one. Either drop it (the `event_id` PK already deduplicates) or call it new.
- **Phase 7 does not say where PLACEMENT evidence is skipped.** It says "PLACEMENT: không ghi bằng chứng", but `_apply` records evidence for every type before the placement branch (`app/application/formal_result_applier.py:91-95`). The plan only mentions removing `:93-95`, not skipping `_apply`.
- **Phase 2 tests a different call shape than ai-learning uses.** The test calls `GET /api/content/knowledge-points?topicId=…`, but ai-learning calls it without `topicId` (`app/clients/content_service.py:45-47`). Test the call shape actually used.
- **Phase 5 leaves an orphan required setting.** Once `UserServiceClient` is removed, `user_service_base_url` is still required (`app/config.py:24,32,46`) and has no reader. List it in phase 5, or state that it is kept.

## Unresolved questions

1. Does anything outside this repo (a demo client) call `POST /api/assessments/attempts/{id}/result` as a learner? If not, delete it (Finding 7).
2. Is keeping `learning_goal_id` in `AssessmentCompleted.v2` a product requirement (for example, per-goal analytics later)? If not, drop the user-service call in assessment (Finding 4).

Status: DONE_WITH_CONCERNS
Summary: Ten findings. One Critical: phase 5 deletes `curriculum_scope.py` while it still has importers, so the phase 5 gate cannot pass and the tutor loses KP details. Five High: an incomplete caller/test list in phase 5, redundant schema in assessment V5 and ai-learning V11, an unexplained keep of the user-service goal call, and an unnecessary `LESSON` removal that breaks the reading use case. Four Medium: scope cuts and consumer omissions in phases 2, 3, 4, 6 and 8.
