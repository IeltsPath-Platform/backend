---
phase: 3
title: "Đối chiếu chéo và nối plan"
status: pending
priority: P1
dependencies: [1, 2]
effort: "0.25 ngày"
---

# Phase 3: Đối chiếu chéo và nối plan

## Overview

Kiểm ba tài liệu nói cùng một điều, khớp với plan triển khai. Bỏ phần sửa tài liệu trùng khỏi plan triển khai, để không ai sửa hai lần.

## Implementation Steps

1. **Bảng đối chiếu tên:** mỗi bảng, cột, endpoint mới hoặc đổi, so ba nguồn:
   - DATABASE_V5 §4.1, §4.2, §5.1, §6.3, §7.1, §7.21–§7.26;
   - `plan.md` của plan chia service;
   - phase 3–6 của plan triển khai.

   Lệch thì sửa tài liệu theo plan triển khai; nếu plan triển khai sai thì báo người dùng.
2. **Grep cả ba file** theo tiêu chí nghiệm thu trong `plan.md` của plan này.
3. **Sửa plan triển khai** `260929-1640-lesson-learning-pipeline-mvp`:
   - phase 1: bỏ mục "Modify: plan kiến trúc … `main-learning-pipeline.md`" và bước 6; thay bằng "đặc tả đã đồng bộ ở plan `260929-1830-sync-database-and-service-split-docs`";
   - phase 8: bỏ `.sdd/database/DATABASE_V5.md` khỏi danh sách sửa;
   - `blockedBy` trong frontmatter **đã đặt** lúc tạo plan này; khi xong thì không cần gỡ (plan này `completed` là đủ).
4. **Plan kiến trúc:** `blockedBy` **đã đặt** lúc tạo plan này.
5. `ck plan status` cho ba plan để kiểm phụ thuộc hiển thị đúng.

## Success Criteria

- [ ] Bảng đối chiếu không còn dòng lệch.
- [ ] Plan triển khai không còn bước sửa ba tài liệu này.
- [ ] Phụ thuộc giữa ba plan hiển thị đúng trong `ck plan status`.

## Risk Assessment

- **Plan triển khai đổi tiếp sau khi đồng bộ:** mỗi lần đổi bảng, cột hay luật ở plan triển khai thì sửa luôn DATABASE_V5 cùng commit. Ghi nhắc này vào `plan.md` của plan triển khai.
