---
phase: 2
title: "Domain statistics APIs"
status: pending
effort: ""
dependencies: []
---

# Phase 2: API tiến độ và lịch sử band

## Overview

Nguồn chuẩn: [thiết kế §2.2, §2.3, §2.5 và §4](./dashboard-statistics-design.md). Có thể triển khai song song phase 1 sau khi chốt quy ước scope/availability. Hai nhóm code dưới đây có owner riêng, không sửa cùng migration/config.

Ngoài phạm vi: ôn từ vựng (plan riêng) và sửa luồng chấm bài. Assessment chỉ thêm API đọc.

## Requirements

- Tiến độ skill và course tính lesson completion; Practice/topic/course test là trạng thái riêng.
- Band history chỉ đọc kết quả đã chấm xong, version mới thay version cũ cùng attempt; placement luôn có nhãn ước lượng.
- Không triển khai Mentor, không gọi LLM mới để tạo dashboard.
- Không sửa luồng finalize/chấm bài, không đổi band/gate/event hiện có.

## Related Code Files

Base BE: `D:/Fall2026/New folder/BE/backend`; các đường dẫn dưới đây tương đối base này.

| Owner | Tệp hiện có phải xem/sửa | Tạo mới |
|---|---|---|
| Content | `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalLearningContentController.java`; `application/port/LearningContentReader.java`; `infrastructure/persistence/adapter/JdbcLearningContentReader.java` cùng package root | Batch metadata use case/result/DTO và query test |
| Learning | `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java`; `infrastructure/client/RestLearningContentClient.java`; `domain/aggregate/LessonProgress.java`; `infrastructure/persistence/JdbcLessonProgressRepository.java` cùng package root | `GetLearningProgressStatisticsUseCase`, read repository/result/DTO/controller, course activity aggregate/repository khi cần |
| Assessment | Chỉ **đọc** `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/entity/SkillScore.java`, `AssessmentResult.java` và repository hiện có; không sửa `PlacementGradingService`, `AssessmentResultCompleter`, `FinalizeAssessmentResultUseCase` | `GetBandHistoryUseCase`, query repository, controller/DTO; index additive nếu EXPLAIN cho thấy cần |

Package path rút gọn trong một ô luôn nối với root Java service ở đầu ô. Migration mới (course activity ở Learning; index nếu cần ở Assessment) tại `services/<owner>-service/src/main/resources/db/migration/`; chọn Vnext khi triển khai, không tự đoán version hôm nay.

## Architecture

- Content sở hữu metadata, trả DTO theo batch. Learning sở hữu completion và tính phần trăm bằng tập metadata đó; không đọc DB Content.
- Learning current counts là query, không phát event/tăng counter ở mọi endpoint rồi khó đối soát. Course activity ghi tại luồng bắt đầu học thành công, giữ lock per-user theo convention Learning.
- Assessment chỉ đọc: một query lấy newest COMPLETED version mỗi attempt. Không đổi AssessmentCompleted.v2, không thêm cột score/provenance.

## Implementation Steps

### A. Learning + Content

1. Xác nhận scope “course đã bắt đầu”/selected course và policy current curriculum revision. Tạo payload metadata tối thiểu: courseId, lessonId, skills, publish status/revision; không đáp án/nội dung nhạy cảm.
2. Batch SQL Content theo tập course/lesson, giới hạn request và response; nếu catalog vượt cap phải có cơ chế lấy đủ denominator theo contract trước trả %. Không cắt danh sách rồi trả 100% sai.
3. Query set-based Learning progress, ghép Map/Set; distinct lesson per course và per skill. Denominator=0 trả null. Không call repo/HTTP bên trong map từng lesson.
4. Ghi bắt đầu/course activity tại open lesson thành công; timestamp lịch sử chưa đủ chứng minh là null/provenance legacy, không dùng refresh timestamp. Không lấy telemetry client làm quyền enrollment.
5. Trả preview/course cursor + resumeTarget theo gate; không đổi endpoint course list cũ hoặc đặt lại placement bắt buộc.

### B. Assessment (chỉ đọc)

1. Đọc code và schema để xác nhận: result nào COMPLETED, trường nào đã lưu, trường nào được tính lại khi đọc (band placement tính lại từ dữ liệu/config). Ghi giới hạn này vào contract.
2. Query newest COMPLETED version mỗi attempt, lọc owner và khoảng trước. Nhóm theo `seriesKind` (loại đề). Placement là series riêng, luôn nhãn "ước lượng placement"; không vẽ Speaking; không tính Overall.
3. Overall chỉ cho loại đề có đủ 4 kỹ năng cùng attempt, đều có điểm do hệ thống chấm. Đề thiếu tiêu chuẩn thì trả unavailable kèm lý do.
4. Timeline sort submittedAt/id; pending regrade giữ điểm completed trước đó kèm `regradePending`; cursor page 100. Summary latest/baseline/delta tính trên toàn range cùng series, không chỉ page đầu.
5. Không migration đổi cột, không regrade, không sửa band/gate cũ. Thêm index chỉ khi EXPLAIN cho thấy cần. Regression: response/gate/event cũ không đổi vì chỉ thêm đường đọc.

## Success Criteria

- [ ] Completion retry không nhân đôi; multi-skill đúng; practice fail không giảm lesson completion.
- [ ] Course chưa pass topic đầu nhưng đã học vẫn hiện; denominator lấy metadata đầy đủ và có revision.
- [ ] Regrade không tăng attempts; placement luôn có nhãn ước lượng.
- [ ] Speaking thiếu không biến thành 0 và không tạo Overall giả.
- [ ] User không đọc kết quả của user khác; static paths không bị bắt bởi `/{id}`.
- [ ] Không file nào trong luồng finalize/chấm bài bị sửa.

## Validation

Unit cho công thức; MVC cho ownership/bounds; Testcontainers cho latest-result query và migration. Chạy lần lượt module bị sửa: `mvn -q -pl services/content-service -am test`, `mvn -q -pl services/learning-service -am test`, `mvn -q -pl services/assessment-service -am test`. Review SQL/EXPLAIN với dữ liệu fixture đại diện.

## Risks and rollback

Band placement hiện được tính lại khi đọc, nên đổi config có thể làm điểm hiển thị đổi theo; chấp nhận vì plan này không sửa luồng chấm, và nhãn ước lượng nói rõ điều đó. Điểm dự phòng (Writing 5.5) không phân biệt được với điểm thật trong dữ liệu; nếu cần phân biệt thì mở plan riêng. Đổi nội dung làm mẫu số thay đổi phải báo revision. Rollback API/UI mới và giữ schema additive.
