# Vocabulary notebook and public catalog v1

All notebook routes are authenticated and owner scoped. They do not award XP or points. Notes are stored through the existing notes API with `sourceType=VOCABULARY_SENSE`; a note for this source accepts at most 300 characters.

## Notebook

### `POST /api/learning-support/flashcards/notebook/entries`

Request: `{"vocabularySenseId":"<uuid>"}`. Creates one active vocabulary flashcard for the current user. The server fills the front from the lemma, the back from Vietnamese meaning (English definition fallback), and the example from the sense.

Returns a `NotebookEntryResponse`: `flashcardId`, `vocabularySenseId`, `vocabularyItemId`, `lemma`, `ipa`, `pronunciationAudioReference`, `partOfSpeech`, `vietnameseMeaning`, `englishDefinition`, `exampleSentence`, `imageUrl`, `level`, `leitnerBox`, `reviewCount`, `lastReviewedAt`, `nextReviewAt`, `createdAt`, and `note`. First save returns 201; a repeated save returns the existing entry with 200. Missing or inactive sense returns 404.

### `GET /api/learning-support/flashcards/notebook/entries`

Query: `q` (optional, up to 100 characters; case-insensitive search across lemma and meanings), repeated `level` (`UNKNOWN`, `FAMILIAR`, `LEARNING`, `MASTERED`), `due` (optional boolean), `sort` (`RECENT` default, `DUE`, `ALPHABET`, `WEAKEST`), `page` (default 0), `size` (default 20, 1–50). Returns `{items,page,size,total}` with the entry fields above. Filtering, ordering, and paging run in SQL.

Understanding is derived from Leitner: box 1 = `UNKNOWN`, 2 = `FAMILIAR`, 3 = `LEARNING`, boxes 4–5 = `MASTERED`.

### `GET /api/learning-support/flashcards/notebook/summary`

Query: `timezone` (IANA zone, default `Asia/Ho_Chi_Minh`). Returns `{total,byLevel:{UNKNOWN,FAMILIAR,LEARNING,MASTERED},dueNow,addedLast7Days,reviewedToday,nextDueAt}`. Summary counts are computed in one SQL aggregate. Invalid timezone returns 400.

### `PUT /api/learning-support/flashcards/notebook/entries/{flashcardId}/level`

Request: `{"level":"UNKNOWN|FAMILIAR|LEARNING|MASTERED","requestId":"<uuid>"}`. Updates the Leitner box and review deadline, returning 204. `UNKNOWN` is due immediately; other levels use configured interval days. Replaying the most recent request ID is a no-op. A foreign, deleted, or non-notebook card returns 404.

Practice remains `POST /api/learning-support/flashcards/{id}/reviews` with `{"correct":true,"requestId":"<uuid>"}`.

### `GET /api/learning-support/flashcards/notebook/saved?senseIds=<uuid>,...`

Returns a JSON string array of saved sense IDs for the current user. Accepts at most 100 UUIDs; invalid input returns 400.

Existing `DELETE /api/learning-support/flashcards/{id}` removes a notebook entry (soft delete).

## Public vocabulary catalog

These two GET routes are public at both Gateway and Library. Other methods and paths remain authenticated.

### `GET /api/content/vocabulary`

Query: `q` (optional, lemma search up to 100 characters), `page` (default 0), `size` (default 20, 1–50). Returns `{items,page,size,total}` of active vocabulary items with active senses, ordered by lemma. Item IDs are paged in SQL, then senses are loaded in one bounded batch. Response is cacheable for 5 minutes.

### `GET /api/content/vocabulary/{id}`

`id` must be a UUID. Returns the existing vocabulary item response with active senses only; inactive or missing items return 404. Response is cacheable for 5 minutes.

`/api/content/vocabulary/search`, `/api/content/vocabulary/senses`, all POST methods, `/api/content/admin/vocabulary/**`, and every notebook route require authentication. Unauthorized requests return 401.

## Errors

Common status codes: 400 invalid query/body/UUID/timezone, 401 missing or invalid authentication, 404 missing/inactive resource or resource not owned by the caller, 409 uniqueness conflict if a concurrent duplicate cannot be read back, and 500 unexpected server error.
