# Phase 15: Màn "Việc cần làm của R&D", "Đánh giá giám khảo", "Đánh giá AI chấm Writing"

Người làm: **Codex**. Nhánh: `feat/content-review-screens` (frontend, worktree `../frontend-claude`, tạo nhánh từ `origin/main` mới nhất).
Trạng thái: Done về code (2026-10-23): A, B, C — chờ backend merge và checklist thật

Repo: `frontend`. Phụ thuộc: phase 14 đã merge và contract cập nhật có trên `main`. Thứ tự trong phase: A → B → C (C làm sau cùng, xem lý do ở mục C). Có thể mở một PR cho mỗi phần.

## Mục tiêu

Ba việc board giao cho R&D (`CONTENT_AUTHOR`) dùng được trên giao diện: xử lý việc hệ thống tự phát hiện (KP thiếu bài luyện, câu quá dễ/khó, quan hệ tiên quyết đáng ngờ), chấm lại mẫu để đo độ chuẩn của giám khảo, và kiểm định AI chấm Writing trước khi cho chấm học viên.

## Đọc trước

- Contract: `learning-staff-area-v1.md` (work items), `admin-area-v1.md` (Examiner quality review), `assessment-grading-v1.md` (AI rubric evaluation and activation), bản đã cập nhật ở phase 14.
- Quy tắc frontend ở `plan.md` (không thêm dependency, `apiRequest`, `useAdminResource`, `AdminPage`, `AdminState`, `adminErrorCode`, `adm-*`, `lucide-react`, Zod, form controlled, mobile-first).
- Khối "Việc cần làm của R&D" hiện có trong `src/features/admin/overview/pages/OverviewPage.tsx` và `overview/lib/overviewLabels.ts` (`WORK_KIND_LABEL`, `WORK_STATUS_LABEL`): dùng lại nhãn, chuyển vào chỗ dùng chung nếu cần.

## Menu và route

Thêm vào `ADMIN_MENU` với `roles: ['CONTENT_AUTHOR']` và nhánh `isAuthorArea` của `AdminRoutes.tsx`. Tên đề xuất:

| Menu | Path | Thư mục | Icon gợi ý |
|---|---|---|---|
| "Việc cần làm" | `/content-author/work-items` | `features/admin/work-queue/` | `ListTodo` |
| "Đánh giá giám khảo" | `/content-author/examiner-review` (`:jobId` màn chấm lại) | `features/admin/examiner-review/` | `UserCheck` |
| "Đánh giá AI chấm Writing" | `/content-author/ai-evaluation` (`runs/:runId` chi tiết) | `features/admin/ai-evaluation/` | `BadgeCheck` |

`EXAMINER` và `ADMIN` không thấy ba menu này.

## A. Việc cần làm của R&D

Hệ thống tự tạo việc mỗi 3 giờ và mỗi khi mở danh sách. Màn này để R&D xem hết, nhận và đóng việc.

- Bộ lọc: loại, trạng thái (mặc định "Đang mở" + "Đang làm"; chọn được "Đã xong").
- Mỗi việc một dòng: loại (nhãn tiếng Việt, kèm một câu giải thích khi bấm "?"), **đối tượng** lấy từ `subject` (tên KP; đoạn đầu câu hỏi; "KP A → KP B"), số liệu từ `detail` hiển thị dạng chữ (ví dụ "Đúng 97% trên 120 lượt", "Hết bài ôn 5 lần"), trạng thái, thời gian tạo. `subject` null thì hiện "Không tải được tên" kèm mã rút gọn.

  | Loại | Nhãn | Giải thích ngắn |
  |---|---|---|
  | `CONTENT_GAP` | KP thiếu bài luyện | Học viên làm hết bộ luyện của KP này mà chưa đạt; bài ôn bị bỏ qua |
  | `QUESTION_TOO_EASY` | Câu quá dễ | Tỉ lệ đúng cao hơn mức của nhãn độ khó |
  | `QUESTION_TOO_HARD` | Câu quá khó | Tỉ lệ đúng thấp hơn mức của nhãn độ khó |
  | `EDGE_SUSPECT` | Quan hệ tiên quyết đáng ngờ | Dò ngược ít khi tìm ra lỗi ở KP tiên quyết này |

