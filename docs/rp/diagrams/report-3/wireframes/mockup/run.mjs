// Grey mock-ups of Report 3 screens, drawn from the real frontend with every API call answered by fixtures.
// Nothing reaches a real backend. Needs Node 22+, Edge and the frontend dev server:
//   VITE_API_BASE_URL=http://localhost:8080 VITE_USE_MOCK_LEARNING=false npx vite --port 5199
//   VITE_API_BASE_URL=http://localhost:8080 VITE_USE_MOCK_LEARNING=true  npx vite --port 5198   (learning path screens)
// Run: node run.mjs <workDir> <screens-*.mjs> [file...]      workDir gets <file>.real.png, .png, .svg, .drawio
import { spawn } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve, dirname, basename } from 'node:path'
import { pathToFileURL, fileURLToPath } from 'node:url'
import { build, svg, drawio } from './mockup.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const [workDir, screensFile, ...only] = process.argv.slice(2)
const extractor = readFileSync(join(here, 'extract.js'), 'utf8').replace(/^(\/\/.*\r?\n)+/, '')
// Shared translations, then the ones of this group (words-<group>.json beside screens-<group>.mjs).
const readWords = (name) => (existsSync(join(here, name)) ? JSON.parse(readFileSync(join(here, name), 'utf8')) : {})
const words = { ...readWords('words.json'), ...readWords(basename(screensFile).replace(/^screens-/, 'words-').replace(/\.mjs$/, '.json')) }
const index = JSON.parse(readFileSync(join(here, '..', 'wf_index.json'), 'utf8'))
const { screens, common = [], patterns = [] } = await import(pathToFileURL(resolve(screensFile)).href)
mkdirSync(workDir, { recursive: true })

const edge = ['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe'].find((path) => existsSync(path))
const profile = mkdtempSync(join(tmpdir(), 'fe-shot-'))
const sleep = (ms) => new Promise((done) => setTimeout(done, ms))
const browser = spawn(edge, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--no-first-run',
  '--disable-extensions', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'], { stdio: 'ignore' })

async function pageSocket() {
  const portFile = join(profile, 'DevToolsActivePort')
  for (let i = 0; i < 100; i += 1) {
    if (existsSync(portFile)) {
      const port = readFileSync(portFile, 'utf8').split('\n')[0]
      const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
      const page = targets.find((target) => target.type === 'page')
      if (page) return page.webSocketDebuggerUrl
    }
    await sleep(200)
  }
  throw new Error('Edge did not open a debugging port')
}

const socket = new WebSocket(await pageSocket())
await new Promise((ok, fail) => { socket.onopen = ok; socket.onerror = fail })
let nextId = 0
const pending = new Map()
let handler = () => {}
socket.onmessage = ({ data }) => {
  const message = JSON.parse(data)
  if (message.method) handler(message)
  const waiter = pending.get(message.id)
  if (!waiter) return
  pending.delete(message.id)
  if (message.error) waiter.reject(new Error(message.error.message))
  else waiter.resolve(message.result)
}
const send = (method, params = {}) => new Promise((ok, fail) => {
  nextId += 1
  pending.set(nextId, { resolve: ok, reject: fail })
  socket.send(JSON.stringify({ id: nextId, method, params }))
})

let fixtures = []
let origin = ''
let missing = new Set()
let errors = []

/** A fixture is [pattern, body, status?]; a null body answers 204. The pattern is tested on "METHOD /path?query". */
async function answer({ requestId, request }) {
  const url = new URL(request.url)
  const cors = [
    { name: 'Access-Control-Allow-Origin', value: origin },
    { name: 'Access-Control-Allow-Credentials', value: 'true' },
    { name: 'Access-Control-Allow-Headers', value: request.headers['Access-Control-Request-Headers'] ?? '*' },
    { name: 'Access-Control-Allow-Methods', value: 'GET, POST, PUT, PATCH, DELETE, OPTIONS' },
  ]
  if (request.method === 'OPTIONS') return send('Fetch.fulfillRequest', { requestId, responseCode: 204, responseHeaders: cors })
  const line = `${request.method} ${url.pathname}${url.search}`
  const found = fixtures.find(([pattern]) => pattern.test(line))
  if (!found) missing.add(line)
  const body = found ? found[1] : { message: 'not mocked' }
  const code = found ? (found[2] ?? (body === null ? 204 : 200)) : 404
  return send('Fetch.fulfillRequest', {
    requestId, responseCode: code,
    responseHeaders: [...cors, { name: 'Content-Type', value: 'application/json' }],
    body: code === 204 ? '' : Buffer.from(JSON.stringify(body)).toString('base64'),
  })
}

handler = (message) => {
  if (message.method === 'Fetch.requestPaused') answer(message.params).catch((error) => errors.push(error.message))
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.exception?.description?.split('\n')[0] ?? 'exception')
}

