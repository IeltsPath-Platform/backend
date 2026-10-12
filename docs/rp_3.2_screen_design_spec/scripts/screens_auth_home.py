"""Wireframes: Auth (P-01…P-04) and Home (P-10)."""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, CANVAS, DARK, MEDIA
from chrome import (navbar, mobile_nav, footer, consult_fab, screen, mobile_screen, phone, eyebrow,
                    DESKTOP_W, MOBILE_W)

AMBIENT = {
    "default": ("Mỗi ngày một bước. Gần hơn band mục tiêu.", "Giữ bài học, lộ trình và từ vựng trong một không gian tập trung."),
    "forgot": ("Quên mật khẩu? Khôi phục nhanh.", "Nhập email đã đăng ký. Mã đặt lại sẽ được xử lý phía máy chủ (demo: xem log BE)."),
}


def auth_backdrop(p, top, ambient="default"):
    title, body = AMBIENT[ambient]
    p.text(160, top + 120, 300, 20, "✦ IELTS SPACE", size=13, color=PRIMARY, bold=True)
    p.text(160, top + 150, 520, 170, title, size=44, color=INK, bold=True, valign="top")
    p.text(160, top + 300, 520, 50, body, size=16, color=MUTED, valign="top")
    p.img(250, top + 400, 300, 280, "Mascot ClassMascot (lg)")
    p.text(60, top + 30, 300, 20, "background: InteractiveCanvasBackground (particles) + floating icons", size=11, color=MUTED, italic=True)
    p.zone(130, top + 145, "A")


