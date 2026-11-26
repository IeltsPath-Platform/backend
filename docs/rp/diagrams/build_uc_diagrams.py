"""Build the use case diagrams of SRS section 4.3 as HTML (SVG) and draw.io files.

Run: python build_uc_diagrams.py   (render the PNG files afterwards with Edge, see render step in the SRS notes)
Layout is manual: every use case sits in a column (0, 1 or 2) and a row. Column 0 holds the use cases the actor
starts; columns 1 and 2 hold use cases that another one includes or extends.
"""
import math
from pathlib import Path
from xml.etree import ElementTree
from xml.sax.saxutils import escape

INK, LINE, PAPER = '#1f2937', '#374151', '#ffffff'
OVAL_FILL, OVAL_BORDER = '#dbeafe', '#3b6ea8'
ACTOR_COLOUR = '#1f4e79'
EXT_FILL, EXT_BORDER = '#f3f4f6', '#6b7280'
FONT = 15
COL_X = [380, 740, 1100]
ROW_H = 70
RX, RY = 150, 26
TOP = 74

DIAGRAMS = [
    {
        'name': 'uc-01-guest', 'title': 'Use cases for Guest', 'caption': 'Figure 4.1 — Use cases of the Guest',
        'actors': [('Guest', [0, 1, 2, 3, 4, 5, 6, 7])],
        'externals': [('Google Identity', ['google'], 3), ('Email Service', ['verify', 'reset'], 0.5)],
        'ucs': {
            'signup': ('Sign Up', 0, 0), 'verify': ('Verify E-mail Address', 1, 0),
            'signin': ('Sign In with Password', 0, 2), 'google': ('Sign In with Google', 0, 3),
            'reset': ('Reset Forgotten Password', 0, 1),
            'vocab': ('Browse Vocabulary', 0, 4), 'word': ('View Word Detail', 1, 4),
        },
        'actor_links': [['signup', 'signin', 'google', 'reset', 'vocab']],
        'relations': [('signup', 'verify', 'include'), ('word', 'vocab', 'extend')],
    },
    {
        'name': 'uc-02-learner-account', 'title': 'Use cases for Learner: account, points and plans',
        'caption': 'Figure 4.2 — Use cases of the Learner: account, points and plans',
        'actors': [('Learner', None)],
        'externals': [('Supabase Storage', ['profile'], 0), ('Firebase Cloud Messaging', ['notifs'], 4), ('Email Service', ['prefs'], 5)],
        'ucs': {
            'profile': ('Edit Profile', 0, 0), 'goal': ('Set Learning Goal', 0, 1), 'password': ('Change Password', 0, 2),
            'signout': ('Sign Out', 0, 3), 'notifs': ('View Notifications', 0, 4),
            'prefs': ('Manage Notification Preferences', 0, 5), 'plans': ('View Plans and Pricing', 0, 6),
            'key': ('Activate Key', 0, 7), 'points': ('View Points and Subscription', 0, 8), 'examiners': ('Browse Examiners', 0, 9),
        },
        'actor_links': [None],
        'relations': [],
    },
    {
        'name': 'uc-03-learner-learning-path', 'title': 'Use cases for Learner: placement test and learning path',
        'caption': 'Figure 4.3 — Use cases of the Learner: placement test and learning path',
        'actors': [('Learner', None)],
        'externals': [('LLM Provider', ['essay'], 5), ('Azure Speech', ['prec'], 1), ('Supabase Storage', ['prec'], 2)],
        'ucs': {
            'pstart': ('Start Placement Test', 0, 0), 'ptake': ('Take Placement Test', 0, 1),
            'prec': ('Record Placement Speaking', 1, 1), 'pres': ('View Placement Result', 0, 2),
            'courses': ('View Courses', 0, 3), 'unlock': ('Unlock Course Skill with Points', 1, 3),
            'topics': ('View Topics of a Course', 0, 4), 'study': ('Study a Lesson', 0, 5),
            'answer': ('Answer Lesson Exercises', 1, 5), 'essay': ('Write Essay in a Lesson', 2, 5),
            'hearts': ('Refill Hearts with Points', 2, 6), 'practise': ('Practise a Lesson', 0, 6),
            'ttest': ('Take Topic Test', 0, 7), 'tres': ('View Topic Test Result', 1, 7),
            'final': ('Take Course Final Test', 0, 8), 'review': ('Review a Weak Skill', 0, 9),
            'prereq': ('Take Prerequisite Check', 1, 9), 'basket': ('Use Review Basket', 0, 10),
            'tree': ('View Knowledge Tree', 0, 11), 'overview': ('View Overview and Achievements', 0, 12),
        },
        'actor_links': [None],
        'relations': [('ptake', 'prec', 'extend', True), ('courses', 'unlock', 'extend', True), ('study', 'answer', 'include'),
                      ('essay', 'answer', 'extend'), ('hearts', 'answer', 'extend'), ('ttest', 'tres', 'include'),
                      ('prereq', 'review', 'extend'), ('basket', 'review', 'include')],
    },
    {
        'name': 'uc-04-learner-practice-grading', 'title': 'Use cases for Learner: practice tests and grading',
        'caption': 'Figure 4.4 — Use cases of the Learner: practice tests and grading',
        'actors': [('Learner', None)],
        'externals': [('LLM Provider', ['write'], 4), ('Azure Speech', ['speak'], 7), ('Supabase Storage', ['record'], 8.5)],
        'ucs': {
            'browse': ('Browse Practice Tests', 0, 0), 'reading': ('Take Reading Practice', 0, 1),
            'listening': ('Take Listening Practice', 0, 2), 'attempt': ('Review Practice Attempt', 1, 1.5),
            'history': ('View Reading History', 0, 3), 'write': ('Write Essay for Grading', 0, 4),
            'wresult': ('View Writing Result', 0, 5), 'rewrite': ('Rewrite Essay', 1, 5),
            'request': ('Request Examiner Review of Essay', 1, 6), 'speak': ('Submit Speaking for Grading', 0, 7),
            'record': ('Record Speaking Answers', 1, 8), 'sresult': ('View Speaking Result', 0, 8),
            'job': ('Track Grading Job', 0, 9), 'subs': ('View Submission History', 0, 10),
        },
        'actor_links': [None],
        'relations': [('attempt', 'reading', 'extend'), ('attempt', 'listening', 'extend'), ('write', 'wresult', 'include'),
                      ('rewrite', 'wresult', 'extend'), ('request', 'wresult', 'extend'), ('speak', 'record', 'include')],
    },
    {
        'name': 'uc-05-learner-vocabulary', 'title': 'Use cases for Learner: vocabulary, flashcards and video',
        'caption': 'Figure 4.5 — Use cases of the Learner: vocabulary, flashcards and video',
        'actors': [('Learner', None)],
        'externals': [('YouTube', ['dictate'], 7)],
        'ucs': {
            'browse': ('Browse Vocabulary', 0, 0), 'word': ('View Word Detail', 1, 0), 'set': ('Study Vocabulary Set', 0, 1),
            'map': ('View Vocabulary Map', 0, 2), 'save': ('Save Word to Notebook', 0, 3), 'notebook': ('Review Notebook', 0, 4),
            'decks': ('Manage Flashcard Decks', 0, 5), 'cards': ('Study Flashcards', 0, 6),
            'videos': ('Browse Dictation Videos', 0, 7), 'dictate': ('Practise Video Dictation', 1, 7),
        },
        'actor_links': [None],
        'relations': [('word', 'browse', 'extend'), ('dictate', 'videos', 'extend')],
    },
    {
        'name': 'uc-06-learner-games-community', 'title': 'Use cases for Learner: games and community',
        'caption': 'Figure 4.6 — Use cases of the Learner: games and community',
        'actors': [('Learner', None)],
        'externals': [('Supabase Storage', ['write'], 8)],
        'ucs': {
            'catalog': ('View Game Catalog', 0, 0), 'setup': ('Set Up Game', 0, 1), 'join': ('Join Game Room', 0, 2),
            'wordly': ('Play Wordly', 0, 3), 'rocket': ('Play Rocket', 0, 4), 'gresult': ('View Game Result', 0, 5),
            'feed': ('View Community Feed', 0, 6), 'comment': ('Comment on Post', 1, 5.5),
            'react': ('React to Post or Comment', 1, 6.5), 'report': ('Report Content', 1, 7.5),
            'write': ('Write Post', 0, 8), 'profile': ('View Learner Profile', 0, 9), 'bookmark': ('Bookmark Post', 0, 10),
        },
        'actor_links': [None],
        'relations': [('comment', 'feed', 'extend'), ('react', 'feed', 'extend'), ('report', 'feed', 'extend')],
    },
    {
        'name': 'uc-07-content-author', 'title': 'Use cases for Content Author', 'caption': 'Figure 4.7 — Use cases of the Content Author',
        'actors': [('Content Author', None)],
        'externals': [('LLM Provider', ['generate', 'evaluate'], 11.5)],
        'ucs': {
            'courses': ('Manage Courses', 0, 0), 'questions': ('Manage Questions', 0, 1), 'packages': ('Manage Test Packages', 0, 2),
            'placement': ('Manage Placement Tests', 0, 3), 'library': ('Manage Practice Library', 0, 4),
            'vocab': ('Manage Vocabulary Content', 0, 5), 'rubrics': ('Manage Grading Rubrics', 0, 6),
            'errors': ('Manage Error Types', 0, 7), 'graph': ('Manage Knowledge Graph', 0, 8), 'code': ('Manage Code Settings', 0, 9),
            'import': ('Import Course', 0, 10), 'generate': ('Generate AI Draft', 0, 11), 'review': ('Review AI Draft', 1, 10),
            'evaluate': ('Evaluate AI Grading', 0, 12), 'work': ('Follow Work Items', 0, 13), 'check': ('Check Examiner Grades', 0, 14),
        },
        'actor_links': [None],
        'relations': [('generate', 'review', 'include')],
    },
    {
        'name': 'uc-08-examiner', 'title': 'Use cases for Examiner', 'caption': 'Figure 4.8 — Use cases of the Examiner',
        'actors': [('Examiner', None)],
        'externals': [('Supabase Storage', ['speaking'], 2)],
        'ucs': {
            'queue': ('View Grading Queue', 0, 0), 'writing': ('Grade Writing Submission', 0, 1), 'review': ('Review Graded Essay', 1, 1),
            'speaking': ('Grade Speaking Submission', 0, 2), 'profile': ('Manage Examiner Profile', 0, 3),
            'graph': ('View Knowledge Graph as Examiner', 0, 4),
        },
        'actor_links': [None],
        'relations': [('review', 'writing', 'extend')],
    },
    {
        'name': 'uc-09-administrator', 'title': 'Use cases for Administrator', 'caption': 'Figure 4.9 — Use cases of the Administrator',
        'actors': [('Administrator', None)],
        'externals': [],
        'ucs': {
            'users': ('Manage Users', 0, 0), 'reset': ('Reset User Password', 1, 0), 'history': ('View Login History', 1, 1),
            'overview': ('View System Overview', 0, 2), 'services': ('View Service Status', 0, 3),
            'activity': ('View Activity Log', 0, 4), 'moderate': ('Moderate Community Reports', 0, 5),
        },
        'actor_links': [None],
        'relations': [('reset', 'users', 'extend'), ('history', 'users', 'extend')],
    },
    {
        'name': 'uc-10-key-sales', 'title': 'Use cases for Sales Manager and Sales Staff',
        'caption': 'Figure 4.10 — Use cases of the Sales Manager and the Sales Staff',
        'actors': [('Sales Manager', [0, 1, 2, 3, 4, 5]), ('Sales Staff', [6, 7, 8])],
        'externals': [],
        'ucs': {
            'overview': ('View Key Overview', 0, 0), 'batch': ('Create Key Batch', 0, 1), 'batches': ('View Key Batches', 0, 2),
            'allocate': ('Allocate Keys to Sales Staff', 0, 3), 'decide': ('Decide Key Request', 0, 4),
            'direct': ('Issue Key Directly', 0, 5), 'stock': ('View Own Key Stock', 0, 6),
            'issue': ('Issue Key to Learner', 0, 7), 'issued': ('View Issued Keys', 0, 8),
        },
        'actor_links': [['overview', 'batch', 'batches', 'allocate', 'decide', 'direct'], ['stock', 'issue', 'issued']],
        'relations': [],
    },
]


