---
type: database-design
version: V5
status: deeptutor-core-baseline
updated: 2026-09-22
scope: IELTSPath MVP
external_baseline: HKUDS/DeepTutor v1.6.9
---

# Database — IELTSPath V5

> V5 **xóa hoàn toàn Adaptive Engine tự thiết kế ở V4**. `ai_learning_db` được thiết kế lại quanh persistence semantics của **DeepTutor Mastery Path + session runtime**. Không còn `mastery_records`, `review_events`, `topic_progress`, `daily_plans`, `daily_tasks`, `ai_conversations`, `ai_messages` hay `ai_assistant_db`.

> DeepTutor là adaptive learning engine duy nhất. `content_db` vẫn là canonical IELTS curriculum/content; `assessment_db` vẫn là formal assessment; `user_db` vẫn là identity/profile/learning goal. `ai_learning_db` chỉ giữ **Adaptive Learning Core / Tutor Runtime** của DeepTutor. Learning activity, streak, video progress, note và flashcard được tách sang `learning_support_db`, thuộc `learning-support-service`, để AI Learning không mang learner utility state không liên quan đến ADP core.

## 0. Lịch sử phiên bản

| Phiên bản | Thay đổi chính |
| :--- | :--- |
| **V1** | Baseline database đầu tiên cho Identity, Content, Assessment, Learning, Personal Library, Notification, Community và AI Chat. |
| **V2** | Review business MVP: Activation Key + Point + Premium; AI grading dùng point; Human Grading dùng Premium quota; ADP cũ dùng DeepTutor-driven mastery; hoàn thiện vocabulary và role nghiệp vụ. |
| **V3** | Tách `game-service`/`game_db`; thêm realtime multiplayer; Personal Library gộp vào `learning-service`. |
| **V4** | Thêm Video Learning qua YouTube URL/Video ID; baseline **87 bảng nghiệp vụ + 9 outbox = 96 bảng vật lý**. |
| **V5** | **Bỏ Adaptive Engine cũ và `ai-assistant-service`**. Learning chuyển sang DeepTutor core. Xóa 6 bảng adaptive cũ + `topic_gate_attempts` + `mistake_notebook_entries` + 2 bảng AI Chat; thêm persistence cho DeepTutor Mastery Path, interaction/evidence/event, tutor session/message/turn runtime và Question Notebook practice. Adaptive core được tách thành `ai-learning-service`/`ai_learning_db`; activity/streak/video progress/note/flashcard chuyển sang `learning-support-service`/`learning_support_db`. Content MVP bỏ adaptive topic graph/gate, bỏ vocab↔KP mapping, nhúng question options vào version, gộp asset links và defer package↔vocabulary mapping. Còn **85 bảng nghiệp vụ + 9 outbox = 94 bảng vật lý**. |

---

## 1. Mục đích

Tài liệu này là baseline database cho kiến trúc IELTSPath V2, trong đó `ai-learning-service` chạy Python/FastAPI và DeepTutor là lõi Adaptive Learning duy nhất; `learning-support-service` giữ learner-owned utility state ngoài ADP core.

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
| Canonical IELTS content | Chỉ `content_db` sở hữu |
| Formal assessment | Chỉ `assessment_db` sở hữu |

## 1.1 Tổng quan baseline V5

| Nhóm | Số bảng nghiệp vụ |
| :--- | ---: |
| Identity | 8 |
| Plans / Activation Key / Point | 8 |
| Content | **16** |
| Assessment | 10 |
| AI Learning / DeepTutor Core | **13** |
| Learning Support | **8** |
| Game học tập | 7 |
| Notification | 5 |
| Community | 3 |
| Quiz / Leaderboard | 4 |
| Grading core | 3 |
| **Tổng nghiệp vụ** | **85** |
| Transactional Outbox | **9 bảng vật lý** |
| **Tổng baseline** | **94 bảng** |

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
= canonical IELTS topics / KPs / question bank / video metadata

assessment_db
= formal attempts / results / item results / formal error analysis

ai_learning_db
= DeepTutor LearningProgress + mastery evidence + review/runtime state
+ tutor sessions/turns

learning_support_db
= learning activity + streak + video learner progress/bookmark
+ personal notes/flashcards

user_db
= identity / learner profile / learning goal
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
# 4. Content — Chủ đề, knowledge point và từ vựng

## 4.1 `topics`

Lưu taxonomy chủ đề chuẩn của IELTSPath để phân loại content. `topics` chỉ mô tả nội dung/curriculum; nó **không** biểu diễn prerequisite, trạng thái unlock hay adaptive progression. `parent_topic_id` nếu dùng chỉ phục vụ phân cấp hiển thị/phân loại.

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
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

Khoảng band là metadata nội dung, không phải trạng thái học. `ai-learning-service` dùng nó để chọn phạm vi path theo band mục tiêu của goal, và để test-out từ placement. Cả hai đầu NULL nghĩa là "mọi band".


## 4.2 `knowledge_points`

