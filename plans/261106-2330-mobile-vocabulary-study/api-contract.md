# Bàn giao BE: học từ vựng theo chủ đề trên mobile

Ngày đối chiếu mã nguồn: 06/11/2026.

Đây là **đề xuất hợp đồng API**, chưa phải các endpoint đã được triển khai. Các URL mới bên dưới là tên đề xuất; đội BE có thể đổi tên nếu cung cấp OpenAPI và thống nhất mapping. Không có thay đổi mã BE trong tài liệu này.

**Bản 2 (06/11/2026):** XP, cấp độ, streak (có freeze) và tim của mobile là hệ thống **độc lập với web**, nằm trong library-service; mua tim và freeze bằng điểm của access-service. Unit do content author biên soạn (không phải admin). Kế hoạch triển khai: `plans/261106-2330-mobile-vocabulary-study/plan.md`.

## 1. Phạm vi sản phẩm đã chốt

- Màn Hôm nay là danh sách chủ đề từ vựng cuộn dọc.
- Mỗi chủ đề có ảnh bìa, tên ở giữa ảnh, số từ, nhiều nút bài học nhỏ.
- Mỗi bài nhỏ có các bước: học từ → trắc nghiệm → ghép từ/nghĩa → điền từ → kết quả.
- Hoàn thành bài hiện tại mới mở bài tiếp theo **trong cùng chủ đề**; bài đã hoàn thành được học lại.
- Đề xuất cho bản đầu: bài đầu của mọi chủ đề được mở, không bắt buộc hoàn thành chủ đề trước để học chủ đề khác.
- Gọi bài nhỏ là `vocabulary unit` trong hợp đồng này. Không dùng ID hay API của lesson IELTS/luyện đề/assessment thay cho bài từ vựng.
- Flashcard là chức năng lưu và ôn từ bên cạnh luồng này. Hoàn thành unit không tự động có nghĩa là đã lưu mọi từ vào flashcard hoặc đã ghi nhớ từ lâu dài.
- Quyết định đã chốt (06/11/2026): XP 20 cho lần hoàn thành đầu, +10 nếu không sai bước nào, replay không có XP; cấp độ mỗi 500 XP; streak (tính khi hoàn thành unit hoặc ôn flashcard) có freeze mua bằng điểm; tim tối đa 5, Premium 7, hồi 1 tim mỗi 5 giờ, hồi đầy bằng 5 điểm; khách chưa đăng nhập giữ cách học thử trên máy như hiện tại; các màn `learn`/`practice` cũ không đổi; tất cả độc lập với XP/streak/tim của web. Chi tiết ở mục 7. Mobile không tự gán điểm.
- Mobile chỉ dùng cho học từ vựng. Các API Learning của web (`/api/learning/me/progress`, `/streak`, `/settings`) không còn được màn từ vựng gọi.

## 2. Hiện trạng đã kiểm tra

| Phần | Hiện tại |
|---|---|
| Chủ đề/từ | Đọc từ `/api/content/vocabulary/topics` và `/api/content/vocabulary?topicId=...` |
| Phát âm | Có dữ liệu IPA/audio trong catalog và `/api/content/vocabulary/lookup?forms=...` |
| Chia bài | Mobile tự đặt tiêu đề, chia danh sách từ theo page/size, sinh ID `${topicId}:lesson:${index}` |
| Bài tập | Mobile tự tạo quiz/ghép/điền và chấm trên thiết bị |
| Hoàn thành/mở khóa | AsyncStorage, phân theo userId/topicId; chưa đồng bộ sang máy khác |
| XP/streak/tim | Mobile đang đọc XP và streak của web từ Learning service (`gamification-api.ts`, `streak-api.ts`); hoàn thành unit từ vựng chưa tính thưởng; mobile chưa có tim |
| Lưu từ | Mobile tạo flashcard và thêm vào bộ. BE đã có API notebook có thể tái sử dụng |

Điểm cần sửa về dữ liệu: tên bài như “Môn học”, “Trong lớp học” phải gắn đúng tập từ; không chỉ lấy trang kế tiếp rồi đặt tên. Chia 12 từ thành 9 trang với size=2 như logic hiện tại có thể tạo trang rỗng ở cuối. Thêm từ vào catalog cũng có thể làm nội dung trang thay đổi trong khi ID bài cũ vẫn giữ nguyên. BE cần trả cấu trúc unit rõ ràng để loại bỏ vấn đề này.

Mã mobile liên quan: `src/features/vocab/topic-lessons.ts`, `components/topic-study-sheet.tsx`, `use-topic-lesson-progress.ts`, `api.ts`.

## 3. API BE đã có: tái sử dụng, không yêu cầu viết lại

