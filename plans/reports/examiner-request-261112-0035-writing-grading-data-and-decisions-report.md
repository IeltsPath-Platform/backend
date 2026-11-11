# Yêu cầu gửi examiner: dữ liệu và quyết định cho chấm Writing bằng AI

**Người gửi:** ______  **Gửi lại cho:** ______  **Hạn trả:** ______

## 1. Tóm tắt

Hệ thống đang nâng cấp phần AI chấm Writing. Để biết bản mới tốt hơn bản cũ hay không, chúng tôi phải so AI với đánh giá của giám khảo người. Phần so sánh này chỉ làm được khi có bài do examiner đã đánh dấu lỗi.

Chúng tôi cần ở examiner 5 việc. Không cần viết code, không cần chạy hệ thống.

| # | Việc | Số lượng | Cần trước hay sau | Ưu tiên |
|---|---|---|---|---|
| 1 | Xác nhận lại 8 bài đã có (band và lỗi) | 8 bài | Ngay | Cao nhất |
| 2 | Bổ sung bài đã đánh dấu lỗi | ít nhất 6 bài, tốt nhất 12 | Ngay | Cao |
| 3 | Ba bài mẫu để đo tốc độ chấm | 3 bài | Ngay | Cao |
| 4 | Rà các đề Task 1 có bảng số liệu | tùy examiner | Ngay | Trung bình |
| 5 | Trả lời các câu quyết định ngắn (mục 7) | 12 câu | Ngay | Trung bình |
| 6 | Kiểm tay kết quả AI trên 10 bài Task 1 và 10 bài Task 2 | 20 bài | **Sau**: khi chúng tôi gửi kết quả AI | Làm sau |

Việc 6 chưa làm được bây giờ vì chưa có kết quả AI để kiểm. Bây giờ chúng tôi chỉ cần examiner xác nhận tiêu chí kiểm (mục 7, câu B2 và B3).

## 2. Việc 1: Xác nhận 8 bài đã có

**Vì sao cần.** Tám bài (mã CORE-01 và AN-01 đến AN-07; 5 bài Task 1, 3 bài Task 2) đang là bộ chuẩn để đo AI. Band và danh sách lỗi của chúng lấy từ một file kết quả mà đầu file ghi: "AI chấm độc lập, chưa phải xác nhận của giám khảo người". Nếu chuẩn đo chưa được người xác nhận thì mọi con số đo AI sau này chỉ là AI so với AI.

**Examiner làm gì.** Với mỗi bài:
1. Xem band 4 tiêu chí và band tổng đang ghi. Ghi **Đồng ý** hoặc ghi band đúng.
2. Xem từng lỗi đang được đánh dấu. Ghi **Đúng**, **Sai** (không phải lỗi) hoặc **Sửa** (là lỗi nhưng sai mã, sai đoạn trích hoặc sai cách sửa).
3. Bổ sung lỗi bị sót.

**Cách nhận bài:** chúng tôi gửi kèm file của 8 bài với danh sách lỗi hiện có, để examiner chỉ cần xác nhận hoặc sửa, không phải đánh dấu lại từ đầu.

## 3. Việc 2: Bổ sung bài

**Vì sao.** Hiện có 5 bài Task 1 nhưng chỉ 3 bài Task 2, quá ít để kết luận về Task 2. Các tình huống "bài thiếu một phần của đề" và "không nêu quan điểm" chưa có bài mẫu nào, trong khi hệ thống sắp kiểm các tình huống này.

**Tối thiểu cần thêm 6 bài:**

| Loại bài | Số bài | Yêu cầu |
|---|---|---|
| Task 2 | 4 | 2 bài **cố ý thiếu một phần của đề** (ví dụ đề hỏi nguyên nhân và giải pháp, bài chỉ nêu nguyên nhân). 1 bài **không nêu quan điểm** khi đề hỏi ý kiến. 1 bài trả lời đủ, band khá. |
| Task 1 | 2 | Loại biểu đồ khác với 5 bài hiện có. Ít nhất 1 bài **không có đoạn overview**. |

**Tốt nhất là thêm 12 bài** (tổng 20: 10 bài để đo, 10 bài để kiểm tay, Task 1 và Task 2 chia đều). Nếu 12 bài là nhiều thì 6 bài tối thiểu vẫn dùng được, nhưng khi đó phần kiểm tay Task 2 sẽ không đủ 10 bài và chúng tôi sẽ ghi rõ giới hạn đó khi báo cáo.

