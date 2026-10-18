# Gói duyệt phase 3b: mã lỗi và gắn phương án sai

Dành cho: người hiểu IELTS duyệt nội dung (và người quyết định của dự án).
Trạng thái: nháp. Chưa có người duyệt. Đánh dấu từng dòng: **Giữ**, **Sửa** (ghi nội dung mới), hoặc **Bỏ**.

Nguồn: V34 (danh mục 25 mã), V35 (gắn `distractors` cho 19 câu). Số liệu đã kiểm trên DB sạch V1–V35.

---

## Quyết định cần làm trước (4 câu)

- [ ] **Q1. Thêm 3 mã mới?** Xem mục C. Nếu không thêm, 6 phương án sai tương ứng vẫn để trống.
  - [ ] Thêm cả 3
  - [ ] Thêm một phần (ghi rõ): ____
  - [ ] Không thêm
- [ ] **Q2. Severity** theo định nghĩa: 1 lỗi nhỏ, 2 làm mất điểm, 3 cản trở hiểu hoặc sai yêu cầu đề. Có đồng ý cách chấm này không?
  - [ ] Đồng ý
  - [ ] Đổi: ____
- [ ] **Q3. Gắn `distractors` vào phiên bản đã publish.** V35 thêm khóa vào `answer_spec` của 19 phiên bản đã publish, không đổi đáp án đúng nên không đổi kết quả các lần làm đã có. Đây là ngoại lệ so với nguyên tắc phiên bản bất biến. Cách thay thế là tạo phiên bản câu mới, nhưng sẽ đổi id phiên bản đang được gói và lần làm tham chiếu.
  - [ ] Chấp nhận cách hiện tại
  - [ ] Yêu cầu tạo phiên bản mới
- [ ] **Q4. Phạm vi.** Hiện chỉ gắn 19 trên 158 câu trắc nghiệm Reading/Listening, vì chỉ gắn khi giải thích của câu đã nêu rõ bẫy. Có cần mở rộng đợt này không?
  - [ ] Không, duyệt phạm vi hiện tại
  - [ ] Mở rộng (người duyệt tự đọc thêm câu, mình sẽ gắn sau khi có danh sách)

---

## A. Danh mục 25 mã (V34)

Mã: đúng quy tắc `^[A-Z][A-Z0-9_]*$`. KP: knowledge point liên kết (trống nếu không có KP phù hợp).

