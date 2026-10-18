# Access points contract v1

Access owns point balances and enforces the daily earning limit. All `/internal/**` routes require a verified internal
service JWT; the Gateway blocks them from public clients.

## Earning points

Points are earned only through the `points.grant.requested` message below. Access exposes no HTTP route that credits
points: `/internal/**` accepts any verified internal JWT, so a credit route would let any service, or anyone holding a
learner's internal token, mint unlimited points. `POST /internal/access/points/credit` does not exist and returns `404`.

`reason` is one of `STREAK_MILESTONE`, `KP_MASTERED`, `DAILY_QUEST`, or `BADGE`. `amount` is 1 through 100,
`referenceId` is 1 through 100 characters, and the idempotency key (the grant ID) is at most 120 characters.

The daily learning-reward limit is shared by `STREAK_MILESTONE`, `KP_MASTERED`, and `DAILY_QUEST`, uses a rolling 24 hours (not a calendar day), and
defaults to 20 points. `BADGE` grants are exempt from the limit and do not count toward the daily total. Configure the limit
with `access.points.daily-earn-limit` or `ACCESS_POINTS_DAILY_EARN_LIMIT`. A partially granted request is `capped`;
when no points remain, nothing is granted and no ledger entry is written. Replaying the same grant with the same user,
amount, reason, and reference returns the original result without crediting again. Reusing the grant ID with different
request data is a conflict.

## Learning reward messages (C6)

Access consumes `points.grant.requested` on durable queue `access.points-grant`, bound to durable topic exchange
`learning.points` with routing key `points.grant.requested`.

Learning publishes with publisher confirms and mandatory routing. A grant counts as sent only after the broker confirms
it and no queue returned it as unroutable; the queue is declared by Access, so a grant sent before Access has declared
it is returned, stays pending without using up one of its sends, and goes out on a later run. A grant given up after
its last send is logged as a warning with its id and the reason.

```json
{
  "grantId": "<uuid>",
  "userId": "<uuid>",
  "amount": 1,
  "reason": "DAILY_QUEST",
  "referenceId": "<reason-specific-reference>"
}
```

The reason set and amount/reference limits are the ones above. Access uses `grantId` as the idempotency key.
The daily limit applies to the three non-badge reasons; `BADGE` grants are always full (within normal request validation)
and are excluded from the amount counted for the rolling 24 hours.
It publishes a result to `learning.points` with routing key `points.grant.settled`:

```json
{
  "grantId": "<uuid>",
  "status": "GRANTED",
  "error": null
}
```

## Failures, retry and dead letters

The grant queue `access.points-grant` dead-letters to `access.points-grant.dlq` (through the direct exchange
`access.points-grant.dlx`), and the consumer never puts a refused message back on the queue. A payload that cannot
be read is answered `FAILED` as below. A grant that violates a database constraint is refused and dead-lettered. Any
other failure is treated as temporary: the message is parked on `access.points-grant.retry`, which holds it for 30
seconds and hands it back to the grant queue, up to 5 times; after that it is dead-lettered. The count travels in the
`x-retry-count` header. Crediting is idempotent per grant ID, so handling a message again never pays twice.

Queue arguments cannot change once a queue exists. A broker that already holds `access.points-grant` (or Learning's
`learning.points-grant-settled`) declared without the dead-letter arguments answers `PRECONDITION_FAILED` at start-up
until that queue is deleted; neither queue exists on `main`, so only development brokers that ran this branch need it.

`status` is `GRANTED`, `CAPPED`, or `FAILED`. Invalid payloads, including an unknown reason, return `FAILED` with
`error: "INVALID_PAYLOAD"`. Reusing a grant ID with different request data returns `FAILED` with
`error: "GRANT_ID_CONFLICT"`. A repeated identical grant is safe and does not add points twice; the settled status
reflects the original credit result.
