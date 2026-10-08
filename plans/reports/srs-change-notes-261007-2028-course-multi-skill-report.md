# Ghi chú thay đổi SRS — v0.9.15 → v0.9.21 (07/10/2026)

File: `capstone-docs/rp_3.0_system-requirements-system/Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx`
Nhánh: `doc/srs-course-multi-skill` (chưa commit). Nguồn dữ kiện: code sau 2 plan `course-band-levels` và
`multi-skill-lessons`, contract `lesson-learning-v1`, `assessment-completed-v2`, `answer-spec-v1`.

## Tóm tắt luồng đã đổi

| Trước | Sau |
| --- | --- |
| Mỗi skill có một chuỗi topic riêng; mỗi skill có 1 topic IN_PROGRESS | Topic thuộc **course** theo band level (IELTS 5.5, 6.5…); mỗi course một chuỗi chung mọi skill, mỗi course có 1 topic IN_PROGRESS; mọi course đều mở |
| Lesson thừa hưởng skill của topic | Lesson dạy **skill của các câu hỏi**, có thể gồm Reading, Listening và Writing |
| Practice đạt chung cho cả lesson | Practice đạt **theo từng skill khách quan** (R, L); bộ Writing tùy chọn |
| Review chặn lesson/practice/test cùng skill của lesson | Review lấy **skill của KP**, chặn mọi lesson có skill đó; chỉ KP R/L sinh review |
| Essay chỉ có trong lesson (trả 3 điểm) | Có thêm essay trong **Practice** (cùng giá, cùng quota 10/ngày) |
| Phần trả lời không chấm tự động được trong đề thi → chờ EXAMINER | Essay trong **TOPIC_GATE/COURSE_GATE** được LLM chấm miễn phí (quota 20/ngày); lỗi, chưa cấu hình hoặc hết quota → EXAMINER; essay không nộp = 0 điểm |
| Placement `[TBC]` | Placement lưu band mới nhất, chỉ **gợi ý course**, không đổi trạng thái topic |
| — | **Thi cuối course** (`COURSE_TEST` → `COURSE_GATE`): mở khi mọi topic của course PASSED, đạt ≥ 70% thì course PASSED, không chặn gì |

## Lịch sử phiên bản (Document Change History)

Thêm 4 dòng ngày 07/10/2026: **v0.9.16** (Part 3), **v0.9.17** (Part 5–6), **v0.9.18** (Part 4), **v0.9.19** (Part 0–2 và các hình).

## Part 1 — System Overview

- **1.1 Objective:** "skill-by-skill sequence of topics" đổi thành courses set by band level; thêm câu về thi cuối course và placement gợi ý course.
- **1.2 Technical Scope:**
  - Modules: content có thêm *courses*; learning có thêm *course-based learning path, placement band, topic- and course-test assignment, Writing grading of lesson and practice essays*; assessment có thêm *LLM for essays in topic and course tests*.
  - Integrations: LLM chấm essay của lesson/practice (learning) và của đề thi topic/course (assessment).
  - Data Boundary: thêm courses, placement band, course progress, course-test assignments.
- **1.3 Roles:** Learner thêm *Courses with a recommended course… topic and course final tests*; Content Author thêm *courses*; dòng LLM Provider nhận essay từ lesson, practice và đề thi topic/course.
- **1.4 Glossary:**
  - Thêm 3 mục: **Course**, **Course final test**, **Recommended course**.
  - Sửa: Skill (lesson nhiều skill), Topic (nằm trong một course), Lesson / Lesson block, Pass mark (thêm course test và per-skill practice), Practice set, Topic final test (trộn R/L/W), Test assignment, AssessmentCompleted.v2, Band, Point (essay trong đề thi miễn phí).
- **1.5 Feature bridge:** FE-04 đổi tên FT-15; FE-05 đổi tên FT-20 và thêm FT-55.

## Part 2 — Scenarios

| SC | Thay đổi |
| --- | --- |
| SC-01 | Business flow có thêm bước placement. Post-condition: "one topic in progress in each course", có course được gợi ý. Narrative: Minh Anh ước tính band 5.0, được gợi ý IELTS 5.5, vẫn tự vào course khác được. Related Feature thêm FT-20; Related UC thêm *View courses* |
| SC-02 | Tên đổi thành **"Study a lesson on the course path"**. Pre-condition: review trong skill mà lesson dạy. Narrative: lesson dạy Reading + Listening, có essay tùy chọn không chặn hoàn thành. Exception: chỉ lesson có Reading bị khóa |
| SC-03 | Pre/Post-condition theo từng skill (Writing tùy chọn). Narrative: review chặn lesson có Reading; lesson chỉ Listening/Writing vẫn mở; "Reading practice for the lesson is cleared" |
| SC-04 | Essay trong lesson hoặc practice set; bỏ câu "Writing topic không có final test"; thêm alternate flow *Essay in a practice set*. Related Feature thêm FT-27, UC thêm *Do lesson practice* |
| SC-05 | Business flow thêm thi cuối course. Đề thi có Reading, Listening và một essay Task 2 do LLM chấm. "Next topic of the course opens". Exception: essay LLM không chấm được → EXAMINER, essay không nộp = 0. Alternate flow mới: thi cuối course. Related Feature thêm FT-55, UC thêm *Take course test* |
| SC-07 | Topic thuộc course: "creates the topic in the IELTS 5.5 course…", "available… in the IELTS 5.5 course sequence" |
| Scenario List | SC-01 thêm FT-20; SC-02 đổi tên; SC-04 thêm FT-27; SC-05 thêm FT-55 |