| # | Mã | Nhóm | Severity | KP liên kết | Gợi ý sửa (nháp, nói với học viên) | Quyết định |
|---|---|---|---|---|---|---|
| 1 | TFNG_NG_AS_FALSE | READING_STRATEGY | 2 | TFNG_FALSE_VS_NOT_GIVEN | Phân biệt "bài nói sai" (False) với "bài không nói tới" (Not Given) trước khi chọn. | [ ] |
| 2 | TFNG_FALSE_AS_NG | READING_STRATEGY | 2 | TFNG_FALSE_VS_NOT_GIVEN | Nếu đoạn văn có ý trái ngược với câu hỏi thì đáp án là False, không phải Not Given. | [ ] |
| 3 | PARAPHRASE_TRAP | READING_STRATEGY | 2 | R65_PARAPHRASE | Đọc lại đoạn văn gốc và kiểm tra ý có giữ nguyên nghĩa không, đừng chỉ dựa vào từ khóa. | [ ] |
| 4 | KEYWORD_MATCH_ONLY | READING_STRATEGY | 1 | TR_R_SCANNING | Trùng từ khóa chưa đủ. Kiểm tra cả câu có đúng ý câu hỏi không. | [ ] |
| 5 | SCOPE_OVERGENERALISED | READING_STRATEGY | 2 | R70_HEDGING_SCOPE | Chú ý từ giới hạn phạm vi như "some", "often" hay "in this case" trong bài. | [ ] |
| 6 | WRONG_PARAGRAPH | READING_STRATEGY | 2 | PR_MATCHING_INFORMATION | Xác định đoạn chứa thông tin bằng ý chính của từng đoạn, không chỉ bằng một chi tiết. | [ ] |
| 7 | INFERENCE_UNSUPPORTED | READING_STRATEGY | 2 | R65_EVIDENCE_INFERENCE | Đáp án phải được bài hỗ trợ trực tiếp. Suy đoán hợp lý vẫn không đủ. | [ ] |
| 8 | SPELLING | SPELLING_NUMBER | 1 | LS_SPELL | Viết lại từ và kiểm tra từng chữ cái, đặc biệt các chữ cái gần giống nhau khi nghe. | [ ] |
| 9 | NUMBER_FORMAT | SPELLING_NUMBER | 2 | LS_NUM | Nghe kỹ số, ngày và giờ. Chú ý các cặp dễ nhầm như 13/30 và 15/50. | [ ] |
| 10 | PLURAL_FORM | SPELLING_NUMBER | 1 | (không) | Kiểm tra đáp án có cần "s" hay không theo đề bài. | [ ] |
| 11 | WORD_LIMIT_EXCEEDED | SPELLING_NUMBER | 1 | PR_SENTENCE_COMPLETION | Đọc kỹ giới hạn số từ trong đề và chỉ chép đúng phần cần điền. | [ ] |
| 12 | DISTRACTOR_CORRECTED | LISTENING_STRATEGY | 2 | L65_CORRECTION | Khi người nói dùng "sorry" hay "actually", chỉ lấy thông tin sau khi sửa. | [ ] |
| 13 | WRONG_SPEAKER | LISTENING_STRATEGY | 3 | L65_SPEAKER_PURPOSE | Theo dõi ai nói câu nào. Đừng chỉ nghe ý kiến mà bỏ qua người nói. | [ ] |
| 14 | TENSE | GRAMMAR | 1 | (không) | Xác định mốc thời gian trong câu trước khi chọn thì. | [ ] |
| 15 | ARTICLE | GRAMMAR | 1 | (không) | Dùng "the" khi đối tượng đã xác định; dùng "a/an" khi nhắc lần đầu. | [ ] |
| 16 | SUBJECT_VERB_AGREEMENT | GRAMMAR | 2 | (không) | Tìm chủ ngữ chính thật sự, không để danh từ đứng gần nhất quyết định động từ. | [ ] |
| 17 | SENTENCE_FRAGMENT | GRAMMAR | 2 | (không) | Mỗi câu cần có chủ ngữ và động từ chính. Cụm phân từ không thể đứng một mình. | [ ] |
| 18 | WORD_CHOICE | VOCABULARY | 1 | W70_LEXICAL_PRECISION | Kiểm tra sắc thái nghĩa của từ trong ngữ cảnh, không chỉ nghĩa gần đúng. | [ ] |
| 19 | COLLOCATION | VOCABULARY | 1 | W70_LEXICAL_PRECISION | Học cụm từ đi cùng nhau thay vì học từng từ riêng lẻ. | [ ] |
| 20 | LINKER_MISUSE | COHERENCE | 1 | W60_PARAGRAPH_COHESION | Từ nối phải thể hiện đúng quan hệ giữa hai ý: bổ sung, đối lập, nguyên nhân hay kết quả. | [ ] |
| 21 | NO_CLEAR_POSITION | COHERENCE | 2 | (không) | Nêu quan điểm rõ ràng ở mở bài và giữ nhất quán đến hết bài. | [ ] |
| 22 | UNDERDEVELOPED_IDEA | TASK | 2 | W70_IDEA_DEVELOPMENT | Mỗi ý chính cần một lý do và một ví dụ cụ thể. | [ ] |
| 23 | OFF_TOPIC | TASK | 3 | (không) | Đọc lại đề và đảm bảo mọi đoạn đều trả lời đúng câu hỏi. | [ ] |
| 24 | T1_NO_OVERVIEW | TASK | 3 | W60_T1_KEY_FEATURES | Thêm một đoạn tổng quan nêu xu hướng chính trước khi đưa số liệu. | [ ] |
| 25 | T1_DATA_MISREPORTED | TASK | 2 | W60_T1_KEY_FEATURES | Đối chiếu từng số liệu với biểu đồ, đặc biệt khi so sánh hai mốc thời gian. | [ ] |

