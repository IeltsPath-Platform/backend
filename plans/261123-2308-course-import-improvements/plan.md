---
title: Cải thiện chức năng nhập khóa học từ Excel
status: complete
created: 2026-11-23
scope: backend (content-service, config-repo, docs/contracts) + frontend (admin/course-import, admin/lessons)
---

# Cải thiện nhập khóa học từ Excel

Nguồn: đánh giá chức năng ngày 2026-11-23 (đọc code FE + BE, FE test 8/8, BE test import pass: authorization 2,
controller 9, integration 8, validator 14, reader 14, template 3 với 1 skip).
Quyết định của chủ sản phẩm đã chốt ở bảng dưới. Sáu phase đã triển khai và kiểm tra xong.

## Quyết định đã chốt

| Chủ đề | Quyết định |
| --- | --- |
| Mã theo "Thiết lập mã" | Khi tải template, đã có sẵn mã tiếp theo theo cấu hình. Tác giả tự sửa mã thì báo ở bước Kiểm tra. |
| Số dòng điền sẵn | Dòng dữ liệu đầu của mỗi trang tính (khóa học, topic, bài học, gói), cộng gợi ý mẫu mã và số tiếp theo ở dòng mô tả cột. |
| Mã sửa được | Khi cấu hình cho phép sửa mã, mã khác mẫu được chấp nhận im lặng. Chỉ báo lỗi khi cấu hình đang khóa hoặc mã bị trùng. |
| Course draft | Làm cùng đợt: sửa ở `CourseMaterializer` để import và duyệt course draft cùng tuân theo Thiết lập mã. |
| Tra mã KP và mã lỗi | Làm cả hai: trang tính chỉ-đọc trong template và nút tra cứu trên trang import. |
| Từ vựng | Không thêm trang tính. Tác giả thêm trong bản nháp của bài; trình chọn từ ghi rõ từ nào đã có trong thư viện, từ nào mới tạo. |
| Việc còn lại | Làm hết: lỗi dây chuyền, báo lỗi một đợt, màn kết quả, bỏ câu tiếng Anh ở FE, tier Gateway, nhãn từ vựng. |

## Phases

| # | Phase | Repo | Phụ thuộc |
| --- | --- | --- | --- |
| 1 | Sửa validator: lỗi dây chuyền và báo lỗi một đợt | backend | - |
| 2 | Mã theo Thiết lập mã: mã có sẵn trong template, kiểm tra khi Kiểm tra, áp cho cả course draft | backend | 1 |
| 3 | Template sinh động: trang tính KnowledgePoints và ErrorTypes | backend | 2 |
| 4 | Trang import: tra cứu, màn kết quả, dọn thông báo | frontend | 2, 3 |
| 5 | Tier rate limit cho course-import | backend (config-repo) | - |
| 6 | Nhãn từ vựng trong trình chọn từ của bài học | frontend | - |

### 1. Validator
- Lỗi dây chuyền: khi một câu hỏi có lỗi, phần tham chiếu `questionRefs` vẫn coi `importRef` đó là "có trong file". Tách tập `importRef` đã khai báo khỏi tập câu hỏi hợp lệ (`CourseImportValidator`: `questions`, `questionRefs`). Tương tự topic có `code` sai định dạng không được kéo theo `UNKNOWN_REFERENCE` ở bài học.
- Báo một đợt: đưa mã khóa/topic/bài học đã tồn tại, band đã dùng, thứ tự topic đã dùng vào `ImportCatalogFacts` để validator báo cùng lúc với lỗi dòng, có số dòng chính xác. Phần phân tích cây tri thức (thứ tự tiên quyết, thiếu luyện tập) vẫn chạy sau cùng vì cần plan hợp lệ.
- Câu T/F/NG: import đang đặt khóa phương án là A đến F, trong khi luồng course draft bắt buộc `TRUE`, `FALSE`, `NOT_GIVEN` (`CourseDraftCodec`) và `TrueFalseNotGivenTraps` chỉ nhận các khóa đó để tự gắn distractor lỗi bẫy. Với loại `TRUE_FALSE_NOT_GIVEN`, tự đặt 3 phương án chuẩn; cột `answer` nhận `TRUE`, `FALSE` hoặc `NOT_GIVEN`; cập nhật dòng mô tả cột trong template và hợp đồng.
- Gói luyện tập nhiều kỹ năng: kỹ năng của section lấy từ câu hỏi đầu tiên (`skillOf`). Báo cảnh báo (hoặc lỗi, chốt khi làm) khi các câu trong cùng một gói có kỹ năng khác nhau.
- Test: câu hỏi lỗi mà block tham chiếu không sinh thêm `UNKNOWN_QUESTION_REF`; mã trùng và lỗi dòng cùng xuất hiện trong một lần Kiểm tra; câu T/F/NG nhập qua Excel có khóa chuẩn và nhận distractor lỗi bẫy; gói lẫn kỹ năng bị báo.

