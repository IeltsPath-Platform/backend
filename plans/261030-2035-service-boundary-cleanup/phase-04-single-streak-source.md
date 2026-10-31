---
phase: 4
title: "Một nguồn streak"
status: pending
priority: P2
effort: "1-1.5 ngày"
dependencies: []
---

# Phase 04: Một nguồn streak

## Overview

Hiện có hai nguồn streak. User-service giữ `streaks` do client gửi lên (`PUT /api/learning-support/streak`); learning-service có `learner_streaks` (V28) tính ở server từ câu trả lời thật và là nguồn duy nhất cho thưởng (`AGENTS.md` §3.8). Hồ sơ công khai vẫn đọc nguồn cũ: `PublicProfileRepositoryAdapter` join `streaks` (dòng 31, 50) và `PublicProfileResponse.streak`; `GET /api/learning-support/streak` mà frontend gọi (`httpLearningApi.ts` ~dòng 234) cũng đọc nguồn cũ.

Quyết định D4 (đã chốt): **giữ hiển thị streak ở community, lấy từ learning theo lô.** Community đã lấy hồ sơ công khai theo lô qua `UserPublicProfileClient` (cài `PublicProfileClient.findProfiles(Set<UUID>)` và `searchProfiles`); record `PublicProfile(id, displayName, avatarUrl, bio, targetBand, Integer streak)` đã có trường `streak` nullable; frontend (`CommunityPage.tsx` dòng ~651 và ~817) chỉ hiện streak khi khác null.

## Requirements

- **Learning-service**: thêm endpoint nội bộ theo lô trả streak hiện tại của nhiều học viên trong một lời gọi, ví dụ `POST /internal/learning/streaks` với body danh sách `userIds` (giới hạn số phần tử, cùng giới hạn với endpoint hồ sơ công khai của user-service), trả `{ userId, currentDays }` cho học viên có streak; học viên không có streak thì vắng mặt trong kết quả. Đọc từ `learner_streaks` qua repository có sẵn (`JdbcStreakRepository`), một truy vấn cho cả lô, không gọi trong vòng lặp (`AGENTS.md` §3.7). Learning-service hiện chưa có controller `/internal/**`: xem cách `InternalUserController` của user-service được bảo vệ và cách `config-repo`/security cho phép `/internal/**`, rồi theo đúng mẫu đó (kiểm tra cả cấu hình bảo mật của learning-service có cho phép đường dẫn này và chặn truy cập từ ngoài Gateway).
- **Community-service**: `UserPublicProfileClient` sau khi nhận hồ sơ từ user-service (cả `findProfiles` và `searchProfiles`) gọi learning đúng một lần cho cả danh sách và điền `streak` vào `PublicProfile`. Learning không phản hồi hoặc lỗi thì để `streak = null` (giao diện đã chịu được), không làm hỏng việc tải hồ sơ. Thêm cấu hình URL learning cho community (`LEARNING_SERVICE_URL` trong `community-service.yaml` và `docker-compose.yml`, cấu hình tập trung, commit riêng). Thêm cạnh community → learning vào bảng phụ thuộc.
- **User-service**: bỏ trường `streak` khỏi `PublicProfileResponse`/`PublicProfileResult` và bỏ join `streaks` trong `PublicProfileRepositoryAdapter`. Community phải chịu được phản hồi user-service không còn `streak` (đọc trường vắng mặt là null) để triển khai lệch phiên bản không vỡ: làm community trước, user-service sau.
- **Ngừng ghi nguồn cũ**: `PUT /api/learning-support/streak` (`StreakController`, `UpsertStreakUseCase`) bị gỡ sau khi frontend không còn gọi. `GET /api/learning-support/streak`: frontend chuyển sang endpoint streak của learning đã có (xem `docs/contracts/gamification-v1.md`); tìm mọi chỗ gọi bằng `grep -rn "learning-support/streak" frontend/src`. Nếu thấy chỗ cần giữ route đọc cũ vì lý do khác, báo cáo thay vì giữ hai giá trị khác nhau.
- Bảng `streaks` trong `user_db` giữ nguyên (không xóa).

## Tests first (red)

- Learning: endpoint lô trả đúng streak cho lô có học viên có và không có streak, giới hạn kích thước lô, từ chối khi gọi không đúng cách xác thực nội bộ, đọc trong một truy vấn.
- Community: `UserPublicProfileClient` điền `streak` từ learning cho cả `findProfiles` và `searchProfiles`; learning lỗi hoặc quá thời gian → `streak = null` và hồ sơ vẫn trả; user-service trả phản hồi không có `streak` vẫn đọc được; chỉ một lời gọi tới learning mỗi lô.
- User-service: hồ sơ công khai không còn trường `streak`; `UpsertStreak` không còn được gọi; cập nhật `UpsertStreakUseCaseTest` và test controller liên quan.
- Frontend: test của `CommunityPage` vẫn qua với `streak` null và có giá trị; chỗ đọc streak học viên đã chuyển nguồn.

## Related Code Files

- Learning: controller nội bộ mới và use case đọc lô streak (`JdbcStreakRepository`, `StreakRepository`).
- Community: `infrastructure/client/UserPublicProfileClient.java`, `application/port/PublicProfileClient.java`, `application/result/PublicProfile.java`, cấu hình client.
- User-service: `PublicProfileRepositoryAdapter`, `PublicProfileResponse`, `GetPublicProfilesUseCase`, `StreakController`, `UpsertStreakUseCase`, `GetStreakUseCase`, `StreakJpaEntity` và các lớp liên quan.
- Config tập trung (commit riêng): `community-service.yaml`, `docker-compose.yml` (community-service), `api-gateway.yaml` route `user-activity-streak-service` nếu phần streak thôi đi qua user-service.
- Frontend: `httpLearningApi.ts`, và nơi khác `grep` tìm ra.
- Tài liệu: `docs/contracts` có nhắc streak hoặc hồ sơ công khai, `docs/system-architecture.md` (cạnh community → learning).

## Implementation Steps

1. Viết test đỏ (learning endpoint, community client, user-service, frontend).
2. Learning: endpoint lô. Community: gọi và ghép. Triển khai hai phần này trước.
3. User-service: bỏ `streak` khỏi hồ sơ công khai và ngừng ghi. Frontend: đổi nguồn đọc streak học viên.
4. Cập nhật tài liệu và bảng phụ thuộc.

## Success Criteria

- [ ] Chỉ còn một nguồn streak cho mọi nơi hiển thị (learning).
- [ ] Trang community vẫn hiện streak; khi learning lỗi chỉ ẩn streak.
- [ ] Không có vòng mới: community → learning chỉ một chiều (learning không gọi community; kiểm bằng `grep` cấu hình).
- [ ] Bảng `streaks` vẫn còn trong `user_db`; test xanh.

## Risk Assessment

- Thứ tự triển khai: community và learning trước, user-service và frontend sau, để không có lúc nào hồ sơ mất streak vì hai bên lệch phiên bản.
- Thêm một lời gọi mỗi lần tải danh sách hồ sơ: một lô một lời gọi, có timeout ngắn và trả null khi lỗi nên ảnh hưởng độ trễ nhỏ.
- Rollback: bật lại join và controller (không đổi dữ liệu).
