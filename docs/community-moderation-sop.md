# Quy trình kiểm duyệt báo cáo nội dung Community

## 1. Phạm vi và vai trò xử lý

- **Đối tượng xử lý:** Bài viết (`POST`) và bình luận (`COMMENT`) bị người dùng báo cáo qua `POST /api/community/reports`.
- **Người xử lý:** Quản trị viên (`ADMIN`) sử dụng trang quản trị `ContentReportsAdminPage` (`GET /api/community/admin/reports`, `PUT /api/community/admin/reports/{reportId}/decision`).

## 2. SLA xử lý (24 giờ)

- **Mục tiêu SLA:** Mọi báo cáo ở trạng thái `PENDING` phải được xử lý trong vòng **24 giờ** kể từ `createdAt`.
- **Thứ tự ưu tiên:** Hàng chờ mặc định sắp theo `createdAt,asc;id,asc` (báo cáo cũ nhất lên đầu) để `ADMIN` xử lý theo thứ tự FIFO, tránh quá hạn SLA.
- **Đo lường SLA:** So sánh `decidedAt - createdAt <= 24 giờ` trên các báo cáo đã xử lý (`DISMISSED` hoặc `ACTIONED`).

## 3. Quyết định xử lý

1. **`DISMISS` (Bỏ qua báo cáo):**
   - Dùng khi nội dung không vi phạm quy tắc cộng đồng.
   - Chỉ chuyển báo cáo được chọn sang `DISMISSED`, giữ nguyên trạng thái `ACTIVE` của bài viết/bình luận.
2. **`HIDE_CONTENT` (Ẩn nội dung vi phạm):**
   - Dùng khi nội dung vi phạm (spam, ngôn từ thù ghét, quấy rối, lừa đảo, nội dung không phù hợp).
   - Hệ thống ẩn bài viết/bình luận (`status = HIDDEN`) và đồng thời chuyển **tất cả** báo cáo `PENDING` cùng `contentType` + `contentId` sang `ACTIONED` trong cùng transaction, ghi nhận `reviewed_by` và `decidedAt`.
3. **Xung đột đồng thời:** Hai `ADMIN` xử lý cùng một báo cáo đồng thời sẽ được tuần tự hóa ở DB; request đến sau nhận `409 Conflict` để tránh ghi đè quyết định.
