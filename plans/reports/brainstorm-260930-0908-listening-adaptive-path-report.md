---
type: brainstorm
date: 2026-09-30
topic: ADP (adaptive learning path) cho Listening
status: agreed
modes: []
related_plans:
  - plans/260930-0851-listening-topic-audio-lessons
  - plans/260929-1640-lesson-learning-pipeline-mvp
---

# Brainstorm: ADP cho Listening

## 1. Vấn đề

Người dùng hỏi "Listening làm ADP như nào". ADP hiện tại (sau plan 1640) không phân biệt kỹ năng, nên Listening đã có ADP ở
mức KP ngay khi đi qua pipeline. Vấn đề thật: engine chỉ biết "KP sai", còn Listening sai vì nhiều nguyên nhân khác nhau:
- không nghe ra (tốc độ, nối âm, giọng);
- nghe ra nhưng viết sai chính tả;
- bị bẫy đổi ý hoặc paraphrase;
- chưa quen dạng bài.

Giao lại một gói cùng loại, cùng độ khó không sửa được nguyên nhân.

## 2. Hiện trạng (scout)

- Path theo `sort_order`. Mastery theo KP: `compute_mastery` trên 5 lần làm gần nhất, chỉ nhận đúng/sai.
- Luyện thêm và ôn bắt buộc khi mastery < 0.6, có câu sai, và bài dạy KP đã xong. Đề cuối ≥ 70% mở topic kế.
- Spaced repetition (`app/mastery/scheduler.py`) chỉ là gợi ý cho tutor.
- `app/mastery/policy.py:37-47`:
  - `MEMORY`/`PROCEDURE` được coi là đã nắm theo độ chính xác (ngưỡng 0.9);
  - `CONCEPT`/`DESIGN` chỉ đã nắm khi tutor chấm giải thích (`mastery_assess`).
- **Lỗi trong plan Listening:** `LS_KP2` đặt là `CONCEPT`, nên sẽ không bao giờ hiện "đã nắm" nhờ làm bài.
- `question_versions.difficulty` (EASY/MEDIUM/HARD) có cột nhưng chưa dùng; chọn gói ôn (1640) chỉ là "gói chưa giao có câu đo KP".

## 3. Các hướng đã so

| | A. Chia KP kỹ năng con | **B. A + chọn gói theo độ khó (chọn)** | C. B + chẩn đoán lỗi điền từ |
| --- | --- | --- | --- |
| Làm gì | KP theo kỹ năng con, mỗi câu 1 KP chính | Gói dễ/vừa/khó; mức theo mastery và kết quả gói trước | Sai gần đúng (≤ 2 ký tự) = nghe đúng, sai chính tả; nghe lại đúng đoạn |
| Code | Không | Hàm chọn gói; không migration; Reading cũng dùng | Đổi contract `answer-spec-v1` (Java + Python), câu ghi 2 KP |
| Hạn chế | Không phân biệt nghe không ra với sai chính tả | Nhiều nội dung hơn | Tốn nhất; heuristic dễ nhầm |

Loại bỏ: dùng số lần nghe lại hay tốc độ phát làm tín hiệu cho mastery. Server không thấy, và client sửa được.

## 4. Thiết kế đã duyệt

### 4.1 KP Listening (đều `PROCEDURE`)

| KP | Đo gì | Bài dạy |
| --- | --- | --- |
| `LS_NUM` | Số, ngày, giờ, giá | LS1 |
| `LS_SPELL` | Đánh vần tên, địa chỉ, mã | LS1 |
| `LS_PARA` | Bắt ý khi đáp án paraphrase | LS2 |
| `LS_TRAP` | Tránh bẫy đổi ý (distractor) | LS2 |

Mỗi câu gắn 1 KP chính. Luật luyện thêm hiện có tự nhắm đúng kỹ năng yếu.

### 4.2 Chọn gói theo độ khó (mọi kỹ năng)

- Độ khó của gói = mức của các câu trong gói (seed để cùng mức; lệch thì lấy mức cao nhất). Endpoint nội bộ tìm gói của
  content trả thêm `difficulty`.
- Mức mục tiêu:
  - mastery < 0.4 → EASY;
  - 0.4 ≤ mastery < ngưỡng → MEDIUM;
  - ≥ ngưỡng → HARD;
  - gói trước trong cùng review item trượt → hạ một mức so với gói đó, thấp nhất là EASY.