Định nghĩa canonical knowledge point trong curriculum. `content-service` chỉ sở hữu định nghĩa và metadata của KP; `ai-learning-service` map các KP này sang DeepTutor `KnowledgePoint` để theo dõi mastery và quyết định adaptive learning.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `topic_id` | uuid | FK → `topics` | Định danh topic liên quan. |
| `code` | — | — | Mã nghiệp vụ ổn định. |
| `name` | — | — | Tên hiển thị. |
| `learning_type` | varchar(20) | CHECK | DeepTutor `KnowledgeType`: `MEMORY`, `CONCEPT`, `PROCEDURE`, `DESIGN`. Đây là type dùng cho mastery/policy trong `ai-learning-service`. |
| `skill?` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `description` | — | — | Mô tả chi tiết. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `band_min?` | numeric(2,1) | CHECK như `topics` | Khoảng band riêng của KP (xem dưới). |
| `band_max?` | numeric(2,1) | CHECK như `topics` | Khoảng band riêng của KP (xem dưới). |
| `created_at` | — | — | Thời điểm tạo bản ghi. |
| `updated_at` | — | — | Thời điểm cập nhật gần nhất. |

Band hiệu lực của KP: nếu KP có khoảng band riêng (ít nhất một đầu khác NULL) thì dùng **trọn** khoảng đó, ngược lại dùng khoảng của topic. Không ghép một đầu của KP với đầu kia của topic, để không bao giờ ra khoảng ngược (`min > max`). Topic con không kế thừa band của topic cha. API trả cả khoảng riêng (`bandMin`/`bandMax`) và khoảng hiệu lực (`effectiveBandMin`/`effectiveBandMax`).

`learning_type` phải map 1:1 sang DeepTutor `KnowledgeType`. Các category nghiệp vụ như Grammar/Vocabulary/Strategy (nếu bổ sung sau) chỉ là metadata phân loại content, không thay thế `learning_type`.

`learning_type` là `MEMORY | CONCEPT | PROCEDURE | DESIGN`, ánh xạ 1:1 sang DeepTutor `KnowledgeType`; không suy ra từ `kind`. Trạng thái `ACTIVE` yêu cầu `learning_type IS NOT NULL`. Migration V2 chuyển các Knowledge Point active chưa được phân loại sang `INACTIVE`, giữ nguyên dữ liệu và chờ content editor phân loại trước khi publish lại.

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

# 5. Content — Đề, bài học và câu hỏi

## 5.1 `content_packages`

Đại diện cho một bộ nội dung hoàn chỉnh như mock test, placement test, practice set, quiz, game set hoặc lesson. Content không sở hữu khái niệm plan `FREE/PREMIUM`; nếu package cần entitlement thì chỉ khai báo `required_feature_key`, còn quyết định user có quyền truy cập thuộc `access-service`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `code` | — | — | Mã nghiệp vụ ổn định. |
| `title` | — | — | Tiêu đề. |
| `package_type` | — | — | Loại package. |
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

Invariant: `rules` không phải adaptive policy store. Mọi mastery gate, prerequisite học tập, retention/review scheduling và quyết định `next_objective()` thuộc `ai-learning-service` / DeepTutor.

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
| `weight` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `PK(question_version_id, knowledge_point_id)` | — | `PK(question_version_id, knowledge_point_id)` | Khóa chính tổng hợp. |


## 5.8 `content_assets`

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

`topic_prerequisites` và `topic_gate_rules` **không còn trong V5**. IELTSPath không duy trì một topic-unlock engine song song; thứ tự objective, mastery gate, review và `next_objective()` thuộc DeepTutor trong `ai-learning-service`.

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
| `topic_id?` | uuid | FK → `topics` | Topic chính liên quan tới video nếu cần phân loại/recommend. |
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
| `attempt_type` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `mode` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `channel` | — | — | Thuộc tính nghiệp vụ của bảng. |
| `status` | — | — | Trạng thái hiện tại của bản ghi. |
| `started_at?` | — | — | Thời điểm bắt đầu nếu có. |
| `submitted_at?` | — | — | Thời điểm submit nếu có. |
| `expires_at?` | — | — | Thời điểm hết hạn nếu áp dụng. |
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


## 6.3 `attempt_items`

