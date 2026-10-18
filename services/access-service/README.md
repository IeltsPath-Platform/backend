# access-service

Gói học, subscription, activation key và ví point. Cổng 8084, DB `access_db` (Postgres local 5432). Cấu hình runtime:
`infra/config-server/config-repo/access-service.yaml`.

## Endpoint

| Nhóm | Path | Ai gọi |
| --- | --- | --- |
| Catalog | `GET /api/access/plans`, `GET /api/access/key-products` | Người dùng đăng nhập |
| Học viên | `GET /api/access/me/subscription`, `GET /api/access/me/points`, `GET /api/access/me/points/history`, `POST /api/access/me/keys/activate` | Người dùng đăng nhập, theo subject của token |
| Admin | `/api/access/admin/**` | `ADMIN` |
| Thao tác trên user khác | `GET /api/access/users/{userId}/entitlement`, `POST /api/access/users/{userId}/consume-human-grading`, `POST /api/access/points/refund` | Chỉ `ADMIN`; chưa service nào gọi |
| Nội bộ | `POST /internal/access/points/debit` | Service khác, mang bearer của học viên. Không có route cộng điểm: điểm chỉ được cộng qua message `points.grant.requested` (queue `access.points-grant`) |

## Luật của `/internal/access/**`

- Gateway chặn `/internal/**` với client; chỉ service gọi trực tiếp được.
- Debit chỉ cho **chính subject của token**: `userId` trong body khác subject → 403.
- Idempotency theo `idempotencyKey`: gửi lại cùng nội dung (`userId`, `amount`, `referenceType`, `referenceId`) → trả ledger
  cũ, ví trừ một lần; dùng lại key cho nội dung khác → 409. Hai request song song cùng key → cả hai nhận cùng ledger.
- Thiếu point → 402.
- Credit nhận `userId`, `amount` (1–100), `reason` (`STREAK_MILESTONE`, `KP_MASTERED`, `DAILY_QUEST`, `BADGE`),
  `referenceId` và `idempotencyKey`. Credit chỉ được cho subject của internal JWT. Gửi lại cùng key và nội dung trả lại
  kết quả ban đầu; dùng lại key với nội dung khác trả 409. `STREAK_MILESTONE`, `KP_MASTERED` và `DAILY_QUEST` dùng chung
  mức kiếm tối đa theo UTC là `access.points.daily-earn-limit` (mặc định 20); vượt mức chỉ cộng phần còn lại và trả
  `capped: true`, hết mức trả `granted: 0`. `BADGE` không dùng và không làm tăng mức trần ngày.
- Debit với `referenceType` `HEART_REFILL` hoặc `STREAK_FREEZE` được ghi lần lượt là `HEART_REFILL_DEBIT` và
  `STREAK_FREEZE_DEBIT`; các reference hiện có vẫn được ghi `AI_GRADING_DEBIT`.

## Đổi route

- `POST /api/access/points/debit` đã chuyển sang `POST /internal/access/points/debit` (path cũ trả 404).
- Refund, consume-human-grading, entitlement giữ path cũ nhưng chỉ `ADMIN` (trước đây mọi người đăng nhập đều gọi được).

## Test

`mvn -q -pl services/access-service -am test`
