---
title: "Cập nhật DB cho chức năng Examiner chấm bài"
description: "Thêm cột lưu lỗi đánh dấu trong bài và bản nháp khi chấm, mở khóa danh mục mã lỗi Writing để Examiner chọn từ dropdown. Không thêm bảng mới."
status: pending
priority: P1
branch: "feat/ielts-manual-grading"
tags: [assessment, content, database, examiner, writing]
blockedBy: []
blocks: []
created: "2026-10-22T01:35:00.000Z"
createdBy: "ck:plan"
source: conversation
---

# Cập nhật DB cho chức năng Examiner chấm bài

Trạng thái: **V15, V16 và code backend của đường review độc lập đã xong và đã chạy test; V59, V60 chờ** (V59 chờ người duyệt, V60 chờ
sửa code Content). Phạm vi ban đầu là migration; code kèm theo ghi ở mục "Đã làm kèm". Wireframe là plan riêng.

## Mục tiêu

Màn chấm Writing của Examiner (wireframe `P-61`) cần lưu được: band và nhận xét từng tiêu chí, overall band, nhận xét
tổng thể, **lỗi đánh dấu trong bài** (đoạn trích, mã lỗi chọn từ danh mục, gợi ý sửa) và **bản nháp**. Đối chiếu với DB hiện
tại cho thấy phần lớn đã có, thiếu hai chỗ lưu và một điều kiện dữ liệu.

## Hiện trạng đã kiểm tra

| Cần lưu | Có sẵn? | Nơi |
|---|---|---|
| Band 4 tiêu chí + nhận xét từng tiêu chí | Có | `human_review_submissions.criteria` (JSONB), `item_results.feedback_snapshot` |
| Overall band, nhận xét tổng thể | Có | `band`, `feedback` / `assessment_results.overall_band` |
| Loại lỗi (mã) | Có, chỉ là danh sách mã | `error_codes VARCHAR(60)[]`, `error_analysis_items.error_type` |
| Danh mục mã lỗi cho dropdown | Có bảng, **nhưng đang trống với assessment** | `error_types` (content-service): mọi mã đang `DRAFT`, endpoint nội bộ chỉ trả `ACTIVE` + `APPROVED` |
| Đoạn trích, vị trí, gợi ý sửa của lỗi | **Chưa** | cần `annotations` |
| Bản nháp khi đang chấm | **Chưa** (review độc lập gửi một lần) | cần `draft_payload` |
| Nháp của bài thi thử, test chương/khóa | Có | `assessment_results` version `DRAFT` |

## Quyết định đã chốt

- **Không thêm bảng.** Hai cột JSONB đủ: lỗi và nháp chỉ được đọc lại cùng bài, không truy vấn theo từng phần tử.
- **Mã lỗi lấy từ `error_types`**, không để Examiner gõ tự do. Nhóm (`category`) dùng để lọc dropdown; tiêu chí suy ra từ nhóm
  (GRAMMAR→GRA, VOCABULARY→LR, COHERENCE→CC, TASK→TA/TR), **không lưu**.
- **Một lỗi** lưu dạng `{"excerpt","start","end","errorCode","suggestion"}`, cùng tên trường với `corrections` mà AI chấm đã lưu
  trong `grading_jobs.llm_feedback`, để màn kết quả học viên vẽ cả hai bằng một đoạn code.
- `suggestion` do Examiner gõ (cách sửa cụ thể). `fix_hint` của mã là lời khuyên chung, chỉ hiện làm hướng dẫn, không điền sẵn.
- Bài thi thử và test chương/khóa **không cần migration**: đưa mảng lỗi vào `feedback_snapshot`.

## Phases

| # | Phase | Service | Migration | Trạng thái |
|---|---|---|---|---|
| 1 | [Cột `annotations`](phase-01-annotations-column.md) | assessment | `V15__human_review_annotations.sql` | **file đã tạo, đã thử** |
| 2 | [Cột nháp `draft_payload`](phase-02-draft-columns.md) | assessment | `V16__human_review_draft.sql` | **file đã tạo, đã thử** |
| 3 | [Duyệt mã lỗi Writing](phase-03-approve-writing-error-codes.md) | content | `drafts/V59__approve_writing_error_codes.sql` | bản nháp đã thử, **chưa vào thư mục migration: chờ người duyệt** |
| 3b | Speaking: rubric và nhóm lỗi | content | `drafts/V60__speaking_rubric_and_error_groups.sql` | bản nháp đã thử, **chưa vào thư mục migration: phải đi cùng thay đổi code** |
| 4 | [Kiểm chứng và triển khai](phase-04-verify-and-rollout.md) | cả hai | không | chờ phase 1–3 |

Thứ tự: 1 và 2 độc lập nhau, làm song song được. Phase 3 độc lập về kỹ thuật nhưng **không merge** khi chưa có người duyệt.
Phase 4 chạy sau cùng.

## Rủi ro chính

