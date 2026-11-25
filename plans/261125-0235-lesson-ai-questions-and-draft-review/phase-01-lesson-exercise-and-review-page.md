# Phase 01 - Sinh câu hỏi trong bài học và trang bản nháp chỉ duyệt

Phủ: L1..L11. Phụ thuộc plan `261124-2200-section-ai-question-generation`: phase 03 (khóa, `itemId`, `revision`) và phase 04
(`DraftReview.tsx`).

## Hiện trạng (đọc code 2026-11-25)

- `ApproveQuestionDraftUseCase` tạo câu `skill = null`; ô chọn câu của bài học mở sẵn theo kỹ năng của chủ đề nên câu bị ẩn.
  Aggregate `Question` không có method đổi kỹ năng sau khi tạo.
- Lưu và xuất bản bài học không so điểm kiến thức (KP) của câu với KP của bài. Ô chọn câu không lọc được theo KP.
- Block `EXERCISE` phải có ít nhất một câu (`LessonBlock`), chỉ sửa được khi bài `DRAFT` (`Lesson.ensureExerciseEditable`).
- Bài học đã chặn xuất bản khi còn câu nháp (`EXERCISE_QUESTION_NOT_PUBLISHED`), không cần thêm chặn như phase 01 của plan sinh câu cho phần của đề.
- `BlockEditor` giữ danh sách câu trong state rồi gửi **cả danh sách** khi lưu.
- `QuestionDraftGenerator` đã loại câu trùng stem giống hệt với câu sẵn có của KP và trong cùng lượt.

## Backend

Dùng lại kind `QUESTIONS`; yêu cầu thêm `lessonId` (bắt buộc với yêu cầu mới) và `blockId` (tùy chọn). Bản nháp cũ không có
`lessonId` vẫn xem và duyệt được như trước (vào ngân hàng), chỉ thêm kỹ năng.

1. Tiếp nhận: bài tồn tại và `DRAFT`; KP `ACTIVE` và thuộc KP của bài; có `blockId` thì block thuộc bài và là `EXERCISE`.
   Độ khó do server đặt `MEDIUM`, client không gửi nữa (vẫn đọc được yêu cầu cũ).
2. Mỗi người tối đa một lượt chưa đóng cho mỗi bài: unique index theo `request ->> 'lessonId'` -> `409 LESSON_DRAFT_OPEN` kèm
   `details.draftId`. `GET /ai-drafts/questions/open?lessonId=`.
3. Prompt `QUESTIONS-v3`: thêm câu đã có của KP (tối đa 30, cắt 300 ký tự) dạng JSON dữ liệu; so trùng bằng `QuestionSimilarity`.
   Không có đoạn văn nên không chạy các kiểm tra "có trong nguồn".
4. Kỹ năng của câu: kỹ năng chủ đề của bài; không có thì kỹ năng của KP; `ALL` hoặc trống thì để trống. Bản nháp cũ: kỹ năng của KP.
5. Duyệt trong một transaction, khóa **bài học trước, bản nháp sau**: bài còn `DRAFT`, KP còn thuộc bài, block còn đó, kiểm trùng lại;
   tạo câu `LEARNING`, nối vào cuối block. Không có `blockId` thì tạo block `EXERCISE` mới ở cuối bài. Lặp lại cùng danh sách trả kết quả cũ.
6. API sửa/bỏ/duyệt của `QUESTIONS` chuyển sang `itemId` + `revision`. Bản nháp `READY` cũ chưa có `itemId`: gán ở lần đọc có khóa đầu tiên.
7. `POST /lessons/{lessonId}/questions/publish-drafts`: cùng luật với bản của phần (tất cả hoặc không câu nào).
8. `GET /admin/questions` thêm `knowledgePointIds` (lặp tham số, tối đa 50), lọc theo phiên bản đang xuất bản, ở database.
9. Gateway: tier `ai` cho `POST /ai-drafts/questions`.

### API danh sách bản nháp

- `kind` nhận nhiều giá trị. Mỗi dòng có `origin { type: SECTION|LESSON|BANK, title, packageId?, versionId?, sectionId?, topicId?,
  lessonId?, exists }`, `itemCount`, `knowledgePointName?`. Tra theo tập, không N+1; không trả `source.text`.
- Nơi gốc đã bị xóa: `exists = false`, vẫn xem và từ chối được, không duyệt được.

