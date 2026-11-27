"""Builds the business-flow swimlane diagrams as HTML (inline SVG) and as editable draw.io files.

Run: python build_flow_diagrams.py   (writes <name>.html and <name>.drawio next to this script, for every flow)
Running the script again overwrites the draw.io files, so after editing one by hand in draw.io, export the
image from draw.io instead.

A flow is a grid of lanes (columns) with steps placed by lane and height; every connector is a list of points.
"""
from html import escape
from pathlib import Path
from xml.etree import ElementTree

INK, PAPER, LINE = '#1f2937', '#ffffff', '#1f4e79'
STYLES = {
    'step': ('#dbeafe', '#3b6ea8'),
    'system': ('#dcfce7', '#15803d'),
    'external': ('#f3f4f6', '#6b7280'),
    'decision': ('#fef3c7', '#8a6100'),
    'start': ('#1f2937', '#1f2937'),
    'end': ('#1f2937', '#1f2937'),
    'stop': ('#fee2e2', '#b91c1c'),
}
HEADER_TOP, HEADER_H = 8, 44
FONT = 15


def node(key, kind, x, y, w, h, *lines):
    """x, y is the centre of the shape."""
    return {'key': key, 'kind': kind, 'x': x, 'y': y, 'w': w, 'h': h, 'lines': list(lines)}


def side(shape, which):
    x, y, w, h = shape['x'], shape['y'], shape['w'], shape['h']
    return {'top': (x, y - h / 2), 'bottom': (x, y + h / 2), 'left': (x - w / 2, y), 'right': (x + w / 2, y)}[which]


def linker(nodes):
    shapes = {item['key']: item for item in nodes}

    def link(source, source_side, target, target_side, *via, label=None):
        points = [side(shapes[source], source_side), *via, side(shapes[target], target_side)]
        return {'source': source, 'target': target, 'points': points, 'label': label}

    return link


def placement_test():
    learner, system, ai = 158, 498, 788
    nodes = [
        node('start', 'start', learner, 80, 26, 26),
        node('open', 'step', learner, 140, 260, 48, 'Open the placement test'),
        node('survey', 'step', learner, 225, 260, 62, 'Answer a short survey', 'about the study goal'),
        node('goal', 'system', system, 225, 300, 62, 'Save the study goal and', 'show the test levels'),
        node('level', 'step', learner, 330, 260, 48, 'Choose a test level'),
        node('can', 'decision', system, 330, 142, 92, 'Can the', 'learner start?'),
        node('why', 'stop', 639, 330, 82, 62, 'Explain', 'why'),
        node('give', 'system', system, 440, 300, 62, 'Give the learner a test'),
        node('take', 'step', learner, 540, 260, 62, 'Do Listening,', 'Reading and Writing'),
        node('speak', 'step', learner, 640, 260, 62, 'Record Speaking answers', '(optional) and submit'),
        node('auto', 'system', system, 640, 300, 62, 'Grade Listening and Reading'),
        node('grade', 'external', ai, 640, 176, 80, 'Grade Writing', 'and the recorded', 'Speaking'),
        node('band', 'system', system, 755, 300, 62, 'Calculate the estimated band'),
        node('course', 'system', system, 855, 300, 62, 'Recommend a suitable course'),
        node('view', 'step', learner, 855, 260, 62, 'View the result and', 'choose a course'),
        node('end', 'end', learner, 945, 26, 26),
    ]
    link = linker(nodes)
    edges = [
        link('start', 'bottom', 'open', 'top'),
        link('open', 'bottom', 'survey', 'top'),
        link('survey', 'right', 'goal', 'left'),
        link('goal', 'bottom', 'level', 'top', (system, 270), (learner, 270)),
        link('level', 'right', 'can', 'left'),
        link('can', 'right', 'why', 'left', label=('No', 580, 321)),
        link('can', 'bottom', 'give', 'top', label=('Yes', 520, 394)),
        link('give', 'left', 'take', 'top', (learner, 440)),
        link('take', 'bottom', 'speak', 'top'),
        link('speak', 'right', 'auto', 'left'),
        link('auto', 'right', 'grade', 'left'),
        link('grade', 'bottom', 'band', 'right', (ai, 755)),
        link('band', 'bottom', 'course', 'top'),
        link('course', 'left', 'view', 'right'),
        link('view', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-01-placement-test', 'title': 'BF-01 Placement test',
        'description': 'How a learner takes the optional placement test and receives a recommended course.',
        'width': 896, 'height': 980,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 688), ('AI Grading', 688, 888)],
        'nodes': nodes, 'edges': edges,
    }


