---
title: "Sắp xếp lộ trình bằng LLM (Gemini) trong phạm vi KP của Content"
description: "Khi tạo path, AI Learning gọi Gemini một lần để sắp thứ tự module và KP theo goal và kết quả placement. LLM chỉ sắp xếp, không thêm, bỏ hay chuyển KP. Kết quả được kiểm tra rồi mới ghi vào DeepTutor. Engine học của DeepTutor giữ nguyên."
status: pending
priority: P1
branch: "feat/ai-learning-service"
tags: [feature, backend, ai, llm, adaptive-learning]
blockedBy: []
blocks: []
created: "2026-09-25T15:47:00.000Z"
createdBy: "claude"
source: conversation
---

# Sắp xếp lộ trình bằng LLM (Gemini) trong phạm vi KP của Content

## Overview

DeepTutor gốc thiết kế lộ trình bằng LLM ở chế độ `outline`: tutor hỏi chuyện học viên, điền `LearnerProfile`,
rồi gọi tool `mastery_build`/`mastery_revise`. IELTSPath đã có sẵn những gì DeepTutor phải hỏi:
- band mục tiêu, ngày thi, số phút mỗi ngày (goal ở User Service);
- trình độ hiện tại (kết quả placement);
- kho KP chuẩn (Content, đã lọc theo band ở plan `260925-0425`).

Vì vậy plan này dùng **cách A: chạy ngầm một lần**. Khi tạo path, AI Learning gọi Gemini một lần với các dữ liệu
trên, và Gemini trả về **thứ tự** module và KP. Không cần session, WebSocket hay hội thoại. Chế độ hội thoại (cách
B) để lại cho Tutor runtime sau này.

Bất biến giữ nguyên từ spec V2 và các plan trước:
- DeepTutor vẫn là engine adaptive duy nhất: `compute_mastery`, cổng mastery, scheduler và `next_objective()` không đổi.
- KP trong path luôn là KP của Content, giữ nguyên id. Kết quả thi từ Assessment vẫn map được vào path.
- LLM chỉ **đề xuất thứ tự**. IELTSPath kiểm tra đề xuất rồi mới ghi vào DeepTutor, không cho LLM ghi thẳng.

## Quyết định đã chốt (hội thoại 2026-09-25)

| # | Quyết định |
| --- | --- |
| L1 | Cách A: gọi LLM một lần khi tạo path, không hội thoại. |
| L2 | Nhà cung cấp: **Gemini**, key qua biến môi trường. |
| L3 | LLM **chỉ sắp xếp lại thứ tự**, không được bỏ bớt hay thêm KP. Thêm ràng buộc: KP không được chuyển sang module khác, vì topic của Content là nhóm cố định. LLM sắp thứ tự module, và thứ tự KP bên trong từng module. |
| L4 | Xử lý khi LLM lỗi: chiến lược đầy đủ để **làm sau** (xem "Làm sau"). MVP chỉ có hành vi mặc định an toàn: giữ thứ tự của Content như hiện nay, ghi log và event, không chặn việc tạo path. |
| L5 | Học viên làm bài sau đó vẫn cập nhật path như cũ: evidence, mastery, test-out và lịch ôn. Thứ tự do LLM sắp là cố định; chỉ trạng thái học thay đổi. |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Gemini client and configuration](./phase-01-gemini-client.md) | Pending |
| 2 | [Ordering request and validation](./phase-02-ordering-request-and-validation.md) | Pending |
| 3 | [Order the path at creation](./phase-03-order-at-path-creation.md) | Pending |
| 4 | [Compose, docs and E2E](./phase-04-compose-docs-e2e.md) | Pending |

## Dependencies

- Cần plan `260925-0425-goal-scoped-curriculum-and-placement-test-out` (PR #10) đã merge: kho KP đã lọc theo band,
  bảng band theo path, và `overall_band` trong event.
- Pha 2 cần pha 1; pha 3 cần pha 1 và 2; pha 4 cần tất cả.
- Gate chung sau mỗi pha: suite Python của AI Learning (PostgreSQL + RabbitMQ), `compileall`, `git diff --check`,
  `graphify update .`. **Không có test nào gọi Gemini thật:** mọi test dùng transport giả của `httpx`.

## Làm sau (ngoài phạm vi, ghi lại theo L4)

1. **Chiến lược khi LLM lỗi:** retry có backoff, giới hạn thời gian chờ tổng, circuit breaker khi Gemini lỗi
   liên tục, sắp xếp lại bất đồng bộ (tạo path theo thứ tự Content trước, rồi sắp lại khi LLM trả lời), cảnh báo
   khi tỉ lệ lỗi cao.
2. **Sắp xếp lại khi placement tới sau khi path đã được tạo.** Hiện chỉ sắp một lần lúc tạo path. Nếu lúc đó chưa
   có placement, thứ tự chỉ dựa trên goal.
3. **Sắp xếp lại khi làm mới path (`POST /paths`).** KP mới vẫn được thêm vào cuối module theo thứ tự của Content.
4. **Chế độ hội thoại (cách B):** học viên chỉnh lộ trình qua chat với tutor. Việc này cần Tutor runtime (session,
   turn, WebSocket).
5. **Giới hạn chi phí và quota** Gemini theo học viên hoặc theo ngày; đo token.
6. Cho LLM bỏ bớt KP: cần quyết định sư phạm mới, hiện không cho phép theo L3.

## Rủi ro chung

- **Thứ tự do LLM sắp khó kiểm chứng đúng hay sai về sư phạm.** Chỉ kiểm tra được về cấu trúc (đúng tập KP, không
  chuyển module). Cần theo dõi sau khi chạy thật.
- **Độ trễ:** việc tạo path chờ LLM. Có giới hạn thời gian chờ; quá hạn thì dùng thứ tự của Content (L4).
- **Chi phí:** mỗi path một lời gọi. Số path tỉ lệ với số goal nên chi phí có giới hạn.
- **Dữ liệu gửi ra ngoài:** chỉ gửi band, số phút mỗi ngày, số ngày tới kỳ thi, tên và loại KP, đúng/sai từng KP
  trong placement. **Không** gửi email, tên, user id hay bất kỳ token nào.
