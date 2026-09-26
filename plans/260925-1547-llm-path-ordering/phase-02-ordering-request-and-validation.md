---
phase: 2
title: "Ordering request and validation"
status: pending
priority: P1
dependencies: [1]
effort: "~4h"
---

# Phase 2: Ordering request and validation

## Overview
Dựng dữ liệu gửi cho Gemini (bối cảnh học viên và kho KP), và kiểm tra chặt đề xuất trả về. Đề xuất chỉ được
chấp nhận khi đúng là một **hoán vị**: cùng tập module, cùng tập KP trong mỗi module, không thêm, không bỏ, không
chuyển KP sang module khác (L3). Phần này là hàm thuần: không đụng DB, không gọi LLM, không đọc đồng hồ hệ thống
(ngày hôm nay được truyền vào). Lời gọi LLM nằm ở pha 1.

## Đọc trước khi code
| File | Để làm gì |
| --- | --- |
| `app/adapters/curriculum_adapter.py` | `modules` được dựng thế nào: mỗi topic là một module, `order` theo thứ tự cây. |
| `app/adapters/curriculum_scope.py` | `ScopedCurriculum` (`topics`, `knowledge_points`, `bands`), `target_band_of`. |
| `app/clients/content_service.py` | Topic và KP thô: topic có `parentTopicId`; KP có `skill`, `description`. |
| `app/adapters/formal_evidence_adapter.py` | `FormalAssessmentCommand`, `ItemObservation`, `KnowledgePointObservation`. |
| `app/learning/placement_test_out.py` (`PlacementTestOut.select`) | Quy tắc đúng/sai theo KP; phải dùng cùng quy tắc. |
| `third_party/deeptutor/deeptutor/learning/models.py` | `LearningModule` (`order`), `KnowledgePoint`, `LearnerProfile`. |
| `third_party/deeptutor/deeptutor/learning/policy.py` (`next_objective`, `map_summary`) | Module được duyệt theo trường `order`; KP theo thứ tự trong list. |
| `third_party/deeptutor/deeptutor/capabilities/mastery/prompts/en/mastery_loop.yaml` (khối `outline`) | Nguyên tắc DeepTutor dùng khi thiết kế lộ trình; prompt của ta theo cùng tinh thần. |

## Context
- `next_objective()` và `map_summary()` của DeepTutor sắp module theo trường `order`, còn KP thì theo thứ tự trong
  list của module. Muốn thứ tự mới có hiệu lực thì phải **vừa đổi thứ tự list, vừa đánh lại `order`**.
- Topic của Content là một cây (`parentTopicId`). `CurriculumAdapter` làm phẳng cây: mỗi topic, kể cả topic cha, là
  một module riêng, theo thứ tự cha trước con.
- Goal từ User Service (`LearningGoalResponse`): `targetBand`, `examDate` (có thể null), `availableMinutesPerDay`
  (có thể null).

