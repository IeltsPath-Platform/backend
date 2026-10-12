# IELTSPath — danh sách UC phân rã chi tiết (đề xuất)

Ngày rà soát: **09/10/2026**.

Danh sách dưới đây có **206 mục UC ứng viên** sau khi phân rã theo thao tác cụ thể: **170 mục tiêu/thao tác**, **34 UC hỗ trợ** và **2 luồng tự động hướng tới người học**. Tổng được đếm từ danh sách, không đặt trước một số lượng mục tiêu.

Đây là mức phân rã chi tiết phục vụ viết đặc tả và rà soát chức năng. Nó không có nghĩa hệ thống bắt buộc có đúng số đó theo UML: khi xây dựng mô hình chính thức, các UC hỗ trợ có thể nằm trong luồng của UC lớn hoặc được nối bằng quan hệ phù hợp. Mã `UC-D` chỉ là mã của bản đề xuất này.

## Nguồn và cách phân rã

- [SRS hiện tại](<Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx>): Part 3, System Behaviour và AC của FT-01–FT-55; Part 4.2, DC38 cho soft delete flashcard/deck.
- [RTW hiện tại](<../rp_3.1_requirements-traceability-workbook/Report 3.1_RTW_IELTSPath_v1.xlsx>): Sheet 2 để đối chiếu tên UC và actor tổng quan.
- Tách những thao tác được SRS mô tả thành từng dòng, gồm tạo, đọc, sửa, xóa và các hành động nghiệp vụ riêng như pause goal, cấp Premium, refund debit, ready room hoặc finalize kết quả.
- Thao tác dùng chung hoặc bước trong một mục tiêu lớn được đánh dấu **Hỗ trợ**; các loại bài thi dùng những thao tác attempt chung, nên tổng danh sách bao gồm cả UC chuyên biệt và thành phần dùng chung.
- Các kết quả của cùng một lần đọc, như course recommendation và progress, được giữ trong một UC. Các nhánh dữ liệu như loại key POINTS/PREMIUM cũng dùng chung UC khi actor thực hiện cùng một thao tác.
- Hint, điều kiện mở bài, tính mastery, debit sau chấm, xử lý idempotency, gọi provider, snapshot, outbox và retry delivery là hành vi trong UC liên quan. Chúng không được cộng thêm chỉ vì có hàm/API riêng.
- Danh sách bao gồm yêu cầu SRS chưa triển khai. Đây là rà soát phạm vi tài liệu, không phải xác nhận tất cả UC đã có trong code.

Actor **Người dùng được phép** trong các catalog là ADMIN, CONTENT_AUTHOR, CUSTOMER hoặc EXAMINER theo FT-15/36/37. **Learner** tương ứng CUSTOMER; **Host** là learner làm chủ room. Administrator chỉ được tham gia UC khi nguồn SRS nêu quyền đó.

## Tài khoản, đăng nhập và hồ sơ (13 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D001 | Đăng ký tài khoản | Guest | FT-01 | Mục tiêu | — |
| UC-D002 | Đăng nhập bằng mật khẩu | Người dùng | FT-02 | Mục tiêu | — |
| UC-D003 | Làm mới phiên đăng nhập | Người dùng | FT-03 | Hỗ trợ | Luồng session của FT-03. |
| UC-D004 | Đăng xuất phiên hiện tại | Người dùng | FT-03 | Mục tiêu | — |
| UC-D005 | Yêu cầu liên kết đặt lại mật khẩu | Guest | FT-04 | Mục tiêu | — |
| UC-D006 | Đặt mật khẩu mới bằng reset token | Guest | FT-04 | Mục tiêu | — |
| UC-D007 | Đăng nhập bằng OAuth | Guest | FT-05 | Mục tiêu | Liên kết lần đầu/tạo CUSTOMER là nhánh của UC này. |
| UC-D008 | Xem danh sách OAuth identity đã liên kết | Người dùng / Administrator | FT-05, FT-08 | Mục tiêu | — |
| UC-D009 | Hủy liên kết một OAuth identity | Người dùng / Administrator | FT-05, FT-08 | Mục tiêu | — |
| UC-D010 | Xem thông tin tài khoản của mình | Người dùng | FT-06 | Mục tiêu | — |
| UC-D011 | Cập nhật thông tin tài khoản của mình | Người dùng | FT-06 | Mục tiêu | — |
| UC-D012 | Xem learner profile | Learner / Administrator | FT-06, FT-08 | Mục tiêu | — |
| UC-D013 | Cập nhật learner profile | Learner / Administrator | FT-06, FT-08 | Mục tiêu | — |

