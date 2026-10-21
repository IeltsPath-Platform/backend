# Báo cáo nghiệm thu Phase 3 — 2026-10-21

**Status: DONE_WITH_CONCERNS**

## Kết quả

- Hoàn tất hiển thị đánh giá từng KP con dưới tiêu chí bài Writing, gồm nhãn Đạt / Cần luyện / Chưa đủ bằng chứng và đoạn trích khi có.
- Hoàn tất hiển thị study point `KNOWLEDGE_POINT` với lý do, đoạn trích và nút Luyện tập mở đúng KP; giữ tương thích với payload cũ và kind chưa biết.
- Cập nhật README phần Writing và đánh dấu Phase 3 hoàn tất trong kế hoạch.

## Tệp đã sửa

- `frontend/src/types/learningPath.ts`
- `frontend/src/features/learning-path/api/learningAdapters.ts`
- `frontend/src/features/learning-path/components/essayCriteria.ts`
- `frontend/src/features/learning-path/components/EssayStudyPoints.tsx`
- `frontend/src/features/learning-path/components/PracticeEssays.tsx`
- `frontend/src/features/learning-path/lib/studyPointPractice.ts`
- `frontend/src/features/learning-path/practice-essay.css`
- `frontend/src/features/learning-path/api/learningAdapters.studyPoints.test.ts`
- `frontend/src/features/learning-path/components/EssayStudyPoints.test.tsx`
- `frontend/src/features/learning-path/components/PracticeEssays.test.tsx`
- `frontend/src/features/learning-path/lib/studyPointPractice.test.ts`
- `frontend/README.md`
- `backend/plans/261021-2016-writing-criterion-sub-kp-judgments/plan.md`
- `backend/plans/261021-2016-writing-criterion-sub-kp-judgments/phase-03-frontend-sub-kp-display.md`
- Báo cáo này.

## Kiểm tra

- 4 bộ test liên quan: **50/50 đạt**.
- Test theo phase: test mới được chạy đỏ trước khi triển khai; sau triển khai các test tập trung đều xanh.
- `npm.cmd run typecheck`: đạt.
- `npm.cmd run lint`: đạt.
- `npm.cmd run build`: đạt; Vite vẫn cảnh báo một số bundle lớn hơn 500 kB.
- Toàn bộ Vitest: **223/224 đạt**. Test đăng nhập `AuthPage community redirect` thất bại do thiếu `ToastProvider`; lỗi ngoài phạm vi Phase 3.
- `graphify update .`: đạt.
- Chưa nghiệm thu giao diện thủ công trên trình duyệt; không có lệnh test riêng nào bị bỏ qua.

## Trạng thái kế hoạch

Phase 3 hoàn tất. Kế hoạch tổng thể vẫn `in_progress` vì các cổng nghiệm thu backend và tích hợp toàn dự án còn lại chưa được xác nhận trong phase này.

**Câu hỏi còn mở:** Không. Cần nghiệm thu trực quan trên trình duyệt trước khi xác nhận giao diện cuối cùng.
