---
phase: 2
title: "Learning: grader gắn KP vào điểm tiêu chí"
status: pending
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Learning: grader gắn KP vào điểm tiêu chí

## Overview

`EssayGrader` đã có rubric trong tay lúc chấm (ngoài transaction). Cho nó gắn `knowledgePointId` vào từng điểm tiêu chí và lưu cùng bài chấm, để bước ghi bằng chứng sau này không cần gọi Content bên trong transaction.

## Requirements

- Không đổi prompt, rubric, mẫu prompt hay `promptVersion` (`rubricCode@version+tplv1`).
- Không lộ `knowledgePointId` cho học viên: response API (`WritingSubmissionResponse`, `EssayGrade` phía frontend) giữ nguyên.
- Bài đã lưu trước đây (JSON không có trường này) vẫn đọc được, ra `null`.

## Architecture

```
Content rubric (criteria[].knowledgePointId)
   -> LearningContentClient.RubricCriterion.knowledgePointId
   -> EssayGrader.parse(): Criterion(code, band, ..., knowledgePointId)
   -> WritingSubmission.grade (JSON StoredGrade) -> finish()/Practice dùng lại
```

## Related Code Files

- Modify: `services/learning-service/.../application/port/LearningContentClient.java` (`RubricCriterion` thêm `UUID knowledgePointId`)
- Modify: lớp client HTTP triển khai `LearningContentClient` ở `infrastructure/client` (map trường mới, tolerant khi thiếu)
- Modify: `.../domain/vo/WritingGrade.java` (`Criterion` thêm `UUID knowledgePointId`, nullable; constructor tiện lợi 4 tham số cũ để giữ test/chỗ gọi cũ)
- Modify: `.../application/service/EssayGrader.java` (`parse` tra KP theo mã tiêu chí từ rubric đang chấm)
- Modify: `.../infrastructure/persistence/JdbcWritingSubmissionRepository.java` (kiểm tra `StoredGrade` đọc/ghi trường mới; JSON cũ thiếu trường -> `null`)
- Kiểm tra: `.../api/dto/response/WritingSubmissionResponse.java` và assembler view không đưa KP ra ngoài

## Implementation Steps

1. Thêm trường vào `RubricCriterion` và map từ JSON của Content (thiếu trường -> `null`).
2. Thêm `knowledgePointId` vào `WritingGrade.Criterion`; giữ constructor 4 tham số cũ chuyển sang `null`.
3. Trong `EssayGrader.parse`, dựng `Map<code, kpId>` từ `rubric.criteria()` và truyền vào `Criterion` mới.
4. Rà `JdbcWritingSubmissionRepository`: ObjectMapper phải chấp nhận JSON cũ không có trường (record thiếu thuộc tính ra `null`); thêm test đọc JSON cũ.
5. Rà các chỗ dựng `WritingGrade.Criterion` (test, repository, view assembler) và sửa cho khớp.

## Success Criteria

- [ ] `EssayGraderTest`: điểm tiêu chí mang đúng `knowledgePointId` theo mã; rubric không có KP -> `null`, bài vẫn chấm được.
- [ ] Đọc bài nộp lưu trước nâng cấp không lỗi, `knowledgePointId` là `null`.
- [ ] Response API của bài nộp không đổi (test so sánh JSON).
- [ ] `OpenAiCompatibleLlmClientTest` và các test grader hiện có vẫn xanh.

## Risk Assessment

- Đổi record `Criterion` làm vỡ nhiều chỗ dựng: giảm bằng constructor tương thích và rà bằng biên dịch.
- Content chưa chạy V69 (khởi động lệch thứ tự): `knowledgePointId = null`, Learning rơi về hành vi cũ ở phase 3 (không lỗi).
- Rollback: bỏ trường mới ở `Criterion`; JSON đã lưu có thêm trường sẽ bị ObjectMapper bỏ qua nếu cấu hình bỏ thuộc tính lạ, kiểm tra cấu hình này trước khi deploy.
