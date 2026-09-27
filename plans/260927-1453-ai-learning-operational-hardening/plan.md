---
title: "Củng cố vận hành AI Learning: hạn mức LLM và E2E pha 5–10"
description: "Chặn chi phí LLM tràn bằng hạn mức lượt tutor và tóm tắt memory theo ngày cho mỗi học viên; kiểm E2E qua Gateway toàn bộ tính năng pha 5–10 với service thật và LLM stub."
status: pending
priority: P2
branch: "feat/ai-learning-service"
tags: [ai-learning, operations, quota, e2e, tdd]
blockedBy: []
blocks: []
created: "2026-09-27T08:02:32.202Z"
createdBy: "ck:plan"
source: skill
---

# Củng cố vận hành AI Learning: hạn mức LLM và E2E pha 5–10

## Overview
Sau pha 1–10 (plan [260926-2249-tutor-study-review](../260926-2249-tutor-study-review/plan.md)), AI Learning đủ chức năng
học cho IELTSPath (xem [báo cáo brainstorm](../reports/brainstorm-260927-1453-ai-learning-vs-deeptutor-gap-report.md)).
Plan này không thêm tính năng; nó làm cho những gì đã có **dùng được thật**:
1. **Hạn mức theo ngày**: mỗi học viên có tối đa 50 lượt tutor và 10 lần tóm tắt memory mỗi ngày (mặc định, chỉnh bằng
   env), tính theo nửa đêm giờ Việt Nam; vượt thì bị chặn **trước khi gọi LLM**; lượt lỗi do hệ thống được hoàn.
2. **E2E qua Gateway** cho mọi tính năng pha 5–10, chạy với service thật và LLM stub, có báo cáo như pha 4.

Làm theo **TDD**: mỗi phase viết test khóa hành vi hiện tại trước, rồi test hành vi mới, rồi mới code.

## Quyết định đã chốt (brainstorm 2026-09-27)

| # | Quyết định |
| --- | --- |
| D1 | Hạn mức **đếm lượt** (không đếm token), đặt trong AI Learning, mức cấu hình bằng biến môi trường. **Không** trừ AI Points ở đợt này (Q-c: làm sau khi sản phẩm định giá mỗi lượt). |
| D2 | **Bỏ stream từng token** khỏi plan này. Stream xung đột với bất biến "prose trong lượt đặt thẻ câu hỏi không bao giờ hiện ra", vì model thường viết câu dẫn rồi mới gọi `mastery_quiz` trong cùng một reply. Cần một thiết kế riêng. |
| D3 | E2E dùng **service thật** (Gateway, User, Content, learning-support) cộng LLM stub ở chế độ script, mở rộng harness `tests/e2e` sẵn có. |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Hạn mức lượt tutor theo ngày](./phase-01-daily-tutor-quota.md) | Pending |
| 2 | [E2E qua Gateway cho pha 5-10](./phase-02-gateway-e2e-phases-5-10.md) | Pending |

Phase 2 phụ thuộc phase 1 (E2E kiểm cả endpoint `/tutor/usage`).

## Dependencies
- Dùng kết quả plan `260926-2249-tutor-study-review` (đã hoàn tất); không có plan nào đang mở chồng phạm vi.
- Các service phải chạy được trên máy cho E2E: Gateway, User, Content, learning-support, và compose của AI Learning.

## Quy tắc cho người implement
1. Không import hay chép code DeepTutor; `tests/test_no_deeptutor_dependency.py` luôn pass.
2. Không test nào gọi LLM thật. Không log nội dung hội thoại, câu trả lời, prompt hay key.
3. Mỗi phase một commit; commit message không nhắc AI, số pha hay mã plan.
4. Gặp chỗ plan sai hoặc không rõ so với code thì dừng và hỏi.

## Làm sau (ngoài phạm vi)
1. Stream từng token, với hợp đồng rõ ràng cho lượt có gọi thẻ câu hỏi (ví dụ bắt model gọi tool mà không viết chữ, server kiểm).
2. Trừ AI Points qua access-service (`POST /api/access/points/debit`, `idempotencyKey` = turnId), khi có giá mỗi lượt.
3. Nhắc lịch ôn qua notification-service; Immersive Watching cho Listening.

## Validation Log

### Session 1 (2026-09-27)

**Verification Results**
- Claims checked: 10 (5/phase, tier Light)
- Verified: 10 | Failed: 0 | Unverified: 0
- Kiểm: `run_turn`/`open_turn`, `finish_turn(turn, status, failure_code)`, prefix `AI_LEARNING_` của `Settings`,
  constructor `LearnerMemoryService(store)`, migration kế tiếp `V9`, helper `check/login/compose_python/stream_turn/_SNAPSHOT`
  trong `tutor_e2e.py`, `_SCRIPT_LOCK/_SCRIPT_INDEX` trong `llm_stub.py`, route `/api/ai-learning/paths/{path_id}/map`,
  service `llm-stub` trong compose.
- Vấn đề thiết kế phát hiện: đặt mức 2 lượt trong `test_tutor_api_postgres.py` sẽ làm hỏng các test khác cùng class.

**Quyết định**

| Câu hỏi | Trả lời | Ảnh hưởng |
| --- | --- | --- |
| Mức mặc định | **50 lượt tutor / 10 lần tóm tắt** mỗi ngày | Phase 1: bảng cấu hình, rủi ro |
| Lượt thất bại | **Hoàn lượt** khi `llm_error` hoặc `llm_not_configured`; lỗi khác vẫn tính | Phase 1: Q1, `refund(user, kind, usage_date)`, bọc generator trong `run_turn`, test 9b |
| Ranh giới ngày | Nửa đêm **giờ Việt Nam** (`Asia/Ho_Chi_Minh`) | Giữ nguyên |
| Test hạn mức API | **File riêng** `tests/test_tutor_quota_api_postgres.py` | Phase 1: Related files, Tests |

### Whole-Plan Consistency Sweep
- Đã rà `plan.md`, phase 1 và phase 2 tìm "100", "20", "Lượt thất bại vẫn tính", "test_tutor_api_postgres.py (đặt".
- Phase 2 (luồng 7) chỉ đọc `/tutor/usage`, không phụ thuộc mức mặc định hay chuyện hoàn lượt: không cần sửa.
- Không còn mâu thuẫn chưa giải quyết.

## Acceptance criteria
- Vượt hạn mức lượt tutor → `429` trước khi mở SSE, không gọi LLM; tóm tắt memory vượt hạn mức thì bị bỏ qua;
  lượt lỗi do hệ thống được hoàn.
- Hạn mức đúng khi nhiều request đồng thời (không vượt quá N).
- Báo cáo E2E pha 5–10 qua Gateway, mọi luồng pass.
- Toàn bộ test Python (0 fail, 0 skip) và Java liên quan pass.
