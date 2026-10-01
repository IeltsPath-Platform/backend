---
phase: 1
title: "Contract và content: cột hint"
status: pending
priority: P2
dependencies: []
effort: "1 ngày"
---

# Phase 1: Contract và content: cột hint

<!-- Updated: Validation Session 1 - hint rỗng lưu NULL -->
<!-- Updated: Validation Session 2 - câu CHOICE từ 3 phương án (kể cả TFNG) có gợi ý; chỉ câu 2 phương án không; seed điền 7/8 câu -->


## Context Links

- `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql:127-141` (`question_versions`)
- `domain/entity/QuestionVersion.java` (constructor 13 tham số, `create(...)`)
- `api/dto/request/AddQuestionVersionRequest.java`, `application/command/AddQuestionVersionCommand.java`,
  `application/usecase/AddQuestionVersionUseCase.java:33`
- `application/result/QuestionVersionResult.java`, `api/dto/response/QuestionVersionResponse.java`
- `infrastructure/persistence/entity/QuestionVersionJpaEntity.java:50`, `infrastructure/persistence/mapper/QuestionPersistenceMapper.java:91,125`
- Plan 1640 phase 3: `GET /internal/learning-content/lessons/{id}`, seed V9 (câu bài học Q1, Q3, Q4, Q5, Q11, Q12, Q13, QT1)
- Plan 1640 phase 1: `lesson-learning-v1.md`, `learning-content-internal-v1.md`, `answer-spec-v1.md`

## Overview

Thêm cột `hint`, cho người soạn nhập qua API admin, trả qua endpoint nội bộ của bài học, và điền gợi ý cho câu bài học Reading
đủ điều kiện.

## Requirements

**Câu đủ điều kiện có gợi ý** (ghi trong contract; ai-learning áp dụng lúc chạy, test seed của content kiểm dữ liệu):
- `answerSpec.type = FILL`; hoặc
- `answerSpec.type = CHOICE` và `options` có **từ 3 phần tử** trở lên; hoặc
- `answerSpec.type = CHOICE` và `options` rỗng hoặc `NULL`: đó là True/False/Not Given, tính là 3 lựa chọn.
- Chỉ câu chọn **2 phương án** không đủ điều kiện (sai một lần thì chỉ còn đúng một lựa chọn).

**Contract (sửa file 1640 tạo, chỉ thêm):**
- `learning-content-internal-v1.md`: mỗi câu trong `GET /lessons/{id}` thêm `hint` (string hoặc `null`). Ghi rõ `GET /package-versions/{id}` không có `hint`.
- `lesson-learning-v1.md`:
  - câu hỏi của `GET /lessons/{id}` thêm `hint` vào danh sách trường cho phép;
  - `results[]` của response nộp khối thêm `hint`;
  - luật: `hint` khác `null` chỉ khi câu đủ điều kiện (như trên) và học viên đã làm sai câu đó ít nhất một lần trong khối chưa đạt;
    giữ tới khi khối đạt; bài ôn không trả gợi ý;
  - luật soạn: gợi ý không chứa đáp án, không chép nguyên câu chứa đáp án; tối đa 500 ký tự.

**Migration** `V13__question_version_hint.sql` (số cố định của lộ trình, sau V12 của Listening; không sửa V1–V12):
```sql
ALTER TABLE question_versions ADD COLUMN hint TEXT;

-- Gợi ý cho câu trong bài tập bài Reading (seed demo). Gợi ý chỉ chỗ đọc lại, không nói đáp án.
UPDATE question_versions SET hint = '...' WHERE id = '<id câu trong V9>';
-- ... một lệnh cho mỗi câu trong bảng dưới
```
- 7/8 câu bài học Reading của V9 đủ điều kiện. Chỉ Q3 (2 phương án: ý chính hay chi tiết) không có gợi ý.

**Gợi ý nháp** (người soạn nội dung duyệt lại trước khi merge):

| Câu | Dạng | Gợi ý |
| --- | --- | --- |
| Q13 | Trắc nghiệm 3 phương án | Câu chủ đề nêu ý chung của cả đoạn B. Câu có số liệu hoặc bắt đầu bằng "As a result" thường là bằng chứng hay hệ quả. |
| Q1 | Trắc nghiệm 3 phương án | Tìm câu mà các câu còn lại của đoạn C đều giải thích hoặc minh họa cho nó. Ví dụ về một thành phố cụ thể hiếm khi là câu chủ đề. |
| Q11 | Trắc nghiệm 3 phương án | Câu báo trước nội dung cả đoạn D thường ngắn và khái quát. Câu nêu một lý do cụ thể chỉ là chi tiết. |
| Q12 | Điền từ | Đọc câu thứ hai của đoạn D. Từ cần tìm là danh từ số nhiều chỉ người, đứng đầu câu. |
| Q5 | Trắc nghiệm 3 phương án | Ý chính của cả bài phải đúng với mọi đoạn, kể cả đoạn D. Loại phương án chỉ khớp với một chi tiết. |
| Q4 | Matching, 3 heading | Đọc câu đầu đoạn C, rồi chọn heading tóm được cả đoạn, không chỉ một ví dụ. |
| QT1 | True/False/Not Given | So từng chi tiết của câu khẳng định với đoạn văn, nhất là ngày trong tuần. |
- Không điền cho câu của V4, V6 (không nằm trong khối bài tập nào), câu của gói luyện thêm và mã đề trong V9 (không dùng tới).

