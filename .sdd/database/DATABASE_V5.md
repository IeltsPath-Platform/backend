---
type: database-design
version: V5
status: deeptutor-core-baseline
updated: 2026-10-01
scope: IELTSPath MVP
external_baseline: HKUDS/DeepTutor v1.6.9
---

# Database — IELTSPath V5

> **2026-10-01:** `ai_learning_db` và `ai-learning-service` (Python) đã bị thay bằng `learning_db`/`learning-service` (Java, §7). Các đoạn nhắc DeepTutor state, tutor hay `ai_learning_db` bên dưới là thiết kế lịch sử của V5.

> V5 **xóa hoàn toàn Adaptive Engine tự thiết kế ở V4**. `ai_learning_db` được thiết kế lại quanh persistence semantics của **DeepTutor Mastery Path + session runtime**. Không còn `mastery_records`, `review_events`, `topic_progress`, `daily_plans`, `daily_tasks`, `ai_conversations`, `ai_messages` hay `ai_assistant_db`. (V5.1 thêm lại `topic_progress` với nghĩa khác, chỉ là tiến độ học bài, xem §7.21.)

> DeepTutor là adaptive learning engine duy nhất. `content_db` sở hữu IELTS curriculum, question bank và bài học; `library_db` sở hữu catalog từ vựng/video và thư viện học cá nhân. `assessment_db` vẫn là formal assessment; `user_db` sở hữu identity/profile/learning goal cùng activity/streak. `ai_learning_db` chỉ giữ **Adaptive Learning Core / Tutor Runtime** của DeepTutor. Service hỗ trợ học viên cũ đã gỡ.

## 0. Lịch sử phiên bản

| Phiên bản | Thay đổi chính |
| :--- | :--- |
| **V1** | Baseline database đầu tiên cho Identity, Content, Assessment, Learning, Personal Library, Notification, Community và AI Chat. |
| **V2** | Review business MVP: Activation Key + Point + Premium; AI grading dùng point; Human Grading dùng Premium quota; ADP cũ dùng DeepTutor-driven mastery; hoàn thiện vocabulary và role nghiệp vụ. |
| **V3** | Tách `game-service`/`game_db`; thêm realtime multiplayer; Personal Library gộp vào `learning-service`. |
| **V4** | Thêm Video Learning qua YouTube URL/Video ID; baseline **87 bảng nghiệp vụ + 9 outbox = 96 bảng vật lý**. |
| **V5** | **Bỏ Adaptive Engine cũ và `ai-assistant-service`**. Learning chuyển sang DeepTutor core. Xóa 6 bảng adaptive cũ + `topic_gate_attempts` + `mistake_notebook_entries` + 2 bảng AI Chat; thêm persistence cho DeepTutor Mastery Path, interaction/evidence/event, tutor session/message/turn runtime và Question Notebook practice. Adaptive core được tách thành `learning-service`/`ai_learning_db`; utility state từng thuộc service hỗ trợ học viên cũ (đã gỡ trong đợt chia service). Content MVP bỏ adaptive topic graph/gate, bỏ vocab↔KP mapping, nhúng question options vào version, gộp asset links và defer package↔vocabulary mapping. Baseline lịch sử: **85 bảng nghiệp vụ + 9 outbox = 94 bảng vật lý**. |
| **V5.1** (2026-09-29, thiết kế đích, chưa có migration) | Học theo chủ đề → bài. Content thêm 5 bảng bài học (§5.13–§5.17); `topics` thêm `required_feature_key`, `test_package_id`; `content_packages.package_type` thay `LESSON` bằng `TOPIC_TEST`; mỗi câu hỏi chỉ gắn KP chính. AI Learning thêm 4 bảng tiến độ học bài (§7.21–§7.24); `topic_progress` quay lại nhưng chỉ lưu học viên đã học tới đâu, mastery vẫn do DeepTutor core giữ. Luật đầy đủ: `plans/260929-1640-lesson-learning-pipeline-mvp/plan.md` (bản gốc `main-learning-pipeline.md` ở git history, commit `6506d5e`). |
| **V5.2** (2026-09-29, thiết kế đích, chưa có migration) | Sửa V5.1 sau red-team. Content: một topic nhiều mã đề qua `content_packages.topic_id` (bỏ `topics.test_package_id`); **giữ** `LESSON` và thêm `TOPIC_TEST`; `knowledge_points` bỏ band. Assessment: không thêm cột; `answer_snapshot` chứa đáp án, `attempt_type` suy từ loại gói. AI Learning: `mastery_paths` một path mỗi học viên; bỏ `mastery_path_knowledge_point_bands`; thêm `path_review_sets` (§7.25), `topic_test_assignments` (§7.26); sửa cột §7.21–§7.23; bài ôn và luyện thêm dùng gói `PRACTICE_SET` câu mới, tính cả bài vừa học; giấu đáp án tới khi đạt. Plan triển khai: `plans/260929-1640-lesson-learning-pipeline-mvp`. |
| **V5.3** (2026-09-30, thiết kế đích, chưa có migration) | Listening đi trọn luồng học. **Không thêm bảng, không thêm cột.** Content: asset `AUDIO` lưu key file ở `media_reference` (content ghép `CONTENT_MEDIA_BASE_URL`, học viên nhận `mediaUrl`), transcript ở `text_content` và giấu tới khi đạt; khối `ASSET` AUDIO trong bài học; section `skill = LISTENING` gắn audio; KP Listening dùng `PROCEDURE`, `kind = STRATEGY`. Assessment: `attempt_sections.section_snapshot` thêm `skill`, `audio`, `solution` (§6.2). AI Learning: chọn gói theo luật V5.2, gói không gắn độ khó (1 gói mỗi KP trong seed). Plan: `plans/260930-0851-listening-topic-audio-lessons`. Chọn gói theo dạng câu và độ khó (cột `path_review_items.wrong_question_types`, mức của gói ở §5.18) **hoãn ngoài MVP** (ý tưởng: `plans/reports/brainstorm-260930-0908-listening-adaptive-path-report.md`). |
| **Chia service** (đã triển khai 2026-10-01) | Library V1 tạo 5 bảng catalog từ vựng/video; library V2 tạo 6 bảng thư viện cá nhân và 4 FK `ON DELETE RESTRICT` tới catalog (§4.3–§4.4, §5.10–§5.12, §8). User V5 tạo `learning_activities`, `streaks` (§8.1–§8.2), không FK tới `users`. Content V7 xóa 5 bảng catalog cũ. Không chép dữ liệu, không tạo outbox ở library; service và DB hỗ trợ học viên cũ đã gỡ. Commit: `465f543`, `0da2eb2`, `d762660`, `fec5d14`, `ab613b1`. |
| **Learning Service** (đã triển khai 2026-10-01) | Bỏ `ai-learning-service` Python và `ai_learning_db`; `learning-service` Java (`learning_db`, Flyway V1) có 9 bảng (§7): tiến độ topic/bài, bài nộp, bài ôn, mã đề, bằng chứng theo user, version kết quả thi, catalog KP. Không chuyển dữ liệu cũ. Content V10–V13: essay Writing Task 2/Task 1, Listening, cột `question_versions.hint`. Plan `261001-1228`. |

---

## 1. Mục đích

Tài liệu này giữ baseline V5 cho kiến trúc DeepTutor và các mục tiêu V5.1–V5.3. Phần chia service đã triển khai: `library-service` giữ catalog và thư viện cá nhân, `user-service` giữ activity/streak; service hỗ trợ học viên cũ đã gỡ. Các số đếm baseline lịch sử bên dưới không phải kiểm kê schema đang chạy.

### Nguyên tắc chung

| Quy tắc | Áp dụng |
| :--- | :--- |
| Database OLTP | PostgreSQL |
| ID nội bộ | Ưu tiên `uuid`; event/sequence có thể dùng `bigint` |
| Thời gian | `timestamptz` |
| Naming | `snake_case` |
| FK cùng service | Có thể dùng FK |
| Cross-service | Chỉ lưu logical ID/reference, không FK xuyên database |
| Event/history | Ưu tiên append-only |
| DeepTutor aggregate | Giữ revision/CAS semantics; không normalize tùy tiện làm mất atomicity |
| AI/RAG vector data | Tách khỏi OLTP khi dùng vector store chuyên dụng |
| Canonical IELTS curriculum, bài học và câu hỏi | Chỉ `content_db` sở hữu; catalog từ vựng/video thuộc `library_db` |
| Formal assessment | Chỉ `assessment_db` sở hữu |

## 1.1 Tổng quan baseline V5

| Nhóm | Số bảng nghiệp vụ |
| :--- | ---: |
| Identity | 8 |
| Plans / Activation Key / Point | 8 |
| Content | **16** |
| Assessment | 10 |
| AI Learning / DeepTutor Core (đã bỏ, thay bằng Learning Service 9 bảng, §7) | **13** |
| Learning Support (baseline lịch sử, đã chia sang User/Library) | **8** |
| Game học tập | 7 |
| Notification | 5 |
| Community | 3 |
| Quiz / Leaderboard | 4 |
| Grading core | 3 |
| **Tổng nghiệp vụ** | **85** |
| Transactional Outbox | **9 bảng vật lý** |
| **Tổng baseline** | **94 bảng** |

Số đếm trên là baseline V5 gốc, chưa gồm các bảng AI Learning thêm sau (§7.18–§7.20), thiết kế V5.1 (+5 bảng Content, +4 bảng AI Learning), V5.2 (+2 bảng AI Learning, −1 bảng AI Learning) hoặc phân bố bảng sau chia service. V5.3 không đổi số bảng.

## 1.2 Các bảng V4 bị xóa

```text
learning_db.mastery_records
learning_db.review_events
learning_db.topic_progress
learning_db.topic_gate_attempts
learning_db.daily_plans
learning_db.daily_tasks
learning_db.mistake_notebook_entries

ai_assistant_db.ai_conversations
ai_assistant_db.ai_messages
ai_assistant_db.outbox_events
```

Không tạo bảng rename tương đương để tiếp tục chạy engine cũ. State adaptive mới nằm trong DeepTutor `LearningProgress` aggregate và các projection/runtime table của DeepTutor.

## 1.3 Source of truth sau V5

```text
content_db
= canonical IELTS topics / KPs / question bank / bài học

library_db
= vocabulary_items / vocabulary_senses / learning_videos / video_segments /
  video_segment_lexical_entries (library V1)
+ flashcard_decks / flashcards / flashcard_deck_items / notes /
  video_learning_progress / saved_video_segments (library V2)

assessment_db
= formal attempts / results / item results / formal error analysis

learning_db
= topic/lesson progress, exercise submissions, reviews, test assignments
+ per-user mastery evidence (kp_evidence) + applied assessment result versions

user_db
= identity / learner profile / learning goal
+ learning_activities / streaks (user V5)
```

---

# 2. Identity — Tài khoản và hồ sơ

## 2.1 `users`

Lưu tài khoản đăng nhập và trạng thái hiện tại của user. Đây là nguồn dữ liệu tài khoản gốc; các domain khác chỉ tham chiếu `user_id` thay vì sao chép thông tin đăng nhập.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `email` | varchar(255) | UNIQUE | Tham chiếu tới đối tượng liên quan. |
| `full_name` | varchar(150) | — | Tham chiếu tới đối tượng liên quan. |
| `phone_number` | varchar(30)? | UNIQUE khi NOT NULL | Số thứ tự/phiên bản. |
| `password_hash` | varchar(255) | — | Tham chiếu tới đối tượng liên quan. |
| `status` | — | — | Giá trị/trạng thái cho phép: `ACTIVE`, `INACTIVE`, `LOCKED`, `PENDING_VERIFY` |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |


## 2.2 `roles`

Danh sách role nghiệp vụ của hệ thống. Role dùng cho authorization và phân quyền chức năng; không dùng để biểu diễn gói Premium hoặc số point.

Các role hiện tại:

| Role | Ý nghĩa |
| :--- | :--- |
| `ADMIN` | Quản trị hệ thống, cấu hình và quản lý nghiệp vụ tổng thể. |
| `CUSTOMER` | Learner/người dùng học tập của hệ thống. |
| `CONTENT_AUTHOR` | Người tạo và quản lý nội dung học, đề, câu hỏi và vocabulary. |
| `EXAMINER` | Người chấm Writing/Speaking theo luồng Human Grading dành cho Premium. |
| `SALES_STAFF` | Nhân viên kinh doanh, quản lý/phát hành key theo nghiệp vụ bán hàng nếu được phân quyền. |

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của role. |
| `name` | varchar(50) | UNIQUE | Mã role, hiện dùng `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`. |
| `description` | varchar(255)? | — | Mô tả quyền/trách nhiệm nghiệp vụ của role. |

## 2.3 `user_roles`

Gán nhiều role cho một user. Bảng nối này cho phép một user có nhiều role và một role được gán cho nhiều user.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `user_id` | uuid | FK → `users` | Định danh user liên quan. |
| `role_id` | uuid | FK → `roles` | Định danh role liên quan. |
| `PK(user_id, role_id)` | — | `PK(user_id, role_id)` | Khóa chính tổng hợp. |


## 2.4 `refresh_tokens`

Lưu refresh token đã hash để duy trì và thu hồi phiên đăng nhập. Nó hỗ trợ duy trì phiên đăng nhập an toàn, rotate/revoke token và đăng xuất chủ động.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | FK → `users` | Định danh user liên quan. |
| `token_hash` | varchar(128) | UNIQUE | Tham chiếu tới đối tượng liên quan. |
| `expires_at` | — | — | Thời điểm hết hạn. |
| `revoked_at?` | — | — | Mốc thời gian, có thể để trống. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


## 2.5 `learner_profiles`

Lưu hồ sơ học tập của learner. Thông tin ở đây phục vụ trải nghiệm học tập và hiển thị hồ sơ, tách khỏi dữ liệu xác thực trong `users`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `user_id PK` | uuid | FK → `users` | Tham chiếu tới đối tượng liên quan. |
| `display_name` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `avatar_reference?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `bio?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `self_reported_band?` | numeric(3,1) | — | Tham chiếu tới đối tượng liên quan. |
| `timezone` | — | — | Múi giờ áp dụng. |
| `profile_visibility` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |


## 2.6 `oauth_identities`

Liên kết user nội bộ với danh tính OAuth như Google. Nó cho phép cùng một tài khoản nội bộ liên kết với Google hoặc provider OAuth khác mà không tạo user trùng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | FK → `users` | Định danh user liên quan. |
| `provider` | — | — | Nhà cung cấp dịch vụ bên ngoài. |
| `provider_subject` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `linked_at` | — | — | Mốc thời gian của sự kiện. |
| `last_authenticated_at?` | — | — | Mốc thời gian, có thể để trống. |
| `UQ(provider, provider_subject)` | — | `UQ(provider, provider_subject)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 2.7 `account_action_tokens`

Token một lần cho quên mật khẩu, xác minh email hoặc thao tác tương tự. Các token một lần được tách riêng để xử lý reset password, verify email và các action nhạy cảm có hạn dùng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | FK → `users` | Định danh user liên quan. |
| `purpose` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `token_hash` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `expires_at` | — | — | Thời điểm hết hạn. |
| `used_at?` | — | — | Mốc thời gian, có thể để trống. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


## 2.8 `learning_goals`

Lưu mục tiêu học của learner. Đây là đầu vào quan trọng cho Adaptive Learning khi xác định target band, ngày thi và thời lượng học phù hợp.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | FK → `users` | Định danh user liên quan. |
| `target_band` | numeric(3,1) | — | Tham chiếu tới đối tượng liên quan. |
| `exam_date?` | date | — | Tham chiếu tới đối tượng liên quan. |
| `available_minutes_per_day` | integer | — | Tham chiếu tới đối tượng liên quan. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `started_at` | — | — | Thời điểm bắt đầu. |
| `ended_at?` | — | — | Mốc thời gian, có thể để trống. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |


Ràng buộc active goal:

```sql
CREATE UNIQUE INDEX uq_learning_goals_one_active_per_user
    ON learning_goals (user_id)
    WHERE status = 'ACTIVE';
