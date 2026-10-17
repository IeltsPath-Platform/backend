---
phase: 2
title: "Mastery MP-1.0"
status: pending
priority: P1
dependencies: []
---

# Phase 2: Mastery MP-1.0

## Overview

`MasteryCalculator` (port DeepTutor) lấy 5 kết quả gần nhất, trọng số 0.5→1.0, chặn trần 0.5 với 1 bằng chứng và 0.8
với 2. Mọi nguồn tính như nhau, không giảm theo thời gian. Tài liệu mentor (01 §8.3, 03 §E.1, 02 §A.3) dùng MP-1.0.

| Luật | MP-1.0 |
|---|---|
| Cửa sổ | 8 bằng chứng hợp lệ gần nhất |
| Tối đa bài luyện trong cửa sổ | 3 (nguồn luyện: lesson, Practice, luyện tự do) |
| Giảm theo tuổi | trọng số × 0.5^(tuổi ngày / 30) |
| Trọng số nguồn | thi (topic/course/practice test) 1.0, ôn 1.0, thăm dò chẩn đoán 1.0, luyện 0.6, essay chấm AI 0.5 |
| Đủ dữ liệu | ≥ 3 bằng chứng, trải trên ≥ 2 ngày khác nhau, tổng trọng số ≥ 1.0 |
| Biên đệm | đổi tier chỉ khi vượt ngưỡng ít nhất 3 điểm (0.03) so với tier cũ |
| Cùng câu | cùng câu hỏi trong 7 ngày chỉ tính lần đầu |
| Tier | `INSUFFICIENT` (chưa đủ dữ liệu), `PRIORITY` < 60, `CONSOLIDATE` 60–79, `MAINTAIN` ≥ 80 |

## Requirements

- **Domain:** `Mp1MasteryCalculator` (domain/service, thuần Java) nhận danh sách bằng chứng `(outcome, source, occurredAt,
  questionId)` và `now`, trả `(mastery, confidence, sufficient)`. Biên đệm nằm ở `MasteryPolicy` vì cần tier cũ.
- **Giữ `MasteryCalculator`** nguyên văn cùng comment nguồn DeepTutor và test gốc (AGENTS.md §5). Chọn công thức bằng
  `learning.mastery.policy: DEEPTUTOR | MP1` (mặc định `DEEPTUTOR` tới khi người dùng xác nhận, xem câu hỏi mở của plan);
  Admin ghi đè được qua `learning_rule_configs`.
- **Tham số** trong `learning.mastery.mp1.*`: `window`, `max-practice-in-window`, `half-life-days`, `min-evidence`,
  `min-distinct-days`, `min-confidence`, `hysteresis`, `same-item-days`, `source-weights.*`. Đưa vào `MasteryRules` và
  `MasteryRulesProvider` như ngưỡng hiện có.
- **Nguồn của evidence:** `kp_evidence` đã có cột nguồn (lesson, practice, review, assessment, diagnostic_probe,
  writing). Cần thêm `question_id` (null được) để áp luật cùng câu; nếu chưa có thì migration learning thêm cột và lấy
  từ `question_attempt_events` khi ghi mới. Bằng chứng cũ không có `question_id` thì không áp luật cùng câu.
- **Migration learning `V21__mastery_mp1.sql`** (số tạm):
  - `kp_mastery_profiles.tier` thêm `INSUFFICIENT` vào CHECK;
  - thêm `confidence NUMERIC(6,3)` vào profile;
  - thêm `question_id` vào `kp_evidence` nếu thiếu.
- **Tier `INSUFFICIENT`:**
  - không vào giỏ ôn, không vào nhóm `due`;
  - gợi ý coi như KP chưa học (đưa vào `NEXT_IN_TREE` nếu tiên quyết đã đạt);
  - **luật sinh bài ôn chặn giữ nguyên**: kiểm `ReviewReevaluation` trước khi code; nếu nó dùng tier thì với
    `INSUFFICIENT` dùng mastery thô so với `priority-below` như cũ, để không làm mất bài ôn chặn.
