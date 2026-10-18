# Kế hoạch triển khai Phân hệ Cộng đồng (Community) & Thông báo Realtime

- **Ngày lập**: 16/10/2026
- **Trạng thái**: Hoàn thành triển khai và kiểm thử tự động ngày 16/10/2026; chưa chạy luồng E2E hai tài khoản trên stack đang chạy.
- **Phạm vi**: Backend (`services/community-service`, `services/notification-service`, `services/user-service`) & Frontend (`src/features/community`, `src/features/notifications`)
- **Kiến trúc**: Microservices, Clean Architecture DDD, Event-Driven với RabbitMQ, Realtime STOMP WebSocket

---

## Quyết định đã duyệt và tiến độ thực thi

Phần này ưu tiên hơn các ví dụ thiết kế ban đầu bên dưới khi có khác biệt.

- WebSocket/STOMP với Spring WebSocket và `@stomp/stompjs` được duyệt.
- External JWT chỉ truyền bằng `Sec-WebSocket-Protocol` trong handshake; Gateway xác thực, chuyển internal JWT, loại token khỏi downstream subprotocol. Không đưa JWT vào URL/log.
- Media chỉ nhận URL HTTPS với allowlist host cấu hình được; chưa triển khai upload file. Embed chỉ YouTube/Loom đã kiểm tra.
- Exchange `community.events`, routing keys `community.post-commented.v1`, `community.comment-replied.v1`, `community.post-reacted.v1`, `community.user-followed.v1`; bổ sung cả ghi outbox và relay vì code cũ mới có bảng.
- Feed mới dùng `pagination=cursor`, keyset `(created_at,id)`, mặc định 10; giữ page/size và response legacy khi không chọn cursor.
- Một reaction/user/post; migration lưu bản sao các dòng legacy bị hợp nhất rồi giữ reaction mới nhất (tie-break xác định).
- Author được gom key và lấy bằng một batch HTTP call tới User Service mỗi trang; không truy vấn trong vòng lặp.
- Không đọc/ghi `.env`, không commit. Không sửa migration cũ. Các bước triển khai sau chỉ bắt đầu khi bước trước đã qua build/test; báo riêng kiểm thử cần Docker nếu môi trường không đáp ứng.

| Bước | Phạm vi | Trạng thái |
| --- | --- | --- |
| [1](phase-01-community-backend.md) | Community, public profiles, outbox | Hoàn thành |
| [2](phase-02-notification-backend.md) | Notification, RabbitMQ, WebSocket, Gateway | Hoàn thành |
| [3](phase-03-community-feed.md) | Feed, tạo/sửa bài, media, reaction | Hoàn thành |
| [4](phase-04-comments-profile.md) | Comments, profiles, follow | Hoàn thành |
| [5](phase-05-realtime-validation.md) | Chuông realtime, kiểm thử tích hợp | Hoàn thành; E2E trình duyệt chưa chạy |

Validation: `mvn -q -pl services/community-service,services/user-service -am test`, `mvn -q -pl services/notification-service test`, `mvn -q -pl infra/api-gateway test`, `mvn -q compile -DskipTests`, FE `npm run typecheck && npm run lint && npm run build`, `docker compose config --quiet`, and `git diff --check`. Details: [validation report](reports/implementation-validation.md).

---

## 1. TỔNG QUAN VÀ ĐỊNH VỊ TÍNH NĂNG

Xây dựng mạng xã hội học tập dành riêng cho học viên IELTS trên nền tảng IELTSPath theo phong cách tương tác của Facebook:
- **Tương tác cốt lõi**:
  - Đăng bài viết (Post) kèm đa hình ảnh (Images) hoặc **Video đính kèm** (Video file trực tiếp / Video link YouTube, Loom, Google Drive chuyên luyện Speaking).
  - Thả cảm xúc đa dạng (Reactions: Like, Love, Helpful, Accurate).
  - Bình luận và trả lời bình luận phân cấp lồng nhau (Threaded Comments & Replies 2 cấp).
  - Theo dõi học viên khác (Follow / Unfollow).
