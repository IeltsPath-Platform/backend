# Phase 2: Attempt — sinh bài, chấm điểm, tiến độ, mở khóa, hoàn thành

Status: complete for backend delivery; automated verification and the live Gateway learner flow passed (239 checks, 50 XP after two units plus replay, streak 1). See [implementation progress](./reports/implementation-progress.md) and [backend handoff](./reports/backend-handoff.md).

Service: library-service. Phụ thuộc: phase 1. Đọc trước: [plan.md](./plan.md) mục 7.

## 1. Mục tiêu

Học viên bắt đầu một unit, nhận danh sách bước đã sinh sẵn, nộp đáp án từng bước (server chấm), thoát ra và quay lại (kể cả máy khác) vẫn tiếp tục đúng bước, rồi hoàn thành. Server tính tiến độ và mở khóa unit kế tiếp. Phần thưởng (XP, streak, tim) thêm ở phase 3; **sau phase 2 chưa bật cho mobile**: `Completion.reward` để vắng mặt (`@JsonInclude(NON_NULL)`).

## 2. Migration `V40__vocabulary_study_attempts.sql`

```sql
CREATE TABLE vocabulary_study_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id),
    revision INT NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('FIRST_COMPLETION', 'REPLAY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'EXPIRED')),
    started_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    start_request_id UUID NOT NULL,
    graded_step_count INT NOT NULL,
    first_try_correct_count INT NOT NULL DEFAULT 0,
    completion JSONB,                    -- Completion đã trả, để trả lại y nguyên khi gọi lại
    UNIQUE (user_id, start_request_id)
);
-- Mỗi người chỉ có một attempt đang làm cho mỗi unit
CREATE UNIQUE INDEX uq_vocab_study_attempt_active
    ON vocabulary_study_attempts (user_id, unit_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_vocab_study_attempt_user_topic ON vocabulary_study_attempts (user_id, topic_id);

CREATE TABLE vocabulary_study_attempt_steps (
    attempt_id UUID NOT NULL REFERENCES vocabulary_study_attempts(id) ON DELETE CASCADE,
    step_id UUID NOT NULL,
    step_order INT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('WORD', 'CHOICE', 'MATCH', 'FILL')),
    graded BOOLEAN NOT NULL,
    payload JSONB NOT NULL,              -- phần học viên được thấy (đúng hình StudyStep trong hợp đồng)
    answer_key JSONB NOT NULL,           -- đáp án chuẩn + giải thích; KHÔNG BAO GIỜ trả ra trước khi nộp
    completed_at TIMESTAMPTZ,
    first_try_correct BOOLEAN,           -- null cho đến lần nộp đầu của bước chấm điểm
    PRIMARY KEY (attempt_id, step_id),
    UNIQUE (attempt_id, step_order)
);

CREATE TABLE vocabulary_study_submissions (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    step_id UUID NOT NULL,
    user_id UUID NOT NULL,
    request_id UUID NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    answer JSONB NOT NULL,
    outcome VARCHAR(12) NOT NULL CHECK (outcome IN ('ACKNOWLEDGED', 'CORRECT', 'INCORRECT')),
    first_submission BOOLEAN NOT NULL,
    result JSONB NOT NULL,               -- StepResult đã trả
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, request_id),
    FOREIGN KEY (attempt_id, step_id) REFERENCES vocabulary_study_attempt_steps(attempt_id, step_id) ON DELETE CASCADE
);
CREATE INDEX idx_vocab_study_submission_step ON vocabulary_study_submissions (attempt_id, step_id, created_at);

CREATE TABLE vocabulary_study_unit_progress (
    user_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id),
    topic_id UUID NOT NULL,
    first_completed_at TIMESTAMPTZ NOT NULL,
    last_completed_at TIMESTAMPTZ NOT NULL,
    best_accuracy_percent INT NOT NULL,
    completed_revision INT NOT NULL,
    PRIMARY KEY (user_id, unit_id)
);
CREATE INDEX idx_vocab_study_progress_topic ON vocabulary_study_unit_progress (user_id, topic_id);
```

## 3. Các endpoint (controller `VocabularyStudyController`, `/api/learning-support/vocabulary-study`)

