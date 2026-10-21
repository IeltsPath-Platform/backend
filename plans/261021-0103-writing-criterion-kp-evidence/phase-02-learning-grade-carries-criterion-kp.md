---
phase: 2
title: "Learning: bài chấm mang KP của từng tiêu chí"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Learning: bài chấm mang KP của từng tiêu chí

## Mục tiêu

`EssayGrader` đã có rubric trong tay lúc chấm (ngoài transaction). Nó gắn `knowledgePointId` của rubric vào từng
`WritingGrade.Criterion`, và trường này được lưu cùng JSON bài chấm. Phase 3 đọc lại từ đó, không gọi Content.

Không đổi: prompt gửi LLM (`renderCriterion` tự dựng text, không serialize record), `promptVersion`, response API cho
học viên.

## Files

| Hành động | File |
|---|---|
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java` (dòng 110-111) |
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/domain/vo/WritingGrade.java` |
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/application/service/EssayGrader.java` |
| Không đổi (chỉ kiểm tra) | `infrastructure/client/RestLearningContentClient.java` (đọc record bằng `ParameterizedTypeReference`, tự map trường mới); `infrastructure/persistence/JdbcWritingSubmissionRepository.java` (`StoredGrade` serialize cả record `Criterion`); `api/dto/response/WritingSubmissionResponse.java` (map 4 trường, không lộ KP) |
| Modify (test) | `services/learning-service/src/test/java/com/ieltspath/learning/application/service/EssayGraderTest.java` |
| Modify (test) | `services/learning-service/src/test/java/com/ieltspath/learning/api/WritingWebMvcTest.java` |

## Bước 1: `RubricCriterion` thêm `knowledgePointId`

```java
record RubricCriterion(String code, String name, String description, int sortOrder,
                       List<RubricBandDescriptor> bandDescriptors, UUID knowledgePointId) {
    /** A criterion from a Content that does not map criteria to knowledge points yet. */
    public RubricCriterion(String code, String name, String description, int sortOrder,
                           List<RubricBandDescriptor> bandDescriptors) {
        this(code, name, description, sortOrder, bandDescriptors, null);
    }
}
```

Constructor 5 tham số giữ cho các stub test hiện có (`EssayGraderTest:60`, `LessonWritingIntegrationTest:467`,
`PracticeEssayIntegrationTest:237`, `FreeWritingIntegrationTest:182`) biên dịch được mà không phải sửa. Jackson dùng
constructor canonical cho record; JSON thiếu `knowledgePointId` thì ra `null`.

## Bước 2: `WritingGrade.Criterion` thêm `knowledgePointId`

```java
/** {@code knowledgePointId} is the point this criterion is evidence for; null for grades stored before the mapping. */
public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                        UUID knowledgePointId) {
    public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {
        this(code, band, strengths, improvements, null);
    }
}
```

Thêm `import java.util.UUID;`. Tiền lệ: `Correction` đã có constructor phụ, và JSON cũ vẫn đọc được qua `StoredGrade`. JSON bài chấm cũ không có
`knowledgePointId` thì ra `null`. JSON mới có thêm trường thì vẫn đọc được nếu rollback, vì ObjectMapper của Spring bỏ
qua thuộc tính lạ.

## Bước 3: `EssayGrader` gắn KP khi parse

Trong `grade(...)`, sau khi kiểm tra rubric hợp lệ (sau dòng 75):

```java
Map<String, UUID> criterionKnowledgePoints = new HashMap<>();
for (RubricCriterion criterion : rubric.criteria()) {
    if (criterion != null && criterion.code() != null && criterion.knowledgePointId() != null) {
        criterionKnowledgePoints.putIfAbsent(criterion.code(), criterion.knowledgePointId());
    }
}
```

Truyền map này vào cả hai lần gọi `parse(...)` (dòng 83 và 86). Đổi chữ ký:

```java
private Optional<WritingGrade> parse(WritingTask task, String essay, Map<String, ErrorType> allowedCodes,
                                     Map<String, UUID> criterionKnowledgePoints, String reply)
```

Trong `parse`, dòng 165-166:

```java
byCode.put(code, new WritingGrade.Criterion(code, value.setScale(1), List.of(),
        comment.isBlank() ? List.of() : List.of(comment), criterionKnowledgePoints.get(code)));
```

Chỗ khác của `parse` giữ nguyên. Rubric không có KP thì `get` ra `null`, bài vẫn chấm bình thường.

## Bước 4: Test

1. `EssayGraderTest`:
   - Thêm test `eachCriterionCarriesTheKnowledgePointOfItsRubricCriterion`. Stub `content.getActiveRubric("TASK_2")`
     trả rubric có 4 tiêu chí TR/CC/LR/GRA, mỗi tiêu chí một UUID khác nhau (dùng constructor 6 tham số). Chấm một
     reply hợp lệ, rồi assert `grade().criteria()` theo thứ tự TR, CC, LR, GRA mang đúng UUID.
   - Thêm test `aRubricWithoutKnowledgePointsStillGrades`. Stub mặc định hiện có (constructor 5 tham số) → mọi
     `knowledgePointId` là `null`, `promptVersion` không đổi.
   - Thêm assert: system prompt (`llm.prompts.getFirst()[0]`) không chứa chuỗi UUID của KP.
2. `WritingWebMvcTest` (dòng ~95): dựng `WritingGrade.Criterion` bằng constructor 5 tham số có UUID, rồi assert
   `jsonPath("$..knowledgePointId").doesNotExist()` (hoặc `$.grade.criteria[0].knowledgePointId`, tùy đường JSON thật
   của response trong test).

Chạy: `mvn -q -pl services/learning-service -am package`. Test `MessageRetry` lỗi biên dịch khi chạy reactor `test` là lỗi
cũ, nên dùng `package`.

## Tiêu chí hoàn thành

- [ ] Bài chấm mới lưu `knowledgePointId` cho từng tiêu chí khi Content trả về; `null` khi Content chưa có.
- [ ] Prompt LLM và `promptVersion` không đổi; response API không có `knowledgePointId`.
- [ ] Test grader và test web hiện có vẫn xanh.
