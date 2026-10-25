# Nghiệm thu phase 1: baseline data và characterization tests

- Ngày: 2026-10-24. Người nghiệm thu: Claude.
- Kết luận: **ĐẠT CÓ GHI CHÚ**.

## Đã kiểm
- Báo cáo `codex-phase-01-baseline-data-report.md`, `baseline-data.md`: đủ bước 1–3 và 5–6; số liệu khớp đếm độc lập trước
  đó (≈ 21 câu đúng `NOT_GIVEN`).
- Diff: chỉ 2 file test, không đụng code sản phẩm hay migration (`git status`).
- `CourseSequenceIntegrationTest.practiceSetOrderPrefersLessonThenKnowledgePointMatchesThenPackageId`: khóa đúng thứ tự lesson →
  số câu khớp KP → mã bộ, có trường hợp hòa. Chạy lại độc lập:
  `mvn -q -pl services/content-service -am test -Dtest=CourseSequenceIntegrationTest` → 9 test, 0 lỗi, **0 skipped** (Docker thật).
- Baseline module do Codex chạy: content 921 test (1 skip có chủ đích `ImportTemplateFileTest`), learning 902 test, 0 lỗi.

## Ghi chú
1. **Điều kiện dừng là lỗi của plan, không phải của Codex.** "Không có bộ T/F/NG ≥ 2 câu bẫy → dừng" viết trước khi có phase 9;
   khoảng trống này chính là việc của phase 9. Đã bỏ điều kiện này khỏi `plan.md`.
2. **Bước 4 (mốc band/báo cáo) chưa làm.** Không cần nữa: test "tổng hợp không đổi" ở phase 4 so sánh cùng một hồ sơ có và
   không có KP con trong cùng test. Đã sửa phase 1 và phase 4.
3. **`ErrorTrapBaselineDataTest` là script đo** (`System.out`, khẳng định yếu), không nên ở lại bộ test: xóa ở phase 9 khi tạo
   `ErrorTrapCoverageMigrationTest`. Đã ghi vào phase 9.

## Ảnh hưởng tới các phase sau (từ số liệu)
- 19/28 mã ACTIVE có KP cha. Trong đó ngoài phạm vi phase 9: mã luật câu điền (`SPELLING`, `WORD_LIMIT_EXCEEDED`; `NUMBER_FORMAT`
  là mã luật câu điền dù có 1 câu `CHOICE`) và mã Writing chỉ có essay (`T1_*`, `UNDERDEVELOPED_IDEA`, có thể `COLLOCATION`,
  `WORD_CHOICE`). Ước tính 10–12 mã trong phạm vi (dưới ngưỡng dừng 15). Phase 9 xác định chính xác bằng truy vấn.
- 9 mã ACTIVE không có KP cha (ví dụ `CONTRADICTED_BY_PASSAGE` có 4 câu) → không có KP con. Gắn KP cha cho chúng là quyết định
  nội dung, ngoài plan; ghi để người dùng cân nhắc.
- Hiện chỉ `INFERENCE_UNSUPPORTED` có 1 bộ ≥ 2 câu bẫy: phase 9 phải bổ sung gần như toàn bộ.

## Câu hỏi còn mở
- Có gắn KP cha cho 9 mã đang thiếu (để chúng có KP con) không? Không chặn plan.