- **Giảm theo thời gian khi không có bằng chứng mới:** mastery đổi theo `now`, nên thêm `MasteryDecayJob`
  (infrastructure/scheduler, chạy mỗi ngày, khóa advisory như `WorkItemSyncJob`) dựng lại profile có `last_evidence_at`
  cũ hơn 1 ngày, theo lô có giới hạn, không vòng gọi repository từng dòng (AGENTS.md §3.7).
- **Đổi công thức:** khi đổi `policy` thì chạy lại nền (`backfill`) như khi Admin đổi ngưỡng.

## Related Code Files

(`L` = `learning-service/src/main/java/com/ieltspath/learning`)

- Tạo: `L/domain/service/Mp1MasteryCalculator.java`, `L/domain/vo/EvidenceSource.java` (nếu chưa có),
  `L/infrastructure/scheduler/MasteryDecayJob.java`, migration
- Sửa: `L/domain/service/MasteryPolicy.java`, `L/domain/vo/MasteryRules.java`,
  `L/application/service/MasteryRulesProvider.java`, `L/application/service/MasteryProfileUpdater.java`,
  `SuggestionRules`, `GetReviewBasketUseCase`, `infra/config-server/config-repo/learning-service.yaml` (khóa mới của
  chính service này)
- Test: `Mp1MasteryCalculatorTest` (bảng: 1–2 bằng chứng → không đủ; 3 bằng chứng cùng ngày → không đủ; half-life 30
  ngày → trọng số 0.5; 5 bài luyện chỉ tính 3; cùng câu trong 7 ngày tính 1 lần; trọng số nguồn), `MasteryPolicyTest`
  (biên đệm: 0.78 từ CONSOLIDATE lên không đổi; 0.83 thì lên MAINTAIN; 0.58 từ CONSOLIDATE xuống không đổi; 0.56 xuống),
  `MasteryProfileIntegrationTest` (đổi policy + backfill, decay job, bài ôn chặn vẫn sinh như cũ)

## Implementation Steps

1. Đọc `ReviewReevaluation`, `MasteryProfileUpdater`, `kp_evidence` để chốt điểm nối.
2. **Đỏ trước:** `Mp1MasteryCalculatorTest`, `MasteryPolicyTest` cho biên đệm.
3. Calculator, cấu hình, provider; chọn policy.
4. Migration và tier `INSUFFICIENT` ở giỏ ôn, gợi ý, báo cáo.
5. Decay job.
6. Cập nhật AGENTS.md §3.8 (công thức mặc định, giữ port DeepTutor), contract `learner-report-v1.md` (tier mới).
7. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [x] Với `policy=MP1`, học viên có bằng chứng chưa đủ thấy tier `INSUFFICIENT`, không bị đưa vào giỏ ôn. (test: `Mp1MasteryIntegrationTest.aPointWithTooLittleEvidence...`, `Mp1MasteryCalculatorTest.oneOrTwoPiecesOfEvidenceAreNotEnough`)
- [x] Bằng chứng 60 ngày tuổi có trọng số 0.25 so với bằng chứng mới. (test: `Mp1MasteryCalculatorTest.evidenceHalvesEveryThirtyDays`)
- [x] Mastery dao động 78–82 không làm tier nhảy qua lại. (test: `MasteryPolicyTest.aScoreSwingingAroundAThresholdDoesNotFlipTheTier`)
- [x] Bài ôn chặn sinh ra đúng như trước ở bộ test hiện có của plan 261009 và plan 1. (test: toàn bộ learning-service 523/0/0)
- [x] `policy=DEEPTUTOR` cho kết quả giống hệt hiện tại. (test: `MasteryProfileIntegrationTest` và `MasteryCalculatorTest` giữ nguyên)

## Risk Assessment

- Đổi công thức đổi tier của học viên đang học: để cờ, bật sau khi xác nhận, chạy backfill.
- Decay job trên nhiều học viên: chạy theo lô, giới hạn thời gian, idempotent.
- Trọng số essay AI 0.5 làm KP Writing lên chậm hơn: ghi rõ trong contract.

## Bàn giao FE

Tier `INSUFFICIENT` hiển thị "Chưa đủ dữ liệu" (màu xám), kèm số bằng chứng còn thiếu.
