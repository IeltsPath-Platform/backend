# Flashcard SRS contract v1

Flashcards and decks are scoped to the authenticated learner through the Gateway routes `/api/learning-support/flashcards/**` and `/api/learning-support/decks/**`.

## Upload flashcard image

`POST /api/learning-support/flashcards/images` accepts `multipart/form-data` with field `file`:
- Allowed MIME: `image/png`, `image/jpeg`, `image/webp`.
- Max size: 2 MB (`UploadPolicy.of(2 MB, ...)`). Content sniffing is enforced.
- Key format: `images/{userId}/{uuid}.{ext}`.
- Response (HTTP 201 Created):

```json
{
  "key": "images/0b1f7e34-5f50-4813-8ad8-8c11ebfa12e6/79e7f8e8-d0df-4f59-a5c9-9524e7eb92b8.png",
  "url": "https://<supabase-url>/storage/v1/object/public/flashcards/images/0b1f7e34-5f50-4813-8ad8-8c11ebfa12e6/79e7f8e8-d0df-4f59-a5c9-9524e7eb92b8.png",
  "contentType": "image/png",
  "size": 12345
}
```

Errors:
- `400 Bad Request`: empty file, unsupported MIME type, or content mismatch.
- `413 Payload Too Large`: exceeds 2 MB.
- `503 Service Unavailable`: storage service unavailable.

## Create / Update Flashcard

`POST /api/learning-support/flashcards` and `PUT /api/learning-support/flashcards/{id}`:
- `imageKey`: optional string (max 300 chars, e.g. `images/{userId}/{uuid}.png`). When provided and changed, verified to be owned by the user and existing in storage; fails with `400 INVALID_IMAGE_KEY` otherwise. On PUT, passing `null` clears the image.
- `exampleSentence`: optional string (max 1000 chars, trimmed).
- Replacing or removing an image (or deleting the flashcard) cleans up the old storage object after transaction commit (best-effort).

## Due cards and Flashcard Response

`GET /api/learning-support/flashcards/due?deckId=<uuid>&page=0&limit=20` returns active cards with no `nextReviewAt` or
with `nextReviewAt` at or before the current time. `deckId` is optional. When supplied, cards are limited to that
learner's active deck; an unknown, inactive, or other learner's deck produces no matching cards. Cards that have never
been reviewed sort first, followed by `nextReviewAt` and card ID.

`page` defaults to `0`; `limit` defaults to `20` and is restricted to 1 through 50. Negative pages or invalid limits
return `400`.

```json
{
  "items": [
    {
      "id": "<flashcard-id>",
      "userId": "<learner-id>",
      "sourceType": "VOCABULARY_SENSE",
      "vocabularySenseId": "<sense-id>",
      "sourceReferenceId": null,
      "highlightedText": null,
      "front": "resilient",
      "back": "able to recover quickly",
      "status": "ACTIVE",
      "createdAt": "2026-10-18T08:00:00Z",
      "updatedAt": "2026-10-18T08:00:00Z",
      "leitnerBox": 1,
      "nextReviewAt": null,
      "lastReviewedAt": null,
      "reviewCount": 0,
      "ipa": "/rɪˈzɪliənt/",
      "pronunciationAudioReference": null,
      "exampleSentence": "The town proved resilient after the storm.",
      "imageKey": "images/<userId>/<uuid>.png",
      "imageUrl": "https://<supabase-url>/storage/v1/object/public/flashcards/images/<userId>/<uuid>.png"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

`exampleSentence` returns the card's custom sentence if set; if null, it falls back to the linked vocabulary sense example sentence. Vocabulary metadata fields (`ipa`, `pronunciationAudioReference`) are `null` when the card has no linked vocabulary sense or the catalog metadata is unavailable.

## Review a card

`POST /api/learning-support/flashcards/{flashcardId}/reviews` accepts:

```json
{
  "correct": true,
  "requestId": "<uuid>"
}
```

It returns the updated flashcard response. A correct answer advances one Leitner box up to box 5; an incorrect answer
resets it to box 1. The next review time uses the configured interval for the resulting box. The default
`library.srs.intervals-days` is `[1, 2, 4, 8, 16]`; configuration must contain five positive day values. Each accepted
review updates `lastReviewedAt` and increments `reviewCount`. Replaying the card's most recent `requestId` is a no-op.
Cards not owned by the learner or not active return `404`; a missing `correct` or `requestId` returns `400`.

## Decks

`POST /api/learning-support/decks` and `PUT /api/learning-support/decks/{id}`:
- Accept `name` (1-150 chars), `description` (optional, max 2000 chars), `coverImageKey` (optional, max 300 chars).
- Response shape:

```json
{
  "id": "<deck-id>",
  "userId": "<learner-id>",
  "name": "IELTS Academic",
  "description": "Essential vocabulary",
  "status": "ACTIVE",
  "createdAt": "2026-10-18T08:00:00Z",
  "updatedAt": "2026-10-18T08:00:00Z",
  "coverImageKey": "images/<userId>/<uuid>.webp",
  "coverImageUrl": "https://<supabase-url>/storage/v1/object/public/flashcards/images/<userId>/<uuid>.webp",
  "cardCount": 25,
  "dueCount": 6
}
```

- `cardCount` and `dueCount` count ACTIVE cards belonging to the deck (due cards: `nextReviewAt IS NULL OR nextReviewAt <= now()`), aggregated across all decks in the page in a single query.

## Deck items

`GET /api/learning-support/decks/{deckId}/items` returns paged `DeckItemResponse`:
- Includes `imageUrl`, `exampleSentence`, `leitnerBox`, and `ipa` for each active card in the deck.
- `leitnerBox` is retrieved from `flashcards.leitner_box` in `findActive`.
- `ipa` and `exampleSentence` (when custom card sentence is null) fallback from catalog metadata via `FlashcardVocabularyMetadataReader`, batched across all linked `vocabularySenseId`s on the page without loop queries.

Sample `DeckItemResponse`:
```json
{
  "deckId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "flashcardId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "sortOrder": 1,
  "addedAt": "2026-10-21T07:00:00Z",
  "front": "resilient",
  "back": "kiên cường",
  "sourceType": "MANUAL",
  "status": "ACTIVE",
  "imageUrl": "https://storage.../images/user-id/card.webp",
  "exampleSentence": "He remained resilient in the face of difficulties.",
  "leitnerBox": 4,
  "ipa": "/rɪˈzɪl.jənt/"
}
```
