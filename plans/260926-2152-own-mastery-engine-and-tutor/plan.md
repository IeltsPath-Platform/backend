---
title: "AI Learning tự chủ: port engine mastery của DeepTutor và tutor study/review tự viết"
description: "Bỏ mọi phụ thuộc runtime vào DeepTutor. Port đúng hành vi lõi mastery (models, policy, scheduler, grading, một phần service) vào app/mastery, tự viết LLM client và một tutor study/review qua HTTP + SSE. third_party/deeptutor chỉ để đọc tham khảo."
status: in-progress
priority: P1
branch: "feat/ai-learning-service"
tags: [refactor, feature, backend, ai, tutor]
supersedes: "260926-0600-deeptutor-tutor-runtime"
created: "2026-09-26T21:52:00.000Z"
source: conversation
---

# AI Learning tự chủ: engine mastery port từ DeepTutor và tutor tự viết

## Overview

Yêu cầu (người dùng, 2026-09-26): **code của `ai-learning-service` không import và không phụ thuộc DeepTutor lúc chạy**.
`third_party/deeptutor` chỉ là bản clone để đọc tham khảo. Hiện trạng thì ngược lại:
- 14 trong 32 file của `app/` import `deeptutor.*`; 11 file trong `tests/` cũng vậy.
- `Dockerfile` cài `pip install ./third_party/deeptutor`.
- Test chạy với `PYTHONPATH=../../third_party/deeptutor`.
- Compose mount `deeptutor-data`.

Plan này:
1. **Port** phần lõi mastery mà app thật sự dùng vào `app/mastery/`, giữ **đúng hành vi** của DeepTutor v1.6.9.
2. **Tự viết** LLM client (API chuẩn OpenAI) thay lớp LLM của DeepTutor.
3. **Tự viết** tutor study/review: vòng lặp tool-calling nhỏ, lưu session/turn trong PostgreSQL, API HTTP + SSE qua Gateway.
4. Gỡ DeepTutor khỏi build, test, compose; thêm test chặn để không ai import lại.

Thay thế plan [`260926-0600-deeptutor-tutor-runtime`](../260926-0600-deeptutor-tutor-runtime/plan.md) (chạy nguyên runtime của DeepTutor), vốn đi ngược yêu cầu này.

## Quyết định đã chốt (hội thoại 2026-09-26)

| # | Quyết định |
| --- | --- |
| P1 | **Không import, không phụ thuộc DeepTutor**, kể cả script dev hay sinh fixture. Được đọc code DeepTutor để chuyển thể. |
| P2 | **Port đúng hành vi, không tự thiết kế lại**: cổng đạt, công thức mastery, lịch ôn, `next_objective`, cách chấm giữ nguyên như v1.6.9 (commit `da856ad`). Engine vẫn là "engine của DeepTutor", chỉ là code nằm trong repo này. |
| P3 | **Kiểm hành vi bằng test chuyển thể từ test của DeepTutor** (`deeptutor/learning/tests/`): giữ nguyên ca kiểm tra và giá trị kỳ vọng, chỉ đổi import. Không sinh fixture bằng cách chạy DeepTutor. |
| P4 | **Giấy phép Apache-2.0**: file port có header ghi nguồn và "modified"; thêm `services/ai-learning-service/licenses/DeepTutor-LICENSE.txt` (bản sao LICENSE) và `services/ai-learning-service/NOTICE`. |
| P5 | **Tutor đầu tiên chỉ study/review**: hỏi, chấm, cập nhật mastery cho KP mà `next_objective()` chọn. Outline, sinh câu hỏi, question notebook, memory, notes, reading làm sau. |
| P6 | **HTTP + SSE**, không WebSocket: mỗi lượt học là một `POST` trả `text/event-stream`. Gateway không đổi cơ chế xác thực. |
| P7 | Id của session, turn, interaction do AI Learning sinh, dùng **uuid**. Không còn vấn đề id dạng chuỗi của DeepTutor. |
| P8 | Path của tutor luôn do **server** xác định (path active của học viên). Client không gửi path id. |

## Bảng port

