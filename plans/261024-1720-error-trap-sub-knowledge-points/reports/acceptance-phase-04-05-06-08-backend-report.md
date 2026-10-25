# Nghiệm thu phase 4, 5, 6, 8 (backend learning)

Người nghiệm thu: Claude. Ngày: 2026-10-25. Không commit.

## Kết luận: CHƯA ĐẠT, trả Codex bổ sung test và dọn code

Code đọc được **đúng thiết kế**; vấn đề là **các đường chấm chưa được test thật**, nên chưa có bằng chứng chạy được.

| Phase | Kết quả |
| --- | --- |
| 4 Bằng chứng KP con | Code đạt; **thiếu test** (T1, T2, T3) |
| 5 Chọn bộ + ERROR_PATTERN | Code đạt; **thiếu test** (T4) |
| 6 Phản hồi + báo cáo | **thiếu test** (T5) |
| 8 Docs + kiểm toàn bộ | Docs đạt; **kiểm thủ công bị bỏ do hiểu sai** (V1) |

## Kiểm độc lập
- `mvn -pl services/learning-service -am test` (Claude, 14:30-14:37): BUILD SUCCESS, **929 test, 0 lỗi, 0 skip**. Khớp báo cáo Codex.
- Đọc: V36, `TrapEvidenceRule`, `ErrorTrapEvidence`, `ErrorTrapReviews`, `MainErrorRule`, diff 5 đường chấm, `GetReviewUseCase`,
  `StartBasketReviewUseCase`, `GetReviewBasketUseCase`, SQL lọc KP con ở `JdbcMasteryProfileRepository`.
- Đúng: điều kiện `counted` ở Practice và luyện tự do; chẩn đoán/ôn/lesson ghi mọi lần; bài thi không ghi; `withoutMilestone` truyền vào
  `recordEvidence`; tạo bài ôn sau `recordEvidence`; catalog đọc từ DB (không HTTP khi ghi bằng chứng); `StartBasketReviewUseCase`
  chặn KP con bằng 409; bảng mục 6 của phase 4 giải trình đủ từng chỗ; `findAll` và `findMasteryHistories(userId)` mặc định loại KP con.

## Lỗi phải sửa
**T1 (chặn): không test nào chạy qua `ErrorTrapEvidence` trong 5 đường chấm.** Test mới của phase 4 chỉ gồm `TrapEvidenceRuleTest`,
`ErrorTrapMp1ParamsTest` và sửa `MasteryProfileIntegrationTest`. Các use case giữ **constructor cũ truyền `null`** nên mọi test use case
cũ chạy mà tính năng tắt. Plan yêu cầu Testcontainers: mỗi đường (lesson, Practice `counted`, ôn, luyện tự do, chẩn đoán) ghi đúng số
dòng `kp_evidence` `source='error_trap'`; Practice không `counted` không ghi; bài thi không ghi; ví dụ "5 câu bẫy X: X, đúng, X, Y, đúng"
→ KP con X có 4 bằng chứng (✗ ✓ ✗ ✓).

**T2 (chặn): thiếu test "không XP mốc" và "danh mục không có KP con → không ghi, không lỗi".**

**T3 (chặn): thiếu test "tổng hợp không đổi".** Thêm KP con có bằng chứng cho hồ sơ mẫu thì `BandEstimator`, `skills`/`focus`, bản chụp,
nhiệm vụ ngày, thống kê staff, huy hiệu phải bằng đúng giá trị khi không có KP con. Đây là chốt chặn của rủi ro "bỏ sót chỗ tổng hợp".

**T4 (chặn): thiếu kịch bản Testcontainers của phase 5.** Tối thiểu: Lan/Minh (bộ ôn KP cha đầu tiên lần lượt PS-A và PS-B); KP cha 85% nhưng
KP con xuống `PRIORITY` → một `ERROR_PATTERN` của KP cha; KP cha đang có bài ôn → không tạo; ôn xong mà KP con vẫn `PRIORITY` → tạo lại,
lần kế không thêm; luyện tự do khi chưa học lesson → không tạo; mở bài ôn có lỗi chính → `errorTrap`; lấy tên lỗi từ Content lỗi → bài ôn vẫn mở;
KP thường không có KP con → hành vi như cũ.

**T5 (chặn): phase 6 chỉ có test hình dạng DTO** (`ErrorTrapFeedbackContractTest`, `ErrorTrapFeedbackResponseTest`,
`ErrorTrapNestingContractTest`). Thiếu: chọn bẫy có mã → `error` đúng; chọn đúng → không `error`; mã không có trong danh mục → không `error`;
Content lỗi → lần nộp vẫn thành công; báo cáo/cây có `errorTraps` chỉ gồm KP con **có bằng chứng**; giỏ ôn `ERROR_PATTERN` có `errorTrap`;
chi tiết bài ôn có `blocking`.

