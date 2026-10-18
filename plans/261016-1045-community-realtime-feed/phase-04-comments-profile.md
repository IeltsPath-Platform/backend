# Comments and profiles

Depends on [feed](phase-03-community-feed.md) verification.

- [x] Root comments and lazy replies, edit/delete and accessible interactions.
- [x] Public profile, target badge, follow/unfollow and paginated followers/following tabs.
- [x] Saved posts visible only to owner; loading/error/empty and keyboard interaction.
- [x] Run lint, typecheck, production build.

Files: FE community components/hooks/pages. Preserve backend schemas and frontend auth conventions.
Risk: ownership and stale state after navigating users; derive viewer from auth and reset request state by profile ID. Roll back route wiring if needed.
