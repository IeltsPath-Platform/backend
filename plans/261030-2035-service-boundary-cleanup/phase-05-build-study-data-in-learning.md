---
phase: 5
title: "Chuyển mục tiêu, thời gian học và hoạt động sang learning (xây phía learning)"
status: pending
priority: P1
effort: "3-4 ngày"
dependencies: ["paywall đã merge vào main"]
---

# Phase 05: Xây dữ liệu học tập trong learning-service

Bắt đầu chỉ khi plan paywall (`261030-1204-band-skill-lesson-paywall`) đã merge vào `main`: hai plan cùng sửa learning-service và cùng cần số migration.

## Overview

Mục tiêu học, hoạt động học và ghi thời gian học (8 bảng, 88 file liên quan trong user-service) không tách được thành hai bước vì tạo mục tiêu, đổi trạng thái mục tiêu và đổi múi giờ hồ sơ đều ghi `study_goal_history` trong cùng luồng. Phase này **xây toàn bộ phía learning-service** và script sao chép dữ liệu, nhưng **chưa đổi Gateway**: người dùng vẫn đi qua user-service. Việc cắt chuyển nằm ở phase 06.

## Requirements

### Bảng (migration mới trong learning-service, số kế tiếp còn trống)

Tạo 8 bảng cùng tên và cột với user-service: `learning_goals`, `learning_activities`, `study_tracking_policy`, `study_sessions`, `study_heartbeat_receipts`, `study_activity_intervals`, `study_daily_totals`, `study_goal_history`. Sao DDL từ `user-service/.../db/migration` V2 (phần `learning_goals`), V4 (ràng buộc một mục tiêu active mỗi user), V5 (`learning_activities`), V6 (sáu bảng còn lại), **bỏ mọi khóa ngoại tới bảng của user-service** (`users`, `learner_profiles`) vì khác database; giữ nguyên chỉ mục, CHECK, UNIQUE. Dữ liệu seed trong policy (nếu V6 có INSERT) phải đi theo. Không tạo lại `streaks`.

### Mã (theo kiến trúc phân tầng của learning-service, `AGENTS.md` §3.1)

Chuyển và thích nghi các lớp sau từ `user-service` (xem danh sách đầy đủ bằng `git ls-tree -r --name-only main services/user-service | grep -i "study\|goal\|activity"`, bỏ các lớp `Streak*` vì thuộc phase 04):
- Mục tiêu: aggregate `LearningGoal`, `GoalStatus`, các exception `LearningGoal*`, repository và adapter, use case `CreateLearningGoal`, `ChangeLearningGoalStatus`, `GetActiveLearningGoal`, `GetLearningGoalsByUserId`, `ScheduleStudyGoal`.
- Hoạt động: `LearningActivity`, `CreateLearningActivity`, `DeleteLearningActivity` và truy vấn danh sách.
- Thời gian học: `StudySession`, `StudyTrackingException`, `StartStudySession`, `RecordStudyHeartbeat`, `GetStudyTimeStatistics`, `StudyTimezone`, `StudyTrackingConfig`, `StudySessionRepository` và adapter.
- Khóa theo user: dùng `LearnerLock` (cài `JdbcLearnerLock`) có sẵn của learning-service thay cho `AdvisoryUserLock`. Giữ thứ tự cũ: xác minh nguồn (gọi mạng) xong rồi mới lấy khóa.

### Endpoint mới trong learning-service (JSON request/response **giống hệt** user-service)

| Đường dẫn công khai hiện tại (giữ nguyên, D2) | Đường dẫn trong learning |
|---|---|
| `GET/POST /api/users/me/learning-goals`, `GET /api/users/me/learning-goals/active`, `PUT /api/users/me/learning-goals/{goalId}/status` | `/api/learning/goals`, `/api/learning/goals/active`, `/api/learning/goals/{goalId}/status` |
| `GET /api/users/{id}/learning-goals`, `PUT /api/users/{id}/learning-goals/{goalId}/status` (chỉ ADMIN, trong `UserManagementController`) | `/api/learning/admin/users/{id}/goals`, `/api/learning/admin/users/{id}/goals/{goalId}/status`, cùng quyền ADMIN. Xem cách learning-service đã phân quyền các endpoint staff hiện có để theo mẫu |
| `POST /api/users/me/study-sessions`, `POST .../study-sessions/{id}/heartbeats`, `POST .../study-sessions/{id}/close` | `/api/learning/study-sessions`, `/api/learning/study-sessions/{id}/heartbeats`, `/api/learning/study-sessions/{id}/close` |
| `GET /api/users/me/statistics/study-time` | `/api/learning/statistics/study-time` (cùng nhóm với `LearningStatisticsController`; kiểm tra không trùng đường dẫn có sẵn) |
| `POST/GET /api/learning-support/activities`, `DELETE /api/learning-support/activities/{id}` | `/api/learning/activities`, `/api/learning/activities/{id}` |

