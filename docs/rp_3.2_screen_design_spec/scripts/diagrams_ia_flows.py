"""Information architecture (site map, navigation) and screen flows F-01…F-05 as draw.io files."""
from wf import PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, DARK, MEDIA, FONT
from sds_screens import SCREENS, GROUPS

STATUS_STYLE = {
    "MVP": dict(fill="#FFFFFF", stroke=PRIMARY, sw=2, dashed=False),
    "MVP·mock": dict(fill=PRIMARY_SOFT, stroke=PRIMARY, sw=2, dashed=False),
    "Later*": dict(fill="#FFFFFF", stroke=DARK, sw=1, dashed=True),
    "Stub": dict(fill=SOFT, stroke=MUTED, sw=1, dashed=True),
    "Later": dict(fill=SOFT, stroke=MUTED, sw=1, dashed=True),
    "Retired": dict(fill=MEDIA, stroke=MUTED, sw=1, dashed=True),
}


def legend(p, x, y, items):
    for i, (label, st) in enumerate(items):
        p.rect(x, y + i * 34, 40, 24, fill=st["fill"], stroke=st["stroke"], sw=st["sw"], dashed=st["dashed"], r=6)
        p.text(x + 50, y + i * 34, 300, 24, label, size=13, color=INK)


def title(p, text, sub, w):
    p.text(0, -90, w, 32, f"<b>{text}</b>", size=22)
    p.text(0, -56, w, 22, sub, size=13, color=MUTED)
    p.rect(0, -28, w, 2, fill=LINE, stroke=LINE, r=0)


# --------------------------------------------------------------------------- site map

def site_map(f):
    p = f.page("IA-01 / Site Map")
    colw, gap = 230, 26
    total = len(GROUPS) * (colw + gap)
    title(p, "IELTS Space — Site Map (Part 2.2)", "Source: frontend/src/app/App.tsx · box = screen [P-ID] + route · border = status (see legend)", total)
    root = p.rect(total / 2 - 160, 0, 320, 56, fill=PRIMARY, stroke=PRIMARY, r=10,
                  value="<b>IELTS Space web app</b><br>/ → /home (Guest) · /overview (Learner)", color="#FFFFFF", size=13)
    for gi, g in enumerate(GROUPS):
        x = gi * (colw + gap)
        head = p.rect(x, 110, colw, 44, fill=INK, stroke=INK, r=8, value=f"<b>{g}</b>", color="#FFFFFF", size=14)
        p.edge(root, head, exit=(0.5, 1), entry=(0.5, 0))
        y = 170
        for s in [s for s in SCREENS if s.group == g]:
            st = STATUS_STYLE[s.status]
            route = s.route if len(s.route) < 34 else s.route[:32] + "…"
            modal = " ◇" if s.level == "MODAL" else ""
            p.rect(x, y, colw, 62, fill=st["fill"], stroke=st["stroke"], sw=st["sw"], dashed=st["dashed"], r=14 if s.level == "MODAL" else 6,
                   value=f"<b>[{s.pid}] {s.name}</b>{modal}<br><font style='font-size:11px' color='#4B5563'>{route} · {s.status}</font>",
                   size=12, align="left")
            y += 72
    legend(p, 6 * 256, 560, [("MVP — built, wired to real API/auth", STATUS_STYLE["MVP"]),
                       ("MVP·mock — built, FE mock data", STATUS_STYLE["MVP·mock"]),
                       ("Later* — UI with mock data, outside Part 4", STATUS_STYLE["Later*"]),
                       ("Stub / Later — RouteStatusPage or flag off", STATUS_STYLE["Stub"])])
    p.text(6 * 256, 710, 480, 120, "◇ = Modal / panel (no route of its own) · Retired = in v0.3.0, removed from App.tsx (P-10a, P-10b, P-60)<br>Roles: Guest (signed out) · Learner (CUSTOMER). RequireAuth sends a Guest to P-01 with state.from.",
           size=13, color=INK, valign="top")


