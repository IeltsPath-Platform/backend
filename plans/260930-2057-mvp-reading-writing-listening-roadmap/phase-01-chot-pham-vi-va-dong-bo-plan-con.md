---
phase: 1
title: "Chốt phạm vi và đồng bộ plan con"
status: completed
priority: P1
dependencies: []
effort: "0.5 ngày"
---

# Phase 1: Chốt phạm vi và đồng bộ plan con

## Overview

Chỉ sửa tài liệu và plan, không sửa code. Đưa tài liệu thiết kế MVP, nhãn phiên bản database và các plan con về cùng một phạm vi
và thứ tự merge của `plan.md`.

## Requirements

- Tài liệu MVP khớp plan 1640 sau Validation Session 1 và phạm vi ở `plan.md`.
- Mỗi plan con ghi đúng số migration và nhãn tài liệu của bảng "Thứ tự merge và số migration".
- Không đổi quyết định đã chốt trong plan con; chỉ đồng bộ số, đường dẫn, phạm vi.

## Related Code Files

- Modify: `.sdd/database/mvp-database.md`, `.sdd/database/DATABASE_V5.md` (dòng V5.3 §0), `.sdd/specs/SERVICE_ARCHITECTURE_V3.md`
  (đoạn "Bổ sung 2026-09-30")
- Modify (plan): `plans/260929-1640-lesson-learning-pipeline-mvp/phase-08-tai-lieu-va-don-dep.md`,
  `plans/260930-0737-lesson-writing-task2-essay/phase-05-tai-lieu.md`, `plans/260930-0851-listening-topic-audio-lessons/phase-02-content-media-url-va-seed-listening.md`,
  `plans/260930-1006-reading-question-hints/phase-01-contract-va-content-hint.md`, `plan.md` của 0737, 0812, 0851, 1006

## Implementation Steps

1. **`.sdd/database/mvp-database.md`** (sửa, không viết lại):
   - Tiêu đề và §"Phạm vi MVP": "AI Learning MVP: tạo path, cập nhật path, điều phối làm bài, chấm Writing". Tutor: code giữ
     nguyên, app MVP không gọi. Bỏ bước 1 của "Thứ tự làm" (gỡ router `/tutor`, `/practice`, startup recovery, quota).
   - Dòng 91, 386, 455: snapshot `mastery_path_knowledge_point_details` **vẫn ghi** (phase 5 của 1640 giữ cho tutor).
   - Dòng 93: bỏ đề xuất dùng cây topic; thứ tự lấy từ `/internal/learning-content/topic-sequence` (topic có bài và có đề).
   - Dòng 99–104, 431: `topic_progress` không có cột `status`, chỉ `passed_at`; API vẫn trả `status` suy ra khi đọc.
   - Dòng 38, 282, 394: consumer chỉ ghi `passed_at`, không "mở topic kế".
   - Dòng 339, 389, 403: bằng chứng bài tập chỉ ghi ở **lần nộp đầu** của khối.
   - Dòng 202, 237, 392, 434: `path_review_items.status` có `SKIPPED` (trượt 3 set); response có `reviewStatus`.
   - Dòng 106–112, 377–378: app MVP chỉ dùng `/topics` và API bài học; `/status`, `/progress` là của tutor.
   - Dòng 423: link migration thành `../../services/ai-learning-service/migrations/V0_1__create_v5_mastery_tables.sql`.
   - Thêm mục Writing: bảng `lesson_writing_submissions` (V12), luồng nộp essay (kiểm số dư → LLM → debit), trừ 3 point.
   - Thêm mục Listening (audio `mediaUrl`, transcript ẩn tới khi đạt) và gợi ý Reading (`hint` sau lần sai đầu), chỉ phần
     ảnh hưởng tới bảng và API của ai-learning.
   - Đầu file: "Danh sách cột chính thức ở `DATABASE_V5.md` §7.21–§7.26; tài liệu này là hướng dẫn luồng API ↔ bảng cho MVP."
   - Số bảng nghiệp vụ MVP: 11 + `lesson_writing_submissions` = 12 (cộng `mastery_interactions` giữ vật lý).
