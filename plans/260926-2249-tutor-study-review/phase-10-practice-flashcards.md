---
phase: 10
title: "Lưu câu luyện thành flashcard"
status: pending
priority: P3
dependencies: [6]
effort: "~1d"
---

# Phase 10: Lưu câu luyện thành flashcard

## Overview
Sau khi trả lời một câu luyện (pha 6), học viên bấm **"Lưu thành flashcard"**. **Frontend** gọi thẳng API flashcard sẵn
có của `learning-support-service`, dùng dữ liệu mà AI Learning vừa trả (câu hỏi, đáp án, giải thích). Chỉ sửa
learning-support: thêm nguồn `PRACTICE_QUESTION` trỏ ngược về câu luyện, và chống tạo trùng.

**AI Learning không gọi learning-support** và không đổi gì trong pha này (phương án A, chốt 2026-09-27).

## Quyết định đã chốt

| # | Quyết định | Lý do |
| --- | --- | --- |
| F1 | Chỉ tạo flashcard **khi học viên chủ động bấm, sau khi đã trả lời**. Không tự động lúc tutor sinh câu. | Mặt sau flashcard chứa đáp án: tạo sớm là lộ đáp án. Tạo tự động làm đầy bộ thẻ bằng câu học viên không định giữ. |
| F2 | Frontend gọi `POST /api/learning-support/flashcards`. Không có lời gọi service-to-service nào. | Hai service không bị nối với nhau, không cần xử lý lỗi xuyên service, không truyền bearer vào engine tutor. |
| F3 | Thêm `FlashcardSourceType.PRACTICE_QUESTION`: **bắt buộc** `sourceReferenceId` = `questionId` (uuid) của câu luyện; **cấm** `vocabularySenseId` và `highlightedText`. | Truy ngược được flashcard về câu luyện. `MANUAL` hiện cấm `sourceReferenceId`. |
| F4 | **Idempotent**: đã có flashcard chưa bị xóa (`status <> 'DELETED'`) cùng `(user, PRACTICE_QUESTION, sourceReferenceId)` → trả thẻ đó với `200`, không tạo mới. Tạo mới → `201`. Có partial unique index chống bấm hai lần đồng thời. | Bấm lại hay mạng chập chờn không sinh thẻ trùng. Thẻ đã xóa thì cho lưu lại. |
| F5 | learning-support **không** kiểm tra với AI Learning rằng câu luyện có thật và đã trả lời. | `front`/`back` vốn do học viên tự nhập (như `MANUAL`); nội dung chỉ là của chính họ. Kiểm chéo sẽ nối hai service với nhau mà không bảo vệ được gì thêm. |
| F6 | Không tự tạo deck. Muốn đưa vào deck thì frontend gọi `POST /api/learning-support/decks/{deckId}/items` sẵn có. | YAGNI; API deck đã đủ. |
| F7 | Flashcard **không** thay lịch ôn câu sai của pha 6. | Flashcard của learning-support không có lịch ôn; đây chỉ là bản lưu để tự lật. |

## Phụ thuộc vào pha 6
Frontend cần `questionId` (uuid) của câu luyện. Pha 6 trả `questionId` trong entry DTO, trong response của
`POST /practice/entries/{entryId}/answer` và của `POST /practice/reviews` (đã ghi trong phase-06). Nếu pha 6 đã làm xong
mà chưa có `questionId`, thêm vào trước khi làm pha này, trong một commit riêng.

## Ràng buộc
- Không import hay chép code DeepTutor (pha này không cần tham khảo gì từ `third_party/`).
- Giữ nguyên hành vi của `MANUAL`, `VOCABULARY_SENSE`, `HIGHLIGHT` và API flashcard/deck hiện có.
- Không log `front`/`back`.

