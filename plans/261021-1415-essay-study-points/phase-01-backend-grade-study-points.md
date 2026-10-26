---
phase: 1
title: "Backend: bài chấm mang studyPoints"
status: completed
priority: P1
effort: "1d"
dependencies: []
repo: backend
---

# Phase 1: Backend: bài chấm mang `studyPoints`

Repo: `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\backend`, service `learning-service`. Đọc `plan.md` (hợp đồng JSON, quyết định S1-S10) trước.

## Mục tiêu

1. `EssayGrader` đọc `focus` của LLM thành mục `AI`.
2. `EssayGrader` sinh mục `CRITERION` cho tiêu chí dưới `passBand` có KP, kèm tên và mô tả KP lấy từ Content.
3. `WritingGrade` mang `studyPoints`, được lưu trong JSON bài chấm và đọc lại (bài cũ → `[]`).
4. `WritingSubmissionResponse` trả `studyPoints`. `criteria[]` và các trường còn lại không đổi.

## Files

| Hành động | File (đường dẫn dưới `services/learning-service/src/`) |
|---|---|
| Modify | `main/java/com/ieltspath/learning/domain/vo/WritingGrade.java` |
| Modify | `main/java/com/ieltspath/learning/application/service/EssayGrader.java` |
| Modify | `main/java/com/ieltspath/learning/infrastructure/persistence/JdbcWritingSubmissionRepository.java` (`StoredGrade`, chỗ ghi và chỗ đọc) |
| Modify | `main/java/com/ieltspath/learning/api/dto/response/WritingSubmissionResponse.java` |
| Modify (test) | `test/java/com/ieltspath/learning/application/service/EssayGraderTest.java` |
| Modify (test) | `test/java/com/ieltspath/learning/domain/vo/WritingGradeJsonTest.java` |
| Modify (test) | `test/java/com/ieltspath/learning/api/WritingWebMvcTest.java` |
| Modify (test) | `test/java/com/ieltspath/learning/infrastructure/persistence/LessonWritingIntegrationTest.java` |
| Không đổi | `essay-prompt-template-v1.txt`, `promptVersion`, `WritingEvidenceRule`, `SubmitLessonEssayUseCase`, `SubmitPracticeAttemptUseCase`, `WritingSubmissionViewAssembler` |

Trước khi sửa, chạy `rg -n "new WritingGrade\(|StoredGrade|new WritingSubmissionResponse\(" services/learning-service/src` để biết hết chỗ dựng.

## Bước 1: `WritingGrade` thêm `studyPoints`

Thêm thành phần cuối `List<StudyPoint> studyPoints`; compact constructor đổi null thành rỗng; giữ constructor 4 tham số cũ
(test và `EssayGrader` hiện có không phải sửa chỗ dựng). Thêm record lồng `StudyPoint`.

```java
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand, List<StudyPoint> studyPoints) {
    public WritingGrade {
        studyPoints = studyPoints == null ? List.of() : List.copyOf(studyPoints);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand) {
        this(criteria, corrections, summary, overallBand, List.of());
    }

    // Criterion và Correction giữ nguyên

    /**
     * One thing the learner should study. A {@code CRITERION} point names a rubric criterion below the pass band and
     * its knowledge point; an {@code AI} point is a focus the grader's model suggested.
     */
    public record StudyPoint(String kind, String criterionCode, BigDecimal band, UUID knowledgePointId, String code,
                             String name, String title, String reason) {
        public static final String CRITERION = "CRITERION";
        public static final String AI = "AI";
    }
}
```

## Bước 2: `EssayGrader`

Thêm hằng (cạnh `MAX_ITEM_CHARS`): `static final int MAX_FOCUS_POINTS = 4;` và `static final int MAX_FOCUS_TITLE_CHARS = 80;`.

### 2a. Đọc `focus` trong `parse`

Cuối `parse`, dựng `WritingGrade` có `studyPoints` là các mục `AI`:

```java
return Optional.of(new WritingGrade(criteria, corrections(root.path("corrections"), essay, allowedCodes), summary,
        WritingScore.overallBand(criteria.stream().map(WritingGrade.Criterion::band).toList()),
        focusPoints(root.path("focus"))));
```

```java
/** Focus entries are "<what to study>: <why>"; anything that is not a non-blank string is dropped. */
private static List<WritingGrade.StudyPoint> focusPoints(JsonNode node) {
    if (!node.isArray()) return List.of();
    List<WritingGrade.StudyPoint> points = new ArrayList<>();
    for (JsonNode item : node) {
        if (points.size() == MAX_FOCUS_POINTS) break;
        if (!item.isTextual()) continue;
        String text = truncate(item.asText().trim(), MAX_ITEM_CHARS);
        int colon = text.indexOf(':');
        String title = (colon < 0 ? text : text.substring(0, colon)).trim();
        String reason = colon < 0 ? "" : text.substring(colon + 1).trim();
        if (title.isBlank()) continue;
        points.add(new WritingGrade.StudyPoint(WritingGrade.StudyPoint.AI, null, null, null, null, null,
                truncate(title, MAX_FOCUS_TITLE_CHARS), reason.isBlank() ? null : reason));
    }
    return points;
}
```

