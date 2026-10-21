---
title: "Mục 'Kiến thức cần học' trong kết quả chấm essay"
description: "Kết quả chấm essay trả thêm danh sách studyPoints: các KP tiêu chí dưới điểm đạt (kèm nút Luyện tập dẫn sang luyện tự do) và các kỹ năng cần học do LLM đề xuất (focus). Hiển thị ở khung kết quả của bộ luyện tập Writing."
status: in-progress
priority: P2
effort: "2.5d"
branch: "fix/learn-writing-skill"
tags: [learning-service, frontend, writing, essay-feedback]
blockedBy: []
blocks: []
created: "2026-10-21T07:15:00.000Z"
revised: "2026-10-21"
createdBy: "ck:plan"
source: skill
---

# Mục "Kiến thức cần học" trong kết quả chấm essay

## Mục tiêu

Kết quả chấm essay hiện có: band tổng, đạt/chưa đạt, nhận xét chung, nhận xét từng tiêu chí, chỗ cần sửa. Thiếu mục
"Kiến thức cần học" (ứng dụng tham chiếu có cột này). Thông tin đã có sẵn nhưng đang bị bỏ:

- Prompt chấm đã yêu cầu LLM trả `focus: ["<kỹ năng cần học>: <lý do>"]` (`essay-prompt-template-v1.txt`), nhưng
  `EssayGrader.parse` không đọc và `WritingGrade` không có chỗ lưu.
- Mỗi tiêu chí đã mang `knowledgePointId` (KP tiêu chí `DEMO_WRITING_CRITERION_*`, đã có 3 bộ luyện `PS-WCR-*` ở V70),
  nhưng không được dùng để chỉ cho học viên cần học gì.

Sau thay đổi, mỗi essay đã chấm trả thêm `studyPoints` gồm hai loại:

| kind | Nguồn | Hiển thị | Hành động |
|---|---|---|---|
| `CRITERION` | Tiêu chí có band < `passBand` của đề và có KP | Tên KP, tiêu chí, band, mô tả KP | Nút "Luyện tập" mở luyện tự do của KP đó |
| `AI` | `focus` của LLM | Tiêu đề, lý do | Không có nút |

Không đổi: prompt LLM và `promptVersion`, cách chấm, band tổng, `passed`, phí 3 điểm, bằng chứng mastery, `criteria[]` và
`corrections[]` của response.

## Quyết định đã chốt

| # | Quyết định | Lý do / bằng chứng trong code |
|---|---|---|
| S1 | Thêm `studyPoints` vào response chấm essay; hai loại `CRITERION` và `AI` | Tận dụng dữ liệu có sẵn, không đổi prompt |
| S2 | Tiêu chí yếu = `band < prompt.passBand`; `passBand == null` thì không sinh mục `CRITERION` | Cùng quy tắc đang dùng để ghi mastery (`WritingEvidenceRule`) |
| S3 | Chỉ tiêu chí có `knowledgePointId` mới thành mục `CRITERION` | Không có KP thì không có chỗ để luyện; nhận xét tiêu chí đã có sẵn |
| S4 | Tính MỘT lần ở `EssayGrader.grade` (ngoài transaction), lưu trong JSON bài chấm; tên và mô tả KP lấy từ `content.getTopicSequence()` lúc chấm (ảnh chụp) | `WritingSubmissionResponse.from` được gọi ở 5 controller và phải giữ thuần túy; không thêm HTTP vào transaction (`finish()`, `assemble()`) |
| S5 | Content lỗi lúc lấy tên KP thì vẫn chấm được; `name`/`reason` để null | Chấm essay tốn điểm, không được hỏng vì một chi tiết phụ |
| S6 | `AI`: tối đa 4 mục; tách `"<tiêu đề>: <lý do>"` tại dấu ':' đầu tiên; tiêu đề cắt 80 ký tự, lý do cắt 300 | Đầu ra LLM là văn bản không tin cậy, phải giới hạn |
| S7 | Thứ tự: `CRITERION` (band tăng dần, hòa theo thứ tự tiêu chí của rubric) rồi `AI` theo thứ tự LLM | Yếu nhất lên trước |
| S8 | `knowledgePointId` lộ ra API CHỈ trong `studyPoints[]`; `criteria[]` không đổi | Đổi có chủ đích so với quyết định "không lộ KP" trước đây: cần id để mở luyện tập |
| S9 | Frontend: `PracticeEssays` nhận callback tùy chọn `onPractise`; không dùng hook router trong component | Test hiện có render `PracticeEssays` không có Router |
| S10 | Bài chấm cũ (JSON không có `studyPoints`) đọc ra `[]`, frontend ẩn mục; không backfill | Giống quyết định trước: không động vào dữ liệu cũ |

