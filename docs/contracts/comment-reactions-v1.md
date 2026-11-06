# Comment reactions

Community comments and replies share the same reaction API. Requests go through the existing
`/api/community/**` gateway route and require an authenticated user. The viewer identity comes from
`CurrentUserProvider`; the request cannot select a different user.

| Method and path | Success | Behavior |
| --- | --- | --- |
| `PUT /api/community/comments/{commentId}/reactions/{type}` | 204, empty body | Add or replace the current user's reaction. Repeating the same type changes nothing. |
| `DELETE /api/community/comments/{commentId}/reactions/{type}` | 204, empty body | Remove only that type for the current user. A missing or different reaction is a no-op. |

`type` is one of `LIKE`, `LOVE`, `HELPFUL`, `ACCURATE`. Unknown values return 400; unauthenticated
requests return 401. Missing, hidden or deleted comments and comments on non-visible posts return
404. Administrators retain the visibility exception used by post reactions. Users may react to their
own comments. There is at most one reaction per `(commentId, userId)`, including simultaneous writes.

Every `CommentResponse` adds these fields:

```json
{
  "reactionCounts": { "LIKE": 0, "LOVE": 1, "HELPFUL": 0, "ACCURATE": 0 },
  "myReaction": "LOVE"
}
```

`reactionCounts` always contains all four types. `myReaction` is null when the authenticated viewer
has no reaction. These fields are present in flat and threaded comment lists, reply lists, and create,
edit and moderation responses. The existing comment fields and HTTP status codes are preserved.
Counts and the viewer's reaction are loaded in one grouped database query for the whole page;
empty pages do not perform a reaction query.

Flyway `V11__comment_reactions.sql` creates `comment_reactions` with a composite primary key,
reaction type constraint, comment/type index, and cascading foreign key to `comments`. This version
was free on `origin/main` with no open PRs when implementation began. Soft hiding/deleting a comment
keeps its reactions. Physical deletion cascades them. Comment reactions do not publish author
notifications in this version.
