"""Builds the IELTSPath system context diagram as HTML (inline SVG) and as an editable draw.io file.

Run: python build_context_diagram.py   (writes system-context.html and system-context.drawio next to this script)
Edit the ACTORS and EXTERNALS tables below to change nodes or data flows. Running the script again overwrites
the draw.io file, so after editing it by hand in draw.io, export the image from draw.io instead.

Layout: the system is a circle in the middle. Each flow runs level from a box (where its label sits), turns once
just outside the circle and goes straight to its own point on the rim, so no two flows cross.
"""
import math
from html import escape
from pathlib import Path
from xml.etree import ElementTree

WIDTH, HEIGHT = 1760, 1100
NODE_W, NODE_H = 240, 112
LEFT_X, RIGHT_X = 16, 1504
CX, CY, R = 880, 550, 180
TURN_GAP = 12          # how far outside the circle a flow turns towards the rim
ARC = 72               # degrees above and below the horizontal that the flows spread over on each side
LABEL_WIDTH = 392      # the level run next to a box, where the label is centred
TOP, BOTTOM = 24, 1076
IN_OFFSET, OUT_OFFSET = 44, 98

INK, MUTED, PAPER = '#1f2937', '#4b5563', '#ffffff'
IN_COLOR, OUT_COLOR = '#1f4e79', '#8a6100'

# name, data sent to IELTSPath, data received from IELTSPath (each up to two lines)
ACTORS = [
    ('Learner',
     ['Sign-up, sign-in, answers, essays, voice recordings,', 'activation key, game moves, community posts'],
     ['Courses, lessons, practice and tests, results,', 'feedback, progress, notifications']),
    ('Content Author',
     ['Courses, knowledge points, questions, lessons,', 'tests, vocabulary, videos, AI draft requests'],
     ['Content lists and previews,', 'AI drafts to review']),
    ('Examiner',
     ['Writing and Speaking grades,', 'human review feedback'],
     ['Essays and recordings to grade,', 'due-date notices']),
    ('Administrator',
     ['Users and roles, plans, keys, point adjustments,', 'moderation, system settings'],
     ['User, plan, key, point and', 'moderation records']),
    ('Sales Manager',
     ['Key batches, allocation to sales,', 'reassign and revoke requests'],
     ['Batches, key stock and issuing', 'overview, CSV export']),
    ('Sales Staff',
     ['Requests to issue an activation key'],
     ['Issued key code, own stock', 'and issued keys']),
]

EXTERNALS = [
    ('Google Identity',
     ['Signed ID token,', 'public signing keys'],
     ['Sign-in request with a one-time nonce']),
    ('Email Service',
     ['Send result'],
     ['Verification, password, account,', 'grading and subscription e-mails']),
    ('LLM Provider',
     ['Band scores, feedback, draft content'],
     ['Essays, speaking transcripts,', 'content generation prompts']),
    ('Azure Speech',
     ['Transcript and pronunciation scores'],
     ['Speaking recordings']),
    ('Supabase Storage',
     ['File addresses and signed links'],
     ['Audio, images, recordings, slide files']),
    ('Firebase Cloud Messaging',
     ['Delivery result'],
     ['Push message and device token']),
    ('YouTube',
     ['Video stream'],
     ['Video ID to play']),
]


