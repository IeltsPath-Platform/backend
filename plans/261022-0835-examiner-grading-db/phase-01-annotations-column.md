---
phase: 1
title: "Cột annotations cho review độc lập"
status: completed
priority: P1
effort: "1h"
dependencies: []
---

# Phase 1: Cột `annotations`

## Overview

Thêm `human_review_submissions.annotations JSONB` để lưu các lỗi Examiner đánh dấu trong bài. File
`services/assessment-service/src/main/resources/db/migration/V15__human_review_annotations.sql` **đã tạo, chú thích đã khớp** mã lỗi lấy từ danh mục, và đã thử trên
PostgreSQL 15 tạm (V1 đến V16 chạy sạch, mỗi file trong một transaction như Flyway).

## Requirements

- Mảng JSON, mỗi phần tử một lỗi: `{"excerpt","start","end","errorCode","suggestion"}`.
- `NULL` nghĩa là không có lỗi nào được đánh dấu; mọi review đã chốt trước đây giữ `NULL`.
- Chỉ có giá trị khi review đã chốt (`completed_at` có giá trị), cùng quy tắc với `band`, `criteria`, `feedback`.
- Tối đa 200 phần tử.

## Files

- Sửa: `V15__human_review_annotations.sql` (chỉ chú thích đầu file).

## Steps

1. Sửa chú thích: thay `errorType`, `group`, `criterion` bằng `errorCode`; ghi rõ `errorCode` thuộc `error_types` (content-service),
   nhóm và tiêu chí suy ra từ nhóm của mã nên **không lưu**; nêu kiểm tra phía ứng dụng (mã có trong danh mục, `start < end`).
2. Giữ nguyên hai ràng buộc: `chk_human_review_annotations_array`, `chk_human_review_annotations_completed`.
3. Không thêm `CHECK` kiểm trường bên trong từng lỗi: danh mục mã nằm ở DB khác, ứng dụng mới kiểm được.

## Validation

Đã chạy trên DB tạm:

| Trường hợp | Kết quả |
|---|---|
| Chốt review kèm một lỗi, đọc lại | lưu và đọc đúng |
| Ghi `annotations` khi chưa chốt | bị từ chối |
| `annotations` không phải mảng | bị từ chối |
| 201 lỗi | bị từ chối |

Còn phải chạy lại cả bốn sau khi sửa chú thích (chú thích không đổi hành vi, chỉ để chắc file không hỏng).

## Rollback

Migration mới chỉ thêm cột nên không phá dữ liệu cũ. Nếu bỏ hướng này: migration mới `DROP CONSTRAINT` hai ràng buộc rồi
`DROP COLUMN annotations`. Không sửa V15 sau khi đã merge.
