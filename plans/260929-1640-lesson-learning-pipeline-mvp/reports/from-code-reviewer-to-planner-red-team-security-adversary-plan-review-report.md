# Red-team review (security adversary): lesson-learning-pipeline-mvp plan

- Date: 2026-09-29
- Scope: `plan.md`, `phase-01` … `phase-08`, brainstorm report `brainstorm-260929-1611-…`, scout report §2
- Method: every finding checked against code with grep/read. No builds or tests run (plan review only).
- Perspective: a logged-in CUSTOMER who wants (a) answers/solutions before earning them, (b) to pass the topic gate or reviews without the skill, (c) to read or alter other learners' state.

## Summary

The plan hides answers in the **ai-learning lesson API** well enough on paper. But it leaves **assessment-service as a learner-driven oracle and solution source for every package**, including the review pool and all test forms. It also enforces gates only on `GET` endpoints. Together these defeat the plan's core premise ("mastery measures skill, not memory of answers") and the topic-gate sequence. Findings 1–3 should block Phase 4 and Phase 6 as written.

---

## Finding 1: Assessment becomes an unlimited answer oracle and solution vending machine for every package (test forms and review pool)
- **Severity:** Critical
- **Location:** Phase 4, sections "Requirements" (request `{ packageVersionId, attemptType, mode, channel, expiresAt? }`; result `items[{…correct}]`, `solutions[]` when `percent ≥ 70`). Phase 2, "Requirements" (package reads left open). Phase 7, "Requirements" (PLACEMENT: no evidence). Phase 3, "Endpoint nội bộ" (deterministic search order).
- **Flaw:**
  - Phase 4 keeps `packageVersionId`, `attemptType` and `expiresAt` learner-controlled.
  - It adds no rule tying an attempt to what ai-learning assigned: no package-type/attempt-type check, no attempt limit, no link to `topic_test_assignments` or `path_review_sets`.
  - Every result returns per-item `correct`, even below 70%. Any result at ≥ 70% returns full `solutions` (`answerSpec` + `explanation`), whatever the attempt type or package type.
  - Phase 7 makes `PLACEMENT` attempts record no evidence, so probing is free.
  - Phase 2 leaves `GET /api/content/packages` and `/{id}` open to CUSTOMER. Those responses expose every package's `currentPublishedVersionId` and the `questionVersionId`s of its sections.
- **Failure scenario:**
  1. The learner lists packages (DRAFT included, because `status` is optional). They pick every `TOPIC_TEST` form and every `PRACTICE_SET` in the review pool.
  2. For each: `POST /api/assessments/attempts {packageVersionId, attemptType: PLACEMENT, expiresAt: null}`, answer every item "A", submit, read `items[].correct`. Repeat with "B" and "C". After k attempts (k = number of options) every CHOICE answer is known. Any run at ≥ 70% also returns FILL `accepted` lists and explanations.
  3. The learner opens the assigned review. Phase 3 search is deterministic ("sắp theo số câu giảm dần rồi `id`"), so they can even predict which package they will get. They score 4/4, and ai-learning records all-correct evidence from "fresh" questions.
  4. They take the TOPIC_GATE attempt on the assigned form at 100%. The topic is PASSED, and `wrong_kps` is empty, so no review is ever inserted.
  5. Side effect: `PREMIUM` packages (`requiredFeatureKey`) get the same treatment. Assessment has no entitlement check, which is out of scope in the plan but newly reachable through this path.
- **Evidence:**
  - Client-chosen fields: `services/assessment-service/.../api/dto/request/StartAssessmentAttemptRequest.java:3`, passed through unchanged at `api/controller/AssessmentAttemptController.java:67-75`. No validation in `domain/aggregate/AssessmentAttempt.java:60-66`.
  - `domain/vo/AttemptType.java` has `PLACEMENT, OFFICIAL_PRACTICE, MOCK, TOPIC_GATE, QUIZ`, all learner-selectable.
  - No `@PreAuthorize` on package reads: `services/content-service/.../api/controller/ContentPackageController.java:41-57`. `ContentPackageDetailResponse` exposes `currentPublishedVersionId` + `sections`. `SectionQuestionResponse` exposes `questionVersionId`.
  - PLACEMENT records no evidence: phase-07 "PLACEMENT: chỉ ghi version đã xử lý; không ghi bằng chứng".
