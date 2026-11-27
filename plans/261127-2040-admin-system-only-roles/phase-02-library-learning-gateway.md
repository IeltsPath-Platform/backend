# Phase 2: library-service, learning-service, api-gateway

## library-service

- `api/controller/AdminVocabularyCatalogController.java`: `hasAnyRole('CONTENT_AUTHOR','ADMIN')` đổi thành `hasRole('CONTENT_AUTHOR')` (đường dẫn `/api/content/admin/vocabulary` giữ nguyên vì web của Content Author đang dùng).
- Test đang cho ADMIN quyền ghi: `VocabularyCatalogSecurityTest`, `BusinessControllerRoleConventionTest`, và rà `StudyUnitSecurityTest`, `StudyAttemptSecurityTest`, `VocabularyLemmaControllerTest`, `VocabularySuggestionsControllerTest`, `GameWordSeedExistingMeaningPostgresTest`. Chỗ nào dùng ADMIN để ghi thì đổi sang CONTENT_AUTHOR và thêm ca ADMIN bị 403.

## learning-service

- Xoá `api/controller/AdminLearningGoalController.java` và `AdminLearningGoalControllerTest`.
- `ChangeLearningGoalStatusUseCase`: bỏ nhánh "không có learner thao tác" nếu không còn nơi nào truyền `null`.
- Giữ `GetLearningGoalsByUserIdUseCase` vì `LearningGoalController` còn dùng.
- Rà `LearningGoalSecurityTest`, `LearningGoalUseCaseTest`.

## api-gateway

- `infra/config-server/config-repo/api-gateway.yaml`: xoá route `/api/users/{id}/learning-goals/**` (rewrite sang `/api/learning/admin/users/{id}/goals`). Route `/api/users/me/learning-goals/**` giữ nguyên.
- `StudyDataRouteConfigTest`: bỏ hai đường admin goals.

## Kiểm tra

- Chạy test của `library-service`, `learning-service`, `api-gateway`.
- Content Author tạo từ mới từ web và từ bước duyệt khoá học nhập Excel vẫn chạy.
