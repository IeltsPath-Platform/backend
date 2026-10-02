# P5 – Learning: thang ôn tập (PRACTICE → THEORY → PRACTICE → SKIPPED)

Service: `learning-service`. Phụ thuộc: P2 (block `knowledgePointIds`), P3 (review có skill), P4 (`ReviewTrigger`,
`ItemGrading`, practice attempts).

## 1. Luật (nguồn sự thật cho test)

Một `ReviewItem` cho một KP có `stage ∈ {PRACTICE, THEORY}` khi `status = PENDING`.

### 1.1 Stage ban đầu khi tạo review (`ReviewRule.initialStage`)

| Nguồn tạo review (`ReviewTrigger`) | Stage ban đầu | `theory_reason` |
| --- | --- | --- |
| `LESSON` (hoàn thành lesson, KP sai ở bài tập lesson) | PRACTICE | – (learner vừa đọc lý thuyết; lần sai trong lesson là "lần trượt 1") |
| `PRACTICE(percent)` với `percent < 0.40` | THEORY | `LOW_SCORE` |
| `PRACTICE` hoặc `ASSESSMENT`, và KP **đã sai** trong first-submission bài tập của lesson dạy KP (`review.lessonId`, dùng `ExerciseSubmissionLog.firstResponses`) | THEORY | `WRONG_IN_LESSON` |
| `PRACTICE` / `ASSESSMENT` còn lại | PRACTICE | – |

### 1.2 Khi nộp set ôn (`ReviewItem.recordSetResult(setId, requestId, correct, total)`)

- đạt (PassMark ≥ 70%) ⇒ `DONE`;
- trượt ⇒ `failedSets++`; nếu `failedSets >= MAX_FAILED_REVIEW_SETS (= 2)` ⇒ `SKIPPED`; ngược lại ⇒ `stage = THEORY`,
  `theory_reason = percent < 0.40 ? LOW_SCORE : SECOND_FAIL`.

Đếm lần trượt từ góc nhìn learner: lần trượt gây ra review = 1, set ôn trượt đầu = 2 (⇒ lý thuyết), set ôn trượt
sau lý thuyết = 3 (⇒ SKIPPED). Review bắt đầu ở THEORY (fast-track / <40%) cũng dừng sau 2 set ôn trượt.

### 1.3 Ở stage THEORY

- `GET /reviews/{id}` **không** giao set mới; trả lý thuyết + quick-check (mục 3).
- `POST /reviews/{id}/submissions` ⇒ 409 `THEORY_REQUIRED`.
- `POST /reviews/{id}/theory-check` ⇒ chấm quick-check, **luôn** chuyển về PRACTICE, `theory_completed_count++`.
  Không ghi `kp_evidence`. Không ảnh hưởng `failedSets`.

### 1.4 Ở stage PRACTICE

- `GET /reviews/{id}` giao set như hiện tại, nhưng chọn package theo thứ tự: (1) gắn `preferredLessonId =
  review.lessonId`, (2) chưa từng dùng trong review **và** practice attempt của learner, (3) LRU (`PackageRotation`).
  Không có package ⇒ `SKIPPED` như hiện tại.
- Item trả kèm `hint`. Sau khi nộp (đạt hay trượt) luôn trả `correctAnswer`, `explanation`, transcript (D9).
- `POST /reviews/{id}/theory-check` ⇒ 409 `THEORY_NOT_REQUIRED`.

### 1.5 Không đổi

Ngưỡng tạo review (mastery < 0.6, có practice set, KP sai, chưa có review chờ), unique pending per (user, KP), cách
chọn `lessonId` (lesson hoàn thành sớm nhất dạy KP), review khoá theo skill (P3). Nhiều KP sai ⇒ nhiều review; danh
sách `GET /reviews?status=PENDING` sắp cũ nhất trước để learner ôn lần lượt.

## 2. Migration `V5__review_ladder.sql`