Học viên dùng `currentUser.requireUserId()` như các controller khác của learning. Mã lỗi và thông điệp lỗi giữ nguyên.

### Xác minh nguồn phiên học (D3)

Port `StudySourceVerifier` trong learning-service:
- `LESSON`, `PRACTICE`, `REVIEW`: gọi trực tiếp trong tiến trình use case đang phục vụ `GET /api/learning/statistics/study-context` (`LearningStatisticsController.context` → `context.execute(userId, sourceType, sourceId)`); không qua HTTP.
- `ASSESSMENT`: adapter HTTP gọi `GET {assessment base-url}/api/assessments/me/statistics/study-context?sourceType=&sourceId=`, chuyển tiếp `Authorization` và `X-Correlation-Id` của yêu cầu hiện tại; timeout kết nối 2s, đọc 3s; ánh xạ lỗi y như `HttpStudySourceVerifier` (404/403/409 → `SOURCE_NOT_AVAILABLE`; lỗi khác → `SOURCE_UNAVAILABLE`; `sourceId` trả về khác `sourceId` đã gửi → `SOURCE_UNAVAILABLE`). Dùng base-url assessment đã có trong cấu hình learning-service (`learning-service.yaml` quanh dòng 33), không thêm biến mới.
- Nguồn không hỗ trợ: `IllegalArgumentException` như cũ.

### Múi giờ và lịch mục tiêu (D7)

User-service hiện đọc múi giờ từ `learner_profiles` qua `StudyTimezone.forUser` và xếp lại lịch mục tiêu khi hồ sơ đổi múi giờ (`UpdateLearnerProfileUseCase`). Sau khi chuyển:
- User-service thêm endpoint nội bộ `GET /internal/users/{userId}/study-timezone` trả `{ "timezone": "..." }` (xem `InternalUserController` và cách `RestDisplayNameClient` xác thực lời gọi nội bộ để theo mẫu; thêm vào tài liệu `/internal/**`).
- Learning thêm port `StudyTimezoneClient` và adapter HTTP gọi endpoint đó (chiều learning → user đã tồn tại). Lời gọi mạng thực hiện **trước** khi lấy khóa/transaction.
- Khi user-service không phản hồi: không làm hỏng việc bắt đầu phiên học; dùng múi giờ đã lưu gần nhất của user (phiên hoặc lịch mục tiêu gần nhất), nếu không có thì dùng mặc định mà `StudyTimezone.forUser` đang dùng khi thiếu hồ sơ. Tạo/đổi mục tiêu khi không lấy được múi giờ mà cũng không có giá trị lưu: lỗi 503 rõ ràng.
- Xếp lại lịch khi múi giờ đổi (thay cho trigger trong `UpdateLearnerProfileUseCase`): khi bắt đầu phiên học, tạo mục tiêu, hoặc đổi trạng thái mục tiêu, nếu học viên có mục tiêu active mà múi giờ hiện tại khác `zone_id` của dòng lịch gần nhất trong `study_goal_history`, gọi `ScheduleStudyGoal.execute` một lần. Giữ nguyên quy tắc ngày hiệu lực ("không đặt mục tiêu cho ngày đã ghi nhận", "thay thế lịch chờ cũ" trong `ScheduleStudyGoalUseCase`).

### Đọc mục tiêu cho báo cáo

`LearnerGoalClient` (dùng bởi `GetLearnerReportUseCase`) được cài lại bằng repository cục bộ của learning; hành vi cũ giữ nguyên (không có mục tiêu thì khối `target` không xuất hiện; `targetBand`/`examDate` null khi không có mục tiêu active). Chưa xóa `RestLearnerGoalClient` ở phase này (xóa ở phase 06).

### Script sao chép dữ liệu

Tạo một script trong `backend/scripts` theo kiểu các script có sẵn (xem thư mục đó để chọn bash hay PowerShell), sao chép 8 bảng từ `user_db` sang `learning_db` trên cùng máy chủ PostgreSQL (mặc định `localhost:5440`):
- Tham số qua biến môi trường, **không** ghi mật khẩu vào repo. Có chế độ `--dry-run` chỉ đếm.
- Idempotent và chạy lại được: nạp vào bảng tạm rồi `INSERT ... ON CONFLICT` theo khóa chính; với bảng có thể thay đổi (`study_sessions`, `study_daily_totals`, `learning_goals`, `study_goal_history`) ghi đè khi bản nguồn mới hơn (`updated_at` hoặc `sequence`/`watermark_ms` lớn hơn tùy bảng, đọc DDL để chọn đúng cột), bảng chỉ-thêm (`study_heartbeat_receipts`, `study_activity_intervals`) bỏ qua bản trùng.
- Không bao giờ xóa hay sửa bên nguồn.
- In bảng đối soát cho từng bảng: số dòng hai bên, `SUM(total_accepted_ms)` của `study_sessions`, số phiên theo `status`, số mục tiêu theo `status`; thoát mã khác 0 nếu có lệch.
- Ghi tệp SQL đối soát riêng (chỉ SELECT) để người khác chạy lại.

