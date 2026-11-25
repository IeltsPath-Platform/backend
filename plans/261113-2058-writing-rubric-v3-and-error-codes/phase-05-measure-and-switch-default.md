# Phase 5: Đo trên bộ bài test và đổi template mặc định

## Context

- Quy trình và mục tiêu: sheet `Huong_dan` và `Tong_hop` của file log. Mỗi lần chỉ đổi một thứ, mỗi cấu hình chấm
  ít nhất 2 lần.
- `docs/llm-grading-log/scripts/replay_writing_grade.py` tự dựng prompt bằng Python. Hiện script cố định template v2,
  `kpj2`, rubric ACTIVE, dòng mã `code — name: fix_hint`, không lọc mã theo task và không có `system_prompt`. Nếu
  không sửa, số đo không khớp với prompt mà service gửi thật.
- Phụ thuộc phase 1–4.

## File

- Sửa `docs/llm-grading-log/scripts/replay_writing_grade.py`:
  - tham số `--template v2|v3` và `--rubric-version N` (mặc định là bản ACTIVE);
  - dòng mã dùng `model_definition` khi có; lọc theo `writing_tasks`;
  - in `system_prompt` của rubric trước phần criteria, giống code phase 3;
  - v3 thì nối `essay-kp-judgment-v3.txt`.
- Sửa `docs/llm-grading-log/llm-writing-grading-prompt-iteration-log-261112.xlsx`:
  - `Bai_test`: thêm ít nhất một bài Task 2 và một bài band 6–7. Bài do dev hoặc examiner cung cấp; không tự sinh
    bài giả làm đáp án;
  - `Doi_chieu_loi`: thêm đáp án lỗi cho bài mới;
  - `Lan_cham`, `Phien_ban_prompt`, `Van_de`: ghi kết quả và trạng thái.
- `infra/config-server/config-repo/learning-service.yaml`: chỉ đổi `default-template` sang `v3` khi đạt mục tiêu.

## Thứ tự đo (deepseek-flash, max_tokens 8192)

| Bước | Rubric | Mã lỗi | Template | Mục đích |
|---|---|---|---|---|
| 0 | @2 | cũ (`fix_hint`, không lọc) | v2 | Mốc so sánh, tức R04b–R07 có sẵn |
| 1 | @2 | mới (phase 1, 3) | v2 | Ảnh hưởng của định nghĩa mã và lọc theo task |
| 2 | @3 | mới | v2 | Ảnh hưởng của rubric v3 lên band |
| 3 | @3 | mới | v3 | Ảnh hưởng của prompt v3 |

Bước 0–1 cần DB chưa chạy V133 hoặc tham số `--rubric-version 2`; bước 1 cần V132. Mỗi bước chấm E01, E01b và các bài
mới, mỗi bài 2 lần.

## Tiêu chí đổi mặc định sang v3 (bước 3, theo `Tong_hop`)

- Recall tất cả lỗi ≥ 85%, recall lỗi số liệu 100% trên E01.
- Câu sửa câu tổng quan vẫn sai số liệu = 0; câu sửa hỏng = 0.
- Không có `INVALID_GRADE` hay quá giờ; thời gian ≤ 20 giây, kể cả bài Task 2.
- Chi phí ≤ $0.01 một bài.
- Band lệch không quá 0.5 so với bước 2 (C11).

Không đạt thì ghi vấn đề mới vào `Van_de`, giữ `default-template: v2` và đưa đề xuất sửa vào v4. Không sửa file v3.

## Rủi ro

- Rubric @3 đã ACTIVE từ phase 2, nên môi trường không ghim prompt chạy @3 + v2 trước khi đo xong bước 2. Nếu muốn
  tránh, deploy phase 2 sau khi đo bước 2 bằng script.
- Hai lần chấm mỗi cấu hình chỉ cho thấy dao động thô (I16).
