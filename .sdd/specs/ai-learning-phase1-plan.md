---
type: implementation-plan
version: phase1
status: ready-for-contract-finalization
service: ai-learning-service
scope: DeepTutor v1.6.9 integration baseline
external_baseline: HKUDS/DeepTutor v1.6.9
---

# Phase 1: ai-learning-service and DeepTutor

## 1. Goal and invariants

Implement the existing Python/FastAPI ai-learning-service scaffold as the IELTSPath integration boundary around DeepTutor v1.6.9. The service adapts platform data, calls DeepTutor, persists its aggregate state, verifies platform identity, exposes learner APIs, and consumes formal assessment events.

**DeepTutor is the sole adaptive learning engine.** IELTSPath must not add another mastery algorithm, adaptive policy, review scheduler, learning planner, weakness engine, or next-action engine. Platform code may validate and transform inputs, invoke DeepTutor operations, persist results, and map safe response DTOs.

Phase 1 must keep these rules:

- Domain state and every adaptive decision come from DeepTutor LearningProgress, LearningService, policy, and SpacedRepetitionScheduler.
- Content Service owns canonical curriculum and Knowledge Point metadata. Assessment Service owns formal results. User Service owns learner identity and learning goals. AI Learning owns only its DeepTutor state and projections.
- PostgreSQL is authoritative in production. mastery_paths.state_json is the serialized LearningProgress; evidence rows are a query projection.
- Cross-service calls use service APIs or integration events. No service reads another service's database.
- Final Phase 1 decisions in this plan are fixed. Only unresolved implementation details and contracts explicitly listed in Sections 13, 16, and Required Follow-up Changes remain open.

**Final invariant:** DeepTutor remains the sole adaptive learning engine. IELTSPath provides platform integration, PostgreSQL persistence, security, formal-assessment adaptation, and RabbitMQ integration, without implementing a second mastery, policy, scheduler, or planner.

## 2. Verified repository baseline

Inspected before preparing this plan:

- Root README.md, AGENTS.md, .gitmodules, .sdd/specs/SERVICE_ARCHITECTURE_V2.md, .sdd/database/DATABASE_V5.md, the existing AI Learning service README and requirements.
- DeepTutor submodule third_party/deeptutor, checked out at tag v1.6.9, commit da856ad67075d49b483150ac44ebca710cb5f266.
- DeepTutor deeptutor/learning/{service,storage,models,mastery,policy,scheduler}.py, deeptutor/services/session/sqlite_store.py, deeptutor/services/practice/storage.py, and the submodule pyproject.toml.
- Existing Gateway token issuer and common security validation code; existing Assessment Service result entities and tracked event references.

The repo has an existing services/ai-learning-service/{main.py,README.md,requirements.txt} scaffold, but no implemented service architecture, assessment event schema, or broker configuration. SERVICE_ARCHITECTURE_V2.md references AssessmentCompleted.v2; it does not define the payload. DATABASE_V5.md is the authoritative PostgreSQL table and column inventory. Do not copy table DDL into this plan.

### DeepTutor v1.6.9 facts that constrain implementation

- LearningService and LearningStore are synchronous. get_or_create(), replace_modules_for_path(), record_quiz_attempt(), calculate_mastery(), update_mastery(), grade_and_record(), and record_qualitative_for_path() are real source methods.
- The existing synchronous LearningStore exposes SQLite transaction/context-manager semantics; it uses BEGIN IMMEDIATE, revisions, and mutation/event handling. An async SQL adapter cannot be injected into this interface.
- grade_and_record() owns the normal answer grading → attempt/evidence → mastery/review state pipeline, but grades from DeepTutor's own pending question/expected answer. There is no public method for recording a pre-graded external item result through the same complete pipeline. This is a DeepTutor boundary extension point, not a reason to recreate mastery logic in IELTSPath.
- KnowledgeType values are MEMORY, CONCEPT, PROCEDURE, and DESIGN. Quantitative gates apply to MEMORY/PROCEDURE; qualitative mastery applies to CONCEPT/DESIGN.
- compute_mastery() uses the latest five attempts with recency weights and caps a one-attempt score at 0.5, and a two-attempt score at 0.8. For [True, False, True], the score is about 0.661.
- policy.next_objective() returns actions in this precedence: answer_pending, review, then the first unmastered objective (probe if new, practice for quantitative evidence, assess for qualitative evidence), otherwise complete.
- The current SpacedRepetitionScheduler is an exponential forgetting baseline with type-specific priors; it is not calibrated FSRS.
- LearningService.replace_modules_for_path(..., append=False) preserves supplied module and KP ids. Do not use append mode when canonical Content KP UUIDs must remain DeepTutor IDs.

## 3. Target architecture and flows

Learner request:
Gateway validates external token and forwards short-lived internal JWT
→ ai-learning-service validates internal JWT and gets user UUID from sub
→ application use case loads the caller's path
→ DeepTutor LearningService/policy operation
→ synchronous PostgreSQL LearningStore in the same mutation transaction
→ safe API response DTO

POST /paths:
required User Service learning goal
→ required Content Service curriculum and KPs
→ CurriculumAdapter
→ LearningService.get_or_create + replace_modules_for_path(append=False)
→ mastery_paths(state_json, user_id, learning_goal_id)