### 2. Mã theo Thiết lập mã
- Hiện import gọi thẳng `createCourse/createTopic/lessons.create/createPackage` nên bỏ qua `CodeSettingsUseCases`. Luồng duyệt course draft dùng chung `CourseMaterializer` nên cùng điều này. Đã chốt làm cùng đợt: đưa việc áp Thiết lập mã vào `CourseMaterializer` (hoặc qua `CodeSettingsUseCases` mà nó gọi) để cả hai luồng tuân theo.
- Rủi ro: course draft được AI và người duyệt dùng; thay đổi ở `CourseMaterializer` phải chạy lại test của course draft (`ApproveCourseDraftUseCase` và test liên quan) trước khi coi là xong.
- Tải template: điền sẵn mã tiếp theo ở dòng dữ liệu đầu của mỗi trang tính (khóa học, topic, bài học, gói); dòng mô tả cột ghi mẫu mã và số tiếp theo. Dùng cùng logic gợi ý mã của `CodeSettingsUseCases.suggest`, chỉ đọc số, không cấp số.
- Kiểm tra: mã khác mẫu khi cấu hình đang khóa (`applied` và không `editable`) thì báo lỗi; nếu cho sửa (`editable`) thì chấp nhận im lặng, không cảnh báo. Mã trùng vẫn là `CODE_CONFLICT`.
- Cập nhật `docs/contracts/course-import-v1.md` (và `course-drafts-v1.md` nếu hành vi duyệt course draft đổi) trong cùng PR.
- Test: template tải về có mã tiếp theo; sửa mã khi khóa bị báo; sửa mã khi cho sửa vẫn qua; duyệt course draft khi cấu hình khóa không tạo mã lách cấu hình.

### 3. Template sinh động
- Template hiện là file tĩnh `src/main/resources/import/course-import-template-v1.xlsx`. Chuyển sang dựng khi tải: lấy file gốc rồi chèn mã có sẵn (phase 2) và hai trang tính chỉ-đọc `KnowledgePoints` (mã, tên, kỹ năng, cấp độ, trạng thái hoạt động) và `ErrorTypes` (mã, tên, nhóm), chỉ các mục đang hoạt động.
- Trang tính chỉ-đọc không được đọc lại như dữ liệu: `PoiCourseWorkbookReader` đã chỉ đọc các trang tính khai báo trong `CourseImportTemplate.SHEETS`, giữ nguyên.
- Không đổi `templateVersion` nếu không đổi cột. Cập nhật `ImportTemplateFileTest` vì không còn so sánh với file tĩnh.

### 4. Frontend trang import
- Nút tra cứu KP: tái dùng `CourseKnowledgeDialog`; thêm tra cứu mã lỗi.
- Màn kết quả: hiện số topic, bài, câu, gói đã tạo; link gói bài dùng tên gói thay vì UUID.
- Danh sách việc còn lại sau import (đã đồng ý giữ các việc tải file ở trình duyệt): kích hoạt khóa học và topic; xuất bản bài học, câu hỏi, gói; thêm từ vựng, ảnh, audio, tài liệu cho từng bài; thêm mô tả, ảnh bìa, banner, điểm mở khóa cho khóa học; chọn nguồn đề cho gói mốc practice test.
- Bỏ dòng `issue.message` tiếng Anh dưới nhãn tiếng Việt (không đổi hợp đồng); bỏ câu "Dòng 0 là vấn đề của cả file"; nêu lý do khi nút Import bị khóa.
- Tuân thủ `.sdd/constraints/admin-ui.md`; chạy `npm run lint`, `npm run typecheck`, test của `course-import`.

