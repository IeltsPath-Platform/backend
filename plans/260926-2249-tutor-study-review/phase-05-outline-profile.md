---
phase: 5
title: "Chỉnh lộ trình bằng hội thoại và hồ sơ học viên"
status: completed
priority: P2
dependencies: [4]
effort: "~1.5d"
---

# Phase 5: Chỉnh lộ trình bằng hội thoại và hồ sơ học viên

## Overview
Học viên trò chuyện với tutor để **xem** lộ trình, **đổi thứ tự** module/KP, và cập nhật **hồ sơ học** (trình độ, mục
tiêu, thời gian, cách học ưa thích). Thêm 3 tool vào tutor hiện có: `path_outline`, `path_reorder`, `learner_profile`.
Ý tưởng từ `mastery_revise`/`mastery_profile` của DeepTutor, tự viết, **chỉ đổi thứ tự**, không thêm/bỏ/chuyển KP.

Không migration, không endpoint REST mới, không đổi Gateway.

## Ràng buộc bắt buộc: không phụ thuộc DeepTutor
- **Không import** bất cứ thứ gì từ `deeptutor` hay `third_party/` (kể cả trong test, script, import trong hàm).
- **Không** thêm `third_party` vào `sys.path`, `PYTHONPATH`, requirements, Dockerfile hay compose.
- **Không chép code** DeepTutor sang (class, hàm, schema tool, prompt nguyên văn). Pha này **tự viết**: chỉ được đọc
  `third_party/deeptutor` để hiểu ý tưởng. Mọi thứ cần dùng đã có sẵn trong `app/` (`OrderingValidator`,
  `LearningService`, `policy`, `curriculum_refresh`).
- Code mới chỉ import từ `app.*`, thư viện chuẩn và các package đã có trong `requirements.txt`.
- Gate: `tests/test_no_deeptutor_dependency.py` phải pass. Trước khi commit, chạy
  `git grep -nE "deeptutor|third_party" -- services/ai-learning-service/app services/ai-learning-service/tests ':!*.pyc'`: chỉ
  được ra các dòng **đã có sẵn** (comment ghi nguồn của file port, test chặn), không có dòng mới do pha này thêm.

## Quyết định đã chốt cho pha này

| # | Quyết định | Lý do |
| --- | --- | --- |
| P5-1 | 3 tool **luôn bật** trong mọi session, không có chế độ `outline`. | Bảng `sessions` (V5) không có cột mode; thêm mode cần migration + API mà không có nhu cầu thật. Prompt quy định khi nào dùng. |
| P5-2 | `path_reorder` nhận **thay đổi từng phần**: `module_ids` (thứ tự module đầy đủ) và/hoặc `knowledge_points` (thứ tự KP đầy đủ của **chỉ những module có đổi**). Server tự ghép phần không đổi rồi mới validate. | Path có tới 300 KP (`MAX_ORDERING_KNOWLEDGE_POINTS`) với id là UUID; bắt LLM chép lại toàn bộ dễ sai và tốn token. |
| P5-3 | Validate bằng `OrderingValidator.apply` có sẵn (so với modules **hiện tại** của path, không so với Content). | Đã đảm bảo đúng bất biến: không thêm, bỏ, trùng, chuyển KP giữa module, module lạ. DRY. |
| P5-4 | Ghi bằng `LearningService.replace_modules_for_path(..., event_type="path.reordered_by_learner", session_id, turn_id)` bên trong **một** `store.transaction(path_id)` cùng bước validate. | Hàm này đã giữ mastery/evidence (tập KP không đổi), giữ câu hỏi đang mở và cập nhật `module_id` của nó; transaction lồng nhau join như `PathService._refresh_path`. `replace_modules` có xóa `stage_failure_counts/notes`, nhưng hai trường này không được đọc ở đâu (đã grep), nên chấp nhận được. |
| P5-5 | Logic reorder đặt ở `app/application/path_reorder.py`, **không** đặt trong `app/mastery`. | `app/mastery` là code port, không được import `app.learning`. Tool chỉ gọi hàm này, nên bất biến nằm ở tầng service chứ không nằm trong prompt. |
| P5-6 | Thứ tự giống hệt hiện tại → trả `unchanged`, không ghi, không event. | Tránh tăng revision và sinh event rỗng. |
| P5-7 | Hồ sơ học viên **đã** có trong context mọi lượt (`TutorTools.status()` trả `learner_profile`, từ pha 2). Pha này không làm lại. | Đã kiểm trong code. |
| P5-8 | `learner_profile` bỏ qua giá trị rỗng; muốn đổi một trường thì gửi text mới. | Tránh LLM vô tình xóa trường khi gửi `""`. |

