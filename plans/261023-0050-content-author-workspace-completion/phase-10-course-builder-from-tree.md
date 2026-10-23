# Phase 10: Dựng course từ cây tri thức

Người làm: **Claude Code**. Nhánh: `feat/content-course-drafts` (backend), tạo từ `origin/main` sau khi phase 09 đã merge.
Trạng thái: Done (2026-10-23, chưa commit/merge; xem mục "Kết quả")

Repo: `backend`, service `content-service`. Phụ thuộc: phase 01 (tạo gói, đề cuối, gắn câu), phase 07 (`import_ref`), phase 08 (bảng bản nháp).

## Mục tiêu

Mentor: "course bám theo cây tri thức để tạo ra", không phải "có đề rồi import vào". Phase này tạo **bản nháp course** (một cấu trúc dữ liệu, chưa phải course thật), bộ kiểm "course phủ cây đến đâu", và bước duyệt biến nó thành course, topic, lesson, gói thật ở trạng thái nháp. Soạn tay và AI (phase 11) dùng chung cấu trúc và API này.

## Đọc trước

- `docs/contracts/lesson-authoring-v1.md` (tạo lesson, block, KP; dùng lại use case, không viết đường ghi riêng).
- Route course/topic admin hiện có (`POST/PUT /api/content/admin/courses`, `/api/content/admin/topics`) và `ContentStatus` (`ACTIVE`/`INACTIVE`).
- Phase 01 mục 7 (tạo `TOPIC_TEST`/`COURSE_TEST`) và các use case gói, section, gắn câu của phase 01.
- `course_practice_test_milestones` (V49) và `PracticeTestMilestoneController`.
- `docs/contracts/knowledge-tree-v1.md` (KP, tiên quyết, band).

## Yêu cầu

- **Cấu trúc bản nháp** lưu trong `ai_drafts.result` với `kind = COURSE`. Bản nháp soạn tay tạo thẳng ở `READY`, `model` null, `requested_by` là người soạn.

  ```json
  { "course": { "code": "IELTS_6_5_R", "name": "IELTS 6.5 Reading", "bandLevel": 6.5 },
    "topics": [{ "code": "…", "name": "…", "skill": "READING", "sortOrder": 1,
      "lessons": [{ "code": "…", "title": "…", "summary": "…", "knowledgePointIds": ["…"],
        "blocks": [{ "type": "TEXT", "text": "…", "knowledgePointIds": ["…"] },
                   { "type": "EXERCISE", "questionRefs": ["<importRef hoặc questionVersionId>"] }],
        "practiceSetPackageIds": ["…"] }],
      "topicTest": { "code": "…", "title": "…" } }],
    "courseTest": { "code": "…", "title": "…" },
    "practiceTestMilestones": [{ "packageId": "…", "afterTopicSequence": 2 }],
    "gaps": [{ "knowledgePointId": "…", "reason": "NO_PRACTICE_SET | FEW_PRACTICE_SETS | NO_EXERCISE_QUESTION" }] }
  ```

  - Khối `TEXT` do người soạn viết hoặc để trống; AI không sinh bài lý thuyết dài.
  - **Đề cuối:** mỗi topic có `topicTest`, course có `courseTest` (mã và tiêu đề). Topic và course là mới nên không có đề cuối nào có sẵn để chọn; duyệt sẽ tạo đề cuối rỗng ở `DRAFT` gắn vào topic/course mới (bằng use case phase 01). Tác giả soạn tiếp đề cuối ở màn "Đề & gói câu hỏi" trước khi kích hoạt course.
- **API** (chỉ `CONTENT_AUTHOR`, chỉ trên bản nháp của mình):
  - `POST /api/content/admin/course-drafts`: tạo bản nháp trống hoặc từ một cấu trúc có sẵn.
  - `PUT /api/content/admin/course-drafts/{id}`: lưu cấu trúc (kiểm cú pháp, chưa kiểm phủ). Chỉ khi `READY`.
  - `GET /api/content/admin/course-drafts/{id}/analysis`: kết quả kiểm phủ.
  - `POST /api/content/admin/course-drafts/{id}/approve`.
  - Danh sách và từ chối dùng API chung của phase 08 (`kind=COURSE`).