- **Suggested fix:**
  - Assessment derives `attemptType` from content's `packageType` server-side, and ignores or rejects the client value. `TOPIC_TEST` maps only to `TOPIC_GATE`. **Reject `PRACTICE_SET`** at assessment, because reviews are graded by ai-learning. `expiresAt` comes from package rules, not the client.
  - Lock `GET /api/content/packages` and `/{id}` (and the question list) to `ADMIN`/`CONTENT_AUTHOR` in Phase 2, or strip `currentPublishedVersionId` and sections for CUSTOMER.
  - Per-item `correct` below 70% on TOPIC_GATE results conflicts with the brainstorm decision ("trả điểm và đúng/sai từng câu"), so **present this to the user rather than change it silently**. Options: (a) keep per-item correctness but allow one TOPIC_GATE attempt per assignment (see Finding 2); (b) return only the score below 70%; (c) accept the oracle and document that topic-gate results are not skill evidence.

## Finding 2: The topic-test gate is checked only when a form is assigned; the assigned form can then be retaken forever, even under `REVIEW_REQUIRED`
- **Severity:** High
- **Location:** Phase 6, "Requirements", `GET /topics/{topicId}/test`. Phase 7, "Requirements", TOPIC_GATE branch. plan.md acceptance: "Làm lại đề cuối nhận mã đề khác khi còn mã chưa làm".
- **Flaw:**
  - The gate ("mọi bài của topic đã xong, không còn bài ôn PENDING") runs only in `GET /topics/{id}/test`.
  - The consumer accepts any TOPIC_GATE result for which some `(user_id, package_version_id)` assignment exists. It does not check that the attempt started after assignment, that the assignment is unused, or that the gate still holds.
  - Assessment has no attempt cap. "Retake gets a different form" is advisory only, because the app can resend the old `packageVersionId` straight to assessment.
- **Failure scenario:**
  1. The learner is assigned form A and fails at 50%.
  2. The consumer inserts review items for KP2 and KP3 (`REVIEW_REQUIRED` now blocks lessons and the test endpoint).
  3. The learner skips the reviews and runs `POST /api/assessments/attempts {packageVersionId: A, attemptType: TOPIC_GATE}` again. The per-item oracle from the first attempt (Finding 1) makes it a guaranteed pass.
  4. The consumer finds the old assignment, marks the topic PASSED and unlocks the next topic, while the reviews stay PENDING.
- **Evidence:**
  - No assignment or attempt linkage in `StartAssessmentAttemptUseCase.java:32-59`.
  - The event carries no attempt start time or assignment id: `application/event/AssessmentCompletedV2.java:24-26` (`attemptId, …, assessmentType, status, completedAt`).
  - Phase 7 looks up the assignment only by `(user_id, package_version_id)`.
- **Suggested fix:**
  - Make an assignment single-use: add `consumed_attempt_id` / `consumed_at` to `topic_test_assignments`.
  - The consumer applies topic state only when the result's `completed_at ≥ assigned_at` and the assignment is unconsumed, and marks it consumed in the same path transaction.
  - A retake must go back through `GET /topics/{id}/test`, which re-checks gates and issues a new row, allowing re-assignment of the same form when forms run out.
  - Add tests: a second TOPIC_GATE result on a consumed assignment does not change topic state; a TOPIC_GATE result while reviews are pending and no fresh assignment exists does not change it either.

