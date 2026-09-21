# IELTSPath — Feature Tree

> Feature tree cấp sản phẩm cho hệ thống IELTSPath. Cây này mô tả **người dùng có thể làm gì**, không phải cấu trúc package/code.

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
│   │
│   ├── Profile
│   │   ├── View profile
│   │   ├── Update profile
│   │   └── Learner profile
│   │
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
│   └── Initial learning setup
│
├── 3. Access, Premium & Point
│   ├── Activation Key
│   │   ├── Enter activation key
│   │   ├── Validate key
│   │   ├── Activate key
│   │   └── View activation result
│   │
│   ├── Point
│   │   ├── POINT_50
│   │   ├── POINT_100
│   │   ├── View point balance
│   │   ├── Point debit for AI grading
│   │   ├── Point refund
│   │   └── Point transaction history
│   │
│   └── Premium
│       ├── PREMIUM_30D
│       ├── PREMIUM_90D
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
│   └── Initialize learner baseline
│       ├── Skill scores
│       ├── Error analysis
│       ├── Initial mastery
│       └── Initial topic progress
│
├── 5. Adaptive Learning
│   ├── Mastery Tracking
│   │   ├── Track mastery by knowledge point
│   │   ├── WEAK
│   │   ├── LEARNING
│   │   └── MASTERED
│   │
│   ├── Topic Progress
│   │   ├── LOCKED
│   │   ├── UNLOCKED
│   │   ├── IN_PROGRESS
│   │   └── COMPLETED
│   │
│   ├── Topic Gate
│   │   ├── Take gate assessment
│   │   ├── Minimum score 70%
│   │   ├── Pass / Fail
│   │   └── Unlock next topic
│   │
│   ├── Daily Planning
│   │   ├── Generate daily plan
│   │   ├── Available study minutes
│   │   ├── Prioritize weak knowledge
│   │   ├── Generate daily tasks
│   │   └── Regenerate future plan from latest progress
│   │
│   ├── Learning Activity
│   │   ├── Track completed activities
│   │   ├── Study history
│   │   └── Streak
│   │
│   └── Mistake Notebook
│       ├── Save detected mistakes
│       ├── View mistakes
│       └── Review mistakes
│
├── 6. Practice
│   ├── Practice by Skill
│   │   ├── Reading
│   │   ├── Listening
│   │   ├── Writing
│   │   └── Speaking
│   │
│   ├── Practice by Question Type
│   │   ├── Reading
│   │   │   ├── True / False / Not Given
│   │   │   ├── Matching Headings
│   │   │   ├── Matching Information
│   │   │   ├── Sentence Completion
│   │   │   ├── Summary Completion
│   │   │   └── Multiple Choice
│   │   │
│   │   └── Listening
│   │       ├── Multiple Choice
│   │       ├── Form Completion
│   │       ├── Map / Diagram Labelling
│   │       ├── Matching
│   │       └── Sentence Completion
│   │
│   ├── Practice by Topic
│   ├── Practice by Knowledge Point
│   ├── Mini Quiz
│   └── Adaptive Recommended Practice
│
├── 7. Mock Test & Assessment
│   ├── Full Mock Test
│   │   ├── Reading
│   │   ├── Listening
│   │   ├── Writing
│   │   └── Speaking
│   │
│   ├── Assessment Attempt
│   │   ├── Start attempt
│   │   ├── Autosave response
│   │   ├── Submit attempt
│   │   └── Resume supported attempt
│   │
│   ├── Objective Scoring
│   │   ├── Reading auto-score
│   │   └── Listening auto-score
│   │
│   ├── Result
│   │   ├── Overall result
│   │   ├── Skill score
│   │   ├── Item result
│   │   ├── Correct / Incorrect
│   │   └── Error analysis
│   │
│   └── Feed result back to Adaptive Learning
│
├── 8. Writing & Speaking Grading
│   ├── Learner Submission
│   │   ├── Writing text submission
│   │   └── Speaking audio submission
│   │
│   ├── AI Grading
│   │   ├── Check point balance
│   │   ├── Writing cost
│   │   ├── Speaking cost
│   │   ├── Create grading job
│   │   ├── Retry without double charge
│   │   ├── Refund on permanent system/provider failure
│   │   └── Return grading result
│   │
│   └── Human Grading
│       ├── Check active Premium
│       ├── Check Human Grading Credit
│       ├── Create human review
│       ├── Assign EXAMINER
│       ├── IN_REVIEW
│       ├── Complete review
│       └── Consume Human Grading Credit
│
├── 9. Learning Content
│   ├── Curriculum
│   │   ├── Topic tree
│   │   ├── Knowledge points
│   │   ├── Topic prerequisites
│   │   └── Topic gate rules
│   │
│   ├── Vocabulary
│   │   ├── Vocabulary item
│   │   ├── Multiple senses
│   │   ├── Part of speech
│   │   ├── Vietnamese meaning
│   │   ├── English definition
│   │   ├── Example
│   │   ├── Image
│   │   └── Pronunciation audio
│   │
│   ├── Question Bank
│   │   ├── Question
│   │   ├── Question version
│   │   ├── Question options
│   │   ├── Question type
│   │   └── Map question → knowledge point
│   │
│   └── Content Package
│       ├── Practice package
│       ├── Mock test package
│       ├── Placement package
│       ├── FREE content
│       └── PREMIUM content
│
├── 10. Personal Learning Library
│   ├── Notes
│   │   ├── Create note
│   │   ├── Edit note
│   │   ├── Delete note
│   │   └── View notes
│   │
│   └── Flashcards
│       ├── Create flashcard manually
│       ├── Create from vocabulary sense
│       ├── Create from highlighted text
│       ├── Edit flashcard
│       ├── Delete flashcard
│       ├── Create deck
│       ├── Manage deck
│       └── Add/remove flashcard from deck
│
├── 11. Game
│   ├── Single-player Game
│   │   ├── Vocabulary game
│   │   ├── Grammar game
│   │   ├── Matching
│   │   ├── Spelling
│   │   ├── Sentence Completion
│   │   ├── Error Correction
│   │   └── Word Order
│   │
│   └── Realtime Multiplayer
│       ├── Create room
│       ├── Join room by code
│       ├── Leave room
│       ├── Player ready
│       ├── Host/start match
│       ├── Countdown
│       ├── Realtime questions
│       ├── Submit realtime answer
│       ├── Live score
│       ├── Reconnect
│       ├── Player disconnect
│       ├── Match result
│       └── Match ranking
│
├── 12. Notification
│   ├── Notification Preferences
│   ├── Push Device Management
│   ├── Reminder Schedule
│   ├── In-app Notification
│   └── Notification Delivery
│       ├── PUSH
│       ├── EMAIL
│       ├── PENDING
│       ├── SENT
│       ├── FAILED
│       └── Retry
│
├── 13. AI Assistant
│   ├── AI Conversation
│   ├── AI Messages
│   └── AI Chat / RAG
│
├── 14. Community
│   ├── Feed
│   ├── Create post
│   ├── Edit/delete own post
│   ├── Comment
│   ├── Reply comment
│   └── React to post
│
├── 15. Content Author Workspace
│   ├── Manage topic tree
│   ├── Manage knowledge points
│   ├── Manage vocabulary
│   ├── Manage question bank
│   ├── Manage practice packages
│   ├── Manage placement test content
│   ├── Manage mock tests
│   ├── Manage FREE/PREMIUM access level
│   └── Publish content version
│
├── 16. Examiner Workspace
│   ├── View assigned reviews
│   ├── Start review
│   ├── Review Writing submission
│   ├── Review Speaking submission
│   ├── Submit score/feedback
│   └── Complete review
│
├── 17. Sales Staff Workspace
│   ├── Manage key products
│   ├── Issue activation keys
│   ├── View key status
│   ├── View key activation
│   └── Support customer activation
│
└── 18. Admin Workspace
    ├── User Management
    ├── Role Management
    ├── Content Administration
    ├── Activation Key Administration
    ├── Point Configuration
    ├── Premium Plan Configuration
    ├── Grading Point Cost Configuration
    ├── Examiner Management
    ├── Notification Administration
    ├── Community Moderation
    └── System Monitoring / Audit
