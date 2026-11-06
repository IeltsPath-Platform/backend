---
phase: 7
title: "Chế độ bóng, hiệu chỉnh và phát hành"
status: pending
priority: P2
effort: "1.5d + thời gian thu dữ liệu"
dependencies: [5, 6, 8]
---

# Phase 7: Chế độ bóng, hiệu chỉnh và phát hành

## Overview

Trước khi học viên thấy điểm AI, so sánh AI với giám khảo trên bài thật, hiệu chỉnh bảng ánh xạ phát âm, rồi bật dần. Đây là phase quyết định có công bố hay không.

## Requirements

- Functional: chạy AI trên một tập bài đã có điểm giám khảo mà học viên chưa thấy kết quả AI; xuất cặp AI và giám khảo; hiệu chỉnh `PronunciationBandMapper`; cờ bật tắt tính năng.
- Non-functional: ngưỡng công bố dùng đúng ngưỡng cổng `ai-eval` của assessment-service (độ lệch tối đa nửa band ở tỷ lệ từ 0.80, QWK từ 0.60); có số liệu theo dõi sau khi bật.

## Architecture

- **Cờ:** `learning.speaking.enabled` (mặc định tắt: endpoint trả 503, scheduler không nhận bài) và `learning.speaking.visibility` = `SHADOW` hoặc `LEARNER`. Ở `SHADOW` kết quả AI được lưu nhưng không trả cho học viên và **không trừ điểm**; chỉ tài khoản nội bộ hoặc danh sách cho phép được nộp.
- **Tập đối chiếu:** bản ghi có điểm giám khảo theo FC, LR, GRA, PR (từ phase 1 mở rộng, và bài giám khảo của assessment-service nếu có đủ đồng ý). Endpoint quản trị xuất cặp `(AI, giám khảo)` theo bài và tiêu chí.
- **Tính ngưỡng:** kiểm tra khả năng dùng lại `AiEvaluationService` của assessment-service cho cặp điểm; nếu không dùng lại được thì tính độ lệch nửa band và QWK bằng một tiện ích có test.
- **Hiệu chỉnh phát âm:** dựng lại ngưỡng từ cặp điểm, tăng `pronunciationMapVersion`, ghi phiên bản vào mỗi kết quả.
- **Theo dõi:** số bài theo trạng thái, tỷ lệ lỗi, p95 thời gian từ nộp đến xong, chi phí ước tính theo bài, số lần `PAYMENT_PENDING`.

## Related Code Files

- Modify: `infra/config-server/config-repo/learning-service.yaml` (cờ, danh sách cho phép, hạn mức ngày giảm lúc đầu)
- Create: endpoint quản trị xuất cặp điểm trong learning-service (`api/controller/` admin, theo quyền sẵn có của khu quản trị) và use case tương ứng
- Create: tiện ích tính độ lệch nửa band và QWK (nếu không dùng lại được) và test
- Modify: `PronunciationBandMapper` cấu hình (bảng mới, phiên bản mới)
- Modify: `AGENTS.md` (bảng module: learning-service có luồng chấm Speaking AI; bảng công nghệ đã cập nhật ở phase 4), `docs/system-architecture.md` (luồng, bucket, cờ), `docs/contracts/speaking-ai-v1.md`, `services/learning-service/README.md`
- Frontend: nhãn "điểm ước lượng" và văn bản đồng ý cuối cùng

## Implementation Steps

1. Bật `enabled` ở môi trường thử với `SHADOW` và danh sách cho phép.
2. Chạy tập đối chiếu qua luồng thật, xuất cặp điểm.
3. Tính độ lệch nửa band và QWK cho FC, LR, GRA, và quan hệ cho PR; so với ngưỡng.
4. Hiệu chỉnh bảng ánh xạ phát âm, chạy lại, tăng phiên bản.
5. Nếu đạt: chuyển `LEARNER` cho nhóm nhỏ, hạn mức ngày thấp, theo dõi một tuần, rồi mở rộng. Nếu không đạt: giữ `SHADOW`, ghi báo cáo và quay lại prompt hoặc rubric.
6. Cập nhật tài liệu, chốt văn bản đồng ý đã qua pháp lý.

## Success Criteria

- [ ] Có số liệu độ lệch nửa band và QWK trên tập đối chiếu đủ lớn (ghi rõ số bài) và so với ngưỡng 0.80 và 0.60.
- [ ] `SHADOW` thật sự không lộ kết quả AI và không trừ điểm (test).
- [ ] A/B lịch sử trên tập đối chiếu: bật hay tắt khối lịch sử không đổi band (D18); nếu có trôi thì tách thành hai lần gọi trước khi phát hành.
- [ ] Bảng ánh xạ phát âm có phiên bản, kết quả ghi phiên bản đó.
- [ ] Tắt `enabled` đưa hệ thống về trạng thái trước phát hành; dữ liệu đã có giữ nguyên.
- [ ] AGENTS.md, kiến trúc và hợp đồng đã cập nhật; nhãn "điểm ước lượng" hiển thị.

## Risk Assessment

- Tập đối chiếu nhỏ làm số liệu nhiễu: đặt số bài tối thiểu trước khi quyết định, báo rõ khoảng tin cậy rộng.
- Giám khảo chấm lệch nhau: nếu có hai giám khảo cho cùng bài thì ghi độ đồng thuận làm mốc trần cho AI.
- Chi phí tăng khi mở rộng: hạn mức ngày, cảnh báo ngân sách Azure và nhà cung cấp LLM.
- Rollback: tắt `enabled` hoặc về `SHADOW`.
