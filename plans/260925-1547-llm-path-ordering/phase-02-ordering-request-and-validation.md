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
chuyển KP sang module khác (L3). Phần này là hàm thuần, không đụng DB hay DeepTutor.

## Context
- Kho KP: `modules` do `CurriculumAdapter` dựng từ curriculum đã lọc band (`CurriculumScope`, plan `260925-0425`).
- Band của từng KP có trong `ScopedCurriculum.bands`.
- Placement: nếu đã có kết quả `PLACEMENT` đang chờ trong hộp chờ của goal này, thì lấy được `overall_band` và
  đúng/sai theo từng KP (`FormalEvidenceAdapter`).

## Requirements
- Functional:
  - `LearnerContext`, chỉ gồm dữ liệu không định danh:
    - `target_band`, `days_until_exam` (hoặc null), `minutes_per_day`;
    - `placement_band` (hoặc null);
    - `placement_results`: danh sách `{knowledge_point_id, correct: bool | null}`.
  - `OrderingRequest.build(context, modules, bands)` → `(system_prompt, user_payload, response_schema)`:
    - Payload liệt kê module và KP bằng id, tên, `learningType`, band. KP được đánh số theo module.
    - Prompt yêu cầu: chỉ sắp thứ tự module và thứ tự KP trong mỗi module; dùng đúng các id đã cho; ưu tiên phần
      yếu theo placement, phần nền tảng trước phần phụ thuộc, và phù hợp thời gian còn lại tới kỳ thi.
    - Schema: `{"modules": [{"id": str, "knowledge_point_ids": [str]}], "rationale": str}`.
  - `OrderingValidator.apply(modules, proposal) -> list[LearningModule]`:
    - Chấp nhận khi: tập module id trùng khớp; mỗi module có đúng tập KP của nó; không trùng lặp; không id lạ.
    - Trả về bản sao của `modules` đã sắp lại. Tên, loại và `module_id` của KP lấy từ Content, không lấy từ LLM.
      Trường `order` được đánh lại.
    - Sai bất kỳ điều kiện nào → `InvalidOrdering(reason)`. Lý do chỉ nêu loại lỗi (thiếu, thừa, trùng, chuyển
      module), không chép nội dung LLM trả về.
  - `rationale` của LLM chỉ để log và event, tối đa 500 ký tự, không hiện cho học viên.
- Non-functional:
  - Không gửi email, tên, user id hay token. Có test khóa điều này.
  - Payload có giới hạn kích thước: nếu kho KP quá lớn (chốt ngưỡng khi cook), bỏ qua LLM và dùng thứ tự Content.

## Related Code Files
- Create: `services/ai-learning-service/app/learning/path_ordering.py`
  (`LearnerContext`, `OrderingRequest`, `OrderingValidator`, `InvalidOrdering`)
- Tests: `tests/test_path_ordering.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**:
   - Hoán vị hợp lệ → thứ tự mới; tên, loại, `module_id` giữ theo Content; `order` được đánh lại 0..n-1.
   - Thiếu một KP, thừa một KP lạ, KP lặp, KP chuyển module, thiếu module, module lạ → `InvalidOrdering`.
   - Payload không chứa email, tên, user id (kiểm bằng dữ liệu mẫu có các trường đó trong goal).
   - Có placement → payload có `placement_band` và đúng/sai theo KP; không có → null.
### Refactor
2. Viết module `path_ordering.py`.
### Tests After / Regression Gate
3. Suite pass; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Không đề xuất nào của LLM làm mất, thêm hay chuyển KP mà vẫn lọt qua.
- [ ] Dữ liệu gửi Gemini không chứa thông tin định danh.

## Risk Assessment
- **LLM trả thiếu hoặc thừa KP khá thường gặp:** validator từ chối và dùng thứ tự Content (L4). Đếm tỉ lệ bị từ
  chối qua event để theo dõi.
