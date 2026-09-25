---
phase: 4
title: "Placement test-out"
status: completed
priority: P1
dependencies: [1, 2, 3]
effort: "~5h"
---

# Phase 4: Placement test-out

## Overview
Kết quả `PLACEMENT` đánh dấu các KP learner đã vững là "được miễn" (test-out), để `next_objective()` bỏ qua
chúng (D3, D5). Test-out dùng cơ chế override sẵn có của DeepTutor, nên không làm giả evidence, không đổi
`compute_mastery`, cổng mastery hay scheduler.

## Context
- DeepTutor có `LearningService.set_learner_mastery_override(book_id, kp_id, mastered, note)`. Override được
  lưu ở `progress.learner_mastery_overrides`; `is_mastered()` tôn trọng nó, nên `next_objective()` bỏ qua KP đó.
- `mastery_source()` chỉ trả `"system"` (có bằng chứng qua cổng) hoặc `"learner"` (có override). Test-out mà
  hiển thị `"learner"` là sai nghĩa (D5).
- `LearnerMasteryOverride` gồm `knowledge_point_id`, `note`, `created_at`; `extra="ignore"`.
- Evidence của placement vẫn được ghi bình thường qua `FormalResultApplier`. Override chỉ thêm vào, không thay evidence.
- Chấm lại placement (version mới) phải thay test-out của version cũ, giống cách evidence được thay (supersede).

## Requirements
- Functional:
  - *(Đổi khi cook: submodule trỏ thẳng vào upstream `HKUDS/DeepTutor`, nhóm không có fork, nên commit trong
    submodule không push được và sẽ làm hỏng clone của mọi người. Vì vậy **không sửa DeepTutor**: provenance nằm
    trong `note` của override (`placement:{attempt_id}:v{result_version}`), và
    `app/learning/placement_test_out.py` trả `masterySource = "placement"` cho các override đó. Kết quả D5 giữ
    nguyên. Phương án gốc bên dưới giữ lại để tham khảo nếu nhóm tạo fork.)*
  - ~~Mở rộng tối thiểu DeepTutor~~ (phương án gốc, không làm):
    - `LearnerMasteryOverride.source: str = "learner"`.
    - `mastery_source()` trả `override.source` thay vì hằng `"learner"`.
    - `set_learner_mastery_override(..., source="learner")` nhận thêm tham số.
    - Không đổi `is_mastered`, `compute_mastery`, cổng mastery hay policy.
  - Khi áp một kết quả có `assessment_type == "PLACEMENT"`, trong cùng transaction với evidence:
    1. Xóa các override `source == "placement"` mà `note` ghi `attempt_id` của attempt này (supersede khi chấm lại).
    2. Luật (a): nếu `overall_band` có giá trị, mọi KP trong path có `effectiveBandMax` khác null và
       `≤ overall_band` → override `source="placement"`.
    3. Luật (b): mọi KP có ít nhất một item placement map tới, và tất cả các item đó `is_correct == true`
       (định lượng) hoặc judgment `PASS` (định tính) → override `source="placement"`.
    4. `note` = `placement:{attempt_id}:v{result_version}` để truy vết và để bước 1 tìm lại được.
  - Không bao giờ ghi đè override `source == "learner"`.
  - KP đã `system` mastered thì không cần override (bỏ qua).
  - API: `masterySource` trong `/progress` và `/map` có thêm giá trị `"placement"`.
  - Band của KP đọc từ `mastery_path_knowledge_point_bands` (pha 2 ghi), trong cùng transaction.
- Non-functional:
  - Toàn bộ logic chọn KP test-out là một hàm thuần trong IELTSPath
    (`PlacementTestOut.select(progress, command) -> set[kp_id]`). Nó chỉ đọc band và kết quả chấm; không tính
    mastery. Việc ghi override đi qua API của DeepTutor.
  - Chạy được cả khi kết quả nằm trong hộp chờ rồi được áp lúc tạo path.

## Related Code Files
- Modify (submodule): `third_party/deeptutor/deeptutor/learning/{models.py,policy.py,service.py}`, kèm test
  upstream tương ứng. Commit trong submodule, rồi cập nhật con trỏ submodule ở repo chính.
- Create: `services/ai-learning-service/app/learning/placement_test_out.py`
- Modify: `app/application/formal_result_applier.py` (gọi test-out cho `PLACEMENT`, cùng transaction)
- Modify: `app/api/dto/responses.py` (không cần đổi kiểu vì `mastery_source` là `str`; thêm test)
- Modify: `docs/contracts/assessment-completed-v2.md`, `services/ai-learning-service/README.md`
- Tests: `tests/test_placement_test_out.py` (mới), `tests/test_formal_assessment_ingestion.py`,
  `tests/test_formal_assessment_postgres.py`, test DeepTutor cho `source`

## Implementation Steps
### Tests Before
1. Chạy suite DeepTutor phần `learning` và suite AI Learning, ghi mốc.
2. Viết test **fail trước**:
   - DeepTutor: override `source="placement"` → `mastery_source == "placement"`; mặc định vẫn `"learner"`;
     `is_mastered` đúng cho cả hai.
   - `PlacementTestOut.select`: luật (a) với band 6.5 chọn KP `effectiveBandMax ≤ 6.5`; band null → chỉ luật (b);
     luật (b) bỏ KP có một item sai; KP không có item nào → không chọn theo (b).
   - Ingestion: `PLACEMENT` → `next_objective()` nhảy qua KP được miễn; bài `QUIZ` cùng nội dung → không test-out.
   - Chấm lại placement với kết quả kém hơn → override của version 1 bị xóa, chỉ còn override của version 2.
   - Override `learner` có sẵn không bị đổi hay xóa.
   - Hộp chờ: placement tới trước khi có path → lần `/status` đầu tiên đã phản ánh test-out.
### Refactor
3. Mở rộng DeepTutor, commit submodule.
4. `PlacementTestOut` và nối vào `FormalResultApplier.apply_to_path`.
5. Cập nhật docs.
### Tests After
6. Suite DeepTutor (phần liên quan) và suite AI Learning pass.
### Regression Gate
7. Gate chung; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Learner mục tiêu 7.5, placement 6.5: KP band ≤ 6.5 được miễn; `/status` bắt đầu từ KP đầu tiên trên 6.5.
- [ ] `masterySource` phân biệt `system` / `placement` / `learner`.
- [ ] Chấm lại placement thay test-out cũ, không cộng dồn.
- [ ] Không có thay đổi nào ở `compute_mastery`, cổng mastery, policy hay scheduler.

## Risk Assessment
- **Miễn nhầm do band gắn sai:** override có provenance rõ ràng và gỡ được (chấm lại placement, hoặc sau này
  tutor/learner clear override). Không mất evidence.
- **Sửa submodule:** giữ diff nhỏ, có test. Ghi vào README AI Learning là fork khác upstream v1.6.9 ở điểm này.
- **Ôn tập:** KP được miễn không có `repetition_state`, nên scheduler không lên lịch ôn. Chấp nhận ở MVP:
  learner đã chứng minh vững. Ghi rõ trong README.
