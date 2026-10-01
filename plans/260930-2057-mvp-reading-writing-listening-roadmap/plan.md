---
title: "MVP học Reading, Writing, Listening (lộ trình tổng)"
description: "Lộ trình gom các plan con thành một MVP: learning-service (Java) chỉ tạo path, cập nhật path, điều phối làm bài và chấm Writing. Chốt phạm vi, thứ tự merge, số migration, tài liệu, và nghiệm thu E2E cho ba kỹ năng. Không chép lại nội dung plan con."
status: in-progress
priority: P1
branch: "feat/main-follow"
tags: [mvp, roadmap, ai-learning, content, assessment, access, reading, writing, listening]
blockedBy: []
blocks: []
created: "2026-09-30T14:00:57.387Z"
createdBy: "ck:plan"
source: skill
---

# MVP học Reading, Writing, Listening (lộ trình tổng)

## Overview

Plan điều phối, không có code riêng. Mỗi phase triển khai trỏ tới một plan con; chi tiết kỹ thuật nằm ở plan con. Plan này giữ:
phạm vi MVP, thứ tự merge, số migration cố định, các điểm chạm chung giữa plan con, và nghiệm thu cuối.

**Learning Service (Java, thay ai-learning Python từ 2026-10-01) trong MVP chỉ làm:**
1. Tạo path (mỗi học viên một path, theo `sort_order`, không LLM).
2. Cập nhật path (bằng chứng từ bài tập, bài ôn, đề cuối, Writing; chèn bài ôn khi KP yếu).
3. Điều phối làm bài (thứ tự topic → bài → bài ôn → đề cuối; cổng mở bài; giấu đáp án tới khi đạt).
4. Chấm Writing bằng LLM (Task 1 Academic và Task 2), trừ point qua access, có hạn mức chấm theo ngày.
5. API xem mastery theo KP (`GET /api/learning/mastery`).

Kỹ năng MVP: **Reading, Writing, Listening**. Speaking ngoài phạm vi.

## Phạm vi (chốt 2026-09-30)

