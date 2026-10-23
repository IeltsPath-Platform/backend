# Phase 01: Hoàn thiện API soạn gói đề

Người làm: **Claude Code** (tiếp quản từ Codex ngày 2026-10-23). Nhánh: `feat/content-package-authoring` (backend, nhánh Codex đã tạo).

Tiếp quản: Codex đã tạo nhánh từ `origin/main` `c600b3f3`, viết dở `docs/contracts/content-package-authoring-v1.md` và tạo `application/port/ContentAudioStorage.java`, chưa commit (xem mục "Kết quả" cuối file). Đọc hai file đó, bổ sung contract mục 7, rồi làm tiếp; không viết lại từ đầu.
Trạng thái: Done về phía backend (2026-10-23, chưa commit/merge; xem mục "Kết quả")

Repo: `backend`, service `content-service`. Phụ thuộc: không.

## Mục tiêu

Tác giả soạn được trọn một gói đề qua API: tạo gói (kể cả đề cuối `TOPIC_TEST`/`COURSE_TEST`), chọn version, thêm/sửa/xóa section, gắn câu hỏi, đặt đoạn văn và audio. Mọi thao tác ghi chỉ được làm trên version `DRAFT`. Các use case ở đây cũng là đường ghi duy nhất mà phase 10–12 (dựng course, import Excel) dùng lại.

## Đọc trước

- `api/controller/ContentPackageController.java`, `api/controller/ContentPackageAdminController.java`, `api/controller/ContentAssetController.java`
- `application/usecase/AddContentSectionUseCase.java`, `GetContentPackageDetailUseCase.java`, `GetPackageVersionPreviewUseCase.java`, `PublishContentPackageUseCase.java`
- `domain/entity/ContentSection.java`, `SectionQuestion.java`, `ContentPackageVersion.java`, `ContentPackage.java`
- `infrastructure/persistence/adapter/JdbcLearningContentReader.java` (cách đọc passage/audio: asset `PASSAGE`/`AUDIO` đầu tiên link vào section, sắp theo `content_asset_links.sort_order`)
- `docs/contracts/content-scheduled-publish-v1.md` (preview, schedule đã có)
- `api/exception/GlobalExceptionHandler.java` (`InvalidContentStateException` → 400; các mã 422 qua `details.code`)

## Contract mới: `docs/contracts/content-package-authoring-v1.md`

Viết file này **trước khi code**. Tất cả route yêu cầu `CONTENT_AUTHOR`. Gói hoặc section không tồn tại → `404`. Version không thuộc gói, hoặc section không thuộc version → `400`. Ghi vào version không phải `DRAFT` → `400` với `details.code = "VERSION_NOT_DRAFT"` (thêm mã này vào handler; giữ `InvalidContentStateException` làm lớp lỗi).

### 1. Chi tiết gói cho tác giả

`GET /api/content/admin/packages/{packageId}`

```json
{
  "id": "…", "code": "R55_PRACTICE_1", "title": "…", "packageType": "PRACTICE_SET",
  "accessLevel": "FREE", "status": "PUBLISHED", "currentPublishedVersionId": "…",
  "lessonId": "…|null", "sourceCode": "…|null", "createdAt": "…", "updatedAt": "…",
  "versions": [
    {
      "id": "…", "versionNumber": 2, "status": "DRAFT", "rulesJson": "{}",
      "publishedAt": null, "publishAt": "2026-10-30T02:00:00Z|null", "lastPublishError": null,
      "sections": [
        {
          "id": "…", "title": "Reading Passage 1", "skill": "READING", "sortOrder": 1,
          "timeLimitSeconds": 1200, "partNo": 1, "instructions": "…",
          "passage": "…|null",
          "audio": { "assetId": "…", "mediaReference": "listening/demo/ls1.mp3", "mediaUrl": "https://…", "durationSeconds": 45, "transcript": "…|null" } ,
          "questions": [
            { "id": "…", "questionVersionId": "…", "questionId": "…", "sortOrder": 1, "maxScore": 1.0,
              "stem": "…", "status": "PUBLISHED", "purpose": "LEARNING" }
          ]
        }
      ]
    }
  ]
}
```