- **Kiểm phủ cây** (domain service thuần `CourseCoverageAnalyzer`):
  - **Phủ KP:** mọi KP `ACTIVE` thuộc band và kỹ năng của course nằm trong ít nhất một lesson; thiếu → cảnh báo `KP_NOT_COVERED`.
  - **Thứ tự tiên quyết:** B có tiên quyết A thì A ở lesson trước B trong cùng course, hoặc thuộc course band thấp hơn; vi phạm → lỗi `PREREQUISITE_ORDER` kèm cặp KP.
  - **Đủ kho đề:** mỗi KP cần tối thiểu 3 gói `PRACTICE_SET` `PUBLISHED` chưa gắn và tối thiểu 1 câu cho khối `EXERCISE`; thiếu → `gaps`.
  - **Gói hợp lệ:** gói chọn phải `PUBLISHED`, đúng loại, và chưa gắn lesson/topic/course nào; trùng → `PACKAGE_ALREADY_LINKED`.
  - Cảnh báo (`KP_NOT_COVERED`, `gaps`) không chặn duyệt; lỗi (`PREREQUISITE_ORDER`, `PACKAGE_ALREADY_LINKED`, mã trùng, tham chiếu không tồn tại) chặn duyệt.
- **Duyệt** (một transaction, tất cả hoặc không):
  - course `INACTIVE`, topic `INACTIVE`, lesson `DRAFT`, block, liên kết KP (use case lesson authoring);
  - gắn gói `PRACTICE_SET` đã chọn vào lesson mới;
  - tạo đề cuối rỗng `TOPIC_TEST` mỗi topic và `COURSE_TEST` cho course (phase 01), kèm một version `DRAFT` rỗng;
  - đặt mốc practice test vào `course_practice_test_milestones` nếu có;
  - mã course/topic/lesson/gói trùng cái đã có → `409 CODE_CONFLICT` kèm danh sách mã trùng;
  - bản nháp chuyển `APPROVED`. Course `INACTIVE` nên học viên không thấy cho tới khi tác giả kích hoạt.
- Contract mới `docs/contracts/course-drafts-v1.md` viết trước khi code.

## Thiết kế phía code

- Tạo: `C/domain/service/CourseCoverageAnalyzer.java`, `C/domain/vo/CourseDraftStructure.java`, `C/application/usecase/{CreateCourseDraftUseCase, SaveCourseDraftUseCase, AnalyzeCourseDraftUseCase, ApproveCourseDraftUseCase}.java`, `C/api/controller/CourseDraftController.java`.
- Sửa: use case/repository gói để gắn `PRACTICE_SET` có sẵn vào lesson khi duyệt (cập nhật `lesson_id` khi đang null); truy vấn "gói chưa gắn" nếu chưa có.
- Ràng buộc "một gói chỉ thuộc một chỗ": kiểm ràng buộc thực tế của `content_packages` trước khi code, ghi vào mục Kết quả. Nếu cần dùng chung đề giữa các course thì phải đổi mô hình gói: dừng và báo người dùng, không tự đổi.

## Test

- `CourseCoverageAnalyzerTest` (bảng tình huống): KP không được phủ; B đứng trước A; tiên quyết ở course band thấp hơn thì hợp lệ; KP thiếu gói; gói đã gắn nơi khác.
- `ApproveCourseDraftIntegrationTest` (Testcontainers): duyệt tạo đúng course/topic/lesson/block, gắn practice set, tạo đề cuối `DRAFT` gắn đúng topic/course, mốc practice test; lỗi giữa chừng thì không còn bản ghi nào; course `INACTIVE` không có trong `topic-sequence` của learning; đề cuối `DRAFT` không có trong `publishedTestPackages`.
- Mã trùng → 409 kèm danh sách.

## Hoàn thành khi

`mvn -q -pl services/content-service,services/learning-service -am test` xanh; phân tích trả đủ ba nhóm (phủ KP, thứ tự tiên quyết, thiếu kho đề); duyệt là nguyên tử; course tạo ra `INACTIVE`.

## Kết quả

**2026-10-23 (Claude Code), chưa commit.** Trạng thái: Done (chờ merge).

