# Brainstorm: KP con theo mã lỗi để cá nhân hóa bài ôn

- Ngày: 2026-10-24
- Loại: brainstorm, đã validate với người dùng; chưa lập plan, chưa sửa code
- Phạm vi: câu khách quan (trắc nghiệm, T/F/NG, Y/N/NG, điền) chấm trong learning-service; dữ liệu ở content-service.
  Essay chỉ nhắc để so sánh.
- Làm **một lần**, không chia giai đoạn.
- **Plan:** `plans/261024-1720-error-trap-sub-knowledge-points/`. Khi lập plan đã chỉnh 4 điểm (người dùng xác nhận), plan là
  nguồn đúng khi lệch với báo cáo: (P1) giữ `error_types.knowledge_point_id` là KP cha, thêm `trap_knowledge_point_id` cho
  KP con, không thêm cột KP cha vào `knowledge_points`; (P2) bẫy T/F/NG ghi vào `distractors` ở content, `ErrorTagger` không
  đổi; (P3) KP con cho mọi mã ACTIVE có KP cha, cây ẩn KP con chưa có bằng chứng; (P4) nguồn `error_trap` trọng số 0.3, không
  tính vào trần 3 bài luyện.

## 1. Vấn đề

Lộ trình ôn chỉ dựa vào KP: hai học viên cùng yếu một KP nhận cùng kiểu bài ôn dù sai theo hai kiểu khác nhau. Mã lỗi
cho biết sai **theo kiểu nào** nhưng hiện chỉ được ghi lại và hiển thị.

Ràng buộc từ người dùng: một lần chọn đáp án sai A chỉ là một quan sát về mã của A. Không chọn B, C không có nghĩa đã
thành thạo mã của B, C; một lần sai có thể là đoán. Không được quyết định lộ trình từ một lần sai.

## 2. Hiện trạng (đã kiểm trong code)

| Chủ đề | Hiện trạng | Nguồn |
| --- | --- | --- |
| KP và mã lỗi | `error_types.knowledge_point_id`: một mã trỏ 0 hoặc 1 KP; một KP có nhiều mã. Danh mục (V45, V47) đang `DRAFT` (V48) | content V45, V48 |
| Tạo mã lỗi | `CHOICE`: lấy từ `answer_spec.distractors` theo đáp án đã chọn. `FILL`: luật `WORD_LIMIT_EXCEEDED`, `NUMBER_FORMAT`, `PLURAL_FORM`, `SPELLING`. Đúng: không có mã | `ErrorTagger` |
| T/F/NG | Lưu `{"type":"CHOICE","correct":"NOT_GIVEN"}`, lựa chọn cố định `TRUE`/`FALSE`/`NOT_GIVEN`; **không câu nào có `distractors`** nên chọn sai không ghi mã | content V30–V32 |
| Ghi nhận | Mỗi câu trả lời một dòng `question_attempt_events.error_code` | learning V13 |
| Dùng mã lỗi | Chỉ hiển thị: báo cáo học viên, top mã khi trượt course test (bài bổ trợ vẫn theo KP sai), thống kê từng câu cho người soạn. Essay là ngoại lệ: danh mục + `fixHint` vào prompt, mã lỗi quy về KP con | `GetLearnerReportUseCase`, `CourseOutcomeService`, `JdbcStaffInsightRepository`, `EssayGrader`, `EssayKnowledgeJudge` |
| `top_error_codes` | Đếm thô `count(*)` mỗi mã theo KP trong 90 ngày, **không có mẫu số** (số lần gặp bẫy) | `MasteryProfileUpdater`, `JdbcQuestionEventLog` |
| Phản hồi khi sai | Không trả mã lỗi hay `fixHint` cho câu khách quan | `SubmitPracticeAttemptUseCase` |
| Câu ↔ KP | `question_knowledge_points (question_version_id, knowledge_point_id, weight)`; một câu nhiều KP | content V1 |
| Bằng chứng | `kp_evidence.source` có `CHECK` liệt kê nguồn; trọng số theo nguồn ở `Mp1Params.SourceWeights` | learning V1, V24 |
| Chọn bộ ôn | Một bộ thuộc KP khi có ≥ 3 câu gắn KP; loại bộ đã giao hoặc đã lộ đáp án; thứ tự: thuộc lesson đã dạy KP → khớp KP nhiều nhất → mã bộ; lấy bộ đầu. Không đọc mã lỗi | `JdbcLearningContentReader.searchPracticeSets`, `GetReviewUseCase.nextPackage` |
| Giỏ ôn | Bắt đầu bài ôn đòi lesson đã hoàn thành dạy KP (`409 NO_LESSON_FOR_KNOWLEDGE_POINT`); từ chối KP mức `PRIORITY`, `INSUFFICIENT` | `StartBasketReviewUseCase` |
| Board Figma | Có Error Taxonomy, event log có mã lỗi, "bổ trợ theo nhóm lỗi"; chưa mô tả cách gắn mã cho đáp án nhiễu hay luật bổ trợ theo lỗi | board "Cây tri thức - đánh giá" |

