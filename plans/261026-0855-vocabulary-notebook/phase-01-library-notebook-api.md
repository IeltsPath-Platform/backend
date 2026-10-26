# Phase 01 — Library: API sổ từ vựng

## Đọc trước

- `AGENTS.md` §3 (layer, N+1, phân trang), `services/library-service/README.md`
- `services/library-service/src/main/java/com/ieltspath/library/`: `domain/aggregate/Flashcard.java`,
  `application/usecase/{CreateFlashcardUseCase,ReviewFlashcardUseCase,ListFlashcardsUseCase,GetDueFlashcardsUseCase}.java`,
  `application/port/FlashcardVocabularyMetadataReader.java`, `infrastructure/adapter/FlashcardRepositoryAdapter.java`,
  `config/FlashcardSrsProperties.java`, `api/controller/FlashcardController.java`
- Migration: `V2__create_personal_library.sql`, `V3__flashcard_leitner.sql`, `V17__flashcard_images_and_examples.sql`

## Endpoint (thêm mới, path cũ giữ nguyên)

Controller mới `VocabularyNotebookController` tại `/api/learning-support/flashcards/notebook` (Gateway đã route
`/api/learning-support/flashcards/**` tới library; không sửa Gateway). Danh tính lấy từ `CurrentUserProvider`.

| Method | Path | Mô tả |
| --- | --- | --- |
| `POST` | `/notebook/entries` | Body `{ vocabularySenseId }`. Server dựng `front` = lemma, `back` = nghĩa Việt (fallback định nghĩa Anh), `exampleSentence` của sense. Đã có mục sống → 200 + mục cũ; tạo mới → 201. Sense không tồn tại/không `PUBLISHED` → 404. |
| `GET` | `/notebook/entries` | Query: `q` (≤100, tìm trong lemma/nghĩa, `ILIKE` không phân biệt hoa thường), `level` (`UNKNOWN`/`FAMILIAR`/`LEARNING`/`MASTERED`, lặp được), `due` (bool), `sort` (`RECENT` mặc định / `DUE` / `ALPHABET` / `WEAKEST`), `page`, `size` (≤50). Trả `PageResponse<NotebookEntryResponse>`. |
| `GET` | `/notebook/summary` | `{ total, byLevel{UNKNOWN,FAMILIAR,LEARNING,MASTERED}, dueNow, addedLast7Days, reviewedToday, nextDueAt }` bằng **một** câu SQL aggregate (`count(*) FILTER (...)`). "Hôm nay" theo `Clock` + query param `timezone` (IANA, mặc định `Asia/Ho_Chi_Minh`, sai định dạng → 400). |
| `PUT` | `/notebook/entries/{flashcardId}/level` | Body `{ level, requestId }`. Đổi box theo bảng ở `plan.md`, `next_review_at` = now (UNKNOWN) hoặc now + interval của box. Idempotent theo `requestId` (dùng lại cột `last_review_request_id` nếu hợp ngữ nghĩa, nếu không thì giải thích). Thẻ của người khác → 404. |
| `DELETE` | `/notebook/entries/{flashcardId}` | Gọi lại `DeleteFlashcardUseCase` (soft delete) — chỉ thêm nếu FE cần alias; nếu không, FE dùng `DELETE /flashcards/{id}` sẵn có. |

### Catalog từ điển (thay mock của FE)

| Method | Path | Mô tả |
| --- | --- | --- |
| `GET` | `/api/content/vocabulary?q=&page=&size=` | **Mới** trong `VocabularyController` (Gateway đã route `/api/content/vocabulary/**`; kiểm path gốc không có `/` cuối có khớp route không, nếu không thì dùng `/api/content/vocabulary/browse`). Chỉ item `ACTIVE`, `q` lọc lemma (≤100), sort lemma A–Z, `size` ≤ 50, trả `PageResponse<VocabularyItemResponse>` (kèm senses). Phân trang ở SQL: query id theo trang trước, rồi nạp senses theo tập id — `@EntityGraph` + `Pageable` trên collection sẽ phân trang trong bộ nhớ (HHH90003004), cấm. Quyền như `/search`. |
| `GET` | `/api/learning-support/flashcards/notebook/saved?senseIds=a,b,…` | Trả `string[]` các sense id đã có trong sổ của user (tối đa 100 id, một query) — để trang từ điển hiển thị trạng thái "Đã lưu". |

