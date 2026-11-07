# Phase 3: XP, streak, tim, mua bằng Points, gateway

Status: complete for backend delivery; final Library 407, Gateway 58 and required Access fix 112 clean tests passed. Live rewards, Points recovery, Premium, flashcard streak and two-way web independence passed; split-run acceptance evidence is recorded in phase 4 reports.

Service: library-service, cộng thêm config-server, api-gateway, docker-compose. Phụ thuộc: phase 2. Đọc trước: [plan.md](./plan.md) mục 7 và hợp đồng mục 7.

## 1. Mục tiêu

Hoàn thành unit cho XP, cấp độ và ngày streak; sai bước làm mất tim; hết tim thì bị chặn; hồi tim và mua freeze bằng Points của access-service. Tất cả độc lập với learning-service.

Quy tắc dưới đây **sao chép từ learning-service** (đã chạy trên web) sang code của library-service. Nguồn để đối chiếu, chỉ đọc, không import: `domain/aggregate/Hearts.java`, `domain/service/StreakRules.java`, `domain/vo/StreakState.java`, `application/usecase/StreakUseCase.java`, `application/service/HeartsService.java`, `application/service/GamificationRecorder.java` (đều trong `learning-service/src/main/java/com/ieltspath/learning/`). Đổi tên lớp sang tiền tố `Study` để không nhầm.

## 2. Migration `V41__vocabulary_study_gamification.sql`

```sql
CREATE TABLE vocabulary_study_profiles (
    user_id UUID PRIMARY KEY,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    total_xp INT NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
    hearts INT NOT NULL,
    max_hearts INT NOT NULL CHECK (max_hearts >= 1),
    hearts_regen_at TIMESTAMPTZ NOT NULL,
    max_hearts_checked_at TIMESTAMPTZ,         -- lần cuối hỏi Access thành công về gói Premium; null = chưa hỏi
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (hearts >= 0 AND hearts <= max_hearts)
);

CREATE TABLE vocabulary_study_xp_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    source VARCHAR(30) NOT NULL CHECK (source IN ('UNIT_FIRST_COMPLETION', 'UNIT_PERFECT_BONUS')),
    source_ref VARCHAR(120) NOT NULL,          -- unitId
    amount INT NOT NULL CHECK (amount > 0),
    local_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, source_ref)
);

-- Cùng cột với learner_streaks của learning-service (V28), bảng riêng
CREATE TABLE vocabulary_study_streaks (
    user_id UUID PRIMARY KEY,
    current_days INT NOT NULL DEFAULT 0 CHECK (current_days >= 0),
    longest_days INT NOT NULL DEFAULT 0 CHECK (longest_days >= 0),
    last_active_date DATE,
    streak_start DATE,
    evaluated_through DATE,
    freeze_available BOOLEAN NOT NULL DEFAULT FALSE,
    freeze_eligible_from DATE,
    frozen_run_end DATE,
    freeze_run INT NOT NULL DEFAULT 0 CHECK (freeze_run >= 0),
    lost_streak_days INT NOT NULL DEFAULT 0 CHECK (lost_streak_days >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vocabulary_study_purchases (
    request_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('HEART_REFILL', 'STREAK_FREEZE')),
    amount INT NOT NULL CHECK (amount > 0),    -- giá đã chốt lúc ghi nhận; retry dùng đúng số này
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'DONE')),
    access_ledger_id UUID,
    balance_after BIGINT,
    result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vocab_study_purchase_user ON vocabulary_study_purchases (user_id, created_at DESC);
```

## 3. Cấu hình

Lớp `config/StudyGamificationProperties` (`@ConfigurationProperties("library.vocabulary-study")`, kiểm tra giá trị hợp lệ trong setter như `VocabularyWeeklyGoalProperties`). Thêm vào `backend/infra/config-server/config-repo/library-service.yaml`:

```yaml
library:
  vocabulary-study:
    xp:
      first-completion: 20
      perfect-bonus: 10
      level-xp: 500
    hearts:
      max: 5
      premium-max: 7
      regen-hours: 5
      refill-cost: 5
      plan-refresh-minutes: 10
    streak:
      freeze-price: 10
      max-consecutive-freezes: 5
    default-timezone: Asia/Ho_Chi_Minh
  access:
    base-url: ${ACCESS_SERVICE_URL:http://localhost:8084}
```

