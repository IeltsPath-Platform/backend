---
phase: 8
title: "Seed demo, docs và verification"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5]
---

# Phase 8: Seed demo, docs và verification

## Overview
Đưa vào vài đề mẫu để chạy thử từ đầu đến cuối (catalog → làm bài → nộp → lịch sử), cập nhật tài liệu và chạy kiểm chứng toàn bộ.
Hiện không có đề nào để hiện trong thư viện (0 `MOCK_TEST`, 0 câu `EXAM`), và không có API soạn câu hỏi vào section, nên nội dung demo đi bằng migration như mọi seed hiện có.

## Requirements
- Functional:
  - Migration `V29__seed_practice_test_demo.sql` (content) tạo, theo mẫu seed hiện có (UUID cố định, `status = 'PUBLISHED'`, version 1):
    - Reading, nguồn `IELTS_SPACE_PRO`: một đề `PRACTICE_TEST` **3 part** (mỗi part một passage, 4-5 câu, 2-3 nhóm gồm `GAP_FILLING` và `YES_NO_NOT_GIVEN` hoặc `MATCHING_INFORMATION`).
    - Reading, nguồn `C10_C20`: một đề **1 part** (để thử `PART` khác nguồn).
    - Listening, nguồn `IELTS_SPACE_PRO`: một đề **2 part** có audio (dùng khóa object key theo bảng audio hiện có; thiếu `CONTENT_MEDIA_BASE_URL` thì Listening trả lỗi như seed Listening cũ, ghi chú trong README).
    - Mỗi section có `part_no`, `time_limit_seconds`, ảnh bìa (`content_assets` loại `IMAGE`), nhóm câu hỏi đủ; câu hỏi `purpose = 'EXAM'`, `answer_spec` chỉ `CHOICE`/`FILL`.
  - Nội dung (passage, câu hỏi, đáp án, giải thích) **tự viết mới**, ngắn, đúng văn phong seed cũ; không chép từ đề hay trang bên ngoài. Chép toàn văn vào Verification để chủ dự án duyệt trong PR.
  - Tài liệu cập nhật (chỉ phần mô tả khớp với thay đổi):
    - `docs/system-architecture.md`: content có thư viện đề (catalog, nguồn, nhóm câu hỏi), assessment có phạm vi attempt, lịch sử, lượt làm; bảng route mới; ghi rõ khóa PRO còn là nhãn khi chưa làm phase 6.
    - `docs/fe-main-flow-guide.md`: một mục luồng thư viện đề: gọi `POST /api/content/practice-tests/search`, ghép `progress`/`stats`, bắt đầu attempt với `sectionIds`,
      hết giờ thì gọi `submit` (không gọi `expire`), lời giải có ngay sau khi nộp.
    - `services/content-service/README.md` và `services/assessment-service/README.md`: endpoint mới, bảng/cột mới, `CONTENT_MEDIA_BASE_URL` cho ảnh bìa.
    - `docs/contracts/learning-content-internal-v1.md`: nếu có mô tả payload `package-versions/{id}`, thêm `partNo`, `timeLimitSeconds`, `groups`, `groupId`; nếu không, mô tả ở README content.
    - `AGENTS.md`/`CLAUDE.md`: chỉ sửa nếu một dòng dữ kiện trong đó trở nên sai (ví dụ danh sách loại gói); không thêm quy tắc mới.
- Non-functional: seed idempotent theo cách các seed cũ làm; không đụng dữ liệu hiện có.

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V29__seed_practice_test_demo.sql`,
  test `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeTestSeedTest.java` (Testcontainers)
- Modify: các file tài liệu liệt kê trên; test cũ có kỳ vọng số đếm bị đổi bởi seed (xem dưới)

## Implementation Steps
1. **Test đỏ** `PracticeTestSeedTest`: ba đề đủ `part_no` không trùng, mọi section có nhóm và mọi câu thuộc nhóm; đề `PUBLISHED` có version hiện hành; câu hỏi `EXAM` và tự chấm được;
   `questionsUsedElsewhere` không báo trùng; catalog (`JdbcPracticeTestCatalogReader`) trả đúng số thẻ ở cả `FULL` và `PART`.
2. Viết seed. Chạy toàn bộ test content.
3. Test cũ đỏ vì số đếm do seed này thêm (ví dụ các test đếm câu hỏi bị "reserved for learning" trong `LessonPipelineSeedTest`): sửa **chỉ con số/danh sách**, theo luật số 6 của `plan.md`, ghi từng test vào Verification.
4. Cập nhật tài liệu.
5. Chạy kiểm chứng:
   - `docker compose config --quiet`
   - `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test`
   - `mvn -q compile -DskipTests` (cả reactor, không `clean` khi có service chạy từ IDE)
   - Chạy thật (khi có Docker): `docker compose up -d postgres rabbitmq`, khởi động config-server → eureka → gateway → content → assessment → learning, rồi qua Gateway: search `PART` và `FULL`,
     bắt đầu attempt với `sectionIds`, lưu đáp án, nộp, đọc kết quả, `progress`, `stats`, `history`. Chép lệnh và kết quả (không chép token hay secret) vào Verification.
6. Điền mục Verification trong `plan.md`: lệnh đã chạy, test bị skip vì thiếu Docker, test cũ đã đổi kỳ vọng (cũ → mới, lý do), nội dung seed.

## Success Criteria
- [ ] Toàn bộ acceptance criteria trong `plan.md` đạt, có bằng chứng lệnh trong Verification.
- [ ] Thiếu Docker thì ghi rõ test Testcontainers nào bị skip; không tuyên bố đã chạy.
- [ ] Tài liệu khớp với hành vi thật (route, mã lỗi, field); không còn mô tả "chưa có thư viện đề".
- [ ] `git status` chỉ chứa file thuộc plan này; không có `.env`, secret hay `graphify-out/`.

## Risk Assessment
- Seed làm đổi số đếm ở test seed cũ; phạm vi sửa kỳ vọng bị giới hạn ở luật số 6, vượt thì `BLOCKED`.
- Audio demo cần file mp3 thật ở bucket; nếu không có, đề Listening demo chỉ kiểm được ở mức dữ liệu. Ghi rõ trong Verification thay vì giả lập thành công.
- Rollback: seed là migration mới; gỡ bằng migration xóa dữ liệu demo (cần duyệt vì là xóa dữ liệu), không sửa V29 đã áp dụng.

## Câu hỏi chưa giải quyết
- Bảng quy đổi điểm thô → band chưa có nguồn được xác nhận (Q4).
- Số lượng và độ dài đề demo tối thiểu để chủ dự án thấy đủ luồng; mặc định 3 đề như trên.