```sql
ALTER TABLE review_items ADD COLUMN stage VARCHAR(10) NOT NULL DEFAULT 'PRACTICE'
    CHECK (stage IN ('PRACTICE', 'THEORY'));
ALTER TABLE review_items ADD COLUMN theory_reason VARCHAR(30)
    CHECK (theory_reason IN ('SECOND_FAIL', 'LOW_SCORE', 'WRONG_IN_LESSON'));
ALTER TABLE review_items ADD COLUMN theory_completed_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE review_items ADD COLUMN trigger_kind VARCHAR(20)
    CHECK (trigger_kind IN ('LESSON', 'PRACTICE', 'ASSESSMENT'));

ALTER TABLE review_sets ADD COLUMN correct_count INTEGER;
ALTER TABLE review_sets ADD COLUMN total_count INTEGER;

CREATE TABLE review_theory_checks (
    id UUID PRIMARY KEY,
    review_item_id UUID NOT NULL REFERENCES review_items(id),
    user_id UUID NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    question_version_ids UUID[] NOT NULL,
    answers JSONB NOT NULL,
    correct_count INTEGER NOT NULL,
    total_count INTEGER NOT NULL,
    response JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_review_theory_checks_item ON review_theory_checks (review_item_id, submitted_at);
```

Dữ liệu cũ (`trigger_kind` để NULL):
- review PENDING có set mở ⇒ giữ `PRACTICE`;
- review PENDING không có set mở và đã có ≥ 1 set trượt ⇒ `stage = 'THEORY', theory_reason = 'SECOND_FAIL'`
  (kể cả khi đã trượt 2 set theo luật cũ: learner được ôn lý thuyết một lần, set kế tiếp trượt sẽ SKIPPED).

## 3. Lý thuyết theo KP và quick-check

Service mới `application/service/TheoryFocus` (logic chọn block thuần nên đặt ở `domain/service/TheoryBlockSelector`):

- **Theory blocks**: từ `content.lesson(review.lessonId)`, lấy block `TEXT`/`ASSET`/`VOCABULARY` có
  `knowledgePointIds` chứa KP, giữ `sortOrder`. Rỗng ⇒ fallback mọi block TEXT/ASSET (hành vi cũ), `theoryScope =
  LESSON_FALLBACK`; ngược lại `theoryScope = KNOWLEDGE_POINT`.
- **Quick-check**: tối đa **3** câu hỏi từ block EXERCISE không phải ESSAY của lesson đó, câu có `knowledgePointIds`
  chứa KP, theo thứ tự block rồi câu. Danh sách là **xác định** (cùng input ⇒ cùng câu) để `theory-check` chấm lại
  được mà không cần lưu trước. Không có câu ⇒ `quickCheck = []`, theory-check nhận `answers = {}` như "đã đọc xong".
- Câu hỏi quick-check không kèm đáp án; có `hint`.

## 4. Code

| Lớp | Việc |
| --- | --- |
| `domain/vo/ReviewStage`, `domain/vo/TheoryReason`, `domain/vo/ReviewTrigger` | enum / record (`ReviewTrigger` có `kind` + `percent` nullable). |
| `domain/service/ReviewRule` | `MAX_FAILED_REVIEW_SETS = 2`; `initialStage(trigger, wrongInLesson)`; `reevaluate` trả stage/reason cho mỗi review mới. |
| `domain/aggregate/ReviewItem` | field `stage`, `theoryReason`, `theoryCompletedCount`, `triggerKind`; `assignSet` chỉ khi PRACTICE (`IllegalStateException` ngược lại); `recordSetResult(setId, requestId, correct, total)` theo 1.2; `completeTheory()` theo 1.3. Javadoc cập nhật. |
| `domain/entity/ReviewSet` | thêm `correct`, `total`. |
| `JdbcReviewItemRepository` | đọc/ghi cột mới; lưu `review_theory_checks`. |
| `application/service/ReviewReevaluation` | nhận `ReviewTrigger`; với PRACTICE/ASSESSMENT tính `wrongInLesson` qua `ExerciseSubmissionLog.firstResponses(userId, lessonId)`. Cập nhật mọi caller: `CompleteLessonUseCase`, `SubmitLessonExerciseUseCase` (LESSON), `SubmitPracticeAttemptUseCase` (PRACTICE + percent), `ApplyAssessmentResultUseCase` (ASSESSMENT), `SubmitLessonEssayUseCase` nếu có gọi. |
| `GetReviewUseCase` | rẽ nhánh theo stage (1.3 / 1.4); rotation mới (dùng `PracticeAttemptRepository.packageIdsUsed`). |
| `SubmitReviewUseCase` | 409 `THEORY_REQUIRED` khi THEORY; luôn reveal; lưu correct/total; response thêm `stage`, `status`, `failedSets`. |
| `SubmitTheoryCheckUseCase` (mới) | `@Transactional`, `LearnerLock`, idempotent theo `requestId`; dùng `ItemGrading`; không ghi evidence. |
| `ReviewController` + DTO | route mới; `ReviewResponse` thêm field mục 5. |

