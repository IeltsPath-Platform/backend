# Kịch bản kiểm thử thủ công: Cộng đồng và Thông báo realtime

- Ngày: 2026-10-18. Dùng sau khi build lại 4 image (community, notification, user, gateway).
- Cách ghi: mỗi ca có **Các bước** và **Kết quả mong đợi**. Đánh dấu `[ ]` khi đạt, ghi chú khi lỗi (kèm ảnh chụp màn hình, giờ, và nội dung tab Network nếu có).
- Chữ trong ngoặc kép là chữ thật trên giao diện.

## 0. Chuẩn bị

### 0.1 Chạy hệ thống
```powershell
# Backend (thư mục backend)
docker compose up -d
docker compose start postgres rabbitmq     # nếu Docker vừa khởi động lại, hai container này có thể đang tắt
docker ps --format "{{.Names}} {{.Status}}"   # tất cả phải "healthy"

# Frontend (thư mục frontend)
npm run dev                                 # http://localhost:5173
```
- Sau khi tạo lại container, **chờ 1 đến 2 phút** cho Gateway định tuyến (503 tạm thời là bình thường).
- Máy có volume Postgres cũ mà chưa có `notification_db`: tạo bằng lệnh ở mục 7 của `community-changes-and-api.md`.

### 0.2 Tài khoản
| Tên gọi trong kịch bản | Tài khoản | Ghi chú |
| --- | --- | --- |
| **A** (học viên) | `learner@ielts.demo` / `Demo@123` | Cửa sổ trình duyệt thường |
| **B** (ADMIN) | `admin@ielts.demo` / `Demo@123` | Cửa sổ **ẩn danh** |
| **C** (học viên mới) | Tự đăng ký ở trang đăng ký, ví dụ `c.test@example.com` | Cửa sổ ẩn danh thứ hai hoặc trình duyệt khác |

Lưu ý: A và B dùng chung trình duyệt thì đăng nhập đè nhau, nên **bắt buộc dùng cửa sổ thường cho A và ẩn danh cho B**.

### 0.3 Dữ liệu mẫu để dán
- Ảnh hợp lệ (host được phép): `https://images.unsplash.com/photo-1506744038136-46273834b3fb.jpg` và `https://i.imgur.com/abc123.jpg`.
- Ảnh bị từ chối: `https://example.com/a.jpg`, `http://images.unsplash.com/a.jpg` (không HTTPS), `https://picsum.photos/200`.
- Video hợp lệ: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`, `https://youtu.be/dQw4w9WgXcQ`.
- Video bị từ chối: `javascript:alert(1)`, `https://vimeo.com/123`.

## 1. Đăng nhập và điều hướng

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 1.1 | Chưa đăng nhập, mở `/community` | Chuyển về trang đăng nhập, không lộ nội dung |
| 1.2 | Đăng nhập A, bấm mục "Cộng đồng" trên thanh điều hướng | Vào `/community`, tiêu đề "Cùng nhau tiến bộ", có ô tìm kiếm, tab "Tất cả" và "Đang theo dõi", các chip chủ đề |
| 1.3 | Tải lại trang (F5) | Vẫn đăng nhập, bảng tin tải lại bình thường |
| 1.4 | Mở DevTools, tab Console | Không có lỗi đỏ (cảnh báo 401 của `/auth/refresh` lúc chưa đăng nhập là chấp nhận được) |