- **Newsfeed tối ưu**:
  - Tải vô tận (**Infinite Scroll**) từng cụm **10 bài viết** một lượt khi cuộn màn hình (Keyset / Cursor Pagination kết hợp Page/Size) tránh nghẽn mạng và lag DOM.
- **Trang cá nhân (Learner Profile)**:
  - Hiển thị thông tin cá nhân: Avatar, Display Name, Bio, Band mục tiêu (Target Band), Chuỗi ngày học liên tục (Streak).
  - Tabs nội dung:
    1. **Bài viết đã đăng**: Lịch sử bài viết của học viên đó (có infinite scroll).
    2. **Bài viết đã lưu (Bookmarks)**: Kho lưu trữ bài viết hữu ích riêng tư.
    3. **Thống kê bạn học**: Danh sách Người theo dõi (Followers) & Đang theo dõi (Following).
    4. **Nút Follow / Unfollow**: Tương tác trực tiếp từ trang cá nhân của bạn học.
- **Thông báo Realtime (WebSocket & Notification Service)**:
  - Khi có sự kiện (ai đó like bài viết, bình luận, trả lời bình luận, hoặc follow), `community-service` phát sự kiện qua **RabbitMQ**.
  - `notification-service` tiêu thụ sự kiện, lưu thông báo vào database và phát ngay lập tức xuống trình duyệt người nhận qua **WebSocket (STOMP)**.
  - Client hiển thị icon chuông thông báo nhảy số đỏ (Unread Count) và toast notification tức thời.

---

## 2. CÁC TÍNH NĂNG BỔ SUNG ĐỘC ĐÁO CHO CỘNG ĐỒNG IELTS

1. **Phân loại bài viết chuyên biệt IELTS (`PostCategory`)**:
   - `GENERAL`: Thảo luận chung, kinh nghiệm luyện thi, truyền động lực.
   - `WRITING_SHARING`: Đăng bài essay Writing Task 1/Task 2 nhờ bạn học sửa bài (Peer Review).
   - `SPEAKING_PARTNER`: Tìm bạn luyện nói 1-1 theo band mục tiêu (VD: *Cần tìm partner luyện Speaking band 7.0 mỗi tối*).
   - `RESOURCE_TIPS`: Chia sẻ từ vựng Collocation, tài liệu đề thi thực tế.
   - `QUESTION`: Hỏi đáp, giải thích câu hỏi khó trong đề Reading/Listening.
2. **Hệ thống Cảm xúc Học thuật (Academic Reactions)**:
   - `LIKE`: Thích.
   - `LOVE`: Yêu thích.
   - `HELPFUL`: Rất hữu ích / Mở mang kiến thức.
   - `ACCURATE`: Đồng tình / Lời giải chính xác.
3. **Lưu bài viết (Post Bookmarks / Saved Posts)**:
   - Giúp học viên bookmark các bài chia sẻ template, cấu trúc câu hay để ôn lại bất cứ lúc nào.
4. **Huy hiệu Target Band trên Avatar tác giả**:
   - Tự động hiển thị huy hiệu band điểm mục tiêu (VD: `Target 7.5`) cạnh tên tác giả dựa trên hồ sơ học viên.
5. **Tag bạn bè (@mention) trong comment**:
   - Hỗ trợ tag tên người được reply trong khung chat comment.

---

## 3. KIẾN TRÚC HỆ THỐNG VÀ LUỒNG DỮ LIỆU LIÊN DỊCH VỤ

```
                      [ Client Frontend (React / Vite) ]
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            │ HTTP /api/community/**  │ HTTP /api/notifications │ WebSocket /ws/notifications
            ▼                         ▼                         ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                       API Gateway (:8080)                        │
   └────────┬─────────────────────────┬─────────────────────────┬─────┘
            │ lb://COMMUNITY-SERVICE  │ lb://NOTIFICATION-SVC   │ lb:ws://NOTIFICATION-SVC
            ▼                         ▼                         ▼
   ┌──────────────────┐      ┌──────────────────┐               │
   │community-service │      │notification-serv.│◄──────────────┘
   │     (:8089)      │      │     (:8088)      │
   └────────┬─────────┘      └────────▲─────────┘
            │ (1) Write Outbox        │ (3) Consume Event
            ▼                         │
   ┌──────────────────┐      ┌────────┴─────────┐
   │   community_db   │      │ RabbitMQ Broker  │
   │ (outbox_events)  │─────►│ (Exchange:       │
   └──────────────────┘  (2) │  community.events│
                        Push └──────────────────┘
```

