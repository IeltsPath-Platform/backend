---
phase: 5
title: "Gọn use case, test aggregate, tài liệu, E2E"
status: completed
---

# Phase 5: Gọn use case, test aggregate, tài liệu, E2E

## Việc

- Tách dựng response bài học (khối, câu, gợi ý, lời giải, asset, essay) khỏi `LearnLessonUseCase` sang
  `application/service/LessonViewAssembler`.
- Unit test cho từng aggregate (chuyển trạng thái hợp lệ và bị từ chối).
- `services/learning-service/README.md` (cấu trúc), `AGENTS.md` §3.1 (learning có aggregate/repository như template),
  `docs/system-architecture.md` nếu có mô tả cấu trúc learning.
- `mvn -q test` toàn reactor; chạy lại kịch bản E2E 95 check trên stack tạm; `graphify update .`.

## Kiểm

Tiêu chí xong ở `plan.md`.
