---
title: "Sinh câu hỏi và course bằng AI, dựng course từ cây tri thức, import Excel"
description: "Đã gộp vào plan 261023-0050-content-author-workspace-completion (phase 07–13)"
status: superseded
supersededBy: "261023-0050-content-author-workspace-completion"
created: "2026-10-16T08:04:00.000Z"
createdBy: "ck:plan"
---

# Đã gộp

Ngày 2026-10-23 người dùng chốt gộp plan này vào
[`261023-0050-content-author-workspace-completion`](../261023-0050-content-author-workspace-completion/plan.md), để cả khu
soạn nội dung (soạn tay và AI/Excel) nằm trong một plan.

| Phase cũ ở đây | Ở plan gộp |
|---|---|
| 1. API soạn lesson và `import_ref` | API lesson đã có (`docs/contracts/lesson-authoring-v1.md`); `import_ref` là phase 07 |
| 2. Hạ tầng bản nháp, job nền, LLM | phase 08 |
| 3. AI sinh câu hỏi | phase 09 |
| 4. Dựng course từ cây tri thức | phase 10 |
| 5. AI sinh course từ yêu cầu | phase 11 |
| 6. Import Excel | phase 12 |
| 7. Contract, tài liệu, nghiệm thu | mỗi phase tự viết contract; tài liệu và kịch bản demo ở mục "Việc cuối" và "Kiểm chứng tổng" của plan gộp |
| "Bàn giao FE" ở từng phase | phase 13 (Claude Code) |

Các quyết định D1–D14 của plan này được giữ ở plan gộp, mục "Quyết định bổ sung khi gộp", trừ chỗ đã đổi: chỉ
`CONTENT_AUTHOR` (bỏ `ADMIN`), số migration lấy theo thứ tự merge thay vì V34/V35, và đề cuối `TOPIC_TEST`/`COURSE_TEST`
được tạo qua công cụ (trước đây chỉ chọn gói có sẵn). Nội dung chi tiết cũ của các phase còn trong lịch sử git.
