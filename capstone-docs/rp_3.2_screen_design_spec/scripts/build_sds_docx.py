"""Build Report 3.2 Screen Design Specification v0.4.0 from the official template.

Usage:  python build_sds_docx.py [--publish]
  --publish  also copy the result to frontend/Report_3.2_ScreenDesignSpec.docx (v0.3.0 archived first)
Inputs: frontend/Report 3.2_ScreenDesignSpec_Template.docx, diagrams/exports/*.png (run export_diagrams.py first)
"""
import argparse
import os
import shutil
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from docx_blocks import SdsDocument  # noqa: E402
from export_diagrams import export_name  # noqa: E402
import sds_content_general as G  # noqa: E402
from sds_content_screens import SPECS  # noqa: E402
from sds_screens import SCREENS, GROUPS, FLOWS, spec_screens  # noqa: E402

ROOT = os.path.normpath(os.path.join(HERE, ".."))
FRONTEND = os.path.normpath(os.path.join(ROOT, "..", ".."))
TEMPLATE = os.path.join(FRONTEND, "Report 3.2_ScreenDesignSpec_Template.docx")
EXPORTS = os.path.join(ROOT, "diagrams", "exports")
OUT = os.path.join(ROOT, f"Report_3.2_ScreenDesignSpec_{G.VERSION}.docx")
PUBLISHED = os.path.join(FRONTEND, "Report_3.2_ScreenDesignSpec.docx")
WIRE = "diagrams/wireframes/"

missing = []


def png(name):
    path = os.path.join(EXPORTS, name)
    if not os.path.exists(path):
        missing.append(name)
    return path


CACHE = tempfile.mkdtemp(prefix="sds-png-")


def compact(path):
    """Wireframes are flat colour: a 256-colour palette PNG keeps them sharp at ~1/4 of the size."""
    try:
        from PIL import Image
    except ImportError:
        return path
    out = os.path.join(CACHE, os.path.basename(path))
    with Image.open(path) as im:
        im.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT).save(out, optimize=True)
    return out


def fig(d, name, caption, **kw):
    path = png(name)
    if os.path.exists(path):
        d.figure(compact(path), caption, **kw)


def site_map_lines():
    lines = ["[Root] IELTS Space web app   / → /home (Guest) · /overview (Learner)", "│"]
    for gi, g in enumerate(GROUPS):
        last_group = gi == len(GROUPS) - 1
        lines.append(("└── " if last_group else "├── ") + g)
        items = [s for s in SCREENS if s.group == g]
        for i, s in enumerate(items):
            branch = "└── " if i == len(items) - 1 else "├── "
            lead = "    " if last_group else "│   "
            roles = {"Guest": "G", "Learner": "L", "Guest, Learner": "G+L"}.get(s.roles, s.roles)
            line = f"{lead}{branch}[{s.pid}] {s.name} ({roles}) [{s.level}] {s.status} · {s.route}"
            lines.append(line if len(line) <= 88 else line[:87] + "…")
        if not last_group:
            lines.append("│")
    return lines