def auth_card(p, x, y, mode, state="populated"):
    """Auth form panel; returns bottom y."""
    w = 500
    heights = {"sign-in": 640, "sign-up": 800, "forgot": 420, "reset": 560}
    h = heights[mode] + (40 if state != "populated" else 0)
    p.card(x, y, w, h, r=24)
    ix, iw = x + 40, w - 80
    copy = {
        "sign-in": ("IELTS SPACE", "Chào mừng bạn trở lại", "Đăng nhập để tiếp tục lộ trình IELTS của bạn."),
        "sign-up": ("BẮT ĐẦU HỌC", "Tạo tài khoản mới", "Bắt đầu kế hoạch học IELTS phù hợp với bạn."),
        "forgot": ("IELTS SPACE", "Quên mật khẩu", "Chúng tôi sẽ xử lý mã đặt lại cho email của bạn."),
        "reset": ("IELTS SPACE", "Đặt lại mật khẩu", "Nhập mã xác nhận và mật khẩu mới."),
    }[mode]
    eyebrow(p, ix, y + 36, copy[0])
    p.text(ix, y + 60, iw, 40, copy[1], size=30, bold=True)
    p.text(ix, y + 102, iw, 22, copy[2], size=14, color=MUTED)
    p.zone(x - 13, y + 60, "A")
    cy = y + 140
    p.zone(x - 13, cy + 10, "B")
    if mode == "sign-in":
        cy = p.field(ix, cy, iw, "Email", "name@example.com")
        cy = p.field(ix, cy, iw, "Mật khẩu", "Mật khẩu", suffix="👁")
        p.link(ix, cy - 4, iw, "Quên mật khẩu?", size=13, align="right")
        cy += 30
    elif mode == "sign-up":
        cy = p.field(ix, cy, iw, "Họ và tên", "Nguyễn Văn A")
        cy = p.field(ix, cy, iw, "Email", "name@example.com")
        cy = p.field(ix, cy, iw, "Mật khẩu", "Từ 6 đến 72 ký tự", suffix="👁")
        err = "Mật khẩu xác nhận không khớp." if state == "error" else None
        cy = p.field(ix, cy, iw, "Xác nhận mật khẩu", "Nhập lại mật khẩu", suffix="👁", error=err)
        cy += 6
    elif mode == "forgot":
        cy = p.field(ix, cy, iw, "Email", "name@example.com")
    else:
        cy = p.field(ix, cy, iw, "Mã đặt lại", "Dán mã từ log/Swagger hoặc email")
        cy = p.field(ix, cy, iw, "Mật khẩu mới", "Từ 6 đến 72 ký tự", suffix="👁")
        cy = p.field(ix, cy, iw, "Xác nhận mật khẩu", "Nhập lại mật khẩu mới", suffix="👁")
    p.zone(x - 13, cy + 10, "C")
    cta = {"sign-in": "Đăng nhập", "sign-up": "Tạo tài khoản", "forgot": "Gửi mã đặt lại", "reset": "Đặt lại mật khẩu"}[mode]
    if state == "loading":
        cta = "Đang gửi…" if mode == "forgot" else "Đang xử lý…"
    if mode == "forgot" and state == "success":
        cta = "Gửi lại mã"
    p.btn(ix, cy, iw, 52, cta, "disabled" if state == "loading" else "primary", size=16)
    cy += 70
    if mode in ("sign-in", "sign-up"):
        p.rect(ix, cy + 10, iw, 1, fill=LINE, stroke=LINE, r=0)
        p.rect(ix + iw / 2 - 30, cy, 60, 20, fill="#FFFFFF", stroke="#FFFFFF", r=0, value="hoặc", color=MUTED, size=12)
        cy += 34
        p.btn(ix, cy, iw, 48, "G  Google (chưa hỗ trợ)", "disabled", size=15)
        cy += 62
    status = {
        ("sign-in", "error"): "Email hoặc mật khẩu không đúng. (thông điệp từ máy chủ)",
        ("sign-up", "error"): "Mật khẩu xác nhận không khớp.",
        ("forgot", "success"): "Nếu email tồn tại, mã đặt lại đã được tạo. (Môi trường demo: lấy mã từ log/Swagger BE.)",
        ("reset", "success"): "Đặt lại mật khẩu thành công. Đang chuyển tới đăng nhập…",
        ("reset", "error"): "Không đặt lại được mật khẩu. Kiểm tra mã và thử lại.",
    }.get((mode, state))
    if status:
        p.rect(ix, cy, iw, 46, fill=SOFT, stroke=DARK, r=8, value=f"role=status · {status}", size=13, color=INK, align="left")
        cy += 58
    p.zone(x - 13, cy, "D")
    prompt = {
        "sign-in": "Chưa có tài khoản? <b><u>Đăng ký ngay</u></b>",
        "sign-up": "Đã có tài khoản? <b><u>Đăng nhập</u></b>",
        "forgot": ("<u>Tôi đã có mã — đặt lại mật khẩu</u> · " if state == "success" else "") + "<u>Quay lại đăng nhập</u>",
        "reset": "Chưa có mã? <u>Yêu cầu lại</u> · <u>Đăng nhập</u>",
    }[mode]
    p.text(ix, cy, iw, 24, prompt, size=14, color=PRIMARY if mode in ("forgot", "reset") else INK, align="center")
    return y + h


def auth_desktop(f, pid, mode, frame, state="populated", title=""):
    h = {"sign-in": 900, "sign-up": 1040, "forgot": 780, "reset": 860}[mode]
    p = screen(f, pid, frame, title, h=h)
    top = navbar(p, role="guest", hide_auth=mode in ("sign-in", "sign-up"))
    p.rect(0, top, DESKTOP_W, h - top - 64, fill="#FFFFFF", stroke="none", r=0)
    auth_backdrop(p, top, "forgot" if mode == "forgot" else "default")
    auth_card(p, 820, top + 60, mode, state)
    consult_fab(p, 1250, h - 140)
    footer(p, h - 64)
    return p