## 3. Phương án đã xét

| Phương án | Kết luận |
| --- | --- |
| Chỉ KP (giữ nguyên) | Không cá nhân hóa được trong cùng KP |
| Lớp "thói quen" riêng (đếm cơ hội, bật/tắt cờ) | **Bỏ**: viết lại mastery lần hai, hai nơi quyết định lộ trình |
| Tách KP gắn thẳng câu vào KP con | **Bỏ**: chọn bẫy khác cũng thành ✗ cho KP con, sai ràng buộc ở mục 1 |
| Chỉ chọn bộ theo bẫy trong cùng KP | Tốt nhưng không bắt được lỗi xuyên KP, cần thống kê lỗi riêng |
| **KP con theo mã lỗi + bằng chứng theo đáp án đã chọn + chọn bộ theo bẫy** | **Chọn**: một cơ chế, dùng lại MP-1.0, giỏ ôn và bộ luyện sẵn có |

## 4. Thiết kế đã chốt

### 4.1 Nguyên tắc

- **KP quyết định học gì, KP con theo mã lỗi quyết định học theo cách nào.** Mọi quyết định vẫn đi qua mastery theo KP.
- Mã lỗi không có luật riêng: nó tạo bằng chứng cho KP con, rồi MP-1.0 lo cửa sổ, giảm theo thời gian, ngưỡng đủ dữ liệu.
- Không có lớp thói quen, không có ngưỡng tỷ lệ riêng.

### 4.2 Nguồn mã lỗi

| Dạng câu | Câu chứa bẫy X khi | Mã khi học viên chọn sai |
| --- | --- | --- |
| A/B/C/D | `answer_spec.distractors` có đáp án mang mã X | Mã của đáp án đã chọn |
| T/F/NG, Y/N/NG | Suy ra từ đáp án đúng (thêm luật vào `ErrorTagger`) | đúng `NOT_GIVEN`, chọn `FALSE`/`NO` → `TFNG_NG_AS_FALSE`; đúng `FALSE`/`NO`, chọn `NOT_GIVEN` → `TFNG_FALSE_AS_NG`; đúng `TRUE`/`YES` → chưa có mã |
| Điền | Không xác định trước | Luật hiện có của `ErrorTagger` |

### 4.3 KP con theo mã lỗi

- Mỗi mã lỗi **đang ACTIVE (kể cả `DRAFT`)** có KP cha tương ứng thành **một KP con** dưới KP cha đó; KP con từ mã `DRAFT`
  được đánh dấu chờ chuyên gia duyệt (như bộ luyện Writing demo V70), duyệt xong không cần đổi code. Ví dụ dưới `TFNG_FALSE_VS_NOT_GIVEN`:
  KP con "không chọn False khi bài không nói tới" (`TFNG_NG_AS_FALSE`) và "không chọn Not Given khi bài đã nói ngược lại"
  (`TFNG_FALSE_AS_NG`); dưới `TR_R_SCANNING`: KP con "không chọn vì trùng từ khóa" (`KEYWORD_MATCH_ONLY`).
- `error_types.knowledge_point_id` trỏ về KP con; KP con trỏ về KP cha.
- Mã không có KP cha: không có KP con, chỉ dùng cho phản hồi và thống kê.
- KP con **không gắn câu** trong `question_knowledge_points` (lý do ở mục 3).
- KP con hiện dưới KP cha trong cây, báo cáo, giỏ ôn (giống KP con Writing).

