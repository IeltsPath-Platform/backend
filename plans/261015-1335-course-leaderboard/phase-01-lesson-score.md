---
phase: 1
title: "Lesson score on completion + backfill"
status: pending
dependencies: []
---

# Pha 1: Điểm lesson khi hoàn thành

Gốc Java: `services/learning-service/src/main/java/com/ieltspath/learning/`.

## Dữ kiện đã xác minh

- `application/service/LessonCompletion.java`: `afterPracticeChange()` gọi `practice.refreshPassForLesson()` rồi `complete()`. `complete()` chỉ save khi `progress.complete()` trả true → đây là điểm duy nhất lesson chuyển sang completed. Caller giữ learner lock + transaction.
- `PracticeProgress.refreshPassForLesson()` lấy `content.lessonPracticeSets(lessonId)` = danh sách bộ bắt buộc dùng cho clearance → dùng lại đúng danh sách này để chấm điểm.
- `practice_attempts` (V4, V9): `correct_count`, `total_count`, `counted_as_evidence`, `submitted_at`, `response` JSONB chứa `PracticeSubmission.skillScores[] {skill, correct, total}`. Chỉ có lần nộp Practice; bài ôn nằm ở `review_sets`.
- `ItemGrading`: `total` gồm cả câu essay → mẫu số objective = tổng `skillScores` trừ `WRITING`.
- Migration mới nhất: `V12__lesson_completion_requires_every_practice_set.sql` → dùng `V13`.

## Luật tính

Với mỗi bộ bắt buộc của lesson: lấy lần nộp Practice **đầu tiên** (`submitted_at` nhỏ nhất).
- `objectiveTotal` = Σ `total` các skillScore khác WRITING (response cũ không có skillScores → `total_count`).
- `objectiveCorrect` = như trên với `correct`; nếu `counted_as_evidence = false` (đã lộ đáp án) → 0.

`lessonScore = round_half_up(10000 × ΣobjectiveCorrect / ΣobjectiveTotal)` lưu dạng `score_units` 0..10000 (0,01 điểm). Σtotal = 0 (lesson chỉ Writing / không có practice) → không ghi dòng điểm.

## Thay đổi

**Tạo**
- `resources/db/migration/V13__lesson_scores.sql`:
  ```sql
  CREATE TABLE lesson_scores (
      user_id UUID NOT NULL,
      lesson_id UUID NOT NULL,
      topic_id UUID NOT NULL,
      score_units INTEGER NOT NULL CHECK (score_units BETWEEN 0 AND 10000),
      correct_count INTEGER NOT NULL CHECK (correct_count >= 0),
      total_count INTEGER NOT NULL CHECK (total_count > 0 AND correct_count <= total_count),
      completed_at TIMESTAMPTZ NOT NULL,
      PRIMARY KEY (user_id, lesson_id)
  );
  CREATE INDEX idx_lesson_scores_topic ON lesson_scores (topic_id, user_id);
  ```
  + câu backfill `INSERT ... SELECT ... ON CONFLICT DO NOTHING`: với mỗi `lesson_progress` đã completed, lấy lần nộp đầu của từng `package_id` trong `practice_attempts` của lesson đó, tính như luật trên từ `response->'skillScores'`; bỏ qua lesson Σtotal = 0. Course không lưu: suy từ topic lúc đọc (pha 2), tránh map topic→course ở write path.
- `domain/vo/LessonScore.java` (record + factory tính units, validate).
- `domain/repository/LessonScoreRepository.java`: `void insertIfAbsent(LessonScore)`.
- `infrastructure/persistence/JdbcLessonScoreRepository.java` (`ON CONFLICT (user_id, lesson_id) DO NOTHING`).
- `application/service/LessonScoring.java`: nhận userId, lesson, danh sách packageId bắt buộc → đọc lần nộp đầu theo tập (một query) → `Optional<LessonScore>`.

**Sửa**
- `PracticeAttemptRepository` + `JdbcPracticeAttemptRepository`: thêm `List<FirstSubmission> firstSubmissions(UUID userId, UUID lessonId, Collection<UUID> packageIds)` (`DISTINCT ON (package_id) ORDER BY submitted_at`), trả counted + correct/total objective đã trừ WRITING.
- `LessonCompletion.afterPracticeChange()`: truyền danh sách bộ của clearance vào `complete(...)`; trong `complete()` sau `lessons.save(progress)` gọi `LessonScoring` rồi `insertIfAbsent`. Không đổi chữ ký public của `complete(userId, lesson)` cho `CompleteLessonUseCase` (lesson không practice → không có điểm).
- `refreshPassForLesson` trả kèm danh sách bộ đã dùng (hoặc tách overload) để không gọi content hai lần.

Lỗi DB khi ghi điểm → rollback cùng completion (không catch nuốt lỗi).

## Test (Testcontainers, theo pattern `RemediationLadderIntegrationTest`)

`LessonScoreIntegrationTest`:
- 2 bộ 1/2 + 9/10 → 8333 units.
- Lần đầu 40%, bài ôn xong → lesson completed, điểm 40.
- Bộ đã lộ đáp án → đóng góp 0 đúng, vẫn có mẫu số.
- Mới nộp 1/2 bộ → chưa có dòng điểm.
- Gọi `afterPracticeChange` hai lần → một dòng.
- Bộ có câu WRITING → mẫu số chỉ câu objective.

Backfill: một test chạy migration trên dữ liệu seed tối thiểu (hoặc test SQL backfill tách thành hàm `pg` gọi được) — nếu Flyway test nặng, chấp nhận verify bằng tay trên DB dev và ghi lại kết quả.

Lệnh: `mvn -pl services/learning-service -am test -Dtest=LessonScoreIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`, rồi toàn bộ learning-service (`AnswerSpecGraderTest` đang lỗi sẵn, không liên quan).
