# Gói duyệt: vi phạm ArchUnit và mã lỗi Writing mới

Viết cho: người phụ trách kiến trúc (phần 1) và examiner/chuyên gia IELTS (phần 2). Không chứa essay, dữ liệu học viên hay secret.

Trạng thái: chờ duyệt. Không có gì được merge hay bật trước khi có quyết định ghi ở cuối file.

---

## Phần 1. 38 vi phạm ArchUnit đã chốt làm baseline

Các vi phạm này đã được đóng băng vào `src/test/resources/archunit_store` của từng service. CI chỉ đỏ khi có vi phạm **mới**. Vi phạm cũ không tự mất; muốn xóa phải sửa code rồi thu hẹp store.

Đây là câu hỏi kiến trúc, không phải câu hỏi nghiệp vụ IELTS. Người duyệt nên là người phụ trách backend.

### Hai kiểu vi phạm

| Kiểu | Service | Số lượng | Vị trí điển hình |
| --- | --- | --- | --- |
| A. `application` dùng class cấu hình nằm trong `infrastructure.config` | content (30), assessment (3 trên 4) | 33 | Constructor, field, và lời gọi getter (`getAi()`, `getModel()`...) |
| B. `application` gọi helper nằm trong `infrastructure.client` | assessment (1 trên 4) | 1 | `AiEvaluationService.start` gọi `RubricPromptBuilder.promptVersion` |
| C. `domain` dùng class của `application` | learning (4) | 4 | `MainErrorRule.mainErrorCode` dùng `ErrorTrapCatalog.ErrorTraps` |

Tổng: 38. Ghi chú: kiểu A chiếm đa số. Trong content, `ContentAiProperties` xuất hiện ở sáu class application (constructor, field, và các lời gọi getter). Trong assessment, `AssessmentLlmProperties` nằm trong constructor, field và một lời gọi getter của `AiEvaluationService`.

### Phương án xử lý

| Kiểu | Phương án | Việc phải làm | Rủi ro | Đề xuất |
| --- | --- | --- | --- | --- |
| A | A1. Chuyển class `*Properties` sang `application.config`; `infrastructure` vẫn bind cấu hình | Di chuyển package; sửa import; test bind cấu hình | Thấp: không đổi hành vi | **Chọn** |
| A | A2. Giữ baseline, ghi nợ kỹ thuật | Không làm gì | Thấp trước mắt, nợ tăng dần | Chỉ nếu có deadline |
| B | B1. Đưa `promptVersion` vào `domain` hoặc `application`, để `infrastructure` gọi ngược | Di chuyển một hàm thuần | Thấp | **Chọn** |
| C | C1. Chuyển record `ErrorTraps` sang `domain.vo` | Di chuyển record và các hàm đọc nó | Trung bình: `ErrorTrapCatalog` cũng đổi | **Chọn** |
| C | C2. Giữ baseline | Không làm gì | Thấp | Chấp nhận nếu chưa đụng error trap |

### Câu hỏi cần trả lời

| # | Câu hỏi | Trả lời (Có / Không / Sửa sau) | Người duyệt |
| --- | --- | --- | --- |
| 1 | Chọn A1 cho `ContentAiProperties` và `AssessmentLlmProperties`? | | |
| 2 | Chọn B1 cho `promptVersion`? | | |
| 3 | Chọn C1 cho `ErrorTraps` (ảnh hưởng luồng error trap)? | | |
| 4 | Chấp nhận baseline hiện tại cho đến khi làm các việc trên? | | |

Lưu ý khi duyệt: các phương án trên không đổi hành vi chấm bài, không đổi contract API, không đổi bảng dữ liệu.

---

## Phần 2. Mã lỗi Writing mới (phục vụ phase Dev A)

Hiện có: `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT`, `PLURAL_FORM` (thuộc `SPELLING_NUMBER`), `LINKER_MISUSE`, `NO_CLEAR_POSITION`, `UNDERDEVELOPED_IDEA`, `OFF_TOPIC`, `WRONG_PARAGRAPH`, `T1_NO_OVERVIEW`, `T1_DATA_MISREPORTED`.

Cần thêm, do chuyên gia đề xuất đầy đủ mã, tên, quy tắc ngắn, mức nghiêm trọng, KP và ví dụ:

| Nhóm dự kiến | Ứng viên mã | Ghi chú |
| --- | --- | --- |
| Ngữ pháp | dạng động từ sau modal (`should going`); thiếu động từ chính (`they was go`); giới từ; danh từ đếm được và không đếm được (`every years`); cấu trúc so sánh | Lấy từ lỗi thật của bài 24 lỗi của examiner |
| Task 1 | ý kiến cá nhân trong bài mô tả biểu đồ | Ứng viên mới |
| Task 2 | `T2_PARTIAL_ANSWER` (trả lời thiếu một phần đề); `T2_NO_CONCLUSION` (thiếu kết bài); `T2_IRRELEVANT_EXAMPLE` (ví dụ không liên quan hoặc khái quát quá mức) | Chờ duyệt |
| Dự phòng | `GRAMMAR_OTHER`, `VOCABULARY_OTHER`, `COHERENCE_OTHER`, `TASK_OTHER` | Không gắn KP; không tạo bài ôn |

Lưu ý: chỉ mã được duyệt mới được seed vào DB. Mã chưa duyệt không được dùng.

### Bảng điền cho chuyên gia

| Mã đề xuất | Tên tiếng Việt | Nhóm | Mức nghiêm trọng (1 đến 3) | Quy tắc ngắn (một câu, hiển thị cho học viên) | KP gắn (hoặc để trống) | Ví dụ sai → đúng (không lấy từ bài của học viên) | Duyệt (Có / Không / Sửa) | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | | |
| | | | | | | | | |
| | | | | | | | | |
| | | | | | | | | |
| | | | | | | | | |

Gợi ý điền:
- **Mức nghiêm trọng:** 3 là lỗi làm tụt band nặng (ví dụ lạc đề, thiếu phần đề); 1 là lỗi nhỏ, ít ảnh hưởng band.
- **Quy tắc ngắn:** viết bằng tiếng Việt, thuật ngữ IELTS giữ tiếng Anh.
- **KP gắn:** để trống nếu không có bài luyện phù hợp. Mã dự phòng không gắn KP.
- **Ví dụ:** một câu ngắn, khác câu của học viên.

### Câu hỏi cần trả lời

| # | Câu hỏi | Trả lời |
| --- | --- | --- |
| 1 | Danh sách ứng viên trên có đủ không, hay cần bỏ/thêm mã? | |
| 2 | Mức nghiêm trọng của các mã Task 2 có hợp lý không? | |
| 3 | `T2_NO_CONCLUSION` có nên gắn KP `W60_...` nào không? | |
| 4 | Có cần mã riêng cho lạc đề một phần không, hay dùng `OFF_TOPIC`? | |

---

## Việc tiếp theo sau khi có quyết định

1. Người phụ trách kiến trúc điền Phần 1; nếu chọn A1, B1, C1 thì mở việc di chuyển package trên nhánh riêng và thu hẹp store bằng lệnh đã ghi trong `AGENTS.md`.
2. Examiner điền bảng Phần 2; Dev A đưa các mã được duyệt vào migration Content mới (V123 theo phân công).
3. Mã chưa duyệt giữ ở trạng thái dự phòng; không seed.