### 4.4 Bằng chứng cho KP con (mã lỗi đổ về KP)

Với mỗi câu trả lời, với **mỗi** bẫy X có trong câu:

| Học viên chọn | KP của câu | KP con X |
| --- | --- | --- |
| Đáp án đúng | ✓ | ✓ (đã cưỡng lại bẫy X) |
| Bẫy X | ✗ | ✗ |
| Bẫy khác hoặc đáp án sai không mã | ✗ | **không ghi** |

- Trọng số bằng chứng KP con = **một nửa** bằng chứng chính cùng nguồn (nguồn mới, cấu hình được trong `learning.mastery.*`).
- KP của câu giữ nguyên cách ghi hiện tại.
- Áp dụng cho câu chấm trong learning-service (bài tập lesson, Practice, bài ôn, luyện tự do, chẩn đoán).

Ví dụ một học viên gặp 5 câu có bẫy X: chọn bẫy X, đúng, chọn bẫy X, chọn bẫy khác, đúng → KP con X nhận 4 bằng chứng
(✗ ✓ ✗ ✓), không phải 5.

### 4.5 Lỗi chính

**Lỗi chính** của học viên ở một KP cha = **KP con yếu nhất đã đủ dữ liệu** theo MP-1.0 (≥ 3 bằng chứng trên ≥ 2 ngày).
Chưa có KP con nào đủ dữ liệu → không có lỗi chính.

### 4.6 Chọn bộ ôn

**"Câu chứa bẫy X"** theo mục 4.2. Số câu bẫy của một bộ đếm trên phiên bản đã xuất bản của bộ.

**Ôn KP cha** (bài ôn chặn và giỏ ôn):
1. Lọc như hiện tại: ≥ 3 câu gắn KP, chưa giao, chưa lộ đáp án.
2. Có lỗi chính X và có bộ ≥ 2 câu chứa bẫy X → xếp: số câu bẫy X (nhiều trước) → tỷ lệ câu bẫy X ÷ tổng câu (cao trước)
   → thứ tự hiện tại (thuộc lesson đã dạy KP → khớp KP nhiều hơn → mã bộ).
3. Ngược lại → thứ tự hiện tại.
4. Lấy bộ đầu; trượt thì lấy bộ kế tiếp; hết bộ → `SKIPPED`.

**Một bài ôn cho cả nhóm KP cha + KP con:**
- KP cha đang có bài ôn chờ (chặn hoặc tùy chọn) → không tạo bài ôn KP con; KP con không hiện riêng trong giỏ ôn (như luật
  hiện có: KP có bài ôn chặn không hiện thêm ở nhóm tùy chọn). Bài ôn KP cha đã chọn bộ theo bẫy của KP con yếu nhất; câu
  bẫy trong bộ vẫn ghi bằng chứng KP con.
- KP cha không có bài ôn mà KP con yếu ("KP cha ổn, KP con yếu") → một bài ôn tùy chọn cho KP con.

**Ôn KP con X** (chỉ khi KP cha không có bài ôn chờ):
1. **Tạo lúc chấm** (theo mẫu `WritingCriterionReviews`) → bài ôn tùy chọn, trigger mới (ví dụ `ERROR_PATTERN`), không
   chặn. Xét cho mỗi KP con **vừa nhận bằng chứng mới** trong lần nộp (học viên vừa làm câu có bẫy của nó), tạo khi đủ:
   (a) KP con **đang ở** `PRIORITY` (không đòi "vừa rơi" xuống: điều kiện "vừa rơi" chỉ xét một lần, bỏ lỡ là kẹt mãi);
   (b) KP cha và KP con chưa có bài ôn chờ; (c) còn bộ khả dụng (≥ 3 câu chứa bẫy X, chưa giao, chưa lộ đáp án);
   (d) không vượt giới hạn mỗi lần nộp (cấu hình, ví dụ 1–2). Mỗi lần gặp lại câu có bẫy là một lần xét lại; không cần
   job nền. KP con tụt `PRIORITY` chỉ do giảm theo thời gian thì chờ lần gặp bẫy kế tiếp. Hiện ở nhóm `weak` của giỏ ôn. Không đổi
   luật giỏ ôn chung. Lý do: giỏ ôn chỉ tự mở bài cho `CONSOLIDATE`/`MAINTAIN` đến hạn; `PRIORITY` chỉ hiện khi đã có bài ôn
   tùy chọn, và KP con không gắn câu nên không bao giờ có bài ôn chặn.
