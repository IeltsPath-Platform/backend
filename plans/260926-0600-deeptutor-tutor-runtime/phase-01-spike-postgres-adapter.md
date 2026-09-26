---
phase: 1
title: "Spike: chạy một turn mastery của DeepTutor trên PostgreSQL"
status: pending
priority: P1
dependencies: []
effort: "~1.5d"
---

# Phase 1: Spike, chạy một turn mastery của DeepTutor trên PostgreSQL

## Overview
Chứng minh T1 khả thi **trước khi** đầu tư các pha sau. Mục tiêu: chạy một turn `study` của capability `mastery` bằng
turn engine của DeepTutor (không phải tự gọi tool). LLM là server giả; path, session, interaction và evidence nằm trong
PostgreSQL; không file SQLite hay JSON nào của DeepTutor được tạo. Kết quả là danh sách điểm thay (patch points) và
một report go/no-go.

## Đọc trước khi code
| File (trong `third_party/deeptutor/deeptutor`) | Để làm gì |
| --- | --- |
| `learning/storage.py` | API đầy đủ của `LearningStore` và `LearningTransaction`: interactions, leases, `bind_session`, events |
| `services/session/protocol.py`, `sqlite_store.py` | Hợp đồng kho session và các bảng của nó |
| `services/session/turns/*`, `services/session/turn_runtime.py`, `runtime/turn_engine.py` | Turn chạy thế nào; chỗ nào tạo `LearningStore()` |
| `app/container.py` | `ApplicationContainer`: capability catalog, turns, coordinator |
| `capabilities/mastery/{capability,binding,tools,mode,loop}.py` | Session được gắn vào path thế nào; tool nào đọc hay ghi gì |
| `multi_user/context.py`, `services/path_service.py` | Phạm vi theo người dùng |
| `api/routers/unified_ws.py`, `api/contracts/turn_protocol.py` | Giao thức mà pha 4 sẽ dùng |

## Requirements
- Functional:
  - Script hoặc test tích hợp `tests/spike/test_mastery_turn_on_postgres.py`:
    1. Bootstrap: gán lại `LearningStore`, `get_session_store` và `get_sqlite_session_store` sang bản PostgreSQL tối thiểu,
       **trước** khi import capability.
    2. Tạo path như hôm nay (`PathService`), đặt current user = learner.
    3. Dựng `ApplicationContainer` chỉ bật capability `mastery`, rồi mở một turn `study` trên session gắn với path.
    4. Server LLM giả trả lần lượt: tool call `mastery_quiz` cho KP mà `next_objective()` chọn; rồi, sau câu trả lời của
       learner, tool call `mastery_grade`.
    5. Kiểm tra trong PostgreSQL:
       - có một `mastery_interactions` đã grade;
       - có evidence mới;
       - `mastery_levels` của KP thay đổi;
       - turn và events đã được lưu;
       - `/status` phản ánh kết quả.
    6. Sau turn, thư mục `DEEPTUTOR_HOME` tạm không có `mastery.sqlite3` hay `chat_history.db`.
  - Đo **coverage**: bọc adapter bằng proxy ghi lại mọi method được gọi. Xuất danh sách method của `LearningStore` và
    `SessionStoreProtocol` mà luồng này dùng.
  - Liệt kê mọi chỗ DeepTutor tạo store hoặc đọc file theo người dùng trong luồng này: memory, notebook, usage ledger,
    settings. Ghi rõ chỗ nào phải thay và chỗ nào để nguyên (ví dụ usage ledger và settings không phải dữ liệu học viên).
- Non-functional: không sửa submodule; spike có thể là code nháp, nhưng test phải chạy được trong suite.

## Related Code Files
- Create: `app/deeptutor_runtime/bootstrap.py` (bản đầu), `tests/spike/…`
- Create: `plans/260926-0600-deeptutor-tutor-runtime/reports/spike-postgres-adapter.md`

## Success Criteria
- [ ] Một turn study thật của DeepTutor chạy xong, toàn bộ state nằm trong PostgreSQL.
- [ ] Report có: danh sách điểm thay, method coverage, các file DeepTutor vẫn ghi, và kết luận **go** hoặc **no-go** kèm lý do.
- [ ] No-go → dừng plan, trình người dùng chọn lại giữa fork DeepTutor và workspace file.

## Risk Assessment
- **Có chỗ import `LearningStore` ở top-level trước khi bootstrap chạy:** bootstrap phải là import đầu tiên của `main.py` và
  của process consumer; có test import thứ tự.
- **Turn engine cần Redis hoặc worker riêng:** kiểm tra chế độ coordination `local` của DeepTutor đủ cho một instance.