`docker-compose.yml`, khối `library-service`: thêm `ACCESS_SERVICE_URL: http://access-service:8084` (khối `learning-service` đã có dòng tương tự). Không thêm `depends_on: access-service`: lời gọi chỉ xảy ra khi mua và đã xử lý lỗi, không nên khóa thứ tự khởi động.

## 4. XP và cấp độ

Lớp thuần `StudyXpRules`.

- `level(totalXp) = totalXp / levelXp + 1` (chia nguyên); `nextLevelAt = level × levelXp`; `xpToNextLevel = nextLevelAt − totalXp`.
- Nguồn XP chỉ có hai: `UNIT_FIRST_COMPLETION` (20) và `UNIT_PERFECT_BONUS` (10), `source_ref = unitId`.

| Tình huống khi `complete` | `firstCompletion` | `firstTry == graded` | XP cộng |
|---|---|---|---|
| Lần đầu hoàn thành, không sai bước nào | true | có | 30 (20 + 10) |
| Lần đầu hoàn thành, có sai | true | không | 20 |
| Replay (dù hoàn hảo) | false | bất kỳ | 0 |
| Gọi `complete` lại cùng attempt | false | bất kỳ | 0 (trả lại kết quả cũ) |
| Hai máy cùng hoàn thành lần đầu | chỉ một bên true | | 20/30 một lần |
| Unit được sửa revision rồi học và hoàn thành lại ở chế độ replay | false | | 0 |

Ví dụ cấp độ với `levelXp = 500`: 0 XP và 499 XP là cấp 1; 500 XP là cấp 2 (`nextLevelAt` 1000); 999 XP cấp 2; 1000 XP cấp 3. Lên cấp thì hồi đầy tim miễn phí.

Cách ghi: `INSERT INTO vocabulary_study_xp_ledger ... ON CONFLICT (user_id, source, source_ref) DO NOTHING` rồi kiểm tra số dòng chèn. Chỉ khoản nào chèn được mới cộng vào `total_xp`. Không có bất kỳ đọc/ghi nào vào bảng XP của learning-service.

## 5. Streak (có freeze)

Lớp thuần `StudyStreakRules` + `StudyStreakState` (record cùng 10 trường với `StreakState` của web). Sao chép đúng các hàm `recordAnswer`, `throughYesterday`, `forRead`, `freezeCost`, `overLimit`, `buyFreeze`, và các hằng số, **trừ phần thưởng mốc** (`isMilestone`, `milestoneXp`, `milestonePoints` không đưa sang). Giá và giới hạn lấy từ cấu hình (`freeze-price`, `max-consecutive-freezes`).

- Hai nguồn ngày hoạt động: (1) `complete` thành công, cả `FIRST_COMPLETION` lẫn `REPLAY`; (2) một lượt ôn flashcard mới (mục 5b). Cả hai gọi cùng một hàm `StudyStreakRecorder.recordActivity(userId, instant)`.
- Ngày tính streak: `today = LocalDate.ofInstant(instant, zoneOf(user))`, rồi `recordAnswer(state, today)`. Lấy múi giờ từ hồ sơ; chưa có hồ sơ thì dùng múi giờ mặc định (không tạo hồ sơ chỉ vì ôn flashcard).
- `streakIncreased = !today.equals(stateTrước.lastActive())`.
- Đọc (summary): `forRead(state, todayNow)`, **không ghi** vào DB. Kết quả này suy ra được từ trạng thái đã lưu nên không cần job đêm, cũng không cần ghi khi đọc.

### 5b. Ôn flashcard giữ streak

Điểm nối: `application/usecase/ReviewFlashcardUseCase.execute(...)` (đã `@Transactional`). Hiện chỉ khi `card.review(correct, requestId, …)` trả `true` (lượt ôn mới, không phải gửi lại cùng `requestId`) thì mới `repository.save` và `reviewEventWriter.append`. Thêm ngay sau `reviewEventWriter.append(...)`, **chỉ trong nhánh `true`**:

```java
studyStreak.recordActivity(userId, card.getLastReviewedAt());
```

