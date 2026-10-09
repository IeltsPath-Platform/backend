"""Report 3.2 SDS v0.4.0 — metadata, Part 1 (design language), Part 2 (IA), Part 3 (flows), Part 5, appendices.

Prose is English. UI copy is quoted verbatim in Vietnamese because that is what the app renders.
Token values come from frontend/src/styles/globals.css, features/learning-path/learning-path.css,
features/learning-path/placement/placement.css and features/practice/practice.css (navbar).
Scope of this revision: desktop web. Mobile layouts are deferred.
"""

VERSION = "v0.4.0"
DATE = "09/10/2026"
DESIGN_DIR = "frontend/docs/report-3.2-sds/diagrams/"

META = [
    ("Project Name", "IELTS Space (IeltsPath Platform)"),
    ("Project Code", "CAPSTONE-FALL26"),
    ("SRS Reference", "IELTSPath SRS v1.0.0 — Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx (FT-01…FT-55, SC-01…SC-08)"),
    ("TDS Reference", "IELTSPath TDS v1.0.0 (Draft) — Report 4_TDS_IELTSPath_IeltsPath_v1.docx"),
    ("Spec Version", VERSION),
    ("Date Created", "05/10/2026"),
    ("Last Updated", DATE),
    ("Author(s)", "UX / Product — IELTS Space capstone team (derived from the frontend codebase)"),
    ("Reviewer(s)", "Tech Lead, Product Owner — pending"),
    ("Status", "Draft for review — desktop web wireframes complete for every MVP screen (draw.io)"),
    ("Design File", "draw.io — " + DESIGN_DIR + "  (wireframes/*.drawio · flows/IELTSSpace_SDS_Flows.drawio · "
                    "IELTSSpace_SDS_InformationArchitecture.drawio). Every figure in this document is exported from these files (diagrams/exports/)."),
]

COVERS = [
    "Design language (colours, typography, grid, icons) — tokens taken from the current CSS",
    "Information architecture (site map covering every App.tsx route, Guest vs Learner navigation)",
    "Screen flows F-01…F-05 (draw.io diagrams), including the course-based learning path and placement test",
    "Screen specifications for every MVP screen on desktop web: layout, states, zones, interactions, UI copy, draw.io wireframes",
    "Responsive rules for desktop and narrow desktop / tablet widths",
]
NOT_COVERS = [
    "Mobile layouts (< 640px) — deferred to a later revision; this revision focuses on the desktop web frontend",
    "Component-level implementation, detailed ARIA, keyboard maps → TDS / Frontend Spec",
    "WCAG contrast audit → Accessibility Review",
    "Motion / animation timing → Frontend Spec",
    "Business rules, API contracts and acceptance criteria → SRS (BR-xx, AC-xx) and FDS (Report 3.2 Functional Design)",
    "Staff / Admin / Content Author screens — not part of the current web frontend",
]
COMPANIONS = [
    "SRS v1.0.0 — Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx — features, scenarios, business rules",
    "FDS v0.4 — Report 3.2_FDS_IELTSPath_v1.docx — backend functional design (a different document)",
    "RTW v1.0 — Report 3.1_RTW_IELTSPath_v1.xlsx — Sheet 2 Use Case List · Sheet 4 Permission Matrix",
    "TDS v1.0.0 — Report 4_TDS_IELTSPath_IeltsPath_v1.docx — technical architecture",
    "Design file: " + DESIGN_DIR + " (draw.io) — see README.md and APPENDIX_INDEX.md in the same folder",
    "UI source of truth: frontend/src/app/App.tsx · frontend/src/features/** · frontend/src/styles/globals.css",
]

HISTORY = [
    ("v0.1.0-phase-A", "05/10/2026", "Metadata, Part 1 Design Language, Part 2 Information Architecture", "Agent draft"),
    ("v0.2.0-phase-B", "05/10/2026", "F-01…F-04, Part 4 MVP, Part 5, Appendix A", "Agent draft"),
    ("v0.2.1-phase-B", "05/10/2026", "F-04 replaced by Guest Home → auth gate", "Agent draft"),
    ("v0.3.0", "06/10/2026", "Embedded FE desktop + mobile screenshots (docs/sds-mockups)", "Agent draft"),
    (VERSION, DATE,
     "Rewritten against the current UI: course-based learning path (P-30 Course List, P-30a Placement, P-30b Topic List); "
     "new navigation (Khóa học / Luyện tập 4 kỹ năng / Sổ từ vựng); added P-10c, P-42c, P-42d, P-51…P-54, P-70…P-72, P-91, P-92; "
     "retired P-10a, P-10b, P-60; F-02 rewritten, F-05 Placement added; Design File = draw.io; real SRS IDs (FT/SC); "
     "Appendix B FE gaps; scope limited to desktop web (mobile deferred); document language English.", "UX / Product"),
]

