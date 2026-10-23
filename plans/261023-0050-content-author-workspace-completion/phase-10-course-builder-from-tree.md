# Phase 10: Dựng course từ cây tri thức

Người làm: **Claude Code**. Nhánh: `feat/content-course-drafts` (backend), tạo từ `origin/main` sau khi phase 09 đã merge.
Trạng thái: Pending

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

(Người làm điền.)