AssessmentCompleted.v2 [NEW CONTRACT REQUIRED]:
Assessment Service transactional outbox → RabbitMQ → AI Learning consumer
→ FormalEvidenceAdapter (data transformation only)
→ FormalAssessmentIngestionService
→ DeepTutor LearningService external-assessment operation
→ aggregate + mastery/evidence/review changes + evidence projection
→ one PostgreSQL transaction
→ acknowledge broker delivery only after commit

FormalEvidenceAdapter does not calculate mastery, choose objectives, or schedule reviews. FormalAssessmentIngestionService coordinates validation, DeepTutor invocation and transaction outcome; it contains no pedagogy. Where the exact upstream DeepTutor API is missing, use the minimal extension described in Section 5.5 and keep it inside DeepTutor's service boundary.

### Phase 1 acceptance flow

```text
Learner sets active learning goal
  → User Service
  → POST /api/ai-learning/paths or first AssessmentCompleted event
  → ensurePath(userId, learningGoalId)
  → Content curriculum with canonical KPs
  → CurriculumAdapter
  → DeepTutor LearningService
  → LearningProgress
  → PostgreSQL

Learner completes assessment
  → Assessment Service grading
  → Question→KP mapping snapshot
  → finalized result version
  → transactional outbox
  → RabbitMQ
  → AI Learning consumer
  → UUIDv5 idempotency and ensurePath()
  → FormalEvidenceAdapter
  → FormalAssessmentIngestionService
  → DeepTutor record_external_assessment(...)
  → LearningProgress: attempts/evidence, quantitative mastery, explicit qualitative state,
    repetition state and review queue
  → atomic PostgreSQL commit
  → RabbitMQ ACK
  → DeepTutor policy.next_objective()
  → GET /api/ai-learning/status
```

Weight remains attribution metadata throughout. Formal grading remains in Assessment Service; all adaptive state transitions remain in DeepTutor.

## 4. Integration matrix

| Responsibility | Phase 1 decision |
| --- | --- |
| DeepTutor LearningProgress, LearningModule, KnowledgePoint, KnowledgeType, LearningEvidence, QuizAttempt, ErrorRecord, RepetitionState, ReviewTask | Reuse upstream models and state semantics. |
| DeepTutor mastery calculation, policy, qualitative gate and scheduler | Reuse unchanged; do not fork or reproduce their logic in IELTSPath. |
| Path creation and curriculum replacement | Call LearningService.get_or_create() and replace_modules_for_path(..., append=False). Add the smallest ownership-aware extension only if required to persist user_id and learning_goal_id in the same creation transaction. |
| Content curriculum → DeepTutor | CurriculumAdapter: map topic/module metadata, use str(content knowledge_points.id) as KnowledgePoint.id, and explicitly map each of the four learning_type enum values. |
| Assessment payload → DeepTutor | FormalEvidenceAdapter transforms the contract; FormalAssessmentIngestionService delegates mutation to DeepTutor. A pre-graded quantitative result requires the extension in Section 5.5. |
| PostgreSQL | Replace only the persistence boundary needed for Phase 1 Mastery Path flow. Use a synchronous LearningStore implementation. |
| Session/chat runtime and Question Notebook/PracticeStore | Defer their adapters and endpoints in Phase 1. DATABASE_V5 remains the prepared schema baseline; if future work ports these stores, preserve the full semantics in Section 7. |
| DeepTutor TopicMetadata / TopicSource storage | Not part of the canonical curriculum path flow. DATABASE_V5 has no mastery_topic_meta or mastery_topic_sources; Content Service remains canonical. Do not add these tables. Any LearningStore method needed for an in-scope call must be assessed explicitly (TO VERIFY). |

## 5. Implementation sequence and boundaries

### 5.1 Implementation order and contract gates

Implement in dependency order. Do not start the RabbitMQ consumer or formal evidence mutation until `AssessmentCompleted.v2`, the database idempotency constraint, and the DeepTutor external-assessment extension contract are approved.

1. Verify the pinned DeepTutor team fork/submodule and v1.6.9 baseline.
2. Lock the architecture contracts listed in Section 8 and Required Follow-up Changes.
3. Design and apply the required database constraints through the repository's approved schema process.
4. Package/install DeepTutor from the local pinned submodule.
5. Implement the synchronous `PostgresLearningStore`.
6. Implement internal JWT verification.
7. Implement the User Service active-goal client.
8. Implement the Content Service curriculum client.
9. Implement `CurriculumAdapter`.
10. Implement concurrency-safe `ensurePath(userId, learningGoalId)`.
11. Add the minimal DeepTutor `record_external_assessment(...)` extension.
12. Define and accept the shared `AssessmentCompleted.v2` contract.
13. Make Assessment Service snapshot Question → KP mappings.
14. Implement Assessment Service transactional outbox → RabbitMQ publishing.
15. Implement the AI Learning RabbitMQ consumer.
16. Implement `FormalEvidenceAdapter`.
17. Implement `FormalAssessmentIngestionService`.
18. Implement result-version-aware idempotency and supersession.
19. Implement progress/status/map APIs.
20. Add focused unit tests.
21. Add PostgreSQL integration tests.
22. Add RabbitMQ integration tests.
23. Add end-to-end coverage.

### 5.2 Confirm contracts and package the pinned submodule

