# Thay đổi nghiệp vụ: trước và sau khi áp dụng hai plan mới

- Ngày cập nhật: 18/10/2026
- Phạm vi: hai plan ngày 17/10/2026, "Mastery, luyện tập và tiến độ" và "Gamification" (tạo động lực học).
- Đối tượng đọc: BA, PO, người phụ trách nội dung và các bên không làm kỹ thuật. Tài liệu không mô tả cách làm.
- Mốc "trước" là hệ thống sau plan đầu tiên (cây kiến thức, chẩn đoán, giỏ ôn, khu quản trị). Mốc "sau" là hệ thống đã có
  hai plan mới, nếu bật hết.

Một vài từ dùng trong tài liệu:

- **KP (điểm kiến thức):** một kỹ năng nhỏ mà học viên cần nắm, ví dụ "phân biệt False và Not Given".
- **Điểm (points):** đơn vị tiền chung của hệ thống. Học viên dùng để mua dịch vụ như chấm essay bằng AI.
- **XP:** điểm kinh nghiệm, đo công sức học, không dùng để mua gì.

---

## 1. Tóm tắt một trang

| Chủ đề | Trước | Sau |
|---|---|---|
| Đánh giá mức nắm KP | Chỉ nhìn tỷ lệ đúng/sai, mọi lần làm có giá trị như nhau, không phai theo thời gian | Có công thức mới, tính cả thời gian, nguồn bài và số ngày. Có mức mới "Chưa đủ dữ liệu". *Đã bật làm mặc định theo quyết định ngày 18/10* |
| Thông tin của KP | Chỉ có tên, mô tả | Thêm mức CEFR, "học viên làm được gì" (can-do), phạm vi trong và ngoài |
| Cây kiến thức | Chỉ có KP chiến lược | Thêm 13 KP Grammar và Vocabulary, nối vào KP chiến lược. *Đang ẩn với học viên* |
| Luyện tập | Chỉ luyện được KP nằm trong giỏ ôn đến hạn | Học viên tự chọn bất kỳ KP của khóa đang học để luyện |
| Màn cây kiến thức của học viên | Không có | Có: mỗi KP tô màu theo mức nắm, kèm nút "Luyện" |
| Báo cáo | Theo từng KP | Thêm 6 kỹ năng, band ước tính, khoảng cách tới band mục tiêu, dự kiến ngày đạt mục tiêu |
| Báo cáo định kỳ | Không có | Báo cáo tuần, xu hướng 4 tuần, báo cáo tháng |
| Flashcard | Chỉ lưu thẻ | Ôn ngắt quãng 5 hộp, có danh sách thẻ đến hạn |
| Động lực học | Streak do máy khách tự báo, không có thưởng | XP, cấp, tim, streak thật, đóng băng, nhiệm vụ ngày, huy hiệu, league tuần, thưởng điểm |
| Kiếm điểm từ việc học | Không có | Có, nhưng giới hạn 20 điểm mỗi ngày (huy hiệu không tính vào giới hạn) |
| Học viên bỏ học | Không ai biết | Quá 3 ngày không làm bài thì xuất hiện cờ cảnh báo cho giáo viên |
| Số liệu hoạt động | Không có | Người dùng hoạt động theo ngày, tuần, tháng và tỷ lệ quay lại |
| Soạn nội dung | Xuất bản ngay | Xem trước gói bài, hẹn giờ xuất bản |

---

## 2. Chi tiết theo từng nhóm

### 2.1 Hiểu đúng năng lực học viên

**Trước**
- Mức nắm KP chỉ dựa vào tỷ lệ trả lời đúng. Một bài làm 3 tháng trước có giá trị như bài làm hôm qua.
- Ba mức: cần ưu tiên (dưới 60%), cần củng cố (60–80%), vững (từ 80%). Không đòi một số bằng chứng tối thiểu, nên chỉ vài câu
  đã đủ để xếp mức.
- KP không có thông tin về trình độ CEFR hay "làm được gì".