### 2b. Sinh mục `CRITERION` trong `grade`

Trong `grade(EssayPrompt prompt, String essay)`, đổi dòng `return new GradedEssay(grade, promptVersion);` thành
`return new GradedEssay(withCriterionStudyPoints(grade, rubric, prompt.passBand()), promptVersion);`. Đây vẫn là ngoài transaction.

```java
/** Criteria below the pass band that have a knowledge point come first, weakest first, then the model's own focus. */
private WritingGrade withCriterionStudyPoints(WritingGrade grade, Rubric rubric, BigDecimal passBand) {
    if (passBand == null) return grade;
    Map<String, Integer> order = new HashMap<>();
    for (RubricCriterion criterion : rubric.criteria()) {
        if (criterion != null && criterion.code() != null) order.putIfAbsent(criterion.code(), criterion.sortOrder());
    }
    List<WritingGrade.Criterion> weak = grade.criteria().stream()
            .filter(item -> item.knowledgePointId() != null && item.band().compareTo(passBand) < 0)
            .sorted(Comparator.comparing(WritingGrade.Criterion::band)
                    .thenComparingInt(item -> order.getOrDefault(item.code(), Integer.MAX_VALUE)))
            .toList();
    if (weak.isEmpty()) return grade;
    Map<UUID, LearningContentClient.KnowledgePoint> details = knowledgePointDetails(
            weak.stream().map(WritingGrade.Criterion::knowledgePointId).collect(Collectors.toSet()));
    List<WritingGrade.StudyPoint> points = new ArrayList<>();
    for (WritingGrade.Criterion criterion : weak) {
        LearningContentClient.KnowledgePoint point = details.get(criterion.knowledgePointId());
        String reason = point == null ? null : firstText(point.canDo(), point.description());
        points.add(new WritingGrade.StudyPoint(WritingGrade.StudyPoint.CRITERION, criterion.code(), criterion.band(),
                criterion.knowledgePointId(), point == null ? null : point.code(), point == null ? null : point.name(),
                point == null ? null : point.name(), reason == null ? null : truncate(reason, MAX_ITEM_CHARS)));
    }
    points.addAll(grade.studyPoints());
    return new WritingGrade(grade.criteria(), grade.corrections(), grade.summary(), grade.overallBand(), points);
}

/** Names come from Content's topic sequence; when Content cannot answer the essay is still graded without them. */
private Map<UUID, LearningContentClient.KnowledgePoint> knowledgePointDetails(Set<UUID> ids) {
    try {
        Map<UUID, LearningContentClient.KnowledgePoint> found = new HashMap<>();
        for (LearningContentClient.Topic topic : content.getTopicSequence()) {
            if (topic.knowledgePoints() == null) continue;
            for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                if (ids.contains(point.id())) found.putIfAbsent(point.id(), point);
            }
        }
        return found;
    } catch (LearningRequestException exception) {
        return Map.of();
    }
}

private static String firstText(String first, String second) {
    if (first != null && !first.isBlank()) return first.trim();
    return second == null || second.isBlank() ? null : second.trim();
}
```

Ghi chú:
- `grade.studyPoints()` lúc này chỉ có mục `AI` (từ `parse`), nên `points.addAll(...)` cho ra thứ tự `CRITERION` rồi `AI` (S7).
- `knowledgePointId` của tiêu chí đến từ `parse` (đã làm ở phần trước). Rubric không có KP → không có mục `CRITERION`.
- Không log nội dung bài viết, nhận xét hay `focus`; không thêm log mới.

## Bước 3: Lưu và đọc (`JdbcWritingSubmissionRepository`)

`StoredGrade` thêm trường cuối `List<WritingGrade.StudyPoint> studyPoints`:

```java
private record StoredGrade(List<WritingGrade.Criterion> criteria, List<WritingGrade.Correction> corrections,
                           String summary, List<WritingGrade.StudyPoint> studyPoints) {}
```

- Chỗ ghi: `write(new StoredGrade(grade.criteria(), grade.corrections(), grade.summary(), grade.studyPoints()))`.
- Chỗ đọc: `new WritingGrade(stored.criteria(), stored.corrections(), stored.summary(), band, stored.studyPoints())`.
  JSON cũ thiếu trường thì `stored.studyPoints()` là null và compact constructor của `WritingGrade` đổi thành `[]`.

