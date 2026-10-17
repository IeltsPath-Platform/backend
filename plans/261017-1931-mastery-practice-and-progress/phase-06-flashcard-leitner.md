---
phase: 6
title: "Flashcard 5 hộp"
status: completed
priority: P2
dependencies: []
---

# Phase 6: Flashcard 5 hộp

## Overview

US-050/051 (03 §B.4) và 02 §A.7: ôn từ vựng theo 5 hộp Leitner (1, 2, 4, 8, 16 ngày). Library đã có `flashcards`,
`flashcard_decks`, từ vựng có IPA, audio phát âm và câu ví dụ, nhưng flashcard chưa có lịch ôn.

## Prerequisite

The existing Library target contained stale malformed bytecode (`ClassFormatError` and JDT compile errors). Removing
only `services/library-service/target` while Maven was idle allowed all production and test sources to compile and the
baseline 53-test suite to pass. No source fix was needed, so there was no separate prerequisite source commit.

## Requirements

- **Migration library `V3__flashcard_leitner.sql`:** thêm vào `flashcards`:
  `leitner_box SMALLINT NOT NULL DEFAULT 1 CHECK (leitner_box BETWEEN 1 AND 5)`, `next_review_at TIMESTAMPTZ NULL`
  (null = chưa ôn lần nào, coi như đến hạn), `last_reviewed_at TIMESTAMPTZ NULL`, `review_count INTEGER NOT NULL DEFAULT 0`;
  index `(user_id, status, next_review_at)`.
- **Domain** (`Flashcard` aggregate): `review(correct, now, intervals)`; đúng → hộp + 1 (tối đa 5), sai → hộp 1;
  `next_review_at = now + interval(hộp)`. Khoảng cách cấu hình `library.srs.intervals-days: [1,2,4,8,16]`.
- **API** (`CUSTOMER`, chỉ thẻ của mình, qua route `/api/learning-support/flashcards/**` đang trỏ về library):
  - `GET /api/learning-support/flashcards/due?deckId=&limit=20` (tối đa 50): thẻ `ACTIVE` có `next_review_at` null hoặc
    ≤ now, sắp theo `next_review_at` (null trước), phân trang ở database;
  - `POST /api/learning-support/flashcards/{id}/reviews` body `{ "correct": true, "requestId": "<uuid>" }`: trả hộp mới và
    `nextReviewAt`. Gửi lại cùng `requestId` không ôn hai lần (lưu `requestId` cuối trên thẻ là đủ).
  - Response thẻ có thêm `leitnerBox`, `nextReviewAt`; thẻ gắn `vocabulary_sense_id` trả thêm IPA, audio, câu ví dụ
    (query gom, không N+1).
- Thẻ `ARCHIVED`/`DELETED` không vào danh sách đến hạn.

## Related Code Files

(`B` = `library-service/src/main/java/com/ieltspath/library`)

- Sửa: `B/domain/aggregate/Flashcard.java`, JPA entity, mapper, `FlashcardRepository` (query đến hạn), controller flashcard
- Tạo: use case ôn thẻ và lấy thẻ đến hạn, migration, properties
- Test: test domain (đúng/sai, trần hộp 5, gửi lại cùng requestId), `@WebMvcTest`, integration Testcontainers (query đến
  hạn đúng thứ tự, chỉ thẻ của user)

## Success Criteria

- [x] Thẻ mới đến hạn ngay; đúng 4 lần liên tiếp lên hộp 5 với hạn 16 ngày; sai một lần về hộp 1.
- [x] Danh sách đến hạn phân trang ở database, không lẫn thẻ của người khác.
- [x] `mvn -B -ntp -pl services/library-service test` xanh.

## Progress

- Migration V3, five-box schedule, owner-scoped due listing, request ID idempotency, vocabulary metadata batching, and README documentation are complete.
- Test: `mvn -B -ntp -pl services/library-service test` — 65 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL Testcontainers ran.

## Risk Assessment

- Library đang ngoài phạm vi nghiệm thu plan 1; phase này kéo library vào CI, phải sửa test cũ trước.

## Bàn giao FE

Màn "Ôn từ vựng hôm nay": lật thẻ, nút Nhớ / Chưa nhớ, phát audio phát âm, hiển thị câu ví dụ.