```

Mỗi learner có tối đa một goal `ACTIVE`. Khi tạo hoặc kích hoạt goal mới, User Service pause goal active hiện tại trong cùng transaction; unique index xử lý các request cạnh tranh.

---
# 3. Gói, quyền sử dụng, Activation Key và Point

Business rule hiện tại chỉ dùng hai mức truy cập chính: `FREE` và `PREMIUM`. Premium được kích hoạt bằng activation key, không phải thanh toán trực tiếp qua payment gateway. Point dùng cho AI grading là một cơ chế riêng, không phải plan và không lưu trong `users`.

Quy tắc đã chốt:

- Key `POINTS` chỉ cộng point để dùng AI grading; kích hoạt point key **không** cấp Premium, không mở premium content/feature và không cấp human grading.
- Key `PREMIUM` cấp Premium access và human grading credit; Premium **không** miễn point cho AI grading. Nếu user Premium muốn AI chấm thì vẫn phải có point.
- `PREMIUM_30D` cấp 30 ngày Premium + 4 lượt Human Grading.
- `PREMIUM_90D` cấp 90 ngày Premium + 12 lượt Human Grading.
- Khi Premium hết hạn, user mất Premium access và mọi human grading credit chưa sử dụng của subscription đó không còn dùng được.
- Point đã được cộng vào wallet không hết hạn theo thời gian.

## 3.1 `plans`

Lưu các gói truy cập như `FREE`, `PREMIUM`. Bảng định nghĩa các loại gói quyền truy cập như `FREE` và `PREMIUM`, không lưu trạng thái cụ thể của từng user.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của plan. |
| `code` | varchar(50) | UNIQUE | Mã ổn định, ví dụ `FREE`, `PREMIUM`. |
| `name` | varchar(100) | — | Tên hiển thị. |
| `status` | — | — | `ACTIVE`, `INACTIVE`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

## 3.2 `plan_features`

Xác định mỗi plan được phép dùng feature nào. `HUMAN_GRADING` chỉ thể hiện Premium có quyền sử dụng human grading; số lượt thực tế không lấy từ `plan_features` mà được cấp bởi từng Premium key và snapshot vào `subscriptions`. `limit_value` chỉ dành cho các feature quota khác nếu sau này cần. Nó mô tả capability nào thuộc từng plan để backend có thể kiểm tra quyền theo feature thay vì hard-code toàn bộ.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất. |
| `plan_id` | uuid | FK → `plans` | Plan sở hữu feature. |
| `feature_key` | varchar(100) | — | Ví dụ `PREMIUM_CONTENT`, `HUMAN_GRADING`, `ADVANCED_ANALYTICS`. |
| `is_enabled` | boolean | — | Feature có được bật cho plan hay không. |
| `limit_value?` | integer | — | Quota mặc định nếu feature có giới hạn; `NULL` khi không áp dụng hoặc không giới hạn. |
| `config?` | jsonb | — | Cấu hình mở rộng khi feature cần rule phức tạp hơn. |
| `UQ(plan_id, feature_key)` | — | `UQ(plan_id, feature_key)` | Một feature chỉ xuất hiện một lần trong mỗi plan. |

## 3.3 `subscriptions`

Lưu Premium access của user và thời gian hiệu lực. User không cần `is_premium` trong bảng `users`; trạng thái Premium được suy ra từ subscription đang `ACTIVE` và còn hạn. Bảng này trả lời user có Premium trong khoảng thời gian nào và còn bao nhiêu Human Grading Credit trong subscription đó.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh subscription. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User sở hữu subscription. |
| `plan_id` | uuid | FK → `plans` | Plan được cấp, chủ yếu là `PREMIUM`. |
| `status` | — | — | `ACTIVE`, `EXPIRED`, `CANCELLED`. |
| `starts_at` | timestamptz | — | Thời điểm bắt đầu hiệu lực. |
| `ends_at?` | timestamptz | — | Thời điểm hết Premium. |
| `source_type` | — | — | Nguồn cấp quyền, ví dụ `ACTIVATION_KEY`, `ADMIN`. |
| `source_reference_id?` | uuid | Logical ref ↗ `Access.key_activations` | Activation đã tạo/cập nhật subscription. |
| `human_grading_credits_total` | integer | CHECK `>= 0` | Tổng số lượt human grading được cấp cho subscription; không có chế độ unlimited. |
| `human_grading_credits_used` | integer | — | Số lượt human grading đã sử dụng. |
| `cancelled_at?` | timestamptz | — | Thời điểm hủy nếu có. |
| `row_version` | bigint | — | Optimistic locking khi cộng thời hạn/credit hoặc tiêu thụ credit. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

Khi user kích hoạt thêm Premium key trong lúc subscription vẫn còn hiệu lực, cộng thời hạn vào `ends_at` và cộng thêm human grading credit. Giá trị credit thực tế phải được snapshot từ key đã kích hoạt, không phụ thuộc vào việc cấu hình plan thay đổi về sau.

Nếu subscription đã hết hạn rồi mới kích hoạt Premium key mới, tạo subscription mới để giữ lịch sử rõ ràng. Credit còn dư của subscription đã hết hạn không được chuyển sang subscription mới.

### Activation Key và Point

Các bảng activation key và point được đặt cùng phần Plan/Subscription để toàn bộ business rule mua quyền sử dụng nằm cạnh nhau trong tài liệu. Về ownership microservice, `access-service` vẫn sở hữu activation key và point wallet; `user-service` vẫn sở hữu `plans`, `plan_features` và `subscriptions`.

Có hai loại key chính:

- `POINTS`: cộng point dùng cho AI grading; không cấp Premium và không cấp Human Grading.
- `PREMIUM`: cấp/gia hạn Premium, mở premium content/feature và cấp quota Human Grading theo sản phẩm key.

Point không lưu trong `users`. Premium cũng không phải role; quyền Premium được xác định từ subscription.

## 3.4 `key_products`

Định nghĩa sản phẩm mà một activation key đại diện. Bảng này mô tả key sẽ cấp point hay Premium và cấp bao nhiêu. Đây là catalog sản phẩm key như `POINT_50`, `POINT_100`, `PREMIUM_30D`, `PREMIUM_90D` và giá trị mà mỗi loại cấp.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh sản phẩm key. |
| `code` | varchar(100) | UNIQUE | Ví dụ `POINT_50`, `POINT_100`, `PREMIUM_30D`. |
| `name` | varchar(150) | — | Tên hiển thị. |
| `key_type` | — | — | `POINTS`, `PREMIUM`. |
| `points_amount?` | integer | — | Số point cấp nếu là `POINTS`. |
| `plan_id?` | uuid | Logical ref ↗ `Plans.plans` | Plan được cấp nếu là `PREMIUM`. |
| `premium_days?` | integer | — | Số ngày Premium được cộng. |
| `human_grading_credits?` | integer | — | Với `PREMIUM` phải > 0; với `POINTS` không áp dụng. Không có Premium unlimited human grading. |
| `status` | — | — | `ACTIVE`, `INACTIVE`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật. |

Baseline đã chốt:

- `POINT_50` → +50 point, không cấp Premium, không cấp human grading.
- `POINT_100` → +100 point, không cấp Premium, không cấp human grading.
- `PREMIUM_30D` → +30 ngày Premium + 4 lượt Human Grading, không tự cộng AI point.
- `PREMIUM_90D` → +90 ngày Premium + 12 lượt Human Grading, không tự cộng AI point.

Số ngày/credit phải được cấu hình trong `key_products` và snapshot khi activation, không hard-code vào business service.

## 3.5 `activation_keys`

Lưu từng activation key được phát hành. Không lưu raw key dạng plaintext; chỉ lưu hash để đối chiếu khi user nhập key. Mỗi record đại diện cho một key thực tế được phát hành; raw key không nên được lưu trực tiếp mà chỉ lưu hash/hint.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh key. |
| `product_id` | uuid | FK → `key_products` | Sản phẩm mà key đại diện. |
| `code_hash` | varchar(128) | UNIQUE | Hash của raw activation key. |
| `code_hint?` | varchar(20) | — | Một phần không nhạy cảm để support nhận diện key, ví dụ 4 ký tự cuối. |
| `status` | — | — | `ACTIVE`, `REDEEMED`, `REVOKED`, `EXPIRED`. |
| `expires_at?` | timestamptz | — | Hạn kích hoạt key nếu có. |
| `created_by` | uuid | Logical ref ↗ `Identity.users` | Admin tạo/import key. |
| `created_at` | timestamptz | — | Thời điểm phát hành. |
| `redeemed_at?` | timestamptz | — | Thời điểm key đã được sử dụng. |

Một key chỉ được redeem một lần.

## 3.6 `key_activations`

Lịch sử append-only của việc user kích hoạt key. Các giá trị được cấp phải snapshot tại thời điểm activation để lịch sử không thay đổi khi `key_products` được chỉnh sau này. Bảng lưu lịch sử redeem key theo kiểu append-only và snapshot chính xác quyền/point đã cấp tại thời điểm activation.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh activation. |
| `key_id` | uuid | FK → `activation_keys`, UNIQUE | Key đã được redeem; UNIQUE đảm bảo một key chỉ có một activation thành công. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User kích hoạt key. |
| `product_type` | — | — | Snapshot `POINTS` hoặc `PREMIUM`. |
| `points_granted` | integer | — | Số point thực tế đã cấp, mặc định `0`. |
| `premium_days_granted` | integer | — | Số ngày Premium thực tế đã cấp, mặc định `0`. |
| `human_grading_credits_granted` | integer | — | Số lượt human grading thực tế đã cấp, mặc định `0`. |
| `activated_at` | timestamptz | — | Thời điểm activation thành công. |
| `idempotency_key` | varchar(255) | UNIQUE | Chống xử lý lặp cùng một yêu cầu activation. |

## 3.7 `point_wallets`

Projection số dư point hiện tại của user. Một user có tối đa một point wallet. Point đã mua/kích hoạt không có ngày hết hạn; balance chỉ thay đổi bởi ledger transaction hợp lệ như cộng key, AI grading debit, refund hoặc admin adjustment. Đây là projection số dư point hiện tại để đọc nhanh khi kiểm tra đủ point trước AI grading.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `user_id` | uuid | PK, Logical ref ↗ `Identity.users` | User sở hữu ví. |
| `balance` | bigint | CHECK `balance >= 0` | Số point hiện có. |
| `total_credited` | bigint | — | Tổng point từng được cộng. |
| `total_debited` | bigint | — | Tổng point từng bị trừ. |
| `row_version` | bigint | — | Optimistic locking khi cộng/trừ point. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

## 3.8 `point_ledger_entries`

Lịch sử append-only cho mọi biến động point. `point_wallets` trả lời user còn bao nhiêu point; ledger trả lời vì sao balance thay đổi. Ledger lưu mọi biến động cộng/trừ/refund point để có thể audit và chống trừ point trùng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh ledger entry. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User bị tác động. |
| `delta` | bigint | — | Số point thay đổi, ví dụ `+50`, `-3`, `-5`. |
| `balance_after` | bigint | — | Balance ngay sau transaction. |
| `transaction_type` | — | — | `KEY_CREDIT`, `AI_GRADING_DEBIT`, `AI_GRADING_REFUND`, `ADMIN_ADJUSTMENT`. |
| `reference_type` | — | — | Loại nghiệp vụ nguồn, ví dụ `KEY_ACTIVATION`, `GRADING_JOB`. |
| `reference_id` | uuid | — | ID nghiệp vụ nguồn. |
| `idempotency_key` | varchar(255) | UNIQUE | Chống trừ/cộng point trùng do retry. |
| `description?` | text | — | Mô tả bổ sung nếu cần. |
| `created_at` | timestamptz | — | Thời điểm phát sinh. |

### Luồng activation chính

```text
Learner nhập key
      ↓
access-service
      ↓
activation_keys
      ↓
key_activations
      ↓
 ┌───────────────┬────────────────────┐
 │ POINTS        │ PREMIUM            │
 ↓               ↓
point_wallets    PremiumActivated /
+ ledger         PremiumExtended event
                 ↓
              user-service
                 ↓
              subscriptions
```

Khi activate key `POINTS`, việc đổi trạng thái key, tạo `key_activations`, cập nhật `point_wallets`, ghi `point_ledger_entries` và ghi outbox event phải nằm trong cùng transaction của `access_db`.

Khi activate key `PREMIUM`, `access-service` ghi activation và publish event. `user-service` consume event theo cách idempotent để tạo/gia hạn `subscriptions` và cộng human grading credit.

---

---
# 4. Content curriculum và Library vocabulary catalog

`topics`, `knowledge_points` (§4.1–§4.2) thuộc `content_db`; `vocabulary_items`, `vocabulary_senses` (§4.3–§4.4) thuộc `library_db` từ library V1. Content V7 đã xóa hai bảng từ vựng cũ.

## 4.1 `topics`

Lưu taxonomy chủ đề chuẩn của IELTSPath để phân loại content. `topics` chỉ mô tả nội dung/curriculum; nó **không** biểu diễn prerequisite hay trạng thái học của từng học viên. Từ V5.1: `sort_order` là thứ tự học ban đầu, giống mọi học viên; thứ tự hiện tại và việc mở khóa theo từng học viên do `learning-service` quyết định. V5.2: thứ tự học chỉ gồm topic có ≥ 1 bài `PUBLISHED` và ≥ 1 gói `TOPIC_TEST` `PUBLISHED`, theo `sort_order`, lưu theo học viên ở `topic_progress.sequence_order` (§7.21); xếp lại thứ tự trong cùng `parent_topic_id` hoãn khỏi MVP.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `parent_topic_id?` | uuid | FK → `topics` | Tham chiếu tới đối tượng liên quan. |
| `code` | — | — | Mã nghiệp vụ ổn định. |
| `name` | — | — | Tên hiển thị. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `band_min?` | numeric(2,1) | CHECK 0.0–9.0, bội số 0.5, `≤ band_max` | Band IELTS thấp nhất mà topic hướng tới. NULL = không giới hạn dưới. |
| `band_max?` | numeric(2,1) | CHECK 0.0–9.0, bội số 0.5 | Band IELTS cao nhất mà topic hướng tới. NULL = không giới hạn trên. |
| `required_feature_key?` | varchar(100) | Logical ref ↗ `Access.plan_features.feature_key` | V5.1. Topic trả phí; `NULL` = miễn phí. Premium tính theo topic: danh sách vẫn hiện topic, nội dung bài bị chặn. V5.2: chưa dùng trong MVP (premium hoãn). |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

Khoảng band là metadata nội dung, không phải trạng thái học, chỉ để hiển thị. Cả hai đầu NULL nghĩa là "mọi band". Từ V5.1 path không dùng band: không lọc topic theo band mục tiêu của goal, không test-out từ placement.

V5.2 thay: bỏ `test_package_id` của V5.1. Một topic có nhiều mã đề; mỗi gói `TOPIC_TEST` trỏ về topic qua `content_packages.topic_id` (§5.1).


## 4.2 `knowledge_points`

Định nghĩa canonical knowledge point trong curriculum. `content-service` chỉ sở hữu định nghĩa và metadata của KP; `learning-service` map các KP này sang DeepTutor `KnowledgePoint` để theo dõi mastery và quyết định adaptive learning.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `topic_id` | uuid | FK → `topics` | Định danh topic liên quan. |
| `code` | — | — | Mã nghiệp vụ ổn định. |
| `name` | — | — | Tên hiển thị. |
| `learning_type` | varchar(20) | CHECK | DeepTutor `KnowledgeType`: `MEMORY`, `CONCEPT`, `PROCEDURE`, `DESIGN`. Đây là type dùng cho mastery/policy trong `learning-service`. |
| `skill?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `description` | — | — | Mô tả chi tiết. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

