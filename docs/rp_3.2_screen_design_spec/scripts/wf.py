"""Tiny wireframe DSL that writes draw.io (.drawio) files.

Each Page is one draw.io tab (= one "Design File Frame"). Shapes are plain mxCells so the
files open and edit normally in draw.io Desktop / diagrams.net.
Style: grayscale wireframe + one accent (#123AB5) for primary actions and active states.
"""
import html as _html
import re
from xml.sax.saxutils import escape, quoteattr

PRIMARY = "#123AB5"
PRIMARY_SOFT = "#E8EDFB"
INK = "#1F2937"
MUTED = "#6B7280"
LINE = "#C9CED6"
SOFT = "#F3F4F6"
CANVAS = "#F9FAFB"
MEDIA = "#E5E7EB"
DARK = "#4B5563"
FONT = "Arial"


def _style(**kw):
    return ";".join(f"{k}={v}" for k, v in kw.items()) + ";"


def page_id(name):
    """Stable draw.io page id used by 'data:page/id,…' links ('P-30 / Populated' -> 'P-30-Populated')."""
    return re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-")


def plain(value):
    """Visible text of a cell value (HTML stripped, whitespace collapsed)."""
    return " ".join(_html.unescape(re.sub(r"<[^>]+>", " ", value or "")).split())


