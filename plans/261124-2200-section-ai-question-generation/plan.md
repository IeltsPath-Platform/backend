# Sinh câu hỏi bằng AI từ nội dung của phần

- Status: đã triển khai các phase; tiêu chí nghiệm thu chưa hoàn tất (xem reports/codex-implementation-report.md)
- Cập nhật: 2026-11-25 (đọc lại code AI draft; thêm chống trùng và bảng trường hợp ngoại lệ)
- Repo: `backend/` (content-service, config-repo của api-gateway) và `frontend/`

## Mục tiêu

Trong bản nháp của đề, phần Listening hoặc Reading có nút "Sinh câu hỏi bằng AI". AI chỉ đọc transcript hoặc đoạn văn của
chính phần đó cùng các câu đã có trong phần. Người soạn xem, sửa, chọn câu rồi mới đưa vào phần ở trạng thái câu nháp.

## Hiện trạng đã kiểm trong code (2026-11-25)

Các điểm dưới đây quyết định thiết kế. Dòng có ⚠ là lỗ hổng sẵn có, phải sửa trước khi thêm tính năng.

1. ⚠ **Xuất bản đề không kiểm câu đã xuất bản.** `PublishContentPackageUseCase.validate` chỉ kiểm câu dùng nơi khác, mục
   đích và đáp án; `packageVersionContent` cũng không lọc `qv.status`. Gắn câu nháp vào đề rồi xuất bản đề là câu nháp tới
   học viên, trái `AGENTS.md` §3.6. Hiện chưa lộ vì ô chọn câu chỉ liệt kê câu đã xuất bản.
2. ⚠ **Lưu bản nháp AI là upsert không điều kiện** (`JdbcAiDraftRepository.save`). Worker bị coi là treo và worker thay thế
   có thể cùng ghi: FAILED đè READY/APPROVED, hoàn lượt hai lần, và bản nháp đã xóa được chèn lại.
3. ⚠ **`claimQueued(2)` gắn `started_at` cho cả hai bản nháp nhưng xử lý lần lượt.** Bản thứ hai có thể bị coi là treo khi
   chưa hề chạy. Không có giới hạn số lần đưa lại hàng chờ.
4. ⚠ **Duyệt và sửa câu dùng `findById` không khóa.** Hai tab sửa cùng lúc ghi đè nhau; xóa một câu làm các chỉ số sau dồn
   lên nên tab kia sửa nhầm câu. Duyệt đôi chỉ được chặn gián tiếp nhờ `importRef` duy nhất.
5. `QuestionDraftValidator.supports` gồm bốn dạng: trắc nghiệm, True/False/Not Given, điền từ, trả lời ngắn (không phải hai).
   Validator chung chỉ kiểm khuôn và trùng stem **giống hệt**; không kiểm mã TFNG, không kiểm đáp án có trong nguồn.
6. `ApproveQuestionDraftUseCase` tạo câu `purpose = LEARNING`, `skill = null` và bắt buộc có KP, nên không dùng lại được nguyên trạng.
7. Đề `PRACTICE_TEST` chỉ xuất bản được khi **mọi câu thuộc một nhóm** (`section_question_groups`), mà chưa có API soạn nhóm
   câu. Thêm câu bằng AI (hay thêm tay) vào loại đề này đều làm đề không xuất bản được.
8. Gateway chỉ giới hạn tier `ai` cho đúng path `/ai-drafts/practice-sets`; path mới phải được thêm.
9. Frontend parse danh sách bản nháp bằng `z.enum(kind)` nghiêm: kind mới làm hỏng parse nếu backend lên trước frontend.

## Quyết định thiết kế

