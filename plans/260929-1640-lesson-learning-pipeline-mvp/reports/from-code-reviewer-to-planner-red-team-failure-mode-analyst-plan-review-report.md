# Red-team plan review: failure mode analyst (lesson learning pipeline MVP)

- Reviewer role: failure mode analyst plus flow tracer (every claim below was checked against code at HEAD `e3ff8ab`)
- Scope: `plan.md`, phase-01 through phase-08
- Result: 2 Critical, 4 High, 4 Medium

Two FAILED behavioral claims:
- Phase 7: "Toàn bộ phần sau khi áp bằng chứng chạy trong cùng transaction path". This fails as designed: see Finding 1.
- Phase 7: "Phần topic và bài ôn cho kết quả đỗ lại chạy lúc áp dụng". Parked results never pass through the new applier: see Finding 1.

---

## Finding 1: Consumer lesson/topic writes sit outside the idempotency boundary, so one failed redelivery drops PASSED for good
- **Severity:** Critical
- **Location:** Phase 7, "Requirements" (the Idempotent bullet and the "Chưa có path" bullet) and "Architecture" (the `formal_assessment_ingestion.py` bullet); Phase 6, "Architecture" (`store.py`)
- **Flaw:** Phase 7 says all post-evidence work runs in the path transaction. The architecture contradicts this in three ways:
  - Ingestion calls `LessonResultApplier` "sau khi áp dụng", which means after `apply_to_path` has returned.
  - Phase 6 says `app/lessons/store.py` opens its own connection on each call.
  - The path transaction commits when the `with` block of `apply_to_path` exits. The only way to join it is the private `_active` ContextVar, and only `PostgresLearningStore`'s own methods can reach it through `_active_connection`. The plan never wires the lesson store onto that connection.

  The parked-result path is also affected. `PathService._create_path` calls `FormalResultApplier.apply_pending`, which calls `apply_to_path` directly and never goes through ingestion. The API's `PathService` is built without any lesson applier. So the claim "topic/review logic runs when parked results are applied" fails.
- **Failure scenario:** A TOPIC_GATE 3/4 event arrives.
  1. `apply_to_path` commits the evidence and the `formal_assessment_result_versions` row.
  2. `LessonResultApplier` then raises (DB blip, constraint bug, or the consumer container is restarted).
  3. The consumer NACKs and the message goes to the retry queue.
  4. On redelivery, `applied_result_version` returns the same version, and `apply_to_path` returns `"duplicate"` early.
  5. The topic is never PASSED, the next topic is never IN_PROGRESS, and the review item is never inserted.

  Nothing reconciles this afterwards. The learner is stuck until they retake a different test form.
- **Evidence:**
  - `services/ai-learning-service/app/persistence/postgres_learning_store.py:150-156`: nesting works only through the ContextVar.
  - `postgres_learning_store.py:221`: commit on exit.
  - `postgres_learning_store.py:270-274`: `_active_connection` is private to the store.
  - `app/application/formal_result_applier.py:77-85`: duplicate short-circuit inside the transaction.
  - `formal_result_applier.py:115`: the transaction ends here.
  - `app/application/formal_assessment_ingestion.py:53`: ingestion returns right after `apply_to_path`.
  - `app/messaging/assessment_consumer.py:58-70`: NACK and retry.
  - `app/application/path_service.py:53,258`: apply_pending goes straight to `FormalResultApplier`.
  - `app/api/dependencies.py:20-27`: no lesson applier is wired.
- **Suggested fix:**
  - Inject `LessonResultApplier` into `FormalResultApplier`, not into ingestion. Call it inside `with self._store.transaction(path_id)`, before `record_applied_result`.
  - Put the lesson/topic SQL on a store API that uses the active path connection, for example a public `active_connection(path_id)` on `PostgresLearningStore`, or lesson methods on that store.
  - With this wiring, `apply_pending` gets the same behavior for free.
  - Add a test: raise inside the lesson applier and assert that no ledger row exists. Then redeliver and assert the topic is PASSED.