2. KP con `CONSOLIDATE` hoặc `MAINTAIN` đến hạn: vào giỏ ôn như KP thường.
3. **Lesson** (bài ôn cần lesson để quay lại lý thuyết, xác định kỹ năng, ưu tiên bộ): tạo lúc chấm → lesson của bài vừa
   làm; bấm từ giỏ ôn hoặc bài không có lesson → lesson đã hoàn thành dạy **KP cha** (`CompletedLessonLocator.teaching`
   tra theo KP cha khi KP là KP con). Chưa học lesson nào của KP cha → không tạo bài ôn. Tránh
   `409 NO_LESSON_FOR_KNOWLEDGE_POINT`.
4. **Bộ:** ≥ 3 câu chứa bẫy X, chưa giao, chưa lộ đáp án; xếp số câu bẫy → tỷ lệ → thuộc lesson của KP cha → mã bộ.
   Dùng lại bộ có sẵn, không soạn bộ mới.
5. KP con không bao giờ chặn.
6. **Lý thuyết nhắm đúng bẫy:** lesson của KP cha chỉ cho lý thuyết nền và có thể không nói về bẫy (ví dụ
   `KEYWORD_MATCH_ONLY` → `TR_R_SCANNING`), lại dạy nhiều KP nên quay lại lý thuyết không nhắm đúng. Bù bằng cách trả
   **tên lỗi + `fixHint`** của mã lỗi khi mở bài ôn KP con và khi quay lại lý thuyết ở lần trượt đầu. Không soạn lý
   thuyết riêng cho từng KP con; chỉ tính tới khi `fixHint` tỏ ra chưa đủ.

Luyện tự do theo KP giữ nguyên cách chọn bộ.

Không ghép câu lẻ từ nhiều bộ: bài ôn giao theo bộ và luật "không giao lại bộ đã lộ đáp án" tính theo bộ.

### 4.7 Phản hồi và hiển thị

- Chọn đáp án sai có mã → trả thêm tên lỗi và `fixHint` (một lần là đủ, chỉ nói về câu vừa làm).
- Bài ôn KP con → trả tên lỗi và `fixHint` khi mở bài và khi quay lại lý thuyết (mục 4.6 bước 6).
- Báo cáo và thống kê dùng mastery KP con thay cho `top_error_codes` đếm thô (bỏ thống kê không có mẫu số).
- Mastery KP con không ảnh hưởng mở bài, điểm khóa học hay cổng Practice.

### 4.8 Phạm vi và quyết định bổ sung

- **Danh mục KP của học viên:** KP con phải có trong `knowledge_point_catalog` (learning lấy từ content qua
  `RefreshLearningTopicsUseCase`), nếu không thì không có mastery và không vào bài ôn. Content trả KP con trong danh mục với
  topic của KP cha; `has_practice_set` của KP con tính theo luật "≥ 3 câu chứa bẫy".
- **Bài thi:** không ghi bằng chứng KP con. Chỉ áp cho câu chấm trong learning (bài tập lesson, Practice, bài ôn, luyện tự
  do, chẩn đoán). Không đổi event `AssessmentCompleted.v2`.
- **KP con:** không có quan hệ tiên quyết, không tham gia chẩn đoán dò ngược.
- **Câu đúng `TRUE`/`YES` chọn sai:** chưa có mã lỗi.
- **Frontend trong phạm vi:** `fixHint` khi chọn sai và khi mở bài ôn KP con; KP con dưới KP cha trong cây kiến thức và báo
  cáo; bài ôn `ERROR_PATTERN` trong giỏ ôn. Trường mới tương thích ngược.
- Làm **một lần**; plan chia phase theo phụ thuộc kỹ thuật (đo dữ liệu → DB → content → learning → contract → frontend), không
  phải chia tính năng làm trước/sau.

## 5. Ví dụ Lan và Minh

