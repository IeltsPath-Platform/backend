---
phase: 13
title: "Contract, tài liệu và nghiệm thu"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15]
---

# Phase 13: Contract, tài liệu và nghiệm thu

## Trạng thái và ghi chú triển khai

- **Phần learning và tài liệu chung: xong** — contract learning đã rà với controller (`lesson-learning-v1`,
  `lesson-writing-v1` trỏ tới contract mới; `practice-v1` không tồn tại, Practice nằm trong `lesson-learning-v1`),
  `AGENTS.md` §3.6 và §3.8, `DATABASE_V5.md` (§7.15–7.21, đảo quyết định về cây KP), `docs/demo/knowledge-tree-demo.md`,
  `reports/fe-handoff.md`.
- **Chưa làm:** bảng assessment/content/access mới trong `DATABASE_V5.md` (chủ service bổ sung), rà contract phía
  assessment/content, `mvn -B -ntp verify` cả reactor, chạy kịch bản demo trên Compose sạch và `reports/acceptance.md`
  (Codex đang nhận), danh sách phase `completed` cuối cùng, board FigJam.

## Overview

Rà lại toàn bộ thay đổi, cập nhật tài liệu cho khớp code, chạy nghiệm thu đầu-cuối và chuẩn bị bằng chứng trình hội
đồng.

## Requirements

- **Contract** trong `docs/contracts/` (đã khôi phục ở phase 1):
  - Sửa: `assessment-completed-v2.md`, `answer-spec-v1.md` (+ vectors), `lesson-learning-v1.md`, `practice-v1.md`,
    `learning-content-internal-v1.md`.
  - Mới: `knowledge-tree-v1.md`, `learner-report-v1.md`, `diagnostics-v1.md`, `course-score-v1.md`,
    `assessment-grading-v1.md`, `admin-area-v1.md`, `essay-prompt-template-v1.txt`, `human-review-v1.md` (phase 15). Phase 14 cập nhật `lesson-writing-v1.md` (API sửa
  và lịch sử).

  Mỗi contract khớp mapping trong controller; AGENTS.md coi controller là nguồn tham chiếu.
- **AGENTS.md:**
  - bảng module (learning thêm event log, mastery profile, chẩn đoán, điểm khóa, hàng việc; content thêm cây, danh mục
    lỗi, rubric; assessment thêm hàng chờ, đánh giá AI, QA Mentor);
  - mục cấu hình (nhóm `learning.mastery.*`, `learning.flags.*`, `assessment.ai-eval.*`, `assessment.qa.*`);
  - trỏ tới `docs/ai-policy.md`.
- **`.sdd/database/DATABASE_V5.md`:** thêm các bảng mới và ghi rõ đảo quyết định "`topic_prerequisites` không còn trong
  V5". Cây nay ở cấp KP, bảng `knowledge_point_prerequisites`.
- **Kịch bản demo** `docs/demo/knowledge-tree-demo.md`, từng bước gọi API qua Swagger hoặc curl:
  1. R&D xem và sửa cây, thử tạo vòng.
  2. Học viên làm Practice sai → bài ôn → trượt 2 set → chẩn đoán → bài ôn KP gốc.
  3. Báo cáo theo Knowledge ID.
  4. Topic test 50% → topic sau mở và có việc bổ trợ cho EXAMINER.
  5. Course test → giám khảo chấm → điểm tổng → `REMEDIATION` → thi lại → `PASSED`.
  6. Admin đổi trọng số.
  7. Chạy đánh giá AI và xem chỉ số.
  8. R&D chấm lại mẫu để đánh giá Mentor.
- **Bảng bàn giao FE** gom từ các phase vào `reports/fe-handoff.md` trong thư mục plan.
- **Board FigJam:** cập nhật nếu có quyết định nào thay đổi trong lúc làm.

## Implementation Steps

1. Rà từng contract với controller (grep mapping), sửa chỗ lệch.
2. Cập nhật AGENTS.md, DATABASE_V5.md, `docs/ai-policy.md`.
3. Chạy `docker compose up -d --build`, đi hết kịch bản demo, ghi kết quả vào `reports/acceptance.md`.
4. `mvn -B -ntp verify` cả reactor. CI xanh.
5. Đánh dấu các phase `completed` trong plan.md.

## Success Criteria

- [ ] Không còn contract lệch với controller.
- [ ] Kịch bản demo chạy hết trên Compose sạch (`docker compose down -v` rồi `up`).
- [ ] `mvn -B -ntp verify` xanh, Testcontainers không bị skip.

## Risk Assessment

- Tài liệu dễ lệch khi các phase merge rải rác. Mỗi phase đã cập nhật contract của mình; phase này chỉ rà lại.
