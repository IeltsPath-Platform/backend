---
title: "Màn soạn bài học: chọn câu hỏi cho block Bài tập (lọc, tìm, phân trang, đề, đánh dấu câu đã có chủ) và cảnh báo KP tiên quyết"
description: "Làm lại QuestionChooser của form Thêm/Sửa block Bài tập (5 việc) và thêm cảnh báo Knowledge point tiên quyết còn thiếu ở ô Knowledge point của bài (việc 6); từ rà soát ngày 2026-11-23. Bốn việc chỉ cần frontend, hai việc cần thêm dữ liệu từ API danh sách câu hỏi"
status: done
priority: P2
branch: "feat/lesson-editor-pickers (frontend + backend)"
tags: [content-service, frontend, admin, lessons, question-picker, knowledge-prerequisite]
blockedBy: []
blocks: []
created: "2026-11-23T22:20:00.000+07:00"
createdBy: "Claude Code"
source: "hội thoại 2026-11-23: màn Thêm block: Bài tập chỉ hiện loại câu, kỹ năng và 8 ký tự ID; câu đã dùng vẫn tick được; chỉ lấy 100 câu đầu; gắn KP cho bài không báo khi thiếu KP tiên quyết"
---

# Màn soạn bài học: hai ô chọn

Repo: `frontend` (chính) và `backend/services/content-service` (hai việc cần dữ liệu thêm). Hai ô chọn của panel soạn bài học: **chọn câu hỏi** cho block Bài tập (việc 1-5) và **chọn Knowledge point** của bài (việc 6). Không liên quan plan [261123-2134-lesson-slide-pdf-upload](../261123-2134-lesson-slide-pdf-upload/plan.md); hai plan không chung file.

## Sáu việc

| # | Việc | Cần backend? | Phase |
|---|---|---|---|
| 2 | Tự lọc theo kỹ năng của chủ đề | Không | 01 |
| 4 | Tìm theo mã câu hỏi và phân trang (hiện chỉ lấy 100 câu đầu) | Không (backend đã có lọc `code` và `page`) | 01 |
| 5 | Bố cục: danh sách cao cố định 280px, panel bỏ trống | Không | 01 |
| 1 | Làm mờ câu đã thuộc block hoặc phần thi khác, không cho tick, cộng công tắc "Chỉ hiện câu còn trống" | Có | 02 + 03 |
| 3 | Hiện đề rút gọn và Knowledge point ngay trong dòng | Có | 02 + 03 |
| 6 | Cảnh báo khi KP đã chọn có KP tiên quyết chưa nằm trong bài, kèm nút "Thêm tiên quyết" | Không (cây tri thức đã trả `edges`) | 04 |

Phase 01 và 04 chỉ cần frontend, giao độc lập ngay. Phase 02 và 03 làm tiếp theo thứ tự (03 cần 02). Thứ tự khuyên: 01, 04, 02, 03.

## Hiện trạng đã kiểm

