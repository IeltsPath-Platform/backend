---
phase: 4
title: "Learning: backfill và rebuild mastery cho essay cũ"
status: pending
priority: P2
effort: "1d"
dependencies: [3]
---

# Phase 4: Learning: backfill và rebuild mastery cho essay cũ

## Overview

Các essay đã chấm trước nâng cấp chỉ có bằng chứng "đạt/không đạt" cho KP của đề. Backfill dựng thêm bằng chứng theo tiêu chí từ điểm đã lưu trong bài nộp, giữ nguyên thời điểm bằng chứng gốc, rồi rebuild mastery của các KP bị ảnh hưởng.

## Requirements

- Idempotent: chạy lại không nhân đôi bằng chứng, không đổi kết quả.
- Chỉ bổ sung cho essay mà trước đây ĐÃ có bằng chứng (essay lesson trước khi block đạt; essay Practice của bộ được tính `counted`). Không tạo bằng chứng mới cho essay mà trước đây không được tính.
- Giữ thời điểm của bằng chứng gốc để hệ số giảm theo tuổi (half-life 30 ngày) và dedupe 7 ngày đúng.
- Có thể tắt bằng cờ cấu hình; không log nội dung bài viết, chỉ đếm và id.
- Tái dùng cơ chế sẵn có: `MasteryProfileMaintenance` (có `backfill` và rebuild theo học viên) và `MasteryProfileBackfillRunner` (cờ `learning.mastery.backfill-on-startup`).

## Architecture

```
WritingCriterionBackfill (application/service)
  với mỗi học viên có bài chấm GRADED lưu điểm tiêu chí chưa có KP:
    1. tra ánh xạ tiêu chí -> KP (rubric active theo task, gọi Content ngoài transaction, cache theo task)
    2. dùng WritingEvidenceRule dựng outcomes
    3. với KP có bằng chứng gốc: giữ nếu KP là KP của đề ngoài ánh xạ; cập nhật `correct` nếu KP của đề trùng KP tiêu chí;
       với KP tiêu chí mới: chèn dòng mới cùng `created_at` của bằng chứng gốc
    4. gom KP đã chạm -> MasteryProfileUpdater.rebuild(user, kps)
```

## Related Code Files

- Create: `services/learning-service/.../application/service/WritingCriterionBackfill.java`
- Modify: `KnowledgeEvidenceRepository` (+ `JdbcKnowledgeEvidenceRepository`): thao tác chèn có thời điểm tường minh và cập nhật `correct` theo `(source, reference, kp)`, chỉ dùng cho backfill
- Modify: `MasteryProfileMaintenance` / `MasteryProfileBackfillRunner` hoặc runner mới trong cùng package: gọi backfill, cờ `learning.mastery.writing-criteria-backfill-on-startup` (câu hỏi mở 3: mặc định bật hay tắt)
- Modify: `infra/config-server/config-repo/learning-service.yaml`: cờ và mô tả
- Có thể cần: câu truy vấn liệt kê bài nộp GRADED cần backfill (trong `JdbcWritingSubmissionRepository`)

## Implementation Steps

1. Đọc `kp_evidence` (schema, khóa unique, cột thời điểm, `JdbcKnowledgeEvidenceRepository.append`) và `MasteryProfileMaintenance.backfill/start` để biết chỗ gắn và cách chạy nền theo học viên, lỗi một học viên không dừng cả đợt.
2. Xác định cách nhận biết "bằng chứng gốc đã tồn tại": essay lesson qua `forWriting(submissionId, kpId)` của KP đề; essay Practice qua `forPracticeSet(requestId, questionVersionId, kpId)` của lần nộp bộ được tính.
3. Viết repository bổ sung (chèn có thời điểm, cập nhật `correct`), có `ON CONFLICT` theo khóa hiện hữu.
4. Viết `WritingCriterionBackfill`: tải ánh xạ theo task qua `LearningContentClient` (ngoài transaction), dùng `WritingEvidenceRule`, ghi trong transaction theo từng học viên (bắt đầu bằng khóa advisory theo user như mọi lượt ghi của học viên), rồi gọi `rebuild` cho KP đã chạm.
5. Gắn vào runner khởi động với cờ; log tổng: số học viên, số bài, số bằng chứng chèn, số cập nhật, số lỗi bỏ qua.
6. Thử trên dữ liệu seed: bài cũ chưa có KP -> sau backfill có bằng chứng tiêu chí; chạy lần hai không đổi gì.

## Success Criteria

- [ ] Sau backfill, học viên có essay cũ có bằng chứng theo tiêu chí ở đúng KP, `created_at` bằng bằng chứng gốc.
- [ ] Chạy lại backfill: số dòng `kp_evidence` không đổi.
- [ ] KP của đề trùng KP tiêu chí được cập nhật `correct` thay vì thêm dòng; KP của đề ngoài ánh xạ giữ nguyên.
- [ ] Essay chưa từng có bằng chứng không bị tạo bằng chứng.
- [ ] Mastery các KP bị ảnh hưởng được rebuild; lỗi một học viên không dừng backfill.
- [ ] Cờ tắt thì không chạy.

## Risk Assessment

- Phase rủi ro nhất: ghi hàng loạt dữ liệu học viên và đổi mức mastery của họ. Thử trên bản sao DB trước; giữ cờ tắt cho tới khi kiểm tra số liệu.
- Sai thời điểm bằng chứng làm lệch hệ số giảm theo tuổi; test kiểm tra `created_at` rõ ràng.
- Gọi Content cho ánh xạ trong lúc chạy nền: cache theo task, lỗi Content thì bỏ qua đợt đó và log, không làm hỏng khởi động.
- Rollback: backfill chỉ thêm dòng (và cập nhật `correct` của KP trùng). Lưu danh sách `(source, reference, kp)` đã chèn trong log số liệu; nếu cần gỡ, xóa theo `source`/`reference` của các dòng mới, rồi `rebuild`.
