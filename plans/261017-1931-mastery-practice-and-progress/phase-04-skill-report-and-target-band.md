---
phase: 4
title: "Báo cáo theo kỹ năng và band mục tiêu"
status: in_progress
priority: P1
dependencies: [2]
---

# Phase 4: Báo cáo theo kỹ năng và band mục tiêu

## Overview

`GET /api/learning/report` trả theo từng KP (điểm, lỗi, bằng chứng, việc nên làm). Tài liệu mentor muốn thêm:

- mastery gom theo kỹ năng (03 US-063, biểu đồ 6 cột);
- top 3 KP cần tập trung (US-064);
- band hiện tại ước tính so với band mục tiêu (US-062, 03 §C.3).

Band mục tiêu nằm ở learning goal của user-service, learning-service chưa đọc.

## Requirements

- [x] **user-service (Codex):** endpoint nội bộ `GET /internal/users/{id}/learning-goal` trả `targetBand`, `examDate` (null nếu
  chưa đặt). Chỉ gọi bằng internal JWT; thêm vào `internal-jwt-paths` nếu Gateway cần (chỉ service gọi nhau, không mở ra
  ngoài). (test: user endpoint suite 9 tests, 0 failures/errors/skips; PostgreSQL Testcontainers ran.)
- **learning-service:** `application/port/LearnerGoalClient` + adapter HTTP, kèm bearer và `X-Correlation-Id`. Lỗi hoặc
  timeout thì báo cáo vẫn trả, `target = null`.
- **Khối `skills` trong báo cáo** (thêm trường, không đổi trường cũ):
  - mỗi skill (LISTENING, READING, WRITING, SPEAKING) và mỗi `kind` nền (GRAMMAR, VOCABULARY):
    `mastery` (trung bình có trọng số theo số bằng chứng, bỏ KP `INSUFFICIENT`), `tierCounts`, `kpCount`;
  - `focus`: 3 KP mastery thấp nhất trong `PRIORITY`/`CONSOLIDATE`, ưu tiên KP có bài ôn chặn.
- **Band ước tính** (`estimatedBand`):
  - band của skill = band cao nhất mà ≥ 80% KP của skill ở band đó đạt `MAINTAIN` (03 §E.7); chưa đạt band nào thì lấy
    band placement của skill; không có placement thì `null`;
  - band tổng = trung bình 4 kỹ năng, làm tròn 0.5 (03 §E.2); Speaking chưa có KP nên lấy band placement Speaking, không
    có thì tính trên 3 kỹ năng và trả `speakingIncluded = false`;
  - Grammar/Vocabulary không vào band (03 §C.3);
  - "gần lên band" (03 §E.7): ≥ 80% KP ở band kế tiếp đạt ≥ `CONSOLIDATE` thì trả `nextBandHint` kèm tối đa 5 KP cần
    làm.
- **So với mục tiêu:** `target { targetBand, gap, examDate }`. Dự kiến ngày đạt tính ở phase 5 (cần bản chụp tuần).
- Không đổi trạng thái topic hay course (M7).

## Related Code Files

- Tạo: user-service internal controller + use case đọc goal; `L/application/port/LearnerGoalClient.java`, adapter;
  `L/domain/service/BandEstimator.java`
- Sửa: `GetLearnerReportUseCase`, `LearnerReportResult`, response DTO
- Test: `BandEstimatorTest` (80% KP MAINTAIN; thiếu Speaking; làm tròn 0.5; dùng placement khi chưa đạt band nào),
  use case (goal client lỗi → `target = null`), `@WebMvcTest` user-service (chỉ internal token)

## Implementation Steps

1. **Đỏ trước:** `BandEstimatorTest`.
2. Endpoint user-service, client learning.
3. Gom kỹ năng, `focus`, band, target.
4. `mvn -q -pl services/user-service -am test`, `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Báo cáo có 6 khối kỹ năng, top 3 KP, band ước tính và khoảng cách tới mục tiêu.
- [ ] user-service tắt thì báo cáo vẫn 200, chỉ thiếu `target`.
- [ ] Không có gọi HTTP trong vòng lặp.

## Risk Assessment

- Thêm cuộc gọi learning → user: cập nhật danh sách gọi đồng bộ ở `docs/system-architecture.md` và AGENTS.md §3.5.
- Band ước tính dễ bị hiểu là band thật: response ghi `estimated`, FE ghi "ước tính".

## Bàn giao FE

Dashboard: thanh 0 → band hiện tại → mục tiêu → 9.0; biểu đồ 6 kỹ năng; danh sách "Tập trung tuần này" có nút Luyện.
