# Realtime and end-to-end validation

Depends on [comments/profile](phase-04-comments-profile.md) verification.

- [x] Install approved STOMP dependency with consistent lockfile.
- [x] Connect after login, disconnect on logout, backoff/reconnect and REST reconciliation.
- [x] Bell count, notification dropdown, mark read/read-all, navigation and accessible announcement.
- [x] Verify FE lint/type/build and impacted backend tests.
- [x] Review cross-module security/contracts; reconcile docs and report actual checks/limitations.
- [ ] Real two-account browser flow; deferred because the local service stack was not started in this session.

Files: FE notifications, navbar, manifest/lockfile, backend/frontend docs and reports.
Risk: token expiry, duplicate pushes, cross-account leakage; refresh through existing HTTP auth, deduplicate and clear on logout. Reconnect always refreshes persisted state. Rollback disconnects realtime while retaining REST.
