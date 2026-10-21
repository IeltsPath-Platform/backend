---
phase: 4
title: "Gộp chấm band và đánh giá KP con trong một request AI"
status: in_progress
dependencies: [2, 3]
---

# Gộp phản hồi AI

Theo yêu cầu người dùng ngày 2026-10-22, thay quyết định dùng lượt gọi KP riêng trong Phase 2 bằng một request AI cho mỗi lần chấm. Phase 1–3 là lịch sử triển khai; tài liệu này thay thế các chỉ dẫn lượt gọi thứ hai của chúng.

## Thực hiện

- Giữ nguyên template chấm band `v1` và activation/rubric được pin. Nối hướng dẫn KP con `kpj2` vào cùng system prompt; phản hồi JSON chứa cả `criteria`, `corrections`, `summary`, `focus` và `judgments`.
- `EssayGrader` gọi LLM đúng một lần; không sửa JSON bằng lượt AI khác. Client không tự lặp request khi gặp 429/503.
- `EssayKnowledgeJudge` chỉ đọc mảng JSON đã nhận và áp dụng luật kiểm chứng hiện có; không có dependency LLM, clock hoặc timeout riêng.
- Thiếu/sai mảng judgments không làm mất band hợp lệ; dùng NOT_ASSESSED và vẫn đối chiếu correction đã kiểm tra. Bài không có KP con không cần mảng judgments.
- Giữ nguyên hợp đồng API/UI, lưu JSON cũ, evidence và đích luyện tập. `promptVersion` ghi phiên bản band; `judgmentVersion=kpj2` ghi hợp đồng KP cùng request, phân biệt với `kpj1` cũ.
- Bỏ quick client, executor, prompt KP cũ, cấu hình judgment timeout/skip và test chỉ dành cho lượt gọi thứ hai.

## Phạm vi tệp

Learning: `EssayGrader`, `EssayKnowledgeJudge`, `LlmClient`, `WritingSettings`, `OpenAiCompatibleLlmClient`, prompt resource KP và test tương ứng; test tích hợp lesson/Practice; Config Server `learning-service.yaml`.
Tài liệu: `AGENTS.md`, `docs/ai-policy.md`, `docs/contracts/lesson-writing-v1.md`, workspace `docs/writing-flow-presentation.md`, kế hoạch và báo cáo.

## Nghiệm thu

- [ ] Chấm có/không có KP con gửi đúng một request, prompt chứa mã/tên/mô tả KP, không chứa UUID hoặc essay trong system prompt.
- [ ] PASS/FAIL hợp lệ, dẫn chứng bịa, judgments thiếu/sai và correction đối chiếu giữ đúng luật.
- [ ] JSON/band sai hoặc lỗi provider không gọi lại, trả mã lỗi an toàn và không thu điểm.
- [ ] Test lesson/Practice dùng JSON gộp và kiểm tra ghi evidence, đọc lại, nộp lặp.
- [ ] Test hẹp và toàn Learning chạy; lỗi nền và test Docker chưa chạy được báo riêng.
- [ ] Tài liệu, cấu hình và kế hoạch khớp một request.

## Giới hạn và hoàn tác

Chưa đo độ chính xác trên gold set hoặc độ trễ provider thực. Đổi lại cơ chế hai request cần khôi phục code/config/prompt tương ứng; không cần migration dữ liệu và bài cũ vẫn đọc được.
