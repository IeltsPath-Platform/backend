---
title: "Mastery MP-1.0, luyện tự do theo KP, báo cáo tiến độ, ôn từ vựng và quản trị"
description: "Đưa phần lõi của bộ tài liệu mentor (01-architecture-overview, 02-dev-integration-spec, 03-ba-spec) vào IELTSPath: metadata KP, mastery MP-1.0, luyện tự do theo KP, báo cáo theo kỹ năng và band mục tiêu, bản chụp tuần/tháng, flashcard 5 hộp, cờ vắng học, thống kê và xuất bản hẹn giờ"
status: pending
priority: P1
branch: "feat/knowledge-tree-assessment-journey"
tags: [content-service, learning-service, library-service, user-service, mastery, practice, report, srs, admin]
blockedBy: ["261016-0131-knowledge-tree-assessment-journey"]
blocks: ["261017-1932-gamification"]
created: "2026-10-17T12:31:00.000Z"
createdBy: "ck:plan"
source: "bộ tài liệu mentor 'IELTS Practice App — Knowledge Graph Architecture' (3 file, 2025-10-16), bỏ qua tech stack"
---

# Mastery MP-1.0, luyện tự do, báo cáo tiến độ, ôn từ vựng và quản trị

## Overview

Plan 1 đã có cây KP, mastery 3 mức, giỏ ôn, chẩn đoán và khu quản trị. So với tài liệu mentor còn lệch:

- Mastery đơn giản hơn MP-1.0: không giảm theo thời gian, không trọng số theo nguồn, không đòi đủ bằng chứng.
- KP thiếu cấp CEFR, mô tả can-do; cây chưa có KP Grammar và Vocabulary.
- Học viên không tự chọn KP để luyện (US-010), chỉ luyện được KP đang nằm trong giỏ ôn.
- Báo cáo chưa gom theo kỹ năng, chưa so với band mục tiêu, chưa có bản chụp tuần/tháng.
- Flashcard chưa có lịch ôn ngắt quãng.
- Khu quản trị chưa có cờ vắng học, thống kê người dùng hoạt động, xem trước và xuất bản hẹn giờ.

Plan này chỉ làm các phần trên. Gamification (XP, tim, points, streak, nhiệm vụ, huy hiệu, league) ở plan
`261017-1932-gamification`.

```text
Evidence (lesson, Practice, ôn, luyện tự do, thi, essay)
   │  trọng số nguồn × giảm theo tuổi (half-life 30 ngày), cửa sổ 8, tối đa 3 bài luyện
   ▼
Mastery MP-1.0 ──> tier INSUFFICIENT │ PRIORITY │ CONSOLIDATE │ MAINTAIN (biên đệm 3 điểm)
   │
   ├──> Gợi ý, giỏ ôn, luyện tự do theo KP
   ├──> Báo cáo: theo KP, theo kỹ năng, band ước tính vs band mục tiêu
   └──> Bản chụp tuần (CN) ──> xu hướng 4 tuần, dự kiến ngày đạt mục tiêu, bản tháng
```

## Quyết định đã chốt

- **M1. Không có phụ huynh, không có lớp.** Không làm thống kê lớp, giao bài cho học viên, nhắn cả lớp (người dùng
  chốt 2026-10-17). Giáo viên dùng hồ sơ 360° và hàng việc có sẵn.
- **M2. Không thêm role, không thêm service** (giữ D11 của plan 1).
- **M3. Bỏ qua tech stack của tài liệu.** Chỉ lấy chức năng và luật nghiệp vụ; công nghệ theo AGENTS.md §2.
- **M4. Mastery theo MP-1.0** (03 §E.1): cửa sổ 8, half-life 30 ngày, tối thiểu 3 bằng chứng trên 2 ngày, biên đệm
  3 điểm, tối đa 3 bài luyện trong cửa sổ, trọng số theo nguồn, không tính lại cùng câu trong 7 ngày. Ngưỡng 60/80 giữ
  nguyên. `MasteryCalculator` (port DeepTutor) giữ lại cùng comment nguồn và test gốc, nhưng không còn là công thức mặc
  định; AGENTS.md §3.8 phải được cập nhật trong phase 2.
