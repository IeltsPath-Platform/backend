# P5 – Learning: thang ôn tập (PRACTICE → THEORY → PRACTICE → SKIPPED)

Service: `learning-service`. Phụ thuộc: P2 (block `knowledgePointIds`), P3 (review có skill), P4 (`ReviewTrigger`,
`ItemGrading`, practice attempts).

## 1. Luật (nguồn sự thật cho test)

Một `ReviewItem` cho một KP có `stage ∈ {PRACTICE, THEORY}` khi `status = PENDING`.

### 1.1 Stage ban đầu khi tạo review (`ReviewRule.initialStage`)

Review chỉ được tạo từ Practice (P4 luật 8) hoặc assessment (luật mastery cũ); hoàn thành lesson không tạo review.
Xét theo thứ tự, dòng đầu khớp thì dùng:

| Nguồn tạo review (`ReviewTrigger`) | Stage ban đầu | `theory_reason` |
| --- | --- | --- |
| `PRACTICE(kpPercent)` với tỉ lệ đúng **của KP trong attempt** < 0.40 | THEORY | `LOW_SCORE` |
| `PRACTICE` hoặc `ASSESSMENT`, và KP **đã sai** trong first-submission bài tập của lesson `review.lessonId` (`ExerciseSubmissionLog.firstResponses`, đọc một lần cho cả tập KP) | THEORY | `WRONG_IN_LESSON` |
| còn lại | PRACTICE | – |

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

- `GET /reviews/{id}` giao set như hiện tại, nhưng chọn package (D11):
  - `exclude` = mọi package **đã lộ** với learner = package của review set đã giao (`reviews.assignedPackageIds`,
    giao mà chưa nộp vẫn tính vì đã thấy đề) ∪ `PracticeAttemptRepository.revealedPackageIds(userId)` (P4: practice
    đã nộp ∪ review set đã nộp);
  - gọi `searchPracticeSets(kp, exclude, MIN_SET_QUESTIONS, preferredLessonId = review.lessonId)`, lấy phần tử đầu;
  - **bỏ** nhánh fallback `PackageRotation.leastRecentlyUsed` (không dùng lại package). Không còn package ⇒
    `SKIPPED` như hiện tại. Nếu `PackageRotation` không còn caller nào thì xoá class và test của nó.
- Item trả kèm `hint`. Sau khi nộp (đạt hay trượt) luôn trả `correctAnswer`, `explanation`, transcript (D9, D11).
- `POST /reviews/{id}/theory-check` ⇒ 409 `THEORY_NOT_REQUIRED`.

### 1.5 Không đổi

Luật tạo review từ assessment (mastery < 0.6, có practice set, KP sai, chưa có review chờ) và cách chọn `lessonId` cho
review assessment (lesson hoàn thành sớm nhất dạy KP); review Practice dùng lesson của attempt (P4). Unique pending per (user, KP), review khoá theo skill (P3). Nhiều KP sai ⇒ nhiều review; danh
sách `GET /reviews?status=PENDING` sắp cũ nhất trước để learner ôn lần lượt.

## 2. Migration `V5__review_ladder.sql`