| Nguồn DeepTutor (`third_party/deeptutor/deeptutor/`) | Đích | App đang dùng |
| --- | --- | --- |
| `learning/models.py` (528 dòng) | `app/mastery/models.py` | `KnowledgePoint`, `KnowledgeType`, `LearningModule`, `LearningProgress`, `LearningEvidence`, `QuizAttempt`, `ErrorType`, cộng các model mà service/policy cần (`PendingQuestion`, `MasteryInteraction`…) |
| `learning/pending.py` (283), `utils/text_display.decode_escaped_unicode_for_display` | `app/mastery/pending.py` | gián tiếp (policy, service) |
| `learning/policy.py` (488) | `app/mastery/policy.py` | `next_objective`, `map_summary`, `find_knowledge_point`, `QUALITATIVE_TYPES`, `mastery_source`, `is_assessed_mastered` |
| `learning/scheduler.py` (352) | `app/mastery/scheduler.py` | `SpacedRepetitionScheduler`, `review_sort_key` |
| `learning/mastery.py` (40), `learning/grading.py` (64) | `app/mastery/mastery.py`, `grading.py` | gián tiếp |
| `learning/service.py` (1247), **chỉ phần dùng** | `app/mastery/service.py` | `get_or_create`, `replace_modules_for_path`, `set_learner_mastery_override`, `record_learner_profile`, `record_quiz_attempt`, `calculate_mastery`, `update_mastery`, `record_qualitative_in_memory`, `_record_quiz_evidence`; tutor thêm `register_question`, `record_question_answer`, `grade_interaction` |
| `learning/storage.py`: class `LearningTransaction` (dòng 104–318, bỏ `put_topic`), `LearningStoreError`, `LearningConflictError` | `app/mastery/store.py` | `PostgresLearningStore` hiện dựng `LearningTransaction` của DeepTutor trên `_Connection` |
| `services/llm` (~11.000 dòng), `structured_retry.json_with_reasoning_retry` | `app/llm/client.py` (tự viết, khoảng 150 dòng) | `deeptutor_llm.py` |
| `capabilities/mastery/tools.py`, `prompts/en/mastery_loop.yaml` | tutor tự viết (pha 5) | chỉ để tham khảo |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Test chặn và engine thuần (models, policy, scheduler, grading)](./phase-01-guard-and-pure-engine.md) | Completed (2026-09-26) |
| 2 | [Port service và hợp đồng store, chuyển toàn bộ app sang `app.mastery`](./phase-02-service-and-switch.md) | Completed (2026-09-26) |
| 3 | [LLM client riêng và gỡ DeepTutor khỏi build](./phase-03-llm-client-and-unplug.md) | Completed (2026-09-26) |
| 4 | [Lưu trữ tutor: session, message, turn](./phase-04-tutor-persistence.md) | Pending |
| 5 | [Tutor engine study/review](./phase-05-tutor-engine.md) | Pending |
| 6 | [API tutor HTTP + SSE](./phase-06-tutor-api-sse.md) | Pending |
| 7 | [Compose, E2E qua Gateway, tài liệu và spec](./phase-07-compose-e2e-docs.md) | Pending |

Thứ tự bắt buộc 1 → 7. Pha 1–3 là refactor không đổi hành vi. Pha 4–6 là tính năng mới.

### Kết quả pha 1–3 (2026-09-26)

- Service không còn phụ thuộc DeepTutor: không file nào import `deeptutor`; `.venv` không cài `deeptutor`; image
  build từ Dockerfile mới không có `third_party` và import được API lẫn consumer.
- Gate: `pytest tests -rs` không có `PYTHONPATH` tới `third_party` → **305 passed, 0 skipped, 0 failed** (có PostgreSQL,
  RabbitMQ); `compileall` sạch. Mốc trước refactor: 179 passed; mọi test cũ chỉ đổi import, không đổi kỳ vọng.
- Engine port: 96 test engine của DeepTutor chuyển thể (`tests/test_mastery_{models,policy,scheduler,grading}.py`)
  và 22 test service (`tests/test_mastery_service.py`, trên `tests/mastery_memory_store.py`); ca bị loại ghi ở đầu mỗi file.
- `state_json` do model DeepTutor ghi: 5/5 path trong DB dev đọc và ghi lại giống hệt bằng model port (kiểm một lần,
  không commit dữ liệu học viên).
