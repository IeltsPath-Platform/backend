# P6 – Contract, tài liệu, E2E

Phụ thuộc: P1–P5.

## 1. Contract

| File | Cập nhật |
| --- | --- |
| `docs/contracts/learning-content-internal-v1.md` | `topic-sequence`: điều kiện mới (có skill + lesson PUBLISHED, không bắt buộc TOPIC_TEST), field `skill`, `hasTopicTest`, thứ tự. Lesson: `skill`, block `knowledgePointIds`. Route mới `lessons/{id}/practice-sets`, `topics/{id}/practice-sets`, `POST practice-sets/availability`. `practice-sets/search`: `preferredLessonId`. Ghi rõ tất cả là thay đổi **thêm**, v1 vẫn tương thích. |
| `docs/contracts/lesson-learning-v1.md` | Lộ trình theo skill; review khoá theo skill; topic không có test (`hasTopicTest`, `testStatus=NONE`, lỗi `NO_TOPIC_TEST`); Practice catalog + attempt (route, trạng thái, lỗi `PRACTICE_LOCKED`); thang ôn tập (stage, `theoryReason`, `theory-check`, lỗi `THEORY_REQUIRED`/`THEORY_NOT_REQUIRED`, `maxFailedSets=2`); `GET /reviews` (`limit`). Thêm sơ đồ trạng thái review. **Sửa các câu đang mâu thuẫn**: dòng ~9 "solutions stay hidden until the relevant block or set is passed", dòng ~106 "its solutions are omitted until the set is passed", dòng ~124 "After the third failed set … reveal solutions only on a passed set" ⇒ mô tả ngoại lệ D11 (lời giải sau khi nộp cho review set/practice, không dùng lại package đã lộ, hết package ⇒ SKIPPED, 2 set trượt ⇒ SKIPPED). Ghi rõ kho package chung giữa Practice và set ôn (D12). **Đổi hành vi**: hoàn thành lesson không còn tạo review (sửa mọi câu nói review tạo khi complete/submit lesson); review từ Practice theo luật KP < 70% trong attempt, lần nộp đầu, không xét mastery; review từ assessment giữ mastery < 0.6; topic test cần mọi lesson qua Practice, lỗi `PRACTICE_REQUIRED`, `practiceStatus` + `practicePassReason` trên lesson, tính đơn điệu (D13). |

Sơ đồ đưa vào contract:

```text
                 (trigger)
                     |
        +------------+-------------+
        | PRACTICE                 | THEORY  (<40% hoặc sai trong lesson)
        v                          v
   GET: giao set có hint      GET: block lý thuyết của KP + quick-check
   nộp set ─ đạt ─> DONE      POST theory-check ─> PRACTICE
        └ trượt ─ failedSets>=2 ─> SKIPPED
                └ còn lại ─> THEORY
```

## 2. README

- `services/learning-service/README.md`: thay mục "review rules" bằng luật mới; thêm Practice; ghi chú di chuyển dữ
  liệu Writing (P3 mục 1).
- `services/content-service/README.md`: `topics.skill`, `DEMO_WRITING`, `lesson_block_knowledge_points`,
  `content_packages.lesson_id`.
- `AGENTS.md` §3.8 (**bắt buộc**, ngoại lệ D11): thay câu "Học viên không bao giờ nhận `answerSpec`, `explanation`
  trước khi đạt, …" bằng: không bao giờ nhận `answerSpec`, `chartFacts`; `explanation`/transcript chỉ sau khi đạt,
  **trừ** practice attempt và review set: hiện sau khi nộp, và package đã lộ không bao giờ được giao lại làm set ôn
  cho chính learner đó. Sửa thêm câu về một chuỗi topic duy nhất nếu có. Không sửa phần khác.
- `CLAUDE.md`: chỉ sửa nếu có câu mô tả sai sau thay đổi.
- `docs/system-architecture.md`: mục Learning (chuỗi topic theo skill, Practice, thang ôn tập). Câu dòng ~181 về
  assessment giữ nguyên (assessment không đổi).
- `docs/fe-main-flow-guide.md`: thêm luồng Practice và review stage; ghi rõ lesson exercise vẫn chỉ hiện lời giải
  khi đạt.

## 3. E2E runbook

Thêm `plans/261002-1600-skill-tracks-practice-remediation/reports/e2e.md` (Codex tạo khi chạy), chạy stack Docker
Compose như các plan trước, learner mới, qua Gateway.

