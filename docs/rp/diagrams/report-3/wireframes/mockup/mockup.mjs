// Turns the shapes of extract.js into a grey mock-up (SVG and draw.io), with English texts and numbered markers.
const esc = (value) => String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
const FONT = 'Segoe UI, Arial, sans-serif'
const widthOf = (text, size, weight) => text.length * size * (weight >= 600 ? 0.58 : 0.53)

function fit(text, size, weight, max) {
  if (widthOf(text, size, weight) <= max * 1.08) return text
  const keep = Math.max(2, Math.floor(max / (size * (weight >= 600 ? 0.58 : 0.53))) - 1)
  return `${text.slice(0, keep)}…`
}

function wrap(text, size, weight, max) {
  const lines = ['']
  for (const word of text.split(' ')) {
    const next = lines[lines.length - 1] ? `${lines[lines.length - 1]} ${word}` : word
    if (widthOf(next, size, weight) > max && lines[lines.length - 1]) lines.push(word)
    else lines[lines.length - 1] = next
  }
  return lines
}

/** English for a Vietnamese string; the missing ones are collected so that none stays untranslated by accident. */
function english(value, words, missing) {
  if (value in words) return words[value]
  if (/[ăâđêôơưàáảãạèéẻẽẹìíỉĩịòóỏõọùúủũụỳýỷỹỵầấẩẫậằắẳẵặềếểễệồốổỗộờớởỡợừứửữự]/i.test(value)) missing.add(value)
  return value
}