def study_and_practice():
    learner, system, side_x = 170, 540, 742
    nodes = [
        node('start', 'start', learner, 80, 26, 26),
        node('choose', 'step', learner, 140, 270, 48, 'Choose a lesson'),
        node('open', 'decision', system, 140, 172, 92, 'Is the', 'lesson open?'),
        node('first', 'stop', side_x, 140, 166, 62, 'Show what to', 'do first'),
        node('study', 'step', learner, 255, 270, 62, 'Study the lesson and', 'do the exercises'),
        node('passed', 'decision', system, 255, 220, 92, 'Scored 70%', 'or more?'),
        node('hint', 'system', system, 355, 300, 62, 'Show hints so the learner', 'can try again'),
        node('finish', 'system', system, 455, 300, 62, 'Finish the lesson', 'and open the practice'),
        node('practise', 'step', learner, 455, 270, 48, 'Do the practice'),
        node('score', 'system', system, 555, 300, 62, 'Show the score and', 'the correct answers'),
        node('weak', 'decision', system, 665, 196, 92, 'Weak skill', 'to review?'),
        node('review', 'system', side_x, 665, 150, 62, 'Review it', '(BF-02b)'),
        node('more', 'decision', system, 775, 240, 92, 'More lessons in', 'this topic?'),
        node('test', 'step', learner, 885, 270, 48, 'Take the topic test'),
        node('next', 'system', system, 885, 300, 62, 'Show the result and', 'open the next topic'),
        node('end', 'end', system, 985, 26, 26),
    ]
    link = linker(nodes)
    edges = [
        link('start', 'bottom', 'choose', 'top'),
        link('choose', 'right', 'open', 'left'),
        link('open', 'right', 'first', 'left', label=('No', 640, 131)),
        link('open', 'bottom', 'study', 'top', (system, 206), (learner, 206), label=('Yes', 562, 201)),
        link('study', 'right', 'passed', 'left'),
        link('passed', 'bottom', 'hint', 'top', label=('No', 562, 317)),
        link('hint', 'left', 'study', 'bottom', (learner, 355)),
        link('passed', 'right', 'finish', 'right', (775, 255), (775, 455), label=('Yes', 700, 247)),
        link('finish', 'left', 'practise', 'right'),
        link('practise', 'bottom', 'score', 'left', (learner, 555)),
        link('score', 'bottom', 'weak', 'top'),
        link('weak', 'right', 'review', 'left', label=('Yes', 650, 656)),
        link('weak', 'bottom', 'more', 'top', label=('No', 562, 727)),
        link('review', 'bottom', 'more', 'right', (side_x, 775)),
        link('more', 'left', 'choose', 'left', (20, 775), (20, 140), label=('Yes', 394, 766)),
        link('more', 'bottom', 'test', 'top', (system, 835), (learner, 835), label=('No', 562, 830)),
        link('test', 'right', 'next', 'left'),
        link('next', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-02a-study-and-practice', 'title': 'BF-02a Study and practice',
        'description': 'How a learner studies a lesson, practises it and finishes a topic.',
        'width': 840, 'height': 1016,
        'lanes': [('Learner', 8, 332), ('IELTSPath', 332, 832)],
        'nodes': nodes, 'edges': edges,
    }


def review_weak_points():
    learner, system, side_x, ring = 170, 540, 755, 855
    nodes = [
        node('start', 'start', system, 80, 26, 26),
        node('ask', 'system', system, 145, 300, 62, 'Ask the learner to review', 'a weak skill'),
        node('new', 'decision', system, 255, 220, 92, 'New questions', 'available?'),
        node('skip', 'system', side_x, 255, 140, 62, 'Skip the', 'review'),
        node('skipped', 'end', ring, 255, 26, 26),
        node('set', 'step', learner, 365, 270, 62, 'Do the review', 'questions'),
        node('passed', 'decision', system, 365, 220, 92, 'Scored 70%', 'or more?'),
        node('close', 'system', side_x, 365, 140, 62, 'Mark the', 'review done'),
        node('closed', 'end', ring, 365, 26, 26),
        node('many', 'decision', system, 485, 220, 92, 'Tried too', 'many times?'),
        node('theory', 'step', learner, 485, 270, 62, 'Read the theory', 'again'),
        node('optional', 'system', system, 600, 340, 62, 'Let the learner continue;', 'the review becomes optional'),
        node('end', 'end', system, 690, 26, 26),
    ]
    link = linker(nodes)
    edges = [
        link('start', 'bottom', 'ask', 'top'),
        link('ask', 'bottom', 'new', 'top'),
        link('new', 'right', 'skip', 'left', label=('No', 668, 246)),
        link('skip', 'right', 'skipped', 'left'),
        link('new', 'bottom', 'set', 'top', (system, 315), (learner, 315), label=('Yes', 562, 310)),
        link('set', 'right', 'passed', 'left'),
        link('passed', 'right', 'close', 'left', label=('Yes', 668, 356)),
        link('close', 'right', 'closed', 'left'),
        link('passed', 'bottom', 'many', 'top', label=('No', 562, 430)),
        link('many', 'left', 'theory', 'right', label=('No', 372, 476)),
        link('many', 'bottom', 'optional', 'top', label=('Yes', 565, 553)),
        link('theory', 'left', 'new', 'left', (20, 485), (20, 255)),
        link('optional', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-02b-review', 'title': 'BF-02b Review of weak skills',
        'description': 'How a learner reviews a weak skill until it is passed, skipped or made optional.',
        'width': 912, 'height': 722,
        'lanes': [('Learner', 8, 332), ('IELTSPath', 332, 904)],
        'nodes': nodes, 'edges': edges,
    }


def auto_linker(nodes):
    shapes = {item['key']: item for item in nodes}

    def point(shape, which):
        return which if isinstance(which, tuple) else side(shape, which)

    def link(source, source_side, target, target_side, *via, label=None):
        points = [point(shapes[source], source_side), *via, point(shapes[target], target_side)]
        placed = None
        if label:
            (x0, y0), (x1, y1) = points[0], points[1]
            if y0 == y1:
                placed = (label, x0 + (24 if x1 > x0 else -24), y0 - 10)
            else:
                placed = (label, x0 + 18, y0 + (20 if y1 > y0 else -8))
        return {'source': source, 'target': target, 'points': points, 'label': placed}

    return link


def placement_start():
    learner, main, side_x = 158, 450, 720
    nodes = [
        node('start', 'start', learner, 70, 26, 26),
        node('survey', 'step', learner, 150, 250, 62, 'Open the test and answer', 'a short survey'),
        node('goal', 'system', main, 150, 280, 62, 'Save the study goal and', 'show the test levels'),
        node('level', 'step', learner, 250, 250, 48, 'Choose a test level'),
        node('d1', 'decision', main, 345, 210, 84, 'Any tries left', 'today?'),
        node('s1', 'stop', side_x, 345, 240, 62, 'Say when the next try', 'is possible'),
        node('d2', 'decision', main, 450, 210, 84, 'Is a test ready', 'at this level?'),
        node('s2', 'stop', side_x, 450, 240, 62, 'Ask the learner to choose', 'another level'),
        node('give', 'system', main, 555, 280, 48, 'Give the learner a test'),
        node('take', 'step', learner, 555, 250, 62, 'Do Listening, Reading', 'and Writing'),
        node('d3', 'decision', learner, 665, 210, 84, 'Leaves before', 'the end?'),
        node('save', 'system', main, 665, 290, 76, 'Save the answers to continue', 'later; a cancelled test', 'does not use up a try'),
        node('d4', 'decision', learner, 775, 210, 84, 'Can the learner', 'record Speaking?'),
        node('skip', 'system', main, 775, 290, 48, 'Skip the Speaking part'),
        node('rec', 'step', learner, 870, 250, 48, 'Record the Speaking answers'),
        node('submit', 'step', learner, 955, 250, 48, 'Submit the test'),
        node('go', 'system', main, 955, 290, 48, 'Go on to grading (BF-01b)'),
        node('end', 'end', main, 1030, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'survey', 'top'),
        link('survey', 'right', 'goal', 'left'),
        link('goal', 'bottom', 'level', 'top', (main, 215), (learner, 215)),
        link('level', 'bottom', 'd1', 'left', (learner, 345)),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'd2', 'top', label='Yes'),
        link('d2', 'right', 's2', 'left', label='No'),
        link('s2', 'right', 'level', 'right', (846, 450), (846, 250)),
        link('d2', 'bottom', 'give', 'top', label='Yes'),
        link('give', 'left', 'take', 'right'),
        link('take', 'bottom', 'd3', 'top'),
        link('d3', 'right', 'save', 'left', label='Yes'),
        link('d3', 'bottom', 'd4', 'top', label='No'),
        link('d4', 'right', 'skip', 'left', label='No'),
        link('d4', 'bottom', 'rec', 'top', label='Yes'),
        link('rec', 'bottom', 'submit', 'top'),
        link('skip', 'bottom', 'submit', 'top', (main, 912), (learner, 912)),
        link('submit', 'right', 'go', 'left'),
        link('go', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-01a-placement-start', 'title': 'BF-01a Placement test: start',
        'description': 'How a learner starts and takes the optional placement test.',
        'width': 858, 'height': 1070,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 850)],
        'nodes': nodes, 'edges': edges,
    }


