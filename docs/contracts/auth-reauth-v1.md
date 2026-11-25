# Reauthentication API v1

All routes require an authenticated access token. Reauthentication tokens are random 32-byte base64url values, expire
after 300 seconds, are bound to the user and current `sid`, and can be consumed once. Redis stores only the SHA-256 hash.

## Create a token

`POST /api/users/me/reauth` accepts exactly one credential:

```json
{ "currentPassword": "..." }
```

or

```json
{ "googleIdToken": "..." }
```

Password verification shares the change-password attempt counter and temporary lock. Google verification applies the
configured ID-token age and nonce checks. The Google subject must match the Google identity linked to the current user.

Successful response:

```json
{ "reauthToken": "...", "expiresInSeconds": 300, "method": "PASSWORD" }
```

The `method` is `PASSWORD` or `GOOGLE`.

Missing, invalid, expired, used, or out-of-scope tokens return 403 `REAUTH_REQUIRED`. A different or unlinked Google
identity returns 403 `REAUTH_ACCOUNT_MISMATCH`. Both use Vietnamese messages and top-level `code` values.
If Redis cannot issue a token, the endpoint returns 503 `REAUTH_UNAVAILABLE`.

## Sensitive operations

`DELETE /api/users/me/oauth/{provider}` optionally accepts `X-Reauth-Token`. Reauthentication is enforced when
`APP_AUTH_REAUTH_REQUIRED_FOR_UNLINK=true`; the default is `false` until the web client is deployed. Successful Google
unlink emits `user.google-account-unlinked.v1` to the user outbox and Notification sends an account notice.

`POST /api/users/me/password/initial` requires `X-Reauth-Token` with `method=GOOGLE`, plus a verified email and a strong
new password. An existing password returns 409 `PASSWORD_ALREADY_SET`. On success the password marker is cleared,
refresh and access tokens are revoked, and `user.password-changed.v1` is emitted. The caller must sign in again.
