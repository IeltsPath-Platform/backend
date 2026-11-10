# `user.email-verification-requested.v1`

User Service publishes this event on `user.events` after registration or a request to verify an email. Notification consumes queue `notification.email-verification-requested.v1` and sends the verification link. Queue retry and dead-letter handling follow `EmailVerificationRequestedTopology` in `common-events`.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique event id; delivery is deduplicated by event and template. |
| `eventType` | string | `user.email-verification-requested.v1`. |
| `occurredAt` | ISO-8601 instant | Request time. |
| `userId` | UUID | Account owner. |
| `email` | string | Verification recipient. |
| `displayName` | string or null | Greeting name. |
| `verificationToken` | string | Transient raw token used to construct the verification link; User Service stores only its hash. |
| `expiresAt` | ISO-8601 instant | Token expiry. |

The token must not be logged or persisted by Notification. Expired requests are ignored.
