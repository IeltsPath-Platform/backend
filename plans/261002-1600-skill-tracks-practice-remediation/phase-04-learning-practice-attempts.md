# P4 – Learning: catalog Practice và practice attempt

Service: `learning-service`. Phụ thuộc: P2 (internal `lessons/{id}/practice-sets`), P3 (skill gate).

## 1. Luật nghiệp vụ

1. Practice của lesson chỉ mở khi `lesson_progress.completed_at IS NOT NULL` ⇒ ngược lại 409 `PRACTICE_LOCKED`.
2. Có review PENDING cùng skill (hoặc skill NULL) ⇒ 409 `REVIEW_REQUIRED` (giống lesson): learner phải ôn trước.
3. Premium: **không kiểm gói** (Learning chưa kiểm gói ở lesson; `AccessClient` chỉ có `balance`/`debit`). Catalog
   trả `accessLevel` (`FREE`/`PREMIUM` theo `requiredFeatureKey`) như `GET /topics`.
4. Mỗi lần "bắt đầu" tạo một attempt gắn `package_version_id` hiện tại. Mỗi (user, package) chỉ có **một attempt mở**
   (chưa nộp); bắt đầu lại khi đang mở ⇒ trả attempt đang mở (idempotent).
5. Nộp: chấm bằng `AnswerSpecGrader` như review set; `passed = PassMark` (≥ 70%). `percent` = đúng / tổng.
6. Evidence: **chỉ lần nộp đầu tiên của mỗi (user, package)** ghi `kp_evidence` source `practice_set`. Package
   "đã lộ" = có practice attempt đã nộp **hoặc** `review_sets` đã nộp (`submitted_at IS NOT NULL`) với cùng
   `package_id`; nộp practice cho package đã lộ ⇒ `countedAsEvidence = false` (kể cả lần practice đầu tiên của package
   đó). Lần được tính ghi
   mỗi item × mỗi KP mapping (giống `SubmitReviewUseCase`). `source_reference_id` =
   `LessonEvidenceReference.forPracticeSet(requestId, questionVersionId, kpId)` – **namespace UUIDv5 mới**
   `ielts-path:practice_set`, vì `kp_evidence` có `UNIQUE(user_id, source, source_reference_id)` và một attempt sinh
   nhiều dòng.
7. Sau khi nộp (ngoại lệ D11): response luôn có `correctAnswer`, `explanation` cho mọi câu, và `transcript` của
   section Listening. Trước khi nộp tuyệt đối không có các field này. Package này từ đó "đã lộ" với learner.
8. **Tạo review từ Practice** (không xét mastery, không dùng `ReviewRule.reevaluate`). Domain service mới
   `domain/service/PracticeReviewRule` (thuần Java, unit test) trả danh sách KP cần review khi **đủ cả**:
   - attempt là lần nộp đầu của package (`countedAsEvidence = true`) và `percent < 0.70`;
   - KP có tỉ lệ đúng < 70% **trong attempt này** (đếm theo item × KP mapping; item map nhiều KP thì tính cho mỗi KP);
   - KP chưa có review PENDING;
   - KP còn ≥ 1 package chưa lộ: gọi **một lần** `content.practiceSetAvailability(kps, exclude = package đã lộ ∪
     package vừa nộp, MIN_SET_QUESTIONS)` (route P2). Hết package ⇒ không tạo.
   Review tạo với `lesson_id` = lesson của attempt, `skill` = skill lesson, `trigger_kind = 'PRACTICE'`,
   `source_attempt_id` = attempt, kèm `kpPercent` để P5 tính stage ban đầu (P4: stage mặc định PRACTICE).
   Attempt không được tính (`countedAsEvidence = false`, gồm package đã lộ qua set ôn) không tạo review.
9. **Bỏ review khi hoàn thành lesson**: `CompleteLessonUseCase`, `SubmitLessonExerciseUseCase` không gọi
   `ReviewReevaluation` nữa (vẫn ghi evidence `lesson_exercise` lần nộp đầu). `ReviewReevaluation` chỉ còn caller
   `ApplyAssessmentResultUseCase` (luật mastery < 0.6 giữ nguyên). Sửa test cũ kỳ vọng review sau khi hoàn thành lesson.