## Part 3 — Features

| FT | Thay đổi chính |
| --- | --- |
| FT-15 | **Đổi tên: Courses, topics & knowledge points.** CRUD course (code ≤ 100, name ≤ 255, band 0–9 bước 0.5 không trùng, ACTIVE/INACTIVE). Topic có course tùy chọn; chỉ topic của course ACTIVE vào lộ trình. AC-04, NAC-04 (409 trùng, 400 band sai, 404 course lạ), BV-03 mới |
| FT-17 | Thêm type `COURSE_TEST`. Practice set chỉ chứa câu thuộc skill lesson dạy. Essay trong practice/topic test/course test bắt buộc có `passBand`. "Current release": TOPIC_TEST và COURSE_TEST chỉ từ seed. AC-03, NAC-05 mới; NAC-03 sửa |
| FT-19 | Bỏ "lesson inherits the topic's skill"; lesson dạy skill của câu hỏi (chưa có Speaking). Essay block có pass band 4.0–9.0. NAC-02 và BV-02 mới |
| FT-20 | **Đổi tên: Course-based learning path.** Thứ tự band level → FREE trước PREMIUM → curriculum. Trạng thái tính theo course. Danh sách course có `recommended` và `testStatus`. AC-01..03 viết lại, AC-04 (gợi ý), NAC-04 (chỉ CUSTOMER), BV-01 theo course, BV-02 (gợi ý). Related Scenario: SC-01, SC-02 |
| FT-21 | Gate review theo "any skill the lesson teaches". Essay không tính hoàn thành. AC-03 sửa thành "topic without a final test"; thêm AC-04 (review Reading không chặn lesson chỉ Listening) |
| FT-25 | Áp dụng cho essay trong practice; essay practice không tính vào clearance. NAC-05 sửa, NAC-06 mới (`ATTEMPT_SUBMITTED`, `NOT_ESSAY_ITEM`) |
| FT-26 | Evidence có thêm essay trong practice attempt; review sau kết quả thi có cả FT-55 |
| FT-27 | Viết lại: lọc bộ theo skill, chấm điểm theo từng skill, đạt theo từng skill khách quan, reason yếu nhất, Writing tùy chọn, review chỉ cho KP R/L. Thêm AC-05, AC-06; BV theo skill. Related Scenario: SC-03, SC-04 |
| FT-28 | Review lấy skill của KP, chặn mọi lesson có skill đó |
| FT-29 | Gate theo skill của topic; đề trộn R/L/W; "next topic of the course" |
| FT-30 | Thêm `COURSE_TEST → COURSE_GATE`; essay nộp như learner submission trước khi nộp bài. AC-03 mới |
| FT-31 | Chấm essay trong đề thi: job AI miễn phí, đạt `passBand` thì đủ điểm, không đạt hoặc không nộp thì 0, lỗi thì EXAMINER. AC-04, AC-05, BV-03 mới |
| FT-32 | Thêm nguồn việc: essay trong đề thi mà AI không chấm được |
| FT-33 | Worker đã chấm essay của TOPIC_GATE/COURSE_GATE (poll 5 s, quota 20/ngày giờ VN, kẹt 10 phút thì chạy lại, lỗi thì chuyển job HUMAN, không trừ điểm). Sửa "Current release"; thêm AC-03, AC-04, NAC-04, BV-01 |
| FT-35 | Gỡ `[TBC]`: lưu placement band mới nhất, gợi ý course, không đổi topic. Bỏ giới hạn "one-time"; NAC-02 và BV-01 viết lại |
| **FT-55** | **Mới — Course final test.** Điều kiện mọi topic PASSED, xoay vòng đề, đạt ≥ 70% thì course PASSED, không chặn topic/course. NAC: `COURSE_TEST_LOCKED`, `NO_COURSE_TEST`, `TEST_UNAVAILABLE`, `COURSE_ALREADY_PASSED`, `COURSE_NOT_FOUND`, 403 role |
| Feature List | Đổi tên FT-15, FT-20; thêm dòng FT-55; cột scenario FT-20 (SC-01, SC-02), FT-27 (SC-03, SC-04) |

