"""Build the screen flow diagrams of SRS section 5.1 as HTML (SVG) and draw.io files.

Run: python build_screen_flows.py   (render the PNG files with Edge afterwards)
A box is a screen, a dashed box is a window that opens over a screen, an arrow is a move from one screen to another.
Boxes sit on a grid (column, row); arrows leave and enter through the named side of a box.
"""
import math
from pathlib import Path
from xml.etree import ElementTree
from xml.sax.saxutils import escape

BOX_W, BOX_H, HGAP, VGAP, MARGIN = 190, 46, 120, 62, 30
INK, LINE, PAPER = '#1f2937', '#374151', '#ffffff'
KINDS = {
    'web': ('#dbeafe', '#3b6ea8', None),
    'mobile': ('#ede9fe', '#7c3aed', None),
    'window': ('#ffffff', '#6b7280', '6 4'),
    'area': ('#dcfce7', '#15803d', None),
}
NORMAL = {'l': (-1, 0), 'r': (1, 0), 't': (0, -1), 'b': (0, 1)}

FLOWS = [
    {
        'name': 'sf-01-public-and-account', 'title': 'Screen flow: public pages and account',
        'caption': 'Figure 5.1 — Screen flow: public pages and account',
        'nodes': {
            'home': ('Home', 1, 1, 'web'),
            'signin': ('Sign In', 2, 1, 'web'), 'register': ('Register', 1, 2, 'web'),
            'verify': ('Verify E-mail', 1, 3, 'web'), 'forgot': ('Forgot Password', 2, 0, 'web'),
            'reset': ('Reset Password', 3, 0, 'web'), 'directory': ('Examiner Directory', 0, 1, 'web'),
            'detail': ('Examiner Detail', 0, 2, 'web'), 'overview': ('Overview', 3, 1, 'web'),
            'stats': ('Detailed Statistics', 4, 0, 'web'), 'ach': ('Achievements', 4, 1, 'web'),
            'profile': ('Profile and Security', 4, 2, 'web'), 'prefs': ('Notification Preferences', 3, 2, 'web'),
            'vocab': ('Vocabulary', 1, 0, 'web'),
            'ca': ('Content Author Home', 3, 3, 'area'), 'ex': ('Examiner Dashboard', 3, 3.6, 'area'),
            'ad': ('Administrator Home', 3, 4.2, 'area'), 'sm': ('Key Overview (Sales Manager)', 3, 4.8, 'area'),
            'ss': ('Issue Key (Sales Staff)', 3, 5.4, 'area'),
        },
        'edges': [
            ('home', 'signin', 'r', 'l', 'sign in'), ('home', 'register', 'b', 't', 'sign up'),
            ('register', 'verify', 'b', 't', 'e-mail link'), ('verify', 'signin', 'r', 'b', ''),
            ('signin', 'forgot', 't', 'b', 'forgot'), ('forgot', 'reset', 'r', 'l', 'e-mail link'),
            ('reset', 'signin', 'b', 't', 'new password'), ('signin', 'overview', 'r', 'l', 'Learner'),
            ('signin', 'ca', 'b', 'l', ''), ('signin', 'ex', 'b', 'l', ''),
            ('signin', 'ad', 'b', 'l', ''), ('signin', 'sm', 'b', 'l', ''),
            ('signin', 'ss', 'b', 'l', ''), ('home', 'directory', 'l', 'r', ''),
            ('directory', 'detail', 'b', 't', ''),
            ('home', 'vocab', 't', 'b', ''), ('overview', 'stats', 'r', 'l', ''), ('overview', 'ach', 'r', 'l', ''),
            ('overview', 'profile', 'r', 'l', ''), ('overview', 'prefs', 'b', 't', ''),
        ],
        'anchors': {('overview', 'stats'): (0.2, 0.5), ('overview', 'ach'): (0.5, 0.5), ('overview', 'profile'): (0.8, 0.5),
                    ('signin', 'forgot'): (0.25, 0.25), ('reset', 'signin'): (0.5, 0.8), ('verify', 'signin'): (0.5, 0.2),
                    ('signin', 'ca'): (0.8, 0.5), ('signin', 'ex'): (0.8, 0.5), ('signin', 'ad'): (0.8, 0.5),
                    ('signin', 'sm'): (0.8, 0.5), ('signin', 'ss'): (0.8, 0.5)},
    },
    {
        'name': 'sf-02-learner-learning-path', 'title': 'Screen flow: learning path',
        'caption': 'Figure 5.2 — Screen flow: placement test and learning path',
        'nodes': {
            'courses': ('Course List', 0, 1, 'web'), 'placement': ('Placement Test', 0, 0, 'web'),
            'topics': ('Topic List', 1, 1, 'web'), 'tree': ('Knowledge Tree', 1, 0, 'web'),
            'topic': ('Topic', 2, 1, 'web'), 'lesson': ('Lesson', 3, 1, 'web'),
            'hearts': ('No Hearts window', 3, 0, 'window'), 'practice': ('Practice Set', 3, 2, 'web'),
            'ttest': ('Topic Test', 2, 2, 'web'), 'tres': ('Topic Test Result', 1, 2, 'web'),
            'review': ('Review', 3, 3, 'web'), 'probe': ('Prerequisite Check', 2, 3, 'web'),
            'basket': ('Review Basket', 4, 3, 'web'),
        },
        'edges': [
            ('courses', 'placement', 't', 'b', 'level test'), ('courses', 'topics', 'r', 'l', 'open course'),
            ('topics', 'tree', 't', 'b', ''), ('topics', 'topic', 'r', 'l', 'open topic'),
            ('topic', 'lesson', 'r', 'l', 'open lesson'), ('lesson', 'hearts', 't', 'b', 'no hearts'),
            ('lesson', 'practice', 'b', 't', 'practice'), ('practice', 'ttest', 'l', 'r', ''),
            ('ttest', 'tres', 'l', 'r', 'submit'), ('tres', 'topics', 't', 'b', 'next topic'),
            ('practice', 'review', 'b', 't', 'weak skill'), ('review', 'probe', 'l', 'r', 'fails twice'),
            ('basket', 'review', 'l', 'r', 'start review'),
        ],
        'anchors': {},
    },
    {
        'name': 'sf-03-practice-and-grading', 'title': 'Screen flow: practice tests and grading',
        'caption': 'Figure 5.3 — Screen flow: practice tests, Writing and Speaking grading',
        'nodes': {
            'catalog': ('Practice Test Catalog', 0, 2, 'web'), 'reading': ('Reading Practice', 1, 0, 'web'),
            'listening': ('Listening Practice', 1, 1, 'web'), 'rresult': ('Reading / Listening Result', 2, 0.5, 'web'),
            'rreview': ('Answer Review', 3, 0.5, 'web'), 'history': ('Reading History', 1, 2, 'web'),
            'wwork': ('Writing Workspace', 1, 3, 'web'), 'wresult': ('Writing Result', 2, 3, 'web'),
            'job': ('Grading Job', 3, 3, 'web'), 'ereviews': ('Examiner Reviews', 3, 4, 'web'),
            'swork': ('Speaking Workspace', 1, 5, 'web'), 'sreview': ('Speaking Review', 2, 5, 'web'),
            'sresult': ('Speaking Result', 3, 5, 'web'), 'subs': ('Submission History', 0, 5, 'web'),
        },
        'edges': [
            ('catalog', 'reading', 'r', 'l', ''), ('catalog', 'listening', 'r', 'l', ''), ('catalog', 'history', 'r', 'l', ''),
            ('catalog', 'wwork', 'r', 'l', ''), ('catalog', 'swork', 'r', 'l', ''),
            ('reading', 'rresult', 'r', 'l', 'submit'), ('listening', 'rresult', 'r', 'l', 'submit'),
            ('rresult', 'rreview', 'r', 'l', 'review'),
            ('wwork', 'wresult', 'r', 'l', 'submit'), ('wresult', 'job', 'r', 'l', 'waiting'),
            ('wresult', 'wwork', 'b', 'b', 'rewrite'), ('wresult', 'ereviews', 'b', 'l', 'ask examiner'),
            ('swork', 'sreview', 'r', 'l', 'finish'), ('sreview', 'sresult', 'r', 'l', 'submit'),
            ('subs', 'catalog', 't', 'b', ''),
        ],
        'anchors': {('catalog', 'reading'): (0.1, 0.5), ('catalog', 'listening'): (0.25, 0.5), ('catalog', 'history'): (0.5, 0.5),
                    ('catalog', 'wwork'): (0.75, 0.5), ('catalog', 'swork'): (0.9, 0.5),
                    ('reading', 'rresult'): (0.5, 0.25), ('listening', 'rresult'): (0.5, 0.75)},
    },
    {
        'name': 'sf-04-vocabulary-flashcards-video', 'title': 'Screen flow: vocabulary, flashcards and video',
        'caption': 'Figure 5.4 — Screen flow: vocabulary, flashcards and video dictation',
        'nodes': {
            'vocab': ('Vocabulary', 0, 1, 'web'), 'word': ('Word Detail', 1, 0, 'web'),
            'set': ('Vocabulary Set', 1, 1, 'web'), 'study': ('Set Study', 2, 1, 'web'),
            'map': ('Vocabulary Map', 1, 2, 'web'), 'notebook': ('Notebook', 0, 3, 'web'),
            'nreview': ('Notebook Review', 1, 3, 'web'), 'deck': ('Deck Detail', 1, 4, 'web'),
            'editor': ('Deck Editor', 2, 4, 'web'), 'learn': ('Deck Learn Mode', 2, 5, 'web'),
            'test': ('Deck Test Mode', 3, 5, 'web'), 'fstudy': ('Flashcard Study', 0, 5, 'web'),
            'dlib': ('Dictation Library', 0, 6.5, 'web'), 'dplay': ('Dictation Player', 1, 6.5, 'web'),
        },
        'edges': [
            ('vocab', 'word', 'r', 'l', ''), ('vocab', 'set', 'r', 'l', ''), ('set', 'study', 'r', 'l', 'study'),
            ('vocab', 'map', 'r', 'l', ''), ('vocab', 'notebook', 'b', 't', 'save word'),
            ('notebook', 'nreview', 'r', 'l', 'review'), ('notebook', 'deck', 'b', 't', ''),
            ('notebook', 'fstudy', 'b', 't', ''), ('deck', 'editor', 'r', 'l', 'edit'),
            ('deck', 'learn', 'b', 't', ''), ('learn', 'test', 'r', 'l', ''), ('dlib', 'dplay', 'r', 'l', 'open video'),
        ],
        'anchors': {('notebook', 'deck'): (0.7, 0.5), ('notebook', 'fstudy'): (0.3, 0.5), ('vocab', 'word'): (0.15, 0.5),
                    ('vocab', 'set'): (0.5, 0.5), ('vocab', 'map'): (0.85, 0.5)},
    },
    {
        'name': 'sf-05-games-and-community', 'title': 'Screen flow: games and community',
        'caption': 'Figure 5.5 — Screen flow: games and community',
        'nodes': {
            'catalog': ('Game Catalog', 0, 0, 'web'), 'wsetup': ('Wordly Setup', 1, 0, 'web'),
            'rsetup': ('Rocket Setup', 1, 1, 'web'), 'room': ('Game Room', 2, 0, 'web'),
            'wplay': ('Wordly Play', 3, 0, 'web'), 'rplay': ('Rocket Play', 3, 1, 'web'),
            'gresult': ('Game Result', 4, 0, 'web'), 'feed': ('Community Feed', 0, 2.2, 'web'),
            'post': ('Post Detail', 1, 2.2, 'web'), 'profile': ('Learner Profile', 2, 2.2, 'web'),
            'write': ('Write Post window', 1, 3.2, 'window'), 'reports': ('Community Reports (Administrator)', 0, 4.2, 'web'),
        },
        'edges': [
            ('catalog', 'wsetup', 'r', 'l', ''), ('catalog', 'rsetup', 'b', 'l', ''), ('wsetup', 'room', 'r', 'l', 'create'),
            ('rsetup', 'room', 'r', 'b', 'create'), ('room', 'wplay', 'r', 'l', 'start'), ('room', 'rplay', 'r', 'l', 'start'),
            ('wplay', 'gresult', 'r', 'l', 'finish'), ('rplay', 'gresult', 'r', 'b', 'finish'),
            ('feed', 'post', 'r', 'l', ''), ('post', 'profile', 'r', 'l', 'author'), ('feed', 'write', 'b', 'l', ''),
            ('feed', 'reports', 'b', 't', ''),
        ],
        'anchors': {('room', 'wplay'): (0.3, 0.5), ('room', 'rplay'): (0.8, 0.5), ('feed', 'write'): (0.75, 0.5),
                    ('feed', 'reports'): (0.25, 0.25)},
    },
    {
        'name': 'sf-06-mobile-app', 'title': 'Screen flow: mobile app',
        'caption': 'Figure 5.6 — Screen flow: mobile app',
        'box_h': 40, 'vgap': 28,
        'nodes': {
            'welcome': ('Welcome', 0, 0, 'mobile'), 'onboard': ('Onboarding', 1, 0, 'mobile'),
            'login': ('Sign In / Register', 2, 0, 'mobile'), 'forgot': ('Forgot / Reset Password', 3, 0, 'mobile'),
            'home': ('Home tab', 0, 1, 'mobile'), 'catalog': ('Practice Test Catalog', 1, 1, 'mobile'),
            'test': ('Reading / Listening Practice', 2, 1, 'mobile'), 'tresult': ('Reading / Listening Result', 3, 1, 'mobile'),
            'answers': ('Answer Review', 4, 1, 'mobile'),
            'writing': ('Writing Workspace', 2, 2, 'mobile'), 'wresult': ('Writing Result', 3, 2, 'mobile'),
            'sreview': ('Speaking Review', 2, 3, 'mobile'), 'sresult': ('Speaking Result', 3, 3, 'mobile'),
            'srecord': ('Speaking Workspace', 2, 4, 'mobile'), 'history': ('Submission History', 1, 4, 'mobile'),
            'cards': ('Flashcards tab', 0, 5, 'mobile'), 'notebook': ('Notebook', 1, 5, 'mobile'),
            'deck': ('Deck Detail', 2, 5, 'mobile'), 'study': ('Flashcard Study', 3, 5, 'mobile'),
            'games': ('Games tab', 0, 6, 'mobile'), 'room': ('Game Room', 1, 6, 'mobile'),
            'play': ('Wordly / Rocket Play', 2, 6, 'mobile'), 'gresult': ('Game Result', 3, 6, 'mobile'),
            'community': ('Community tab', 0, 7, 'mobile'), 'post': ('Post Detail', 1, 7, 'mobile'),
            'composer': ('Post Composer', 2, 7, 'mobile'), 'notifs': ('Notifications', 3, 7, 'mobile'),
            'account': ('Account / Profile tab', 0, 8, 'mobile'), 'edit': ('Edit Profile', 1, 8, 'mobile'),
            'password': ('Change Password', 2, 8, 'mobile'), 'devices': ('Login Devices', 3, 8, 'mobile'),
        },
        'edges': [
            ('welcome', 'onboard', 'r', 'l', ''), ('onboard', 'login', 'r', 'l', ''), ('login', 'forgot', 'r', 'l', 'forgot'),
            ('login', 'home', 'b', 't', 'signed in'), ('home', 'catalog', 'r', 'l', 'skill'),
            ('catalog', 'test', 'r', 'l', 'start'), ('test', 'tresult', 'r', 'l', 'submit'), ('tresult', 'answers', 'r', 'l', ''),
            ('catalog', 'writing', 'b', 'l', ''), ('writing', 'wresult', 'r', 'l', 'grade'),
            ('catalog', 'sreview', 'b', 'l', ''), ('sreview', 'sresult', 'r', 'l', 'send'), ('sreview', 'srecord', 'b', 't', 'record'),
            ('home', 'history', 'b', 'l', ''), ('cards', 'notebook', 'r', 'l', ''), ('notebook', 'deck', 'r', 'l', ''),
            ('deck', 'study', 'r', 'l', 'study'), ('games', 'room', 'r', 'l', ''), ('room', 'play', 'r', 'l', 'start'),
            ('play', 'gresult', 'r', 'l', ''), ('community', 'post', 'r', 'l', ''), ('post', 'composer', 'r', 'l', 'write'),
            ('account', 'edit', 'r', 'l', ''), ('edit', 'password', 'r', 'l', ''), ('password', 'devices', 'r', 'l', ''),
        ],
        'anchors': {('catalog', 'writing'): (0.55, 0.5), ('catalog', 'sreview'): (0.4, 0.5), ('home', 'history'): (0.5, 0.5)},
    },
    {
        'name': 'sf-07-content-author', 'title': 'Screen flow: Content Author',
        'caption': 'Figure 5.7 — Screen flow: Content Author',
        'box_h': 40, 'vgap': 24,
        'nodes': {
            'home': ('Content Author Home', 0, 0, 'area'),
            'courses': ('Courses', 1, 0, 'web'), 'questions': ('Question Bank', 1, 1, 'web'),
            'packages': ('Test Packages', 1, 2, 'web'), 'placement': ('Placement Tests', 1, 3, 'web'),
            'library': ('Practice Library', 1, 4, 'web'), 'vocab': ('Vocabulary Content', 1, 5, 'web'),
            'rubrics': ('Grading Rubrics', 1, 6, 'web'), 'errors': ('Error Types', 1, 7, 'web'),
            'graph': ('Knowledge Graph', 1, 8, 'web'), 'code': ('Code Settings', 1, 9, 'web'),
            'import': ('Course Import', 1, 10, 'web'), 'drafts': ('AI Drafts', 1, 11, 'web'),
            'eval': ('AI Evaluation', 1, 12, 'web'), 'qa': ('Examiner Grade Check', 1, 13, 'web'),
            'work': ('Work Queue', 1, 14, 'web'),
            'qdetail': ('Question Detail', 2, 1, 'web'), 'editor': ('Package Editor', 2, 2, 'web'),
            'idraft': ('Import Draft', 2, 10, 'web'), 'ddetail': ('AI Draft Detail', 2, 11, 'web'),
            'run': ('Evaluation Run', 2, 12, 'web'), 'essay': ('Review Essay', 2, 13, 'web'),
        },
        'edges': [
            ('home', 'courses', 'r', 'l', ''), ('home', 'questions', 'r', 'l', ''), ('home', 'packages', 'r', 'l', ''),
            ('home', 'placement', 'r', 'l', ''), ('home', 'library', 'r', 'l', ''), ('home', 'vocab', 'r', 'l', ''),
            ('home', 'rubrics', 'r', 'l', ''), ('home', 'errors', 'r', 'l', ''), ('home', 'graph', 'r', 'l', ''),
            ('home', 'code', 'r', 'l', ''), ('home', 'import', 'r', 'l', ''), ('home', 'drafts', 'r', 'l', ''),
            ('home', 'eval', 'r', 'l', ''), ('home', 'qa', 'r', 'l', ''), ('home', 'work', 'r', 'l', ''),
            ('questions', 'qdetail', 'r', 'l', 'open'), ('packages', 'editor', 'r', 'l', 'open'),
            ('import', 'idraft', 'r', 'l', 'check'), ('drafts', 'ddetail', 'r', 'l', 'open'),
            ('eval', 'run', 'r', 'l', 'open'), ('qa', 'essay', 'r', 'l', 'open'),
        ],
        'anchors': {},
    },
    {
        'name': 'sf-08-examiner-and-admin', 'title': 'Screen flow: Examiner and Administrator',
        'caption': 'Figure 5.8 — Screen flow: Examiner and Administrator',
        'box_h': 40, 'vgap': 24,
        'nodes': {
            'edash': ('Examiner Dashboard', 0, 0, 'area'),
            'queue': ('Writing Queue', 1, 0, 'web'), 'sgrade': ('Speaking Grading', 1, 1, 'web'),
            'eprofile': ('Examiner Profile', 1, 2, 'web'), 'egraph': ('Knowledge Graph', 1, 3, 'web'),
            'wgrade': ('Writing Grading', 2, 0, 'web'), 'greview': ('Graded Review', 3, 0, 'web'),
            'ahome': ('Administrator Home', 0, 5, 'area'),
            'users': ('Users', 1, 5, 'web'), 'system': ('System Overview', 1, 6, 'web'),
            'services': ('Service Status', 1, 7, 'web'), 'activity': ('Activity Log', 1, 8, 'web'),
            'reports': ('Community Reports', 1, 9, 'web'),
            'umenu': ('User actions window', 2, 5, 'window'), 'history': ('Login History window', 3, 5, 'window'),
        },
        'edges': [
            ('edash', 'queue', 'r', 'l', ''), ('edash', 'sgrade', 'r', 'l', ''), ('edash', 'eprofile', 'r', 'l', ''),
            ('edash', 'egraph', 'r', 'l', ''), ('queue', 'wgrade', 'r', 'l', 'open'), ('wgrade', 'greview', 'r', 'l', 'done'),
            ('ahome', 'users', 'r', 'l', ''), ('ahome', 'system', 'r', 'l', ''), ('ahome', 'services', 'r', 'l', ''),
            ('ahome', 'activity', 'r', 'l', ''), ('ahome', 'reports', 'r', 'l', ''),
            ('users', 'umenu', 'r', 'l', 'row menu'), ('umenu', 'history', 'r', 'l', ''),
        ],
        'anchors': {},
    },
    {
        'name': 'sf-09-key-sales', 'title': 'Screen flow: key sales',
        'caption': 'Figure 5.9 — Screen flow: Sales Manager and Sales Staff',
        'nodes': {
            'overview': ('Key Overview', 0, 0, 'area'), 'batches': ('Key Batches', 1, 0, 'web'),
            'create': ('Create Batch', 2, 0, 'web'), 'created': ('Batch Created', 3, 0, 'web'),
            'detail': ('Batch Detail', 2, 1, 'web'), 'direct': ('Issue Key Directly', 1, 2, 'web'),
            'issue': ('Issue Key', 0, 4, 'area'), 'issued': ('Issued Keys', 1, 4, 'web'),
        },
        'edges': [
            ('overview', 'batches', 'r', 'l', ''), ('batches', 'create', 'r', 'l', 'new'), ('create', 'created', 'r', 'l', 'save'),
            ('batches', 'detail', 'b', 'l', 'open'), ('created', 'detail', 'b', 'r', ''),
            ('overview', 'direct', 'b', 'l', ''), ('issue', 'issued', 'r', 'l', ''),
        ],
        'anchors': {},
    },
]