- `versions` sắp theo `versionNumber` tăng dần. `audio` là `null` khi chưa có. `mediaUrl` resolve bằng cùng `mediaPolicy` mà preview đang dùng.
- `questions[].stem` cắt tối đa 200 ký tự (để hiển thị danh sách, không phải nội dung đầy đủ). `status` và `purpose` lấy từ `question_versions`/`questions` để màn hình cảnh báo câu `DRAFT` hoặc sai mục đích trước khi publish.
- Một query cho cả gói (không N+1): đọc version, section, section_questions join question_versions/questions, asset passage/audio theo lô.

`GET /api/content/admin/packages?type=&status=&lessonId=&q=` — danh sách cho tác giả, trả mảng phần tử như `ContentPackageResponse` hiện có cộng `latestVersionNumber` và `hasDraft` (boolean). `q` lọc theo `code` hoặc `title` (ILIKE). Không phân trang ở v1 (số gói nhỏ); giới hạn 500 bản ghi, sắp `updatedAt` giảm dần.

### 2. Version

`POST /api/content/admin/packages/{packageId}/versions` — tạo bản nháp mới.

```json
{ "rulesJson": "{}" }
```

Server tự lấy `versionNumber = max + 1`. Nếu gói đã có một version `DRAFT` → `409` với `details.code = "DRAFT_EXISTS"` (mỗi gói tối đa một bản nháp). Trả `201` với version vừa tạo (cùng shape phần tử `versions[]`, `sections: []`).

Endpoint cũ `POST /api/content/packages/{id}/versions` giữ nguyên hành vi.

### 3. Section (chỉ version DRAFT)

- `POST /api/content/admin/packages/{packageId}/versions/{versionId}/sections` — body như `AddContentSectionRequest` hiện có (`title`, `skill`, `sortOrder`, `timeLimitSeconds`, `instructions`, `partNo`, `coverAssetId`). `201`.
- `PUT …/sections/{sectionId}` — cùng body. `200`, trả section.
- `DELETE …/sections/{sectionId}` — xóa section, các `section_questions` và `content_asset_links` của nó (asset giữ nguyên). `204`.
- `PUT …/sections/order` — `{ "sectionIds": ["…", "…"] }`, phải đủ và đúng tập section của version, nếu không `400`. Ghi lại `sort_order` 1..n. `204`.

Sửa thêm: `AddContentSectionUseCase` (route cũ `POST /api/content/packages/versions/{versionId}/sections`) cũng phải từ chối version không `DRAFT` (lỗ hổng hiện tại).

### 4. Câu hỏi trong section (chỉ version DRAFT)

- `POST …/sections/{sectionId}/questions` — `{ "questionVersionId": "…", "maxScore": 1.0 }`. Thêm vào cuối (`sortOrder = max + 1`). Câu đã có trong **cùng version** → `409` `details.code = "QUESTION_ALREADY_IN_VERSION"`. `questionVersionId` không tồn tại → `404`. Không kiểm purpose/đã dùng ở đây; publish đã kiểm (`QUESTION_PURPOSE_MISMATCH`, `QUESTION_ALREADY_USED`). `maxScore` mặc định 1.0, phải > 0. `201`, trả phần tử `questions[]`.
- `DELETE …/sections/{sectionId}/questions/{sectionQuestionId}` — `204`. Đánh lại `sort_order` liên tục.
- `PUT …/sections/{sectionId}/questions/order` — `{ "sectionQuestionIds": [...] }`, phải đủ và đúng tập. `204`.

### 5. Đoạn văn và audio của section (chỉ version DRAFT)

