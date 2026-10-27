---
phase: 4
title: "Kiểm chứng và triển khai"
status: pending
priority: P1
effort: "1h"
dependencies: [1, 2, 3]
---

# Phase 4: Kiểm chứng và triển khai

## Overview

Xác nhận các migration chạy sạch từ DB trống, không làm hỏng luồng chấm hiện có, rồi đưa lên môi trường theo thứ tự.

## Steps

1. **DB tạm.** PostgreSQL 15 trong Docker: áp toàn bộ migration assessment-service (V1 đến V16) theo thứ tự với `ON_ERROR_STOP=1`;
   chạy lại các câu thử của phase 1 và 2. Đã làm: assessment V1 đến V16 và chuỗi Speaking; content V1 đến V58 rồi hai bản nháp (V59, V60). Chạy mỗi file với `psql -1` để giống Flyway (V42 của content dùng bảng tạm `ON COMMIT DROP`).
2. **Content-service.** Áp V1 đến V59 trên DB tạm; kiểm tra tập mã `ACTIVE` + `APPROVED` (phase 3).
3. **Test Java.** Chạy test của hai service, vì Flyway chạy cùng Testcontainers PostgreSQL:
   `mvn -q -pl services/assessment-service -am test` và `mvn -q -pl services/content-service -am test`.
   Trọng tâm: `HumanReviewFlowIntegrationTest`, các test của `SaveAssessmentResultDetailsUseCase`, test chấm tự động.
   Máy hiện tại **chưa có Maven** nên bước này phải chạy trên máy dev hoặc CI; thiếu Docker thì test bị skip, phải báo rõ.
4. **Thứ tự triển khai:** content-service V59 (sau khi có người duyệt) trước, assessment-service V15 và V16 sau. Hai cột mới
   cho phép `NULL`, nên bản backend cũ vẫn chạy bình thường trên schema mới.
5. **Sau triển khai:** chạy `SELECT count(*) FROM human_review_submissions WHERE annotations IS NOT NULL` và kiểm tra
   `draft_payload` rỗng ở mọi dòng (chưa có code ghi).

## Tiêu chí hoàn thành

- V15 và V16 áp sạch từ DB trống; mọi câu thử của phase 1 và 2 cho kết quả như bảng.
- Test hai service xanh, hoặc nêu rõ test nào bị skip và vì sao.
- V59 chỉ merge khi PR có tên người duyệt.
- Không migration nào đã áp dụng bị sửa.

## Rollback

Mọi thay đổi đều là thêm cột hoặc đổi dữ liệu nên đảo ngược bằng migration mới (xem phase 1 đến 3). Backend cũ vẫn chạy với schema mới.

## Phase 3b: Speaking (bản nháp V60)

`drafts/V60__speaking_rubric_and_error_groups.sql` đã thử trên PostgreSQL 15 tạm sau V1 đến V58 và V59:

| Kiểm tra | Kết quả |
|---|---|
| Rubric Speaking | 1 rubric `DRAFT`, 4 tiêu chí (FC, LR, GRA, PR), 44 mô tả band |
| Rubric đang dùng | `TASK_1` và `TASK_2` vẫn `ACTIVE`, không đổi |
| Mã lỗi mới | 6 mã (3 PRONUNCIATION, 3 FLUENCY), đều `DRAFT`, không vào danh mục đã duyệt |
| Giá trị ngoài danh sách (`WRITING`) | vẫn bị `CHECK` từ chối |

Bên assessment-service không cần migration cho Speaking: đã thử lưu một bài Speaking của bài thi thử (nộp với `audio_reference`,
việc chấm HUMAN, nhận bài, kết quả `DRAFT` rồi `COMPLETED`, điểm Speaking, ghi chú theo mốc thời gian trong `feedback_snapshot`).
