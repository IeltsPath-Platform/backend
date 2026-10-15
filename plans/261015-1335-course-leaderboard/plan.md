---
title: "Course leaderboard: learning score, topic tests, final test"
description: "Ba bảng xếp hạng theo khóa học; điểm học tập cộng khi lesson hoàn thành đủ Practice."
status: pending
priority: P2
branch: "feat/lesson-resources"
tags: [feature, backend, frontend, database, api]
created: "2026-10-15"
---

# Bảng xếp hạng khóa học

Ngoài phạm vi: manifest JSONB, rules_version, bảng kết quả thi riêng, xử lý regrade, backfill dry-run/checkpoint.

## Quyết định đã chốt với người dùng (2026-10-15)

| Chủ đề | Quyết định |
| --- | --- |
| Phạm vi | Đủ 3 bảng: Học tập, Thi chặng (chọn topic), Thi cuối khóa |
| Điểm lesson | Tối đa 100; điểm khóa = **tổng** điểm lesson; chỉ cộng khi lesson hoàn thành đủ Practice bắt buộc |
| Lần nộp practice | Lần nộp Practice **đầu tiên** của mỗi bộ bắt buộc; bộ đã lộ đáp án (không counted) = 0 đúng; làm lại/bài ôn không nâng điểm; trượt <70% vẫn có điểm |
| Điểm thi | Lần thi **đầu tiên** cho mỗi chặng/khóa, kể cả trượt; chấm lại không đổi điểm bảng |
| Đồng điểm | Cùng hạng (`RANK()`: 1, 2, 2, 4) |
| Tên hiển thị | `displayName` hồ sơ → nếu chưa có hồ sơ dùng `fullName` tài khoản; hồ sơ `PRIVATE` → "Học viên ẩn danh" |
| Dữ liệu cũ | Backfill đơn giản trong migration; lesson thiếu dữ liệu thì bỏ qua |
| Giao diện | Tab `Lộ trình | Bảng xếp hạng` trên trang khóa học |

## Phases

| # | Phase | Status |
| --- | --- | --- |
| 1 | [Điểm lesson khi hoàn thành + backfill](./phase-01-lesson-score.md) | Pending |
| 2 | [API bảng xếp hạng + tên học viên](./phase-02-leaderboard-api.md) | Pending |
| 3 | [Tab bảng xếp hạng ở frontend](./phase-03-frontend-tab.md) | Pending |

Pha 2 cần bảng của pha 1; pha 3 cần API của pha 2. Bảng thi (pha 2) không phụ thuộc pha 1 nên làm song song được.

## Acceptance

- [ ] Lesson 2 bộ: bộ 1 đúng 1/2, bộ 2 đúng 9/10 → lesson 83,33 (theo tổng câu, không trung bình %).
- [ ] Hai lesson 100 + 80 → khóa 180, `2 bài`; người có một lesson 100 xếp dưới.
- [ ] Lần đầu 40%, làm bài ôn rồi hoàn thành → lesson vẫn 40.
- [ ] Mới xong 1/2 bộ hoặc còn bài ôn pending → chưa có điểm.
- [ ] Hoàn thành lại / replay / hai request song song → một dòng điểm (unique).
- [ ] Thi chặng lần đầu 55%, thi lại 85% → bảng thi chặng hiện 55.
- [ ] Đồng điểm cùng hạng; tôi ngoài top 10 vẫn thấy hạng đúng; tôi chưa có điểm → không hiện hạng giả.
- [ ] user-service lỗi → bảng vẫn hiện điểm/hạng, tên "Học viên ẩn danh".
- [ ] Đổi tab không mất `?topic=` và chặng đang mở.

## Rủi ro chấp nhận

- Tổng điểm thưởng cho số bài đã học (người học nhiều bài điểm thấp có thể trên người ít bài điểm cao) — người dùng đã chọn SUM; UI ghi rõ "tổng điểm các bài đã học".
- Backfill không biết danh sách bộ bắt buộc tại thời điểm hoàn thành cũ → dùng các bộ học viên đã nộp Practice trong lesson đó.
- Regrade bài thi không cập nhật bảng (giữ đúng hành vi hiện tại của gate).
