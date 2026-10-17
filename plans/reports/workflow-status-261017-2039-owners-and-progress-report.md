# Phân công và tiến độ: ba plan đang chạy

Cập nhật: 2026-10-17, nhánh `feat/knowledge-tree-assessment-journey`.

## Cách đọc

- **Người phụ trách** theo service, đúng với `plans/261017-1931-mastery-practice-and-progress/reports/work-split.md`.
  - **Claude:** `learning-service`, tài liệu chung (`AGENTS.md`, `docs/`, contract learning).
  - **Codex:** `content-service`, `assessment-service`, `access-service`, `user-service`, `library-service`, Gateway, Compose.
- Git ghi mọi commit dưới cùng một tài khoản `hieuLeFpt`, nên không tách được người theo commit. Cột "Người phụ trách" là phân công theo service, không phải lịch sử ai gõ code.
- Trạng thái:
  - **Pending:** chưa bắt đầu.
  - **In progress:** đang làm, đã có phần commit.
  - **Completed:** tiêu chí đã có test, không còn việc của phase.
  - **Completed (có mục mở):** phase đóng nhưng còn tiêu chí chưa kiểm chứng. Ghi rõ ở cột ghi chú.
  - **Blocked:** chờ người hoặc phase khác.

## Tổng hợp nhanh

| Plan | Phase | Tổng trạng thái |
|---|---|---|
| `261016-0131` Knowledge tree và hành trình đánh giá | 15 (17 mục với 2a/2b, 3a/3b) | Hoàn thành: 3b, 5, 8, 9, 10, 12. Hoàn thành có mục mở: 1, 2a, 3a, 4, 6, 7, 13, 14, 15. Còn mở: 2b. Blocked: 11 |
| `261017-1931` Mastery, luyện tự do, báo cáo | 9 | 1 completed (2), 1 in progress (3), 7 pending |
| `261017-1932` Gamification | 8 | 1 completed (1, Codex), 7 pending |

## Plan 261016: Cây tri thức và hành trình đánh giá

Plan đang `in_progress` vì phase 11 và 13 còn mở.

