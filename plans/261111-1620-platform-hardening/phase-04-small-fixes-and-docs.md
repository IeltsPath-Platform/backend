---
phase: 4
title: "Small fixes and docs"
status: done
priority: P3
dependencies: []
---

# Phase 4: Chỗ lệch nhỏ và tài liệu (frontend, docs)

## Requirements
- **Giá chấm ở frontend:** bỏ `FALLBACK_TERMS = { pointCost: 3, ... }` (`frontend/src/features/learning-path/components/usePracticeEssayGradeChoice.tsx`)
  và `SPEAKING_AI_POINT_COST` (`frontend/src/features/practice/speaking/`). API điều khoản chấm (`grading-terms`) lỗi thì
  nút "Chấm bằng AI" bị khóa kèm trạng thái lỗi và nút thử lại, không hiện giá đoán. `minWords` dự phòng: kiểm có cần
  không, nếu chỉ để hiển thị thì cũng bỏ.
- **Chính sách phí:** `backend/docs/ai-policy.md` thêm mục "Ai trả tiền cho lượt chấm AI": essay Practice/lesson/tự do
  trừ Points (`learning.writing.point-cost`); essay trong thi gate miễn phí vì thuộc khóa đã trả tiền và có hạn mức riêng
  (`ASSESSMENT_LLM_DAILY_LIMIT`); `MOCK` do EXAMINER chấm.
- **Gamification mobile:** `AGENTS.md` §3.8 thêm một câu: XP, streak (có freeze) và tim của học từ vựng trên mobile thuộc
  library-service, độc lập với gamification của Learning theo plan `261106-2330-mobile-vocabulary-study`; "nguồn duy nhất
  cho thưởng" chỉ nói về web. Cập nhật dòng ngày ở đầu `AGENTS.md`.
- **Bài Writing ở hai DB:** `docs/system-architecture.md` thêm một dòng: bài luyện ở Learning, bài thi ở Assessment, có chủ
  đích; lịch sử Writing của học viên (plan `261111-1557-writing-progress-tracking`) chỉ lấy bài luyện.

## Implementation Steps
1. Frontend: test đỏ cho trạng thái API giá lỗi → sửa; `npm run lint`, `npm run typecheck`, `npm run build`.
2. Sửa ba tài liệu; kiểm link và đường dẫn.

## Success Criteria
- [ ] `grep -rn "pointCost: 3\|POINT_COST" frontend/src` không còn giá cứng.
- [ ] Ba tài liệu đã ghi, đúng với code.
