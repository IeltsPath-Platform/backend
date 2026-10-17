# Cập nhật tiến độ các phase

Thời điểm: 2026-10-17 22:15. Nhánh `feat/knowledge-tree-assessment-journey`, HEAD `6599129`.
Báo cáo trước: `workflow-status-261017-2039-owners-and-progress-report.md` (mốc lịch sử).

Ghi chú: Claude = `learning-service` và tài liệu. Codex = content, assessment, access, user, library, Gateway. Git ghi
mọi commit dưới một tài khoản, nên phân công theo service.

## Tổng quan

| Plan | Phase | Completed | In progress | Pending | Blocked |
|---|---|---|---|---|---|
| `261016` Knowledge tree | 15 (17 mục) | 6 | 9 (có mục mở) | 0 | 2 (2b, 11) |
| `261017-1931` Mastery | 9 | 2 (2 Claude, 6 Codex) | 2 (1 Codex, 3 Claude) | 5 | 0 |
| `261017-1932` Gamification | 8 | 1 (1 Codex) | 2 (2, 3 Claude) | 5 | 0 |

Số "9 (có mục mở)" ở plan 261016 gồm các phase đã đóng nhưng còn tiêu chí chưa kiểm chứng. Xem chi tiết bên dưới.

## Plan 261016: Cây tri thức và hành trình đánh giá (in_progress)

| Phase | Service | Người | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|
| 1 | content | Codex | Completed (có mục mở) | ef2d259 | Chưa tick: contract `knowledge-tree-v1.md` khớp controller |
| 2a | content, learning | Codex, Claude | Completed (có mục mở) | ef2d259 | Chưa tick: học viên không thấy course 6.0/7.0; difficulty thiếu → 400 |
| 2b | content | Codex | Blocked | 4979d4b, e445f03, f982c6a | 7 KP Listening chưa đủ 3 bộ (cần audio). Bộ Reading/Writing là bản nháp |
| 3a | content, learning, assessment | Codex, Claude | Completed (có mục mở) | a140b8c | Chưa tick: `SPELLING`, distractors không lộ, mã ngoài danh mục |
| 3b | content | Codex | Completed | 4979491, 80e45d2, 7a822d8 | V35, V36. Mã lỗi vẫn là bản nháp chờ chuyên gia |
| 4 | learning, assessment | Claude, Codex | Completed (có mục mở) | 85ad593 | Chưa tick: bài thi có câu gắn mã → event có `error_code` |
| 5 | learning | Claude | Completed | 31decbe | Đủ tiêu chí |
| 6 | learning | Claude | Completed (có mục mở) | 9dd1005 | Chưa tick: KP 85% đến hạn ôn |
| 7 | learning | Claude | Completed (có mục mở) | 4a0b56e | Chưa tick: báo cáo trả `DIAGNOSING` khi phiên đang chạy |
| 8 | content, assessment, learning | Codex, Claude | Completed | e710f11 | Đủ tiêu chí |
| 9 | learning | Claude | Completed | 4107e44 | Đủ tiêu chí |
| 10 | assessment | Codex | Completed | 80123b5 | Đủ tiêu chí |
| 11 | content, assessment, learning | Codex (công cụ), chuyên gia IELTS (nghiệm thu) | Blocked | f9c6fbd | Thiếu gold set do chuyên gia chấm và lần đánh giá thật. Số liệu tổng hợp không dùng làm bằng chứng |
| 12 | learning, assessment, access | Claude, Codex | Completed | 3ef1939, 1ea5e44, c014d56 | Gateway có test |
| 13 | docs | Chung | Completed (có mục mở) | 14853bf, 3fb9632 | `mvn verify` xanh trừ library-service. Demo Compose chưa đạt trọn: bước 4, 5, 8 cần dựng lại image Learning |
| 14 | learning | Claude | Completed (có mục mở) | 9cf6d74 | Chưa tick: luật bằng chứng không đổi |
| 15 | learning, assessment, access | Claude, Codex | Completed (có mục mở) | c03b9e0, 0a35bc4 | Chưa chạy thử nối thật hai service |

Ghi chú: trong `plan.md` plan này có hai dòng "Claude" và "Codex" nằm ngoài bảng phase, ghi phân công cho plan `261009` và
phase 11. Cần rà lại trước khi dùng làm nguồn chính thức.