def navigation(f):
    p = f.page("IA-02 / Navigation")
    title(p, "SiteNavbar — Guest vs Learner (Part 2.1 / 2.3)", "Source: frontend/src/components/SiteNavbar.tsx · primary item → subnav section → target screen", 1500)
    cols = [
        ("Guest (signed out)", [
            ("Trang chủ", [("—", "P-10")]),
            ("Khóa học", [("Test đầu vào 4 kỹ năng FREE", "P-30a 🔒"), ("Khóa học", "P-30 🔒")]),
            ("Luyện tập 4 kỹ năng", [("Listening / Reading / Writing / Speaking", "P-41?skill= 🔒")]),
            ("Bài mẫu Writing 8.0+", [("—", "P-52 🔒")]),
            ("Kết quả học viên", [("—", "P-53 🔒")]),
            ("[Đăng nhập] [Đăng ký]", [("right-hand buttons", "P-01 / P-02")]),
        ]),
        ("Learner (CUSTOMER)", [
            ("Trang chủ", [("Dashboard", "P-11"), ("Lịch sử nộp bài", "P-54"), ("Khóa học của tôi", "P-30")]),
            ("Khóa học", [("Test đầu vào 4 kỹ năng FREE", "P-30a"), ("Khóa học", "P-30")]),
            ("Luyện tập 4 kỹ năng", [("Listening / Reading / Writing / Speaking", "P-41?skill=")]),
            ("Sổ từ vựng", [("Flashcard của tôi", "P-51"), ("Kho từ vựng", "P-50"), ("Bài mẫu 8đ", "P-52")]),
            ("Kết quả học viên", [("—", "P-53")]),
            ("◯ Học viên · Points ▾", [("Simulated plan · Đăng xuất", "→ P-01")]),
        ]),
    ]
    for ci, (head, items) in enumerate(cols):
        x0 = ci * 780
        p.rect(x0, 0, 720, 44, fill=PRIMARY, stroke=PRIMARY, r=8, value=f"<b>{head}</b>", color="#FFFFFF", size=15)
        y = 64
        for label, children in items:
            h = 40 * len(children) + 12
            main = p.rect(x0, y, 220, h, fill="#FFFFFF", stroke=PRIMARY, sw=2, r=8, value=f"<b>{label}</b>", size=13)
            for k, (child, target) in enumerate(children):
                c = p.rect(x0 + 270, y + 6 + k * 40, 290, 32, fill=PRIMARY_SOFT if child != "—" else SOFT, stroke=LINE, r=6, value=child, size=12)
                t = p.rect(x0 + 600, y + 6 + k * 40, 120, 32, fill=SOFT, stroke=DARK, r=6, value=f"<b>{target}</b>", size=12)
                p.edge(main, c, exit=(1, 0.5), entry=(0, 0.5))
                p.edge(c, t, exit=(1, 0.5), entry=(0, 0.5))
            y += h + 16
    p.text(0, 720, 1500, 60, "🔒 = RequireAuth: a Guest is sent to P-01 and returns to the original route (state.from) after sign-in, default /learn (P-30). "
           "Desktop > 1240px: link row + sliding indicator; ≤ 1240px: ☰ button opens a link grid.", size=13, valign="top")


def build_ia(new):
    f = new("IELTSSpace_SDS_InformationArchitecture")
    site_map(f)
    navigation(f)
    f.save()


# --------------------------------------------------------------------------- flows

