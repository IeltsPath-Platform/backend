---
phase: 1
title: "Library sense batch API and vocabulary seed"
status: in-progress
priority: P2
dependencies: []
---

# Phase 1: Library sense batch API and vocabulary seed

## Overview
FE flashcard cần nội dung thẻ cho danh sách `senseId` của block. Thêm API batch đọc sense ở library-service và seed
sense có UUID cố định để content tham chiếu được.

## Requirements
- `GET /api/content/vocabulary/senses?ids=<uuid>,<uuid>…` → `[{senseId, vocabularyItemId, lemma, ipa,
  pronunciationAudioReference, partOfSpeech, englishDefinition, vietnameseMeaning, exampleSentence, imageUrl}]`.
  - Giữ thứ tự `ids`; bỏ qua id không tồn tại hoặc item/sense không ACTIVE (không 404 cả lô).
  - Tối đa 100 id; rỗng, > 100 hoặc id sai định dạng → 400.
  - Public như `/search`: `/api/content/vocabulary/**` đã nằm trong `public-endpoints` của library (verified). Controller map cả
    `/api/content/admin/vocabulary`, nên endpoint cũng xuất hiện dưới prefix admin — chấp nhận, chỉ đọc.
<!-- Updated: Validation Session 1 - security verified, seed scope DEMO_READING L1–L4 -->
- Seed: UUID sense cố định, idempotent, không nhân đôi lemma đã có từ V4.

## Architecture
- Gateway đã route `/api/content/vocabulary/**` → library; không đổi Gateway.
- Spring map `/senses` literal trước `/{id}`; vẫn thêm test controller để chắc không đụng `GET /{id}`.
- Use case mới đọc qua `VocabularyRepository` (thêm `findActiveSensesByIds`), mapper ra DTO.

## Related Code Files
- Create: `library-service/.../application/usecase/GetVocabularySensesUseCase.java`
- Create: `library-service/.../api/dto/.../VocabularySenseCardResponse.java` (đặt theo package DTO hiện có)
- Create: `library-service/src/main/resources/db/migration/V15__seed_lesson_vocabulary.sql`
- Modify: `library-service/.../api/controller/VocabularyController.java`
- Modify: `library-service/.../domain/repository/VocabularyRepository` + `infrastructure/persistence/adapter/VocabularyRepositoryAdapter.java`
- Tests: `library-service/src/test/.../api/controller/VocabularySensesControllerTest.java`,
  `.../application/usecase/GetVocabularySensesUseCaseTest.java`,
  `.../infrastructure/persistence/LessonVocabularySeedMigrationTest.java` (theo mẫu `VocabularySeedMigrationTest`)

## Implementation Steps
1. **Test trước**: use case — giữ thứ tự, lọc INACTIVE, bỏ id lạ; controller — 400 khi rỗng/>100/sai UUID, 200 đúng shape,
   `GET /{id}` vẫn chạy.
2. Thêm query repository (một query `WHERE s.id = ANY(?)` join item, cả hai ACTIVE).
3. Use case + DTO + endpoint. Chạy `mvn -q -pl services/library-service -am test`.
4. **Test trước** cho seed: mọi sense id trong V15 tồn tại, ACTIVE, `vietnamese_meaning` + `example_sentence` không rỗng,
   không có hai item cùng `normalized_lemma` do V15 tạo.
5. V15: với mỗi từ — `INSERT item … WHERE NOT EXISTS (normalized_lemma)` dùng UUID cố định; sense `INSERT … SELECT item.id
   FROM vocabulary_items WHERE normalized_lemma = … LIMIT 1 ON CONFLICT (id) DO NOTHING` với UUID sense cố định.
   Prefix UUID riêng (vd `7a000000-0000-4000-8000-…`) để dễ grep.
6. Danh sách từ: L1–L4 của topic `DEMO_READING`, 8–15 từ/lesson, trích từ text/passage của từng lesson (đọc seed content để lấy
   passage). Trình danh sách cho người dùng duyệt trước khi viết SQL.

## Success Criteria
- [ ] Test controller/use case/seed xanh; test library hiện có không đỏ.
- [ ] Gọi qua Gateway 8080 trả đúng thẻ cho 3 sense seed.

## Risk Assessment
- Lemma trùng V4 → gắn sense mới vào item cũ (bước 5), không tạo item trùng.
- Danh sách từ chưa được duyệt → phase dừng ở bước 6; API (bước 1–3) làm trước được.


## Progress — 2026-10-21

- Đã thêm batch API, use case, DTO/domain projection, truy vấn chỉ lấy item và sense ACTIVE, và migration V15 idempotent.
- Người dùng duyệt nguyên danh sách 8 từ cho mỗi lesson L1–L4. benefit và drawback tái sử dụng item hiện có.
- Đã thêm controller, use case và migration tests. Sửa các lỗi review về annotation và thứ tự constructor; UUID có khoảng trắng bị từ chối.
- git diff --check sạch. Lệnh mvn -q -pl services/library-service -am test chưa chạy được vì Maven CLI không có trong PATH; lần compile trước bị chặn do compiler plugin thiếu trong cache và mạng bị giới hạn. Chưa xác minh test hoặc API qua Gateway.
- Phase vẫn in-progress; tiêu chí thành công giữ chưa đánh dấu cho đến khi kiểm tra test/build và API qua Gateway.

- Rà soát closeout phát hiện và sửa annotation `@Override` bị lặp tại `VocabularyRepositoryAdapter`; tests/Gateway vẫn cần chạy để hoàn tất acceptance.
