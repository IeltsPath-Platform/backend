# Prompt giao cho agent

Thứ tự: **BE-1 → BE-2 → BE-3** (cùng library-service và chuỗi migration, không chạy song song).
**FE-1** chạy sau khi BE-1 xong; **FE-2** chạy sau BE-3 (và sau FE-1, vì cùng feature folder).
Mỗi agent đọc báo cáo của agent trước trong `plans/reports/` (BE) để lấy contract cuối cùng.

Phần chung cho mọi prompt BE (đã chép vào từng prompt):
`graphify-out/graph.json` đã có → dùng `graphify explain "<Symbol>"` / `graphify affected "<Symbol>"` với symbol cụ thể
trước khi đọc source; câu query chung chung trả quá nhiều node.

---

## Prompt BE-1 — Sổ tay + từ điển công khai (phase 01)

```text
Bạn làm việc trong repo backend: d:\Fall2026\New folder\BE\backend, nhánh feat/vocabulary-notebook (không đổi nhánh).

Tiết kiệm token: graphify-out/graph.json đã có. Trước khi đọc source, dùng `graphify explain "Flashcard"`,
`graphify explain "FlashcardRepositoryAdapter"`, `graphify affected "Flashcard"`. Chỉ mở file mà graph trỏ tới.

Nhiệm vụ: làm phase 01 trong plans/261026-0855-vocabulary-notebook/ (đọc plan.md và phase-01-library-notebook-api.md
trước, rồi AGENTS.md, CLAUDE.md, services/library-service/README.md và mục "Đọc trước" của phase). Phase 03/04 do agent
khác làm sau — đừng làm trước, nhưng thiết kế để chúng mở rộng được (summary, entries chỉ thêm trường về sau).

Ràng buộc:
- Chỉ sửa library-service, docs/contracts/vocabulary-notebook-v1.md (tạo mới), README library; ngoại lệ là phần
  TỪ ĐIỂN CÔNG KHAI dưới đây. Không sửa common-security, Docker. Không dựng lại VocabularyReviewController/
  VocabularyProgress đã bị xóa ở commit ea515ba2.
- Mục sổ tay = flashcard source_type VOCABULARY_SENSE; mức hiểu suy ra từ leitner_box (1 UNKNOWN, 2 FAMILIAR,
  3 LEARNING, 4-5 MASTERED). Luật ở domain (UnderstandingLevel, Flashcard.setUnderstanding).
- Path/response cũ giữ nguyên; endpoint mới chỉ thêm. Ôn tập dùng POST /flashcards/{id}/reviews sẵn có.
- Migration V18. TRƯỚC khi thêm index unique, kiểm DB local có bản trùng (user, sense); có thì DỪNG và báo tôi.
- Danh sách: filter/sort/phân trang ở SQL, size ≤ 50, không N+1.
- Bỏ mock FE cần: `GET /api/content/vocabulary?q=&page=&size=` (chỉ ACTIVE, phân trang id ở SQL rồi nạp senses theo
  tập — không @EntityGraph + Pageable trên collection) và `GET /api/learning-support/flashcards/notebook/saved?senseIds=`
  (≤100 id). Không đổi /search, /senses. Dùng seed thật V4/V15, không thêm seed giả.
- TỪ ĐIỂN CÔNG KHAI (tôi đã duyệt): khách không đăng nhập được GET /api/content/vocabulary và
  GET /api/content/vocabulary/{id} (regex UUID để KHÔNG mở /search, /senses):
  * Gateway: KHÔNG thêm vào public-endpoints của api-gateway.yaml (list đó permitAll mọi method). Thêm
    pathMatchers(HttpMethod.GET, ...).permitAll() trong infra/api-gateway/.../security/SecurityConfig.java (hoặc mở rộng
    PublicEndpointProperties có method — chọn cách nhỏ hơn, để phase 03 thêm path dễ). Kiểm filter internal JWT với
    request không token.
  * Library: thêm mục method GET vào app.security.public-endpoints (config-repo/library-service.yaml); bỏ @PreAuthorize
    chỉ ở method duyệt và GET /{id}. /search, /senses, POST, /api/content/admin/** và mọi endpoint sổ tay vẫn cần đăng nhập.
  * Endpoint public không đọc user, không trả dữ liệu cá nhân, size ≤ 50, q ≤ 100.
  * Test: GET public không token → 200; POST /api/content/vocabulary, GET /search, /senses, admin/vocabulary/{id} không
    token → 401 (gateway và library). Chạy cả `mvn -q -pl infra/api-gateway test`.
- Summary một câu SQL aggregate; "hôm nay" bằng Clock + timezone IANA.
- Không log nội dung ghi chú. Không gọi LLM. Không cộng XP/điểm.
- Commit conventional, không nhắc AI/plan/phase trong commit, comment, tên test, migration.

Kiểm chứng: unit domain + use case, @WebMvcTest (401/400/404), Testcontainers cho migration và query.
`mvn -q -pl services/library-service -am test`; thiếu Docker thì nói rõ test nào skip. Sau đó `graphify update .`.

Báo cáo: plans/reports/be-261026-vocabulary-notebook-phase-01-report.md — endpoint cuối cùng (path, query, body,
response, mã lỗi), số migration đã dùng, kết quả test thật, lệch so với phase và lý do, câu hỏi mở. Dòng cuối:
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```

