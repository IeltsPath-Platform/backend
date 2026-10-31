---
phase: 5
title: "Vocabulary catalog covers"
status: pending
priority: P2
effort: "1d"
dependencies: [1]
---

# Phase 5: Vocabulary catalog covers

<!-- Updated: Validation Session 1 - bỏ giả định job dọn mồ côi (không tồn tại); ảnh bìa cũ để mồ côi -->

## Phân công

- **Codex**, theo `briefs/codex-phase-05-catalog-covers.md`. Claude review.
- Điều kiện: phase 3 đã merge (dùng `mediaUploadApi.ts`). Seed không đặt `cover_key`; `library_db` thật chưa kiểm được (cần người dùng chạy truy vấn ở bước 2 trên DB đang dùng). Chưa có kết quả thì Codex không siết `CatalogText`.

## Overview

Ảnh bìa chủ đề (`vocabulary_topics.cover_key`) và bộ từ vựng (`vocabulary_sets.cover_key`) của library-service phải là file trên Supabase. Đã xác minh: `FlashcardImageStorageAdapter` dùng bucket `flashcards` (`library.flashcard-images.bucket`, mặc định `flashcards`), và `VocabularyCatalogJdbcAdapter` resolve `cover_key` qua `FlashcardImageStorage.publicUrl`. Còn thiếu: không có endpoint upload dành cho admin catalog, nên admin không có cách tạo khóa hợp lệ.

## Requirements

- Functional: `POST /api/content/admin/vocabulary/covers` (multipart, cùng quyền `CONTENT_AUTHOR`/`ADMIN` như `AdminVocabularyCatalogController`) nhận jpeg/png/webp ≤ 2 MB, lưu `catalog-cover/{userId}/{uuid}.{ext}` trong bucket `flashcards`, trả `{key, url}`. Form admin chủ đề/bộ từ vựng chọn ảnh bằng upload, gửi `coverKey`.
- Non-functional: không bucket mới; không tái dùng khóa `images/{userId}/…` của học viên; bìa bộ thẻ cá nhân (`coverImageKey` của `FlashcardDeck`) không đổi, đã upload qua `POST /api/learning-support/flashcards/images`. Thay hoặc xóa bìa catalog: object cũ **để mồ côi** (quyết định đã chốt), không viết code xóa.

## Architecture

`CatalogText.coverKey` đã từ chối URL, chỉ nhận khóa; thêm upload là đủ, không đổi mô hình dữ liệu. Dùng port `FlashcardImageStorage` hiện có, thêm use case riêng `UploadCatalogCoverUseCase` (policy và tiền tố khác với ảnh flashcard của học viên).

An toàn trước luồng học viên (đã xác minh trong code): `CreateFlashcardUseCase`, `UpdateFlashcardUseCase`, `CreateFlashcardDeckUseCase`, `UpdateFlashcardDeckUseCase` gọi `ObjectKeys.requireOwnedBy(key, "images", userId)`, nên học viên không thể gán khóa `catalog-cover/…` vào thẻ hay bộ thẻ. Các use case xóa/đổi ảnh chỉ xóa object khi `!FlashcardImageUsageService.isStillReferenced(oldKey)` cho khóa của chính họ. Library-service không có job `@Scheduled` dọn ảnh nào.

## Related Code Files

- Create: `application/usecase/UploadCatalogCoverUseCase.java`, response DTO, test; endpoint trong `AdminVocabularyCatalogController` (hoặc controller con cùng đường dẫn `/api/content/admin/vocabulary`).
- Modify: form admin từ vựng ở frontend (`src/features/admin/...` phần catalog chủ đề/bộ từ vựng; xác định tệp chính xác khi bắt đầu), type request thêm `coverKey`/xem trước `coverUrl`.
- Tham khảo: `UploadFlashcardImageUseCase` (mẫu `ObjectKeys.forUser`), `FlashcardImageUsageService`, `VocabularyTopicRequest` / `VocabularySetRequest`, `V19__vocabulary_catalog_enrichment.sql`.

## Implementation Steps

1. **Đọc trước:** `UploadFlashcardImageUseCase`, `CatalogText.coverKey`, `AdminVocabularyCatalogController`, form admin FE.
2. **Kiểm dữ liệu** trên `library_db` (chỉ đọc): `select 'topic', slug, cover_key from vocabulary_topics where cover_key is not null union all select 'set', title, cover_key from vocabulary_sets where cover_key is not null;` và so với `storage.objects` của `flashcards` (hiện chỉ 1 object của học viên demo). Mọi `cover_key` không có object tương ứng là bìa hỏng cần upload lại; liệt kê cho R&D.
3. **Test trước:** policy (từ chối SVG, >2 MB, byte giả), khóa đúng tiền tố, quyền (học viên `CUSTOMER` bị 403); học viên không gán được khóa `catalog-cover/` cho thẻ/bộ thẻ (`requireOwnedBy` từ chối); thay `coverKey` của chủ đề không xóa object cũ.
4. Thêm use case + endpoint; trả cả `key` và `url` (qua `FlashcardImageStorage.publicUrl`).
5. Frontend: thêm điều khiển upload vào form chủ đề và bộ từ vựng (create/edit trong drawer theo quy ước admin của dự án); đặt `coverKey` theo `key` trả về; xem trước bằng `url`.
6. Cập nhật tài liệu phase 2: `flashcards` có tiền tố `images/` (học viên) và `catalog-cover/` (admin).
7. Chạy `mvn -B -ntp -pl services/library-service -am test` và test FE catalog.

## Success Criteria

- [ ] Admin upload được bìa cho chủ đề và bộ từ vựng; `cover_key` trỏ tới object tồn tại, `coverUrl` hiển thị cho học viên.
- [ ] Học viên không gọi được endpoint; SVG và file lớn bị từ chối; học viên không gán được khóa `catalog-cover/` vào thẻ hay bộ thẻ.
- [ ] Danh sách `cover_key` hỏng (nếu có) đã được báo cho R&D.
- [ ] Test library-service và FE xanh.

## Risk Assessment

- Object bìa cũ còn lại sau khi thay (mồ côi): chấp nhận theo quyết định; dung lượng tăng chậm vì bìa ít đổi.
- Bucket `flashcards` giới hạn 2 MB và 3 loại ảnh đúng với nhu cầu bìa; nếu R&D cần ảnh lớn hơn, nâng giới hạn có chủ đích, không phải mặc định.
- Rollback: endpoint mới thêm vào, revert không ảnh hưởng dữ liệu cũ.
