# Hướng dẫn tạo ảnh biểu đồ PS-W1-WR-3 (dùng cho công cụ tạo ảnh)

Chỉ cần tạo **một** ảnh. Các biểu đồ và ảnh bìa khác đã có sẵn dưới dạng SVG trong cơ sở dữ liệu, không cần tạo lại.

## File đầu ra

- Tên file đúng từng ký tự: `ps-w1-wr-3-marriage-age.png`
- PNG, nền trắng, chữ đen, kích thước khoảng 1200 × 800 px.
- Không logo, không watermark, không chữ ngoài nhãn biểu đồ.

## Nội dung (số liệu đã chốt, không được đổi)

Tiêu đề trên biểu đồ: **Situation of marriage and average age at marriage in Australia, 1960 to 2000**

Trục hoành: năm **1960, 1970, 1980, 1990, 2000**

| Chuỗi dữ liệu | 1960 | 1970 | 1980 | 1990 | 2000 |
|---|---:|---:|---:|---:|---:|
| Religious marriages (số cuộc) | 25,000 | 28,000 | 50,000 | 32,000 | 34,000 |
| Civil marriages (số cuộc) | 8,000 | 12,000 | 16,000 | 23,000 | 40,000 |
| Average age of bride (tuổi) | 21 | 23 | 25 | 27 | 30 |
| Average age of groom (tuổi) | 24 | 25 | 27 | 30 | 32 |

## Cách trình bày

- Hai ô cạnh nhau, dùng chung trục năm ở dưới.
- **Ô trái:** số cuộc hôn nhân, hai đường. Trục đứng là "Number of marriages", từ 0 đến 60,000.
- **Ô phải:** tuổi trung bình khi kết hôn, hai đường. Trục đứng là "Average age (years)", từ 15 đến 35.
- Mỗi điểm có nhãn số đúng như bảng (có dấu phẩy với số hàng nghìn).
- Chú thích đặt dưới mỗi ô, đúng bốn tên trong bảng.
- Hai đường khác màu, đủ tương phản khi in đen trắng; dùng ký hiệu điểm khác nhau cho từng đường.

## Lời nhắc (copy vào công cụ tạo ảnh)

```
Create a clean two-panel line chart on a plain white background, black text, no logo, no watermark, no decorative elements.
Title across the top: "Situation of marriage and average age at marriage in Australia, 1960 to 2000".
Shared x-axis labelled "Year" with ticks 1960, 1970, 1980, 1990, 2000.
Left panel: y-axis "Number of marriages", range 0 to 60,000. Two lines:
  - "Religious marriages": 25,000; 28,000; 50,000; 32,000; 34,000
  - "Civil marriages": 8,000; 12,000; 16,000; 23,000; 40,000
Right panel: y-axis "Average age (years)", range 15 to 35. Two lines:
  - "Average age of bride": 21; 23; 25; 27; 30
  - "Average age of groom": 24; 25; 27; 30; 32
Label every data point with its exact value (use thousands separators on the left panel).
Each line a different colour and marker shape, legend under each panel. Clear, readable, textbook style.
```

## Kiểm tra trước khi nộp

- [ ] Đủ 20 điểm dữ liệu (4 chuỗi × 5 năm), số khớp bảng trên.
- [ ] Tiêu đề, nhãn trục, chú thích đều bằng tiếng Anh, viết đúng chính tả.
- [ ] Không có chữ thừa, không có số phụ.
- [ ] Tên file đúng `ps-w1-wr-3-marriage-age.png`.

Sau khi có file, đặt vào `incoming-media\image\` và báo lại.