## Finding 3: Gates apply only to `GET /lessons/{id}`; submissions, `complete` and the test gate skip `TOPIC_LOCKED`/`LESSON_LOCKED`/`REVIEW_REQUIRED`, and KP capture can be dodged
- **Severity:** High
- **Location:** Phase 6, "Requirements": `GET /topics/{topicId}/lessons`, `GET /lessons/{lessonId}` ("Cổng theo thứ tự …", "Lưu `knowledge_point_ids` … lần đầu mở"), `POST …/submissions`, `POST /lessons/{id}/complete`, `GET /topics/{id}/test`. Phase 6 "Implementation Steps" 4 and 6.
- **Flaw:**
  - The gate order is specified only for `GET /lessons/{id}`. The submission and `complete` endpoints list idempotency, a 422 check and grading, but **no gate**.
  - `GET /topics/{id}/lessons` lists lessons even for LOCKED topics.
  - The test gate checks "all lessons done, no pending review" but not "topic is IN_PROGRESS".
  - `lesson_progress.knowledge_point_ids` is written only on first `GET`, and the review rule's third clause (`kp ∈ knowledge_point_ids`) depends on it.
- **Failure scenarios:**
  - **(a) Topic skip:** the learner posts submissions or `complete` for every lesson of the locked topic T3 (ids from the lessons listing or the fixed-UUID V8 seed), then calls `GET /topics/T3/test`, which passes the lesson-complete gate. After a pass, Phase 7 inserts T3 PASSED plus the next topic IN_PROGRESS, skipping T2.
  - **(b) Review bypass:** with `REVIEW_REQUIRED` pending, the learner keeps submitting other lessons' blocks, because only `GET` returns 403.
  - **(c) Review evasion:** the learner submits a lesson's blocks without ever calling `GET /lessons/{id}` (question ids from a second account or the seed). `knowledge_point_ids` stays empty, so later wrong answers on that KP never insert a review for this lesson.
- **Evidence:**
  - Plan text: Phase 6 lists gates under `GET /lessons/{lessonId}` only; step 4 tests gates as pure functions, and step 6 tests only "mở TF1 khi còn bài ôn → 403" (GET).
  - Phase 7: "topic kế theo thứ tự path thành IN_PROGRESS nếu chưa có dòng" inserts rows for skipped topics.
  - Precedent that ownership/gating must live in the write path: `services/ai-learning-service/app/practice/store.py:314-324` (`WHERE e.id = %s AND e.user_id = %s` inside the write transaction).
- **Suggested fix:**
  - Define one `authorize_lesson_access(user, lesson)` in `gates.py`. Call it from `GET /lessons/{id}`, `POST …/submissions` and `POST …/complete`, with the same error order.
  - The test gate adds `topic_progress.status = IN_PROGRESS`.
  - A submission already fetches the lesson from content to grade it, so upsert `knowledge_point_ids` / `lesson_sort_order` there too, not only on `GET`.
  - Add API tests for 403 on POST to a locked lesson, a locked topic, and under `REVIEW_REQUIRED`.

## Finding 4: `/reviews/{reviewId}`: no ownership rule, id type unspecified, and no state machine for POST on a closed set
- **Severity:** High
- **Location:** Phase 6, "Requirements", `GET /reviews/{reviewId}` and `POST /reviews/{reviewId}/submissions`. "Architecture", `path_review_items` / `path_review_sets`.
- **Flaw:**
  - Neither endpoint says `review.user_id` must equal the current user.
  - `path_review_items.id` has no stated type, and the repo already uses sequential ids (`BIGINT GENERATED ALWAYS AS IDENTITY`; practice `entryId: int`).
  - `requestId` conflict semantics (409 for another user/target) are specified for lesson submissions only.
  - Nothing says a POST must target the **currently open** set (`submitted_at IS NULL`). "Set đóng … lần GET sau giao gói khác" is enforced only by the GET path.
  - `path_review_sets.request_id` is a single nullable column on the set row, so the "one submission per set" rule is implicit, not specified.