## Mục tiêu học tập (8 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D014 | Thiết lập mục tiêu học tập | Learner | FT-09 | Mục tiêu | — |
| UC-D015 | Xem mục tiêu đang ACTIVE | Learner / Administrator | FT-09, FT-08 | Mục tiêu | — |
| UC-D016 | Xem lịch sử mục tiêu học tập | Learner / Administrator | FT-09, FT-08 | Mục tiêu | — |
| UC-D017 | Cập nhật thông tin mục tiêu học tập | Learner / Administrator | FT-09, FT-08 | Mục tiêu | FT-09 yêu cầu cập nhật goal hiện tại; còn khác với current release. |
| UC-D018 | Tạm dừng mục tiêu học tập | Learner | FT-09 | Mục tiêu | — |
| UC-D019 | Tiếp tục mục tiêu đang PAUSED | Learner | FT-09 | Mục tiêu | — |
| UC-D020 | Đánh dấu mục tiêu ACHIEVED | Learner | FT-09 | Mục tiêu | — |
| UC-D021 | Đánh dấu mục tiêu ABANDONED | Learner | FT-09 | Mục tiêu | — |

## Quản trị tài khoản (9 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D022 | Xem danh sách người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D023 | Xem thông tin một tài khoản người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D024 | Tạo tài khoản người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D025 | Cập nhật thông tin tài khoản người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D026 | Thay thế tập vai trò của người dùng | Administrator | FT-08, FT-07 | Mục tiêu | — |
| UC-D027 | Thay đổi trạng thái tài khoản người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D028 | Xóa tài khoản người dùng | Administrator | FT-08 | Mục tiêu | — |
| UC-D029 | Xem danh sách account action token của người dùng | Administrator | FT-04, FT-08 | Mục tiêu | — |
| UC-D030 | Thu hồi account action token | Administrator | FT-04, FT-08 | Mục tiêu | — |

## Plan, key product, subscription và wallet (14 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D031 | Xem danh sách plan | Người dùng đã đăng nhập | FT-10 | Mục tiêu | — |
| UC-D032 | Tạo plan | Administrator | FT-10 | Mục tiêu | — |
| UC-D033 | Xem danh sách key product | Người dùng đã đăng nhập | FT-10 | Mục tiêu | — |
| UC-D034 | Tạo key product | Administrator | FT-10 | Mục tiêu | POINTS/PREMIUM là hai loại dữ liệu của cùng thao tác. |
| UC-D035 | Cấp thời hạn Premium cho người dùng | Administrator | FT-10 | Mục tiêu | Bao gồm cộng thêm ngày khi subscription vẫn còn hạn. |
| UC-D036 | Xem subscription của mình | Learner | FT-10 | Mục tiêu | — |
| UC-D037 | Phát hành activation key | Sale / Administrator | FT-11 | Mục tiêu | SRS cho staff; current release chỉ cho ADMIN. |
| UC-D038 | Thu hồi activation key chưa redeem | Sale / Administrator | FT-11 | Mục tiêu | — |
| UC-D039 | Kích hoạt activation key | Learner | FT-11, FT-10, FT-12, FT-14 | Mục tiêu | Lợi ích POINTS/PREMIUM là nhánh theo key product. |
| UC-D040 | Xem số dư điểm | Learner | FT-12 | Mục tiêu | — |
| UC-D041 | Xem lịch sử giao dịch điểm | Learner | FT-12 | Mục tiêu | — |
| UC-D042 | Điều chỉnh điểm người dùng | Administrator | FT-12 | Mục tiêu | — |
| UC-D043 | Refund một khoản debit | Administrator | FT-12 | Mục tiêu | — |
| UC-D044 | Xem quyền feature của subscription | Administrator | FT-13 | Mục tiêu | FT-13 nêu entitlement read trong current release chỉ dành cho ADMIN. |

