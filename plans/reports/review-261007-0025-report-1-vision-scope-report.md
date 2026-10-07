# Review Report 1 (Vision & Scope): cần sửa gì

> **Đã áp vào docx (v1.1.0, 2026-10-07).** Chủ dự án chốt app mobile, cộng đồng và flashcard **trong phạm vi**
> (FE-07, FE-08, FE-09); bảng loại trừ thêm LI-05 (dashboard giáo viên). File docx là nguồn đúng; các ô `[...]` vẫn chờ
> số liệu IELTS Space.

- File: `capstone-docs/rp_1_vision-scope/Report 1_Vision_Scope_IeltsPath.docx` (v1.0.0, tạo 9/9/2026).
- Đối chiếu với: template `doc/Report 1_Vision_Scope_Template.docx`, góp ý mentor buổi họp 2026-10-06
  ([báo cáo họp](brainstorm-261006-2228-meeting-ba-analysis-roadmap-report.md)), code `main` @ `6351f3a`, các plan
  `261006-2228-course-band-levels` và `261006-2309-multi-skill-lessons`.
- Docx viết bằng tiếng Anh nên bản nháp đề xuất ở §4 cũng bằng tiếng Anh. Chỗ `[...]` là số liệu nhóm phải điền;
  **không tự bịa số**.

## 1. Đánh giá nhanh

| Tiêu chí | Hiện trạng | Mức độ |
| --- | --- | --- |
| Đủ khung template | Đủ 5 mục, nhưng còn placeholder và thiếu đoạn "Unmet Need" ở mục 2 | Phải sửa |
| Khớp code và plan | 3 mục "loại trừ" (LI-01, LI-03, LI-04) **đang có code**; thiếu các tính năng lõi đã làm hoặc đã chốt | Phải sửa |
| Khớp góp ý mentor | Thiếu business case 2 nhóm khách hàng; thiếu mô hình thẻ premium; mục 2 chưa nối được "đối thủ thiếu gì → mình làm gì" | Phải sửa |
| Chất lượng lập luận | Mục 1 chỉ dựa vào một nghiên cứu mà chính bài viết thừa nhận là không nói về IELTS; nhận xét về STUDY4 chưa có nguồn | Nên sửa |
| Văn phong | Vài câu tiếng Anh sai ngữ pháp, chen ngôi "your" | Nên sửa |

## 2. Lỗi phải sửa

### 2.1 Placeholder còn sót
- Bảng thông tin: `Last Updated [DD/MM/YYYY]`, `Reviewer(s) [Reviewer Name]`, `Status "Draft / In Review / Approved"`
  chưa chọn trạng thái. `Author(s)` chỉ có PhuPD: ghi đủ thành viên nhóm hoặc ghi người soạn và người review.
- Bảng Change History: dòng v1.0.0 còn `[DD/MM/YYYY]`, `[Author]`. Thêm dòng v1.1.0 cho lần sửa này.
- Tiêu đề trang bìa thiếu mã dự án: template là `[Project Name] ([CODE]) - Report 1`, nên viết
  `... for IELTS Space (IELTSPath) - Report 1`.
- Heading `2.1 [Solution Name 1]`, `2.2 [Solution Name 2]` → `2.1 STUDY4`, `2.2 ELSA Speak`.

### 2.2 Mâu thuẫn với code (hội đồng sẽ hỏi ngay khi xem demo)

| Mục trong Report 1 | Code thực tế | Cần quyết định |
| --- | --- | --- |
| LI-01: loại trừ native mobile app | Repo có `mobile/` (app Expo / React Native) | Mobile có nằm trong phạm vi demo không? Có thì bỏ LI-01, thêm feature; không thì ghi rõ "mobile app là prototype, không thuộc phạm vi đánh giá" |
| LI-03: loại trừ cộng đồng | Có `community-service` (bài viết, bình luận, reaction, kiểm duyệt) | Như trên |
| LI-04: loại trừ flashcard | `library-service` có flashcard, deck, note | Như trên |

Template quy định: mục 4 là baseline để đánh giá change request, nên scope phải khớp với cái sẽ demo.

