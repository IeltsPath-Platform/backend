# Community: những gì đã làm, API đã dùng, thay đổi ở các service khác

- Ngày: 2026-10-18. Nhánh: `feat/community-service`. **Toàn bộ thay đổi chưa commit.**
- Nguồn: mã nguồn hiện tại và kết quả chạy thật trên Docker local (không chỉ báo cáo test).
- Liên quan: `docs/contracts/community-events-v1.md` (hợp đồng sự kiện), `plans/261016-1045-community-realtime-feed/`, `plans/261016-community-enhancements/`.

## 1. Tổng quan

Phân hệ Cộng đồng gồm 3 backend và 1 frontend:

| Thành phần | Vai trò | Mức thay đổi |
| --- | --- | --- |
| `community-service` (8089, `community_db`) | Bài viết, bình luận, cảm xúc, follow, bookmark, tìm kiếm, mention, báo cáo; ghi outbox và relay sang RabbitMQ | Mở rộng lớn (88 file), migration V2 đến V6 |
| `notification-service` (8088, `notification_db`) | Nhận sự kiện, lưu thông báo, tùy chọn thông báo, đẩy realtime qua WebSocket | Trước đó chỉ là khung; nay làm đủ (11 file), migration V1 đến V3 |
| `user-service` (8085) | Cung cấp hồ sơ công khai cho community | Thêm 3 endpoint, không đổi luật cũ (14 file) |
| `api-gateway` (8080) | Route WebSocket và nhận token qua subprotocol | Thêm 1 filter + 1 route |
| Frontend (`src/features/community`) | Giao diện, WebSocket STOMP, test | Mới hoàn toàn |

Luồng sự kiện: `community-service` ghi `outbox_events` trong cùng transaction với thao tác, relay (chu kỳ 2 giây) gửi lên exchange `community.events`, `notification-service` nhận, lưu `notifications` rồi đẩy xuống trình duyệt qua STOMP `/user/queue/notifications`.

## 2. community-service

### 2.1 Cơ sở dữ liệu (Flyway, migration mới, không sửa V1)

| Migration | Nội dung |
| --- | --- |
| V2 `enhance_community_features` | `posts`: thêm `image_urls` (JSONB, tối đa 4), `video_url`, `target_band` (0 đến 9, bước 0.5); danh mục mở rộng: GENERAL, QUESTION, DISCUSSION, WRITING_SHARING, SPEAKING_PARTNER, RESOURCE_TIPS; cảm xúc mở rộng: LIKE, LOVE, HELPFUL, ACCURATE (có bảng lưu trữ `post_reaction_archive` cho dữ liệu cũ); bảng follow và bookmark |
| V3 `schedule_outbox_retries` | `outbox_events.next_attempt_at` + index cho relay thử lại có lùi dần |
| V4 `index_following_category_feed` | Index cho feed theo người theo dõi và danh mục |
| V5 `add_community_post_search` | Index GIN `to_tsvector('simple', title || body)` cho tìm kiếm |
| V6 `add_community_reports` | Bảng `community_reports` (unique theo người báo cáo + loại + nội dung; trạng thái PENDING, DISMISSED, ACTIONED) |

### 2.2 API của community-service (qua Gateway: `/api/community/**`)

Tất cả cần đăng nhập. "ADMIN" nghĩa là bắt buộc role ADMIN.

