---
type: feature-tree
version: V2
status: historical-baseline-with-current-mvp-overlay
updated: 2026-10-02
scope: IELTSPath MVP
---

# IELTSPath — Feature Tree V2

> **Baseline lịch sử 2026-09-22:** V2 từng chọn DeepTutor-driven Adaptive Learning & AI Tutoring, với Tutor Session là learning surface chính. Quyết định này đã được thay bằng MVP Java dưới đây; cây feature giữ lại để tra cứu phạm vi sản phẩm, không phải danh sách tính năng đã triển khai.

# 0. MVP hiện tại — cập nhật 2026-10-02

**Bài học trong Content là nơi học chính.** `learning-service` Java lưu tiến độ theo user và `kp_evidence` trong
`learning_db` (V1–V2, 11 bảng); không có tutor runtime, path DeepTutor, notebook, learner memory hay `next_objective`.
Chỉ công thức `compute_mastery` được port từ DeepTutor; mastery tính khi đọc, không điều khiển unlock bằng ngưỡng 0.9.

- `GET /api/learning/topics` lấy Content `topic-sequence` một lần và dựng thứ tự từ `sort_order`; không cần goal,
  placement hoặc LLM. Topic `PASSED` từ `passed_at`; topic đầu chưa đạt là `IN_PROGRESS`, các topic sau `LOCKED`.
- `LessonAccessGate` quyết định cổng review/topic/bài trước. Khối tự chấm đạt ≥70%; chỉ lần nộp đầu mỗi khối ghi
  evidence, nộp lại chỉ để qua khối. Đáp án/lời giải giấu tới khi đạt; transcript giấu tới khi bài hoặc bài thi đạt.
- KP yếu, có câu sai, đã có bài dạy hoàn thành và có gói luyện thì tạo review. Gói `PRACTICE_SET` ≥70% → `DONE`;
  trượt set thứ ba hoặc không còn gói → `SKIPPED`; `PENDING` chặn học tiếp.
- Đề cuối được giao qua `topic_test_assignments`. Assessment suy `TOPIC_TEST` → `TOPIC_GATE`, không gọi User lấy goal;
  câu khách quan tự chấm khi submit, rồi outbox → RabbitMQ → consumer Learning không gọi HTTP.
- Consumer bỏ version bằng/cũ, thay evidence khi version cao hơn; `PLACEMENT` chỉ lưu version. Assignment cùng
  user/version, chưa consume và `assigned_at <= completed_at` mới được consume; ≥70% ghi `passed_at` một chiều.
- Writing Task 1/2 và Listening đã có trong luồng bài học. Writing chấm LLM có quota ngày và trừ point sau khi lưu grade;
  essay không chặn hoàn thành bài. Content V13 có cột hint; trả Reading hint theo câu sai cho learner còn pending.

Nguồn hiện tại: [Learning README](../../services/learning-service/README.md), [schema/flow MVP](../database/mvp-database.md)
và [contract bài học](../../docs/contracts/lesson-learning-v1.md). Các mục lịch sử bên dưới không thay thế các cổng này.

---

# 1. Feature Tree (baseline lịch sử; không phải implementation checklist)