| API hiện có | Công dụng/ghi chú |
|---|---|
| `GET /api/content/vocabulary/topics` | Chủ đề: id, slug, name, nameVi, coverKey, coverUrl, sortOrder, wordCount |
| `GET /api/content/vocabulary?topicId={id}&page=0&size=20` | Catalog từ; size hiện tối đa 50; có senses |
| `GET /api/content/vocabulary/{id}` | Chi tiết từ, IPA và audio UK/US, các nghĩa |
| `GET /api/content/vocabulary/lookup?forms=...` | Tra phát âm/từ; hiện yêu cầu quyền đăng nhập, tối đa 10 forms/request |
| `POST /api/learning-support/flashcards/notebook/entries` | Lưu nghĩa từ với body `{vocabularySenseId}`; controller trả 201 khi tạo, 200 khi đã có |
| `GET /api/learning-support/flashcards/notebook/saved?senseIds=...` | Lấy senseId đã lưu; tối đa 100 IDs |
| `GET /api/learning-support/flashcards/notebook/entries` | Sổ từ: tìm kiếm/lọc/phân trang, lọc topicId/setId |
| `GET /api/learning-support/flashcards/notebook/summary` | Tổng quan sổ từ |
| `GET, PUT /api/learning-support/flashcards/notebook/weekly-goal` | Mục tiêu tuần |
| `GET /api/learning-support/flashcards/notebook/topics-progress` | Tiến độ từ vựng/notebook theo chủ đề; KHÔNG đồng nghĩa tiến độ hoàn thành các unit mới |
| `GET /api/learning-support/flashcards/notebook/sets/{setId}/progress` | Tiến độ bộ từ |
| `GET /api/learning-support/flashcards/notebook/recent-result` | Kết quả hiện có của notebook; cần đọc đúng semantics khi tích hợp |
| `GET /api/learning-support/flashcards/notebook/achievements` | Thành tích ôn từ; không đồng nhất mặc định với streak tài khoản Learning service |
| `POST /api/learning-support/flashcards/{id}/reviews` | Gửi kết quả ôn một flashcard; không dùng giả lập hoàn thành unit |
| `GET /api/learning/me/progress` | Của web. Mobile từ vựng không dùng; thay bằng `/vocabulary-study/me/summary` |
| `GET /api/learning/me/streak` | Của web. Mobile từ vựng không dùng |
| `GET, PUT /api/learning/me/settings` | Múi giờ của web. Mobile dùng `/vocabulary-study/me/settings` |

Các endpoint notebook đã có trong BE nhưng nhiều màn mobile chưa tích hợp. Đây là việc phía mobile cần làm thêm, không phải BE viết lại API.

## 4. API mới cần bổ sung — mức tối thiểu để học và đồng bộ

Các API public chỉ chứa nội dung. Tiến độ, phiên học và kết quả bắt buộc xác thực; userId lấy từ token, không nhận userId do mobile gửi. Không cache public response có dữ liệu cá nhân.

| # | Endpoint đề xuất | Request | Response bắt buộc |
|---|---|---|---|
| 1 | `GET /api/content/vocabulary/topics/{topicId}/units` | topicId | `TopicUnits`: danh sách unit đúng thứ tự và phiên bản |
| 2 | `GET /api/content/vocabulary/units/{unitId}` | unitId | `UnitContent`: nội dung học từ, đúng sense được chọn |
| 3 | `GET /api/learning-support/vocabulary-study/progress?topicIds=...` | Tối đa 100 topicId/lần; mobile chia batch nếu nhiều hơn | `TopicProgress[]`: trạng thái từng unit, bài tiếp theo, attempt đang học |
| 4 | `POST /api/learning-support/vocabulary-study/units/{unitId}/attempts` | `{requestId, mode: RESUME_OR_START hoặc REPLAY}` | `Attempt`: phiên mới hoặc phiên chưa hoàn thành hiện có, snapshot bài tập |
| 5 | `GET /api/learning-support/vocabulary-study/attempts/{attemptId}` | attemptId | `Attempt`: bước hiện tại, kết quả đã ghi nhận, hoặc kết quả hoàn thành |
| 6 | `POST /api/learning-support/vocabulary-study/attempts/{attemptId}/steps/{stepId}/answers` | `{requestId, answer}` theo dạng bài | `StepResult`: đúng/sai, giải thích, bước tiếp theo, tiến độ |
| 7 | `POST /api/learning-support/vocabulary-study/attempts/{attemptId}/complete` | `{requestId}` | `Completion`: kết quả BE tính, tiến độ mới, unit vừa mở, XP/streak/tim sau khi hoàn thành |
| 8 | `GET /api/learning-support/vocabulary-study/me/summary` | - | `StudySummary`: XP, cấp độ, streak, tim trong một lần gọi cho màn Hôm nay |
| 9 | `PUT /api/learning-support/vocabulary-study/me/settings` | `{timezone}` (IANA) | `{timezone}`. Múi giờ riêng của hồ sơ từ vựng, mặc định `Asia/Ho_Chi_Minh` |
| 10 | `POST /api/learning-support/vocabulary-study/me/hearts/refill` | `{requestId}` | `{hearts: HeartsView, pointsSpent, pointsBalanceAfter}` |
| 11 | `POST /api/learning-support/vocabulary-study/me/streak/freeze` | `{requestId}` | `{streak: StreakView, pointsSpent, pointsBalanceAfter}` |

Có thể gộp API 1 và 2 hoặc trả content cùng attempt nếu cần, nhưng phải giữ đủ dữ liệu và bảo đảm không tải toàn bộ bài tập của tất cả chủ đề mỗi lần mở Hôm nay. Có thể bổ sung abandon attempt sau; bản đầu cho phép thoát màn rồi tiếp tục bằng attempt đã lưu.

### 4.1 API biên soạn unit (chỉ content author)

Content author tạo unit trong màn biên soạn. Các API nằm dưới `/api/content/admin/vocabulary` (tiền tố biên soạn từ vựng sẵn có, gateway đã trỏ sang library-service). Chỉ vai trò `CONTENT_AUTHOR` được gọi; `ADMIN` không có quyền này. Mobile không gọi các API này.