**Sau**
- **Công thức mới (MP-1.0):**
  - Chỉ xét 8 bằng chứng gần nhất.
  - Bằng chứng cũ giá trị giảm dần, giảm một nửa sau khoảng 30 ngày.
  - Mỗi nguồn bài có trọng số khác nhau: bài thi, bài ôn và thăm dò chẩn đoán tính đủ giá trị; luyện tập chỉ tính 60%, essay 50%.
  - Tối đa 3 bài luyện được tính trong 8 bằng chứng.
  - Cùng một câu làm lại trong 7 ngày không tính thêm.
  - Cần ít nhất 3 bằng chứng trên 2 ngày khác nhau. Nếu chưa đủ, KP hiện **"Chưa đủ dữ liệu"** thay vì bị xếp mức quá sớm.
  - Ngưỡng 60% và 80% giữ nguyên.
- **Công thức mới là mặc định** từ quyết định ngày 18/10. Công thức cũ vẫn còn và bật lại được bằng một biến cấu hình. Khi đổi công thức, mức của học viên đang học có thể đổi, nên môi trường đã có dữ liệu cần tính lại một lần.
- **KP có thêm thông tin:** mức CEFR (A1–C2), một câu "học viên làm được gì", phạm vi nằm trong và ngoài KP.
- **Cây kiến thức đủ 6 miền:** thêm 8 KP Grammar (câu phức, mệnh đề quan hệ, bị động, so sánh, thì hoàn thành, mạo từ, hòa hợp chủ vị, từ nối) và 5 KP Vocabulary (kết hợp từ học thuật, từ vựng mô tả biểu đồ, từ đồng nghĩa, dạng từ, chủ đề IELTS thông dụng). Mỗi KP gắn vào KP chiến lược mà nó hỗ trợ. **Chúng đang ẩn**, học viên chỉ thấy khi Content chuyển sang hoạt động. Không có KP Speaking vì chưa chấm được Speaking.

### 2.2 Luyện tập và cây kiến thức của học viên

**Trước**
- Học viên không tự chọn KP để luyện. Chỉ làm được KP hệ thống đưa vào giỏ ôn khi đến hạn.
- Không có màn nào cho học viên thấy toàn bộ KP của khóa.

**Sau**
- **Màn cây kiến thức:** mỗi KP của khóa đang học có mức nắm (tô màu), trình độ CEFR, "làm được gì", và nút "Luyện".
- **Luyện tự do:** bấm "Luyện" trên bất kỳ KP nào của khóa đang học để làm một bộ bài chưa từng thấy.
  - Làm sai không tạo bài ôn bắt buộc, không khóa bài học, không đổi điểm khóa học.
  - Kết quả vẫn được tính làm bằng chứng nắm KP, vẫn cho XP và vẫn tốn tim.
  - Nếu KP đang có một bài ôn bắt buộc chưa làm thì phải làm bài ôn trước.
- **Bảo vệ kho bài ôn:** hệ thống luôn giữ lại 2 bộ cuối cùng của mỗi KP cho bài ôn. Khi chỉ còn 2 bộ thì luyện tự do báo hết bộ.
- **Không giao bộ quá khó:** không đưa bộ có band cao hơn band của khóa. Bộ yêu cầu gói trả phí chỉ đưa cho người có gói đang hiệu lực.
- **Giới hạn đã biết:** chỉ báo "còn bộ để luyện" ở cây kiến thức là ước lượng. Có thể nút "Luyện" bấm được nhưng hệ thống trả lời "hết bộ" vì các bộ còn lại bị loại do band, gói hoặc có essay. Màn hình cần xử lý bằng thông báo thân thiện.

### 2.3 Báo cáo tiến độ

**Trước**
- Báo cáo chỉ liệt kê từng KP và mức nắm.

