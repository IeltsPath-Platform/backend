---
title: "Học viên chọn examiner và hàng đợi theo examiner"
description: "Mọi việc chấm Writing và Speaking bằng người đều do học viên chọn examiner. Không còn hàng chung. Examiner chỉ thấy bài của mình, học viên thấy tên, ảnh, profile examiner và đổi người khi quá hạn."
status: in-progress
priority: P1
branch: "feat/ielts-manual-grading"
tags: [assessment, user, learning, examiner, queue]
blockedBy: []
blocks: []
created: "2026-10-22T04:50:00.000Z"
createdBy: "ck:plan"
source: conversation
---

# Học viên chọn examiner và hàng đợi theo examiner

Thay quy tắc cũ "một hàng chung, ai nhận trước thì chấm" (FT-32, `V11__examiner_queue.sql`) bằng quy tắc của chủ dự án.
Đây là **thay đổi hành vi có chủ ý**: các test đang kiểm tra hàng chung (nhận đồng thời, nhận lại sau 24 giờ bởi người khác) phải viết lại.

## Quyết định đã chốt (hội thoại 2026-10-22)

| Chủ đề | Quyết định |
|---|---|
| Hàng chung | **Không có.** Writing và Speaking đều do học viên chọn examiner |
| Examiner thấy gì | Chỉ bài chỉ định cho mình. Không nhận được bài của người khác |
| Bài do hệ thống tạo (AI chấm không được, bài thi thử) | Học viên chưa chọn nên bài ở trạng thái **chờ chọn giáo viên**, chưa examiner nào thấy, học viên được yêu cầu chọn |
| Bài thi thử có cả Writing và Speaking | **Một examiner chấm cả bài** |
| Examiner chậm | Không tự chuyển. Quá hạn thì **báo học viên và học viên chọn examiner khác**; yêu cầu và lượt chấm giữ nguyên |
| Hạn | 48 giờ kể từ lúc chỉ định (mốc "quá hạn" hiện có) |
| Số lần đổi | Tối đa 2 lần mỗi bài (giả định, chỉnh được) |
| Học viên thấy gì về examiner | Tên, ảnh, giới thiệu (có sẵn trong hồ sơ công khai) cộng **band và chứng chỉ** (mới) |
| Giới hạn nhận bài | Tối đa **10 bài mới nhận trong một ngày** (0h–24h giờ Việt Nam, đã chấm hay chưa đều tính) mỗi examiner, chỉnh được theo từng người (1 đến 50), và có nút "đang nhận bài" bật/tắt. Học viên không chọn được examiner đã đủ hoặc đang tắt nhận bài |
| Chấm mù | Examiner **vẫn không biết học viên là ai** |
| Đường QA (`QA_SAMPLE`) | Không đổi: do CONTENT_AUTHOR hoặc ADMIN duyệt, không qua lựa chọn của học viên |

Lý do chọn 10: một bài Writing mất khoảng 15 đến 20 phút, Speaking khoảng 10 đến 15 phút; 10 bài là 3 đến 4 giờ việc, kịp hạn 48 giờ cả khi examiner làm bán thời gian.

## Phases

| # | Phase | Service | Trạng thái |
|---|---|---|---|
| 1 | [Chỉ định examiner, hàng đợi riêng, đổi examiner](phase-01-assessment-assignment.md) | assessment | completed |
| 2 | [Hồ sơ examiner và danh sách cho học viên](phase-02-examiner-profile.md) | user | completed |
| 3 | [Học viên gửi bài kèm examiner, thấy examiner](phase-03-learning-flow.md) | learning | completed |
| 4 | [Tài liệu hợp đồng và wireframe](phase-04-docs-and-wireframes.md) | docs | in-progress |

Phase 1 cần một cổng `ExaminerDirectory` do phase 2 cung cấp (endpoint nội bộ của user-service); trong test phase 1 dùng bản giả.

## Rủi ro chính

| Rủi ro | Cách xử lý |
|---|---|
| Việc chấm đang chờ trong DB (`QUEUED`, chưa ai nhận) sẽ **không examiner nào thấy** sau thay đổi | Migration giữ việc đã có examiner nhận (đặt người đó làm examiner được chọn). Việc chưa ai nhận thành "chờ chọn giáo viên", học viên chọn qua API mới. Cần thông báo vận hành trước khi triển khai |
| Hai học viên chọn cùng examiner ở chỗ cuối cùng | Khóa theo examiner trong transaction rồi mới đếm số bài nhận trong ngày |
| Thay đổi đồng thời học viên đổi examiner và examiner chốt bài | Chốt bài thắng: việc đổi bị từ chối khi việc đã `COMPLETED` |
| Nháp của examiner cũ lộ cho examiner mới | Đổi examiner xóa nháp và đặt lại việc chấm (đã có cơ chế xóa nháp khi nhận lại) |
| Nối Learning và user-service | Learning không lưu examiner; assessment là nguồn duy nhất của "bài này gửi cho ai" |

## Ngoài phạm vi

- Email và thông báo đẩy qua điện thoại khi quá hạn (chưa có kênh này). Đã làm: thông báo trong app gửi một lần qua notification-service, xem `docs/contracts/grading-overdue-v1.md`.
- Quản trị viên chuyển bài bằng tay, bảng điều khiển quá hạn cho quản trị viên.
- Examiner khác nhau cho Writing và Speaking trong cùng một bài thi thử (cần thiết kế lại cách chốt điểm tổng).
- Mở Speaking (xem plan `261022-0835-examiner-grading-db`).

## Câu hỏi chưa giải quyết

1. Số lần đổi tối đa (2) và hạn 48 giờ có đúng không?
2. Examiner mới vào có mặc định "đang nhận bài" không, hay phải tự bật?
3. Học viên cũ đang có việc chờ chọn giáo viên: có cần nhắc qua email khi triển khai không?
