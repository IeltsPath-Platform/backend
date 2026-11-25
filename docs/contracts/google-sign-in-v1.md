# Google sign-in v1

## Request

`POST /auth/google` is a public, rate-limited authentication endpoint. It does not require a bearer token.

```json
{ "idToken": "<Google ID token>" }
```

`idToken` is required and limited to 4096 characters. The browser and mobile clients send the signed Google ID token;
the User Service verifies it against Google's JWKS before looking up any account.

Before requesting a Google ID token, clients may call public, rate-limited `POST /auth/google/nonce`:

```json
{ "nonce": "<43-character base64url value>", "expiresInSeconds": 300 }
```

The response is `Cache-Control: no-store`. Include the nonce in the Google Identity Services request (`nonce`) or Android
Credential Manager request (`setNonce`). The resulting signed ID token must carry the same nonce; each value can be used
once. The server stores only its SHA-256 hash. `APP_AUTH_GOOGLE_REQUIRE_NONCE` defaults to `false` during rollout; when
enabled, tokens without an issued nonce are rejected. Google ID tokens older than five minutes plus 60 seconds of clock
tolerance, more than 60 seconds in the future, or missing `iat` are rejected with the same generic 401 message. The age
check can be disabled with `APP_AUTH_GOOGLE_MAX_TOKEN_AGE_SECONDS=0`.

## Responses

- `200`: same `AuthTokenResponse` body and auth cookies as `POST /auth/login`.
- `400`: missing, blank, or oversized `idToken`.
- `401`: invalid/expired token or account that is not `ACTIVE` (messages are localized in Vietnamese).
- `403 GOOGLE_SIGNIN_NOT_ALLOWED`: account has any role other than `CUSTOMER`.
- `403 GOOGLE_EMAIL_NOT_SUPPORTED`: address is not `@gmail.com`, `@googlemail.com`, or a Workspace address whose `hd`
  claim matches the email domain.
- `409 GOOGLE_ACCOUNT_CONFLICT`: that user already has a different Google identity linked.
- `429`: Gateway `auth` tier limit exceeded.
- `503 GOOGLE_SIGNIN_UNAVAILABLE`: no `GOOGLE_CLIENT_IDS` are configured or Google's signing keys cannot be fetched.

Google sign-in errors use Vietnamese messages while keeping these status codes and stable `code` values.
Invalid, expired, stale, or replayed Google ID tokens use HTTP 401, top-level code `GOOGLE_ID_TOKEN_INVALID`, and one
generic Vietnamese message so clients cannot distinguish the rejection reason.

## Account matching and sessions

The service looks up `(GOOGLE, sub)` first, then the verified email, and creates a `CUSTOMER` account if neither exists.
An already-linked `sub` may sign in without checking the current email domain, and the stored email is not updated from the token.
For a new `sub`, email authority is required before creating an account or linking by email.
Google has already confirmed the email (`email_verified`), so linking to an existing account that was not yet verified marks
it verified and publishes `user.email-verification-state.v1` with `verified=true`; a newly created Google account publishes
the same state.

When the existing account was **unverified** and has a password, that password may have been set by someone other than the
address owner, so the service clears it and revokes all active refresh tokens while holding the per-user lock. Password
sign-in then fails until the user sets a new password through the reset email flow (`users.password_cleared_at` records this, so the
reset flow stays open for it; an account created by Google has no password and gets a "use Google" mail instead, see
[password-reset-google-only-v1](password-reset-google-only-v1.md)). A **verified** account keeps its
password and can sign in with either method. A `user.google-account-linked.v1` event is published whenever a Google identity
is newly linked to an account that already existed, whether or not its password is cleared.

When a Google identity is unlinked, User-service publishes `user.google-account-unlinked.v1` through the outbox and
Notification emails the account owner. See [user-google-account-unlinked-v1](user-google-account-unlinked-v1.md).

An access token issued before linking remains valid until it expires (at most one hour). Access tokens are not revoked by
this flow. Unlinking the only login identity remains prohibited by the existing OAuth unlink rule.

The service checks `iss`, `aud`, optional `azp`, `exp`, `iat`, the RS256 signature, and `email_verified`. A client ID found in
`aud` must be listed in comma-separated `GOOGLE_CLIENT_IDS`. Google Workspace addresses are accepted only when `hd`
matches their domain.

## Mobile clients

Android should obtain its ID token through Credential Manager using the **web client ID** as `serverClientId`, then
send the token to this endpoint. Add every client ID that may appear in `aud` to `GOOGLE_CLIENT_IDS`.