Xác thực bắt buộc, `userId` từ token. Gateway chưa có route này (phase 3 thêm), nên trong phase 2 test trực tiếp vào library-service.

| Endpoint | Thành công | Lỗi chính |
|---|---|---|
| `GET /progress?topicIds=a,b,c` | 200 `TopicProgress[]` | 400 nếu quá 100 id hoặc id sai |
| `POST /units/{unitId}/attempts` | 201 attempt mới, hoặc 200 attempt có sẵn | 404 `VOCAB_UNIT_NOT_FOUND`, 409 `VOCAB_UNIT_LOCKED`, `VOCAB_UNIT_COMPLETED`, `VOCAB_UNIT_NOT_COMPLETED` |
| `GET /attempts/{attemptId}` | 200 `Attempt` | 404 `ATTEMPT_NOT_FOUND` (cả khi attempt của người khác) |
| `POST /attempts/{attemptId}/steps/{stepId}/answers` | 200 `StepResult` | 404, 409 `STEP_OUT_OF_ORDER`, `ATTEMPT_EXPIRED`, `ATTEMPT_ALREADY_COMPLETED`, `IDEMPOTENCY_CONFLICT`; 422 `INVALID_ANSWER` |
| `POST /attempts/{attemptId}/complete` | 200 `Completion` | 404, 409 `ATTEMPT_NOT_COMPLETABLE`, `ATTEMPT_EXPIRED` |

Các mã lỗi mới `VOCAB_UNIT_COMPLETED`, `VOCAB_UNIT_NOT_COMPLETED`, `ATTEMPT_ALREADY_COMPLETED`, `STEP_NOT_FOUND` được thêm vào bảng lỗi của hợp đồng mục 10.

Tất cả đều dùng `StudyRequestException` (plan.md mục 7).

## 4. Bắt đầu attempt

Body: `{ "requestId": "uuid", "mode": "RESUME_OR_START" | "REPLAY" }`.

Trong một transaction có khóa người dùng:

1. Nếu đã có attempt với `(user_id, start_request_id)`: trả lại attempt đó (200).
2. Đọc unit đã phát hành (không có: 404). Lấy `published_revision`.
3. Tính trạng thái các unit của topic (mục 7). Unit `LOCKED` và không có attempt đang làm: `409 VOCAB_UNIT_LOCKED` với `details: { requiredUnitId }` (unit ngay trước nó).
4. Tìm attempt `IN_PROGRESS` của `(user, unit)`. Nếu `expires_at < now`: đặt `EXPIRED` (xem mục 6 về cách giữ thay đổi này), coi như không có.
5. Quyết định theo bảng:

| Yêu cầu | Unit chưa xong, không có attempt đang làm | Unit chưa xong, có attempt đang làm | Unit đã xong, không có attempt đang làm | Unit đã xong, có replay đang làm |
|---|---|---|---|---|
| `RESUME_OR_START` | tạo `FIRST_COMPLETION` | trả attempt đó | `409 VOCAB_UNIT_COMPLETED` | trả replay đó |
| `REPLAY` | `409 VOCAB_UNIT_NOT_COMPLETED` | `409 VOCAB_UNIT_NOT_COMPLETED` | tạo `REPLAY` | trả replay đó |

6. Tạo attempt mới: đọc các sense ACTIVE của revision đã phát hành, nạp bể đáp án nhiễu, chạy `ExerciseGenerator` (mục 5), ghi `attempts` (`expires_at = now + 14 ngày`, `graded_step_count`) và toàn bộ `attempt_steps`. Nếu sau khi bỏ sense INACTIVE còn dưới 4 từ: `409 VOCAB_UNIT_UNAVAILABLE`.

Phase 3 chèn thêm vào bước 6: attempt `FIRST_COMPLETION` cần ít nhất 1 tim.

Phản hồi `Attempt` đúng hình hợp đồng mục 6. Ví dụ rút gọn:

```json
{
  "attemptId": "6f0c…", "topicId": "7a10…", "unitId": "3d9f…", "revision": 2,
  "status": "IN_PROGRESS", "mode": "FIRST_COMPLETION",
  "startedAt": "2026-11-07T10:00:00Z", "expiresAt": "2026-11-21T10:00:00Z",
  "currentStepId": "b81a…", "completedStepIds": [],
  "steps": [
    { "stepId": "b81a…", "order": 1, "type": "WORD", "instructionVi": "Học từ mới",
      "imageUrl": null, "audioUrl": "https://…/homework.mp3",
      "word": { "vocabularySenseId": "a1b2…", "lemma": "homework", "vietnameseMeaning": "bài tập về nhà" } },
    { "stepId": "c92b…", "order": 9, "type": "CHOICE", "instructionVi": "Chọn nghĩa đúng của từ",
      "imageUrl": null, "audioUrl": null, "prompt": "homework",
      "options": [
        { "optionId": "o1…", "text": "kỳ thi", "imageUrl": null },
        { "optionId": "o2…", "text": "bài tập về nhà", "imageUrl": null },
        { "optionId": "o3…", "text": "học bổng", "imageUrl": null }
      ] }
  ],
  "completion": null
}
```

Lưu ý: `steps[].payload` không chứa cờ đáp án, không chứa `answer_key`. `GET attempt` trả toàn bộ steps (số bước vài chục, vài KB) kèm `currentStepId` và `completedStepIds`.

## 5. Bộ sinh bài `ExerciseGenerator` (lớp thuần, `domain/study`)

Đầu vào: danh sách từ của unit theo thứ tự tác giả (`StudyWordSnapshot`: senseId, itemId, lemma, partOfSpeech, vietnameseMeaning, englishDefinition, ipa, audio URL, image, câu ví dụ + dịch), bể đáp án nhiễu (các sense ACTIVE khác của cùng topic, tối đa 200), và `attemptId`. Ngẫu nhiên: `SplittableRandom(seed)` với `seed = attemptId.getMostSignificantBits() ^ attemptId.getLeastSignificantBits()`. Cùng attempt luôn cho cùng đề (test được). `stepId = UUID.nameUUIDFromBytes((attemptId + ":" + order).getBytes(UTF_8))`; `optionId` sinh từ cùng bộ ngẫu nhiên nên cũng ổn định.

Thứ tự bước cố định (đánh số `order` từ 1 liên tục):

1. **WORD** cho từng từ theo thứ tự tác giả. `audioUrl` = audio US nếu có, không thì UK. Không chấm điểm.
2. **CHOICE** cho từng từ, thứ tự đã xáo. Prompt là lemma. Phương án đúng là `vietnameseMeaning`; phương án nhiễu lấy ưu tiên từ nghĩa của các từ khác trong unit, rồi từ bể topic; mỗi nghĩa nhiễu phải khác (sau chuẩn hóa) nghĩa đúng và khác các phương án còn lại. Cố gắng đủ 4 phương án; chấp nhận 3 hoặc 2; ít hơn 2 thì bỏ bước này. Thứ tự phương án đã xáo.
3. **MATCH**: xáo từ, chia nhóm tối đa 5 cặp. Nếu nhóm cuối chỉ còn 1 từ, chuyển một từ từ nhóm trước sang để nhóm cuối có 2 (nhóm trước còn 4). Tổng dưới 2 từ thì không có MATCH. Cột trái là lemma, cột phải là nghĩa tiếng Việt; **hai cột có `optionId` độc lập** (không dùng chung id), cột phải đã xáo. Khóa đáp án là tập cặp `(leftOptionId, rightOptionId)`.
4. **FILL** cho từng từ có câu ví dụ chứa đúng từ, thứ tự đã xáo. Tìm lần xuất hiện đầu tiên của lemma trong câu ví dụ bằng regex không phân biệt hoa thường, ranh giới là ký tự không phải chữ/số: `(?<![\p{L}\p{N}])` + lemma (khoảng trắng trong lemma thành `\s+`, các ký tự đặc biệt được `Pattern.quote`) + `(?![\p{L}\p{N}])`. Không tìm thấy thì không có bước FILL cho từ đó. `sentenceBefore`/`sentenceAfter` là phần câu trước/sau chỗ khớp; `prompt` là nghĩa tiếng Việt của từ. `inputMode` là `TEXT` (kiểu `WORD_BANK` được hợp đồng bảo lưu nhưng v1 không sinh). Đáp án chấp nhận: văn bản đã khớp trong câu và lemma, đã chuẩn hóa.

