# User management API v1

Base path: `/api/users`. Every endpoint in this contract requires the `ADMIN` role.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/users` | List a filtered page of users. |
| GET | `/api/users/admin/all` | Read the complete user list for the bounded system overview aggregate. |
| POST | `/api/users` | Create a user with `email`, `password`, `fullName`, optional `phoneNumber`, and optional `roles`. |
| GET | `/api/users/{id}` | Get a user. |
| PUT | `/api/users/{id}` | Update `fullName` and optional `phoneNumber`. |
| PUT | `/api/users/{id}/roles` | Replace the user's roles with a non-empty `roles` set. |
| PUT | `/api/users/{id}/status` | Change status to `ACTIVE`, `INACTIVE`, `LOCKED`, or `PENDING_VERIFY`. |
| DELETE | `/api/users/{id}` | Deactivate the user by setting status to `INACTIVE`. |
| GET | `/api/users/{id}/profile` | Read the learner profile. |
| PUT | `/api/users/{id}/profile` | Update the learner profile. |

User responses contain `id`, `email`, `fullName`, `phoneNumber`, `status`, `roles`, `createdAt`, `updatedAt`, and `hasPassword`.
`hasPassword` is `true` when a password hash exists; the hash is never returned.
The create request defaults an omitted or empty role set to `CUSTOMER`.

### User list pagination

Compared with the previous contract, `GET /api/users` returned an array; it now returns `{ "content": [...], "page": 0, "size": 25, "totalElements": 0, "totalPages": 0 }`.
`page` is zero-based. `size` defaults to 25 and must be 1–100. Optional `role`, `status`, and `search` filters are applied in the database; `search` matches email or full name without case sensitivity. Results are ordered by creation time and then `id` for a stable page boundary. Invalid page parameters return HTTP 400 with a Vietnamese message.

The system overview uses `GET /api/users/admin/all` to calculate totals across all accounts. This endpoint retains the existing user response fields and is restricted by the same `ADMIN` role.

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
