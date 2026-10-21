---
phase: 4
title: "Attach vocabulary blocks to lessons"
status: in-progress
priority: P2
dependencies: [1]
---

# Phase 4: Attach vocabulary blocks to lessons
<!-- Updated: Validation Session 2 - data only, no gate; no longer waits for flashcard UI -->

## Overview
Seed block `VOCABULARY` cho L1–L4 của topic `DEMO_READING` ở content-service, tham chiếu sense UUID cố định của library V15.
Block chỉ là dữ liệu: nó không chặn hoàn thành lesson. FE hiện bỏ qua loại block này, nên merge lúc nào cũng an toàn.

## Requirements
- Lesson mục tiêu: L1–L4 của topic `DEMO_READING`. Lấy `lessons.code` thật từ seed content trước khi viết SQL.
- Mỗi lesson có đúng 1 block VOCABULARY, đặt đầu bài (pre-teach), gồm 8–15 sense.
- Migration idempotent, không phá `uq_lesson_blocks_sort_order`.
- Mọi `vocabulary_sense_id` phải có trong library V15.
- Điều kiện hoàn thành lesson không đổi: `isExercise()` không tính VOCABULARY, nên block này tự được bỏ qua.

## Architecture
- `V72__seed_lesson_vocabulary_blocks.sql`:
  - Block id cố định.
  - `sort_order = (SELECT MIN(sort_order) - 1 FROM lesson_blocks WHERE lesson_id = …)` để không phải dịch các block cũ.
  - Lesson chọn theo `lessons.code`.
  - `ON CONFLICT DO NOTHING`.
- Bảng `lesson_block_vocabulary(block_id, vocabulary_sense_id, sort_order)` đã có. Internal API
  (`JdbcLearningContentReader`) và learning (`LessonViewAssembler` → `LessonResponse.vocabularySenseIds`) đã chuyển tiếp
  dữ liệu, nên không cần đổi code.

## Related Code Files
- Create: `content-service/src/main/resources/db/migration/V72__seed_lesson_vocabulary_blocks.sql` (kiểm số V mới nhất trước khi tạo)
- Create: `content-service/src/test/.../infrastructure/persistence/LessonVocabularyBlockSeedTest.java` (theo mẫu `LessonPipelineSeedTest`)
- Modify: `content-service/src/test/.../infrastructure/persistence/LessonPipelineSeedTest.java` (đồng bộ assertion với Writing seed và block VOCABULARY)
- Modify (test only): `learning-service/src/test/.../infrastructure/persistence/LessonSubmissionIntegrationTest.java`, hoặc
  file test completion tương đương

## Implementation Steps
1. **Test đỏ (content):**
   - Mỗi lesson code mục tiêu có đúng 1 block VOCABULARY, nằm đầu bài, có số sense đúng.
   - Danh sách sense id khớp hằng số của library V15. Hằng số copy vào test, kèm comment trỏ tới file V15.
   - Internal API `/internal/learning-content/lessons/{id}` trả `vocabularySenseIds` đúng thứ tự.
2. **Test khóa hành vi (learning, phải xanh ngay, không cần sửa code):**
   - Lesson có VOCABULARY + EXERCISE: pass hết exercise thì `lessonCompleted=true`, lesson kế mở.
   - Lesson chỉ TEXT + VOCABULARY: `POST /complete` thành công.
   - `GET lesson` trả block VOCABULARY kèm `vocabularySenseIds`.
   - `kp_evidence` không thay đổi.
3. Viết V72. Chạy `mvn -q -pl services/content-service -am test` và `mvn -q -pl services/learning-service -am test`.
4. Kiểm thủ công qua Gateway:
   - `GET lesson` L1 có `vocabularySenseIds`.
   - `GET /api/content/vocabulary/senses?ids=…` với các id đó trả đủ thẻ.
   - Học L1 như cũ thì L2 mở.

## Success Criteria
- [ ] Seed test và test khóa hành vi xanh. Test content và learning hiện có không đỏ.
- [ ] Kiểm thủ công bước 4 đạt.

## Risk Assessment
- **Lệch id giữa content và library** (khác DB, không có FK): test bước 1 cộng kiểm thủ công. Sense lạ chỉ làm thiếu thẻ, không
  khóa user.
- **Rollback:** thêm migration xóa block VOCABULARY theo id cố định. Không ảnh hưởng tiến độ học.


## Progress — 2026-10-21

- Đã thêm migration V72 để tạo một block VOCABULARY đầu bài cho từng lesson L1–L4 và gắn đúng 8 sense đã duyệt mỗi bài.
- Đã thêm content seed test kiểm tra thứ tự block, thứ tự sense qua response nội bộ và chạy lại migration không nhân đôi dữ liệu.
- Đã thêm Learning integration cases cho lesson VOCABULARY + EXERCISE và TEXT + VOCABULARY; kiểm tra completion, unlock và không tạo kp_evidence từ block từ vựng.
- Cập nhật contract nội bộ để bỏ nhận định MVP chưa có vocabulary block.
- Chưa chạy Maven tests hoặc Gateway check; Maven CLI không có trong PATH, lần compile trước bị chặn do plugin thiếu cache và mạng hạn chế. Các tiêu chí thành công vẫn để chưa đánh dấu.


## Startup follow-up — 2026-10-21

- Root cause: database đã áp dụng Content V59–V70 trên nhánh Writing nhưng nhánh vocabulary thiếu các file này. Khôi phục đúng 12 file gốc từ `fix/learn-writing-skill` (`ae44b87d`), kiểm tra trùng byte và đã copy vào runtime `target/classes`.
- Flyway áp dụng thành công toàn bộ V1–V58 và V72 (V59–V71 thuộc nhánh Writing, đã tách khỏi PR này) trên PostgreSQL Testcontainers.
- Lượt test toàn Content chạy 520 test: 6 assertion seed cũ trong `LessonPipelineSeedTest` chưa tính Writing/VOCABULARY. Đã cập nhật các kỳ vọng chính xác theo migration, giữ kiểm tra thứ tự, câu hỏi và KP; chạy lại cả `LessonPipelineSeedTest` + `LessonVocabularyBlockSeedTest`: 34/34 pass, không skip. 486 test Content khác pass trong lượt đầy đủ.
- `git diff --check` sạch. Learning tests và Gateway acceptance vẫn chưa xác minh; phase giữ in-progress. Database đang dùng của người dùng chưa được khởi động lại/ghi bởi lượt kiểm tra này.
