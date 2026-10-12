"""Clickable desktop prototype: every wireframe frame in one draw.io file, buttons linked to their target frames.

Open diagrams/IELTSSpace_SDS_Prototype.drawio (draw.io Desktop or viewer.diagrams.net) and click a button:
draw.io jumps to the linked tab. The first tab is an index; every frame title links back to it.

Links follow the Key Interactions in Part 4 (sds_content_screens.py). Matching is on the visible text of a shape:
  "Label"   — buttons / pills / links (non-text shapes, or text containing →, ←, ✕ or underline)
  "!Label"  — force a plain text cell to be clickable (e.g. a card title)
A value may be a list: the n-th matching shape gets the n-th target (None = leave unlinked).
"""
import os

from sds_screens import by_id
from wf import DrawioFile, Page, page_id, plain, PRIMARY, PRIMARY_SOFT, INK, MUTED, LINE, SOFT, DARK

INDEX = "INDEX / Prototype"

# Navbar / subnav links, applied only to shapes inside the navbar band (y < 120) of every frame.
NAV = {
    "IELTS Space logo": "P-10 / Populated · Learner", "!Trang chủ": "P-10 / Populated · Learner",
    "!Khóa học": "P-30 / Populated", "!Khóa học của tôi": "P-30 / Populated",
    "!Test đầu vào 4 kỹ năng FREE": "P-30a / Survey",
    "!Luyện tập 4 kỹ năng": "P-41 / Populated", "!Listening": "P-41 / Populated", "!Reading": "P-41 / Populated",
    "!Writing": "P-41 / Populated", "!Speaking": "P-41 / Populated",
    "!Sổ từ vựng": "P-92 / Populated", "!Flashcard của tôi": "P-92 / Populated", "!Kho từ vựng": "P-92 / Populated",
    "!Bài mẫu 8đ": "P-92 / Populated", "!Bài mẫu Writing 8.0+": "P-92 / Populated", "!Kết quả học viên": "P-92 / Populated",
    "!Dashboard": "P-92 / Populated", "!Lịch sử nộp bài": "P-92 / Populated",
    "◯ Học viên · 30 Points ▾": "SHELL / User Menu",
    "Đăng nhập": "P-01 / Populated", "Đăng ký": "P-02 / Populated",
}
GUEST_HOME = "P-10 / Populated"

# SiteFooter links (only shapes drawn by chrome.footer, tagged zone='footer').
FOOTER = {"Giới thiệu": "P-10 / Populated", "Khóa học": "P-41 / Populated", "Điều khoản sử dụng": "P-92 / Populated",
          "Chính sách bảo mật": "P-92 / Populated", "Chính sách bản quyền": "P-92 / Populated"}

P01 = {"Đăng nhập": "P-30 / Populated", "Quên mật khẩu?": "P-03 / Populated",
       "Chưa có tài khoản? Đăng ký ngay": "P-02 / Populated"}
