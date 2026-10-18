---
phase: 7
title: "League tuần"
status: completed
priority: P3
dependencies: [2]
---

# Phase 7: League tuần

## Overview

01 §5.3, 03 §E.5: nhóm 30 người cùng hạng, xếp theo XP tuần, chốt Chủ nhật 00:00 UTC: top 7 lên hạng, hạng 23–30 xuống
hạng, giữa giữ nguyên. 7 hạng: Bronze, Silver, Gold, Sapphire, Diamond, Ruby, Amethyst (xem câu hỏi mở của plan).

## Requirements

- **Bảng:**
  - `league_weeks(week_start PK, closed_at NULL)`;
  - `league_groups(id, week_start, tier, created_at)`;
  - `league_members(week_start, user_id, group_id, tier, weekly_xp, final_rank NULL, outcome NULL
    (PROMOTED|STAYED|DEMOTED), PRIMARY KEY(week_start, user_id))`, index `(group_id, weekly_xp DESC)`;
  - `learner_league(user_id PK, tier)` (hạng hiện tại, mặc định Bronze).
- **Vào nhóm:** lần đầu có XP tính (counted) trong tuần → vào nhóm đang mở cùng hạng có < 30 người (khóa hàng nhóm khi
  thêm), không có thì tạo nhóm mới. Ai không có XP trong tuần không vào league tuần đó.
- **Cộng XP tuần** trong `GamificationRecorder` (chỉ XP `counted`).
- **Chốt tuần** `LeagueCloseJob` (Chủ nhật 00:00 UTC, khóa advisory, idempotent theo `league_weeks.closed_at`):
  - xếp hạng trong nhóm theo `weekly_xp` giảm dần, hòa thì ai đạt trước xếp trên;
  - top 7 lên một hạng (Amethyst giữ); hạng 23–30 xuống một hạng (Bronze giữ); nhóm < 23 người thì không ai xuống;
  - một câu UPDATE theo nhóm/tập, không vòng gọi repository từng người.
- [x] **Tên hiển thị (Codex):** user-service mở `POST /internal/users/display-names` (body danh sách id, tối đa 50, trả `id`,
  `displayName`; không trả email). Learning gọi một lần mỗi lần xem bảng (cùng client internal ở plan 3 phase 4).
  (test: user endpoint suite 9 tests, 0 failures/errors/skips; PostgreSQL Testcontainers ran.)
- **API:**
  - `GET /api/learning/me/league`: hạng, nhóm tuần này, danh sách 30 người (tên, XP tuần, thứ hạng), vùng lên/xuống, thời
    điểm chốt;
  - `GET /api/learning/me/league/history?weeks=8`: kết quả các tuần trước.

## Related Code Files

- Tạo: migration, `L/domain/service/LeagueRules.java`, repository + adapter, `L/infrastructure/scheduler/LeagueCloseJob.java`,
  use case; user-service internal controller + use case
- Sửa: `GamificationRecorder`, `GamificationController`
- Test: `LeagueRulesTest` (top 7, 23–30, nhóm nhỏ, hạng đầu/cuối, hòa điểm), integration (31 người vào 2 nhóm; job chạy hai
  lần không chốt lại; vào nhóm song song không vượt 30), user-service `@WebMvcTest` (chỉ internal token, không lộ email)

## Success Criteria

- [x] Nhóm không bao giờ quá 30 người. (test: `LeagueIntegrationTest.learnersJoiningAtTheSameTimeNeverOverfillAGroup` 40 luồng song song; `thirtyOneLearnersFillOneGroupAndOpenASecond`)
- [x] Sau chốt tuần, top 7 lên hạng, 23–30 xuống hạng đúng luật. (test: `closingAWeekPromotesTheTopSevenAndDemotesRanksTwentyThreeAndBelow`, `LeagueRulesTest` 13)
- [x] Bảng xếp hạng chỉ có tên hiển thị, không có email. (bảng chỉ có hạng, tên, XP tuần, cờ `you`, vùng; không có user id người khác. Test: `theTableShowsDisplayNamesAndZonesAndAsksForNamesOnce`)

## Tiến độ (2026-10-17)

- Xong: V28 (`league_weeks`, `league_groups`, `league_members`, `learner_league`), `LeagueRules`, `LeagueService`
  (vào nhóm khi có XP tính đầu tuần, khóa advisory theo tuần và hạng), `LeagueCloseJob` (Chủ nhật 00:00 UTC, một câu SQL xếp
  hạng cả tuần), `DisplayNameClient` + `RestDisplayNameClient` (một lần gọi cho cả bảng), `GetLeagueUseCase`,
  `LeagueController` (`GET /api/learning/me/league`, `/history?weeks=1..26`), cộng XP tuần trong `GamificationRecorder`.
- Quyết định: hạng lưu số 1..7; tuần league chạy Chủ nhật đến Chủ nhật theo UTC; hòa XP thì người đạt mức đó trước xếp trên
  (`xp_updated_at`); nhóm dưới 23 người không ai xuống hạng, nhưng top 7 vẫn lên (nhóm 7 người trở xuống thì tất cả lên).
  Người dùng chọn giữ đúng tài liệu, không thêm ngưỡng nhóm tối thiểu (2026-10-17).
- Chỉ XP `counted` vào league: XP vượt trần 100/ngày (shadow) không vào. Test: 12 câu khó earn 240, vào league 100.
- Test HTTP `LeagueController` (`GamificationBoardsControllerTest`) và test adapter `RestDisplayNameClient` (`RestUserServiceClientsTest`) đã có.
- Chưa làm: ẩn danh (câu hỏi mở của plan).

## Risk Assessment

- Lộ tên học viên cho người lạ: mặc định tên hiển thị, có thể thêm ẩn danh sau (câu hỏi mở).

## Bàn giao FE

Tab League: huy hiệu hạng, danh sách 30 người, vùng xanh/đỏ, đếm ngược tới lúc chốt.
