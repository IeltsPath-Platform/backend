# Review: plan MVP đã đủ để giao Codex code chưa

- Ngày: 2026-10-01. Phạm vi: roadmap `260930-2057` + 1640, 0737, 0812, 0851, 1006. Chỉ đánh giá, không sửa plan.
- Tiêu chí "đủ": agent không có ngữ cảnh hội thoại, không có skill `/ck:*`, không mở được artifact claude.ai, đọc plan là code được mà không phải tự ra quyết định thiết kế.

## Kết luận

| Plan | Sẵn sàng? | Lý do chính |
| --- | --- | --- |
| 1640 nền | **Gần đủ**, thiếu 3 chỗ (B1–B3) | Đã validate, có file/dòng, test, thứ tự. Thiếu bảng seed, luật khi KP không có gói luyện |
| 0737 Writing T2 | **Chưa** | Chưa nhận quyết định Validation Session 1 của roadmap (refund, bằng chứng Writing); 3 điểm còn "đề xuất" |
| 0812 Writing T1 | **Chưa** | Chưa validate lần nào; chưa có quy ước hàm kiểm media |
| 0851 Listening | **Chưa** | Plan tự ghi "cần validate lại"; còn gói theo mức, `V<n>`, thiếu `kind` KP, thiếu dạng `section_snapshot` |
| 1006 Gợi ý | **Chưa** | Còn toàn bộ phần `hints_used` (đã chốt bỏ); `V<n>` |

Roadmap phase 1 (đồng bộ tài liệu, số migration) và phase 2 (validate lại 4 plan con) **chưa làm**. Đây là điều kiện trước khi giao Codex bất kỳ plan con nào.

## Lỗ trong 1640 (sửa trước khi giao)

- **B1 — Seed chỉ có trong artifact demo.** Phase 3 ghi "nội dung lấy từ demo tương tác" (claude.ai, private). Repo chỉ có đoạn *Green roofs* (V6, 0 câu hỏi). Không có bảng bài → khối → câu → KP → đáp án; kịch bản Lan (sai Q5 ở L2, sai Q11 ở L1, sai Q15 ở đề) và test `review_rule` phụ thuộc bảng này. 1006 cũng tham chiếu Q1…QT1. Cần bảng seed trong plan (hoặc file `seed-map.md`).
- **B2 — KP không có gói luyện.** Seed V8: gói luyện chỉ cho KP1–KP4; KP5 (TFNG) không có. Sai câu KP5 ở đề TFNG + mastery < 0.6 → chèn review KP5 → `GET /reviews` không có gói nào để giao, "lấy gói lâu nhất" cũng không có. Hành vi chưa định nghĩa. Writing/Listening KP cùng rủi ro. Cần luật: không chèn review nếu KP không có gói, hoặc review chỉ lý thuyết rồi DONE/SKIPPED.
- **B3 — `kind` của KP.** `knowledge_points.kind` NOT NULL + CHECK (`V1:23`). Seed KP2–KP5 (1640), KP Writing (0737, 0812) không ghi `kind`. Codex sẽ gặp lỗi migration rồi tự chọn. Ghi sẵn (đề xuất `STRATEGY` cho Reading/Listening, `STRATEGY` hoặc `GRAMMAR` cho Writing).

## Lỗ trong plan con (xử lý ở roadmap phase 2)

- 0737: phase 1 vẫn "đổi cả 4 endpoint" gồm refund sang `/internal/access` (ngược quyết định "refund không mở cho service"); mã `ESSAY_BLOCK` có ở text nhưng không có trong bảng lỗi (bảng chỉ có `NOT_ESSAY_BLOCK`); debit song song cùng key → 500; `temperature` bị bỏ khi `REASONING_EFFORT=low`; `GetUserEntitlementUseCase` không có command; router phải tự lấy bearer (`AuthenticatedUser` không giữ token). Điều kiện "nếu V11 giới hạn index" đã biết là không, nên bỏ.
- 0812: không Validation Log; hàm kiểm media dùng chung và tên `mediaReference`/`mediaUrl` chưa chốt.
- 0851: `phase-02:42,56,62,83,106` và `plan.md:81` còn gói theo mức, `difficulty`, `V<n>`; 4 KP thiếu `kind`; dạng `section_snapshot` của assessment chưa được 1640 phase 4 định nghĩa nên chỗ thêm `audio`/`solution` chưa rõ.
- 1006: `phase-02` (tên phase, bước 5, test), `phase-03 §7.5`, `plan.md:35,69` còn `hints_used`; `phase-01:49,94` còn `V<n>`.

