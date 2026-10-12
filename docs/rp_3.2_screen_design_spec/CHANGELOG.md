# Changelog — Report 3.2 Screen Design Specification

## v0.4.0 — 09/10/2026

Full rewrite against the current UI (`frontend/src/app/App.tsx`). Design File moved to draw.io.

### Scope and language
- Desktop web only; mobile layouts (< 640px) deferred. Part 5.5 Touch Targets marked not applicable.
- Document written in English; quoted UI copy stays in Vietnamese (as rendered by the app).

### Design File
- Design File = draw.io (`diagrams/`): 31 files, 69 frames. Every MVP screen in Part 4 points to a file path + frame name.
- All DOCX figures are exported from draw.io; `docs/sds-mockups/*.png` is no longer the Design File.
- Glossary A.3: "Design file" = draw.io (completed), "(TBD)" removed.

### Information architecture
- Course-based learning path: `/learn` = **P-30 Course List** (was Topic List).
- Added **P-30a Placement Test** (`/learn/placement`) and **P-30b Topic List in a course** (`/learn/courses/:courseId`).
- P-35 / P-36 shared by the topic final test (`?topic=`) and the course final test (`?course=`, FT-55).
- New navbar: Trang chủ · Khóa học · Luyện tập 4 kỹ năng · Sổ từ vựng · Kết quả học viên (+ subnav). Guests also see Bài mẫu Writing 8.0+.
- After sign-in: original route (state.from) or `/learn`; `/` → `/overview` for Learners.
- Added: P-10c Key Activation, P-42c Saved Flashcards, P-42d Floating Notes, P-51…P-54 (stubs), P-70…P-72 (legal stubs), P-91 Learn Not Found, P-92 Route Status (stub template).
- Retired: P-10a Mentor Detail, P-10b Plan Detail, P-60 Materials (`/materials` no longer in App.tsx).
- Appendix A.4: every `<Route>` in App.tsx (39 declarations, 38 paths) ↔ P-ID.

### Flows
- F-02 rewritten as the Course-based Learning Path (placement gate, practice, review, topic test, course final test).
- Added F-05 Placement Test. F-01, F-03, F-04 updated for the new routes and copy. All 5 flows have draw.io diagrams.

### Content
- Part 1: tokens re-taken from CSS (navbar navy gradient, placement blue, Be Vietnam Pro display font, 1240/1024 breakpoints).
- Part 4: 28 screens fully specified (states, zones, interactions, UI copy, wireframes).
- Real SRS IDs (SRS v1.0.0: FT-01…FT-55, SC-01…SC-08) replace TBD.
- Appendix B: 9 FE ↔ spec gaps (e.g. PlacementCard links to `/placement` → 404) and 4 open questions.

## v0.3.0 — 06/10/2026
- Embedded FE desktop + mobile screenshots (`docs/sds-mockups`); P-35/P-36 could not be captured.

## v0.2.x — 05/10/2026
- F-01…F-04, Part 4 MVP, Part 5, Appendix A.

## v0.1.0 — 05/10/2026
- Metadata, Part 1, Part 2.
