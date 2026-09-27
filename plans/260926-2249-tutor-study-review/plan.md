---
title: "Tutor study/review tự viết cho AI Learning"
description: "Học viên học đúng KP mà next_objective() chọn cùng một tutor AI: hỏi, chấm, cập nhật mastery. Vòng lặp tool-calling tự viết trên engine app/mastery và LLM client app/llm, lưu session/turn/message trong PostgreSQL, API HTTP + SSE qua Gateway. Không dùng code DeepTutor."
status: in_progress
priority: P2
branch: "feat/ai-learning-service"
tags: [feature, backend, ai, tutor]
blockedBy: []
created: "2026-09-26T22:49:00.000Z"
source: conversation
---

# Tutor study/review tự viết cho AI Learning

## Overview

`/status` cho biết học viên nên học KP nào tiếp theo, nhưng **chưa có chỗ để học KP đó**: evidence chỉ đến từ bài thi
chính thức. Plan này thêm một tutor AI: học viên mở buổi học, tutor dạy và hỏi đúng KP mà `next_objective()` chọn, chấm
câu trả lời, mastery cập nhật ngay, qua cổng thì sang KP tiếp theo.

Tutor **tự viết**, dựa trên ý tưởng mastery loop của DeepTutor (đặt câu hỏi thì kết thúc lượt, câu trả lời là lượt sau,
chấm tất định theo đáp án lưu ở server), **không import code DeepTutor**. Dùng engine `app/mastery` và LLM client
`app/llm/client.py` đã có từ [plan gỡ phụ thuộc DeepTutor](../260926-2152-remove-deeptutor-dependency/plan.md).

Thay cho plan cũ `260926-0600-deeptutor-tutor-runtime` (chạy nguyên runtime DeepTutor trong service; đã xóa vì đi ngược
yêu cầu không phụ thuộc DeepTutor, nội dung còn trong lịch sử git). Mọi tính năng của plan đó có mặt ở đây: study/review
(pha 1–4), năm tính năng còn lại (pha 5–9) và lưu câu luyện thành flashcard (pha 10).

## Quyết định đã chốt (hội thoại 2026-09-26)

| # | Quyết định |
| --- | --- |
| T1 | **Đợt đầu chỉ study/review** (pha 1–4): hỏi, chấm, cập nhật mastery cho KP mà `next_objective()` chọn. Các tính năng khác của plan cũ là pha 5–9. |
| T2 | **HTTP + SSE**, không WebSocket: mỗi lượt học là một `POST` trả `text/event-stream`. Gateway không đổi cơ chế xác thực. |
| T3 | Id của session, turn, interaction do AI Learning sinh, dùng **uuid**. |
| T4 | Path của tutor luôn do **server** xác định (path active của học viên). Client không gửi path id. |
| T5 | Không code DeepTutor: chỉ đọc `capabilities/mastery/tools.py` và `prompts/en/mastery_loop.yaml` để chuyển thể prompt và hợp đồng tool; phần port thêm vào engine theo quy tắc port của plan gỡ phụ thuộc. |

Mặc định trong file pha, đổi được nếu cần: tutor đọc 20 message gần nhất; tối đa 6 vòng tool mỗi lượt; học viên đóng tab
thì lượt vẫn chạy xong ở server; message tối đa 4.000 ký tự.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Lưu trữ tutor: session, message, turn](./phase-01-tutor-persistence.md) | Complete |
| 2 | [Tutor engine study/review](./phase-02-tutor-engine.md) | Complete |
| 3 | [API tutor HTTP + SSE](./phase-03-tutor-api-sse.md) | Complete |
| 4 | [Compose, E2E qua Gateway, tài liệu](./phase-04-compose-e2e-docs.md) | Complete |
| 5 | [Chỉnh lộ trình bằng hội thoại và hồ sơ học viên](./phase-05-outline-profile.md) | Complete |
| 6 | [Luyện câu hỏi theo KP, sổ câu hỏi và ôn câu sai](./phase-06-question-practice.md) | Complete |
| 7 | [Tutor nhớ học viên qua các buổi học](./phase-07-learner-memory.md) | Complete |
| 8 | [Lưu ghi chú vào notes của learning-support](./phase-08-save-to-notes.md) | Complete |
| 9 | [Đọc bài Reading của Content cùng tutor](./phase-09-reading.md) | Complete |
| 10 | [Lưu câu luyện thành flashcard](./phase-10-practice-flashcards.md) | Complete |

Đợt này làm pha 1 → 4 (study/review, khoảng 7–8 ngày công). Pha 5–9 đều phụ thuộc pha 4 và độc lập với nhau; pha 10 phụ thuộc pha 6; làm sau,
theo thứ tự nhóm cần.

Đợt study/review (pha 1–4) đã được kiểm chứng ngày 2026-09-26; xem
[báo cáo E2E](./reports/e2e-verification-260926-tutor-study-review.md). Pha 5–10 đã xong (2026-09-27): Java và Python đều đã chạy test (AI Learning 409/410, lỗi còn lại nằm ở một test pha 6 và đã sửa).

## Quy tắc cho người implement

1. **Không `import deeptutor`**; test chặn `tests/test_no_deeptutor_dependency.py` phải luôn pass.
2. Mọi thao tác theo học viên kiểm chủ sở hữu ở store. Không log nội dung hội thoại, câu trả lời, prompt hay key.
3. Đáp án đúng (`expected_answer`) không bao giờ xuất hiện trong sự kiện SSE hay message.
4. Không test nào gọi LLM thật; dùng `tests/e2e/llm_stub.py` (thêm mode `script` ở pha 2) và `tests/llm_test_support.py`.
5. Mỗi pha: test trước, rồi code, chạy gate, **một commit**. Commit message không nhắc AI, số pha hay mã plan. Gặp chỗ plan
   không rõ hoặc sai so với code thì dừng lại và hỏi.

## Lệnh gate

Như plan gỡ phụ thuộc: `python -m pytest tests -rs` (0 fail, 0 skip, có PostgreSQL và RabbitMQ), `compileall`,
`git diff --check`, `graphify update .`. Pha 4 thêm `mvn -pl infra/api-gateway -am test` nếu có đổi cấu hình Gateway.

## Làm sau (ngoài phạm vi plan này)

1. Stream từng token của LLM (đợt này stream theo từng bước).
2. Giới hạn chi phí LLM theo học viên.
3. Content chưa kiểm quyền gói PREMIUM ở chi tiết gói, asset và bài đọc (phát hiện khi lập pha 9).
4. `ContentAssetController` không có `@PreAuthorize`: mọi người dùng đã đăng nhập đều tạo và link asset được.

## Rủi ro

- **Chất lượng dạy**: tutor đơn giản hơn DeepTutor; prompt chuyển thể từ `mastery_loop.yaml`, cần đánh giá với Gemini thật sau pha 4.
- **Dữ liệu gửi LLM**: hội thoại đi tới nhà cung cấp; không gửi email hay tên thật trong prompt.
- **SSE qua Gateway**: có thể bị gom buffer hoặc timeout; pha 4 kiểm và chỉnh cấu hình route nếu cần.
