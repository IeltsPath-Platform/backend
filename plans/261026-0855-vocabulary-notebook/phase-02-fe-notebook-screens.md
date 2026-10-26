# Phase 02 — FE: màn hình sổ từ vựng

Repo: `d:\Fall2026\New folder\FE\frontend` (nhánh `feat/vocabulary-notebook`).

## Đọc trước

- `AGENTS.md`, `README.md`, `.sdd/global/constitution.md`, `.sdd/constraints/global.md` của FE
- Feature tham khảo: `src/features/flashcards/` (`FlashcardFace`, `CorrectBurst`, `OwlCoach`, `lib/speak.ts`,
  `lib/answer-feedback.ts`, `flashcard-modes.css`), `src/features/vocabulary/`, `src/features/learning-path/components/{LessonVocabulary,PronunciationButton}.tsx`
- `src/lib/httpClient.ts`, `src/components/ui/` (shadcn), `src/app/App.tsx`, `src/components/SiteNavbar.tsx`
- Wireframe: `d:\Fall2026\New folder\docs\wireframes\exports\P-50__populated.png`, `P-57__dinh-nghia.png`, `P-59a__*.png`, `P-59d__*.png`
- Figma (nguồn chính về giao diện): file `MuSfpqT61Xk6vgtvqDJEH6`. Gọi `create_session` rồi `get_design_context` /
  `get_screenshot` cho từng node, **không** gọi `get_metadata` trên cả trang `1:3` (8,8 triệu ký tự). Lấy màu/typography
  bằng `get_variable_defs` và map sang token CSS hiện có.

## Đối chiếu Figma → màn hình

**Làm đầy đủ mọi khối Figma** (người dùng chốt 2026-10-26), dữ liệu từ phase 01/03/04. Khối nào API trả null
(ví dụ `frequencyRank` chưa có nguồn) thì ẩn riêng dòng đó, không điền số giả.

| Node Figma | Route | Khối và nguồn dữ liệu |
| --- | --- | --- |
| `6038:19017` TIS - Từ Điển | `/vocabulary` (public) | Hero "Từ điển và học từ vựng", "Bạn đã học X / Y từ" + thanh % (`summary.learnedCount/catalogTotal`), "Tiếp tục học" (chủ đề hiện tại) / "Ôn từ cần nhớ" (review); thẻ **Trình độ hiện tại** (CEFR từ `estimatedBand` của `GET /api/learning/report`, nút "Test lại" theo luật placement ở phase 04) + 3 bộ đếm; ô tìm có dropdown **Anh–Việt / Việt–Anh** (`lang`), gợi ý khi gõ (`/suggestions`), "Gần đây" (localStorage); **Chủ đề đang học** (`topics-progress.current`); **Mục tiêu tuần** T2–CN (`weekly-goal`, bấm số để sửa mục tiêu); **Khám phá bộ từ vựng** 4 tab: Theo band điểm (`/sets?kind=BAND`), Theo chủ đề (`/sets?kind=TOPIC` + `/topics`), Sổ tay (→ `/vocabulary/notebook`), Mindmap (`/mindmap`, vẽ bằng `@xyflow/react` đã có trong dependency, như `features/admin/knowledge-graph`); **Bản đồ từ vựng của bạn** 6 cột chồng 4 màu Đã học/Đang ôn/Cần ôn lại/Chưa học (`vocabulary-map`), cột hiện tại có nhãn "Trình độ hiện tại", "Cần bổ sung"; "Xem bản đồ chi tiết" → `/vocabulary/map` (`3042:15953`). Khách: khối cá nhân thay bằng CTA đăng nhập, khối catalog vẫn hiện. |
| `3967:17116` Search từ vựng | `/vocabulary/:id` (public) | Breadcrumb, lemma, IPA **UK và US** mỗi cái một nút nghe, "Lưu vào sổ tay"; ô Loại từ / CEFR / Nghĩa / Chủ đề; tab **Definitions · Collocations · Thesaurus · Common Mistakes · Examples** có số đếm (`counts`), nhãn "Ngữ pháp & cách dùng", "Ứng dụng IELTS", ví dụ có dịch; Word family; cột phải: Hình ảnh ghi nhớ, Mức độ hiểu (đăng nhập), **Thông tin nhanh** (Frequency COCA, CEFR, Academic Word List, Chủ đề), Ghi chú 0/300 (đăng nhập), "Lưu và học từ này", "Ôn từ này sau". Từ trong synonym/collocation có `relatedItemId` thì là link. |
| `3042:16743` tổng quan bộ từ vựng | `/vocabulary/sets/:id` (public, phần tiến độ khi đăng nhập) | Hero bộ (tên, mô tả, "Đã học X/Y", owl, 3 thẻ Đã thành thạo/Đang học/Cần ôn từ `sets/{id}/progress`); tab Tổng quan · Chủ đề · Tất cả từ vựng · Tiến độ; "Tiếp tục lộ trình", thẻ chủ đề có % và nhãn Hoàn thành/Đang học/Chưa học, bảng "Từ vựng gần đây", "Lưu bộ từ" (lưu cả bộ vào sổ tay — gọi `POST /notebook/entries` theo lô có giới hạn, hoặc endpoint lô nếu BE thêm); cột phải Mục tiêu tuần, **Kết quả gần nhất** (`recent-result`), Cách học đề xuất (tĩnh), **Thành tích bộ từ** (`achievements`); thanh "Sẵn sàng học tiếp?". |
| (không có màn riêng) | `/vocabulary/notebook` (auth) | Trang Sổ tay dùng cùng ngôn ngữ thiết kế của `3042:16743`; nội dung ở mục "Màn hình 1" bên dưới. |
| `3959:17478` Học từ vựng | `/vocabulary/notebook/review`, `/vocabulary/sets/:id/study` | Đoán nghĩa (`4002:19639`, đúng `4016:21190`, sai `4013:20369`), Luyện nhớ (`4099:17728`), Kiểm tra (`4104:18321`), **Đặt câu** (`4034:17523`: học viên viết câu, hiện câu mẫu từ `examples`, tự đánh giá Nhớ/Quên — không LLM), Chi tiết bài/Lộ trình (`3959:18608`, `3959:17824`), thông báo skip/hoàn thành chặng (`4104:18034`, `6007:18811`, `6063:19716`, `4113:19345`). Mỗi phiên tạo `sessionId` gửi kèm review để có "Kết quả gần nhất". |
| `3808:18343` Trang từ vựng game | — | Ngoài scope (thuộc game-service); chỉ thêm link "Chơi game từ vựng" nếu route game đã có. |

