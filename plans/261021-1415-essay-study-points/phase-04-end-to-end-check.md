---
phase: 4
title: "Kiểm chứng đầu-cuối"
status: in-progress
priority: P1
effort: "0.25d"
dependencies: [2, 3]
repo: both
---

# Phase 4: Kiểm chứng đầu-cuối

Chạy sau khi phase 2 và 3 xong. Mục tiêu: chứng minh backend và frontend khớp hợp đồng JSON thật, không chỉ khớp dữ liệu giả của từng bên.

## Bước 1: Khớp hợp đồng bằng dữ liệu thật (không cần chạy cả hệ thống)

1. Backend: trong `WritingWebMvcTest` (phase 1) có sẵn JSON mẫu của `studyPoints`. Lưu đúng chuỗi JSON mà test này sinh ra cho một mục `CRITERION` và một mục `AI`
   (in ra bằng log tạm hoặc đọc từ assert), rồi dán thành hằng trong `learningAdapters.studyPoints.test.ts` (frontend) làm fixture "JSON do server thật sinh ra".
   Test adapter phải map đúng fixture này (khóa đúng tên: `kind`, `criterionCode`, `band`, `knowledgePointId`, `code`, `name`, `title`, `reason`).
   Nếu hai bên lệch tên khóa, sửa phía ít sai hơn và ghi vào báo cáo.
2. Không để lại log tạm trong code.

## Bước 2: Kiểm tra thủ công (nếu môi trường chạy được cả hai)

Điều kiện: Docker, Postgres, RabbitMQ, config-server, gateway và hai service Content và Learning chạy bản mới của nhánh này (đã áp V69, V70). Nếu không dựng được, ghi rõ
"chưa kiểm tra thủ công" trong báo cáo, không bịa kết quả.

1. Đăng nhập một học viên có điểm và có khóa học Writing, mở bộ luyện tập có essay (ví dụ `PS-W2-WR-1`).
2. Viết một bài Task 2 đủ số từ nhưng cố ý yếu về liên kết, bấm "Nộp để chấm".
3. Kỳ vọng ở khung kết quả: có mục "Kiến thức cần học". Mỗi tiêu chí dưới band đạt có mục `CRITERION` kèm nút "Luyện tập"; có thể có thêm mục `AI`.
4. Bấm "Luyện tập" ở một mục: mở trang luyện tự do của KP (ví dụ "Mạch lạc và liên kết (CC)") với 3 câu. Làm xong và quay lại bằng liên kết quay về.
5. Mở lại bộ luyện tập, bài đã chấm vẫn còn, bấm "Hoàn thành bộ luyện tập" vẫn hoạt động.
6. Quay lại khung kết quả của chính bài đó rồi bấm "Luyện tập" lần thứ hai cho cùng KP: kỳ vọng thông báo "Đã hết bộ luyện tự do cho điểm kiến thức này...".
   (Đây là hành vi đã biết vì `reserve = 2`; không phải lỗi.)
7. Mở lại bài đã chấm từ trước khi có tính năng (nếu có): không hiện mục "Kiến thức cần học", không lỗi.

Ghi lại ảnh chụp hoặc mô tả ngắn từng bước vào báo cáo.

## Bước 2b: Kiểm tra DB (nếu có quyền)

```sql
-- bài chấm mới có studyPoints
SELECT result::jsonb -> 'studyPoints' FROM lesson_writing_submissions ORDER BY submitted_at DESC LIMIT 1;
-- bằng chứng mastery vẫn ghi như cũ, không bị ảnh hưởng
SELECT kp_id, correct, source FROM kp_evidence WHERE user_id = '<user-id>' ORDER BY ordinal DESC LIMIT 6;
```

## Báo cáo cuối (bắt buộc, theo mẫu)

```text
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
Backend: files changed (git diff --stat), lệnh mvn đã chạy, số test chạy/lỗi/bỏ qua, test Testcontainers nào bị bỏ qua
Frontend: files changed (git diff --stat), kết quả typecheck/eslint/vitest/npm test, số test chạy/lỗi
Hợp đồng: có lệch tên khóa giữa hai bên không, đã sửa thế nào
Thủ công: đã làm bước nào, bước nào chưa làm và vì sao
Deviations: chỗ nào làm khác plan, lý do
Concerns: nếu có
```

## Tiêu chí hoàn thành

- [x] Test adapter ở frontend map đúng JSON do server thật sinh ra.
- [x] Kiểm tra thủ công đã làm, hoặc ghi rõ là chưa làm.
- [x] Báo cáo cuối trung thực về mọi test bị bỏ qua hoặc lỗi.
- [ ] Không file ngoài danh sách của phase 1-3 bị sửa; không commit.

## Đồng bộ trạng thái — 2026-10-21

Artifact `services/learning-service/target/writing-study-points-contract.json` do MockMvc sinh đã đối chiếu với fixture frontend; kiểm thử adapter nằm trong 41 test vừa qua. Chưa kiểm tra thủ công luồng chấm → mở luyện → quay lại; không có kiểm tra DB thực trong lần này. Báo cáo đã ghi các lỗi/giới hạn. Workspace còn diff của các công việc khác, chưa đối chiếu tách toàn bộ phạm vi file; không commit. Chi tiết: [báo cáo đồng bộ](./reports/pm-2026-10-21-writing-plan-status.md).
