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

The listed GET routes are public at both Gateway and Library. Public responses contain catalog data only. Requests are not authenticated into an individual user context.

### `GET /api/content/vocabulary`

Query: `q` (optional, up to 100 characters), `lang=EN|VI` (default `EN`), `cefr=A1|A2|B1|B2|C1|C2`, `topicId=<uuid>`, `page` (default 0), `size` (default 20, 1–50). `EN` searches lemma; `VI` searches Vietnamese meaning. Filters, ordering, count, and paging run in SQL. Returns `{items,page,size,total}` of active items with active senses. Related tables are loaded in bounded batches (one query per table). Response is cacheable for 5 minutes.

### `GET /api/content/vocabulary/{id}`

`id` must be a UUID. Returns the existing vocabulary item fields with active senses only; inactive or missing items return 404. Existing fields are preserved. Added item fields are `ipaUk`, `ipaUs`, `audioUkReference`, `audioUsReference`, `frequencyRank`, `academicWordList`, `wordFamily`, and aggregate `counts`. `ipaUk`/`audioUkReference` fall back to legacy `ipa`/`pronunciationAudioReference` when unset. Each sense adds `cefrLevel`, derived `bandRange:{min,max}`, `topic`, `register`, `grammarNote`, `usageNote`, `exampleTranslation`, `collocations[]`, `synonyms[]`, `antonyms[]`, `commonMistakes[]`, `examples[]`, and `counts:{definitions,collocations,thesaurus,commonMistakes,examples}`. A missing CEFR has a null `bandRange`. No frequency rank is supplied without a verified source. Response is cacheable for 5 minutes.

The CEFR-to-IELTS mapping is domain-owned: A1 1.0–2.5, A2 3.0–3.5, B1 4.0–5.0, B2 5.5–6.5, C1 7.0–8.0, C2 8.5–9.0.

### `GET /api/content/vocabulary/suggestions?q=`

`q` is required and 1–50 characters. Returns at most eight active `{id,lemma}` suggestions whose lemmas start with `q`.

### `GET /api/content/vocabulary/topics`

Returns active vocabulary topics with `{id,slug,name,nameVi,coverKey,coverUrl,sortOrder,wordCount}`. Counts are grouped in SQL.

### `GET /api/content/vocabulary/sets?kind=BAND|TOPIC&page=0&size=20`

`kind` is optional; `size` is 1–50. Returns `{items,page,size,total}` with set title, description, kind, CEFR/band range or topic, cover URL, topic count, and word count.

### `GET /api/content/vocabulary/sets/{id}`

Returns active set metadata, `coverUrl`, `bandRange` when applicable, `wordCount`, and topic summaries with word counts. Missing/inactive set returns 404.

### `GET /api/content/vocabulary/sets/{id}/words?topicId=&page=0&size=20`

Returns `{items,page,size,total}` of active set members. Optional `topicId` filters membership; `size` is 1–50.

### `GET /api/content/vocabulary/mindmap?topicId=<uuid>` or `?itemId=<uuid>`

Exactly one parameter is required. Returns `{nodes,edges}` with at most 80 total nodes; topic queries include the topic node and related words, collocations, and synonyms.

### Vocabulary catalog ownership

`vocabulary_topics` belongs to Library and is separate from Content's curriculum `topics`. Catalog browsing does not call `content-service`. Band is derived from CEFR and is not persisted.

### Authenticated vocabulary author API

These paths require `CONTENT_AUTHOR` or `ADMIN`; missing/invalid authentication returns 401 and other roles return 403. All writes use the normal `ErrorResponse` handler and do not log submitted text.

| Method and path | Request | Success |
| --- | --- | --- |
| `PUT /api/content/admin/vocabulary/items/{id}` | JSON fields `ipaUk`, `ipaUs`, `audioUkReference`, `audioUsReference`, `frequencyRank`, `academicWordList`, `wordFamily` | 200 updated item |
| `PUT /api/content/admin/vocabulary/senses/{id}` | JSON fields `cefrLevel`, `topicId`, `register`, `grammarNote`, `usageNote`, `exampleTranslation` | 200 updated sense |
| `PUT /api/content/admin/vocabulary/senses/{id}/collocations` | Array of `{phrase,meaningVi,example}` (≤30) | 200 replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/relations` | Array of `{relatedLemma,relatedItemId,kind}`; kind is `SYNONYM|ANTONYM|RELATED` (≤30) | 200 replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/common-mistakes` | Array of `{wrongUsage,correctUsage,explanation}` (≤30) | 200 replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/examples` | Array of `{sentence,translationVi}` (≤30) | 200 replacement list |
| `GET /api/content/admin/vocabulary/topics` | — | 200 topic list |
| `POST /api/content/admin/vocabulary/topics` | `{slug,name,nameVi,coverKey,sortOrder,status}` | 201 topic |
| `PUT /api/content/admin/vocabulary/topics/{id}` | Same topic fields | 200 topic |
| `DELETE /api/content/admin/vocabulary/topics/{id}` | — | 204; marks inactive |
| `GET /api/content/admin/vocabulary/sets` | — | 200 set list |
| `POST /api/content/admin/vocabulary/sets` | `{title,description,kind,cefrLevel,topicId,coverKey,sortOrder,status}`; BAND requires CEFR; TOPIC requires topicId | 201 set |
| `PUT /api/content/admin/vocabulary/sets/{id}` | Same set fields | 200 set |
| `DELETE /api/content/admin/vocabulary/sets/{id}` | — | 204; marks inactive |
| `PUT /api/content/admin/vocabulary/sets/{id}/items` | JSON array of ordered sense UUIDs (≤500; unique) | 204 |

Topic/set covers store an object key in the existing `flashcards` bucket and resolve a public URL through common-storage. Uploads reuse `POST /api/learning-support/flashcards/images`; no extra bucket or environment setting is introduced.

Migrations V19 (additive schema) and V20 (demo catalog) extend the library database. Demo CEFR/topic/examples/collocations/mistakes are authored content and require IELTS examiner review. `frequency_rank` remains null without a COCA source; only the verified Coxhead AWL lemmas `data`, `method`, and `research` are marked true.

`/api/content/vocabulary/search`, `/api/content/vocabulary/senses`, catalog writes, `/api/content/admin/vocabulary/**`, and every notebook route require authentication. Unauthorized requests return 401; authenticated users without the required author role receive 403 for the catalog author API.

## Errors

Common status codes: 400 invalid query/body/UUID/timezone, 401 missing or invalid authentication, 404 missing/inactive resource or resource not owned by the caller, 409 uniqueness conflict if a concurrent duplicate cannot be read back, and 500 unexpected server error.
