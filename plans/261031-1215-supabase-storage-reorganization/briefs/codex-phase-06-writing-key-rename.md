# Brief Codex: phase 6, đổi khóa Writing `writtings/leassons` → `writing/lessons`

Người làm: **Codex** (migration, test, script, tài liệu). Claude làm sao chép file và kiểm kê. Không ghi Supabase trực tiếp, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Điều kiện bắt đầu

1. Claude báo đã sao chép các file sang khóa mới trên `media` và `curl` trả `200 image/png` cho từng khóa mới.
2. Phase 4 đã merge (cùng vùng `content_assets`; V91 đứng sau V90).
3. Bạn đã chọn: sẽ đưa file ảnh `PS-W1-WR-3`. Claude upload vào `writing/lessons/PS-W1-WR-3/` trước. Nếu file chưa có, làm các dòng còn lại và báo `DONE_WITH_CONCERNS`, chưa xóa tham chiếu của dòng đó.

## Hiện trạng đã kiểm (trên DB tạm chạy đủ 89 migration)

Có **5 dòng** `content_assets` chứa `writtings`, không phải 4 như plan cũ. Cả 5 đều lưu **URL đầy đủ** của Supabase, không phải khóa:

| id | Khóa cũ (trong URL) | Có trên Supabase? |
| --- | --- | --- |
| `1c7fbf03-1337-4754-a507-d1e89abe65db` | `writtings/leassons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png` | có |
| `0daa5bcc-248c-5461-6db0-8dedb5468724` | `writtings/leassons/PS-W1-WR-2/ps-w1-wr-2-family-spending.png` | có |
| `d03370cd-2688-2a76-db3e-f18f6f5cd5f6` | `writtings/leassons/PS-W5-WR-1/ps-w5-wr-1-park-visitors.png` | có |
| `fcee2dbb-09bb-27ce-a23a-649525f7766d` | `writtings/leassons/PS-W5-WR-2/ps-w5-wr-2-fuel-prices.png` | có |
| `4dff3a73-31aa-fc4c-be21-d0988f7a041e` | `writtings/leassons/PS-W1-WR-3/ps-w1-wr-3-marriage…png` | **không có** (ảnh đang hỏng) |

Danh sách đầy đủ: `plans/261031-1215-supabase-storage-reorganization/data/phase-06-writtings-url-rows.csv`.

Vì là URL đầy đủ, migration phải **thay chuỗi con** `writtings/leassons/` thành `writing/lessons/` trong `media_reference`, không chỉ thay tiền tố khóa. Query `LIKE 'writtings/leassons/%'` của plan cũ **sẽ không khớp** và phải bỏ.

## Đã chốt

`PS-W1-WR-3` không có file trên bucket. Bạn sẽ đưa file ảnh. Claude upload trước khi Codex merge V91. Codex không xóa tham chiếu của dòng này.

## Việc cần làm

1. Test migration trước (TDD): sau khi áp `V91`:
   - không còn `writtings` trong bất kỳ cột text nào của `content_assets` (test bằng truy vấn trên DB test);
   - 4 dòng có khóa mới trong URL, phần còn lại của URL giữ nguyên.
2. Tạo `services/content-service/src/main/resources/db/migration/V91__writing_chart_keys_renamed.sql`:
   - `UPDATE content_assets SET media_reference = replace(media_reference, 'writtings/leassons/', 'writing/lessons/') WHERE media_reference LIKE '%writtings/leassons/%';`
   - Idempotent. Không đụng các dòng `data:`.
3. Các chỗ khác còn khóa cũ (đã kiểm bằng grep, bỏ `target/`):
   - `scripts/generate-course-writing-charts.py`: đổi đường dẫn sinh khóa sang `writing/lessons/`.
   - `docs/assets/course-writing-charts/upload-urls.csv` và `README.md` cùng thư mục: đổi khóa.
   - `services/content-service/src/test/java/.../LessonPipelineSeedTest.java`: đổi khẳng định khóa.
   - Không sửa `V64`, `V65`, `V67` (đã áp dụng).
4. Chạy test content-service.
5. Chỉ sau khi Claude báo đã xóa khóa cũ trên bucket (bước cuối, Claude làm): cập nhật tài liệu nói khóa cũ đã bỏ.

## Không làm

- Không xóa object cũ trên Supabase (việc của Claude, sau khi kiểm hiển thị từng bài Writing).
- Không sửa migration đã áp dụng.
- Không ghi Supabase.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/content-service -am test
```
Dán số test thật vào PR.

## Tiêu chí xong

- Migration V91 áp sạch; không còn `writtings` trong `content_assets`.
- Bốn bài Writing có biểu đồ hiển thị (kiểm bằng Claude trên dev sau khi deploy).
- Dòng `PS-W1-WR-3` được xử lý theo quyết định của bạn.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Chưa có quyết định về `PS-W1-WR-3`.
- Có cột khác (JSON, bảng khác) chứa `writtings` ngoài `content_assets`: báo danh sách, không tự sửa.
