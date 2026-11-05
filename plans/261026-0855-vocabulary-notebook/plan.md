# Sổ từ vựng (Vocabulary Notebook)

- Trạng thái: proposed (chờ duyệt)
- Nhánh: `feat/vocabulary-notebook` (BE và FE)
- Ngày: 2026-10-26

## Bối cảnh

- Figma đã đối chiếu 2026-10-26 (file `MuSfpqT61Xk6vgtvqDJEH6`, trang LMS `1:3`, section "Từ điển 2" `3042:15077`):
  `6038:19017` "TIS - Từ Điển" (trang chủ từ điển), `3967:17116` "Search từ vựng" (chi tiết từ), `3042:16743`
  "tổng quan bộ từ vựng", `3042:15953` "Bản đồ từ vựng"; section "Học từ vựng" `3959:17478` (Đoán nghĩa, Luyện nhớ,
  Kiểm tra, Đặt câu). Figma gọi sổ từ là **"Sổ tay"**: một tab trong "Khám phá bộ từ vựng" + nút "Lưu vào sổ tay";
  **không có màn riêng** cho Sổ tay → màn sổ từ trong plan là phần thêm, phải dùng cùng ngôn ngữ thiết kế.
  Header Figma có "Đăng nhập/Đăng ký" trên trang từ điển → khớp quyết định từ điển công khai.
  Wireframe repo (`docs/wireframes/exports/P-50`, `P-57`, `P-59*`) vẫn dùng làm tham chiếu phụ.
- Figma có nhiều dữ liệu DB **chưa có**: CEFR, chủ đề, band, Frequency (COCA), Academic Word List, IPA tách UK/US,
  Collocations/Thesaurus/Common Mistakes, bản dịch câu ví dụ, bộ từ theo band/chủ đề, trình độ B1 + "Test lại",
  mục tiêu tuần, bản đồ từ vựng theo band. **Người dùng chốt 2026-10-26: làm đầy đủ** → thêm phase 03 (schema + API
  catalog + admin + seed demo) và phase 04 (log ôn, mục tiêu tuần, bản đồ, tiến độ chủ đề/bộ, thành tích).
  Dữ kiện đo được (tần suất COCA) không bịa: null khi chưa có nguồn; seed demo ghi rõ "cần người duyệt".
- `docs/plans/reports/api-mapping-261013-1423-new-ui-screens-report.md` dòng 95-102: lưu từ dùng flashcard
  `VOCABULARY_SENSE`; mức độ hiểu (D2) **thiếu**.
- Commit `ea515ba2` đã **chủ động xóa** `VocabularyReviewController`/`VocabularyProgress` khỏi library ("approved scope").
  Vì vậy không dựng lại bảng tiến độ riêng; sổ từ dựa trên `flashcards` + Leitner (`leitner_box` 1-5, `next_review_at`).
- Lỗi đang có: FE `features/vocabulary/api/vocabularyReviewApi.ts` và `VocabularyReviewPage` (route `/vocabulary/review`)
  gọi `vocabulary-study-items`/`vocabulary-review-sessions` không còn tồn tại → luôn lỗi. Plan thay trang này.

## Quyết định thiết kế

1. Một mục sổ từ = một flashcard `ACTIVE` có `source_type = 'VOCABULARY_SENSE'`. Không thêm aggregate mới.
2. Mức độ hiểu suy ra từ `leitner_box`, không thêm cột:
   box 1 `UNKNOWN` (Chưa biết) · box 2 `FAMILIAR` (Mới biết) · box 3 `LEARNING` (Đang học) · box 4-5 `MASTERED` (Đã nhớ).
   Học viên đặt mức thủ công → đổi box (1/2/3/4) và `next_review_at` theo `library.srs.intervals-days`; `UNKNOWN` thì đến hạn ngay.