# ------------------------------------------------------------------ Part 1
PRINCIPLES = [
    ("Progress first", "Every learning screen shows progress (topics / lessons / test sections) and one clear next step — e.g. the “Tiếp theo” card with the “Học tiếp →” CTA on P-30b and P-31."),
    ("Gate honestly", "Locked content always states the reason and how to unlock it: “🔒 Hoàn thành chặng trước để mở”, the “Để mở bài kiểm tra” checklist, the “Cần ôn lại trước khi học tiếp” banner."),
    ("Course before topic", "Learners pick a course by band (P-30); without a placement result they are sent to P-30a. Topics are only shown inside a course (P-30b)."),
    ("One primary action", "Each screen has a single prominent CTA (Học tiếp, Nộp bài, BẮT ĐẦU LÀM BÀI); secondary actions use outline buttons or links."),
    ("Mock vs live clarity", "Screens backed by mock data (Practice, Overview, Classroom) stay navigable but are labelled MVP·mock / Later*; unbuilt routes use P-92 with the copy “nội dung vẫn đang được hoàn thiện”."),
]
BRAND_COLOURS = [
    ("Primary", "#123AB5", "Primary buttons, links, active state (`--classroom-primary`, `--primary`, `--lp-primary`)"),
    ("Primary Dark", "#0D2B8D", "Hover / pressed (`--classroom-primary-strong`)"),
    ("Primary Light", "#EEF3FF", "Selected backgrounds, soft panels, subnav (`--classroom-soft`, `--lp-soft`)"),
    ("Canvas", "#F8F9FC", "Learn / classroom page background (`--classroom-canvas`, `--lp-canvas`)"),
    ("Navbar Navy", "#072A80 → #0A359C", "SiteNavbar gradient (`.site-main-header`)"),
    ("Hero Start / End", "#061C64 / #1647D6", "Hero gradient (`--classroom-hero-start/end`)"),
    ("Accent / CTA", "#FF7624", "Learning path accent CTA (`.lp-btn--accent`), consultation button, suggestion chip (`--classroom-warning`, `--lp-accent`)"),
    ("Placement Blue", "#0E44CF / #0A359C", "Placement exam shell and stepper (`--pl-blue`, `--pl-blue-navy`)"),
]
SEMANTIC_COLOURS = [
    ("Success", "#13845A", "Passed / achieved (`--lp-success`)"),
    ("Success Light", "#E6F5EE", "Success chip background (`--lp-success-soft`)"),
    ("Warning", "#FF7624", "Practice required, pending (`--lp-accent`)"),
    ("Warning Ink / Light", "#A94300 / #FFF1E7", "Warning text and background (`--lp-accent-ink`, `--lp-accent-soft`)"),
    ("Error", "#C2332B", "Load / submit errors (`--lp-danger`)"),
    ("Error Light", "#FDECEA", "Error background (`--lp-danger-soft`)"),
    ("Info", "#388BFF", "Informational highlight (`--particle-blue`)"),
    ("Focus", "#005FCC", "Focus-visible outline (`--color-focus`)"),
    ("Highlight", "#FEF08A", "Reading passage highlight (`--practice-highlight-bg`)"),
]
NEUTRAL_COLOURS = [
    ("Text Primary", "#202633", "Headings, body (`--classroom-text`, `--lp-ink`)"),
    ("Text Secondary", "#5F6774 / #77808E", "Descriptions, meta (`--lp-muted`, `--classroom-text-muted`)"),
    ("Locked", "#7A8291", "Locked topic / lesson (`--lp-locked`)"),
    ("Locked Soft", "#EFF1F5", "Locked card background (`--lp-locked-soft`)"),
    ("Border", "#E3E7EF", "Cards, inputs, dividers (`--classroom-border`, `--lp-line`)"),
    ("Surface", "#FFFFFF", "Cards / panels (`--classroom-surface`)"),
    ("Text (global)", "#111827 / #4B5563", "Default text outside feature scopes (`--color-text`, `--color-text-muted`)"),
    ("Footer", "#061C64 → #0D2B8D", "SiteFooter gradient (hero-start → primary-strong)"),
]
SHADOW_NOTE = ("Shadow: `0 8px 22px rgb(32 55 115 / 8%)` (`--classroom-shadow`); learn card hover `0 16px 36px rgb(18 58 181 / 13%)`. "
               "Radius: `--radius` 0.625rem (10px, shadcn), `--lp-radius` 16px for learning path cards; 999px pills for chips and segments.")