Bước chấm điểm = CHOICE, MATCH, FILL. Mỗi bước MATCH tính là một bước chấm điểm.

`answer_key` mỗi bước chứa: đáp án chuẩn và `explanationVi`. Ví dụ giải thích: CHOICE `"'homework' nghĩa là 'bài tập về nhà'."`; FILL: câu đầy đủ và bản dịch; MATCH: các cặp từ.

Ví dụ cho unit 5 từ: 5 WORD (order 1–5), 5 CHOICE (6–10), 1 MATCH 5 cặp (11), tối đa 5 FILL (12–16). Số bước chấm điểm = 5 + 1 + số FILL.

## 6. Nộp đáp án

Body: `{ "requestId": "uuid", "answer": { … } }`. Các dạng `answer` (hợp đồng mục 6): `{type:'WORD', acknowledged:true}`, `{type:'CHOICE', optionId}`, `{type:'MATCH', pairs:[{leftOptionId,rightOptionId}]}`, `{type:'FILL_TEXT', text}`.

Thực hiện bằng `TransactionTemplate` (use case **không** để `@Transactional` ở mức method), vì có trạng thái phải được lưu dù request vẫn trả lỗi (mục hết hạn). Thứ tự:

1. Mở transaction, lấy khóa người dùng.
2. Tra `(user_id, request_id)` trong `submissions`. Có: hash khớp thì trả `result` đã lưu; khác thì `409 IDEMPOTENCY_CONFLICT`. Việc này nằm **trước** mọi kiểm tra trạng thái.
3. Đọc attempt thuộc người dùng (không có: 404). Đọc step (không có: `404 STEP_NOT_FOUND`).
4. Nếu attempt `IN_PROGRESS` mà `expires_at < now`: đặt `EXPIRED`, commit, **sau transaction** ném `409 ATTEMPT_EXPIRED`. Nếu đã `EXPIRED`: ném luôn. Nếu `COMPLETED`: `409 ATTEMPT_ALREADY_COMPLETED`.
5. Bước phải là bước hiện tại (bước đầu tiên chưa hoàn thành theo `step_order`), nếu không `409 STEP_OUT_OF_ORDER`.
6. (Phase 3: kiểm tra tim ở đây cho attempt `FIRST_COMPLETION` và bước chấm điểm.)
7. Kiểm tra dạng đáp án (`type` đúng loại bước, `optionId` thuộc bước, MATCH đủ mọi `leftOptionId` đúng một lần, `FILL_TEXT.text` 1–100 ký tự sau trim): sai thì `422 INVALID_ANSWER`, `details: { "field": "answer.optionId" }`; **không** ghi submission.
8. Chấm bằng `AnswerGrader` (mục 8). `first_submission = true` nếu chưa có submission nào của bước này. Ghi `submissions` (kèm `result`).
9. Đúng hoặc WORD: đặt `completed_at` cho bước. Với bước chấm điểm, nếu là lần nộp đầu thì ghi `first_try_correct` và cộng `first_try_correct_count` của attempt khi đúng.
10. Cập nhật `last_activity_at = now`, `expires_at = now + 14 ngày`.

`StepResult` (đúng hình hợp đồng mục 6). Ví dụ trả lời sai:

```json
{
  "attemptId": "6f0c…", "stepId": "c92b…", "submissionId": "5d77…",
  "outcome": "INCORRECT",
  "correctAnswer": { "type": "CHOICE", "optionId": "o2…" },
  "explanationVi": "'homework' nghĩa là 'bài tập về nhà'.",
  "stepCompleted": false, "completedStepCount": 8, "totalStepCount": 16,
  "nextStepId": "c92b…", "canComplete": false,
  "heartLost": false, "hearts": null
}
```

Quyết định cần ghi nhớ: khi sai, `correctAnswer` **được trả ra** (người học thấy đáp án rồi làm lại). Điều này không ảnh hưởng độ chính xác vì độ chính xác chỉ tính lần nộp đầu của mỗi bước. `heartLost` và `hearts` được điền ở phase 3; ở phase 2 trả `false` và `null`.

## 7. Tiến độ và mở khóa

`UnitUnlockRules` (lớp thuần). Đầu vào: danh sách unit đã phát hành, chưa lưu trữ của topic theo `sort_order`; các dòng `unit_progress` của người dùng; các attempt `IN_PROGRESS`.

