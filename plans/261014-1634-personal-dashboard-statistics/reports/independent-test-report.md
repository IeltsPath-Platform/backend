# Báo cáo kiểm thử độc lập — Dashboard thống kê cá nhân

- Thời điểm: 2026-10-15, 09:47–10:18 (Asia/Ho_Chi_Minh). Có hai vòng: vòng A lúc Docker chưa chạy, vòng B khi đã bật Docker.
- Người kiểm: agent độc lập. Chỉ đọc code, chạy lệnh và viết file này. Không sửa mã nguồn, không checkout/reset/clean, không commit/push, không đọc `.env`.
- Các chương trình kiểm tra tạm (`ParamCheck.java`, `RouteBands.java`, `RouteAnswers.java`) nằm trong scratchpad của phiên, ngoài cả hai repo.

## 1. Đối tượng kiểm thử

| Repo | Branch | HEAD | Trạng thái |
|---|---|---|---|
| BE `D:\Fall2026\New folder\BE\backend` | `docs/personal-dashboard-statistics-plan` | `8d6a351` (docs: plan personal dashboard statistics) | Tính năng là thay đổi chưa commit: 29 file sửa và nhiều file mới untracked. Giữ nguyên. |
| FE `D:\Fall2026\New folder\FE\frontend` | `main` | `0def395` | 12 file sửa và các file mới untracked. Giữ nguyên. |

Môi trường: Windows 11, Java 21.0.7, Maven 3.9.9, Node v24.12.0.

- Vòng A: Docker chưa chạy, nên mọi test Testcontainers bị skip.
- Vòng B: Docker Engine 29.4.3. Mọi test Testcontainers đều chạy, 0 skip.

## 2. Lệnh đã chạy và exit code

| # | Vòng | Lệnh | Exit | Kết quả |
|---|---|---|---|---|
| 1 | A | `mvn -B -ntp -pl services/user-service,services/content-service,services/learning-service,services/assessment-service,services/library-service -am test '-Ddebug=false'` | 1 | Learning lỗi do fixture baseline. Assessment bị SKIPPED theo thứ tự reactor |
| 2 | A | `mvn -B -ntp -pl services/assessment-service -am test '-Ddebug=false'` | 1 | Fixture baseline |
| 3 | A | `git diff --check` (BE và FE) | 0 / 0 | Sạch. Quét riêng file untracked chỉ khớp file nhị phân mp3/zip |
| 4 | A | `npm run lint` / `npm run typecheck` / `npm run build` | 0 / 0 / 0 | Pass. Vite cảnh báo chunk > 500 kB |
| 5 | A | `node --test src/features/study-time/activeIntervals.test.mjs` | 0 | 3 pass / 0 fail |
| 6 | A | `mvn help:effective-pom`, `javap -v`, scratch `DefaultParameterNameDiscoverer` + `MockMvcBuilders.standaloneSetup` trên class đã build | — | Bằng chứng cho P1-1 và P1-2 |
| 7 | B | `mvn -B -ntp -pl services/user-service,services/content-service,services/library-service,services/assessment-service -am test '-Ddebug=false'` | 1 | Dừng ở user-service vì 1 lỗi. Content/Library/Assessment bị SKIPPED |
| 8 | B | `mvn -B -ntp -pl services/learning-service -am test '-Ddebug=false'` | 1 | 1 failure của implementation và 1 error baseline |
| 9 | B | `mvn -B -ntp -fae -pl services/content-service,services/library-service,services/assessment-service -am test '-Ddebug=false'` | 1 | Library có 1 failure; Assessment có 1 error baseline |

### Số test Maven ở vòng B (có Docker; đây là số chính thức)

