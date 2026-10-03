# P2b – Content: mỗi câu hỏi chỉ thuộc một nơi dùng

Service: `content-service`. Phụ thuộc: P2. Một migration mới: `V17__question_purpose.sql`.

## 1. Vì sao

Lesson và Practice luôn hiện lời giải cho học viên (D11). Nếu cùng câu đó xuất hiện lại trong đề thi thử (`MOCK_TEST`),
thi xếp lớp (`PLACEMENT_TEST`) hoặc thi cuối topic, học viên được điểm nhờ nhớ đáp án: band sai và evidence mastery sai.
Hiện chỉ có một chiều được chặn: đề Practice không chứa câu của lesson hoặc `TOPIC_TEST`. Đề thi thử và thi xếp lớp
chưa được kiểm tra.

## 2. Quyết định (D16)

- **Dùng chung ngân hàng câu hỏi** (`questions`, `question_versions`), không tách bảng cho câu thi thử.
- **Thêm cột `questions.purpose`** (`LEARNING` | `EXAM`, chủ dự án chốt 2026-10-03): đánh dấu câu dành cho học (lesson,
  Practice, thi cuối topic) hay cho thi (thi thử, thi xếp lớp), để lọc ngân hàng khi soạn đề và chặn gắn nhầm loại.
- **Vẫn giữ luật một chủ (§3):** `purpose` không chặn được một câu nằm ở hai đề thi thử, hay vừa ở lesson vừa ở Practice.
- **Không dùng trigger DB.** Kiểm tra khi publish (chỉ đề đã publish tới tay học viên) và bằng test seed.

## 3. Luật

Một câu hỏi (theo `question_id`, mọi version của nó) có **tối đa một chủ**:

- một lesson (qua `lesson_block_questions`), **hoặc**
- một package (mọi version của cùng package dùng chung câu là hợp lệ).

Áp cho mọi loại package dùng trong học và thi: `PRACTICE_SET`, `TOPIC_TEST`, `MOCK_TEST`, `PLACEMENT_TEST`. `QUIZ`
và `LESSON` (package đọc cũ) ngoài phạm vi; ghi vào README nếu seed có trường hợp này.

"Chủ khác" được tính khi câu nằm trong: một lesson bất kỳ (mọi trạng thái, vì lesson chỉ đến từ seed), hoặc **version
đã publish** của một package khác. Đề nháp (DRAFT) không chặn nhau; đề nào publish trước giữ câu.

## 3b. Cột `purpose`

Migration `V17__question_purpose.sql`:

```sql
ALTER TABLE questions ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'LEARNING'
    CONSTRAINT chk_questions_purpose CHECK (purpose IN ('LEARNING', 'EXAM'));
-- Questions already in a mock or placement test are exam questions.
UPDATE questions q SET purpose = 'EXAM'
WHERE EXISTS (SELECT 1 FROM question_versions qv
              JOIN section_questions sq ON sq.question_version_id = qv.id
              JOIN content_sections s ON s.id = sq.section_id
              JOIN content_package_versions v ON v.id = s.package_version_id
              JOIN content_packages p ON p.id = v.package_id
              WHERE qv.question_id = q.id AND p.package_type IN ('MOCK_TEST', 'PLACEMENT_TEST'));
CREATE INDEX idx_questions_purpose ON questions (purpose);
```

Seed hiện chưa có đề thi thử hay xếp lớp, nên mọi câu thành `LEARNING`; giữ câu `UPDATE` để chạy đúng trên DB có dữ liệu.

Luật khớp loại (ngoài luật một chủ):

| Nơi dùng | `purpose` bắt buộc |
| --- | --- |
| Lesson, `PRACTICE_SET`, `TOPIC_TEST` | `LEARNING` |
| `MOCK_TEST`, `PLACEMENT_TEST` | `EXAM` |

- Kiểm khi publish package (cùng chỗ với luật một chủ): câu sai loại ⇒ `422`, `details.code = QUESTION_PURPOSE_MISMATCH`.
- Lesson chỉ đến từ seed ⇒ kiểm bằng test seed.
- Không có API thêm câu vào section (đề chỉ đến từ seed), nên không có điểm kiểm thứ ba.
- API câu hỏi: `POST /api/content/questions` nhận `purpose` tuỳ chọn (mặc định `LEARNING`); response câu hỏi trả `purpose`;
  `GET /api/content/questions` nhận filter `?purpose=`. Không có API đổi `purpose` (không có API sửa câu hỏi; thêm version
  giữ nguyên `purpose`).