- **Failure scenarios:**
  - **(a) IDOR:** learner B walks `reviewId` 1..N. `GET` opens sets on A's review items (`excludePackageIds` = packages assigned to *B*, so the selection logic gets confused). `POST` with wrong answers burns A's unseen packages until A falls into "hết gói → gói giao lâu nhất" and only gets packages whose solutions were already shown. A `POST` with right answers marks A's review DONE.
  - **(b) Oracle retry:** after failing a set, the learner POSTs again to the same review with a new `requestId` before calling GET. If the implementation grades against the latest (closed) set, they retry the same questions with per-question feedback until they pass. That defeats decision #6 ("lấy gói khác chưa làm").
- **Evidence:**
  - Sequential-id precedent: `services/ai-learning-service/migrations/V0_1__create_v5_mastery_tables.sql:46`, `app/api/practice.py:49` (`entry_id: int = Path(alias="entryId", gt=0)`).
  - Ownership plus conflict precedent the plan cites but does not restate for reviews: `app/practice/store.py:283-288` (`_replay_or_conflict` checks `user_id` and `entry_id`).
- **Suggested fix:**
  - Use UUID ids.
  - Every review query is `WHERE id = %s AND user_id = %s`, returning 404 (not 403) otherwise.
  - A POST locks the open set (`SELECT … FOR UPDATE WHERE review_item_id = %s AND submitted_at IS NULL`) and returns 409 `REQUEST_CONFLICT` if none is open or `requestId` belongs to another user, review or set.
  - Tests: cross-user GET/POST gets 404, POST on a closed set gets 409, and a replayed `requestId` returns the stored response.

## Finding 5: `explanation` leaks through `GET /lessons/{id}` (unpassed blocks) and `GET /reviews/{id}`, because the plan strips by denylist ("bỏ `answerSpec`")
- **Severity:** High
- **Location:** Phase 6, "Requirements": `GET /lessons/{lessonId}` ("Bỏ `answerSpec`. Khối đã đạt thì kèm `solutions`"), `GET /reviews/{reviewId}` ("gói (đoạn văn + câu, không `answerSpec`)"). Phase 6 step 6 test. plan.md acceptance ("API học viên không bao giờ trả `answerSpec`"). Phase 3, "Endpoint nội bộ" (internal lesson and package payloads include `answerSpec`, `explanation`).
- **Flaw:**
  - The internal payloads carry `explanation` for every question.
  - The plan says only "remove `answerSpec`" for lesson and review reads, and the tests assert only that `answerSpec` is absent (plus `correctAnswer`/`explanation` absent in the *submission* response).
  - A straightforward implementation (copy the internal item, drop `answerSpec`) ships `explanation` on unpassed blocks and on every review question.
- **Failure scenario:** the learner opens L1 and reads `explanation` for Q11 before answering. For review sets, every explanation is visible up front, so the review's "fresh questions" measure nothing.
- **Evidence:**
  - Seed explanations name the answer: `services/content-service/src/main/resources/db/migration/V4__seed_main_flow_content.sql:84-88`. Option A is "The benefits of urban trees" and the explanation is "The passage focuses on how urban trees improve city life."
  - Phase 3 table: `GET /lessons/{lessonId}` "Trả `answerSpec`, `explanation` …"; `GET /package-versions/{id}` items include `explanation`.
- **Suggested fix:**
  - Learner-facing question DTOs are **allowlists**: `questionVersionId, sortOrder, stem, options` (plus `passage` at section level). Pydantic response models forbid extra fields.
  - Test the exact key set of every question object in `GET /lessons/{id}` (unpassed block) and `GET /reviews/{id}`. Add `explanation` to the forbidden-keys assertion alongside `answerSpec`.