```

---

## Mapping feature → service

| Feature group                                                                    | Service chính           |
| :------------------------------------------------------------------------------- | :----------------------- |
| Account, role, profile, learning goal, Premium subscription                      | `user-service`         |
| Activation Key, Point                                                            | `access-service`       |
| Curriculum, vocabulary, question bank, content package                           | `content-service`      |
| Placement, practice attempt, mock test, result, grading                          | `assessment-service`   |
| Mastery, topic progress, daily plan, streak, mistake notebook, notes, flashcards | `learning-service`     |
| Single-player / realtime multiplayer game                                        | `game-service`         |
| AI Chat / RAG                                                                    | `ai-assistant-service` |
| Notification / push / reminder                                                   | `notification-service` |
| Feed / comment / reaction                                                        | `community-service`    |

---

## Core learner flow

```text
Register/Login
    ↓
Create Learning Goal
    ↓
Activate Point if needed
    ↓
Placement Test
    ↓
Initial Assessment Result
    ↓
Initial Mastery + Topic Progress
    ↓
Adaptive Daily Plan
    ↓
Practice / Learn
    ↓
Assessment
    ↓
Update Mastery
    ↓
Re-plan
    ↺
```

Các feature như Flashcard, Game, AI Assistant, Community và Notification hỗ trợ vòng học chính này nhưng không thay thế Adaptive Learning core.

# Mô tả chi tiết từng chức năng

Phần này diễn giải chi tiết các node trong Feature Tree theo góc nhìn nghiệp vụ. Mục tiêu là để cả team hiểu **feature dùng để làm gì, ai sử dụng, luồng chính và rule quan trọng**, không đi sâu vào API/code implementation.

---

## 1. Account & Identity

### 1.1 Authentication

| Chức năng               | Mô tả                                                                                                                                                                                                           |
| :------------------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Register**        | Cho phép người dùng tạo tài khoản mới. Sau khi đăng ký thành công, user được tạo trong`user-service`, gán role mặc định `CUSTOMER` và có thể tiếp tục hoàn thiện learner profile. |
| **Login**           | Xác thực tài khoản và cấp access token/refresh token để client sử dụng các API được bảo vệ.                                                                                                       |
| **Logout**          | Kết thúc phiên đăng nhập hiện tại. Nếu hệ thống quản lý refresh token server-side thì refresh token tương ứng phải bị revoke/xóa.                                                             |
| **Refresh token**   | Cấp access token mới khi access token hết hạn nhưng refresh token vẫn còn hợp lệ.                                                                                                                        |
| **Forgot password** | User yêu cầu đặt lại mật khẩu bằng email hoặc cơ chế xác minh tài khoản đã đăng ký.                                                                                                            |
| **Reset password**  | Kiểm tra reset token còn hợp lệ rồi cho phép user đặt mật khẩu mới. Reset token chỉ được sử dụng một lần và có thời hạn.                                                                   |
| **OAuth login**     | Cho phép đăng nhập qua provider bên ngoài nếu hệ thống bật OAuth. Identity từ provider được ánh xạ vào user nội bộ.                                                                            |

### 1.2 Profile

| Chức năng               | Mô tả                                                                                                                                                                |
| :------------------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **View profile**    | Hiển thị thông tin tài khoản và learner profile của user đang đăng nhập.                                                                                    |
| **Update profile**  | Cho phép cập nhật các thông tin cá nhân được phép thay đổi. Các field hệ thống như role hoặc Premium không được cập nhật qua chức năng này. |
| **Learner profile** | Lưu các thuộc tính phục vụ trải nghiệm học tập của learner, tách khỏi thông tin xác thực tài khoản.                                                  |

### 1.3 Role & Authorization

Hệ thống hiện có 5 role:

| Role               | Trách nhiệm chính                                                                               |
| :----------------- | :------------------------------------------------------------------------------------------------- |
| `ADMIN`          | Quản trị toàn hệ thống, user, cấu hình nghiệp vụ và các chức năng quản trị.         |
| `CUSTOMER`       | Người học sử dụng hệ thống IELTSPath.                                                       |
| `CONTENT_AUTHOR` | Xây dựng và quản lý topic, vocabulary, câu hỏi, practice, placement và mock test.          |
| `EXAMINER`       | Chấm Writing/Speaking trong luồng Human Grading.                                                 |
| `SALES_STAFF`    | Thực hiện nghiệp vụ liên quan đến key sản phẩm và hỗ trợ kích hoạt cho khách hàng. |

Role dùng để phân quyền. **Premium và Point không phải role**.

---

## 2. Learning Goal & Onboarding

### Set target IELTS band

Learner nhập band mục tiêu, ví dụ:

```text
Current estimate: 5.5
Target band: 7.0
```

Band mục tiêu là một trong các dữ liệu đầu vào để hệ thống lập kế hoạch học.

### Set exam date

Learner khai báo ngày dự kiến thi IELTS. ADP có thể dùng khoảng thời gian còn lại để xác định cường độ kế hoạch học.

### Set available study time

Learner khai báo số phút có thể học mỗi ngày, ví dụ:

```text
30 phút/ngày
60 phút/ngày
90 phút/ngày
```

`daily_plans` dùng dữ liệu này để giới hạn tổng `estimated_minutes` của các task được tạo.

### Update learning goal

Learner có thể thay đổi target band, exam date hoặc thời gian học. Khi learning goal thay đổi, kế hoạch tương lai có thể được tính lại nhưng không làm mất lịch sử học trước đó.

### Initial learning setup

Đây là bước hoàn tất onboarding trước khi hệ thống bắt đầu cá nhân hóa:

```text
Account
   ↓
