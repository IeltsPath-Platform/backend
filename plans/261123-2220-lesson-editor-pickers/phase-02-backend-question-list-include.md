# Phase 02: Backend, thêm đề, Knowledge point và trạng thái "đã có chủ" vào danh sách câu hỏi

Plan: [plan.md](plan.md) · Trạng thái: done · Có thể làm song song với phase 01

Việc: **1** (đánh dấu và lọc câu đã có chủ) và **3** (đề rút gọn và Knowledge point), phía dữ liệu.

## Context

- `backend/services/content-service/src/main/java/com/ieltspath/content/api/controller/QuestionController.java` (`listQuestions`, dòng ~39)
- `.../application/usecase/ListQuestionsUseCase.java` (trần `MAX_PAGE_SIZE = 100`)
- `.../domain/repository/QuestionRepository.java` và `.../infrastructure/persistence/adapter/QuestionRepositoryAdapter.java` (`findPage`)
- `.../api/dto/response/QuestionResponse.java`, `QuestionPageResponse.java`, `.../application/result/QuestionResult.java`
- `.../domain/repository/LessonAuthoringRepository.java` và adapter: `questionVersionsOwnedElsewhere(exceptBlockId, ids)` (đã gộp `lesson_block_questions` và `section_questions`)
- `backend/docs/contracts/question-publication-v1.md` (mục `GET /api/content/admin/questions`, dòng 7-35)
- `backend/AGENTS.md` §3.3 (lỗi tiếng Việt), §3.7 (không N+1, không `findAll()`), §5 (không đổi contract âm thầm)

## Yêu cầu

Thêm ba tham số tùy chọn cho `GET /api/content/admin/questions`; thiếu cả ba thì phản hồi y như hiện tại (không thêm trường, không thêm truy vấn).

| Tham số | Ý nghĩa |
|---|---|
| `include` | Danh sách phân tách bằng dấu phẩy, giá trị `ownership` và/hoặc `preview`. Giá trị lạ trả `400` tiếng Việt |
| `exceptBlockId` | UUID của block đang sửa; chỉ có nghĩa với `ownership` và `onlyFree`. Câu thuộc chính block này không tính là đã có chủ |
| `onlyFree` | `true` thì chỉ trả câu mà phiên bản đã xuất bản **chưa** nằm ở block bài học nào (trừ `exceptBlockId`) và chưa nằm ở `section_questions`. Lọc chạy trong truy vấn phân trang, nên `totalElements` và `totalPages` đúng. Câu chưa có phiên bản xuất bản không có chủ nên không bị loại. Độc lập với `include` |

Khi `include=ownership`, mỗi item có `ownedElsewhere` (boolean): phiên bản đã xuất bản của câu nằm ở block bài học khác hoặc phần thi (`section_questions`). Câu chưa có phiên bản xuất bản thì `false`.

Khi `include=preview`, mỗi item có:
- `stemExcerpt`: đoạn đầu của đề phiên bản đã xuất bản, tối đa 160 ký tự, cắt ở ranh giới từ và thêm `…` khi bị cắt; `null` nếu không có phiên bản xuất bản.
- `knowledgePoints`: tối đa 4 mục `{ code, name }` của phiên bản đó, theo trọng số giảm dần rồi mã A→Z; kèm `knowledgePointTotal` nếu có hơn 4.

## Thiết kế