FONTS = [
    ("Font family:", "Primary: Inter (Google Fonts, weights 400–800; imported in learning-path.css) — body and UI."),
    ("Display:", "“Be Vietnam Pro” 600–800 (`--lp-display`) — learning path and placement headings."),
    ("Fallback:", "ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif"),
    ("Monospace:", "ui-monospace, SFMono-Regular, Menlo, monospace — clocks, test codes, timers."),
]
TYPE_SCALE = [
    ("heading-1", "32px (hero clamp up to ~51px)", "800", "1.1–1.2", "Page titles (`lp-pagehead h1`), Home hero"),
    ("heading-2", "22–24px", "700–800", "1.3", "Section titles (“Tất cả khóa học”, “Các chặng”), dialogs"),
    ("heading-3", "18–20px", "700", "1.35", "Card titles (course, topic, lesson)"),
    ("heading-4", "16px", "600", "1.4", "Form group labels"),
    ("body-lg", "16px", "400", "1.5–1.6", "Lead text, descriptions"),
    ("body-md", "14–15px", "400", "1.5", "Default body, inputs"),
    ("body-sm", "12–13px", "400", "1.5", "Meta, helper text, timestamps"),
    ("eyebrow / label", "12px UPPERCASE", "700", "1.4", "`lp-eyebrow` (“KHÓA HỌC”, “CHẶNG 03 · READING”)"),
    ("button", "14–16px", "700", "1.0", "Button labels"),
    ("badge / chip", "11–13px", "600–800", "1.2", "Status chips, band pill"),
    ("timer", "26–34px mono", "800", "1.0", "Placement clock, workspace timer"),
]
SPACING = [
    ("xs", "4px", "Icon ↔ label (`--space-1`)"),
    ("sm", "8px", "Closely related elements (`--space-2`)"),
    ("md", "16px", "Card padding (`--space-3`)"),
    ("lg", "24px", "Form groups, section spacing (`--space-4`)"),
    ("xl", "28–32px", "Between large cards; `lp-main` padding-top 1.75rem"),
    ("2xl", "48–56px", "Large section gaps; `lp-main` padding-bottom 3.5rem"),
]
BREAKPOINTS = [
    ("Narrow desktop / tablet", "1024–1240px (navbar ☰ ≤ 1240px)", "8–12", "20–24px", "24–32px (`clamp(1rem,3.5vw,2rem)`)"),
    ("Desktop", "1025–1440px", "12", "24px", "32px"),
    ("Wide", "> 1440px", "12", "24–32px", "auto; shell 1240px (learn) / 1440px (navbar, practice)"),
]
LAYOUT_ZONES = [
    ("Top navbar", "Sticky, full width; shell max 1440px; ~72px high + 44px subnav"),
    ("Learn content (`.lp-shell`)", "max 1240px, padding-inline clamp(1rem, 3.5vw, 2rem)"),
    ("Lesson rail (P-32)", "Sticky right column ≥ 1025px; at ≤ 1024px replaced by a fixed bottom bar (lp-lesson-mobile-bar)"),
    ("Practice sidebar (P-41)", "~260px left column; stacks above content at ≤ 960px"),
    ("Auth card", "~480–500px, right aligned; at ≤ 1024px the ambient copy is hidden and the card is centred"),
    ("Modal small / medium", "~420–520px (confirmations, Key Activation, survey summary)"),
    ("Modal large", "~780px (P-41a Mode Select), ~1000px (P-44a chart)"),
    ("Exam shell (P-30a)", "Full screen (role=dialog) over the navbar"),
]
ICON_LIB = "Lucide React (`lucide-react`) — the only icon dependency for UI icons; logo and mascot are brand images."
ICON_SIZES = "14–16px (inline, chips) · 18–20px (buttons, default) · 22–28px (standalone, status)"
ICONS = [
    ("X", "Close dialog / leave exam (“Lưu và quay lại”)", "Permanent delete"),
    ("ArrowLeft / ArrowRight", "Back link (“← Tất cả khóa học”), forward CTA", "Form submit"),
    ("Lock", "Locked topic / lesson / test", "System error"),
    ("Check / CheckCircle2", "Passed / saved / completed", "Answer radio"),
    ("Flag / Trophy", "Topic final test / achieved", "Question bookmark (⚑ in question map)"),
    ("ShieldAlert", "Review gate (ReviewGateBanner)", "Security warning"),
    ("Crown", "Premium content", "Passed state"),
    ("Loader2 (spin)", "Loading / submitting", "Empty state"),
    ("Menu", "Open navigation at ≤ 1240px", "Open user dropdown"),
    ("Headphones / BookOpenText / NotebookPen / MicVocal", "Listening / Reading / Writing / Speaking", "General navigation"),
]