Nhãn theo Figma: "Sổ tay", "Lưu vào sổ tay", "Chưa biết / Mới biết / Đang học / Đã nhớ", "Đã thành thạo",
"Cần ôn". Map: Đã thành thạo = `MASTERED`; Đang học = `FAMILIAR + LEARNING`; Cần ôn = `dueNow`.

## Route

| Route | Trang | Ghi chú |
| --- | --- | --- |
| `/vocabulary/:id` | `WordDetailPage` | Public. |
| `/vocabulary/sets/:id` | `VocabularySetPage` | Public; tiến độ khi đăng nhập. |
| `/vocabulary/sets/:id/study` | `SetStudyPage` | `RequireAuth`. |
| `/vocabulary/map` | `VocabularyMapPage` (`3042:15953`) | `RequireAuth`. |
| `/admin/vocabulary` | trang admin sửa từ, nghĩa, collocation, thesaurus, lỗi thường gặp, ví dụ, chủ đề, bộ từ | `CONTENT_AUTHOR`/`ADMIN`, theo pattern `features/admin/*` hiện có. |
| `/vocabulary/notebook` | `VocabularyNotebookPage` | `RequireAuth`. Navbar đổi nhãn "Kho từ vựng" → "Từ điển" như Figma. |
| `/vocabulary/notebook/review` | `NotebookReviewPage` | Phiên ôn; query `?level=&mode=` |
| `/vocabulary/review` | redirect → `/vocabulary/notebook/review` | Thay trang cũ đang gọi API đã bị xóa; xóa `vocabularyReviewApi` phần chết (giữ type còn dùng ở overview). |

## Màn hình 0 — Từ điển `/vocabulary`: bỏ mock, dùng dữ liệu thật

- `VocabularyPage.tsx` đang import `LESSON_VOCABULARY` từ `@/mocks/lessonData` (6 từ cứng, kèm `level`, `tags`, tiêu đề
  "Nature & Environment" cứng). Thay bằng `GET /api/content/vocabulary?q=&page=&size=` (phase 01) qua hook có
  `AbortController`, tìm kiếm debounce 300ms ở server (không lọc trong bộ nhớ), "Tải thêm" theo trang.
