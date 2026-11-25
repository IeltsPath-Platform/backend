# Báo cáo triển khai

Ngày: 2026-11-25

## Phạm vi đã triển khai

- **Phase 01:** gia cố lưu trạng thái và claim job AI, giới hạn số lần thử/hoàn quota, hết hạn an toàn, kiểm tra câu chưa xuất bản trước khi xuất bản đề.
- **Phase 02:** thêm kind `SECTION_QUESTIONS`, chụp nguồn Reading/Listening ở server, yêu cầu và sinh bản nháp, kiểm tra dạng câu/nguồn/trùng, cấu hình ngưỡng và giới hạn, giới hạn tier Gateway.
- **Phase 03:** thêm API xem/sửa/xóa/duyệt bản nháp theo `itemId` và `revision`, gắn câu được duyệt vào phần, API xuất bản loạt câu nháp và job hết hạn.
- **Phase 04:** thêm giao diện yêu cầu sinh câu, theo dõi bản nháp, xem/sửa/chọn câu và xử lý trạng thái phần; cập nhật nhãn, schema và CSS admin liên quan.
- **Phase 05:** cập nhật contract AI draft/content package, chính sách AI, kiến trúc, cấu hình Gateway và báo cáo này.

Các file thay đổi trải trên `backend/services/content-service`, `backend/infra/api-gateway`, `backend/infra/config-server/config-repo`, `backend/docs` và `frontend/src/features/admin/{ai-drafts,packages}`. Workspace có thay đổi chưa commit của các nhiệm vụ khác; không đưa chúng vào phạm vi báo cáo này và không hoàn tác.

## Kiểm tra đã chạy

- `mvn -q -pl services/content-service -am test` — **đạt**, mã thoát 0. Báo cáo Surefire ghi 1.429 lượt test, 0 lỗi, 0 failure, 1 skipped (`ImportTemplateFileTest`). Testcontainers/Docker hoạt động; test bị skip là test riêng có đánh dấu skip, không phải do thiếu Docker. Surefire có cảnh báo JVM shutdown kéo dài sau khi test đã trả mã 0.
- `mvn -q -pl services/content-service -am '-Dtest=JdbcAiDraftStoreTest,SectionQuestionDraftReviewUseCaseTest,SectionQuestionDraftGeneratorTest,SectionQuestionChecksTest,RequestSectionQuestionDraftUseCaseTest,ArchitectureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` — **đạt**, mã thoát 0. Bao gồm test PostgreSQL giới hạn một bản nháp đang mở trên mỗi phần, revision cũ, sửa câu không hợp lệ, duyệt lặp, chọn rỗng, snapshot nguồn không đáng tin cậy, không log nội dung, JSON lỗi, câu bị loại, số lượng tối đa và các quy tắc kiểm tra câu.
- `mvn -q -pl infra/api-gateway test` — **đạt**, mã thoát 0; có test đảm bảo tier AI chỉ áp dụng cho POST route tạo câu theo phần.
- `npx.cmd eslint src/features/admin` — **đạt**.
- `npx.cmd tsc -b` — **đạt**.
- `npm.cmd run build` — **đạt**; build phát sinh worker PDF như cấu hình hiện có. Có cảnh báo chunk lớn hơn 500 KB.
- `npx.cmd vitest run src/features/admin` — **không hoàn toàn đạt**: 927 passed, 5 failed. Các lỗi nằm ở hai test `AdminRoutes`, hai test `QuestionDraftForm` và một test `ErrorTypesPage`, thuộc khu vực khác với phần AI câu hỏi; không sửa hoặc bỏ qua chúng.
- `docker compose config --quiet` — **đạt**, mã thoát 0; Docker báo không truy cập được một số file cấu hình cục bộ, không ảnh hưởng kết quả parse. Không đọc nội dung `.env` hay secret.
- `graphify update .` — đã chạy cho backend và frontend; backend hoàn tất mã thoát 0. Graphify báo thiếu parser SQL tùy chọn, nên các file SQL không được đưa vào graph.

