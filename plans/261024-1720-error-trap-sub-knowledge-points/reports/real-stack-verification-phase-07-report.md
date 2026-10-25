# Kiểm thử trên stack thật + nghiệm thu phase 7 (frontend)

Người làm: Claude. Ngày: 2026-10-25.

## Cách chạy
Postgres + RabbitMQ của `docker compose` (cổng 5440), chạy trên máy: config-server (8888), content-service (8082), learning-service (8086);
bỏ eureka và gateway. Flyway tự migrate: content tới V80, learning tới V36. Token nội bộ HS256 (`iss urn:code-base:api-gateway`) tự tạo bằng
`GATEWAY_INTERNAL_JWT_SECRET` trong `.env`. Không đụng Postgres lạ ở cổng 5432 của máy.

## Đã chạy thật và thấy đúng
- Content: 19 KP con, 19 bộ `PS-TRAP-*`, 51 câu T/F/NG có `distractors`.
- Learning làm mới danh mục qua `topic-sequence?includeErrorTraps=true`: catalog từ 0 lên 13 KP con có `parent_kp_id` + `error_code`.
- Học viên mới mở luyện tự do KP `TFNG_FALSE_VS_NOT_GIVEN` → nhận bộ `PS-TRAP-TFNG_FALSE_AS_NG-1`.
- Nộp trả lời chọn `NOT_GIVEN` cho câu đúng `FALSE`: 2 dòng `kp_evidence` `source='error_trap'` (sai), 4 dòng `free_practice`; `question_attempt_events.error_code`
  = `TFNG_FALSE_AS_NG`; hồ sơ KP con tạo ra (INSUFFICIENT, 2 bằng chứng).
- `GET /tree`, `/report`, `/review-basket`, `/suggestions` đều 200; cây không lộ KP con ở danh sách chính; KP cha hiện `errorTraps` của KP con có bằng chứng.

## Lỗi tìm thấy khi chạy thật (test cũ không bắt được) và đã sửa
1. **Học viên không bao giờ thấy `error`/`errorTrap`.** Endpoint Content `/internal/learning-content/error-types` chỉ trả mã `APPROVED`, mà cả 28 mã đều `DRAFT`
   → learning nhận 0 mã → bỏ `error` âm thầm (đúng thiết kế "lỗi Content không làm hỏng lần nộp", nên không có lỗi nào lộ ra). Trái quyết định
   "cho phép DRAFT, đánh dấu". Sửa: `?includeDraft=true` (mặc định giữ nguyên, `EssayGrader` vẫn dùng danh sách đã duyệt); learning thêm
   `LearningContentClient.learnerErrorTypes()` chỉ cho `ErrorTypeTexts`. Test: `InternalLearningContentControllerTest`, `RestLearningContentClientTest`,
   cập nhật `ErrorTypeTextsTest`, `LessonSubmissionIntegrationTest`, `ReviewBasketIntegrationTest`. Chạy lại: câu bẫy trả `error` với tên + `fixHint`.
2. **`errorTraps[].code` ở cây kiến thức là mã KP con (`TRAP_...`)** thay vì mã lỗi như hợp đồng (báo cáo thì đúng). Sửa trong `GetLearnerTreeUseCase`,
   thêm khẳng định vào `FreePracticeIntegrationTest` (đỏ trước, xanh sau).

## Frontend (phase 7)
- Payload thật của lần nộp (kèm `error`) được đưa vào `errorTrapAdapters.test.ts` → adapter map đúng; câu không bẫy không có `error`.
- Chạy: 28 file / 177 test learning-path xanh, typecheck và lint exit 0. Lỗi ngẫu nhiên có sẵn của `owl-reactions.test.ts` không liên quan.
- Chưa bấm thử giao diện trên trình duyệt với backend thật (cần gateway + đăng nhập); hợp đồng JSON đã đối chiếu bằng payload thật.

## Chưa kiểm chứng trực tiếp
- Tạo `ERROR_PATTERN` và `errorTrap` trên bài ôn/giỏ ôn: cần KP con xuống `PRIORITY` (≥ 3 bằng chứng trên ≥ 2 ngày) nên không dựng được thật trong một phiên; được phủ bởi test Testcontainers.
- 13/19 KP con vào danh mục learning: 6 mã còn lại có KP cha nằm ngoài các chủ đề của khóa học đang mở (topic-sequence chỉ trả KP của chủ đề trong khóa). Không phải lỗi; ghi nhận.
- Bộ `PS-TRAP-TFNG_FALSE_AS_NG-1`, câu 4 đúng `FALSE` nhưng chưa gắn `distractors` (bộ vẫn đủ 2 câu bẫy). Cân nhắc gắn khi giám khảo duyệt dữ liệu mẫu.

## Câu hỏi mở
- Có muốn đánh dấu "chưa duyệt" ở UI khi `fixHint` đến từ mã DRAFT không? Hiện chỉ trả nội dung, không có cờ.

## Cập nhật 2026-10-26: giám khảo đã duyệt, bỏ DRAFT
- Theo chủ dự án, giám khảo IELTS duyệt toàn bộ dữ liệu: V81 bỏ tiền tố `Draft practice:` của 19 bộ; V82 chuyển mọi mã lỗi `DRAFT` sang `APPROVED`.
- Vì mã đã duyệt, đường vòng `includeDraft` / `learnerErrorTypes` (lỗi 1 ở trên) được gỡ: học viên lại chỉ thấy mã đã duyệt, đúng nguyên tắc của V48. Endpoint `/error-types` trở về như cũ.
- Hệ quả khác của V82: danh sách mã mà chấm bài viết (AI) và phân tích lỗi của Assessment chấp nhận đổi từ rỗng sang 28 mã; tự điền bẫy T/F/NG khi tác giả lưu câu mới bắt đầu hoạt động.