## Đọc trước (chỉ phần liên quan)
- `services/ai-learning-service/app/tutor/tools.py`: `TOOL_DEFINITIONS`, `ToolOutcome`, `TutorTools.execute`/`status`.
- `services/ai-learning-service/app/tutor/prompts.py`: `SYSTEM_PROMPT`.
- `services/ai-learning-service/app/learning/path_ordering.py`: `OrderingValidator.apply`, `InvalidOrdering.reason`.
- `services/ai-learning-service/app/mastery/service.py`: `replace_modules_for_path` (dòng ~732), `record_learner_profile` (~842), `_LEARNER_PROFILE_FIELDS`, `_MAX_PROFILE_FIELD_LEN = 600`.
- `services/ai-learning-service/app/mastery/policy.py`: `next_objective` (đi theo `module.order` rồi thứ tự KP), `objective_status`, `is_mastered`.
- `services/ai-learning-service/app/application/curriculum_refresh.py`: `same_structure`, `merge_curriculum` (giữ thứ tự hiện có của path).
- `docs/contracts/tutor-sse-v1.md`: bảng event.
- Tham khảo, chỉ đọc: `third_party/deeptutor/deeptutor/capabilities/mastery/tools.py` (`mastery_revise` ~L1749, `mastery_profile` ~L1629).

## Related Code Files
- Create: `services/ai-learning-service/app/application/path_reorder.py`
- Modify: `services/ai-learning-service/app/tutor/tools.py` (3 tool, docstring "four tools" → cập nhật)
- Modify: `services/ai-learning-service/app/tutor/prompts.py` (mục Path & profile, docstring)
- Modify: `docs/contracts/tutor-sse-v1.md` (tên tool trong `tool.called`, 2 event mới)
- Modify: `services/ai-learning-service/README.md` (mục Tutor: 3 tool, quyết định P5-1)
- Create: `services/ai-learning-service/tests/test_tutor_path_tools.py`
- Modify: `services/ai-learning-service/tests/test_tutor_engine_postgres.py`, `tests/test_path_refresh.py`
- Không đổi: `app/tutor/engine.py` (tool mới tự đi qua `TOOL_DEFINITIONS` và `execute`), migration, Gateway, `app/mastery/*`.

## Hợp đồng tool (thêm vào `TOOL_DEFINITIONS`)

```text
path_outline()                                   # không tham số
  → {"modules": [{"id", "name", "order",
                  "knowledge_points": [{"id", "name", "type", "status", "mastered"}]}],
     "objective_id": "<KP next_objective() đang chọn hoặc null>"}
  name cắt 120 ký tự; status = objective_status(progress, kp); mastered = is_mastered(progress, kp).
  Không trả mastery_levels chi tiết, evidence hay câu hỏi.

path_reorder(module_ids?: [str], knowledge_points?: [{module_id: str, knowledge_point_ids: [str]}])
  Cần ít nhất một trong hai.
  → thành công: {"status": "reordered", "objective_id": ..., "modules": [{"id", "knowledge_point_ids"}]}
                 event SSE ("path.reordered", {"module_count", "knowledge_point_count"})
  → không đổi:  {"status": "unchanged"}
  → lỗi:        {"error": "<câu tiếng Anh dễ hiểu cho LLM>", "reason": "<InvalidOrdering.reason>"}

learner_profile(prior_knowledge?, target_level?, time_budget?, preferences?, notes?: str)
  Chỉ giữ key hợp lệ có giá trị là chuỗi khác rỗng; không còn gì → error.
  → {"recorded": [tên trường thật sự đổi], "learner_profile": {...5 trường...}}
  event SSE ("profile.updated", {"fields": recorded}) khi recorded khác rỗng.
```

Mô tả tool (description) phải nói rõ: `path_reorder` **chỉ đổi thứ tự**, không thêm/bỏ/chuyển KP; gọi `path_outline`
trước để lấy id; chỉ gọi khi học viên yêu cầu rõ ràng.

## Architecture

