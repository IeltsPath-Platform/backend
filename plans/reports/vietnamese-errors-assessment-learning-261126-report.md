# Báo cáo chuẩn hóa thông báo lỗi tiếng Việt (assessment-service & learning-service)

- **Nhánh:** `fix/vietnamese-errors-assessment-learning`
- **Ngày:** 2026-11-26

## 1. Nối lớp chốt chặn `VietnameseErrorMessages.userFacing`

- `services/assessment-service/src/main/java/com/ieltspath/assessment/api/exception/GlobalExceptionHandler.java`: nối `VietnameseErrorMessages.userFacing` vào `build(...)` và `responseStatus(...)` (giữ nguyên `code` từ `ResponseStatusException.getReason()`), ghi log cảnh báo khi có thông báo không phải tiếng Việt bị thay thế.
- `services/learning-service/src/main/java/com/ieltspath/learning/api/exception/GlobalExceptionHandler.java`: nối `VietnameseErrorMessages.userFacing` qua helper `userFacing(status, message)` vào cả `build(...)` (`ErrorResponse`) lẫn tất cả các handler trả `LearningErrorResponse`.
- `services/learning-service/src/main/java/com/ieltspath/learning/api/exception/StudyDataExceptionHandler.java`: nối `VietnameseErrorMessages.userFacing` vào `error(...)` và ánh xạ các mã `StudyTrackingException` sang thông báo tiếng Việt cụ thể (giữ nguyên `details.code`).

## 2. Kiểm tra chống tái phạm

- `services/assessment-service/src/test/java/com/ieltspath/assessment/api/exception/VietnameseErrorMessagesRegressionTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/api/exception/VietnameseErrorMessagesRegressionTest.java`

## 3. Kết quả kiểm thử

- `mvn -q -pl services/assessment-service -am test`: **PASS** (exit code 0, bao gồm Testcontainers).
- `mvn -q -pl services/learning-service -am clean test`: 2007 tests run, **0 lỗi liên quan thông báo tiếng Việt**; có 9 test fail/error có sẵn trên nhánh ở 5 class integration test (`FreePracticeIntegrationTest`, `LessonWritingIntegrationTest`, `PracticeEssayIntegrationTest`, `PracticeGatedLessonUnlockIntegrationTest`, `SuggestionsAndOverviewIntegrationTest`) liên quan mock `restAccessClient.premium()`.
- Thủ công `GET /api/assessments/attempts/<uuid>`: chưa kiểm tra do stack local không chạy.

## 4. Danh sách câu đã dịch theo file