| Method | Đường dẫn | Mục đích | Ghi chú |
| --- | --- | --- | --- |
| GET | `/posts` | Feed | Tham số: `pagination=cursor` (bắt buộc cho tìm kiếm và feed theo dõi), `size` (mặc định 10), `cursor`, `category`, `scope=ALL\|FOLLOWING`, `q` (tối đa 120 ký tự). Trả `content`, `nextCursor`, `hasNext`, `size` |
| POST | `/posts` | Tạo bài | Body: `category`, `title`, `body`, `imageUrls[]`, `videoUrl`, `targetBand` |
| GET | `/posts/{id}` | Chi tiết | Bài đã ẩn trả 404 |
| PUT | `/posts/{id}` | Sửa | Chỉ tác giả (người khác kể cả ADMIN nhận 403) |
| DELETE | `/posts/{id}` | Xóa | Tác giả hoặc ADMIN |
| PUT | `/posts/{id}/status` | Kiểm duyệt bài | Đã có từ trước (ADMIN) |
| GET / POST | `/posts/{postId}/comments` | Danh sách bình luận gốc / tạo bình luận hoặc trả lời | `parentCommentId` để trả lời; trả lời của trả lời gộp về bình luận gốc (2 cấp) |
| GET | `/comments/{commentId}/replies` | Danh sách trả lời | Phân trang |
| PUT / DELETE | `/comments/{id}` | Sửa / xóa bình luận | Sửa: chỉ tác giả (ADMIN nhận 403); Xóa: tác giả hoặc ADMIN |
| PUT | `/comments/{id}/status` | Kiểm duyệt bình luận | Đã có từ trước |
| PUT / DELETE | `/posts/{postId}/reactions/{type}` | Thả / bỏ cảm xúc | `type`: LIKE, LOVE, HELPFUL, ACCURATE. Mỗi người một cảm xúc mỗi bài |
| POST / DELETE | `/posts/{postId}/bookmark` | Lưu / bỏ lưu | 204 |
| GET | `/posts/bookmarked` | Bài đã lưu của tôi | Phân trang |
| POST / DELETE | `/users/{userId}/follow` | Theo dõi / bỏ theo dõi | Tự follow trả 400; trạng thái không đổi thì không phát sự kiện |
| GET | `/users/{userId}/followers`, `/following` | Danh sách | Cursor |
| GET | `/users/{userId}/posts` | Bài của một người | Phân trang |
| GET | `/users/{userId}/relationship` | Đang theo dõi, số người theo dõi, số đang theo dõi | |
| GET | `/users/mention-suggestions?q=` | Gợi ý @mention | `q` từ 2 đến 80 ký tự, tối đa 8 kết quả, loại trừ chính mình |
| POST | `/reports` | Báo cáo vi phạm | Body: `contentType` (POST hoặc COMMENT), `contentId`, `reason` (1 đến 1000 ký tự) |
| GET | `/admin/reports` | Hàng chờ báo cáo | ADMIN; phân trang `page`, `size` (tối đa 100); cũ nhất trước |
| PUT | `/admin/reports/{reportId}/decision` | Xử lý báo cáo | ADMIN; body `{"decision":"DISMISS"\|"HIDE_CONTENT"}` |

Đường dẫn trong plan nhưng **không làm**: `POST /posts/upload-image`, `POST /posts/upload-video`, `GET /users/{id}/public-profile` ở community (hồ sơ công khai lấy từ user-service, xem mục 3.1).

### 2.3 Quy tắc nghiệp vụ đã chốt

- **Media**: chỉ HTTPS, tối đa 4 ảnh, host phải nằm trong danh sách cho phép. Ảnh: `images.unsplash.com`, `i.imgur.com`. Video: `youtube.com`, `www.youtube.com`, `youtu.be`, `loom.com`, `www.loom.com`, `drive.google.com`. Đổi bằng biến `COMMUNITY_IMAGE_HOSTS`, `COMMUNITY_VIDEO_HOSTS`. Chưa có upload tệp (repo chưa có nơi lưu).
- **Mention**: định dạng `@[Tên](uuid)`; tối đa 5 người khác nhau mỗi bình luận; người phải tồn tại và hiển thị được; mention trùng chỉ tạo một sự kiện; tự mention không thông báo.
- **Tìm kiếm**: PostgreSQL full-text cấu hình `simple`, truy vấn gắn tham số, kết hợp được với danh mục, tab theo dõi và cursor.
- **Báo cáo**: mỗi người một lần cho mỗi nội dung (409 nếu trùng); không báo cáo nội dung của chính mình (403); `HIDE_CONTENT` ẩn nội dung và tự đóng mọi báo cáo PENDING khác của cùng nội dung trong một transaction; `DISMISS` chỉ đóng báo cáo được chọn; hai ADMIN xử lý đồng thời thì một bên nhận 409.
- **Cảm xúc**: bấm lại đúng loại đang chọn thì bỏ; chọn loại khác thì thay thế.
- **Sự kiện**: không thông báo cho chính mình; lặp lại hành động không đổi trạng thái không phát thêm sự kiện.

### 2.4 Sự kiện phát ra (hợp đồng đầy đủ ở `docs/contracts/community-events-v1.md`)

Exchange `community.events`: `community.post-commented.v1`, `community.comment-replied.v1`, `community.comment-mentioned.v1`, `community.post-reacted.v1`, `community.user-followed.v1`. Chỉ chứa định danh, không chứa nội dung bình luận hay thông tin riêng tư. Relay dùng publisher confirm, mandatory routing, thử lại lùi dần tối đa 1 giờ.

### 2.5 Cấu hình (config-repo, không có secret)