class Page:
    def __init__(self, name):
        self.name = name
        self.cells = []   # vertex dicts (may receive a link) or raw edge XML strings
        self._n = 1
        self.caption_id = None

    def _id(self):
        self._n += 1
        return f"c{self._n}"

    # ---- primitives -------------------------------------------------------
    def cell(self, value, style, x, y, w, h):
        cid = self._id()
        self.cells.append(dict(id=cid, value=value, style=style, x=x, y=y, w=w, h=h, link=None))
        return cid

    def vertices(self):
        return [c for c in self.cells if isinstance(c, dict)]

    def edge(self, src, dst, label="", dashed=False, color=DARK, exit=None, entry=None, waypoints=None):
        cid = self._id()
        st = dict(edgeStyle="orthogonalEdgeStyle", rounded=1, html=1, endArrow="block", endFill=1,
                  strokeColor=color, fontSize=12, fontFamily=FONT, fontColor=INK, labelBackgroundColor="#FFFFFF")
        if dashed:
            st["dashed"] = 1
        if exit:
            st.update(exitX=exit[0], exitY=exit[1], exitDx=0, exitDy=0)
        if entry:
            st.update(entryX=entry[0], entryY=entry[1], entryDx=0, entryDy=0)
        pts = ""
        if waypoints:
            pts = "<Array as=\"points\">" + "".join(f'<mxPoint x="{px}" y="{py}"/>' for px, py in waypoints) + "</Array>"
        self.cells.append(
            f'<mxCell id="{cid}" value={quoteattr(label)} style={quoteattr(_style(**st))} edge="1" parent="1" '
            f'source="{src}" target="{dst}"><mxGeometry relative="1" as="geometry">{pts}</mxGeometry></mxCell>')
        return cid

    def rect(self, x, y, w, h, fill="#FFFFFF", stroke=LINE, r=8, dashed=False, sw=1, value="", size=14,
             color=INK, bold=False, align="center", valign="middle", opacity=None, shadow=False):
        st = dict(rounded=1 if r else 0, arcSize=r, absoluteArcSize=1, whiteSpace="wrap", html=1,
                  fillColor=fill, strokeColor=stroke, strokeWidth=sw, fontFamily=FONT, fontSize=size,
                  fontColor=color, align=align, verticalAlign=valign, spacingLeft=8, spacingRight=8)
        if dashed:
            st["dashed"] = 1
        if bold:
            st["fontStyle"] = 1
        if opacity is not None:
            st["opacity"] = opacity
        return self.cell(value, _style(**st), x, y, w, h)

    def text(self, x, y, w, h, value, size=16, color=INK, bold=False, align="left", valign="middle", italic=False):
        fs = (1 if bold else 0) + (2 if italic else 0)
        st = dict(text=None, html=1, whiteSpace="wrap", align=align, verticalAlign=valign, fontFamily=FONT,
                  fontSize=size, fontColor=color, fontStyle=fs, spacing=0)
        style = "text;" + _style(**{k: v for k, v in st.items() if k != "text"})
        return self.cell(value, style, x, y, w, h)

    # ---- components -------------------------------------------------------
    def btn(self, x, y, w, h, label, kind="primary", size=15):
        if kind == "primary":
            return self.rect(x, y, w, h, fill=PRIMARY, stroke=PRIMARY, r=10, value=label, color="#FFFFFF", bold=True, size=size)
        if kind == "secondary":
            return self.rect(x, y, w, h, fill="#FFFFFF", stroke=DARK, r=10, value=label, color=INK, bold=True, size=size)
        if kind == "disabled":
            return self.rect(x, y, w, h, fill=SOFT, stroke=LINE, r=10, value=label, color=MUTED, size=size)
        return self.text(x, y, w, h, f"<u>{label}</u>", size=size, color=PRIMARY, bold=True, align="center")  # link

    def link(self, x, y, w, label, size=14, align="left"):
        return self.text(x, y, w, 22, f"<u>{label}</u>", size=size, color=PRIMARY, align=align)

    def field(self, x, y, w, label, placeholder, suffix=None, error=None):
        self.text(x, y, w, 20, label, size=14, bold=True)
        self.rect(x, y + 24, w, 44, fill="#FFFFFF", stroke=DARK if not error else INK, r=8,
                  value=placeholder, color=MUTED, align="left", size=14, sw=1 if not error else 2)
        if suffix:
            self.text(x + w - 40, y + 24, 30, 44, suffix, size=14, color=MUTED, align="center")
        if error:
            self.text(x, y + 72, w, 20, "⚠ " + error, size=13, color=INK, italic=True)
            return y + 98
        return y + 80

    def img(self, x, y, w, h, label="Image"):
        self.rect(x, y, w, h, fill=MEDIA, stroke=LINE, r=6)
        return self.text(x, y + h / 2 - 12, w, 24, "▨ " + label, size=13, color=MUTED, align="center")

    def chip(self, x, y, label, kind="neutral", w=None, size=12):
        w = w or max(56, int(len(label) * 7.4) + 22)
        fill, stroke, color = {"neutral": (SOFT, LINE, INK), "active": (PRIMARY_SOFT, PRIMARY, PRIMARY),
                               "solid": (PRIMARY, PRIMARY, "#FFFFFF"), "dark": (DARK, DARK, "#FFFFFF"),
                               "outline": ("#FFFFFF", DARK, INK)}[kind]
        self.rect(x, y, w, 24, fill=fill, stroke=stroke, r=12, value=label, size=size, color=color, bold=True)
        return x + w + 8

    def progress(self, x, y, w, pct, h=8):
        self.rect(x, y, w, h, fill=MEDIA, stroke=MEDIA, r=h // 2)
        if pct:
            self.rect(x, y, max(h, int(w * pct / 100)), h, fill=PRIMARY, stroke=PRIMARY, r=h // 2)

    def lines(self, x, y, w, n=3, gap=16, h=8, last=0.6):
        for i in range(n):
            ww = w if i < n - 1 else int(w * last)
            self.rect(x, y + i * gap, ww, h, fill=MEDIA, stroke=MEDIA, r=4)
        return y + n * gap

    def zone(self, x, y, letter):
        st = _style(ellipse=None, whiteSpace="wrap", html=1, fillColor=INK, strokeColor="#FFFFFF", strokeWidth=2,
                    fontColor="#FFFFFF", fontStyle=1, fontSize=13, fontFamily=FONT, align="center")
        return self.cell(letter, "ellipse;" + st.replace("ellipse=None;", ""), x, y, 26, 26)

    def icon(self, x, y, glyph, size=18, color=MUTED, box=24):
        return self.text(x, y, box, box, glyph, size=size, color=color, align="center")

    def card(self, x, y, w, h, fill="#FFFFFF", stroke=LINE, r=14, dashed=False, sw=1):
        return self.rect(x, y, w, h, fill=fill, stroke=stroke, r=r, dashed=dashed, sw=sw)

    def overlay(self, w, h, oy=0, ox=0):
        frame = getattr(self, "frame_cell", None)
        if frame is not None and ox == frame["x"] and oy == frame["y"]:
            h = max(h, frame["h"])  # cover a footer that already grew the frame
        return self.rect(ox, oy, w, h, fill="#111827", stroke="none", r=0, opacity=45)

    def caption(self, title, sub, w):
        """Frame title + legend strip drawn above the frame (y < 0)."""
        self.caption_id = self.text(0, -86, w * 0.62, 30, f"<b>{escape(title)}</b>", size=20, color=INK)
        self.text(0, -54, w * 0.62, 22, sub, size=13, color=MUTED)
        lx = w - 560
        self.rect(lx, -84, 18, 18, fill=PRIMARY, stroke=PRIMARY, r=4)
        self.text(lx + 24, -86, 120, 22, "Primary CTA", size=12, color=MUTED)
        self.rect(lx + 130, -84, 18, 18, fill="#FFFFFF", stroke=DARK, r=4)
        self.text(lx + 154, -86, 120, 22, "Secondary", size=12, color=MUTED)
        self.rect(lx + 250, -84, 18, 18, fill=MEDIA, stroke=LINE, r=2)
        self.text(lx + 274, -86, 120, 22, "Media / data", size=12, color=MUTED)
        self.zone(lx + 380, -86, "A")
        self.text(lx + 412, -86, 150, 22, "Zone (Part 4)", size=12, color=MUTED)
        self.rect(0, -32, w, 2, fill=LINE, stroke=LINE, r=0)

    def frame(self, w, h, ox=0, oy=0, fill="#FFFFFF"):
        cid = self.rect(ox, oy, w, h, fill=fill, stroke="#9CA3AF", r=0, sw=2)
        self.frame_cell = self.vertices()[-1]  # footer() may grow it
        return cid

    @staticmethod
    def _cell_xml(c):
        if isinstance(c, str):
            return c
        geom = f'<mxGeometry x="{c["x"]}" y="{c["y"]}" width="{c["w"]}" height="{c["h"]}" as="geometry"/>'
        if c["link"]:
            # Clickable shape: draw.io stores links on a UserObject wrapper; 'data:page/id,…' jumps to another tab.
            return (f'<UserObject label={quoteattr(c["value"])} link={quoteattr("data:page/id," + c["link"])} id="{c["id"]}">'
                    f'<mxCell style={quoteattr(c["style"])} vertex="1" parent="1">{geom}</mxCell></UserObject>')
        return (f'<mxCell id="{c["id"]}" value={quoteattr(c["value"])} style={quoteattr(c["style"])} vertex="1" parent="1">'
                f"{geom}</mxCell>")

    def xml(self):
        body = "".join(self._cell_xml(c) for c in self.cells)
        return (f'<diagram name={quoteattr(self.name)} id={quoteattr(page_id(self.name))}>'
                f'<mxGraphModel dx="1600" dy="1000" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" '
                f'arrows="1" fold="1" page="0" pageScale="1" math="0" shadow="0"><root><mxCell id="0"/>'
                f'<mxCell id="1" parent="0"/>{body}</root></mxGraphModel></diagram>')


class DrawioFile:
    def __init__(self, path):
        self.path = path
        self.pages = []

    def page(self, name):
        p = Page(name)
        self.pages.append(p)
        return p

    def save(self):
        xml = ('<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="drawio" agent="IELTSSpace SDS generator" '
               'version="24.0.0">' + "".join(p.xml() for p in self.pages) + "</mxfile>\n")
        with open(self.path, "w", encoding="utf-8") as fh:
            fh.write(xml)
