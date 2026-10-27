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

## Examiners

The learner chooses who grades a Writing or Speaking submission, so User Service publishes the examiners. An examiner is
an `ACTIVE` user with the `EXAMINER` role. Name, photo and introduction come from the learner profile
(`display_name`, `avatar_reference`, `bio`); the band, certificates, availability and workload limit are stored in
`examiner_profiles`, and an examiner who never edited it counts as accepting submissions with a limit of 30.

```json
{
  "id": "<user-id>",
  "displayName": "Ms Lan",
  "avatarUrl": "https://images.example/lan.png",
  "bio": "Ten years of IELTS teaching",
  "ieltsBand": 8.5,
  "certificates": [{ "name": "CELTA", "issuer": "Cambridge", "year": 2018, "score": "A" }],
  "acceptingSubmissions": true
}
```

- `GET /api/users/examiners` — any signed-in user; the examiners with `acceptingSubmissions: true`.
- `GET /api/users/examiners/{id}` — any signed-in user; one examiner even when they stopped accepting (so a learner can
  still read the profile of the examiner they chose). `404` for an unknown, inactive or non-examiner user.
- `PUT /api/users/me/examiner-profile` — role `EXAMINER`, edits the caller's own profile:
  `{ "ieltsBand": 8.5, "certificates": [...], "acceptingSubmissions": true }`. `ieltsBand` is optional, a half-band from
  0.0 to 9.0; at most 10 certificates, `name` required (150 characters), `issuer` 150, `year` 1950–2100, `score` 20.
  `acceptingSubmissions` is required. An invalid body is refused.
- `PUT /api/users/{id}/examiner-profile/capacity` — role `ADMIN`: `{ "dailyLimit": 30 }`, 1 through 50 (default 30): how many new
  submissions the examiner takes a day, Writing and Speaking together (counted by Assessment). The number is never shown to learners.

`avatarUrl` is the stored picture address only when it is an `http` or `https` URL without credentials, otherwise `null`.
The responses never contain email addresses.

### The examiner profile is public on purpose

`GET /api/users/examiners` and `GET /api/users/examiners/{id}` have no role check: **every signed-in user** (`requireUserId()`),
not only learners, can read an examiner's profile. This is intended: the learner chooses who grades their essay, so they need to
see who the examiner is before choosing and while waiting. An unauthenticated caller gets `401`.

The only fields returned are the ones in the example above: `id`, `displayName`, `avatarUrl`, `bio`, `ieltsBand`,
`certificates` (`name`, `issuer`, `year`, `score`) and `acceptingSubmissions`. Never returned: the email address, the
phone, the roles, the workload limit (`dailyLimit`) or how many jobs the examiner has today. An examiner who must not be
shown stops taking submissions (`acceptingSubmissions: false`) and drops off the list; their profile is still readable by
learners who already chose them.

### `GET /internal/users/examiners/{id}`

For Assessment, before it gives an examiner a job. It needs the internal service JWT and is not routed through the
gateway.

```json
{ "id": "<user-id>", "acceptingSubmissions": true, "dailyLimit": 10 }
```

`404` for an unknown, inactive or non-examiner user.
