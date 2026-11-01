---
title: "Sửa ranh giới service: gỡ vòng gọi đồng bộ, đưa dữ liệu học tập về learning-service"
description: "Gỡ ba vòng gọi hai chiều (content-library, user-learning, user-assessment), sửa lỗi cấu hình client từ vựng, chuyển mục tiêu học và thời gian học từ user-service sang learning-service"
status: implemented
priority: P2
branch: "fix/service-boundary-cleanup, feat/study-data-to-learning"
tags: [architecture, service-boundary, migration, gateway, tdd]
blockedBy: []
blocks: []
created: "2026-10-30"
createdBy: "ck:plan"
source: service-boundary-plan.md (căn cứ code trên main, commit 1a7a7db)
---

# Sửa ranh giới service

Căn cứ: code trên `main` tại `1a7a7db` (merge PR #118, 2026-10-29). Mọi dữ kiện trong mục "Đã xác minh" được đối chiếu lại với code ngày 2026-10-30. Chủ dự án đã chốt hướng làm: chuyển dữ liệu học tập sang learning-service, **không tạo service mới**.

## Mục tiêu và nguyên tắc

**Mục tiêu:** không còn cặp service nào gọi HTTP đồng bộ hai chiều; dữ liệu học tập (mục tiêu, thời gian học, hoạt động học) nằm ở learning-service.

**Nguyên tắc (theo `backend/AGENTS.md`):**
- Không sửa migration đã áp dụng; mọi thay đổi schema là migration mới. Không xóa bảng hay dữ liệu: bảng cũ trong `user_db` được giữ nguyên, việc xóa là một đợt riêng cần duyệt, **không thuộc plan này**.
- Mỗi service chỉ đọc database của chính nó. Dữ liệu đi giữa service qua API, event hoặc script có đối soát.
- Hợp đồng công khai (đường dẫn, JSON request/response) **giữ nguyên** để frontend không gãy; chỉ đổi service đích ở Gateway.
- Mỗi phase kết thúc bằng test xanh và có cách quay lại.
- `AGENTS.md` cấm sửa Docker, config tập trung hay shared security như hệ quả phụ của task một service. Plan này **chủ động yêu cầu** sửa `docker-compose.yml`, `infra/config-server/config-repo/*` và Gateway ở các phase 01, 03, 06. Mỗi lần sửa phải nằm trong commit riêng, có mô tả lý do, để người sở hữu hạ tầng duyệt.

## Đã xác minh (không cần tìm lại)

| # | Dữ kiện | Nơi trong code |
|---|---|---|
| A | Content gọi library: `LibraryVocabularySenseClient` gọi `/api/content/vocabulary/senses` và `/api/content/admin/vocabulary/bulk`. Library gọi content: `ContentTopicClient` gọi `/api/content/topics/{id}` qua port `TopicLookup`, dùng trong `CreateLearningVideoUseCase` | `content-service/.../infrastructure/client/`, `library-service/.../infrastructure/client/`, `application/port/TopicLookup.java` |
| B | Learning gọi user (`RestLearnerGoalClient` → `/internal/users/{id}/learning-goal`, `RestDisplayNameClient`). User gọi learning (`HttpStudySourceVerifier` → `/api/learning/statistics/study-context`) | `learning-service/.../infrastructure/client/`, `user-service/.../infrastructure/client/HttpStudySourceVerifier.java` |
| C | Assessment gọi user (`UserExaminerClient`, `RestUserDirectory`). User gọi assessment (`HttpStudySourceVerifier` → `/api/assessments/me/statistics/study-context`) | `assessment-service/.../infrastructure/client/` |
| D | `user_db` giữ: `learning_goals` (V2, ràng buộc một mục tiêu active ở V4), `learning_activities` và `streaks` (V5), `study_tracking_policy`, `study_sessions`, `study_heartbeat_receipts`, `study_activity_intervals`, `study_daily_totals`, `study_goal_history` (V6). **8 bảng phải chuyển**; `streaks` xử lý ở phase 04 | `user-service/src/main/resources/db/migration` |
| E | Hai nguồn streak: user giữ `streaks` do client gửi; learning có `learner_streaks` (V28) tính ở server là nguồn duy nhất cho thưởng. Hồ sơ công khai đọc `streaks` (`PublicProfileRepositoryAdapter` dòng 31, 50), `PublicProfileResponse` có trường `streak` | `AGENTS.md` §3.8 |
| F | `LibraryVocabularySenseClient` đọc `${library.content.base-url:http://localhost:8082}`. Khóa này chỉ được định nghĩa trong `config-repo/library-service.yaml` (dòng 45-49, để library trỏ tới content). Content-service không có cấu hình nào, `docker-compose.yml` cũng không đưa URL library cho content-service; `localhost:8082` chính là cổng của content-service. Unit test dùng `http://library.test` nên không bắt được. **Chưa chạy thử thật** | `config-repo/content-service.yaml`, `docker-compose.yml` |
| G | `AGENTS.md` dòng 31 và `CLAUDE.md` dòng 36 ghi notification-service là khung/chưa có DB. Code có listener chấm bài, inbox, tùy chọn thông báo, migration V1-V4, `notification_db` trong Compose | `services/notification-service` |
| H | Mặc định DB trong config-repo: 5432 (access, assessment, community, content, notification, user), 5435 (game), 5436 (learning), 5437 (library). Compose chạy một PostgreSQL ở 5440 | `config-repo/*-service.yaml` |
| I | Trong user-service, `ChangePasswordUseCase`, `RefreshTokenUseCase`, `ResetPasswordUseCase` dùng port `UserLock` (cài bằng `AdvisoryUserLock`). `studySessions.lockUser` được gọi từ `RecordStudyHeartbeatUseCase`, `StartStudySessionUseCase` và `ScheduleStudyGoalUseCase.lock` (dùng bởi `CreateLearningGoalUseCase`, `ChangeLearningGoalStatusUseCase`, `UpdateLearnerProfileUseCase`). Hai nhóm dùng chung khóa vì cùng `AdvisoryUserLock.key` | `user-service/.../application/usecase`, `infrastructure/adapter` |
| J | Tạo mục tiêu, đổi trạng thái mục tiêu và sửa hồ sơ (khi đổi múi giờ) đều gọi `ScheduleStudyGoalUseCase.execute`, ghi `study_goal_history` trong cùng luồng. Vì vậy mục tiêu và thời gian học **không tách được thành hai bước** | `CreateLearningGoalUseCase`, `ChangeLearningGoalStatusUseCase`, `UpdateLearnerProfileUseCase` |
| K | `StartStudySessionUseCase` xác minh nguồn (`verifier.verify`) **trước** khi lấy khóa DB, rồi tạo phiên với múi giờ từ hồ sơ (`StudyTimezone.forUser`) | `user-service/.../StartStudySessionUseCase.java` |
| L | Learning đã gọi assessment bằng HTTP (`RestHumanReviewClient`, `RestEssayPromptActivationClient`); assessment không gọi learning (trong Compose chỉ có URL content, access, user) | `learning-service/.../infrastructure/client/` |
| M | Frontend đang gọi: `/api/users/me/learning-goals*` (`profileApi.ts`, `LearningGoalsSection.tsx`, `httpLearningApi.ts`), `/api/users/me/study-sessions*` (`StudyActivityTracker.tsx`), `GET /api/learning-support/streak` (`httpLearningApi.ts`). `CommunityPage.tsx`/`communityApi.ts` có dùng streak | `frontend/src` |
| Q | Community lấy hồ sơ công khai theo lô qua `UserPublicProfileClient` (cài `PublicProfileClient.findProfiles(Set<UUID>)`, `searchProfiles`); `PublicProfile(id, displayName, avatarUrl, bio, targetBand, Integer streak)`; frontend `CommunityPage.tsx` (~651, ~817) chỉ hiện streak khi khác null. Learning-service chưa có controller `/internal/**` | `community-service/.../infrastructure/client/UserPublicProfileClient.java` |
| N | Gateway: route `user-service` khớp `/auth/**,/api/users/**`; route `user-activity-streak-service` khớp `/api/learning-support/activities/**,/api/learning-support/streak/**`; route `learning-service` khớp `/api/learning/**`. Có test `LearningSupportRouteConfigTest`, `LibraryRouteConfigTest` đọc cấu hình | `config-repo/api-gateway.yaml`, `infra/api-gateway/src/test` |

Kiểm tra toàn bộ URL gọi giữa service trong `docker-compose.yml` và `config-repo`: ngoài A, B, C **không có vòng nào khác**.

## Quyết định đã chốt (mặc định Codex dùng; đổi chỉ khi chủ dự án nói)

| # | Câu hỏi | Quyết định |
|---|---|---|
| D1 | Library có kiểm topic của video không | Bỏ kiểm tra (cách A): library chỉ lưu `topicId` |
| D2 | Đường dẫn công khai sau khi chuyển | **Giữ nguyên đường dẫn cũ**; Gateway thêm route cụ thể hơn, đặt trước route `user-service`, rewrite sang `/api/learning/...`. Không đổi frontend trong plan này |
| D3 | Xác minh phiên của assessment | Learning xác minh nguồn `ASSESSMENT` bằng cách gọi endpoint `study-context` đã có của assessment (chiều learning → assessment đã tồn tại, assessment không gọi ngược). Nguồn `LESSON`/`PRACTICE`/`REVIEW` xác minh cục bộ trong learning. Thay cho phương án (a)(b)(c) của bản trước |
| D4 | Hồ sơ công khai có hiện streak không | **Có, lấy từ learning theo lô**: community gọi learning một lần cho cả danh sách hồ sơ và điền `streak` (null khi lỗi), user-service bỏ trường `streak`. Thêm cạnh community → learning một chiều (chủ dự án chọn) |
| D5 | Thời điểm | Phase 01, 02, 03, 04, 07 làm bất cứ lúc nào (phase 04 chỉ chạm learning ở một endpoint đọc mới; nếu paywall chưa merge thì không thêm migration). Phase 05, 06 chỉ bắt đầu **sau khi plan paywall đã merge vào `main`** (cùng sửa learning-service và cùng cần số migration). Các file plan này được commit lên nhánh docs riêng sau khi Codex làm xong paywall |
| D9 | Dữ liệu thật | Chỉ có dữ liệu dev/staging (chủ dự án xác nhận). Phase 06 vẫn đối soát số liệu nhưng **bỏ cửa sổ bảo trì** |
| D6 | Bảng cũ trong `user_db` | Giữ nguyên, đánh dấu deprecated trong tài liệu. Không có migration xóa trong plan này |
| D7 | Múi giờ khi mục tiêu sang learning | Learning lấy múi giờ học viên từ user-service qua endpoint nội bộ mới (chiều learning → user đã tồn tại), và xếp lại lịch mục tiêu khi thấy múi giờ khác với múi giờ đã dùng để xếp lịch |
| D8 | Cổng DB mặc định trong config-repo | Đồng nhất về `localhost:5440` (cổng Compose, `AGENTS.md` §3.6 đã hướng dẫn IDE dùng 5440) |

## Các phase

| Phase | Nội dung | Phụ thuộc | Trạng thái |
|---|---|---|---|
| 01 | [Sửa cấu hình client từ vựng của content](./phase-01-fix-content-library-client-config.md) | - | Pending |
| 02 | [Gỡ lời gọi library → content](./phase-02-remove-library-to-content-call.md) | 01 (cùng vùng, nên sau 01) | Pending |
| 03 | [Tài liệu và cổng DB mặc định](./phase-03-docs-and-config-defaults.md) | - | Pending |
| 04 | [Một nguồn streak](./phase-04-single-streak-source.md) | - | Pending |
| 05 | [Chuyển mục tiêu, thời gian học và hoạt động sang learning](./phase-05-build-study-data-in-learning.md) | paywall đã merge | Pending |
| 06 | [Cắt chuyển, Gateway, dọn user-service](./phase-06-cutover-and-cleanup.md) | 05 | Pending |
| 07 | [Chặn vòng phụ thuộc quay lại](./phase-07-dependency-guard.md) | 02, 06 | Pending |

## Tiêu chí nghiệm thu chung

- Không còn cặp service nào gọi HTTP đồng bộ hai chiều; test ở phase 07 kiểm điều này tự động.
- Frontend không phải sửa gì cho các route đã chuyển (D2); các endpoint chuyển cho cùng JSON như trước.
- Mỗi phase có test mới hoặc cập nhật, `mvn -B -ntp verify` xanh, `docker compose config --quiet` xanh.
- Số bản ghi, tổng thời gian học (`total_accepted_ms`) và số mục tiêu giữa `user_db` và `learning_db` khớp trước khi cắt Gateway; báo cáo đối soát được lưu trong `plans/reports/`.
- `docs/contracts`, `docs/system-architecture.md` và các README khớp code.

## Ngoài phạm vi

- Tạo service mới, tách content-service hoặc learning-service.
- Xóa bảng cũ, đổi cơ chế xác thực/role, đổi tên đường dẫn công khai.
- Đổi cách tính streak, thưởng hay mastery.

## Ghi chú cho người thực hiện

- Đọc `backend/AGENTS.md` trước. Đường dẫn trong các phase là tương đối theo `backend/services/<service>/src/main` trừ khi ghi khác.
- Chạy một service: `mvn -B -ntp -pl services/<service> -am test`. CI chạy `mvn -B -ntp verify` cả reactor. Test tích hợp dùng Testcontainers PostgreSQL nên cần Docker.
- Số migration: kiểm tra số mới nhất của từng thư mục `db/migration` ngay trước khi tạo file. Plan paywall đang giữ learning V38, V39 và content V86-V88, nên phase 05 dùng số kế tiếp còn trống.
- TDD: mỗi phase liệt kê test viết trước. Không xóa hay làm yếu test cũ; test cũ đổi hành vi có chủ đích thì ghi lý do.
- Tên migration, test và comment mô tả hành vi, không chứa số phase hay mã plan.
- Mọi lệnh bằng lệnh Windows hoặc bash: dùng script có sẵn trong `backend/scripts` làm mẫu cách viết.

## Câu hỏi chưa giải quyết

- Không còn câu hỏi chặn. Đã chốt D4 (streak lấy từ learning theo lô), D3 (learning gọi assessment), D5 (đợi paywall merge, commit plan sau), D9 (chỉ dev/staging).
- Còn mở nhưng không chặn: cách bảo vệ `/internal/**` ở learning-service (chưa có controller nội bộ; phase 04 yêu cầu theo mẫu của user-service); tần suất heartbeat thực tế chưa đo (chỉ ảnh hưởng thời lượng đồng bộ ở phase 06).

## Kết quả thực hiện (2026-10-31)

Code xong cả 7 phase trên hai nhánh, chưa chạy `docker compose up` toàn hệ thống. Test đã chạy: user 201, learning 1124,
community 138, gateway 34, đều xanh; `docker compose config` xanh. Đã gọi thử qua Gateway thật trên stack tạm: goals, activity,
study-session, thống kê study-time, streak, đường dẫn bị chặn và dữ liệu rơi đúng vào `learning_db`.

| Phase | Nhánh | Trạng thái |
| --- | --- | --- |
| 01, 02, 03, 04, 07 | `fix/service-boundary-cleanup` | Xong |
| 05, 06 | `feat/study-data-to-learning` (đã gộp nhánh trên và nhánh paywall) | Xong, gồm script chép dữ liệu `scripts/copy-study-data-to-learning.sh` |

Chưa làm: chạy script chép trên dữ liệu dev thật; thử một study session mở thành công qua Gateway (nguồn bài học thật) để
kiểm việc dời mục tiêu khi đổi múi giờ; token ADMIN cho endpoint admin goals.

### Lệch so với plan

- **Múi giờ học:** user-service lỗi thì learning dùng múi giờ đã lưu lần gần nhất, rồi `Asia/Ho_Chi_Minh`, thay vì trả 503. Việc dời
  mục tiêu khi đổi múi giờ xảy ra ở lần bắt đầu phiên học kế tiếp, không còn lúc lưu hồ sơ.
- **`StudySourceVerifier.verify`** nhận thêm `userId`; nguồn `LESSON`, `PRACTICE`, `REVIEW` kiểm trong `learning_db`, chỉ `ASSESSMENT`
  gọi assessment.
- **`RestLearnerGoalClient`** bị xóa từ phase 05; báo cáo học viên đọc mục tiêu cục bộ nên script chép phải chạy trước hoặc cùng lúc cắt Gateway.
- **Body mục tiêu sai** nay trả 400 (trước đây là 500 ngoài ý muốn).
- **Endpoint admin goals** giữ ở `/api/learning/admin/users/{id}/goals` với role `ADMIN`, cần một ngoại lệ trong
  `BusinessControllerRoleConventionTest`. Chủ dự án chốt giữ nguyên.
- **`targetBand` bị bỏ khỏi hồ sơ công khai.** Plan bỏ sót việc user-service đọc `learning_goals` trong `user_db` cho trường này;
  sau khi dữ liệu chuyển đi nó sẽ cũ. Chủ dự án chốt bỏ hẳn (không màn hình nào hiện target band của tác giả). Trường
  `targetBand` trong `PublicProfile` của community giữ nguyên và luôn null.
- Xóa bốn lớp tiện ích chỉ phục vụ mã đã gỡ: `DomainChecks`, `ApplicationSupport`, `JpaSupport`, `PersistenceExceptions`.
- Danh sách cạnh vòng đã biết trong `ServiceDependencyGraphTest` được bỏ vì không còn cạnh nào.
