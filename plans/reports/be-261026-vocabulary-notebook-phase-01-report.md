# BE-1 — Vocabulary notebook phase 01 report

Date: 2026-10-26  
Branch: `feat/vocabulary-notebook`

## Delivered

- Added vocabulary notebook entries on `flashcards` with `VOCABULARY_SENSE`, unique live `(user_id, vocabulary_sense_id)`, SQL filtering/sorting/paging, one-query summary aggregation, batched saved-ID lookup, and set-understanding behavior in the domain.
- Added `VOCABULARY_SENSE` notes with a 300-character body limit. Notebook responses include the latest active note via one lateral join; note bodies are not logged.
- Added public, cached dictionary browse/detail GET routes at Gateway and Library. Only ACTIVE items/senses are returned. `/search`, `/senses`, create/update, admin paths, and notebook endpoints remain authenticated.
- Kept existing flashcard review and deletion routes. No XP, points, LLM calls, fake seeds, or phase 03/04 work was added.

## HTTP contract

All notebook routes require authentication and use the current user's JWT subject. Common errors use the existing `ErrorResponse` shape.

| Method and path | Query/body | Success response | Main errors |
| --- | --- | --- | --- |
| `POST /api/learning-support/flashcards/notebook/entries` | `{"vocabularySenseId":"<uuid>"}` | 201 new / 200 existing; `NotebookEntryResponse`; archived existing card is restored | 400 invalid body; 401 unauthenticated; 404 missing or inactive sense |
| `GET /api/learning-support/flashcards/notebook/entries` | `q` ≤100; repeatable `level`; `due`; `sort=RECENT\|DUE\|ALPHABET\|WEAKEST`; `page` ≥0; `size` 1–50 | `{items,page,size,total}` | 400 invalid query/value; 401 |
| `GET /api/learning-support/flashcards/notebook/summary` | `timezone` IANA, default `Asia/Ho_Chi_Minh` | `{total,byLevel,dueNow,addedLast7Days,reviewedToday,nextDueAt}` | 400 invalid timezone; 401 |
| `PUT /api/learning-support/flashcards/notebook/entries/{flashcardId}/level` | `{"level":"UNKNOWN\|FAMILIAR\|LEARNING\|MASTERED","requestId":"<uuid>"}` | 204; maps to box 1/2/3/4; UNKNOWN due immediately | 400 invalid body; 401; 404 foreign/deleted/non-notebook card |
| `GET /api/learning-support/flashcards/notebook/saved` | `senseIds=<uuid>,...` (≤100) | JSON array of saved sense ID strings | 400 invalid or >100 IDs; 401 |
| `POST /api/learning-support/flashcards/{id}/reviews` | Existing body `{"correct":boolean,"requestId":"<uuid>"}` | Existing `FlashcardResponse` | Existing contract/statuses |
| `DELETE /api/learning-support/flashcards/{id}` | — | Existing 204 soft delete | Existing contract/statuses |
| `GET /api/content/vocabulary` (public) | `q` ≤100; `page` ≥0; `size` 1–50 | 200 `{items,page,size,total}`; active items and senses; public cache 5 minutes | 400 invalid query |
| `GET /api/content/vocabulary/{id}` (public UUID only) | UUID path | 200 existing item response with active senses; public cache 5 minutes | 400 malformed UUID; 404 missing/inactive item |

`GET /api/content/vocabulary/search`, `GET /api/content/vocabulary/senses`, `POST /api/content/vocabulary`, `/api/content/admin/vocabulary/**`, and all notebook routes remain protected. Missing authentication returns 401 at Gateway and Library.

Full contract: [`docs/contracts/vocabulary-notebook-v1.md`](../../docs/contracts/vocabulary-notebook-v1.md).

## Migration and duplicate check

- Added migration V18: `V18__vocabulary_notebook.sql` (1 migration added).
- Ran the requested duplicate query against local `library_db` before adding the unique index: 0 duplicate `(user_id, vocabulary_sense_id)` groups among non-deleted `VOCABULARY_SENSE` cards.
- Testcontainers validated Flyway through v18 and verified the unique index rejects a second live card for the same user/sense.

## Verification

- `mvn -q -pl services/library-service -am test`: exit 0; 201 tests, 0 failures, 0 errors, 0 skipped. Testcontainers ran with Docker.
- `mvn -q -pl infra/api-gateway test`: exit 0; 20 tests, 0 failures, 0 errors, 0 skipped. Includes public GET returning 200 through a test-only local forward route and unauthenticated adjacent routes returning 401.
- Focused tests also passed for understanding-level transitions, repeated save returning the existing entry/note, SQL filters/summary/saved IDs, invalid catalog limits, and inactive detail 404.
- `git diff --check`: clean.
- `graphify update .`: completed; graph reports 19,208 nodes and 80,691 edges. Graphify noted SQL AST extraction is unavailable because `tree_sitter_sql` is not installed; Java graph update succeeded.
- Final code review found no remaining findings. Archived cards are restored on re-save; note updates enforce the same 300-character limit as creation. Review and set-level changes share the existing pessimistic row lock in `findForReviewByIdAndUserId`.

## Deviations and open questions

- No functional scope deviations. `GET /api/content/vocabulary` uses the already configured library route `/api/content/vocabulary/**`; its no-trailing-slash root path is covered by the gateway test route.
- Phase 03/04 schema and learner progress work remains deferred as requested. The contract responses leave room for later fields.
- No open questions for phase 01. No existing duplicate user/sense cards were found.

Status: DONE
