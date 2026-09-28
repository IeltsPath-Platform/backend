---
phase: 2
title: "Gộp kiến trúc đích vào tài liệu sau khi tách xong"
status: pending
priority: P2
dependencies: [1]
---

# Phase 2: Gộp kiến trúc đích vào tài liệu sau khi tách xong

## Overview

Khi code đã tách xong (library-service tồn tại, learning-support đã xóa), viết lại §1–§11 của `docs/system-architecture.md` theo code mới, xóa §12. Plan triển khai sau này không cần sửa lại file này nữa, pha này đã lo.

## Requirements

- Functional: §1–§11 mô tả đúng code sau khi tách; không còn mục "kiến trúc đích".
- Non-functional: kiểm từng dữ kiện với file thật (AGENTS §6); cập nhật "Cập nhật lần cuối" + commit kiểm chứng.

## Related Code Files

- Modify: `docs/system-architecture.md`:
  - §1 Tổng quan + sơ đồ: thêm library :8081, bỏ learning-support :8086; thêm mũi tên game → library, library → content.
  - §2 bảng service: thêm dòng library; content (bỏ từ vựng/video, thêm bài học); user (thêm activity, streak, `examiner_profiles` nếu đã có code); notification (bảng nếu đã có code); ai-learning và assessment (phần học bài, tự chấm đề cuối nếu đã có code); **xóa dòng** learning-support; ghi route Gateway theo nhóm path.
  - §3 giao tiếp HTTP: thêm library → content (`GET /api/content/topics/{id}`, `CONTENT_SERVICE_URL`), game → library (`LIBRARY_SERVICE_URL`); câu cuối §3 (outbox chưa relay) giữ access/content/game.
  - §5: bỏ "learning-support" khỏi câu `query` (kiểm lại library/user có `application/query` không).
  - §7: compose bỏ `learning-support-db`, thêm `library-db`; thứ tự chạy host có library sau content.
  - §8: thêm quyết định tách library, giải thể learning-support, giữ path qua Gateway, model bài học.
  - §9: thêm "Scoped exception advice (`assignableTypes`) giữ contract lỗi khi chuyển controller" nếu code dùng.
  - §11: bỏ dòng "community, learning-support có bảng `outbox_events`" → chỉ community; sửa dòng README root "5433 là learning-support"; thêm điểm mới nếu có (ví dụ path `/api/learning-support/**` không còn service cùng tên; streak do client tự khai).
  - Xóa §12.

## Implementation Steps

1. Xác nhận code đã tách (`services/library-service` tồn tại, `services/learning-support-service` đã xóa, route Gateway mới), lấy commit hiện tại.
2. Kiểm dữ kiện: `pom.xml` modules, `docker-compose.yml`, `api-gateway.yaml` routes, `config-repo/*.yaml`, controller `@RequestMapping` của library/user/content.
3. Viết lại các mục theo danh sách; xóa §12.
4. `grep -n "learning-support" docs/system-architecture.md` → chỉ còn path `/api/learning-support/...` hợp lệ hoặc ghi chú lịch sử có chủ đích.

## Success Criteria

- [ ] Không còn §12; §1–§11 khớp code (từng dòng bảng đã đối chiếu file).
- [ ] Không còn `learning-support-service`, `learning_support_db`, `:8086`, `5433` trong tài liệu (trừ ghi chú lịch sử có chủ đích).
- [ ] "Cập nhật lần cuối" có ngày và commit mới.

## Risk Assessment

| Rủi ro | Giảm thiểu |
| --- | --- |
| Code cuối cùng lệch plan (đổi trong lúc cook) | Viết theo code thật ở bước 2, không chép từ §12 |
| Plan triển khai sau này cũng sửa `docs/system-architecture.md` → trùng | Ghi trong plan triển khai: phần tài liệu kiến trúc do pha này làm |
