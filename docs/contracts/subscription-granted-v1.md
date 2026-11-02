# "Premium subscription granted" notice (v1)

When an administrator grants a learner a Premium subscription, Access tells Notification, which stores a notice for the
learner and pushes it to them.

## Event

Published by Access on exchange `access.events`, routing key `access.subscription-granted.v1`. It goes through the
outbox and the relay in Access: at-least-once, persistent, and counted as sent only when the broker has confirmed it and
routed it to a queue. The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "40000000-0000-4000-8000-000000000001",
  "eventType": "access.subscription-granted.v1",
  "occurredAt": "2026-11-01T02:00:00Z",
  "userId": "40000000-0000-4000-8000-000000000002",
  "subscriptionId": "40000000-0000-4000-8000-000000000003",
  "planCode": "PREMIUM_MONTHLY",
  "endsAt": "2026-12-01T02:00:00Z"
}
```

- `endsAt` is when the granted access ends. The notice carries no price and no payment detail.
- The outbox row is written in the same transaction as the subscription. A grant that is rolled back is never announced.
- Only grants made through the administrator endpoint are announced. Other outbox event types in Access (point debits, human
  grading credits) are stored as `NOT_RELAYED` and never leave the service.

## Granting: `Idempotency-Key`

`POST /api/access/admin/subscriptions/grant` requires an `Idempotency-Key` header of 1 to 100 characters. The key is what
makes a retried grant safe.

- Same key, same payload (user, plan, duration, grading credits): returns the first result. The subscription is not extended
  again and no second event is written.
- Same key, different payload: `409 Conflict`.
- Missing, blank or longer than 100 characters: `400` with code `IDEMPOTENCY_KEY_REQUIRED`.
- Two requests with the same key at the same time: the second waits for the first to commit, then returns the same result.
  Neither fails.

## Queue topology

Queue `notification.subscription-granted.v1`, retry queue `….retry` (30 second delay), dead-letter queue `….dlq`, at most
5 retries. Both Access and Notification declare it from `SubscriptionGrantedTopology` in `common-events`.

## What Notification does

1. Refuses an event that cannot be read or breaks the contract (a `message-id` that is not the `eventId`, or an event type
   other than the routing key): it goes to the dead-letter queue without a retry.
2. Stores one notification per `eventId`: `recipient` = `userId`, `actor` = `userId`, `eventType` =
   `access.subscription-granted.v1`.
3. Pushes `{ id, actorId, eventType, createdAt }` to the learner on `/user/queue/notifications`. A failed push loses nothing:
   the inbox is the source of truth.
4. Treats any other failure as temporary and retries it.

The notice is not subject to the community notification preferences.