P04 = {"Chưa có mã? Yêu cầu lại · Đăng nhập": "P-01 / Populated"}
BACK_P31 = "← Khóa IELTS Band 6.0"
LINKS = {
    "P-01 / Populated": P01, "P-01 / Loading": {"Đang xử lý…": "P-30 / Populated", "Quên mật khẩu?": "P-03 / Populated", "Chưa có tài khoản? Đăng ký ngay": "P-02 / Populated"}, "P-01 / Error": P01,
    "P-02 / Populated": {"Tạo tài khoản": "P-30 / Populated", "Đã có tài khoản? Đăng nhập": "P-01 / Populated"},
    "P-02 / Error": {"Tạo tài khoản": "P-30 / Populated", "Đã có tài khoản? Đăng nhập": "P-01 / Populated"},
    "P-03 / Populated": {"Gửi mã đặt lại": "P-03 / Success", "Quay lại đăng nhập": "P-01 / Populated"},
    "P-03 / Success": {"Gửi lại mã": "P-03 / Success", "Tôi đã có mã — đặt lại mật khẩu · Quay lại đăng nhập": "P-04 / Populated"},
    "P-04 / Populated": dict(P04, **{"Đặt lại mật khẩu": "P-04 / Success"}),
    "P-04 / Success": dict(P04, **{"role=status · Đặt lại mật khẩu thành công. Đang chuyển tới đăng nhập…": "P-01 / Populated"}),
    "P-04 / Error": dict(P04, **{"Đặt lại mật khẩu": "P-04 / Success"}),
    "P-10 / Populated": {"Khám phá bài luyện →": "P-01 / Populated", "Kích hoạt": "P-10c / Populated", "Nạp Key": "P-10c / Populated"},
    "P-10 / Populated · Learner": {"Khám phá bài luyện →": "P-41 / Populated", "Kích hoạt": "P-10c / Populated", "Nạp Key": "P-10c / Populated"},
    "P-10c / Populated": {"✕": "P-10 / Populated · Learner"},
    "P-30 / Populated": {"Học tiếp →": "P-30b / Populated", "Xem lại lộ trình →": "P-30b / Populated",
                         "Bắt đầu học →": "P-30b / Populated", "Làm test đầu vào →": "P-30a / Survey",
                         "Gợi ý cho bạn 1": "P-30 / Empty", "Đã hoàn thành 1": "P-30 / Empty", "Đang học 1": "P-30 / Empty"},
    "P-30 / Empty": {"Xem tất cả khóa học": "P-30 / Populated"},
    "P-30 / Error": {"↻ Thử lại": "P-30 / Populated"},
    "P-30 / Loading": {"!Lộ trình IELTS theo band mục tiêu": "P-30 / Populated"},
    "P-30a / Survey": {"Dưới 1 tiếng": "P-30a / Survey Summary", "Khoảng 1 – 2 tiếng": "P-30a / Survey Summary",
                       "Khoảng 2 – 3 tiếng": "P-30a / Survey Summary", "Trên 3 tiếng": "P-30a / Survey Summary"},
    "P-30a / Survey Summary": {"Tiếp tục": "P-30a / Test Hub"},
    "P-30a / Test Hub": {"Làm tiếp": "P-30a / Exam — Reading / Listening",
                         "Làm bài": [None, "P-30a / Exam — Writing", "P-30a / Exam — Writing", "P-30a / Exam — Speaking"],
                         "↺ Làm lại khảo sát": "P-30a / Survey"},
    "P-30a / Exam — Reading / Listening": {"✕": "P-30a / Test Hub", "✓ Nộp phần này": "P-30a / Test Hub", "Làm tiếp": "P-30a / Exam — Reading / Listening"},
    "P-30a / Exam — Writing": {"✕": "P-30a / Test Hub", "✓ Hoàn thành": "P-30a / Grading"},
    "P-30a / Exam — Speaking": {"✕": "P-30a / Test Hub", "Dừng & sang câu tiếp": "P-30a / Grading", "Tiếp tục →": "P-30a / Exam — Speaking",
                                "Bắt đầu": "P-30a / Exam — Speaking"},
    "P-30a / Grading": {"!Đang chấm bài của bạn…": "P-30a / Result Report"},
    "P-30a / Result Report": {"Bắt đầu học →": "P-30b / Populated", "Xem khóa học →": "P-30b / Populated"},
    "P-30b / Populated": {"← Tất cả khóa học": "P-30 / Populated", "Học tiếp →": "P-31 / Populated", "Xem lại →": "P-31 / Populated",
                          "!AVAILABLE → “Làm bài thi cuối khóa” button (→ P-35 ?course=) PASSED → “Đã đạt” chip": "P-35 / Populated"},
    "P-30b / Loading": {"← Tất cả khóa học": "P-30 / Populated"},
    "P-30b / Empty / Error": {"← Tất cả khóa học": "P-30 / Populated", "↻ Thử lại": "P-30b / Populated"},
    "P-31 / Populated": {BACK_P31: "P-30b / Populated", "Học tiếp →": "P-32 / Populated", "Xem lại →": "P-32 / Completed",
                         "○ Hoàn thành 2 bài học còn lại": "P-32 / Populated",
                         "!AVAILABLE → “Làm bài kiểm tra” button (“Đang giao đề…”) → P-35 PASSED → “Đã đạt” chip · NONE → “Chặng này không có bài kiểm tra cuối.”": "P-35 / Populated"},
    "P-31 / Locked Test / Review Gate": {BACK_P31: "P-30b / Populated", "Làm bài ôn →": "P-34 / Theory", "Xem lại →": "P-32 / Completed",
                                         "🏋 Cần luyện thêm trước khi mở bài kiểm tra chặng — Luyện ngay → (P-33)": "P-33 / Set List",
                                         "○ Hoàn thành 1 bài học còn lại": "P-32 / Populated",
                                         "○ Hoàn thành phần luyện thêm của 1 bài": "P-33 / Set List",
                                         "○ Làm 1 bài ôn bắt buộc": "P-34 / Theory"},
    "P-32 / Populated": {"← Matching Headings": "P-31 / Populated", "Hoàn thành bài": "P-32 / Completed", "Nộp": "P-32 / Completed",
                         "Danh sách bài của chặng": "P-31 / Populated"},
    "P-32 / Completed": {"← Matching Headings": "P-31 / Populated", "Bài tiếp theo →": "P-32 / Populated",
                         "Luyện thêm bài này": "P-33 / Set List", "Về danh sách bài": "P-31 / Populated", "Danh sách bài của chặng": "P-31 / Populated"},
    "P-32 / Review Required": {"← Matching Headings": "P-31 / Locked Test / Review Gate", "Làm bài ôn": "P-34 / Theory", "Làm bài ôn →": "P-34 / Theory",
                               "Về danh sách bài": "P-31 / Locked Test / Review Gate", "Danh sách bài của chặng": "P-31 / Locked Test / Review Gate"},
    "P-33 / Set List": {"← Bài 2: Xác định ý chính của đoạn": "P-32 / Completed", "Làm bộ này": "P-33 / Attempt", "Làm lại": "P-33 / Attempt"},
    "P-33 / Attempt": {"← Danh sách luyện thêm": "P-33 / Set List", "Nộp": "P-33 / Outcome"},
    "P-33 / Outcome": {"← Danh sách luyện thêm": "P-33 / Set List", "Tiếp tục luyện / về topic": "P-31 / Populated",
                       "Về danh sách": "P-33 / Set List", "Làm bài ôn →": "P-34 / Theory"},
    "P-34 / Theory": {"← Về lộ trình học": "P-31 / Populated", "Nộp": "P-34 / Practice Set"},
    "P-34 / Practice Set": {"← Về lộ trình học": "P-31 / Populated", "Nộp": "P-34 / Finished", "↻ Làm bộ tiếp theo": "P-34 / Practice Set"},
    "P-34 / Finished": {"← Về lộ trình học": "P-31 / Populated", "Học tiếp →": "P-31 / Populated"},
    "P-35 / Populated": {"← Về topic (course test: “Về khóa học”)": "P-31 / Populated", "➤ Nộp bài": "P-35 / Confirm Submit",
                         "Phần tiếp →": "P-35 / Populated"},
    "P-35 / Confirm Submit": {"← Về topic (course test: “Về khóa học”)": "P-31 / Populated", "➤ Vẫn nộp bài": "P-36 / Passed"},
    "P-36 / Passed": {"🏆 Sang chặng tiếp theo →": "P-31 / Populated", "Xem danh sách khóa": "P-30 / Populated"},
    "P-36 / Failed": {"↺ Về topic để làm lại": "P-31 / Populated"},
    "P-41 / Populated": {"▶ TEST NGAY ✦": "P-41a / Populated", "!Vitamins - To supplement or not?": "P-41a / Populated",
                         "!The Evolutionary Mystery: Crocodile…": "P-41a / Populated", "!How Does Watching Sports Influenc…": "P-41a / Populated"},
    "P-41a / Populated": {"✕": "P-41 / Populated", "BẮT ĐẦU LÀM BÀI": "P-42 / Exam Mode", "Thi thử": "P-42 / Exam Mode",
                          "!Luyện tập": "P-42 / Practice Mode"},
    "P-42 / Practice Mode": {"← Thoát": "P-41 / Populated", "▤ Tạo Flashcard": "P-42a / Populated", "文 Tra từ vựng": "P-42b / Populated",
                             "!▤ Thẻ đã lưu": "P-42c / Populated", "🗒 Note": "P-42d / Populated", "🗒 Ghi chú (0)": "P-42d / Populated"},
    "P-42 / Exam Mode": {"← Thoát": "P-41 / Populated"},
    "P-42a / Populated": {"✕": "P-42 / Practice Mode", "Lưu Flashcard": "P-42 / Practice Mode", "← Thoát": "P-41 / Populated"},
    "P-42b / Populated": {"✕": "P-42 / Practice Mode", "← Thoát": "P-41 / Populated"},
    "P-42c / Populated": {"✕": "P-42 / Practice Mode", "← Thoát": "P-41 / Populated"},
    "P-42d / Populated": {"⠿ Ghi chú — ✕": "P-42 / Practice Mode", "Lưu ghi chú": "P-42d / Populated", "← Thoát": "P-41 / Populated"},
    "P-43 / Populated": {"← Thoát": "P-41 / Populated"},
    "P-44 / Populated": {"← Thoát": "P-41 / Populated", "⤢ Phóng to biểu đồ": "P-44a / Populated"},
    "P-44a / Populated": {"✕": "P-44 / Populated"},
    "P-90 / Populated": {"Về Overview": INDEX},
    "P-91 / Populated": {"Về lộ trình": "P-30 / Populated"},
    "P-92 / Populated": {"Về Overview": INDEX, "Đến lớp học": INDEX},
    "SHELL / User Menu": {"Đăng xuất": "P-01 / Populated", "Chuyển sang Premium (demo)": "SHELL / User Menu"},
}

