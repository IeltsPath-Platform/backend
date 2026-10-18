# Access points contract v1

Access owns point balances and enforces the daily earning limit. All `/internal/**` routes require a verified internal
service JWT; the Gateway blocks them from public clients.

## Credit points over HTTP

`POST /internal/access/points/credit` credits the authenticated learner. `userId` must match the user in the internal JWT.

Request:

```json
{
  "userId": "<uuid>",
  "amount": 5,
  "reason": "KP_MASTERED",
  "referenceId": "<knowledge-point-id>",
  "idempotencyKey": "<request-key>"
}
```

`reason` is one of `STREAK_MILESTONE`, `KP_MASTERED`, `DAILY_QUEST`, or `BADGE`. `amount` is 1 through 100,
`referenceId` is 1 through 100 characters, and `idempotencyKey` is 1 through 120 characters.

Response `200`:

```json
{
  "granted": 2,
  "capped": true,
  "balance": 41,
  "transactionId": "<uuid>"
}
```

The daily learning-reward limit is shared by all four reasons, uses the UTC day, and defaults to 20 points. Configure it
with `access.points.daily-earn-limit` or `ACCESS_POINTS_DAILY_EARN_LIMIT`. A partially granted request returns
`capped: true`; when no points remain, `granted` is `0` and `transactionId` is `null`. Replaying the same idempotency key
with the same user, amount, reason, and reference returns the original result without crediting again. Reusing the key
with different request data returns `409`; a JWT user that differs from `userId` returns `403`; invalid input, including
an unknown reason, returns `400`.

## Learning reward messages (C6)

Access consumes `points.grant.requested` on durable queue `access.points-grant`, bound to durable topic exchange
`learning.points` with routing key `points.grant.requested`.

```json
{
  "grantId": "<uuid>",
  "userId": "<uuid>",
  "amount": 1,
  "reason": "DAILY_QUEST",
  "referenceId": "<reason-specific-reference>"
}
```

The reason set and amount/reference limits match the HTTP credit operation. Access uses `grantId` as the idempotency key.
It publishes a result to `learning.points` with routing key `points.grant.settled`:

```json
{
  "grantId": "<uuid>",
  "status": "GRANTED",
  "error": null
}
```

`status` is `GRANTED`, `CAPPED`, or `FAILED`. Invalid payloads, including an unknown reason, return `FAILED` with
`error: "INVALID_PAYLOAD"`. Reusing a grant ID with different request data returns `FAILED` with
`error: "GRANT_ID_CONFLICT"`. A repeated identical grant is safe and does not add points twice; the settled status
reflects the original credit result.
