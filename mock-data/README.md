# Dữ liệu mẫu từ vựng (300 từ)

Dữ liệu mock cho `library_db`: 300 từ IELTS, 302 nghĩa, chia 11 chủ đề. Nội dung do tự soạn, **chưa có người biết
tiếng Anh duyệt** (đặc biệt phiên âm IPA và nghĩa tiếng Việt). Không dùng cho học viên thật trước khi duyệt.

## Các file

| File | Dùng để làm gì |
| --- | --- |
| `vocabulary-mock-300.tables.json` | Hai bảng của từ vựng, đúng tên cột DB: `vocabulary_items` (300 dòng) và `vocabulary_senses` (302 dòng, khóa ngoại `vocabulary_item_id`). Đưa cho agent hoặc script để nạp |
| `vocabulary-mock-300.json` | Dạng lồng nhau (từ chứa danh sách nghĩa), khớp request của API admin. Có thêm trường `topic` chỉ để gom nhóm, DB không lưu |
| `vocabulary-mock-300.csv` | Mỗi dòng một nghĩa, mở bằng Excel để người duyệt đọc và sửa |
| `services/library-service/src/main/resources/db/migration/V3__seed_mock_vocabulary.sql` | Seed Flyway: cách để mọi dev có cùng dữ liệu |

## Để mọi dev có dữ liệu này

Seed nằm trong migration `V3` của library-service. Flyway tự chạy khi service khởi động:

- DB đã có V1, V2: chạy lại `library-service`, Flyway áp V3.
- DB mới (`docker compose down -v` rồi `up`): Flyway áp V1, V2, V3.

Migration an toàn khi chạy lại:

- Id của từ và nghĩa cố định (UUIDv5 theo từ), nên mọi máy đều có cùng id.
- Từ đã có sẵn (cùng `normalized_lemma`, ví dụ thêm qua API admin) thì bỏ qua cả từ lẫn các nghĩa của nó.
- Nghĩa chỉ được thêm nếu từ tương ứng do chính seed tạo ra.

Không sửa file `V3` sau khi đã áp dụng (Flyway kiểm checksum). Muốn đổi dữ liệu thì thêm migration mới.

## Nếu muốn nạp bằng agent hoặc script thay vì Flyway

Dùng `vocabulary-mock-300.tables.json`:

1. Với mỗi dòng của `vocabulary_items`: tạo từ bằng `POST /api/content/admin/vocabulary` (`lemma`, `ipa`).
2. Với mỗi nghĩa trong `vocabulary_senses` có `vocabulary_item_id` trỏ tới từ đó: gọi
   `POST /api/content/admin/vocabulary/{id}/senses` (`partOfSpeech`, `englishDefinition`, `vietnameseMeaning`,
   `exampleSentence`, `sortOrder`).
3. API tự sinh id mới và kiểm trùng từ theo `normalized_lemma`, nên các id trong file bị bỏ qua.

Ghi vào bảng trực tiếp thì phải dùng đúng id trong file, và nạp `vocabulary_items` trước `vocabulary_senses`.

## Chưa có

- `pronunciation_audio_reference` và `image_url` đang để trống.
- `normalized_lemma` chưa có ràng buộc `UNIQUE` ở DB (chỉ có index thường); việc chống trùng đang nằm ở code.
