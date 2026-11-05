# Checklist kiểm endpoint upload (backend) trước khi merge

Áp dụng cho mọi PR của Codex có endpoint nhận file: phase 3 (`POST /api/content/admin/media/image`), phase 5 (`POST /api/content/admin/vocabulary/covers`), phase 8 (`POST /api/assessments/speaking-recordings`), và mọi endpoint upload khác sau này. Kiểm cả phía backend lẫn phía Supabase, không chỉ một bên.

Kết quả ghi vào mô tả PR, kèm lệnh đã chạy và số liệu thật. Không ghi "đã kiểm" cho bước chưa chạy.

## 1. Mã nguồn và test

- [ ] `mvn -B -ntp -pl <service> -am test` chạy thật; ghi số test chạy, lỗi, bỏ qua.
- [ ] Diff chỉ nằm trong danh sách file của brief. Không sửa test cũ để cho qua.
- [ ] Không có comment, tên test, tên migration nhắc số phase hay mã plan.

## 2. Gọi endpoint thật (không chỉ unit test)

Chạy service trên stack local (hoặc Testcontainers) và gọi bằng `curl` với token từng role.

- [ ] Tác giả hợp lệ + file png/jpeg/webp thật: **201**, body đúng hình (`mediaReference`, `mediaUrl`, `contentType`, `sizeBytes`).
- [ ] File rỗng: **400**.
- [ ] File SVG (khai `image/svg+xml`): **400**.
- [ ] File khai `image/png` nhưng nội dung không phải ảnh: **400**.
- [ ] File lớn hơn giới hạn (2 MB cho ảnh, 5 MB cho ghi âm): **413** hoặc **400**, đúng như brief.
- [ ] Chưa đăng nhập: **401**. Sai role (ví dụ học viên gọi endpoint tác giả): **403**.

## 3. Khóa object và quyền sở hữu

- [ ] Khóa đúng tiền tố (`content-image/`, `catalog-cover/`, `speaking/`), có `{userId}` của người gọi.
- [ ] Khóa không chứa `..`, không bắt đầu bằng `/`.
- [ ] Người dùng A không gán được khóa của người dùng B (`requireOwnedBy` từ chối).
- [ ] Đuôi file khớp nội dung thật (`ContentTypes`), không lấy từ tên file client gửi.

## 4. Object thật trên Supabase

- [ ] Object xuất hiện đúng bucket, đúng khóa (kiểm bằng truy vấn chỉ đọc `storage.objects`).
- [ ] Tải qua URL công khai (hoặc URL ký) và so mã băm với file đã gửi: khớp từng byte.
- [ ] Bucket riêng tư (ghi âm Speaking): URL công khai bị chặn; URL ký hết hạn đúng thời gian.

## 5. Bản ghi trong DB

- [ ] Cột lưu **khóa**, không lưu URL đầy đủ (trừ dữ liệu cũ `https://`).
- [ ] Đọc lại qua API trả URL đầy đủ, đúng base URL.
- [ ] `CONTENT_MEDIA_BASE_URL` trống: API vẫn trả 200, trường ảnh là `null`, có cảnh báo trong log.

## 6. Lỗi và dọn dẹp

- [ ] Lưu trữ lỗi (ví dụ `UnavailableObjectStorage` hoặc Supabase trả 5xx): trả **503**, không tạo bản ghi DB mồ côi.
- [ ] Lỗi giữa chừng (timeout không rõ kết quả): đúng theo quy tắc `outcomeUnknown` của `common-storage`.
- [ ] Log không chứa khóa bí mật, không chứa URL ký, không chứa nội dung file.

## 7. Frontend (nếu PR có)

- [ ] `npm run typecheck`, `npm run lint`, test Vitest của form.
- [ ] Thử tay một lượt với file thật: chọn file, upload, thấy ảnh xem trước, lưu, tải lại trang vẫn thấy.
- [ ] Thông báo lỗi 400/413/503 hiển thị đúng.

## Kết luận

Chỉ đánh dấu "đạt" khi mọi mục áp dụng đều có bằng chứng (lệnh và kết quả thật). Mục nào không kiểm được thì ghi rõ lý do và báo lại người quyết định.
