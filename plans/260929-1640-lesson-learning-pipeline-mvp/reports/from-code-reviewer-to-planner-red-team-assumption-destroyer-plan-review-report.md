# Red-team review: lesson-learning-pipeline-mvp (Assumption Destroyer + Scope Auditor)

Reviewer: code-reviewer. Date: 2026-09-29. Scope: plan.md + phase-01..08. Every claim below was checked against code on
branch `feat/main-follow`. Paths are relative to repo root; ai-learning paths are relative to
`services/ai-learning-service/` unless they start with `services/`.

## Finding 1: The assessment service lets learners pick any package and attempt type, so the answer-hiding and multi-form design can be bypassed
- **Severity:** Critical
- **Location:** Phase 4, sections "Requirements" (create-attempt request `{packageVersionId, attemptType, ...}`, result `items[{correct}]`, `solutions[]` at ≥70%) and "Success Criteria" ("Không còn đường nào trả `answerSpec` cho học viên trước khi đạt"). Phase 2, "Requirements". plan.md, "Đề cuối" row.
- **Flaw:** The plan assumes learners can reach a TOPIC_TEST form or a review PRACTICE_SET only through ai-learning (`GET /topics/{id}/test`, `GET /reviews/{id}`). That is not true. Assessment accepts any `packageVersionId` together with an `attemptType` the client chooses. It has no role check and no check that the attempt type matches the package type. The content package list and detail endpoints are open to CUSTOMER and return every package's `packageType` and `currentPublishedVersionId`. After phase 4, every objective attempt is auto-graded on submit and returns a `correct` flag for each item, plus `solutions[]` at ≥70%.
- **Failure scenario:** A learner calls `GET /api/content/packages?status=PUBLISHED` and collects the version ids of form A, form B, and every PRACTICE_SET. The learner starts `attemptType=MOCK` on form B and on each KP1 practice set, then submits blank or guessed answers. The result shows which items are correct. One or two retries reach ≥70%, and the full `solutions[]` comes back. The learner then sits the real TOPIC_GATE and the review sets already knowing the answers, which defeats the purpose of having several forms. Side effects: every fabricated MOCK event runs `reevaluate_reviews` (phase 7), so the learner can add or dodge reviews. Labelling any attempt `PLACEMENT` stops its evidence from being recorded (phase 7).
- **Evidence:** `services/assessment-service/.../api/dto/request/StartAssessmentAttemptRequest.java:3` (`attemptType` comes from the client, only `@NotNull`); `.../api/controller/AssessmentAttemptController.java:45-77` (no `@PreAuthorize`, no type/package check); `.../domain/vo/AttemptType.java:3`; `services/content-service/.../api/controller/ContentPackageController.java:29-57` (GET list/detail, no `@PreAuthorize` on class or method); `.../api/dto/response/ContentPackageResponse.java:16,19` (`packageType`, `currentPublishedVersionId` exposed).
- **Suggested fix:** Assessment must take the attempt type from the package, not from the client. The content payload of `GET /internal/learning-content/package-versions/{id}` already carries `packageType` and `topicId`. Force `TOPIC_TEST` to `TOPIC_GATE`, and reject PRACTICE_SET review packages and `PLACEMENT` unless they come from an allowed flow. Alternatively, have ai-learning record the assignment and have assessment verify it through an internal call. Either restrict `GET /api/content/packages*` to authors or filter out TOPIC_TEST and PRACTICE_SET for CUSTOMER. Add both items to phase 2's scope.

## Finding 2: Phase 5 deletes `curriculum_scope.py` although other modules still import it and the tutor depends on it
- **Severity:** High
- **Location:** Phase 5, "Architecture → Code → Xóa" and "Related Code Files → Delete".
- **Flaw:** `app/adapters/curriculum_scope.py` does more than goal scoping:
  - it defines `parse_band`, which the formal-event adapter uses;
  - it defines `KnowledgePointBand` and `KnowledgePointDetails`, which the store imports;
  - it is the only producer of the `details` (skill, description) that are written to `mastery_path_knowledge_point_details`. The tutor tool `knowledge_point_details` reads that table.

  Other modules import it at load time. `placement_test_out.py` is deleted only in phase 7. `path_ordering.py` imports it at the top of the file, and phase 5 keeps that file for `OrderingValidator`.
