---
phase: 6
title: "Giỏ ôn nhiều mức và recheck"
status: in_progress
priority: P1
dependencies: [5, "plan 261009"]
---

# Phase 6: Giỏ ôn nhiều mức và recheck

## Trạng thái và ghi chú triển khai

- **Đã xong (6a):** bài ôn không chặn, giỏ ôn và recheck. Code và test xong, **chưa commit**. Contract:
  `docs/contracts/lesson-learning-v1.md` (mục `GET /review-basket`, `POST /review-basket/{id}/start`, trường `blocking`).
- **Chưa làm (6b): `GET /api/learning/suggestions`** (gợi ý bài từ kho đã duyệt). Cần endpoint nội bộ mới của content
  để tìm `PRACTICE_TEST` và `LESSON` theo KP và band; content đang do Codex giữ. Làm sau khi chốt ai làm phần content.
- Khác với plan:
  - Migration là `V15__review_tiers.sql` (V13 là mastery profile, V14 là prompt_version của phase 11).
  - `blocking` nằm trong `PendingReview`; lọc chặn tập trung ở `LessonAccessGate.blocking`, nên mọi nơi chặn
    (`LessonAccess`, `PracticeAccess`, `AssignTopicTestUseCase`, `GetTopicLessonsUseCase`) tự bỏ qua bài ôn tự chọn.
    `PracticeClearance` chỉ xét bài ôn `PRACTICE` (luôn chặn) nên không đổi.
  - Bài ôn tự chọn nâng thành chặn ở cả hai luồng: kết quả thi (`ReviewReevaluation`) và Practice
    (`SubmitPracticeAttemptUseCase`), qua `ReviewItemRepository.promoteToBlocking`.
  - Practice test thuộc mốc không sinh bài ôn nào; chỉ practice test tự do sinh bài ôn tự chọn (`FREE_PRACTICE_TEST`).
  - Thêm `KnowledgePointDirectory` dùng chung cho báo cáo và giỏ ôn (code, tên, skill, band từ Content).
  - `POST .../start` từ chối KP ở mức ưu tiên hoặc duy trì chưa đến hạn (`409 NOT_IN_REVIEW_BASKET`), và KP chưa có
    lesson đã hoàn thành dạy nó (`409 NO_LESSON_FOR_KNOWLEDGE_POINT`; `review_items.lesson_id` bắt buộc).

## Overview

D5: giữ bài ôn chặn như plan 261009, thêm "giỏ ôn" không chặn cho mức củng cố (60–79%) và mức duy trì khi đến hạn ôn.
Theo board, phần này "optional, không trừ Process Score" và kết quả chỉ dùng cho mastery. Recheck là một set ngắn trong
giỏ; làm xong thì cập nhật mastery và hạn ôn.

## Requirements

- **Migration `V13__review_tiers.sql`:**
  - `ALTER TABLE review_items ADD COLUMN blocking BOOLEAN NOT NULL DEFAULT TRUE;`
  - Mở rộng CHECK của `trigger_kind`: thêm `'CONSOLIDATE'`, `'MAINTAIN'`, `'FREE_PRACTICE_TEST'`, `'DIAGNOSTIC'`
    (giá trị cuối dùng ở phase 7).
  - Unique index `uq_review_items_pending_kp` giữ nguyên: mỗi KP chỉ có một bài ôn `PENDING`. Nếu KP đang có bài ôn chặn
    thì không tạo thêm bài ôn tùy chọn.
- **Chặn chỉ khi `blocking = true`:** sửa `LessonAccessGate.blocking`, `PracticeAccess`, `AssignTopicTestUseCase` và
  `PracticeClearance` (plan 261009). Bài ôn không chặn không bao giờ làm khóa lesson, Practice hay topic test.
- **Bài ôn sinh từ practice test tự do** (`OFFICIAL_PRACTICE` không thuộc mốc của phase 8): tạo với `blocking = false`
  và `trigger_kind = 'FREE_PRACTICE_TEST'`. Board: phòng luyện thi là tùy chọn.
- **Giỏ ôn:**
  - `GET /api/learning/review-basket` trả 3 nhóm:
    - `required`: bài ôn chặn đang `PENDING`;
    - `consolidate`: KP mức `CONSOLIDATE`;
    - `due`: KP mức `MAINTAIN` có `next_review_at <= now`.
    Mỗi mục kèm KP, mức, lý do, và có còn gói chưa lộ hay không.
  - `POST /api/learning/review-basket/{knowledgePointId}/start`:
    - tạo bài ôn `blocking = false`, `trigger_kind` là `CONSOLIDATE` hoặc `MAINTAIN`, rồi giao một set chưa lộ;
    - hết gói → `409 NO_UNREVEALED_SET`, kèm tín hiệu cho R&D ở phase 7 và 12;
    - KP đang có bài ôn `PENDING` → trả lại bài ôn đó.