Ghi chú cho người duyệt: SPELLING, NUMBER_FORMAT, PLURAL_FORM, WORD_LIMIT_EXCEEDED được thiết kế cho câu điền từ (FILL). V35 chưa có câu điền từ nào, nên bốn mã này chưa được gắn cho câu nào (trừ NUMBER_FORMAT ở dòng 19 của mục B, dùng cho câu trắc nghiệm).

---

## B. 19 câu đã gắn phương án sai (V35)

Mỗi dòng: câu, đáp án đúng, phương án sai được gắn, mã, giải thích gốc của câu (đã có trong seed). Đây là căn cứ để người duyệt kiểm tra mã có đúng với lý do sai không.

| # | Question version id (rút gọn) | Câu hỏi | Đáp án đúng | Phương án sai → mã | Giải thích gốc | Quyết định |
|---|---|---|---|---|---|---|
| 1 | 23…070000000006 (Listening) | What will the council do because of safety concerns? | B. Build protected lanes by the river | A. Limit each ride to thirty minutes → KEYWORD_MATCH_ONLY | "protected cycle lanes along the river". Ba mươi phút là bẫy: đó là thời gian miễn phí, không liên quan an toàn. | [ ] |
| 2 | 23…070000000011 (Listening) | What must guests pay extra for? | B. Parking | A. Breakfast → KEYWORD_MATCH_ONLY | Bẫy: bữa sáng được nhắc trước nhưng đã gồm trong giá; "parking is charged separately". | [ ] |
| 3 | 23…070000000015 (Listening) | What can people bring on the tour? | B. A small backpack | A. Large bags → KEYWORD_MATCH_ONLY | "do not bring large bags. A small backpack is fine." Large bags là bẫy vì được nhắc tới đầu tiên. | [ ] |
| 4 | 23…070000000026 (Listening) | Where will they meet? | C. At a cafe | A. At the station → DISTRACTOR_CORRECTED; B. In the library → KEYWORD_MATCH_ONLY | Station bị đổi thành "the cafe next to the library". Library là bẫy. | [ ] |
| 5 | 24…070000000002 (Reading) | Passage về bảo tàng; xác định đoạn nói về việc vào cửa miễn phí | B. Paragraph B | A. Paragraph A → KEYWORD_MATCH_ONLY | "Stopped paying" được diễn đạt lại thành "entry has been free since 2001". "Wealthy" ở đoạn A là bẫy về tiền. | [ ] |
| 6 | 27…070000000014 (Reading) | Choose the best heading for paragraph B. | A. A sudden shift | C. Kitchens of the future → KEYWORD_MATCH_ONLY | "almost overnight", "within weeks": thay đổi rất nhanh. "kitchens" chỉ là chi tiết, đó là bẫy lặp từ. | [ ] |
| 7 | 33…070000000001 (Reading) | Which conclusion is best supported by the passage? | B. The night bus helped workers get home after late shifts | C. The night bus caused taxi prices to fall → INFERENCE_UNSUPPORTED. (A chưa gắn, xem mục C) | Gần một nửa hành khách là nhân viên bệnh viện và nhà hàng vừa tan ca. A trái với số liệu; C là bẫy nhân quả: bài chỉ nói số lời phàn nàn giảm, không nói giá taxi giảm. | [ ] |
| 8 | 26…070000000001 (Reading) | What is the passage mainly about? | B. The return of night trains and the problems they face | C. How to book a sleeper carriage → OFF_TOPIC. (A chưa gắn, xem mục C) | Đoạn A, B nói tàu đêm quay lại và vì sao khách chọn; đoạn C nói khó khăn. A sai vì vé thường đắt hơn máy bay; C không được nhắc. | [ ] |
| 9 | 26…070000000003 (Reading) | What is the main idea of paragraph C? | C. Night trains struggle to make money | B. Old carriages are being replaced → INFERENCE_UNSUPPORTED | Đoạn C nói tàu đêm khó có lãi: vé đắt, toa cũ, phí đường ray cao. Bài không nói toa cũ đang được thay. | [ ] |
| 10 | 26…070000000013 (Reading) | Passage về thư viện trường và laptop miễn phí; câu hỏi về chi phí | B. Paragraph B | A. Paragraph A → KEYWORD_MATCH_ONLY; C. Paragraph C → KEYWORD_MATCH_ONLY | "free of charge" nghĩa là không tốn tiền. Đoạn A và C lặp từ "library" nhưng không nói về chi phí. | [ ] |
| 11 | 26…070000000015 (Reading) | Passage về mưa lớn và mùa màng; đoạn nói về thiệt hại lương thực | C. Paragraph C | B. Paragraph B → WRONG_PARAGRAPH | "damaged their crops" là thiệt hại cho sản xuất lương thực; đoạn B nói đường bị ngập, không nói về lương thực. | [ ] |
| 12 | 27…070000000010 (Reading) | Choose the best heading for paragraph A. | A. The origins of coffee drinking | C. How coffee is grown → WRONG_PARAGRAPH. (B chưa gắn, xem mục C) | Đoạn A kể nơi và thời điểm cà phê bắt đầu được uống. "religion today" sai thời điểm; cách trồng thuộc đoạn C. | [ ] |
| 13 | 27…070000000012 (Reading) | Choose the best heading for paragraph C. | B. A global crop | C. The price of coffee → INFERENCE_UNSUPPORTED | Hơn 50 nước trồng, Brazil chiếm một phần ba: cà phê là cây trồng toàn cầu. Đoạn không nói về giá. | [ ] |
| 14 | 27…070000000013 (Reading) | Choose the best heading for paragraph A. | B. Life before the change | A. Why workers prefer offices; C. The cost of home offices → INFERENCE_UNSUPPORTED (cả hai) | Đoạn A mô tả tình hình trước 2020. Bài không nói người lao động thích văn phòng, cũng không nói chi phí. | [ ] |
| 15 | 27…070000000015 (Reading) | Choose the best heading for paragraph C. | C. A middle way | B. Working longer hours → INFERENCE_UNSUPPORTED. (A chưa gắn, xem mục C) | "a mix of both": kết hợp văn phòng và ở nhà. Văn phòng không biến mất, và đoạn không nói giờ làm. | [ ] |
| 16 | 33…070000000002 (Reading) | What can be inferred from the sentence about complaints concerning taxi prices? | A. Some residents may have used the bus instead of paying for taxis | C. The council plans to ban taxis at night → INFERENCE_UNSUPPORTED. (B chưa gắn, xem mục C) | Giá taxi không đổi nhưng phàn nàn giảm, nên suy luận hợp lý là một số người đi xe buýt thay taxi. B trái với bài (giá không đổi); C không có căn cứ. | [ ] |
| 17 | 33…070000000005 (Reading) | What can be inferred from the survey result? | B. Sunday hours may be encouraging some people to visit more often | A. Every visitor wants Sunday opening… → SCOPE_OVERGENERALISED; C. Most visitors dislike the weekday opening hours → INFERENCE_UNSUPPORTED | 6/10 người nói sẽ đến ít hơn nếu bỏ giờ Chủ nhật, nên giờ Chủ nhật có thể khiến một số người đến thường xuyên hơn. A khái quát quá mức ("every"); C không có căn cứ. | [ ] |
| 18 | 33…070000000008 (Reading) | Which option is closest in meaning to "tickets are priced to cover costs rather than to make a profit"? | B. The orchestra charges only what it needs to pay its expenses | A. Tickets are so cheap that the orchestra loses money → PARAPHRASE_TRAP; C. The orchestra refuses to sell tickets → INFERENCE_UNSUPPORTED | "Cover costs" là đủ bù chi phí, "rather than profit" là không tính lợi nhuận. A suy diễn lỗ vốn; C không có trong bài. | [ ] |
| 19 | 33…070000000007 (Reading) | Which option is closest in meaning to "Membership has since tripled"? | A. The orchestra now has three times as many members as it had at the start | B. The orchestra has gained three new members → NUMBER_FORMAT; C. Membership fees have risen threefold → OFF_TOPIC | "Tripled" nghĩa là gấp ba (14 thành 42 người). B sai số lượng; C đổi chủ đề sang phí thành viên. | [ ] |