class Flow:
    """Grid helper: col/row → absolute coordinates."""
    CW, RH = 270, 140

    def __init__(self, p):
        self.p = p

    def xy(self, c, r):
        return c * self.CW, r * self.RH

    def screen(self, c, r, pid, name, status="MVP"):
        x, y = self.xy(c, r)
        st = STATUS_STYLE[status]
        return self.p.rect(x, y, 210, 70, fill=st["fill"], stroke=st["stroke"], sw=st["sw"], dashed=st["dashed"], r=10,
                           value=f"<b>{pid}</b><br>{name}", size=13)

    def decision(self, c, r, text):
        x, y = self.xy(c, r)
        st = f"rhombus;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor={DARK};fontFamily={FONT};fontSize=12;fontColor={INK};"
        return self.p.cell(text, st, x + 5, y - 10, 200, 90)

    def terminal(self, c, r, text, end=False):
        x, y = self.xy(c, r)
        fill = INK if end else "#FFFFFF"
        st = (f"ellipse;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={INK};fontFamily={FONT};fontSize=12;"
              f"fontColor={'#FFFFFF' if end else INK};")
        return self.p.cell(text, st, x + 5, y, 200, 70)

    def note(self, c, r, text, w=210, h=70):
        x, y = self.xy(c, r)
        st = f"shape=note;size=14;whiteSpace=wrap;html=1;fillColor={SOFT};strokeColor={MUTED};fontFamily={FONT};fontSize=11;fontColor={INK};align=left;spacingLeft=6;"
        return self.p.cell(text, st, x, y, w, h)

    def go(self, a, b, label="", **kw):
        return self.p.edge(a, b, label, **kw)


def flow_page(f, fid, name, sub, w=1900):
    p = f.page(f"{fid} / {name}")
    title(p, f"Flow [{fid}] — {name}", sub, w)
    legend(p, w - 360, -10, [("MVP screen", STATUS_STYLE["MVP"]), ("MVP·mock screen", STATUS_STYLE["MVP·mock"]),
                             ("Later* / Stub", STATUS_STYLE["Later*"])])
    p.text(w - 360, 100, 360, 40, "◇ decision · ⬭ entry / end · arrow label = action / route", size=11, color=MUTED)
    return p, Flow(p)


def f01(f):
    p, g = flow_page(f, "F-01", "Authentication", "SC-01 · FT-01…FT-05 · Guest → Learner", 1700)
    start = g.terminal(0, 1, "Guest opens /login<br>or is blocked by RequireAuth")
    s1 = g.screen(1, 1, "P-01", "Sign In")
    d1 = g.decision(2, 1, "Email + password<br>valid?")
    d2 = g.decision(3, 1, "state.from present?")
    end_from = g.terminal(4, 0, "Original route (from)", end=True)
    end_learn = g.terminal(4, 2, "P-30 Course List<br>(/learn)", end=True)
    s2 = g.screen(1, 3, "P-02", "Sign Up")
    s3 = g.screen(2, 4, "P-03", "Forgot Password")
    s4 = g.screen(3, 4, "P-04", "Reset Password")
    s5 = g.screen(0, 3, "P-05", "OAuth Callback", "Later")
    g.go(start, s1)
    g.go(s1, d1, "Đăng nhập")
    g.go(d1, s1, "No: role=status error", exit=(0.5, 0), entry=(0.5, 0), waypoints=[(645, 95), (375, 95)])
    g.go(d1, d2, "Yes")
    g.go(d2, end_from, "Yes")
    g.go(d2, end_learn, "No")
    g.go(s1, s2, "Đăng ký ngay", exit=(0.4, 1), entry=(0.4, 0))
    g.go(s2, s1, "Đăng nhập", exit=(0.6, 0), entry=(0.6, 1))
    g.go(s2, end_learn, "Tạo tài khoản → auto sign-in", exit=(1, 0.5), entry=(0, 0.5), waypoints=[(1080, 455)])
    g.go(s1, s3, "Quên mật khẩu?", exit=(0.9, 1), entry=(0, 0.5))
    g.go(s3, s4, "Tôi đã có mã")
    g.go(s4, s1, "Success → 0.9 s", exit=(0.5, 1), entry=(0.2, 1), waypoints=[(915, 680), (312, 680)])
    g.go(s4, s3, "Yêu cầu lại", exit=(0.2, 1), entry=(0.8, 1), waypoints=[(852, 640), (708, 640)])
    g.go(s1, s5, "Google (flag off)", dashed=True, exit=(0, 0.8), entry=(0.5, 0))
    g.note(4, 4, "GuestOnly: a signed-in Learner opening /login, /register, /forgot-password, /reset-password, /auth/oauth/callback → P-30.", w=300, h=80)


