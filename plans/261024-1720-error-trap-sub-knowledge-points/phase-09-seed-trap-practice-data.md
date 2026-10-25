---
phase: 9
title: Seed trap practice data
status: completed
priority: P1
effort: 1.5d
dependencies:
  - 3
---

# Phase 9: Seed trap practice data

> Làm **ngay sau phase 3** (chỉ cần V79 và tự điền T/F/NG); có thể song song với phase 4–7; phải xong trước phase 8.

## Overview
Dữ liệu mẫu đủ để mọi mã lỗi trong phạm vi có KP con nhận bằng chứng, bài ôn KP cha chọn được bộ theo bẫy, và
`ERROR_PATTERN` tạo được. Nội dung do AI soạn, đánh dấu chờ giám khảo IELTS duyệt (như bộ luyện Writing demo V70).

## Requirements

**Phạm vi mã lỗi** (ghi danh sách cụ thể vào `reports/seed-trap-scope.md` trước khi soạn):
- Trong phạm vi: mã ACTIVE có `trap_knowledge_point_id` **và** KP cha có ít nhất một câu `CHOICE` (trắc nghiệm hoặc T/F/NG)
  đã xuất bản.
- Ngoài phạm vi (ghi lý do vào báo cáo): mã chỉ do luật câu điền gắn (`WORD_LIMIT_EXCEEDED`, `NUMBER_FORMAT`, `PLURAL_FORM`,
  `SPELLING`); mã mà KP cha chỉ có câu essay.

**Ngưỡng "đủ"** cho mỗi mã X trong phạm vi, tính trên bộ luyện đã xuất bản của KP cha:
- ≥ 2 bộ đủ điều kiện ôn KP cha (tổng ≥ 3 câu, có câu gắn KP cha), mỗi bộ có **≥ 2 câu chứa bẫy X**. Hai bộ để có bộ kế
  tiếp khi trượt và vẫn còn bộ sau khi luyện tự do giữ dự trữ.
- T/F/NG tính riêng từng bẫy: ≥ 2 bộ có ≥ 2 câu đúng `NOT_GIVEN`, và ≥ 2 bộ có ≥ 2 câu đúng `FALSE`/`NO`.

**Cách bổ sung, theo thứ tự ưu tiên:**
1. Gắn `distractors` cho câu `CHOICE` đã có của KP cha khi lời giải (`explanation`) nói rõ vì sao đáp án sai đó sai và khớp
   mã. Khuôn V46: `UPDATE question_versions SET answer_spec = answer_spec || '{"distractors": {...}}'::jsonb WHERE id = '...'
   AND answer_spec->>'correct' = '...'` (điều kiện theo đáp án đúng để không gắn nhầm câu đã đổi).
2. Chưa đủ thì **thêm bộ luyện mới** cho KP cha (khuôn V70 `V70__writing_criterion_practice_sets.sql`): gói `PRACTICE_SET`
   đã xuất bản, mã gói `PS-TRAP-<MÃ_LỖI>-<n>`, 4–5 câu/bộ, mỗi câu gắn KP cha (`question_knowledge_points`, weight 1.00),
   có `explanation`, `difficulty`, và ≥ 2 câu có `distractors` mang mã X; câu T/F/NG không cần ghi `distractors` (V79/phase 3
   tự điền — nhưng migration chèn thẳng SQL thì **phải ghi** vì không đi qua `AddQuestionVersionUseCase`).
3. Mỗi câu mới viết nội dung tự soạn, đúng kiến thức KP cha, không chép đề thi thật.

**Chất lượng dữ liệu** (thêm sau nghiệm thu lần 1, xem `reports/acceptance-phase-02-03-09-content-report.md`):
- Hai bộ của cùng một mã phải gồm **câu khác nhau** (không dùng chung một bảng câu mẫu cho cả hai bộ): mỗi bộ 2 câu bẫy + ≥ 2
  câu không bẫy, khác hẳn bộ kia. Cần ≥ 8 câu mẫu cho mỗi mã có 2 bộ mới; `INFERENCE_UNSUPPORTED` chỉ thêm 1 bộ (4 câu) và phải
  khác bộ có sẵn.
- Đáp án đúng của câu trắc nghiệm phải **xen kẽ** `A`/`B`/`C`; trong một bộ từ 3 câu trở lên không để một khóa chiếm quá một nửa;
  bẫy mang mã nằm ở các lựa chọn sai khác nhau.
