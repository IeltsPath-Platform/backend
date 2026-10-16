# Kết quả hoàn thiện dashboard thống kê cá nhân

Ngày: 2026-10-15 (Asia/Ho_Chi_Minh).

BE giữ branch `docs/personal-dashboard-statistics-plan`; FE giữ branch `main`. Giữ các thay đổi chưa commit có sẵn. Không reset/clean, commit/push, đọc hoặc hiển thị nội dung `.env`. Không đổi dependency, migration, controller cũ, Gateway, shared security hoặc config runtime trong lượt này.

## 1. Khôi phục tài liệu và fixture

Đã chạy đúng `git checkout fee364e^ -- docs/contracts docs/system-architecture.md` (exit 0). Lần đầu sandbox chặn ghi `.git/index.lock`; chạy lại với quyền được cấp đã thành công. Đây là checkout theo đường dẫn, không đổi branch. Lệnh đặt các file khôi phục vào index theo hành vi Git; không commit.

Tám file được khôi phục:

- `docs/contracts/answer-spec-v1-vectors.json`
- `docs/contracts/answer-spec-v1.md`
- `docs/contracts/assessment-completed-v2.md`
- `docs/contracts/game-content-snapshot-v1.md`
- `docs/contracts/learning-content-internal-v1.md`
- `docs/contracts/lesson-learning-v1.md`
- `docs/contracts/lesson-writing-v1.md`
- `docs/system-architecture.md`

Fixture tồn tại. `git diff fee364e^ -- docs/contracts docs/system-architecture.md` không có khác biệt: nội dung Git khớp bản gốc; checkout áp quy tắc LF/CRLF của repository. Không sửa `AnswerSpecGraderTest`, bộ chấm hoặc vector. Hai suite grader đã pass, mỗi service 34 test; không có vector lệch cần báo/dừng.

Đã kiểm các tham chiếu `docs/...` trong BE `AGENTS.md` và `CLAUDE.md`: `docs/system-architecture.md`, `docs/contracts/`, cùng năm tên rút gọn được mở rộng thành `lesson-learning-v1.md`, `lesson-writing-v1.md`, `learning-content-internal-v1.md`, `answer-spec-v1.md`, `assessment-completed-v2.md`. Tất cả tồn tại; mục §7 và §11 của kiến trúc cũng còn. **Đường dẫn còn thiếu trong phạm vi kiểm tra: không có.** Không viết lại tài liệu kiến trúc. FE không có `CLAUDE.md`; đã đọc `AGENTS.md` và `README.md` của FE.

## 2. Kết quả từng yêu cầu

| Mục | Kết quả |
|---|---|
| 2a — ngày của band | Đã thêm `@RequestParam(name="timezone", defaultValue="Asia/Ho_Chi_Minh")`; kiểm tra timezone bằng danh sách zone ID của Java, từ chối zone không tồn tại và offset thuần bằng 400. Khoảng truy vấn là từ đầu ngày `from` đến trước đầu ngày kế tiếp của `to` theo zone đó. `dataFrom` dùng cùng timezone; timestamp vẫn là instant. FE gửi `Intl.DateTimeFormat().resolvedOptions().timeZone`. |
| 2b — reload | Đã tách scope account/filter khỏi version request. Reload giữ dữ liệu cũ; card ưu tiên hiển thị dữ liệu thay vì dòng tải. Đổi account/filter hoặc logout xóa dữ liệu và lỗi cũ trước khi render con. Abort và kiểm key/version loại response trễ, kể cả A → B → A. Focus khi tab hiện giới hạn một lần/30 giây; sự kiện thay đổi thống kê và retry thủ công vẫn reload ngay. Refresh lỗi giữ dữ liệu trước đó cùng thông báo/nút thử lại. |
| 2c — course của assessment | Đã điều tra; giữ `courseId=null` theo nhánh xử lý được yêu cầu. Không có course/topic hợp lệ được lưu tại Assessment để trả về. Lý do và bằng chứng bên dưới. |
| 2d — trên 100 khóa | Giữ nguyên giới hạn và 503 của BE. FE dịch riêng `503 PROGRESS_UNAVAILABLE` thành thông báo tiếng Việt nêu rõ giới hạn 100 khóa đã bắt đầu, khả năng thiếu/quá lớn metadata và hướng thử lại/liên hệ hỗ trợ. Không khẳng định mọi 503 đều do số khóa vì BE dùng chung mã lỗi cho các nguyên nhân đó. |
| 2e — tài liệu | Thêm ba tên biến `LEARNING_SERVICE_URL`, `ASSESSMENT_SERVICE_URL`, `LIBRARY_SERVICE_URL` vào cạm bẫy chạy user-service trên host, yêu cầu trỏ localhost, không ghi giá trị gán. Bỏ dòng Phase 1 trùng; cập nhật cả ba phase và design theo kết quả có bằng chứng, giữ các tiêu chí browser/E2E chưa nghiệm thu. |

