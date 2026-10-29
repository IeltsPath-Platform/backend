# Đề xuất chia service cho nền tảng luyện IELTS

- **Ngày:** 18/09/2026.
- **Trạng thái:** Đề xuất để thảo luận; chưa phải quyết định triển khai đã được phê duyệt.
- **Phạm vi:** Phân chia trách nhiệm và dữ liệu cho MVP theo điều chỉnh của người dùng: bỏ phần gia sư khỏi phương án này và dùng `payment-service` cho thanh toán, gói và quyền lợi.
- **Đầu vào:** `C:\Users\LOQ\Downloads\02-dac-ta-tong-hop.md`, [README dự án](../../README.md) và [quy tắc kiến trúc](../../AGENTS.md).
- **Thông tin còn thiếu:** Quy mô nhóm backend và thời hạn hoàn thành MVP.

## 1. Đề xuất tổng quan

Đề xuất **8 service: 7 service nghiệp vụ và 1 service thông báo**. Đây là cách chia trách nhiệm đích; nên triển khai theo từng luồng hoàn chỉnh.

Backend hiện mới có `user-service` cùng Gateway, Config Server, Eureka và thư viện `common-security`. Các tính năng được ghi “đã có” trong đặc tả chưa đồng nghĩa đã có trong backend này.

### Điều chỉnh theo yêu cầu người dùng

- Loại phần gia sư khỏi danh sách service và phạm vi triển khai của phương án này.
- Dùng `payment-service` để quản lý thanh toán, đồng thời giữ trách nhiệm quản lý gói, quyền lợi và quota.
- Các service còn lại giữ ranh giới trách nhiệm đã đề xuất.

