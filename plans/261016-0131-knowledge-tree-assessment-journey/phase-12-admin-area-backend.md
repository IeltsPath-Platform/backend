---
phase: 12
title: "Khu quản trị: hồ sơ 360°, cờ, hàng việc, đánh giá Mentor"
status: completed
priority: P2
dependencies: [5, 7, 9, 10, 11]
---

# Phase 12: Khu quản trị – hồ sơ 360°, cờ, hàng việc, đánh giá Mentor

## Overview

Board cụm 4 gồm hồ sơ 360°, Work Queue và Alert; mỗi vai trò ghi Action / Result / Note; kết quả đồng bộ về học viên.
Mentor: "System làm gì, Mentor làm gì, R&D làm gì… cái này phải làm", và R&D có "luồng độc lập" để chấm lại bài Mentor
đã chữa nhằm đánh giá chất lượng Mentor.

Phase này làm phần backend. Mỗi chức năng đặt ở service đang sở hữu dữ liệu (D11); frontend gom thành một khu quản trị.

## Trạng thái và ghi chú triển khai

- **Phần Assessment/Access: xong** (Codex, `dbe35e7`, `1ea5e44`, `9293d6e`). **Phần Learning: xong** (contract
  `docs/contracts/learning-staff-area-v1.md`; learning-service 471 test xanh). Còn thiếu Gateway route (Codex kiểm).
- Khác với plan ở phần Learning:
  - Migration là `V19__flags_and_work_items.sql`. `work_items` thêm cột `detail` (JSON số liệu của việc).
  - Hàng việc đồng bộ khi mở danh sách (`WorkItemSync`) và qua job định kỳ `WorkItemSyncJob` (mỗi 3 giờ, khóa advisory để
    nhiều instance không chạy trùng).
  - Cờ chạy qua sự kiện `QuestionEventsAppended` sau khi commit, transaction riêng (`FlagDetector`). Cờ cùng loại không
    nâng lại trong 24 giờ.
  - Câu quá dễ/khó dùng tỷ lệ đúng lần nộp đầu; tag `difficulty` của Content chỉ dịch ngưỡng (HARD quá dễ từ 80%, EASY quá
    khó đến 40%), không đổi bài giao cho học viên. Cần endpoint nội bộ `POST /internal/learning-content/questions/difficulties`
    của Content; chưa có thì thống kê vẫn chạy với ngưỡng chung.
  - Hồ sơ 360° chỉ đọc, không làm tươi dữ liệu học viên; tên khóa/KP do frontend ghép.

## Requirements

### Learning: hồ sơ học viên 360° (`ADMIN`, `EXAMINER`)

`GET /api/learning/admin/learners/{userId}/overview` trả:
- course đang học và tiến độ;
- điểm khóa (phase 9);
- số KP theo mức và đường dẫn tới báo cáo (phase 5);
- bài ôn đang chờ;
- phiên chẩn đoán (phase 7);
- cờ bổ trợ;
- cờ bất thường;
- việc đang mở.

Hồ sơ cá nhân và gói lấy từ API có sẵn của user-service và access-service; frontend tự ghép. Learning không gọi thêm
service khác.

### Learning: cờ bất thường

Migration `V17__flags_and_work_items.sql`:

```sql
CREATE TABLE learner_flags (
    id UUID PRIMARY KEY, user_id UUID NOT NULL,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('TOO_FAST_HIGH_SCORE','BURST_SUBMISSIONS','DUPLICATE_ESSAY')),
    evidence JSONB NOT NULL, status VARCHAR(12) NOT NULL CHECK (status IN ('OPEN','CONFIRMED','DISMISSED')),
    reviewed_by UUID, note TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, reviewed_at TIMESTAMPTZ
);
CREATE TABLE work_items (
    id UUID PRIMARY KEY,
    queue VARCHAR(20) NOT NULL CHECK (queue IN ('EXAMINER','CONTENT_AUTHOR')),
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('MID_COURSE_REMEDIATION','POST_COURSE_REMEDIATION','LEARNER_FLAG',
        'CONTENT_GAP','QUESTION_TOO_EASY','QUESTION_TOO_HARD','EDGE_SUSPECT')),
    subject_user_id UUID, subject_ref UUID, status VARCHAR(12) NOT NULL CHECK (status IN ('OPEN','IN_PROGRESS','DONE')),
    assignee_id UUID, action TEXT, result TEXT, note TEXT, note_visible_to_learner BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (kind, subject_ref)
);
```

