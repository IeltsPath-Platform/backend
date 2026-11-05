# Audio phát âm cho từ vựng (UK + US)

- Trạng thái: đã sinh và upload đủ 11.456 file (2026-10-31); `V25` (URL) và `V26` (đổi sang key) cùng resolver đã viết,
  test mới đạt; còn khởi động lại library-service để áp V26 rồi thử nghe trên UI
- Chạy lại: `generate-vocabulary-audio.mjs`, `upload-vocabulary-audio.mjs`, `generate-audio-migration.mjs` trong
  `scripts/vocabulary-audio/` (cần `npm install` trong thư mục đó và ffmpeg)
- Phạm vi: toàn bộ `vocabulary_items` của `library_db` (5.728 từ, 11.456 file mp3)

## Hiện trạng (đã kiểm DB local ngày 2026-10-31)

- `audio_uk_reference`, `audio_us_reference` đã có từ V19, đang NULL ở cả 5.728 dòng.
- `pronunciation_audio_reference` (cột cũ) chỉ có 8 dòng (V16, eSpeak US). Notebook, flashcard và Reading lookup chỉ đọc
  cột cũ này; trang chi tiết từ (`WordDetailPage.tsx`) đọc `audioUk`/`audioUs` rồi mới tới cột cũ.
- 330 từ ACTIVE, 5.398 INACTIVE. Tổng 39.820 ký tự, dài nhất 23. 100 từ có dấu `'` (we've, o'clock), 25 cụm từ.
- IPA: `ipa_us` 48 dòng, `ipa_uk` 51, `ipa` cũ 5.261 (kiểu Mỹ). Không đủ để ép phát âm bằng IPA, nên đọc thẳng chữ.
- Bucket `media` public, giới hạn 25 MB, cho `audio/mpeg`. Khóa Supabase trong `backend/.env` đã kiểm hoạt động.
- Không có hai từ nào trùng tên file sau khi chuẩn hóa.

## Quyết định

| Mục | Chọn |
| --- | --- |
| TTS | edge-tts (`msedge-tts` npm), US `en-US-AriaNeural`, UK `en-GB-SoniaNeural`. Không chính thức; bạn đã chấp nhận rủi ro thương mại. Đổi sang Azure Speech sau này thì tạo lại file |
| Văn bản đọc | `lemma` gốc (giữ hoa thường, dấu `'`) |
| Tên file | `normalized_lemma` thay mọi ký tự ngoài `a-z0-9` bằng `-`, ví dụ `green-roof.mp3`, `we-ve.mp3` |
| Đường dẫn bucket | `media/vocabulary/pronunciation/{uk,us}/<tên>.mp3` (cùng quy ước V16) |
| Xử lý file | cắt im lặng đầu/cuối bằng ffmpeg (mẫu thử có hơn 1 giây im lặng ở cuối) |
| Ghi DB | `V25` UPDATE theo công thức từ `normalized_lemma`; `V26` đổi giá trị sang **object key** (`vocabulary/pronunciation/uk/democracy.mp3`) thay vì URL đầy đủ, không sửa migration cũ |
| Ghép URL | `VocabularyAudioUrlResolver` (library-service) dùng `ObjectStorage.publicUrl("media", key)` như `cover_key`, cần `SUPABASE_URL`; thiếu cấu hình thì trả null (frontend dùng giọng đọc của trình duyệt). Giá trị đã là `https://` giữ nguyên. Bucket đổi qua `library.vocabulary-audio.bucket` (mặc định `media`) |
| Cột cũ | Không đụng `pronunciation_audio_reference` (bạn chọn chỉ dùng hai cột UK/US). 8 từ của V16 giữ eSpeak ở cột cũ, giọng mới nằm ở `audio_us_reference` |
| Từ INACTIVE | Làm cả 5.398 từ (bạn đã chọn) |

## Các bước

1. **Xuất danh sách từ.** `docker exec ieltspath-postgres-1 psql ... > words.tsv` (`normalized_lemma`, `lemma`). Script
   nhận file này, không cần mật khẩu DB.
2. **Sinh mp3.** `backend/scripts/vocabulary-audio/generate-vocabulary-audio.mjs`: song song 4 luồng, thử lại có chờ khi
   lỗi, bỏ qua file đã có (chạy lại được khi bị ngắt), cắt im lặng, ghi vào thư mục tạm `.vocabulary-audio-cache/`
   (thêm vào `.gitignore`). Ghi danh sách từ lỗi ra `failures.txt`.
3. **Thử 50 từ** (cả cụm từ và từ có dấu `'`), bạn nghe lại.
4. **Upload.** `upload-vocabulary-audio.mjs`: `POST /storage/v1/object/media/<đường dẫn>` với `x-upsert: true`,
   `Content-Type: audio/mpeg`, đọc `SUPABASE_URL`/`SUPABASE_SECRET_KEY` từ `.env`, không in khóa. Bỏ qua file đã
   upload đủ kích thước. Dùng cùng khóa nên chạy từ máy bạn.
5. **Kiểm.** HEAD ngẫu nhiên 100 URL công khai phải trả 200; số file trong bucket khớp 11.456.
6. **Migration `V25__vocabulary_pronunciation_audio.sql`** (chỉ UPDATE `audio_uk_reference`, `audio_us_reference`) chỉ tạo sau khi bước 5 đạt 100%, để URL không trỏ vào file
   không tồn tại. Nếu còn từ lỗi thì liệt kê và chỉ UPDATE các từ có đủ hai file.
7. Chạy library-service để Flyway áp V25; thử nút UK/US trên trang chi tiết từ, notebook và flashcard.

## Kiểm chứng và rollback

- Test: mở rộng `VocabularySeedMigrationTest` (hoặc test migration tương đương) kiểm hai cột và cột cũ không còn NULL
  cho từ có đủ file, URL bắt đầu bằng `https://`, dài tối đa 500 ký tự (giới hạn của `VocabularyItemDetails`).
- Rollback DB: migration mới đặt hai cột về NULL.
- Rollback file: xóa mọi object dưới `vocabulary/pronunciation/uk/` và `.../us/` (8 file US của V16 bị ghi đè cùng tên,
  cột cũ vẫn trỏ vào đó và sẽ nghe giọng mới; muốn eSpeak lại phải sinh lại).

## Hệ quả đã biết của việc chỉ dùng hai cột

Notebook, flashcard và tra từ trong Reading chỉ đọc `pronunciation_audio_reference` nên **chưa phát được audio mới**;
chỉ trang chi tiết từ (`WordDetailPage.tsx`) phát. Muốn các màn kia phát thì sửa backend/frontend đọc `audioUs` (việc
riêng, chưa nằm trong plan này).

## Rủi ro

- edge-tts không có SLA và có thể giới hạn tốc độ; script thử lại và chạy lại được.
- Từ lạ hoặc viết tắt có thể đọc sai; nghe ngẫu nhiên khi duyệt, từ sai thì ghi đè từng file.
- Khóa Supabase từng xuất hiện trong ảnh gửi qua chat: nên đổi khóa sau khi upload xong.

## Câu hỏi còn mở

- Có sửa các màn notebook/flashcard/Reading để đọc `audioUs` không? (làm sau khi audio đã lên)