| Endpoint | Công dụng |
|---|---|
| `GET /api/content/admin/vocabulary/topics/{topicId}/study-units` | Danh sách unit của chủ đề, gồm bản nháp và đã lưu trữ |
| `POST /api/content/admin/vocabulary/topics/{topicId}/study-units` | Tạo unit nháp: `{title, description, kind, introductionVi, senseIds[]}`; `senseIds` có thứ tự |
| `PUT /api/content/admin/vocabulary/study-units/{unitId}` | Sửa. Unit đã phát hành: tạo revision nháp mới; revision đang phát hành vẫn phục vụ cho đến khi phát hành revision mới |
| `PUT /api/content/admin/vocabulary/topics/{topicId}/study-units/order` | Sắp xếp lại: `{unitIds[]}` |
| `GET /api/content/admin/vocabulary/study-units/{unitId}/validation` | Lỗi chặn phát hành và cảnh báo |
| `POST /api/content/admin/vocabulary/study-units/{unitId}/publish` | Phát hành revision nháp; tăng `curriculumVersion` của chủ đề |
| `POST /api/content/admin/vocabulary/study-units/{unitId}/archive` | Gỡ khỏi người học; lịch sử và attempt cũ giữ nguyên |

Điều kiện chặn phát hành: ít nhất 4 nghĩa (sense) đang ACTIVE, mỗi nghĩa có `vietnameseMeaning`; có ít nhất một bước chấm điểm; CHOICE cần ít nhất 1 nghĩa khác trong chủ đề làm đáp án nhiễu (cảnh báo nếu dưới 3, vì khi đó chỉ có 2 phương án); MATCH chỉ sinh khi có từ 2 cặp; FILL chỉ sinh cho nghĩa có câu ví dụ chứa đúng dạng từ. Cảnh báo (không chặn): thiếu IPA/audio/ảnh.

## 5. Response chi tiết cho chủ đề, unit và tiến độ

Các mẫu dùng TypeScript để mô tả JSON. `Id` là string ID ổn định do BE sinh, `Instant` là ISO-8601 UTC. URL media là HTTPS dùng được từ điện thoại; trường thiếu nội dung trả null. Số đếm luôn là số không âm. Không dùng thứ tự hoặc số trang làm ID.

```ts
type Id = string;
type Instant = string;

type UnitSummary = {
  unitId: Id;
  topicId: Id;
  revision: number;
  title: string;                 // Ví dụ: Trong lớp học; phải đúng tập từ
  description: string | null;
  sortOrder: number;
  kind: 'LEARN' | 'REVIEW';
  wordCount: number;             // Số từ riêng biệt trong unit
  estimatedMinutes: number;
  exerciseTypes: ('CHOICE' | 'MATCH' | 'FILL')[];
};

type TopicUnits = {
  topicId: Id;
  curriculumVersion: number;
  totalUnits: number;
  uniqueWordCount: number;       // Không cộng trùng từ ở unit ôn tập
  items: UnitSummary[];
};

type StudyWord = {
  vocabularyItemId: Id;
  vocabularySenseId: Id;         // Bắt buộc chọn nghĩa cụ thể
  sortOrder: number;
  lemma: string;
  partOfSpeech: string | null;
  vietnameseMeaning: string;     // Bắt buộc cho bài EN–VI
  englishDefinition: string | null;
  ipaUk: string | null;
  ipaUs: string | null;
  audioUkUrl: string | null;
  audioUsUrl: string | null;
  imageUrl: string | null;
  exampleSentence: string | null;
  exampleTranslationVi: string | null;
};

type UnitContent = {
  unit: UnitSummary;
  introductionVi: string;
  words: StudyWord[];
};

type UnitProgress = {
  unitId: Id;
  status: 'LOCKED' | 'AVAILABLE' | 'IN_PROGRESS' | 'COMPLETED';
  prerequisiteUnitId: Id | null;
  currentAttemptId: Id | null;
  completedStepCount: number;
  totalStepCount: number;
  firstCompletedAt: Instant | null;
  lastCompletedAt: Instant | null;
  bestAccuracyPercent: number | null;
};

type TopicProgress = {
  topicId: Id;
  curriculumVersion: number;
  totalUnits: number;
  completedUnits: number;
  currentUnitId: Id | null;       // Bài đang học hoặc bài tiếp theo; null nếu xong
  units: UnitProgress[];
  updatedAt: Instant;
};
```

Yêu cầu nội dung:

- Mỗi unit LEARN có tập từ/nghĩa được biên soạn và sắp xếp rõ ràng. Không ép mọi chủ đề phải có đúng 9 unit.
- Unit ôn tập có thể dùng lại từ trước; tổng từ chủ đề không tính lặp.
- Không publish unit rỗng, từ không có nghĩa phù hợp, câu hỏi thiếu đáp án hoặc loại bài mobile chưa hỗ trợ.
- Catalog hiện đã có nhiều trường IPA/audio và senses. BE nên tái sử dụng dữ liệu này; lớp response có thể map tên trường theo hợp đồng mới.
- Audio/ảnh có thể thiếu; mobile dùng placeholder/TTS. Không chặn học chỉ vì media không tải được.
- Nếu dùng URL ký có hạn, trả `expiresAt` và hỗ trợ lấy URL mới qua GET content/attempt, không bắt mobile giữ URL hết hạn.

## 6. Phiên học, bài tập và nộp đáp án

