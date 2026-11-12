# Notification Service

Notification Service owns `notification_db`, notification delivery, and its RabbitMQ consumer queues.

## Password reset email

`PasswordResetRequestedListener` consumes `user.password-reset-requested.v1` only when
`NOTIFICATION_MAIL_ENABLED=true` and `SMTP_HOST`, `MAIL_FROM`, and `FRONTEND_BASE_URL` are configured. SMTP calls happen
outside database transactions. Delivered event IDs are stored in `mail_deliveries`; email addresses and reset tokens are
not stored or logged by this service.

Local development can use Mailpit from the backend root:

```powershell
docker compose --profile dev-mail up -d mailpit
```

Configure `SMTP_HOST=mailpit`, `SMTP_PORT=1025`, `SMTP_AUTH=false`, `SMTP_STARTTLS=false`,
`MAIL_FROM=no-reply@ieltspath.local`, and `FRONTEND_BASE_URL=http://localhost:5173`. Set
`NOTIFICATION_MAIL_ENABLED=true` to register the consumer. Mailpit UI: `http://localhost:8025`.

For Gmail SMTP, use `smtp.gmail.com:587` with STARTTLS and an App Password in `SMTP_PASSWORD`; keep all credentials in
the ignored root `.env` file. See [`docs/contracts/password-reset-requested-v1.md`](../../docs/contracts/password-reset-requested-v1.md)
for the event contract.