def f02(f):
    p, g = flow_page(f, "F-02", "Course-based Learning Path", "SC-01, SC-02, SC-03, SC-05 · FT-20…FT-29, FT-35, FT-55", 1900)
    start = g.terminal(0, 0, "Nav “Khóa học”<br>or after sign-in")
    c30 = g.screen(1, 0, "P-30", "Course List")
    dpl = g.decision(2, 0, "API returns<br>PLACEMENT_REQUIRED?")
    c30a = g.screen(3, 0, "P-30a", "Placement Test (F-05)")
    c30b = g.screen(1, 2, "P-30b", "Topic List (course)")
    final = g.note(0, 2, "<b>Course final test</b> (course AVAILABLE) → P-35 ?course= → P-36 → Về khóa học (P-30b)", w=220, h=80)
    dlock = g.decision(1, 3, "Topic LOCKED?")
    nonav = g.note(0, 3.6, "No navigation; lock reason / Premium chip shown", w=220, h=60)
    c31 = g.screen(1, 4.5, "P-31", "Topic Detail")
    c32 = g.screen(2, 4.5, "P-32", "Lesson Player")
    dprac = g.decision(3, 4.5, "Lesson needs<br>extra practice?")
    c33 = g.screen(4, 4.5, "P-33", "Lesson Practice")
    c34 = g.screen(4, 6, "P-34", "Review Session")
    dtest = g.decision(1, 8, "Topic test<br>AVAILABLE?")
    c35 = g.screen(2, 8, "P-35", "Topic / Course Test")
    c36 = g.screen(3, 8, "P-36", "Test Result")
    dpass = g.decision(4, 8, "Score ≥ 70%?")
    retry = g.terminal(5, 8, "Failed: retry from topic (P-31) / course (P-30b)", end=True)
    g.go(start, c30)
    g.go(c30, dpl, "load courses")
    g.go(dpl, c30a, "Yes (notice)")
    g.go(dpl, c30b, "No → pick a course card", exit=(0.5, 1), entry=(0.5, 0))
    g.go(c30a, c30b, "Report: “Bắt đầu học” / “Xem khóa học”", exit=(0.5, 1), entry=(1, 0.5), waypoints=[(915, 315)])
    g.go(c30b, final, "", dashed=True, exit=(0, 0.5), entry=(1, 0.5))
    g.go(c30b, dlock, "pick a topic")
    g.go(dlock, nonav, "Yes", exit=(0, 0.5), entry=(0.5, 0))
    g.go(dlock, c31, "No")
    g.go(c31, c32, "Học tiếp")
    g.go(c32, dprac, "Hoàn thành bài")
    g.go(dprac, c33, "Yes")
    g.go(dprac, c31, "No → next lesson / back to topic", exit=(0.5, 1), entry=(0.5, 1), waypoints=[(915, 740), (375, 740)])
    g.go(c33, c34, "review created")
    g.go(c32, c34, "REVIEW_REQUIRED → “Làm bài ôn”", dashed=True, exit=(0.5, 1), entry=(0, 0.5), waypoints=[(645, 875)])
    g.go(c34, c31, "Done / skipped → “Học tiếp”", exit=(0.5, 1), entry=(0, 0.5), waypoints=[(1185, 990), (200, 990), (200, 665)])
    g.go(c31, dtest, "Kiểm tra cuối chặng")
    g.go(dtest, c35, "Yes")
    g.go(c35, c36, "Nộp bài")
    g.go(c36, dpass)
    g.go(dpass, retry, "No")
    g.go(dpass, c31, "Passed → “Sang chặng tiếp theo”", exit=(0.5, 1), entry=(0, 0.8), waypoints=[(1185, 1260), (150, 1260), (150, 686)])
    g.note(5, 2, "Lock redirect (ApiErrorState): TOPIC_LOCKED → P-30 · LESSON_LOCKED / TEST_LOCKED → P-31 · NOT_FOUND → P-91", w=280, h=80)


