# Phase 2: Migration rubric Writing @3

## Context

- Đề xuất Phần A (A1–A8). Quyết định D3 trong [plan.md](./plan.md).
- Mẫu: V122 (ngừng dùng bản ACTIVE, chép sang version mới trong vòng lặp `DO $$` cho hai task, kiểm 4 × 18).
- Schema: `rubrics` (`system_prompt` TEXT, tối đa 4000 ký tự theo `Rubric`), `rubric_criteria.description`,
  `rubric_band_descriptors` (band 0–9 bước 0.5).
- Prompt in mỗi tiêu chí theo dạng `CODE (name): description`, rồi tới từng dòng `Band X: descriptor`
  (`EssayGrader.renderCriterion`, `RubricPromptBuilder.formatCriterion`).

## File

- Tạo `services/content-service/src/main/resources/db/migration/V133__writing_rubric_v3_criterion_descriptors.sql`
- Sửa `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/WritingRubricLowBandMigrationTest.java`:
  test này đang đòi @2 ACTIVE; sau V133 @2 là RETIRED.
- Tạo `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/WritingRubricV3MigrationTest.java`

## Bước làm

1. Bảng tạm `writing_v3_headers (task, criterion_code, description)` chứa dòng tiêu đề của A4–A8. CC, LR, GRA có cùng
   tiêu đề cho hai task.
2. Bảng tạm `writing_v3_bands (task, criterion_code, band, descriptor)` chứa 10 band nguyên (0–9) của A4–A8.
   TA chỉ có ở TASK_1, TR chỉ có ở TASK_2.
3. Band x.5 (1.5, 2.5 … 8.5, tức 8 band) sinh bằng SQL:
   `'Meets every point of band ' || floor(band) || ' and some, but not all, points of band ' || (floor(band)+1) || '.'`.
   Không thêm tiền tố `Band X.5:` vì renderer đã in. Tổng mỗi tiêu chí là 10 + 8 = 18 band, cùng tập band với @2.
4. `system_prompt` = phần chung A3. Câu cuối của A3 đổi thành
   `Use the word count given with the essay; do not count the words yourself.`, vì user prompt đã có sẵn
   `Word count of the essay: …` (`EssayGrader.userPrompt`). Bỏ `{{paragraph_count}}`, vì backend chưa đếm đoạn (D1).
5. Vòng lặp như V122 cho `IELTS_WRITING_TASK_1` và `IELTS_WRITING_TASK_2`, với UUID cố định mới
   (`39a00000-0000-4000-8000-000000000005` và `…0006`):
   - đặt bản ACTIVE thành RETIRED, chèn @3 ACTIVE với `source_note` ghi là diễn đạt lại descriptor công khai, do
     product owner duyệt và chưa qua examiner;
   - chèn criteria (giữ `name`, `sort_order` của @2, `description` mới);
   - chèn descriptor từ hai bảng tạm;
   - chép `rubric_examples` từ @2;
   - kiểm tra 4 tiêu chí × 18 band, ngược lại `RAISE EXCEPTION`.
6. Không đụng `writing_criterion_knowledge_points`: bảng này gắn KP theo task và mã tiêu chí, không theo version.

## Test

- Mỗi task có đúng một rubric ACTIVE, là version 3; @2 RETIRED; @1 và @2 không đổi nội dung.
- 4 tiêu chí, mỗi tiêu chí 18 band, có band 0 và 9, không có band 0.5.
- Band 4.5 có descriptor nhắc band 4 và band 5.
- `system_prompt` không chứa `{{`.
- Sửa `WritingRubricLowBandMigrationTest` để kiểm @2 với trạng thái RETIRED, hoặc bỏ dòng kiểm trạng thái ACTIVE.

## Rủi ro

- Môi trường không ghim prompt sẽ chấm theo @3 ngay khi deploy.
- Prompt dài hơn: dòng tiêu đề dài, nhưng descriptor ngắn và band .5 rất ngắn. Cần đo token khi chấm lại.
- Admin UI không sửa được @3 (`validateForActivation` vẫn đòi band 4.0–9.0), giống @2.
- Rollback: migration mới đặt @3 RETIRED và @2 ACTIVE.