`community-service.yaml`: RabbitMQ (`RABBITMQ_*`), `community.user-service.base-url` (`USER_SERVICE_URL`), `community.media.*`, `community.messaging.exchange`, `community.outbox.relay.*` (bật bằng `COMMUNITY_OUTBOX_RELAY_ENABLED`, chu kỳ `PT2S`, lô 50, timeout xác nhận `PT5S`).

### 2.6 Xử lý lỗi

`GlobalExceptionHandler` trả về `ErrorResponse` thống nhất. Các ca đã được xử lý: enum sai trong JSON → 400; đường dẫn không tồn tại → 404; sai phương thức → 405; tranh chấp ghi → 409; lỗi bất ngờ → 500 (không lộ chi tiết).

## 3. Thay đổi ở các service khác

### 3.1 user-service (chỉ thêm)

| Method | Đường dẫn | Dùng bởi |
| --- | --- | --- |
| GET | `/api/users/{userId}/public-profile` | Frontend (qua Gateway): tên, avatar, bio, band mục tiêu, streak. Không trả email, số điện thoại hay role |
| POST | `/internal/users/public-profiles` | community-service: lấy hồ sơ theo lô (tránh N+1) |
| GET | `/internal/users/public-profiles/search` | community-service: gợi ý mention |

`/internal/**` không được Gateway route ra ngoài. community-service gọi trực tiếp qua `USER_SERVICE_URL` (mặc định `http://localhost:8085`; trong Compose `http://user-service:8085`). Không có migration mới ở user-service. `GlobalExceptionHandler` có chỉnh nhỏ.

### 3.2 notification-service (làm mới)

- Database `notification_db`, migration V1 (bảng `notifications`, index theo người nhận và chưa đọc), V2 (unique theo `(comment_id, recipient_id)` cho mention), V3 (bảng `notification_preferences`).
- Consumer RabbitMQ: khử trùng theo `eventId`, ACK sau khi commit, thử lại có hạn, hàng đợi lỗi (DLQ) cho sự kiện sai hợp đồng hoặc hết lượt thử.
- API (qua Gateway `/api/notifications/**`):

| Method | Đường dẫn | Ghi chú |
| --- | --- | --- |
| GET | `/api/notifications?size=` | Danh sách của tôi |
| GET | `/api/notifications/unread-count` | Trả `{"count": n}` |
| PUT | `/api/notifications/{id}/read` | Không phải của tôi hoặc không tồn tại → 404; gọi lại vẫn 200 |
| PUT | `/api/notifications/read-all` | |
| GET | `/api/notifications/preferences` | Liệt kê đủ 5 loại, mặc định bật |
| PUT | `/api/notifications/preferences/{eventType}` | Body `{"enabled": true\|false}`; loại sai → 400. Tắt thì không lưu và không đẩy |

- WebSocket: `/ws/notifications` (STOMP 1.2), đăng ký `/user/queue/notifications`; người dùng chỉ nhận thông báo của chính mình, không được gửi lên kênh; kiểm tra Origin theo `FRONTEND_ORIGIN` (mặc định `http://localhost:5173`).
- `pom.xml`: thêm `spring-boot-starter-amqp`, `spring-boot-starter-websocket` và Testcontainers (test).

### 3.3 api-gateway

- Route `notification-service-websocket` (`lb:ws://NOTIFICATION-SERVICE`, `/ws/notifications`) trong `api-gateway.yaml`.
- `WebSocketProtocolAuthFilter`: trình duyệt không gửi được header Authorization khi mở WebSocket, nên gửi token qua subprotocol `v12.stomp, bearer.<token>`. Filter kiểm tra token ngoài, ký lại JWT nội bộ ngắn hạn, bỏ subprotocol chứa token và chỉ đàm phán `v12.stomp`. Không hỗ trợ token trên query string.
- Hệ quả: handshake chỉ chấp nhận token qua subprotocol; gửi header `Authorization` trực tiếp cho `/ws/notifications` giờ trả 401.

### 3.4 Hạ tầng và tài liệu

| Tệp | Thay đổi |
| --- | --- |
| `docker-compose.yml` | Thêm service `notification-service`; community-service phụ thuộc RabbitMQ, user-service, notification-service; thêm biến RabbitMQ, `USER_SERVICE_URL`, `NOTIFICATION_DB_*`, `FRONTEND_ORIGIN` |
| `infra/postgres/init-databases.sql` | Thêm `CREATE DATABASE notification_db` (script chỉ chạy khi volume mới) |
| `infra/config-server/config-repo/*.yaml` | `api-gateway`, `community-service`, `notification-service` như mục trên |
| `docs/contracts/community-events-v1.md` | Hợp đồng sự kiện mới |
| `docs/system-architecture.md`, README các service | Cập nhật theo trạng thái hiện tại |

