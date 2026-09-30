---
phase: 2
title: "Chặn lộ đáp án"
status: pending
priority: P1
dependencies: [1]
effort: "1 ngày"
---

# Phase 2: Chặn lộ đáp án

## Overview

Đóng các đường mà học viên xem được đáp án, dò được mã đề hay gói ôn, tự tạo kết quả, hoặc gọi được endpoint nội bộ. Việc làm ở content, assessment và Gateway. Lỗ của access-service và secret fallback thuộc plan khác (xem `plan.md`).

## Requirements

- **Content**, chỉ cho `ADMIN`, `CONTENT_AUTHOR` (`@PreAuthorize` theo method):
  - `QuestionController`: mọi method (danh sách `:33`, chi tiết `:42` trả `answerSpecJson`/`explanation`, ghi).
  - `ContentPackageController`: mọi method (danh sách và chi tiết lộ `packageType`, `currentPublishedVersionId`, id câu).
  - Endpoint ghi của `ContentAssetController`, `KnowledgePointController`, `LearningVideoController` (`:58,76,93,109`).
- **Content giữ mở cho mọi role:** `GET /api/content/topics`, `GET /api/content/knowledge-points` (ai-learning gọi **không kèm** `topicId`, `app/clients/content_service.py:45-47`), `GET /api/content/reading/**` (tutor, `content_service.py:23`). Đã grep: không service nào khác gọi `/api/content/packages` hay `/api/content/questions`.
- **Assessment:**
  - structure bỏ `answerSnapshot`;
  - **xóa** `POST /api/assessments/attempts/{id}/result` của học viên (`AssessmentResultController.java:29-38`) cùng đường `execute` cho học viên trong `CreateAssessmentResultUseCase` (`:22-27`). Người chấm đã có route ở `GradingController`.
- **Gateway:** chặn tường minh `/internal/**` (403) trước khi xét route, vì endpoint nội bộ mới trả `answerSpec`.

## Architecture

- Không đổi response schema nào ngoài `answerSnapshot` và endpoint bị xóa (đã có trong contract phase 1).
- Gateway: thêm matcher deny trong `infra/api-gateway/.../SecurityConfig.java` (chuỗi `SecurityWebFilterChain` riêng của Gateway), không đụng route.

## Related Code Files

- Modify (content): `api/controller/QuestionController.java`, `ContentPackageController.java`, `ContentAssetController.java`, `KnowledgePointController.java`, `LearningVideoController.java`
- Modify (assessment): `application/usecase/GetAttemptStructureUseCase.java`, `application/result/AttemptStructureResult.java`, `api/dto/response/AttemptStructureResponse.java`, `api/controller/AssessmentResultController.java`, `application/usecase/CreateAssessmentResultUseCase.java`
- Modify (gateway): `infra/api-gateway/src/main/java/.../security/SecurityConfig.java`
- Create (tests): `content-service/src/test/.../api/controller/ContentAuthorizationWebMvcTest.java` (security thật, không `standaloneSetup`); test structure và result của assessment; test Gateway cho `/internal/**`

## Implementation Steps

**Tests Before** (hành vi phải giữ):
1. CUSTOMER `GET /api/content/topics` → 200.
2. CUSTOMER `GET /api/content/knowledge-points` (không có `topicId`) → 200.
3. CUSTOMER `GET /api/content/reading/sections/{id}` → 200.
4. Chủ attempt `GET /attempts/{id}/structure` → 200, các trường khác giữ nguyên.
5. EXAMINER đi luồng chấm tay qua `GradingController` → giữ nguyên.

**Tests After** (viết trước khi sửa, lúc đầu fail):
6. CUSTOMER: `GET /api/content/questions`, `GET /api/content/questions/{id}`, `GET /api/content/packages`, `GET /api/content/packages/{id}` → 403. CONTENT_AUTHOR → 200.
7. CUSTOMER ghi trên question, package, asset, knowledge point, video → 403; ADMIN → 2xx.
8. JSON structure không có khóa `answerSnapshot`.
9. CUSTOMER `POST /attempts/{id}/result` → 404 hoặc 405 (route không còn).
10. Gateway: request tới `/internal/learning-content/...` có token hợp lệ → 403.

**Refactor:** thêm `@PreAuthorize` theo mẫu `TopicController.java:49,62`; bỏ `answerSnapshot`; xóa route học viên mở result; thêm deny ở Gateway.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
mvn -q -pl services/assessment-service -am test
mvn -q -pl infra/api-gateway test
```

## Success Criteria

- [ ] Test bước 1–10 pass; không test cũ nào bị xóa hay nới lỏng để pass.
- [ ] Học viên không đọc được `answerSpec`, `explanation`, danh sách gói hay danh sách câu qua API công khai.

## Risk Assessment

- **Công cụ soạn nội dung dùng tài khoản CUSTOMER:** chưa có frontend; tài khoản soạn phải có role `CONTENT_AUTHOR`.
- **Sửa Gateway:** chỉ thêm deny, không đổi route hay filter JWT; test Gateway hiện có phải pass.
