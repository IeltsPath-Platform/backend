# Password reset API v1

## Request reset

`POST /auth/forgot-password` is public and belongs to the Gateway `auth` rate-limit tier.

```json
{ "email": "learner@example.com" }
```

Every syntactically valid JSON request receives HTTP 200 with the same body, whether the email is unknown, invalid, inactive, or in cooldown:

```json
{ "message": "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi." }
```

Only `ACTIVE` accounts receive a reset link. A request within 60 seconds of the account's previous reset request creates no new token or event. A later request invalidates earlier unused reset tokens. The token is single-use and expires after 15 minutes. Accounts that signed up with Google can use this endpoint to create a password.

## Apply reset

`POST /auth/reset-password` accepts `{ "token": "...", "newPassword": "..." }`. The token is consumed once and all active refresh tokens are revoked. A non-`ACTIVE` account cannot reset its password.

The response does not reveal whether a reset email was delivered. Invalid JSON may still receive the standard request-validation error.
