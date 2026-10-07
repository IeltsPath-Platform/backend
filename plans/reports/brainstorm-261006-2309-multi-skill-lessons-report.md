# Brainstorm: lesson nhiều skill (Reading, Listening, Writing; Speaking làm sau)

- Ngày: 2026-10-06. Đối chiếu với code `main` @ `6351f3a`.
- Làm **sau** plan `261006-2228-course-band-levels`. Plan đó đã được sửa sang chuỗi theo course (xem §5).
- Nguồn quyết định: chủ dự án chốt trong phiên brainstorm 2026-10-06.

## 1. Vấn đề

Chủ dự án muốn 1 lesson dạy nhiều skill, giống một unit trên lớp: lý thuyết, bài tập Reading, Listening, Writing
cùng một chủ đề. Code hiện tại ép **1 lesson = 1 skill**:
- content V15 `topics.skill`; lesson kế thừa skill của topic; publish kiểm câu hỏi cùng skill với topic;
- learning `LessonAccess`, `PracticeAccess`, `StartPracticeAttemptUseCase` dùng skill của topic hoặc lesson;
  `PracticeAttempt` lưu một skill; `LessonAccessGate` chặn bài theo skill.

Những gì đã sẵn:
- lesson tự hoàn thành khi mọi block bài tập đạt, nên "xong mọi skill thì xong lesson" có sẵn;
- lý thuyết gắn theo KP (`lesson_block_knowledge_points` V16 + `TheoryBlockSelector`), KP có skill riêng, nên bài ôn chỉ
  hiện lý thuyết đúng skill bị sai;
- thang ôn tập chạy theo KP.

## 2. Quyết định đã chốt

> **Đã thay đổi khi validate plan (2026-10-06):** Writing không chặn tiến độ. Lesson xong khi R+L đạt; bộ W trong Practice là tự chọn; Writing bắt buộc chỉ ở thi topic. Nguồn đúng: `plans/261006-2309-multi-skill-lessons/plan.md` (M2, M5, M6, M14).

| # | Quyết định | Phương án bị loại |
| --- | --- | --- |
| M1 | Lesson chứa R + L + W; **Speaking làm sau** | Làm Speaking ngay (chưa có lưu audio, chưa có chấm) |
| M2 | Lesson hoàn thành khi mọi block bài tập (mọi skill) đạt | — |
| M3 | **Chuỗi theo course**: trong một course, topic đi lần lượt theo `sortOrder`; mọi course mở. Áp dụng cả cho topic một skill cũ | Chuỗi riêng cho topic một skill (luật rối) |
| M4 | Review chờ ở skill X **khóa cả lesson có skill X** (cùng Practice và thi topic của lesson đó); lesson không có X vẫn học được | Chỉ khóa phần X trong lesson (phải gác cổng theo block) |
| M5 | Practice: **author soạn sẵn** bộ riêng (chỉ R, chỉ L, chỉ W) và bộ trộn; lọc theo skill; mỗi lần nộp tính điểm theo từng skill; Practice của lesson xong khi **mỗi skill của lesson ≥ 70%** (ở bộ riêng hay bộ trộn) | Hệ thống tự ghép đề từ kho (use case ghép, chống lặp, lưu đề) |
| M6 | **Writing có trong Practice**: LLM chấm, **trừ điểm + hạn mức ngày**, dùng lại `EssayGrader` và luật trừ điểm của bài luận lesson | Practice chỉ R+L |
| M7 | Đề thi topic **trộn R + L + W** | Đề chỉ R+L |
| M8 | Writing trong thi topic (`TOPIC_GATE`, `COURSE_GATE`): **LLM chấm trong assessment, không trừ điểm, có hạn mức ngày**. Thi thử (`MOCK`) vẫn EXAMINER chấm | EXAMINER chấm mọi bài thi; đề chỉ R+L |
| M9 | LLM lỗi hoặc hết hạn mức khi chấm thi topic → attempt chuyển hàng chờ EXAMINER, không kẹt | Báo lỗi cho học viên, bắt thi lại |

## 3. Thiết kế

### Content
- Skill của lesson/topic **suy ra** từ KP + câu hỏi trong block. `topics.skill` để nullable, không dùng cho lộ trình
  (giữ cột, không xóa).
- `topic-sequence` trả `skills: [...]` của topic; điều kiện vào lộ trình là có lesson PUBLISHED và có course (không
  còn yêu cầu `skill IS NOT NULL`).
- Bỏ luật publish "câu lesson / Practice phải cùng skill với topic". Thay bằng: câu Practice phải thuộc skill có trong
  lesson.
- Practice set trả `skills: [...]` (suy ra từ câu hỏi). `GET .../lessons/{id}/practice-sets?skill=READING` lọc set
  **chỉ có** skill đó; không truyền thì trả tất cả.
- `TOPIC_TEST` cho phép essay (purpose `LEARNING`).