def placement_result():
    learner, main, ai = 158, 450, 905
    nodes = [
        node('start', 'start', main, 60, 26, 26),
        node('auto', 'system', main, 140, 290, 48, 'Grade Listening and Reading'),
        node('d5', 'decision', main, 240, 210, 84, 'Writing essay', 'sent?'),
        node('grade', 'external', ai, 240, 190, 80, 'Grade Writing and', 'any recorded', 'Speaking'),
        node('wz', 'system', main, 350, 290, 48, 'Score the Writing part as 0'),
        node('d6', 'decision', ai, 350, 170, 84, 'Graded in', 'time?'),
        node('inc', 'stop', ai, 460, 190, 62, 'Keep the bands of', 'the graded parts'),
        node('band', 'system', main, 540, 290, 48, 'Calculate the estimated band'),
        node('course', 'system', main, 630, 290, 48, 'Recommend a suitable course'),
        node('view', 'step', learner, 630, 250, 62, 'View the result and', 'choose a course'),
        node('end', 'end', learner, 710, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'auto', 'top'),
        link('auto', 'bottom', 'd5', 'top'),
        link('d5', 'right', 'grade', 'left', label='Yes'),
        link('d5', 'bottom', 'wz', 'top', label='No'),
        link('grade', 'bottom', 'd6', 'top'),
        link('d6', 'left', 'band', 'right', (700, 350), (700, 540), label='Yes'),
        link('d6', 'bottom', 'inc', 'top', label='No'),
        link('inc', 'left', 'band', 'right', (700, 460), (700, 540)),
        link('wz', 'bottom', 'band', 'top'),
        link('band', 'bottom', 'course', 'top'),
        link('course', 'left', 'view', 'right'),
        link('view', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-01b-placement-result', 'title': 'BF-01b Placement test: result',
        'description': 'How the placement test is graded and a course is recommended.',
        'width': 1018, 'height': 760,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 800), ('AI Grading', 800, 1010)],
        'nodes': nodes, 'edges': edges,
    }