```ts
type StepBase = {
  stepId: Id;
  order: number;
  instructionVi: string;
  imageUrl: string | null;
  audioUrl: string | null;
};

type StudyStep = StepBase & (
  | { type: 'WORD'; word: StudyWord }
  | { type: 'CHOICE'; prompt: string;
      options: { optionId: Id; text: string; imageUrl: string | null }[] }
  | { type: 'MATCH';
      left: { optionId: Id; text: string }[];
      right: { optionId: Id; text: string }[] }
  | { type: 'FILL'; prompt: string; sentenceBefore: string | null;
      sentenceAfter: string | null; inputMode: 'TEXT' | 'WORD_BANK';
      options: { optionId: Id; text: string }[] | null }
);

type Attempt = {
  attemptId: Id;
  topicId: Id;
  unitId: Id;
  revision: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'EXPIRED';
  mode: 'FIRST_COMPLETION' | 'REPLAY';
  startedAt: Instant;
  expiresAt: Instant | null;
  currentStepId: Id | null;
  completedStepIds: Id[];
  steps: StudyStep[];            // Snapshot, thứ tự/optionId ổn định trong attempt
  completion: Completion | null;
};

type Answer =
  | { type: 'WORD'; acknowledged: true }
  | { type: 'CHOICE'; optionId: Id }
  | { type: 'MATCH'; pairs: { leftOptionId: Id; rightOptionId: Id }[] }
  | { type: 'FILL_TEXT'; text: string }      // khi step.inputMode = 'TEXT'
  | { type: 'FILL_BANK'; optionId: Id };     // khi step.inputMode = 'WORD_BANK'

type StepResult = {
  attemptId: Id;
  stepId: Id;
  submissionId: Id;
  outcome: 'ACKNOWLEDGED' | 'CORRECT' | 'INCORRECT';
  correctAnswer: Answer | null;  // Feedback sau khi trả lời; WORD trả null
  explanationVi: string | null;
  stepCompleted: boolean;
  completedStepCount: number;
  totalStepCount: number;
  nextStepId: Id | null;         // Sai: giữ step hiện tại; đúng: sang step tiếp
  canComplete: boolean;
  heartLost: boolean;            // true nếu lần nộp này làm mất 1 tim
  hearts: HeartsView | null;     // null với attempt REPLAY (không có tim)
};
```

Quy tắc chấm và resume:

1. BE sinh hoặc tải câu hỏi từ nội dung đã duyệt, gắn với đúng unit/revision. Bản đầu không cần AI sinh theo từng request.
2. API bắt đầu trả lại attempt đang học nếu tồn tại; bấm đúp/retry không tạo nhiều phiên. REPLAY chỉ hợp lệ sau khi unit đã hoàn thành, mỗi lần replay có attemptId mới.
3. GET attempt trả bước đã lưu để đổi máy hoặc đóng app vẫn tiếp tục được. BE xác thực quyền sở hữu và trạng thái khóa khi bắt đầu/nộp bài.
4. WORD được xác nhận là đã xem; đây không phải câu trả lời chấm đúng/sai. Mobile vẫn cho nghe/đọc lại các từ đã xem.
5. CHOICE có 2–4 phương án hợp lệ, không có hai phương án hiển thị giống nhau nhưng bị chấm khác. Nếu unit ít từ, lấy distractor phù hợp từ nguồn khác hoặc dùng loại bài khác.
6. MATCH dùng ID độc lập cho hai cột, không để cùng ID bộc lộ cặp đúng. Mỗi câu MATCH gửi trọn bộ cặp; hoàn thành khi tất cả đúng. Nếu ít hơn 2 cặp thì không tạo bước MATCH.
7. FILL bản đầu là một chỗ trống. TEXT chuẩn hóa trim/Unicode/case/khoảng trắng; các biến thể đúng do BE khai báo. Không tự bỏ mọi dấu câu vì có thể đổi nghĩa; WORD_BANK gửi optionId. Điền nhiều ô/sắp xếp câu là mở rộng sau.
8. Cho làm lại câu sai; bước bắt buộc hoàn thành khi đúng. Accuracy tính trên lần trả lời đầu của mỗi bước có chấm điểm; mỗi MATCH tính một bước. WORD không nằm trong mẫu số. Nếu không có bước chấm điểm thì không publish unit có thưởng.
9. Không nhận `correct`, `score`, `xp`, `streak`, `unlockedUnitId` do client tự quyết định. Client chỉ gửi lựa chọn/câu trả lời và requestId.
10. Không trả cờ đáp án đúng trong câu hỏi CHOICE/MATCH/FILL trước khi nộp. Trang học từ vốn hiển thị nghĩa là chủ đích học tập; việc giấu đáp án không thay thế xác thực và chống gửi trùng phía server.
11. `requestId` là UUID do mobile sinh cho mỗi lần gửi logic. Retry dùng lại cùng ID và cùng body. Cùng ID khác body trả 409. Cùng ID cùng body trả kết quả ban đầu, kể cả khi attempt đã hoàn thành. Lần sửa đáp án dùng ID mới.
12. Cập nhật step, attempt và completion phải có kiểm soát đồng thời. Hai thiết bị hoàn thành cùng unit không mở khóa hoặc thưởng hai lần.
13. Tim (xem mục 7.5): sai lần đầu của một bước chấm điểm trong attempt `FIRST_COMPLETION` mất 1 tim; về 0 tim thì lần nộp chấm điểm kế tiếp trả `409 VOCAB_NO_HEARTS`. `REPLAY` không có tim.
14. Bài tập được sinh một lần khi tạo attempt, theo thứ tự cố định: WORD cho từng từ, CHOICE nghĩa của từ, MATCH theo nhóm tối đa 5 cặp, FILL điền từ vào câu ví dụ. Hạt giống ngẫu nhiên lấy từ `attemptId` để cùng attempt luôn ra cùng đề. Đáp án chuẩn nằm trong snapshot ở server, không trả về trước khi nộp.
15. `FILL_TEXT` chuẩn hóa trim, Unicode NFC, chữ thường và gộp khoảng trắng. Câu sai trả `correctAnswer` sau khi nộp.

## 7. Kết quả hoàn thành, XP, streak và tim (độc lập với web)

### 7.1 Nguyên tắc tách biệt

