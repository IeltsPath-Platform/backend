---
phase: 6
title: "Cắt chuyển, Gateway và dọn user-service"
status: pending
priority: P1
effort: "2-3 ngày"
dependencies: [5]
---

# Phase 06: Cắt chuyển, Gateway và dọn user-service

## Overview

Phase 05 đã dựng đủ phía learning-service nhưng người dùng vẫn đi qua user-service. Phase này chuyển lưu lượng sang learning (đổi route Gateway, giữ nguyên đường dẫn công khai), đối soát dữ liệu, rồi gỡ mã và cấu hình cũ khỏi user-service. Chia hai đợt có thể dừng giữa chừng: **đợt A** (cắt chuyển) và **đợt B** (dọn dẹp, chỉ sau một thời gian chạy ổn).

## Requirements

### Đợt A: cắt chuyển

1. **Chuẩn bị.** Triển khai learning-service có endpoint mới (phase 05), Gateway chưa đổi. Chạy script sao chép (phase 05) và lưu báo cáo đối soát vào `plans/reports/`. Mọi dòng đối soát phải khớp: số dòng từng bảng, `SUM(total_accepted_ms)`, số phiên theo `status`, số mục tiêu theo `status`.
2. **Gateway** (cấu hình tập trung, commit riêng): trong `infra/config-server/config-repo/api-gateway.yaml` thêm các route tới `lb://learning-service`, đặt **trước** route `user-service` (khớp `/auth/**,/api/users/**`) và trước route `user-activity-streak-service`, vì Gateway chọn route khớp đầu tiên. Dùng `RewritePath` để giữ đường dẫn công khai cũ (D2):

| Predicate Path | Rewrite sang |
|---|---|
| `/api/users/me/learning-goals/**` | `/api/learning/goals/...` |
| `/api/users/me/study-sessions/**` | `/api/learning/study-sessions/...` |
| `/api/users/me/statistics/study-time` | `/api/learning/statistics/study-time` |
| `/api/users/*/learning-goals/**` (admin, **không** khớp `me`; đặt route `me` trước) | `/api/learning/admin/users/{id}/goals/...` |
| `/api/learning-support/activities/**` | `/api/learning/activities/...` |

   Sửa route `user-activity-streak-service`: bỏ `activities`. Nếu phase 04 đã xong thì `streak` cũng đã bỏ và route này không còn đường dẫn nào, xóa hẳn route; nếu phase 04 chưa xong, giữ `streak`. Cập nhật `springdoc.paths-to-match` trong `user-service.yaml` (bỏ activities). Cập nhật và bổ sung test cấu hình của Gateway (`LearningSupportRouteConfigTest` và các test route cùng kiểu): khẳng định mỗi đường dẫn cũ đi tới learning, thứ tự route đúng (route cụ thể trước route `user-service`), `/api/users/me` và `/auth/**` vẫn đi tới user-service.
3. **Giao thức cắt chuyển (D9: chỉ dev/staging).** Không cần cửa sổ bảo trì. Thứ tự: (a) chạy script sao chép và đối soát, (b) bật route mới, (c) chạy lại script lần cuối để bắt các thay đổi trong khoảng giữa, (d) đối soát lần hai và lưu cả hai báo cáo. Phiên đang mở (`status='OPEN'`) bị cắt ngang được đóng khi lease hết hạn theo logic cũ; không để tổng thời gian đã nhận bị tính hai lần: kiểm tra bằng đối soát `SUM(total_accepted_ms)` trước và sau. Nếu sau này có dữ liệu production, quy trình này phải được xem lại (thêm cửa sổ bảo trì) trước khi chạy ở đó; ghi nhắc này vào báo cáo.
4. Giữ nguyên toàn bộ code user-service thêm ít nhất một vòng chạy ổn định để quay lại được (rollback = đưa route Gateway về user-service).

### Đợt B: dọn dẹp (commit/PR riêng, sau đợt A chạy ổn)