---

## Prompt BE-2 — Làm giàu catalog theo Figma (phase 03)

```text
Bạn làm việc trong repo backend: d:\Fall2026\New folder\BE\backend, nhánh feat/vocabulary-notebook.

Tiết kiệm token: dùng `graphify explain "VocabularyController"`, `graphify explain "VocabularyRepositoryAdapter"`,
`graphify affected "VocabularySense"` trước khi đọc source.

Nhiệm vụ: làm phase 03 — plans/261026-0855-vocabulary-notebook/phase-03-library-catalog-enrichment.md. Đọc thêm plan.md,
báo cáo plans/reports/be-261026-vocabulary-notebook-phase-01-report.md (contract cuối của phase 01), AGENTS.md.
Mục tiêu: đủ dữ liệu cho mọi khối Figma của trang Từ điển và chi tiết từ: CEFR, band (suy từ CEFR), chủ đề, IPA/audio
UK-US, tần suất, Academic Word List, word family, Collocations, Thesaurus, Common Mistakes, Examples có dịch, bộ từ
theo band/chủ đề, gợi ý tìm kiếm, tìm Việt–Anh, mindmap; API admin cho CONTENT_AUTHOR/ADMIN; seed demo.

Ràng buộc:
- Migration V19 (schema, chỉ thêm cột/bảng, không xóa cột cũ) và V20 (seed). Số đã dùng thì lấy số kế tiếp.
- Band suy ra từ CEFR trong domain (A1 1.0–2.5, A2 3.0–3.5, B1 4.0–5.0, B2 5.5–6.5, C1 7.0–8.0, C2 8.5–9.0).
- Chủ đề từ vựng là bảng của library (vocabulary_topics), không gọi content-service; ghi rõ trong contract và
  docs/system-architecture.md.
- KHÔNG bịa dữ kiện đo được: frequency_rank để null (không có nguồn COCA); academic_word_list chỉ true cho từ thuộc
  danh sách AWL Coxhead. Seed CEFR/chủ đề/collocation/ví dụ/lỗi thường gặp chỉ cho từ đã có ở V4/V15, comment đầu file:
  "Demo content authored by AI; requires review by an IELTS examiner before production." Ghi tương tự trong README.
- GET /api/content/vocabulary/{id}: chỉ THÊM trường, không đổi/bỏ trường cũ (content-service và lesson đang dùng).
  Bảng con nạp theo tập sense id, mỗi bảng một query; không N+1.
- Các GET public mới (suggestions, topics, sets, sets/{id}, sets/{id}/words, mindmap) thêm vào cả Gateway và library theo
  cách phase 01 đã làm, chỉ method GET. Admin dưới /api/content/admin/vocabulary, @PreAuthorize CONTENT_AUTHOR/ADMIN.
- Ảnh bìa topic/set dùng common-storage như ảnh flashcard (không thêm bucket/biến env mới nếu bucket hiện có dùng được;
  nếu cần biến mới thì báo tôi, không tự sửa Docker).
- Giới hạn: size ≤ 50, mindmap ≤ 80 node, suggestions ≤ 8, danh sách con admin ≤ 30 mục, set ≤ 500 sense.
- Không log nội dung, không gọi LLM. Commit conventional, không nhắc AI/plan/phase.

Kiểm chứng: như mục Kiểm thử của phase 03; `mvn -q -pl services/library-service -am test` và
`mvn -q -pl infra/api-gateway test`; nói rõ test bị skip. `graphify update .`. Cập nhật
docs/contracts/vocabulary-notebook-v1.md (hoặc tạo vocabulary-catalog-v1.md nếu tách gọn hơn).

Báo cáo: plans/reports/be-261026-vocabulary-notebook-phase-03-report.md (endpoint cuối, schema, migration, kết quả test,
lệch và lý do, câu hỏi mở). Dòng cuối: Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```