ROUTES = [  # every <Route> in frontend/src/app/App.tsx
    ("/", "Redirect → /home (Guest) · /overview (Learner)", "—"), ("/home", "HomePage", "P-10"), ("/overview", "OverviewPage 🔒", "P-11"),
    ("/classroom", "ClassroomPage 🔒", "P-20"), ("/learn (index)", "LearnLayout › CourseListPage 🔒", "P-30"),
    ("/learn/placement", "PlacementPage", "P-30a"), ("/learn/courses/:courseId", "TopicListPage", "P-30b"),
    ("/learn/topics/:topicId", "TopicDetailPage", "P-31"), ("/learn/lessons/:lessonId", "LessonPage", "P-32"),
    ("/learn/lessons/:lessonId/practice", "PracticePage", "P-33"), ("/learn/reviews/:reviewId", "ReviewPage", "P-34"),
    ("/learn/tests/:attemptId", "TopicTestPage", "P-35"), ("/learn/tests/:attemptId/result", "TopicTestResultPage", "P-36"),
    ("/learn/*", "NotFoundState", "P-91"), ("/practice", "RouteStatusPage “Thực hành” 🔒", "P-40 (P-92)"),
    ("/practice-tests", "PracticeCatalogPage 🔒", "P-41"), ("/practice/test/:testId", "PracticeTestPage 🔒", "P-42"),
    ("/practice/test", "PracticeTestPage (default test) 🔒", "P-42"), ("/practice/listening/:testId", "ListeningPage 🔒", "P-43"),
    ("/practice/writing/:taskId", "WritingPage 🔒", "P-44"), ("/practice/speaking/:cueId", "SpeakingPage 🔒", "P-45"),
    ("/vocabulary", "VocabularyPage", "P-50"), ("/submission-history", "RouteStatusPage 🔒", "P-54 (P-92)"),
    ("/flashcards", "RouteStatusPage 🔒", "P-51 (P-92)"), ("/writing-samples", "RouteStatusPage 🔒", "P-52 (P-92)"),
    ("/student-results", "RouteStatusPage 🔒", "P-53 (P-92)"), ("/login", "AuthPage sign-in (GuestOnly)", "P-01"),
    ("/register", "AuthPage sign-up (GuestOnly)", "P-02"), ("/forgot-password", "ForgotPasswordPage (GuestOnly)", "P-03"),
    ("/reset-password", "ResetPasswordPage (GuestOnly)", "P-04"), ("/auth/oauth/callback", "OAuthCallbackPage (GuestOnly)", "P-05"),
    ("/classes/:classCode/join", "RouteStatusPage", "P-22 (P-92)"), ("/lessons/:lessonId", "LessonWorkspacePage 🔒", "P-21"),
    ("/mentors/:mentorSlug", "RouteStatusPage", "P-23 (P-92)"), ("/terms", "RouteStatusPage", "P-70 (P-92)"),
    ("/privacy", "RouteStatusPage", "P-71 (P-92)"), ("/copyright", "RouteStatusPage", "P-72 (P-92)"), ("*", "NotFoundPage", "P-90"),
]