def study_lesson():
    learner, main, side_x = 158, 450, 720
    nodes = [
        node('start', 'start', learner, 70, 26, 26),
        node('choose', 'step', learner, 150, 250, 48, 'Choose a lesson'),
        node('d1', 'decision', main, 150, 210, 84, 'Can the lesson', 'be opened?'),
        node('no1', 'stop', side_x, 150, 250, 92, 'Tell the learner why:', 'an earlier lesson or practice', 'is unfinished, a review is', 'waiting, or it is not unlocked'),
        node('d2', 'decision', main, 260, 210, 84, 'Any hearts', 'left?'),
        node('no2', 'stop', side_x, 260, 250, 62, 'Wait for hearts to refill,', 'or buy more with points'),
        node('study', 'step', learner, 370, 250, 62, 'Study the lesson, do the', 'exercises and submit'),
        node('d3', 'decision', main, 470, 210, 84, 'Did the answers', 'arrive?'),
        node('resend', 'stop', side_x, 470, 250, 80, 'Send again: the same result', 'comes back and is never', 'counted twice'),
        node('d4', 'decision', main, 580, 210, 84, 'Scored 70%', 'or more?'),
        node('hint', 'step', learner, 580, 250, 62, 'Read the hints; each wrong', 'answer costs a heart'),
        node('finish', 'system', main, 690, 270, 62, 'Finish the lesson and', 'open the practice'),
        node('end', 'end', main, 770, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'choose', 'top'),
        link('choose', 'right', 'd1', 'left'),
        link('d1', 'right', 'no1', 'left', label='No'),
        link('d1', 'bottom', 'd2', 'top', label='Yes'),
        link('d2', 'right', 'no2', 'left', label='No'),
        link('d2', 'bottom', 'study', 'top', (main, 330), (learner, 330), label='Yes'),
        link('study', 'bottom', 'd3', 'left', (learner, 470)),
        link('d3', 'right', 'resend', 'left', label='No'),
        link('resend', 'top', 'study', 'right', (side_x, 370)),
        link('d3', 'bottom', 'd4', 'top', label='Yes'),
        link('d4', 'left', 'hint', 'right', label='No'),
        link('hint', 'left', 'study', 'left', (14, 580), (14, 370)),
        link('d4', 'bottom', 'finish', 'top', label='Yes'),
        link('finish', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-02a-study-lesson', 'title': 'BF-02a Study a lesson',
        'description': 'How a learner studies a lesson and what stops them.',
        'width': 858, 'height': 820,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 850)],
        'nodes': nodes, 'edges': edges,
    }


def practice_lesson():
    learner, main, side_x = 158, 450, 720
    nodes = [
        node('start', 'start', learner, 70, 26, 26),
        node('open', 'step', learner, 150, 250, 62, 'Open the practice', 'of the lesson'),
        node('d1', 'decision', main, 150, 210, 84, 'Is there a', 'practice set?'),
        node('s1', 'system', side_x, 150, 250, 62, 'Skip the practice and go', 'to the next lesson'),
        node('d2', 'decision', main, 260, 210, 84, 'Any hearts', 'left?'),
        node('s2', 'stop', side_x, 260, 250, 62, 'Wait for hearts to refill,', 'or buy more with points'),
        node('doit', 'step', learner, 370, 250, 62, 'Do the practice', 'and submit'),
        node('d3', 'decision', main, 470, 210, 84, 'Did the answers', 'arrive?'),
        node('resend', 'stop', side_x, 470, 250, 80, 'Send again: the same result', 'comes back and is never', 'counted twice'),
        node('d4', 'decision', main, 580, 210, 84, 'Scored 70%', 'or more?'),
        node('rev', 'system', side_x, 580, 250, 76, 'If a skill is weak, suggest', 'a review (BF-02d); it is', 'required if weak again'),
        node('cleared', 'system', main, 690, 250, 48, 'Practice is cleared'),
        node('end', 'end', main, 770, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'open', 'top'),
        link('open', 'right', 'd1', 'left'),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'd2', 'top', label='Yes'),
        link('d2', 'right', 's2', 'left', label='No'),
        link('d2', 'bottom', 'doit', 'top', (main, 330), (learner, 330), label='Yes'),
        link('doit', 'bottom', 'd3', 'left', (learner, 470)),
        link('d3', 'right', 'resend', 'left', label='No'),
        link('resend', 'top', 'doit', 'right', (side_x, 370)),
        link('d3', 'bottom', 'd4', 'top', label='Yes'),
        link('d4', 'right', 'rev', 'left', label='No'),
        link('d4', 'bottom', 'cleared', 'top', label='Yes'),
        link('rev', 'bottom', 'cleared', 'right', (side_x, 690)),
        link('cleared', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-02b-practice', 'title': 'BF-02b Practice a lesson',
        'description': 'How a learner practises a lesson and when a review is suggested.',
        'width': 858, 'height': 820,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 850)],
        'nodes': nodes, 'edges': edges,
    }