- Chọn gói chưa giao đúng mức. Không có thì lấy mức gần nhất (bằng khoảng cách thì lấy mức dễ hơn). Hết gói chưa giao thì
  lấy gói giao lâu nhất (luật cũ).
- Hàm thuần `choose_pack(candidates, mastery, last_set)` trong ai-learning; không migration; không đổi luật chèn, ngưỡng, evidence.
- Nhật ký hoặc response ghi lý do chọn mức (ví dụ `difficultyReason: "LOW_MASTERY" | "PREVIOUS_SET_FAILED" | "NEAREST_AVAILABLE"`).

### 4.3 Hướng dẫn soạn audio theo mức

- EASY: 1 người nói, chậm và rõ, không bẫy.
- MEDIUM: 2 người nói, tốc độ thường, 1 bẫy.
- HARD: nhanh hơn, giọng khác, từ 2 bẫy trở lên, thông tin rải xa nhau.

### 4.4 Seed MVP

- `LS_NUM`, `LS_SPELL`: đủ 3 mức (6 gói).
- `LS_PARA`, `LS_TRAP`: mỗi KP 1 gói MEDIUM (2 gói).
- Tổng 8 gói + 2 bài + 2 mã đề = 12 file audio.

### 4.5 Nơi làm

- Plan mới, nhỏ: "chọn gói theo độ khó", `blockedBy` 1640. Không mở lại plan 1640 đã red-team.
- Sửa plan Listening `260930-0851`: 4 KP `PROCEDURE`, gắn lại KP từng câu, danh sách gói theo mức; `blockedBy` thêm plan chọn gói.

### 4.6 Bổ sung sau brainstorm (2026-09-30): ưu tiên cùng dạng câu

Người dùng muốn "sai dạng nào luyện dạng đó". Giữ KP theo kỹ năng; lúc chèn luyện thêm sau khi bài xong, ghi dạng câu sai vào
cột mới `path_review_items.wrong_question_types`; `choose_pack` lọc gói cùng dạng trước (ưu tiên mềm), rồi mới xét mức. Bài
ôn sinh từ đề cuối chưa có dạng câu (event không mang). Chi tiết: plan `260930-0908-practice-set-difficulty-selection`.

## 5. Rủi ro

| Rủi ro | Xử lý |
| --- | --- |
| Thiếu gói ở một mức nên thích ứng yếu | Luật "mức gần nhất"; seed đủ 3 mức cho 2 KP hay sai nhất |
| Gắn `difficulty` sai khi soạn | Hướng dẫn mức trong contract; test seed kiểm mọi câu trong gói cùng mức |
| Gói HARD làm học viên yếu nản | Chỉ giao HARD khi mastery ≥ ngưỡng; trượt thì hạ mức ngay |
| Hàm chọn gói sửa hai lần (1640 rồi plan mới) | Plan mới chỉ thay một hàm thuần có test bảng ca |

## 6. Tiêu chí nghiệm thu

- Mastery `LS_NUM` 0.3 → gói EASY; 0.5 → MEDIUM.
- Trượt gói MEDIUM → lần mở sau nhận gói EASY chưa làm.
- Không có gói EASY chưa giao → gói MEDIUM chưa giao (gần nhất), log `NEAREST_AVAILABLE`.
- Reading: KP chỉ có gói không gắn `difficulty` → hành vi y như luật cũ.
- `LS_PARA` làm đúng liên tục → mastery ≥ 0.9 hiện "đã nắm" trên `/progress` (không cần tutor).

## 7. Bước tiếp

1. `/ck:plan` cho "chọn gói theo độ khó".
2. Sửa plan Listening theo 4.1, 4.4.
3. Cập nhật demo: 4 KP Listening, gói có nhãn mức, log lý do chọn mức.

## Câu hỏi còn mở

- Ngưỡng 0.4 giữa EASY và MEDIUM có cần đưa thành setting không (đề xuất: hằng số, đổi khi có dữ liệu).
- Gói không gắn `difficulty` (Reading cũ) coi là MEDIUM hay loại khỏi luật mức (đề xuất: coi là MEDIUM).
