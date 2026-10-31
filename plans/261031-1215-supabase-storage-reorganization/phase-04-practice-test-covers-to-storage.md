---
phase: 4
title: "Practice test covers to storage"
status: pending
priority: P1
effort: "1.5d"
dependencies: [1, 3]
---

# Phase 4: Practice test covers to storage

<!-- Updated: Validation Session 1 - phạm vi mở rộng sang biểu đồ Writing; WebP; base URL mặc định compose đã đổi -->

## Phân công

- **Claude** làm bước 1 đến 5: kiểm kê id (truy vấn chỉ đọc), kiểm luồng chấm dùng biểu đồ (bước 2), xuất WebP, lưu bản sao lưu SVG ngoài repo, upload lên `media` (xin xác nhận từng lần ghi).
- Claude xuất một file danh sách `id → khóa` trong thư mục plan để Codex dùng viết migration.
- **Codex** làm bước 3 và 6 (test và migration `V90`) **sau** khi Claude báo đã upload đủ. Brief: `briefs/codex-phase-04-inline-images.md`. Đã kiểm kê trên DB tạm chạy đủ 89 migration: 58 bìa và 28 biểu đồ = 86 dòng, khớp plan. Danh sách: `data/phase-04-inline-images.csv`.

## Overview

Chuyển ảnh nhúng dạng data URI SVG base64 trong `content_assets.media_reference` sang file WebP trên bucket `media`; DB chỉ giữ khóa. Gồm hai nhóm:

- **Bìa phần đề luyện tập:** 58 dòng do `V34__practice_test_topic_covers.sql` đặt.
- **Đã kiểm lại (2026-10-31):** 86 ảnh gồm 70 bìa (58 của V34 và 12 ảnh chữ T1/T2 khác) và 16 biểu đồ thật (có trục, nhãn). Con số "28 biểu đồ" trước đây là sai, vì gộp cả 12 ảnh chữ vào. Nguồn: `V11`, `V32`, `V37`, `V38`, `V44` (con số cuối lấy từ kiểm kê DB ở bước 1, vì các migration ghi đè lên nhau).

Tên phase giữ nguyên như plan; phạm vi gồm cả hai nhóm (quyết định đã chốt ở phiên validate 1).

## Requirements

- Functional: bìa là object `practice/covers/{assetId}.webp` (1280×800), biểu đồ là `writing/charts/{assetId}.webp`, cùng bucket `media`; `content_assets.media_reference` giữ khóa; `SearchPracticeTestsUseCase.coverUrl` và `GetWritingTaskUseCase` trả URL công khai như cũ.
- Non-functional: không sửa `V34`/`V11`/`V37`/`V38`/`V44` (đã áp dụng); không đổi giao diện; không cần `image/svg+xml` trong bucket. Biểu đồ phải đọc rõ chữ và số trục sau khi rasterize.

## Architecture

`MediaReferencePolicy.resolve(AssetType.IMAGE, …)` đã xử lý khóa tương đối khi `CONTENT_MEDIA_BASE_URL` có giá trị, nên code đọc không đổi. Việc cần làm là dữ liệu: xuất ảnh từ SVG nhúng, upload, rồi migration mới thay giá trị cột. Với bìa dùng WebP lossy (~q85); với biểu đồ (đường nét mảnh, chữ) dùng WebP lossless hoặc near-lossless để không mờ chữ. Kích thước biểu đồ: gấp đôi viewBox của SVG gốc, tối đa 1600 px chiều rộng.

