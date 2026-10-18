# #2 Content reports

## Status

Initial report flows passed the user's browser check. Follow-up changes and automated gates are complete: invalid JSON enums map to 400, admin names are batch-enriched, and report cascade behavior follows the user's selected rule. The new name display has FE page coverage; check it in the browser after rebuilding/recreating the Community Service container.

## Requirements

- A signed-in user can report an active post or comment once per target. The content author cannot report their own content.
- Persist reports in `community_db`; enforce uniqueness on `(reporter_id, content_type, target_id)` in PostgreSQL.
- Only `ADMIN` can read the pending queue or resolve reports. Admin can dismiss a report or hide its target; hiding reuses existing post/comment moderation rules.
- On `HIDE_CONTENT`, atomically mark every pending report for that target `ACTIONED` with the handling admin and review timestamp. `DISMISS` only closes the selected report. Concurrent handling serializes; a losing admin gets 409 and no partial state is committed.
- Batch-enrich reporter and content-author display names through the existing public profile client; the UI falls back to shortened IDs if a profile is unavailable.
- Invalid enum values in JSON request DTOs return 400 through the global exception handler.
- No moderator notification event is in scope, so this slice emits no new outbox event.
- Add Mockito use-case tests, `@WebMvcTest` coverage for 401/403/400/201 and ADMIN paths, PostgreSQL Testcontainers migration/constraint tests, and FE page tests rendering URLs with mocked APIs.
- Build every changed service image with Docker Compose and inspect the complete build log.

## Implementation plan

1. Add a forward-only community migration, report aggregate/repository adapter, use cases, validated HTTP DTOs and ADMIN endpoints.
2. Add report controls for posts/comments and an ADMIN queue route; keep API checks authoritative and restrict the queue UI using the authenticated user's roles.
3. Add the requested tests, update Community API and root runtime documentation, then run backend, frontend and Docker gates.

## Contracts

- `POST /api/community/reports` with `{contentType, contentId, reason}` returns `201`.
- `GET /api/community/admin/reports?page=0&size=20` lists pending reports, ordered oldest first.
- `PUT /api/community/admin/reports/{reportId}/decision` with `{decision: "DISMISS"|"HIDE_CONTENT"}` resolves a pending report.
- Reports accept `POST` and `COMMENT` targets and a trimmed reason of 1–1000 characters.

## Risks and rollback

- Existing data is untouched; the migration only adds a table and index. Roll back by deploying the prior application image; keep the new table until a separately approved schema removal.
- Concurrent duplicate reports are rejected by the unique database constraint even if application pre-checks race.
- Hiding content can close multiple pending reports in one transaction; a concurrent losing decision returns 409 and leaves all report rows consistent.
- No cross-service event or notification changes are introduced.
