# Phase 01: Bỏ ADMIN khỏi endpoint nghiệp vụ (backend)

## Bối cảnh

Hiện `ADMIN` có mặt ở khoảng 88 chỗ `@PreAuthorize` (content-service 53, learning 12, library 11, assessment 9,
access 2, user 1). Phần lớn là `hasAnyRole('ADMIN', 'CONTENT_AUTHOR')`, tức ADMIN đang làm được mọi việc của R&D.

Liệt kê lại trước khi sửa:

```bash
grep -rn "'ADMIN'" services/*/src/main/java --include=*.java
```

## Quy tắc thay thế

| Biểu thức hiện tại | Biểu thức mới |
|---|---|
| `hasAnyRole('ADMIN', 'CONTENT_AUTHOR')` (mọi biến thể thứ tự, có/không dấu cách) | `hasRole('CONTENT_AUTHOR')` |
| `hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'EXAMINER')` | `hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')` |
| `hasAnyRole('ADMIN', 'EXAMINER')`, `hasAnyRole('EXAMINER','ADMIN')` | `hasRole('EXAMINER')` |
| `hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')` | `hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')` |
| `hasAnyRole('CUSTOMER', 'ADMIN')` | xem endpoint cụ thể: nếu là dữ liệu học viên thì `hasRole('CUSTOMER')` |

Lưu ý `hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')`: đây là các endpoint đọc nội dung chung (ví dụ cây
tri thức, khóa học). Bỏ ADMIN ở đây là đúng ý "ADMIN không đọc nghiệp vụ".

## Endpoint chỉ có `hasRole('ADMIN')` (xử lý từng cái)

| Endpoint | Service | Quyết định |
|---|---|---|
| `/api/users/**` quản lý người dùng (`UserManagementController`) | user | **Giữ ADMIN** |
| `/api/access/admin/**` (khóa, gói, điểm, cấp subscription) | access | **Giữ ADMIN** |
| `/api/access/users/{id}/entitlement`, `/points/refund`, `/consume-human-grading` (`InternalAccessController`) | access | **Giữ ADMIN** |
| `GET /api/learning/admin/activity-stats` | learning | **Giữ ADMIN** (số liệu vận hành) |
| `/api/learning/admin/rule-configs` (luật mastery) | learning | Đổi sang `CONTENT_AUTHOR` |
| `/api/learning/admin/score-config` | learning | Đổi sang `CONTENT_AUTHOR` |
| `PATCH /api/learning/admin/badges/{code}` | learning | Đổi sang `CONTENT_AUTHOR` (câu hỏi mở trong plan) |
| `/api/assessments/admin/ai-*` (`AiEvaluationController`) | assessment | Đổi sang `CONTENT_AUTHOR` (câu hỏi mở) |
| `/api/assessments/admin/examiner-quality` | assessment | Đổi sang `CONTENT_AUTHOR` (câu hỏi mở) |

Ngoài ra trong learning: `GET /api/learning/admin/learners/{userId}/courses/{courseId}/score` đổi thành `hasRole('EXAMINER')`;
`StaffAdminController` (work-items, question-stats, tree-quality, flags) áp quy tắc thay thế ở trên và cập nhật hàm
`queues(authentication)` để ADMIN không còn được thấy queue nào (kiểm tra code ánh xạ vai trò sang queue).

## Việc cần làm

1. Áp bảng trên cho toàn bộ `services/*/src/main/java`. Không đổi đường dẫn, không đổi body.
2. **Kiểm tra gọi giữa các service**: tìm mọi chỗ một service gọi service khác (`RestClient`, `WebClient`, các
   `*Client` trong `infrastructure/client`) và xác nhận chúng chuyển tiếp token của người dùng hoặc gọi `/internal/**`,
   không dùng token hệ thống mang vai trò ADMIN. Nếu có chỗ dùng ADMIN, dừng lại và báo, không tự sửa.
3. Kiểm tra `SecurityConfig` từng service và gateway: không có `pathMatchers(...).hasRole("ADMIN")` cho đường dẫn
   nghiệp vụ. Nếu có, áp cùng quy tắc.
4. Sửa test phân quyền hiện có: mọi test kiểu "admin làm được việc nội dung" (ví dụ
   `ContentAuthorizationWebMvcTest.adminCanWriteContent`) đổi thành "admin bị từ chối" và thêm/giữ test "content author
   làm được". Đặt tên test theo hành vi, không theo phase.
5. Thêm một test kiểm tra quy ước cho mỗi service nghiệp vụ (content, library, assessment, learning): quét
   annotation `@PreAuthorize` của các controller bằng reflection và khẳng định không còn `ADMIN`, trừ danh sách cho phép
   (`ActivityStatsController`). Test này giữ quy ước không bị phá về sau.
6. Cập nhật contract có nhắc vai trò: `knowledge-tree-v1.md`, `lesson-authoring-v1.md`, `question-publication-v1.md`,
   `admin-overview-v1.md` (bỏ ADMIN, còn CONTENT_AUTHOR và EXAMINER đọc), `learning-staff-area-v1.md`, và contract của
   library/assessment nếu có ghi vai trò.

## Test

`mvn -q -pl services/content-service,services/learning-service,services/library-service,services/assessment-service -am test`.
Testcontainers cần Docker.

## Tiêu chí hoàn thành

- `grep -rn "'ADMIN'" services/{content,library,assessment}-service/src/main/java` chỉ còn những chỗ trong bảng "Giữ ADMIN"
  (assessment không còn chỗ nào). learning chỉ còn `ActivityStatsController`.
- Test phân quyền mới pass; các test cũ đã sửa pass.