1. Preserve the root Git submodule pin at the team's DeepTutor fork, based on upstream v1.6.9. Keep the fork diff minimal and limited to platform integration; the submodule change is an explicitly approved boundary.
2. Package the service with its existing Python requirements convention. Install DeepTutor reproducibly from the checked-out local submodule during the service image build (for example, pip install ./third_party/deeptutor). Do not modify sys.path at runtime.
3. Let the resolver install the dependencies declared by DeepTutor's pyproject.toml. Add only direct dependencies required by IELTSPath. The existing service requirements already declare SQLAlchemy and a PostgreSQL driver; use their synchronous SQLAlchemy interface for this adapter. Do not introduce asyncpg/async SQLAlchemy into the LearningStore path.
4. Resolve and accept the Assessment event contract and question-to-KP snapshot ownership before enabling a consumer (Section 8).

**Acceptance:** a clean image build can import DeepTutor from the pinned submodule without runtime path hacks or manually overriding DeepTutor's transitive dependency versions.

### 5.3 Synchronous persistence boundary

Implement PostgresLearningStore against the actual synchronous DeepTutor store/transaction contract. The FastAPI layer may move blocking application/database work to its supported threadpool execution path. Any async broker consumer must bridge to the synchronous mutation without passing an awaitable store into DeepTutor.

The adapter must implement only the LearningStore operations exercised by Phase 1 and keep their transaction/context-manager behavior, including aggregate load/create, mutation, save, interaction/event operations needed by those calls, revision conflicts, and evidence projection synchronization. Inventory the exact interface from the pinned source before implementation; do not claim unneeded methods are supported.

The SQL strategy must preserve all of these invariants together:

- A mutation loads one current aggregate revision inside one database transaction.
- A path row lock and revision compare-and-swap prevent concurrent lost updates; stale revision fails with the DeepTutor-compatible conflict behavior.
- state_json, revision, matching mastery_interactions, mastery_events, and the synchronized mastery_learning_evidence projection commit atomically when applicable.
- A successful aggregate mutation increments revision exactly once.
- A path lease remains a separate single-mutating-turn contract; it is not replaced by the row lock or CAS.
- Broker acknowledgement happens only after the transaction commits. Redelivery after a failed or uncertain commit must be safe through item-level idempotency.

Do not describe PostgreSQL FOR UPDATE as a literal replacement for SQLite BEGIN IMMEDIATE; it is one part of the target transaction protocol.

### 5.4 Path bootstrap and curriculum mapping

1. Derive user_id exclusively from validated internal JWT sub.
2. Fetch the caller's active learning goal from User Service and the required published curriculum/KPs from Content Service. Exact HTTP route/DTO shapes are TO VERIFY against those services before implementation. Exactly one active goal is a USER CONTRACT REQUIRED; if there is no active goal, return a domain error and never invent a default goal.
3. If either required service is unavailable, fail bootstrap with a retryable dependency error. Do not create an empty or goal-less path. Optional enrichment, if any is later added, must be identified separately.
4. Validate all curriculum KPs before mutation. learning_type is required metadata. Explicit mapping is MEMORY → KnowledgeType.MEMORY, CONCEPT → KnowledgeType.CONCEPT, PROCEDURE → KnowledgeType.PROCEDURE, DESIGN → KnowledgeType.DESIGN. Missing or invalid values are a curriculum contract violation: reject the bootstrap, record a safe diagnostic, and do not fall back to another type.
5. Use str(content knowledge_points.id) for DeepTutor KnowledgePoint.id. Preserve module/topic identity and order as source data allows. KP.code is display/business-readable metadata, not aggregate identity. The same canonical Content UUID must match AssessmentCompleted mappings.
6. Route both `POST /api/ai-learning/paths` and the Assessment consumer through one application operation: `ensurePath(userId, learningGoalId)`. It looks up `(user_id, learning_goal_id)`, returns the existing path, or creates exactly one path and persists ownership in the same transaction. Do not duplicate path-creation logic across API and consumer.
7. Make concurrent ensure calls safe with a database uniqueness constraint and conflict recovery: after a uniqueness race, reload and return the winning path. An application SELECT-before-INSERT alone is insufficient. See Required Follow-up Changes.
8. Call DeepTutor `LearningService.get_or_create(path_id)` and `replace_modules_for_path(..., append=False, ...)`; do not construct `LearningProgress` manually. The smallest ownership-aware extension at the DeepTutor service/store boundary remains an implementation detail to verify.

**Acceptance:** one `(user_id, learning_goal_id)` identifies one path; API and event bootstrap converge on that same path under concurrency. Invalid curriculum and dependency failures leave no partial path. `/progress` and `/status` use only the active goal's path; no active goal returns a domain error.

### 5.5 Formal assessment ingestion and required DeepTutor extension

LearningService.grade_and_record() cannot be used for an external result as-is: it performs DeepTutor grading from a stored expected answer. record_quiz_attempt() alone records an attempt but is not the complete mastery/evidence/review pipeline. Therefore, add or propose a narrowly scoped public operation inside the pinned DeepTutor LearningService boundary before implementing the consumer, for example record_external_assessment(...) (proposed extension name, not an existing API).

That operation must reuse DeepTutor's existing state transitions and internal operations for:

