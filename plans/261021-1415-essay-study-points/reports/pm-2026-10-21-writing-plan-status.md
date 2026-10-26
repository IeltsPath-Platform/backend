# Đồng bộ trạng thái hai plan Writing — 2026-10-21

Status: DONE_WITH_CONCERNS (đã đồng bộ tài liệu; hai plan chưa đóng toàn bộ gate nghiệm thu).

| Plan | Tiêu chí đạt | Phase hoàn tất | Trạng thái |
|---|---|---|---|
| [0103: bằng chứng theo tiêu chí](../../261021-0103-writing-criterion-kp-evidence/plan.md) | 13/15 (87%) | 2/4 | in-progress |
| [1415: kiến thức cần học](../plan.md) | 15/18 (83%) | 1/4 | in-progress |

Trước đồng bộ, metadata ghi completed cho các phase nhưng toàn bộ checkbox vẫn trống. Đã tick phần có bằng chứng, giữ mở các gate chưa đạt và đồng bộ bảng phase/frontmatter. Không sửa code, không commit/push trong lần cập nhật này.

## Backend: bằng chứng hiện có, không chạy lại Maven

Đọc các XML trong `services/*/target/surefire-reports` ngày 2026-10-21; đây là artifact các lượt chạy trước, không phải một lượt test mới hay bằng chứng exit code của lệnh package.

| Service | Test | Failure | Error | Bỏ qua |
|---|---:|---:|---:|---:|
| Content | 526 | 0 | 0 | 0 |
| Learning | 826 | 1 | 1 | 0 |
| Assessment | 358 | 0 | 0 | 0 |

Các test trực tiếp liên quan đều qua: RubricRepositoryIntegrationTest 4; LessonPipelineSeedTest 34; PracticeSetCoverageMigrationTest 6; WritingEvidenceRuleTest 10; EssayGraderTest 25; WritingGradeJsonTest 4; WritingWebMvcTest 5; LessonWritingIntegrationTest 16; PracticeEssayIntegrationTest 4. Các lớp Testcontainers trong danh sách không bị bỏ qua theo XML hiện có.

Hai lỗi Learning đúng với ngoại lệ plan 1415 đã ghi:

- `ReviewBasketIntegrationTest.aWrongAnswerInTheMainFlowUpgradesTheOptionalReviewOfThatPointToBlocking`: 1 failure.
- `StudyContextTest.lessonUsesExistingGateAndTrustedCourse`: 1 error.

Không xác minh lại baseline của hai lỗi này trong lần cập nhật tài liệu. Không có log đủ để khẳng định mọi lệnh package trong hai plan đều kết thúc thành công; vì vậy các checkbox tương ứng còn mở. Không dùng `clean`, không ghi lại `target/` từ Maven.

## Frontend: kiểm tra đã chạy

Lệnh chạy mới trong lần đồng bộ:

```powershell
npm test -- src/features/learning-path/api/learningAdapters.studyPoints.test.ts src/features/learning-path/components/EssayStudyPoints.test.tsx src/features/learning-path/lib/studyPointPractice.test.ts src/features/learning-path/components/PracticeEssays.test.tsx --maxWorkers=1
```

Kết quả: **4 file, 41 test qua, 0 lỗi**.

Các kết quả từ lượt sửa review ngay trước trong cùng cuộc hội thoại, không chạy lại ở task cập nhật status:

- `npm run lint`, `npm run typecheck`, `npm run build`: qua.
- `node --test tests/learning-path.test.mjs`: 32/32 qua.
- `npm test`: 212/215 qua, 3 lỗi (AuthPage thiếu ToastProvider; PracticeEssaysSample chờ switch Bài mẫu; PracticeEssays timeout).
- Chạy riêng ba file lỗi với `--maxWorkers=1`: 26/28 qua; timeout hết, còn lỗi AuthPage và PracticeEssaysSample.

Chưa chạy hai lỗi frontend còn lại trên HEAD sạch. Không kết luận là hồi quy hoặc lỗi nền; không đổi test/assert chỉ để xanh. Không tạo worktree hay đổi nhánh để cập nhật tài liệu.

## Hợp đồng và triển khai

- MockMvc đã sinh `services/learning-service/target/writing-study-points-contract.json`. Đối chiếu artifact với fixture frontend: mục CRITERION có đúng 8 khóa, mục AI có kind/title/reason; criterionCode, band, knowledgePointId, code và tiêu đề AI khớp. Test adapter trong lượt 41 test qua.
- `criteria[]` vẫn không lộ KP. Plan 1415 cho phép có chủ đích KP trong `studyPoints[]`; ghi chú của plan 0103 đã làm rõ điểm kế thừa này.
- Đã đối chiếu chặn XP mốc theo `criterionOnly` và `withoutMilestone`; phase 3 của 0103 cho phép review code nếu fixture chưa tạo được tình huống vượt mốc. Chưa có test riêng cho tình huống đó; chưa kiểm chứng nhánh migration V69 thiếu KP nguồn.
- UI có nhóm gợi ý AI theo tiêu chí và khôi phục bài đã chấm từ công việc khác trong workspace; không chỉnh hay hoàn tác các thay đổi đó trong lần này. Test Bài mẫu đang cần đối chiếu với hành vi đã thay đổi.

## Thủ công và phạm vi diff

Chưa thực hiện luồng đăng nhập → chấm essay thật → luyện KP → quay lại → hoàn thành → thử hết bộ. Chưa kiểm tra DB thực. Không gọi LLM thật. Việc đối chiếu JSON MockMvc không thay thế E2E trên hệ thống chạy thật.

Workspace có thay đổi chưa commit của V70, studyPoints, khôi phục bài chấm và review UI. Chưa tách và xác nhận toàn bộ diff theo danh sách file của từng phase, nên checkbox phạm vi của 1415 còn mở. Không tạo commit, không push, không đổi nhánh.

## Ghi chú bàn giao Figma của plan 0103

Chưa sửa trực tiếp board. Nội dung cần cập nhật ở board “IELTSPath – Cây tri thức – đánh giá”, hai hộp “Bài viết (essay)” và “Chẩn đoán lỗi theo Knowledge ID”:

> KP của đề nhận đạt/không đạt; mỗi tiêu chí TA/TR, CC, LR, GRA là một bằng chứng riêng cho KP tiêu chí trong topic Writing. Trọng số essay lesson 0.5, essay Practice 0.6. Mã lỗi phổ biến nhất được ghi vào event. Writing không sinh bài ôn chặn.

## Việc còn lại

1. Xử lý hoặc xác nhận cách nghiệm thu hai lỗi Learning đã ghi; lưu log package cần thiết.
2. Đối chiếu hai lỗi test frontend còn lại với baseline/hành vi Writing hiện tại và hoàn tất gate toàn bộ test.
3. Tách phạm vi diff theo từng công việc; thực hiện kiểm tra thủ công nếu môi trường cho phép và ghi kết quả thực tế.

Không có yêu cầu triển khai mới được thêm; các việc trên là phần kiểm chứng còn mở trong plan.