- Tính theo **tập**: gom `currentPublishedVersionId` của trang (tối đa 100) rồi chạy **một** truy vấn cho sở hữu, **một** cho đề, **một** cho Knowledge point; ghép bằng `Map`. Không gọi repository trong vòng lặp.
- Tái dùng `questionVersionsOwnedElsewhere` cho sở hữu (đã đúng luật thật khi Lưu); thêm cổng đọc ở `LessonAuthoringRepository` hoặc `QuestionRepository` theo ranh giới sẵn có, không để use case chạm JPA.
- `QuestionResult`/`QuestionResponse`: thêm trường nullable; `from(...)` cũ giữ nguyên cho nơi khác. Trường mới chỉ được serialize khi có giá trị (hoặc luôn có với `null`, theo cách `QuestionResponse` đang làm; kiểm tra ảnh hưởng tới schema zod `questionSchema` ở frontend, vốn dùng `nullish`).
- Không đổi `ListQuestionsUseCase.execute(...)` đã có; thêm một phương thức làm giàu riêng để `QuestionBankPage` và các nơi gọi khác không tốn thêm truy vấn.
- Không log đề câu hỏi.
- **`onlyFree`** nằm trong truy vấn phân trang, không lọc sau khi lấy trang: thêm điều kiện `NOT EXISTS` trên `lesson_block_questions` (loại trừ `exceptBlockId` bằng `block_id <> :except`, trường hợp null phải tách riêng vì `<> NULL` không bao giờ đúng, giống cách `questionVersionsOwnedElsewhere` đang làm) và `NOT EXISTS` trên `section_questions`, cùng khóa là `current_published_version_id` của câu. Mở rộng `QuestionJpaRepository.findPage`/`QuestionRepository.findPage` bằng tham số tùy chọn, không làm đổi chữ ký các lời gọi cũ (thêm phương thức mới hoặc tham số mặc định).
- Hai nơi định nghĩa "đã có chủ" (cờ `ownedElsewhere` và bộ lọc `onlyFree`) phải dùng **cùng một luật**; đặt luật ở một chỗ (một đoạn SQL hoặc một cổng) để chúng không lệch nhau, và có test chung.

## Files sửa / tạo

- `QuestionController.java`: nhận `include`, `exceptBlockId`; kiểm giá trị `include`.
- `ListQuestionsUseCase.java` (hoặc use case riêng `EnrichQuestionListUseCase` nếu file phình ra), `QuestionResult.java`, `QuestionResponse.java`, `QuestionPageResponse.java`.
- Repository và adapter: truy vấn đề cắt và Knowledge point theo tập phiên bản.
- `backend/docs/contracts/question-publication-v1.md`: ghi hai tham số, ba trường mới, và câu "không có `include` thì không đổi so với trước".

## Test

- `QuestionControllerTest`: không `include` thì JSON cũ không đổi; `include` lạ trả `400` tiếng Việt; `include=ownership,preview` trả trường mới.
- Test use case (Mockito): câu có chủ ở block khác là `true`; câu thuộc `exceptBlockId` là `false`; câu ở `section_questions` là `true`; câu chưa xuất bản là `false`; đề dài bị cắt ở 160 ký tự; Knowledge point quá 4 thì có `knowledgePointTotal`.
- `onlyFree=true`: câu ở block khác và câu ở `section_questions` bị loại; câu thuộc `exceptBlockId` còn lại; `totalElements` đúng (kiểm với dữ liệu nhiều hơn một trang); kết hợp được với `skill`, `purpose`, `status`, `code`. Mỗi câu mà `ownedElsewhere` là `true` thì **không** xuất hiện khi `onlyFree=true` (test chung cho hai chỗ dùng cùng luật).
- Test Testcontainers cho adapter: một trang 25 câu chỉ chạy số truy vấn cố định (không tăng theo số dòng). Test tự bỏ qua khi không có Docker; nếu bị bỏ qua thì ghi rõ.
- ArchUnit: `mvn -q -pl services/content-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Chạy `mvn -q -pl services/content-service -am test`.

## Rủi ro và rollback

- Thêm trường và tham số tùy chọn, không đổi dữ liệu, không có migration; lùi bằng cách bỏ code mới, frontend cũ vẫn chạy vì không gửi `include`.
- Hiệu năng: ba truy vấn theo tập cho tối đa 100 phiên bản, và `onlyFree` thêm hai `NOT EXISTS` vào truy vấn phân trang chạy trên toàn bảng câu hỏi. Kiểm chỉ mục trên `lesson_block_questions(question_version_id)` và `section_questions(question_version_id)` (xem `EXPLAIN` với dữ liệu thật hoặc seed lớn); nếu thiếu thì cần migration chỉ mục: ghi vào báo cáo và **hỏi bạn trước khi thêm**.