- Thao tác (gọi `PATCH /api/learning/admin/work-items/{id}`): "Nhận" → `IN_PROGRESS`; "Trả lại" → `OPEN`; "Xong" mở form ngắn "Đã làm gì" (`action`), "Kết quả" (`result`), "Ghi chú" (`note`) rồi gửi `DONE`. Không có tùy chọn "hiện cho học viên" (việc của R&D không gắn học viên). Nút khóa khi đang gửi; xong thì tải lại danh sách.
- Nút "Xử lý" đi thẳng tới chỗ sửa:
  - `CONTENT_GAP` → `/content-author/packages?create=PRACTICE_SET` (màn phase 02). `PackagesPage` hiện chưa đọc query này: thêm việc đọc `create` để mở sẵn form "Gói mới" với loại đã chọn (sửa tối thiểu). Nếu màn "Sinh câu hỏi bằng AI" (phase 13) đã có thì thêm nút "Sinh câu cho KP này" → `/content-author/ai-drafts?new=questions&knowledgePointId=…` (màn phase 13 đọc hai query này để mở form và điền sẵn KP).
  - `QUESTION_TOO_EASY` / `QUESTION_TOO_HARD` → `/content-author/questions/:questionId` (route đã có, `QuestionDetailPage` đọc `questionId` từ URL).
  - `EDGE_SUSPECT` → `/content-author/knowledge-graph?kp=<from>`. `KnowledgeGraphPage` hiện **không đọc query nào** (đã kiểm): thêm việc đọc `kp` để chọn sẵn nút đó và mở panel chi tiết (sửa tối thiểu, không đổi phần khác).
- Trang Tổng quan: khối "Việc cần làm của R&D" thêm link "Xem tất cả" sang màn này và hiện tên đối tượng thay cho JSON thô.

## B. Đánh giá giám khảo

Hệ thống tự bốc ngẫu nhiên khoảng 10% bài Writing giám khảo đã chấm. R&D chấm lại **mù** (không thấy giám khảo nào chấm, chấm mấy điểm); hệ thống so điểm để biết giám khảo nào lệch.

Ba tab:

- **"Bài chờ chấm lại"** (`GET /api/assessments/qa-reviews/queue`, phân trang): Task, nguồn bài (`attemptType`), thời gian chờ, badge "Quá 48 giờ" khi `overdue`. Nút "Nhận" → claim rồi mở màn chấm.
- **"Bài tôi đang chấm"** (`GET /api/assessments/qa-reviews/mine`): bài đã nhận chưa nộp, nút "Chấm tiếp".
- **"Chất lượng giám khảo"** (`GET /api/assessments/admin/examiner-quality`): bảng tên giám khảo (`examinerName`, null thì mã rút gọn), số bài đã chấm lại, lệch trung bình (band), % bài lệch hơn 1 band. Sắp theo lệch trung bình giảm dần. Ghi chú: "Ít bài thì số liệu chưa đáng tin." Không tự đặt ngưỡng tô đỏ (contract không có).

**Màn chấm lại** (`/content-author/examiner-review/:jobId`): đề và bài viết (`GET …/{jobId}/essay`), rubric đi kèm (hiện để tham khảo), form chấm bốn tiêu chí `taskResponse`, `coherence`, `lexical`, `grammar` (chọn 0–9 bước 0.5), band tổng (điền sẵn trung bình bốn tiêu chí làm tròn 0.5, sửa được), ghi chú ≤ 4000 ký tự. Gửi `POST /api/assessments/qa-reviews` → quay về tab "Bài tôi đang chấm". Ghi chú cố định: "Đây là chấm mù. Chấm theo rubric, không cần đoán điểm giám khảo." Lỗi server (ví dụ người chấm lại chính là giám khảo gốc) hiện bằng `adminErrorMessage`.

## C. Đánh giá AI chấm Writing (làm sau cùng)

Mục đích: trước khi tin AI chấm học viên, cho AI chấm lại một bộ bài mà giám khảo người đã chấm, so độ khớp; chỉ bản nào đạt mới được kích hoạt. Hiện **chưa có bộ bài chuẩn do chuyên gia chấm**, nên phần này làm sau A và B; màn vẫn phải dùng được ngay khi có dữ liệu.

- **Đầu trang: trạng thái từng Task** (`GET /api/assessments/admin/ai-prompt-activations`):
  - `promptVersion` null → cảnh báo vàng: "Task 1 chưa được kiểm định. AI đang chấm học viên bằng rubric hiện tại, chưa đối chiếu với giám khảo."
  - Có giá trị → "Task 1 đang dùng bản `…`, kích hoạt ngày …, theo lượt đánh giá …" (link sang lượt đó).