- XP, cấp độ, streak, tim của mobile là **hệ thống riêng**, nằm trong library-service. Không đọc hay ghi `learner_xp`, `xp_ledger`, `learner_streaks`, bảng tim của learning-service. XP web và XP mobile không cộng với nhau; hoàn thành bài trên web không ảnh hưởng streak/tim mobile và ngược lại.
- Dùng chung duy nhất: `userId` từ token và **ví điểm (Points) của access-service**, vì người dùng chọn mua tim và freeze bằng điểm. Số dư điểm là một cho cả web và mobile. Gói Premium (đọc từ access-service) cũng được dùng để quyết định số tim tối đa.
- Mọi phép tính (XP, level, streak, tim) chạy trong **cùng transaction** với việc ghi hoàn thành unit. Không có outbox, không có sự kiện sang Learning, không có trạng thái `PENDING`/`DISABLED`. Response `complete` luôn trả số thực.
- Mobile không tự cộng XP, tim hay ngày streak; chỉ hiển thị số server trả.

### 7.2 Kiểu dữ liệu

```ts
type XpView = {
  totalXp: number;
  level: number;                 // = floor(totalXp / levelXp) + 1
  nextLevelAt: number;           // = level * levelXp
  xpToNextLevel: number;
};

type StreakView = {
  currentDays: number;
  longestDays: number;
  lastQualifiedDate: string | null;   // YYYY-MM-DD theo timezone hồ sơ
  qualifiedToday: boolean;
  freezeAvailable: boolean;           // đang sở hữu 1 freeze chưa dùng
  freezeCost: number | null;          // điểm cần trả nếu mua bây giờ; null nếu không mua được
  freezeBlockedReason: 'NO_STREAK' | 'ALREADY_OWNED' | 'LIMIT' | null;
  lostStreakDays: number;             // độ dài chuỗi vừa mất, 0 nếu không mất
  timezone: string;                   // IANA
};

type HeartsView = {
  hearts: number;
  maxHearts: number;
  nextHeartAt: Instant | null;        // null khi đầy tim
  refillCost: number;                 // điểm để hồi đầy
};

type StudySummary = { xp: XpView; streak: StreakView; hearts: HeartsView };

type Completion = {
  attemptId: Id;
  unitId: Id;
  topicId: Id;
  completedAt: Instant;
  durationSeconds: number;
  gradedStepCount: number;
  firstTryCorrectCount: number;
  accuracyPercent: number;
  studiedWordCount: number;
  firstCompletion: boolean;
  topicProgress: TopicProgress;
  unlockedUnitIds: Id[];
  nextUnitId: Id | null;
  reward: {
    xpEarned: number;                 // 0 khi replay
    xpBreakdown: { firstCompletion: number; perfectBonus: number };
    leveledUp: boolean;
    streakIncreased: boolean;         // true nếu lần này làm chuỗi tăng
    summary: StudySummary;            // trạng thái sau khi hoàn thành
  };
};
```

`durationSeconds` là thời gian phiên theo mốc server, không dùng để tính thưởng.

### 7.3 Quy tắc XP và cấp độ

| Quy tắc | Giá trị mặc định (cấu hình BE) |
|---|---|
| Hoàn thành unit lần đầu | 20 XP |
| Thưởng không sai bước nào (`firstTryCorrectCount == gradedStepCount`) | +10 XP |
| Replay | 0 XP |
| Mỗi cấp | 500 XP |
| Trần XP mỗi ngày | Không có (số unit hữu hạn, XP tự giới hạn) |
| Lên cấp | Hồi đầy tim miễn phí |

- Chỉ có hai nguồn XP, ghi vào sổ XP riêng với khóa duy nhất `(userId, source, sourceRef)`: `UNIT_FIRST_COMPLETION` và `UNIT_PERFECT_BONUS`, `sourceRef = unitId`. Mỗi unit trả tối đa một lần mỗi loại, kể cả khi unit được sửa revision hay hai thiết bị cùng bấm hoàn thành.
- Thưởng "không sai bước nào" chỉ xét ở lần hoàn thành đầu. Lần đầu có sai thì replay hoàn hảo sau đó cũng không nhận bonus.
- Không có XP khi xem từ, lưu flashcard, hay ôn flashcard.

### 7.4 Quy tắc streak (có freeze, giống web nhưng sổ riêng)

- Ngày đủ điều kiện: ngày (theo timezone hồ sơ) mà người học **hoàn thành một attempt** (lần đầu hoặc replay) **hoặc ôn một flashcard** (`POST /api/learning-support/flashcards/{id}/reviews` tạo lượt ôn mới, đúng hay sai đều tính; gửi lại cùng `requestId` không tính). Ôn flashcard chỉ giữ streak, không cộng XP. Ngày được tính theo `completedAt` của server và lưu cố định cùng bản ghi; đổi timezone sau đó không đổi ngày đã ghi.
- Nhiều unit trong một ngày chỉ tăng chuỗi một lần. Một ngày không hoàn thành unit nào mà không có freeze thì chuỗi về 0 (đánh giá khi đọc, không cần job đêm).
- **Freeze**: mua bằng điểm. Giá `10 × số ngày freeze liên tiếp tính cả ngày này`, tối đa 5 ngày liên tiếp, chỉ giữ 1 freeze tại một thời điểm, chỉ mua được khi chuỗi hiện tại > 0. Freeze phủ ngày trống kế tiếp kể từ hôm nay. Dùng lại công thức `StreakRules` của web nhưng sao chép sang library-service (không import module learning).
- Không có thưởng mốc streak (XP/điểm khi đạt 7, 30 ngày) trong bản đầu.
- Chuỗi ôn thẻ có sẵn trong `/notebook/achievements` vẫn là thành tích riêng, không đổi; streak học từ vựng là số trong `StudySummary.streak`.

### 7.5 Quy tắc tim

