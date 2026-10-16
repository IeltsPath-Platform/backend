---
phase: 5
title: "Mastery profile 3 mức, hạn ôn, API báo cáo"
status: pending
priority: P1
dependencies: [4]
---

# Phase 5: Mastery profile 3 mức, hạn ôn, API báo cáo

## Overview

`MasteryCalculator` lấy trọng số theo 5 lần gần nhất và chỉ có một ngưỡng 0.6. Board yêu cầu:
- mastery theo Knowledge ID, chia 3 mức (dưới 60% / 60–79% / từ 80%);
- decay và luật retake;
- một output schema cố định Score / Error / Evidence / Action. Mentor gọi phần báo cáo học viên là "Key".

Phase này lưu mastery profile cho từng học viên × KP, tính hạn ôn, và mở API báo cáo.

## Requirements

- **Migration `V12__kp_mastery_profiles.sql`:**

  ```sql
  CREATE TABLE kp_mastery_profiles (
      user_id UUID NOT NULL,
      knowledge_point_id UUID NOT NULL,
      mastery NUMERIC(4,3) NOT NULL CHECK (mastery BETWEEN 0 AND 1),
      tier VARCHAR(12) NOT NULL CHECK (tier IN ('PRIORITY','CONSOLIDATE','MAINTAIN')),
      evidence_count INTEGER NOT NULL,
      success_streak INTEGER NOT NULL DEFAULT 0,
      last_evidence_at TIMESTAMPTZ NOT NULL,
      next_review_at TIMESTAMPTZ,
      median_duration_ms BIGINT,
      top_error_codes VARCHAR(60)[] NOT NULL DEFAULT '{}',
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (user_id, knowledge_point_id)
  );
  CREATE INDEX idx_kp_mastery_due ON kp_mastery_profiles (user_id, next_review_at);
  ```

- **Luật (domain service `MasteryPolicy`):**
  - Mastery vẫn tính bằng `MasteryCalculator`, không đổi công thức.
  - Mức: `< priority-below` (0.60) → `PRIORITY`; `< maintain-from` (0.80) → `CONSOLIDATE`; còn lại → `MAINTAIN`.
  - Hạn ôn (decay theo lịch ôn ngắt quãng):
    - `PRIORITY`: không đặt hạn, vì đã có bài ôn chặn lo;
    - `CONSOLIDATE`: `last_evidence_at + 3 ngày`;
    - `MAINTAIN`: `last_evidence_at + intervals[min(success_streak, 4)]`, với `intervals = [7, 14, 30, 45, 60]` ngày.
  - `success_streak`: tăng khi evidence mới đúng và mức không giảm; về 0 khi sai.
  - Retake giữ luật D7: chỉ lần nộp đầu của mỗi gói ghi evidence. Làm lại chỉ ghi event (`first_submission=false`),
    không đổi mastery.
  - Ngưỡng và lịch ôn đặt trong `config-repo/learning-service.yaml`, nhóm `learning.mastery.*`.
    `review-mastery-threshold` giữ là alias của `priority-below`.
- **Cập nhật profile:** application service `MasteryProfileUpdater` chạy ngay sau mỗi lần `evidence.append(...)`, cùng
  transaction, cho các KP vừa có evidence.
  - Đọc lịch sử qua `findMasteryHistories`.
  - Trung vị thời gian và 3 mã lỗi hay gặp nhất lấy từ `question_attempt_events` trong 90 ngày gần nhất.
- **Backfill:** job một lần khi khởi động (idempotent) tạo profile từ `kp_evidence` cũ.
- **API báo cáo (output schema cố định):**
  - `GET /api/learning/report` (`CUSTOMER`, của chính mình).
  - `GET /api/learning/admin/learners/{userId}/report` (`ADMIN`, `EXAMINER`).

  ```json
  {
    "userId": "…", "generatedAt": "…",
    "knowledgePoints": [{
      "knowledgeId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…", "skill": "READING", "band": 6.0,
      "score":    { "mastery": 0.72, "tier": "CONSOLIDATE", "evidenceCount": 9 },
      "errors":   [{ "code": "TFNG_NG_AS_FALSE", "count": 3 }],
      "evidence": [{ "questionVersionId": "…", "correct": false, "durationMs": 41000,
                     "source": "practice_set", "errorCode": "TFNG_NG_AS_FALSE", "at": "…" }],
      "action":   { "type": "REVIEW_REQUIRED", "reviewId": "…", "dueAt": null }
    }]
  }
  ```

  - `action.type` là một trong: `REVIEW_REQUIRED` (có bài ôn chặn), `DIAGNOSING` (phase 7), `CONSOLIDATE`, `MAINTAIN_DUE`
    (đã tới hạn), `NONE`.
  - `evidence` lấy 5 dòng gần nhất. Code, name, skill, band lấy từ content qua client đang có.
- `GET /api/learning/mastery` giữ nguyên để không gãy client.

## Related Code Files

(`L` = learning package)

- Tạo: `L/domain/service/MasteryPolicy.java`, `L/domain/vo/MasteryTier.java`, `L/domain/aggregate/MasteryProfile.java`,
  `L/application/service/MasteryProfileUpdater.java`, `L/application/usecase/GetLearnerReportUseCase.java`,
  `L/infrastructure/persistence/JdbcMasteryProfileRepository.java`, `L/api/controller/ReportController.java`, DTO báo cáo
- Sửa: các use case đang gọi `evidence.append` (lesson, Practice, set ôn, essay, assessment), `learning-service.yaml`,
  `ReviewReevaluation.java` (đọc ngưỡng từ cấu hình mới)
- Test: `MasteryPolicyTest` (biên 0.599 / 0.6 / 0.8, lịch ôn, streak), `MasteryProfileIntegrationTest`,
  `ReportControllerTest` (CUSTOMER chỉ xem của mình; EXAMINER xem được học viên khác; CUSTOMER gọi API admin → 403)

## Implementation Steps

1. **Đỏ trước:** `MasteryPolicyTest`.
2. Migration, aggregate, repository, updater; nối vào các điểm ghi evidence.
3. Backfill và test backfill idempotent.
4. Use case và controller báo cáo, contract `docs/contracts/learner-report-v1.md`.
5. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Sau mỗi lần nộp, profile của các KP liên quan đúng mức và đúng hạn ôn.
- [ ] Báo cáo trả đủ 4 khối cho mọi KP học viên đã có evidence.
- [ ] Đổi ngưỡng trong cấu hình thì mức thay đổi theo mà không cần sửa code.

## Risk Assessment

- Cập nhật đồng bộ làm request nộp bài chậm thêm. Chỉ tính cho các KP vừa đổi (thường dưới 10 KP).
- Lịch ôn là quy ước, chưa kiểm chứng. Ghi rõ trong tài liệu là "lịch ôn mặc định, cấu hình được".

## Bàn giao FE

- Màn báo cáo học viên theo cây: màu theo mức, lỗi hay gặp, việc nên làm.
- Màn hồ sơ học viên dùng API admin.
