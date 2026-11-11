# Gói duyệt v2: 38 vi phạm ArchUnit và mã lỗi Writing mới

**Nguồn:** bản review v1 (`review-pack-261111-2010-archunit-violations-and-writing-error-codes-report.md`) đã được sửa theo phản hồi. Không có tham chiếu bộ benchmark, link ngoài hay nhãn `task_response` chưa kiểm chứng.

**Trạng thái:** đề xuất để duyệt. Chưa có người nào ký. Không merge, không cập nhật store, không seed V123 trước khi có quyết định ghi ở Phần 3.

Thay đổi so với v1:
- Bỏ nhận định "chuyển `*Properties` có thể vẫn vi phạm vì `application` dùng Spring". Rule hiện tại chỉ cấm `application` → `api`/`infrastructure`.
- Bỏ mọi tham chiếu benchmark và link ngoài chưa kiểm.
- Bỏ nhãn `task_response` và ánh xạ `LANGUAGE`/`DATA`.

---

## Phần 1. 38 vi phạm ArchUnit (người duyệt: phụ trách backend)

### 1.1. Baseline
Số liệu đã đối chiếu trực tiếp với các file store trong repo (`archunit_store`) ngày 2026-11-11.

| Nhóm | Luồng phụ thuộc | Service | Số vi phạm |
|---|---|---|---:|
| A | `application` → `infrastructure.config` (`ContentAiProperties`, `AssessmentLlmProperties`) | content 30, assessment 3 | 33 |
| B | `application` → `infrastructure.client` (`AiEvaluationService.start` → `RubricPromptBuilder.promptVersion`) | assessment | 1 |
| C | `domain` → `application` (`MainErrorRule.mainErrorCode` → `ErrorTrapCatalog.ErrorTraps`) | learning | 4 |
| | **Tổng** | | **38** |

### 1.2. Đề xuất

| # | Câu hỏi | Đề xuất | Người duyệt |
|---|---|---|---|
| 1 | Chọn A1 cho `ContentAiProperties`, `AssessmentLlmProperties`? | Có | Chưa ký |
| 2 | Chọn B1 cho `promptVersion`? | Có, đặt ở `application` | Chưa ký |
| 3 | Chọn C1 cho `ErrorTraps`? | Có, kèm test hồi quy luồng error trap | Chưa ký |
| 4 | Chấp nhận baseline tạm thời? | Có, không tăng số vi phạm | Chưa ký |

**Nhóm A (A1):** chuyển `*Properties` sang `application.config`. Giữ nguyên prefix, field, default, validation. Binding vẫn do `infrastructure` đảm nhiệm. Kiểm bằng build thật: service content và assessment khởi động và bind như cũ; vi phạm nhóm A về 0; không phát sinh vi phạm mới.

**Nhóm B (B1):** nếu `promptVersion` là hàm thuần, chuyển sang `application`. Nếu version phụ thuộc template trong adapter, định nghĩa port ở `application`, `infrastructure` triển khai. Tiêu chí: version không đổi với cùng template; nhóm B về 0.

**Nhóm C (C1):** chuyển record `ErrorTraps` sang `domain.vo`, không kéo theo class của `application` hay framework. Test hồi quy: danh sách trap rỗng, một trap, nhiều trap, dữ liệu thiếu; `mainErrorCode` không đổi trước và sau. Tiêu chí: nhóm C về 0.

**CI và store:** giữ baseline tạm thời; không xóa store, không refreeze toàn bộ để làm xanh CI. Thu hẹp store theo đúng lệnh trong `AGENTS.md` và review cả diff code lẫn store. Thứ tự PR đề xuất: A1 → B1 → C1.

---

## Phần 2. Mã lỗi Writing mới (người duyệt: examiner/chuyên gia IELTS)

**Mã đã có:** `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT`, `PLURAL_FORM` (thuộc `SPELLING_NUMBER`), `LINKER_MISUSE`, `NO_CLEAR_POSITION`, `UNDERDEVELOPED_IDEA`, `OFF_TOPIC`, `WRONG_PARAGRAPH`, `T1_NO_OVERVIEW`, `T1_DATA_MISREPORTED`.

**Thang mức nghiêm trọng (đề xuất):** 3 = lỗi làm tụt band nặng; 1 = lỗi nhỏ. Mức này là gợi ý, không phải công thức trừ band.

