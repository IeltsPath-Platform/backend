"""Wireframes: practice catalog and skill workspaces (P-41…P-44a). Content is mock data in the FE."""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, CANVAS, DARK, MEDIA
from chrome import (navbar, mobile_nav, footer, consult_fab, screen, mobile_screen, phone, eyebrow,
                    DESKTOP_W, MOBILE_W)


def catalog_base(p, h, dim=False):
    y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng", child="Reading")
    # sidebar
    p.zone(14, y + 28, "A")
    p.card(40, y + 24, 260, 640, r=16)
    p.text(64, y + 44, 220, 50, "<b>LUYỆN ĐỀ</b><br><font style='font-size:12px' color='#6B7280'>The IELTS Space</font>", size=18, valign="top")
    p.text(64, y + 110, 200, 20, "KỸ NĂNG", size=12, color=PRIMARY, bold=True)
    p.rect(60, y + 136, 220, 42, fill=PRIMARY, stroke=PRIMARY, r=10, value="📖 Reading ˄", color="#FFFFFF", size=14, bold=True, align="left")
    for i, t in enumerate(("• Bài lẻ", "   ◉ Passage 1", "   ○ Passage 2", "   ○ Passage 3", "○ Full đề")):
        p.text(84, y + 186 + i * 28, 190, 24, t, size=13, color=INK if i != 1 else PRIMARY)
    for i, t in enumerate(("🎧 Listening ˅", "✎ Writing ˅", "🎤 Speaking ˅")):
        p.text(70, y + 336 + i * 44, 200, 30, t, size=14)
    p.text(64, y + 480, 200, 20, "NGUỒN TÀI LIỆU", size=12, color=PRIMARY, bold=True)
    for i, t in enumerate(("☑ The IELTS Space PRO  [PRO]", "☐ Actual Tests", "☐ Các nguồn khác")):
        p.text(70, y + 510 + i * 32, 220, 26, t, size=13)
    # banner
    mx, mw = 330, 1070
    p.zone(mx - 30, y + 24, "B")
    p.rect(mx, y + 24, mw, 200, fill=PRIMARY, stroke=PRIMARY, r=24)
    p.img(mx + 40, y + 44, 150, 160, "Owl mascot SVG")
    p.text(mx + 300, y + 60, 600, 40, "<b>Chưa biết mình đang ở band nào?</b>", size=26, color="#FFFFFF")
    p.text(mx + 300, y + 104, 600, 24, "Test đầu vào và khám phá trình độ IELTS hiện tại của bạn.", size=15, color="#FFFFFF")
    p.rect(mx + 300, y + 144, 170, 46, fill="#FFFFFF", stroke="#FFFFFF", r=23, value="▶ TEST NGAY ✦", color=PRIMARY, bold=True, size=14)
    # controls
    p.zone(mx - 30, y + 250, "C")
    p.rect(mx, y + 250, 250, 46, fill="#FFFFFF", stroke=LINE, r=23)
    p.rect(mx + 4, y + 254, 124, 38, fill=PRIMARY, stroke=PRIMARY, r=19, value="Bài chưa làm", color="#FFFFFF", size=13, bold=True)
    p.text(mx + 132, y + 254, 114, 38, "Bài đã làm", size=13, align="center")
    p.rect(mx + mw - 320, y + 250, 320, 46, fill="#FFFFFF", stroke=LINE, r=23, value="🔍 Tìm tên bài tập", color=MUTED, size=13, align="left")
    # cards
    p.zone(mx - 30, y + 320, "D")
    cw = (mw - 40) // 3
    cards = [("MIỄN PHÍ", "Passage 1", "Vitamins - To supplement or not?", ["Gap Filling", "Match Information", "Yes/No/Not Given"]),
             ("MIỄN PHÍ", "Passage 1", "The Evolutionary Mystery: Crocodile…", ["Gap Filling", "Matching Features"]),
             ("PRO", "Passage 3", "How Does Watching Sports Influenc…", ["Summary Completion", "Multiple Choice"])]
    for i, (tag, badge, title, bullets) in enumerate(cards):
        cx = mx + i * (cw + 20)
        p.card(cx, y + 320, cw, 350, r=16)
        p.img(cx + 1, y + 321, cw - 2, 170, "Test image")
        p.rect(cx + cw - 90, y + 330, 80, 24, fill=DARK if tag == "PRO" else "#FFFFFF", stroke=DARK, r=4, value=tag, size=11,
               color="#FFFFFF" if tag == "PRO" else INK, bold=True)
        p.rect(cx + 1, y + 490, cw - 2, 179, fill=PRIMARY, stroke=PRIMARY, r=0)
        p.chip(cx + 20, y + 504, badge, "outline")
        p.text(cx + 20, y + 538, cw - 40, 26, f"<b>{title}</b>", size=15, color="#FFFFFF")
        for j, b in enumerate(bullets):
            p.text(cx + 20, y + 572 + j * 24, cw - 40, 22, "• " + b, size=13, color="#FFFFFF")
    consult_fab(p, 1240, h - 140)
    footer(p, h - 64)
    return y


