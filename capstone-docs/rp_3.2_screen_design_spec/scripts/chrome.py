"""Shared chrome (SiteNavbar, subnav, footer, LearnLayout bits) mirrored from the React components."""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, CANVAS, DARK

DESKTOP_W = 1440
MOBILE_W = 375

GUEST_NAV = ["Trang chủ", "Khóa học", "Luyện tập 4 kỹ năng", "Bài mẫu Writing 8.0+", "Kết quả học viên"]
MEMBER_NAV = ["Trang chủ", "Khóa học", "Luyện tập 4 kỹ năng", "Sổ từ vựng", "Kết quả học viên"]
SUBNAV = {
    "Khóa học": ["Test đầu vào 4 kỹ năng FREE", "Khóa học"],
    "Luyện tập 4 kỹ năng": ["Listening", "Reading", "Writing", "Speaking"],
    "Trang chủ@member": ["Dashboard", "Lịch sử nộp bài", "Khóa học của tôi"],
    "Sổ từ vựng": ["Flashcard của tôi", "Kho từ vựng", "Bài mẫu 8đ"],
}
NAV_H = 72
# Mobile frames are out of scope for this SDS revision (desktop web first). Set True to draw them again.
INCLUDE_MOBILE = False
SUB_H = 44


def navbar(p, role="learner", active=None, child=None, ox=0, oy=0, w=DESKTOP_W, hide_auth=False):
    """Desktop SiteNavbar. Returns y after navbar (+subnav when the active item has sections)."""
    p.rect(ox, oy, w, NAV_H, fill=PRIMARY, stroke=PRIMARY, r=0)
    p.rect(ox + 40, oy + 14, 150, 44, fill=PRIMARY, stroke="#FFFFFF", r=6, value="IELTS Space logo",
           color="#FFFFFF", size=13, bold=True)
    items = MEMBER_NAV if role == "learner" else GUEST_NAV
    x = ox + 300
    for label in items:
        lw = int(len(label) * 8.6) + 36
        if label == active:
            p.rect(x, oy + 16, lw, 40, fill="#FFFFFF", stroke="#FFFFFF", r=20, value=label, color=PRIMARY, bold=True, size=15)
        else:
            p.text(x, oy + 16, lw, 40, label, size=15, color="#FFFFFF", align="center")
        x += lw + 10
    if role == "learner":
        p.rect(ox + w - 250, oy + 14, 210, 44, fill=PRIMARY, stroke="#FFFFFF", r=22,
               value="◯ Học viên · 30 Points ▾", color="#FFFFFF", size=14, bold=True)
    elif not hide_auth:
        p.rect(ox + w - 260, oy + 16, 110, 40, fill=PRIMARY, stroke="#FFFFFF", r=20, value="Đăng nhập", color="#FFFFFF", size=14, bold=True)
        p.rect(ox + w - 140, oy + 16, 100, 40, fill="#FFFFFF", stroke="#FFFFFF", r=20, value="Đăng ký", color=PRIMARY, size=14, bold=True)
    y = oy + NAV_H
    key = f"{active}@member" if role == "learner" and active == "Trang chủ" else active
    if key in SUBNAV:
        p.rect(ox, y, w, SUB_H, fill=PRIMARY_SOFT, stroke=LINE, r=0)
        cx = ox + 300
        for label in SUBNAV[key]:
            lw = int(len(label) * 8) + 30
            if label == child:
                p.text(cx, y + 6, lw, 32, f"<b><u>{label}</u></b>", size=14, color=PRIMARY, align="center")
            else:
                p.text(cx, y + 6, lw, 32, label, size=14, color=INK, align="center")
            cx += lw + 12
        y += SUB_H
    return y


