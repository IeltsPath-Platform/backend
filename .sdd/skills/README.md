# Thư viện Skill cho AI Agent

Các skill trong thư mục này chỉ được nạp khi task khớp với `trigger`.

| File | Trigger |
| --- | --- |
| `api-security-auditor.md` | Tạo/review HTTP API hoặc thay đổi security-sensitive |
| `sql-performance-tuner.md` | Viết/review query, repository, pagination, data access |
| `test-coverage-engineer.md` | Thêm/sửa behavior, bug fix hoặc test |
| `data-migration-safe.md` | Tạo/review migration hoặc schema/data change |
| `ddd-domain-modeling.md` | Thêm/sửa Aggregate, Entity, Value Object hoặc business invariant |
| `clean-architecture-reviewer.md` | Thêm/sửa layer, port/adapter, repository, use case hoặc dependency direction |

## Quy tắc sử dụng

1. Đọc trigger trước khi lập plan.
2. Chỉ nạp skill liên quan.
3. Thực hiện theo `WORKFLOW`.
4. Trước khi kết thúc, chạy `CHECKLIST`.
5. Kiểm tra `ANTI-PATTERNS`.
6. Skill không được override Constitution hoặc Constraints.

## Bối cảnh bắt buộc

- Đọc `.sdd/constraints/global.md` cùng các tài liệu `.sdd/global/` liên quan trước khi áp dụng skill.
- Chỉ áp dụng công nghệ, command và convention đã được xác minh cho scope thay đổi; không suy diễn một pattern riêng của `user-service` thành chuẩn bắt buộc cho mọi service tương lai.
- Repository hiện không có frontend, CI workflow, formatter/linter, coverage gate hoặc OpenAPI contract. Không tự tạo requirement cho các capability này.