## 2. Đăng bài

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 2.1 | A bấm "Bài viết mới", chọn danh mục "Chung", nhập tiêu đề và nội dung, gửi | Bài xuất hiện ở đầu bảng tin, đúng tên A, đúng danh mục, thời gian vừa xong |
| 2.2 | Thử gửi form để trống tiêu đề hoặc nội dung | Có thông báo lỗi bằng tiếng Việt, không gửi |
| 2.3 | Đăng bài với "Band mục tiêu" 7.5 | Hiện huy hiệu `Target 7.5` cạnh tên tác giả (nếu giao diện hiển thị) |
| 2.4 | Dán 1 ảnh hợp lệ (mục 0.3) | Ảnh hiển thị trong bài |
| 2.5 | Dán 4 ảnh hợp lệ | Lưới ảnh gọn, đủ 4 ảnh |
| 2.6 | Dán 5 ảnh | Bị từ chối (thông báo tối đa 4) |
| 2.7 | Dán ảnh bị từ chối (`example.com`, `http://`, `picsum.photos`) | Bị từ chối, bài không được tạo |
| 2.8 | Dán link YouTube hợp lệ | Hiện khung xem video nhúng an toàn hoặc liên kết |
| 2.9 | Dán `javascript:alert(1)` làm video | Bị từ chối, không có cửa sổ cảnh báo nào bật lên |
| 2.10 | Đăng bài có nội dung `<script>alert(1)</script>` và `<b>đậm</b>` | Hiển thị thành **văn bản thô**, không chạy script, không in đậm |
| 2.11 | Sửa bài của chính A (menu bài viết) | Nội dung đổi, hiện sau khi tải lại |
| 2.12 | Với B, mở bài của A | Không có menu sửa/xóa của A (hoặc bị từ chối nếu thử gọi) |
| 2.13 | A xóa bài của mình | Bài biến mất, tải lại vẫn không có; mở link trực tiếp `/community/posts/{id}` báo không tìm thấy |

## 3. Bảng tin và cuộn vô tận

Chuẩn bị: cần ít nhất 25 bài. Tạo nhanh bằng PowerShell hoặc đăng tay (xem mục 12 cho lệnh tạo hàng loạt).

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 3.1 | Mở `/community`, cuộn xuống đáy | Tải thêm 10 bài mỗi lượt, có hiệu ứng chờ (skeleton), không nhảy vị trí cuộn |
| 3.2 | Cuộn đến hết | Hiện "Bạn đã xem hết bảng tin.", không gọi thêm |
| 3.3 | Đếm bài, tìm trùng lặp | Mỗi bài xuất hiện đúng 1 lần |
| 3.4 | Bấm từng chip chủ đề (Chung, Hỏi đáp, Thảo luận, Chia sẻ Writing, Tìm bạn Speaking, Tài nguyên & mẹo) | Chỉ hiện bài đúng danh mục; "Tất cả chủ đề" trả về đủ |
| 3.5 | Tắt mạng (DevTools, Offline) rồi cuộn để tải thêm | Hiện thông báo lỗi và nút "Thử lại"; bật mạng bấm "Thử lại" thì tải được |
| 3.6 | Chưa có bài nào trong danh mục | Có trạng thái rỗng, không trống trơn |

