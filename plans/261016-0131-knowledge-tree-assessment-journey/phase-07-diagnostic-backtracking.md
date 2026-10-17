---
phase: 7
title: "Chẩn đoán dò ngược theo cây"
status: completed
priority: P1
dependencies: [1, 2, 6]
---

# Phase 7: Chẩn đoán dò ngược theo cây

## Trạng thái và ghi chú triển khai

- **Đã xong.** Contract: `docs/contracts/diagnostics-v1.md`. Test: `PrerequisiteDiagnosisTest` (9), `DiagnosticIntegrationTest` (6).
- Khác với plan:
  - Migration là `V16__diagnostics.sql` (V14 là prompt_version của phase 11, V15 là phase 6).
  - Không tách `DiagnosticSession`/`DiagnosticProbe` thành aggregate: là record trong `domain/vo`, luật nằm ở
    `PrerequisiteDiagnosis` (thuần) và luồng ở `DiagnosticFlow` (application/service). `DiagnosticStarter` gộp vào đó.
  - Điểm kích hoạt: `SubmitReviewUseCase` (set trượt thứ 2) và `GetReviewUseCase` (hết gói, gọi sau khi `skip()` để bài ôn
    B đã `SKIPPED` thì không bị đặt trạng thái chờ).
  - Bài ôn sinh từ chẩn đoán (`DIAGNOSTIC`) không mở chẩn đoán nữa, để việc dò ngược hữu hạn.
  - Gói của thăm dò tính là đã lộ cho Practice/bài ôn (`revealedPackageIds`, `assignedPackageIds` gộp `diagnostic_probes`).
    Thăm dò mở mà gói bị lộ nơi khác thì đổi gói khi học viên mở (`GetDiagnosticProbeUseCase`) hoặc thành `NO_SET`.
  - Bài ôn KP gốc dùng lesson đã hoàn thành dạy KP đó (`CompletedLessonLocator`, tách ra từ giỏ ôn), không có thì lesson của B.
  - Gửi lại cùng `requestId` trả lại kết quả cũ, không ghi gì thêm.
  - `GET /report` trả `action.type = DIAGNOSING` (kèm `reviewId` của B) khi phiên của B đang `PROBING`.
  - Chưa có API đọc `content_gap_signals`; phase 12 sẽ dùng bảng này.

## Overview

Mentor: "B làm B1, B2, B3 vẫn không được thì rủi ro cao là do không hiểu A… quay ngược cho làm thử mỗi cái hai ba câu,
dò tiếp cho tới khi phát hiện điểm tắc… đánh giá theo ma trận." Đây là phần khiến cây tri thức "phát huy giá trị".
Phase này thêm phiên chẩn đoán: thăm dò các KP tiên quyết bằng set ngắn, ghi ma trận kết quả, kết luận KP gốc, và giao
bài ôn cho KP gốc trước.

## Requirements

- **Kích hoạt (D8):** bài ôn chặn của KP B gặp một trong hai trường hợp:
  1. trượt set ôn thứ 2 (`failedSets == 2`);
  2. không còn gói chưa lộ (`skip()` vì hết gói).

  Đồng thời B có ít nhất một tiên quyết `ACTIVE` (lấy qua API nội bộ của phase 1). Mỗi bài ôn chỉ mở tối đa một phiên.
- **Migration `V14__diagnostics.sql`:**

  ```sql
  CREATE TABLE diagnostic_sessions (
      id UUID PRIMARY KEY,
      user_id UUID NOT NULL,
      root_review_id UUID NOT NULL REFERENCES review_items(id),
      root_kp_id UUID NOT NULL,
      status VARCHAR(12) NOT NULL CHECK (status IN ('PROBING','CONCLUDED')),
      conclusion_kp_ids UUID[] NOT NULL DEFAULT '{}',
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      concluded_at TIMESTAMPTZ,
      UNIQUE (root_review_id)
  );
  CREATE TABLE diagnostic_probes (
      id UUID PRIMARY KEY,
      session_id UUID NOT NULL REFERENCES diagnostic_sessions(id),
      kp_id UUID NOT NULL,
      parent_kp_id UUID NOT NULL,
      depth SMALLINT NOT NULL CHECK (depth BETWEEN 1 AND 3),
      package_id UUID, package_version_id UUID,
      status VARCHAR(10) NOT NULL CHECK (status IN ('OPEN','ANSWERED','NO_SET')),
      correct INTEGER, total INTEGER, passed BOOLEAN,
      request_id UUID UNIQUE, answered_at TIMESTAMPTZ,
      UNIQUE (session_id, kp_id)
  );
  CREATE TABLE content_gap_signals (
      kp_id UUID PRIMARY KEY,
      occurrences INTEGER NOT NULL,
      first_detected_at TIMESTAMPTZ NOT NULL,
      last_detected_at TIMESTAMPTZ NOT NULL
  );
  ALTER TABLE review_items ADD COLUMN waiting_on_review_ids UUID[] NOT NULL DEFAULT '{}';
  ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
  ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
      ('lesson_exercise','review_set','assessment','lesson_writing','practice_set','diagnostic_probe'));
  ```

  `question_attempt_events.source` đã có `diagnostic_probe` từ phase 4.

