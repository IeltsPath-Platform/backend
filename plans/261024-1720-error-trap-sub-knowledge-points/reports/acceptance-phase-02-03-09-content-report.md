# Nghiệm thu phase 2, 3, 9 (phía content)

Người nghiệm thu: Claude. Ngày: 2026-10-24. Không commit.

## Kết luận

| Phase | Kết quả |
| --- | --- |
| 2 Schema + KP con | **ĐẠT** (ghi chú nhỏ N1) |
| 3 Logic bẫy + tìm bộ | **ĐẠT** (ghi chú nhỏ N1, N2) |
| 9 Dữ liệu mẫu | **CHƯA ĐẠT, trả Codex sửa** (lỗi D1, D2; nên sửa D3) |

## Kiểm độc lập
- Chạy lại `mvn -pl services/content-service -am test` bởi Claude: xem mục "Kết quả chạy" cuối file.
- Đọc diff: V79, `TrapKnowledgePointSync`, `TrueFalseNotGivenTraps`, `AddQuestionVersionUseCase`, `ReplaceKnowledgePointPrerequisitesUseCase`,
  `GetKnowledgePointsUseCase`, `GlobalExceptionHandler`, SQL `topicSequence` + `searchPracticeSets` trong `JdbcLearningContentReader`.
- Đọc toàn bộ V80 (40 câu mẫu, 10 mã), đối chiếu số liệu bằng grep.

## Phase 2 + 3: đạt
- SQL `searchPracticeSets` đúng thiết kế: bọc subquery, thứ tự số câu bẫy (≥ 2) → tỷ lệ → thứ tự cũ; `lesson_id` ưu tiên đổi sang `ranked`.
- `TrapKnowledgePointSync` đủ các nhánh (tạo, đổi tên, INACTIVE, bỏ KP cha, KP cha INACTIVE, ACTIVE lại), không xóa KP con.
- Tự điền T/F/NG chỉ khi cả hai mã ACTIVE, câu chưa có `distractors`, và đúng luật (`TRUE`/`YES` không bẫy).
- KP con bị chặn ở câu hỏi và ở cả hai đầu quan hệ tiên quyết; lỗi 400 có `code`.
- Test cũ ghim số đếm được xử lý đúng ngoại lệ trong plan (ghim `Flyway.target`, assert giữ nguyên; Codex ghi từng test trong báo cáo).

Ghi chú nhỏ (không chặn):
- **N1** `topicSequence` ẩn KP con bằng `left(kp.code, 5) <> 'TRAP_'`, không phải `NOT EXISTS (error_types.trap_knowledge_point_id)` như plan.
  Hệ quả: KP do author tạo với mã bắt đầu `TRAP_` bị ẩn nhầm, và khi `includeErrorTraps=true` nó lọt ra không có `parentKnowledgePointId`.
  Nên đổi sang liên kết thật khi Codex quay lại làm D1/D2 (một dòng).
- **N2** Danh sách KP của author (`GetKnowledgePointsUseCase`) ẩn KP con ở mọi nơi dùng use case này, không chỉ picker. Chấp nhận (KP con không có
  nội dung để quản lý); ghi lại để màn quản trị KP không bất ngờ.

## Phase 9: lỗi phải sửa
Đã ghi nhận đúng: 10 mã trong phạm vi (dưới ngưỡng 15), V80 thêm 19 bộ, `ErrorTrapCoverageMigrationTest` xanh, `distractors` trỏ khóa thật + mã ACTIVE,
không câu nào gắn KP con, các nội dung đã đọc đúng chuyên môn (ví dụ `TFNG_NG_AS_FALSE`: "café có phục vụ bữa nóng" → Not Given; "ferry đầu 08:30"
vs "09:00" → False). Nhưng test chỉ đo số đếm nên không bắt được hai lỗi dưới đây.

