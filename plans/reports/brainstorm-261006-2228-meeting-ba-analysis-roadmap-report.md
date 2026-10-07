# Brainstorm: phân tích cuộc họp BA/mentor 2026-10-06 và roadmap

- Ngày: 2026-10-06. Nguồn: transcript ghi âm cuộc họp (dán trong chat, không lưu vào repo).
- Đối chiếu với code `main` @ `6351f3a`.
- Phạm vi đã chốt cho đợt này: **(1) test tự động + CI**, **(2) Course theo band + AI sinh đề cho Educator**.
- Ngoài phạm vi (đã ghi lại, làm sau): performance test k6, API tra log thẻ cho sales, bảng exception case, slide business
  case và existing system.

## 1. Tóm tắt cuộc họp

| Chủ đề | Mentor yêu cầu |
| --- | --- |
| Business case | 2 nhóm khách: **mới**, vào học để biết đến thương hiệu trung tâm rồi đăng ký thêm; **cũ**, đang học hoặc chờ lớp, vào để luyện tập duy trì. |
| Premium | Không làm payment hay kế toán. Hệ thống in lô thẻ theo mệnh giá, sales tự bán. Hệ thống chỉ **ghi log ai dùng thẻ nào, lúc nào** để sales tra khi có sự cố. |
| Existing system | Phân tích 2–3 đối thủ, show tài liệu (bản sao đã đổi tên). Slide phải lồng demo kiểu "cái này bọn em làm ở đây". |
| Microservice | Trình bày lợi ích **cho bài toán này**, không đọc lý thuyết. Phải **chứng minh hệ thống đáng dùng microservice**: performance test, CPU/RAM khi có nhiều user. |
| Exception | Đưa swimlane cho AI tìm các nhánh lỗi chưa bắt. Chỉ có happy case thì được khoảng 6–7 điểm. |
| AI | Không phải "gọi API AI". Hiểu tư tưởng của tool (DeepTutor có 2 vai: học viên và Educator), xác định input, dùng cho cá nhân hóa: sinh đề vào kho đề, KP/tag yếu thì giao bài tương tự. |
| TDD / "Jest" | Phân 1 người dựng hệ thống test tự động có AI hỗ trợ, chạy hằng ngày, **log làm bằng chứng**. Báo cáo **cuối tuần này**. |
| Course và band | **Course theo band, lesson không gắn band.** Course là trục chính; lesson gồm lý thuyết, slide và bài tập. Placement xác định bậc hiện tại; target band chỉ là thông tin, học viên vẫn học tăng dần. Kho đề tag theo (band, dạng bài) để lấy bài tương tự nhanh. Lesson đơn skill hay đa skill đều được. |

Tiêu chí chấm theo mentor: 5–7 happy case · 7–8 bài toán hoàn chỉnh · 8–9 công nghệ mới **có chứng minh** + exception
case · ≥9 AI dùng đúng tư tưởng cho adaptive learning.

## 2. Đối chiếu với code

| Ý | Hiện trạng | Khoảng trống |
| --- | --- | --- |
| Thẻ premium | `access-service` đã có `key_products` (POINTS/PREMIUM, mệnh giá), `activation_keys` (hash, status), `key_activations` (`user_id`, `activated_at`, idempotency), API admin tạo lô thẻ và thu hồi thẻ | Thiếu API tra "thẻ X ai dùng, lúc nào"; `SALES_STAFF` chưa có quyền nào (mọi API admin là `hasRole('ADMIN')`) |
| Microservice | 9 service, mỗi service có DB riêng; outbox + RabbitMQ có DLQ; game dùng WebSocket riêng; chấm bằng LLM lỗi thì trả 503 cục bộ | Không có số liệu tải; không có k6/JMeter |
| Exception | Đã có DLQ/retry, replay `requestId`, advisory lock, hạn mức LLM, thẻ dùng lại hoặc đã thu hồi | Chưa liệt kê thành tài liệu |
| AI phía học viên | Mastery theo KP (port từ DeepTutor), LLM chấm Writing, thang ôn tập theo KP (Practice trượt → set cùng KP → lý thuyết → SKIPPED) | Chưa trình bày thành "vòng adaptive" |
| AI phía Educator | Đề do CONTENT_AUTHOR nhập tay | **Chưa có AI sinh đề** |
| Test | Khoảng 165 lớp test JUnit, Mockito, Testcontainers | Không có CI, không có E2E black-box, không có log chạy test |
| Band | `topics.band_min/max` (V5); user có `target_band`, `self_reported_band`; event `AssessmentCompleted.v2` có `assessment_type=PLACEMENT` + `overall_band`; learning đã ghi nhận placement nhưng không dùng | `topic-sequence` **không lọc band**; chưa có thực thể Course; placement không quyết định học viên bắt đầu ở đâu |

Lưu ý: KP thuộc đúng một topic (`knowledge_points.topic_id NOT NULL`), nên thang ôn tập chọn set theo KP là đã
**không lẫn band**, miễn mỗi topic thuộc một course. Ví dụ "cây cối band 5" và "cây cối band 8" là hai topic khác nhau
với KP riêng. Nghĩa là lớp truy xuất đã đúng ý mentor, chỉ thiếu lớp Course.