V5.2: KP không còn band (bỏ `band_min`, `band_max` và luật "band hiệu lực"). API không trả `bandMin`, `bandMax`, `effectiveBandMin`, `effectiveBandMax`. Band chỉ còn ở `topics` để hiển thị.

`learning_type` phải map 1:1 sang DeepTutor `KnowledgeType`. Các category nghiệp vụ như Grammar/Vocabulary/Strategy (nếu bổ sung sau) chỉ là metadata phân loại content, không thay thế `learning_type`.

`learning_type` là `MEMORY | CONCEPT | PROCEDURE | DESIGN`, ánh xạ 1:1 sang DeepTutor `KnowledgeType`; không suy ra từ `kind`. Trạng thái `ACTIVE` yêu cầu `learning_type IS NOT NULL`. Migration V2 chuyển các Knowledge Point active chưa được phân loại sang `INACTIVE`, giữ nguyên dữ liệu và chờ content editor phân loại trước khi publish lại.

V5.3: KP đo bằng câu tự chấm (Listening, Reading) nên là `MEMORY` hoặc `PROCEDURE`. DeepTutor coi `CONCEPT`/`DESIGN` là đạt chỉ khi tutor chấm một lời giải thích (`mastery_assess`), nên làm bài đúng bao nhiêu cũng không hiện "đã nắm".

## 4.3 `vocabulary_items`

Lưu từ/lemma ở mức từ vựng gốc. Một `vocabulary_item` có thể có nhiều nghĩa và nhiều loại từ thông qua `vocabulary_senses`. Bảng giữ identity chung của một từ/cụm từ; các nghĩa và part of speech cụ thể được tách sang `vocabulary_senses`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh từ vựng. |
| `lemma` | varchar(255) | — | Dạng từ chuẩn, ví dụ `record`, `abandon`. |
| `normalized_lemma` | varchar(255) | INDEX | Dạng chuẩn hóa phục vụ tìm kiếm. |
| `ipa?` | varchar(255) | — | Phiên âm IPA nếu có. |
| `pronunciation_audio_reference?` | text | — | URL/reference tới file âm thanh phát âm trong object storage. |
| `status` | — | — | `ACTIVE`, `INACTIVE`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

Vocabulary được lưu hoàn toàn trong database nội bộ; runtime không phụ thuộc Dictionary/Translation API. File audio không lưu binary trong PostgreSQL, database chỉ lưu URL/reference tới object storage.

## 4.4 `vocabulary_senses`

Lưu từng nghĩa/loại từ của một từ. Một từ có thể có nhiều `sense`, kể cả nhiều nghĩa cùng một part of speech.

Mỗi sense hiện tại có **một ví dụ** và có thể có **một ảnh minh họa lưu bằng URL**; không cần tách bảng `vocabulary_examples` trong MVP. Mỗi record biểu diễn một nghĩa/từ loại cụ thể, kèm nghĩa tiếng Việt, ví dụ, ảnh minh họa và dữ liệu phát âm cần thiết.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh nghĩa cụ thể. |
| `vocabulary_item_id` | uuid | FK → `vocabulary_items` | Từ gốc sở hữu sense này. |
| `part_of_speech` | varchar(50) | — | Loại từ như `NOUN`, `VERB`, `ADJECTIVE`, `ADVERB`. |
| `english_definition?` | text | — | Định nghĩa tiếng Anh. |
| `vietnamese_meaning` | text | — | Nghĩa tiếng Việt của sense. |
| `example_sentence` | text | — | Một câu ví dụ cho nghĩa/loại từ này. |
| `image_url?` | text | — | URL ảnh minh họa; chỉ lưu URL, không lưu binary. |
| `sort_order` | integer | — | Thứ tự hiển thị các sense của cùng một từ. |
| `status` | — | — | `ACTIVE`, `INACTIVE`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

Ví dụ từ `record` có thể có một sense `NOUN` với nghĩa "bản ghi" và một sense `VERB` với nghĩa "ghi lại"; mỗi sense có ví dụ riêng.

# 5. Content — Đề, bài học, câu hỏi; Library — video catalog

Content sở hữu §5.1–§5.9 và §5.13–§5.18. `learning_videos`, `video_segments`, `video_segment_lexical_entries` (§5.10–§5.12) thuộc `library_db` từ library V1; content V7 đã xóa ba bảng video cũ. Số mục giữ nguyên để không làm gãy các tham chiếu nội bộ.

## 5.1 `content_packages`

Đại diện cho một bộ nội dung hoàn chỉnh như mock test, placement test, practice set, quiz hoặc đề cuối topic. V5.1 thêm `TOPIC_TEST`. V5.2 **giữ** `LESSON` (V5.1 định bỏ; luồng đọc bài của tutor còn dùng; bài học mới dùng bảng riêng §5.13). V5.2: `PRACTICE_SET` có ≥ 3 câu là ngân hàng câu cho bài ôn và luyện thêm (§7.25), không trùng câu với bài học hay đề cuối của cùng topic; `TOPIC_TEST` chỉ tạo bằng seed, không qua API. Content không sở hữu khái niệm plan `FREE/PREMIUM`; nếu package cần entitlement thì chỉ khai báo `required_feature_key`, còn quyết định user có quyền truy cập thuộc `access-service`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `code` | — | — | Mã nghiệp vụ ổn định. |
| `title` | — | — | Tiêu đề. |
| `package_type` | varchar(50) | CHECK `MOCK_TEST`/`PLACEMENT_TEST`/`PRACTICE_SET`/`QUIZ`/`LESSON`/`TOPIC_TEST` | Loại package. `TOPIC_TEST` là một mã đề cuối của topic. |
| `topic_id?` | uuid | FK → `topics`; CHECK bắt buộc khi `package_type = 'TOPIC_TEST'` | V5.2. Topic mà mã đề thuộc về. Một topic có nhiều mã đề; learning-service giao mã đề cho học viên (§7.26). |
| `required_feature_key?` | varchar(100) | Logical ref ↗ `Access.plan_features.feature_key` | Feature cần có để truy cập package; `NULL` nếu không có feature gate riêng. Không FK xuyên service. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `current_published_version_id?` | — | — | Phiên bản đang publish. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

## 5.2 `content_package_versions`

Lưu từng phiên bản của một content package. Version đã publish không sửa trực tiếp. Versioning giúp nội dung đã publish không bị sửa ngược và attempt cũ luôn biết mình đã dùng phiên bản nào.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `package_id` | uuid | FK → `content_packages` | Định danh content package liên quan. |
| `version_number` | — | — | Số thứ tự/phiên bản. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `rules` | jsonb | — | Chỉ chứa delivery/assessment configuration như shuffle, navigation mode, timer/section behavior. Không chứa mastery threshold, topic unlock, prerequisite, adaptive progression, review schedule hoặc `next_objective` rule. |
| `schema_version` | integer | — | Phiên bản schema của payload. |
| `published_at?` | — | — | Thời điểm publish nếu đã publish. |
| `published_by?` | uuid | Logical ref ↗ `Identity.users` | Tham chiếu tới đối tượng liên quan. |
| `UQ(package_id, version_number)` | — | `UQ(package_id, version_number)` | Ràng buộc duy nhất cho tổ hợp cột. |

Invariant: `rules` không phải adaptive policy store. Mọi mastery gate, prerequisite học tập, retention/review scheduling và quyết định `next_objective()` thuộc `learning-service` / DeepTutor.

## 5.3 `content_sections`

Chia package thành section như Reading, Listening hoặc từng passage/part. Bảng chia một package version thành các phần có thứ tự, thời lượng và instruction riêng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `package_version_id` | uuid | FK → `content_package_versions` | Định danh phiên bản package liên quan. |
| `title` | — | — | Tiêu đề. |
| `skill?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `time_limit_seconds?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `instructions` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `UQ(package_version_id, sort_order)` | — | `UQ(package_version_id, sort_order)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 5.4 `questions`

ID ổn định của một câu hỏi qua nhiều lần chỉnh sửa; nội dung thật nằm trong `question_versions`. Nếu cần gate một question khi tái sử dụng độc lập, Content chỉ khai báo `required_feature_key`; entitlement do `access-service` quyết định.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `question_type` | — | — | Loại câu hỏi. |
| `skill?` | — | — | Kỹ năng liên quan. |
| `required_feature_key?` | varchar(100) | Logical ref ↗ `Access.plan_features.feature_key` | Feature cần có để truy cập question khi áp dụng; `NULL` nếu không có gate riêng. Không FK xuyên service. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `current_published_version_id?` | — | — | Phiên bản đang publish. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

Content không tự suy luận hierarchy entitlement giữa package và question. Khi resource có `required_feature_key`, quyền truy cập được kiểm tra qua `access-service`; Content chỉ giữ requirement key.

## 5.5 `question_versions`

Lưu nội dung thực tế của từng phiên bản câu hỏi. Bảng lưu nội dung câu hỏi tại từng version để publish/versioning và snapshot assessment hoạt động nhất quán.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `question_id` | uuid | FK → `questions` | Định danh câu hỏi liên quan. |
| `version_number` | — | — | Số thứ tự/phiên bản. |
| `stem` | — | — | Nội dung câu hỏi/prompt của version. |
| `options?` | jsonb | — | Danh sách lựa chọn/phần tử ghép của các question type cần option; thuộc immutable question version, không tách bảng riêng. |
| `answer_spec` | jsonb | — | Đáp án chuẩn/rule chấm của question version. |
| `schema_version` | integer | — | Phiên bản schema của payload. |
| `explanation` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `difficulty?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `UQ(question_id, version_number)` | — | `UQ(question_id, version_number)` | Ràng buộc duy nhất cho tổ hợp cột. |

`answer_spec` theo dạng câu tự chấm (V5.1; contract ghi vào `docs/contracts/` khi triển khai, dùng chung cho game, assessment, learning-service):

| `question_type` | `options` | `answer_spec` | Đúng khi |
| :--- | :--- | :--- | :--- |
| `MULTIPLE_CHOICE` | `[{optionKey, content, sortOrder}]` | `{"correct":"A"}` | Chọn đúng `optionKey`. |
| `TRUE_FALSE_NOT_GIVEN` | NULL | `{"correct":"NOT_GIVEN"}` | Chọn đúng `TRUE`, `FALSE` hoặc `NOT_GIVEN`. |
| `FILL_IN_BLANK`, `SHORT_ANSWER` | NULL | `{"accepted":["two decades","20 years"]}` | Khớp một đáp án sau khi bỏ phân biệt hoa thường và khoảng trắng thừa; sai chính tả là sai. |
| `MATCHING` | Danh sách heading dùng chung | `{"correct":"iii"}` | Mỗi câu chỉ một cặp (một đoạn ↔ một heading); một câu một điểm. |


## 5.6 `section_questions`

Gắn một question version vào vị trí cụ thể trong section. Bảng nối quyết định câu hỏi/version nào xuất hiện ở vị trí nào trong một section cụ thể.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `section_id` | uuid | FK → `content_sections` | Định danh section liên quan. |
| `question_version_id` | uuid | FK → `question_versions` | Định danh phiên bản câu hỏi liên quan. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `max_score` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `UQ(section_id, sort_order)` | — | `UQ(section_id, sort_order)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 5.7 `question_knowledge_points`

Gắn câu hỏi với knowledge point mà nó kiểm tra. Mapping này cho biết một câu hỏi đang đánh giá knowledge point nào và trọng số đóng góp của từng point.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `question_version_id` | uuid | FK → `question_versions` | Định danh phiên bản câu hỏi liên quan. |
| `knowledge_point_id` | uuid | FK → `knowledge_points` | Định danh đối tượng `knowledge_point` liên quan. |
| `weight` | numeric(5,2) | mặc định 1.00 | Trọng số khai báo. Engine mastery hiện không dùng giá trị này. |
| `PK(question_version_id, knowledge_point_id)` | — | `PK(question_version_id, knowledge_point_id)` | Khóa chính tổng hợp. |

V5.1: mỗi câu chỉ gắn KP mà nó thật sự đo, `weight` để 1.00. Engine mastery (`compute_mastery`) chỉ nhận đúng/sai: mỗi cặp (câu, KP) được tính là một lần làm đầy đủ, nên gắn thêm KP phụ sẽ làm mastery KP đó lệch.


## 5.8 `content_assets`

Passage của section Reading được đọc qua `GET /api/content/reading/sections/{sectionId}`: chỉ section `READING` thuộc
version đang publish của gói `PRACTICE_SET` hoặc `LESSON`; text của các asset `PASSAGE` (theo `sort_order` của link) được
chia đoạn theo dòng trống và gán nhãn A, B, C... Mọi trường hợp khác trả 404.

Lưu passage text hoặc metadata của audio/hình ảnh/media. Bảng quản lý passage, audio, image và media metadata dùng chung cho content mà không cần lưu binary trực tiếp trong PostgreSQL.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `asset_type` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `text_content?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `media_reference?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `duration_seconds?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `checksum` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `validation_status` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


File binary không nhất thiết lưu trực tiếp trong DB.


## 5.9 `content_asset_links`

Gắn `content_assets` vào **section** hoặc **question version**. Một bảng link chung thay cho hai bảng `section_assets` và `question_assets`, vì cả hai đều biểu diễn cùng một quan hệ: asset được dùng bởi một content owner cụ thể.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh link. |
| `asset_id` | uuid | FK → `content_assets` | Asset được sử dụng. |
| `section_id?` | uuid | FK → `content_sections` | Có giá trị khi asset thuộc một section. |
| `question_version_id?` | uuid | FK → `question_versions` | Có giá trị khi asset thuộc một question version. |
| `sort_order` | integer | DEFAULT 0 | Thứ tự hiển thị/xử lý asset trong owner. |
| `created_at` | timestamptz | — | Thời điểm tạo mapping. |

Constraint bắt buộc:

```text
exactly one of:
- section_id
- question_version_id
```

Không cho phép cả hai cùng `NULL` hoặc cùng có giá trị. Có unique constraint/index riêng cho `(section_id, asset_id)` và `(question_version_id, asset_id)` khi cột owner tương ứng không `NULL`.