```text
IELTSPath
│
├── 1. Account & Identity
│   ├── Authentication
│   │   ├── Register
│   │   ├── Login
│   │   ├── Logout
│   │   ├── Refresh token
│   │   ├── Forgot password
│   │   ├── Reset password
│   │   └── OAuth login
│   ├── Profile
│   │   ├── View profile
│   │   ├── Update profile
│   │   └── Learner profile
│   └── Role & Authorization
│       ├── ADMIN
│       ├── CUSTOMER
│       ├── CONTENT_AUTHOR
│       ├── EXAMINER
│       └── SALES_STAFF
│
├── 2. Learning Goal & Onboarding
│   ├── Set target IELTS band
│   ├── Set exam date
│   ├── Set available study time
│   ├── Update learning goal
│   └── Build learner context for DeepTutor
│
├── 3. Access, Premium & Point
│   ├── Activation Key
│   │   ├── Enter activation key
│   │   ├── Validate key
│   │   ├── Activate key
│   │   └── View activation result
│   ├── Point
│   │   ├── View point balance
│   │   ├── Point debit for AI grading
│   │   ├── Point refund
│   │   └── Point transaction history
│   └── Premium
│       ├── Premium content access
│       ├── Premium feature access
│       ├── Human grading credits
│       └── Premium expiration
│
├── 4. Placement Test
│   ├── Start placement test
│   ├── Reading placement
│   ├── Listening placement
│   ├── Writing placement
│   │   └── AI grading using point
│   ├── Speaking placement
│   │   └── AI grading using point
│   ├── Submit placement test
│   ├── View placement result
│   └── Seed DeepTutor learner state
│       ├── Formal skill scores
│       ├── Formal item evidence
│       └── Formal error analysis
│
├── 5. Adaptive Learning & AI Tutoring
│   ├── Mastery Path
│   │   ├── Create path from IELTS curriculum
│   │   ├── View learning map
│   │   ├── View current objective
│   │   ├── View objective mastery
│   │   ├── View mastery provenance
│   │   ├── View due reviews
│   │   └── Resume path
│   │
│   ├── Guided Learning Session
│   │   ├── Start study session
│   │   ├── Resume study session
│   │   ├── Pause/end session
│   │   ├── Continue current objective
│   │   └── View session summary
│   │
│   ├── Tutor Agent
│   │   ├── Explain
│   │   ├── Ask diagnostic question
│   │   ├── Give hint
│   │   ├── Guide step by step
│   │   ├── Reteach using another explanation
│   │   ├── Generate example
│   │   ├── Generate practice question
│   │   ├── Review previous mistake
│   │   └── Move to next objective when gate is satisfied
│   │
│   ├── Mastery Evaluation
│   │   ├── Quantitative mastery
│   │   ├── Qualitative mastery
│   │   ├── Probe/test-out
│   │   ├── Tutor quiz grading
│   │   └── Mastery gate
│   │
│   ├── Review & Retention
│   │   ├── Schedule review
│   │   ├── View due review
│   │   ├── Review weak/forgotten objective
│   │   ├── Track lapse/review state
│   │   └── Prioritize review before new objective when due
│   │
│   ├── Formal Assessment Feedback
│   │   ├── Ingest Placement result
│   │   ├── Ingest Practice result
│   │   ├── Ingest Mock result
│   │   ├── Ingest Topic Gate result
│   │   └── Convert formal result to learning evidence
│   │
│   ├── Topic Gate Integration
│   │   ├── Detect formal gate requirement
│   │   ├── Request gate assessment
│   │   ├── Receive gate result
│   │   ├── Remediate after failed gate
│   │   └── Advance after passed gate
│   │
│   ├── Learner Memory
│   │   ├── Remember recurring misconception
│   │   ├── Remember teaching preference
│   │   ├── Recall relevant previous interaction
│   │   └── Use memory in later tutor turns
│   │
│   ├── RAG Learning Support
│   │   ├── Retrieve relevant IELTS material
│   │   ├── Ground explanation in content
│   │   └── Use source context during tutoring
│   │
│   └── Learning History
│       ├── Learning activity
│       ├── Streak
│       ├── Tutor session history
│       ├── Mistake/error history
│       └── Progress overview
│
├── 6. IELTS Practice
│   ├── Practice by Skill
│   │   ├── Reading
│   │   ├── Listening
│   │   ├── Writing
│   │   └── Speaking
│   ├── Practice by Question Type
│   ├── Practice by Topic
│   ├── Practice by Knowledge Point
│   ├── Official Practice
│   ├── Submit answer
│   ├── View result
│   ├── View explanation
│   └── Feed formal result to DeepTutor
│
├── 7. Mock Test
│   ├── Start mock test
│   ├── Timed sections
│   ├── Submit mock test
│   ├── Reading/Listening auto scoring
│   ├── Writing/Speaking grading
│   ├── Skill scores
│   ├── Overall result
│   ├── Error analysis
│   └── Feed formal result to DeepTutor
│
├── 8. Writing & Speaking Grading
│   ├── AI Grading
│   │   ├── Check point balance
│   │   ├── Debit point
│   │   ├── Queue grading job
│   │   ├── View AI feedback
│   │   └── Refund point on failed job when applicable
│   └── Human Grading
│       ├── Check Premium quota
│       ├── Submit human grading request
│       ├── Examiner review
│       └── View human feedback
│
├── 9. Vocabulary
│   ├── Browse vocabulary
│   ├── View sense/definition
│   ├── View example
│   ├── Link vocabulary to knowledge point
│   └── Use vocabulary in content/video/tutor context
│
├── 10. Video Learning
│   ├── Learn from YouTube URL / Video ID
│   ├── Subtitle synchronization
│   ├── Interactive lexical entries
│   ├── Click word/phrase for meaning
│   ├── Save video segment
│   ├── Resume video progress
│   ├── Dictation
│   ├── Shadowing
│   └── Ask Tutor about current segment
│
├── 11. Personal Library
│   ├── Notes
│   │   ├── Create note
│   │   ├── Update note
│   │   ├── Delete note
│   │   └── View notes
│   └── Flashcards
│       ├── Create deck
│       ├── Create/edit card
│       ├── Add card to deck
│       ├── Remove card from deck
│       └── Review personal flashcards
│
├── 12. Game Learning
│   ├── Vocabulary Game
│   ├── Grammar Game
│   ├── Single-player session
│   ├── Multiplayer room
│   ├── Matchmaking
│   ├── Realtime answer/event
│   ├── Match result
│   └── Leaderboard eligibility
│
├── 13. Quiz & Leaderboard
│   ├── Quiz event
│   ├── Quiz participation
│   ├── Score/rank
│   ├── Leaderboard period
│   └── Leaderboard entries
│
├── 14. Notification
│   ├── Notification preference
│   ├── Push device
│   ├── Reminder schedule
│   ├── In-app notification
│   ├── Push/email delivery
│   └── Retry delivery
│
├── 15. Community
│   ├── Create post
│   ├── View feed
│   ├── Comment
│   └── React
│
└── 16. Content Administration
    ├── Topic management
    ├── Knowledge point management
    ├── Prerequisite management
    ├── Topic gate rule management
    ├── Vocabulary management
    ├── Content package/version management
    ├── Question bank management
    ├── Question ↔ knowledge point mapping
    ├── Video metadata/segment management
    └── Publish/archive content
```

