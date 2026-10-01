---
phase: 3
title: "Assessment: đề cuối có audio"
status: completed
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

- [x] Transcript không lộ ở cấu trúc attempt, dù trong chuỗi hay object lồng nhau.
- [x] Đề Reading không đổi hành vi.
- [x] Không migration.

## Risk Assessment

- **Đổi schema response cấu trúc attempt** (từ chuỗi `snapshot` sang object): AGENTS cấm đổi âm thầm. Ghi vào contract
  `lesson-learning-v1.md` (phase 1). Repo chưa có frontend, nên chỉ cần sửa test.
- **Attempt cũ tạo trước khi có `skill`:** DTO coi thiếu `skill` là `READING`.

## Security Considerations

- `solution` là dữ liệu chỉ server đọc, giống `answer_snapshot`.

## Kết quả

Hoàn tất ngày 2026-10-02 trên nhánh `feat/lesson-listening-final-test`, tạo từ `feat/main-follow` tại `e94befc`
(đã merge tự chấm assessment). Không push.

### Đã làm

- Port và `ContentPackageClient` đọc audio `{assetId,mediaUrl,durationSeconds,transcript}`; từ chối thiếu asset ID,
  URL trống hoặc duration âm bằng lỗi Content hiện có. Duration/transcript nullable được giữ nguyên.
- `AttemptCreator.sectionSnapshot` đóng băng audio URL/duration và `solution.transcript`; section không có audio giữ
  snapshot Reading hiện có. Không lấy lại dữ liệu Content khi xem kết quả.
- `AttemptStructureResponse.snapshot` là object allowlist, chỉ có title/skill/instructions/passage/audio URL và duration.
  Không trả solution/transcript, kể cả metadata lồng nhau. JSON cũ thiếu skill mặc định READING; snapshot không parse
  được trả các field null thay vì lỗi 500.
- Kết quả thêm `sectionSolutions:[{attemptSectionId,transcript}]` riêng khi percent ≥ 70; cả hai khóa solution vắng
  dưới 70. `solutions[]` của item giữ nguyên; kết quả đạt mà không có transcript trả `sectionSolutions:[]`.
  Đọc các section bằng một query theo attempt sau khi kiểm ownership và ngưỡng.
- Contract và README assessment được cập nhật. Không đổi tự chấm, attemptType, outbox, event hoặc migration;
  `AssessmentCompleted.v2` không chứa transcript.

### Kiểm chứng

- Tests Before: `mvn -q -pl services/assessment-service -am test` → 110 pass, 0 fail/error, 0 skip.
- Tests After trước code: chạy `ContentPackageClientTest,AssessmentAnswerExposureWebMvcTest,AutoGradingIntegrationTest`
  → 25 test, 10 pass, 15 fail đúng hành vi thiếu, 0 error, 0 skip.
- Focused sau code: cùng ba lớp → 25 pass, 0 fail/error, 0 skip. Sau đó bổ sung test HTTP kết quả đạt 70% và kiểm
  mảng sectionSolutions rỗng của Reading; cả hai được kiểm chứng trong regression.
- Regression Gate: `mvn -q -pl services/assessment-service -am test` → **125 pass, 0 fail/error, 0 skip**
  (assessment 117; common-security 8). Docker hoạt động; Testcontainers PostgreSQL đã chạy, gồm luồng Listening
  60%/70%, snapshot audio/transcript, submit lặp chỉ một event và event không chứa transcript.
- WebMvc kiểm khóa cấm đệ quy cả object/array và JSON trong chuỗi; passage Reading và snapshot legacy vẫn hoạt động.
- Scout và code review độc lập không có finding. `git diff --check` pass; 19 khối JSON trong contract hợp lệ.
- `graphify update .` hoàn tất; các artifact graph/build không commit.

### Lệch phase gốc đã áp dụng theo yêu cầu

- Dựng snapshot ở `AttemptCreator`, không ở `StartAssessmentAttemptUseCase`, vì nền tự chấm đã tách writer.
- Dùng khóa riêng `sectionSolutions`, không đổi mảng item `solutions` thành `solutions.sections`.
- Không còn lệch yêu cầu; không cần quyết định bổ sung.

### File đổi

- `services/assessment-service/README.md`
- `services/assessment-service/src/main/java/com/group01/assessment/api/controller/AssessmentAttemptController.java`
- `services/assessment-service/src/main/java/com/group01/assessment/api/dto/response/AttemptStructureResponse.java`
- `services/assessment-service/src/main/java/com/group01/assessment/api/dto/response/LearnerAssessmentResultResponse.java`
- `services/assessment-service/src/main/java/com/group01/assessment/application/port/ContentPackageProvider.java`
- `services/assessment-service/src/main/java/com/group01/assessment/application/result/LearnerAssessmentResult.java`
- `services/assessment-service/src/main/java/com/group01/assessment/application/usecase/AttemptCreator.java`
- `services/assessment-service/src/main/java/com/group01/assessment/application/usecase/GetAssessmentResultUseCase.java`
- `services/assessment-service/src/main/java/com/group01/assessment/infrastructure/client/ContentPackageClient.java`
- `services/assessment-service/src/test/java/com/group01/assessment/api/controller/AssessmentAnswerExposureWebMvcTest.java`
- `services/assessment-service/src/test/java/com/group01/assessment/application/usecase/AutoGradingIntegrationTest.java`
- `services/assessment-service/src/test/java/com/group01/assessment/application/usecase/StartAssessmentAttemptUseCaseTest.java`
- `services/assessment-service/src/test/java/com/group01/assessment/infrastructure/client/ContentPackageClientTest.java`
- `docs/contracts/lesson-learning-v1.md`
- `plans/260930-0851-listening-topic-audio-lessons/phase-03-assessment-de-cuoi-co-audio.md`
- `plans/260930-0851-listening-topic-audio-lessons/plan.md`
