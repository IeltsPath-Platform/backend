# common-events

Message contracts and broker topology shared by the services. Each contract is a record whose name carries its version
(`GradingCompletedV1`, `AssessmentCompletedV2`); the contract document lives in `docs/contracts/`.

## Changing a contract

- **Adding an optional field:** keep the version. Consumers ignore fields they do not know: the JSON mapper must not fail on
  unknown properties, and each contract has a test that reads a sample with an extra field.
- **Renaming, removing, retyping, or changing the meaning of a field:** create the next version (`...V{n+1}`) with a new
  routing key. Publish both versions until every consumer has moved, then stop publishing the old one.
- **Every contract has a golden test:** a sample JSON in the test that must match what the record serializes to. A change
  that is not deliberate fails here first.

## Replaying dead-lettered messages

A message parked on a dead-letter queue is not retried by itself. After the cause is fixed, an administrator can send up to
500 messages per call back to the queue they came from:

```
GET  /api/notifications/admin/dead-letters                       # count per dead-letter queue
POST /api/notifications/admin/dead-letters/{queue}/replay?max=50  # {moved, remaining}
```

Replace `notifications` with the service's prefix (`/api/access/...`, `/api/learning/...`) for its own queues. Only queues the
service declares are accepted; any other name returns 404.

- Fix the cause first. A message that fails again returns to its dead-letter queue, so replaying without a fix only repeats
  the failure.
- The replay takes each message without auto-acknowledgement and acknowledges it only after the broker confirms the
  republished copy. A broker failure leaves the message where it was.
- Each call is logged with the queue name and the counts, never the message body.
- A replay can deliver a message a second time, when the first delivery was processed but its acknowledgement was lost. A
  message that reaches the same queue again must be handled by an idempotent consumer, keyed on the event id.
- A route that sends messages back through a shared exchange (`DeadLetterRoute.byOriginalRoutingKey`) needs the original
  routing key. The consumer records it in the `x-original-routing-key` header when it parks the message. A message parked
  before that header existed fails its replay and stays in the dead-letter queue.
