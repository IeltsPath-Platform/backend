# Cập nhật tiến độ các phase (lần 3)

Thời điểm: 2026-10-17 23:10. Nhánh `feat/knowledge-tree-assessment-journey`, HEAD `4108814`.
Báo cáo trước: `workflow-status-261017-2215-phase-progress-update-report.md`.

Ghi chú: Claude = `learning-service` và tài liệu. Codex = content, assessment, access, user, library, Gateway. Git ghi
mọi commit dưới một tài khoản, nên phân công theo service.

## Thay đổi từ báo cáo trước

- Codex xong: C2 `learning-goal` và C3 `display-names` (user-service, 572fc14), Mastery 8 preview/hẹn giờ (Content V43,
  66351e9), acceptance plan 261016 (4108814). Image Learning đã dựng lại; demo bước 5 và 8 đạt.
- Claude xong: Mastery 7 phần learning (cờ `INACTIVE` + thống kê hoạt động, 725baf5, đã commit). Gamification 4 (streak)
  đã code và có test, **chưa commit**.
- Mở khóa: Mastery 4 và Gamification 7 (hết chờ C2, C3).
- Chặn mới: relay points của Gamification 4 chờ Codex làm consumer Access (hợp đồng C6).

## Tổng quan

| Plan | Phase | Completed | In progress | Pending | Blocked |
|---|---|---|---|---|---|
| `261016` Knowledge tree | 15 (17 mục) | 6 | 9 (có mục mở) | 0 | 2 (2b, 11) |
| `261017-1931` Mastery | 9 | 3 (2 Claude; 6, 8 Codex) | 3 (1 Codex; 3, 7 Claude) | 3 (4, 5, 9) | 0 |
| `261017-1932` Gamification | 8 | 1 (1 Codex) | 3 (2, 3, 4 Claude) | 4 (5, 6, 7, 8) | 0 |

## Plan 261016 (chỉ ghi phần đổi)

| Phase | Người | Trạng thái | Ghi chú |
|---|---|---|---|
| 13 | Chung | Completed (có mục mở) | Demo bước 5 (403 `COURSE_TEST_LOCKED`) và 8 (QA queue 200) đạt. Bước 4 không tái hiện, chưa tìm ra nguồn giá trị sai ở lần chạy trước. 12 hàng `question_versions` có `difficulty IS NULL` |

Các phase còn lại giữ như báo cáo 22:15 (2b và 11 vẫn Blocked).

## Plan 261017-1931: Mastery

| Phase | Service | Người | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|
| 1 | content | Codex | In progress | 316f763 | Có CEFR, can-do, phạm vi (407 test). Chưa có V42 seed Grammar/Vocab, chờ duyệt |
| 2 | learning | Claude | Completed | fc59156, 6ba6180 | Công thức mặc định vẫn `DEEPTUTOR`. Bật `MP1` chờ bạn xác nhận |
| 3 | learning, content | Claude | In progress | 3b01d59, f0e249f | Còn: lọc band và PREMIUM (Content chưa trả trường), cefr trên cây, contract |
| 4 | learning, user | Codex, Claude | Pending, **đã mở** | – | C2 xong (572fc14). Claude làm báo cáo kỹ năng |
| 5 | learning | Claude | Pending | – | Phụ thuộc phase 4 |
| 6 | library | Codex | Completed | fec4c19 | Leitner 5 hộp, 65 test |
| 7 | learning | Claude | In progress | 725baf5 | Cờ `INACTIVE` tự mở/đóng, DAU/WAU/MAU và retention (UTC). Còn: test hàng việc EXAMINER có cờ INACTIVE |
| 8 | content | Codex | Completed | 66351e9 | Content V43, 22 test. **Chưa chạy lại scheduler integration test sau thay đổi annotation cuối** |
| 9 | docs | Claude | Pending | – | Chờ mọi phase |

## Plan 261017-1932: Gamification