# ------------------------------------------------------------------ Part 2
ROLES = [
    ("Guest", "Trang chủ · Khóa học🔒 · Luyện tập 4 kỹ năng🔒 · Bài mẫu Writing 8.0+🔒 · Kết quả học viên🔒 · [Đăng nhập] [Đăng ký]",
     "P-10 Home (/ → /home). 🔒 items lead to P-01."),
    ("Learner (CUSTOMER)", "Trang chủ (Dashboard, Lịch sử nộp bài, Khóa học của tôi) · Khóa học (Test đầu vào 4 kỹ năng FREE, Khóa học) · "
     "Luyện tập 4 kỹ năng (Listening/Reading/Writing/Speaking) · Sổ từ vựng (Flashcard của tôi, Kho từ vựng, Bài mẫu 8đ) · Kết quả học viên · user menu",
     "After sign-in: the originally requested route (state.from) or P-30 Course List (/learn). Opening / → P-11 /overview."),
]
ROLE_NOTES = [
    "The FE only distinguishes Guest and Learner through `RequireAuth` / `GuestOnly`; backend role CUSTOMER = Learner. There are no Staff/Admin screens in the web FE.",
    "Learner without a placement result: the learning API returns PLACEMENT_REQUIRED and the FE redirects to P-30a with a notice (BR-36, BR-40).",
]
NAV_PRIMARY = [
    ("Pattern", "Sticky top navbar (SiteNavbar) — logo left, primary items centre, account right; second subnav row for items with sections"),
    ("Desktop width", "Full viewport; shell max 1440px; sliding indicator under the active item (> 1240px)"),
    ("Narrow desktop", "≤ 1240px: link row hidden, ☰ button (44×44) opens a link grid below the navbar; Esc closes it"),
    ("Active state", "White pill with Primary text for the main item; active subnav item underlined + aria-current=page (matches path and ?skill= query)"),
    ("Badge / count", "No badges; Points and tier are shown in the UserTierDropdown"),
]
NAV_TABS_USED = "Segment filters: P-30 (Tất cả / Gợi ý cho bạn / Đang học / Đã hoàn thành), P-30b (Tất cả / Listening / Reading / Writing / Speaking), P-41 (Bài chưa làm / Bài đã làm); section tabs on P-35 (Phần 1…N); DictionaryPanel (Tra Việt-Anh / Tra Anh-Việt)."
NAV_TABS = [
    ("Style", "Pill segment — aria-pressed buttons; active = Primary fill with white text; optional count (`lp-segment__count`)"),
    ("Position", "Right of the section title"),
    ("Overflow", "Only tabs with data are shown (‘Tất cả’ is always shown); the row wraps when it does not fit"),
]
CRUMBS_USED = "No multi-level breadcrumbs. The learning path uses a single back link (`lp-back`): P-30b “← Tất cả khóa học”, P-31 “← {course title}”, P-32 “← {topic title}”, P-33 “← {lesson title}”, P-34 “← Về lộ trình học”, P-35 “← Về topic / Về khóa học”."
CRUMBS = [
    ("Format", "← {Parent title} (Primary bold link, ArrowLeft icon 16px)"),
    ("Current page", "Eyebrow + H1 directly below the back link"),
]

