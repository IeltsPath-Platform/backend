# Community UI implementation guidance

Date: 2026-10-16. Status: design ready; FE implementation waits for backend steps 1–2 verification. Source inspected read-only. Paths below relative to `D:/Fall2026/New folder/FE/frontend` unless stated otherwise.

## Direction and existing conventions

Community is a working space for IELTS questions, peer feedback and finding speaking partners. Keep the current royal-blue identity, white surfaces, light blue selected states and readable Vietnamese copy. The distinctive element is the academic context on each post: category plus `Target 7.5` near the author. Avoid a large marketing hero; the composer and actual discussions should be visible immediately.

Verified sources:

| File | Reuse / implication |
| --- | --- |
| `src/styles/globals.css` | Classroom semantic tokens, Inter/system font, 3px blue focus outline, shadcn theme aliases. |
| `src/features/overview/overview.css` | Local token aliases; 1440px shell; 16px surfaces; horizontal mobile navigation; 44–46px actions. |
| `src/components/SiteNavbar.tsx` | Add member `NavItem` at `/community`; nested URLs already activate parent. Bell goes before `UserTierDropdown` in actions. |
| `src/features/practice/practice.css` | Actual shared navbar styles; already imported by App. Check crowded desktop nav after adding Community and bell. |
| `src/features/practice/components/PracticeNavbar.tsx` | Thin wrapper around SiteNavbar; Community can use SiteNavbar directly. |
| `src/app/App.tsx` | Real BrowserRouter, RequireAuth and outer ErrorBoundary exist. Footer already rendered once outside routes. |
| `src/components/ui/{button,input,textarea,select,dialog,radio-group,context-menu}.tsx` | Existing primitives. Dialog has accessible Radix focus management, overlay z80/content z81. |
| `src/lib/httpClient.ts` | `apiRequest`, `HttpError`, abort signal, authenticated requests and memory-only session token. |
| `src/features/auth/authSession.ts` | Session lifecycle; do not create another identity store. |

FE AGENTS and old `.sdd` documents describe some implemented features as absent; source is newer. `authApi.ts` uses typed requests but does not currently parse responses with Zod. Community should add the requested runtime validation within its own API module. No FE Graphify graph or `docs/design-guidelines.md` exists. This report is the permitted design artifact; controller can incorporate standards into FE documentation during implementation.

## Tokens and geometry

Define scoped `--community-*` aliases in `community.css`, not hardcoded colors per component:

| Role | Existing token |
| --- | --- |
| Page / surface | `--classroom-canvas` / `--classroom-surface` |
| Main / secondary text | `--classroom-text` / `--color-text-muted` |
| Primary / hover | `--classroom-primary` / `--classroom-primary-strong` |
| Selected background / border | `--classroom-soft` / `--classroom-border` |
| Focus / error | `--color-focus` / `--destructive` |

Use `--color-text-muted` for small metadata rather than the lighter classroom muted token. Keep current font stack; do not introduce a new font download or icon family. Lucide icons only: MessageCircle, ThumbsUp, Heart, Lightbulb, BadgeCheck, Bookmark, Ellipsis, Bell, Users, Image, Link. Main title 28–32px/1.2; post title 18–20px/1.4; prose and fields 16px/1.6; metadata 13–14px/1.5. Preserve Vietnamese marks without clipping.

Use a 4/8px spacing scale, 16px card radius, 10px field/action radius, 1px borders. Page width max 1440px; gutters 16px mobile, 24–40px desktop. Keep card padding 16px mobile/20–24px desktop. Motion limited to 150–180ms color/opacity feedback; no perpetual animations or feed card lift.

## Screen layout

Desktop at >=1024px: `grid-template-columns: minmax(160px, .75fr) minmax(0, 2.5fr) minmax(180px, 1fr)` with 16–24px gaps. Main feed has at least ~480px at the boundary. Wider screens can cap sidebars near 220/280px; no equal-width columns.

1. Left: compact `Cộng đồng` heading; category navigation; own profile and saved shortcuts. Category state uses query parameters and an active indicator plus text, not color alone.
2. Center: short heading/subtitle, composer invitation, chronological feed, page-loading status. One primary `Viết bài` action.
3. Right: follow discovery and useful posts from the controller-requested bounded `/api/community/discover?size=5` API (`suggestedUsers`, `topPosts`; final DTO pending backend owner). Each section has independent loading/error/empty presentation; compact skeleton rows, local `Thử lại`, and omit an empty section rather than invent users or ranks. Suggested-user rows show avatar/name/optional Target and follow button; top-post rows show linked title and only server-provided counts. Avoid repeated cards nested inside a sidebar card. A brief etiquette panel can supplement these real-data sections.