1. `GET /api/learning/topics` ⇒ có READING, LISTENING, WRITING, mỗi skill một IN_PROGRESS.
2. `GET /lessons/{L1}/practice-sets` khi chưa xong L1 ⇒ mọi item `LOCKED`; start ⇒ 409 `PRACTICE_LOCKED`.
3. Học L1 cố ý sai **một** câu (block vẫn đạt) ⇒ hoàn thành ⇒ **không** có review; practice-sets `AVAILABLE`.
4. L1 chỉ có một đề (`PS-KP3-A`): làm 2/4 ⇒ `reviewsCreated=[]` (không còn đề chưa lộ, D12), response có
   `explanation`; L1 `practiceStatus=PASSED/ALL_SETS_ATTEMPTED`. Xong L2, làm `PS-KP1-A` (KP Main idea có 4 đề) 2/4 ⇒
   `reviewsCreated` có review `stage=PRACTICE`. Các bước 5–8 chạy thang ôn trên L2.
5. Lesson Reading kế tiếp (L3) ⇒ 403 `REVIEW_REQUIRED`; mở LS1 (Listening) ⇒ 200.
6. `GET /reviews/{id}` ⇒ set là package **khác** `PS-KP1-A`, câu có `hint`; nộp sai ≥ 40% ⇒ `stage=THEORY`,
   `theoryReason=SECOND_FAIL`, `results` có `explanation`, `failedSets=1`.
7. `GET /reviews/{id}` ⇒ `theory` chỉ gồm TEXT của KP1, có `quickCheck`, `set=null`; nộp set ⇒ 409
   `THEORY_REQUIRED`; `theory-check` ⇒ `stage=PRACTICE`.
8. `GET /reviews/{id}` ⇒ package thứ ba, chưa lộ; nộp sai ⇒ `reviewStatus=SKIPPED`; lesson Reading kế tiếp mở lại;
   L1 giờ "qua Practice" (review PRACTICE của L1 đã kết thúc).
9. Xong L3–L4 nhưng chưa qua Practice ⇒ `POST /topics/{DEMO_READING}/test-assignments` ⇒ 409 `PRACTICE_REQUIRED`;
   làm đạt practice lần đầu của từng lesson ⇒ giao đề được.
10. W1, W2 ⇒ `/complete` ⇒ topic `DEMO_WRITING` PASSED; `POST /topics/{DEMO_WRITING}/test-assignments` ⇒ 409
    `NO_TOPIC_TEST`.
11. Listening (chỉ kiểm phần có đủ dữ liệu): LS1 chưa xong ⇒ practice-sets `LOCKED`; xong ⇒ `AVAILABLE`; làm
    `PS-NUM` dưới 70% ⇒ **không** tạo review (KP chỉ có 1 đề, đã lộ), `reviewsCreated=[]`; khi mọi package của LS1 đã
    nộp ⇒ LS1 `practiceStatus=PASSED`, `practicePassReason=ALL_SETS_ATTEMPTED`.

Báo cáo che token, mật khẩu, email.

## 4. Kết thúc

- `mvn -q -pl services/content-service,services/learning-service -am test` xanh.
- `graphify update .`.
- `plan.md`: Status → completed, điền Verification từng phase.

## Verification

- Tài liệu: `AGENTS.md` §3.8 (ngoại lệ lời giải D11, chuỗi topic theo skill), `docs/system-architecture.md` (Learning,
  route nội bộ, transcript, flow), `docs/fe-main-flow-guide.md` (Practice, stage bài ôn, mã lỗi mới),
  `docs/contracts/lesson-learning-v1.md` (sơ đồ trạng thái review; phần còn lại cập nhật ở P3–P5). Contract nội bộ
  Content và README hai service đã cập nhật ở P1–P5. `CLAUDE.md` không có câu sai, không sửa.
- E2E: [reports/e2e.md](reports/e2e.md), 47/47 PASS qua Gateway của `docker-compose.mvp.yml` sau khi build lại
  content và learning, learner mới. Runbook bước 4 đã sửa theo dữ liệu seed thật (L1 chỉ có một đề Practice).
- `graphify update .` đã chạy.
- Test: content-service 185/185 (lần chạy ở P2b, không đổi code content sau đó); learning-service 216 test ở P5 (xem
  phase-05). Không chạy lại gộp hai service sau phần tài liệu vì P6 không đổi code.