## Finding 2: Exercise-submission dual write — both orderings the plan allows either lose or duplicate mastery evidence
- **Severity:** Critical
- **Location:** Phase 6, "Risk Assessment" (the "Transaction path và nhiều bảng mới" bullet) and "Architecture" (`service.py` `source` / `source_reference_id`)
- **Flaw:** The atomicity decision is deferred ("Chốt cách làm ở bước store"). The fallback option, "ghi tiến độ idempotent, sau đó ghi bằng chứng tất định", is unsafe. The evidence-identity design also does not match the code:
  - **(a)** `record_external_quiz_outcome` hard-sets `evidence.source = FORMAL_EVIDENCE_SOURCE`, and it has no source parameter. A `lesson_exercise` / `review_set` source cannot be set without changing `external_assessment.py`, and that file is not in phase 6's Modify list.
  - **(b)** If the source is changed, `formal_source_reference` returns `None` for any non-formal source. The projection then writes `source_reference_id = NULL`, and the partial unique index `WHERE source_reference_id IS NOT NULL` never protects lesson evidence. Nothing in the DB would stop duplicates.
  - **(c)** uuid5 seeded by `submission_id` (a server-generated id) changes on a retry whose submission row never committed.
- **Failure scenario:**
  - **(A) Progress first, then evidence.** The submission row with its `response` commits. The path transaction then fails (`LearningConflictError` or a dropped connection). The client retries with the same `requestId`, and the store replays the saved response as designed. The evidence is never written. The block is already marked passed, so later submissions get `evidence_recorded=false`. Mastery for those KPs stays wrong permanently, and `reevaluate_reviews` works from wrong numbers.
  - **(B) Evidence first, then progress.** The evidence commits but the submission insert fails. The retry finds no row, creates a new `submission_id` and therefore a new uuid5, and records the evidence twice. `compute_mastery` counts both attempts.
- **Evidence:**
  - `app/learning/external_assessment.py:61-72`: the source is hard-set.
  - `app/learning/formal_provenance.py:22,80-93`: references only exist for the formal source.
  - `postgres_learning_store.py:240-268`: projection rebuild.
  - `migrations/V2__formal_assessment_evidence.sql:29-31`: the partial unique index.
  - `app/practice/store.py:296-356`: the "pattern" phase 6 cites uses one connection and never touches the mastery aggregate, so it does not cover this dual write.
- **Suggested fix:**
  - Use one transaction in this order:
    1. Open the path transaction.
    2. On the same connection, look up the `request_id` (replay or conflict).
    3. Read `lesson_progress ... FOR UPDATE`.
    4. Mutate `tx.progress`.
    5. Insert the submission and update `lesson_progress`.
    6. Run `reevaluate_reviews`.
    7. Commit once.
  - Seed uuid5 from (`request_id`, `question_version_id`, `knowledge_point_id`).
  - Choose the source explicitly:
    - Either keep `assessment_service` and use a provenance string that can never decode as `assessment:…`, so `supersede_external_assessment` cannot match it.
    - Or add a `source` parameter and extend `formal_source_reference` plus the unique index to cover the new sources.
  - Add fault-injection tests at both failure points, then assert exactly one evidence row per (question, KP).

## Finding 3: Lesson state has no per-user serialization — race conditions cause double evidence, lost `passed_block_ids` updates, and a duplicate first-topic row
- **Severity:** High
- **Location:** Phase 6, "Requirements" (`POST .../submissions`, `GET /topics`) and "Architecture" (V11 `lesson_progress.passed_block_ids TEXT[]`)
- **Flaw:** The "khối chưa từng đạt" check and the `passed_block_ids` update are a read-modify-write, and the plan specifies no lock. The lesson store opens a connection per call. The only per-user mutex in the service is the `mastery_paths` row lock (one path per user after phase 5), and the plan does not require lesson reads to take it first.
- **Failure scenario:**
  1. The contract says a retry uses a new `requestId`. After a client timeout, two tabs or a resubmit hit the same not-yet-passed block. Both read `passed=false` and both record evidence. This breaks the acceptance criterion "nộp lại khối đã đạt: mastery không đổi".
  2. Blocks B1 and B2 of one lesson are submitted concurrently. Each writes its own array, so one passed block is lost. The lesson never completes, and resubmitting the lost block records evidence again.
  3. Both requests see "all blocks passed". Each gathers "mọi lần nộp của bài" from its own snapshot, so `wrong_kps` misses the other submission and a review is never inserted.
  4. The first `GET /topics` inserts `topic_progress` for the first topic. If the client fires it in parallel, one request gets a PK violation and a 500.
- **Evidence:**
  - `postgres_learning_store.py:165-169`: `SELECT ... FOR UPDATE` on `mastery_paths` is the only lock.
  - `app/practice/store.py:308-316`: the in-transaction `FOR UPDATE` pattern the plan does not require.
  - `app/application/path_service.py:167-175`: precedent that concurrent creates need explicit `UniqueViolation` handling.
