---
phase: 9
title: "Contract, tài liệu, nghiệm thu"
status: in_progress
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

- [ ] Không contract nào lệch controller. (phần learning đã viết và đối chiếu code; còn phần của Codex)
- [ ] Demo 1–7 chạy trên Compose sạch.
- [ ] `mvn -B -ntp verify` xanh, không skip.

## Tiến độ (2026-10-18)

- Xong (phần learning, Claude): `learner-report-v1.md` (khối `skills`, `focus`, `estimatedBand`, `target`, tier
  `INSUFFICIENT` đã có); tạo `progress-snapshots-v1.md` và `free-practice-v1.md`; `learning-staff-area-v1.md` (cờ `INACTIVE`,
  thống kê hoạt động, khối `weekly`); AGENTS.md §3.5, §3.6, §3.8; `.sdd/database/DATABASE_V5.md` (§7.15, §7.22–§7.28).
- Chưa làm (thuộc Codex): `knowledge-tree-v1.md` (trường KP mới, đã được Codex sửa), `flashcard-srs-v1.md`,
  `content-scheduled-publish-v1.md`, `user-internal-v1.md` (learning goal), README của library và content.
- Chưa làm: `mvn -B -ntp verify` cả reactor (gồm library) và `docker compose config --quiet`; 7 kịch bản demo trên Compose;
  `reports/acceptance.md` của plan này. `docs/system-architecture.md` không tồn tại trong repo.
- Không ghi "đã chạy" cho các mục trên khi chưa chạy.
