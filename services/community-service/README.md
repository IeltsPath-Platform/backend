# Community Service

Community Service owns posts, threaded comments, reactions, follows, bookmarks, and content reports in the isolated PostgreSQL database `community_db`. It serves `/api/community` through the Gateway.

## Content and feeds

- Post categories: `GENERAL`, `QUESTION`, `DISCUSSION`, `WRITING_SHARING`, `SPEAKING_PARTNER`, and `RESOURCE_TIPS`.
- Post status: `ACTIVE`, `HIDDEN`, or `DELETED`. Authors can edit and soft-delete their own active content; an `ADMIN` can hide or restore non-deleted content.
- A post body is required. Titles are optional (up to 200 characters); body is up to 20,000 characters. Target band is optional and ranges from 0 to 9 in half-band increments.
- `background` is an optional fixed key (`VIOLET`, `CRIMSON`, `MIDNIGHT`, `SUNSET`, `OCEAN`, `FOREST`, `ROSE`, or `AURORA`) accepted on create and edit. It is only valid for text-only posts with bodies up to 280 characters. Omit or send `null` to clear it when editing; responses include the key or `null`, including post details and feeds. Invalid keys return `400`. Flyway V8 adds the nullable `background_style` column and database constraints.
- Up to four image URLs are accepted (each up to 500 characters). Video URL is optional (up to 500 characters). Media must use HTTPS, have no user information or non-standard port, and match the configured image/video host allowlist (`AllowedMediaPolicy`).
- Standard feeds are zero-based, default to 20 and cap at 100. Cursor feeds default to 10 and use `(created_at, id)` ordering to avoid repeated or skipped posts across pages.
- Cursor feed search uses PostgreSQL `simple` full-text search over title and body. Search input is plain text and limited to 120 characters; category and `scope=FOLLOWING` can be combined with search and cursor pagination.
- `scope=FOLLOWING` requires authentication. A user can view another user's posts, followers, following list, and relationship state through the profile endpoints.
- Public author profiles are fetched from User Service in batches. If a profile is not returned because it is missing or hidden, the post remains readable with the existing anonymous/fallback author presentation.

## Comments and reactions

- Comments support parent/reply relationships. The paged comment endpoint can return a threaded view; replies can also be loaded by parent comment ID. Comment body is required and limited to 5,000 characters.
- A comment may mention up to five distinct visible, existing users using `@[display name](user UUID)` syntax. Unknown or hidden users are rejected. Self-mentions do not emit notifications. Mention events carry IDs, not comment text.
- Reactions are `LIKE`, `LOVE`, `HELPFUL`, and `ACCURATE`. Each user can have one reaction per post; adding/removing a reaction is idempotent.

## Following, bookmarks, and reports

- Follow/unfollow is available at `/users/{userId}/follow`; follower and following lists are cursor-paginated. A user can save and remove bookmarks at `/posts/{postId}/bookmark` and list saved posts at `/posts/bookmarked`.
- A user may report another user's post or comment once per `(reporter, content type, content ID)`. Authors cannot report their own content. PostgreSQL enforces the unique rule; a duplicate returns `409`.
- Only `ADMIN` can list or resolve the pending queue. `DISMISS` closes the selected report only. `HIDE_CONTENT` hides the reported item and marks every pending report for that same item `ACTIONED`, with the handling admin and review time, in the same transaction. Concurrent decisions serialize; the losing request receives `409` and the queue remains consistent.
- Report submission and review do not emit notification events.

## Notification events and preferences

Community writes versioned events to its transactional outbox; the relay publishes them to RabbitMQ. The notification service handles post comments, comment replies, mentions, reactions, and follows according to `docs/contracts/community-events-v1.md`. Notification preferences are owned by Notification Service, default every supported type to enabled, and suppress both persistence and push for disabled types. The consumer is idempotent. Community reports are not notification events.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST`, `GET` | `/api/community/posts` | Create posts or read a paged feed; cursor mode supports search, category, and following scope |
| `GET`, `PUT`, `DELETE` | `/api/community/posts/{id}` | Read, edit, or soft-delete a post |
| `PUT` | `/api/community/posts/{id}/status` | Admin moderation |
| `POST`, `GET` | `/api/community/posts/{postId}/comments` | Add comments or read a page; `threaded=true` returns a tree |
| `GET` | `/api/community/comments/{commentId}/replies` | Read a page of replies |
| `PUT`, `DELETE` | `/api/community/comments/{id}` | Edit or soft-delete a comment |
| `PUT` | `/api/community/comments/{id}/status` | Admin moderation |
| `GET` | `/api/community/users/mention-suggestions?q=...` | Search up to eight visible profiles for the comment composer |
| `PUT`, `DELETE` | `/api/community/posts/{postId}/reactions/{type}` | Add or remove a reaction type |
| `POST`, `DELETE` | `/api/community/posts/{postId}/bookmark` | Save or remove a bookmark |
| `GET` | `/api/community/posts/bookmarked` | Cursor-paginated saved posts |
| `POST`, `DELETE` | `/api/community/users/{userId}/follow` | Follow or unfollow a user |
| `GET` | `/api/community/users/{userId}/followers` | Cursor-paginated followers |
| `GET` | `/api/community/users/{userId}/following` | Cursor-paginated following list |
| `GET` | `/api/community/users/{userId}/relationship` | Read the viewer's relationship to a user |
| `GET` | `/api/community/users/{userId}/posts` | Cursor-paginated posts by a user |
| `POST` | `/api/community/reports` | Submit a report |
| `GET` | `/api/community/admin/reports?page=0&size=20` | ADMIN-only pending report queue, oldest first; display profiles are batch-enriched |
| `PUT` | `/api/community/admin/reports/{reportId}/decision` | ADMIN dismisses (`DISMISS`) or hides (`HIDE_CONTENT`) |

## Local runtime and tests

Set the required root Compose variables (`POSTGRES_PASSWORD`, `EXTERNAL_JWT_SECRET`, and `GATEWAY_INTERNAL_JWT_SECRET`), then run `docker compose up -d --build community-service`. Flyway initializes `community_db` when the service starts. The local stack uses the shared PostgreSQL container and RabbitMQ; each service still owns its own database.

Run the service tests from the backend root with `mvn -q -pl services/community-service -am test`. PostgreSQL integration tests use Testcontainers and require Docker.
