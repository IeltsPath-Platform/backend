# `user.password-changed.v1`

User Service publishes this security notice on `user.events` after a password change or a successful password reset. Notification sends it to the address on the account. It has no user preference switch.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id. |
| `eventType` | string | `user.password-changed.v1`. |
| `occurredAt` | ISO-8601 instant | Change time. |
| `userId` | UUID | Account owner. |
| `email` | string | Notice recipient. |
| `displayName` | string or null | Greeting name. |

No password, password hash, reset token, or IP address is included.
