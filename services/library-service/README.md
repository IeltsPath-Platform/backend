# Library Service

`library-service` (Spring Boot, Eureka name `LIBRARY-SERVICE`) runs on port `8081` and owns `library_db`. The root
Compose stack runs Library with the other deployed services; PostgreSQL publishes at host port `5440`.

## Data and routes

Flyway V1 creates the five catalog tables: `vocabulary_items`, `vocabulary_senses`, `learning_videos`,
`video_segments`, `video_segment_lexical_entries`. V2 creates six personal-library tables: `flashcard_decks`,
`flashcards`, `flashcard_deck_items`, `notes`, `video_learning_progress`, `saved_video_segments`. V3 adds the
five-box flashcard review schedule. V4 seeds 300 mock vocabulary items and 302 senses for local demos, using stable
UUIDv5 identifiers and idempotent inserts. V17 adds `image_key` and `example_sentence` to `flashcards`, and `cover_image_key` to `flashcard_decks`. Four V2 foreign keys to catalog tables use `ON DELETE RESTRICT`; there is no
`outbox_events` table. Data is not copied from the former service/database.

Gateway routes `/api/content/videos/**`, `/api/content/vocabulary/**` and `/api/content/admin/vocabulary/**` here.
It also routes `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` here; the public
paths and request/response shapes remain the same. Catalog writes require `ADMIN` or `CONTENT_AUTHOR`; personal
library writes are owner-scoped.

## Flashcard images and review schedule

- Bucket: `flashcards` (configurable via `LIBRARY_FLASHCARD_BUCKET`), **Public**, max 2 MB, MIME `image/png, image/jpeg, image/webp`.
  - **Bucket creation in Supabase**: Create bucket `flashcards` in Supabase Storage with "Public bucket" enabled.
  - **Public access & security**: Public bucket allows direct image delivery via CDN/public URL without presigned overhead. Images are stored with path `images/{userId}/{uuid}.{ext}` where `{uuid}` is a cryptographically strong UUIDv4, making URL enumeration infeasible.
  - **Behavior when Supabase is missing**: When `SUPABASE_URL` is empty or `app.storage.enabled: false`, `UnavailableObjectStorage` is used. `library-service` starts cleanly and all core endpoints (catalog, videos, text flashcards) work normally. Card image URLs resolve to `null`. Only storage write/upload requests fail with 503 Service Unavailable.
- Images are uploaded through `POST /api/learning-support/flashcards/images` (multipart field `file`), returning `{key, url, contentType, size}`.
- Object keys follow `images/{userId}/{uuid}.{ext}`. Database stores only keys; URLs are generated dynamically in responses via `ObjectStorage.publicUrl`.
- Image keys on create/update must belong to the authenticated user, exist in storage, and **not already be in use** by another active flashcard or deck.
- Storage checks (`storage.exists`) run **outside database transactions** to prevent connection holding.
- **PUT semantics**: Omitting `imageKey` / `coverImageKey` in update requests preserves existing images; passing `null` or `""` explicitly deletes the image.
- Deletion of storage objects is reference-counted: an image is deleted after commit **only if no other active flashcard or deck references it**.

`GET /api/learning-support/flashcards/due?deckId=<uuid>&page=0&limit=20` returns the authenticated learner's active
due cards, ordered with never-reviewed cards first and then by their next review time. The query filters to an active
deck owned by that learner when `deckId` is provided. Results are paged in PostgreSQL; `limit` defaults to 20 and
cannot exceed 50. Card responses include the Leitner box and schedule; cards linked to a vocabulary sense also include
the item's `ipa`, `pronunciationAudioReference`, and the sense's `exampleSentence`. Custom card `exampleSentence` takes precedence over sense metadata.

`POST /api/learning-support/flashcards/{id}/reviews` accepts
`{"correct":true,"requestId":"<uuid>"}` and returns the updated card. A correct answer advances one box up to 5;
an incorrect answer resets the card to box 1. Replaying the most recent `requestId` is a no-op. The interval for the
resulting box comes from `library.srs.intervals-days`, defaulting to `[1, 2, 4, 8, 16]`.
See the [flashcard SRS contract](../../docs/contracts/flashcard-srs-v1.md) for request, response, and ownership details.

## Vocabulary notebook and catalog

`GET /api/content/vocabulary?q=&page=0&size=20` browses active catalog items (maximum page size 50); `GET /api/content/vocabulary/{id}` returns one active item. These two GET routes are public and cacheable for five minutes. `/search`, `/senses`, writes, and `/api/content/admin/vocabulary/**` still require authentication. See the [vocabulary notebook contract](../../docs/contracts/vocabulary-notebook-v1.md).