## Đọc trước (trong `services/learning-support-service`)
- `domain/vo/FlashcardSourceType.java`, `domain/aggregate/Flashcard.java` (switch validate nguồn, khoảng L80–110).
- `application/usecase/CreateFlashcardUseCase.java`, `UpdateFlashcardUseCase.java`, `DeleteFlashcardUseCase.java`.
- `domain/repository/FlashcardRepository.java`, `infrastructure/persistence/repository/FlashcardJpaRepository.java` và
  adapter tương ứng.
- `api/controller/FlashcardController.java` (`create` đang trả `@ResponseStatus(CREATED)`).
- `api/exception/GlobalExceptionHandler.java`, `domain/exception/ConflictException`.
- `src/main/resources/db/migration/` (hiện chỉ có `V1__create_learning_support_tables.sql`).
- Test mẫu: `application/usecase/PersonalLibraryUseCaseTest.java` (mock repository),
  `infrastructure/persistence/LearningSupportSchemaTest.java` (Testcontainers, kiểm bảng và index),
  `api/controller/NoteSecurityTest.java` (`@WebMvcTest`).
- `.sdd/database/DATABASE_V5.md` §8.7 `flashcards`.

## Related Code Files
- Modify: `domain/vo/FlashcardSourceType.java`, `domain/aggregate/Flashcard.java`
- Modify: `domain/repository/FlashcardRepository.java`, JPA repository và adapter
  (`findActiveByUserIdAndSourceTypeAndSourceReferenceId`, nghĩa là `status <> DELETED`)
- Modify: `application/usecase/CreateFlashcardUseCase.java` (trả kèm cờ `created`), `api/controller/FlashcardController.java`
  (`ResponseEntity`: 201 hoặc 200)
- Create: `src/main/resources/db/migration/V{n}__flashcard_practice_question_source.sql` với `n` là **số phiên bản kế
  tiếp còn trống** (hiện là 2; pha 8 cũng dự định thêm migration ở service này, ai làm sau thì lấy số kế tiếp)
- Create/Modify tests: xem mục Tests
- Modify docs: `.sdd/database/DATABASE_V5.md` §8.7; `docs/contracts/practice-v1.md` (tạo ở pha 6) thêm mục "Save as flashcard"

## Migration

```sql
-- One live flashcard per practised question and learner; a deleted card may be saved again.
CREATE UNIQUE INDEX uq_flashcards_user_practice_question
    ON flashcards (user_id, source_reference_id)
    WHERE source_type = 'PRACTICE_QUESTION' AND status <> 'DELETED';
```
`source_type` là `varchar(50)` không có CHECK, nên không cần đổi cột.

## Implementation Steps
1. **Test trước** (mục Tests), chạy thấy fail.
2. Enum thêm `PRACTICE_QUESTION`. Trong `Flashcard` (switch validate), nhánh mới: `sourceReferenceId` bắt buộc,
   `vocabularySenseId` và `highlightedText` phải `null` (sai → `InvalidDataException`, tức 400).
3. Repository: thêm hàm tìm thẻ chưa xóa theo `(userId, PRACTICE_QUESTION, sourceReferenceId)`.
4. `CreateFlashcardUseCase`: với `PRACTICE_QUESTION`, tìm trước; có rồi → trả thẻ cũ và `created=false`. Không có →
   `save`. Nếu `save` ném `DataIntegrityViolationException` do đúng index `uq_flashcards_user_practice_question`
   (bấm đồng thời) → đọc lại và trả thẻ đó với `created=false`. Nguồn khác giữ nguyên hành vi.
5. `FlashcardController.create` trả `201` khi `created`, `200` khi trả thẻ cũ. Body vẫn là `FlashcardResponse`.
6. `UpdateFlashcardUseCase`: đổi thẻ sang `PRACTICE_QUESTION` trùng một thẻ đang sống → `ConflictException` (409), bắt
   từ vi phạm index. Không cần logic riêng nếu handler hiện có đã map lỗi này; kiểm bằng test.