- **Failure scenario:** After phase 5, `import app.application.path_service` pulls in `formal_result_applier`, which pulls in `placement_test_out`, which fails with ImportError. Neither the API nor the consumer starts, and the phase 5 regression gate cannot pass. If the implementer only patches the imports, new paths get no details rows. The tutor's `knowledge_point_details` tool then returns `skill: null, description: ""`, and practice questions get worse without any test catching it. Deleting `tests/test_path_ordering*.py` also deletes `OrderingValidatorTest`, which leaves the kept validator (used by tutor reorder) with no unit tests.
- **Evidence:** `app/adapters/formal_evidence_adapter.py:17,183`; `app/persistence/postgres_learning_store.py:406,431`; `app/learning/placement_test_out.py:26`; `app/learning/path_ordering.py:11`; `app/application/formal_result_applier.py:19`; `app/adapters/curriculum_scope.py:33-47,91-94`; `app/application/path_service.py:244-245`; `app/tutor/tools.py:340-358`; `tests/test_path_ordering.py:76-141`.
- **Suggested fix:**
  - Move `parse_band`, `KnowledgePointBand` and `KnowledgePointDetails` into a module that survives phase 5.
  - Keep building details from `get_curriculum` output in both create and refresh.
  - Either delete placement test-out in phase 5, or keep `curriculum_scope.py` until phase 7.
  - Move `OrderingValidator` and `OrderingValidatorTest` into their own module and test file before deleting `path_ordering.py`.

## Finding 3: The transaction boundary is left undecided, and phase 6's store design breaks phase 7's idempotency claim
- **Severity:** High
- **Location:** Phase 6, "Architecture → store.py" ("mở kết nối mỗi lần gọi") and "Risk Assessment → Transaction path" ("Chốt cách làm ở bước store"). Phase 7, "Requirements → Idempotent" ("Toàn bộ phần sau khi áp bằng chứng chạy trong cùng transaction path").
- **Flaw:** `PostgresLearningStore.transaction()` opens and commits its own psycopg2 connection. Only methods that go through `_active_connection` take part in that transaction. A lessons store that opens a connection per call commits on its own, outside the path transaction and outside its row lock. The two phases contradict each other, and phase 6 leaves the choice for later.
- **Failure scenario:**
  - (a) Submission: the lessons store commits a `lesson_exercise_submissions` row with the cached `response` and `evidence_recorded=true`. The path transaction then raises `LearningConflictError` (for example, a revision race with a tutor turn) and rolls back. The client retries with the same `requestId`, gets the cached response, and the evidence is never written.
  - (b) Consumer: `topic_progress` PASSED and next-topic IN_PROGRESS commit, then the path transaction rolls back before `record_applied_result`. Redelivery re-applies the result on top of state that is already partly committed.
  - (c) Race: two parallel submissions for the last two blocks of a lesson each read `passed_block_ids` before the other commits, so neither sets `completed_at`. Resubmitting a passed block records nothing, so the lesson can stay incomplete forever.
- **Evidence:** `app/persistence/postgres_learning_store.py:141-229` (own `psycopg2.connect`, commit at `:219`), `:270-274` (`_active_connection` guard), `:206-209` (`LearningConflictError`); `app/application/formal_result_applier.py:77-115` (one transaction boundary).
- **Suggested fix:** Decide this in the plan, not in the store step. Every write to `lesson_*`, `topic_progress`, `path_review_*` and `topic_test_assignments` should go through the active path connection. Every block submission should open the path transaction, including submissions for blocks that already passed, so work for one user is serialized. Add a failure-injection test: raise after evidence is recorded, then assert that no submission row and no progress change remain.

## Finding 4: `record_external_quiz_outcome` does not support the call phase 6 describes
- **Severity:** High
- **Location:** Phase 6, "Architecture → service.py" (`source`: `lesson_exercise` / `review_set`, uuid5 `source_reference_id` "để không đụng unique").
- **Flaw:** The method has no `source` or weight parameter. It always sets `evidence.source = "assessment_service"`, and it requires `module_id` and `provenance`. The database uniqueness guard applies only to formal-source evidence. The projection computes `source_reference_id` through `formal_source_reference`, which returns `None` for any other source. The partial unique index then never fires. Weights in `knowledgePointMappings` are ignored by the engine.
- **Failure scenario:** Two outcomes, both bad:
  - (a) The implementer adds a `source` argument. The "deterministic uuid5 to avoid duplicates" protection then disappears without any error, because `source_reference_id` is stored as NULL.
  - (b) The implementer keeps the formal source. Lesson and review evidence is then stored as `assessment_service` next to real exam evidence and cannot be told apart. `FormalProvenance`/supersede logic also becomes one encoding mistake away from deleting lesson evidence.
