# Chia việc Claude / Codex cho hai plan mới

Áp dụng cho `261017-1931-mastery-practice-and-progress` (gọi là **MP**) và `261017-1932-gamification` (gọi là **GM**).

## Nguyên tắc để ít lỗi nhất

1. **Mỗi service một chủ.** Không sửa file trong service của bên kia. Cần đổi thì nhắn bên chủ.
   - **Claude:** `learning-service` (toàn bộ MP + GM phía learning), tài liệu chung (`AGENTS.md`, `docs/`, `DATABASE_V5.md`), rà contract.
   - **Codex:** `content-service`, `library-service`, `access-service`, `user-service`, Gateway/Compose.
2. **Contract trước, code sau.** Các điểm nối giữa service ghi ở mục "Hợp đồng nối service" bên dưới. Hai bên code đúng theo
   đó. Muốn đổi một dòng ở đó thì sửa file này trước, commit, rồi báo bên kia.
3. **Số migration cố định, không chọn lúc code:**

   | Service | Số | Việc |
   |---|---|---|
   | content | V41, V42, V43 | MP-1 schema CEFR; MP-1 seed Grammar/Vocab (INACTIVE); MP-8 xuất bản hẹn giờ |
   | access | V4 | GM-1 loại giao dịch mới |
   | library | V3 | MP-6 flashcard 5 hộp |
   | user | V6 | chỉ khi cần bảng mới (hai endpoint nội bộ không cần bảng) |
   | learning | V21 trở đi | Claude tự đánh số theo thứ tự làm |

   Trước khi tạo file, `git pull --rebase --autostash` và xem `db/migration` của chính service đó.
4. **Maven không chạy chồng nhau trên cùng module dùng chung.** Hai tiến trình `-am` cùng build `common-*` làm hỏng
   `target/` (đã gặp). Quy tắc: chạy `-pl services/<service của mình>` **không `-am`** sau khi đã `mvn -DskipTests install`
   các module `common-*` một lần. Chỉ `mvn verify` cả reactor một lần ở cuối, do một bên chạy khi bên kia đã dừng.
5. **Commit nhỏ, mỗi phase một commit, không đẩy khi đỏ.** `git pull --rebase --autostash` ngay trước `git push`. Không
   `--force`. Không sửa plan của phase người khác; chỉ đánh dấu phase của mình.
6. **Không bật cờ nguy hiểm trên môi trường thật.** `learning.mastery.policy` mặc định `DEEPTUTOR` (cần người dùng xác nhận
   mới chuyển `MP1`). Trần points/ngày mặc định 20 chờ người dùng chốt.
7. **Test đỏ trước, rồi mới code**, đúng như từng phase đã ghi. Integration phải chạy Testcontainers thật (skipped ≠ xanh).

## Thứ tự

```text
Đợt 1 (song song ngay)
  Claude : MP-2 Mastery MP-1.0  ──>  MP-7 Cờ vắng học  ──>  MP-3 Luyện tự do + cây học viên
  Codex  : GM-1 Access points credit  │  MP-1 Metadata KP  │  MP-6 Flashcard 5 hộp  │  MP-8 Xuất bản hẹn giờ
Đợt 2
  Claude : MP-4 Báo cáo kỹ năng  ──>  MP-5 Bản chụp tuần/tháng
  Codex  : endpoint user-service (learning-goal, display-names)  +  (nếu còn) 3 việc tồn từ plan 261016
Đợt 3 (GM, sau khi MP-3 xong)
  Claude : GM-2 XP  ──>  GM-3 Tim  ──>  GM-4 Streak server  ──>  GM-5 Nhiệm vụ  ──>  GM-6 Huy hiệu  ──>  GM-7 League
  Codex  : rà, kiểm thử tích hợp qua Compose; xử lý lỗi phía access/user nếu Claude phát hiện
Cuối
  Claude : MP-9 và GM-8 (contract, tài liệu)  ·  một bên chạy verify cả reactor, bên kia dừng Maven
```

Lý do: GM-3 (tim) sửa chính các use case nộp Practice mà MP-3 cũng sửa; làm tuần tự trong cùng một service tránh xung đột
merge. Codex không đụng learning nên các phase của Codex không phụ thuộc thứ tự của Claude, trừ endpoint ở đợt 2.

## Hợp đồng nối service

Mọi endpoint `/internal/**` chỉ gọi bằng internal JWT, Gateway từ chối từ client (như `InternalPointsController`). Lỗi định
dạng `{"code","message"}` như các service hiện có. Chỉ thêm trường, không đổi trường cũ.

### C1. Access: cộng thưởng points (GM-1, Codex làm server, Claude làm client)