Learner Profile
   ↓
Learning Goal
   ↓
Placement Test
```

---

## 3. Access, Premium & Point

## 3.1 Activation Key

### Enter activation key

User nhập key nhận được từ hệ thống bán hàng.

### Validate key

`access-service` kiểm tra:

```text
key có tồn tại?
key còn hiệu lực?
key chưa được sử dụng?
key có bị khóa/hủy không?
```

Raw key không nên được lưu trực tiếp nếu có thể tránh; server có thể lưu hash để xác thực.

### Activate key

Sau khi hợp lệ, hệ thống tạo `key_activations` và snapshot giá trị thực tế user được nhận tại thời điểm activation.

### View activation result

Client hiển thị kết quả rõ ràng, ví dụ:

```text
Kích hoạt thành công POINT_50
Bạn nhận +50 point
```

hoặc:

```text
Kích hoạt thành công PREMIUM_30D
Premium đến 21/10/2026
Human grading credits: +4
```

---

## 3.2 Point

Point chỉ dùng cho các nghiệp vụ có tính phí bằng point, hiện tại trọng tâm là **AI Grading**.

### POINT_50 / POINT_100

Các key product ví dụ:

```text
POINT_50  → +50 point
POINT_100 → +100 point
```

Giá trị thực tế được snapshot vào `key_activations`.

### View point balance

User có thể xem số point hiện tại trong `point_wallets`.

### Point debit for AI grading

Khi Writing/Speaking yêu cầu AI chấm:

```text
Check balance
   ↓