- **Evidence:** `app/learning/external_assessment.py:30-41,72`; `app/learning/formal_provenance.py:22,80-93`; `app/persistence/postgres_learning_store.py:231-268`; `migrations/V2__formal_assessment_evidence.sql:28-31`; `app/application/formal_result_applier.py:136-139,154-163`.
- **Suggested fix:** Specify a new sibling method in `app/learning/`, not `app/mastery`, that takes an explicit `source`. Extend the projection so it emits `source_reference_id` for the new sources too. State in the contract that weights are not used.

## Finding 5: The one path per user never picks up new topics or knowledge points (stale path)
- **Severity:** High
- **Location:** Phase 5, "Requirements" (created once; `POST /paths` is the only refresh). Phase 6, "GET /topics". Phase 7, "topic kế theo thứ tự path". Phase 3, seed V8 (KP2–KP5 and topic `TFNG_SKILLS` are new).
- **Flaw:** `ensure_active_path` only creates a path when none exists. Content changes reach an existing path only through `POST /paths`, and the new learner flow never calls it. Recording evidence needs the knowledge point to be in `state_json.modules` (for `module_id` and `find_knowledge_point`). The formal applier skips unknown knowledge points without failing.
- **Failure scenario:** A dev user already has a path, which V10 keeps. The V8 seed then adds KP2–KP5 and TFNG_SKILLS.
  - `GET /topics` shows only DEMO_READING.
  - Submissions and exam items for KP2–KP4 are skipped as unknown, so `mastery_levels[KP2]` stays missing. The acceptance scenario "đề sai Q15 → chèn L3" therefore fails.
  - After DEMO_READING passes there is no next topic in the path, so TF1 can never be reached.

  The same thing happens to any lesson KP that is added later or belongs to another topic.
- **Evidence:** `app/application/path_service.py:76-103`; `main.py:118-131`; `app/application/formal_result_applier.py:136-139`; `app/mastery/policy.py:197-205`; `app/application/curriculum_refresh.py:63-73` (new modules are appended at the end, not placed by `sort_order`).
- **Suggested fix:** Have `GET /topics` call `refresh_active_path`; the merge only adds and does nothing when content is unchanged. Also refresh when a submission meets an unknown knowledge point. Define what happens to lesson knowledge points that are not in the path. Decide how "appended at the end" fits with ordering by `sort_order`.

## Finding 6: "Path order" is not a safe topic sequence: lesson-less or parent topics block the learner, and tutor reorder skips the sequence
- **Severity:** High
- **Location:** Phase 6, "GET /topics" (first topic in the path becomes IN_PROGRESS) and "GET /topics/{id}/test" gate. Phase 7, "TOPIC_GATE ≥70% → topic kế theo thứ tự path". Phase 5, "Giữ `OrderingValidator` nếu `path_reorder.py` còn dùng".
- **Flaw:** The path has one module per ACTIVE topic, flattened parent-first, and that includes topics with no knowledge points. The plan has no rule for a topic without lessons or without a TOPIC_TEST. Separately, the tutor's `path_reorder` tool can reorder modules on the learner's request. Phase 7 picks the "next topic" from the current module order.
- **Failure scenario:**
  - (a) An author creates a parent topic, or any topic with `sortOrder < 900` that has knowledge points. It becomes the first topic and is set IN_PROGRESS. Its lesson list is empty, and the test gate is trivially open but no TOPIC_TEST exists. Every other topic stays `TOPIC_LOCKED`, so the learner is stuck.
  - (b) The learner asks the tutor to move an advanced topic to the front. After the first topic passes, the "next topic" is the one the learner chose. This contradicts "Path tạo theo `sort_order`" and the out-of-scope item "Xếp lại topic chưa học".
- **Evidence:** `app/adapters/curriculum_adapter.py:31-74`; `app/clients/content_service.py:79-102`; `app/adapters/curriculum_scope.py:104-109`; `app/application/path_reorder.py:44-81`; `app/tutor/tools.py:114-116`; `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql:9` (`parent_topic_id`).
- **Suggested fix:** Build the topic sequence from content: topics that have at least one PUBLISHED lesson and a TOPIC_TEST, in `sort_order`. Store that sequence, for example in `topic_progress` or a snapshot, rather than reading the mutable `state_json.modules`. Either stop the tutor from reordering modules (allow only knowledge-point order within a module), or keep tutor reordering out of topic sequencing.

