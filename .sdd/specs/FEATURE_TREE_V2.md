---
type: feature-tree
version: V3.1
status: current
updated: 2026-10-03
scope: IELTSPath backend
---

# IELTSPath — Feature Tree

Cây tính năng của backend, soát lại với code trên `main` ngày 2026-10-03 (sau khi merge skill tracks và Practice).
Mỗi nhánh được kiểm theo route trong controller, route Gateway, seed migration, dữ liệu trong DB của stack
`docker-compose.mvp.yml` và hai báo cáo E2E ngày 2026-10-02. Nhãn:

- ✅ dùng được: có route, có dữ liệu, người dùng làm được ngay.
- 🟡 một phần: có code nhưng thiếu dữ liệu, thiếu một bước, hoặc người dùng chưa tự làm được. Ghi rõ phần còn thiếu.
- ⏳ chưa làm: chưa có code dùng được.
- `[E2E]`: đã chạy đầu cuối qua Gateway trong nghiệm thu E2E. Nhánh không có dấu này mới chỉ có test tự động.

Thiết kế cũ dựa trên DeepTutor (Tutor Agent, Guided Session, Learner Memory, RAG, `next_objective`) đã bị gỡ cùng
service Python ngày 2026-10-01; chỉ công thức mastery được port sang `MasteryCalculator`. Xem git history nếu cần bản cũ.

## 1. Cần biết trước khi demo

- Chỉ user, access, content, assessment, learning nằm trong stack chạy thử `docker-compose.mvp.yml`. Library, game,
  community chưa từng chạy thử đầu cuối.
- Không có tài khoản admin dựng sẵn; admin đầu tiên phải gán role bằng SQL.
- Library chưa có seed: không có từ vựng, không có video.
- Bài Listening chưa phát được audio: chưa có bucket, 8 file mp3 chưa thu âm (E2E dùng `CONTENT_MEDIA_BASE_URL` giả).
- Chấm Writing mới chạy thử với stub OpenAI-compatible cục bộ, chưa thử với LLM thật.

## 2. Cây tính năng