- Lệch so với file pha: test để phẳng `tests/test_mastery_*.py` theo quy ước repo; nhóm interaction của
  `LearningTransaction` port ngay ở pha 2 vì `replace_modules_for_path` cần; parse JSON không có bước json-repair
  (JSON hỏng → retry như cũ); `.gitignore` vẫn giữ `deeptutor-data/` vì thư mục cũ trên máy dev có thể chứa key.
- Chưa làm: E2E ordering qua Gateway với stub (cần dựng đủ stack); đã thay bằng test HTTP thật với stub và smoke
  test trong image.

## Quy tắc cho người implement

1. **Không `import deeptutor`**, không thêm `third_party` vào `PYTHONPATH`, `sys.path`, `Dockerfile`, requirements hay compose. Test chặn (pha 1) giữ một **danh sách file còn được phép import**, chỉ được thu nhỏ; pha 3 làm nó rỗng.
2. **Port, không sáng tạo.** Khi chuyển thể, chép logic và hằng số (ngưỡng, hệ số, tên event, chuỗi status) nguyên văn. Chỉ bỏ phần app không dùng (SQLite, topic/RAG, book, file workspace, i18n prompt). Mỗi hàm port phải có test chuyển thể tương ứng.
3. Mỗi file port bắt đầu bằng header:
   ```python
   # Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/<file>.py @ da856ad.
   # Modified for IELTSPath: <thay đổi chính, một dòng>.
   ```
4. **Không đổi hợp đồng công khai** ở pha 1–3: response `/status`, `/map`, `/progress`, `/paths`, schema DB, event RabbitMQ, tên event `path.*`. Suite hiện có phải pass **không sửa kỳ vọng**, chỉ đổi import.
5. Mọi thao tác theo học viên kiểm chủ sở hữu path ở store. Không log nội dung hội thoại, câu trả lời, prompt hay key.
6. Không test nào gọi LLM thật; dùng `tests/e2e/llm_stub.py`.
7. Mỗi pha: test trước, rồi code, chạy gate, **một commit**. Commit message không nhắc AI, số pha hay mã plan. Gặp chỗ plan không rõ hoặc sai so với code thì dừng lại và hỏi.

## Lệnh gate

Từ `services/ai-learning-service`, cần PostgreSQL và RabbitMQ (`docker compose up -d ai-learning-db rabbitmq`).

```bash
export PYTHONDONTWRITEBYTECODE=1
export AI_LEARNING_TEST_DATABASE_URL=postgresql://<user>:<pass>@localhost:<port>/<db_test>
export AI_LEARNING_TEST_AMQP_URL=amqp://<user>:<pass>@localhost:5672/
python -m pytest tests -rs          # 0 fail, 0 skip
python -m compileall -q app main.py
git diff --check && graphify update .
```

Pha 1–2 vẫn cần `PYTHONPATH="../../third_party/deeptutor;."` cho các file chưa chuyển. **Từ pha 3, gate chạy không có `PYTHONPATH` tới `third_party`.** Pha 7 thêm Gateway: `mvn -pl infra/api-gateway -am test` nếu có đổi cấu hình Gateway.

## Làm sau (ngoài phạm vi)

1. Outline có ràng buộc (chỉnh thứ tự path bằng hội thoại), hồ sơ học viên qua hội thoại.
2. Sinh câu hỏi luyện thêm, question notebook, practice review.
3. Memory của tutor; lưu ghi chú vào notes của learning-support; reading với bài của Content.
4. Stream từng token của LLM (pha 5–6 stream theo từng bước).
5. Giới hạn chi phí LLM theo học viên.

## Rủi ro

- **Lệch hành vi khi port**: phòng bằng test chuyển thể, không sửa kỳ vọng của suite hiện có.
- **Tutor tự viết đơn giản hơn DeepTutor**: prompt chuyển thể từ `mastery_loop.yaml`; chất lượng dạy cần đánh giá với Gemini thật sau pha 7.
- **Spec của nhóm nói ngược lại** (`.sdd/specs/ai-learning-phase1-plan.md`: "install DeepTutor from the local pinned submodule"). Pha 7 soạn bản sửa spec; **cần nhóm duyệt**, không tự merge.
- **Dữ liệu gửi LLM**: hội thoại đi tới nhà cung cấp; không gửi email hay tên thật trong prompt.