## Ma trận E1–E41

Trạng thái dưới đây phản ánh test tự động hoặc kiểm tra thực tế có bằng chứng trong lượt này. “Chưa đủ” nghĩa là cần thêm test riêng hoặc kiểm tra tay như plan yêu cầu.

| Trường hợp | Trạng thái | Bằng chứng / phần còn thiếu |
|---|---|---|
| E1 | Có test | `RequestSectionQuestionDraftUseCaseTest` từ chối nguồn trống/quá ngắn. |
| E2 | Có test | Cùng test xác nhận không tiêu quota khi nguồn không hợp lệ. |
| E3 | Có test | Test nguồn quá giới hạn ký tự, không cắt ngắn nguồn. |
| E4 | Chưa đủ | Chưa có test riêng về nguồn thay đổi sau lúc tạo khi mở trang duyệt. |
| E5 | Có test | `SectionQuestionDraftGeneratorTest` kiểm tra nguồn được đóng gói như dữ liệu không tin cậy. |
| E6 | Có test một phần | `RequestSectionQuestionDraftUseCaseTest` chứng minh Reading dùng passage thay vì transcript; chưa có test snapshot Listening riêng. |
| E7 | Có test một phần | `JdbcAiDraftStoreTest.anAuthorCanHaveOnlyOneOpenDraftForASection` kiểm unique index và mở khóa sau khi duyệt; chưa thử hai request cạnh tranh đồng thời. |
| E8 | Có test một phần | `JdbcAiDraftStoreTest.dailyAllowanceStopsAtTheLimitAndRefundReopensIt` kiểm quota chung; chưa có test tích hợp quota hết qua endpoint tạo câu theo phần. |
| E9 | Có test một phần | Validation số lượng có ở server và modal; chưa có đầy đủ test biên API cho mọi giá trị. |
| E10 | Có kiểm tra một phần | Server và UI từ chối kỹ năng/loại đề không hỗ trợ; chưa có test riêng cho toàn bộ tổ hợp. |
| E11 | Có kiểm tra một phần | Có guard bản nháp/quan hệ package-section; chưa có test API bao phủ mọi trường hợp không tồn tại. |
| E12 | Chưa đủ | UI có gợi ý số câu còn thiếu; chưa có test hành vi với phần đã đủ/vượt mục tiêu. |
| E13 | Có kiểm tra một phần | Cơ chế job chung có xử lý lỗi và hoàn quota; chưa kiểm tay timeout, 429/5xx và mất kết nối qua LLM thật. |
| E14 | Có test | `SectionQuestionDraftGeneratorTest` kiểm JSON sai và thiếu `items`. |
| E15 | Có test | Test generator kiểm câu bị loại, lý do, và kết quả một phần. |
| E16 | Có test | Test generator yêu cầu ít hơn số ứng viên trả về và chỉ giữ số lượng yêu cầu. |
| E17 | Có test | Test generator xác nhận `NO_USABLE_ITEMS`. |
| E18 | Có test | `SectionQuestionChecksTest` bao phủ trùng trong phần và trong bản nháp. |
| E19 | Có test | Test kiểm đáp án không xuất hiện như từ độc lập trong nguồn. |
| E20 | Có test | Test kiểm đáp án lộ trong câu, số chỗ trống và độ dài đáp án. |
| E21 | Có test | Test kiểm bộ lựa chọn TFNG và lựa chọn trắc nghiệm trùng. |
| E22 | Có test | Test kiểm câu hỏi sao chép đoạn dài từ nguồn. |
| E23 | Chưa làm kiểm tay | Cần người duyệt đối chiếu đáp án/câu đồng nghĩa và câu có hai đáp án đúng trên nội dung thật. |
| E24 | Có test | `JdbcAiDraftStoreTest.staleGeneratingDraftsReturnToTheQueue` kiểm số lần claim tối đa; `AiDraftJobStateUseCasesTest` kiểm hoàn quota khi hết lượt. |
| E25 | Có test | `JdbcAiDraftStoreTest.aReplacementClaimMakesLateWorkerUpdatesNoOps` và `separateWorkersNeverClaimTheSameDraft`, cùng `AiDraftJobStateUseCasesTest`, kiểm worker stale không ghi/hoàn quota sai. |
| E26 | Có guard một phần | Duyệt đọc lại package/section và từ chối thay đổi; chưa có integration test xóa/xuất bản trong lúc job đang chạy. |
| E27 | Có test một phần | `SectionQuestionDraftReviewUseCaseTest` kiểm duyệt gọi lại cùng tập và từ chối tập khác; chưa có test hai request duyệt song song. |
| E28 | Có test một phần | Cùng test xác nhận revision cũ bị từ chối; chưa có test API hai tab và giữ nội dung người dùng đang nhập. |
| E29 | Có test một phần | Cùng test xác nhận sửa thành câu không hợp lệ trả `422` và không lưu; chưa bao phủ đủ mọi lý do. |
| E30 | Có guard một phần | Duyệt kiểm trùng với câu mới nhất; chưa có integration test xác nhận không tạo câu nào. |
| E31 | Có guard một phần | Duyệt kiểm trạng thái bản đề/phần/kỹ năng; thiếu integration test từng nhánh. |
| E32 | Chưa đủ | Thiết kế nằm trong transaction; chưa có test lỗi khi tạo câu thứ k để chứng minh rollback toàn bộ. |
| E33 | Có kiểm tra một phần | Modal dùng API lưu trạng thái bản nháp; chưa có kiểm tra tay F5 trong lúc sinh/duyệt. |
| E34 | Có kiểm tra một phần | Job hết hạn `READY` quá TTL đã triển khai; chưa có test tích hợp xác nhận mở khóa phần và không chạm `APPROVED`. |
| E35 | Có guard | API lọc theo chủ sở hữu và kind, trả như không tìm thấy; chưa có test controller riêng trong bộ mới. |
| E36 | Có test một phần | Test review xác nhận không thể duyệt danh sách rỗng; chưa có test UI thao tác bỏ hết câu. |
| E37 | Có test | Test xuất bản package từ chối câu hỏi chưa xuất bản; nằm trong `PublishContentPackageUseCaseTest`. |
| E38 | Chưa đủ | API xuất bản loạt kiểm tra trước khi ghi; chưa có test integration nhiều câu với một câu lỗi để xác nhận rollback. |
| E39 | Có test | Test bắt log xác nhận không ghi nội dung nguồn/prompt/phản hồi. |
| E40 | Có test | `AuthRouteConsistencyTest` đã chạy trong bộ Gateway. |
| E41 | Chưa làm kiểm tay | Chưa triển khai theo thứ tự frontend trước/backend sau trên môi trường chạy thật; yêu cầu một phiên bản backend duy nhất khi migration. |

## Chưa kiểm tra / việc còn lại

- Chạy LLM thật cho tối thiểu một phần Reading và một phần Listening, ghi số lượng yêu cầu/được giữ/bị loại theo từng lý do và hiệu chỉnh ngưỡng. Không có thông tin cấu hình hoặc quyền truy cập LLM trong môi trường test hiện tại.
- Kiểm tay E23 và E41; F5 khi đang sinh, hai tab cùng sửa, đổi nguồn trước duyệt, màn hình hẹp và điều hướng bàn phím.
- Bổ sung test riêng còn thiếu ở E4, E7–E13, E24–E38 theo bảng trước khi coi toàn bộ tiêu chí plan đã đạt.
- Frontend Vitest còn 5 test lỗi nêu trên; chưa chạy lại trình duyệt thật.

Không tạo commit, push hoặc PR.
