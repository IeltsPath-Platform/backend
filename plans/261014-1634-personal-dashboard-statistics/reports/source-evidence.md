# Bằng chứng nghiên cứu dashboard cá nhân

Ngày: 14/10/2026. BE `main@28c18bb`, FE `main@0def395`. Repo docs có wireframe P-11. Không đọc `.env`, `.env (1)`, database cá nhân hoặc secret. Báo cáo chỉ kiểm code/schema, không xác nhận dữ liệu runtime.

## Hiện trạng đã kiểm

Các đường dẫn BE tính từ `D:/Fall2026/New folder/BE/backend`, FE từ `D:/Fall2026/New folder/FE/frontend`, Docs từ `D:/Fall2026/New folder/docs`.

| Nguồn | Bằng chứng/ý nghĩa |
|---|---|
| BE `services/user-service/src/main/java/com/ieltspath/user/domain/aggregate/LearningActivity.java` | create nhận occurredAt/durationSeconds từ caller, UUID.randomUUID, verifiedAt null; không phải active-time ledger |
| BE `services/user-service/src/main/java/com/ieltspath/user/application/usecase/CreateLearningActivityUseCase.java` | Lưu activity; không thấy luồng lesson tự gửi hoạt động qua use case này trong tìm kiếm |
| BE `services/user-service/src/main/java/com/ieltspath/user/domain/aggregate/LearningGoal.java` | targetBand/examDate/availableMinutesPerDay/status; không có lịch target per-course hoặc weekday |
| BE `services/user-service/src/main/java/com/ieltspath/user/api/controller/LearningGoalController.java` | GET/POST goal dưới `/api/users/me/learning-goals`, PUT status; có thể giữ contract cũ và thêm history bên trong |
| BE `services/user-service/src/main/java/com/ieltspath/user/application/usecase/UpsertStreakUseCase.java` | Streak có write API; không được tự xem là máy tính thời gian học |
| BE `services/learning-service/src/main/java/com/ieltspath/learning/domain/aggregate/LessonProgress.java` | complete idempotent; completedAt và passedBlockIds tách nhau |
| BE `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` | lesson_progress chứa completed_at/updated_at nhưng không active duration; evidence và result version log là cấu trúc khác |
| BE `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/ListCoursesUseCase.java` | Tổng hợp topicCount/passedTopicCount, recommended và courseTestStatus; gọi refreshTopics, không là enrollment list |
| BE `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java` | LessonSummary/Lesson có skills; hiện đọc lessons theo từng topic; cần batch cho aggregate dashboard |
| BE `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/CompleteLessonUseCase.java` | LearnerLock/transaction và complete một lần; không dùng dashboard thay business completion |
| BE `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalLearningContentController.java` | Internal metadata có topic-sequence/topic lessons; không có endpoint dashboard batch được plan đề xuất |
| BE `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/entity/AssessmentResult.java` | Result version/status/overallBand/completedAt; completed result bất biến, regrade phiên bản tiếp theo |
| BE `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/entity/SkillScore.java` | Skill/band/gradingSource; có thể mở rộng provenance, không cần duplicate result store |
| BE `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/PlacementGradingService.java` | Speaking config default 0.0; Writing fallback 5.5; skill bands được recompute từ dữ liệu/config khi đọc, cần frozen provenance cho dashboard |
| BE `services/assessment-service/src/main/java/com/ieltspath/assessment/domain/vo/AttemptType.java` | Có enum PLACEMENT/MOCK/gates/QUIZ; enum không chứng minh flow hoặc rubric đủ chuẩn |
| BE `services/library-service/src/main/java/com/ieltspath/library/domain/aggregate/Flashcard.java` | Source/sense/front/back/status/timestamps; chưa có memory level/nextReviewAt/recall event |
| BE `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` | Personal flashcards/decks; không phải schema SRS/review progression |
| BE `infra/config-server/config-repo/api-gateway.yaml` | Route owner theo prefix, flashcards thuộc Library; không có route catch-all learning-support chung |
| FE `README.md` | Dashboard dùng dữ liệu mẫu riêng, không theo VITE_USE_MOCK_LEARNING |
| FE `src/features/overview/pages/OverviewPage.tsx` | Local tab state, sidebar và import MOCK_*; chi tiết chưa có API hook |
| FE `src/features/overview/components/OverviewDetailedStats.tsx` | Time chart/band/mentor hardcoded; pill chỉ state; buckets 12/25/34/20/110 và donut 34% mâu thuẫn |
| FE `src/types/overview.ts`, `src/mocks/overviewData.ts` | Shape hiển thị mock, không phải contract aggregate BE |
| FE `src/lib/httpClient.ts` | Bearer memory-only, credentials include, refresh single-flight; chưa có keepalive option |
| FE `src/features/learning-path/api/httpLearningApi.ts`, `lib/useApiResource.ts` | Nền integration thật để tái sử dụng thay vì viết client/auth mới |
| FE `package.json` | Có lint/typecheck/build, không script test; không chart/query framework mặc định |
| Docs `wireframes/screens/P-11_OverviewDashboard.drawio`, `wireframes/exports/P-11__du-lieu-chi-tiet.png` | Bố cục tham chiếu màn hình người dùng gửi; Mentor bị loại bởi quyết định mới |
| Docs `rp_3.2_screen_design_spec/CHANGELOG.md`, `scripts/sds_content_general.py` trong cùng thư mục | Navigation wireframe mới dùng menu thay sidebar; ghi nhận riêng, không tự mở rộng scope thống kê |

## Giới hạn nguồn

- BE README/AGENTS tham chiếu docs/system-architecture.md và docs/contracts nhưng thư mục `backend/docs/` không tồn tại ở checkout này; không coi tài liệu thiếu là đã đọc.
- Không có `graphify-out/graph.json`; dùng rg và source trực tiếp, không cài/build Graphify.
- FE AGENTS còn mô tả skeleton trong khi code đã có router/features; fact lấy từ code/README/manifest mới.
- FE plans rỗng theo scout; một số BE plan có nội dung cũ về placement. Không suy từ trạng thái plan rằng hành vi đó đang chạy.
- Nghiên cứu phụ bị dừng bởi quota sau các kết quả trung gian; không có review độc lập hoàn chỉnh. Controller đối chiếu các nguồn chính và tự rà nhất quán tài liệu.

## Phân biệt kết luận và đề xuất

Sự tồn tại/thiếu trường trong code, hardcode FE và version result là kết luận nguồn. Endpoint mới, table mới, lịch ôn 1/3/7/14 ngày, heartbeat 30s, idle 120s, retention 90 ngày và SLO là **đề xuất chưa được duyệt**. Người dùng mới chốt hoàn thành bài học cho radar và loại Mentor.