Luật cờ (domain service `AnomalyRules`, ngưỡng đặt trong cấu hình `learning.flags.*`):
- `TOO_FAST_HIGH_SCORE`: trong 1 ngày, ít nhất 10 câu có `duration_ms`, trung vị dưới 3 giây, và tỷ lệ đúng ≥ 90%;
- `BURST_SUBMISSIONS`: hơn 30 lần nộp (event) trong 10 phút;
- `DUPLICATE_ESSAY`: essay chuẩn hóa (bỏ khoảng trắng, chữ thường) có SHA-256 trùng với essay của học viên khác. Thêm cột
  `essay_hash` cho bảng essay của learning.

Kiểm tra cờ chạy ngay sau mỗi lần ghi event, và chỉ xét học viên vừa ghi.

### Learning: hàng việc

Nguồn sinh việc (idempotent theo `UNIQUE (kind, subject_ref)`):

| Việc | Hàng | Nguồn |
|---|---|---|
| Học viên cần bổ trợ giữa khóa / sau khóa | EXAMINER | `remediation_flags` (phase 9) |
| Cờ bất thường mới | EXAMINER | `learner_flags` |
| KP thiếu bộ câu | CONTENT_AUTHOR | `content_gap_signals` (phase 7) |
| Cạnh tiên quyết đáng ngờ | CONTENT_AUTHOR | Job hằng ngày đọc `diagnostic_probes` (bên dưới) |
| Câu quá dễ / quá khó | CONTENT_AUTHOR | Job hằng ngày đọc event log: câu có ≥ 20 lần nộp đầu, tỷ lệ đúng > 0.9 hoặc < 0.3, hoặc lệch so với `difficulty` |

**Đánh giá chất lượng cây từ dữ liệu học viên** (mentor: "đánh giá cả Knowledge Tree có tốt hay không"):
- Với mỗi cạnh A → B, job tính từ các phiên chẩn đoán (phase 7) đã kết luận: số phiên có B trượt và A được thăm dò,
  tỷ lệ A thăm dò đạt, tỷ lệ A được kết luận là KP gốc.
- Cạnh có ít nhất 20 phiên và tỷ lệ A đạt ≥ 0.80 (nền vững mà vẫn trượt B, nên cạnh không giải thích được lỗi) hoặc tỷ lệ
  được kết luận là KP gốc < 0.10 → việc `EDGE_SUSPECT` cho CONTENT_AUTHOR kèm số liệu. Ngưỡng ở `learning.tree-quality.*`.
- `GET /api/learning/admin/tree-quality?courseId=`: bảng thống kê theo cạnh và theo KP (số lần là KP gốc, tỷ lệ trượt,
  mức mastery trung bình) để R&D chỉnh cây qua API của phase 1. Learning chỉ giữ id; tên KP lấy từ content.

API:
- `GET /api/learning/admin/work-items?queue=&status=` (EXAMINER chỉ thấy hàng EXAMINER; CONTENT_AUTHOR chỉ thấy hàng
  của mình; ADMIN thấy cả hai).
- `PATCH /api/learning/admin/work-items/{id}`: nhận việc, ghi action / result / note, đổi trạng thái.
- `PATCH /api/learning/admin/flags/{id}`: xác nhận hoặc bỏ cờ, kèm ghi chú.
- `GET /api/learning/admin/question-stats?minAttempts=20`: thống kê câu cho R&D (tỷ lệ đúng, trung vị thời gian, mã lỗi
  hay gặp, difficulty đã gắn).
- Học viên: `GET /api/learning/notes` trả các ghi chú có `note_visible_to_learner = true` (đồng bộ ngược về LMS).

### Assessment: R&D đánh giá Mentor (luồng độc lập)

- Migration `V11__examiner_qa.sql`, bảng `examiner_qa_reviews`: id, submission_id, examiner_id, examiner_band,
  reviewer_id, reviewer_band, reviewer_criteria jsonb, note, created_at.
- Khi giám khảo `finalize` một essay, chọn ngẫu nhiên theo tỷ lệ `assessment.qa.sample-rate` (mặc định 10%) và tạo job
  `HUMAN` với `reason = QA_SAMPLE`. Job này **không** ảnh hưởng kết quả của học viên.