Trạng thái từng unit, theo thứ tự ưu tiên:

1. `COMPLETED`: có dòng `unit_progress`. Không bao giờ bị khóa lại, kể cả khi sau này chèn unit mới phía trước.
2. `IN_PROGRESS`: có attempt `FIRST_COMPLETION` đang làm.
3. `AVAILABLE`: là unit đầu tiên của topic, hoặc unit liền trước (trong danh sách đã phát hành chưa lưu trữ) là `COMPLETED`.
4. `LOCKED`: còn lại.

Chỉ bắt đầu được attempt đầu cho unit `AVAILABLE` hoặc `IN_PROGRESS`. Một unit `COMPLETED` đang có replay vẫn là `COMPLETED`, nhưng `currentAttemptId` trỏ tới replay đó.

`GET /progress?topicIds=…` trả mảng `TopicProgress` (hình hợp đồng mục 5). Quy tắc từng trường:

- `prerequisiteUnitId`: unit liền trước, `null` cho unit đầu.
- `completedStepCount`/`totalStepCount`: từ attempt đang làm (0/0 nếu không có).
- `currentUnitId`: unit có attempt `FIRST_COMPLETION` đang làm; nếu không có thì unit `AVAILABLE` đầu tiên chưa hoàn thành; nếu tất cả xong thì `null`.
- `completedUnits`, `totalUnits`, `curriculumVersion` (0 nếu topic chưa có dòng), `bestAccuracyPercent`, `firstCompletedAt`, `lastCompletedAt` lấy từ `unit_progress`.
- Topic không có unit nào: trả phần tử với `totalUnits = 0`.

Hiệu năng: tối đa 100 topic mỗi lần; dùng **3 truy vấn gộp** (unit của các topic; tiến độ của người dùng theo các unit đó; attempt đang làm kèm số bước đã xong), không truy vấn riêng cho từng topic.

## 8. Chấm điểm `AnswerGrader` (lớp thuần)

- Chuẩn hóa chuỗi `normalize(s)`: trim, Unicode NFC, `toLowerCase(Locale.ROOT)`, gộp mọi chuỗi khoảng trắng thành một dấu cách. Không bỏ dấu câu và không bỏ dấu thanh.
- CHOICE: đúng khi `optionId` bằng `correctOptionId` của khóa.
- MATCH: đúng khi tập cặp gửi lên bằng đúng tập cặp trong khóa; thiếu, thừa hoặc trùng cặp là `422`, ghép sai là `INCORRECT`.
- FILL_TEXT: đúng khi `normalize(text)` thuộc tập đáp án chấp nhận đã chuẩn hóa.
- WORD: luôn `ACKNOWLEDGED`.

## 9. Hoàn thành

Body: `{ "requestId": "uuid" }`. Trong transaction có khóa:

1. Attempt đã `COMPLETED`: trả lại `completion` đã lưu (bất kể `requestId`), không ghi gì thêm.
2. Attempt `EXPIRED`: `409 ATTEMPT_EXPIRED`. Còn bước chưa xong: `409 ATTEMPT_NOT_COMPLETABLE`.
3. Tính: `gradedStepCount`, `firstTryCorrectCount`, `accuracyPercent = round(100 × firstTry / graded)` (0 nếu `graded` = 0), `durationSeconds = completed_at − started_at`, `studiedWordCount` = số bước WORD.
4. Chế độ `FIRST_COMPLETION`: `INSERT` vào `unit_progress` (`ON CONFLICT DO NOTHING`); `firstCompletion = (số dòng chèn = 1)`. Chế độ `REPLAY`: cập nhật `last_completed_at` và `best_accuracy_percent = max(cũ, mới)`; `firstCompletion = false`. Hai thiết bị cùng hoàn thành: dòng unique/khóa bảo đảm chỉ một bên có `firstCompletion = true`.
5. Đặt attempt `COMPLETED`, ghi `completion` JSON.
6. `unlockedUnitIds`: nếu `firstCompletion` và unit kế tiếp đang `LOCKED` trước đó thì `[nextUnitId]`, ngược lại rỗng. `nextUnitId`: unit kế tiếp trong danh sách đã phát hành (null nếu là cuối). `topicProgress`: tính lại như mục 7.

