// Screenshot every screen of wf_index.json with one headless Edge, driven over the DevTools protocol.
// Starting Edge once per screen hangs now and then. Needs Node 22 or later (global WebSocket).
// Run: node wf_shoot.mjs   (wf_build.py calls it)
import { spawn } from 'node:child_process'
import { existsSync, mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const edge = ['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe'].find((path) => existsSync(path))
// Screens in mockup/applied.json are drawn from the real frontend (mockup/run.mjs) and are not shot again here.
const applied = existsSync(join(here, 'mockup', 'applied.json')) ? JSON.parse(readFileSync(join(here, 'mockup', 'applied.json'), 'utf8')) : []
const screens = JSON.parse(readFileSync(join(here, 'wf_index.json'), 'utf8')).filter((screen) => !applied.includes(screen.file))
const profile = mkdtempSync(join(tmpdir(), 'wf-edge-'))
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

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
await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject })
let nextId = 0
const pending = new Map()
let loaded = () => {}
socket.onmessage = ({ data }) => {
  const message = JSON.parse(data)
  if (message.method === 'Page.loadEventFired') loaded()
  const waiter = pending.get(message.id)
  if (!waiter) return
  pending.delete(message.id)
  if (message.error) waiter.reject(new Error(message.error.message))
  else waiter.resolve(message.result)
}
const send = (method, params = {}) => new Promise((resolve, reject) => {
  nextId += 1
  pending.set(nextId, { resolve, reject })
  socket.send(JSON.stringify({ id: nextId, method, params }))
})

try {
  await send('Page.enable')
  for (const screen of screens) {
    await send('Emulation.setDeviceMetricsOverride', { width: screen.width, height: screen.height, deviceScaleFactor: 2, mobile: false })
    const done = new Promise((resolve) => { loaded = resolve })
    await send('Page.navigate', { url: pathToFileURL(join(here, 'screens', `${screen.file}.html`)).href })
    await done
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    writeFileSync(join(here, 'screens', `${screen.file}.png`), Buffer.from(shot.data, 'base64'))
  }
  console.log('png written:', screens.length)
  await send('Browser.close').catch(() => {})
} finally {
  browser.kill()
  await sleep(1000)
  try { rmSync(profile, { recursive: true, force: true }) } catch { /* Edge may still hold a file */ }
  process.exit(0)
}