| # | Quyết định | Lý do |
| --- | --- | --- |
| D1 | `DraftKind.SECTION_QUESTIONS` riêng, generator và bước duyệt riêng | Không làm đổi hành vi bản nháp theo KP |
| D2 | Server đọc nguồn và **chụp lại** vào yêu cầu (văn bản + SHA-256); client không gửi văn bản | Sinh, kiểm và duyệt trên cùng một nguồn; phát hiện được nguồn đã đổi |
| D3 | Mỗi người soạn tối đa **một lượt chưa đóng** cho một phần (`QUEUED/GENERATING/READY`), chặn bằng unique index | Chống bấm đôi, hai tab, hai instance; không bị bản nháp bỏ quên của người khác khóa phần |
| D4 | Câu trong bản nháp có `itemId` ổn định, API mới dùng `itemId` thay vì chỉ số | Hai tab không sửa nhầm câu |
| D5 | Duyệt: khóa gói → khóa bản nháp → kiểm lại toàn bộ → tạo câu và gắn vào phần, một transaction | Tất cả hoặc không câu nào |
| D6 | Xuất bản đề **từ chối câu chưa xuất bản** (`QUESTION_NOT_PUBLISHED`), và có nút xuất bản loạt câu nháp của phần | Bịt lỗ hổng 1 mà không bắt bấm xuất bản từng câu |
| D7 | KP không bắt buộc; độ khó mặc định `MEDIUM`, không hiện trên giao diện; điểm 1 | Theo quyết định đã chốt với chủ dự án |
| D8 | V1 hỗ trợ `MOCK_TEST`, `PLACEMENT_TEST`, `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST`; không hỗ trợ `PRACTICE_TEST`, `QUIZ`, `LESSON` | Điểm 7 ở trên; hai loại sau không có mục đích câu xác định |
| D9 | Thiếu transcript/đoạn văn: nút tắt; server cũng từ chối | Chống lạc đề; AI không nghe được mp3 |

## Luồng

```text
Bấm nút -> POST section-questions (khóa gói, chụp nguồn, trừ 1 lượt) -> QUEUED
        -> job: claim 1 bản -> gọi LLM ngoài transaction -> kiểm khuôn + nguồn + trùng -> READY | FAILED (hoàn lượt)
        -> modal duyệt: xem / sửa / bỏ / chọn -> approve -> câu DRAFT + gắn vào phần
        -> xuất bản loạt câu nháp -> xuất bản đề (chặn nếu còn câu nháp)
```

## Phase

| # | Phase | Phụ thuộc | Trạng thái |
| --- | --- | --- | --- |
| 01 | [Bịt lỗ hổng sẵn có](phase-01-harden-existing.md): chặn xuất bản đề có câu nháp, lưu có điều kiện, job an toàn | không | Đã triển khai; còn thiếu test một số tình huống cạnh tranh |
| 02 | [Yêu cầu và sinh câu](phase-02-request-and-generate.md) | 01 | Đã triển khai; còn thiếu một số test biên và kiểm tra LLM thật |
| 03 | [Duyệt và gắn vào phần](phase-03-review-and-attach.md) | 02 | Đã triển khai; còn thiếu test riêng theo ma trận E |
| 04 | [Giao diện](phase-04-frontend.md) | 03 | Đã triển khai; Vitest admin còn 5 lỗi ở khu vực khác, chưa kiểm tay |
| 05 | [Tài liệu và kiểm tra cuối](phase-05-docs-and-verification.md) | 04 | Tài liệu và tự động hóa đã cập nhật; chưa đạt nghiệm thu do test/kiểm tay còn thiếu |

Phase 01 có giá trị độc lập và nên lên trước dù chưa làm phần còn lại.

## Bảng trường hợp ngoại lệ

Mỗi dòng phải có ít nhất một test; tên test ghi hành vi, không ghi mã E.

