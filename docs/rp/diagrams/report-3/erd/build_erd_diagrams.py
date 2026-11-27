"""Build the entity relationship diagrams as HTML (SVG) and draw.io files.

Run: python build_erd_diagrams.py   (then render the PNG files with render_erd_png.ps1)
Data lives in erd_data.py. The crow-foot marks are drawn by hand in SVG and use the
matching ER arrows in draw.io, with the same corner points so both files look alike.
"""
from pathlib import Path
from xml.etree import ElementTree
from xml.sax.saxutils import escape

from erd_data import DIAGRAMS
from erd_labels import LABELS

BOX_W, HGAP, VGAP, MARGIN = 205, 170, 84, 30
HEAD_H, LINE_H, PAD = 32, 18, 8
COMPACT_H = 44
INK, LINE, PAPER = '#1f2937', '#374151', '#ffffff'
HEADER_FILL, BORDER = '#dbeafe', '#3b6ea8'
GHOST_FILL, GHOST_BORDER = '#f3f4f6', '#9ca3af'
FONT_TITLE, FONT_ATTR, FONT_LABEL = 15, 12.5, 12.5
NORMAL = {'l': (-1, 0), 'r': (1, 0), 't': (0, -1), 'b': (0, 1)}
DRAWIO_END = {'1': 'ERmandOne', '01': 'ERzeroToOne', '1N': 'ERoneToMany', '0N': 'ERzeroToMany'}


def parse(diagram):
    """Turn the grid description into placed boxes: x, y, w, h in pixels."""
    raw = {}
    for key, spec in diagram['boxes'].items():
        title, col, row, attrs, *extra = spec
        span = next((e for e in extra if isinstance(e, int)), 1)
        ghost = 'ghost' in extra
        natural = COMPACT_H if not attrs else HEAD_H + LINE_H * len(attrs) + PAD
        raw[key] = dict(key=key, title=title, col=col, row=row, attrs=attrs, span=span, ghost=ghost, natural=natural)
    rows = max(b['row'] + 1 for b in raw.values())
    height = [COMPACT_H] * rows
    for box in raw.values():
        if box['span'] == 1:
            height[box['row']] = max(height[box['row']], box['natural'])
    top = [MARGIN]
    gap = diagram.get('vgap', VGAP)
    for value in height:
        top.append(top[-1] + value + gap)
    for box in raw.values():
        box['x'] = MARGIN + box['col'] * (BOX_W + HGAP)
        box['y'] = top[box['row']]
        last = box['row'] + box['span'] - 1
        box['w'] = BOX_W
        box['h'] = box['natural']
    for box in raw.values():
        if box['span'] > 1:  # a hub box is as tall as its outgoing lines need, not stretched over all rows
            count = sum(1 for e in diagram['edges'] if e[0] == box['key'] and e[2] == 'r')
            box['h'] = max(box['natural'], 20 * (count + 1))
    width = MARGIN * 2 + (max(b['col'] for b in raw.values()) + 1) * BOX_W + max(b['col'] for b in raw.values()) * HGAP
    return raw, width, top[-1] - gap + MARGIN


def anchor(box, side, fraction):
    x, y, w, h = box['x'], box['y'], box['w'], box['h']
    return {'l': (x, y + h * fraction), 'r': (x + w, y + h * fraction),
            't': (x + w * fraction, y), 'b': (x + w * fraction, y + h)}[side]