- Tối đa 5 tim (Premium 7), hồi 1 tim mỗi 5 giờ; đồng hồ dừng khi đầy tim (đúng như web). Người học mới bắt đầu với đầy tim.
- Chỉ attempt `FIRST_COMPLETION` có tim. Sai lần đầu của **một bước chấm điểm** (CHOICE, MATCH, FILL) mất 1 tim; các lần sai sau của cùng bước không mất thêm. Bước WORD không mất tim.
- `REPLAY` không kiểm tra và không mất tim.
- Khi 0 tim: không bắt đầu được attempt `FIRST_COMPLETION` mới và không nộp được đáp án chấm điểm của attempt đang làm (`409 VOCAB_NO_HEARTS`, kèm `nextHeartAt` và `refillCost`). Vẫn xem được nội dung unit, vẫn `GET` attempt, vẫn replay unit đã hoàn thành. Attempt đang làm giữ nguyên, tiếp tục được khi có tim.
- **Hồi đầy bằng điểm**: `POST /me/hearts/refill`, giá mặc định 5 điểm. Từ chối khi đã đầy (`409 HEARTS_FULL`).
- Gói Premium được server làm mới tối đa mỗi 10 phút mỗi người. Nâng lên Premium không tự hồi tim (5/5 thành 5/7); hạ xuống thì tim bị cắt về tối đa 5. Access không phản hồi thì giữ mức tối đa đã lưu. `HeartsView.maxHearts` luôn là mức hiện hành; mobile không tự suy ra.

### 7.6 Mua tim và freeze bằng điểm (gọi sang Access)

Trừ điểm là lời gọi HTTP duy nhất sang service khác. Không gọi khi đang giữ transaction hay khóa người dùng. Quy trình ba bước, an toàn khi gọi lại cùng `requestId`:

1. **Ghi nhận**: transaction ngắn kiểm tra điều kiện (chưa đầy tim / đủ điều kiện freeze) và ghi dòng `purchase` trạng thái `PENDING` theo `requestId`. Nếu `requestId` đã có `DONE` thì trả lại kết quả đã lưu; nếu khác loại hoặc khác người dùng thì `409 IDEMPOTENCY_CONFLICT`.
2. **Trừ điểm**: gọi Access `POST /internal/access/points/debit` với token của người học, `referenceType` là `HEART_REFILL` hoặc `STREAK_FREEZE`, `referenceId` và `idempotencyKey` là `requestId`. Access đã idempotent theo khóa này. Thiếu điểm trả `402 INSUFFICIENT_POINTS`; Access lỗi trả `503 ACCESS_UNAVAILABLE`, dòng `PENDING` được giữ để retry.
3. **Áp dụng**: transaction ngắn có khóa người dùng ghi hiệu ứng (hồi đầy tim / đặt freeze) và đánh dấu `DONE` cùng kết quả. Mobile retry cùng `requestId` sẽ hoàn tất dòng `PENDING` mà không trừ điểm lần hai.

Rủi ro đã biết: tim tự đầy do hồi theo giờ ngay giữa bước 2 và 3 thì người học trả điểm cho một lần hồi không cần thiết. Cửa sổ rất ngắn; chấp nhận ở bản đầu và ghi trong kiểm thử.

### 7.7 Cấu hình

Giá trị mặc định đặt trong cấu hình library-service, không hardcode trong mobile:

```
library.vocabulary-study.xp.first-completion=20
library.vocabulary-study.xp.perfect-bonus=10
library.vocabulary-study.xp.level-xp=500
library.vocabulary-study.hearts.max=5
library.vocabulary-study.hearts.premium-max=7
library.vocabulary-study.hearts.plan-refresh-minutes=10
library.vocabulary-study.hearts.regen-hours=5
library.vocabulary-study.hearts.refill-cost=5
library.vocabulary-study.streak.freeze-price=10
library.vocabulary-study.streak.max-consecutive-freezes=5
library.access.base-url=...
```

### 7.8 Những gì không đổi và không làm

- Không import tiến độ AsyncStorage cũ thành XP/streak.
- Không có bảng xếp hạng, huy hiệu, nhiệm vụ ngày, hay thưởng mốc streak. Thêm sau nếu có yêu cầu.
- `GET /api/learning/me/progress`, `/api/learning/me/streak`, `/api/learning/me/settings` vẫn phục vụ web; mobile từ vựng không gọi chúng.

## 8. Lưu từ vào flashcard và thống kê

- Giữ thao tác lưu từ do người dùng chọn. Ưu tiên API notebook/entries với vocabularySenseId để tái sử dụng flashcard đã có, tránh tạo nhiều thẻ cho cùng nghĩa.
- Nếu vẫn tổ chức theo bộ của chủ đề, mobile có thể gọi API deck hiện có sau khi lấy flashcardId từ notebook. Nếu cần tự động tạo/gắn bộ một cách atomic, BE có thể bổ sung thao tác riêng; không phải điều kiện bắt buộc để hoàn thành unit.
- Không gửi review flashcard giả cho mọi từ vừa học để tạo số liệu hoặc thưởng. Ôn SRS và hoàn thành unit là hai sự kiện khác nhau.
- Lượt ôn flashcard thật giữ streak mobile (mục 7.4) nhưng không cộng XP. Phản hồi của API review không đổi; mobile đọc lại `me/summary` để thấy streak.
- Thống kê notebook và tiến độ unit cần tên/semantics riêng. “3/9 bài hoàn thành” không tương đương “33% từ đã ghi nhớ”.
- API catalog công khai không chứa cờ `saved` theo user; lấy trạng thái đã lưu bằng API saved hoặc response xác thực riêng.

## 9. Phiên bản nội dung và dữ liệu lưu