```text
IELTSPath
│
├── 1. Account & Identity                                   user-service
│   ├── Authentication
│   │   ├── ✅ [E2E] Register, Login (đăng ký công khai không tự gán được ADMIN)
│   │   ├── ✅ Refresh token, Logout
│   │   ├── 🟡 Forgot/Reset password: tạo token 15 phút và route đặt lại; token không tới được người dùng
│   │   │        (chưa gửi email, response không trả token)
│   │   └── ⏳ OAuth: chỉ có xem/gỡ liên kết, không có cách tạo liên kết hay đăng nhập bằng OAuth
│   ├── Profile
│   │   ├── ✅ View/Update account (/me)
│   │   └── ✅ Learner profile
│   ├── ✅ [E2E] Role & Authorization: ADMIN, CUSTOMER, CONTENT_AUTHOR, EXAMINER, SALES_STAFF
│   └── ✅ User management (admin): CRUD, roles, status, action tokens; chưa có admin seed
│
├── 2. Learning Goal                                        user-service
│   ├── ✅ Set/Update target band, exam date, study time (một goal ACTIVE)
│   └── ⏳ Dùng goal để xếp lộ trình (goal chỉ được lưu)
│
├── 3. Access, Premium & Point                              access-service
│   ├── ✅ Plans (có seed), subscription của tôi; nhận gói qua activation key hoặc admin cấp
│   ├── ✅ Activation Key: tạo theo key product (có seed), kích hoạt, thu hồi
│   ├── ✅ [E2E] Point: số dư, lịch sử, admin nạp/điều chỉnh, trừ khi chấm Writing
│   │        (route hoàn point nội bộ có nhưng chưa service nào gọi)
│   ├── ⏳ Thanh toán mua gói hoặc nạp point
│   ├── 🟡 Premium content: topic gắn requiredFeatureKey (có seed); Learning không kiểm gói khi học
│   └── 🟡 Human grading credits: có route trừ lượt; chưa có luồng gửi bài cho giám khảo
│
├── 4. Curriculum & Content Administration                  content-service
│   ├── ✅ Topic (mỗi topic một skill, khóa đổi skill khi đã có bài)
│   ├── ✅ Knowledge point, learning type
│   ├── ✅ Question bank: version, hint, purpose LEARNING/EXAM, một câu một chủ (seed 147 câu)
│   ├── ✅ Content package/version: PRACTICE_SET gắn bài, TOPIC_TEST gắn topic, publish
│   ├── 🟡 Asset: lưu URL hoặc key, Content ghép URL media; không có upload file, chưa có bucket
│   ├── 🟡 Lesson: đọc qua API nội bộ; không có API soạn bài, bài mới chỉ thêm bằng seed migration
│   └── ⏳ Nội dung MOCK_TEST, PLACEMENT_TEST (chưa có đề nào)
│
├── 5. Learning Path & Lessons                              learning-service
│   ├── ✅ [E2E] Lộ trình theo skill: mỗi skill một chuỗi topic, một topic IN_PROGRESS
│   ├── ✅ [E2E] Bài Reading: khối lý thuyết, bài đọc, bài tập tự chấm (≥70%), bằng chứng lần nộp đầu
│   ├── 🟡 [E2E] Bài Listening: chạy với URL audio giả; audio thật chưa có
│   ├── ✅ [E2E] Gợi ý (hint) cho câu sai khi khối chưa đạt
│   ├── ✅ [E2E] Writing Task 1/2 trong bài: chấm LLM, hạn mức ngày, trừ point (mới thử với LLM stub)
│   ├── ✅ [E2E] Mastery theo KP (công thức port từ DeepTutor v1.6.9)
│   ├── ✅ [E2E] Practice theo bài: catalog, attempt, lời giải sau khi nộp, cờ "đã lộ"
│   ├── ✅ [E2E] Bài ôn (thang ôn tập)
│   │   ├── Tạo từ Practice < 70% (theo KP) hoặc từ kết quả thi (mastery < 0.6)
│   │   ├── Set ôn có hint → trượt → lý thuyết đúng KP + quick-check → set mới chưa lộ
│   │   ├── Trượt set thứ hai hoặc hết đề → SKIPPED
│   │   └── Chỉ chặn bài, Practice, thi cuối cùng skill
│   ├── ✅ [E2E] Thi cuối topic: cần mọi bài qua Practice; mã đề dùng một lần, xoay vòng
│   ├── ✅ [E2E] Topic không có thi cuối (Writing) đạt khi xong mọi bài
│   └── ✅ [E2E] Nhận kết quả thi (AssessmentCompleted.v2) qua RabbitMQ
│
├── 6. Assessment                                           assessment-service
│   ├── ✅ [E2E] Attempt: tạo từ package, cấu trúc đề, lưu câu trả lời, nộp
│   │        (quá giờ thì bị đóng ở thao tác kế tiếp; không có tác vụ nền tự đóng)
│   ├── ✅ [E2E] Tự chấm Reading/Listening, kết quả, lời giải khi ≥70%
│   ├── ✅ [E2E] Phát AssessmentCompleted.v2 qua outbox
│   ├── 🟡 Chấm bởi giám khảo (EXAMINER): route tạo/sửa/chốt kết quả; không có danh sách bài chờ chấm,
│   │        chưa có đề cần chấm tay
│   ├── 🟡 Grading job AI: tạo và xem job; chưa có worker chấm
│   ├── 🟡 Video practice: ghi lần luyện Dictation/Shadowing theo segment; chưa có luồng chấm, chưa có video
│   ├── 🟡 Placement Test: có loại attempt; chưa có đề, Learning chỉ lưu version
│   ├── ⏳ Mock Test (thi thử 4 kỹ năng, quy đổi band): plan riêng
│   └── ⏳ Speaking: chỉ có chỗ lưu bài nộp dạng audio, chưa có luồng luyện, chấm, xếp lớp
│
├── 7. Vocabulary & Video                                   library-service (chưa có seed)
│   ├── 🟡 Vocabulary: tạo từ, thêm nghĩa, tìm kiếm, xem chi tiết; DB chưa có từ nào
│   ├── 🟡 Video catalog: tạo, chia segment, gắn lexical entry, publish; DB chưa có video nào
│   ├── 🟡 Video progress, saved segments: chưa dùng được vì chưa có video
│   └── 🟡 Dictation, Shadowing: lần luyện lưu ở assessment (video practice), chưa có luồng chấm
│
├── 8. Personal Library                                     library-service
│   ├── ✅ Notes (gắn được KP hoặc section Reading)
│   └── ✅ Flashcards, decks, deck items, lưu câu hỏi Practice thành thẻ
│
├── 9. Learning Activity & Streak                           user-service
│   └── 🟡 Ghi activity, xem streak; học xong bài không tự ghi activity, FE phải tự gọi API
│
├── 10. Game Learning                                       game-service
│   ├── 🟡 5 loại game: WORD_MEANING_MATCH, SPELLING (từ vựng); SENTENCE_COMPLETION, ERROR_CORRECTION,
│   │        WORD_ORDER (ngữ pháp). Không có dữ liệu: library chưa có từ vựng, cả 147 câu content đều
│   │        thuộc bài hoặc đề nên bị chặn khỏi game
│   ├── 🟡 Single-player session (1–20 câu, 1 điểm mỗi câu đúng, bỏ ngang được); học viên không có API
│   │        để chọn câu ngữ pháp (chỉ ADMIN, CONTENT_AUTHOR liệt kê được câu hỏi)
│   ├── 🟡 Multiplayer room: tạo/vào bằng mã, ready, leave, WebSocket ticket, match, reconnect;
│   │        chưa có dữ liệu và chưa chạy thử đầu cuối
│   └── ⏳ Matchmaking tự động
│
├── 11. Quiz & Leaderboard                                  game-service
│   └── ⏳ Quiz event, participation, leaderboard (có bảng ở game V1, chưa có API)
│
├── 12. Community                                           community-service
│   ├── ✅ Post, feed, comment, reaction
│   └── ✅ Kiểm duyệt (admin đổi status post/comment)
│
└── 13. Notification                                        notification-service
    └── ⏳ Preference, push device, reminder, in-app, gửi lại (service mới là khung package)
```