## Finding 7 (Scope Auditor): The new progress and review state duplicates the kept DeepTutor engine and disagrees with it
- **Severity:** Medium
- **Location:** plan.md "Quyết định áp dụng" ("Tutor, `/status`, `/progress` dùng cùng path"; "Ngưỡng bài ôn"). Phase 6, V11 tables and `app/config.py` `review_mastery_threshold`.
- **Flaw:** Four places where the new state overlaps existing state:
  1. **Two mastery thresholds.** `AI_LEARNING_REVIEW_MASTERY_THRESHOLD` (0.6) sits next to the engine's gate of 0.9 for MEMORY/PROCEDURE. CONCEPT/DESIGN knowledge points have no quiz gate at all; they clear only through a qualitative pass.
  2. **Three review queues.** `path_review_items` is added alongside the engine's `review_queue`, which every recorded outcome rebuilds, and alongside practice-notebook reviews.
  3. **Two "what to learn next" answers.** `topic_progress`/lesson gates sit next to the `next_objective` cursor, which returns due reviews first and then the first unmastered knowledge point in module order.
  4. **A frozen copy of content.** `lesson_progress.knowledge_point_ids` and `lesson_sort_order` are captured "lần đầu mở" and never refreshed.
- **Failure scenario:**
  - A topic passes with KP mastery 0.75. The lesson pipeline says no review is needed, while `/status` and the tutor keep telling the learner to "learn" or "review" that KP, sometimes one in a LOCKED topic.
  - CONCEPT/DESIGN knowledge points never clear the gate from quiz evidence, so `/status` stays on the first one forever.
  - After a learner opens a lesson, an author edits that lesson's knowledge points. The consumer then chooses the teaching lesson from the stale list.
- **Evidence:** `app/mastery/policy.py:37-52,75-80,219-232`; `app/application/formal_result_applier.py:152-153`; `app/learning/external_assessment.py:76-83`; `migrations/V6__practice_notebook.sql:14-59`.
- **Suggested fix:**
  - Choose one authority for "what next". Either derive `/status` from the lesson pipeline, or scope it explicitly to the tutor in the contract.
  - Document how the review threshold relates to the engine gate.
  - Refresh `lesson_progress.knowledge_point_ids` on every open and submit, since content is fetched on those calls anyway.

## Finding 8: Every lesson submission rewrites the path's whole evidence projection, one INSERT per row
- **Severity:** Medium
- **Location:** Phase 6, "Requirements" (record evidence for every (question, KP) pair on each submission of a block that has not passed yet) and "Success Criteria" ("Không gọi content hay repository trong vòng lặp").
- **Flaw:** Every path commit rewrites the full `state_json`. It then DELETEs all `mastery_learning_evidence` rows for the path and re-INSERTs them one row at a time. Until now such writes were rare (exam results, tutor grades). The plan turns every failed block retry into a path commit that adds evidence, and retries are unlimited. Per-question correct flags also encourage guess-and-retry.
- **Failure scenario:** About 50 lesson questions × knowledge points × retries soon adds up to thousands of evidence rows. Each submission then does thousands of INSERT round trips while holding the per-user row lock. Tutor turns for that user wait on the lock, and latency grows with the learner's total activity. That is O(n) per request and O(n²) over time.
- **Evidence:** `app/persistence/postgres_learning_store.py:196-205,231-268`; `AGENTS.md` §3.7 and §5 ("không gọi repository... trong vòng lặp").
- **Suggested fix:** Make the projection append-only (insert only new ordinals) or batch it (`execute_values`) inside phase 6. Also cap evidence per block, for example record only the first failed submission and the passing one, and add a cap on retries.

## Finding 9: Phase 3 breaks the tutor reading use case, and the V4 seed pollutes the review pool
- **Severity:** Medium
- **Location:** Phase 3, "Architecture → `PackageType` bỏ `LESSON`", "Seed V8", and "Success Criteria" ("Test cũ của content pass").
- **Flaw:** Production code (the tutor reading path) and a test both reference `PackageType.LESSON`, and neither file is in the Modify list. Separately, the V4 seed has a PUBLISHED PRACTICE_SET with one KP1 question. That question is about "urban trees", while the V6 passage in the same section is about green roofs. The practice-set search will return this package as a KP1 candidate.
- **Failure scenario:**
  - (a) content-service no longer compiles after the enum change. Fixing it means editing `GetReadingPassageUseCaseTest`, which the plan does not list.
  - (b) A learner fails two KP1 review sets. The third review then gets the one-question V4 package, where 1/1 = 100% passes and 0/1 fails, and the question does not match the passage. The phase 6 tests use fake content and assume 4 questions per set, so they will not catch this.