**Sau**
- **Theo 6 kỹ năng:** Listening, Reading, Writing, Speaking, Grammar, Vocabulary. Mỗi kỹ năng có số KP theo từng mức.
- **Band ước tính:** trung bình các band kỹ năng, làm tròn 0,5. Speaking chỉ tính khi đã có band. Grammar và Vocabulary không đưa vào band. Band này chỉ là gợi ý, **không** làm đổi trạng thái học hay khóa học.
- **So với mục tiêu:** nếu học viên đã đặt band mục tiêu thì báo cáo hiện khoảng cách. Nếu chưa đặt thì phần này không xuất hiện.
- **Dự kiến ngày đạt mục tiêu:** dựa trên tiến bộ 4 tuần gần nhất. Khi dữ liệu chưa đủ hoặc chưa có tiến bộ, báo cáo nói rõ thay vì đoán.
- **Danh sách "nên tập trung":** các KP yếu nhất chưa xong, bài ôn bắt buộc xếp trước.
- **Báo cáo tuần và tháng:** hệ thống chụp lại tình hình học viên vào cuối mỗi tuần (tuần bắt đầu Chủ nhật), dùng để so sánh với tuần trước, vẽ xu hướng và làm báo cáo tháng. Chỉ xem trong ứng dụng, không gửi email, không xuất PDF.

### 2.4 Tạo động lực học (Gamification)

**Trước**
- Chuỗi ngày học do máy khách tự báo, không dùng để thưởng.
- Không có XP, cấp độ, tim, nhiệm vụ, huy hiệu hay bảng xếp hạng.
- Điểm chỉ dùng để chi tiêu, chưa có cách kiếm từ việc học.

**Sau**

| Tính năng | Cách hoạt động |
|---|---|
| **XP** | Mỗi câu đúng lần đầu cho XP theo độ khó: dễ 10, vừa 15, khó 20, chưa gắn nhãn 10. Một ngày chỉ tính tối đa 100 XP. Phần vượt vẫn được lưu để xem lịch sử nhưng không tính vào cấp và league. Làm lại cùng bài không có XP |
| **XP mốc KP** | KP lên mức "cần củng cố" cho 25 XP, lên mức "vững" cho 50 XP, mỗi KP chỉ một lần và **chỉ khi bằng chứng đến từ luyện tập** (không tính từ bài thi, thăm dò chẩn đoán hay essay) |
| **Cấp độ** | Cứ 1000 XP lên một cấp. Lên cấp thì được hồi đầy tim miễn phí |
| **Tim** | Mỗi học viên có 5 tim (gói trả phí có 7). Làm sai một câu trắc nghiệm trong bài tập hoặc Practice hoặc luyện tự do thì mất 1 tim. Cứ 5 giờ hồi 1 tim. Hết tim thì không bắt đầu hoặc nộp được bộ luyện tập cho đến khi có tim, hoặc dùng 5 điểm hồi đầy. Bài ôn, kiểm tra, bài thi, chẩn đoán và essay không tốn tim và vẫn làm được khi hết tim |
| **Streak** | Tính ở máy chủ theo ngày của học viên (có cài đặt múi giờ, mặc định giờ Việt Nam). Một ngày có ít nhất một bài nộp thì chuỗi tiếp tục. Essay đã chấm cũng tính là ngày học |
| **Đóng băng streak** | Mua bằng điểm để bỏ qua một ngày trống mà không mất chuỗi. Giá 10 điểm cho lần đầu, 20 cho lần thứ hai liên tiếp, đến 50. Tối đa 5 lần liên tiếp. Mỗi lúc chỉ giữ một lượt. Chưa có chuỗi thì không mua được |
| **Thưởng streak** | Mỗi mốc 7 ngày: +100 XP và +10 điểm. Mốc 30 ngày: +500 XP, không thêm điểm |
| **Nhiệm vụ ngày** | Mỗi ngày 3 nhiệm vụ: (1) làm 5 câu luyện tập, thưởng 20 XP và 5 điểm. (2) Luyện KP yếu nhất: làm 3 câu trong luyện tự do hoặc bài ôn ở KP đó, thưởng 30 XP. Nếu không có KP nào phù hợp thì thay bằng làm 10 câu, thưởng 30 XP. (3) Giữ streak: trả lời ít nhất 1 câu, thưởng 10 XP |
| **Huy hiệu** | 9 huy hiệu, mỗi cái nhận một lần, kèm điểm thưởng (xem bảng dưới). Quản trị viên có thể tắt một huy hiệu; huy hiệu đã nhận được giữ nguyên |
| **League tuần** | 7 hạng: Bronze, Silver, Gold, Sapphire, Diamond, Ruby, Amethyst. Mỗi tuần học viên có XP được xếp vào nhóm tối đa 30 người cùng hạng. Cuối tuần (Chủ nhật 00:00 giờ UTC, tức 7 giờ sáng giờ Việt Nam) top 7 lên hạng, hạng 23 đến 30 xuống hạng. Nhóm dưới 23 người thì không ai xuống hạng. Bảng hiển thị tên hiển thị của học viên |
| **Thưởng điểm** | Điểm được cộng khi: đạt mốc streak 7 ngày, hoàn thành nhiệm vụ luyện 5 câu, nhận huy hiệu, hoặc một KP đạt từ 90% mức nắm lần đầu (+5 điểm, mỗi KP một lần, chỉ tính bằng chứng luyện tập) |
| **Giới hạn điểm kiếm mỗi ngày** | Tối đa 20 điểm mỗi ngày từ streak, nhiệm vụ và KP. **Huy hiệu không tính vào giới hạn**, nên huy hiệu 50 điểm vẫn nhận đủ. Mục đích: tránh học viên cày điểm để dùng chấm essay AI miễn phí |

