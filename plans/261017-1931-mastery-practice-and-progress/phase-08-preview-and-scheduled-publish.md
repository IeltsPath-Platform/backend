---
phase: 8
title: "Xem trước và xuất bản hẹn giờ"
status: pending
priority: P3
dependencies: []
---

# Phase 8: Xem trước và xuất bản hẹn giờ

## Overview

US-102: xem trước nội dung trước khi xuất bản. US-104: hẹn giờ xuất bản. Content đã có version `DRAFT` / `PUBLISHED` /
`ARCHIVED` và endpoint publish, nhưng không có xem trước dưới góc học viên, không có hẹn giờ.

## Requirements

- **Xem trước** `GET /api/content/admin/packages/{id}/versions/{versionId}/preview` (`ADMIN`, `CONTENT_AUTHOR`):
  - trả đúng hình dạng mà học viên nhận (bỏ `answerSpec`, `chartFacts`) cộng một khối `answers` riêng để người soạn đối
    chiếu;
  - xem được version `DRAFT`; không ghi gì, không tạo attempt.
- **Hẹn giờ:**
  - migration content: thêm `publish_at TIMESTAMPTZ NULL` vào bảng version gói (và version câu hỏi nếu publish riêng);
  - `PUT .../versions/{versionId}/schedule` body `{ "publishAt": "..." }` (phải ở tương lai, version phải `DRAFT` và qua
    kiểm tra publish hiện có); `DELETE .../schedule` để hủy;
  - `ScheduledPublishJob` (mỗi phút, khóa advisory) gọi đúng use case publish đang có cho version đến hạn; lỗi thì giữ
    `DRAFT` và ghi `last_publish_error` (mã lỗi, không ghi nội dung) để màn quản trị hiện;
  - outbox của content chưa có relay (AGENTS.md §7): publish hẹn giờ không làm thay đổi gì ở phần đó.
- Không đổi luật publish hiện có; hẹn giờ chỉ là gọi publish muộn.

## Related Code Files

- Tạo: use case preview, schedule, job; migration
- Sửa: controller admin gói, use case publish (tách hàm kiểm tra để schedule dùng lại)
- Test: preview không lộ `answerSpec` ngoài khối `answers`; schedule quá khứ → 400; job publish đúng một lần khi chạy
  song song (khóa); job lỗi giữ `DRAFT`

## Success Criteria

- [ ] Người soạn xem được bản nháp đúng như học viên sẽ thấy.
- [ ] Version hẹn giờ tự xuất bản trong vòng 1 phút sau `publishAt`.

## Risk Assessment

- Content không có hàng việc: lỗi hẹn giờ chỉ hiện ở màn quản trị, người soạn phải tự xem.

## Bàn giao FE

Nút "Xem trước" và "Hẹn giờ xuất bản" ở màn version; danh sách bản đã hẹn và bản lỗi.