- `studyStreak` là port `application/port/StudyStreakActivity` (một phương thức `recordActivity(UUID userId, Instant at)`), cài đặt bởi `StudyStreakRecorder`. Lớp flashcard chỉ biết port, không biết chi tiết streak.
- `StudyStreakRecorder.recordActivity` chạy trong transaction sẵn có: lấy khóa người dùng (`JdbcStudyLock`), đọc `vocabulary_study_streaks` (không có dòng thì `StudyStreakState.empty()`), `recordAnswer(state, ngày)`, `UPSERT`. Không đụng XP, tim, hồ sơ, sổ XP. Không gọi Access.
- Thứ tự khóa: `findForReviewByIdAndUserId` khóa dòng thẻ trước, khóa người dùng sau. `CompleteStudyAttemptUseCase` chỉ lấy khóa người dùng và không khóa thẻ, nên không có vòng chờ lẫn nhau. Ghi chú điều này trong comment của `recordActivity`.
- Lượt ôn gửi lại với cùng `requestId` (`card.review` trả `false`) không gọi `recordActivity`. Dù gọi lại thì `recordAnswer` cùng ngày cũng không đổi gì (hàm đã idempotent theo ngày).
- Cập nhật `ReviewFlashcardUseCaseTest`: constructor có thêm port; thêm test (a) lượt ôn mới gọi `recordActivity` đúng một lần với `lastReviewedAt`, (b) gửi lại cùng `requestId` không gọi, (c) thẻ không thuộc người dùng hoặc không `ACTIVE` ném `ResourceNotFoundException` và không gọi.
- Phản hồi của `POST /api/learning-support/flashcards/{id}/reviews` **không đổi**. Mobile đọc lại `me/summary` để thấy streak mới.
- Chỉ lượt ôn qua `ReviewFlashcardUseCase` mới tính. Tạo thẻ, lưu từ vào sổ, xem thẻ không tính.

Ví dụ (múi giờ `Asia/Ho_Chi_Minh`, chưa có freeze):

| Ngày | Hành động | `currentDays` đọc được | Ghi chú |
|---|---|---|---|
| N | hoàn thành unit | 1 | `lastActive = N` |
| N | hoàn thành thêm unit | 1 | cùng ngày, không tăng |
| N+1 | không làm gì, đọc lúc 23:00 | 1 | ngày N+1 chưa kết thúc, còn cứu được |
| N+2 | đọc | 0 | ngày N+1 trống, chuỗi mất, `lostStreakDays = 1` |
| N+2 | hoàn thành unit | 1 | chuỗi mới bắt đầu |

Với freeze (mua vào ngày N+1 sau khi đã có chuỗi 1): ngày N+1 trống nhưng được phủ, `freezeAvailable` về false, `currentDays` giữ 1; hoàn thành ở N+2 thì `currentDays = 2`. Mua tiếp ngay ngày N+2 (khi ngày N+1 vừa được phủ) có giá `10 × 2 = 20` vì tính cả ngày liên tiếp trước đó.

`StreakView` (hợp đồng 7.2) được dựng từ trạng thái: `qualifiedToday = (lastActive == today)`; lý do không mua được theo đúng thứ tự kiểm tra của web: `ALREADY_OWNED` (đang có freeze) → `NO_STREAK` (`currentDays == 0`) → `LIMIT` (`overLimit`); `freezeCost = null` khi không mua được, ngược lại `freezeCost(state, today)`.

## 6. Tim

Lớp thuần `StudyHearts` (sao chép `Hearts`): `regen`, `lose`, `refillFull`, `setMax`, `full`, `nextHeartAt`.

- Hằng số: 1 tim sau mỗi `regen-hours` (5 giờ); đồng hồ dừng khi đầy tim (lúc đầy, `lastRegenAt` luôn bằng `now`); khi đang đầy mà mất tim, đồng hồ bắt đầu từ lúc mất; tim không xuống dưới 0.
- Ví dụ: đầy 5 tim lúc 08:00, sai một bước lúc 08:10 → còn 4 tim, đồng hồ chạy từ 08:10; lúc 13:10 có 5 tim. Hết tim lúc 09:00; lúc 14:00 có 1 tim, lúc 19:00 có 2 tim.
- Người học mới: hồ sơ được tạo lười (mục 7) với `hearts = max_hearts`, trong đó `max_hearts` là 5 (thường) hoặc 7 (Premium) theo mục 6b.
- Đọc (summary): tính hồi tim trong bộ nhớ, **không ghi**. Mọi đường ghi: nạp hồ sơ, `regen(now)`, rồi mới áp thay đổi, rồi lưu.