Danh sách huy hiệu:

| Huy hiệu | Điều kiện | Điểm thưởng |
|---|---|---|
| Chuỗi 7 ngày | Học liên tục 7 ngày | 5 |
| Chuỗi 30 ngày | Học liên tục 30 ngày | 15 |
| Chuỗi 100 ngày | Học liên tục 100 ngày | 50 |
| 10 kiến thức vững | 10 KP ở mức vững | 5 |
| 50 kiến thức vững | 50 KP ở mức vững | 20 |
| Bài kiểm tra chủ đề đầu tiên | Hoàn thành bài kiểm tra chủ đề đầu tiên | 3 |
| Hoàn thành khóa học | Đạt bài kiểm tra cuối khóa | 20 |
| Cấp độ 10 | Đạt cấp 10 | 20 |
| 30 nhiệm vụ ngày | Hoàn thành 30 nhiệm vụ | 10 |

Lưu ý cho người dùng:
- Điểm thưởng đi qua một hàng chờ nên **có thể về chậm** vài phút. Nếu hệ thống điểm tạm gián đoạn thì hệ thống tự thử lại và bỏ cuộc sau 10 lần.
- Hồi tim hoặc mua đóng băng bị trừ điểm trước. Nếu lỗi giữa chừng, học viên gửi lại cùng thao tác sẽ không bị trừ lần hai. **Không có nút hoàn điểm tự động**: nếu học viên không gửi lại thì cần quản trị viên hoàn thủ công khi có khiếu nại.

### 2.5 Ôn từ vựng bằng flashcard

**Trước**
- Học viên lưu được thẻ nhưng không có lịch ôn.

**Sau**
- Phương pháp 5 hộp: trả lời đúng thì thẻ lên một hộp, trả lời sai thì về hộp 1. Khoảng cách ôn lại theo hộp là 1, 2, 4, 8, 16 ngày.
- Có danh sách thẻ đến hạn: thẻ chưa từng ôn xếp trước. Có thể lọc theo bộ thẻ.
- Gửi lại cùng một lượt ôn không bị tính hai lần.
- Thêm danh mục 300 từ vựng mẫu (kèm các nghĩa của từ) để học viên có sẵn từ để tạo thẻ.

### 2.6 Giáo viên và quản trị

**Trước**
- Không biết học viên đã bỏ học.
- Không có số liệu tổng về mức độ hoạt động.
- Nội dung đã soạn xuất bản ngay, không xem trước được bản nháp như học viên.

