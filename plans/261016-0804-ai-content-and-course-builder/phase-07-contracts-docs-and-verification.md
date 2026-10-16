---
phase: 7
title: "Contract, tài liệu và nghiệm thu"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5, 6]
---

# Phase 7: Contract, tài liệu và nghiệm thu

## Overview

Rà lại toàn bộ thay đổi của plan, cập nhật tài liệu cho khớp code, chạy nghiệm thu đầu-cuối.

## Requirements

- **Contract** trong `docs/contracts/` (thư mục được khôi phục ở plan 261016-0131, phase 1):
  - mới: `lesson-authoring-v1.md`, `ai-drafts-v1.md` (bản nháp, hàng đợi, trạng thái, lỗi `429`), `course-drafts-v1.md`
    (cấu trúc, phân tích, duyệt), `course-import-v1.md` (template, báo cáo kiểm tra, mã lỗi);
  - sửa: `learning-content-internal-v1.md` nếu có thay đổi (kỳ vọng không có).
  Mỗi contract khớp mapping trong controller.
- **AGENTS.md:** thêm vào bảng module trách nhiệm mới của content (soạn lesson, bản nháp AI, dựng course, import Excel);
  mục cấu hình (nhóm `content.ai.*`, biến `CONTENT_LLM_*`); ghi chú: nội dung AI luôn qua bản nháp và duyệt.
- **`.sdd/database/DATABASE_V5.md`:** thêm `ai_drafts`, `ai_usage_daily`, cột `questions.import_ref`.
- **`docs/ai-policy.md`** (của plan 261016-0131, phase 10): bổ sung mục "AI sinh nội dung": chỉ ra bản nháp, người duyệt,
  không tới học viên khi chưa duyệt, chỉ chọn từ kho khi sinh course.
- **Kịch bản demo** `docs/demo/ai-content-demo.md`, từng bước gọi API:
  1. R&D soạn một lesson bằng API.
  2. Yêu cầu sinh 5 câu cho một KP; sửa một câu; duyệt; câu hỏi `DRAFT` xuất hiện.
  3. Yêu cầu course "6.5, chủ đề Paraphrase và TFNG"; xem bản nháp, `gaps`, `unmatchedTopics`; duyệt thành course
     `INACTIVE`.
  4. Tải template, điền, kiểm tra (một dòng lỗi), sửa, import thành công.
  5. Kích hoạt course bằng API course hiện có; học viên thấy course trong danh sách.
- **Bảng bàn giao FE** gom từ các phase vào `reports/fe-handoff.md` trong thư mục plan.

## Implementation Steps

1. Rà contract với controller (grep mapping), sửa chỗ lệch.
2. Cập nhật AGENTS.md, DATABASE_V5.md, `docs/ai-policy.md`.
3. `docker compose up -d --build`, đi hết kịch bản demo (với cổng LLM giả hoặc cấu hình thật do bạn cung cấp), ghi kết quả
   vào `reports/acceptance.md`.
4. `mvn -B -ntp verify` cả reactor; CI xanh.
5. Đánh dấu các phase `completed` trong `plan.md`.

## Success Criteria

- [ ] Không còn contract lệch với controller.
- [ ] Kịch bản demo chạy hết trên Compose sạch (`docker compose down -v` rồi `up`).
- [ ] `mvn -B -ntp verify` xanh, Testcontainers không bị skip.

## Risk Assessment

- Demo cần endpoint LLM thật hoặc giả. Nếu không có khóa, chạy bằng adapter giả đã dùng trong test và ghi rõ trong báo cáo
  nghiệm thu (không tuyên bố đã chạy LLM thật khi chưa chạy).
