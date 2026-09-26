---
title: "Tích hợp Tutor runtime của DeepTutor vào AI Learning"
description: "Chạy nguyên Tutor của DeepTutor (mastery loop study/review/outline, sinh câu hỏi, sổ câu hỏi, reading, memory, notebook) bên trong ai-learning-service. Lưu trữ đổi sang PostgreSQL qua adapter; học viên kết nối bằng WebSocket qua Gateway; ghi chú đi vào notes của learning-support-service. Không RAG, không sửa submodule."
status: pending
priority: P1
branch: "feat/ai-learning-service"
tags: [feature, backend, ai, deeptutor, tutor, websocket]
blockedBy: []
blocks: []
created: "2026-09-26T06:00:00.000Z"
createdBy: "claude"
source: conversation
---

# Tích hợp Tutor runtime của DeepTutor vào AI Learning

## Overview

AI Learning hiện chỉ dùng **engine** của DeepTutor:
- mastery, cổng đạt, lịch ôn và `next_objective`;
- lớp LLM, để Gemini sắp thứ tự path.

`/status` nói học viên nên học KP nào tiếp theo, nhưng **chưa có chỗ để học viên học KP đó**: evidence chỉ đến từ bài thi chính thức của Assessment. Plan này đưa phần **Tutor** của DeepTutor vào AI Learning:

| Chức năng DeepTutor | Học viên nhận được |
| --- | --- |
| Mastery loop, chế độ `study` và `review` (`mastery_quiz`, `mastery_grade`, `mastery_assess`, `mastery_skip_question`) | Tutor dạy và hỏi đúng KP mà `next_objective()` chọn; câu trả lời thành evidence, mastery cập nhật ngay |
| Chế độ `outline` (`mastery_revise`, `mastery_profile`) | Học viên trò chuyện để chỉnh thứ tự lộ trình và hồ sơ học ("cách B" của plan `260925-1547`) |
| Sinh câu hỏi (`deep_question`) và Question Notebook, practice review | Luyện thêm câu hỏi theo KP; ôn lại câu đã sai theo lịch |
| Reading | Đọc bài IELTS lấy từ Content, hỏi đáp và quiz ngay trên bài đọc |
| Memory | Tutor nhớ học viên qua các buổi học |
| Notebook | Lưu nội dung hay từ buổi học vào **notes của học viên** (learning-support-service) |

Bất biến giữ nguyên từ spec V2 và các plan trước:
- DeepTutor là engine adaptive duy nhất. `ai_learning_db` là nguồn sự thật duy nhất cho path, session và evidence.
- KP trong path luôn là KP của Content, giữ nguyên id. LLM không được thêm, bỏ hay chuyển KP (L3 của plan `260925-1547`).
- Mọi lời gọi LLM đi qua lớp LLM và catalog của DeepTutor.

## Quyết định đã chốt (hội thoại 2026-09-26)

| # | Quyết định |
| --- | --- |
| T1 | **Adapter PostgreSQL.** Chạy nguyên code Tutor của DeepTutor. Lúc process khởi động, thay kho lưu trữ mặc định (SQLite/file) bằng adapter PostgreSQL của AI Learning. Không sửa submodule. Pha 1 là spike go/no-go. |
| T2 | **WebSocket qua Gateway**, dùng đúng giao thức `unified_ws` của DeepTutor (`turn_protocol`). |
| T3 | **Notebook → `notes` của learning-support-service**, thêm cột nguồn `source_type`, `source_reference_id` (giống flashcards). Không dùng notebook dạng file của DeepTutor. |
| T4 | **Không RAG**: knowledge base, embedding và các capability đọc kho tài liệu đều tắt. |
| T5 | Suy ra từ L3: kho path **từ chối** mọi thay đổi module làm lệch tập KP của Content. `mastery_build` bị chặn; `mastery_revise` chỉ được sắp lại thứ tự. |

## Vì sao cần adapter (đã kiểm chứng trên DeepTutor v1.6.9)

- Tutor tự tạo `LearningService(LearningStore())` ở khoảng 40 chỗ (`capabilities/mastery/tools.py`, `binding.py`, `services/session/turns/*`, `api/routers/mastery_path.py`…). `LearningStore` là SQLite theo workspace, và không có hook để thay.
- Kho session chỉ chọn được `SQLiteSessionStore` hoặc `PocketBaseSessionStore` (`services/session/__init__.py`). Giao diện chung là `SessionStoreProtocol` (`services/session/protocol.py`), nên có thể viết bản cho PostgreSQL.
- Memory (`services/memory`) và notebook (`services/notebook`) lưu file dưới `PathService` của từng người dùng.
- `unified_ws` nhận `ApplicationContainer` từ `ws.app.state.application_container`, nên app chủ (AI Learning) có thể tự dựng container.
- Người dùng hiện tại nằm trong ContextVar `deeptutor.multi_user.context` (`set_current_user`). Mọi phạm vi theo người dùng của DeepTutor đọc từ đây.