## Phases

| Phase | Repo | Nội dung | Effort | Status |
|---|---|---|---|---|
| 1 | backend | [Bài chấm mang studyPoints (grader, lưu trữ, response, test)](./phase-01-backend-grade-study-points.md) | 1d | Completed (6/6) |
| 2 | backend | [Tài liệu hợp đồng và kiểm tra build backend](./phase-02-backend-docs-and-verification.md) | 0.25d | In progress (2/3; chờ xác nhận package) |
| 3 | frontend | [Hiển thị 'Kiến thức cần học' và nút Luyện tập](./phase-03-frontend-study-points-ui.md) | 1d | In progress (4/5; chức năng xong, npm test còn lỗi) |
| 4 | cả hai | [Kiểm chứng đầu-cuối](./phase-04-end-to-end-check.md) | 0.25d | In progress (3/4; khớp hợp đồng, chưa kiểm tra thủ công và tách diff) |

Thứ tự: 1 → 2 → 3 → 4. Phase 3 có thể bắt đầu song song sau khi phase 1 chốt hợp đồng JSON (phần "Hợp đồng JSON" bên dưới).

## Hợp đồng JSON (cố định cho cả hai repo)

`studyPoints` nằm cạnh `corrections` trong response của essay đã chấm (`status = GRADED`). Trường null bị bỏ khỏi JSON.

```json
"studyPoints": [
  {
    "kind": "CRITERION",
    "criterionCode": "CC",
    "band": 5.5,
    "knowledgePointId": "3c000000-0000-4000-8000-020000000003",
    "code": "DEMO_WRITING_CRITERION_CC",
    "name": "Mạch lạc và liên kết (CC)",
    "title": "Mạch lạc và liên kết (CC)",
    "reason": "Chia đoạn hợp lý, sắp ý theo trình tự rõ ràng và nối câu bằng từ nối đúng chức năng."
  },
  {
    "kind": "AI",
    "title": "Cohesive Devices",
    "reason": "Dùng từ nối tự nhiên để liên kết câu và đoạn mạch lạc."
  }
]
```

Quy ước: mục `CRITERION` luôn có `criterionCode`, `band`, `knowledgePointId`; `code`/`name`/`title`/`reason` có thể thiếu khi
không lấy được chi tiết KP. Mục `AI` chỉ có `kind`, `title` và (có thể) `reason`. Essay không có gì cần học thì `studyPoints` là `[]`.

## Ngoài phạm vi

- Đổi prompt LLM hoặc `promptVersion` (`focus` đã có trong template).
- Backfill bài chấm cũ; tính lại mục `CRITERION` khi đọc.
- Giao diện essay của lesson (frontend hiện chỉ hiển thị kết quả essay ở khung luyện tập `PracticeEssays`) và trang kết quả Writing của assessment (`WritingResultPage`).
- Soạn thêm bộ luyện hoặc đổi `learning.free-practice.reserve` (xem rủi ro 1).
- Khóa nút "Chấm lại" (đã làm riêng ở frontend).

## Rủi ro và giới hạn đã biết

