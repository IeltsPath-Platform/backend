---
phase: 3
title: "Duyệt mã lỗi Writing trong danh mục"
status: blocked
priority: P1
effort: "1h + thời gian người duyệt"
dependencies: []
---

# Phase 3: Duyệt mã lỗi Writing

## Overview

Danh mục `error_types` (content-service) đã có đủ nhóm cho Writing, nhưng mọi mã đang `review_status = 'DRAFT'` (migration
`V48__error_type_review_status.sql`, mặc định `DRAFT`, không dữ liệu nào đặt `APPROVED`). Endpoint nội bộ
`/internal/learning-content/error-types` mà assessment-service đọc chỉ trả mã vừa `ACTIVE` vừa `APPROVED`. Kết quả trên DB mới:
danh mục trống, Examiner ghi bất kỳ mã nào cũng bị `UnknownErrorCodeException`, và AI chấm Writing bị bỏ hết `corrections`.

## Điều kiện chặn

Theo quy định của dự án, học viên **không được thấy** mã hay gợi ý sửa còn `DRAFT`. Duyệt mã là quyết định chuyên môn của người
hiểu IELTS, **không phải việc kỹ thuật**. Phase này không merge khi chưa có tên người duyệt.

## Mã Writing hiện có (12)

| Nhóm | Mã |
|---|---|
| GRAMMAR | `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT` |
| VOCABULARY | `WORD_CHOICE`, `COLLOCATION` |
| COHERENCE | `LINKER_MISUSE`, `NO_CLEAR_POSITION` |
| TASK | `UNDERDEVELOPED_IDEA`, `OFF_TOPIC`, `T1_NO_OVERVIEW`, `T1_DATA_MISREPORTED` |

Mã Reading, Listening, chính tả **không đổi** trong phase này.

## Files

- Bản nháp đã viết và thử: `drafts/V59__approve_writing_error_codes.sql` (cạnh file này). Sau khi có người duyệt, chép vào
  `services/content-service/src/main/resources/db/migration/`. **Không** đặt vào thư mục migration trước đó vì Flyway áp dụng khi service khởi động.

## Steps

1. Gửi bảng mã (tên, `fix_hint`, mức nghiêm trọng) cho người duyệt; ghi lại danh sách mã được duyệt và danh sách cần sửa.
2. Nếu người duyệt muốn thêm mã (dạng từ, dấu câu, lặp từ): thêm vào cùng migration bằng `INSERT`, `status = 'ACTIVE'`,
   `review_status = 'APPROVED'`, đúng `category` đã có trong `CHECK`.
3. Viết migration dạng:

```sql
UPDATE error_types SET review_status = 'APPROVED', updated_at = CURRENT_TIMESTAMP
WHERE code IN ('TENSE','ARTICLE','SUBJECT_VERB_AGREEMENT','SENTENCE_FRAGMENT','WORD_CHOICE','COLLOCATION',
               'LINKER_MISUSE','NO_CLEAR_POSITION','UNDERDEVELOPED_IDEA','OFF_TOPIC','T1_NO_OVERVIEW','T1_DATA_MISREPORTED');
```

   Chỉ đưa vào `IN (...)` những mã người duyệt đã đồng ý.
4. Ghi tên người duyệt và ngày vào chú thích đầu file migration và mô tả PR.

Cách thay thế cho môi trường dev: duyệt bằng `PUT /api/content/error-types/{id}` với `reviewStatus`, không cần migration. Không dùng
cho môi trường thật vì không có dấu vết trong Git.

## Tác động khi duyệt (cần người duyệt biết)

| Nơi | Thay đổi |
|---|---|
| Examiner chấm | Dropdown hiện đủ mã; ghi mã được chấp nhận |
| AI chấm Writing | Prompt có thêm 12 mã; `corrections` có mã bắt đầu được lưu và hiện cho học viên |
| Soạn câu hỏi trắc nghiệm | Thêm phiên bản câu hỏi có `distractors` dùng các mã này giờ qua được kiểm tra |
| Báo cáo học viên | Có thể hiện tên mã Writing |

## Validation

Đã thử trên PostgreSQL 15 tạm: áp V1 đến V58 của content-service, rồi bản nháp. Số mã `ACTIVE` + `APPROVED` đi từ 0 lên 12.
Còn phải kiểm tra endpoint `/internal/learning-content/error-types` trả đúng tập đó khi chạy service thật.

## Rollback

Migration mới đặt lại `review_status = 'DRAFT'` cho các mã đó. Không sửa V59 sau khi merge.