7. Migration; cập nhật `LearningSupportSchemaTest` nếu test kiểm danh sách index.
8. Docs:
   - DATABASE_V5 §8.7: thêm giá trị `PRACTICE_QUESTION` và quy tắc F3/F4.
   - `practice-v1.md`: luồng frontend và định dạng khuyến nghị:
     - `front` = câu hỏi, rồi mỗi option một dòng `A. ...`
     - `back` = `Answer: <đáp án>` (choice: `B. <nội dung>`), dòng trống, rồi `explanation`
9. Gate, rồi **một commit**: `feat(learning-support): save practice questions as flashcards`.

## Luồng frontend (ghi vào `practice-v1.md`)

```text
POST /api/ai-learning/practice/entries/{entryId}/answer   → {questionId, isCorrect, correctAnswer, explanation, ...}
(hiện nút "Lưu thành flashcard" sau khi có kết quả)
POST /api/learning-support/flashcards
     {sourceType: "PRACTICE_QUESTION", sourceReferenceId: questionId, front, back}
     → 201 thẻ mới | 200 thẻ đã lưu trước đó
(tùy chọn) POST /api/learning-support/decks/{deckId}/items {flashcardId}
```
Nút cũng có trong notebook (`GET /practice/notebook`) cho các entry đã trả lời.

## Tests
Unit (`PersonalLibraryUseCaseTest` hoặc file mới `PracticeQuestionFlashcardTest`):
1. `PRACTICE_QUESTION` thiếu `sourceReferenceId`, hoặc có `vocabularySenseId` hay `highlightedText` → `InvalidDataException`.
2. Tạo lần đầu → `created=true`, `save` được gọi. Đã có thẻ chưa xóa → trả thẻ cũ, `created=false`, không `save`.
3. `save` ném vi phạm unique index → đọc lại, trả thẻ cũ, `created=false`.
4. `MANUAL`, `VOCABULARY_SENSE`, `HIGHLIGHT` giữ nguyên hành vi (các test cũ pass nguyên).

Web (`@WebMvcTest(FlashcardController)`, theo mẫu `NoteSecurityTest`):

5. Tạo mới → 201; lần hai cùng `sourceReferenceId` → 200, cùng `id`; `sourceType` sai hoặc thiếu `sourceReferenceId` → 400.

Persistence (Testcontainers, theo mẫu `LearningSupportSchemaTest`):

6. Index tồn tại. Hai thẻ chưa xóa cùng user và `sourceReferenceId` → vi phạm index. Sau khi thẻ đầu `DELETED` thì tạo
   lại được. User khác, cùng `sourceReferenceId` → được.

## Gate
Trong repo root: `mvn -pl services/learning-support-service -am test` (0 fail; test Testcontainers cần Docker, không được
skip. Không có Docker thì dừng và báo), `git diff --check`. Sau đó `graphify update .`.

## Success Criteria
- [ ] Học viên lưu được câu luyện đã trả lời thành flashcard, truy ngược được bằng `sourceReferenceId`.
- [ ] Bấm nhiều lần (kể cả đồng thời) chỉ ra một thẻ; thẻ đã xóa thì lưu lại được.
- [ ] Hành vi các nguồn flashcard cũ không đổi; AI Learning không đổi.
- [ ] DATABASE_V5 và hợp đồng practice đã cập nhật.

## Risk Assessment
- **Pha 6 chưa trả `questionId`** → frontend không có uuid để gửi. Đã ghi vào phase-06; kiểm lại trước khi làm.
- **Trùng số migration với pha 8** → dùng số kế tiếp còn trống lúc implement; phase-08 đã ghi quy tắc tương tự.
- **Nội dung thẻ do frontend ghép** → có thể khác định dạng giữa các màn; `practice-v1.md` ghi một định dạng chung.
- Rollback: revert commit và `DROP INDEX uq_flashcards_user_practice_question`. Thẻ `PRACTICE_QUESTION` đã tạo vẫn đọc
  được nếu giữ giá trị enum; nếu bỏ enum thì phải đổi các thẻ đó sang `MANUAL` và xóa `source_reference_id` trước.