| Phase | Nội dung | Service | Người phụ trách | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|---|
| 1 | Cây tri thức: cạnh tiên quyết và API | content | Codex | Completed (có mục mở) | ef2d259 | Còn mục chưa tick: contract `knowledge-tree-v1.md` khớp controller |
| 2a | Course/topic 6.0, 7.0 INACTIVE, difficulty bắt buộc | content, learning | Codex (content), Claude (learning) | Completed (có mục mở) | ef2d259 | Mục chưa tick: học viên không thấy course 6.0/7.0; tạo version thiếu difficulty trả 400 |
| 2b | Seed KP, cạnh, difficulty backfill, bộ câu | content | Codex | Pending (còn mở) | 4979d4b, e445f03, f982c6a | Còn 7 KP Listening chưa đủ 3 bộ (cần audio). Bộ Reading/Writing là bản nháp chưa duyệt |
| 3a | Error taxonomy: bảng, CRUD, ErrorTagger | content, learning, assessment | Codex (content, assessment), Claude (learning) | Completed (có mục mở) | a140b8c | Mục chưa tick: câu điền sai chính tả → `SPELLING`; distractors không lộ trước khi nộp; giám khảo không lưu mã ngoài danh mục |
| 3b | Duyệt mã lỗi chính thức, gắn distractors | content | Codex | Completed | 4979491, 80e45d2, 7a822d8 | V35, V36. Bộ mã vẫn là bản nháp chờ chuyên gia duyệt |
| 4 | Event log từng câu và thời gian làm | learning, assessment | Claude, Codex | Completed (có mục mở) | 85ad593 | Chưa tick: bài thi có câu gắn mã → event có `error_code` |
| 5 | Mastery profile, hạn ôn, báo cáo | learning | Claude | Completed | 31decbe | Đủ tiêu chí |
| 6 | Giỏ ôn nhiều mức và recheck | learning | Claude | Completed (có mục mở) | 9dd1005 | Chưa tick: KP 85% đến hạn xuất hiện ở nhóm `due` và làm đạt thì lùi hạn |
| 7 | Chẩn đoán dò ngược theo cây | learning | Claude | Completed (có mục mở) | 4a0b56e | Chưa tick: `GET /api/learning/report` trả `DIAGNOSING` khi phiên đang chạy |
| 8 | Practice test theo mốc course | content, assessment, learning | Codex, Claude | Completed | e710f11 | Đủ tiêu chí trong phase file |
| 9 | Điểm có trọng số và kết quả khóa | learning | Claude | Completed | 4107e44 | Đủ tiêu chí |
| 10 | Hàng chờ giám khảo và chính sách AI | assessment | Codex | Completed | 80123b5 | Đủ tiêu chí |
| 11 | Rubric dạng dữ liệu và bộ test nghiệm thu AI | content, assessment, learning | Codex (công cụ), chuyên gia IELTS (nghiệm thu) | Blocked | f9c6fbd | Công cụ xong. Chưa có gold set do chuyên gia chấm và chưa có lần đánh giá thật. Lần chạy 30 bài tổng hợp lỗi `LLM_REQUEST_FAILED`. Số liệu tổng hợp không được dùng làm bằng chứng độ chính xác |
| 12 | Khu quản trị: hồ sơ 360°, cờ, hàng việc, đánh giá Mentor | learning, assessment, access | Claude, Codex | Completed | 3ef1939, 1ea5e44, c014d56 | Gateway route đã có test |
| 13 | Contract, tài liệu, nghiệm thu | docs | Chung | Completed (có mục mở) | 14853bf, 3fb9632 | `mvn verify` xanh trừ library-service. Bước 5 demo đã sửa trong code (58ec6f0) nhưng chưa dựng lại image Learning. Bước 4 (packageVersionId lệch) chưa điều tra. Bước 8 chưa chạy được |
| 14 | Học viên sửa essay và nộp lại | learning | Claude | Completed (có mục mở) | 9cf6d74 | Chưa tick: luật bằng chứng không đổi (test cũ của `SubmitLessonEssayUseCase` xanh) |
| 15 | Mentor chữa essay Practice và lesson | learning, assessment, access | Claude, Codex | Completed (có mục mở) | c03b9e0, 0a35bc4 | Chưa tick: học viên nhận phản hồi thật từ giám khảo khi nối hai service. Chưa chạy thử nối thật |

## Plan 261017-1931: Mastery MP-1.0, luyện tự do, báo cáo tiến độ

Nguồn: `plan.md`, `reports/work-split.md`. Tổng `pending`, phase 2 và 3 đang chạy.

| Phase | Nội dung | Service | Người phụ trách | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|---|
| 1 | Metadata KP (CEFR, can-do, phạm vi), cây 6 miền | content | Codex | Pending | – | Migration V41 (số tạm). Seed Grammar/Vocab chờ chuyên gia duyệt. Learning chưa đọc trường mới; phase 3 đang chờ cefr từ phase này |
| 2 | Mastery MP-1.0 | learning | Claude | Completed | fc59156, 6ba6180 | Công thức mặc định vẫn `DEEPTUTOR`. Chuyển sang `MP1` cần bạn xác nhận. Đã có decay job và rebuild |
| 3 | Luyện tự do theo KP và cây của học viên | learning, content | Claude (learning), Codex (chưa làm trường band/PREMIUM của Content) | In progress | 3b01d59, f0e249f | Đã có start, submit, dự trữ 2 gói, chặn bài ôn, cây. Còn: lọc band course và gói PREMIUM (cần Content trả thêm trường), cefr chờ phase 1, contract chưa cập nhật |
| 4 | Báo cáo theo kỹ năng và band mục tiêu | learning, user | Codex (endpoint user), Claude (learning) | Pending | – | Phía learning chờ endpoint `GET /internal/users/{id}/learning-goal` của Codex (C2) |
| 5 | Bản chụp tuần, xu hướng, bản tháng | learning | Claude | Pending | – | Phụ thuộc phase 4 |
| 6 | Flashcard 5 hộp | library | Codex | Pending | – | Migration library V3 (số tạm) |
| 7 | Cờ vắng học và thống kê hoạt động | learning | Claude | Pending | – | Không phụ thuộc phase khác |
| 8 | Xem trước và xuất bản hẹn giờ | content | Codex | Pending | – | Migration content V43 (số tạm) |
| 9 | Contract, tài liệu, nghiệm thu | docs | Claude | Pending | – | Đợi mọi phase trên |