def p41(f):
    h = 1080
    p = screen(f, "P-41", "Populated", "Practice Catalog", h=h)
    catalog_base(p, h)
    p.text(330, 1000 - 40, 1070, 30, "All cards are MOCK_PRACTICE_CARDS (no API yet). Click / Enter / Space on a card → P-41a. Filters: skill (sidebar + ?skill=), tab, search.", size=12, italic=True)
    p = mobile_screen(f, "P-41", "Practice Catalog", 2, h=820)
    ox = phone(p, 0, "Populated · 375", h=820)
    y = mobile_nav(p, ox=ox)
    p.rect(ox, y, MOBILE_W, 40, fill=PRIMARY_SOFT, stroke=LINE, r=0, value="Listening · <u><b>Reading</b></u> · Writing · Speaking", size=12)
    y += 52
    p.card(ox + 12, y, 351, 90, r=12)
    p.text(ox + 24, y + 10, 320, 70, "<b>LUYỆN ĐỀ</b> — sidebar xếp lên đầu (kỹ năng + nguồn)", size=13, valign="top")
    p.rect(ox + 12, y + 104, 351, 150, fill=PRIMARY, stroke=PRIMARY, r=16, value="<b>Chưa biết mình đang ở band nào?</b><br>▶ TEST NGAY ✦", color="#FFFFFF", size=14)
    p.rect(ox + 12, y + 268, 351, 40, fill="#FFFFFF", stroke=LINE, r=20, value="Bài chưa làm | Bài đã làm", size=12)
    p.rect(ox + 12, y + 316, 351, 40, fill="#FFFFFF", stroke=LINE, r=20, value="🔍 Tìm tên bài tập", color=MUTED, size=12, align="left")
    p.card(ox + 12, y + 370, 351, 300, r=14)
    p.img(ox + 13, y + 371, 349, 140, "Test image")
    p.rect(ox + 13, y + 510, 349, 158, fill=PRIMARY, stroke=PRIMARY, r=0, value="<b>Vitamins - To supplement or not?</b><br>• Gap Filling • …", color="#FFFFFF", size=13)
    ox = phone(p, 1, "Mode select (P-41a) · 375", h=820)
    p.overlay(MOBILE_W, 820, ox=ox)
    p.card(ox + 12, 120, 351, 560, r=16)
    p.text(ox + 28, 136, 320, 26, "<b>Vitamins - To supplement…</b>", size=15, color=PRIMARY)
    p.rect(ox + 12, 180, 351, 500, fill=PRIMARY, stroke=PRIMARY, r=0)
    p.text(ox + 28, 190, 320, 24, "Lựa chọn chế độ làm bài", size=14, color="#FFFFFF", bold=True)
    for i, t in enumerate(("Thi thử", "Luyện tập")):
        p.rect(ox + 28, 226 + i * 180, 319, 164, fill="#FFFFFF", stroke="#FFFFFF" if i else PRIMARY_SOFT, r=12, sw=3 if i == 0 else 1,
               value=f"<b>{t}</b><br>(mô tả)", size=13)
    p.rect(ox + 28, 600, 319, 52, fill="#FFFFFF", stroke="#FFFFFF", r=26, value="BẮT ĐẦU LÀM BÀI", color=PRIMARY, bold=True, size=14)


