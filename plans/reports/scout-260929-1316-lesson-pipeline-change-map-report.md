---
title: "Scout — bản đồ thay đổi cho pipeline học chính (topic → bài → đề cuối)"
date: 2026-09-29 13:16
input: artifact "Pipeline học IELTSPath" (claude.ai/artifact/T5gAgf4BRHWqG9CyqZcrZH), plans/260928-2019-architecture-doc-service-split/main-learning-pipeline.md
branch: feat/main-follow
status: đầu vào cho plan triển khai (chưa sửa code)
---

# Scout — bản đồ thay đổi cho pipeline học chính

## Tóm tắt

- Artifact mô tả đúng hướng đi, nhưng đối chiếu với code thì còn **4 chỗ chặn** mà thiết kế chưa giải. Cần chốt 4 chỗ này trước khi lập plan:
  1. Consumer RabbitMQ của ai-learning không có token nên không gọi được content.
  2. Chưa có contract `answer_spec`: seed dùng `{"correct":"A"}` nhưng bộ chấm của game không đọc khóa `correct`.
  3. Path, pending result và tutor đều đang gắn theo goal.
  4. Chưa có đường ghi (API) để publish câu hỏi hay gắn câu vào section; đề TOPIC_TEST chỉ tạo được bằng seed.
- Có **6 lỗ hổng bảo mật sẵn có** nằm ngay trên đường của pipeline. Nên sửa trước hoặc sửa cùng lúc (xem §2).
- Quy mô: 5 service, 3 migration mới (content V7, assessment V5, ai-learning V10), 4 thay đổi contract cần duyệt.

## 1. Chỗ chặn và quyết định cần chốt trước plan

| # | Vấn đề | Bằng chứng | Đề xuất |
| --- | --- | --- | --- |
| B1 | Bước 10: consumer cần hỏi content "package này là đề cuối của topic nào", nhưng consumer không có JWT học viên và chưa có cơ chế service token | `app/messaging/assessment_consumer.py:116-122` (PathService không có client); `app/security/internal_jwt.py` chỉ kiểm token, không ký; `ConsumerSettings` `app/config.py:104-134` không có URL service | Không gọi HTTP từ consumer. Có hai cách, (a) nên chọn hơn: **(a)** assessment đã lấy đề từ content lúc tạo attempt, nên ghi luôn `package_id` và `topic_id` (hoặc `package_type`) vào snapshot rồi đưa vào event. **(b)** ai-learning lưu `test_package_id` vào `topic_progress` khi mở topic (bước 4 đã nhận `testPackageId`) rồi tra cục bộ. Lưu ý: event mang `package_version_id`, còn `topics.test_package_id` trỏ tới **package**, nên cần thêm `package_id` |
| B2 | Chưa có contract `answer_spec`. Ba nơi sẽ chấm: game (có sẵn), assessment (mới), ai-learning (mới) | Seed `content V4:85-86` `{"correct":"A"}`; `game-service/.../JacksonGameAnswerEvaluator.java:12-47` chỉ đọc `answer`, `correctAnswer`, `correctOptionId`, `optionId`, `expected` | Viết `docs/contracts/answer-spec-v1.md`: dạng spec và payload trả lời cho MC, TFNG, FILL/SHORT (danh sách đáp án chấp nhận) và MATCHING, kèm bộ test vector JSON dùng chung cho cả ba bộ test. Game hoặc sửa để đọc thêm khóa `correct`, hoặc đổi seed |
| B3 | Path gắn theo goal ở mọi tầng | `V1__one_mastery_path_per_learning_goal.sql:16-18`; `path_service.py:56-74` (`ActiveGoalRequired`→409), `:111-137` (lọc band + `PathOrderer` gọi LLM); `V3` pending `learning_goal_id NOT NULL`; tutor `app/api/tutor.py:121` | Migration V10: path duy nhất theo user. Tách đường tạo path mới (không qua goal, band, LLM). Cần chốt: path cũ theo goal xử lý ra sao; tutor, `/progress`, `/status` có chuyển sang path theo user không; bỏ pending hay đổi khóa sang user |
| B4 | Không có API publish câu hỏi hay gắn câu vào section | `Question.publishVersion` (`domain/aggregate/Question.java:43`) không ai gọi; `ContentSection.addQuestion` (`domain/entity/ContentSection.java:46`) không ai gọi | Chốt phạm vi MVP: chỉ dùng seed (V7) cho bài học và TOPIC_TEST, hay làm luôn authoring. Nếu chỉ seed thì ghi rõ trong plan |