### Nguyên tắc kiến trúc:
- **Database riêng biệt**: `community-service` dùng `community_db`, `notification-service` dùng `notification_db`. Tuyệt đối không query chéo database.
- **Giao tiếp bất đồng bộ**: Dùng Transactional Outbox Pattern + RabbitMQ exchange `community.events` để đảm bảo độ tin cậy khi phát thông báo.
- **Bảo mật**: Xác thực thông qua Internal JWT do Gateway ký, kiểm tra quyền sở hữu bài viết/comment thông qua `CurrentUserProvider` (`common-security`).

---

## 4. THIẾT KẾ DATABASE (SCHEMA MIGRATIONS)

### 4.1. `community-service` (Flyway `V2__enhance_community_features.sql`)

```sql
-- 1. Hỗ trợ hình ảnh đính kèm, video và mục tiêu band cho bài viết
ALTER TABLE posts ADD COLUMN IF NOT EXISTS image_urls JSONB DEFAULT '[]'::jsonb;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS target_band NUMERIC(2,1);

-- 2. Mở rộng danh mục bài viết cho IELTS
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_category_check;
ALTER TABLE posts ADD CONSTRAINT posts_category_check 
    CHECK (category IN ('GENERAL','QUESTION','DISCUSSION','WRITING_SHARING','SPEAKING_PARTNER','RESOURCE_TIPS'));

-- 3. Mở rộng Reaction Types
ALTER TABLE post_reactions DROP CONSTRAINT IF EXISTS post_reactions_reaction_type_check;
ALTER TABLE post_reactions ADD CONSTRAINT post_reactions_reaction_type_check 
    CHECK (reaction_type IN ('LIKE','LOVE','HELPFUL','ACCURATE'));

-- 4. Bảng Theo dõi bạn học (User Follows)
CREATE TABLE IF NOT EXISTS user_follows (
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX IF NOT EXISTS idx_user_follows_followee ON user_follows(followee_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows(follower_id, created_at DESC);

-- 5. Bảng Lưu bài viết (Post Bookmarks / Saved Posts)
CREATE TABLE IF NOT EXISTS post_bookmarks (
    user_id UUID NOT NULL,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_post_bookmarks_user ON post_bookmarks(user_id, created_at DESC);
```