def ellipse_edge(cx, cy, tx, ty):
    """Point on the oval (centre cx, cy) in the direction of (tx, ty)."""
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    scale = 1 / math.sqrt((dx / RX) ** 2 + (dy / RY) ** 2)
    return cx + dx * scale, cy + dy * scale


def wrap(label, limit=30):
    if len(label) <= limit:
        return [label]
    words, lines, current = label.split(), [], ''
    for word in words:
        if current and len(current) + 1 + len(word) > limit:
            lines.append(current)
            current = word
        else:
            current = (current + ' ' + word).strip()
    lines.append(current)
    return lines


def layout(diagram):
    ucs = {}
    for key, (label, col, row) in diagram['ucs'].items():
        ucs[key] = dict(key=key, label=label, cx=COL_X[col], cy=TOP + 40 + row * ROW_H, col=col, row=row)
    last_row = max(u['row'] for u in ucs.values())
    columns = max(u['col'] for u in ucs.values()) + 1
    height = TOP + 40 + last_row * ROW_H + 70
    boundary_right = COL_X[columns - 1] + RX + 40
    externals_x = boundary_right + 120
    width = boundary_right + (260 if diagram['externals'] else 30)
    actors = []
    ordered = list(ucs.values())
    for index, (name, _) in enumerate(diagram['actors']):
        links = diagram['actor_links'][index]
        targets = [ucs[k] for k in links] if links else [u for u in ordered if u['col'] == 0]
        cy = sum(u['cy'] for u in targets) / len(targets)
        actors.append(dict(name=name, x=80, y=cy, targets=targets))
    externals = []
    for name, targets, row in diagram['externals']:
        externals.append(dict(name=name, x=externals_x, y=TOP + 40 + row * ROW_H, targets=[ucs[k] for k in targets]))
    return ucs, actors, externals, width, height, boundary_right


