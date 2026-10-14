---
phase: 2
title: "Domain statistics APIs"
status: pending
effort: ""
dependencies: []
---

# Phase 2: API tiến độ, band và ôn từ vựng

## Overview

Nguồn chuẩn: [thiết kế §2.2–2.5 và §4](./dashboard-statistics-design.md). Có thể triển khai song song phase 1 sau khi chốt quy ước scope/availability. Ba nhóm code dưới đây có owner riêng, không sửa cùng migration/config.

## Requirements

- Tiến độ skill và course tính lesson completion; Practice/topic/course test là trạng thái riêng.
- Band history chỉ dùng kết quả đủ provenance, version mới thay version cũ cùng attempt.
- Vocab có write flow thật, due scheduling và counters nhất quán; không fake số liệu.
- Không triển khai Mentor, không gọi LLM mới để tạo dashboard.

## Related Code Files

Base BE: `D:/Fall2026/New folder/BE/backend`; các đường dẫn dưới đây tương đối base này.

| Owner | Tệp hiện có phải xem/sửa | Tạo mới |
|---|---|---|
| Content | `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalLearningContentController.java`; `application/port/LearningContentReader.java`; `infrastructure/persistence/adapter/JdbcLearningContentReader.java` cùng package root | Batch metadata use case/result/DTO và query test |
| Learning | `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java`; `infrastructure/client/RestLearningContentClient.java`; `domain/aggregate/LessonProgress.java`; `infrastructure/persistence/JdbcLessonProgressRepository.java` cùng package root | `GetLearningProgressStatisticsUseCase`, read repository/result/DTO/controller, course activity aggregate/repository khi cần |
| Assessment | `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/PlacementGradingService.java`, `AssessmentResultCompleter.java`, `FinalizeAssessmentResultUseCase.java`; `domain/entity/SkillScore.java`, `AssessmentResult.java` cùng package root | `GetBandHistoryUseCase`, query repository, provenance/snapshot fields, controller/DTO, migration additive |
| Library | `services/library-service/src/main/java/com/ieltspath/library/domain/aggregate/Flashcard.java`; use cases `CreateFlashcardUseCase`, `UpdateFlashcardUseCase`, `DeleteFlashcardUseCase`; `api/controller/FlashcardController.java` cùng package root | Personal vocabulary progress + review session/event model/use cases/repositories/API/migration |

Package path rút gọn trong một ô luôn nối với root Java service ở đầu ô. Migration mới tại `services/<owner>-service/src/main/resources/db/migration/`; chọn Vnext khi triển khai, không tự đoán version hôm nay.

## Architecture

- Content sở hữu metadata, trả DTO theo batch. Learning sở hữu completion và tính phần trăm bằng tập metadata đó; không đọc DB Content.
- Learning current counts là query, không phát event/tăng counter ở mọi endpoint rồi khó đối soát. Course activity ghi tại luồng bắt đầu học thành công, giữ lock per-user theo convention Learning.
- Assessment freeze score/provenance lúc finalize trong transaction; một query lấy newest COMPLETED version mỗi attempt. Không đổi AssessmentCompleted.v2 chỉ để có dữ liệu dashboard.
- Library aggregate bảo vệ state transition theo `stateVersion`, review request idempotent, history/event trong DB cùng transaction; không cần RabbitMQ cho việc đổi bucket nội bộ.

## Implementation Steps

### A. Learning + Content

1. Xác nhận scope “course đã bắt đầu”/selected course và policy current curriculum revision. Tạo payload metadata tối thiểu: courseId, lessonId, skills, publish status/revision; không đáp án/nội dung nhạy cảm.
2. Batch SQL Content theo tập course/lesson, giới hạn request và response; nếu catalog vượt cap phải có cơ chế lấy đủ denominator theo contract trước trả %. Không cắt danh sách rồi trả 100% sai.
3. Query set-based Learning progress, ghép Map/Set; distinct lesson per course và per skill. Denominator=0 trả null. Không call repo/HTTP bên trong map từng lesson.
4. Ghi bắt đầu/course activity tại open lesson thành công; timestamp lịch sử chưa đủ chứng minh là null/provenance legacy, không dùng refresh timestamp. Không lấy telemetry client làm quyền enrollment.
5. Trả preview/course cursor + resumeTarget theo gate; không đổi endpoint course list cũ hoặc đặt lại placement bắt buộc.

