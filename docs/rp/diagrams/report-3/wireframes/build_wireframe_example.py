"""Example wireframe for Part III: the Sign In screen of the web app, with numbered markers that match the field table.

Run: python build_wireframe_example.py   (then render the PNG with Edge, as for the other diagrams)
"""
from pathlib import Path
from xml.sax.saxutils import escape

W, H = 760, 470
INK, GREY, LIGHT, LINE, MARK = '#333333', '#8a8a8a', '#f2f2f2', '#b5b5b5', '#c0392b'
parts = []


def rect(x, y, w, h, fill='#ffffff', stroke=LINE, r=4, dash=''):
    d = f' stroke-dasharray="{dash}"' if dash else ''
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="1.2"{d}/>')


def text(x, y, value, size=13, weight=400, colour=INK, anchor='start'):
    parts.append(f'<text x="{x}" y="{y}" font-family="Segoe UI, Arial, sans-serif" font-size="{size}" font-weight="{weight}" '
                 f'fill="{colour}" text-anchor="{anchor}">{escape(value)}</text>')


def marker(x, y, number):
    parts.append(f'<circle cx="{x}" cy="{y}" r="10" fill="{MARK}"/>')
    text(x, y + 4.5, str(number), 12, 700, '#ffffff', 'middle')


# browser window
rect(10, 10, 740, 450, '#ffffff', '#888888', 8)
rect(10, 10, 740, 30, LIGHT, '#888888', 8)
text(28, 30, 'IELTSPath  -  Sign In', 12, 400, GREY)
# navigation bar
rect(10, 40, 740, 40, '#e9e9e9', LINE, 0)
text(28, 65, 'IELTSPath', 15, 700)
for i, label in enumerate(('Home', 'Courses', 'Practice', 'Vocabulary')):
    text(300 + i * 90, 65, label, 12, 400, GREY)
marker(722, 60, 1)
# left side: slogan
text(40, 170, 'IELTS SPACE', 12, 700, GREY)
text(40, 200, 'One step a day.', 20, 700)
text(40, 226, 'Closer to your target band.', 20, 700)
text(40, 252, 'Lessons, learning path and vocabulary', 12, 400, GREY)
text(40, 268, 'in one focused space.', 12, 400, GREY)
rect(60, 300, 120, 110, LIGHT, LINE, 6, '4 3')
text(120, 360, 'Owl mascot', 11, 400, GREY, 'middle')
# card
rect(400, 105, 330, 335, '#ffffff', '#888888', 8)
rect(400, 105, 165, 38, '#ffffff', LINE, 0)
rect(565, 105, 165, 38, LIGHT, LINE, 0)
text(482, 129, 'Sign In', 13, 700, INK, 'middle')
text(647, 129, 'Sign Up', 13, 400, GREY, 'middle')
marker(712, 124, 2)
text(565, 170, 'Sign in to start your journey!', 14, 700, INK, 'middle')
marker(715, 164, 3)
# e-mail field
text(425, 200, 'E-mail or phone number', 12, 600)
rect(425, 207, 280, 32, '#ffffff', LINE, 4)
text(437, 228, 'Enter your e-mail or phone number', 12, 400, '#b0b0b0')
marker(716, 190, 4)
# password
text(425, 262, 'Password', 12, 600)
rect(425, 269, 280, 32, '#ffffff', LINE, 4)
text(437, 290, '••••••••', 12, 400, '#b0b0b0')
text(688, 290, 'show', 11, 400, GREY, 'middle')
marker(716, 252, 5)
# forgot link
text(705, 320, 'Forgot password?', 11, 400, '#2a6fb0', 'end')
marker(440, 316, 6)
# submit
rect(425, 332, 280, 36, '#555555', '#555555', 6)
text(565, 355, 'Sign In', 13, 700, '#ffffff', 'middle')
marker(716, 330, 7)
# divider + google
parts.append(f'<line x1="425" y1="388" x2="520" y2="388" stroke="{LINE}"/><line x1="610" y1="388" x2="705" y2="388" stroke="{LINE}"/>')
text(565, 392, 'or', 11, 400, GREY, 'middle')
rect(425, 400, 280, 30, '#ffffff', '#888888', 15)
text(565, 420, 'Continue with Google', 12, 400, INK, 'middle')
marker(716, 398, 8)

svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">' + ''.join(parts) + '</svg>'
out = Path(__file__).parent
(out / 'wireframe-example-sign-in.svg').write_text(svg, encoding='utf-8')
(out / 'wireframe-example-sign-in.html').write_text(
    f'<!doctype html><meta charset="utf-8"><body style="margin:0;background:#fff;overflow:hidden"><div style="line-height:0">{svg}</div></body>', encoding='utf-8')
print('written', W, H)
