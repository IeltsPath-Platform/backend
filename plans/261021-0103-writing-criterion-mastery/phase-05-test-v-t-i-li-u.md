---
phase: 5
title: "Test và tài liệu"
status: pending
priority: P1
effort: "0.5d"
dependencies: [1, 2, 3, 4]
---

# Phase 5: Test và tài liệu

## Overview

Chốt chất lượng bằng test đúng tầng (Mockito cho use case, đơn vị cho rule, Testcontainers cho migration và persistence), chạy lại các test đã có, rồi cập nhật tài liệu và ghi chú cho board Figma.

## Requirements

- Không để test gọi LLM thật (quy tắc `AGENTS.md`); dùng `LlmClient` giả hoặc `MockRestServiceServer`.
- Test Testcontainers tự bỏ qua khi không có Docker: báo rõ nếu bị bỏ qua.
- Chạy Maven bằng `package`, không `clean` khi service đang chạy từ IDE; không chạy song song với Maven của Codex/IDE.

## Test cần viết

| Tầng | Test | Kiểm tra |
|---|---|---|
| Domain | `WritingEvidenceRuleTest` | ngưỡng `>= passBand`; Task 1 và Task 2; tiêu chí dưới ngưỡng ghi sai; hai tiêu chí chung KP -> AND; KP đề trùng KP tiêu chí -> tiêu chí thắng; KP đề ngoài ánh xạ -> `passed`; `knowledgePointId == null` -> bỏ; mã lỗi nhiều nhất, hòa theo chữ cái, rỗng |
| Application | `EssayGraderTest` (bổ sung) | điểm tiêu chí mang đúng `knowledgePointId`; thiếu KP vẫn chấm được |
| Application | `SubmitLessonEssayUseCaseTest` (bổ sung hoặc mới) | 4 bằng chứng đúng KP; `firstAttempt` đúng; KP không thuộc danh mục bị bỏ; event có `error_code`; bài cũ không KP rơi về hành vi cũ |
| Application | `SubmitPracticeAttemptUseCaseTest` (bổ sung) | essay item tạo bằng chứng theo tiêu chí; câu khách quan không đổi; `reviewNeeds` và kết quả đạt/không đạt không đổi |
| Persistence | Testcontainers | event essay có `error_code` và mảng KP; bằng chứng nhiều dòng một bài; chạy lại không nhân đôi; JSON bài chấm cũ đọc ra `null` |
| Persistence | Testcontainers cho backfill | bài cũ -> bằng chứng tiêu chí với `created_at` gốc; chạy hai lần không đổi; KP đề trùng được cập nhật |
| Content | seed/migration test | rubric active TASK_1/TASK_2 mỗi tiêu chí có KP, KP đôi một khác nhau; rubric phiên bản cũ cũng có KP; migration thất bại khi thiếu KP nguồn |

Lệnh chạy hẹp trước, rồi mở rộng:

```powershell
mvn -q -pl services/content-service -am package   # kèm test mới của content
mvn -q -pl services/learning-service -am package   # lỗi biên dịch test MessageRetry ở reactor 'test' là vấn đề cũ, dùng 'package'
```

## Tài liệu cần cập nhật

- `AGENTS.md` §3.8: ghi rằng essay ghi một bằng chứng cho mỗi tiêu chí vào KP ánh xạ (ngưỡng `passBand`), event essay mang `error_code`, và Writing vẫn không sinh bài ôn.
- `docs/contracts/learning-content-internal-v1.md`: trường `criteria[].knowledgePointId` (làm ở phase 1; kiểm lại ở đây).
- `docs/contracts/lesson-writing-v1.md` (do Codex phụ trách, thống nhất trước khi sửa): mô tả bằng chứng theo tiêu chí và rằng API không lộ KP.
- `docs/system-architecture.md`: chỉ sửa nếu đoạn mô tả luồng chấm/mastery Writing hiện có (kiểm tra trước khi sửa).
- Board Figma "IELTSPath – Cây tri thức – đánh giá" (việc thủ công, ghi cho người dùng): đổi hộp "Bài viết (essay)" và "Chẩn đoán lỗi theo Knowledge ID" cho khớp: mỗi tiêu chí một bằng chứng KP, trọng số essay lesson 0.5 / essay Practice 0.6, mã lỗi essay vào event; Writing không sinh bài ôn chặn.

## Implementation Steps

1. Viết `WritingEvidenceRuleTest` cùng lúc với rule (phase 3), các test còn lại ngay sau khi code của phase tương ứng xong.
2. Chạy `package` của content-service và learning-service; ghi lại số test chạy/bị bỏ qua.
3. Rà toàn bộ plan và tài liệu một lần để thống nhất tên trường (`knowledgePointId`), tên bảng, số migration (V69), và quy tắc D1 đến D7.
4. Cập nhật các tài liệu ở trên.

## Success Criteria

- [ ] Test mới xanh; test hiện có của hai service xanh (hoặc ghi rõ test bị bỏ qua do thiếu Docker).
- [ ] `grep` không còn mô tả cũ "essay ghi một kết quả cho KP của đề" trong tài liệu đã nêu.
- [ ] Người dùng nhận được ghi chú cụ thể để sửa board Figma.
- [ ] Không file nào của Codex bị sửa ngoài `lesson-writing-v1.md` (và chỉ sau khi thống nhất).

## Risk Assessment

- Test persistence chậm hoặc bỏ qua vì thiếu Docker: báo rõ, không tuyên bố đã chạy.
- Tranh chấp file với Codex ở tài liệu hợp đồng: hỏi trước, hoặc để Codex cập nhật theo mô tả của phase này.
