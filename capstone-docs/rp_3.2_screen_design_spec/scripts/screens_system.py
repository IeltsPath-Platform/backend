"""Wireframes: shared layout shell (Part 4.0) and system screens (P-90, P-91, P-92)."""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, CANVAS, DARK, MEDIA
from chrome import (navbar, mobile_nav, footer, consult_fab, screen, mobile_screen, phone, eyebrow,
                    DESKTOP_W, MOBILE_W, NAV_H, SUB_H)


def note(p, x, y, w, text, h=40):
    p.text(x, y, w, h, text, size=13, color=INK, italic=True, valign="top")


def build_shell(new):
    f = new("SHELL_SharedLayout")
    # Learner desktop
    p = screen(f, "SHELL", "Learner Desktop", "Shared Layout — Authenticated Shell", h=760)
    y = navbar(p, role="learner", active="Khóa học", child="Khóa học")
    p.zone(14, 22, "A")
    p.zone(14, NAV_H + 8, "B")
    note(p, 1100, NAV_H + SUB_H + 6, 320, "B — subnav: sections of the active / hovered primary item", 40)
    p.zone(100 - 36, y + 20, "C")
    p.rect(100, y + 20, 1240, 64, fill=SOFT, stroke=DARK, r=12, value="🛡 ReviewGateBanner (/learn/* only, when a review is mandatory) · NoticeBanner (location.state.notice) ✕", size=14)
    p.zone(100 - 36, y + 100, "D")
    p.rect(100, y + 100, 1240, 380, fill="#FFFFFF", stroke=LINE, r=14, dashed=True,
           value="<b>main#main-content</b> — page content (LearnLayout: .lp-shell max 1240px; Practice/Home: shell max 1440px)<br>Per-route ErrorBoundary: “Không thể hiển thị lộ trình học”", size=16, color=MUTED)
    p.zone(100 - 36, y + 500, "E")
    p.rect(100, y + 500, 1240, 46, fill=SOFT, stroke=LINE, r=10, value="🧪 DemoControls (only when VITE_USE_MOCK_LEARNING): “Đặt lại demo” · “Giả lỗi máy chủ 500”", size=13)
    consult_fab(p, 1250, 760 - 130)
    p.zone(14, 760 - 50, "F")
    footer(p, 760 - 64)
    # user dropdown
    p = screen(f, "SHELL", "User Menu", "Shared Layout — UserTierDropdown", h=560)
    navbar(p, role="learner", active="Khóa học", child="Khóa học")
    p.card(1080, 70, 330, 300, r=16, stroke=DARK, sw=2)
    p.zone(1067, 80, "A")
    p.text(1100, 86, 290, 18, "GÓI HỌC MÔ PHỎNG", size=11, color=PRIMARY, bold=True)
    p.text(1100, 108, 290, 30, "<b>Free learner</b>   [FREE]", size=17)
    p.text(1100, 140, 290, 50, "Bạn đang trải nghiệm không gian học cơ bản.<br><b>30</b> Points sẵn sàng cho hành trình hôm nay.", size=12, color=MUTED, valign="top")
    p.btn(1100, 196, 290, 40, "Chuyển sang Premium (demo)", "secondary", size=13)
    p.text(1100, 244, 290, 36, "Chế độ demo — không thay đổi gói học thực tế.", size=11, color=MUTED, italic=True)
    p.btn(1100, 292, 290, 40, "Đăng xuất", size=13)
    note(p, 100, 400, 900, "“Đăng xuất” → logoutSession() → /login (replace). The tier toggle only changes FE demo state (setDemoTier).", 50)
    # Guest
    p = screen(f, "SHELL", "Guest Desktop", "Shared Layout — Public Shell", h=520)
    y = navbar(p, role="guest", active="Trang chủ")
    p.zone(14, 22, "A")
    note(p, 300, y + 30, 1000, "Guest nav: Trang chủ · Khóa học · Luyện tập 4 kỹ năng · Bài mẫu Writing 8.0+ · Kết quả học viên. Every item except Trang chủ is RequireAuth → P-01 (state.from). The Đăng nhập / Đăng ký buttons are hidden on /login and /register.", 70)
    p.rect(100, y + 120, 1240, 220, fill="#FFFFFF", stroke=LINE, r=14, dashed=True, value="Public content (P-10, P-50) — floating subnav when hovering an item with sections", size=15, color=MUTED)
    footer(p, 520 - 64)
    # Auth shell
    p = screen(f, "SHELL", "Auth Shell", "Shared Layout — AuthShell", h=620)
    y = navbar(p, role="guest", hide_auth=True)
    p.rect(0, y, DESKTOP_W, 620 - y - 64, fill="#FFFFFF", stroke="none", r=0)
    p.zone(130, y + 60, "A")
    p.rect(160, y + 60, 560, 360, fill=SOFT, stroke=LINE, r=14, dashed=True, value="auth-ambient-copy (✦ IELTS SPACE · title · description) + ClassMascot lg<br>particle background (InteractiveCanvasBackground) + floating icons", size=14, color=MUTED)
    p.zone(790, y + 60, "B")
    p.rect(820, y + 40, 500, 400, fill="#FFFFFF", stroke=DARK, r=24, value="<b>auth-form-panel</b><br>(P-01 / P-02 / P-03 / P-04 / P-05)", size=16)
    footer(p, 620 - 64)
    # Exam shell
    p = screen(f, "SHELL", "Exam Shell", "Shared Layout — PlacementExamShell", h=520, fill="#FFFFFF")
    p.rect(0, 0, DESKTOP_W, 72, fill=PRIMARY, stroke=PRIMARY, r=0, value="Logo · Bài kiểm tra đầu vào · {skill} · Đã làm mm:ss · không giới hạn thời gian · [audio toolbar] · ✕ Lưu và quay lại", color="#FFFFFF", size=15)
    p.zone(14, 22, "A")
    p.rect(0, 72, DESKTOP_W, 50, fill=SOFT, stroke=LINE, r=0, value="Part N · section instructions", size=14, align="left")
    p.zone(14, 140, "B")
    p.rect(40, 140, 1360, 260, fill="#FFFFFF", stroke=LINE, r=12, dashed=True, value="pl-exam__body — skill runner (Objective / Writing / Speaking)", size=15, color=MUTED)
    p.zone(14, 420, "C")
    p.rect(0, 410, DESKTOP_W, 110, fill=SOFT, stroke=LINE, r=0, value="pl-exam__foot (per runner): question bar + ✓ Nộp phần này button", size=14)
    # Mobile
    p = mobile_screen(f, "SHELL", "Shared Layout", 2, h=700, frame="Mobile Nav")
    ox = phone(p, 0, "Learner · đóng", h=700)
    y = mobile_nav(p, ox=ox)
    p.rect(ox, y, MOBILE_W, 40, fill=PRIMARY_SOFT, stroke=LINE, r=0, value="subnav (cuộn ngang)", size=12)
    p.rect(ox + 16, y + 60, 343, 520, fill="#FFFFFF", stroke=LINE, r=12, dashed=True, value="Nội dung 1 cột · lề 16px", size=13, color=MUTED)
    footer(p, 700 - 64, ox=ox, w=MOBILE_W)
    ox = phone(p, 1, "Guest · menu mở", h=700)
    y = mobile_nav(p, role="guest", ox=ox)
    p.rect(ox, y, MOBILE_W, 330, fill=PRIMARY, stroke=PRIMARY, r=0)
    for i, t in enumerate(("Trang chủ", "Khóa học", "Luyện tập 4 kỹ năng", "Bài mẫu Writing 8.0+", "Kết quả học viên")):
        p.rect(ox + 16, y + 16 + i * 60, 343, 48, fill="#FFFFFF" if i == 0 else PRIMARY, stroke="#FFFFFF", r=10, value=t,
               color=PRIMARY if i == 0 else "#FFFFFF", size=14, bold=True)
    f.save()


