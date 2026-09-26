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
- Lời gọi LLM nằm **ngoài** transaction và row lock, để không giữ lock hay connection DB trong lúc chờ mạng.
- Nếu catalog chưa cấu hình LLM, LLM lỗi, hoặc LLM trả đề xuất sai, path dùng thứ tự Content như hiện nay (L4).
- Đồng thời ghi `LearnerProfile` của DeepTutor bằng API của DeepTutor, để Tutor dùng sau này.

## Đọc trước khi code
| File | Để làm gì |
| --- | --- |
| `app/application/path_service.py` | `_create_from_content`, `ensure_path`, `_create_path`, `_refresh_path`. |
| `app/application/formal_result_applier.py` (`apply_pending`) | Cách đọc và parse kết quả đang chờ. |
| `app/persistence/postgres_learning_store.py` | `find_path` (mẫu truy vấn ngoài transaction), `pending_formal_results` (chỉ chạy trong transaction). |
| `tests/formal_assessment_support.py` | Store in-memory dùng trong test; cần method tương ứng. |
| `main.py` (`get_path_service`) | Nơi nối `PathService`. |
| `third_party/deeptutor/deeptutor/learning/service.py` (`record_learner_profile`) | API của DeepTutor để ghi `learner_profile`; merge, không thay thế. |
| `third_party/deeptutor/deeptutor/learning/storage.py` (`emit`) | Ghi event trong transaction. |

## Context
- Có hai đường tạo path, cả hai đều đi qua `_create_from_content`:
  - `ensure_active_path` (GET `/progress`, `/status`... khi goal chưa có path);
  - `refresh_active_path` (`POST /paths`) khi goal chưa có path.
  Khi path đã có, `refresh_active_path` chỉ gộp KP mới (`_refresh_path`) và **không gọi LLM**.
- `PathService` đã là async và gọi DB qua `run_in_threadpool`, nên có thể `await` lời gọi LLM trước khi vào
  `run_in_threadpool(ensure_path ...)`.
- `pending_formal_results` dùng `FOR UPDATE` và chỉ chạy được trong transaction của path. Cần một truy vấn mới, chỉ
  đọc, theo mẫu của `find_path` (connection riêng, không lock).
- Lời gọi lồng vào transaction đang mở là cách code hiện tại vẫn làm: `_refresh_path` gọi
  `set_learner_mastery_override` (đi qua `_store.mutate`) bên trong `self._store.transaction(path_id)`.
  `record_learner_profile` cũng đi qua `_store.mutate`, nên gọi được trong `_create_path`.
- Hai request cùng tạo path: advisory lock và unique index đã chọn ra một bên thắng; bên thua trả về path của bên
  thắng, bỏ kết quả sắp xếp của mình. Có thể tốn thêm một lời gọi LLM. Chấp nhận được.

## Requirements
- Functional:
  - `PathOrderer` (`app/application/path_orderer.py`):
    ```python
    @dataclass(frozen=True)
    class OrderingOutcome:
        modules: list[LearningModule]      # đã sắp, hoặc đúng thứ tự Content
        source: str                         # "llm" | "content"
        reason: str | None                  # None khi source == "llm"
        detail: str | None                  # reason của InvalidOrdering, nếu có
        model: str | None
        rationale: str                      # "" nếu không có
        learner_profile: dict[str, str]     # chỉ các trường có giá trị

    class PathOrderer:
        def __init__(self, store, llm: DeepTutorOrderingLlm, *, today: Callable[[], date] = <UTC today>): ...
        async def order(self, user_id, goal, modules, scoped) -> OrderingOutcome: ...
    ```
    `order` làm lần lượt:
    1. Đọc payload đang chờ: `await run_in_threadpool(store.pending_formal_payloads, user_id, goal["id"])`.
    2. Parse từng payload bằng `FormalEvidenceAdapter.to_command`; bỏ qua payload lỗi (`ValueError`), như
       `apply_pending`. Giữ loại `PLACEMENT`, và chỉ version mới nhất của mỗi attempt.
    3. `LearnerContext.from_goal(goal, placements, today())` và `learner_profile` (xem dưới).
    4. `OrderingRequest.build(...)`; `PayloadTooLarge` → Content, reason `payload_too_large`, không gọi LLM.
    5. `await llm.propose(system_prompt, payload)`. `reason` khác None → Content với reason đó.
    6. `OrderingValidator.apply(modules, proposal.payload)`. `InvalidOrdering(x)` → Content, reason
       `invalid_ordering`, `detail = x`.
    7. Thành công → `source="llm"`, modules đã sắp, `model`, `rationale`.
    `order` không bao giờ ném lỗi vì LLM; lỗi đọc DB ở bước 1 thì vẫn ném (như mọi lỗi DB khác khi tạo path).
  - `learner_profile`, luôn được tính từ goal và placement, kể cả khi dùng thứ tự Content. Văn bản tự do tiếng Anh,
    như DeepTutor mong đợi; bỏ trường nào không có dữ liệu:
    - `target_level`: `"IELTS band 6.5"`;
    - `time_budget`: `"45 minutes per day; 60 days until the exam"` (bỏ vế nào null);
    - `prior_knowledge`: `"Placement band 5.5; 7 of 12 tested knowledge points answered correctly"` (bỏ vế nào
      không có). Không có placement thì bỏ trường này.
  - Store: `PostgresLearningStore.pending_formal_payloads(user_id, goal_id) -> list[dict]`
    - `SELECT payload FROM pending_formal_assessment_results WHERE user_id = %s AND learning_goal_id = %s
       ORDER BY attempt_id, result_version`, connection riêng, không `FOR UPDATE`, không cần path.
    - Store in-memory trong `tests/formal_assessment_support.py` có method cùng tên.
  - `PathService`:
    - Constructor nhận thêm `orderer: PathOrderer | None = None`. Không có orderer (consumer, test cũ) → hành vi như
      hôm nay: thứ tự Content, không ghi event `path.ordered`, không ghi profile.
    - `_create_from_content`: sau `_scoped_curriculum`, nếu có orderer thì `outcome = await self._orderer.order(...)`;
      truyền `outcome.modules` và `ordering=outcome` vào `ensure_path`.
    - `ensure_path(..., ordering: OrderingOutcome | None = None)` chuyển tiếp xuống `_create_path`.
    - `_create_path`, trong cùng transaction, sau `path.scope_applied` và trước `apply_pending`:
      - `tx.emit("path.ordered", {...})` với `source`, `reason`, `detail`, `model`, `rationale`, `module_count`,
        `knowledge_point_count`. Không có prompt, payload hay key;
      - `self._learning.record_learner_profile(path_id, fields=outcome.learner_profile)` nếu profile không rỗng.
  - `main.py`: `get_path_service` luôn truyền `PathOrderer(store, DeepTutorOrderingLlm())`. Không kiểm tra key ở
    đây; catalog chưa cấu hình thì lời gọi trả `llm_not_configured`.
  - `/progress`, `/status`, `/map` và `POST /paths` không đổi contract. Thứ tự module/KP trả về chính là thứ tự đã sắp.
  - Consumer không nhận orderer, không import lớp LLM của DeepTutor.
