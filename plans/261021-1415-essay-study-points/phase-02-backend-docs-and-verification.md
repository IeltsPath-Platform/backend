---
phase: 2
title: "Backend: tài liệu hợp đồng và kiểm tra build"
status: in-progress
priority: P1
effort: "0.25d"
dependencies: [1]
repo: backend
---

# Phase 2: Backend: tài liệu hợp đồng và kiểm tra build

Repo: `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\backend`.

## Tài liệu

1. Tìm mọi tài liệu mô tả response essay đã chấm: `rg -n "pointsCharged|corrections" docs/contracts`. Với mỗi file mô tả response
   của essay (chắc chắn có `docs/contracts/lesson-writing-v1.md`; kiểm tra thêm các file về luyện tập và essay revision/human
   review), thêm `studyPoints` vào ví dụ JSON và bảng trường:
   - Mảng `studyPoints` của essay `GRADED`, luôn có mặt (có thể rỗng); trường null bị bỏ.
   - Mục `CRITERION`: `criterionCode`, `band`, `knowledgePointId` luôn có; `code`, `name`, `title`, `reason` có thể thiếu khi Content không trả được chi tiết KP lúc chấm.
   - Mục `AI`: `kind`, `title`, `reason` (tùy chọn); tối đa 4 mục, tiêu đề tối đa 80 ký tự.
   - Quy tắc: tiêu chí `band < passBand` và có KP; thứ tự `CRITERION` (yếu nhất trước) rồi `AI`.
   - Câu cũ "Criterion knowledge points are not part of the response" (đã thêm ở lần trước trong `lesson-writing-v1.md`) phải sửa lại cho đúng:
     `criteria[]` không có KP; KP chỉ xuất hiện trong `studyPoints[]`.
2. `AGENTS.md` §3.8, đoạn "Bằng chứng essay..." đã có: thêm một câu: kết quả chấm trả thêm `studyPoints` (mục KP tiêu chí yếu và
   `focus` của LLM), tính một lần lúc chấm và lưu trong JSON bài chấm; bài cũ không có.
3. Không sửa `docs/system-architecture.md` trừ khi có đoạn mô tả đúng luồng này (đã kiểm tra lần trước: không có).
4. Kiểm tra lại: `rg -n "not part of the response" docs AGENTS.md` không còn câu mâu thuẫn.

## Kiểm tra build

```powershell
mvn -q -pl services/learning-service -am package
mvn -q -pl services/assessment-service -am package
```

`assessment-service` không dùng `WritingGrade` của Learning nhưng chạy lại để chắc không vỡ chung. Ghi vào báo cáo: số test chạy/lỗi/bỏ qua của
mỗi service; xác nhận lỗi (nếu có) chỉ là hai test sẵn có của learning-service.

## Tiêu chí hoàn thành

- [x] Mọi contract mô tả response essay có `studyPoints` đúng với hợp đồng trong `plan.md`.
- [x] Không còn câu nói KP "không có trong response" mà mâu thuẫn.
- [ ] Hai lệnh `package` chạy; kết quả được ghi trung thực (kể cả test bị bỏ qua).

## Đồng bộ trạng thái — 2026-10-21

Contract và AGENTS đã khớp `studyPoints`; không còn câu mâu thuẫn về KP trong response ở các đoạn liên quan. Báo cáo hiện có: Learning 826 test (1 failure, 1 error, 0 bỏ qua), Assessment 358 test (không lỗi, không bỏ qua). Hai lỗi Learning đúng tên ngoại lệ đã ghi trong plan. Không có log lệnh package để xác nhận cả hai lệnh đã kết thúc; không suy diễn kết quả package từ XML test. Chi tiết: [báo cáo đồng bộ](./reports/pm-2026-10-21-writing-plan-status.md).