def svg_text(x, y, value, size=FONT, weight=400, anchor='middle', colour=INK, style=''):
    return (f'<text x="{x:g}" y="{y:g}" text-anchor="{anchor}" font-size="{size}" font-weight="{weight}" fill="{colour}"{style}>'
            f'{escape(value)}</text>')


def figure_actor(x, y, name):
    head = f'<circle cx="{x}" cy="{y - 30}" r="11" fill="{PAPER}" stroke="{ACTOR_COLOUR}" stroke-width="2"/>'
    body = (f'<path d="M{x} {y - 19}V{y + 10}M{x - 20} {y - 8}H{x + 20}M{x} {y + 10}L{x - 16} {y + 34}M{x} {y + 10}L{x + 16} {y + 34}" '
            f'fill="none" stroke="{ACTOR_COLOUR}" stroke-width="2" stroke-linecap="round"/>')
    return head + body + svg_text(x, y + 58, name, 15, 700)


def write_html(diagram, ucs, actors, externals, width, height, boundary_right):
    parts = [f'<defs><marker id="open" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto">'
             f'<path d="M1 1L9 5L1 9" fill="none" stroke="{LINE}" stroke-width="1.6"/></marker></defs>']
    parts.append(f'<rect x="170" y="14" width="{boundary_right - 170}" height="{height - 28}" rx="6" fill="none" stroke="{LINE}" stroke-width="1.8"/>')
    parts.append(svg_text(186, 42, 'IELTSPath', 17, 700, 'start'))
    for actor in actors:
        for target in actor['targets']:
            x1, y1 = actor['x'] + 22, actor['y'] - 4
            x2, y2 = target['cx'] - RX, target['cy']  # leftmost point, so no line crosses a neighbouring oval
            parts.append(f'<line x1="{x1}" y1="{y1:g}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{LINE}" stroke-width="1.4"/>')
    for external in externals:
        for target in external['targets']:
            x1, y1 = external['x'] - 70, external['y']
            x2, y2 = target['cx'] + RX, target['cy']
            parts.append(f'<line x1="{x1}" y1="{y1:g}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{LINE}" stroke-width="1.4"/>')
    for relation in diagram['relations']:
        source, target, kind = relation[:3]
        a, b = ucs[source], ucs[target]
        x1, y1 = ellipse_edge(a['cx'], a['cy'], b['cx'], b['cy'])
        x2, y2 = ellipse_edge(b['cx'], b['cy'], a['cx'], a['cy'])
        parts.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{LINE}" stroke-width="1.4" '
                     f'stroke-dasharray="7 5" marker-end="url(#open)"/>')
    for relation in diagram['relations']:
        source, target, kind = relation[:3]
        a, b = ucs[source], ucs[target]
        mx, my = (a['cx'] + b['cx']) / 2, (a['cy'] + b['cy']) / 2
        if abs(a['cx'] - b['cx']) < 5:  # a vertical line: put the label beside it, clear of both ovals
            mx += 100
        text = f'«{kind}»'
        parts.append(f'<rect x="{mx - 36:.1f}" y="{my - 10:.1f}" width="72" height="20" fill="{PAPER}"/>')
        parts.append(svg_text(mx, my + 5, text, 13, 400, 'middle', LINE, ' font-style="italic"'))
    for uc in ucs.values():
        parts.append(f'<ellipse cx="{uc["cx"]}" cy="{uc["cy"]:g}" rx="{RX}" ry="{RY}" fill="{OVAL_FILL}" stroke="{OVAL_BORDER}" stroke-width="1.8"/>')
        lines = wrap(uc['label'])
        for number, line in enumerate(lines):
            offset = (number - (len(lines) - 1) / 2) * 17
            parts.append(svg_text(uc['cx'], uc['cy'] + 5 + offset, line, FONT, 600))
    for actor in actors:
        parts.append(figure_actor(actor['x'], actor['y'], actor['name']))
    for external in externals:
        x, y = external['x'], external['y']
        parts.append(f'<rect x="{x - 70}" y="{y - 24}" width="140" height="48" rx="3" fill="{EXT_FILL}" stroke="{EXT_BORDER}" stroke-width="1.6"/>')
        for number, line in enumerate(wrap(external['name'], 16)):
            parts.append(svg_text(x, y + 5 + (number - (len(wrap(external['name'], 16)) - 1) / 2) * 16, line, 14, 700))
    name = diagram['name']
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width:g} {height:g}" width="{width:g}" height="{height:g}" '
           f'role="img" aria-labelledby="{name}-title" font-family="Segoe UI, Arial, sans-serif">'
           f'<title id="{name}-title">{escape(diagram["title"])}</title><rect width="{width:g}" height="{height:g}" fill="{PAPER}"/>'
           + ''.join(parts) + '</svg>')
    html = (f'<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{escape(diagram["title"])}</title>'
            f'<style>body{{margin:0;background:{PAPER}}}svg{{display:block}}</style></head><body>{svg}</body></html>')
    Path(__file__).with_name(f'{name}.html').write_text(html, encoding='utf-8')