## Frontend

### Trong bài học

- Nút "Sinh câu hỏi bằng AI" ở đầu danh sách block (tạo block mới) và trên từng block bài tập; **không** đặt trong form sửa block vì
  form đó gửi lại cả danh sách câu. Có form block đang mở thì nút tắt.
- Trạng thái nút như ở phần của đề. Ẩn khi bài không còn `DRAFT`; bài chưa có KP thì tắt, `title` nêu lý do.
- Modal yêu cầu: KP (chỉ KP của bài, chọn một; chỉ có một thì chọn sẵn), dạng câu, số câu 1..10, ghi chú. Không độ khó, không chữ giải thích.
- Bài có câu nháp: nút "Xuất bản N câu nháp".
- Ô chọn câu: công tắc "Chỉ câu thuộc điểm kiến thức của bài", mặc định bật khi bài có KP. Nơi khác dùng `QuestionChooser` không đổi.

### Trang bản nháp (`/content-author/ai-drafts`)

- Đổi tên menu và tiêu đề thành **"Duyệt câu hỏi AI"**. Bỏ nút "Sinh câu hỏi", drawer tạo và `?new=questions`; xóa `QuestionDraftForm`
  nếu không còn ai dùng.
- Danh sách gồm câu cho phần của đề và câu cho bài học. Mặc định lọc "Chờ duyệt"; có "Tất cả trạng thái". Mỗi dòng: nơi dùng
  (`Phần X · đề Y` hoặc `Bài Z · KP`), số câu, trạng thái, thời gian. Không hiện UUID.
- Trang chi tiết duyệt được **cả hai loại**, dùng chung thành phần duyệt với modal tại chỗ; có link "Mở phần" hoặc "Mở bài học".
  Lượt lỗi: link về nơi gốc để sinh lại. Kind lạ: báo không hỗ trợ, không vỡ trang.
- Link "Mở bài học": hiện chưa có đường dẫn nào mở thẳng một bài (chỉ có ngăn mở từ cây chủ đề trong khóa, không dùng query). Thêm
  `/content-author/courses?lesson=<id>`: trang khóa học mở `LessonEditorDrawer` của bài đó; đóng ngăn thì bỏ query. Bài không còn: báo lỗi trong trang.
  Sinh câu và duyệt xong trong ngăn thì ở lại ngăn, không chuyển trang.
- `workQueueLabels.ts` bỏ link "Sinh câu cho KP này" (trỏ `?new=questions`). Việc `CONTENT_GAP` là thiếu gói luyện, đã có link
  "Tạo gói luyện"; sinh câu ngay trong phần của gói đó.

## Trường hợp ngoại lệ

| Mã | Trường hợp | Xử lý |
| --- | --- | --- |
| L1 | Bài đã xuất bản, lưu trữ hoặc bị xóa trước khi duyệt | `409 LESSON_CHANGED` / `404`; không tạo câu nào |
| L2 | KP bị gỡ khỏi bài hoặc ngừng dùng khi chờ duyệt | `409 LESSON_CHANGED` kèm lý do |
| L3 | Block đích bị xóa khi chờ duyệt | `409 BLOCK_MISSING`; giao diện cho duyệt vào block mới |
| L4 | Bấm đôi, hai tab cùng yêu cầu | Unique index, `409 LESSON_DRAFT_OPEN`, mở lại lượt đó, trừ một lượt |
| L5 | Duyệt ở trang bản nháp và ở bài học cùng lúc | Khóa dòng; một bên thắng, bên kia nhận kết quả cũ hoặc `409` |
| L6 | Tab cũ lưu form block sau khi câu AI đã vào block | Form gửi `expectedQuestionVersionIds`; lệch -> `409 BLOCK_CHANGED`, không gỡ câu ngầm |
| L7 | Chủ đề và KP đều không có kỹ năng cụ thể | Câu để trống kỹ năng; ô chọn câu thấy ở "Tất cả" |
| L8 | Bản nháp cũ thiếu `lessonId`, `itemId` hoặc `revision` | Vẫn mở, sửa, duyệt vào ngân hàng |
| L9 | Nơi gốc bị xóa | `exists = false`, chỉ từ chối được |
| L10 | Danh sách gặp kind hoặc status lạ | Bỏ qua dòng đó, phần còn lại vẫn hiện |
| L11 | Bỏ quên bản nháp của bài học | Hết hạn 14 ngày như phần của đề (chỉ bản có `lessonId`) |