def auth_mobile(f, pid, mode, title):
    p = mobile_screen(f, pid, title, 1)
    ox = phone(p, 0, "Populated · 375", h=900)
    y = mobile_nav(p, role="guest", ox=ox, hide_auth=mode in ("sign-in", "sign-up"))
    p.img(ox + 220, y + 20, 140, 120, "Mascot mờ (opacity .27)")
    p.text(ox + 16, y + 30, 200, 60, "≤1024px: ẩn ambient copy; mascot mờ phía sau form", size=11, color=MUTED, italic=True)
    p.card(ox + 16, y + 100, 343, 470 if mode == "sign-in" else 620, r=18)
    ix, iw = ox + 36, 303
    copy = {"sign-in": ("Chào mừng bạn trở lại", "Đăng nhập"), "sign-up": ("Tạo tài khoản mới", "Tạo tài khoản")}[mode]
    p.text(ix, y + 120, iw, 30, copy[0], size=22, bold=True)
    cy = y + 166
    if mode == "sign-up":
        cy = p.field(ix, cy, iw, "Họ và tên", "Nguyễn Văn A")
    cy = p.field(ix, cy, iw, "Email", "name@example.com")
    cy = p.field(ix, cy, iw, "Mật khẩu", "Mật khẩu", suffix="👁")
    if mode == "sign-up":
        cy = p.field(ix, cy, iw, "Xác nhận mật khẩu", "Nhập lại mật khẩu")
    else:
        p.link(ix, cy - 4, iw, "Quên mật khẩu?", size=12, align="right")
        cy += 26
    p.btn(ix, cy, iw, 48, copy[1])
    p.btn(ix, cy + 62, iw, 44, "G  Google (chưa hỗ trợ)", "disabled", size=13)
    p.text(ix, cy + 118, iw, 22, "Chưa có tài khoản? <b><u>Đăng ký ngay</u></b>" if mode == "sign-in" else "Đã có tài khoản? <b><u>Đăng nhập</u></b>", size=13, align="center")
    footer(p, 900 - 64, ox=ox, w=MOBILE_W)
    return p


def build_auth(new):
    f = new("P-01_SignIn")
    auth_desktop(f, "P-01", "sign-in", "Populated", title="Sign In")
    auth_desktop(f, "P-01", "sign-in", "Loading", "loading", "Sign In")
    auth_desktop(f, "P-01", "sign-in", "Error", "error", "Sign In")
    auth_mobile(f, "P-01", "sign-in", "Sign In")
    f.save()

    f = new("P-02_SignUp")
    auth_desktop(f, "P-02", "sign-up", "Populated", title="Sign Up")
    auth_desktop(f, "P-02", "sign-up", "Error", "error", "Sign Up")
    auth_mobile(f, "P-02", "sign-up", "Sign Up")
    f.save()

    f = new("P-03_ForgotPassword")
    auth_desktop(f, "P-03", "forgot", "Populated", title="Forgot Password")
    auth_desktop(f, "P-03", "forgot", "Success", "success", "Forgot Password")
    f.save()

    f = new("P-04_ResetPassword")
    auth_desktop(f, "P-04", "reset", "Populated", title="Reset Password")
    auth_desktop(f, "P-04", "reset", "Success", "success", "Reset Password")
    auth_desktop(f, "P-04", "reset", "Error", "error", "Reset Password")
    f.save()


# ---------------------------------------------------------------- Home (P-10)