def layout():
    """Positions shared by both outputs: the boxes, and for every flow its three points and label centre."""
    nodes, flows = [], []
    for kind, entries, node_x, side in (('actor', ACTORS, LEFT_X, -1), ('external', EXTERNALS, RIGHT_X, 1)):
        step = (BOTTOM - TOP - NODE_H) / (len(entries) - 1)
        count = len(entries) * 2
        near = node_x + NODE_W if side < 0 else node_x
        turn = CX + side * (R + TURN_GAP)
        for index, (name, to_system, from_system) in enumerate(entries):
            key = f'{kind}{index}'
            y = round(TOP + index * step)
            nodes.append({'key': key, 'kind': kind, 'name': name, 'x': node_x, 'y': y})
            for position, (offset, lines, inbound) in enumerate(((IN_OFFSET, to_system, True),
                                                                (OUT_OFFSET, from_system, False))):
                rank = index * 2 + position
                angle = math.radians(-ARC + 2 * ARC * rank / (count - 1))
                rim = (round(CX + side * R * math.cos(angle), 1), round(CY + R * math.sin(angle), 1))
                line_y = y + offset
                points = [(near, line_y), (turn, line_y), rim]
                flows.append({'key': f'{key}-{"in" if inbound else "out"}', 'node': key, 'inbound': inbound,
                              'points': points if inbound else points[::-1], 'lines': lines,
                              'node_side': 1 if side < 0 else 0, 'offset': offset,
                              'label': (near - side * LABEL_WIDTH / 2, line_y)})
    return nodes, flows


def text(x, y, value, size=13, colour=INK, weight=400):
    return (f'<text x="{x}" y="{y}" text-anchor="middle" font-size="{size}" font-weight="{weight}" '
            f'fill="{colour}">{escape(value)}</text>')


def write_html(nodes, flows):
    parts = []
    for flow in flows:
        colour, marker, dash = ((IN_COLOR, 'in', '') if flow['inbound']
                                else (OUT_COLOR, 'out', ' stroke-dasharray="6 4"'))
        path = ' '.join(f'{x},{y}' for x, y in flow['points'])
        parts.append(f'<polyline points="{path}" fill="none" stroke="{colour}" stroke-width="1.6"{dash} '
                     f'marker-end="url(#{marker})"/>')
        centre, line_y = flow['label']
        first = line_y - 8 - 17 * (len(flow['lines']) - 1)
        parts.extend(text(round(centre), first + 17 * i, line) for i, line in enumerate(flow['lines']))
    parts.append(f'<circle cx="{CX}" cy="{CY}" r="{R}" fill="#dcfce7" stroke="#15803d" stroke-width="2.4"/>')
    parts.append(text(CX, CY - 8, 'IELTSPath', 30, INK, 700))
    parts.append(text(CX, CY + 26, 'IELTS learning and', 15, MUTED))
    parts.append(text(CX, CY + 47, 'practice system', 15, MUTED))
    for node in nodes:
        actor = node['kind'] == 'actor'
        fill, stroke = ('#dbeafe', '#3b6ea8') if actor else ('#f3f4f6', '#6b7280')
        centre = node['x'] + NODE_W // 2
        parts.append(f'<rect x="{node["x"]}" y="{node["y"]}" width="{NODE_W}" height="{NODE_H}" '
                     f'rx="{10 if actor else 2}" fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>')
        parts.append(text(centre, node['y'] + NODE_H // 2 + 6, node['name'], 17, INK, 700))
    markers = ''.join(
        f'<marker id="{name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" '
        f'orient="auto"><path d="M0 0L10 5L0 10z" fill="{colour}"/></marker>'
        for name, colour in (('in', IN_COLOR), ('out', OUT_COLOR)))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {WIDTH} {HEIGHT}" width="{WIDTH}" '
           f'height="{HEIGHT}" role="img" aria-labelledby="system-context-title system-context-desc" '
           f'font-family="Segoe UI, Arial, sans-serif">'
           '<title id="system-context-title">IELTSPath system context</title>'
           '<desc id="system-context-desc">The data IELTSPath exchanges with its six user roles and seven '
           'external systems.</desc>'
           f'<defs>{markers}</defs><rect width="{WIDTH}" height="{HEIGHT}" fill="{PAPER}"/>{"".join(parts)}</svg>')
    page = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            '<title>IELTSPath system context</title>'
            f'<style>body{{margin:0;background:{PAPER}}}.wrap{{overflow-x:auto}}svg{{min-width:{WIDTH}px;display:block}}</style>'
            f'</head><body><div class="wrap">{svg}</div></body></html>')
    target = Path(__file__).with_name('system-context.html')
    target.write_text(page, encoding='utf-8')
    print(target)