**Đã chốt (2026-09-29):** lúc tạo path không dùng hồ sơ học viên. Không lấy goal (band mục tiêu, phút/ngày, ngày thi), không lấy kết quả placement, không gọi LLM, không lọc band. Path ban đầu = mọi topic đã publish theo `sort_order` của content, giống nhau cho mọi học viên. Chỉ cá nhân hóa sau khi có kết quả (xếp lại topic chưa học + chèn bài ôn, tính bằng luật, không dùng LLM). Code bị ảnh hưởng: đường tạo path bài học không gọi `PathOrderer.order` (`app/application/path_orderer.py:64-104`), `LearnerContext`/`_learner_profile` (`:40-53`), `ordering_llm.py`, `CurriculumScope.select` (`app/adapters/curriculum_scope.py:72-107`), `pending_formal_payloads` cho placement. Còn phải chốt: xóa hẳn các phần này hay giữ lại cho tutor/`/status` cũ (xem B3).

**Đã chốt (2026-09-29): đánh giá lại path ở 3 lúc.** (1) học xong một bài: mọi khối bài tập đều có một lần nộp ≥ 70%, hoặc bấm hoàn thành với bài không có bài tập; (2) có kết quả đề cuối topic (TOPIC_GATE), chạy dù đạt hay trượt; (3) có kết quả MOCK hoặc OFFICIAL_PRACTICE. Nộp từng khối bài tập chỉ cập nhật mastery, path không đổi. Placement không kích hoạt.
- Mỗi lần đánh giá lại làm hai việc theo luật (không dùng LLM): chèn bài ôn (KP < ngưỡng + có câu sai trong kết quả vừa xét + bài dạy KP đó đã xong và không phải bài vừa xong) và xếp lại topic chưa học trong cùng topic cha (yếu → chưa có dữ liệu → vững).
- Chia vai: thuật toán DeepTutor (`compute_mastery` qua `record_external_quiz_outcome`) chạy ở **mọi lần nộp** để cập nhật số mastery; luật đánh giá lại là code mới (ví dụ `app/lessons/path_reevaluation.py`), chỉ đọc số mastery. Không dùng `grade_answer` hay `review_queue` của DeepTutor cho luồng này; không sửa `app/mastery`.
- Code hiện tại không có cơ chế này: thứ tự chỉ đặt lúc tạo path, và chỉ đổi khi học viên nhờ tutor (`path_reorder`, `app/tutor/tools.py:114`). Dùng lại `reorder_path` (`app/application/path_reorder.py:44`) và `OrderingValidator.apply` để ghi thứ tự mới dưới khóa path.
- Gọi từ 3 chỗ: endpoint nộp bài tập (khi bài vừa xong), consumer (TOPIC_GATE), consumer (MOCK/OFFICIAL_PRACTICE). Hàm phải idempotent, vì consumer có thể nhận lại event.

**Đã chốt (2026-09-29): làm lại một khối bài tập = nộp lại cả khối.** Học viên trả lời mọi câu của khối rồi nộp một lần `{requestId, answers}`; server chấm cả khối, trả ngay đúng/sai kèm giải thích từng câu. Chưa đạt 70% thì trả lời lại cả khối và nộp với `requestId` mới, không giới hạn số lần. Không có kiểu "câu sai xếp về cuối" (nộp từng câu), cũng không có kiểu chỉ nộp câu sai. Mọi lần nộp đều ghi bằng chứng mastery. Chấp nhận đánh đổi: lần nộp lại ngay sau khi đã xem đáp án có thể làm mastery cao hơn thực tế.

