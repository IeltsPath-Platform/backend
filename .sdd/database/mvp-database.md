# Database cho Learning MVP (lộ trình theo skill, bài học, Practice, bài ôn, đề cuối, chấm Writing)

**Trạng thái 2026-10-03:** schema triển khai bằng migration Java Learning V1–V5 (**14 bảng**) và Content V8–V18; không
xác nhận migration đã chạy trên database của lập trình viên.

Danh sách cột nằm trong [DATABASE_V5 §7](./DATABASE_V5.md#7-learning-service--tiến-độ-học-bằng-chứng-mastery-bài-ôn-mã-đề);
route và cấu hình ở [Learning Service README](../../services/learning-service/README.md).

## Ranh giới dữ liệu

| Service | Sở hữu |
| --- | --- |
| Content | Topic (một skill mỗi topic), KP, bài học, khối và KP của khối TEXT, câu hỏi/version (hint, purpose), answer spec, gói (Practice gắn bài, đề cuối gắn topic) và asset. Mỗi câu chỉ thuộc một nơi dùng. |
| Learning | Thứ tự topic theo user và skill, tiến độ bài, submissions, Practice attempt, bài qua Practice, bài ôn (stage, quick-check), lần giao đề, bằng chứng mastery, version kết quả đã nhận và hạn mức chấm Writing. |
| Assessment | Attempt, snapshot đề/đáp án, câu trả lời, result/version và outbox `AssessmentCompleted.v2`. |
| Access | Ví và sổ điểm; trừ point sau khi Learning lưu kết quả chấm Writing. |

ID của topic, bài, câu, KP, gói và attempt là logical reference; không có FK xuyên database. Không cần learning goal hoặc
placement để bắt đầu luồng học; không sắp thứ tự bằng LLM hay lọc theo band.

Client gọi `/api/learning/**` qua Gateway với bearer; `CurrentUserProvider` lấy user từ internal JWT. Mọi lượt ghi của
một học viên lấy `pg_advisory_xact_lock` theo user ở đầu transaction. Quota và trạng thái trung gian Writing dùng SQL
cập nhật nguyên tử có điều kiện ngoài các transaction khóa này. Khi đọc Content, Learning forward bearer và
`X-Correlation-Id`; consumer kết quả thi không gọi HTTP.

## Luồng API ↔ bảng

Tất cả path có prefix `/api/learning`. Payload chuẩn ở [lesson-learning-v1](../../docs/contracts/lesson-learning-v1.md),
[lesson-writing-v1](../../docs/contracts/lesson-writing-v1.md) và [assessment-completed-v2](../../docs/contracts/assessment-completed-v2.md).

| Bước | Đọc/gọi | Ghi vào `learning_db` | Kết quả |
| --- | --- | --- | --- |
| `GET /topics` | Content `topic-sequence` một lần: topic `ACTIVE` có skill và bài đã publish, kèm `hasTopicTest`, KP và `hasPracticeSet`. | Refresh `knowledge_point_catalog` (kèm skill), `topic_progress` (thứ tự, skill, `has_topic_test`); điền skill cho bài ôn cũ chưa có. | Topic theo skill rồi thứ tự; mỗi skill một `IN_PROGRESS`. |
| `GET /topics/{id}/lessons` | Content danh sách bài và Practice của topic; progress, review, Practice của user. | Không ghi. | Bài kèm `practiceStatus`, `practicePassReason`; `testStatus` (`NONE` khi không có đề cuối). |
| `GET /lessons/{id}` | Content nội dung; kiểm review cùng skill, topic, bài trước. | Refresh `lesson_progress`. | Khối/câu theo allowlist; lời giải chỉ ở khối đã đạt, transcript chỉ khi bài hoàn thành. |
| `POST /lessons/{id}/exercises/{blockId}/submissions` | `requestId`, cổng bài; answer spec và KP; chấm cả khối. | `lesson_exercise_submissions`, khối đạt, `completed_at`; lần nộp đầu ghi `kp_evidence`; bài vừa xong thì lưu `lesson_practice_passes` (`NO_PRACTICE`) nếu bài không có đề. Không tạo bài ôn. | Đúng/sai từng câu; khối đạt ≥70% có lời giải. |
| `POST /lessons/{id}/complete` | Bài không có khối tự chấm (Writing W1/W2). | `completed_at`; topic không có đề cuối thì `passed_at` khi xong mọi bài. | `LESSON_HAS_EXERCISES` nếu bài có bài tập. |
| `GET /lessons/{id}/practice-sets` | Content đề Practice của bài; attempt, set ôn của user. | Không ghi. | Item kèm `status`, `revealed`; `practiceStatus` của bài. |
| `POST /lessons/{id}/practice-attempts`, `POST /practice-attempts/{id}/submissions` | Bài đã xong, không có review cùng skill; package version từ Content. | `practice_attempts`; lần nộp đầu của gói chưa lộ ghi `practice_set` evidence; < 70% tạo `review_items` (`trigger_kind = PRACTICE`, stage ban đầu); lưu `lesson_practice_passes` khi bài qua. | Lời giải và transcript sau khi nộp; `reviewsCreated`. |
| `GET /reviews/{id}` | Review của user; khối TEXT của KP và câu quick-check từ bài; gói chưa giao/chưa lộ từ Content. | PRACTICE: tạo `review_sets` (hết gói → `SKIPPED`); review từ kết quả thi lần đầu mở có thể chuyển THEORY (`WRONG_IN_LESSON`). | Lý thuyết của KP; set có hint, hoặc quick-check khi THEORY. |
| `POST /reviews/{id}/submissions` | Set mở; answer spec của gói. | Đóng set (`correct_count`, `total_count`), `review_set` evidence; đạt → `DONE`, trượt → THEORY, trượt set thứ hai → `SKIPPED`. | Lời giải và transcript mọi lần nộp; `THEORY_REQUIRED` khi đang THEORY. |
| `POST /reviews/{id}/theory-check` | Review ở THEORY; câu quick-check từ bài. | `review_theory_checks`; review về PRACTICE, `theory_completed_count++`; không ghi evidence. | Lời giải quick-check. |
| `POST /topics/{id}/test-assignments` | Topic đang học, mọi bài xong và qua Practice, không review cùng skill; Content mã đề. | Lưu `lesson_practice_passes` tính được; trả assignment mở hoặc tạo `topic_test_assignments`. | `REVIEW_REQUIRED` (403), `PRACTICE_REQUIRED`/`NO_TOPIC_TEST` (409), `TEST_LOCKED` (403). |
| `GET /reviews` | `review_items` của user. | Không ghi. | Cũ nhất trước, lọc `status`, `skill`, `limit` ≤ 100. |
| `GET /mastery` | Catalog và evidence của user; không gọi Content. | Không ghi. | Điểm từ năm outcomes mới nhất theo `ordinal`, kèm skill. |
| Event `AssessmentCompleted.v2` | Version đã áp, evidence, assignment từ DB; không HTTP. | Một transaction: lưu version, thay evidence khi version cao hơn, consume assignment đủ điều kiện, ghi `passed_at` nếu đạt, xét bài ôn theo mastery. | ACK sau commit; retry/DLQ theo cấu hình. |

## Trạng thái và bằng chứng

### Topic, cổng bài, Practice

`topic_progress` **không có cột `status`**. Trong mỗi skill: `PASSED` khi có `passed_at`, topic đầu chưa đạt theo
`sequence_order` là `IN_PROGRESS`, còn lại `LOCKED`. `passed_at` một chiều.

Bài mở theo thứ tự sau khi bài trước hoàn thành. Cổng kiểm `REVIEW_REQUIRED` (chỉ review cùng skill, hoặc review chưa có
skill) → `TOPIC_LOCKED` → `LESSON_LOCKED`. Bài qua Practice (một chiều, `lesson_practice_passes`) theo thứ tự lý do:
`FIRST_SUBMISSION`, `REVIEW_FINISHED`, `ALL_SETS_ATTEMPTED`, `NO_PRACTICE`. Gói "đã lộ" = đã nộp ở Practice hoặc set ôn;
nộp gói đã lộ không ghi evidence, không tạo bài ôn, không cho `FIRST_SUBMISSION`.

### Evidence và bài ôn

- Nguồn evidence: `lesson_exercise` (lần nộp đầu mỗi khối), `practice_set` (lần nộp đầu của gói chưa lộ), `review_set`
  (mỗi set), `assessment` (theo result version), `lesson_writing`. UUIDv5 provenance cùng UNIQUE (`user_id`, `source`,
  `source_reference_id`) chống ghi trùng. Quick-check không ghi evidence.
- Mastery tính khi đọc bằng `MasteryCalculator` (port `compute_mastery` DeepTutor v1.6.9): năm outcomes mới nhất theo
  `ordinal`, trọng số `0.5, 0.7, 0.85, 0.95, 1.0`; một/hai outcomes có trần `0.5`/`0.8`; không có evidence thì `0`.
- Bài ôn từ Practice: lần nộp đầu < 70%, KP < 70% trong attempt, chưa có review chờ, còn gói chưa lộ. Bài ôn từ kết quả
  thi: mastery dưới `learning.review-mastery-threshold` (`0.6`), có câu sai, có bài đã xong dạy KP, `has_practice_set`.
- Thang ôn: stage PRACTICE (set có hint) hoặc THEORY (khối TEXT của KP + tối đa 3 câu quick-check). Bắt đầu ở THEORY khi
  KP < 40% trong attempt hoặc đã sai trong lần nộp đầu bài tập của bài. `MAX_FAILED_REVIEW_SETS = 2` là hằng số code.

### Đề cuối và consumer

Client gửi `{packageVersionId, mode, channel}` tới Assessment. `AttemptCreator` suy `TOPIC_TEST` → `TOPIC_GATE` và lưu
snapshot; câu tự chấm được thì submit tạo result version 1 và outbox cùng transaction. Consumer khóa theo user, bỏ
version bằng/cũ, thay evidence khi version cao hơn; `PLACEMENT` chỉ lưu version. Với `TOPIC_GATE`, assignment phải cùng
user/package version, `consumed_at IS NULL` và `assigned_at <= completed_at`; consume cả khi trượt; ≥70% ghi `passed_at`.

## 14 bảng hiện có

| Migration | Bảng | Khóa/ràng buộc chính và mục đích |
| --- | --- | --- |
| V1 (+V3 `skill`) | `knowledge_point_catalog` | PK `kp_id`; `topic_id`, `has_practice_set`, `skill`, `refreshed_at`. |
| V1 (+V3) | `topic_progress` | PK (`user_id`, `topic_id`); `sequence_order`, `passed_at`, `skill`, `has_topic_test`. |
| V1 | `lesson_progress` | PK (`user_id`, `lesson_id`); topic/thứ tự, `knowledge_point_ids`, `passed_block_ids`, `completed_at`. |
| V1 | `lesson_exercise_submissions` | PK `id`; UNIQUE `request_id`; user/lesson/block, answers, response. |
| V1 (+V3–V5) | `review_items` | Partial UNIQUE user/KP khi `PENDING`; `skill`, `trigger_kind`, `source_attempt_id`, `stage`, `theory_reason`, `theory_completed_count`. |
| V1 (+V5) | `review_sets` | FK `review_item_id`; một set mở mỗi review; `correct_count`, `total_count`. |
| V1 | `topic_test_assignments` | Một assignment chưa consume mỗi user/topic. |
| V1 (+V4 source) | `kp_evidence` | `ordinal` identity; UNIQUE user/source/reference; source gồm `practice_set`. |
| V1 | `assessment_result_versions` | PK (`user_id`, `attempt_id`); version ≥ 1. |
| V2 | `lesson_writing_submissions` | UNIQUE `request_id`; partial UNIQUE user/block khi `GRADING`. |
| V2 | `llm_daily_usage` | PK (`user_id`, `usage_date`, `kind`); hạn mức chấm Writing. |
| V4 | `practice_attempts` | Partial UNIQUE (`user_id`, `package_id`) khi chưa nộp; UNIQUE `request_id`; `counted_as_evidence`. |
| V4 | `lesson_practice_passes` | PK (`user_id`, `lesson_id`); `reason`, một chiều. |
| V5 | `review_theory_checks` | FK `review_item_id`; UNIQUE `request_id`; câu đã hỏi, answers, response. |

## Writing, Listening và Reading hints

- **Writing (V2):** Task 1/Task 2 dùng `POST /lessons/{id}/essays/{blockId}/submissions`; kiểm 50–1.000 từ, giữ lượt ngày,
  gọi LLM ngoài transaction, lưu `PAYMENT_PENDING`, trừ point idempotent rồi `GRADED`. Mặc định 3 point, 10 lượt/ngày.
  Bài luận nằm ở topic `DEMO_WRITING` (W1, W2), không có đề cuối. Chi tiết ở [README Learning](../../services/learning-service/README.md#writing-essays).
- **Listening:** Content V12 seed audio; Content ghép key với `CONTENT_MEDIA_BASE_URL`. Transcript hiện khi bài xong, sau
  khi nộp Practice hay set ôn, hoặc khi result Assessment ≥70%. Mỗi KP Listening chỉ có một đề Practice (chưa có mp3 mới).
- **Reading hints:** Content V13 `question_versions.hint`. Trong bài học, hint mở cho câu từng sai của khối chưa đạt;
  câu của đề Practice, set ôn và quick-check luôn kèm hint; đề cuối và game không có.

## Nguồn đã đối chiếu

- Learning migration [V1](../../services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql)–[V5](../../services/learning-service/src/main/resources/db/migration/V5__review_ladder.sql).
- Content migration V8–V18 trong `services/content-service/src/main/resources/db/migration/`.
- Use case Learning: `GetReviewUseCase`, `SubmitReviewUseCase`, `SubmitTheoryCheckUseCase`, `SubmitPracticeAttemptUseCase`,
  `AssignTopicTestUseCase`, `ApplyAssessmentResultUseCase` trong `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/`.