### 4.2. `notification-service` (Flyway `V1__create_notification_tables.sql`)

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'POST_COMMENT', 'COMMENT_REPLY', 'POST_REACTION', 'NEW_FOLLOWER'
    reference_id UUID NOT NULL, -- postId, commentId
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id, is_read, created_at DESC);
```

---

## 5. DANH SÁCH CHI TIẾT TẤT CẢ CÁC API

### 5.1. Nhóm Bài viết & Bảng tin (Posts & Feed) — `community-service`

| Method | Endpoint | Mô tả | Query / Payload |
|---|---|---|---|
| `GET` | `/api/community/posts` | Lấy Newsfeed (10 bài/lần, lướt vô tận) | `?page=0&size=10&category=...&cursor=...` |
| `POST` | `/api/community/posts` | Đăng bài viết mới kèm ảnh/video | Body: `{ category, title, body, imageUrls: string[], videoUrl?: string }` |
| `GET` | `/api/community/posts/{id}` | Lấy chi tiết một bài viết | Path: `id` |
| `PUT` | `/api/community/posts/{id}` | Chỉnh sửa nội dung, ảnh & video | Body: `{ category, title, body, imageUrls: string[], videoUrl?: string }` |
| `DELETE` | `/api/community/posts/{id}` | Xóa bài viết | Path: `id` (Chỉ tác giả hoặc ADMIN) |
| `POST` | `/api/community/posts/upload-image` | Tải ảnh đính kèm bài viết | Multipart form-data `file` -> trả về URL ảnh |
| `POST` | `/api/community/posts/upload-video` | Tải video ngắn đính kèm (hoặc dán link) | Multipart `file` hoặc gửi URL YouTube/Loom/Drive |

### 5.2. Nhóm Bình luận & Trả lời (Comments & Replies) — `community-service`

| Method | Endpoint | Mô tả | Query / Payload |
|---|---|---|---|
| `GET` | `/api/community/posts/{postId}/comments` | Lấy danh sách comment gốc (cấp 1) | `?page=0&size=10` |
| `GET` | `/api/community/comments/{commentId}/replies` | Lấy danh sách reply của một comment | `?page=0&size=10` |
| `POST` | `/api/community/posts/{postId}/comments` | Đăng bình luận hoặc trả lời | Body: `{ body: string, parentCommentId?: UUID }` |
| `PUT` | `/api/community/comments/{id}` | Sửa bình luận | Body: `{ body: string }` |
| `DELETE` | `/api/community/comments/{id}` | Xóa bình luận | Path: `id` |

### 5.3. Nhóm Cảm xúc & Lưu bài viết (Reactions & Bookmarks) — `community-service`

| Method | Endpoint | Mô tả | Query / Payload |
|---|---|---|---|
| `PUT` | `/api/community/posts/{postId}/reactions/{type}` | Thả cảm xúc (`LIKE`, `LOVE`, `HELPFUL`, `ACCURATE`) | Path: `postId`, `type` |
| `DELETE` | `/api/community/posts/{postId}/reactions/{type}` | Hủy thả cảm xúc | Path: `postId`, `type` |
| `POST` | `/api/community/posts/{postId}/bookmark` | Lưu bài viết vào kho cá nhân | Path: `postId` |
| `DELETE` | `/api/community/posts/{postId}/bookmark` | Bỏ lưu bài viết | Path: `postId` |
| `GET` | `/api/community/posts/bookmarked` | Xem danh sách bài viết đã lưu | `?page=0&size=10` |

### 5.4. Nhóm Trang cá nhân & Theo dõi (Profile & Follows)

| Method | Endpoint | Service | Mô tả |
|---|---|---|---|
| `GET` | `/api/users/{userId}/public-profile` | `user-service` | Lấy profile: Tên, avatar, bio, target band, streak. |
| `GET` | `/api/community/users/{userId}/posts` | `community-service` | Lịch sử bài viết của user đó (10 bài/lần). |
| `POST` | `/api/community/users/{userId}/follow` | `community-service` | Theo dõi bạn học. |
| `DELETE` | `/api/community/users/{userId}/follow` | `community-service` | Hủy theo dõi. |
| `GET` | `/api/community/users/{userId}/followers` | `community-service` | Danh sách người theo dõi. |
| `GET` | `/api/community/users/{userId}/following` | `community-service` | Danh sách người đang theo dõi. |

### 5.5. Nhóm Thông báo Realtime — `notification-service`

| Method / Protocol | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/notifications` | Lấy danh sách thông báo (`page=0&size=15`). |
| `GET` | `/api/notifications/unread-count` | Lấy số thông báo chưa đọc để hiện huy hiệu đỏ. |
| `PUT` | `/api/notifications/{id}/read` | Đánh dấu một thông báo đã đọc. |
| `PUT` | `/api/notifications/read-all` | Đánh dấu tất cả thông báo đã đọc. |
| **WebSocket** | `/ws/notifications` (STOMP) | Nhận thông báo realtime qua topic `/user/queue/notifications`. |

---

## 6. THIẾT KẾ GIAO DIỆN FRONTEND (FE ARCHITECTURE)

Thư mục mới: `src/features/community/` trong repo Frontend:

