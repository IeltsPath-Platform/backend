# P4 – Learning: catalog Practice và practice attempt

Service: `learning-service`. Phụ thuộc: P2 (internal `lessons/{id}/practice-sets`), P3 (skill gate).

## 1. Luật nghiệp vụ

1. Practice của lesson chỉ mở khi `lesson_progress.completed_at IS NOT NULL` ⇒ ngược lại 409 `PRACTICE_LOCKED`.
2. Có review PENDING cùng skill (hoặc skill NULL) ⇒ 409 `REVIEW_REQUIRED` (giống lesson): learner phải ôn trước.
3. Entitlement: package có `requiredFeatureKey` ⇒ kiểm qua `AccessClient` như lesson/topic premium hiện có
   (409/403 theo đúng mã lỗi đang dùng cho premium – Codex tra `AccessUnavailableException`).
4. Mỗi lần "bắt đầu" tạo một attempt gắn `package_version_id` hiện tại. Mỗi (user, package) chỉ có **một attempt mở**
   (chưa nộp); bắt đầu lại khi đang mở ⇒ trả attempt đang mở (idempotent).
5. Nộp: chấm bằng `AnswerSpecGrader` như review set; `passed = PassMark` (≥ 70%). `percent` = đúng / tổng.
6. Evidence: **chỉ attempt nộp đầu tiên của mỗi (user, package)** ghi `kp_evidence` source `practice_set`,
   `source_reference_id` = attempt id, mỗi item × mỗi KP mapping (giống `SubmitReviewUseCase`).
7. Sau khi nộp: response luôn có `correctAnswer`, `explanation`, transcript (nếu có) cho mọi item.
8. Sau khi ghi evidence: gọi `ReviewReevaluation.execute(userId, consideredKps, wrongKps)` với `consideredKps` = KP của
   package, `wrongKps` = KP có ít nhất một item sai. Truyền thêm ngữ cảnh `ReviewTrigger.practice(percent)` để P5 quyết
   định stage ban đầu (P4 tạo kiểu dữ liệu, P5 dùng). Attempt **không phải lần đầu** không ghi evidence nhưng vẫn gọi
   reevaluate với `wrongKps` (mastery không đổi nên chỉ tạo review nếu mastery đã < 0.6).
9. Practice không bắt buộc để mở topic test (D7).

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

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check
    CHECK (source IN ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set'));
```

## 3. Code

| Lớp | Việc |
| --- | --- |
| `application/port/LearningContentClient` | thêm `List<PracticeSetSummary> lessonPracticeSets(UUID lessonId)`; thêm `preferredLessonId` vào `searchPracticeSets`. Cài ở `infrastructure/client`. |
| `domain/aggregate/PracticeAttempt` | `start(...)`, `acceptsAnswers(requestId)`, `recordResult(correct, total, countedAsEvidence)`; bất biến: không nộp 2 lần với request khác nhau (trả 409 `ATTEMPT_ALREADY_SUBMITTED`), cùng `requestId` ⇒ trả lại `response` đã lưu. |
| `domain/repository/PracticeAttemptRepository` + `infrastructure/persistence/JdbcPracticeAttemptRepository` | lưu/đọc; `hasCountedEvidence(userId, packageId)`; `latestByLesson(userId, lessonId)`; `packageIdsUsed(userId)` (cho rotation ở P5). |
| `application/service/PracticeAccess` | luật 1–3, dùng lại `LessonAccess` cho lesson và gate. |
| `application/service/ItemGrading` (tách từ `SubmitReviewUseCase`) | chấm một `PackageVersion` theo `AnswerSheet`, trả item kết quả + KP sai; dùng chung cho review set, practice, quick-check (P5). Không đổi output của review. |
| `GetLessonPracticeSetsUseCase` | danh sách practice của lesson + trạng thái. |
| `StartPracticeAttemptUseCase` | luật 1–4. |
| `GetPracticeAttemptUseCase` | trả câu hỏi (không đáp án; có `hint`), hoặc kết quả nếu đã nộp. |
| `SubmitPracticeAttemptUseCase` | luật 5–8, `@Transactional`, `LearnerLock`. |
| `api/controller/PracticeController` + DTO request/response | route mục 4. |

## 4. API public

| Route | Mô tả |
| --- | --- |
| `GET /api/learning/lessons/{id}/practice-sets` | `{lessonId, skill, lessonCompleted, items:[{packageId, code, title, questionCount, status, bestPercent, lastAttemptId}]}`. `status`: `LOCKED` (lesson chưa xong hoặc review chờ cùng skill), `AVAILABLE`, `IN_PROGRESS` (có attempt mở), `PASSED` (có attempt đạt), `ATTEMPTED` (đã làm, chưa đạt). Không lỗi khi lesson chưa xong – trả LOCKED. |
| `POST /api/learning/lessons/{id}/practice-attempts` body `{packageId}` | 201 `{attemptId, packageId, packageVersionId, sections/items (không đáp án, có hint)}`; lỗi 409 `PRACTICE_LOCKED` / `REVIEW_REQUIRED`, 404 nếu package không thuộc lesson. |
| `GET /api/learning/practice-attempts/{id}` | như trên, hoặc kết quả nếu đã nộp. 404 nếu không phải của user. |
| `POST /api/learning/practice-attempts/{id}/submissions` body `{requestId, answers}` | `{attemptId, correct, total, percent, passed, countedAsEvidence, items:[{itemId, correct, correctAnswer, explanation, transcript?}], reviewsCreated:[{reviewId, knowledgePointId, stage}]}` |

Gateway: các route nằm dưới `/api/learning/**` đã route sẵn – Codex kiểm tra config gateway, không cần thêm nếu đã có.

## 5. Test

- Unit: `PracticeAttempt` (idempotent theo requestId, không nộp 2 lần).
- Integration:
  1. Lesson chưa xong ⇒ catalog LOCKED, start 409 PRACTICE_LOCKED.
  2. Xong lesson ⇒ AVAILABLE ⇒ start ⇒ submit 100% ⇒ PASSED, evidence `practice_set` ghi đúng số dòng.
  3. Submit lần 2 cùng requestId trả cùng body; requestId khác ⇒ 409.
  4. Attempt thứ hai cùng package ⇒ `countedAsEvidence=false`, không thêm `kp_evidence`.
  5. Submit sai đủ để mastery < 0.6 ⇒ review PENDING được tạo, `reviewsCreated` không rỗng; practice khác cùng skill
     ⇒ LOCKED; lesson skill khác vẫn vào được.
- MVC: không lộ `answerSpec`/`explanation` trước khi nộp.

## Acceptance

`mvn -q -pl services/learning-service -am test` xanh. Test review set cũ vẫn xanh sau khi tách `ItemGrading`.

## Verification

(Codex điền.)
