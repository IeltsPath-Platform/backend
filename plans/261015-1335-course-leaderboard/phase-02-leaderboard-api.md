---
phase: 2
title: "Leaderboard API + learner names"
status: pending
dependencies: [1]
---

# Pha 2: API bảng xếp hạng

## Dữ kiện đã xác minh

- `topic_test_assignments` (V1) và `course_test_assignments` (V7) lưu `consumed_attempt_id`, `consumed_at`, `percent` cho **mọi** lần thi đã nộp, kể cả trượt (`ApplyAssessmentResultUseCase.applyTopicGate/applyCourseGate` → `assignment.consume(attemptId, percent)`). Thi lại = assignment mới. → Bảng thi đọc thẳng từ đây, **không cần bảng mới, không cần backfill**.
- Regrade sau khi đã consume bị bỏ qua (log "without an open assignment") → bảng giữ điểm lần chấm đầu, đúng quyết định.
- `LearningContentClient.Topic.course()` cho biết topic thuộc course nào; `CourseController` đã có route `/api/learning/courses/**`.
- Pattern gọi service khác: `infrastructure/client/RestAccessClient.java` + `GatewayRequestHeaders` (forward bearer + correlation id), endpoint nội bộ `/internal/**` bị gateway chặn với client (`access-service InternalPointsController`).
- user-service: `User.fullName` (bắt buộc khi đăng ký); `LearnerProfile.displayName`, `avatarReference`, `visibility` (PUBLIC/COMMUNITY/PRIVATE) chỉ có khi hồ sơ đã được tạo.

## API

`GET /api/learning/courses/{courseId}/leaderboard?board=LEARNING|TOPIC_TEST|FINAL_TEST&topicId=&limit=10`

- `limit` 1..50 (mặc định 10). `TOPIC_TEST` bắt buộc `topicId` thuộc course (400 nếu không). `FINAL_TEST` khi course không có bài thi → `available=false`.
- Response:
  ```json
  { "board": "LEARNING", "courseId": "…", "topicId": null, "available": true,
    "totalParticipants": 37,
    "entries": [{ "rank": 1, "userId": "…", "displayName": "Lan", "avatarUrl": null,
                  "score": 180.00, "lessonCount": 2, "isMe": false }],
    "me": { "rank": 12, "score": 95.5, "lessonCount": 1 } }
  ```
  `me = null` khi tôi chưa có điểm ở bảng này. `lessonCount` chỉ có ở LEARNING. Score là decimal từ units/percent; FE không tự tính hạng.
- Quyền: learner đã đăng nhập; không bị chặn bởi bài ôn pending hay lesson khóa. Identity từ `CurrentUserProvider`.

## SQL (một query mỗi bảng, CTE rank trên toàn population rồi mới cắt)

- LEARNING: `SELECT user_id, SUM(score_units), COUNT(*) FROM lesson_scores WHERE topic_id = ANY(:topicIds) GROUP BY user_id` → `RANK() OVER (ORDER BY total DESC)`; `topicIds` lấy từ content (một lần gọi `getTopicSequence`, lọc theo course).
- TOPIC_TEST: `DISTINCT ON (user_id) ... FROM topic_test_assignments WHERE topic_id = :topicId AND consumed_at IS NOT NULL ORDER BY user_id, consumed_at` → rank theo `percent`.
- FINAL_TEST: tương tự với `course_test_assignments WHERE course_id = :courseId`.
- Mỗi query trả top `limit` + dòng của tôi + `COUNT(*)` trong cùng statement (`WHERE rn <= :limit OR user_id = :me`). Thứ tự hiển thị trong nhóm đồng hạng: `user_id` (chỉ để ổn định, không ảnh hưởng `RANK`).

## Tên học viên

**user-service (tạo):** `POST /internal/users/display-names` body `{ "userIds": [...] }` (≤ 51, dedupe) → `[{ userId, displayName, avatarUrl }]`.
- Có hồ sơ PRIVATE → bỏ (learning hiện ẩn danh). Có hồ sơ PUBLIC/COMMUNITY → `displayName` hồ sơ. Không có hồ sơ → `fullName`. Không trả email/SĐT/bio.
- Chỉ đọc, không tạo hồ sơ (không dùng `GetLearnerProfileUseCase`). Một query batch cho users + một cho profiles.
- Files: `api/controller/InternalUserNamesController.java`, `application/usecase/GetDisplayNamesUseCase.java`, method batch trong `UserRepository`/`LearnerProfileRepository` + adapter JPA; security cho `/internal/**` theo pattern access-service.

**learning-service (tạo):** `application/port/LearnerNamesClient.java` + `infrastructure/client/RestLearnerNamesClient.java` (copy cấu hình timeout 2s/5s của `RestAccessClient`, property `learning.user.base-url`). Gọi **một lần** sau khi đọc SQL xong. Lỗi/timeout/thiếu → "Học viên ẩn danh", không làm hỏng response.

**learning-service (tạo):** `application/usecase/GetLeaderboardUseCase.java`, `domain/repository/LeaderboardRepository.java`, `infrastructure/persistence/JdbcLeaderboardRepository.java`, `api/controller/LeaderboardController.java` (hoặc thêm vào `CourseController` nếu gọn hơn), DTO response.

## Test

- `LeaderboardIntegrationTest` (Testcontainers): tổng 180 trên 100; đồng điểm cùng hạng `1,2,2,4`; tôi hạng 12 ngoài top 10 vẫn có `me`; tôi chưa có điểm → `me = null`; lesson topic khóa khác không lọt vào; thi chặng 55 rồi 85 → 55; thi trượt vẫn có mặt; topic không thuộc course → 400.
- `RestLearnerNamesClient` lỗi → tên ẩn danh, điểm vẫn đúng (unit test với stub).
- user-service: `GetDisplayNamesUseCaseTest` — PRIVATE bị bỏ, không hồ sơ → fullName, không tạo hồ sơ mới.

Lệnh: `mvn -pl services/learning-service,services/user-service -am test`. Gateway: kiểm tra `/internal/users/**` bị chặn từ ngoài (theo cấu hình hiện có).