Below 1024px: one main column, max width ~720px; categories become a labeled horizontal chip strip. Keep keyboard focus visible inside overflow. Move own profile/saved shortcuts into a compact row. Secondary sidebar content may collapse beneath the main controls; do not force a two-column tablet layout. At 320px, titles and controls wrap; only the intentional chip strip may scroll horizontally.

Detail page: back link, full PostCard, comment composer and thread. Profile: compact avatar/name/bio header, optional Target and streak facts, follow control, navigation for posts/saved/followers/following. Saved navigation rendered only for self; do not request private bookmarks for other users. Use query-param links for tabs unless implementing full keyboard tab semantics.

## Components and interaction states

**PostCard.** Semantic `article`, author link, `time dateTime`, category, optional target badge, title, text body, media, count row and action row. Render body as text with `white-space: pre-wrap; overflow-wrap: anywhere`; no raw HTML. Link title to detail; do not make the entire card a nested clickable container. Long feed bodies expand through an explicit `Xem thêm` button. Failed avatar falls back to initials; absent target is omitted, not guessed. Menus expose edit/delete only under confirmed permissions; server remains authoritative. Delete requires confirmation.

**Composer dialog.** Reuse Dialog and fields; use react-hook-form with Zod validation via existing packages, not a new resolver dependency. Fields: category, title, body, target band, image URLs and video URL. Initial viewport shows writing fields, optional attachments revealed through labeled controls. Numeric band range 0–9 in 0.5 steps; blank remains optional. Max four image entries with preview, remove button and URL-specific error. Label `Thêm liên kết ảnh`, never `Tải ảnh lên`. Video helper names accepted providers. Submit error preserves text and media; pending disables repeated submit and shows `Đang đăng…`. Successful response supplies authoritative post. Preserve draft when dismissing; confirm before discarding dirty content.

Mobile dialog: width `calc(100% - 24px)`, max-height `calc(100dvh - 24px)`, scrollable body, safe-area-aware footer. Existing Dialog max-width can be overridden to ~640px desktop. **SelectContent must get z-index above 81**, e.g. `className="z-[90]"`; its current z50 otherwise places the popup behind the modal. Give dialog title/description and return focus to trigger on close.

**Media.** One image: natural ratio, bounded height and contain when needed for essay screenshots. Two: paired cells; three: large left plus two right; four: 2×2. Button-wrapped images open Dialog lightbox with next/previous and visible close. Reserve ratio before load; lazy-load below-fold images; failure fallback keeps layout and lets users open the approved source link. HTTPS and exact allowlisted hosts required. Construct YouTube/Loom embeds from parsed validated IDs, never arbitrary URL strings. Use titled sandboxed iframe with minimal provider-compatible permissions, no autoplay. Drive remains an approved external link, not an invented embed. If no descriptive alt field exists, use modest context (`Ảnh 2 đính kèm bài…`), never fabricate visual descriptions.

**ReactionPicker.** Four labeled choices: Thích / Yêu thích / Hữu ích / Chính xác. Current reaction uses icon + label + selected state. Primary reaction toggle and explicit picker trigger support mouse, touch and keyboard; hover/long-press only shortcuts. Use existing Radix facilities for focus/escape handling. For each choice expose checked/pressed state. Selecting another replaces the old reaction; selecting the active one removes it. Optimistic counts adjust old/new totals once. Serialize mutations per post or use operation revision guards; rollback must not overwrite a newer action. Errors announced near the action. Bookmark and follow use the same per-target pending/rollback discipline.

**Comments.** Two visual levels, 16–24px indentation mobile; never recursively indent replies. Reply-of-reply remains under root with visible `@Name` and reply context. Keep composer draft on failure. Temporary comment exposes pending/retry state and disables edit/delete until authoritative ID. Replies load on `Xem thêm phản hồi`; only display a numeric remaining count if server returns one. Respect IME composition; Enter must not unexpectedly submit Vietnamese input. Provide an explicit submit button.