const evaluate = async (expression) => {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description?.split('\n')[0] ?? 'script failed')
  return result.result.value
}
// Helpers for the scripts of the screens: click or fill by the visible (Vietnamese) text.
const helpers = `window.__click = (text, nth = 0) => { const all = [...document.querySelectorAll('button, a, [role=button], [role=tab], label, summary, [role=menuitem]')].filter((e) => (e.textContent.trim().startsWith(text) || (e.getAttribute('aria-label') || '').startsWith(text)) && e.getBoundingClientRect().width > 0); if (!all[nth]) throw new Error('nothing to click: ' + text); all[nth].click(); return true };
window.__fill = (selector, value) => { const el = document.querySelector(selector); const proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement : el.tagName === 'SELECT' ? HTMLSelectElement : HTMLInputElement; Object.getOwnPropertyDescriptor(proto.prototype, 'value').set.call(el, value); el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); return true };
window.__wait = (ms) => new Promise((done) => setTimeout(done, ms));`

try {
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Fetch.enable', { patterns: [{ urlPattern: 'http://localhost:8080/*' }] })
  await send('Page.addScriptToEvaluateOnNewDocument', { source: helpers })
  for (const screen of screens) {
    if (only.length && !only.includes(screen.file)) continue
    const entry = index.find((item) => item.file === screen.file)
    if (!entry) { console.log(`${screen.file}: NOT IN wf_index`); continue }
    try {
      fixtures = [...(screen.fixtures ?? []), ...(screen.guest ? [[/POST \/auth\/refresh/, null]] : common)]
      missing = new Set()
      errors = []
      origin = `http://localhost:${screen.port ?? 5199}`
      const width = screen.width ?? 1100
      await send('Emulation.setDeviceMetricsOverride', { width, height: screen.height ?? 800, deviceScaleFactor: 1, mobile: false })
      await send('Storage.clearDataForOrigin', { origin, storageTypes: 'local_storage,session_storage' })
      if (screen.storage) {
        await send('Page.navigate', { url: origin + '/terms' })
        await sleep(2500)
        for (const [key, value] of Object.entries(screen.storage)) await evaluate(`localStorage.setItem(${JSON.stringify(key)}, ${JSON.stringify(JSON.stringify(value))})`)
      }
      await send('Page.navigate', { url: 'about:blank' })
      await send('Page.navigate', { url: origin + screen.url })
      await sleep(screen.wait ?? 3500)
      for (const step of [screen.script ?? []].flat()) { await evaluate(`(async () => { ${step} })()`); await sleep(900) }
      // The footer and the floating mascot repeat on every page and are not part of any screen.
      if (!screen.keepFooter) await evaluate(`document.querySelectorAll('.site-footer, [data-pose]').forEach((el) => { (el.matches('[data-pose]') ? el.parentElement && el : el).style.display = 'none' })`)
      const metrics = await send('Page.getLayoutMetrics')
      const height = screen.height ?? Math.max(560, Math.min(Math.ceil(metrics.cssContentSize.height), 2000))
      await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false })
      await sleep(900)
      const shot = await send('Page.captureScreenshot', { format: 'png' })
      writeFileSync(join(workDir, `${screen.file}.real.png`), Buffer.from(shot.data, 'base64'))
      const targets = screen.targets ?? []
      const options = {
        root: screen.root, backdrop: screen.backdrop,
        // The built-in mock names its course "Demo", which does not belong in a report.
        patterns: [...patterns, ['IELTS Demo', 'IELTS Foundation']],
        words: { ...words, ...(screen.words ?? {}) },
        markers: entry.elements.map(([number, label], i) => ({ number, label, target: targets[i] })).filter((m) => m.target !== null || targets[m.number - 1] === undefined),
      }
      const data = await evaluate(`(${extractor})(${JSON.stringify(options)})`)
      const made = build(data, options.words)
      const picture = svg(made)
      writeFileSync(join(workDir, `${screen.file}.svg`), picture)
      writeFileSync(join(workDir, `${screen.file}.drawio`), drawio(made, entry.name))
      const html = join(resolve(workDir), `${screen.file}.html`)
      writeFileSync(html, `<!doctype html><meta charset="utf-8"><body style="margin:0;overflow:hidden"><div style="line-height:0">${picture}</div></body>`)
      await send('Emulation.setDeviceMetricsOverride', { width: made.width, height: made.height, deviceScaleFactor: 1.5, mobile: false })
      await send('Page.navigate', { url: pathToFileURL(html).href })
      await sleep(600)
      const drawn = await send('Page.captureScreenshot', { format: 'png' })
      writeFileSync(join(workDir, `${screen.file}.png`), Buffer.from(drawn.data, 'base64'))
      console.log(`${screen.file}: ${width}x${made.height}`)
      if (made.unplaced.length) console.log(`  unplaced: ${made.unplaced.join(' | ')}`)
      if (made.missing.length) console.log(`  untranslated: ${JSON.stringify(made.missing)}`)
    } catch (error) {
      console.log(`${screen.file}: FAILED ${error.message}`)
    }
    for (const line of missing) console.log(`  not mocked: ${line}`)
    for (const line of [...new Set(errors)].slice(0, 4)) console.log(`  error: ${line}`)
  }
  await send('Browser.close').catch(() => {})
} finally {
  browser.kill()
  await sleep(800)
  try { rmSync(profile, { recursive: true, force: true }) } catch { /* Edge may still hold a file */ }
  process.exit(0)
}