def status_card(p, y, icon, kicker, title, body, ctas):
    x, w = 470, 500
    p.card(x, y, w, 360, r=26)
    p.zone(x - 13, y + 30, "A")
    p.rect(x + w / 2 - 28, y + 36, 56, 56, fill=PRIMARY_SOFT, stroke=PRIMARY_SOFT, r=16, value=icon, size=22, color=PRIMARY)
    p.text(x, y + 108, w, 20, kicker, size=13, color=PRIMARY, bold=True, align="center")
    p.text(x + 20, y + 132, w - 40, 40, f"<b>{title}</b>", size=28, align="center")
    p.text(x + 30, y + 180, w - 60, 50, body, size=15, color=MUTED, align="center", valign="top")
    p.zone(x - 13, y + 270, "B")
    if len(ctas) == 1:
        p.btn(x + w / 2 - 90, y + 268, 180, 48, ctas[0])
    else:
        p.btn(x + 60, y + 268, 180, 48, ctas[0])
        p.btn(x + 260, y + 268, 180, 48, ctas[1], "secondary")


def build_system(new):
    f = new("P-90_NotFound")
    p = screen(f, "P-90", "Populated", "Not Found", h=720)
    status_card(p, 140, "←", "404", "Không tìm thấy trang", "Không có màn hình nào được khai báo cho /duong-dan-sai.", ["Về Overview"])
    note(p, 470, 520, 500, "No SiteNavbar. The global footer still renders below <Routes>. A Guest clicking “Về Overview” → RequireAuth → P-01.", 60)
    footer(p, 720 - 64)
    f.save()

    f = new("P-91_LearnNotFound")
    p = screen(f, "P-91", "Populated", "Learning Path — Not Found", h=700)
    y = navbar(p, role="learner", active="Khóa học")
    p.card(470, y + 80, 500, 300, r=18)
    p.zone(457, y + 100, "A")
    p.text(470, y + 110, 500, 36, "📄", size=26, align="center")
    p.text(470, y + 156, 500, 34, "<b>Không tìm thấy trang</b>", size=24, align="center")
    p.text(500, y + 196, 440, 44, "Nội dung này không tồn tại hoặc đã bị gỡ.", size=15, color=MUTED, align="center")
    p.zone(457, y + 270, "B")
    p.btn(620, y + 270, 200, 48, "Về lộ trình")
    note(p, 470, y + 400, 500, "Rendered inside LearnLayout for unmatched /learn/* and when the API returns NOT_FOUND.", 40)
    footer(p, 700 - 64)
    f.save()

    f = new("P-92_RouteStatus")
    p = screen(f, "P-92", "Populated", "Route Status (stub template)", h=720)
    status_card(p, 140, "🚧", "IELTSPATH", "{title}", "Trang này đã có đường dẫn riêng nhưng nội dung vẫn đang được hoàn thiện.", ["Về Overview", "Đến lớp học"])
    note(p, 300, 520, 840, "Used by: /practice “Thực hành” · /submission-history · /flashcards · /writing-samples · /student-results · /classes/:classCode/join · /mentors/:mentorSlug · /terms · /privacy · /copyright.", 60)
    footer(p, 720 - 64)
    f.save()