10. **Topic test cần qua Practice (D13)**: domain service `domain/service/PracticeClearance` (thuần Java) nhận, cho
    mỗi lesson của topic: package PRACTICE_SET PUBLISHED đang gắn lesson, attempt đã nộp của learner (cờ lần đầu đạt),
    review `trigger_kind='PRACTICE'` của lesson và trạng thái, và dòng `lesson_practice_passes` đã lưu ⇒ trả
    `practiceStatus` (`LOCKED` – lesson chưa COMPLETED; `REQUIRED` – chưa qua; `PASSED`) và `practicePassReason`
    (`FIRST_SUBMISSION | REVIEW_FINISHED | ALL_SETS_ATTEMPTED | NO_PRACTICE`, ưu tiên theo thứ tự này khi nhiều điều
    kiện cùng đúng). Dòng đã lưu luôn thắng (đơn điệu).
    - **Lưu** (`INSERT … ON CONFLICT DO NOTHING`, trong transaction có `LearnerLock`) ở mọi đường ghi có thể làm lesson
      qua: `SubmitPracticeAttemptUseCase`, `SubmitReviewUseCase` và `GetReviewUseCase` khi review kết thúc
      (DONE/SKIPPED), `CompleteLessonUseCase`/`SubmitLessonExerciseUseCase` khi lesson vừa COMPLETED (bắt
      `NO_PRACTICE`), `AssignTopicTestUseCase` (lưu các lesson tính ra PASSED trước khi xét).
    - Đường đọc (`GetTopicLessonsUseCase`, catalog) chỉ tính, không ghi.
    - `AssignTopicTestUseCase`: sau `REVIEW_REQUIRED`, trước `TEST_LOCKED` ⇒ 409 `PRACTICE_REQUIRED`
      (body liệt kê `lessonIds` chưa qua);
    - `GetTopicLessonsUseCase`: mỗi lesson thêm `practiceStatus`, `practicePassReason`; `testStatus` LOCKED nếu còn
      lesson chưa PASSED. `GET /lessons/{id}/practice-sets` cũng trả hai field này.
    Dữ liệu đọc theo tập: một lần lấy practice sets của mọi lesson trong topic (Content: thêm
    `GET /internal/learning-content/topics/{id}/practice-sets` trả theo lesson, hoặc lặp `lessons/{id}/practice-sets`
    là **cấm**), một query attempt, một query review.

## 2. Migration `V4__practice_attempts.sql`

```sql
CREATE TABLE practice_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    skill VARCHAR(20) NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    request_id UUID UNIQUE,
    correct_count INTEGER,
    total_count INTEGER,
    passed BOOLEAN,
    counted_as_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    response JSONB,
    CHECK ((submitted_at IS NULL) = (request_id IS NULL))
);
CREATE UNIQUE INDEX uq_practice_attempt_open ON practice_attempts (user_id, package_id) WHERE submitted_at IS NULL;
CREATE INDEX idx_practice_attempts_user_lesson ON practice_attempts (user_id, lesson_id, started_at DESC);

ALTER TABLE review_items ADD COLUMN trigger_kind VARCHAR(20) CHECK (trigger_kind IN ('PRACTICE', 'ASSESSMENT'));
ALTER TABLE review_items ADD COLUMN source_attempt_id UUID REFERENCES practice_attempts(id);
CREATE INDEX idx_review_items_user_lesson_trigger ON review_items (user_id, lesson_id, trigger_kind);

-- Lesson đã qua Practice thì không bao giờ quay lại chưa qua, kể cả khi seed thêm package sau này.
CREATE TABLE lesson_practice_passes (
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL
        CHECK (reason IN ('FIRST_SUBMISSION', 'REVIEW_FINISHED', 'ALL_SETS_ATTEMPTED', 'NO_PRACTICE')),
    passed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, lesson_id)
);

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check
    CHECK (source IN ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set'));
```

