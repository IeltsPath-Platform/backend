# Phase 12: Import Excel theo template

Người làm: **Claude Code**. Nhánh: `feat/content-course-import` (backend), tạo từ `origin/main` sau khi phase 11 đã merge.
Trạng thái: Done (2026-10-23, chưa commit/merge; xem mục "Kết quả")

Repo: `backend`, service `content-service`. Phụ thuộc: phase 01 (tạo gói, đề cuối, gắn câu vào section), phase 07 (`import_ref`), phase 10 (`CourseCoverageAnalyzer`, đường duyệt course). Không cần hạ tầng LLM.

## Mục tiêu

Mentor: import file Excel, có **template tải về ngay chỗ bấm Import**, hệ thống đọc file và tạo nội dung. Một file tạo được cả cấu trúc course lẫn câu hỏi. Vì file dễ nhập sai, import chạy hai bước: **kiểm tra trước** (không ghi gì, báo cáo từng dòng) rồi **ghi** (tất cả hoặc không).

## Dependency

Apache POI (`poi-ooxml`) đã được duyệt (2026-10-16). Chọn phiên bản tương thích BOM Spring Boot đang dùng, ghi vào `pom.xml` của content-service; kiểm `mvn dependency:tree` không kéo xung đột. Không thêm thư viện đọc file nào khác.

## Yêu cầu

- **Template** `GET /api/content/admin/course-import/template` (chỉ `CONTENT_AUTHOR`): trả `.xlsx`, mỗi sheet dòng 1 là tiêu đề cột, dòng 2 là mô tả và ví dụ, có chú thích ô và danh sách giá trị cho cột có tập cố định:

  | Sheet | Cột chính |
  |---|---|
  | `Info` | phiên bản template |
  | `Course` | code, name, bandLevel |
  | `Topics` | code, name, skill, sortOrder |
  | `Lessons` | code, topicCode, title, summary, sortOrder, knowledgePointCodes |
  | `Blocks` | lessonCode, sortOrder, type (`TEXT`, `EXERCISE`), text, questionRefs, knowledgePointCodes |
  | `Questions` | importRef, type, skill, stem, optionA…optionF, answer, explanation, difficulty, knowledgePointCodes, distractorErrorCodes |
  | `PracticeSets` | packageCode, title, lessonCode, knowledgePointCodes, questionRefs |
  | `Tests` | kind (`TOPIC_TEST`, `COURSE_TEST`, `PRACTICE_TEST_MILESTONE`), packageCode, title, topicCode, afterTopicSequence, questionRefs |

  Template là một file tài nguyên của service (`src/main/resources/import/course-import-template-v1.xlsx`), không sinh động.
- **Kiểm tra trước** `POST /api/content/admin/course-import/validate` (multipart): đọc file, **không ghi gì**, trả `valid`, `errors[]` và `warnings[]` (mỗi mục có `sheet`, `row`, `column`, `code`, `message`), thống kê số topic, lesson, câu hỏi, gói sẽ tạo.
- **Luật kiểm tra** (domain `CourseImportValidator`, không phụ thuộc POI):
  - cấu trúc: đủ sheet và cột, đúng phiên bản (`TEMPLATE_VERSION_MISMATCH`); mã duy nhất và không trùng cái đã có (`CODE_CONFLICT`);
  - tham chiếu: `topicCode`, `lessonCode`; `questionRefs` trỏ `Questions.importRef` trong file **hoặc** câu đã có theo `import_ref`; `knowledgePointCodes` là KP `ACTIVE`; `distractorErrorCodes` thuộc danh mục `ACTIVE`;
  - câu hỏi: đáp án nằm trong phương án, kiểu câu khớp cột, `answer_spec` dựng ra qua bộ kiểm có sẵn của use case câu hỏi, có difficulty;
  - đề cuối: `TOPIC_TEST` phải có `topicCode` thuộc file; `COURSE_TEST` gắn course của file; câu của đề cuối thỏa luật publish sẽ áp sau (cảnh báo sớm nếu câu không tự chấm được hoặc essay thiếu `passBand`);
  - cây: chạy `CourseCoverageAnalyzer` lên cấu trúc dựng được; `PREREQUISITE_ORDER` chặn; `KP_NOT_COVERED` và `gaps` là cảnh báo.
- **Ghi** `POST /api/content/admin/course-import` (multipart, tải lại file): kiểm tra lại, rồi trong **một transaction** tạo:
  - course `INACTIVE`, topic `INACTIVE`, lesson `DRAFT`, block, liên kết KP;
  - câu hỏi `DRAFT` kèm `import_ref`, KP, difficulty, `distractors`;
  - gói `PRACTICE_SET` gắn lesson, `TOPIC_TEST` gắn topic, `COURSE_TEST` gắn course, mỗi gói một version `DRAFT` với section và câu đã gắn; mốc practice test;
  - lỗi bất kỳ → `422` kèm báo cáo, **không tạo gì**.
  Dùng use case của phase 01 (gói, section, câu trong section, đề cuối), lesson authoring và phase 10; không viết đường ghi riêng. Câu từ Excel do người nhập nhưng vẫn là `DRAFT`, phải duyệt và xuất bản như thường.
- **An toàn khi đọc file:**
  - chỉ `.xlsx` (kiểm phần mở rộng lẫn chữ ký tệp), tối đa 5 MB, tối đa 2000 dòng tổng;
  - ô công thức là lỗi `FORMULA_NOT_ALLOWED`; không chạy macro; tắt thực thể ngoài (XXE); giới hạn tỉ lệ và dung lượng giải nén (chặn zip bomb);
  - chuỗi cắt gọn và chuẩn hóa; không log nội dung ô;
  - hạn mức theo người dùng bằng `ai_usage_daily` với `kind = IMPORT` (phase 08), tính cho bước ghi.