def route(boxes, edge, anchors):
    source, target, s_side, t_side = edge[:4]
    a, b = boxes[source], boxes[target]
    fa, fb = anchors.get((source, target), (None, None))
    if fa is None and s_side in 'lr' and t_side in 'lr':
        low, high = max(a['y'], b['y']), min(a['y'] + a['h'], b['y'] + b['h'])
        if high - low >= 16:
            mid = (low + high) / 2
            fa, fb = (mid - a['y']) / a['h'], (mid - b['y']) / b['h']
    if fa is None and s_side in 'tb' and t_side in 'tb':
        low, high = max(a['x'], b['x']), min(a['x'] + a['w'], b['x'] + b['w'])
        if high - low >= 16:
            mid = (low + high) / 2
            fa, fb = (mid - a['x']) / a['w'], (mid - b['x']) / b['w']
    fa = 0.5 if fa is None else fa
    fb = 0.5 if fb is None else fb
    p, q = anchor(a, s_side, fa), anchor(b, t_side, fb)
    if s_side in 'lr' and t_side in 'lr':
        mid = round((p[0] + q[0]) / 2, 1)
        if a['span'] > 1 and s_side == 'r':
            mid = round(p[0] + 40, 1)  # hub lines share a trunk close to the hub
        points = [p, q] if abs(p[1] - q[1]) < 1 else [p, (mid, p[1]), (mid, q[1]), q]
    elif s_side in 'tb' and t_side in 'tb':
        mid = round((p[1] + q[1]) / 2, 1)
        points = [p, q] if abs(p[0] - q[0]) < 1 else [p, (p[0], mid), (q[0], mid), q]
    elif s_side in 'lr':
        points = [p, (q[0], p[1]), q]
    else:
        points = [p, (p[0], q[1]), q]
    return points, fa, fb


def hub_fractions(diagram, boxes):
    fractions = {}
    for key, box in boxes.items():
        if box['span'] > 1:
            targets = sorted((e for e in diagram['edges'] if e[0] == key and e[2] == 'r'), key=lambda e: boxes[e[1]]['y'] + boxes[e[1]]['h'] / 2)
            for number, e in enumerate(targets):
                fractions[(key, e[1])] = ((number + 1) / (len(targets) + 1), None)
    return fractions


def label_spot(points):
    longest = max(range(len(points) - 1), key=lambda i: abs(points[i][0] - points[i + 1][0]) + abs(points[i][1] - points[i + 1][1]))
    (x0, y0), (x1, y1) = points[longest], points[longest + 1]
    return (x0 + x1) / 2, (y0 + y1) / 2


def mark(point, side, kind, hub=False):
    """SVG path data for the crow-foot mark at the box end of a line."""
    nx, ny = NORMAL[side]
    px, py = -ny, nx
    x, y = point

    def at(distance, across=0):
        return x + nx * distance + px * across, y + ny * distance + py * across

    def seg(p, q):
        return f'M{p[0]:.1f} {p[1]:.1f}L{q[0]:.1f} {q[1]:.1f}'

    half = 6 if hub else 7
    bar = lambda d: seg(at(d, -half), at(d, half))
    foot = seg(at(14, 0), at(0, -8)) + seg(at(14, 0), at(0, 8)) + seg(at(14, 0), at(0, 0))
    circle = lambda d: at(d)
    if kind == '1':
        return bar(10) + bar(16), None
    if kind == '01':
        return bar(10), circle(21)
    if kind == '1N':
        return foot + bar(20), None
    return foot, circle(25)


def text(x, y, value, size, colour=INK, weight=400, anchor_='start', style=''):
    return (f'<text x="{x:g}" y="{y:g}" text-anchor="{anchor_}" font-size="{size}" font-weight="{weight}" '
            f'fill="{colour}"{style}>{escape(value)}</text>')