- `QuestionChooser.tsx` gọi `listQuestions({ status: 'PUBLISHED', purpose, skill, size: 100 })`; 100 cũng là trần của backend (`ListQuestionsUseCase.MAX_PAGE_SIZE`), nên không có trang 2.
- Mỗi dòng hiện `id.slice(0, 8)` dù câu hỏi đã có `code` ổn định (V147) và backend lọc được theo `code` (khớp một phần, không phân biệt hoa thường).
- `BlockEditor` có `skill` của chủ đề nhưng chỉ truyền cho ô Knowledge point, không truyền cho `QuestionChooser`.
- Backend từ chối lúc Lưu nếu câu đã nằm ở block khác hoặc `section_questions` (`questionVersionsOwnedElsewhere`, `QuestionAlreadyOwnedException`). Giao diện không báo trước.
- API danh sách không trả đề (`stem`) hay Knowledge point; `QuestionResponse` chỉ có id, loại, kỹ năng, trạng thái, phiên bản đã xuất bản, mục đích, `importRef`, `code`.
- `QuestionChooser` còn được `packages/components/SectionQuestionsEditor.tsx` dùng (chọn một câu, mục đích `EXAM`/`PLACEMENT`). Mọi thay đổi phải **không đổi hành vi** ở đó (xem phase 03).
- Phát hiện ngoài phạm vi: `QuestionFilters.knowledgePointId` ở frontend được gửi nhưng `QuestionController` không nhận tham số này, nên bị bỏ qua. Không sửa ở plan này.
- Ràng buộc sẵn có: chỉ bài `DRAFT` mới thêm, sửa, xóa được block bài tập (`ensureExerciseEditable`); giữ nguyên.
- Việc 6: gắn KP cho bài chỉ chọn đúng KP được tick, không kéo theo KP tiên quyết và không báo gì (đã kiểm: dialog chỉ thêm hoặc bỏ đúng `id`, use case lưu bài không đọc cạnh tiên quyết). `LessonKnowledgeField` đã tải `fetchKnowledgeTree` và phản hồi có sẵn `edges` (`from` là KP tiên quyết, `to` là KP phụ thuộc), nên cảnh báo tính hoàn toàn ở frontend, không thêm lời gọi và không đổi backend.

## Quyết định (đã xác nhận 2026-11-23)

| Chủ đề | Quyết định | Lý do |
|---|---|---|
| Câu đã có chủ | **Làm mờ, vô hiệu hóa checkbox, kèm nhãn "Đã dùng ở nơi khác" và thêm công tắc "Chỉ hiện câu còn trống" (mặc định tắt)** lọc ở server | Lọc ở server để số trang và tổng số vẫn đúng; ẩn ở client sẽ làm trang ngắn |
| Kỹ năng mặc định | Mặc định theo kỹ năng chủ đề, vẫn đổi được sang "Tất cả" hoặc kỹ năng khác | Một bài có thể chứa nhiều kỹ năng (`AGENTS.md` §3.8) |
| Tìm kiếm | Chỉ theo **mã câu hỏi** | Tìm theo nội dung đề cần ILIKE trên bảng phiên bản, không làm |
| Knowledge point trong dòng | Hiện **mã**, tên đầy đủ trong tooltip (`title`) | Dòng đã có đề rút gọn, giữ một dòng nhãn gọn |
| Cỡ trang | 25 câu | Mỗi dòng giờ cao hơn (đề + Knowledge point) |
| Dữ liệu thêm cho danh sách | Tham số tùy chọn `include=ownership,preview`, cộng `onlyFree` và `exceptBlockId` | Chỉ màn chọn câu trả thêm dữ liệu; Ngân hàng câu hỏi và `SectionQuestionsEditor` không đổi hành vi và không tốn truy vấn thêm |
| "Xem nội dung" | Giữ để xem đề đầy đủ; dòng chỉ hiện đoạn đầu | Đề Reading và Listening dài |
| Thông báo bài đã khóa | Bạn chọn "có thêm", nhưng **đã có sẵn** nên chỉ thêm test hồi quy, không viết code mới (xem bên dưới) | `LessonEditorPanel.tsx` dòng 180-202 đã hiện ghi chú khóa và vô hiệu hóa nút "Bài tập" khi bài không còn là nháp |

### Về thông báo bài đã khóa

Lúc hỏi, tôi nói admin phải chờ lỗi khi Lưu. Điều đó sai: ở bài không còn `DRAFT`, panel hiện ghi chú `EXERCISE_LOCK_NOTE`, nút "Bài tập" bị vô hiệu hóa kèm lời giải thích, và block bài tập có sẵn không sửa hay xóa được. Form chọn câu hỏi không mở được ở bài đã khóa nên không cần thông báo trong form. Chỉ còn một trường hợp hẹp: bài được xuất bản ở nơi khác trong lúc form đang mở. Plan chỉ thêm test hồi quy cho hành vi hiện có (phase 01) và không làm gì cho trường hợp đua đó; backend vẫn từ chối khi Lưu. Nếu bạn muốn xử lý cả trường hợp đua đó, nói tôi.