---

# 2. Core learner journey V2

Luồng MVP hiện tại:

```text
Register / Login → GET /api/learning/topics → học bài → ôn KP yếu → nhận mã đề cuối
→ Assessment submit / auto-grade → AssessmentCompleted.v2 → Learning cập nhật evidence và passed_at
→ GET /api/learning/topics mở topic kế theo trạng thái suy ra
```

Luồng Tutor dưới đây là lịch sử, không chạy trong MVP Java:

```text
Register / Login
      ↓
Learning Goal
      ↓
Placement Test
      ↓
Formal Assessment Result
      ↓
DeepTutor Mastery Path initialized
      ↓
Start Guided Learning Session
      ↓
DeepTutor next_objective()
      ↓
Tutor Agent teaches
      ↓
explain / ask / hint / guide / generate / review
      ↓
learner interaction
      ↓
Tutor assessment + mastery/review update
      ↓
next_objective()
      ↺
```

Formal assessment periodically feeds evidence back into the same path:

```text
Official Practice / Mock / Topic Gate
                 ↓
         assessment-service
                 ↓
       formal learning evidence
                 ↓
       DeepTutor LearningProgress
                 ↓
        next objective changes
```

---

# 3. Thay đổi quan trọng so với Feature Tree V1 (lịch sử)

