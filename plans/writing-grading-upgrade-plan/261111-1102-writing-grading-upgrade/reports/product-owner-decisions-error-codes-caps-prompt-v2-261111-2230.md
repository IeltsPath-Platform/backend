# Quyết định thay examiner: mã lỗi, ngưỡng band, prompt v2

Ngày 2026-11-11. Người dùng (chủ sản phẩm) giao quyết định thay vì chờ examiner. Các quyết định dưới đây **chưa qua examiner**;
có thể đổi sau bằng migration hoặc cấu hình mới.

## 1. Mã lỗi Writing (phase 3) — migration Content `V125`

V124 giữ cho Dev B (work-split 261111-1628), nên dùng V125. Kiểm lại số trước khi merge.

| Mã | Tên | Nhóm | Mức | Kiến thức | Lỗi trong bộ chuẩn |
|---|---|---|---|---|---|
| `PASSIVE_VOICE` | Sai cấu trúc bị động | GRAMMAR | 2 | `G55_PASSIVE_VOICE` | AN-03 E03–E05, AN-04 E02, E05 |
| `VERB_FORM` | Sai dạng động từ | GRAMMAR | 2 | `G55_BASIC_SENTENCE_PATTERNS` | CORE-01 E20, AN-05 E01, E04, AN-07 E02 |
| `PREPOSITION` | Sai giới từ | GRAMMAR | 1 | `V55_ACADEMIC_COLLOCATIONS` | CORE-01 E21, AN-02 E07 |
| `COMPARISON` | Sai cấu trúc so sánh | GRAMMAR | 1 | `G55_COMPARISON` | AN-02 E06 |
| `T2_PARTIAL_ANSWER` | Trả lời thiếu phần của đề Task 2 | TASK | 3 | `W65_DISCUSS_BOTH_VIEWS` | (phase 8) |
| `T2_NO_CONCLUSION` | Thiếu kết bài Task 2 | TASK | 2 | `TR_W_OPINION` | (phase 8). **Bỏ ngày 2026-11-12 (V127, `INACTIVE`):** chỉ còn cờ cấu trúc `noConclusion` |
| `T2_IRRELEVANT_EXAMPLE` | Ví dụ không liên quan hoặc quá chung chung | TASK | 2 | `W70_IDEA_DEVELOPMENT` | (phase 8) |

- `NO_CLEAR_POSITION` gắn kiến thức `TR_W_OPINION` (trước đây không có).
- **Chưa tạo điểm error-trap `TRAP_<mã>`** cho 8 mã này. `ErrorTrapCoverageMigrationTest` đòi mọi mã có điểm trap phải có 2 bộ luyện tập đã xuất bản với câu hỏi bẫy; các câu hỏi đó chưa có, và câu hỏi cho học viên do AI soạn phải qua người duyệt (`AGENTS.md`). Điểm trap thêm cùng migration thêm bộ luyện tập; **chủ sản phẩm quyết hoãn việc này** (xem phase-03, mục Quyết định hoãn). Hệ quả: lỗi chấm ra các mã này hiện tên và gợi ý sửa, nhưng chưa ghi bằng chứng error-trap.
- CORE-01 E22 (`This data suggest`) gộp vào `SUBJECT_VERB_AGREEMENT`, đúng quy tắc examiner ghi.
- CORE-01 E14 (gợi ý) và E24 giữ `GRAMMAR_OTHER`: hai lỗi khác kiểu, không đủ để mở mã.
- Lý do chọn `V55_ACADEMIC_COLLOCATIONS` cho giới từ: lỗi trong bộ là giới từ đi theo cụm cố định (increase by, despite + N); chưa có kiến thức ngữ pháp riêng cho giới từ.
- Đã chạy thử V125 trên `content_db` local trong transaction rồi rollback: đủ 8 mã, đủ kiến thức. Test migration của content-service đã cập nhật: `GrammarErrorCodeMappingMigrationTest` (NO_CLEAR_POSITION nay có kiến thức `TR_W_OPINION`, chưa có trap) và `ErrorTypeSeedMigrationTest` (thêm hai cột `skills`, `fallback` vì test dừng ở V81; test này đã fail từ khi V123 thêm cột).
- Bộ chuẩn `gold-examiner-v1.json` (ngoài repo) đã gắn lại 13 lỗi; còn 2 lỗi `GRAMMAR_OTHER`.

**Rủi ro (theo phase 3):** danh mục gửi cho LLM thêm 7 mã mà `promptVersion` không đổi, nên hành vi chấm đổi ngay khi V125 chạy.
Phải chạy lại eval prompt v1 với danh mục mới và so với số nền trước khi merge.

## 2. Prompt v2 (phase 4)
- Duyệt nội dung `docs/contracts/essay-prompt-template-v2.txt`; vẫn **chưa kích hoạt**.
- Sửa một câu: ví dụ minh họa lỗi phải là câu mới, không lấy từ bài (bản cũ dễ hiểu thành "một câu khác trong bài").
- Giữ yêu cầu liệt kê mọi lỗi; độ trễ với bài dài đo ở spike phase 0.
- Giữ định nghĩa ERROR (vi phạm quy tắc) và SUGGESTION (đúng nhưng có cách rõ ràng tốt hơn).

## 3. Ngưỡng band (phase 6)

| Luật | Band tối đa của TA/TR | Căn cứ |
|---|---|---|
| Bài quá ngắn | 5.0 | Thiếu độ dài bị trừ ở TA/TR |
| Lạc đề | 4.0 | Band 4: trả lời tối thiểu hoặc lạc hướng |
| Thiếu tổng quan (Task 1) | 5.0 | Band 6 TA đòi có tổng quan |
| Trả lời thiếu phần đề (Task 2) | 5.0 | Band 5 TR: chỉ trả lời một phần |
| Không có quan điểm (Task 2) | 5.0 | Band 6 TR đòi quan điểm liên quan, rõ |

Đặt làm mặc định trong `config-repo/learning-service.yaml`; `band-caps-enabled` **vẫn false**. Bốn cờ cuối chỉ có
khi prompt v2 trả về; bật sau khi có eval với cap.

## 4. AN-01 E08
Câu sửa: "January and February, as well as between February and March" (phương án A).

## Eval sau V125 (danh mục mã mới)
Content-service đã build lại, V125 đã áp vào DB local, 8 bài chuẩn đã gắn mã mới. Chạy eval 3 lượt (Task 1 và Task 2): **không lượt nào đủ bài**, vì Google trả `429 RESOURCE_EXHAUSTED` và `503 UNAVAILABLE` (gói miễn phí, 5 bài gọi dồn). `invalid_grade_rate` từ 0.2 đến 0.67 nên `passed = false`; số liệu không so được với số nền.
Chỉ có dấu hiệu sơ bộ từ các bài chấm được: không bài nào lệch quá 0.5 band; `error_recall` Task 1 0.89 đến 0.93 (số nền 0.94); `data_error_recall` 0.77 đến 0.83 (số nền 0.75). n quá nhỏ để kết luận.

## Chưa giải quyết
- Chạy lại eval sau V125 khi API ổn định (hạn mức Gemini miễn phí không đủ cho 5 lượt gọi đồng thời); nên bật thanh toán cho project hoặc giãn lượt gọi.
- Commit và PR: chưa làm.