### 5. Gateway
- Thêm route riêng cho `/api/content/admin/course-import/**` vào tier `upload` trong `config-repo/api-gateway.yaml`; kiểm tra route không làm đổi `AuthRouteConsistencyTest`.

### 6. Nhãn từ vựng (trình soạn bài, ngoài import)
- `VocabularySensePicker`: khi gõ trùng khớp một từ đã có thì hiện "Đã có trong thư viện" và ẩn nút tạo; từ vừa tạo trong phiên gắn nhãn "Mới tạo" trong danh sách đã chọn.

## Tiến độ triển khai
- [x] Validator: chặn lỗi dây chuyền, báo lỗi cùng lượt, chuẩn hóa T/F/NG và từ chối gói trộn kỹ năng.
- [x] Mã: sinh gợi ý chỉ-đọc theo Thiết lập mã, kiểm tra mã khóa khi import và dùng cùng luồng code-setting cho course draft.
- [x] Template: thêm mã gợi ý cùng KnowledgePoints và ErrorTypes đang hoạt động/được duyệt.
- [x] Frontend import: tra cứu, kết quả theo tên gói, checklist việc còn lại và thông báo tiếng Việt.
- [x] Gateway: route course-import dùng tier `upload`.
- [x] Từ vựng: phân biệt mục đã có trong thư viện với mục vừa tạo.

Kiểm tra: frontend tests (18), lint và typecheck đều đạt; backend validator, template, controller, code settings và materializer tests đạt; `CourseImportIntegrationTest` (8) và `ApproveCourseDraftIntegrationTest` (14) đạt; `AuthRouteConsistencyTest` (2) đạt. `ImportTemplateFileTest` có một test xuất file tĩnh được skip theo cấu hình hiện tại.

## Acceptance criteria
- Template tải về có mã tiếp theo, có trang KnowledgePoints và ErrorTypes.
- Một lần Kiểm tra báo cả lỗi dòng lẫn mã trùng; không có `UNKNOWN_QUESTION_REF` sai.
- Sửa mã khi cấu hình khóa bị báo ở Kiểm tra; import không thể tạo mã lách cấu hình.
- Màn kết quả không còn UUID; trang import không còn câu tiếng Anh.
- Test BE và FE của course-import pass; hợp đồng `course-import-v1.md` khớp hành vi.

## Ghi nhận, chưa làm
- Import ghi từng câu hỏi trong một transaction (nhiều truy vấn mỗi câu): giới hạn 2000 dòng, chưa đo, để sau nếu có file lớn thật.
- Hai lần import trùng cùng lúc có thể ra lỗi 500 do ràng buộc unique: xác suất thấp, FE đã khóa nút.
- Câu essay (Writing/Speaking) và chỗ gắn đoạn văn Reading/audio Listening chưa được import hỗ trợ: cần quyết định sản phẩm riêng.
- Import không làm lại hay hoàn tác được: chạy lại cùng file báo `CODE_CONFLICT`; sai nội dung phát hiện sau import thì sửa trong trình duyệt hoặc xóa tay. Giữ nguyên cho đợt này, ghi rõ trên trang import. Hướng sau: gắn mã lô import để lọc và xóa theo lô, hoặc cho xem trước nội dung.
- Validator cho nhiều `TOPIC_TEST` cùng một topic và nhiều mốc cùng `afterTopicSequence`; DB chỉ có index, chưa thấy ràng buộc unique. Cần kiểm tra learning-service xử lý ra sao trước khi quyết định chặn.
- Chỉ hỗ trợ trắc nghiệm một đáp án; dạng chọn nhiều đáp án chưa nhập được qua Excel. Ghi vào tài liệu hợp đồng.

## Câu hỏi còn mở
Không còn. Ba câu về số dòng điền sẵn, mã sửa được và course draft đã được chốt ở bảng quyết định.