| Mã | Trường hợp | Xử lý | Phase |
| --- | --- | --- | --- |
| **Nguồn** | | | |
| E1 | Không có transcript/đoạn văn (hoặc chỉ khoảng trắng) | `422 SECTION_SOURCE_REQUIRED`, không trừ lượt, nút tắt | 02, 04 |
| E2 | Nguồn dưới 80 từ | `422 SECTION_SOURCE_TOO_SHORT` | 02 |
| E3 | Nguồn trên 20.000 ký tự (đoạn văn cho phép tới 50.000) | `422 SECTION_SOURCE_TOO_LONG`, không cắt ngầm | 02 |
| E4 | Nguồn đổi hoặc bị gỡ khi đang sinh hay chờ duyệt | Sinh trên bản chụp; `sourceChanged = true`; duyệt trả `409 SECTION_SOURCE_CHANGED` trừ khi xác nhận rõ | 02, 03 |
| E5 | Nguồn, ghi chú hoặc câu cũ chứa lệnh cho mô hình | Đưa vào prompt dưới dạng chuỗi JSON trong khối dữ liệu; đầu ra vẫn qua mọi kiểm tra | 02 |
| E6 | Phần có cả đoạn văn lẫn audio | Listening chỉ lấy transcript, Reading chỉ lấy đoạn văn | 02 |
| **Yêu cầu** | | | |
| E7 | Bấm đôi, hai tab, gửi lại vì mất mạng | Unique index; `409 SECTION_DRAFT_OPEN` kèm `draftId`, giao diện mở lại đúng lượt đó; chỉ trừ một lượt | 02, 04 |
| E8 | Hết hạn mức ngày | `429 AI_QUOTA_EXCEEDED`, không tạo bản nháp; giao diện hiện số lượt còn lại | 02, 04 |
| E9 | Số câu ngoài 1..10, dạng câu không hợp kỹ năng, ghi chú quá 500 ký tự | `400`, lỗi đỏ dưới ô | 02, 04 |
| E10 | Phần Writing/Speaking/chưa có kỹ năng, loại đề không hỗ trợ | `400 SECTION_NOT_SUPPORTED`; không hiện nút | 02, 04 |
| E11 | Bản đề không còn là nháp; phần hoặc gói không tồn tại; phần thuộc bản khác | `400` VersionNotDraft, hoặc `404` | 02 |
| E12 | Phần đã đủ hoặc vượt số câu thường dùng | Không chặn; chỉ gợi ý số câu còn thiếu | 04 |
| **Sinh** | | | |
| E13 | Chưa cấu hình LLM, timeout, 429/5xx, mất kết nối | `FAILED` với mã hiện có, hoàn đúng một lượt | 02 |
| E14 | Không phải JSON, JSON bị cắt cụt, thiếu `items` | `FAILED INVALID_REPLY`, hoàn lượt | 02 |
| E15 | Trả ít hơn yêu cầu, hoặc nhiều câu bị loại | `READY` với câu hợp lệ; hiện "yêu cầu N, dùng được M" và lý do loại | 02, 04 |
| E16 | Trả nhiều hơn yêu cầu | Lấy đủ N câu hợp lệ đầu tiên | 02 |
| E17 | Không câu nào dùng được | `FAILED NO_USABLE_ITEMS`, hoàn lượt | 02 |
| E18 | Trùng câu trong phần hoặc trong lượt | Loại `DUPLICATE_IN_SECTION` / `DUPLICATE_IN_DRAFT`, kèm số câu bị trùng | 02 |
| E19 | Đáp án điền từ không có trong nguồn (bịa) | Loại `ANSWER_NOT_IN_SOURCE` | 02 |
| E20 | Đáp án lộ trong câu hỏi; câu điền từ không có đúng một chỗ trống; đáp án quá dài | Loại `ANSWER_IN_STEM` / `BAD_BLANK` / `ANSWER_TOO_LONG` | 02 |
| E21 | TFNG sai bộ lựa chọn; trắc nghiệm có hai lựa chọn giống nhau | Loại `BAD_TFNG_OPTIONS` / `DUPLICATE_OPTIONS` | 02 |
| E22 | Chép nguyên văn đoạn dài của nguồn vào câu hỏi | Loại `COPIED_FROM_SOURCE` | 02 |
| E23 | Trắc nghiệm có hai đáp án cùng đúng; câu cùng ý khác chữ | Không kiểm được bằng thuật toán: người duyệt đối chiếu nguồn hiện cạnh câu | 04 |
| **Job, nhiều instance** | | | |
| E24 | Instance chết giữa chừng | Đưa lại hàng chờ; quá 3 lần thì `FAILED INTERNAL` và hoàn lượt một lần | 01 |
| E25 | Worker chậm bị thay thế, cả hai cùng xong | Lưu có điều kiện theo lần claim; kẻ đến sau không ghi và không hoàn lượt | 01 |
| E26 | Phần/gói bị xóa, bản đề được xuất bản khi đang sinh | Sinh vẫn chạy trên bản chụp; chặn ở bước duyệt | 02, 03 |
| **Duyệt** | | | |
| E27 | Bấm duyệt hai lần, hoặc mất phản hồi rồi bấm lại | Khóa dòng; cùng danh sách câu thì trả lại kết quả cũ, khác danh sách thì `409 DRAFT_ALREADY_REVIEWED` | 03 |
| E28 | Hai tab cùng sửa hoặc xóa | `itemId` + `revision`; lệch thì `409 DRAFT_CHANGED`, tải lại và giữ chữ đang gõ | 03, 04 |
| E29 | Sửa câu thành trùng, sai khuôn hoặc đáp án ngoài nguồn | Chạy lại cùng bộ kiểm tra; `422 INVALID_DRAFT_ITEM` kèm lý do | 03 |
| E30 | Phần đã có thêm câu sau lúc sinh nên câu chọn thành trùng | Kiểm lại khi duyệt; `409 DRAFT_ITEMS_DUPLICATED` kèm danh sách, không tạo câu nào | 03 |
| E31 | Phần đổi kỹ năng, bị xóa, hoặc bản đề không còn nháp | `409 SECTION_CHANGED` / `404` / `400`; không tạo câu nào | 03 |
| E32 | Tạo câu thứ k lỗi | Rollback toàn bộ; bản nháp vẫn `READY` | 03 |
| E33 | Đóng trình duyệt khi đang sinh hoặc đang duyệt | Mở lại phần thấy lượt đang dở; không mất dữ liệu đã lưu | 04 |
| E34 | Bỏ quên bản nháp | Quá 14 ngày chuyển `EXPIRED`, mở khóa phần | 03 |
| E35 | Người khác mở bản nháp | `404` như hiện có | 03 |
| E36 | Bỏ hết câu trong bản nháp | Không duyệt được; gợi ý từ chối lượt | 03, 04 |
| **Xuất bản** | | | |
| E37 | Đề còn câu nháp | `422 QUESTION_NOT_PUBLISHED`, kể cả xuất bản theo lịch | 01 |
| E38 | Xuất bản loạt, một câu không hợp lệ | Không câu nào được xuất bản; chỉ rõ câu lỗi | 03, 04 |
| **An toàn** | | | |
| E39 | Nội dung nguồn, prompt, phản hồi lọt vào log | Chỉ log id, số lượng và mã lý do; có test bắt log | 02 |
| E40 | Gọi dồn dập | Tier `ai` ở Gateway cho path mới | 02 |
| E41 | Bản cũ gặp kind/status mới khi triển khai | Frontend lên trước; chỉ chạy một phiên bản backend khi migration | 05 |