- Bộ `TFNG_*` dùng câu T/F/NG cho cả bộ (có ít nhất một câu đúng `TRUE`, không bẫy), không trộn câu trắc nghiệm không liên quan.
- Test `ErrorTrapCoverageMigrationTest` thêm hai assert: không có hai câu của hai bộ cùng mã trùng `stem`; trong mỗi bộ trắc nghiệm
  từ 3 câu trở lên đáp án đúng không cùng một khóa.

**Không** sửa migration cũ; không đổi `correct` của câu đã có; không gắn câu vào KP con.

Test cũ ghim số đếm của dữ liệu seed khác (câu hỏi, bộ luyện, KP, `distractors`) có thể đổi số vì dữ liệu mẫu mới: xử lý như
phase 2 (ghim vào `Flyway.target` đúng phiên bản nó viết cho, hoặc giới hạn phạm vi đếm; giữ nguyên ý định và assert; ghi từng
test đã đổi vào báo cáo). `DistractorSeedMigrationTest` đã ghim ở V78 từ phase 2.

## Architecture
- Một migration `V80__seed_error_trap_practice_data.sql` (kiểm số trước; nếu quá dài thì tách `V80`, `V81` theo skill),
  mở đầu bằng comment: dữ liệu mẫu do AI soạn, chờ giám khảo IELTS duyệt; mỗi `UPDATE` gắn `distractors` có điều kiện đáp án đúng.
- UUID cố định theo dải riêng (như các seed khác) để chạy lại không trùng.
- `learning.free-practice.reserve` giữ gói chưa xem cho bài ôn: ngưỡng 2 bộ ở trên đã tính dự trữ này; nếu cấu hình dự trữ
  > 1 thì tăng ngưỡng tương ứng và ghi vào báo cáo.

## Related Code Files
- Create: `reports/seed-trap-scope.md` (phạm vi mã, số liệu trước/sau)
- Create: `services/content-service/src/main/resources/db/migration/V80__seed_error_trap_practice_data.sql`
- Create: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapCoverageMigrationTest.java`
- Delete: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapBaselineDataTest.java`
  (script đo của phase 1; số liệu đã nằm trong `reports/baseline-data.md`)
- Read (khuôn): `V46__seed_distractors_reading_listening.sql`, `V70__writing_criterion_practice_sets.sql`, `V30__seed_reading_practice_demo.sql`

## Implementation Steps
1. Từ số liệu phase 1 (`reports/baseline-data.md`) lập `reports/seed-trap-scope.md`: bảng mã × KP cha × số bộ hiện có ≥ 2 câu
   bẫy × số cần thêm; danh sách ngoài phạm vi và lý do.
2. **Đỏ trước** — `ErrorTrapCoverageMigrationTest` (chạy toàn bộ Flyway): với mỗi mã trong phạm vi (truy vấn bằng đúng định
   nghĩa phạm vi ở trên, không hard-code), đếm bộ đủ điều kiện của KP cha có ≥ 2 câu bẫy → ≥ 2; T/F/NG tính riêng từng bẫy;
   mọi câu có `distractors` trỏ khóa là lựa chọn thật của câu và mã ACTIVE; không câu nào gắn vào KP con.
   `PracticeSetCoverageMigrationTest` vẫn xanh.
3. Soạn migration theo thứ tự ưu tiên (gắn trước, thêm bộ sau); chạy test đến khi xanh.
4. Cập nhật `reports/seed-trap-scope.md` với số liệu sau.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] `ErrorTrapCoverageMigrationTest` xanh: mọi mã trong phạm vi đạt ngưỡng, hai bộ cùng mã không trùng câu, đáp án đúng xen kẽ.
- [ ] Báo cáo phạm vi ghi đủ mã ngoài phạm vi và lý do.
- [ ] Test content cũ xanh.

## Risk Assessment
- Nội dung AI có thể sai chuyên môn: đánh dấu chờ duyệt; người nghiệm thu đọc mẫu ngẫu nhiên ≥ 10 câu (đáp án đúng, mã lỗi
  của đáp án sai khớp lời giải).
- Số mã trong phạm vi lớn hơn dự kiến (> 15) → dừng và báo kèm bảng phạm vi trước khi soạn.