## Course, topic và knowledge point của curriculum (9 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D045 | Xem danh sách course của curriculum | Người dùng được phép | FT-15 | Mục tiêu | Catalog nội dung; khác danh sách course kèm tiến độ của learner. |
| UC-D046 | Tạo course | Content Author / Administrator | FT-15 | Mục tiêu | — |
| UC-D047 | Cập nhật course | Content Author / Administrator | FT-15 | Mục tiêu | Trạng thái ACTIVE/INACTIVE là trường của thao tác cập nhật. |
| UC-D048 | Xem danh sách topic | Người dùng được phép | FT-15 | Mục tiêu | — |
| UC-D049 | Xem thông tin topic | Người dùng được phép | FT-15 | Mục tiêu | — |
| UC-D050 | Tạo topic | Content Author / Administrator | FT-15 | Mục tiêu | — |
| UC-D051 | Cập nhật topic | Content Author / Administrator | FT-15 | Mục tiêu | — |
| UC-D052 | Xem danh sách knowledge point | Người dùng đã đăng nhập | FT-15 | Mục tiêu | — |
| UC-D053 | Tạo knowledge point | Content Author / Administrator | FT-15 | Mục tiêu | — |

## Ngân hàng câu hỏi và content package (10 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D054 | Tra cứu ngân hàng câu hỏi theo purpose/skill | Content Author / Administrator | FT-16 | Mục tiêu | — |
| UC-D055 | Đọc nội dung phiên bản câu hỏi | Content Author / Administrator | FT-16 | Mục tiêu | — |
| UC-D056 | Tạo câu hỏi | Content Author / Administrator | FT-16 | Mục tiêu | — |
| UC-D057 | Tạo phiên bản mới của câu hỏi | Content Author / Administrator | FT-16 | Mục tiêu | — |
| UC-D058 | Archive câu hỏi | Content Author / Administrator | FT-16 | Mục tiêu | — |
| UC-D059 | Tạo content package | Content Author / Administrator | FT-17 | Mục tiêu | TOPIC_TEST/COURSE_TEST hiện chỉ từ seed, không có public create API. |
| UC-D060 | Tạo phiên bản DRAFT của package | Content Author / Administrator | FT-17 | Mục tiêu | — |
| UC-D061 | Soạn section của phiên bản package | Content Author / Administrator | FT-17 | Hỗ trợ | Phân rã thao tác soạn nội dung phiên bản. |
| UC-D062 | Gán câu hỏi vào section của package | Content Author / Administrator | FT-17 | Hỗ trợ | Phân rã thao tác soạn nội dung phiên bản. |
| UC-D063 | Xuất bản phiên bản package | Content Author / Administrator | FT-17 | Mục tiêu | Validation/purpose/ownership là bước nội bộ, không đếm riêng. |

## Media và soạn lesson (10 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D064 | Upload media lên Media Storage | Content Author / Administrator | FT-18 | Mục tiêu | SRS yêu cầu; upload chưa được tích hợp. |
| UC-D065 | Ghi reference cho media asset | Content Author / Administrator | FT-18 | Mục tiêu | — |
| UC-D066 | Đọc media asset trực tiếp | Content Author / Administrator | FT-18 | Mục tiêu | Asset có thể chứa transcript; learner không được đọc trực tiếp. |
| UC-D067 | Tạo lesson trong topic | Content Author / Administrator | FT-19 | Mục tiêu | — |
| UC-D068 | Soạn block lý thuyết của lesson | Content Author / Administrator | FT-19 | Hỗ trợ | Bao gồm gắn knowledge point mà block dạy. |
| UC-D069 | Gắn asset vào lesson | Content Author / Administrator | FT-19, FT-18 | Hỗ trợ | — |
| UC-D070 | Soạn block exercise của lesson | Content Author / Administrator | FT-19 | Hỗ trợ | — |
| UC-D071 | Soạn block essay của lesson | Content Author / Administrator | FT-19 | Hỗ trợ | — |
| UC-D072 | Sắp xếp thứ tự các block của lesson | Content Author / Administrator | FT-19 | Hỗ trợ | — |
| UC-D073 | Xuất bản lesson | Content Author / Administrator | FT-19 | Mục tiêu | — |

