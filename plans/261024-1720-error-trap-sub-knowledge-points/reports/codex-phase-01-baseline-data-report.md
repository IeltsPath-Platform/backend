# Báo cáo phase 1: baseline data và characterization tests

Status: BLOCKED

## File đã sửa/tạo
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/CourseSequenceIntegrationTest.java` — characterization thứ tự ưu tiên lesson, số KP khớp, rồi UUID bộ; test xanh.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapBaselineDataTest.java` — truy vấn baseline trên Testcontainers schema V78.
- `plans/261024-1720-error-trap-sub-knowledge-points/reports/baseline-data.md` — số liệu baseline và điều kiện dừng.
- Báo cáo phase này.

Không sửa code sản phẩm hoặc migration. Migration mới nhất đã kiểm tra: `V78__section_question_sort_order_deferrable.sql`.

## Lệnh test đã chạy và kết quả
- `mvn -q -pl services/content-service -am test` (baseline trước test mới): exit 0; 921 tests, 0 failures, 0 errors, 1 skipped.
- `mvn -q -pl services/learning-service -am test` (baseline): exit 0; 902 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -q -pl services/content-service -am test -Dtest=CourseSequenceIntegrationTest '-Dsurefire.failIfNoSpecifiedTests=false'`: exit 0; characterization Testcontainers xanh. Lần gọi trước đó thiếu quoting cho property Surefire và Maven phân tích nhầm thành lifecycle phase; lệnh đã sửa chạy thành công.
- `mvn -q -pl services/content-service -am test -Dtest=ErrorTrapBaselineDataTest '-Dsurefire.failIfNoSpecifiedTests=false'`: exit 0; dữ liệu V78 truy vấn được, test xanh.
- `mvn -q -pl services/content-service -am test` (toàn module sau test mới): exit 0; 921 tests, 0 failures, 0 errors, 1 skipped.
- Một lần chạy Maven trong sandbox bị chặn tải BOM từ Maven Central (`Permission denied: getsockopt`); cùng lệnh sau đó chạy với quyền mạng được cấp và kết thúc exit 0.

Test bị skip: `ImportTemplateFileTest.writeTheTemplateFile`, do thiếu system property `writeImportTemplate` (chỉ dùng khi chủ ý ghi lại workbook). Không có test đỏ nền trong báo cáo Surefire. Learning có log retry RabbitMQ/Hikari trong teardown container, nhưng suite kết thúc exit 0 và báo cáo XML không có failure/error.

## Kết quả đo và lý do dừng
- 28 mã lỗi ACTIVE; 19 có KP cha.
- T/F/NG đã xuất bản: FALSE 18 câu/12 bộ, NO 4/4, NOT_GIVEN 21/13, TRUE 19/13, YES 2/2.
- Ba bộ `PS-TF-A/B/C` đều chỉ có một câu đúng FALSE và một câu đúng NOT_GIVEN. Backfill giả lập cho hai mã T/F/NG cho tối đa 1 câu cùng bẫy/bộ; không bộ nào đạt ngưỡng ≥2.
- Trong 19 mã ACTIVE có KP cha, chỉ `INFERENCE_UNSUPPORTED` hiện có một bộ đạt ≥2 câu bẫy; mọi mã khác có 0 bộ đạt ngưỡng. Chi tiết ở `baseline-data.md`.
- Không thu thập snapshot band/báo cáo học viên vì điều kiện dừng của plan đã xảy ra trước phase 2.

## Test chưa chạy và câu hỏi còn mở
- Chưa chạy `BandEstimator`/learner-report snapshot riêng làm mốc; dừng theo hướng dẫn khi dữ liệu T/F/NG không có bộ nào đạt ≥2 câu/bẫy.
- Cần quyết định cách xử lý khoảng trống dữ liệu T/F/NG trước khi phase 2. Theo chỉ dẫn, chưa bắt đầu phase 2, 3, 9, 4–8.