def f03(f):
    p, g = flow_page(f, "F-03", "Practice Catalog to Skill Workspace", "FT-34, FT-42, FT-43 · FE mock data", 2000)
    start = g.terminal(0, 1.2, "Nav “Luyện tập 4 kỹ năng”<br>/ Home “Khám phá bài luyện”")
    c41 = g.screen(1, 1.2, "P-41", "Practice Catalog", "MVP·mock")
    c41a = g.screen(2, 1.2, "P-41a", "Mode Select", "MVP·mock")
    dsk = g.decision(3, 1.2, "Card skill?")
    c42 = g.screen(4, 0, "P-42", "Reading (?mode=)", "MVP·mock")
    c43 = g.screen(4, 1.2, "P-43", "Listening", "MVP·mock")
    c44 = g.screen(4, 2.4, "P-44", "Writing", "MVP·mock")
    c45 = g.screen(4, 3.6, "P-45", "Speaking", "Later*")
    tools = g.screen(5, 0, "P-42a / b / c / d", "Flashcard · Dictionary · Saved cards · Notes", "MVP·mock")
    c44a = g.screen(5, 2.4, "P-44a", "Chart Zoom", "MVP·mock")
    g.go(start, c41)
    g.go(c41, c41a, "pick card / TEST NGAY")
    g.go(c41a, c41, "close ✕", exit=(0.5, 1), entry=(0.5, 1), waypoints=[(645, 280), (375, 280)])
    g.go(c41a, dsk, "BẮT ĐẦU")
    g.go(dsk, c42, "reading", exit=(0.5, 0), entry=(0, 0.5), waypoints=[(915, 35)])
    g.go(dsk, c43, "listening")
    g.go(dsk, c44, "writing", exit=(0.5, 1), entry=(0, 0.5), waypoints=[(915, 371)])
    g.go(dsk, c45, "speaking", dashed=True, exit=(0.75, 0.75), entry=(0, 0.5), waypoints=[(1010, 539)])
    g.go(c42, tools, "mode=practice")
    g.go(c44, c44a, "Phóng to biểu đồ")
    g.note(0, 3.2, "Every workspace (P-42…P-45): “← Thoát” → P-41. The banner TEST NGAY ✦ opens P-41a for the first card (not the placement test) — see Appendix B.", w=520, h=70)


def f04(f):
    p, g = flow_page(f, "F-04", "Guest Home to Auth Gate", "SC-01, SC-06 · FT-02, FT-10, FT-11", 2000)
    start = g.terminal(0, 1.1, "Open /")
    d = g.decision(1, 1.1, "Signed in?")
    c11 = g.screen(2, 0, "P-11", "Overview", "Later*")
    c10 = g.screen(2, 2.2, "P-10", "Home (Guest)")
    gate = g.decision(3, 2.2, "Route<br>RequireAuth?")
    c01 = g.screen(4, 2.2, "P-01", "Sign In (state.from)")
    back = g.terminal(5, 2.2, "Back to original route<br>(e.g. P-41, P-30)", end=True)
    c02 = g.screen(4, 0.8, "P-02", "Sign Up")
    c10c = g.screen(3, 4, "P-10c", "Key Activation")
    c23 = g.screen(1, 4, "P-23", "Contact Mentor", "Stub")
    g.go(start, d)
    g.go(d, c11, "Yes", exit=(0.5, 0), entry=(0, 0.5), waypoints=[(375, 35)])
    g.go(d, c10, "No", exit=(0.5, 1), entry=(0, 0.5), waypoints=[(375, 343)])
    g.go(c10, gate, "CTA / nav")
    g.go(gate, c01, "Yes")
    g.go(c01, back, "Đăng nhập (F-01)")
    g.go(c10, c02, "Đăng ký (nav)", exit=(0.8, 0), entry=(0, 0.5), waypoints=[(708, 147)])
    g.go(c10, c10c, "Kích hoạt / Nạp Key", exit=(0.8, 1), entry=(0, 0.5), waypoints=[(708, 595)])
    g.go(c10c, c01, "Guest: Đăng nhập", exit=(1, 0.5), entry=(0.5, 1), waypoints=[(1185, 595)])
    g.go(c10, c23, "Xem hồ sơ chuyên gia", dashed=True, exit=(0.3, 1), entry=(0.5, 0), waypoints=[(603, 480), (375, 480)])
    g.note(5, 3.3, "“Gặp gỡ Mentor” = anchor #home-mentors (scroll). /vocabulary (P-50) is public but only linked from the Learner navbar.", w=320, h=80)


