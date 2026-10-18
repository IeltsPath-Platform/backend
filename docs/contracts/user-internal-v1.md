# User internal contract v1

These service-to-service lookups require a verified internal JWT. The Gateway denies `/internal/**` requests from public
clients.

## Learning goal

`GET /internal/users/{userId}/learning-goal` returns the learner's active goal:

```json
{
  "targetBand": 6.5,
  "examDate": "2027-01-20"
}
```

If the user exists but has no active goal, both values are `null`:

```json
{
  "targetBand": null,
  "examDate": null
}
```

An unknown user returns `404`.

## Batch display names

`POST /internal/users/display-names` accepts 1 through 50 non-null UUIDs:

```json
{
  "ids": ["<user-id-1>", "<user-id-2>"]
}
```

The response preserves the first occurrence order, removes duplicate IDs, and omits IDs that do not belong to a user:

```json
{
  "items": [
    { "id": "<user-id-1>", "displayName": "Mina Tran" }
  ]
}
```

`displayName` uses a non-blank profile display name when present and otherwise the user's full name. The response never
contains email addresses. An empty list, a list over 50, or a null ID returns `400`.
