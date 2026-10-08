# Phase 5 — Part 5 Business Rules và Part 6 NFR

## Context

- Bảng BR-01..BR-34 ở Part 5 có cột "features that apply each rule".
- Part 6: các dòng có LLM / daily limit (text ~4105), Notes "current release".

## Sửa BR

| BR | Sửa |
| --- | --- |
| BR-12 | Bỏ hoặc viết lại "A topic belongs to exactly one skill…". Chỉ bỏ nếu code không còn khóa skill của topic (kiểm `TOPIC_SKILL_LOCKED`). |
| BR-13 | Danh sách owner của câu hỏi thêm course test. |
| BR-14 | Purpose LEARNING áp dụng cả cho course test. |
| BR-15 | Pass mark 70% áp dụng thêm cho course final test. Practice tính 70% **theo từng skill khách quan**. |
| BR-16 | "In each course the first unpassed topic is IN_PROGRESS…; order bandLevel → FREE/PREMIUM → sortOrder; every course is open". |
| BR-17 | "A pending review blocks every lesson containing its skill, their practice and topic test". |
| BR-22 | Thêm: course test cần mọi topic của course PASSED, không chặn tiến độ. |
| Mới BR-35 | Placement chỉ gợi ý course (luật chọn course), không đổi trạng thái. |
| Mới BR-36 | Lesson hoàn thành khi mọi block khách quan đạt; essay không bao giờ chặn lesson hay Practice. |
| Mới BR-37 | Review chỉ sinh cho KP Reading/Listening; skill của review = skill của KP. |
| Mới BR-38 | Essay trong TOPIC_GATE/COURSE_GATE: LLM chấm miễn phí, quota theo ngày; lỗi, chưa cấu hình hoặc hết quota thì chuyển EXAMINER. Essay chưa nộp được 0 điểm. Essay đạt khi band ≥ `passBand`. |

Cập nhật cột feature của từng BR theo phase 3: FT-55, đổi tên FT-20.

## Sửa NFR

- Dòng giới hạn chi phí LLM: thêm quota chấm essay trong gate (`ASSESSMENT_LLM_DAILY_LIMIT`, mặc định 20) bên cạnh
  quota 10 lượt/ngày của learner. Kiểm trong code xem quota là toàn hệ thống hay theo learner rồi mới viết.
- Availability: thêm dòng hoặc note "LLM không khả dụng không chặn bài thi, essay chuyển EXAMINER". Chỉ thêm nếu bảng
  6.x có dòng degrade tương tự để theo; không thì để trong Notes.

## Steps

1. Sửa các dòng BR tại chỗ, thêm BR-35..38 cuối bảng (clone row).
2. Đảm bảo mọi FT ở phase 3 trỏ đúng số BR mới.
3. Revision history v0.9.17.

## Validation

- Mọi BR-xx được trích trong Part 3 đều có trong bảng Part 5, và ngược lại.