**KP:** chưa có catalog trong tài liệu này. Để trống; không tự tạo KP ID.

| Mã đề xuất | Tên | Nhóm | Mức | Quy tắc ngắn | Ví dụ (tự tạo) | Đề xuất |
|---|---|---|---:|---|---|---|
| `MODAL_BASE_VERB` | Sai dạng động từ sau modal | Ngữ pháp | 2 | Sau modal dùng động từ nguyên mẫu không *to*. | `should going` → `should go` | Seed |
| `MISSING_MAIN_VERB` | Thiếu động từ chính | Ngữ pháp | 2 | Mỗi mệnh đề cần động từ chính. | `The graph a clear increase.` → `shows` | Seed |
| `PREPOSITION_MISUSE` | Dùng sai giới từ | Ngữ pháp | 1 | Chọn giới từ đúng với động từ/tính từ. | `depends of` → `depends on` | Seed |
| `COUNTABILITY_MISUSE` | Sai danh từ đếm được/không đếm được | Ngữ pháp | 2 | Dùng lượng từ đúng với danh từ. | `Many informations` → `Much information` | Seed |
| `COMPARATIVE_STRUCTURE` | Sai cấu trúc so sánh | Ngữ pháp | 2 | Không lặp từ đánh dấu so sánh. | `more higher` → `higher` | Seed |
| `T1_PERSONAL_OPINION` | Ý kiến cá nhân trong Task 1 | Task 1 | 2 | Task 1 chỉ mô tả dữ kiện. | `I think this is wonderful` → `The figures show an upward trend` | Seed |
| `T2_PARTIAL_ANSWER` | Trả lời thiếu một phần đề | Task 2 | 3 | Trả lời đủ từng yêu cầu của đề. | Đề *causes and solutions*, bài chỉ có *causes* | Seed |
| `T2_IRRELEVANT_EXAMPLE` | Ví dụ không liên quan đến luận điểm | Task 2 | 2 | Ví dụ phải làm rõ luận điểm. | Ví dụ xe đạp cho luận điểm học online | Seed (xem câu hỏi bên dưới) |
| `T2_NO_CONCLUSION` | Thiếu kết bài | Task 2 | 1 | Khép lại bài bằng tổng kết quan điểm. | — | Nhận xét cấu trúc, không trừ điểm |
| `GRAMMAR_OTHER`, `VOCABULARY_OTHER`, `COHERENCE_OTHER`, `TASK_OTHER` | Dự phòng | Dự phòng | 1 | — | — | Chỉ dùng nội bộ, không seed cho đến khi duyệt |

**Câu hỏi cho examiner:**
- `T2_IRRELEVANT_EXAMPLE`: mã chỉ dành cho ví dụ không liên quan, hay gồm cả ví dụ quá chung? Nếu gồm cả, cần quy tắc phân biệt với `UNDERDEVELOPED_IDEA`.
- `every years`: `PLURAL_FORM` (đề xuất) hay `COUNTABILITY_MISUSE`?
- `they was go`: `SUBJECT_VERB_AGREEMENT` (đề xuất). Chưa có lý do thêm mã mới về dạng động từ.
- `T2_NO_CONCLUSION`: nhận xét cấu trúc (đề xuất) hay mã trừ điểm?

---

## Phần 3. Nhật ký quyết định (người duyệt ký)

| Đầu việc | Đề xuất | Quyết định | Người duyệt | Ngày |
|---|---|---|---|---|
| A1 — chuyển `*Properties` | Chấp thuận, kèm test binding | Chưa | — | — |
| B1 — `promptVersion` | Chấp thuận, đặt ở `application` | Chưa | — | — |
| C1 — `ErrorTraps` | Chấp thuận, kèm test hồi quy | Chưa | — | — |
| Baseline ArchUnit | Tạm chấp nhận | Chưa | — | — |
| Mã Writing ứng viên | Chấp thuận sau kiểm tra schema/KP | Chưa | — | — |
| `T2_NO_CONCLUSION` | Nhận xét cấu trúc | Chưa | — | — |
| 4 mã dự phòng | Chờ, chưa seed | Chưa | — | — |
| Migration Content V123 | Chỉ sau khi bảng mã được duyệt | Chưa | — | — |

File này là đề xuất, không xác nhận thay đổi code hay database nào đã thực hiện.