- adding the attempt and evidence to LearningProgress;
- computing and storing mastery using calculate_mastery() / update_mastery();
- the existing error/evidence semantics where the formal result contains sufficient information;
- SpacedRepetitionScheduler and review-queue rebuild behavior where the existing pipeline requires them;
- the same transaction/event/revision semantics used by the store.

Do not copy algorithm or schedule logic into the IELTSPath service. If upstream source requires a modification to share a helper currently private to LearningService, expose the smallest public operation inside that boundary. Keep this extension isolated from unrelated DeepTutor product features.

Dispatch by the KnowledgeType from the path's validated curriculum snapshot:

- MEMORY / PROCEDURE: quantitative, attempt-based ingestion of the Assessment Service's finalized pre-graded correctness/result through the external-result operation. Reuse DeepTutor attempt, evidence, mastery, scheduler and review-queue semantics. Do not call `grade_and_record()` when it would re-grade an answer against an expected answer; do not send answer keys to AI Learning.
- CONCEPT / DESIGN: update qualitative mastery only when the event has an explicit per-KP qualitative judgment (for example `PASS`, `FAIL`, or `NOT_ASSESSED`). An overall IELTS Writing/Speaking band or score never implies qualitative mastery. Without an explicit judgment, supporting evidence may be stored if DeepTutor supports it, but `qualitative_mastery` remains unchanged and DeepTutor policy decides the next action.

One event follows one pipeline:

AssessmentCompleted
→ FormalEvidenceAdapter transforms item-level data
→ FormalAssessmentIngestionService validates/maps and delegates
→ DeepTutor LearningService mutation
→ LearningProgress (attempt/evidence/mastery/errors/repetition/review)
→ state and projections commit atomically

### 5.6 Learner API and identity boundary

Implement only the Phase 1 learner APIs with defined use cases:

- POST /api/ai-learning/paths
- GET /api/ai-learning/progress
- GET /api/ai-learning/status
- GET /api/ai-learning/paths/{pathId}/map

`/progress` and `/status` resolve the active learning goal from User Service, then use `ensurePath(userId, activeGoalId)`. Historical/inactive goals do not drive these default endpoints. If no active goal exists, return a domain error; do not select a random path or create a fake goal. Do not add duplicate self aliases. Take identity only from the validated JWT, never a request-supplied user ID or identity header.

Map policy.next_objective() and map summary to explicit platform response DTOs. Do not return serialized LearningProgress, RepetitionState, expected answers, internal events, or model reasoning. Keep API fields limited to user-visible progress and the action/objective needed by the client. Existing API route, status-code, and Gateway conventions must be checked before wiring; do not silently change another service's public contract.

### 5.7 RabbitMQ consumer and transaction

RabbitMQ is the Phase 1 transport. Exchange and routing-key names, retry counts/timeouts, DLQ bindings, and the Python client library are TO DEFINE during implementation. Do not introduce a broker library during this planning edit.

Consumer behavior:

1. Receive and validate a finalized `AssessmentCompleted.v2` event, including envelope, result version and self-contained item/KP snapshots.
2. Use the event's `learning_goal_id` for the assessment's associated goal. If it is omitted, resolve the learner's sole active goal from User Service; if no unique active goal can be resolved, reject/defer the event as a contract error rather than attributing it to a fabricated goal. Derive deterministic UUIDv5 evidence references, then call `ensurePath(user_id, learning_goal_id)`.
3. Apply DeepTutor mutations, result-version state, `state_json`, revision, evidence projection and internal `mastery_events` within one PostgreSQL transaction for the affected path, wherever feasible. Never commit the evidence projection before its aggregate.
4. ACK only after commit. A redelivery after commit/before ACK is safe through deterministic UUIDv5 identities and database uniqueness.
5. NACK/requeue transient failures according to retry policy. Send permanent contract/validation failures to the dead-letter flow; do not retry indefinitely.

Assessment Service must commit the formal result and its `outbox_events` row in the same database transaction. An outbox publisher delivers to RabbitMQ after commit. The Assessment integration outbox is distinct from AI Learning's internal DeepTutor `mastery_events` log.

## 6. Mastery and policy behavior the plan must preserve

- Quantitative types are MEMORY and PROCEDURE; qualitative types are CONCEPT and DESIGN.
- Quantitative mastered status is based on DeepTutor's weighted score meeting its 0.9 gate, not lifetime raw percentage. Recent attempts and confidence caps apply.
- Qualitative mastered status uses DeepTutor's qualitative assessment state. Quiz accuracy alone does not pass this gate.
- After one correct quantitative attempt, DeepTutor's score is capped at 0.5; next_objective() must not advance to the next KP merely because that first response is correct.
- next_objective() action precedence and action strings are those in the pinned policy source: answer_pending, review, probe, practice, assess, complete.
- Use the existing SpacedRepetitionScheduler. Preserve all RepetitionState data: interval_index, consecutive_correct, consecutive_wrong, next_review_at, difficulty, stability, retrievability, desired_retention, review_count, lapse_count, and last_review_at.
- The existing scheduler is not calibrated FSRS. Do not call it FSRS and do not create another scheduler.

## 7. PostgreSQL mapping (DATABASE_V5 is authoritative)

The following is a semantic mapping, not a replacement schema. Use the table and column definitions, types, constraints, indexes and table inventory in .sdd/database/DATABASE_V5.md. Do not repeat full DDL in this plan.