## 3. Hạng mục 1: test tự động + CI

### Các cách đã cân nhắc

| Cách | Kết luận |
| --- | --- |
| Viết lại test bằng Jest | Loại: Jest là framework JS, backend là Java |
| JUnit + REST-assured + CI | Được, nhưng không khớp chữ "Jest" mentor dùng và không dùng chung được với frontend |
| **JUnit + E2E Jest gọi qua Gateway + CI** | **Chọn.** Unit và integration giữ nguyên trong Maven; E2E black-box viết bằng Jest + axios đúng như mentor nói |

### Thiết kế

- **CI (GitHub Actions)**:
  - Job `unit`: `mvn -B test` toàn reactor; runner ubuntu có Docker nên Testcontainers chạy được. Upload
    `target/surefire-reports` làm artifact.
  - Job `e2e`: `docker compose -f docker-compose.mvp.yml up -d --build`, chờ health rồi chạy Jest. Xuất kết quả bằng
    `--json --outputFile`, upload thành artifact.
- **E2E**: thư mục `e2e/` ở root (Node, Jest + axios), chỉ gọi qua Gateway, không đụng DB. Kịch bản ưu tiên:
  1. đăng ký → đăng nhập → refresh token; 401 khi không có token hoặc token hết hạn;
  2. danh sách topic → lesson → nộp bài tập → hoàn thành lesson; 403/409 khi lesson còn khóa;
  3. Practice trượt → review được tạo → lesson cùng skill bị khóa, lesson skill khác vẫn mở;
  4. kích hoạt thẻ: hợp lệ, dùng lại, đã thu hồi, sai mã;
  5. nộp Writing khi không có cấu hình LLM → **503** (exception case thật, không gọi LLM thật).
- **Quy trình TDD**: mỗi feature mới có test E2E hoặc unit trước rồi mới code; CI đỏ thì không merge.
- **Bằng chứng nộp hội đồng**: lịch sử run Actions + artifact báo cáo + 1 trang tổng hợp (số test, thời gian, lỗi đã bắt).

### Ràng buộc và rủi ro

- AGENTS.md coi CI và tooling mới là quyết định thiết kế: **đã được người dùng duyệt trong buổi này**; cần cập nhật
  AGENTS.md §2 (dòng "Repository chưa có") khi làm.
- Stack MVP gồm 8 module Java + 5 DB, nên job E2E có thể mất 10–15 phút và gần chạm RAM của runner (7 GB): cần giới hạn
  heap JVM, chạy E2E khi push lên `main` hoặc PR, không chạy mỗi commit.
- Seed demo là dữ liệu chung: test E2E phải tạo user mới mỗi lần chạy để không phụ thuộc thứ tự.

## 4. Hạng mục 2a: Course theo band (cách A)

> **Đã thay đổi khi validate (2026-10-06):** mọi course mở, chuỗi theo (course, skill), placement chỉ gợi ý course, có thi cuối course không chặn. Nguồn đúng: `plans/261006-2228-course-band-levels/plan.md`.

### Các cách đã cân nhắc

| Cách | Kết luận |
| --- | --- |
| **A. Bảng `courses` nhóm topic** | **Chọn.** Khớp ngôn ngữ mentor, migration nhỏ, contract chỉ thêm field |
| B. Chỉ lọc `band_min/max` | Loại: không có thực thể Course để trình bày |
| C. Course aggregate riêng, nhân bản lesson | Loại: quá tốn, phá các plan đã hoàn thành |

### Thiết kế

- **Content** (migration mới, không sửa migration cũ):
  - `courses(id, code, name, band_level NUMERIC(2,1), sort_order, status)` và `topics.course_id` (FK).
  - Backfill seed hiện có vào một course mặc định.
  - Giữ cột `band_min/max` (không xóa, tránh thay đổi phá dữ liệu); ghi rõ course là nguồn band chính.
- **Contract `learning-content-internal-v1`**: `topic-sequence` thêm `course {id, code, bandLevel, sortOrder}` cho mỗi
  topic. Chỉ thêm field, consumer cũ không vỡ.
- **Learning**:
  - Bậc bắt đầu = `overall_band` của placement mới nhất (đã nhận qua consumer `AssessmentCompleted.v2`, không cần event
    mới). Chưa thi placement thì dùng mặc định là course thấp nhất.
  - Course hiện tại = course thấp nhất có `band_level` ≥ bậc bắt đầu mà chưa PASS. Course PASS khi mọi topic PASS.
  - Bên trong một course vẫn giữ luật hiện tại: chuỗi theo từng skill, review chỉ khóa cùng skill.
  - Course dưới bậc bắt đầu: mở để tự học, không bắt buộc. Target band chỉ hiển thị.
- **Lesson**: giữ "1 lesson = 1 skill" (mentor xác nhận đa skill hay đơn skill đều được; giữ quyết định D1 của plan
  `261002-1600`).
- **Seed demo**: thêm ít nhất 1 course bậc 2 (ví dụ Reading 6.5) để demo việc lên band và học viên ở band khác nhau
  nhận bài khác nhau.