def write_drawio(diagram, ucs, actors, externals, width, height, boundary_right):
    cells = [f'<mxCell id="boundary" value="IELTSPath" style="rounded=0;html=1;fillColor=none;strokeColor={LINE};strokeWidth=2;'
             f'verticalAlign=top;align=left;spacingLeft=14;spacingTop=8;fontStyle=1;fontSize=17;fontColor={INK};" vertex="1" parent="1">'
             f'<mxGeometry x="170" y="14" width="{boundary_right - 170}" height="{height - 28}" as="geometry"/></mxCell>']
    for uc in ucs.values():
        cells.append(f'<mxCell id="{uc["key"]}" value="{escape(uc["label"])}" style="ellipse;whiteSpace=wrap;html=1;fillColor={OVAL_FILL};'
                     f'strokeColor={OVAL_BORDER};strokeWidth=1.8;fontStyle=1;fontSize={FONT};fontColor={INK};" vertex="1" parent="1">'
                     f'<mxGeometry x="{uc["cx"] - RX}" y="{uc["cy"] - RY:g}" width="{2 * RX}" height="{2 * RY}" as="geometry"/></mxCell>')
    for index, actor in enumerate(actors):
        actor_id = f'actor{index}'
        cells.append(f'<mxCell id="{actor_id}" value="{escape(actor["name"])}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;'
                     f'html=1;outlineConnect=0;strokeColor={ACTOR_COLOUR};strokeWidth=2;fontStyle=1;fontSize=15;fontColor={INK};" vertex="1" parent="1">'
                     f'<mxGeometry x="{actor["x"] - 20}" y="{actor["y"] - 40:g}" width="40" height="76" as="geometry"/></mxCell>')
        for number, target in enumerate(actor['targets']):
            cells.append(f'<mxCell id="link{index}-{number}" style="endArrow=none;html=1;strokeColor={LINE};strokeWidth=1.4;" edge="1" parent="1" '
                         f'source="{actor_id}" target="{target["key"]}"><mxGeometry relative="1" as="geometry"/></mxCell>'.replace('strokeWidth=1.4;"', 'strokeWidth=1.4;entryX=0;entryY=0.5;entryDx=0;entryDy=0;"'))
    for index, external in enumerate(externals):
        cells.append(f'<mxCell id="ext{index}" value="{escape(external["name"])}" style="rounded=0;html=1;whiteSpace=wrap;fillColor={EXT_FILL};'
                     f'strokeColor={EXT_BORDER};strokeWidth=1.6;fontStyle=1;fontSize=14;fontColor={INK};" vertex="1" parent="1">'
                     f'<mxGeometry x="{external["x"] - 70}" y="{external["y"] - 24:g}" width="140" height="48" as="geometry"/></mxCell>')
        for number, target in enumerate(external['targets']):
            cells.append(f'<mxCell id="extlink{index}-{number}" style="endArrow=none;html=1;strokeColor={LINE};strokeWidth=1.4;" edge="1" parent="1" '
                         f'source="ext{index}" target="{target["key"]}"><mxGeometry relative="1" as="geometry"/></mxCell>'.replace('strokeWidth=1.4;"', 'strokeWidth=1.4;entryX=1;entryY=0.5;entryDx=0;entryDy=0;"'))
    for index, relation in enumerate(diagram['relations']):
        source, target, kind = relation[:3]
        cells.append(f'<mxCell id="rel{index}" value="&lt;&lt;{kind}&gt;&gt;" style="endArrow=open;endFill=0;dashed=1;html=1;strokeColor={LINE};'
                     f'strokeWidth=1.4;fontStyle=2;fontSize=13;fontColor={LINE};labelBackgroundColor={PAPER};" edge="1" parent="1" '
                     f'source="{source}" target="{target}"><mxGeometry relative="1" as="geometry"/></mxCell>')
    xml = (f'<mxfile host="app.diagrams.net"><diagram id="{diagram["name"]}" name="{escape(diagram["title"])}">'
           f'<mxGraphModel grid="1" gridSize="10" guides="1" connect="1" arrows="1" page="1" pageWidth="{width:g}" pageHeight="{height:g}" '
           f'background="{PAPER}"><root><mxCell id="0"/><mxCell id="1" parent="0"/>' + ''.join(cells) + '</root></mxGraphModel></diagram></mxfile>')
    ElementTree.fromstring(xml)
    Path(__file__).with_name(f'{diagram["name"]}.drawio').write_text(xml, encoding='utf-8')


for diagram in DIAGRAMS:
    ucs, actors, externals, width, height, boundary_right = layout(diagram)
    write_html(diagram, ucs, actors, externals, width, height, boundary_right)
    write_drawio(diagram, ucs, actors, externals, width, height, boundary_right)
    print(diagram['name'], round(width), round(height), len(ucs), 'use cases')



