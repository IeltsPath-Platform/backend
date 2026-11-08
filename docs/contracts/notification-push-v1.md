# Notification push v1

This contract adds Android Firebase Cloud Messaging as a best-effort delivery channel. The PostgreSQL notification inbox and authenticated REST APIs remain authoritative. Existing RabbitMQ event envelopes, community preferences, unread counts, and WebSocket behavior do not change.

## Device registration

Both endpoints require the authenticated internal JWT forwarded by the API Gateway. The service derives `userId` from `CurrentUserProvider`; clients must not submit an owner ID.

| Method and path | Request | Success |
| --- | --- | --- |
| `PUT /api/notifications/devices/{installationId}` | `{ "provider": "FCM", "platform": "ANDROID", "token": "…", "appVersion": "…" }` | `204 No Content` |
| `DELETE /api/notifications/devices/{installationId}` | No body | `204 No Content` |

`installationId` is a client generated UUID stable for one app installation. The token is required, nonblank, and at most 4096 characters; `appVersion` is optional and at most 128 characters. Invalid requests return `400` without echoing the token. Missing authentication returns `401`; a principal rejected by existing service authorization returns `403`.

PUT is idempotent. Repeating the same active owner, installation, and token refreshes registration metadata without changing `registrationVersion`. A token, owner, platform, or active-state change increments the version. An installation has one current owner, and an active token has one installation. A token collision moves the active mapping to the most recent successful registration. DELETE is owner scoped and returns `204` for both unknown and non-owned installation IDs; it does not reveal or alter another user's registration.

Mobile should register or refresh after login and token rotation, and unregister before an online logout. Offline logout may leave a mapping until a later registration or cleanup; no push notification is an authorization grant. The app must fetch protected content again through REST when a notification is tapped.

## Queue and delivery semantics

The notification row and one push delivery per active Android device/version are written in the same PostgreSQL transaction. A duplicate event creates neither a second notification nor more deliveries. A registration created after a notification does not receive historical inbox items. Delivery stores identifiers and a registration version; it does not duplicate the token or private post/comment text.

Enqueue and worker flags default to false. With enqueue enabled, a delivery expires 24 hours after the notification was created. Worker claims are leased and fenced by a claim ID; FCM calls run outside database transactions. Retries use bounded exponential backoff and jitter, with a 60-second minimum for quota errors. There are at most eight claims. Expired bindings are cancelled; unregistered tokens are disabled only when owner, version, and token hash still match the send snapshot. Permanent payload errors are parked.

`SENT` means FCM accepted the request. It does not mean Android displayed the notification or the user read it. A timeout after provider acceptance can cause a duplicate. Mobile should deduplicate foreground display by `notificationId`, avoid showing a second native banner when the corresponding WebSocket item is already visible, and never mark the inbox item read just because FCM accepted it.

Push content uses a generic title/body and data identifiers (`notificationId`, `eventType`, and available actor/post/comment/grading IDs). It excludes source text, tokens, credentials, and private profile data. Community notification preferences continue to govern whether a community notification is created; grading and subscription policies are unchanged.

## Client handling

The Android client must use a native FCM registration token, request notification permission where the OS requires it, and create the Android notification channel configured by `FIREBASE_ANDROID_CHANNEL_ID` (default `default`). Do not use an Expo push token or Expo Go for remote FCM validation. On tap, route using identifiers and refetch the appropriate resource with the user's current authenticated session. If access has changed, show the existing authorization result without exposing cached private content.

## Configuration

| Setting | Default | Meaning |
| --- | --- | --- |
| `NOTIFICATION_PUSH_ENQUEUE_ENABLED` | `false` | Snapshot active devices when a new notification is inserted |
| `NOTIFICATION_PUSH_WORKER_ENABLED` | `false` | Start the leased FCM worker and initialize Firebase lazily |
| `NOTIFICATION_PUSH_POLL_INTERVAL` | `PT1S` | Delay between worker polls |
| `FIREBASE_PROJECT_ID` | empty | Optional explicit Firebase project ID; ADC may provide it |
| `FIREBASE_ANDROID_CHANNEL_ID` | `default` | Android notification channel name configured by the app |
| `GOOGLE_APPLICATION_CREDENTIALS` | unset | Optional path to a mounted service-account credential for non-Google environments; prefer workload identity/ADC |

Never put Firebase credentials in source, container images, or client apps. When the worker flag is false, Firebase ADC is not looked up. When credentials are unavailable, the worker leaves jobs queued and the inbox/WebSocket continue to operate.

## Operations and rollout

See [notification push operations](../notification-push-operations.md) for staged activation, metrics, pause, rollback, and production gates. The API Gateway already routes authenticated `/api/notifications/**` paths.
