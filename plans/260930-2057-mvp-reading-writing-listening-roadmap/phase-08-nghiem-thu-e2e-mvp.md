---
phase: 8
title: "Nghiệm thu E2E MVP"
status: pending
priority: P1
dependencies: [7]
effort: "1 ngày"
---

# Phase 8: Nghiệm thu E2E MVP

## Overview

Chạy một học viên mới qua cả ba kỹ năng bằng API thật qua Gateway, kiểm luật lộ đáp án, trừ point, và dọn tài liệu cuối.

## Requirements

- Chạy sau khi plan chia service (`261001-0205`) đã merge: stack có `library-service`, không còn learning-support.
- Stack chạy theo `CLAUDE.md` §3: Config → Eureka → Gateway → user, content, assessment, access trên host; compose chạy
  RabbitMQ và stack AI Learning; LLM qua `llm-stub` (profile `llm-stub`) hoặc model thật có key.
- Không log hay ghi report chứa token, mật khẩu, nội dung bài viết của học viên.

## Implementation Steps

1. Đăng ký học viên mới, nạp point (đủ ≥ 2 lần chấm Writing).
2. **Reading:** `GET /topics` → học hết bài `DEMO_READING` (cố ý sai một câu lần đầu để có luyện thêm) → làm bài ôn → giao mã
   đề → làm đề qua assessment → `GET /topics` thấy PASSED và topic kế IN_PROGRESS.
3. **Writing:** nộp essay Task 2 và Task 1 → nhận band 4 tiêu chí, ví −3 mỗi lần; gửi lại cùng `requestId` không trừ thêm; rút
   hết point rồi vẫn hoàn thành bài.
4. **Listening:** mở bài có audio (URL phát được, không có transcript) → nộp form completion → xong bài thấy transcript → đề
   cuối Listening có audio, đạt thì có transcript trong lời giải.
5. **Gợi ý:** sai câu Reading có gợi ý → response và `GET /lessons/{id}` có `hint`; khối đạt thì không còn `hint`.
6. **Chặn lộ:** quét mọi response ở các bước trên: không `answerSpec`, `chartFacts`, `explanation`/transcript/`solution`
   trước khi đạt. CUSTOMER gọi `/api/content/questions`, `/packages`, `/assets/{id}` → 403; `/internal/**` qua Gateway → bị chặn.
7. Tài liệu: `docs/ai-learning-database.md` (bảng V10–V12), `.sdd/database/mvp-database.md`, `DATABASE_V5.md` đánh dấu các
   mục đã có migration; `graphify update . --force`.
8. Ghi report vào `plans/260930-2057-mvp-reading-writing-listening-roadmap/reports/`.

## Success Criteria

- [ ] Bước 2–6 pass; report E2E có kết quả từng bước (không chứa secret hay nội dung bài viết).
- [ ] `mvn -q compile -DskipTests`, test content/assessment/access/Gateway, pytest ai-learning (đủ biến DB và AMQP) pass.
- [ ] Không `.pyc`, `graphify-out/`, mp3 hay `.env` trong diff.

## Risk Assessment

- **LLM thật cho điểm dao động:** E2E dùng `llm-stub` cho bước kiểm luật; chạy thử một lần với model thật chỉ để xem chất
  lượng chấm, không làm tiêu chí pass.
