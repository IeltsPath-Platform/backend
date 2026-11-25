---
title: "Khu quản trị: danh sách tự tải thêm khi cuộn, bỏ nút Trang trước/Trang sau"
description: "Thay phân trang bằng nút ở mọi danh sách khu admin bằng cuộn vô hạn: một hook và một thành phần dùng chung, chuyển 8 màn đang phân trang, thêm phân trang phía server cho các danh sách còn tải cả mảng, cập nhật quy ước UI"
status: in_progress
priority: P2
branch: "feat/admin-infinite-scroll (frontend + backend)"
tags: [frontend, admin, content-service, pagination, infinite-scroll, ux]
blockedBy: []
blocks: []
created: "2026-11-24T14:12:00.000+07:00"
createdBy: "Claude Code"
source: "hội thoại 2026-11-24: bộ chọn câu hỏi trong hộp thoại Thêm block có nút Trước/Sau; chủ dự án muốn cuộn gần cuối thì tự lấy thêm, áp dụng cho mọi danh sách trang quản trị"
---

# Cuộn vô hạn cho danh sách khu quản trị

Repo: `frontend` (chính), `backend` (chỉ phase 03, các API còn trả cả mảng). Giữ nguyên hình dạng phản hồi trang đã có (`{ content, page, size, totalElements, totalPages }`), không đổi hợp đồng của 8 API đang phân trang.

## Khảo sát hiện trạng (2026-11-24)

**A. Đã phân trang ở server, đang dùng nút Trước/Sau (8 màn, 11 danh sách):**

| Màn | Danh sách | API |
|---|---|---|
| `QuestionBankPage` | câu hỏi | `GET /api/content/admin/questions` (`page`, `size`) |
| `PackagesPage` | gói đề | `GET /api/content/admin/packages` (vừa phân trang 2026-11-24) |
| `QuestionChooser` (hộp thoại Thêm block Bài tập, cả màn phần thi) | câu hỏi để chọn | cùng API câu hỏi, size 25 |
| `AiDraftsPage` | bản nháp AI | `/admin/ai-drafts` (`page`, `size`) |
| `AiEvaluationPage` | bài mẫu, lượt đánh giá | `/ai-gold-essays`, `/ai-evaluations` (`page`, `size=20`) |
| `ExaminerReviewPage` | hàng chờ, bài của tôi | `/qa/queue`, `/qa/mine` (`page`, `size`) |
| `LoginHistoryModal` | lịch sử đăng nhập | `/api/users/{id}/login-history` (`page`, `size`) |

**B. Phân trang giả ở client (tải cả mảng rồi cắt):** `UsersPage` (`GET /api/users` trả mảng, `slice` 25 dòng).

**C. Tải cả mảng, không phân trang:** hàng việc (`work-items`), lỗi (`error-types`), rubric, khóa học, chủ đề, từ vựng quản trị, v.v. Phần lớn là danh sách nhỏ hoặc dạng cây.

Chưa có hook hay thành phần phân trang dùng chung: mỗi màn tự giữ `page` và tự vẽ thanh nút (`qb-pager`, `adm-package-pager`, `ls-questions__pagination`...). Dữ liệu tải qua `useAdminResource` (một lần theo khóa, hủy yêu cầu cũ).

## Quyết định (đã xác nhận 2026-11-24)

| Chủ đề | Đề xuất | Lý do |
|---|---|---|
| Phạm vi | Cuộn vô hạn cho **danh sách có thể lớn không giới hạn** (A, B, hàng việc, từ vựng quản trị). **Không** áp cho danh sách nhỏ cố định (khóa học, rubric, lỗi, dịch vụ) và cây (khóa/chủ đề, cây tri thức) | Đã chốt; cây không phân trang được, danh sách nhỏ tải một lần là đủ |
| Người dùng | Phân trang **ở server** (page, size, bộ lọc vai trò, trạng thái, tìm kiếm) | Đã chốt; đúng `AGENTS.md` §3.7 |
| Ô tìm của Ngân hàng câu hỏi | **Tìm ở server** bằng bộ lọc `code` có sẵn (chỉ theo mã, không theo nội dung đề) | Đã chốt; lọc trên dòng đã tải sẽ gây hiểu nhầm là không có kết quả |
| Cách phân trang phía server | Giữ `page`/`size` (offset), giữ hình dạng phản hồi hiện có | Đã có ở 8 API; cursor cần đổi hợp đồng và chưa cần |
| Trùng/sót khi dữ liệu đổi giữa các lần tải | Gộp theo `id`, bỏ trùng; mọi thao tác ghi xong thì **tải lại từ đầu** | Offset có thể lệch khi thêm/xóa; chấp nhận, đơn giản |
| Khi nào tải thêm | Còn cách đáy vùng cuộn khoảng 1 màn hình (`rootMargin` 400px) | Tải trước khi người dùng chạm đáy |
| Cỡ trang | 25 (bảng), giữ 20 ở màn đang dùng 20 | Không đổi hành vi server |
| Vùng cuộn | Gốc quan sát là **vùng cuộn gần nhất** (thân hộp thoại, ngăn kéo, danh sách có `overflow`, hoặc cửa sổ) | Trong hộp thoại, quan sát theo cửa sổ không bao giờ kích hoạt |
| Cột STT | Vẫn có, đánh số nối tiếp qua các lần tải | Quy ước bảng admin |
| Tổng số | Dòng "Đang hiện n / tổng …" **ở đầu danh sách, dính** (cố định khi cuộn) thay cho "Trang n / tổng" | Đã chốt; luôn biết còn bao nhiêu |
| Dòng đầu bảng | Đầu bảng dính trên cùng (`position: sticky`) trong vùng cuộn | Bỏ phân trang thì cuộn dài, cần giữ nhãn cột |
| Danh sách rất dài | Không ảo hóa (YAGNI); ghi nhận rủi ro, đo khi quá 1000 dòng | DOM nặng chỉ khi người dùng cuộn rất sâu |