# ------------------------------------------------------------------ Part 3
FLOWS = {
    "F-01": dict(
        name="Authentication & Account Recovery",
        meta=[("Related SRS Scenario", "SC-01 Join IELTSPath and set a learning goal"), ("Primary Actor", "Guest → Learner"),
              ("Entry Point", "Navbar [Đăng nhập]/[Đăng ký], or RequireAuth redirect to /login with state.from"),
              ("Success End State", "Session created; user lands on the original route (from) or P-30 /learn"),
              ("Design File", "flows/IELTSSpace_SDS_Flows.drawio — frame 'F-01 / Authentication'")],
        image="F-01__authentication.png",
        decisions=[("B1", "Wrong email/password (HttpError)", "Stay on P-01; server message shown in the role=status area"),
                   ("B2", "state.from present (blocked by RequireAuth)", "Return to the original route after sign-in; otherwise → /learn"),
                   ("B3", "Sign-up succeeds", "User is signed in automatically and routed as in B2 (no return to P-01)"),
                   ("B4", "Signed-in Learner opens an auth route", "GuestOnly → P-30"),
                   ("B5", "Password reset succeeds", "Success message, then automatic redirect to P-01 after ~0.9 s"),
                   ("B6", "Google OAuth (OAUTH_ENABLED = false)", "“Google (chưa hỗ trợ)” button disabled; P-05 is Later")]),
    "F-02": dict(
        name="Course-based Learning Path",
        meta=[("Related SRS Scenario", "SC-01, SC-02, SC-03, SC-05"), ("Primary Actor", "Learner"),
              ("Entry Point", "Navbar “Khóa học” / “Khóa học của tôi”, or after sign-in (/learn)"),
              ("Success End State", "Topic PASSED → next topic unlocked; all topics passed → course final test PASSED"),
              ("Design File", "flows/IELTSSpace_SDS_Flows.drawio — frame 'F-02 / Course-based Learning Path'")],
        image="F-02__course-based-learning-path.png",
        decisions=[("B1", "API returns PLACEMENT_REQUIRED", "Redirect to P-30a with the notice “Hãy làm bài kiểm tra đầu vào để chọn course học.”"),
                   ("B2", "Topic LOCKED / Premium", "Topic card is not a link; lock reason and Premium chip shown"),
                   ("B3", "Lesson still has practiceStatus REQUIRED", "Rail shows “Cần luyện thêm” → P-33; topic test stays locked"),
                   ("B4", "Mandatory review pending (REVIEW_REQUIRED)", "ReviewGateBanner + content temporarily locked → P-34"),
                   ("B5", "Topic test AVAILABLE", "“Làm bài kiểm tra” → create assignment + attempt → P-35"),
                   ("B6", "Score < 70%", "P-36 “Chưa đạt lần này”, answers hidden; a retry gets a different test code"),
                   ("B7", "All topics passed (course AVAILABLE)", "P-30b shows “Làm bài thi cuối khóa” → P-35 ?course= → P-36")]),
    "F-03": dict(
        name="Practice Catalog to Skill Workspace",
        meta=[("Related SRS Scenario", "— (FT-34 Mock test, FT-42 Notes, FT-43 Flashcards; FE mock data)"), ("Primary Actor", "Learner"),
              ("Entry Point", "Navbar “Luyện tập 4 kỹ năng” (?skill=), Home “Khám phá bài luyện”"),
              ("Success End State", "Learner works in a skill workspace and exits back to P-41"),
              ("Design File", "flows/IELTSSpace_SDS_Flows.drawio — frame 'F-03 / Practice Catalog to Skill Workspace'")],
        image="F-03__practice-catalog-to-skill-workspace.png",
        decisions=[("B1", "Mode dialog closed", "Stay on P-41"),
                   ("B2", "mode=exam (Thi thử)", "P-42 hides Highlight / Note / dictionary / Flashcard tools"),
                   ("B3", "Skill = speaking", "P-45 Later* (recording UI, not in Part 4)"),
                   ("B4", "“Thoát” button", "Back to /practice-tests; attempt state is not saved")]),
    "F-04": dict(
        name="Guest Home to Auth Gate",
        meta=[("Related SRS Scenario", "SC-01, SC-06"), ("Primary Actor", "Guest"),
              ("Entry Point", "Open / or /home"),
              ("Success End State", "Guest signs in and reaches the selected screen (state.from), or keeps browsing Home"),
              ("Design File", "flows/IELTSSpace_SDS_Flows.drawio — frame 'F-04 / Guest Home to Auth Gate'")],
        image="F-04__guest-home-to-auth-gate.png",
        decisions=[("B1", "Already signed in when opening /", "Redirect to /overview (P-11)"),
                   ("B2", "Guest opens a RequireAuth route", "Navigate /login (replace) with state.from = original route"),
                   ("B3", "Guest clicks Kích hoạt / Nạp Key", "P-10c shows a sign-in prompt with an “Đăng nhập” button"),
                   ("B4", "Guest only browses Home", "No authentication required")]),
    "F-05": dict(
        name="Placement Test",
        meta=[("Related SRS Scenario", "SC-01 · FT-09, FT-35 · BR-36 (placement taken once)"), ("Primary Actor", "Learner"),
              ("Entry Point", "/learn/placement — subnav “Test đầu vào 4 kỹ năng FREE” or PLACEMENT_REQUIRED redirect"),
              ("Success End State", "Result report shown; Learner opens the recommended course → P-30b"),
              ("Design File", "flows/IELTSSpace_SDS_Flows.drawio — frame 'F-05 / Placement Test'")],
        image="F-05__placement-test.png",
        decisions=[("B1", "No attempt / EXPIRED / CANCELLED", "Start with the 3-question survey"),
                   ("B2", "Attempt IN_PROGRESS", "Go straight to the Test Hub (answers are saved on the server)"),
                   ("B3", "Attempt already submitted", "Grading screen (polling), then the report"),
                   ("B4", "PLACEMENT_ALREADY_DONE", "Redirect to /learn (P-30)"),
                   ("B5", "NO_PLACEMENT_TEST", "“Chưa có bài kiểm tra đầu vào nào được mở. Hãy quay lại sau.”"),
                   ("B6", "Grading takes > 90 s", "“Kết quả đang được chấm” + “Kiểm tra lại” button")]),
}

