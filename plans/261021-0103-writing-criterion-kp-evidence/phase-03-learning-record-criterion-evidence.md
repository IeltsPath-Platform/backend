---
phase: 3
title: "Learning: ghi bằng chứng tiêu chí và mã lỗi cho essay lesson và Practice"
status: completed
priority: P1
effort: "1d"
dependencies: [2]
---

# Phase 3: Learning: ghi bằng chứng tiêu chí và mã lỗi cho essay lesson và Practice

## Mục tiêu

Một domain rule thuần (`WritingEvidenceRule`) biến một bài chấm thành:
- kết quả đúng/sai theo từng KP: KP của đề nhận `passed`, KP tiêu chí nhận `band >= passBand`;
- mã lỗi chính của bài.

Hai đường ghi dùng chung rule này: essay lesson và essay Practice.

Giữ nguyên:
- bài nào được tính: `firstAttempt` ở lesson, `counted` ở Practice;
- khóa idempotent: `forWriting(submissionId, kpId)` và `forPracticeSet(requestId, questionVersionId, kpId)` đều đã
  chứa `kpId`, nên mỗi bài có nhiều dòng (một dòng mỗi KP) mà không va khóa
  `UNIQUE (user_id, source, source_reference_id)`;
- `PracticeReviewRule.ItemOutcome` và `reviewNeeds`;
- XP, tim, nhiệm vụ;
- mỗi essay vẫn đúng 1 event.

## Files

| Hành động | File |
|---|---|
| Create | `services/learning-service/src/main/java/com/ieltspath/learning/domain/service/WritingEvidenceRule.java` |
| Create (test) | `services/learning-service/src/test/java/com/ieltspath/learning/domain/service/WritingEvidenceRuleTest.java` |
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitLessonEssayUseCase.java` (`finish`, dòng 164-194) |
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitPracticeAttemptUseCase.java` (`execute` dòng 116-141 và 187-189; `gradedEssays` dòng 200-214) |
| Modify | `services/learning-service/src/main/java/com/ieltspath/learning/application/service/MasteryProfileUpdater.java` (`recordEvidence`, dòng 76-83) |
| Modify (test) | `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/LessonWritingIntegrationTest.java` |
| Modify (test) | `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/PracticeEssayIntegrationTest.java` |
| Không đổi | `ItemGrading`, `EssayGrader`, `Mp1MasteryCalculator`, `LessonEvidenceReference`, `JdbcKnowledgeEvidenceRepository`, `JdbcQuestionEventLog`, `FreePracticeSubmission`, `SubmitFreeWritingUseCase` |

## Bước 1: `WritingEvidenceRule` (viết test trước)

```java
package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.math.BigDecimal;
import java.util.*;

/**
 * Turns one graded essay into mastery outcomes. The question's knowledge points take the essay's pass; each rubric
 * criterion that names a knowledge point is evidence for it on its own, right when its band reaches the pass band.
 * A point that is both takes the criterion's result, and two criteria on one point must both pass.
 */
public final class WritingEvidenceRule {
    /** {@code correctByKnowledgePoint} keeps the question's points first, in order; {@code criterionKnowledgePoints}
     * are the points that took a criterion's result. */
    public record Outcome(Map<UUID, Boolean> correctByKnowledgePoint, Set<UUID> criterionKnowledgePoints,
                          String dominantErrorCode) {
        public Outcome {
            correctByKnowledgePoint = Collections.unmodifiableMap(new LinkedHashMap<>(correctByKnowledgePoint));
            criterionKnowledgePoints = Set.copyOf(criterionKnowledgePoints);
        }
    }

    public Outcome derive(Collection<UUID> questionKnowledgePoints, BigDecimal passBand, boolean passed,
                          WritingGrade grade) {
        Map<UUID, Boolean> correct = new LinkedHashMap<>();
        for (UUID kp : questionKnowledgePoints) if (kp != null) correct.putIfAbsent(kp, passed);
        Set<UUID> fromCriteria = new LinkedHashSet<>();
        if (passBand != null && grade != null && grade.criteria() != null) {
            for (WritingGrade.Criterion criterion : grade.criteria()) {
                if (criterion == null || criterion.knowledgePointId() == null || criterion.band() == null) continue;
                boolean reached = criterion.band().compareTo(passBand) >= 0;
                UUID kp = criterion.knowledgePointId();
                if (fromCriteria.add(kp)) correct.put(kp, reached);
                else correct.put(kp, correct.get(kp) && reached);
            }
        }
        return new Outcome(correct, fromCriteria, dominantErrorCode(grade));
    }

    /** The most frequent error code of the corrections; a tie goes to the alphabetically first code. */
    static String dominantErrorCode(WritingGrade grade) {
        if (grade == null || grade.corrections() == null) return null;
        Map<String, Integer> counts = new TreeMap<>();
        for (WritingGrade.Correction correction : grade.corrections()) {
            if (correction == null || correction.errorCode() == null || correction.errorCode().isBlank()) continue;
            counts.merge(correction.errorCode(), 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) { best = entry.getKey(); bestCount = entry.getValue(); }
        }
        return best;
    }
}
```