**Sau**
- **Cờ vắng học:** học viên đang theo một khóa mà quá 3 ngày liền không trả lời câu nào thì hệ thống tạo một cờ "INACTIVE" trong hàng việc của giáo viên chấm bài. Cờ tự đóng khi học viên làm bài lại. Hoạt động được tính bằng việc **làm bài**, không phải đăng nhập.
- **Thống kê hoạt động (chỉ quản trị viên):** số học viên hoạt động theo ngày, tuần, tháng và tỷ lệ quay lại theo từng nhóm học viên cùng tuần bắt đầu.
- **Xem trước gói bài:** người soạn xem bản nháp đúng như học viên sẽ thấy, và xem đáp án riêng.
- **Hẹn giờ xuất bản:** đặt một giờ trong tương lai, hệ thống tự xuất bản trong khoảng một phút sau giờ đó. Nếu lỗi (ví dụ câu hỏi đã dùng ở gói khác) thì bản nháp được giữ, hiển thị mã lỗi để sửa, và hệ thống thử lại. Có thể hủy lịch trước giờ.
- **Quản lý huy hiệu:** quản trị viên bật hoặc tắt từng huy hiệu.
- **Soạn KP:** form có thêm trường CEFR, "làm được gì" và phạm vi. Mã KP có dạng chữ cái cộng hai chữ số rồi tên, ví dụ `G55_PASSIVE_VOICE` cho Grammar và `V60_DATA_TREND_WORDS` cho Vocabulary; mã sai quy tắc bị từ chối.

---

## 3. Những gì không thay đổi

- Các bước học, chặn bài, kiểm tra chủ đề và cuối khóa như cũ.
- Placement (kiểm tra đầu vào) và chẩn đoán không đổi.
- Chấm essay bằng AI vẫn dùng điểm; chấm tay bởi giám khảo vẫn qua hàng chờ.
- Số điểm hiện có của học viên giữ nguyên.
- Streak cũ do máy khách tự báo ở phần hồ sơ người dùng vẫn tồn tại nhưng **không** dùng để thưởng.

## 4. Ngoài phạm vi (đã quyết không làm)

- Phụ huynh, lớp học, thống kê theo lớp, giao bài cho học viên, nhắn cả lớp.
- Thêm vai trò hoặc dịch vụ mới.
- Gửi email, thông báo đẩy cho báo cáo tuần và tháng; xuất PDF.
- Tùy chọn ẩn danh trên bảng league (hiện luôn hiển thị tên hiển thị).
- KP Speaking.
- Nút hoàn điểm tự động cho học viên khi mua tim hoặc đóng băng bị lỗi giữa chừng.

## 5. Các quyết định đã chốt với bạn

| Quyết định | Lựa chọn |
|---|---|
| XP mốc KP chỉ tính cho bằng chứng luyện tập | Có |
| Giới hạn XP câu trả lời mỗi ngày | 100 |
| Luật league nhóm nhỏ | Giữ đúng theo tài liệu mentor: top 7 luôn lên hạng kể cả nhóm nhỏ, nhóm dưới 23 người không ai xuống hạng |
| Công thức mastery mặc định | Công thức mới (bạn chọn bật ngày 18/10; khuyến nghị lúc đó là giữ công thức cũ tới khi có dữ liệu học viên thật) |
| Điểm thưởng đi từ Learning sang Access | Qua hàng chờ để không mất nếu một bên tạm lỗi |
| Hoàn điểm khi lỗi giữa chừng | Không thêm chức năng; xử lý thủ công khi có khiếu nại |
| Tim trừ khi nào | Mỗi lần nộp bài mới |
| Seed KP Grammar và Vocabulary | Đã duyệt, đang ẩn |
| Trần điểm kiếm mỗi ngày | 20 điểm; huy hiệu không tính vào |
| Essay đã chấm | Tính là ngày học và là một câu luyện tập; không cho XP câu trả lời, không tốn tim |

## 6. Tình trạng hiện tại

**Đã xong và kiểm thử ở phía máy chủ:** tất cả tính năng ở mục 2. Toàn bộ bộ kiểm thử tự động của dự án (khoảng 1.750 bài kiểm tra) đã chạy xanh vào ngày 18/10, không bỏ qua bài nào. 

