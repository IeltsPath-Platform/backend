---
phase: 2
title: Plan chia service và luồng học
status: in-progress
priority: P1
dependencies: []
effort: 0.5 ngày
---

# Phase 2: Plan chia service và luồng học

## Overview

Sửa hai file trong `plans/260928-2019-architecture-doc-service-split/`:
- `plan.md` (204 dòng): đặc tả chia service;
- `main-learning-pipeline.md` (218 dòng): luật luồng học.

Mục tiêu là khớp D1–D11. Không đổi cách chia service (library-service, giải thể learning-support, …); chỉ sửa phần liên quan tới bài học, đề cuối, assessment, ai-learning.

## Sửa `plan.md` (plan chia service)

| Mục | Đang ghi | Sửa thành |
| --- | --- | --- |
| Bảng service, dòng content-service (`:36`) | "`package_type` bỏ `LESSON`, thêm `TOPIC_TEST`; topic có premium và đề cuối" | "+ bài học; `package_type` thêm `TOPIC_TEST` (giữ `LESSON`); gói `TOPIC_TEST` gắn topic; KP bỏ band" |
| Bảng service, dòng assessment-service (`:38`) | "Tự chấm câu khách quan cho đề `TOPIC_TEST` (không đổi bảng)" | Giữ; thêm "lấy đề từ content, tự quyết `attemptType`, không gọi user-service" |
| Bảng service, dòng ai-learning (`:44`) | "+ 4 bảng; path gắn học viên thay vì goal" | "+ 6 bảng, − 1 bảng band; path gắn học viên; bài ôn và luyện thêm bằng gói câu mới; giao mã đề" |
| Bảng theo service, `ai_learning_db` (`:83,88`) | (21); có `mastery_path_knowledge_point_bands`; tiến độ 4 bảng | **(22)**; bỏ `mastery_path_knowledge_point_bands`; tiến độ thêm **`path_review_sets`**, **`topic_test_assignments`** |
| Chi tiết `content_db` (`:114-123`) | `topics` + `test_package_id`; `package_type` bỏ `LESSON` | Bỏ `test_package_id`; thêm `content_packages.topic_id` (bắt buộc với `TOPIC_TEST`, nhiều mã đề mỗi topic); giữ `LESSON`; `knowledge_points` bỏ band; `lesson_block_questions` không trùng câu đề và gói luyện tập; `required_feature_key` của topic: chưa dùng trong MVP |
| Chi tiết `ai_learning_db` (`:139-145`) | 4 bảng với cột V5.1 | Cột theo DATABASE_V5 §7.21–§7.26 (V5.2); `mastery_paths` và pending theo user; xóa bảng band; dẫn tới DATABASE_V5 thay vì chép lại chi tiết |
| Giao tiếp mới, ai-learning → content (`:154`) | 2 endpoint + "tra `package_version_id` → chủ đề" | 6 endpoint `/internal/learning-content/*` (`topic-sequence`, `topics/{id}/lessons`, `lessons/{id}`, `topics/{id}/test-packages`, `practice-sets/search`, `package-versions/{id}`); bỏ việc tra chủ đề (consumer tra `topic_test_assignments`) |
| Giao tiếp mới, ai-learning → access (`:155`) | Kiểm premium | Xóa dòng; ghi "premium hoãn khỏi MVP" ở Điểm mở |
| Giao tiếp mới (dòng mới) | — | assessment → content `GET /internal/learning-content/package-versions/{id}` (thay `/internal/assessment-content/knowledge-point-mappings`); ghi rõ assessment không còn gọi user-service |
| Giao tiếp mới, event (`:156`) | `package_version_id` tùy chọn; goal tùy chọn | Producer luôn gửi `package_version_id`; `learning_goal_id` = null; consumer thiếu `package_version_id` thì bỏ bước topic |
| Đoạn API học viên (`:158`) | "…nộp bài tập, hoàn thành bài, gợi ý ôn" | "…nộp khối, hoàn thành bài (bài không có bài tập), bài ôn và luyện thêm `/reviews/{id}`, giao mã đề `/topics/{id}/test-assignments`; chưa đạt chỉ trả đúng/sai, không trả `answer_spec`" |
| Quyết định chính, dòng luồng học (`:173`) | "…xếp lại chủ đề chưa học (cùng cha) và chèn bài ôn bắt buộc cho KP yếu" | "…thứ tự học = topic có bài và có đề; KP yếu kèm câu sai thì giao gói câu mới (tính cả bài vừa học = luyện thêm theo mastery); giấu đáp án tới khi đạt; mã đề dùng một lần. Xếp lại topic hoãn" |
| Quyết định chính, dòng chấm (`:174`) | Ba nơi chấm theo `answer_spec` | Giữ; thêm "`PRACTICE_SET` chỉ chấm ở ai-learning; assessment từ chối" |
| Điểm mở (`:184-185`) | "bỏ `LESSON` không ảnh hưởng"; entitlement chưa guard | Sửa thành "giữ `LESSON`"; entitlement và các lỗ access chuyển sang plan bảo mật riêng; thêm "giới hạn số lần trượt gói ôn: chưa chốt" |

