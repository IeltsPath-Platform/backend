# `user.password-reset-google-only.v1`

User Service emits this event on its `user.events` exchange when someone asks to reset the password of an account that signs in with Google and never had a password. No reset link or token is created for it; Notification mails the owner to use Google instead.

## When it is emitted

`ForgotPasswordUseCase` emits it for an `ACTIVE` account that has no password hash, has a linked Google identity, and whose password was never removed (`users.password_cleared_at` is null). An account created by Google sign-in is the case.

An account whose password was removed when Google was linked to it (it was unverified, see [google-sign-in-v1](google-sign-in-v1.md)) has `password_cleared_at` set and keeps the normal reset flow, so its owner can choose a password again. An account with a password, or without a linked Google identity, also keeps the normal flow.

The response of `POST /auth/forgot-password` is the same in every case, so the caller cannot tell the cases apart.

## Payload

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Derived from the account and a 15-minute window (`nameUUIDFromBytes("password-reset-google-only:{userId}:{window}")`). It is also the AMQP `message_id` and the outbox row id. |
| `eventType` | string | `user.password-reset-google-only.v1`. Consumers reject any other value. |
| `occurredAt` | ISO-8601 instant | When the request was made. |
| `userId` | UUID | The account. |
| `email` | string | Address to write to. |
| `displayName` | string or null | Greeting name snapshot. |

## Consumer

Notification uses queue `notification.password-reset-google-only.v1` with `.retry` and `.dlq`; delivery is idempotent by event id and template (`password-reset-google-only`), so repeated requests inside one window send one mail. The template can be switched off with `notification.mail.templates.password-reset-google-only.enabled`. Consumers make no synchronous call to User.