## 4. Frontend (tóm tắt)

Tuyến mới: `/community`, `/community/posts/:postId`, `/community/users/:userId`, `/community/admin/reports` (ADMIN), `/notifications/preferences`. Có nút chuông, toast, WebSocket tự kết nối lại. Nút cảm xúc kiểu Facebook (một nút "Thích", rê chuột hoặc nhấn giữ để mở 4 cảm xúc). Thêm thư viện: `@stomp/stompjs` (chạy thật), và `vitest`, `@testing-library/*`, `jsdom` (chỉ để test), script `npm run test`.

## 5. Lỗi phát hiện khi kiểm tra tích hợp thật và đã sửa

| Lỗi | Hậu quả | Cách sửa |
| --- | --- | --- |
| Ghi `outbox_events` với kiểu `Instant` không có kiểu SQL rõ | Follow, bình luận, cảm xúc trả 500 | Dùng `Timestamp` tường minh |
| Relay thiếu routing key `community.comment-mentioned.v1` | Không có thông báo mention nào | Thêm vào bảng routing; thêm test đối chiếu mọi `event_type` với routing key |
| Frontend dùng `uuid()` của zod 4 quá chặt | Bảng tin lỗi với ID tài khoản demo | Dùng `guid()` |
| Tab "Đang theo dõi" trùng tên tham số `tab=following` với tab hồ sơ | Tab trống, không gọi API | Chỉ coi là tab hồ sơ khi có `userId` |
| Test của notification-service import lớp của community-service | `docker compose build` thất bại | Gỡ phụ thuộc; test toàn tuyến đặt ở community-service, consumer test dùng JSON theo hợp đồng |
| Enum sai trong JSON | 500 | 400 |
| Image Gateway bản cũ, WebSocket 401 | Không có thông báo realtime | Build lại Gateway; thêm filter subprotocol |
| Đường dẫn không tồn tại (ví dụ `/posts/upload-image`) | 500 | 404 |
| Đánh dấu đã đọc thông báo của người khác trả 200 vô tác dụng | Gây hiểu nhầm | 404 |

## 6. Kết quả kiểm tra (2026-10-18)

- Chạy thật trên Docker local với tài khoản `learner@ielts.demo`, `admin@ielts.demo` và các tài khoản tạo thêm: feed, theo dõi, tìm kiếm, mention, tùy chọn thông báo, báo cáo, WebSocket, bookmark, bình luận và trả lời, sửa và xóa kèm quyền: **đạt**.
- Giao diện kiểm tra bằng trình duyệt headless (sự kiện giả lập; chuột và phím thật chưa kiểm tra): popup cảm xúc, bàn phím, reduced motion, không cuộn ngang ở 360 đến 1920 px: **đạt**.
- Maven (community, notification, user-service, gateway và common-security): `BUILD SUCCESS`, 0 lỗi và 0 bỏ qua. Số test: 76 (community), 16 (notification), 157 (user-service), 10 (gateway), 8 (common-security).
- Frontend: lint, typecheck, test (7 file, 33 test) và build đều đạt.
- Chi tiết kịch bản thử tay: `docs/community-manual-test-plan.md`.

## 7. Việc cần làm khi chạy hoặc triển khai

1. Build lại 4 image khi đổi mã: `docker compose up -d --build community-service notification-service user-service api-gateway`.
2. Máy đã có volume Postgres từ trước: tạo `notification_db` thủ công, ví dụ `docker exec ieltspath-postgres-1 sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE notification_db;"'`.
3. Sau khi tạo lại container, Gateway cần khoảng 1 đến 2 phút để định tuyến tới instance mới (tạm thời 503).
4. Docker Desktop khởi động lại có thể làm Postgres và RabbitMQ thoát với mã 255: chạy `docker compose start postgres rabbitmq`.

## 8. Giới hạn và việc còn lại

- Chưa có upload ảnh và video từ máy; chỉ dán URL thuộc host cho phép.
- `PUT /api/notifications/{id}/read` với id sai định dạng (ví dụ `abc`) đang trả 500, nên là 400.
- Outbox của access, content, game vẫn chưa có relay (có từ trước, ngoài phạm vi).
- Migration V6 của user-service (`study_time_tracking`) đã bị sửa sau khi áp dụng ở commit khác: máy khác có thể gặp lỗi checksum Flyway. Đây không phải thay đổi của Cộng đồng nhưng nên xử lý trước khi hợp nhất.
- Chưa chạy toàn bộ `mvn -B -ntp verify` cho cả reactor như CI; chưa có commit nào cho các thay đổi trên.
