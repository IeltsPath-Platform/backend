---
title: "Gamification: XP, cấp độ, tim, points, streak, nhiệm vụ ngày, huy hiệu, league"
description: "Lớp tạo động lực theo tài liệu mentor (01 §5, 03 §B.3, §C.5, §C.6, §E.3–E.5): XP theo câu đúng và mastery, tim chặn bài luyện khi hết, gem gộp vào points của access-service, streak tính phía server có đóng băng, 3 nhiệm vụ ngày, huy hiệu, league tuần 30 người"
status: pending
priority: P2
branch: "feat/knowledge-tree-assessment-journey"
tags: [learning-service, access-service, user-service, gamification, xp, hearts, points, streak, league]
blockedBy: ["261017-1931-mastery-practice-and-progress"]
blocks: []
created: "2026-10-17T12:32:00.000Z"
createdBy: "ck:plan"
source: "bộ tài liệu mentor (01, 02, 03) + quyết định của người dùng 2026-10-17"
---

# Gamification

## Overview

Tài liệu mentor có một lớp gamification kiểu Duolingo tách khỏi mastery. IELTSPath hiện chỉ có streak do client tự gửi
(user-service) và points mua bằng activation key để trả phí chấm AI (access-service). Plan này thêm XP, cấp độ, tim,
thưởng points, streak phía server, nhiệm vụ ngày, huy hiệu và league.

```text
Nộp bài (lesson, Practice, luyện tự do, ôn)
   │ cùng transaction, khóa advisory theo user
   ├─ chấm ─> sai câu khách quan ─> trừ tim (lesson, Practice, luyện tự do) ─> hết tim: 403 HEARTS_DEPLETED
   ├─ đúng câu ─> XP (10/15/20 theo độ khó, trần 100/ngày) ─> cấp độ ─> lên cấp: hồi đầy tim
   ├─ ngày có hoạt động ─> streak server ─> mốc 7 ngày: XP + points
   ├─ tiến độ nhiệm vụ ngày ─> xong: XP (+ points)
   ├─ huy hiệu
   └─ XP tuần ─> league 30 người ─> CN 00:00 UTC: lên/xuống hạng
Points (access) ── kiếm: thưởng học (có trần/ngày) │ tiêu: hồi tim, đóng băng streak, chấm AI
```

## Quyết định đã chốt (người dùng, 2026-10-17)

- **G1. Tim áp đúng tài liệu.** 5 tim (gói PREMIUM 7). Sai 1 câu khách quan mất 1 tim ở: **bài tập trong lesson,
  Practice set của lesson, luyện tự do theo KP**. Không mất tim ở bài ôn, quick-check, thăm dò chẩn đoán, practice test,
  topic/course test, placement, essay. Hết tim thì **chặn**: chờ +1 tim mỗi 5 giờ hoặc trả points để hồi đầy. Người dùng
  chấp nhận rằng hết tim có thể làm học viên tạm không làm được Practice bắt buộc.
- **G2. Gem gộp vào points** của access-service. Một loại tiền duy nhất: kiếm thêm từ học, tiêu cho hồi tim, đóng băng
  streak và chấm AI (như hiện tại). Không mua "bỏ qua bài", "làm lại", "mở premium" bằng points (phá cổng khóa và gói
  PREMIUM).
- **G3. Giữ XP + cấp độ, nhiệm vụ ngày, huy hiệu, league tuần.**
- **G4. Streak thưởng do server tính** từ lần nộp bài thật. API streak cũ của user-service (`/api/learning-support/streak`)
  giữ nguyên, không đổi contract; FE chuyển dần sang streak server.
- **G5. Không thêm service, không thêm role.** XP, tim, streak server, nhiệm vụ, huy hiệu, league ở learning-service
  (nơi có bài nộp, mastery và khóa theo user). Points ở access-service. user-service chỉ mở endpoint nội bộ lấy tên hiển
  thị cho bảng xếp hạng.
- **G6. Chống cày:** XP chỉ tính ở lần nộp đầu của mỗi khối/set (cùng luật bằng chứng, AGENTS.md §3.8); trần 100 XP/ngày
  từ câu trả lời (phần vượt ghi `shadow`, không tính, 03 §C.5); trần points kiếm được mỗi ngày (mặc định 20) vì points
  trả được phí LLM.

## Phases

| # | Phase | Service | Status | Phụ thuộc |
|---|---|---|---|---|
| 1 | [Points: cộng thưởng và loại giao dịch mới](phase-01-access-points-credit.md) | access | completed | – |
| 2 | [XP, cấp độ, cài đặt múi giờ](phase-02-xp-and-levels.md) | learning | completed (XP, cấp độ, múi giờ, khối `gamification` qua HTTP, ranh giới ngày theo múi giờ) | – |
| 3 | [Tim](phase-03-hearts.md) | learning, access | in_progress (gate, trừ tim, refill, PREMIUM 7, 403 qua HTTP, bài ôn khi hết tim đều có test; chỉ còn refund, chờ Codex thêm route nội bộ ở access) | 1, 2, plan 3 phase 3 |
| 4 | [Streak phía server, đóng băng, thưởng points](phase-04-server-streak-and-rewards.md) | learning, access | in_progress (streak, đóng băng, mốc 7/30 có test DB; relay points và refund chưa làm) | 1, 2 |
| 5 | [Nhiệm vụ ngày](phase-05-daily-quests.md) | learning | completed (V29, sinh lười, tiến độ theo câu mới, thưởng một lần, test HTTP controller) | 2, 4 |
| 6 | [Huy hiệu](phase-06-badges.md) | learning | completed (V30, 9 huy hiệu, cấp một lần, bật/tắt, test HTTP controller) | 2, 4 |
| 7 | [League tuần](phase-07-weekly-league.md) | learning, user | completed (V28, chốt tuần, bảng và lịch sử, test HTTP controller và adapter tên hiển thị; còn: ẩn danh) | 2 |
| 8 | [Contract, tài liệu, nghiệm thu](phase-08-contracts-and-acceptance.md) | docs | in_progress (contract learning, AGENTS.md, DATABASE_V5 xong; còn contract access/user, verify, demo) | tất cả |

## Câu hỏi mở

- **Kinh tế points:** points kiếm từ học dùng được để chấm AI (mỗi lần 3 points), nên mỗi học viên có thể dùng LLM nhiều
  hơn mà không trả tiền. Trần mặc định 20 points/ngày (tối đa ~6 lần chấm). Cần người dùng chốt con số.
- **Hạng league:** tài liệu ghi "5 leagues" nhưng liệt kê 7 (Bronze, Silver, Gold, Sapphire, Diamond, Ruby, Amethyst) và
  kèm khoảng XP mâu thuẫn với luật lên/xuống hạng. Plan dùng 7 hạng và luật lên/xuống hạng, bỏ khoảng XP.
- **Ẩn danh trên bảng xếp hạng** (03 Part J #8): mặc định hiện tên hiển thị; có thể thêm tùy chọn ẩn danh sau.
- **Mốc cấp độ:** tài liệu có hai cách (mốc 100/500/1000/5000 ở 01 §6.3, "mỗi 1000 XP" ở 03 §C.6). Plan dùng mỗi
  1000 XP một cấp, cấu hình được.
- Số migration là tạm.
