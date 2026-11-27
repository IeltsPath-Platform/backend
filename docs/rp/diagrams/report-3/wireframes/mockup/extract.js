// Runs inside the page: lists what is visible as simple shapes (boxes, lines, pictures, text) at their real places.
(options) => {
  const words = options.words || {}
  const patterns = (options.patterns || []).map(([from, to]) => [new RegExp(from, 'g'), to])
  const english = (value) => {
    const key = value.replace(/\s+/g, ' ').trim()
    if (!key) return value
    if (Object.prototype.hasOwnProperty.call(words, key)) return value.match(/^\s*/)[0] + words[key] + value.match(/\s*$/)[0]
    return patterns.reduce((text, [from, to]) => text.replace(from, to), value)
  }
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    const next = english(node.textContent)
    if (next !== node.textContent) node.textContent = next
  }
  for (const el of document.querySelectorAll('input, textarea')) {
    if (el.placeholder) el.placeholder = english(el.placeholder)
    if (el.value && el.type !== 'password' && el.type !== 'file') { const v = english(el.value); if (v !== el.value) el.value = v }
  }
  const root = document.querySelector(options.root || 'body')
  const width = document.documentElement.clientWidth
  const height = Math.min(window.innerHeight, 4000)
  const shapes = []
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 1
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  const colour = (value) => {
    ctx.clearRect(0, 0, 1, 1)
    ctx.fillStyle = 'rgba(0,0,0,0)'
    ctx.fillStyle = value
    ctx.fillRect(0, 0, 1, 1)
    const [r, g, b, a] = ctx.getImageData(0, 0, 1, 1).data
    return { light: 0.299 * r + 0.587 * g + 0.114 * b, alpha: a / 255 }
  }
  const cut = (a, b) => {
    const x = Math.max(a.x, b.x); const y = Math.max(a.y, b.y)
    return { x, y, w: Math.min(a.x + a.w, b.x + b.w) - x, h: Math.min(a.y + a.h, b.y + b.h) - y }
  }
  const box = (r) => ({ x: r.left, y: r.top, w: r.width, h: r.height })
  const page = { x: 0, y: 0, w: width, h: height }

  const covered = (element, x, y) => {
    const hit = document.elementFromPoint(Math.min(Math.max(x, 1), width - 1), Math.min(Math.max(y, 1), height - 1))
    return Boolean(hit) && !element.contains(hit) && !hit.contains(element)
  }
  const textLines = (node, clip) => {
    const value = node.textContent
    const words = [...value.matchAll(/\S+/g)]
    const lines = []
    const range = document.createRange()
    for (const word of words) {
      range.setStart(node, word.index)
      range.setEnd(node, word.index + word[0].length)
      const rect = range.getClientRects()[0]
      if (!rect) continue
      const last = lines[lines.length - 1]
      if (last && Math.abs(last.top - rect.top) < rect.height / 2) {
        last.words.push(word[0]); last.right = rect.right
      } else lines.push({ top: rect.top, bottom: rect.bottom, left: rect.left, right: rect.right, words: [word[0]] })
    }
    return lines.filter((line) => line.bottom > clip.y + 4 && line.top < clip.y + clip.h - 4
      && line.left < clip.x + clip.w - 4 && line.right > clip.x + 4
      && !covered(node.parentElement, (line.left + line.right) / 2, (line.top + line.bottom) / 2))
  }

  const walk = (element, clip, dark, faded) => {
    const style = getComputedStyle(element)
    if (style.display === 'none') return
    const opacity = Number(style.opacity)
    if (opacity < 0.05) return
    const hidden = style.visibility === 'hidden'
    const rect = box(element.getBoundingClientRect())
    const seen = cut(rect, style.position === 'fixed' ? page : clip)
    const leaf = element.children.length === 0 && rect.h < height * 0.6
    const visible = seen.w > 1 && seen.h > 1 && !hidden && !(leaf && covered(element, seen.x + seen.w / 2, seen.y + seen.h / 2))
    const tag = element.tagName.toLowerCase()
    const radius = Math.min(parseFloat(style.borderTopLeftRadius) || 0, rect.h / 2)
    let isDark = dark
    if (visible) {
      const picture = tag === 'img' || tag === 'video' || tag === 'canvas' || tag === 'iframe'
        || (style.backgroundImage.includes('url(') && rect.w > 60 && rect.h > 60 && element.children.length === 0)
      if (picture || tag === 'svg') {
        if (Math.max(rect.w, rect.h) > 44) shapes.push({ kind: 'image', ...seen })
        else if (rect.w > 5) shapes.push({ kind: 'icon', ...seen })
        return
      }
      const back = colour(style.backgroundColor)
      let fill = null
      let light = back.light
      if (back.alpha > 0.04) light = back.light * back.alpha + 255 * (1 - back.alpha)
      if (style.backgroundImage.includes('gradient')) {
        const stops = style.backgroundImage.match(/(rgba?\([^)]*\)|#[0-9a-f]{3,8}|oklch\([^)]*\)|oklab\([^)]*\))/gi) || []
        const solid = stops.map(colour).filter((c) => c.alpha > 0.5)
        if (solid.length) { light = solid.reduce((sum, c) => sum + c.light, 0) / solid.length; back.alpha = 1 }
      }
      if (back.alpha > 0.04) {
        fill = light > 251 ? '#ffffff' : light > 236 ? '#f3f3f3' : light > 190 ? '#e3e3e3' : light > 120 ? '#b9b9b9' : '#555555'
        isDark = light <= 120
        if (back.alpha < 0.8 && light < 200) isDark = false
      }
      const sides = ['Top', 'Right', 'Bottom', 'Left'].map((side) => {
        const w = parseFloat(style[`border${side}Width`]) || 0
        const c = colour(style[`border${side}Color`])
        return w > 0 && style[`border${side}Style`] !== 'none' && c.alpha > 0.1 ? c : null
      })
      const shadow = style.boxShadow !== 'none' && rect.w > 80 && rect.h > 30
      const all = sides.every(Boolean)
      const stroke = all ? (sides[0].light < 120 ? '#555555' : '#b5b5b5') : shadow ? '#cccccc' : null
      if (fill || stroke) shapes.push({ kind: 'rect', ...seen, fill: fill || 'none', stroke: stroke || 'none', r: radius })
      if (!all) {
        const edges = [[rect.x, rect.y, rect.x + rect.w, rect.y], [rect.x + rect.w, rect.y, rect.x + rect.w, rect.y + rect.h],
          [rect.x, rect.y + rect.h, rect.x + rect.w, rect.y + rect.h], [rect.x, rect.y, rect.x, rect.y + rect.h]]
        sides.forEach((side, i) => {
          if (side && rect.y <= height) shapes.push({ kind: 'line', x1: edges[i][0], y1: edges[i][1], x2: edges[i][2], y2: edges[i][3], colour: side.light < 120 ? '#555555' : '#c4c4c4' })
        })
      }
      if (tag === 'input' || tag === 'textarea' || tag === 'select') {
        const type = element.getAttribute('type')
        if (type === 'checkbox' || type === 'radio') {
          shapes.push({ kind: 'rect', ...seen, fill: element.checked ? '#555555' : '#ffffff', stroke: '#888888', r: type === 'radio' ? seen.h / 2 : 3 })
          return
        }
        const value = tag === 'select' ? element.selectedOptions[0]?.textContent : element.value || element.placeholder
        const pad = parseFloat(style.paddingLeft) || 8
        const size = parseFloat(style.fontSize)
        if (value) shapes.push({ kind: 'text', x: rect.x + pad, y: rect.y + rect.h / 2 + size * 0.35, text: value.trim(), size, weight: 400, colour: element.value || tag === 'select' ? '#333333' : '#9a9a9a', max: rect.w - pad - 8 })
        if (tag === 'select') shapes.push({ kind: 'text', x: rect.x + rect.w - 16, y: rect.y + rect.h / 2 + 4, text: '▾', size: 12, weight: 400, colour: '#777777' })
        return
      }
    }
    const clips = style.overflowX !== 'visible' || style.overflowY !== 'visible'
    const inner = clips ? cut(rect, style.position === 'fixed' ? page : clip) : (style.position === 'fixed' ? page : clip)
    for (const child of element.childNodes) {
      if (child.nodeType === 3 && child.textContent.trim() && !hidden) {
        const size = parseFloat(style.fontSize)
        if (size < 7) continue
        const lines = textLines(child, clips ? inner : clip)
        if (!lines.length) continue
        const ink = colour(style.color)
        if (ink.alpha < 0.2 || (rect.w <= 1 && rect.h <= 1)) continue
        const shade = isDark ? '#ffffff' : ink.light < 120 ? '#333333' : '#8a8a8a'
        const weight = Number(style.fontWeight) >= 600 ? 700 : 400
        const full = child.textContent.replace(/\s+/g, ' ').trim()
        const caps = style.textTransform === 'uppercase'
        shapes.push({ kind: 'node', full, caps, size, weight, colour: shade, align: style.textAlign,
          box: { x: rect.x, w: rect.w }, lines: lines.map((l) => ({ x: l.left, right: l.right, y: l.top + (l.bottom - l.top) / 2 + size * 0.35, text: l.words.join(' ') })) })
      } else if (child.nodeType === 1) walk(child, inner, isDark, faded)
    }
  }

  if (options.backdrop) shapes.push({ kind: 'rect', x: 0, y: 0, w: width, h: height, fill: '#d9d9d9', stroke: 'none', r: 0 })
  walk(root, page, false, false)

  const markers = []
  const shown = (el) => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 && getComputedStyle(el).visibility !== 'hidden' && r.top < height && r.left < width && r.bottom > 0 }
  const norm = (value) => (value || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()
  const scope = document.querySelector(options.root || 'body')
  const own = (el) => norm([...el.childNodes].filter((n) => n.nodeType === 3).map((n) => n.textContent).join(' '))
  const names = (el) => [own(el), norm(el.getAttribute('placeholder')), norm(el.getAttribute('aria-label')), norm(el.textContent.length < 80 ? el.textContent : '')]
  const byText = (wanted) => {
    const inMain = (el) => !el.closest('header, .site-navbar, [class*=navbar]')
    const all = [...scope.querySelectorAll('*')].filter(shown).sort((a, b) => Number(inMain(b)) - Number(inMain(a)))
    for (const test of [(n) => n === wanted, (n) => n.startsWith(wanted), (n) => n.includes(wanted)]) {
      const hit = all.find((el) => names(el).some((n) => n && test(n)))
      if (hit) return hit
    }
    return null
  }
  const find = (target, label) => {
    if (target && target.startsWith('text=')) return byText(norm(target.slice(5)))
    if (target) return [...document.querySelectorAll(target)].find(shown) || null
    const parts = [label, ...label.split(/\s*(?:,|\/|>| - |:)\s*/)].map(norm).filter((p) => p.length > 1)
    for (const part of parts) { const hit = byText(part); if (hit) return hit }
    return null
  }
  const unplaced = []
  for (const { number, target, label } of options.markers || []) {
    const element = find(target, label)
    if (!element) { unplaced.push(`${number}:${label}`); continue }
    const r = element.getBoundingClientRect()
    markers.push({ number, x: r.left, y: r.top, w: r.width, h: r.height })
  }
  return { width, height, shapes, markers, unplaced }
}