- Domain: `Question` có `purpose` (enum `QuestionPurpose` trong `domain/vo`), JPA entity/mapper/command/result/DTO nối qua.

## 4. Thay đổi code

| Chỗ | Việc |
| --- | --- |
| `JdbcLearningContentReader.LESSON_OR_TEST_QUESTION_VERSIONS` | Thêm câu của `MOCK_TEST`, `PLACEMENT_TEST` (cùng `TOPIC_TEST`). Đề Practice chứa câu thi thử/xếp lớp không còn eligible cho search, availability, `hasPracticeSet`. Cập nhật Javadoc port ("used by a lesson or by any final test, mock test or placement test"). |
| `questionVersionsReservedForLearning` | Thêm `MOCK_TEST`, `PLACEMENT_TEST`, để game không lấy câu thi. |
| Port `LearningContentReader` | Thêm `List<QuestionUsageConflict> questionsUsedElsewhere(UUID packageVersionId)`: mỗi câu của version đang được lesson khác hoặc version đã publish của package khác dùng, kèm `questionId`, `ownerType` (`LESSON`/`PACKAGE`), `ownerCode`. Một query. |
| `PublishContentPackageUseCase` | Với 4 loại package ở §3: gọi `questionsUsedElsewhere(targetVersion)`; có xung đột ⇒ `QuestionAlreadyUsedException` trước khi publish, không lưu. |
| `domain/exception/QuestionAlreadyUsedException` + `GlobalExceptionHandler` | `422`, `details.code = QUESTION_ALREADY_USED`, message liệt kê tối đa 10 câu và chủ của chúng. |
| `api/dto/...` | Không đổi request/response. |

## 5. Seed

- Test nhất quán (Testcontainers, `LessonPipelineSeedTest`): không câu hỏi nào có hai chủ theo §3, xét lesson và mọi
  package PUBLISHED thuộc 4 loại trên. Nếu seed hiện có vi phạm, **không sửa migration cũ**: ghi danh sách vào
  Verification và dừng hỏi chủ dự án (sửa bằng migration mới hoặc nới luật).
- Seed hiện chưa có `MOCK_TEST`; plan thi thử sau này phải qua test này.

## 6. Test

- Unit `PublishContentPackageUseCaseTest`: xung đột ⇒ `QuestionAlreadyUsedException`, không `save`, package vẫn DRAFT;
  câu sai `purpose` ⇒ `QuestionPurposeMismatchException`, không `save`.
- Integration (rollback): tạo `MOCK_TEST` chứa một câu của L1 ⇒ `questionsUsedElsewhere` trả xung đột `LESSON L1`;
  `MOCK_TEST` chứa câu của `PS-KP1-A` ⇒ xung đột `PACKAGE PS-KP1-A`; câu mới hoàn toàn ⇒ rỗng; hai version của cùng
  package dùng chung câu ⇒ không xung đột.
- Integration: đề Practice chứa câu của một `MOCK_TEST` đã publish ⇒ không còn trong `search`/`availability`.
- MVC: `422` với `details.code = QUESTION_ALREADY_USED`.

## 7. Không làm ở phase này

Giao mã đề thi thử chưa làm cho từng học viên, quy đổi band, chấm Writing/Speaking trong assessment, seed đề thi thử:
thuộc plan thi thử riêng.

## Acceptance

`mvn -pl services/content-service -am test` xanh (0 skip); contract `learning-content-internal-v1` và README content
mô tả luật một chủ, cột `purpose` và hai mã lỗi `QUESTION_ALREADY_USED`, `QUESTION_PURPOSE_MISMATCH`.

## Verification

Implemented 2026-10-03, including the revised purpose-column/API scope approved during implementation. Only the new
`V17__question_purpose.sql` migration was added; existing migrations and dependencies were unchanged.

- Seed ownership audit (one SQL query, grouped by `question_id` and distinct owner): **0 conflicting questions**.
  Seed purpose audit: **0 mismatches**. Intentional conflicting fixtures run inside rollback transactions.
- Ownership SQL compares all question versions, ignores other drafts and the same package, and returns lesson or
  package owners. Publication locks question rows in UUID order until commit, then checks ownership and purpose in
  separate queries so a waiting publisher sees the committed owner. Both failures leave the draft unsaved.