def topic_test():
    learner, main, side_x = 158, 450, 720
    nodes = [
        node('start', 'start', main, 70, 26, 26),
        node('d1', 'decision', main, 160, 210, 84, 'Does the topic', 'have a test?'),
        node('s1', 'system', side_x, 160, 250, 62, 'The topic is passed once', 'all its lessons are done'),
        node('d2', 'decision', main, 270, 210, 84, 'Anything left to', 'finish first?'),
        node('s2', 'stop', side_x, 270, 250, 62, 'Tell the learner to finish', 'lessons, practice or reviews'),
        node('d3', 'decision', main, 380, 210, 84, 'Is a test', 'available?'),
        node('s3', 'stop', side_x, 380, 250, 62, 'Tell the learner that no', 'test is available now'),
        node('take', 'step', learner, 490, 250, 62, 'Take the test', 'and submit'),
        node('d4', 'decision', main, 600, 210, 84, 'Did the answers', 'arrive?'),
        node('resend', 'stop', side_x, 600, 250, 80, 'Send again: the same result', 'comes back and is never', 'counted twice'),
        node('d5', 'decision', main, 710, 210, 84, 'Could every answer', 'be graded?'),
        node('s4', 'stop', side_x, 710, 250, 80, 'Wait for an examiner; the', 'learner is told when the', 'result is ready'),
        node('d6', 'decision', main, 820, 210, 84, 'Scored 70%', 'or more?'),
        node('passed', 'system', side_x, 820, 250, 48, 'Mark the topic as passed'),
        node('completed', 'system', main, 920, 250, 48, 'Mark the topic as completed'),
        node('next', 'system', main, 1010, 250, 62, 'Open the next topic'),
        node('end', 'end', main, 1090, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'd1', 'top'),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'd2', 'top', label='Yes'),
        link('d2', 'right', 's2', 'left', label='Yes'),
        link('d2', 'bottom', 'd3', 'top', label='No'),
        link('d3', 'right', 's3', 'left', label='No'),
        link('d3', 'bottom', 'take', 'top', (main, 440), (learner, 440), label='Yes'),
        link('take', 'bottom', 'd4', 'left', (learner, 600)),
        link('d4', 'right', 'resend', 'left', label='No'),
        link('resend', 'top', 'take', 'right', (side_x, 490)),
        link('d4', 'bottom', 'd5', 'top', label='Yes'),
        link('d5', 'right', 's4', 'left', label='No'),
        link('d5', 'bottom', 'd6', 'top', label='Yes'),
        link('d6', 'right', 'passed', 'left', label='Yes'),
        link('d6', 'bottom', 'completed', 'top', label='No'),
        link('completed', 'bottom', 'next', 'top'),
        link('passed', 'bottom', 'next', 'right', (side_x, 1010)),
        link('next', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-02c-topic-test', 'title': 'BF-02c Topic test',
        'description': 'How a learner takes the final test of a topic and moves on.',
        'width': 858, 'height': 1130,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 850)],
        'nodes': nodes, 'edges': edges,
    }


