# Brief Codex: phase 4, ảnh nhúng trong DB (bìa đề luyện tập và biểu đồ Writing)

Người làm: **Codex** (migration và test). Claude làm xuất ảnh và upload trước. Không ghi Supabase, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Điều kiện bắt đầu (phải đủ trước khi làm)

1. Claude báo đã upload đủ 86 file lên `media`: `practice/covers/{id}.webp` (58 file) và `writing/charts/{id}.webp` (28 file), và `curl` từng khóa trả `200 image/webp`.
2. Phase 3 đã merge vào `feat/supabase-storage-reorganization` (vì cùng chạm `ContentMediaController`).
3. Số migration: `V90`. Kiểm lại `services/content-service/src/main/resources/db/migration` trước khi tạo, vì có thể đã có số mới.

Nếu thiếu điều kiện nào: trả `NEEDS_CONTEXT`, không tạo migration.

## Nhánh và PR

- `feat/storage-p4-inline-images` từ `feat/supabase-storage-reorganization`, PR vào đó (draft).

## Dữ liệu đầu vào (đã xuất, đã kiểm)

File: `plans/261031-1215-supabase-storage-reorganization/data/phase-04-inline-images.csv`, 86 dòng, cột `id,kind,target_key,svg_data_uri_chars`.
- `kind = practice-cover`: 58 dòng. Đúng bằng danh sách id trong `V34__practice_test_topic_covers.sql`.
- `kind = writing-chart`: 28 dòng. Nằm trong `V11`, `V37`, `V38`, `V44`.
- Trạng thái đã kiểm trên DB tạm chạy đủ 89 migration: `content_assets` có 86 dòng `IMAGE` chứa `data:image/svg+xml;base64,...`, không dòng nào khác.

## Việc cần làm

1. Viết test migration (TDD) trước: sau khi áp `V90`:
   - không còn dòng `content_assets` nào có `media_reference LIKE 'data:%'`;
   - 58 dòng bìa có `media_reference = 'practice/covers/' || id || '.webp'`;
   - 28 dòng biểu đồ có `media_reference = 'writing/charts/' || id || '.webp'`;
   - số dòng `IMAGE` không đổi (86).
2. Tạo `services/content-service/src/main/resources/db/migration/V90__inline_images_to_storage_keys.sql`:
   - Có 86 câu `UPDATE` với id tường minh, lấy từ CSV (script một lần, không viết tay). Mỗi câu có điều kiện `AND media_reference LIKE 'data:image/svg+xml;base64,%'` để idempotent.
   - Tên migration mô tả hành vi. Không có số phase hay mã plan trong file.
3. Chạy test migration và các test content-service liên quan (`GetWritingTaskUseCase`, `SearchPracticeTestsUseCase`, test coverUrl). Các test này phải đọc được khóa và resolve ra URL khi `CONTENT_MEDIA_BASE_URL` có giá trị.
4. Nếu có test cũ hard-code `data:image/svg+xml` cho các id này: sửa test để khớp khóa mới. Ghi rõ từng test đã sửa trong `Summary`.

## Không làm

- Không sửa `V34`, `V11`, `V37`, `V38`, `V44` (đã áp dụng).
- Không đổi `MediaReferencePolicy` (phase 7).
- Không xóa bản SVG gốc khỏi repo. Bản sao lưu nằm ngoài repo, do Claude giữ.
- Không ghi Supabase.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/content-service -am test
```
Dán số test thật vào PR.

## Tiêu chí xong

- Migration V90 áp dụng sạch trên DB mới; test migration xanh.
- Không còn `data:` trong `content_assets`.
- Test content-service xanh.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Điều kiện bắt đầu chưa đủ.
- Số lượng dòng khi chạy migration khác 86 hoặc có dòng lạ.
- Test cũ mâu thuẫn với khóa mới: báo, không xóa test.