`topic_prerequisites` và `topic_gate_rules` **không còn trong V5**. IELTSPath không duy trì một topic-unlock engine song song; thứ tự objective, mastery gate, review và `next_objective()` thuộc DeepTutor trong `learning-service`.

`package_lexical_entries` cũng chưa đưa vào MVP. Nếu sau này có feature "Vocabulary in this lesson" hoặc pre-learn vocabulary theo package, mapping package ↔ vocabulary sense sẽ được bổ sung khi business flow đó được chốt.

## 5.10 `learning_videos`

Lưu metadata của video học tập được IELTSPath chọn từ YouTube. Hệ thống **không lưu file video**, không transcode và không stream video; YouTube chịu trách nhiệm phát video. IELTSPath chỉ lưu tham chiếu YouTube và metadata phục vụ học tập.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh video học tập trong IELTSPath. |
| `youtube_video_id` | varchar(32) | UNIQUE | ID video YouTube dùng trực tiếp cho YouTube IFrame Player API. |
| `youtube_url` | varchar(500) | — | URL YouTube gốc để quản trị/đối chiếu. |
| `title` | varchar(255) | — | Tiêu đề video hiển thị trong hệ thống. |
| `description?` | text | — | Mô tả ngắn về nội dung học. |
| `thumbnail_url?` | varchar(500) | — | Thumbnail dùng cho danh sách video. |
| `duration_seconds?` | integer | CHECK >= 0 | Thời lượng video nếu đã biết. |
| `topic_id?` | uuid | Logical ref ↗ `Content.topics`, không FK xuyên DB | Topic chính liên quan tới video. Khi ghi video, library kiểm tra bằng `GET /api/content/topics/{id}`. |
| `level?` | varchar(20) | — | Mức độ nội dung, ví dụ `A2`, `B1`, `B2`, `C1`. |
| `required_feature_key?` | varchar(100) | Logical ref ↗ `Access.plan_features.feature_key` | Feature cần có để truy cập video; `NULL` nếu không có gate riêng. Không FK xuyên service. |
| `status` | varchar(30) | — | Trạng thái như `DRAFT`, `PUBLISHED`, `ARCHIVED`. |
| `created_by` | uuid | Logical ref ↗ `Identity.users` | CONTENT_AUTHOR/ADMIN tạo record video. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

Ví dụ:

```text
learning_videos
youtube_video_id = "abc123"
title = "How Climate Change Affects Cities"
topic = Environment
level = B2
required_feature_key = VIDEO_LEARNING_PREMIUM
```

Frontend dùng `youtube_video_id` với YouTube IFrame Player API để play, pause, seek và đọc `currentTime`.

## 5.11 `video_segments`

Chia một YouTube video thành các đoạn transcript/subtitle có mốc thời gian. Đây là bảng nền cho subtitle đồng bộ, replay/loop segment, Dictation, Shadowing và lưu câu/đoạn để học lại.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh segment. |
| `video_id` | uuid | FK → `learning_videos` | Video chứa segment. |
| `sequence_no` | integer | CHECK > 0 | Thứ tự segment trong video. |
| `start_ms` | integer | CHECK >= 0 | Mốc bắt đầu theo millisecond. |
| `end_ms` | integer | CHECK > `start_ms` | Mốc kết thúc theo millisecond. |
| `transcript` | text | — | Transcript tiếng Anh của segment. |
| `translation_vi?` | text | — | Bản dịch tiếng Việt nếu content author cung cấp. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |
| `UQ(video_id, sequence_no)` | — | UNIQUE | Không trùng thứ tự segment trong cùng video. |

Ví dụ:

```text
00:10.000 → 00:15.000
Climate change is affecting cities around the world.
```

Frontend đọc `currentTime` từ YouTube, tìm segment có `start_ms <= currentTime < end_ms` rồi highlight subtitle tương ứng.

## 5.12 `video_segment_lexical_entries`

Gắn các **từ hoặc cụm từ đáng học** xuất hiện trong một video segment với kho vocabulary của IELTSPath. Không lưu mọi token trong subtitle thành row; chỉ lưu lexical entry mà UI cần cho tương tác học từ/cụm từ.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh mapping lexical entry. |
| `segment_id` | uuid | FK → `video_segments` | Segment chứa từ/cụm từ. |
| `vocabulary_sense_id?` | uuid | FK → `vocabulary_senses` | Nghĩa cụ thể trong vocabulary nếu entry đã được map. |
| `surface_text` | varchar(255) | — | Text thực tế xuất hiện trong subtitle, có thể là một từ hoặc cụm từ. |
| `start_char` | integer | CHECK >= 0 | Vị trí bắt đầu trong `transcript`. |
| `end_char` | integer | CHECK > `start_char` | Vị trí kết thúc trong `transcript`. |
| `sort_order?` | integer | — | Thứ tự lexical entry trong segment nếu cần. |
| `created_at` | timestamptz | — | Thời điểm tạo mapping. |
| `UQ(segment_id, start_char, end_char)` | — | UNIQUE | Không đánh dấu trùng cùng một vùng text trong segment. |

Ví dụ:

```text
"We need to take climate change into account."

lexical entries:
- climate change
- take into account
```

Khi learner click lexical entry, frontend có thể mở `vocabulary_sense` tương ứng rồi cho phép **Add to Flashcard**.

### Video comprehension trong V4

V4 **không có bảng `video_questions`**. Baseline hiện tại chỉ chốt Watch/Subtitle, interactive vocabulary, saved segment, Dictation và Shadowing. Nếu sau này bổ sung comprehension question, hệ thống sẽ ưu tiên tái sử dụng `questions` và `question_versions` (options nằm trong `question_versions.options`) rồi chỉ thiết kế mapping video ↔ question khi business flow được chốt.

## 5.13 `lessons` (V5.1)

Một bài học trong topic. Học viên học các bài theo `sort_order`; bài sau mở khi bài trước hoàn thành (luật và tiến độ ở `learning-service`). Bài học không versioning.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh bài. |
| `topic_id` | uuid | FK → `topics` | Topic sở hữu bài; mỗi bài thuộc đúng một topic. |
| `code` | varchar(100) | UNIQUE | Mã nghiệp vụ ổn định. |
| `title` | varchar(255) | NOT NULL | Tên bài. |
| `summary?` | text | — | Mô tả ngắn, mục tiêu hiển thị. |
| `sort_order` | integer | NOT NULL | Thứ tự học trong topic. |
| `status` | varchar(20) | CHECK `DRAFT`/`PUBLISHED`/`ARCHIVED` | Chỉ bài `PUBLISHED` xuất hiện với học viên. |
| `created_at`, `updated_at` | timestamptz | NOT NULL | |
| `UQ(topic_id, sort_order)` | — | — | Mỗi vị trí trong topic một bài. |

Thêm bài mới vào topic học viên đã `PASSED` không khóa lại topic; bài mới hiện `AVAILABLE`, không bắt buộc.

## 5.14 `lesson_blocks` (V5.1)

Thân bài: chuỗi khối theo thứ tự. Bài không chép đoạn văn, từ vựng hay câu hỏi; mỗi khối chỉ giữ thứ tự và tham chiếu. Chỉ khối `TEXT` tự chứa nội dung.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh khối. |
| `lesson_id` | uuid | FK → `lessons` ON DELETE CASCADE | Bài chứa khối. |
| `sort_order` | integer | NOT NULL | Thứ tự hiển thị trong bài. |
| `block_type` | varchar(20) | CHECK `TEXT`/`ASSET`/`VOCABULARY`/`EXERCISE` | Loại khối. |
| `text_content?` | text | — | Markdown của khối `TEXT` (bắt buộc); hướng dẫn tùy chọn của khối `EXERCISE`. |
| `asset_id?` | uuid | FK → `content_assets` | Bắt buộc với khối `ASSET` (`PASSAGE`, `IMAGE`, `AUDIO`). |
| `created_at`, `updated_at` | timestamptz | NOT NULL | |
| `UQ(lesson_id, sort_order)` | — | — | |

CHECK theo `block_type`: `TEXT` cần `text_content` và không có `asset_id`; `ASSET` cần `asset_id`; `VOCABULARY` không có cả hai; `EXERCISE` không có `asset_id`. Nội dung khối `VOCABULARY`, `EXERCISE` nằm ở §5.15, §5.16.

## 5.15 `lesson_block_vocabulary` (V5.1)

Các từ trong một khối `VOCABULARY`. Nghĩa, ví dụ, phát âm nằm ở `vocabulary_senses`; "thêm vào flashcard" dùng đúng `vocabulary_sense_id` này.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `block_id` | uuid | FK → `lesson_blocks` ON DELETE CASCADE | Khối từ vựng. |
| `vocabulary_sense_id` | uuid | Logical ref ↗ `Library.vocabulary_senses`, không FK xuyên DB | Nghĩa từ được học. |
| `sort_order` | integer | NOT NULL | Thứ tự trong khối. |
| `PK(block_id, vocabulary_sense_id)` | — | — | |

## 5.16 `lesson_block_questions` (V5.1)

Các câu trong một khối `EXERCISE`, theo thứ tự. Học viên nộp và được chấm theo từng khối; bài hoàn thành khi mỗi khối bài tập có một lần nộp ≥ 70%. Câu hỏi không có `lesson_id`: bài biết câu nào qua bảng này, không suy qua KP.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `block_id` | uuid | FK → `lesson_blocks` ON DELETE CASCADE | Khối bài tập. |
| `question_version_id` | uuid | FK → `question_versions` | Ghim version đã publish; câu được sửa thì khối vẫn trỏ version cũ tới khi author đổi. |
| `sort_order` | integer | NOT NULL | Thứ tự câu trong khối. |
| `PK(block_id, question_version_id)` | — | INDEX(`question_version_id`) | Index để biết một câu đang được bài nào dùng. |

Luật kiểm ở use case: chỉ câu tự chấm (bảng `answer_spec` ở §5.5); câu thuộc đề cuối (`TOPIC_TEST`) hoặc gói luyện tập (`PRACTICE_SET`) của cùng topic không được gắn vào khối bài tập, và ngược lại (V5.2 thêm gói luyện tập).

## 5.17 `lesson_knowledge_points` (V5.1)

Bài dạy KP nào. `learning-service` dùng bảng này để tìm bài ôn khi KP yếu và để hiện mục tiêu của bài.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `lesson_id` | uuid | FK → `lessons` ON DELETE CASCADE | Bài dạy. |
| `knowledge_point_id` | uuid | FK → `knowledge_points` | KP được dạy. |
| `PK(lesson_id, knowledge_point_id)` | — | INDEX(`knowledge_point_id`) | Index cho câu hỏi "KP này do bài nào dạy". |

KP phải cùng topic với bài (kiểm ở use case). Một bài dạy được nhiều KP; một KP có thể được nhiều bài dạy. V5.2: learning-service chép danh sách KP của bài vào `lesson_progress.knowledge_point_ids` (§7.22) mỗi khi học viên mở hoặc nộp bài, để tìm bài dạy KP mà không gọi Content.

## 5.18 Listening và độ khó của gói (V5.3)

Không thêm bảng hay cột. Listening dùng lại các bảng content hiện có:

| Bảng | Dữ liệu Listening |
| :--- | :--- |
| `topics` | Topic Listening, vào thứ tự học như topic khác (seed demo: `DEMO_LISTENING`, `sort_order` 920, sau `TFNG_SKILLS`). |
| `knowledge_points` | KP theo kỹ năng con, `skill = LISTENING`, `learning_type = PROCEDURE`: `LS_NUM` (số, ngày, giờ, giá), `LS_SPELL` (đánh vần tên, địa chỉ, mã), `LS_PARA` (bắt ý qua paraphrase), `LS_TRAP` (tránh bẫy đổi ý). |
| `lessons`, `lesson_blocks`, `lesson_block_questions`, `lesson_knowledge_points` | Bài nghe: khối `TEXT` → khối `ASSET` (asset `AUDIO`) → khối `EXERCISE`. |
| `questions`, `question_versions`, `question_knowledge_points` | Câu `FILL_IN_BLANK` (form, note completion) và `MULTIPLE_CHOICE`, chấm theo `answer_spec` như Reading; mỗi câu 1 KP chính; câu trong gói luyện có `difficulty`. |
| `content_assets` | `asset_type = AUDIO`: `media_reference` = key file, `duration_seconds`, `text_content` = transcript. |
| `content_packages`, `content_package_versions`, `content_sections`, `section_questions`, `content_asset_links` | Gói `PRACTICE_SET` theo mức và mã `TOPIC_TEST`; section `skill = LISTENING` gắn một asset AUDIO qua `content_asset_links.section_id`. |

Luật:

- **`media_reference` của AUDIO:** key (không scheme, không `..`, không bắt đầu bằng `/`) hoặc URL `https://`. Content ghép key với
  `CONTENT_MEDIA_BASE_URL` khi trả ra; DB không lưu URL bucket, nên đổi bucket chỉ cần đổi env. File mp3 nằm trên object storage
  (MVP: bucket public-read), không lưu binary trong DB.
- **Transcript là đáp án:** `text_content` của AUDIO chỉ đi qua endpoint nội bộ `/internal/learning-content/*`.
  `GET /api/content/assets/{id}` chỉ cho `ADMIN`, `CONTENT_AUTHOR`. Học viên thấy transcript khi bài xong (§7.3), khi gói
  đạt ≥ 70% (§7.6) hoặc khi đề đạt ≥ 70% (§6.2).
- **Mức của gói:** mức cao nhất trong `question_versions.difficulty` của các câu trong gói (`EASY < MEDIUM < HARD`); câu không
  có `difficulty` tính là `MEDIUM`. Endpoint nội bộ tìm gói và package version trả kèm `difficulty`; tìm gói trả thêm
  `questionTypes` (các `questions.question_type` khác nhau trong gói). Content chỉ trả metadata; chọn gói là việc của
  learning-service (§7.6).
- **Dạng câu Listening:** 6 dạng IELTS đều biểu diễn được bằng loại câu sẵn có: multiple choice → `MULTIPLE_CHOICE`; matching
  và plan/map/diagram labelling → `MATCHING` (ảnh qua `content_asset_links`); form/note/table/flow-chart/summary completion
  và sentence completion → `FILL_IN_BLANK`; short answer → `SHORT_ANSWER`. Dạng "chọn HAI đáp án" chưa có trong
  `answer-spec-v1`.
- **Luật soạn gói luyện:** mọi câu trong một gói cùng KP và cùng mức. Audio theo mức: `EASY` một người nói, chậm và rõ, không
  bẫy; `MEDIUM` hai người nói, tốc độ thường, một bẫy; `HARD` nhanh hơn, giọng khác, từ hai bẫy trở lên.

---
# 6. Assessment — Làm bài, nộp bài và kết quả

## 6.1 `assessment_attempts`

Một lần learner làm placement, mock, practice hoặc quiz. Placement test vẫn áp dụng quy tắc tính point cho phần Writing/Speaking vì hai kỹ năng này được AI chấm. Đây là record cha cho một lần learner làm placement, mock, practice hoặc quiz và giữ lifecycle của cả attempt.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Định danh user liên quan. |
| `package_version_id` | uuid | Logical ref ↗ `Content` | Định danh phiên bản package liên quan. |
| `attempt_type` | — | — | V5.2: assessment suy từ loại gói (`TOPIC_TEST` → `TOPIC_GATE`, `MOCK_TEST` → `MOCK`, `PLACEMENT_TEST` → `PLACEMENT`, `QUIZ` → `QUIZ`); `PRACTICE_SET`, `LESSON` bị từ chối. Không nhận từ app. |
| `mode` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `channel` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `started_at?` | — | — | Thời điểm bắt đầu nếu có. |
| `submitted_at?` | — | — | Thời điểm submit nếu có. |
| `expires_at?` | — | — | Thời điểm hết hạn nếu áp dụng. V5.2: lấy từ luật của package version, không nhận từ app. |
| `row_version` | bigint | — | Version dùng cho optimistic locking. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |


## 6.2 `attempt_sections`

Các section thực tế trong một attempt. Bảng materialize các section thực tế trong attempt và snapshot cấu hình section tại lúc user làm bài.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `attempt_id` | uuid | FK → `assessment_attempts` | Định danh attempt liên quan. |
| `content_section_id` | uuid | Logical ref ↗ `Content` | Định danh đối tượng `content_section` liên quan. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `section_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `started_at?` | — | — | Thời điểm bắt đầu nếu có. |
| `completed_at?` | — | — | Thời điểm hoàn thành nếu đã hoàn thành. |
| `UQ(attempt_id, sort_order)` | — | `UQ(attempt_id, sort_order)` | Ràng buộc duy nhất cho tổ hợp cột. |

V5.3: `section_snapshot` có dạng `{title, skill, instructions, passage?, audio?: {url, durationSeconds}, solution?: {transcript}}`.
`audio.url` là URL đầy đủ content đã ghép. `solution` chỉ server đọc: cấu trúc attempt trả section cho học viên theo danh
sách trường cho phép, không bao giờ có `solution`; transcript chỉ nằm trong lời giải khi kết quả ≥ 70%. Không migration.


## 6.3 `attempt_items`

Từng câu hỏi thực tế được giao trong attempt. Mỗi record là một câu hỏi thực tế được giao cho learner, kèm snapshot question/answer/knowledge để kết quả cũ không đổi.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `attempt_section_id` | uuid | FK → `attempt_sections` | Định danh đối tượng `attempt_section` liên quan. |
| `question_version_id` | uuid | Logical ref ↗ `Content` | Định danh phiên bản câu hỏi liên quan. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `question_snapshot` | jsonb | — | V5.2: đề và lựa chọn, **không** có đáp án. Assessment tự lấy từ Content khi tạo attempt. |
| `answer_snapshot` | jsonb | — | V5.2: `{answerSpec, explanation, maxScore}` chép từ Content khi tạo attempt; dùng để tự chấm; không bao giờ trả cho học viên. |
| `knowledge_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `UQ(attempt_section_id, sort_order)` | — | `UQ(attempt_section_id, sort_order)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 6.4 `attempt_responses`

Câu trả lời hiện tại của learner cho từng item và dữ liệu autosave. Bảng giữ câu trả lời hiện tại/autosave của learner cho từng attempt item và hỗ trợ revision/idempotency ở tầng response.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `attempt_item_id` | uuid | FK → `attempt_items` | Định danh đối tượng `attempt_item` liên quan. |
| `response_payload` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `schema_version` | integer | — | Phiên bản schema của payload. |
| `revision` | bigint | — | Tham chiếu tới đối tượng liên quan. |
| `saved_at` | — | — | Mốc thời gian của sự kiện. |
| `submitted_at?` | — | — | Thời điểm submit nếu có. |
| `UQ(attempt_item_id)` | — | `UQ(attempt_item_id)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 6.5 `learner_submissions`

Bài Writing hoặc audio Speaking được nộp để chấm. Nó đại diện cho bài Writing hoặc audio Speaking cần chuyển sang grading pipeline thay vì chấm đáp án trực tiếp.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Định danh user liên quan. |
| `attempt_item_id?` | uuid | FK → `attempt_items` | Tham chiếu tới đối tượng liên quan. |
| `prompt_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `skill` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `text_payload?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `audio_reference?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `submission_key` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `submitted_at` | — | — | Mốc thời gian của sự kiện. |
| `UQ(user_id, submission_key)` | — | `UQ(user_id, submission_key)` | Ràng buộc duy nhất cho tổ hợp cột. |


### Quy tắc placement test và point

Placement test không được miễn phí AI grading chỉ vì đây là bài đầu vào:

- Reading/Listening được chấm tự động theo đáp án và không trừ AI point.
- Writing tạo `learner_submissions` + `grading_jobs` với `grading_mode = AI` và trừ point theo `grading_point_costs`. Baseline hiện tại: 3 point.
- Speaking tạo `learner_submissions` + `grading_jobs` với `grading_mode = AI` và trừ point theo `grading_point_costs`. Baseline hiện tại: 5 point.
- Nếu placement gồm cả Writing và Speaking thì baseline tổng chi phí AI grading là 8 point.
- Quy tắc này áp dụng cả với user Premium: Premium không làm AI grading trở thành miễn phí và human grading credit không được dùng thay cho point của placement test.
- Nếu không đủ point tại thời điểm tạo AI grading job thì không khởi tạo job chấm tương ứng. Retry kỹ thuật của cùng job không được trừ thêm point.

## 6.6 `assessment_results`

Kết quả tổng hợp của một attempt. Đây là kết quả tổng hợp cấp attempt, có version để hỗ trợ tính lại kết quả khi grading hoàn tất hoặc thay đổi hợp lệ.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `attempt_id` | uuid | FK → `assessment_attempts` | Định danh attempt liên quan. |
| `result_version` | integer | — | Thông tin phiên bản. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `overall_band?` | numeric(3,1) | — | Tham chiếu tới đối tượng liên quan. |
| `completed_at?` | — | — | Thời điểm hoàn thành nếu đã hoàn thành. |
| `UQ(attempt_id, result_version)` | — | `UQ(attempt_id, result_version)` | Ràng buộc duy nhất cho tổ hợp cột. |

V5.2: attempt mà mọi câu tự chấm được thì khi nộp tự tạo version 1 `COMPLETED` và outbox `AssessmentCompleted.v2` (có `package_version_id`, `learning_goal_id` null) trong cùng transaction. Điểm trả học viên tính từ `item_results` (đúng/sai từng câu); lời giải chỉ trả khi ≥ 70%. Không thêm cột.


## 6.7 `skill_scores`

Điểm theo từng kỹ năng. Bảng tách điểm từng kỹ năng Reading/Listening/Writing/Speaking và ghi rõ nguồn chấm AUTO/AI/HUMAN.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `result_id` | uuid | FK → `assessment_results` | Định danh kết quả liên quan. |
| `skill` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `raw_score?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `band?` | numeric(3,1) | — | Tham chiếu tới đối tượng liên quan. |
| `grading_source` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `feedback_revision_id?` | uuid | Logical ref ↗ `AI Evaluation` | Tham chiếu tới đối tượng liên quan. |
| `UQ(result_id, skill)` | — | `UQ(result_id, skill)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 6.8 `item_results`

Kết quả từng câu hỏi. Bảng lưu kết quả chi tiết từng câu để dashboard, error analysis và Adaptive Learning biết learner sai ở đâu.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `result_id` | uuid | FK → `assessment_results` | Định danh kết quả liên quan. |
| `attempt_item_id` | uuid | FK → `attempt_items` | Định danh đối tượng `attempt_item` liên quan. |
| `score` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `is_correct?` | — | — | Cờ boolean. |
| `duration_milliseconds?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `feedback_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `UQ(result_id, attempt_item_id)` | — | `UQ(result_id, attempt_item_id)` | Ràng buộc duy nhất cho tổ hợp cột. |


## 6.9 `error_analysis_items`

Phân loại lỗi theo knowledge point để phục vụ mastery và luyện điểm yếu. Nó gắn lỗi cụ thể với knowledge point, biến kết quả assessment thành tín hiệu đầu vào cho mastery/adaptive learning.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `item_result_id` | uuid | FK → `item_results` | Định danh đối tượng `item_result` liên quan. |
| `knowledge_point_id` | uuid | Logical ref ↗ `Content` | Định danh đối tượng `knowledge_point` liên quan. |
| `error_type` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `explanation` | — | — | Thuộc tính nghiệp vụ của bảng. |



## 6.10 `video_practice_attempts`

Lưu một lần learner luyện tập trên video segment. Bảng dùng chung cho các mode như `DICTATION` và `SHADOWING`, tránh tạo một bảng attempt riêng cho từng loại practice.

Video YouTube không được lưu ở bảng này. Nếu `SHADOWING` cần lưu recording của learner thì `audio_reference` chỉ tham chiếu **audio do learner ghi âm**, không phải file video YouTube.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh lần luyện video. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner thực hiện practice. |
| `video_id` | uuid | Logical ref ↗ `Library.learning_videos` | Video đang luyện. |
| `segment_id` | uuid | Logical ref ↗ `Library.video_segments` | Segment cụ thể được dùng cho attempt. |
| `practice_type` | varchar(30) | — | Loại practice; baseline V4 dùng `DICTATION`, `SHADOWING`. |
| `reference_text_snapshot` | text | — | Snapshot transcript chuẩn tại thời điểm learner luyện để kết quả cũ không thay đổi nếu content được sửa sau này. |
| `response_text?` | text | — | Câu learner nhập khi `DICTATION`. |
| `audio_reference?` | varchar(500) | — | Reference tới recording của learner khi `SHADOWING`, nếu hệ thống chọn lưu audio. |
| `score?` | numeric(5,2) | — | Điểm tổng hợp của attempt nếu có. |
| `result_payload?` | jsonb | — | Chi tiết kết quả, ví dụ word differences hoặc pronunciation scores từ provider. |
| `status` | varchar(30) | — | `IN_PROGRESS`, `COMPLETED`, `FAILED`. |
| `started_at` | timestamptz | — | Thời điểm bắt đầu. |
| `completed_at?` | timestamptz | — | Thời điểm hoàn thành. |
| `created_at` | timestamptz | — | Thời điểm tạo record. |

Ví dụ Dictation:

```text
practice_type = DICTATION

reference:
Climate change is affecting cities.

response:
Climate change affecting city.

result:
accuracy = 75
missing = ["is"]
incorrect = [{"expected":"cities","actual":"city"}]
```

Ví dụ Shadowing:

```text
practice_type = SHADOWING

result_payload:
accuracyScore = 85
fluencyScore = 78
completenessScore = 90
pronunciationScore = 82
```

`result_payload` cho phép giữ response chi tiết của pronunciation provider trong MVP mà chưa phải tách nhiều bảng rubric/word score.
---

# 7. Learning Service — tiến độ học, bằng chứng mastery, bài ôn, mã đề

`learning-service` (Java, `learning_db`, Flyway V1) thay `learning-service` Python từ 2026-10-01 (plan
`261001-1228`). Toàn bộ schema `ai_learning_db` cũ (mastery path DeepTutor, tutor, practice notebook, learner memory,
hạn mức tutor) đã bị bỏ, không chuyển dữ liệu. Không có aggregate path: bằng chứng gắn với user; mastery của KP tính khi
đọc bằng `compute_mastery` (port từ DeepTutor v1.6.9: 5 lần gần nhất, trọng số 0.5→1.0, trần 0.5/0.8 khi có 1/2 lần).
Mọi lượt ghi của một học viên chạy trong một transaction mở đầu bằng `pg_advisory_xact_lock(hashtext(user_id))`.
Không có FK sang DB khác; id của topic, bài, khối, câu, gói, KP là id logic của Content.

## 7.1 `knowledge_point_catalog`

Bảng dùng chung mọi học viên, ghi lại từ Content `topic-sequence` mỗi lần `GET /api/learning/topics`; consumer đọc
`has_practice_set` mà không gọi HTTP.

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `kp_id` | uuid | PK | KP của Content |
| `topic_id` | uuid | NOT NULL, INDEX | |
| `has_practice_set` | boolean | NOT NULL | Content có gói `PRACTICE_SET` đủ điều kiện cho KP; không có thì không chèn bài ôn |
| `refreshed_at` | timestamptz | NOT NULL | |

## 7.2 `topic_progress`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `user_id`, `topic_id` | uuid | PK | |
| `sequence_order` | int | NULL | Thứ tự học hiện tại; topic không còn trong `topic-sequence` thì NULL và bị bỏ qua |
| `passed_at` | timestamptz | NULL | Một chiều: đã đạt thì không gỡ |
| `updated_at` | timestamptz | NOT NULL | |

Không lưu trạng thái: `PASSED` khi có `passed_at`, topic đầu chưa đạt theo `sequence_order` là `IN_PROGRESS`, còn lại `LOCKED`.

## 7.3 `lesson_progress`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `user_id`, `lesson_id` | uuid | PK | |
| `topic_id` | uuid | NOT NULL | INDEX (`user_id`, `topic_id`, `lesson_sort_order`) |
| `lesson_sort_order` | int | NOT NULL | |
| `knowledge_point_ids` | uuid[] | NOT NULL, GIN | KP bài dạy, chép từ Content mỗi lần đọc/nộp; luật bài ôn tìm bài dạy KP ở đây |
| `passed_block_ids` | text[] | NOT NULL | Khối bài tập đã đạt |
| `completed_at` | timestamptz | NULL | Bài xong khi mọi khối `blockKind = EXERCISE` đạt; khối essay không tính |
| `updated_at` | timestamptz | NOT NULL | |

## 7.4 `lesson_exercise_submissions`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `id` | uuid | PK | |
| `user_id`, `lesson_id`, `block_id` | uuid | NOT NULL | INDEX (`user_id`, `lesson_id`, `block_id`): xác định lần nộp đầu của khối |
| `request_id` | uuid | UNIQUE | Idempotency; trùng thì trả `response` cũ |
| `answers` | jsonb | NOT NULL | |
| `block_passed` | boolean | NOT NULL | ≥ 70% |
| `response` | jsonb | NOT NULL | Response đã trả, dùng để replay và tìm câu sai lần đầu |
| `submitted_at` | timestamptz | NOT NULL | Ghi bằng `clock_timestamp()` sau khi khóa |

## 7.5 `review_items`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `id` | uuid | PK | |
| `user_id`, `knowledge_point_id`, `lesson_id` | uuid | NOT NULL | `lesson_id`: bài dạy KP có thứ tự nhỏ nhất |
| `status` | varchar(20) | CHECK `PENDING`, `DONE`, `SKIPPED` | Partial UNIQUE (`user_id`, `knowledge_point_id`) WHERE `PENDING` |
| `created_at`, `done_at` | timestamptz | | `done_at`: lúc rời `PENDING` |

Chèn khi: mastery < ngưỡng (0.6), KP có câu sai trong kết quả vừa xét, có bài đã xong dạy KP, và `has_practice_set`.
Trượt set thứ 3 hoặc không còn gói nào thì `SKIPPED`.

## 7.6 `review_sets`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `id` | uuid | PK | |
| `review_item_id` | uuid | FK → `review_items` | Partial UNIQUE (`review_item_id`) WHERE `submitted_at IS NULL`: một set mở |
| `user_id`, `package_id`, `package_version_id` | uuid | NOT NULL | INDEX (`user_id`, `package_id`): gói chưa giao đi trước, hết thì gói giao lâu nhất |
| `assigned_at`, `submitted_at` | timestamptz | | |
| `passed` | boolean | NULL | |
| `request_id` | uuid | UNIQUE NULL | |
| `response` | jsonb | NULL | |

## 7.7 `topic_test_assignments`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `id` | uuid | PK | |
| `user_id`, `topic_id`, `package_id`, `package_version_id` | uuid | NOT NULL | Partial UNIQUE (`user_id`, `topic_id`) WHERE `consumed_at IS NULL` |
| `assigned_at` | timestamptz | NOT NULL | INDEX (`user_id`, `package_version_id`, `assigned_at`) |
| `consumed_attempt_id`, `consumed_at`, `percent` | | NULL | Attempt đầu tiên hoàn thành sau `assigned_at` dùng hết lần giao; ≥ 70% thì topic `PASSED` |

## 7.8 `kp_evidence`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `id` | uuid | PK | |
| `ordinal` | bigint | IDENTITY | Thứ tự chèn; mastery đọc theo cột này vì `created_at` trùng trong một transaction |
| `user_id`, `kp_id` | uuid | NOT NULL | INDEX (`user_id`, `kp_id`, `ordinal`) |
| `correct` | boolean | NOT NULL | |
| `source` | varchar(30) | CHECK `lesson_exercise`, `review_set`, `assessment` | UNIQUE (`user_id`, `source`, `source_reference_id`) |
| `source_reference_id` | uuid | NOT NULL | UUIDv5 tất định theo request/kết quả, câu, KP |
| `attempt_id`, `result_version` | | NULL | Chỉ với `assessment`; chấm lại thì xóa bằng chứng của version cũ |
| `created_at` | timestamptz | NOT NULL | |

Bài học ghi ở lần nộp đầu của khối; bài ôn ghi mỗi set; kết quả thi ghi mỗi KP của item (`is_correct`, hoặc judgment
`PASS`/`FAIL`). `PLACEMENT` không ghi.

## 7.9 `assessment_result_versions`

| Cột | Kiểu | Ràng buộc | Ghi chú |
| --- | --- | --- | --- |
| `user_id`, `attempt_id` | uuid | PK | |
| `result_version` | int | CHECK ≥ 1 | Version đã áp; bằng hoặc thấp hơn thì bỏ qua, cao hơn thì thay bằng chứng |
| `processed_at` | timestamptz | NOT NULL | |

Kế tiếp (plan 0737): migration V2 thêm `lesson_writing_submissions`, `llm_daily_usage` (hạn mức chấm Writing) và giá trị
`lesson_writing` cho `kp_evidence.source`.

---

# 8. Activity/Streak ở User; Video Progress và Personal Library ở Library

Service hỗ trợ học viên cũ đã gỡ. User V5 tạo `learning_activities`, `streaks` trong **`user_db`**; library V2 tạo sáu bảng còn lại trong **`library_db`**. Không chép dữ liệu cũ và không có migration trên DB cũ. AI Learning không sở hữu các bảng tiện ích này.

Ownership boundary:

```text
user_db (user-service, V5)
├── learning_activities
└── streaks