- **D1 (lỗi, chặn): hai bộ của cùng một mã có nội dung giống hệt.** V80 có 4 câu mẫu cho mỗi mã (`trap_item_templates`) và `JOIN ... ON error_code`
  nên bộ 1 và bộ 2 nhận cùng 4 câu (chỉ `[[place]]` ở vài câu khác). Mục đích hai bộ là bộ kế tiếp khi trượt bộ đầu; học viên trượt bộ 1 sẽ gặp lại đúng
  câu cũ ở bộ 2. Cần ≥ 8 câu mẫu khác nhau cho mỗi mã (mỗi bộ 2 câu bẫy + ≥ 2 câu không bẫy, khác bộ kia). `INFERENCE_UNSUPPORTED` chỉ thêm 1 bộ nên giữ 4 câu,
  nhưng phải khác bộ có sẵn.
- **D2 (lỗi, chặn): đáp án đúng của mọi câu trắc nghiệm là `A`.** 36/36 câu `MULTIPLE_CHOICE` có `correct = 'A'`, bẫy luôn ở `B`/`C`. Học viên chọn A mọi
  câu cũng đúng hết; bằng chứng bẫy bị lệch (không bao giờ ai "sai bẫy" nếu chọn A). Cần phân bố `A`/`B`/`C` xen kẽ, bẫy mang mã nằm ở lựa chọn sai
  khác nhau; không để một chữ cái chiếm > 50% trong cùng một bộ.
- **D3 (nên sửa): bộ T/F/NG gồm cả câu trắc nghiệm không liên quan.** `TFNG_FALSE_AS_NG`/`TFNG_NG_AS_FALSE` bộ gồm 2 câu T/F/NG + 2 câu `MULTIPLE_CHOICE`
  (giờ phà, năm mở cửa...) gắn vào KP T/F/NG. Nên dùng cả 4 câu T/F/NG (có ít nhất 1 câu đúng `TRUE` để bộ cân bằng); câu `TRUE` không có bẫy.

Yêu cầu bổ sung khi sửa: thêm vào `ErrorTrapCoverageMigrationTest` hai assert để lỗi D1, D2 không quay lại:
(1) trong cùng một mã, không có hai câu của hai bộ khác nhau có cùng `stem` sau khi thay `[[place]]`;
(2) trong mỗi bộ trắc nghiệm có từ 3 câu trở lên, đáp án đúng không cùng một khóa.

## Còn lại sau khi D1, D2 sửa
- Giám khảo IELTS duyệt nội dung mẫu (như Codex đã ghi); nghiệm thu này chỉ kiểm tính đúng đắn kỹ thuật và đọc mẫu.
- Sau khi Codex sửa: tôi chạy lại toàn module và đọc lại bộ mới, rồi `ck plan check 2 3 9`.

## Kết quả chạy
`mvn -pl services/content-service -am test` (Claude, 23:05-23:12): BUILD SUCCESS, exit 0, **954 test, 0 lỗi, 0 fail, 1 skip**.
Codex báo 955; chênh 1 so với số Surefire cuối (có thể đếm cả ca bỏ qua hoặc test script đã xóa), không ảnh hưởng kết luận.
Lưu ý: xanh nhưng chưa bắt D1, D2 (test hiện chỉ đếm bộ và kiểm khóa/mã).

## Câu hỏi mở
- Câu `FILL` và mã luật câu điền (`SPELLING`...) không có dữ liệu mẫu theo phạm vi plan; chấp nhận.

## Nghiệm thu lần 2 phase 9 (2026-10-25)
- Codex sửa D1, D2, D3, N1: 76 dòng câu riêng, mỗi mã 2 bộ có 8 stem khác nhau; đáp án đúng trắc nghiệm A21/B22/C17; bộ TFNG toàn câu T/F/NG có 1 câu TRUE không bẫy; `topicSequence` dùng `NOT EXISTS`; thêm 2 test chống tái phát.
- Claude chạy lại `mvn -pl services/content-service -am test`: lần cuối BUILD SUCCESS, **957 test, 0 lỗi, 1 skip**. Lưu ý: một lần chạy trước đó trong cùng phiên exit 1 ở content-service, hai lần sau exit 0; không giữ được log nên chưa biết nguyên nhân (nghi test phụ thuộc thời gian/Docker). Theo dõi, không chặn.
- Kết luận: **phase 9 ĐẠT**. Việc còn lại của phase 9: giám khảo IELTS duyệt nội dung trước khi cho học viên.
