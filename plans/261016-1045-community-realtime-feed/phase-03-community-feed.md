# Community feed

Depends on backend verification. Context: [plan](plan.md).

- [x] Add authenticated routes/nav and validated API client using existing HTTP helper.
- [x] Responsive feed with category chips, keyset infinite scrolling, loading/error/empty/retry states.
- [x] Accessible create/edit dialog with validated media URL previews and target band.
- [x] Post cards, image gallery/lightbox, safe video embedding, reaction and bookmark interactions.
- [x] Run lint, typecheck, production build.

Files: FE `src/features/community/**`, `src/app/App.tsx`, `src/components/SiteNavbar.tsx`, existing UI primitives if required.
Risk: stale asynchronous responses, duplicate loads, XSS; abort/ignore old requests, deduplicate IDs, render text only. Roll back routes to hide feature if necessary.
