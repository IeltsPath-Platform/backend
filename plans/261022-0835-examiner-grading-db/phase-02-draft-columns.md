---
phase: 2
title: "Cột nháp cho review độc lập"
status: completed
priority: P1
effort: "1h"
dependencies: []
---

# Phase 2: Cột nháp `draft_payload`

## Overview

Nút "Lưu nháp" của màn chấm cần chỗ lưu bản đang chấm dở. Review độc lập hiện chỉ có một thao tác lưu là `POST /human-review`, chốt
luôn kết quả. Thêm hai cột vào `human_reviews` (mỗi việc chấm có đúng một dòng, đã có `grader_user_id`, `status` với giá trị
`IN_REVIEW` chưa dùng, và `started_at`). Bài thi thử và test chương/khóa đã có version `DRAFT` nên không dùng cột này.

## Requirements

- `draft_payload JSONB`: object cùng hình dạng dữ liệu chốt nhưng cho phép thiếu (band, `criteria`, `feedback`, `annotations`).
- `draft_saved_at TIMESTAMPTZ`: giờ lưu nháp gần nhất, để màn hiện "Đã lưu nháp lúc …".
- `NULL` khi chưa lưu nháp lần nào; trả về `NULL` sau khi chốt.
- Học viên không bao giờ đọc được nháp (chỉ các cột chính thức của `human_review_submissions` được trả).

## Files

- Tạo (đã xong): `services/assessment-service/src/main/resources/db/migration/V16__human_review_draft.sql`

## SQL (đã thử trên PostgreSQL 15 tạm)

```sql
ALTER TABLE human_reviews ADD COLUMN draft_payload JSONB;
ALTER TABLE human_reviews ADD COLUMN draft_saved_at TIMESTAMPTZ;

ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_object
    CHECK (draft_payload IS NULL OR jsonb_typeof(draft_payload) = 'object');
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_size
    CHECK (draft_payload IS NULL OR octet_length(draft_payload::text) <= 204800);
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_saved_at
    CHECK ((draft_payload IS NULL) = (draft_saved_at IS NULL));
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_open
    CHECK (draft_payload IS NULL OR status <> 'COMPLETED');
```

## Steps

1. Tạo file `V16` với nội dung trên, thêm chú thích đầu file nói rõ mục đích và rằng nháp không kiểm tra band hay mã lỗi chặt
   (kiểm tra đầy đủ chỉ khi chốt).
2. Trước khi đặt tên, `git fetch` và kiểm tra nhánh khác có dùng `V16` ở assessment-service không.

## Validation

Đã thử trên DB tạm sau V1 đến V15:

| Trường hợp | Kết quả |
|---|---|
| Lưu nháp object + giờ, đặt `IN_REVIEW` | chấp nhận |
| Nháp là mảng | bị từ chối |
| Có nháp mà không có giờ lưu | bị từ chối |
| Nháp lớn hơn 200 KB | bị từ chối |
| Đặt `COMPLETED` mà vẫn giữ nháp | bị từ chối |
| Đặt `COMPLETED` và xóa nháp trong cùng câu UPDATE | chấp nhận |

## Lưu ý cho code đi kèm

- Câu UPDATE chốt trong `JdbcHumanReviewStore.complete` phải đặt `draft_payload = NULL, draft_saved_at = NULL` **cùng lúc** với
  `status = 'COMPLETED'`. Nếu quên, ràng buộc `chk_human_reviews_draft_open` từ chối: đây là lưới an toàn có chủ ý.
- `completeAttempt` và `reopenCompleted` của đường kết quả không đụng cột nháp (luôn `NULL`) nên không đổi.
- Lần lưu nháp đầu đặt `status = 'IN_REVIEW'` và `started_at`.

## Rollback

Migration mới `DROP CONSTRAINT` bốn ràng buộc rồi `DROP COLUMN` hai cột. Chỉ mất các bản nháp chưa chốt.