Việc còn lại của phase 2, ghi để khỏi quên: chờ xác nhận bật `MP1`, cập nhật `learner-report-v1.md` đã xong (commit 6ba6180).

## Plan 261017-1932: Gamification (XP, tim, streak, nhiệm vụ, huy hiệu, league)

Nguồn: `plan.md`, `reports/work-split.md`.

| Phase | Nội dung | Service | Người phụ trách | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|---|---|
| 1 | Points: cộng thưởng, loại giao dịch mới | access | Codex | Completed | e7b78aa | Plan ghi completed. Chưa kiểm tra lại từng tiêu chí phase file. Trần kiếm points mặc định 20/ngày chờ bạn chốt |
| 2 | XP, cấp độ, múi giờ | learning | Claude | Pending | – | Có thể bắt đầu ngay, không phụ thuộc |
| 3 | Tim | learning, access | Claude (learning), Codex (trừ points hồi tim) | Pending | – | Phụ thuộc phase 1, 2 và mục luyện tự do (đã xong) |
| 4 | Streak phía server, đóng băng, thưởng points | learning, access | Claude, Codex | Pending | – | Phụ thuộc phase 1, 2 |
| 5 | Nhiệm vụ ngày | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 6 | Huy hiệu | learning | Claude | Pending | – | Phụ thuộc phase 2, 4 |
| 7 | League tuần | learning, user | Claude (learning), Codex (`POST /internal/users/display-names`, C3) | Pending | – | Endpoint user chưa có |
| 8 | Contract, tài liệu, nghiệm thu | docs | Claude | Pending | – | Đợi mọi phase trên |

## Việc đang chờ theo người

**Codex cần làm**
1. Endpoint `GET /internal/users/{id}/learning-goal` (C2). Chặn plan 261017-1931 phase 4.
2. Endpoint `POST /internal/users/display-names` (C3). Chặn phase 7 của gamification.
3. Thêm `band` và `requiredFeatureKey` vào `PracticeSet` trả cho learning (mới phát sinh từ phase 3). Chặn lọc band và PREMIUM của luyện tự do.
4. Migration access `V4` (đã commit, xác nhận khi chạy Compose).
5. Dựng lại image Learning và chạy lại demo bước 4, 5, 8 của plan 261016 phase 13.
6. Migration content V41, V43 và library V3 của plan 261017-1931 (phase 1, 6, 8).

**Claude cần làm**
1. Tiếp tục gamification phase 2 (XP, cấp độ).
2. Hoàn thiện phase 3 mastery: contract luyện tự do, test `REVIEW_REQUIRED` và test điểm course không đổi.
3. Phase 5 và 7 mastery, sau khi phase 4 có endpoint.

**Bạn cần quyết định**
1. Bật công thức `MP1` ở môi trường thật (đổi tier học viên đang học).
2. Trần points kiếm mỗi ngày (mặc định 20) và kinh tế points.
3. Chuyên gia IELTS: gold set và rubric của plan 261016 phase 11, và duyệt seed Grammar/Vocab của plan 261017-1931 phase 1.

## Câu hỏi còn mở

- Có muốn tách tên người trong commit theo phân công không? Hiện mọi commit cùng một tài khoản git nên không tra được người làm theo commit.
- Plan 261016 phase 11 có chấp nhận được khi chưa có chuyên gia không? Nếu không thì phase này ở trạng thái blocked, không tính là hoàn thành.
- Plan 261016 phase 13 có cần rerun Compose sạch để đóng không? Hay chấp nhận đóng với mục mở như hiện tại?