## 3. Code

| Lớp | Việc |
| --- | --- |
| `application/port/LearningContentClient` | thêm `lessonPracticeSets(lessonId)`, `topicPracticeSets(topicId)`, `practiceSetAvailability(kps, exclude, minQuestions)`; thêm `preferredLessonId` vào `searchPracticeSets`. Cài ở `infrastructure/client`. |
| `domain/service/PracticeReviewRule`, `domain/service/PracticeClearance` | luật 8 và 10, thuần Java. |
| `domain/aggregate/PracticeAttempt` | `start(...)`, `acceptsAnswers(requestId)`, `recordResult(correct, total, countedAsEvidence)`; bất biến: không nộp 2 lần với request khác nhau (trả 409 `ATTEMPT_ALREADY_SUBMITTED`), cùng `requestId` ⇒ trả lại `response` đã lưu. |
| `domain/repository/PracticeAttemptRepository` + `infrastructure/persistence/JdbcPracticeAttemptRepository` | lưu/đọc; `latestByLesson(userId, lessonId)`; `revealedPackageIds(userId)` = package có practice attempt đã nộp ∪ `review_sets` đã nộp (một query UNION; dùng cho `countedAsEvidence`, luật 8, `ALL_SETS_ATTEMPTED`, và P5 chọn set ôn – D11). Mọi hàm đọc theo tập, không gọi trong vòng lặp. |
| `application/service/LessonEvidenceReference` | thêm `forPracticeSet` (namespace riêng). |
| `application/service/PracticeAccess` | luật 1–2, dùng lại `LessonAccess` cho lesson và gate. |
| `application/service/ItemGrading` (tách từ `SubmitReviewUseCase`) | chấm một `PackageVersion` theo `AnswerSheet`, trả item kết quả + KP sai; dùng chung cho review set, practice, quick-check (P5). Không đổi output của review. |
| `GetLessonPracticeSetsUseCase` | danh sách practice của lesson + trạng thái. |
| `StartPracticeAttemptUseCase` | luật 1–4. |
| `GetPracticeAttemptUseCase` | trả câu hỏi (không đáp án; có `hint`), hoặc kết quả nếu đã nộp. |
| `SubmitPracticeAttemptUseCase` | luật 5–8, `@Transactional`, `LearnerLock`. |
| `api/controller/PracticeController` + DTO request/response | route mục 4. |

## 4. API public

| Route | Mô tả |
| --- | --- |
| `GET /api/learning/lessons/{id}/practice-sets` | `{lessonId, skill, lessonCompleted, items:[{packageId, code, title, questionCount, accessLevel, status, bestPercent, lastAttemptId}]}`. Số package mỗi lesson nhỏ (seed ≤ 4); vẫn đặt trần 50 ở Content. `status`: `LOCKED` (lesson chưa xong hoặc review chờ cùng skill), `AVAILABLE`, `IN_PROGRESS` (có attempt mở), `PASSED` (có attempt đạt), `ATTEMPTED` (đã làm, chưa đạt). Thêm `revealed: boolean` (đã nộp ở Practice hoặc set ôn ⇒ lần nộp tới không tính điểm). Không lỗi khi lesson chưa xong – trả LOCKED. |
| `POST /api/learning/lessons/{id}/practice-attempts` body `{packageId}` | 201 `{attemptId, packageId, packageVersionId, passage?, audio?, questions}` – cùng shape với `set` của `GET /reviews/{id}` (allowlist câu hỏi của lesson `{questionVersionId, sortOrder, stem, options, hint}`, `audio` không có transcript); lỗi 409 `PRACTICE_LOCKED` / `REVIEW_REQUIRED`, 404 nếu package không thuộc lesson. |
| `GET /api/learning/practice-attempts/{id}` | như trên, hoặc kết quả đã lưu nếu đã nộp. 404 nếu không phải của user. |
| `POST /api/learning/practice-attempts/{id}/submissions` body `{requestId, answers}` (answers cùng shape lesson submission, đủ mọi câu) | `{attemptId, correct, total, percent, passed, countedAsEvidence, results:[{questionVersionId, correct, correctAnswer, explanation, hint:null}], transcript?, reviewsCreated:[{reviewId, knowledgePointId, stage}]}` – `results` dùng lại `SubmissionResponse.SolvedAnswer`. |