## Test

- Backend (Testcontainers): từng dòng L1..L9, L11; kỹ năng theo chủ đề, theo KP, `ALL`; nối vào block và tạo block mới; duyệt song song
  với sửa block không deadlock; xuất bản loạt; lọc theo KP; danh sách không N+1.
- Frontend: trạng thái nút, modal, công tắc lọc KP, trang bản nháp không còn nút tạo, lọc mặc định, duyệt cả hai loại, dòng lạ, link nơi
  gốc; cập nhật `adminMenu*.test.ts`, `AdminSidebar.test.tsx`, `AdminRoutes.test.tsx`, `AiDraftsPage.test.tsx`, `AiDraftDetailPage.test.tsx`, test hàng việc.

### Ánh xạ L1..L11 sang test

| Trường hợp | Test |
| --- | --- |
| L1 | `ApproveLessonQuestionDraftUseCaseTest.refusesApprovalAfterTheLessonHasBeenPublished` |
| L2 | `ApproveLessonQuestionDraftUseCaseTest.refusesApprovalAfterTheKnowledgePointWasRemovedFromTheLesson` |
| L3 | `ApproveLessonQuestionDraftUseCaseTest.offersAConflictWhenTheRequestedExerciseBlockWasRemoved`; `AiDraftDetailPage` test `offers a new exercise block when the requested block was removed` |
| L4 | `JdbcAiDraftStoreTest.anAuthorCanHaveOnlyOneOpenQuestionDraftForALesson` |
| L5 | `ApproveLessonQuestionDraftUseCaseTest.locksLessonBeforeDraftAndUsesTopicSkillBeforeKnowledgePointSkill` |
| L6 | `LessonAuthoringUseCaseTest.refusesAStaleExerciseQuestionSnapshotBeforeReplacingTheBlock` |
| L7 | `ApproveLessonQuestionDraftUseCaseTest.leavesSkillEmptyWhenTopicAndKnowledgePointHaveNoSpecificSkill` |
| L8 | `EditDraftItemUseCaseTest.anOlderQuestionDraftGetsStableItemIdsWhenItIsEdited`; `ApproveQuestionDraftUseCaseTest.approvedItemBecomesADraftQuestionWithAStableReference` |
| L9 | `JdbcAiDraftStoreTest.deletedLessonSourcesRemainVisibleAsUnavailableOrigins` |
| L10 | `aiDraftApi.test.ts` test `sends repeatable kinds and skips rows with unknown kinds or statuses` |
| L11 | `JdbcAiDraftStoreTest.readyLessonQuestionDraftsExpireAfterTheirRetentionPeriod` |

### Kết quả kiểm tra

- `mvn -q -pl services/content-service -am test`: đạt, 1,448 test qua, 0 lỗi/thất bại; 1 test tùy chọn ghi template bị bỏ qua do chưa đặt `writeImportTemplate`.
- `mvn -q test` tại `infra/api-gateway`: đạt.
- `ArchitectureTest`: đạt trong lượt Maven đầy đủ.
- `npx vitest run src/features/admin`: đạt, 82 file / 923 test.
- `npx eslint src/features/admin` và `npx tsc -b`: đạt.
- `graphify update .`: đạt ở hai repo; backend bỏ qua 365 file SQL vì môi trường thiếu `tree_sitter_sql`, frontend không có thay đổi topology.
- Còn chờ: thử sinh câu với LLM thật, xác minh trực tiếp trên trình duyệt và đếm câu đã duyệt `skill = null` trong cơ sở dữ liệu ứng dụng. Cơ sở dữ liệu test không dùng làm đại diện cho dữ liệu ứng dụng.

## Rủi ro và lùi lại

- `expectedQuestionVersionIds` tùy chọn ở server để frontend cũ vẫn lưu được block; frontend mới luôn gửi.
- Câu đã duyệt trước đây đang `skill = null`: không sửa dữ liệu tự động; đếm và báo chủ dự án trước.
- Deploy theo phase 05 của plan đó (frontend lên trước). Frontend mới gọi backend cũ: `lessonId` bị bỏ qua nên nút trong bài học phải ẩn khi
  `GET /ai-drafts/questions/open` trả `404`.