Ghi chú: mã NUMBER_FORMAT ở dòng 19 được dùng cho "sai số", không phải sai định dạng. Đây là cách hiểu theo tên mã trong V34 ("Sai số hoặc sai định dạng số"). Người duyệt có thể chọn mã khác hoặc tạo mã riêng cho sai số trong câu trắc nghiệm.

---

## C. 6 phương án sai chưa có mã

| # | Câu | Phương án chưa gắn | Vì sao chưa gắn | Mã đề xuất |
|---|---|---|---|---|
| a | 26…070000000001 | A. Night trains are cheaper than flights | Bài nói ngược lại (vé thường đắt hơn) | CONTRADICTED_BY_PASSAGE |
| b | 33…070000000001 | A. Students were the main users of the night bus | Trái với số liệu trong bài | CONTRADICTED_BY_PASSAGE |
| c | 27…070000000015 | A. The end of the office | Bài nói văn phòng không biến mất | CONTRADICTED_BY_PASSAGE |
| d | 33…070000000002 | B. Taxi drivers lowered their fares | Bài nói giá không đổi | CONTRADICTED_BY_PASSAGE |
| e | 20…070000000003 | B. Critics point out that green roofs are expensive… (chi tiết, không phải ý chính) | Lấy một lý do cụ thể làm ý chính của đoạn | DETAIL_AS_MAIN_IDEA |
| f | 27…070000000010 | B. Coffee and religion today | Gắn ý với thời điểm (hiện nay) mà đoạn không nói tới | WRONG_TIME_FRAME |