Tổng: 58 nhánh lá, 31 ✅, 18 🟡, 9 ⏳; 17 nhánh có `[E2E]`.

## 3. Luồng học chính

```text
Register / Login
→ GET /api/learning/topics (mỗi skill một topic IN_PROGRESS)
→ học bài → Practice của bài (lời giải sau khi nộp)
    → Practice < 70%: bài ôn cùng skill (set → lý thuyết + quick-check → set; tối đa 2 set trượt)
→ mọi bài qua Practice → nhận mã đề cuối → Assessment chấm → AssessmentCompleted.v2
→ Learning ghi evidence, ≥70% thì topic PASSED → topic kế tiếp của skill mở
```

Chi tiết: [contract bài học](../../docs/contracts/lesson-learning-v1.md),
[hướng dẫn FE](../../docs/fe-main-flow-guide.md), [Learning README](../../services/learning-service/README.md).

## 4. Service sở hữu

| Nhóm tính năng | Service |
| :--- | :--- |
| Account, role, profile, learning goal, activity, streak | `user-service` |
| Plan, subscription, activation key, point | `access-service` |
| Topic, KP, question bank, package, lesson, asset | `content-service` |
| Vocabulary, video catalog, video progress, notes, flashcards | `library-service` |
| Attempt, chấm, kết quả thi, grading job, video practice | `assessment-service` |
| Lộ trình, tiến độ bài, Practice, bài ôn, mastery, giao mã đề, Writing trong bài | `learning-service` |
| Game, phòng, trận | `game-service` |
| Community | `community-service` |
| Notification | `notification-service` (khung) |
