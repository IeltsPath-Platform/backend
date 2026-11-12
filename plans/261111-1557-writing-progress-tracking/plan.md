---
title: "Writing progress across essays"
description: "Theo dõi tiến bộ Writing qua nhiều bài: lỗi hay gặp, xu hướng band từng tiêu chí, lỗi đã khắc phục"
status: done
priority: P2
branch: "fix/grade-writting-llm"
tags: [learning-service, frontend, writing, report]
blockedBy: [261111-1102-writing-grading-upgrade]
blocks: []
created: "2026-11-11T08:57:00.000Z"
---

# Writing progress across essays

Tách từ plan `writing-grading-upgrade-plan/261111-1102-writing-grading-upgrade` (quyết định 19 của plan đó). Chưa có code
nào được viết. Các quyết định ở mục "Quyết định đã chốt" do người dùng duyệt ngày 2026-11-11.

## Vì sao làm
Kết quả chấm hiện chỉ nói về một bài. Học viên không thấy mình hay sai dạng gì qua nhiều bài, band từng tiêu chí đang lên
hay xuống, và lỗi nào đã hết mắc. Dữ liệu đã có trong JSON bài chấm; chỉ thiếu phần tổng hợp và hiển thị.

## Hiện trạng (đã đọc, chưa chạy)

| Dữ kiện | Nơi | Hệ quả |
| --- | --- | --- |
| Bài Writing lesson, Practice và tự do cùng nằm trong `lesson_writing_submissions` (`source`, `status`, `overall_band`, `result` JSONB, `graded_at`, `revision_no`, `previous_submission_id`); `task` nằm trong `prompt_snapshot` | Learning V2, V10, V12, V20 | Đọc được từ một bảng, không gọi service khác |
| Chưa có endpoint liệt kê bài Writing của học viên; chỉ có `GET /writing-submissions/{id}` và `/{id}/history` (chuỗi nộp lại) | `WritingController` | Cần endpoint mới |
| Index hiện có theo `(user_id, lesson_id, block_id, submitted_at)`; không có index theo user và thời gian chấm | V2 | Cần index mới cho truy vấn theo user |
| Lịch sử band (`261109-band-history` ở frontend) lấy bài **thi** từ Assessment | `/api/assessments/me/statistics/bands` | Không trùng: plan này chỉ lấy bài Writing của Learning |
| Báo cáo `GET /api/learning/report` có mastery theo KP tiêu chí Writing, không có lỗi theo mã hay band từng bài | `ReportController` | Không sửa báo cáo; endpoint riêng, frontend có thể đặt cạnh |
| Prompt v1 trả tối đa 10 correction, v2 liệt kê mọi lỗi (plan nâng cấp) | `EssayGrader` | Đếm **số lỗi** qua hai phiên bản sẽ thấy tăng giả; phải đếm theo **có/không** mắc mã lỗi trong bài |
| Tên và `fixHint` của mã lỗi: bài chấm sau plan nâng cấp có sẵn trong JSON; bài cũ thì không | phase 2 plan nâng cấp, `ErrorTypeTexts` | Bài cũ tra tên một lần theo lô, best-effort (`AGENTS.md` §3.6) |

## Phạm vi

1. **Xu hướng band:** mỗi bài đã chấm một điểm: thời điểm, task, nguồn, overall, band bốn tiêu chí.
2. **Lỗi hay gặp:** theo mã lỗi, số bài có mắc trên số bài trong cửa sổ, lần gần nhất, tên, `fixHint`, KP luyện.
3. **Lỗi đã khắc phục:** mã lỗi từng mắc mà không lặp lại ở các bài sau cùng task (định nghĩa ở mục "Cần chốt").

Không thuộc phạm vi: bài thi gate và mock (Assessment, đã có lịch sử band riêng); gợi ý lộ trình; thông báo; đổi mastery
hay evidence. Endpoint chỉ đọc.

## Quyết định đã chốt (không đảo; muốn đổi phải hỏi người dùng)

| Câu hỏi | Quyết định | Lý do |
| --- | --- | --- |
| Bài nộp lại tính thế nào | Chỉ lấy **lần nộp đầu** (`revision_no = 1`) cho xu hướng và lỗi | Lần sửa có phản hồi AI dẫn đường, không phản ánh trình độ tự viết; tiến bộ trong chuỗi sửa đã có ở màn kết quả |
| Cửa sổ dữ liệu | 10 bài đã chấm gần nhất mỗi task (`limit` tối đa 20) | Đủ thấy xu hướng, chặn trên cho truy vấn và tính toán |
| "Đã khắc phục" | Mã lỗi `ERROR` có ở một bài, không có ở **2 bài liền sau cùng task** (cấu hình được) | Một bài không mắc có thể do đề không đụng tới dạng lỗi đó |
| Hiển thị ở đâu | Mục "Tiến bộ Writing" trong trang báo cáo học tập | Học viên đã quen tìm tiến độ ở đó; không thêm điểm vào mới |

## Phases

| Phase | Việc | Service | Phụ thuộc | Đổi contract | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | [Endpoint tổng hợp](./phase-01-progress-endpoint.md) | learning | plan nâng cấp phase 2, phase 4 (`certainty`, chưa có: coi là ERROR) | Có (endpoint mới) | Xong |
| 2 | [Màn tiến bộ Writing](./phase-02-progress-view.md) | frontend | 1 | Không | Xong ở frontend PR #81 (nhánh của Dev A không có trên remote nên viết lại theo contract; test chạy được, chưa xem trên trình duyệt) |

Phase 1 làm được trước khi plan nâng cấp xong: thiếu `certainty` thì coi là `ERROR`, thiếu tên thì tra theo lô.

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Đổi prompt v1 → v2 làm số lỗi tăng giả | Đếm theo có/không mắc mỗi bài; điểm dữ liệu mang `promptVersion` để giao diện đánh dấu mốc đổi |
| Ít bài (1 đến 2) khiến "xu hướng" và "đã khắc phục" vô nghĩa | Dưới 3 bài cùng task: trả dữ liệu nhưng `insufficient = true`; giao diện hiện trạng thái "cần thêm bài" |
| Mã lỗi dự phòng (`*_OTHER`) chiếm đầu danh sách | Xếp mã dự phòng cuối, không tính "đã khắc phục" cho chúng |
| Đọc JSON `result` nhiều bài tốn tài nguyên | Một truy vấn, tối đa 20 dòng, chỉ lấy các trường cần bằng toán tử JSONB |

## Pattern áp dụng (`AGENTS.md` §3.7, §3.9)
- Một truy vấn có giới hạn, sắp ở DB; không N+1; tra tên lỗi tối đa một lần gọi Content qua `ResilientCall`, lỗi thì
  trả mã không tên.
- Endpoint mới dưới `/api/learning/**` đã có route Gateway; kiểm tier rate limit `default`.
- Contract mới `docs/contracts/writing-progress-v1.md` trong cùng PR. Migration mới chỉ thêm index.