5. **User-service**: gỡ controller, request/response, use case, command/result, aggregate, entity, mapper, repository/adapter, test của: mục tiêu học, hoạt động học, thời gian học (xem danh sách tệp ở phase 05); gỡ `HttpStudySourceVerifier`, port `StudySourceVerifier`, `StudyTrackingConfig`; gỡ cấu hình `app.study-time.learning-url` và `assessment-url` (`user-service.yaml` dòng 31-33), và `LEARNING_SERVICE_URL`, `ASSESSMENT_SERVICE_URL` của user-service trong `docker-compose.yml` (dòng ~140-141); gỡ endpoint `GET /internal/users/{userId}/learning-goal` và `GetInternalLearningGoalUseCase`. Admin: gỡ hai endpoint goal khỏi `UserManagementController`.
6. `UpdateLearnerProfileUseCase`: bỏ `ScheduleStudyGoalUseCase` và `LearningGoalRepository`, bỏ kích hoạt xếp lại lịch khi đổi múi giờ (learning tự phát hiện, phase 05). Dùng `UserLock` cho khóa theo user như `ChangePasswordUseCase`. Giữ `AdvisoryUserLock` cho nhóm tài khoản; bỏ `lockUser`/`lockGoalOwner` khỏi `StudySessionRepository`.
7. **Không** thêm migration xóa bảng. Bảng cũ trong `user_db` (8 bảng, và `streaks`) vẫn còn; đánh dấu deprecated trong tài liệu.
8. **Learning-service**: xóa `RestLearnerGoalClient` (đã thay bằng cài đặt cục bộ); xóa endpoint `GET /api/learning/statistics/study-context` nếu không còn ai gọi (chỉ `HttpStudySourceVerifier` gọi nó; kiểm bằng `grep`), giữ use case bên trong vì verifier cục bộ dùng.
9. **Tài liệu:** `docs/system-architecture.md` (bảng route và sơ đồ phụ thuộc: user-service không còn gọi service khác), `docs/contracts` (hợp đồng mục tiêu, thời gian học, hoạt động nay do learning phục vụ cùng đường dẫn công khai; endpoint nội bộ `GET /internal/users/{userId}/study-timezone`), `user-service/README.md` (dòng ~168, 173, 282), `learning-service/README.md`, `AGENTS.md` và `CLAUDE.md` (bảng module: user-service chỉ còn tài khoản/hồ sơ; learning-service gồm thêm mục tiêu và thời gian học).

## Tests first (red)

- Test route Gateway (mục 2) viết trước, đỏ trước khi sửa cấu hình.
- Test hồi quy sau dọn dẹp: user-service không còn bean nào gọi HTTP tới learning hay assessment (test quét cấu hình/classpath đơn giản); `UpdateLearnerProfileUseCaseTest` cập nhật không còn xếp lịch mục tiêu.
- Test tích hợp đầu-cuối trong learning-service (đã có từ phase 05) chạy lại sau dọn dẹp.
- Kịch bản kiểm thủ công hoặc tự động (ghi rõ cái nào là thủ công trong báo cáo): tạo mục tiêu qua đường dẫn cũ → thấy ở `GET /api/learning/report` (khối `target`); bắt đầu phiên học `LESSON` và `ASSESSMENT`, gửi heartbeat, đóng, xem `statistics/study-time`; đổi múi giờ hồ sơ rồi bắt đầu phiên → lịch mục tiêu được xếp lại; tạo/xóa hoạt động học; admin xem và đổi trạng thái mục tiêu của một user.

## Related Code Files

- Gateway/config (commit riêng): `infra/config-server/config-repo/api-gateway.yaml`, `user-service.yaml`, `docker-compose.yml`; test trong `infra/api-gateway/src/test`.
- User-service: các tệp ở mục 5-6; learning-service: mục 8.
- Tài liệu: mục 9.

## Success Criteria

- [ ] Báo cáo đối soát khớp trước khi cắt Gateway và sau khi cắt (số dòng, tổng thời gian, theo `status`).
- [ ] Mọi đường dẫn công khai cũ vẫn hoạt động, phục vụ bởi learning-service; frontend không đổi mã.
- [ ] Sau đợt B: user-service không còn client HTTP tới service nào khác; `mvn -B -ntp verify` và `docker compose config --quiet` xanh.
- [ ] Không có migration xóa bảng; bảng cũ còn nguyên.
- [ ] Tài liệu khớp code.

## Risk Assessment

- Mất hoặc đếm đôi thời gian học khi cắt chuyển: giảm bằng đối soát hai lần và cửa sổ bảo trì. Nếu đối soát lệch, dừng và báo cáo, không bật route mới.
- Thứ tự route Gateway sai làm yêu cầu rơi vào user-service sau khi mã đã gỡ: test route ở mục 2 chặn điều này; kiểm thêm bằng một yêu cầu thử cho từng đường dẫn sau khi bật.
- Rollback đợt A: đưa route về user-service (mã chưa gỡ). Rollback đợt B: revert commit; bảng cũ còn nguyên nên không mất dữ liệu, nhưng dữ liệu ghi vào learning sau cắt chuyển không tự quay về `user_db`, vì vậy chỉ làm đợt B sau khi chắc chắn không quay lại.