## 4. Cảm xúc (nút "Thích" kiểu Facebook)

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 4.1 | Nhìn thanh hành động của một bài chưa ai thả cảm xúc | Chỉ có nút "Thích" (icon bàn tay). **Không** có dòng "0 Thích / 0 Yêu thích / 0 Hữu ích / 0 Chính xác" |
| 4.2 | Rê chuột vào nút "Thích" và giữ khoảng nửa giây | Popup nhỏ hiện phía trên với 4 icon màu (Thích, Yêu thích, Hữu ích, Chính xác), có hiệu ứng trồi lên và nảy |
| 4.3 | Rê chuột qua từng icon trong popup | Icon phóng to và nhấc lên, hiện nhãn tên |
| 4.4 | Di chuột ra ngoài khỏi nút và popup | Popup đóng sau một khoảng ngắn, không chớp |
| 4.5 | Chọn "Yêu thích" | Có hiệu ứng hạt nổ; nút chính đổi thành "Yêu thích" màu hồng đỏ |
| 4.6 | Tải lại trang | Vẫn là "Yêu thích" |
| 4.7 | Bấm nhanh nút chính khi đang là "Yêu thích" | Bỏ cảm xúc, nút về "Thích" mặc định |
| 4.8 | Bấm nhanh nút "Thích" khi chưa chọn | Bật "Thích"; bấm lại thì tắt |
| 4.9 | Với tài khoản khác thả cảm xúc lên cùng bài | Ở A hiện dòng tóm tắt gồm vài icon chồng nhau và tổng số; rê vào dòng đó thấy chi tiết từng loại (chỉ loại lớn hơn 0) |
| 4.10 | Bài chưa có cảm xúc nào | Không hiện dòng tóm tắt |
| 4.11 | Bàn phím: Tab đến nút "Thích", nhấn mũi tên xuống | Popup mở, focus vào icon đầu tiên |
| 4.12 | Nhấn mũi tên phải/trái, rồi Enter | Focus di chuyển giữa 4 icon; Enter chọn và đóng popup, focus về nút chính |
| 4.13 | Mở popup rồi nhấn Escape | Đóng, focus về nút chính |
| 4.14 | Điện thoại hoặc giả lập cảm ứng: nhấn giữ nút khoảng nửa giây | Popup mở, chạm icon để chọn, chạm ra ngoài thì đóng |
| 4.15 | Windows: bật "Hiệu ứng hoạt hình" tắt (Cài đặt, Trợ năng), tải lại | Không còn hiệu ứng nảy hay hạt nổ, vẫn dùng được |
| 4.16 | Ngắt backend rồi thả cảm xúc | Giao diện hoàn về trạng thái cũ và có thông báo lỗi |

## 5. Bình luận và trả lời

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 5.1 | A mở bình luận của một bài, viết bình luận, gửi | Bình luận hiện ngay, đúng tên A, số bình luận tăng |
| 5.2 | B trả lời bình luận của A | Hiện thụt vào dưới bình luận gốc |
| 5.3 | A trả lời lại trả lời của B | Vẫn hiện dưới bình luận **gốc** (chỉ 2 cấp), có `@tên` người được trả lời |
| 5.4 | Bài có nhiều trả lời | Có "Xem thêm n phản hồi" hoặc tải thêm; không thiếu, không lặp |
| 5.5 | A sửa bình luận của mình | Nội dung đổi |
| 5.6 | B thử sửa/xóa bình luận của A (qua giao diện không có nút; qua API trả 403) | Bị chặn |
| 5.7 | A xóa bình luận của mình | Biến mất, số bình luận giảm |
| 5.8 | Bình luận chứa `<img src=x onerror=alert(1)>` | Hiện thành chữ thô, không chạy mã |
| 5.9 | Bình luận chỉ có dấu cách hoặc dài hơn 5000 ký tự | Bị từ chối hoặc bị giới hạn ở ô nhập |

## 6. @Mention

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 6.1 | Trong ô bình luận gõ `@` rồi 2 chữ đầu tên một người (ví dụ `@Demo`) | Hiện danh sách gợi ý tối đa 8 người, không có chính mình |
| 6.2 | Dùng mũi tên lên/xuống rồi Enter | Chèn đúng người; ô nhập chứa mention; danh sách đóng |
| 6.3 | Nhấn Escape khi danh sách đang mở | Danh sách đóng |
| 6.4 | Gửi bình luận có mention | Bình luận hiển thị `@Tên` (nổi bật), **không lộ** chuỗi `@[Tên](uuid)` |
| 6.5 | Đăng nhập người được mention | Chuông tăng 1, thông báo "Bạn được nhắc trong một bình luận"; bấm vào đi đến đúng bài |
| 6.6 | Mention cùng một người 3 lần trong một bình luận | Người đó chỉ nhận **1** thông báo |
| 6.7 | Tự mention chính mình | Bình luận vẫn đăng, chính mình **không** nhận thông báo mention |
| 6.8 | Mention 5 người khác nhau | Thành công, mỗi người nhận 1 thông báo |
| 6.9 | Mention 6 người khác nhau | Bị từ chối (tối đa 5) |
| 6.10 | Mention người không tồn tại (sửa tay UUID) | Bị từ chối |
| 6.11 | Gõ `@` rồi chỉ 1 ký tự | Chưa hiện gợi ý (cần ít nhất 2 ký tự) |

