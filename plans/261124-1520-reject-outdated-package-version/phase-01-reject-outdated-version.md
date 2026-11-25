# Phase 01: Chặn bắt đầu làm bài trên bản không còn hiện hành

Plan: [plan.md](plan.md) · Trạng thái: todo

## Context

- `services/content-service/.../infrastructure/persistence/adapter/JdbcLearningContentReader.java`:
  `publishedPackageVersion` và `packageVersionContent(id, publishedOnly)` (khoảng dòng 857-880), điều kiện `v.status = 'PUBLISHED'`.
- `content-service/.../application/usecase/GetPackageVersionContentUseCase.java`, `InternalLearningContentController`
  (`/internal/learning-content/package-versions/{id}`).
- `assessment-service/.../application/usecase/StartAssessmentAttemptUseCase.java` (dòng 31-32, `AssessmentNotFoundException`),
  `infrastructure/client/ContentPackageClient.java` (404 thành `Optional.empty()`).
- `learning-service`: `AssignPlacementTestUseCase`, `PlacementAssignment`, `JdbcPlacementAssignmentRepository`
  (lưu `packageVersionId` cụ thể cho từng lượt giao placement).
- `AGENTS.md` §3.3 (lỗi tiếng Việt), §3.5 (contract), §5.

## Bước 0: kiểm tra bên gọi (làm trước, chưa sửa code)

`publishedOnly = true` hiện chỉ có assessment gọi qua endpoint nội bộ trên. Cần xác nhận lại bằng `grep` toàn repo
(kể cả `mobile/` nếu có) rằng không còn bên gọi nào, rồi trả lời:

1. Learning giao placement kèm `packageVersionId` cụ thể. Nếu bản mới được xuất bản sau khi giao mà trước khi học viên
   bấm "Bắt đầu", học viên sẽ bị chặn. Giao đề chọn bản **hiện hành tại lúc giao** hay bản khác? Học viên có cách nào
   lấy lại đề mới (release assignment, giao lại) không? Ghi kết luận vào mục "Kết quả Bước 0".
2. Review set và diagnostic probe của learning lưu `packageVersionId`: kiểm xem chúng có đi qua đường assessment-start
   không. Nếu không thì không bị ảnh hưởng.
3. Frontend/mobile có cache `packageVersionId` lâu hơn một phiên không (không sửa, chỉ ghi nhận).

Kết luận đã có ở mục "Kết quả Bước 0" bên dưới; Codex chỉ xác nhận lại. Nếu grep cho thấy có bên gọi khác dùng endpoint
này, hoặc học viên có thể kẹt lâu hơn 15 phút: **dừng và hỏi chủ dự án**.

## Bước 1: sửa truy vấn (content-service)

Khi `publishedOnly`, thêm điều kiện bản phải là bản hiện hành của gói:

```sql
WHERE v.id = :versionId AND v.status = 'PUBLISHED' AND v.id = p.current_published_version_id
```

- `packageVersionContent(id, false)` (đọc cho khu author, xem trước) **không đổi**.
- Không đổi hình dạng response, không thêm endpoint. Bản cũ rơi vào nhánh "không tìm thấy" đang có.
- Cập nhật `docs/contracts/learning-content-internal-v1.md`: endpoint chỉ trả bản **hiện hành**; bản cũ trả 404.

## Bước 2: thông báo lỗi (assessment-service)

`StartAssessmentAttemptUseCase` đang ném `AssessmentNotFoundException("Package version not found")` (tiếng Anh).
Đổi thành câu tiếng Việt dùng chung cho cả hai trường hợp (không có bản, hoặc bản đã được cập nhật), ví dụ:
"Đề này đã được cập nhật hoặc không còn. Hãy tải lại trang để làm bản mới nhất." Không đưa UUID vào câu.
Cập nhật test đang so khớp câu cũ nếu có (`hasMessageContaining`).

## Test

- Persistence (Testcontainers, content-service): gói có hai bản `PUBLISHED`, hiện hành là bản 2: bản 2 đọc được, bản 1
  trả rỗng; gói một bản hiện hành đọc bình thường; bản `DRAFT` vẫn trả rỗng; đường đọc cho author vẫn đọc được bản cũ.
- Use case (Mockito, assessment-service): `findPackageVersion` rỗng thì ném lỗi với câu tiếng Việt, không gọi `AttemptCreator`.
- Giữ xanh `ContentPackageClientTest` (404 thành rỗng).
- Chạy hẹp trước, rồi cả hai service và `ArchitectureTest`.

## Rủi ro

- Học viên được giao assignment placement trong 15 phút trước khi bản mới xuất bản bị chặn tới khi hết cửa sổ dùng lại:
  đã chấp nhận (xem Kết quả Bước 0), ghi vào mô tả PR.
- Gói có đúng một bản `PUBLISHED` nhưng `current_published_version_id` bị null do dữ liệu cũ: truy vấn mới sẽ trả rỗng.
  Kiểm bằng `SELECT` đếm trước khi triển khai; nếu có, dừng và báo.
- Đổi hành vi endpoint nội bộ đã có contract: cập nhật tài liệu cùng PR.

## Kết quả Bước 0 (đã đọc code, Codex xác nhận lại bằng grep trước khi sửa)

- Placement: `AssignPlacementTestUseCase` chọn gói từ `PlacementCatalog.packages(pool)` (bản hiện hành lúc giao). Assignment
  được dùng lại trong `learning.placement.reuse-window-minutes` (mặc định 15 phút, `findReusable`). Quá 15 phút, lần giao
  kế tiếp chọn bản hiện hành mới.
- Hệ quả sau khi sửa: chỉ có một cửa sổ hẹp, tối đa 15 phút kể từ lúc giao. Nếu bản mới xuất bản trong cửa sổ đó, học
  viên bấm "Bắt đầu" bị chặn, và tải lại trang vẫn nhận assignment cũ (còn trong cửa sổ dùng lại) cho tới khi hết 15 phút.
  Không kẹt vĩnh viễn, không mất lượt thi (chưa có attempt nào được tạo), nhưng hạn mức 3 lần trong 24 giờ đã bị tính cho
  assignment cũ.
- **Đã chốt: chấp nhận cửa sổ hẹp này, không sửa learning-service trong phạm vi plan.** Nếu sau này gặp thực tế, cách
  chữa là cho `findReusable` bỏ qua assignment có `packageVersionId` không còn là bản hiện hành (plan riêng).
- Review set và diagnostic probe của learning lưu `packageVersionId` nhưng đọc nội dung qua đường learning-content của
  learning, không qua assessment-start: không bị ảnh hưởng. Codex xác nhận lại bằng grep.
- Các loại gói khác (practice, topic test, course test): học viên chọn từ catalog trả bản hiện hành, nên chỉ bị chặn khi
  giữ ID cũ qua tab mở sẵn hoặc đường dẫn đã lưu, đúng mục tiêu.
