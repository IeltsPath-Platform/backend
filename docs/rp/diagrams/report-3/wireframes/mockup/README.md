# Report 3 mock-ups drawn from the real frontend

Grey mock-ups of the Part III screens. The real frontend runs with made-up answers for every API call; the page is
translated to English, redrawn as grey boxes at the same places, and numbered after the element list of the screen.
Layout must look like the frontend; colours stay black, white and grey.

## Files

| File | Content |
|---|---|
| `run.mjs` | `node run.mjs <workDir> <screens-*.mjs> [file...]` |
| `extract.js`, `mockup.mjs` | page to shapes, shapes to SVG and draw.io |
| `common.mjs` | `uid`, `person`, `me`, `page`, `signedIn(role)` |
| `words.json` | shared Vietnamese to English |
| `words-<group>.json` | translations of one group (beside `screens-<group>.mjs`) |
| `add-words.mjs` | `node add-words.mjs lines.txt words-<group>.json`, lines are `Vietnamese\|English` |
| `screens-1-auth.mjs`, `screens-2-learning.mjs` | finished examples |
| `../wf_index.json` | the screens: `file`, `elements` (number, label, description), `shell` |

## Dev servers (already running; do not stop them)

- `http://localhost:5199` — real API mode: every call to `http://localhost:8080` is answered by fixtures.
- `http://localhost:5198` — built-in learning-path mock of the frontend (`screen.port = 5198`).

Run the dev servers from a separate checkout of the frontend repository and only read its source. Never read any `.env` file.

## A screens file

```js
import { signedIn, page, uid } from './common.mjs'
export const common = signedIn('CUSTOMER')          // ADMIN, EXAMINER, CONTENT_AUTHOR, SALES_STAFF...
export const patterns = [['^(\\d+) bài$', '$1 items']] // optional regex translations of text nodes
export const screens = [
  {
    file: 'administration-users',                   // `file` of wf_index.json
    url: '/admin/users',
    fixtures: [[/GET \/api\/users\?/, page([...])]],  // [regex on "METHOD /path?query", body, status?]; null body = 204
    targets: [undefined, 'text=New account', 'table'], // by element order; undefined = found by label, null = no number
    script: `__click('Tạo mới')`,                   // optional, before drawing; helpers __click(text,n) __fill(sel,v) __wait(ms)
    storage: { key: value },                         // optional localStorage preset
    root: '[role=dialog]', backdrop: true,           // for modal windows
    wait: 5000, height: 800,                         // optional
  },
]
```

Scripts run before translation, so they match the Vietnamese texts. Targets are resolved after translation, so
`text=` targets use the English text. A target that is not `text=` is a CSS selector.

## Output of a run

`<file>.real.png` (the real page, for checking), `<file>.png`, `<file>.svg`, `<file>.drawio`. The log lists
`unplaced` numbers, `untranslated` texts, `not mocked` calls and page errors.

## Done means

1. The real page shows the screen with realistic data, not an error or empty state (unless the screen is that state).
2. No `untranslated` text; made-up data is written in English.
3. Every element number is placed, on the element it names. An element that the frontend really does not have is
   given `null` and reported, with the reason. Never invent UI to fill a number.
4. The picture was looked at once: no overlapping or clipped text that hides meaning.

## In the report

`applied.json` lists the screens whose picture in `../screens/` comes from here. `wf_build.py` and `wf_shoot.mjs`
leave those pictures alone. Two screens keep their drawn wireframe: `grading-by-examiners-speaking-grading` and
`community-community-reports`. Mobile-only screens are drawn by the old tools.
