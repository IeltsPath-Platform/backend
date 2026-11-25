# Phase 03 - Duyệt và gắn vào phần

Phủ: E4, E26..E32, E34..E36, E38.

## API (chỉ chủ bản nháp, người khác nhận `404`)

| Method | Path (sau `/api/content/admin/ai-drafts/section-questions`) | Body |
| --- | --- | --- |
| GET | `/{draftId}` | trả bản nháp + `sourceChanged`, `sectionState` (`OK`, `MISSING`, `VERSION_NOT_DRAFT`, `SKILL_CHANGED`) |
| PATCH | `/{draftId}/items/{itemId}` | `{ revision, stem?, explanation?, options?, correct?, accepted? }` |
| DELETE | `/{draftId}/items/{itemId}?revision=` | |
| POST | `/{draftId}/approve` | `{ itemIds, revision, acceptSourceChange }` |
| POST | `/{draftId}/reject` | `{ note }`, dùng lại quy tắc hiện có |

Route theo chỉ số của bản nháp theo KP giữ nguyên và từ chối kind mới (đã kiểm `kind`).

## Sửa và bỏ câu

- `findByIdForUpdate`; trạng thái `READY`; `revision` khớp, lệch -> `409 DRAFT_CHANGED` kèm `details.revision`. Mỗi lần ghi `revision + 1`.
- Sửa xong chạy lại **toàn bộ** `SectionQuestionChecks` trên bản chụp nguồn, câu hiện có của phần và các câu khác trong bản
  nháp. Sai -> `422 INVALID_DRAFT_ITEM` kèm `details.reason`, `details.with`; không ghi gì.

## Duyệt - `ApproveSectionQuestionDraftUseCase` (một transaction)

Thứ tự khóa cố định: gói trước, bản nháp sau, để không deadlock với thêm/gỡ câu thủ công.

1. Đọc bản nháp không khóa để biết `packageId`, kiểm chủ sở hữu và kind.
2. Khóa gói, rồi khóa bản nháp và đọc lại.
3. Đã `APPROVED`: cùng tập `itemIds` -> trả kết quả cũ; khác -> `409 DRAFT_ALREADY_REVIEWED`. Trạng thái khác `READY` -> `409`.
4. `revision` khớp; `itemIds` không rỗng, không lặp, đều tồn tại.
5. Bản đề còn nháp, phần còn trong bản đề (`404`), kỹ năng như lúc yêu cầu (`409 SECTION_CHANGED`), loại đề còn được hỗ trợ.
6. Đọc lại nguồn, so SHA-256. Khác và không có `acceptSourceChange` -> `409 SECTION_SOURCE_CHANGED`. Có thì các kiểm tra bước 7
   chạy trên **nguồn mới**; nguồn đã bị gỡ hẳn thì luôn từ chối.
7. Chạy lại kiểm tra cho từng câu chọn với câu hiện tại của phần. Trùng -> `409 DRAFT_ITEMS_DUPLICATED`, `details.items = [{itemId, with}]`.
   Lý do khác -> `422 INVALID_DRAFT_ITEM`. Gom **hết** lỗi rồi trả một lần.
8. Theo thứ tự trong bản nháp: `CreateQuestionUseCase` (dạng, `skill` của phần, `purpose` theo loại đề, `importRef = ai-{draftId}-{itemId}`),
   `AddQuestionVersionUseCase` (phiên bản 1, `MEDIUM`, không KP), rồi gắn vào phần bằng method của `ContentSection`, điểm 1.
   Chú ý flush giữa JPA và JDBC như `JdbcPackageVersionCopier`.
9. Ghi `result.approved = [{itemId, questionId, questionVersionId, sectionQuestionId}]`, bản nháp `APPROVED`.

Lỗi ở bất kỳ bước nào rollback hết; bản nháp vẫn `READY` (E32). Câu không được chọn nằm lại trong bản nháp đã duyệt để tra cứu.
`importRef` duy nhất là chốt cuối chống tạo đôi.

## Xuất bản loạt câu nháp

`POST /api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}/questions/publish-drafts`

- Khóa gói, kiểm bản đề nháp; lấy các `question_version` đang `DRAFT` của phần; khóa `questions` theo `id` tăng dần.
- Dùng cùng kiểm tra với `PublishQuestionVersionUseCase` (tách phần kiểm ra để dùng chung, không chép). Kiểm hết trước, có lỗi
  thì `422 QUESTION_VERSION_NOT_PUBLISHABLE` kèm danh sách câu và không câu nào đổi.
- Không có câu nháp -> `200`, số lượng 0 (gọi lại an toàn). Câu đang dùng ở nơi khác vẫn xuất bản được; trùng sở hữu do xuất bản đề kiểm.

## Dọn bản nháp bỏ quên

Trong job hiện có: `UPDATE ai_drafts SET status = 'EXPIRED' WHERE kind = 'SECTION_QUESTIONS' AND status = 'READY' AND finished_at < now - 14 ngày`
(`content.ai.section-questions.ready-ttl`). Lặp an toàn, nhiều instance chạy được. Không hoàn lượt vì người soạn đã nhận kết quả.

## Test

- Testcontainers: duyệt thành công (câu `DRAFT`, đúng purpose cho từng loại đề, đúng kỹ năng, thứ tự nối tiếp, điểm 1);
  hai lần duyệt song song chỉ tạo một bộ câu; gọi lại trả kết quả cũ; câu thứ hai lỗi thì không còn dòng nào ở cả ba bảng.
- Phần đổi nguồn/kỹ năng/bị xóa, bản đề được xuất bản, phần có thêm câu trùng giữa lúc sinh và duyệt.
- Duyệt song song với thêm câu thủ công: không deadlock, không trùng thứ tự.
- Sửa với revision cũ; sửa thành trùng; bỏ hết câu rồi duyệt.
- Luồng trọn: sinh (LLM giả) -> duyệt -> xuất bản đề bị chặn -> xuất bản loạt -> xuất bản đề qua.
- Hết hạn: mở khóa được phần; bản `APPROVED` không bị đụng.

## Rủi ro và lùi lại

- Lùi: tắt nút ở frontend và gỡ route; dữ liệu đã duyệt là câu hỏi bình thường.
- Không chạy lại bản backend cũ khi bảng còn dòng `SECTION_QUESTIONS` hoặc `EXPIRED` (xem phase 05).
