# `user.email-verification-state.v1`

User Service publishes this state/contact snapshot on `user.events`. Notification consumes it to maintain its local contact record; other consumers use the newest `occurredAt` value as the current state. This event does not request an email.

## Payload

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id. |
| `eventType` | string | `user.email-verification-state.v1`. |
| `occurredAt` | ISO-8601 instant | State/contact update time; consumers apply the latest value. |
| `userId` | UUID | Account owner. |
| `verified` | boolean | Whether the current email is verified. |
| `email` | string | Current contact email. |
| `displayName` | string or null | Display name for notices. |

The event contains no verification token or credential. Consumers must not call User Service to obtain the contact details.
