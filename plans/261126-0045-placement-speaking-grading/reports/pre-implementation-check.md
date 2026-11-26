# Báo cáo kiểm tra trước triển khai — 2026-11-26

Đã đối chiếu plan với hai worktree `feat/placement-speaking-grading` ở `origin/main`. Worktree backend có sẵn plan chưa commit; frontend sạch.

## Kết quả ba điều kiện

1. **Cấu trúc đề placement và bộ chấm Speaking — đạt, cần adapter placement.** `SpeakingGradingInput.Answer` nhận `partNo`, `questionNo`, `questionText` và phân tích lời nói; `SpeakingPromptAssembler` duyệt danh sách part/câu hỏi động; `SpeakingResultValidator` chỉ chấp nhận ghi chú part thuộc tập part đầu vào. Vì vậy số part và số câu không bị cố định trong grader. Cần adapter ở learning để biến event placement thành input này, ghép kết quả Azure Speech theo từng bản ghi, và truyền câu hỏi/prompt snapshot. Đặt scope placement theo cách không giả định đây là bài luyện đủ ba part: `SpeakingResultAssembler` chỉ tính overall của bài luyện khi scope `FULL` và đúng các part `{1,2,3}`. Không cần sửa rubric/parser để hỗ trợ số part placement.

2. **Bucket — đạt theo cấu hình hiện tại.** Cấu hình assessment dùng `${ASSESSMENT_SPEAKING_BUCKET:speaking-recordings}`, learning dùng `${LEARNING_SPEAKING_BUCKET:speaking-recordings}`; cả hai đi qua `common-storage`/Supabase và cấu hình storage dùng chung. Các tên bucket mặc định trùng nhau. Giới hạn web/assessment là 5 MB; cấu hình bucket shared hiện là 10 MB nên WAV placement trong giới hạn 5 MB có thể lưu và đọc. Quyền truy cập runtime vẫn cần `SUPABASE_URL` và `SUPABASE_SECRET_KEY` hợp lệ như các luồng storage hiện hữu.

3. **Migration tiếp theo — đạt, không trùng version.** Assessment hiện cao nhất V24, kế tiếp **V25**. Learning hiện cao nhất V59, kế tiếp **V60**.

## Kết luận

Không phát hiện điều kiện nào sai với plan; tiếp tục triển khai. Adapter placement phải tránh dùng semantics full-test của bài luyện và giữ rubric/parser ở dạng linh hoạt theo danh sách câu hỏi.
