"""Wireframes: course-based learning path (P-30, P-30a, P-30b, P-31…P-36).

Copy is taken from frontend/src/features/learning-path. Course / topic names are sample data.
"""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, CANVAS, DARK, MEDIA
from chrome import (navbar, mobile_nav, footer, screen, mobile_screen, phone, eyebrow, back_link, notice,
                    DESKTOP_W, MOBILE_W)

X0, CW = 100, 1240  # .lp-shell max-width 1240


def learn_page(f, pid, frame, title, h, child=None, gate=False):
    p = screen(f, pid, frame, title, h=h)
    y = navbar(p, role="learner", active="Khóa học", child=child)
    if gate:
        p.rect(X0, y + 16, CW, 64, fill=SOFT, stroke=DARK, r=12, sw=2,
               value="🛡 <b>Cần ôn lại trước khi học tiếp</b> — Bắt đầu với “Nhận diện paraphrase” để mở bài kế tiếp.",
               align="left", size=14)
        p.btn(X0 + CW - 170, y + 30, 150, 36, "Làm bài ôn →", size=13)
        p.text(X0 + CW + 6, y + 30, 90, 36, "ReviewGate<br>Banner", size=10, color=MUTED)
        y += 80
    footer(p, h - 64)
    return p, y + 28


def spotlight(p, x, y, w, label="Đang học dở", cta="Học tiếp →", progress=True):
    p.card(x, y, w, 210, fill=PRIMARY_SOFT, stroke=PRIMARY, r=20)
    p.rect(x + 28, y + 40, 120, 130, fill="#FFFFFF", stroke=PRIMARY, r=12, value="Band<br><b><font style='font-size:34px'>6.0</font></b>", size=14, color=PRIMARY)
    p.text(x + 176, y + 30, 300, 20, label.upper(), size=12, color=PRIMARY, bold=True)
    p.text(x + 176, y + 54, 700, 34, "Khóa IELTS Band 6.0 <font color='#6B7280' style='font-size:13px'>(sample data)</font>", size=24, bold=True)
    p.text(x + 176, y + 92, 760, 22, "8 chặng topic · đầu vào khuyến nghị ~5.0 · kết thúc bằng bài thi cuối khóa", size=14, color=MUTED)
    if progress:
        p.progress(x + 176, y + 128, 420, 37)
        p.text(x + 610, y + 120, 120, 24, "3/8 chặng", size=13, color=MUTED)
    p.btn(x + 176, y + 152, 170, 44, cta)
    p.img(x + w - 230, y + 20, 200, 170, "Mascot")


def course_card(p, x, y, w, band, chip, title, meta, prog, cta):
    p.card(x, y, w, 230, r=16)
    p.rect(x + 20, y + 20, 92, 34, fill=PRIMARY_SOFT, stroke=PRIMARY_SOFT, r=17, value=f"<b>Band {band}</b>", size=13, color=PRIMARY)
    if chip:
        p.chip(x + w - 150, y + 25, chip, "active" if chip != "Đã hoàn thành" else "dark")
    p.text(x + 20, y + 70, w - 40, 28, title, size=18, bold=True)
    p.text(x + 20, y + 102, w - 40, 22, meta, size=13, color=MUTED)
    if prog:
        p.progress(x + 20, y + 144, w - 130, prog[0])
        p.text(x + w - 100, y + 136, 90, 24, prog[1], size=12, color=MUTED)
    else:
        p.text(x + 20, y + 134, 200, 24, "Chưa bắt đầu", size=13, color=MUTED, italic=True)
    p.text(x + 20, y + 182, w - 40, 26, f"<b>{cta} →</b>", size=14, color=PRIMARY)


# ------------------------------------------------------------------- P-30 Course List

def p30_head(p, y):
    p.zone(X0 - 36, y, "A")
    eyebrow(p, X0, y, "Khóa học")
    p.text(X0, y + 22, 900, 40, "Lộ trình IELTS theo band mục tiêu", size=32, bold=True)
    p.text(X0, y + 66, 1000, 24, "Mỗi khóa gồm các chặng topic ngắn. Học xong các bài trong chặng và đạt bài kiểm tra chặng để mở chặng kế tiếp.", size=15, color=MUTED)
    return y + 110


def p30_section_head(p, y, counts=True):
    p.zone(X0 - 36, y, "C")
    p.text(X0, y, 400, 32, "Tất cả khóa học", size=22, bold=True)
    if counts:
        x = X0 + CW - 560
        for i, t in enumerate(("Tất cả 4", "Gợi ý cho bạn 1", "Đang học 1", "Đã hoàn thành 1")):
            w = len(t) * 8 + 30
            p.rect(x, y, w, 34, fill=PRIMARY if i == 0 else "#FFFFFF", stroke=PRIMARY if i == 0 else LINE, r=17,
                   value=t, size=13, color="#FFFFFF" if i == 0 else INK, bold=i == 0)
            x += w + 8
    return y + 52


