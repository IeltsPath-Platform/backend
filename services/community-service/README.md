# Community Service

Community Service owns posts, threaded comments, reactions, moderation, and the isolated PostgreSQL database `community_db`.

## Contract

- Route root: `/api/community`
- Categories: `GENERAL`, `QUESTION`, `DISCUSSION`
- Reactions: `LIKE`, `LOVE`
- Statuses: `ACTIVE`, `HIDDEN`, `DELETED`
- Pagination: zero-based, default 20, maximum 100
- Post feed: `createdAt DESC, id DESC`
- Comments: flat page ordered by `createdAt ASC, id ASC`; clients assemble trees using `parentCommentId`
- Identity comes only from the gateway-signed internal JWT. Responses expose `authorId` and do not query User Service.

Authenticated users create content and may edit or soft-delete only their own active content. `ADMIN` may hide or restore non-deleted content.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST / GET | `/api/community/posts` | Create or page active posts |
| GET / PUT / DELETE | `/api/community/posts/{id}` | Read, edit, or soft-delete a post |
| PUT | `/api/community/posts/{id}/status` | Admin moderation |
| POST / GET | `/api/community/posts/{id}/comments` | Create or page comments |
| PUT / DELETE | `/api/community/comments/{id}` | Edit or soft-delete a comment |
| PUT | `/api/community/comments/{id}/status` | Admin moderation |
| PUT / DELETE | `/api/community/posts/{id}/reactions/{LIKE|LOVE}` | Idempotently add/remove a reaction |

## Local runtime

Set the same required variables as the root Compose stack (`POSTGRES_PASSWORD`, `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET`), then run `docker compose up -d --build`. Docker starts `community-db` from `postgres:15-alpine`; Flyway initializes the schema when Community Service starts.