---

## Prompt BE-3 — Tiến độ học viên (phase 04)

```text
Bạn làm việc trong repo backend: d:\Fall2026\New folder\BE\backend, nhánh feat/vocabulary-notebook.

Tiết kiệm token: dùng `graphify explain "ReviewFlashcardUseCase"`, `graphify affected "ReviewFlashcardUseCase"` trước khi đọc source.

Nhiệm vụ: làm phase 04 — plans/261026-0855-vocabulary-notebook/phase-04-library-learner-progress.md. Đọc plan.md và hai
báo cáo phase-01/phase-03 trong plans/reports để lấy contract cuối. Cần có: log ôn (flashcard_review_events, có sessionId
tùy chọn), mục tiêu tuần (GET/PUT), bản đồ từ vựng 6 band, tiến độ chủ đề và bộ từ, chủ đề đang học, kết quả gần nhất,
thành tích (tính khi đọc), summary mở rộng (learnedCount, catalogTotal…), filter topicId/setId cho entries.

Ràng buộc:
- Migration V21 (hoặc số kế tiếp). Event ôn ghi cùng transaction với ReviewFlashcardUseCase, chỉ khi review thật sự áp
  dụng (request lặp theo requestId không ghi thêm). sessionId là trường TÙY CHỌN mới — client cũ vẫn chạy.
- Định nghĩa đã học / tuần / ngày đạt / thành tích nằm ở domain, có unit test; tuần Thứ Hai–Chủ Nhật theo timezone IANA.
- Mặc định mục tiêu tuần `library.vocabulary.weekly-goal-default: 120` trong config-repo/library-service.yaml.
- Mọi đếm bằng SQL aggregate, không N+1, không lặp gọi repository; không lộ dữ liệu user khác (test nhiều user).
- Mọi endpoint cần đăng nhập, danh tính từ CurrentUserProvider. Library KHÔNG gọi learning-service: trình độ hiện tại
  do FE đọc từ /api/learning/report. Không cộng XP/điểm.
- Response cũ chỉ được thêm trường. Commit conventional, không nhắc AI/plan/phase.

Kiểm chứng: mục Kiểm thử của phase 04; `mvn -q -pl services/library-service -am test`; nói rõ test skip;
`graphify update .`; cập nhật contract và README library.

Báo cáo: plans/reports/be-261026-vocabulary-notebook-phase-04-report.md. Dòng cuối:
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```

---

## Prompt FE-1 — Từ điển thật, chi tiết từ, Sổ tay, phiên ôn