Cả hai yếu KP `TFNG_FALSE_VS_NOT_GIVEN`. Lan hay chọn False ở câu đúng là Not Given; Minh hay chọn Not Given ở câu đúng
là False.

| | Lan | Minh |
| --- | --- | --- |
| Câu đúng NG, chọn False | KP cha ✗, KP con NG_AS_FALSE ✗ | — |
| Câu đúng False, chọn NG | — | KP cha ✗, KP con FALSE_AS_NG ✗ |
| Câu đúng NG, làm đúng | KP cha ✓, KP con NG_AS_FALSE ✓ | KP cha ✓, KP con NG_AS_FALSE ✓ |
| Sau vài ngày | KP con NG_AS_FALSE yếu nhất, đủ dữ liệu → lỗi chính | KP con FALSE_AS_NG yếu nhất → lỗi chính |
| Bài ôn KP cha | Bộ nhiều câu đúng là NG (bẫy False) trước | Bộ nhiều câu đúng là False (bẫy NG) trước |
| Giỏ ôn | Không có bài ôn KP con riêng (KP cha đã có bài ôn nhắm đúng bẫy) | Như Lan |
| Nếu KP cha đã ổn (≥ 80%) mà KP con vẫn yếu | Bài ôn tùy chọn cho KP con NG_AS_FALSE | Bài ôn tùy chọn cho KP con FALSE_AS_NG |

Bộ giả định cùng KP: PS-A (4 câu NG, 1 câu False), PS-B (1 NG, 4 False), PS-C (thuộc lesson; 2 NG, 3 False). Hiện tại
cả hai nhận PS-C. Thiết kế mới: Lan nhận PS-A → PS-C → PS-B; Minh nhận PS-B → PS-C → PS-A.

## 6. Thay đổi cần có

| Chỗ | Thay đổi | Loại |
| --- | --- | --- |
| Content DB | Cột KP cha cho KP con (ví dụ `knowledge_points.parent_knowledge_point_id`, nullable) | **Schema** |
| Content DB | Tạo KP con từ mã lỗi đã duyệt; trỏ `error_types.knowledge_point_id` về KP con | Dữ liệu |
| Learning DB | Thêm nguồn bằng chứng KP con vào `CHECK` của `kp_evidence.source` (cách V24 đã làm) | **Schema** |
| Learning | `ErrorTagger`: luật T/F/NG, Y/N/NG | Code |
| Learning | Ghi bằng chứng KP con theo bảng 4.4, trọng số 0.5 (`EvidenceSource`, `Mp1Params`, config) | Code |
| Learning | Lỗi chính = KP con yếu nhất; truyền khi tìm bộ ôn KP cha | Code |
| Learning | Lúc chấm: tạo bài ôn tùy chọn `ERROR_PATTERN` cho KP con vừa rơi `PRIORITY` khi KP cha không có bài ôn chờ; cấu hình giới hạn số bài | Code + config |
| Learning | Giỏ ôn: không hiện/không tạo bài ôn KP con khi KP cha có bài ôn chờ | Code |
| Learning | Tìm lesson cho KP con theo KP cha (`CompletedLessonLocator`) | Code |
| Learning | Bài ôn KP con trả tên lỗi + `fixHint` khi mở bài và khi quay lại lý thuyết | Code + contract `lesson-learning-v1` |
| API danh mục KP nội bộ (content → learning) | Thêm trường KP cha; trả KP con trong danh mục topic với `has_practice_set` theo câu bẫy | Contract nội bộ |
| Frontend | `fixHint` khi sai và khi mở bài ôn KP con; KP con trong cây và báo cáo; bài ôn `ERROR_PATTERN` trong giỏ ôn | Code |
| Learning | Trả tên lỗi + `fixHint` khi chọn sai có mã; báo cáo dùng KP con | Code + contract |
| Content | `searchPracticeSets` và `practiceSetAvailability`: tham số tùy chọn mã lỗi; đếm câu bẫy; chế độ đủ điều kiện "≥ 3 câu bẫy" cho KP con | Code |
| Contract | `learning-content-internal-v1`, `lesson-learning-v1`, `answer-spec-v1` (luật T/F/NG), báo cáo | Tài liệu |
| Nội dung | `distractors` cho câu A/B/C/D; duyệt danh mục mã lỗi | Dữ liệu |