Gateway: các route nằm dưới `/api/learning/**` đã route sẵn – Codex kiểm tra config gateway, không cần thêm nếu đã có.

## 5. Test

- Unit: `PracticeAttempt` (idempotent theo requestId, không nộp 2 lần); `PracticeReviewRule` (attempt ≥ 70% ⇒ không
  tạo; KP 2/3 đúng trong attempt 50% ⇒ tạo cho KP < 70% thôi; lần làm lại ⇒ không tạo; KP đã có review chờ ⇒ không
  tạo; availability 0 ⇒ không tạo); `PracticeClearance` (4 reason D13 và thứ tự ưu tiên; lần làm lại đạt **không**
  tính; review PRACTICE của lesson DONE/SKIPPED tính; nộp hết set nhưng còn review PRACTICE chờ ⇒ REQUIRED; dòng đã
  lưu thắng dù có package mới chưa làm).
- Integration thêm: lesson PASSED theo `ALL_SETS_ATTEMPTED`, sau đó publish thêm package cho lesson ⇒ vẫn PASSED
  (đơn điệu); lesson không có package ⇒ PASSED/`NO_PRACTICE`; đường đọc không ghi `lesson_practice_passes`.
- Integration:
  1. Lesson chưa xong ⇒ catalog LOCKED, start 409 PRACTICE_LOCKED.
  2. Xong lesson ⇒ AVAILABLE ⇒ start ⇒ submit 100% ⇒ PASSED, evidence `practice_set` ghi đúng số dòng.
  3. Submit lần 2 cùng requestId trả cùng body; requestId khác ⇒ 409.
  4. Attempt thứ hai cùng package ⇒ `countedAsEvidence=false`, không thêm `kp_evidence`. Package đã nộp trong set ôn
     (insert `review_sets` đã nộp) ⇒ practice attempt đầu tiên của nó cũng `countedAsEvidence=false`, trượt không
     tạo review, đạt không cho `FIRST_SUBMISSION`.
  5. Submit 50% (mastery KP vẫn ≥ 0.6) ⇒ review PENDING vẫn được tạo cho KP < 70%, `reviewsCreated` không rỗng,
     `trigger_kind=PRACTICE`; practice khác cùng skill ⇒ LOCKED; lesson skill khác vẫn vào được.
  6. Hoàn thành lesson có câu sai ⇒ **không** tạo review; evidence `lesson_exercise` vẫn ghi.
  7. Topic test: xong mọi lesson nhưng chưa qua Practice ⇒ 409 `PRACTICE_REQUIRED`; lần nộp đầu đạt ở mỗi lesson ⇒
     giao đề được. Một lesson làm lại đạt sau khi lần đầu trượt và review chưa xong ⇒ vẫn chưa qua.
  8. Assessment TOPIC_GATE sai KP với mastery < 0.6 vẫn tạo review như cũ (`trigger_kind=ASSESSMENT`).
- MVC: không lộ `answerSpec`/`explanation` trước khi nộp.

## Acceptance

`mvn -q -pl services/learning-service -am test` xanh. Test review set cũ vẫn xanh sau khi tách `ItemGrading`.

## Verification

Files changed: `services/learning-service` (V4 migration, Content client, practice catalog/attempt API, one-way clearance, review selection, topic-test gate, and focused tests), `docs/contracts/lesson-learning-v1.md`, and the learning-service README. Focused unit/MVC tests: 38 passed; focused integration tests: 24 passed with Docker. The single full-suite run executed 211 tests with 2 failed assertions (the client DTO gained `hint`; schema version/table count advanced). After correcting those assertions, only the two failing classes were rerun: 24 passed, 0 failed. Current Surefire reports total 211 passed, 0 failed, 0 skipped; the full command was not rerun.