| V1 | V2 |
| :--- | :--- |
| `Mastery Tracking` custom | DeepTutor Mastery Path |
| `WEAK / LEARNING / MASTERED` custom threshold | DeepTutor mastery gates |
| `Topic Progress` là adaptive authority | Progress/map là projection từ DeepTutor path |
| `Daily Planning` là lõi | **Bỏ khỏi adaptive core** |
| `daily_tasks` điều khiển học | `next_objective()` điều khiển học |
| AI Assistant riêng | **Bỏ** |
| AI Chat/RAG | Capability/tool bên trong Tutor |
| AI sinh bài là flow chưa chốt | Tutor có thể generate practice trong session |
| Learner click từng task | Tutor Session dẫn learner qua learning loop |

---

# 4. Adaptive Learning V2 hoạt động thế nào (lịch sử)

Không còn flow:

```text
mastery_records
    ↓
custom rule
    ↓
daily plan
    ↓
daily task
```

Flow mới:

```text
LearningProgress
      ↓
DeepTutor policy
      ↓
next_objective()
      ↓
Tutor Agent behavior
      ↓
learner response
      ↓
grade/evidence
      ↓
mastery + scheduler
      ↓
next_objective()
      ↺
```

DeepTutor v1.6.9 baseline ưu tiên:

```text
pending question
→ due review
→ first unmastered objective
→ complete
```

IELTSPath được phép extend policy để yêu cầu formal Topic Gate hoặc dùng target/exam context, nhưng không có một planner song song.

---

# 5. Mastery behavior (baseline DeepTutor lịch sử)

MVP hiện tại dùng evidence theo user và công thức mastery ở §0; cổng bài học và đề cuối quyết định tiến độ.

DeepTutor baseline có hai gate:

```text
Quantitative
MEMORY / PROCEDURE
→ mastery score >= 0.90

Qualitative
CONCEPT / DESIGN
→ tutor qualitative assessment pass
```

Feature không expose thuật toán chi tiết cho learner; UI chỉ cần hiển thị state/progress phù hợp như:

```text
Not started
Learning
Mastered
Review due
```

Đây là display state từ DeepTutor, không phải một business mastery engine riêng.

---

# 6. Tutor behavior (lịch sử, ngoài runtime MVP)

Tutor Agent không chỉ trả lời chat. Mỗi turn có thể chọn behavior phù hợp với current objective:

```text
learner context
      ↓
current objective
      ↓
Tutor Agent
      ↓
choose tutoring behavior
      ↓
EXPLAIN
HINT
ASK
GUIDE
RETEACH
GENERATE
ASSESS
REVIEW
      ↓
learner interaction
      ↺
```

Ví dụ:

```text
Objective: NOT_GIVEN_IDENTIFICATION
Learner answers wrong
        ↓
Tutor detects misconception
        ↓
HINT
        ↓
still wrong
        ↓
RETEACH with another example
        ↓
ASK simpler probe
        ↓
correct
        ↓
ASK harder practice
```

---

# 7. Formal Assessment boundary

Trong MVP hiện tại, bài tập lesson/review được Learning chấm; attempt/result đề cuối thuộc Assessment.
Consumer Learning nhận `AssessmentCompleted.v2` trực tiếp theo user; không cần path/goal. Các mô tả Tutor bên dưới là lịch sử.

Các feature sau vẫn thuộc Assessment, không bị DeepTutor nuốt:

```text
Placement
Official Practice
Mock Test
Writing/Speaking scoring
Formal Topic Gate
```

DeepTutor nhận result/evidence để điều chỉnh learning path.

Tutor-generated micro questions không cần tạo `assessment_attempt` formal cho từng turn.

---

# 8. Topic Gate V2

MVP hiện tại: mọi bài hoàn thành và không còn review `PENDING` → giao mã đề → Assessment chấm → consumer consume
assignment đủ điều kiện → ≥70% ghi `topic_progress.passed_at`. Cổng bài học và trạng thái topic suy ra là authority;
không có policy Tutor chạy song song. Flow DeepTutor dưới đây là lịch sử:

