# `access.subscription-expiry-notice.v1`

Access Service emits this event once per subscription and stage on its access events exchange. The scheduled producer emits `SEVEN_DAYS`, `ONE_DAY`, and `EXPIRED` stages; subscription status remains `ACTIVE`, and access checks determine expiry from `endsAt`.

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Unique stage event id. |
| `eventType` | string | `access.subscription-expiry-notice.v1`. |
| `occurredAt` | ISO-8601 instant | Event creation time. |
| `userId` | UUID | Subscription owner. |
| `subscriptionId` | UUID | Subscription that is ending or ended. |
| `stage` | string | `SEVEN_DAYS`, `ONE_DAY`, or `EXPIRED`. |
| `endsAt` | ISO-8601 instant | Subscription end time. |
| `email` | string | Notice recipient snapshot. |
| `displayName` | string or null | Greeting name snapshot. |

Notification uses queue `notification.subscription-expiry-notice.v1` with `.retry` and `.dlq`; delivery is idempotent by event and template. Consumers do not make a synchronous call to Access or User Service.