Debit point
   ↓
Create grading job
```

Chi phí hiện tại:

```text
Writing  → 3 point
Speaking → 5 point
```

### Point refund

Nếu AI provider hoặc hệ thống thất bại vĩnh viễn sau khi đã trừ point, hệ thống tạo một ledger entry mới để hoàn lại point.

Không sửa hoặc xóa transaction cũ.

### Point transaction history

`point_ledger_entries` cho user/system biết point thay đổi do đâu:

```text
+50 KEY_CREDIT
-3  AI_GRADING_DEBIT
+3  AI_GRADING_REFUND
```

Point đã mua **không mất khi Premium hết hạn**.

---

## 3.3 Premium

### PREMIUM_30D / PREMIUM_90D

Ví dụ sản phẩm hiện tại:

```text
PREMIUM_30D
→ 30 ngày Premium
→ 4 Human Grading Credits

PREMIUM_90D
→ 90 ngày Premium
→ 12 Human Grading Credits
```

### Premium content access

Learner có subscription Premium đang active được truy cập content có `access_level = PREMIUM`.

### Premium feature access

Một số capability có thể được giới hạn cho Premium thông qua plan/feature rule.

### Human grading credits

Human grading không dùng AI point. Nó sử dụng quota nằm trên Premium subscription.

Ví dụ:

```text
total = 4
used  = 2
remaining = 2
```

### Premium expiration

Khi Premium hết hạn:

```text
Premium content access → mất
Premium feature access → mất
Human grading credit chưa dùng → không còn sử dụng được
AI point trong wallet → vẫn giữ nguyên
```

---

## 4. Placement Test

Placement Test xác định baseline ban đầu của learner để ADP bắt đầu cá nhân hóa.

### Start placement test

Tạo `assessment_attempts`, snapshot các section và item cần thiết để nội dung attempt không thay đổi nếu author sửa đề sau đó.

### Reading placement

Learner làm các câu Reading. Kết quả có thể auto-score bằng đáp án.

### Listening placement

Learner làm Listening. Kết quả objective có thể auto-score.

### Writing placement

Learner nộp bài Writing vào `learner_submissions`.

Luồng:

```text
Writing submission
    ↓
AI grading
    ↓
-3 point
    ↓
Writing score
```

### Speaking placement

Learner nộp audio Speaking.

Luồng:

```text
Speaking audio
    ↓
AI grading
    ↓
-5 point
    ↓
Speaking score
```

### Submit placement test

Hoàn tất attempt và khóa trạng thái cần thiết để bắt đầu scoring/grading.

### View placement result

Learner nhận:

```text
Reading score
Listening score
Writing score
Speaking score
Overall/summary
```

### Initialize learner baseline

Placement không chỉ để cho ra một Overall Band. Hệ thống phải khai thác kết quả chi tiết:

```text
item_results
+
question_knowledge_points
+
error_analysis
```

để khởi tạo:

```text
mastery_records
topic_progress
```

Ví dụ:

```text
Reading Inference     0.25 → WEAK
Present Perfect       0.30 → WEAK
Articles              0.75 → MASTERED
```

Đây là điểm xuất phát của Adaptive Learning.

---

## 5. Adaptive Learning

Đây là core loop của hệ thống.

```text
ASSESS
  ↓
MEASURE
  ↓
ADAPT
  ↓
LEARN
  ↓
ASSESS AGAIN
  ↺
```

## 5.1 Mastery Tracking

### Track mastery by knowledge point

Mỗi learner có mastery riêng cho từng `knowledge_point`.

Ví dụ:

```text
U01 + Present Perfect → 0.35
U02 + Present Perfect → 0.82
```

### WEAK / LEARNING / MASTERED

MVP dùng hard-coded rule.

Ví dụ hiện tại:

```text
0.00 - 0.39 → WEAK
0.40 - 0.69 → LEARNING
0.70 - 1.00 → MASTERED
```

Các threshold/update rule có thể được chỉnh sau, nhưng schema không cần thay đổi lớn.

---

## 5.2 Topic Progress

Topic là cấp lớn hơn knowledge point.

### LOCKED

User chưa đủ prerequisite để học topic.

### UNLOCKED

Đã đủ prerequisite và có thể bắt đầu.

### IN_PROGRESS

User đang học topic.

### COMPLETED

User đã đạt điều kiện gate của topic.

---

## 5.3 Topic Gate

Topic Gate quyết định learner có hoàn thành topic hay chưa.

### Take gate assessment

Learner làm assessment dành cho topic.

### Minimum score 70%

MVP hiện dùng rule:

```text
score >= 70%
→ PASS
```

### Pass / Fail

Mỗi lần làm gate được lưu trong `topic_gate_attempts`, nên có thể biết lịch sử:

```text
55% → FAIL
68% → FAIL
82% → PASS
```

### Unlock next topic

Sau PASS:

```text
Current topic → COMPLETED
      ↓