**Cơ cấu band:** càng trải đều từ 4.0 đến 8.0 càng tốt. Nếu chỉ toàn band 6.0 đến 7.0 thì chúng tôi không đo được AI có chấm quá tay hay quá nhẹ ở hai đầu hay không.

**Cách đánh dấu mỗi bài:** theo mẫu ở mục 6.

## 4. Việc 3: Ba bài mẫu để đo tốc độ chấm

**Vì sao.** AI sẽ được yêu cầu liệt kê mọi lỗi, nên bài dài và nhiều lỗi chấm lâu hơn. Chúng tôi cần biết AI mất bao lâu trong trường hợp xấu nhất để đặt giới hạn chờ cho hợp lý.

**Cần 3 bài, không cần đánh dấu lỗi:**
- 1 bài Task 1, khoảng 150 đến 170 từ.
- 1 bài Task 2, khoảng 260 từ.
- 1 bài **dài và nhiều lỗi**, khoảng 400 từ trở lên (Task 2). Đây là bài quan trọng nhất của 3 bài.

Được dùng lại bài ở Việc 1 nếu đã có đúng độ dài. Riêng bài dài có thể phải viết hoặc chọn thêm.

## 5. Việc 4: Rà các đề Task 1 có bảng số liệu

**Vì sao.** Với Task 1, AI đối chiếu gợi ý sửa với bảng số liệu của đề. Đề nào thiếu giá trị hoặc thiếu đơn vị sẽ khiến AI báo sai oan.

**Examiner làm gì.** Xem các đề Task 1 trong hệ thống (chúng tôi gửi danh sách), liệt kê đề nào thiếu hoặc sai số liệu, đơn vị. Chỉ cần số đề examiner rà được, không bắt buộc hết.

**Cách trả lời:** một bảng ngắn: mã đề, vấn đề (thiếu giá trị, thiếu đơn vị, sai số, khác), cách sửa nếu biết.

## 6. Định dạng nộp bài đã đánh dấu (Việc 1, 2)

**Định dạng ưu tiên:** một file Google Sheet hoặc Excel có 2 trang tính. Nếu examiner quen dùng Word hoặc văn bản thì vẫn được, miễn giữ đúng các cột dưới đây, mỗi lỗi một dòng.

### Trang 1: Bài (mỗi bài một dòng)

| Cột | Nội dung | Ví dụ |
|---|---|---|
| `ma_bai` | Mã bài. Với bài cũ giữ nguyên mã. Bài mới tự đặt, không trùng. | AN-08 |
| `task` | `TASK_1` hoặc `TASK_2` | TASK_2 |
| `de_bai` | Nguyên văn đề bài (với Task 1 ghi mã đề hoặc dán bảng số liệu) | Some people think... |
| `bai_viet` | Nguyên văn bài viết, **không sửa lỗi nào** | ... |
| `ta_tr` | Band Task Achievement (Task 1) hoặc Task Response (Task 2) | 5.5 |
| `cc` | Band Coherence and Cohesion | 6.0 |
| `lr` | Band Lexical Resource | 6.0 |
| `gra` | Band Grammatical Range and Accuracy | 5.5 |
| `band_tong` | Band tổng | 6.0 |
| `ghi_chu_dac_biet` | Chỉ điền nếu bài có tình huống đặc biệt: `THIEU_PHAN_DE`, `KHONG_QUAN_DIEM`, `THIEU_OVERVIEW`, `LAC_DE`, `QUA_NGAN` | THIEU_PHAN_DE |

### Trang 2: Lỗi (mỗi lỗi một dòng)

| Cột | Nội dung | Ví dụ |
|---|---|---|
| `ma_bai` | Mã bài, khớp Trang 1 | AN-08 |
| `doan_sai` | Đoạn sai, **trích nguyên văn đúng từng ký tự** từ bài (kể cả dấu cách và dấu câu). Hệ thống so khớp đúng từng ký tự, chỉ lệch một dấu cách là bị tính là không tìm thấy. | the number of students are increase |
| `cach_sua` | Cách sửa đúng | the number of students is increasing |
| `ma_loi` | Mã lỗi trong danh sách ở mục 7 (C1). Không chắc thì ghi mã dự phòng `*_OTHER` và nêu lý do ở cột ghi chú. | SUBJECT_VERB_AGREEMENT |
| `loai` | `NGON_NGU` (lỗi câu chữ) hoặc `SO_LIEU` (lỗi số liệu của biểu đồ, chỉ Task 1) | NGON_NGU |
| `ghi_chu` | Tùy chọn | |

