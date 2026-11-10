# `user.google-account-linked.v1`

User Service publishes this security notice on `user.events` when linking Google clears an existing password. Notification sends it to the account email; the event is not emitted for a link that does not clear a password.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id. |
| `eventType` | string | `user.google-account-linked.v1`. |
| `occurredAt` | ISO-8601 instant | Link time. |
| `userId` | UUID | Account owner. |
| `email` | string | Notice recipient. |
| `displayName` | string or null | Greeting name. |

The event contains no Google credential or password material.