def f05(f):
    p, g = flow_page(f, "F-05", "Placement Test", "SC-01 · FT-09 Learning goal · FT-35 Placement test · BR-36 taken once", 1900)
    start = g.terminal(0, 2, "Open /learn/placement<br>(or PLACEMENT_REQUIRED)")
    d0 = g.decision(1, 2, "Current attempt?")
    sv = g.screen(2, 0, "P-30a", "Survey (3 questions)")
    sm = g.screen(3, 0, "P-30a", "Survey Summary (modal)")
    hub = g.screen(3, 2, "P-30a", "Test Hub")
    ex = g.screen(5, 2, "P-30a", "Exam shell (L/R/W/S)")
    dl = g.decision(4, 3.2, "Last section<br>submitted?")
    gr = g.screen(2, 4.4, "P-30a", "Grading (poll)")
    rr = g.screen(3, 4.4, "P-30a", "Result Report")
    end = g.terminal(4, 4.4, "P-30b recommended course", end=True)
    g.go(start, d0)
    g.go(d0, sv, "None / EXPIRED / CANCELLED", exit=(0.5, 0), entry=(0, 0.5), waypoints=[(375, 35)])
    g.go(d0, hub, "IN_PROGRESS")
    g.go(d0, gr, "Submitted", exit=(0.5, 1), entry=(0, 0.5), waypoints=[(375, 651)])
    g.go(sv, sm, "pick band → save goal")
    g.go(sm, hub, "“Tiếp tục” → create attempt")
    g.go(hub, sv, "↺ Làm lại khảo sát", dashed=True, exit=(0.15, 0), entry=(0.5, 1), waypoints=[(842, 200), (645, 200)])
    g.go(hub, ex, "Làm bài / Làm tiếp", exit=(1, 0.3), entry=(0, 0.3))
    g.go(ex, hub, "✕ Lưu và quay lại", exit=(0, 0.8), entry=(1, 0.8))
    g.go(ex, dl, "✓ Nộp phần này (confirm)", exit=(0.5, 1), entry=(1, 0.5))
    g.go(dl, hub, "No", exit=(0, 0.5), entry=(0.5, 1))
    g.go(dl, gr, "Yes → submit attempt", exit=(0.5, 1), entry=(0.5, 0), waypoints=[(1185, 560), (645, 560)])
    g.go(gr, rr, "band available")
    g.go(rr, end, "Bắt đầu học")
    g.note(0, 3.6, "PLACEMENT_ALREADY_DONE (no own attempt) → redirect P-30 · NO_PLACEMENT_TEST → “Chưa có bài kiểm tra đầu vào nào được mở…”", w=230, h=100)
    g.note(5, 4.2, "Grading: polls GET /courses 45 × 2 s; timeout → “Kết quả đang được chấm”; error → “Chưa lấy được kết quả” + “Kiểm tra lại”.", w=300, h=90)


def build_flows(new):
    f = new("IELTSSpace_SDS_Flows")
    for fn in (f01, f02, f03, f04, f05):
        fn(f)
    f.save()