### 2.3 Thiếu tính năng lõi (đã có code hoặc đã chốt plan)

| Tính năng | Bằng chứng | Vì sao phải có trong Report 1 |
| --- | --- | --- |
| Placement test + gợi ý course theo band | assessment `PLACEMENT`, plan Course | Đây là "điểm vào" của lộ trình cá nhân hóa |
| Course theo band, lesson nhiều skill (lý thuyết + bài tập) | content lessons, plan multi-skill | Mentor: "course là core, lesson nằm trong course" |
| Ôn tập thích ứng theo KP + mastery | learning `ReviewItem`, `MasteryCalculator` | Đây là phần "AI/adaptive" để lấy mức trên 9 điểm |
| Chấm Writing bằng AI (LLM) | learning `EssayGrader` | Là AI đã chạy thật; hiện Report 1 chỉ nhắc AI ở phần loại trừ |
| Kích hoạt premium bằng thẻ + log sử dụng | access-service: activation key, point, `key_activations` | Mentor: premium là bán thẻ, hệ thống chỉ log; Report 1 chưa nhắc |

### 2.4 Lỗi cấu trúc so với template
- Mục 2 thiếu đoạn mô tả 2–3 câu trước mỗi bảng, và thiếu đoạn kết **"Section Summary — Unmet Need"**.
- Mã GAP (GAP-01…) chỉ xuất hiện sau bảng feature, còn mục 2 thì không có. Template muốn mỗi solution kết bằng GAP;
  nên đánh mã ngay ở mục 2 rồi bảng 4.1 tham chiếu lại.
- Dòng `+Addresses Gap` trước danh sách GAP là ký tự thừa.
- Mục 1 thiếu hai ý template yêu cầu: "chi phí nếu không giải quyết" và "vì sao là lúc này".

## 3. Sửa theo góp ý mentor
1. **Business case 2 nhóm khách** (mục 1, 3, 5.1):
   - khách mới: học thử miễn phí, biết tới IELTS Space, rồi đăng ký khóa;
   - học viên hiện tại hoặc đang chờ lớp: luyện tập duy trì ngoài giờ học.
   Hiện Report 1 chỉ nói về "self-learners" chung chung và chỉ nhắc nhóm thứ hai ở 5.1.
2. **Premium = thẻ, không làm thanh toán hay kế toán** (4.1 + 4.2): sales bán thẻ ngoài hệ thống, hệ thống chỉ ghi ai
   kích hoạt thẻ nào, lúc nào. Ghi rõ việc loại trừ cổng thanh toán và báo cáo doanh thu.
3. **Existing solutions để khoe sản phẩm:** mỗi GAP ở mục 2 phải có feature tương ứng ở 4.1, để slide trình bày được
   "đối thủ thiếu X → bọn em làm X, đây là demo". Nên thêm một solution thứ 3: **cách học ngoài giờ hiện tại của IELTS
   Space** (template cho phép "manual workarounds"), vì nó nối thẳng vào business case.
4. **Không đưa vào Report 1:** lý do dùng microservice, performance test, test tự động. Các phần này thuộc Report 4
   (TDS) và Report 5 (test). Report 1 chỉ nói *cái gì* và *vì sao*, không nói *làm thế nào*.

## 4. Bản nháp đề xuất (tiếng Anh, dán vào docx)

### 4.1 Mục 1 — Product Background (thay toàn bộ)

> IELTS Space is an IELTS training centre in [city]. Its learners fall into two groups. The first group is new
> learners who are still choosing a centre: they want to try the teaching approach before paying for a course, but
> today the centre can only offer them [a free consultation / a trial class]. The second group is current students and
> students on the waiting list for their next class. Between lessons, or while waiting [average waiting time: … weeks],
> they practise on their own with [photocopied worksheets / shared Google Forms / free websites], and teachers cannot
> see what they practised or where they went wrong.
>
> A typical case: Linh, a second-year student targeting band 6.5, has finished the band 5.5 class and is waiting
> [… weeks] for the next intake. She completes practice tests online and sees a score, but not which reading skill
> caused her mistakes or what to study next. By the time the new class starts, she has lost the routine she built and
> her teacher has no record of her practice.
>
> [Data from IELTS Space: number of students per term, share on the waiting list, share of trial visitors who enrol.]
> Without a structured practice channel, the centre loses contact with learners between classes and has no low-cost
> way to show new visitors how it teaches. Learners, in turn, spend their self-study time without knowing which
> weaknesses to fix first.
>
> Online practice is now part of how Vietnamese students learn English: in a survey of 827 students at a Vietnamese
> university, content relevance and self-management were among the factors that shaped engagement in online learning
> (Tran et al., 2026). [Add the full reference to a References section, or remove if the source cannot be listed.]