**Đã chốt (2026-09-29, brainstorm `brainstorm-260929-1611-lesson-feedback-review-question-pool-report.md`):**
- **Phản hồi:** chỉ báo đúng/sai; lời giải khi khối đạt. Nộp lại khối đã đạt thì không ghi mastery.
- **Bài ôn:** lý thuyết của bài dạy KP + một gói `PRACTICE_SET` chưa làm có câu đo KP yếu. Trượt thì giao gói khác. Không còn "làm lại bài tập cũ".
- **Đề cuối:** nhiều mã đề mỗi topic (`content_packages.topic_id`). ai-learning giao mã đề và ghi `topic_test_assignments`; nhờ đó consumer tra topic cục bộ. **B1 được giải theo cách này.**

**Đã chốt (2026-09-29, sau red-team):** bỏ điều kiện "bài dạy KP không phải bài vừa xong" → **luyện thêm theo mastery**: bài vừa xong mà KP dưới ngưỡng và có câu sai thì giao ngay gói câu mới cùng KP trước khi mở bài kế. Bỏ band của KP (content, API, ai-learning). Chi tiết ở plan `260929-1640-lesson-learning-pipeline-mvp`.

Quyết định phụ (không chặn nhưng phải ghi vào plan):
- Mastery: `compute_mastery` giới hạn 0.5 sau 1 lần làm, 0.8 sau 2 lần (`app/mastery/mastery.py:23`). Cổng policy là 0.9 (`app/mastery/policy.py:37-40`). **Ngưỡng bài ôn 0.6** là hằng số mới, riêng cho luồng bài học; không sửa engine (AGENTS §3.8).
- Bằng chứng của bài tập cần `source` riêng và `source_reference_id` tất định. Nếu không, sẽ đụng unique `(path_id, source, source_reference_id)` của `mastery_learning_evidence` (`V2:3-31`). Seam nên dùng: `ExternalAssessmentLearningService.record_external_quiz_outcome` (`app/learning/external_assessment.py:30-84`).
- `state_json` không lưu topic cha (`app/adapters/curriculum_adapter.py:23-81`), nên "xếp lại trong cùng topic cha" cần lưu `parent_topic_id` và `sort_order`, ví dụ trong `topic_progress` hoặc trong module.
- `attemptType`: enum không có `PRACTICE`, chỉ có `OFFICIAL_PRACTICE` (`assessment domain/vo/AttemptType.java:3`, DB CHECK `V1:7`). Artifact nên dùng đúng tên này.
- Feature key cho topic premium: dùng `PREMIUM_CONTENT` có sẵn (`access V1:161-164`), hay thêm key mới. Học viên miễn phí luôn nhận `enabledFeatures` rỗng.
- Body lỗi của ai-learning hiện chỉ có `{"detail"}` (`main.py:59-105`). Cần chốt dạng `{detail, code}` cho `REVIEW_REQUIRED`, `LESSON_LOCKED`, `PREMIUM_REQUIRED`.

## 2. Bảo mật sẵn có trên đường của pipeline