- API cho `CONTENT_AUTHOR`, `ADMIN`:
  - `GET /api/assessments/qa-reviews/queue`;
  - `POST /api/assessments/qa-reviews`: chấm lại mù, không thấy điểm của giám khảo trước khi nộp;
  - `GET /api/assessments/admin/examiner-quality`: theo từng EXAMINER: số bài được chấm lại, độ lệch trung bình,
    tỷ lệ lệch > 1 band.
- Cảnh báo: việc `HUMAN` chờ quá 48 giờ → hiện cờ `overdue` trong hàng chờ của phase 10.

### Access và assessment: gói có Mentor

- Access thêm API nội bộ `POST /internal/access/human-grading/consume`, nhận bearer của học viên, idempotent theo
  `submissionId`. Gọi tới use case `ConsumeHumanGradingCreditUseCase` đã có.
- Assessment: học viên còn tín dụng thì được yêu cầu người chấm essay của topic test, qua
  `POST /api/assessments/attempts/{id}/items/{itemId}/human-grading`. Trừ tín dụng xong thì `enqueueHuman` với
  `reason = HUMAN_CREDIT`. Kết quả của giám khảo thay kết quả LLM qua luồng chấm lại đang có (result version mới).

## Related Code Files

- learning: migration V17, `domain/service/AnomalyRules.java`, `domain/aggregate/{LearnerFlag, WorkItem}.java`,
  `application/service/{FlagDetector, WorkItemSync}.java`, `infrastructure/scheduler/QuestionStatsJob.java`, use case
  và controller admin, `api/controller/LearnerNotesController.java`
- assessment: migration V11, use case QA, `ExaminerQualityController`, nút yêu cầu người chấm, client gọi access
- access: `api/controller/InternalAccessController.java` (mở rộng `/internal/access`), use case tiêu tín dụng đã có
- Gateway: kiểm route `/api/learning/admin/**`, `/api/assessments/qa-reviews/**`, `/api/assessments/admin/**` đã đi qua
  route có sẵn và có internal JWT
- Test:
  - `AnomalyRulesTest`: biên của từng luật;
  - `WorkItemSyncIntegrationTest`: idempotent; mỗi nguồn sinh đúng 1 việc; phân quyền theo hàng;
  - `ExaminerQaIntegrationTest`: lấy mẫu với tỷ lệ 100% trong test; chấm mù; thống kê đúng;
  - `HumanGradingCreditIntegrationTest`: hết tín dụng → 402 hoặc 409 theo contract access; không trừ hai lần.

## Implementation Steps

1. **Đỏ trước:** `AnomalyRulesTest`, test idempotent của hàng việc.
2. Learning: cờ, hàng việc, thống kê câu, hồ sơ 360°, ghi chú cho học viên.
3. Assessment: QA Mentor, chất lượng giám khảo, cảnh báo quá hạn.
4. Access và assessment: tín dụng chấm bởi người.
5. Contract `docs/contracts/admin-area-v1.md`.
6. `mvn -B -ntp verify`.

## Success Criteria

- [ ] EXAMINER mở một trang thấy: bài chờ chấm, học viên cần bổ trợ, cờ bất thường. Mỗi việc ghi được Action / Result /
      Note.
- [ ] CONTENT_AUTHOR thấy: KP thiếu bộ câu, câu quá dễ / quá khó, bài cần chấm lại để đánh giá Mentor.
- [ ] Admin xem được chất lượng từng EXAMINER.

## Risk Assessment

- Luật cờ có thể báo nhầm. Cờ chỉ là gợi ý cho người xem, không tự khóa tài khoản. False-positive của cờ = số cờ
  `DISMISSED` / tổng số cờ đã xét, hiện trong thống kê (bổ sung cho phase 11).
- Job thống kê quét event log hằng ngày. Có index theo câu; giới hạn cửa sổ 90 ngày.

## Bàn giao FE

- Khu quản trị theo role:
  - hồ sơ học viên 360°;
  - hàng việc của EXAMINER và CONTENT_AUTHOR;
  - xét cờ;
  - chất lượng Mentor;
  - thống kê câu.
- Phía học viên: ghi chú từ Mentor, nút yêu cầu người chấm.