## Catalog từ vựng và video (10 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D074 | Tạo mục từ vựng | Content Author / Administrator | FT-36 | Mục tiêu | — |
| UC-D075 | Thêm sense cho mục từ vựng | Content Author / Administrator | FT-36 | Mục tiêu | — |
| UC-D076 | Tìm kiếm từ vựng | Người dùng được phép | FT-36 | Mục tiêu | — |
| UC-D077 | Đọc mục từ vựng và các sense | Người dùng được phép | FT-36 | Mục tiêu | — |
| UC-D078 | Tạo learning video | Content Author / Administrator | FT-37 | Mục tiêu | — |
| UC-D079 | Thêm segment cho video | Content Author / Administrator | FT-37 | Mục tiêu | — |
| UC-D080 | Gắn lexical entry vào video segment | Content Author / Administrator | FT-37 | Mục tiêu | — |
| UC-D081 | Xuất bản video | Content Author / Administrator | FT-37 | Mục tiêu | — |
| UC-D082 | Xem danh sách video | Người dùng được phép | FT-37 | Mục tiêu | — |
| UC-D083 | Xem video và các segment | Người dùng được phép | FT-37 | Mục tiêu | — |

## Lộ trình và học lesson (14 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D084 | Xem danh sách course kèm tiến độ/đề xuất | Learner | FT-20, FT-35, FT-55 | Mục tiêu | Thông tin recommended và testStatus nằm trong cùng kết quả đọc. |
| UC-D085 | Chọn course đang học lần đầu | Learner | FT-20 | Mục tiêu | — |
| UC-D086 | Đổi course đang học | Learner | FT-20 | Mục tiêu | — |
| UC-D087 | Xem learning path theo course | Learner | FT-20, FT-13 | Mục tiêu | — |
| UC-D088 | Xem danh sách lesson của topic | Learner | FT-20, FT-21, FT-27 | Mục tiêu | — |
| UC-D089 | Mở lesson để học | Learner | FT-21, FT-13 | Mục tiêu | — |
| UC-D090 | Nghe audio trong lesson | Learner | FT-24 | Hỗ trợ | Hành động học trong lesson; transcript theo điều kiện của FT-24. |
| UC-D091 | Hoàn thành lesson không có exercise block | Learner | FT-21 | Mục tiêu | — |
| UC-D092 | Nộp câu trả lời exercise block | Learner | FT-22, FT-23, FT-26 | Hỗ trợ | Thử lại vẫn là UC này. Hint/đáp án/giải thích là nhánh kết quả. |
| UC-D093 | Nộp essay trong lesson để chấm AI | Learner | FT-25, FT-12 | Mục tiêu | — |
| UC-D094 | Nộp essay trong practice đang mở để chấm AI | Learner | FT-25, FT-27, FT-12 | Mục tiêu | — |
| UC-D095 | Xem điểm và nhận xét Writing của mình | Learner | FT-25 | Mục tiêu | — |
| UC-D096 | Đọc sample answer của essay đã đạt | Learner | FT-25 | Hỗ trợ | Chỉ được hiện sau khi essay block đạt pass band. |
| UC-D097 | Xem mastery theo knowledge point | Learner | FT-26 | Mục tiêu | — |

## Practice và review (10 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D098 | Xem danh sách practice set của lesson | Learner | FT-27, FT-13 | Mục tiêu | — |
| UC-D099 | Bắt đầu practice attempt | Learner | FT-27 | Hỗ trợ | — |
| UC-D100 | Tiếp tục practice attempt đang mở | Learner | FT-27 | Mục tiêu | — |
| UC-D101 | Nộp practice attempt | Learner | FT-27, FT-26 | Hỗ trợ | Kết quả gồm lời giải và transcript; clearance tính theo skill. |
| UC-D102 | Đọc kết quả practice đã nộp | Learner | FT-27 | Mục tiêu | — |
| UC-D103 | Xem danh sách review của mình | Learner | FT-28 | Mục tiêu | — |
| UC-D104 | Mở review để nhận set ở stage PRACTICE | Learner | FT-28 | Mục tiêu | — |
| UC-D105 | Đọc lại lý thuyết ở stage THEORY | Learner | FT-28 | Hỗ trợ | — |
| UC-D106 | Nộp quick check của review | Learner | FT-28 | Hỗ trợ | — |
| UC-D107 | Nộp review set | Learner | FT-28, FT-26 | Hỗ trợ | — |