### 4.2 Mục 2 — câu mở đầu mỗi solution, GAP có mã, đoạn kết

- **2.1 STUDY4** (câu mở): "STUDY4 is a Vietnamese online platform for IELTS practice tests and skill courses, widely
  used by self-learners to get familiar with the exam format."
  - GAP-01: "Test results show scores but do not identify the knowledge behind each mistake or what to study next."
  - GAP-02: "Learners practise full tests without first learning the knowledge each question type requires."
  - Câu "Writing and Speaking grading is not accurate or consistent enough": thêm nguồn (lần dùng thử của nhóm
    vào [ngày], hoặc review công khai) hoặc bỏ.
- **2.2 ELSA Speak** (câu mở): "ELSA Speak is an AI pronunciation and speaking coach used by English learners, with an
  IELTS Speaking course among its offerings."
  - GAP-03: "There is no IELTS path that connects Reading, Listening and Writing practice with feedback on written
    tasks."
- **2.3 IELTS Space's current out-of-class practice** (mới): mô tả cách trung tâm đang giao bài ngoài giờ.
  - GAP-04: "Teachers and the centre cannot see learners' practice between classes."
  - GAP-05: "New visitors have no way to try the centre's teaching before enrolling."
- **Section Summary — Unmet Need** (mới):
  > Test-practice platforms give scores without a learning path, speaking apps cover only one skill, and the centre's
  > own worksheets are invisible to teachers. None of them offers learners a band-based path that teaches the
  > knowledge first, then adapts practice to each learner's mistakes, while giving IELTS Space a channel to reach new
  > learners and keep current students practising between classes.

### 4.3 Mục 3 — Proposed Solution (thay)

> IELTSPath is IELTS Space's online learning channel. A short placement test estimates the learner's band and
> recommends a course; learners may still choose any course. Each course belongs to one band level and is made of
> topics and lessons that teach the knowledge behind each question type before practice (GAP-02). When a learner
> answers wrongly, the system records which knowledge point failed and assigns targeted review, so the next step is
> always specific (GAP-01). Written answers receive band-level feedback on the four IELTS Writing criteria (GAP-03).
>
> New visitors can study free topics and experience the centre's method before enrolling (GAP-05). Current students
> and those on the waiting list keep a daily practice routine, and the centre sees their progress (GAP-04). Premium
> content is unlocked with activation cards sold by the centre's sales staff, so the centre needs no online payment
> process.

### 4.4 Mục 4.1 — Major Features (thay bảng)

| ID | Feature Name | Description | Addresses Gap |
| --- | --- | --- | --- |
| FE-01 | Placement & Course Recommendation | A placement test estimates the learner's band and recommends a band-level course; learners can open any course. | GAP-01 |
| FE-02 | Band-Based Courses & Multi-Skill Lessons | Courses by band contain topics and lessons with theory and exercises for Reading, Listening and Writing. | GAP-02, GAP-03 |
| FE-03 | Adaptive Practice & Review | Practice results update mastery per knowledge point; weak points trigger targeted review sets and theory refreshers. | GAP-01 |
| FE-04 | AI Writing Feedback | Essays are graded by AI against IELTS Writing criteria with strengths and improvements for each criterion. | GAP-03 |
| FE-05 | Topic & Course Tests with Progress Tracking | Final tests close each topic and course; learners and the centre can track progress over time. | GAP-04 |
| FE-06 | Learning Games & Competitions | Real-time vocabulary and grammar games let learners compete and return to practise regularly. | GAP-04 |
| FE-07 | Vocabulary Through Videos | Learners study vocabulary in video context and save segments to review. | GAP-02 |
| FE-08 | Premium Activation Cards | Sales staff distribute activation cards; the system unlocks premium content and logs who activated each card and when. | GAP-05 |
| [FE-09] | [Flashcards / Community / Mobile app] | [Chỉ thêm nếu nhóm quyết định đưa vào phạm vi, xem §2.2] | [GAP-…] |