- **Recheck:**
  - Bài ôn không chặn chỉ có một set (`recordSetResult` đạt hay trượt đều kết thúc: `DONE` hoặc `SKIPPED`), không qua bước
    theory. Đây là quy tắc riêng cho `blocking = false`, viết trong `ReviewItem`.
  - Kết quả ghi evidence `review_set` như hiện nay, profile cập nhật qua phase 5.
- **Gợi ý bài phù hợp từ kho đã duyệt** (đầu ra "Gợi ý bài" của board):
  - `GET /api/learning/suggestions`: tối đa 5 gợi ý cho học viên, mỗi gợi ý có `type` (`PRACTICE_SET`, `PRACTICE_TEST`,
    `LESSON`), mục tiêu KP, lý do (`PRIORITY`, `CONSOLIDATE`, `DUE`, `NEXT_IN_TREE`) và đường dẫn mở.
  - Luật: ưu tiên KP mức `CONSOLIDATE` và `DUE` còn gói chưa lộ; KP mức `MAINTAIN` vững thì gợi ý KP mà nó là nền
    (cạnh tiên quyết ngược, phase 1); chỉ gợi ý thứ `PUBLISHED`, chưa làm, đúng band course học viên đang học hoặc thấp
    hơn. Gợi ý không bao giờ chặn tiến độ.
  - Learning gọi content qua API nội bộ tìm gói theo KP; thêm endpoint tìm `PRACTICE_TEST` theo KP nếu chưa có.
  - Test: `SuggestionRulesTest` (thứ tự ưu tiên, không gợi ý thứ đã làm, không vượt band).
- **Không tính vào Process Score:** phase 9 chỉ lấy Practice và practice test theo mốc. Ghi rõ trong contract.

## Related Code Files

(`L` = learning package)

- Sửa: `L/domain/aggregate/ReviewItem.java` (thêm `blocking`, luật một set), `L/domain/service/LessonAccessGate.java`,
  `L/application/service/PracticeAccess.java`, `L/application/usecase/AssignTopicTestUseCase.java`,
  `L/application/usecase/ApplyAssessmentResultUseCase.java` (phân biệt practice test tự do),
  `L/infrastructure/persistence/JdbcReviewItemRepository.java`, `L/application/usecase/ListReviewsUseCase.java`
- Tạo: `L/application/usecase/GetReviewBasketUseCase.java`, `StartBasketReviewUseCase.java`,
  `L/api/controller/ReviewBasketController.java`
- Test:
  - `ReviewItemTest`: không chặn, 1 set, trượt → `SKIPPED`;
  - `LessonAccessGateTest`: bài ôn không chặn không khóa;
  - `ReviewBasketIntegrationTest`: đủ 3 nhóm; start khi đang có bài ôn chặn trả lại bài ôn chặn; hết gói → 409.

## Implementation Steps

1. Xác nhận plan 261009 đã merge. Rebase.
2. **Đỏ trước:** test chặn / không chặn và test giỏ ôn.
3. Migration, aggregate, repository, cổng chặn.
4. API giỏ ôn và recheck.
5. Cập nhật `docs/contracts/lesson-learning-v1.md` (field `blocking` trong danh sách bài ôn, API giỏ ôn).
6. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] KP 65% không chặn gì, xuất hiện ở nhóm `consolidate`.
- [ ] KP 85% đến hạn ôn xuất hiện ở nhóm `due`. Làm đạt → hạn ôn lùi theo lịch.
- [ ] Practice test tự do làm sai không khóa lesson.

## Risk Assessment

- Ràng buộc một bài ôn `PENDING` mỗi KP: nếu học viên mở bài ôn tùy chọn rồi KP đó bị sai trong luồng chính, bài ôn
  tùy chọn phải được nâng thành chặn. Luật: `ReviewReevaluation` gặp bài ôn `PENDING` không chặn → set `blocking = true`
  và đổi `trigger_kind` sang nguồn hiện tại. Viết test cho trường hợp này.

## Bàn giao FE

- Màn giỏ ôn 3 nhóm, nút "Ôn ngay".
- Danh sách bài ôn hiện nhãn "Bắt buộc" hoặc "Tự chọn".