Quy tắc trong luồng học (sửa các use case của phase 2):

**`StartStudyAttemptUseCase`**, bước tạo attempt mới `FIRST_COMPLETION`: nếu sau `regen` `hearts == 0` thì `409 VOCAB_NO_HEARTS`. Trả lại attempt đang làm có sẵn thì **không** kiểm tra tim (cho phép xem lại). `REPLAY` không kiểm tra tim.

**`SubmitStudyAnswerUseCase`**, chèn vào bước 6 của phase 2 (sau tra idempotency và kiểm tra thứ tự bước, trước khi chấm), chỉ khi attempt là `FIRST_COMPLETION` và bước là bước chấm điểm: nếu `hearts == 0` thì `409 VOCAB_NO_HEARTS`. Sau khi chấm, nếu kết quả `INCORRECT` **và** `first_submission` thì `lose(1)`. Đáp án sai thứ hai trở đi của cùng bước không mất thêm. `StepResult.heartLost = true` khi vừa mất tim, `hearts` là `HeartsView` sau thay đổi; với `REPLAY` thì `heartLost = false`, `hearts = null`.

Lỗi `VOCAB_NO_HEARTS`:

```json
{ "status": 409, "code": "VOCAB_NO_HEARTS",
  "message": "Bạn đã hết tim. Chờ hồi tim hoặc dùng điểm để hồi đầy.",
  "details": { "nextHeartAt": "2026-11-07T15:02:11Z", "refillCost": "5" } }
```

Trường hợp biên cần có test: sai đúng ở bước cuối cùng làm tim về 0 → kết quả vẫn trả bình thường (`hearts.hearts = 0`), lần nộp chấm điểm kế sẽ bị chặn; gửi lại đúng request cũ (cùng `requestId`) khi đã 0 tim vẫn trả kết quả cũ vì tra idempotency đứng trước kiểm tra tim.

### 6b. Số tim tối đa theo gói (Premium 7, thường 5)

Gói Premium nằm ở access-service. Trên web, `HeartsService.maxFor` gọi `access.premium()` trong khi caller đang giữ transaction. Ở đây **không làm vậy**: không HTTP trong transaction. Dùng lớp `StudyPlanRefresher`:

```
ensureFresh(userId)   // gọi ĐẦU use case, trước khi mở transaction
  1. Đọc nhanh max_hearts_checked_at của hồ sơ (không có hồ sơ = chưa kiểm tra).
  2. Nếu chưa kiểm tra, hoặc cũ hơn plan-refresh-minutes (10 phút):
       premium = access.premium()            // ngoài transaction, có ResilientCall
       - lỗi/mạch hở: bỏ qua, GIỮ max_hearts đang lưu (người mới chưa có hồ sơ thì dùng 5), không cập nhật checked_at
       - thành công: mở transaction ngắn có khóa người dùng:
             tạo hồ sơ nếu chưa có (hearts = max mới)
             max = premium ? premium-max : max
             hearts.regen(now); hearts.setMax(max, now); checked_at = now; lưu
  3. Nếu còn mới: không làm gì thêm.
```

- Gọi `ensureFresh` ở: `StartStudyAttemptUseCase` (chế độ `FIRST_COMPLETION`), `SubmitStudyAnswerUseCase` (bước chấm điểm của attempt `FIRST_COMPLETION`), `RefillStudyHeartsUseCase`, `GetStudySummaryUseCase`. Tối đa một lời gọi Access mỗi người mỗi 10 phút.
- `setMax` giữ nguyên quy tắc của web: nâng lên 7 không tự hồi tim (người đang 5/5 thành 5/7 và đồng hồ hồi bắt đầu); hạ xuống 5 thì cắt tim về tối đa 5.
- Summary khi mới hết hạn làm mới sẽ ghi một lần (qua `ensureFresh`); sau đó lại là đường chỉ đọc.
- Đọc gói bằng `GET /api/access/me/subscription` của access-service: 200 là Premium đang hiệu lực, 204 là không có, cách `RestAccessClient.premium()` của learning-service đang làm.
- Lên cấp hồi đầy tim tới `max_hearts` hiện tại.