def home_desktop(f, role):
    frame = "Populated" if role == "guest" else "Populated · Learner"
    h = 2060
    p = screen(f, "P-10", frame, "Home / Landing", h=h, fill="#FFFFFF")
    y = navbar(p, role=role, active="Trang chủ", child=None)
    # Hero
    p.rect(0, y, DESKTOP_W, 560, fill=SOFT, stroke="none", r=0)
    p.zone(70, y + 60, "A")
    p.chip(110, y + 70, "✦ KHÔNG GIAN HỌC IELTS CỦA BẠN", "active")
    p.text(110, y + 110, 640, 130, "Mỗi ngày một bước.<br><i>Gần hơn band mục tiêu.</i>", size=46, bold=True, valign="top")
    p.text(110, y + 250, 600, 60, "Học có định hướng, luyện tập chủ động và nhận phản hồi từ Mentor. Tất cả trong một không gian dành riêng cho bạn.", size=16, color=MUTED, valign="top")
    p.btn(110, y + 330, 230, 54, "Khám phá bài luyện →")
    p.btn(356, y + 330, 210, 54, "Gặp gỡ Mentor →", "secondary")
    x = 110
    for t in ("✓ 4 kỹ năng", "✓ Lộ trình rõ ràng", "✓ Học theo nhịp riêng"):
        x = p.chip(x, y + 410, t, "outline")
    p.img(820, y + 50, 500, 420, "Hero visual")
    p.text(820, y + 480, 500, 24, "✦ Hành trình lớn bắt đầu từ một bài học nhỏ", size=13, color=MUTED, align="center")
    y += 560
    # Introduction
    p.zone(70, y + 40, "B")
    p.img(110, y + 40, 420, 330, "Introduction image + 4 highlights")
    p.text(580, y + 40, 200, 20, "⌂ Giới thiệu ✦", size=13, color=PRIMARY, bold=True)
    p.text(580, y + 70, 740, 70, "THE IELTS SPACE KHÔNG GIAN HỌC TẬP TÍCH HỢP THẾ HỆ MỚI", size=26, bold=True, valign="top")
    p.lines(580, y + 160, 720, 4, gap=22, h=10)
    p.btn(580, y + 270, 300, 50, "Xem thêm về The IELTS Space →")
    y += 410
    # Orbit + quality
    p.zone(70, y + 30, "C")
    p.text(110, y + 30, 1220, 34, "TỐI ƯU HÀNH TRÌNH HỌC & LUYỆN THI IELTS", size=24, bold=True, align="center")
    for i, t in enumerate(("HỌC THÍCH ỨNG AI", "ĐỀ THI CHUẨN", "THƯ VIỆN CÁ NHÂN", "CHẤM ĐIỂM KÉP")):
        p.card(110 + i * 310, y + 90, 290, 150)
        p.text(130 + i * 310, y + 105, 250, 22, t, size=15, bold=True)
        p.lines(130 + i * 310, y + 145, 250, 3)
    p.text(110, y + 270, 1220, 30, "Commitments (01–03) · Teachers (3 cards + <u>Xem hồ sơ chuyên gia</u> → P-23) · HỌC VIÊN NÓI GÌ (carousel with “Tạm dừng” button)", size=14, color=MUTED, align="center")
    y += 330
    # Pricing
    p.zone(70, y + 20, "D")
    p.text(110, y + 20, 1220, 34, "BẢNG GÓI DỊCH VỤ & KÍCH HOẠT MÃ", size=24, bold=True, align="center")
    p.text(110, y + 56, 1220, 22, "🔑 Chọn Premium hoặc thẻ Point phù hợp rồi nhập Activation Key được cấp.", size=14, color=MUTED, align="center")
    p.rect(110, y + 90, 1220, 56, fill=PRIMARY_SOFT, stroke=PRIMARY, r=10,
           value=("<b>Gói FREE mặc định</b> — Miễn phí cho mọi tài khoản đăng ký, gồm bài học và câu hỏi tiêu chuẩn. (Guest)"
                  if role == "guest" else "<b>Đang sử dụng · FREE</b> (Learner) — or the notice “Phiên đăng nhập hiện tại chưa kết nối với dịch vụ gói và Activation Key.”"),
           size=14, align="left")
    for i, (t, items, cta) in enumerate((("Gói Premium", ["Premium 30 Ngày", "Premium 90 Ngày"], "Kích hoạt"),
                                          ("Thẻ nạp Point", ["Thẻ Point 50 · +50 Points", "Thẻ Point 100 · +100 Points"], "Nạp Key"))):
        cx = 110 + i * 620
        p.card(cx, y + 166, 600, 300)
        p.text(cx + 24, y + 186, 400, 30, t, size=20, bold=True)
        p.lines(cx + 24, y + 226, 540, 3, gap=20)
        for j, it in enumerate(items):
            p.rect(cx + 24 + j * 280, y + 300, 264, 140, fill=SOFT, stroke=LINE, r=10, value=f"<b>{it}</b><br>price · benefits", size=14)
            p.btn(cx + 44 + j * 280, y + 390, 224, 38, cta, size=13)
    y += 500
    consult_fab(p, 1250, h - 140)
    footer(p, h - 64)
    return p


