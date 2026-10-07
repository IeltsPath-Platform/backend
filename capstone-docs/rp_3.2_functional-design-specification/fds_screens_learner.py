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
REVIEW_GATE_RULE = ("BR-17", "A pending review blocks lessons, practice and the final test of its own skill",
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
        nav_to=[("Original route, else Learning Path (/learn)", "Sign-in succeeds"),
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
        nav_to=[("Learning Path (/learn)", "Registration and the automatic sign-in succeed"),
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
        nav_to=[("Learning Path (/learn)", "Tokens received and user.me succeeds"), ("Login", "User clicks \"Quay lại đăng "
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
        frontend="Built on mock data — features/overview/pages/OverviewPage.tsx with mocks/overviewData.ts; no API "
                 "call.",
        purpose="Gives the learner a dashboard of study time, four-skill progress, words to review, mentor feedback, "
                "courses and the study streak.",
        nav_from=[("Browser", "Signed-in user opens \"/\""), ("Top bar", "User clicks \"Tổng quát\"")],
        nav_to=[("Selected route", "User follows a card link or the top bar")],
        pre=[REQUIRE_AUTH],
        entry=[("Default route", "Browser", "—"), ("Top bar", "Any page", "—")],
        exits=[EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Hero banner", "Banner", "—", "\"Xin chào, {username} 👋\" · \"Band hiện tại: {band}\""),
               ("PNL", "Sidebar", "Tab list", "—", "Overview sections"),
               ("CRD", "Dữ Liệu Học Tổng Quan", "Stat cards", "—", "Tổng thời gian học, Số ngày học, Số đề đã luyện, Tỷ lệ "
                "chính xác, Streak học"),
               ("PNL", "Biểu đồ thời gian học", "Chart", "—", "Thực tế vs Kế hoạch per week"),
               ("PNL", "Tiến Độ 4 Kỹ Năng", "Panel", "—", "\"AI đánh giá so với kế hoạch lộ trình\""),
               ("PNL", "Từ Vựng Cần Ôn", "Panel", "—", "Đã học / Chưa học; Từ mới, Ôn lần 1–3, Đã thuộc"),
               ("PNL", "Gợi Ý Ôn Luyện Từ AI", "Panel", "—", "\"Bắt đầu ngay\""),
               ("PNL", "Phản Hồi Từ Mentor · Các Khoá Học Của Tôi", "Panels", "—", "—")],
        apis=[("Page load", None, "mocks/overviewData.ts", "Render cards", "—")],
        interactions=[("Data source", ["All numbers are mock data. When wired, study time and streak come from user-service "
                                       "(activity.list, streak.get) and skill progress from learn.mastery (OQ-17)."])],
        msgs=[("Empty State", "Design target: no activity", "\"Chưa có dữ liệu học tập.\"", "—")],
        rules=[("BR-27", "Personal data visible only to its owner", "Design target when the APIs are wired")],
    ),
    # ------------------------------------------------------------------ Learning Path
    screen(
        key="LearningPath", name="Learning Path", route="/learn · RequireAuth", roles="Signed-in learner",
        ft="FT-13, FT-20, FT-28", uc="UC: View learning path", fe="api",
        status="Specified (skill grouping: GAP-01; Premium lock: Draft)",
        frontend=FE_LEARN.format(page="TopicListPage.tsx", route="/learn"),
        shots=[("LearningPath.png", "Learning Path (/learn) — stations of the trail")],
        purpose="Shows the learner's topics as an ordered trail with each topic's status, so the learner knows which "
                "topic to study next.",
        nav_from=[("Login / Register", "Sign-in without a stored route"), ("Top bar", "User clicks \"Lộ trình\""),
                  ("Topic Detail, Test Result, Review Session", "User goes back to the trail")],
        nav_to=[("Topic Detail", "User clicks a PASSED or IN_PROGRESS station"),
                ("Review Session", "User clicks \"Làm bài ôn\" in the review banner")],
        pre=[REQUIRE_AUTH],
        entry=[("Sign-in", "Login", "—"), ("Top bar", "Any page", "—"), ("Lock redirect", "Topic/Lesson pages",
               "Router state `notice` (GLB-09)")],
        exits=[("User clicks a station", "Not LOCKED", "Topic Detail", "—"),
               ("LOCKED station", "—", "Not clickable", "Lock reason shown under the card"), EX_TOPBAR, EX_SESSION],
        comps=[("PNL", "Hero", "Header", "—", "\"Lộ trình Reading\" · \"Đi từng chặng, mở khóa từng kỹ năng\" · \"{passed}/{n} "
                "chặng đã qua\""),
               ("TBL", "Trail", "Ordered list", "Sorted by sequenceOrder (all skills in one list, GAP-01)", "One station per "
                "topic: \"Chặng {n}\", code, title, \"{done}/{total} bài đã xong\""),
               ("BDG", "Topic status", "Badge", "PASSED \"Đã qua\" (green), IN_PROGRESS \"Đang học\" (blue), LOCKED \"Chưa mở\" "
                "(grey)", "—"),
               ("BDG", "Premium", "Badge", "Shown when the lock reason mentions Premium", "—"),
               ("PNL", "Notice banner", "Banner", "—", "Shows a redirect notice (GLB-09)"),
               ("PNL", "Review banner", "Alert banner", "—", "GLB-10 when pending reviews are known")],
        apis=[("Page load", "learn.topics", "—", "Render the trail · loading: \"Đang tải lộ trình…\"", "GLB-04 / GLB-05 panel "
               "with \"Thử lại\"")],
        interactions=[("Station state", ["IF status = LOCKED THEN render a disabled card with the lock reason",
                                         "ELSE render a link to /learn/topics/{id}", "END IF"]),
                      ("Review banner source", ["In HTTP mode the banner shows reviews already known from Topic Detail, "
                                                "Lesson or Practice responses; /learn itself does not call learn.reviews."])],
        msgs=[("Empty State", "No topic", "\"Chưa có topic nào được giao cho bạn.\"", "—"),
              ("Loading", "Page load", "\"Đang tải lộ trình…\"", "—")],
        rules=[("BR-16", "One IN_PROGRESS topic per skill; later ones LOCKED", "Badges; LOCKED cards disabled. Not grouped "
                "by skill yet (GAP-01)"),
               ("BR-34", "Premium content needs an entitlement", "Only a Premium badge (OQ-04)")],
    ),
    # ------------------------------------------------------------------ Topic Detail
    screen(
        key="TopicDetail", name="Topic Detail", route="/learn/topics/:topicId · RequireAuth", roles="Signed-in learner",
        ft="FT-20, FT-21, FT-27, FT-29", uc="UC: View learning path; Take test attempt", fe="api", status="Specified",
        frontend=FE_LEARN.format(page="TopicDetailPage.tsx", route="/learn/topics/:topicId"),
        shots=[("TopicDetail.png", "Topic Detail — lesson list with lesson status")],
        purpose="Lists the ordered lessons of a topic and the final test card, so the learner can open the next lesson, "
                "go to practice or start the final test.",
        nav_from=[("Learning Path", "User clicks a station"), ("Lesson Player, Practice Set, Test Result", "User goes back "
                  "to the topic")],
        nav_to=[("Lesson Player", "User clicks an AVAILABLE or COMPLETED lesson"),
                ("Practice Set", "User clicks \"Mở luyện thêm\" (lesson COMPLETED, practice REQUIRED)"),
                ("Review Session", "User follows an unlock hint for a pending review"),
                ("Test Attempt", "User clicks \"Làm bài kiểm tra\" and the attempt is created")],
        pre=[REQUIRE_AUTH, ("Topic exists and is not LOCKED", "Continue", "TOPIC_LOCKED → /learn with notice (GLB-09); 404 → "
             "GLB-03")],
        entry=[("Station click", "Learning Path", "`topicId`"), ("Back link", "Lesson Player", "`topicId`")],
        exits=[("User clicks a lesson", "Not LOCKED", "Lesson Player", "—"),
               ("User clicks Làm bài kiểm tra", "testStatus AVAILABLE; APIs 4–5 succeed", "Test Attempt",
                "`/learn/tests/{attemptId}?topic={topicId}`"),
               ("Final test refused", "403 / 409", "Stay", "MSG-02…MSG-05"),
               ("User clicks Lộ trình", "Any", "Learning Path", "—"), EX_SESSION],
        comps=[("LNK", "Lộ trình", "Back link", "—", "To /learn"),
               ("PNL", "Topic header", "Header", "—", "\"Chặng {n} · {code}\", title, status badge, \"{done}/{total} bài đã "
                "xong\" + progress bar"),
               ("TBL", "Lessons", "Ordered list", "Sorted by sortOrder", "\"Bài {n}: {title}\", \"{min} phút\", lock reason"),
               ("BDG", "Lesson status", "Badge", "COMPLETED \"Đã xong\", AVAILABLE \"Đang mở\", LOCKED \"Đang khóa\"", "—"),
               ("LNK", "Mở luyện thêm", "Link", "Visible when COMPLETED and practice REQUIRED", "Note \"Cần luyện thêm trước "
                "khi mở đề cuối\""),
               ("CRD", "Bài kiểm tra cuối", "Card", "—", "\"{n} câu · cần đạt từ 70% · không giới hạn thời gian\" (+ \"lần gần "
                "nhất {p}%\")"),
               ("BDG", "Test status", "Badge", "PASSED \"Đã đạt\", AVAILABLE \"Sẵn sàng\", LOCKED \"Đang khóa\", NONE \"Không có "
                "đề\"", "—"),
               ("PNL", "Unlock hints", "List of links", "Visible when testStatus = LOCKED", "\"Chưa mở đề — cần hoàn tất:\""),
               ("BTN", "Làm bài kiểm tra", "Button (Primary)", "Visible only when AVAILABLE", "\"Đang giao đề…\" while pending")],
        apis=[("Page load", "learn.topicLessons", "Path: `topicId`", "Render lessons and test card · loading: \"Đang tải "
               "topic…\"", "GLB-03/04/09/10"),
              ("Page load (parallel)", "learn.topics", "— (cached)", "Topic title, code, description", "Fallback title "
               "\"Topic\""),
              ("Page load (parallel)", "learn.reviews", "Query: `status=PENDING&limit=50`", "Sync the review banner and hints",
               "Error → empty list"),
              ("User clicks Làm bài kiểm tra", "learn.assignTest", "Path: `topicId`", "Then API 5", "MSG-02…MSG-05"),
              ("Assignment received", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}`",
               "Navigate Test Attempt", "Server message + \" Hãy thử lại.\"")],
        interactions=[("Unlock hints (testStatus = LOCKED)", [
            "IF lessons not completed → \"Hoàn thành {n} bài học còn lại (vd. Bài {k}: {title}).\" → first such lesson",
            "IF lessons need practice → \"Luyện thêm {n} bài đã học (practice REQUIRED) — vào trang luyện thêm của bài.\"",
            "IF pending reviews → \"Hoàn thành {n} bài ôn bắt buộc (review) trước khi mở đề.\" → first review",
            "IF none of these → \"Hoàn thành mọi bài học, luyện thêm (nếu có) và bài ôn còn treo để mở đề cuối.\""]),
            ("Final test start", ["An in-flight guard stops a second click. learn.assignTest returns the open assignment "
                                  "when one exists, so a retry reuses the same test version (FT-29/AC-01)."])],
        msgs=[("Empty State", "Topic has no lesson", "\"Topic này chưa có bài học.\"", "—"),
              ("Inline Error", "409 TEST_UNAVAILABLE / NO_TOPIC_TEST", "\"Chưa có đề cho topic này. Hãy quay lại sau.\"",
               "On the test card"),
              ("Inline Error", "409 PRACTICE_REQUIRED / PRACTICE_LOCKED", "\"Cần luyện thêm trước khi mở đề: Bài {n}, … Mở bài "
               "học tương ứng và hoàn thành phần luyện thêm.\"", "On the card; page reloads"),
              ("Inline Error", "403 REVIEW_REQUIRED", "\"Cần làm bài ôn “{KP}” trước khi làm bài kiểm tra.\"", "On the card; "
               "page reloads"),
              ("Inline Error", "403 TEST_LOCKED", "\"Bài kiểm tra chưa mở. Hoàn thành bài học, luyện thêm và bài ôn (nếu có).\"",
               "On the card; page reloads"),
              ("Info Text", "testStatus NONE", "\"Topic này không có đề cuối (NO_TOPIC_TEST).\"", "On the card")],
        rules=[("BR-17", "Lessons open in order", "LOCKED rows are not links; lock reason shown"), REVIEW_GATE_RULE,
               ("BR-22", "Final test needs every lesson completed and practice-cleared; codes rotate",
                "Button only when AVAILABLE; unlock hints; MSG-02…MSG-05")],
    ),
    # ------------------------------------------------------------------ Lesson Player
    screen(
        key="LessonPlayer", name="Lesson Player", route="/learn/lessons/:lessonId · RequireAuth", roles="Signed-in learner",
        ft="FT-21, FT-22, FT-23, FT-24, FT-25", uc="UC: Study lesson; Submit Writing task; Grade essay", fe="api", status="Specified",
        frontend=FE_LEARN.format(page="LessonPage.tsx with components/blocks (BlockList, TextBlock, PassageBlock, "
                                      "AudioBlock, ExerciseBlock, EssayBlock)", route="/learn/lessons/:lessonId"),
        shots=[("LessonPlayer-1.png", "Lesson Player — passage and an exercise block after a wrong answer, with the hint"),
               ("LessonPlayer-2.png", "Lesson Player — a passed block shows the answer and explanation")],
        purpose="Presents a lesson's blocks in order — text, passage, audio, exercises and essays — grades each exercise "
                "block, so the learner can pass the lesson and move on.",
        nav_from=[("Topic Detail", "User clicks a lesson"), ("Lesson Player", "User clicks \"Bài tiếp theo\""),
                  ("Review Session", "User clicks \"Học tiếp\" after the review")],
        nav_to=[("Practice Set", "Lesson completed; user clicks \"Luyện thêm\""),
                ("Review Session", "A review was created; user clicks \"Làm bài ôn\""),
                ("Lesson Player (next)", "User clicks \"Bài tiếp theo\""), ("Topic Detail", "User clicks the topic link")],
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
               ("BTN", "Hoàn thành bài", "Button (Primary)", "Only for a lesson without exercise blocks", "—"),
               ("PNL", "Completion panel", "Panel (role=status)", "—", "Buttons \"Luyện thêm\" or \"Làm bài ôn\", \"Bài tiếp "
                "theo\" or \"Về topic / đề cuối\", \"Về topic\"")],
        apis=[("Page load", "learn.lesson", "Path: `lessonId`", "Render blocks · loading: \"Đang tải bài học…\"",
               "GLB-03/04/09/10"),
              ("Page load (parallel)", "learn.topics", "— (cached)", "Topic title for the back link", "—"),
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
        ],
        msgs=[("Inline Result", "Block passed", "\"Đạt {c}/{t} câu ({p}%). Đáp án và giải thích hiện dưới từng câu.\"",
               "Persistent"),
              ("Inline Result", "Block not passed", "\"Đúng {c}/{t} câu ({p}%). Cần từ 70% để đạt. Sửa các câu sai rồi nộp "
               "lại.\"", "Persistent"),
              ("Info Text", "Lesson without exercises", "\"Bài này không có bài tập. Đọc xong thì đánh dấu hoàn thành.\"",
               "Above the button"),
              ("Completion Panel", "Lesson completed", "\"Bạn đã hoàn thành bài này\" · \"Tiếp theo: làm luyện thêm (practice) nếu "
               "còn REQUIRED — đề cuối chỉ mở khi mọi bài đã practice PASSED và không còn review.\"", "Persistent"),
              ("Completion Panel", "Review created", "\"Trước khi sang bài kế, cần ôn lại \"{KP}\". Bài ôn gồm phần lý thuyết "
               "và một bộ câu hỏi ngắn.\"", "Button \"Làm bài ôn\""),
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
                "Client timeout 60 s (Section 2.0) covers it; MSG-07")],
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
        status="Specified (Premium sets disabled: GAP-05)",
        frontend=FE_LEARN.format(page="PracticePage.tsx (list and attempt on the same route)",
                                 route="/learn/lessons/:lessonId/practice"),
        purpose="Lists the practice sets of a completed lesson and lets the learner answer one set, see every solution and "
                "start the review it creates.",
        nav_from=[("Lesson Player", "User clicks \"Luyện thêm\""), ("Topic Detail", "User clicks \"Mở luyện thêm\" or an "
                  "unlock hint")],
        nav_to=[("Review Session", "A review was created; user clicks \"Làm bài ôn\""),
                ("Learning Path", "Practice PASSED; user clicks \"Về lộ trình\""), ("Lesson Player", "User clicks \"Về bài học\"")],
        pre=[REQUIRE_AUTH, ("No pending review in the skill", "Continue", "403 REVIEW_REQUIRED → review panel (GLB-10)")],
        entry=[("Luyện thêm", "Lesson Player", "`lessonId`"), ("Mở luyện thêm", "Topic Detail", "`lessonId`")],
        exits=[("User clicks Làm bộ này / Làm lại", "Set not LOCKED or PREMIUM", "Same route, attempt view", "—"),
               ("Submission done", "API 200", "Same route, result view", "MSG-02…MSG-05"),
               ("User clicks Danh sách luyện thêm / Về danh sách", "Any", "Same route, list view", "List reloads"), EX_SESSION],
        comps=[("PNL", "Header", "Header", "—", "\"Luyện thêm\" · \"Củng cố trước khi mở đề cuối\" · \"Trạng thái practice: "
                "{status} · {reason}\""),
               ("CRD", "Practice set card", "Card list", "—", "Code, title, \"{n} câu · best {p}%\", \"Đã lộ đáp án — không tính "
                "evidence.\" when revealed"),
               ("BDG", "Set status", "Badge", "AVAILABLE \"Sẵn sàng\", IN_PROGRESS \"Đang làm\", PASSED \"Đã đạt\", ATTEMPTED "
                "\"Đã thử\", LOCKED \"Khóa\"", "—"),
               ("BTN", "Làm bộ này / Làm lại", "Button (Primary)", "Disabled when LOCKED or PREMIUM", "\"Đang mở…\" while "
                "starting; label \"Premium\" for Premium sets"),
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
        msgs=[("Empty State", "No practice set", "\"Bài này không có bộ luyện thêm (hoặc đã được coi là PASSED).\"", "—"),
              ("Success Panel", "Set passed", "\"Đã đạt bộ luyện thêm ({p}%).\"", "Button \"Tiếp tục luyện / về topic\""),
              ("Inline Result", "Set not passed", "\"Chưa đạt ({p}%). Có thể chọn bộ khác hoặc làm bài ôn nếu được tạo.\"",
               "Button \"Về danh sách\""),
              ("Review Panel", "Review created", "\"Đã tạo bài ôn bắt buộc sau lần luyện này.\"", "Button \"Làm bài ôn\""),
              ("Info Text", "Set already revealed", "\"Bộ này đã lộ đáp án trước đó — lần nộp này không tính evidence.\"",
               "Attempt header"),
              ("Success Panel", "Practice PASSED for the lesson", "\"Practice đã PASSED. Có thể quay lại topic để mở đề cuối (nếu "
               "không còn review).\"", "Button \"Về lộ trình\""),
              ("Info Text", "Premium set", "\"Premium — chưa mở trong giai đoạn này.\"", "On the card")],
        rules=[("BR-15", "Pass mark 70%", "MSG-02 / MSG-03"),
               ("BR-18", "Only the first submission of an unrevealed set records evidence", "MSG-05"),
               ("BR-20", "A revealed set is never reused as a review set", "\"Đã lộ đáp án\" note"),
               ("BR-21", "Reviews open for knowledge points below 70%", "MSG-04"), REVIEW_GATE_RULE,
               ("BR-34", "Premium sets need an entitlement", "Always disabled today (GAP-05)")],
    ),
    # ------------------------------------------------------------------ Review List (not built)
    screen(
        key="ReviewList", name="Review List", route="/learn/reviews (proposed)", roles="CUSTOMER",
        ft="FT-28", uc="UC: Complete review", status="Specified (backend); screen not built (GAP-07)",
        purpose="Lists the learner's reviews, oldest first, so the learner can finish the ones that block a skill.",
        nav_from=[("Learning Path / Topic Detail / Lesson Player", "User clicks the pending-review banner or link"),
                  ("Main menu", "User clicks \"Reviews\"")],
        nav_to=[("Review Session", "User clicks a review")],
        pre=[AUTH, LEARNER],
        entry=[("Pending-review banner", "Learning Path, Topic Detail, Lesson Player", "Query: `?skill=`"),
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
        comps=[("PNL", "Header", "Header", "—", "Stage and \"fail {n}/{max}\""),
               ("PNL", "Lý thuyết cần nhớ", "Rich text", "—", "Theory blocks of the knowledge point"),
               ("PNL", "Quick-check lý thuyết", "Questions", "THEORY stage", "\"Đọc lại lý thuyết rồi trả lời quick-check để mở bộ "
                "luyện ôn.\""),
               ("BTN", "Confirm read", "Button", "Only when there is no quick-check", "\"Không có câu quick-check. Xác nhận đã đọc lý "
                "thuyết để sang PRACTICE.\" (GAP-12)"),
               ("PNL", "Bộ câu hỏi ôn", "Questions + passage / \"Audio ôn\"", "PRACTICE stage", "\"Bộ {k}/{max} · {code}\"; "
                "\"Làm bộ câu hỏi ôn. Đạt để hoàn thành; chưa đạt có thể sang THEORY hoặc bộ khác.\""),
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
                                  "PRACTICE: set < 70% → \"Chưa đạt (failedSets {n}/{max}). Tải bộ tiếp theo.\"",
                                  "Second failed set or no unseen set → SKIPPED (MSG-02)"])],
        msgs=[("Inline Result", "Set below 70%", "\"Chưa đạt (failedSets {n}/{max}). Tải bộ tiếp theo.\"", "Button \"Làm bộ tiếp "
               "theo\""),
              ("Info Panel", "Review SKIPPED", "\"Đã bỏ qua bài ôn\" · \"Bạn chưa đạt sau {max} bộ. Bài ôn được bỏ qua để bạn tiếp "
               "tục học.\"", "Button \"Học tiếp\""),
              ("Success Panel", "Review DONE", "\"Đã hoàn thành bài ôn\" · \"Lộ trình đã mở lại. Bạn có thể học tiếp hoặc quay lại "
               "luyện thêm / đề cuối.\"", "Button \"Học tiếp\"")],
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
        nav_from=[("Learning Path", "User clicks \"View mastery\""), ("Main menu", "User clicks \"Mastery\"")],
        nav_to=[("Topic Detail", "User clicks a topic group header")],
        pre=[AUTH, LEARNER],
        entry=[("View mastery link", "Learning Path", "—"), ("Main menu", "Any learner screen", "Query: `?skill=`")],
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
        key="TestAttempt", name="Test Attempt", route="/learn/tests/:attemptId?topic=… · RequireAuth",
        roles="Signed-in learner", ft="FT-24, FT-30, FT-31", uc="UC: Take test attempt", fe="api",
        status="Specified (text answers saved at submit; no timer: GAP-09)",
        frontend=FE_LEARN.format(page="TopicTestPage.tsx", route="/learn/tests/:attemptId"),
        purpose="Lets the learner sit the topic final test section by section, with answers saved as they go, and submit "
                "it for grading.",
        nav_from=[("Topic Detail", "Final test assigned and attempt created")],
        nav_to=[("Test Result", "Submit succeeds"), ("Topic Detail", "User clicks \"Về topic\"")],
        pre=[REQUIRE_AUTH, ("Attempt belongs to the user", "Continue", "403 → error panel")],
        entry=[("Làm bài kiểm tra", "Topic Detail", "`attemptId`, Query `topic`")],
        exits=[("User clicks Nộp bài (all saved)", "API 200", "Test Result", "`/learn/tests/{id}/result?topic=…`, replace"),
               ("Save failed for some answer", "—", "Stay", "MSG-02"), ("User clicks Về topic", "Any", "Topic Detail",
               "Saved answers stay on the server"), EX_SESSION],
        comps=[("PNL", "Header", "Header", "—", "\"Bài kiểm tra cuối\" · \"Làm từng phần, câu trả lời được lưu ngay\" · \"Không "
                "giới hạn thời gian\""),
               ("TAB", "Các phần của đề", "Section list", "—", "\"Phần {n}\" with \"{done}/{total} câu\""),
               ("PNL", "Section content", "Passage / audio", "—", "No transcript during the test"),
               ("RAD", "Answer", "Radio (choices) / Text Input", "—", "Choice answers save on change"),
               ("BDG", "Save state", "Inline text", "\"Đang lưu…\" / \"Đã lưu\" / \"Chưa lưu được, sẽ thử lại khi nộp\"", "Per item"),
               ("BTN", "Phần trước / Phần tiếp", "Buttons", "—", "\"Phần {k}/{n}\""),
               ("BTN", "Nộp bài / Vẫn nộp", "Button (Primary)", "Second click needed when answers are missing",
                "\"Đang nộp…\" while pending")],
        apis=[("Page load", "as.structure", "Path: `attemptId`", "Render sections · loading: \"Đang tải đề…\"", "GLB-03/04"),
              ("Choice picked; and every answer at submit", "as.save", "`{payload: {answer}, schemaVersion: 1, "
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
               ("FT-30 BV-01", "Save needs the current revision", "Revision kept per item")],
    ),
    # ------------------------------------------------------------------ Test Result
    screen(
        key="TestResult", name="Test Result", route="/learn/tests/:attemptId/result?topic=… · RequireAuth",
        roles="Signed-in learner", ft="FT-14, FT-24, FT-29, FT-31, FT-33", uc="UC: View result; Request AI grading; Request examiner grading", fe="api",
        status="Specified (examiner and AI grading requests: Draft)",
        frontend=FE_LEARN.format(page="TopicTestResultPage.tsx", route="/learn/tests/:attemptId/result"),
        shots=[("TestResult.png", "Test Result — a failed topic test (25%)")],
        purpose="Shows the score of the submitted topic test, per-question correctness and — at 70% or more — the correct "
                "answers, and tells the learner whether the topic is passed.",
        nav_from=[("Test Attempt", "Submit succeeded")],
        nav_to=[("Topic Detail (next topic)", "Passed; user clicks \"Sang chặng tiếp theo\""),
                ("Learning Path", "User clicks \"Xem lộ trình\""), ("Topic Detail", "Failed; user clicks \"Về topic để làm "
                "lại\"")],
        pre=[REQUIRE_AUTH, ("Attempt belongs to the user", "Continue", "403 → error panel")],
        entry=[("Submit", "Test Attempt", "`attemptId`, Query `topic`")],
        exits=[("User clicks a navigation button", "Any", "Topic / Learning Path", "—"), EX_SESSION],
        comps=[("PRG", "Score dial", "Ring", "—", "Percent"),
               ("PNL", "Outcome", "Header", "—", "\"Kết quả · mã đề {code}\" · \"Đạt bài kiểm tra cuối\" / \"Chưa đạt lần này\""),
               ("PNL", "Score line", "Text", "—", "\"{score}/{max} câu đúng · {p}% · cần từ 70%\""),
               ("TBL", "Từng câu", "List", "—", "\"Câu {n}\" ✓/✗; \"Đáp án: {answer}\" only when passed"),
               ("BTN", "Sang chặng tiếp theo / Xem lộ trình / Về topic để làm lại", "Buttons", "—", "By outcome")],
        apis=[("Page load", "as.result", "Path: `attemptId`", "Render score and items · loading: \"Đang chấm bài…\"",
               "404 / 403 → error panel"),
              ("Passed: re-read the trail up to 4 times, 400 ms apart", "learn.topics", "—", "Find PASSED topic and the next "
               "IN_PROGRESS topic", "Error → panel"),
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
            "ELSE → \"Đáp án được ẩn khi chưa đạt. Ôn lại các bài trong topic rồi làm lại; lần sau bạn sẽ nhận mã đề khác.\""]),
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
               ("BR-15", "Topic pass mark 70%", "\"cần từ 70%\" in the score line"),
               ("NFR-P05", "A result reaches Learning Service within 5 s (95%)", "Re-read /topics long enough to see the "
                "next topic (GAP-14)"),
               ("BR-31", "Examiner grading consumes one credit of an ACTIVE subscription [Draft]",
                "402 → \"Bạn đã hết lượt chấm giáo viên.\"; nothing is consumed")],
    ),
    # ------------------------------------------------------------------ Mock & Placement Tests (not built)
    screen(
        key="MockTests", name="Mock & Placement Tests", route="/tests (proposed)", roles="CUSTOMER",
        ft="FT-34, FT-35", uc="UC: Take mock test; Take placement test", status="Draft",
        draft="Draft: no mock or placement content exists and learners cannot list these tests in the current "
              "release; attempts of type MOCK and PLACEMENT can already be started by package version id. The "
              "frontend's Luyện đề area is a separate mock-data prototype (Practice Test Catalog).",
        purpose="Lists the published mock tests and the one-time placement test, so the learner can estimate a "
                "starting level or practise under exam conditions.",
        nav_from=[("Main menu", "User clicks \"Tests\""), ("Learning Goals", "User clicks \"Take the placement test\"")],
        nav_to=[("Test Attempt", "User starts a test"), ("Test Result", "User opens a past attempt")],
        pre=[AUTH, LEARNER],
        entry=[("Main menu", "Any learner screen", "—"), ("Placement prompt", "Learning Goals", "Query: `?type=PLACEMENT`")],
        exits=[("User clicks Start", "Test available", "Test Attempt", "Confirm modal MSG-02"),
               ("User clicks a past attempt", "Any", "Test Result", "—"), ex_menu(), EX_SESSION],
        comps=[("TAB", "Type tabs", "Tab bar", "MOCK, PLACEMENT", "—"),
               ("CRD", "Test card", "Card list", "—", "Title, skills covered, estimated duration [TBC]"),
               ("BTN", "Start", "Button (Primary)", "Placement: disabled after one completed attempt [TBC]", "Triggers API 2"),
               ("TBL", "Past attempts", "Table", "Newest first", "Date, overall band (mock), status")],
        apis=[("Page load (mount)", "as.mockList", "Query: `type`", "Render cards · loading: skeleton", "Error → GLB-04"),
              ("User confirms Start", "as.start", "`{packageVersionId, mode: STANDARD, channel: WEB}`",
               "Navigate Test Attempt", "422 PACKAGE_NOT_ATTEMPTABLE / 404 → MSG-03; 503 → GLB-05")],
        interactions=[("Placement once", ["IF a PLACEMENT attempt is COMPLETED [TBC, OQ-06]",
                                          "THEN   hide Start and show the placement result",
                                          "END IF"])],
        msgs=[("Empty State", "No published test", "Title: \"No tests yet\" · Description: \"Mock tests will appear here "
               "when they are published.\"", "—"),
              ("Confirm Modal", "User clicks Start", "Title: \"Start {test}?\" · Body: \"Find a quiet place. Your answers "
               "are saved as you go.\"", "Buttons: \"Start\" + \"Cancel\""),
              ("Error Toast", "Start fails", "\"This test cannot be started right now. Please try again later.\"",
               "Auto-dismiss 5s")],
        rules=[("BR-14", "Mock/placement tests contain EXAM questions only", "Authoring rule; no UI effect"),
               ("BR-33", "Mock bands 0–9 in 0.5 steps; overall = mean rounded to 0.5", "Shown on Test Result [TBC]"),
               ("FT-35 BV-01", "One placement test per learner [TBC]", "Start hidden after completion")],
    ),
]