## 7. Lưu bài (bookmark)

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 7.1 | A bấm "Lưu" ở một bài | Nút đổi trạng thái đã lưu |
| 7.2 | Vào mục "Đã lưu" (cột trái hoặc `/community?tab=bookmarks`) | Thấy bài vừa lưu |
| 7.3 | Tải lại | Trạng thái lưu vẫn còn |
| 7.4 | Bỏ lưu | Bài biến khỏi "Đã lưu" |
| 7.5 | B mở "Đã lưu" | Không thấy bài đã lưu của A (riêng tư theo tài khoản) |

## 8. Theo dõi và hồ sơ

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 8.1 | A bấm tên tác giả (B) để mở hồ sơ | Hiện avatar chữ cái, tên, giới thiệu, số người theo dõi và đang theo dõi, nút "Theo dõi" |
| 8.2 | Bấm "Theo dõi" | Nút đổi tức thì thành đang theo dõi; số người theo dõi tăng 1 |
| 8.3 | Tải lại | Trạng thái giữ nguyên |
| 8.4 | Tab "Người theo dõi" của B | Có A |
| 8.5 | Tab "Đang theo dõi" của A (mở hồ sơ A) | Có B |
| 8.6 | Tab "Bài viết" của hồ sơ | Danh sách bài của người đó, cuộn vô tận |
| 8.7 | Với B: thông báo | Có "Một học viên đã theo dõi bạn"; bấm vào đi đến hồ sơ của A |
| 8.8 | A bấm lại "Theo dõi" liên tục nhanh nhiều lần | Không tạo thông báo trùng, trạng thái cuối cùng đúng |
| 8.9 | A bỏ theo dõi | Số giảm; **thông báo đã gửi không bị thu hồi** (đúng theo thiết kế) |
| 8.10 | Mở hồ sơ của chính A | Không có nút tự theo dõi |
| 8.11 | Tab "Đang theo dõi" ở bảng tin khi A chưa theo dõi ai | Trạng thái rỗng: "Bạn chưa có bài viết từ người đang theo dõi" kèm gợi ý người nên theo dõi |
| 8.12 | A theo dõi B rồi mở tab "Đang theo dõi" | Chỉ thấy bài của B, mỗi bài một lần; đổi qua lại hai tab nhiều lần không nhân đôi bài |

## 9. Tìm kiếm

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 9.1 | Đăng bài có tiêu đề chứa từ hiếm `zebraword123`; tìm `zebraword123` và Enter | Chỉ hiện bài đó; URL có `?q=zebraword123` |
| 9.2 | Tìm từ chỉ có trong **nội dung** bài | Tìm thấy bài đó |
| 9.3 | Tìm chuỗi không tồn tại | "Không tìm thấy bài viết phù hợp" |
| 9.4 | Tìm `a&b\|c'!(d):*<->` | Không lỗi, hiện rỗng hoặc kết quả hợp lệ |
| 9.5 | Dán chuỗi dài hơn 120 ký tự | Ô nhập tự cắt ở 120 ký tự |
| 9.6 | Từ khóa kết hợp chip "Hỏi đáp" | Chỉ còn bài hỏi đáp khớp |
| 9.7 | Từ khóa kết hợp tab "Đang theo dõi" rồi đổi qua "Tất cả" | Từ khóa **được giữ** trong ô và URL |
| 9.8 | Có hơn 10 bài khớp, cuộn xuống | Tải tiếp, không thiếu, không lặp |
| 9.9 | Xóa từ khóa (xóa hết chữ, Enter) | Quay về bảng tin thường, URL không còn `q=` |
| 9.10 | Trên điện thoại (390 px) | Ô tìm kiếm nằm gọn trong màn hình |

## 10. Thông báo realtime