## Bài thi và các thao tác dùng chung (18 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D108 | Yêu cầu giao đề cuối topic | Learner | FT-29 | Hỗ trợ | Assignment của Learning; chưa phải bước bắt đầu Assessment attempt. |
| UC-D109 | Làm bài thi cuối topic | Learner | FT-29, FT-30, FT-31 | Mục tiêu | Dùng các thao tác attempt chung; điều kiện mở bài thuộc FT-29. |
| UC-D110 | Yêu cầu giao đề cuối course | Learner | FT-55 | Hỗ trợ | — |
| UC-D111 | Làm bài thi cuối course | Learner | FT-55, FT-30, FT-31 | Mục tiêu | — |
| UC-D112 | Làm mock test | Learner | FT-34, FT-30, FT-32 | Mục tiêu | — |
| UC-D113 | Làm placement test | Learner | FT-35, FT-30 | Mục tiêu | — |
| UC-D114 | Bắt đầu assessment attempt | Learner | FT-30 | Hỗ trợ | Thao tác dùng chung cho các loại bài thi; không phải loại đề mới. |
| UC-D115 | Đọc cấu trúc attempt của mình | Learner | FT-30 | Hỗ trợ | — |
| UC-D116 | Lưu câu trả lời objective của attempt | Learner | FT-30 | Hỗ trợ | FT-30 mô tả lưu với expected revision. |
| UC-D117 | Nộp Writing response cho attempt item | Learner | FT-30, FT-33 | Hỗ trợ | Submission trước khi hand-in/submit attempt. |
| UC-D118 | Nộp Speaking response cho attempt item | Learner | FT-33 | Hỗ trợ | — |
| UC-D119 | Nộp assessment attempt | Learner | FT-30, FT-31 | Hỗ trợ | — |
| UC-D120 | Xem kết quả assessment của mình | Learner | FT-31 | Mục tiêu | — |
| UC-D121 | Mở section placement | Learner | FT-35 | Hỗ trợ | — |
| UC-D122 | Nộp section placement | Learner | FT-35 | Hỗ trợ | Section đã hand-in không nhận thêm đáp án. |
| UC-D123 | Tiếp tục placement đang làm dở | Learner | FT-35 | Mục tiêu | — |
| UC-D124 | Xem placement report | Learner | FT-35 | Mục tiêu | — |
| UC-D125 | Reset placement của learner | Administrator | FT-35 | Mục tiêu | SRS yêu cầu; current release chưa có reset. |

## Chấm AI và chấm thủ công trong Assessment (8 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D126 | Yêu cầu chấm AI cho submission | Learner | FT-33 | Mục tiêu | — |
| UC-D127 | Xem grading job của mình | Learner | FT-33 | Mục tiêu | — |
| UC-D128 | Yêu cầu thử lại grading job bị lỗi | Learner | FT-33 | Mục tiêu | — |
| UC-D129 | Yêu cầu examiner chấm attempt bằng grading credit | Learner | FT-14, FT-32 | Mục tiêu | Learner flow chưa xây dựng theo Current release của FT-14. |
| UC-D130 | Mở result version để chấm | Examiner / Administrator | FT-32 | Hỗ trợ | — |
| UC-D131 | Lưu grading details của result version | Examiner / Administrator | FT-32 | Hỗ trợ | — |
| UC-D132 | Finalize result version | Examiner / Administrator | FT-32, FT-31 | Mục tiêu | — |
| UC-D133 | Chấm lại attempt đã có kết quả | Examiner / Administrator | FT-32 | Mục tiêu | Mở version mới, thay evidence của version cũ khi finalized. |

## Speaking, shadowing và dictation (9 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D134 | Gửi recording Speaking để đánh giá | Learner | FT-39 | Mục tiêu | — |
| UC-D135 | Xem transcript và điểm Speaking | Learner | FT-39 | Mục tiêu | — |
| UC-D136 | Gửi lại Speaking attempt bị lỗi | Learner | FT-39 | Mục tiêu | — |
| UC-D137 | Tạo shadowing attempt cho segment | Learner | FT-40 | Mục tiêu | — |
| UC-D138 | Nộp recording shadowing | Learner | FT-40, FT-39 | Hỗ trợ | — |
| UC-D139 | Xem điểm shadowing | Learner | FT-40, FT-39 | Mục tiêu | — |
| UC-D140 | Tạo dictation attempt cho segment | Learner | FT-41 | Mục tiêu | — |
| UC-D141 | Nộp nội dung dictation đã gõ | Learner | FT-41 | Hỗ trợ | — |
| UC-D142 | Xem điểm và các từ sai/thiếu của dictation | Learner | FT-41 | Mục tiêu | — |