Check topic_prerequisites
      ↓
Next eligible topic → UNLOCKED
```

---

## 5.4 Daily Planning

### Generate daily plan

Mỗi ngày hệ thống tạo một `daily_plan` riêng cho learner.

Input chính:

```text
learning goal
mastery records
topic progress
due/review state
available minutes
```

### Available study minutes

Plan không nên vượt quá thời gian user khai báo.

### Prioritize weak knowledge

MVP có thể ưu tiên:

```text
WEAK
→ LEARNING
→ review cần thiết
→ MASTERED
```

tùy hard-coded algorithm.

### Generate daily tasks

Một plan chứa nhiều task, ví dụ:

```text
1. Matching Headings Practice - 20 phút
2. Present Perfect Practice   - 15 phút
3. Vocabulary Review          - 10 phút
4. Mini Quiz                  - 10 phút
```

### Regenerate future plan from latest progress

Kế hoạch không cố định vĩnh viễn. Khi mastery/topic progress thay đổi, các plan tương lai có thể thay đổi theo.

---

## 5.5 Learning Activity

### Track completed activities

Khi learner hoàn thành task/practice/game phù hợp, hệ thống có thể ghi một `learning_activity`.

### Study history

Dùng để hiển thị user đã học gì theo ngày/thời gian.

### Streak

Theo dõi chuỗi ngày học:

```text
current_streak
longest_streak
```

Streak là engagement feature, không quyết định mastery.

---

## 5.6 Mistake Notebook

### Save detected mistakes

Các lỗi có ý nghĩa từ assessment có thể được đưa vào notebook.

### View mistakes

Learner xem lại:

```text
câu đã sai
đáp án
knowledge point liên quan
error type
```

### Review mistakes

Cho phép learner quay lại học/luyện các lỗi cũ.

---

## 6. Practice

Practice khác Full Mock Test ở chỗ learner có thể tập trung vào một phạm vi nhỏ hơn.

## 6.1 Practice by Skill

### Reading

Chỉ luyện Reading.

### Listening

Chỉ luyện Listening.

### Writing

Chỉ luyện Writing và có thể yêu cầu AI/Human grading tùy mode.

### Speaking

Chỉ luyện Speaking và có thể yêu cầu grading.

---

## 6.2 Practice by Question Type

Mục tiêu là luyện riêng đúng dạng IELTS learner đang yếu.

### Reading

Ví dụ:

```text
True / False / Not Given
Matching Headings
Matching Information
Sentence Completion
Summary Completion
Multiple Choice
```

### Listening

Ví dụ:

```text
Multiple Choice
Form Completion
Map / Diagram Labelling
Matching
Sentence Completion
```

ADP có thể kết hợp:

```text
Knowledge Point weakness
+
Question Type weakness
```

để chọn bài phù hợp.

Ví dụ:

```text
Reading Inference yếu
+
Matching Headings sai nhiều
↓
recommend Matching Headings practice
```

---

## 6.3 Practice by Topic

Learner chủ động chọn một topic đã unlock để luyện.

## 6.4 Practice by Knowledge Point

Cho phép luyện sâu một knowledge point cụ thể.

Ví dụ:

```text
Present Perfect / Since-For
Reading / Inference
```

## 6.5 Mini Quiz

Một bài ngắn giúp learner kiểm tra nhanh kiến thức vừa học.

## 6.6 Adaptive Recommended Practice

Practice được ADP đề xuất dựa trên state hiện tại, thay vì learner tự chọn.

---

## 7. Mock Test & Assessment

## 7.1 Full Mock Test

Mô phỏng bài thi đầy đủ:

```text
Reading
Listening
Writing
Speaking
```

Mock Test dùng cùng assessment engine nhưng content package có cấu trúc đầy đủ hơn practice.

---

## 7.2 Assessment Attempt

### Start attempt

Tạo một attempt mới và snapshot đề.

### Autosave response

Khi learner đang làm bài, đáp án hiện tại được lưu để tránh mất dữ liệu.

### Submit attempt

Chuyển attempt sang trạng thái đã nộp và bắt đầu scoring/grading.

### Resume supported attempt

Nếu loại assessment cho phép resume, learner có thể tiếp tục attempt chưa submit. Rule resume phụ thuộc loại bài và business setting.

---

## 7.3 Objective Scoring

### Reading auto-score

So đáp án learner với đáp án chuẩn để tính item result.

### Listening auto-score

Tương tự Reading.

---

## 7.4 Result

### Overall result

Kết quả tổng hợp cấp attempt.

### Skill score

Điểm riêng cho từng skill.

### Item result

Điểm/correctness cấp từng question item.

### Correct / Incorrect

Lưu trạng thái learner trả lời đúng hay sai.

### Error analysis

Xác định lỗi liên quan knowledge point nào để phục vụ ADP.

---

## 7.5 Feed result back to Adaptive Learning

Sau assessment:

```text
AssessmentCompleted
      ↓
