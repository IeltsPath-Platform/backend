---
phase: 6
title: "Writing key rename"
status: pending
priority: P3
effort: "0.5d"
dependencies: [1, 4]
---

# Phase 6: Writing key rename

## Phân công

- **Claude**: bước 1 (quét DB toàn bộ cột và JSON tìm `writtings`), bước 3 (sao chép 4 file, cộng các file biểu đồ phase 4 nếu chúng nằm cùng thư mục), bước 7 (xóa khóa cũ sau khi xác nhận).
- **Codex**: bước 2, 4, 5 (test, migration `V91`, cập nhật `scripts/generate-course-writing-charts.py`, CSV, README, `LessonPipelineSeedTest`). Chưa có brief Codex; viết sau khi Claude có kết quả quét.
- Deploy: migration chỉ merge sau khi file đã có trên bucket.

## Overview

Đổi khóa ảnh biểu đồ Writing từ `writtings/leassons/…` sang `writing/lessons/…` (4 ảnh trong bucket `media`) và cập nhật mọi tham chiếu trong `content_db`, tài liệu và seed. Thứ tự an toàn: sao chép file → migration → kiểm → xóa khóa cũ.

## Requirements

- Functional: 4 ảnh có mặt ở khóa mới; mọi tham chiếu trỏ khóa mới; ảnh Writing hiển thị.
- Non-functional: không có thời điểm nào tham chiếu trỏ tới khóa chưa có file; có đường quay lui cho tới khi xác nhận.

## Architecture

Các khóa hiện có trong bucket (đọc ngày 2026-10-31):

| Khóa cũ | Khóa mới |
|---|---|
| `writtings/leassons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png` | `writing/lessons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png` |
| `writtings/leassons/PS-W1-WR-2/ps-w1-wr-2-family-spending.png` | `writing/lessons/PS-W1-WR-2/ps-w1-wr-2-family-spending.png` |
| `writtings/leassons/PS-W5-WR-1/ps-w5-wr-1-park-visitors.png` | `writing/lessons/PS-W5-WR-1/ps-w5-wr-1-park-visitors.png` |
| `writtings/leassons/PS-W5-WR-2/ps-w5-wr-2-fuel-prices.png` | `writing/lessons/PS-W5-WR-2/ps-w5-wr-2-fuel-prices.png` |

Các migration đã áp dụng (`V64`, `V65`, `V67`) vẫn chèn khóa cũ; **không sửa chúng**. Migration mới đổi tiền tố, nên mọi DB mới chạy `V64..` rồi migration này và kết thúc ở khóa mới.

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V91__writing_chart_keys_renamed.sql` (số kiểm lại lúc làm), test migration.
- Modify: `docs/assets/course-writing-charts/upload-urls.csv` và `README.md` cùng thư mục, `scripts/generate-course-writing-charts.py`, `services/content-service/README.md` nếu nêu khóa, test seed khẳng định khóa cũ (`LessonPipelineSeedTest`).
  <!-- Updated: Validation Session 1 - thêm generate-course-writing-charts.py (grep thấy 'writtings' ở đây) -->
  Các file hiện chứa `writtings` (đã grep, bỏ `target/`): `V64`, `V65`, `V67` (không sửa), `LessonPipelineSeedTest.java`, `scripts/generate-course-writing-charts.py`, `docs/assets/course-writing-charts/upload-urls.csv`, `docs/assets/course-writing-charts/README.md`.
- Tham khảo: `V64__course_writing_charts_and_guides.sql`, `V65__organize_course_writing_charts_by_task.sql`, `V67__writing_w1_table_essay.sql`.

## Implementation Steps

1. **Kiểm kê tham chiếu:** `grep -rn "writtings" backend frontend docs` (bỏ `target/`, `node_modules/`) và trên `content_db` quét **mọi** cột text/jsonb (ví dụ `pg_dump --data-only content_db | grep -c writtings`) vì khóa có thể nằm trong JSON hướng dẫn chứ không chỉ `media_reference`. Ghi danh sách bảng/cột.
2. **Test trước:** test migration: trước khi áp dụng một dòng mẫu mang khóa cũ, sau khi áp dụng mang khóa mới; không còn dòng chứa `writtings/leassons`.
3. **Sao chép file** (xin người dùng đồng ý): Dashboard "Move" hoặc Storage REST `POST /storage/v1/object/copy` (`bucketId`, `sourceKey`, `destinationKey`) chạy trên máy người dùng bằng khóa server. Kiểm 4 khóa mới trả `200 image/png`.
4. **Migration:** `UPDATE` thay tiền tố trong mọi cột đã liệt kê ở bước 1, ví dụ `UPDATE content_assets SET media_reference = replace(media_reference, 'writtings/leassons/', 'writing/lessons/') WHERE media_reference LIKE 'writtings/leassons/%';` cộng các cột khác nếu bước 1 tìm thấy. Idempotent.
5. Cập nhật CSV/README/seed test cho khóa mới.
6. Triển khai, xem 4 bài Writing có biểu đồ (PS-W1-WR-1/2, PS-W5-WR-1/2) hiển thị ảnh.
7. **Sau khi xác nhận**, xóa 4 object cũ và các `.emptyFolderPlaceholder` còn sót dưới `writtings/`.

## Success Criteria

- [ ] `grep` repo và quét `content_db` không còn `writtings/leassons`.
- [ ] 4 khóa mới trả `200 image/png`; 4 bài Writing hiển thị biểu đồ.
- [ ] Khóa cũ đã xóa sau khi xác nhận.
- [ ] Test migration và seed xanh.

## Risk Assessment

- Khóa cũ xuất hiện ở cột/JSON chưa kiểm kê: bước 1 quét toàn DB; thất bại thì ảnh hỏng — kiểm hiển thị từng bài ở bước 6 trước khi xóa khóa cũ.
- Deploy migration trước khi sao chép file: tham chiếu trỏ khóa chưa có. Giữ đúng thứ tự bước 3 → 4.
- Rollback: giữ khóa cũ tới bước 7; hoàn tác bằng migration đảo `replace`.