## Requirements
- Functional:
  - `LearnerContext` (dataclass), chỉ gồm dữ liệu không định danh:
    - `target_band: Decimal`, lấy bằng `target_band_of(goal)`;
    - `days_until_exam: int | None`, bằng `examDate - today`; null nếu không có ngày thi; nhỏ hơn 0 thì là 0;
    - `minutes_per_day: int | None`, từ `availableMinutesPerDay`;
    - `placement_band: Decimal | None`;
    - `placement_results: dict[kp_id, bool]`.
  - `LearnerContext.from_goal(goal, placements, today)`, với `placements` là list `FormalAssessmentCommand` loại
    `PLACEMENT`, đã lọc còn version mới nhất của mỗi attempt (pha 3 lo việc đọc):
    - `placement_band` = `overall_band` của command có `completed_at` mới nhất mà có band.
    - `placement_results`: gom mọi item của mọi command, cùng quy tắc với `PlacementTestOut.select`:
      - quan sát đúng = `is_correct is True` hoặc `qualitative_judgment == "PASS"`;
      - quan sát sai = `is_correct is False` hoặc `qualitative_judgment == "FAIL"`;
      - KP có ít nhất một quan sát sai → `False`; chỉ có quan sát đúng → `True`; không có quan sát → không có mặt.
  - `OrderingRequest.build(context, modules, scoped) -> (system_prompt, user_payload)`, là hai tham số
    `system_prompt` và `prompt` của `complete()` (pha 1).
    - `user_payload` là chuỗi JSON (`ensure_ascii=False`, key sắp xếp cố định) dạng:
      ```json
      {
        "learner": {"target_band": "6.5", "days_until_exam": 60, "minutes_per_day": 45,
                    "placement_band": "5.5"},
        "modules": [
          {"id": "<topic uuid>", "name": "...", "parent_id": "<uuid or null>",
           "knowledge_points": [
             {"id": "<kp uuid>", "name": "...", "type": "CONCEPT", "skill": "WRITING",
              "description": "<tối đa 200 ký tự>", "band_min": "5.0", "band_max": "6.5",
              "placement": "correct | incorrect | not_tested"}
           ]}
        ]
      }
      ```
      - Module và KP theo **thứ tự Content**; `type` là `learningType` của KP.
      - `parent_id`, `skill`, `description` lấy từ `scoped.topics` và `scoped.knowledge_points` (bản ghi thô của Content).
      - Band lấy từ `scoped.bands`; đầu mở là `null`.
    - System prompt (tiếng Anh) gồm:
      - Vai trò: sắp thứ tự một lộ trình IELTS cho một học viên.
      - Chỉ được sắp thứ tự module, và thứ tự KP bên trong từng module. Không thêm, bỏ, đổi tên hay chuyển KP sang
        module khác. Dùng đúng các id đã cho.
      - Nguyên tắc, theo tinh thần khối `outline` của DeepTutor (lộ trình xoay quanh người học, không chép mục lục):
        - phần học viên làm sai ở placement và phần nền tảng (band thấp) đứng trước;
        - phần phụ thuộc đứng sau phần nền tảng của nó; `parent_id` cho biết nhóm chủ đề;
        - phù hợp số ngày còn lại tới kỳ thi và số phút mỗi ngày.
      - Dạng JSON trả về: `{"modules": [{"id": str, "knowledge_point_ids": [str]}], "rationale": str}`.
        DeepTutor chỉ gửi `response_format: {"type": "json_object"}`, không gửi schema riêng.
  - `OrderingValidator.apply(modules, proposal) -> list[LearningModule]`:
    - Chấp nhận khi: tập module id trùng khớp; mỗi module có đúng tập KP của nó; không trùng lặp; không id lạ.
      `proposal["modules"]` và `knowledge_point_ids` phải là list; id phải là chuỗi.
    - Trả về bản sao sâu (`model_copy(deep=True)`) của `modules`, đã đổi thứ tự list module và list KP.
      - `order` của module được đánh lại `0..n-1` theo thứ tự mới.
      - Tên, loại, `module_id` của KP lấy từ `modules` (Content), không lấy từ LLM.
    - Sai bất kỳ điều kiện nào → `InvalidOrdering(reason)`. `reason` là một trong: `malformed`, `missing_module`,
      `unknown_module`, `duplicate_module`, `missing_knowledge_point`, `unknown_knowledge_point`,
      `duplicate_knowledge_point`, `moved_knowledge_point`. Không chép nội dung LLM trả về vào lỗi.
    - Không kiểm tra quan hệ cha–con giữa module: L3 chỉ cấm thêm, bỏ, chuyển KP.
  - `rationale`: chuỗi, cắt còn tối đa 500 ký tự; chỉ để log và event, không hiện cho học viên. Thiếu hoặc không phải
    chuỗi thì là `""`, không làm đề xuất bị từ chối.
  - Giới hạn kích thước: tổng số KP lớn hơn `MAX_ORDERING_KNOWLEDGE_POINTS = 300`, hoặc `user_payload` dài hơn
    `MAX_ORDERING_PAYLOAD_CHARS = 60_000` → `PayloadTooLarge`. Pha 3 khi đó bỏ qua LLM, lý do `payload_too_large`.
- Non-functional:
  - Không gửi email, tên, user id, goal id hay token. Có test khóa điều này.
  - Cùng đầu vào → cùng `user_payload`, từng byte một (để test và để so sánh log).

## Related Code Files
- Create: `services/ai-learning-service/app/learning/path_ordering.py`
  (`LearnerContext`, `OrderingRequest`, `OrderingValidator`, `InvalidOrdering`, `PayloadTooLarge`, hai hằng giới hạn)
- Tests: `tests/test_path_ordering.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**:
   - Hoán vị hợp lệ → thứ tự mới; `order` là `0..n-1`; tên, loại, `module_id` giữ theo Content; `modules` gốc không
     bị sửa.
   - `next_objective()` của DeepTutor trên progress dùng module đã sắp → KP đầu tiên theo thứ tự mới.
   - Mỗi `reason` của `InvalidOrdering` có ít nhất một test.
   - Payload không chứa email, user id, goal id (dùng goal mẫu có các trường đó).
   - Placement: KP vừa đúng vừa sai → `incorrect`; chỉ đúng → `correct`; không có → `not_tested`; `placement_band`
     theo `completed_at` mới nhất; không có placement → null.
   - `examDate` null hoặc đã qua; `availableMinutesPerDay` null.
   - Vượt giới hạn KP hoặc ký tự → `PayloadTooLarge`.
   - Cùng đầu vào hai lần → payload giống hệt.
### Refactor
2. Viết module `path_ordering.py`.
### Tests After / Regression Gate
3. Suite pass, 0 skip; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Không đề xuất nào của LLM làm mất, thêm hay chuyển KP mà vẫn lọt qua.
- [ ] Thứ tự mới có hiệu lực trong `next_objective()` của DeepTutor.
- [ ] Dữ liệu gửi Gemini không chứa thông tin định danh.

## Risk Assessment
- **LLM trả thiếu hoặc thừa KP khá thường gặp:** validator từ chối và dùng thứ tự Content (L4). Tỉ lệ bị từ chối
  đếm được qua event `path.ordered`.
- **LLM đặt topic con trước topic cha:** được phép. Chỉ có hại nếu cha là nền tảng của con; prompt đã gửi
  `parent_id` và yêu cầu nền tảng trước. Theo dõi sau khi chạy thật.
