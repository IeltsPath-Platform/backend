---
phase: 8
title: Docs and full verification
status: completed
priority: P2
effort: 0.5d
dependencies:
  - 7
  - 9
---

# Phase 8: Docs and full verification

## Overview
Cập nhật tài liệu dự án theo hành vi mới và chạy kiểm tra toàn bộ.

## Requirements
- Functional: tài liệu mô tả KP con theo mã lỗi, nguồn bằng chứng `error_trap`, `ERROR_PATTERN`, chọn bộ theo bẫy, phản hồi
  `fixHint`, bẫy T/F/NG tự điền.
- Non-functional: không tuyên bố check chưa chạy.

## Related Code Files
- Modify: `AGENTS.md` §3.8 (bằng chứng, bài ôn, giỏ ôn), §3.6 (config `learning.error-trap.*`, trọng số mới)
- Modify: `docs/system-architecture.md`, `services/content-service/README.md`, `services/learning-service/README.md` nếu có mục liên quan
- Modify: `plans/reports/brainstorm-261024-0135-error-code-personalized-review-report.md` (ghi chỉnh P1–P4 của plan)

## Implementation Steps
1. Cập nhật tài liệu; đối chiếu từng contract đã sửa ở phase 3–6 với code.
2. Chạy `mvn -q -pl services/content-service -am package`, `mvn -q -pl services/learning-service -am package`,
   `docker compose config --quiet`, test + build frontend.
3. Kiểm thủ công trên Compose (nếu chạy được), dùng dữ liệu mẫu phase 9: làm vài câu T/F/NG theo kịch bản Lan và vài câu
   Reading có bẫy `KEYWORD_MATCH_ONLY` → thấy `error`, giỏ ôn có `ERROR_PATTERN`, bộ ôn đầu là bộ nhiều câu bẫy.
   Không chạy được thì ghi "chưa kiểm thủ công".
4. Ghi `reports/verification.md`: lệnh đã chạy, kết quả, test bị bỏ qua vì thiếu Docker.

## Success Criteria
- [ ] Hai lệnh package xanh; frontend xanh; compose config xanh.
- [ ] Tài liệu khớp code; báo cáo brainstorm ghi các chỉnh khi lập plan.

## Risk Assessment
- Test đỏ có sẵn từ trước (phase 1) không chặn nghiệm thu nếu không liên quan; ghi rõ trong báo cáo.
