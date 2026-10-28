# Vocabulary Lemma Lookup and Bulk Resolve API v1

Both routes are also served under `/api/content/admin/vocabulary`. `CONTENT_AUTHOR` only. Used by the
[course drafts](course-drafts-v1.md) to check and write the words of a lesson.

## Look up by lemma

`GET /api/content/vocabulary/by-lemmas?lemmas={lemma},{lemma},...`

Returns the `ACTIVE` words whose lemma is exactly one of those asked for (trimmed, compared in lower case), each with its
`ACTIVE` senses ordered by `sortOrder`. A word without an active sense, and a word that is switched off, are omitted.
`lemmas` has 1–100 entries of up to 255 characters; anything else returns `400`.

`200 OK`, JSON array of the same items as `GET /api/content/vocabulary/search`:

```json
[{ "id": "uuid", "lemma": "trend", "ipa": "/trend/", "status": "ACTIVE",
   "senses": [{ "id": "uuid", "partOfSpeech": "NOUN", "englishDefinition": "...", "vietnameseMeaning": "...",
                "exampleSentence": "...", "status": "ACTIVE", "sortOrder": 0 }] }]
```

## Resolve reviewed words

`POST /api/content/vocabulary/bulk`

```json
{ "entries": [{ "lemma": "Trend", "ipa": "/trend/", "partOfSpeech": "NOUN", "englishDefinition": "...",
                "vietnameseMeaning": "xu hướng", "exampleSentence": "There is an upward trend." }] }
```

- 1–200 entries. `lemma` (up to 255), `partOfSpeech`, `vietnameseMeaning` and `exampleSentence` are required;
  `partOfSpeech` is `NOUN`, `VERB`, `ADJECTIVE`, `ADVERB`, `PREPOSITION`, `CONJUNCTION`, `IDIOM` or `PHRASAL_VERB`.
- For each distinct lemma (trimmed, lower case): if the library has the word and it has an active sense, that sense is
  returned and the entry's text is ignored (`created: false`); if the word has no active sense, the entry's sense is added to
  it; if the library lacks the word, the word and the entry's sense are created. Created words and senses are `ACTIVE`.
- A word that exists but is switched off returns `400` and nothing is written. The whole call is one transaction.

`200 OK`, one item per distinct lemma, in order of first appearance:

```json
[{ "lemma": "trend", "senseId": "uuid", "created": true }]
```