## Sửa `main-learning-pipeline.md` (luồng học)

| Mục | Sửa |
| --- | --- |
| §1 Nguyên tắc | Thêm: giấu đáp án tới khi đạt; bài ôn và luyện thêm luôn dùng câu mới; mã đề dùng một lần |
| §2 Sơ đồ tổng | Nhánh "chưa đạt" của khối: làm lại cả khối, chỉ biết đúng/sai. Nhánh "bài xong": nếu KP yếu kèm câu sai → gói luyện thêm (chặn bài kế). Nhánh đề: giao mã đề → nộp → PASSED hoặc giữ → bài ôn |
| §3.2 Bắt đầu học | Thứ tự học = topic có bài và có đề; không goal, không LLM, không band |
| §3.3 Học một bài | Phản hồi: chưa đạt chỉ đúng/sai, đạt mới có lời giải; khối đã đạt nộp lại không ghi mastery |
| §3.4 Đề cuối | Giao mã qua ai-learning (một lần dùng); assessment tự quyết `TOPIC_GATE`; lời giải khi ≥ 70%; làm lại nhận mã khác |
| §3.5 Path đổi | Bỏ "xếp lại chủ đề chưa học" (hoãn); luật giao gói: tính cả bài vừa xong; ví dụ Lan sau L2 được luyện thêm KP1 |
| §4 Luật | Viết lại luật bài ôn theo DATABASE_V5 §7.24 (V5.2); DONE khi gói ≥ 70%; trượt → gói khác; giới hạn số lần: chưa chốt |
| §5 Nguồn cập nhật mastery | Thêm nguồn `lesson_exercise`, `review_set`; placement không ghi; khối đã đạt không ghi |
| §6 Service tham gia | ai-learning không gọi access trong MVP; assessment không gọi user-service; consumer không gọi HTTP |
| §7 Thay đổi bắt buộc | Thay danh sách bằng liên kết tới plan triển khai `260929-1640-lesson-learning-pipeline-mvp` (tránh hai nơi liệt kê) |
| §8 Câu hỏi mở | Bỏ các câu đã chốt; thêm giới hạn số lần trượt gói, ngưỡng 0.6/0.9 |
| Đầu file | Dòng "Cập nhật 2026-09-29 (V5.2)": tóm tắt những gì thay so với bản sáng 29/9 |

## Implementation Steps

1. Đọc đầy đủ hai file (`plan.md` 204 dòng, `main-learning-pipeline.md` 218 dòng).
2. Sửa `plan.md` theo bảng; giữ nguyên mọi phần về library-service, user-service, notification, game, community.
3. Sửa `main-learning-pipeline.md` theo bảng; sơ đồ ở §2 sửa tại chỗ, giữ định dạng hiện có.
4. Grep hai file: `test_package_id`, `bỏ \`LESSON\``, `knowledge_point_bands`, `gợi ý ôn`, `xếp lại chủ đề chưa học` (chỉ còn ở ngữ cảnh "hoãn"), `entitlement` (chỉ còn ở ghi chú plan bảo mật), `không phải bài vừa xong`.

## Success Criteria

- [ ] Mọi mục trong hai bảng đã sửa.
- [ ] Grep bước 4 sạch.
- [ ] Phần chia service không liên quan bài học giữ nguyên.

## Risk Assessment

- **`plan.md` là nguồn cho phase 1 của plan kiến trúc** (§12 trong `system-architecture.md`): sửa xong mới chạy phase đó (đã ghi phụ thuộc).
- **Sơ đồ §2 dài, dễ lệch khi sửa:** so từng nhánh với §3 sau khi sửa.