## Video cá nhân, notes, flashcard và deck (25 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D143 | Lưu tiến độ xem video | Learner | FT-38 | Hỗ trợ | — |
| UC-D144 | Đọc tiến độ xem video của mình | Learner | FT-38 | Mục tiêu | — |
| UC-D145 | Lưu video segment cá nhân | Learner | FT-38 | Mục tiêu | — |
| UC-D146 | Xem danh sách segment đã lưu | Learner | FT-38 | Mục tiêu | — |
| UC-D147 | Xóa segment đã lưu | Learner | FT-38 | Mục tiêu | — |
| UC-D148 | Tạo note | Learner | FT-42 | Mục tiêu | — |
| UC-D149 | Xem danh sách note | Learner | FT-42 | Mục tiêu | — |
| UC-D150 | Đọc note | Learner | FT-42 | Mục tiêu | — |
| UC-D151 | Sửa note | Learner | FT-42 | Mục tiêu | — |
| UC-D152 | Xóa note | Learner | FT-42 | Mục tiêu | — |
| UC-D153 | Tạo flashcard thủ công | Learner | FT-43 | Mục tiêu | — |
| UC-D154 | Tạo flashcard từ vocabulary sense | Learner | FT-43 | Mục tiêu | — |
| UC-D155 | Tạo flashcard từ highlight | Learner | FT-43 | Mục tiêu | — |
| UC-D156 | Lưu practice question thành flashcard | Learner | FT-43 | Mục tiêu | Lưu lại cùng question trả thẻ đã có. |
| UC-D157 | Xem danh sách flashcard | Learner | FT-43 | Mục tiêu | — |
| UC-D158 | Đọc flashcard | Learner | FT-43 | Mục tiêu | — |
| UC-D159 | Sửa flashcard | Learner | FT-43 | Mục tiêu | FT-43 nêu card changeable bởi owner. |
| UC-D160 | Xóa flashcard | Learner | FT-43 | Mục tiêu | SRS Part 4.2, DC38 quy định soft delete. |
| UC-D161 | Tạo deck | Learner | FT-43 | Mục tiêu | — |
| UC-D162 | Xem danh sách deck | Learner | FT-43 | Mục tiêu | — |
| UC-D163 | Sửa deck | Learner | FT-43 | Mục tiêu | FT-43 nêu deck changeable bởi owner. |
| UC-D164 | Xóa deck | Learner | FT-43 | Mục tiêu | SRS Part 4.2, DC38 quy định soft delete. |
| UC-D165 | Thêm card vào deck | Learner | FT-43 | Mục tiêu | — |
| UC-D166 | Xem các card trong deck | Learner | FT-43 | Mục tiêu | — |
| UC-D167 | Gỡ card khỏi deck | Learner | FT-43 | Mục tiêu | — |

## Activity và streak (4 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D168 | Ghi nhận learning activity | Learner | FT-44 | Mục tiêu | — |
| UC-D169 | Xem danh sách learning activity của mình | Learner | FT-44 | Mục tiêu | — |
| UC-D170 | Xóa learning activity của mình | Learner | FT-44 | Mục tiêu | — |
| UC-D171 | Xem study streak của mình | Learner | FT-45 | Mục tiêu | — |

## Game một người, phòng nhiều người và matchmaking (12 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D172 | Bắt đầu game session một người | Learner | FT-46 | Mục tiêu | — |
| UC-D173 | Nộp câu trả lời cho game item | Learner | FT-46 | Hỗ trợ | — |
| UC-D174 | Abandon game session | Learner | FT-46 | Mục tiêu | — |
| UC-D175 | Tạo multiplayer room | Learner | FT-47 | Mục tiêu | — |
| UC-D176 | Tham gia multiplayer room | Learner | FT-47 | Mục tiêu | — |
| UC-D177 | Đánh dấu ready trong room | Learner | FT-47 | Hỗ trợ | — |
| UC-D178 | Rời multiplayer room | Learner | FT-47 | Mục tiêu | — |
| UC-D179 | Bắt đầu multiplayer match | Host | FT-47 | Mục tiêu | — |
| UC-D180 | Đóng multiplayer room | Host | FT-47 | Mục tiêu | — |
| UC-D181 | Kết nối lại multiplayer match sau khi mất kết nối | Learner | FT-47 | Hỗ trợ | — |
| UC-D182 | Xem bảng xếp hạng sau match | Learner | FT-47 | Mục tiêu | — |
| UC-D183 | Tham gia hàng chờ matchmaking | Learner | FT-48 | Mục tiêu | — |