**Đã chạy thử trên hệ thống dựng thật (Docker Compose, 18/10):** cả 13 kịch bản demo (7 cho mastery, 6 cho gamification), gồm hết tim và hồi tim, giới hạn XP ngày, streak 7 ngày, mua đóng băng, nhiệm vụ điểm yếu, league 31 học viên, báo cáo, bản chụp tuần, flashcard, cờ vắng học và xuất bản hẹn giờ. Điểm thưởng đã đi từ Learning sang Access thật và được cộng vào ví; khi Access tắt thì khoản thưởng được giữ lại và cộng sau khi Access bật lại. Việc chạy thử tìm ra ba lỗi mà kiểm thử tự động không thấy (Access thiếu cấu hình hàng chờ, Learning không tìm được User Service, bản chụp tuần luôn lỗi khi chạy thật) và cả ba đã được sửa.

**Chưa làm hoặc chưa chạy:**
- Trên hệ thống dựng thật, chưa thử: nộp bài ôn khi hết tim, nhánh lỗi của xuất bản hẹn giờ, và so sánh tuần này với tuần trước trong báo cáo tuần (mới có một tuần dữ liệu).
- Nội dung khóa học hiện chưa có câu nào gắn độ khó "Khó", nên mức thưởng 20 XP của câu khó chưa xảy ra với nội dung thật cho tới khi có người gắn nhãn.
- 13 KP Grammar và Vocabulary chưa kích hoạt cho học viên.

**Giao diện học viên:**
- Đã cập nhật phần luồng học: hiện tim, nút hồi tim bằng điểm, thông báo hết tim, XP và cấp sau mỗi lần nộp Practice hoặc bài ôn.
- **Chưa làm:** màn cây kiến thức và nút "Luyện", thanh tiến độ và cấp ở đầu trang, streak và đóng băng, nhiệm vụ ngày, huy hiệu, league, báo cáo mở rộng và báo cáo tuần/tháng, màn ôn flashcard, các màn quản trị mới.
- Giao diện chưa được xem trên trình duyệt thật.

## 7. Ảnh hưởng tới từng nhóm

| Nhóm | Thay đổi chính |
|---|---|
| Học viên | Thêm tim (có thể bị chặn khi hết tim), XP, cấp, streak, nhiệm vụ, huy hiệu, league; tự chọn KP để luyện; báo cáo chi tiết hơn |
| Giáo viên / giám khảo | Nhận cờ cảnh báo học viên vắng học trong hàng việc |
| Quản trị viên | Xem thống kê hoạt động; bật tắt huy hiệu; hoàn điểm thủ công khi có khiếu nại |
| Người soạn nội dung | Xem trước bản nháp; hẹn giờ xuất bản; điền CEFR và phạm vi cho KP; kích hoạt các KP Grammar và Vocabulary khi sẵn sàng |
| Đội giao diện | Có khoảng 9 màn hoặc cụm màn mới cho học viên, 4 chỗ quản trị, và 3 đến 4 chỗ phải sửa ở màn có sẵn (quan trọng nhất: xử lý lỗi hết tim) |

## 8. Việc cần bạn quyết hoặc cần biết thêm

1. **Bật công thức mastery mới hay không?** Bật thì học viên mới thấy mức "Chưa đủ dữ liệu" và mức nắm phai theo thời gian. Có thể khiến một số KP đổi mức so với hiện nay.
2. **Khi nào kích hoạt 13 KP Grammar và Vocabulary** cho học viên? Cần có bộ bài tập gắn vào các KP này, nếu không học viên không luyện được.
3. **Có muốn thêm tùy chọn ẩn danh trên league không?** Hiện luôn hiện tên hiển thị.
4. **Có đẩy (push) toàn bộ thay đổi lên kho mã chung chưa?** Hiện các thay đổi nằm ở máy cục bộ.
5. **Rủi ro cần theo dõi:** điểm thưởng có giới hạn 20 điểm mỗi ngày, huy hiệu lớn thì không giới hạn. Nếu thấy học viên nhận điểm quá nhiều, cần xem lại mức thưởng huy hiệu.