# ------------------------------------------------------------------ Part 4.0 shell
SHELL_NAV = [
    ("Trang chủ", "— (text)", "Guest, Learner", "No — Learner subnav: Dashboard / Lịch sử nộp bài / Khóa học của tôi"),
    ("Khóa học", "—", "Guest🔒, Learner", "No — subnav: Test đầu vào 4 kỹ năng FREE / Khóa học"),
    ("Luyện tập 4 kỹ năng", "—", "Guest🔒, Learner", "No — subnav: Listening / Reading / Writing / Speaking"),
    ("Sổ từ vựng", "—", "Learner", "No — subnav: Flashcard của tôi / Kho từ vựng / Bài mẫu 8đ"),
    ("Bài mẫu Writing 8.0+", "—", "Guest🔒", "No"),
    ("Kết quả học viên", "—", "Guest🔒, Learner", "No"),
    ("User menu", "◯ avatar + Points ▾", "Learner", "Points / tier shown in the trigger"),
    ("☰ Menu", "Menu / X", "Guest, Learner", "≤ 1240px only"),
]
SHELL_FRAMES = [
    ("Learner Desktop", "SHELL__learner-desktop.png", "Authenticated shell (LearnLayout)"),
    ("User Menu", "SHELL__user-menu.png", "UserTierDropdown"),
    ("Guest Desktop", "SHELL__guest-desktop.png", "Public shell"),
    ("Auth Shell", "SHELL__auth-shell.png", "AuthShell (P-01…P-05)"),
    ("Exam Shell", "SHELL__exam-shell.png", "PlacementExamShell (P-30a)"),
]
SHELL_ZONES = [
    ("A", "SiteNavbar: logo (→ /home), primary items, user menu or [Đăng nhập][Đăng ký]; skip link “Chuyển đến nội dung chính”."),
    ("B", "Subnav: sections of the active item; hovering another item previews its sections (reverts after 200 ms). For a Guest on Trang chủ the subnav floats over the page."),
    ("C", "Banners inside /learn/*: ReviewGateBanner (“Cần ôn lại trước khi học tiếp” + “Làm bài ôn”) and NoticeBanner (redirect notices, ✕ to dismiss)."),
    ("D", "main#main-content — per-route ErrorBoundary (“Không thể hiển thị lộ trình học”)."),
    ("E", "DemoControls (only when VITE_USE_MOCK_LEARNING): “Đặt lại demo”, “Giả lỗi máy chủ 500”."),
    ("F", "SiteFooter (every route, rendered after <Routes>): contact info + map, about, social links, support centre (/terms, /privacy, /copyright), legal info; floating “Tư vấn miễn phí” (tel) button."),
]