GAP-04 gộp hai ý: "trung tâm không thấy quá trình luyện tập" và "học viên thiếu động lực quay lại". Nếu muốn tách,
thêm GAP-06 cho động lực, rồi đổi FE-06 sang GAP-06.

### 4.5 Mục 4.2 — Limitations & Exclusions (thay bảng)

| ID | Excluded Item | Reason |
| --- | --- | --- |
| LI-01 | Speaking practice and AI Speaking assessment | Recording storage and reliable speech scoring need further validation; this version covers Reading, Listening and Writing. |
| LI-02 | Online payment, invoicing and revenue reporting | Premium access is granted through activation cards sold by sales staff; the system records card activations only and performs no accounting. |
| LI-03 | RAG-based IELTS assistant | Requires a curated IELTS knowledge base and answer-quality testing. |
| LI-04 | Full four-skill mock test | Depends on Speaking assessment (LI-01). |
| [LI-05] | [Native mobile app / Community / Flashcards] | [Chỉ giữ nếu nhóm quyết định loại khỏi phạm vi; nếu loại thì phải giải thích vì sao repo vẫn có code] |

### 4.6 Mục 5.1 — Business & Operational Impact (thay)

> IELTSPath gives IELTS Space two new channels. For acquisition, free topics let new visitors experience the centre's
> teaching before enrolling; the centre can measure how many trial learners register for a course [target: …%].
> For retention, current and waiting-list students keep practising between classes, and teachers can see which
> knowledge points each learner struggles with before the next class starts. Sales staff can answer card enquiries
> directly from the activation log (who activated which card and when) without accounting work.

Mục 5.2 giữ ý cũ, thêm một câu cho người mới: học thử miễn phí trước khi quyết định đăng ký khóa.
Mục 5.3 đổi tham chiếu LI theo bảng mới (Speaking → LI-01, RAG → LI-03). Có thể thêm "AI-assisted question
generation for teachers" làm hướng tiếp theo nếu nhóm chưa làm kịp.

### 4.7 Sửa câu (văn phong)
- "Vietnamese learners are preparing for…" → "Vietnamese learners preparing for…".
- "Create numerous opportunities for active speaking practice and receive quick feedback on your speaking style." →
  "It gives learners many chances to speak and quick feedback on their delivery."
- GAP-02 cũ "Based on your experience, the feedback…" → bỏ "Based on your experience", viết ở ngôi thứ ba.
- Dùng thống nhất tên sản phẩm **IELTSPath** trong thân bài (hiện thân bài không nhắc tên sản phẩm lần nào).

## 5. Thứ tự sửa đề xuất
1. Chốt phạm vi mobile, community, flashcard (§2.2): đây là quyết định, các bước khác phụ thuộc vào nó.
2. Lấy số liệu thật từ IELTS Space cho mục 1 và 5.1.
3. Viết lại mục 2 (thêm solution 3, đánh mã GAP, đoạn Unmet Need) → mục 3 → bảng 4.1 → 4.2 → 5.
4. Điền metadata, change history v1.1.0, rồi cho một người review đối chiếu bảng 4.1 với demo.

## Câu hỏi còn mở
1. Mobile app, community, flashcard: trong hay ngoài phạm vi đánh giá?
2. IELTS Space hiện giao bài ngoài giờ bằng cách nào? Học viên chờ lớp trung bình bao lâu? Có số liệu số học viên và
   tỉ lệ học thử chuyển thành đăng ký không?
3. Nghiên cứu Tran et al. (2026): có trích dẫn đầy đủ không? Template không có mục References, nên cần thêm mục này
   hoặc bỏ trích dẫn.
4. Reviewer chính thức của tài liệu là ai (giảng viên hướng dẫn)?