**Quy ước:**
- Một đoạn sai chứa hai lỗi khác nhau thì tách thành hai dòng.
- Gợi ý sửa kiểu "có cách hay hơn" (câu đúng ngữ pháp nhưng chưa tối ưu) **không ghi vào Trang 2**. Chỉ ghi lỗi vi phạm quy tắc. Nếu examiner muốn ghi cả gợi ý, thêm cột `muc` với giá trị `LOI` hoặc `GOI_Y`.
- Ghi tất cả lỗi examiner thấy, kể cả lỗi nhỏ. Đo AI bắt được bao nhiêu lỗi thì lấy số lỗi của examiner làm mẫu số.

## 7. Việc 5: Các câu quyết định ngắn

**Nguyên tắc:** bên dưới ghi giá trị mà chủ sản phẩm đã chọn tạm để dev có thể làm tiếp. Các giá trị này **chưa qua examiner** và có thể đổi bằng cấu hình hoặc migration mới. Examiner chọn Đồng ý, Sửa hoặc Cần thêm thông tin; chúng tôi không điền thay examiner.

### B1. Nội dung hướng dẫn AI (prompt v2)
File: `docs/contracts/essay-prompt-template-v2.txt` (chúng tôi gửi kèm bản đọc được). Chưa được bật cho học viên.
- Examiner ghi: câu nào cần sửa, câu nào giữ: ______

### B2. Tiêu chí kiểm tay Task 1 (làm sau khi có kết quả AI)
Đạt khi **không có gợi ý nào** mâu thuẫn với bảng số liệu: nêu giá trị, xu hướng hoặc thứ tự trái với bảng, hoặc đưa số không có trong bảng.
- ☐ Đồng ý ☐ Sửa: ______

### B3. Tiêu chí kiểm tay Task 2 (làm sau)
AI nhận đúng yêu cầu của đề và quan điểm ở ít nhất 8 trên 10 bài; trong đó mọi bài cố ý thiếu một phần đề phải được nhận ra.
- ☐ Đồng ý ☐ Sửa: ______

### C1. Duyệt mã lỗi mới
Các mã dưới đây đã nằm trong hệ thống ở trạng thái đang dùng. Examiner chọn **Giữ**, **Sửa** (ghi sửa gì) hoặc **Bỏ**.

| Mã | Tên | Nhóm | Mức (1 đến 3) | Quyết định | Sửa đổi |
|---|---|---|---|---|---|
| `PASSIVE_VOICE` | Sai cấu trúc bị động | Ngữ pháp | 2 | | |
| `VERB_FORM` | Sai dạng động từ | Ngữ pháp | 2 | | |
| `PREPOSITION` | Sai giới từ | Ngữ pháp | 1 | | |
| `COMPARISON` | Sai cấu trúc so sánh | Ngữ pháp | 1 | | |
| `T2_PARTIAL_ANSWER` | Trả lời thiếu phần của đề Task 2 | Yêu cầu đề | 3 | | |
| `T2_NO_CONCLUSION` | Thiếu kết bài Task 2 | Yêu cầu đề | 2 | | |
| `T2_IRRELEVANT_EXAMPLE` | Ví dụ không liên quan hoặc quá chung chung | Yêu cầu đề | 2 | | |

Hai điểm cần examiner nhìn kỹ:
- **Giới từ** đang được gắn vào nhóm kiến thức "từ ghép học thuật" vì hệ thống chưa có kiến thức ngữ pháp riêng cho giới từ. Examiner có thấy hợp lý không? ______
- **`T2_IRRELEVANT_EXAMPLE`** đang gộp cả "ví dụ không liên quan" và "ví dụ quá chung chung", trong khi hệ thống đã có mã `UNDERDEVELOPED_IDEA` (ý chưa phát triển). Chọn một:
  ☐ A. Chỉ cho ví dụ không liên quan; ví dụ quá chung dùng `UNDERDEVELOPED_IDEA`
  ☐ B. Giữ gộp cả hai; cần quy tắc phân biệt: ______

### C2. Thiếu kết bài Task 2
`T2_NO_CONCLUSION` đang là **mã lỗi** (có mức nghiêm trọng 2). Chúng tôi từng cân nhắc chỉ coi là nhận xét cấu trúc, không trừ điểm.
- ☐ Giữ là mã lỗi ☐ Chuyển thành nhận xét cấu trúc ☐ Khác: ______

