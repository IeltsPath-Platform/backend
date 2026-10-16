---
title: "Sinh câu hỏi và course bằng AI, dựng course từ cây tri thức, import Excel"
description: "Công cụ cho R&D tạo nội dung: API soạn lesson, bản nháp do AI sinh (câu hỏi, course) có duyệt, dựng course bám cây KP, import Excel theo template"
status: pending
priority: P2
branch: "feat/ai-content-course-builder"
tags: [content-service, ai, course-builder, excel-import, knowledge-tree, llm]
blockedBy: ["261016-0131-knowledge-tree-assessment-journey"]
blocks: []
created: "2026-10-16T08:04:00.000Z"
createdBy: "ck:plan"
source: "biên bản góp ý mentor (file ghi âm 3 và 4) + board FigJam 'cây tri thức'"
---

# Sinh câu hỏi và course bằng AI, dựng course từ cây tri thức, import Excel

## Overview

Mentor nói quy trình tạo course cũ là "có đề rồi mới tạo course và import đề vào". Quy trình mới phải đảo lại:
dựng **cây tri thức trước**, course **bám theo cây**, bài tập của từng buổi lấy từ kho đề, và có hai cách làm:
- nhập bằng **Excel theo template** (có nút tải template ngay chỗ Import);
- để **LLM đọc kho đề và đề xuất**, sau đó **người duyệt**.

Về sau, mentor muốn nhập "6.5, luyện chủ đề A, B, C" thì AI sinh ra một course phù hợp. Mentor gọi đây là "cái khó thứ hai"
của nhóm. Plan này làm phần đó. Nó là plan thứ hai, bổ sung cho
[plan cây tri thức](../261016-0131-knowledge-tree-assessment-journey/plan.md), không thay thế.

```text
R&D nhập yêu cầu ──> job nền (queue) ──> LLM (qua port) ──> BẢN NHÁP (ai_drafts)
   "6.5, chủ đề A, B, C"                  chọn từ kho đề            │ R&D xem, sửa
                                          theo cây KP               ▼
Excel (template) ──> kiểm tra (dry-run) ──────────────────────> DUYỆT (CONTENT_AUTHOR)
                                                                    │
                                    course INACTIVE / lesson DRAFT / câu hỏi DRAFT ◄┘
                                                                    │ xuất bản như hiện nay
                                                                    ▼
                                                               học viên thấy
```

## Quyết định đã chốt

Bốn quyết định đầu do người dùng chốt ngày 2026-10-16. Các quyết định còn lại theo khuyến nghị.

- **D1. Đặt ở content-service, không thêm service mới.** Content đang sở hữu câu hỏi, gói, topic, KP, course. LLM nằm sau
  port (`ContentGenerationPort`) để sau này tách `ai-service` chỉ cần đổi adapter (xem plan 261016-0131, mục ai-service).
- **D2. Excel gồm cả cấu trúc lẫn câu hỏi.** Một file tạo được course, topic, lesson, block, câu hỏi, gói practice set
  và mốc practice test. Template phức tạp hơn mức chỉ tham chiếu; bù lại có bước kiểm tra trước khi ghi (phase 6).
- **D3. Sinh course chỉ chọn từ kho, không bịa.** Chỗ nào thiếu câu cho một KP thì liệt kê thiếu, không tự sinh câu mới
  trong luồng sinh course. R&D muốn bù thì dùng chức năng sinh câu hỏi riêng (phase 3).
- **D4. Một bước duyệt:** `CONTENT_AUTHOR` (hoặc `ADMIN`) duyệt bản nháp.
- **D5. Bản nháp không bao giờ tới học viên.** AI chỉ ghi vào bảng nháp. Duyệt xong mới tạo thực thể thật ở trạng thái
  nháp (`DRAFT` / `INACTIVE`), vẫn phải qua bước `PUBLISHED` hiện có. Khớp chính sách AI ở plan 261016-0131, phase 10.
- **D6. Việc chạy nền, không chạy trong request.** API trả `202` kèm id. Một scheduler lấy job, gọi LLM ngoài transaction,
  ghi kết quả. Theo mẫu `GradeGateEssayJobUseCase` của assessment (claim job, quota, lỗi thì đánh dấu thất bại).
- **D7. Hạn mức LLM theo ngày bắt buộc** (AGENTS.md §3.8), bộ đếm riêng của content, tính theo người yêu cầu.
- **D8. Không thêm role.** Dùng `CONTENT_AUTHOR` và `ADMIN` có sẵn.
- **D9. Test không gọi LLM thật** (AGENTS.md §5): dùng `ContentGenerationPort` giả hoặc `MockRestServiceServer`.
- **D10. Không log nội dung do AI sinh, prompt hay khóa API;** log chỉ mang id, loại việc, mã lỗi.
- **D11. Repo public:** AI phải tự viết nội dung; prompt hướng dẫn không chép đề thương mại. Người duyệt chịu trách nhiệm
  kiểm tra khi bấm duyệt.

## Phát hiện từ code (ảnh hưởng thiết kế)

1. **Chưa có API tạo lesson.** Lesson, block, liên kết KP chỉ có qua seed SQL (`lessons`, `lesson_blocks`,
   `lesson_block_questions`, `lesson_knowledge_points`). Muốn duyệt bản nháp thành course thật thì phải có API soạn
   lesson trước (phase 1).
2. **Bảng `questions` không có cột mã.** Excel không thể tham chiếu câu hỏi đã có theo mã. Thêm cột `import_ref` (unique,
   nullable) để Excel và bản nháp trỏ tới câu hỏi (phase 1).
3. **Chưa có thư viện đọc Excel** trong repo. Đọc `.xlsx` cần thêm dependency (thường là Apache POI). AGENTS.md §2 yêu cầu
   duyệt trước khi thêm. Đây là điều kiện chặn của phase 6 (xem Open questions).