## 7. Hồ sơ và múi giờ

- Hồ sơ `vocabulary_study_profiles` tạo lười ở đường **ghi** đầu tiên của người dùng (`INSERT ... ON CONFLICT (user_id) DO NOTHING` rồi đọc lại), với `hearts = max_hearts`, `hearts_regen_at = now`, `timezone` mặc định.
- Đường đọc (summary) khi chưa có hồ sơ: trả giá trị mặc định (0 XP, cấp 1, đầy tim, streak 0) mà không ghi.
- `PUT me/settings`: kiểm tra `ZoneId.getAvailableZoneIds().contains(value)`, sai thì `400` (cách làm có sẵn ở `VocabularyProgressController.zone`). Chỉ ảnh hưởng ngày của sự kiện sau; các `local_date` đã ghi giữ nguyên.

## 8. Tích hợp vào `complete`

Bổ sung bước 4–5 của phase 2, trong cùng transaction và dưới khóa người dùng, theo thứ tự:

1. Hoàn tất tiến độ unit (như phase 2).
2. Nạp hồ sơ (tạo lười), `regen`.
3. XP: nếu `mode == FIRST_COMPLETION` và `firstCompletion`, chèn khoản `UNIT_FIRST_COMPLETION`; nếu chèn được và `graded > 0` và `firstTry == graded`, chèn `UNIT_PERFECT_BONUS`. `earned` = tổng các khoản chèn được.
4. Cập nhật `total_xp`, `level`. Nếu `level` mới lớn hơn: `leveledUp = true`, hồi đầy tim (`refillFull(now)`).
5. Streak: `recordAnswer(state, today)`; lưu.
6. Lưu hồ sơ.
7. Dựng `reward` và lưu cùng `completion`:

```json
"reward": {
  "xpEarned": 30,
  "xpBreakdown": { "firstCompletion": 20, "perfectBonus": 10 },
  "leveledUp": false,
  "streakIncreased": true,
  "summary": {
    "xp": { "totalXp": 130, "level": 1, "nextLevelAt": 500, "xpToNextLevel": 370 },
    "streak": { "currentDays": 4, "longestDays": 6, "lastQualifiedDate": "2026-11-07", "qualifiedToday": true,
                "freezeAvailable": false, "freezeCost": 10, "freezeBlockedReason": null,
                "lostStreakDays": 0, "timezone": "Asia/Ho_Chi_Minh" },
    "hearts": { "hearts": 4, "maxHearts": 5, "nextHeartAt": "2026-11-07T15:02:11Z", "refillCost": 5 }
  }
}
```

Gọi `complete` lần hai trả đúng JSON đã lưu (kể cả `reward`), không tính lại.

## 9. Các endpoint `me/*` (controller `VocabularyStudyMeController`)

`GET /api/learning-support/vocabulary-study/me/summary` → `StudySummary` (như khối `summary` ở trên). Không giữ khóa, không ghi.

`PUT /api/learning-support/vocabulary-study/me/settings`, body `{ "timezone": "Asia/Ho_Chi_Minh" }` → `{ "timezone": "Asia/Ho_Chi_Minh" }`.

`POST /api/learning-support/vocabulary-study/me/hearts/refill`, body `{ "requestId": "uuid" }`:

```json
{ "hearts": { "hearts": 5, "maxHearts": 5, "nextHeartAt": null, "refillCost": 5 },
  "pointsSpent": 5, "pointsBalanceAfter": 37 }
```

`POST /api/learning-support/vocabulary-study/me/streak/freeze`, body `{ "requestId": "uuid" }`:

```json
{ "streak": { "currentDays": 4, "…": "…", "freezeAvailable": true, "freezeCost": null, "freezeBlockedReason": "ALREADY_OWNED" },
  "pointsSpent": 10, "pointsBalanceAfter": 27 }
```

Mã lỗi: `409 HEARTS_FULL`, `409 FREEZE_ALREADY_OWNED`, `409 NO_STREAK_TO_PROTECT`, `409 FREEZE_LIMIT`, `402 INSUFFICIENT_POINTS`, `503 ACCESS_UNAVAILABLE`, `409 IDEMPOTENCY_CONFLICT`.

