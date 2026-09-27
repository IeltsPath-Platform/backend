---
phase: 2
title: "E2E qua Gateway cho pha 5-10"
status: pending
priority: P2
dependencies: [1]
effort: "~1.5d"
---

# Phase 2: E2E qua Gateway cho pha 5-10

## Overview
Chạy mọi tính năng pha 5–10 **qua Gateway thật** với User, Content, learning-support thật và LLM stub (chế độ script), đúng
như cách pha 4 đã kiểm pha 1–4. Kết quả là script E2E chạy lại được và một báo cáo kiểm chứng.

## Hiện trạng (đã kiểm 2026-09-27)
- `services/ai-learning-service/tests/e2e/tutor_e2e.py`: chạy từ host qua Gateway (`--gateway`, `--register-disposable`),
  có helper `check`, `register_disposable`, `login`, `compose_python`, `stream_turn`, `evidence`, `formal_event`.
- `tests/e2e/llm_stub.py`, chế độ `script`:
  - đọc script từ `LLM_STUB_SCRIPT`, lấy bước theo `_SCRIPT_INDEX % len(script)`;
  - thay `${knowledge_point_id}` và `${question_id}` từ status;
  - request **không có tools** (ví dụ tóm tắt memory) chưa được xử lý ở script mode.
- `docker-compose.yml`, service `llm-stub` (profile `llm-stub`):
  - mount `llm_stub.py` và `tutor-stub-script.json` chỉ đọc;
  - không publish port.
- Content đã seed bài đọc mẫu: section `10000000-0000-4000-8000-000000000005`, gói `PRACTICE_SET` đã publish.
- Báo cáo E2E mẫu: `plans/260926-2249-tutor-study-review/reports/e2e-verification-260926-tutor-study-review.md`.

## Quyết định

| # | Quyết định |
| --- | --- |
| E1 | Script mới `tests/e2e/tutor_features_e2e.py` cho pha 5–10, **dùng lại helper** của `tutor_e2e.py` (import, không chép). `tutor_e2e.py` giữ nguyên để vẫn kiểm pha 1–4. |
| E2 | Mỗi luồng tự nạp script LLM riêng: stub thêm `POST /stub/script` (chỉ ở `script` mode) để thay script và **reset index**. Harness gọi endpoint này **bên trong mạng compose** (`docker compose exec llm-stub python -c ...`), nên không phải publish port. |
| E3 | Id cụ thể (module, KP, section) do harness lấy từ API thật (`GET /api/ai-learning/paths/{pathId}/map`, `/status`) rồi ghi thẳng vào script trước khi nạp; stub không cần thêm placeholder nào. |
| E4 | Script mode xử lý request **không có tools** (tóm tắt memory) bằng một câu trả lời cố định lấy từ bước `"memory"` của script (mặc định `"- Prefers worked examples."`). |
| E5 | Harness đóng vai frontend cho note và flashcard: nhận `note.draft` rồi `POST /api/learning-support/notes`; trả lời câu luyện rồi `POST /api/learning-support/flashcards`. |
| E6 | Học viên test là tài khoản dùng một lần (`--register-disposable`), như pha 4. Không in token, câu hỏi, câu trả lời hay nội dung hội thoại. |

## Luồng kiểm (mỗi luồng in `PASS: ...`)
1. **Đổi lộ trình và hồ sơ (pha 5):**
   - script gọi `path_outline` rồi `path_reorder` (đảo 2 module đầu, id lấy từ `/paths/{id}/map`), cuối cùng trả chữ;
   - SSE có `path.reordered`; `/status` có objective thuộc module mới đứng đầu;
   - lượt sau gọi `learner_profile(time_budget=...)`: SSE có `profile.updated`.
2. **Luyện câu hỏi (pha 6):**
   - script gọi `practice_questions` với 2 câu (1 short, 1 choice);
   - SSE có `practice.questions`, **không** có đáp án;
   - `POST /practice/entries/{id}/answer`: câu đúng trả `isCorrect`, câu sai có `dueAt`;
   - đặt lại `due_at` qua `compose_python` (SQL), rồi `GET /practice/due` và `POST /practice/reviews`; replay cùng
     `requestId` → cùng outcome;
   - `/status` revision **không đổi** vì luyện thêm.
3. **Memory (pha 7):**
   - chạy đủ lượt để có ≥ 8 message; chờ tối đa 10 giây cho tác vụ nền;
   - `GET /tutor/memory` có nội dung (từ bước `"memory"` của stub); `DELETE` → `204`, `GET` lại rỗng.