- `PUT …/sections/{sectionId}/passage` — `{ "text": "…" }` (không rỗng, tối đa 50 000 ký tự). Trong một transaction: tạo asset `PASSAGE` mới, gỡ mọi link `PASSAGE` cũ của section, link asset mới `sort_order = 0`. `200`, trả `{ "passage": "…" }`.
- `DELETE …/sections/{sectionId}/passage` — gỡ link `PASSAGE`. `204`.
- `PUT …/sections/{sectionId}/audio` — `{ "mediaReference": "listening/demo/ls1.mp3", "durationSeconds": 45, "transcript": "…|null" }`. `mediaReference` kiểm bằng `ObjectKeys.requireSafe` của `shared/common-storage` (dependency thêm ở mục 6), gọi từ tầng application, không import vào domain. `durationSeconds` 1..7200. Nhận cả key do mục 6 trả về lẫn key file seed có sẵn. Tạo asset `AUDIO` (`text_content = transcript`), gỡ link `AUDIO` cũ, link mới. `200`, trả object `audio` như ở mục 1.
- `DELETE …/sections/{sectionId}/audio` — `204`.

Không xóa bản ghi `content_assets` cũ (có thể đang được version đã xuất bản dùng).

### 6. Upload file audio

`POST /api/content/admin/media/audio` — `multipart/form-data`, field `file`. Không gắn với gói hay section: chỉ lưu file và trả key, rồi màn soạn gọi `PUT …/sections/{sectionId}/audio` với key đó.

```json
{ "mediaReference": "content-audio/2f0c…/9c2e….mp3", "mediaUrl": "https://…/storage/v1/object/public/media/content-audio/…", "contentType": "audio/mpeg", "sizeBytes": 2483021 }
```

- Kiểm bằng `UploadPolicy.of(MAX, "audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg")` của `common-storage` (kiểm magic bytes, không tin `Content-Type` của client). Giới hạn **25 MB**; đặt `spring.servlet.multipart.max-file-size: 25MB`, `max-request-size: 26MB` trong `infra/config-server/config-repo/content-service.yaml`.
- Key: `ObjectKeys.forUser("content-audio", userId, ext)` (userId lấy từ `CurrentUserProvider` của `common-security`), bucket cấu hình `content.media.bucket: ${CONTENT_MEDIA_BUCKET:media}`.
- Thời lượng **không** đọc ở server (không thêm thư viện xử lý audio). Frontend đọc từ thẻ `<audio>` (`loadedmetadata`) rồi gửi trong `PUT …/audio`.
- Lỗi: file rỗng `400`; sai định dạng `400` (`details.code = "UNSUPPORTED_AUDIO"`); quá lớn `413`; storage không cấu hình hoặc lỗi `503` (`details.code = "STORAGE_UNAVAILABLE"`). Ánh xạ `UploadRejectedException`, `MaxUploadSizeExceededException`, `StorageException` trong `GlobalExceptionHandler` theo cách library-service đã làm. Log lỗi storage chỉ ghi `operation` và `reason`, không ghi key hay tên file.
- File tải lên nhưng không được gắn vào section nào thì để lại (chưa dọn trong plan này; ghi vào backlog của contract).

Thay đổi cấu hình kèm theo (đều là cần thiết cho tính năng, ghi rõ trong PR vì `AGENTS.md` hạn chế sửa cấu hình như tác dụng phụ):

- `services/content-service/pom.xml`: thêm `com.ieltspath:common-storage` `${project.version}`.
- `config-repo/content-service.yaml`: `app.storage.enabled: true`, `app.storage.supabase.url: ${SUPABASE_URL:}`, `app.storage.supabase.secret-key: ${SUPABASE_SECRET_KEY:}`, `content.media.bucket`, multipart như trên.
- `docker-compose.yml` service `content-service`: `SUPABASE_URL: "${SUPABASE_URL:-}"`, `SUPABASE_SECRET_KEY: "${SUPABASE_SECRET_KEY:-}"` (cả hai mặc định rỗng).
- `services/content-service/README.md`: ghi biến môi trường cần thiết và bucket.

Mẫu code để chép: `services/library-service/.../application/usecase/UploadFlashcardImageUseCase.java`, `.../application/port/FlashcardImageStorage.java`, `.../infrastructure/adapter/FlashcardImageStorageAdapter.java`, phần upload trong `FlashcardController.java` và `GlobalExceptionHandler.java` của library-service. Port đặt ở `application/port/` của content-service; domain không biết tới storage.

### 7. Tạo gói, kể cả đề cuối topic/course