Cách T1: một module `app/deeptutor_runtime/bootstrap.py` chạy **trước khi** import bất kỳ capability nào, và gán lại các
điểm dưới đây sang bản của AI Learning. Có test khóa danh sách này, để lần nâng DeepTutor sau báo ngay nếu thiếu điểm nào.

### Điểm thay đã kiểm chứng trên v1.6.9 (2026-09-26)

| # | Điểm thay | Vì sao làm được | Ràng buộc |
| --- | --- | --- | --- |
| B1 | `deeptutor.learning.storage.LearningStore` → adapter pha 2 | `deeptutor/learning/__init__.py` chỉ import `models`. Có 4 module import `LearningStore` ở top-level (`learning/service.py`, `learning/navigation.py`, `api/routers/sessions.py`, `api/routers/mastery_path.py`); các module còn lại import trong hàm. | Bootstrap phải chạy **trước** mọi import `deeptutor.learning.service`: là dòng import đầu tiên của `main.py`, của `app/messaging/assessment_consumer.py` và của `tests/conftest.py`. AI Learning hiện import `deeptutor.learning.service` ở nhiều file. Adapter nhận tham số `root` như bản gốc, nhưng bỏ qua giá trị. |
| B2 | `deeptutor.services.session.{get_session_store, get_sqlite_session_store, get_sqlite_session_store_for}` **và** `deeptutor.services.session.sqlite_store.get_sqlite_session_store` → adapter pha 3 | Package init chỉ import `protocol`, `sqlite_store` và `turn_runtime`. Có 6 module import ở top-level qua package (`app/container.py`, routers `sessions`, `practice`, `courses`, `dashboard`, `question_notebook`), khoảng 40 chỗ import trong hàm, và 3 chỗ lấy thẳng từ `sqlite_store`. | Thay ở **cả hai** module. `services/workspace/session_{transfer,move}.py` dùng class `SQLiteSessionStore` trực tiếp, nhưng không nằm trong luồng Tutor (không bật workspace transfer). |
| B3 | `deeptutor.api.routers.auth.ws_require_auth` → bản IELTSPath | `unified_websocket` import hàm này **trong hàm**. | **Bắt buộc.** Khi auth của DeepTutor tắt (mặc định), bản gốc gán user là `local_admin_user()`, tức **học viên thành admin** và thấy dữ liệu của mọi người. Bản thay phải: xác thực internal JWT (header `Authorization` do Gateway gửi); tạo `CurrentUser(id=<learner uuid>, username=<learner uuid>, role="user", scope=scope_for_user(id, is_admin=False))`; gọi `set_current_user`; trả token để `unified_websocket` reset như bản gốc. Không hợp lệ → `ws.close(code=4001)` và trả `ws_auth_failed`. |
| B4 | `deeptutor.capabilities.registry.BUILTIN_LOOP_CAPABILITY_SPECS` → chỉ `mastery`, `ask_questions`, `immersive_reading`; `discover_external_loop_capabilities` → `()` | `all_loop_capabilities()` đọc tuple này mỗi turn và tự đăng ký vào catalog. | Tắt: `solve`, `obsidian`, `marginnote4`, `subagent`, `ima`, `course_study`, `immersive_watching`, **`explore_context`** (đọc nguồn tài liệu đính kèm, T4), `setup`, `partner_authoring`, `partner_group`, `visualization_generation`. Turn capability (`chat`, `deep_question`…) có registry riêng (`runtime/registry/capability_registry.py`); pha 1 xác định cách lọc tương tự. |
| B5 | `ApplicationContainer.build()` rồi gán vào `app.state.application_container` và `set_application_container(...)` | `unified_ws` ưu tiên `ws.app.state.application_container`. `set_capability_catalog` và `set_application_container` là hook có sẵn. | Coordination mặc định là `MemoryCoordinator` (trong process), không cần Redis. Chạy **một** instance API. |
| B6 | `MemoryStore` / `get_memory_store` → adapter pha 9 | `services/memory/store.py` có `get_memory_store()`. | Pha 9 đo các kiểu import như B1 và B2. |
| B7 | `NotebookManager` → adapter pha 10 | `services/notebook/service.py`. | Như trên. |