| Module | Run | Fail | Error | Skip | Lỗi |
|---|---|---|---|---|---|
| common-security | 8 | 0 | 0 | 0 | — |
| user-service | 131 | 0 | 1 | 0 | `StudyTimePersistenceTest.receiptAndTotalsRollbackTogether` (P1-3) |
| content-service | 255 | 0 | 0 | 0 | — |
| library-service | 69 | 1 | 0 | 0 | `VocabularyReviewPersistenceTest.addDedupeReplayArchiveAndDueSummaryUsePersistentState` (P1-3) |
| learning-service | 248 | 1 | 1 | 0 | `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` (P1-4); `AnswerSpecGraderTest` (B1) |
| assessment-service | 206 | 0 | 1 | 0 | `AnswerSpecGraderTest` (B1) |

Các test persistence mới đã chạy thật trên PostgreSQL 15 và pass, gồm `BandStatisticsPersistenceTest` (3), `LearningProgressIntegrationTest` (2), `CourseSequenceIntegrationTest` (8), các test còn lại của `StudyTimePersistenceTest` và `VocabularyReviewPersistenceTest`. Migration V6 (user), V11 (learning), V8 (assessment) và V3 (library) đều áp được.

## 3. Lỗi baseline (không do implementation)

**B1. `AnswerSpecGraderTest.gradesEverySharedContractVector`** lỗi ở Learning và Assessment: `IO Shared answer-spec vectors were not found.`

- `backend/docs/` không có trong HEAD.
- Commit `fee364e` (2026-10-12, "del capstone doc") đã xóa `docs/contracts/answer-spec-v1-vectors.json` và `answer-spec-v1.md`.
- Không tìm thấy file này ở đâu khác dưới `D:\Fall2026\New folder`.

Test được giữ nguyên, không làm yếu đi.

**B2. Build không bật `-parameters`.** Effective POM không có `<parameters>`, và `javap` không thấy attribute `MethodParameters`. Các controller cũ dùng `@PathVariable UUID id` không ghi tên có cùng rủi ro như P1-1 và P1-2: Library (`NoteController`, `FlashcardDeckController`, `FlashcardController`, `VideoProgressController`, `SavedVideoSegmentController`), Learning `CourseController`, User `LearningActivityController`, và các controller của Game. Với `NoteController`, `DefaultParameterNameDiscoverer` trả `null`. Tôi chưa gọi request thật vào các route cũ này.

## 4. Lỗi của implementation

### P1-1 — `GET /api/assessments/me/statistics/bands` luôn trả 400; card band không có dữ liệu

- Vị trí: `services/assessment-service/.../api/controller/BandStatisticsController.java:17-19`. Sáu `@RequestParam` không ghi tên.
- Bằng chứng: chạy standalone MockMvc trên class đã build, request hợp lệ nhận `IllegalArgumentException: Name for argument of type [java.time.LocalDate] not specified ... '-parameters' flag`. `GlobalExceptionHandler` map lỗi này thành 400.
- Test `invalidDateIsBadRequestInsteadOfInternalError` pass nhưng sai lý do: 400 đến từ lỗi tên tham số, không phải từ ngày sai. Không có test happy-path.

### P1-2 — `POST .../vocabulary-review-sessions/{id}/answers` luôn lỗi; không ôn từ được

- Vị trí: `services/library-service/.../api/controller/VocabularyReviewController.java:37`, `@PathVariable UUID id` không ghi tên.
- Bằng chứng: chạy standalone MockMvc nhận `IllegalArgumentException: Name for argument of type [java.util.UUID] not specified ...`.
- Hệ quả: state từ vựng không bao giờ rời `NEW`, nên card vocab không phản ánh việc ôn.

### P1-3 — Instant có độ chính xác nano giây, DB lưu micro giây: replay và kiểm thời gian sai lệch (test đỏ khi có Docker)

