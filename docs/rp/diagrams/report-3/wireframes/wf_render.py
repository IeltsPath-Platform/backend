"""Wireframe renderer for Part III of the SRS.

A screen is a dict: name, shell ('site' | 'staff' | 'modal' | 'mobile' | 'bare'), area (label for the shell), items.
An item is a tuple (kind, label, description) or (kind, [labels], description). Kinds:
  title, text, field, select, textarea, check, button, button2, link, tabs, table, cards, stats, image, chips, steps,
  split (kind='split', left items, right items), group (a labelled box that holds items).
Every item except text/title helpers gets a number; render() returns the svg and the numbered element list.
"""
from xml.sax.saxutils import escape

QUOTE = {'"': '&quot;'}

INK, GREY, LIGHT, LINE, MARK, DARK = '#333333', '#8a8a8a', '#f2f2f2', '#b5b5b5', '#c0392b', '#555555'
FONT = 'Segoe UI, Arial, sans-serif'
NUMBERED = {'title', 'field', 'select', 'textarea', 'check', 'button', 'button2', 'link', 'tabs', 'table', 'cards',
            'stats', 'image', 'chips', 'steps', 'text', 'panel'}


class Canvas:
    def __init__(self):
        self.parts = []
        self.count = 0
        self.elements = []
        self.shapes = []  # the same drawing as primitives, for the draw.io file

    def rect(self, x, y, w, h, fill='#ffffff', stroke=LINE, r=4, dash=''):
        d = f' stroke-dasharray="{dash}"' if dash else ''
        self.shapes.append(('rect', x, y, w, h, fill, stroke, r, dash))
        self.parts.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{r}" fill="{fill}" '
                          f'stroke="{stroke}" stroke-width="1.2"{d}/>')

    def line(self, x1, y1, x2, y2, colour=LINE):
        self.shapes.append(('line', x1, y1, x2, y2, colour))
        self.parts.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{colour}"/>')

    def text(self, x, y, value, size=12, weight=400, colour=INK, anchor='start'):
        self.shapes.append(('text', x, y, str(value), size, weight, colour, anchor))
        self.parts.append(f'<text x="{x:.1f}" y="{y:.1f}" font-family="{FONT}" font-size="{size}" font-weight="{weight}" '
                          f'fill="{colour}" text-anchor="{anchor}">{escape(str(value))}</text>')

    def marker(self, x, y, label, description):
        self.count += 1
        self.shapes.append(('circle', x, y, 9.5, MARK))
        self.parts.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="9.5" fill="{MARK}"/>')
        self.text(x, y + 4, self.count, 11, 700, '#ffffff', 'middle')
        self.elements.append((self.count, label, description))


def clip(value, limit):
    value = str(value)
    return value if len(value) <= limit else value[:limit - 1] + '…'


def wrap(value, limit):
    words, lines, line = str(value).split(), [], ''
    for word in words:
        if len(line) + len(word) + 1 > limit and line:
            lines.append(line)
            line = word
        else:
            line = (line + ' ' + word).strip()
    return lines + [line] if line else lines