| DeepTutor / platform meaning | DATABASE_V5 target | Notes |
| --- | --- | --- |
| LearningProgress.book_id and aggregate identity | mastery_paths.path_id | UUID stored as string at DeepTutor boundary; state_json is authoritative serialized LearningProgress; keep revision, timestamps and ownership fields. |
| Authenticated path owner | mastery_paths.user_id | Required owner field. learning_goal_id is optional logical reference. Authorization always checks this row field. |
| Module/KP snapshot and adaptive state | mastery_paths.state_json | Includes DeepTutor module/KP snapshot, knowledge types, attempts, mastery, qualitative state, evidence, errors, repetition, review queue and other model fields. Content Service remains canonical. |
| Tutor session membership | mastery_path_sessions | Path/session link only. It has no user_id; do not use it for ownership. |
| Pending question lifecycle | mastery_interactions | Preserve status, question JSON, session/turn references, learner answer and result JSON. Expected answer remains server-side and is never exposed through learner APIs. |
| DeepTutor path event log | mastery_events | Internal aggregate events, not integration delivery. Preserve aggregate revision and payload. |
| LearningEvidence query projection | mastery_learning_evidence | Projection/index only; it is not the mastery authority. Preserve ordinal, KP, occurred_at, source, source_reference_id, assessment type/result/quality, attempts and context fields, plus evidence_json. |
| DeepTutor RepetitionState / review queue | mastery_paths.state_json | Preserve the full scheduler state listed in Section 6. Do not reduce it to interval alone. |
| Single mutating tutor turn lease | mastery_path_leases | Keep separate from row lock and CAS. |
| Session/message/turn runtime | sessions, messages, turns, turn_events | Schema is prepared in DATABASE_V5. Phase 1 does not port SessionStore absent a required Phase 1 API. If later ported, preserve session title, compressed summary and summary boundary, preferences and timestamps; message parent branch, capability, events, metadata and attachments; turn status, owner_id, fencing_token, state_version, failure_code, retryable, assistant_message_id and timestamps; and turn-event seq, type, source, stage, content, metadata and timestamp with UNIQUE(turn_id, seq). |
| Question notebook and per-question practice review | notebook_entries, practice_review_state, practice_review_events | Defer PracticeStore in Phase 1 unless an accepted core use case requires it. If ported later, preserve notebook question/type/options/correct answer/explanation/difficulty snapshots; user answer, attempt count, hints, confidence, response time, quality, result/resolution/bookmark state; practice review timing/ease/streak/lapses/version and append-only review event fields. Follow DATABASE_V5 and DeepTutor source; do not replace these with a minimal question/KP/result row. |
| Integration event publication from AI Learning | outbox_events | Use only for an explicitly required outbound integration event, inserted in the same transaction. Do not publish every mastery_events record. |
| DeepTutor topic metadata/source catalog | No DATABASE_V5 table | Content Service owns canonical curriculum. Topic-source product functionality is out of scope; any required in-scope LearningStore behavior is TO VERIFY. |

Refer to DATABASE_V5 for the authoritative service table inventory. Do not hard-code a total table count in this plan.

## 8. Assessment event, identity, and idempotency contracts

### 8.1 Assessment event contract

AssessmentCompleted.v2 is referenced by the architecture/database design, but no tracked schema or implemented event producer was found in this repository. Before consumer implementation, publish and agree a language-neutral event contract. Mark this work **NEW CONTRACT REQUIRED**.

Target envelope:

- `event_id`, `event_type = AssessmentCompleted.v2`, `occurred_at`, `source`.
- `data`: `user_id`, `learning_goal_id` when appropriate, `attempt_id`, `result_id`, monotonic `result_version`, `assessment_type`, finalization/status marker, `completed_at`, `item_results[]`.
- Each item: `item_result_id`, `question_version_id`, explicit correctness/result, `score`/`max_score` when applicable, explicit per-KP `qualitative_judgment` when applicable, and `knowledge_point_mappings[]`.
- Each mapping: canonical `knowledge_point_id` and `weight`.

Assessment Service remains formal grading authority; AI Learning consumes finalized and regraded outcomes. Do not include `correct_answer` unless separately required by an accepted contract. Never include hidden examiner reasoning or raw model chain-of-thought. The schema and producer are **NEW CONTRACT REQUIRED**, not an existing contract.

### 8.2 Ownership of question-to-KP mappings

Content Service owns `question_knowledge_points`. AI Learning must not query content_db or issue one HTTP lookup per item after receiving an event. Assessment Service snapshots the mapping used for the attempt/result and includes it in the event. Each mapping contains canonical `knowledge_point_id` and `weight`. Identity is Content KP UUID = event KP UUID = DeepTutor `KnowledgePoint.id` (string form); `KP.code` is never identity. Mapping snapshot is a final Phase 1 decision and part of the NEW CONTRACT REQUIRED.

Phase 1 preserves mapping weight in the event and evidence metadata/audit where useful. Weight is attribution metadata, not a pedagogy input: it does not change mastery calculation, `LearningEvidence.quality`, scheduler behavior, or KP selection. Each mapped KP may receive its own evidence.

### 8.3 Idempotency and result versions