## Ngoài phạm vi

- So trùng theo nghĩa (embedding hoặc gọi LLM lần hai).
- Nghe file mp3, tự tạo transcript.
- Soạn nhóm câu cho `PRACTICE_TEST`; cần tính năng soạn nhóm riêng trước.
- Đổi quota: hiện 20 lượt/người/ngày, dùng chung với các loại bản nháp AI khác.

## Tiêu chí hoàn thành

- Mọi dòng E1..E41 có test hoặc, với E23 và E41, có bước kiểm tay ghi trong báo cáo.
- Không có đường nào để câu nháp tới học viên qua xuất bản đề (cả thủ công lẫn theo lịch).
- `mvn -q -pl services/content-service -am test` và test Gateway đạt; vitest, eslint, tsc khu admin đạt.
- Thử với LLM thật trên ít nhất một phần Reading và một phần Listening, ghi tỉ lệ câu bị loại theo từng lý do để chỉnh ngưỡng.

## Đã chốt với chủ dự án (2026-11-25)

1. **Chặn xuất bản mọi đề còn câu chưa xuất bản** (D6), không chỉ câu do AI viết. Trước khi bật, chạy bước 0 của phase 01; có đề
   cũ bị ảnh hưởng thì báo chủ dự án kèm danh sách rồi mới bật.
2. **Bản đầu không hỗ trợ `PRACTICE_TEST`** (D8), chờ chức năng tạo nhóm câu.
3. **Ngưỡng trùng 0,8 và 0,5**, để trong cấu hình, chỉnh sau khi thử với LLM thật ở phase 05.
