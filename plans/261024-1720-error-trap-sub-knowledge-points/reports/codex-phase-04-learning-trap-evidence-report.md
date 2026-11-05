# Báo cáo phase 4 — Learning trap evidence

Ngày: 2026-10-25  
Trạng thái: DONE — tự kiểm Success Criteria đạt.

## Quy trình và kiểm chứng

- Baseline: `mvn -q -pl services/learning-service -am test` — 902 tests, 0 failures, 0 errors, 0 skips. Surefire phát cảnh báo phải kết thúc fork 30 giây sau `System.exit(0)` do listener/scheduler còn chạy; Maven trả exit 0.
- Đỏ trước khi hoàn tất implementation: test mới cho `TrapEvidenceRule`, trọng số MP-1.0 và tích hợp mastery dùng các API/class mới của phase; tập trung kiểm tra ánh xạ mã lỗi, đọc mặc định không gồm KP con và đọc chuyên biệt gồm KP con. Trong quá trình tích hợp, bộ test tập trung phát hiện URI cũ bị thêm query `includeErrorTraps=false`, badge query bỏ profile chưa có catalog, và test ứng dụng ghim Flyway ở V35; cả ba đều đã được sửa theo hợp đồng mới.
- Test cũ cần cập nhật theo migration mới: `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` đổi phiên bản kỳ vọng từ `35` sang `36`; số bảng giữ nguyên. Đây là test ghim phiên bản migration, không phải số lượng seed.
- Test tập trung sau sửa: `RestLearningContentClientTest`, `BadgeIntegrationTest`, `LearningServiceApplicationTests`, `TrapEvidenceRuleTest`, `ErrorTrapMp1ParamsTest`, `MasteryProfileIntegrationTest` — exit 0.
- Lệnh toàn module cuối: `mvn -q -pl services/learning-service -am test` — exit 0; 122 suites, 908 tests, 0 failures, 0 errors, 0 skips. Testcontainers xác nhận Docker Desktop/PostgreSQL khả dụng. Surefire lại in cảnh báo kết thúc fork sau 30 giây vì listener/scheduler nền; không ảnh hưởng exit code hay báo cáo test.
- Ngoại lệ ghim số đếm: không ghim/đổi assertion đếm dữ liệu. Chỉ cập nhật phiên bản Flyway như liệt kê trên.

## Thay đổi

- Tạo migration mới `V36__error_trap_evidence.sql`: thêm `parent_kp_id`, `error_code`; cho phép nguồn `error_trap` và trigger `ERROR_PATTERN`. Không sửa migration cũ.
- Thêm `TrapEvidenceRule`, `ErrorTrapCatalog`, `ErrorTrapEvidence`; ghi bằng chứng đúng điều kiện trong năm đường chấm: lesson exercise, Practice có tính điểm, review, free practice có tính điểm và diagnostic probe. Không thêm vào assessment. Danh mục lỗi/KP con được đọc từ DB trước khi ghi; không gọi HTTP trong bước ghi bằng chứng.
- Thêm `EvidenceSource.ERROR_TRAP`, trọng số mặc định 0.3 cùng cấu hình Admin `mastery.mp1.source-weight.error-trap`; nguồn này không được tính là practice. Toàn bộ KP con của batch được đưa vào `withoutMilestone`.
- Đồng bộ catalog KP con từ `topic-sequence?includeErrorTraps=true`; giữ nguyên GET URI cũ cho consumer gọi `getTopicSequence()` không yêu cầu KP con.
- Bổ sung `findErrorTrapProfiles(userId)` để phase 5–6 đọc hồ sơ KP con riêng.

## Mục 6 — từng chỗ đọc/tổng hợp KP con

