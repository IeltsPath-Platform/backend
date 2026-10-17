---
phase: 6
title: "Chặn khóa PRO khi bắt đầu attempt"
status: blocked
priority: P2
dependencies: [3]
blockedBy: ["Q2 trong plan.md"]
---

# Phase 6: Chặn khóa PRO khi bắt đầu attempt

**Không làm phase này khi Q2 chưa được chủ dự án duyệt.** Nó thêm một endpoint mới ở access-service và một dependency mới của assessment, nên cần duyệt trước.

## Overview
Hiện `requiredFeatureKey` chỉ là nhãn hiển thị: Content lưu nó, Learning đổi thành `accessLevel = PREMIUM`, nhưng **không service nào kiểm quyền khi cho làm bài**,
và endpoint entitlement của Access (`GET /api/access/users/{userId}/entitlement`) là ADMIN-only, ghi chú "chưa service nào gọi".
Nếu không có phase này, đề PRO chỉ bị khóa ở giao diện: gọi thẳng `POST /api/assessments/attempts` vẫn làm được.

## Requirements
- Functional:
  - Access: `GET /api/access/me/entitlement` cho chính người gọi (danh tính qua `CurrentUserProvider`), trả `UserEntitlementResponse` hiện có
    (`isPremium`, `enabledFeatures`, …) bằng cách tái dùng `GetUserEntitlementUseCase`. Không mở endpoint nhận `userId` từ client.
  - Content: payload nội bộ `package-versions/{id}` thêm `requiredFeatureKey` (additive, chưa có ở payload này).
  - Assessment: port `application/port/EntitlementReader` + adapter REST trong `infrastructure/client` theo mẫu `RestAccessClient` của Learning
    (chuyển tiếp bearer của học viên và `X-Correlation-Id`; base URL `app.access.base-url: ${ACCESS_SERVICE_URL:http://localhost:8084}` thêm vào `assessment-service.yaml`).
  - `StartAssessmentAttemptUseCase`: nếu package có `requiredFeatureKey` thì kiểm khóa đó nằm trong `enabledFeatures`; thiếu → `403` mã `FEATURE_REQUIRED`;
    Access không gọi được → `503` mã `ACCESS_UNAVAILABLE` (đóng cửa với đề PRO). Package miễn phí **không gọi Access**, nên không thêm điểm lỗi cho luồng miễn phí.
  - Chỉ áp cho attempt `OFFICIAL_PRACTICE` trong plan này; premium topic của lộ trình giữ nguyên (xem Risk).
- Non-functional: không log token; một lần gọi Access mỗi lần start attempt PRO; timeout ngắn như client Access của Learning.

## Related Code Files
- Create: assessment `application/port/EntitlementReader.java`, `application/exception/FeatureRequiredException.java`,
  `infrastructure/client/RestEntitlementClient.java`
- Modify: access `api/controller/LearnerAccessController.java`; content `api/dto/internal/PackageVersionContentResponse.java`,
  `application/result/PackageVersionContentResult.java`, `JdbcLearningContentReader.publishedPackageVersion`;
  assessment `application/port/ContentPackageProvider.java`, `infrastructure/client/ContentPackageClient.java`,
  `application/usecase/StartAssessmentAttemptUseCase.java`, `api/exception/GlobalExceptionHandler.java`,
  `infra/config-server/config-repo/assessment-service.yaml` (thêm `app.access.base-url`)
- Tests: access `LearnerAccessControllerTest`; assessment `StartAssessmentAttemptUseCaseTest`, `RestEntitlementClientTest` (`MockRestServiceServer`, không gọi dịch vụ thật)

## Implementation Steps
1. **Test đỏ**: học viên có `PREMIUM_CONTENT` bắt đầu được đề PRO; không có → 403 `FEATURE_REQUIRED`; Access trả lỗi hoặc timeout → 503; đề miễn phí không phát sinh lời gọi Access;
   endpoint `/me/entitlement` chỉ trả dữ liệu của người gọi.
2. Access endpoint; content payload; assessment port, client, use case, mapping lỗi.
3. `mvn -q -pl services/access-service,services/content-service,services/assessment-service -am test`.

## Success Criteria
- [ ] Đề PRO không bắt đầu được khi thiếu quyền, kể cả gọi API trực tiếp.
- [ ] Luồng miễn phí không đổi và không phụ thuộc Access.
- [ ] Không có endpoint nào nhận `userId` từ client.

## Risk Assessment
- Phase này sửa access-service (thêm một endpoint đọc) và config assessment: vượt phạm vi "một service" của `AGENTS.md`, nên cần duyệt (Q2).
- Premium topic của lộ trình học hiện cũng chỉ là nhãn; phase này không sửa, để tránh đổi hành vi Learning. Ghi lại để chủ dự án quyết riêng.
- Access sập thì đề PRO không làm được (đóng cửa có chủ ý); đề miễn phí vẫn chạy.