START = [("▶ Start as Guest — Home", "P-10 / Populated"), ("▶ Start as Guest — Sign in", "P-01 / Populated"),
         ("▶ Start as Learner — Course List", "P-30 / Populated"), ("▶ Placement test", "P-30a / Survey"),
         ("▶ Practice catalog", "P-41 / Populated")]


def clickable(cell):
    """Buttons, pills, cards and link-like text; plain headings stay inert unless forced with '!'."""
    if not cell["style"].startswith("text;"):
        return True
    text = cell["value"]
    return "<u>" in text or any(sym in text for sym in ("→", "←", "✕"))


def apply_links(page, names):
    missing = []
    rules = [(key, val, None) for key, val in LINKS.get(page.name, {}).items()]
    rules += [(key, val, "nav") for key, val in NAV.items()]
    rules += [(key, val, "footer") for key, val in FOOTER.items()]
    learner = any(plain(c["value"]) == "◯ Học viên · 30 Points ▾" for c in page.vertices())
    used = {}
    for cell in page.vertices():
        text = plain(cell["value"])
        if not text:
            continue
        if cell["id"] == page.caption_id:
            cell["link"] = page_id(INDEX)
            continue
        in_footer = cell.get("zone") == "footer"
        for key, target, scope in rules:
            forced = key.startswith("!")
            label = key[1:] if forced else key
            if text != label or not (forced or clickable(cell)):
                continue
            if (scope == "footer") != in_footer or (scope == "nav" and not (0 <= cell["y"] < 120)):
                continue
            if isinstance(target, list):
                n = used.get(key, 0)
                used[key] = n + 1
                target = target[n] if n < len(target) else None
            if scope == "nav" and target == "P-10 / Populated · Learner" and not learner:
                target = GUEST_HOME
            if target:
                if target not in names:
                    missing.append(f"{page.name}: '{key}' -> unknown frame '{target}'")
                else:
                    cell["link"] = page_id(target)
            break
    found = {plain(c["value"]) for c in page.vertices()}
    for key in LINKS.get(page.name, {}):
        if key.lstrip("!") not in found:
            missing.append(f"{page.name}: label not found '{key}'")
    return missing