def layout(flow):
    boxes = {}
    box_h, vgap = flow.get('box_h', BOX_H), flow.get('vgap', VGAP)
    for key, spec in flow['nodes'].items():
        label, col, row, kind = spec[:4]
        span = spec[4] if len(spec) > 4 else 1
        boxes[key] = dict(key=key, label=label, kind=kind, x=MARGIN + col * (BOX_W + HGAP), y=MARGIN + row * (box_h + vgap),
                          w=BOX_W, h=box_h * span + vgap * (span - 1), span=span)
    width = math.ceil(max(b['x'] + b['w'] for b in boxes.values()) + MARGIN)
    height = math.ceil(max(b['y'] + b['h'] for b in boxes.values()) + MARGIN)
    return boxes, width, height


def anchor(box, side, fraction):
    x, y, w, h = box['x'], box['y'], box['w'], box['h']
    return {'l': (x, y + h * fraction), 'r': (x + w, y + h * fraction), 't': (x + w * fraction, y), 'b': (x + w * fraction, y + h)}[side]


def route(boxes, edge, anchors):
    source, target, s_side, t_side = edge[:4]
    a, b = boxes[source], boxes[target]
    fa, fb = anchors.get((source, target), (0.5, 0.5))
    if a['span'] > 1 and s_side == 'r' and t_side == 'l':  # a tall hub box: leave level with the target
        fa = min(0.97, max(0.03, (b['y'] + b['h'] / 2 - a['y']) / a['h']))
    p, q = anchor(a, s_side, fa), anchor(b, t_side, fb)
    if s_side in 'lr' and t_side in 'lr':
        mid = round((p[0] + q[0]) / 2, 1)
        points = [p, q] if abs(p[1] - q[1]) < 1 else [p, (mid, p[1]), (mid, q[1]), q]
    elif s_side in 'tb' and t_side in 'tb':
        if s_side == t_side:  # a loop back on the same side
            extra = 30 if s_side == 'b' else -30
            level = max(p[1], q[1]) + 30 if s_side == 'b' else min(p[1], q[1]) - 30
            points = [p, (p[0], level), (q[0], level), q]
        else:
            mid = round((p[1] + q[1]) / 2, 1)
            points = [p, q] if abs(p[0] - q[0]) < 1 else [p, (p[0], mid), (q[0], mid), q]
    elif s_side in 'lr':
        points = [p, (q[0], p[1]), q]
    else:
        points = [p, (p[0], q[1]), q]
    return points, fa, fb