## Tests first (red)

Trong `learning-service`:
- Mang theo các test tương đương của user-service (18 tệp: `LearningGoalUseCaseTest`, `ScheduleStudyGoalUseCaseTest`, `StudyTimeUseCaseTest`, `StudySessionTest`, `LearningGoalTest`, `CreateLearningActivityUseCaseTest`, các test bảo mật controller, các test migration/persistence) và để chúng đỏ trước khi mã được chuyển.
- Migration: ràng buộc một mục tiêu active mỗi user; sáu bảng thời gian học tồn tại với khóa chính, CHECK và UNIQUE như nguồn.
- Xác minh nguồn: `LESSON`/`PRACTICE`/`REVIEW` đi qua use case cục bộ; `ASSESSMENT` qua client giả (thành công, 404/403/409 → `SOURCE_NOT_AVAILABLE`, timeout/5xx → `SOURCE_UNAVAILABLE`, `sourceId` lệch); thứ tự: xác minh xong trước khi lấy khóa.
- Múi giờ: múi giờ đổi → lịch mục tiêu được xếp lại đúng một lần; user-service ngừng → dùng giá trị lưu gần nhất và phiên vẫn bắt đầu được; không có giá trị lưu khi tạo mục tiêu → 503.
- Hợp đồng: mỗi endpoint mới trả JSON có cùng tên trường và mã lỗi như bản cũ (so với các DTO đã chuyển); admin cần ADMIN, học viên chỉ thấy dữ liệu của mình.
- Báo cáo: `GET /api/learning/report` vẫn có `target` đúng khi dùng mục tiêu cục bộ (cập nhật test hiện có của `GetLearnerReportUseCase`).
- User-service: test endpoint `GET /internal/users/{id}/study-timezone` (có hồ sơ → múi giờ; không có hồ sơ → hành vi mặc định đã định nghĩa; yêu cầu xác thực nội bộ).

## Related Code Files

- Tạo trong learning-service: migration mới, các lớp đã liệt kê ở Requirements, adapter `StudyTimezoneClient`, adapter xác minh assessment, controller mới.
- Sửa trong user-service: thêm `GET /internal/users/{userId}/study-timezone` trong `InternalUserController` và use case tương ứng. Chưa xóa gì ở user-service.
- Tạo: script sao chép và tệp SQL đối soát trong `backend/scripts`.
- Tài liệu: ghi các endpoint mới và endpoint nội bộ vào `docs/contracts` khi phase 06 cắt chuyển (không ghi sớm để tài liệu khớp hệ thống đang chạy).

## Implementation Steps

1. Viết test đỏ (migration, use case, verifier, múi giờ, controller).
2. Tạo migration; chuyển và thích nghi từng nhóm lớp: mục tiêu → hoạt động → thời gian học.
3. Cài `StudySourceVerifier` (cục bộ + assessment) và `StudyTimezoneClient`; thêm endpoint nội bộ ở user-service.
4. Thêm controller mới; cài lại `LearnerGoalClient` bằng repository cục bộ.
5. Viết script sao chép và SQL đối soát; chạy thử trên hai database cục bộ với dữ liệu mẫu (có thể dùng dữ liệu seed hoặc tạo bản ghi thử), lưu kết quả vào `plans/reports/`.
6. `mvn -B -ntp -pl services/learning-service,services/user-service -am test`.

## Success Criteria

- [ ] Learning-service phục vụ đủ các endpoint ở bảng trên với JSON giống bản cũ; chưa có lưu lượng thật đi qua (Gateway chưa đổi).
- [ ] Test learning-service và user-service xanh; test được chuyển không bị làm yếu.
- [ ] Script sao chép chạy lại nhiều lần cho cùng kết quả đối soát và không đổi dữ liệu nguồn.
- [ ] Báo cáo đối soát chạy thử được lưu trong `plans/reports/`.

## Risk Assessment

- Lệch ngữ nghĩa phiên học (`sequence`, `watermark_ms`, `epoch`, lease): chuyển mã nguyên văn trước, chỉ đổi phần phụ thuộc (khóa, verifier, múi giờ). Nếu một test cũ đỏ vì khác biệt ngữ nghĩa mà không giải thích được, dừng và báo cáo thay vì sửa test.
- Hai nơi cùng chứa bản sao dữ liệu cho tới phase 06: chưa có lưu lượng ghi vào learning ở phase này nên không lệch; đừng bật Gateway sớm.
- Learning-service đã là service nhiều bảng nhất (khoảng 47); thêm 8 bảng là chủ ý của chủ dự án.