def write_html(diagram, boxes, width, height):
    name, parts = diagram['name'], []
    for edge in diagram['edges']:
        points, _, _ = route(boxes, edge, diagram.get('edge_anchor', {}))
        hub = boxes[edge[0]]['span'] > 1 and edge[2] == 'r'
        parts.append('<polyline points="' + ' '.join(f'{x:g},{y:g}' for x, y in points) +
                     f'" fill="none" stroke="{LINE}" stroke-width="1.5" stroke-linejoin="round"/>')
        for end, side, kind in ((points[0], edge[2], edge[5]), (points[-1], edge[3], edge[6])):
            path, ring = mark(end, side, kind, hub and end is points[0])
            parts.append(f'<path d="{path}" fill="none" stroke="{LINE}" stroke-width="1.5"/>')
            if ring:
                parts.append(f'<circle cx="{ring[0]:.1f}" cy="{ring[1]:.1f}" r="5" fill="{PAPER}" stroke="{LINE}" stroke-width="1.5"/>')
    for edge in diagram['edges']:
        points, _, _ = route(boxes, edge, diagram.get('edge_anchor', {}))
        x, y = label_spot(points)
        lines = edge[4].split('\n')
        if boxes[edge[0]]['span'] > 1 and edge[2] == 'r' and len(points) == 4:
            x, y = (points[1][0] + points[3][0] - 46) / 2, points[3][1]  # on the last stretch, next to its target
        if len(lines) > 1 and any(abs(points[i][0] - points[i + 1][0]) < 1 and abs(points[i][1] - points[i + 1][1]) > 20 for i in range(len(points) - 1)):
            x += max(len(line) for line in lines) * 3.1 + 10  # beside a vertical line, clear of the marks
        w = max(len(line) for line in lines) * 6.2 + 12
        h = 20 if len(lines) == 1 else 38
        parts.append(f'<rect x="{x - w / 2:.1f}" y="{y - h / 2:.1f}" width="{w:.1f}" height="{h}" fill="{PAPER}"/>')
        for number, line in enumerate(lines):
            parts.append(text(x, y + 4.5 + (number - (len(lines) - 1) / 2) * 16, line, FONT_LABEL, LINE, 400, 'middle', ' font-style="italic"'))
    for box in boxes.values():
        fill, border = (GHOST_FILL, GHOST_BORDER) if box['ghost'] else (PAPER, BORDER)
        dash = ' stroke-dasharray="6 4"' if box['ghost'] else ''
        x, y, w, h = box['x'], box['y'], box['w'], box['h']
        parts.append(f'<rect x="{x:g}" y="{y:g}" width="{w}" height="{h:g}" rx="6" fill="{fill}" stroke="{border}" stroke-width="1.6"{dash}/>')
        header = min(h, HEAD_H if box['attrs'] or box['span'] > 1 else h)
        if not box['ghost']:
            parts.append(f'<path d="M{x:g} {y + header:g}V{y + 6:g}Q{x:g} {y:g} {x + 6:g} {y:g}H{x + w - 6:g}Q{x + w:g} {y:g} {x + w:g} {y + 6:g}V{y + header:g}z" fill="{HEADER_FILL}"/>')
            parts.append(f'<rect x="{x:g}" y="{y:g}" width="{w}" height="{h:g}" rx="6" fill="none" stroke="{border}" stroke-width="1.6"/>')
        parts.append(text(x + w / 2, y + header / 2 + 5.5, box['title'], FONT_TITLE, INK, 700, 'middle'))
        for number, line in enumerate(box['attrs']):
            colour = '#6b7280' if box['ghost'] else '#374151'
            parts.append(text(x + 12, y + HEAD_H + 14 + number * LINE_H, line if box['ghost'] else '+ ' + line, FONT_ATTR, colour))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width:g} {height:g}" width="{width:g}" height="{height:g}" '
           f'role="img" aria-labelledby="{name}-title {name}-desc" font-family="Segoe UI, Arial, sans-serif">'
           f'<title id="{name}-title">{escape(diagram["title"])}</title><desc id="{name}-desc">{escape(diagram["description"])}</desc>'
           f'<rect width="{width:g}" height="{height:g}" fill="{PAPER}"/>' + ''.join(parts) + '</svg>')
    html = (f'<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{escape(diagram["title"])}</title>'
            f'<style>body{{margin:0;background:{PAPER}}}svg{{display:block}}</style></head><body>{svg}</body></html>')
    Path(__file__).with_name(f'{name}.html').write_text(html, encoding='utf-8')


