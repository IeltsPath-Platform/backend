# Phase 01 - Bịt lỗ hổng sẵn có

Phủ: E24, E25, E37. Không thêm tính năng mới; hành vi ba loại bản nháp hiện có giữ nguyên ngoài các sửa lỗi dưới đây.

## Bước 0 - đo dữ liệu (chỉ đọc)

```sql
SELECT p.code, p.package_type, v.version_number, v.status, count(*) AS cau_chua_xuat_ban
FROM content_packages p
JOIN content_package_versions v ON v.package_id = p.id
JOIN content_sections s ON s.package_version_id = v.id
JOIN section_questions sq ON sq.section_id = s.id
JOIN question_versions qv ON qv.id = sq.question_version_id
WHERE qv.status <> 'PUBLISHED'
GROUP BY 1, 2, 3, 4 ORDER BY 1, 3;
```

Có bản `PUBLISHED` trong kết quả thì dừng, báo chủ dự án trước khi bật chặn (quyết định 1 trong plan.md). Chạy thêm trên schema dựng từ
toàn bộ migration để biết seed có vi phạm không.

## 1.1 Chặn xuất bản đề còn câu chưa xuất bản

- `LearningContentReader.unpublishedQuestions(packageVersionId)` + `JdbcLearningContentReader`: trả `question_id, code, section title`
  của các dòng có `qv.status <> 'PUBLISHED'`.
- `PublishContentPackageUseCase.validate`: gọi **sau** `lockQuestionsForPublishing` (khóa `questions`, cùng khóa mà xuất bản
  và lưu trữ câu dùng, nên trạng thái không đổi giữa lúc kiểm và lúc commit). Áp cho sáu loại đề trong `switch`.
- `QuestionNotPublishedException` -> `422`, `details.code = QUESTION_NOT_PUBLISHED`, `details.questions = [{id, code, section}]`,
  câu tiếng Việt "Đề còn N câu hỏi chưa xuất bản. Xuất bản các câu đó trước."
- `ScheduledPublishJob`: ánh xạ ngoại lệ sang `QUESTION_NOT_PUBLISHED`. `validateForPublish` (lúc đặt lịch) dùng chung nên cũng bị chặn.
- Câu `ARCHIVED` cũng bị chặn. Thông báo phân biệt nháp và lưu trữ.

## 1.2 Lưu bản nháp AI có điều kiện

Migration mới (số kế tiếp tại thời điểm làm; hiện `V150`):

```sql
ALTER TABLE ai_drafts ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE ai_drafts ADD COLUMN claim_token UUID;
```

- `claimQueued`: mỗi dòng nhận `claim_token = gen_random_uuid()`, `attempt_count + 1`; trả cặp `(id, claim_token)`.
- `completeIfClaimed(id, token, result, model, promptVersion, now)` và `failIfClaimed(id, token, code, now)`:
  `UPDATE ... WHERE id = ? AND status = 'GENERATING' AND claim_token = ?`; trả số dòng.
- `RunAiDraftJobUseCase`: hoàn lượt **chỉ khi** `failIfClaimed` trả 1, trong cùng transaction với nó (bean riêng để
  `@Transactional` có hiệu lực). Trả 0 thì log id, bỏ qua.
- Job không gọi `save()` nữa, nên bản nháp đã xóa không bị chèn lại. `save()` vẫn dùng cho tạo mới và duyệt.
- `AiDraftRepository.findByIdForUpdate`: dùng ở sửa, từ chối, duyệt của **mọi** kind (hiện dùng `findById`).

## 1.3 Job an toàn

- Claim từng bản: vòng lặp tối đa `batch-size`, mỗi vòng `claimQueued(1)` rồi xử lý, để `started_at` là lúc chạy thật.
- `requeueStaleGenerating`: `attempt_count >= content.ai.max-attempts` (mặc định 3) thì chuyển `FAILED INTERNAL` và hoàn lượt
  cùng transaction; còn lại về `QUEUED`, xóa `claim_token`. Câu lệnh `UPDATE ... RETURNING` để mỗi dòng chỉ một instance xử lý.
- Kiểm thời gian xấu nhất của một lần gọi LLM (timeout 180 giây và số lần thử lại thực tế trong `common-llm`) rồi đặt
  `content.ai.stale-after` lớn hơn. Tài liệu ghi thử lại sau 5 và 15 giây nhưng chưa thấy chỗ đó trong code: kiểm lại, sửa tài liệu nếu sai.
- Cập nhật bảng "Scheduler và nhiều instance" trong `docs/system-architecture.md` nếu dòng của job này đổi.

## Test

- Testcontainers: đề có câu nháp/lưu trữ bị chặn; đề toàn câu đã xuất bản thì qua; lưu trữ câu đồng thời với xuất bản đề chỉ một bên thắng.
- Testcontainers: worker cũ hoàn tất sau khi bị thay thế thì không ghi đè; `FAILED` đến muộn không đè `READY`/`APPROVED`;
  hoàn lượt đúng một lần; xóa bản nháp lúc đang sinh thì không hồi sinh; quá số lần thì `FAILED` và hoàn lượt; hai instance claim song song không trùng.
- `ScheduledPublishJob` ghi đúng mã. Chạy lại toàn bộ test hiện có của ba kind.

## Rủi ro và lùi lại

- Đề cũ bị chặn xuất bản lại: do bước 0 phát hiện. Lùi: bỏ lời gọi `unpublishedQuestions`.
- Hai cột mới có giá trị mặc định nên bản backend cũ vẫn chạy được.