Từng câu hỏi thực tế được giao trong attempt. Mỗi record là một câu hỏi thực tế được giao cho learner, kèm snapshot question/answer/knowledge để kết quả cũ không đổi.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của bản ghi. |
| `attempt_section_id` | uuid | FK → `attempt_sections` | Định danh đối tượng `attempt_section` liên quan. |
| `question_version_id` | uuid | Logical ref ↗ `Content` | Định danh phiên bản câu hỏi liên quan. |
| `sort_order` | — | — | Thứ tự hiển thị/xử lý. |
| `question_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
| `answer_snapshot` | jsonb | — | Tham chiếu tới đối tượng liên quan. |
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
| `video_id` | uuid | Logical ref ↗ `Content.learning_videos` | Video đang luyện. |
| `segment_id` | uuid | Logical ref ↗ `Content.video_segments` | Segment cụ thể được dùng cho attempt. |
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

# 7. AI Learning — DeepTutor Core & Tutor Runtime

Phần này là **persistence adapter cho DeepTutor**, không phải một Adaptive Engine do IELTSPath tự thiết kế. Source behavior nằm ở DeepTutor `LearningProgress`, `mastery.py`, `policy.py`, `scheduler.py`, `service.py` và session runtime.

Baseline upstream: **HKUDS/DeepTutor v1.6.9**.

DeepTutor upstream lưu Mastery Path trong workspace-scoped SQLite với một aggregate `state_json`, revision compare-and-swap, interaction lifecycle, event log và evidence projection. V5 port các semantics đó sang PostgreSQL để phù hợp microservice/multi-user của IELTSPath.

## 7.0 Quy tắc ownership và mapping

```text
DeepTutor KnowledgePoint.id
→ reference `content_db.knowledge_points.id`

DeepTutor LearningModule
→ snapshot/grouping được build từ IELTS curriculum

DeepTutor LearnerProfile
→ snapshot/context lấy từ user-service

Formal assessment evidence
→ source từ assessment-service

