# Vocabulary Sense Batch Lookup API v1

## Endpoint

`GET /api/content/vocabulary/senses?ids={uuid},{uuid},...`

Returns active vocabulary senses in the same order as the requested IDs. Unknown and inactive senses are omitted. The `ids` parameter must contain 1–100 valid UUIDs; malformed or missing input returns `400`.

## Response

`200 OK`, JSON array:

```json
[
  {
    "senseId": "uuid",
    "vocabularyItemId": "uuid",
    "lemma": "adapt",
    "ipa": "/əˈdæpt/",
    "pronunciationAudioReference": null,
    "partOfSpeech": "VERB",
    "englishDefinition": "...",
    "vietnameseMeaning": "...",
    "exampleSentence": "...",
    "imageUrl": null
  }
]
```

The endpoint is readable by `ADMIN`, `CONTENT_AUTHOR`, `CUSTOMER`, and `EXAMINER`. Authoring clients can compare returned `senseId` values with the submitted IDs to detect missing or inactive senses.
