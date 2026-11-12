# Dev B: trạng thái nhánh nền tảng

- Ngày: 2026-11-11, 18:40 (Asia/Bangkok)
- Phạm vi: phase 0b (1102), 1620-1..4, 1640-1..5
- Trạng thái: các nhánh đã commit local; chỉ `feat/common-llm` đã push

## Nhánh

| Nhánh | Repo | Tách từ | Commit | Push |
| --- | --- | --- | --- | --- |
| `feat/common-llm` | backend | `origin/fix/grade-writting-llm` | 2 (0b) | có |
| `chore/remove-secret-fallbacks` | backend | `origin/fix/grade-writting-llm` | 2 | chưa |
| `chore/drop-dead-outbox` | backend | `origin/fix/grade-writting-llm` | 2 | chưa |
| `chore/small-fixes-and-docs` | backend | `origin/fix/grade-writting-llm` | 1 | chưa |
| `test/architecture-rules` | backend | `feat/common-llm` | 3 | chưa |
| `chore/production-deployment` | backend | `chore/remove-secret-fallbacks` | 3 | chưa |
| `chore/small-fixes-and-docs` | frontend | `origin/fix/grade-writting-llm` | 1 | chưa |
| `chore/production-deployment` | frontend | `chore/small-fixes-and-docs` | 1 | chưa |

Thứ tự merge: `feat/common-llm` → các nhánh còn lại. `chore/production-deployment` (backend) cần `chore/remove-secret-fallbacks`; nên merge hai nhánh theo thứ tự đó.

## Kiểm đã chạy

| Việc | Lệnh / đối tượng | Kết quả |
| --- | --- | --- |
| Thư viện LLM | `shared/common-llm` | 28 test xanh |
| 0b ở ba service | learning, assessment, content | exit 0 |
| Cấu hình secret | config-server | 3 test xanh (1 cũ + 2 mới) |
| Outbox | game (focused) | 44 test xanh |
| Outbox | content (migration) | xanh; V124 áp được |
| Pricing frontend | 2 file test | 29 test xanh; typecheck, build xanh |
| Architecture | 9 service, `-Dtest=ArchitectureTest` | bootstrap xanh; chạy thường xanh |
| Vi phạm mới | thử một class domain gọi Spring | test đỏ, đã xóa class thử |
| Production | `test-production.py` | 8 test xanh |
| Compose production | `scripts/production/compose.py config` với env rỗng trong thư mục tạm | xanh |
| Workflow | parse `images.yml`, `deploy.yml` | hợp lệ |
| Reactor | `mvn -B -ntp verify -fae` cả 18 module, trên nhánh tích hợp tạm `verify/dev-b-integration` | 18/18 module xanh; notification có 2 test broker lỗi lần chạy đầu, chạy lại 4/4 xanh (flaky dưới tải) |
| Docker | build 12 image Spring (eureka, gateway, 9 service) + config-server | 13/13 build xanh |
| Docker | build `infra/config-server/Dockerfile`, chạy image | HTTP 200, không lỗi |
| Docker | chạy image, gọi config | HTTP 200, không lỗi |

## Chưa chạy

- Verify cả reactor chỉ chạy trên nhánh tích hợp tạm `verify/dev-b-integration`, không chạy trên từng nhánh riêng.
- `actionlint` không có trên máy; workflow mới parse được nhưng chưa lint.
- Chưa dựng stack production thật trên máy này.

## Việc chỉ người mới làm được

- VPS, tên miền, nơi lưu bản sao lưu.
- GitHub secrets và environment `production`.
- Chạy `deploy.yml` lần đầu.
- Xoay toàn bộ secret (giá trị cũ đã nằm trong lịch sử git).
- Cấu hình DNS và CDN frontend.

## Lệch so với plan

- **Test notification:** test không biên dịch được ngay trên `fix/grade-writting-llm`, nên CI đỏ. Tôi sửa một dòng trên `test/architecture-rules`, commit riêng; có thể cần đưa về nhánh gốc.
- **ArchUnit:** vi phạm cũ được chốt tự động (content 30, learning 4, assessment 4). Chưa xem từng vi phạm; cần người duyệt trước khi merge.
- **Dockerfile:** bản staging bỏ vòng retry của `dependency:go-offline` và thêm user không phải root. Tôi dùng bản staging.

## Chưa giải quyết

- `features/practice/writing/useWritingGrading.ts` còn hằng giá dự phòng `pointCost: 3`. Thuộc Dev A.
- Lint frontend đỏ sẵn ở `ListeningScript.tsx` trên nhánh gốc.
- Chưa xác nhận URL Assessment chỉ có khoảng trắng đã được xử lý.
- `.dev-b-staging/` ở gốc workspace có bản sao `.m2`. Không commit; nên xóa.
