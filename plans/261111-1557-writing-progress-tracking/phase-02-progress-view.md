---
phase: 2
title: "Progress view"
status: done
priority: P2
dependencies: [1]
---

# Phase 2: Màn tiến bộ Writing (frontend)

## Overview
Mục "Tiến bộ Writing" trong trang báo cáo học tập, dựng hoàn toàn từ endpoint của phase 1.

## Requirements
- Chọn Task 1 / Task 2.
- **Biểu đồ band:** overall và bốn tiêu chí theo bài; mốc đổi `promptVersion` có đánh dấu nhỏ; band ghi là ước tính.
  Dùng skill `dataviz` trước khi viết biểu đồ.
- **Lỗi hay gặp:** tối đa 5 dòng: tên lỗi, "x/y bài", quy tắc (`fixHint`), nút "Luyện lỗi này" (bộ luyện đã duyệt theo
  `practiceKnowledgePointId`; không có thì ẩn nút).
- **Đã khắc phục:** danh sách ngắn kèm "không lặp lại ở N bài gần nhất".
- Trạng thái: đang tải, lỗi, rỗng (chưa có bài nào: dẫn tới luyện Writing), `insufficient` (cần thêm bài để thấy xu hướng).
- Mobile trước, bàn phím, tương phản WCAG AA; bọc `ErrorBoundary` (`frontend/AGENTS.md` §4).

## Related Code Files
- Tìm bằng `graphify query` trang báo cáo học tập hiện có và feature Writing trước khi tạo file.
- Create: API client và schema Zod cho `writing/progress`, component mục tiến bộ, test component.
- Không thêm dependency biểu đồ mới nếu repo đã có; chưa có thì dừng hỏi.

## Implementation Steps
1. Đọc contract `writing-progress-v1.md` và JSON mẫu của phase 1.
2. Test đỏ cho mapping và ba trạng thái (rỗng, ít bài, đủ bài) → component.
3. `npm run lint`, `npm run typecheck`, `npm run build`.

## Success Criteria
- [ ] Dựng được từ một request, không gọi thêm API.
- [ ] Học viên mới (0 bài) và học viên 1 đến 2 bài không thấy biểu đồ trống vô nghĩa.
- [ ] "Luyện lỗi này" mở đúng bộ luyện của mã lỗi.