```python
# app/application/path_reorder.py
@dataclass(frozen=True)
class ReorderResult:
    status: str                      # "reordered" | "unchanged"
    progress: LearningProgress

class ReorderRejected(ValueError):   # reason = mã của InvalidOrdering hoặc "malformed"/"empty"
    def __init__(self, reason: str): ...

def reorder_path(learning: LearningService, path_id: str, *, module_ids: list[str] | None,
                 knowledge_points: list[dict] | None, session_id: str = "", turn_id: str = "") -> ReorderResult:
    if module_ids is None and knowledge_points is None: raise ReorderRejected("empty")
    with learning.store.transaction(path_id) as tx:          # khóa row path: validate + ghi là một revision
        current = sorted(tx.progress.modules, key=lambda m: m.order)
        overrides = _kp_overrides(knowledge_points)          # {module_id: [kp ids]}; module_id trùng → "duplicate_module";
                                                             # sai kiểu → "malformed"
        order = module_ids if module_ids is not None else [m.id for m in current]
        proposal = {"modules": [{"id": mid,
                                 "knowledge_point_ids": overrides.get(mid, _current_kp_ids(current, mid))}
                                for mid in order]}
        # module_id trong overrides mà không có trong order → "unknown_module" (bắt trước khi apply)
        try:
            ordered = OrderingValidator.apply(current, proposal)
        except InvalidOrdering as exc:
            raise ReorderRejected(exc.reason) from exc
        if same_structure(current, ordered):
            return ReorderResult("unchanged", tx.progress)
        progress = learning.replace_modules_for_path(path_id, ordered, event_type="path.reordered_by_learner",
                                                     session_id=session_id, turn_id=turn_id)
        return ReorderResult("reordered", progress)
```

Lưu ý khi implement:
- `_current_kp_ids(current, mid)` với `mid` lạ trả `[]`; `OrderingValidator` sẽ báo `unknown_module`.
- Payload event do `replace_modules_for_path` tự ghi (`mode`, `module_count`, `knowledge_point_count`); không thêm text hội thoại.
- `TutorTools._reorder` bắt `ReorderRejected` → `{"error": _REORDER_MESSAGES[reason], "reason": reason}`, với các
  reason: `empty`, `malformed`, `duplicate_module`, `unknown_module`, `missing_module`, `duplicate_knowledge_point`,
  `unknown_knowledge_point`, `moved_knowledge_point`, `missing_knowledge_point`. Thông điệp cho `moved_*`/`missing_*`
  cần nói rõ "only reordering is allowed; content cannot be added, removed or moved between modules".
- `TutorTools._profile` gọi `self._service.record_learner_profile(self._path_id, fields=..., session_id, turn_id)`.
- Thêm các handler mới vào dict `handlers` trong `execute`. `except` hiện có đã bắt `ValueError`, nên `ReorderRejected`
  (kế thừa `ValueError`) vẫn an toàn nếu lọt ra.

## Prompt (thêm vào `SYSTEM_PROMPT`, trước mục "Style")

```text
Path and profile:
- When the learner asks what their path contains or wants to change its order, call `path_outline` first and use the
  ids it returns verbatim.
- Call `path_reorder` only when the learner clearly asks to change the order. It can only reorder modules and the
  knowledge points inside a module; it cannot add, remove or move content between modules. If the learner asks for
  that, explain it is not possible here. Send only what changes: `module_ids` for a new module order, and
  `knowledge_points` for each module whose inner order changes. After it succeeds, tell the learner what moved and
  what they will work on next (`objective_id`).
- When the learner tells you their current level, target, available time or how they like to learn, record it with
  `learner_profile`, passing only the fields they actually stated, in their words. Never record names, emails or
  contact details.
- Reordering and profile changes never change mastery; the gate rules above still apply.
```

## Implementation Steps
1. **Test trước**: tạo `tests/test_tutor_path_tools.py` (dùng `InMemoryLearningStore` trong
   `tests/formal_assessment_support.py`, `LearningService`, `TutorTools`; dựng path 2 module × 2 KP (loại PROCEDURE)
   bằng `replace_modules_for_path`). Viết các case ở mục Tests 1–7. Chạy thấy fail.
2. Viết `app/application/path_reorder.py` theo mục Architecture.
3. Thêm 3 tool definition và 3 handler trong `app/tutor/tools.py`; cập nhật docstring module.
4. Cập nhật `SYSTEM_PROMPT` (docstring "for four tools" → "for the tutor tools").
5. Thêm test Postgres (mục Tests 8–9) và test refresh (mục Tests 10).
6. Cập nhật `docs/contracts/tutor-sse-v1.md`: `tool.called` liệt kê thêm `path_outline|path_reorder|learner_profile`;
   thêm hàng `path.reordered` `{ "moduleCount", "knowledgePointCount" }` và `profile.updated` `{ "fields": [...] }`
   (SSE tự đổi sang camelCase qua `camel()`).
7. README service: liệt kê 3 tool, ghi P5-1 và P5-2.
8. Chạy gate, rồi tạo **một commit**: `feat(tutor): let learners reorder their path and update their profile in chat`.