| Mức | Lỗ | Vị trí | Hướng sửa |
| --- | --- | --- | --- |
| P0 | Học viên bất kỳ gọi được `points/debit`, `points/refund`, `consume-human-grading` cho **user bất kỳ** | `access/.../InternalAccessController.java:22,36,48,60` (không `@PreAuthorize`, không kiểm chủ sở hữu); Gateway route `/api/access/**` | Đổi sang `/internal/access` và thêm kiểm chủ sở hữu hoặc ADMIN |
| P0 | Đọc entitlement của user khác | cùng file `:31-34` | Như trên. Thêm `GET /api/access/me/entitlement` vào `LearnerAccessController.java:23` |
| P0 | `GET /api/content/questions/{id}` trả `answerSpecJson` và `explanation` cho CUSTOMER | `content/.../QuestionController.java:42`, `QuestionVersionResponse.java:18-19` | Giới hạn cho ADMIN/CONTENT_AUTHOR, hoặc bỏ đáp án khỏi response |
| P0 | Structure của attempt trả `answerSnapshot` cho học viên | `assessment/.../GetAttemptStructureUseCase.java:3`, `AttemptStructureResponse.java:3` | Bỏ trường này (đổi contract response) |
| P1 | Controller Question, ContentPackage, ContentAsset, KnowledgePoint không có `@PreAuthorize`: ai đăng nhập cũng tạo được nội dung | `content/.../KnowledgePointController.java:19`, `ContentPackageController.java`, … | Thêm `hasAnyRole('ADMIN','CONTENT_AUTHOR')` cho các thao tác ghi |
| P1 | Học viên tự mở được result DRAFT, sẽ đụng với v1 do tự chấm tạo ra | `assessment/.../AssessmentResultController.java:29-38` | Bỏ endpoint này, hoặc chỉ cho EXAMINER/ADMIN |
| P2 | `/internal/**` nhận mọi internal JWT, không kiểm role | `common-security/.../CommonSecurityAutoConfiguration.java:54-68` | Chốt: giữ nguyên, hay thêm claim hoặc role cho service. Gateway không route `/internal/**` (`api-gateway.yaml:9-93`) |

## 3. Thay đổi theo service

### content-service (migration kế tiếp: **V7**)

| Việc | File | Loại |
| --- | --- | --- |
| Bảng `lessons`, `lesson_blocks`, `lesson_block_vocabulary`, `lesson_block_questions`, `lesson_knowledge_points`; thêm `topics.required_feature_key`, `topics.test_package_id`; thêm `TOPIC_TEST` vào CHECK `package_type` (constraint inline không tên, dùng `DROP CONSTRAINT IF EXISTS content_packages_package_type_check`) | `db/migration/V7__...sql` | MỚI |
| Topic: thêm 2 trường vào constructor, `create`, `update` | `domain/aggregate/Topic.java:10-70`, `TopicJpaEntity.java`, `TopicResponse` | SỬA |
| `PackageType` thêm `TOPIC_TEST` | `domain/vo/PackageType.java:3-9` | SỬA |
| Aggregate Lesson, repository, adapter, JPA, mapper theo layout của service (`domain/repository`, không dùng `application/port`) | `domain/aggregate/Lesson.java`, `domain/repository/LessonRepository.java`, `infrastructure/persistence/...` | MỚI |
| `/internal/learning-content/*`: danh sách bài của topic, chi tiết bài (kèm answerSpec và KP), `lessons/by-knowledge-points`. Mẫu tham khảo: `GetGameContentSnapshotUseCase.java:73-97` | `api/controller/InternalLearningContentController.java` + use case | MỚI |
| `/internal/assessment-content/package-versions/{id}` (section, item, answerSpec, maxScore, và topic hoặc package id theo B1). Query theo `versionId` có entity graph; nạp question version và KP theo lô (`findByQuestionVersionIds`). Tránh N+1 kiểu `findDistinctByVersions_Id` | `InternalAssessmentContentController.java:14` + use case | MỚI |
| Seed bài học và TOPIC_TEST cho DEMO_READING (id `10000000-…-01`). Sửa luôn chỗ lệch: câu hỏi V4 nói về "urban trees" còn passage V6 nói về green roofs | `V8__seed_...sql` (hoặc gộp vào V7) | MỚI |
| Thêm `LessonNotFoundException` vào danh sách 404 | `api/exception/GlobalExceptionHandler.java:25-34` | SỬA |
| Test: Testcontainers theo mẫu `DemoReadingPassageSeedTest`; controller test dùng `standaloneSetup` (không kiểm được `@PreAuthorize`, nên cần `@WebMvcTest` cho phần security) | `src/test/...` | MỚI |

### assessment-service (migration kế tiếp: **V5**)