def label_spot(points):
    longest = max(range(len(points) - 1), key=lambda i: abs(points[i][0] - points[i + 1][0]) + abs(points[i][1] - points[i + 1][1]))
    (x0, y0), (x1, y1) = points[longest], points[longest + 1]
    return (x0 + x1) / 2, (y0 + y1) / 2


def svg_text(x, y, value, size=14, weight=400, colour=INK, style='', anchor_='middle'):
    return (f'<text x="{x:g}" y="{y:g}" text-anchor="{anchor_}" font-size="{size}" font-weight="{weight}" fill="{colour}"{style}>'
            f'{escape(value)}</text>')


def wrap(label, limit=24):
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


def write_html(flow, boxes, width, height):
    name, parts = flow['name'], []
    parts.append(f'<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto">'
                 f'<path d="M0 0L10 5L0 10z" fill="{LINE}"/></marker></defs>')
    labels = []
    for edge in flow['edges']:
        points, _, _ = route(boxes, edge, flow['anchors'])
        parts.append('<polyline points="' + ' '.join(f'{x:g},{y:g}' for x, y in points) +
                     f'" fill="none" stroke="{LINE}" stroke-width="1.6" stroke-linejoin="round" marker-end="url(#arrow)"/>')
        if edge[4]:
            labels.append((edge[4], *label_spot(points)))
    for text, x, y in labels:
        w = len(text) * 6.4 + 10
        parts.append(f'<rect x="{x - w / 2:.1f}" y="{y - 10:.1f}" width="{w:.1f}" height="19" fill="{PAPER}"/>')
        parts.append(svg_text(x, y + 4, text, 12.5, 400, LINE, ' font-style="italic"'))
    for box in boxes.values():
        fill, border, dash = KINDS[box['kind']]
        dash_attr = f' stroke-dasharray="{dash}"' if dash else ''
        parts.append(f'<rect x="{box["x"]:g}" y="{box["y"]:g}" width="{box["w"]}" height="{box["h"]}" rx="8" fill="{fill}" '
                     f'stroke="{border}" stroke-width="1.8"{dash_attr}/>')
        lines = wrap(box['label'])
        centre = box['y'] + (BOX_H / 2 if box['span'] > 1 else box['h'] / 2)
        for number, line in enumerate(lines):
            parts.append(svg_text(box['x'] + box['w'] / 2, centre + 5 + (number - (len(lines) - 1) / 2) * 16, line, 14, 700))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width:g} {height:g}" width="{width:g}" height="{height:g}" '
           f'role="img" aria-labelledby="{name}-title" font-family="Segoe UI, Arial, sans-serif"><title id="{name}-title">{escape(flow["title"])}</title>'
           f'<rect width="{width:g}" height="{height:g}" fill="{PAPER}"/>' + ''.join(parts) + '</svg>')
    html = (f'<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{escape(flow["title"])}</title>'
            f'<style>body{{margin:0;background:{PAPER}}}svg{{display:block}}</style></head><body>{svg}</body></html>')
    Path(__file__).with_name(f'{name}.html').write_text(html, encoding='utf-8')


