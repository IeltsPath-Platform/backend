# P3 – Learning: lộ trình theo skill, review khoá theo skill

Service: `learning-service`. Phụ thuộc: P1 (topic-sequence có `skill`, `hasTopicTest`; lesson DTO có `skill`).

## 1. Migration `V3__skill_tracks.sql`

```sql
ALTER TABLE topic_progress ADD COLUMN skill VARCHAR(20);
ALTER TABLE topic_progress ADD COLUMN has_topic_test BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE knowledge_point_catalog ADD COLUMN skill VARCHAR(20);
ALTER TABLE review_items ADD COLUMN skill VARCHAR(20);
CREATE INDEX idx_review_items_user_skill_pending ON review_items (user_id, skill) WHERE status = 'PENDING';
```

Tất cả **nullable** – được điền ở lần refresh curriculum đầu tiên (mục 3). Không backfill bằng hằng số skill.

### Dữ liệu Writing bị chuyển lesson (P1 mục 1.3)

Đã kiểm trên main: essay block **không bao giờ** vào `passed_block_ids` (chỉ `SubmitLessonExerciseUseCase` gọi
`passBlock`), và `lesson_progress.knowledge_point_ids` được ghi lại từ Content mỗi lần truy cập
(`LessonAccess.refresh` → `LessonProgress.place`). Vì vậy V3 chỉ cần, **với đúng các ID seed** (ghi rõ trong comment
SQL):

1. `UPDATE lesson_writing_submissions SET lesson_id = W1/W2` theo `block_id` essay đã chuyển.
2. `UPDATE review_items SET lesson_id = W1/W2` cho review của KP6/KP7 đang trỏ L3/L4.
3. Không đụng `lesson_progress`, `kp_evidence` (KP ID không đổi). W1/W2 tự tạo `lesson_progress` khi learner mở.

Ghi chú vào README: đây là di chuyển dữ liệu demo, không phải pattern chung.

## 2. Domain

| Thay đổi | File |
| --- | --- |
| `TopicProgress` thêm `skill` (enum `LearningSkill` mới trong `domain/vo`: LISTENING/READING/WRITING/SPEAKING) và `hasTopicTest`. | `domain/entity/TopicProgress.java`, `domain/vo/LearningSkill.java` |
| `TopicStatusDeriver.derive`: nhóm theo `skill`, mỗi nhóm áp dụng luật cũ (PASSED / topic chưa pass đầu tiên IN_PROGRESS / còn lại LOCKED). Topic `skill == null` xếp vào nhóm riêng "UNSPECIFIED" (giữ luật cũ, không crash). | `domain/service/TopicStatusDeriver.java` |
| `LearnerCurriculum.reorder` nhận thêm skill/hasTopicTest (đổi chữ ký thành list `TopicPlacement(topicId, skill, hasTopicTest)`). | `domain/aggregate/LearnerCurriculum.java` |
| `PendingReview` thêm `skill` (nullable). | `domain/vo/PendingReview.java` |
| `LessonAccessGate.authorize(pendingReviews, currentReviewId, lessonSkill, topicStatus, previousLessonsComplete)`: chỉ review có `skill == lessonSkill` **hoặc** `skill == null` chặn. | `domain/service/LessonAccessGate.java` |
| `ReviewItem` thêm `skill`; `ReviewRule.reevaluate` trả skill của lesson dạy KP (lấy từ `lessonsByKp` → topic → skill). | `domain/aggregate/ReviewItem.java`, `domain/service/ReviewRule.java` |

Unit test bắt buộc cho `TopicStatusDeriver` (2 skill độc lập, mỗi skill 1 IN_PROGRESS; topic skill null) và
`LessonAccessGate` (review Reading không chặn Listening; review skill null chặn tất cả; review hiện tại không tự chặn).

## 3. Application

- `RefreshLearningTopicsUseCase`:
  - lưu `skill`, `hasTopicTest` cho từng topic;
  - lưu `knowledge_point_catalog.skill`;
  - sau đó backfill `review_items.skill` còn NULL của user: join `review_items.lesson_id → lesson_progress.topic_id →
    topic_progress.skill`;
  - `TopicResult` thêm `skill`, `hasTopicTest`. Thứ tự trả về: theo skill (LISTENING, READING, WRITING, SPEAKING) rồi
    `sequence_order`.
- `LessonAccess.authorize`:
  - truyền skill của topic chứa lesson;
  - nếu `topic_progress.skill` NULL ⇒ gọi refresh trước (như nhánh "KP thiếu" hiện có).
- `GetTopicLessonsUseCase`: `testStatus` LOCKED chỉ khi có review chờ **cùng skill** (hoặc skill NULL); thêm
  `hasTopicTest` vào response; nếu `hasTopicTest=false` thì `testStatus = NONE`.
