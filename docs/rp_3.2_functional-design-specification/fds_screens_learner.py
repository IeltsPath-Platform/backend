"""Screens of the public area, the learning loop and assessment.

Built screens describe the frontend as it is (IeltsPath Platform Frontend, commit ac25901): routes, React pages,
Vietnamese UI copy and the API calls the code makes. Screens not built keep the design target.
"""
from fds_screens import AUTH, EX_SESSION, ex_menu, owner_only, role, screen

LEARNER = role("CUSTOMER")
REQUIRE_AUTH = ("Session exists (RequireAuth, GG-01)", "Continue rendering",
                "Redirect → /login (state.from kept); GLB-01")
GUEST_ONLY = ("No signed-in session (GuestOnly, GG-03)", "Render the form", "Redirect → /learn")
EX_TOPBAR = ("User picks an item in the top bar (SiteNavbar)", "Any", "Selected route", "No unsaved-change prompt")
REVIEW_GATE_RULE = ("BR-17", "A pending review blocks the lessons that teach its skill, their practice and the "
                    "final test of a topic that teaches it",
                    "403 REVIEW_REQUIRED → review panel / banner GLB-10 with \"Làm bài ôn\"")
FE_AUTH = "Built — features/auth/pages/{page} (route {route}); calls the real backend."
FE_LEARN = "Built — features/learning-path/pages/{page} inside LearnLayout (route {route}); calls the real backend."