Các dữ liệu logic cần có (tên bảng do BE quyết định):

- Unit: ID ổn định, topicId, thứ tự, title, kind, trạng thái draft/published, revision.
- Unit revision: tập vocabularySenseId có thứ tự; mẫu bài tập và đáp án chuẩn. Snapshot revision đã dùng không bị sửa tại chỗ.
- User unit progress: userId, unitId, trạng thái/hoàn thành, attempt hiện tại, thời điểm, kết quả tốt nhất. Unique userId + unitId.
- Attempt + step submissions: userId, unit/revision, trạng thái, thứ tự, đáp án đã nộp, kết quả server, requestId và payload hash để idempotency.
- Hồ sơ từ vựng của người học: timezone, tổng XP, cấp độ, tim, mốc hồi tim. Một dòng mỗi người dùng.
- Sổ XP: một dòng mỗi lần thưởng, khóa duy nhất `(userId, source, sourceRef)` để chống trả hai lần.
- Streak: dạng dữ liệu của `learner_streaks` bên web (ngày hiện tại, kỷ lục, ngày cuối, đã đánh giá đến ngày nào, freeze), bảng riêng trong library-service.
- Giao dịch mua (tim, freeze): `requestId` làm khóa chính, loại, số điểm, trạng thái `PENDING`/`DONE`, id sổ điểm bên Access, kết quả đã lưu để trả lại khi retry.
- Mọi bảng mới dùng tiền tố `vocabulary_study_`, Flyway của library-service bắt đầu từ V39. Không có khóa ngoại sang bảng của service khác.
- Revision mới không làm mất lịch sử. Attempt đang làm dùng snapshot cũ; nếu bắt buộc thu hồi thì trả lỗi rõ, không âm thầm chấm bằng đáp án mới.
- Không khóa lại bài đã hoàn thành khi thêm/chèn unit. Bài mới nhận trạng thái theo prerequisite đã xác định. Progress trả curriculumVersion để mobile phát hiện cache nội dung cũ.
- Tiến độ AsyncStorage cũ không phải bằng chứng đáng tin cậy để thưởng hồi tố. Bản đầu không chuyển số local thành XP/streak; nếu muốn import tiến độ cần endpoint/policy riêng và đánh dấu nguồn import, không thưởng tự động.
- Khách có thể xem/học thử local bằng catalog. Đồng bộ tài khoản và thưởng chỉ áp dụng khi đăng nhập. Bản đầu không yêu cầu thưởng offline; mất mạng giữ đáp án chờ retry, không tự mở khóa bằng kết quả chưa xác nhận.

## 10. Lỗi và quy ước response

Response lỗi theo đúng `ErrorResponse` hiện có của library-service (`details` là map chuỗi sang chuỗi; mọi giá trị là string):

```json
{
  "timestamp": "2026-11-07T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Hoàn thành bài trước để tiếp tục.",
  "path": "/api/learning-support/vocabulary-study/units/3d9f…/attempts",
  "details": { "requiredUnitId": "7a10…" },
  "code": "VOCAB_UNIT_LOCKED"
}
```

| HTTP | Code đề xuất | Mobile xử lý |
|---|---|---|
| 401 | UNAUTHENTICATED | Đăng nhập lại |
| 403 hoặc 409 | VOCAB_UNIT_LOCKED | Hiện bài cần hoàn thành, tải lại progress |
| 404 | VOCAB_UNIT_NOT_FOUND / ATTEMPT_NOT_FOUND | Không tồn tại, không thuộc người dùng hoặc không công khai |
| 409 | ATTEMPT_EXPIRED / CONTENT_VERSION_RETIRED | Thông báo và tải phiên hợp lệ; không mất lịch sử đã hoàn thành |
| 409 | IDEMPOTENCY_CONFLICT | Cùng requestId khác dữ liệu; không tự retry với ID mới |
| 409 | STEP_OUT_OF_ORDER / ATTEMPT_NOT_COMPLETABLE | Lấy lại attempt và tiếp tục đúng bước |
| 409 | ATTEMPT_ALREADY_COMPLETED | Tải lại attempt, hiển thị kết quả đã có |
| 409 | VOCAB_UNIT_COMPLETED / VOCAB_UNIT_NOT_COMPLETED | Gọi lại với `mode` đúng (`REPLAY` cho unit đã xong, `RESUME_OR_START` cho unit chưa xong) |
| 409 | VOCAB_UNIT_UNAVAILABLE / VOCAB_UNIT_ARCHIVED | Tải lại danh sách unit |
| 404 | STEP_NOT_FOUND / VOCAB_TOPIC_NOT_FOUND | Tải lại dữ liệu |
| 422 | VOCAB_UNIT_INVALID | (API biên soạn) Unit chưa đủ điều kiện phát hành; `details` nêu lý do |
| 409 | VOCAB_NO_HEARTS | Mở sheet hết tim: chờ `nextHeartAt` hoặc hồi bằng điểm; giữ nguyên attempt |
| 409 | HEARTS_FULL / FREEZE_ALREADY_OWNED / NO_STREAK_TO_PROTECT / FREEZE_LIMIT | Tải lại summary, ẩn nút tương ứng |
| 402 | INSUFFICIENT_POINTS | Báo thiếu điểm, không retry |
| 503 | ACCESS_UNAVAILABLE | Retry cùng requestId |
| 422 | INVALID_ANSWER | Chỉ rõ field không hợp lệ; đáp án sai bình thường trả 200 + INCORRECT |
| 429 | RATE_LIMITED | Trả Retry-After |
| 5xx | SERVER_ERROR | Cho retry cùng requestId |