def lay(canvas, items, x, y, width, mark_x, scale=1.0):
    """Draw items from y downwards inside [x, x+width]; return the new y."""
    box = min(width, 440 if width > 440 else width)
    chars = int(width / 6.4)
    for item in items:
        kind, label = item[0], item[1]
        desc = item[2] if len(item) > 2 else ''
        top = y
        mx = mark_x
        if kind == 'split':
            left_end = lay(canvas, item[1], x, y, width / 2 - 30, x + width / 2 - 14)
            right_end = lay(canvas, item[2], x + width / 2 + 8, y, width / 2 - 8, mark_x)
            y = max(left_end, right_end)
            continue
        if kind == 'title':
            canvas.text(x, y + 20, clip(label, chars - 6), 16, 700)
            y += 34
        elif kind == 'text':
            lines = wrap(label, chars)[:3]
            for i, line in enumerate(lines):
                canvas.text(x, y + 14 + i * 16, line, 11.5, 400, GREY)
            y += 8 + 16 * len(lines)
        elif kind in ('field', 'select', 'textarea'):
            canvas.text(x, y + 12, clip(label, 60), 11.5, 600)
            height = 64 if kind == 'textarea' else 28
            canvas.rect(x, y + 18, box, height, '#ffffff', LINE, 4)
            if kind == 'select':
                canvas.text(x + box - 16, y + 37, '▾', 11, 400, GREY, 'middle')
            else:
                canvas.text(x + 8, y + 37, clip(item[3], 48) if len(item) > 3 else '', 11, 400, '#b0b0b0')
            mx = min(mark_x, x + box + 16)
            y += 18 + height + 12
        elif kind == 'check':
            canvas.rect(x, y + 2, 14, 14, '#ffffff', LINE, 2)
            canvas.text(x + 22, y + 14, clip(label, chars - 4), 11.5)
            y += 26
        elif kind in ('button', 'button2'):
            w = min(box, max(110, len(label) * 8 + 40))
            canvas.rect(x, y, w, 30, DARK if kind == 'button' else '#ffffff', DARK if kind == 'button' else '#888888', 6)
            canvas.text(x + w / 2, y + 20, clip(label, 28), 12, 700 if kind == 'button' else 400,
                        '#ffffff' if kind == 'button' else INK, 'middle')
            mx = min(mark_x, x + w + 16)
            y += 42
        elif kind == 'link':
            canvas.text(x, y + 14, clip(label, chars - 4), 11.5, 400, '#2a6fb0')
            y += 24
        elif kind == 'tabs':
            names = label
            tab_w = min(120, width / max(1, len(names)))
            for i, name in enumerate(names):
                active = item[3] if len(item) > 3 else 0
                canvas.rect(x + i * tab_w, y, tab_w, 28, '#ffffff' if i == active else LIGHT, LINE, 0)
                canvas.text(x + i * tab_w + tab_w / 2, y + 18, clip(name, int(tab_w / 6.5)), 11,
                            700 if i == active else 400, INK if i == active else GREY, 'middle')
            y += 40
            label = ' / '.join(names)
        elif kind == 'table':
            names = label
            col = width / max(1, len(names))
            canvas.rect(x, y, width, 24, '#e9e9e9', LINE, 0)
            for i, name in enumerate(names):
                canvas.text(x + i * col + 8, y + 16, clip(name, max(4, int(col / 6.5))), 11, 700)
            for r in range(3):
                canvas.rect(x, y + 24 + r * 22, width, 22, '#ffffff', LINE, 0)
                for i in range(len(names)):
                    canvas.rect(x + i * col + 8, y + 24 + r * 22 + 8, min(col - 24, 50 + 14 * ((r + i) % 3)), 6, '#dddddd', '#dddddd', 3)
            y += 24 + 3 * 22 + 14
            label = ', '.join(names)
        elif kind == 'cards':
            count = 3 if width > 300 else 2
            card = (width - (count - 1) * 10) / count
            for i in range(count):
                canvas.rect(x + i * (card + 10), y, card, 64, '#ffffff', LINE, 6)
                canvas.rect(x + i * (card + 10) + 8, y + 10, card - 16, 8, '#dddddd', '#dddddd', 3)
                canvas.rect(x + i * (card + 10) + 8, y + 26, card * 0.6, 6, '#e6e6e6', '#e6e6e6', 3)
                canvas.text(x + i * (card + 10) + 8, y + 54, clip(label, int(card / 6.4)), 10.5, 400, GREY)
            y += 78
        elif kind == 'stats':
            names = label
            count = max(1, len(names))
            tile = (width - (count - 1) * 8) / count
            for i, name in enumerate(names):
                canvas.rect(x + i * (tile + 8), y, tile, 52, LIGHT, LINE, 6)
                canvas.text(x + i * (tile + 8) + tile / 2, y + 24, '0', 15, 700, INK, 'middle')
                canvas.text(x + i * (tile + 8) + tile / 2, y + 42, clip(name, int(tile / 6)), 10, 400, GREY, 'middle')
            y += 66
            label = ', '.join(names)
        elif kind == 'image':
            canvas.rect(x, y, box, 84, LIGHT, LINE, 6, '4 3')
            canvas.text(x + box / 2, y + 46, clip(label, 50), 11, 400, GREY, 'middle')
            mx = min(mark_x, x + box + 16)
            y += 98
        elif kind == 'chips':
            cx = x
            for name in label:
                w = len(name) * 6.6 + 22
                if cx + w > x + width:
                    break
                canvas.rect(cx, y, w, 24, LIGHT, LINE, 12)
                canvas.text(cx + w / 2, y + 16, name, 10.5, 400, INK, 'middle')
                cx += w + 8
            y += 36
            label = ', '.join(label)
        elif kind == 'steps':
            names = label
            gap = width / max(1, len(names))
            for i, name in enumerate(names):
                canvas.rect(x + i * gap + 2, y, gap - 6, 30, '#ffffff', LINE, 15)
                canvas.text(x + i * gap + gap / 2 - 1, y + 19, clip(name, int(gap / 6.4)), 10.5, 400, INK, 'middle')
            y += 44
            label = ' > '.join(names)
        elif kind == 'panel':
            y += 16
            canvas.marker(mark_x, y + 6, label, desc)
            end = lay(canvas, item[3], x + 10, y + 10, width - 20, mark_x - 6)
            canvas.rect(x, y, width, end - y + 2, 'none', '#999999', 6)
            canvas.text(x + 10, y - 4, clip(label, 50), 10.5, 700, GREY)
            y = end + 12
            continue
        if kind in NUMBERED and desc:
            canvas.marker(mx, top + (8 if kind in ('field', 'select', 'textarea') else 12), label, desc)
    return y