## Finding 6: `/internal/learning-content/*` hands the whole answer bank to *any* internal JWT, including learner tokens, and the plan's design makes that permanent
- **Severity:** High
- **Location:** Phase 3, "Risk Assessment" ("chỉ dưới `/internal/**`, Gateway không route … vẫn nhận mọi internal JWT"). Phase 4 "Requirements" (forward bearer). Phase 6 "Requirements" ("forward bearer khi gọi content"). plan.md "Câu hỏi mở" #3.
- **Flaw:**
  - Content's servlet chain treats `/internal/**` as "authenticated", with no role or audience check.
  - The plan *requires* learner-scoped internal JWTs to be accepted there, because ai-learning and assessment forward the learner's bearer.
  - So the only thing protecting every `answerSpec`/`explanation` is (1) the gateway not routing `/internal/**` and (2) the internal HMAC secret staying secret. Both are weaker than the plan assumes:
    - `content-service.yaml` (and every other service yaml except game) has a committed fallback for `GATEWAY_INTERNAL_JWT_SECRET`. Any environment that forgets the variable accepts tokens anyone can mint with the value in git.
    - The gateway exposes the `gateway` actuator. `GET /actuator/**` is anonymous and other methods need only "authenticated". Whether Spring Cloud Gateway's route-write operations are live at runtime is **UNVERIFIED** (needs a curl check). If they are, the route table itself is learner-writable.
  - Forwarding learner tokens to `/internal` also blocks a later move to service-only auth: the consumer still cannot mint tokens (scout B1).
- **Failure scenario:** a demo or staging deploy without `GATEWAY_INTERNAL_JWT_SECRET`, or any host where content's 8082 is reachable. An attacker signs `{sub: <uuid>, roles:[CUSTOMER]}` with the public fallback and calls `GET /internal/learning-content/package-versions/{id}` for every id from `GET /api/content/packages`, getting every answer and explanation.
- **Evidence:**
  - `shared/common-security/.../config/CommonSecurityAutoConfiguration.java:54-68`: only `anyRequest().authenticated()`, no `/internal` rule.
  - Learner bearer forwarded to `/internal`: `services/assessment-service/.../infrastructure/client/ContentKnowledgeMappingClient.java:29-31`.
  - Fallback secrets: `infra/config-server/config-repo/content-service.yaml:16`, `assessment-service.yaml:41`, `api-gateway.yaml:101` (values not reproduced here).
  - Actuator: `infra/config-server/config-repo/api-gateway.yaml:133` (`include: health,info,gateway,refresh`); `infra/api-gateway/.../security/SecurityConfig.java:101` (`GET /actuator/**` permitAll) and `:108`.
- **Suggested fix:** not decided silently. The project rules say the fallback secrets are to be reported to the user, not fixed as a side effect. Minimum inside this plan:
  1. Content rejects `/internal/learning-content/**` tokens whose roles contain only `CUSTOMER`, if a service claim can be added. Otherwise, record in the contract that answer exposure equals internal-secret exposure, and add a startup check that refuses to boot content with the fallback secret.
  2. Add an explicit gateway deny for `/internal/**`. The scout recommended this and the plan dropped it.
  3. Ask the user to decide on gateway actuator exposure (`gateway`, `refresh`).
  4. Answer the plan's open question #3 in Phase 1 with this trade-off written out, not by default.

## Finding 7: Game-service is an out-of-scope but live correctness oracle for any published question, including test and review questions
- **Severity:** Medium
- **Location:** Phase 2 overall ("Đóng các đường học viên xem được đáp án …"). plan.md "Ngoài phạm vi" ("Sửa `JacksonGameAnswerEvaluator` … để đọc khóa `correct`").
- **Flaw:**
  - The learner picks up to 20 arbitrary question ids for a game session.
  - Content's game snapshot checks only `PUBLISHED`. It does not check that the question belongs to a game-eligible pool.
  - The evaluator falls back to whole-JSON equality when the spec has none of its known keys, so submitting the object `{"type":"CHOICE","correct":"B"}` as the answer returns `correct=true` exactly when B is right. This works today against answer-spec-v1.
  - If someone later "fixes" the evaluator to read `correct`, as the scout suggested, a plain "B" works.
  - Question ids are enumerable because `GET /api/content/questions` (list) is unguarded and not in Phase 2.
