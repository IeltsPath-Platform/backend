---
phase: 8
title: "Nghiệm thu E2E MVP"
status: completed
priority: P1
dependencies: [7]
effort: "1 ngày"
---

# Phase 8: Nghiệm thu E2E MVP

## Overview

Chạy một học viên mới qua cả ba kỹ năng bằng API thật qua Gateway, kiểm luật lộ đáp án, trừ point, hạn mức chấm, và dọn
tài liệu cuối. Đã có kiểm chứng nền Reading (topic → bài → bài ôn → đề cuối → event → topic kế, DLQ replay):
[`e2e-261002-foundation-learning-pipeline.md`](../260929-1640-lesson-learning-pipeline-mvp/reports/e2e-261002-foundation-learning-pipeline.md).
Phase này không lặp lại các kiểm tra đó ngoài phần cần để đi tiếp; nó phủ những gì report nền ghi là chưa kiểm: đăng
ký/đăng nhập thật, Writing (point, hạn mức, `requestId`), Listening (audio, transcript), gợi ý Reading.

## Requirements

- Stack Java hiện tại (sau chia service và thay ai-learning Python): Eureka → Gateway → user, content, assessment, access,
  learning chạy trên host; PostgreSQL và RabbitMQ chạy trong container. Không còn ai-learning, learning-support hay `llm-stub`.
- Theo cách của report nền: container PostgreSQL/RabbitMQ tạm, database mới (`user_db`, `content_db`, `assessment_db`,
  `access_db`, `learning_db`), credential và khóa ký JWT sinh ngẫu nhiên trong bộ nhớ, không đọc `.env`, không dùng database của
  lập trình viên. Script điều phối, stub và helper chỉ nằm trong `target/` (ignored), không commit.
- **LLM:** bước kiểm luật dùng stub OpenAI-compatible cục bộ (`POST /chat/completions`) trỏ bằng `LEARNING_LLM_BASE_URL`;
  stub trả JSON đúng định dạng `EssayGrader` đọc
  (`services/learning-service/src/main/java/com/group01/learning/application/writing/EssayGrader.java`; tiêu chí `TA|TR`,
  `CC`, `LR`, `GRA`, band 0–9 bước 0.5). Stub đếm số request để kiểm "không gọi LLM hai lần". Model thật chỉ chạy thử một lần,
  không làm tiêu chí pass.
- **Audio:** `CONTENT_MEDIA_BASE_URL` bắt buộc là `https://` (`MediaReferencePolicy` từ chối http). Seed V12 dùng key
  `listening/demo/ls1.mp3` … (8 file). Có bucket thật thì kiểm URL phát được; chưa có thì đặt một base https bất kỳ và chỉ
  kiểm URL đúng dạng `base + key`, ghi rõ giới hạn trong report.
- **Point:** user-service không có tài khoản ADMIN seed. Nạp point bằng `POST /api/access/admin/points/adjust` với JWT ADMIN
  của fixture, ký bằng khóa external tạm của lần chạy (như report nền); học viên thì đăng ký và đăng nhập thật.
- Không log hay ghi report chứa token, mật khẩu, khóa ký, nội dung bài viết, prompt hay output LLM.

## Implementation Steps

1. **Tài khoản:** `POST /api/users/register` → `POST /auth/login` qua Gateway, lấy access token CUSTOMER thật. Admin nạp point
   đủ cho ≥ 3 lần chấm (≥ 9 point); `GET /api/access/me/points` thấy số dư.
2. **Reading:** `GET /api/learning/topics` → đi hết `DEMO_READING` theo kịch bản Lan ở
   [`seed-content.md`](./seed-content.md#kịch-bản-kiểm-thử) (sai một câu lần đầu) → bài ôn → giao mã đề → làm đề qua
   `/api/assessments/*` → `GET /topics` thấy `PASSED` và topic kế `IN_PROGRESS`.
3. **Gợi ý Reading** (`lesson-learning-v1`: `hint` luôn có mặt, nullable): sai câu có gợi ý → response nộp bài và
   `GET /api/learning/lessons/{id}` có `hint` khác null; khối đạt, câu không có gợi ý, bài ôn và đề cuối đều `hint: null`.
4. **Writing Task 2 và Task 1:** nộp essay (50–1.000 từ) → nhận band 4 tiêu chí; ví −3 mỗi lần chấm thành công.
   - Gửi lại cùng `requestId`: không trừ thêm, stub không nhận thêm request.
   - Task 1: prompt cho học viên có `images` (data URI của seed), không có `chartFacts`.
   - Stub trả lỗi một lần: `GRADING` không trừ point; gửi lại cùng `requestId` thì chấm tiếp.
   - Hai request khác `requestId` gửi song song: mỗi lần chấm thành công trừ đúng 3, không trừ trùng, `llm_daily_usage` đếm
     đúng (report nền ghi một số cập nhật Writing/quota chạy ngoài advisory lock; bước này kiểm hệ quả đó).
   - Hạn mức: chạy learning-service với `LEARNING_WRITING_DAILY_GRADING_LIMIT` nhỏ (ví dụ 3) → lần vượt nhận
     `429 DAILY_LIMIT_REACHED`, stub không nhận request, không trừ point.
   - Rút hết point (admin adjust `delta` âm) → nộp nhận `402 INSUFFICIENT_POINTS`, stub không nhận request; vẫn hoàn thành
     được bài (khối essay không chặn).
5. **Listening:** mở bài có audio (`mediaUrl` = `CONTENT_MEDIA_BASE_URL` + key, không có transcript) → nộp form completion → xong
   bài thấy transcript → đề cuối Listening có audio; đạt thì lời giải có transcript.
6. **Chặn lộ:** quét mọi response ở các bước trên: không `answerSpec`, `chartFacts`, `explanation`, `solution` hay transcript
   trước khi đạt. CUSTOMER gọi `/api/content/questions`, `/api/content/packages`, `/api/content/assets/{id}` → 403;
   `/internal/**` qua Gateway → bị chặn.
7. **Tài liệu:** đánh dấu các mục đã có migration trong `.sdd/database/DATABASE_V5.md` và `.sdd/database/mvp-database.md`
   (schema learning ở DATABASE_V5 §7 và `services/learning-service/README.md`; không tạo lại `docs/ai-learning-database.md`).
   Sau khi sửa code (nếu có): `graphify update .`.
8. Ghi report vào `plans/260930-2057-mvp-reading-writing-listening-roadmap/reports/` theo dạng report nền: môi trường, bảng
   kết quả từng bước, lệnh regression, giới hạn.

## Success Criteria

- [x] Bước 1–6 pass; report có kết quả từng bước, không chứa secret hay nội dung bài viết.
- [x] `mvn -q compile -DskipTests` và `mvn -q test` (toàn reactor, Docker bật để Testcontainers chạy) pass; ghi số test
      theo module.
- [x] `docker compose config --quiet` pass.
- [x] Không `.pyc`, `graphify-out/`, mp3, `.env` hay script trong `target/` trong diff.

## Risk Assessment

- **LLM thật cho điểm dao động:** kiểm luật bằng stub; model thật chỉ để xem chất lượng chấm.
- **Chưa có bucket mp3:** chỉ kiểm được dạng URL, chưa kiểm phát được; ghi là giới hạn, không tính là pass phần "phát được".
- **Lỗi phát hiện ở bước 4 (song song/hạn mức):** không sửa code trong phase này nếu ngoài phạm vi nhỏ; ghi vào report và
  tạo plan sửa riêng.
