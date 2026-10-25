# Báo cáo phase 5 — Learning review selection and trap reviews

Ngày: 2026-10-25  
Trạng thái: DONE — tự kiểm Success Criteria đạt.

## Quy trình và kiểm chứng

- Baseline trước sửa: `mvn -q -pl services/learning-service -am test` — exit 0; 122 suites, 908 tests, 0 failures, 0 errors, 0 skips. Surefire kết thúc fork sau 30 giây do listener nền còn chạy.
- Đỏ trước implementation: `MainErrorRuleTest` và `ErrorTrapReviewsTest` mới không biên dịch do thiếu `ErrorTrapReviews` (hành vi trigger chưa tồn tại). Đây là lỗi đỏ đúng lý do.
- Test tập trung: `MainErrorRuleTest`, `ErrorTrapReviewsTest`, `ErrorTypeTextsTest`, `ReviewBasketIntegrationTest`, `RestLearningContentClientTest` — exit 0. Bao phủ chọn mã thấp nhất/hòa theo mã, tier không hợp lệ, pending parent, không lesson, Content lỗi, ngưỡng 2 trap questions, giới hạn một review, tái tạo sau review hoàn tất, trả `errorTrap` khi mở review, lọc KP con khỏi giỏ và gửi mã lỗi khi tìm set.
- Lệnh toàn module cuối: `mvn -q -pl services/learning-service -am test` — exit 0; 125 suites, 922 tests, 0 failures, 0 errors, 0 skips. Surefire in cảnh báo fork kết thúc 30 giây sau `System.exit(0)` do listener nền; Maven trả exit 0.
- Ngoại lệ ghim số đếm: phase 5 không sửa assertion số lượng seed hoặc dữ liệu cũ. Test ghim phiên bản migration từ phase 4 vẫn là `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` (V36).

## Thay đổi

- Thêm `MainErrorRule`: lỗi chính của KP cha là mã của KP con có tier khác `INSUFFICIENT`, mastery thấp nhất; hòa theo mã alphabetic.
- Thêm `ErrorTrapReviews`, giới hạn mặc định 1 review mỗi submission, xử lý một lần danh sách pending, ưu tiên lesson hiện tại khi nó dạy KP cha, loại package đã mở/đã giao, chỉ tạo `ERROR_PATTERN` optional cho KP cha khi set có ít nhất hai câu của lỗi đó. Lỗi từ Content được ghi log với user/KP/mã và không lan sang submission.
- Gọi trigger ngay sau `recordEvidence` trong lesson exercise, Practice có tính điểm, review, free practice có tính điểm và diagnostic probe. Assessment không được thêm trigger.
- Thêm overload search truyền `trapErrorCode`; cả kiểm tra set ở `StartBasketReviewUseCase` và lúc mở/lấy set tiếp theo ở `GetReviewUseCase` dùng lỗi chính hiện tại. Luyện tự do và diagnostic vẫn dùng overload cũ. Basket từ chối KP con với `409 NOT_IN_REVIEW_BASKET`.
- Thêm `ErrorTypeTexts`; lấy tên và `fixHint` tối đa một lần mỗi lần mở review, lỗi lookup trả map rỗng.
- `ReviewResult` thêm tùy chọn `errorTrap: {code, name, fixHint}`. Review trả về lỗi chính hiện tại cả khi ở theory sau lần trượt đầu.
- Cập nhật `learning.error-trap.review-limit` và `docs/contracts/lesson-learning-v1.md`.

## Tự kiểm Success Criteria

- [x] Bộ rule, trigger, Content failure, basket child rejection, review/error display và search mã lỗi đã được kiểm bằng unit/integration tests.
- [x] Không tạo review cho KP con; review `ERROR_PATTERN` gắn KP cha, `blocking=false`.
- [x] Contract lesson-learning cập nhật trigger, selection và trường `errorTrap`.

## Ghi chú

Không gọi HTTP khi ghi bằng chứng; mapping mã lỗi/KP con lấy từ catalog DB. Không sửa migration cũ, không commit.

## Bổ sung sau nghiệm thu chưa đạt

- Testcontainers đã chạy `ReviewBasketIntegrationTest.aPriorityTrapCreatesOneOptionalReviewForItsParent`,
  `anErrorPatternReviewUsesTheCurrentMainTrapAndReturnsItsFixHint`, `aTrapKnowledgePointCannotBeStartedFromTheReviewBasket`,
  `anErrorPatternBasketItemShowsTheCurrentMainError`, cùng hai test submit review/diagnostic mới trong
  `DiagnosticIntegrationTest`. Các bài ôn gắn parent; child không mở trực tiếp từ basket.
- Thêm `completedOrSkippedReviewDoesNotLookUpTrapText` cho trạng thái `DONE` và `SKIPPED`, với child priority có catalog;
  test xanh khi Content lookup bị bỏ qua.
- Red-proof trigger: đảo tạm điều kiện giới hạn trong `ErrorTrapReviews.create` để không bao giờ tạo review; test
  `aPriorityTrapCreatesOneOptionalReviewForItsParent` đỏ tại kỳ vọng pending `ERROR_PATTERN`.
- Red-proof S3: đảo tạm `GetReviewUseCase` về tính `errorTrap` vô điều kiện; cả hai trạng thái DONE/SKIPPED đỏ vì gọi
  `activeErrorTypes()`. Điều kiện chỉ tính khi PENDING đã khôi phục.
- End-to-end free-practice test kiểm tra review tùy chọn được tạo cho parent sau khi child chuyển `PRIORITY`; red-proof bằng đảo
  append làm test đỏ trước bước evidence/profile/review.
- Lần chạy cuối `mvn -q -pl services/learning-service -am test`: exit 0; 129 suites, 941 tests, 0 failures/errors/skips.
- Không ghim hoặc đổi assertion số đếm test cũ.