- **Failure scenario:** the learner lists questions, starts `k` grammar sessions with the 20 TOPIC_TEST and review question ids, answers option j in session j, and reads `isCorrect`. That gives every CHOICE answer without touching assessment or ai-learning.
- **Evidence:**
  - `services/content-service/.../application/usecase/GetGameContentSnapshotUseCase.java:76-88` (any id, PUBLISHED only).
  - `services/game-service/.../api/dto/request/StartGameSessionRequest.java:15` (`contentIds`, max 20).
  - `services/game-service/.../application/usecase/SubmitGameAnswerUseCase.java:63` (returns `correct`).
  - `JacksonGameAnswerEvaluator` (falls through to `matches(expected, actual)` on an unknown-key object).
  - `services/content-service/.../api/controller/QuestionController.java:33-40` (list, no guard).
- **Suggested fix:**
  - The Phase 2 answer-leak audit must include game.
  - Content's game snapshot refuses question versions linked to `TOPIC_TEST` or `PRACTICE_SET` sections or `lesson_block_questions` (the same "no reuse across pools" rule the seed test enforces).
  - Guard `GET /api/content/questions` (list) for `ADMIN`/`CONTENT_AUTHOR`.
  - Keep the evaluator change out of scope, but state that fixing it without the pool check makes the leak worse.

## Finding 8: Lesson and review evidence would be stamped as proctored assessment evidence (the plan's `source` claim is false)
- **Severity:** Medium
- **Location:** Phase 6, "Architecture", `service.py`: "Ghi mastery qua `ExternalAssessmentLearningService.record_external_quiz_outcome` … `source`: `lesson_exercise` … `review_set`".
- **Flaw:**
  - The cited method has **no `source` parameter**. It unconditionally sets `evidence.source = FORMAL_EVIDENCE_SOURCE` (`"assessment_service"`).
  - Self-paced, oracle-able lesson and review answers (Findings 1, 3, 4) would be indistinguishable from assessment results in `mastery_paths.state_json` and `mastery_learning_evidence`, which is provenance forgery for any report, tutor prompt or audit.
  - They would also share one `(path_id, source, source_reference_id)` uniqueness namespace with formal evidence.
- **Evidence:**
  - `services/ai-learning-service/app/learning/external_assessment.py:30-40` (signature: `provenance`, `source_reference_id`, no `source`) and `:72`.
  - `app/learning/formal_provenance.py:22` (`FORMAL_EVIDENCE_SOURCE = "assessment_service"`).
  - `migrations/V2__formal_assessment_evidence.sql:29-31` (unique on `(path_id, source, source_reference_id)`).
- **Suggested fix:**
  - Add a sibling method (or a `source` argument) outside `app/mastery/` that sets `lesson_exercise` / `review_set`.
  - Use a separate uuid5 namespace for lesson and review references.
  - Test that the stored `source` value differs per origin, and that `supersede_external_assessment` never removes lesson or review evidence.

## Finding 9: "Không ghi mastery khi khối đã đạt" and "set đóng khi trượt" have no specified concurrency control, so parallel submits inflate mastery
- **Severity:** Medium
- **Location:** Phase 6, "Risk Assessment", first bullet ("ghi cùng kết nối, hoặc theo thứ tự … Chốt cách làm ở bước store"). Phase 6 step 5 tests.
- **Flaw:**
  - The plan leaves open whether progress tables are written on the path-transaction connection.
  - The path transaction serializes only on `mastery_paths … FOR UPDATE`. If the "block never passed" read happens on a separate connection, or before that lock, N parallel correct submissions with distinct `requestId`s all see "chưa từng đạt" and all record correct evidence.
  - The tests cover replay and mid-failure, not concurrency.