### B. Assessment

1. Audit mọi producer finalize thật sự đang dùng (placement, objective/gate, HUMAN/AI), xác định trường nào persisted, trường nào tính lại. Thêm snapshot/provenance ở đúng nơi hoàn tất, không chỉ patch GET result.
2. Tách `gradingSource` và `measurementQuality`; điểm 0/5.5 không tự chứng minh fallback. Đảm bảo fallback default và unassessed Speaking được tag ngay lúc chấm.
3. Eligibility theo loại đề/rubric/skill coverage; kết quả cũ thiếu chứng cứ là UNKNOWN_LEGACY. Không dùng điểm ngắn hoặc overall của gate để giả full IELTS band.
4. Timeline newest COMPLETED/attempt, sort submittedAt/id; pending regrade giữ previous completed có nhãn; cursor page 100. Summary latest/baseline/delta lấy trên toàn range cùng series, không chỉ page đầu.
5. Không regrade hàng loạt, không sửa band/gate cũ để “làm đẹp” dashboard. Thêm migration và contract mới, regression test đảm bảo response/gate/event cũ vẫn đúng.

### C. Library

1. Duyệt countingUnit=VOCABULARY_SENSE, lịch ôn, cách recall objective/self-rated. Enum/method domain cho NEW → L1 → L2 → L3 → RETAINED và quên → L1.
2. Khóa duy nhất user+sense, quản lý add/archive/reactivate; nhiều deck không tạo duplicate. Lưu lịch sử khi archive, không hard delete state như side effect.
3. Start review queue giới hạn, kiểm ownership, phiên hết hạn, item stateVersion. Prompt/feedback theo phase; không expose correct answer trước submit nếu objective.
4. Submit requestId + expectedVersion trong transaction: replay trả kết quả cũ; mismatch trả conflict; thay state, dueAt, event atomic. Luyện trước hạn không thăng cấp.
5. Summary GROUP BY states và due predicate; tổng bucket=total, retainedPercent cùng mẫu số. Kết nối “Ôn ngay” FE là công việc bắt buộc để card có dữ liệu thật, không hẹn vô thời hạn ngoài plan.

## Success Criteria

- [ ] Completion retry không nhân đôi; multi-skill đúng; practice fail không giảm lesson completion.
- [ ] Course chưa pass topic đầu nhưng đã học vẫn hiện; denominator lấy metadata đầy đủ và có revision.
- [ ] Regrade không tăng attempts; fallback/unassessed không xuất hiện như measured.
- [ ] Speaking thiếu không biến thành 0 và không tạo Overall đủ 4 kỹ năng giả.
- [ ] Vocab 201 total/110 retained trả 54.7% theo precision policy; due là tập riêng.
- [ ] Hai review đồng thời không vượt một level; replay trả đúng snapshot ban đầu.
- [ ] User không đọc/ghi kết quả/session của user khác; static paths không bị bắt bởi `/{id}`.
- [ ] Đủ producer và luồng ghi trước khi bật card; không seed mock để pass.

## Validation

Unit cho công thức/eligibility/state; MVC cho ownership/bounds; Testcontainers cho latest-result query, unique/locking, aggregate và migration. Chạy lần lượt module bị sửa: `mvn -q -pl services/content-service -am test`, `mvn -q -pl services/learning-service -am test`, `mvn -q -pl services/assessment-service -am test`, `mvn -q -pl services/library-service -am test`. LLM giả/stub, không gọi dịch vụ chấm thật trong test. Review SQL/EXPLAIN với dữ liệu fixture đại diện.

## Risks and rollback

Đề thi hiện tại có thể chưa đủ rubric/Speaking để Overall có điểm; hiển thị unavailable là đúng. Không coi enum MOCK tồn tại là toàn bộ mock-test flow đã có. Đổi nội dung làm mẫu số thay đổi phải báo revision. Rollback API/UI mới và giữ schema additive, không đổi scoring/gate cũ.