- Contract `course-drafts-v1.md` viết trước khi code; có phần "AI course drafts" để phase 11 bổ sung.
- Domain thuần: `CourseDraftStructure`, `CourseCatalogFacts`, `CourseDraftAnalysis`, `CourseCoverageAnalyzer` (không đọc DB). Kho dữ kiện đọc theo lô qua port `CourseCatalogReader` (`JdbcCourseCatalogReader`, khoảng 12 truy vấn cố định).
- Use case: `CourseDraftUseCases` (tạo thẳng `READY`, `model` null, không trừ hạn mức AI; sửa; phân tích), `ApproveCourseDraftUseCase` (một transaction, dùng lại `CreateCourseUseCase`, `UpdateCourseUseCase`, `CreateTopicUseCase`, `UpdateTopicUseCase`, `LessonAuthoringUseCase`, `CreateContentPackageUseCase.executeAsAuthor`, `CreateDraftPackageVersionUseCase`). Route `CourseDraftController` dưới `/api/content/admin/course-drafts`; danh sách và từ chối dùng route chung `ai-drafts?kind=COURSE`.
- Quyết định phải ghi lại:
  - **"KP thuộc band của course"** hiểu là KP `ACTIVE` có `cefr_level` bằng mức CEFR của band course (tới 4.5 B1, 6.0 B2, 7.5 C1, còn lại C2) và kỹ năng thuộc kỹ năng các topic của bản nháp hoặc `ALL`/rỗng. KP không có `cefr_level` không được mong đợi. Lý do: KP gắn với topic hiện có, còn topic của course mới chưa tồn tại nên không suy band từ cây được.
  - **Mốc practice test** ghi thẳng bằng `PracticeTestMilestoneRepository` chứ không qua `ReplacePracticeTestMilestonesUseCase`: use case đó chỉ nhận vị trí của topic `ACTIVE` có lesson `PUBLISHED`, mà topic mới là `INACTIVE` và lesson là `DRAFT`, nên luôn bị từ chối. Test và vị trí đã được kiểm bằng phân tích và kiểm cú pháp.
  - **Gắn practice set vào lesson** bằng câu `UPDATE` có điều kiện (`lesson_id IS NULL`, đúng loại) qua `ContentPackageRepository.attachPracticeSetToLesson`, không ghi lại cả gói và các version.
  - **Ràng buộc "một gói chỉ thuộc một chỗ"**: `content_packages` có `lesson_id`, `topic_id`, `course_id` riêng; gói `PRACTICE_SET` chỉ cho vào lesson khi cả ba đều null. `course_practice_test_milestones.package_id` là `UNIQUE`. Không phải đổi mô hình gói.
  - Mã trùng, band đã có course, `sort_order` topic gốc trùng đều là xung đột `409 CODE_CONFLICT`; mọi lỗi khác là `422 COURSE_DRAFT_INVALID`; lỗi cú pháp là `400 INVALID_COURSE_DRAFT` kèm đường dẫn.
  - `TopicRepositoryAdapter.save` đổi sang `saveAndFlush` để các ghi JDBC cùng transaction (lesson của topic mới, đề cuối) thấy topic.
  - Khối `EXERCISE` không mang KP (KP lấy từ câu); khối `TEXT` có thể trống.
- Test: `CourseCoverageAnalyzerTest` (19 tình huống), `CourseDraftCodecTest`, `CourseDraftUseCasesTest`, `CourseDraftControllerTest`, `CourseDraftAuthorizationTest`, `ApproveCourseDraftIntegrationTest` (Testcontainers: duyệt tạo đúng course/topic/lesson/block, gắn practice set, đề cuối `DRAFT` kèm version rỗng, mốc, course/topic `INACTIVE` không có trong `topicSequence`, đề cuối không có trong `publishedTestPackages`/`courseTestPackages`; lỗi giữa chừng không còn bản ghi nào; mã trùng là xung đột; tiên quyết sai thứ tự là lỗi; bản nháp của người khác là 404).
- Chưa làm: kiểm "course đã duyệt kích hoạt được" (kích hoạt vẫn là bước thủ công ở màn Khóa học, và đòi lesson/đề cuối đã xuất bản); sửa từng phần của bản nháp (chỉ thay cả cấu trúc bằng `PUT`).
