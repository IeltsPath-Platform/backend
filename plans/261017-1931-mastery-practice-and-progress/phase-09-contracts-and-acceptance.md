---
phase: 9
title: "Contract, tài liệu, nghiệm thu"
status: pending
priority: P1
dependencies: [1, 2, 3, 4, 5, 6, 7, 8]
---

# Phase 9: Contract, tài liệu, nghiệm thu

## Requirements

- **Contract** trong `docs/contracts/`:
  - sửa `knowledge-tree-v1.md` (trường KP mới), `learner-report-v1.md` (tier `INSUFFICIENT`, khối `skills`, band, target),
    `learning-content-internal-v1.md` nếu learning đọc trường KP mới, `learning-staff-area-v1.md` (cờ `INACTIVE`, thống
    kê hoạt động, khối weekly);
  - tạo `free-practice-v1.md`, `progress-snapshots-v1.md`, `flashcard-srs-v1.md`, `content-scheduled-publish-v1.md`,
    `user-internal-v1.md` (learning goal).
- **Tài liệu:**
  - AGENTS.md §3.8: công thức mastery mặc định, port DeepTutor giữ lại, luyện tự do không chặn, dự trữ gói; §3.5: gọi
    learning → user; §3.6: nhóm cấu hình mới;
  - `docs/system-architecture.md`, `.sdd/database/DATABASE_V5.md` (bảng và cột mới);
  - README của library (SRS) và content (hẹn giờ).
- **Kịch bản demo** (thêm vào kịch bản của plan 1):
  1. Học viên có 2 bằng chứng thấy "Chưa đủ dữ liệu"; thêm bằng chứng ngày khác thì lên tier thật.
  2. Luyện tự do một KP, trượt, không bị khóa bài; KP chỉ còn 2 gói thì `FREE_PRACTICE_EXHAUSTED`.
  3. Báo cáo có 6 kỹ năng, band ước tính, khoảng cách tới mục tiêu.
  4. Chạy job snapshot, xem báo cáo tuần và xu hướng.
  5. Ôn 5 thẻ flashcard, thẻ đúng lên hộp 2.
  6. Học viên vắng 3 ngày (dựng dữ liệu) có cờ `INACTIVE`; nộp bài thì cờ đóng.
  7. Hẹn giờ xuất bản một gói, gói tự xuất bản.
- **Kiểm chứng:** `mvn -B -ntp verify` cả reactor (lần này gồm library), Testcontainers không skip;
  `docker compose config --quiet`. Ghi kết quả vào `reports/acceptance.md` của plan này, không ghi đã chạy khi chưa chạy.

## Success Criteria

- [ ] Không contract nào lệch controller.
- [ ] Demo 1–7 chạy trên Compose sạch.
- [ ] `mvn -B -ntp verify` xanh, không skip.