- **Failure scenario:** once the learner knows a block's answers (from the per-question feedback), they fire 20 parallel correct submissions. They get 20× correct evidence per (question, KP), mastery goes above `AI_LEARNING_REVIEW_MASTERY_THRESHOLD`, and the review-insert rule's first clause never fires for those KPs. The same race on review sets lets two submissions grade one open set.
- **Evidence:** `services/ai-learning-service/app/persistence/postgres_learning_store.py:141-168` (lock is on `mastery_paths` inside `transaction`; nothing else is serialized).
- **Suggested fix:**
  - Require the check-and-mark to be atomic and on the path-transaction connection, for example `UPDATE lesson_progress SET passed_block_ids = array_append(…) WHERE … AND NOT ($block = ANY(passed_block_ids)) RETURNING`.
  - Record evidence only if this submission was the first to pass, or the block was never passed.
  - Add a two-thread PostgreSQL test in `test_lesson_store_postgres.py` for both blocks and review sets.

## Finding 10: Phase 2 lockdown list is incomplete, and Phase 3's `LESSON` removal breaks the build
- **Severity:** Medium
- **Location:** Phase 2, "Requirements" (four controllers named as the ones without `@PreAuthorize`). Phase 3, "Architecture" (`PackageType` drops `LESSON`) and "Related Code Files".
- **Flaw:**
  - `LearningVideoController` writes are also unguarded: any CUSTOMER can create, segment and **publish** videos. It is not in the list.
  - The Phase 2 "keep reads open" rule also keeps package list/detail and question list open, which feed Findings 1 and 7. Package list returns DRAFT packages when `status` is omitted.
  - Phase 3 removes `PackageType.LESSON`, but `GetReadingPassageUseCase` and its test reference it and are not in the Modify list, so the build breaks. Whoever edits it also decides which package passages CUSTOMER can read, which is a security-relevant allowlist that must stay `PRACTICE_SET` only and must not gain `TOPIC_TEST`.
- **Evidence:**
  - `services/content-service/.../api/controller/LearningVideoController.java:58,76,93,109` (no `@PreAuthorize` in the file).
  - `ContentPackageController.java:41-57` (list with optional `status`, detail).
  - `QuestionController.java:33`.
  - `application/usecase/GetReadingPassageUseCase.java:35` (`Set.of(PackageType.PRACTICE_SET, PackageType.LESSON)`) and `src/test/.../GetReadingPassageUseCaseTest.java:86`.
- **Suggested fix:**
  - Phase 2: add `LearningVideoController` writes, and restrict package list/detail and question list to authors (the ai-learning client does not call them).
  - Phase 3: add `GetReadingPassageUseCase.java` and its test to the Modify list, with an explicit "readable = `PRACTICE_SET` only" test.

---

## Fact check (sampled claims)

