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
Khi tạo path, AI Learning gọi Gemini qua lớp LLM của DeepTutor (pha 1) để sắp thứ tự, rồi mới ghi path.
- Lời gọi LLM nằm **ngoài** transaction và row lock, để không giữ lock trong lúc chờ mạng.
- Nếu catalog chưa cấu hình LLM, LLM lỗi, hoặc LLM trả đề xuất sai, path dùng thứ tự Content như hiện nay (L4).
- Đồng thời điền `LearnerProfile` của DeepTutor để Tutor dùng sau này.

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
    3. Dựng `OrderingRequest` (pha 2), gọi `DeepTutorOrderingLlm.propose` (pha 1), rồi `OrderingValidator.apply`.
       Giữ thứ tự Content khi:
       - `LlmProposal` có `reason`: `llm_not_configured`, `llm_timeout`, `llm_error` hoặc `llm_unusable_response`;
       - `InvalidOrdering` → reason `invalid_ordering`;
       - kho KP vượt giới hạn kích thước (pha 2) → reason `payload_too_large`, không gọi LLM.
    4. `ensure_path(..., modules=đã_sắp_xếp)`, như cũ: band snapshot, áp kết quả đang chờ, test-out.
    5. Trong cùng transaction tạo path:
       - Ghi event `path.ordered` với `{source: "llm" | "content", reason, model, rationale}`.
       - Điền `learner_profile`:
         - `target_level` từ band mục tiêu;
         - `time_budget` từ số phút mỗi ngày và ngày thi;
         - `prior_knowledge` từ band placement, nếu có.
  - `/progress` và `/map` không đổi contract. Thứ tự module/KP trả về chính là thứ tự đã sắp.
  - `POST /paths` với path đã có: không gọi LLM (sắp lại khi refresh thuộc phần "làm sau"). KP mới được thêm vào
    cuối module như hiện nay.
  - Consumer không gọi LLM, không import lớp LLM của DeepTutor và không gắn catalog.
- Non-functional:
  - Không giữ DB connection trong lúc chờ LLM.
  - Thời gian chờ tối đa bằng timeout của lời gọi (pha 1). Quá hạn → thứ tự Content.

## Related Code Files
- Modify: `app/application/path_service.py` (bước sắp xếp trước `ensure_path`; nhận `PathOrderer` tùy chọn)
- Create: `app/application/path_orderer.py`
  - ghép context, lời gọi LLM (pha 1) và validator (pha 2);
  - quyết định `llm` hay `content`, trả kết quả kèm lý do.
- Modify: `app/persistence/postgres_learning_store.py` (đọc chỉ-đọc kết quả đang chờ theo `(user, goal)`)
- Modify: `tests/formal_assessment_support.py` (store in-memory có method tương ứng)
- Modify: `main.py`
  - `get_path_service` luôn nối `PathOrderer` với `DeepTutorOrderingLlm`.
  - Không cần kiểm tra key ở đây: catalog chưa cấu hình thì lời gọi trả `llm_not_configured`.
- Tests: `tests/test_path_orderer.py` (mới), `tests/test_goal_scoped_path.py`,
  `tests/test_goal_scoped_path_postgres.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**. Lời gọi LLM là một `DeepTutorOrderingLlm` giả trả `LlmProposal` định sẵn, như test của
   DeepTutor thay hàm gọi LLM:
   - Đề xuất là hoán vị hợp lệ:
     - path theo thứ tự đó;
     - event `path.ordered` có `source=llm` và tên model;
     - `next_objective()` đi theo thứ tự mới.
   - Mỗi reason `llm_not_configured`, `llm_timeout`, `llm_error`, `llm_unusable_response`, hoặc đề xuất có KP lạ:
     - path theo thứ tự Content;
     - event có `source=content` kèm đúng lý do;
     - việc tạo path vẫn thành công.
   - Có placement đang chờ:
     - payload gửi LLM có đúng/sai theo KP;
     - sau khi tạo path, test-out vẫn áp dụng như cũ.
   - `learner_profile` được điền, không chứa email, tên hay id.
   - Lời gọi LLM xảy ra khi chưa mở transaction nào (kiểm bằng store giả ghi lại thứ tự gọi).
   - Học viên làm bài sau khi path đã sắp (L5):
     - evidence, mastery và `/status` cập nhật như cũ;
     - thứ tự module/KP không đổi.
   - PostgreSQL: hai request tạo path song song → một path, không lỗi.
2. Một test đi hết đường thật: `PathService` + `DeepTutorOrderingLlm` thật + server giả kiểu OpenAI và catalog tạm
   (dùng lại phần dựng của pha 1). Path tạo ra theo thứ tự server giả trả về.
### Refactor
3. `PathOrderer`, nối vào `PathService` và `main.py`.
### Tests After / Regression Gate
4. Suite Python (PostgreSQL + RabbitMQ): 0 fail, 0 skip; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Catalog có Gemini: path mới theo thứ tự Gemini, nhưng đúng tập KP của Content.
- [ ] Catalog chưa cấu hình hoặc LLM lỗi: tạo path vẫn thành công, theo thứ tự Content, và có event ghi rõ lý do.
- [ ] Kết quả chấm về sau vẫn cập nhật path như trước.

## Risk Assessment
- **Placement tới sau khi path đã tạo:** thứ tự khi đó chỉ dựa trên goal. Sắp lại là phần "làm sau" số 2.