## Phases

| Phase | Nội dung | Repo | Trạng thái |
|---|---|---|---|
| [01](phase-01-shared-infinite-list.md) | Hook `useInfiniteList`, thành phần `InfiniteSentinel`, tiện ích test, CSS | frontend | done |
| [02](phase-02-convert-paged-screens.md) | Chuyển 8 màn nhóm A, gỡ thanh nút và CSS cũ | frontend | done |
| [03](phase-03-backend-paging-for-array-lists.md) | Phân trang phía server cho danh sách còn trả mảng (người dùng, hàng việc, từ vựng quản trị, ...) và contract | backend | done |
| [04](phase-04-convert-array-screens.md) | Chuyển `UsersPage` và các màn nhóm C đã có API trang | frontend | done |
| [05](phase-05-conventions-and-docs.md) | Cập nhật `admin-ui.md`, bộ nhớ dự án, dọn CSS | frontend, docs | done |

Thứ tự khuyên: 01, 02 (đã đủ dùng ngay cho bộ chọn câu hỏi và các màn đang phân trang), 03, 04, 05.

## Tiêu chí hoàn thành

- Không còn nút "Trước/Sau/Trang n" ở màn admin nào thuộc phạm vi.
- Cuộn gần đáy thì tự tải thêm; đổi bộ lọc hoặc tìm kiếm thì về đầu danh sách và hủy yêu cầu cũ; tải lỗi thì giữ các dòng đã có và hiện "Thử lại" ngay tại đáy.
- Hết dữ liệu thì dừng, hiện dòng "Đã hiện tất cả n …".
- Bộ chọn câu hỏi trong hộp thoại (và trong ngăn kéo phần thi) cuộn tải thêm được, không cần nút.
- `npm run lint`, `typecheck`, `build`, test admin xanh; backend (phase 03) test và ArchUnit xanh.

## Rủi ro

0. Ô tìm của Ngân hàng câu hỏi hiện lọc client trên một trang: chuyển sang server **thu hẹp** phạm vi tìm về mã; báo rõ cho chủ dự án khi bàn giao.
1. **Gốc quan sát sai** trong hộp thoại, ngăn kéo hoặc danh sách có chiều cao riêng: tải thêm không bao giờ chạy. Hook phải tìm vùng cuộn gần nhất và có test với từng loại vùng.
2. **Vòng lặp tải**: danh sách ngắn hơn vùng nhìn thì cuối danh sách luôn thấy; hook phải tải tiếp cho đến khi đầy vùng hoặc hết dữ liệu, có chặn khi lỗi để không lặp vô hạn.
3. **Trùng dòng do offset** khi dữ liệu đổi giữa chừng (đã nêu ở trên).
4. **Mất vị trí** khi thao tác ghi làm tải lại từ đầu: chấp nhận, hoặc giữ danh sách cũ trong lúc tải lại (`refresh`, không nháy trắng).
5. **Test**: jsdom không có `IntersectionObserver`; cần một bản giả chung để kích hoạt thủ công, nếu không mọi test đổi sang cách khác nhau.
6. Phase 03 đổi hình dạng phản hồi của các API đang trả mảng: mọi nơi gọi (kể cả `mobile/` nếu có) phải cập nhật cùng lúc.

## Câu hỏi còn mở

- Đã chốt 2026-11-24: phạm vi chỉ danh sách lớn; người dùng phân trang ở server; ô tìm của Ngân hàng câu hỏi tìm ở server; bộ đếm dính ở đầu danh sách.
- Còn lại: từ vựng quản trị và hàng việc: xác nhận API hiện trả mảng đầy đủ (chưa kiểm sâu; làm ở bước kiểm kê của phase 03).
- Ô tìm của Ngân hàng câu hỏi chỉ khớp theo **mã** câu hỏi (bộ lọc server sẵn có). Muốn tìm theo nội dung đề thì cần thêm ở backend, ngoài plan này.
