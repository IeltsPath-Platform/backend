---
title: "Cây tri thức, chẩn đoán và hành trình đánh giá theo board mentor"
description: "Dựng cây KP có cạnh tiên quyết, event log từng câu, mastery 3 mức, chẩn đoán dò ngược, điểm có trọng số 20/30/50, hàng chờ giám khảo, rubric và bộ test AI, khu quản trị"
status: in_progress
priority: P1
branch: "feat/knowledge-tree-assessment-journey"
tags: [content-service, learning-service, assessment-service, access-service, knowledge-tree, mastery, diagnostics, scoring, ai-policy]
blockedBy: ["261009-1417-practice-gated-lesson-unlock"]
blocks: []
created: "2026-10-16T01:31:00.000Z"
createdBy: "ck:plan"
source: "board FigJam 'cây tri thức' + biên bản góp ý mentor (4 file ghi âm)"
---

# Cây tri thức, chẩn đoán và hành trình đánh giá

## Overview

Mentor yêu cầu IELTSPath có một cây tri thức thật sự quyết định hành vi học ("điểm ăn điểm"). Cụ thể:

- KP có cạnh tiên quyết.
- Mọi câu trả lời ghi thành event log.
- Mastery theo Knowledge ID có ngưỡng và hạn ôn.
- Khi học viên vẫn sai, hệ thống dò ngược theo cây để tìm điểm tắc.

Kèm theo là điểm có trọng số do Admin cấu hình, AI không tự quyết đạt/trượt và có bộ test nghiệm thu, cùng phân việc
System / Mentor / R&D.

Board mới đã chuyển sang IELTSPath (bỏ BTVN, điểm danh, cam kết đầu ra, 9 vai trò):
https://www.figma.com/board/iTbzVZl1h4QLxkeF078uAS

```text
Lesson ──> Practice (chặn, plan 261009) ──> Practice test theo mốc
   │            │                                 │
   └──── response + error tag + duration ─────────┴──> Event log từng câu ──> Mastery profile (3 mức, hạn ôn)
                                                                                   │
Topic test (giữa khóa, 30%) ─ < 60% → bổ trợ ─> topic sau          < 60% chặn │ 60–79 củng cố │ ≥ 80 duy trì
        │ thi hết topic test                                                       │
Course test (50%, essay do EXAMINER) ─> Điểm tổng ≥ 70%? ─ không ─> bổ trợ theo nhóm lỗi ─> thi lại mã khác
                                                                                   │
Bài ôn trượt 2 set / hết bộ câu ──> Chẩn đoán dò ngược KP tiên quyết ──> bài ôn cho KP gốc
```

## Quyết định đã chốt