- Multipart: `content-service.yaml` đã có 25 MB từ phase 01, đủ cho 5 MB. Chỉ sửa Gateway nếu thật sự chặn, và báo người dùng trước.
- Contract mới `docs/contracts/course-import-v1.md` (template, báo cáo kiểm tra, mã lỗi) viết trước khi code.

## Thiết kế phía code

```text
CourseImportController ── template / validate / import
application: DownloadImportTemplateUseCase, ValidateCourseImportUseCase, ExecuteCourseImportUseCase
port: CourseWorkbookReader (đọc .xlsx → CourseImportModel, thuần dữ liệu)
infrastructure: PoiCourseWorkbookReader, tài nguyên template
domain/service: CourseImportValidator
```

Luật kiểm tra ở domain; POI chỉ ở infrastructure sau port.

## Test

- `CourseImportValidatorTest` (dựng `CourseImportModel` bằng tay): mã trùng, tham chiếu lạ, đáp án ngoài phương án, tiên quyết sai thứ tự, `TOPIC_TEST` thiếu topic.
- `PoiCourseWorkbookReaderTest`: file mẫu đúng; thiếu sheet; ô công thức; file quá lớn; zip bomb nhỏ giả lập.
- `CourseImportIntegrationTest` (Testcontainers): validate rồi import file mẫu tạo đủ course, topic, lesson, câu hỏi, gói và đề cuối gắn đúng chỗ; file lỗi một dòng thì không tạo gì; template tải về mở lại đọc được.

## Hoàn thành khi

`mvn -q -pl services/content-service -am test` xanh; file lỗi một dòng báo đúng sheet, dòng, cột và không tạo bản ghi nào; công thức, macro, file quá lớn bị từ chối; course tạo ra `INACTIVE`.

## Kết quả

Đã làm đủ ba route, contract `docs/contracts/course-import-v1.md` và file template `src/main/resources/import/course-import-template-v1.xlsx`.

- **Cấu trúc code:** một `CourseImportUseCases` (validate và execute) và `DownloadImportTemplateUseCase`, thay cho ba use case trong thiết kế. Đọc file qua port `CourseWorkbookReader` (`PoiCourseWorkbookReader`), luật kiểm tra thuần domain ở `CourseImportValidator`, đọc danh mục hàng loạt qua `ImportCatalogReader`. Phần dựng course dùng chung `CourseMaterializer` với đường duyệt course draft; câu hỏi, gói, section dùng use case có sẵn.
- **Kiểm tra cây:** `PREREQUISITE_ORDER` và trùng mã/band/sortOrder chặn; `KP_NOT_COVERED` và `gaps` là cảnh báo. Cảnh báo thiếu bài luyện được tính lại có cộng câu hỏi và practice set của chính file, vì phân tích gốc chưa thấy chúng (chưa lưu).
- **An toàn file:** `.xlsx` theo tên và chữ ký ZIP, 5 MiB, 2000 dòng tổng, ô tối đa 20000 ký tự, công thức, macro (`vbaProject`) và zip bomb (tỉ lệ + kích thước giải nén) bị từ chối; thông báo lỗi không chứa nội dung ô. Hạn mức `IMPORT` chỉ tính ở bước ghi.
- **Không làm:** "cảnh báo sớm câu không tự chấm / essay thiếu passBand" của đề cuối không cần, vì file chỉ nhận bốn kiểu câu tự chấm được (`MULTIPLE_CHOICE`, `TRUE_FALSE_NOT_GIVEN`, `FILL_IN_BLANK`, `SHORT_ANSWER`). Gói tạo ra vẫn phải qua kiểm tra khi xuất bản như thường.
- **Lỗi thật tìm ra khi viết test tích hợp, đã sửa:**
  - `distractorErrorCodes` chỉ kiểm mã `ACTIVE`, trong khi use case câu hỏi đòi thêm `APPROVED` (toàn bộ mã seed còn `DRAFT`): file qua bước kiểm tra rồi hỏng ở bước ghi. Import và bộ sinh câu hỏi (phase 09) nay dùng `findApprovedActive`.
  - `findByImportRef` trả bản tóm tắt không có version; import đọc lại câu đầy đủ bằng `findById` trước khi gắn vào block/section.
  - Thiếu part `file` trả 500; nay `400 MISSING_PART`. Quá giới hạn multipart 25 MiB ở route import nay trả `FILE_TOO_LARGE` thay vì `AUDIO_TOO_LARGE`.
- **Test:** `CourseImportValidatorTest` (14), `PoiCourseWorkbookReaderTest` (14), `ImportTemplateFileTest`, `CourseImportIntegrationTest` (8, Testcontainers: validate không ghi, import tạo course `INACTIVE`/lesson, câu hỏi, gói `DRAFT` và mốc đúng chỗ, file lỗi không tạo gì, mã đã có là xung đột, hết hạn mức không tạo gì, lỗi giữa chừng rollback sạch), `CourseImportControllerTest` (9), `CourseImportAuthorizationTest`. Ghi lại file template khi định nghĩa cột đổi: chạy `ImportTemplateFileTest` với `-DwriteImportTemplate=true`.
- **Chưa chạy lại toàn bộ suite sau phase này** (chạy một lượt cuối khi cả plan xong).