Không đổi `/search`, `/senses` (content-service và lesson đang dùng); `/{id}` chỉ đổi quyền (xem dưới), response giữ nguyên.

### Mở công khai catalog (không cần đăng nhập)

Chỉ hai route **GET**: `/api/content/vocabulary` (duyệt) và `/api/content/vocabulary/{id}` với id là UUID.
Dùng pattern có regex để không vô tình mở `/search`, `/senses`:
`/api/content/vocabulary/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}`.

1. **Gateway** — `infra/config-server/config-repo/api-gateway.yaml` `public-endpoints` chỉ là danh sách path và
   `SecurityConfig` gọi `pathMatchers(path).permitAll()` cho **mọi method** → không thêm vào list đó (sẽ mở luôn
   `POST /api/content/vocabulary` ở gateway). Thay vào đó thêm trong
   `infra/api-gateway/src/main/java/com/ieltspath/apigateway/security/SecurityConfig.java`, cạnh dòng `/actuator/**`:
   `exchange.pathMatchers(HttpMethod.GET, "/api/content/vocabulary", "<pattern {id} ở trên>").permitAll();`
   (hoặc mở rộng `PublicEndpointProperties` hỗ trợ `method` giống library — chọn cách nhỏ hơn, giải thích trong báo cáo).
   Kiểm filter ký internal JWT (`internal-jwt-paths`) cho request không có token: phải chuyển tiếp không kèm
   Authorization, không 401/500. Nếu request có token hợp lệ thì vẫn ký internal JWT như cũ.
2. **Library** — `infra/config-server/config-repo/library-service.yaml` `app.security.public-endpoints`: thêm mục
   `method: GET` với hai pattern trên. `VocabularyController`: bỏ `@PreAuthorize` khỏi method duyệt và `getDetail` (GET
   `/{id}`); giữ nguyên trên `/search`, `/senses`, POST. Lưu ý controller map cả `/api/content/admin/vocabulary`: path
   admin không có trong public list nên vẫn bắt đăng nhập — ghi rõ điều này trong test.
3. Endpoint public không dùng `CurrentUserProvider`, không trả dữ liệu cá nhân; chỉ item `ACTIVE`; `size` ≤ 50, `q` ≤ 100
   (chống quét không giới hạn). Thêm `Cache-Control: public, max-age=300` cho response public nếu đơn giản.
4. Test bắt buộc: gateway — GET không token tới hai route → không 401; POST `/api/content/vocabulary`, GET `/search`,
   GET `/senses` không token → 401; library `@WebMvcTest`/security test tương ứng; GET `/api/content/admin/vocabulary/{id}`
   không token → 401.
5. Cập nhật `docs/contracts/vocabulary-notebook-v1.md` (đánh dấu route public), `services/library-service/README.md`,
   và mục route/public trong `docs/system-architecture.md` nếu có liệt kê endpoint public.

`NotebookEntryResponse`: `flashcardId, vocabularySenseId, vocabularyItemId, lemma, ipa, pronunciationAudioReference,
partOfSpeech, vietnameseMeaning, englishDefinition, exampleSentence, imageUrl, level, leitnerBox, reviewCount,
lastReviewedAt, nextReviewAt, createdAt, note` (`note` = body của note `VOCABULARY_SENSE` gần nhất, null nếu không có).

Ôn tập: dùng `POST /api/learning-support/flashcards/{id}/reviews` `{correct, requestId}` sẵn có — không viết lại SRS.

## Ghi chú theo từ

- Thêm `VOCABULARY_SENSE` vào `domain/vo/NoteSourceType.java`; `source_reference_id` = `vocabularySenseId`.
- Kiểm `NoteController`/`CreateNoteUseCase` hiện có đã nhận `sourceType/sourceReferenceId` chưa; nếu chưa, bổ sung tối thiểu.
- Giới hạn 300 ký tự cho note của từ (validation ở use case khi `sourceType = VOCABULARY_SENSE`).
- Danh sách sổ từ lấy note bằng một query theo tập sense id (không gọi trong vòng lặp).

