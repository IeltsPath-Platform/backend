# Brainstorm: rò nội dung khóa chưa mua qua truy vấn KP ngược

- Ngày: 2026-11-17
- Trạng thái: thiết kế đã duyệt (hướng A), chưa lập plan
- Phạm vi: learning-service (chính), frontend khu bài ôn
- Mode: markdown (không `--html`, không `--wiki`)
- **Cập nhật khi lập plan (2026-11-17):** phần "ẩn lý thuyết bài ôn" đã bỏ. Xác minh: mọi bài ôn trỏ tới lesson học viên đã học (`CompletedLessonLocator.teaching` fallback lesson của bài ôn gốc; `ReviewRule`, `ErrorTrapReviews` dùng lesson đã làm), nên người chưa học khóa thấp không thấy lý thuyết khóa đó. Trường hợp hết Premium sau khi học để sau (Premium chưa làm kỹ). Plan chỉ lọc gợi ý: [plan](../261117-0829-suggestions-hide-unowned-course-lessons/plan.md).

## 1. Vấn đề

Yêu cầu gốc: mua một khóa thì chỉ truy cập khóa đó, không mở khóa band thấp hơn.

Kết quả scout:
- Mua khóa (unlock bằng điểm, `UnlockCourseUseCase`) chỉ ghi `learner_course_unlocks` cho đúng một `courseId`. `PaywallGate`/`PaywallPolicy` xét theo từng khóa, không có luật band. Access-service không biết khóa học (chỉ Plan/Premium, ví điểm, key). → Mở lesson của khóa band thấp đã bị chặn đúng.
- Câu hỏi thật của người dùng: truy vấn kiến thức ở khóa cao có đi ngược xuống KP khóa thấp không. **Có**, ở ba chỗ:
  1. `DiagnosticFlow.prerequisitesOf` đi đồ thị tiên quyết chung (≤ 3 tầng), không lọc theo khóa; thăm dò bằng bộ đề trong kho theo KP; kết luận tạo bài ôn chặn `DIAGNOSTIC` cho KP gốc, có thể thuộc khóa chưa mua. Có chủ đích: `PREREQUISITE_ORDER` cho phép tiên quyết nằm ở khóa band thấp hơn.
  2. `ReviewResult.theory` chép khối TEXT của lesson dạy KP (`KNOWLEDGE_POINT`), hoặc **toàn bộ** TEXT của lesson (`LESSON_FALLBACK`). Use case bài ôn không qua paywall → **lộ bài giảng khóa chưa mua**.
  3. `GetSuggestionsUseCase` lấy candidates band ≤ band cao nhất đang học (gồm khóa thấp hơn), không lọc paywall → gợi ý bài rồi bấm vào bị `403 PAYWALL`.
- Đã chặn đúng: đọc lesson (`LessonAccess`), cây kiến thức, luyện tự do theo KP (`knowledgePointOpen`).

## 2. Các hướng đã cân nhắc

| Hướng | Nội dung | Ưu | Nhược |
| --- | --- | --- | --- |
| **A (chọn)** | Giữ dò ngược + bài ôn bằng câu hỏi trong kho; ẩn lý thuyết bài ôn từ lesson không mở được; lọc gợi ý | Chặn đúng chỗ rò bài giảng, giữ giá trị chẩn đoán, thay đổi nhỏ | Câu hỏi kho của KP khóa thấp vẫn dùng trong bài ôn |
| B | Chẩn đoán chỉ dò KP thuộc khóa đã mua/miễn phí | Ranh giới tuyệt đối | Chẩn đoán vô dụng với người mua khóa cao (nền nằm ở khóa thấp); thêm kiểm tra quyền trong vòng lặp dò |
| C | Như A, KP gốc thuộc khóa chưa mua chỉ tạo bài ôn tùy chọn | Không chặn người đã trả tiền | Học viên bỏ qua lỗ hổng nền; đổi luật sư phạm `diagnostics-v1.md` |

## 3. Giải pháp đã duyệt