| Plan | Nội dung | MVP |
| --- | --- | --- |
| `261001-1228` learning-service Java | Xóa ai-learning Python; path theo user, bài học, bài ôn, mã đề, consumer, `/mastery` bằng Java (thay phase 5–7 của 1640) | Có (chốt 2026-10-01) |
| [`260929-1640`](../260929-1640-lesson-learning-pipeline-mvp/plan.md) | Nền: contract, chặn lộ đáp án, content bài học, assessment tự chấm, Reading | Có; phase 5–7 thay bởi `261001-1228` |
| `260930-0737` | Writing Task 2, LLM chấm, trừ 3 point qua access | Có; refund không mở cho service |
| [`260930-0812`](../260930-0812-lesson-writing-task1-academic/plan.md) | Writing Task 1 Academic (biểu đồ, `chartFacts`) | Có |
| [`260930-0851`](../260930-0851-listening-topic-audio-lessons/plan.md) | Listening (audio, transcript ẩn tới khi đạt) | Có, không chờ 0908 |
| [`260930-1006`](../260930-1006-reading-question-hints/plan.md) | Gợi ý câu Reading sau lần sai đầu | Có; bỏ `hints_used` |
| `261001-0205` Chia lại service | Tạo `library-service`, giải thể learning-support, activity/streak sang user-service (`.sdd/specs/SERVICE_ARCHITECTURE_V3.md` §1, §4, §9) | Có (chốt 2026-10-01); S1 merge **trước** 1640 (đổi 2026-10-01), S2–S4 lúc nào cũng được. Tiến độ: **xong** S1–S4, đã merge vào `feat/main-follow` (PR #21) |
| ~~`260930-0908`~~ (đã xóa) | Chọn gói luyện theo độ khó và dạng câu | **Hoãn**; ý tưởng ở `plans/reports/brainstorm-260930-0908-listening-adaptive-path-report.md` |

**Ngoài MVP (không làm, không gỡ):**
- Tutor chat, learner memory, practice notebook, sắp path bằng LLM, `/status`, `/progress`: **xóa hẳn** cùng service Python (đổi 2026-10-01).
- Speaking, Writing trong đề cuối, EXAMINER chấm tay Writing, upload/signed URL audio, giới hạn lượt nghe.
- Chọn gói theo độ khó (0908), premium/entitlement topic.
- Trong kiến trúc đích cũ nhưng ngoài MVP: bảng `examiner_profiles` (user-service) và 5 bảng của notification-service.

## Thứ tự merge và số migration (cố định)

```text
Chia service S1 ──► 1640 (phase 1–3) ──► 1228 learning-service ──► 1640 (phase 4, 8) ──► 0737 Writing T2 ──► 0812 Writing T1 ──► 0851 Listening ──► 1006 Gợi ý Reading
Chia service S2 ──► S3 ──► S4: không đụng migration content/ai-learning, merge xen vào lúc nào cũng được
```

| Service | Số migration |
| --- | --- |
| content | V7 xóa 5 bảng từ vựng/video (chia service S1; phá hủy, cần duyệt) · V8 bảng bài học, V9 seed Reading (1640) · V10 seed Writing T2 (0737) · V11 seed Writing T1 (0812) · V12 seed Listening (0851) · V13 cột `hint` + seed (1006) |
| learning-service (mới) | V1 toàn bộ schema lõi (1228) · V2 `lesson_writing_submissions`, `llm_daily_usage` (0737). Plan khác không có migration. ai-learning Python bị xóa (không còn V10–V12) |
| assessment, access | Không migration mới |
| user | V5 `learning_activities`, `streaks` (chia service) |
| library (mới) | V1 trở đi: 5 bảng catalog + 6 bảng thư viện cá nhân (chia service) |
| learning-support | Không migration mới; module, database và route bị gỡ (chia service) |
| Nhãn `DATABASE_V5.md` §0 | V5.3 Listening (đã ghi) · V5.4 gợi ý Reading · V5.5 Writing (Task 1 + Task 2) |

Plan vào sau không đổi số của plan vào trước. Nếu buộc phải đổi thứ tự, sửa bảng này trước rồi sửa plan con.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Chốt phạm vi và đồng bộ plan con](./phase-01-chot-pham-vi-va-dong-bo-plan-con.md) | Completed |
| 2 | [Validate lại plan con](./phase-02-validate-lai-plan-con.md) | Completed |
| 3 | [Triển khai nền 1640](./phase-03-trien-khai-nen-1640.md) | In progress (1640 phase 1–4 xong, assessment tự chấm merge PR #32; `261001-1228` hoàn thành; còn 1640 phase 8 tài liệu) |
| 4 | [Triển khai Writing Task 2](./phase-04-trien-khai-writing-task-2.md) | Completed (0737 xong 5/5 phase, plan đã xóa; chấm bài luận ở learning-service) |
| 5 | [Triển khai Writing Task 1](./phase-05-trien-khai-writing-task-1.md) | In progress (0812 phase 1–2 xong, grader dùng chung với 0737; còn phase 3 tài liệu) |
| 6 | [Triển khai Listening](./phase-06-trien-khai-listening.md) | In progress (0851 phase 1, 2, 4 xong; phase 3 đề cuối có audio đang giao Codex; phase 5 tài liệu) |
| 7 | [Triển khai gợi ý Reading](./phase-07-trien-khai-goi-y-reading.md) | In progress (1006 phase 1 xong, merge PR #30; phase 2 trả gợi ý ở learning-service chưa làm; phase 3 tài liệu) |
| 8 | [Nghiệm thu E2E MVP](./phase-08-nghiem-thu-e2e-mvp.md) | Pending |

Phase 1–2 chỉ sửa tài liệu và plan. Phase 3–7 theo đúng thứ tự merge; mỗi phase xong thì merge rồi mới sang phase kế.

## Dependencies

- Plan con giữ quan hệ `blockedBy` của chúng; plan này không chặn plan nào, chỉ sắp thứ tự.
- Dọn plan (2026-09-30): đã xóa `260929-1830` (đồng bộ tài liệu, bị 1640 thay), `260930-0908` (chọn gói theo độ khó, hoãn) và
  `260928-2019` (tài liệu chia service; chia service giờ thuộc MVP, cần plan triển khai mới). Nội dung còn trong git history (commit `6506d5e` cho 1830 và 2019; 0908 chưa
  từng commit, ý tưởng giữ ở brainstorm report). Thư mục `plans/` còn 5 plan con MVP + plan tổng này.
- Hạ tầng: bucket cloud có URL công khai cho mp3 Listening (`CONTENT_MEDIA_BASE_URL`); model LLM OpenAI-compatible cho
  Writing (`LEARNING_LLM_*`).

## Tiêu chí nghiệm thu MVP

- Học viên mới đi hết: Reading → Writing → Listening theo thứ tự topic, qua bài học, bài ôn (khi yếu) và đề cuối, chỉ bằng API
  `/api/learning/*` (topic, bài, bài ôn, giao mã đề, nộp Writing) và `/api/assessments/*` (làm đề).
- Không response nào cho học viên chứa `answerSpec`, `chartFacts`, `explanation` trước khi đạt, hay transcript trước khi đạt.
- Chấm Writing trừ đúng 3 point mỗi lần chấm thành công; hết point vẫn học hết topic (khối essay không chặn).
- Toàn bộ test learning-service, content, assessment, access, Gateway pass.

## Nội dung seed

[`seed-content.md`](./seed-content.md): toàn bộ đoạn văn, câu hỏi, đáp án, khối bài học, gói luyện, mã đề, audio, đề Writing, gợi ý và kịch bản kiểm thử (có số mastery) cho mọi plan con. Agent code không cần mở trang demo.

## Câu hỏi mở

- Chọn dịch vụ cloud cho mp3 và người thu âm 8 file seed Listening (không ảnh hưởng code).
- Ai soạn `chartFacts` và ảnh SVG cho seed Task 1.

## Validation Log

### Session 1 — 2026-09-30
**Trigger:** `/ck:plan validate` ngay sau khi tạo plan tổng.
**Questions asked:** 4

#### Verification Results
- Tier: Standard (plan điều phối; phase 3–7 là con trỏ tới plan con, nên kiểm các dữ kiện mà plan tổng dựa vào).
- Claims checked: 9 | Verified: 9 | Failed: 0 | Unverified: 0
- Đã kiểm: frontmatter `blockedBy`/`blocks` của 0737, 0812, 0851, 1006, 0908; số migration ghi sẵn (0737 V9 seed và
  `V12__lesson_writing_submissions`; 0812 V10 seed; 0851, 1006 dùng `V<n>`); nhãn `V5.3` ở 0737 phase 5 và 0851 phase 5, `V5.4`
  ở 1006 phase 3; 0737 chuyển "4 endpoint" sang `/internal/access/**`; `writing_point_cost = 3`; migration 1640 (content V7/V8,
  ai-learning V10/V11).

#### Questions & Answers
1. **[Tradeoff]** Thứ tự merge sau 1640: Writing trước hay Listening trước?
   - Options: Writing trước | Listening trước
   - **Answer:** Writing trước.
   - **Rationale:** khớp số migration plan con đã ghi; rủi ro access/LLM lộ sớm.
2. **[Risk]** Refund của access (cộng point với `amount` tùy ý, chỉ cần đúng subject) xử lý thế nào?
   - Options: Không mở cho service | Giữ như 0737
   - **Answer:** Không mở cho service.
   - **Rationale:** Writing không dùng refund; mở cho service là đường tự cộng point.
3. **[Architecture]** Bằng chứng mastery của khối Writing?
   - Options: Mọi lần tới khi đạt | Chỉ lần đầu
   - **Answer:** Mọi lần tới khi đạt.
   - **Rationale:** mỗi lần nộp là bài viết mới, không phải đoán loại trừ như câu tự chấm.
4. **[Scope]** `hints_used` của plan 1006?
   - Options: Bỏ hints_used | Ghi vào submission
   - **Answer:** Bỏ hints_used.

#### Impact on Phases
- Phase 2: mục 0737 (bằng chứng Writing, refund) và 1006 (`hints_used`) đổi từ "đề xuất" thành "đã chốt".
- Phase 4: yêu cầu access và rủi ro refund.
- Phase 7: yêu cầu bỏ `hints_used`.
- `plan.md`: bảng phạm vi.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-08.
- Decision deltas checked: 4.
- Stale terms searched: "4 endpoint sang", "đề xuất" cho refund/`hints_used`/bằng chứng Writing, thứ tự Listening trước Writing,
  tên file phase 5/6 cũ.
- Reconciled: 6 (phase-02 3, phase-04 2, phase-07 1) + bảng phạm vi plan.md 2 dòng.
- Unresolved contradictions: 0.

### Session 2 — 2026-10-01
**Trigger:** người dùng muốn chia lại service ngay trong MVP ("chia lại service như vậy luôn, làm gì còn learning-support").
Trước đó plan xếp việc chia service ra sau MVP (hiểu sai ý người dùng; đã hoàn tác cùng ngày).
**Questions asked:** 3

#### Verification Results
- Chưa có plan triển khai chia service: plan `260928-2019` (git history `6506d5e`) chỉ ghi tài liệu và tự ghi plan triển khai trước đó đã bị xóa.
- Content: 5 bảng từ vựng/video (`V1`), 56 file Java, không có seed; FK chỉ nằm trong nhóm này. Không service nào gọi API từ vựng/video.
- Game: snapshot `VOCABULARY` lấy từ content (`GameContentProvider`), phải chuyển sang library.
- learning-support: 140 file Java, 8 bảng + outbox (V1–V3); Gateway route `/api/learning-support/**` (`api-gateway.yaml:65-68`, `:110`);
  compose `learning-support-db`; module trong `pom.xml:30`.
- user-service migration tới V4; AI Learning không gọi learning-support hay API từ vựng/video.

#### Questions & Answers
1. **[Scope]** Chia service gồm gì? — Options: chỉ chia service | toàn bộ kiến trúc đích cũ (thêm `examiner_profiles`, bảng notification).
   **Answer:** chỉ chia service.
2. **[Order]** Merge lúc nào? — Options: sau cùng, sau 1006 | trước 1640 (lùi số migration). **Answer:** sau cùng; content V13.
3. **[Data]** Dữ liệu dev? — Options: không chép, xóa bảng cũ | chép rồi xóa | không chép, không xóa.
   **Answer:** không chép, xóa bảng cũ (content V13 phá hủy, người dùng duyệt trước khi chạy; bỏ `learning_support_db`).

#### Impact
- Bảng phạm vi, thứ tự merge, bảng số migration của plan này.
- 1640: `lesson_block_vocabulary` là id logic (không FK) vì từ vựng sang library; phase 8 không viết lại kiến trúc (plan chia service làm).
- Cần tạo plan triển khai chia service (`/ck:plan`), đặt sau 1006 trong lộ trình; thêm một phase triển khai vào plan này khi plan đó có.

### Session 3 — 2026-10-01
**Trigger:** agent code đã làm xong PR S1 của plan chia service (commit `99d5347`) khi plan 1640 chưa có dòng code nào.
**Questions asked:** 1

#### Verification Results
- S1 thêm content `V13__drop_vocabulary_and_video_tables.sql`; content hiện có V1–V6. Nếu V13 chạy trước, Flyway từ chối V7–V12
  của luồng học (bản thấp hơn bản đã chạy; repo không bật `out-of-order`).
- Không code nào nhắc số 13 của migration này; đổi tên file không làm hỏng test.

#### Questions & Answers
1. **[Order]** Giữ chia service sau cùng, hay merge S1 trước 1640? — Options: B. S1 trước, đổi V13 thành V7, lùi số content của
   luồng học 1 bậc | A. giữ như Session 2. **Answer:** B (thay câu 2 của Session 2).

#### Impact
- Content: V7 xóa bảng (S1); 1640 V8/V9, 0737 V10, 0812 V11, 0851 V12, 1006 V13. Ai-learning, user, library không đổi.
- 1640 `blockedBy` plan chia service (chỉ cần S1). Plan con đã đổi số trong phần đặc tả; validation log cũ giữ số cũ.