| Việc | File | Loại |
| --- | --- | --- |
| Request tạo attempt: bỏ `sections`; lấy đề qua port mới `ContentPackageProvider` và `ContentPackageClient`, làm theo mẫu `ContentKnowledgeMappingClient` | `api/dto/request/StartAssessmentAttemptRequest.java:3-5`, `StartAssessmentAttemptUseCase.java:32-59`, `application/port/` | SỬA + MỚI |
| Lưu answerSpec và maxScore cho từng item (`attempt_items` chưa có cột này, V1:30-39). Thêm cột score/maxScore vào `assessment_results`, hoặc tính lúc đọc | `V5__...sql` | MỚI |
| Structure bỏ `answerSnapshot` | `GetAttemptStructureUseCase`, `AttemptStructureResult`, `AttemptStructureResponse` | SỬA |
| Submit tự chấm: `AutoGradeAttemptService` mới; câu không trả lời được 0 điểm; `feedback_snapshot` là NOT NULL nên ghi `'{}'` | `SubmitAssessmentAttemptUseCase.java:9` | SỬA + MỚI |
| Tách phần "complete + ghi outbox" ra helper dùng chung | `FinalizeAssessmentResultUseCase.java:88-104` | SỬA |
| Bỏ hai chỗ chặn event khi không có goal | `FinalizeAssessmentResultUseCase.java:90-94`, `AssessmentCompletedEventFactory.java:29` | SỬA |
| Event thêm `package_version_id` (+ `package_id` hoặc `topic_id` theo B1) | `application/event/AssessmentCompletedV2.java:24-36`, `AssessmentCompletedEventFactory.java:23-67` | SỬA |
| Result thêm `score`, `maxScore`, `percent` | `AssessmentResultResponse.java:3`, `AssessmentResultResult.java:6-7`, `GetAssessmentResultUseCase.java:3` (hiện trả cả bản DRAFT) | SỬA |
| Bug sẵn có: submit khi đã hết hạn thì `expire()` bị rollback | `domain/aggregate/AssessmentAttempt.java:68-89` | SỬA (tùy chọn) |
| Test phải đổi: `StartAssessmentAttemptUseCaseTest`, `FinalizeAssessmentResultUseCaseTest:168` (test "không có goal" sẽ đảo kết quả), `AssessmentOutboxIntegrationTest` | `src/test/...` | SỬA |

Giữ nguyên: lưu câu trả lời `PUT .../response` (`SaveAttemptResponseUseCase.java:36-73`), outbox relay và topology RabbitMQ.

### ai-learning-service (migration kế tiếp: **V10**)

| Việc | File | Loại |
| --- | --- | --- |
| Path unique theo user; bảng `topic_progress`, `lesson_progress`, `lesson_exercise_submissions`, `path_review_items`; pending đổi khóa hoặc bỏ | `migrations/V10__...sql` (+ README :187-202, `tests/test_migrations_postgres.py`, DATABASE_V5) | MỚI |
| Store: `find_path_by_user`, advisory lock chỉ theo user | `app/persistence/postgres_learning_store.py:140-228, 315-321, 447-456` | SỬA |
| Đường tạo path theo `sort_order`: bỏ qua `_active_goal`, `CurriculumScope.select` và `PathOrderer`. Dùng lại `replace_modules_for_path` (`app/mastery/service.py:732`) và `curriculum_refresh.merge_curriculum` | `app/application/path_service.py` hoặc service mới | SỬA + MỚI |
| Router học viên `/topics`, `/topics/{id}/lessons`, `/lessons/{id}`, `/lessons/{id}/exercises/{blockId}/submissions`; register trong `main.py:55-56` | `app/api/lessons.py`, `app/api/dto/lessons.py` | MỚI |
| Module nghiệp vụ bài học: cổng mở bài, chấm theo answer_spec (không sửa `app/mastery/grading.py`), idempotent theo requestId (làm theo mẫu `app/practice/store.py:260-356`), đánh giá lại path (dùng lại `app/application/path_reorder.py:44-81` và `OrderingValidator.apply`) | `app/lessons/` (grading, gates, reevaluate, store) | MỚI |
| Handler lỗi có `code` | `main.py:59-105` | SỬA |
| Client: thêm các endpoint `/internal/learning-content/*`; client access mới; thêm setting `access_service_base_url` | `app/clients/content_service.py`, `app/clients/access_service.py`, `app/config.py:13-60`, `app/api/dependencies.py` | SỬA + MỚI |
| Consumer: goal thành tùy chọn, nhận `package_version_id`, tìm path theo user, ≥70% thì PASSED và mở topic kế, rồi đánh giá lại path. Placement bỏ test-out | `app/adapters/formal_evidence_adapter.py:76-117` (:108), `app/application/formal_assessment_ingestion.py:39-53`, `app/application/formal_result_applier.py:93-95`, `app/learning/placement_test_out.py` | SỬA |
| Test: `test_formal_evidence_adapter.py:56` (test thiếu goal bị từ chối sẽ đảo kết quả); fake `tests/formal_assessment_support.py:80-236` đang tìm path theo goal; không sửa `test_mastery_*` | `tests/` | SỬA + MỚI |