Use deterministic UUIDv5 for each formal evidence identity, derived from canonical `(result_id, result_version, item_result_id, knowledge_point_id)`. Store it as `source_reference_id`; preserve original identifiers in evidence metadata. Define the UUID namespace and canonical serialization consistently during implementation.

The database must enforce `UNIQUE(path_id, source, source_reference_id)` or an exact equivalent. SELECT-before-INSERT is insufficient under concurrent deliveries. DATABASE_V5 currently documents only a non-unique index, so this is **SCHEMA CHANGE REQUIRED**; see Required Follow-up Changes.

`result_version` increases monotonically per `result_id`. Under the path aggregate lock/transaction, compare it with the latest version recorded in authoritative DeepTutor evidence metadata: an already applied version is ignored, an older late-arriving version is ignored, and a newer regrade is accepted. The latest-version lookup/update, evidence dedupe, DeepTutor `LearningProgress` mutation, `state_json`, revision increment, evidence projection synchronization and internal `mastery_events` commit together. Superseded formal evidence must not remain double-counted. The approved Phase 1 correction policy is to rebuild affected KP derived adaptive state from authoritative evidence history: supersede old formal evidence, replay relevant DeepTutor evidence/attempt history, then recompute mastery and repetition state. Exact replay mechanism and efficient version-history lookup are TO VERIFY during implementation; the no-double-count business rule is final.

## 9. Security contract

The learner request enters through API Gateway. Gateway validates the external user token and forwards an internal HMAC-SHA256 JWT. AI Learning is a downstream service and validates the internal token, not a guessed external-token/JWKS contract.

Verified internal-token behavior from Gateway/common-security:

- signing algorithm: HS256;
- configured issuer contract, currently urn:code-base:api-gateway by default;
- sub must parse as a UUID;
- expiration and canonical roles are required;
- the internal secret is separate from the external signing secret and is supplied through environment/configuration only. Never put secret values in this plan, code, examples, logs, or commits.

The Python verifier must enforce the same issuer/subject/expiry/role contract and accepted signing key configuration. Verify how the Python service receives the internal signing secret and canonical roles during deployment (TO VERIFY); do not trust X-User-* headers or accept identity supplied by a request body. Return the API's chosen 403 or 404 convention for a non-owned path after checking mastery_paths.user_id.

## 10. API baseline and path selection

### POST /api/ai-learning/paths

Authenticated learner path bootstrap. User identity comes from JWT. Fetch the active goal and curriculum, validate all KPs, and call `ensurePath(userId, learningGoalId)`. Return the existing path or create exactly once, including under concurrent API/event requests. Do not accept caller-supplied `user_id` or silently create an empty path. Exact response fields and create-vs-existing HTTP status follow the new service's API convention (TO VERIFY before implementation).

### GET /api/ai-learning/progress

Return a safe progress summary for the active goal's path using DeepTutor's map/status semantics. Resolve the active goal from User Service; if none exists, return a domain error. No internal aggregate serialization.

### GET /api/ai-learning/status

Return the safe result of DeepTutor policy.next_objective() for the active goal's path plus only approved display fields. No active goal is a domain error. Preserve the real action enum/string and policy precedence.

### GET /api/ai-learning/paths/{pathId}/map

Return a response DTO mapped from DeepTutor map summary and module/KP information. Resolve the path only where mastery_paths.user_id equals the authenticated UUID.

Final rule: one active learning goal drives one active mastery path. Historical/inactive goals do not drive default `/progress` or `/status`; these endpoints never choose a random path. User Service must define/enforce exactly one active goal (**USER CONTRACT REQUIRED**). With no active goal, return a domain error and never create a fake/default goal. Historical path selection is future scope.

Do not expose raw LearningProgress, RepetitionState, pending expected answers, internal mastery_events, or private reasoning.

## 11. Test and acceptance scenarios for the later implementation

Tests must verify the behavior below using the pinned v1.6.9 functions and PostgreSQL adapter. These are implementation acceptance criteria; this task changes only this plan.

### DeepTutor pipeline and policy

- compute_mastery([True]) is 0.5; compute_mastery([True, False, True]) is approximately 0.661, not above 0.7.
- Exercise is_assessed_mastered() / actual policy with recent sequences below and at/above the quantitative 0.9 gate. Do not use lifetime raw percentages as a proxy for the DeepTutor score.
- One correct attempt leaves a quantitative KP at 0.5; the next action remains on that KP (normally practice after evidence exists), not the next KP.
- Qualitative quiz accuracy alone does not pass CONCEPT/DESIGN; only valid qualitative DeepTutor state can pass.
- An overall Writing/Speaking score never updates qualitative mastery; explicit per-KP qualitative PASS may update through DeepTutor. With no explicit judgment, qualitative mastery is unchanged.
- Mapping weight is retained as attribution metadata and never changes mastery, evidence quality, scheduler behavior, or KP selection.
- Verify real next_objective() precedence and actions: pending question → answer_pending; due review → review; new unmastered KP → probe; quantitative KP with evidence below gate → practice; qualitative KP without pass → assess; all mastered/no due work → complete.
- Validate that formal assessment is processed once through the single ingestion pipeline and that the adapter itself performs no mastery, scheduler, or policy mutation.

### Persistence and event delivery