`POST /api/content/admin/packages`, body:

```json
{ "code": "X5", "title": "…", "packageType": "TOPIC_TEST", "accessLevel": "FREE",
  "lessonId": null, "sourceCode": null, "topicId": "…|null", "courseId": "…|null" }
```

- `packageType` thuộc `PRACTICE_SET`, `PRACTICE_TEST`, `MOCK_TEST`, `PLACEMENT_TEST`, `QUIZ`, `TOPIC_TEST`, `COURSE_TEST` (không `LESSON`).
- `TOPIC_TEST`: `topicId` bắt buộc và topic phải tồn tại; `lessonId`, `courseId`, `sourceCode` phải null. Sai → `400` `details.code = "INVALID_PACKAGE_TOPIC"`.
- `COURSE_TEST`: `courseId` bắt buộc và course phải tồn tại; `lessonId`, `topicId`, `sourceCode` phải null. Sai → `400` `details.code = "INVALID_PACKAGE_COURSE"`.
- Loại khác: `topicId`, `courseId` phải null; `lessonId`, `sourceCode` theo luật `CreateContentPackageUseCase` hiện có (`sourceCode` chỉ cho `PRACTICE_TEST`, tùy chọn, có thì phải là `TestSource` hợp lệ).
- Mã trùng → `409` như route cũ. Không tự tạo version; màn gọi tiếp `POST …/{packageId}/versions`. Trả `201` cùng shape chi tiết ở mục 1 (`versions: []`).
- Topic/course của đề cuối không đổi được sau khi tạo (v1, backlog).
- **Route cũ `POST /api/content/packages` giữ nguyên lệnh cấm `TOPIC_TEST`/`COURSE_TEST`.** Đặt luật tạo ở một chỗ; route cũ gọi với tập loại hẹp hơn.
- Chi tiết (mục 1) và danh sách trả thêm `topicId`, `courseId`; danh sách nhận thêm lọc `topicId`, `courseId`.

Domain và persistence:

- `ContentPackage` thêm `topicId`, kèm bất biến "`TOPIC_TEST` phải có `topicId`" (đặt cạnh bất biến `COURSE_TEST` phải có `courseId` đang có).
- `ContentPackageJpaEntity` **chưa map cột `topic_id`** (cột đã có trong bảng, seed đang ghi). Thêm mapping; không cần migration. `course_id` đã có ràng buộc DB `chk_content_packages_course_test_course`.
- Kiểm topic/course tồn tại qua repository/reader trong content-service, không thêm bảng.
- Publish không đổi luật (đã có kiểm riêng cho `TOPIC_TEST`/`COURSE_TEST`). Learning chỉ đọc gói `PUBLISHED` có `current_published_version_id` (`JdbcLearningContentReader`: `has_topic_test`, `publishedTestPackages`, `courseTestPackages`), nên đề cuối còn `DRAFT` không lộ cho học viên.

## Thiết kế phía code

- Use case mới trong `application/usecase/` (một use case một hành động, theo pattern hiện có): `GetAuthorPackageUseCase`, `ListAuthorPackagesUseCase`, `CreateDraftPackageVersionUseCase`, `UpdateContentSectionUseCase`, `DeleteContentSectionUseCase`, `ReorderContentSectionsUseCase`, `AddSectionQuestionUseCase`, `RemoveSectionQuestionUseCase`, `ReorderSectionQuestionsUseCase`, `SetSectionPassageUseCase`, `ClearSectionPassageUseCase`, `SetSectionAudioUseCase`, `ClearSectionAudioUseCase`.
- Đặt kiểm "version phải DRAFT" ở một chỗ (method domain trên `ContentPackageVersion`, ví dụ `requireDraft()`), mọi use case ghi đều gọi.
- Đọc chi tiết dùng JDBC theo lô (giống `JdbcLearningContentReader`), qua một port mới trong `application/port/` (ví dụ `AuthorPackageReader`), adapter ở `infrastructure/persistence/adapter/`.
- Controller mới `ContentPackageAuthoringController` dưới `/api/content/admin/packages`; giữ `ContentPackageAdminController` cho preview/schedule.
- Cần migration chỉ khi thiếu ràng buộc: kiểm `section_questions` đã có unique `(section_id, question_version_id)` chưa; nếu thêm unique theo version thì làm ở tầng ứng dụng (query kiểm) thay vì đổi schema, trừ khi dữ liệu seed cho phép. Nếu cần migration thì dùng **V74**.

