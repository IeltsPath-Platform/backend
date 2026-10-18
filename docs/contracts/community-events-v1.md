# Community events v1

Approved 2026-10-16. Community Service owns these events and writes an outbox row in the same transaction as the interaction. Notification Service consumes them into its own database. No consumer reads Community's database.

## Envelope and routing

Durable topic exchange: `community.events`. Each message uses JSON UTF-8, persistent delivery, and AMQP `message_id` equal to `eventId`. `eventType` equals the routing key.

| Field | Type | Meaning |
| --- | --- | --- |
| eventId | UUID | Stable across relay retries; consumer deduplication key. |
| eventType | string | One of the five routing keys below. |
| occurredAt | ISO-8601 instant | Committed interaction's event creation time. |
| actorId | UUID | Authenticated person performing the interaction. |
| recipientId | UUID | Owner being notified; must differ from actorId. |
| postId | UUID or null | Destination post for post/comment interactions. |
| commentId | UUID or null | Newly created comment/reply, otherwise null. |

| Routing key | Recipient | Required reference |
| --- | --- | --- |
| community.post-commented.v1 | Post author | postId, commentId |
| community.comment-replied.v1 | Author of the comment actually replied to, even when UI flattens replies under the root | postId, commentId |
| community.comment-mentioned.v1 | Mentioned visible user; one event per distinct user in the comment | postId, commentId |
| community.post-reacted.v1 | Post author | postId |
| community.user-followed.v1 | Followed user | actorId for profile navigation; postId/commentId null |

Self-interactions do not emit notifications. A comment may mention at most five distinct visible existing users; mentioning the author creates no notification. Repeated mention tokens for the same user in one comment produce one event. When a comment mentions a user who is also the post author or the parent comment author, the system prioritizes the mention event (`community.comment-mentioned.v1`) and suppresses `community.post-commented.v1` or `community.comment-replied.v1` for that recipient, ensuring each recipient receives at most one notification per comment. Notification storage also enforces uniqueness by `(commentId, recipientId)` for mention events. Repeating the same reaction or follow without changing state emits no new event. Removing a reaction or follow does not retract delivered notifications. A changed reaction is a new interaction. Comment/reply/mention events contain identifiers only, never body text, private profile data, credentials or tokens.

## Delivery guarantees

- Relay claims only committed rows, uses publisher confirms and mandatory routing, and marks published only after successful routing confirmation. Broker failures leave retryable rows. A crash between broker confirmation and database commit may duplicate a delivery.
- Consumer uses a unique eventId in the notification database, applies persistence transactionally, and ACKs only after commit. Redelivery has no duplicate notification or unread-count increment.
- Transient failures use bounded delayed retries. Malformed/unsupported events and exhausted deliveries go to a durable DLQ. Parking must be confirmed before acknowledging the original message.
- STOMP delivery happens after commit. A disconnected browser or push failure never rolls back the notification. Reconnecting clients reconcile the REST list and unread count.
- Consumers must validate routing/event type consistency, UUID fields, timestamp and required references. Logs contain IDs and failure types, never raw payloads.

## Notification preferences

Notification Service exposes authenticated `GET /api/notifications/preferences` and `PUT /api/notifications/preferences/{eventType}` with body `{"enabled": boolean}`. The response lists every supported event type. A recipient with no saved preference is treated as enabled for every type. Disabling a type causes the consumer to ACK the event without inserting a notification or sending a WebSocket push; enabling it again applies to subsequently consumed events. Preference rows are scoped by recipient UUID and event type in Notification Service's database.

## Deployment

Start Notification's topology/consumer before enabling Community's relay. Until a bound queue exists, mandatory publishing must retain events in the outbox. Each service uses its own database and existing RabbitMQ credentials from runtime configuration. No new secret values belong in this contract.

## WebSocket authentication

Browser opens `/ws/notifications` with subprotocols `v12.stomp` and `bearer.<external JWT>`. Gateway validates the external token through its existing resource-server validators, strips the credential-bearing subprotocol and forwards a short-lived internal JWT in Authorization. The response negotiates only `v12.stomp`. No JWT query parameter is supported.

Notification Service validates the internal JWT during the HTTP handshake and carries its UUID principal into STOMP. Clients may subscribe only to `/user/queue/notifications`; client SEND, arbitrary broker destinations, and other users' destinations are denied. Origin must match configured frontend origins. REST remains the durable source of truth.

Implementation references: [Spring STOMP authentication](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html), [STOMP authorization](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authorization.html), [StompJS client lifecycle](https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html).
