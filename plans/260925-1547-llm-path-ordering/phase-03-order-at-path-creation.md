---
phase: 3
title: "Order the path at creation"
status: pending
priority: P1
dependencies: [1, 2]
effort: "~4h"
---

# Phase 3: Order the path at creation

## Overview
Khi tạo path, AI Learning gọi Gemini để sắp thứ tự rồi mới ghi path. Lời gọi LLM nằm **ngoài** transaction và row
lock, để không giữ lock trong lúc chờ mạng. Nếu LLM tắt, lỗi hoặc trả đề xuất sai, path dùng thứ tự Content như
hiện nay (L4). Đồng thời điền `LearnerProfile` của DeepTutor để Tutor dùng sau này.

## Context
- `PathService._create_from_content` gọi `_scoped_curriculum` (lấy goal và Content), rồi `ensure_path` →
  `_create_path` trong một transaction có advisory lock.
- Kết quả đang chờ của goal chỉ đọc được trong transaction (`pending_formal_results`). Cần thêm một cách đọc chỉ-đọc
  ngoài transaction để lấy placement trước khi gọi LLM.
- `LearningProgress.learner_profile` (DeepTutor) có các trường `prior_knowledge`, `target_level`, `time_budget`,
  `preferences`, `notes`, đều là văn bản tự do.
- Hai request cùng tạo path một lúc: advisory lock và unique index đã chọn ra một bên thắng. Bên thua trả về path
  của bên thắng, nên có thể tốn thêm một lời gọi LLM. Chấp nhận được.

## Requirements
- Functional:
  - Luồng tạo path mới:
    1. Lấy goal, curriculum đã lọc band (như cũ).
    2. Đọc chỉ-đọc kết quả `PLACEMENT` đang chờ của `(user, goal)`, nếu có. Lấy version mới nhất của mỗi attempt.
    3. Nếu Gemini bật: dựng `OrderingRequest`, gọi `GeminiClient`, rồi `OrderingValidator.apply`.
       Nếu Gemini tắt, `LlmUnavailable` hoặc `InvalidOrdering` → giữ thứ tự Content.
    4. `ensure_path(..., modules=đã_sắp_xếp)`, như cũ: band snapshot, áp kết quả đang chờ, test-out.
    5. Trong cùng transaction tạo path:
       - Ghi event `path.ordered` với `{source: "llm" | "content", reason, model, rationale}`.
       - Điền `learner_profile`: `target_level` từ band mục tiêu, `time_budget` từ phút/ngày và ngày thi,
         `prior_knowledge` từ band placement (nếu có).
  - `/progress` và `/map` không đổi contract. Thứ tự module/KP trả về chính là thứ tự đã sắp.
  - `POST /paths` với path đã có: không gọi LLM (sắp lại khi refresh thuộc phần "làm sau"). KP mới được thêm vào
    cuối module như hiện nay.
  - Consumer không gọi LLM và không nhận key.
- Non-functional:
  - Không giữ DB connection trong lúc chờ Gemini.
  - Thời gian chờ tối đa bằng timeout của client (pha 1). Quá hạn → thứ tự Content.

## Related Code Files
- Modify: `app/application/path_service.py` (bước sắp xếp trước `ensure_path`; nhận `PathOrderer` tùy chọn)
- Create: `app/application/path_orderer.py` (ghép context, client và validator; quyết định llm/content; trả kết quả
  kèm lý do)
- Modify: `app/persistence/postgres_learning_store.py` (đọc chỉ-đọc kết quả đang chờ theo `(user, goal)`)
- Modify: `tests/formal_assessment_support.py` (store in-memory có method tương ứng)
- Modify: `main.py` (`get_path_service` nối `PathOrderer` khi có key)
- Tests: `tests/test_path_orderer.py` (mới), `tests/test_goal_scoped_path.py`,
  `tests/test_goal_scoped_path_postgres.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước** (Gemini giả qua `httpx.MockTransport`):
   - Gemini trả hoán vị hợp lệ → path theo thứ tự đó; event `path.ordered` có `source=llm`;
     `next_objective()` đi theo thứ tự mới.
   - Gemini tắt, timeout, 500, hoặc trả KP lạ → path theo thứ tự Content; event `source=content` kèm lý do;
     việc tạo path vẫn thành công.
   - Có placement đang chờ → payload gửi Gemini có đúng/sai theo KP; sau khi tạo path, test-out vẫn áp dụng như cũ.
   - `learner_profile` được điền, không chứa email, tên hay id.
   - Lời gọi Gemini xảy ra khi chưa mở transaction nào (kiểm bằng store giả ghi lại thứ tự gọi).
   - Học viên làm bài sau khi path đã sắp → evidence, mastery và `/status` cập nhật như cũ; thứ tự module/KP không đổi (L5).
   - PostgreSQL: hai request tạo path song song → một path, không lỗi.
### Refactor
2. `PathOrderer`, nối vào `PathService` và `main.py`.
### Tests After / Regression Gate
3. Suite Python (PostgreSQL + RabbitMQ): 0 fail, 0 skip; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Có key: path mới theo thứ tự Gemini, nhưng đúng tập KP của Content.
- [ ] Không có key hoặc LLM lỗi: tạo path vẫn thành công, theo thứ tự Content, và có event ghi rõ lý do.
- [ ] Kết quả chấm về sau vẫn cập nhật path như trước.

## Risk Assessment
- **Placement tới sau khi path đã tạo:** thứ tự khi đó chỉ dựa trên goal. Sắp lại là phần "làm sau" số 2.
