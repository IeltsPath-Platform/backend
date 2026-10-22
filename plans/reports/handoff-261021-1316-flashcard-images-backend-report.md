# Prompt cho agent BE: ảnh flashcard, ví dụ, ảnh bìa bộ thẻ, số thẻ trong bộ

Dán nguyên phần dưới đường kẻ cho agent BE.

---

## Nhiệm vụ

Mở rộng flashcard của `services/library-service` để FE (đã làm xong ở `FE/frontend/src/features/flashcards/`) dùng được:
ảnh minh họa cho thẻ, câu ví dụ nhập tay, ảnh bìa cho bộ thẻ (deck = chủ đề), số thẻ và số thẻ đến hạn của mỗi bộ.
Ảnh lưu trên Supabase Storage qua `shared/common-storage`, giống cách `community-service` upload ảnh bài viết.

Đọc trước khi làm: `AGENTS.md`, `CLAUDE.md`, `services/library-service/README.md`, `docs/contracts/flashcard-srs-v1.md`,
`shared/common-storage` (`ObjectStorage`, `ObjectKeys`, `UploadPolicy`, `ContentTypes`), và mẫu tham khảo trong community:
`UploadCommunityImageUseCase`, `CommunityStorageAdapter`, `CommunityMediaController`, `infra/config-server/config-repo/community-service.yaml` (khối `app.storage`, `spring.servlet.multipart`).

## Hiện trạng (đã kiểm)

- `FlashcardController` `/api/learning-support/flashcards`: POST, GET (page), GET `/due`, POST `/{id}/reviews`, GET/PUT/DELETE `/{id}`.
- `FlashcardDeckController` `/api/learning-support/decks`: CRUD + `/{deckId}/items` (POST/GET/DELETE).
- Bảng `flashcards`, `flashcard_decks`, `flashcard_deck_items` ở `V2__create_personal_library.sql`; migration mới nhất là `V14`.
- `FlashcardResponse.exampleSentence` hiện chỉ lấy từ metadata từ vựng (`FlashcardVocabularyMetadataReader`).
- Gateway đã route `/api/learning-support/flashcards/**` và `/decks/**` tới library-service, không cần sửa Gateway.
- library-service chưa phụ thuộc `common-storage`.

## Supabase (người dùng tự tạo trên dashboard)

- Bucket: `flashcards`, **Public**, giới hạn 2 MB, MIME `image/png, image/jpeg, image/webp`.
- Object key do server sinh: `ObjectKeys.forUser("images", userId, ext)` → `images/{userId}/{uuid}.{ext}`.
- URL công khai: `{SUPABASE_URL}/storage/v1/object/public/flashcards/images/{userId}/{uuid}.webp` (lấy bằng `ObjectStorage.publicUrl`, không tự ghép chuỗi).
- Không tạo policy RLS cho client: chỉ server ghi bằng `SUPABASE_SECRET_KEY`.

## Yêu cầu

### 1. Cấu hình

- `services/library-service/pom.xml`: thêm dependency `common-storage` (như community-service).
- `infra/config-server/config-repo/library-service.yaml`:
  - `app.storage.enabled: true`, `app.storage.supabase.url: ${SUPABASE_URL:}`, `app.storage.supabase.secret-key: ${SUPABASE_SECRET_KEY:}` (chép đúng khối của community).
  - `library.flashcard-images.bucket: ${LIBRARY_FLASHCARD_BUCKET:flashcards}`.
  - `spring.servlet.multipart.max-file-size: 2MB`, `max-request-size: 3MB`.
- Không sửa giá trị fallback secret có sẵn trong config-repo; nếu thấy thì báo lại.

### 2. Upload ảnh

- `POST /api/learning-support/flashcards/images`, `multipart/form-data`, field `file`.
- 201 `{ "key": "images/{userId}/{uuid}.png", "url": "https://…/object/public/flashcards/images/…", "contentType": "image/png", "size": 12345 }`.
- `UploadPolicy.of(2 MB, "image/png", "image/jpeg", "image/webp")`: kiểm bằng sniff nội dung, không tin `Content-Type` của client.
- Lỗi: sai loại/rỗng → 400; quá cỡ → 413 (map `MaxUploadSizeExceededException` trong `GlobalExceptionHandler`); storage lỗi → 503. Body theo `ErrorResponse` hiện có.
- Cấu trúc: port `application/port/FlashcardImageStorage` (`store`, `publicUrl`, `exists`, `delete`), adapter `infrastructure/adapter/FlashcardImageStorageAdapter` dùng `ObjectStorage` (xử lý `outcomeUnknown` như `CommunityStorageAdapter`), use case `UploadFlashcardImageUseCase`.
- Không log key/URL ảnh kèm nội dung thẻ; chỉ log id và mã lỗi.

### 3. Migration `V15__flashcard_images_and_examples.sql`

```sql
ALTER TABLE flashcards ADD COLUMN image_key varchar(300);
ALTER TABLE flashcards ADD COLUMN example_sentence varchar(1000);
ALTER TABLE flashcard_decks ADD COLUMN cover_image_key varchar(300);
```