**Infinite feed.** IntersectionObserver plus visible `Tải thêm` fallback. Cursor opaque, size 10, explicit `pagination=cursor`; never send OFFSET/page for the new feed. Guard in-flight requests; abort/reset on category/user changes; deduplicate by ID with Map/Set; stop at null cursor. Keep old content during subsequent loads and show a small status at the list end. On failure, pause observer retries until `Thử lại`. Initial loading uses 2–3 geometry-matched skeletons; empty copy offers `Viết bài đầu tiên`; exhausted state says `Bạn đã xem hết bài viết`. Preserve loaded pages/scroll on back navigation using feature state keyed by filter, cleared on logout. No new virtualization dependency; measure long sessions, optionally use scoped `content-visibility` with care for focus/search.

**Notifications.** Bell has at least 44px target, label includes unread count, badge capped visually at `99+` but accessible label uses actual count. Dropdown width ~360px desktop and viewport-minus-24px mobile, bounded scroll with focus management; use an accessible Radix/shadcn primitive from installed radix-ui, not a handcrafted focus trap. Rows use actual actor/text/time, unread dot plus explicit accessible state. Link opens post/profile and triggers mark-read; failed read does not erase row. Deleted content yields an unavailable state with return-to-feed action. `Đánh dấu tất cả đã đọc` shows pending/error feedback. Toast uses polite live region, avoids duplicate announcements, does not steal focus. A small `Đang kết nối lại…` status belongs inside panel rather than persistent alarming banners.

One socket per authenticated app session, not per card/page. Reconnect reconciles list and unread-count through REST; dedupe by notification ID; cleanup/logout clears socket and per-user state. The accepted handshake uses Sec-WebSocket-Protocol, never token query strings. UI says notifications are live only after connection; persisted REST data stays usable during disconnection.

## Implementation boundaries and acceptance

- `src/features/community/api`: DTO schemas, actual endpoint methods, media validation aligned with backend allowlist.
- `src/features/community/hooks`: feed pagination, mutations and scroll state; component state where sufficient, existing Zustand only where cross-page sharing earns it.
- `src/features/community/components`: PostCard, PostCreateModal, PostImageGallery, ReactionPicker, CommentList/Item and shared profile header.
- `src/features/community/pages`: feed/detail/profile, each under RequireAuth, with feature ErrorBoundary if needed for recovery.
- `src/features/notifications`: REST API, connection hook, notification bell/list/toast. SiteNavbar renders bell but owns no event business logic.
- Keep styles scoped; no global primitive redesign. Add `min-h-11 min-w-11` to relevant buttons and 44px select/input controls; defaults are smaller. Select/textarea remain >=16px in this feature.

Validation after implementation: 320/375/768/1024/1440px and 200% zoom; keyboard-only composer/picker/lightbox/bell; long Vietnamese title/body/URL; reduced motion; broken media; empty/failed/expired session; rapid reaction changes and rollback; repeated observer intersections; category switch during fetch; back-scroll restore; two-account notification/comment/follow flow and reconnect. Run actual FE lint/typecheck/build, not a claimed automated browser suite. No build or browser validation performed for this read-only report.

Design research used local ui-ux-pro-max product/style/type/color searches and design-system generation. General recommendations were adapted to the existing source palette, not treated as permission to redesign it. Accessibility review reference: [Vercel Web Interface Guidelines](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md), retrieved 2026-10-16.

## Open integration questions / concerns

1. Controller requested bounded `/api/community/discover?size=5` from backend owner during this review. Wait for its verified DTO, ranking definition and pagination/limit contract before integration; do not claim an unimplemented endpoint exists.
2. Confirm final response fields for current follow state/counts, reply totals, moderation permissions, notification destination and user ID before coding their controls. Do not infer follow state by loading an unbounded following list.
3. FE/backend allowlist must match; user media can fail despite a valid host because remote permissions and provider embed policies remain external. Show an actionable fallback.
4. Existing global classroom colors are light-only while shadcn has dark tokens. Do not claim dark-mode support without explicit scoped mappings and contrast verification.

Status: DONE_WITH_CONCERNS
Summary: Design mapped to current FE tokens, navigation, primitives, responsive layouts and approved backend decisions. Implementation-ready guidance covers async states, media, accessibility and realtime behavior.
Concerns: Discovery endpoint is requested but its final contract and relation metadata still require reconciliation with verified backend DTOs. No FE source changed.