| Chỗ đọc trong plan | Xử lý trong phase 4 |
| --- | --- |
| `QuestService` — `profiles.findAll` | Không sửa use case; `JdbcMasteryProfileRepository.findAll` mặc định loại hồ sơ có `parent_kp_id`. |
| `GetLearnerReportUseCase` — `profiles.findAll` | Loại gián tiếp qua `findAll`; báo cáo tổng hợp chỉ nhận KP cha. |
| `GetReviewBasketUseCase` — `profiles.findAll` | Loại gián tiếp qua `findAll`; giỏ không lấy KP con làm mục ôn. |
| `GetSuggestionsUseCase` — `profiles.findAll` | Loại gián tiếp qua `findAll`. |
| `StaffInsightUseCase` — `profiles.findAll` | Loại gián tiếp qua `findAll`; truy vấn thống kê KP trong `JdbcStaffInsightRepository` cũng join catalog và lọc `parent_kp_id IS NULL`. |
| `SnapshotService` — `catalog.findAll` | Bỏ rõ các entry có `parentKnowledgePointId != null` khi tạo snapshot. |
| `LessonAccess` — `findMasteryHistories(userId)` | Không sửa caller; mặc định repository loại KP con. |
| `ReviewReevaluation` — `findMasteryHistories(userId)` | Không sửa caller; mặc định repository loại KP con. |
| `GetMasteryUseCase` — `findMasteryHistories(userId)` | Không sửa caller; mặc định repository loại KP con. |
| `SubmitPracticeAttemptUseCase` — `findMasteryHistories(userId)` | Không sửa caller; mặc định repository loại KP con trong kết quả tổng hợp. |
| `MasteryProfileUpdater` — `findMasteryHistories(userId, kpIds)` | Giữ KP con; truy vấn theo ID tiếp tục phục vụ tính mastery riêng cho KP con. |
| `JdbcBadgeRepository` | Thay tổng hợp bằng `NOT EXISTS` child check; hồ sơ không có catalog vẫn được giữ như trước. |
| `JdbcSnapshotRepository` | Join catalog và lọc `parent_kp_id IS NULL`. |
| `JdbcStaffInsightRepository` | Join catalog và lọc `parent_kp_id IS NULL`. |
| `JdbcReviewItemRepository` | Giữ nguyên: đọc các dòng review cá nhân, không tổng hợp hồ sơ/KP; phase 5 không tạo review cho KP con. |
| `JdbcKnowledgeEvidenceRepository` | `findMasteryHistories(userId)` loại KP con; overload có danh sách KP giữ con để updater tính riêng. |
| `JdbcMasteryProfileRepository` | `findAll(userId)` loại KP con; `findErrorTrapProfiles(userId)` đọc riêng KP con; các API batch/rebuild giữ cả hai loại để maintenance không bỏ sót mastery. |

## Tự kiểm Success Criteria

- [x] Test rule, trọng số, lưu bằng chứng và lọc tổng hợp xanh; suite module đầy đủ xanh.
- [x] Báo cáo liệt kê từng chỗ đọc trong bảng mục 6, ghi rõ đổi, giữ hoặc kế thừa lọc mặc định.

## Ghi chú

Không có thay đổi frontend, commit, HTTP call cho bằng chứng, hay sửa migration cũ.

## Bổ sung sau nghiệm thu chưa đạt

- Baseline trước lần sửa này: `mvn -q -pl services/learning-service -am test` — exit 0 (128 suites / 929 tests theo Surefire).
- Testcontainers bổ sung chạy qua các use case submit thật và PostgreSQL: `LessonSubmissionIntegrationTest.taggedLessonAnswerWritesChildEvidenceToPostgres`,
  `fiveLessonAnswersPersistTheExpectedTrapXHistory`, `PracticeIntegrationTest.practiceWritesTaggedChildEvidenceOnlyForTheCountedPackageSubmission`,
  `FreePracticeIntegrationTest.countedFreePracticeSubmissionPersistsTaggedChildEvidence`, và các test submit review/diagnostic trong
  `DiagnosticIntegrationTest`. Thực hành lặp không đếm không thêm evidence; bài thi không được gắn vào ErrorTrapEvidence.
- Bộ ví dụ 5 câu kiểm tra KP con X có thứ tự kết quả `false, true, false, true` và KP con Y nhận một evidence.
- `ErrorTrapEvidenceTest` kiểm tra child được thêm vào `withoutMilestone` và mã không có trong catalog bỏ qua an toàn.
- Red-proof: implementation hiện tại ban đầu xanh; đảo tạm guard `ErrorTrapEvidence.append` làm từng test Lesson, Practice, free-practice,
  review và diagnostic đỏ ở số dòng `kp_evidence`/profile mong đợi. Cùng phép đảo làm cả hai test `ErrorTrapEvidenceTest` đỏ.
- Red-proof ví dụ 5 câu: chạy riêng `fiveLessonAnswersPersistTheExpectedTrapXHistory` với cùng guard append đảo; assertion lịch sử X đỏ.
- Red-proof tổng hợp: đảo tạm `NOT EXISTS` thành `EXISTS` trong `JdbcMasteryProfileRepository.findAll`; test `MasteryProfileIntegrationTest.trapEvidenceBuildsChildMasteryButDefaultLearnerReadsExcludeIt`
  đỏ ở khẳng định profile/report KP con không lọt top-level và tổng hợp Band/skills/focus không đổi. Predicate đã được khôi phục.
- Lần chạy cuối `mvn -q -pl services/learning-service -am test`: exit 0; 129 suites, 941 tests, 0 failures/errors/skips.
- Không có assertion số đếm cũ nào được ghim hay nới trong bổ sung này. Không sửa migration cũ.
