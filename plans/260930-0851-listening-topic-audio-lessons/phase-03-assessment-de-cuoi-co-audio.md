---
phase: 3
title: "Assessment: đề cuối có audio"
status: pending
priority: P1
dependencies: [2]
effort: "1 ngày"
---

# Phase 3: Assessment: đề cuối có audio

## Context Links

- Plan 1640 phase 4 (lấy package version từ content nội bộ, tự chấm, result cho học viên có `solutions` khi ≥ 70%)
- `services/assessment-service/src/main/java/com/group01/assessment/api/dto/response/AttemptStructureResponse.java:3`
  (trả `snapshot` của section **nguyên chuỗi** cho học viên)
- `application/usecase/StartAssessmentAttemptUseCase.java:47` (tạo `AttemptSection` với snapshot)

## Overview

Đề cuối Listening chạy qua assessment như đề Reading. Việc mới:
- lưu audio vào snapshot của section;
- trả section cho học viên theo allowlist để transcript không lộ;
- trả transcript trong `solutions` khi ≥ 70%.

Không migration (`section_snapshot` là jsonb).

## Requirements

- **Tạo attempt:** từ payload package version (phase 2), mỗi `attempt_sections.section_snapshot` có dạng
  ```json
  {"title": "...", "skill": "LISTENING", "instructions": "...",
   "audio": {"url": "https://...", "durationSeconds": 95},
   "solution": {"transcript": "..."}}
  ```
  Section Reading giữ dạng của 1640 phase 4 (`{title, skill, instructions, passage}`), không có `solution`.
- **Cấu trúc attempt cho học viên** (`AttemptStructureResponse`): thay `String snapshot` bằng DTO allowlist
  `{title, skill, instructions, passage?, audio?}`. Không bao giờ trả `solution`.
  - Đây là đổi response schema: kiểm các bên đọc (grep `AttemptStructureResponse`, test controller).
  - 1640 giữ `snapshot` là chuỗi JSON (không có `solution`); plan này đổi sang DTO allowlist vì snapshot bắt đầu chứa
    `solution`. Ghi đổi schema vào contract (phase 1).
- **Kết quả cho học viên** (`LearnerAssessmentResultResponse` của 1640): khi `percent ≥ 70`, `solutions` thêm
  `sections[{attemptSectionId, transcript}]` cho section có `solution.transcript`.
- DTO của người chấm (`GradingController`) không đổi.
- Tự chấm, `attemptType`, outbox, event: không đổi; câu Listening là `CHOICE`/`FILL` như Reading.

## Related Code Files

- Modify (assessment): `application/usecase/StartAssessmentAttemptUseCase.java` (dựng snapshot), `api/dto/response/
  AttemptStructureResponse.java` (allowlist), DTO kết quả học viên của 1640 (`solutions.sections`), mapper nếu có
- Tests: test use case tạo attempt, `@WebMvcTest` cấu trúc attempt và kết quả, test Testcontainers luồng nộp đề

## Implementation Steps

**Tests Before:**
1. `mvn -q -pl services/assessment-service -am test` và lưu baseline: đề Reading tạo attempt, nộp, tự chấm, event như cũ.

**Tests After** (viết trước code):
2. Tạo attempt từ package version Listening (content giả) → snapshot có `audio.url` và `solution.transcript`.
3. `GET` cấu trúc attempt:
   - JSON không chứa khóa `solution` hay `transcript` ở bất kỳ độ sâu nào;
   - có `audio.url`, `audio.durationSeconds`;
   - section Reading vẫn có `passage`.
4. Nộp đề Listening:
   - < 70% → không có `solutions`;
   - ≥ 70% → `solutions.sections[].transcript` đúng section.
5. Event `AssessmentCompleted.v2` của đề Listening giống đề Reading (không có transcript).

**Implement:** dựng snapshot → allowlist cấu trúc → `solutions.sections`.

**Regression Gate:**
```powershell
mvn -q -pl services/assessment-service -am test
```

## Success Criteria

- [ ] Transcript không lộ ở cấu trúc attempt, dù trong chuỗi hay object lồng nhau.
- [ ] Đề Reading không đổi hành vi.
- [ ] Không migration.

## Risk Assessment

- **Đổi schema response cấu trúc attempt** (từ chuỗi `snapshot` sang object): AGENTS cấm đổi âm thầm. Ghi vào contract
  `lesson-learning-v1.md` (phase 1). Repo chưa có frontend, nên chỉ cần sửa test.
- **Attempt cũ tạo trước khi có `skill`:** DTO coi thiếu `skill` là `READING`.

## Security Considerations

- `solution` là dữ liệu chỉ server đọc, giống `answer_snapshot`.