def p41a(f):
    h = 1080
    p = screen(f, "P-41a", "Populated", "Practice Mode Select", h=h)
    catalog_base(p, h)
    p.overlay(DESKTOP_W, h)
    x, y, w = 330, 170, 780
    p.card(x, y, w, 640, r=20)
    p.zone(x - 13, y + 20, "A")
    p.text(x + 32, y + 22, w - 100, 34, "<b>Vitamins - To supplement or not?</b>", size=22, color=PRIMARY)
    p.text(x + w - 56, y + 24, 30, 30, "✕", size=18, color=MUTED, align="center")
    p.rect(x, y + 80, w, 560, fill=PRIMARY, stroke=PRIMARY, r=0)
    p.text(x + 32, y + 100, 500, 26, "<b>Lựa chọn chế độ làm bài</b>", size=17, color="#FFFFFF")
    p.zone(x - 13, y + 150, "B")
    for i, (t, d) in enumerate((("Thi thử", "Tập trung như phòng thi thật. Không có Highlight, Note, tra từ hay Flashcard."),
                                 ("Luyện tập", "Học chủ động với các hỗ trợ phù hợp cho từng kỹ năng và xem lại tiến độ của bạn."))):
        cx = x + 32 + i * 366
        p.rect(cx, y + 146, 350, 330, fill="#FFFFFF", stroke=PRIMARY_SOFT if i == 0 else "#FFFFFF", r=16, sw=4 if i == 0 else 1)
        if i == 0:
            p.chip(cx + 16, y + 160, "Thi thử", "solid")
        p.img(cx + 24, y + 196, 302, 140, "Preview " + ("exam UI" if i == 0 else "practice workspace"))
        p.text(cx + 24, y + 350, 302, 28, f"<b>{t}</b>", size=18, align="center")
        p.text(cx + 24, y + 384, 302, 70, d, size=13, color=MUTED, align="center", valign="top")
    p.text(x + 32, y + 482, 600, 20, "aria-pressed: the selected card has a thick border (default Thi thử)", size=12, color="#FFFFFF", italic=True)
    p.zone(x - 13, y + 540, "C")
    p.rect(x + w / 2 - 160, y + 540, 320, 56, fill="#FFFFFF", stroke="#FFFFFF", r=28, value="BẮT ĐẦU LÀM BÀI", color=PRIMARY, bold=True, size=17)


def workspace_bar(p, y, mode="Luyện tập", progress="1/5 câu · 20%", right=True):
    p.zone(14, y + 18, "A")
    p.rect(0, y, DESKTOP_W, 72, fill="#FFFFFF", stroke=LINE, r=0)
    p.rect(40, y + 16, 110, 40, fill="#FFFFFF", stroke=DARK, r=20, value="← Thoát", size=14, bold=True)
    p.chip(420, y + 24, mode, "active")
    p.rect(520, y + 18, 140, 36, fill=PRIMARY_SOFT, stroke=PRIMARY_SOFT, r=18, value="🕒 00:12:40", size=14, color=PRIMARY, bold=True)
    p.text(690, y + 12, 330, 20, f"Tiến độ   {progress}", size=12, color=MUTED)
    p.progress(690, y + 40, 330, 20)
    if right:
        p.rect(1180, y + 16, 150, 40, fill="#FFFFFF", stroke=DARK, r=10, value="🗒 Ghi chú (0)", size=13)
        p.rect(1344, y + 16, 44, 40, fill="#FFFFFF", stroke=DARK, r=10, value="⚙", size=16)
    return y + 72