- **Tab "Bộ bài chuẩn"** (`GET …/ai-gold-essays`): lọc Task, số bài mỗi Task, bảng band giám khảo, nguồn, đoạn đầu bài, ngày thêm. Nút "Thêm bài chuẩn" mở form: Task, đề, số liệu biểu đồ (chỉ hiện và bắt buộc với Task 1), bài viết, band giám khảo (0–9 bước 0.5), điểm tiêu chí và mã lỗi (tùy chọn). Nút "Nhập từ các bài AI và giám khảo đã cùng chấm" (`POST …/import-comparisons`) → báo "Đã nhập N bài".
- **Tab "Các lượt đánh giá"** (`GET …/ai-evaluations`): nút "Chạy đánh giá" chọn Task → `202`; lượt đang chạy thì tự tải lại mỗi 5 giây tới khi xong (dọn interval khi rời trang). Bảng: Task, thời gian, số bài, % lệch ≤ 0.5 band, QWK, đạt/không.
- **Chi tiết lượt** (`GET …/ai-evaluations/{id}`):
  - chỉ số chính so với ngưỡng (`thresholds` từ phase 14): "% bài lệch không quá 0.5 band: 76% (cần ≥ 80%)", "Độ đồng thuận QWK: 0.64 (cần ≥ 0.60)", lệch trung bình, tỉ lệ gắn sai mã lỗi;
  - bảng theo tiêu chí (TA/TR, CC, LR, GRA) để thấy AI lệch ở đâu;
  - bảng từng bài: band giám khảo, band AI, chênh lệch; sắp chênh lệch giảm dần;
  - nút "Kích hoạt bản này" chỉ bật khi lượt đã xong và đạt; hộp xác nhận: "Từ lúc kích hoạt, mọi bài Writing của học viên (cả thi topic lẫn bài học) được chấm bằng bản này. Sửa rubric sau đó sẽ không tự áp dụng cho tới khi một lượt mới đạt và được kích hoạt." `409 EVALUATION_NOT_PASSED` → "Lượt này chưa đạt nên không kích hoạt được."
  - Lượt không đạt: gợi ý "Xem tiêu chí lệch nhiều nhất, sửa mô tả band ở màn Rubric chấm Writing rồi chạy lại."

## Test (Vitest + Testing Library, mock lớp `api`)

- Menu: `CONTENT_AUTHOR` thấy ba mục; `ADMIN`, `EXAMINER` không thấy.
- A: lọc gửi đúng query; "Nhận" gửi `IN_PROGRESS`; "Xong" gửi `DONE` kèm `action`/`result`/`note`; `subject` null hiện chữ thay thế; nút "Xử lý" của từng loại trỏ đúng đường dẫn.
- B: claim xong điều hướng sang màn chấm; band tổng điền sẵn đúng làm tròn 0.5; gửi đúng body theo contract; bài `overdue` có badge; bảng chất lượng sắp đúng.
- C: Task chưa kích hoạt hiện cảnh báo; nút kích hoạt khóa khi `passed` false; form bài chuẩn bắt buộc số liệu biểu đồ với Task 1; lượt `RUNNING` tự tải lại và dừng khi xong.

## Kiểm thủ công

Đăng nhập `content@gmail.com` với backend thật: mở "Việc cần làm", nhận và đóng một việc, bấm "Xử lý" một việc `CONTENT_GAP`. Ở "Đánh giá giám khảo", nhận một bài mẫu (nếu hàng chờ rỗng thì ghi rõ), chấm và nộp, thấy bảng chất lượng cập nhật. Ở "Đánh giá AI", thêm hai bài chuẩn bằng tay, chạy một lượt (LLM giả hoặc thật, ghi rõ), xem chi tiết; không kích hoạt trên môi trường dùng chung nếu không được phép. Đo 390 px không cuộn ngang trang.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh; checklist thủ công đã chạy hoặc ghi rõ bước nào không chạy được.

## Kết quả

- 2026-10-23: đã fetch backend origin/main (`c600b3f3`); phase 14 còn Pending. Chưa có xác nhận merge thay đổi API/contract `admin-area-v1`, `assessment-grading-v1`, `learning-staff-area-v1` mà phase này yêu cầu.
- Chưa tạo nhánh `feat/content-review-screens`, chưa viết hoặc đoán API/schema/UI, chưa chạy test/checklist. Chờ phase 14 hoàn tất và contract cập nhật trên main.

### Cập nhật 2026-10-23 — A, B, C đã triển khai

- Lượt 2026-10-23 (Claude Code tiếp quản vì Codex hết token), nhánh `feat/content-author-screens`, đã push vào [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29). A `work-queue/` (lọc loại/trạng thái, tên đối tượng và số liệu thành chữ, Nhận/Trả lại/Xong, nút Xử lý theo loại; `KnowledgeGraphPage` đọc `?kp=`; Tổng quan hiện tên đối tượng và link "Xem tất cả"). B `examiner-review/` (ba tab, màn chấm lại mù, band tổng tự điền làm tròn 0.5, bảng chất lượng sắp theo lệch). C `ai-evaluation/` (Codex đã viết trang, lượt này thêm test và hiển thị điểm hai chữ số).
- Quyền: các route đánh giá giám khảo và đánh giá AI ở backend là **CONTENT_AUTHOR**, không phải ADMIN (đã đối chiếu `@PreAuthorize`); menu theo đó.
- Nút "Sinh câu cho KP này" ở `CONTENT_GAP` chỉ hiện khi `QUESTION_DRAFTS_ENABLED` bật.
- Test: `WorkQueuePage.test.tsx` (9), `ExaminerReviewPages.test.tsx` (11), `AiEvaluationPages.test.tsx` (11), menu, tổng quan, đồ thị tri thức.
- typecheck, lint, build qua; `npm test` 1047/1049 qua, lỗi còn lại là flashcard `owl-reactions.test.ts` (nội dung chọn ngẫu nhiên, ngoài phạm vi, không sửa). Chưa chạy checklist thủ công với backend thật.