## Test (bắt buộc)

Test controller/integration theo pattern các test hiện có của content-service (Testcontainers):

- Chi tiết gói trả đủ version, section theo thứ tự, passage/audio, câu hỏi kèm status/purpose; gói không tồn tại → 404.
- Tạo bản nháp khi đã có bản nháp → 409 `DRAFT_EXISTS`; `versionNumber` tự tăng.
- Mọi route ghi vào version `PUBLISHED` → 400 `VERSION_NOT_DRAFT`, **kể cả route cũ thêm section**.
- Section/version không khớp → 400; section không tồn tại → 404.
- Thêm câu trùng trong version → 409; xóa câu đánh lại thứ tự; reorder thiếu/thừa id → 400.
- Đặt passage hai lần: preview trả đoạn văn mới, chỉ còn một link `PASSAGE`. Tương tự audio. `mediaReference` có `..` → 400.
- Xóa section xóa luôn câu và link, không xóa asset.
- Phân quyền: `CUSTOMER` và `EXAMINER` gọi route ghi → 403.
- Upload audio (mock `ObjectStorage`, không gọi Supabase thật): file mp3 hợp lệ → 201 với key dưới `content-audio/{userId}/`; file PNG đổi đuôi thành .mp3 → 400 `UNSUPPORTED_AUDIO`; file rỗng → 400; vượt 25 MB → 413; storage ném `StorageException` → 503 `STORAGE_UNAVAILABLE`; không có `SUPABASE_*` (bean `UnavailableObjectStorage`) → 503 và **service vẫn khởi động** (test context load với cả hai biến rỗng).
- Luồng trọn: tạo gói → tạo bản nháp → 2 section → passage → gắn câu `PUBLISHED` → preview → publish → học viên đọc được qua route internal hiện có.
- Tạo gói qua route mới: `TOPIC_TEST` có topic → `201` và chi tiết trả `topicId`; thiếu `topicId` hoặc topic không tồn tại → `400` `INVALID_PACKAGE_TOPIC`; `TOPIC_TEST` kèm `lessonId` → `400`. Tương tự `COURSE_TEST` với `INVALID_PACKAGE_COURSE`. Loại khác kèm `topicId` → `400`.
- Route cũ `POST /api/content/packages` vẫn từ chối `TOPIC_TEST` và `COURSE_TEST` như trước.
- Đề cuối `DRAFT` gắn vào topic không xuất hiện trong `publishedTestPackages(topicId)` và không làm `has_topic_test` đổi; sau khi publish thì xuất hiện.

## Hoàn thành khi

- Contract `content-package-authoring-v1.md` có đủ route, body, mã lỗi như trên; `content-service/README.md` thêm một đoạn ngắn trỏ tới contract.
- `mvn -q -pl services/content-service -am test` xanh.
- Không đổi hành vi các route cũ, trừ việc chặn thêm section vào version không `DRAFT`.

## Quyết định gỡ chặn (người dùng chốt 2026-10-23)

Hai mâu thuẫn nêu ở mục "Kết quả" bên dưới đã được chốt:

1. **Mở tạo `TOPIC_TEST`/`COURSE_TEST`** cho tác giả qua route mới `POST /api/content/admin/packages` (mục 7 ở trên). Route cũ giữ lệnh cấm. Checklist phase 02 tạo `TOPIC_TEST` bằng route mới, có chọn topic.
2. **`PRACTICE_TEST` dùng câu `EXAM`**, giữ đúng `PublishContentPackageUseCase`. Phase 02 đã sửa: `QuestionChooser` nhận `purpose = EXAM` cho `MOCK_TEST` và `PRACTICE_TEST`.

