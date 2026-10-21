---
title: "Bằng chứng mastery Writing theo tiêu chí chấm"
description: "Chấm xong essay thì ghi một bằng chứng cho mỗi tiêu chí (TA/TR, CC, LR, GRA) vào KP tiêu chí riêng trong topic DEMO_WRITING, và ghi mã lỗi phổ biến nhất vào event của essay."
status: in-progress
priority: P2
effort: "2.25d"
branch: "fix/learn-writing-skill"
tags: [content-service, learning-service, mastery, writing]
blockedBy: []
blocks: []
created: "2026-10-20T18:07:41.859Z"
revised: "2026-10-21"
createdBy: "ck:plan"
source: skill
---

# Bằng chứng mastery Writing theo tiêu chí chấm

## Mục tiêu

Hiện nay mỗi essay chỉ ghi MỘT kết quả (`passed` = band tổng >= `passBand`) cho KP gắn với đề, nên mastery không biết
học viên yếu tiêu chí nào; mã lỗi trong `corrections` không vào `question_attempt_events.error_code`.

Sau thay đổi:
- Mỗi tiêu chí của bài chấm là một bằng chứng cho KP tiêu chí của nó: đúng khi `band tiêu chí >= passBand` của đề.
- KP của đề vẫn nhận `passed` như cũ.
- Event của essay (vẫn 1 dòng) mang `error_code` = mã lỗi gặp nhiều nhất trong `corrections`.

Không đổi: prompt LLM và `promptVersion`, band tổng, `passed`, phí 3 điểm, trọng số nguồn (essay lesson 0.5, essay
Practice 0.6), quy tắc bài nào được tính (`firstAttempt` / `counted`), Writing không sinh bài ôn, XP và nhiệm vụ, response
API cho học viên.

## Quyết định đã chốt

| # | Quyết định | Lý do / bằng chứng trong code |
|---|---|---|
| D1 | Tạo 5 KP tiêu chí MỚI trong topic `DEMO_WRITING` (đang ACTIVE): `DEMO_WRITING_CRITERION_TA/TR/CC/LR/GRA` | KP `W60_*`/`W70_*` thuộc topic 6.0/7.0 `INACTIVE` (V41), không vào danh mục học viên (`RefreshLearningTopicsUseCase.java:48-49`) nên bằng chứng sẽ bị bỏ. Không bật topic 6.0/7.0 vì sẽ thêm topic vào lộ trình mọi học viên |
| D2 | Ánh xạ `(task, criterion_code) -> KP` ở bảng `writing_criterion_knowledge_points` của Content, không gắn phiên bản rubric | Rubric đã publish bất biến; rubric cũ (`RETIRED`) mà prompt đang kích hoạt vẫn có KP |
| D3 | Tiêu chí đúng khi `band >= prompt.passBand`; `passBand == null` thì bỏ qua tiêu chí | Cùng ngưỡng với `passed` (`WritingSubmission.java:186`) |
| D4 | KP của đề giữ `passed`. Nếu một KP vừa là KP của đề vừa là KP tiêu chí thì kết quả tiêu chí thắng; hai tiêu chí cùng KP thì AND | Với seed hiện có không trùng (đề gắn `DEMO_*` khác), quy tắc chỉ để xác định |
| D5 | Event của essay giữ `knowledge_point_ids` = KP của đề (không thêm KP tiêu chí), `correct = passed`, thêm `error_code` | Báo cáo học viên hiển thị `event.correct` theo KP (`GetLearnerReportUseCase.java:107`); thêm KP tiêu chí chưa đạt vào event sẽ hiện ✓ sai |
| D6 | `error_code` = mã có số lần nhiều nhất trong `corrections`, hòa thì lấy mã nhỏ nhất theo chữ cái, bỏ mã null/rỗng; không có thì `null` | Cột chỉ chứa một giá trị |
| D7 | `knowledgePointId` lưu trong JSON bài chấm (`WritingGrade.Criterion`), lấy từ rubric lúc chấm | Không gọi Content trong transaction ghi bằng chứng; bài cũ thiếu trường thì `null` và đi theo hành vi cũ |
| D8 | KP tiêu chí từ essay Practice KHÔNG được trả XP mốc | Nguồn `PRACTICE_SET` được XP mốc (`MasteryProfileUpdater.java:79-86`); giữ XP không đổi |
| D9 | Không backfill essay cũ | `kp_evidence` sắp theo `ordinal` (thứ tự chèn), không theo `created_at` (`V1__learning_schema.sql:92`, `JdbcKnowledgeEvidenceRepository.java:52,112`); chèn bù sẽ làm bằng chứng cũ thành mới nhất trong cửa sổ MP-1.0 |

## Phases

| Phase | Nội dung | Effort | Status |
|---|---|---|---|
| 1 | [Content: KP tiêu chí, bảng ánh xạ, rubric trả `knowledgePointId`](./phase-01-content-criterion-knowledge-points.md) | 0.5d | Completed (4/4) |
| 2 | [Learning: bài chấm mang KP của từng tiêu chí](./phase-02-learning-grade-carries-criterion-kp.md) | 0.5d | Completed (3/3) |
| 3 | [Learning: ghi bằng chứng tiêu chí và mã lỗi cho essay lesson và Practice](./phase-03-learning-record-criterion-evidence.md) | 1d | In progress (4/5; triển khai xong, gate toàn Learning còn lỗi) |
| 4 | [Tài liệu và kiểm tra toàn bộ](./phase-04-docs-and-full-verification.md) | 0.25d | In progress (2/3; chờ xác nhận package) |