## 5. API public

`GET /api/learning/reviews/{reviewId}`:

```json
{
  "reviewId": "…", "knowledgePointId": "…", "knowledgePointName": "…", "skill": "READING",
  "lessonId": "…", "status": "PENDING", "stage": "THEORY", "theoryReason": "SECOND_FAIL",
  "failedSets": 1, "maxFailedSets": 2,
  "theory": { "scope": "KNOWLEDGE_POINT", "blocks": [ {"blockId": "…", "blockType": "TEXT", "sortOrder": 1, "textContent": "…", "asset": null} ] },
  "quickCheck": [ {"questionVersionId": "…", "stem": "…", "options": [], "hint": "…"} ],
  "set": null
}
```

Ở PRACTICE: `theory = null`, `quickCheck = []`, `set = {setId, packageId, packageVersionId, sections/items có hint}`.
Field cũ của response giữ nguyên tên; nếu response cũ đã có `theory` dạng khác, giữ tên cũ và thêm `theoryScope`
thay vì đổi shape – Codex đối chiếu `ReviewResponse` hiện tại và ghi quyết định vào Verification.

`POST /api/learning/reviews/{reviewId}/theory-check` body `{requestId, answers: {questionVersionId: answer}}` ⇒
`{reviewId, correct, total, items:[{questionVersionId, correct, correctAnswer, explanation}], stage: "PRACTICE"}`.

`POST /api/learning/reviews/{reviewId}/submissions` response thêm `stage`, `status`, `failedSets`; `items[*]`
luôn có `correctAnswer`, `explanation`.

## 6. Test

Unit (`ReviewItemTest`, `ReviewRuleTest`, `TheoryBlockSelectorTest`):

- bảng 1.1 đủ 4 dòng; bảng 1.2 (đạt → DONE; trượt lần 1 → THEORY/SECOND_FAIL; trượt < 40% → LOW_SCORE;
  trượt lần 2 → SKIPPED);
- `assignSet` khi THEORY ném lỗi; `completeTheory` khi PRACTICE ném lỗi;
- chọn block theo KP; fallback khi không có mapping; quick-check tối đa 3, bỏ ESSAY, thứ tự xác định.

Integration `RemediationLadderIntegrationTest` (stub Content có lesson 2 KP với block→KP, 3 practice set cho KP A):

1. **Fail 1 → set có hint**: practice attempt 50% sai KP A (mastery < 0.6) ⇒ review PRACTICE; GET review có set, item
   có `hint`; lesson cùng skill bị REVIEW_REQUIRED; lesson skill khác vào được.
2. **Fail 2 → lý thuyết đúng KP**: nộp set 50% ⇒ THEORY/SECOND_FAIL; GET review chỉ trả block gắn KP A (không có block
   KP B); có ≤ 3 câu quick-check KP A; submissions ⇒ 409 THEORY_REQUIRED.
3. theory-check sai hết ⇒ vẫn về PRACTICE, có `explanation`, không thêm `kp_evidence`; idempotent theo requestId.
4. **Fail 3 → SKIPPED**: set tiếp theo trượt ⇒ SKIPPED; lesson cùng skill mở lại.
5. **< 40%**: practice attempt 20% ⇒ review tạo ở THEORY/LOW_SCORE.
6. **Fast-track**: KP A sai ở first-submission bài tập lesson, sau đó practice 60% ⇒ review THEORY/WRONG_IN_LESSON.
7. **Đạt**: set ≥ 70% ⇒ DONE, response có lời giải.
8. **Nhiều KP**: practice sai cả A và B ⇒ 2 review; `GET /reviews?status=PENDING` cũ nhất trước.
9. Rotation: set ôn ưu tiên package gắn lesson và chưa từng làm trong practice.

Cập nhật test cũ đang kỳ vọng `MAX_FAILED_REVIEW_SETS = 3` hoặc "trượt chưa thấy lời giải" – đây là thay đổi chủ
đích (D8, D9); ghi danh sách test đã sửa vào Verification.

## Acceptance

`mvn -q -pl services/learning-service -am test` xanh; 9 kịch bản trên có test.

## Verification

(Codex điền.)