def build():
    d = SdsDocument(TEMPLATE)
    d.set_header("Template v1.0", f"IELTS Space {G.VERSION}")
    d.set_core("Screen Design Specification — IELTS Space", "IELTS Space capstone team", "Report 3.2 SDS " + G.VERSION)

    # ---------------------------------------------------------------- cover
    d.title("Screen Design Specification", "IELTS Space (CAPSTONE-FALL26) — Report 3.2")
    d.kv(G.META, weights=(2400, 6626))
    d.heading("Purpose & Scope", 1)
    d.callout("teal", "What this document covers", G.COVERS)
    d.callout("teal", "What this document does NOT cover", G.NOT_COVERS)
    d.callout("teal", "Companion documents", G.COMPANIONS)
    d.heading("Document Change History", 1)
    d.table(["Version", "Date", "Changes", "Author"], G.HISTORY, [1.35, 1.25, 4.8, 1.2])

    # ---------------------------------------------------------------- Part 1
    d.page_break()
    d.heading("Part 1 — Design Language", 1)
    d.para("Visual rules applied consistently across every screen. Tokens are taken from the current CSS: `globals.css`, `learning-path.css`, `placement.css`, `practice.css`.")
    d.heading("1.1  Design Principles", 2)
    d.table(["#", "Principle", "What It Means in Practice"], [(str(i + 1), a, b) for i, (a, b) in enumerate(G.PRINCIPLES)], [0.5, 1.6, 5.5])
    d.heading("1.2  Colour Palette", 2)
    d.heading("Brand / Primary Colours", 3)
    d.table(["Name", "Hex", "Usage"], G.BRAND_COLOURS, [1.5, 1.6, 4.5], mono_cols=(1,))
    d.heading("Semantic / Feedback Colours", 3)
    d.table(["Name", "Hex", "Usage"], G.SEMANTIC_COLOURS, [1.5, 1.6, 4.5], mono_cols=(1,))
    d.heading("Neutral / Text / Surface Colours", 3)
    d.table(["Name", "Hex", "Usage"], G.NEUTRAL_COLOURS, [1.5, 1.6, 4.5], mono_cols=(1,))
    d.para(G.SHADOW_NOTE)
    d.heading("1.3  Typography", 2)
    for label, text in G.FONTS:
        d.labelled(label, text)
    d.para("Default line-height 1.5 (`:root`).")
    d.table(["Style", "Size", "Weight", "Line Height", "Used For"], G.TYPE_SCALE, [1016000, 1100000, 762000, 762000, 2302510])
    d.heading("1.4  Layout Grid & Spacing", 2)
    d.labelled("Spacing base unit:", "4px (`--space-1…4` in globals.css; Tailwind spacing 0.25rem).")
    d.table(["Token", "Value", "Primary Usage"], G.SPACING, [762000, 889000, 4080510])
    d.table(["Breakpoint", "Width Range", "Columns", "Gutter", "Page Margin"], G.BREAKPOINTS, [1016000, 1500000, 700000, 800000, 1700000])
    d.table(["Layout Zone", "Width"], G.LAYOUT_ZONES, [2032000, 3699510])
    d.heading("1.5  Iconography", 2)
    d.labelled("Icon library:", G.ICON_LIB)
    d.labelled("Sizes:", G.ICON_SIZES)
    d.table(["Icon", "Meaning", "Must NOT be used for"], G.ICONS, [1524000, 2400000, 1807510])

    # ---------------------------------------------------------------- Part 2
    d.page_break()
    d.heading("Part 2 — Information Architecture", 1)
    d.para("Product structure: pages, how they are organised and how users move between them. Sources: `frontend/src/app/App.tsx`, `SiteNavbar.tsx`, `AuthGuards.tsx`. "
           "Design file: `diagrams/IELTSSpace_SDS_InformationArchitecture.drawio`.")
    d.heading("2.1  User Roles & Navigation Access", 2)
    d.table(["Role", "Visible Navigation Sections", "Landing Page After Login"], G.ROLES, [1397000, 2683510, 1651000])
    for n in G.ROLE_NOTES:
        d.bullet(n)
    d.heading("2.2  Site Map", 2)
    d.para("Format: [P-ID] Page Name (Roles: G = Guest, L = Learner) [PRIMARY | SUB | DEEP | MODAL | AUTH] Status · Route. Status: MVP, MVP·mock, Later*, Stub, Later, Retired (Glossary A.3). "
           "Every App.tsx route is cross-checked in Appendix A.4.")
    d.callout("code", "Site Map — IELTS Space web (from App.tsx, 09/10/2026)", site_map_lines(), mono=True)
    d.orientation("landscape")
    fig(d, "IA-01__site-map.png", "Site Map — draw.io: diagrams/IELTSSpace_SDS_InformationArchitecture.drawio › 'IA-01 / Site Map'")
    fig(d, "IA-02__navigation.png", "Navigation Guest vs Learner (2.1 / 2.3) — draw.io: IELTSSpace_SDS_InformationArchitecture.drawio › 'IA-02 / Navigation'")
    d.orientation("portrait")
    d.heading("2.3  Navigation Patterns", 2)
    d.heading("Primary Navigation", 3)
    d.table(["Attribute", "Specification"], G.NAV_PRIMARY, [1905000, 3826510])
    d.heading("In-Page Navigation (Tabs)", 3)
    d.labelled("Used on:", G.NAV_TABS_USED)
    d.table(["Attribute", "Specification"], G.NAV_TABS, [1905000, 3826510])
    d.heading("Breadcrumbs", 3)
    d.labelled("Used on:", G.CRUMBS_USED)
    d.table(["Attribute", "Specification"], G.CRUMBS, [1905000, 3826510])

    # ---------------------------------------------------------------- Part 3 (landscape)
    d.orientation("landscape")
    d.heading("Part 3 — Screen Flows", 1)
    d.para("Main MVP journeys. Each flow has a diagram in `diagrams/flows/IELTSSpace_SDS_Flows.drawio` (one tab per flow). "
           "Box = screen (P-ID), diamond = condition, arrow = action / route; dashed = secondary branch or Later.")
    for fid, name, _ in FLOWS:
        f = G.FLOWS[fid]
        if fid != "F-01":
            d.page_break()
        d.heading(f"Flow [{fid}] — {f['name']}", 2)
        d.kv(f["meta"], weights=(1651000, 4080510))
        d.heading("Key Decision Points", 3)
        d.table(["Branch", "Condition", "Outcome"], f["decisions"], [1016000, 2286000, 2429510])
        d.page_break()
        d.heading(f"Flow Diagram — [{fid}]", 3)
        fig(d, f["image"], f"Flow [{fid}] {f['name']} — draw.io: diagrams/{f['meta'][-1][1]}", max_h=5.35)

    # ---------------------------------------------------------------- Part 4
    d.orientation("portrait")
    d.heading("Part 4 — Screen Specifications", 1)
    d.para("One section per MVP / MVP·mock screen (from Site Map 2.2), desktop web only. The shared shell is described once in 4.0. "
           "Each screen points to its draw.io Design File (path + frame name) and embeds images exported from those frames. Later* / Stub screens appear only in the Site Map and Appendix A.")
    d.heading("4.0 — Shared Layout (Authenticated Shell)", 2)
    d.labelled("Design file:", f"draw.io — `{WIRE}SHELL_SharedLayout.drawio` — frames 'SHELL / Learner Desktop', 'SHELL / User Menu', 'SHELL / Guest Desktop', 'SHELL / Auth Shell', 'SHELL / Exam Shell', 'SHELL / Mobile Nav'")
    d.callout("code", "Shell layout structure", [
        "┌──────────────────────────────────────────────────────────────┐",
        "│ [A] SiteNavbar: logo | primary items | user menu (Points)     │",
        "│ [B] Subnav: sections of the active item                      │",
        "├──────────────────────────────────────────────────────────────┤",
        "│ [C] ReviewGateBanner / NoticeBanner (/learn/* only)          │",
        "│ [D] main#main-content (ErrorBoundary theo route)             │",
        "│ [E] DemoControls (mock learning)                             │",
        "├──────────────────────────────────────────────────────────────┤",
        "│ [F] SiteFooter                         (Tư vấn miễn phí)     │",
        "└──────────────────────────────────────────────────────────────┘",
        "AuthShell (P-01…P-05): navbar + ambient copy/mascot + form card.",
        "PlacementExamShell (P-30a): full-screen dialog over the navbar."], mono=True)
    d.table(["Nav Item", "Icon", "Visible to Roles", "Shows Badge?"], G.SHELL_NAV, [1524000, 1016000, 1524000, 1667510])
    d.para("Zone descriptions:")
    for z, t in G.SHELL_ZONES:
        d.bullet(f"**Zone {z}:**  {t}")
    for frame, image, cap in G.SHELL_FRAMES:
        fig(d, image, f"{cap} — draw.io: {WIRE}SHELL_SharedLayout.drawio › 'SHELL / {frame}'")

    for s in spec_screens():
        spec = SPECS[s.pid]
        d.page_break()
        d.heading(f"{s.section} — {s.name} ({s.pid})", 2)
        frames = ", ".join(f"'{s.pid} / {fr}'" for fr in s.frames)
        d.kv([("Page ID", s.pid), ("Screen name", s.name),
              ("Design file", f"draw.io — {WIRE}{s.drawio} — frames {frames}"),
              ("SRS Feature", s.ft), ("SRS Scenario", s.sc), ("Roles with access", s.roles),
              ("Route", s.route), ("Nav level", s.level), ("Status", s.status), ("Flow(s)", s.flows)],
             weights=(1651000, 4080510))
        d.para(spec["purpose"])
        d.heading("Screen States", 3)
        d.table(["State", "When It Appears", "Design File Frame"], spec["states"], [1524000, 2559000, 1648510])
        d.heading("Layout Description", 3)
        if spec.get("layout"):
            d.callout("code", f"Layout zones — {s.pid} {s.name}", spec["layout"], mono=True)
        else:
            d.para(f"Layout: see frame '{s.pid} / Populated' (figure below); zone badges A/B/C match the list that follows.", italic=True)
        d.para("Zone descriptions:")
        for z, t in spec["zones"]:
            d.bullet(f"**Zone {z}:**  {t}")
        d.heading("Key Interactions", 3)
        d.table(["User Action", "What Happens", "Notes"], spec["interactions"], [1615440, 2790190, 1325880])
        d.heading("Mockup", 3)
        d.callout("purple", "Mockup / Wireframe — draw.io", [
            f"Design file: {WIRE}{s.drawio}",
            f"Frames: {frames}",
            "Images below are exported from draw.io (scripts/export_diagrams.py); edit the .drawio file, then rebuild this document."])
        for fr in s.frames:
            name = export_name(f"{s.pid} / {fr}")
            fig(d, name + ".png", f"[{s.pid}] {s.name} — {fr} · draw.io frame '{s.pid} / {fr}'", max_h=7.6)

    # ---------------------------------------------------------------- Part 5
    d.page_break()
    d.heading("Part 5 — Responsive Rules", 1)
    d.para("Global rules for desktop web, taken from the CSS media queries. Mobile layouts (< 640px) are out of scope for this revision.")
    d.heading("5.1  Navigation", 2)
    d.table(["Breakpoint", "Behaviour"], G.RESP_NAV, [1345565, 4410075])
    d.heading("5.2  Content Layout", 2)
    d.table(["Breakpoint", "Layout Behaviour"], G.RESP_LAYOUT, [1270000, 4461510])
    d.heading("5.3  Data Tables", 2)
    d.callout("blue", "Pattern used per surface", G.DATA_TABLES)
    d.heading("5.4  Typography Scaling", 2)
    d.table(["Breakpoint", "Heading Adjustment"], G.RESP_TYPE, [1270000, 4461510])
    d.heading("5.5  Touch Targets (Mobile)", 2)
    d.callout("blue", "Not applicable in this revision", G.TOUCH_NOTE)

    # ---------------------------------------------------------------- Appendix A
    d.page_break()
    d.heading("Appendix A — Screen Index & Design File", 1)
    d.heading("A.1  Screen Index", 2)
    d.para("1:1 with Site Map 2.2. Design Frame = draw.io file + default frame (number of additional frames in brackets). Stub screens share the 'P-92 / Populated' frame.")
    rows = []
    for s in SCREENS:
        if s.drawio:
            frame = f"{s.drawio}\n'{s.pid} / {s.frames[0]}'" + (f" (+{len(s.frames) - 1})" if len(s.frames) > 1 else "")
        elif s.status == "Stub":
            frame = "P-92_RouteStatus.drawio\n'P-92 / Populated'"
        else:
            frame = "— (not drawn: " + s.status + ")"
        rows.append((s.pid, s.name, s.ft, s.flows, s.section if s.section != "—" else "Deferred", frame, s.status))
    d.table(["Page ID", "Screen Name", "SRS Feature", "Flow(s)", "Spec Section", "Design Frame", "Status"], rows, [0.75, 1.45, 1.0, 0.75, 0.9, 2.85, 1.05])
    d.heading("A.2  Design File Structure", 2)
    d.callout("code", "Design file structure (draw.io)", G.DESIGN_TREE, mono=True)
    d.para("The old screenshot set `frontend/docs/sds-mockups/` (v0.3.0) is evidence only; it is not the Design File.", italic=True)
    d.heading("A.3  Glossary", 2)
    d.table(["Term", "Definition"], G.GLOSSARY, [1.4, 5.0])
    d.heading("A.4  Route Coverage (App.tsx)", 2)
    d.para("Every <Route> in `frontend/src/app/App.tsx` and its P-ID. 🔒 = RequireAuth.")
    d.table(["Route", "Element", "P-ID"], ROUTES, [2.6, 3.0, 1.2], mono_cols=(0,))

    # ---------------------------------------------------------------- Appendix B
    d.heading("Appendix B — FE ↔ Spec Gaps & Open Questions", 1)
    d.heading("B.1  Gaps found in the UI audit", 2)
    d.table(["ID", "Screen", "Observation (current code)", "Suggested resolution"], G.GAPS, [0.6, 1.1, 3.0, 2.3])
    d.heading("B.2  Open questions (SRS ↔ FE)", 2)
    for q in G.OPEN_QUESTIONS:
        d.bullet(q)
    d.spacer()
    d.para(f"— End of Report 3.2 Screen Design Specification {G.VERSION} —", align="center", italic=True, color="595959")
    d.save(OUT)
    return d


