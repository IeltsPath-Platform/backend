# `user.deleted.v1`

User Service publishes this event on `user.events` when a user deletes their own account via `DELETE /api/users/me`.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id used for consumer idempotency. |
| `eventType` | string | `user.deleted.v1`. |
| `occurredAt` | ISO-8601 instant | Account deletion timestamp. |
| `userId` | UUID | Deleted account id. |

No PII (email, display name, phone number) is included. Correlation id and trace context are carried in AMQP headers (`x-correlation-id`, `traceparent`).