- **M5. Luyện tự do không được ăn hết kho bài ôn.** Gói đã nộp không bao giờ giao lại (AGENTS.md §3.8), nên luyện tự do
  chỉ được dùng một KP khi còn nhiều hơn `reserve` gói chưa lộ cho bài ôn (mặc định 2).
- **M6. Không gửi email, push.** notification-service chưa có; báo cáo tuần/tháng chỉ xem trong app. PDF không làm.
- **M7. Band ước tính chỉ là gợi ý,** không đổi trạng thái topic hay course (giống placement).

## Phases

| # | Phase | Service | Status | Phụ thuộc |
|---|---|---|---|---|
| 1 | [Metadata KP và cây đủ 6 miền](phase-01-kp-metadata-and-domains.md) | content | in_progress (CEFR, can-do, phạm vi đã commit `316f763`; còn seed Grammar/Vocab chờ duyệt và kiểm vòng cạnh) | – |
| 2 | [Mastery MP-1.0](phase-02-mastery-mp1.md) | learning | completed (công thức sau cờ `DEEPTUTOR`; chờ xác nhận bật `MP1`) | – |
| 3 | [Luyện tự do theo KP và cây của học viên](phase-03-free-practice-and-learner-tree.md) | learning, content | completed (luyện tự do, cây, lọc band/gói, CEFR; `practiceAvailable` của cây chỉ gần đúng, xem phase) | 2 |
| 4 | [Báo cáo theo kỹ năng và band mục tiêu](phase-04-skill-report-and-target-band.md) | learning, user | completed (còn: docs kiến trúc, test adapter HTTP) | 2 |
| 5 | [Bản chụp tuần, xu hướng, bản tháng](phase-05-weekly-monthly-snapshots.md) | learning | completed (V27, job, 3 endpoint, dự báo ngày đạt mục tiêu; còn: test cron thật, contract) | 4 |
| 6 | [Flashcard 5 hộp](phase-06-flashcard-leitner.md) | library | completed (commit `fec4c19`, test library xanh) | – |
| 7 | [Cờ vắng học và thống kê hoạt động](phase-07-inactivity-flag-and-activity-stats.md) | learning | completed (cờ INACTIVE, tự đóng, hàng việc EXAMINER, DAU/WAU/MAU và giữ chân có test) | – |
| 8 | [Xem trước và xuất bản hẹn giờ](phase-08-preview-and-scheduled-publish.md) | content | completed (commit `66351e9`, 22 test; còn: chạy lại scheduler test sau thay đổi annotation cuối) | – |
| 9 | [Contract, tài liệu, nghiệm thu](phase-09-contracts-and-acceptance.md) | docs | in_progress (contract learning, AGENTS.md, DATABASE_V5 xong; còn contract của Codex, verify, demo) | tất cả |

Song song được: 1, 2, 6, 7, 8. Sau 2 thì 3 và 4; 5 sau 4.

## Ngoài phạm vi

- Phụ huynh, lớp, thống kê lớp, giao bài, nhắn cả lớp (M1).
- Dạng bài mới: nối cặp, sắp xếp câu, chép chính tả, nghe-gõ; chấm phát âm Speaking.
- Email/push/thông báo realtime, PDF báo cáo (M6).
- Cổng thanh toán, dùng thử, hoàn tiền mua gói; xuất dữ liệu và tự xóa tài khoản.
- Tự thêm từ vựng của bài học vào flashcard (US-050): cần nối lesson với library, để plan sau.

## Câu hỏi mở

- Thay công thức mastery mặc định (M4) đổi tier của học viên đang học. Cần người dùng xác nhận trước khi bật cờ
  `learning.mastery.policy=MP1` ở môi trường thật (phase 2 mặc định để `DEEPTUTOR` cho tới khi xác nhận).
- Speaking chưa có KP. Band tổng tạm lấy band Speaking của placement nếu có, không có thì tính trên 3 kỹ năng (phase 4).
- KP Grammar/Vocabulary phải do người hiểu IELTS soạn và duyệt (D14 plan 1: nội dung tự viết, không chép Cambridge).
- Số migration là tạm, lấy số kế tiếp lúc code.
