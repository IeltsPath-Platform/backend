---
type: feature-tree
version: V3
status: current
updated: 2026-10-03
scope: IELTSPath backend
---

# IELTSPath — Feature Tree

Cây tính năng của backend, đối chiếu với code ngày 2026-10-03 (nhánh `feat/skill-tracks-practice`). Nhãn:

- ✅ đã làm: có route và dữ liệu, chạy được qua Gateway.
- 🟡 một phần: có bảng hoặc một phần route, thiếu luồng hoặc dữ liệu để dùng trọn.
- ⏳ chưa làm: chỉ là phạm vi sản phẩm, chưa có code.

Thiết kế cũ dựa trên DeepTutor (Tutor Agent, Guided Session, Learner Memory, RAG, `next_objective`) đã bị gỡ cùng
service Python ngày 2026-10-01; chỉ công thức mastery được port sang `MasteryCalculator`. Xem git history nếu cần bản cũ.

## 1. Cây tính năng

```text
IELTSPath
│
├── 1. Account & Identity                                   user-service
│   ├── Authentication
│   │   ├── ✅ Register, Login, Logout, Refresh token
│   │   ├── ✅ Forgot password, Reset password (action token)
│   │   └── 🟡 OAuth: xem/gỡ liên kết tài khoản; chưa có đăng nhập bằng OAuth
│   ├── Profile
│   │   ├── ✅ View/Update account (/me)
│   │   └── ✅ Learner profile
│   ├── ✅ Role & Authorization: ADMIN, CUSTOMER, CONTENT_AUTHOR, EXAMINER, SALES_STAFF
│   └── ✅ User management (admin): status, roles, action tokens
│
├── 2. Learning Goal                                        user-service
│   ├── ✅ Set/Update target band, exam date, study time (một goal ACTIVE)
│   └── ⏳ Dùng goal để xếp lộ trình (lộ trình hiện chỉ theo thứ tự topic)
│
├── 3. Access, Premium & Point                              access-service
│   ├── ✅ Plans, subscription, entitlement theo user
│   ├── ✅ Activation Key: tạo, kích hoạt, thu hồi, key products
│   ├── ✅ Point: số dư, lịch sử, trừ (chấm Writing), hoàn, điều chỉnh
│   ├── 🟡 Premium content: Content gắn requiredFeatureKey; Learning chưa kiểm gói khi học
│   └── 🟡 Human grading credits: có API trừ lượt; chưa có luồng chấm bằng người cho học viên
│
├── 4. Curriculum & Content Administration                  content-service
│   ├── ✅ Topic (mỗi topic một skill, khóa đổi skill khi đã có bài)
│   ├── ✅ Knowledge point, learning type
│   ├── ✅ Question bank: version, hint, purpose LEARNING/EXAM, một câu một chủ
│   ├── ✅ Content package/version: PRACTICE_SET gắn bài, TOPIC_TEST gắn topic, publish/archive
│   ├── ✅ Asset: passage, image, audio (URL media do Content ghép)
│   ├── 🟡 Lesson: đọc qua API nội bộ; tạo/sửa bài chỉ bằng seed migration (chưa có API soạn bài)
│   └── ⏳ Công cụ soạn đề thi thử / xếp lớp (MOCK_TEST, PLACEMENT_TEST)
│
├── 5. Learning Path & Lessons                              learning-service
│   ├── ✅ Lộ trình theo skill: mỗi skill một chuỗi topic, một topic IN_PROGRESS
│   ├── ✅ Bài học: khối lý thuyết, asset, bài tập tự chấm (≥70%), bằng chứng lần nộp đầu
│   ├── ✅ Gợi ý (hint) cho câu sai khi khối chưa đạt
│   ├── ✅ Writing Task 1/2 trong bài: chấm LLM, hạn mức ngày, trừ point
│   ├── ✅ Mastery theo KP (công thức port từ DeepTutor v1.6.9)
│   ├── ✅ Practice theo bài: catalog, attempt, lời giải sau khi nộp, cờ "đã lộ"
│   ├── ✅ Bài ôn (thang ôn tập)
│   │   ├── Tạo từ Practice < 70% (theo KP) hoặc từ kết quả thi (mastery < 0.6)
│   │   ├── Set ôn có hint → trượt → lý thuyết đúng KP + quick-check → set mới chưa lộ
│   │   ├── Trượt set thứ hai hoặc hết đề → SKIPPED
│   │   └── Chỉ chặn bài, Practice, thi cuối cùng skill
│   ├── ✅ Thi cuối topic: cần mọi bài qua Practice; mã đề dùng một lần, xoay vòng
│   ├── ✅ Topic không có thi cuối (Writing) đạt khi xong mọi bài
│   └── ✅ Nhận kết quả thi (AssessmentCompleted.v2) qua RabbitMQ
│
├── 6. Assessment                                           assessment-service
│   ├── ✅ Attempt: tạo từ package, cấu trúc đề, lưu câu trả lời, nộp, hết giờ
│   ├── ✅ Tự chấm Reading/Listening, kết quả, lời giải khi ≥70%
│   ├── ✅ Phát AssessmentCompleted.v2 qua outbox
│   ├── ✅ Chấm bởi giám khảo (EXAMINER): tạo/sửa/chốt kết quả
│   ├── 🟡 Grading job AI: tạo và xem job; chưa có worker chấm
│   ├── 🟡 Video practice: ghi lần luyện Dictation/Shadowing theo segment; chưa có luồng chấm
│   ├── 🟡 Placement Test: có loại attempt; chưa có đề, Learning chỉ lưu version
│   ├── ⏳ Mock Test (thi thử 4 kỹ năng, quy đổi band): plan riêng
│   └── ⏳ Speaking: luyện, chấm, xếp lớp
│
├── 7. Vocabulary & Video                                   library-service
│   ├── ✅ Vocabulary catalog: sense, tìm kiếm, publish
│   ├── ✅ Video catalog: segment, lexical entry
│   ├── ✅ Video progress, saved segments
│   └── 🟡 Dictation, Shadowing: lần luyện lưu ở assessment (video practice)
│
├── 8. Personal Library                                     library-service
│   ├── ✅ Notes
│   └── ✅ Flashcards, decks, deck items
│
├── 9. Learning Activity & Streak                           user-service
│   └── ✅ Ghi activity, xem streak
│
├── 10. Game Learning                                       game-service
│   ├── ✅ Vocabulary game (snapshot từ library), Grammar game (snapshot từ content)
│   ├── ✅ Single-player session, trả lời từng câu
│   ├── ✅ Multiplayer room: tạo/vào bằng mã, ready, leave, WebSocket ticket, match
│   └── ⏳ Matchmaking tự động
│
├── 11. Quiz & Leaderboard                                  game-service
│   └── ⏳ Quiz event, participation, leaderboard (có bảng ở game V1, chưa có API)
│
├── 12. Community                                           community-service
│   ├── ✅ Post, feed, comment, reaction
│   └── ✅ Kiểm duyệt (đổi status post/comment)
│
└── 13. Notification                                        notification-service
    └── ⏳ Preference, push device, reminder, in-app, gửi lại (service mới là khung package)
```

## 2. Luồng học chính

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

## 3. Service sở hữu

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