def write_drawio(flow, boxes, width, height):
    cells = []
    for box in boxes.values():
        fill, border, dash = KINDS[box['kind']]
        dash_style = 'dashed=1;' if dash else ''
        cells.append(f'<mxCell id="{box["key"]}" value="{escape(box["label"])}" style="rounded=1;arcSize=18;whiteSpace=wrap;html=1;'
                     f'fillColor={fill};strokeColor={border};strokeWidth=1.8;fontStyle=1;fontSize=14;fontColor={INK};{dash_style}" vertex="1" parent="1">'
                     f'<mxGeometry x="{box["x"]:g}" y="{box["y"]:g}" width="{box["w"]}" height="{box["h"]}" as="geometry"/></mxCell>')
    for index, edge in enumerate(flow['edges']):
        points, fa, fb = route(boxes, edge, flow['anchors'])
        exits = {'l': (0, fa), 'r': (1, fa), 't': (fa, 0), 'b': (fa, 1)}[edge[2]]
        enters = {'l': (0, fb), 'r': (1, fb), 't': (fb, 0), 'b': (fb, 1)}[edge[3]]
        style = (f'edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;strokeColor={LINE};strokeWidth=1.6;'
                 f'fontSize=12;fontStyle=2;fontColor={LINE};labelBackgroundColor={PAPER};exitX={exits[0]:.4f};exitY={exits[1]:.4f};'
                 f'exitDx=0;exitDy=0;exitPerimeter=0;entryX={enters[0]:.4f};entryY={enters[1]:.4f};entryDx=0;entryDy=0;entryPerimeter=0;')
        via = ''.join(f'<mxPoint x="{x:g}" y="{y:g}"/>' for x, y in points[1:-1])
        geometry = '<mxGeometry relative="1" as="geometry">' + (f'<Array as="points">{via}</Array>' if via else '') + '</mxGeometry>'
        cells.append(f'<mxCell id="edge{index}" value="{escape(edge[4])}" style="{style}" edge="1" parent="1" '
                     f'source="{edge[0]}" target="{edge[1]}">{geometry}</mxCell>')
    xml = (f'<mxfile host="app.diagrams.net"><diagram id="{flow["name"]}" name="{escape(flow["title"])}">'
           f'<mxGraphModel grid="1" gridSize="10" guides="1" connect="1" arrows="1" page="1" pageWidth="{width:g}" pageHeight="{height:g}" '
           f'background="{PAPER}"><root><mxCell id="0"/><mxCell id="1" parent="0"/>' + ''.join(cells) + '</root></mxGraphModel></diagram></mxfile>')
    ElementTree.fromstring(xml)
    Path(__file__).with_name(f'{flow["name"]}.drawio').write_text(xml, encoding='utf-8')


for flow in FLOWS:
    boxes, width, height = layout(flow)
    for edge in flow['edges']:
        for key in edge[:2]:
            if key not in boxes:
                raise SystemExit(f'{flow["name"]}: unknown box {key}')
    write_html(flow, boxes, width, height)
    write_drawio(flow, boxes, width, height)
    print(flow['name'], round(width), round(height), len(boxes), 'screens')