## 10. Mua bằng Points: ba bước, không HTTP trong transaction

Lớp điều phối `StudyPurchaseCoordinator` dùng `TransactionTemplate` (use case không `@Transactional`).

```
Bước 1  tx A (có khóa người dùng)
          - nạp hồ sơ/streak; nếu CHƯA có dòng purchase cho requestId:
              kiểm tra điều kiện (tim chưa đầy / freeze mua được) -> sai thì ném lỗi 409
              INSERT purchase(request_id, user, kind, amount = giá hiện tại, status = PENDING)
          - nếu ĐÃ có dòng: user hoặc kind khác -> 409 IDEMPOTENCY_CONFLICT; status DONE -> trả result đã lưu
          commit
Bước 2  KHÔNG có transaction, KHÔNG giữ khóa
          Access.debit(userId, amount của dòng purchase, referenceType, referenceId = requestId,
                       idempotencyKey = "vocab-study:" + requestId, description)
          - 402 -> xóa dòng PENDING (chưa trừ gì), ném 402 INSUFFICIENT_POINTS
          - lỗi mạng/5xx/mạch hở -> giữ dòng PENDING, ném 503 ACCESS_UNAVAILABLE
Bước 3  tx B (có khóa người dùng)
          - nạp lại hồ sơ/streak (có thể đã đổi), áp hiệu ứng (hồi đầy tim / đặt freeze)
          - đặt purchase = DONE, ghi access_ledger_id, balance_after, result
          commit, trả kết quả
```

Điểm bắt buộc:

- **Số tiền lấy từ dòng purchase**, không tính lại ở lần retry. Access so sánh `amount`, `referenceType`, `referenceId`, `userId` khi phát lại cùng `idempotencyKey`; số khác đi sẽ bị `409` ở Access.
- `referenceType` là `HEART_REFILL` hoặc `STREAK_FREEZE` (Access ánh xạ sang `HEART_REFILL_DEBIT` / `STREAK_FREEZE_DEBIT`); `description` ghi rõ nguồn, ví dụ `"Hồi tim (từ vựng)"`, `"Đóng băng streak (từ vựng)"`.
- `idempotencyKey` của Access là cột `VARCHAR(255)` duy nhất toàn hệ thống; tiền tố `vocab-study:` tránh đụng khóa của web.
- Retry sau lỗi 503 hoặc app bị tắt giữa chừng dùng lại cùng `requestId` sẽ đi tiếp từ dòng `PENDING`: bước 2 phát lại (Access trả khoản đã trừ), bước 3 hoàn tất. Không có job nền trong v1; hoàn tất do client gọi lại.
- Hiệu ứng ở bước 3 áp theo trạng thái lúc đó. Nếu tim đã tự đầy trong lúc chờ, vẫn đánh dấu `DONE` (người học đã trả; cửa sổ này rất hẹp, chấp nhận). Giá freeze ở bước 3 không tính lại.

Ma trận lỗi cần có test:

| Tình huống | Điểm bị trừ | Trạng thái học | Phản hồi |
|---|---|---|---|
| Đủ điểm, mọi thứ bình thường | 1 lần | đổi | 200 |
| Gọi lại cùng `requestId` sau thành công | không thêm | không đổi | 200, kết quả cũ |
| Không đủ điểm | không | không đổi | 402 `INSUFFICIENT_POINTS` |
| Access chậm/hỏng ở bước 2 | không | không đổi | 503; dòng `PENDING` còn |
| Retry cùng `requestId` sau 503, Access đã hồi phục | 1 lần | đổi | 200 |
| Access đã trừ nhưng thời gian chờ hết (không biết kết quả), retry | 1 lần (Access phát lại) | đổi | 200 |
| Hai máy bấm hồi tim cùng lúc, hai `requestId` khác nhau | máy thứ hai bị 409 `HEARTS_FULL` ở bước 1 nếu máy một đã xong; nếu cả hai qua bước 1 trước khi ai xong thì có thể trừ hai lần | | ghi nhận là giới hạn đã biết của thiết kế |
| Cùng `requestId` nhưng khác loại mua | không | không đổi | 409 `IDEMPOTENCY_CONFLICT` |