- **Suggested fix:**
  - Require every lesson/review mutation to run inside the path transaction, so the per-user row lock serializes them.
  - Read `lesson_progress` `FOR UPDATE`.
  - Update with `array_append ... WHERE NOT (:block = ANY(passed_block_ids)) RETURNING`, and use the returned row count to decide whether to record evidence.
  - Use `INSERT ... ON CONFLICT DO NOTHING` for `topic_progress`.
  - Add a two-thread PostgreSQL test: same block, different `requestId`s → exactly one evidence set.

## Finding 4: The phase 4/7 rollout order contradicts itself and loses events for good in both directions
- **Severity:** High
- **Location:** `plan.md`, "Thứ tự" (lines 73-74); Phase 4, "Risk Assessment"; Phase 7, "Requirements" (`package_version_id` bắt buộc)
- **Flaw:** `plan.md` says both "Phase 7 cần 4 và 6" and "hoặc bật phase 7 trước". Phase 7 cannot ship before phase 4 under its own dependency rule.
  - **Old consumer, new producer:** the current adapter requires the goal. A null goal raises `ContractError`, which goes straight to the DLQ with no retry. The service has no DLQ replay tooling; only the topology is declared.
  - **New consumer, old events:** phase 7 makes `package_version_id` mandatory for TOPIC_GATE. Pre-phase-4 TOPIC_GATE events lack it and go to the DLQ. These can still be in the outbox (the relay retries up to 20 times) or in the retry queue. Pre-phase-4 events parked in `pending_formal_assessment_results` fail `to_command` inside `apply_pending` and stay parked forever.