3. Một mục sống cho mỗi (user, sense): index unique một phần (migration mới) + use case idempotent.
4. Ghi chú theo từ dùng bảng `notes` có sẵn: thêm `NoteSourceType.VOCABULARY_SENSE` (enum app, không cần đổi CHECK).
5. Không cộng XP/điểm ở library (XP thuộc learning-service, điểm đi qua outbox). Không gọi LLM (cần quota + chính sách AI).
6. Bỏ mock: trang từ điển `/vocabulary` đang render `LESSON_VOCABULARY` từ `src/mocks/lessonData.ts` (6 từ cứng).
   Chuyển sang catalog thật trong `library_db` (`vocabulary_items` + `vocabulary_senses`, seed V4/V15) qua endpoint duyệt
   có phân trang mới. `GET /api/content/vocabulary/search` hiện không giới hạn (query rỗng trả toàn bộ catalog) nên không
   dùng để duyệt. DB không có CEFR/chủ đề/tag cho từ → UI bỏ các trường đó, không bịa giá trị.
   **Từ điển công khai (người dùng duyệt 2026-10-26):** khách không đăng nhập xem được danh sách và chi tiết từ.
   Chỉ mở **GET** đọc catalog: `/api/content/vocabulary` (duyệt, phân trang) và `/api/content/vocabulary/{id}` (UUID).
   Giữ đăng nhập cho `/search` (không giới hạn kết quả, lesson editor đang dùng), `/senses`, mọi POST/admin và toàn bộ
   sổ từ. Đây là thay đổi ranh giới bảo mật: sửa cả Gateway và library, mỗi tầng chỉ mở GET.
7. Animation chỉ CSS + `tw-animate-css` đã có; không thêm framer-motion/thư viện mới; tôn trọng `prefers-reduced-motion`.

## Phase

| # | File | Phụ thuộc | Chủ sở hữu |
| --- | --- | --- | --- |
| 1 | [phase-01-library-notebook-api.md](phase-01-library-notebook-api.md) | — | Agent BE-1 (V18) |
| 3 | [phase-03-library-catalog-enrichment.md](phase-03-library-catalog-enrichment.md) | Phase 1 | Agent BE-2 (V19, V20) |
| 4 | [phase-04-library-learner-progress.md](phase-04-library-learner-progress.md) | Phase 1, 3 | Agent BE-3 (V21) |
| 2 | [phase-02-fe-notebook-screens.md](phase-02-fe-notebook-screens.md) | Contract phase 1/3/4 | Agent FE-1 (từ điển, chi tiết, sổ tay, ôn) rồi FE-2 (bộ từ, bản đồ, mindmap, admin) |

Thứ tự chạy: BE-1 → BE-2 → BE-3 (cùng `library-service`, cùng chuỗi migration → **không chạy song song**).
FE-1 bắt đầu sau BE-1; FE-2 sau BE-3.
| — | [agent-prompts.md](agent-prompts.md) | Prompt giao cho agent BE/FE | — |

## Tiêu chí chấp nhận

- Lưu một từ hai lần không tạo bản ghi thứ hai (trả mục cũ, 200).
- Danh sách sổ từ phân trang, lọc theo mức/tìm kiếm/sắp xếp ở database; không N+1 khi ghép metadata từ vựng.
- Đổi mức hiểu cập nhật box và hạn ôn; ôn tập đi qua `POST /flashcards/{id}/reviews` sẵn có (idempotent theo `requestId`).
- FE: trang sổ từ, ngăn chi tiết, phiên ôn nhiều chế độ chạy với API thật; trạng thái chờ/lỗi/rỗng; dùng được bằng bàn phím; mobile-first.
- `mvn -q -pl services/library-service -am test` xanh (Testcontainers cho query mới); FE `npm run lint && npm run typecheck && npm run build && npm test` xanh.

## Câu hỏi mở

1. DB thật đã có bản trùng (user, sense) chưa? Nếu có, migration index unique sẽ fail → cần duyệt cách gộp (không tự xóa dữ liệu).
2. Sổ từ chỉ gồm `VOCABULARY_SENSE`, hay cả thẻ `HIGHLIGHT`/`MANUAL` (thẻ người dùng tự tạo trong deck)? Plan mặc định chỉ `VOCABULARY_SENSE`.
3. ~~Trang từ điển dùng mock~~ — đã chốt: chuyển sang dữ liệu thật (quyết định 6).
4. Có cần thêm CEFR/chủ đề cho từ vựng (migration + seed) không? Hiện ngoài scope; UI ẩn các trường này.
5. ~~Làm khối Figma cần dữ liệu mới?~~ — đã chốt: làm đầy đủ (phase 03, 04).
6. Nguồn dữ liệu tần suất COCA (có license?) — chưa có thì để null. Ai duyệt seed demo trước khi lên production?
