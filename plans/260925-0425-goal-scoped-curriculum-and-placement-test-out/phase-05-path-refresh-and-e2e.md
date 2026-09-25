---
phase: 5
title: "Path refresh and E2E verification"
status: completed
priority: P1
dependencies: [1, 2, 3, 4]
effort: "~4h"
---

# Phase 5: Path refresh and E2E verification

## Overview
`POST /api/ai-learning/paths` làm mới phạm vi của path đang có (D6): KP mới của Content trong phạm vi được thêm
vào, thứ tự được cập nhật. Sau đó chạy E2E thật qua Gateway với hai learner khác band.

## Context
- `ensure_path` trả path có sẵn mà không gọi Content. Path đóng băng từ lúc tạo.
- `LearningService.replace_modules_for_path(..., append=False)` giữ id KP do IELTSPath cung cấp, nhưng
  `replace_modules()` **xóa** mastery, evidence, attempts và override của mọi KP không còn trong module mới.
- Path tạo trước pha 2 chứa toàn bộ curriculum, không lọc.

## Requirements
- Functional:
  - `POST /paths` với path đã có: lấy curriculum, lọc theo `targetBand` (pha 2), rồi:
    - **Chỉ thêm, không bớt:** giữ mọi KP đang có trong path, kể cả KP đã ra khỏi phạm vi hoặc đã INACTIVE bên
      Content, để không bao giờ xóa trạng thái học. Thêm KP mới trong phạm vi; cập nhật tên và thứ tự theo Content.
    - Nếu không có gì thay đổi: không tăng `revision`, không ghi event.
    - Có thay đổi: một revision, event `path.scope_refreshed` gồm số KP thêm.
  - GET (`/progress`, `/status`, `/map`) không đổi hành vi: không gọi Content với path đã có.
  - KP INACTIVE bên Content vẫn còn trong path nhưng bị đánh dấu để `next_objective()` bỏ qua. **TO VERIFY lúc
    cook:** DeepTutor có cơ chế ẩn KP nào không. *(Kết quả: không có. Dùng override với note `retired:content`,
    `masterySource = "retired"`, và tự gỡ khi KP quay lại phạm vi. Đánh đổi: phần `counts` của DeepTutor tính KP
    retired là đã qua.)*
- Non-functional:
  - Làm mới chạy dưới row lock của path (transaction hiện có), an toàn khi chạy song song với consumer.

## Related Code Files
- Modify: `app/application/path_service.py` (`refresh_scope`, gọi từ `POST /paths`)
- Modify: `main.py` (`POST /paths` trả thêm `addedKnowledgePointCount`, chỉ cộng thêm)
- Modify: `app/api/dto/responses.py`, `services/ai-learning-service/README.md`
- Tests: `tests/test_path_refresh.py` (mới, in-memory + PostgreSQL)
- Create: `plans/260925-0425-goal-scoped-curriculum-and-placement-test-out/reports/e2e-verification-<YYMMDD-HHMM>-report.md`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**:
   - Path có KP-A; Content thêm KP-C trong phạm vi → `POST /paths` thêm KP-C, mastery của KP-A giữ nguyên.
   - Content bỏ KP-A (INACTIVE) → KP-A vẫn còn trong path, evidence còn nguyên, `next_objective()` không chọn KP-A.
   - Gọi hai lần không có thay đổi → `revision` không đổi.
   - Refresh song song với consumer áp kết quả (PostgreSQL) → không mất cập nhật, không lỗi.
### Refactor
2. `refresh_scope` dùng `replace_modules_for_path(append=False)` với danh sách hợp (KP cũ ∪ KP mới trong phạm vi).
### Tests After
3. Suite pass.
### E2E thật qua Gateway (runbook)
4. Content (ADMIN):
   - Topic *Basic* band 4.0–5.0, *Complex* 6.0–7.0, *Academic* 7.0–8.0. Mỗi topic 1 KP PROCEDURE.
   - Question cho mỗi KP.
5. Learner A: goal mục tiêu 5.5, placement band 4.5, trả lời đúng câu *Basic*.
   - Path có *Basic* và *Complex*, không có *Academic*.
   - *Basic* được miễn theo luật (b); `/status` = `probe` KP của *Complex*.
6. Learner B: goal mục tiêu 7.5, placement band 6.5, sai câu *Complex*.
   - Path có đủ 3 topic.
   - *Basic* được miễn theo luật (a); `/status` = `probe`/`practice` KP của *Complex*.
7. Chấm lại placement của B với band 7.0 → KP *Complex* (`effectiveBandMax` 7.0) được miễn; `/status` chuyển sang *Academic*.
8. Content thêm KP mới trong *Complex* → B gọi `POST /paths` → KP mới xuất hiện, mastery cũ giữ nguyên.
9. `masterySource` trong `/map`: `placement` cho KP được miễn, `system` cho KP qua cổng bằng bằng chứng.
10. Ghi bằng chứng vào report (che token, secret, email).
### Regression Gate
11. Gate chung, 0 fail, 0 skip; `git status` không có `.env`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Làm mới chỉ thêm, không bao giờ xóa trạng thái học.
- [ ] Hai learner khác band nhận path và điểm bắt đầu khác nhau từ cùng một curriculum.
- [ ] Chấm lại placement thay đổi điểm bắt đầu đúng như luật.
- [ ] Report E2E có bằng chứng thật.

## Risk Assessment
- **Path phình theo thời gian** vì chỉ thêm, không bớt. Chấp nhận: số KP của một goal có giới hạn.
- **KP INACTIVE:** cách bỏ qua phụ thuộc kết quả TO VERIFY ở trên.