Test ngày bao gồm bài nộp `2026-10-14T23:30:00Z` = 06:30 ngày 15/10 tại Việt Nam: có trong ngày 15, không trong ngày 14. Test persistence còn kiểm đúng đầu ngày được tính, đầu ngày kế tiếp bị loại. Unit test kiểm ngày DST 08/03/2026 tại New York dài 23 giờ, `dataFrom` địa phương và timezone sai. Test MVC kiểm default, timezone truyền rõ ràng và 400.

### Bằng chứng cho `courseId=null`

- `AssessmentAttempt` và `AssessmentAttemptJpaEntity` có `userId`, `packageVersionId`, `attemptType`, trạng thái/thời điểm và `learningGoalId` legacy; không có topic/course.
- Schema `assessment_attempts` và các migration Assessment không lưu topic/course; V5 chỉ mở thêm loại `COURSE_GATE`.
- `ContentPackageProvider.PackageVersion` chỉ có package version, loại package và sections. `AttemptCreator` lưu snapshot section, câu hỏi, đáp án và knowledge point, không có mapping course/topic.
- `packageVersionId` không phải course ID và không chứng minh course ownership. Không suy diễn từ mã đề hoặc knowledge point, không gọi DB service khác, không thêm HTTP lookup hay đổi `AssessmentCompleted.v2`.
- Test mới kiểm cả `TOPIC_GATE` và `COURSE_GATE` đang hoạt động/thuộc user trả `sourceId` đúng, `courseId=null`. Thời gian thi vẫn thuộc tổng ALL, chưa thể gán vào course.

## 3. File sửa/thêm trong lượt này

Ngoài tám file khôi phục ở trên, các đường dẫn sau được sửa/thêm. Các file tính năng đã untracked từ lượt trước vẫn được giữ nguyên trạng thái Git; danh sách này chỉ mô tả phần chạm đến trong lượt hiện tại.

BE:

- `services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/BandStatisticsController.java`
- `services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetBandHistoryUseCase.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/api/controller/BandStatisticsControllerTest.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GetBandHistoryUseCaseTest.java`
- `services/assessment-service/src/test/java/com/ieltspath/assessment/infrastructure/persistence/BandStatisticsPersistenceTest.java`
- `CLAUDE.md`
- `plans/261014-1634-personal-dashboard-statistics/plan.md`
- `plans/261014-1634-personal-dashboard-statistics/phase-01-study-time-collection.md`
- `plans/261014-1634-personal-dashboard-statistics/phase-02-domain-statistics-apis.md`
- `plans/261014-1634-personal-dashboard-statistics/phase-03-frontend-integration-and-validation.md`
- `plans/261014-1634-personal-dashboard-statistics/dashboard-statistics-design.md`
- Báo cáo `reports/final-fixes-report.md` này.

FE:

- `src/features/overview/api/overviewApi.ts`
- `src/features/overview/hooks/useStatisticsResource.ts`
- `src/features/overview/hooks/statisticsResourceState.ts` (mới)
- `src/features/overview/hooks/statisticsResourceState.test.mjs` (mới)
- `src/features/overview/components/StatisticsCard.tsx`

## 4. Kiểm chứng

Các lệnh BE chạy tại `D:\Fall2026\New folder\BE\backend`; FE tại `D:\Fall2026\New folder\FE\frontend`.