| Phase | Service | Người | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|
| 1 | access | Codex | Completed | e7b78aa | Endpoint cộng points C1, 54 test |
| 2 | learning | Claude | In progress | 9275542, 34ad080, b5610cf | Còn: HTTP cho khối `gamification`, test nửa đêm, contract |
| 3 | learning, access | Claude | In progress | 3648f87 | Còn: refund (Access chưa có route), test HTTP 403, test luồng ôn |
| 4 | learning, access | Claude, Codex | In progress | chưa commit | Xong: `StreakRules` (11 test), V26, ghi streak trong transaction nộp bài, `GET /me/streak`, `POST /me/streak/freeze`, mốc 7 ngày +100 XP và hàng đợi 10 points, mốc 30 ngày +500 XP, 5 test DB. Chưa: relay points (chờ Codex), thưởng KP 90% (+5 points), essay chưa tính ngày hoạt động, refund |
| 5 | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 6 | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 7 | learning, user | Claude, Codex | Pending, **đã mở** | – | C3 xong (572fc14) |
| 8 | docs | Claude | Pending | – | Chờ mọi phase |

Quyết định ở phase 4: lượt đóng băng mua ngày D phủ D nếu D còn trống khi được chấm, không thì ngày trống đầu tiên sau D.
Luật cũ (phủ ngày mai) làm giá 10/20/30 không bao giờ đạt được. Mua khi chưa có streak trả `409 NO_STREAK_TO_PROTECT`.

## Kiểm chứng learning-service

- 27 test mục tiêu (streak, gamification, tim, migration) đều pass.
- Suite đầy đủ đang chạy lại sau khi dọn `target/` (lần trước hỏng do IDE ghi đè `target/classes`). Chưa có kết quả cuối.

## Việc đang chờ

**Codex**
1. Consumer Access cho hợp đồng C6 (`work-split.md`): nhận `points.grant.requested`, cộng idempotent theo `grantId`, trả `points.grant.settled`. Chặn relay của Gamification 4.
2. Route refund nội bộ cho points (hoàn khi đóng băng hoặc hồi tim lỗi).
3. Content trả band và `requiredFeatureKey` trên `PracticeSet` (Mastery 3).
4. Seed V42 Grammar/Vocab sau khi duyệt (Mastery 1).
5. Chạy lại scheduler integration test của Content V43; gán `difficulty` cho 12 câu đang NULL (hoặc xác nhận mặc định 10 XP chấp nhận được); ghi bước 4 là "không tái hiện" trong `acceptance.md`.

**Claude**
1. Chờ suite đầy đủ, rồi commit Gamification 4 (phần streak).
2. Viết phía learning của C6 (relay job và listener `points.grant.settled`) khi Codex đồng ý hợp đồng.
3. Mastery 4 và 5 (đã mở); Gamification 7 (đã mở).
4. Test HTTP cho gamification, tim 403, luồng ôn khi hết tim; test EXAMINER có cờ INACTIVE.

**Bạn**
1. Duyệt seed V42 Grammar/Vocab; chuyên gia IELTS: gold set và rubric (261016 phase 11).
2. Bật công thức `MP1` ở môi trường thật (đổi tier học viên đang học).
3. Hoàn points khi lỗi: nhờ Codex thêm route refund, hay chấp nhận idempotent không refund?
4. Tim trừ mỗi lần nộp mới (hiện tại), hay chỉ lần đầu?
5. Xác nhận luật đóng băng mới ("phủ ngày mua nếu trống") thay luật "phủ ngày mai".

## Câu hỏi còn mở

- Phạm vi phiên Codex "rubric và AI grading" đang sửa flashcard và 134 file; cần xác nhận.
- Essay có tính là ngày hoạt động của streak không (hiện không, vì không phát sự kiện câu hỏi)?
- Plan 261016 có hai dòng phân công ngoài bảng phase; cần rà trước khi dùng.
