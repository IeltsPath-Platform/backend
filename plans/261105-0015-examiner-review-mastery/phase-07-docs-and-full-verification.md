---
phase: 7
title: "Docs and full verification"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5, 6]
---

# Phase 7: Docs and full verification

## Overview
Cập nhật contract và tài liệu cho khớp code đã làm, chạy kiểm chứng toàn bộ. Chỉ ghi điều đã có trong code.

## Requirements
- `docs/contracts/human-review-v1.md`: bỏ "feedback only / no evidence / no reviews"; mô tả: sau `DONE`, Learning áp kết quả (link
  `human-review-completed-v1.md`), evidence `human_review` + `error_trap`, thay evidence AI tiêu chí của cùng bài, bài ôn không chặn,
  `studyPoints` trong GET.
- `docs/contracts/lesson-writing-v1.md` mục "Revisions and Mentor review" và evidence: Mentor review nay đổi evidence tiêu chí của bài.
- `docs/contracts/human-review-completed-v1.md` (phase 1) khớp code cuối.
- `docs/system-architecture.md`: event mới trong danh sách event Assessment → Learning.
- `services/learning-service/README.md`: nguồn `human_review`, cấu hình `learning.mastery.mp1.source-weights.human-review`, `learning.human-review.review-limit`,
  và cách đẩy lại event từ DLQ `learning.human-review-completed.v1.dlq` (Red Team #5).
- Ghi rõ: review đã `DONE` trước khi phát hành **không** được áp (không backfill) (Red Team #7).
- Nếu phase 3 đã merge: ghi mapping mã → KP vào tài liệu error taxonomy nơi đang mô tả (grep `LINKER_MISUSE` trong `docs/`).

## Related Code Files
- Modify: các file docs ở trên.

## Implementation Steps
1. Sửa docs.
2. Kiểm chứng:
   - `mvn -q -pl shared/common-events -am test`
   - `mvn -q -pl services/assessment-service -am test`
   - `mvn -q -pl services/learning-service -am test`
   - `mvn -q -pl services/content-service -am test` (nếu phase 3 đã làm)
   - `mvn -q compile -DskipTests` toàn reactor
   - frontend: `npx vitest run src/features/practice/writing`, `npm run typecheck`, `npm run lint`
   - `docker compose config --quiet`
3. Kiểm tay (nếu chạy được Compose): learner nhờ examiner chấm một bài lesson, examiner đánh dấu `LINKER_MISUSE` và CC thấp →
   cây kiến thức đổi mastery, giỏ ôn có review, trang kết quả có "Cần ôn". Không chạy được → ghi "chưa kiểm tay".
4. Ghi báo cáo: test đã chạy + kết quả thật, test chưa chạy và lý do, lỗi nền.

## Success Criteria
- [ ] Docs không còn câu "feedback only" cho Writing; mọi khẳng định có code tương ứng.
- [ ] Mọi lệnh kiểm chứng xanh hoặc lỗi nền đã ghi.
- [ ] Mỗi tiêu chí nghiệm thu trong `plan.md` có test hoặc ghi rõ chưa kiểm.

## Risk Assessment
- Docs lệch code nếu viết trước khi code xong: chỉ làm phase này sau phase 1–6.