| # | Plan claim | Result |
| --- | --- | --- |
| 1 | ai-learning calls topics/KPs with learner token, `app/clients/content_service.py:32` | VERIFIED (`content_service.py:32-48`) |
| 2 | Learner can open DRAFT result, `AssessmentResultController.java:29-38` | VERIFIED (`:29-38`; `CreateAssessmentResultUseCase.java:24-26`) |
| 3 | `@PreAuthorize` pattern `TopicController.java:47,61` | VERIFIED (annotations at `:49`, `:62`; `:47`/`:61` are the mappings) |
| 4 | Four content controllers lack `@PreAuthorize` | VERIFIED but **FAILED on completeness**: `LearningVideoController.java:58,76,93,109` also unguarded (Finding 10) |
| 5 | `answerSnapshot` in structure use case, result and response | VERIFIED (`GetAttemptStructureUseCase.java:3`, `AttemptStructureResult.java:3`, `AttemptStructureResponse.java:3`) |
| 6 | Goal gate `FinalizeAssessmentResultUseCase.java:88-104`, `:90-94` | VERIFIED |
| 7 | `AssessmentCompletedEventFactory.java:29` null-goal guard | VERIFIED |
| 8 | "No goal" test at `FinalizeAssessmentResultUseCaseTest :168` | VERIFIED (`:168`) |
| 9 | `AssessmentAttemptController.java:45-77`, `StartAssessmentAttemptUseCase.java:32-59` | VERIFIED |
| 10 | `ContentKnowledgeMappingClient` forwards bearer + correlation id; delete "và test của chúng" | VERIFIED (`:29-31`); no `ContentKnowledgeMappingClientTest` exists (minor) |
| 11 | Patterns `GetGameContentSnapshotUseCase.java:73-97`; mapping limit 500 | VERIFIED (`GetQuestionKnowledgePointMappingsUseCase.java:20`) |
| 12 | Next migrations: content V7, assessment V5, ai-learning V10 | VERIFIED (content V1–V6, assessment V1–V4, ai-learning V0_1–V9) |
| 13 | Inline `package_type` CHECK; drop `LESSON` | VERIFIED (`V1__create_content_tables.sql:74`); **FAILED**: `GetReadingPassageUseCase.java:35` + test `:86` omitted (build break) |
| 14 | `uq_mastery_paths_user_learning_goal` | VERIFIED (`V1__one_mastery_path_per_learning_goal.sql:16`; also referenced in `path_service.py:170`) |
| 15 | `_lock_goal`, `park_formal_result`, `pending_formal_payloads` | VERIFIED (`postgres_learning_store.py:316,323,358`) |
| 16 | `OrderingValidator` may live in `path_ordering.py` | VERIFIED (`app/learning/path_ordering.py:110`); plan's split instruction applies |
| 17 | Idempotency pattern `app/practice/store.py:260-356` | VERIFIED |
| 18 | `record_external_quiz_outcome` (`external_assessment.py:30-84`) with `source` = `lesson_exercise`/`review_set` | Location VERIFIED; **FAILED**: no `source` param, hard-coded `assessment_service` at `:72` (Finding 8) |
| 19 | `extra="forbid"` in `app/api/dto/practice.py` | VERIFIED (`:13`, `:19`) |
| 20 | `formal_evidence_adapter.py:76-117`; test `:56` | VERIFIED |
| 21 | `formal_result_applier.py:93-95` placement test-out call | VERIFIED |
| 22 | Applied-version ledger store `:276-313` | VERIFIED (`:276`, `:287`) |
| 23 | Contract `assessment-completed-v2.md:25-26`, `:89-91` | VERIFIED |
| 24 | `main.py:118-160`, `app/api/tutor.py:121` | VERIFIED |
| 25 | ai-learning `README.md:187-202` migration table | VERIFIED |
| 26 | `AssessmentAttempt.java:68-89` expire-rollback bug location | VERIFIED |
| 27 | `/internal/**` accepts any internal JWT (`CommonSecurityAutoConfiguration.java:54-68`) | VERIFIED (`:68`) |
| 28 | "Gateway không route `/internal/**`" | VERIFIED statically (`api-gateway.yaml:9-93`); gateway actuator exposed (`:133`), runtime route-write **UNVERIFIED** |
| 29 | Path transaction exists for the lesson store to join | VERIFIED (`postgres_learning_store.py:141`, `FOR UPDATE` `:165-168`) |
| 30 | `build_consumer` wiring point | VERIFIED (`app/messaging/assessment_consumer.py:116`) |

## Unresolved questions

1. Per-item `correct` on TOPIC_GATE results below 70% was a user decision (brainstorm §1). Keep it with a one-attempt-per-assignment rule, or show only the score below 70%?
2. Should assessment refuse `PRACTICE_SET` packages outright, or is a free "luyện thêm" mode through assessment wanted (brainstorm open question 3)? If wanted, its history must feed `excludePackageIds`.
3. Fallback internal secrets in config-repo and the gateway actuator exposure: which does the user want handled, and in which plan?
4. Can `/internal/learning-content/**` require a non-learner claim, given that ai-learning has no service credential today?

Status: DONE_WITH_CONCERNS
Summary: 10 findings (1 Critical, 5 High, 4 Medium). The Critical one: assessment stays a learner-driven oracle and solution source for every test form and review package, which defeats the plan's answer-hiding and mastery premise. Gates, review ownership and `explanation` stripping are also unspecified on the write and read paths that matter.
