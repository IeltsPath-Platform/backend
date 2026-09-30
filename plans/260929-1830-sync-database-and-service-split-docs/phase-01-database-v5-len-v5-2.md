---
phase: 1
title: DATABASE_V5 lên V5.2
status: completed
priority: P1
dependencies: []
effort: 0.5 ngày
---

# Phase 1: DATABASE_V5 lên V5.2

## Overview

Sửa `.sdd/database/DATABASE_V5.md` (2.678 dòng) theo D1–D11 trong `plan.md`. Chỉ sửa các mục liệt kê dưới đây. Giữ văn phong bảng "Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa" của file.

## Requirements

- Mỗi thay đổi ghi nhãn "V5.2" ngay tại cột hoặc đoạn đổi (như file đang ghi "V5.1").
- Không xóa lịch sử: đoạn V5.1 bị thay thì sửa thành luật mới và thêm câu ngắn "V5.2 thay: …".
- Tên cột và bảng lấy đúng từ plan triển khai (phase 3 V7, phase 5 V10, phase 6 V11).

## Sửa theo mục

| Mục (dòng hiện tại) | Sửa |
| --- | --- |
| §0 Lịch sử (`:25`) | Thêm dòng **V5.2 (2026-09-29, thiết kế đích)**: nhiều mã đề qua `content_packages.topic_id` (bỏ `topics.test_package_id`); giữ `LESSON`; KP bỏ band; bài ôn và luyện thêm bằng gói `PRACTICE_SET`; AI Learning thêm `path_review_sets`, `topic_test_assignments`, bỏ `mastery_path_knowledge_point_bands`; path theo user; assessment không thêm cột |
| §1.1 (`:49-67`) | Câu cuối: "…thiết kế V5.1 (+5 Content, +4 AI Learning) và V5.2 (+2 AI Learning, −1 AI Learning)" |
| §4.1 `topics` (`:480-501`) | Bỏ dòng `test_package_id`. Đoạn mở đầu: thứ tự học = topic có ≥ 1 bài PUBLISHED và ≥ 1 gói `TOPIC_TEST` PUBLISHED, theo `sort_order`, lưu theo học viên ở `topic_progress.sequence_order` (§7.21); xếp lại theo `parent_topic_id` hoãn. `required_feature_key`: thêm "chưa dùng trong MVP". Band topic giữ nguyên |
| §4.2 `knowledge_points` (`:504-527`) | Xóa dòng `band_min?`, `band_max?` và đoạn "Band hiệu lực của KP…" (`:525`); thay bằng một câu "V5.2: KP không còn band; API không trả `bandMin`/`bandMax`/`effectiveBand*`" |
| §5.1 `content_packages` (`:576-593`) | Đoạn mở đầu: giữ `LESSON` (V5.1 định bỏ, V5.2 giữ vì luồng reading của tutor còn dùng). CHECK `package_type` thêm lại `LESSON`. Thêm dòng **`topic_id?`**: uuid, FK → `topics`, bắt buộc khi `TOPIC_TEST` (CHECK); một topic nhiều mã đề. Ghi chú: `PRACTICE_SET` có ≥ 3 câu là ngân hàng câu cho bài ôn và luyện thêm; `TOPIC_TEST` không tạo qua API (seed) |
| §5.13–§5.17 bài học (`:865-936`) | §5.16: câu trong khối bài tập không trùng câu đề cuối **và gói luyện tập** của cùng topic. §5.17: ghi "ai-learning chép KP của bài vào `lesson_progress.knowledge_point_ids` khi học viên mở hoặc nộp bài" |
| §6.1 `assessment_attempts` (`:939`) | `attempt_type` suy từ loại gói (`TOPIC_TEST` → `TOPIC_GATE`, …; `PRACTICE_SET` bị từ chối); `expires_at` từ luật gói; `learning_goal_id` luôn NULL từ V5.2 |
| §6.3 `attempt_items` (`:980-996`) | `question_snapshot`: stem và options, không đáp án. `answer_snapshot`: `{answerSpec, explanation, maxScore}` chép từ content lúc tạo attempt; không bao giờ trả học viên |
| §6.6 `assessment_results` (`:1048`) | Đề tự chấm được: tự tạo version 1 `COMPLETED` khi nộp; điểm cho học viên tính từ `item_results`; lời giải chỉ trả khi ≥ 70% |
| §7.1 `mastery_paths` (`:1232-1258`) | Index: bỏ `UNIQUE (user_id, learning_goal_id) WHERE …`, thay **`UNIQUE (user_id)`**. Đoạn giải thích: một path mỗi học viên; `learning_goal_id` giữ cột, không dùng để tìm path; tạo theo `sort_order`, không LLM |
| §7.5 `mastery_learning_evidence` (`:1329`) | `source` thêm `lesson_exercise`, `review_set`; unique (`path_id`, `source`, `source_reference_id`) áp cho cả ba nguồn; reference tất định theo `request_id` |
| §7.15 nhận kết quả thi (`:1617`) | Goal tùy chọn; pending theo `user_id`; PLACEMENT chỉ ghi version, không bằng chứng, không test-out; TOPIC_GATE: tra `topic_test_assignments`, đánh dấu đã dùng, PASSED, rồi luật bài ôn, **trong cùng transaction** trước khi ghi version đã áp |
| §7.16 transaction (`:1639`) | Thêm: mọi thay đổi tiến độ bài học, bài ôn, giao mã đề chạy trong path transaction, cùng connection |
| §7.17 snapshot (`:1658-1668`) | Xóa dòng `mastery_path_knowledge_point_bands` và phần "giữ lại band" trong đoạn `:1668`; ghi "V5.2 xóa bảng" |
| §7.21 `topic_progress` (`:1734`) | Thêm `sequence_order` (integer, NOT NULL); bỏ `best_test_score_percent`. Đoạn cuối: tạo dòng cho mọi topic trong thứ tự học ở `GET /topics`; PASSED một chiều; topic kế theo `sequence_order` |
| §7.22 `lesson_progress` (`:1749`) | Thêm `lesson_sort_order` (int), `knowledge_point_ids` (uuid[], GIN), `passed_block_ids` (text[]); bỏ `best_score_percent` |
| §7.23 `lesson_exercise_submissions` (`:1762`) | Bỏ `correct_count`, `total_count`, `score_percent`; thêm `block_passed` (boolean), `response` (jsonb, trả lại khi nộp trùng). Đoạn cuối: chưa đạt chỉ trả đúng/sai; đạt thì trả đáp án và `explanation`; khối đã từng đạt nộp lại không ghi bằng chứng |
| §7.24 `path_review_items` (`:1780`) | Luật: bỏ "và không phải bài vừa xong" (bài vừa xong = **luyện thêm theo mastery**); ngưỡng là setting (mặc định 0.6). `DONE` khi **một gói luyện tập** đạt ≥ 70% (§7.25), không làm lại bài tập cũ. Giới hạn số lần trượt: "chưa chốt" |
| §7.25 (mới) `path_review_sets` | `id` uuid PK; `review_item_id` FK → `path_review_items`; `user_id`; `package_id`, `package_version_id` (logical ref ↗ Content); `assigned_at`; `submitted_at?`; `passed?`; `request_id?` UNIQUE; `response?` jsonb. Partial UQ(`review_item_id`) WHERE `submitted_at IS NULL` (một set mở mỗi bài ôn); INDEX(`user_id`, `package_id`). Chọn gói chưa giao; hết thì gói giao lâu nhất |
| §7.26 (mới) `topic_test_assignments` | `id` uuid PK; `user_id`; `topic_id`; `package_id`, `package_version_id`; `assigned_at`; `consumed_attempt_id?`; `consumed_at?`; `percent?`. Partial UQ(`user_id`, `topic_id`) WHERE `consumed_at IS NULL`. Dùng một lần; mã khác tính theo `package_id` |

## Implementation Steps

1. Đọc lại các mục trong bảng trên và các phase 3–6 của plan triển khai (tên cột).
2. Sửa theo bảng, theo thứ tự trong file (từ trên xuống) để số dòng tham chiếu ít lệch.
3. Thêm §7.25, §7.26 ngay sau §7.24, cùng khuôn bảng.
4. Grep trong file: `test_package_id`, `bỏ loại \`LESSON\``, `band_min` (chỉ còn ở `topics`), `effectiveBand`, `knowledge_point_bands`, `không phải bài vừa xong`, `learning_goal_id) WHERE`. Chỉ còn ở dòng lịch sử hoặc câu "V5.2 thay".

## Success Criteria

- [ ] Mọi mục trong bảng đã sửa; §7.25, §7.26 có đủ cột và ràng buộc.
- [ ] Grep bước 4 sạch.
- [ ] Không đụng mục ngoài danh sách.

## Risk Assessment

- **File dài, số dòng lệch khi sửa:** tìm mục theo tiêu đề (`## 4.1`, …), không theo số dòng.
- **Lệch với plan triển khai khi plan đó đổi tiếp:** phase 3 kiểm chéo; đổi sau này thì sửa cả hai.