learning-service
      ↓
update mastery
update topic progress
update mistake notebook
      ↓
daily plan tiếp theo thay đổi
```

---

## 8. Writing & Speaking Grading

## 8.1 Learner Submission

### Writing text submission

Lưu nội dung Writing user nộp.

### Speaking audio submission

Lưu reference tới audio Speaking. Binary nên nằm ở object storage, DB chỉ lưu metadata/reference.

---

## 8.2 AI Grading

### Check point balance

Trước khi tạo grading job:

```text
balance >= required point
```

### Writing cost

Hiện tại:

```text
3 point
```

### Speaking cost

Hiện tại:

```text
5 point
```

### Create grading job

Một `grading_job` đại diện cho một yêu cầu chấm cụ thể.

### Retry without double charge

Technical retry của cùng job không được trừ point lần nữa.

Cần dùng idempotency/reference tới ledger debit ban đầu.

### Refund on permanent system/provider failure

Nếu provider/system không thể hoàn tất job:

```text
original debit
+
refund ledger entry
```

### Return grading result

Kết quả được cập nhật về assessment result/skill score tương ứng.

---

## 8.3 Human Grading

### Check active Premium

Human Grading chỉ được dùng khi subscription Premium còn active.

### Check Human Grading Credit

Phải còn:

```text
used < total
```

### Create human review

Tạo workflow chấm người thật.

### Assign EXAMINER

Gán bài cho user có role `EXAMINER`.

### IN_REVIEW

Examiner đã bắt đầu xử lý bài.

### Complete review

Examiner hoàn tất score/feedback.

### Consume Human Grading Credit

Một Human Review hợp lệ tiêu thụ quota của Premium subscription.

Human Grading **không trừ AI point**.

---

## 9. Learning Content

## 9.1 Curriculum

### Topic tree

Cấu trúc các chủ đề học theo hierarchy.

### Knowledge points

Đơn vị kiến thức/kỹ năng nhỏ mà mastery theo dõi.

### Topic prerequisites

Mô tả topic nào phải hoàn thành trước topic nào.

### Topic gate rules

Điều kiện hoàn thành topic, MVP hiện dùng assessment score threshold.

---

## 9.2 Vocabulary

### Vocabulary item

Headword chung của một từ.

### Multiple senses

Một từ có thể có nhiều nghĩa.

Ví dụ:

```text
run
1. verb → chạy
2. verb → vận hành
3. noun → lượt/chặng...
```

### Part of speech

Mỗi sense xác định noun/verb/adjective...

### Vietnamese meaning

Nghĩa tiếng Việt của sense.

### English definition

Định nghĩa tiếng Anh.

### Example

Mỗi sense có một câu ví dụ.

### Image

Mỗi sense có thể có ảnh minh họa.

### Pronunciation audio

DB lưu reference/URL; audio binary nằm ở object storage.

---

## 9.3 Question Bank

### Question

Identity logic của câu hỏi.

### Question version

Nội dung phiên bản cụ thể để hỗ trợ chỉnh sửa/publish mà không làm mất lịch sử.

### Question options

Các lựa chọn cho question type cần option.

### Question type

Ví dụ:

```text
MULTIPLE_CHOICE
MATCHING_HEADINGS
TRUE_FALSE_NOT_GIVEN
...
```

### Map question → knowledge point

Dùng để biết câu hỏi đang kiểm tra kiến thức nào.

Đây là liên kết quan trọng cho ADP.

---

## 9.4 Content Package

### Practice package

Bộ nội dung phục vụ luyện tập.

### Mock test package

Bộ đề mô phỏng thi.

### Placement package

Bộ đề đầu vào.

### FREE content

CUSTOMER thông thường được truy cập.

### PREMIUM content

Yêu cầu active Premium.

Ngoài package-level access, câu hỏi riêng lẻ có thể có access level nếu business cần.

---

## 10. Personal Learning Library

Personal Library thuộc `learning-service`, nhưng không trực tiếp ảnh hưởng mastery.

## 10.1 Notes

### Create note

Learner tạo ghi chú học tập.

### Edit note

Cập nhật nội dung note.

### Delete note

Xóa note do user sở hữu.

### View notes

Xem danh sách hoặc chi tiết note.

---

## 10.2 Flashcards

### Create flashcard manually

User tự nhập front/back.

### Create from vocabulary sense

Flashcard có thể tham chiếu `vocabulary_sense_id`.

Front/back vẫn nên snapshot để flashcard cũ không tự đổi nếu dictionary content được sửa.

### Create from highlighted text

Learner highlight một đoạn text rồi tạo flashcard từ phần đã chọn.

### Edit flashcard

User chỉnh front/back.

### Delete flashcard

Xóa flashcard cá nhân.

### Create deck

Tạo bộ flashcard như:

```text
IELTS Environment
Academic Vocabulary
My Reading Mistakes
```

### Manage deck

Đổi tên/mô tả/trạng thái deck.

### Add/remove flashcard from deck

Quan hệ many-to-many cho phép một flashcard nằm trong nhiều deck.

---

## 11. Game

Game không cập nhật mastery trực tiếp.

## 11.1 Single-player Game

### Vocabulary game

Game dựa trên vocabulary item/sense.

### Grammar game

Game dựa trên question/version hoặc snapshot grammar item.

### Matching

Ghép từ-nghĩa, ảnh-từ hoặc cặp dữ liệu tương ứng.

### Spelling

Learner nhập chính tả.

### Sentence Completion

Điền phần còn thiếu.

### Error Correction

Tìm/sửa lỗi.

### Word Order

Sắp xếp từ/cụm từ thành câu đúng.

Kết quả dùng cho:

```text
score
game history
leaderboard nếu cần
```

không dùng làm mastery source ở MVP.

---

## 11.2 Realtime Multiplayer

### Create room

User tạo `game_room`, hệ thống sinh `room_code`.

### Join room by code

Người chơi nhập code để tham gia.

### Leave room

Player rời phòng trước hoặc trong trận theo rule.

### Player ready

Player báo sẵn sàng. Live ready state có thể được giữ ở Redis.

### Host/start match

Host hoặc hệ thống kiểm tra điều kiện rồi tạo `game_match`.

### Countdown

Countdown là realtime state; không ghi mỗi tick xuống PostgreSQL.

### Realtime questions

Server broadcast câu/game state tới các client trong room.

### Submit realtime answer

Player gửi answer qua WebSocket.

### Live score

Điểm tạm thời có thể giữ ở Redis để broadcast nhanh.

### Reconnect

Khi client mất kết nối ngắn hạn, user có thể reconnect vào match đang chạy bằng identity/session hiện tại.

### Player disconnect

Ghi nhận state disconnect cần thiết; không cần persist mọi heartbeat.

### Match result

Cuối trận persist final score.

### Match ranking

Tạo thứ hạng player trong match.

Persistent model:

```text
game_rooms
   ├── game_room_members
   └── game_matches
        ├── game_match_players
        ├── game_sessions
        │    └── game_answers
        └── game_events