**Domain và API admin:**
- `QuestionVersion`: thêm field `hint` (nullable), getter; constructor và `create(...)` nhận `hint`. Chuỗi rỗng hoặc chỉ khoảng trắng
  lưu `NULL`; còn lại lưu sau khi trim.
- `AddQuestionVersionRequest`: `@Size(max = 500) String hint` (tùy chọn). Command, result, response admin thêm `hint`.
- JPA entity `@Column(name = "hint", columnDefinition = "TEXT")`; mapper hai chiều.
- `QuestionController` (đã khóa cho `ADMIN`, `CONTENT_AUTHOR` ở phase 2 của 1640) trả `hint` trong chi tiết câu.
- Content **không** từ chối gợi ý cho câu không đủ điều kiện (ai-learning bỏ qua lúc chạy); không kiểm lộ đáp án ở API (đợt sau).

**Endpoint nội bộ:**
- `GET /internal/learning-content/lessons/{id}`: result và DTO của câu thêm `hint`. Lấy cùng query theo lô của câu (không thêm query).
- Không thêm `hint` vào `GET /package-versions/{id}`, `GameContentSnapshotResponse`, API học viên nào khác.

## Architecture

```text
người soạn → POST version câu (hint) → question_versions.hint
ai-learning → GET /internal/learning-content/lessons/{id} → câu có answerSpec, options, explanation, hint
                                                            (ai-learning quyết định có trả hint cho học viên hay không)
```

## Related Code Files

- Create: `db/migration/V13__question_version_hint.sql`, test `QuestionHintSeedTest`
- Modify: `QuestionVersion.java`, `AddQuestionVersionRequest.java`, `AddQuestionVersionCommand.java`,
  `AddQuestionVersionUseCase.java`, `QuestionVersionResult.java`, `QuestionVersionResponse.java`, `QuestionController.java`,
  `QuestionVersionJpaEntity.java`, `QuestionPersistenceMapper.java`, result và DTO câu của `GetLessonContentUseCase` (1640),
  `docs/contracts/lesson-learning-v1.md`, `docs/contracts/learning-content-internal-v1.md`
- Tests: `QuestionControllerTest`, `AddQuestionVersionUseCaseTest`, test controller nội bộ bài học (1640), mọi chỗ gọi constructor `QuestionVersion`

## Implementation Steps

**Tests Before:**
1. `mvn -q -pl services/content-service -am test` sau khi 1640 xong; ghi baseline. Khóa tập key hiện có của
   `GET /package-versions/{id}` và game snapshot (để chứng minh không bị thêm `hint`).

**Tests After** (viết trước code):
2. Migration (Testcontainers): cột `hint` tồn tại, nullable; sau migration, 7 câu trong bảng gợi ý có `hint`, Q3 vẫn `NULL`.
3. `QuestionHintSeedTest`:
   - trong khối EXERCISE của bài thuộc topic Reading (`DEMO_READING`, `TFNG_SKILLS`): câu đủ điều kiện có `hint`; câu không đủ điều
     kiện có `hint = NULL`;
   - với mọi câu có `hint`: dài 1–500 ký tự; `FILL` thì không đáp án nào trong `accepted` xuất hiện trong gợi ý (chuẩn hóa như
     `answer-spec-v1`: trim, gộp khoảng trắng, không phân biệt hoa thường; so theo nguyên từ); `CHOICE` thì gợi ý không chứa nội dung
     phương án đúng và không chứa mã phương án đúng khi mã dài từ 2 ký tự; True/False/Not Given thì không chứa `TRUE`, `FALSE`,
     `NOT GIVEN`, `NOT_GIVEN` (không phân biệt hoa thường, so theo nguyên từ).
4. Domain và API admin: thêm version kèm `hint` → chi tiết câu trả `hint` đã trim; `hint = "   "` → `null`; `hint` 501 ký tự → 400;
   không gửi `hint` → `null`.
5. Nội bộ: `GET /internal/learning-content/lessons/{id}` có `hint` cho từng câu (`null` khi không có); `GET /package-versions/{id}`
   và game snapshot giữ nguyên tập key đã khóa ở bước 1.

**Implement:** contract → migration → domain và persistence → API admin → endpoint nội bộ.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] Một migration content, chỉ thêm cột và `UPDATE` seed; không sửa migration cũ.
- [ ] Gợi ý chỉ ra ngoài content qua API admin và endpoint nội bộ của bài học.
- [ ] Test seed chặn gợi ý chứa đáp án và gợi ý đặt nhầm vào câu không đủ điều kiện.

## Risk Assessment

- **Gợi ý lộ đáp án bằng cách diễn đạt khác** (test chỉ bắt chuỗi trùng): người soạn duyệt gợi ý; luật soạn ghi trong contract.
- **Đổi constructor `QuestionVersion` làm vỡ nhiều chỗ gọi:** sửa hết trong cùng commit; compile cả reactor (`mvn -q compile -DskipTests`).
- **Id câu trong V9 chưa biết lúc lập plan:** lấy từ V9 khi 1640 đã merge; test ở bước 2 bắt thiếu.
- **Gợi ý True/False/Not Given dễ thành lời giải** (ví dụ nói thẳng "đoạn văn không nhắc tới"): gợi ý chỉ nói chỗ cần so,
  không nói kết luận; test seed chặn chuỗi `TRUE`, `FALSE`, `NOT GIVEN`.

## Security Considerations

- `hint` không phải bí mật như `answerSpec`, nhưng vẫn chỉ đi qua endpoint nội bộ (Gateway chặn `/internal/**` từ phase 2 của 1640),
  để học viên không đọc được gợi ý trước khi làm.