## Rủi ro khi giao Codex (không phải lỗ plan)

- Roadmap phase 3–7 ghi `/ck:cook …`: Codex không có skill này. Cần prompt giao việc riêng cho mỗi PR.
- Hai điểm dừng bắt buộc (duyệt contract cuối 1640 phase 1; duyệt V10 phá hủy dữ liệu ở 1640 phase 5) phải nằm trong prompt; nếu không, agent chạy qua luôn.
- Số dòng (`file.py:80-93`) sẽ lệch sau PR trước; ghi trong prompt là "gợi ý vị trí, tìm theo tên symbol".
- Khối lượng: 1640 ước 12–15 ngày công, 8 phase. Giao theo từng PR (2, 3, 5a, 5b, 6a, 6b, 4, 7, 8), không giao cả plan một lần.
- Test Postgres/RabbitMQ cần biến môi trường (`AI_LEARNING_TEST_DATABASE_URL`, `…_AMQP_URL`) và Docker cho Testcontainers; thiếu thì test skip và "pass giả".

## Trạng thái sau khi xử lý (2026-10-01)

- 1640: B1 (`seed-content.md`), B2 (`hasPracticeSet` + `has_practice_set`, điều kiện 4 của luật chèn), B3 (`kind = STRATEGY`)
  đã vào plan (Validation Session 3). Thêm: dạng `section_snapshot` ở phase 4. **Sẵn sàng giao agent code** theo
  `plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md`.
- Roadmap phase 1 và 2: xong. 0737 (Session 2), 0812 (Session 1), 0851 (Session 1), 1006 (Session 3) đã validate lại.
- Chưa có: prompt giao việc cho 4 plan con (viết khi 1640 gần merge, vì phụ thuộc tên file và hàm 1640 tạo ra).

## Việc cần làm theo thứ tự (lúc review)

1. Sửa 1640: B1 (bảng seed), B2 (luật KP không có gói), B3 (`kind`).
2. Roadmap phase 1 (tài liệu, số migration, frontmatter).
3. Roadmap phase 2: validate lại 0737 → 0812 → 0851 → 1006.
4. Viết prompt giao Codex cho từng PR của 1640 (phạm vi file, điểm dừng, lệnh test, biến môi trường).

## Quyết định của người dùng (2026-10-01)

| # | Chủ đề | Quyết định | Áp vào |
| --- | --- | --- | --- |
| 1 | KP không có gói luyện | **Không chèn bài ôn**: trước khi chèn, hỏi content có gói luyện cho KP; không có thì bỏ qua. Đề cuối vẫn kiểm KP | 1640 phase 6 (`reevaluate_reviews`), contract, test |
| 2 | Nguồn seed 1640 | **Chép từ demo vào plan**: bảng bài → khối → câu → KP → đáp án → câu Lan sai, trong 1640 phase 3 | 1640 phase 3 |
| 3 | `kind` của KP mới | **Tất cả `STRATEGY`** (Reading, Listening, Writing) | 1640 phase 3, 0737 phase 2, 0812 phase 1, 0851 phase 2 |
| 4 | Giao Codex | **Mỗi PR một prompt**: phạm vi file, điểm dừng duyệt, lệnh test, biến môi trường | Viết sau khi plan xong |
| 5 | Refund access | **Giữ route, chỉ `ADMIN`** (`@PreAuthorize`); không chuyển sang `/internal/access` | 0737 phase 1 |
| 6 | Temperature khi chấm Writing | **Chấp nhận như hiện tại**: không gửi khi có reasoning effort; band là ước lượng | 0737 phase 3 |
| 7 | Seed Listening | **1 gói luyện mỗi KP**: 4 gói, tổng 8 mp3 (2 bài, 4 gói, 2 đề) | 0851 phase 2, `plan.md` |
| 8 | Trường link cho học viên | **`mediaUrl`** cho ảnh và audio; nội bộ giữ `mediaReference` | 0812, 0851, roadmap phase 1 |
| — | Debit song song cùng key (tự chốt, cách xử lý rõ) | Bắt UNIQUE → trả giao dịch cũ; replay khác `amount`/`referenceId` → 409 | 0737 phase 1 |

## Câu hỏi mở

- Không còn cho lượt review này. Các điểm còn lại của plan con (`GetUserEntitlementUseCase` không có command, bearer cho access, dạng `section_snapshot`) là việc xác minh code khi validate, không phải quyết định.
