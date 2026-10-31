# Brief Codex: phase 5, bìa chủ đề và bộ từ vựng

Người làm: **Codex**. Claude review. Không ghi Supabase, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Điều kiện bắt đầu

- Phase 3 đã merge vào `feat/supabase-storage-reorganization` (cần `mediaUploadApi.ts` ở frontend).
- Claude đã xác nhận trong `library_db` mọi `cover_key` hiện có. Nếu có khóa không bắt đầu bằng `catalog-cover/`, dừng và hỏi trước khi siết `CatalogText` (xem bước 4).

## Nhánh và PR

- Backend: `feat/storage-p5-catalog-covers` từ `feat/supabase-storage-reorganization`, PR vào đó.
- Frontend: `feat/storage-p5-catalog-covers` từ `feat/supabase-storage-reorganization` (frontend), PR vào đó.

## Hợp đồng (đã chốt)

- Khóa: `catalog-cover/{userId}/{uuid}.{ext}`, lưu trong bucket `flashcards` (bucket này do `FlashcardImageStorage` dùng sẵn, nên không cần cấu hình thêm).
- Giới hạn: 2 MB; `image/jpeg`, `image/png`, `image/webp`.
- Endpoint: `POST /api/content/admin/vocabulary/covers`, multipart field `file`, quyền `CONTENT_AUTHOR` hoặc `ADMIN` (class đã có `@PreAuthorize`), trả **201**.
- Response: `{"mediaReference": "catalog-cover/...", "mediaUrl": "...", "contentType": "image/webp", "sizeBytes": 12345}`. Cùng hình với phase 3.
- Khóa lưu vào `vocabulary_topics.cover_key` và `vocabulary_sets.cover_key` (cột `TEXT`, không đổi schema).

## Đọc trước (đường dẫn đã kiểm)

- `services/library-service/src/main/java/com/ieltspath/library/application/port/FlashcardImageStorage.java`: `StoredImageResult store(String key, byte[] content, String contentType)`, `String publicUrl(String key)`, `boolean exists(String key)`, `void delete(String key)`. Đọc `StoredImageResult` để biết các trường.
- `.../application/usecase/UploadFlashcardImageUseCase.java`: mẫu `ObjectKeys.forUser` và policy.
- `.../api/controller/AdminVocabularyCatalogController.java` (base `/api/content/admin/vocabulary`, đã có `@PreAuthorize`).
- `.../domain/catalog/CatalogText.java` (`coverKey`, dòng ~30): hiện đã từ chối URL, chỉ nhận khóa.
- `.../domain/catalog/VocabularyTopicDraft.java`, `VocabularySetDraft.java`, `api/dto/request/VocabularyTopicRequest.java`, `VocabularySetRequest.java`.
- `.../infrastructure/adapter/VocabularyCatalogJdbcAdapter.java` dòng 578–592: đọc `cover_key` và gọi `storage.publicUrl(coverKey)`. Không sửa.
- Frontend: `frontend/src/features/admin/vocabulary/pages/AdminVocabularyPage.tsx` (form chủ đề và bộ từ vựng nằm ở đây), `vocabularySchemas.ts`.
- Frontend: `frontend/src/features/admin/media/api/mediaUploadApi.ts` (tạo ở phase 3).

## Backend: file tạo và sửa

Dưới `services/library-service/src/main/java/com/ieltspath/library/`:

| Hành động | File | Nội dung |
| --- | --- | --- |
| Tạo | `application/usecase/UploadCatalogCoverUseCase.java` | policy 2 MB và 3 MIME; `ContentTypes.extensionFor`; `ObjectKeys.forUser("catalog-cover", userId, ext)`; gọi `FlashcardImageStorage.store`; trả khóa và URL |
| Tạo | `api/dto/response/CatalogCoverResponse.java` | record `(mediaReference, mediaUrl, contentType, sizeBytes)` |
| Sửa | `api/controller/AdminVocabularyCatalogController.java` | `@PostMapping(value="/covers", consumes=MULTIPART)`, `@ResponseStatus(CREATED)`; file rỗng → 400 |
| Sửa (có điều kiện) | `domain/catalog/CatalogText.java` | nếu Claude xác nhận mọi khóa hiện có đều là `catalog-cover/...` hoặc rỗng: chỉ chấp nhận `catalog-cover/` và kiểm ký tự như `ObjectKeys`. Nếu chưa xác nhận: để nguyên và báo trong `Concerns`. |

Không thêm migration.

Upload dùng `ContentTypes` và `UploadPolicy` của `shared/common-storage`. Nếu library-service chưa có dependency đó, kiểm `services/library-service/pom.xml` trước (đã dùng `ObjectKeys` nên có sẵn).

## Frontend

- Trong `AdminVocabularyPage.tsx`, form chủ đề và form bộ từ vựng: thêm file picker bìa, xem trước bằng `mediaUrl`, nút xóa (gửi `coverKey: null`).
- Khi chọn file: gọi `uploadAdminImage('/api/content/admin/vocabulary/covers', file)` từ `mediaUploadApi.ts`. Lưu `mediaReference` vào `coverKey`.
- `vocabularySchemas.ts`: thêm `coverKey` (nullable) vào schema của chủ đề và bộ.
- Không đổi bố cục các trường khác của form.

## Test viết trước (TDD)

Backend:
- `UploadCatalogCoverUseCaseTest`: từ chối SVG, >2 MB, rỗng, byte không phải ảnh; chấp nhận jpeg/png/webp; khóa bắt đầu bằng `catalog-cover/{userId}/`.
- Test controller admin từ vựng (đặt cạnh test hiện có của `AdminVocabularyCatalogController`; tìm bằng `grep -rln AdminVocabularyCatalogController services/library-service/src/test`): `/covers` 201; học viên `CUSTOMER` trả 403; file rỗng 400.
- Test `CatalogText`: chấp nhận `catalog-cover/...`; từ chối `https://...`, `../x`, `/x`. Nếu bước siết chưa làm, không viết test siết.
- Test `FlashcardImageUsageService` hoặc test tương đương: khóa `catalog-cover/` không bị coi là ảnh thẻ của học viên.

Frontend:
- `AdminVocabularyPage`: chọn bìa → gọi upload → form giữ `coverKey`; xóa bìa → gửi `null`.
- `vocabularySchemas`: `coverKey` là chuỗi hoặc null.

## Không làm

- Không tạo bucket mới. Không đổi `FlashcardImageStorage`.
- Không viết job dọn ảnh mồ côi (quyết định: để mồ côi).
- Không đổi `VocabularyCatalogJdbcAdapter`.
- Không đổi bìa bộ thẻ cá nhân (`coverImageKey` của `FlashcardDeck`, luồng học viên).

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/library-service -am test
```
Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run src/features/admin/vocabulary`.

Dán số test thật vào PR.

## Tiêu chí xong

- Admin tạo và sửa bìa chủ đề và bộ từ vựng bằng upload; học viên thấy bìa.
- Học viên không gọi được endpoint.
- Test xanh; diff nằm trong danh sách file trên.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Nếu `library_db` có `cover_key` không phải `catalog-cover/`, hoặc không có object tương ứng: báo, không siết.
- Nếu `AdminVocabularyPage.tsx` chưa có form chủ đề hoặc bộ tách riêng: báo số dòng, không tự tách component lớn.
