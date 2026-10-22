# Phase 06: Màn Dịch vụ, Hoạt động và Tổng quan hệ thống (frontend)

Phụ thuộc phase 03 (`system-monitoring-v1.md`). Thư mục `src/features/admin/system/`.

## 1. Dịch vụ (`/admin/services`)

- Gọi `GET /api/admin/system/services`. Bảng: tên dịch vụ (đổi `CONTENT-SERVICE` thành "Content"), trạng thái (badge
  `adm-badge--ok` cho UP, `adm-badge--danger` cho DOWN), số instance, thời gian phản hồi nhanh nhất, URI instance (chữ
  nhỏ, màu muted).
- Nút "Kiểm tra lại" ở thanh tiêu đề và tự làm mới mỗi 30 giây khi tab đang mở (`document.visibilityState === 'visible'`),
  dừng khi rời trang. Hiển thị "Kiểm tra lúc HH:mm:ss" theo `checkedAt`.
- Lỗi gọi API (ví dụ gateway lỗi) hiện `AdminError`, không xóa bảng cũ đang có.

## 2. Hoạt động (`/admin/activity`)

- Gọi `GET /api/learning/admin/activity-stats?from=&to=` (contract `learning-staff-area-v1.md`: ngày ISO, tối đa 180 ngày,
  ngày theo UTC). Mặc định 30 ngày gần nhất; chọn nhanh 7 / 30 / 90 ngày và chọn khoảng tùy ý.
- Ba thẻ số của ngày cuối: DAU, WAU, MAU.
- Biểu đồ DAU theo ngày bằng SVG tự vẽ (cột, không thư viện), có trục và nhãn ngày thưa; có bảng dữ liệu ẩn cho trình đọc
  màn hình.
- Bảng retention theo cohort tuần: tuần bắt đầu, cỡ cohort, tuần 1/2/4/8 dạng phần trăm; `null` hiện "chưa đủ dữ liệu",
  không hiện 0.
- Ghi chú dưới tiêu đề: "Ngày tính theo UTC; hoạt động là có trả lời câu hỏi, không phải đăng nhập."
- Lỗi `INVALID_RANGE`, `RANGE_TOO_LONG` hiện thành câu tiếng Việt.

## 3. Tổng quan hệ thống (`/admin/system`)

Ba khối độc lập (một khối lỗi không làm trắng trang, theo mẫu `overview/pages/OverviewPage.tsx`):
- **Dịch vụ**: số UP / tổng, liệt kê dịch vụ DOWN; liên kết tới `/admin/services`.
- **Người dùng**: tổng tài khoản, số theo vai trò, số bị khóa (đếm phía client từ `GET /api/users`); liên kết tới
  `/admin/users`.
- **Hoạt động hôm qua**: DAU, WAU, MAU của ngày gần nhất (gọi activity-stats 7 ngày); liên kết tới `/admin/activity`.

## Test

- Dịch vụ: hiển thị UP/DOWN, tự làm mới gọi lại API (dùng fake timers), dừng khi unmount, giữ bảng cũ khi lần gọi sau lỗi.
- Hoạt động: đổi khoảng thời gian gọi đúng tham số, retention `null` hiện "chưa đủ dữ liệu", lỗi khoảng quá dài.
- Tổng quan hệ thống: mỗi khối độc lập khi nguồn kia lỗi.

## Tiêu chí hoàn thành

typecheck, lint, test xanh. Thử tay: tắt một service trong IntelliJ, màn Dịch vụ báo DOWN sau lần làm mới kế tiếp.