def write_index():
    """APPENDIX_INDEX.md — screen ↔ draw.io file ↔ frame ↔ exported PNG (generated, do not edit by hand)."""
    out = ["# Appendix Index — Report 3.2 SDS " + G.VERSION, "",
           "Generated by `scripts/build_sds_docx.py`. Design File = draw.io; PNG = exports embedded in the DOCX.", "",
           "## Screens (Part 4 / Appendix A.1)", "",
           "| P-ID | Screen | Route | Status | Spec | draw.io file | Frame → PNG |", "|---|---|---|---|---|---|---|"]
    for s in SCREENS:
        if s.drawio:
            frames = "<br>".join(f"`{s.pid} / {fr}` → `exports/{export_name(s.pid + ' / ' + fr)}.png`" for fr in s.frames)
            file = f"`wireframes/{s.drawio}`"
        elif s.status == "Stub":
            frames, file = "`P-92 / Populated` (template chung)", "`wireframes/P-92_RouteStatus.drawio`"
        else:
            frames, file = "—", "—"
        route = s.route.replace("|", "\\|")
        out.append(f"| {s.pid} | {s.name} | `{route}` | {s.status} | {s.section} | {file} | {frames} |")
    out += ["", "## Shared layout (Part 4.0)", "", "| Frame | PNG |", "|---|---|"]
    out += [f"| `SHELL / {fr}` | `exports/{png_name}` |" for fr, png_name, _ in G.SHELL_FRAMES]
    out += ["", "## Information architecture & flows (Part 2, Part 3)", "", "| Frame | draw.io file | PNG |", "|---|---|---|",
            "| `IA-01 / Site Map` | `IELTSSpace_SDS_InformationArchitecture.drawio` | `exports/IA-01__site-map.png` |",
            "| `IA-02 / Navigation` | `IELTSSpace_SDS_InformationArchitecture.drawio` | `exports/IA-02__navigation.png` |"]
    out += [f"| `{fid}` — {G.FLOWS[fid]['name']} | `flows/IELTSSpace_SDS_Flows.drawio` | `exports/{G.FLOWS[fid]['image']}` |" for fid, _, _ in FLOWS]
    out += ["", "## Evidence only", "", "`frontend/docs/sds-mockups/*.png` (v0.3.0 FE captures) — reference only, **not** the Design File.", ""]
    with open(os.path.join(ROOT, "APPENDIX_INDEX.md"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(out))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--publish", action="store_true")
    args = ap.parse_args()
    d = build()
    write_index()
    print(f"written {OUT} ({d.figure_no} figures) + APPENDIX_INDEX.md")
    if missing:
        print("MISSING exports:", ", ".join(missing))
        sys.exit(1)
    if args.publish:
        archive = os.path.join(ROOT, "archive", "Report_3.2_ScreenDesignSpec_v0.3.0.docx")
        if os.path.exists(PUBLISHED) and not os.path.exists(archive):
            os.makedirs(os.path.dirname(archive), exist_ok=True)
            shutil.copy2(PUBLISHED, archive)
            print("archived previous submission ->", archive)
        shutil.copy2(OUT, PUBLISHED)
        print("published ->", PUBLISHED)


if __name__ == "__main__":
    main()