2. **`DATABASE_V5.md` §0 dòng V5.3** và **`SERVICE_ARCHITECTURE_V3.md`**: bỏ phần "chọn gói theo dạng câu và độ khó", cột
   `wrong_question_types`, `difficultyReason`; ghi "hoãn ngoài MVP" (tham chiếu plan đã sửa lúc xóa 0908). Giữ phần Listening.
3. **Plan 1640 phase 8**: đổi đường dẫn `services/ai-learning-service/mvp-database.md` thành `.sdd/database/mvp-database.md`.
4. **Số migration và nhãn**:
   - 0737 phase 5: nhãn `V5.3` → `V5.5` (Writing Task 1 + Task 2).
   - 0851 phase 2: `V<n>__seed_listening_demo.sql` → `V11__seed_listening_demo.sql`.
   - 1006 phase 1: `V<n>__question_version_hint.sql` → `V12__question_version_hint.sql`; nhãn giữ `V5.4`.
   - 0737 (V9, V12) và 0812 (V10) giữ nguyên.
5. **Frontmatter**: 0737 `blocks: [260930-0812-lesson-writing-task1-academic]` giữ; 0812 thêm `blocks: [260930-0851-listening-topic-audio-lessons]`;
   0851 thêm `blockedBy: [..., 260930-0812-lesson-writing-task1-academic]` (thứ tự merge và hàm kiểm media dùng chung);
   1006 thêm `blockedBy: [..., 260930-0851-listening-topic-audio-lessons]`.
6. **Quy ước chung cho mọi plan con** (ghi vào mục Dependencies của 0737, 0812, 0851, 1006):
   - Kiểm media: một hàm ở content domain, một mã lỗi `INVALID_MEDIA_REFERENCE`. 0812 vào trước nên viết hàm (nhận `https://`
     và `data:image/svg+xml;base64,…` cho IMAGE); 0851 mở rộng thêm key + `CONTENT_MEDIA_BASE_URL` cho AUDIO. DB lưu
     `media_reference` (giá trị gốc); content resolve thành `mediaUrl` trong payload nội bộ; học viên nhận `mediaUrl`. Chốt ở phase 2 khi validate 0812.
   - Test tập key của response học viên: kiểm "không chứa key cấm" (`answerSpec`, `explanation` trước khi đạt, `chartFacts`,
     `transcript` trước khi đạt, `solution`) thay vì so bằng đúng một tập key, để plan sau thêm trường không làm vỡ test plan trước.

## Success Criteria

- [x] `mvp-database.md` không còn: `status` lưu ở `topic_progress`, "khối chưa từng đạt", review chỉ `PENDING|DONE`, đề xuất gỡ
      router tutor, dùng cây topic làm thứ tự, link `migrations/…` hỏng.
- [x] Grep toàn bộ plan con: không còn `V<n>` cho migration, không còn hai plan cùng một số migration hay cùng một nhãn V5.x.
- [x] Không file code nào đổi trong phase này.

## Kết quả (2026-10-01)

- Làm đủ bước 1–6. Thêm ngoài danh sách (cùng lượt, sau review sẵn sàng giao agent code,
  `plans/reports/review-261001-0116-mvp-plans-codex-readiness-report.md`):
  - `seed-content.md` (plan này): nội dung seed trích từ trang demo bằng script, dùng chung cho 1640, 0737, 0812, 0851, 1006;
  - `DATABASE_V5.md` §7.17, §7.21, §7.23–§7.25 theo quyết định đã chốt (`passed_at`, lần nộp đầu, `SKIPPED`,
    `has_practice_set`, hoãn chọn gói theo độ khó); plan 1640 phase 8 chỉ còn đánh dấu mục đã có migration;
  - plan 1640 Validation Session 3 (KP không có gói luyện, seed, `kind`).
- `SERVICE_ARCHITECTURE_V3.md` là kiến trúc đích của MVP (chia service thuộc MVP, chốt 2026-10-01); khối
  `question_versions.difficulty` sửa thành "hoãn ngoài MVP".

## Risk Assessment

- **Sửa tài liệu thiết kế trước khi code:** tài liệu mô tả đích; ghi rõ "chưa có migration" ở từng mục như hiện tại.
- **Đổi frontmatter làm lệch `ck plan status`:** chạy `ck plan status` cho từng plan sau khi sửa.
