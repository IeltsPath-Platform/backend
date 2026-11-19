# Đề placement theo kho: hướng dẫn thu âm, duyệt và upload

Năm đề mới (cùng đề `PLACEMENT-4SKILLS` có sẵn ở kho `BAND_5_9`) giúp mỗi kho có hai đề. Các file trong thư mục này được sinh từ `scripts/placement-exams/exams/` bằng `node scripts/placement-exams/generate-placement-seed.mjs`; **sửa file nguồn JSON rồi chạy lại generator**, đừng sửa tay (`--check` sẽ báo lệch).

## Các đề

| Mã gói | Kho | Dạng | Số file audio | Tổng thời lượng audio | Tài liệu |
| --- | --- | --- | --- | --- | --- |
| `PLACEMENT-B03-A` | BAND_0_3 | rút gọn | 2 | 211 giây | [Kịch bản thu âm](PLACEMENT-B03-A-recording.md) · [Bản duyệt](PLACEMENT-B03-A-review.md) |
| `PLACEMENT-B03-B` | BAND_0_3 | rút gọn | 2 | 193 giây | [Kịch bản thu âm](PLACEMENT-B03-B-recording.md) · [Bản duyệt](PLACEMENT-B03-B-review.md) |
| `PLACEMENT-B35-A` | BAND_3_5 | rút gọn | 2 | 269 giây | [Kịch bản thu âm](PLACEMENT-B35-A-recording.md) · [Bản duyệt](PLACEMENT-B35-A-review.md) |
| `PLACEMENT-B35-B` | BAND_3_5 | rút gọn | 2 | 258 giây | [Kịch bản thu âm](PLACEMENT-B35-B-recording.md) · [Bản duyệt](PLACEMENT-B35-B-review.md) |
| `PLACEMENT-B59-B` | BAND_5_9 | đủ dài | 4 | 641 giây | [Kịch bản thu âm](PLACEMENT-B59-B-recording.md) · [Bản duyệt](PLACEMENT-B59-B-review.md) |

Tổng cộng 12 file audio.

## Thang độ khó theo kho (hướng dẫn soạn đề, chưa hiệu chỉnh)

| Kho | Trần band | Trình độ | Lời nói | Văn bản |
| --- | --- | --- | --- | --- |
| `BAND_0_3` | 3.0 | CEFR A1-A2 | Câu ngắn, chủ đề đời sống hằng ngày, nói chậm và rõ | Câu ngắn, từ vựng thông dụng |
| `BAND_3_5` | 5.0 | CEFR A2-B1 | Chủ đề quen thuộc, tốc độ vừa | Đoạn ngắn, có vài từ mới suy ra được từ ngữ cảnh |
| `BAND_5_9` | 9.0 | CEFR B1-C2 | Tốc độ tự nhiên | Văn bản học thuật, ý trừu tượng |

Đây chỉ là thang hướng dẫn cho người soạn và người duyệt, không phải kết quả hiệu chỉnh: chưa có dữ liệu học viên thật. Hai tuần đầu nên theo dõi phân bố band theo kho.

## Yêu cầu thu âm

- Định dạng **mp3**, 44.1 kHz trở lên, **128 kbps trở lên**; mono hoặc stereo đều được.
- Phòng yên tĩnh, không nhạc nền, không tạp âm. Đầu file 1 giây im lặng, cuối file dưới 1 giây.
- Mỗi nhân vật một giọng khác nhau (xem cột Giọng trong kịch bản của từng đề). Độc thoại thì một người.
- Tên người nói in đậm chỉ để biết ai nói, **không đọc ra**. Không đọc số part hay hướng dẫn câu hỏi: file chỉ chứa lời thoại.
- Đọc **đúng nguyên văn** từng chữ; đánh vần từng chữ cái khi lời có đánh vần (ví dụ "H, A, R, P, E, R"), ngắt rõ. Số và giờ phải nghe rõ.
- Tốc độ: theo số từ mỗi phút ghi trong kịch bản của từng đề. Thời lượng thật phải nằm trong ±15% thời lượng khai báo.

## Upload