```

Redis:

```text
presence
connection
ready state
countdown
current question
live score
```

PostgreSQL:

```text
room
match
players
answers
final score
business events cần audit
```

---

## 12. Notification

## 12.1 Notification Preferences

User cấu hình loại notification hoặc channel được phép nhận.

## 12.2 Push Device Management

Quản lý device/token dùng để push qua FCM hoặc provider tương đương.

## 12.3 Reminder Schedule

Cho phép tạo rule nhắc học:

```text
20:00 mỗi ngày
→ nhắc learner học
```

## 12.4 In-app Notification

`notifications` lưu nội dung hiển thị trong app:

```text
"Bài Writing của bạn đã được chấm."
"Đã đến giờ học hôm nay."
```

## 12.5 Notification Delivery

`notification_deliveries` lưu từng lần hệ thống cố gửi notification ra ngoài.

Ví dụ:

```text
attempt 1 → FAILED
attempt 2 → SENT
```

Hỗ trợ:

```text
PUSH
EMAIL
PENDING
SENT
FAILED
retry
provider message id
failure reason
```

---

## 13. AI Assistant

## 13.1 AI Conversation

Một conversation gom các message trong cùng một cuộc hội thoại.

## 13.2 AI Messages

Lưu message user/assistant theo thứ tự.

## 13.3 AI Chat / RAG

Cho phép learner hỏi AI trong ngữ cảnh học tập.

RAG/vector/citation architecture chi tiết chưa phải phần đã chốt hoàn toàn trong schema hiện tại, nên Feature Tree chỉ coi đây là capability của `ai-assistant-service`.

---

## 14. Community

## 14.1 Feed

Hiển thị bài post từ community chung.

Không còn Study Group.

## 14.2 Create post

User tạo bài đăng.

## 14.3 Edit/delete own post

User chỉ sửa/xóa nội dung thuộc quyền của mình, trừ quyền moderation của Admin.

## 14.4 Comment

Thêm bình luận vào post.

## 14.5 Reply comment

`parent_comment_id` cho phép tạo reply.

## 14.6 React to post

User react theo reaction type hệ thống hỗ trợ.

Không còn Challenge trong Community.

---

## 15. Content Author Workspace

Actor chính:

```text
CONTENT_AUTHOR
ADMIN
```

### Manage topic tree

Tạo/sửa topic và hierarchy.

### Manage knowledge points

Quản lý các đơn vị kiến thức/kỹ năng bên dưới topic.

### Manage vocabulary

Tạo từ, sense, definition, example, image/audio reference.

### Manage question bank

Tạo/sửa/version các câu hỏi và mapping knowledge point.

### Manage practice packages

Ghép question/content thành các bộ luyện.

### Manage placement test content

Quản lý đề dùng để xác định baseline ban đầu.

### Manage mock tests

Quản lý đề thi thử.

### Manage FREE/PREMIUM access level

Xác định content nào user Free/Premium được dùng.

### Publish content version

Đưa một content version từ trạng thái draft sang trạng thái có thể sử dụng trong learner flow.

Content đã được learner làm nên được snapshot/version thay vì sửa ngược lịch sử attempt.

---

## 16. Examiner Workspace

Actor:

```text
EXAMINER
```

### View assigned reviews

Xem danh sách Human Review đang được assign.

### Start review

Chuyển trạng thái review sang `IN_REVIEW`.

### Review Writing submission

Đọc bài Writing và thực hiện chấm.

### Review Speaking submission

Nghe audio Speaking và thực hiện chấm.

### Submit score/feedback

Nhập kết quả chấm và feedback.

### Complete review

Kết thúc review, grading job được hoàn tất và learner nhận kết quả.

---

## 17. Sales Staff Workspace

Actor:

```text
SALES_STAFF
ADMIN
```

### Manage key products

Quản lý loại sản phẩm key:

```text
POINT_50
POINT_100
PREMIUM_30D
PREMIUM_90D
```

### Issue activation keys

Sinh/phát hành key theo product.

### View key status

Xem:

```text
AVAILABLE
REDEEMED
EXPIRED
CANCELLED
...
```

### View key activation

Tra cứu ai kích hoạt key nào, lúc nào, được cấp gì.

### Support customer activation

Hỗ trợ kiểm tra lỗi kích hoạt mà không trực tiếp sửa point/subscription trái với ledger/audit rule.

---

## 18. Admin Workspace

## 18.1 User Management

Quản lý tài khoản, trạng thái tài khoản và các thao tác admin được phép.

## 18.2 Role Management

Gán/thu hồi role phù hợp.

Ví dụ:

```text
CUSTOMER
→ thêm CONTENT_AUTHOR
```

Không dùng role để cấp Premium.

## 18.3 Content Administration

Kiểm soát và quản trị content ở cấp hệ thống.

## 18.4 Activation Key Administration

Quản lý các vấn đề key ở cấp admin.

## 18.5 Point Configuration

Quản lý rule/sản phẩm liên quan point; lịch sử wallet vẫn phải dựa trên ledger.

## 18.6 Premium Plan Configuration

Quản lý plan/feature hoặc cấu hình Premium tương ứng.

## 18.7 Grading Point Cost Configuration

Quản lý `grading_point_costs`.

Ví dụ:

```text
WRITING AI  = 3
SPEAKING AI = 5
```

Khi thay đổi giá, grading job phải giữ `point_cost_snapshot`.

## 18.8 Examiner Management

Quản lý account/role examiner và hỗ trợ assignment nếu nghiệp vụ cần.

## 18.9 Notification Administration

Quản trị các vấn đề notification hệ thống, template/rule nếu sau này bổ sung.

## 18.10 Community Moderation

Xử lý post/comment vi phạm hoặc cần moderation.

## 18.11 System Monitoring / Audit

Theo dõi trạng thái hệ thống, integration/event failure và các log/audit cần thiết. Đây là chức năng vận hành, không đồng nghĩa lưu toàn bộ technical log trong business database.

---

# Quan hệ giữa các feature chính

Luồng learner quan trọng nhất:

```text
Account
   ↓
Learning Goal
   ↓
Placement Test
   ↓
Assessment Result
   ↓
Initial Mastery
   ↓
Adaptive Daily Plan
   ↓
Practice
   ↓
Assessment
   ↓
Update Mastery
   ↓
New Daily Plan
   ↺
```

Các capability hỗ trợ:

```text
Point
→ AI Grading

Premium
→ Premium Content
→ Human Grading

Personal Library
→ Notes / Flashcards

Game
→ Engagement / Practice support

Notification
→ Reminder / grading result

AI Assistant
→ Learning support

Community
→ Social interaction
```

Điểm cần giữ rõ trong implementation:

```text
Assessment
= đo learner làm được gì

Learning / ADP
= quyết định learner nên học gì tiếp

Content
= cung cấp nội dung chuẩn

Grading
= chấm Writing/Speaking

Game
= engagement/realtime gameplay

Personal Library
= dữ liệu học cá nhân

Access
= key + point

User
= identity + role + Premium subscription
```