Tutor-generated question
→ nằm trong mastery interaction/session runtime
→ KHÔNG tự trở thành canonical question bank
```

`mastery_paths.state_json` được phép giữ snapshot module/KP cần cho một learning path ổn định, nhưng authoring/canonical content vẫn chỉ sửa ở `content-service`.

### DeepTutor `LearningProgress` chứa gì?

Upstream v1.6.9 hiện chứa các state chính:

```text
learner_profile
modules / knowledge_points
diagnostic
mastery_levels
qualitative_mastery
knowledge_types
quiz_attempts
error_records
learning_evidence
repetition_states
review_queue
learner_mastery_overrides
pending_question
feynman retries/explanations
stage failure state
version
```

V5 **không tách từng field trên thành một bộ bảng custom mới**. Aggregate JSONB là source of truth; các bảng projection chỉ tồn tại khi upstream semantics cần query/concurrency riêng.

---

## 7.1 `mastery_paths`

Source of truth của một DeepTutor `LearningProgress` aggregate. Đây là bảng thay thế vai trò tổng hợp của `mastery_records + topic_progress + daily plan state` cũ; tuy nhiên semantics hoàn toàn theo DeepTutor, không theo rule V4.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `path_id` | uuid | PK | ID learning/mastery path. Adapter map vào `LearningProgress.book_id`. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner sở hữu path. Bắt buộc vì PostgreSQL V5 là multi-user shared DB thay vì per-user workspace. |
| `learning_goal_id?` | uuid | Logical ref ↗ `Identity.learning_goals` | Goal dùng để build/contextualize path; chỉ là reference, không copy ownership. |
| `state_json` | jsonb | NOT NULL | Serialized DeepTutor `LearningProgress`. Đây là aggregate state chính. |
| `revision` | bigint | NOT NULL | Optimistic revision / compare-and-swap giống upstream. |
| `owner_session_id?` | uuid | Logical ref → `sessions` | Session sở hữu scratch/ad-hoc path nếu có; path curriculum chính có thể `NULL`. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo path. |
| `updated_at` | timestamptz | NOT NULL | Lần mutation gần nhất. |

Index đề xuất:

```text
(user_id, updated_at DESC)
(learning_goal_id)
UNIQUE (user_id, learning_goal_id) WHERE learning_goal_id IS NOT NULL
```

Partial unique index trên bảo đảm mỗi learning goal có tối đa một mastery path thuộc learner đó. Các path không gắn goal (`learning_goal_id IS NULL`) vẫn có thể tồn tại nhiều bản ghi cho cùng user.

Không tạo các cột `WEAK / LEARNING / MASTERED` riêng. Display status được derive bằng DeepTutor policy từ aggregate.

---

## 7.2 `mastery_path_sessions`

Liên kết conversation/tutor session với Mastery Path. Semantics giữ từ upstream: một session chỉ thuộc tối đa một path tại một thời điểm.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `path_id` | uuid | FK → `mastery_paths` | Path được session sử dụng. |
| `session_id` | uuid | FK → `sessions` | Tutor/chat session. |
| `created_at` | timestamptz | NOT NULL | Lần bind đầu tiên. |
| `last_seen_at` | timestamptz | NOT NULL | Hoạt động gần nhất của session trên path. |
| `PK(path_id, session_id)` | — | PK | Membership identity. |
| `UQ(session_id)` | — | UNIQUE | Ngăn một conversation được claim đồng thời bởi hai path. |

---

## 7.3 `mastery_interactions`

Persist lifecycle của một learner-facing mastery question/interaction. Bảng này rất quan trọng để grading deterministic qua nhiều turn: expected answer được giữ server-side trong `question_json`, không phụ thuộc LLM phải “nhớ” câu trả lời đúng ở turn sau.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `interaction_id` | uuid | PK | ID interaction. |
| `path_id` | uuid | FK → `mastery_paths` | Path chứa interaction. |
| `status` | varchar(30) | NOT NULL | `REGISTERED`, `AWAITING_INPUT`, `ANSWERED`, `GRADED`, `ABANDONED`. |
| `question_json` | jsonb | NOT NULL | DeepTutor `PendingQuestion`, gồm prompt/type/options/expected answer/explanation/difficulty. |
| `session_id?` | uuid | FK → `sessions` | Session tạo interaction. |
| `turn_id?` | uuid | Logical/FK → `turns` khi có | Turn tạo/grade interaction. |
| `user_answer` | text | DEFAULT '' | Câu trả lời learner. |
| `result_json` | jsonb | DEFAULT `{}` | Kết quả grade/metadata. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo. |
| `updated_at` | timestamptz | NOT NULL | Lần cập nhật gần nhất. |

Ràng buộc quan trọng:

```text
mỗi path chỉ có tối đa một active interaction
status ∈ REGISTERED/AWAITING_INPUT/ANSWERED
```

PostgreSQL nên dùng partial unique index tương đương upstream.

---

## 7.4 `mastery_events`

Append-only event log nội bộ của DeepTutor Mastery Path. Dùng cho recovery, audit và future live UI. Đây **không phải** integration outbox.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | bigint | PK identity | Event sequence vật lý. |
| `path_id` | uuid | FK → `mastery_paths` | Path phát event. |
| `revision` | bigint | NOT NULL | Aggregate revision sau commit. |
| `event_type` | varchar(100) | NOT NULL | Ví dụ path created/saved, interaction/topic events. |
| `payload_json` | jsonb | DEFAULT `{}` | Payload nội bộ. |
| `session_id?` | uuid | — | Session liên quan nếu có. |
| `turn_id?` | uuid | — | Turn liên quan nếu có. |
| `created_at` | timestamptz | NOT NULL | Thời điểm commit. |

Index:

```text
(path_id, revision, id)
```

---

## 7.5 `mastery_learning_evidence`

Projection/index của DeepTutor `LearningEvidence`. Aggregate `state_json` vẫn giữ evidence theo model DeepTutor; bảng này tồn tại để query theo knowledge point/time và ingest formal evidence hiệu quả mà không scan toàn bộ JSONB.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `path_id` | uuid | FK → `mastery_paths` | Path nhận evidence. |
| `ordinal` | bigint | NOT NULL | Thứ tự evidence ổn định trong aggregate. |
| `knowledge_point_id` | uuid | Logical ref ↗ `Content.knowledge_points` | KP chịu tác động. |
| `occurred_at` | timestamptz | NOT NULL | Thời điểm evidence xảy ra. |
| `source` | varchar(50) | NOT NULL | Ví dụ `mastery_path`, `placement`, `practice`, `mock`, `topic_gate`. |
| `source_reference_id?` | uuid | IELTSPath extension | ID formal source như `item_result_id`; dùng idempotency/correlation. |
| `assessment_type` | varchar(30) | NOT NULL | DeepTutor baseline: `quiz`, `qualitative`, `review`; adapter có thể map formal evidence vào type tương thích. |
| `result` | varchar(20) | NOT NULL | `correct`, `incorrect`, `partial`. |
| `quality?` | numeric(5,4) | `0..1` | Review/evidence strength normalized. |
| `hints_used` | integer | DEFAULT 0 | Số hint đã dùng. |
| `attempt_count` | integer | DEFAULT 1 | Số lần thử. |
| `confidence?` | numeric(5,4) | `0..1` | Confidence nếu có. |
| `response_time_seconds?` | numeric | — | Thời gian phản hồi. |
| `session_id?` | uuid | — | Tutor session nếu evidence đến từ tutor. |
| `turn_id?` | uuid | — | Tutor turn nếu có. |
| `evidence_json` | jsonb | NOT NULL | Payload DeepTutor đầy đủ để replay/audit. |
| `PK(path_id, ordinal)` | — | PK | Identity projection theo aggregate. |

Index:

```text
(path_id, knowledge_point_id, occurred_at DESC)
(path_id, source, source_reference_id)
```

`source_reference_id` là extension của IELTSPath để ingest event từ `assessment-service` idempotently. Nếu model DeepTutor được mở rộng, field này phải nằm trong fork/model adapter chứ không được tạo một mastery pipeline riêng.

---

## 7.6 `mastery_path_leases`

Giữ invariant “một mutating turn được quyền sửa path tại một thời điểm”, tương đương upstream `MasteryPathLease`.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `path_id` | uuid | PK, FK → `mastery_paths` | Path đang bị lease. |
| `session_id` | uuid | FK → `sessions` | Session sở hữu mutation. |
| `turn_id` | uuid | UQ, FK → `turns` | Turn đang mutate path. |
| `acquired_at` | timestamptz | NOT NULL | Thời điểm lấy lease. |

Lease được release khi turn kết thúc/cancel/recovery. Nếu implementation chọn Redis lock thay vì PostgreSQL row, bảng này có thể trở thành recovery/audit adapter; nhưng **single-mutator invariant phải giữ**.

---

## 7.7 `sessions`

Shared Tutor/Chat session runtime theo semantics DeepTutor session store. Đây thay `ai_conversations` cũ và đồng thời là learning-session conversation state.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Session ID. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Owner; extension cần cho centralized multi-user PostgreSQL. |
| `title` | varchar(200) | DEFAULT `New conversation` | Tên session. |
| `compressed_summary` | text | DEFAULT '' | Summary lịch sử dài để context compression. |
| `summary_up_to_message_id?` | bigint | Logical ref → `messages.id` | Message boundary đã được summary. |
| `preferences_json` | jsonb | DEFAULT `{}` | Session/surface preferences. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo. |
| `updated_at` | timestamptz | NOT NULL | Lần hoạt động gần nhất. |
| `archived_at?` | timestamptz | — | Archive/soft-hide khi cần. |

Index:

```text
(user_id, updated_at DESC)
```

---

## 7.8 `messages`

Persist message history của Tutor Agent/session. DeepTutor hỗ trợ message branching bằng `parent_message_id`; V5 giữ behavior này.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | bigint | PK identity | Message ID. |
| `session_id` | uuid | FK → `sessions` | Session chứa message. |
| `role` | varchar(30) | NOT NULL | `user`, `assistant`, `system/tool` theo runtime contract. |
| `content` | text | DEFAULT '' | Nội dung hiển thị/lưu. |
| `capability` | varchar(100) | DEFAULT '' | Capability xử lý message. |
| `events_json` | jsonb | DEFAULT `[]` | Render/runtime events gắn vào message. |
| `attachments_json` | jsonb | DEFAULT `[]` | Metadata attachment nếu surface hỗ trợ. |
| `metadata_json` | jsonb | DEFAULT `{}` | Metadata mở rộng. |
| `parent_message_id?` | bigint | FK self → `messages` | Parent trên active conversation branch. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo. |

Index:

```text
(session_id, created_at, id)
(session_id, parent_message_id)
```

---

## 7.9 `turns`

Durable runtime state cho mỗi agent turn. Dùng cho concurrency, resume/recovery và WebSocket rehydration.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Turn ID. |
| `session_id` | uuid | FK → `sessions` | Session chứa turn. |
| `capability` | varchar(100) | DEFAULT '' | Ví dụ mastery/tutor capability. |
| `status` | varchar(30) | NOT NULL | `QUEUED`, `RUNNING`, `WAITING_INPUT`, `COMPLETED`, `FAILED`, `CANCELLED`. |
| `error` | text | DEFAULT '' | Error text an toàn cho runtime/debug. |
| `owner_id` | varchar(255) | DEFAULT '' | Worker owner. |
| `fencing_token` | bigint | DEFAULT 0 | Ngăn stale worker commit. |
| `state_version` | bigint | DEFAULT 1 | Runtime state version. |
| `failure_code` | varchar(100) | DEFAULT '' | Machine-readable failure code. |
| `retryable` | boolean | DEFAULT false | Turn có thể retry hay không. |
| `assistant_message_id?` | bigint | FK → `messages` | Final assistant message nếu đã materialize. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo. |
| `updated_at` | timestamptz | NOT NULL | Lần cập nhật gần nhất. |
| `finished_at?` | timestamptz | — | Thời điểm terminal. |

Index:

```text
(session_id, updated_at DESC)
(session_id, status, updated_at DESC)
```

Nên có partial unique constraint bảo đảm một session không có hai active turns cạnh tranh nếu runtime contract yêu cầu.

---

## 7.10 `turn_events`

Append-only ordered event stream của một agent turn, dùng để stream/replay UI và khôi phục trace sau refresh.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | bigint | PK identity | Event row ID. |
| `turn_id` | uuid | FK → `turns` | Turn sở hữu event. |
| `seq` | integer | NOT NULL | Sequence tăng dần trong turn. |
| `type` | varchar(100) | NOT NULL | Loại stream event. |
| `source` | varchar(100) | DEFAULT '' | Agent/tool/capability source. |
| `stage` | varchar(100) | DEFAULT '' | Stage runtime nếu có. |
| `content` | text | DEFAULT '' | Nội dung stream. |
| `metadata_json` | jsonb | DEFAULT `{}` | Tool args/result/render hints đã sanitize khi cần. |
| `occurred_at` | timestamptz | NOT NULL | Timestamp event logic. |
| `created_at` | timestamptz | NOT NULL | Timestamp persist. |
| `UQ(turn_id, seq)` | — | UNIQUE | Bảo đảm deterministic replay order. |

---


## 7.11 `notebook_entries`

Durable Question Notebook của DeepTutor. Bảng này lưu **câu hỏi mà learner đã thực sự làm** sau khi interaction/assessment được grade. Nó khác `mastery_interactions`: `mastery_interactions` quản lý lifecycle câu hỏi đang chờ trả lời, còn `notebook_entries` là history/search/review source sau khi câu hỏi đã được materialize.

Các nguồn điển hình gồm Tutor Mastery Path, generated quiz/deep question và — nếu product policy bật — formal assessment item được import sang mistake review. Canonical IELTS question bank vẫn thuộc `content-service`; bảng này chỉ giữ learner-specific attempt/question snapshot.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | bigint | PK identity | ID notebook entry. |
| `session_id` | uuid | FK → `sessions` | Session sở hữu entry; với formal import có thể dùng system/import session theo application policy. |
| `turn_id` | uuid? | Logical/FK → `turns` khi có | Tutor/agent turn tạo assessment. |
| `question_id` | varchar(255) | NOT NULL | ID câu hỏi trong source runtime/canonical source. |
| `question` | text | NOT NULL | Snapshot nội dung câu hỏi learner đã làm. |
| `question_type` | varchar(50) | DEFAULT '' | Loại câu hỏi. |
| `options_json` | jsonb | DEFAULT `{}` | Options snapshot khi là choice question. |
| `correct_answer` | text | DEFAULT '' | Đáp án đúng snapshot; không gửi ngược ra pending question trước khi learner trả lời. |
| `explanation` | text | DEFAULT '' | Giải thích/reference explanation. |
| `difficulty` | varchar(50) | DEFAULT '' | Difficulty metadata nếu có. |
| `user_answer` | text | DEFAULT '' | Câu trả lời learner. |
| `source` | varchar(50) | NOT NULL | Ví dụ `mastery_path`, `deep_question`; IELTSPath adapter có thể mở rộng cho formal assessment import. |
| `material_id` | varchar(255) | DEFAULT '' | Source/material/path reference theo DeepTutor assessment semantics. |
| `material_title` | text | DEFAULT '' | Snapshot title dùng hiển thị. |
| `section_id` | varchar(255) | DEFAULT '' | Section/objective reference. |
| `section_title` | text | DEFAULT '' | Snapshot section title. |
| `assessment_type` | varchar(30) | DEFAULT '' | `quiz`, `qualitative`, `review` hoặc compatible adapter value. |
| `result` | varchar(20) | DEFAULT '' | `correct`, `incorrect`, `partial`, `ungraded` theo assessment contract. |
| `mastery_path_id?` | uuid | FK → `mastery_paths` khi source là mastery path | Path linkage cho Tutor Mastery. |
| `knowledge_point_id?` | uuid | Logical ref ↗ `Content.knowledge_points` | KP linkage; không FK xuyên service. |
| `attempt_count` | integer | DEFAULT 1, CHECK >= 1 | Số attempt liên quan. |
| `hints_used` | integer | DEFAULT 0, CHECK >= 0 | Số hint đã dùng. |
| `confidence?` | numeric(5,4) | `0..1` | Confidence nếu surface có thu thập. |
| `response_time_seconds?` | numeric | — | Response time nếu có. |
| `quality?` | numeric(5,4) | `0..1` | Normalized quality/evidence strength. |
| `is_correct` | boolean | DEFAULT false | Compatibility/read filter; `result` là representation giàu hơn. |
| `resolved` | boolean | DEFAULT false | Mistake/question đã được giải quyết chưa. |
| `bookmarked` | boolean | DEFAULT false | Learner bookmark entry. |
| `created_at` | timestamptz | NOT NULL | Thời điểm tạo. |
| `updated_at` | timestamptz | NOT NULL | Lần cập nhật gần nhất. |
| `UQ(session_id, turn_id, question_id)` | — | UNIQUE | Giữ idempotency tương đương DeepTutor assessment notebook. |

Index đề xuất:

```text
(session_id, created_at DESC)
(source, mastery_path_id, knowledge_point_id)
(result, resolved, updated_at DESC)
(bookmarked, created_at DESC)
```

Không dùng `notebook_entries` làm mastery authority. Mastery vẫn được mutation thông qua DeepTutor `LearningService` và `mastery_paths.state_json`.

---

## 7.12 `practice_review_state`

Current review state cho **một question notebook entry cụ thể**. Đây là question-level spaced practice của DeepTutor, khác với KP-level `repetition_states/review_queue` nằm trong `LearningProgress`.

```text
KP-level retention
→ mastery_paths.state_json.repetition_states