def p30(f):
    h = 1260
    p, y = learn_page(f, "P-30", "Populated", "Course List", h, child="Khóa học")
    y = p30_head(p, y)
    p.zone(X0 - 36, y, "B")
    spotlight(p, X0, y, CW)
    y = p30_section_head(p, y + 240)
    cw3 = (CW - 40) // 3
    cards = [("5.0", "Đã hoàn thành", "Khóa IELTS Band 5.0", "6 chặng topic · đầu vào ~4.0", (100, "6/6 chặng"), "Xem lại lộ trình"),
             ("6.0", "Đang học", "Khóa IELTS Band 6.0", "8 chặng topic · đầu vào ~5.0", (37, "3/8 chặng"), "Học tiếp"),
             ("6.5", "Gợi ý cho bạn", "Khóa IELTS Band 6.5", "8 chặng topic · đầu vào ~5.5", None, "Bắt đầu học"),
             ("7.0", None, "Khóa IELTS Band 7.0", "10 chặng topic · đầu vào ~6.0", None, "Bắt đầu học")]
    for i, c in enumerate(cards):
        course_card(p, X0 + (i % 3) * (cw3 + 20), y + (i // 3) * 250, cw3, *c)
    px, py = X0 + cw3 + 20, y + 250
    p.card(px, py, cw3, 230, r=16, dashed=True, stroke=DARK)
    p.text(px + 20, py + 20, 40, 40, "🎓", size=24)
    p.text(px + 20, py + 66, cw3 - 40, 28, "Chưa biết nên chọn band nào?", size=18, bold=True)
    p.text(px + 20, py + 98, cw3 - 40, 50, "Làm bài test đầu vào 4 kỹ năng miễn phí trong 15–20 phút để được gợi ý khóa phù hợp.", size=13, color=MUTED)
    p.text(px + 20, py + 182, cw3 - 40, 26, "<b>Làm test đầu vào →</b>", size=14, color=PRIMARY)
    p.zone(px - 13, py + 10, "D")
    p.text(px + cw3 + 30, py + 60, 360, 90, "PlacementCard is shown only on the “Tất cả” tab. ⚠ It currently links to /placement (no route → P-90). See Appendix B.", size=12, color=INK, italic=True)
    return p


def p30_states(f):
    # Loading
    p, y = learn_page(f, "P-30", "Loading", "Course List", 900, child="Khóa học")
    y = p30_head(p, y)
    p.card(X0, y, CW, 200, fill="#FFFFFF", r=20)
    p.rect(X0 + 28, y + 36, 120, 130, fill=MEDIA, stroke=MEDIA, r=12)
    p.lines(X0 + 176, y + 40, 600, 4, gap=30, h=14)
    p.rect(X0 + CW - 200, y + 140, 170, 40, fill=MEDIA, stroke=MEDIA, r=10)
    y = p30_section_head(p, y + 230, counts=False)
    cw3 = (CW - 40) // 3
    for i in range(3):
        p.card(X0 + i * (cw3 + 20), y, cw3, 200, r=16)
        p.lines(X0 + i * (cw3 + 20) + 20, y + 24, cw3 - 40, 5, gap=30, h=14)
    p.text(X0, y + 220, CW, 22, "CourseCatalogSkeleton · aria-busy=true · “Đang tải danh sách khóa học…”", size=13, color=MUTED, align="center", italic=True)
    # Empty (filter)
    p, y = learn_page(f, "P-30", "Empty", "Course List", 820, child="Khóa học")
    y = p30_head(p, y)
    y = p30_section_head(p, y)
    p.card(X0 + 320, y + 20, 600, 300, r=18, dashed=True, stroke=DARK)
    p.text(X0 + 320, y + 50, 600, 50, "📄", size=34, align="center")
    p.text(X0 + 320, y + 110, 600, 30, "Không tìm thấy khóa học", size=22, bold=True, align="center")
    p.text(X0 + 320, y + 146, 600, 24, "Không có khóa học nào trong nhóm này.", size=15, color=MUTED, align="center")
    p.btn(X0 + 500, y + 200, 240, 48, "Xem tất cả khóa học")
    # Error
    p, y = learn_page(f, "P-30", "Error", "Course List", 820, child="Khóa học")
    y = p30_head(p, y)
    y = p30_section_head(p, y, counts=False)
    error_panel(p, X0 + 320, y + 20)
    p.text(X0, y + 360, CW, 44, "PLACEMENT_REQUIRED does not show this panel: ApiErrorState redirects to /learn/placement (P-30a) with the notice “Hãy làm bài kiểm tra đầu vào để chọn course học.”",
           size=13, color=INK, align="center", italic=True)


def error_panel(p, x, y, w=600):
    p.card(x, y, w, 290, r=18, stroke=DARK, sw=2)
    p.text(x, y + 30, w, 40, "⚠", size=30, align="center")
    p.text(x, y + 84, w, 30, "Chưa tải được nội dung", size=22, bold=True, align="center")
    p.text(x + 30, y + 122, w - 60, 44, "Máy chủ đang gặp sự cố hoặc mất kết nối. Hãy thử lại.", size=15, color=MUTED, align="center")
    p.btn(x + w / 2 - 90, y + 196, 180, 48, "↻ Thử lại", "secondary")


def p30_mobile(f):
    p = mobile_screen(f, "P-30", "Course List", 2, h=900)
    ox = phone(p, 0, "Populated · 375", h=900)
    y = mobile_nav(p, ox=ox)
    p.rect(ox, y, MOBILE_W, 40, fill=PRIMARY_SOFT, stroke=LINE, r=0, value="<u><b>Khóa học</b></u> · Test đầu vào… (subnav cuộn ngang)", size=11)
    y += 56
    eyebrow(p, ox + 16, y, "Khóa học", 300)
    p.text(ox + 16, y + 20, 343, 64, "Lộ trình IELTS theo band mục tiêu", size=22, bold=True, valign="top")
    p.card(ox + 16, y + 92, 343, 240, fill=PRIMARY_SOFT, stroke=PRIMARY, r=16)
    p.rect(ox + 32, y + 108, 80, 80, fill="#FFFFFF", stroke=PRIMARY, r=10, value="Band<br><b>6.0</b>", size=13, color=PRIMARY)
    p.text(ox + 124, y + 112, 220, 70, "<b>Khóa IELTS Band 6.0</b><br>8 chặng · ~5.0", size=14, valign="top")
    p.progress(ox + 32, y + 210, 230, 37)
    p.btn(ox + 32, y + 260, 311, 44, "Học tiếp →")
    p.text(ox + 16, y + 350, 343, 28, "Tất cả khóa học", size=18, bold=True)
    p.rect(ox + 16, y + 384, 343, 34, fill="#FFFFFF", stroke=LINE, r=17, value="Tất cả 4 · Gợi ý 1 · Đang học 1 → (cuộn ngang)", size=11)
    for i in range(2):
        p.card(ox + 16, y + 432 + i * 170, 343, 154, r=14)
        p.text(ox + 32, y + 446 + i * 170, 300, 24, f"<b>Band {['5.0', '6.5'][i]}</b> · Khóa IELTS Band {['5.0', '6.5'][i]}", size=14)
        p.lines(ox + 32, y + 482 + i * 170, 300, 2)
        p.text(ox + 32, y + 540 + i * 170, 300, 24, f"<b>{['Xem lại lộ trình', 'Bắt đầu học'][i]} →</b>", size=13, color=PRIMARY)
    ox = phone(p, 1, "Menu mở (☰) · ≤1240px", h=900)
    y = mobile_nav(p, ox=ox)
    p.rect(ox, y, MOBILE_W, 330, fill=PRIMARY, stroke=PRIMARY, r=0)
    for i, t in enumerate(("Trang chủ", "Khóa học", "Luyện tập 4 kỹ năng", "Sổ từ vựng", "Kết quả học viên")):
        p.rect(ox + 16, y + 16 + i * 60, 343, 48, fill="#FFFFFF" if i == 1 else PRIMARY, stroke="#FFFFFF", r=10,
               value=t, color=PRIMARY if i == 1 else "#FFFFFF", size=14, bold=True)
    p.text(ox + 16, y + 346, 343, 60, "site-nav-links.is-open: lưới link 48px; Esc hoặc chọn link để đóng.", size=12, color=MUTED, italic=True)


# ------------------------------------------------------------------- P-30a Placement

def stepper(p, y, current, fill=0.5):
    x0, w = X0 + 220, 800
    labels = ["Khảo sát", "Bài test", "Kết quả"]
    for i, t in enumerate(labels):
        cx = x0 + i * (w // 2)
        state = "done" if i < current else "current" if i == current else "todo"
        p.rect(cx - 20, y, 40, 40, fill=PRIMARY if state != "todo" else "#FFFFFF", stroke=PRIMARY if state != "todo" else LINE,
               r=20, value="✓" if state == "done" else str(i + 1), color="#FFFFFF" if state != "todo" else MUTED, bold=True, size=15)
        p.text(cx - 80, y + 46, 160, 22, t, size=14, bold=state == "current", align="center", color=INK if state != "todo" else MUTED)
        if i < 2:
            seg_fill = 1 if i < current else fill if i == current else 0
            p.rect(cx + 30, y + 17, w // 2 - 60, 6, fill=MEDIA, stroke=MEDIA, r=3)
            if seg_fill:
                p.rect(cx + 30, y + 17, int((w // 2 - 60) * seg_fill), 6, fill=PRIMARY, stroke=PRIMARY, r=3)
    p.zone(X0 - 36, y + 6, "A")
    return y + 96


def placement_page(f, frame, h, step, fill=0.5):
    p, y = learn_page(f, "P-30a", frame, "Placement Test", h, child="Test đầu vào 4 kỹ năng FREE")
    return p, stepper(p, y, step, fill)


def p30a(f):
    # Survey
    p, y = placement_page(f, "Survey", 860, 0, 0.33)
    p.zone(X0 - 36, y, "B")
    p.img(X0 + 220, y, 90, 90, "Mascot")
    p.rect(X0 + 330, y + 14, 520, 60, fill=PRIMARY_SOFT, stroke=PRIMARY, r=16, value="<b>Bạn có thể dành bao nhiêu thời gian học mỗi ngày?</b>", size=16, align="left")
    p.text(X0 + 870, y + 30, 360, 40, "Question 2/3 (question 1: exam month — chips Tháng m/yyyy · Thời gian khác 📅 · Chưa có kế hoạch)", size=12, color=MUTED, italic=True)
    for i, t in enumerate(("Dưới 1 tiếng", "Khoảng 1 – 2 tiếng", "Khoảng 2 – 3 tiếng", "Trên 3 tiếng")):
        p.rect(X0 + 330, y + 110 + i * 64, 520, 52, fill=PRIMARY_SOFT if i == 1 else "#FFFFFF",
               stroke=PRIMARY if i == 1 else DARK, r=12, value=t, size=15, align="left", sw=2 if i == 1 else 1)
    p.text(X0 + 330, y + 380, 200, 24, "← Câu trước", size=14, color=PRIMARY, bold=True)
    p.text(X0 + 870, y + 110, 360, 120, "Question 3/3: “Mục tiêu điểm IELTS của bạn là?” — Dưới IELTS 5.5 · 6.0 · 6.5 · 7.0 · 7.5 · 8.0 trở lên. Picking a band saves the goal (FT-09) and opens the summary.", size=12, color=MUTED, italic=True)
    # Summary modal
    p, y = placement_page(f, "Survey Summary", 860, 0, 1)
    p.overlay(DESKTOP_W, 860)
    x, w = 470, 500
    p.card(x, 150, w, 520, r=20)
    p.zone(x - 13, 170, "B")
    p.text(x, 176, w, 40, "🎉", size=30, align="center")
    p.text(x + 30, 220, w - 60, 30, "Bạn đã hoàn thành khảo sát!", size=22, bold=True, align="center")
    p.text(x + 40, 256, w - 80, 44, "Mục tiêu này giúp hệ thống gợi ý lộ trình phù hợp sau bài kiểm tra đầu vào.", size=14, color=MUTED, align="center")
    p.text(x + 40, 310, w - 80, 24, "📅 12/2026      🕒 ~11h/tuần", size=15, align="center", bold=True)
    for i, b in enumerate(("≤5.5", "6.0", "6.5", "7.0", "7.5", "≥8.0")):
        bh = 40 + i * 22
        tone = PRIMARY if b == "6.5" else (DARK if i < 2 else MEDIA)
        p.rect(x + 60 + i * 64, 470 - bh, 40, bh, fill=tone, stroke=tone, r=4)
        p.text(x + 50 + i * 64, 476, 60, 20, b, size=12, align="center")
    p.text(x + 170, 330, 160, 20, "Band mục tiêu ↓", size=12, color=PRIMARY, bold=True, align="center")
    p.zone(x - 13, 590, "C")
    p.btn(x + 40, 590, w - 80, 52, "Tiếp tục")
    p.text(x + 40, 646, w - 80, 20, "A goal-save failure only shows a hint; the test can continue.", size=11, color=MUTED, italic=True, align="center")
    # Test hub
    p, y = placement_page(f, "Test Hub", 940, 1, 0.5)
    p.zone(X0 - 36, y, "B")
    p.card(X0, y, 400, 440, r=18)
    p.text(X0 + 24, y + 20, 360, 28, "Bài kiểm tra đầu vào", size=20, bold=True)
    p.text(X0 + 24, y + 70, 360, 22, "Bạn đã bắt đầu được", size=14, color=MUTED, align="center")
    for i, (v, c) in enumerate((("00", "Giờ"), ("12", "Phút"), ("34", "Giây"))):
        p.rect(X0 + 44 + i * 110, y + 104, 90, 80, fill=SOFT, stroke=LINE, r=12, value=f"<b><font style='font-size:30px'>{v}</font></b><br>{c}", size=12)
    p.text(X0 + 24, y + 220, 352, 120, "Không giới hạn thời gian. Câu trả lời được lưu trên hệ thống nên có thể thoát và quay lại làm tiếp; phần nào đã nộp thì không mở lại được.", size=13, color=MUTED, valign="top")
    p.zone(X0 + 420 - 13, y, "C")
    px, pw = X0 + 430, CW - 430
    p.card(px, y, pw, 520, r=18)
    p.text(px + 24, y + 20, 400, 28, "Phần thi của bạn", size=20, bold=True)
    p.text(px + pw - 220, y + 22, 200, 24, "1/5 đã hoàn thành", size=14, color=MUTED, align="right")
    rows = [("✓", "Listening", "18 phút", "Làm bài", True), ("📖", "Reading", "13 câu", "Làm tiếp", False),
            ("✎", "Writing Task 1", "1 câu", "Làm bài", False), ("✎", "Writing Task 2", "1 câu", "Làm bài", False),
            ("🎤", "Speaking", "6 câu", "Làm bài", False)]
    for i, (ic, name, qty, cta, done) in enumerate(rows):
        ry = y + 70 + i * 74
        p.rect(px + 20, ry, pw - 40, 62, fill=SOFT if done else "#FFFFFF", stroke=LINE, r=12)
        p.text(px + 36, ry + 16, 30, 30, ic, size=18, align="center")
        p.text(px + 80, ry + 16, 300, 30, f"<b>{name}</b>", size=16)
        p.text(px + pw - 330, ry + 16, 130, 30, qty, size=14, color=MUTED, align="right")
        p.btn(px + pw - 170, ry + 12, 130, 38, cta, "disabled" if done else "primary", size=13)
    p.text(px + 24, y + 450, pw - 48, 44, "Làm lần lượt từng phần. Bài được nộp tự động khi bạn hoàn thành phần cuối cùng.", size=14, color=MUTED)
    p.zone(X0 - 36, y + 560, "D")
    p.btn(X0, y + 560, 220, 44, "↺ Làm lại khảo sát", "secondary", size=14)
    # Exam shells
    exam_objective(f)
    exam_writing(f)
    exam_speaking(f)
    # Grading
    p, y = placement_page(f, "Grading", 700, 2, 0)
    p.card(X0 + 320, y + 20, 600, 280, r=18)
    p.text(X0 + 320, y + 50, 600, 40, "⟳", size=30, align="center")
    p.text(X0 + 320, y + 100, 600, 30, "Đang chấm bài của bạn…", size=22, bold=True, align="center")
    p.text(X0 + 360, y + 140, 520, 44, "Bài luận được chấm tự động nên có thể mất tới một phút. Đừng đóng trang này.", size=15, color=MUTED, align="center")
    p.text(X0 + 340, y + 200, 560, 80, "After 45 polls (≈90 s): “Kết quả đang được chấm” + “Kiểm tra lại” · Other errors: “Chưa lấy được kết quả” + “Kiểm tra lại”", size=12, color=INK, align="center", italic=True)
    p30a_result(f)
    p30a_mobile(f)


def exam_frame(f, frame, h=860, toolbar=None):
    p = screen(f, "P-30a", frame, "Placement Test", h=h, fill="#FFFFFF",
               sub="Desktop 1440px · PlacementExamShell (role=dialog, full screen, over the navbar) · draw.io frame 'P-30a / " + frame + "'")
    p.rect(0, 0, DESKTOP_W, 72, fill=PRIMARY, stroke=PRIMARY, r=0)
    p.rect(30, 14, 140, 44, fill=PRIMARY, stroke="#FFFFFF", r=6, value="Logo", color="#FFFFFF", size=13, bold=True)
    p.text(200, 12, 600, 26, f"<b>Bài kiểm tra đầu vào · {frame.split('—')[-1].strip().split(' /')[0]}</b>", size=17, color="#FFFFFF")
    p.text(200, 38, 600, 22, "Đã làm 04:12 · không giới hạn thời gian", size=13, color="#FFFFFF")
    if toolbar:
        p.rect(820, 16, 480, 40, fill=PRIMARY, stroke="#FFFFFF", r=20, value=toolbar, color="#FFFFFF", size=13)
    p.rect(1370, 18, 36, 36, fill=PRIMARY, stroke="#FFFFFF", r=18, value="✕", color="#FFFFFF", size=15)
    p.text(1250, 74, 180, 18, "✕ = “Lưu và quay lại”", size=11, color=MUTED, align="right")
    p.zone(10, 22, "A")
    p.rect(0, 72, DESKTOP_W, 50, fill=SOFT, stroke=LINE, r=0)
    return p


def exam_objective(f):
    p = exam_frame(f, "Exam — Reading / Listening", toolbar="▶  ━━━●━━━  02:10 / 06:30   🔊  1x (Listening)")
    p.text(30, 72, 1200, 50, "<b>Part 2</b>   Read the text and answer questions 1–13", size=15)
    p.zone(10, 140, "B")
    p.card(30, 140, 680, 590, r=10)
    p.text(50, 156, 600, 30, "<b>Passage title</b>", size=20)
    for i, L in enumerate("ABCDE"):
        p.text(50, 200 + i * 104, 30, 24, f"<b>{L}</b>", size=15)
        p.lines(84, 206 + i * 104, 600, 4, gap=20)
    p.rect(716, 140, 8, 590, fill=MEDIA, stroke=MEDIA, r=4)
    p.text(700, 420, 40, 30, "⇔", size=16, align="center")
    p.zone(730, 140, "C")
    p.card(740, 140, 670, 590, r=10)
    for i in range(4):
        qy = 160 + i * 140
        p.text(760, qy, 600, 24, f"<b>{i + 1}.</b> Question prompt …", size=15)
        p.rect(760, qy + 34, 620, 40, fill="#FFFFFF", stroke=DARK, r=8, value="answer input / select", color=MUTED, size=13, align="left")
        p.text(760, qy + 80, 300, 20, "⚑ flag · ⚠ “Chưa lưu được, sẽ thử lại khi nộp phần này”", size=11, color=MUTED)
    p.rect(1300, 680, 46, 40, fill="#FFFFFF", stroke=DARK, r=8, value="←", size=16)
    p.rect(1354, 680, 46, 40, fill="#FFFFFF", stroke=DARK, r=8, value="→", size=16)
    p.zone(10, 760, "D")
    p.rect(0, 750, DESKTOP_W, 110, fill=SOFT, stroke=LINE, r=0)
    for i in range(13):
        p.rect(60 + i * 52, 780, 42, 42, fill=PRIMARY if i < 5 else "#FFFFFF", stroke=PRIMARY if i < 5 else DARK, r=8,
               value=str(i + 1), color="#FFFFFF" if i < 5 else INK, size=14, bold=True)
    p.text(760, 780, 120, 42, "5 of 13", size=15, bold=True)
    p.rect(1330, 774, 56, 56, fill=PRIMARY, stroke=PRIMARY, r=28, value="✓", color="#FFFFFF", size=22)
    p.text(1110, 836, 290, 20, "✓ Nộp phần này → confirmation dialog", size=11, color=MUTED, align="right")
    # confirm dialog inset
    p.card(960, 390, 420, 200, r=14, stroke=DARK, sw=2)
    p.text(980, 404, 380, 26, "<b>Nộp phần Reading?</b>", size=17)
    p.text(980, 436, 380, 60, "Bạn còn 8 câu chưa trả lời. Sau khi nộp, phần này không mở lại được.", size=13, color=MUTED, valign="top")
    p.btn(980, 520, 160, 42, "Làm tiếp", "secondary", size=13)
    p.btn(1156, 520, 200, 42, "✓ Nộp phần này", size=13)
    p.text(960, 594, 420, 20, "(confirmation dialog — opens on ✓)", size=11, color=MUTED, italic=True, align="center")


def exam_writing(f):
    p = exam_frame(f, "Exam — Writing")
    p.text(30, 72, 1200, 50, "<b>Part 3</b>   (section instructions)", size=15)
    p.zone(10, 140, "B")
    p.card(30, 140, 470, 700, r=10)
    p.text(50, 156, 430, 60, "<b>Task prompt</b> — Writing Task 1 (with data table / chart when present)", size=14, valign="top")
    p.img(50, 230, 430, 260, "Task data (table / chart)")
    p.zone(510, 140, "C")
    p.card(520, 140, 560, 700, r=10)
    p.text(540, 152, 520, 26, "✎ Word count: <b>86</b>/150", size=14, align="right")
    for i, (lab, hh) in enumerate((("Mở bài", 90), ("Thân bài", 150), ("Kết bài", 90))):
        yy = 190 + sum(x for x in (0, 130, 320)[: i + 1])
        p.text(540, yy, 300, 22, f"<b>{lab}</b>", size=14)
        p.rect(540, yy + 26, 520, hh, fill="#FFFFFF", stroke=DARK, r=8, value="Nhập phần viết của bạn ở đây", color=MUTED, size=13, align="left", valign="top")
    p.text(540, 744, 220, 30, "Thời gian: <b>00:04:12</b>", size=14)
    p.btn(900, 738, 160, 44, "✓ Hoàn thành")
    p.text(540, 790, 520, 40, "Dialog “Nộp phần Writing?” — an empty essay scores 0; “Viết tiếp” / submit", size=12, color=MUTED, italic=True)
    p.zone(1090, 140, "D")
    p.card(1100, 140, 310, 700, r=10)
    p.text(1120, 156, 270, 26, "📘 <b>Tra từ vựng</b>", size=15)
    p.rect(1120, 196, 130, 34, fill=PRIMARY_SOFT, stroke=PRIMARY, r=17, value="Tra Việt-Anh", size=12, color=PRIMARY, bold=True)
    p.rect(1258, 196, 132, 34, fill="#FFFFFF", stroke=LINE, r=17, value="Tra Anh-Việt", size=12)
    p.text(1120, 250, 270, 90, "Nhập từ cần tra bên dưới. Kết quả mở ở tab mới nên bài viết của bạn không bị gián đoạn.", size=13, color=MUTED, valign="top")
    p.rect(1120, 720, 270, 40, fill="#FFFFFF", stroke=DARK, r=8, value="Nhập cụm Tiếng Việt cần tra", color=MUTED, size=12, align="left")
    p.btn(1120, 770, 270, 40, "Tra từ vựng", size=13)


def exam_speaking(f):
    p = exam_frame(f, "Exam — Speaking", h=760)
    p.text(30, 72, 1200, 50, "<b>Part 5</b>   (section instructions)", size=15)
    for i, (t, body, cta) in enumerate((("Hướng dẫn chung", "• Kiểm tra loa/tai nghe và micro đã kết nối.<br>• Làm bài ở nơi yên tĩnh.<br>• Mỗi câu ghi âm tối đa N giây.<br><i>Phần Speaking hiện chưa được chấm riêng; bản ghi chỉ lưu trên trình duyệt của bạn.</i>", "Tiếp tục →"),
                                       ("Kiểm tra micro", "• Trình duyệt sẽ hỏi quyền dùng micro; hãy bấm cho phép.<br>• Nghe lại bản ghi để chắc chắn giọng của bạn rõ ràng.<br>[● Ghi thử] [■ Dừng] ▶ audio", "Bắt đầu"),
                                       ("Câu hỏi 1/6", "Question prompt …<br>🎤 ● REC 00:12<br>“Ghi âm sẽ bắt đầu sau câu hỏi”", "Dừng & sang câu tiếp"))):
        x = 60 + i * 450
        p.zone(x - 14, 150, "BCD"[i])
        p.card(x, 150, 420, 480, r=14)
        p.text(x + 24, 170, 370, 30, f"<b>{t}</b>", size=19)
        p.text(x + 24, 214, 370, 280, body, size=14, color=INK, valign="top")
        p.btn(x + 24, 560, 370, 48, cta)
    p.text(60, 650, 1300, 40, "Three sequential steps in one frame: instructions → microphone check → each question (recording starts automatically, “Dừng & hoàn thành” on the last one). Save error: “Chưa lưu được bản ghi. Kiểm tra kết nối rồi ghi lại câu này.”", size=13, color=MUTED, italic=True)


def p30a_result(f):
    h = 1540
    p, y = placement_page(f, "Result Report", h, 2, 1)
    p.zone(X0 - 36, y, "B")
    p.text(X0, y, 600, 20, "IELTS Academic · 09/10/2026", size=13, color=MUTED)
    p.text(X0, y + 24, 800, 40, "Báo cáo kết quả bài test", size=30, bold=True)
    p.text(X0, y + 66, 600, 24, "<b>Tên học viên</b>", size=16)
    p.text(X0, y + 94, 1000, 24, "Dưới đây là phân tích trình độ dựa trên bài test đầu vào: điểm ước lượng, đánh giá khả năng đạt mục tiêu và nhận xét chi tiết từng kỹ năng.", size=14, color=MUTED)
    y += 140
    for i, (t, big, rows) in enumerate((("Estimated overall band score", "5.5", ["Reading 6.0", "Listening 5.5", "Writing 5.0", "Speaking 5.5"]),
                                         ("Target band", "6.5", ["Số ngày còn lại · 83 ngày", "Thời gian học · 11 giờ/tuần"]))):
        x = X0 + i * 660
        p.card(x, y, 580, 250, r=18)
        p.text(x + 24, y + 18, 530, 24, f"<b>{t}</b>", size=16)
        p.text(x + 24, y + 50, 200, 70, f"<b>{big}</b>", size=52, color=PRIMARY if i == 0 else INK)
        for j, r in enumerate(rows):
            p.text(x + 260, y + 50 + j * 40, 300, 30, r, size=14)
    p.text(X0 + 590, y + 110, 60, 30, "»", size=28, align="center")
    y += 280
    p.zone(X0 - 36, y, "C")
    p.text(X0, y, 400, 30, "Đánh giá khả thi", size=22, bold=True)
    for i in range(8):
        st = DARK if i == 2 else PRIMARY if i == 5 else MEDIA
        p.rect(X0 + 20 + i * 70, y + 200 - (40 + i * 18), 44, 40 + i * 18, fill=st, stroke=st, r=4)
    p.text(X0 + 120, y + 50, 140, 20, "Bạn ở đây", size=12, bold=True, align="center")
    p.text(X0 + 330, y + 40, 140, 20, "Band mục tiêu", size=12, bold=True, color=PRIMARY, align="center")
    p.text(X0 + 640, y + 50, 600, 70, "Feasibility text (based on band gap and study time).", size=15, valign="top")
    p.rect(X0 + 640, y + 130, 600, 60, fill=SOFT, stroke=LINE, r=10, value="💡 <b>Gợi ý:</b> tip for the feasibility level", size=14, align="left")
    y += 240
    p.zone(X0 - 36, y, "D")
    p.text(X0, y, 400, 30, "Phân tích chi tiết", size=22, bold=True)
    for i, t in enumerate(("Reading — correct/incorrect table", "Listening — correct/incorrect table", "Writing Task 1/2 — band + AI feedback", "Speaking — band")):
        p.card(X0 + (i % 2) * 630, y + 44 + (i // 2) * 160, 610, 140, r=14)
        p.text(X0 + 20 + (i % 2) * 630, y + 58 + (i // 2) * 160, 560, 24, f"<b>{t}</b>", size=15)
        p.lines(X0 + 20 + (i % 2) * 630, y + 96 + (i // 2) * 160, 560, 3)
    y += 380
    p.zone(X0 - 36, y, "E")
    p.text(X0, y, 400, 30, "Lộ trình khóa học", size=22, bold=True)
    cw = (CW - 60) // 4
    for i, (badge, band, cta) in enumerate((("Ôn nền tảng", "5.0", "Xem khóa học"), ("Đề xuất cho bạn", "6.0", "Bắt đầu học"),
                                             ("Nâng cao", "6.5", "Xem khóa học"), ("Nâng cao", "7.0", "Xem khóa học"))):
        x = X0 + i * (cw + 20)
        rec = badge.startswith("Đề xuất")
        p.card(x, y + 44, cw, 200, r=14, stroke=PRIMARY if rec else LINE, sw=2 if rec else 1)
        p.chip(x + 16, y + 60, badge, "solid" if rec else "neutral")
        p.text(x + 16, y + 94, cw - 32, 24, f"<b>Khóa IELTS Band {band}</b>", size=15)
        p.text(x + 16, y + 122, cw - 32, 20, f"Band {band} · 8 topic", size=13, color=MUTED)
        p.btn(x + 16, y + 180, cw - 32, 42, cta + " →", "primary" if rec else "secondary", size=13)


def p30a_mobile(f):
    p = mobile_screen(f, "P-30a", "Placement Test", 3, h=820)
    ox = phone(p, 0, "Survey · 375", h=820)
    y = mobile_nav(p, ox=ox)
    p.text(ox + 16, y + 20, 343, 40, "① Khảo sát ─ ② Bài test ─ ③ Kết quả", size=13, bold=True, align="center")
    p.rect(ox + 16, y + 80, 343, 60, fill=PRIMARY_SOFT, stroke=PRIMARY, r=14, value="<b>Bạn dự định thi IELTS khi nào?</b>", size=14)
    for i, t in enumerate(("Tháng 11/2026", "Tháng 12/2026", "Tháng 1/2027", "Thời gian khác 📅", "Chưa có kế hoạch")):
        p.rect(ox + 16 + (i % 2) * 175, y + 160 + (i // 2) * 52, 168, 42, fill="#FFFFFF", stroke=DARK, r=21, value=t, size=12)
    ox = phone(p, 1, "Test Hub · 375", h=820)
    y = mobile_nav(p, ox=ox)
    p.card(ox + 16, y + 20, 343, 150, r=14)
    p.text(ox + 16, y + 40, 343, 60, "<b>00 : 12 : 34</b>", size=26, align="center")
    p.text(ox + 30, y + 100, 315, 60, "no time limit note…", size=12, color=MUTED, align="center")
    for i, t in enumerate(("Listening ✓", "Reading", "Writing Task 1", "Writing Task 2", "Speaking")):
        p.rect(ox + 16, y + 190 + i * 70, 343, 60, fill="#FFFFFF", stroke=LINE, r=12, value=f"<b>{t}</b>", size=14, align="left")
        p.btn(ox + 249, y + 200 + i * 70, 96, 40, "Làm bài", "disabled" if i == 0 else "primary", size=12)
    ox = phone(p, 2, "Exam shell · 375", h=820)
    p.rect(ox, 0, MOBILE_W, 60, fill=PRIMARY, stroke=PRIMARY, r=0, value="Bài kiểm tra đầu vào · Reading   ✕", color="#FFFFFF", size=13, bold=True)
    p.card(ox + 12, 76, 351, 300, r=10)
    p.text(ox + 24, 86, 320, 24, "<b>Passage</b> (top, own scroll)", size=13)
    p.lines(ox + 24, 120, 320, 8, gap=26)
    p.card(ox + 12, 390, 351, 300, r=10)
    p.text(ox + 24, 400, 320, 24, "<b>Questions</b> (below)", size=13)
    p.rect(ox, 720, MOBILE_W, 100, fill=SOFT, stroke=LINE, r=0, value="1 2 3 4 5 … (horizontal scroll) · 5 of 13 · ✓", size=13)


# ------------------------------------------------------------------- P-30b Topic List

def overview_header(p, y, eyebrow_txt, title, lead, stats, next_lbl, next_txt, cta):
    p.zone(X0 - 36, y, "A")
    eyebrow(p, X0, y, eyebrow_txt)
    p.text(X0, y + 22, 720, 44, title, size=30, bold=True)
    p.text(X0, y + 70, 720, 48, lead, size=15, color=MUTED, valign="top")
    p.zone(X0 + 760 - 36, y, "B")
    p.card(X0 + 760, y, CW - 760, 200, r=18)
    p.text(X0 + 784, y + 20, 300, 24, stats[0], size=14)
    p.text(X0 + CW - 100, y + 20, 80, 24, f"<b>{stats[1]}%</b>", size=14, align="right")
    p.progress(X0 + 784, y + 52, CW - 808, stats[1])
    if next_lbl:
        p.text(X0 + 784, y + 76, CW - 808, 44, f"<font color='#6B7280'>{next_lbl}</font><br><b>{next_txt}</b>", size=14, valign="top")
        p.btn(X0 + 784, y + 136, 200, 46, cta)
    return y + 230


def stop(p, y, node, kicker, title, foot, state="open", premium=False, is_next=False, cta="Vào chặng"):
    p.rect(X0 + 10, y + 24, 40, 40, fill=PRIMARY if state in ("open", "passed") else "#FFFFFF",
           stroke=PRIMARY if state != "locked" else LINE, r=20, value=node, color="#FFFFFF" if state != "locked" else MUTED, size=14, bold=True)
    p.card(X0 + 70, y, CW - 70, 100, r=14, stroke=PRIMARY if is_next else LINE, sw=2 if is_next else 1,
           fill="#FFFFFF" if state != "locked" else SOFT, dashed=state == "locked")
    p.text(X0 + 94, y + 12, 500, 20, kicker.upper(), size=12, color=PRIMARY if state != "locked" else MUTED, bold=True)
    if premium:
        p.chip(X0 + CW - 130, y + 12, "👑 Premium", "neutral")
    p.text(X0 + 94, y + 34, 800, 28, f"<b>{title}</b>", size=18, color=INK if state != "locked" else MUTED)
    p.text(X0 + 94, y + 66, 700, 24, foot, size=13, color=MUTED)
    if state != "locked":
        p.text(X0 + CW - 200, y + 62, 180, 26, f"<b>{cta} →</b>", size=14, color=PRIMARY, align="right")
    return y + 116


def p30b(f):
    h = 1300
    p, y = learn_page(f, "P-30b", "Populated", "Topic List (in course)", h)
    back_link(p, X0, y, "Tất cả khóa học")
    y = overview_header(p, y + 40, "Band 6.0 · Lộ trình khóa học", "Khóa IELTS Band 6.0",
                        "Học lần lượt từng chặng. Hoàn thành các bài học và đạt bài kiểm tra chặng (từ 70%) để mở chặng tiếp theo.",
                        ("<b>2/5</b> chặng đã qua", 40), "Tiếp theo", "Chặng 3: Matching Headings", "Học tiếp →")
    p.zone(X0 - 36, y, "C")
    p.text(X0, y, 300, 30, "Các chặng", size=22, bold=True)
    x = X0 + CW - 470
    for i, t in enumerate(("Tất cả", "Listening", "Reading", "Writing", "Speaking")):
        w = len(t) * 9 + 30
        p.rect(x, y, w, 34, fill=PRIMARY if i == 0 else "#FFFFFF", stroke=PRIMARY if i == 0 else LINE, r=17, value=t,
               size=13, color="#FFFFFF" if i == 0 else INK, bold=i == 0)
        x += w + 8
    y += 56
    p.zone(X0 - 36, y + 30, "D")
    y = stop(p, y, "✓", "Chặng 01 · Reading", "Đọc hiểu nền tảng", "✔ Đã qua · 4 bài", "passed", cta="Xem lại")
    y = stop(p, y, "✓", "Chặng 02 · Reading", "Kỹ năng True / False / Not Given", "✔ Đã qua · 3 bài", "passed", cta="Xem lại")
    y = stop(p, y, "3", "Chặng 03 · Reading", "Matching Headings", "▬▬▭▭ 1/3 bài", "open", is_next=True, cta="Học tiếp")
    y = stop(p, y, "🔒", "Chặng 04 · Listening", "Form completion", "🔒 Hoàn thành chặng trước để mở", "locked")
    y = stop(p, y, "🔒", "Chặng 05 · Writing", "Task 1 — Line graph", "🔒 (same reason as above → visually hidden, kept for screen readers)", "locked", premium=True)
    p.zone(X0 - 36, y + 20, "E")
    p.rect(X0 + 10, y + 24, 40, 40, fill="#FFFFFF", stroke=DARK, r=20, value="⚑", size=16)
    p.card(X0 + 70, y, CW - 70, 120, r=14, stroke=DARK, sw=2)
    p.text(X0 + 94, y + 14, 300, 20, "VỀ ĐÍCH", size=12, color=MUTED, bold=True)
    p.text(X0 + 94, y + 36, 600, 28, "<b>Bài thi cuối khóa</b>", size=18)
    p.text(X0 + 94, y + 70, 800, 40, "Mở khi qua đủ 5 chặng (hiện 2/5). Cần đạt từ 70%.", size=14, color=MUTED)
    p.text(X0 + CW - 420, y + 30, 400, 60, "AVAILABLE → “Làm bài thi cuối khóa” button (→ P-35 ?course=)<br>PASSED → “Đã đạt” chip", size=12, color=INK, italic=True, align="right")
    return p


def p30b_states(f):
    p, y = learn_page(f, "P-30b", "Loading", "Topic List (in course)", 820)
    back_link(p, X0, y, "Tất cả khóa học")
    p.lines(X0, y + 50, 600, 3, gap=30, h=16)
    for i in range(4):
        p.rect(X0 + 10, y + 180 + i * 120, 40, 40, fill=MEDIA, stroke=MEDIA, r=20)
        p.card(X0 + 70, y + 160 + i * 120, CW - 70, 100, r=14)
        p.lines(X0 + 94, y + 176 + i * 120, 600, 3, gap=24, h=12)
    p.text(X0, y + 650, CW, 22, "TopicListSkeleton · “Đang tải lộ trình chặng học…”", size=13, color=MUTED, italic=True, align="center")
    p, y = learn_page(f, "P-30b", "Empty / Error", "Topic List (in course)", 820)
    back_link(p, X0, y, "Tất cả khóa học")
    p.text(X0, y + 50, 900, 40, "<b>Khóa IELTS Band 7.0</b>", size=28)
    p.rect(X0, y + 120, 560, 60, fill=SOFT, stroke=LINE, r=10, value="Empty: “Khóa này chưa có chặng nào.”", size=15, align="left")
    error_panel(p, X0 + 640, y + 110)
    p.text(X0, y + 440, 1240, 60, "TOPIC_LOCKED when opening a locked topic: redirect to /learn with the notice “Topic đó chưa mở. Hãy hoàn thành chặng trước.” · Course final test start errors are shown inline (role=alert): TEST_LOCKED / TEST_UNAVAILABLE.", size=13, color=INK, italic=True)


def p30b_mobile(f):
    p = mobile_screen(f, "P-30b", "Topic List (in course)", 1, h=820)
    ox = phone(p, 0, "Populated · 375", h=820)
    y = mobile_nav(p, ox=ox) + 16
    p.text(ox + 16, y, 343, 22, "← Tất cả khóa học", size=13, color=PRIMARY, bold=True)
    p.text(ox + 16, y + 30, 343, 30, "<b>Khóa IELTS Band 6.0</b>", size=20)
    p.card(ox + 16, y + 70, 343, 150, r=14)
    p.text(ox + 32, y + 84, 300, 22, "<b>2/5</b> chặng đã qua · 40%", size=13)
    p.progress(ox + 32, y + 112, 311, 40)
    p.btn(ox + 32, y + 160, 311, 44, "Học tiếp →")
    p.rect(ox + 16, y + 236, 343, 34, fill="#FFFFFF", stroke=LINE, r=17, value="Tất cả · Listening · Reading → (cuộn ngang)", size=11)
    for i, (t, st) in enumerate((("Đọc hiểu nền tảng", "✔ Đã qua"), ("Matching Headings", "1/3 bài"), ("Form completion", "🔒 Hoàn thành chặng trước để mở"))):
        p.rect(ox + 16, y + 300 + i * 110, 30, 30, fill=PRIMARY if i < 2 else "#FFFFFF", stroke=PRIMARY if i < 2 else LINE, r=15)
        p.card(ox + 56, y + 286 + i * 110, 303, 96, r=12, dashed=i == 2)
        p.text(ox + 70, y + 296 + i * 110, 280, 70, f"<b>{t}</b><br>{st}", size=13, valign="top")


# ------------------------------------------------------------------- P-31 Topic Detail

def lesson_step(p, y, n, title, meta, state, cta=None, practice=False):
    p.rect(X0 + 10, y + 16, 36, 36, fill=PRIMARY if state != "locked" else "#FFFFFF", stroke=PRIMARY if state != "locked" else LINE,
           r=18, value="✓" if state == "done" else str(n), color="#FFFFFF" if state != "locked" else MUTED, size=14, bold=True)
    p.card(X0 + 66, y, CW - 66, 68, r=12, fill="#FFFFFF" if state != "locked" else SOFT, stroke=PRIMARY if state == "next" else LINE, sw=2 if state == "next" else 1)
    p.text(X0 + 88, y + 10, 800, 24, f"<b>{title}</b>", size=16, color=INK if state != "locked" else MUTED)
    p.text(X0 + 88, y + 38, 800, 20, meta, size=12, color=MUTED)
    if cta:
        p.text(X0 + CW - 200, y + 22, 180, 24, f"<b>{cta} →</b>", size=14, color=PRIMARY, align="right")
    y += 80
    if practice:
        p.rect(X0 + 66, y - 6, CW - 66, 44, fill=SOFT, stroke=DARK, r=10,
               value="🏋 Cần luyện thêm trước khi mở bài kiểm tra chặng  —  <b><u>Luyện ngay →</u></b> (P-33)", size=13, align="left")
        y += 50
    return y


def p31(f, frame="Populated", locked=False):
    h = 1200
    p, y = learn_page(f, "P-31", frame, "Topic Detail", h, gate=locked)
    back_link(p, X0, y, "Khóa IELTS Band 6.0")
    if locked:
        y = overview_header(p, y + 40, "Chặng 03 · Reading", "Matching Headings", "Topic description …",
                            ("<b>2/3</b> bài đã xong", 67), "Cần làm trước", "Bài ôn: Nhận diện paraphrase", "Làm bài ôn →")
    else:
        y = overview_header(p, y + 40, "Chặng 03 · Reading", "Matching Headings", "Topic description …",
                            ("<b>1/3</b> bài đã xong", 33), "Tiếp theo", "Bài 2: Xác định ý chính của đoạn", "Học tiếp →")
    p.zone(X0 - 36, y, "C")
    p.text(X0, y, 400, 30, "Bài học trong chặng", size=22, bold=True)
    p.text(X0 + CW - 300, y + 4, 300, 24, "3 bài · khoảng 45 phút", size=14, color=MUTED, align="right")
    y += 50
    if locked:
        y = lesson_step(p, y, 1, "Bài 1: Đọc lướt tìm ý", "🕒 15 phút", "done", "Xem lại", practice=True)
        y = lesson_step(p, y, 2, "Bài 2: Xác định ý chính của đoạn", "🕒 15 phút", "done", "Xem lại")
        y = lesson_step(p, y, 3, "Bài 3: Luyện Matching Headings", "🕒 15 phút · 🔒 Hoàn thành bài trước", "locked")
    else:
        y = lesson_step(p, y, 1, "Bài 1: Đọc lướt tìm ý", "🕒 15 phút", "done", "Xem lại")
        y = lesson_step(p, y, 2, "Bài 2: Xác định ý chính của đoạn", "🕒 15 phút", "next", "Học tiếp")
        y = lesson_step(p, y, 3, "Bài 3: Luyện Matching Headings", "🕒 15 phút · 🔒 Hoàn thành bài trước", "locked")
    p.zone(X0 - 36, y + 10, "D")
    p.rect(X0 + 10, y + 16, 36, 36, fill="#FFFFFF", stroke=DARK, r=18, value="⚑", size=15)
    hh = 250 if locked else 210
    p.card(X0 + 66, y, CW - 66, hh, r=14, stroke=DARK, sw=2)
    p.text(X0 + 88, y + 14, 400, 20, "KIỂM TRA CUỐI CHẶNG", size=12, color=MUTED, bold=True)
    p.text(X0 + 88, y + 36, 600, 28, "<b>Bài kiểm tra: Matching Headings</b>", size=18)
    p.text(X0 + 88, y + 68, 900, 22, "20 câu hỏi · cần đạt từ 70% · không giới hạn thời gian", size=14, color=MUTED)
    p.text(X0 + 88, y + 100, 300, 22, "<b>Để mở bài kiểm tra:</b>", size=14)
    items = (["○ <u>Hoàn thành 1 bài học còn lại</u>", "○ <u>Hoàn thành phần luyện thêm của 1 bài</u>", "○ <u>Làm 1 bài ôn bắt buộc</u>"] if locked
             else ["○ <u>Hoàn thành 2 bài học còn lại</u>"])
    for i, it in enumerate(items):
        p.text(X0 + 100, y + 128 + i * 28, 600, 24, it, size=14, color=PRIMARY)
    p.text(X0 + CW - 460, y + 30, 440, 70, "AVAILABLE → “Làm bài kiểm tra” button (“Đang giao đề…”) → P-35<br>PASSED → “Đã đạt” chip · NONE → “Chặng này không có bài kiểm tra cuối.”", size=12, italic=True, align="right")
    return p


def p31_mobile(f):
    p = mobile_screen(f, "P-31", "Topic Detail", 1, h=820)
    ox = phone(p, 0, "Populated · 375", h=820)
    y = mobile_nav(p, ox=ox) + 16
    p.text(ox + 16, y, 343, 22, "← Khóa IELTS Band 6.0", size=13, color=PRIMARY, bold=True)
    p.text(ox + 16, y + 30, 343, 50, "<b>Matching Headings</b>", size=20)
    p.card(ox + 16, y + 80, 343, 160, r=14)
    p.text(ox + 32, y + 94, 311, 22, "<b>1/3</b> bài đã xong · 33%", size=13)
    p.progress(ox + 32, y + 120, 311, 33)
    p.text(ox + 32, y + 136, 311, 40, "Tiếp theo · <b>Bài 2: Xác định ý chính</b>", size=12)
    p.btn(ox + 32, y + 184, 311, 44, "Học tiếp →")
    for i, t in enumerate(("✓ Bài 1 · 15 phút", "2 Bài 2 · 15 phút", "🔒 Bài 3")):
        p.card(ox + 16, y + 260 + i * 76, 343, 64, r=12, dashed=i == 2)
        p.text(ox + 32, y + 270 + i * 76, 311, 44, t, size=13)
    p.card(ox + 16, y + 500, 343, 170, r=12, stroke=DARK, sw=2)
    p.text(ox + 32, y + 512, 311, 150, "<b>⚑ Kiểm tra cuối chặng</b><br>20 câu · cần đạt từ 70%<br>Để mở bài kiểm tra: checklist link", size=13, valign="top")


# ------------------------------------------------------------------- P-32 Lesson Player

def p32(f, frame="Populated", state="reading"):
    h = 1180
    p, y = learn_page(f, "P-32", frame, "Lesson Player", h, gate=state == "review")
    mw = 820
    back_link(p, X0, y, "Matching Headings")
    p.zone(X0 - 36, y + 40, "A")
    eyebrow(p, X0, y + 40, "Bài 2 · Lý thuyết và bài tập")
    p.text(X0, y + 62, mw, 40, "<b>Xác định ý chính của đoạn</b>", size=30)
    y0 = y + 120
    if state == "review":
        p.card(X0, y0, mw, 280, r=18, stroke=DARK, sw=2)
        p.text(X0, y0 + 30, mw, 36, "🔒", size=26, align="center")
        p.text(X0, y0 + 76, mw, 30, "<b>Nội dung này đang tạm khóa</b>", size=22, align="center")
        p.text(X0 + 60, y0 + 114, mw - 120, 44, "Làm xong bài ôn “Nhận diện paraphrase” trước, bài học sẽ tự mở lại.", size=15, color=MUTED, align="center")
        p.btn(X0 + mw / 2 - 170, y0 + 190, 160, 46, "Làm bài ôn")
        p.link(X0 + mw / 2 + 20, y0 + 202, 200, "Về danh sách bài")
        p.text(X0, y0 + 300, mw, 40, "ApiErrorState REVIEW_REQUIRED — the ReviewGate banner is shown at the top of the page.", size=12, italic=True)
    else:
        p.zone(X0 - 36, y0, "B")
        p.card(X0, y0, mw, 200, r=14)
        p.text(X0 + 20, y0 + 14, 400, 22, "<b>TextBlock</b> — theory", size=14)
        p.lines(X0 + 20, y0 + 50, mw - 40, 6, gap=22)
        p.card(X0, y0 + 216, mw, 90, r=14)
        p.text(X0 + 20, y0 + 230, 760, 60, "<b>AudioBlock</b> ▶ ━━━●━━━ 01:20 / 03:40 · transcript   ·   <b>PassageBlock</b> (đoạn A–E)", size=14, valign="top")
        p.zone(X0 - 36, y0 + 322, "C")
        p.card(X0, y0 + 322, mw, 300, r=14, stroke=PRIMARY if state == "completed" else LINE)
        eyebrow(p, X0 + 20, y0 + 336, "Bài tập")
        p.text(X0 + 20, y0 + 356, 600, 26, "<b>ExerciseBlock</b> — questions 1–5", size=16)
        for i in range(3):
            p.rect(X0 + 20, y0 + 396 + i * 50, mw - 40, 40, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. Question … [answer]", color=MUTED, size=13, align="left")
        if state == "completed":
            p.rect(X0 + 20, y0 + 548, mw - 40, 50, fill=SOFT, stroke=DARK, r=8, value="✔ Đạt 4/5 câu (80%). Đáp án và giải thích hiện dưới từng câu.", size=13, align="left")
        else:
            p.btn(X0 + 20, y0 + 552, 120, 42, "Nộp")
            p.text(X0 + 160, y0 + 556, 600, 36, "EssayBlock (when present): “Nộp bài · 3 điểm” · AI band estimate", size=12, color=MUTED, italic=True)
        if state == "completed":
            p.zone(X0 - 36, y0 + 640, "E")
            p.card(X0, y0 + 640, mw, 170, r=16, stroke=PRIMARY, sw=2)
            p.text(X0 + 24, y0 + 656, 700, 30, "<b>Bạn đã hoàn thành bài này</b>", size=20)
            p.text(X0 + 24, y0 + 692, 760, 24, "Kiến thức của bài đã được ghi nhận. Bạn có thể sang bài tiếp theo.", size=14, color=MUTED)
            p.link(X0 + 24, y0 + 740, 200, "Luyện thêm bài này")
            p.link(X0 + 240, y0 + 740, 200, "Về danh sách bài")
    # rail
    rx, rw = X0 + mw + 40, CW - mw - 40
    p.zone(rx - 30, y + 40, "D")
    p.card(rx, y + 40, rw, 300, r=16)
    p.text(rx + 20, y + 56, rw - 40, 20, "BÀI 2 · MATCHING HEADINGS", size=12, color=MUTED, bold=True)
    label, hint, cta = {"reading": ("◉ Đang học", "Làm và nộp bài tập trong bài để hoàn thành.", "Hoàn thành bài"),
                        "completed": ("✔ Đã hoàn thành", "Sẵn sàng sang bài tiếp theo.", "Bài tiếp theo →"),
                        "review": ("🛡 Cần làm bài ôn", "Hoàn thành bài ôn “Nhận diện paraphrase” để học tiếp.", "Làm bài ôn →")}[state]
    p.text(rx + 20, y + 84, rw - 40, 28, f"<b>{label}</b>", size=17)
    p.text(rx + 20, y + 118, rw - 40, 60, hint, size=14, color=MUTED, valign="top")
    p.btn(rx + 20, y + 196, rw - 40, 48, cta)
    p.link(rx + 20, y + 262, rw - 40, "Danh sách bài của chặng")
    p.text(rx, y + 350, rw, 120, "Sticky rail. States: Đang học · Cần làm bài ôn · Cần luyện thêm (→ “Luyện thêm bài này”) · Đã hoàn thành. Last lesson: “Về chặng làm bài kiểm tra”.", size=12, color=MUTED, italic=True, valign="top")
    return p


def p32_mobile(f):
    p = mobile_screen(f, "P-32", "Lesson Player", 1, h=820)
    ox = phone(p, 0, "Populated · sticky bar · 375", h=820)
    y = mobile_nav(p, ox=ox) + 16
    p.text(ox + 16, y, 343, 22, "← Matching Headings", size=13, color=PRIMARY, bold=True)
    p.text(ox + 16, y + 30, 343, 50, "<b>Xác định ý chính của đoạn</b>", size=19)
    p.card(ox + 16, y + 90, 343, 220, r=12)
    p.lines(ox + 30, y + 110, 310, 7, gap=24)
    p.card(ox + 16, y + 326, 343, 260, r=12)
    p.text(ox + 30, y + 336, 310, 22, "<b>Bài tập</b>", size=14)
    for i in range(3):
        p.rect(ox + 30, y + 370 + i * 50, 315, 40, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. …", size=12, align="left")
    p.rect(ox, 820 - 76, MOBILE_W, 76, fill="#FFFFFF", stroke=DARK, r=0)
    p.text(ox + 16, 820 - 62, 150, 48, "<b>◉ Đang học</b>", size=13)
    p.btn(ox + 170, 820 - 64, 190, 48, "Hoàn thành bài", size=13)
    p.text(ox, 820 + 8, MOBILE_W, 20, "lp-lesson-mobile-bar thay cho rail", size=11, color=MUTED, italic=True, align="center")


# ------------------------------------------------------------------- P-33 Lesson Practice

def p33(f):
    p, y = learn_page(f, "P-33", "Set List", "Lesson Practice", 900)
    back_link(p, X0, y, "Bài 2: Xác định ý chính của đoạn")
    p.zone(X0 - 36, y + 40, "A")
    eyebrow(p, X0, y + 40, "Luyện thêm")
    p.text(X0, y + 62, 1000, 40, "<b>Củng cố trước khi mở bài kiểm tra chặng</b>", size=28)
    p.text(X0, y + 108, 1100, 24, "Bắt buộc: đạt ít nhất một bộ để mở bài kiểm tra chặng · <i>practicePassReason</i>", size=14, color=MUTED)
    p.zone(X0 - 36, y + 160, "B")
    for i, (t, chip, meta, cta, prem) in enumerate((("Bộ 1 · PS-01", "Đã thử", "10 câu · điểm cao nhất 60%", "Làm lại", False),
                                                     ("Bộ 2 · PS-02", "Sẵn sàng", "10 câu", "Làm bộ này", False),
                                                     ("Bộ 3 · PS-03", "Chưa mở", "10 câu · Bộ Premium, chưa mở trong giai đoạn này.", "Khóa", True))):
        yy = y + 160 + i * 110
        p.card(X0, yy, CW, 96, r=14, fill=SOFT if prem else "#FFFFFF", stroke=PRIMARY if i == 1 else LINE, sw=2 if i == 1 else 1)
        p.text(X0 + 24, yy + 14, 500, 26, f"<b>{t}</b>", size=17)
        p.chip(X0 + 24, yy + 50, chip, "active" if i == 1 else "neutral")
        p.text(X0 + 140, yy + 50, 700, 24, meta, size=13, color=MUTED)
        p.btn(X0 + CW - 200, yy + 26, 170, 44, cta, "disabled" if prem else ("primary" if i == 1 else "secondary"), size=14)
    p.text(X0, y + 500, CW, 40, "Empty: “Bài này không có bộ luyện thêm.” · Passed: “Bạn đã đạt phần luyện thêm. Quay lại chặng để làm bài kiểm tra (nếu không còn bài ôn).” + “Về lộ trình” button", size=13, italic=True)
    # attempt
    p, y = learn_page(f, "P-33", "Attempt", "Lesson Practice", 980)
    p.text(X0, y, 400, 24, "← Danh sách luyện thêm", size=14, color=PRIMARY, bold=True)
    p.zone(X0 - 36, y + 40, "A")
    eyebrow(p, X0, y + 40, "Luyện thêm · PS-02")
    p.text(X0, y + 62, 900, 40, "<b>Bộ 2 — Main idea</b>", size=28)
    p.zone(X0 - 36, y + 130, "B")
    p.card(X0, y + 130, 560, 620, r=14)
    p.text(X0 + 20, y + 144, 500, 24, "<b>Practice audio / PassageBlock</b>", size=15)
    p.lines(X0 + 20, y + 186, 520, 14, gap=26)
    p.zone(X0 + 580 - 30, y + 130, "C")
    p.card(X0 + 580, y + 130, CW - 580, 620, r=14)
    p.text(X0 + 600, y + 146, 500, 24, "<b>ExerciseBlock</b> (no resubmission)", size=15)
    for i in range(6):
        p.rect(X0 + 600, y + 190 + i * 60, CW - 620, 46, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. Question …", color=MUTED, size=13, align="left")
    p.btn(X0 + 600, y + 680, 140, 46, "Nộp")
    p.text(X0, y + 770, CW, 40, "Warning when answers were revealed: “Bộ này đã lộ đáp án trước đó nên lần nộp này không được tính.”", size=13, italic=True)
    # outcome
    p, y = learn_page(f, "P-33", "Outcome", "Lesson Practice", 760)
    p.text(X0, y, 400, 24, "← Danh sách luyện thêm", size=14, color=PRIMARY, bold=True)
    for i, (t, body, cta) in enumerate((("Đạt", "Đã đạt bộ luyện thêm (90%).", "Tiếp tục luyện / về topic"),
                                         ("Chưa đạt", "Chưa đạt (40%). Có thể chọn bộ khác hoặc làm bài ôn nếu được tạo.", "Về danh sách"),
                                         ("Tạo bài ôn", "Đã tạo bài ôn bắt buộc sau lần luyện này.", "Làm bài ôn →"))):
        x = X0 + i * 420
        p.zone(x - 13, y + 60, "DEF"[i])
        p.card(x, y + 60, 400, 260, r=16, stroke=PRIMARY if i == 2 else DARK, sw=2)
        p.text(x + 24, y + 80, 350, 28, f"<b>{t}</b>", size=20)
        p.text(x + 24, y + 120, 350, 80, body, size=15, color=MUTED, valign="top")
        p.btn(x + 24, y + 240, 350, 48, cta, "primary" if i == 2 else "secondary", size=14)
    p.text(X0, y + 360, CW, 30, "A created review → navigate to P-34. After passing → back to P-31 to unlock the topic test.", size=13, italic=True)


# ------------------------------------------------------------------- P-34 Review

def p34(f):
    for frame, stage in (("Theory", "THEORY"), ("Practice Set", "PRACTICE_SET"), ("Finished", "DONE")):
        p, y = learn_page(f, "P-34", frame, "Review Session", 1000)
        back_link(p, X0, y, "Về lộ trình học")
        p.zone(X0 - 36, y + 40, "A")
        eyebrow(p, X0, y + 40, "Bài ôn bắt buộc · Bước " + ("đọc lý thuyết" if stage == "THEORY" else "luyện tập"))
        p.text(X0, y + 62, 900, 40, "<b>Nhận diện paraphrase</b>", size=28)
        lead = ("Đọc lại lý thuyết rồi trả lời vài câu kiểm tra nhanh để mở bộ luyện ôn." if stage == "THEORY"
                else "Làm bộ câu hỏi ôn. Đạt là hoàn thành; chưa đạt thì đọc lại lý thuyết hoặc làm bộ khác. Số bộ chưa đạt: 1/3.")
        p.text(X0, y + 108, 1100, 24, lead, size=14, color=MUTED)
        y2 = y + 150
        if stage == "DONE":
            p.zone(X0 - 36, y2, "D")
            p.card(X0, y2, CW, 200, r=18, stroke=PRIMARY, sw=2)
            p.text(X0 + 30, y2 + 24, 800, 30, "<b>Đã hoàn thành bài ôn</b>", size=22)
            p.text(X0 + 30, y2 + 64, 1000, 24, "Lộ trình đã mở lại. Bạn có thể học tiếp.", size=15, color=MUTED)
            p.btn(X0 + 30, y2 + 120, 180, 48, "Học tiếp →")
            p.text(X0, y2 + 230, CW, 40, "SKIPPED: “Đã bỏ qua bài ôn” — “Bạn chưa đạt sau 3 bộ. Bài ôn được bỏ qua để bạn tiếp tục học.”", size=13, italic=True)
            y2 += 280
        p.zone(X0 - 36, y2, "B")
        p.card(X0, y2, CW, 200, r=14)
        p.text(X0 + 20, y2 + 14, 600, 26, "📘 <b>Lý thuyết cần nhớ</b>", size=17)
        p.lines(X0 + 20, y2 + 56, CW - 40, 5, gap=24)
        y2 += 220
        if stage == "THEORY":
            p.zone(X0 - 36, y2, "C")
            p.card(X0, y2, CW, 300, r=14)
            p.text(X0 + 20, y2 + 14, 600, 26, "<b>Kiểm tra nhanh lý thuyết</b>", size=17)
            for i in range(3):
                p.rect(X0 + 20, y2 + 56 + i * 54, CW - 40, 42, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. …", color=MUTED, size=13, align="left")
            p.btn(X0 + 20, y2 + 230, 120, 44, "Nộp")
            p.text(X0 + 160, y2 + 236, 900, 40, "No questions: “Xác nhận đã đọc lý thuyết…” + “Tiếp tục luyện ôn” button", size=12, color=MUTED, italic=True)
        elif stage == "PRACTICE_SET":
            p.zone(X0 - 36, y2, "C")
            p.card(X0, y2, 560, 330, r=14)
            p.text(X0 + 20, y2 + 14, 500, 24, "<b>Review audio / Passage</b>", size=15)
            p.lines(X0 + 20, y2 + 56, 520, 8, gap=26)
            p.card(X0 + 580, y2, CW - 580, 330, r=14)
            eyebrow(p, X0 + 600, y2 + 14, "Bộ 2/3 · RV-PKG-02")
            for i in range(3):
                p.rect(X0 + 600, y2 + 50 + i * 56, CW - 620, 44, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. …", color=MUTED, size=13, align="left")
            p.btn(X0 + 600, y2 + 230, 120, 44, "Nộp")
            p.btn(X0 + 740, y2 + 230, 220, 44, "↻ Làm bộ tiếp theo", "secondary", size=13)


# ------------------------------------------------------------------- P-35 Test / P-36 Result

def p35(f, frame="Populated", confirming=False):
    p, y = learn_page(f, "P-35", frame, "Topic / Course Test", 1060)
    back_link(p, X0, y, "Về topic   (course test: “Về khóa học”)")
    p.zone(X0 - 36, y + 40, "A")
    eyebrow(p, X0, y + 40, "Bài kiểm tra cuối   (course: “Thi cuối khóa”)")
    p.text(X0, y + 62, 900, 40, "<b>Làm từng phần, câu trả lời được lưu ngay</b>", size=28)
    p.text(X0 + CW - 300, y + 70, 300, 24, "∞ Không giới hạn thời gian", size=14, color=MUTED, align="right")
    p.zone(X0 - 36, y + 120, "B")
    for i in range(3):
        p.rect(X0 + i * 170, y + 120, 160, 56, fill=PRIMARY_SOFT if i == 0 else "#FFFFFF", stroke=PRIMARY if i == 0 else LINE, r=12,
               value=f"<b>Phần {i + 1}</b><br>{[4, 0, 0][i]}/{[10, 10, 5][i]} câu", size=13)
    p.zone(X0 - 36, y + 200, "C")
    p.card(X0, y + 200, 560, 560, r=14)
    p.text(X0 + 20, y + 214, 500, 24, "<b>Section AudioBlock / PassageBlock</b>", size=15)
    p.lines(X0 + 20, y + 256, 520, 14, gap=26)
    p.card(X0 + 580, y + 200, CW - 580, 560, r=14)
    eyebrow(p, X0 + 600, y + 214, "Phần 1/3")
    p.text(X0 + 600, y + 236, 500, 26, "<b>Section title</b>", size=17)
    for i in range(5):
        p.rect(X0 + 600, y + 280 + i * 70, CW - 620, 44, fill="#FFFFFF", stroke=DARK, r=8, value=f"{i + 1}. Question … [answer]", color=MUTED, size=13, align="left")
        p.text(X0 + 600, y + 326 + i * 70, 400, 18, ["✓ Đã lưu", "Đang lưu…", "⚠ Chưa lưu được, sẽ thử lại khi nộp", "", ""][i], size=11, color=MUTED)
    p.btn(X0 + CW - 160, y + 700, 140, 42, "Phần tiếp →", "secondary", size=13)
    p.zone(X0 - 36, y + 790, "D")
    p.rect(0, y + 780, DESKTOP_W, 90, fill="#FFFFFF", stroke=DARK, r=0)
    p.text(X0, y + 800, 300, 40, "<b>4/25</b> câu đã trả lời", size=16)
    if confirming:
        p.rect(X0 + 320, y + 798, 620, 46, fill=SOFT, stroke=DARK, r=8, value="Còn 21 câu bỏ trống, các câu đó sẽ tính sai. Bấm lần nữa để nộp.", size=13)
    p.btn(X0 + CW - 200, y + 796, 200, 52, "➤ " + ("Vẫn nộp bài" if confirming else "Nộp bài"))
    p.text(X0, y + 880, CW, 20, "lp-test-bar sticky at the bottom", size=11, color=MUTED, italic=True)


def p36(f, passed):
    frame = "Passed" if passed else "Failed"
    p, y = learn_page(f, "P-36", frame, "Test Result", 960)
    p.zone(X0 - 36, y, "A")
    p.card(X0, y, CW, 300, r=20, stroke=PRIMARY if passed else DARK, sw=2)
    p.rect(X0 + 40, y + 50, 180, 180, fill="#FFFFFF", stroke=PRIMARY if passed else DARK, r=90, sw=10, value=f"<b>{85 if passed else 55}%</b>", size=30)
    eyebrow(p, X0 + 260, y + 34, "Kết quả · mã đề TT-RD-03-B")
    p.text(X0 + 260, y + 56, 900, 40, "<b>" + ("Đạt bài kiểm tra cuối" if passed else "Chưa đạt lần này") + "</b>", size=28)
    p.text(X0 + 260, y + 100, 900, 24, f"<b>{17 if passed else 11}/20</b> câu đúng · {85 if passed else 55}% · cần từ 70%", size=15)
    msg = ("Topic đã qua. Chặng tiếp theo đã mở (làm mới từ GET /topics)." if passed
           else "Đáp án được ẩn khi chưa đạt. Ôn lại các bài trong topic rồi làm lại; lần sau bạn sẽ nhận mã đề khác.")
    p.text(X0 + 260, y + 132, 900, 44, msg, size=14, color=MUTED, valign="top")
    p.zone(X0 + 260 - 36, y + 200, "B")
    if passed:
        p.btn(X0 + 260, y + 200, 260, 48, "🏆 Sang chặng tiếp theo →")
        p.btn(X0 + 540, y + 200, 220, 48, "Xem danh sách khóa", "secondary", size=14)
    else:
        p.btn(X0 + 260, y + 200, 240, 48, "↺ Về topic để làm lại", "secondary", size=14)
    p.zone(X0 - 36, y + 330, "C")
    p.text(X0, y + 330, 400, 30, "<b>Từng câu</b>", size=22)
    for i in range(4):
        p.card(X0, y + 376 + i * 70, CW, 60, r=10)
        p.text(X0 + 20, y + 386 + i * 70, 1000, 40,
               f"<b>Câu {i + 1}</b> · prompt …  " + ("✓ correct · answer: …" if passed else "✗ / ✓ (answers hidden when failed)"), size=14)
    p.text(X0, y + 670, CW, 40, "Course test (?course=): title “Đạt thi cuối khóa”, button “Về khóa học”; failed: “Về khóa để làm lại”. ⚠ Current copy exposes technical details (“poll GET /courses”) — see Appendix B.", size=13, italic=True)


def build(new):
    f = new("P-30_CourseList"); p30(f); p30_states(f); p30_mobile(f); f.save()
    f = new("P-30a_Placement"); p30a(f); f.save()
    f = new("P-30b_TopicList"); p30b(f); p30b_states(f); p30b_mobile(f); f.save()
    f = new("P-31_TopicDetail"); p31(f); p31(f, "Locked Test / Review Gate", locked=True); p31_mobile(f); f.save()
    f = new("P-32_LessonPlayer"); p32(f); p32(f, "Completed", "completed"); p32(f, "Review Required", "review"); p32_mobile(f); f.save()
    f = new("P-33_LessonPractice"); p33(f); f.save()
    f = new("P-34_ReviewSession"); p34(f); f.save()
    f = new("P-35_TopicTest"); p35(f); p35(f, "Confirm Submit", True); f.save()
    f = new("P-36_TestResult"); p36(f, True); p36(f, False); f.save()