SCREENS = [
    # ------------------------------------------------------------------ Home
    screen(
        key="Home", name="Home", route="/home · Public page (default route for guests)", roles="Guest and any signed-in user",
        ft="FT-10, FT-11, FT-12", uc="UC: View subscription; Activate key", fe="partial",
        status="Specified (key activation not wired in the frontend: GAP-03)",
        frontend="Partly built — features/home/HomePage.tsx with PricingCards and KeyActivationDialog. Landing "
                 "sections use static content; the pricing area calls access-service only when an access client is "
                 "passed in, which the route does not do yet (GAP-03).",
        shots=[("Home.png", "Home (/home), hero section as a guest")],
        purpose="Introduces IELTS Space, shows the FREE / Premium plans and point cards, and lets a signed-in learner "
                "redeem an activation key for Premium days or points.",
        nav_from=[("Browser", "User opens \"/\" without a session, or /home"), ("Top bar", "User clicks \"Trang chủ\" or "
                  "the logo")],
        nav_to=[("Login / Register", "Guest clicks \"Đăng nhập\" / \"Đăng ký\" in the top bar or in the key dialog"),
                ("Practice Test Catalog", "User clicks \"Khám phá bài luyện\" (signed in)")],
        pre=[("None — public page", "Render", "—")],
        entry=[("Open the site", "Browser", "Path `/` → `/home` for guests"), ("Top bar", "Any page", "—")],
        exits=[("User clicks Đăng nhập / Đăng ký", "Guest", "Login / Register", "—"),
               ("User completes key activation", "Signed in, client available", "Stay", "Pricing reloads"),
               EX_TOPBAR],
        comps=[("PNL", "Hero", "Section", "—", "\"Mỗi ngày một bước. Gần hơn band mục tiêu.\" · CTA \"Khám phá bài luyện\", "
                "\"Gặp gỡ Mentor\""),
               ("PNL", "Introduction, learning orbit, quality, teachers, learner feedback", "Sections", "—", "Static content"),
               ("CRD", "Gói FREE mặc định", "Card", "—", "Badge \"Đang sử dụng\" when no Premium is active"),
               ("CRD", "Gói Premium", "Card", "Duration choice: Premium 30 Ngày / Premium 90 Ngày", "Shows days and "
                "examiner credits; \"Còn {n} ngày · hết hạn {date}\" when active"),
               ("CRD", "Thẻ nạp Point", "Card", "Choice: Thẻ Point 50 / Thẻ Point 100", "—"),
               ("BTN", "Kích hoạt bằng Mã Key — {product}", "Button (Primary)", "—", "Opens MDL-01 for the chosen product"),
               ("MDL", "Kích hoạt Gói / Nạp Key", "Dialog", "—", "Holds TXT-01 and BTN-02"),
               ("TXT", "Mã Activation Key", "Text Input", "Required; trimmed and upper-cased", "Placeholder "
                "\"IP-XXXX-XXXX-XXXX\""),
               ("BTN", "Kích hoạt bằng Mã Key", "Button (Primary)", "Disabled while pending or not signed in", "Shows "
                "\"Đang kích hoạt…\" while pending")],
        apis=[("Pricing area mounts (signed in, client present)", "access.mySub", "—",
               "Mark the active plan · loading: \"Đang tải thông tin gói hiện tại…\"",
               "Error → \"Chưa thể tải thông tin gói hiện tại.\" + \"Thử tải lại\""),
              ("User clicks Kích hoạt bằng Mã Key", "access.activate", "`{rawKey, idempotencyKey}` (UUID kept while the "
               "same key is retried)", "Dialog shows MSG-01 or MSG-02; pricing reloads", "Mapped messages MSG-03…MSG-07")],
        interactions=[
            ("Dialog state", [
                "IF the user is not signed in",
                "THEN   show \"Đăng nhập tài khoản của bạn để kích hoạt gói hoặc nạp điểm.\" + button \"Đăng nhập\"",
                "ELSE IF no access client (current /home route, GAP-03)",
                "THEN   show \"Phiên đăng nhập demo chưa hỗ trợ kích hoạt Key. Mã của bạn chưa được gửi hoặc sử dụng.\"",
                "ELSE   show the submit button",
                "END IF"]),
            ("Premium check", ["A plan is shown as active when planCode = PREMIUM, status = ACTIVE and endsAt is empty "
                               "or in the future."]),
        ],
        msgs=[("Success Panel", "POINTS key redeemed", "\"Kích hoạt thành công\" · \"Đã nạp {n} Points vào ví điểm. Điểm không "
               "hết hạn và không cấp quyền Premium hay lượt chấm giáo viên.\"", "Button \"Hoàn tất\""),
              ("Success Panel", "PREMIUM key redeemed", "\"Kích hoạt thành công\" · \"Đã cộng {d} ngày Premium và {c} lượt chấm "
               "giáo viên. Gói không cộng thêm AI Points.\"", "Button \"Hoàn tất\""),
              ("Inline Error", "Key already redeemed", "\"Mã này đã được sử dụng.\"", "Below the field"),
              ("Inline Error", "Key expired / revoked", "\"Mã kích hoạt đã hết hạn.\" / \"Mã kích hoạt đã bị thu hồi.\"",
               "Below the field"),
              ("Inline Error", "Unknown key or other 4xx", "\"Mã kích hoạt không hợp lệ. Vui lòng kiểm tra lại mã.\" / \"Chưa "
               "thể kích hoạt mã. Vui lòng kiểm tra mã và thử lại.\"", "Below the field"),
              ("Inline Error", "401 / 403 / 429", "\"Phiên đăng nhập đã hết hạn hoặc chưa hợp lệ. Vui lòng đăng nhập lại.\" / "
               "\"Tài khoản của bạn chưa được phép thực hiện thao tác này.\" / \"Bạn thao tác quá nhanh. Vui lòng chờ một "
               "chút rồi thử lại.\"", "Below the field"),
              ("Inline Error", "5xx or network", "\"Dịch vụ tạm thời gián đoạn. Vui lòng thử lại sau.\" / \"Chưa xác nhận được "
               "kết quả kích hoạt. Hãy giữ nguyên mã và thử lại khi kết nối ổn định.\"", "Below the field"),
              ("Inline Error", "Empty key", "\"Vui lòng nhập mã kích hoạt.\"", "Below the field; focus returns")],
        rules=[("BR-07", "A key is redeemed at most once", "MSG-03 / MSG-04"),
               ("BR-08", "Premium days extend the subscription", "MSG-02 and the new end date on the Premium card"),
               ("BR-30", "Same idempotencyKey grants once", "Key id kept while the same key is retried")],
    ),
    # ------------------------------------------------------------------ Login
    screen(
        key="Login", name="Login", route="/login · Public page (GuestOnly)", roles="Guest",
        ft="FT-02, FT-03, FT-05, FT-07", uc="UC: Sign in; Sign in with OAuth", fe="api",
        status="Specified (OAuth button: Draft; role-based home: GAP-02)",
        frontend=FE_AUTH.format(page="AuthPage.tsx (mode sign-in) with AuthForm", route="/login"),
        shots=[("Login.png", "Login (/login)")],
        purpose="Lets a registered user sign in with email and password, so the app can return to the page the user "
                "wanted or open the learning path.",
        nav_from=[("Any protected route", "No session (RequireAuth); the original location is kept"),
                  ("Register", "User clicks \"Đăng nhập\""), ("Reset Password", "Reset succeeded (redirect after 0.9 s)"),
                  ("User menu", "User clicks \"Đăng xuất\"")],
        nav_to=[("Original route, else Course List (/learn)", "Sign-in succeeds"),
                ("Register", "User clicks \"Đăng ký ngay\""), ("Forgot Password", "User clicks \"Quên mật khẩu?\"")],
        pre=[GUEST_ONLY],
        entry=[("Protected route without session", "Any", "Router state `from`"), ("Top bar \"Đăng nhập\"", "Home, "
               "Vocabulary", "—"), ("Sign-out", "User menu", "—")],
        exits=[("User clicks Đăng nhập", "Credentials accepted", "state.from or /learn", "No message"),
               ("User clicks Đăng ký ngay", "Any", "Register", "—"),
               ("User clicks Quên mật khẩu?", "Any", "Forgot Password", "—"),
               ("Sign-in rejected", "HTTP 401 / 400", "Stay", "MSG-01")],
        comps=[("TXT", "Email", "Email Input", "Required (HTML)", "Placeholder \"name@example.com\""),
               ("TXT", "Mật khẩu", "Password Input", "Required", "Placeholder \"Mật khẩu\"; show/hide toggle"),
               ("LNK", "Quên mật khẩu?", "Link", "—", "Opens /forgot-password"),
               ("BTN", "Đăng nhập", "Button (Primary)", "Disabled while submitting", "Label \"Đang xử lý…\" while pending"),
               ("BTN", "Google (chưa hỗ trợ)", "Button (Outline)", "Disabled while VITE_OAUTH_ENABLED = false",
                "Label \"Continue with Google\" when enabled"),
               ("LNK", "Đăng ký ngay", "Link", "—", "Opens /register"),
               ("PNL", "Status line", "Text (role=status)", "—", "Shows MSG-01 / MSG-02")],
        apis=[("User clicks Đăng nhập", "auth.login", "`{email, password}`", "Keep the access token in memory · "
               "loading: \"Đang xử lý…\"", "Server message → MSG-01"),
              ("Login succeeded", "user.me", "Bearer access token", "Fill name and email of the session (the app "
               "reads /api/users/me, not /auth/me: GAP-11)", "Error → MSG-01"),
              ("Login succeeded", "access.myPoints", "—", "Point balance in the user menu", "Error ignored (points stay 0)"),
              ("User clicks the Google button (OAuth enabled)", "auth.oauthStart", "Query: `redirect_uri=<origin>/auth/oauth/"
               "callback`", "Browser goes to the provider", "Not enabled → MSG-02")],
        interactions=[("After sign-in", ["navigate(location.state.from.pathname ?? '/learn', replace)",
                                          "The app does not read roles yet: every user lands on /learn (GAP-02)."]),
                      ("Failure message", ["The status line shows the server message of the 401. The backend returns "
                                           "the same message for every failure (FT-02/NAC-01)."])],
        msgs=[("Inline Error", "API returns 401 / 400", "Server message, else \"Không thể xác thực. Vui lòng thử lại.\"",
               "Status line until the next submit"),
              ("Inline Error", "User clicks Google while OAuth is off", "\"Đăng nhập với Google chưa được hỗ trợ.\"",
               "Status line"),
              ("Inline Error", "Network error", "GLB-08", "Status line"),
              ("Inline Error", "Design target: HTTP 429 after 5 failed sign-ins in 15 minutes (NFR-SEC11)",
               "\"Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.\"", "Status line; button stays "
               "enabled")],
        rules=[("BR-04", "Access token 3,600 s; refresh token 7 days, single use", "Silent refresh (Section 2.0)"),
               ("FT-02/NAC-01", "Same message for every sign-in failure", "Server message shown as is"),
               ("NFR-SEC11", "At most 5 failed sign-ins per account and IP in 15 minutes, then HTTP 429 [not built]",
                "MSG-04"),
               ("BR-06", "Five canonical roles", "Design target: open the role home (GAP-02)")],
    ),
    # ------------------------------------------------------------------ Register
    screen(
        key="Register", name="Register", route="/register · Public page (GuestOnly)", roles="Guest",
        ft="FT-01, FT-02", uc="UC: Register", fe="api", status="Specified",
        frontend=FE_AUTH.format(page="AuthPage.tsx (mode sign-up) with AuthForm", route="/register"),
        shots=[("Register.png", "Register (/register)")],
        purpose="Lets a guest create a learner account and signs the guest in straight away, so learning can start.",
        nav_from=[("Login", "User clicks \"Đăng ký ngay\""), ("Top bar", "Guest clicks \"Đăng ký\"")],
        nav_to=[("Course List (/learn)", "Registration and the automatic sign-in succeed"),
                ("Login", "User clicks \"Đăng nhập\"")],
        pre=[GUEST_ONLY],
        entry=[("Đăng ký ngay", "Login", "—"), ("Top bar", "Home, Vocabulary", "—")],
        exits=[("User clicks Tạo tài khoản", "Register 201 and login 200", "state.from or /learn", "No message"),
               ("Rejected", "409 / 400", "Stay", "MSG-01"), ("User clicks Đăng nhập", "Any", "Login", "—")],
        comps=[("TXT", "Họ và tên", "Text Input", "Required; trimmed not blank", "Placeholder \"Nguyễn Văn A\""),
               ("TXT", "Email", "Email Input", "Required", "Placeholder \"name@example.com\""),
               ("TXT", "Mật khẩu", "Password Input", "6–72 characters (BV-01); MSG-02", "Hint \"Từ 6 đến 72 ký tự.\""),
               ("TXT", "Xác nhận mật khẩu", "Password Input", "Must equal Mật khẩu; MSG-03", "Placeholder \"Nhập lại mật khẩu\""),
               ("BTN", "Tạo tài khoản", "Button (Primary)", "Disabled while submitting", "\"Đang xử lý…\" while pending"),
               ("BTN", "Google (chưa hỗ trợ)", "Button (Outline)", "Disabled (OAuth off)", "—"),
               ("LNK", "Đăng nhập", "Link", "—", "Opens /login")],
        apis=[("User clicks Tạo tài khoản", "user.register", "`{email, password, fullName}`", "Then API 2", "409 / 400 → MSG-01"),
              ("Registration succeeded", "auth.login", "`{email, password}`", "Then user.me and access.myPoints; navigate",
               "Error → MSG-01")],
        interactions=[("Client checks", ["IF full name blank OR email/password empty → do nothing (HTML required)",
                                         "IF password length outside 6–72 → MSG-02",
                                         "IF confirmation differs → MSG-03",
                                         "ELSE submit"])],
        msgs=[("Inline Error", "API error", "Field message from the server, else server message, else \"Không thể đăng ký. "
               "Vui lòng thử lại.\"", "Status line"),
              ("Inline Error", "Password outside 6–72", "\"Mật khẩu phải có từ 6 đến 72 ký tự.\"", "Status line"),
              ("Inline Error", "Confirmation differs", "\"Mật khẩu xác nhận không khớp.\"", "Status line")],
        rules=[("BR-01", "Public registration creates a CUSTOMER account; one account per email", "409 → MSG-01"),
               ("BR-02 / FT-01 BV-01", "Password 6–72 characters", "MSG-02 before any request"),
               ("NFR-SEC04", "Password at most 72 bytes (BCrypt limit); the current release counts characters",
                "Accented characters take 2–3 bytes: a 72-character Vietnamese password can be rejected later")],
    ),
    # ------------------------------------------------------------------ Forgot Password
    screen(
        key="ForgotPassword", name="Forgot Password", route="/forgot-password · Public page (GuestOnly)", roles="Guest",
        ft="FT-04", uc="UC: Reset password; Send reset link", fe="api", status="Specified (email delivery: Draft, GAP-06)",
        frontend=FE_AUTH.format(page="ForgotPasswordPage.tsx", route="/forgot-password"),
        shots=[("ForgotPassword.png", "Forgot Password (/forgot-password)")],
        purpose="Lets a user who forgot the password ask for a reset code, so a new password can be set.",
        nav_from=[("Login", "User clicks \"Quên mật khẩu?\""), ("Reset Password", "User clicks \"Yêu cầu lại\"")],
        nav_to=[("Reset Password", "User clicks \"Tôi đã có mã — đặt lại mật khẩu\" (after sending)"),
                ("Login", "User clicks \"Quay lại đăng nhập\"")],
        pre=[GUEST_ONLY],
        entry=[("Quên mật khẩu?", "Login", "—")],
        exits=[("User clicks Gửi mã đặt lại", "API 200", "Stay", "MSG-01; button becomes \"Gửi lại mã\""),
               ("User clicks Tôi đã có mã", "After sending", "Reset Password", "—"),
               ("User clicks Quay lại đăng nhập", "Any", "Login", "—")],
        comps=[("TXT", "Email", "Email Input", "Required; trimmed", "Disabled while sending"),
               ("BTN", "Gửi mã đặt lại / Gửi lại mã", "Button (Primary)", "Disabled while sending", "\"Đang gửi…\" while pending"),
               ("PNL", "Status line", "Text (role=status)", "—", "MSG-01 / MSG-02"),
               ("LNK", "Tôi đã có mã — đặt lại mật khẩu", "Link", "Visible after a success", "Opens /reset-password"),
               ("LNK", "Quay lại đăng nhập", "Link", "—", "Opens /login")],
        apis=[("User clicks Gửi mã đặt lại", "auth.forgot", "`{email}`", "Show MSG-01", "404 / 400 → MSG-02")],
        interactions=[("No email yet", ["The backend stores the token but sends no email (Email Service not integrated); "
                                        "in the demo the code is read from the backend log and pasted on Reset Password."])],
        msgs=[("Info Text", "API returns 200", "Server message, else \"Nếu email tồn tại, mã đặt lại đã được tạo. (Môi trường "
               "demo: lấy mã từ log/Swagger BE.)\"", "Status line"),
              ("Inline Error", "API error", "Server message, else \"Không gửi được yêu cầu. Thử lại.\"", "Status line"),
              ("Inline Error", "Design target: HTTP 429 after 3 requests for one email in an hour (NFR-SEC11)",
               "\"Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau.\"", "Status line")],
        rules=[("BR-03", "Reset token single-use, valid 15 minutes", "Not stated on the screen (design target: MSG-01)"),
               ("FT-04/NAC-01", "Unknown email → 404 (current release)", "Server message shown (reveals unknown emails; "
                "OQ-03)"),
               ("NFR-SEC12", "Same response whether or not the email is registered [open issue in the backend]",
                "Design target: always MSG-01; the 404 branch goes away"),
               ("NFR-SEC11", "At most 3 reset requests per email per hour, then HTTP 429 [not built]", "MSG-03")],
    ),
    # ------------------------------------------------------------------ Reset Password
    screen(
        key="ResetPassword", name="Reset Password", route="/reset-password?token=… · Public page (GuestOnly)",
        roles="Guest", ft="FT-04", uc="UC: Reset password", fe="api", status="Specified (link delivery: Draft, GAP-06)",
        frontend=FE_AUTH.format(page="ResetPasswordPage.tsx", route="/reset-password"),
        shots=[("ResetPassword.png", "Reset Password with the code prefilled from ?token=")],
        purpose="Lets the user set a new password with the reset code, so the user can sign in again.",
        nav_from=[("Forgot Password", "User clicks \"Tôi đã có mã — đặt lại mật khẩu\""), ("Reset link", "Opened with ?token=")],
        nav_to=[("Login", "Reset succeeded (after 0.9 s) or user clicks \"Đăng nhập\""),
                ("Forgot Password", "User clicks \"Yêu cầu lại\"")],
        pre=[GUEST_ONLY],
        entry=[("Link from Forgot Password", "Forgot Password", "—"), ("Reset link", "Email (Draft)", "Query: `?token=`")],
        exits=[("User clicks Đặt lại mật khẩu", "API 200", "Login", "MSG-01, redirect after 900 ms"),
               ("Rejected", "API 400", "Stay", "MSG-02")],
        comps=[("TXT", "Mã đặt lại", "Text Input", "Required; prefilled from ?token=", "Hint \"Demo: BE ghi mã vào log khi "
                "gọi forgot-password.\""),
               ("TXT", "Mật khẩu mới", "Password Input", "6–72 characters; MSG-03", "Hint \"Từ 6 đến 72 ký tự.\""),
               ("TXT", "Xác nhận mật khẩu", "Password Input", "Must match; MSG-04", "Placeholder \"Nhập lại mật khẩu mới\""),
               ("BTN", "Đặt lại mật khẩu", "Button (Primary)", "Disabled while submitting", "\"Đang xử lý…\" while pending"),
               ("LNK", "Yêu cầu lại · Đăng nhập", "Links", "—", "—")],
        apis=[("User clicks Đặt lại mật khẩu", "auth.reset", "`{token, newPassword}`", "MSG-01; navigate /login after 900 ms",
               "400 → MSG-02")],
        interactions=[("Token source", ["The code field is prefilled from ?token= and can be edited, because the demo has "
                                        "no email: the user pastes the code from the backend log."])],
        msgs=[("Info Text", "API returns 200", "Server message, else \"Đặt lại mật khẩu thành công. Đang chuyển tới đăng "
               "nhập…\"", "Status line; redirect after 0.9 s"),
              ("Inline Error", "API error", "Field message from the server, else \"Không đặt lại được mật khẩu. Kiểm tra mã và "
               "thử lại.\"", "Status line"),
              ("Inline Error", "Password outside 6–72", "\"Mật khẩu phải có từ 6 đến 72 ký tự.\"", "Status line"),
              ("Inline Error", "Confirmation differs", "\"Mật khẩu xác nhận không khớp.\"", "Status line")],
        rules=[("BR-03", "Token single-use, 15 minutes", "Expired or used code → MSG-02"),
               ("BR-02", "New password 6–72 characters (frontend check)", "MSG-03; the backend accepts ≥ 6"),
               ("NFR-SEC04", "Reset must also enforce at most 72 bytes [open issue in the backend]",
                "Frontend check stays until the backend enforces it")],
    ),
    # ------------------------------------------------------------------ OAuth Callback
    screen(
        key="OAuthCallback", name="OAuth Callback", route="/auth/oauth/callback · Public page (GuestOnly)", roles="Guest",
        ft="FT-05", uc="UC: Sign in with OAuth", fe="placeholder", status="Draft",
        draft="Draft: OAuth sign-in is not built in the backend; the frontend route only shows a message while "
              "VITE_OAUTH_ENABLED = false.",
        frontend="Placeholder — features/auth/pages/OAuthCallbackPage.tsx; exchanges nothing until the backend "
                 "endpoint exists (authApi.handleOAuthCallback throws).",
        purpose="Finishes a sign-in with the OAuth identity provider and opens the learning path.",
        nav_from=[("OAuth Identity Provider", "Provider redirects back with code/state")],
        nav_to=[("Course List (/learn)", "Tokens received and user.me succeeds"), ("Login", "User clicks \"Quay lại đăng "
                "nhập\"")],
        pre=[GUEST_ONLY],
        entry=[("Provider redirect", "OAuth Identity Provider", "Query: `code, state` [TBC]")],
        exits=[("Sign-in completes", "OAuth enabled and exchange succeeds", "/learn", "—"),
               ("User clicks Quay lại đăng nhập", "Any", "Login", "—")],
        comps=[("PNL", "Message", "Heading + text", "—", "\"Đang hoàn tất đăng nhập…\" or MSG-01 / MSG-02"),
               ("LNK", "Quay lại đăng nhập", "Link", "—", "Opens /login")],
        apis=[("Page load (OAuth enabled) [Draft]", "auth.oauthStart", "Code exchange endpoint [TBC]",
               "loginWithAccessToken → user.me → /learn", "Error → MSG-02")],
        interactions=[("Never invents a session", ["Without a backend exchange the page shows a message only; it never "
                                                   "creates tokens on the client."])],
        msgs=[("Info Text", "OAuth disabled", "\"Đăng nhập mạng xã hội chưa được hỗ trợ.\"", "Persistent"),
              ("Inline Error", "Exchange fails", "Error message, else \"OAuth thất bại.\"", "Persistent")],
        rules=[("FT-05", "OAuth sign-in issues tokens as in FT-02 [TBC]", "Not built (OQ-02)")],
    ),
    # ------------------------------------------------------------------ Overview
    screen(
        key="Overview", name="Overview", route="/overview · RequireAuth (default route when signed in)",
        roles="Any signed-in user", ft="FT-26, FT-44, FT-45 (intended)", uc="UC: View activity and streak; View mastery",
        fe="mock", status="Draft (UI on mock data, OQ-17)",
        shots=[("Overview.png", "Overview (/overview) — dashboard on mock data")],
        frontend="Built on mock data — features/overview/pages/OverviewPage.tsx with mocks/overviewData.ts; no API "
                 "call.",
        purpose="Gives the learner a dashboard of study time, four-skill progress, words to review, mentor feedback, "
                "courses and the study streak.",
        nav_from=[("Browser", "Signed-in user opens \"/\""), ("Top bar", "User opens \"Trang chủ\" → \"Dashboard\"")],
        nav_to=[("Selected route", "User follows a card link or the top bar")],
        pre=[REQUIRE_AUTH],
        entry=[("Default route", "Browser", "—"), ("Top bar", "Any page", "—")],
        exits=[EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Hero banner", "Banner", "—", "\"Xin chào, {username} 👋\" · \"Band hiện tại: {band}\" · \"Mục tiêu: "
                "{target} IELTS\""),
               ("PNL", "Sidebar", "Section menu (sticky; a row on small screens)", "—", "Overview sections"),
               ("CRD", "Dữ Liệu Học Tổng Quan", "Stat cards", "—", "Tổng thời gian học, Số ngày học, Số đề đã luyện, Tỷ lệ "
                "chính xác, Streak học"),
               ("PNL", "Biểu đồ “chăm chỉ” của bạn", "Month calendar", "Month switch; pick a day or a week", "\"đã học\" / "
                "\"chưa học\" / \"sắp tới\"; \"Dữ liệu mẫu · Hoạt động minh họa trong tuần hiện tại.\""),
               ("PNL", "Biểu đồ thời gian học", "Chart", "—", "Thực tế vs Kế hoạch per week"),
               ("PNL", "Tiến Độ 4 Kỹ Năng", "Panel", "—", "\"AI đánh giá so với kế hoạch lộ trình\""),
               ("PNL", "Study Plan · SPACE Autonomy · My Learning Orbit", "Panels", "—", "\"Kế hoạch AI cá nhân hoá\", "
                "\"Bài học tiếp theo:\""),
               ("PNL", "Gợi Ý Ôn Luyện Từ AI", "Panel", "—", "\"Bắt đầu ngay\""),
               ("PNL", "Phản Hồi Từ Mentor · Khoá Đang Học · Các Khoá Học Của Tôi", "Panels", "—", "—")],
        apis=[("Page load", None, "mocks/overviewData.ts", "Render cards", "—")],
        interactions=[("Data source", ["All numbers are mock data. When wired, study time and streak come from user-service "
                                       "(activity.list, streak.get) and skill progress from learn.mastery (OQ-17)."])],
        msgs=[("Empty State", "Design target: no activity", "\"Chưa có dữ liệu học tập.\"", "—")],
        rules=[("BR-27", "Personal data visible only to its owner", "Design target when the APIs are wired")],
    ),
    # ------------------------------------------------------------------ Course List
    screen(
        key="CourseList", name="Course List", route="/learn · RequireAuth (index of the learning area)",
        roles="Signed-in learner", ft="FT-20, FT-35, FT-55", uc="UC: View and choose courses; View learning path",
        fe="api", status="Specified (current course of BR-40: Draft, OQ-22; placement card link: GAP-15)",
        frontend=FE_LEARN.format(page="CourseListPage.tsx", route="/learn"),
        shots=[("CourseList.png", "Course List (/learn) — spotlight course and course cards")],
        purpose="Shows every band-level course with the learner's progress, puts forward the course to continue or "
                "the one the placement recommends, and leads to a course's topic path or to the placement test.",
        nav_from=[("Login / Register", "Sign-in without a stored route"),
                  ("Top bar", "User clicks \"Khóa học\" or \"Khóa học của tôi\""),
                  ("Course Path, Test Result", "User clicks \"Tất cả khóa học\" / \"Xem danh sách khóa\""),
                  ("Placement Test", "Placement already taken (PLACEMENT_ALREADY_DONE)")],
        nav_to=[("Course Path", "User clicks the spotlight button or a course card"),
                ("Placement Test", "User clicks the card \"Chưa biết nên chọn band nào?\" (wrong link today: GAP-15)")],
        pre=[REQUIRE_AUTH],
        entry=[("Sign-in", "Login", "—"), ("Top bar", "Any page", "—"),
               ("Lock redirect", "Learning pages", "Router state `notice` (GLB-09)")],
        exits=[("User clicks a course", "Any course (all are open)", "Course Path", "`/learn/courses/{courseId}`"),
               ("User clicks the placement card", "Filter Tất cả", "NotFoundPage today; design target Placement Test",
                "GAP-15"), EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Page header", "Header", "—", "Eyebrow \"Khóa học\" · \"Lộ trình IELTS theo band mục tiêu\" · \"Mỗi khóa "
                "gồm các chặng topic ngắn. Học xong các bài trong chặng và đạt bài kiểm tra chặng để mở chặng kế tiếp.\""),
               ("CRD", "Spotlight", "Card", "Course in progress, else the recommended course, else the lowest band",
                "Band ticket; label \"Đang học dở\" / \"Gợi ý cho bạn\" / \"Gợi ý bắt đầu\"; \"{n} chặng topic · đầu vào khuyến "
                "nghị ~{band} · kết thúc bằng bài thi cuối khóa\"; progress \"{passed}/{n} chặng\""),
               ("BTN", "Spotlight action", "Button (Accent)", "—", "\"Bắt đầu học\" / \"Học tiếp\" / \"Làm bài thi cuối khóa\" / "
                "\"Xem lại lộ trình\" by course state"),
               ("TAB", "Lọc khóa học", "Segmented buttons", "Filters with no course are hidden", "\"Tất cả\", \"Gợi ý cho bạn\", "
                "\"Đang học\", \"Đã hoàn thành\", each with a count"),
               ("CRD", "Course card", "Card grid", "Sorted by band level", "\"Band {x.x}\", title, \"{n} chặng topic · đầu vào "
                "~{band}\", progress or \"Chưa bắt đầu\", chip \"Đã hoàn thành\" / \"Sẵn sàng thi cuối\" / \"Đang học\" / \"Gợi ý "
                "cho bạn\", action label"),
               ("CRD", "Placement card", "Card", "Shown with the Tất cả filter", "\"Chưa biết nên chọn band nào?\" · \"Làm bài "
                "test đầu vào 4 kỹ năng miễn phí trong 15–20 phút để được gợi ý khóa phù hợp.\" · \"Làm test đầu vào\""),
               ("PNL", "Notice banner", "Banner", "—", "Redirect notice (GLB-09)")],
        apis=[("Page load", "learn.courses", "—", "Render spotlight and cards · loading: course skeleton",
               "GLB-04 / GLB-05 panel with \"Thử lại\"")],
        interactions=[("Course state", [
            "In progress  = passedTopicCount > 0 AND testStatus ≠ PASSED",
            "Action label = PASSED → \"Xem lại lộ trình\"; AVAILABLE → \"Làm bài thi cuối khóa\";",
            "               in progress → \"Học tiếp\"; else \"Bắt đầu học\"",
            "Entry band   = max(bandLevel − 1, 3.5)"]),
            ("Recommendation", ["recommended comes from the latest placement band (BR-36): the lowest course at or above it,",
                                "else the highest course. Without a placement nothing is recommended; every course stays open."])],
        msgs=[("Empty State", "A filter has no course", "Title \"Không tìm thấy khóa học\" · \"Không có khóa học nào trong nhóm "
               "này.\"", "Button \"Xem tất cả khóa học\""),
              ("Loading", "Page load", "Skeleton \"Đang tải danh sách khóa học…\"", "—")],
        rules=[("BR-35", "Courses are band levels shared by all skills, ordered by band", "Cards sorted by band level"),
               ("BR-36", "Placement only recommends a course", "Chip \"Gợi ý cho bạn\"; no course is locked"),
               ("BR-40", "One current course [Draft]", "Not built: every course opens (OQ-22)")],
    ),
    # ------------------------------------------------------------------ Placement Test
    screen(
        key="PlacementTest", name="Placement Test", route="/learn/placement · RequireAuth (survey, test and grading "
        "steps)", roles="Signed-in learner", ft="FT-09, FT-35, FT-39",
        uc="UC: Take placement test; Manage profile and learning goal", fe="api",
        status="Specified (grading wait: GAP-17; Speaking recordings not uploaded: GAP-19)",
        frontend=FE_LEARN.format(page="PlacementPage.tsx with placement/ (PlacementSurvey, PlacementDashboard, "
                                      "ObjectiveSectionRunner, WritingSectionRunner, SpeakingSectionRunner)",
                                 route="/learn/placement"),
        shots=[("PlacementDashboard.png", "Placement Test — test step: the five sections of an attempt in progress")],
        purpose="Lets a learner take the optional four-skill placement test once: three survey questions that save a "
                "learning goal, then the Listening, Reading, Writing and Speaking sections handed in one by one, then "
                "the wait while the essays are graded.",
        nav_from=[("Top bar", "User clicks \"Test đầu vào 4 kỹ năng FREE\""),
                  ("Course List", "Placement card (wrong link today: GAP-15)")],
        nav_to=[("Placement Report", "Grading finished"),
                ("Course List", "Placement already taken and no attempt of the learner's own (PLACEMENT_ALREADY_DONE)")],
        pre=[REQUIRE_AUTH,
             ("The learner has not finished a placement", "Survey, or the open attempt", "409 PLACEMENT_ALREADY_DONE → "
              "redirect /learn"),
             ("A PLACEMENT_TEST package is published", "Continue", "404 NO_PLACEMENT_TEST → MSG-01")],
        entry=[("Top bar", "Any page", "—"),
               ("Resume", "Learner comes back later", "Open attempt from as.placementCurrent: IN_PROGRESS → test step; "
                "submitted → grading step")],
        exits=[("User clicks Tiếp tục in the survey dialog", "Attempt created or resumed", "Test step (same route)", "—"),
               ("Last section handed in", "Attempt submitted automatically", "Grading step", "—"),
               ("Grading finished", "Result available", "Placement Report (same route)", "—"),
               ("User clicks Lưu và quay lại", "Inside a section", "Section list", "Answers already saved"),
               EX_TOPBAR, EX_SESSION],
        comps=[("PRG", "Stepper", "Steps", "—", "\"Khảo sát\" · \"Bài test\" · \"Kết quả\" with the progress of the current step"),
               ("PNL", "Survey question", "Chat bubble with mascot", "One question at a time", "\"Bạn dự định thi IELTS khi "
                "nào?\" · \"Bạn có thể dành bao nhiêu thời gian học mỗi ngày?\" · \"Mục tiêu điểm IELTS của bạn là?\"; "
                "\"Câu trước\""),
               ("BTN", "Exam month", "Chips", "Next six months, \"Thời gian khác\" (month input), \"Chưa có kế hoạch\"",
                "Exam date = last day of the month; \"Chưa có kế hoạch\" sends no date"),
               ("BTN", "Study time", "Option list", "\"Dưới 1 tiếng\" 45, \"Khoảng 1 – 2 tiếng\" 90, \"Khoảng 2 – 3 tiếng\" 150, "
                "\"Trên 3 tiếng\" 210 minutes a day", "—"),
               ("BTN", "Target band", "Option list", "\"Dưới IELTS 5.5\" (5.5) … \"IELTS 8.0 trở lên\" (8.0)", "Saves the goal"),
               ("MDL", "Bạn đã hoàn thành khảo sát!", "Dialog", "—", "Exam month, \"~{h}h/tuần\", band chart, button "
                "\"Tiếp tục\" (\"Đang mở bài test…\")"),
               ("PNL", "Bài kiểm tra đầu vào", "Dashboard", "—", "\"Bạn đã bắt đầu được {h} giờ {m} phút {s} giây\"; \"Không "
                "giới hạn thời gian. Câu trả lời được lưu trên hệ thống nên có thể thoát và quay lại làm tiếp; phần nào đã "
                "nộp thì không mở lại được.\""),
               ("TBL", "Phần thi của bạn", "Section list", "Section order of the package (Reading, Listening, Writing "
                "Task 1, Writing Task 2, Speaking in the seed)", "\"{done}/{n} đã hoàn thành\"; per section \"{n} câu\", time "
                "\"{m} phút\" or \"Đã hoàn thành\" once handed in, button \"Làm bài\" / \"Làm tiếp\"; \"Làm lần lượt từng "
                "phần. Bài được nộp tự động khi bạn hoàn thành phần cuối cùng.\""),
               ("BTN", "Làm lại khảo sát", "Button (Outline)", "Test step", "Back to the survey; the attempt is kept"),
               ("PNL", "Listening / Reading section", "Split view (passage or audio + questions)", "—",
                "\"Chuyển nhanh tới câu hỏi\", flag, \"Câu trước\" / \"Câu tiếp theo\", \"Nộp phần này\", \"Lưu và quay lại\""),
               ("TXA", "Bài viết", "Text Area per Writing task", "Saved as the learner types", "Placeholder \"Nhập phần viết của "
                "bạn ở đây\"; panel \"Tra từ vựng\" (opens a dictionary in a new tab, ≤ 50 characters)"),
               ("PNL", "Speaking", "Recorder", "Needs microphone permission", "\"Hướng dẫn chung\", \"Kiểm tra micro\", recording "
                "starts after each question, \"Dừng & sang câu tiếp\" / \"Dừng & hoàn thành\""),
               ("MDL", "Hand in a section", "Confirm dialog", "—", "\"Sau khi nộp, phần này không mở lại được.\" (+ unanswered "
                "count, or \"Bài viết đang để trống nên sẽ tính 0 điểm.\")"),
               ("PNL", "Grading", "Status panel (aria-live)", "—", "MSG-07 / MSG-08 / \"Chưa lấy được kết quả\" + \"Kiểm tra lại\"")],
        apis=[("Page load", "as.placementCurrent", "— (204 when none)", "Choose the step", "GLB-04 panel"),
              ("Page load", "learn.placementTest", "—", "Keep packageVersionId", "409 PLACEMENT_ALREADY_DONE → /learn; "
               "404 NO_PLACEMENT_TEST → MSG-01"),
              ("User picks a target band", "goal.create", "`{targetBand, examDate | null, availableMinutesPerDay}`",
               "Summary dialog", "MSG-02 (the test can still start)"),
              ("User clicks Tiếp tục (no attempt yet)", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}`",
               "Test step (attempt type PLACEMENT)", "MSG-03"),
              ("Test step load", "as.structure", "Path: `attemptId`", "Sections and items", "GLB-04 panel"),
              ("Test step load (parallel)", "as.responses", "Path: `attemptId`", "Restore saved answers", "GLB-04 panel"),
              ("User opens a section the first time", "as.sectionStart", "Path: `attemptId, sectionId`",
               "Section clock starts", "Ignored (the clock is only shown)"),
              ("Answer picked, essay typed, recording stopped", "as.save", "`{payload, schemaVersion, expectedRevision}`",
               "Save state", "MSG-04"),
              ("Writing or Speaking section handed in", "as.submission", "`{attemptItemId, skill, textPayload | "
               "audioReference, promptSnapshot, submissionKey}`", "Essay queued for JOB-03", "MSG-05"),
              ("User hands in a section", "as.sectionComplete", "Path: `attemptId, sectionId`", "Section closed", "MSG-05"),
              ("Last section handed in", "as.submit", "Path: `attemptId`", "Grading step", "MSG-06"),
              ("Grading step, every 2 s, up to 45 times", "learn.courses", "—", "Treats success as graded (GAP-17)",
               "Error → \"Chưa lấy được kết quả\"")],
        interactions=[
            ("Step on load", ["No attempt, or attempt EXPIRED / CANCELLED → survey",
                              "Attempt IN_PROGRESS → test step",
                              "Attempt submitted → grading step",
                              "No attempt of the learner's own AND PLACEMENT_ALREADY_DONE → redirect /learn"]),
            ("Sections", ["Each section is handed in on its own and is not reopened.",
                          "When the last section is handed in, the attempt is submitted automatically.",
                          "Speaking saves the recording length; the audio stays in the browser and only",
                          "local-recording:{attemptId}:{itemId} is sent (GAP-19)."]),
            ("Design target for the grading wait", ["Poll as.placementResult every 2 s until it stops returning 404, "
                                                    "then open the report (GAP-17)."])],
        msgs=[("Empty State", "404 NO_PLACEMENT_TEST", "\"Chưa có bài kiểm tra đầu vào nào được mở. Hãy quay lại sau.\"", "—"),
              ("Info Text", "Goal not saved", "\"Chưa lưu được mục tiêu lên hệ thống ({message}). Bạn vẫn có thể làm bài.\"",
               "In the survey dialog"),
              ("Inline Error", "Attempt not started", "Server message + \" Hãy thử lại.\"", "In the survey dialog"),
              ("Inline Error", "Answer, essay or recording not saved", "\"Chưa lưu được một số câu trả lời. Kiểm tra kết nối rồi "
               "nộp lại.\" / \"Chưa lưu được bài viết. Kiểm tra kết nối rồi nộp lại.\" / \"Chưa lưu được bản ghi. Kiểm tra kết "
               "nối rồi ghi lại câu này.\"", "In the section"),
              ("Inline Error", "Section not handed in", "Server message + \" Hãy nộp lại.\"", "In the section"),
              ("Inline Error", "Submit failed", "Server message + \" Hãy thử nộp lại.\" · \"Bạn đã hoàn thành tất cả các phần nhưng "
               "bài chưa được nộp.\"", "Buttons \"Thử nộp lại\" / \"Nộp bài\""),
              ("Info Panel", "Grading", "\"Đang chấm bài của bạn…\" · \"Bài luận được chấm tự động nên có thể mất tới một phút. "
               "Đừng đóng trang này.\"", "Until graded"),
              ("Info Panel", "Grading slow (after 45 checks)", "\"Kết quả đang được chấm\" · \"Việc chấm đang lâu hơn bình thường. "
               "Bài của bạn đã được nộp, hãy kiểm tra lại sau ít phút.\"", "Button \"Kiểm tra lại\"")],
        rules=[("BR-36", "Optional, taken once; only recommends a course", "Redirect when already taken; nothing locks"),
               ("BR-41", "Per-skill bands; Writing by the LLM or 5.5; Speaking 5.5 for now; overall rounded to 0.5",
                "Shown on the Placement Report"),
               ("BR-39", "Essays graded by the LLM (JOB-03)", "Grading step (MSG-07)"),
               ("BR-05", "At most one ACTIVE learning goal", "A refused goal only shows MSG-02"),
               ("BR-14", "Placement questions have purpose PLACEMENT", "Authoring rule; no UI effect")],
    ),
    # ------------------------------------------------------------------ Placement Report
    screen(
        key="PlacementReport", name="Placement Report", route="/learn/placement · result step",
        roles="Signed-in learner", ft="FT-20, FT-35", uc="UC: Take placement test; View and choose courses", fe="api",
        status="Specified",
        frontend=FE_LEARN.format(page="placement/PlacementResultView.tsx with ReportSkillPanels",
                                 route="/learn/placement"),
        purpose="Reports the placement result — overall and skill bands against the learner's target band, how "
                "reachable the target is and the detail of every skill with the Writing feedback — and recommends a "
                "course.",
        nav_from=[("Placement Test", "Grading finished, or the learner comes back after grading")],
        nav_to=[("Course Path", "User clicks \"Bắt đầu học\" (recommended course) or \"Xem khóa học\"")],
        pre=[REQUIRE_AUTH, owner_only("placement attempt")],
        entry=[("Grading finished", "Placement Test", "`attemptId`")],
        exits=[("User clicks a course", "Any", "Course Path", "—"), EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Report header", "Header", "—", "\"IELTS Academic · {date}\" · \"Báo cáo kết quả bài test\" · learner name · "
                "\"Dưới đây là phân tích trình độ dựa trên bài test đầu vào…\""),
               ("CRD", "Estimated overall band score", "Card", "—", "Overall band and one band per skill"),
               ("CRD", "Target band", "Card", "Needs a learning goal", "Target band, \"Số ngày còn lại\" (or \"Chưa chọn\"), "
                "\"Thời gian học {h} giờ/tuần\""),
               ("PNL", "Đánh giá khả thi", "Band steps + advice", "—", "Tags \"Bạn ở đây\" and \"Band mục tiêu\"; advice text and "
                "\"Gợi ý:\" tip"),
               ("PNL", "Phân tích chi tiết", "Skill panels", "One per section", "Reading / Listening: right and wrong "
                "questions; Writing Task 1 / Task 2: band and LLM feedback; Speaking: interim band"),
               ("CRD", "Lộ trình khóa học", "Course cards", "Sorted by band", "\"Đề xuất cho bạn\" / \"Ôn nền tảng\" / \"Nâng "
                "cao\", \"Band {x}\", \"{n} topic · đã qua {p}/{n}\", \"Bắt đầu học\" / \"Xem khóa học\"")],
        apis=[("Report load", "as.placementResult", "Path: `attemptId`", "Bands, sections and essays · loading: \"Đang tải "
               "báo cáo…\"", "404 while grading → error panel with \"Thử lại\" (GAP-17)"),
              ("Report load (parallel)", "learn.courses", "—", "Recommended course and course cards", "Error panel"),
              ("Report load (parallel)", "goal.active", "—", "Target side of the report", "404 → report without a target")],
        interactions=[("Feasibility", ["The advice compares the overall band with the target band, the days left to the "
                                       "exam month and the weekly study hours of the goal; without a goal only the bands "
                                       "are shown."]),
                      ("Course badge", ["Recommended → \"Đề xuất cho bạn\"; lower band → \"Ôn nền tảng\"; higher band → \"Nâng "
                                        "cao\"; no recommendation → \"Khóa học\"."])],
        msgs=[("Info Text", "No section detail", "\"Chưa có dữ liệu chi tiết cho bài làm này.\"", "—"),
              ("Loading", "Report load", "\"Đang tải báo cáo…\"", "—")],
        rules=[("BR-36", "The placement band only recommends a course", "Badge \"Đề xuất cho bạn\"; every course stays open"),
               ("BR-41", "Overall band = mean of the skill bands rounded to 0.5", "Overall card")],
    ),
    # ------------------------------------------------------------------ Course Path
    screen(
        key="CoursePath", name="Course Path", route="/learn/courses/:courseId · RequireAuth", roles="Signed-in learner",
        ft="FT-13, FT-20, FT-28, FT-55", uc="UC: View learning path; Take course test", fe="api",
        status="Specified (course test error codes: GAP-16; Premium lock: Draft)",
        frontend=FE_LEARN.format(page="TopicListPage.tsx", route="/learn/courses/:courseId"),
        shots=[("CoursePath.png", "Course Path — the topic stations of one course")],
        purpose="Shows the topics of one course as an ordered route with each topic's status and lesson progress, "
                "ending with the course final test, so the learner knows which topic to study next.",
        nav_from=[("Course List", "User clicks a course"), ("Topic Detail", "User clicks the course back link"),
                  ("Test Result", "Course test: user clicks \"Về khóa học\" / \"Về khóa để làm lại\"")],
        nav_to=[("Topic Detail", "User clicks a PASSED or IN_PROGRESS station, or \"Bắt đầu chặng\" / \"Học tiếp\""),
                ("Test Attempt", "User clicks \"Làm bài thi cuối khóa\" and the attempt is created"),
                ("Course List", "User clicks \"Tất cả khóa học\"")],
        pre=[REQUIRE_AUTH],
        entry=[("Course card", "Course List", "`courseId`"), ("Back link", "Topic Detail", "`courseId`")],
        exits=[("User clicks a station", "Not LOCKED", "Topic Detail", "—"),
               ("LOCKED station", "—", "Not clickable", "Lock reason under the card"),
               ("User clicks Làm bài thi cuối khóa", "testStatus AVAILABLE; APIs 3–4 succeed", "Test Attempt",
                "`/learn/tests/{attemptId}?course={courseId}`"),
               ("Course test refused", "403 / 409", "Stay", "MSG-03…MSG-05"), EX_TOPBAR, EX_SESSION],
        comps=[("LNK", "Tất cả khóa học", "Back link", "—", "To /learn"),
               ("PNL", "Course header", "Header", "—", "\"Band {x.x} · Lộ trình khóa học\", course title, \"Học lần lượt từng "
                "chặng. Hoàn thành các bài học và đạt bài kiểm tra chặng (từ 70%) để mở chặng tiếp theo.\""),
               ("PRG", "Course progress", "Meter", "—", "\"{passed}/{total} chặng đã qua\" and percent"),
               ("BTN", "Next topic", "Button (Accent)", "When a topic is IN_PROGRESS", "\"Tiếp theo · Chặng {n}: {title}\" + "
                "\"Bắt đầu chặng\" / \"Học tiếp\""),
               ("TAB", "Lọc theo kỹ năng", "Segmented buttons", "When the course has topics of more than one skill",
                "\"Tất cả\" + Listening, Reading, Writing, Speaking, and \"Khác\" for topics without a skill"),
               ("TBL", "Các chặng", "Ordered route", "Sorted by sequenceOrder", "\"Chặng {nn} · {skill}\", title, description, "
                "\"{done}/{total} bài\" or \"Đã qua · {n} bài\", action \"Vào chặng\" / \"Học tiếp\" / \"Xem lại\""),
               ("BDG", "Premium", "Chip", "Topic accessLevel PREMIUM", "\"Premium\""),
               ("PNL", "Lock reason", "Inline text", "LOCKED stations", "Server lockedReason, else \"Hoàn thành chặng trước để "
                "mở\"; a reason repeated on the next station is hidden"),
               ("CRD", "Bài thi cuối khóa", "Final station", "Hidden when the course has no final test (testStatus NONE)",
                "\"Về đích\" and a status text (see Interactions)"),
               ("BTN", "Làm bài thi cuối khóa", "Button (Accent)", "Only when testStatus = AVAILABLE", "\"Đang giao đề…\" "
                "while pending; chip \"Đã đạt\" when PASSED")],
        apis=[("Page load", "learn.topics", "—", "Keep the topics of this course · loading: topic skeleton",
               "GLB-04 / GLB-05 panel with \"Thử lại\""),
              ("Page load (parallel)", "learn.courses", "—", "Course title, band, testStatus and counts", "Same panel"),
              ("User clicks Làm bài thi cuối khóa", "learn.assignCourseTest", "Path: `courseId`", "Then API 4",
               "MSG-03…MSG-05"),
              ("Assignment received", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}` (COURSE_GATE)",
               "Navigate Test Attempt with `?course=`", "Server message + \" Hãy thử lại.\"")],
        interactions=[("Final station text", ["PASSED    → \"Bạn đã đạt bài thi cuối khóa.\"",
                                              "AVAILABLE → \"Bạn đã qua mọi chặng. Đề tổng hợp theo band mục tiêu, cần đạt từ 70%.\"",
                                              "LOCKED    → \"Mở khi qua đủ {n} chặng (hiện {passed}/{n}). Cần đạt từ 70%.\""]),
                      ("Station state", ["IF status = LOCKED THEN a disabled card with the lock reason",
                                         "ELSE a link to /learn/topics/{id}; the next IN_PROGRESS topic is marked as the "
                                         "current step"])],
        msgs=[("Empty State", "Course has no topic", "\"Khóa này chưa có chặng nào.\"", "—"),
              ("Success Text", "All topics passed", "\"Bạn đã qua tất cả các chặng.\"", "Header panel"),
              ("Inline Error", "403 TEST_LOCKED (the backend sends COURSE_TEST_LOCKED, which falls back to MSG-05: "
               "GAP-16)", "\"Bài thi cuối khóa chưa mở. Hoàn thành mọi chặng trong khóa trước.\"",
               "On the final station; page reloads"),
              ("Inline Error", "409 TEST_UNAVAILABLE", "\"Chưa có đề thi cuối cho khóa này. Hãy quay lại sau.\"",
               "On the final station"),
              ("Inline Error", "Other error (incl. NO_COURSE_TEST, COURSE_ALREADY_PASSED, COURSE_NOT_FOUND)",
               "Server message + \" Hãy thử lại.\"", "On the final station")],
        rules=[("BR-16", "One topic sequence per course; first unpassed topic IN_PROGRESS, later ones LOCKED",
                "Badges; LOCKED stations disabled"),
               ("BR-37", "Course test opens when every topic is PASSED; 70% passes the course", "Final station by testStatus"),
               ("BR-22", "Course test code single-use", "A new assignment for each try"),
               ("BR-34", "Premium content needs an entitlement", "Only a Premium chip (OQ-04)")],
    ),
    # ------------------------------------------------------------------ Topic Detail
    screen(
        key="TopicDetail", name="Topic Detail", route="/learn/topics/:topicId · RequireAuth", roles="Signed-in learner",
        ft="FT-20, FT-21, FT-27, FT-29", uc="UC: View learning path; Take test attempt", fe="api", status="Specified",
        frontend=FE_LEARN.format(page="TopicDetailPage.tsx", route="/learn/topics/:topicId"),
        shots=[("TopicDetail.png", "Topic Detail — lessons of a topic and its final test")],
        purpose="Lists the ordered lessons of a topic, points to the next step — a pending review, the next lesson, the "
                "practice a lesson still needs or the final test — and starts the topic final test.",
        nav_from=[("Course Path", "User clicks a station"),
                  ("Lesson Player, Practice Set, Test Result", "User goes back to the topic")],
        nav_to=[("Lesson Player", "User clicks an AVAILABLE or COMPLETED lesson, or the next-step button"),
                ("Practice Set", "User clicks \"Luyện ngay\" or the next-step button \"Luyện thêm\""),
                ("Review Session", "User clicks \"Làm bài ôn\" in the header or the unlock checklist"),
                ("Test Attempt", "User clicks \"Làm bài kiểm tra\" and the attempt is created"),
                ("Course Path", "User clicks the course back link")],
        pre=[REQUIRE_AUTH, ("Topic exists and is not LOCKED", "Continue", "TOPIC_LOCKED → /learn with notice (GLB-09); "
             "404 → GLB-03")],
        entry=[("Station click", "Course Path", "`topicId`"), ("Back link", "Lesson Player", "`topicId`")],
        exits=[("User clicks a lesson", "Not LOCKED", "Lesson Player", "—"),
               ("User clicks Làm bài kiểm tra", "testStatus AVAILABLE; APIs 4–5 succeed", "Test Attempt",
                "`/learn/tests/{attemptId}?topic={topicId}`"),
               ("Final test refused", "403 / 409", "Stay", "MSG-02…MSG-06"),
               ("User clicks the course link", "Any", "Course Path", "—"), EX_SESSION],
        comps=[("LNK", "{course title}", "Back link", "—", "To /learn/courses/{courseId}; \"Lộ trình khóa học\" → /learn when "
                "the topic has no course"),
               ("PNL", "Topic header", "Header", "—", "\"Chặng {nn} · {skill}\", title, description"),
               ("PRG", "Topic progress", "Meter", "—", "\"{done}/{total} bài đã xong\" and percent"),
               ("BTN", "Next step", "Button (Accent)", "First match: pending review → next lesson → lesson needing practice",
                "\"Cần làm trước · Bài ôn: {KP}\" + \"Làm bài ôn\"; \"Bắt đầu với\" / \"Tiếp theo · Bài {n}: {title}\" + "
                "\"Bắt đầu học\" / \"Học tiếp\"; \"Cần luyện thêm\" + \"Luyện thêm\"; else \"Đã học xong các bài. Làm bài "
                "kiểm tra chặng\" (jump) or \"Bạn đã qua chặng này · {p}%\""),
               ("TBL", "Bài học trong chặng", "Ordered route", "Sorted by sortOrder", "\"{n} bài · khoảng {m} phút\"; lesson "
                "title, \"{min} phút\", lock reason, action \"Học tiếp\" / \"Xem lại\" / \"Mở bài\""),
               ("LNK", "Luyện ngay", "Link", "Lesson COMPLETED and practice REQUIRED", "\"Cần luyện thêm trước khi mở bài kiểm "
                "tra chặng\""),
               ("CRD", "Kiểm tra cuối chặng", "Final station", "—", "Test title and \"{n} câu hỏi · cần đạt từ 70% · không "
                "giới hạn thời gian · lần gần nhất {p}%\"; \"Chặng này không có bài kiểm tra cuối.\" when NONE"),
               ("PNL", "Để mở bài kiểm tra", "Checklist", "Visible when testStatus = LOCKED", "Items with links (see "
                "Interactions)"),
               ("BTN", "Làm bài kiểm tra", "Button (Accent)", "Only when AVAILABLE", "\"Đang giao đề…\" while pending; chip "
                "\"Đã đạt\" when PASSED")],
        apis=[("Page load", "learn.topicLessons", "Path: `topicId`", "Render lessons and the final station · loading: "
               "\"Đang tải chặng học…\"", "GLB-03/04/09/10"),
              ("Page load (parallel)", "learn.topics", "— (cached)", "Topic title, skill, description and course",
               "Fallback title"),
              ("Page load (parallel)", "learn.reviews", "Query: `status=PENDING&limit=50`", "Next step and review banner",
               "Error → empty list"),
              ("User clicks Làm bài kiểm tra", "learn.assignTest", "Path: `topicId`", "Then API 5", "MSG-02…MSG-06"),
              ("Assignment received", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}` (TOPIC_GATE)",
               "Navigate Test Attempt", "Server message + \" Hãy thử lại.\"")],
        interactions=[("Unlock checklist (testStatus = LOCKED)", [
            "\"Hoàn thành {n} bài học còn lại\" (done: \"Hoàn thành tất cả bài học\") → first unfinished lesson",
            "IF lessons need practice → \"Hoàn thành phần luyện thêm của {n} bài\" → its practice",
            "IF pending reviews → \"Làm {n} bài ôn bắt buộc\" → first review",
            "IF every item is done → \"Hoàn tất phần luyện thêm và bài ôn còn treo (nếu có)\""]),
            ("Final test start", ["An in-flight guard stops a second click. learn.assignTest returns the open assignment "
                                  "when one exists, so a retry reuses the same test version (FT-29/AC-01)."])],
        msgs=[("Empty State", "Topic has no lesson", "\"Chặng này chưa có bài học.\"", "—"),
              ("Inline Error", "409 TEST_UNAVAILABLE / NO_TOPIC_TEST", "\"Chưa có đề cho chặng này. Hãy quay lại sau.\"",
               "On the final station"),
              ("Inline Error", "409 PRACTICE_REQUIRED", "\"Cần luyện thêm trước khi mở đề: Bài {n}, … Mở bài học tương ứng và "
               "hoàn thành phần luyện thêm.\"", "On the station; page reloads"),
              ("Inline Error", "403 REVIEW_REQUIRED", "\"Cần làm bài ôn “{KP}” trước khi làm bài kiểm tra.\"",
               "On the station; page reloads"),
              ("Inline Error", "403 TEST_LOCKED", "\"Bài kiểm tra chưa mở. Hoàn thành bài học, luyện thêm và bài ôn (nếu "
               "có).\"", "On the station; page reloads"),
              ("Inline Error", "Other error", "Server message + \" Hãy thử lại.\"", "On the station")],
        rules=[("BR-17", "Lessons open in order", "LOCKED lessons are not links; the lock reason is shown"),
               REVIEW_GATE_RULE,
               ("BR-22", "Topic test needs every lesson completed and practice-cleared; codes rotate",
                "Checklist; the open assignment is reused"),
               ("BR-38", "Practice is cleared skill by skill; essays never block", "\"Luyện ngay\" only while practice "
                "is REQUIRED"),
               ("BR-15", "Topic pass mark 70%", "\"cần đạt từ 70%\"")],
    ),
    # ------------------------------------------------------------------ Lesson Player
    screen(
        key="LessonPlayer", name="Lesson Player", route="/learn/lessons/:lessonId · RequireAuth", roles="Signed-in learner",
        ft="FT-21, FT-22, FT-23, FT-24, FT-25", uc="UC: Study lesson; Submit Writing task; Grade essay", fe="api", status="Specified",
        frontend=FE_LEARN.format(page="LessonPage.tsx with components/blocks (BlockList, TextBlock, PassageBlock, "
                                      "AudioBlock, ExerciseBlock, EssayBlock)", route="/learn/lessons/:lessonId"),
        shots=[("LessonPlayer.png", "Lesson Player — lesson content with the progress rail")],
        purpose="Presents a lesson's blocks in order — text, passage, audio, exercises and essays — grades each exercise "
                "block, so the learner can pass the lesson and move on.",
        nav_from=[("Topic Detail", "User clicks a lesson or the next-step button"),
                  ("Lesson Player", "User clicks \"Bài tiếp theo\""),
                  ("Review Session", "User clicks \"Học tiếp\" after the review")],
        nav_to=[("Practice Set", "User clicks \"Luyện thêm bài này\""),
                ("Review Session", "A review is pending; user clicks \"Làm bài ôn\""),
                ("Lesson Player (next)", "User clicks \"Bài tiếp theo\""),
                ("Topic Detail", "User clicks the topic link, \"Danh sách bài của chặng\", \"Về danh sách bài\" or "
                 "\"Về chặng làm bài kiểm tra\"")],
        pre=[REQUIRE_AUTH,
             ("No pending review in the lesson's skill", "Continue", "403 REVIEW_REQUIRED → review panel (GLB-10)"),
             ("Topic not LOCKED and earlier lessons completed", "Continue", "TOPIC_LOCKED → /learn; LESSON_LOCKED → topic "
              "(GLB-09)")],
        entry=[("Lesson row", "Topic Detail", "`lessonId`"), ("Bài tiếp theo", "Lesson Player", "`nextLessonId`")],
        exits=[("Last exercise block passes", "lessonCompleted = true", "Stay; completion panel", "MSG-04"),
               ("User clicks Luyện thêm / Bài tiếp theo / Về topic", "Completion panel", "Practice Set / next lesson / Topic "
                "Detail", "—"),
               ("Gate refused on load", "403", "Redirect or review panel", "GLB-09 / GLB-10"), EX_SESSION],
        comps=[("LNK", "{topic title}", "Back link", "—", "To the topic"),
               ("PNL", "Text block", "Rich text", "—", "—"),
               ("PNL", "Passage block", "Panel", "—", "\"Bài đọc\", paragraphs \"Đoạn {label}\""),
               ("AUD", "Audio block", "Native audio player", "—", "Title \"Bài nghe\", duration, no download; "
                "\"Transcript\" collapsible only when the API sends it"),
               ("RAD", "Choice question", "Radio group", "One answer per question", "\"Câu {n}\""),
               ("TXT", "Fill / short answer", "Text Input", "—", "Placeholder \"Nhập câu trả lời\""),
               ("BTN", "Nộp / Nộp lại", "Button (Primary)", "Disabled while submitting", "Shows \"Còn {n} câu chưa trả lời\""),
               ("BTN", "Làm lại từ đầu", "Button (Outline)", "Visible after a graded try", "Clears the answers"),
               ("PNL", "Per-question feedback", "Inline", "—", "\"Đúng\" / \"Sai\"; \"Gợi ý:\" when hint is not null; "
                "\"Đáp án:\" + explanation after the block passes"),
               ("TXA", "Bài làm của bạn (essay)", "Text Area", "Word count ≥ max(minWords, 50) and ≤ 1,000", "Placeholder "
                "\"Viết bài luận tại đây…\"; live \"{n} từ\""),
               ("BTN", "Nộp bài · 3 điểm", "Button (Primary)", "Disabled outside the word limits or when passed",
                "\"Đang chấm (có thể 10–45 giây)…\" while grading"),
               ("PNL", "Lesson header", "Header", "—", "\"Bài {n} · Lý thuyết và bài tập\" or \"Bài {n} · Lý thuyết\", title"),
               ("PNL", "Tiến độ bài học", "Side rail (bottom bar on mobile)", "—", "\"Bài {n} · {topic}\", state with a hint "
                "(see Interactions), primary action, link \"Danh sách bài của chặng\""),
               ("BTN", "Primary action", "Button (Accent)", "By lesson state", "\"Làm bài ôn\" / \"Luyện thêm bài này\" / "
                "\"Bài tiếp theo\" or \"Về chặng làm bài kiểm tra\" / \"Hoàn thành bài\" (\"Đang ghi nhận…\") / \"Đến phần "
                "bài tập\""),
               ("PNL", "Completion panel", "Panel (role=status)", "Lesson COMPLETED", "\"Bạn đã hoàn thành bài này\" / \"Bài "
                "này đã hoàn thành\", message, links \"Luyện thêm bài này\" and \"Về danh sách bài\"")],
        apis=[("Page load", "learn.lesson", "Path: `lessonId`", "Render blocks · loading: \"Đang tải nội dung bài học…\"",
               "GLB-03/04/09/10"),
              ("Page load (after the lesson)", "learn.topics", "— (cached)", "Topic title for the back link", "—"),
              ("Page load (after the lesson)", "learn.topicLessons", "Path: `topicId`", "Next lesson and practice status "
               "for the rail; reloaded after completion", "Rail without the next lesson"),
              ("User clicks Nộp", "learn.submitBlock", "`{requestId, answers:[{questionVersionId, answer}]}`",
               "Mark answers, hints, solutions on pass; completion panel when lessonCompleted", "Server message + \" Hãy thử "
               "nộp lại.\""),
              ("User clicks Hoàn thành bài", "learn.complete", "Path: `lessonId`", "Completion panel", "Server message + "
               "\" Hãy thử lại.\""),
              ("User clicks Nộp bài · 3 điểm", "learn.submitEssay", "`{requestId, essayText}` (timeout 60 s)",
               "Inline result (see Writing Feedback); then access.myPoints", "MSG-06…MSG-10"),
              ("Essay graded", "access.myPoints", "—", "Update the balance in the user menu", "Ignored")],
        interactions=[
            ("Exercise result", [
                "IF the block passed",
                "THEN   \"Đạt {c}/{t} câu ({p}%). Đáp án và giải thích hiện dưới từng câu.\"; answers locked",
                "ELSE   \"Đúng {c}/{t} câu ({p}%). Cần từ 70% để đạt. Sửa các câu sai rồi nộp lại.\"; hints shown",
                "END IF",
                "Each try sends a new requestId (no reuse after a network error)."]),
            ("Essay pre-check", ["IF balance < 3 THEN \"Không đủ điểm. Cần 3 điểm để chấm bài (hiện có {n}).\" and no request",
                                 "The balance comes from the session (access.myPoints)."]),
            ("Rail state", ["Review pending      → \"Cần làm bài ôn\": \"Hoàn thành bài ôn “{KP}” để học tiếp.\"",
                            "Done, practice due  → \"Cần luyện thêm\": \"Luyện thêm bài này để mở bài kiểm tra chặng.\"",
                            "Done                → \"Đã hoàn thành\": \"Sẵn sàng sang bài tiếp theo.\" or, for the last",
                            "                      lesson, \"Đây là bài cuối của chặng. Tiếp theo là bài kiểm tra chặng.\"",
                            "Has exercises       → \"Đang học\": \"Làm và nộp bài tập trong bài để hoàn thành.\"",
                            "Reading only        → \"Đang học\": \"Đọc hết nội dung rồi bấm hoàn thành để ghi nhận tiến độ.\""]),
        ],
        msgs=[("Inline Result", "Block passed", "\"Đạt {c}/{t} câu ({p}%). Đáp án và giải thích hiện dưới từng câu.\"",
               "Persistent"),
              ("Inline Result", "Block not passed", "\"Đúng {c}/{t} câu ({p}%). Cần từ 70% để đạt. Sửa các câu sai rồi nộp "
               "lại.\"", "Persistent"),
              ("Info Text", "Lesson without exercises", "\"Hết nội dung bài học. Bấm Hoàn thành bài để ghi nhận tiến độ.\"",
               "Under the content"),
              ("Completion Panel", "Lesson completed", "\"Bạn đã hoàn thành bài này\" · \"Kiến thức của bài đã được ghi nhận. Bạn "
               "có thể sang bài tiếp theo.\" (last lesson: \"Bạn đã học xong bài cuối của chặng. Quay về chặng để làm bài kiểm "
               "tra.\"; practice due: \"Bài này yêu cầu luyện thêm trước khi mở bài kiểm tra chặng.\")", "Persistent"),
              ("Completion Panel", "Review pending", "\"Bạn cần củng cố lại “{KP}” trước khi học tiếp. Bài ôn gồm tóm tắt lý "
               "thuyết và vài câu hỏi ngắn.\"", "Rail button \"Làm bài ôn\""),
              ("Inline Error", "Balance below 3 (client) / 402 INSUFFICIENT_POINTS", "\"Không đủ điểm. Cần 3 điểm để chấm bài (hiện có {n}).\" / "
               "\"Không đủ điểm để chấm bài luận.\"", "Below the editor"),
              ("Inline Error", "503 GRADING_UNAVAILABLE", "\"Hệ thống chấm bài tạm thời không khả dụng. Thử lại sau.\"",
               "Below the editor"),
              ("Inline Error", "429 DAILY_LIMIT_REACHED", "\"Bạn đã hết hạn mức chấm bài trong ngày.\"", "Below the editor"),
              ("Inline Error", "Other essay error (422 ESSAY_EMPTY / ESSAY_TOO_SHORT / ESSAY_TOO_LONG, 409 "
               "GRADING_IN_PROGRESS / REQUEST_CONFLICT)", "Server message + \" Hãy thử lại.\"", "Below the editor"),
              ("Inline Error", "503 PAYMENT_UNAVAILABLE (graded, point debit failed)", "Today: server message + \" Hãy thử "
               "lại.\" Design target: \"Bài đã chấm xong nhưng chưa trừ được điểm. Bấm Nộp lại để hoàn tất (không chấm "
               "lại).\" and resend the same requestId (GAP-13)", "Below the editor")],
        rules=[("BR-15", "Pass mark 70%", "MSG-01 / MSG-02 show the score and the threshold"),
               ("BR-18", "Only the first submission records evidence", "Not stated on the screen"),
               ("BR-19", "Answers hidden until the block passes; hints after a wrong answer",
                "Solutions rendered only for a passed block"),
               REVIEW_GATE_RULE, ("BR-10, BR-11", "3 points per essay; 10 gradings per day", "MSG-06 / MSG-08"),
               ("NFR-P03", "Grade or 503 GRADING_UNAVAILABLE within 45 s; each LLM call stops after 20 s",
                "Client timeout 60 s (Section 2.0) covers it; MSG-07"),
               ("BR-38", "A lesson is completed by its objective blocks; Writing essays never block it",
                "Completion panel after the last objective block")],
    ),
    # ------------------------------------------------------------------ Writing Feedback
    screen(
        key="WritingFeedback", name="Writing Feedback", route="Inline panel in /learn/lessons/:lessonId (EssayBlock)",
        roles="Signed-in learner", ft="FT-12, FT-25", uc="UC: Submit Writing task; Grade essay; View points", fe="partial",
        status="Specified (separate page and re-reading a submission: GAP-08)",
        frontend="Partly built — feedback is rendered inside EssayBlock.tsx on the Lesson Player; there is no separate "
                 "route and learn.essay is not called (GAP-08).",
        purpose="Shows the AI grading of a Writing essay — estimated band, pass or not, criteria, corrections and the "
                "sample answer — so the learner can improve the next essay.",
        nav_from=[("Lesson Player", "Essay graded, or the block's latest submission is GRADED on load")],
        nav_to=[("Lesson Player", "Same page — the learner keeps studying")],
        pre=[REQUIRE_AUTH],
        entry=[("Essay graded", "Lesson Player", "Submission response"),
               ("Lesson load", "Lesson Player", "`latestSubmission` with status GRADED (band and pass only)")],
        exits=[("User edits and submits again", "Not passed", "Same panel", "Next grading")],
        comps=[("BDG", "Đã đạt", "Badge", "Shown when passed", "Editor hidden once passed"),
               ("PNL", "Band ước lượng", "Text", "—", "\"Band ước lượng: {band} · Đạt / Chưa đạt\""),
               ("PNL", "Summary", "Text", "—", "—"),
               ("TBL", "Criteria", "List", "TA/TR, CC, LR, GRA", "Band + first improvement"),
               ("TBL", "Corrections", "List", "First 5 shown", "~~excerpt~~ → suggestion"),
               ("PNL", "Bài mẫu", "Collapsible", "Only when sampleAnswer is sent", "—"),
               ("PNL", "Disclaimer", "Text", "—", "\"Band là ước lượng AI, không phải điểm IELTS chính thức.\"")],
        apis=[("Essay submitted (Lesson Player)", "learn.submitEssay", "`{requestId, essayText}`", "Render the panel from the "
               "response", "See Lesson Player MSG-06…MSG-10"),
              ("Design target: reopen a result", "learn.essay", "Path: `submissionId`", "Show FAILED / grading states after "
               "reload", "Not called today (GAP-08)")],
        interactions=[("Result display", ["IF status = GRADED THEN show band, summary, criteria, corrections, sample answer",
                                          "Other statuses (FAILED, PAYMENT_PENDING) show only the error message of the "
                                          "request (GAP-08)."])],
        msgs=[("Info Text", "Always under the editor", "\"Band là ước lượng AI, không phải điểm IELTS chính thức.\"", "—"),
              ("Info Text", "Essay header", "\"Tối thiểu {min} từ · đạt từ band {pass} · mỗi lần chấm tốn 3 điểm (còn {n})\"",
               "—")],
        rules=[("BR-10", "3 points per grading, after success", "Cost shown on the button and the header"),
               ("BR-11", "10 gradings per day", "Lesson Player MSG-08")],
    ),
    # ------------------------------------------------------------------ Practice Set
    screen(
        key="PracticeSet", name="Practice Set", route="/learn/lessons/:lessonId/practice · RequireAuth",
        roles="Signed-in learner", ft="FT-23, FT-24, FT-26, FT-27", uc="UC: Do lesson practice", fe="api",
        status="Specified (Premium sets disabled: GAP-05; Writing questions not answerable: GAP-18)",
        frontend=FE_LEARN.format(page="PracticePage.tsx (list and attempt on the same route)",
                                 route="/learn/lessons/:lessonId/practice"),
        purpose="Lists the practice sets of a completed lesson and lets the learner answer one set, see every solution and "
                "start the review it creates.",
        nav_from=[("Lesson Player", "User clicks \"Luyện thêm\""), ("Topic Detail", "User clicks \"Mở luyện thêm\" or an "
                  "unlock hint")],
        nav_to=[("Review Session", "A review was created; user clicks \"Làm bài ôn\""),
                ("Course List", "Practice PASSED; user clicks \"Về lộ trình\""), ("Lesson Player", "User clicks \"Về bài học\"")],
        pre=[REQUIRE_AUTH, ("No pending review in the skill", "Continue", "403 REVIEW_REQUIRED → review panel (GLB-10)")],
        entry=[("Luyện thêm", "Lesson Player", "`lessonId`"), ("Mở luyện thêm", "Topic Detail", "`lessonId`")],
        exits=[("User clicks Làm bộ này / Làm lại", "Set not LOCKED or PREMIUM", "Same route, attempt view", "—"),
               ("Submission done", "API 200", "Same route, result view", "MSG-02…MSG-05"),
               ("User clicks Danh sách luyện thêm / Về danh sách", "Any", "Same route, list view", "List reloads"), EX_SESSION],
        comps=[("PNL", "Header", "Header", "—", "\"Luyện thêm\" · \"Củng cố trước khi mở bài kiểm tra chặng\" · REQUIRED "
                "\"Bắt buộc: đạt ít nhất một bộ để mở bài kiểm tra chặng\", PASSED \"Đã đạt phần luyện thêm\", LOCKED \"Chưa "
                "mở\" (+ \" · Hoàn thành bài học trước khi luyện.\")"),
               ("CRD", "Practice set card", "Card list", "—", "Title, \"{n} câu · điểm cao nhất {p}%\", \"Đã lộ đáp án nên không "
                "được tính.\" when revealed"),
               ("BDG", "Set status", "Badge", "AVAILABLE \"Sẵn sàng\", IN_PROGRESS \"Đang làm\", PASSED \"Đã đạt\", ATTEMPTED "
                "\"Đã thử\", LOCKED \"Chưa mở\"", "—"),
               ("BTN", "Làm bộ này / Làm tiếp / Làm lại", "Button", "Hidden on LOCKED sets; disabled on PREMIUM sets",
                "The set in progress (else the first open set) is the accent button; \"Đang mở…\" while starting"),
               ("PNL", "Passage / audio", "Split view", "—", "Left side of the attempt view"),
               ("PNL", "Questions", "ExerciseBlock (no resubmit)", "One answer per question", "Instructions \"Trả lời hết câu. "
                "Đạt mới được tính evidence cho practice của bài.\""),
               ("PNL", "Result", "Panel", "—", "Solutions under each question; review link or success panel")],
        apis=[("Page load", "learn.practiceSets", "Path: `lessonId`", "Render the list · loading: \"Đang tải luyện thêm…\"",
               "GLB-03/04/10"),
              ("User clicks Làm bộ này", "learn.startPractice", "`{packageId}`", "Show the attempt (open attempt reused)",
               "Server message (409 PRACTICE_LOCKED, 403 REVIEW_REQUIRED)"),
              ("User clicks Nộp", "learn.submitPractice", "`{requestId, answers}`", "Show score, solutions, transcript; store "
               "created reviews for the banner", "Server message + \" Hãy thử nộp lại.\"")],
        interactions=[("Result outcome", [
            "IF passed → \"Đã đạt bộ luyện thêm ({p}%).\" + \"Tiếp tục luyện / về topic\"",
            "ELSE → \"Chưa đạt ({p}%). Có thể chọn bộ khác hoặc làm bài ôn nếu được tạo.\" + \"Về danh sách\"",
            "IF reviewsCreated not empty → \"Đã tạo bài ôn bắt buộc sau lần luyện này.\" + \"Làm bài ôn\""])],
        msgs=[("Empty State", "No practice set", "\"Bài này không có bộ luyện thêm.\"", "—"),
              ("Success Panel", "Set passed", "\"Đã đạt bộ luyện thêm ({p}%).\"", "Button \"Tiếp tục luyện / về topic\""),
              ("Inline Result", "Set not passed", "\"Chưa đạt ({p}%). Có thể chọn bộ khác hoặc làm bài ôn nếu được tạo.\"",
               "Button \"Về danh sách\""),
              ("Review Panel", "Review created", "\"Đã tạo bài ôn bắt buộc sau lần luyện này.\"", "Button \"Làm bài ôn\""),
              ("Info Text", "Set already revealed", "\"Bộ này đã lộ đáp án trước đó nên lần nộp này không được tính.\"",
               "Attempt header"),
              ("Success Panel", "Practice PASSED for the lesson", "\"Bạn đã đạt phần luyện thêm. Quay lại chặng để làm bài kiểm "
               "tra (nếu không còn bài ôn).\"", "Button \"Về lộ trình\""),
              ("Info Text", "Premium set", "\"Bộ Premium, chưa mở trong giai đoạn này.\"", "On the card")],
        rules=[("BR-15", "Pass mark 70%", "MSG-02 / MSG-03"),
               ("BR-18", "Only the first submission of an unrevealed set records evidence", "MSG-05"),
               ("BR-20", "A revealed set is never reused as a review set", "\"Đã lộ đáp án\" note"),
               ("BR-21", "Reviews open for knowledge points below 70%", "MSG-04"), REVIEW_GATE_RULE,
               ("BR-34", "Premium sets need an entitlement", "Always disabled today (GAP-05)"),
               ("BR-38", "Practice is cleared skill by skill for Reading and Listening; essays never block",
                "Writing questions of a set have no essay box yet (GAP-18)")],
    ),
    # ------------------------------------------------------------------ Review List (not built)
    screen(
        key="ReviewList", name="Review List", route="/learn/reviews (proposed)", roles="CUSTOMER",
        ft="FT-28", uc="UC: Complete review", status="Specified (backend); screen not built (GAP-07)",
        purpose="Lists the learner's reviews, oldest first, so the learner can finish the ones that block a skill.",
        nav_from=[("Course Path / Topic Detail / Lesson Player", "User clicks the pending-review banner or link"),
                  ("Main menu", "User clicks \"Reviews\"")],
        nav_to=[("Review Session", "User clicks a review")],
        pre=[AUTH, LEARNER],
        entry=[("Pending-review banner", "Course Path, Topic Detail, Lesson Player", "Query: `?skill=`"),
               ("Main menu", "Any learner screen", "—")],
        exits=[("User clicks a review", "Any", "Review Session", "—"), ex_menu(), EX_SESSION],
        comps=[("SEL", "Status filter", "Select", "PENDING (default), DONE, SKIPPED", "—"),
               ("SEL", "Skill filter", "Select", "All, LISTENING, READING, WRITING, SPEAKING", "—"),
               ("TBL", "Reviews", "Table", "Oldest first; 20 per page (limit 1–100)", "Columns: knowledge point, skill, "
                "stage (PRACTICE / THEORY), created"),
               ("BTN", "Load more", "Button (Secondary)", "—", "Raises limit by 20 up to 100")],
        apis=[("Page load / filter change", "learn.reviews", "Query: `status, skill, limit`", "Render rows · loading: "
               "skeleton", "400 INVALID_LIMIT → reset to 20"),
              ("Page load (parallel)", "content.kps", "—", "Map knowledgePointId → name", "Error → show id")],
        interactions=[("Knowledge point names", ["Names come from one content.kps call and are joined in memory by id;",
                                                 "the screen never calls one request per row."])],
        msgs=[("Empty State", "No PENDING review", "Title: \"No reviews to do\" · Description: \"Reviews appear when a "
               "practice set or test shows a weak point.\"", "CTA: \"Back to learning path\""),
              ("Info Banner", "PENDING reviews exist", "\"Each pending review blocks the lessons, practice and final test "
               "of its own skill.\"", "Persistent")],
        rules=[("BR-17", "Pending review blocks only its own skill", "MSG-02"),
               ("FT-28 BV-04", "List limit 1–100, default 20", "Load more stops at 100")],
    ),
    # ------------------------------------------------------------------ Review Session
    screen(
        key="ReviewSession", name="Review Session", route="/learn/reviews/:reviewId · RequireAuth",
        roles="Signed-in learner", ft="FT-23, FT-24, FT-26, FT-28", uc="UC: Complete review", fe="api",
        status="Specified (empty quick-check: GAP-12)",
        frontend=FE_LEARN.format(page="ReviewPage.tsx", route="/learn/reviews/:reviewId"),
        purpose="Guides the learner through one review — theory with a quick check, then a set of questions — so a weak "
                "knowledge point is repaired and the skill is unblocked.",
        nav_from=[("Review banner / panel", "User clicks \"Làm bài ôn\""), ("Lesson Player, Practice Set, Topic Detail",
                  "A review was created")],
        nav_to=[("Lesson Player or Topic Detail", "Review DONE or SKIPPED; user clicks \"Học tiếp\"")],
        pre=[REQUIRE_AUTH, owner_only("review")],
        entry=[("Làm bài ôn", "Banner, Lesson Player, Practice Set", "`reviewId`")],
        exits=[("Set passed", "reviewStatus DONE", "Stay; done panel", "MSG-03"),
               ("Second failure / no set left", "reviewStatus SKIPPED", "Stay; skipped panel", "MSG-02"),
               ("User clicks Học tiếp", "DONE or SKIPPED", "resumeLessonId lesson, else the topic, else /learn", "—"),
               EX_SESSION],
        comps=[("LNK", "Về lộ trình học", "Back link", "—", "To the lesson to resume, else the topic, else /learn"),
               ("PNL", "Header", "Header", "—", "\"Bài ôn bắt buộc · Bước đọc lý thuyết\" or \"· Bước luyện tập\"; \"Số bộ chưa "
                "đạt: {n}/{max}.\" after a failed set"),
               ("PNL", "Lý thuyết cần nhớ", "Rich text", "—", "Theory blocks of the knowledge point"),
               ("PNL", "Kiểm tra nhanh lý thuyết", "Questions", "THEORY stage", "\"Đọc lại lý thuyết rồi trả lời vài câu kiểm tra "
                "nhanh để mở bộ luyện ôn.\""),
               ("BTN", "Confirm read", "Button", "Only when there is no quick-check", "\"Không có câu kiểm tra nhanh. Xác nhận đã "
                "đọc lý thuyết để sang bước luyện tập.\" (GAP-12)"),
               ("PNL", "Bộ câu hỏi ôn", "Questions + passage / \"Audio ôn\"", "PRACTICE stage", "\"Làm bộ câu hỏi ôn. Đạt là hoàn "
                "thành; chưa đạt thì đọc lại lý thuyết hoặc làm bộ khác.\""),
               ("BTN", "Làm bộ tiếp theo", "Button (Secondary)", "After a failed set", "Reloads the review"),
               ("BTN", "Học tiếp", "Button (Primary)", "DONE or SKIPPED", "—")],
        apis=[("Page load", "learn.review", "Path: `reviewId`", "Render the stage · loading: \"Đang tải bài ôn…\"",
               "GLB-03/04"),
              ("User submits the set", "learn.review", "Path: `reviewId` (re-read for lessonId)", "Then API 3", "—"),
              ("User submits the set", "learn.submitReview", "`{reviewSetId, requestId, answers}`", "Show results; follow stage "
               "/ status", "409 REVIEW_SET_CLOSED (set already submitted) / THEORY_REQUIRED (theory first), BR-21 → server "
               "message + \" Hãy thử nộp lại.\""),
              ("User submits the quick check", "learn.theoryCheck", "`{requestId, answers}`", "Show results; reload for the new "
               "set", "422 INVALID_ANSWERS when there is no quick-check (GAP-12)")],
        interactions=[("Ladder", ["THEORY: quick check (no evidence) → PRACTICE with a new set",
                                  "PRACTICE: set ≥ 70% → DONE (MSG-03)",
                                  "PRACTICE: set < 70% → \"Chưa đạt ({n}/{max} bộ). Thử bộ tiếp theo.\"",
                                  "Second failed set or no unseen set → SKIPPED (MSG-02)"])],
        msgs=[("Inline Result", "Set below 70%", "\"Chưa đạt ({n}/{max} bộ). Thử bộ tiếp theo.\"", "Button \"Làm bộ tiếp "
               "theo\""),
              ("Info Panel", "Review SKIPPED", "\"Đã bỏ qua bài ôn\" · \"Bạn chưa đạt sau {max} bộ. Bài ôn được bỏ qua để bạn tiếp "
               "tục học.\"", "Button \"Học tiếp\""),
              ("Success Panel", "Review DONE", "\"Đã hoàn thành bài ôn\" · \"Lộ trình đã mở lại. Bạn có thể học tiếp.\"",
               "Button \"Học tiếp\"")],
        rules=[("BR-21", "Set → theory (≤ 3 quick checks, no evidence) → set; 70% DONE; second fail SKIPPED", "Stage header and "
                "panels"),
               ("BR-20", "Revealed packages are never reused", "SKIPPED panel when no set remains"),
               ("FT-28/NAC-02", "A review with no quick-check → 422", "The confirm button fails today (GAP-12)")],
    ),
    # ------------------------------------------------------------------ Mastery (not built)
    screen(
        key="Mastery", name="Mastery", route="/learn/mastery (proposed)", roles="CUSTOMER",
        ft="FT-26", uc="UC: View mastery", status="Specified (backend); screen not built (GAP-07)",
        purpose="Shows the learner's mastery from 0 to 1 for every knowledge point, grouped by skill and topic, so the "
                "learner can see strong and weak points.",
        nav_from=[("Course Path", "User clicks \"View mastery\""), ("Main menu", "User clicks \"Mastery\"")],
        nav_to=[("Topic Detail", "User clicks a topic group header")],
        pre=[AUTH, LEARNER],
        entry=[("View mastery link", "Course Path", "—"), ("Main menu", "Any learner screen", "Query: `?skill=`")],
        exits=[("User clicks a topic", "Topic not LOCKED", "Topic Detail", "—"), ex_menu(), EX_SESSION],
        comps=[("TAB", "Skill tabs", "Tab bar", "LISTENING, READING, WRITING, SPEAKING", "—"),
               ("TBL", "Knowledge points", "Table grouped by topic", "Sortable: mastery, evidence count", "Name, mastery "
                "bar, evidence count"),
               ("BDG", "Mastery level", "Badge", "< 0.6→red \"Needs work\", 0.6–0.8→amber, ≥ 0.8→green", "0 evidence→grey "
                "\"Not started\"")],
        apis=[("Page load (mount)", "learn.mastery", "—", "Render table · loading: skeleton", "503 → GLB-05"),
              ("Page load (parallel)", "content.kps", "—", "Join names by id in memory", "Error → show ids")],
        interactions=[("Display rule", ["Mastery is shown as a percentage bar (mastery × 100). With one or two pieces of",
                                        "evidence the value is capped (0.5 / 0.8); the tooltip says \"More practice "
                                        "gives a more reliable score\"."])],
        msgs=[("Empty State", "No knowledge points", "Title: \"Nothing measured yet\" · Description: \"Finish an exercise "
               "block to see your mastery.\"", "CTA: \"Go to learning path\"")],
        rules=[("BR-23", "Latest 5 outcomes weighted; caps 0.5 / 0.8", "Tooltip on low-evidence rows"),
               ("BR-23", "Mastery < 0.6 opens a review after an assessment", "Red badge \"Needs work\"")],
    ),
    # ------------------------------------------------------------------ Test Attempt
    screen(
        key="TestAttempt", name="Test Attempt", route="/learn/tests/:attemptId?topic=… | ?course=… · RequireAuth",
        roles="Signed-in learner", ft="FT-24, FT-30, FT-31, FT-55", uc="UC: Take test attempt; Take course test",
        fe="api", status="Specified (no timer: GAP-09)",
        frontend=FE_LEARN.format(page="TopicTestPage.tsx", route="/learn/tests/:attemptId"),
        purpose="Lets the learner sit a topic final test or a course final test section by section, with answers saved "
                "as they go, and submit it for grading.",
        nav_from=[("Topic Detail", "Topic final test assigned and attempt created"),
                  ("Course Path", "Course final test assigned and attempt created")],
        nav_to=[("Test Result", "Submit succeeds"), ("Topic Detail", "User clicks \"Về topic\""),
                ("Course Path", "User clicks \"Về khóa học\"")],
        pre=[REQUIRE_AUTH, ("Attempt belongs to the user", "Continue", "403 → error panel")],
        entry=[("Làm bài kiểm tra", "Topic Detail", "`attemptId`, Query `topic`"),
               ("Làm bài thi cuối khóa", "Course Path", "`attemptId`, Query `course`")],
        exits=[("User clicks Nộp bài (all saved)", "API 200", "Test Result", "`/learn/tests/{id}/result?topic=…`, replace"),
               ("Save failed for some answer", "—", "Stay", "MSG-02"), ("User clicks Về topic", "Any", "Topic Detail",
               "Saved answers stay on the server"), EX_SESSION],
        comps=[("LNK", "Về topic / Về khóa học", "Back link", "—", "To the topic or the course"),
               ("PNL", "Header", "Header", "—", "\"Bài kiểm tra cuối\" (course: \"Thi cuối khóa\") · \"Làm từng phần, câu trả "
                "lời được lưu ngay\" · \"Không giới hạn thời gian\""),
               ("TAB", "Các phần của đề", "Section list", "—", "\"Phần {n}\" with \"{done}/{total} câu\""),
               ("PNL", "Section content", "Passage / audio", "—", "No transcript during the test"),
               ("RAD", "Answer", "Radio (choices) / Text Input", "—", "Choice answers save on change"),
               ("BDG", "Save state", "Inline text", "\"Đang lưu…\" / \"Đã lưu\" / \"Chưa lưu được, sẽ thử lại khi nộp\"", "Per item"),
               ("BTN", "Phần trước / Phần tiếp", "Buttons", "—", "\"Phần {k}/{n}\""),
               ("BTN", "Nộp bài / Vẫn nộp", "Button (Primary)", "Second click needed when answers are missing",
                "\"Đang nộp…\" while pending")],
        apis=[("Page load", "as.structure", "Path: `attemptId`", "Render sections · loading: \"Đang tải đề…\"", "GLB-03/04"),
              ("Choice picked, text field left, and every answer at submit", "as.save", "`{payload: {answer}, schemaVersion: 1, "
               "expectedRevision}`", "Badge \"Đã lưu\"; keep the new revision", "Badge \"Chưa lưu được, sẽ thử lại khi nộp\""),
              ("User confirms Nộp bài", "as.submit", "Path: `attemptId`", "Navigate Test Result", "409 ATTEMPT_ALREADY_SUBMITTED / "
               "ATTEMPT_EXPIRED → server message + \" Hãy thử nộp lại.\"")],
        interactions=[("Submit", ["IF unanswered > 0 AND not confirmed → show MSG-01, button becomes \"Vẫn nộp bài\"",
                                  "Save every answered item (queued per item); IF any save fails → MSG-02 and stop",
                                  "ELSE submit and open the result"]),
                      ("Saves per item", ["Saves of one item run one after another; each sends the last revision so a "
                                          "stale save is refused by the server."])],
        msgs=[("Warning Text", "Unanswered items on first submit", "\"Còn {n} câu bỏ trống, các câu đó sẽ tính sai. Bấm lần nữa "
               "để nộp.\"", "Until the next change"),
              ("Inline Error", "A save failed at submit", "\"Chưa lưu được một số câu trả lời. Kiểm tra các câu có cảnh báo rồi nộp "
               "lại.\"", "Persistent"),
              ("Empty State", "Structure has no question", "\"Đề này chưa có câu hỏi.\"", "—"),
              ("Item Badge", "409 on save: stale revision (another tab or device saved first) or ATTEMPT_EXPIRED",
               "Today: \"Chưa lưu được, sẽ thử lại khi nộp\". Design target: stale → reload the item answer and revision "
               "from as.attempt; expired → \"Bài làm đã hết thời gian. Các câu đã lưu được giữ lại.\" and open Test "
               "Result (GAP-09)", "Per item"),
              ("Inline Error", "409 ATTEMPT_EXPIRED on submit", "Today: server message + \" Hãy thử nộp lại.\" Design "
               "target: same text as MSG-04", "Persistent")],
        rules=[("FT-30", "Frozen snapshot; no answers, hints or transcripts in the structure", "Nothing to hide client-side"),
               ("FT-30 BV-01", "Save needs the current revision", "Revision kept per item"),
               ("BR-39", "Essays of topic and course tests are graded by the LLM after submit (JOB-03)",
                "No essay box in the test runner yet; an essay never sent scores 0")],
    ),
    # ------------------------------------------------------------------ Test Result
    screen(
        key="TestResult", name="Test Result", route="/learn/tests/:attemptId/result?topic=… | ?course=… · RequireAuth",
        roles="Signed-in learner", ft="FT-14, FT-24, FT-29, FT-31, FT-33, FT-55",
        uc="UC: View result; Take course test; Request AI grading; Request examiner grading", fe="api",
        status="Specified (examiner and AI grading requests: Draft)",
        frontend=FE_LEARN.format(page="TopicTestResultPage.tsx", route="/learn/tests/:attemptId/result"),
        shots=[("TestResult.png", "Test Result — a topic final test result")],
        purpose="Shows the score of the submitted topic or course test, per-question correctness and — at 70% or more — "
                "the correct answers, and tells the learner whether the topic or the course is passed.",
        nav_from=[("Test Attempt", "Submit succeeded")],
        nav_to=[("Topic Detail (next topic)", "Topic passed; user clicks \"Sang chặng tiếp theo\""),
                ("Course Path", "Course passed: \"Về khóa học\"; failed: \"Về khóa để làm lại\""),
                ("Course List", "User clicks \"Xem danh sách khóa\""),
                ("Topic Detail", "Topic failed; user clicks \"Về topic để làm lại\"")],
        pre=[REQUIRE_AUTH, ("Attempt belongs to the user", "Continue", "403 → error panel")],
        entry=[("Submit", "Test Attempt", "`attemptId`, Query `topic` or `course`")],
        exits=[("User clicks a navigation button", "Any", "Topic Detail, Course Path or Course List", "—"), EX_SESSION],
        comps=[("PRG", "Score dial", "Ring", "—", "Percent"),
               ("PNL", "Outcome", "Header", "—", "\"Kết quả · mã đề {code}\" · \"Đạt bài kiểm tra cuối\" (course: \"Đạt thi "
                "cuối khóa\") / \"Chưa đạt lần này\""),
               ("PNL", "Score line", "Text", "—", "\"{score}/{max} câu đúng · {p}% · cần từ 70%\""),
               ("TBL", "Từng câu", "List", "—", "\"Câu {n}\" ✓/✗; \"Đáp án: {answer}\" only when passed"),
               ("BTN", "Sang chặng tiếp theo / Về khóa học / Xem danh sách khóa / Về topic (khóa) để làm lại", "Buttons",
                "—", "By outcome and test kind")],
        apis=[("Page load", "as.result", "Path: `attemptId`", "Render score and items · loading: \"Đang chấm bài…\"",
               "404 / 403 → error panel"),
              ("Topic passed: re-read the topics up to 4 times, 400 ms apart", "learn.topics", "—", "Find the PASSED "
               "topic and the next IN_PROGRESS topic", "Error → panel"),
              ("Course passed: re-read the courses up to 4 times, 400 ms apart", "learn.courses", "—", "Find the course "
               "with testStatus PASSED", "Error → panel"),
              ("Design target: user clicks \"Chấm bằng AI\" on a Writing/Speaking item [Draft]", "as.submission",
               "`{attemptItemId, skill, textPayload | audioReference, submissionKey}`", "Then API 4", "400 → GLB-06"),
              ("Design target: submission created [Draft]", "as.createJob", "`{submissionId, skill, gradingMode, "
               "idempotencyKey}`", "Show the job status", "402 → \"Bạn không đủ điểm để chấm bằng AI.\""),
              ("Design target: every 5 s until final [Draft]", "as.job", "Path: `id`", "GRADED → reload API 1", "403 → GLB-02"),
              ("Design target: user clicks \"Gửi giáo viên chấm\" [Draft]", "access.consumeCredit", "Path: `userId` "
               "(learner flow [TBC])", "Credits left shown", "402 → \"Bạn đã hết lượt chấm giáo viên.\"")],
        interactions=[("Outcome text", [
            "IF passed AND a next topic is IN_PROGRESS → \"Topic đã qua. Chặng tiếp theo đã mở (làm mới từ GET /topics).\"",
            "ELSE IF passed → \"Topic đã qua. Bạn đã hoàn thành toàn bộ lộ trình hiện có.\"",
            "ELSE → \"Đáp án được ẩn khi chưa đạt. Ôn lại các bài trong topic rồi làm lại; lần sau bạn sẽ nhận mã đề khác.\"",
            "Course test: passed and course PASSED → \"Khóa đã đánh dấu PASSED (poll GET /courses). Bạn có thể chọn khóa",
            "             khác hoặc xem lại lộ trình.\"; passed, not yet PASSED → \"Đã đạt điểm. Hệ thống đang cập nhật trạng",
            "             thái khóa — làm mới danh sách khóa nếu chưa thấy PASSED.\"; failed → \"Đáp án được ẩn khi chưa đạt.",
            "             Ôn lại các topic rồi làm lại; lần sau bạn sẽ nhận mã đề khác.\""]),
            ("Why it re-reads topics", ["The topic is passed asynchronously by JOB-02 after JOB-01 relays the event "
                                        "(relay every 2 s; NFR-P05 allows up to 5 s), so the page re-reads /topics "
                                        "before showing the next topic.",
                                        "Today it re-reads 4 times, 400 ms apart (about 1.6 s), which is often too "
                                        "short (GAP-14)."])],
        msgs=[("Success Header", "Passed", "\"Đạt bài kiểm tra cuối\"", "Persistent"),
              ("Info Header", "Not passed", "\"Chưa đạt lần này\"", "Persistent"),
              ("Info Text", "Not passed", "\"Đáp án được ẩn khi chưa đạt. Ôn lại các bài trong topic rồi làm lại; lần sau bạn sẽ "
               "nhận mã đề khác.\"", "Persistent")],
        rules=[("BR-24", "Answers and transcripts only at ≥ 70%", "\"Đáp án\" lines only when passed"),
               ("BR-22", "Test code consumed even on failure; the next code rotates", "MSG-03"),
               ("BR-15", "Topic and course pass mark 70%", "\"cần từ 70%\" in the score line"),
               ("BR-37", "A course passes permanently at 70%; topics never relock", "Course outcome text"),
               ("NFR-P05", "A result reaches Learning Service within 5 s (95%)", "Re-read /topics long enough to see the "
                "next topic (GAP-14)"),
               ("BR-31", "Examiner grading consumes one credit of an ACTIVE subscription [Draft]",
                "402 → \"Bạn đã hết lượt chấm giáo viên.\"; nothing is consumed")],
    ),
    # ------------------------------------------------------------------ Mock Tests (not built)
    screen(
        key="MockTests", name="Mock Tests", route="/tests (proposed)", roles="CUSTOMER",
        ft="FT-34", uc="UC: Take mock test", status="Draft",
        draft="Draft: no mock test content exists and learners cannot list mock tests in the current release; an "
              "attempt of type MOCK can already be started by package version id. The placement test is built "
              "separately (Placement Test). The frontend's Luyện tập 4 kỹ năng area is a separate prototype "
              "(Practice Test Catalog).",
        purpose="Lists the published mock tests so the learner can practise under exam conditions and get an "
                "estimated band per skill.",
        nav_from=[("Main menu", "User clicks \"Tests\"")],
        nav_to=[("Test Attempt", "User starts a test"), ("Test Result", "User opens a past attempt")],
        pre=[AUTH, LEARNER],
        entry=[("Main menu", "Any learner screen", "—")],
        exits=[("User clicks Start", "Test available", "Test Attempt", "Confirm modal MSG-02"),
               ("User clicks a past attempt", "Any", "Test Result", "—"), ex_menu(), EX_SESSION],
        comps=[("CRD", "Test card", "Card list", "—", "Title, skills covered, estimated duration [TBC]"),
               ("BTN", "Start", "Button (Primary)", "—", "Triggers API 2"),
               ("TBL", "Past attempts", "Table", "Newest first", "Date, overall band (mock), status")],
        apis=[("Page load (mount)", "as.mockList", "Query: `type`", "Render cards · loading: skeleton", "Error → GLB-04"),
              ("User confirms Start", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}`",
               "Navigate Test Attempt", "422 PACKAGE_NOT_ATTEMPTABLE / 404 → MSG-03; 503 → GLB-05")],
        interactions=[("Grading", ["Objective sections are graded at submit; Writing and Speaking of a mock test are "
                                   "graded by an Examiner (SRS v0.9.20) and the bands use the official IELTS Academic "
                                   "tables (BR-33)."])],
        msgs=[("Empty State", "No published test", "Title: \"No tests yet\" · Description: \"Mock tests will appear here "
               "when they are published.\"", "—"),
              ("Confirm Modal", "User clicks Start", "Title: \"Start {test}?\" · Body: \"Find a quiet place. Your answers "
               "are saved as you go.\"", "Buttons: \"Start\" + \"Cancel\""),
              ("Error Toast", "Start fails", "\"This test cannot be started right now. Please try again later.\"",
               "Auto-dismiss 5s")],
        rules=[("BR-14", "Mock tests contain EXAM questions only", "Authoring rule; no UI effect"),
               ("BR-33", "Mock bands from the official tables; overall = mean rounded to 0.5", "Shown on Test Result")],
    ),
]