Đề xuất cho ba mã mới (nếu được duyệt):
- **CONTRADICTED_BY_PASSAGE**: nhóm READING_STRATEGY, severity 2. Gợi ý sửa: "Phương án này ngược với bài. Đọc lại câu gốc để xem bài khẳng định điều gì."
- **DETAIL_AS_MAIN_IDEA**: nhóm READING_STRATEGY, severity 2. Gợi ý sửa: "Đây là một chi tiết hỗ trợ, không phải ý chính. Tìm câu nêu ý chung của cả đoạn."
- **WRONG_TIME_FRAME**: nhóm READING_STRATEGY, severity 2. Gợi ý sửa: "Bài không nói về thời điểm này. Kiểm tra mốc thời gian trong đoạn trước khi chọn."

---

## D. Cần người duyệt xác nhận

- Các mã ở mục A có đúng nghĩa và thứ tự ưu tiên không.
- Mỗi cặp câu và mã ở mục B có đúng lý do sai không. Dòng 7, 8, 13, 14 và 18 có mã INFERENCE_UNSUPPORTED hoặc OFF_TOPIC; người duyệt có thể đề xuất mã cụ thể hơn.
- Các câu chưa gắn còn 139 câu trắc nghiệm Reading/Listening (xem Q4).

---

## E. Sau khi có quyết định

Mình sẽ làm theo đúng quyết định đã ghi:
1. Sửa mã, severity, gợi ý sửa ở V34 bằng migration mới (V36), không sửa V34 đã commit.
2. Sửa hoặc bỏ phương án sai ở V35 bằng migration mới nếu có dòng bị bỏ.
3. Thêm 3 mã mới vào V36 nếu Q1 đồng ý, rồi gắn 6 phương án ở mục C.
4. Chạy lại test migration và cả ba service, cập nhật trạng thái phase 3 thành hoàn thành.

Phase 3 chỉ được đánh dấu hoàn thành khi các mục Q1 đến Q4 đã có quyết định và người duyệt đã ký mục D.

Người duyệt: ____________________  Ngày: __________
