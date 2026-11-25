# Google account unlinked event v1

Routing key: `user.google-account-unlinked.v1` on the durable `user.events` topic exchange.

```json
{
  "eventId": "uuid",
  "eventType": "user.google-account-unlinked.v1",
  "occurredAt": "2026-11-25T10:00:00Z",
  "userId": "uuid",
  "email": "person@example.test",
  "displayName": "Person"
}
```

User-service writes the event to its outbox in the same transaction as Google unlink. Notification owns a durable queue,
retry queue, and dead-letter queue and sends one idempotent account-security email per event. The event contains no token,
password, Google subject, or other authentication secret.
