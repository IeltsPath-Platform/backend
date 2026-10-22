# User management API v1

Base path: `/api/users`. Every endpoint in this contract requires the `ADMIN` role.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/users` | List all users. The response is an unpaginated array; clients may filter it locally. |
| POST | `/api/users` | Create a user with `email`, `password`, `fullName`, optional `phoneNumber`, and optional `roles`. |
| GET | `/api/users/{id}` | Get a user. |
| PUT | `/api/users/{id}` | Update `fullName` and optional `phoneNumber`. |
| PUT | `/api/users/{id}/roles` | Replace the user's roles with a non-empty `roles` set. |
| PUT | `/api/users/{id}/status` | Change status to `ACTIVE`, `INACTIVE`, `LOCKED`, or `PENDING_VERIFY`. |
| DELETE | `/api/users/{id}` | Deactivate the user by setting status to `INACTIVE`. |
| GET | `/api/users/{id}/profile` | Read the learner profile. |
| PUT | `/api/users/{id}/profile` | Update the learner profile. |

User responses contain `id`, `email`, `fullName`, `phoneNumber`, `status`, `roles`, `createdAt`, and `updatedAt`.
The create request defaults an omitted or empty role set to `CUSTOMER`.

## Role and account rules

- The supported roles are `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, and `SALES_STAFF`.
- A user may have `ADMIN` with `CUSTOMER` or `SALES_STAFF`, but may not combine `ADMIN` with `CONTENT_AUTHOR` or `EXAMINER`. This is validated when creating a user or replacing roles. Existing accounts are not changed automatically, and sign-in is not blocked by this rule.
- An administrator cannot remove their own `ADMIN` role, change their own status away from `ACTIVE`, or deactivate their own account.
- The last active administrator cannot lose `ADMIN`, be changed to `LOCKED` or `INACTIVE`, or be deactivated. Other status transitions remain subject to the self-protection rule.
- Other role sets and statuses remain available to administrators subject to the endpoint validation rules.

Rule failures use the standard user-service error envelope and include `details.code`:

| HTTP | `details.code` | Meaning |
|---|---|---|
| 422 | `ROLE_CONFLICT` | `ADMIN` was combined with `CONTENT_AUTHOR` or `EXAMINER`. The message identifies the conflicting pair. |
| 409 | `SELF_ADMIN_REMOVAL` | The caller attempted to remove their own `ADMIN` role. |
| 409 | `SELF_ACCOUNT_CHANGE` | The caller attempted to change their own status away from `ACTIVE` or deactivate their own account. |
| 409 | `LAST_ADMIN` | The operation would leave no active administrator. |

## Existing conflicting accounts

The rule applies on user creation and role replacement. Operators can inspect legacy conflicts with:

```sql
SELECT u.email, string_agg(r.name, ',')
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
GROUP BY u.email
HAVING bool_or(r.name = 'ADMIN')
   AND bool_or(r.name IN ('CONTENT_AUTHOR', 'EXAMINER'));
```

No automatic data cleanup is performed.