Dòng "hai máy" là giới hạn cố ý chấp nhận, vì việc khóa chặt hơn buộc giữ khóa khi gọi HTTP (điều plan này cấm). Ghi chú nó trong tài liệu API.

## 11. Access client mới

`application/port/StudyAccessClient` có hai phương thức:

- `PointsDebit debit(UUID userId, long amount, String referenceType, UUID referenceId, String idempotencyKey, String description)` trả `PointsDebit(UUID ledgerId, long balanceAfter)`; ném `InsufficientPointsException`, `AccessUnavailableException`, `PointsIdempotencyConflictException`. Kiểu gọi `CallKind.IDEMPOTENT_WRITE`.
- `boolean premium()`: `GET /api/access/me/subscription` với bearer của người dùng; 200 là `true`, 204 là `false`, lỗi khác ném `AccessUnavailableException`. Kiểu gọi `CallKind.READ` (được retry). Chỉ `StudyPlanRefresher` dùng (mục 6b).

`infrastructure/client/RestStudyAccessClient`: sao chép mẫu `RestAccessClient` của learning-service.

- `RestClient` với `SimpleClientHttpRequestFactory`, connect timeout 2 s, read timeout 5 s, `baseUrl = ${library.access.base-url}`.
- `POST /internal/access/points/debit`, body `{userId, amount, referenceType, referenceId, idempotencyKey, description}`; header `Authorization: Bearer <JWT của request hiện tại>` và `X-Correlation-Id`. Access kiểm tra token trùng `userId`, nên **gọi trên chính thread của request** (không đẩy sang thread khác).
- Gói trong `ResilientCall.call("access", CallKind.IDEMPOTENT_WRITE, …)` (instance `access` đã có trong `config-repo/application.yaml`).
- Ánh xạ: HTTP 402 → `InsufficientPointsException`; 409 → `PointsIdempotencyConflictException`; 5xx, timeout, mạch hở (`DownstreamUnavailableException`) → `AccessUnavailableException`.
- Lớp trợ giúp `GatewayRequestHeaders` (đọc bearer từ `SecurityContextHolder` kiểu `JwtAuthenticationToken`, lấy `correlationId` từ `SecurityHeaders.CORRELATION_ID`) sao chép từ learning-service vào `infrastructure/client`.
- `pom.xml` của library-service: thêm dependency `common-resilience` (cùng cách khai báo như `learning-service/pom.xml`). Kiểm tra rằng ứng dụng khởi động được và bean `ResilientCall` có mặt (`CommonResilienceAutoConfiguration`).

## 12. Gateway và cấu hình

- `backend/infra/config-server/config-repo/api-gateway.yaml`: trong route `library-personal-service` thêm `/api/learning-support/vocabulary-study/**` vào `Path=`. Hiện route đó chỉ liệt kê flashcards, decks, notes, video-progress, saved-segments. Nếu thiếu, mọi API học sẽ không tới được library-service.
- `library-service.yaml`: khối `library.vocabulary-study` và `library.access` (mục 3).
- Thêm một test route cho api-gateway (mẫu: `LearningEssayRouteTierTest`, `ServiceUrlWiringConfigTest` trong `infra/api-gateway/src/test`): yêu cầu tới `/api/learning-support/vocabulary-study/me/summary` được chuyển sang `LIBRARY-SERVICE`.

## 13. Kiểm thử (viết trước khi code)

