# Flashcard SRS contract v1

Flashcards are scoped to the authenticated learner through the Gateway route `/api/learning-support/flashcards/**`.

## Due cards

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
      "exampleSentence": "The town proved resilient after the storm."
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

Vocabulary metadata fields are `null` when the card has no linked vocabulary sense or the catalog metadata is unavailable.

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