def home_mobile(f):
    p = mobile_screen(f, "P-10", "Home / Landing", 2)
    ox = phone(p, 0, "Hero · Guest", h=760)
    y = mobile_nav(p, role="guest", ox=ox)
    p.rect(ox, y, MOBILE_W, 640, fill=SOFT, stroke="none", r=0)
    p.chip(ox + 16, y + 20, "✦ KHÔNG GIAN HỌC IELTS CỦA BẠN", "active", size=10)
    p.text(ox + 16, y + 56, 343, 90, "Mỗi ngày một bước. <i>Gần hơn band mục tiêu.</i>", size=26, bold=True, valign="top")
    p.lines(ox + 16, y + 160, 330, 3)
    p.btn(ox + 16, y + 220, 343, 48, "Khám phá bài luyện →")
    p.btn(ox + 16, y + 280, 343, 48, "Gặp gỡ Mentor →", "secondary")
    p.img(ox + 16, y + 350, 343, 230, "Hero visual (xếp dưới)")
    ox = phone(p, 1, "Bảng gói · 1 cột", h=760)
    y = mobile_nav(p, role="learner", ox=ox)
    p.text(ox + 16, y + 20, 343, 50, "BẢNG GÓI DỊCH VỤ & KÍCH HOẠT MÃ", size=18, bold=True, align="center")
    for i, t in enumerate(("Gói Premium", "Thẻ nạp Point")):
        p.card(ox + 16, y + 90 + i * 300, 343, 280)
        p.text(ox + 32, y + 104 + i * 300, 300, 26, t, size=17, bold=True)
        p.lines(ox + 32, y + 140 + i * 300, 300, 3)
        p.rect(ox + 32, y + 200 + i * 300, 311, 70, fill=SOFT, stroke=LINE, r=8, value="gói / thẻ (xếp dọc)", size=12)
        p.btn(ox + 32, y + 284 + i * 300, 311, 40, "Kích hoạt" if i == 0 else "Nạp Key", size=13)
    return p


def key_activation(f):
    p = screen(f, "P-10c", "Populated", "Key Activation Dialog", h=820)
    navbar(p, role="learner", active="Trang chủ")
    p.overlay(DESKTOP_W, 820)
    x, y, w = 470, 150, 500
    p.card(x, y, w, 470, r=18)
    p.text(x + 32, y + 28, 400, 30, "Kích hoạt Gói / Nạp Key", size=22, bold=True)
    p.text(x + w - 50, y + 28, 24, 24, "✕", size=18, color=MUTED, align="center")
    p.zone(x - 13, y + 28, "A")
    p.rect(x + 32, y + 76, w - 64, 60, fill=SOFT, stroke=LINE, r=8, value="<b>Premium 30 Ngày</b> — selected product", size=14, align="left")
    p.zone(x - 13, y + 160, "B")
    p.field(x + 32, y + 160, w - 64, "Mã Activation Key", "IELTS-XXXX-XXXX")
    p.text(x + 32, y + 246, w - 64, 36, "Nhập đầy đủ mã được cấp, bao gồm tiền tố …", size=13, color=MUTED)
    p.zone(x - 13, y + 300, "C")
    p.btn(x + 32, y + 300, w - 64, 50, "Kích hoạt bằng Mã Key")
    p.rect(x + 32, y + 366, w - 64, 70, fill=SOFT, stroke=DARK, r=8,
           value="Guest: “Đăng nhập tài khoản của bạn để kích hoạt gói hoặc nạp điểm.” + “Đăng nhập” button (→ P-01)<br>Demo session: “Phiên đăng nhập demo chưa hỗ trợ kích hoạt Key…”",
           size=12, align="left")
    return p


def build_home(new):
    f = new("P-10_Home")
    home_desktop(f, "guest")
    home_desktop(f, "learner")
    home_mobile(f)
    f.save()
    f = new("P-10c_KeyActivation")
    key_activation(f)
    f.save()