Question-level mistake review
→ practice_review_state
```

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `entry_id` | bigint | PK, FK → `notebook_entries` ON DELETE CASCADE | Question notebook entry được schedule. |
| `is_mistake` | boolean | DEFAULT true | Entry hiện đang thuộc mistake review hay không. |
| `first_wrong_at` | timestamptz | NOT NULL | Lần sai đầu tiên làm entry vào review flow. |
| `due_at` | timestamptz | NOT NULL | Lần review kế tiếp. |
| `interval_days` | numeric | DEFAULT 1 | Interval hiện tại theo ngày. |
| `ease` | numeric | DEFAULT 2.5 | Ease factor của question-level practice scheduler. |
| `streak` | integer | DEFAULT 0 | Consecutive successful review count. |
| `lapses` | integer | DEFAULT 0 | Số lần quên/sai lại. |
| `review_count` | integer | DEFAULT 0 | Tổng số review. |
| `last_review_at?` | timestamptz | — | Lần review gần nhất. |
| `version` | bigint | DEFAULT 0 | Optimistic version, ngăn stale review submit. |

Index:

```text
(due_at, entry_id)
(is_mistake, due_at)
```

Bảng này không thay thế DeepTutor Mastery scheduler. Hai scheduler phục vụ hai granularities khác nhau: **Knowledge Point** và **specific question/mistake**.

---

## 7.13 `practice_review_events`

Append-only/idempotent history của từng lần learner review một `notebook_entries` item. `practice_review_state` là current state; bảng này giải thích state đã thay đổi qua những lần review nào.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `request_id` | uuid | PK | Idempotency key cho một review submission. |
| `entry_id` | bigint | FK → `notebook_entries` ON DELETE CASCADE | Entry được review. |
| `rating` | varchar(20) | NOT NULL | Rating theo Practice scheduler contract, ví dụ `again`, `hard`, `good`, `easy`. |
| `answer` | text | NOT NULL | Answer learner submit trong lần review. |
| `reviewed_at` | timestamptz | NOT NULL | Thời điểm review. |
| `outcome_json` | jsonb | NOT NULL | Snapshot kết quả schedule/state sau review để audit/idempotency. |

Index:

```text
(entry_id, reviewed_at DESC)
```

Không tạo `practice_imports` trong MVP vì IELTSPath chưa có requirement import Question Notebook/practice file. `notebook_categories` và `notebook_entry_categories` cũng để sau, không thuộc baseline V5 hiện tại.

---

## 7.14 Dữ liệu DeepTutor không tạo bảng riêng trong V5

### Mistake Notebook

Không còn `mistake_notebook_entries`. Mistake Review được derive từ:

```text
DeepTutor LearningProgress.error_records
mastery_interactions
mastery_learning_evidence
notebook_entries
practice_review_state / practice_review_events
+
assessment-service.error_analysis_items
```

Nếu sau này cần read model/search chuyên biệt, có thể thêm projection, nhưng projection không được trở thành mastery authority.

### Daily Plan

Không còn `daily_plans` / `daily_tasks`. `GET /learning/today` là projection runtime từ current path + due reviews + goal/time budget.

### Topic Progress

Không còn `topic_progress` table. Topic/path progress được derive từ DeepTutor map/progress. Formal Topic Gate result nằm ở Assessment và được ingest vào DeepTutor.

### Learner Memory

DeepTutor v1.6.9 có memory subsystem L1/L2/L3 riêng. V5 **chưa normalize memory thành OLTP tables**. `ai-learning-service` phải cung cấp production memory storage adapter tách biệt (object/document/vector storage tùy layer) và scope theo `user_id`. Không dùng local per-user workspace làm production authority.

### RAG index

Vector embeddings/index không nằm trong `ai_learning_db` baseline; quản lý bởi vector/RAG storage. Canonical source metadata vẫn thuộc `content-service`.

---

## 7.15 Formal evidence ingest rule

Formal result từ `assessment-service` không ghi trực tiếp `state_json` bằng SQL. Flow bắt buộc:

```text
AssessmentCompleted.v2
      ↓