## Phases

| Phase | Nội dung | Trạng thái |
|---|---|---|
| [01](phase-01-chooser-frontend-quick-wins.md) | Frontend: lọc kỹ năng mặc định, hiện mã, tìm theo mã, phân trang, bố cục | done |
| [02](phase-02-backend-question-list-include.md) | Backend: `include=ownership,preview` cho danh sách câu hỏi, contract, test | done |
| [03](phase-03-chooser-frontend-ownership-preview.md) | Frontend: dùng `ownedElsewhere`, đề rút gọn, Knowledge point | done, phụ thuộc 02 |
| [04](phase-04-lesson-kp-prerequisite-warning.md) | Frontend: cảnh báo KP tiên quyết còn thiếu ở ô Knowledge point của bài | done, độc lập |

## Tiêu chí hoàn thành

- Mở "Thêm block: Bài tập" trong bài thuộc chủ đề Listening: danh sách tự lọc Listening, đổi được sang Tất cả.
- Mỗi dòng hiện mã câu hỏi, loại, kỹ năng, đoạn đầu của đề và các Knowledge point.
- Câu đã dùng ở block hoặc phần thi khác bị mờ, không tick được, trừ câu đang thuộc chính block này khi sửa.
- Tìm theo mã và chuyển trang chạy; câu đã chọn ở trang khác vẫn hiện đúng tên trong mục "Câu hỏi đã chọn".
- Vùng danh sách dùng hết chiều cao còn lại của panel, không bỏ trống.
- `SectionQuestionsEditor` (phần thi) hoạt động y như trước.
- Chọn KP của bài mà KP đó có tiên quyết chưa nằm trong bài: hiện cảnh báo nêu KP nào thiếu tiên quyết nào, kèm nút "Thêm tiên quyết"; bỏ cảnh báo khi đã đủ. Không chặn Lưu.
- Backend và frontend xanh (lệnh ở từng phase); lệnh nào không chạy được thì nêu rõ.

## Rủi ro chính

1. **Làm hỏng `SectionQuestionsEditor`**: dùng chung `QuestionChooser`. Mọi tính năng mới là tùy chọn, mặc định tắt; có test cho cả hai nơi dùng.
2. **N+1 truy vấn** khi thêm đề và Knowledge point cho 25 dòng: phải truy vấn theo tập (một truy vấn cho cả trang), theo `AGENTS.md` §3.7.
3. **Đổi contract công khai**: thêm trường và tham số mới, không đổi trường cũ; cập nhật `question-publication-v1.md` cùng PR.
4. **Đua dữ liệu**: `ownedElsewhere` chỉ là gợi ý lúc chọn; backend vẫn từ chối ở lúc Lưu như hiện nay (không bỏ kiểm tra đó).

## Câu hỏi còn mở

- Việc 6, phạm vi: plan chỉ cảnh báo ở ô "Knowledge point của bài" (cấp bài), không cảnh báo ở ô KP của block Văn bản, vì tiên quyết có thể nằm ở block khác của cùng bài. Nếu bạn muốn cả block, nói tôi.
- Việc 6, độ sâu: chỉ xét tiên quyết **trực tiếp** (một tầng). Thêm tiên quyết xong thì tiên quyết đó lại có thể báo tiên quyết của nó, nên chuỗi dài sẽ hiện dần. Tính cả chuỗi dễ vượt giới hạn 50 KP của bài.

- Đã chốt 2026-11-23: bài được xuất bản ở nơi khác trong lúc form đang mở thì **không** xử lý thêm, backend từ chối khi Lưu.
- Chỉ mục trên `lesson_block_questions(question_version_id)` và `section_questions(question_version_id)`: cần kiểm ở phase 02 vì công tắc "Chỉ hiện câu còn trống" thêm điều kiện NOT EXISTS vào truy vấn danh sách. Nếu thiếu chỉ mục thì cần migration, và việc đó cần bạn duyệt.