## Tests
`test_tutor_path_tools.py` (không cần DB):
1. `path_outline` trả module theo `order`, KP theo thứ tự trong module, đủ id/name/type/status/mastered và
   `objective_id` bằng `next_objective()`; không có khóa `expected_answer`.
2. Chỉ đổi `module_ids` → thứ tự module đổi, thứ tự KP trong module giữ nguyên, `next_objective()` trỏ tới KP đầu của
   module mới đứng đầu, đúng 1 event `path.reordered_by_learner` có session_id/turn_id, outcome có event `path.reordered`.
3. Chỉ đổi `knowledge_points` của một module → module khác và thứ tự module giữ nguyên.
4. Mỗi lỗi sau trả `error` + `reason` đúng, `progress.version` không đổi, không có event: không tham số; `module_ids`
   thiếu module, trùng module, module lạ; KP lạ; KP của module khác; thiếu KP; trùng KP; `module_id` trùng trong
   `knowledge_points`; sai kiểu (chuỗi thay vì mảng).
5. Gửi đúng thứ tự hiện tại → `unchanged`, version không đổi, không event.
6. Giữ trạng thái: KP đã có mastery/quiz attempt vẫn giữ `mastery_levels` và `quiz_attempts` sau reorder; câu hỏi đang mở
   (`register_question` rồi reorder) vẫn là `active_interaction` và `question.module_id` đúng module của KP.
7. `learner_profile`: trường hợp lệ được ghi và trả trong `recorded`; key lạ bị bỏ qua; tất cả rỗng → error; gửi lại
   giá trị cũ → `recorded == []` và không có event `profile.updated`; giá trị > 600 ký tự bị cắt.

`test_tutor_engine_postgres.py` (theo mẫu `ScriptedChat`, `call(...)`):

8. Một lượt: model gọi `path_outline`, rồi `path_reorder` đưa module 2 lên đầu, rồi trả text → SSE có `tool.called` ×2,
   `path.reordered`, `turn.completed`; đọc lại progress từ DB thấy thứ tự mới; lượt sau, system status gửi cho model có
   `objective` là KP của module mới đứng đầu.
9. Lượt gọi `learner_profile(time_budget=...)` → SSE `profile.updated`; lượt sau, status trong prompt chứa giá trị mới.

`test_path_refresh.py`:

10. Sau khi học viên reorder: refresh với curriculum không đổi → không có revision mới, thứ tự của học viên giữ nguyên;
    refresh có KP mới → KP mới nối cuối module của nó, phần còn lại giữ thứ tự của học viên.

## Gate
Chạy trong `services/ai-learning-service`:
`python -m pytest tests -rs` (0 fail, 0 skip; cần PostgreSQL + RabbitMQ như plan), `python -m compileall -q app`,
`git diff --check`. Sau đó chạy `graphify update .` ở root repo. Không đổi Gateway nên không cần `mvn`.

## Success Criteria
- [x] Học viên xem được lộ trình và đổi thứ tự bằng hội thoại; `next_objective()` đi theo thứ tự mới ngay lượt sau.
- [x] Tập KP của path không thể đổi qua tutor: đã có test cho mọi mã lỗi, và lỗi không làm tăng revision.
- [x] Mastery, evidence và câu hỏi đang mở giữ nguyên sau reorder.
- [x] Hồ sơ học viên cập nhật được từng trường qua hội thoại và hiện trong context lượt sau.
- [x] Hợp đồng SSE và README đã cập nhật; `test_no_deeptutor_dependency.py` vẫn pass.

## Risk Assessment
- **LLM chép sai UUID** → validator trả lỗi có `reason`, model sửa trong giới hạn 6 vòng. P5-2 giảm lượng id phải chép.
- **Model reorder khi học viên không yêu cầu** → server không kiểm được ý định. Giảm bằng prompt, và mỗi lần reorder
  đều có event `path.reordered_by_learner` để truy vết. Hậu quả chỉ là đổi thứ tự, không mất dữ liệu.
- **Ghi đồng thời với kết quả thi chính thức** → cả hai cùng khóa row path qua `store.transaction`, nên được tuần tự hóa.
- **Output `path_outline` lớn** (tối đa ~300 KP, cỡ 30–40k ký tự) → chấp nhận được với giới hạn hiện tại; nếu Gemini
  chậm thì sau này bỏ `name` của KP đã mastered. Chưa làm (YAGNI).
- Rollback: revert commit. Không có migration, và dữ liệu path đã reorder vẫn hợp lệ với code cũ.