def question_map(p, x, y, n=5, active=1):
    p.card(x, y, 220, 290, r=14)
    p.text(x + 16, y + 12, 140, 18, "QUESTION MAP", size=11, color=PRIMARY, bold=True)
    p.text(x + 16, y + 32, 180, 24, "<b>Bản đồ câu hỏi</b>", size=15)
    for i in range(n):
        p.rect(x + 16 + (i % 3) * 64, y + 100 + (i // 3) * 54, 56, 44, fill=PRIMARY_SOFT if i + 1 == active else "#FFFFFF",
               stroke=PRIMARY if i + 1 == active else LINE, r=8, value=str(i + 1), size=14, bold=True)
    p.btn(x + 16, y + 226, 88, 40, "← Back", "secondary", size=13)
    p.btn(x + 116, y + 226, 88, 40, "Next →", size=13)


def reading(p, y, mode):
    y = workspace_bar(p, y, mode, right=mode == "Luyện tập")
    p.zone(14, y + 24, "B")
    question_map(p, 30, y + 24)
    cx = 270
    if mode == "Luyện tập":
        p.zone(cx - 26, y + 24, "C")
        p.card(cx, y + 24, 560, 140, r=14)
        p.text(cx + 20, y + 36, 200, 18, "PRACTICE TOOLS", size=11, color=PRIMARY, bold=True)
        p.text(cx + 20, y + 56, 240, 24, "<b>Công cụ hỗ trợ</b>", size=15)
        p.text(cx + 280, y + 40, 270, 36, "Bôi đen một đoạn trong bài đọc để bắt đầu.", size=11, color=MUTED, align="right")
        for i, t in enumerate(("✎ Highlight", "🗒 Note", "文 Tra từ vựng", "▤ Tạo Flashcard")):
            p.rect(cx + 20 + i * 132, y + 92, 124, 36, fill="#FFFFFF", stroke=LINE, r=8, value=t, size=13)
        p.text(cx + 20, y + 132, 200, 24, "▤ Thẻ đã lưu", size=13)
        py = y + 184
    else:
        py = y + 24
    p.zone(cx - 26, py, "D")
    p.card(cx, py, 560, 700 - (py - y), r=14)
    p.text(cx + 24, py + 16, 400, 18, "READING · BÀI ĐỌC MINH HỌA", size=11, color=PRIMARY, bold=True)
    p.text(cx + 24, py + 38, 400, 34, "<b>Snow Makers</b>", size=26)
    for i, L in enumerate("ABCD"):
        yy = py + 90 + i * 110
        if yy + 90 > y + 690:
            break
        p.text(cx + 24, yy, 24, 24, f"<b>{L}.</b>", size=14)
        if i == 1 and mode == "Luyện tập":
            p.rect(cx + 50, yy + 2, 300, 14, fill="#FEF08A", stroke="#E5C100", r=2)
        p.lines(cx + 50, yy + 4, 480, 4, gap=22)
    ax = 850
    p.zone(ax - 26, y + 24, "E")
    p.card(ax, y + 24, 560, 560, r=14)
    p.text(ax + 24, y + 44, 400, 30, "<b>Question 1 of 5</b>", size=22, color=PRIMARY)
    p.text(ax + 24, y + 80, 520, 20, "Choose the correct heading for each paragraph from the list below.", size=12, color=MUTED)
    p.card(ax + 24, y + 110, 512, 280, r=10)
    p.text(ax + 40, y + 120, 300, 22, "<b>List of Headings</b>", size=13)
    for i, t in enumerate(("i. Considering ecological costs", "ii. Modifications to the design…", "iii. The need for different varieties…", "iv. …", "x. Snow formation in nature")):
        p.rect(ax + 40, y + 150 + i * 46, 480, 36, fill="#FFFFFF", stroke=LINE, r=6, value=t, size=12, align="left")
    p.rect(ax + 24, y + 410, 512, 100, fill="#FFFFFF", stroke=PRIMARY, r=10, sw=2)
    p.text(ax + 40, y + 420, 400, 22, "<b>1. Paragraph C</b>   ⚑", size=14)
    p.rect(ax + 40, y + 452, 480, 40, fill="#FFFFFF", stroke=DARK, r=8, value="Chưa chọn đáp án ˅", color=MUTED, size=13, align="left")
    p.text(ax + 24, y + 520, 300, 20, "Đã chọn 0/5 câu", size=12, color=MUTED)


def p42(f):
    h = 960
    for mode, frame in (("Luyện tập", "Practice Mode"), ("Thi thử", "Exam Mode")):
        p = screen(f, "P-42", frame, "Reading Practice Test", h=h)
        y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
        reading(p, y, mode)
        if mode == "Thi thử":
            p.text(270, h - 110, 560, 40, "Thi thử: hides Practice Tools, the Ghi chú button, Floating Notes and dialogs; ⚙ settings remain.", size=12, italic=True)
        footer(p, h - 64)
    p = mobile_screen(f, "P-42", "Reading Practice Test", 1, h=820)
    ox = phone(p, 0, "Practice · 375", h=820)
    y = mobile_nav(p, ox=ox)
    p.rect(ox, y, MOBILE_W, 60, fill="#FFFFFF", stroke=LINE, r=0)
    p.rect(ox + 12, y + 12, 80, 36, fill="#FFFFFF", stroke=DARK, r=18, value="← Thoát", size=12)
    p.text(ox + 100, y + 12, 200, 36, "Luyện tập · 🕒 00:12:40", size=12, bold=True)
    p.rect(ox + 316, y + 12, 44, 36, fill="#FFFFFF", stroke=DARK, r=8, value="⚙", size=14)
    y += 72
    p.card(ox + 12, y, 351, 110, r=12)
    p.text(ox + 24, y + 8, 320, 90, "<b>Bản đồ câu hỏi</b><br>[1][2][3][4][5]  ← Back · Next →", size=13, valign="top")
    p.card(ox + 12, y + 124, 351, 300, r=12)
    p.text(ox + 24, y + 134, 300, 24, "<b>Snow Makers</b>", size=16)
    p.lines(ox + 24, y + 170, 320, 9, gap=26)
    p.card(ox + 12, y + 438, 351, 200, r=12)
    p.text(ox + 24, y + 448, 320, 24, "<b>Question 1 of 5</b>", size=15, color=PRIMARY)
    p.rect(ox + 24, y + 490, 327, 40, fill="#FFFFFF", stroke=DARK, r=8, value="Chưa chọn đáp án ˅", color=MUTED, size=12, align="left")
    p.text(ox, 830, MOBILE_W, 20, "Ba cột xếp dọc (map → passage → câu hỏi)", size=11, color=MUTED, italic=True, align="center")


def dialog_over_reading(f, pid, title, w, h_dialog, body):
    h = 960
    p = screen(f, pid, "Populated", title, h=h)
    y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
    reading(p, y, "Luyện tập")
    footer(p, h - 64)
    p.overlay(DESKTOP_W, h)
    x, yy = (DESKTOP_W - w) // 2, 140
    p.card(x, yy, w, h_dialog, r=18)
    p.text(x + w - 50, yy + 22, 26, 26, "✕", size=17, color=MUTED, align="center")
    body(p, x, yy, w)
    return p


def p42a(f):
    def body(p, x, y, w):
        p.zone(x - 13, y + 20, "A")
        p.text(x + 32, y + 22, w - 100, 30, "<b>Tạo Flashcard</b>", size=22)
        p.text(x + 32, y + 56, w - 64, 40, "Lưu từ vựng và ảnh minh họa trên trình duyệt này. Chưa đồng bộ tài khoản.", size=13, color=MUTED, valign="top")
        p.zone(x - 13, y + 110, "B")
        yy = p.field(x + 32, y + 104, w - 64, "Từ / cụm từ *", "snow gun  (prefilled from the selection)")
        p.text(x + 32, yy, 300, 20, "<b>Nghĩa / định nghĩa *</b>", size=14)
        p.rect(x + 32, yy + 24, w - 64, 64, fill="#FFFFFF", stroke=DARK, r=8)
        p.text(x + 32, yy + 100, 300, 20, "<b>Ví dụ / ngữ cảnh</b>", size=14)
        p.rect(x + 32, yy + 124, w - 64, 64, fill="#FFFFFF", stroke=DARK, r=8, value="(sentence from the passage)", color=MUTED, size=12, align="left", valign="top")
        p.text(x + 32, yy + 200, 300, 20, "<b>Ảnh minh họa</b>", size=14)
        p.btn(x + 32, yy + 224, 180, 40, "🖼 Chọn ảnh", "secondary", size=13)
        p.text(x + 224, yy + 232, 300, 24, "PNG, JPG hoặc WebP · tối đa 1 MB", size=12, color=MUTED)
        p.zone(x - 13, yy + 290, "C")
        p.text(x + 32, yy + 280, w - 64, 20, "Errors: “Nhập từ vựng và nghĩa trước khi lưu.” / “Không lưu được: bộ nhớ đầy…”", size=11, color=MUTED, italic=True)
        p.btn(x + w - 212, yy + 300, 180, 46, "Lưu Flashcard")
    dialog_over_reading(f, "P-42a", "Create Flashcard", 620, 600, body)


def p42b(f):
    def body(p, x, y, w):
        p.zone(x - 13, y + 20, "A")
        p.text(x + 32, y + 22, w - 100, 30, "<b>Tra từ vựng</b>", size=22)
        p.text(x + 32, y + 56, w - 64, 24, "Từ điển nhỏ của bài đọc. Có thể tra cứu thêm trên Wiktionary.", size=13, color=MUTED)
        p.zone(x - 13, y + 100, "B")
        p.text(x + 32, y + 100, w - 64, 30, "<b>snow gun</b>", size=20)
        p.text(x + 32, y + 136, w - 64, 30, "máy phun tuyết", size=16)
        p.text(x + 32, y + 176, w - 64, 40, "Not in the glossary: “Chưa có nghĩa trong từ điển của bài. Hãy chọn một từ/cụm từ ngắn hoặc mở trang tra cứu bên dưới.”", size=12, color=MUTED, italic=True, valign="top")
        p.zone(x - 13, y + 236, "C")
        p.link(x + 32, y + 236, 400, "↗ Wiktionary (new tab)")
    dialog_over_reading(f, "P-42b", "Dictionary Lookup", 520, 290, body)


def p42c(f):
    def body(p, x, y, w):
        p.zone(x - 13, y + 20, "A")
        p.text(x + 32, y + 22, w - 100, 30, "<b>Flashcard đã lưu (2)</b>", size=22)
        p.zone(x - 13, y + 80, "B")
        for i, (word, mean) in enumerate((("snow gun", "máy phun tuyết"), ("condenses", "ngưng tụ"))):
            p.card(x + 32, y + 76 + i * 110, w - 64, 96, r=12)
            p.img(x + 44, y + 86 + i * 110, 76, 76, "img")
            p.text(x + 136, y + 88 + i * 110, w - 200, 70, f"<b>{word}</b><br>{mean}<br><i>example …</i>", size=13, valign="top")
        p.text(x + 32, y + 310, w - 64, 44, "Empty: “Chưa có thẻ. Chọn từ trong bài đọc và nhấn “Tạo Flashcard”.” · localStorage read errors use role=alert.", size=12, color=MUTED, italic=True, valign="top")
    dialog_over_reading(f, "P-42c", "Saved Flashcards", 600, 380, body)


def p42d(f):
    h = 960
    p = screen(f, "P-42d", "Populated", "Floating Notes", h=h)
    y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
    reading(p, y, "Luyện tập")
    footer(p, h - 64)
    x, yy, w = 900, 300, 420
    p.card(x, yy, w, 470, r=14, stroke=DARK, sw=2)
    p.zone(x - 13, yy + 10, "A")
    p.rect(x, yy, w, 48, fill=SOFT, stroke=DARK, r=14, value="⠿ <b>Ghi chú</b>            —   ✕", size=14, align="left")
    p.text(x + 16, yy + 54, w - 32, 20, "Kéo thanh tiêu đề để di chuyển  ◀ ▶", size=11, color=MUTED)
    p.zone(x - 13, yy + 90, "B")
    p.text(x + 16, yy + 84, w - 32, 20, "Đoạn B · “water vapour condenses…”", size=12, color=PRIMARY)
    p.text(x + 16, yy + 108, 300, 20, "<b>Nội dung ghi chú</b>", size=13)
    p.rect(x + 16, yy + 132, w - 32, 80, fill="#FFFFFF", stroke=DARK, r=8)
    p.btn(x + w - 156, yy + 222, 140, 40, "Lưu ghi chú", size=13)
    p.text(x + 16, yy + 270, w - 32, 20, "Ghi chú giữ trong phiên làm bài; tải lại trang sẽ xóa.", size=11, color=MUTED, italic=True)
    p.zone(x - 13, yy + 300, "C")
    for i in range(2):
        p.card(x + 16, yy + 300 + i * 76, w - 32, 66, r=8)
        p.text(x + 28, yy + 306 + i * 76, w - 100, 54, f"<b>Đoạn {'BC'[i]}</b> · 10:2{i}<br>note text …", size=12, valign="top")
        p.text(x + w - 60, yy + 316 + i * 76, 30, 30, "🗑", size=14)


def p43(f):
    h = 900
    p = screen(f, "P-43", "Populated", "Listening Practice", h=h)
    y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
    p.zone(14, y + 18, "A")
    p.rect(0, y, DESKTOP_W, 72, fill="#FFFFFF", stroke=LINE, r=0)
    p.rect(40, y + 16, 110, 40, fill="#FFFFFF", stroke=DARK, r=20, value="← Thoát", size=14, bold=True)
    p.chip(420, y + 24, "Listening", "active")
    p.text(520, y + 24, 200, 24, "Section 1 · Practice", size=13, color=MUTED)
    p.progress(720, y + 32, 330, 0)
    p.text(1340, y + 20, 40, 32, "🎧", size=20)
    y += 72
    p.zone(14, y + 24, "B")
    question_map(p, 30, y + 24, n=5, active=3)
    p.zone(244, y + 24, "C")
    p.card(270, y + 24, 640, 160, r=14)
    p.text(290, y + 40, 600, 30, "▶  ━━━━━●━━━━━━━  01:42 / 03:06", size=18, bold=True)
    p.text(290, y + 90, 600, 60, "🔊 volume ━━●━   ·   speed 0.75x / 1x / 1.25x", size=14)
    p.card(270, y + 200, 640, 220, r=14)
    p.text(290, y + 214, 200, 18, "SECTION 1", size=11, color=PRIMARY, bold=True)
    p.text(290, y + 236, 600, 30, "<b>Student services enquiry</b>", size=20)
    p.text(290, y + 276, 600, 120, "Listen to a conversation between a student and a university adviser. The question map follows the current point in the audio; you can also select any question to review its cue.", size=14, color=MUTED, valign="top")
    p.zone(904, y + 24, "D")
    p.card(930, y + 24, 480, 400, r=14)
    p.text(950, y + 40, 400, 26, "<b>Question 3 of 5</b>", size=20, color=PRIMARY)
    p.text(950, y + 80, 440, 40, "The workshop will be held in room ______.<br><i>Write ONE word and/or a number.</i>", size=14, valign="top")
    p.rect(950, y + 140, 440, 44, fill="#FFFFFF", stroke=DARK, r=8, value="Nhập đáp án cho câu 3", color=MUTED, size=13, align="left")
    p.btn(950, y + 200, 160, 40, "⚑ Đánh dấu", "secondary", size=13)
    p.text(270, y + 450, 1100, 40, "Audio, cues and questions are mock data (MOCK_LISTENING_QUESTIONS). No submission/grading yet; “Thoát” → P-41.", size=12, italic=True)
    footer(p, h - 64)


def p44(f):
    h = 940
    p = screen(f, "P-44", "Populated", "Writing Practice", h=h)
    y = navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
    p.zone(14, y + 18, "A")
    p.rect(0, y, DESKTOP_W, 72, fill="#FFFFFF", stroke=LINE, r=0)
    p.rect(40, y + 16, 110, 40, fill="#FFFFFF", stroke=DARK, r=20, value="← Thoát", size=14, bold=True)
    p.text(420, y + 20, 600, 32, "[Writing]  <b>Transport use in a European city</b>", size=16)
    p.text(1340, y + 20, 40, 32, "✎", size=20)
    y += 72
    p.zone(14, y + 24, "B")
    p.card(40, y + 24, 640, 640, r=14)
    p.text(60, y + 40, 300, 18, "WRITING PROMPT", size=11, color=PRIMARY, bold=True)
    p.text(60, y + 62, 600, 30, "<b>Transport use in a European city</b>", size=20)
    p.lines(60, y + 106, 600, 3, gap=22)
    p.text(60, y + 180, 300, 18, "TASK VISUAL", size=11, color=PRIMARY, bold=True)
    p.img(60, y + 206, 600, 300, "Bar chart (SVG) · legend Car / Bus / Bicycle …")
    p.btn(60, y + 524, 220, 44, "⤢ Phóng to biểu đồ", "secondary", size=13)
    p.zone(684, y + 24, "C")
    p.card(710, y + 24, 690, 640, r=14)
    p.text(730, y + 40, 300, 18, "YOUR RESPONSE", size=11, color=PRIMARY, bold=True)
    p.text(730, y + 62, 400, 30, "<b>Write your answer</b>", size=20)
    p.rect(1220, y + 50, 160, 40, fill=PRIMARY_SOFT, stroke=PRIMARY_SOFT, r=20, value="⏱ đếm ngược", color=PRIMARY, size=15, bold=True)
    p.rect(730, y + 110, 650, 440, fill="#FFFFFF", stroke=DARK, r=10, value="Start writing your response here…", color=MUTED, size=14, align="left", valign="top")
    p.text(730, y + 566, 460, 40, "Mục tiêu: ít nhất 150 từ. Bài viết chỉ được giữ trong phiên hiện tại.", size=13, color=MUTED)
    p.text(1200, y + 566, 180, 30, "<b>0</b> / 150 từ", size=14, align="right")
    footer(p, h - 64)


def p44a(f):
    h = 940
    p = screen(f, "P-44a", "Populated", "Writing Chart Zoom", h=h)
    navbar(p, role="learner", active="Luyện tập 4 kỹ năng")
    p.overlay(DESKTOP_W, h)
    p.card(220, 130, 1000, 660, r=18)
    p.zone(207, 150, "A")
    p.text(250, 150, 800, 30, "<b>Transport use in a European city</b>", size=20)
    p.text(1170, 150, 30, 30, "✕", size=18, color=MUTED, align="center")
    p.img(250, 200, 940, 540, "Enlarged bar chart (is-large)")
    p.zone(207, 200, "B")


def build(new):
    f = new("P-41_PracticeCatalog"); p41(f); f.save()
    f = new("P-41a_PracticeModeSelect"); p41a(f); f.save()
    f = new("P-42_ReadingPracticeTest"); p42(f); f.save()
    f = new("P-42a_CreateFlashcard"); p42a(f); f.save()
    f = new("P-42b_DictionaryLookup"); p42b(f); f.save()
    f = new("P-42c_SavedFlashcards"); p42c(f); f.save()
    f = new("P-42d_FloatingNotes"); p42d(f); f.save()
    f = new("P-43_ListeningPractice"); p43(f); f.save()
    f = new("P-44_WritingPractice"); p44(f); f.save()
    f = new("P-44a_WritingChartZoom"); p44a(f); f.save()