`POST /internal/access/points/credit`

```json
{ "userId": "uuid", "amount": 5, "reason": "STREAK_MILESTONE", "referenceId": "string<=100", "idempotencyKey": "string<=120" }
```

- `reason` ∈ `STREAK_MILESTONE`, `KP_MASTERED`, `DAILY_QUEST`, `BADGE`; giá trị khác → 400.
- `amount` 1 đến 100.
- Trả 200: `{ "granted": 2, "capped": true, "balance": 41, "transactionId": "uuid|null" }`.
- Cùng `idempotencyKey` → trả đúng kết quả lần đầu, không cộng thêm. Hết trần ngày → 200, `granted = 0`, `capped = true`,
  `transactionId = null`.
- Trần ngày theo UTC, `access.points.daily-earn-limit` mặc định 20.

Trừ points dùng endpoint có sẵn `POST /internal/access/points/debit`, thêm `referenceType` ∈ `HEART_REFILL`,
`STREAK_FREEZE` (giữ nguyên `AI_GRADING`). Thiếu points → mã lỗi hiện đang dùng của endpoint này (Codex kiểm và ghi vào
contract `admin-area-v1.md` hoặc file mới nếu chưa có).

### C2. User: mục tiêu học (MP-4, Codex server, Claude client)

`GET /internal/users/{userId}/learning-goal` → 200 `{ "targetBand": 6.5, "examDate": "2027-01-20" }`; chưa đặt mục tiêu →
200 `{ "targetBand": null, "examDate": null }`; user không tồn tại → 404.

### C3. User: tên hiển thị (GM-7, Codex server, Claude client)

`POST /internal/users/display-names` body `{ "ids": ["uuid", ...] }` (1 đến 50, vượt → 400) → 200
`{ "items": [ { "id": "uuid", "displayName": "string" } ] }`. Không trả email. Id không có thì bỏ khỏi `items`.

### C4. Content: KP có thêm trường (MP-1, Codex server, Claude đọc)

Các response KP và cây (cả nội bộ cho learning) thêm `cefrLevel` (`A1`..`C2` hoặc null), `canDo`, `scopeIn`, `scopeOut`
(null được). Learning phải chạy đúng khi các trường này là null (đợt 1 learning có thể chạy trước khi Codex xong MP-1).

### C5. Library: flashcard (MP-6)

Chỉ trong library-service và FE; learning không gọi. Không có hợp đồng nối.

### C6. Points thưởng: learning → access (gamification phase 4, Claude publish và settle, Codex consume)

Learning ghi thưởng vào `reward_grants` (PENDING) trong transaction nộp bài, rồi gửi qua RabbitMQ. Không có danh tính
service nào được thêm, access không nhận lệnh cộng từ HTTP của learning.

- Exchange `learning.points` (topic, durable). Learning gửi với routing key `points.grant.requested`.
- Payload `points.grant.requested`: `{"grantId": uuid, "userId": uuid, "amount": 1..100, "reason": "STREAK_MILESTONE",
  "referenceId": "<streakStart>:<day>"}`.
- Access: queue `access.points-grant` bind vào `points.grant.requested`; cộng bằng cùng luật với
  `POST /internal/access/points/credit`, `idempotencyKey = grantId`. Gửi kết quả lên exchange `learning.points`, routing key
  `points.grant.settled`: `{"grantId": uuid, "status": "GRANTED" | "CAPPED" | "FAILED", "error": string|null}`.
- Learning: queue `learning.points-grant-settled` nhận kết quả và đổi `reward_grants.status`. Relay gửi lại các grant còn
  PENDING quá 5 phút; access bỏ qua grantId đã cộng (idempotent), nên gửi lại an toàn.
- Access tắt hoặc chưa nhận: grant nằm PENDING, bật lại thì gửi tiếp (tiêu chí của gamification phase 4).

## Việc tồn từ plan 261016 (Codex)

- Dựng lại image Learning rồi chạy lại demo bước 5 và 8.
- Điều tra bước 4: `packageVersionId` Learning giao khác bản đã publish ở Content.
- `SELECT count(*) ... WHERE difficulty IS NULL` trên bảng câu hỏi.

## Nghiệm thu chéo

- Sau mỗi phase xong, bên làm ghi vào phase file `(test: ...)` như đã làm ở plan 261016.
- Điểm nối C1–C3: bên server viết `@WebMvcTest` cho đúng JSON mẫu ở trên; bên client viết test với JSON mẫu y hệt. Hai
  bên cùng một ví dụ nên khác biệt lộ ngay ở test, không đợi tới Compose.