def review_skills():
    learner, main, side_x = 158, 450, 720
    nodes = [
        node('start', 'start', main, 70, 26, 26),
        node('ask', 'system', main, 150, 290, 62, 'Ask the learner to review', 'a weak skill'),
        node('d1', 'decision', main, 250, 210, 84, 'Could the review', 'be loaded?'),
        node('s1', 'stop', side_x, 250, 250, 62, 'Show an error; the learner', 'can try again later'),
        node('d2', 'decision', main, 360, 210, 84, 'New questions', 'available?'),
        node('s2', 'system', side_x, 360, 250, 48, 'Skip the review'),
        node('doq', 'step', learner, 470, 250, 62, 'Answer the review', 'questions'),
        node('d3', 'decision', main, 570, 210, 84, 'Scored 70%', 'or more?'),
        node('done', 'system', side_x, 570, 250, 48, 'Mark the review as done'),
        node('d4', 'decision', main, 680, 210, 84, 'Failed four', 'times?'),
        node('opt', 'system', side_x, 680, 250, 62, 'Let the learner continue;', 'the review becomes optional'),
        node('d5', 'decision', main, 790, 210, 84, 'Is this the second', 'failed try?'),
        node('diag', 'system', side_x, 790, 250, 76, 'Check the earlier knowledge', 'it builds on; it can be', 'stopped after one answer'),
        node('theory', 'step', learner, 790, 250, 62, 'Read the theory again', 'and answer a quick check'),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'ask', 'top'),
        link('ask', 'bottom', 'd1', 'top'),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'd2', 'top', label='Yes'),
        link('d2', 'right', 's2', 'left', label='No'),
        link('d2', 'bottom', 'doq', 'top', (main, 430), (learner, 430), label='Yes'),
        link('doq', 'bottom', 'd3', 'left', (learner, 570)),
        link('d3', 'right', 'done', 'left', label='Yes'),
        link('d3', 'bottom', 'd4', 'top', label='No'),
        link('d4', 'right', 'opt', 'left', label='Yes'),
        link('d4', 'bottom', 'd5', 'top', label='No'),
        link('d5', 'right', 'diag', 'left', label='Yes'),
        link('d5', 'left', 'theory', 'right', label='No'),
        link('theory', 'left', 'd2', 'left', (14, 790), (14, 360)),
        link('diag', 'bottom', 'd2', 'left', (side_x, 880), (14, 880), (14, 360)),
    ]
    return {
        'name': 'bf-02d-review', 'title': 'BF-02d Review of weak skills',
        'description': 'How a learner reviews a weak skill until it is done, skipped or made optional.',
        'width': 858, 'height': 930,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 850)],
        'nodes': nodes, 'edges': edges,
    }


def writing_grading():
    learner, main, side_x, ai = 158, 450, 695, 905
    nodes = [
        node('start', 'start', learner, 70, 26, 26),
        node('write', 'step', learner, 150, 250, 62, 'Write the essay', 'and submit it'),
        node('d1', 'decision', main, 150, 210, 84, 'Allowed to', 'submit?'),
        node('s1', 'stop', side_x, 150, 210, 92, 'Tell the learner why: not', 'enough points or the', 'daily limit is reached'),
        node('dc', 'decision', main, 260, 210, 84, 'Did the essay', 'arrive?'),
        node('resend', 'stop', side_x, 260, 210, 62, 'Ask the learner to', 'send it again'),
        node('grade', 'external', ai, 370, 190, 62, 'Grade the essay by', 'the IELTS criteria'),
        node('d2', 'decision', ai, 480, 170, 84, 'Graded', 'properly?'),
        node('show', 'system', main, 480, 290, 62, 'Take the points; show the', 'band and the feedback'),
        node('failed', 'stop', ai, 600, 190, 76, 'Take no points; ask the', 'learner to try later'),
        node('read', 'step', learner, 480, 250, 48, 'Read the feedback'),
        node('d3', 'decision', learner, 600, 210, 84, 'Rewrite the', 'essay?'),
        node('end', 'end', learner, 700, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'write', 'top'),
        link('write', 'right', 'd1', 'left'),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'dc', 'top', label='Yes'),
        link('dc', 'right', 'resend', 'left', label='No'),
        link('dc', 'bottom', 'grade', 'left', (main, 370), label='Yes'),
        link('grade', 'bottom', 'd2', 'top'),
        link('d2', 'left', 'show', 'right', label='Yes'),
        link('d2', 'bottom', 'failed', 'top', label='No'),
        link('show', 'left', 'read', 'right'),
        link('read', 'bottom', 'd3', 'top'),
        link('d3', 'left', 'write', 'left', (14, 600), (14, 150), label='Yes'),
        link('d3', 'bottom', 'end', 'top', label='No'),
    ]
    return {
        'name': 'bf-03-writing-grading', 'title': 'BF-03 Writing grading',
        'description': 'How a learner gets a Writing essay graded and may rewrite it.',
        'width': 1018, 'height': 750,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 800), ('AI Grading', 800, 1010)],
        'nodes': nodes, 'edges': edges,
    }