```sql
ALTER TABLE review_items ADD COLUMN stage VARCHAR(10) NOT NULL DEFAULT 'PRACTICE'
    CHECK (stage IN ('PRACTICE', 'THEORY'));
ALTER TABLE review_items ADD COLUMN theory_reason VARCHAR(30)
    CHECK (theory_reason IN ('SECOND_FAIL', 'LOW_SCORE', 'WRONG_IN_LESSON'));
ALTER TABLE review_items ADD COLUMN theory_completed_count INTEGER NOT NULL DEFAULT 0;

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

Dữ liệu cũ (`trigger_kind` để NULL – cột thêm ở V4):
- review PENDING có set mở ⇒ giữ `PRACTICE`;
- review PENDING không có set mở và đã có ≥ 1 set trượt ⇒ `stage = 'THEORY', theory_reason = 'SECOND_FAIL'`
  (kể cả khi đã trượt 2 set theo luật cũ: learner được ôn lý thuyết một lần, set kế tiếp trượt sẽ SKIPPED).

## 3. Lý thuyết theo KP và quick-check

Service mới `application/service/TheoryFocus` (logic chọn block thuần nên đặt ở `domain/service/TheoryBlockSelector`):

- **Theory blocks**: từ `content.getLesson(review.lessonId)`, lấy block `TEXT` có `textContent` và
  `knowledgePointIds` chứa KP, giữ `sortOrder`. Rỗng ⇒ fallback mọi block TEXT (đúng hành vi cũ ở
  `GetReviewUseCase`), `theoryScope = LESSON_FALLBACK`; ngược lại `theoryScope = KNOWLEDGE_POINT`. Không bao giờ đưa
  ASSET vào (transcript Listening).
- **Quick-check**: tối đa **3** câu hỏi từ block EXERCISE không phải ESSAY của lesson đó, câu có `knowledgePointIds`
  chứa KP, theo thứ tự block rồi câu. Danh sách là **xác định** (cùng input ⇒ cùng câu) để `theory-check` chấm lại
  được mà không cần lưu trước. Không có câu ⇒ `quickCheck = []`, theory-check nhận `answers = {}` như "đã đọc xong".
- Câu hỏi quick-check không kèm đáp án; có `hint`.

## 4. Code

| Lớp | Việc |
| --- | --- |
| `domain/vo/ReviewStage`, `domain/vo/TheoryReason`, `domain/vo/ReviewTrigger` | enum / record (`ReviewTrigger` có `kind` PRACTICE|ASSESSMENT + `kpPercent` nullable; `ReviewTrigger` tạo ở P4). |
| `domain/service/ReviewRule` | `MAX_FAILED_REVIEW_SETS = 2`; `initialStage(trigger, wrongInLesson)`; `reevaluate` trả stage/reason cho mỗi review mới. |
| `domain/aggregate/ReviewItem` | field `stage`, `theoryReason`, `theoryCompletedCount`, `triggerKind`; `assignSet` chỉ khi PRACTICE (`IllegalStateException` ngược lại); `recordSetResult(setId, requestId, correct, total)` theo 1.2; `completeTheory()` theo 1.3. Javadoc cập nhật. |
| `domain/entity/ReviewSet` | thêm `correct`, `total`. |
| `JdbcReviewItemRepository` | đọc/ghi cột mới; lưu `review_theory_checks`. |
| `application/service/ReviewReevaluation` | nhận `ReviewTrigger`; với PRACTICE/ASSESSMENT tính `wrongInLesson` qua `ExerciseSubmissionLog.firstResponses(userId, lessonId)`. Caller còn lại: `ApplyAssessmentResultUseCase` (ASSESSMENT). Review từ Practice tính stage qua cùng `ReviewRule.initialStage` trong `SubmitPracticeAttemptUseCase` (kpPercent của từng KP). |
| `GetReviewUseCase` | rẽ nhánh theo stage (1.3 / 1.4); chọn package theo 1.4 (dùng `PracticeAttemptRepository.revealedPackageIds`). |
| `SubmitReviewUseCase` | 409 `THEORY_REQUIRED` khi THEORY; luôn reveal; lưu correct/total; response thêm `stage`, `failedSets`. |
| `SubmitTheoryCheckUseCase` (mới) | `@Transactional`, `LearnerLock`, idempotent theo `requestId`; dùng `ItemGrading`; không ghi evidence. |
| `ReviewController` + DTO | route mới; `ReviewResponse` thêm field mục 5. |

## 5. API public

`GET /api/learning/reviews/{reviewId}` – DTO hiện tại là
`ReviewResponse(reviewId, reviewStatus, lessonId, theory: List<String>, set{reviewSetId, packageId, packageVersionId,
passage, audio, questions})`. **Giữ nguyên mọi field và kiểu**, chỉ thêm:

```json
{
  "reviewId": "…", "reviewStatus": "PENDING", "lessonId": "…",
  "theory": ["đoạn lý thuyết của KP …"],
  "set": null,
  "knowledgePointId": "…", "skill": "READING",
  "stage": "THEORY", "theoryReason": "SECOND_FAIL", "theoryScope": "KNOWLEDGE_POINT",
  "failedSets": 1, "maxFailedSets": 2,
  "quickCheck": [ {"questionVersionId": "…", "sortOrder": 1, "stem": "…", "options": [], "hint": "…"} ]
}
```

- `theory` vẫn là mảng chuỗi, luôn có ở cả hai stage (giờ chỉ gồm TEXT của KP, hoặc fallback cả lesson).
- Ở PRACTICE: `quickCheck = []`, `set` như cũ, câu hỏi trong `set.questions` có `hint`.
- Ở THEORY: `set = null`.

`POST /api/learning/reviews/{reviewId}/theory-check` body `{requestId, answers}` (`answers` cùng shape mảng như lesson
submission, đủ mọi câu quick-check; không có câu ⇒ `[]`) ⇒
`{reviewId, correct, total, results:[{questionVersionId, correct, correctAnswer, explanation, hint:null}], stage:"PRACTICE"}`.

`POST /api/learning/reviews/{reviewId}/submissions` – DTO hiện tại
`ReviewSubmissionResponse(reviewStatus, results, transcript)`. Giữ nguyên, thêm `stage`, `failedSets`. `results[*]`
luôn là dạng `SolvedAnswer` (có `correctAnswer`, `explanation`) kể cả khi trượt; `transcript` luôn có với set Listening.

## 6. Test

Unit (`ReviewItemTest`, `ReviewRuleTest`, `TheoryBlockSelectorTest`):

- bảng 1.1 đủ 4 dòng; bảng 1.2 (đạt → DONE; trượt lần 1 → THEORY/SECOND_FAIL; trượt < 40% → LOW_SCORE;
  trượt lần 2 → SKIPPED);
- `assignSet` khi THEORY ném lỗi; `completeTheory` khi PRACTICE ném lỗi;
- chọn block theo KP; fallback khi không có mapping; quick-check tối đa 3, bỏ ESSAY, thứ tự xác định.

Integration `RemediationLadderIntegrationTest` (stub Content có lesson 2 KP với block→KP, 3 practice set cho KP A):

1. **Fail 1 → set có hint**: practice attempt lần đầu 50%, KP A đúng 50% (mastery vẫn ≥ 0.6, KP A không sai trong lesson) ⇒ review PRACTICE; GET review có set, item
   có `hint`; lesson cùng skill bị REVIEW_REQUIRED; lesson skill khác vào được.
2. **Fail 2 → lý thuyết đúng KP**: nộp set 50% ⇒ THEORY/SECOND_FAIL; GET review chỉ trả block gắn KP A (không có block
   KP B); có ≤ 3 câu quick-check KP A; submissions ⇒ 409 THEORY_REQUIRED.
3. theory-check sai hết ⇒ vẫn về PRACTICE, có `explanation`, không thêm `kp_evidence`; idempotent theo requestId.
4. **Fail 3 → SKIPPED**: set tiếp theo trượt ⇒ SKIPPED; lesson cùng skill mở lại.
5. **< 40%**: practice attempt có KP A đúng 1/4 ⇒ review KP A tạo ở THEORY/LOW_SCORE; KP B đúng 3/4 cùng attempt ⇒ không tạo review cho B.
6. **Fast-track**: KP A sai ở first-submission bài tập lesson, sau đó practice 60% ⇒ review THEORY/WRONG_IN_LESSON.
7. **Đạt**: set ≥ 70% ⇒ DONE, response có lời giải.
8. **Nhiều KP**: practice sai cả A và B ⇒ 2 review; `GET /reviews?status=PENDING` cũ nhất trước.
9. Không dùng lại package: set ôn ưu tiên package gắn lesson; **không bao giờ** giao package đã nộp ở practice hoặc
   đã giao ở review trước; hết package ⇒ review SKIPPED (thay test LRU cũ).
10. Không lộ trước khi nộp: `GET /reviews/{id}`, `GET /practice-attempts/{id}` (chưa nộp), quick-check không chứa
    `correctAnswer`, `explanation`, `answerSpec`, transcript.

Cập nhật test cũ đang kỳ vọng `MAX_FAILED_REVIEW_SETS = 3` hoặc "trượt chưa thấy lời giải" – đây là thay đổi chủ
đích (D8, D9); ghi danh sách test đã sửa vào Verification.

## Acceptance

`mvn -q -pl services/learning-service -am test` xanh; 10 kịch bản trên được phủ bởi unit hoặc integration (được gộp
nhiều kịch bản vào một test method).

## Verification

(Codex điền.)