Base URL: `docker-compose.yml` nay mặc định trỏ URL Supabase thật, còn `config-repo/content-service.yaml` mặc định rỗng. Chạy ngoài compose mà không đặt biến thì khóa không resolve (audio ném lỗi, ảnh về `null` hoặc lỗi tùy chỗ gọi). Thứ tự triển khai vì thế vẫn quan trọng (xem Risk).

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V90__inline_images_to_storage_keys.sql` (số kiểm lại lúc làm), test migration.
- Modify: không có code đọc. Có thể phải sửa nơi dùng ảnh biểu đồ nếu bước 2 phát hiện luồng dùng data URI trực tiếp.
- Công cụ một lần (không đưa vào repo): script xuất ảnh đặt ngoài repo, ví dụ `sharp` hoặc ImageMagick; kết quả và bản sao SVG gốc lưu ngoài repo.
- Tham khảo: `V34__practice_test_topic_covers.sql`, `SearchPracticeTestsUseCase.coverUrl`, `JdbcPracticeTestCatalogReader` (nguồn `coverReference`), `GetWritingTaskUseCase` (dùng `MediaReferencePolicy` cho ảnh Writing).

## Implementation Steps

1. **Kiểm kê thực tế** trên `content_db` (chỉ đọc): `select id, asset_type, left(media_reference, 40) from content_assets where media_reference like 'data:%' order by id;`. Phân loại: bìa phần đề (58 id của `V34`: `2d000000-0000-4000-8000-0100000010xx` và `…0000000000xx`) và biểu đồ Writing (id còn lại). Lấy danh sách id từ chính truy vấn này, không chép tay. Ghi tổng số mỗi nhóm.
2. **Kiểm người dùng của ảnh biểu đồ:** tìm trong content-service, assessment-service, ai-learning-service mọi chỗ đọc `media_reference`/`mediaUrl` của ảnh Writing (ví dụ chấm bài Writing bằng LLM có gửi ảnh). Nếu có chỗ dùng data URI trực tiếp, ghi lại và quyết định cách lấy ảnh từ URL (hoặc tải khóa từ storage) trước khi migration.
3. **Test trước:** test migration: sau khi áp dụng, các id đã liệt kê có `media_reference` khớp `practice/covers/<id>.webp` hoặc `writing/charts/<id>.webp` và không còn `data:`; test `coverUrl` với khóa + base URL → URL đầy đủ; test `GetWritingTaskUseCase` trả URL cho ảnh biểu đồ.
4. **Xuất ảnh:** với mỗi id, lấy `media_reference`, giải base64 ra SVG, rasterize sang WebP theo quy tắc ở Architecture. Mở ngẫu nhiên 5 bìa và **tất cả** biểu đồ có chữ nhỏ để đối chiếu với SVG (gradient, nét, nhãn trục, số liệu). Lưu SVG gốc vào kho ngoài repo làm bản sao lưu.
5. **Upload** lên `media/practice/covers/` và `media/writing/charts/` (xin người dùng đồng ý lúc chạy; Dashboard hoặc script bằng khóa server trên máy người dùng). Kiểm: `curl -sI` từng URL trả `200 image/webp`; `storage.objects` đếm đủ theo từng nhóm.
6. **Migration** (sau khi file đã có trên bucket): hai câu `UPDATE content_assets SET media_reference = '<tiền tố>' || id || '.webp' WHERE asset_type = 'IMAGE' AND media_reference LIKE 'data:image/svg+xml;base64,%' AND id IN (<danh sách id của nhóm>)`, một cho bìa và một cho biểu đồ, liệt kê id tường minh, idempotent. Tên migration mô tả hành vi, không có số phase/plan.
7. Chạy test content-service; kiểm trên dev: thư viện đề luyện tập hiện đủ bìa; mở từng bài Writing có biểu đồ, chữ đọc rõ.
8. Ghi vào tài liệu phase 2: tiền tố `practice/covers/` và `writing/charts/` thuộc `media`.

## Success Criteria

- [ ] Số object `practice/covers/*.webp` và `writing/charts/*.webp` khớp số id kiểm kê, mỗi cái trả `200 image/webp`.
- [ ] `select count(*) from content_assets where asset_type = 'IMAGE' and media_reference like 'data:%'` bằng 0.
- [ ] Thư viện đề luyện tập hiển thị đủ bìa; mọi bài Writing có biểu đồ hiển thị đọc rõ trên dev.
- [ ] Không còn luồng nào phụ thuộc data URI của biểu đồ (hoặc đã chuyển sang URL).
- [ ] Test content-service xanh; bản sao lưu SVG gốc được lưu ngoài repo và vị trí được ghi lại cho người dùng.

## Risk Assessment

- **Thứ tự quan trọng:** chạy migration trước khi file lên bucket, hoặc khi base URL sai, làm ảnh hỏng. `SearchPracticeTestsUseCase.coverUrl` nuốt `RuntimeException` và trả `null` cho bìa; biểu đồ có thể gây lỗi ở `GetWritingTaskUseCase`. Giữ thứ tự bước 5 → 6; kiểm base URL (phase 1) trước khi deploy.
- Môi trường mới (fresh DB): `V34`… rồi migration này chạy liên tiếp, ảnh chỉ hiện khi bucket đã có file và base URL đúng. Ghi rõ trong tài liệu triển khai.
- Chất lượng rasterize: biểu đồ chữ nhỏ có thể mờ; dùng lossless và kiểm từng cái ở bước 4. Nếu vẫn mờ, tăng độ phân giải.
- Luồng chấm AI có thể đang đọc data URI (bước 2); đổi sang URL cần xác nhận không làm đổi kết quả chấm.
- Lợi ích dung lượng DB nhỏ (mỗi SVG vài KB); giá trị chính là thống nhất nguồn ảnh và cho R&D thay ảnh không cần migration. Nói rõ khi báo cáo.
- Rollback: giữ SVG gốc ngoài repo; một migration ngược đưa `data:` về từ bản sao lưu; file trên bucket có thể giữ nguyên.
