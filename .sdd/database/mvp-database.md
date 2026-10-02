# Database cho Learning MVP (bài học, bài ôn, đề cuối, chấm Writing)

**Trạng thái 2026-10-02:** schema đã được triển khai bằng migration Java, không phải xác nhận migration đã chạy trên database của lập trình viên. `learning-service` thay `ai-learning-service` Python; sở hữu PostgreSQL `learning_db`, với **9 bảng ở V1 và 2 bảng Writing ở V2 (11 bảng)**.

Danh sách cột nằm trong [DATABASE_V5 §7](./DATABASE_V5.md#7-learning-service--tiến-độ-học-bằng-chứng-mastery-bài-ôn-mã-đề); route và cấu hình ở [Learning Service README](../../services/learning-service/README.md). Tài liệu `docs/ai-learning-database.md` đã bị xóa cùng mô hình Python; dùng lại hai nguồn này, không tạo tài liệu thay thế cho schema cũ.

## Ranh giới dữ liệu

| Service | Sở hữu |
| --- | --- |
| Content | Topic, KP, bài học, câu hỏi/version, answer spec, gói và asset. V8 tạo năm bảng bài học, thêm `content_packages.topic_id` cho `TOPIC_TEST`, bỏ band ở KP; V9 seed pipeline Reading. |
| Learning | Thứ tự topic theo user, tiến độ bài, submissions, bài ôn, lần giao đề, bằng chứng mastery, version kết quả đã nhận và hạn mức chấm Writing. |
| Assessment | Attempt, snapshot đề/đáp án, câu trả lời, result/version và outbox `AssessmentCompleted.v2`. |
| Access | Ví và sổ điểm; trừ point sau khi Learning lưu kết quả chấm Writing. |

ID của topic, bài, câu, KP, gói và attempt là logical reference; không có FK xuyên database. Không còn `mastery_paths.state_json`, bảng path DeepTutor, tutor, practice notebook, learner memory hay bảng đỗ kết quả thi. Không cần learning goal hoặc placement để bắt đầu luồng học; không sắp thứ tự bằng LLM hay lọc theo band.

Client gọi `/api/learning/**` qua Gateway với bearer; `CurrentUserProvider` lấy user từ internal JWT. Client không tự truyền danh tính cho progress. Ghi progress/evidence, bài ôn, giao đề và các transaction bắt đầu/kết thúc chấm Writing lấy `pg_advisory_xact_lock` theo user ở đầu transaction. Quota và trạng thái trung gian Writing hiện dùng SQL cập nhật nguyên tử có điều kiện ngoài các transaction khóa này. Khi đọc Content, Learning forward bearer và `X-Correlation-Id`; consumer kết quả thi không gọi HTTP.

## Luồng API ↔ bảng

Tất cả path trong bảng có prefix `/api/learning`. Payload chuẩn ở [lesson-learning-v1](../../docs/contracts/lesson-learning-v1.md), [lesson-writing-v1](../../docs/contracts/lesson-writing-v1.md) và [assessment-completed-v2](../../docs/contracts/assessment-completed-v2.md).

| Bước | Đọc/gọi | Ghi vào `learning_db` | Kết quả |
| --- | --- | --- | --- |
| `GET /topics` | Content `topic-sequence` một lần: topic `ACTIVE` có bài và mã đề `TOPIC_TEST` đã publish, kèm KP và `hasPracticeSet`. | Refresh `knowledge_point_catalog`, thứ tự user trong `topic_progress`. Topic bị bỏ khỏi sequence có `sequence_order = NULL`. | Topic theo thứ tự, trạng thái suy ra và số bài hoàn thành. Không gọi User hoặc LLM. |
| `GET /topics/{id}/lessons` | Content danh sách bài; progress và review của user. | Không tạo attempt thi. | Danh sách bài và `testStatus`; topic khóa trả `TOPIC_LOCKED`. |
| `GET /lessons/{id}` | Content nội dung; kiểm review, topic và bài trước. | Refresh `lesson_progress` (topic, thứ tự và KP bài dạy). | Khối/câu hỏi theo allowlist; lời giải chỉ ở khối đã đạt, transcript chỉ khi bài hoàn thành. |
| `POST /lessons/{id}/exercises/{blockId}/submissions` | Kiểm `requestId`, cổng bài; Content answer spec và KP; chấm cả khối. | `lesson_exercise_submissions`, khối đã đạt và `completed_at`; chỉ lần nộp đầu ghi `kp_evidence`; lúc bài mới hoàn thành xét tạo `review_items`. Cùng transaction. | Đúng/sai từng câu; khối đạt ≥70% có lời giải. Replay request cùng scope trả response đã lưu, khác scope trả `REQUEST_CONFLICT`. |
| `POST /lessons/{id}/complete` | Cùng cổng bài; chỉ cho bài không có khối tự chấm. | `lesson_progress.completed_at`; không tạo bằng chứng exercise. | Bài hoàn tất; bài có exercise trả `LESSON_HAS_EXERCISES`. Essay không chặn hoàn thành. |
| `GET /reviews/{id}` | Review thuộc user; TEXT của bài dạy KP; gói `PRACTICE_SET` từ Content. | Trả set mở hoặc tạo `review_sets`; không còn gói thì đổi review thành `SKIPPED`. | Lý thuyết và câu mới; ưu tiên package chưa giao, hết thì package giao lâu nhất. |
| `POST /reviews/{id}/submissions` | Set mở và answer spec của package. | Đóng set, lưu response và `review_set` evidence; đạt ≥70% → `DONE`, trượt lần thứ ba → `SKIPPED`, còn lại `PENDING`. | Lời giải/transcript chỉ khi set đạt. Replay cùng request trả response cũ; request mới cho set đóng trả `REVIEW_SET_CLOSED`. |
| `POST /topics/{id}/test-assignments` | Topic đang học, mọi bài đã xong, không còn review `PENDING`; Content mã đề. | Trả assignment mở hoặc tạo `topic_test_assignments`; chưa ghi mastery. | `assignmentId`, `packageId`, `packageVersionId`. Ưu tiên package chưa dùng, hết thì xoay lại package dùng lâu nhất. |
| `GET /mastery` | Catalog và evidence của user; không gọi Content. | Không lưu điểm mastery. | Điểm tính từ năm outcomes mới nhất theo `ordinal`, kèm tổng `evidenceCount`. |
| Event `AssessmentCompleted.v2` | Version đã áp, evidence và assignment từ DB; không HTTP, không cần trạng thái học có sẵn. | Trong một transaction: lưu version, thay evidence của attempt khi version cao hơn, consume assignment đủ điều kiện, ghi `passed_at` nếu đạt và xét review. | ACK sau commit; equal/older version bị bỏ qua; retry/DLQ theo cấu hình consumer. |

## Trạng thái và bằng chứng

### Topic và cổng bài học

`topic_progress` **không có cột `status`**. `PASSED` khi có `passed_at`; topic đầu chưa đạt theo `sequence_order` là `IN_PROGRESS`; các topic sau là `LOCKED`. `passed_at` một chiều: chấm lại điểm thấp hơn không khóa lại topic đã đạt.

Bài học mở theo thứ tự sau khi bài trước hoàn thành. Cổng kiểm theo thứ tự `REVIEW_REQUIRED` → `TOPIC_LOCKED` → `LESSON_LOCKED`; `LessonAccessGate` là authority cho truy cập bài. Tutor và `next_objective` không có trong runtime Java MVP.

### Evidence và review

- Lần nộp **đầu tiên của mỗi user/lesson/block** ghi đúng/sai của các câu theo KP vào `kp_evidence`, kể cả khối trượt. Nộp lại có thể qua khối nhưng không thêm bằng chứng exercise.
- Mastery tính khi đọc bằng `MasteryCalculator` (port `compute_mastery` DeepTutor v1.6.9): năm outcomes mới nhất theo `ordinal`, trọng số `0.5, 0.7, 0.85, 0.95, 1.0`; một/hai outcomes có trần `0.5`/`0.8`; không có evidence thì `0`.
- UUIDv5 provenance cùng UNIQUE (`user_id`, `source`, `source_reference_id`) chống ghi trùng. Nguồn gồm `lesson_exercise`, `review_set`, `assessment` và `lesson_writing` (V2).
- Review cần đủ bốn điều kiện: mastery dưới `learning.review-mastery-threshold` (`0.6` trong config-repo), có câu sai trong kết quả đang xét, có bài đã hoàn thành dạy KP và catalog có `has_practice_set`. Không tạo trùng review `PENDING` cho cùng user/KP.
- Set chỉ nộp một lần, mỗi set ghi evidence; review đạt → `DONE`, trượt set thứ ba hoặc không còn gói → `SKIPPED`. Cả hai trạng thái cho học tiếp. Số ba là hằng số, không phải setting; config hiện không khai báo alias env riêng cho review threshold.

### Đề cuối và consumer không phụ thuộc goal

Client gửi `{packageVersionId, mode, channel}` tới Assessment. `StartAssessmentAttemptUseCase` đọc Content trước transaction; `AttemptCreator` suy `TOPIC_TEST` → `TOPIC_GATE` và lưu snapshot. Không gọi User lấy goal; `learning_goal_id` của attempt mới và event là null.

Nếu mọi câu tự chấm được theo answer-spec v1, submit tạo result version 1 `COMPLETED`, item results và outbox trong cùng transaction; submit lặp không tạo thêm result. Câu không tự chấm được đi qua flow người chấm. Snapshot `{answerSpec, explanation, maxScore}` ở Assessment; learner chỉ thấy lời giải khi result đạt ≥70%. Thay đổi này dùng schema Assessment hiện có, không thêm migration.

Consumer khóa theo user và so version trên (`user_id`, `attempt_id`). Version bằng hoặc thấp hơn không làm gì; version cao hơn xóa `assessment` evidence cũ của attempt rồi thêm evidence mới. `PLACEMENT` chỉ lưu version. `TOPIC_GATE`, `MOCK`, `OFFICIAL_PRACTICE`, `QUIZ` xét lại review; không cần bảng path hoặc parked result.

Với `TOPIC_GATE`, assignment phải cùng user/package version, `consumed_at IS NULL` và **`assigned_at <= completed_at`**; lấy assignment mới nhất thỏa điều kiện. Consume cả khi trượt, lưu attempt/percent; đạt ≥70% thì ghi `passed_at`. Attempt khác hoặc version chấm lại tới sau khi assignment đã consume không mở lại lần giao đó. Consumer xử lý event đến; không có cơ chế sắp lại event theo thời gian hoàn thành.

## 11 bảng hiện có

| Migration | Bảng | Khóa/ràng buộc chính và mục đích |
| --- | --- | --- |
| V1 | `knowledge_point_catalog` | PK `kp_id`; catalog dùng chung với `topic_id`, `has_practice_set`, `refreshed_at`. |
| V1 | `topic_progress` | PK (`user_id`, `topic_id`); `sequence_order`, `passed_at`, `updated_at`. |
| V1 | `lesson_progress` | PK (`user_id`, `lesson_id`); topic/thứ tự, `knowledge_point_ids`, `passed_block_ids`, `completed_at`. |
| V1 | `lesson_exercise_submissions` | PK `id`; UNIQUE `request_id`; user/lesson/block, answers, response và thời điểm nộp. |
| V1 | `review_items` | PK `id`; partial UNIQUE user/KP khi `PENDING`; trạng thái `PENDING`, `DONE`, `SKIPPED`. |
| V1 | `review_sets` | PK `id`; FK `review_item_id` → `review_items`; một set mở mỗi review; UNIQUE nullable `request_id`. |
| V1 | `topic_test_assignments` | PK `id`; một assignment chưa consume mỗi user/topic; version đã giao, `assigned_at`, `consumed_attempt_id`, `consumed_at`, `percent`. |
| V1 | `kp_evidence` | PK `id`; `ordinal` identity; user/KP/correct/source/provenance; UNIQUE user/source/reference; attempt/version khi source assessment. |
| V1 | `assessment_result_versions` | PK (`user_id`, `attempt_id`); version ≥1 và `processed_at`. |
| V2 | `lesson_writing_submissions` | PK `id`; UNIQUE `request_id`; partial UNIQUE user/block khi `GRADING`; essay, prompt snapshot, grade/payment state, result và ledger ref. |
| V2 | `llm_daily_usage` | PK (`user_id`, `usage_date`, `kind`); `count` cho hạn mức chấm Writing. |

## Writing, Listening và Reading hints

### Writing đã triển khai (V2)

Task 1/Task 2 dùng `POST /lessons/{id}/essays/{blockId}/submissions`, `{requestId, essayText}`. Kiểm 50–1.000 từ và ≤10.000 ký tự → lookup request → đọc bài → kiểm số dư Access → transaction ngắn tạo `GRADING` → giữ lượt ngày → LLM ngoài transaction → lưu `PAYMENT_PENDING` → debit idempotent → transaction ghi `GRADED` và `lesson_writing` evidence. Mặc định 3 point, 10 lượt/ngày theo `Asia/Ho_Chi_Minh`; LLM lỗi không trừ point và không trả lượt quota.

Request lặp tiếp tục bước còn dang dở; grade chỉ được lộ khi `GRADED`. Evidence ghi từng bài đã chấm tới khi khối essay đạt lần đầu. Khối essay không chặn hoàn thành bài; `chartFacts` không ra learner, sample answer chỉ khi đạt. Xem chi tiết state/setting trong [README Learning](../../services/learning-service/README.md#writing-essays).

### Listening đã triển khai, không thêm bảng Learning

Content V12 seed audio; `media_reference` là key hoặc https, Content ghép key với `CONTENT_MEDIA_BASE_URL` và trả `mediaUrl`. File mp3 upload riêng theo [bảng key](../../services/content-service/README.md#media), không lưu binary trong DB. Transcript chỉ hiện khi bài xong, review set đạt ≥70%, hoặc result Assessment đạt ≥70%. Dùng lại progress, submissions, review và assignments ở V1.

### Reading hints đã triển khai, không thêm schema Learning

Content V13 thêm `question_versions.hint` nullable và seed gợi ý Reading; API thêm version giới hạn 500 ký tự. Content trả hint qua lesson nội bộ, Learning Java quyết định hiển thị: answer spec hợp lệ `FILL` hoặc `CHOICE` có ≥3 lựa chọn (TFNG hỗ trợ options thiếu/rỗng), câu từng sai trong cùng user/bài/khối chưa đạt. Gợi ý giữ cả khi lần sau câu đúng nhưng khối vẫn trượt; khối đạt thì mọi hint null. GET câu hỏi và POST results luôn có khóa `hint`; DTO dùng chung cho review luôn null, package/đề cuối/game không cấp gợi ý.

Lịch sử câu sai đọc từ `lesson_exercise_submissions.response.results` của mọi lần nộp; dùng index (`user_id`, `lesson_id`, `block_id`) đã có ở V1, không lưu cột trạng thái riêng. Replay `requestId` trả nguyên response đã lưu, kể cả hint cũ sau khi khối đạt. Evidence/mastery vẫn dùng lần nộp đầu; không thêm `hints_used` vào `kp_evidence`. Xem [contract bài học](../../docs/contracts/lesson-learning-v1.md).

## Nguồn đã đối chiếu

- [Learning V1](../../services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql), [V2](../../services/learning-service/src/main/resources/db/migration/V2__lesson_writing.sql).
- [Content V8](../../services/content-service/src/main/resources/db/migration/V8__lessons_and_topic_tests.sql), [V9](../../services/content-service/src/main/resources/db/migration/V9__seed_lesson_pipeline_demo.sql).
- [LearnLessonUseCase](../../services/learning-service/src/main/java/com/group01/learning/application/usecase/LearnLessonUseCase.java), [ReviewUseCase](../../services/learning-service/src/main/java/com/group01/learning/application/usecase/ReviewUseCase.java), [LessonEvidenceReference](../../services/learning-service/src/main/java/com/group01/learning/application/LessonEvidenceReference.java).
- [ApplyAssessmentResultUseCase](../../services/learning-service/src/main/java/com/group01/learning/application/usecase/ApplyAssessmentResultUseCase.java), [JdbcAssessmentResultStore](../../services/learning-service/src/main/java/com/group01/learning/infrastructure/persistence/JdbcAssessmentResultStore.java), [JdbcLearningProgressStore](../../services/learning-service/src/main/java/com/group01/learning/infrastructure/persistence/JdbcLearningProgressStore.java).