1. `StudyXpRulesTest`: bảng ở mục 4, ranh giới cấp 499/500/999/1000.
2. `StudyStreakRulesTest`: sao chép các ca của test streak bên learning-service (cùng ngày hai lần, qua ngày trống, freeze phủ đúng một ngày, giá 10/20/30, giới hạn 5 ngày, freeze cần chuỗi đang sống, chỉ giữ một freeze) và chạy lại các ví dụ ở mục 5.
3. `StudyHeartsTest`: các ví dụ giờ ở mục 6; đồng hồ dừng khi đầy; không xuống dưới 0; hồi đầy khi lên cấp.
4. Tích hợp (`@JdbcTest`, Testcontainers): hoàn thành hai lần và hai thread cùng lúc chỉ trả XP một lần (khóa unique của sổ XP); `local_date` ghi đúng theo múi giờ (đặt `Clock` ở 17:30 UTC với `Asia/Ho_Chi_Minh` là ngày hôm sau); đổi múi giờ không đổi ngày đã ghi; replay không cộng XP mà vẫn cập nhật streak.
5. Luồng tim: sai bước chấm điểm lần đầu mất 1 tim, sai lần hai không mất thêm; WORD không mất tim; replay không mất tim và `hearts = null`; 0 tim chặn bắt đầu attempt mới và chặn nộp bước chấm điểm, nhưng không chặn `GET` attempt, trả attempt đang làm, replay, hay phát lại request cũ.
6. `StudyPurchaseCoordinatorTest` với `StudyAccessClient` giả, đi hết ma trận ở mục 10, kể cả "ném lỗi sau bước 2, trước bước 3, rồi retry".
7. Test hợp đồng `RestStudyAccessClient` với server HTTP giả (WireMock hoặc `MockRestServiceServer`): chuyển đúng bearer và correlation id, đúng body, ánh xạ 402/409/5xx, timeout.
8. Test độc lập: một test kiến trúc (ArchUnit hoặc quét class) khẳng định không có lớp nào trong `com.ieltspath.library` import gói `com.ieltspath.learning` hay gọi URL `learning`; kiểm chứng dữ liệu hai chiều thực hiện ở phase 4.
9. Test route gateway (mục 12).
10. Ôn flashcard giữ streak (mục 5b): lượt ôn mới ở ngày N tạo streak 1 dù chưa hoàn thành unit nào; ôn rồi hoàn thành unit cùng ngày chỉ tăng chuỗi một lần; ôn sai (`correct=false`) vẫn tính; gửi lại cùng `requestId` không đổi gì; ôn không cộng XP và không đổi tim; thẻ của người khác không tạo streak; `local_date` theo múi giờ hồ sơ (hoặc mặc định khi chưa có hồ sơ); lượt ôn không tạo hồ sơ.
11. `StudyPlanRefresher` (mục 6b), với `StudyAccessClient` giả:
    - người mới Premium nhận hồ sơ 7/7 tim, người thường 5/5;
    - trong 10 phút chỉ có đúng một lời gọi `premium()` dù gọi nhiều use case (đặt `Clock`);
    - sau 10 phút gọi lại một lần;
    - đang 5/5 rồi lên Premium: thành 5/7, không tự đầy; hạ về thường lúc đang 7/7: thành 5/5; hạ lúc 3/7: giữ 3/5;
    - `premium()` lỗi: giữ `max_hearts` đã lưu, `checked_at` không đổi, request học vẫn thành công;
    - không có lời gọi Access nào trong lúc giữ transaction (kiểm bằng thứ tự gọi trên mock: `premium()` xảy ra trước khi mở transaction).
12. Lên cấp hồi đầy tim tới 7 với Premium.

## 14. Xong khi

- Mọi quy tắc ở hợp đồng mục 7 có test đỏ rồi xanh.
- Hoàn thành cùng một unit hai lần, từ hai thiết bị, hoặc sau khi sửa revision đều trả XP đúng một lần.
- Access hỏng hay chậm không làm mất Points, không cấp tim hay freeze khi chưa trả.
- `mvn -pl services/library-service -o clean test` và test route của api-gateway xanh.

## Implementation clarifications (2026-11-07)

- The timezone example uses 17:30 UTC: Asia/Ho_Chi_Minh is UTC+7, so 16:30 UTC is still 23:30 on the same date. The test fixes the example rather than changing the timezone rules.
- Gateway has its own explicit public GET allowlist in SecurityConfig, in addition to Library configuration. Both exact UUID unit read paths are added there so anonymous reads work through the actual ingress.
- API result records are typed for hearts, rewards, summary, and purchases; public JSON shapes remain the approved contract and stored retries preserve all timestamps/values.

- A real Access concurrency defect contradicted the plan assumption that debit replay was already safe: a same-key loser could return 402 after the winner committed its debit. The necessary narrow dependency fix reuses DebitPointsUseCase existing committed-ledger recovery for InsufficientPointsException. It changes no HTTP/API/locking design, adds no Library business retry, and retains normal 402 pending cleanup. Parent-owned Access tests observe the actual PostgreSQL interleaving RED before the fix.
