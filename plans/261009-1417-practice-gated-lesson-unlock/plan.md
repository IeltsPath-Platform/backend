---
title: "Practice-gated lesson unlock"
description: "Lesson sau chỉ mở khi Practice của mọi lesson trước PASSED; bài ôn lặp đến khi đạt, hết bộ câu mới cho qua"
status: pending
priority: P1
branch: "main"
tags: [learning-service, lesson-gate, practice, review, tdd]
blockedBy: []
blocks: []
created: "2026-10-09T07:37:24.523Z"
createdBy: "ck:plan"
source: skill
---

# Practice-gated lesson unlock

## Overview

Hiện lesson N mở khi các lesson trước COMPLETED, không xét Practice; bài ôn sai 2 set thì bị bỏ qua. Plan đổi luồng:

```text
L(N-1) COMPLETED → Practice: lần nộp đầu đạt? ── có ──→ PASSED → L(N) mở
                            │ không
                            ▼
              bài ôn từng KP yếu: trượt → theory → set khác → ... đến khi ĐẠT
              hết bộ câu chưa lộ → SKIPPED (NO_PACKAGE), coi là xong
                            ▼ mọi bài ôn của lesson xong
                     Practice PASSED → L(N) mở
```

Nguồn quyết định: [brainstorm report](../reports/brainstorm-261009-1417-practice-gated-lesson-unlock-report.md).

Quyết định đã chốt:
- Lesson N mở khi mọi lesson trước trong topic COMPLETED **và** Practice của chúng PASSED. Áp cho mọi lesson (dữ liệu cũ
  chỉ là demo, không xử lý chuyển tiếp).
- Bài ôn trượt không còn bị bỏ qua; lặp set đến khi DONE. Chỉ lần trượt đầu bắt đọc theory + quick-check; các lần
  sau vào thẳng set mới. `SKIPPED` chỉ khi Content hết package chưa lộ.
- Lesson trước còn bài ôn PENDING → mở lesson sau: `403 REVIEW_REQUIRED` + danh sách bài ôn, bất kể skill.
- Practice PASSED nhờ bài ôn chỉ khi mọi bài ôn sinh từ Practice của skill đó đã xong (không còn PENDING).
- Bị chặn vì Practice chưa làm/chưa đạt (không có bài ôn PENDING) → `403 PRACTICE_REQUIRED` + `lessonIds`.
- `maxFailedSets` trong response bài ôn → `null` (không giới hạn).
- Không migration. Không đổi cổng thi topic, mở topic/course, Writing tùy chọn.
- Tính năng "ôn lại KP yếu" (refresh) đã hủy; plan và báo cáo cũ đã xóa.

Chế độ: `--tdd`: mỗi phase viết/sửa test cho hành vi mới trước, thấy đỏ, rồi sửa code.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Review repeats until passed](./phase-01-review-repeats-until-passed.md) | Pending |
| 2 | [Practice clearance waits for every review](./phase-02-practice-clearance-waits-for-every-review.md) | Pending |
| 3 | [Practice-gated lesson gate](./phase-03-practice-gated-lesson-gate.md) | Pending |
| 4 | [Fixtures docs and verification](./phase-04-fixtures-docs-and-verification.md) | Pending |

Thứ tự: 1 → 2 → 3 → 4. Phase 1 và 2 độc lập về file nhưng phase 3 dựa trên luật của cả hai.

## Acceptance criteria

- L1 COMPLETED, Practice chưa làm → mở L2 (đọc, nộp bài tập, nộp essay, hoàn thành): `403 PRACTICE_REQUIRED`,
  `lessonIds=[L1]`; `GET /topics/{id}/lessons`: L2 `LOCKED`.
- Practice L1 đạt lần đầu → L2 `AVAILABLE` và mở được.
- Practice L1 trượt → bài ôn; mở L2 → `403 REVIEW_REQUIRED` liệt kê bài ôn đó; set ôn trượt lần 1 → THEORY, trượt
  lần 2, 3 → vẫn PENDING ở PRACTICE (set mới ngay); đạt → DONE → L2 mở.
- Hai bài ôn cùng skill, một DONE một PENDING → Practice `REQUIRED`, L2 khóa.
- Hết package chưa lộ → bài ôn SKIPPED → Practice PASSED → L2 mở (không ai kẹt).
- `GET /reviews/{id}` trả `maxFailedSets: null`.
- Cổng thi topic, trạng thái topic/course không đổi hành vi.
- `mvn -q -pl services/learning-service -am test` xanh, Testcontainers chạy (không skip).

## Dependencies

- Không chặn/bị chặn bởi plan đang mở (`261007-2315-placement-test-gate` chỉ còn frontend).
- FE: xử lý `PRACTICE_REQUIRED` ở cổng lesson (dẫn tới Practice của `lessonIds`) và `REVIEW_REQUIRED` từ lesson trước
  khác skill (dẫn vào bài ôn); bỏ hiển thị giới hạn số lần trượt; theory chỉ bắt đọc ở lần trượt đầu.
- SRS (file Word): người dùng cập nhật luồng mở lesson và bài ôn.
- Nhóm nội dung: seed có 9 KP chỉ 1 bộ PRACTICE_SET → trượt Practice ở các KP này vẫn qua (ALL_SETS_ATTEMPTED). Cần
  ≥ 3–4 bộ/KP để luật nghiêm có tác dụng.

## Open questions

None.

## Validation Log

### Session 1 (2026-10-09)

Verification Results
- Claims checked: 10
- Verified: 10 | Failed: 0 | Unverified: 0
- Tier: Standard
- Ghi chú: `ReviewResponse` không có `@JsonInclude` cấp record → `maxFailedSets: null` vẫn xuất hiện trong JSON (đổi sang
  `Integer`); `PracticeClearance` chỉ tính review `trigger_kind = 'PRACTICE'` (`JdbcReviewItemRepository` L68, L84);
  chỉ `LessonAccess` gọi `gate.authorize`; `RemediationLadderIntegrationTest` L154 kỳ vọng SKIPPED sau 2 set trượt.

Câu hỏi (2) và quyết định
1. Đọc theory sau set trượt → chỉ lần trượt đầu; các lần sau vào thẳng set mới.
2. Lesson trước còn bài ôn PENDING → `403 REVIEW_REQUIRED` + danh sách bài ôn, bất kể skill (bài ôn có `lessonId`
   thuộc lesson trước trong cùng topic); `PRACTICE_REQUIRED` chỉ khi chưa làm/chưa đạt Practice mà không có bài ôn chờ.

Propagation: plan.md (quyết định, acceptance), phase 1 (luật theory), phase 3 (blocking theo lesson trước, chữ ký gate,
test, rủi ro).

### Whole-Plan Consistency Sweep

- Rà mọi file: không còn "THEORY ↔ PRACTICE" mỗi lần trượt; PENDING review của lesson trước → `REVIEW_REQUIRED` thống
  nhất ở plan.md và phase 3; `PRACTICE_REQUIRED` mô tả cùng phạm vi ở plan.md, phase 3, phase 4 (contract).
- Mâu thuẫn còn lại: 0.