1. **Nút "Luyện tập" chỉ dùng được một lần cho mỗi KP tiêu chí.** Luyện tự do chỉ bắt đầu khi số bộ chưa xem lớn hơn
   `learning.free-practice.reserve` (đang là 2, `config-repo/learning-service.yaml`), mà mỗi KP tiêu chí chỉ có 3 bộ (V70).
   Lần thứ hai server trả `409 FREE_PRACTICE_EXHAUSTED`. Frontend phải hiện thông báo có sẵn của `userMessage` ("Đã hết bộ
   luyện tự do... được giữ cho bài ôn"), không được báo lỗi chung chung. Muốn dùng nhiều lần thì cần soạn thêm bộ (ngoài phạm vi).
2. `focus` là văn bản tự do của LLM, có thể thiếu, sai hoặc rỗng. Chỉ hiển thị như văn bản thường (React tự escape), không render HTML.
3. Tên/mô tả KP là ảnh chụp lúc chấm; đổi tên KP ở Content không cập nhật bài đã chấm.
4. Mỗi lần chấm thêm một lời gọi `content.getTopicSequence()` (chỉ khi có tiêu chí yếu có KP). Lỗi Content không làm hỏng việc chấm (S5).
5. Bấm "Luyện tập" chuyển sang trang luyện tự do và rời khung viết. Bài đã chấm không mất: server giữ bài chấm mới nhất và
   `PracticeEssays` khôi phục bằng `latest`. Học viên quay lại qua `backTo` rồi mở lại bộ để bấm "Hoàn thành".

## Tiêu chí nghiệm thu (toàn plan)

- Essay đã chấm có tiêu chí dưới `passBand` và có KP → response có mục `CRITERION` đúng thứ tự, kèm `name`/`reason` khi Content trả được.
- `focus` hợp lệ của LLM thành mục `AI` (tối đa 4); mục rỗng/không phải chuỗi bị bỏ.
- Bài chấm cũ và bài chấm không có gì cần học: `studyPoints = []`, giao diện không hiện mục này.
- `criteria[]`, `corrections[]`, band, `passed`, phí, bằng chứng mastery không đổi; `criteria[]` vẫn không có `knowledgePointId`.
- Content lỗi khi lấy tên KP vẫn chấm xong, mục `CRITERION` vẫn có `knowledgePointId` nhưng thiếu `name`.
- Frontend hiện mục "Kiến thức cần học"; nút "Luyện tập" mở luyện tự do; lỗi (hết bộ, hết tim, chưa tham gia khóa) hiện đúng thông báo ngay dưới mục.
- Backend `package` và frontend `typecheck`, `lint`, `test` xanh (ngoại trừ 2 test backend lỗi sẵn có, xem bên dưới).

## Ghi chú cho người thực hiện

- Hai repo git riêng, cùng tên nhánh `fix/learn-writing-skill`: backend ở `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\backend`,
  frontend ở `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\frontend`. Mỗi phase ghi rõ repo.
- KHÔNG commit, KHÔNG push. Để người nghiệm thu xem diff.
- Đọc `AGENTS.md` (backend, §3.8) trước. Không gọi LLM thật trong test (dùng `LlmClient` giả).
- Backend test Testcontainers cần Docker chạy. Nếu bị bỏ qua phải ghi rõ tên test, không được báo là đã chạy.
- Maven dùng `package`, không `clean`; không chạy hai lệnh Maven song song.
- Có sẵn 2 test lỗi từ trước trên HEAD, KHÔNG phải do plan này, không tính là hồi quy:
  `ReviewBasketIntegrationTest.aWrongAnswerInTheMainFlowUpgradesTheOptionalReviewOfThatPointToBlocking` và
  `StudyContextTest.lessonUsesExistingGateAndTrustedCourse` (learning-service).
- Không đặt mã plan, phase hay "S1..S10" vào comment, tên test hay commit message; mô tả trực tiếp hành vi.

## Đồng bộ trạng thái — 2026-10-21

Chức năng backend/frontend đã có; nghiệm thu **15/18 tiêu chí (83%)**, 1/4 phase hoàn tất. Giữ `in-progress`: còn xác nhận package, gate test frontend và rà phạm vi diff. Việc chưa kiểm tra thủ công được ghi rõ, không tính là đã chạy E2E thật.

Chi tiết: [báo cáo đồng bộ](./reports/pm-2026-10-21-writing-plan-status.md).
