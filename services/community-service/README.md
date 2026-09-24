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

## Internal architecture

- `api`: HTTP controllers, request/response DTOs, validation, and mapping between authenticated identity and application
  input.
- `application`: one use case per post, comment, moderation, reaction, or listing operation; commands, queries, and
  result records.
- `domain`: post/comment aggregates, value objects, domain exceptions, and repository contracts.
- `infrastructure`: JPA entities and repositories, persistence mappers, and adapters that implement domain repository
  contracts.

Controllers do not return domain aggregates. Application inputs receive the verified user ID and admin capability from
the API boundary; they do not read security context or raw identity headers themselves.

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
