---
title: Đồng bộ đặc tả DATABASE_V5 và plan chia service
description: >-
  Đưa .sdd/database/DATABASE_V5.md lên V5.2 và sửa plan chia service (plan.md +
  main-learning-pipeline.md) theo các quyết định ngày 29/9 của pipeline học,
  trước khi viết code.
status: in-progress
priority: P1
branch: feat/main-follow
tags:
  - docs
  - database
  - architecture
  - lesson
blockedBy: []
blocks:
  - 260929-1640-lesson-learning-pipeline-mvp
  - 260928-2019-architecture-doc-service-split
created: '2026-09-29T11:28:55.256Z'
createdBy: 'ck:plan'
source: skill
---

# Đồng bộ đặc tả DATABASE_V5 và plan chia service

## Overview

Ba tài liệu đặc tả đang mô tả thiết kế V5.1 (chốt sáng 29/9). Các quyết định chiều 29/9 đã làm chúng lỗi thời:
- `.sdd/database/DATABASE_V5.md`: đặc tả schema toàn hệ thống.
- `plans/260928-2019-architecture-doc-service-split/plan.md`: cách chia service và bảng theo service.
- `plans/260928-2019-architecture-doc-service-split/main-learning-pipeline.md`: luật luồng học.
- **Thêm (người dùng yêu cầu 29/9):** `.sdd/specs/SERVICE_ARCHITECTURE_V2.md` → đổi tên và nâng thành `SERVICE_ARCHITECTURE_V3.md`. Gồm cả phần chia service (library-service, giải thể learning-support) lẫn luồng học bài. **Đã làm.**

Plan này chỉ sửa tài liệu, không sửa code hay migration. Làm xong thì plan triển khai `260929-1640-lesson-learning-pipeline-mvp` code theo đặc tả đã khớp.

Nguồn quyết định (đọc trước khi sửa):
- Plan triển khai: [plan.md](../260929-1640-lesson-learning-pipeline-mvp/plan.md) (bảng "Quyết định áp dụng", mục Red Team Review), phase 3 (content), phase 4 (assessment), phase 5 (V10), phase 6 (V11).
- [Brainstorm giấu đáp án, bài ôn, mã đề](../reports/brainstorm-260929-1611-lesson-feedback-review-question-pool-report.md)
- [Scout report](../reports/scout-260929-1316-lesson-pipeline-change-map-report.md), mục "Đã chốt".

## Thay đổi cần phản ánh (so với V5.1)

| # | Quyết định | Ảnh hưởng tài liệu |
| --- | --- | --- |
| D1 | Đề cuối: nhiều mã đề mỗi topic, gói gắn topic qua `content_packages.topic_id`; bỏ `topics.test_package_id` | DB §4.1, §5.1; plan chia service (chi tiết content) |
| D2 | Giữ `LESSON` trong `package_type`, chỉ thêm `TOPIC_TEST` | DB §0, §5.1; plan chia service (bảng service, điểm mở) |
| D3 | Bỏ band của KP (cột, API); band topic giữ | DB §4.2, §7.17; plan chia service (bảng ai-learning) |
| D4 | Bài ôn và luyện thêm dùng gói `PRACTICE_SET` câu mới, không làm lại bài tập cũ; tính cả bài vừa xong | DB §5.1, §5.16, §7.24, §7.25 mới; luồng học §3.5, §4 |
| D5 | Giấu đáp án tới khi đạt; khối đã đạt nộp lại không ghi mastery | DB §7.23; luồng học §3.3, §5 |
| D6 | Mã đề giao qua ai-learning, dùng một lần; consumer tra topic tại chỗ | DB §7.26 mới, §7.15; plan chia service (giao tiếp mới) |
| D7 | Assessment không migration mới: `answer_snapshot` chứa đáp án; tự quyết `attemptType`; không gọi user-service; `learning_goal_id` null | DB §6.1, §6.3, §6.6; plan chia service (bảng service, giao tiếp mới) |
| D8 | Path một mỗi user, pending theo user, xóa `mastery_path_knowledge_point_bands`; evidence thêm 2 nguồn | DB §7.1, §7.5, §7.15, §7.17; plan chia service (bảng ai-learning) |
| D9 | Thứ tự học = topic có bài và có đề, lưu `topic_progress.sequence_order`; xếp lại topic hoãn; PASSED một chiều | DB §4.1, §7.21; luồng học §3.2, §3.5, §4 |
| D10 | Premium và client access hoãn khỏi MVP; lỗ entitlement sang plan bảo mật riêng | DB §4.1 (ghi chú); plan chia service (giao tiếp mới, điểm mở) |
| D11 | Placement không ghi mastery, không test-out; không LLM khi tạo path | DB §7.15; luồng học §3.2, §5 |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [DATABASE_V5 lên V5.2](./phase-01-database-v5-len-v5-2.md) | Completed |
| 2 | [Plan chia service và luồng học](./phase-02-plan-chia-service-va-luong-hoc.md) | In Progress |
| 3 | [Đối chiếu chéo và nối plan](./phase-03-doi-chieu-cheo-va-noi-plan.md) | Pending |

Phase 1 và 2 làm song song được (khác file). Phase 3 cần cả hai.

## Tiêu chí nghiệm thu

- Ba tài liệu không còn: `test_package_id`, "bỏ `LESSON`", band của KP, `mastery_path_knowledge_point_bands`, "không phải bài vừa xong", "làm lại bài tập của bài ôn", unique theo goal, tra `package_version_id → chủ đề` qua HTTP, ai-learning → access trong MVP.
- Tên bảng, cột, endpoint khớp từng chữ với phase 3–6 của plan triển khai.
- DATABASE_V5 có dòng V5.2 trong lịch sử, và số đếm §1.1 ghi đúng số bảng V5.2 thêm.
- Lịch sử quyết định cũ không bị xóa: mục bị thay ghi "V5.2 thay" kèm lý do ngắn.
- Plan triển khai không còn tự sửa ba tài liệu này (tránh sửa trùng).

## Dependencies

- **Chặn** `260929-1640-lesson-learning-pipeline-mvp`: code viết theo đặc tả đã khớp.
- **Chặn** `260928-2019-architecture-doc-service-split`: phase 1 của plan đó ghi §12 "kiến trúc đích" vào `system-architecture.md` từ đặc tả trong `plan.md` của nó, nên cần đặc tả mới trước.

## Câu hỏi mở

- Giới hạn số lần trượt gói ôn (đề xuất 3 lần rồi cho học tiếp và gắn cờ "cần hỗ trợ") chưa chốt. Tài liệu ghi "chưa chốt"; chốt thì sửa §7.24 và luồng học §4.
- Ngưỡng bài ôn 0.6 hay 0.9: tài liệu ghi "setting, mặc định 0.6".