```text
src/features/community/
├── api/
│   ├── communityApi.ts              # API client cho bài viết, comment, reaction, follow
│   └── notificationApi.ts           # API client cho thông báo REST fallback
├── components/
│   ├── PostCard.tsx                 # Card bài viết hoàn chỉnh (Header, Content, Gallery, Actions)
│   ├── PostCreateModal.tsx          # Modal soạn bài viết kèm xem trước ảnh
│   ├── PostImageGallery.tsx         # Lưới hiển thị 1-4 ảnh phong cách Facebook
│   ├── ReactionPicker.tsx           # Thanh chọn icon Like, Love, Helpful, Accurate khi hover
│   ├── CommentList.tsx              # Danh sách comment và reply lồng nhau
│   ├── CommentItem.tsx              # Từng bình luận kèm nút Reply inline
│   ├── InfiniteFeed.tsx             # Wrapper xử lý tải vô tận với IntersectionObserver
│   └── FollowButton.tsx             # Nút Theo dõi / Đang theo dõi với phản hồi UI tức thì
├── hooks/
│   ├── useCommunityFeed.ts          # Hook quản lý nạp 10 bài/lần, lướt xuống nạp tiếp
│   ├── usePostReactions.ts          # Optimistic update khi bấm like/react
│   └── useRealtimeNotifications.ts    # Kết nối WebSocket STOMP nhận thông báo tức thời
├── pages/
│   ├── CommunityFeedPage.tsx        # Trang Bảng tin chính (3 cột)
│   ├── PostDetailPage.tsx           # Trang chi tiết bài viết
│   └── LearnerProfilePage.tsx       # Trang cá nhân của học viên
└── community.css                    # CSS hiện đại, tối ưu responsive
```

### Cơ chế Infinite Scroll (10 bản ghi/lượt):
- Dùng `IntersectionObserver` theo dõi thẻ sentinel `<div ref={bottomRef} />` ở cuối danh sách bài viết.
- Khi người dùng cuộn đến đáy trang, hook `useCommunityFeed` tự động kích hoạt `fetchNextPage()` với `page = page + 1` và `size = 10`.
- Hiển thị spinner nhẹ nhàng khi đang tải; giữ nguyên vị trí cuộn trang, nối dữ liệu mới vào cuối mảng (`posts = [...prev, ...newPosts]`).

---

## 7. LỘ TRÌNH THỰC HIỆN TỪNG BƯỚC (ROADMAP)

1. **Phase 1: Backend Database & Core Entities**
   - Viết migration `V2__enhance_community_features.sql` cho `community-service`.
   - Mở rộng Aggregate `Post` và các Command/UseCase để hỗ trợ `imageUrls`, `targetBand`.
   - Tạo Repository & UseCase cho tính năng Follow và Bookmark.
2. **Phase 2: Notification Service & Realtime STOMP**
   - Viết migration `V1__create_notification_tables.sql` cho `notification-service`.
   - Cấu hình Spring WebSocket STOMP endpoint `/ws/notifications`.
   - Lắng nghe RabbitMQ event từ `community-service` để tự động tạo thông báo và push qua WebSocket.
3. **Phase 3: Frontend Newsfeed, Post Creation & Infinite Scroll**
   - Dựng trang `CommunityFeedPage.tsx` và component `InfiniteFeed.tsx` (10 bài/lượt cuộn).
   - Xây dựng component soạn thảo bài viết `PostCreateModal.tsx` hỗ trợ đính kèm nhiều ảnh.
   - Hoàn thiện `ReactionPicker.tsx` (Like, Love, Helpful, Accurate).
4. **Phase 4: Frontend Threaded Comments & Learner Profile**
   - Xây dựng component `CommentList.tsx` hỗ trợ reply phân cấp 2 tầng.
   - Xây dựng `LearnerProfilePage.tsx` xem thông tin học viên, lịch sử bài viết, bài viết đã bookmark.
5. **Phase 5: WebSocket Integration & Kiểm thử toàn diện**
   - Tích hợp hook `useRealtimeNotifications.ts` vào Header app (icon chuông + popup toast).
   - Chạy toàn bộ test suite Backend (`mvn test`) và Frontend (`npm run lint; npm run typecheck; npm run build`).