### Learning
- `TopicStatusDeriver`: khóa chuỗi = `courseId` (đã đưa vào plan Course).
- Gác cổng: review chờ có `skill ∈ lesson.skills` thì chặn lesson, Practice, thi topic. `LessonAccessGate` nhận
  `Set<LearningSkill>` thay cho một skill.
- `PracticeAttempt`: thay `skill` bằng `skills`; kết quả nộp có `skillScores {skill: percent}`.
- `PracticeClearance`: PASSED khi với **mỗi** skill của lesson, có một attempt lần nộp đầu đạt ≥ 70% phần câu của skill
  đó (hoặc skill đó đã đi hết thang ôn, hoặc lesson không có set nào chứa skill đó). Giữ các lý do PASS cũ theo từng skill.
- Essay trong Practice:
  - phần khách quan chấm ngay;
  - essay chấm bằng `EssayGrader` (đồng bộ, như bài luận lesson), trừ điểm qua `AccessClient`, kiểm `LlmUsageQuota`;
  - thiếu điểm → 402/409 như bài luận lesson; LLM lỗi → 503, attempt chưa nộp, nộp lại được;
  - band essay ≥ `passBand` của câu thì tính là đúng.
- Thang ôn tập: không đổi (theo KP). Review sinh ra mang skill của KP.

### Assessment
- `AutoGradeAttemptService`: với `TOPIC_GATE`/`COURSE_GATE` có essay → chấm phần khách quan + gọi port
  `EssayGradingPort` (LLM) cho từng essay, có `LlmUsageQuota` theo ngày, **không trừ điểm**. Đủ điểm thì hoàn thành
  result và phát outbox như hiện tại.
- LLM lỗi hoặc hết hạn mức → attempt ở trạng thái chờ chấm tay; EXAMINER chấm qua `GradingController` sẵn có.
- `MOCK`: không đổi.
- LLM nằm trong assessment (port + adapter, prompt band descriptors chép từ learning). Lý do không gọi learning:
  learning nhận event của assessment, gọi ngược lại tạo phụ thuộc vòng; `shared/` không chứa logic nghiệp vụ.
- Test dùng LLM giả hoặc `MockRestServiceServer` (AGENTS §5), không log essay hay prompt.

### Seed
1 topic nhiều skill trong course 5.5: 2 lesson R+L+W; Practice gồm bộ R, bộ L, bộ W và bộ R+L; 1 `TOPIC_TEST` trộn có
1 essay.

## 4. Rủi ro

- **Đổi luật chuỗi**: topic Reading và Listening cũ cùng course không còn học song song được. Đây là chủ ý (M3); phải
  cập nhật test `TopicStatusDeriverTest` và test integration của plan `261002-1600`.
- **Practice clearance theo skill**: đụng `PracticeClearance`, `PracticeProgress`, `lesson_practice_passes`, nhiều test
  của thang ôn tập. Bắt buộc `--tdd`.
- **Assessment có LLM**: thêm cấu hình, hạn mức, chi phí. Cần biến `.env` mới (tên biến, không ghi giá trị).
- **Essay trong Practice trừ điểm**: học viên hết điểm thì Practice có essay không xong được, nên lesson có Writing chặn
  thi topic. Cần thông báo rõ (409 `INSUFFICIENT_POINTS` sẵn có).
- **Nội dung seed** nhiều skill tốn công soạn (3 skill × 2 lesson + 4 bộ Practice + đề thi).

## 5. Ảnh hưởng tới plan Course (đã sửa)

Plan `261006-2228-course-band-levels`: D5 đổi từ chuỗi (course, skill) sang **chuỗi theo course**; D7 thứ tự
`course.band_level → miễn phí/premium → sortOrder → topicId`; ví dụ và acceptance criteria cập nhật; luật review chặn
theo skill (D6) giữ nguyên ở plan Course (topic vẫn một skill), plan multi-skill đổi sang "lesson chứa skill".

## 6. Tiêu chí thành công

- Lesson R+L+W hoàn thành chỉ khi cả 3 phần đạt.
- `?skill=READING` chỉ trả bộ thuần Reading. Đạt bộ trộn R+L cộng bộ W thì Practice của lesson PASSED.
- Review Listening đang chờ chặn lesson có Listening, không chặn lesson chỉ Reading.
- Thi topic có essay: LLM giả trả band → event `TOPIC_GATE` phát. LLM lỗi → attempt chờ EXAMINER; EXAMINER chấm xong →
  event phát.
- Mọi test hiện có xanh sau khi cập nhật luật chuỗi.

## 7. Bước tiếp theo

1. Cook plan Course (đã sửa).
2. `/ck:plan --tdd` cho multi-skill, dùng báo cáo này.

## Câu hỏi mở

1. `passBand` của essay trong Practice và thi topic lấy từ `answer_spec.passBand` (như lesson) hay một ngưỡng chung?
2. Essay đã nộp trong Practice có tính vào thang ôn tập (KP Writing) như câu khách quan không?
3. Hạn mức LLM của assessment dùng chung con số với learning hay riêng?
4. Speaking: plan riêng (lưu audio cần object storage, đây là quyết định hạ tầng chưa được duyệt).