Mở **hai cửa sổ cùng lúc**: A (cửa sổ thường) đứng ở `/community`, B (ẩn danh) thực hiện hành động.

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 10.1 | Ở A mở DevTools, tab Network, lọc "WS" | Có kết nối `/ws/notifications` trạng thái 101, thấy khung `CONNECTED` |
| 10.2 | B bình luận vào bài của A | Trong vài giây (không tải lại) chuông của A hiện số **1**, có toast "Bạn có thông báo mới." |
| 10.3 | B thả cảm xúc vào bài của A | Số tăng thêm 1 |
| 10.4 | B trả lời bình luận của A | Có thông báo trả lời |
| 10.5 | B theo dõi A | Có thông báo "Một học viên đã theo dõi bạn" |
| 10.6 | A bấm chuông | Danh sách thông báo, mới nhất ở trên, mục chưa đọc nổi bật |
| 10.7 | Bấm một thông báo | Đi đến đúng bài hoặc hồ sơ; thông báo đó thành đã đọc; số giảm 1 |
| 10.8 | Bấm "đọc tất cả" | Số về 0 |
| 10.9 | Tải lại trang | Số chưa đọc và danh sách khớp với trước khi tải lại |
| 10.10 | B tự thả cảm xúc hoặc bình luận vào bài của **chính B** | B **không** nhận thông báo |
| 10.11 | Tắt Wi-Fi của máy A khoảng 30 giây, B bình luận 2 lần, bật lại | Sau khi kết nối lại, chuông và danh sách có đủ 2 thông báo (không mất) |
| 10.12 | A đăng xuất | Kết nối WebSocket đóng; không còn nhận thông báo |
| 10.13 | Mở 2 tab của A | Cả hai tab đều nhận thông báo, mỗi tab một lần |
| 10.14 | Đăng nhập A ở tab này và C ở tab kia (hai trình duyệt khác nhau); B bình luận bài của A | Chỉ A nhận; C không nhận gì |

## 11. Tùy chọn thông báo

Vào "Tùy chọn" trong ngăn chuông hoặc `/notifications/preferences`. Dùng tài khoản C (mới) để thấy mặc định.

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 11.1 | C mở trang tùy chọn | Có 5 công tắc, **tất cả bật**: "Bình luận bài viết", "Phản hồi bình luận", "Được nhắc trong bình luận", "Cảm xúc bài viết", "Lượt theo dõi mới" |
| 11.2 | Tắt "Cảm xúc bài viết", tải lại trang | Công tắc vẫn tắt |
| 11.3 | B thả cảm xúc vào bài của C | C **không** nhận thông báo: chuông không nhảy, không có trong danh sách |
| 11.4 | B bình luận vào bài của C | C vẫn nhận (loại khác còn bật) |
| 11.5 | Bật lại "Cảm xúc bài viết"; B đổi sang một cảm xúc khác | C nhận thông báo mới đúng 1 lần; các cảm xúc đã bỏ lỡ lúc tắt **không** xuất hiện sau đó |
| 11.6 | Tắt cả 5 công tắc; B làm đủ 5 loại hành động | C không nhận thông báo nào, số chưa đọc không đổi |
| 11.7 | Bật chọn lọc chỉ "Cảm xúc" và "Được nhắc" | Chỉ hai loại đó đến |
| 11.8 | A (tài khoản khác) mở trang tùy chọn | A vẫn thấy cấu hình của **A**, không bị ảnh hưởng bởi C |
| 11.9 | Ngắt backend, bật tắt một công tắc | Công tắc hoàn về trạng thái cũ và hiện "Không thể lưu tùy chọn. Hãy thử lại." |
| 11.10 | Mobile 390 px | Trang không cuộn ngang, công tắc bấm được |