| Rủi ro | Cách xử lý |
|---|---|
| Duyệt mã lỗi (phase 3) là thay đổi **học viên nhìn thấy**: AI chấm bắt đầu trả `corrections` có mã, báo cáo học viên hiện mã | Chỉ merge sau khi người hiểu IELTS xác nhận từng mã; ghi tên người duyệt vào mô tả PR |
| Cột nháp có ràng buộc "đã COMPLETED thì không còn nháp" | Code chốt bài phải xóa nháp **trong cùng câu UPDATE** đặt `COMPLETED`, nếu không DB từ chối (cố ý, làm lưới an toàn) |
| Trùng số migration với nhánh khác (`V16` ở assessment, `V59` ở content) | Kiểm tra `git fetch` và các nhánh đang mở trước khi đặt tên file |
| Sửa migration đã áp dụng | V15 **chưa commit, chưa chạy ở môi trường nào ngoài DB thử**, nên sửa chú thích được. Sau khi merge thì không sửa nữa |

## Đã làm kèm: mã lỗi chưa duyệt vẫn dùng được (cách B)

Quyết định: `review_status` chỉ ngăn **học viên** thấy tên mã và `fix_hint`, không ngăn Examiner ghi mã. Nhờ vậy duyệt mã (V59)
có thể làm sau mà tính năng vẫn chạy.

| Thay đổi | Nơi |
|---|---|
| Endpoint nội bộ `GET /internal/learning-content/error-types/staff`: mọi mã `ACTIVE` kèm `reviewStatus` | content-service |
| `ErrorTypeCatalog.staffCodes()`: Examiner ghi được mã `ACTIVE` dù còn DRAFT; `activeCodes()` (chỉ APPROVED) vẫn là tập học viên được thấy | assessment-service |
| `HumanReviewService`: kiểm mã trong `errorCodes` và `annotations`, kiểm đoạn trích khớp bài, nhận và trả `annotations`, bản nháp | assessment-service |
| Màn học viên đọc review: bỏ mã còn DRAFT khỏi `errorCodes` và từng lỗi (giữ `excerpt`, `suggestion`); Content không liên lạc được thì không hiện mã nào | assessment-service |
| API `PUT/GET /api/assessments/grading-jobs/{jobId}/human-review/draft`; chốt bài xóa nháp cùng câu UPDATE đặt `COMPLETED` | assessment-service |
| Nhận lại một claim hết hạn thì xóa nháp của người trước (cùng người nhận lại thì giữ) | assessment-service |
| Tài liệu `human-review-v1.md`, `learning-content-internal-v1.md` | docs |

Phạm vi đường **review độc lập** (bài luyện tập, bài học). Đường bài thi thử và test chương/khóa vẫn chỉ nhận mã APPROVED, vì
mã chảy sang báo cáo học viên ở learning-service; chưa xác nhận ở đó mã DRAFT có bị lộ không.

Đã chạy (Maven offline, Testcontainers PostgreSQL, Flyway V1 đến V16 trên DB thật): `HumanReviewFlowIntegrationTest` (8),
`InternalHumanReviewControllerTest` (10), `SaveAssessmentResultDetailsUseCaseTest` (13), `InternalLearningContentControllerTest` (16).

## Ngoài phạm vi (plan riêng)

1. **Code backend (đã làm phần review độc lập, xem mục trên; còn lại: mở Speaking, đổi mã lỗi trong các đường khác):** `CompleteHumanReviewRequest` nhận `annotations`; `JdbcHumanReviewStore.complete` ghi `annotations` và xóa nháp;
   `HumanReviewResponse` trả `annotations`; API lưu nháp và đọc nháp; `HumanReviewService` kiểm `errorCode` có trong danh mục,
   `start < end` trong độ dài bài, tối đa 200 lỗi; `ContentErrorTypeClient` giữ `category`, `severity`; sửa ví dụ
   `COHESION_DEVICE_OVERUSE` trong `docs/contracts/human-review-v1.md` (mã không có trong danh mục).
2. **Wireframe:** popover thêm lỗi dùng dropdown mã lỗi, bỏ hàng chip tiêu chí; sửa `P-63`, `P-65` còn mục dựa trên knowledge point.
3. **Speaking, phần code:** DB của `assessment-service` đã đủ (đã thử chuỗi nộp, chấm, kết quả Speaking, xem phase 3b). Còn thiếu ở code: link nghe cho Examiner,
   tạo việc chấm cho Speaking của bài thi thử, API xem bài trả `audioReference`. Ở content-service: `Rubric`, `ManageRubricsUseCase`,
   `JdbcRubricRepository` (tiêu chí khi kích hoạt) và enum `ErrorCategory` chỉ biết Task 1, Task 2 và 7 nhóm lỗi cũ. Bản nháp V60 chỉ được vào thư mục migration
   **cùng PR** sửa các chỗ đó, vì các dòng V60 thêm sẽ làm `GET /api/content/rubrics` và `GET /api/content/error-types` lỗi.

## Câu hỏi chưa giải quyết

1. Ai là người duyệt 12 mã Writing, và họ có muốn thêm mã mới (dạng từ, dấu câu, lặp từ) trước khi duyệt không?
2. Giữ nút "Lưu nháp" cho review độc lập không? Nếu bỏ thì bỏ phase 2.
3. Speaking có làm trong đợt này không, và chỉ bài thi thử hay cả bài luyện tập?
4. Mapping nhóm lỗi → tiêu chí (GRAMMAR→GRA…) là suy luận theo thông lệ IELTS, cần xác nhận. `SPELLING_NUMBER` có thuộc LR không?