def write_drawio(diagram, boxes, width, height):
    cells = []
    for box in boxes.values():
        fill, border = (GHOST_FILL, GHOST_BORDER) if box['ghost'] else (HEADER_FILL, BORDER)
        dash = 'dashed=1;' if box['ghost'] else ''
        body = '&lt;br&gt;'.join(escape(line if box['ghost'] else '+ ' + line) for line in box['attrs'])
        if box['attrs'] or box['span'] > 1:
            cells.append(f'<mxCell id="{box["key"]}" value="{escape(box["title"])}" style="swimlane;html=1;startSize={HEAD_H};'
                         f'fillColor={fill};strokeColor={border};fontColor={INK};fontStyle=1;fontSize={FONT_TITLE};'
                         f'rounded=1;arcSize=6;swimlaneFillColor=#ffffff;collapsible=0;{dash}" vertex="1" parent="1">'
                         f'<mxGeometry x="{box["x"]:g}" y="{box["y"]:g}" width="{box["w"]}" height="{box["h"]:g}" as="geometry"/></mxCell>')
            if body:
                cells.append(f'<mxCell id="{box["key"]}-fields" value="{body}" style="text;html=1;align=left;verticalAlign=top;'
                             f'whiteSpace=wrap;fontSize={FONT_ATTR};fontColor=#374151;spacingLeft=6;" vertex="1" parent="{box["key"]}">'
                             f'<mxGeometry y="{HEAD_H}" width="{box["w"]}" height="{box["h"] - HEAD_H:g}" as="geometry"/></mxCell>')
        else:
            cells.append(f'<mxCell id="{box["key"]}" value="{escape(box["title"])}" style="rounded=1;arcSize=14;html=1;'
                         f'fillColor={fill};strokeColor={border};fontColor={INK};fontStyle=1;fontSize={FONT_TITLE};{dash}" vertex="1" parent="1">'
                         f'<mxGeometry x="{box["x"]:g}" y="{box["y"]:g}" width="{box["w"]}" height="{box["h"]:g}" as="geometry"/></mxCell>')
    for index, edge in enumerate(diagram['edges']):
        points, fa, fb = route(boxes, edge, diagram.get('edge_anchor', {}))
        exits = {'l': (0, fa), 'r': (1, fa), 't': (fa, 0), 'b': (fa, 1)}[edge[2]]
        enters = {'l': (0, fb), 'r': (1, fb), 't': (fb, 0), 'b': (fb, 1)}[edge[3]]
        style = (f'edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow={DRAWIO_END[edge[6]]};startArrow={DRAWIO_END[edge[5]]};'
                 f'endFill=0;startFill=0;strokeColor={LINE};strokeWidth=1.5;fontSize={FONT_LABEL};fontStyle=2;fontColor={LINE};'
                 f'labelBackgroundColor={PAPER};exitX={exits[0]:.4f};exitY={exits[1]:.4f};exitDx=0;exitDy=0;exitPerimeter=0;'
                 f'entryX={enters[0]:.4f};entryY={enters[1]:.4f};entryDx=0;entryDy=0;entryPerimeter=0;')
        via = ''.join(f'<mxPoint x="{x:g}" y="{y:g}"/>' for x, y in points[1:-1])
        geometry = '<mxGeometry relative="1" as="geometry">' + (f'<Array as="points">{via}</Array>' if via else '') + '</mxGeometry>'
        cells.append(f'<mxCell id="edge{index}" value="{'&lt;br&gt;'.join(escape(l) for l in edge[4].split(chr(10)))}" style="{style}" edge="1" parent="1" '
                     f'source="{edge[0]}" target="{edge[1]}">{geometry}</mxCell>')
    xml = (f'<mxfile host="app.diagrams.net"><diagram id="{diagram["name"]}" name="{escape(diagram["title"])}">'
           f'<mxGraphModel grid="1" gridSize="10" guides="1" connect="1" arrows="1" page="1" pageWidth="{width:g}" '
           f'pageHeight="{height:g}" background="{PAPER}"><root><mxCell id="0"/><mxCell id="1" parent="0"/>'
           + ''.join(cells) + '</root></mxGraphModel></diagram></mxfile>')
    ElementTree.fromstring(xml)
    Path(__file__).with_name(f'{diagram["name"]}.drawio').write_text(xml, encoding='utf-8')


for diagram in DIAGRAMS:
    labels = LABELS.get(diagram['name'], {})
    diagram['edges'] = [e[:4] + (labels.get((e[0], e[1]), e[4]),) + e[5:] for e in diagram['edges']]
    for spec in diagram['boxes'].values():
        for line in spec[3]:
            if len(line) > 30:
                print('LONG', diagram['name'], spec[0], line)
    boxes, width, height = parse(diagram)
    diagram['edge_anchor'] = {**hub_fractions(diagram, boxes), **diagram.get('edge_anchor', {})}
    for edge in diagram['edges']:
        for key in edge[:2]:
            if key not in boxes:
                raise SystemExit(f'{diagram["name"]}: unknown box {key}')
    write_html(diagram, boxes, width, height)
    write_drawio(diagram, boxes, width, height)
    print(diagram['name'], round(width), round(height))