The public catalog also supports `lang=EN|VI`, CEFR/topic filters, `/suggestions`, `/topics`, `/sets`, set words, and `/mindmap`. V19 adds UK/US pronunciation slots, CEFR and library-owned vocabulary topics, examples/collocations/thesaurus/common-mistake tables, and band/topic sets. IELTS band ranges are derived from CEFR in the domain. V20 adds six demo topics and demo enrichment for existing V4/V15 entries; frequency ranks remain null because no COCA source is loaded. The AWL flag is enabled only for verified Coxhead entries. **Demo content authored by AI; requires review by an IELTS examiner before production.**

Topics and sets are part of the Library dictionary and are independent from Content curriculum topics. Topic/set cover keys reuse the existing `flashcards` storage bucket and `POST /api/learning-support/flashcards/images`; no additional bucket or environment variable is required.

Learners save vocabulary senses with `POST /api/learning-support/flashcards/notebook/entries`, list/filter/sort them at `/entries`, read aggregate counts at `/summary`, and set understanding at `/entries/{flashcardId}/level`. Existing flashcard review and delete routes remain in use. One non-deleted card is allowed per learner and sense; level is derived from Leitner boxes (1 UNKNOWN, 2 FAMILIAR, 3 LEARNING, 4–5 MASTERED). Notes use `sourceType=VOCABULARY_SENSE` and are limited to 300 characters. Migration V18 adds the partial unique index.

When saving a video, Library verifies its topic through Content's `GET /api/content/topics/{id}`, forwarding the
authenticated bearer token and `X-Correlation-Id`. The HTTP client uses a 2-second connection and 5-second read
timeout; Content failures return 503. `POST /internal/game-content/snapshots` serves `VOCABULARY` using the shared
[game snapshot contract](../../docs/contracts/game-content-snapshot-v1.md); Content serves `GRAMMAR`.

## Local setup

For the full container stack, run `docker compose up -d --build`. To run Library from the host, start PostgreSQL with
`docker compose up -d postgres` and set `LIBRARY_DB_URL=jdbc:postgresql://localhost:5440/library_db` and
`LIBRARY_DB_PASSWORD` to the same local value as `POSTGRES_PASSWORD`. Runtime config is in
`infra/config-server/config-repo/library-service.yaml`:

| Variable | Default or purpose |
| --- | --- |
| `LIBRARY_DB_URL` | `jdbc:postgresql://localhost:5440/library_db` |
| `LIBRARY_DB_USERNAME` | `postgres` |
| `LIBRARY_DB_PASSWORD` | Required database password; no fallback in service config |
| `CONTENT_SERVICE_URL` | `http://localhost:8082` for topic lookup |
| `GATEWAY_INTERNAL_JWT_SECRET` | Verifies Gateway's internal JWT; use the same value as Gateway |
| `SUPABASE_URL` | Supabase API URL for object storage |
| `SUPABASE_SECRET_KEY` | Supabase secret key for storage access |
| `LIBRARY_FLASHCARD_BUCKET` | Bucket name for flashcard illustrations (`flashcards` by default) |

Verify with `mvn -q -pl services/library-service -am test`. Migration tests use Testcontainers and require Docker.

## Wordly random vocabulary

POST `/internal/game-content/wordly/random` accepts `{"count":10}` (integer 1-100) and
returns `{"words":[{"sourceWordId":"<sense UUID>","answer":"AIR","meaning":"..."}]}`.
This is a service-to-service endpoint protected by the existing internal JWT mechanism,
not a Gateway/browser route. It returns up to count unique uppercase alphabetic answers
(1–100 letters), using ACTIVE items/senses with a nonblank Vietnamese meaning. Senses are
selected deterministically by sort_order/id before randomizing the distinct words.
The caller decides whether fewer than count words are sufficient for its game mode.
`data/wordly-starter-vocabulary.json` contains the 100-word starter dataset imported through
the ADMIN vocabulary API for local testing. It is not an automatic production seed.


POST `/internal/game-content/wordly/catalog` accepts `{"after":"","limit":500}`.
It returns the same words shape, ordered by uppercase answer, strictly after the cursor
(the previous page's last answer). limit is 1–500; an empty page ends pagination.
It uses the same ACTIVE/meaning/alphabetic filters as random and the same internal JWT.
Game stores these pages as a shared immutable dictionary version for guess validation and endless progression.
This endpoint is internal; it must not be exposed as a browser/Gateway route.

## Chưa làm / Known gaps

- **Dọn ảnh mồ côi (Orphaned images cleanup)**: Ảnh đã upload lên bucket nhưng người dùng hủy không tạo thẻ hoặc không lưu deck sẽ tồn tại trong bucket như rác. Cần một background job định kỳ (hoặc lifecycle rule trên bucket) để dọn dẹp các object không được tham chiếu trong DB.
- **Hạn mức tải ảnh theo người dùng (Per-user upload rate limit / quota)**: Hiện tại người dùng có thể tải lên ảnh 2MB mà chưa có hạn mức theo ngày (daily upload count / bandwidth quota). Cần bổ sung rate limit hoặc kiểm tra hạn mức lưu trữ theo tài khoản để tránh lạm dụng dung lượng storage.