ai-learning-service application layer
      ↓
map item result → DeepTutor LearningEvidence / attempt
      ↓
DeepTutor LearningService
      ↓
mastery/scheduler/policy mutation
      ↓
PostgreSQL AI LearningStore transaction
```

Cấm consumer update `mastery_paths.state_json` bằng ad-hoc SQL.

---

## 7.16 Transaction boundary

Một mutating tutor turn cần commit atomically các dữ liệu liên quan khi phù hợp:

```text
mastery_paths revision/state_json
mastery_interactions
mastery_events
mastery_learning_evidence projection
notebook_entries / practice review state-events khi turn tạo hoặc review question
turn status/events
ai_learning_db.outbox_events
```

Implementation có thể chia transaction theo runtime architecture, nhưng phải giữ idempotency và không để trạng thái mastery commit trong khi durable interaction/evidence bị mất.

---

# 8. Learning Support — Tracking, Video Progress và Personal Library

Phần này thuộc **`learning-support-service`**, database **`learning_support_db`**. Service này giữ learner-owned utility state và được tách khỏi `ai-learning-service` để AI Learning chỉ tập trung vào DeepTutor ADP/Tutor core.

Ownership boundary:

```text
learning-support-service
├── Learning Tracking
│   ├── learning_activities
│   ├── streaks
│   ├── video_learning_progress
│   └── saved_video_segments
└── Personal Library
    ├── notes
    ├── flashcard_decks
    ├── flashcards
    └── flashcard_deck_items