- Map `VocabularyItemResponse` (mở rộng ở phase 03: IPA UK/US, CEFR, chủ đề…) sang props của `VocabularyCollection`;
  cập nhật kiểu props cho khớp API thay vì `VocabularyEntry` của mock. Trường nào null thì ẩn.
- Số từ ở hero lấy từ `totalElements` của trang, không đếm mảng.
- Nút "Lưu" gọi `POST /notebook/entries` với sense đầu (hoặc sense được chọn); trạng thái đã lưu lấy từ
  `GET /notebook/saved?senseIds=` cho các sense trên trang hiện tại; khách chưa đăng nhập bấm Lưu → chuyển đăng nhập.
- Bỏ `useState savedEntries` giả lập. Xóa `src/mocks/lessonData.ts` nếu không còn chỗ nào import (đã kiểm: chỉ
  `VocabularyPage` dùng; chạy lại grep trước khi xóa, giữ type nếu nơi khác cần).
- `/vocabulary` **công khai**, không bọc `RequireAuth`. Gọi catalog không bắt buộc token (kiểm `apiRequest` với
  `auth: false`/tùy chọn: khách thì không gửi Authorization, đã đăng nhập thì vẫn gửi). Thêm trang chi tiết từ public
  `/vocabulary/:id` (bám P-57) dùng `GET /api/content/vocabulary/{id}`.
- Khách: ẩn trạng thái "Đã lưu" (không gọi `/notebook/saved`); bấm "Lưu" → chuyển trang đăng nhập kèm `returnTo`, sau khi
  đăng nhập quay lại và tự lưu từ đó. Phần mức hiểu/ghi chú ở trang chi tiết chỉ hiện khi đã đăng nhập.
- `/vocabulary/notebook` và phiên ôn vẫn `RequireAuth`.
- Trạng thái chờ (skeleton), rỗng (không có kết quả cho "q"), lỗi + thử lại.

## Màn hình 1 — Sổ từ (`VocabularyNotebookPage`)

1. **Hero thống kê** (từ `GET /notebook/summary`): tổng số từ (count-up), vòng tròn tiến độ 4 mức (SVG `stroke-dashoffset`
   animate), chip "N từ đến hạn ôn" + CTA "Ôn ngay" (pulse nhẹ khi `dueNow > 0`), "+X từ tuần này", "Đã ôn hôm nay: Y".
2. **Thanh công cụ**: ô tìm (debounce 300ms, `AbortController`), chip lọc 4 mức (đếm từ summary), toggle "Đến hạn",
   sort (Mới lưu / Đến hạn / A–Z / Yếu nhất), toggle lưới/danh sách (nhớ bằng `localStorage` trong try/catch).
   Đồng bộ filter vào URL search params để chia sẻ/back được.
3. **Thêm từ nhanh**: combobox gọi `GET /api/content/vocabulary/search`, chọn sense → `POST /notebook/entries`;
   thẻ mới bay vào đầu danh sách (animation), toast "Đã có trong sổ" nếu 200 trùng.
4. **Lưới thẻ từ** (bám P-50): lemma, loại từ, IPA, nút phát âm (`pronunciationAudioReference` hoặc `speak.ts`),
   nghĩa, ví dụ (highlight lemma trong câu), badge mức hiểu có màu, "Ôn sau 2 ngày" (relative time từ `nextReviewAt`),
   ảnh nếu có. Hover: nâng thẻ; click: mở ngăn chi tiết. Phân trang bằng "Tải thêm" / infinite scroll (IntersectionObserver).
5. **Từ của ngày**: chọn tất định theo ngày từ các từ yếu nhất trong trang đầu (`sort=WEAKEST`), hiển thị thẻ nổi bật lật được.
6. Trạng thái: skeleton shimmer khi tải, rỗng (minh họa owl + CTA "Tra từ điển"), lỗi có nút thử lại; `ErrorBoundary`.

## Màn hình 2 — Ngăn chi tiết từ (Sheet/Drawer, bám P-57 cột phải)

