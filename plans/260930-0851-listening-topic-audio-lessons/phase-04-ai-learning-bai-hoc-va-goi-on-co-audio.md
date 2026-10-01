---
phase: 4
title: "AI Learning: bài học và gói ôn có audio"
status: completed
priority: P1
dependencies: [2]
effort: "1 ngày"
---

# Phase 4: AI Learning: bài học và gói ôn có audio

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md)). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Kết quả (2026-10-01, cùng PR 3 của `261001-1228`)

- Khối AUDIO: `mediaUrl`, `durationSeconds`; khóa `transcript` chỉ có khi bài xong. Set ôn: `audio {mediaUrl, durationSeconds}`; nộp đạt ≥ 70% trả `transcript`.
- Lệch plan: `transcript` của set ôn đặt ở cấp response (cạnh `results`), không nằm trong `solutions`; test dùng content giả trong `ReviewAndTestAssignmentIntegrationTest`, chưa chạy kịch bản LS1/LS2 với seed thật.

## Context Links

- Plan 1640 phase 6 (`app/lessons/`, DTO allowlist `extra="forbid"`, `GET /reviews/{id}`, nộp gói, `solutions`)
- Contract `docs/contracts/lesson-learning-v1.md` (bản sửa ở phase 1)

## Overview

Chuyển audio từ content tới học viên trong bài học và gói ôn, và giấu transcript tới khi đạt. Chấm, evidence, luật luyện
thêm, giao mã đề, consumer giữ nguyên: câu Listening là `CHOICE`/`FILL` như Reading, còn KP Listening đi cùng cơ chế mastery.

## Requirements

- **`GET /lessons/{id}`:** khối `ASSET` loại AUDIO → DTO `{blockType: "ASSET", assetType: "AUDIO", mediaUrl, durationSeconds}`.
  - Thêm `transcript` khi `lesson_progress.completed_at` khác NULL.
  - Bài chưa xong thì DTO **không có khóa** `transcript` (không phải `null`), để test tập key bắt được.
- **`GET /reviews/{id}`:** section của gói trả `audio {mediaUrl, durationSeconds}` cạnh `passage`; không có transcript.
- **Nộp gói đạt ≥ 70%:** `solutions` thêm `transcript` của section. Chưa đạt thì không có.
- **Lý thuyết của bài ôn:** chỉ khối `TEXT` như hiện tại. Không lặp lại audio của bài học; audio luyện nằm trong gói.
- **Content client:** đọc `assets[]` của section và khối AUDIO (`mediaUrl` đã do content ghép). Khối ASSET có `assetType`
  lạ thì bỏ qua và ghi log (id).
- **DTO:** map payload content sang model học viên riêng (không có trường `transcript` khi chưa đạt). Không `model_validate`
  thẳng payload content vào DTO `extra="forbid"` (payload có `transcript` sẽ thành lỗi 500).
- **Bằng chứng:** như Reading, chỉ ở lần nộp đầu của mỗi khối (luật 1640).
- Không migration, không đổi `answer-spec-v1`, không đổi `reevaluate_reviews`.

## Related Code Files

- Modify: `services/ai-learning-service/app/api/dto/lessons.py`, `app/lessons/service.py` (map khối ASSET, cờ đã xong),
  phần bài ôn trong `app/lessons/` (section audio, `solutions.transcript`), `app/clients/content_service.py`
- Tests: `tests/test_lesson_api.py`, `tests/test_review_sets.py`, `tests/test_lesson_store_postgres.py` (nếu cần kịch bản đủ)

## Implementation Steps

**Tests Before:**
1. Chạy toàn bộ test ai-learning (sau plan 1640). Kịch bản Lan và tập key của bài Reading không đổi.

**Tests After** (viết trước code; content giả):
2. `test_lesson_api.py`:
   - `LS1` chưa xong: khối AUDIO có `mediaUrl`, `durationSeconds`, **không có khóa** `transcript`;
   - nộp đủ khối đạt → bài xong → `GET` lại có `transcript`;
   - cổng `TOPIC_LOCKED` khi `DEMO_LISTENING` chưa mở.
3. `test_review_sets.py`:
   - luyện thêm `LS_SPELL` sau `LS1` (lần đầu điền sai LQ1, KP dưới ngưỡng) → gói `PS-SPELL` có `audio`, không có
     transcript;
   - nộp 1/3 → không có `solutions`; `GET` sau giao lại chính `PS-SPELL` (KP chỉ có 1 gói, luật "hết gói thì lấy gói giao lâu
     nhất" của 1640);
   - nộp 3/3 → `solutions.transcript`.
4. Kịch bản đầy đủ (DB thật, content và assessment giả): học `LS1`, `LS2` → giao mã đề Listening → event đạt → topic
   PASSED, không có topic kế (Listening đứng cuối).

**Implement:** content client → DTO bài học → bài ôn và `solutions`.

**Regression Gate:**
```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests -p no:cacheprovider
```

## Success Criteria

- [ ] Transcript chỉ xuất hiện khi bài xong hoặc gói đạt.
- [ ] Không đổi luật mastery hay luyện thêm; Reading không hồi quy.

## Risk Assessment

- **Học viên bấm "xong bài" để lấy transcript:** bài có khối bài tập không dùng được `complete` (luật 1640), nên transcript
  chỉ mở sau khi mọi khối đạt.
- **Thời lượng audio sai trong seed:** chỉ để hiển thị; không dùng để chặn.

## Security Considerations

- Allowlist `extra="forbid"` bắt được mọi trường thừa, kể cả `transcript` lỡ đi theo payload content.