- Non-functional:
  - Không giữ DB connection trong lúc chờ LLM.
  - Thời gian chờ thêm tối đa bằng timeout của pha 1 (20 giây), chỉ ở request tạo path.

## Related Code Files
- Create: `app/application/path_orderer.py` (`PathOrderer`, `OrderingOutcome`)
- Modify: `app/application/path_service.py`
- Modify: `app/persistence/postgres_learning_store.py` (`pending_formal_payloads`)
- Modify: `tests/formal_assessment_support.py` (`pending_formal_payloads` cho store in-memory)
- Modify: `main.py`
- Tests: `tests/test_path_orderer.py` (mới), `tests/test_goal_scoped_path.py`,
  `tests/test_goal_scoped_path_postgres.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**. Lời gọi LLM là một `DeepTutorOrderingLlm` giả trả `LlmProposal` định sẵn:
   - Đề xuất là hoán vị hợp lệ:
     - path theo thứ tự đó; `/status` (`next_objective()`) trỏ vào KP đầu theo thứ tự mới;
     - event `path.ordered` có `source=llm` và tên model.
   - Mỗi reason `llm_not_configured`, `llm_timeout`, `llm_error`, `llm_unusable_response`, `payload_too_large`,
     `invalid_ordering`:
     - path theo thứ tự Content; event có `source=content` và đúng reason;
     - việc tạo path vẫn thành công.
   - Có placement đang chờ:
     - payload gửi LLM có `placement` theo KP và `placement_band`;
     - sau khi tạo path, test-out vẫn áp dụng như cũ.
   - `learner_profile` được ghi qua `record_learner_profile`, có event `path.learner_profile_recorded`; không chứa
     email, tên hay id.
   - Lời gọi LLM xảy ra khi chưa mở transaction nào (store giả ghi lại thứ tự gọi).
   - Path đã có: `POST /paths` không gọi LLM.
   - Học viên làm bài sau khi path đã sắp (L5): evidence, mastery và `/status` cập nhật như cũ; thứ tự module/KP
     không đổi.
   - `PathService` không có orderer: hành vi và event giống hệt hôm nay (test cũ vẫn pass không sửa).
   - PostgreSQL: `pending_formal_payloads` đọc đúng khi không có transaction; hai request tạo path song song → một
     path, không lỗi.
2. Một test đi hết đường thật: `PathService` + `PathOrderer` + `DeepTutorOrderingLlm` thật + server giả kiểu OpenAI và
   catalog tạm (dùng `tests/deeptutor_llm_support.py` của pha 1). Path tạo ra theo thứ tự server giả trả về.
### Refactor
3. `PathOrderer`, store, `PathService`, `main.py`.
### Tests After / Regression Gate
4. Suite Python (PostgreSQL + RabbitMQ): 0 fail, 0 skip; `compileall`; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Catalog có Gemini: path mới theo thứ tự Gemini, nhưng đúng tập KP của Content.
- [ ] Catalog chưa cấu hình hoặc LLM lỗi: tạo path vẫn thành công, theo thứ tự Content, và có event ghi rõ lý do.
- [ ] Kết quả chấm về sau vẫn cập nhật path như trước.

## Risk Assessment
- **Placement tới sau khi path đã tạo**, hoặc tới giữa lúc đọc hộp chờ và lúc tạo path: thứ tự khi đó không tính
  placement đó, nhưng test-out vẫn áp dụng đúng. Sắp lại là phần "làm sau" số 2.
- **Request tạo path chậm hơn** (tối đa thêm 20 giây): chỉ xảy ra một lần mỗi goal.