**V1 (chặn): phase 8 bỏ kiểm thủ công vì hiểu sai.** Ràng buộc "không gọi HTTP khi ghi bằng chứng" nói về **code** (đọc mã lỗi từ catalog
trong `learning_db`), không cấm chạy thử. Lời tự kiểm "integration tests exercised ... submission" trong `verification.md` không đúng
(xem T1). Cần hoặc chạy Compose thật hoặc, nếu không chạy được, một test tích hợp end-to-end có thật (Content thật hoặc giả có `distractors`
kèm catalog KP con → nộp bài → có bằng chứng, hồ sơ KP con, bài ôn `ERROR_PATTERN`).

## Nên dọn (không chặn)
- **S1** Tám use case mang constructor cũ truyền `null` (`ErrorTrapEvidence`, `FreePracticeSubmission`, `GetReviewUseCase`,
  `StartBasketReviewUseCase`, `GetReviewBasketUseCase`, `SubmitPracticeAttemptUseCase`...) chỉ để test cũ biên dịch được, và âm thầm tắt tính năng.
  Bỏ overload, sửa test cũ truyền bean thật hoặc mock; `ErrorTrapEvidence` cũng có hai constructor `null`.
- **S2** `SubmitPracticeAttemptUseCase` dòng ~160 truyền chuỗi giả `"ESSAY_RESULT"` để bắt buộc gọi Content khi có bài viết. Thay bằng điều kiện rõ ràng.
- **S3** `GetReviewUseCase` tính `errorTrap` kể cả khi bài ôn đã DONE/SKIPPED (tốn một truy vấn hồ sơ mỗi lần mở). Chỉ cần khi `PENDING`.
- **S4** `verification.md`: frontend còn 2 test `owl-reactions` đỏ ngẫu nhiên (có sẵn từ baseline, không liên quan); chấp nhận.

## Việc còn lại sau khi Codex sửa
- Tôi chạy lại toàn module learning, đọc test mới, rồi `ck plan check 4 5 6 8`.
- Sau đó mới chạy frontend (phase 7) với backend thật, đối chiếu hợp đồng JSON.

## Câu hỏi mở
- Không.

## Nghiệm thu lần 2 (2026-10-25 15:50): ĐẠT, có ghi chú
- Claude chạy lại `mvn -pl services/learning-service -am test`: BUILD SUCCESS, **941 test, 0 lỗi, 0 skip** (Codex báo 941).
- T1, T2 đạt: test Testcontainers cho Lesson (cả ca 5 câu → ✗✓✗✓ cho X, 1 bằng chứng cho Y), Practice `counted`, luyện tự do, ôn, chẩn đoán;
  `ErrorTrapEvidenceTest` (không XP mốc, thiếu ánh xạ không lỗi).
- T3 đạt một phần có kiểm: `MasteryProfileIntegrationTest.trapEvidenceBuildsChildMastery...` so `estimatedBand`, `skills`, `focus`, danh sách KP trước/sau khi thêm KP con
  và không có dòng `xp_ledger` mốc. Bản chụp, thống kê staff, huy hiệu không có test riêng; Claude rà thủ công: ba repo đó đã join catalog lọc `parent_kp_id IS NULL`
  (báo cáo phase 4 mục 6), không còn chỗ đọc hồ sơ nào khác ngoài `findBatchAfter` (job decay, cần cả KP con). Chấp nhận, ghi nhận rủi ro.
- T4, T5 đạt: ERROR_PATTERN, 409 KP con, `errorTrap` khi mở bài ôn, bỏ qua tra tên lỗi khi DONE/SKIPPED, lỗi Content không làm hỏng lần nộp, `blocking`, `errorTraps` ở báo cáo.
- S1, S2, S3 đã làm (bỏ constructor null, bỏ `ESSAY_RESULT`, chỉ tra khi PENDING).
- **Kiểm đột biến của Claude:** bỏ điều kiện `if (counted)` quanh `errorTrapEvidence.append` ở `SubmitPracticeAttemptUseCase` → `PracticeIntegrationTest.practiceWritesTaggedChildEvidenceOnlyForTheCountedPackageSubmission` đỏ. Đã khôi phục (hash trùng bản gốc).
- V1: Compose không chạy được (không có service nào đang chạy); thay bằng test end-to-end Testcontainers + Content stub. Chấp nhận; **việc chạy thật qua HTTP còn nợ** → làm khi chạy frontend với backend thật.

## Còn lại
- Chạy frontend phase 7 với backend thật, đối chiếu hợp đồng JSON (so `error`, `errorTrap`, `errorTraps`, `blocking`).
- Giám khảo IELTS duyệt dữ liệu mẫu V80 và `fixHint` của mã DRAFT trước khi cho học viên.