export function build(data, words) {
  const missing = new Set()
  const out = []
  for (const shape of data.shapes) {
    if (shape.kind === 'node') {
      const { full, size, weight, colour, lines, align, box } = shape
      const translated = full in words || lines.length === 1 ? english(full, words, missing) : null
      const caps = (value) => (shape.caps ? value.toUpperCase() : value)
      if (translated !== null && translated !== full) {
        const centred = align === 'center'
        const room = Math.max(box.w - 4, lines[0].right - lines[0].x, 24)
        const rows = lines.length > 1 ? wrap(caps(translated), size, weight, room) : [caps(translated)]
        const step = lines.length > 1 ? lines[1].y - lines[0].y : size * 1.4
        rows.forEach((row, i) => {
          const x = centred && lines.length === 1 ? (lines[0].x + lines[0].right) / 2 : centred ? box.x + box.w / 2 : lines[0].x
          out.push({ kind: 'text', x, y: lines[0].y + i * step, text: row, size, weight, colour, anchor: centred ? 'middle' : 'start' })
        })
      } else {
        for (const line of lines) {
          const value = caps(english(line.text, words, missing))
          const length = line.right - line.x
          out.push({ kind: 'text', x: line.x, y: line.y, text: value, size, weight, colour, anchor: 'start', length: value === line.text && value.length > 2 && length > 0 ? length : 0 })
        }
      }
    } else if (shape.kind === 'text') {
      const value = english(shape.text, words, missing)
      out.push({ ...shape, text: shape.max ? fit(value, shape.size, shape.weight, shape.max) : value, anchor: 'start' })
    } else out.push(shape)
  }
  for (const s of out) if (s.kind === 'text' && s.x < 150 && s.text === 'Active') s.text = 'Activity'
  const taken = []
  for (const marker of data.markers) {
    // top right corner of the element, kept inside the picture and off the numbers already placed
    let x = Math.min(Math.max(marker.x + marker.w + 2, 12), data.width - 12)
    const y = Math.min(Math.max(marker.y + 2, 12), data.height - 12)
    while (taken.some(([tx, ty]) => Math.abs(tx - x) < 22 && Math.abs(ty - y) < 22) && x > 36) x -= 24
    taken.push([x, y])
    out.push({ kind: 'mark', x, y, number: marker.number })
  }
  return { width: data.width, height: data.height, shapes: out, missing: [...missing], unplaced: data.unplaced }
}
export function svg({ width, height, shapes }) {
  const parts = [`<rect width="${width}" height="${height}" fill="#ffffff"/>`]
  const n = (v) => Number(v).toFixed(1)
  for (const s of shapes) {
    if (s.kind === 'rect') parts.push(`<rect x="${n(s.x)}" y="${n(s.y)}" width="${n(s.w)}" height="${n(s.h)}" rx="${n(s.r)}" fill="${s.fill}" stroke="${s.stroke}" stroke-width="1.2"/>`)
    else if (s.kind === 'line') parts.push(`<line x1="${n(s.x1)}" y1="${n(s.y1)}" x2="${n(s.x2)}" y2="${n(s.y2)}" stroke="${s.colour}"/>`)
    else if (s.kind === 'image' && s.w > 520) parts.push(`<rect x="${n(s.x)}" y="${n(s.y)}" width="${n(s.w)}" height="${n(s.h)}" rx="6" fill="#eeeeee" stroke="#cccccc"/>`)
    else if (s.kind === 'image') parts.push(`<rect x="${n(s.x)}" y="${n(s.y)}" width="${n(s.w)}" height="${n(s.h)}" rx="6" fill="#eeeeee" stroke="#aaaaaa"/><path d="M${n(s.x)} ${n(s.y)}L${n(s.x + s.w)} ${n(s.y + s.h)}M${n(s.x + s.w)} ${n(s.y)}L${n(s.x)} ${n(s.y + s.h)}" stroke="#bbbbbb" fill="none"/>`)
    else if (s.kind === 'icon') parts.push(`<rect x="${n(s.x + 1)}" y="${n(s.y + 1)}" width="${n(s.w - 2)}" height="${n(s.h - 2)}" rx="3" fill="#bdbdbd"/>`)
    else if (s.kind === 'text') parts.push(`<text x="${n(s.x)}" y="${n(s.y)}" font-family="${FONT}" font-size="${s.size}" font-weight="${s.weight}" fill="${s.colour}" text-anchor="${s.anchor}"${s.length ? ` textLength="${n(s.length)}" lengthAdjust="spacingAndGlyphs"` : ''}>${esc(s.text)}</text>`)
    else if (s.kind === 'mark') parts.push(`<circle cx="${n(s.x)}" cy="${n(s.y)}" r="11" fill="#c0392b"/><text x="${n(s.x)}" y="${n(s.y + 4.5)}" font-family="${FONT}" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">${s.number}</text>`)
  }
  parts.push(`<rect x="0.5" y="0.5" width="${width - 1}" height="${height - 1}" fill="none" stroke="#888888"/>`)
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${parts.join('')}</svg>`
}

export function drawio({ width, height, shapes }, name) {
  const cells = []
  const n = (v) => Number(v).toFixed(1)
  const vertex = (style, x, y, w, h, value = '') => cells.push(`<mxCell id="c${cells.length}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" as="geometry"/></mxCell>`)
  const line = (x1, y1, x2, y2, c) => cells.push(`<mxCell id="c${cells.length}" style="endArrow=none;strokeColor=${c};" edge="1" parent="1"><mxGeometry relative="1" as="geometry"><mxPoint x="${n(x1)}" y="${n(y1)}" as="sourcePoint"/><mxPoint x="${n(x2)}" y="${n(y2)}" as="targetPoint"/></mxGeometry></mxCell>`)
  for (const s of shapes) {
    if (s.kind === 'rect') vertex(`${s.r ? `rounded=1;absoluteArcSize=1;arcSize=${n(2 * s.r)};` : 'rounded=0;'}fillColor=${s.fill};strokeColor=${s.stroke};`, s.x, s.y, s.w, s.h)
    else if (s.kind === 'line') line(s.x1, s.y1, s.x2, s.y2, s.colour)
    else if (s.kind === 'image') { vertex('rounded=0;fillColor=#eeeeee;strokeColor=#aaaaaa;', s.x, s.y, s.w, s.h); line(s.x, s.y, s.x + s.w, s.y + s.h, '#bbbbbb'); line(s.x + s.w, s.y, s.x, s.y + s.h, '#bbbbbb') }
    else if (s.kind === 'icon') vertex('rounded=1;fillColor=#bdbdbd;strokeColor=none;', s.x + 1, s.y + 1, s.w - 2, s.h - 2)
    else if (s.kind === 'text') {
      const w = widthOf(s.text, s.size, s.weight) + 12
      const left = s.anchor === 'middle' ? s.x - w / 2 : s.x
      vertex(`text;html=0;spacing=0;fontFamily=Segoe UI;fontSize=${s.size};fontColor=${s.colour};align=${s.anchor === 'middle' ? 'center' : 'left'};verticalAlign=middle;fontStyle=${s.weight >= 600 ? 1 : 0};`, left, s.y - s.size, w, s.size * 1.35, s.text)
    } else if (s.kind === 'mark') vertex('ellipse;fillColor=#c0392b;strokeColor=none;fontColor=#ffffff;fontStyle=1;', s.x - 11, s.y - 11, 22, 22, String(s.number))
  }
  return `<mxfile host="app.diagrams.net"><diagram id="wireframe" name="${esc(name)}"><mxGraphModel dx="800" dy="600" grid="0" page="1" pageWidth="${width}" pageHeight="${height}"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join('')}</root></mxGraphModel></diagram></mxfile>`
}