- Round-trip the full LearningProgress JSON through the synchronous adapter, including qualitative state, attempts, errors, evidence, review queue, every RepetitionState field and revision.
- Verify atomic state/projection/event commit, single revision increment, stale revision failure, lock/CAS behavior, and separate path-lease behavior against PostgreSQL.
- `POST /paths` and an Assessment event racing for the same `(user_id, learning_goal_id)` use exactly one path.
- Concurrent duplicate RabbitMQ deliveries create one evidence effect through UUIDv5 identity and database uniqueness.
- A newer result version is accepted; superseded older evidence is not double-counted after regrade/replay.
- Replaying a delivery after transaction failure or process restart produces no lost or double mutation; acknowledge only after commit.
- RabbitMQ ACK happens only after PostgreSQL commit; transient failures follow retry policy and permanent contract failures use DLQ.
- Verify mastery_learning_evidence is rebuilt/synchronized from the mutated aggregate and never treated as an independent mastery writer.

### Curriculum, security, and API

- Canonical Content KP UUID string equals the corresponding Assessment mapping and DeepTutor KnowledgePoint.id; KP.code does not replace identity.
- Missing/invalid learning_type rejects bootstrap before state mutation.
- User or Content timeout fails path bootstrap with a retryable dependency error; no empty/goal-less path is persisted.
- Valid/invalid/expired internal JWT, invalid UUID subject, unsupported role, and missing token produce the established authentication behavior.
- A learner cannot read another user's path even with its pathId; request identity headers/body do not override JWT identity.
- Responses expose only the agreed platform DTO fields, not aggregate internals or expected answers.

## 12. Phase 1 scope and file outline

### In scope

1. Existing service scaffold and reproducible DeepTutor submodule installation.
2. Synchronous PostgreSQL LearningStore for the Mastery Path aggregate and needed projections.
3. Internal JWT validation and path ownership.
4. Required User Service and Content Service clients plus explicit curriculum mapping.
5. Minimal DeepTutor service-boundary extension for external pre-graded assessment, without changing core mastery, policy, gates, scheduler algorithm, or KnowledgeType semantics.
6. Accepted self-contained Assessment event contract, mapping snapshot, transactional outbox and RabbitMQ consumer with versioned UUIDv5 idempotency.
7. Path bootstrap, progress/status/map API DTOs.
8. Focused unit, PostgreSQL integration, security, and end-to-end coverage for the flows above.

### Explicitly deferred

- Custom mastery/policy/scheduler/planner implementations.
- Full DeepTutor chat/session runtime persistence, Tutor Chat APIs/UI, SessionStore port, and turn streaming.
- PracticeStore, notebook APIs, question-bank practice workflows, and question review endpoints unless an approved Phase 1 API requires them.
- DeepTutor RAG, memory, video, visual/scientific and unrelated product capabilities.
- Exact RabbitMQ exchange/routing names, retry counts/timeouts, deployment values, and Python client library choice until implementation.
- Curriculum cache/TTL, speculative circuit-breaker infrastructure, and any cross-service database access.

### Target ownership layout (implementation guide only)

services/ai-learning-service/
- app/api/ — FastAPI routes and response DTO mapping
- app/application/ — path bootstrap and formal assessment orchestration
- app/integrations/ — User/Content clients and contracted event consumer
- app/adapters/ — curriculum and formal evidence transformations
- app/persistence/ — synchronous PostgreSQL LearningStore
- app/security/ — internal JWT verification
- tests/
- requirements.txt — existing dependency convention

third_party/deeptutor/ — pinned submodule; modify only for approved minimal extension

Do not create application modules solely to wrap DeepTutor mastery, policy, or scheduler calls.

## 13. Delivery risks and unresolved decisions

- **DeepTutor external-result API:** v1.6.9 has no public external pre-graded full-pipeline method. The minimal fork extension signature and replay implementation must be approved before formal mutation.
- **Path ownership during creation:** `get_or_create(book_id)` has no owner/goal parameters. `ensurePath` must preserve DeepTutor's service boundary while persisting owner and goal atomically; exact extension point remains TO VERIFY.
- **Assessment event contract:** `AssessmentCompleted.v2` schema, producer, result-version lifecycle and mapping snapshot must be accepted before consumer implementation (NEW CONTRACT REQUIRED). RabbitMQ transport is decided; names/config remain TO DEFINE.
- **Database constraints:** `mastery_paths` has no unique `(user_id, learning_goal_id)` constraint, and `mastery_learning_evidence` documents a non-unique source index. Both constraints must be approved/designed before concurrent path creation or formal consumer implementation.
- **Active-goal contract:** User Service must expose/enforce exactly one active goal per learner (USER CONTRACT REQUIRED); no active goal means a domain error.
- **Service HTTP contracts:** verify actual User/Content client routes, DTOs, service authentication, and retryable error mapping before implementing either client.
- **Blocking database calls:** use the synchronous adapter and FastAPI threadpool boundary; capacity tuning is deployment work, not a reason to change DeepTutor to async.
- **Database migration execution:** DATABASE_V5 is a design source, not an implemented Python migration. Select a reproducible schema rollout process before deployment (TO VERIFY); do not duplicate or silently diverge from DATABASE_V5.

## 14. Architecture / Schema Contradictions

DATABASE_V5 is authoritative and remains unchanged in this task. The required follow-ups are listed explicitly at the end of this plan.