### `services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/ExaminerAvailabilityController.java`
- Trước: `throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must hold from 1 to " + MAX_IDS + " examiners");`
- Sau: `"Danh sách giám khảo phải có từ 1 đến " + MAX_IDS + " người.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/ExaminerHistoryController.java`
- Trước: `throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or more and size from 1 to " + MAX_SIZE);`
- Sau: `"Số trang phải từ 0 trở lên và kích thước trang từ 1 đến " + MAX_SIZE + ".");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Graded review not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy bài đã chấm."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/InternalHumanReviewController.java`
- Trước: `throw new AssessmentAccessDeniedException("A review can only be requested for the authenticated learner");`
- Sau: `throw new AssessmentAccessDeniedException("Chỉ có thể yêu cầu chấm bài cho chính tài khoản đang đăng nhập.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/api/exception/GlobalExceptionHandler.java`
- Trước: `return build(HttpStatus.SERVICE_UNAVAILABLE, "Recording storage is unavailable", r.getRequestURI(), null,`
- Trước: `"STORAGE_UNAVAILABLE");`
- Sau: `"Dịch vụ lưu trữ bản ghi âm tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Sau: `r.getRequestURI(), null, "STORAGE_UNAVAILABLE");`
- Trước: `return build(HttpStatus.PAYLOAD_TOO_LARGE, "Recording file is too large", r.getRequestURI(), null,`
- Trước: `"UPLOAD_TOO_LARGE");`
- Sau: `return build(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp ghi âm vượt quá dung lượng cho phép.",`
- Sau: `r.getRequestURI(), null, "UPLOAD_TOO_LARGE");`
- Trước: `return build(HttpStatus.SERVICE_UNAVAILABLE, "User Service is unavailable", r.getRequestURI(), null,`
- Trước: `"USER_SERVICE_UNAVAILABLE");`
- Sau: `"Dịch vụ người dùng tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Sau: `r.getRequestURI(), null, "USER_SERVICE_UNAVAILABLE");`
- Trước: `return build(HttpStatus.SERVICE_UNAVAILABLE, "Content Service is unavailable", r.getRequestURI(), null,`
- Trước: `"CONTENT_UNAVAILABLE");`
- Sau: `"Dịch vụ nội dung tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Sau: `r.getRequestURI(), null, "CONTENT_UNAVAILABLE");`
- Trước: `return build(HttpStatus.CONFLICT, "The assessment was changed by another request", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.CONFLICT, "Bài thi đã được cập nhật bởi một yêu cầu khác. Vui lòng tải lại và thử lại.",`
- Trước: `return build(HttpStatus.CONFLICT, "The assessment request conflicts with existing data", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.CONFLICT, "Yêu cầu xung đột với dữ liệu hiện có. Vui lòng kiểm tra lại.",`
- Trước: `return build(HttpStatus.FORBIDDEN, "Access denied", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này.", r.getRequestURI(), null);`
- Trước: `return build(HttpStatus.BAD_REQUEST, "Validation failed", r.getRequestURI(), details);`
- Sau: `return build(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.", r.getRequestURI(), details);`
- Trước: `return build(HttpStatus.SERVICE_UNAVAILABLE, "A required service is unavailable", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ liên quan tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Trước: `return build(HttpStatus.FORBIDDEN, "Access denied", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.", r.getRequestURI(), null);`
- Trước: `return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức yêu cầu không được hỗ trợ.", r.getRequestURI(), null);`
- Sau: `log.warn("Replaced non-Vietnamese ResponseStatusException message (status={}): {}", status.value(), raw);`
- Trước: `return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", r.getRequestURI(), null);`
- Sau: `return build(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi nội bộ, vui lòng thử lại sau.",`
- Sau: `log.warn("Replaced non-Vietnamese error message (status={}, code={}): {}", s.value(), code, m);`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/exception/UnknownErrorCodeException.java`
- Trước: `super("Unknown error code(s): " + String.join(", ", codes));`
- Sau: `super("Mã lỗi không tồn tại hoặc chưa được kích hoạt: " + String.join(", ", codes));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/AiEvaluationReadService.java`
- Trước: `throw new IllegalArgumentException("task must be TASK_1 or TASK_2");`
- Sau: `throw new IllegalArgumentException("Loại bài viết phải là TASK_1 hoặc TASK_2.");`
- Trước: `throw new IllegalArgumentException("Invalid pagination (size 1.." + MAX_PAGE_SIZE + ")");`
- Sau: `throw new IllegalArgumentException("Thông số phân trang không hợp lệ (kích thước từ 1 đến " + MAX_PAGE_SIZE + ").");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/AiEvaluationService.java`
- Trước: `throw new IllegalArgumentException("Examiner error excerpt must be an exact part of the essay");`
- Sau: `throw new IllegalArgumentException("Đoạn trích lỗi của giám khảo phải khớp chính xác một phần trong bài viết.");`
- Trước: `throw new IllegalArgumentException("Examiner error needs an error code");`
- Sau: `throw new IllegalArgumentException("Lỗi do giám khảo đánh dấu phải có mã lỗi.");`
- Trước: `throw new IllegalArgumentException("Examiner error kind must be LANGUAGE or DATA");`
- Sau: `throw new IllegalArgumentException("Loại lỗi của giám khảo phải là LANGUAGE hoặc DATA.");`
- Trước: `throw new IllegalArgumentException("DATA errors apply to Task 1 only");`
- Sau: `throw new IllegalArgumentException("Lỗi số liệu (DATA) chỉ áp dụng cho bài viết Task 1.");`
- Trước: `try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("Invalid criteria"); }`
- Sau: `try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("Dữ liệu tiêu chí chấm không hợp lệ."); }`
- Trước: `throw new IllegalArgumentException("Band must be between 0 and 9 in half-band steps");`
- Sau: `throw new IllegalArgumentException("Điểm band phải nằm trong khoảng từ 0 đến 9 theo bước 0,5.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/ExaminerAssignmentService.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Examiner not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy giám khảo. Vui lòng chọn giám khảo khác."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Grading job not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));`
- Trước: `throw new RevisionConflictException("Grading of this submission is already finished");`
- Sau: `throw new RevisionConflictException("Bài nộp này đã được chấm xong.");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Grading job not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));`
- Trước: `throw new RevisionConflictException("Grading of this submission is already finished");`
- Sau: `throw new RevisionConflictException("Bài nộp này đã được chấm xong.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/ExaminerQaService.java`
- Trước: `throw new IllegalArgumentException("assessment.qa.sample-rate must be between 0 and 1");`
- Sau: `throw new IllegalArgumentException("Tỷ lệ lấy mẫu kiểm tra chất lượng phải nằm trong khoảng từ 0 đến 1.");`
- Trước: `throw new InvalidAssessmentStateException("Reviewer band must be from 0 to 9 in half bands");`
- Sau: `throw new InvalidAssessmentStateException("Điểm band đánh giá lại phải từ 0 đến 9 theo bước 0,5.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/HumanReviewService.java`
- Trước: `() -> new AssessmentNotFoundException("Human review not found"));`
- Sau: `() -> new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài bởi giám khảo."));`
- Trước: `throw new InvalidAssessmentStateException("draft must not be larger than 200 KB");`
- Sau: `throw new InvalidAssessmentStateException("Bản nháp nhận xét không được vượt quá 200 KB.");`
- Trước: `() -> new AssessmentNotFoundException("Standalone human review not found"));`
- Sau: `() -> new AssessmentNotFoundException("Không tìm thấy bài viết cần chấm."));`
- Trước: `"annotations[" + index + "] must quote the essay between start and end");`
- Sau: `"Đoạn trích lỗi đánh dấu không khớp chính xác với vị trí trong bài viết.");`
- Trước: `throw new InvalidAssessmentStateException(field + " must be an IELTS half-band from 0 to 9");`
- Sau: `throw new InvalidAssessmentStateException("Điểm tiêu chí phải là điểm band IELTS từ 0 đến 9 theo bước 0,5.");`
- Trước: `if (node == null || !node.isObject()) throw new InvalidAssessmentStateException(field + " must be a JSON object");`
- Sau: `if (node == null || !node.isObject()) throw new InvalidAssessmentStateException("Dữ liệu nhận xét phải ở định dạng đối tượng JSON hợp lệ.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/AssessmentResultCompleter.java`
- Trước: `throw new InvalidAssessmentStateException("An attempt without items cannot be finalized");`
- Sau: `throw new InvalidAssessmentStateException("Không thể hoàn tất kết quả khi bài thi chưa có câu hỏi nào.");`
- Trước: `throw new InvalidAssessmentStateException("Every assessment item must be graded before finalization");`
- Sau: `throw new InvalidAssessmentStateException("Tất cả câu hỏi trong bài thi phải được chấm trước khi hoàn tất kết quả.");`
- Trước: `throw new InvalidAssessmentStateException("Every graded item must record its score and maximum score");`
- Sau: `throw new InvalidAssessmentStateException("Mỗi câu đã chấm phải ghi nhận đầy đủ điểm đạt được và điểm tối đa.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/AttemptCreator.java`
- Trước: `throw new PackageNotAttemptableException("Package version has a section without questions");`
- Sau: `throw new PackageNotAttemptableException("Gói đề thi có phần chưa có câu hỏi nào.");`
- Trước: `throw new PackageNotAttemptableException("Package identity is missing");`
- Sau: `throw new PackageNotAttemptableException("Thiếu thông tin định danh của gói đề thi.");`
- Trước: `throw new PackageNotAttemptableException("Every question group needs an id and a format code");`
- Sau: `throw new PackageNotAttemptableException("Mỗi nhóm câu hỏi phải có mã định danh và mã định dạng.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CancelPlacementAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ClaimGradingJobUseCase.java`
- Trước: `var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Grading job not found"));`
- Sau: `var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CompleteAttemptSectionUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Attempt section not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy phần thi trong lượt làm bài này."));`
- Trước: `throw new InvalidAssessmentStateException("Only in-progress attempts can complete a section");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể hoàn thành phần thi khi lượt làm bài đang diễn ra.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CreateAssessmentResultUseCase.java`
- Trước: `var attempt=attempts.findById(attemptId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `var attempt=attempts.findById(attemptId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `if(attempt.getStatus()!=AttemptStatus.SUBMITTED) throw new InvalidAssessmentStateException("Only submitted attempts can have a result");`
- Sau: `if(attempt.getStatus()!=AttemptStatus.SUBMITTED) throw new InvalidAssessmentStateException("Chỉ có thể tạo kết quả cho lượt làm bài đã nộp.");`
- Trước: `if(latest.isPresent()&&latest.get().isGradable()) throw new InvalidAssessmentStateException("The latest result version is still being graded");`
- Sau: `if(latest.isPresent()&&latest.get().isGradable()) throw new InvalidAssessmentStateException("Phiên bản kết quả gần nhất vẫn đang được chấm.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CreateGradingJobUseCase.java`
- Trước: `var submission=submissions.findById(c.submissionId()).orElseThrow(()->new AssessmentNotFoundException("Learner submission not found"));`
- Trước: `if(!submission.userId().equals(c.userId())) throw new AssessmentAccessDeniedException("Submission does not belong to current user");`
- Trước: `if(c.skill()!=Skill.WRITING&&c.skill()!=Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions can be graded");`
- Trước: `if(c.gradingMode()==GradingMode.AI&&c.pointCostSnapshot()==null) throw new InvalidAssessmentStateException("AI grading requires a point cost snapshot");`
- Trước: `if(c.gradingMode()==GradingMode.HUMAN&&c.examinerId()==null) throw new InvalidAssessmentStateException("Human grading requires the examiner the learner chose");`
- Trước: `if(c.gradingMode()==GradingMode.AI&&c.examinerId()!=null) throw new InvalidAssessmentStateException("AI grading has no examiner");`
- Sau: `var submission=submissions.findById(c.submissionId()).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy bài nộp của học viên."));`
- Sau: `if(!submission.userId().equals(c.userId())) throw new AssessmentAccessDeniedException("Bài nộp này không thuộc về tài khoản hiện tại.");`
- Sau: `if(c.skill()!=Skill.WRITING&&c.skill()!=Skill.SPEAKING) throw new InvalidAssessmentStateException("Chỉ hỗ trợ chấm bài kỹ năng Writing hoặc Speaking.");`
- Sau: `if(c.gradingMode()==GradingMode.AI&&c.pointCostSnapshot()==null) throw new InvalidAssessmentStateException("Chấm bằng AI yêu cầu thông tin mức điểm quy đổi.");`
- Sau: `if(c.gradingMode()==GradingMode.HUMAN&&c.examinerId()==null) throw new InvalidAssessmentStateException("Vui lòng chọn giám khảo trước khi gửi chấm.");`
- Sau: `if(c.gradingMode()==GradingMode.AI&&c.examinerId()!=null) throw new InvalidAssessmentStateException("Chế độ chấm AI không hỗ trợ chọn giám khảo.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CreateLearnerSubmissionUseCase.java`
- Trước: `if (c.skill() != com.ieltspath.assessment.domain.vo.Skill.WRITING && c.skill() != com.ieltspath.assessment.domain.vo.Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions are supported");`
- Sau: `if (c.skill() != com.ieltspath.assessment.domain.vo.Skill.WRITING && c.skill() != com.ieltspath.assessment.domain.vo.Skill.SPEAKING) throw new InvalidAssessmentStateException("Chỉ hỗ trợ nộp bài kỹ năng Writing hoặc Speaking.");`
- Trước: `if ((c.textPayload() == null) == (c.audioReference() == null)) throw new InvalidAssessmentStateException("Exactly one text or audio payload is required");`
- Sau: `if ((c.textPayload() == null) == (c.audioReference() == null)) throw new InvalidAssessmentStateException("Cần cung cấp đúng một nội dung bài viết hoặc tệp ghi âm.");`
- Trước: `var item=items.findById(c.attemptItemId()).orElseThrow(()->new AssessmentNotFoundException("Attempt item not found"));`
- Trước: `var section=sections.findById(item.attemptSectionId()).orElseThrow(()->new AssessmentNotFoundException("Attempt section not found"));`
- Trước: `attempts.findByIdAndUserId(section.attemptId(),c.userId()).orElseThrow(()->new AssessmentAccessDeniedException("Attempt item does not belong to current user"));`
- Sau: `var item=items.findById(c.attemptItemId()).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy câu hỏi trong lượt làm bài."));`
- Sau: `var section=sections.findById(item.attemptSectionId()).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy phần thi trong lượt làm bài."));`
- Sau: `attempts.findByIdAndUserId(section.attemptId(),c.userId()).orElseThrow(()->new AssessmentAccessDeniedException("Câu hỏi này không thuộc lượt làm bài của bạn."));`
- Trước: `return new InvalidAssessmentStateException("Speaking recording is invalid or unavailable");`
- Sau: `return new InvalidAssessmentStateException("Bản ghi âm Speaking không hợp lệ hoặc không khả dụng.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ExaminerGradingPolicy.java`
- Trước: `throw new AssessmentAccessDeniedException("Only the assigned examiner may grade this attempt");`
- Sau: `throw new AssessmentAccessDeniedException("Chỉ giám khảo được phân công mới có quyền chấm lượt làm bài này.");`
- Trước: `throw new AssessmentAccessDeniedException("Only the assigned examiner may finalize this attempt");`
- Sau: `throw new AssessmentAccessDeniedException("Chỉ giám khảo được phân công mới có quyền hoàn tất lượt làm bài này.");`
- Trước: `throw new InvalidAssessmentStateException("Each submitted course essay requires feedbackSnapshot.band (0..9 in half bands)");`
- Sau: `throw new InvalidAssessmentStateException("Mỗi bài viết khóa học đã nộp cần có điểm band từ 0 đến 9 theo bước 0,5.");`
- Trước: `throw new InvalidAssessmentStateException("Essay feedback must be valid JSON with a band");`
- Sau: `throw new InvalidAssessmentStateException("Nhận xét bài viết phải ở định dạng JSON hợp lệ và có điểm band.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ExpireAssessmentAttemptUseCase.java`
- Trước: `@Transactional public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); a.expire(Instant.now()); var saved=repository.save(a); return AssessmentAttemptResult.from(saved);}`
- Sau: `@Transactional public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại.")); a.expire(Instant.now()); var saved=repository.save(a); return AssessmentAttemptResult.from(saved);}`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/FinalizeAssessmentResultUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));`
- Trước: `throw new InvalidAssessmentStateException("Only the latest result version can be finalized");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể hoàn tất phiên bản kết quả mới nhất.");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `throw new InvalidAssessmentStateException("Only a submitted attempt can have a final result");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể chốt kết quả cho lượt làm bài đã nộp.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetAssessmentAttemptUseCase.java`
- Trước: `@Transactional(readOnly=true) public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); return AssessmentAttemptResult.from(a);}`
- Sau: `@Transactional(readOnly=true) public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại.")); return AssessmentAttemptResult.from(a);}`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetAssessmentResultUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetAttemptStructureUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetBandHistoryUseCase.java`
- Trước: `throw new IllegalArgumentException("Invalid band range, series, skill or page size");`
- Sau: `throw new IllegalArgumentException("Khoảng band, chuỗi dữ liệu, kỹ năng hoặc kích thước trang không hợp lệ.");`
- Trước: `throw new IllegalArgumentException("Invalid band range");`
- Sau: `throw new IllegalArgumentException("Khoảng điểm band không hợp lệ.");`
- Trước: `throw new IllegalArgumentException("Invalid IANA timezone");`
- Sau: `throw new IllegalArgumentException("Múi giờ IANA không hợp lệ.");`
- Trước: `throw new IllegalArgumentException("Invalid cursor");`
- Sau: `throw new IllegalArgumentException("Con trỏ phân trang không hợp lệ.");`
- Trước: `throw new IllegalArgumentException("Invalid cursor");`
- Sau: `throw new IllegalArgumentException("Con trỏ phân trang không hợp lệ.");`
- Trước: `throw new IllegalArgumentException("Invalid source type");`
- Sau: `throw new IllegalArgumentException("Loại nguồn bài đánh giá không hợp lệ.");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy bài đánh giá."));`
- Trước: `throw new AssessmentNotFoundException("Active assessment not found");`
- Sau: `throw new AssessmentNotFoundException("Không tìm thấy bài đánh giá đang hoạt động.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetGradingJobUseCase.java`
- Trước: `@Service public class GetGradingJobUseCase { private final GradingJobRepository jobs; public GetGradingJobUseCase(GradingJobRepository jobs){this.jobs=jobs;} @Transactional(readOnly=true) public GradingJobResult execute(UUID userId,UUID id){var j=jobs.findById(id).orElseThrow(()->new AssessmentNotFoundException("Grading job not found")); if(!j.userId().equals(userId)) throw new AssessmentAccessDeniedException("Grading job does not belong to current user"); return new GradingJobResult(j.id(),j.submissionId(),j.userId(),j.skill(),j.gradingMode(),j.status(),j.pointCostSnapshot(),j.idempotencyKey(),j.createdAt(),j.completedAt());} }`
- Sau: `@Service public class GetGradingJobUseCase { private final GradingJobRepository jobs; public GetGradingJobUseCase(GradingJobRepository jobs){this.jobs=jobs;} @Transactional(readOnly=true) public GradingJobResult execute(UUID userId,UUID id){var j=jobs.findById(id).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài.")); if(!j.userId().equals(userId)) throw new AssessmentAccessDeniedException("Yêu cầu chấm bài này không thuộc về tài khoản hiện tại."); return new GradingJobResult(j.id(),j.submissionId(),j.userId(),j.skill(),j.gradingMode(),j.status(),j.pointCostSnapshot(),j.idempotencyKey(),j.createdAt(),j.completedAt());} }`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetJobEssayUseCase.java`
- Trước: `var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Grading job not found"));`
- Trước: `if (!examiner.equals(job.assignedExaminerId())) throw new AssessmentAccessDeniedException("Claim the job before reading its essay");`
- Trước: `return store.essay(jobId).orElseThrow(()->new AssessmentNotFoundException("Essay not found"));`
- Sau: `var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));`
- Sau: `if (!examiner.equals(job.assignedExaminerId())) throw new AssessmentAccessDeniedException("Vui lòng nhận bài chấm trước khi xem nội dung bài viết.");`
- Sau: `return store.essay(jobId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy bài viết."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetPlacementResultUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Placement attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài kiểm tra đầu vào."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetPracticeHistoryUseCase.java`
- Trước: `if (page < 0 || page > MAX_PAGE || size < 1 || size > 50) throw new IllegalArgumentException("Invalid history page or size");`
- Sau: `if (page < 0 || page > MAX_PAGE || size < 1 || size > 50) throw new IllegalArgumentException("Số trang hoặc kích thước trang lịch sử không hợp lệ.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetPracticeProgressUseCase.java`
- Trước: `if (packageIds.size() > MAX_PACKAGE_IDS) throw new IllegalArgumentException("Too many package ids");`
- Sau: `if (packageIds.size() > MAX_PACKAGE_IDS) throw new IllegalArgumentException("Số lượng gói bài thi vượt quá giới hạn cho phép.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetProvisionalFeedbackUseCase.java`
- Trước: `var attempt=attempts.findByIdAndUserId(attemptId,user).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found"));`
- Trước: `if (attempt.getAttemptType()!=AttemptType.COURSE_GATE) throw new AssessmentNotFoundException("Provisional course feedback not found");`
- Sau: `var attempt=attempts.findByIdAndUserId(attemptId,user).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Sau: `if (attempt.getAttemptType()!=AttemptType.COURSE_GATE) throw new AssessmentNotFoundException("Không tìm thấy nhận xét tạm thời cho bài thi khóa học.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetSpeakingRecordingUrlUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Speaking recording not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy bản ghi âm Speaking."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ListAttemptResponsesUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ListGradingQueueUseCase.java`
- Trước: `if (page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination (size 1..100)");`
- Sau: `if (page<0 || size<1 || size>100) throw new IllegalArgumentException("Thông số phân trang không hợp lệ (kích thước từ 1 đến 100).");`
- Trước: `throw new IllegalArgumentException("Invalid queue filters or pagination (size 1..100)");`
- Sau: `throw new IllegalArgumentException("Bộ lọc hàng chờ hoặc thông số phân trang không hợp lệ (kích thước từ 1 đến 100).");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/PlacementGradingService.java`
- Trước: `.orElseThrow(() -> new InvalidAssessmentStateException("Placement item has no answer snapshot"));`
- Sau: `.orElseThrow(() -> new InvalidAssessmentStateException("Câu hỏi bài kiểm tra đầu vào thiếu dữ liệu đáp án."));`
- Trước: `throw new InvalidAssessmentStateException("Placement section has no valid skill");`
- Sau: `throw new InvalidAssessmentStateException("Phần thi kiểm tra đầu vào chưa có kỹ năng hợp lệ.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/RequestHumanGradingUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Submitted topic essay not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy bài viết đã nộp theo chủ đề."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/SaveAssessmentResultDetailsUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `throw new com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException("Course results can only be edited by an examiner");`
- Sau: `throw new com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException("Chỉ giám khảo mới có quyền chỉnh sửa kết quả bài thi khóa học.");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));`
- Trước: `throw new InvalidAssessmentStateException("Only the latest result version can be graded");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể chấm phiên bản kết quả mới nhất.");`
- Trước: `"A finalized result cannot be changed; create a new result version to regrade");`
- Sau: `"Không thể chỉnh sửa kết quả đã chốt; vui lòng tạo phiên bản kết quả mới để chấm lại.");`
- Trước: `throw new InvalidAssessmentStateException("Only one score per skill is allowed");`
- Sau: `throw new InvalidAssessmentStateException("Mỗi kỹ năng chỉ được ghi nhận một điểm số.");`
- Trước: `"Each item result must reference a distinct assessment item");`
- Sau: `"Mỗi kết quả câu hỏi phải gắn với một câu hỏi riêng biệt.");`
- Trước: `"Item results must reference items from this assessment attempt");`
- Sau: `"Kết quả câu hỏi phải thuộc về lượt làm bài thi này.");`
- Trước: `"Item maximum score must be positive and not below the awarded score");`
- Sau: `"Điểm tối đa của câu hỏi phải lớn hơn 0 và không nhỏ hơn điểm đạt được.");`
- Trước: `throw new InvalidAssessmentStateException("Error analysis IDs must be present and unique");`
- Sau: `throw new InvalidAssessmentStateException("Mã phân tích lỗi phải đầy đủ và không được trùng lặp.");`
- Trước: `"Error analysis must reference an item result from this assessment");`
- Sau: `"Phân tích lỗi phải gắn với một câu hỏi đã chấm trong bài thi này.");`
- Trước: `"Error analysis ID already belongs to another assessment result");`
- Sau: `"Mã phân tích lỗi đã thuộc về một kết quả bài thi khác.");`
- Trước: `throw new InvalidAssessmentStateException("Knowledge judgments require item, knowledge point and judgment");`
- Sau: `throw new InvalidAssessmentStateException("Đánh giá kiến thức cần đầy đủ câu hỏi, điểm kiến thức và mức đánh giá.");`
- Trước: `throw new InvalidAssessmentStateException("Only one judgment per item and knowledge point is allowed");`
- Sau: `throw new InvalidAssessmentStateException("Mỗi cặp câu hỏi và điểm kiến thức chỉ được đánh giá một lần.");`
- Trước: `throw new InvalidAssessmentStateException("Knowledge judgments must reference a graded item of this result");`
- Sau: `throw new InvalidAssessmentStateException("Đánh giá kiến thức phải gắn với câu hỏi đã được chấm trong kết quả này.");`
- Trước: `"Knowledge judgments must reference a knowledge point mapped to the item");`
- Sau: `"Đánh giá kiến thức phải tham chiếu đến điểm kiến thức được gắn với câu hỏi.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/SaveAttemptResponseUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `throw new InvalidAssessmentStateException("Only in-progress attempts accept responses");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể lưu câu trả lời khi lượt làm bài đang diễn ra.");`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Attempt item not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy câu hỏi trong lượt làm bài."));`
- Trước: `throw new InvalidAssessmentStateException("The section of this item is already completed");`
- Sau: `throw new InvalidAssessmentStateException("Phần thi của câu hỏi này đã được hoàn thành.");`
- Trước: `throw new RevisionConflictException("Response revision conflict");`
- Sau: `throw new RevisionConflictException("Dữ liệu câu trả lời đã thay đổi. Vui lòng tải lại.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/StartAssessmentAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Package version not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy phiên bản gói đề thi."));`
- Trước: `if (!practiceTest) throw new PackageNotAttemptableException("Sections can only be selected for practice tests");`
- Sau: `if (!practiceTest) throw new PackageNotAttemptableException("Chỉ bài luyện tập mới hỗ trợ chọn từng phần thi.");`
- Trước: `throw new PackageNotAttemptableException("Selected sections must be unique");`
- Sau: `throw new PackageNotAttemptableException("Các phần thi được chọn không được trùng lặp.");`
- Trước: `throw new PackageNotAttemptableException("A selected section does not belong to this package version");`
- Sau: `throw new PackageNotAttemptableException("Có phần thi được chọn không thuộc phiên bản gói đề này.");`
- Trước: `if (selected.isEmpty()) throw new PackageNotAttemptableException("Package version has no sections");`
- Sau: `if (selected.isEmpty()) throw new PackageNotAttemptableException("Phiên bản gói đề thi chưa có phần thi nào.");`
- Trước: `throw new PackageNotAttemptableException("Practice test section must have a skill");`
- Sau: `throw new PackageNotAttemptableException("Phần thi của bài luyện tập phải có kỹ năng.");`
- Trước: `throw new PackageNotAttemptableException("All selected sections must use the same skill");`
- Sau: `throw new PackageNotAttemptableException("Các phần thi được chọn phải thuộc cùng một kỹ năng.");`
- Trước: `throw new PackageNotAttemptableException("Timed attempts require a time limit on every section");`
- Sau: `throw new PackageNotAttemptableException("Bài làm tính giờ yêu cầu mỗi phần thi phải có giới hạn thời gian.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/StartAttemptSectionUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Attempt section not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy phần thi trong lượt làm bài này."));`
- Trước: `throw new InvalidAssessmentStateException("Only in-progress attempts can start a section");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể bắt đầu phần thi khi lượt làm bài đang diễn ra.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/SubmitAssessmentAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));`
- Sau: `.orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/aggregate/AssessmentAttempt.java`
- Trước: `throw new InvalidAssessmentStateException("Only an in-progress attempt can be submitted");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể nộp bài khi lượt làm bài đang diễn ra.");`
- Trước: `throw new InvalidAssessmentStateException("Only an in-progress attempt can expire");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể hết hạn lượt làm bài đang diễn ra.");`
- Trước: `throw new InvalidAssessmentStateException("Only an in-progress placement attempt can be cancelled");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể hủy bài kiểm tra đầu vào đang làm dở.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/entity/AssessmentResult.java`
- Trước: `throw new InvalidAssessmentStateException("Only a draft or processing result can be graded");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể chấm kết quả đang ở trạng thái nháp hoặc đang xử lý.");`
- Trước: `throw new InvalidAssessmentStateException("Only a draft or processing result can be finalized");`
- Sau: `throw new InvalidAssessmentStateException("Chỉ có thể hoàn tất kết quả đang ở trạng thái nháp hoặc đang xử lý.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/entity/AttemptItemKnowledgePoint.java`
- Trước: `throw new IllegalArgumentException("Knowledge-point weight must not be negative");`
- Sau: `throw new IllegalArgumentException("Trọng số điểm kiến thức không được âm.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/AssessmentAccessDeniedException.java`
- Trước: `public AssessmentAccessDeniedException() { super("You do not have access to this assessment"); }`
- Sau: `public AssessmentAccessDeniedException() { super("Bạn không có quyền truy cập bài thi này."); }`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/AssessmentNotFoundException.java`
- Trước: `public AssessmentNotFoundException(UUID id) { super("Assessment attempt not found: " + id); }`
- Sau: `public AssessmentNotFoundException(UUID id) { super("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."); }`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/AttemptExpiredException.java`
- Trước: `super("Attempt has expired");`
- Sau: `super("Thời gian làm bài đã hết.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/ExaminerChangeNotAllowedException.java`
- Trước: `case NOT_OVERDUE -> "The examiner can still be waited for: the deadline has not passed";`
- Trước: `case LIMIT_REACHED -> "The examiner of this submission was already changed the most times allowed";`
- Trước: `case SAME_EXAMINER -> "Choose an examiner other than the current one";`
- Sau: `case NOT_OVERDUE -> "Chưa quá hạn chấm bài nên chưa thể đổi giám khảo.";`
- Sau: `case LIMIT_REACHED -> "Bạn đã đổi giám khảo cho bài nộp này đạt số lần tối đa cho phép.";`
- Sau: `case SAME_EXAMINER -> "Vui lòng chọn giám khảo khác với giám khảo hiện tại.";`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/ExaminerUnavailableException.java`
- Trước: `case FULL -> "The examiner has reached the limit of submissions for today";`
- Trước: `case NOT_ACCEPTING -> "The examiner is not accepting submissions";`
- Trước: `case OVERDUE_BACKLOG -> "The examiner has too many late submissions and takes no new ones for now";`
- Sau: `case FULL -> "Giám khảo đã nhận đủ số bài chấm trong hôm nay. Vui lòng chọn giám khảo khác.";`
- Sau: `case NOT_ACCEPTING -> "Giám khảo hiện không nhận chấm bài mới. Vui lòng chọn giám khảo khác.";`
- Sau: `case OVERDUE_BACKLOG -> "Giám khảo đang có quá nhiều bài quá hạn nên tạm thời không nhận thêm bài mới.";`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/GradingCancelNotAllowedException.java`
- Trước: `case NOT_OVERDUE -> "The examiner can still be waited for: the deadline has not passed";`
- Trước: `case CHANGES_LEFT -> "The examiner can still be changed; cancel once no change is left";`
- Sau: `case NOT_OVERDUE -> "Chưa quá hạn chấm bài nên chưa thể hủy yêu cầu chấm.";`
- Sau: `case CHANGES_LEFT -> "Bạn vẫn còn lượt đổi giám khảo; chỉ có thể hủy sau khi đã hết lượt đổi.";`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/HumanGradingCreditConflictException.java`
- Trước: `super("The human grading request conflicts with a previous request");`
- Sau: `super("Yêu cầu chấm bài bởi giám khảo bị xung đột với một yêu cầu trước đó.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/HumanGradingCreditInsufficientException.java`
- Trước: `super("No human grading credits are available");`
- Sau: `super("Bạn không đủ lượt chấm bài bởi giám khảo. Vui lòng mua thêm lượt để tiếp tục.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/JobAlreadyClaimedException.java`
- Trước: `public JobAlreadyClaimedException() { super("Grading job is already claimed or no longer available"); }`
- Sau: `public JobAlreadyClaimedException() { super("Bài chấm này đã có giám khảo khác nhận hoặc không còn khả dụng."); }`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/PlacementLimitReachedException.java`
- Trước: `public PlacementLimitReachedException() { super("Placement attempts are limited to three per rolling 24 hours"); }`
- Sau: `public PlacementLimitReachedException() { super("Bạn đã đạt giới hạn tối đa 3 lần làm bài kiểm tra đầu vào trong 24 giờ qua."); }`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/RevisionConflictException.java`
- Trước: `super("Response revision conflict. Expected " + expected + " but was " + actual);`
- Sau: `super("Dữ liệu bài làm đã thay đổi trên phiên khác. Vui lòng tải lại và thử lại.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/service/AgreementMetrics.java`
- Trước: `throw new IllegalArgumentException("Agreement scores must be non-empty lists of equal size");`
- Sau: `throw new IllegalArgumentException("Danh sách điểm đối chiếu không được rỗng và phải có cùng số lượng.");`
- Trước: `throw new IllegalArgumentException("Bands must be between 0 and 9 in half-band steps");`
- Sau: `throw new IllegalArgumentException("Điểm band phải nằm trong khoảng từ 0 đến 9 theo bước 0,5.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/service/AnswerSpecGrader.java`
- Trước: `throw new IllegalArgumentException("Answer must be a string or null.");`
- Sau: `throw new IllegalArgumentException("Câu trả lời phải là chuỗi ký tự hoặc để trống.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/service/PlacementBandCalculator.java`
- Trước: `throw new IllegalArgumentException("Only Listening and Reading are converted from correct answers");`
- Sau: `throw new IllegalArgumentException("Chỉ kỹ năng Listening và Reading được quy đổi từ số câu trả lời đúng.");`
- Trước: `throw new IllegalArgumentException("At least one skill band is required");`
- Sau: `throw new IllegalArgumentException("Cần ít nhất một điểm band kỹ năng để tính điểm tổng.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/vo/AttemptType.java`
- Trước: `throw new PackageNotAttemptableException("Package type is missing");`
- Sau: `throw new PackageNotAttemptableException("Thiếu thông tin loại gói bài thi.");`
- Trước: `default -> throw new PackageNotAttemptableException("Packages of type " + packageType`
- Trước: `+ " cannot be taken as an assessment attempt");`
- Sau: `default -> throw new PackageNotAttemptableException("Loại gói đề này không thể dùng để làm bài đánh giá.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcExaminerQaRepository.java`
- Trước: `if (!exists) throw new AssessmentNotFoundException("QA grading job not found");`
- Sau: `if (!exists) throw new AssessmentNotFoundException("Không tìm thấy bài chấm kiểm tra chất lượng.");`
- Trước: `if (!assigned) throw new AssessmentAccessDeniedException("QA job is not assigned to this reviewer");`
- Trước: `throw new InvalidAssessmentStateException("QA review was already submitted or the reviewer is not independent");`
- Sau: `if (!assigned) throw new AssessmentAccessDeniedException("Bài chấm kiểm tra chất lượng này không được phân công cho bạn.");`
- Sau: `throw new InvalidAssessmentStateException("Bài đánh giá chất lượng đã được gửi hoặc người đánh giá không độc lập.");`
### `services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcHumanReviewStore.java`
- Trước: `if (!same(stored, review)) throw new RevisionConflictException("requestId was already used for another review");`
- Sau: `if (!same(stored, review)) throw new RevisionConflictException("Mã yêu cầu này đã được sử dụng cho một yêu cầu chấm bài khác.");`
- Trước: `if (sourceExists) throw new RevisionConflictException("Essay already has a human review request");`
- Trước: `throw new RevisionConflictException("Human review request conflicts with existing data");`
- Sau: `if (sourceExists) throw new RevisionConflictException("Bài viết này đã có yêu cầu chấm bởi giám khảo.");`
- Sau: `throw new RevisionConflictException("Yêu cầu chấm bài xung đột với dữ liệu hiện có.");`
- Trước: `.stream().findFirst().orElseThrow(() -> new AssessmentNotFoundException("Human review job not found"));`
- Trước: `if (job.submissionId() == null) throw new AssessmentNotFoundException("Standalone human review not found");`
- Sau: `.stream().findFirst().orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));`
- Sau: `if (job.submissionId() == null) throw new AssessmentNotFoundException("Không tìm thấy bài viết cần giám khảo chấm.");`
- Trước: `throw new RevisionConflictException("Human review has already been completed");`
- Sau: `throw new RevisionConflictException("Bài chấm này đã được hoàn thành trước đó.");`
- Trước: `if ("CANCELLED".equals(job.status())) throw new RevisionConflictException("The learner cancelled this request");`
- Sau: `if ("CANCELLED".equals(job.status())) throw new RevisionConflictException("Học viên đã hủy yêu cầu chấm bài này.");`
- Trước: `throw new AssessmentAccessDeniedException("Claim the human review before completing it");`
- Sau: `throw new AssessmentAccessDeniedException("Vui lòng nhận bài chấm trước khi hoàn thành.");`
- Trước: `if (changed != 1) throw new RevisionConflictException("Human review has already been completed");`
- Sau: `if (changed != 1) throw new RevisionConflictException("Bài chấm này đã được hoàn thành trước đó.");`
- Trước: `if (states.isEmpty()) return new AssessmentNotFoundException("Standalone human review not found");`
- Sau: `if (states.isEmpty()) return new AssessmentNotFoundException("Không tìm thấy bài viết cần giám khảo chấm.");`
- Trước: `return new RevisionConflictException("Human review has already been completed");`
- Sau: `return new RevisionConflictException("Bài chấm này đã được hoàn thành trước đó.");`
- Trước: `if ("CANCELLED".equals(states.getFirst())) return new RevisionConflictException("The learner cancelled this request");`
- Trước: `return new AssessmentAccessDeniedException("Claim the human review before " + action);`
- Sau: `if ("CANCELLED".equals(states.getFirst())) return new RevisionConflictException("Học viên đã hủy yêu cầu chấm bài này.");`
- Sau: `return new AssessmentAccessDeniedException("Vui lòng nhận bài chấm trước khi thực hiện thao tác này.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/api/controller/PlacementController.java`
- Trước: `throw new LearningRequestException(400, "INVALID_PLACEMENT_POOL", "Placement pool is invalid");`
- Sau: `throw new LearningRequestException(400, "INVALID_PLACEMENT_POOL", "Nhóm đề kiểm tra đầu vào không hợp lệ.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/api/controller/PracticeController.java`
- Trước: `"skill must be LISTENING, READING, WRITING or SPEAKING");`
- Sau: `"Kỹ năng phải là LISTENING, READING, WRITING hoặc SPEAKING.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/api/controller/ReviewController.java`
- Trước: `throw new LearningRequestException(400, "INVALID_LIMIT", "limit must be between 1 and 100");`
- Sau: `throw new LearningRequestException(400, "INVALID_LIMIT", "Giới hạn số lượng phải từ 1 đến 100.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/api/exception/GlobalExceptionHandler.java`
- Trước: `.body(new LearningErrorResponse(exception.getMessage(), "UNKNOWN_DEAD_LETTER_QUEUE", null));`
- Sau: `"UNKNOWN_DEAD_LETTER_QUEUE", null));`
- Trước: `.body(new LearningErrorResponse(exception.getMessage(), "INVALID_DEAD_LETTER_BATCH", null));`
- Sau: `"INVALID_DEAD_LETTER_BATCH", null));`
- Trước: `.body(new LearningErrorResponse(exception.getMessage(), "ATTEMPT_ALREADY_SUBMITTED", null));`
- Sau: `"ATTEMPT_ALREADY_SUBMITTED", null));`
- Trước: `.body(new LearningErrorResponse(exception.getMessage(), "HEARTS_DEPLETED", null, null, null,`
- Sau: `"HEARTS_DEPLETED", null, null, null,`
- Trước: `.body(new LearningErrorResponse("Not enough points", "INSUFFICIENT_POINTS", null));`
- Sau: `.body(new LearningErrorResponse("Bạn không đủ điểm để thực hiện thao tác này.", "INSUFFICIENT_POINTS", null));`
- Trước: `.body(new LearningErrorResponse("Points service is unavailable; try again later",`
- Sau: `.body(new LearningErrorResponse("Dịch vụ điểm thưởng tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Trước: `return ResponseEntity.unprocessableEntity().body(new LearningErrorResponse("Invalid request", null, null));`
- Sau: `new LearningErrorResponse("Dữ liệu gửi lên không hợp lệ.", null, null));`
- Trước: `return ResponseEntity.badRequest().body(new LearningErrorResponse("Required header is missing",`
- Trước: `"INVALID_REQUEST", null));`
- Sau: `new LearningErrorResponse("Thiếu thông tin bắt buộc trong yêu cầu.", "INVALID_REQUEST", null));`
- Trước: `return ResponseEntity.status(status).body(new LearningErrorResponse("The recording was refused",`
- Sau: `"Bản ghi âm không hợp lệ hoặc không được chấp nhận.",`
- Trước: `"The recording is too large", "RECORDING_TOO_LARGE", null));`
- Sau: `"Tệp ghi âm vượt quá dung lượng cho phép.", "RECORDING_TOO_LARGE", null));`
- Trước: `"Recording storage is unavailable", "STORAGE_UNAVAILABLE", null));`
- Sau: `"Dịch vụ lưu trữ bản ghi âm tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Sau: `"STORAGE_UNAVAILABLE", null));`
- Trước: `return ResponseEntity.unprocessableEntity().body(new LearningErrorResponse("Invalid request", null, null));`
- Sau: `new LearningErrorResponse("Dữ liệu gửi lên không hợp lệ.", null, null));`
- Trước: `return build(HttpStatus.FORBIDDEN, "Access denied", request, null);`
- Sau: `return build(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này.", request, null);`
- Trước: `return build(HttpStatus.NOT_FOUND, "Not found", request, null);`
- Sau: `return build(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu được yêu cầu.", request, null);`
- Trước: `return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, null);`
- Sau: `return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức yêu cầu không được hỗ trợ.", request, null);`
- Trước: `return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", request, null);`
- Sau: `return build(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi nội bộ, vui lòng thử lại sau.", request, null);`
- Sau: `log.warn("Replaced non-Vietnamese error message (status={}): {}", status, message);`
### `services/learning-service/src/main/java/com/ieltspath/learning/api/exception/StudyDataExceptionHandler.java`
- Trước: `return error(status, exception.getCode(), request, Map.of("code", exception.getCode()));`
- Sau: `case "SESSION_NOT_FOUND" -> "Không tìm thấy phiên học. Vui lòng bắt đầu phiên học mới.";`
- Sau: `case "SOURCE_NOT_AVAILABLE" -> "Nguồn học hiện không khả dụng hoặc không tồn tại.";`
- Sau: `case "SOURCE_UNAVAILABLE" -> "Không thể xác minh nguồn học lúc này. Vui lòng thử lại sau.";`
- Sau: `case "SESSION_IN_USE" -> "Phiên học đang hoạt động trên thiết bị hoặc tab khác.";`
- Sau: `case "SESSION_CLOSED" -> "Phiên học đã kết thúc. Vui lòng mở phiên học mới.";`
- Sau: `case "LEASE_EXPIRED" -> "Phiên học đã hết hạn giữ chỗ. Vui lòng mở lại phiên học.";`
- Sau: `case "STALE_EPOCH" -> "Phiên học đã được làm mới ở nơi khác. Vui lòng tải lại.";`
- Sau: `case "INVALID_SEQUENCE" -> "Thứ tự nhịp học không hợp lệ. Vui lòng đồng bộ lại phiên học.";`
- Sau: `case "PAYLOAD_CONFLICT" -> "Yêu cầu xung đột với dữ liệu phiên học đã ghi nhận.";`
- Sau: `return error(status, message, request, Map.of("code", exception.getCode()));`
- Sau: `log.warn("Replaced non-Vietnamese error message (status={}): {}", status.value(), message);`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/exception/InsufficientPointsException.java`
- Trước: `super("Insufficient points");`
- Sau: `super("Bạn không đủ điểm để thực hiện thao tác này.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/AnswerSheet.java`
- Trước: `return new LearningRequestException(422, "INVALID_ANSWERS", "Submit one answer for every exercise question");`
- Sau: `return new LearningRequestException(422, "INVALID_ANSWERS", "Vui lòng trả lời đầy đủ tất cả câu hỏi trong bài tập.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/CourseMilestones.java`
- Trước: `"The practice test milestones of this course are not known yet"));`
- Sau: `"Chưa xác định được các mốc bài thi luyện tập của khóa học này."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/EssaySubmissionFlow.java`
- Trước: `throw new LearningRequestException(422, "ESSAY_EMPTY", "The essay is empty");`
- Sau: `throw new LearningRequestException(422, "ESSAY_EMPTY", "Bài viết không được để trống.");`
- Trước: `"The essay needs at least " + settings.minWords() + " words");`
- Sau: `"Bài viết cần tối thiểu " + settings.minWords() + " từ.");`
- Trước: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable");`
- Sau: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Dịch vụ thanh toán tạm thời không khả dụng.");`
- Trước: `"Grading costs " + settings.pointCost() + " points");`
- Sau: `"Chấm bài cần " + settings.pointCost() + " điểm.");`
- Trước: `throw new LearningRequestException(429, "DAILY_LIMIT_REACHED", "Daily grading limit reached");`
- Sau: `throw new LearningRequestException(429, "DAILY_LIMIT_REACHED", "Bạn đã đạt giới hạn số lần chấm bài trong hôm nay.");`
- Trước: `throw new LearningRequestException(402, "INSUFFICIENT_POINTS", "Not enough points to release the grade",`
- Sau: `throw new LearningRequestException(402, "INSUFFICIENT_POINTS", "Bạn không đủ điểm để nhận kết quả chấm bài.",`
- Trước: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable; resend later",`
- Sau: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Dịch vụ thanh toán tạm thời không khả dụng. Vui lòng thử lại sau.",`
- Trước: `return new LearningRequestException(422, "ESSAY_TOO_LONG", "The essay is too long");`
- Sau: `return new LearningRequestException(422, "ESSAY_TOO_LONG", "Bài viết vượt quá độ dài cho phép.");`
- Trước: `return new LearningRequestException(503, "GRADING_UNAVAILABLE", "Grading is unavailable; nothing was charged",`
- Sau: `return new LearningRequestException(503, "GRADING_UNAVAILABLE", "Dịch vụ chấm bài tạm thời không khả dụng; bạn chưa bị trừ điểm.",`
- Trước: `return new LearningRequestException(409, "GRADING_IN_PROGRESS", "This essay block is already being graded");`
- Sau: `return new LearningRequestException(409, "GRADING_IN_PROGRESS", "Bài viết này đang được chấm. Vui lòng chờ kết quả.");`
- Trước: `return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `return new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/FreePracticeSubmission.java`
- Trước: `"Free practice does not take essays");`
- Sau: `"Luyện tập tự do không hỗ trợ bài viết luận.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/FreeWritingPrompts.java`
- Trước: `"Free writing is only available for the prompts of a practice test");`
- Sau: `"Luyện viết tự do chỉ áp dụng cho đề bài thuộc bài luyện tập.");`
- Trước: `"This writing prompt was not found in the test"));`
- Sau: `"Không tìm thấy đề bài viết này trong bài luyện tập."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/service/WritingSettings.java`
- Trước: `@AssertTrue(message = "minWords must be below maxWords")`
- Sau: `@AssertTrue(message = "Số từ tối thiểu phải nhỏ hơn số từ tối đa.")`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/AssignCourseTestUseCase.java`
- Trước: `throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found");`
- Sau: `throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Không tìm thấy khóa học.");`
- Trước: `throw new LearningRequestException(409, "NO_COURSE_TEST", "This course has no final test");`
- Sau: `throw new LearningRequestException(409, "NO_COURSE_TEST", "Khóa học này không có bài kiểm tra cuối khóa.");`
- Trước: `throw new LearningRequestException(403, "COURSE_TEST_LOCKED", "Complete all course topics first");`
- Sau: `throw new LearningRequestException(403, "COURSE_TEST_LOCKED", "Vui lòng hoàn thành tất cả các chủ đề trong khóa học trước.");`
- Trước: `throw new LearningRequestException(409, "COURSE_ALREADY_PASSED", "This course has already been passed");`
- Sau: `throw new LearningRequestException(409, "COURSE_ALREADY_PASSED", "Bạn đã hoàn thành đạt yêu cầu khóa học này.");`
- Trước: `"Finish the reviews from the course test before taking it again");`
- Sau: `"Vui lòng hoàn thành các bài ôn tập bổ sung từ bài kiểm tra khóa học trước khi thi lại.");`
- Trước: `"No published course test is available"));`
- Sau: `"Hiện chưa có đề kiểm tra cuối khóa nào được xuất bản."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/AssignPlacementTestUseCase.java`
- Trước: `"No published placement test is available");`
- Sau: `"Hiện chưa có bài kiểm tra đầu vào nào được xuất bản.");`
- Trước: `return new LearningRequestException(429, "PLACEMENT_LIMIT_REACHED", "Placement limit reached");`
- Sau: `return new LearningRequestException(429, "PLACEMENT_LIMIT_REACHED", "Bạn đã đạt giới hạn số lần làm bài kiểm tra đầu vào.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/AssignPracticeTestUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404,"PRACTICE_TEST_NOT_FOUND","Practice test is not a milestone of this course"));`
- Trước: `if ("LOCKED".equals(milestone.status())) throw new LearningRequestException(403,"PRACTICE_TEST_LOCKED","Pass the milestone topic first");`
- Sau: `.orElseThrow(() -> new LearningRequestException(404,"PRACTICE_TEST_NOT_FOUND","Bài luyện tập không thuộc cột mốc của khóa học này."));`
- Sau: `if ("LOCKED".equals(milestone.status())) throw new LearningRequestException(403,"PRACTICE_TEST_LOCKED","Vui lòng hoàn thành chủ đề cột mốc trước.");`
- Trước: `"This practice test was assigned in another course");`
- Sau: `"Bài luyện tập này đã được giao trong một khóa học khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/AssignTopicTestUseCase.java`
- Trước: `throw new LearningRequestException(409, "NO_TOPIC_TEST", "This topic has no final test");`
- Sau: `throw new LearningRequestException(409, "NO_TOPIC_TEST", "Chủ đề này không có bài kiểm tra cuối chủ đề.");`
- Trước: `throw new LearningRequestException(409, "PRACTICE_REQUIRED", "Complete practice for these lessons",`
- Sau: `throw new LearningRequestException(409, "PRACTICE_REQUIRED", "Vui lòng hoàn thành phần luyện tập của các bài học này trước.",`
- Trước: `throw new LearningRequestException(403, "TEST_LOCKED", "Complete the topic lessons first");`
- Sau: `throw new LearningRequestException(403, "TEST_LOCKED", "Vui lòng hoàn thành các bài học trong chủ đề trước.");`
- Trước: `"No published test code is available"));`
- Sau: `"Hiện chưa có đề kiểm tra chủ đề nào được xuất bản."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/BadgesUseCase.java`
- Trước: `throw new LearningRequestException(404, "BADGE_NOT_FOUND", "No badge with this code");`
- Sau: `throw new LearningRequestException(404, "BADGE_NOT_FOUND", "Không tìm thấy huy hiệu với mã này.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ChangeLearningGoalStatusUseCase.java`
- Trước: `throw new LearningGoalConflictException("An active learning goal already exists for this user");`
- Sau: `throw new LearningGoalConflictException("Bạn đã có một mục tiêu học tập đang hoạt động.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CompleteLessonUseCase.java`
- Trước: `throw new LearningRequestException(409, "LESSON_HAS_EXERCISES", "Lesson contains exercise blocks");`
- Sau: `throw new LearningRequestException(409, "LESSON_HAS_EXERCISES", "Bài học này có phần bài tập cần hoàn thành.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CompleteSpeakingSubmissionUseCase.java`
- Trước: `"Speaking submission not found"));`
- Sau: `"Không tìm thấy bài nộp Speaking."));`
- Trước: `submission.missingRecordings() + " question(s) have no recording yet", submissionId);`
- Sau: `"Còn " + submission.missingRecordings() + " câu hỏi chưa có bản ghi âm.", submissionId);`
- Trước: `"This upload was given up; start a new submission", submissionId);`
- Sau: `"Lượt tải lên này đã bị hủy; vui lòng tạo bài nộp mới.", submissionId);`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CreateLearningGoalUseCase.java`
- Trước: `throw new LearningGoalConflictException("An active learning goal already exists for this user");`
- Sau: `throw new LearningGoalConflictException("Bạn đã có một mục tiêu học tập đang hoạt động.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CreateLessonSpeakingSubmissionUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
- Trước: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");`
- Sau: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "Lượt luyện tập này đã được nộp trước đó.");`
- Trước: `"Free practice does not take recorded answers here");`
- Sau: `"Luyện tập tự do không hỗ trợ nộp bài ghi âm tại đây.");`
- Trước: `"This attempt already has a graded answer; start a new attempt to answer again");`
- Sau: `"Lượt luyện tập này đã có bài nói được chấm; vui lòng tạo lượt mới để trả lời lại.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CreateSpeakingSubmissionUseCase.java`
- Trước: `"AI Speaking grading is not available yet");`
- Sau: `"Chức năng chấm bài Speaking bằng AI hiện chưa khả dụng.");`
- Trước: `throw new LearningRequestException(422, "CONSENT_REQUIRED", "Consent to process the recordings is required");`
- Sau: `throw new LearningRequestException(422, "CONSENT_REQUIRED", "Cần đồng ý xử lý bản ghi âm để tiếp tục.");`
- Trước: `"You already have a Speaking submission waiting to be graded", active.id());`
- Sau: `"Bạn đang có một bài nộp Speaking khác đang chờ chấm.", active.id());`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "This request id was used for another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable");`
- Sau: `throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Dịch vụ thanh toán tạm thời không khả dụng.");`
- Trước: `"Grading costs " + settings.pointCost() + " points");`
- Sau: `"Chấm bài cần " + settings.pointCost() + " điểm.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetActivityStatsUseCase.java`
- Trước: `if (to.isBefore(from)) throw new LearningRequestException(400, "INVALID_RANGE", "to must not be before from");`
- Sau: `if (to.isBefore(from)) throw new LearningRequestException(400, "INVALID_RANGE", "Ngày kết thúc không được trước ngày bắt đầu.");`
- Trước: `throw new LearningRequestException(400, "RANGE_TOO_LONG", "A range may cover at most 180 days");`
- Sau: `throw new LearningRequestException(400, "RANGE_TOO_LONG", "Khoảng thời gian thống kê tối đa 180 ngày.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetCourseScoreUseCase.java`
- Trước: `if (!known) throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found");`
- Sau: `if (!known) throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Không tìm thấy khóa học.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetDiagnosticProbeUseCase.java`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Probe changed while it was being opened");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán đã thay đổi trong lúc mở.");`
- Trước: `return new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found");`
- Sau: `return new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy câu hỏi kiểm tra chẩn đoán.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetDiagnosticUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy phiên chẩn đoán."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetEssayHistoryUseCase.java`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLeagueUseCase.java`
- Trước: `throw new LearningRequestException(400, "INVALID_WEEKS", "weeks must be between 1 and " + MAX_HISTORY_WEEKS);`
- Sau: `throw new LearningRequestException(400, "INVALID_WEEKS", "Số tuần phải từ 1 đến " + MAX_HISTORY_WEEKS + ".");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLearnerTreeUseCase.java`
- Trước: `if (topics.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Course was not found");`
- Sau: `if (topics.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy khóa học.");`
- Trước: `throw new LearningRequestException(403, "COURSE_NOT_OPEN", "The course is closed");`
- Sau: `throw new LearningRequestException(403, "COURSE_NOT_OPEN", "Khóa học này hiện đang đóng.");`
- Trước: `if (!taking) throw new LearningRequestException(403, "COURSE_NOT_JOINED", "The learner is not taking this course");`
- Sau: `if (!taking) throw new LearningRequestException(403, "COURSE_NOT_JOINED", "Bạn chưa tham gia khóa học này.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLearningProgressStatisticsUseCase.java`
- Trước: `throw new LearningRequestException(400, "INVALID_PAGE", "Page must be 0..10000 and size 1..20");`
- Sau: `throw new LearningRequestException(400, "INVALID_PAGE", "Số trang phải từ 0 đến 10000 và kích thước từ 1 đến 20.");`
- Trước: `"Progress statistics support at most 100 started courses");`
- Sau: `"Thống kê tiến độ hỗ trợ tối đa 100 khóa học đã bắt đầu.");`
- Trước: `if (activities.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Started course was not found");`
- Sau: `if (activities.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy khóa học đã bắt đầu.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLegacyPlacementTestUseCase.java`
- Trước: `throw new LearningRequestException(409, "PLACEMENT_ALREADY_DONE", "The placement test was already taken");`
- Sau: `throw new LearningRequestException(409, "PLACEMENT_ALREADY_DONE", "Bạn đã hoàn thành bài kiểm tra đầu vào.");`
- Trước: `throw new LearningRequestException(404, "NO_PLACEMENT_TEST", "No placement test is published");`
- Sau: `throw new LearningRequestException(404, "NO_PLACEMENT_TEST", "Hiện chưa có bài kiểm tra đầu vào nào được xuất bản.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetPracticeAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetPracticeSinceEssayUseCase.java`
- Trước: `throw new IllegalArgumentException("at most " + MAX_POINTS + " knowledge points");`
- Sau: `throw new IllegalArgumentException("Chỉ được chọn tối đa " + MAX_POINTS + " điểm kiến thức.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetReviewUseCase.java`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review changed while it was being opened");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bài ôn tập đã thay đổi trong lúc mở.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài ôn tập."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetSnapshotReportsUseCase.java`
- Trước: `throw new LearningRequestException(400, "INVALID_WEEK_START", "weekStart must be a Monday");`
- Sau: `throw new LearningRequestException(400, "INVALID_WEEK_START", "Ngày bắt đầu tuần phải là thứ Hai.");`
- Trước: `"weeks must be between " + MIN_HISTORY_WEEKS + " and " + MAX_HISTORY_WEEKS);`
- Sau: `"Số tuần phải nằm trong khoảng từ " + MIN_HISTORY_WEEKS + " đến " + MAX_HISTORY_WEEKS + ".");`
- Trước: `throw new LearningRequestException(400, "INVALID_MONTH_START", "monthStart must be the first of a month");`
- Sau: `throw new LearningRequestException(400, "INVALID_MONTH_START", "Ngày bắt đầu tháng phải là ngày mùng 1.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetSpeakingSubmissionUseCase.java`
- Trước: `"Speaking submission not found"));`
- Sau: `"Không tìm thấy bài nộp Speaking."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetStudyContextUseCase.java`
- Trước: `throw new LearningRequestException(400, "INVALID_SOURCE", "Unsupported study source");`
- Sau: `throw new LearningRequestException(400, "INVALID_SOURCE", "Loại nguồn học không được hỗ trợ.");`
- Trước: `return new LearningRequestException(404, "NOT_FOUND", "Study source was not found");`
- Sau: `return new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy nguồn học.");`
- Trước: `return new LearningRequestException(409, "STUDY_SOURCE_INACTIVE", "Study source is no longer active");`
- Sau: `return new LearningRequestException(409, "STUDY_SOURCE_INACTIVE", "Nguồn học này không còn hoạt động.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetStudyTimeStatisticsUseCase.java`
- Trước: `throw new IllegalArgumentException("Study-time range must contain 1 to 366 days");`
- Trước: `if(!Set.of("DAY","WEEK","MONTH").contains(grain)) throw new IllegalArgumentException("grain must be DAY, WEEK or MONTH");`
- Sau: `throw new IllegalArgumentException("Khoảng thời gian thống kê phải từ 1 đến 366 ngày.");`
- Sau: `if(!Set.of("DAY","WEEK","MONTH").contains(grain)) throw new IllegalArgumentException("Đơn vị thống kê phải là DAY, WEEK hoặc MONTH.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetWritingProgressUseCase.java`
- Trước: `throw new LearningRequestException(400, "INVALID_LIMIT", "limit must be between 3 and 20");`
- Sau: `throw new LearningRequestException(400, "INVALID_LIMIT", "Giới hạn số lượng phải từ 3 đến 20.");`
- Trước: `new LearningRequestException(400, "INVALID_TASK", "task must be TASK_1 or TASK_2"));`
- Sau: `new LearningRequestException(400, "INVALID_TASK", "Loại bài viết phải là TASK_1 hoặc TASK_2."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetWritingSubmissionUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/HumanReviewUseCase.java`
- Trước: `throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Choose the examiner who will grade the essay");`
- Sau: `throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Vui lòng chọn giám khảo chấm bài.");`
- Trước: `"Only a graded essay, or one sent to an examiner, can be sent to a Mentor");`
- Sau: `"Chỉ bài viết đã chấm hoặc đang chờ giám khảo mới có thể gửi chấm bởi giám khảo.");`
- Trước: `"NO_HUMAN_REVIEW", "No Mentor review was asked for this essay"));`
- Sau: `"NO_HUMAN_REVIEW", "Bài viết này chưa có yêu cầu chấm bởi giám khảo."));`
- Trước: `"NO_HUMAN_REVIEW", "No Mentor review was asked for this essay"));`
- Sau: `"NO_HUMAN_REVIEW", "Bài viết này chưa có yêu cầu chấm bởi giám khảo."));`
- Trước: `"This request was cancelled and its credit given back; send the essay again as a new one");`
- Sau: `"Yêu cầu chấm này đã bị hủy và lượt chấm đã được hoàn lại; vui lòng gửi bài viết lại như một bài mới.");`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ListPracticeTestsUseCase.java`
- Trước: `if (topics.isEmpty()) throw new LearningRequestException(404,"COURSE_NOT_FOUND","Active course not found");`
- Sau: `if (topics.isEmpty()) throw new LearningRequestException(404,"COURSE_NOT_FOUND","Không tìm thấy khóa học đang hoạt động.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ListSubmissionHistoryUseCase.java`
- Trước: `throw new IllegalArgumentException("skill must be WRITING or SPEAKING for free practice");`
- Sau: `throw new IllegalArgumentException("Kỹ năng luyện tập tự do phải là WRITING hoặc SPEAKING.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/RecordStudyHeartbeatUseCase.java`
- Trước: `if(epoch<1 || sequence<1 || intervals==null || intervals.size()>16) throw new IllegalArgumentException("Invalid heartbeat");`
- Sau: `if(epoch<1 || sequence<1 || intervals==null || intervals.size()>16) throw new IllegalArgumentException("Nhịp học không hợp lệ.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ReleasePlacementAssignmentUseCase.java`
- Trước: `new LearningRequestException(404, "PLACEMENT_ASSIGNMENT_NOT_FOUND", "Placement assignment not found"));`
- Sau: `new LearningRequestException(404, "PLACEMENT_ASSIGNMENT_NOT_FOUND", "Không tìm thấy lượt giao bài kiểm tra đầu vào."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ReviewPracticeAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
- Trước: `throw new LearningRequestException(409, "ATTEMPT_NOT_SUBMITTED", "Practice attempt is still open");`
- Sau: `throw new LearningRequestException(409, "ATTEMPT_NOT_SUBMITTED", "Lượt luyện tập này vẫn đang mở.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SettleSpeakingSubmissionUseCase.java`
- Trước: `"Speaking submission not found"));`
- Sau: `"Không tìm thấy bài nộp Speaking."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SkipDiagnosticUseCase.java`
- Trước: `"Answer at least one diagnostic probe before skipping");`
- Sau: `"Vui lòng trả lời ít nhất một câu hỏi kiểm tra chẩn đoán trước khi bỏ qua.");`
- Trước: `return new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found");`
- Sau: `return new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy phiên chẩn đoán.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/StaffWorkUseCase.java`
- Trước: `throw new LearningRequestException(403, "QUEUE_NOT_ALLOWED", "This queue is not yours to see");`
- Sau: `throw new LearningRequestException(403, "QUEUE_NOT_ALLOWED", "Bạn không có quyền xem hàng chờ này.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Work item was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy đầu việc."));`
- Trước: `throw new LearningRequestException(422, "NOTE_REQUIRED", "A note shown to the learner cannot be empty");`
- Sau: `throw new LearningRequestException(422, "NOTE_REQUIRED", "Ghi chú hiển thị cho học viên không được để trống.");`
- Trước: `throw new LearningRequestException(422, "NO_LEARNER", "This item has no learner to show a note to");`
- Sau: `throw new LearningRequestException(422, "NO_LEARNER", "Đầu việc này không có học viên để gửi ghi chú.");`
- Trước: `throw new LearningRequestException(422, "INVALID_FLAG_STATUS", "A flag is confirmed or dismissed");`
- Sau: `throw new LearningRequestException(422, "INVALID_FLAG_STATUS", "Trạng thái cờ chỉ có thể là xác nhận hoặc bác bỏ.");`
- Trước: `insight.findFlag(flagId).orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Flag was not found"));`
- Sau: `insight.findFlag(flagId).orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy cờ cảnh báo."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/StartBasketReviewUseCase.java`
- Trước: `"This knowledge point is not in the review basket");`
- Sau: `"Điểm kiến thức này không nằm trong giỏ ôn tập.");`
- Trước: `"No completed lesson teaches this knowledge point"));`
- Sau: `"Chưa có bài học nào đã hoàn thành giảng dạy điểm kiến thức này."));`
- Trước: `"This knowledge point is not in the review basket");`
- Sau: `"Điểm kiến thức này không nằm trong giỏ ôn tập.");`
- Trước: `"This knowledge point is not in the review basket");`
- Sau: `"Điểm kiến thức này không nằm trong giỏ ôn tập.");`
- Trước: `"NO_LESSON_FOR_KNOWLEDGE_POINT", "No completed lesson teaches this knowledge point"));`
- Sau: `"NO_LESSON_FOR_KNOWLEDGE_POINT", "Chưa có bài học nào đã hoàn thành giảng dạy điểm kiến thức này."));`
- Trước: `"No practice set the learner has not seen is left for this knowledge point");`
- Sau: `"Hiện không còn bộ bài tập mới nào cho điểm kiến thức này.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/StartFreePracticeUseCase.java`
- Trước: `"FREE_PRACTICE_UNAVAILABLE", "This knowledge point has no skill to practise"));`
- Sau: `"FREE_PRACTICE_UNAVAILABLE", "Điểm kiến thức này chưa có kỹ năng để luyện tập."));`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Knowledge point was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy điểm kiến thức."));`
- Trước: `throw new LearningRequestException(403, "COURSE_NOT_OPEN", "The course of this knowledge point is closed");`
- Sau: `throw new LearningRequestException(403, "COURSE_NOT_OPEN", "Khóa học chứa điểm kiến thức này hiện đang đóng.");`
- Trước: `throw new LearningRequestException(403, "COURSE_NOT_JOINED", "The learner is not taking this course");`
- Sau: `throw new LearningRequestException(403, "COURSE_NOT_JOINED", "Bạn chưa tham gia khóa học này.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Knowledge point was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy điểm kiến thức."));`
- Trước: `"No practice set is left for this knowledge point; the rest is kept for reviews");`
- Sau: `"Hiện không còn bộ bài tập luyện tập tự do nào cho điểm kiến thức này; phần còn lại được dành cho bài ôn tập.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/StreakUseCase.java`
- Trước: `throw new LearningRequestException(409, "FREEZE_ALREADY_OWNED", "A freeze is already owned");`
- Sau: `throw new LearningRequestException(409, "FREEZE_ALREADY_OWNED", "Bạn đã có sẵn vật phẩm bảo vệ chuỗi ngày học.");`
- Trước: `throw new LearningRequestException(409, "NO_STREAK_TO_PROTECT", "There is no streak to protect");`
- Sau: `throw new LearningRequestException(409, "NO_STREAK_TO_PROTECT", "Bạn chưa có chuỗi ngày học nào để bảo vệ.");`
- Trước: `throw new LearningRequestException(409, "FREEZE_LIMIT", "Consecutive freezes are at their limit");`
- Sau: `throw new LearningRequestException(409, "FREEZE_LIMIT", "Bạn đã dùng bảo vệ chuỗi đạt giới hạn liên tiếp cho phép.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitDiagnosticProbeUseCase.java`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy phiên chẩn đoán."));`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy câu hỏi kiểm tra chẩn đoán."));`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán đã đóng.");`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán đã đóng.");`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is no longer available");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán không còn khả dụng.");`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Probe changed while it was being submitted");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán đã thay đổi trong lúc nộp.");`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy phiên chẩn đoán."));`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy câu hỏi kiểm tra chẩn đoán."));`
- Trước: `throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");`
- Sau: `throw new LearningRequestException(409, "PROBE_CLOSED", "Câu hỏi kiểm tra chẩn đoán đã đóng.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitEssayRevisionUseCase.java`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));`
- Sau: `() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));`
- Trước: `throw new LearningRequestException(409, "NOT_REVISABLE", "Only a lesson essay can be revised");`
- Sau: `throw new LearningRequestException(409, "NOT_REVISABLE", "Chỉ có thể chỉnh sửa lại bài viết thuộc bài học.");`
- Trước: `throw new LearningRequestException(409, "NOT_REVISABLE", "Only a graded essay can be revised");`
- Sau: `throw new LearningRequestException(409, "NOT_REVISABLE", "Chỉ có thể chỉnh sửa lại bài viết đã được chấm.");`
- Trước: `throw new LearningRequestException(409, "REVISION_LIMIT", "This essay has no revisions left");`
- Sau: `throw new LearningRequestException(409, "REVISION_LIMIT", "Bài viết này đã hết lượt chỉnh sửa.");`
- Trước: `"This essay has already been revised; revise the newest version");`
- Sau: `"Bài viết này đã có bản chỉnh sửa mới hơn; vui lòng sửa trên bản mới nhất.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitFreeWritingToExaminerUseCase.java`
- Trước: `"This essay was already sent to an examiner who has not graded it yet", submissionId);`
- Sau: `"Bài viết này đã được gửi đến giám khảo và đang chờ chấm.", submissionId);`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitLessonEssayUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Essay block was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy khối bài viết."));`
- Trước: `throw new LearningRequestException(409, "NOT_ESSAY_BLOCK", "This block is not an essay");`
- Sau: `throw new LearningRequestException(409, "NOT_ESSAY_BLOCK", "Khối nội dung này không phải là bài viết luận.");`
- Trước: `"This essay has already been revised; revise the newest version");`
- Sau: `"Bài viết này đã có bản chỉnh sửa mới hơn; vui lòng sửa trên bản mới nhất.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitLessonExerciseUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Exercise block was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy khối bài tập."));`
- Trước: `throw new LearningRequestException(409, "ESSAY_BLOCK", "Essay blocks are submitted as essays");`
- Sau: `throw new LearningRequestException(409, "ESSAY_BLOCK", "Khối bài viết luận cần được nộp tại mục nộp bài viết.");`
- Trước: `throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Exercise contains unsupported questions");`
- Sau: `throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Bài tập chứa dạng câu hỏi chưa được hỗ trợ.");`
- Trước: `return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `return new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitPracticeAttemptUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `"Submit every essay of the set and wait for its grade first");`
- Sau: `"Vui lòng nộp tất cả bài viết trong bộ đề và chờ chấm xong trước.");`
- Trước: `"Record your answer and wait for its grade first"));`
- Sau: `"Vui lòng ghi âm câu trả lời và chờ chấm xong trước."));`
- Trước: `"Record your answer and wait for its grade first");`
- Sau: `"Vui lòng ghi âm câu trả lời và chờ chấm xong trước.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitPracticeEssayToExaminerUseCase.java`
- Trước: `throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Choose the examiner who will grade the essay");`
- Sau: `throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Vui lòng chọn giám khảo chấm bài.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
- Trước: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");`
- Sau: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "Lượt luyện tập này đã được nộp trước đó.");`
- Trước: `throw new LearningRequestException(409, "FREE_PRACTICE_ESSAY_UNSUPPORTED", "Free practice does not take essays");`
- Sau: `throw new LearningRequestException(409, "FREE_PRACTICE_ESSAY_UNSUPPORTED", "Luyện tập tự do không hỗ trợ bài viết luận.");`
- Trước: `"This question is not an essay of the practice set"));`
- Sau: `"Câu hỏi này không phải là bài viết luận của bộ luyện tập."));`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `"This essay was already graded by the AI; ask a teacher from its result instead");`
- Sau: `"Bài viết này đã được AI chấm; vui lòng gửi yêu cầu giám khảo từ màn hình kết quả.");`
- Trước: `"This essay was already sent to an examiner who has not graded it yet", submissionId);`
- Sau: `"Bài viết này đã được gửi đến giám khảo và đang chờ chấm.", submissionId);`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitPracticeEssayUseCase.java`
- Trước: `"This question is not an essay of the practice set"));`
- Sau: `"Câu hỏi này không phải là bài viết luận của bộ luyện tập."));`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy lượt luyện tập."));`
- Trước: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");`
- Sau: `throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "Lượt luyện tập này đã được nộp trước đó.");`
- Trước: `"Free practice does not take essays");`
- Sau: `"Luyện tập tự do không hỗ trợ bài viết luận.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitReviewUseCase.java`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `throw new LearningRequestException(409, "THEORY_REQUIRED", "Read the theory before the next set");`
- Sau: `throw new LearningRequestException(409, "THEORY_REQUIRED", "Vui lòng đọc phần lý thuyết trước khi làm bộ bài tập tiếp theo.");`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bộ bài ôn tập đã đóng.");`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bộ bài ôn tập đã đóng.");`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is no longer available");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bộ bài ôn tập không còn khả dụng.");`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
- Trước: `throw new LearningRequestException(409, "THEORY_REQUIRED", "Read the theory before the next set");`
- Sau: `throw new LearningRequestException(409, "THEORY_REQUIRED", "Vui lòng đọc phần lý thuyết trước khi làm bộ bài tập tiếp theo.");`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bộ bài ôn tập đã đóng.");`
- Trước: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");`
- Sau: `throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Bộ bài ôn tập đã đóng.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài ôn tập."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitTheoryCheckUseCase.java`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài ôn tập."));`
- Trước: `throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");`
- Sau: `throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "Bài ôn tập này không ở giai đoạn kiểm tra lý thuyết.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài ôn tập."));`
- Trước: `throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");`
- Sau: `throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "Bài ôn tập này không ở giai đoạn kiểm tra lý thuyết.");`
- Trước: `throw new LearningRequestException(422, "INVALID_ANSWERS", "This review has no quick-check questions");`
- Sau: `throw new LearningRequestException(422, "INVALID_ANSWERS", "Bài ôn tập này không có câu hỏi kiểm tra nhanh.");`
- Trước: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");`
- Sau: `throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/UnlockCourseUseCase.java`
- Trước: `"Idempotency-Key is required");`
- Sau: `"Thiếu mã định danh yêu cầu.");`
- Trước: `throw new LearningRequestException(400, "INVALID_UNLOCK_TARGET", "Unsupported unlock target");`
- Sau: `throw new LearningRequestException(400, "INVALID_UNLOCK_TARGET", "Mục tiêu mở khóa không được hỗ trợ.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found"));`
- Sau: `.orElseThrow(() -> new LearningRequestException(404, "COURSE_NOT_FOUND", "Không tìm thấy khóa học."));`
### `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/UploadSpeakingRecordingUseCase.java`
- Trước: `throw new IllegalArgumentException("durationMs is out of range");`
- Sau: `throw new IllegalArgumentException("Thời lượng bản ghi âm nằm ngoài giới hạn cho phép.");`
- Trước: `"This submission no longer takes recordings", submissionId);`
- Sau: `"Bài nộp này không còn tiếp nhận tệp ghi âm mới.", submissionId);`
- Trước: `() -> new LearningRequestException(404, "SPEAKING_QUESTION_NOT_FOUND", "The question is not in this submission"));`
- Sau: `() -> new LearningRequestException(404, "SPEAKING_QUESTION_NOT_FOUND", "Câu hỏi này không thuộc bài nộp Speaking."));`
- Trước: `"The recording must be WAV, 16-bit, mono, 16 kHz");`
- Sau: `"Tệp ghi âm phải có định dạng WAV, 16-bit, mono, 16 kHz.");`
- Trước: `"This recording is being uploaded by another request", submissionId);`
- Sau: `"Bản ghi âm này đang được tải lên bởi một yêu cầu khác.", submissionId);`
- Trước: `"This submission no longer takes recordings", submissionId);`
- Sau: `"Bài nộp này không còn tiếp nhận tệp ghi âm mới.", submissionId);`
- Trước: `"This recording is being uploaded by another request", submissionId);`
- Sau: `"Bản ghi âm này đang được tải lên bởi một yêu cầu khác.", submissionId);`
- Trước: `return new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND", "Speaking submission not found");`
- Sau: `return new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp Speaking.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/aggregate/StudySession.java`
- Trước: `if (intervals == null || intervals.size() > 16) throw new IllegalArgumentException("At most 16 intervals are allowed");`
- Sau: `if (intervals == null || intervals.size() > 16) throw new IllegalArgumentException("Chỉ cho phép tối đa 16 khoảng thời gian hoạt động.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/exception/HeartsDepletedException.java`
- Trước: `super("No hearts left; wait for the next heart or refill with points");`
- Sau: `super("Bạn đã hết tim. Vui lòng chờ hồi tim hoặc dùng điểm để nạp thêm.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/exception/LearningGateException.java`
- Trước: `case "REVIEW_REQUIRED" -> "Complete the pending reviews first.";`
- Trước: `case "TOPIC_LOCKED" -> "This topic is locked.";`
- Trước: `case "LESSON_LOCKED" -> "Complete the earlier lessons first.";`
- Trước: `case "PRACTICE_REQUIRED" -> "Complete practice of the earlier lessons first.";`
- Trước: `case "PAYWALL" -> "Unlock this skill to access the lesson.";`
- Trước: `default -> throw new IllegalArgumentException("Unknown learning gate code.");`
- Sau: `case "REVIEW_REQUIRED" -> "Vui lòng hoàn thành các bài ôn tập đang chờ trước khi tiếp tục.";`
- Sau: `case "TOPIC_LOCKED" -> "Chủ đề này hiện đang bị khóa. Vui lòng hoàn thành chủ đề trước đó.";`
- Sau: `case "LESSON_LOCKED" -> "Vui lòng hoàn thành các bài học trước đó để mở khóa bài này.";`
- Sau: `case "PRACTICE_REQUIRED" -> "Vui lòng hoàn thành phần luyện tập của các bài học trước.";`
- Sau: `case "PAYWALL" -> "Vui lòng mở khóa kỹ năng này để truy cập bài học.";`
- Sau: `default -> throw new IllegalArgumentException("Mã điều kiện mở bài học không hợp lệ.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/exception/PracticeAttemptAlreadySubmittedException.java`
- Trước: `super("Practice attempt was already submitted with a different requestId");`
- Sau: `super("Lượt luyện tập này đã được nộp trước đó với một yêu cầu khác.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/service/AnswerSpecGrader.java`
- Trước: `throw new IllegalArgumentException("Answer must be a string or null.");`
- Sau: `throw new IllegalArgumentException("Câu trả lời phải là chuỗi ký tự hoặc để trống.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/domain/vo/Mp1Params.java`
- Trước: `throw new IllegalArgumentException("Invalid MP-1.0 parameters");`
- Sau: `throw new IllegalArgumentException("Tham số MP-1.0 không hợp lệ.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestEssayPromptActivationClient.java`
- Trước: `() -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));`
- Sau: `() -> new LearningRequestException(401, "UNAUTHORIZED", "Yêu cầu cần có phiên đăng nhập hợp lệ.")));`
- Trước: `return new LearningRequestException(503, "PROMPT_VERSION_UNAVAILABLE", "Essay prompt activation is unavailable");`
- Sau: `return new LearningRequestException(503, "PROMPT_VERSION_UNAVAILABLE", "Dịch vụ cấu hình đề bài viết tạm thời không khả dụng.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestHumanReviewClient.java`
- Trước: `"No human-grading credit is left");`
- Sau: `"Bạn đã hết lượt chấm bài bởi giám khảo.");`
- Trước: `"The request can be cancelled only when it is late and the examiner can no longer be changed");`
- Sau: `"Chỉ có thể hủy yêu cầu khi đã quá hạn chấm và không còn lượt đổi giám khảo.");`
- Trước: `"The chosen examiner cannot take this essay; choose another examiner");`
- Sau: `"Giám khảo đã chọn hiện không thể nhận bài viết này; vui lòng chọn giám khảo khác.");`
- Trước: `throw new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "The review request conflicts");`
- Sau: `throw new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "Yêu cầu chấm bài bị xung đột.");`
- Trước: `throw new LearningRequestException(502, "HUMAN_REVIEW_REJECTED", "The Mentor review was refused");`
- Sau: `throw new LearningRequestException(502, "HUMAN_REVIEW_REJECTED", "Yêu cầu chấm bài bởi giám khảo đã bị từ chối.");`
- Trước: `() -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));`
- Sau: `() -> new LearningRequestException(401, "UNAUTHORIZED", "Yêu cầu cần có phiên đăng nhập hợp lệ.")));`
- Trước: `return new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "Mentor review is unavailable");`
- Sau: `return new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "Dịch vụ chấm bài bởi giám khảo tạm thời không khả dụng.");`
### `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestLearningContentClient.java`
- Trước: `throw new LearningRequestException(404, "NOT_FOUND", "Learning content was not found");`
- Sau: `throw new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy nội dung học tập.");`
- Trước: `.orElseThrow(() -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));`
- Sau: `.orElseThrow(() -> new LearningRequestException(401, "UNAUTHORIZED", "Yêu cầu cần có phiên đăng nhập hợp lệ.")));`
- Trước: `return new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Learning content is unavailable");`
- Sau: `return new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Dịch vụ nội dung học tập tạm thời không khả dụng.");`
- Trước: `return new LearningRequestException(502, "CONTENT_FAILURE", "Learning content request failed");`
- Sau: `return new LearningRequestException(502, "CONTENT_FAILURE", "Không thể tải nội dung học tập lúc này.");`