Chưa có thông tin quy mô nhóm nên thứ tự triển khai và phân công cần cân đối với đội ngũ và mức độ phụ thuộc giữa các nghiệp vụ; đây cũng là các tiêu chí trong [hướng dẫn xác định ranh giới microservice của Microsoft](https://learn.microsoft.com/en-us/azure/architecture/microservices/model/microservice-boundaries).

## 2. Danh sách service và dữ liệu sở hữu

| Service | Trách nhiệm | Dữ liệu sở hữu |
| --- | --- | --- |
| **`user-service`** — mở rộng service hiện có | Đăng ký, đăng nhập, OAuth, quên mật khẩu, hồ sơ cơ bản, consent, khóa/mở tài khoản. | User, role, thông tin xác thực, liên kết OAuth, tùy chọn riêng tư. |
| **`content-service`** | Kho đề/câu hỏi, từ điển, ngữ pháp, nhãn kiến thức, định nghĩa cây chủ đề; nội dung nghe/phát âm/game; import, duyệt, phát hành, thu hồi và version nội dung. Phục vụ §7 và phần nội dung của §2–6. | Câu hỏi, đáp án, giải thích, passage, từ vựng, metadata audio, điểm kiến thức, phiên bản và trạng thái duyệt. |
| **`assessment-service`** | Thi đầu vào, thi thử, luyện ngắn, lượt quiz/game; autosave, thời gian, nộp bài, chấm Reading/Listening, tiếp nhận Writing/Speaking và tổng hợp kết quả. | Phiên làm bài, câu trả lời, thời gian, bản nội dung dùng khi làm bài, bài nộp và kết quả. |
| **`learning-service`** | Mục tiêu học, FSRS, mức thành thạo, mở khóa chủ đề, kế hoạch ngày, streak, dashboard cá nhân, ghi chú, sổ tay lỗi sai, flashcard và nhập tiến độ cũ. | Mục tiêu, lịch ôn, lịch sử học, tiến độ trên cây chủ đề, ghi chú/thẻ cá nhân và trạng thái nhập dữ liệu. |
| **`ai-service`** | Chấm Writing/Speaking, ASR, sinh bài theo yêu cầu, trợ lý RAG; retry, chấm lại, hiệu chỉnh và theo dõi chi phí. | Tác vụ AI, transcript, phản hồi có phiên bản, prompt/rubric version, lịch sử xử lý và chi phí. |
| **`community-service`** | Bài viết, bình luận, thích/theo dõi, nhóm học, thử thách, kiểm duyệt và bảng xếp hạng. | Nội dung cộng đồng, thành viên nhóm, báo cáo vi phạm, quyết định kiểm duyệt và điểm xếp hạng. |
| **`payment-service`** | Thanh toán mua/gia hạn gói, xử lý kết quả thanh toán; quản lý gói Free/Pro/Pro+, quyền sử dụng tính năng, hạn mức thi/AI/RAG, đăng ký gói và điều chỉnh thủ công của admin. | Giao dịch và trạng thái thanh toán, gói, quyền lợi, đăng ký gói, lượt sử dụng, quota và lịch sử điều chỉnh. |
| **`notification-service`** | Thông báo in-app, email, push; giờ nhận, bật/tắt kênh, lịch gửi và retry. | Tùy chọn nhận, thiết bị push, thông báo và trạng thái giao nhận. |

### Nguyên tắc sở hữu dữ liệu

- Mỗi service sở hữu database và domain model riêng theo quy tắc repo.
- Các service tham chiếu nhau bằng ID và hợp đồng API.
- Không đọc trực tiếp bảng hoặc dùng chung JPA entity giữa các service.
- Domain bảo vệ quy tắc nghiệp vụ; application điều phối use case; infrastructure triển khai persistence và tích hợp bên ngoài.
- Chỉ `user-service` đã tồn tại trong danh sách trên. Các trách nhiệm mở rộng và bảy service còn lại là nội dung đề xuất.

## 3. Các ranh giới cần giữ rõ

### 3.1. Nội dung và tiến độ

`content-service` định nghĩa điểm kiến thức và cây chủ đề. `learning-service` biết từng người đã học, thành thạo và mở khóa đến đâu.

Ví dụ: định nghĩa chủ đề “Daily Life” thuộc content; trạng thái một người đã hoàn thành chủ đề đó thuộc learning.

### 3.2. Làm bài và học tập

`assessment-service` lưu kết quả của từng lượt làm. `learning-service` sử dụng kết quả để cập nhật điểm yếu, lịch ôn và kế hoạch ngày.

Phiên làm bài cần gắn với phiên bản nội dung đã sử dụng để việc sửa đề sau này không làm thay đổi kết quả cũ.

### 3.3. Quiz/game và bảng xếp hạng

- Content cung cấp bộ câu hỏi và nội dung.
- Assessment xác minh lượt chơi, câu trả lời, thời gian và điểm.
- Community áp dụng luật xếp hạng như “chỉ tính lần đầu”.
- Learning sử dụng kết quả phù hợp để cập nhật tiến độ.

Điểm nhập từ máy người học không tự trở thành điểm thi đua đã xác minh.

### 3.4. AI và duyệt nội dung

AI sinh bản nháp; content quyết định duyệt và phát hành. Mọi câu AI phải được duyệt trước khi đưa tới người học.

RAG chỉ sử dụng nội dung được phép, kể cả khi nội dung bị thu hồi sau đó. Phản hồi chấm AI có phiên bản; assessment tiếp nhận kết quả chấm để tổng hợp kết quả bài làm.

### 3.5. Lịch học và thông báo

Learning quyết định khi nào cần nhắc ôn hoặc nhắc học. Notification chịu trách nhiệm gửi đúng giờ, đúng kênh và theo tùy chọn người nhận.

### 3.6. Thanh toán, gói sử dụng và role bảo mật

`payment-service` sở hữu giao dịch thanh toán, đăng ký gói và quyền lợi Free/Pro/Pro+. Role bảo mật hiện tại vẫn là `ADMIN`, `LEARNER`.

Việc kích hoạt quyền lợi từ thanh toán phải dựa trên kết quả được backend xác minh. Xử lý thông báo thanh toán lặp phải bảo đảm không kích hoạt hoặc gia hạn gói nhiều lần cho cùng một giao dịch.

Backend phải kiểm tra và ghi nhận quota, chống tính lượt trùng khi retry. Không chỉ dựa vào việc frontend ẩn nút hoặc chặn thao tác.

## 4. Admin và hạ tầng dùng chung

### API quản trị thuộc service sở hữu nghiệp vụ

| Chức năng admin | Service chịu trách nhiệm |
| --- | --- |
| Quản lý tài khoản, khóa/mở người dùng | `user-service` |
| Quản lý câu hỏi, đề, nhãn kiến thức, duyệt và phát hành | `content-service` |
| Vận hành chấm AI, retry, hiệu chỉnh, theo dõi chi phí | `ai-service` |
| Kiểm duyệt bài viết, quản lý nhóm và thử thách | `community-service` |
| Theo dõi giao dịch thanh toán, quản lý gói, đăng ký gói và quota | `payment-service` |

Audit được ghi tại nơi thực hiện thay đổi. Dashboard vận hành tổng hợp qua API. Chưa cần tạo một `admin-service` chứa toàn bộ nghiệp vụ.

### Cấu trúc thư mục dự kiến

```text
backend/
├── infra/                         # Đã có
│   ├── api-gateway/
│   ├── config-server/
│   └── eureka-server/
├── shared/
│   └── common-security/           # Thư viện đã có
└── services/
    ├── user-service/              # Đã có, cần mở rộng
    ├── content-service/           # Đề xuất mới
    ├── assessment-service/        # Đề xuất mới
    ├── learning-service/          # Đề xuất mới
    ├── ai-service/                # Đề xuất mới
    ├── community-service/         # Đề xuất mới
    ├── payment-service/           # Đề xuất mới
    └── notification-service/      # Đề xuất mới
```

Tám service đề xuất ở đây không bao gồm ba service hạ tầng. `common-security` là thư viện, không phải service deploy độc lập.

## 5. Thứ tự triển khai đề xuất

### Bước 1 — Chốt dữ liệu nội dung

- Lấy file mẫu để thiết kế câu hỏi, nhãn kiến thức và phiên bản nội dung.
- Xác định người chịu trách nhiệm duyệt nội dung AI.
- Rà soát phần user và engine thi cũ có thể tái sử dụng.

### Bước 2 — Làm luồng học xuyên suốt

```text
Nội dung đã duyệt
    → Làm bài
    → Nhận kết quả
    → Cập nhật tiến độ và lịch ôn
```

Trọng tâm là `content-service`, `assessment-service` và `learning-service`, kết hợp xác thực của `user-service`.

### Bước 3 — Tích hợp AI, thanh toán, quota và thông báo

- Bổ sung chấm bất đồng bộ, sinh câu có duyệt và RAG.
- Bổ sung thanh toán mua/gia hạn gói và cập nhật quyền lợi từ kết quả được xác minh.
- Kiểm tra quota trước khi thực hiện tác vụ có hạn mức.
- Bổ sung nhắc học và thông báo phù hợp.
- Chống xử lý lặp để retry không cộng điểm, trừ quota hoặc kích hoạt/gia hạn gói nhiều lần.

### Bước 4 — Hoàn thiện community

Phần community có thể phát triển song song sau khi thống nhất hợp đồng user, content và kết quả đã xác minh.

**Community vẫn thuộc MVP trong phương án này.** Thứ tự triển khai trên không chuyển phần này sang sau MVP.

## 6. Các điểm cần chốt trước thiết kế chi tiết

| Chủ đề | Hiện trạng và quyết định còn mở |
| --- | --- |
| **Quy mô nhóm và thời hạn** | Chưa có thông tin để cân đối số service triển khai đồng thời hoặc phân công nhân sự cụ thể. |
| **FSRS** | Đặc tả chọn `ts-fsrs`, một [thư viện TypeScript](https://github.com/open-spaced-repetition/ts-fsrs), trong khi repo dùng Java. Giữ trách nhiệm tại learning; lựa chọn cách tích hợp cần quyết định riêng. |
| **Thanh toán** | Đã chọn `payment-service` trong phương án. Cần chốt cổng thanh toán, quy trình mua/gia hạn gói, cách xác minh kết quả và phạm vi hoàn tiền/đối soát trước khi thiết kế chi tiết. |
| **Dữ liệu mẫu** | Chưa có file mẫu thật để chốt schema nội dung và quy trình import. |
| **Người duyệt AI** | Chưa rõ thầy hay team chịu trách nhiệm duyệt câu hỏi và rà soát phản hồi. Không tự thêm role mới vào contract hiện tại. |
| **Hạ tầng AI/media** | Lưu audio, xử lý tác vụ nền và xác thực giữa các service cần thiết kế thêm. Repo chưa có broker hoặc object storage; tài liệu này chưa chọn công nghệ mới. |
| **Tái sử dụng engine thi** | Cần kiểm tra mã nguồn engine cũ trước khi ước lượng phần “mở rộng” trong đặc tả. |

## 7. Phạm vi Web và Mobile

- Web và Mobile dùng chung các service backend.
- Full exam, thi thử và bài kiểm tra đầu vào đầy đủ thực hiện trên **Web**.
- Mobile phục vụ học hằng ngày, luyện ngắn, game, lộ trình, tra cứu, cộng đồng và xem kết quả.
- Mobile có thể nộp Speaking ngoài ngữ cảnh kỳ thi đầy đủ và xem phản hồi Writing/Speaking.
- Các mục đã hoãn trong đặc tả, như Speaking AI video realtime hoặc quiz PvP realtime, không được tự đưa vào MVP.

## 8. Tiêu chí kiểm tra đề xuất trước khi triển khai

- Mỗi tính năng MVP trong phạm vi đã điều chỉnh có service chịu trách nhiệm và chủ sở hữu dữ liệu rõ ràng.
- Phân biệt được nội dung gốc, kết quả làm bài, tiến độ học và dữ liệu xếp hạng.
- Không service nào cần đọc database của service khác.
- AI không phát hành câu hỏi chưa duyệt; quota và điểm không bị tính lại do retry.
- Quyền lợi từ thanh toán chỉ được kích hoạt sau khi xác minh; thông báo lặp không làm gia hạn gói nhiều lần.
- Giữ giới hạn full exam trên Web.
- Chốt các vấn đề còn mở ở mục 6 trước khi chuyển thành kế hoạch triển khai chi tiết.