```text
Bạn làm việc trong repo frontend: d:\Fall2026\New folder\FE\frontend, nhánh feat/vocabulary-notebook.

Đọc: d:\Fall2026\New folder\BE\backend\plans\261026-0855-vocabulary-notebook\plan.md và phase-02-fe-notebook-screens.md;
contract cuối ở d:\Fall2026\New folder\BE\backend\docs\contracts\vocabulary-notebook-v1.md và các báo cáo
be-261026-vocabulary-notebook-phase-0*-report.md trong BE plans/reports (thắng phase 01/03/04 nếu lệch). Đọc AGENTS.md,
README.md, .sdd/ của FE và mục "Đọc trước" của phase 02.

Thiết kế: Figma là nguồn chính — file MuSfpqT61Xk6vgtvqDJEH6, theo bảng "Đối chiếu Figma → màn hình" trong phase 02.
Gọi create_session, rồi get_design_context/get_screenshot theo TỪNG node, get_variable_defs cho màu/chữ; KHÔNG
get_metadata cả trang 1:3 (quá lớn). Không truy cập được Figma thì nói rõ và dùng wireframe
d:\Fall2026\New folder\docs\wireframes\exports\P-50, P-57, P-59*.

Phạm vi FE-1 (dùng API phase 01; khối cần API phase 03/04 mà BE chưa xong thì viết component nhận dữ liệu thật,
hiện trạng thái rỗng/ẩn khi endpoint chưa có — không mock):
0. BỎ MOCK — /vocabulary (VocabularyPage.tsx) đang render LESSON_VOCABULARY từ src/mocks/lessonData.ts. Chuyển sang
   GET /api/content/vocabulary (tìm ở server, debounce, tải thêm), "Đã lưu" qua /notebook/saved, Lưu qua
   POST /notebook/entries. Không fallback về dữ liệu cứng khi lỗi (hiện lỗi + thử lại). Xóa lessonData.ts nếu grep xác
   nhận không còn nơi import.
1. TỪ ĐIỂN CÔNG KHAI: /vocabulary và /vocabulary/:id KHÔNG bọc RequireAuth; khách không gửi Authorization (kiểm option
   apiRequest). Khách: khối cá nhân thay bằng CTA đăng nhập; bấm "Lưu vào sổ tay" → đăng nhập kèm returnTo rồi tự lưu.
2. Trang /vocabulary theo node 6038:19017 (đầy đủ các khối trong bảng phase 02 mà API đã có).
3. Trang chi tiết /vocabulary/:id theo node 3967:17116: tab Definitions/Collocations/Thesaurus/Common Mistakes/Examples
   có số đếm, IPA UK/US, Thông tin nhanh, Mức độ hiểu (optimistic + rollback), Ghi chú ≤300 autosave, "Lưu và học từ
   này", "Ôn từ này sau". Trường null thì ẩn dòng đó.
4. Trang Sổ tay /vocabulary/notebook (RequireAuth), ngôn ngữ thiết kế của node 3042:16743: hero thống kê (count-up, 3 thẻ
   Đã thành thạo/Đang học/Cần ôn, "Ôn từ cần nhớ"), tìm, lọc mức, sort, lưới/danh sách, thêm nhanh, "Từ của ngày",
   bảng "Từ vựng gần đây", ngăn chi tiết (xóa có undo, ←/→).
5. Phiên ôn /vocabulary/notebook/review theo node 3959:17478: Đoán nghĩa (màn đúng/sai), Luyện nhớ (flashcard 3D),
   Kiểm tra (gõ từ), Nghe chép, Điền vào câu, Đặt câu (tự viết + câu mẫu + tự đánh giá, không LLM), học trộn; phím tắt
   (Space, 1-4, Enter), vuốt mobile, hộp thoại hoàn thành/skip theo Figma, màn tổng kết + "Ôn lại từ sai".
   Review: POST /api/learning-support/flashcards/{id}/reviews {correct, requestId, sessionId}; requestId giữ nguyên khi retry.
6. /vocabulary/review gọi API đã bị xóa: redirect tới /vocabulary/notebook/review, bỏ code chết. Nút "Lưu vào sổ tay"
   ở LessonVocabulary (sửa tối thiểu). Navbar "Kho từ vựng" → "Từ điển".

Animation: chỉ CSS + tw-animate-css (stagger, flip 3D, shake/burst, shimmer, count-up, ring/bar progress, View
Transitions có fallback); tắt dưới prefers-reduced-motion; chỉ transform/opacity.

Con cú (hero Sổ tay, bộ từ, phiên ôn, màn đúng/sai/hoàn thành): DÙNG LẠI asset và component có sẵn, không thêm ảnh mới:
src/components/OwlHero.tsx (prop `pose` hoặc `poses` để xoay vòng, tự đứng yên khi reduced-motion), pose trong
src/components/owlPoses.ts (idea, cheer, party, confused, confident, cool, wave, tired, desk, laptop; file ở
src/assets/owl/hero-*.webp). Gợi ý: hero Sổ tay/bộ từ `desk` hoặc `confident`; trả lời đúng `cheer`, sai `confused`,
hoàn thành phiên `party`, chưa có từ (empty) `idea`, hết thời gian/nghỉ `tired`. Phản ứng trong phiên ôn có thể tái dùng
OwlCoach + lib/owl-reactions.ts của features/flashcards. Nếu Figma dùng tư thế không có trong danh sách, chọn tư thế gần
nhất và ghi trong báo cáo — không tự export/thêm asset.

Ràng buộc: không thêm dependency, không sửa package.json/vite/tsconfig/eslint; shadcn trong src/components/ui,
lucide-react; mobile-first, bàn phím, WCAG AA, ErrorBoundary, trạng thái chờ/lỗi/rỗng; không mock trong code production;
không hardcode màu (map token Figma vào biến CSS); localStorage chỉ cho tùy chọn hiển thị/lịch sử tìm, bọc try/catch;
không cộng XP/điểm. FE chưa có graphify-out: đọc file có chủ đích, không quét cả repo.

Kiểm chứng: Vitest cho lib (quiz, understanding, word-of-day), api client, trang chính; `npm run lint && npm run
typecheck && npm run build && npm test`; báo kết quả thật. BE chạy được (Gateway 8080) thì thử luồng thật ở 375px và
desktop, cả khi chưa đăng nhập.

Báo cáo: plans/reports/fe-261026-vocabulary-notebook-part-1-report.md trong repo FE (màn đã làm, file chính, kết quả
kiểm tra, lệch so với Figma, phần chờ API, câu hỏi mở). Dòng cuối: Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```