Lưu ý: `correct.put(kp, reached)` ở lần đầu một KP được tiêu chí gán là cố ý: nó ghi đè `passed` khi KP của đề trùng KP
tiêu chí (D4). `LinkedHashMap.put` trên khóa đã có giữ nguyên vị trí của khóa.

`WritingEvidenceRuleTest` (JUnit 5 thuần), mỗi case một test:
1. Task 2, `passBand` 6.0, TR 6.5 / CC 5.5 / LR 6.0 / GRA 5.0, `passed = true`, 1 KP đề: kết quả là KP đề true,
   TR true, CC false, LR true (đúng ngưỡng 6.0 là đạt), GRA false. Thứ tự key: KP đề trước, rồi TR, CC, LR, GRA.
2. `passed = false` nhưng mọi tiêu chí >= ngưỡng: KP đề false, KP tiêu chí true.
3. KP đề trùng KP của CC: kết quả lấy theo CC, không theo `passed`.
4. Hai tiêu chí cùng một KP, một đạt một không: KP đó false.
5. `knowledgePointId == null` ở mọi tiêu chí (bài cũ): chỉ có KP đề với `passed`, `criterionKnowledgePoints` rỗng.
6. `passBand == null`: bỏ qua tiêu chí, chỉ có KP đề.
7. `grade == null`: chỉ có KP đề, `dominantErrorCode` null.
8. Mã lỗi: `[GRAMMAR, VOCAB, GRAMMAR]` → `GRAMMAR`; hòa `[VOCAB, GRAMMAR]` → `GRAMMAR`; có null/rỗng thì bỏ qua;
   không có `corrections` → null.
9. KP đề bị lặp trong danh sách: chỉ một key.

## Bước 2: `MasteryProfileUpdater` cho phép bỏ XP mốc theo KP

```java
/** Call right after {@code evidence.append}; the streak moves by the newest outcome of each point. */
public void recordEvidence(UUID userId, List<KnowledgeEvidence> appended) {
    recordEvidence(userId, appended, Set.of());
}

/** As above; points in {@code withoutMilestone} move their profile but earn no milestone XP (essay criteria). */
public void recordEvidence(UUID userId, List<KnowledgeEvidence> appended, Set<UUID> withoutMilestone) {
    Set<UUID> touched = appended.stream().map(KnowledgeEvidence::knowledgePointId).collect(Collectors.toSet());
    // Milestone XP is paid only for practice evidence: tests and essays do not earn it.
    Set<UUID> practiced = appended.stream().filter(item -> PRACTICE_SOURCES.contains(item.source()))
            .map(KnowledgeEvidence::knowledgePointId).filter(kp -> !withoutMilestone.contains(kp))
            .collect(Collectors.toSet());
    update(userId, touched, Mode.RECORD, practiced);
}
```

## Bước 3: Essay lesson (`SubmitLessonEssayUseCase.finish`)

Thêm field `private final WritingEvidenceRule writingEvidence = new WritingEvidenceRule();`, theo cùng cách
`SubmitPracticeAttemptUseCase` giữ `PracticeReviewRule`. Thay dòng 168-190:

```java
// Every essay before the block is first passed is new writing, so each one is evidence; none after.
boolean firstAttempt = !essays.blockPassed(submission.userId(), submission.blockId(), submission.id());
boolean passed = Boolean.TRUE.equals(submission.passed());
List<UUID> questionKps = List.copyOf(new LinkedHashSet<>(submission.prompt().knowledgePointIds()));
var outcome = writingEvidence.derive(questionKps, submission.prompt().passBand(), passed, submission.grade());
// Each essay is one event for its prompt; a repeat after the block passed is logged with first_submission=false.
questionEvents.append(List.of(QuestionAttemptEvent.of(submission.userId(), submission.prompt().questionVersionId(),
        questionKps, passed, null, outcome.dominantErrorCode(), QuestionEventSource.LESSON_WRITING,
        submission.id(), firstAttempt)));
if (firstAttempt) {
    List<UUID> kpIds = new ArrayList<>(outcome.correctByKnowledgePoint().keySet());
    Set<UUID> known = access.knownKnowledgePoints(submission.userId(), kpIds);
    List<KnowledgeEvidence> essayEvidence = new ArrayList<>();
    for (UUID kpId : kpIds) {
        if (known.contains(kpId)) {
            essayEvidence.add(KnowledgeEvidence.of(kpId, outcome.correctByKnowledgePoint().get(kpId),
                    EvidenceSource.LESSON_WRITING, LessonEvidenceReference.forWriting(submission.id(), kpId)));
        } else {
            log.warn("Unknown writing evidence KP: userId={}, submissionId={}, kpId={}",
                    submission.userId(), submission.id(), kpId);
        }
    }
    evidence.append(submission.userId(), essayEvidence);
    masteryProfiles.recordEvidence(submission.userId(), essayEvidence);
}
```