1. **Quy ước đặt tên.** Mỗi đề có một thư mục riêng tên là mã gói viết thường; trong đó là các file `part1.mp3`, `part2.mp3`, ... theo thứ tự part trong kịch bản. Tên file nằm trong thư mục, nên 5 đề không bị trùng tên khi lưu cùng một chỗ. Thư mục và tên file **chính là phần cuối của key** trên bucket `media` (biến `CONTENT_MEDIA_BUCKET`): giữ nguyên thư mục khi upload (key đầy đủ có tiền tố `listening/placement/`). Key sai thì Listening báo `INVALID_MEDIA_REFERENCE` cho mọi học viên được giao đề đó. Chỉ chữ thường, không dấu cách, đuôi `.mp3`.

   ```
   placement-b03-a/part1.mp3   placement-b03-a/part2.mp3
   placement-b03-b/part1.mp3   placement-b03-b/part2.mp3
   placement-b35-a/part1.mp3   placement-b35-a/part2.mp3
   placement-b35-b/part1.mp3   placement-b35-b/part2.mp3
   placement-b59-b/part1.mp3   part2.mp3   part3.mp3   part4.mp3
   ```

   **Để file ở đâu:** trong `services/content-service/media/listening/placement/`, giữ nguyên thư mục con như trên (thư mục `media/` nằm trong `.gitignore`, không bị commit). Ví dụ `services/content-service/media/listening/placement/placement-b03-a/part1.mp3`. Thư mục con đã được tạo sẵn.

2. **Upload** bằng script (cần `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, và `CONTENT_MEDIA_BUCKET` nếu bucket không phải `media`; script tự đọc từ `.env` ở thư mục gốc nếu biến chưa được đặt, không in ra):

   ```
   powershell -File scripts/placement-exams/upload-placement-audio.ps1 -DryRun   # liệt kê file có và thiếu, không gọi Supabase
   powershell -File scripts/placement-exams/upload-placement-audio.ps1           # upload; không ghi đè object đã có
   ```

   Script chỉ upload khi đủ 12 file, mỗi file dài hơn 1 KB. Thu lại một file đã upload thì thêm `-Overwrite`. **Không cần chèn gì vào database**: các migration `V138` đến `V140` đã ghi sẵn key vào `content_assets.media_reference`; Content ghép key với `CONTENT_MEDIA_BASE_URL` để ra URL, và database không bao giờ lưu URL hay file.

3. Kiểm bằng generator (cần `CONTENT_MEDIA_BASE_URL`, là https của bucket):

   ```
   node scripts/placement-exams/generate-placement-seed.mjs --audio "$CONTENT_MEDIA_BASE_URL"
   ```

   Lệnh kiểm từng key: HTTP 200, content type `audio/*`, thời lượng trong ±15% khai báo. Lệnh thoát khác 0 nếu có file lỗi.
4. Chỉ deploy migration Content khi lệnh ở bước 3 báo đủ 12 file đạt.

| File khi thu (thư mục/tên file) | Key trên bucket | Thời lượng khai báo |
| --- | --- | --- |
| `placement-b03-a/part1.mp3` | `listening/placement/placement-b03-a/part1.mp3` | 98 giây |
| `placement-b03-a/part2.mp3` | `listening/placement/placement-b03-a/part2.mp3` | 113 giây |
| `placement-b03-b/part1.mp3` | `listening/placement/placement-b03-b/part1.mp3` | 105 giây |
| `placement-b03-b/part2.mp3` | `listening/placement/placement-b03-b/part2.mp3` | 88 giây |
| `placement-b35-a/part1.mp3` | `listening/placement/placement-b35-a/part1.mp3` | 130 giây |
| `placement-b35-a/part2.mp3` | `listening/placement/placement-b35-a/part2.mp3` | 139 giây |
| `placement-b35-b/part1.mp3` | `listening/placement/placement-b35-b/part1.mp3` | 116 giây |
| `placement-b35-b/part2.mp3` | `listening/placement/placement-b35-b/part2.mp3` | 142 giây |
| `placement-b59-b/part1.mp3` | `listening/placement/placement-b59-b/part1.mp3` | 137 giây |
| `placement-b59-b/part2.mp3` | `listening/placement/placement-b59-b/part2.mp3` | 157 giây |
| `placement-b59-b/part3.mp3` | `listening/placement/placement-b59-b/part3.mp3` | 182 giây |
| `placement-b59-b/part4.mp3` | `listening/placement/placement-b59-b/part4.mp3` | 165 giây |

## Duyệt nội dung (trước khi merge)

Mỗi đề cần một người có chuyên môn IELTS ký duyệt bản duyệt (`<mã gói>-review.md`): độ khó đúng kho, đáp án đúng và duy nhất, không mơ hồ, đề Writing và Speaking phù hợp. Đề được xuất bản ngay khi migration chạy nên không có vòng duyệt sau deploy. Ghi người duyệt và ngày trong mô tả PR.