## Migration (mới, không sửa migration cũ)

`V18__vocabulary_notebook.sql`:
- Trước khi viết: kiểm DB local có bản trùng không
  (`SELECT user_id, vocabulary_sense_id, count(*) FROM flashcards WHERE source_type='VOCABULARY_SENSE' AND status<>'DELETED' GROUP BY 1,2 HAVING count(*)>1`).
  Có trùng → DỪNG, báo người dùng (không tự xóa).
- `CREATE UNIQUE INDEX uq_flashcards_user_vocabulary_sense ON flashcards (user_id, vocabulary_sense_id) WHERE source_type='VOCABULARY_SENSE' AND status<>'DELETED';`
- Index phục vụ lọc/sort nếu `EXPLAIN` cần (ví dụ `(user_id, status, source_type, leitner_box)`); không thêm index đoán mò.

Use case tạo mục: tìm mục sống trước; bắt `DataIntegrityViolationException` của index (race) rồi đọc lại mục cũ → 200.

## File

Tạo (theo convention library):
- `domain/vo/UnderstandingLevel.java` (enum + `fromBox`, `targetBox`) — luật nằm ở domain, có unit test
- `Flashcard` thêm method `setUnderstanding(level, now, intervals, requestId)`
- `application/usecase/{AddNotebookEntryUseCase,ListNotebookEntriesUseCase,GetNotebookSummaryUseCase,SetUnderstandingLevelUseCase}.java`
- `application/query/NotebookQuery.java`, `application/result/{NotebookEntryResult,NotebookSummaryResult}.java`
- `api/controller/VocabularyNotebookController.java`, `api/dto/request/{AddNotebookEntryRequest,SetUnderstandingLevelRequest}.java`,
  `api/dto/response/{NotebookEntryResponse,NotebookSummaryResponse}.java`
- `domain/repository/FlashcardRepository` (hoặc repository riêng `VocabularyNotebookRepository`) + adapter JDBC/JPA:
  một query join `flashcards` ↔ `vocabulary_senses` ↔ `vocabulary_items`, filter/sort/phân trang ở SQL
- `docs/contracts/vocabulary-notebook-v1.md` (contract HTTP), cập nhật `services/library-service/README.md`

Sửa: `NoteSourceType`, `GlobalExceptionHandler` (nếu cần mã lỗi mới, ví dụ `VOCABULARY_SENSE_NOT_FOUND`).

## Kiểm thử

Theo mẫu test sẵn có trong `services/library-service/src/test/java/com/ieltspath/library/`:
`api/controller/{FlashcardLeitnerControllerTest,NoteSecurityTest}.java`,
`application/usecase/PersonalLibraryUseCaseTest.java`,
`infrastructure/persistence/{FlashcardDueRepositoryIntegrationTest,FlashcardImageAndDeckCountIntegrationTest}.java`.
Lưu ý: content-service gọi library qua `LibraryVocabularySenseClient` (`/api/content/vocabulary/senses`) — không đổi endpoint đó.

- Unit: `UnderstandingLevel` (box↔level), `Flashcard.setUnderstanding` (idempotent requestId, interval), use case thêm mục (trùng → mục cũ).
- `@WebMvcTest`: 401 khi thiếu token, validation 400 (`size>50`, `level` sai, timezone sai), 404 thẻ người khác.
- Testcontainers: migration V18, query danh sách (lọc level/q/due, sort WEAKEST/DUE), summary đếm đúng, race thêm trùng.
- Chạy: `mvn -q -pl services/library-service -am test`. Thiếu Docker thì báo rõ test bị skip.

## Rủi ro

- Index unique fail nếu dữ liệu trùng → câu hỏi mở 1.
- `ILIKE '%q%'` không dùng index; chấp nhận vì đã giới hạn theo `user_id` (sổ từ một người nhỏ). Ghi chú gần code.
- Rollback: endpoint chỉ thêm mới; index có thể drop bằng migration sau.
