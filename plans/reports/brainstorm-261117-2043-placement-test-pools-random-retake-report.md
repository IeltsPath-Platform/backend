# Brainstorm: placement test theo kho band, random không trùng, thi lại, chấm Speaking AI

- Ngày: 2026-11-17
- Trạng thái: thiết kế đã duyệt, chưa lập plan
- Phạm vi: content-service, learning-service, assessment-service, frontend (bài test đầu vào)
- Mode: markdown (không `--html`, không `--wiki`)

## 1. Yêu cầu (vận hành)

- Mỗi khoảng band có một kho đề placement riêng: 0-3 / 3-5 / 5-9.
- Chọn đề random để học viên thi lại ngay mà không trùng đề đã làm.
- Phân tích kèm 2 mở rộng từ buổi brainstorm: band chặn theo trần kho; chấm Speaking của placement bằng Azure thay vì hằng số.

## 2. Hiện trạng (đã scout)

- Chỉ 1 đề: seed V25 `PLACEMENT-4SKILLS` (L/R/W/S). Gói `PLACEMENT_TEST` không có kho/band. `BandRange` chỉ có ở topic.
- Learning `GetPlacementTestUseCase`: `packages.getFirst()`; đã có placement thì `409 PLACEMENT_ALREADY_DONE`; FE `PlacementPage` chuyển về `/learn`. Luật "một lần" chỉ nằm ở đây.
- Assessment mở lượt thi theo `packageVersionId` do client gửi, không biết đề thuộc kho nào, không có luật một lần.
- `learner_placements`: 1 dòng/học viên. `LearnerPlacement.record` đã cho lượt thi mới hơn, hoặc chấm lại cùng lượt, ghi đè band.
- Band placement = trung bình 4 kỹ năng: L/R quy từ số câu đúng (thang 40 câu, bảng IELTS), W do LLM chấm (mặc định 5.5 khi LLM lỗi), **S là hằng số cấu hình, mặc định 0.0** (`assessment.placement.speaking-band`, không có override trong repo). Ví dụ L6, R6, W6, S0 ra 4.5.
- Placement chờ chấm Writing bằng job nội bộ của Assessment (`PlacementGradingService.onSubmit`).
- Learning có luồng chấm Speaking (Azure Speech + LLM) nhưng gắn với luyện tập: tốn 5 điểm/lượt, tối đa 5 lượt/ngày, cần học viên đồng ý, cờ `learning.speaking.enabled` mặc định tắt vì chưa hiệu chuẩn với giám khảo. Plan chấm Speaking AI đang làm đã **loại placement khỏi phạm vi**.
- Ghi âm placement đã upload qua Assessment vào bucket `speaking-recordings` (cùng bucket với Learning).
- Nơi đọc band placement (chỉ đọc, không sửa): gợi ý khóa (`recommended`), band gợi ý học khi chưa có khóa đang học, mốc trong báo cáo học viên. Band chỉ gợi ý, không mở/khóa topic.

## 3. Quyết định

| # | Quyết định |
| --- | --- |
| 1 | Kho cố định 3 mức; học viên **tự chọn** kho trước khi thi |
| 2 | Hết đề chưa làm trong kho: **lặp lại đề giao cách đây lâu nhất**; đề bỏ dở tính là đã làm |
| 3 | Thi lại tối đa **3 lần/ngày**; band lấy **lần mới nhất** |
| 4 | Band **chặn theo trần kho** (3.0 / 5.0 / 9.0); chạm trần thì gợi ý làm kho cao hơn |
| 5 | Speaking: nối chấm Azure, cách **B (chờ chấm xong mới trả kết quả)** |
| 6 | Speaking placement **miễn phí, hạn mức riêng**, không dùng chung 5 điểm/5 lượt của luyện Speaking |
| 7 | Công tắc **riêng cho placement, mặc định tắt**; tắt thì bỏ Speaking khỏi trung bình và trả kết quả ngay |
| 8 | Chờ Speaking quá hạn hoặc lỗi: trả band từ L/R/W, ghi chú chưa chấm Speaking |

Không làm lần này: thi thích ứng, bảng quy đổi riêng mỗi kho (chưa có dữ liệu hiệu chuẩn).

## 4. Thiết kế

### 4.1 Content
- Cột kho cho gói `PLACEMENT_TEST` (3 giá trị cố định, null với loại gói khác). Migration mới, không sửa migration cũ; đề seed cần được gán kho (vận hành quyết định).
- Endpoint nội bộ trả đề đã xuất bản theo kho, kèm kho của từng gói. Cập nhật `learning-content-internal-v1`.
- Khi xuất bản đề placement: cảnh báo nếu kho có ít hơn số đề tối thiểu.
- Trang soạn đề của tác giả: chọn kho cho đề placement.

### 4.2 Learning
- Bảng giao đề: học viên, gói, kho, trạng thái (ASSIGNED / STARTED / COMPLETED), thời gian. Chỉ số theo (học viên, thời gian).
- API lấy đề nhận tham số kho:
  1. Đang có đề giao dở: trả lại đúng đề đó (tải lại không đổi đề).
  2. Không có: kiểm tra giới hạn 3 lần/ngày (theo múi giờ học của học viên) rồi random trong kho trừ các đề đã giao.
  3. Hết đề: lấy đề giao cách đây lâu nhất.
- Bỏ `PLACEMENT_ALREADY_DONE`. Ghi dữ liệu học của học viên mở đầu bằng advisory lock theo học viên (AGENTS.md §3.8/3.9).
- `applyPlacement` giữ nguyên: lượt mới hơn thay band.