Plan đã gộp phần AI/Excel thành phase 07–12. Cùng ngày người dùng đổi phân công: Claude Code làm toàn bộ backend (phase này, 07–12, 14), Codex làm toàn bộ giao diện (xem `plan.md`). Phase 01 tiếp tục từ chỗ đã dừng; contract `content-package-authoring-v1.md` đang viết dở cần bổ sung mục 7.

## Kết quả

**Lát 3 (2026-10-23, Claude Code), chưa commit — phase 01 xong về phía backend.**
- Mục 1: danh sách `GET /admin/packages?type=&status=&lessonId=&topicId=&courseId=&q=` (tối đa 500, `latestVersionNumber` null khi chưa có version, `hasDraft`); chi tiết đủ `topicId`, `questionId`, `stem` (cắt 200), `status`, `purpose`, `passage`, `audio` (có `mediaUrl`, null nếu key không resolve được). Đọc theo lô qua port `AuthorPackageReader` (`JdbcAuthorPackageReader`), không N+1.
- Mục 4: thêm/xóa/sắp xếp câu trong section (V78 deferrable), chặn trùng trong version.
- Mục 5: `PUT/DELETE …/passage` và `…/audio` (`SectionMediaUseCases`, `JdbcSectionMediaWriter`): tạo asset mới, gỡ link cũ, giữ asset cũ; `mediaReference` kiểm bằng `ObjectKeys.requireSafe`, thời lượng 1..7200.
- Mục 6: `POST /admin/media/audio` (25 MB, magic bytes, key `content-audio/{userId}/…`); `common-storage` thêm vào content-service; cấu hình `app.storage`, multipart 25/26 MB, `content.media.bucket`; compose thêm `SUPABASE_URL`/`SUPABASE_SECRET_KEY` (cả hai mặc định rỗng → thiếu thì upload 503, service vẫn chạy). Mã lỗi: `EMPTY_FILE`, `UNSUPPORTED_AUDIO`, `AUDIO_TOO_LARGE` (413), `STORAGE_UNAVAILABLE`.
- Mục 7: `POST /admin/packages` tạo gói kể cả `TOPIC_TEST`/`COURSE_TEST` (`INVALID_PACKAGE_TOPIC`, `INVALID_PACKAGE_COURSE`); `topicId` map vào `ContentPackage`/`ContentPackageJpaEntity`. Luật tạo ở một chỗ (`CreateContentPackageUseCase`): route cũ vẫn cấm hai loại này.
- Aggregate chỉ ép "TOPIC_TEST phải có topic" khi tạo mới (`createFinalTest`); dữ liệu cũ đọc lên không bị chặn, ràng buộc DB `chk_content_packages_topic_test_topic` vẫn là chốt cuối.
- Test mới: `JdbcAuthorPackageReaderTest`, `JdbcSectionMediaWriterTest`, `SectionMediaUseCasesTest`, `UploadContentAudioUseCaseTest`, `ContentMediaControllerTest`, `ContentMediaAuthorizationTest`, mở rộng `CreateContentPackageUseCaseTest`, `ContentPackageAuthoring*Test`. Toàn bộ content-service xanh (725).
- Chưa làm: test "luồng trọn" qua HTTP với Supabase thật (không gọi dịch vụ ngoài); kiểm `has_topic_test` đổi sau khi publish đề cuối (đã có luật publish và đọc `PUBLISHED` ở Learning, không đổi trong phase này).