Phase 3 thêm vào bước 4–5: XP, streak, `reward`.

## 10. Lớp cần tạo

`domain/study`: `ExerciseGenerator`, `AnswerGrader`, `UnitUnlockRules`, `StudyText` (chuẩn hóa), các record `StudyStepSnapshot`; `application/port`: `StudyAttemptRepository`, `StudyProgressReader`; `infrastructure/adapter/StudyAttemptJdbcAdapter`, `JdbcStudyLock`; use case `StartStudyAttemptUseCase`, `GetStudyAttemptUseCase`, `SubmitStudyAnswerUseCase`, `CompleteStudyAttemptUseCase`, `GetStudyProgressUseCase`; DTO/Result; cập nhật `ExercisePreview` của phase 1 để dùng bộ sinh thật.

## 11. Kiểm thử (viết trước khi code)

1. `ExerciseGeneratorTest`: thứ tự và số bước theo ví dụ ở mục 5; 2–4 phương án, không hai phương án trùng văn bản; không có `optionId` dùng chung giữa hai cột MATCH; nhóm cuối MATCH không bao giờ có đúng 1 cặp; bỏ FILL khi câu ví dụ không chứa từ (kể cả từ nằm trong từ khác như "art" trong "party"); cùng `attemptId` cho đề giống hệt; payload không chứa khóa đáp án.
2. `AnswerGraderTest`: dấu cách thừa, hoa/thường, Unicode tổ hợp và dựng sẵn; MATCH thiếu cặp, trùng cặp, ghép sai.
3. `UnitUnlockRulesTest`: unit đầu `AVAILABLE`; unit sau `LOCKED` tới khi unit trước xong; chèn unit mới phía trước làm unit sau nó `LOCKED` nhưng các unit đã xong vẫn `COMPLETED`; unit đang làm giữ `IN_PROGRESS` dù tiền đề thay đổi; unit trước bị lưu trữ thì dùng unit trước đó.
4. `StudyAttemptJdbcIntegrationTest`: bắt đầu hai lần cùng `requestId` và hai lần khác `requestId` đều ra cùng một attempt đang làm; sai rồi đúng; nộp lại cùng `requestId` trả kết quả cũ kể cả sau khi attempt đã `COMPLETED`; cùng `requestId` khác đáp án là 409; nộp sai thứ tự là 409; tiếp tục từ phiên khác; replay tạo attempt mới; sửa và phát hành revision mới giữa chừng không đổi snapshot; hai thread cùng `complete` cho đúng một `firstCompletion`; attempt quá hạn chuyển `EXPIRED` và lần bắt đầu kế tạo attempt mới; `422` không ghi submission.
5. Test controller/bảo mật: cần đăng nhập; attempt của người khác trả 404; body có thêm `correct`, `xp`, `userId` thì bị bỏ qua (không có trường đó trong DTO); không có `answer_key` trong JSON của `Attempt`.
6. Test hiệu năng nhẹ: `GET /progress` với 100 topic chạy đúng 3 truy vấn (đếm câu lệnh).

## 12. Xong khi

- Chạy được trọn một unit chỉ bằng API, tiếp tục từ phiên khác, replay, và gọi lại mọi POST với cùng `requestId` an toàn.
- `accuracyPercent` và `firstTryCorrectCount` chỉ tính lần nộp đầu.
- `mvn -pl services/library-service -o clean test` xanh.

## Implementation notes (2026-11-07)

- V40 additionally persists `start_payload_hash` and `vocabulary_study_start_requests`: every start/resume POST requestId is durably bound to its payload and original attempt. Without resume aliases, retrying a resume after completion returned VOCAB_UNIT_COMPLETED and the key could be reused on another unit. The regression was observed RED before the fix.
- Answer request hashes include attemptId and stepId as well as canonical body content; MATCH pairs are ordered by leftOptionId. Expiry transitions commit before returning their 409.
- Completion includes the contract topicId/completedAt and excludes the finishing attempt from currentAttemptId. Progress reads use exactly three batched SQL queries, including curriculumVersion for topics whose units have all been archived.
- Unicode NBSP and narrow-NBSP normalization is covered; whitespace-only fill input is invalid rather than a wrong answer.