1. **DeepTutor evidence is aggregate state; SQL evidence is a projection:** `mastery_paths.state_json` is authoritative and `mastery_learning_evidence` is a query projection. The consumer updates both atomically; it never writes only the projection.
2. **Session/Practice tables are outside Phase 1's required flow:** keep their schema intact and defer runtime adapters absent an accepted API need.
3. **DeepTutor topic metadata storage is absent from DATABASE_V5:** Content Service remains canonical. Do not add or depend on `mastery_topic_meta` or `mastery_topic_sources` for Phase 1.

## 15. Phase 1 definition of done

- [ ] The team's minimal DeepTutor fork remains based on upstream v1.6.9 and is installed from the pinned submodule reproducibly.
- [ ] The synchronous PostgreSQL adapter preserves the actual DeepTutor LearningStore transaction semantics and DATABASE_V5 mapping.
- [ ] One active goal drives one path; User Service's exactly-one-active-goal contract is accepted.
- [ ] Path uniqueness and evidence idempotency database constraints are approved and applied before concurrent bootstrap/consumer work.
- [ ] `AssessmentCompleted.v2`, Question→KP snapshots, transactional outbox and regrade semantics are accepted before RabbitMQ consumer/formal mutation work.
- [ ] Curriculum bootstrap uses canonical UUID identity and rejects invalid required curriculum metadata.
- [ ] Formal evidence dispatches by KnowledgeType and mutates mastery/review only through DeepTutor's service boundary.
- [ ] Required learner APIs use JWT identity, enforce mastery_paths.user_id, and return safe response DTOs.
- [ ] Assessment event duplicates and regrades follow the agreed version-aware idempotency behavior; commit precedes broker acknowledgement.
- [ ] Focused unit, PostgreSQL integration, security, and end-to-end checks cover the acceptance scenarios in Section 11.
- [ ] No raw SQL is duplicated here as an alternate schema; DATABASE_V5 remains authoritative.

## 16. TO VERIFY / NEW CONTRACT REQUIRED

- **NEW CONTRACT REQUIRED:** accepted `AssessmentCompleted.v2` schema: finalized status, monotonic `result_version`, item outcomes, explicit qualitative judgment when applicable, and Question→KP mapping+weight snapshot.
- **USER CONTRACT REQUIRED:** User Service route/DTO for active goal and invariant of exactly one active goal; no active goal is an error case.
- **TO VERIFY:** exact User Service and Content Service routes/DTOs; internal HTTP service authentication.
- **TO VERIFY:** DeepTutor `record_external_assessment(...)` signature and ownership-aware path-creation seam; exact regrade replay implementation. Core behavior is fixed: superseded result versions must not double-count.
- **TO DEFINE:** RabbitMQ exchange/routing names, retry count/timeout and DLQ bindings, Python client library if no repo convention exists, and deployment configuration. RabbitMQ itself is final, not an open choice.
- **TO VERIFY:** schema migration tooling/rollout for the Python service and the approved design/application of both required unique constraints.
- Whether future PracticeStore or SessionStore operations are necessary for an explicitly accepted Phase 1 use case.

## Required Follow-up Changes

### DATABASE_V5

Do not edit DATABASE_V5 in this planning task. Its current definitions show two required schema changes:

- Add database uniqueness for one non-null goal-bound path per `(user_id, learning_goal_id)` (for example `UNIQUE (user_id, learning_goal_id) WHERE learning_goal_id IS NOT NULL`). `user_id` is required while `learning_goal_id` is nullable/optional, so retain optional semantics for non-goal paths. This constraint makes concurrent `ensurePath` converge on exactly one row.
- Replace the non-unique `(path_id, source, source_reference_id)` index on `mastery_learning_evidence` with a unique constraint/index for formal evidence idempotency. `source_reference_id` is a UUID and formal references are populated with deterministic UUIDv5; database enforcement prevents duplicate effects from concurrent RabbitMQ deliveries.

Both require schema-owner approval/design and an approved migration/rollout before implementation of those flows.

### SERVICE_ARCHITECTURE_V2

Do not edit SERVICE_ARCHITECTURE_V2 in this task. Update its generic assessment integration flow to specify **Assessment Service transactional outbox → RabbitMQ → AI Learning consumer**. The outbox row commits with the formal result; a publisher sends it after commit. Keep Assessment `outbox_events` separate from AI Learning's internal DeepTutor `mastery_events`.

### Assessment Service

**NEW CONTRACT REQUIRED:** publish and accept `AssessmentCompleted.v2` with monotonic `result_version`, finalized result marker, and per-item snapshot of `question_version_id → knowledge_point_id + weight`. Include per-KP qualitative judgment only when explicitly assessed. Assessment Service remains the formal grading authority.

### Implementation gate

Implementation MUST NOT start on the RabbitMQ consumer or formal evidence mutation until:

1. `AssessmentCompleted.v2` is accepted.
2. The evidence idempotency constraint is approved.
3. The DeepTutor external-assessment extension contract is approved.

---

**Invariant:** DeepTutor remains the sole adaptive learning engine. IELTSPath provides platform integration, PostgreSQL persistence, security, formal-assessment adaptation, and RabbitMQ integration, without implementing a second mastery, policy, scheduler, or planner.
