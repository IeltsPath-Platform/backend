# Learning Support Service

Learning Support Service owns each learner's notes, flashcards, decks, video progress, saved segments, learning activities, and streak data. API requests go through the gateway with a bearer token; ownership is determined from the authenticated user, not from a request body.

## Flashcard and deck restoration

- `DELETE /api/learning-support/flashcards/{id}` soft-deletes a flashcard.
- `POST /api/learning-support/flashcards/{id}/restore` restores the learner's deleted flashcard to `ACTIVE` and returns its `FlashcardResponse`.
- `DELETE /api/learning-support/decks/{id}` soft-deletes a deck.
- `POST /api/learning-support/decks/{id}/restore` restores the learner's deleted deck to `ACTIVE` and returns its `FlashcardDeckResponse`.

Deleting or archiving a flashcard or deck preserves its deck memberships. `GET /api/learning-support/decks/{deckId}/items` exposes only active flashcards in an active deck. Old memberships become visible again when both the flashcard and deck are active. Explicit `DELETE /api/learning-support/decks/{deckId}/items/{flashcardId}` removes a membership; restoration of a parent does not recreate explicitly removed memberships.

Restore applies only to a `DELETED` resource owned by the authenticated learner. A missing or other learner's resource returns 404; restoring an already-active or archived resource returns 400. A deck restore can return 409 if another active deck now uses its name.