### access-service, user-service, gateway

- access: xem §2. Ai-learning gọi bằng bearer của học viên, theo cùng mẫu forward token như assessment gọi content và user.
- user: giữ nguyên. Goal chỉ để hiển thị; V4 (một goal ACTIVE) giữ nguyên.
- gateway: không đổi (`/internal/**` không có route). Có thể thêm deny tường minh để phòng thủ nhiều lớp.
- learning-support: "thêm vào flashcard" dùng `POST /api/learning-support/flashcards` với `sourceType=VOCABULARY_SENSE`, `vocabularySenseId` (`FlashcardController.java:34`). Không đổi.

## 4. Chỗ artifact lệch với code (sửa khi cập nhật artifact hoặc plan)

1. Bước 10 để ai-learning gọi content `/package-versions/{id}/topic`. Consumer không làm được việc này (B1).
2. Sơ đồ vòng học ghi bảng `mastery_interactions`, còn ma trận ghi `mastery_events` và `mastery_learning_evidence`. Cả ba bảng đều có (V0_1, V2). Engine ghi vào `state_json` rồi dựng lại bảng `mastery_learning_evidence` mỗi lần commit (`postgres_learning_store.py:213-214`). Nên thống nhất một cách gọi.
3. Bước 8 ghi "assessment ghi đáp án chỉ ở server", nhưng `attempt_items` chưa có cột answer_spec hay max_score, và `assessment_results` không có cột điểm.
4. Bước 9 ghi "chấm theo answer_spec" nhưng chưa có contract; seed không khớp bộ chấm của game (B2).
5. `attemptType` `PRACTICE` không tồn tại, phải là `OFFICIAL_PRACTICE`.
6. Bước 3 và 5 dựa vào entitlement, nhưng endpoint hiện cho đọc (và trừ điểm) của user khác.
7. Xếp lại "trong cùng topic cha" chưa có dữ liệu cha trong path.

## 5. Tài liệu phải cập nhật

- `docs/contracts/assessment-completed-v2.md`: `:25-26` (không phát event khi không có goal), `:45` (`learning_goal_id` Required), `:89-91` (đỗ lại theo goal). Thêm `package_version_id`.
- `docs/contracts/answer-spec-v1.md` (mới) và contract API bài học (mới, ví dụ `lesson-learning-v1.md`).
- `docs/system-architecture.md`: §3 bảng HTTP `:53-67`, §4 `:85-103`, §6 `:145-155`, §11 `:219`.
- `.sdd/database/DATABASE_V5.md`: content §4.1 `:480`, §5.13-5.17 `:865-936`; ai-learning §7.1 `:1232`, §7.21-7.24 `:1734-1798`.
- `.sdd/specs/FEATURE_TREE_V2.md`: `:11` (còn ghi tutor là nơi học chính), `:269`, `:330`, `:472`, `:591`.

