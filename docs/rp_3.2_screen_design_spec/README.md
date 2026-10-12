# Report 3.2 — Screen Design Specification (IELTS Space) · v0.4.0

Screen Design Spec for the web frontend, rebuilt against the current UI (`frontend/src`).
Scope: **desktop web**. Mobile layouts are deferred to a later revision.
The Design File is **draw.io** — every figure in the DOCX is exported from the `.drawio` files here.

> Not to be confused with the backend FDS (`backend/capstone-docs/rp_3.2_functional-design-specification/`).

## Deliverables

| File | Description |
|---|---|
| `Report_3.2_ScreenDesignSpec_v0.4.0.docx` | Final document, built from `frontend/Report 3.2_ScreenDesignSpec_Template.docx` |
| `../../Report_3.2_ScreenDesignSpec.docx` | Main submission copy (v0.3.0 kept in `archive/`) |
| `diagrams/wireframes/*.drawio` | One file per P-ID (+ `SHELL_SharedLayout.drawio`), one tab per frame `'<P-ID> / <State>'` |
| `diagrams/flows/IELTSSpace_SDS_Flows.drawio` | Flows F-01 … F-05 |
| `diagrams/IELTSSpace_SDS_InformationArchitecture.drawio` | IA-01 Site Map, IA-02 Navigation |
| `diagrams/exports/*.png` | PNG exports (`<ID>__<frame-slug>.png`) embedded in the DOCX |
| `APPENDIX_INDEX.md` | P-ID ↔ route ↔ draw.io file ↔ frame ↔ PNG (generated) |
| `CHANGELOG.md` | Changes since v0.3.0 |

Counts: 31 `.drawio` files, 69 frames (28 screens specified in Part 4 + shell + IA + 5 flows).

## Opening the design files

There is no online link yet — the files live in this folder (not committed).

- **draw.io Desktop** (installed at `C:\Program Files\draw.io\`): double-click any `.drawio` file.
- **VS Code**: extension "Draw.io Integration" (hediet.vscode-drawio), then open the file.
- **Browser**: https://app.diagrams.net → *Open Existing Diagram* → pick the file.

Each frame is a tab at the bottom of the window (e.g. `P-30 / Populated`).
Once the folder is pushed to GitHub, a view link has the form
`https://viewer.diagrams.net/?url=https://raw.githubusercontent.com/IeltsPath-Platform/frontend/<branch>/docs/report-3.2-sds/diagrams/wireframes/P-30_CourseList.drawio`.

## Clickable prototype

`diagrams/IELTSSpace_SDS_Prototype.drawio` holds every wireframe frame in one file with buttons linked
to their target frames (taken from the Key Interactions in Part 4).

1. Open the file in draw.io Desktop (double-click) or at https://app.diagrams.net.
2. Start on the **INDEX / Prototype** tab: pick a start button (Guest Home, Sign in, Course List, Placement, Practice) or any frame.
3. Click buttons, links, navbar items and cards to move between screens.
   In the draw.io editor use **Ctrl/Cmd + click** (a plain click only selects the shape); in the viewer
   (*File → Publish / Preview*, or `viewer.diagrams.net`) a plain click follows the link.
4. Click the frame title (top-left of each frame) to go back to the index.

Links live in `scripts/prototype.py` (`LINKS` per frame, `NAV` for the navbar) and are rebuilt by `build_diagrams.py`.

## Rebuild

Requirements: draw.io Desktop, Python with `python-docx` and Pillow (venv `~/.claude/skills/.venv`).

```bash
cd frontend/docs/report-3.2-sds/scripts
PY=~/.claude/skills/.venv/Scripts/python.exe
$PY build_diagrams.py              # regenerate every .drawio from code (overwrites)
$PY export_diagrams.py             # export PNGs (changed tabs only; --force for all)
$PY build_sds_docx.py --publish    # DOCX + APPENDIX_INDEX.md; --publish copies to frontend/Report_3.2_ScreenDesignSpec.docx
```

Edited a diagram by hand in draw.io? Run only `export_diagrams.py` and `build_sds_docx.py` — **not**
`build_diagrams.py`, which overwrites the files from code. Mobile frames can be re-enabled with
`INCLUDE_MOBILE = True` in `scripts/chrome.py`.

## Sources

| Content | Source |
|---|---|
| P-ID / route / status | `scripts/sds_screens.py` ← `frontend/src/app/App.tsx` |
| UI copy, layout | `frontend/src/features/**`, `frontend/src/components/SiteNavbar.tsx` |
| Part 1 tokens | `globals.css`, `learning-path.css`, `placement.css`, `practice.css` |
| FT / SC IDs | SRS v1.0.0 (`backend/capstone-docs/rp_3.0_system-requirements-system/`) |
| Document text | `scripts/sds_content_general.py`, `scripts/sds_content_screens.py` |

Language: the document is in English; quoted UI strings stay in Vietnamese because that is what the app renders.

## Wireframe conventions

Grayscale + `#123AB5` accent (primary CTA, active nav). Each frame has a title and legend
(Primary CTA · Secondary · Media · Zone). Zone badges A/B/C… match the Zone lists in Part 4.
Desktop width 1440px. Course / topic names in the wireframes are sample data.

The old screenshots in `frontend/docs/sds-mockups/` are evidence only, not the Design File.
