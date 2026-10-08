# Bàn giao: SRS v0.9.16–v0.9.19 → RTW (Report 3.1) và FDS (Report 3.2)

SRS đã sửa theo luồng course, lesson nhiều skill và chấm essay trong đề thi (plan
[261007-1934-srs-course-multi-skill-update](../261007-1934-srs-course-multi-skill-update/plan.md)). RTW và FDS chưa sửa.
Danh sách dưới đây là những thứ hai tài liệu đó phải theo.

## Thay đổi định danh trong SRS

| Loại | Thay đổi |
| --- | --- |
| Feature | FT-15 đổi tên → *Courses, topics & knowledge points*; FT-20 → *Course-based learning path*; thêm **FT-55 Course final test** (FE-05, SC-05) |
| Use case | Thêm *View and choose courses* (Learner, nhóm Learning) và *Take course test* «extend» *Take test attempt* (nhóm Assessment); FT-15 Related UC: *Manage courses, topics, …* |
| Business rule | Sửa BR-12…BR-17, BR-22, BR-23, BR-25; thêm BR-35 (course), BR-36 (placement một lần, gợi ý course), BR-37 (thi cuối course), BR-38 (lesson/practice theo skill), BR-39 (essay trong đề thi chấm LLM, fallback EXAMINER), BR-40 (học sau placement, trong course đang học) |
| Data constraint | Sửa DC-01, 15, 17, 18, 28, 31, 32, 36; thêm DC-41…DC-45 |
| Entity | Thêm Course (content), Course Test Assignment, Course Progress, Learner Placement, Current Course (learning); sửa mô tả Topic, Lesson, Content Package, Learner Submission, Grading Job, Topic Progress, Practice Attempt, Writing Submission |
| State | 4.4.3 Topic Progress theo course; thêm 4.4.7 Course Progress (NONE / LOCKED / AVAILABLE / PASSED); 4.4.6 thêm COURSE_TEST và nhánh chấm AI cho essay |
| NFR | NFR-A05, NFR-SEC13 (quota 20 essay/ngày của assessment) |
| Scenario | SC-02 đổi tên *Study a lesson on the course path*; SC-01, 03, 04, 05, 07 sửa nội dung; Scenario List: SC-01 thêm FT-20, SC-04 thêm FT-27, SC-05 thêm FT-55 |

## RTW cần làm

- Sheet 2 (Use Case List): thêm 2 UC trên, gắn FT-20 / FT-55, SC-01 / SC-05.
- Sheet 3 (Feature Traceability): đổi tên FT-15, FT-20; thêm dòng FT-55; cột scenario của FT-20 (SC-01, SC-02), FT-27 (SC-03, SC-04).
- Sheet 4 (Permission Matrix): `GET /api/learning/courses` và `POST /courses/{id}/test-assignments` chỉ CUSTOMER; CRUD course của content cho ADMIN, CONTENT_AUTHOR.
- Sheet 5 (Data Dictionary): 5 entity mới, thuộc tính mới (`topics.course_id`, `content_packages.course_id`, `topic_progress.skills`, `practice_attempts.skills/passed_skills`, `lesson_writing_submissions.source/practice_attempt_id`, `grading_jobs.llm_band`).
- Sheet 6 (Business Rules Register): toàn văn BR-35…BR-40 và bản sửa BR-12…17, 22, 23, 25.
- Sheet 7 (NFR Tracker): NFR-A05, NFR-SEC13.

## FDS cần làm

- Onboarding: thi placement một lần (chặn thi lại, ADMIN reset), sau đó chọn course đang học; chưa thi thì chỉ xem.
- Màn hình learning path: nhóm topic theo course (không theo skill), mỗi course một topic IN_PROGRESS; đổi course đang học.
- Màn hình danh sách course: band level, số topic đã pass, trạng thái course test, nhãn "recommended".
- Luồng thi cuối course: nút nhận mã đề khi course AVAILABLE, kết quả PASSED.
- Lesson: hiển thị skills của lesson; review chặn theo skill.
- Practice: lọc bộ theo skill, trạng thái đạt theo từng skill, nộp essay trong bộ Writing (trừ 3 điểm).
- Đề thi topic/course có essay: nộp essay trước khi nộp bài, kết quả chờ chấm AI.

## Ghi chú

- Diagram SC-05 (sequence) chỉ sửa nhãn; chưa vẽ thêm participant cho bước LLM chấm essay ở assessment và nhánh EXAMINER (đã có trong narrative và FT-31/FT-33).
- `docs/contracts/lesson-learning-v1.md` dòng 63 vẫn ghi lesson có một `skill`, trong khi code trả `skills` (danh sách). Nên sửa contract.

## Câu hỏi chưa chốt

- Không có.
