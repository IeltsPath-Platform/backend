# Password reset requested event v1

Producer: User Service transactional outbox. Consumer: Notification Service.

- Exchange: `user.events`
- Routing key and event type: `user.password-reset-requested.v1`
- Queue: `notification.password-reset-requested.v1`
- Retry queue: `notification.password-reset-requested.v1.retry` (30 seconds, at most five attempts)
- Dead-letter queue: `notification.password-reset-requested.v1.dlq`

Payload fields: `eventId`, `eventType`, `occurredAt`, `userId`, `email`, `displayName`, `resetToken`, `expiresAt`.
`eventId` is also the AMQP message ID and the notification idempotency key. The outbox row is written with the reset-token hash in the same transaction; the relay waits for publisher confirmation and clears the payload after publication. The broker message expires when the token does. The consumer skips expired events and events already present in `mail_deliveries`.

The raw reset token is transiently present in the outbox and broker so Notification can create the reset URL. It is not logged or stored in `mail_deliveries`; User Service stores only its hash. Anyone with read access to the broker or outbox can obtain an unexpired token during its 15-minute lifetime. Access to those systems must remain restricted.