- **Library:** `VocabularyReviewPersistenceTest:46`. Lần trả lời đầu trả `nextReviewAt=…03:14:00.765810100Z` (từ bộ nhớ), còn replay đọc từ DB trả `…03:14:00.765810Z`. Như vậy replay không trả đúng snapshot ban đầu, vi phạm tiêu chí phase 2. Gốc: `VocabularyReviewUseCase` dùng `Instant.now()` không cắt độ chính xác.
- **User:** `StudyTimePersistenceTest.receiptAndTotalsRollbackTogether:61`: `Intervals must be ordered, recent and no later than server time`.
  - Session lưu `started_at` vào `timestamptz`, và pgjdbc làm tròn nano giây sang micro giây, có thể làm tròn lên.
  - `startedAt` đọc lại vì thế muộn hơn vài trăm nano giây, nên `Duration.toMillis()` cho 29999 ms.
  - Interval `[0,30000]` bị từ chối. Lỗi ngắt quãng (flaky) tùy phần nano của `Instant.now()`.
  - Ở production, request hợp lệ có thể bị 400 ở biên mili giây theo cùng cơ chế.
  - Gốc: `StudySession.start`/`accept` dùng thời điểm không cắt độ chính xác.

### P1-4 — Test khởi động Learning chưa cập nhật theo migration V11

`LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth:60`: `expected: <10> but was: <11>`. Test vẫn kiểm Flyway version `"10"` và 17 bảng, trong khi V11 thêm bảng `course_study_activity`. Cần cập nhật thành `"11"` và 18 bảng. Đây là cập nhật hợp lệ theo migration mới, không làm yếu test. CI (`mvn verify` có Docker) sẽ đỏ nếu không sửa.

### P2-1 — Bộ ghi thời gian bị khóa vĩnh viễn sau một lỗi 409/4xx (theo đọc code, chưa chạy browser)

- Vị trí: FE `src/features/study-time/StudyActivityTracker.tsx`. `fail()` đặt `blocked = true`, và từ đó `start()` luôn return sớm.
- Sau `SESSION_IN_USE`, `LEASE_EXPIRED` (ví dụ sleep > 90 s), `SESSION_CLOSED` hoặc lỗi 400, tracker ngừng ghi. Ngừng cả khi tab hiện lại, cho tới khi người học tự bấm nút. Phase 1 bước 7 yêu cầu reset sau lease expiry.
- Race khả dĩ khi chuyển qua lại hai tab cùng bài: tab A `close()` bất đồng bộ, còn tab B `start()` ngay. Nếu B tới server trước, nó nhận `SESSION_IN_USE` và bị khóa.
- Không gây đếm đôi, nhưng có thể đếm thiếu thời gian.

### P2-2 — Thiếu test happy-path cho route mới

Các test MVC mới chủ yếu kiểm 401/400, nên P1-1 và P1-2 lọt qua. Cần test 200 cho `/bands` (có tham số) và `/answers`.

### P3

1. Band lọc theo ngày UTC (`GetBandHistoryUseCase.execute`), còn FE gửi ngày local của trình duyệt. Bài nộp trước 07:00 giờ VN bị lệch ngày.
2. FE tính tuần/tháng theo đồng hồ trình duyệt; BE dùng timezone hồ sơ.
3. FE in band và delta dạng float thô, ví dụ band Writing placement 6.333… (`StatisticsCharts.tsx`, `OverviewDetailedStats.tsx`).
4. Assessment `StudyContext` luôn trả `courseId=null`, nên thời gian làm topic/course test không vào scope course.
5. `useStatisticsResource` reload cả 4 card mỗi lần cửa sổ focus, và dữ liệu chớp do quay về trạng thái "Đang tải…".
6. Hơn 100 khóa đã bắt đầu thì `/progress` trả 503 cho cả card.
7. Khi chạy user-service trên host, `user-service.yaml` mặc định URL Docker. `services/user-service/README.md:207` đã ghi, nhưng phần cạm bẫy trong `CLAUDE.md` chưa có.
8. `plan.md` có 2 dòng Phase 1; frontmatter `phase-01` ghi `completed` dù trạng thái chưa rõ.

## 5. Đối chiếu theo chủ đề

