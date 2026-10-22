# Phase 02: Luật vai trò khi quản lý người dùng (backend, user-service)

## Hiện trạng

`UserManagementController` (`/api/users`, `@PreAuthorize("hasRole('ADMIN')")`):

| Method | Path | Body |
|---|---|---|
| GET | `/api/users` | trả toàn bộ người dùng (`UserResponse`: id, email, fullName, phoneNumber, status, roles[], createdAt, updatedAt) |
| POST | `/api/users` | `CreateUserRequest {email, password (6-72), fullName, phoneNumber?, roles: Set<String>}` |
| GET | `/api/users/{id}` | |
| PUT | `/api/users/{id}` | `UpdateUserRequest {fullName, phoneNumber?}` |
| PUT | `/api/users/{id}/roles` | `AssignRoleRequest {roles: Set<String>}` (không rỗng) |
| PUT | `/api/users/{id}/status` | `ChangeUserStatusRequest {status: ACTIVE|INACTIVE|LOCKED|PENDING_VERIFY}` |
| DELETE | `/api/users/{id}` | |
| GET/PUT | `/api/users/{id}/profile` | hồ sơ học viên |

Vai trò hợp lệ (bảng `roles`, V3): `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`.
Đăng ký công khai chỉ cho `CUSTOMER` (`RegisterUseCase`), giữ nguyên.

## Luật mới (domain, không phụ thuộc Spring)

Tạo một quy tắc thuần trong domain user-service (ví dụ `RoleAssignmentPolicy`) và gọi nó từ `CreateUserUseCase`
và use case gán vai trò:

1. **Loại trừ**: một tài khoản có `ADMIN` không được có `CONTENT_AUTHOR` hay `EXAMINER`.
   Vi phạm: `422`, `details.code = ROLE_CONFLICT`, thông điệp nêu cặp vai trò xung đột.
2. **Tự bảo vệ** (cần id người gọi, lấy từ `CurrentUserProvider` ở controller rồi truyền vào use case):
   - Không tự gỡ `ADMIN` của chính mình: `409 SELF_ADMIN_REMOVAL`.
   - Không tự đổi trạng thái của mình sang khác `ACTIVE`, không tự xóa mình: `409 SELF_ACCOUNT_CHANGE`.
3. **Admin cuối cùng**: không cho gỡ `ADMIN`, khóa (`LOCKED`/`INACTIVE`) hay xóa khi đó là tài khoản `ADMIN` đang
   `ACTIVE` cuối cùng: `409 LAST_ADMIN`. Đếm bằng một truy vấn, khóa phù hợp để hai request đồng thời không cùng lọt.

Map các mã lỗi trong `GlobalExceptionHandler` của user-service theo mẫu hiện có (`details.code`).

## Dữ liệu cũ

Không viết migration tự sửa dữ liệu. Thêm vào README hoặc contract một câu SQL để người vận hành tự kiểm tra tài khoản
đang vi phạm luật loại trừ:

```sql
SELECT u.email, string_agg(r.name, ',') FROM users u
JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
GROUP BY u.email HAVING bool_or(r.name = 'ADMIN') AND bool_or(r.name IN ('CONTENT_AUTHOR', 'EXAMINER'));
```

Luật chỉ áp khi tạo hoặc đổi vai trò, không chặn đăng nhập của tài khoản cũ.

## Contract

Tạo `backend/docs/contracts/user-management-v1.md`: liệt kê endpoint ở bảng trên, các luật và mã lỗi mới, và ghi rằng
`GET /api/users` trả toàn bộ danh sách (chưa phân trang; frontend lọc phía client).

## Test

- Unit cho `RoleAssignmentPolicy`: mọi tổ hợp hợp lệ/không hợp lệ, `ADMIN + CUSTOMER` hợp lệ.
- Use case (Mockito): tự gỡ ADMIN, tự khóa, tự xóa, admin cuối, xung đột vai trò khi tạo và khi gán.
- `@WebMvcTest`: non-ADMIN nhận `403`; mã lỗi và HTTP status đúng.

## Tiêu chí hoàn thành

`mvn -q -pl services/user-service -am test` xanh; contract mới khớp code.