Lưu **key**, không lưu URL: URL tính lúc trả response, đổi bucket/host không cần migrate dữ liệu.

### 4. Domain và use case

- `Flashcard`: thêm `imageKey`, `exampleSentence` (optional, tối đa 1000, trim, rỗng → null). Đi qua `create`/`update`; `PRACTICE_QUESTION` vẫn cho phép, `SavePracticeQuestionFlashcardUseCase` truyền null.
- `FlashcardDeck`: thêm `coverImageKey`.
- Kiểm key trong use case (không trong domain): nếu key khác null **và khác key đang lưu** thì `ObjectKeys.requireOwnedBy(key, "images", userId)` và `storage.exists(key)`; sai → 400 `INVALID_IMAGE_KEY`. Key cũ giữ nguyên thì không gọi mạng.
- Khi ảnh bị thay/bỏ, hoặc thẻ/bộ bị xóa: xóa object cũ **sau khi commit** (`TransactionSynchronization.afterCommit` hoặc `@TransactionalEventListener(AFTER_COMMIT)`), best-effort, lỗi chỉ log mã lỗi.
- Ảnh đã upload nhưng không gắn vào thẻ nào sẽ thành rác; ghi vào phần "chưa làm" của README, không cần job dọn ở task này.

### 5. Thay đổi API (chỉ thêm field, không đổi status/method)

| DTO | Thêm |
| --- | --- |
| `CreateFlashcardRequest`, `UpdateFlashcardRequest` | `imageKey` (`@Size(max=300)`), `exampleSentence` (`@Size(max=1000)`); PUT là thay toàn bộ: `imageKey = null` nghĩa là bỏ ảnh |
| `CreateDeckRequest`, `UpdateDeckRequest` | `coverImageKey` (`@Size(max=300)`) |
| `FlashcardResponse` | `imageKey`, `imageUrl`; `exampleSentence` = giá trị của thẻ, nếu null thì giữ fallback metadata từ vựng như hiện nay |
| `FlashcardDeckResponse` | `coverImageKey`, `coverImageUrl`, `cardCount`, `dueCount` |
| `DeckItemResponse` | `imageUrl`, `exampleSentence`, `ipa`, `leitnerBox` (màn duyệt bộ thẻ theo Figma cần ảnh, IPA, ví dụ và lọc "Đã thuộc" = `leitnerBox >= 4`); `ipa`/ví dụ fallback lấy metadata từ vựng gom theo tập sense id như `GetDueFlashcardsUseCase`, không gọi trong vòng lặp |

- `cardCount`/`dueCount`: một query gom theo `deck_id IN (:ids)` cho cả trang deck (thẻ `ACTIVE`; đến hạn khi `next_review_at IS NULL OR next_review_at <= now()`, cùng điều kiện với `findDueCards`). Không gọi repository trong vòng lặp (AGENTS §3.7).
- Tùy chọn nếu còn thời gian: `GET /flashcards?q=` lọc `front/back ILIKE` ở DB (FE hiện chỉ lọc trong trang đã tải).

### 6. Contract và tài liệu

- Cập nhật `docs/contracts/flashcard-srs-v1.md` (hoặc tạo `flashcard-images-v1.md`) với endpoint upload, các field mới, mã lỗi.
- `services/library-service/README.md`: bucket `flashcards`, biến `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `LIBRARY_FLASHCARD_BUCKET`.
- `CLAUDE.md` §2/§5 nếu cần ghi library-service dùng storage.

### 7. Test

- Mockito: `UploadFlashcardImageUseCase` (đúng loại, sai loại, quá cỡ), create/update thẻ với key của user khác → 400, key không tồn tại → 400, key không đổi → không gọi `exists`, thay ảnh → xóa key cũ sau commit.
- `@WebMvcTest`: upload multipart 201/400/413, request không đăng nhập → 401.
- Testcontainers: migration V15, lưu/đọc `image_key`, `example_sentence`, `cover_image_key`; query đếm thẻ của nhiều deck trong một lần.
- Không gọi Supabase thật: mock `ObjectStorage`.
- Chạy: `mvn -q -pl services/library-service -am test` và `mvn -q compile -DskipTests`. Báo rõ nếu test Testcontainers bị skip vì thiếu Docker.

### Ràng buộc

- Giữ layer `api → application → domain`; domain không import Spring/JPA/storage.
- Không sửa migration cũ; không đổi route/method/status hiện có.
- Commit theo conventional commit, không nhắc AI, không ghi mã plan trong code/commit.

### Tiêu chí xong

1. Upload ảnh qua Gateway `POST /api/learning-support/flashcards/images` trả `key` + `url` mở được trên trình duyệt.
2. Tạo/sửa thẻ với `imageKey`, `exampleSentence`; GET trả `imageUrl`.
3. Tạo/sửa deck với `coverImageKey`; danh sách deck có `coverImageUrl`, `cardCount`, `dueCount`.
4. Thay hoặc bỏ ảnh thì object cũ bị xóa khỏi bucket.
5. Toàn bộ test library-service xanh.

Kết thúc bằng:

```
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
Summary: …
Concerns/Blockers: …
```