# ------------------------------------------------------------------ Part 5
RESP_NAV = [
    ("Desktop (> 1240px)", "Full link row with sliding indicator; subnav on the second row; user menu as dropdown"),
    ("Narrow desktop / tablet (1024–1240px)", "Links hidden; ☰ button (44×44) opens a link grid (min-height 44px); subnav still visible"),
]
RESP_LAYOUT = [
    ("Desktop (≥ 1025px)", "Course grid auto-fit min 280px (3 columns at 1240px); overview header 2 columns (text | progress panel); lesson with sticky right rail; passage | questions split; practice 3 columns (map | passage | questions)"),
    ("Narrow desktop / tablet (≤ 1024px)", "Overview header stacks; lesson rail becomes a fixed bottom bar; practice sidebar moves above content (≤ 960px); card grids drop to 2 columns"),
]
DATA_TABLES = [
    "Pattern B — Card reflow (default): courses, topics, lessons, practice sets and per-question results are cards/lists that reflow with the container.",
    "Pattern A — Horizontal scroll: only for the correct/incorrect tables in the placement report (ReportSkillPanels) when columns overflow.",
]
RESP_TYPE = [
    ("Desktop", "Full type scale as defined in Part 1.3"),
    ("Narrow desktop / tablet", "heading-1 ~28px; body unchanged"),
]
TOUCH_NOTE = ["Mobile layouts are out of scope for this revision. Interactive controls already use a 44px minimum height (`min-h-11`); "
              "icon-only buttons (☰, ✕, ⚙) keep a 44 × 44px hit area. Full touch-target rules will be specified with the mobile revision."]

# ------------------------------------------------------------------ Appendix
DESIGN_TREE = [
    "frontend/docs/report-3.2-sds/                 (Design File = draw.io)",
    "├── diagrams/",
    "│   ├── IELTSSpace_SDS_InformationArchitecture.drawio   tabs: IA-01 Site Map · IA-02 Navigation",
    "│   ├── flows/IELTSSpace_SDS_Flows.drawio              tabs: F-01 … F-05",
    "│   ├── IELTSSpace_SDS_Prototype.drawio                clickable prototype: every frame, buttons linked",
    "│   ├── wireframes/                                    one file per P-ID, one tab per frame",
    "│   │   ├── SHELL_SharedLayout.drawio   (Learner / Guest / Auth / Exam shells, User Menu)",
    "│   │   ├── P-01_SignIn.drawio          tabs 'P-01 / Populated' 'P-01 / Loading' …",
    "│   │   ├── P-30_CourseList.drawio … P-36_TestResult.drawio",
    "│   │   ├── P-41_PracticeCatalog.drawio … P-44a_WritingChartZoom.drawio",
    "│   │   └── P-90_NotFound.drawio · P-91_LearnNotFound.drawio · P-92_RouteStatus.drawio",
    "│   └── exports/                                       PNG export: <ID>__<frame-slug>.png",
    "├── scripts/   build_diagrams.py → export_diagrams.py (draw.io CLI) → build_sds_docx.py",
    "├── Report_3.2_ScreenDesignSpec_v0.4.0.docx",
    "├── README.md · CHANGELOG.md · APPENDIX_INDEX.md",
    "",
    "Frame naming: '<P-ID> / <State>' — State ∈ Populated, Loading, Empty, Error, Success, Locked …",
    "Grayscale wireframes + #123AB5 accent; zone badges A/B/C… match the Zone lists in Part 4.",
    "Open: double-click a .drawio file (draw.io Desktop) or app.diagrams.net → Open Existing Diagram.",
    "Prototype: open IELTSSpace_SDS_Prototype.drawio, start from the INDEX tab and click buttons to move between screens.",
]
GLOSSARY = [
    ("P-xx", "Page ID — used in the Site Map, Part 4, draw.io file names and frame names"),
    ("F-xx", "Flow ID — Screen Flow in Part 3 (F-01…F-05)"),
    ("FT-xx", "SRS Feature ID (SRS v1.0.0, FT-01…FT-55)"),
    ("SC-xx", "SRS Scenario ID (SC-01…SC-08)"),
    ("AC-xx / NAC-xx", "SRS acceptance criteria — not repeated here"),
    ("BR-xx", "SRS business rule (e.g. BR-36 placement taken once)"),
    ("MVP", "Built and wired to the real API / auth"),
    ("MVP·mock", "Built and navigable; content comes from FE mock data"),
    ("Later*", "UI exists with mock data; outside Part 4 in this revision"),
    ("Stub", "Route exists but renders P-92 Route Status"),
    ("Screen state", "A visual variant of a screen: Loading / Empty / Populated / Error / Locked / Success …"),
    ("Design file", "draw.io source of the wireframes (frontend/docs/report-3.2-sds/diagrams/) — the source of truth for every figure in this document (completed)"),
    ("Design frame", "One tab in a .drawio file, named '<P-ID> / <State>'"),
    ("Wireframe", "Lo-fi structural drawing; colours and fonts follow Part 1"),
    ("Evidence screenshot", "FE captures (docs/sds-mockups, v0.3.0) — reference only, not the Design File"),
]