library_db (library-service, V2)
├── video_learning_progress
├── saved_video_segments
├── notes
├── flashcard_decks
├── flashcards
└── flashcard_deck_items
```

Quy tắc:

- không bảng tiện ích nào là mastery authority; User/Library không chạy DeepTutor policy/scheduler hoặc quyết định `next_objective()`;
- video canonical metadata/segment thuộc `library_db` (§5.10–§5.12);
- activity phát sinh từ Tutor/Assessment/Game dùng API/event contract khi tích hợp, không query database service khác;
- `user_id` trong hai DB này là logical reference, không FK xuyên DB; user V5 không FK tới `users`;
- library V2 có bốn FK nội bộ `ON DELETE RESTRICT`: `flashcards.vocabulary_sense_id` → `vocabulary_senses`,
  `video_learning_progress.video_id` và `saved_video_segments.video_id` → `learning_videos`,
  `saved_video_segments.segment_id` → `video_segments`.

## 8.1 `learning_activities`

Nhật ký activity phục vụ dashboard/streak/analytics. Đây **không phải mastery evidence authority**; nếu activity đến từ Tutor/Assessment thì service nhận integration event thay vì tự mutation DeepTutor state.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Activity ID. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner. |
| `activity_type` | varchar(50) | — | Ví dụ `TUTOR_SESSION`, `FORMAL_PRACTICE`, `VIDEO`, `FLASHCARD`. |
| `source_type` | varchar(50) | — | Domain nguồn. |
| `source_id` | uuid | — | ID nguồn. |
| `occurred_at` | timestamptz | NOT NULL | Thời điểm activity. |
| `duration_seconds` | integer | CHECK >= 0 | Thời lượng. |
| `verified_at?` | timestamptz | — | Verification nếu cần cho leaderboard/streak rule. |

---

## 8.2 `streaks`

Projection đọc nhanh được tổng hợp từ learning activity. Streak không tham gia mastery/policy DeepTutor và chỉ là learner engagement projection.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `user_id` | uuid | PK, Logical ref ↗ `Identity.users` | Learner. |
| `current_days` | integer | DEFAULT 0 | Streak hiện tại. |
| `longest_days` | integer | DEFAULT 0 | Streak dài nhất. |
| `last_qualified_date?` | date | — | Ngày gần nhất đủ điều kiện. |
| `timezone` | varchar(100) | — | Timezone tính streak. |

---

## 8.3 `video_learning_progress`

Giữ learner-owned progress của YouTube learning. Bảng này không phải DeepTutor mastery state. `learning-service` có thể đọc progress qua API/event contract khi cần context học video.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Progress ID. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner. |
| `video_id` | uuid | FK → `Library.learning_videos` (`ON DELETE RESTRICT`) | Video. |
| `last_position_ms` | integer | DEFAULT 0, CHECK >= 0 | Vị trí phát gần nhất. |
| `watched_duration_seconds` | integer | DEFAULT 0, CHECK >= 0 | Tổng thời lượng đã ghi nhận. |
| `progress_percent` | numeric(5,2) | `0..100` | UI progress. |
| `status` | varchar(30) | — | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. |
| `started_at?` | timestamptz | — | Lần bắt đầu đầu tiên. |
| `last_watched_at?` | timestamptz | — | Lần xem gần nhất. |
| `completed_at?` | timestamptz | — | Hoàn thành. |
| `updated_at` | timestamptz | NOT NULL | Cập nhật gần nhất. |
| `UQ(user_id, video_id)` | — | UNIQUE | Một progress/video/learner. |

---

## 8.4 `saved_video_segments`

Bookmark learner-owned cho video segment; không sao chép video.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Bookmark ID. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner. |
| `video_id` | uuid | FK → `Library.learning_videos` (`ON DELETE RESTRICT`) | Video nguồn. |
| `segment_id` | uuid | FK → `Library.video_segments` (`ON DELETE RESTRICT`) | Segment. |
| `transcript_snapshot` | text | — | Snapshot đoạn được lưu. |
| `note?` | text | — | Ghi chú learner. |
| `created_at` | timestamptz | NOT NULL | Thời điểm lưu. |
| `UQ(user_id, segment_id)` | — | UNIQUE | Không lưu trùng. |

---

## 8.5 `notes`

Ghi chú cá nhân của learner. Bảng lưu note tự do để learner tự ghi lại kiến thức, mẹo làm bài hoặc nội dung cần nhớ. Note có thể không có nguồn, hoặc gắn với một buổi học tutor hay một knowledge point. Không dùng tag.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của note. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner sở hữu note. |
| `title` | varchar(255) | — | Tiêu đề note. |
| `body` | text | — | Nội dung note. |
| `source_type` | varchar(50) | Nullable; `TUTOR_SESSION`, `KNOWLEDGE_POINT` hoặc `READING` | Loại nguồn của note; enum `NoteSourceType` xác thực giá trị. `READING` trỏ tới id section Reading của Content. |
| `source_reference_id` | uuid | Nullable; logical reference, không FK xuyên service | ID session hoặc knowledge point tương ứng. |
| `status` | — | — | Trạng thái như `ACTIVE`, `ARCHIVED`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

`chk_notes_source_pair` buộc `source_type` và `source_reference_id` cùng null hoặc cùng có giá trị. Index
`idx_notes_user_source` trên `(user_id, source_type, source_reference_id, updated_at DESC)` cho các note có nguồn
phục vụ lọc theo learner và nguồn. Nguồn bất biến sau khi tạo; note cũ giữ cặp nguồn null. Khi tutor phát
`note.draft`, frontend gửi `title`, `body`, `sourceType`, `sourceReferenceId` tới API notes bằng token của learner.
AI Learning không ghi trực tiếp vào bảng này.

## 8.6 `flashcard_decks`

Bộ flashcard do learner quản lý. Mỗi deck gom các flashcard theo một mục đích học như `Academic Vocabulary`, `Reading Words`, `Grammar Notes` hoặc bộ tự tạo khác. Deck thuộc riêng từng user; không cần versioning trong MVP.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của deck. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner sở hữu deck. |
| `name` | varchar(150) | — | Tên bộ flashcard. |
| `description?` | text | — | Mô tả ngắn cho deck. |
| `status` | — | — | Trạng thái như `ACTIVE`, `ARCHIVED`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo deck. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |
| `UQ(user_id, name)` | — | `UQ(user_id, name)` | Tránh trùng tên deck trong cùng một user nếu business muốn áp dụng. |

Không dùng `deck_version_id`. Deck là dữ liệu cá nhân mutable; user đổi tên/mô tả deck thì update trực tiếp. Nếu sau này cần versioning/audit riêng mới thiết kế thêm.

## 8.7 `flashcards`

Flashcard cá nhân của learner. Flashcard có thể được tạo thủ công, tạo từ một nghĩa cụ thể trong kho vocabulary, hoặc tạo nhanh bằng cách highlight một đoạn text trên giao diện rồi chọn **Create flashcard**.

`front` và `back` luôn lưu snapshot trực tiếp. Vì vậy flashcard đã tạo không tự thay đổi nếu vocabulary, note hoặc nội dung nguồn được chỉnh sửa sau này.

Flashcard trong MVP **không tham gia knowledge point/mastery** và không lưu lịch sử review.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của flashcard. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner sở hữu flashcard. |
| `source_type` | varchar(50) | — | Nguồn tạo flashcard: `MANUAL`, `VOCABULARY_SENSE`, `HIGHLIGHT` hoặc `PRACTICE_QUESTION`. |
| `vocabulary_sense_id?` | uuid | FK → `Library.vocabulary_senses` (`ON DELETE RESTRICT`) | Có giá trị khi flashcard được tạo từ một nghĩa/từ loại cụ thể trong kho vocabulary. Đây là field thay cho `entry_version_id`. |
| `source_reference_id?` | uuid | — | ID nguồn khi cần truy ngược flashcard được highlight từ note/content nào; dùng cùng `source_type`, không tạo FK đa hình. |
| `highlighted_text?` | text | — | Snapshot đoạn text learner đã highlight khi tạo flashcard bằng chức năng highlight. |
| `front` | text | — | Mặt trước của flashcard; lưu snapshot trực tiếp. |
| `back` | text | — | Mặt sau của flashcard; lưu snapshot trực tiếp. |
| `status` | — | — | Trạng thái như `ACTIVE`, `ARCHIVED`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

Quy ước nguồn:

- `MANUAL`: learner tự nhập `front`/`back`; các field nguồn có thể để `NULL`.
- `VOCABULARY_SENSE`: dùng `vocabulary_sense_id` để biết flashcard được tạo từ nghĩa/từ loại nào, nhưng `front`/`back` vẫn là snapshot.
- `HIGHLIGHT`: lưu `highlighted_text` và có thể lưu `source_reference_id` để truy ngược nguồn highlight nếu sản phẩm cần.
- `PRACTICE_QUESTION`: learner lưu một câu luyện đã trả lời của AI Learning; `source_reference_id` bắt buộc là `questionId` của
  câu đó, `vocabulary_sense_id` và `highlighted_text` phải `NULL`. Frontend tạo thẻ bằng dữ liệu `/practice/.../answer` trả về;
  library không gọi AI Learning. Library V2 tạo partial unique index `uq_flashcards_user_practice_question`
  `(user_id, source_reference_id) WHERE source_type = 'PRACTICE_QUESTION' AND status <> 'DELETED'`: mỗi learner có tối đa
  một thẻ chưa xóa cho một câu; lưu lại trả thẻ cũ với `200` (thẻ đang `ARCHIVED` được đưa về `ACTIVE`, giữ nguyên
  nội dung), thẻ đã xóa thì lưu lại được.

Không dùng `note_reference_id` riêng. Chức năng highlight có thể xuất phát từ note hoặc content khác; nếu cần truy nguồn thì dùng `source_type` + `source_reference_id`.

## 8.8 `flashcard_deck_items`

Bảng nối flashcard với deck. Tách bảng này thay vì đặt `deck_id` trực tiếp trong `flashcards` để một flashcard có thể nằm trong nhiều bộ và để quản lý thứ tự card trong từng deck.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `deck_id` | uuid | FK → `flashcard_decks` | Bộ flashcard. |
| `flashcard_id` | uuid | FK → `flashcards` | Flashcard được thêm vào bộ. |
| `sort_order?` | integer | — | Thứ tự hiển thị card trong deck nếu cần. |
| `added_at` | timestamptz | — | Thời điểm thêm card vào deck. |
| `PK(deck_id, flashcard_id)` | — | `PK(deck_id, flashcard_id)` | Một flashcard không xuất hiện trùng hai lần trong cùng một deck. |

Business rule đề xuất:

- Deck và flashcard phải cùng thuộc một `user_id`.
- Xóa/archived một deck không xóa flashcard gốc; chỉ mất quan hệ trong deck đó.
- Xóa một flashcard thì các record `flashcard_deck_items` tương ứng phải được xóa/cascade trong cùng service.
- Một flashcard có thể được thêm vào nhiều deck.

---

# 9. Game học tập — Vocabulary và Grammar

Game được tách thành **`game-service` riêng**, sở hữu `game_db`, vì roadmap có realtime multiplayer/WebSocket. Workload giữ connection lâu, room/match state, broadcast và scale theo số connection khác đáng kể so với Note/Flashcard CRUD.

Game hỗ trợ cả **Vocabulary** và **Grammar**, nhưng **không liên kết với `knowledge_points` và không trực tiếp cập nhật mastery**.

- Vocabulary game lấy dữ liệu từ `vocabulary_items`, `vocabulary_senses`, ảnh minh họa và audio pronunciation khi cần.
- Grammar game có thể lấy câu từ `questions/question_versions` hoặc dùng item grammar được sinh riêng rồi snapshot vào game session.
- Khi bắt đầu match, `game-service` lấy snapshot từ `library-service` nếu `learningDomain = VOCABULARY`, từ `content-service` nếu `GRAMMAR`, qua cùng contract `POST /internal/game-content/snapshots`; không gọi service cho từng WebSocket message.
- Realtime state ngắn hạn như connection, ready state, countdown, current question và temporary live score có thể đặt ở Redis.
- PostgreSQL `game_db` lưu dữ liệu bền vững như room, match, player, answer và các game event nghiệp vụ cần audit.
- Không lưu heartbeat/timer tick liên tục vào PostgreSQL.

Ví dụ game type có thể gồm `WORD_MEANING_MATCH`, `IMAGE_WORD_MATCH`, `SPELLING`, `SENTENCE_COMPLETION`, `ERROR_CORRECTION`, `WORD_ORDER`.

## 9.1 `game_sessions`

Đại diện cho một lượt chơi game học tập của learner, từ lúc bắt đầu đến khi kết thúc. Session lưu loại game, domain Vocabulary/Grammar và snapshot dữ liệu đầu vào để lượt chơi cũ không bị thay đổi nếu vocab hoặc câu grammar được chỉnh về sau.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh một lượt chơi của một learner. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner thực hiện game session. |
| `match_player_id?` | uuid | FK → `game_match_players` | Có giá trị khi session thuộc một người chơi trong multiplayer match; `NULL` với single-player. |
| `game_type` | — | — | Kiểu gameplay, ví dụ matching, spelling, sentence completion hoặc error correction. |
| `learning_domain` | — | — | Miền game: `VOCABULARY`, `GRAMMAR` hoặc `MIXED`. |
| `mode` | — | — | Chế độ chơi nếu sản phẩm có nhiều mode như practice/timed. |
| `topic_id?` | uuid | Logical ref ↗ `Content.topics` | Topic được dùng để lọc/chọn nội dung game nếu session được tạo theo một topic cụ thể. |
| `source_snapshot` | jsonb | — | Snapshot bộ vocab/câu grammar và cấu hình được dùng cho session. |
| `started_at` | timestamptz | — | Thời điểm bắt đầu. |
| `ended_at?` | timestamptz | — | Thời điểm kết thúc nếu đã hoàn thành/dừng. |
| `score?` | — | — | Điểm tổng của lượt chơi nếu game có tính điểm. |
| `status` | — | — | Trạng thái session, ví dụ `IN_PROGRESS`, `COMPLETED`, `ABANDONED`. |
| `verification_status` | — | — | Trạng thái xác minh nếu kết quả game được dùng cho leaderboard. |
| `UQ(match_player_id)` | — | UNIQUE khi NOT NULL | Mỗi participant trong một match chỉ có một game session chính thức; reconnect tiếp tục dùng session cũ. |

## 9.2 `game_answers`

Lưu kết quả từng item trong một game session. Bảng này dùng để tính score, hiển thị lịch sử đúng/sai và phân tích hiệu suất chơi game. Game MVP không được ingest trực tiếp vào DeepTutor mastery, vì vậy bảng không có `knowledge_point_id`; nếu tương lai game trở thành learning evidence thì phải đi qua adapter của `learning-service`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh câu/item trong lịch sử game. |
| `session_id` | uuid | FK → `game_sessions` | Game session chứa item này. |
| `item_sequence` | integer | — | Thứ tự item trong session. |
| `vocabulary_sense_id?` | uuid | Logical ref ↗ `Library.vocabulary_senses` | Nghĩa/từ loại cụ thể nếu item thuộc Vocabulary. Có thể để `NULL` với Grammar game. |
| `question_version_id?` | uuid | Logical ref ↗ `Content.question_versions` | Câu grammar nguồn nếu item được lấy từ question bank; có thể `NULL` nếu item được sinh riêng rồi chỉ lưu snapshot. |
| `item_snapshot` | jsonb | — | Snapshot nội dung learner đã nhìn thấy, gồm prompt/options/image/audio metadata khi cần. |
| `response_payload` | jsonb | — | Câu trả lời learner gửi cho item. |
| `is_correct` | boolean | — | Kết quả đúng/sai. |
| `duration_milliseconds` | bigint | — | Thời gian learner xử lý item. |
| `UQ(session_id, item_sequence)` | — | UNIQUE | Một vị trí trong session chỉ có một result record. |

Luồng Vocabulary game có thể là `vocabulary_senses → game session/item snapshot → game_answers → score`. Luồng Grammar game có thể là `question_versions → game session/item snapshot → game_answers → score`.

## 9.3 `game_rooms`

Đại diện cho lobby/phòng chờ multiplayer trước khi bắt đầu một match. Room tồn tại để người chơi join bằng code/link, ready và chờ host hoặc hệ thống start game. Realtime presence có thể được giữ ở Redis, còn bảng này lưu trạng thái bền vững của room.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của room. |
| `room_code` | varchar(20) | UNIQUE | Mã ngắn để learner join room. |
| `host_user_id` | uuid | Logical ref ↗ `Identity.users` | User tạo/host room. |
| `game_type` | varchar(50) | — | Loại game sẽ chơi trong room. |
| `learning_domain` | varchar(30) | — | `VOCABULARY`, `GRAMMAR` hoặc `MIXED`. |
| `mode` | varchar(30) | — | Chế độ multiplayer/timed/team nếu sản phẩm có. |
| `max_players` | integer | CHECK > 0 | Số người tối đa được phép tham gia. |
| `config_snapshot` | jsonb | — | Snapshot cấu hình room như time limit, số câu, scoring rule. |
| `status` | varchar(30) | — | Ví dụ `WAITING`, `IN_MATCH`, `CLOSED`, `EXPIRED`. |
| `created_at` | timestamptz | — | Thời điểm tạo room. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |
| `expires_at?` | timestamptz | — | Thời điểm room hết hạn nếu room không được sử dụng. |

`room_code` chỉ dùng để tìm room; các quan hệ nội bộ vẫn dùng `room_id`.

## 9.4 `game_room_members`

Lưu membership bền vững của user trong một game room. Bảng này giúp biết ai đã join, ai là host/player, trạng thái ready/left/disconnected và hỗ trợ reconnect.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh membership trong room. |
| `room_id` | uuid | FK → `game_rooms` | Room mà user tham gia. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User tham gia room. |
| `member_role` | varchar(20) | — | Ví dụ `HOST`, `PLAYER`. |
| `status` | varchar(30) | — | Ví dụ `JOINED`, `READY`, `IN_MATCH`, `LEFT`, `DISCONNECTED`. |
| `joined_at` | timestamptz | — | Thời điểm join room. |
| `ready_at?` | timestamptz | — | Thời điểm user chuyển sang ready. |
| `left_at?` | timestamptz | — | Thời điểm rời room. |
| `last_seen_at?` | timestamptz | — | Mốc gần nhất để hỗ trợ reconnect/debug presence. |
| `UQ(room_id, user_id)` | — | UNIQUE | Một user chỉ có một membership trong cùng một room. |

Không cần ghi heartbeat mỗi vài giây vào PostgreSQL. Presence/connection sống nên đặt ở Redis; `last_seen_at` chỉ cập nhật ở các mốc cần thiết.

## 9.5 `game_matches`

Đại diện cho một trận game thực tế. Một room có thể tạo một hoặc nhiều match theo thời gian; mỗi match phải snapshot content/config để kết quả cũ không đổi khi content nguồn được sửa.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của match. |
| `room_id?` | uuid | FK → `game_rooms` | Room tạo ra match; có thể `NULL` nếu sau này có matchmaking không qua lobby. |
| `game_type` | varchar(50) | — | Loại gameplay của match. |
| `learning_domain` | varchar(30) | — | `VOCABULARY`, `GRAMMAR` hoặc `MIXED`. |
| `config_snapshot` | jsonb | — | Snapshot rule, time limit, scoring config của match. |
| `content_snapshot` | jsonb | — | Snapshot bộ item/question/vocabulary dùng trong match. |
| `status` | varchar(30) | — | Ví dụ `CREATED`, `COUNTDOWN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. |
| `started_at?` | timestamptz | — | Thời điểm match thực sự bắt đầu. |
| `ended_at?` | timestamptz | — | Thời điểm kết thúc. |
| `created_at` | timestamptz | — | Thời điểm tạo match. |