Đây là các khuyến nghị ngày 2026-10-15. Người dùng yêu cầu lập plan theo đúng các khuyến nghị này ("làm luôn một plan
để impl tất cả"). Muốn đổi quyết định nào thì sửa ở đây và ở các phase liên quan trước khi code.

- **D1. Không có BTVN.** Bài tập trong lesson chỉ ghi vào event log và mastery, không tính điểm.
- **D2. Giữa khóa là topic test.** Điểm giữa khóa là trung bình % của lần thi đầu tiên ở mỗi topic test trong course.
  Học viên thi xong topic test (điểm bao nhiêu cũng được) thì topic sau mở. Dưới 60% thì bật cờ bổ trợ giữa khóa.
  Nội dung bổ trợ là bài ôn của các KP sai mà hệ thống đã tự sinh.
- **D3. Course test mở khi đã thi hết topic test của course.** Không xét điểm và không xét bài ôn ("100% được thi").
- **D4. Đạt đầu ra khi điểm tổng ≥ 70%.**
  - Điểm tổng = 20% Process + 30% giữa khóa + 50% cuối khóa. Process = Practice (lần nộp đầu) + practice test theo mốc.
  - Admin chỉnh được trọng số, tỷ lệ con và ngưỡng.
  - Học viên bỏ một thành phần thì thành phần đó tính 0. Course không có loại bài nào đó thì chia lại trọng số cho các
    phần còn lại.
  - Thi lại sẽ thay điểm cuối khóa bằng lần thi lại mới nhất.
- **D5. Giỏ ôn chặn ở mức ưu tiên.** Giữ nguyên các nguồn sinh bài ôn đang chặn, như plan 261009. Thêm hai mức không
  chặn: 60–79% là củng cố, từ 80% là duy trì (nhắc khi đến hạn ôn). Kết quả ôn chỉ cập nhật mastery. Bài ôn sinh từ
  practice test tự do (`OFFICIAL_PRACTICE` không thuộc mốc) cũng không chặn.
- **D6. LLM không quyết định đạt/trượt.**
  - Essay của course test luôn do EXAMINER chấm mù. LLM vẫn chấm song song để lưu so sánh và trả nhận xét tạm.
  - Topic test, lesson, Practice và placement vẫn do LLM chấm. Những chỗ này không quyết định đạt/trượt.
- **D7. Đơn vị chọn câu vẫn là gói `PRACTICE_SET`, tìm theo tag KP.** Không giao lại gói đã lộ lời giải (AGENTS.md
  §3.8, D11 của plan 261002). Hết gói thì `SKIPPED`, kèm chẩn đoán và báo R&D.
- **D8. Chẩn đoán dò ngược** bắt đầu khi trượt 2 set ôn của cùng KP hoặc khi KP hết gói.
  - Thăm dò mỗi KP tiên quyết bằng một set chưa lộ, sâu tối đa 3 tầng, tối đa 6 lần thăm dò mỗi phiên.
  - Ngưỡng đạt của lần thăm dò là `PassMark` (70%).
- **D9. Thời gian làm:** thêm `item_results[].duration_ms` (cho phép null) vào `AssessmentCompleted.v2`, không tạo v3
  (constitution API-01).
- **D10. Difficulty chỉ là tag,** dùng để lọc theo band và thống kê. Không dùng để hạ độ khó khi học viên làm sai
  (mentor dặn không làm adaptive/IRT đợt này).
- **D11. Không thêm role, không thêm service.**
  - Chức năng quản trị đặt ở service đang sở hữu dữ liệu. Frontend gom các màn lại.
  - Vai trò trên board quy về role có sẵn: Mentor/GV/Tester → `EXAMINER`, R&D → `CONTENT_AUTHOR`,
    Admin/Quản lý đào tạo → `ADMIN`.
- **D12. Cây tiên quyết ở cấp KP, là đồ thị không vòng (DAG).** Use case kiểm tra không tạo vòng. Cùng một kỹ năng ở
  hai band khác nhau là hai KP riêng, nối với nhau bằng cạnh.
- **D13. Course 6.0 và 7.0 tạo ở trạng thái `INACTIVE`** để chứa topic/KP của cây cho tới khi có bài học. Học viên
  không thấy hai course này.
- **D14. Repo đang public.** Rubric diễn giải lại band descriptor công khai của IELTS và ghi nguồn. Bài mẫu và câu hỏi
  seed phải tự viết.

## Ngoài phạm vi

- Chấm Speaking, điểm danh, BTVN, cam kết đầu ra.
- Điều chỉnh độ khó thích ứng (IRT).
- Gói Mock Test 4 kỹ năng.
- AI sinh câu hỏi và course, import Excel: có plan riêng `261016-0804-ai-content-and-course-builder`.
- Frontend: repo riêng. Mỗi phase có mục "Bàn giao FE".
- Cập nhật SRS (file Word).

## Phases

| Phase | Name | Service | Status |
|-------|------|---------|--------|
| 1 | [Cây tri thức: cạnh tiên quyết và API](./phase-01-content-knowledge-tree.md) | content | Completed (ef2d259) |
| 2a | [Course/topic 6.0, 7.0 INACTIVE, difficulty bắt buộc, lọc learning](./phase-02-content-seed-tree-and-difficulty.md) | content, learning | Completed (ef2d259) |
| 2b | [Seed KP, cạnh, difficulty backfill, bộ câu](./phase-02-content-seed-tree-and-difficulty.md) | content | Còn 7 KP Listening chưa đủ 3 bộ (cần audio). Bộ Reading/Writing đã seed (V44) nhưng là **bản nháp chưa duyệt: không giao cho học viên thật** cho tới khi người hiểu IELTS duyệt (xem phase 2b) |
| 3a | [Error taxonomy: bảng, CRUD, ErrorTagger, ghi error analysis](./phase-03-content-error-taxonomy.md) | content, learning, assessment | Completed (a140b8c) |
| 3b | [Duyệt mã lỗi chính thức và gắn distractors cho câu](./phase-03-content-error-taxonomy.md) | content | Completed (V35, V36; quyết định Q1–Q4 đã chốt) |
| 4 | [Event log từng câu và thời gian làm](./phase-04-question-event-log-and-duration.md) | learning, assessment | Completed |
| 5 | [Mastery profile 3 mức, hạn ôn, API báo cáo](./phase-05-mastery-profile-and-report.md) | learning | Completed |
| 6 | [Giỏ ôn nhiều mức và recheck](./phase-06-review-tiers-and-recheck.md) | learning | Completed (6a + 6b) |
| 7 | [Chẩn đoán dò ngược theo cây](./phase-07-diagnostic-backtracking.md) | learning | Completed (4a0b56e) |
| 8 | [Practice test theo mốc course](./phase-08-practice-test-milestones.md) | content, assessment, learning | Completed (933 test của ba service xanh, không skip; xem báo cáo) |
| 9 | [Điểm có trọng số và kết quả khóa](./phase-09-course-scoring-and-outcome.md) | learning | Completed |
| 10 | [Hàng chờ giám khảo và chính sách AI](./phase-10-examiner-queue-and-ai-policy.md) | assessment | Completed (80123b5) |
| 11 | [Rubric dạng dữ liệu và bộ test nghiệm thu AI](./phase-11-rubric-data-and-ai-evaluation.md) | content, assessment, learning | **Công cụ xong, chưa nghiệm thu**: chưa có bằng chứng (cần gold set chuyên gia và lần chạy đánh giá thật). Không bật mặc định quyết định nào dựa trên kết quả đánh giá AI |
| 12 | [Khu quản trị: hồ sơ 360°, cờ, hàng việc, đánh giá Mentor](./phase-12-admin-area-backend.md) | learning, assessment, access | Completed (Gateway route đã có test, c014d56) |
| 13 | [Contract, tài liệu và nghiệm thu](./phase-13-contracts-docs-and-verification.md) | docs | Đóng, còn mục mở (contract và tài liệu xong; `mvn verify` xanh toàn reactor; demo Compose bước 4, 5, 8 đã chạy lại trên project riêng ngày 2026-10-18 và đạt; còn bước 5 chưa chạy phần bài viết của đề cuối khóa vì đề seed không có essay, và Phase 11 chưa nghiệm thu; xem `reports/acceptance.md`) |
| 14 | [Học viên sửa essay và nộp lại](./phase-14-essay-revision.md) | learning | Completed (9cf6d74, báo cáo có `revisionDelta`) |
| 15 | [Mentor chữa essay Practice và lesson](./phase-15-mentor-practice-essays.md) | learning, assessment, access | Completed (c03b9e0, 0a35bc4; chưa chạy thử nối thật hai service) |

## Phân công đang làm (một nhánh `feat/knowledge-tree-assessment-journey`)

| Người | Phase | File chạm tới | Migration được dùng |
|---|---|---|---|
| Claude | plan 261009, rồi phase 9 | learning: `ReviewItem`, `ReviewRule`, `PracticeClearance`, `LessonAccessGate`, `LessonAccess`, `GetTopicLessonsUseCase`; phase 9: `ApplyAssessmentResultUseCase`, `TopicStatusDeriver`, `AssignCourseTestUseCase`, `CourseProgress` | learning **V15** (V14 dành cho phase 11) |
| Codex | 11 (rubric dạng dữ liệu, bộ test nghiệm thu AI) | content: rubric, API, seed; assessment: gold set, đánh giá, kích hoạt prompt, `OpenAiCompatibleEssayGrader`, `GradeGateEssayJobUseCase`; learning chỉ `EssayGrader`, client đọc rubric, lưu `prompt_version` | content **V39**, assessment **V10**, learning **V14** |

Đã xong: phase 4 (Claude, commit `85ad593`), phase 8 (Codex, commit `e710f11`), phase 10 (Codex, commit `80123b5`) và
phase 5 (Claude).

Quy tắc để không đè nhau trên cùng nhánh:
1. Mỗi bên chỉ sửa file trong cột của mình. Nếu cần sửa file của bên kia, dừng và hỏi.
2. Commit bằng đường dẫn cụ thể (`git add <file>`), không dùng `git add -A`.
3. Chỉ một Maven build chạy tại một thời điểm, vì `target/` dùng chung. Codex chạy xong thì báo, Claude mới build.
4. Phase 9 làm sau plan 261009, cùng người giữ learning. Phase 6/7 chờ plan 261009.

## Thứ tự và phụ thuộc

```text
1 ──> 2 ─────────────────────────────┐
3 ──> 4 ──> 5 ──> 6 ──> 7 <───────────┘ (7 cần cây seed + bài ôn nhiều mức)
            │
8 ──> 9 <───┘ (9 cần event log cho nhóm lỗi, practice test cho Process)
10 ──> 11 (gold set lấy từ essay giám khảo chấm)
5, 7, 9, 10, 11 ──> 12 ──> 13
(đã có phase đọc essay: 14 cần 4, 5; 15 cần 10, 12)
plan 261009 (pending) ──> 6, 7 (cùng sửa ReviewItem, LessonAccessGate)
```

Có thể chia ba nhánh song song: **A** (1 → 2), **B** (3 → 4 → 5), **C** (10 → 11). Nhánh 8 → 9 bắt đầu sau khi
plan 261009 đã merge.

Số migration dự kiến:
- content: V29 (P1), V30 (P2), V31 (P3), V32 (P8), V33 (P11).
- learning: V11 (P4), V12 (P5, gồm `learning_rule_configs`), V13 (P6), V14 (P7), V15 (P8), V16 (P9), V17 (P12), V18 (P14),
  V19 (P15, nếu cần).
- assessment: V8 (P4), V9 (P10), V10 (P11), V11 (P12).

Đánh lại số theo thứ tự merge thực tế. Không sửa migration đã merge.

## Acceptance tổng

- Cây:
  - R&D thêm, sửa, ngưng KP và cạnh qua API. Thêm cạnh tạo vòng → `422 PREREQUISITE_CYCLE`.
  - `GET /api/content/knowledge-tree` trả đủ node và cạnh của band 5.5 → 7.
- Event log:
  - Mỗi câu nộp ở lesson, Practice, set ôn, lần thăm dò và bài thi đều có một dòng event, kèm `duration_ms` nếu client
    gửi, và mã lỗi khi chọn phương án sai đã gắn mã.
- Mastery và chẩn đoán:
  - `GET /api/learning/report` trả Score / Error / Evidence / Action cho từng Knowledge ID.
  - Trượt 2 set ôn của KP B có tiên quyết A → phiên chẩn đoán thăm dò A. A trượt → bài ôn chặn cho A, và bài ôn B chờ
    tới khi A xong.
- Điểm và kết quả khóa:
  - Thi xong topic test (kể cả dưới 70%) → topic sau mở.
  - Thi hết topic test → course test mở.
  - Điểm tổng tính đúng trọng số Admin cấu hình. Dưới 70% → `REMEDIATION`, xong bổ trợ → `RETEST_READY`, thi lại mã
    khác → xét lại.
- Chính sách AI:
  - Essay course test không có kết quả tới khi EXAMINER chấm.
  - `essay_grade_comparisons` có cả điểm LLM lẫn điểm giám khảo.
  - Lần chạy đánh giá AI trả accuracy, QWK, false-positive.
- Khu quản trị:
  - EXAMINER thấy hàng chờ chấm, cờ bất thường, học viên cần bổ trợ.
  - CONTENT_AUTHOR thấy KP thiếu bộ câu, câu quá khó/dễ, mẫu chấm lại để đánh giá Mentor.
  - Mỗi việc lưu Action / Result / Note.
- Kiểm chứng: `mvn -B -ntp verify` cả reactor xanh, Testcontainers chạy (không skip).

## Dependencies

- **Plan 261009** (pending) phải merge trước phase 6 và 7.
- **Nội dung:** cây 5.5 → 7, mỗi KP có ít nhất 3–4 bộ `PRACTICE_SET`, rubric diễn giải, bài mẫu. Có thể giao nhóm nội
  dung soạn theo template ở phase 2 và 11, nhưng cần người hiểu IELTS duyệt.
- **Gold set:** vài chục essay do người có chuyên môn IELTS chấm tay (phase 11).
- **FE** (repo `frontend`, `mobile`):
  - màn quản lý cây;
  - báo cáo mastery;
  - giỏ ôn;
  - chẩn đoán;
  - điểm khóa;
  - cấu hình điểm cho Admin;
  - hàng chờ EXAMINER / R&D;
  - hồ sơ 360°;
  - gửi `durationMs`.

## Open questions

None.

Đã xác nhận bởi người dùng:
- **2026-10-17:** D2, D3, D4 (giữa khóa là topic test, course test mở khi đã thi hết topic test, đạt đầu ra khi điểm tổng
  20/30/50 từ 70%). Phase 9 được phép bắt đầu.
- **2026-10-16:** phase 14 (giữ luật bằng chứng hiện tại cho essay nộp lại).

Còn lại theo khuyến nghị, chưa xác nhận riêng: D5 (giỏ ôn chặn ở mức ưu tiên) và D6 (essay course test do EXAMINER chấm
mù). Sửa trước khi bắt đầu phase 6 và phase 10 nếu muốn đổi.

Số migration trong plan là dự kiến; đánh lại theo thứ tự merge thực tế (ví dụ phase 9 có thể thành V17 nếu một phase
trước đó đã dùng V16).
