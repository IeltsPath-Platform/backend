# Giai đoạn 4: tài liệu và dọn dẹp

Xem [plan.md](./plan.md). Làm sau giai đoạn 3.

## 4.1 Tài liệu

| File | Sửa |
| --- | --- |
| `AGENTS.md` | Bảng cấu trúc (khoảng dòng 21 và 236): thêm `shared/common-events` (tên và giá trị exchange, routing key, header; record payload; `MessageRetry` và `RetryTopology`; không deploy độc lập). Dòng "13 module trong reactor: 3 infra, 1 shared, 9 business" → "14 module: 3 infra, 2 shared, 9 business". Dòng "Shared code" thêm `common-events`. Mục 3.5 (`shared/ chỉ chứa technical concern`) ghi thêm: "`common-events` chỉ chứa tên và hình dạng của event dùng chung, không có logic nghiệp vụ". Lệnh test hẹp (khoảng dòng 325): thêm `mvn -q -pl shared/common-events test`. |
| `CLAUDE.md` | Đồng bộ với các thay đổi trên (nếu có bảng module/lệnh). |
| `docs/system-architecture.md` | Thêm `common-events` vào sơ đồ/danh sách module; ghi quy tắc "đổi tên exchange/routing key/payload chỉ ở `common-events`, queue do service sở hữu khai báo". |
| `docs/contracts/access-points-v1.md`, `assessment-completed-v2.md`, `community-events-v1.md` | Thêm một dòng đầu mục messaging: "Tên exchange và routing key, và hình dạng payload, được mã hóa ở `shared/common-events` (lớp `...`). Đổi chúng phải đổi tài liệu này và module cùng lúc." |
| `plans/261019-0025-common-events/plan.md` | Cập nhật trạng thái các giai đoạn thành "đã xong" kèm số commit. |
| `README.md` gốc | Nếu có danh sách module/lệnh build, thêm `common-events`. |

## 4.2 Kiểm tra bằng grep (phải sạch)

Chạy từ gốc backend; mỗi lệnh phải **không có kết quả** ngoài `shared/common-events` và các ngoại lệ ghi ở `plan.md`:

```
grep -rnE "\"(learning\.points|points\.grant\.(requested|settled)|assessment\.events|assessment\.completed\.v2|community\.events)\"" services/*/src/main
grep -rnE "\"community\.(post-commented|comment-replied|comment-mentioned|post-reacted|user-followed)\.v1\"" services/*/src/main
grep -rn "\"x-retry-count\"" services/*/src/main
grep -rnE "class MessageRetry|record (CommunityEvent|AssessmentCompletedV2|PointsGrantSettled)\b" services shared --include=*.java
```

Ngoại lệ được phép còn literal: `assessment-service/src/main/resources`/`application*.yml` chỉ khi nó là *giá trị cấu hình ghi đè* (property), và
`notification-service/.../db/migration/V3__...sql`. Ghi rõ từng ngoại lệ trong PR.

## 4.3 Kiểm tra cuối

```
mvn -B -ntp verify                    # cả reactor; ghi số test từng module
docker compose config --quiet
```

Báo cáo trong PR: số test trước và sau của từng module (access, learning, assessment, community, notification, common-events), danh sách test
Testcontainers đã chạy (không skip), kết quả các lệnh grep ở 4.2, và xác nhận không có `PRECONDITION_FAILED` khi khởi động access/learning với broker đã có queue.

## 4.4 Triển khai (ghi vào mô tả PR)

- Không cần xóa queue/exchange, không cần migration, không đổi biến môi trường.
- Build toàn reactor như bình thường; `Dockerfile.spring-service` tự kéo `common-events` qua `-am`.
- Rollback là revert; không có thay đổi dữ liệu hay broker.