Khi match đã bắt đầu, gameplay nên chạy từ `content_snapshot`/Redis thay vì gọi `content-service` cho từng câu.

## 9.6 `game_match_players`

Lưu từng participant của một match và trạng thái/điểm tổng của họ. Bảng này là nguồn dữ liệu để kết thúc trận, xếp hạng trong trận và tạo lịch sử multiplayer.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh participant trong match. |
| `match_id` | uuid | FK → `game_matches` | Match mà player tham gia. |
| `room_member_id?` | uuid | FK → `game_room_members` | Membership nguồn nếu match được tạo từ room. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User tham gia match. |
| `score` | integer | DEFAULT 0 | Điểm hiện tại/final của player. |
| `rank?` | integer | — | Thứ hạng final sau khi match hoàn thành. |
| `status` | varchar(30) | — | Ví dụ `ACTIVE`, `FINISHED`, `DISCONNECTED`, `LEFT`. |
| `joined_at` | timestamptz | — | Thời điểm player được đưa vào match. |
| `finished_at?` | timestamptz | — | Thời điểm player hoàn thành match. |
| `UQ(match_id, user_id)` | — | UNIQUE | Một user chỉ xuất hiện một lần trong cùng match. |

Live score có thể nằm ở Redis trong khi trận đang chạy; khi có mốc quan trọng hoặc match kết thúc thì đồng bộ xuống bảng này.

## 9.7 `game_events`

Lưu **event nghiệp vụ bền vững** phát sinh trong một match để audit, replay mức nghiệp vụ hoặc debug. Không dùng bảng này để ghi mọi WebSocket frame.

Ví dụ event nên lưu: `MATCH_STARTED`, `PLAYER_JOINED_MATCH`, `PLAYER_ANSWERED`, `SCORE_CHANGED`, `PLAYER_DISCONNECTED`, `MATCH_COMPLETED`.

Không nên lưu: `HEARTBEAT`, timer tick mỗi giây, cursor/presence noise hoặc các message tạm thời khác.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh event. |
| `match_id` | uuid | FK → `game_matches` | Match phát sinh event. |
| `match_player_id?` | uuid | FK → `game_match_players` | Player liên quan nếu event thuộc một participant cụ thể. |
| `sequence_no` | bigint | — | Thứ tự event trong match để xử lý/replay đúng trình tự. |
| `event_type` | varchar(50) | — | Loại event nghiệp vụ. |
| `payload` | jsonb | — | Dữ liệu chi tiết của event. |
| `occurred_at` | timestamptz | — | Thời điểm event xảy ra. |
| `created_at` | timestamptz | — | Thời điểm record được persist. |
| `UQ(match_id, sequence_no)` | — | UNIQUE | Một sequence trong cùng match chỉ có một event. |

`game_events` khác `outbox_events`: `game_events` là lịch sử nghiệp vụ bên trong game; `outbox_events` là cơ chế kỹ thuật để publish integration event từ `game-service` sang service khác.

### Luồng realtime multiplayer

```text
Client A/B/C
    ↓ WebSocket
API Gateway
    ↓
game-service
    ↓
game_rooms / game_room_members
    ↓
READY
    ↓
game_matches
    ↓
game_match_players
    ↓
Redis: live room/match state
    ↓
WebSocket broadcast
    ↓
game_sessions + game_answers
    ↓
game_events (chỉ event nghiệp vụ cần persist)
    ↓
match COMPLETED
    ↓
persist final score/rank
```

Quan hệ chính:

```text
game_rooms
   ├──< game_room_members
   └──< game_matches
            ├──< game_match_players
            │         └── 0..1 game_sessions
            │                    └──< game_answers
            └──< game_events
```

---

# 10. Notification — Nhắc học và thông báo

## 10.1 `notification_preferences`

Cấu hình nhận thông báo của user. Bảng lưu lựa chọn nhận thông báo theo channel/thời gian của từng user để hệ thống tôn trọng preference cá nhân.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Định danh user liên quan. |
| `channel` | — | — | Kênh thông báo. |
| `is_enabled` | boolean | — | Cờ bật/tắt kênh. |
| `preferred_local_time?` | time | — | Giờ địa phương user muốn nhận thông báo. |
| `timezone` | — | — | Múi giờ áp dụng. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |
| `UQ(user_id, channel)` | — | `UQ(user_id, channel)` | Một cấu hình cho mỗi user/kênh. |

## 10.2 `push_devices`

Các thiết bị có thể nhận push notification. Bảng quản lý các thiết bị/installation có thể nhận push và lifecycle của endpoint/token tương ứng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User sở hữu thiết bị. |
| `installation_id` | — | — | Định danh installation trên client. |
| `platform` | — | — | Nền tảng thiết bị. |
| `endpoint_reference` | — | — | Token/endpoint tham chiếu để gửi push. |
| `status` | — | — | Trạng thái thiết bị. |
| `last_seen_at` | — | — | Lần gần nhất thiết bị hoạt động. |
| `revoked_at?` | — | — | Thời điểm thu hồi nếu có. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

## 10.3 `reminder_schedules`

Lưu lịch nhắc học hoặc lịch notification đã được hệ thống lên kế hoạch cho user. Bảng mô tả reminder đã lên lịch, giúp worker biết khi nào và qua channel nào cần tạo/gửi notification.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh schedule. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User nhận reminder. |
| `trigger_type` | — | — | Loại trigger, ví dụ daily study, due review, plan reminder. |
| `channel` | — | — | Kênh gửi. |
| `scheduled_at` | timestamptz | — | Thời điểm dự kiến gửi. |
| `deduplication_key` | varchar(255) | — | Khóa chống tạo lịch trùng. |
| `status` | — | — | Trạng thái schedule. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật. |
| `UQ(user_id, deduplication_key)` | — | `UQ(user_id, deduplication_key)` | Chống schedule trùng cho cùng user. |

## 10.4 `notifications`

Nội dung notification thực tế mà user nhìn thấy. Đây là nội dung notification user nhìn thấy trong hệ thống và trạng thái đã đọc của từng bản tin.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh notification. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User nhận notification. |
| `schedule_id?` | uuid | FK → `reminder_schedules` | Schedule sinh ra notification nếu có. |
| `title` | — | — | Tiêu đề. |
| `body` | — | — | Nội dung chính. |
| `target_type?` | — | — | Loại đối tượng được điều hướng tới. |
| `target_id?` | uuid | — | ID đối tượng đích. |
| `read_at?` | — | — | Thời điểm user đã đọc. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |

## 10.5 `notification_deliveries`

Lưu từng lần hệ thống thực sự cố gửi một `notification` ra kênh bên ngoài như Firebase Cloud Messaging (push notification) hoặc email. Bảng này tách khỏi `notifications` vì một nội dung thông báo có thể được gửi nhiều lần do retry, hoặc gửi qua nhiều kênh/thiết bị khác nhau.

Ví dụ một `notification` có nội dung “Bài Writing của bạn đã được chấm xong”. Lần gửi push đầu tiên bị timeout, worker retry lần hai và gửi thành công. `notifications` vẫn chỉ có một record nội dung, còn `notification_deliveries` lưu hai lần gửi để có thể debug và audit.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của một lần gửi. |
| `notification_id` | uuid | FK → `notifications` | Notification đang được gửi. |
| `channel` | — | — | Kênh gửi, ví dụ `PUSH`, `EMAIL`. |
| `push_device_id?` | uuid | FK → `push_devices` | Thiết bị nhận push nếu channel là `PUSH`; có thể `NULL` với channel khác. |
| `provider` | — | — | Provider thực hiện gửi, ví dụ `FCM`. |
| `attempt_number` | integer | — | Số thứ tự lần thử gửi của cùng notification/kênh/đích nhận. |
| `status` | — | — | Trạng thái như `PENDING`, `SENT`, `FAILED`. |
| `provider_message_id?` | varchar(255) | — | ID message do provider trả về khi có. |
| `idempotency_key` | varchar(255) | UNIQUE | Chống worker tạo/xử lý trùng cùng một delivery attempt. |
| `attempted_at` | timestamptz | — | Thời điểm hệ thống thực hiện lần gửi. |
| `sent_at?` | timestamptz | — | Thời điểm gửi thành công nếu có. |
| `failure_reason?` | text | — | Lý do thất bại để debug/retry. |
| `created_at` | timestamptz | — | Thời điểm tạo record delivery. |

