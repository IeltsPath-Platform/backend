# Phase 3: Learning, endpoint, quy đổi band, cổng 403

## Context

- `ApplyAssessmentResultUseCase.applyPlacement` chỉ lưu khi `overallBand != null`.
- `LearnerPlacementRepository`, `LearnerPlacement` đã có; `ListCoursesUseCase` tính `recommended` từ placement.
- Mẫu endpoint: `CourseController` + `AssignCourseTestUseCase`; client Content: `LearningContentClient`.
- `LearningRequestException(status, code, message)` là cách trả lỗi có `code`.

## Requirements

- `GET /api/learning/placement-test`: CUSTOMER; đã có placement thì `409 PLACEMENT_ALREADY_DONE`; không có package thì `404 NO_PLACEMENT_TEST`; ngược lại `{packageId, packageVersionId}`.
- Consumer PLACEMENT: tính band từ `item_results` theo kỹ năng, lưu `LearnerPlacement` (một lần; nếu đã có thì bỏ qua, nhất quán D3).
- `GET /courses`, `GET /topics`: chưa có placement thì `403 PLACEMENT_REQUIRED`.

## Quy đổi band (đề xuất, chờ duyệt)

- Listening, Reading: % đúng → band: dưới 30% → 4.0; 30–44 → 4.5; 45–59 → 5.0; 60–69 → 5.5; 70–79 → 6.0; 80–89 → 6.5; 90 trở lên → 7.0.
- Writing: band từ LLM (trung bình các item Writing). Speaking: điểm cố định đổi sang band theo cùng bảng.
- Band tổng = trung bình 4 band kỹ năng, làm tròn về bội số 0.5. Bảng nằm trong một class domain (`PlacementBandCalculator`) có test từng mốc.

## Files

- Create: `api/controller/PlacementController.java`, `application/usecase/GetPlacementTestUseCase.java`, `domain/service` hoặc `domain/vo` `PlacementBandCalculator.java`, `application/usecase/RequirePlacement` (kiểm tra dùng chung cho `ListCoursesUseCase` và topics).
- Modify: `LearningContentClient` + adapter HTTP (gọi `placement-packages`), `ApplyAssessmentResultUseCase` (tính band khi `overall_band` null), `ListCoursesUseCase`, use case liệt kê topic, `GlobalExceptionHandler`/`LearningRequestException` mapping, `config-repo/learning-service.yaml` nếu cần.
- Gateway: không đổi (đã route `/api/learning/**`).
- Tests: `PlacementBandCalculatorTest`, `GetPlacementTestUseCaseTest`, `ApplyPlacementResultUseCaseTest` mở rộng, `CoursePathIntegrationTest` cập nhật (hiện giả định không có cổng; tạo placement trong setup), web test cho `403`/`409`.

## Steps

1. Calculator + test mốc.
2. Content client + use case + controller.
3. Consumer tính band; giữ idempotent theo `(attempt_id, result_version)`.
4. Cổng 403 ở courses/topics; sửa test hiện có phụ thuộc "mọi course mở".
5. `mvn -q -pl services/learning-service -am test`.

## Risks

- Test và e2e hiện có (và demo learner) bị khóa; cần seed placement cho học viên demo hoặc cập nhật test (câu hỏi mở 3).
- Race: học viên gọi `/courses` ngay sau khi nộp, band chưa kịp lưu → `403`; FE phải poll (ghi trong phase 4).
- Rollback: gỡ kiểm tra cổng là một thay đổi gọn ở một use case dùng chung.
