# `access.subscription-expiry-notice.v1`

Access Service emits this event once per subscription, stage and end date on its access events exchange: a renewed or extended subscription has a new `endsAt` and gets the stages again. The scheduled producer emits `SEVEN_DAYS`, `ONE_DAY`, and `EXPIRED` stages. Access checks determine expiry from `endsAt`; the subscription row may already be marked `EXPIRED` by the lapse job when the final stage is emitted. `EXPIRED` is not emitted for a subscription that ended more than 7 days ago, nor for an already marked one whose owner has an active or later subscription. The mail consumer drops an about-to-end notice whose end has passed and an `EXPIRED` notice for an end older than 7 days; it does not detect a later renewal or extension.

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

Notification uses queue `notification.subscription-expiry-notice.v1` with `.retry` and `.dlq`; Access declares that queue too, so a notice is kept before Notification has started. The in-app inbox queue exists only after Notification has started once. Delivery is idempotent by event and template. Consumers do not make a synchronous call to Access or User Service.