def draw(screen):
    """Return (canvas, width, height)."""
    canvas = Canvas()
    shell = screen.get('shell', 'site')
    area = screen.get('area', 'IELTSPath')
    mobile = shell == 'mobile'
    width = 330 if mobile else 760
    # lay out on a scratch canvas first to learn the height
    probe = Canvas()
    if mobile:
        x0, cw, mark_x, y0 = 22, 266, 312, 78
    elif shell == 'staff':
        x0, cw, mark_x, y0 = 168, 540, 736, 84
    elif shell == 'modal':
        x0, cw, mark_x, y0 = 170, 420, 584, 120
    else:
        x0, cw, mark_x, y0 = 50, 640, 736, 86
    end = lay(probe, screen['items'], x0, y0, cw, mark_x)
    pad = 70 if mobile else (60 if shell == 'modal' else 30)
    height = int(max(end + pad, 260 if not mobile else 420))
    canvas.parts.append(f'<rect width="{width}" height="{height}" fill="#ffffff"/>')
    if mobile:
        canvas.rect(6, 6, width - 12, height - 12, '#ffffff', '#777777', 26)
        canvas.rect(110, 12, 110, 8, '#dddddd', '#dddddd', 4)
        canvas.text(22, 52, clip(screen['name'], 28), 14, 700)
        canvas.line(14, 62, width - 14, 62)
        bar = screen.get('tabs', ['Home', 'Learn', 'Practice', 'Account'])
        if bar:
            canvas.rect(14, height - 56, width - 28, 40, LIGHT, LINE, 10)
        for i, tab in enumerate(bar):
            canvas.text(14 + (width - 28) * (i + 0.5) / len(bar), height - 31, clip(tab, 9), 10, 400, GREY, 'middle')
    else:
        canvas.rect(6, 6, width - 12, height - 12, '#ffffff', '#888888', 8)
        canvas.rect(6, 6, width - 12, 26, LIGHT, '#888888', 8)
        canvas.text(22, 24, f'IELTSPath  -  {clip(screen["name"], 50)}', 11, 400, GREY)
        if shell == 'staff':
            canvas.rect(6, 32, 150, height - 38, '#ececec', LINE, 0)
            canvas.text(20, 56, 'IELTSPath', 14, 700)
            canvas.text(20, 74, clip(area, 18), 10.5, 400, GREY)
            for i, entry in enumerate(screen.get('menu', ['Menu item 1', 'Menu item 2', 'Menu item 3'])):
                canvas.text(20, 104 + i * 22, clip(entry, 20), 11, 700 if entry.lower() in screen['name'].lower() else 400,
                            INK if entry.lower() in screen['name'].lower() else GREY)
        elif shell == 'modal':
            canvas.rect(6, 32, width - 12, height - 38, '#d9d9d9', '#d9d9d9', 0)
            canvas.rect(150, 70, 460, height - 110, '#ffffff', '#777777', 10)
            canvas.text(170, 100, clip(screen['name'], 44), 14, 700)
            canvas.line(150, 110, 610, 110)
        elif shell == 'bare':
            pass
        else:
            canvas.rect(6, 32, width - 12, 36, '#e9e9e9', LINE, 0)
            canvas.text(24, 55, 'IELTSPath', 14, 700)
            for i, entry in enumerate(screen.get('menu', ['Home', 'Courses', 'Practice', 'Vocabulary'])):
                canvas.text(250 + i * 90, 55, clip(entry, 12), 11.5, 400, GREY)
    lay(canvas, screen['items'], x0, y0, cw, mark_x)
    return canvas, width, height