**Nguyên tắc:** câu hỏi trong kho dùng chung; bài giảng và lesson chỉ cho người mở được khóa. "Mở được" = đúng luật `PaywallGate` hiện có (unlock skill/combo, khóa miễn phí, 3 bài xem thử mỗi kỹ năng, Premium, `grandfathered`). Không tạo luật mới.

1. **Chẩn đoán và bài ôn:** giữ nguyên. Không đổi `diagnostics-v1.md`.
2. **Lý thuyết bài ôn** (learning-service): khi dựng `ReviewResult`, hỏi `paywall.forLearner(userId).lessonOpen(...)` cho lesson nguồn. Không mở được → `theory = []` + trường mới `theoryLockedCourse { courseId, title }` (mặc định null, additive). Giữ `quickCheck` và bộ câu hỏi ôn. Cập nhật contract trong `docs/contracts/` cùng PR.
3. **Gợi ý học** (learning-service): `GetSuggestionsUseCase` bỏ lesson và practice test thuộc khóa không mở được; giữ bài xem thử. Bộ luyện tự do theo KP giữ nguyên. Hình dạng API không đổi.
4. **Frontend** (khu bài ôn): `theory` rỗng + có `theoryLockedCourse` → hiện "Lý thuyết của phần này nằm trong khóa {title}" kèm nút xem khóa; giai đoạn lý thuyết vẫn làm được câu kiểm tra nhanh.

**Ngoài phạm vi:** mô hình bán khóa, Premium, 3 bài xem thử (giữ nguyên), luật `PREREQUISITE_ORDER` khi soạn khóa, nhãn "mua khóa" trong gợi ý (đã chọn ẩn hẳn).

## 4. Tiêu chí chấp nhận

- Học viên có unlock khóa 6.5, không có 5.0, không Premium: bài ôn KP có lesson nguồn là bài thứ 5 của kỹ năng ở khóa 5.0 → `theory = []`, `theoryLockedCourse` = khóa 5.0; câu hỏi ôn vẫn có.
- Cùng KP, lesson nguồn là bài 1–3 (xem thử) hoặc học viên có Premium → lý thuyết như cũ.
- Gợi ý học không chứa lesson/practice test khóa 5.0 ngoài 3 bài xem thử.
- Chẩn đoán vẫn tạo bài ôn `DIAGNOSTIC` cho KP khóa 5.0.
- Client cũ không vỡ (trường mới null mặc định).

## 5. Rủi ro và điểm cần xác minh khi lập plan

- Bài ôn `DIAGNOSTIC` gắn `lessonId` nào (lesson gốc khóa 6.5 hay lesson dạy KP khóa 5.0): đọc `DiagnosticFlow.conclude` trước. Nếu lý thuyết lấy từ lesson khóa 6.5 thì nhánh này không rò.
- Liệt kê mọi nơi dựng `ReviewResult` có `theory` (bài ôn thường, `WRITING_CRITERION`, giỏ ôn) để không sót đường.
- Candidates gợi ý từ Content có `courseId` và `skillPosition` của lesson chưa; thiếu thì phải mở rộng contract nội bộ `learning-content-internal-v1`.
- `PaywallGate` đọc unlock theo từng khóa (`computeIfAbsent`); với gợi ý, số khóa nhỏ, ghi chú cận trên. Lỗi Access (Premium) khi lọc gợi ý: chọn fail-closed (ẩn) hay trả lỗi, quyết định trong plan.
- Test: Mockito cho use case (lọc gợi ý, ẩn lý thuyết theo từng nhánh mở được), Testcontainers nếu chạm query.

## 6. Bước tiếp theo

- Lập plan (`/ck:plan --tdd` khuyến nghị: đổi hành vi bài ôn/gợi ý đang có test) với báo cáo này làm đầu vào.

## Câu hỏi còn mở

- Hành vi khi Access không phản hồi lúc lọc gợi ý (ẩn bài trả phí hay trả lỗi 503).
