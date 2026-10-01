# Game content snapshot v1

Game Service requests an immutable content snapshot before starting a game. Two
services implement the same private HTTP contract:

| `learningDomain` | Owner | URL |
| --- | --- | --- |
| `VOCABULARY` | Library Service | `POST /internal/game-content/snapshots` |
| `GRAMMAR` | Content Service | `POST /internal/game-content/snapshots` |

The caller sends its verified internal bearer JWT and `X-Correlation-Id`. The
endpoint is not routed publicly through Gateway. Each service rejects the other
domain. Game Service selects the owner before sending the request.

## Request

```json
{
  "gameType": "SPELLING",
  "contentIds": ["00000000-0000-4000-8000-000000000001"],
  "learningDomain": "VOCABULARY"
}
```

`gameType` is nonempty (at most 50 characters), `contentIds` is a nonempty
list of at most 20 UUIDs with no duplicates, and `learningDomain` is
`VOCABULARY` or `GRAMMAR`. Vocabulary supports `WORD_MEANING_MATCH` and
`SPELLING`; grammar supports `SENTENCE_COMPLETION`, `ERROR_CORRECTION`,
and `WORD_ORDER`.

## Response

```json
{
  "items": [
    {
      "canonicalId": "00000000-0000-4000-8000-000000000001",
      "vocabularySenseId": "00000000-0000-4000-8000-000000000002",
      "questionVersionId": null,
      "prompt": "a fruit",
      "options": [],
      "answerSpecJson": "{\"answer\":\"apple\"}",
      "explanation": "An apple."
    }
  ]
}
```

Each item has a `canonicalId`, a `prompt`, `options`, an
`answerSpecJson` string, and nullable `explanation`. Vocabulary sets
`vocabularySenseId` and leaves `questionVersionId` null. Grammar does the
reverse. Items whose source is not eligible for play may be omitted, preserving
the previous snapshot behavior. Duplicate IDs, missing IDs, unsupported game
types or domains return 400. The wire shape and field names are identical at
both services.
