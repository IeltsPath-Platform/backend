# Phase 1: Content, package placement và purpose PLACEMENT

## Context

- `QuestionPurpose` hiện có `LEARNING`, `EXAM`; V17 gom `MOCK_TEST` và `PLACEMENT_TEST` vào `EXAM`.
- `PublishContentPackageUseCase` ép `MOCK_TEST, PLACEMENT_TEST -> EXAM`; `questionsWithWrongPurpose` kiểm.
- Internal API mẫu: `InternalLearningContentController` `courses/{id}/test-packages`.
- DB hiện không có package `PLACEMENT_TEST`; có sẵn 25 câu Listening, 170+ Reading, 7 Writing (ESSAY), 0 Speaking, đều `LEARNING`.

## Requirements

- Thêm `PLACEMENT` vào `QuestionPurpose` và CHECK của `questions.purpose`.
- `PLACEMENT_TEST` chỉ nhận câu `PLACEMENT`; `MOCK_TEST` vẫn chỉ nhận `EXAM`.
- Seed một package `PLACEMENT_TEST` published có 4 section: Listening, Reading, Writing, Speaking, với câu mới `purpose=PLACEMENT` (không dùng lại câu LEARNING, vì rule độc quyền câu hỏi).
- Endpoint nội bộ trả package/version published mới nhất.

## Files

- Create: `db/migration/V24__placement_question_purpose.sql` (nới CHECK; không sửa V17).
- Create: `db/migration/V25__seed_placement_test.sql` (câu, version, section, package, version published).
- Modify: `domain/vo/QuestionPurpose.java`, `PublishContentPackageUseCase.java` (mapping purpose), `InternalLearningContentController.java`, `LearningContentReader` + `JdbcLearningContentReader` (query package PLACEMENT_TEST published), `GetCourseTestPackagesUseCase` tương đương cho placement.
- Tests: `PublishContentPackageUseCaseTest` (PLACEMENT/EXAM không lẫn), seed test kiểu `LessonPipelineSeedTest`, test controller nội bộ.

## Steps

1. Migration purpose; cập nhật enum và mapping publish.
2. Soạn seed: Listening MCQ/fill-in (cần `CONTENT_MEDIA_BASE_URL`; audio dùng key seed hiện có hoặc URL đầy đủ), Reading MCQ/TFNG, Writing ESSAY có `passBand`, Speaking `SPEAKING` (prompt, không answer spec chấm).
3. Use case + endpoint nội bộ `placement-packages`.
4. Test.

## Risks

- Câu Speaking: `AnswerSnapshot` cần xử lý loại không có answer spec; kiểm `QuestionType.SPEAKING` đã có ở enum (xác nhận khi làm).
- Audio Listening không phát được nếu thiếu cấu hình media; seed cần chạy được với test (URL https đầy đủ).
- Rollback: migration mới, không đụng V17; xóa package seed bằng migration mới nếu cần.
