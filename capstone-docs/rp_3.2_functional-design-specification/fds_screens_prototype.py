"""Frontend screens built on mock data with no backend counterpart in the SRS yet (OQ-17)."""
from fds_screens import EX_SESSION, screen

REQUIRE_AUTH = ("Session exists (RequireAuth, GG-01)", "Continue rendering",
                "Redirect → /login (state.from kept); GLB-01")
EX_TOPBAR = ("User picks an item in the top bar (SiteNavbar)", "Any", "Selected route", "Typed answers are lost")
MOCK_RULE = ("OQ-17", "Prototype on mock data", "No business rule is enforced; data resets on reload")

SCREENS = [
    # ------------------------------------------------------------------ Practice Test Catalog
    screen(
        key="PracticeCatalog", name="Practice Test Catalog", route="/practice-tests · RequireAuth",
        roles="Any signed-in user", ft="— (no SRS feature; closest FT-34 Mock test)", uc="—", fe="mock",
        status="Prototype (mock data, OQ-17)",
        frontend="Built on mock data — features/practice/pages/PracticeCatalogPage.tsx with mocks/practiceData.ts; no "
                 "API call.",
        purpose="Lets the learner browse practice tests by skill and open one in practice or exam mode.",
        nav_from=[("Top bar", "User clicks \"Luyện đề\""), ("Practice Workspaces", "User clicks \"Thoát\"")],
        nav_to=[("Practice Workspaces", "User picks a test and a mode")],
        pre=[REQUIRE_AUTH],
        entry=[("Top bar", "Any page", "—")],
        exits=[("User confirms a mode", "Any", "/practice/test/:id (Reading) or /practice/{skill}/:id", "Query `?mode=`"),
               EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Sidebar", "Panel", "—", "\"LUYỆN ĐỀ\", \"KỸ NĂNG\" (Reading, Listening, Writing, Speaking), \"Bài lẻ\" / "
                "\"Full đề\", \"NGUỒN TÀI LIỆU\""),
               ("TAB", "Bài chưa làm / Bài đã làm", "Segmented tabs", "—", "Filters the cards"),
               ("TXT", "Tìm tên bài tập", "Search Input", "Case-insensitive title match", "Client-side filter"),
               ("CRD", "Practice test card", "Card grid", "—", "Title and meta; opens MDL-01"),
               ("MDL", "Lựa chọn chế độ làm bài", "Dialog", "—", "Options \"Luyện tập\" and \"Thi thử\"")],
        apis=[("Page load / filters", None, "mocks/practiceData.ts", "Filter cards in memory", "—")],
        interactions=[("Open a test", ["Reading → /practice/test/{id}?mode={mode}",
                                       "Listening / Writing / Speaking → /practice/{skill}/{id}?mode={mode}"])],
        msgs=[("Empty State", "No card matches", "Design target: \"Không có bài phù hợp.\"", "—")],
        rules=[MOCK_RULE],
    ),
    # ------------------------------------------------------------------ Practice Workspaces
    screen(
        key="PracticeWorkspace", name="Practice Workspaces (Reading, Listening, Writing, Speaking)",
        route="/practice/test/:testId · /practice/listening/:testId · /practice/writing/:taskId · "
              "/practice/speaking/:cueId · RequireAuth",
        roles="Any signed-in user", ft="— (closest FT-25, FT-39, FT-42, FT-43)", uc="—", fe="mock",
        status="Prototype (mock data and browser storage, OQ-17)",
        frontend="Built on mock data — features/practice/pages/PracticeTestPage.tsx, ListeningPage.tsx, WritingPage.tsx "
                 "and SpeakingPage.tsx; notes, highlights and flashcards stay in the browser (GAP-10); recordings are "
                 "not sent anywhere.",
        purpose="Gives a full-screen workspace for one practice test per skill, with tools for highlighting, notes, a "
                "small dictionary and flashcards.",
        nav_from=[("Practice Test Catalog", "User confirms a mode")],
        nav_to=[("Practice Test Catalog", "User clicks \"Thoát\"")],
        pre=[REQUIRE_AUTH],
        entry=[("Mode dialog", "Practice Test Catalog", "Path id, Query `mode` (Luyện tập / Thi thử)")],
        exits=[("User clicks Thoát", "Any", "Practice Test Catalog", "Work is not saved"), EX_SESSION],
        comps=[("PNL", "Reading: passage + questions", "Split view", "—", "Question map, navigation, timer \"Thời gian làm bài\""),
               ("PNL", "Passage tools", "Context menu", "Text must be selected first", "\"Tra từ vựng\", \"Tạo Flashcard\", "
                "Highlight / \"Bỏ Highlight\""),
               ("MDL", "Tạo Flashcard", "Dialog", "Word and meaning required; image PNG/JPG/WebP ≤ 1 MB",
                "\"Lưu từ vựng và ảnh minh họa trên trình duyệt này. Chưa đồng bộ tài khoản.\""),
               ("PNL", "Ghi chú", "Floating notes", "—", "\"Ghi chú ({n})\"; kept in the page"),
               ("AUD", "Listening player", "Audio player", "Playback rate", "\"Section 1 · Practice\""),
               ("TXA", "Writing editor", "Text Area", "Minimum words shown", "Countdown timer; word counter; \"Bài viết chỉ được "
                "giữ trong phiên hiện tại.\""),
               ("BTN", "Speaking recorder", "Button", "Microphone permission", "\"Thời gian nói {mm:ss}\"; \"Bản ghi chỉ ở trong "
                "trình duyệt hiện tại và chưa được gửi cho Mentor.\"")],
        apis=[("Page load", None, "mocks/practiceData.ts", "Render the test", "—"),
              ("Save flashcard", None, "localStorage (practice/lib/flashcardStorage.ts)", "\"Đã lưu Flashcard trên trình duyệt "
               "này. Mở “Thẻ đã lưu” để xem.\"", "Storage full → MSG-02")],
        interactions=[("Design target", ["When these workspaces are wired: Writing uses FT-25 grading, Speaking uses FT-39, "
                                         "notes and flashcards use library-service (FT-42, FT-43) instead of the browser."])],
        msgs=[("Info Text", "Tool used without a selection", "\"Hãy bôi đen một đoạn trong bài đọc trước khi chọn công cụ.\"",
               "Inline"),
              ("Inline Error", "Flashcard not stored", "\"Không lưu được: bộ nhớ đầy, bị chặn hoặc dữ liệu cũ không hợp lệ. Thử bỏ "
               "ảnh hoặc bật bộ nhớ trình duyệt; dữ liệu đã lưu không bị ghi đè.\"", "In the dialog"),
              ("Inline Error", "Word or meaning empty", "\"Nhập từ vựng và nghĩa trước khi lưu.\"", "In the dialog")],
        rules=[MOCK_RULE, ("GAP-10", "Personal data in the browser, not in library-service", "Lost on another device")],
    ),
    # ------------------------------------------------------------------ Classroom
    screen(
        key="Classroom", name="Classroom", route="/classroom · RequireAuth", roles="Any signed-in user",
        ft="— (mentor classes are not in the SRS)", uc="—", fe="mock", status="Prototype (mock data, OQ-17)",
        frontend="Built on mock data — features/classroom/ClassroomPage.tsx with mocks/classroomData.ts; no API call.",
        purpose="Shows the learner's class, teacher and a calendar of class sessions with a link to join or open each "
                "session.",
        nav_from=[("Top bar", "User clicks \"Lớp học\"")],
        nav_to=[("Classroom Lesson Workspace", "User opens a session's lesson"),
                ("Placeholder /classes/:classCode/join", "User clicks \"Join class! Vào lớp ngay\"")],
        pre=[REQUIRE_AUTH],
        entry=[("Top bar", "Any page", "—")],
        exits=[("User opens a session", "Any", "Classroom Lesson Workspace or detail modal", "—"), EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Class header", "Header", "—", "\"LỚP HỌC CỦA TÔI\", \"Xin chào {name}\", \"Mã lớp\", \"Giáo viên\", \"Thời "
                "gian\""),
               ("PNL", "Calendar controls", "Toolbar", "—", "\"Tháng trước\" / \"Tháng sau\", \"Xem theo tháng\" / \"Xem theo lộ "
                "trình\", \"Xem dạng danh sách\""),
               ("CRD", "Schedule card", "Card", "—", "One per session"),
               ("MDL", "Lesson detail", "Dialog", "—", "Session details"),
               ("PNL", "Class progress", "Panel + mascot", "—", "Progress of the course")],
        apis=[("Page load", None, "mocks/classroomData.ts", "Render the calendar", "—")],
        interactions=[("Open question", ["Mentor-led classes are not specified in the SRS; decide whether they become a new "
                                         "feature or are removed (OQ-17)."])],
        msgs=[("Info Text", "Join link", "\"● Join class! Vào lớp ngay\"", "Opens a placeholder page")],
        rules=[MOCK_RULE],
    ),
    # ------------------------------------------------------------------ Classroom Lesson Workspace
    screen(
        key="ClassLesson", name="Classroom Lesson Workspace", route="/lessons/:lessonId · RequireAuth",
        roles="Any signed-in user", ft="— (closest FT-36, FT-43)", uc="—", fe="mock", status="Prototype (mock data, OQ-17)",
        frontend="Built on mock data — features/classroom/pages/LessonWorkspacePage.tsx with mocks/lessonData.ts; no API "
                 "call. Not to be confused with the Lesson Player (/learn/lessons/:lessonId).",
        purpose="Shows one class session with its materials, a short quiz and the topic vocabulary to review.",
        nav_from=[("Classroom", "User opens a session")],
        nav_to=[("Classroom", "User goes back")],
        pre=[REQUIRE_AUTH],
        entry=[("Session link", "Classroom", "`lessonId`")],
        exits=[EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Sidebar", "Panel", "—", "\"Buổi {n}\", \"Tiến độ khoá học\", \"Tiến độ Homework Hub\""),
               ("PNL", "Lesson materials", "Panel", "—", "Session documents"),
               ("PNL", "Lesson quiz", "Questions", "—", "Short quiz"),
               ("TBL", "Homework Hub / Từ vựng", "Vocabulary collection", "—", "Same component as the Vocabulary screen")],
        apis=[("Page load", None, "mocks/lessonData.ts", "Render the session", "—")],
        interactions=[("Data source", ["All content is mock data (OQ-17)."])],
        msgs=[("Info Text", "Progress", "\"Hoàn thành 8 / 12 bài tập\" (mock)", "—")],
        rules=[MOCK_RULE],
    ),
]
