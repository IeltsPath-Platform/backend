---
phase: 2
title: "Goal-scoped curriculum"
status: completed
priority: P1
dependencies: [1]
effort: "~3h"
---

# Phase 2: Goal-scoped curriculum

## Overview
Khi tạo path, AI Learning chỉ giữ các KP trong phạm vi band mục tiêu của goal (D2). Đây là lọc phạm vi nội
dung ở adapter, trước khi dữ liệu vào DeepTutor. Không có quyết định adaptive nào ở đây.

## Context
- `PathService._active_goal()` đã có goal từ `GET /api/users/me/learning-goals/active`, trong đó có `targetBand`,
  nhưng hiện chỉ dùng `id` và `userId`.
- `ContentServiceClient.get_curriculum()` trả `(topics, knowledge_points)`; `CurriculumAdapter.to_modules()` dựng module.
- `CurriculumAdapter` ném `CurriculumContractError` (502) khi curriculum không còn KP ACTIVE nào.

## Requirements
- Functional:
  - Luật phạm vi: KP vào path khi `effectiveBandMin` là null hoặc `effectiveBandMin ≤ targetBand`.
    Không có luật cận trên: KP dễ hơn mục tiêu vẫn nằm trong path, và pha 4 dùng test-out để bỏ qua.
  - Module (topic) không còn KP nào sau khi lọc thì bỏ, không tạo module rỗng.
  - `targetBand` thiếu hoặc sai kiểu trong response của User → lỗi contract (502), không âm thầm lấy cả curriculum.
  - Sau khi lọc mà không còn KP nào: trả **409** với thông báo "Không có nội dung phù hợp với band mục tiêu".
    Đây không phải lỗi của Content, nên không trả 502.
  - Lưu band hiệu lực của mọi KP trong path vào bảng `mastery_path_knowledge_point_bands` (migration V4), trong
    cùng transaction tạo path và trước khi áp kết quả trong hộp chờ. *(Chốt khi cook, chuyển từ pha 4:
    `KnowledgePoint` của DeepTutor không có trường metadata, và consumer không có token để gọi Content.)*
  - Ghi phạm vi đã dùng vào `mastery_events` (`path.scope_applied`, gồm `targetBand` và số KP giữ/bỏ) để truy vết.
- Non-functional:
  - Luật phạm vi là một hàm thuần, test được độc lập (`CurriculumScope.select(topics, points, target_band)`).
  - Không đổi thứ tự module/KP so với hiện nay.

## Related Code Files
- Create: `services/ai-learning-service/app/adapters/curriculum_scope.py`
- Modify: `app/application/path_service.py` (đọc `targetBand`, gọi scope trước `to_modules`, lỗi 409 mới)
- Modify: `main.py` (exception handler cho `NoCurriculumInScope` → 409)
- Modify: `app/clients/content_service.py` (giữ `effectiveBandMin`/`effectiveBandMax`, validate kiểu)
- Modify: `services/ai-learning-service/README.md` (luật phạm vi)
- Tests: `tests/test_curriculum_scope.py` (mới), `tests/test_formal_assessment_postgres.py` (stub Content có band),
  test API cho 409

## Implementation Steps
### Tests Before
1. Chạy suite Python, ghi mốc (hiện 80 pass).
2. Viết test **fail trước**:
   - `CurriculumScope`: goal 5.5 giữ KP band 4.0–5.0 và KP band trống, bỏ KP `effectiveBandMin = 7.0`;
     topic chỉ có KP bị bỏ thì biến mất; thứ tự còn lại giữ nguyên.
   - `targetBand` = 9.0 → giữ mọi KP (tương đương hành vi hiện nay).
   - PostgreSQL E2E: learner mục tiêu 5.5 và learner mục tiêu 8.0 với cùng curriculum stub ra `knowledgePointCount` khác nhau.
   - Không còn KP nào sau khi lọc → `NoCurriculumInScope`; API trả 409.
### Refactor
3. `CurriculumScope` và nối vào `ensure_active_path`.
4. Exception handler và README.
### Tests After
5. Toàn bộ suite pass. Các test cũ dùng curriculum không có band vẫn pass, vì band trống = mọi band.
### Regression Gate
6. Gate chung; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Hai learner khác band mục tiêu nhận hai tập KP khác nhau từ cùng một curriculum.
- [ ] Nội dung chưa gắn band vẫn vào path của mọi learner.
- [ ] Không có module rỗng; thứ tự không đổi.
- [ ] Thiếu `targetBand` → 502; lọc xong rỗng → 409.

## Risk Assessment
- **Path đã tạo trước pha này** chứa toàn bộ curriculum. Pha 5 (làm mới phạm vi) xử lý các path đó.
- **Kết quả chấm cho KP ngoài phạm vi** bị bỏ qua (log `unknown`). Ghi trong README và contract doc.