def label_position(points, label_x):
    """draw.io places an edge label by its share of the path length: -1 is the start, 1 the end."""
    lengths = [math.dist(a, b) for a, b in zip(points, points[1:])]
    walked = 0.0
    for (start, end), length in zip(zip(points, points[1:]), lengths):
        if start[1] == end[1] and min(start[0], end[0]) <= label_x <= max(start[0], end[0]):
            return round(2 * (walked + abs(label_x - start[0])) / sum(lengths) - 1, 4)
        walked += length
    raise ValueError('label is not on the level run of its flow')


def write_drawio(nodes, flows):
    """The same diagram as an editable draw.io file: boxes are shapes, flows are connectors with labels."""
    cells = []

    def vertex(key, value, style, x, y, w, h):
        cells.append(f'<mxCell id="{key}" value="{escape(value)}" style="{style}" vertex="1" parent="1">'
                     f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')

    vertex('system', '<b style="font-size:30px">IELTSPath</b><br><br>IELTS learning and<br>practice system',
           'ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#dcfce7;strokeColor=#15803d;strokeWidth=2;fontSize=15;',
           CX - R, CY - R, 2 * R, 2 * R)
    for node in nodes:
        actor = node['kind'] == 'actor'
        fill, stroke = ('#dbeafe', '#3b6ea8') if actor else ('#f3f4f6', '#6b7280')
        vertex(node['key'], f'<b style="font-size:17px">{escape(node["name"])}</b>',
               f'rounded={int(actor)};arcSize=10;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};'
               'strokeWidth=1.5;', node['x'], node['y'], NODE_W, NODE_H)
    for flow in flows:
        points = flow['points']
        rim = points[-1] if flow['inbound'] else points[0]
        node_at = (flow['node_side'], round(flow['offset'] / NODE_H, 4))
        rim_at = (round((rim[0] - (CX - R)) / (2 * R), 4), round((rim[1] - (CY - R)) / (2 * R), 4))
        start, end = (node_at, rim_at) if flow['inbound'] else (rim_at, node_at)
        source, target = (flow['node'], 'system') if flow['inbound'] else ('system', flow['node'])
        colour = IN_COLOR if flow['inbound'] else OUT_COLOR
        style = ('endArrow=block;endFill=1;html=1;rounded=0;fontSize=13;verticalAlign=bottom;'
                 f'labelBackgroundColor=none;strokeWidth=1.6;strokeColor={colour};fontColor={INK};'
                 f'dashed={int(not flow["inbound"])};'
                 f'exitX={start[0]};exitY={start[1]};exitDx=0;exitDy=0;exitPerimeter=0;'
                 f'entryX={end[0]};entryY={end[1]};entryDx=0;entryDy=0;entryPerimeter=0;')
        value = escape('<br>'.join(escape(line) for line in flow['lines']))
        turn = points[1]
        cells.append(f'<mxCell id="{flow["key"]}" value="{value}" style="{style}" edge="1" parent="1" '
                     f'source="{source}" target="{target}">'
                     f'<mxGeometry x="{label_position(points, flow["label"][0])}" relative="1" as="geometry">'
                     f'<Array as="points"><mxPoint x="{turn[0]}" y="{turn[1]}"/></Array></mxGeometry></mxCell>')
    xml = ('<mxfile host="app.diagrams.net"><diagram id="system-context" name="System Context">'
           f'<mxGraphModel dx="{WIDTH}" dy="{HEIGHT}" grid="1" gridSize="8" guides="1" connect="1" arrows="1" '
           f'page="1" pageWidth="{WIDTH}" pageHeight="{HEIGHT}" background="{PAPER}"><root>'
           '<mxCell id="0"/><mxCell id="1" parent="0"/>' + ''.join(cells) +
           '</root></mxGraphModel></diagram></mxfile>')
    ElementTree.fromstring(xml)  # fail here rather than in draw.io if the XML is malformed
    target = Path(__file__).with_name('system-context.drawio')
    target.write_text(xml, encoding='utf-8')
    print(target)


boxes, connections = layout()
write_html(boxes, connections)
write_drawio(boxes, connections)