- Thông tin đầy đủ, nút nghe UK/US (nếu chỉ có một audio thì một nút).
- **Mức độ hiểu**: 4 nút segmented (Chưa biết/Mới biết/Đang học/Đã nhớ) → `PUT .../level`, optimistic update + rollback khi lỗi.
- **Ghi chú của tôi** (≤300, bộ đếm, autosave debounce 800ms, trạng thái "Đã lưu ✓"): notes API với `sourceType=VOCABULARY_SENSE`.
- "Ôn từ này ngay" (mở review với 1 từ), "Xóa khỏi sổ" (ConfirmDialog có sẵn, undo toast 5s trước khi gọi DELETE).
- Điều hướng ←/→ giữa các từ trong danh sách hiện tại.

## Màn hình 3 — Phiên ôn (`NotebookReviewPage`)

Lấy thẻ từ `GET /flashcards/due` (hoặc `/notebook/entries?level=...` khi người dùng chọn ôn theo mức). Mỗi câu trả lời
→ `POST /flashcards/{id}/reviews {correct, requestId}`; `requestId` tạo một lần mỗi câu, retry giữ nguyên.

Chế độ (sinh ở client, nhiễu lấy từ chính sổ từ; cần ≥4 từ cho trắc nghiệm, thiếu thì tự rơi về flashcard):
- **Flashcard lật 3D** (tái dùng `FlashcardFace`): Space lật, `1` Quên / `2` Nhớ, vuốt trái/phải trên mobile (pointer events).
- **Trắc nghiệm nghĩa** (4 lựa chọn, phím 1–4).
- **Gõ từ** (cho nghĩa, gõ lemma; so khớp bỏ hoa thường/khoảng trắng; gợi ý chữ cái đầu sau 1 lần sai).
- **Nghe chép** (phát audio/TTS, gõ lại).
- **Điền vào câu** (ví dụ có lemma bị che `____`).
- **Học trộn**: xoay vòng chế độ theo mức (UNKNOWN → flashcard/trắc nghiệm, LEARNING+ → gõ/nghe).
- Hiệu ứng: đúng → `CorrectBurst` + rung nhẹ xanh; sai → shake + hiện đáp án; thanh tiến độ mượt; combo đếm chuỗi đúng.
- Kết thúc: màn tổng kết (tỉ lệ đúng count-up, từ cần ôn lại, từ lên mức), nút "Ôn lại từ sai".
- Không cộng XP/điểm ở FE (không có API); chỉ hiển thị thống kê.

## Tích hợp điểm lưu từ

- `LessonVocabulary.tsx` và trang từ điển: nút bookmark "Lưu vào sổ" → `POST /notebook/entries` (trạng thái đã lưu, animation bookmark).
  Chỉ sửa tối thiểu, không refactor component.

## Animation (CSS, không thêm dependency)

- Stagger fade/slide-up cho thẻ (`animation-delay: calc(var(--i) * 40ms)`), lật 3D, count-up (requestAnimationFrame),
  ring progress, shimmer skeleton, shake, pulse CTA, View Transitions API khi đổi filter (fallback không animation).
- Mọi animation tắt dưới `@media (prefers-reduced-motion: reduce)`. Chỉ animate `transform`/`opacity`.
- Dùng token/CSS biến hiện có, không hardcode màu.

## File (đề xuất)

```
src/features/vocabulary/
  api/notebookApi.ts            (+ notebookApi.test.ts)
  lib/understanding.ts          (level ↔ nhãn/màu, relative due)       (+ test)
  lib/quiz.ts                   (sinh câu hỏi, chọn nhiễu, so khớp)       (+ test)
  lib/word-of-day.ts                                                     (+ test)
  hooks/useNotebookEntries.ts, hooks/useNotebookSummary.ts
  components/NotebookHero.tsx, NotebookToolbar.tsx, WordCard.tsx, WordDetailSheet.tsx,
             QuickAddWord.tsx, ReviewModes/*.tsx, ReviewSummary.tsx
  pages/VocabularyNotebookPage.tsx, pages/NotebookReviewPage.tsx
  vocabulary-notebook.css
```

## Kiểm thử

- Vitest: `quiz.ts`, `understanding.ts`, `word-of-day.ts`, `notebookApi` (path/body đúng contract), trang sổ từ render
  rỗng/lỗi/có dữ liệu, phím tắt trong phiên ôn.
- `npm run lint && npm run typecheck && npm run build && npm test`.
- Chạy thật với BE (Gateway 8080) và kiểm tay mobile 375px + bàn phím.
