# Course unlock and human grading failure recovery

## Course unlock

Learning records a `PENDING` unlock intent with the caller's `Idempotency-Key` before calling Access to debit Points.
The debit uses the same key, and activation records the returned ledger entry. If the debit times out, the intent stays
pending; retrying or running the reconciler checks the same key before attempting another debit. If the process stops
after debit but before activation, the reconciler activates the existing intent from the existing ledger result.
Insufficient balance cancels the pending intent.

The behavior is covered by `UnlockCourseUseCaseTest` and `ReconcilePendingCourseUnlocksTest`, including pending intent
replay, timeout recovery, post-debit activation recovery, and insufficient balance cancellation.

## Human grading

Assessment uses the essay submission ID as the stable credit key. It checks whether the job already exists before
spending a credit, and it writes the job in a separate short transaction after the Access call. If job creation fails
after this request spent a credit, the use case refunds that credit only when it can confirm that no job exists. If job
existence is uncertain or the refund fails, it preserves the credit state and a repeated request reuses the original
credit instead of charging twice.

The behavior is covered by `RequestHumanGradingUseCaseTest` and `HumanGradingRequestIntegrationTest`, including duplicate
requests, failed job creation, uncertain job persistence, refund failure, and retry.

## Admin subscription grant

The Access grant endpoint requires `Idempotency-Key`. The key reserves one request hash and subscription ID in the same
database transaction as the subscription and `SubscriptionGranted` outbox event. Repeating the same key and payload
returns the saved subscription without extending it or writing another event. Reusing the key with a different payload
returns conflict. A concurrent request that loses the unique-key reservation rolls back its subscription and outbox
changes.

`GrantSubscriptionIdempotencyTest` covers same-key replay, payload conflict, missing key, concurrent requests, and one
outbox event for the replayed grant.
