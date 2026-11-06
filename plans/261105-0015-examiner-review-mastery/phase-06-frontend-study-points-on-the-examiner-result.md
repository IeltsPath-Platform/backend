---
phase: 6
title: "Frontend study points on the examiner result"
status: pending
priority: P2
dependencies: [5]
---

# Phase 6: Frontend study points on the examiner result

## Overview
Trang kết quả examiner (`ExaminerResult`) hiện khối "Cần ôn" từ `studyPoints` của `GET …/human-review` (JSON chốt ở phase 5).
Repo `frontend/`.

## Requirements
- Schema `HumanReview` (zod) thêm `studyPoints` optional, mặc định `[]`; parse lỗi một điểm không làm vỡ cả review.
- Khối "Cần ôn" chỉ khi `status === 'DONE'` và có điểm; mỗi điểm: tên KP (fallback mã), nhãn nguồn ("Lỗi giáo viên đánh dấu ×n" với
  tên loại lỗi từ catalogue đang có trên trang / "Tiêu chí CC: band 5.0"), nút:
  - `reviewItemId` có → "Ôn ngay" mở trang bài ôn sẵn có;
  - không, có `practiceKnowledgePointId` → "Luyện tập" theo cùng điều hướng free practice mà `EssayStudyPoints` (luồng AI) đang dùng;
  - không có cả hai → chỉ hiển thị.
- Không đổi bố cục phần chấm hiện có.

## Architecture
Tái dùng nhãn/điều hướng của `EssayStudyPoints.tsx` (luồng AI) thay vì viết mới; nếu component đó nhận được shape tương thích thì
dùng lại trực tiếp, không thì tách phần render nút chung.

## Related Code Files
- Modify: `frontend/src/features/practice/writing/examinerApi.ts` (schema), `frontend/src/features/practice/writing/ExaminerResult.tsx`,
  CSS `writing-examiner.css`
- Đọc: `EssayStudyPoints.tsx` (tìm bằng grep), route trang bài ôn và free practice
- Tests: `frontend/src/features/practice/writing/ExaminerResult.test.tsx`

## Implementation Steps
1. Baseline: `npx vitest run src/features/practice/writing`, `npm run typecheck`, `npm run lint`; ghi lỗi nền.
2. Test đỏ: DONE + 2 điểm → khối hiện đúng tên, nhãn, nút "Ôn ngay" (href review) và "Luyện tập"; không có `studyPoints` → không có khối;
   PENDING → không có khối; điểm thiếu tên → hiện mã.
3. Code.
4. Chạy lại test, typecheck, lint.

## Success Criteria
- [ ] Test xanh; typecheck, lint sạch.
- [ ] Review cũ (không có `studyPoints`) hiển thị như trước.

## Risk Assessment
- Outcome áp sau event (vài giây sau `DONE`): learner mở trang ngay có thể chưa thấy khối. Trang đã có poll khi chưa DONE; sau DONE
  không poll. Chấp nhận (lần mở sau sẽ thấy); không thêm poll.