## Community và kiểm duyệt (15 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D184 | Xem community feed | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D185 | Tạo post | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D186 | Sửa post của mình | Tác giả post | FT-49 | Mục tiêu | — |
| UC-D187 | Xóa post của mình | Tác giả post | FT-49 | Mục tiêu | — |
| UC-D188 | Xem danh sách comment của post | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D189 | Tạo comment | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D190 | Reply một comment | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D191 | Sửa comment của mình | Tác giả comment | FT-49 | Mục tiêu | — |
| UC-D192 | Xóa comment của mình | Tác giả comment | FT-49 | Mục tiêu | — |
| UC-D193 | Thêm reaction vào post | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D194 | Gỡ reaction khỏi post | Người dùng đã đăng nhập | FT-49 | Mục tiêu | — |
| UC-D195 | Ẩn post | Administrator | FT-50 | Mục tiêu | — |
| UC-D196 | Khôi phục post HIDDEN | Administrator | FT-50 | Mục tiêu | — |
| UC-D197 | Ẩn comment | Administrator | FT-50 | Mục tiêu | — |
| UC-D198 | Khôi phục comment HIDDEN | Administrator | FT-50 | Mục tiêu | — |

## Thông báo và thiết bị (8 mục)

| Mã nháp | UC cụ thể | Actor | FT | Loại | Ghi chú |
|---|---|---|---|---|---|
| UC-D199 | Xem notification preference | Learner | FT-51 | Mục tiêu | — |
| UC-D200 | Cập nhật notification preference theo channel | Learner | FT-51 | Mục tiêu | — |
| UC-D201 | Đăng ký thiết bị nhận push | Learner | FT-51 | Mục tiêu | Đăng ký lại cùng installation cập nhật token; không tách thêm UC. |
| UC-D202 | Thu hồi thiết bị nhận push | Learner | FT-51 | Mục tiêu | — |
| UC-D203 | Xem danh sách in-app notification | Learner | FT-54 | Mục tiêu | — |
| UC-D204 | Đánh dấu notification đã đọc | Learner | FT-54 | Mục tiêu | — |
| UC-D205 | Nhận grading-done alert | Learner (người nhận) | FT-52 | Tự động | Trigger: examiner finalize kết quả; không gọi Notification Service là actor bên ngoài. |
| UC-D206 | Nhận study reminder | Learner (người nhận) | FT-53 | Tự động | Trigger: đến giờ nhắc và chưa có qualifying activity. |

## Cách sử dụng danh sách

Ưu tiên đặc tả actor, trigger, tiền điều kiện, luồng chính, nhánh lỗi và hậu điều kiện cho từng mục. Sau đó xác định UC hỗ trợ nào cần có đặc tả riêng hoặc quan hệ với UC lớn; không dùng tổng số dòng này để thay trực tiếp số UC trong sơ đồ tổng quan.

Phân quyền, triển khai và các nhánh Draft cần lấy theo FT gốc. Ví dụ: subscription được cấp bằng activation key hoặc Administrator grant; SRS không mô tả learner tự mua/chuyển plan. FT-15 hiện mô tả tạo knowledge point, nên danh sách không tự bổ sung sửa/xóa knowledge point.

Các loại nguồn flashcard được tách để rà thao tác người học; nếu đặc tả dùng một luồng tạo flashcard chung với nhánh nguồn, có thể hợp nhất chúng khi xây mô hình chính thức. Các yêu cầu đọc danh sách và đọc một đối tượng cũng được tách ở mức thao tác.

Hai dòng loại Tự động ghi Learner là người hưởng kết quả. Khi vẽ UML cần thể hiện trigger và ranh giới hệ thống; dịch vụ nội bộ IELTSPath không mặc nhiên trở thành actor bên ngoài.

Đối với yêu cầu retry grading job, FT-33 cho phép retry nhưng chưa nêu rõ actor/trigger của retry. Dòng đó là ứng viên cần chốt trong đặc tả, không phải xác nhận có nút retry của learner.

Phiên bản này chỉ cập nhật danh sách Markdown đề xuất; các mã UC chính thức, SRS Word và RTW chưa được đồng bộ.