## 6. Thứ tự phase gợi ý

1. **Chốt contract**: answer_spec v1 + test vector; event v2 (goal tùy chọn, thêm package fields); API bài học + body lỗi; auth của `/internal`.
2. **Bảo mật tiên quyết** (§2): P0 của access và content, structure của assessment. Nhỏ, tách PR được.
3. **content**: V7 + domain + endpoint `/internal` + seed. Phase 4 và 5 phụ thuộc phase này.
4. **assessment**: V5, lấy đề từ content, tự chấm, result có điểm, event.
5. **ai-learning**: V10, path theo user, router và module bài học, consumer.
6. **Docs và FEATURE_TREE**.

Phase 4 và 5 chạy song song được sau phase 3, nhưng chỉ khi contract event đã chốt ở phase 1.

## Relevant Files

- ai-learning: `app/application/path_service.py`, `app/persistence/postgres_learning_store.py`, `app/messaging/assessment_consumer.py`, `app/adapters/formal_evidence_adapter.py`, `app/application/formal_assessment_ingestion.py`, `app/application/formal_result_applier.py`, `app/learning/external_assessment.py`, `app/application/path_reorder.py`, `app/clients/content_service.py`, `app/config.py`, `main.py`, `migrations/V1__one_mastery_path_per_learning_goal.sql`, `migrations/V3__pending_formal_assessment_results.sql`
- content: `domain/aggregate/Topic.java`, `domain/vo/PackageType.java`, `api/controller/InternalAssessmentContentController.java`, `api/controller/QuestionController.java`, `application/usecase/GetGameContentSnapshotUseCase.java`, `db/migration/V1..V6`
- assessment: `StartAssessmentAttemptUseCase.java`, `SubmitAssessmentAttemptUseCase.java`, `FinalizeAssessmentResultUseCase.java`, `AssessmentCompletedEventFactory.java`, `GetAttemptStructureUseCase.java`, `AssessmentResultController.java`, `infrastructure/client/ContentKnowledgeMappingClient.java`
- access: `api/controller/InternalAccessController.java`, `api/controller/LearnerAccessController.java`, `application/usecase/GetUserEntitlementUseCase.java`
- game (tham khảo bộ chấm): `infrastructure/client/JacksonGameAnswerEvaluator.java`
- plan liên quan: `plans/260928-2019-architecture-doc-service-split/main-learning-pipeline.md`, `plan.md`

## Unresolved Questions

1. B1: đưa topic hoặc package vào event (a) hay tra cục bộ trong `topic_progress` (b)?
2. B3: path cũ theo goal: migrate hay bỏ? Tutor, `/progress`, `/status` có chuyển sang path theo user không?
3. B4: MVP chỉ dùng seed, hay làm authoring (publish câu, gắn câu vào section, CRUD bài học)?
4. `/internal/**`: giữ "mọi internal JWT" hay thêm role hoặc claim cho service?
5. Feature key cho topic premium: `PREMIUM_CONTENT` hay key mới?
6. Đề có câu không khách quan (writing, speaking): chấm tự động một phần rồi examiner tạo version sau, hay cấm trong TOPIC_TEST?
7. Lưu score, maxScore, percent trên `assessment_results` hay tính lúc đọc?
8. Khối từ vựng trỏ tới `vocabulary_senses` trong content_db, trong khi plan tách service dự định dời vocabulary sang library-service?
9. Có sửa các lỗ P0 của access và content trong cùng đợt, hay tách thành plan bảo mật riêng?
10. Làm xong bài ôn (khối ≥ 70%, `path_review_items` chuyển DONE) có kích hoạt đánh giá lại path không? Bài đó đã có `completed_at` nên không rơi vào lúc (1). Đề xuất: không, chỉ đánh dấu DONE.
11. Một KP được nhiều bài dạy thì chèn bài nào làm bài ôn? Artifact tạm lấy `sort_order` nhỏ nhất.