## 5. Hạng mục 2b: AI sinh đề cho Educator (đòn bẩy để lên trên 9 điểm)

- **Lập luận khi trình bày**: thang ôn tập chuyển review sang `SKIPPED` khi hết package chưa lộ. AI sinh đề nháp theo
  (course/band, KP, dạng câu hỏi), giúp kho đề luôn còn "bài tương tự", từ đó vòng adaptive khép kín. Đúng tư tưởng
  DeepTutor có hai vai: học viên dùng mastery và ôn tập, Educator dùng sinh đề.
- **Thiết kế**:
  - Use case ở content-service (service sở hữu câu hỏi). Port `QuestionDraftGenerator` ở `application/port`, adapter LLM
    tương thích OpenAI ở infrastructure.
  - Input: KP, band của course, dạng câu hỏi, vài câu PUBLISHED cùng KP làm ví dụ few-shot.
  - Output: question version hoặc practice set ở trạng thái **DRAFT**. Phải đúng schema `answer-spec-v1` mới được lưu;
    sai schema thì loại. **Không bao giờ tự publish**: CONTENT_AUTHOR duyệt qua flow publish sẵn có.
  - Hạn mức sinh theo ngày cho mỗi author (tương tự plan 0737). Chỉ log id và mã lỗi, không log prompt hay output.
  - Test dùng LLM giả hoặc `MockRestServiceServer`, không gọi LLM thật.
- **Ràng buộc**: cần thêm biến cấu hình LLM vào `config-repo/content-service.yaml` (sửa config tập trung, thuộc phạm vi
  feature này).

## 6. Roadmap đề xuất

| # | Hạng mục | Hạn | Trạng thái |
| --- | --- | --- | --- |
| 1 | CI chạy `mvn test` + artifact | Cuối tuần này | Trong phạm vi |
| 2 | E2E Jest qua Gateway (5 kịch bản) + job CI | Cuối tuần này, có thể chỉ xong kịch bản 1–2 | Trong phạm vi |
| 3 | Course theo band + placement chọn course bắt đầu + seed course bậc 2 | Sau 1–2 | Trong phạm vi |
| 4 | AI sinh đề DRAFT cho Educator | Sau 3 (cần band của course) | Trong phạm vi |
| 5 | Performance test k6 (ramp 50→500 user ảo, p95, CPU/RAM theo service) | Trước buổi review kiến trúc | **Ngoài phạm vi, rủi ro cao**, xem §7 |
| 6 | API tra log thẻ + quyền `SALES_STAFF` | Bất kỳ lúc nào (nhỏ) | Ngoài phạm vi |
| 7 | Bảng exception case theo luồng + slide business case/existing system | Phía BA | Ngoài phạm vi code |

## 7. Rủi ro

- **Bỏ performance test là rủi ro lớn nhất về điểm**: mentor nói rõ không có số liệu tải thì không chứng minh được
  microservice, tức là mất mức 8–9. Nên đưa k6 vào đợt ngay sau. Đừng hứa con số "10.000 user" trên laptop hay 1 VPS;
  kịch bản đúng là cho thấy service nghẽn rồi scale riêng service đó.
- Course theo band làm tăng khối lượng seed nội dung (mỗi bậc cần topic, KP và đề riêng), đây là nút thắt thật chứ
  không phải code. AI sinh đề giảm được một phần.
- Thời gian CI E2E và RAM của runner (§3).
- Học viên có placement cao hơn course cao nhất, hoặc chưa thi placement: phải có luật rõ ràng (§4).

## 8. Tiêu chí thành công

- Mỗi push lên `main` đều có run Actions xanh kèm artifact báo cáo test; demo được 1 run đỏ do bug thật đã bị bắt.
- 2 học viên có placement khác nhau thấy course bắt đầu khác nhau; review chỉ giao set trong đúng course.
- Author sinh được set nháp hợp lệ `answer-spec-v1`, duyệt xong thì set đó được dùng làm bài ôn.

## 9. Bước tiếp theo

- Hạng mục 1–2 (CI + E2E): `/ck:plan` riêng, ưu tiên ngay.
- Hạng mục 3–4: `/ck:plan --tdd` (sửa luật cổng bài và topic-sequence đang có test).

## Câu hỏi còn mở

1. Course dùng một `band_level` (6.5) hay một khoảng (6.0–6.5)? Ý mentor ("6.5 chỉ dùng cho 6.5") nghiêng về một giá
   trị.
2. Placement test đã có package nội dung chưa, hay phải seed thêm? Nếu chưa có, demo phải dùng mặc định là course thấp
   nhất.
3. Frontend dùng JS/TS không? Nếu có thì nên dùng chung Jest cho E2E UI sau này.
4. Repo org trên GitHub có giới hạn phút Actions hay chặn Docker trên runner không?
5. Course PASS có cần một bài thi cuối course riêng, hay chỉ cần mọi topic PASS?
6. AI sinh đề trong đợt này có cần làm cả Writing/Listening (cần ảnh hoặc audio) hay chỉ Reading và câu hỏi văn bản?