## Plan 261017-1931: Mastery MP-1.0, luyện tự do, báo cáo (đang làm)

| Phase | Service | Người | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|
| 1 | content | Codex | In progress | 316f763 | Đã có CEFR, can-do, phạm vi. Chưa có: seed Grammar/Vocab (chờ chuyên gia duyệt), kiểm vòng cạnh |
| 2 | learning | Claude | Completed | fc59156, 6ba6180 | Công thức mặc định vẫn `DEEPTUTOR`. Bật `MP1` chờ bạn xác nhận |
| 3 | learning, content | Claude | In progress | 3b01d59, f0e249f | Đã có luyện tự do, cây, dự trữ 2 gói, chặn bài ôn. Còn: lọc band và PREMIUM (Content chưa trả trường), cefr trên cây, contract |
| 4 | learning, user | Codex (user), Claude (learning) | Pending | – | Chờ `GET /internal/users/{id}/learning-goal` của Codex (C2) |
| 5 | learning | Claude | Pending | – | Phụ thuộc phase 4 |
| 6 | library | Codex | Completed | fec4c19 | Flashcard 5 hộp, đến hạn phân trang ở DB, test library xanh |
| 7 | learning | Claude | Pending | – | Không phụ thuộc, có thể bắt đầu ngay |
| 8 | content | Codex | Pending | – | Migration content V43 (số tạm) |
| 9 | docs | Claude | Pending | – | Chờ mọi phase |

## Plan 261017-1932: Gamification (chưa xong)

| Phase | Service | Người | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|
| 1 | access | Codex | Completed | e7b78aa | Points cộng thưởng có trần, idempotent. Chưa xác nhận lại từng tiêu chí |
| 2 | learning | Claude | In progress | 9275542, 34ad080, b5610cf | XP, cấp độ, múi giờ, progress có test (546 test xanh). Còn: HTTP cho khối `gamification`, test ranh giới nửa đêm, contract (phase 8) |
| 3 | learning, access | Claude (learning) | In progress | 3648f87 | Gate, trừ tim, refill, PREMIUM 7 có test. Còn: refund (access chưa có route nội bộ), test HTTP 403, test luồng ôn, contract |
| 4 | learning, access | Claude, Codex | Pending | – | Streak server. Access đã có `STREAK_FREEZE_DEBIT`. Phụ thuộc phase 2 (đang làm) |
| 5 | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 6 | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 7 | learning, user | Claude, Codex | Pending | – | Cần `POST /internal/users/display-names` (C3) |
| 8 | docs | Claude | Pending | – | Chờ mọi phase |

## Việc đang chờ

**Codex**
1. Route refund nội bộ cho points (`/internal/access/points/refund`). Chặn phần hoàn points của gamification phase 3.
2. Endpoint C2 `learning-goal` và C3 `display-names` ở user-service. Chặn mastery phase 4 và gamification phase 7.
3. Content trả thêm band và `requiredFeatureKey` trên `PracticeSet`. Chặn lọc band và PREMIUM của mastery phase 3.
4. Hoàn thành seed Grammar/Vocab sau khi chuyên gia duyệt (mastery phase 1).
5. Dựng lại image Learning và chạy lại demo bước 4, 5, 8 (plan 261016 phase 13).

**Claude**
1. Mastery phase 7 (cờ vắng học): không phụ thuộc, có thể làm ngay.
2. Gamification phase 2: test HTTP cho khối `gamification`.
3. Gamification phase 3: test HTTP 403 và test luồng ôn khi hết tim.

**Bạn**
1. Bật công thức `MP1` ở môi trường thật (đổi tier học viên đang học).
2. Chuyên gia IELTS: gold set và rubric (plan 261016 phase 11), duyệt seed Grammar/Vocab (mastery phase 1).
3. Hoàn points khi refill lỗi: nhờ Codex thêm route refund, hay chấp nhận cơ chế idempotent không refund?
4. Tim trừ mỗi lần nộp mới (hiện tại), hay chỉ lần đầu?

## Câu hỏi còn mở

- Phiên Codex hiện tại đang mang tiêu đề "rubric và AI grading" nhưng sửa flashcard. Cần xác nhận phạm vi phiên.
- Plan 261016 có hai dòng phân công ngoài bảng; cần rà trước khi dùng.