def mobile_nav(p, role="learner", ox=0, oy=0, w=MOBILE_W, hide_auth=False):
    p.rect(ox, oy, w, 60, fill=PRIMARY, stroke=PRIMARY, r=0)
    p.rect(ox + 16, oy + 12, 100, 36, fill=PRIMARY, stroke="#FFFFFF", r=6, value="Logo", color="#FFFFFF", size=12, bold=True)
    if role == "learner":
        p.rect(ox + w - 120, oy + 12, 56, 36, fill=PRIMARY, stroke="#FFFFFF", r=18, value="◯ ▾", color="#FFFFFF", size=13)
    elif not hide_auth:
        p.rect(ox + w - 196, oy + 14, 74, 32, fill=PRIMARY, stroke="#FFFFFF", r=16, value="Đăng nhập", color="#FFFFFF", size=11, bold=True)
        p.rect(ox + w - 118, oy + 14, 60, 32, fill="#FFFFFF", stroke="#FFFFFF", r=16, value="Đăng ký", color=PRIMARY, size=11, bold=True)
    p.rect(ox + w - 56, oy + 8, 44, 44, fill=PRIMARY, stroke=PRIMARY, r=8, value="☰", color="#FFFFFF", size=22)
    return oy + 60


FOOTER_H = 420
FOOTER_FILL = "#0D2B8D"  # --classroom-primary-strong (real footer: hero-start → primary-strong gradient)


def footer(p, y, ox=0, w=DESKTOP_W, h=64):
    """SiteFooter (components/SiteFooter.tsx). Desktop draws the full footer and grows the frame to fit it."""
    if w < 500:
        p.rect(ox, y, w, h, fill=FOOTER_FILL, stroke=FOOTER_FILL, r=0, value="SiteFooter (see 4.0)", color="#FFFFFF", size=10)
        return y + h
    start = len(p.cells)
    white, soft = "#FFFFFF", "#C7D2F0"
    p.rect(ox, y, w, FOOTER_H, fill=FOOTER_FILL, stroke=FOOTER_FILL, r=0)
    # column 1 — brand + contact
    x1 = ox + 110
    p.rect(x1, y + 30, 150, 44, fill=FOOTER_FILL, stroke=white, r=6, value="IELTS Space logo", color=white, size=12, bold=True)
    p.rect(x1 + 165, y + 30, 1, 44, fill=soft, stroke=soft, r=0)
    p.text(x1 + 180, y + 30, 330, 44, "<b>THE IELTS SPACE - KHÔNG GIAN HỌC TẬP TÍCH HỢP THẾ HỆ MỚI</b>", size=12, color=white)
    p.text(x1, y + 92, 400, 22, "<b>THÔNG TIN LIÊN HỆ</b>", size=14, color=white)
    p.rect(x1, y + 122, 480, 150, fill="#E9EFF8", stroke="#E9EFF8", r=10,
           value="📍 <b>The IELTS Space</b><br><br>Mở trong Maps ↗", size=12, color=INK)
    for i, line in enumerate(("📍 Tòa Nhà Hoà Phát, 257 Giải Phóng, Bạch Mai, Hà Nội",
                              "✉ theenglishspace01@gmail.com", "☎ 0888.861.786")):
        p.text(x1, y + 284 + i * 26, 480, 22, line, size=12, color=white)
    # column 2 — about + social
    x2 = ox + 680
    p.text(x2, y + 40, 320, 22, "<b>VỀ THE IELTS SPACE</b>", size=14, color=white)
    p.text(x2, y + 76, 200, 22, "<u>Giới thiệu</u>", size=12, color=white)
    p.text(x2, y + 104, 200, 22, "<u>Khóa học</u>", size=12, color=white)
    p.text(x2, y + 150, 320, 22, "<b>KẾT NỐI VỚI THE IELTS SPACE</b>", size=14, color=white)
    for i, glyph in enumerate(("f", "♪", "◎", "▶")):
        p.rect(x2 + i * 56, y + 186, 44, 44, fill=white, stroke=white, r=22, value=glyph, color=FOOTER_FILL, size=15, bold=True)
    # column 3 — support + legal
    x3 = ox + 1040
    p.text(x3, y + 40, 300, 22, "<b>TRUNG TÂM HỖ TRỢ</b>", size=14, color=white)
    for i, label in enumerate(("Điều khoản sử dụng", "Chính sách bảo mật", "Chính sách bản quyền")):
        p.text(x3, y + 76 + i * 28, 280, 22, f"<u>{label}</u>", size=12, color=white)
    p.text(x3, y + 172, 300, 22, "<b>THÔNG TIN PHÁP LÝ</b>", size=14, color=white)
    p.text(x3, y + 206, 300, 22, "🏢 CÔNG TY CỔ PHẦN GD&amp;ĐT The Space", size=12, color=white)
    p.text(x3, y + 232, 300, 22, "Mã số thuế: 0110883975", size=12, color=white)
    # bottom bar
    p.rect(ox, y + FOOTER_H - 46, w, 1, fill=soft, stroke=soft, r=0)
    p.text(ox, y + FOOTER_H - 40, w, 32, "📖 © 2026 The IELTS Space. All rights reserved.", size=12, color=soft, align="center")
    for cell in p.cells[start:]:
        if isinstance(cell, dict):
            cell["zone"] = "footer"
    # grow the frame (and any full-frame overlay) so the footer sits inside it
    frame = getattr(p, "frame_cell", None)
    if frame is not None and frame["y"] + frame["h"] < y + FOOTER_H:
        old_h, frame["h"] = frame["h"], y + FOOTER_H - frame["y"]
        for cell in p.vertices():
            if "opacity=45" in cell["style"] and cell["h"] == old_h:
                cell["h"] = frame["h"]
    return y + FOOTER_H