- **Evidence:** `services/content-service/.../application/usecase/GetReadingPassageUseCase.java:35`; `.../test/.../GetReadingPassageUseCaseTest.java:86`; `V4__seed_main_flow_content.sql:30-38,79-90,113-128`; `V6__seed_demo_reading_passage.sql:4-33`.
- **Suggested fix:** Add `GetReadingPassageUseCase` and its test to the Modify list. Archive the V4 package in a new migration, or require a minimum question count in the search. Add a seed test that checks the exact search results for each knowledge point.

## Finding 10: Regrades and republished forms leave topic, review and assignment state inconsistent
- **Severity:** Medium
- **Location:** Phase 7, "Risk → Hai bản ghi của cùng attempt". Phase 6, "GET /topics/{id}/test" (fallback to the oldest assigned form). V11 `topic_test_assignments` with UQ(`user_id`, `package_version_id`).
- **Flaw:** An examiner can open result v2 on any auto-graded attempt, and superseding it removes only the evidence. The plan says "PASSED tính theo version mới nhất" but gives no rule for un-passing a topic. Assignments are keyed by package version, so a republished form counts as a new form. The fallback can also hand out an old version, but content returns only PUBLISHED versions.
- **Failure scenario:**
  - v1 scores 75%: the topic is PASSED, topic 2 is IN_PROGRESS, and a KP2 review is inserted.
  - v2 then scores 50%: either the topic is un-passed while the learner is already in topic 2, or PASSED stays and contradicts "latest version". The v1 review stays either way.
  - An author republishes form A as a new version: a learner who failed A gets A-v2 as the "different form".
  - Every form has been assigned and the oldest is an archived version: attempt creation returns 404/503, and the learner cannot retake the test.
- **Evidence:** `services/assessment-service/.../api/controller/GradingController.java:30,45`; `.../application/usecase/CreateAssessmentResultUseCase.java:40-50`; `app/learning/external_assessment.py:109-147`; Phase 3, "Chỉ trả bài, gói, câu hỏi đã `PUBLISHED`".
- **Suggested fix:** Make PASSED one-way, or define an explicit revert rule. Judge "different form" by `package_id`. Make the fallback always resolve the package's current published version. Add a test for the regrade path.

## Unresolved questions / smaller items
- Phase 2, step 6: restricting `POST /attempts/{id}/result` to EXAMINER/ADMIN leaves a useless endpoint. The use case still requires the caller to own the attempt (`CreateAssessmentResultUseCase.java:34-36`), so an examiner gets 404 on learners' attempts. The grader flow already exists in `GradingController`. Deleting the learner route is simpler.
- `answer-spec-v1` says a learner answer is a string, but assessment stores `attempt_responses.response_payload` as JSONB with no defined shape (`V1__create_assessment_tables.sql:44`, `SaveAttemptResponseRequest.payload`). The rule for extracting the answer must be part of the shared vectors, or Java and Python can grade differently.
- Phase 4, step 9 expects 400 for the old `sections` field. Spring Boot's default ignores unknown JSON properties, so today that request succeeds. Decide the behavior explicitly.
- `AttemptType.QUIZ` is not handled in phase 7.
- `main.py:88-90` maps every content `HTTPStatusError` to 502, so `GET /lessons/{unknown}` returns 502 instead of 404.
- `StartAssessmentAttemptUseCase.java:46` still calls user-service for the goal on every attempt start. A user-service outage therefore blocks the final test, even though the plan says the goal is no longer needed.

Status: DONE
Summary: The plan's central assumptions do not hold in the code. Answers can still be harvested through assessment (Finding 1). Phase 5's deletions break imports (Finding 2). The path transaction boundary is contradictory (Finding 3). The evidence API does not support custom sources (Finding 4). The per-user path is never refreshed and its module order is not a safe topic sequence (Findings 5 and 6). Findings 7–10 are Medium items that should be fixed before the contract is approved.
