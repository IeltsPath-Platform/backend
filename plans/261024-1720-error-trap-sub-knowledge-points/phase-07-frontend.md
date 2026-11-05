---
phase: 7
title: Frontend
status: completed
priority: P2
effort: 2d
dependencies:
  - 6
---

# Phase 7: Frontend

## Overview
Repo `frontend/` (cạnh `backend/`): hiện lỗi + `fixHint` khi chọn sai và khi mở bài ôn có lỗi chính; mục `ERROR_PATTERN`
trong giỏ ôn; KP con lồng dưới KP cha trong cây/báo cáo. Dùng đúng hợp đồng JSON ở phase 6.

## Requirements
- Kết quả câu (Practice, ôn, lesson, luyện tự do, chẩn đoán): có `error` → khối "Bạn mắc lỗi: {name}" + `fixHint`.
- Mở bài ôn và màn quay lại lý thuyết: có `errorTrap` → khối "Lỗi cần sửa: {name}" + `fixHint`.
- Giỏ ôn: mục `reason = ERROR_PATTERN` hiện tên KP cha kèm "lỗi: {errorTrap.name}"; nút "Ôn ngay" như mục tùy chọn khác.
- Cây kiến thức/báo cáo: `errorTraps` lồng dưới KP cha (tên, tier, mastery). Backend đã lọc KP con chưa có bằng chứng.
- Thiếu trường (backend cũ) → không hiện gì thêm, không lỗi. Không đổi API client ngoài trường mới; cập nhật mock.

## Related Code Files (grep xác nhận trước khi sửa; khác hẳn mô tả → dừng và báo)
- Modify: `src/features/learning-path/api/{learningApi.ts,httpLearningApi.ts,masteryGuideAdapters.ts}`, `api/mock/*`
- Modify: `src/features/learning-path/components/PracticeAttemptReview.tsx`, `src/features/practice/reading/ReadingReview.tsx`,
  màn kết quả lesson/ôn/chẩn đoán tương ứng
- Modify: thành phần giỏ ôn (`src/features/learning-path/components/guide/*`), cây kiến thức và báo cáo học viên
- Modify: `src/types/*` (kiểu `error`, `errorTrap`, `errorTraps`)

## Implementation Steps
1. Đọc `frontend/AGENTS.md`; baseline test/lint/build (lệnh trong `frontend/package.json`); ghi đỏ có sẵn.
2. **Đỏ trước** (Vitest + Testing Library sẵn có): kết quả câu có/không `error`; bài ôn có/không `errorTrap`; giỏ ôn có mục
   `ERROR_PATTERN`; cây/báo cáo lồng `errorTraps`; adapter đọc trường mới, thiếu trường vẫn chạy.
3. Kiểu → adapter → mock → component.
4. Chạy test, lint, build.

## Success Criteria
- [ ] Test mới xanh, test cũ xanh; lint và build xanh.
- [ ] Chạy được với backend thiếu trường.

## Risk Assessment
- Repo frontend có nhánh khác đang làm (màn author, plan 261023-0050): làm trên nhánh riêng, không sửa màn mã lỗi.
