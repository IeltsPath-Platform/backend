# Notification push operations

## Safe rollout

1. Deploy the additive Notification Service migrations, device API, and worker code with both `NOTIFICATION_PUSH_ENQUEUE_ENABLED=false` and `NOTIFICATION_PUSH_WORKER_ENABLED=false`.
2. Configure the environment's Firebase project and minimum-permission workload identity or read-only mounted credentials. Keep credential files out of source, images, and command output.
3. Release the Android client with the matching Firebase application/package ID and notification channel. Verify authenticated device registration and online logout cleanup against a test account.
4. In staging, enable enqueue only. Emit controlled events and confirm one notification plus one job per active device/version; no Firebase send occurs while the worker flag is false.
5. Enable the worker in staging and confirm delivery state, owner/version, generic payload, tap routing, queue metrics, and inbox/WebSocket behavior.
6. Enable in production gradually under the existing operational change process. Monitor queue age and delivery outcomes before increasing traffic.

The service does not backfill inbox history when enqueue is enabled. A job records FCM acceptance, not device display. An in-flight send can arrive after logout, and a provider timeout can lead to a duplicate display.

## Configuration and pause

Both flags are off by default. Configuration changes to `worker-enabled` control a conditional Spring bean, so restart Notification Service instances after changing this flag. Set worker false to stop new claims. Set enqueue false as well to stop creating jobs; existing jobs remain durable until they expire or are drained. To drain a bounded backlog, leave the worker on and disable enqueue.

The worker uses Google Application Default Credentials. In Google-hosted environments prefer workload identity. Elsewhere mount a least-privilege service-account file read-only and set `GOOGLE_APPLICATION_CREDENTIALS` to its runtime path. `FIREBASE_PROJECT_ID` can pin the intended project. Do not log or copy the credential JSON.

## Monitoring

Metrics use bounded labels only; do not add user, device, notification, or token identifiers as labels.

- `notification_push_pending`: nonterminal queued and in-flight job count.
- `notification_push_oldest_pending_age_seconds`: age of the oldest nonterminal job.
- `notification_push_deliveries_total{provider,outcome,error}`: sent, retried, parked, cancelled, or deactivated results.
- Existing Notification Service health and inbox endpoints remain independent of Firebase availability.

Application logs record delivery IDs and normalized error codes, never tokens, source payloads, provider response bodies, or credentials.

## Recovery and rollback

Pause claims by turning off the worker. Turn off enqueue to stop new work. Deploy the prior application version if required, but keep V5/V6: both migrations are additive and existing inbox data stays intact. Do not drop tables or replay historical RabbitMQ events as a push recovery method.

There is no public arbitrary-send or replay endpoint. Parked jobs need a reviewed operational replay procedure that checks expiry and current active owner/version before resetting state; do not update job status directly without that check. An accepted push cannot be recalled. Expired jobs are cancelled by the worker when it polls.

## Production gates still requiring environment/product decisions

- Verify Firebase project ID, Android package ID, enabled FCM HTTP v1, runtime identity permissions, egress, and a real Android development build.
- Confirm database access controls and encryption at rest for stored registration tokens. Field encryption requires an approved key management and rotation design before any plaintext fallback is considered.
- Connect account deletion to device deactivation and cancellation of outstanding deliveries. No account deletion hook was added to this service; complete and verify that integration before production activation.
- Agree on terminal delivery retention, inactive device retention, and any purge job. No automated purge is enabled by this change.
- Tune rate and concurrency using staging traffic and number of replicas; current batch, timeout, backoff, and TTL values are starting bounds, not measured SLAs.

## Android smoke checklist

- Login and register; rotate token; transfer an installation to another account; unregister on online logout.
- Test one user with two devices and two users switching a shared installation.
- Exercise community, grading, and subscription events in foreground/background; deduplicate native and WebSocket display by `notificationId`.
- Tap each supported notification and refetch its target through REST. Verify unread state changes only through the existing inbox API.
- Test denied OS permission, offline logout, stale token, Firebase outage, worker pause, and expiry. Inbox data must remain available throughout.