### C3. Hai lỗi mẫu đang gộp vào mã có sẵn
- `This data suggest` được tính vào `SUBJECT_VERB_AGREEMENT` (đúng quy tắc examiner đã ghi). ☐ Đồng ý ☐ Sửa: ______
- Các lỗi dạng "they was go", "every years": ☐ Đồng ý với cách gộp hiện tại ☐ Cần mã riêng: ______

### C4. Mã dự phòng `*_OTHER` (4 mã: ngữ pháp, từ vựng, mạch lạc, yêu cầu đề)
Hiện chỉ dùng nội bộ, không hiện cho học viên, không tạo bài ôn tự động. Còn **2 lỗi** trong bộ bài chuẩn đang phải dùng `GRAMMAR_OTHER`.
- ☐ Đồng ý ☐ Hiển thị cho học viên ☐ Khác: ______
- Hai lỗi còn lại thuộc mã nào? (chúng tôi gửi kèm 2 lỗi): ______

### D1. Mức trần band cho từng luật
Khi bài vi phạm luật, band của tiêu chí liên quan bị **hạ xuống mức trần**, không bao giờ được nâng. Các luật đang **tắt**; chỉ bật sau khi so thử trên bộ bài.

| Luật | Tiêu chí | Mức trần đang đặt tạm | Quyết định |
|---|---|---|---|
| Bài ngắn hơn số từ tối thiểu của đề | TA/TR | 5.0 | ☐ Đồng ý ☐ Sửa thành ____ |
| Task 1 không có đoạn tổng quan (overview) | TA | 5.0 | ☐ Đồng ý ☐ Sửa thành ____ |
| Task 2 trả lời thiếu phần của đề | TR | 5.0 | ☐ Đồng ý ☐ Sửa thành ____ |
| Task 2 đề hỏi ý kiến mà bài không nêu quan điểm | TR | 5.0 | ☐ Đồng ý ☐ Sửa thành ____ |
| Lạc đề (xem D2) | TA/TR | 4.0 | ☐ Đồng ý ☐ Sửa thành ____ |

Với bài ngắn: có muốn hai bậc (thiếu ít, thiếu nhiều) không? ______

### D2. Lạc đề: hạ band hay chỉ gắn cờ?
Hạ band nếu AI nhầm sẽ làm học viên bị chấm thấp oan. Chỉ gắn cờ thì không đổi band mà báo cho người chấm lại.
- ☐ Chỉ gắn cờ ☐ Hạ band khi AI trích dẫn được đoạn lạc đề ☐ Khác: ______

## 8. Việc 6 (làm sau): Kiểm tay kết quả AI

Khi bản AI mới chạy xong, chúng tôi sẽ gửi lại một bộ gồm 20 bài (10 Task 1, 10 Task 2) kèm kết quả AI. Examiner chỉ cần với mỗi bài trả lời đúng hai câu:

| Task | Câu hỏi | Cách trả lời |
|---|---|---|
| Task 1 | Có gợi ý sửa nào của AI mâu thuẫn với bảng số liệu không? | Có/Không; nếu có, trích gợi ý đó |
| Task 2 | AI nhận đúng yêu cầu của đề và quan điểm của bài chưa? | Đúng/Sai/Một phần; nếu sai, ghi điều đúng |

Việc này mất ít công hơn đánh dấu lỗi từ đầu. Chúng tôi sẽ báo trước khi gửi.

## 9. Điều chúng tôi sẽ làm với phần examiner trả lời

- Bài đã xác nhận trở thành **bộ chuẩn** để đo AI. Số đo được báo bằng số tuyệt đối (ví dụ 18 trên 24 lỗi), không chỉ phần trăm.
- Quyết định ở mục 7 được ghi vào cấu hình hoặc migration. Mục nào examiner sửa so với giá trị tạm thì chúng tôi đổi theo trước khi bật.
- Chúng tôi không dùng nội dung bài của học viên thật; chỉ dùng bài examiner gửi.

## 10. Danh sách cần có khi nộp

- ☐ File bài đã đánh dấu (Trang 1 và Trang 2)
- ☐ 3 bài mẫu đo tốc độ
- ☐ Danh sách đề Task 1 cần sửa (nếu có)
- ☐ Mục 7 đã điền
- ☐ Số bài có thể cung cấp trong tuần này: ______

**Người điền:** ______  **Ngày:** ______