## Ngoài phạm vi

- Tách `ai-service` riêng (hướng mở rộng khi sinh hàng loạt hoặc cần scale độc lập).
- AI sinh bài viết lý thuyết dài, tạo audio Listening, sinh ảnh biểu đồ Writing Task 1.
- Chấm bài (đã ở plan 261016-0131).
- Độ khó thích ứng; tự động xuất bản không qua người duyệt.
- Frontend: repo riêng. Mỗi phase có mục "Bàn giao FE".

## Phases

| Phase | Name | Làm gì | Status |
|-------|------|--------|--------|
| 1 | [API soạn lesson và mã tham chiếu câu hỏi](./phase-01-lesson-authoring-api.md) | CRUD lesson, block, liên kết KP; cột `import_ref` cho câu hỏi | Pending |
| 2 | [Hạ tầng bản nháp, job nền và LLM](./phase-02-draft-infrastructure.md) | Bảng nháp, hàng đợi, quota, port và adapter LLM, cấu hình | Pending |
| 3 | [AI sinh câu hỏi](./phase-03-ai-question-drafts.md) | Sinh nháp câu hỏi theo KP, kiểm tra, duyệt thành câu hỏi `DRAFT` | Pending |
| 4 | [Dựng course từ cây tri thức](./phase-04-course-builder-from-tree.md) | Bản nháp course có cấu trúc, kiểm tra phủ KP, duyệt thành course thật | Pending |
| 5 | [AI sinh course từ yêu cầu](./phase-05-ai-course-drafts.md) | "6.5, chủ đề A, B, C" → nháp course chọn từ kho, liệt kê KP thiếu câu | Pending |
| 6 | [Import Excel theo template](./phase-06-excel-import.md) | Template tải về, đọc file, kiểm tra trước, ghi thành nháp hoặc course | Pending |
| 7 | [Contract, tài liệu và nghiệm thu](./phase-07-contracts-docs-and-verification.md) | Contract, AGENTS.md, DATABASE_V5, kịch bản demo, verify | Pending |

## Thứ tự và phụ thuộc

```text
plan 261016-0131: phase 1 (cây), 2 (seed + difficulty), 3 (mã lỗi)  ──┐
                                                                       ▼
1 ──> 4 ──> 5            2 ──> 3 ──> 5 (5 dùng hạ tầng 2; thiếu câu thì chuyển sang 3)
1 ──> 6 (Excel ghi lesson, câu hỏi, gói; dùng analyzer của phase 4)   4 ──> 6
```

Làm song song được: phase 1 và phase 2. Phase 3, 4 chạy song song sau đó. Phase 5 cần 2, 3, 4. Phase 6 cần 1, 4 và chờ
duyệt dependency Excel; không cần hạ tầng LLM của phase 2.

Migration content: V34 (phase 1, `import_ref`), V35 (phase 2, bản nháp và hạn mức). Các phase còn lại dự kiến không cần
migration, trừ khi phase 4 phải đổi ràng buộc gói. Plan 261016-0131 dùng tới V33. Đánh lại theo thứ tự merge thực tế, không
sửa migration đã merge.

## Acceptance tổng

- R&D tạo, sửa, sắp xếp lesson, block và liên kết KP qua API (không cần SQL).
- Yêu cầu "sinh 10 câu cho KP X, band 6.0, độ khó MEDIUM":
  - trả `202` kèm id;
  - bản nháp có đủ câu, mỗi câu hợp lệ theo `answer_spec`, có difficulty và mã lỗi lấy từ danh mục;
  - duyệt xong thì tạo câu hỏi ở trạng thái `DRAFT`.
- Yêu cầu "course 6.5, chủ đề A, B, C":
  - sinh bản nháp course gồm topic và lesson theo thứ tự cây, chỉ dùng câu và gói đã có trong kho;
  - có danh sách KP thiếu câu;
  - duyệt xong tạo course `INACTIVE` cùng topic, lesson (`DRAFT`), gói đã liên kết.
- Excel đúng template: có báo cáo kiểm tra từng dòng trước khi ghi; sai thì không ghi gì (tất cả hoặc không);
  đúng thì tạo đúng cấu trúc và câu hỏi; tải được template ngay chỗ Import.
- Hạn mức ngày: vượt hạn mức thì `429 AI_QUOTA_EXCEEDED`, không có việc nào bị mất.
- Không có nội dung AI nào tới học viên khi chưa duyệt và xuất bản.
- Kiểm chứng: `mvn -B -ntp verify` cả reactor xanh, Testcontainers chạy, không test nào gọi LLM thật.

## Dependencies

- Plan 261016-0131, các phase 1 (cây), 2 (difficulty, KP) và 3 (danh mục mã lỗi).
- **Duyệt dependency đọc Excel** trước phase 6.
- Biến môi trường mới cho LLM của content (phase 2); không ghi giá trị thật vào repo.
- FE (repo `frontend`): màn tạo yêu cầu sinh, xem và sửa bản nháp, duyệt, màn import Excel kèm tải template.

## Open questions

1. **Dependency Excel:** dùng Apache POI (chuẩn, nặng) hay thư viện nhẹ hơn, hay chốt định dạng CSV thay Excel? Plan
   mặc định POI; chưa được duyệt nên phase 6 chưa bắt đầu được.
2. **Nhà cung cấp LLM của content:** dùng chung endpoint OpenAI-compatible như assessment (biến `CONTENT_LLM_*` mới) hay
   cấu hình riêng?
3. **Hạn mức mặc định:** số việc sinh mỗi người mỗi ngày (đề xuất 20 việc) và số câu tối đa mỗi việc (đề xuất 20).
