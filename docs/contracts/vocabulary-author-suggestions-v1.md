# Author vocabulary suggestions

Updated: 2026-11-04

`GET /api/content/admin/vocabulary/suggestions?q=poll&page=0&size=20` is authenticated and requires
`CONTENT_AUTHOR` or `ADMIN`. Library owns the data. Gateway already routes this path to Library with internal JWT
exchange and the default rate-limit tier (20 requests/second, burst 40, per authenticated subject).
The public `/api/content/vocabulary/suggestions` and existing `/search` contracts are unchanged.

## Query and response

- `q`: at most 100 characters, trimmed and lowercased with `Locale.ROOT`, matching `normalized_lemma`.
- Empty/whitespace query returns an empty page without reading the catalog.
- One character: exact match only. Two characters: prefix match. Three or more: substring match.
- `%`, `_`, and backslash are literal characters, not caller-supplied LIKE wildcards.
- `page`: zero-based, nonnegative. `size`: defaults to 20, minimum 1, maximum 50 **words**, independent of sense count.
- Rank: exact, prefix, other substring; ties use normalized lemma then item UUID. Only active words with active senses appear.

```json
{
  "content": [{
    "id": "10000000-0000-4000-8000-000000000001",
    "lemma": "pollution",
    "ipa": "/pəˈluːʃən/",
    "senses": [{
      "id": "10000000-0000-4000-8000-000000000002",
      "partOfSpeech": "NOUN",
      "englishDefinition": "contamination of the environment",
      "vietnameseMeaning": "ô nhiễm"
    }]
  }],
  "page": 0,
  "size": 20,
  "hasMore": false
}
```

`ipa` and `englishDefinition` may be null. There is no total count. The database fetches `size + 1` candidate words
to determine `hasMore`, then fetches active senses for the selected word IDs in one batch. It does not paginate a
collection-fetch join or load every matching word into application memory. Pages are independent reads; catalog edits
between requests can change page boundaries.

## Bounds and failures

Invalid query length/page/size: 400. Anonymous: 401. Other authenticated roles: 403.
The read-only transaction has a two-second timeout; query timeout returns 503 with the standard ErrorResponse.
The lesson picker has a five-second request deadline and offers retry on failure; replacing a query cancels silently.

## Index and deployment

Flyway V36 adds `pg_trgm` and two partial indexes on active `vocabulary_items.normalized_lemma`:
`varchar_pattern_ops` B-tree for equality/prefix and `gin_trgm_ops` GIN for substring matches. The existing sense-to-item
index supports the active-sense lookup. The extension must be installed, or the migration role must have permission
to install it. Index creation takes a write lock: schedule this additive migration during the deployment window.

Deploy Library (including V36) before the frontend that calls this endpoint. No change to the Content schema is needed.
The frontend debounces 300 ms from two characters, supports explicit one-character searches, and never waits for
IME compositionend. Load-more retains existing rows and selected sense IDs; a new query resets pagination.

## Verification

`VocabularySuggestionsPersistenceTest` exercises real PostgreSQL 15 with Flyway, active-state filtering, ranking,
literal patterns, word-before-sense pagination, and an additional 20,000 generated words in an isolated Testcontainer.
It prints `EXPLAIN (ANALYZE, BUFFERS)` and times 24 use-case reads with four workers. This measures database/use-case
latency, not HTTP p95 through Gateway. Check API p95 on the deployment environment before claiming the 200 ms target.