## Bước 4: Response (`WritingSubmissionResponse`)

Thêm `List<StudyPoint> studyPoints` ngay sau `corrections` và record lồng `StudyPoint` (có `@JsonInclude(NON_NULL)` để trường null bị bỏ):

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudyPoint(String kind, String criterionCode, BigDecimal band, UUID knowledgePointId, String code,
                         String name, String title, String reason) {}
```

Trong `from`: `grade == null ? null : grade.studyPoints().stream().map(point -> new StudyPoint(point.kind(),
point.criterionCode(), point.band(), point.knowledgePointId(), point.code(), point.name(), point.title(),
point.reason())).toList()`. `Criterion` của response giữ 4 trường (không có `knowledgePointId`).

## Bước 5: Test

### 5a. `EssayGraderTest` (đơn vị, Mockito `content`)

Thêm helper tạo reply có `focus` (giống `reply(...)` hiện có nhưng `"focus":%s`), và helper rubric có KP
(`new LearningContentClient.RubricCriterion(code, code, "d", order, List.of(descriptor), kpId)`, `sortOrder` TR=1, CC=2, LR=3, GRA=4).
Stub Content: `when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(UUID.randomUUID(), "WRITING",
"Writing", 900, null, List.of(new LearningContentClient.KnowledgePoint(kpCc, "DEMO_WRITING_CRITERION_CC", "Mạch lạc và liên kết (CC)",
"PROCEDURE", "WRITING", "Nối câu bằng từ nối đúng chức năng.", true), ...), LearningSkill.WRITING, true)))`.
`task2()` có `passBand` 6.0. Test đề xuất (đặt tên đúng như dưới đây):

| Test | Cấu hình | Kỳ vọng |
|---|---|---|
| `focusEntriesBecomeAiStudyPointsSplitAtTheFirstColon` | `focus = ["Cohesive Devices: Dùng từ nối: tự nhiên", "Counter-argument"]` | 2 mục `AI`: (title `Cohesive Devices`, reason `Dùng từ nối: tự nhiên`) và (title `Counter-argument`, reason null) |
| `blankNonTextualAndExtraFocusEntriesAreDropped` | `focus = ["", "  ", 5, null, ": chỉ có lý do", "A: 1", "B: 2", "C: 3", "D: 4", "E: 5"]` | đúng 4 mục `AI` theo thứ tự A, B, C, D; không có mục từ phần tử rỗng, số, null, hoặc không có tiêu đề |
| `aTooLongFocusTitleIsTruncated` | tiêu đề 200 ký tự | `title.length() <= 80` |
| `weakCriteriaBecomeCriterionStudyPointsWeakestFirstWithTheKnowledgePointDetails` | bands TR 5.0, CC 5.5, LR 6.0, GRA 6.5; cả 4 có KP; `passBand` 6.0 | 2 mục `CRITERION`: TR rồi CC (band tăng dần); `name`, `title`, `code` lấy từ KP; `reason` = `canDo` nếu có, nếu không là `description` |
| `criteriaAtOrAboveThePassBandOrWithoutAKnowledgePointAddNoCriterionPoint` | LR 6.0 (bằng ngưỡng), TR 4.0 nhưng không có KP | không có mục `CRITERION` nào cho LR; không có mục cho TR |
| `aPromptWithoutAPassBandAddsNoCriterionPoints` | `passBand` null | không có mục `CRITERION` (mục `AI` vẫn có nếu có `focus`) |
| `criterionPointsComeBeforeAiPoints` | một tiêu chí yếu có KP + một `focus` | `kind` theo thứ tự `CRITERION`, `AI` |
| `unavailableContentStillGradesAndLeavesTheKnowledgePointNameEmpty` | `getTopicSequence()` ném `LearningRequestException(503, "CONTENT_UNAVAILABLE", "x")` | chấm xong bình thường; mục `CRITERION` vẫn có `knowledgePointId`, `criterionCode`, `band`; `name`, `title`, `code`, `reason` đều null |
| `gradeWithoutFocusHasNoAiPoints` | reply cũ với `"focus":[]` hoặc không có trường | `studyPoints` rỗng khi không có tiêu chí yếu có KP |

Các test hiện có phải giữ xanh không phải sửa (reply cũ có `"focus":[]`).

### 5b. `WritingGradeJsonTest`

- `studyPointsSurviveStorage`: serialize/deserialize một `WritingGrade` có 2 `StudyPoint` (một `CRITERION` đủ trường, một `AI`), assert bằng nhau.
- `aGradeStoredWithoutStudyPointsReadsAsEmpty`: JSON `{"criteria":[],"corrections":[],"summary":"x","overallBand":6.0}` đọc ra `studyPoints()` rỗng, không null.

### 5c. `WritingWebMvcTest`

- Sửa `gradedEssayHasTheContractKeysAndNothingServerSide`: tập khóa mong đợi thêm `"studyPoints"`; vẫn assert khóa của `criteria[0]` chỉ gồm `code, band, strengths, improvements`.
- Thêm `studyPointsAreExposedWithOnlyTheirOwnFields`: dựng `WritingGrade` có một `CRITERION` (đủ trường) và một `AI` (chỉ `kind`, `title`, `reason`); assert khóa của `studyPoints[0]` đúng bằng `kind, criterionCode, band, knowledgePointId, code, name, title, reason` và khóa của `studyPoints[1]` đúng bằng `kind, title, reason` (trường null bị bỏ).

### 5d. `LessonWritingIntegrationTest` (Testcontainers)

Thêm `aGradedEssayCarriesStudyPointsFromWeakCriteriaAndTheModelFocus`:
- `content.getActiveRubric(anyString())` trả `rubricWithCriterionPoints("TASK_2")` (helper đã có trong file).
- `content.getTopicSequence()` trả topic hiện có cộng hai KP `CC_KP` ("Mạch lạc và liên kết (CC)", mô tả "Nối câu...") và `GRA_KP` ("Ngữ pháp đa dạng và chính xác (GRA)", mô tả "Kết hợp câu đơn và câu phức...").
- LLM trả bands TR 7.0, CC 5.5, LR 6.5, GRA 5.0 (cần helper mới `gradeWithFocus(...)`: như `gradeWithBands` nhưng thêm `"focus":["Cohesive Devices: Dùng từ nối tự nhiên"]`); `passBand` 6.0 (khối essay trong `seed`).
- Sau `submitLessonEssayUseCase.execute(...)`: `result.grade().studyPoints()` gồm theo thứ tự: `CRITERION` GRA (5.0), `CRITERION` CC (5.5), `AI` "Cohesive Devices". `name` của hai mục đầu khớp tên KP đã stub.
- Đọc lại bằng `getWritingSubmissionUseCase.execute(USER, result.submissionId())`: `grade().studyPoints()` bằng nhau (chứng minh lưu và đọc).
- Ghi đè JSON để mô phỏng bài cũ: `UPDATE lesson_writing_submissions SET result = '<JSON criteria/corrections/summary, KHÔNG có studyPoints>' WHERE id = ?` rồi đọc lại: không lỗi, `studyPoints()` rỗng.

Test hiện có trong file phải giữ xanh (rubric mặc định `testRubric` không có KP → không sinh mục `CRITERION`).

## Chạy

```powershell
mvn -q -pl services/learning-service -am package
```

Kỳ vọng: xanh, trừ 2 test lỗi sẵn có đã nêu ở `plan.md`. Ghi số test chạy/lỗi/bỏ qua vào báo cáo.

## Tiêu chí hoàn thành

- [x] `focus` hợp lệ thành mục `AI` (tối đa 4, cắt tiêu đề 80); phần tử không hợp lệ bị bỏ.
- [x] Tiêu chí `band < passBand` có KP thành mục `CRITERION`, yếu nhất trước, trước các mục `AI`; bằng ngưỡng không thành mục.
- [x] Content lỗi: vẫn chấm xong, thiếu `name`/`reason`.
- [x] Lưu/đọc `studyPoints` qua JSON; bài cũ đọc ra `[]`.
- [x] Response có `studyPoints`; `criteria[]` không có `knowledgePointId`; trường null bị bỏ.
- [x] Prompt, `promptVersion`, band, phí, mastery không đổi.

## Rủi ro và hoàn tác

- Thêm thành phần vào record `WritingGrade`: giữ constructor 4 tham số để không vỡ chỗ dựng cũ; dùng biên dịch để rà.
- JSON đã lưu có thêm `studyPoints` vẫn đọc được nếu rollback code vì ObjectMapper của Spring bỏ qua thuộc tính lạ (đã dùng cách này ở lần thêm `knowledgePointId`).
- Hoàn tác: bỏ `withCriterionStudyPoints` và `focusPoints` thì `studyPoints` luôn rỗng, giao diện tự ẩn mục này.

## Đồng bộ trạng thái — 2026-10-21

Đã đối chiếu grader, JSON lưu trữ, response và artifact test hiện có: EssayGraderTest 25, WritingGradeJsonTest 4, WritingWebMvcTest 5, LessonWritingIntegrationTest 16 test đều qua, không bỏ qua. Không chạy lại Maven trong lần này. Chi tiết: [báo cáo đồng bộ](./reports/pm-2026-10-21-writing-plan-status.md).