## 12. Báo cáo vi phạm và hàng chờ ADMIN

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 12.1 | A thấy nút "Báo cáo" ở bài của B | Có |
| 12.2 | A bấm "Báo cáo", hộp thoại "Báo cáo bài viết" mở | Có ô lý do, đếm ký tự `n/1000`, nút "Hủy" và "Gửi báo cáo" |
| 12.3 | Gửi khi để trống lý do | Không gửi; có nhắc nhập lý do |
| 12.4 | Nhập lý do rồi gửi | "Đã gửi báo cáo để ADMIN xem xét." |
| 12.5 | A báo cáo **lại** cùng bài | "Bạn đã báo cáo nội dung này." |
| 12.6 | Người khác (C) báo cáo cùng bài | Thành công |
| 12.7 | A thử báo cáo bài của chính A | Không có nút hoặc bị chặn |
| 12.8 | Báo cáo một bình luận | Thành công; hộp thoại ghi "Báo cáo bình luận" |
| 12.9 | A (không phải ADMIN) mở `/community/admin/reports` | "Bạn không có quyền xem hàng chờ này." |
| 12.10 | B (ADMIN) mở `/community/admin/reports` | "Hàng chờ báo cáo", mỗi mục hiện loại nội dung, thời gian, tiêu đề hoặc đoạn trích, lý do, **tên** người báo cáo và tác giả (không phải UUID thô), nút "Mở nội dung", "Bỏ qua", "Ẩn nội dung" |
| 12.11 | Cũ nhất đứng đầu | Đúng thứ tự thời gian tăng dần |
| 12.12 | B bấm "Bỏ qua" với một báo cáo | Báo cáo biến khỏi hàng chờ; bài **vẫn** hiển thị ở bảng tin; các báo cáo khác về bài đó **còn nguyên** |
| 12.13 | B bấm "Ẩn nội dung" với bài có 3 báo cáo | Cả 3 báo cáo biến khỏi hàng chờ; bài biến khỏi bảng tin, tìm kiếm; mở link trực tiếp báo không tìm thấy |
| 12.14 | Ẩn một bình luận | Bình luận biến khỏi bài, số bình luận giảm |
| 12.15 | Hai ADMIN cùng bấm "Ẩn nội dung" cho cùng một bài (hai cửa sổ ADMIN) | Một bên thành công, bên kia nhận lỗi tranh chấp rõ ràng; không có báo cáo kẹt trạng thái dở |
| 12.16 | Mobile 390 px | Trang ADMIN không cuộn ngang, nút bấm được |

## 13. Giao diện và khả năng truy cập

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 13.1 | Thu cửa sổ về 1920, 1366, 1024, 768, 390, 360 px | Không có thanh cuộn ngang, không chữ bị cắt, không phần tử đè nhau; ở 360 và 390 px thanh hành động của bài (Thích, Bình luận, Lưu, Báo cáo) nằm gọn một hàng |
| 13.2 | Nút nổi "Tư vấn miễn phí" trên mobile | Không che nút "Báo cáo" hoặc nút hành động nào |
| 13.3 | Chữ phụ (mô tả dưới tiêu đề, thời gian, mô tả cột phải) | Đọc rõ, đủ tương phản |
| 13.4 | Hàng chip chủ đề trên mobile | Vuốt ngang được, **không** hiện thanh cuộn xám |
| 13.5 | Chỉ dùng bàn phím (Tab, Shift+Tab, Enter, Escape) | Mọi chức năng chính dùng được; vòng focus thấy rõ |
| 13.6 | Trình đọc màn hình (NVDA) hoặc kiểm tra aria bằng DevTools | Nút "Thích" có tên và trạng thái; popup nhận diện là menu; thông báo mới được đọc ("Bạn có thông báo mới.") |
| 13.7 | Bật chế độ giảm chuyển động của hệ điều hành | Hiệu ứng nảy và hạt nổ tắt |

