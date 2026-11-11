# Phiếu quyết định cho examiner: chấm Writing bằng AI

**Cách điền:** đọc phần "Đề xuất của đội" để tham khảo, rồi ghi quyết định vào ô "Quyết định của examiner". Đội kỹ thuật không điền thay examiner. Mục nào chưa chắc thì ghi "cần thêm thông tin" và nêu điều cần biết.

**Trạng thái:** chưa có quyết định nào. Đề xuất chỉ là điểm bắt đầu.

---

## A. Bộ bài để đo chất lượng AI

### A1. Số bài examiner có thể cung cấp
Đề xuất của đội: **20 bài**, gồm 10 bài để đo (gold) và 10 bài để kiểm tay (Task 1 và Task 2 chia đều). Có thể gộp thành 10 bài nếu examiner muốn ít công hơn, nhưng khi đó không kiểm tay được Task 2 đủ số lượng.

- Quyết định của examiner: ☐ 20 bài (tách 10 đo + 10 kiểm tay) ☐ 10 bài dùng chung ☐ Khác: ______
- Số bài có thể cung cấp trong tuần này: ______
- Mỗi bài cần ghi: đoạn sai (trích nguyên văn), mã lỗi, và với Task 1 là lỗi ngôn ngữ hay số liệu.

### A2. Ngưỡng bật prompt mới
Đề xuất của đội: AI bắt được lỗi tăng ít nhất 15 điểm phần trăm so với prompt cũ; báo nhầm không tăng quá 10 điểm phần trăm; chấm hỏng không vượt 5%.

- Quyết định của examiner (ghi số): bắt lỗi tăng ≥ ____%; báo nhầm tăng ≤ ____%; chấm hỏng ≤ ____%
- Định nghĩa đội dùng: "bắt được" = tỷ lệ lỗi gold mà AI tìm đúng; "báo nhầm" = lỗi AI báo mà không có trong gold; "chấm hỏng" = bài AI trả kết quả lỗi định dạng hoặc không dùng được.
- Cần báo cáo kèm số lỗi tuyệt đối (ví dụ 18/24), không chỉ phần trăm.

---

## B. Prompt và kiểm tra bài

### B1. Bản hướng dẫn cho AI
Examiner đọc bản prompt trước khi đội viết code. Đội sẽ cung cấp file `.md` trong nhánh hoặc gửi trực tiếp.

- Đường dẫn/cách nhận: ______
- Ghi chú sau khi đọc (câu nào cần sửa, câu nào giữ): ______

### B2. Kiểm tay 10 bài Task 1
Đề xuất của đội: đạt khi **không có gợi ý nào** mâu thuẫn với bảng số liệu. Gợi ý mâu thuẫn là gợi ý nêu giá trị, xu hướng hoặc thứ tự trái với bảng, hoặc đưa số không có trong bảng.

- Quyết định của examiner: ☐ Đồng ý ☐ Sửa tiêu chí: ______
- Số bài Task 1 có bảng examiner có thể cung cấp: ______

### B3. Kiểm tay 10 bài Task 2
Đề xuất của đội: AI nhận đúng yêu cầu của đề, quan điểm và dàn ý ở ít nhất 8/10 bài. Trong đó, cả hai bài cố ý thiếu một phần đề phải được AI nhận ra.

- Quyết định của examiner: ☐ Đồng ý ☐ Sửa tiêu chí: ______

### B4. Chất lượng số liệu của đề Task 1 hiện có
Đề có phần số liệu thiếu giá trị hoặc đơn vị sẽ khiến AI báo sai oan.

- Số đề Task 1 examiner có thể rà: ______
- Đề nào cần sửa trước: ______

---

## C. Mã lỗi Writing mới

### C1. Duyệt từng mã
Với mỗi mã, chọn **Seed** (đưa vào hệ thống), **Chờ** (chưa đưa vào), hoặc **Bỏ**. Sửa tên, mức nghiêm trọng (1 đến 3), quy tắc ngắn và ví dụ nếu cần.

