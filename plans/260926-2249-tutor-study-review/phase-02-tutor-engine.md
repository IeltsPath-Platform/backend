---
phase: 2
title: "Tutor engine study/review"
status: complete
priority: P1
dependencies: [1]
effort: "~3d"
---

# Phase 2: Tutor engine study/review

## Overview
Vòng lặp tool-calling tự viết, không phụ thuộc framework. Mỗi turn:
1. Nạp session, path và mục tiêu hiện tại (`next_objective`).
2. Gọi LLM với prompt study/review và 4 tool.
3. Thực thi tool trên engine đã port.
4. Phát sự kiện ra ngoài (pha 3 đổi sự kiện thành SSE).

Hành vi dạy học chuyển thể từ mastery loop của DeepTutor.

## Đọc trước (chỉ đọc)
Trong `third_party/deeptutor/deeptutor/`:
- `capabilities/mastery/tools.py`:
  - `MasteryStatusTool` (dòng 662), `MasteryQuizTool` (755), `MasteryGradeTool` (1015), `MasteryAssessTool` (1237): mô tả tool, tham số, kiểm tra đầu vào, kết quả trả cho LLM;
  - `final_text_override` và `_end_turn_on_card` (`capabilities/mastery/loop.py`): đặt câu hỏi xong là kết thúc turn.
- `capabilities/mastery/prompts/en/mastery_loop.yaml` (122 dòng): các khối `general`, `runtime_policy`, `loop`, `session`, `playbook`.
- `learning/service.py`: `register_question` (384), `record_question_answer` (505), `grade_interaction` (575, idempotent theo `question_id`), `record_qualitative_for_path` (1059).
- `learning/storage.py`: `LearningTransaction.get_interaction`, `active_interaction`, `put_interaction`, `abandon_active_interactions` (dòng 151–253).
- `services/session/turns/learning_adapter.py` (`_commit_mastery_card_answer`): ghi câu trả lời từ thẻ **trước** khi LLM chạy.
- Test tham khảo: `learning/tests/test_guided_mastery_updates.py` (20), phần quiz/grade/assess của `test_mastery_tools.py` và `test_mastery_choices.py`.

## Requirements
- **Port tiếp vào engine** (quy tắc port như plan refactor [260926-2152](../260926-2152-remove-deeptutor-dependency/plan.md)):
  - Nhóm interaction của `LearningTransaction` đã port trong plan refactor (`app/mastery/store.py`); chỉ thêm test nếu cần.
  - `app/mastery/service.py`: `register_question`, `record_question_answer`, `grade_interaction`, `record_qualitative_for_path` và helper.
  - Test chuyển thể tương ứng trong `tests/test_mastery_*.py`, dùng `tests/mastery_memory_store.py` khi cần store.
- **Tool** (`app/tutor/tools.py`), schema JSON gửi cho LLM:

  | Tool | Tham số | Hành vi |
  | --- | --- | --- |
  | `mastery_status` | — | Mục tiêu hiện tại (module, KP, loại, trạng thái, mastery), chế độ `study`/`review` theo `next_objective`, câu hỏi đang chờ (bản công khai, **không** có `expected_answer`) |
  | `mastery_quiz` | `knowledge_point_id`, `question`, `expected_answer`, `question_type`, `options`, `explanation`, `difficulty` (như DeepTutor) | Chỉ cho KP MEMORY/PROCEDURE của path. `register_question`. Thành công → **kết thúc turn**, phát sự kiện `question` (bản công khai) |
  | `mastery_grade` | `answer`, `question_id` | `grade_interaction` (tất định). Trả kết quả đúng/sai, mastery mới, KP đã qua cổng chưa |
  | `mastery_assess` | `knowledge_point_id`, `passed`, `feedback` | Chỉ cho KP CONCEPT/DESIGN. `record_qualitative_for_path` |

  - Tham số sai (KP không thuộc path, sai loại, không có câu hỏi chờ) → trả **lỗi dạng text cho LLM**, không làm hỏng turn.
  - Evidence từ tutor dùng `source` mặc định của engine (`mastery_path`), phân biệt với evidence chính thức.
- **Vòng lặp** (`app/tutor/engine.py`, `TutorEngine.run_turn(user_id, session_id, message | answer) -> AsyncIterator[TutorEvent]`):
  1. `begin_turn` (pha 1); lưu message của học viên.
  2. Nếu request có `answer {question_id, text}`: `record_question_answer` **trước** khi gọi LLM, như DeepTutor.
  3. Prompt: system prompt chuyển thể từ `mastery_loop.yaml`, chỉ phần study/review, tiếng Anh cho nội dung IELTS; context gồm kết quả `mastery_status` và 20 message gần nhất của session.
  4. Lặp: gọi `ChatCompletionsClient` (`app/llm/client.py`) với 4 tool. Có tool call thì chạy tool, thêm kết quả, lặp tiếp. Dừng khi LLM trả text cuối, hoặc khi `mastery_quiz` thành công. Tối đa 6 vòng; quá → `failed/too_many_rounds`.
  5. Lưu message assistant (có `question_id` nếu vừa đặt câu hỏi); `finish_turn`.
  - Sự kiện: `turn.started`, `assistant.message` (text của từng vòng), `tool.called` (chỉ tên tool), `question`, `grading`, `turn.completed`, `turn.failed` (`failure_code`). Không sự kiện nào chứa `expected_answer`, prompt hay key.
  - Lỗi LLM hoặc lỗi bất ngờ → `turn.failed`; message học viên đã lưu vẫn còn; turn không kẹt ở `running`.
- **Đồng thời**: mỗi lần tool ghi là một giao dịch ngắn trên hàng `mastery_paths` (khóa hàng và revision như hiện có). Không cần bảng lease. Hai session của cùng học viên đặt câu hỏi cùng lúc → unique index "một interaction active mỗi path" chặn câu thứ hai, và tool trả lỗi cho LLM.
- **LLM giả**: thêm mode `script` cho `tests/e2e/llm_stub.py`: đọc danh sách phản hồi (text hoặc tool call) từ `LLM_STUB_SCRIPT`, trả lần lượt. Chỉ mode này ghi lại shape của request (role, tên tool), có lock tạo sẵn ở mức module; các mode cũ không đổi.

## Implementation Steps
### Tests Before
1. Test engine port (interaction) chuyển thể.
2. Test `TutorEngine` với PostgreSQL và stub script:
   - KP PROCEDURE: turn 1 quiz → `question`, turn kết thúc, interaction `awaiting_input`. Turn 2 có `answer` → answer được ghi trước LLM → `grade` → evidence, mastery tăng, `/status` đổi. Lặp tới khi qua cổng thì `next_objective` sang KP khác.
   - KP CONCEPT: `assess` passed → mastered.
   - Trả lời sai → error record và lịch ôn; chế độ `review` khi tới hạn.
   - KP đã miễn bằng placement không được hỏi.
   - Tool sai tham số → LLM nhận lỗi, turn vẫn `completed`.
   - Quá 6 vòng → `failed/too_many_rounds`. LLM lỗi → `failed/llm_error`.
   - `expected_answer` không xuất hiện trong sự kiện hay message.
   - Kết quả thi chính thức (consumer) áp vào cùng path giữa hai turn → không mất evidence, revision đúng.
### Refactor
3. Port interaction; tools; engine; prompt; stub.
### Tests After
4. Gate đầy đủ.

## Success Criteria
- [x] Một chu kỳ quiz → answer → grade chạy hết trên PostgreSQL bằng engine port, không import DeepTutor.
- [x] Mastery do tutor cập nhật giống hệt cách engine cập nhật từ test chuyển thể.