## 7. Rủi ro và kiểm chứng

| Rủi ro | Giảm thiểu |
| --- | --- |
| Danh mục mã lỗi đang `DRAFT` | Cho phép tạo KP con từ mã `DRAFT`, đánh dấu chờ duyệt; chuyên gia duyệt song song |
| Gắn mã sai dẫn học viên sai hướng | Bắt đầu từ T/F/NG suy ra bằng luật; duyệt `distractors` |
| KP con thiếu bộ có ≥ 3 câu bẫy | Đo trước; KP con không đủ bộ thì không vào giỏ ôn |
| Một câu ảnh hưởng hai KP | Trọng số 0.5; KP con không chặn |
| Bài ôn trùng KP cha và KP con, tiêu nhanh bộ luyện | Một bài ôn cho cả nhóm: KP cha có bài ôn thì không tạo bài ôn KP con |
| Lý thuyết lesson KP cha không nói về bẫy của KP con | Trả tên lỗi + `fixHint` khi mở bài ôn KP con và khi quay lại lý thuyết; chưa đủ mới soạn lý thuyết ngắn theo mã lỗi |
| Cây KP và giỏ ôn đông hơn | KP con hiện dưới KP cha; chỉ vào giỏ ôn khi đủ dữ liệu |
| Đáp án nhiễu quá mạnh (đa số học viên đều chọn) | Thống kê từng câu cho người soạn; xem xét loại khỏi bằng chứng KP con |

Kiểm chứng ngoại tuyến: cắt dữ liệu mỗi học viên làm hai nửa theo thời gian; tính KP con trên nửa đầu; kiểm nửa sau người
có KP con X yếu có mắc bẫy X nhiều hơn rõ rệt người khác không.

Chỉ số: độ phủ câu có bẫy theo KP; độ chính xác dự đoán của KP con yếu; tốc độ giảm tỷ lệ mắc bẫy sau bài ôn theo bẫy so
với bài ôn thường.

## 8. Câu hỏi còn mở

- `fixHint` hiện là bản nháp chờ duyệt cùng danh mục; nó gánh phần lý thuyết nhắm bẫy nên cần duyệt kỹ (rõ, ngắn, đúng).
- Sửa dữ liệu KP trên câu đã xuất bản: thiết kế không gắn câu vào KP con nên tránh được, cần xác nhận khi lập plan.
- Chưa xác minh: thứ tự `searchPracticeSets` với nhiều bộ cùng hạng; tổng số câu có bẫy theo KP (mới đếm nhanh T/F/NG:
  khoảng 13 câu đúng `NOT_GIVEN`, 8 câu đúng `FALSE`).

## Các quyết định đã chốt trong plan (2026-10-25)

Plan đã điều chỉnh và người dùng chốt các nội dung sau; đây là quyết định triển khai cuối cùng thay cho phương án brainstorm
nếu khác nhau:

- **P1:** giữ `error_types.knowledge_point_id` là KP cha để Writing và author workspace không đổi; thêm
  `trap_knowledge_point_id` làm KP con duy nhất. KP cha của child được suy ra từ error type.
- **P2:** `answer_spec.distractors` là nguồn gắn mã bẫy và đếm câu cho practice set; không thêm luật T/F/NG thứ hai vào
  `ErrorTagger`. Hai bẫy T/F/NG được tự điền khi cả hai mã đang ACTIVE.
- **P3:** đồng bộ một KP con cho mọi mã ACTIVE có KP cha, kể cả DRAFT có cờ chờ duyệt; report/tree chỉ lồng child khi
  người học đã có bằng chứng.
- **P4:** nguồn mastery riêng `error_trap`, trọng số 0.3, ngoài trần ba bài luyện MP-1.0.
- Đơn vị ôn luôn là KP cha. Chọn bộ chỉ cần ít nhất hai câu có mã bẫy của lỗi chính; lỗi chính được chọn từ child yếu
  nhất đủ dữ liệu. Nếu child vừa vào PRIORITY khi cha không có review pending, Learning có thể tạo một review tùy chọn
  `ERROR_PATTERN` cho KP cha. Child không có review riêng, không vào giỏ như một item riêng và không được đưa vào các
  phép tổng hợp KP cha.