Thư viện: trong venv sạch chỉ có `requirements-test.txt` hiện tại, import `deeptutor.app.container`, `api.routers.unified_ws`,
các capability `mastery`, `reading` và `question`, cùng `services.memory`, `notebook`, `practice` đều **thành công**, không cần gói
mới (kiểm chứng 2026-09-26). Khi chạy thật có thể cần thêm gói nạp lười; pha 1 ghi lại và thêm vào `requirements-test.txt`,
kèm ràng buộc phiên bản lấy từ `third_party/deeptutor/pyproject.toml`.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Spike: chạy một turn mastery của DeepTutor trên PostgreSQL](./phase-01-spike-postgres-adapter.md) | Pending |
| 2 | [LearningStore adapter đầy đủ](./phase-02-learning-store-adapter.md) | Pending |
| 3 | [SessionStore adapter (sessions, messages, turns, question notebook)](./phase-03-session-store-adapter.md) | Pending |
| 4 | [Tutor runtime và WebSocket trong AI Learning](./phase-04-tutor-runtime-websocket.md) | Pending |
| 5 | [Gateway WebSocket và xác thực](./phase-05-gateway-websocket.md) | Pending |
| 6 | [Study và review: luyện KP của path](./phase-06-study-review.md) | Pending |
| 7 | [Outline có ràng buộc và hồ sơ học viên](./phase-07-outline-profile.md) | Pending |
| 8 | [Sinh câu hỏi, Question Notebook và practice review](./phase-08-question-practice.md) | Pending |
| 9 | [Memory trên PostgreSQL](./phase-09-memory.md) | Pending |
| 10 | [Notebook vào notes của learning-support-service](./phase-10-notebook-notes.md) | Pending |
| 11 | [Reading với bài đọc từ Content](./phase-11-reading.md) | Pending |
| 12 | [Compose, tài liệu, E2E](./phase-12-compose-docs-e2e.md) | Pending |

## Dependencies

- Thứ tự: pha 1 → 2 → 3 → 4 → 5 → 6. Sau pha 6, các pha 7 đến 11 độc lập với nhau. Pha 12 cần tất cả.
- **Pha 1 là cổng go/no-go.** Nếu spike cho thấy không thể thay kho mà không sửa DeepTutor, dừng lại và quay về chọn giữa fork DeepTutor và workspace file.
- Gate sau mỗi pha:
  - suite Python AI Learning (PostgreSQL + RabbitMQ), 0 fail, 0 skip;
  - suite Java của service bị sửa;
  - `compileall`, `git diff --check`, `graphify update .`.
- **Không test nào gọi LLM thật.** LLM giả là server kiểu OpenAI (`tests/e2e/llm_stub.py`, mở rộng để kịch bản hóa tool call), đi qua lớp LLM thật của DeepTutor.
- Đọc trước: `.sdd/database/DATABASE_V5.md` mục 7 (bảng Tutor runtime đã thiết kế) và `.sdd/specs/ai-learning-phase1-plan.md`.

## Quy tắc cho người implement

1. **Không sửa gì trong `third_party/deeptutor`.** Mọi thay đổi hành vi đi qua adapter trong `app/deeptutor_runtime/`.
2. **Chạy code của DeepTutor, không viết lại.** Tool, prompt, turn engine và giao thức WebSocket là của DeepTutor. AI Learning chỉ cung cấp lưu trữ, xác thực, danh sách capability được bật, và dữ liệu Content.
3. **Một nguồn sự thật:** không để DeepTutor ghi file SQLite hay JSON nào chứa dữ liệu học viên. Có test kiểm tra thư mục `deeptutor-data/` sau E2E không có `mastery.sqlite3`, `chat_history.db`, file memory hay notebook.
4. Mọi thao tác theo người dùng chạy trong `set_current_user(learner)`. Học viên không bao giờ là admin của DeepTutor.
5. Invariant L3/T5 được kiểm tra **ở kho lưu trữ**, không phụ thuộc vào prompt.
6. Không log nội dung hội thoại, câu trả lời, prompt hay key. Log chỉ ghi id, loại event, thời gian và mã lỗi.
7. Mỗi pha: viết test fail trước, rồi code, rồi chạy gate, mỗi pha một commit. Gặp chỗ plan không rõ hoặc sai so với code thì dừng lại và hỏi.

## Làm sau (ngoài phạm vi)

1. Giới hạn chi phí LLM theo học viên hoặc theo ngày. Tutor gọi LLM nhiều hơn hẳn việc sắp thứ tự path.
2. Nhiều instance AI Learning: phối hợp turn qua Redis (`DEEPTUTOR_TURN_COORDINATION_BACKEND`).
3. Giọng nói cho Speaking (TTS/STT của DeepTutor).
4. Import bài thi chính thức sai vào Question Notebook.
5. Frontend.

## Rủi ro chung

- **Thay class lúc chạy dễ vỡ khi nâng DeepTutor.** Pin v1.6.9, có test khóa danh sách điểm thay, và chạy test hợp đồng lấy từ test của DeepTutor.
- **Khối lượng lớn.** `SQLiteSessionStore` có khoảng 150 hàm. Chỉ làm những hàm mà các capability được bật thực sự gọi (pha 1 đo); hàm còn lại ném `NotImplementedError` có tên rõ ràng.
- **Chi phí và độ trễ LLM:** mỗi turn của tutor gọi LLM nhiều lần.
- **Dữ liệu gửi ra ngoài:** hội thoại của học viên đi tới nhà cung cấp LLM. Cần ghi rõ trong điều khoản sử dụng, và không gửi email hay tên thật trong prompt.