Thứ tự bắt buộc: 1 → 2 → 3 → 4. Mỗi phase tự viết test của nó và phải xanh trước khi sang phase sau.

Triển khai: content-service chạy V69 trước. Learning chịu được thiếu trường (`knowledgePointId = null` → hành vi cũ), nên
khởi động lệch thứ tự không lỗi. Rollback: bỏ trường `knowledgePointId` khỏi response rubric nội bộ → mọi bài mới về hành
vi cũ; dữ liệu đã ghi không cần gỡ.

## Ngoài phạm vi

- Backfill bằng chứng cho essay đã chấm (D9).
- Free writing (`SubmitFreeWritingUseCase`) và essay của assessment-service: không ghi `kp_evidence`.
- Bài ôn chặn sinh từ essay: Writing vẫn không sinh bài ôn chặn. (Bộ luyện demo cho 5 KP tiêu chí được thêm riêng ở V70.)
- Đổi prompt LLM, gán mã lỗi theo `category` sang KP tiêu chí, dùng `severity`.
- Cho Admin sửa ánh xạ tiêu chí → KP qua UI (do migration sở hữu).

## Ảnh hưởng đã biết (chấp nhận)

- 5 KP mới xuất hiện trong cây tri thức, báo cáo học viên và giỏ ôn. Không chặn tiến độ: qua topic theo lesson/thi topic,
  không theo mastery KP.
- Mỗi KP tiêu chí có 3 bộ luyện tự do (`PS-WCR-*`, V70, mỗi bộ 3 câu, không gắn lesson/topic), nên `hasPracticeSet = true`:
  KP yếu có thể được chọn cho bài ôn, nhiệm vụ luyện tập và luyện tự do như mọi KP khác. Test độ phủ
  (`PracticeSetCoverageMigrationTest`) không còn ngoại lệ cho 5 KP này.
- Nội dung V70 là demo do AI soạn, cần giám khảo IELTS duyệt trước khi dùng như tài liệu chính thức.
- Bằng chứng tích lũy từ bài nộp sau khi deploy; essay cũ không đổi.

## Khoảng trống còn lại

- Chưa có test tự động cho việc essay Practice không sinh XP mốc cho KP tiêu chí (`MasteryProfileUpdater.recordEvidence`
  với `withoutMilestone`). Test tích hợp hiện tại không tạo được kịch bản đạt mốc chỉ từ một essay. Đã kiểm tra bằng đọc code.
- Chưa có test cho nhánh migration V69 thất bại khi thiếu KP nguồn (guard trong SQL chưa được kiểm chứng).
- Phase 1-4 đã commit trên `fix/learn-writing-skill` (3 commit). V70 và thay đổi test độ phủ chưa commit.
- `docs/contracts/lesson-writing-v1.md` đã đối chiếu với code: bằng chứng tiêu chí đúng mô tả; KP chỉ lộ trong `studyPoints[]` theo bổ sung của plan 1415.

## Tiêu chí nghiệm thu

- Essay lesson Task 2 đạt ghi bằng chứng cho KP của đề (`passed`) và 4 KP tiêu chí TR/CC/LR/GRA, đúng/sai theo `band >= passBand`;
  Task 1 dùng TA thay TR.
- KP không có trong danh mục học viên vẫn bị bỏ ở essay lesson (như hiện nay).
- Essay Practice được tính (`counted`) ghi bằng chứng `practice_set` như trên; `reviewNeeds`, đạt/không đạt của bộ, tim, XP không đổi.
- Event của essay: đúng 1 dòng, `knowledge_point_ids` như cũ, `error_code` theo D6.
- Bài chấm cũ (JSON không có `knowledgePointId`) đọc được và ghi bằng chứng như cũ.
- `WritingSubmissionResponse.criteria[]` không có `knowledgePointId`; prompt LLM không đổi. KP trong `studyPoints[]` là bổ sung có chủ đích của plan 1415.
- `mvn -q -pl services/content-service -am package` và `mvn -q -pl services/learning-service -am package` xanh.

## Ghi chú cho người thực hiện

- Đọc `AGENTS.md` (§3.8) trước. Không gọi LLM thật trong test (dùng `LlmClient` giả như test hiện có).
- Maven: dùng `package`, không `clean` khi service đang chạy từ IDE; không chạy song song hai lệnh Maven.
- Test Testcontainers tự bỏ qua khi không có Docker: nếu bị bỏ qua thì ghi rõ, không báo là đã chạy.
- Kiểm tra số migration ngay trước khi tạo file: content-service hiện tới V68, file mới là V69. Nếu V69 đã có, dùng số kế
  tiếp và sửa mọi chỗ nhắc "V69" trong plan.
- Không đặt mã plan/phase vào comment, tên test hay commit message.

## Đồng bộ trạng thái — 2026-10-21

Triển khai đã có; nghiệm thu **13/15 tiêu chí (87%)**, 2/4 phase hoàn tất. Đổi trạng thái tổng từ `completed` về `in-progress` vì checklist kiểm chứng toàn Learning và package chưa đạt bằng chứng xanh. Đây là cập nhật trạng thái, không mở rộng phạm vi hay khẳng định hai lỗi nền do tính năng Writing gây ra.

Bằng chứng và giới hạn: [báo cáo đồng bộ](../261021-1415-essay-study-points/reports/pm-2026-10-21-writing-plan-status.md).