- `AssignTopicTestUseCase`:
  - REVIEW_REQUIRED chỉ xét review cùng skill;
  - topic `hasTopicTest=false` ⇒ 409 `NO_TOPIC_TEST`.
- **Pass topic không có test**: service mới `application/service/TopicCompletion` với
  `onLessonCompleted(userId, topicId)`. Nếu `hasTopicTest=false` và mọi lesson PUBLISHED của topic đã COMPLETED ⇒
  `curriculum.pass(topicId, now)` và lưu. Gọi ở `CompleteLessonUseCase`, `SubmitLessonExerciseUseCase` (khi
  `newlyCompleted`). Kiểm tra `SubmitLessonEssayUseCase`: nếu use case này có thể hoàn thành lesson thì gọi luôn; nếu
  không thì ghi rõ trong Verification.
- `ApplyAssessmentResultUseCase.applyTopicGate`: không đổi (chỉ áp dụng cho topic có test).
- `ReviewReevaluation.execute`: điền `skill` khi tạo review.
- `GET /api/learning/mastery`: mỗi KP thêm `skill` (từ catalog).

## 4. API (public) – chỉ thêm field

| Route | Thêm |
| --- | --- |
| `GET /api/learning/topics` | `skill`, `hasTopicTest` cho mỗi topic |
| `GET /api/learning/topics/{id}/lessons` | `skill`, `hasTopicTest`; `testStatus` có thể là `NONE` |
| `GET /api/learning/lessons/{id}` | `skill` |
| Lỗi `REVIEW_REQUIRED` | mỗi review trong danh sách có `skill`; chỉ liệt kê review cùng skill |
| Mới: `GET /api/learning/reviews?status=PENDING&skill=&limit=` | danh sách review của learner, cũ nhất trước, `limit` mặc định 20, tối đa 100 (vượt ⇒ 400); `status` mặc định `PENDING`. Item `{reviewId, knowledgePointId, lessonId, skill, stage, createdAt}` (`stage` thêm ở P5; P3 trả `PRACTICE`). Không trả tên KP (catalog không lưu tên; client đã có tên từ `GET /mastery`), chỉ đọc `learning_db`. |
| `GET /api/learning/topics` | thứ tự premium: topic `accessLevel=PREMIUM` vẫn xếp sau topic free **trong cùng skill**. |

## 5. Test

- Integration (Testcontainers, `LearningFlowIntegrationTest` hoặc file mới `SkillTrackIntegrationTest`), dùng
  `LearningContentClient` stub có 2 topic READING + 1 LISTENING + 1 WRITING (`hasTopicTest=false`):
  1. Ban đầu READING#1, LISTENING#1, WRITING#1 đều IN_PROGRESS.
  2. Có review Reading PENDING (tạo qua `AssessmentResult` TOPIC_GATE sai KP hoặc insert thẳng `review_items` trong
     test – P4 mới bỏ review khi hoàn thành lesson, nhưng test này không được dựa vào luồng đó) ⇒ lesson Reading tiếp
     theo 409 REVIEW_REQUIRED; lesson Listening 200.
  3. Hoàn thành mọi lesson Writing ⇒ topic Writing PASSED, không cần test.
  4. `POST /topics/{writing}/test-assignments` ⇒ 409 NO_TOPIC_TEST.
  5. Review cũ `skill NULL` chặn mọi skill cho tới khi refresh backfill.
- Migration test cho phần di chuyển dữ liệu Writing (seed row giả với ID demo).
- MVC test các field mới và route `GET /reviews`.

## Acceptance

`mvn -q -pl services/learning-service -am test` xanh; behaviour cũ trong một skill không đổi (test cũ chỉ được sửa
để thêm field/skill, không được đổi kỳ vọng nghiệp vụ trong cùng skill).

## Verification

- Changed learning-service migration, domain, use cases, JDBC adapters, API DTO/controller, four requested test classes, and the migration-version assertion; updated the learning README and learner contract.
- Focused four-class run: 50 tests passed, 0 failed, 0 skipped. The one full learning-service run: 202 tests, 201 passed, 1 failed because its Flyway version assertion still expected V2; after updating that assertion for V3, its one-class rerun passed (1/1). Docker/Testcontainers ran; no tests were skipped.
- `SubmitLessonEssayUseCase` records/grading submissions but does not call `LessonProgress.complete`, so it cannot complete a lesson and does not need a topic-completion hook.
- The existing `REVIEW_REQUIRED` HTTP status remains 403 per the public contract; the new Reading review flow verifies the code, while `NO_TOPIC_TEST` returns 409.