def render(screen):
    """Return (svg, width, height, elements)."""
    canvas, width, height = draw(screen)
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">'
           + ''.join(canvas.parts) + '</svg>')
    return svg, width, height, canvas.elements


def drawio(screen):
    """The same wireframe as an editable draw.io file."""
    canvas, width, height = draw(screen)
    cells = []

    def vertex(style, x, y, w, h, value=''):
        cells.append(f'<mxCell id="c{len(cells)}" value="{escape(value, QUOTE)}" style="{style}" vertex="1" parent="1">'
                     f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/></mxCell>')

    for shape in canvas.shapes:
        kind = shape[0]
        if kind == 'rect':
            _, x, y, w, h, fill, stroke, r, dash = shape
            round_ = f'rounded=1;absoluteArcSize=1;arcSize={2 * r};' if r else 'rounded=0;'
            vertex(f'{round_}fillColor={fill};strokeColor={stroke};' + ('dashed=1;' if dash else ''), x, y, w, h)
        elif kind == 'line':
            _, x1, y1, x2, y2, colour = shape
            cells.append(f'<mxCell id="c{len(cells)}" style="endArrow=none;strokeColor={colour};" edge="1" parent="1">'
                         f'<mxGeometry relative="1" as="geometry"><mxPoint x="{x1:.1f}" y="{y1:.1f}" as="sourcePoint"/>'
                         f'<mxPoint x="{x2:.1f}" y="{y2:.1f}" as="targetPoint"/></mxGeometry></mxCell>')
        elif kind == 'circle':
            _, cx, cy, r, fill = shape
            vertex(f'ellipse;fillColor={fill};strokeColor=none;', cx - r, cy - r, 2 * r, 2 * r)
        else:
            _, x, y, value, size, weight, colour, anchor = shape
            w = len(value) * size * 0.62 + 12
            left, align = {'start': (x, 'left'), 'middle': (x - w / 2, 'center'), 'end': (x - w, 'right')}[anchor]
            vertex(f'text;html=0;spacing=0;fontFamily=Segoe UI;fontSize={size};fontColor={colour};align={align};'
                   f'verticalAlign=middle;fontStyle={1 if weight >= 600 else 0};', left, y - size, w, size * 1.35, value)
    name = escape(screen['name'], QUOTE)
    return (f'<mxfile host="app.diagrams.net"><diagram id="wireframe" name="{name}">'
            f'<mxGraphModel dx="800" dy="600" grid="0" page="1" pageWidth="{width}" pageHeight="{height}"><root>'
            '<mxCell id="0"/><mxCell id="1" parent="0"/>' + ''.join(cells) + '</root></mxGraphModel></diagram></mxfile>')