def speaking_grading():
    learner, main, side_x, ai = 158, 450, 690, 905
    nodes = [
        node('start', 'start', learner, 70, 26, 26),
        node('record', 'step', learner, 150, 250, 62, 'Agree to the terms and', 'record the answers'),
        node('d1', 'decision', main, 150, 210, 84, 'Allowed to', 'submit?'),
        node('s1', 'stop', side_x, 150, 200, 92, 'Tell the learner why:', 'grading is off or the', 'daily limit is reached'),
        node('dc', 'decision', main, 260, 210, 84, 'Did the recordings', 'arrive?'),
        node('resend', 'stop', side_x, 260, 200, 62, 'Ask the learner to', 'send them again'),
        node('accept', 'system', main, 370, 290, 62, 'Accept the recordings and', 'grade them in the background'),
        node('ai', 'external', ai, 370, 190, 62, 'Listen to the answers', 'and grade Speaking'),
        node('d2', 'decision', ai, 480, 170, 84, 'Speech', 'heard?'),
        node('nospeech', 'stop', side_x, 480, 200, 76, 'Tell the learner that', 'no speech was heard'),
        node('d3', 'decision', ai, 590, 170, 84, 'Graded', 'properly?'),
        node('exam', 'stop', side_x, 590, 200, 76, 'Take no points; pass', 'it to an examiner'),
        node('d4', 'decision', main, 700, 210, 84, 'Enough', 'points?'),
        node('hold', 'stop', learner, 700, 250, 76, 'Keep the grade until the', 'learner has enough points'),
        node('result', 'system', main, 810, 290, 62, 'Take the points; show the', 'band and the feedback'),
        node('read', 'step', learner, 810, 250, 48, 'Read the feedback'),
        node('end', 'end', learner, 890, 26, 26),
    ]
    link = auto_linker(nodes)
    edges = [
        link('start', 'bottom', 'record', 'top'),
        link('record', 'right', 'd1', 'left'),
        link('d1', 'right', 's1', 'left', label='No'),
        link('d1', 'bottom', 'dc', 'top', label='Yes'),
        link('dc', 'right', 'resend', 'left', label='No'),
        link('dc', 'bottom', 'accept', 'top', label='Yes'),
        link('accept', 'right', 'ai', 'left'),
        link('ai', 'bottom', 'd2', 'top'),
        link('d2', 'left', 'nospeech', 'right', label='No'),
        link('d2', 'bottom', 'd3', 'top', label='Yes'),
        link('d3', 'left', 'exam', 'right', label='No'),
        link('d3', 'bottom', 'd4', 'right', (ai, 700), label='Yes'),
        link('d4', 'left', 'hold', 'right', label='No'),
        link('d4', 'bottom', 'result', 'top', label='Yes'),
        link('result', 'left', 'read', 'right'),
        link('read', 'bottom', 'end', 'top'),
    ]
    return {
        'name': 'bf-04-speaking-grading', 'title': 'BF-04 Speaking grading',
        'description': 'How a learner gets recorded Speaking answers graded.',
        'width': 1018, 'height': 930,
        'lanes': [('Learner', 8, 308), ('IELTSPath', 308, 800), ('AI Grading', 800, 1010)],
        'nodes': nodes, 'edges': edges,
    }


FLOWS = [placement_start, placement_result, study_lesson, practice_lesson, topic_test, review_skills, writing_grading, speaking_grading]


def svg_text(x, y, value, size=FONT, colour=INK, weight=400):
    return (f'<text x="{x}" y="{y}" text-anchor="middle" font-size="{size}" font-weight="{weight}" '
            f'fill="{colour}">{escape(value)}</text>')


def write_html(flow):
    width, height, name = flow['width'], flow['height'], flow['name']
    parts = []
    for title, left, right in flow['lanes']:
        parts.append(f'<rect x="{left}" y="{HEADER_TOP}" width="{right - left}" height="{height - 2 * HEADER_TOP}" '
                     'fill="none" stroke="#9ca3af" stroke-width="1.2"/>')
        parts.append(f'<rect x="{left}" y="{HEADER_TOP}" width="{right - left}" height="{HEADER_H}" '
                     'fill="#e5e7eb" stroke="#9ca3af" stroke-width="1.2"/>')
        parts.append(svg_text((left + right) / 2, HEADER_TOP + 29, title, 17, INK, 700))
    for edge in flow['edges']:
        path = ' '.join(f'{x:g},{y:g}' for x, y in edge['points'])
        parts.append(f'<polyline points="{path}" fill="none" stroke="{LINE}" stroke-width="1.6" '
                     'marker-end="url(#arrow)"/>')
        if edge['label']:
            value, x, y = edge['label']
            parts.append(svg_text(x, y, value, 14, LINE, 700))
    for item in flow['nodes']:
        x, y, w, h, kind = item['x'], item['y'], item['w'], item['h'], item['kind']
        fill, stroke = STYLES[kind]
        paint = f'fill="{fill}" stroke="{stroke}" stroke-width="1.6"'
        if kind in ('start', 'end'):
            if kind == 'start':
                parts.append(f'<circle cx="{x}" cy="{y}" r="{w / 2:g}" {paint}/>')
            else:
                parts.append(f'<circle cx="{x}" cy="{y}" r="{w / 2:g}" fill="{PAPER}" stroke="{stroke}" stroke-width="2"/>')
                parts.append(f'<circle cx="{x}" cy="{y}" r="{w / 2 - 6:g}" {paint}/>')
        elif kind == 'decision':
            parts.append(f'<polygon points="{x},{y - h / 2:g} {x + w / 2:g},{y} {x},{y + h / 2:g} {x - w / 2:g},{y}" {paint}/>')
        else:
            parts.append(f'<rect x="{x - w / 2:g}" y="{y - h / 2:g}" width="{w}" height="{h}" rx="10" {paint}/>')
        first = y + 5 - 9.5 * (len(item['lines']) - 1)
        parts.extend(svg_text(x, f'{first + 19 * i:g}', line) for i, line in enumerate(item['lines']))
    marker = ('<marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" '
              f'orient="auto"><path d="M0 0L10 5L0 10z" fill="{LINE}"/></marker>')
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}" '
           f'role="img" aria-labelledby="{name}-title {name}-desc" font-family="Segoe UI, Arial, sans-serif">'
           f'<title id="{name}-title">{escape(flow["title"])}</title>'
           f'<desc id="{name}-desc">{escape(flow["description"])}</desc><defs>{marker}</defs>'
           f'<rect width="{width}" height="{height}" fill="{PAPER}"/>{"".join(parts)}</svg>')
    page = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            f'<title>{escape(flow["title"])}</title><style>body{{margin:0;background:{PAPER}}}'
            f'.wrap{{overflow-x:auto}}svg{{min-width:{width}px;display:block}}</style></head>'
            f'<body><div class="wrap">{svg}</div></body></html>')
    Path(__file__).with_name(f'{name}.html').write_text(page, encoding='utf-8')


