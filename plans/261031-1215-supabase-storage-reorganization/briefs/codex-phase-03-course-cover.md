# Brief Codex: phase 3, upload ảnh bìa khóa học

Người làm: **Codex**. Claude review. Không ghi Supabase, không đọc `.env`.
Kết quả: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, kèm `Summary` và `Concerns`.

## Nhánh và PR

- Backend: nhánh `feat/storage-p3-course-cover` từ `feat/supabase-storage-reorganization`. PR vào `feat/supabase-storage-reorganization`.
- Frontend (repo `frontend/`): nhánh `feat/storage-p3-course-cover` từ `feat/supabase-storage-reorganization` của frontend. Tạo nhánh tích hợp này từ `main` nếu chưa có. PR vào đó.
- Không sửa `docker-compose.yml`, `infra/`, migration có số khác V90.

## Hợp đồng (đã chốt, không đổi)

- Khóa ảnh: `content-image/{userId}/{uuid}.{ext}`, ext là `jpg`, `png` hoặc `webp` theo byte thật.
- Giới hạn: 2 MB; chỉ `image/jpeg`, `image/png`, `image/webp`; SVG bị từ chối.
- DB `courses.cover_image_url` lưu **khóa** (`content-image/...`) hoặc URL `https://` cũ. Content-service resolve khóa thành URL khi trả response.
- Endpoint: `POST /api/content/admin/media/image`, multipart field `file`, quyền `CONTENT_AUTHOR`, trả **201**.
- Response: `{"mediaReference": "content-image/...", "mediaUrl": "https://.../storage/v1/object/public/media/content-image/...", "contentType": "image/webp", "sizeBytes": 12345}`. Cùng hình với `StoredAudioResponse`.
- Nếu `CONTENT_MEDIA_BASE_URL` trống: khóa không resolve được, trả `coverImageUrl = null` và ghi một cảnh báo log. Không làm hỏng cả danh sách.

## Đọc trước (đường dẫn đã kiểm)

- `services/content-service/src/main/java/com/ieltspath/content/api/controller/ContentMediaController.java`
- `.../application/usecase/UploadContentAudioUseCase.java`, `.../infrastructure/storage/ContentAudioStorageAdapter.java`
- `.../domain/aggregate/Course.java` (`normalizeCoverImageUrl`, quanh dòng 131)
- `.../api/dto/request/CreateCourseRequest.java` (dòng 11, `@Pattern`), `.../api/dto/request/UpdateCourseRequest.java`
- `.../api/dto/response/CourseResponse.java`, `.../api/dto/internal/TopicSequenceResponse.java`
- `.../domain/vo/MediaReferencePolicy.java` (đã có `resolve(AssetType, String)`; khóa cần `CONTENT_MEDIA_BASE_URL`)
- `services/learning-service/src/main/java/.../application/usecase/ListCoursesUseCase.java` và `.../api/dto/response/CourseResponse.java`: xác nhận chỉ chuyển tiếp `coverImageUrl`
- Frontend: `frontend/src/features/admin/courses/components/CourseForm.tsx`, `types.ts`; mẫu upload ở `frontend/src/features/admin/packages/api/packageApi.ts` (hàm `uploadAudio`, dòng 82).

## Backend: đổi tên trước (một commit riêng)

Đổi tên cơ học, giữ nguyên hành vi audio. Tám file bị đụng:
`ContentMediaController`, `StoredAudioResponse`, `ContentAudioStorage` → `ContentMediaStorage`, `StoredAudioResult` → `StoredMediaResult`, `UploadContentAudioUseCase`, `ContentAudioStorageAdapter` → `ContentMediaStorageAdapter`, `ContentMediaControllerTest`, `UploadContentAudioUseCaseTest`.
`StoredAudioResponse` và đường dẫn `/audio` **giữ nguyên**. Chạy test content-service, phải xanh trước khi làm tiếp.

## Backend: file tạo và sửa

| Hành động | File (dưới `services/content-service/src/main/java/com/ieltspath/content/`) | Nội dung |
| --- | --- | --- |
| Tạo | `application/usecase/UploadContentImageUseCase.java` | `UploadPolicy.of(2L * 1024 * 1024, "image/jpeg", "image/png", "image/webp")`; `ContentTypes.extensionFor`; `ObjectKeys.forUser("content-image", userId, ext)`; gọi `ContentMediaStorage.store`. Trả `StoredMediaResult`. |
| Tạo | `api/dto/response/StoredImageResponse.java` | record `(mediaReference, mediaUrl, contentType, sizeBytes)`, `from(StoredMediaResult)` |
| Sửa | `api/controller/ContentMediaController.java` | thêm `@PostMapping(value="/image", consumes=MULTIPART)`, `@ResponseStatus(CREATED)`; file rỗng → `UploadRejectedException(EMPTY)` |
| Sửa | `domain/aggregate/Course.java` | `normalizeCoverImageUrl`: nếu bắt đầu bằng `content-image/` thì kiểm khóa: không `..`, không `/` đầu, chỉ `[A-Za-z0-9._/-]`, tối đa 500; nếu không thì giữ kiểm `https?://` như cũ. Từ chối mọi tiền tố khác (ví dụ `media/`). Blank → null. |
| Sửa | `api/dto/request/CreateCourseRequest.java`, `UpdateCourseRequest.java` | `@Pattern` cho phép `https?://\S+` **hoặc** `content-image/[A-Za-z0-9._/-]+`, hoặc rỗng |
| Sửa | nơi dựng `CourseResponse` và `TopicSequenceResponse` (tìm bằng `grep -rn "new CourseResponse\|new TopicSequenceResponse\|CourseResponse.from\|TopicSequenceResponse.from"`) | nếu `coverImageUrl` là khóa: gọi `mediaPolicy.resolve(AssetType.IMAGE, khóa)`; bắt `RuntimeException` → null và log warn. Resolve ở tầng application/adapter, **không** trong domain. |