Business rule MVP vẫn có thể giữ:

```text
minimum score = 70%
```

Nhưng flow đổi thành:

```text
DeepTutor reaches gate boundary
      ↓
Formal Topic Gate
      ↓
assessment-service
      ↓
PASS / FAIL
      ↓
DeepTutor ingests result
      ↓
PASS → advance
FAIL → remediation
```

Không còn `topic_progress` engine độc lập quyết định unlock song song.

---

# 9. “Today” experience (lịch sử)

V2 không còn yêu cầu một Daily Plan persisted để làm adaptive authority.

Trang Today có thể là projection:

```text
Continue current objective
Reviews due today
Recent mistake to revisit
Estimated focus based on available study time
```

CTA chính:

```text
Start today's learning session
```

Sau đó DeepTutor điều phối session.

---

# 10. Mistake Review (lịch sử)

Mistake history của Tutor được derive từ DeepTutor:

```text
QuizAttempt
ErrorRecord
MasteryInteraction
LearningEvidence
```

Formal assessment mistakes vẫn đến từ:

```text
assessment-service.error_analysis_items
```

UI có thể hợp nhất hai nguồn thành Mistake Review nhưng không cần một mastery engine thứ hai.

---

# 11. RAG và generation (lịch sử, ngoài runtime MVP)

RAG là một tool của Tutor:

```text
Tutor needs source
      ↓
RAG
      ↓
relevant IELTS material
      ↓
Tutor continues teaching
```

Generated practice mặc định là session artifact:

```text
Tutor generates question
      ↓
learner answers
      ↓
DeepTutor grades/records
```

Không tự publish vào canonical Question Bank.

---

# 12. Service ownership hiện tại

| Feature Group | Owner |
| :--- | :--- |
| Account / Role / Profile / Learning Goal | `user-service` |
| Premium / Activation Key / Point | `access-service` |
| Curriculum / Lessons / Question Bank / Assets | `content-service` |
| Vocabulary / Video metadata / Notes / Flashcards / learner video progress | `library-service` |
| Placement / Practice / Mock / Formal Gate / Grading | `assessment-service` |
| Topic/lesson progress / KP evidence and mastery / Review sets / Test assignments / Lesson Writing | `learning-service` (Java) |
| Learning activity / Streak | `user-service` |
| Game | `game-service` |
| Notification | `notification-service` |
| Community | `community-service` |

Ownership không khẳng định toàn bộ feature trong baseline đã triển khai. Tutor/RAG/memory/session của DeepTutor đã
bị gỡ cùng service Python; thư mục `third_party/deeptutor` chỉ là nguồn tham khảo công thức mastery.

---

# 13. MVP boundaries

MVP Java hiện tại có giới hạn ở §0 và contract bài học: không Tutor Session WebSocket, RAG hay DeepTutor scheduler.
Các yêu cầu sau là baseline V2 lịch sử, đã được thay thế; không dùng làm acceptance criteria cho MVP hiện tại.

MVP V2 **không** yêu cầu:

```text
custom BKT/IRT model
custom FSRS implementation
custom mastery engine ngoài DeepTutor
custom daily planner ngoài DeepTutor
DeepTutor frontend nguyên bản
DeepTutor auth làm identity authority
DeepTutor local SQLite làm production database
AI-generated canonical question publishing tự động
```

MVP cần:

```text
DeepTutor path + mastery + policy + scheduler chạy được
PostgreSQL persistence adapter
IELTS curriculum adapter
formal assessment evidence adapter
Tutor Session WebSocket
RAG/tool integration
learner context integration
```

---

# 14. Product definition sau V2 (baseline lịch sử)

IELTSPath không còn là:

```text
IELTS platform
+ adaptive daily plan
+ chatbot RAG
```

Mà trở thành:

```text
IELTS platform
+ formal assessment engine
+ canonical IELTS curriculum
+ DeepTutor-driven adaptive tutor
```

Đây là baseline Feature Tree V2.
