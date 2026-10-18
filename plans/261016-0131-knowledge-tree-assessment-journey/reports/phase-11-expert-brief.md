# Nhờ chuyên gia IELTS: chấm bộ bài mẫu để kiểm tra AI chấm Writing

## Vì sao cần anh/chị

Hệ thống có AI chấm sơ bộ bài Writing. Trước khi cho AI chấm thật, nhóm phải chứng minh nó chấm gần giống giáo viên. Cách
kiểm: cho AI chấm một bộ bài đã được chuyên gia chấm, rồi so điểm. Hiện **chưa có bộ bài nào do người có chuyên môn chấm**,
nên chưa thể nói AI chính xác tới đâu. Điểm do người không chuyên hoặc do AI tự tạo không dùng được cho việc này.

Anh/chị giúp hai việc: **(B) xem lại rubric nháp** (ngắn hơn, nên làm **trước**, vì rubric sai thì chấm xong mới biết sẽ
tốn công) rồi **(A) chấm bộ bài mẫu**.

## Việc A: chấm bộ bài mẫu

**Số lượng.** Mỗi Task (Task 1 và Task 2) cần ít nhất **30 bài**; càng nhiều số liệu càng ổn định. Nếu chỉ làm được một
Task, ưu tiên Task 2 trước.

**Chọn bài.**
- Bài thật của người học (đã xin phép, bỏ tên và thông tin cá nhân), hoặc bài mẫu viết tay có chủ đích ở nhiều trình độ.
- Trải đều các band, khoảng **band 4.0 đến 8.0**, mỗi band ít nhất vài bài. Đừng chỉ chọn bài trung bình, vì khi đó AI
  "đoán trung bình" cũng trông có vẻ đúng.
- Không dùng bài do AI viết.
- Task 1: kèm đề và **dữ liệu của biểu đồ/bảng** (số liệu chính) để AI và người chấm nhìn cùng một thông tin.

**Cách chấm.**
- Chấm theo band descriptor chính thức, **độc lập, không xem điểm AI** (nhóm cũng sẽ không cho AI xem điểm của anh/chị).
- Cho **band từng tiêu chí** (0 đến 9, bước 0.5). **Band tổng** tính theo công thức của hệ thống, giống cách IELTS làm:
  trung bình 4 tiêu chí, làm tròn tới 0.5 (0.25 làm tròn lên 0.5; 0.75 làm tròn lên số nguyên kế tiếp). Anh/chị có thể
  tự ghi cột `band_tong` theo công thức này; nhóm sẽ kiểm tra lại cho khớp:
  - Task 1: Task Achievement (TA), Coherence & Cohesion (CC), Lexical Resource (LR), Grammatical Range & Accuracy (GRA).
  - Task 2: Task Response (TR), CC, LR, GRA.
- Nếu tiện, ghi thêm **các lỗi chính** (tối đa vài lỗi) kèm mã lỗi trong danh mục nhóm gửi kèm. Việc này không bắt buộc;
  nếu không ghi thì bỏ trống, nhóm vẫn dùng được điểm.
- Nếu có hai chuyên gia, nên cho **ít nhất 15–20 bài** được hai người chấm riêng rồi gửi cả hai bảng; nhóm dùng để đo
  mức đồng thuận giữa người với người, làm mốc so với AI. Cần chốt trước khi hai người lệch nhiều thì lấy trung bình
  hay họp thống nhất.
- Bài thiếu số từ (dưới 150 từ Task 1, dưới 250 từ Task 2) hoặc lạc đề: vẫn chấm theo descriptor (trừ ở TA/TR) và ghi
  vào `ghi_chu`. Bài học thuộc lòng nguyên văn: ghi `ghi_chu` và không đưa vào bộ so sánh.

**Mẫu bảng gửi lại** (một dòng một bài; Excel hoặc Google Sheet đều được):

| Cột | Ghi gì |
| --- | --- |
| `task` | `TASK_1` hoặc `TASK_2` |
| `prompt` | Đề bài đầy đủ |
| `chart_facts` | Task 1: số liệu/mô tả biểu đồ. Task 2: để trống |
| `essay` | Bài viết nguyên văn, không sửa lỗi |
| `band_tong` | 0 đến 9, bước 0.5 |
| `band_TA_hoac_TR`, `band_CC`, `band_LR`, `band_GRA` | Mỗi cột 0 đến 9, bước 0.5 |
| `ma_loi` (không bắt buộc) | Các mã lỗi, cách nhau bằng dấu phẩy |
| `nguoi_cham` | Tên hoặc mã người chấm (để tách chấm riêng) |
| `ghi_chu` (không bắt buộc) | Bài khó chấm vì sao, nếu có |

## Việc B: xem lại rubric nháp

Rubric (tiêu chí, mô tả từng band, bài ví dụ) mà AI đang dùng **do AI soạn dựa trên band descriptor công khai, chưa có
chuyên gia duyệt**. Nhờ anh/chị xem:
- Mô tả band 4 đến 9 của từng tiêu chí có đúng tinh thần IELTS không, chỗ nào sai hoặc mơ hồ.
- Bài ví dụ trong rubric có đúng band đã gán không.

Nhóm sẽ gửi file rubric kèm; anh/chị đánh dấu chỗ cần sửa là đủ, nhóm tự nhập lại vào hệ thống.

## Sau khi nhận bảng điểm, nhóm làm gì

Nhập bài và điểm vào hệ thống, cho AI chấm cùng bộ bài, rồi so sánh. AI được phép bật cho học viên khi **cả hai điều
kiện** đạt:
- **Ít nhất 80%** số bài AI lệch không quá 0.5 band so với chuyên gia;
- **QWK ≥ 0.60** (chỉ số đồng thuận, 1 là trùng hoàn toàn).

Nếu chưa đạt, nhóm chỉnh rubric và prompt rồi chạy lại. Kết quả (số bài, độ lệch, QWK, bài lệch nhiều nhất) sẽ gửi lại
anh/chị xem.

## Lưu ý

- **Số bài ít thì kết quả dao động.** Báo cáo luôn ghi kèm số bài. Dưới 30 bài mỗi Task chỉ nên coi là thử nghiệm.
- Điểm của anh/chị chỉ dùng để kiểm tra AI, **không hiển thị cho học viên**.
- Bài của học viên thật cần bỏ thông tin nhận dạng trước khi gửi.
- Hệ thống không được dùng điểm giả lập để đóng việc này. Khi chưa có bảng điểm thật, mục "độ chính xác của AI" ở báo
  cáo đồ án phải ghi là **chưa kiểm chứng**.