**Lát 2 (2026-10-23, Claude Code), chưa commit:**
- Mục 1 (một phần): `GET /api/content/admin/packages/{id}` trả mọi version kèm section và câu (id, questionVersionId, sortOrder, maxScore). Chưa có: `questionId`, `stem`, `status`, `purpose`, `passage`, `audio`.
- Mục 2: `POST /api/content/admin/packages/{id}/versions`, `DRAFT_EXISTS` 409, khóa dòng package (`findByIdForUpdate`) để hai yêu cầu không cùng tạo bản nháp, `rulesJson` phải là JSON hợp lệ.
- Mục 3: tạo, sửa, xóa section; `PUT …/sections/order` một transaction. Migration V74 đổi unique `(package_version_id, sort_order)` thành `DEFERRABLE INITIALLY IMMEDIATE`; reorder gọi `SET CONSTRAINTS … DEFERRED` trước khi ghi. Thứ tự trùng khi tạo/sửa section trả 400.
- Kiểm tra: section thuộc version khác → 400; không tồn tại → 404; mọi thao tác ghi kiểm bản nháp trước.
- Test: 4 lớp mới (aggregate 9 test, lỗi HTTP 4, phân quyền 1, migration thật trên Postgres 2). Toàn bộ `content-service` xanh.
- Contract `content-package-authoring-v1.md` có mục "Implementation status" ghi phần còn thiếu.
- Còn lại của phase 01: danh sách gói (`GET /admin/packages`), câu trong section (mục 4), đoạn văn và audio (5), upload (6), tạo `TOPIC_TEST`/`COURSE_TEST` (7).

**Lát 1 (2026-10-23, Claude Code), chưa commit:**
- Bất biến "chỉ sửa bản nháp" nằm trong aggregate: `ContentPackageVersion.requireDraft()`, gọi trong `addSection`. Mọi đường ghi section đi qua đó, gồm cả route cũ `POST /api/content/packages/versions/{id}/sections` (lỗ hổng #3).
- Lỗi mới `VersionNotDraftException` (kế thừa `InvalidContentStateException`), handler trả `400` với `details.code = "VERSION_NOT_DRAFT"`.
- Test: `ContentPackageVersionTest` (bản nháp nhận section; bản đã xuất bản từ chối và không đổi gì). Toàn bộ `services/content-service` xanh.
- Sửa lỗi biên dịch có sẵn: `ContentAudioStorage` (WIP của Codex) tham chiếu `StoredAudioResult` chưa tồn tại; đã thêm record này theo mẫu `StoredImageResult`.

**Chưa làm trong phase này:** mục 1 (`GET /admin/packages/{id}` với version và section đầy đủ), mục 2 (tạo bản nháp), mục 3 (sửa/xóa/sắp xếp section), mục 4 (câu hỏi trong section), mục 5 (đoạn văn/audio), mục 6 (upload audio), mục 7 (tạo `TOPIC_TEST`/`COURSE_TEST`). Contract `content-package-authoring-v1.md` mới mô tả đến mục 1; cần bổ sung khi làm tiếp.

---

- Đã đọc quy tắc, plan và các phần liên quan; tạo nhánh `feat/content-package-authoring` từ `origin/main` (`c600b3f3`) sau khi fetch thành công.
- Đã viết contract `docs/contracts/content-package-authoring-v1.md` trước khi code. Port `application/port/ContentAudioStorage.java` mới bắt đầu được tạo; chưa có triển khai hoàn chỉnh, chưa commit/push.
- Migration cao nhất sau fetch là V73; số kế tiếp vẫn là V74. Chưa tạo migration mới.
- Kiểm tra môi trường: Maven 3.9.11, Java 21.0.8; Docker daemon 29.1.3 truy cập được qua quyền thực thi được cấp. Chưa chạy Maven test hoặc checklist thủ công vì đang chờ xác nhận xung đột dưới đây. Không gọi Supabase hay LLM thật.
- Đã dừng thay đổi code theo yêu cầu dừng khi contract/plan mâu thuẫn. `CreateContentPackageUseCase` hiện cấm tạo `TOPIC_TEST` qua API cũ, nhưng checklist của màn mới yêu cầu tạo loại này. `PublishContentPackageUseCase` yêu cầu `EXAM` cho `PRACTICE_TEST`, trong khi màn chọn câu ở phase 02 chỉ định `LEARNING` cho loại đó. Cần người dùng quyết định giữ hành vi backend và điều chỉnh màn, hay phê duyệt thay đổi hợp đồng tương ứng.
- Chưa bắt đầu phase 02; vẫn phải xác nhận backend đã merge và frontend `origin/main` có route `/content-author` trước khi tạo worktree.
- Giữ nguyên thay đổi có sẵn `services/.gitkeep` và mọi thay đổi chưa commit của frontend gốc. Không sửa `plan.md` hay file phase của bên còn lại.
