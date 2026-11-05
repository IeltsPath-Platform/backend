---
phase: 4
title: "Tài liệu hợp đồng và wireframe"
status: in-progress
priority: P2
effort: "0.5d"
dependencies: [1, 2, 3]
---

# Phase 4: tài liệu và wireframe

- `docs/contracts/human-review-v1.md`: bỏ câu "never includes the examiner's identity" (học viên thấy examiner), thêm `examinerId`, chọn và đổi examiner, hàng đợi riêng, giới hạn nhận bài.
- Hợp đồng nội bộ user-service cho `GET /internal/users/examiners/{id}`.
- Wireframe `docs/wireframes/IELTSPath_Prototype.drawio` (chỉ thêm hoặc sửa các page P-6x):
  - **P-60**: hàng đợi riêng của examiner, bỏ câu "Mọi Examiner đều có thể chấm mọi bài", bỏ cột và ô tìm học viên, bỏ tab hàng chung, bỏ lọc "nguồn".
  - **P-64**: bước "Chọn examiner" (danh sách tên, ảnh, band, chứng chỉ, nút chọn, trạng thái "đã đủ bài"), trạng thái "Quá hạn, chọn giáo viên khác", "Chờ chọn giáo viên".
  - **P-65**: hiện tên, ảnh, profile của examiner đã chấm.