| Chủ đề | Kết luận | Ghi chú |
|---|---|---|
| Phân quyền theo user | Đạt | Query lọc `user_id` từ `CurrentUserProvider`; test MVC bỏ qua header `X-User-Id` |
| Rỗng / 0 / chưa đo | Đạt | `NOT_TRACKED`/`FUTURE`/0 tách bạch; `percent=null` khi total=0 |
| Bài nhiều kỹ năng | Đạt | Map theo lessonId; `LearningProgressIntegrationTest` pass trên DB |
| Pagination band / khóa | Band: logic và SQL pass trên DB, **route hỏng (P1-1)**. Khóa: phân trang trong bộ nhớ, giới hạn 100 | |
| Chấm lại | Đạt | `BandStatisticsPersistenceTest` pass trên DB |
| Eligibility Speaking/Writing placement | Đạt (theo code) | Speaking cần EXAMINER verified; Writing fallback gắn `FALLBACK`; Overall cần đủ 4 skill |
| Replay / sequence / lease | Đạt phần lớn; **P1-3** | Replay đồng thời pass trên DB; lỗi ở biên độ chính xác thời gian |
| Tab ẩn / idle / audio / ghi âm / logout | Logic có; **P2-1** | Chưa chạy browser |
| stateVersion khi ôn từ | Domain đạt; **P1-2, P1-3** | |
| Lỗi một service không hỏng card khác | Đạt | Mỗi card một resource riêng |
| Race đổi account/filter | Đạt | Key gồm email + filter + version |
| DTO thật ↔ type FE, route FE/Gateway | Khớp | `bandLevel` nullable ở BE nhưng FE khai `number` |
| Keyboard / mobile | Chưa kiểm thật | |
| Bỏ Mentor | Đạt | |

## 6. Kiểm thử thủ công

- **Đã làm:** chạy route thật bằng standalone MockMvc cho `/bands` và `/answers`; đọc code đối chiếu DTO và route.
- **Chưa làm:** browser/E2E, chạy cả stack, HTTP xuyên service, 2 tab, sleep, offline, logout, audio, keyboard/mobile.

## 7. Kết luận

**Chưa đạt nghiệm thu.**

- FE lint/typecheck/build/unit pass.
- Migration và SQL mới chạy được trên PostgreSQL.
- Có 4 nhóm lỗi P1 của implementation: hai route hỏng vì thiếu tên tham số, sai lệch độ chính xác thời gian làm replay và kiểm interval sai, và test Learning chưa cập nhật theo V11.
- Ngoài ra còn P2 ở tracker FE, và lỗi fixture baseline B1.

## 8. Prompt cho agent sửa lỗi

