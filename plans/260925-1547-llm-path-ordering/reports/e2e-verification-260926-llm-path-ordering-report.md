---
date: 2026-09-26
kind: verification-report
phase: 4
scope: LLM path ordering through DeepTutor's LLM layer, end to end through the Gateway
---

# Kiểm chứng E2E: sắp thứ tự path bằng LLM qua lớp LLM của DeepTutor

Kiểm chứng code của commit `13dcdd8` (pha 1–4), cộng `d5d3837` (`.gitattributes` giữ LF cho file `.sh`).
Chạy trong container Linux (cloud session). Không dùng key Gemini thật: LLM là server giả kiểu OpenAI
(`tests/e2e/llm_stub.py`), được gọi qua đúng lớp LLM của DeepTutor như khi chạy Gemini thật. Token, mật khẩu,
email, prompt và payload đều không được ghi.

## Môi trường

| Thành phần | Trạng thái |
| --- | --- |
| DB Java | `user_db`, `content_db`, `assessment_db` tạo mới; Flyway chạy lại từ đầu |
| AI Learning | compose `down -v` rồi `up`; image build từ Dockerfile của repo, thêm CA của sandbox (không commit) |
| Catalog | `deeptutor-data/user/settings/model_catalog.json` chép từ `tests/e2e/model_catalog.stub.json`; key giả |
| Server giả | service `llm-stub` (`--profile llm-stub`), đổi chế độ bằng `LLM_STUB_MODE` rồi tạo lại container |
| Service Java | config → eureka → gateway → user → content → assessment, `java -jar` trên host |

Riêng trong sandbox: Docker Hub trả 429, nên image `python:3.11-slim` của `llm-stub` được thay bằng tag cục bộ trỏ
tới image AI Learning (cũng có Python). Không ảnh hưởng tới kết quả; máy nhóm dùng image gốc.

## Dữ liệu

| Topic | `sortOrder` | Band | KP (PROCEDURE) |
| --- | --- | --- | --- |
| BASIC | 0 | 4.0–5.0 | `basic-1`, `basic-2` (tạo trước `basic-3` ở bước refresh) |
| COMPLEX | 1 | 6.0–7.0 | `complex` |
| DEMO (seed V4) | 900 | không có | `demo-kp` |

Mọi learner có goal mục tiêu 7.5, 45 phút/ngày, ngày thi 2026-12-01. Thứ tự Content là BASIC → COMPLEX → DEMO.
Server giả ở chế độ `reverse` đảo cả thứ tự module lẫn thứ tự KP trong module.

## Kết quả

| # | Kịch bản | Kết quả quan sát | |
| --- | --- | --- | --- |
| 1 | `reverse`; placement band 4.5 (`basic-1` đúng, `basic-2` và `complex` sai) chấm **trước** khi có path | Consumer `pending`. `POST /paths` (1,4 s) → module DEMO → COMPLEX → BASIC, BASIC = `basic-2`, `basic-1`. Event `path.ordered`: `source=llm`, model `gemini-2.5-flash`, 3 module / 4 KP | ✅ |
| 1 | Test-out vẫn đúng | `basic-1` `masterySource = placement`; `/status` = `probe` KP đầu tiên theo thứ tự mới (`demo-kp`) | ✅ |
| 1 | `learner_profile` ghi qua `record_learner_profile` | Event `path.learner_profile_recorded` với `target_level`, `time_budget`, `prior_knowledge`. Giá trị: "IELTS band 7.5"; "45 minutes per day; 66 days until the exam"; "Placement band 4.5; 1 of 3 tested knowledge points answered correctly". Không có email, tên hay id | ✅ |
| 1 | L5: kết quả chấm sau khi path đã có | Placement thứ hai (`complex` đúng) → consumer `applied`; `complex` thành `placement`; thứ tự module/KP **không đổi** | ✅ |
| 1 | Refresh (`POST /paths`) sau khi Content thêm `basic-3` | `addedKnowledgePointCount = 1`; thứ tự LLM giữ nguyên, `basic-3` vào cuối BASIC; vẫn chỉ có 1 event `path.ordered` (không gọi LLM lại) | ✅ |
| 2 | `unknown_kp` | Thứ tự Content; `source=content`, `reason=invalid_ordering`, `detail=unknown_knowledge_point` | ✅ |
| 3 | `slow` (server ngủ 30 s) | Path vẫn được tạo sau 20,2 s, thứ tự Content; `reason=llm_timeout` | ✅ |
| 4 | `error` (HTTP 500) | Thứ tự Content; `reason=llm_error` | ✅ |
| 5 | Xóa catalog, restart API | Thứ tự Content; `reason=llm_not_configured`, `model = null` | ✅ |

Log API có đúng một dòng `warning` cho mỗi lần LLM không dùng được (timeout, lỗi, chưa cấu hình), ví dụ
`Path ordering fallback reason=llm_timeout model=gemini-2.5-flash elapsed=20.004s error_type=TimeoutError`.
Đề xuất bị validator từ chối chỉ ghi log mức `info`, và lý do nằm trong event. Log không chứa key
(`grep stub-key` = 0), prompt hay message lỗi gốc.

## Kiểm chứng container

| Kiểm tra | Kết quả |
| --- | --- |
| Thư mục data gắn từ host, do root sở hữu | Entrypoint `chown` về uid 10001; DeepTutor ghi được `model_catalog.json`, `system.json`, `usage.sqlite3` (quyền `0600`) |
| User của process | API và consumer đều chạy bằng uid 10001 sau `setpriv` |
| `docker compose exec ai-learning-api deeptutor config show` | Provider `gemini`, model, `base_url`; `api_key` hiện `***` |
| Consumer | Không gắn `deeptutor-data`, không import lớp LLM |

## Gate

| Gate | Kết quả |
| --- | --- |
| Suite Python AI Learning (PostgreSQL + RabbitMQ) | 179 passed, 0 skipped |
| Suite Java (`mvn -o test`, toàn repo) | BUILD SUCCESS: 317 tests, 0 failures, 0 errors, 0 skipped |
| `compileall`, `git diff --check` | Sạch |

## Ghi chú

- **Sự cố trong lúc chạy, không phải lỗi của tính năng.** Lần chạy đầu, kịch bản 5 nhận 500 từ Gateway: "Connection
  reset by peer". Gateway dùng lại connection cũ tới container API vừa restart, và request không tới được API.
  Script đã được sửa để chờ API thật sự sẵn sàng và gọi thử một request có token qua Gateway. Lần chạy lại từ đầu
  (run `b22a04b4`) pass cả 5 kịch bản.
- **Điều Gateway nên biết (có từ trước, ngoài phạm vi plan):** sau khi restart một service phía sau, request đầu
  tiên qua Gateway có thể nhận 500 một lần.
- **Lỗi phát hiện sau report này (2026-09-26):** `docker compose exec` mặc định chạy bằng root. Nếu
  `deeptutor config show` chạy bằng root **trước** lần đầu API đọc catalog, DeepTutor ghi lại catalog thành file
  của root (`0600`). API (uid 10001) không đọc được, coi catalog là rỗng và ghi đè nó; path nhận
  `llm_not_configured`. Đã tái hiện ngoài container. Trong E2E ở trên, lệnh này chạy sau các kịch bản nên không
  lộ ra. README giờ dùng `docker compose exec -u appuser`.
- **Chưa kiểm chứng:** gọi Gemini thật. Nhóm tự làm theo mục "LLM path ordering" trong README AI Learning.