Luồng điển hình:

```text
reminder_schedules / domain event
             ↓
       notifications
             ↓
 notification_deliveries
             ↓
        FCM / Email
             ↓
      SENT hoặc FAILED
             ↓
         retry nếu cần
```

`notifications` là nội dung user nhìn thấy; `notification_deliveries` là lịch sử vận chuyển nội dung đó ra provider bên ngoài.

# 11. Community — Feed

Community trong MVP chỉ giữ feed chung, comment/reply và reaction. Không có study group và không có challenge.

## 11.1 `posts`

Bài viết trên feed chung của hệ thống. Đây là aggregate chính của phần community discussion, dùng để learner đăng bài chia sẻ, hỏi đáp hoặc thảo luận với cộng đồng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bài viết. |
| `author_id` | uuid | Logical ref ↗ `Identity.users` | User tạo bài viết. |
| `category` | — | — | Nhóm/chủ đề hiển thị của bài viết nếu UI có phân loại feed. |
| `title?` | — | — | Tiêu đề bài viết, có thể để trống nếu hỗ trợ post ngắn. |
| `body` | — | — | Nội dung chính của bài viết. |
| `status` | — | — | Trạng thái bài viết, ví dụ `ACTIVE`, `HIDDEN`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo bài viết. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

## 11.2 `comments`

Lưu comment và reply dưới bài viết. `parent_comment_id` cho phép tạo cây hội thoại nhiều cấp mà không cần bảng reply riêng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của comment. |
| `post_id` | uuid | FK → `posts` | Bài viết chứa comment này. |
| `author_id` | uuid | Logical ref ↗ `Identity.users` | User tạo comment/reply. |
| `parent_comment_id?` | uuid | FK → `comments` | Comment cha nếu đây là reply; `NULL` nếu là comment gốc. |
| `body` | — | — | Nội dung comment. |
| `status` | — | — | Trạng thái comment, ví dụ `ACTIVE`, `HIDDEN`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo comment. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

## 11.3 `post_reactions`

Lưu reaction của user với bài viết. Bảng mapping này cho phép một post có nhiều reaction từ nhiều user và chống ghi trùng cùng một reaction của cùng user.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `post_id` | uuid | FK → `posts` | Bài viết được reaction. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | User thực hiện reaction. |
| `reaction_type` | — | — | Loại reaction như `LIKE`, `LOVE` nếu sản phẩm hỗ trợ nhiều loại. |
| `created_at` | timestamptz | — | Thời điểm reaction được tạo. |
| `PK(post_id, user_id, reaction_type)` | — | `PK(post_id, user_id, reaction_type)` | Ngăn cùng user tạo trùng cùng một reaction trên cùng post. |

---

# 12. Quiz và Leaderboard

## 12.1 `quiz_events`

Một đợt quiz mà nhiều learner có thể tham gia. Bảng đại diện cho một đợt quiz có thời gian mở/đóng và content package được nhiều learner cùng tham gia.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `package_version_id` | uuid | Logical ref ↗ `Content` | Định danh phiên bản package liên quan. |
| `topic_id` | uuid | Logical ref ↗ `Content` | Định danh topic liên quan. |
| `opens_at` | — | — | Mốc thời gian của sự kiện. |
| `closes_at?` | — | — | Mốc thời gian, có thể để trống. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


## 12.2 `quiz_participations`

Từng lượt user tham gia quiz. Bảng nối user với quiz event và assessment attempt thực tế, đồng thời giữ score/duration phục vụ xếp hạng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `quiz_event_id` | uuid | FK → `quiz_events` | Định danh đối tượng `quiz_event` liên quan. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Định danh user liên quan. |
| `attempt_id` | uuid | Logical ref ↗ `Assessment.assessment_attempts` | Định danh attempt liên quan. |
| `attempt_number` | integer | — | Số thứ tự/phiên bản. |
| `score?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `duration_milliseconds?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `verification_status` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


## 12.3 `leaderboard_periods`

Một kỳ leaderboard như tuần/tháng/all-time. Bảng định nghĩa phạm vi thời gian và rule của một bảng xếp hạng cụ thể.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `topic_id?` | uuid | Logical ref ↗ `Content` | Tham chiếu tới đối tượng liên quan. |
| `activity_type` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `period_type` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `starts_at` | — | — | Mốc thời gian của sự kiện. |
| `ends_at?` | — | — | Mốc thời gian, có thể để trống. |
| `timezone` | — | — | Múi giờ áp dụng. |
| `rules_version?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `created_at` | — | — | Thời điểm tạo bản ghi. |


## 12.4 `leaderboard_entries`

Projection điểm/rank hiện tại để đọc leaderboard nhanh. Đây là projection rank/score hiện tại để đọc leaderboard nhanh mà không tính lại toàn bộ contribution khi request.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `period_id` | uuid | FK → `leaderboard_periods` | Định danh đối tượng `period` liên quan. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Định danh user liên quan. |
| `score` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `tie_break_duration_milliseconds?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `rank?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `calculated_at` | — | — | Mốc thời gian của sự kiện. |
| `UQ(period_id, user_id)` | — | `UQ(period_id, user_id)` | Ràng buộc duy nhất cho tổ hợp cột. |


---

# 13. Grading — AI và người chấm

Business rule chấm Writing/Speaking có hai mode:

- `AI`: user trả bằng point. Baseline đề xuất `WRITING = 3 point`, `SPEAKING = 5 point`; chi phí được cấu hình trong DB để thay đổi mà không sửa code. Quy tắc này áp dụng cả Writing/Speaking trong placement test và cả user Premium.
- `HUMAN`: chỉ user có Premium hợp lệ và còn human grading credit mới được tạo job chấm bởi người thật. Point key không bao giờ mở HUMAN mode. Human grading credit không dùng thay cho AI point.

Các bảng core dưới đây thuộc `assessment_db` để giữ submission, grading job và kết quả assessment trong cùng bounded context. Point vẫn do `access-service` sở hữu; grading chỉ giữ logical reference tới ledger entry.

## 13.1 `grading_point_costs`

Cấu hình point cost cho AI grading theo skill và thời gian hiệu lực. Bảng cấu hình giá point của AI grading theo skill và thời gian hiệu lực để tránh hard-code chi phí trong Java.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh rule giá. |
| `skill` | — | — | `WRITING`, `SPEAKING`. |
| `grading_mode` | — | — | `AI`; giữ field để có thể mở rộng mode về sau. |
| `point_cost` | integer | CHECK `point_cost >= 0` | Số point trừ cho một lần chấm thành công được khởi tạo. |
| `effective_from` | timestamptz | — | Thời điểm rule bắt đầu có hiệu lực. |
| `effective_to?` | timestamptz | — | Thời điểm hết hiệu lực nếu có. |
| `status` | — | — | `ACTIVE`, `INACTIVE`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |

Baseline đã chốt hiện tại: `WRITING = 3`, `SPEAKING = 5`. Cùng rule giá này áp dụng cho AI grading thông thường và AI grading của placement test; nếu sau này placement có giá riêng thì mới mở rộng thêm scope/context cho bảng này.

## 13.2 `grading_jobs`

Một yêu cầu chấm cho `learner_submissions`. Job snapshot mode và point cost tại thời điểm tạo để lịch sử không thay đổi khi bảng cấu hình giá được chỉnh. Đây là aggregate theo dõi một yêu cầu chấm Writing/Speaking từ lúc tạo đến khi AI/Human hoàn tất.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh grading job. |
| `submission_id` | uuid | FK → `learner_submissions` | Submission Writing/Speaking cần chấm. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner yêu cầu chấm. |
| `skill` | — | — | `WRITING`, `SPEAKING`. |
| `grading_mode` | — | — | `AI`, `HUMAN`. |
| `status` | — | — | `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`. |
| `point_cost_snapshot?` | integer | — | Cost đã chốt khi tạo AI job; `NULL` cho human grading. |
| `point_ledger_entry_id?` | uuid | Logical ref ↗ `Access.point_ledger_entries` | Ledger entry đã debit point cho AI job. |
| `premium_subscription_id?` | uuid | Logical ref ↗ `Plans.subscriptions` | Subscription dùng để xác thực human grading. |
| `idempotency_key` | varchar(255) | UNIQUE | Chống tạo/trừ tiền cho cùng một request nhiều lần. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `completed_at?` | timestamptz | — | Thời điểm hoàn tất. |

Retry kỹ thuật của cùng một AI job không được trừ point lần nữa. Nếu job thất bại vĩnh viễn do lỗi hệ thống/provider sau khi đã debit, hệ thống tạo `AI_GRADING_REFUND` tương ứng trong point ledger.

## 13.3 `human_reviews`

Workflow phân công và theo dõi người thật chấm bài Premium. Người chấm dùng role `EXAMINER`. Bảng lưu workflow người thật nhận/chấm bài và liên kết grader với grading job Premium tương ứng.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh human review. |
| `grading_job_id` | uuid | FK → `grading_jobs`, UNIQUE | Human grading job tương ứng. |
| `grader_user_id?` | uuid | Logical ref ↗ `Identity.users` | Người chấm được phân công. |
| `status` | — | — | `QUEUED`, `ASSIGNED`, `IN_REVIEW`, `COMPLETED`. |
| `assigned_at?` | timestamptz | — | Thời điểm phân công. |
| `started_at?` | timestamptz | — | Thời điểm bắt đầu chấm. |
| `completed_at?` | timestamptz | — | Thời điểm hoàn tất. |
| `reviewer_note?` | text | — | Ghi chú nội bộ của grader nếu cần. |

Điểm/feedback cuối cùng tiếp tục được phản ánh vào các bảng kết quả Assessment như `assessment_results`, `skill_scores`, `item_results`. Các cấu trúc grading nâng cao như rubric chi tiết, transcript hoặc feedback revision chưa nằm trong baseline hiện tại.

---

# 14. Transactional Outbox

Mỗi business service/database sở hữu **một bảng `outbox_events` riêng** để publish integration event an toàn sang broker.

Nguyên tắc:

- Business data và `outbox_events` được ghi trong cùng database transaction khi event phát sinh từ mutation đó.
- Không có outbox dùng chung toàn hệ thống.
- Retry phải idempotent; consumer không giả định exactly-once delivery.
- `mastery_events` của DeepTutor là internal aggregate log, **không thay** `outbox_events`.

## 14.1 `outbox_events`

Schema chuẩn:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Integration event ID. |
| `aggregate_type` | varchar(100) | — | Aggregate phát event. |
| `aggregate_id` | varchar(255) | — | ID aggregate. |
| `event_type` | varchar(150) | — | Event contract/version. |
| `payload` | jsonb | — | Payload. |
| `created_at` | timestamptz | — | Tạo event. |
| `published_at?` | timestamptz | — | Publish thành công. |
| `retry_count` | integer | DEFAULT 0 | Số retry. |
| `last_error?` | text | — | Error gần nhất. |

Index ưu tiên:

```text
(published_at, created_at)
```

Các database có outbox trong thiết kế đích V3 sau chia service:

```text
user_db.outbox_events
access_db.outbox_events
content_db.outbox_events
assessment_db.outbox_events
game_db.outbox_events
community_db.outbox_events
```

Baseline V5 lịch sử có **9 bảng outbox vật lý**; thiết kế đích V3 còn **7**. Library V1/V2 không tạo `outbox_events`; notification cũng chưa có outbox. Service hỗ trợ học viên cũ đã gỡ. Đây là số đếm thiết kế, không phải kiểm kê các DB đang chạy. Không còn `ai_assistant_db.outbox_events` vì `ai-assistant-service` đã bị xóa.

---

# 15. Các quyết định cố ý không đưa thành bảng MVP

## 15.1 AI-generated canonical content

DeepTutor được phép generate practice trong Tutor Session và lưu câu đang hỏi trong `mastery_interactions.question_json`/session runtime.

MVP **không** tạo:

```text
practice_generation_requests
generation_request_knowledge_points
```

và không tự ghi câu AI sinh vào `content_db.questions`.

Nếu tương lai muốn tái sử dụng/publish content AI sinh:

```text
DeepTutor generated candidate
        ↓
Content Author review
        ↓
content-service authoring flow
        ↓
questions / question_versions
```

Schema cho candidate/review workflow chỉ thêm khi business feature này được chốt.

## 15.2 Custom mastery / planner tables

Không được thêm lại các bảng kiểu:

```text
learner_mastery
learner_objective_states
review_states
daily_learning_plans
daily_learning_objectives
```

nếu mục đích là tạo một Adaptive Engine song song. Nếu cần projection đọc nhanh, projection phải derive từ DeepTutor state và không có policy/mutation độc lập.

## 15.3 Memory relational schema

DeepTutor Memory v2 là subsystem L1/L2/L3 với trace/consolidation/snapshot semantics. V5 chưa chốt một relational schema giả định cho subsystem này. Production adapter phải được thiết kế khi implement memory persistence, dựa trên DeepTutor interface/semantics thực tế chứ không đoán trước bằng vài bảng `learner_memories` chung chung.

---

# 16. Ownership theo database V5

| Database | Owner service | Nhóm dữ liệu chính |
| :--- | :--- | :--- |
| `user_db` | `user-service` | identity, roles, learner profile, learning goal, `learning_activities`, `streaks` (user V5) |
| `access_db` | `access-service` | plans, subscriptions, activation key, point wallet |
| `content_db` | `content-service` | curriculum, KP, packages, question bank, bài học (V5.1); content V7 xóa 5 bảng catalog cũ |
| `assessment_db` | `assessment-service` | formal attempts/results/error analysis/grading |
| `learning_db` | `learning-service` | tiến độ topic/bài, bài nộp, bài ôn, mã đề, bằng chứng mastery theo user, version kết quả thi (§7) |
| `library_db` | `library-service` | 5 bảng catalog từ vựng/video (library V1), 6 bảng thư viện cá nhân (library V2) |
| `game_db` | `game-service` | game/realtime durable state |
| `notification_db` | `notification-service` | notification/reminder/delivery |
| `community_db` | `community-service` | post/comment/reaction |

Không có `ai_assistant_db`; service và DB hỗ trợ học viên cũ đã gỡ.

---

# 17. Baseline cuối

```text
Business tables: 85  (baseline V5 lịch sử)
Outbox tables:    9  (baseline V5 lịch sử)
Physical total: 94  (baseline V5 lịch sử)
```

V5.1–V5.3 đã triển khai: +5 bảng Content (§5.13–§5.17, content V8). Learning Service thay toàn bộ nhóm AI Learning bằng 9 bảng (§7, learning V1).

Quan trọng hơn số lượng bảng là boundary:

```text
learning_db (learning-service)
        = learner progress, reviews, test assignments and mastery evidence

content_db
        = canonical learning content

assessment_db
        = formal measurement authority

library_db
        = vocabulary/video catalog + personal library, not adaptive authority

user_db
        = identity + learning activity/streak, not adaptive authority
```

Đây là Database V5 baseline cho kiến trúc DeepTutor-core.