| Mã | Tên đề xuất | Mức đề xuất | Quy tắc ngắn đề xuất | Ví dụ sai → đúng đề xuất | Quyết định (Seed/Chờ/Bỏ) | Sửa đổi |
|---|---|---|---|---|---|---|
| `MODAL_BASE_VERB` | Sai dạng động từ sau modal | 2 | Sau can/should/must dùng động từ nguyên mẫu. | Students should going earlier. → go earlier | | |
| `MISSING_MAIN_VERB` | Thiếu động từ chính | 2 | Mỗi mệnh đề cần động từ chính. | The graph a clear increase. → shows | | |
| `PREPOSITION_MISUSE` | Dùng sai giới từ | 1 | Chọn giới từ đúng với động từ/tính từ. | depends of → depends on | | |
| `COUNTABILITY_MISUSE` | Sai danh từ đếm được/không đếm được | 2 | Dùng lượng từ đúng với danh từ. | Many informations → Much information | | |
| `COMPARATIVE_STRUCTURE` | Sai cấu trúc so sánh | 2 | Không lặp từ đánh dấu so sánh. | more higher → higher | | |
| `T1_PERSONAL_OPINION` | Ý kiến cá nhân trong Task 1 | 2 | Task 1 chỉ mô tả dữ kiện khách quan. | I think this is wonderful → The figures show an upward trend | | |
| `T2_PARTIAL_ANSWER` | Trả lời thiếu một phần đề | 3 | Đề có nhiều yêu cầu thì trả lời đủ từng yêu cầu. | Đề causes and solutions, bài chỉ có causes | | |
| `T2_IRRELEVANT_EXAMPLE` | Ví dụ không liên quan đến luận điểm | 2 | Ví dụ phải làm rõ luận điểm. | Ví dụ về chiếc xe đạp khi luận điểm là học online | | |

**Câu hỏi riêng cho `T2_IRRELEVANT_EXAMPLE`:** đề xuất ban đầu gồm cả "ví dụ quá chung chung". Ví dụ liên quan nhưng thiếu cụ thể đang được xử lý bởi `UNDERDEVELOPED_IDEA`. Examiner chọn:
- ☐ A. Mã chỉ cho ví dụ không liên quan; ví dụ quá chung dùng `UNDERDEVELOPED_IDEA`
- ☐ B. Mã gồm cả hai trường hợp; cần quy tắc phân biệt: ______
- ☐ C. Khác: ______

### C2. `T2_NO_CONCLUSION` (thiếu kết bài): mã lỗi hay nhận xét?
- Đề xuất của đội: nhận xét cấu trúc, không ảnh hưởng điểm, không seed như mã lỗi.
- Quyết định của examiner: ☐ Nhận xét cấu trúc ☐ Mã lỗi có trừ điểm ☐ Khác: ______

### C3. Lỗi "every years" thuộc mã nào?
- Đề xuất của đội: `PLURAL_FORM` (đã có). Lỗi là dạng số nhiều không đúng sau *every*.
- Quyết định của examiner: ☐ `PLURAL_FORM` ☐ `COUNTABILITY_MISUSE` ☐ Khác: ______

### C4. Lỗi "they was go" thuộc mã nào?
- Đề xuất của đội: `SUBJECT_VERB_AGREEMENT` (lỗi chính của "they was"). Không tạo mã mới về dạng động từ ngay; chỉ cân nhắc khi lỗi này lặp lại nhiều bài.
- Quyết định của examiner: ☐ `SUBJECT_VERB_AGREEMENT` ☐ Mã mới (ghi tên): ______

### C5. Bốn mã dự phòng `*_OTHER`
- Đề xuất của đội: chỉ dùng nội bộ, không hiển thị cho học viên, không gắn KP, không tạo bài ôn tự động.
- Quyết định của examiner: ☐ Đồng ý ☐ Hiển thị cho học viên ☐ Khác: ______

---

## D. Luật hạ band

Các mức trần dưới đây **chưa có căn cứ** trong hệ thống. Đội chưa đối chiếu với mô tả band chính thức của IELTS. Examiner xác định mức nào đúng.

### D1. Luật nào bật, và mức trần bao nhiêu?

| Điều kiện | Trần đề xuất (chưa kiểm chứng) | Bật? | Mức examiner chọn |
|---|---|---|---|
| Bài dưới số từ tối thiểu | TA 5.0 | ☐ | ______ |
| Task 1 thiếu đoạn tổng quan | TA ____ (examiner đề xuất) | ☐ | ______ |
| Task 2 trả lời thiếu phần đề | TR 5.0 | ☐ | ______ |
| Task 2 không có quan điểm khi đề yêu cầu | TR 5.0 | ☐ | ______ |

Cách triển khai đề xuất: chạy các luật ở chế độ **chỉ ghi nhận** trên bộ bài gold trước, so với band examiner, rồi mới bật thật.

### D2. Lạc đề: hạ band hay chỉ gắn cờ?
- Đề xuất của đội: chỉ gắn cờ cho Mentor chấm lại, không đổi band, cho đến khi có đủ bài để kiểm.
- Quyết định của examiner: ☐ Chỉ gắn cờ ☐ Hạ band khi có trích dẫn ☐ Khác: ______

---

**Người điền:** ______ **Ngày:** ______ **Chữ ký examiner:** ______