def consult_fab(p, x, y):
    p.rect(x, y, 170, 44, fill="#FFFFFF", stroke=DARK, r=22, value="💬 Tư vấn miễn phí", color=INK, size=13, bold=True)


def back_link(p, x, y, label):
    p.text(x, y, 420, 24, f"← {label}", size=14, color=PRIMARY, bold=True)


def eyebrow(p, x, y, label, w=600):
    p.text(x, y, w, 20, label.upper(), size=12, color=PRIMARY, bold=True)


def notice(p, x, y, w, label):
    p.rect(x, y, w, 40, fill=SOFT, stroke=DARK, r=8, value=f"ⓘ {label}   ✕", color=INK, size=13, align="left")


PHONE_GAP = 70


def phone(p, i, label, h=760):
    """i-th mobile 375 frame in a row, labelled above. Returns its x offset."""
    ox = i * (MOBILE_W + PHONE_GAP)
    p.text(ox, -22, MOBILE_W, 20, f"<b>{label}</b>", size=13, color=MUTED, align="center")
    p.frame(MOBILE_W, h, ox=ox, oy=0, fill=CANVAS)
    return ox


def screen(f, pid, frame, title, w=DESKTOP_W, h=900, fill=CANVAS, sub=None):
    """New tab named '<pid> / <frame>' with caption + frame border. Returns the page."""
    p = f.page(f"{pid} / {frame}")
    p.caption(f"[{pid}] {title} — {frame}", sub or f"Desktop {w}px · draw.io frame '{pid} / {frame}'", max(w, 1100))
    p.frame(w, h, fill=fill)
    return p


def mobile_screen(f, pid, title, count, h=760, frame="Mobile"):
    if not INCLUDE_MOBILE:
        from wf import Page
        return Page(f"{pid} / {frame}")  # detached: drawn but never saved
    p = f.page(f"{pid} / {frame}")
    total = count * MOBILE_W + (count - 1) * PHONE_GAP
    p.caption(f"[{pid}] {title} — {frame}", f"Mobile {MOBILE_W}px · draw.io frame '{pid} / {frame}'", max(total, 1100))
    return p