---

## Prompt FE-2 — Bộ từ, bản đồ, mindmap, tiến độ, admin

```text
Bạn làm việc trong repo frontend: d:\Fall2026\New folder\FE\frontend, nhánh feat/vocabulary-notebook.

Đọc như FE-1 (plan.md, phase-02, contract và báo cáo BE phase 01/03/04) cộng báo cáo
plans/reports/fe-261026-vocabulary-notebook-part-1-report.md của repo FE. Tái dùng component/hook/api của FE-1, không
viết lại.

Phạm vi FE-2 (Figma file MuSfpqT61Xk6vgtvqDJEH6, đọc từng node như FE-1):
1. Hoàn tất các khối của /vocabulary (node 6038:19017) còn chờ API: Trình độ hiện tại (CEFR từ estimatedBand của
   GET /api/learning/report; "Test lại" → /placement nếu chưa làm, đã làm thì → báo cáo học tập — placement chỉ một lần),
   Chủ đề đang học, Mục tiêu tuần T2–CN (sửa mục tiêu), Khám phá bộ từ vựng 4 tab (Theo band điểm, Theo chủ đề, Sổ tay,
   Mindmap), Bản đồ từ vựng của bạn (6 cột chồng 4 màu, nhãn Trình độ hiện tại / Cần bổ sung).
2. /vocabulary/sets/:id (node 3042:16743, public; tiến độ khi đăng nhập): hero bộ + owl + 3 thẻ số, tab Tổng quan/Chủ đề/
   Tất cả từ vựng/Tiến độ, Tiếp tục lộ trình, thẻ chủ đề có % và nhãn, bảng Từ vựng gần đây, Lưu bộ từ, cột phải Mục tiêu
   tuần, Kết quả gần nhất (vòng %), Cách học đề xuất, Thành tích bộ từ, thanh "Sẵn sàng học tiếp?".
   /vocabulary/sets/:id/study (RequireAuth) tái dùng phiên ôn FE-1, lộ trình theo node 3959:17824/3959:18608.
3. /vocabulary/map (node 3042:15953, RequireAuth): bản đồ chi tiết theo band.
4. Mindmap: dùng @xyflow/react đã có (tham khảo features/admin/knowledge-graph), node chủ đề → từ → collocation/
   synonym, click từ mở /vocabulary/:id, zoom/pan, bàn phím được, tối đa số node API trả.
5. Admin /admin/vocabulary (CONTENT_AUTHOR/ADMIN, theo pattern features/admin/*): sửa từ (IPA UK/US, AWL, word family),
   nghĩa (CEFR, chủ đề, ghi chú, dịch ví dụ), danh sách collocation/thesaurus/lỗi thường gặp/ví dụ, CRUD chủ đề và bộ từ,
   sắp thứ tự từ trong bộ. Form react-hook-form + zod (đã có trong dependency).

Animation, ràng buộc, kiểm chứng: giống FE-1. Thêm Vitest cho tính toán bản đồ/% chủ đề/mục tiêu tuần ở FE (nếu có),
mindmap layout, form admin validation.

Báo cáo: plans/reports/fe-261026-vocabulary-notebook-part-2-report.md. Dòng cuối:
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```
