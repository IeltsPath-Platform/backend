---
phase: 2
title: "Validate lại plan con"
status: completed
priority: P1
dependencies: [1]
effort: "1 ngày (4 phiên validate)"
---

# Phase 2: Validate lại plan con

## Overview

Chạy `/ck:plan validate` cho 0737, 0812, 0851, 1006 theo thứ tự merge, để mỗi plan khớp với 1640 sau Validation Session 1 và
với các plan vào trước. Danh sách dưới đây là các điểm đã biết từ scout
(`plans/reports/scout-260930-1642-lesson-plans-vs-codebase-report.md`) và tư vấn kiến trúc; mỗi phiên validate xác nhận hoặc
chốt lại với người dùng.

## Requirements

- Không plan con nào còn mâu thuẫn với 1640: bằng chứng lần đầu, `SKIPPED`, trạng thái topic suy ra, hiển thị MVP.
- Mỗi plan con có Validation Log và Whole-Plan Consistency Sweep với 0 mâu thuẫn còn lại.

## Implementation Steps

1. **0737 Writing Task 2**
   - **Đã chốt (Validation Session 1 của lộ trình):** bằng chứng Writing giữ luật của 0737 (ghi mọi lần nộp tới khi khối đạt).
     Ghi rõ trong 0737 đây là ngoại lệ có lý do so với luật "lần nộp đầu" của bài tập tự chấm: mỗi lần nộp là bài viết mới.
   - **Đã chốt:** refund **không** mở cho service gọi. Chỉ chuyển debit (và entitlement nếu cần) sang `/internal/access/**`;
     refund chỉ cho `ADMIN` qua `@PreAuthorize`, hoặc xóa route nếu grep xác nhận không ai gọi (hiện chỉ test gọi).
     Sửa quyết định "chuyển 4 endpoint" của 0737 tương ứng.
   - Debit song song cùng key → vi phạm UNIQUE thành 500; replay khác `amount`/`referenceId` không bị chặn. Đề xuất: bắt
     UNIQUE → trả lại giao dịch cũ; replay khác nội dung → 409.
   - `GetUserEntitlementUseCase.execute(UUID)` không có command (plan nói có).
   - Grader: compose đặt `AI_LEARNING_LLM_REASONING_EFFORT=low` nên `supports_temperature` là false và temperature 0 không được
     gửi. Chốt: chấp nhận, hay grader tự đặt `reasoning_effort`.
   - `AuthenticatedUser` không giữ token: router phải tự lấy bearer để forward cho access.
   - Nhãn tài liệu `V5.5` (phase 1).
2. **0812 Writing Task 1**
   - Viết hàm kiểm media dùng chung (quy ước ở phase 1); mã lỗi `INVALID_MEDIA_REFERENCE` thay `INVALID_LESSON_BLOCK` cho lỗi media.
   - Load asset theo lô (`ContentAssetLinkJpaRepository` hiện chỉ tìm theo từng version).
   - SVG data URI: contract ghi frontend chỉ render qua `<img>`.
   - `ContentAssetController` chưa có `@PreAuthorize`: phase 2 của 1640 khóa ghi; GET mở tới khi 0851 khóa. `chartFacts` nằm
     trong `answerSpec`, không nằm trong asset, nên chấp nhận khoảng hở này.
3. **0851 Listening**
   - Bỏ mọi chỗ nhắc luật chọn gói theo mức (`phase-04:51`); gói luyện không gắn `difficulty`; rút số gói và số mp3 seed
     (không còn "8 gói theo mức").
   - `knowledge_points.kind` NOT NULL, CHECK: chọn `kind` cho `LS_NUM`, `LS_SPELL`, `LS_PARA`, `LS_TRAP` (đề xuất `STRATEGY`).
   - Dạng `section_snapshot` do phase 4 của 1640 định nghĩa; 0851 phase 3 thêm `audio`, `solution` vào đúng dạng đó.
   - DTO học viên: model riêng không có `transcript` (không `model_validate` thẳng payload content với `extra="forbid"` → 500).
   - `ContentAssetRepository` cần hàm load theo lô.
   - Mở rộng hàm kiểm media của 0812 cho key + `CONTENT_MEDIA_BASE_URL`.
   - Bằng chứng: lần nộp đầu (theo 1640).
4. **1006 Gợi ý Reading**
   - **Đã chốt:** bỏ phần `hints_used` (1640 chỉ ghi bằng chứng ở lần nộp đầu, gợi ý chỉ mở sau lần sai đầu, nên không lần nộp
     nào vừa có bằng chứng vừa có gợi ý). Sửa phase 2 bước 5, test `test_practice_evidence.py`, phase 3 §7.5, mô tả plan.md.
   - Câu "làm lại **đúng các câu đó**" lệch 1640 ("nộp lại cả khối, cùng câu"): sửa theo 1640; gợi ý hiện cho câu đã từng sai.
   - Số migration `V12`, nhãn `V5.4` (phase 1).

<!-- Updated: Validation Session 1 - chốt bằng chứng Writing, refund, hints_used -->

## Success Criteria

- [x] 4 plan con có Validation Log mới; Whole-Plan Consistency Sweep 0 mâu thuẫn.
- [x] Không plan con nào còn nhắc 0908 như điều kiện, `V<n>`, `status` lưu ở `topic_progress`, hay luật bằng chứng cũ.

## Kết quả (2026-10-01)

| Plan | Session | Chốt |
| --- | --- | --- |
| 0737 | Session 2 | Chỉ debit sang `/internal/access`; refund, consume, entitlement chỉ ADMIN; debit song song cùng key trả ledger cũ, khác nội dung → 409; không gửi temperature khi có reasoning effort; bằng chứng Writing mọi lần tới khi đạt (ngoại lệ có lý do) |
| 0812 | Session 1 | `mediaUrl` cho học viên, content resolve; `MediaReferencePolicy` + `INVALID_MEDIA_REFERENCE`; load asset theo tập |
| 0851 | Session 1 | 1 gói mỗi KP, 8 mp3; mở rộng `MediaReferencePolicy` cho AUDIO; `section_snapshot` của 1640 + `audio`/`solution`, đổi sang DTO allowlist; DTO riêng không có `transcript` |
| 1006 | Session 3 | Bỏ `hints_used`; nộp lại cả khối; query tập câu đã mở là query riêng; file phase 2 đổi tên |

1640 cũng sửa thêm: `section_snapshot` (phase 4), payload section (phase 3).

## Risk Assessment

- **Validate lần lượt lâu:** các plan con độc lập về quyết định; có thể validate 0812, 0851, 1006 song song sau 0737, miễn
  không sửa cùng file.
- **Người dùng đổi quyết định ở plan con:** cập nhật bảng phạm vi và số migration trong `plan.md` của plan này.