def write_drawio(flow):
    cells = []

    def vertex(key, value, style, x, y, w, h):
        cells.append(f'<mxCell id="{key}" value="{escape(value)}" style="{style}" vertex="1" parent="1">'
                     f'<mxGeometry x="{x:g}" y="{y:g}" width="{w:g}" height="{h:g}" as="geometry"/></mxCell>')

    for index, (title, left, right) in enumerate(flow['lanes']):
        vertex(f'lane{index}', title,
               f'swimlane;html=1;startSize={HEADER_H};fillColor=#e5e7eb;strokeColor=#9ca3af;fontStyle=1;fontSize=17;'
               'collapsible=0;pointerEvents=0;', left, HEADER_TOP, right - left, flow['height'] - 2 * HEADER_TOP)
    shapes = {item['key']: item for item in flow['nodes']}
    for item in flow['nodes']:
        fill, stroke = STYLES[item['kind']]
        shape = {'decision': 'rhombus;', 'start': 'ellipse;aspect=fixed;', 'end': 'shape=endState;aspect=fixed;'}.get(
            item['kind'], 'rounded=1;arcSize=16;')
        if item['kind'] == 'end':
            fill = '#1f2937'
        vertex(item['key'], '<br>'.join(escape(line) for line in item['lines']),
               f'{shape}whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};strokeWidth=1.5;'
               f'fontSize={FONT};fontColor={INK};',
               item['x'] - item['w'] / 2, item['y'] - item['h'] / 2, item['w'], item['h'])

    def attach(shape, point):
        return (round((point[0] - (shape['x'] - shape['w'] / 2)) / shape['w'], 4),
                round((point[1] - (shape['y'] - shape['h'] / 2)) / shape['h'], 4))

    for index, edge in enumerate(flow['edges']):
        start = attach(shapes[edge['source']], edge['points'][0])
        end = attach(shapes[edge['target']], edge['points'][-1])
        style = (f'endArrow=block;endFill=1;html=1;rounded=0;strokeWidth=1.6;strokeColor={LINE};'
                 f'fontSize=14;fontStyle=1;fontColor={LINE};labelBackgroundColor={PAPER};'
                 f'exitX={start[0]};exitY={start[1]};exitDx=0;exitDy=0;exitPerimeter=0;'
                 f'entryX={end[0]};entryY={end[1]};entryDx=0;entryDy=0;entryPerimeter=0;')
        via = ''.join(f'<mxPoint x="{x:g}" y="{y:g}"/>' for x, y in edge['points'][1:-1])
        geometry = '<mxGeometry relative="1" as="geometry">' + (f'<Array as="points">{via}</Array>' if via else '')
        value = edge['label'][0] if edge['label'] else ''
        cells.append(f'<mxCell id="edge{index}" value="{value}" style="{style}" edge="1" parent="1" '
                     f'source="{edge["source"]}" target="{edge["target"]}">{geometry}</mxGeometry></mxCell>')
    xml = (f'<mxfile host="app.diagrams.net"><diagram id="{flow["name"]}" name="{escape(flow["title"])}">'
           f'<mxGraphModel grid="1" gridSize="10" guides="1" connect="1" arrows="1" page="1" '
           f'pageWidth="{flow["width"]}" pageHeight="{flow["height"]}" background="{PAPER}"><root>'
           '<mxCell id="0"/><mxCell id="1" parent="0"/>' + ''.join(cells) +
           '</root></mxGraphModel></diagram></mxfile>')
    ElementTree.fromstring(xml)  # fail here rather than in draw.io if the XML is malformed
    Path(__file__).with_name(f'{flow["name"]}.drawio').write_text(xml, encoding='utf-8')


for make in FLOWS:
    built = make()
    write_html(built)
    write_drawio(built)
    print(built['name'], built['width'], built['height'])