## 14. Phân quyền và bảo mật

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 14.1 | Chưa đăng nhập, gọi `GET /api/community/posts` | 401 |
| 14.2 | Token học viên gọi `GET /api/community/admin/reports` | 403 |
| 14.3 | Học viên sửa hoặc xóa bài và bình luận của người khác | 403 |
| 14.4 | Học viên đọc hoặc đánh dấu đã đọc thông báo của người khác | 404, không đổi gì |
| 14.5 | Mở WebSocket với token sai hoặc không có token | Bị từ chối (401) |
| 14.6 | Mở WebSocket từ trang web có Origin lạ | Bị từ chối |
| 14.7 | Trong DevTools, xem phản hồi của API hồ sơ và bài viết | Không có email, số điện thoại, mật khẩu, token của người khác |
| 14.8 | Xem kỹ phản hồi `GET /api/users/{id}/public-profile` | Chỉ tên, avatar, bio, band mục tiêu, streak |

## 15. Lỗi và phục hồi

| ID | Các bước | Kết quả mong đợi |
| --- | --- | --- |
| 15.1 | `docker stop ieltspath-rabbitmq-1`, A bình luận bài của B, đợi 10 giây, `docker start ieltspath-rabbitmq-1` | Bình luận vẫn thành công; sau khi RabbitMQ chạy lại, B nhận thông báo trong khoảng 1 phút (outbox giữ và gửi lại) |
| 15.2 | `docker stop ieltspath-notification-service-1`, tạo vài hành động, `docker start` lại | Sau khi service chạy lại, các thông báo được tạo (không mất) |
| 15.3 | `docker stop ieltspath-community-service-1` | Trang Cộng đồng báo lỗi tải, có nút "Thử lại"; không trắng trang |
| 15.4 | Khởi động lại Gateway | WebSocket tự nối lại sau khi Gateway hoạt động; chuông vẫn đúng sau khi nối lại |
| 15.5 | Gọi đường dẫn không tồn tại, ví dụ `/api/community/posts/upload-image` | 404 (không phải 500) |

## 16. Lệnh hỗ trợ (tùy chọn)

Tạo nhanh 25 bài mẫu bằng tài khoản B (PowerShell):
```powershell
$api = 'http://localhost:8080'
$t = (Invoke-RestMethod "$api/auth/login" -Method Post -ContentType 'application/json' -Body (@{email='admin@ielts.demo';password='Demo@123'} | ConvertTo-Json)).accessToken
1..25 | ForEach-Object {
  Invoke-RestMethod "$api/api/community/posts" -Method Post -Headers @{Authorization="Bearer $t"} -ContentType 'application/json' `
    -Body (@{category='GENERAL';title="Bai mau $_";body="noi dung mau $_";imageUrls=@()} | ConvertTo-Json) | Out-Null
}
```

Xóa dữ liệu thử nghiệm (chỉ khi chắc chắn, xóa **toàn bộ** dữ liệu local): `docker compose down -v`, rồi tạo lại `notification_db` như mục 7 của `community-changes-and-api.md`.

## 17. Test tự động đã chạy (2026-10-18)

| Module | Số test | Kết quả |
| --- | --- | --- |
| common-security | 8 | 0 lỗi, 0 bỏ qua |
| api-gateway | 10 | 0 lỗi, 0 bỏ qua |
| user-service | 157 | 0 lỗi, 0 bỏ qua |
| notification-service | 16 | 0 lỗi, 0 bỏ qua |
| community-service | 76 | 0 lỗi, 0 bỏ qua |

Lệnh: `mvn -B -ntp -pl services/community-service,services/notification-service,services/user-service,infra/api-gateway -am test` (cần Docker cho Testcontainers). Kết quả: `BUILD SUCCESS`.

Frontend (thư mục `frontend`): `npm run lint` đạt, `npm run typecheck` đạt, `npm run test` đạt (7 file, 33 test), `npm run build` đạt (có cảnh báo gói JavaScript lớn hơn 500 kB, chưa xử lý).

## 18. Lỗi đã biết, chưa sửa

- `PUT /api/notifications/abc/read` (id sai định dạng) trả 500, nên là 400.
- Chưa có upload ảnh và video từ máy.
- Chuột và phím thật chưa được kiểm tra tự động; các ca có hover, nhấn giữ và bàn phím (mục 4, 6, 13) cần bạn tự thử.