### 4.3 Assessment
- Đọc kho của đề từ Content (đã có `ContentPackageProvider`), áp **trần band** khi chấm: `band cuối = min(band tính, trần kho)`; kèm cờ `capped` khi band tính ≥ trần và kho chưa phải cao nhất. Không cần gọi Learning.
- Khi chưa chấm Speaking: trung bình chỉ L/R/W (không dùng 0).
- Kết quả placement trả thêm kho và cờ `capped`; cập nhật contract.

### 4.4 Speaking AI cho placement (cách B)
Chỉ chạy khi công tắc placement bật; khi tắt, trả kết quả ngay như 4.3.
1. Nộp bài: Assessment giữ lượt thi ở trạng thái chờ Speaking và ghi sự kiện outbox `placement.speaking-requested` (attempt, user, prompt, tham chiếu ghi âm). Tên và hình dạng event nằm ở `common-events`, contract trong `docs/contracts/`.
2. Learning (consumer idempotent, retry/DLQ theo mẫu có sẵn) chấm bằng `SpeakingGrader` hiện có với cấu hình placement: không trừ điểm, hạn mức riêng.
3. Learning gọi Assessment (chiều gọi hợp lệ) qua `ResilientCall` để đưa band Speaking (idempotent theo attempt).
4. Assessment hoàn tất kết quả 4 kỹ năng (áp trần), phát `AssessmentCompleted.v2` như hiện nay.
5. Quá hạn: job nền (claim `FOR UPDATE SKIP LOCKED`) hoàn tất bằng L/R/W và ghi chú chưa chấm Speaking. Band Speaking đến muộn bị bỏ qua (YAGNI).
- Cần chốt khi lập plan: học viên đồng ý xử lý ghi âm bằng AI ở placement (luyện Speaking đang yêu cầu đồng ý).

### 4.5 Frontend
- Bước chọn kho (3 thẻ có mô tả, "không chắc thì chọn 3-5").
- Trang kết quả: nút "Làm lại" (hiện số lần còn lại hôm nay), cảnh báo "kết quả mới thay kết quả cũ", gợi ý đổi kho khi `capped`, trạng thái "đang chấm Speaking" khi công tắc bật.
- Cập nhật `mockLearningApi`, `ErrorBoundary`, trạng thái loading/lỗi/hết đề/hết lượt.

## 5. Tiêu chí chấp nhận
- Mỗi kho có đề; học viên chọn kho nào chỉ nhận đề kho đó.
- Thi lại liên tiếp không trùng đề tới khi hết đề trong kho; sau đó lặp đề cũ nhất.
- Tải lại trang giữa chừng không đổi đề; đề bỏ dở tính là đã làm.
- Lần thi thứ 4 trong ngày bị từ chối với mã lỗi rõ ràng.
- Chọn kho 0-3 và làm tốt: band không vượt 3.0 và có gợi ý làm kho cao hơn.
- Công tắc tắt: kết quả trả ngay, Speaking không kéo band xuống (không dùng 0).
- Công tắc bật: kết quả chờ Speaking; lỗi/quá hạn vẫn trả band L/R/W kèm ghi chú.
- Speaking placement không trừ điểm và không tiêu hạn mức luyện Speaking.
- Band mới nhất thay band cũ; gợi ý khóa và gợi ý học theo band mới.

## 6. Rủi ro
- Hết kho: số lần thi lại không trùng bị giới hạn bởi số đề; vận hành phải soạn đủ.
- Writing tốn hạn mức LLM theo ngày: thi lại liên tục làm Writing rơi về band mặc định 5.5. Giới hạn 3 lần/ngày giảm nhưng không loại bỏ.
- Trần band chỉ chặn band cao ngảo; không sửa band thấp khi chọn kho quá khó.
- Band Speaking AI chưa hiệu chuẩn: giữ công tắc mặc định tắt cho tới khi vận hành tin chất lượng.
- Assessment vẫn nhận mọi `packageVersionId` hợp lệ: người dùng biết UUID có thể vượt giới hạn 3 lần/ngày. Chấp nhận ở bước đầu; có thể chặn thêm ở Assessment sau.
- Thống kê band (`BandStatistics`) có thể đếm một học viên nhiều lần; cần quyết định.
- Đổi cách tính band (bỏ 0 của Speaking khi công tắc tắt) làm band placement mới cao hơn trước; không đổi band đã lưu.

## 7. Chia việc
- **Luồng 1 (kho đề, random, thi lại, trần band, bỏ Speaking khỏi trung bình khi chưa chấm):** Content, Learning, Assessment (trần), Frontend.
- **Luồng 2 (Speaking AI cho placement, cách B):** event, Learning chấm, Assessment chờ và quá hạn, công tắc, hạn mức. Có thể làm plan riêng; luồng 1 phát hành độc lập được vì công tắc mặc định tắt.

## 8. Câu hỏi còn mở
- Số đề tối thiểu mỗi kho và đề seed hiện có thuộc kho nào.
- Giá trị thật của `ASSESSMENT_PLACEMENT_SPEAKING_BAND` trên server vận hành (nếu đã đặt khác 0).
- Biên chính xác giữa các kho (3 và 5 thuộc kho nào); trần 3.0/5.0 có bao gồm biên không.
- Thời gian chờ Speaking tối đa trước khi hết hạn.
- Cách xin đồng ý xử lý ghi âm bằng AI ở placement.
- Thi lại có tính nhiều lần trong `BandStatistics` không.