Không tạo migration. Cột `cover_image_url` đã đủ (V86).

Thêm vào phạm vi (đã chốt 2026-10-31): `services/content-service/src/test/java/com/ieltspath/content/api/controller/ContentAuthorizationWebMvcTest.java`. CourseController nay phụ thuộc MediaReferencePolicy, nên test này cần thêm `@MockitoBean MediaReferencePolicy`. Chỉ thêm bean giả; không đổi assertion, không xoá test, không đổi danh sách test.


## Frontend (repo `frontend/`)

- Tạo `frontend/src/features/admin/media/api/mediaUploadApi.ts`:
  - `uploadAdminImage(endpoint: string, file: File)`: kiểm client (2 MB, png/jpeg/webp) rồi POST multipart `file` tới `endpoint`, parse response bằng zod.
  - Dùng cùng mẫu với `uploadAudio` trong `packageApi.ts`, không copy hằng số: tách hằng số ảnh vào file này.
  - Phase 5 sẽ dùng lại hàm này. Không đổi `packageApi.ts`.
- Sửa `CourseForm.tsx`: thay ô nhập URL bằng file picker (`accept="image/png,image/jpeg,image/webp"`), xem trước, nút xóa ảnh. Khi chọn file: gọi `uploadAdminImage('/api/content/admin/media/image', file)` và lưu `mediaReference` vào form. Nếu khóa cũ là `https://` thì hiển thị URL đó và cho phép thay.
- Sửa `types.ts` cho khớp.
- Xóa ảnh: gửi `coverImageUrl: null`.

## Test viết trước (TDD)

Backend:
- `UploadContentImageUseCaseTest`: từ chối SVG (cả khai báo lẫn byte), từ chối >2 MB, từ chối rỗng, từ chối byte không phải ảnh dù khai `image/png`; chấp nhận jpeg/png/webp thật; khóa bắt đầu bằng `content-image/{userId}/`.
- `ContentMediaControllerTest`: `/image` trả 201 đúng hình; file rỗng trả 400.
- `ContentMediaAuthorizationTest`: `/image` trả 403 với người không phải `CONTENT_AUTHOR`.
- Test domain `Course` (tạo `CourseTest` nếu chưa có): chấp nhận khóa `content-image/...`; từ chối `content-image/../x`, `/content-image/x`, `media/x`, `javascript:...`; giữ chấp nhận `https://`; blank → null.
- Test controller khóa học (`CourseControllerTest` đã có): request với khóa hợp lệ được nhận; khóa sai bị 400.
- Test response: khóa → URL đầy đủ có base; URL `https://` cũ giữ nguyên; base trống → null, danh sách vẫn trả 200.

Frontend:
- `mediaUploadApi`: từ chối SVG và file >2 MB ở client; gọi đúng endpoint; parse lỗi 400.
- `CourseForm`: chọn file hợp lệ → gọi upload → form giữ `mediaReference`; xóa ảnh → gửi `null`.

## Không làm

- Không đổi `docker-compose.yml`, `infra/config-server/`.
- Không đổi `MediaReferencePolicy` (phase 7 sẽ sửa).
- Không tự tải ảnh của bên thứ ba về bucket.
- Không xóa object cũ khi thay ảnh (quyết định: để mồ côi).
- Không ghi comment hay tên test có số phase, mã plan.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/content-service -am test
mvn -B -ntp -pl services/learning-service -am test   # chỉ để xác nhận không lỗi, nếu không đổi learning
```
Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run src/features/admin/courses src/features/admin/media`.

Dán số test thật (chạy, lỗi, bỏ qua) vào mô tả PR. Không ghi "đã chạy" cho lệnh chưa chạy.

## Tiêu chí xong

- Upload ảnh hợp lệ trả 201; object sẽ nằm dưới `content-image/` trong `media` (kiểm bằng Claude sau khi merge, không tự ghi Supabase).
- Tạo và sửa khóa học bằng upload; learner nhận URL đầy đủ.
- Test xanh; diff chỉ nằm trong danh sách file trên.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- Nếu chỗ dựng `CourseResponse` không khớp mô tả, hoặc learning-service có kiểm `http(s)` lại: dừng, báo file và dòng.
- Nếu test cũ của `Course` hoặc `CourseControllerTest` mâu thuẫn với hành vi mới: báo, không xóa test.
