---
phase: 12
title: "Compose, tài liệu, E2E"
status: pending
priority: P1
dependencies: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
effort: "~2d"
---

# Phase 12: Compose, tài liệu, E2E

## Overview
Chạy toàn bộ Tutor qua Gateway trong compose, viết tài liệu cho nhóm, và kiểm chứng E2E bằng LLM giả, không cần key thật.
Có hướng dẫn để nhóm tự chạy thử với Gemini.

## Requirements
- Functional:
  - Compose:
    - `ai-learning-api` không cần biến mới cho LLM, vì dùng catalog như plan `260925-1547`;
    - `learning-support-service` chạy được local, vì notes cần nó;
    - server LLM giả được mở rộng để kịch bản hóa tool call của DeepTutor (quiz → grade, assess, revise, profile, save
      to notebook).
  - E2E qua Gateway (script Python bằng thư viện `websockets`, cùng bộ Postman cho phần REST):
    1. Học viên có path → mở WebSocket → study một KP tới mastered → `/status` sang KP khác.
    2. Kết quả thi chính thức tới trong lúc đang học → không mất evidence.
    3. Outline: đổi thứ tự hợp lệ được; thêm KP bị chặn.
    4. Sinh câu hỏi cho KP → làm sai → practice review có câu đó.
    5. Memory: session 2 nhắc lại điều học viên nói ở session 1.
    6. Lưu ghi chú → xuất hiện trong `GET /api/learning-support/notes?sourceType=TUTOR_SESSION`.
    7. Reading một bài của Content (nếu pha 11 xong).
    8. Học viên B không xem được session, memory hay note của A.
    9. Sau E2E, `deeptutor-data/` không có dữ liệu học viên (`mastery.sqlite3`, `chat_history.db`, memory, notebook).
  - Tài liệu:
    - README AI Learning, mục "Tutor": giao thức WebSocket (link tới `turn_protocol` của DeepTutor), subprotocol token,
      các mode, capability được bật và bị tắt, dữ liệu gửi LLM, nơi lưu memory và notes.
    - README root: cách chạy.
    - `docs/contracts`: event và message chính.
- Non-functional: report trong `reports/` không chứa token, key, nội dung hội thoại đầy đủ.

## Success Criteria
- [ ] 9 kịch bản E2E pass qua Gateway.
- [ ] Gate Java và Python: 0 fail, 0 skip.
- [ ] Nhóm chạy được Tutor với Gemini thật theo README.
