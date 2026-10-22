# Phase 01: Đóng việc đồ thị tri thức (backend)

Trạng thái: code xong, test liên quan xanh, chưa commit.

## Đã có trong working tree (repo `backend`, nhánh `feat/content-author`)

- `V73__knowledge_graph_completion.sql`: điền CEFR và can-do cho 44 KP; nối 12 KP cô lập; thêm 34 cạnh; thêm 14 KP nháp `INACTIVE` (8 Speaking, 6 nền A1 đến B1) trong 4 topic `INACTIVE` không gắn course. KP active còn thiếu CEFR/can-do sau backfill chuyển `INACTIVE`. Cuối file có khối `DO $$` tự kiểm (cạnh không ngược cấp, không vòng, KP nháp vẫn INACTIVE).
- Luật trong `KnowledgePoint`: KP active bắt buộc `cefrLevel` và `canDo` ở `create`, `update`, `activate` (không ở constructor tái dựng).
- Luật cấp độ cạnh: `KnowledgeGraph.levelConflicts/levelViolations/isolated`; dùng trong `ReplaceKnowledgePointPrerequisitesUseCase` và `UpdateKnowledgePointUseCase` (khi đổi CEFR); lỗi `422 PREREQUISITE_LEVEL_INVALID` kèm `invalidIds` (`PrerequisiteLevelException`, `GlobalExceptionHandler`).
- Mã Speaking: tiền tố `S` trong `KnowledgePointCodeRules` và `CreateKnowledgePointRequest` (yêu cầu `skill=SPEAKING`); `canDo` bắt buộc khi tạo.
- `GET /api/content/knowledge-tree/health` (`ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`): `isolatedIds`, `missingMetadataIds`, `levelViolations`; `GetKnowledgeTreeHealthUseCase`, `KnowledgeTreeHealthResult/Response`.
- Contract đã cập nhật: `docs/contracts/knowledge-tree-v1.md`.
- Không có CHECK ở DB cho luật CEFR/can-do (nhiều test chèn KP active bằng SQL không có hai trường này); luật nằm ở aggregate.

## Test

- Mới: `KnowledgeGraphCompletionMigrationTest`, `KnowledgeGraphCompletionLegacyRowsMigrationTest`; thêm vào `KnowledgePointTest`, `KnowledgeGraphTest`, `CreateKnowledgePointUseCaseTest`, `KnowledgePointControllerTest`, `KnowledgeTreeControllerTest`, `KnowledgeTreeIntegrationTest`.
- Ghim phiên bản migration cho hai test lịch sử (`KnowledgePointMetadataMigrationTest` ở V52, `GrammarVocabularySeedMigrationTest` ở V55) vì V73 đổi dữ liệu mà chúng kiểm.
- Đã sửa 5 test từng lỗi: JSON tạo KP trong `ContentAuthorizationWebMvcTest` thiếu `canDo`; `JdbcTestKnowledgePointRepository` trong `KnowledgeTreeIntegrationTest` không đọc `cefr_level`, `can_do`, `scope_in`, `scope_out`.
- Kết quả: 11 class liên quan, 76 test, xanh. Lần chạy toàn bộ content-service trước bản sửa: 558 test, đúng 5 test lỗi nêu trên. **Cần chạy lại toàn bộ** `mvn -q -pl services/content-service -am test` để xác nhận trước khi commit.

## Việc còn lại

1. Chạy lại toàn bộ test content-service, ghi kết quả.
2. Commit khi người dùng đồng ý, đề xuất một commit: `feat(content): complete the knowledge graph with CEFR levels and a health check`.
3. Số liệu seed sau V73 (đo trên Postgres tạm áp V1 đến V73): 71 KP (44 active, 27 inactive), 71 cạnh, CEFR gồm A1 2, A2 3, B1 23, B2 27, C1 16, C2 0; 0 KP active cô lập. Cấp C2 chưa có KP nào.