# FE ↔ spec gaps found during the audit (Appendix B)
GAPS = [
    ("G-01", "P-30", "PlacementCard links to `/placement` — no such route, so it falls through to P-90.",
     "Change to `/learn/placement` (CourseListPage.tsx)."),
    ("G-02", "P-36", "Result copy exposes technical details: “poll GET /courses”, “làm mới từ GET /topics”.",
     "Rewrite as learner-facing copy (e.g. “Chặng tiếp theo đã mở.”)."),
    ("G-03", "P-41", "The “TEST NGAY ✦” button on the “Chưa biết mình đang ở band nào?” banner opens P-41a for the first card instead of the placement test.",
     "Link to /learn/placement (P-30a) to match the message."),
    ("G-04", "P-10 / P-10c", "App.tsx does not pass `accessClient` to HomePage, so a signed-in Learner always sees “Phiên đăng nhập hiện tại chưa kết nối với dịch vụ gói và Activation Key.”",
     "Wire the access API (FT-10/FT-11) or mark it Later for the demo."),
    ("G-05", "P-90 / P-92", "The “Về Overview” CTA points to /overview (RequireAuth), so a Guest is pushed to P-01.",
     "Choose the target by session: Guest → /home."),
    ("G-06", "P-41…P-44", "Catalog and workspaces use mock data (MOCK_PRACTICE_CARDS…); results are not saved or graded.",
     "Wire FT-34 Mock test when the backend is ready."),
    ("G-07", "Guest navbar", "“Khóa học”, “Luyện tập”, “Bài mẫu”, “Kết quả học viên” are shown to Guests but are all RequireAuth.",
     "Accept (auth gate F-04) or add public landing pages."),
    ("G-08", "P-30a Speaking", "Placement Speaking is “chưa được chấm riêng; bản ghi chỉ lưu trên trình duyệt”.",
     "Per FT-39 (Draft) — keep the warning copy."),
    ("G-09", "P-11, P-20, P-21, P-45, P-50", "UI exists but uses mock data — not in Part 4.", "Specify in a later revision once APIs exist."),
]
OPEN_QUESTIONS = [
    "Switching the current course (SRS FT-20 course choice: Draft) — the FE has no “change course” UI; P-30 lets the learner open any course. The flow needs to be agreed.",
    "Course final test with AI-graded essays (BR-39) — P-35 currently supports objective questions only.",
    "Multi-skill lessons (BR-38): P-32 shows only the topic skill; the list of lesson skills is not displayed.",
    "Mobile layouts (< 640px) — to be specified in the next revision.",
]