| Lệnh | Exit | Kết quả |
|---|---:|---|
| `mvn -B -ntp -fae -pl services/assessment-service,services/learning-service -am test '-Dtest=AnswerSpecGraderTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Ddebug=false'` | 0 | PASS: 34 test mỗi service, 0 fail/error/skip. Lượt đầu bị sandbox chặn tải Maven POM; đã chạy lại thành công với quyền được cấp. |
| `mvn -B -ntp -pl services/assessment-service -am test '-Dtest=GetBandHistoryUseCaseTest,BandStatisticsControllerTest,BandStatisticsPersistenceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Ddebug=false'` | 0 | PASS: use case 7, controller 6, persistence 4; tổng 17, 0 fail/error/skip. PostgreSQL Testcontainer chạy thật. |
| `mvn -B -ntp -fae -pl services/user-service,services/content-service,services/library-service,services/assessment-service,services/learning-service -am test '-Ddebug=false'` (lần sửa này) | 0 | BUILD SUCCESS. `common-security` 8/8 · `user-service` 132/132 · `content-service` 255/255 · `library-service` 70/70 · `learning-service` 277/277 · `assessment-service` 243/243. **Tổng: 985/985, 0 fail, 0 error, 0 skip.** `AnswerSpecGraderTest` 34/34 pass tại assessment; `BandStatisticsPersistenceTest` 4/4 pass (kể cả test ranh giới ngày VN). Testcontainers PostgreSQL chạy thật. |
| `git checkout fee364e^ -- docs/contracts docs/system-architecture.md` | 0 | 8 file khôi phục; không đổi branch, không commit. |
| `git diff --check` (BE) | 0 | PASS; chỉ có cảnh báo chuẩn hóa LF/CRLF. |
| `git diff --cached --check` (BE) | 0 | PASS cho file khôi phục trong index. |
| `docker compose config --quiet` | 0 | PASS; sandbox cảnh báo không đọc được `C:\Users\LOQ\.docker\config.json`, lệnh kiểm cấu hình vẫn thành công. Không in nội dung cấu hình hoặc `.env`. |
| `npm run lint` | 0 | PASS. |
| `npm run typecheck` | 0 | PASS. |
| `npm run build` | 0 | PASS; cảnh báo Vite chunk JavaScript lớn hơn 500 kB. |
| `node --test src/features/study-time/activeIntervals.test.mjs` | 0 | PASS: 4 pass, 0 fail/skip. |
| `node --test src/features/overview/hooks/statisticsResourceState.test.mjs` | 0 | PASS: 4 pass, 0 fail/skip; giữ dữ liệu khi refresh/thất bại, xóa khi đổi scope, loại response cũ. |
| `git diff --check` (FE) | 0 | PASS; chỉ có cảnh báo LF/CRLF ở các thay đổi có sẵn. |

Rà soát độc lập phần sửa không tìm thấy lỗi runtime cần xử lý; đã kiểm caller, phân quyền theo user, biên ngày, reload, key/version và thông báo 503. Rà soát lifecycle FE bằng code và test reducer, không phải browser test.

## 5. Giới hạn còn lại

- Chưa chạy browser/E2E, HTTP xuyên toàn stack, kiểm thực tế nhiều tab, sleep/offline/logout/audio, keyboard, screen reader hoặc mobile. Plan vẫn giữ các tiêu chí nghiệm thu tương ứng chưa hoàn tất.
- `courseId` Assessment vẫn null vì không có dữ liệu sở hữu đủ tin cậy, như mục 2c; không thêm schema hoặc đổi contract event để giải quyết ngoài scope.
- Giới hạn 100 khóa và giới hạn metadata vẫn giữ. BE chưa có mã lỗi riêng phân biệt từng nguyên nhân; FE thông báo trung thực các trường hợp thay vì đoán.
- Khoảng tuần/tháng cho **thời gian học** còn được chọn theo ngày trình duyệt trong khi BE ghi ngày theo timezone hồ sơ; sửa timezone lần này chỉ áp dụng lịch sử band theo yêu cầu.
- Các controller cũ dùng tham số không ghi tên và reactor thiếu `-parameters` nằm ngoài scope; không sửa trong lượt này.
- Tài liệu kiến trúc khôi phục giữ nguyên nội dung lịch sử, không cập nhật toàn bộ kiến trúc để mô tả dashboard mới.
