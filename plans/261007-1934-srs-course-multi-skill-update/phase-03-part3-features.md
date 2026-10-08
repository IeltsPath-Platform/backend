# Phase 3 — Part 3: Feature Description

## Context

- SRS Part 3 có FT-01..FT-54 (dòng text trích: FT-17 ~1222, FT-19 ~1292, FT-20 ~1324, FT-21 ~1358, FT-25 ~1495,
  FT-27 ~1592, FT-28 ~1636, FT-29 ~1683, FT-30 ~1723, FT-31 ~1758, FT-32 ~1795, FT-33 ~1837, FT-35 ~1900) và Feature
  List cuối Part 3.
- Nguồn: `docs/contracts/lesson-learning-v1.md` (`GET /courses`, `POST /courses/{id}/test-assignments`, practice,
  review, topic test), `assessment-completed-v2.md` (essay trong gate, `COURSE_GATE`), `answer-spec-v1.md`
  (`passBand`), controller ở `services/learning-service/.../api/controller/` (`CourseController`,
  `PracticeController` có `POST /practice-attempts/{id}/essays/{questionVersionId}/submissions`).
- Domain cần kiểm trước khi viết: `LessonAccessGate`, `PracticeClearance`, `ReviewRule`, `LearnerCurriculum`,
  `TopicProgress` (learning); use case chấm essay gate trong assessment.

## Requirements

Mỗi FT giữ đủ khối template: Source Feature, Related Scenario, Related UC, Summary, System Behaviour, BR, AC, NAC, BV.
Câu "Current release:" chỉ sửa khi trạng thái thật đã đổi.

## Sửa từng feature

| FT | Sửa gì |
| --- | --- |
| FT-15 Topics & KPs | Topic thuộc một course (nullable; topic không có course ACTIVE thì không vào lộ trình). Xem lại NAC-01 `TOPIC_SKILL_LOCKED`, kiểm code xem còn hay không. |
| FT-17 Packages | Thêm type `COURSE_TEST`, gắn một course. Essay trong `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST` bắt buộc có `passBand`. Practice set chứa câu thuộc tập skill của lesson (bỏ luật "cùng skill với topic"). Purpose của câu: `COURSE_TEST` dùng câu LEARNING. |
| FT-19 Lesson authoring | Bỏ "lesson inherits the topic's skill". Skill của lesson suy ra từ câu hỏi trong các block, nên một lesson có thể có nhiều skill (R, L, W; Speaking chưa có). |
| FT-20 → đổi tên **"Course-based learning path"** | Viết lại: các topic được nhóm theo course. Thứ tự: `bandLevel → FREE trước PREMIUM → sortOrder → topicId`. Mỗi course có đúng một topic IN_PROGRESS, mọi course đều mở. Thêm `GET /courses` (chỉ CUSTOMER) trả `topicCount`, `passedTopicCount`, `recommended`, `testStatus`. Sửa AC-01..03, BV-01 theo course. Thêm NAC 403 cho role khác CUSTOMER khi gọi `/courses`. |
| FT-21 Lesson gates | Gate 1: không có review chờ ở **bất kỳ skill nào của lesson**. Lesson hoàn thành khi mọi block khách quan (R, L) đạt, essay không chặn. AC-03 "Writing topic (no final test)" phải kiểm lại với seed và code. |
| FT-22 Exercise grading | Kiểm lại xem evidence có ghi theo skill của KP không; chỉ sửa khi lệch. |
| FT-25 Writing grading | Phạm vi mở rộng sang essay trong Practice (`/practice-attempts/{id}/essays/...`): cùng giá 3 điểm, cùng hạn mức 10 lượt/ngày, cùng request id. Essay trong Practice không tính vào việc Practice PASSED. |
| FT-26 Mastery | Essay trong Practice ghi evidence cho KP Writing. |
| FT-27 Lesson practice | Có bộ riêng từng skill và bộ trộn, lọc bằng `?skill=`. Mỗi lần nộp được chấm điểm theo từng skill. Practice PASSED khi mỗi skill khách quan có trong Practice của lesson đạt ≥ 70% ở lần nộp đầu. Review chỉ mở cho KP Reading/Listening. Viết lại AC/BV theo từng skill. |
| FT-28 Review ladder | Skill của review = skill của KP. Review chờ khóa mọi lesson có skill đó, cùng Practice và thi topic của các lesson ấy; lesson không có skill đó vẫn học được. |
| FT-29 Topic final test | Đề trộn R+L+W. Điều kiện: không có review chờ ở các skill của topic. Đạt thì mở topic kế tiếp **trong course** (sửa AC-02 "next topic of the same skill"). |
| FT-30 Attempts | Thêm `COURSE_TEST → COURSE_GATE`. Essay trong attempt được nộp qua `POST /api/assessments/submissions`. |
| FT-31 Auto-grading | Attempt gate có essay: nếu essay đã có submission thì tạo job AI, essay chưa nộp được 0 điểm. Essay đạt khi band ≥ `passBand`, khi đó được đủ điểm câu, chưa đạt thì 0. Ngưỡng chung vẫn 70%. Result chỉ final sau khi essay được chấm. |
| FT-32 Examiner | Thêm nguồn việc: essay gate chuyển sang EXAMINER khi LLM lỗi, chưa cấu hình hoặc hết quota. |
| FT-33 AI grading jobs | Câu "Current release" sửa: worker đã chấm Writing cho `TOPIC_GATE`/`COURSE_GATE`, miễn phí, quota `ASSESSMENT_LLM_DAILY_LIMIT` (mặc định 20). `MOCK` vẫn do EXAMINER chấm. |
| FT-35 Placement | Gỡ `[TBC]`: lưu `overall_band` mới nhất (khác null). Course gợi ý là course thấp nhất có `bandLevel` ≥ band; không có thì lấy course cao nhất. Không đổi trạng thái topic. Sửa "Current release" (learning đã đọc band). Kiểm lại NAC-02 `[TBC]`. |
| **FT-55 Course final test** (mới) | `POST /courses/{id}/test-assignments`, chỉ CUSTOMER. Mở khi mọi topic của course PASSED, đề trộn R/L/W, đạt ≥ 70% thì course PASSED. Không chặn topic hay course nào. NAC: `403 COURSE_TEST_LOCKED`, `409 NO_COURSE_TEST`, `409 COURSE_ALREADY_PASSED`, `404 COURSE_NOT_FOUND`, `409 TEST_UNAVAILABLE`. BV: 69.9% / 70.0%. Source V&S: FE-05/FE-06 (kiểm bridge). |
| Feature List | Đổi tên FT-20, thêm FT-55. |

## Steps

1. Đọc code/contract cho từng dòng trên, ghi lại mã lỗi và con số thật.
2. Viết script python-docx sửa từng FT tại chỗ. FT-55 chèn sau FT-54 bằng cách clone khối của FT-29 để giữ style.
3. Thêm revision history v0.9.16.
4. Xuất text, grep các cụm "per skill", "of the skill", "inherits the topic's skill", "TBC: uses it".

## Validation

- Text trích ra của Part 3 không còn cụm cũ; mỗi mã lỗi mới có trong code (`grep` theo tên).

## Câu hỏi chưa chốt (cần user)

1. Course test thành FT-55 riêng, hay gộp vào FT-29 thành "Topic & course final tests"? Plan đang đề xuất FT-55, vì
   cách này không phải đánh số lại và RTW chỉ cần thêm một dòng.
2. Placement có cần FT riêng không? Plan đang đề xuất không: gợi ý course nằm trong FT-35 và FT-20.