## Part 4 — Data

- **4.1 Entities:**
  - Thêm **Course** (content) và **Course Test Assignment**, **Course Progress**, **Learner Placement** (learning).
  - Sửa mô tả: Topic, Lesson, Content Package, Learner Submission, Grading Job, Topic Progress, Writing Submission (lesson block hoặc practice), Practice Attempt.
- **4.2 Data constraints:**
  - Sửa DC-01, DC-15 (pass band essay), DC-17 (pass band essay block), DC-18 (topic thuộc ≤ 1 course), DC-28, DC-31, DC-32 (course passed là one-way), DC-36.
  - Thêm **DC-41** (course unique code/band), **DC-42** (placement band), **DC-43** (quota essay đề thi), **DC-44** (skills của practice attempt, nguồn của writing submission).
- **4.3 Retention:** dòng learning progress thêm course progress, placement band, course test assignment; dòng essay ghi rõ lesson và practice; dòng curriculum thêm courses.
- **4.4 State tables:**
  - Intro đổi từ "Six" thành "Seven entities".
  - 4.4.3 Topic Progress: chuỗi theo course.
  - 4.4.4 Review Item: KP R/L, chặn lesson có skill đó.
  - 4.4.5 Writing Submission: thêm nguồn practice.
  - 4.4.6 Attempt: thêm COURSE_TEST, thêm 2 dòng (job AI xong thì COMPLETED; job AI lỗi thì chờ examiner).
  - **4.4.7 Course Progress (mới):** NONE / LOCKED / AVAILABLE / PASSED, kèm bảng valid/invalid và hình.

## Part 5 — Business Rules

- **Sửa:**
  - BR-12: topic khai báo ≤ 1 skill, lesson có thể nhiều skill.
  - BR-13, BR-14: thêm course test.
  - BR-15: pass mark cho course test và theo từng skill của practice.
  - BR-16: chuỗi theo course.
  - BR-17: review chặn theo skill của KP.
  - BR-22: mã đề topic hoặc course.
  - BR-23, BR-25: cột Applied in thêm FT-55.
- **Thêm:**
  - **BR-35:** course theo band, mọi course mở.
  - **BR-36:** placement chỉ gợi ý.
  - **BR-37:** thi cuối course.
  - **BR-38:** lesson/practice theo skill, essay không chặn.
  - **BR-39:** essay trong đề thi chấm LLM, quota 20, fallback EXAMINER.

## Part 6 — NFR

- **NFR-A05:** LLM hỏng thì essay lesson/practice trả 503, essay trong đề thi chuyển EXAMINER.
- **NFR-SEC13:** thêm quota 20 essay đề thi/learner/ngày.

## Hình

| Hình | Thay đổi | File nguồn |
| --- | --- | --- |
| 1.3 Use Case | Thêm *View courses* (Learning) và *Take course test* «extend» *Take test attempt* | `part2-diagrams/IELTSPath_UseCaseDiagram.drawio.xml` + `.png` |
| SC-01, 02, 03, 07 | Đổi nhãn (course, skill của lesson/topic); PNG export lại bằng draw.io Desktop | `part2-diagrams/IELTSPath_Scenarios.drawio` + 4 PNG |
| SC-05 | **Vẽ lại toàn trang**: thêm lifeline LLM Provider; nộp essay cùng bài; khung alt [LLM chấm → band → đủ điểm hoặc 0] / [LLM lỗi, chưa cấu hình, hết quota → EXAMINER chấm → alert]; next topic **of the course** | trang `SC-05 Sequence` + `IELTSPath_SC-05_SequenceDiagram.png` |
| 4.1b ERD | Thêm Course; Topic `FK course_id`; Content Package `FK course_id (course test)` | `part4-diagrams/IELTSPath_ERD.drawio`, `ERD_2.png` |
| 4.1c ERD | Thêm Course Test Assignment, Course Progress, Learner Placement; Topic Progress `course_id, skills`; Practice Attempt `skills, passed_skills`; liên kết Practice Attempt → Writing Submission | `ERD_3.png` |
| 4.4.3, 4.4.6 | Nhãn theo course; "or essays AI-graded" | `IELTSPath_StateMachines.drawio`, `StateMachine_3/6.png` |
| 4.4.7 | Mới: Course Progress | `StateMachine_7.png` (page 7) |

## v0.9.20 — điền các chỗ [TBC] (bạn chốt 07/10/2026)