- **Failure scenario:** Phase 4 is merged and assessment is restarted on the host. The `ai-learning-consumer` container still runs the phase-6 image. A goal-less learner (the plan's target user) passes a topic test. The event is DLQ'd, the topic is never PASSED, and nothing alerts anyone.
- **Evidence:**
  - `app/adapters/formal_evidence_adapter.py:108`: `learning_goal_id=_uuid(data, "learning_goal_id")`.
  - `app/messaging/assessment_consumer.py:50-56`: contract error → DLQ.
  - `app/messaging/topology.py:24,56`: the DLQ is only declared.
  - `services/assessment-service/.../infrastructure/messaging/OutboxRelay.java:29,40`: up to 20 relay attempts.
  - `app/application/formal_result_applier.py:61-66`: unparseable parked rows stay parked.
- **Suggested fix:**
  - Split phase 7:
    - 7a: the adapter accepts a null goal and treats `package_version_id` as optional. It depends only on phase 5 and must ship before phase 4.
    - 7b: the TOPIC_GATE logic.
  - When a TOPIC_GATE event has no `package_version_id`, do what phase 7 already does for a missing assignment: log, apply the evidence, and skip the topic step. Do not raise `ContractError`.
  - Add a DLQ replay step (shovel or `rabbitmqadmin`) to the E2E and to the runbook.

## Finding 5: The path contains every ACTIVE content topic and is never refreshed on read — progression dead-ends, and lesson evidence is silently dropped
- **Severity:** High
- **Location:** Phase 5, "Requirements" (`ensure_path`); Phase 6, `GET /topics` ("topic đầu path" IN_PROGRESS); Phase 7, TOPIC_GATE ("topic kế theo thứ tự path"); Phase 3, the seed
- **Flaw:**
  - **(a) Topics without lessons block progress.** `get_curriculum` returns every ACTIVE topic, and the path mirrors it. Some topics have no published lessons and no `TOPIC_TEST`: any topic created through `TopicController`, and DEMO_READING is seeded at `sort_order` 900, so lower-ordered topics are clearly expected. For such a topic:
    - "mọi bài đã xong" is vacuously true;
    - `/test-packages` returns an empty list, so no assignment is possible;
    - the topic can never be PASSED, and every topic after it stays `TOPIC_LOCKED`.
  - **(b) Existing paths never pick up new content.** `ensure_active_path` only creates a path; an existing one is returned unchanged. The dev paths that V10 keeps were built before the V8 seed, so they have no TFNG_SKILLS module and no KP2–KP5. For a KP that is not in the path, `find_knowledge_point` returns `None` and the applier skips it as "unknown".
- **Failure scenario:** A dev user whose path predates V8 runs the Lan scenario:
  - L2/L3 submissions record nothing for KP2–KP4.
  - Getting Q15 wrong on the topic test (KP2) finds KP2 "unknown", so no review is inserted. The acceptance criterion "chèn bài ôn cho KP2" fails.
  - `GET /topics` never shows TFNG_SKILLS.
  - After PASSED, the "next topic" is a topic with no lessons, or nothing at all.
- **Evidence:**
  - `app/clients/content_service.py:44`: every ACTIVE topic is kept.
  - `services/content-service/src/main/resources/db/migration/V4__seed_main_flow_content.sql:10`: `sort_order` 900.
  - `app/application/path_service.py:76-84,156-158`: an existing path is returned without a merge.
  - `app/mastery/policy.py:197-205`: `find_knowledge_point` returns `None`.
  - `app/application/formal_result_applier.py:136-139`: unknown KPs are skipped.
- **Suggested fix:**
  - Build the path only from topics that have at least one PUBLISHED lesson, using one batch internal call. Alternatively, have the gates auto-skip topics that have no lessons or no test.
  - Merge the curriculum on read when content has changed (hash or `updatedAt`), or have `GET /topics` call the refresh.
  - Define what a submission does when a KP is missing from the path: refresh the path, or return 409. Also define how `reevaluate_reviews` treats a missing mastery value.

## Finding 6: V10 destructive migration — parked results are stranded, a timestamp tie can stop the whole AI Learning stack, and there is no rollback
- **Severity:** High
- **Location:** Phase 5, "Architecture", Migration `V10__one_mastery_path_per_user.sql`
- **Flaw:**
  - **(a) Stranded parked results.** Parked results are applied only when a path is created; `path_service.py:258` is the only `apply_pending` call site. Consider a user with rows parked under goal G2 who, after V10, keeps the G1 path. In the per-user model that user never gets a new path, so those rows are never applied.
  - **(b) No tie-breaker.** "Giữ path có `updated_at` mới nhất" says nothing about equal timestamps. On a tie both paths survive, `CREATE UNIQUE INDEX uq_mastery_paths_user` fails, and the Flyway container exits non-zero. Because of `depends_on: service_completed_successfully`, neither `ai-learning-api` nor `ai-learning-consumer` will start.
  - **(c) Wrong keep rule.** The newest path can be an empty one from a recent goal switch, so the rule can delete the path that holds months of evidence.
  - **(d) The plan misstates the current index.** It says "index unique đổi sang ...", but the V3 index is not unique. Making it unique adds a constraint that `park_formal_result`'s `ON CONFLICT (event_id)` does not cover. A duplicate with a new `event_id` would then raise `UniqueViolation`, retry, and end in the DLQ.
  - **(e) No safety net.** The V1 precedent refuses duplicates with `RAISE EXCEPTION` instead of deleting them. The plan has no backup or restore step, and Flyway cannot undo cascaded deletes.
- **Failure scenario:** A shared dev DB holds two paths per user with equal `updated_at` values, or a learner's G2 results are parked. The migration either stops the AI Learning stack, or it silently strands the parked results and deletes the path that holds the evidence.
- **Evidence:**
  - `app/application/path_service.py:258`: the only drain point.
  - `migrations/V3__pending_formal_assessment_results.sql:14-15`: the index is non-unique.
  - `app/persistence/postgres_learning_store.py:342-346`: `ON CONFLICT (event_id)` only.
  - `migrations/V1__one_mastery_path_per_learning_goal.sql:2-14`: refuse, don't delete.
  - `docker-compose.yml:149-151,165-169`: api and consumer are gated on the migrate container.
- **Suggested fix:**
  - Rank paths with `ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY <evidence count> DESC, updated_at DESC, path_id)` so there is always one winner.
  - Log the deleted counts with `RAISE NOTICE`.
  - Keep the pending index non-unique.
  - Add a drain step for existing paths: in the existing-path branch of `ensure_path`, apply pending rows by user under the path lock when any exist.
  - Add a pre-check query and a `pg_dump` step to the approval step before running on the shared DB.

## Finding 7: GETs with side effects (test form, review set) race and burn assignments, and assessment never enforces them
- **Severity:** Medium
- **Location:** Phase 6, "Requirements" (`GET /topics/{topicId}/test`, `GET /reviews/{reviewId}`) and "Architecture" (V11 `topic_test_assignments` UQ, `path_review_sets`)
- **Flaw:**
  - **Every GET writes a test assignment.** A form is "chưa làm" when it has never been assigned, not when it has never been taken.
  - **Reuse breaks the unique key.** With UQ(`user_id`, `package_version_id`), "hết thì lấy mã giao lâu nhất" plus "ghi assignment" becomes an INSERT on an existing key. That is a `UniqueViolation` from the third call on, unless it is written as an upsert.
  - **Concurrent GETs.** Two concurrent GETs pick the same form, and one gets a 500.
  - **Review sets can be opened twice.** Nothing makes the open set unique per review item, so two concurrent `GET /reviews` create two open sets. `POST /reviews/{id}/submissions` carries no `reviewSetId`, so one set gets graded and the other stays open. After a failure the next GET returns the same package, which breaks "trượt thì giao gói khác".
  - **Assessment does not enforce assignments.** It accepts whatever `packageVersionId` and `attemptType` the client sends. Once form A is assigned, a learner can start TOPIC_GATE attempts on A directly while reviews are PENDING. This bypasses `TEST_LOCKED` / `REVIEW_REQUIRED`. Because each result shows per-item correct/incorrect, a 4-question form can be brute-forced on the second try.
- **Failure scenario:** A learner opens the test page (gets A), leaves, reopens (gets B), and refreshes again (gets A, the oldest). The retake is the same form, so "làm lại đề cuối nhận mã đề khác" fails. Or the third open returns a 500.
- **Evidence:**
  - `services/assessment-service/.../application/usecase/StartAssessmentAttemptUseCase.java:40`: client-supplied `packageVersionId` / `attemptType`, which phase 4 keeps.
  - `app/practice/store.py:273-278` and `app/application/path_service.py:167-175`: the only existing handling of this race type; the plan specifies neither for the new tables.
- **Suggested fix:**
  - Make assignment a POST that also starts the attempt server-side, or that idempotently returns an existing unstarted assignment.
  - Pick "not taken" forms by `last_completed_at IS NULL`, and upsert when reusing a form.
  - Add a partial unique index on `path_review_sets(review_item_id) WHERE submitted_at IS NULL`, use `ON CONFLICT` to return the existing set, and include `reviewSetId` in the POST.
  - In the consumer, either ignore TOPIC_GATE results while reviews are PENDING, or record this as an accepted MVP risk.

## Finding 8: A regrade (result v2) reverses evidence but not PASSED / unlock / reviews, and the plan's rules contradict each other
- **Severity:** Medium
- **Location:** Phase 7, "Risk Assessment" ("Hai bản ghi của cùng attempt"); Phase 4, "Requirements" (`GET /attempts/{id}/result`)
- **Flaw:**
  - **v2 is allowed on an auto-graded result.** An examiner can open v2 on a COMPLETED auto-graded result, because COMPLETED is not "gradable" and a new version is created.
  - **Only evidence is reversed.** The consumer replaces v1's evidence, but it does not version PASSED, the next topic's IN_PROGRESS row, or review items created from v1's wrong KPs.
  - **The rules conflict.** The plan says "PASSED tính theo version mới nhất; best giữ max". If v2 is below 70%, there are two outcomes:
    - Revoking PASSED re-locks a topic the learner is already studying.
    - Keeping PASSED makes "theo version mới nhất" false.
  - **The score disappears during regrade.** `GetAssessmentResultUseCase` returns the latest version even when it is DRAFT. Phase 4 hides the score for non-COMPLETED results, so the learner's score and solutions vanish while v2 is being graded.
- **Failure scenario:** Examiner regrades a passed TOPIC_GATE to 50%. ai-learning drops v1's evidence, but the topic stays PASSED (or is revoked mid-lesson), and v1's reviews remain PENDING and block every lesson.
- **Evidence:**
  - `services/assessment-service/.../api/controller/GradingController.java:45`: grader entry point.
  - `CreateAssessmentResultUseCase.java:34-39`: v2 is created after COMPLETED.
  - `GetAssessmentResultUseCase.java:3`: `findLatestByAttemptId`.
  - `app/application/formal_result_applier.py:88-91` and `app/learning/external_assessment.py:109-147`: supersede touches only mastery state.
- **Suggested fix:**
  - Make PASSED one-way (never revoked) and keep `best` as the max. Document that a regrade only affects mastery and reviews.
  - Store `attempt_id` / `result_version` on review items so a supersede can cancel PENDING reviews that came from the old version.
  - Have the result GET return the latest COMPLETED version, or forbid regrading auto-graded TOPIC_GATE attempts.

## Finding 9: With auto-grading, the known expire-on-submit rollback bug blocks learners
- **Severity:** Medium
- **Location:** `plan.md`, "Ngoài phạm vi" (line 84); Phase 4, "Requirements" (auto-grade on submit); Phase 1, contract (`expiresAt?` in the create-attempt request)
- **Flaw:** A submit after `expiresAt` calls `expire()` and then throws, inside `@Transactional`. The whole transaction rolls back, so the attempt stays IN_PROGRESS, is never graded, and emits no event. No scheduler expires attempts: only `OutboxRelayScheduler` has `@Scheduled`, and `ExpireAssessmentAttemptUseCase` runs only on demand. In the new flow, submitting is the only way to get a TOPIC_GATE result.
- **Failure scenario:**
  - A learner submits a timed topic test two seconds late because of network lag and gets a 4xx.
  - Every retry fails the same way, and the attempt stays IN_PROGRESS forever.
  - To try again, the learner re-opens the test. That rotates the form (Finding 7) and throws away all the answers.
- **Evidence:**
  - `services/assessment-service/.../domain/aggregate/AssessmentAttempt.java:73-76`: `expire` then throw.
  - `.../application/usecase/SubmitAssessmentAttemptUseCase.java:9`: `@Transactional`, so everything rolls back.
  - `.../infrastructure/messaging/OutboxRelayScheduler.java:21`: the only `@Scheduled` job.
- **Suggested fix:** Pick one of these:
  - Bring the fix into scope: return an EXPIRED outcome without rolling back, or use `noRollbackFor`, and optionally grade the answered items within a grace window.
  - Or ban `expiresAt` for TOPIC_GATE in the phase 1 contract and add a test for it.

## Finding 10: Every exercise submission now rewrites the learner's whole evidence projection while holding the per-user lock
- **Severity:** Medium
- **Location:** Phase 6, "Requirements" (evidence is written on every submit of a block that has not passed yet, and on every review set) and "Architecture" (`service.py` → `record_external_quiz_outcome`)
- **Flaw:** Every changed path commit does the following:
  - deletes all of the path's rows in `mastery_learning_evidence`;
  - re-inserts every evidence row with one `execute` per row (a repository call in a loop, which AGENTS §3.7 forbids on the request path);
  - rewrites the whole `state_json`.

  Today this runs for rare formal results and for tutor tools. The plan adds it to every block submission, every review and every test. Cost grows with the learner's total evidence, and all of it runs under the `mastery_paths` row lock, which tutor tools and the consumer also need.
- **Failure scenario:** A learner has about 3k evidence rows.
  - Each block submit issues about 3k INSERT round trips (seconds) while holding the lock.
  - Tutor tool calls and consumer applies for that user queue behind it.
  - Client timeouts trigger same-`requestId` retries, which also wait on the lock.
- **Evidence:**
  - `app/persistence/postgres_learning_store.py:196-208`: `state_json` rewrite.
  - `postgres_learning_store.py:240-268`: per-row INSERT loop.
  - `app/tutor/tools.py:258,288,342,377,414`: tutor tools that contend for the same lock.
- **Suggested fix:**
  - Before phase 6, make the projection sync incremental: insert only new ordinals, and delete only superseded references. Alternatively, batch with `execute_values`.
  - Add a latency or query-count test with at least 1k evidence rows.

---

## Unresolved questions
- `AttemptType.QUIZ` (`services/assessment-service/.../domain/vo/AttemptType.java:3`) has no rule in phase 7: record evidence? reevaluate? ignore?
- Phase 4 adds a bigger content call inside `@Transactional` start (`StartAssessmentAttemptUseCase.java:32-38` already calls user/content before writes). This holds a JDBC connection for up to 2 s + 5 s. Is Hikari pool exhaustion under a slow content service acceptable for MVP?
- Which `source` value will lesson/review evidence carry in `mastery_learning_evidence`? This decides whether any DB-level dedupe exists (Finding 2).

Status: DONE
Summary: Two Critical transaction-boundary defects: consumer lesson writes happen after the idempotency ledger commits, and HTTP submissions dual-write across connections. Either one can permanently lose topic PASSED or duplicate/lose mastery evidence. Rollout order, V10, path staleness, and GET side effects add High/Medium failure modes, all traced to file:line.