```text
Sửa các lỗi trong báo cáo kiểm thử độc lập:
D:\Fall2026\New folder\BE\backend\plans\261014-1634-personal-dashboard-statistics\reports\independent-test-report.md

Repo BE: D:\Fall2026\New folder\BE\backend (branch docs/personal-dashboard-statistics-plan)
Repo FE: D:\Fall2026\New folder\FE\frontend (branch main)
Đọc AGENTS.md, CLAUDE.md và báo cáo trên trước khi sửa. Workspace có thay đổi chưa commit của tính năng
và file người dùng có sẵn: giữ nguyên, không checkout/reset/clean, không commit/push, không đọc/hiển thị .env.
Docker đang chạy; test Testcontainers phải chạy thật, không skip.

Phạm vi bắt buộc:
1. P1-1: BandStatisticsController (assessment) ghi tên tường minh cho mọi @RequestParam/@PathVariable,
   theo cách StudySessionController đã làm. Thêm test MVC happy-path: GET /bands với from/to hợp lệ trả 200
   và gọi use case với đúng tham số. Sửa test invalidDate để chứng minh 400 đến từ ngày sai
   (ví dụ use case không bị gọi, còn request hợp lệ thì có).
2. P1-2: VocabularyReviewController (library) dùng @PathVariable("id"). Thêm test happy-path
   POST /vocabulary-review-sessions/{id}/answers trả 200 và truyền đúng sessionId.
   Rà mọi controller MỚI của tính năng (user, learning, content, assessment, library) để không còn tham số
   binding không tên. Không tự sửa controller cũ (B2) và không bật -parameters cho reactor;
   chỉ ghi lại đề xuất nếu thấy cần.
3. P1-3: chuẩn hóa thời điểm mà tính năng dùng và lưu xuống timestamptz, cắt về độ chính xác DB
   (ví dụ truncatedTo(ChronoUnit.MICROS) hoặc MILLIS) tại nguồn thời gian của use case/aggregate,
   để giá trị trả lần đầu và replay giống hệt nhau và kiểm interval không lệch ở biên mili giây:
   - library VocabularyReviewUseCase (Instant.now() ở study/start/answer/statistics)
   - user StartStudySessionUseCase/RecordStudyHeartbeatUseCase/StudySession (clock.instant())
   Không nới điều kiện kiểm interval hay assertion của test để test xanh. Bổ sung một unit test cho domain
   chứng minh started_at sau round-trip không làm interval hợp lệ bị từ chối.
4. P1-4: cập nhật LearningServiceApplicationTests theo migration V11: Flyway version "11" và số bảng tương ứng
   (đếm thực tế; dự kiến 18). Không xóa assertion.
5. P2-1 (FE StudyActivityTracker.tsx): sau LEASE_EXPIRED/SESSION_CLOSED, hoặc khi tab hiện lại, tự mở
   session mới (clientSessionId mới, takeover=false). Với SESSION_IN_USE, tự thử lại có giới hạn (backoff ngắn,
   tối đa vài lần) trước khi hiện nút "Ghi thời gian tại đây". Giữ: không đếm đôi, không gửi sequence mới khi
   payload cũ chưa ACK, telemetry lỗi không chặn làm bài. Cập nhật hoặc bổ sung node test nếu tách được hàm thuần.
6. P3 nên làm nếu nhỏ: FE làm tròn hiển thị band/delta 1 chữ số; type bandLevel nullable.
   Các P3 khác chỉ ghi vào báo cáo kết quả nếu không sửa.

Không làm: không sửa AnswerSpecGraderTest hay tạo fixture giả cho B1; không thêm dependency; không đổi
config/shared security/Gateway; không sửa migration đã có; không thêm comment chứa mã plan/finding.

Kiểm chứng (ghi lệnh, exit code, pass/fail/skip):
  Set-Location 'D:\Fall2026\New folder\BE\backend'
  mvn -B -ntp -fae -pl services/user-service,services/content-service,services/library-service,services/assessment-service,services/learning-service -am test '-Ddebug=false'
  git diff --check
  Set-Location 'D:\Fall2026\New folder\FE\frontend'
  npm run lint; npm run typecheck; npm run build
  node --test src/features/study-time/activeIntervals.test.mjs
Kỳ vọng: chỉ còn AnswerSpecGraderTest (B1) lỗi ở learning và assessment; 0 skip.

Ghi kết quả vào:
D:\Fall2026\New folder\BE\backend\plans\261014-1634-personal-dashboard-statistics\reports\fix-after-independent-test.md
(file đã sửa, lỗi đã sửa hoặc chưa sửa kèm lý do, kết quả lệnh, giới hạn còn lại như browser/E2E chưa chạy).
Không tuyên bố đã chạy browser/E2E nếu không làm thật.
```

## Câu hỏi còn mở

1. Bật `-parameters` toàn reactor để sửa luôn B2, hay chỉ ghi tên ở controller mới (prompt trên chọn cách này)?
2. Cắt thời điểm về MICROS hay MILLIS? MILLIS khớp với offset của client hơn.
3. Ngày cho band history tính theo timezone hồ sơ hay UTC?
4. Ai khôi phục `docs/contracts/answer-spec-v1-vectors.json`?