| Chỗ | Giá trị đã điền |
| --- | --- |
| FT-05 OAuth | Lần đầu: liên kết vào tài khoản có email đã được provider xác minh; chưa có thì tạo CUSTOMER |
| FT-18 Media | Audio ≤ 20 MB, ảnh ≤ 5 MB |
| FT-33 AI grading (ngoài đề topic/course) | 3 điểm, trừ một lần khi chấm xong; 4.4.6 dòng PROCESSING ghi rõ |
| FT-34 / BR-33 Mock | Writing và Speaking do EXAMINER chấm; band theo bảng IELTS Academic chính thức (L/R quy về 40 câu) |
| FT-39 Speaking | Bản ghi 10–120 s, trả điểm trong 30 s |
| FT-41 Dictation | % từ đúng, bỏ qua hoa thường và dấu câu |
| FT-48 Matchmaking | 2 người/trận, chờ tối đa 60 s |
| FT-51 / FT-52 / FT-54 | Kênh PUSH, EMAIL, IN_APP; alert trong 5 phút; thử lại tối đa 3 lần |
| 4.3 Retention | Notification giữ 90 ngày; outbox/DLQ 30 ngày (Team policy) |
| NFR-S02 | 50 phòng, 100 người chơi/instance (MVP target) |
| NFR-SEC01 | TLS kết thúc ở reverse proxy / load balancer trước Gateway |
| NFR-C01 | Job đóng tài khoản phải xong trước lần deploy production đầu |
| AC/NAC | Gỡ 24 dấu `[TBC]` ở đầu AC/NAC; trạng thái "chưa làm" vẫn ghi ở Status Draft và câu Current release |

Còn lại 3 lần chữ `[TBC]`, đều là câu giải thích quy ước (Part 3, Part 6) và dòng lịch sử v0.9.20, không còn giá trị trống.

## v0.9.21 — placement một lần, chọn course đang học (bạn chốt 07/10/2026)

| Chỗ | Thay đổi |
| --- | --- |
| Luồng | Learner thi placement **một lần** (ADMIN reset được để thi thêm một lần) → có kết quả mới được học → **chọn tự do một course đang học** trong các course ACTIVE (course gợi ý được đánh dấu) → đổi course bất cứ lúc nào, giữ tiến độ. Chưa thi: xem được lộ trình nhưng không học |
| FT-20 | Thêm việc chọn và đổi course đang học; AC-05, AC-06; NAC-05 `PLACEMENT_REQUIRED`, NAC-06 `COURSE_NOT_FOUND`; UC đổi tên **View and choose courses**; status `Specified (course choice: Draft)` |
| FT-21 / 27 / 29 / 55 | Cổng mới: cần đã có kết quả placement và chỉ học trong course đang học (`PLACEMENT_REQUIRED`, `COURSE_NOT_CURRENT`); mỗi FT có câu "Current release: not enforced" và status `Specified (... gate: Draft)` |
| FT-35 | Thi một lần; lần hai không có reset → 409 `PLACEMENT_ALREADY_TAKEN`; reset chỉ ADMIN (NAC-03); AC-03 và BV-01 viết lại; Current release ghi rõ code chưa chặn |
| BR | BR-35 bỏ "every course is open"; BR-36 viết lại (thi một lần, reset bởi ADMIN); **BR-40 mới** (học sau placement, trong một course đang học, đổi được) |
| Part 4 | Entity **Current Course**; Learner Placement ghi "one-time, reset by an Administrator"; DC-42 sửa; **DC-45 mới**; retention có current course |
| Glossary | Course sửa; thêm **Current course**, **Placement test** |
| Part 1 / 2 | 1.1 và vai trò Learner; SC-01 (narrative, post-condition, 2 exception mới); SC-02, SC-05 pre-condition "current course" |
| Hình | SC-01 thêm luồng thi placement → chọn course; sửa id trùng làm mất mũi tên luồng quên mật khẩu; ngắt dòng nhãn dài ở SC-01/03/07 (draw.io 31 không tự xuống dòng); 1.3 đổi tên UC; 4.1c thêm Current Course |

**Lệch với code (cần sửa code sau):** code hiện cho thi placement nhiều lần, mọi course mở, không có "course đang học" (quyết định D4 cũ). SRS ghi các phần này là Draft trong "Current release". Contract `lesson-learning-v1.md` ("Every course is open") cũng phải sửa khi làm code.

## Cần bạn xem kỹ

1. **FT-15 đổi tên** và việc thêm CRUD course: code có API admin course nên tôi đưa vào; nếu muốn giữ tên cũ thì báo lại.
2. **FT-35**: đã chốt thi một lần (v0.9.21); code chưa chặn.
3. **SC-05 sequence diagram** đã vẽ lại đầy đủ (v0.9.20).
4. Năm sơ đồ scenario đã export lại bằng draw.io Desktop (CLI), không còn ảnh vá chữ.
5. RTW và FDS chưa sửa; danh sách việc ở [srs-to-rtw-fds-handoff-261007-1934-course-multi-skill-report.md](srs-to-rtw-fds-handoff-261007-1934-course-multi-skill-report.md).
