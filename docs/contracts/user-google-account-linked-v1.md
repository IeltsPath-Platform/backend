# `user.google-account-linked.v1`

User Service publishes this security notice on `user.events` when a Google identity is newly linked to an existing account, whether or not the existing password is cleared. Notification sends it to the account email. The event is not emitted when a new account is created through Google sign-in.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id. |
| `eventType` | string | `user.google-account-linked.v1`. |
| `occurredAt` | ISO-8601 instant | Link time. |
| `userId` | UUID | Account owner. |
| `email` | string | Notice recipient. |
| `displayName` | string or null | Greeting name. |

The event contains no Google credential or password material.