- V17 was tested against both the seed and an isolated schema migrated to V16 with an existing mock-test question:
  the question becomes `EXAM`, new questions default to `LEARNING`, and invalid purpose values fail the DB constraint.
- MVC checks cover both `422` codes, default/explicit purpose on creation, the catalog purpose filter and invalid
  filter values (`400`). Versions retain the question's immutable purpose.
- Source review: no remaining concrete defects. `graphify update .` succeeded (7,385 nodes, 25,048 edges); SQL files
  were not included in the graph because the existing installation lacks `tree_sitter_sql`. No graph artifacts committed.

Test commands and results (Docker running; no Testcontainers skips):

```powershell
mvn -q -pl services/content-service -am test '-Dtest=PublishContentPackageUseCaseTest,LessonPipelineSeedTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Initial ownership-only run: **35 passed**, 0 failures/errors/skips (5 use-case, 30 seed tests).

```powershell
mvn -q -pl services/content-service -am test '-Dtest=PublishContentPackageUseCaseTest,LessonPipelineSeedTest,QuestionControllerTest,ContentPackageControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Expanded run: **54 tests, 53 passed, 1 failed, 0 errors/skips**. The lock test received PostgreSQL's expected lock
timeout, but expected a different Spring exception classification. It now asserts SQLSTATE `55P03` directly.

```powershell
mvn -q -pl services/content-service -am test '-Dtest=LessonPipelineSeedTest#seedQuestionsMatchTheirOwnersPurposeAndPublishingLocksHoldUntilRollback' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Corrected lock test: **1 passed**, 0 failures/errors/skips. Passing focused classes were not rerun.

```powershell
mvn -q -pl services/content-service -am test
```

Final full suite ran **once** and passed: content-service **177 tests in 30 classes**, common-security **8 tests in
4 classes**; **185 passed total, 0 failures, 0 errors, 0 skips**. All Docker-backed tests executed. No passing suite
was rerun after this final check.

Changed files (Java paths below are relative to `services/content-service/src/main/java/com/group01/content/`):

| Area | Files |
| --- | --- |
| Domain | `domain/aggregate/Question.java`, `domain/repository/QuestionRepository.java`, `domain/vo/QuestionPurpose.java`, `domain/vo/QuestionUsageConflict.java`, `domain/exception/QuestionAlreadyUsedException.java`, `domain/exception/QuestionPurposeMismatchException.java` |
| API | `api/controller/QuestionController.java`, `api/dto/request/CreateQuestionRequest.java`, `api/dto/response/QuestionResponse.java`, `api/dto/response/QuestionDetailResponse.java`, `api/exception/GlobalExceptionHandler.java` |
| Application | `application/command/CreateQuestionCommand.java`, `application/port/LearningContentReader.java`, `application/result/QuestionResult.java`, `application/result/QuestionDetailResult.java`, `application/usecase/CreateQuestionUseCase.java`, `application/usecase/ListQuestionsUseCase.java`, `application/usecase/GetQuestionDetailUseCase.java`, `application/usecase/AddQuestionVersionUseCase.java`, `application/usecase/ArchiveQuestionUseCase.java`, `application/usecase/PublishContentPackageUseCase.java` |
| Persistence | `infrastructure/persistence/adapter/JdbcLearningContentReader.java`, `infrastructure/persistence/adapter/QuestionRepositoryAdapter.java`, `infrastructure/persistence/entity/QuestionJpaEntity.java`, `infrastructure/persistence/mapper/QuestionPersistenceMapper.java`, `infrastructure/persistence/repository/QuestionJpaRepository.java` |

Other changed files:

- `services/content-service/src/main/resources/db/migration/V17__question_purpose.sql`
- `services/content-service/src/test/java/com/group01/content/application/usecase/PublishContentPackageUseCaseTest.java`
- `services/content-service/src/test/java/com/group01/content/infrastructure/persistence/LessonPipelineSeedTest.java`
- `services/content-service/src/test/java/com/group01/content/api/controller/QuestionControllerTest.java`
- `services/content-service/src/test/java/com/group01/content/api/controller/ContentPackageControllerTest.java`
- `services/content-service/README.md`
- `docs/contracts/learning-content-internal-v1.md`
- `plans/261002-1600-skill-tracks-practice-remediation/plan.md`
- `plans/261002-1600-skill-tracks-practice-remediation/phase-02b-content-question-exclusivity.md`