Mỗi lỗi nghiệp vụ phải có `code` (machine-readable); hiện library-service chưa điền `code` cho lỗi nào nên cần thêm exception mang mã lỗi (xem plan BE, mục 7). Không có `traceId`; dùng correlation id của gateway để truy vết. Media lỗi không biến thành lỗi mất kết nối API toàn màn hình.

## 11. Checklist nghiệm thu để đội BE bàn giao

- [ ] Có dữ liệu mẫu ít nhất 2 chủ đề, mỗi chủ đề nhiều unit với từ đúng nội dung tên bài.
- [ ] Mọi unit trả nội dung không rỗng; wordCount/uniqueWordCount chính xác, sortOrder ổn định.
- [ ] Phân biệt đúng các nghĩa của từ đa nghĩa bằng vocabularySenseId.
- [ ] Hỗ trợ WORD, CHOICE, MATCH, FILL; không tạo quiz chỉ có một lựa chọn hoặc ghép một cặp.
- [ ] Mở bài đầu mỗi chủ đề, chặn bài khóa trên server; complete hợp lệ mở đúng bài tiếp.
- [ ] Đang học ở máy A, đăng nhập máy B thấy cùng tiến độ và tiếp tục đúng bước.
- [ ] Sai đáp án có feedback, làm lại được; accuracy lần đầu không bị tăng giả khi retry.
- [ ] Mất mạng sau khi server đã nhận: gửi lại answer/complete không ghi trùng, không thưởng trùng.
- [ ] Hai máy bấm hoàn thành đồng thời vẫn chỉ có một lần thưởng unit.
- [ ] Replay không thưởng XP; vẫn tính ngày streak.
- [ ] Bonus +10 chỉ ở lần hoàn thành đầu và chỉ khi không sai bước nào; sửa revision không trả XP lần hai.
- [ ] XP, streak, tim của mobile không đổi khi làm bài trên web và ngược lại (kiểm thử hai chiều).
- [ ] Nửa đêm theo múi giờ, đổi múi giờ, hai thiết bị cùng hoàn thành đều cho streak và XP đúng một lần.
- [ ] Về 0 tim: không bắt đầu được attempt mới, không nộp đáp án chấm điểm; replay vẫn mở; tim hồi sau 5 giờ.
- [ ] Ôn một flashcard trong ngày giữ streak mobile (kể cả khi chưa hoàn thành unit nào), không cộng XP; gửi lại cùng `requestId` không tính thêm.
- [ ] Premium có tối đa 7 tim, người thường 5; hạ gói thì tim bị cắt; Access lỗi vẫn học được với mức tối đa đã lưu.
- [ ] Khách không token đọc được hai endpoint nội dung unit; mọi API `vocabulary-study/*` còn lại trả 401.
- [ ] Hồi tim và mua freeze: thiếu điểm bị từ chối; gọi lại cùng `requestId` chỉ trừ điểm một lần; Access lỗi không mất điểm hay đổi trạng thái.
- [ ] Content author biên soạn, kiểm tra hợp lệ, phát hành, lưu trữ unit; `ADMIN` không gọi được các API này; attempt đang làm dùng snapshot cũ.
- [ ] Thêm từ/sửa nội dung không làm mất nghĩa ID bài hoặc kết quả cũ.
- [ ] Ảnh/audio HTTPS dùng được từ thiết bị; dữ liệu null có fallback.
- [ ] Không cần tạo flashcard để được học/hoàn thành unit; lưu từ là thao tác độc lập.
- [ ] Cung cấp OpenAPI, enum, ví dụ request/response và lỗi, dữ liệu seed, giới hạn payload/phân trang, cơ chế auth/gateway.

Thứ tự thực hiện: **P0** unit, biên soạn của content author, attempt/chấm đáp án, progress/mở khóa; **P1** XP, cấp độ, streak, tim, mua bằng điểm trong library-service; **P2** nhập tiến độ cũ, offline sync, thống kê nâng cao nếu có yêu cầu. Kế hoạch từng giai đoạn nằm ở `plans/261106-2330-mobile-vocabulary-study/`. API notebook đã tồn tại được mobile tích hợp song song.

## 12. Căn cứ mã BE đã đọc

- `backend/services/library-service/.../api/controller/VocabularyCatalogController.java`
- `backend/services/library-service/.../api/controller/VocabularyController.java`
- `backend/services/library-service/.../api/controller/VocabularyLookupController.java`
- `backend/services/library-service/.../api/controller/VocabularyNotebookController.java`
- `backend/services/library-service/.../api/controller/VocabularyProgressController.java`
- `backend/services/library-service/.../application/usecase/ReviewFlashcardUseCase.java`
- `backend/services/learning-service/.../application/service/GamificationRecorder.java`
- `backend/services/learning-service/.../application/service/StreakRecorder.java`
- `backend/services/learning-service/.../api/controller/GamificationController.java`
- `backend/services/learning-service/.../api/controller/StreakController.java`
- `backend/services/learning-service/.../application/service/HeartsService.java`, `.../domain/aggregate/Hearts.java`, `.../domain/service/StreakRules.java`, `.../application/usecase/StreakUseCase.java` (quy tắc tim và freeze được sao chép sang library-service)
- `backend/services/learning-service/.../infrastructure/client/RestAccessClient.java`, `backend/services/access-service/.../api/controller/InternalPointsController.java`, `.../application/usecase/DebitPointsUseCase.java` (mua bằng điểm)
- `backend/infra/config-server/config-repo/api-gateway.yaml` (route `library-service`, `library-personal-service`)
- `backend/services/library-service/src/main/resources/db/migration` (V19 topic/set, hiện tới V38)

Đây là đối chiếu source code trong workspace, không phải xác nhận các API mới đã tồn tại trên môi trường đang chạy.