```

Quy tắc:

- không bảng nào trong `learning_support_db` là mastery authority;
- service này không chạy DeepTutor policy/scheduler và không quyết định `next_objective()`;
- video canonical metadata/segment vẫn thuộc `content_db`;
- activity phát sinh từ Tutor/Assessment/Game được nhận qua API/event contract, không query database service khác;
- `user_id`, `video_id`, `vocabulary_sense_id` và các source ID xuyên service chỉ là logical reference, không physical FK.

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

Giữ learner-owned progress của YouTube learning. Bảng này không phải DeepTutor mastery state. `ai-learning-service` có thể đọc progress qua API/event contract khi cần context học video.

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Progress ID. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner. |
| `video_id` | uuid | Logical ref ↗ `Content.learning_videos` | Video. |
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
| `video_id` | uuid | Logical ref ↗ `Content.learning_videos` | Video nguồn. |
| `segment_id` | uuid | Logical ref ↗ `Content.video_segments` | Segment. |
| `transcript_snapshot` | text | — | Snapshot đoạn được lưu. |
| `note?` | text | — | Ghi chú learner. |
| `created_at` | timestamptz | NOT NULL | Thời điểm lưu. |
| `UQ(user_id, segment_id)` | — | UNIQUE | Không lưu trùng. |

---

## 8.5 `notes`

Ghi chú cá nhân của learner. Bảng lưu note tự do để learner tự ghi lại kiến thức, mẹo làm bài hoặc nội dung cần nhớ. MVP không dùng tag và không gắn note vào knowledge point.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh duy nhất của note. |
| `user_id` | uuid | Logical ref ↗ `Identity.users` | Learner sở hữu note. |
| `title` | varchar(255) | — | Tiêu đề note. |
| `body` | text | — | Nội dung note. |
| `status` | — | — | Trạng thái như `ACTIVE`, `ARCHIVED`, `DELETED`. |
| `created_at` | timestamptz | — | Thời điểm tạo. |
| `updated_at` | timestamptz | — | Thời điểm cập nhật gần nhất. |

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
| `source_type` | varchar(50) | — | Nguồn tạo flashcard, ví dụ `MANUAL`, `VOCABULARY_SENSE`, `HIGHLIGHT`. |
| `vocabulary_sense_id?` | uuid | Logical ref ↗ `Content.vocabulary_senses` | Có giá trị khi flashcard được tạo từ một nghĩa/từ loại cụ thể trong kho vocabulary. Đây là field thay cho `entry_version_id`. |
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
- Khi bắt đầu match, `game-service` nên lấy/snapshot đủ content cần thiết thay vì gọi `content-service` cho từng WebSocket message.
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

Lưu kết quả từng item trong một game session. Bảng này dùng để tính score, hiển thị lịch sử đúng/sai và phân tích hiệu suất chơi game. Game MVP không được ingest trực tiếp vào DeepTutor mastery, vì vậy bảng không có `knowledge_point_id`; nếu tương lai game trở thành learning evidence thì phải đi qua adapter của `ai-learning-service`.

Thuộc tính chính:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc / Quan hệ | Chức năng / Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | uuid | PK | Định danh câu/item trong lịch sử game. |
| `session_id` | uuid | FK → `game_sessions` | Game session chứa item này. |
| `item_sequence` | integer | — | Thứ tự item trong session. |
| `vocabulary_sense_id?` | uuid | Logical ref ↗ `Content.vocabulary_senses` | Nghĩa/từ loại cụ thể nếu item thuộc Vocabulary. Có thể để `NULL` với Grammar game. |
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

Các database có outbox trong V5:

```text
user_db.outbox_events
access_db.outbox_events
content_db.outbox_events
assessment_db.outbox_events
ai_learning_db.outbox_events
learning_support_db.outbox_events
game_db.outbox_events
notification_db.outbox_events
community_db.outbox_events
```

Tổng **9 bảng outbox vật lý**. Không còn `ai_assistant_db.outbox_events` vì `ai-assistant-service` đã bị xóa.

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
| `user_db` | `user-service` | identity, roles, learner profile, learning goal |
| `access_db` | `access-service` | plans, subscriptions, activation key, point wallet |
| `content_db` | `content-service` | curriculum, KP, vocabulary, packages, question bank, video metadata |
| `assessment_db` | `assessment-service` | formal attempts/results/error analysis/grading |
| `ai_learning_db` | `ai-learning-service` | DeepTutor adaptive learning, mastery, tutor runtime, Question Notebook review |
| `learning_support_db` | `learning-support-service` | learning activity, streak, video progress/bookmarks, notes, flashcards |
| `game_db` | `game-service` | game/realtime durable state |
| `notification_db` | `notification-service` | notification/reminder/delivery |
| `community_db` | `community-service` | post/comment/reaction |

Không có `ai_assistant_db`.

---

# 17. Baseline cuối

```text
Business tables: 85
Outbox tables:    9
Physical total: 94
```

Quan trọng hơn số lượng bảng là boundary:

```text
DeepTutor state in ai_learning_db
        = sole adaptive authority

content_db
        = canonical learning content

assessment_db
        = formal measurement authority

learning_support_db
        = learner-owned utility state, not adaptive authority
```

Đây là Database V5 baseline cho kiến trúc DeepTutor-core.