def index_page(names):
    p = Page(INDEX)
    p.text(0, 0, 1500, 40, "<b>IELTS Space — Clickable Desktop Prototype</b>", size=28)
    p.text(0, 46, 1500, 44, "Click a box to open a frame. Inside a frame, click buttons / links to move between screens; "
           "click the frame title (top-left) to come back here. Generated from the SDS wireframes (Report 3.2 v0.4.0).", size=14, color=MUTED)
    for i, (label, target) in enumerate(START):
        cid = p.rect(i * 300, 110, 280, 48, fill=PRIMARY, stroke=PRIMARY, r=10, value=label, color="#FFFFFF", bold=True, size=14)
        p.vertices()[-1]["link"] = page_id(target)
    groups = {}
    for name in names:
        groups.setdefault(name.split(" / ")[0], []).append(name)
    y, col_w = 200, 760
    row_h = 0
    for gi, (ident, frames) in enumerate(groups.items()):
        x = (gi % 2) * col_w
        if gi % 2 == 0 and gi:
            y += row_h
        if gi % 2 == 0:
            row_h = 0
        title = "Shared layout" if ident == "SHELL" else by_id(ident).name
        p.text(x, y, 105, 34, f"<b>{ident}</b><br><font style='font-size:11px'>{title}</font>", size=13, valign="top")
        for k, name in enumerate(frames):
            bx, by = x + 110 + (k % 3) * 210, y + (k // 3) * 42
            p.rect(bx, by, 200, 34, fill=PRIMARY_SOFT, stroke=PRIMARY, r=6, value=name.split(" / ", 1)[1], size=12, color=INK)
            p.vertices()[-1]["link"] = page_id(name)
        row_h = max(row_h, ((len(frames) - 1) // 3 + 1) * 42 + 14)
    return p


class _Collector:
    """Stands in for DrawioFile so the screen builders add their pages to one prototype file."""

    def __init__(self, target):
        self.target = target

    def page(self, name):
        return self.target.page(name)

    def save(self):
        pass


def build(path, builders):
    proto = DrawioFile(path)
    collector = _Collector(proto)
    for fn in builders:
        fn(lambda stem: collector)
    names = [p.name for p in proto.pages]
    problems = []
    for page in proto.pages:
        problems += apply_links(page, set(names) | {INDEX})
    proto.pages.insert(0, index_page(names))
    proto.save()
    linked = sum(1 for p in proto.pages for c in p.vertices() if c["link"])
    return len(proto.pages), linked, problems