- **Luật (domain service `PrerequisiteDiagnosis`, thuần):**
  - Tầng 1: mỗi tiên quyết trực tiếp của B thành một lần thăm dò. Chọn một gói `PRACTICE_SET` chưa lộ với học viên
    (D7). Không còn gói → `NO_SET`, coi như "chưa rõ", không kết luận là lỗ hổng.
  - Thăm dò đạt nếu tỷ lệ đúng ≥ `PassMark` (70%).
  - Thăm dò A trượt và depth < 3 → thêm các tiên quyết của A ở depth + 1 (bỏ KP đã thăm dò trong phiên).
  - Tổng tối đa 6 lần thăm dò mỗi phiên. Hết hạn mức thì kết luận theo dữ liệu đang có.
  - **Kết luận** khi mọi lần thăm dò `OPEN` đã xong:
    - KP gốc lỗ hổng = KP thăm dò trượt mà mọi tiên quyết của nó (nếu đã thăm dò) đều đạt, hoặc nó không có tiên quyết,
      hoặc đã chạm depth 3;
    - không có lần thăm dò nào trượt → kết luận là chính B (nền đã vững).
  - **Hành động:**
    - mỗi KP gốc lỗ hổng: tạo bài ôn chặn `trigger_kind = 'DIAGNOSTIC'`;
    - bài ôn B giữ `PENDING`, chuyển sang trạng thái chờ (`waiting_on_review_ids`) và chỉ giao set mới khi các bài ôn
      KP gốc đã `DONE` hoặc `SKIPPED`. Đây là "giao lại bài cho KP gốc";
    - nếu B đã `SKIPPED` vì hết gói, các bài ôn KP gốc vẫn tạo bình thường.
  - Khi B hết gói, ghi hoặc cộng dồn `content_gap_signals` (phase 12 đưa thành việc cho R&D).
- **Ma trận:** mỗi lần thăm dò lưu đúng/tổng, đạt/trượt. Response trả ma trận kp × kết quả để hiển thị và trình bày.
- **Thăm dò ghi evidence** với `source = 'diagnostic_probe'` (event và `kp_evidence`; mở CHECK nguồn trong migration),
  nên mastery của A cập nhật theo kết quả.
- **API:**
  - `GET /api/learning/diagnostics/{id}`: phiên, các lần thăm dò, ma trận, kết luận.
  - `GET /api/learning/diagnostics/{id}/probes/{probeId}`: câu hỏi của set (không kèm đáp án).
  - `POST /api/learning/diagnostics/{id}/probes/{probeId}/submissions`: body giống `SubmitExerciseRequest`, chấm bằng
    `ItemGrading`, sau khi nộp thì lộ lời giải như set ôn (D11 của plan 261002).
  - Response bài ôn B thêm `diagnosticSessionId` và trạng thái `DIAGNOSING`.
- **Chặn:** phiên đang `PROBING` thì bài ôn B vẫn chặn như trước. Học viên phải làm các lần thăm dò để đi tiếp, và luôn
  có lối ra (hết gói thì `NO_SET`, hạn mức 6 lần).

## Related Code Files

(`L` = learning package)

- Tạo: `L/domain/service/PrerequisiteDiagnosis.java`, `L/domain/aggregate/DiagnosticSession.java`,
  `L/domain/entity/DiagnosticProbe.java`, `L/application/usecase/{GetDiagnosticUseCase, SubmitProbeUseCase}.java`,
  `L/application/service/DiagnosticStarter.java`, `L/infrastructure/persistence/JdbcDiagnosticRepository.java`,
  `L/api/controller/DiagnosticController.java`, phương thức `prerequisites(ids)` trong `LearningContentClient` và
  `RestLearningContentClient`
- Sửa: `SubmitReviewUseCase` (kích hoạt sau set trượt thứ 2), `GetReviewUseCase` (kích hoạt khi hết gói; trạng thái chờ),
  `ReviewItem` (trạng thái chờ), `ApplyAssessmentResultUseCase` (không đổi)
- Test:
  - `PrerequisiteDiagnosisTest` (bảng tình huống):
    - A đạt, X đạt → kết luận B;
    - A trượt, A không có tiên quyết → A;
    - A trượt, tiên quyết Z của A đạt → A;
    - A trượt, Z trượt → Z;
    - depth 3 → dừng;
    - 6 lần thăm dò → dừng;
    - `NO_SET` không bị coi là lỗ hổng.
  - `DiagnosticIntegrationTest`: chạy trọn luồng với seed cây phase 2.

## Implementation Steps

1. **Đỏ trước:** `PrerequisiteDiagnosisTest` với bảng tình huống ở trên.
2. Migration, aggregate, repository.
3. Nối điểm kích hoạt vào `SubmitReviewUseCase` và `GetReviewUseCase`; thêm trạng thái chờ cho bài ôn B.
4. API thăm dò, chấm, ghi evidence và event.
5. Contract `docs/contracts/diagnostics-v1.md`.
6. Test integration trọn luồng: B trượt 2 set → phiên → A trượt → bài ôn A → A `DONE` → B nhận set mới → B đạt.
7. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Kịch bản của mentor chạy được: B trượt → dò A, X, Y → chốt điểm tắc → giao bài ôn KP gốc → quay lại B.
- [ ] Không học viên nào bị kẹt: mọi nhánh đều kết thúc.
- [ ] `GET /api/learning/report` trả `action.type = DIAGNOSING` khi phiên đang chạy.

## Risk Assessment

- Seed mỏng làm nhiều lần thăm dò rơi vào `NO_SET`, chẩn đoán kém. Phụ thuộc bộ câu của phase 2.
- Học viên đoán may ở lần thăm dò (mentor có nói "làm đúng do may mắn"). Set thăm dò có ít nhất 3 câu; dưới 3 câu thì
  coi như `NO_SET`.

## Bàn giao FE

- Màn chẩn đoán: danh sách KP đang thăm dò, làm từng set, ma trận kết quả, lời kết luận.
- Bài ôn B hiện "đang chờ ôn KP nền".