Log chỉ ghi id; không log bài viết, prompt hay `corrections`. `LESSON_WRITING` không nằm trong `PRACTICE_SOURCES`, nên
không cần overload mới ở đây. Thứ tự lock và transaction giữ nguyên.

## Bước 4: Essay Practice (`SubmitPracticeAttemptUseCase`)

1. `gradedEssays` trả `Map<UUID, WritingSubmission>` (theo `questionVersionId`), giữ nguyên kiểm tra
   `ESSAY_NOT_GRADED`. Sửa javadoc: "The newest submission of each essay of the set; every essay must be GRADED first."
   Thêm import `com.ieltspath.learning.domain.aggregate.WritingSubmission`.
2. Dòng 116:
   ```java
   Map<UUID, WritingSubmission> essaysByQuestion = gradedEssays(userId, attempt, version);
   Map<UUID, Boolean> essayPassed = new HashMap<>();
   essaysByQuestion.forEach((question, essay) -> essayPassed.put(question, Boolean.TRUE.equals(essay.passed())));
   var graded = grading.grade(version, command.answers(), "Practice set", essayPassed);
   ```
3. Thêm field `private final WritingEvidenceRule writingEvidence = new WritingEvidenceRule();`, và trước vòng lặp:
   `Set<UUID> itemKps = new HashSet<>(); Set<UUID> criterionOnly = new HashSet<>();`
4. Trong vòng lặp (dòng 126-145), sau khi dựng `kps` và `outcome` (giữ nguyên `ItemOutcome(kps, grade.correct())`):
   ```java
   itemKps.addAll(kps);
   WritingSubmission essay = essaysByQuestion.get(item.questionVersionId());
   Map<UUID, Boolean> evidenceByKp = new LinkedHashMap<>();
   String errorCode = graded.errorCodes().get(index);
   if (essay != null) {
       var essayOutcome = writingEvidence.derive(kps, essay.prompt().passBand(), grade.correct(), essay.grade());
       evidenceByKp.putAll(essayOutcome.correctByKnowledgePoint());
       criterionOnly.addAll(essayOutcome.criterionKnowledgePoints());
       errorCode = essayOutcome.dominantErrorCode();
   } else {
       for (UUID kp : kps) evidenceByKp.put(kp, grade.correct());
   }
   if (counted) for (var entry : evidenceByKp.entrySet()) firstAnswers.add(KnowledgeEvidence.of(entry.getKey(),
           entry.getValue(), EvidenceSource.PRACTICE_SET,
           LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), entry.getKey()))
           .forQuestion(item.questionVersionId()));
   events.add(QuestionAttemptEvent.of(userId, item.questionVersionId(), List.copyOf(kps), grade.correct(),
           durations.get(item.questionVersionId()), errorCode,
           QuestionEventSource.PRACTICE_SET, command.requestId(), counted));
   ```
   - KP của đề lấy từ `item.knowledgePointMappings()` (`kps`, như hiện nay), không lấy từ `essay.prompt()`.
   - `grade.correct()` của item essay chính là `passed` của bài chấm mới nhất (do `ItemGrading` lấy từ `essayPassed`).
5. Sau vòng lặp: `criterionOnly.removeAll(itemKps);`. KP vừa là KP tiêu chí vừa có câu khác trong bộ vẫn được XP mốc
   như trước.
6. Dòng 189: `masteryProfiles.recordEvidence(userId, firstAnswers, criterionOnly);`
7. Không thêm bộ lọc `knownKnowledgePoints` ở Practice. Đường này hiện không lọc, và KP ngoài danh mục bị bỏ khi đọc
   mastery (`JdbcKnowledgeEvidenceRepository.java:117`, join `knowledge_point_catalog`). Sau V69, KP tiêu chí thuộc
   topic active nên sẽ có trong danh mục.
8. `reviewNeeds`, `candidates`, XP (`graded.correctQuestionIds()`), tim (`wrongObjectiveAnswers`) không đổi.

## Bước 5: Test tích hợp (Testcontainers)

Không sửa `testRubric(...)` mặc định trong hai file test (KP `null`), để các assert hiện có giữ nguyên ý nghĩa
"bài không có KP tiêu chí thì ghi như cũ". Thêm hằng `TR_KP, CC_KP, LR_KP, GRA_KP` và helper:

```java
private static LearningContentClient.Rubric rubricWithCriterionKps(String task) { /* như testRubric nhưng truyền KP ở constructor 6 tham số; TASK_2: TR/CC/LR/GRA */ }
private static String gradeWithBands(Map<String, String> bands, String... errorCodes) { /* criteria theo bands; mỗi errorCode một correction có excerpt là chuỗi con thật của ESSAY */ }
```

Mã lỗi phải có trong `content.activeErrorTypes()` của test. Hiện stub chỉ có `GRAMMAR`; thêm `VOCABULARY` vào stub
nếu cần hai mã.

`LessonWritingIntegrationTest`, test mới `aGradedEssayRecordsEvidenceForEachCriterionKnowledgePoint`:
- `when(content.getActiveRubric("TASK_2")).thenReturn(rubricWithCriterionKps("TASK_2"))`.
- `content.getTopicSequence()` thêm `kp(TR_KP), kp(CC_KP), kp(LR_KP)`, KHÔNG thêm `GRA_KP`, rồi gọi lại
  `topics.execute(USER)`.
- Essay `passBand` 6.0 (từ `seed`). Bands TR 7.0, CC 5.5, LR 6.5, GRA 6.0, kèm 2 correction `GRAMMAR`.
- Assert `kp_evidence` (`source = 'lesson_writing'`) có đúng 4 dòng: `ESSAY_KP=true, TR_KP=true, CC_KP=false, LR_KP=true`;
  không có `GRA_KP` (ngoài danh mục).
- Assert `question_attempt_events`: 1 dòng, `knowledge_point_ids = {ESSAY_KP}`, `error_code = 'GRAMMAR'`, `correct = true`.
- Assert JSON `lesson_writing_submissions.result` chứa `knowledgePointId`.

Test mới `aGradeStoredBeforeCriterionKnowledgePointsStillReads`:
- Chấm một bài như `gradedEssayChargesOnceRecordsEvidenceAndAResendReplaysIt`.
- `UPDATE lesson_writing_submissions SET result = '<JSON cũ, criteria không có knowledgePointId>'`.
- `getWritingSubmissionUseCase.execute(USER, id)` không lỗi và trả 4 tiêu chí.

`PracticeEssayIntegrationTest`, test mới `aCountedPracticeEssayRecordsEvidenceForEachCriterion`:
- Dựng theo `anEssayIsGradedAndChargedOnceThenCountsAsWritingWithoutAReviewOrAPracticeRequirement` (dòng 151), với rubric
  có KP tiêu chí và bands như trên.
- Assert `kp_evidence WHERE source = 'practice_set'` cho essay: 5 dòng (KP đề + 4 KP tiêu chí, Practice không lọc danh
  mục), mỗi dòng có `question_version_id = ESSAY`.
- Assert event của essay có `error_code` và mảng KP chỉ gồm KP của đề.
- Assert response: `passed`, `reviewsCreated` không đổi so với test gốc.
- Assert `xp_ledger` không có dòng mốc mới cho KP tiêu chí (`source_ref` chứa id KP; xem `GamificationRecorder.recordRises`).
  Test chạy theo công thức mặc định trong code (`MasteryCalculator`/DeepTutor, 5 kết quả gần nhất), trừ khi file test
  đặt `learning.mastery.policy=MP1`. Kiểm tra trước. Nếu một bài chưa đủ đẩy `TR_KP` qua ngưỡng `CONSOLIDATE`, chèn trước
  vài dòng `kp_evidence` đúng (`source = 'practice_set'`) cho `TR_KP` bằng SQL rồi gọi `masteryProfiles.rebuild(USER,
  List.of(TR_KP))`. Kiểm chứng ngược: tạm gọi `recordEvidence` không có `withoutMilestone` thì phải có dòng mốc. Nếu
  fixture không cho phép thì ghi rõ trong báo cáo và dựa vào review code của bước 2.

Chạy: `mvn -q -pl services/learning-service -am package`.

## Tiêu chí hoàn thành

- [ ] `WritingEvidenceRuleTest` xanh, đủ 9 case.
- [ ] Essay lesson và essay Practice ghi bằng chứng tiêu chí đúng theo `band >= passBand`; bài không có KP tiêu chí ghi như cũ.
- [ ] Event essay: 1 dòng, KP như cũ, có `error_code`.
- [ ] Không có XP mốc cho KP chỉ đến từ tiêu chí essay Practice.
- [ ] Mọi test hiện có của learning-service xanh, đặc biệt `LessonWritingIntegrationTest` dòng 149, 228, 244, 255, 368
      và `PracticeEssayIntegrationTest` dòng 175, 178 (không phải sửa assert).