4. **Lưu note (pha 8):**
   - script gọi `save_note` với `knowledge_point_id`;
   - harness lấy `note.draft` rồi `POST /api/learning-support/notes` → `201`;
   - `GET /api/learning-support/notes?sourceType=KNOWLEDGE_POINT&sourceReferenceId=...` trả đúng note.
5. **Đọc bài (pha 9):**
   - `POST /tutor/sessions` với `readingSectionId` = section demo → `material` có `sectionId`;
   - `GET` session trả `paragraphs` (5 đoạn A–E);
   - script gọi `reading_questions` → notebook `?materialId=` có entry `source=tutor_reading`;
   - `readingSectionId` lạ → `404`, không tạo session.
6. **Flashcard (pha 10):** với câu luyện đã trả lời ở luồng 2, `POST /api/learning-support/flashcards`
   `{sourceType: PRACTICE_QUESTION, sourceReferenceId: questionId}` → `201`, lần hai → `200` cùng `id`.
7. **Hạn mức (phase 1):** `GET /tutor/usage` tăng đúng theo số lượt đã chạy trong script. Không cố chạm tới hạn mức ở E2E;
   `429` đã được test tích hợp phủ.
8. **An toàn:**
   - học viên B không đọc được session, notebook, memory của A (404 hoặc rỗng);
   - không container nào ghi file dưới `/app` (dùng lại kiểm tra snapshot của `tutor_e2e.py`).

## Related Code Files
- Create: `services/ai-learning-service/tests/e2e/tutor_features_e2e.py`
- Modify: `services/ai-learning-service/tests/e2e/llm_stub.py`:
  - thêm `POST /stub/script`, chỉ ở `script` mode; thân request là list các bước; reset `_SCRIPT_INDEX` và
    `_SCRIPT_REQUESTS` dưới `_SCRIPT_LOCK`; lưu script trong bộ nhớ, ưu tiên hơn `LLM_STUB_SCRIPT`;
  - xử lý request không có tools (E4).
- Modify (nếu cần tách helper): `tests/e2e/tutor_e2e.py` chỉ để export helper; hành vi pha 1–4 không đổi.
- Create: `plans/260927-1453-ai-learning-operational-hardening/reports/e2e-verification-<ngày>-phases-5-10.md`
- Modify: `services/ai-learning-service/README.md` (mục chạy E2E: lệnh, service cần chạy, profile `llm-stub`)

## Implementation Steps (TDD)
1. **Khóa hành vi hiện tại:** chạy `tutor_e2e.py` (pha 1–4) và ghi lại PASS trước khi sửa stub. Sau khi thêm
   `/stub/script`, chạy lại để chứng minh không đổi.
2. Thêm test đơn vị cho stub (`tests/test_llm_stub.py`, không cần mạng thật, gọi handler hoặc hàm `scripted_tutor_reply`):
   nạp script mới thì index về 0; request không có tools trả câu memory; script sai định dạng → `400`.
3. Sửa `llm_stub.py`, rồi viết `tutor_features_e2e.py` theo 8 luồng, dùng helper sẵn có.
4. Chạy trên stack thật:
   - `docker compose --profile llm-stub up -d` với `LLM_STUB_MODE=script`, cùng compose AI Learning và các service Java;
   - chạy `python tests/e2e/tutor_features_e2e.py --register-disposable`.
5. Viết báo cáo E2E (theo mẫu báo cáo pha 4), kèm kết quả gate tự động.
6. Gate: `python -m pytest tests -rs` (0 fail, 0 skip), `git diff --check`, `graphify update .`. **Một commit**:
   `test(tutor): verify reading, practice, memory, notes and flashcards through the gateway`.

## Success Criteria
- [ ] 8 luồng PASS qua Gateway với service thật và LLM stub.
- [ ] `tutor_e2e.py` (pha 1–4) vẫn PASS sau khi sửa stub.
- [ ] Báo cáo E2E có lệnh chạy, môi trường, kết quả từng luồng, gate tự động.
- [ ] Script không in token, câu hỏi, câu trả lời hay nội dung hội thoại.

## Risk Assessment
- **Stack local nặng** (5+ service Java và compose): ghi rõ thứ tự khởi động trong README; harness kiểm health của Gateway
  trước khi chạy và báo lỗi rõ.
- **Tác vụ memory chạy nền**: chờ có hạn (10 giây, poll `/tutor/memory`), không `sleep` cứng.
- **Section demo phải thuộc gói PRACTICE_SET đã publish**: seed V6 của Content bảo đảm; nếu DB Content cũ chưa chạy V6 thì
  harness báo "run Content migrations" thay vì fail mơ hồ.
- **Dữ liệu tài khoản test còn lại trong DB** (như pha 4): ghi trong báo cáo; không có luồng xóa tài khoản.
