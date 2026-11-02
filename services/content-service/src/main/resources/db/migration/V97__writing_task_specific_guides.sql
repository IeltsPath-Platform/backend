-- Task-specific writing guides for the 24 prompts of the Writing practice tests PT-WRITING-01 to PT-WRITING-12.
--
-- V38 gave every prompt the same short template for its type (a handful of texts shared by many prompts). Each prompt now has its
-- own guide: what the task asks, the figures or ideas that matter for this prompt, what the overview or thesis should say, how to
-- split the body paragraphs, useful words and the mistakes to avoid. The text is light Markdown (## headings, - bullets, **bold**)
-- and is shown beside the prompt on the practice screen; it does not appear on the exam screen. No schema change: it is the
-- existing question_versions.guide column. Idempotent: the guide is overwritten.

-- PT-WRITING-01 Task 1 (line graph)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Line graph**: so sánh phần trăm hộ gia đình sở hữu bốn loại thiết bị điện tử từ 1995 đến 2015. Mốc thời gian đã qua nên dùng thì quá khứ (rose, fell, peaked). Đơn vị là phần trăm hộ gia đình, không phải số lượng thiết bị.

## 2. Số liệu đáng chú ý
- **Điện thoại di động** tăng mạnh nhất: từ 5% lên 95%, tức tăng 90 điểm phần trăm, và cao nhất vào cuối kỳ.
- **Máy tính** tăng đều từ 20% lên 85% (tăng 65 điểm phần trăm).
- **Máy tính bảng** xuất hiện muộn: 0% cho đến sau 2010, rồi lên 60%.
- **MP3 player** là thiết bị duy nhất giảm: đạt đỉnh 45% năm 2010 rồi tụt còn 20%.

## 3. Gợi ý Overview
Chỉ nêu hai ý lớn, chưa kèm số liệu: ba thiết bị tăng trong cả giai đoạn (điện thoại di động tăng nhanh nhất), còn MP3 player tăng rồi giảm sau 2010.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "shows the percentage of households owning four types of electronic devices", ví dụ "illustrates the proportion of homes that had four kinds of electronic equipment".
- **Body 1** (3-4 câu): điện thoại và máy tính, hai thiết bị tăng liên tục và đứng đầu cuối kỳ; so sánh mức tăng 90 và 65 điểm phần trăm.
- **Body 2** (3-4 câu): máy tính bảng xuất hiện muộn và tăng nhanh, đối lập với MP3 player tăng rồi giảm.

## 5. Từ và cấu trúc nên dùng
- Xu hướng: rose sharply, climbed steadily, peaked at, declined to, remained stable.
- Đối chiếu: whereas, while, in contrast.
- Mốc thời gian: between 1995 and 2015, after 2010, by the end of the period.

## 6. Lưu ý
- Ghi đơn vị % và nhắc năm khi đưa số liệu.
- Không giải thích nguyên nhân hay nêu ý kiến cá nhân, vì đề không cho thông tin đó.
- Gom theo xu hướng, không viết bốn đoạn cho bốn đường.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001094';

-- PT-WRITING-01 Task 2 (agree / disagree)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Agree / Disagree**: làm việc tại nhà tốt hơn cho **cả nhân viên lẫn doanh nghiệp**. Từ khóa quan trọng là "both": bạn phải nói về cả hai bên, không chỉ một bên.

## 2. Hướng triển khai
Lập trường dễ bảo vệ là đồng ý một phần ("I partly agree"): lợi nhiều hơn hại nhưng có điều kiện.
- **Nhân viên**: tiết kiệm thời gian đi lại, giờ giấc linh hoạt, cân bằng công việc và cuộc sống. Mặt trái: cô lập, khó tách công việc khỏi đời sống riêng.
- **Doanh nghiệp**: giảm chi phí văn phòng, tuyển được người ở xa. Mặt trái: khó giám sát, giảm tương tác và sáng tạo của nhóm.
- Ví dụ gợi ý: các công ty chuyển sang làm việc từ xa trong và sau đại dịch.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu rõ quan điểm.
- **Body 1**: lợi ích cho nhân viên, rồi cho doanh nghiệp, kèm một ví dụ.
- **Body 2**: giới hạn của làm việc tại nhà, giải thích vì sao chưa thể nói là "tốt hơn" trong mọi trường hợp.
- **Conclusion**: nhắc lại quan điểm, không thêm ý mới.

## 4. Từ vựng gợi ý
- flexible working hours, work-life balance, commute, overhead costs, productivity, face-to-face interaction, isolation, remote workforce.

## 5. Lưu ý
- Giữ một lập trường từ Introduction đến Conclusion.
- Mỗi bên (nhân viên, doanh nghiệp) cần ít nhất một lý do; ví dụ cụ thể có giá trị hơn nhận định chung chung.
- Mỗi thân bài chỉ một ý chính: câu chủ đề, giải thích, ví dụ.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001095';

-- PT-WRITING-02 Task 1 (bar chart)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Bar chart** chỉ có một mốc thời gian (2020): so sánh số giờ học trung bình mỗi tuần của sinh viên ở năm nước. Không có xu hướng tăng giảm nên dùng thì quá khứ đơn và các từ so sánh, không dùng rose hay fell.

## 2. Số liệu đáng chú ý
- **Hàn Quốc** cao nhất với 18 giờ, gấp đôi **Brazil** thấp nhất với 9 giờ (chênh 9 giờ).
- **Nhật Bản** đứng thứ hai với 14 giờ.
- **Canada** (12 giờ) và **Đức** (11 giờ) gần nhau, nằm ở giữa.

## 3. Gợi ý Overview
Nêu nước cao nhất và thấp nhất, kèm nhận xét chung: sinh viên Hàn Quốc và Nhật Bản học nhiều giờ hơn ba nước còn lại, trong khi Brazil học ít nhất.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "compares the average number of hours students spent studying each week".
- **Body 1** (3-4 câu): Hàn Quốc (18) và Nhật Bản (14), hai nước đứng đầu.
- **Body 2** (3-4 câu): Canada (12), Đức (11) và Brazil (9), so sánh khoảng cách giữa nhóm này với nhóm đầu.

## 5. Từ và cấu trúc nên dùng
- So sánh: the highest, the lowest, twice as many as, slightly more than, considerably fewer than.
- Liên kết: whereas, while, in comparison, by contrast.

## 6. Lưu ý
- Ghi đơn vị "hours per week" và nhắc năm 2020.
- Chỉ chọn số liệu quan trọng để so sánh, không liệt kê lần lượt cả năm nước theo thứ tự trong đề.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001096';

-- PT-WRITING-02 Task 2 (discuss both views and give your opinion)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Discuss both views and give your own opinion**: một bên cho rằng đại học nên miễn phí, bên kia cho rằng sinh viên phải tự trả học phí. Bạn cần trình bày cả hai quan điểm **và** nêu ý kiến riêng.

## 2. Hướng triển khai
- **Ủng hộ miễn phí**: mọi người có cơ hội học bất kể thu nhập, tăng khả năng thăng tiến xã hội, xã hội có lực lượng lao động giỏi hơn.
- **Ủng hộ thu học phí**: ngân sách nhà nước có hạn, người đóng thuế không đi học vẫn phải gánh chi phí, sinh viên tự trả thường nghiêm túc hơn.
- **Ý kiến riêng gợi ý**: miễn phí cho các ngành thiếu nhân lực hoặc cho sinh viên khó khăn, còn lại thu một phần học phí.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase hai quan điểm và nêu sơ lược ý kiến của bạn.
- **Body 1**: quan điểm thứ nhất kèm lý do và ví dụ.
- **Body 2**: quan điểm thứ hai kèm lý do và ví dụ.
- **Conclusion**: khẳng định ý kiến riêng và lý do chính.

## 4. Từ vựng gợi ý
- tuition fees, higher education, equal access, social mobility, public funding, financial burden, taxpayers, scholarship.

## 5. Lưu ý
- Lỗi phổ biến nhất là chỉ bàn hai quan điểm mà quên nêu ý kiến riêng.
- Hai thân bài nên cân đối về độ dài.
- Dùng cách diễn đạt trung tính khi trình bày quan điểm của người khác: some people argue that, others claim that.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001097';

-- PT-WRITING-03 Task 1 (two pie charts)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Pie charts** (hai biểu đồ tròn): so sánh cách một hộ gia đình trung bình dùng năng lượng năm 1990 và 2020. Mỗi biểu đồ cộng lại 100%, và việc cần làm là so sánh sự thay đổi giữa hai năm. Dùng thì quá khứ.

## 2. Số liệu đáng chú ý
- **Sưởi ấm** vẫn lớn nhất ở cả hai năm nhưng giảm từ 52% xuống 40% (giảm 12 điểm phần trăm).
- **Đồ điện gia dụng** tăng mạnh nhất, từ 18% lên 38% (hơn gấp đôi), gần ngang sưởi ấm vào 2020.
- **Chiếu sáng** giảm từ 20% xuống 8%, tức giảm hơn một nửa.
- **Nấu ăn** tăng nhẹ từ 10% lên 14%.

## 3. Gợi ý Overview
Sưởi ấm là mục lớn nhất cả hai năm nhưng giảm; đồ điện gia dụng tăng mạnh nhất và gần bắt kịp sưởi ấm; chiếu sáng giảm nhiều.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "how an average household used energy in 1990 and in 2020".
- **Body 1** (3-4 câu): hai mục giảm, sưởi ấm và chiếu sáng, kèm số liệu hai năm.
- **Body 2** (3-4 câu): hai mục tăng, đồ điện gia dụng và nấu ăn; nhấn mạnh mức tăng của đồ điện gia dụng.

## 5. Từ và cấu trúc nên dùng
- accounted for, made up, the largest share, fell from ... to ..., more than doubled, more than halved, a slight rise.
- Đối chiếu: whereas, in contrast.

## 6. Lưu ý
- Nhắc rõ đơn vị phần trăm và hai năm đang so sánh.
- Không giải thích nguyên nhân vì đề không cho dữ kiện.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001098';

-- PT-WRITING-03 Task 2 (problems and measures)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Problems and solutions** với hai câu hỏi: tắc nghẽn giao thông gây **vấn đề gì**, và có thể làm **biện pháp nào** để giảm. Bài phải trả lời đủ cả hai.

## 2. Hướng triển khai
- **Vấn đề**: mất thời gian và giảm năng suất, ô nhiễm không khí và tiếng ồn, tốn nhiên liệu, xe cấp cứu đến chậm, căng thẳng cho người lái xe.
- **Biện pháp**: đầu tư giao thông công cộng, thu phí vào vùng đông xe, làm làn đường dành cho xe đạp, khuyến khích đi chung xe, giờ làm việc linh hoạt hoặc làm việc từ xa.
- Chọn hai vấn đề rõ nhất cho Body 1 và hai biện pháp khả thi nhất cho Body 2, mỗi ý kèm giải thích và ví dụ.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nói bài sẽ bàn về vấn đề rồi biện pháp.
- **Body 1**: các vấn đề của tắc nghẽn giao thông.
- **Body 2**: các biện pháp, nêu vì sao chúng giúp giảm tắc nghẽn.
- **Conclusion**: tóm tắt ngắn gọn cả hai ý.

## 4. Từ vựng gợi ý
- traffic congestion, rush hour, air pollution, public transport, congestion charge, cycle lanes, carpooling, commuters.

## 5. Lưu ý
- Mỗi biện pháp nên gắn với một vấn đề đã nêu để bài logic.
- Ví dụ về một thành phố đã áp dụng biện pháp sẽ thuyết phục hơn nói chung chung.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001099';

-- PT-WRITING-04 Task 1 (table)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Table**: phần trăm người đi xem phim ít nhất mỗi tháng một lần, chia theo bốn nhóm tuổi, ở ba mốc 2000, 2010 và 2020. Có ba mốc nên bạn mô tả được xu hướng. Dùng thì quá khứ.

## 2. Số liệu đáng chú ý
- **15-24 tuổi** cao nhất cả ba năm: 48%, 52%, rồi giảm còn 40% năm 2020.
- **25-34 tuổi** có cùng dạng tăng rồi giảm: 35%, 38%, 33%.
- **35-49 tuổi** gần như ổn định: 22%, 25%, 24%.
- **50 tuổi trở lên** là nhóm duy nhất tăng liên tục: 10%, 14%, 18%.
- Khoảng cách giữa nhóm trẻ nhất và nhóm già nhất thu hẹp từ 38 xuống 22 điểm phần trăm.

## 3. Gợi ý Overview
Nhóm trẻ nhất đi xem phim thường xuyên nhất nhưng giảm sau 2010; chỉ nhóm từ 50 tuổi trở lên tăng đều, nên khoảng cách giữa các nhóm tuổi thu hẹp lại.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "the percentage of people in four age groups who visited the cinema at least once a month".
- **Body 1** (3-4 câu): hai nhóm trẻ (15-24 và 25-34) tăng đến 2010 rồi giảm.
- **Body 2** (3-4 câu): nhóm 35-49 ổn định và nhóm 50 trở lên tăng đều, so sánh với hai nhóm trên.

## 5. Từ và cấu trúc nên dùng
- peaked at, fell back to, remained relatively stable, rose steadily, the gap narrowed.
- Đối chiếu: whereas, in contrast.

## 6. Lưu ý
- Ghi rõ nhóm tuổi và đơn vị phần trăm, không chép lại cả bảng.
- Nhắc mốc năm mỗi khi đưa số liệu.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001100';

-- PT-WRITING-04 Task 2 (advantages outweigh disadvantages)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Advantages and disadvantages**, hỏi "Do the advantages outweigh the disadvantages?". Bạn phải bàn cả hai mặt **và** kết luận mặt nào nhiều hơn.

## 2. Hướng triển khai
- **Lợi ích**: nâng cao ngoại ngữ, tiếp xúc nền văn hóa khác, tiếp cận nền giáo dục chất lượng hơn, tăng cơ hội nghề nghiệp, trưởng thành và tự lập hơn.
- **Bất lợi**: chi phí học tập và sinh hoạt cao, nhớ nhà và cú sốc văn hóa, nguy cơ chảy máu chất xám cho quê hương.
- Lập trường gợi ý: lợi ích nhiều hơn nhưng cần chuẩn bị tài chính và tâm lý.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu rõ bên nào nhiều hơn.
- **Body 1**: các lợi ích chính kèm ví dụ.
- **Body 2**: các bất lợi, rồi giải thích vì sao chúng nhẹ hơn lợi ích.
- **Conclusion**: nhắc lại quan điểm.

## 4. Từ vựng gợi ý
- international exposure, cultural awareness, tuition fees, cost of living, homesickness, culture shock, brain drain, employability.

## 5. Lưu ý
- Phải có câu trả lời trực tiếp cho "outweigh" ngay từ Introduction.
- Nếu Body 1 dài hơn Body 2, đó là phần bạn muốn nhấn mạnh; hãy giữ nhất quán với kết luận.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001101';

-- PT-WRITING-05 Task 1 (process diagram: tea bags)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Process diagram**: quy trình sản xuất túi trà gồm 7 giai đoạn, từ hái lá đến đóng hộp. Dùng **thì hiện tại đơn dạng bị động** (are picked, are withered) và các từ chỉ thứ tự. Không có số liệu để so sánh.

## 2. Các giai đoạn cần nắm
- Lá trà được hái (picked), làm héo (withered), vò (rolled) rồi sấy khô (dried).
- Sau đó lá được phân loại theo kích cỡ (sorted by size).
- Cuối cùng cho vào túi giấy (packed into paper bags) và đóng hộp (boxed).

## 3. Gợi ý Overview
Quy trình gồm bảy giai đoạn liên tiếp, bắt đầu từ việc hái lá và kết thúc ở việc đóng hộp thành phẩm; bốn giai đoạn đầu xử lý lá trà, ba giai đoạn sau là phân loại và đóng gói.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "shows the stages in the production of tea bags".
- **Overview**: số giai đoạn, điểm đầu và điểm cuối.
- **Body 1**: giai đoạn 1 đến 4, xử lý lá (picked, withered, rolled, dried).
- **Body 2**: giai đoạn 5 đến 7, phân loại và đóng gói.

## 5. Từ và cấu trúc nên dùng
- Thứ tự: first, next, then, after that, following this, finally.
- Nối hai bước: once the leaves have been dried, they are sorted; before being packed, ...
- Bị động: are picked, are rolled, are packed into.

## 6. Lưu ý
- Không thêm chi tiết đề không có (thời gian sấy, nhiệt độ, nơi làm).
- Không đưa ý kiến cá nhân.
- Mỗi giai đoạn nên xuất hiện trong bài theo đúng thứ tự.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001102';

-- PT-WRITING-05 Task 2 (causes and evaluation)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Two-part question**: (1) vì sao người trẻ ít đọc báo và xem tin tức, (2) đây có phải vấn đề của xã hội không. Cần trả lời cả hai câu và kết luận rõ ràng cho câu thứ hai.

## 2. Hướng triển khai
- **Nguyên nhân**: tin tức đến thẳng điện thoại qua mạng xã hội, người trẻ thích video ngắn và nội dung nhanh, tin tức bị cho là tiêu cực hoặc thiếu tin cậy, không có thời gian.
- **Có phải vấn đề?** Có: người trẻ dễ tin tin giả, kém hiểu biết về các vấn đề chung, ít tham gia việc cộng đồng.
- Quan điểm cân bằng: chỉ là vấn đề khi họ không còn nguồn thông tin đáng tin cậy nào; nhiều bạn trẻ vẫn nắm tin qua kênh khác.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase hiện tượng và nói bạn sẽ bàn nguyên nhân rồi tác động.
- **Body 1**: hai nguyên nhân chính kèm giải thích.
- **Body 2**: có phải vấn đề không, kèm hệ quả hoặc ví dụ.
- **Conclusion**: tóm tắt nguyên nhân và nêu kết luận.

## 4. Từ vựng gợi ý
- news outlets, social media feeds, misinformation, fake news, attention span, informed citizens, civic engagement, reliable sources.

## 5. Lưu ý
- Câu hỏi thứ hai cần câu trả lời trực tiếp ("it is / it is not a serious problem"), không nói lửng.
- Mỗi nguyên nhân cần một giải thích ngắn, không chỉ liệt kê.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001103';

-- PT-WRITING-06 Task 1 (two maps: village centre)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Map** (hai bản đồ): so sánh trung tâm làng năm 1995 và hiện tại. Dùng thì quá khứ cho 1995, hiện tại hoàn thành hoặc hiện tại đơn cho "today", và thể bị động (was replaced, has been built).

## 2. Các thay đổi cần nêu
- **Đất nông nghiệp** được thay bằng nhà ở (farmland replaced by housing).
- **Trường học cũ** trở thành thư viện (the old school became a library).
- **Bãi đỗ xe mới** được xây cạnh nhà ga (a new car park beside the station).
- **Cái ao** bị lấp (the pond was filled in).

## 3. Gợi ý Overview
Trung tâm làng được phát triển thêm: đất nông nghiệp và ao biến mất, nhà ở và bãi đỗ xe được thêm vào, còn trường học đổi công năng thành thư viện.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "show a village centre in 1995 and today".
- **Body 1** (3-4 câu): thay đổi về khu đất, farmland thành housing và ao bị lấp.
- **Body 2** (3-4 câu): thay đổi về công trình, trường học thành thư viện và bãi đỗ xe mới cạnh ga.

## 5. Từ và cấu trúc nên dùng
- Thay đổi: was replaced by, was converted into, was filled in, has been constructed, has disappeared.
- Vị trí: beside, next to, in the centre, to the north of.

## 6. Lưu ý
- Chỉ dùng vị trí có trên bản đồ, không tự thêm hướng hay chi tiết.
- Không đưa ý kiến về việc thay đổi tốt hay xấu.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001104';

-- PT-WRITING-06 Task 2 (agree / disagree)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Agree / Disagree**: trẻ em nên bắt đầu học ngoại ngữ từ **bậc tiểu học**. Hãy bám vào độ tuổi này, đừng bàn chung về việc học ngoại ngữ.

## 2. Hướng triển khai
- **Ủng hộ**: trẻ nhỏ tiếp thu ngôn ngữ và phát âm tự nhiên hơn, có nhiều năm để đạt trình độ cao, được lợi về tư duy và cơ hội nghề nghiệp sau này.
- **Phản đối**: lấn thời gian của các môn nền tảng, thiếu giáo viên đủ chất lượng, tạo áp lực, trẻ cần vững tiếng mẹ đẻ trước.
- Lập trường gợi ý: đồng ý, nếu việc học theo hình thức vui như trò chơi và bài hát.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu quan điểm.
- **Body 1**: lý do chính ủng hộ, kèm ví dụ về trẻ học ngoại ngữ sớm.
- **Body 2**: một lo ngại thường gặp, rồi giải thích vì sao nó có thể giải quyết được.
- **Conclusion**: nhắc lại quan điểm.

## 4. Từ vựng gợi ý
- primary school, foreign language, native-like pronunciation, cognitive development, curriculum, qualified teachers, mother tongue, fluency.

## 5. Lưu ý
- Giữ nhất quán một quan điểm.
- Dùng ví dụ cụ thể (một quốc gia dạy tiếng Anh từ lớp 1) thay vì nhận định chung chung.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000001105';

-- PT-WRITING-07 Task 1 (table: library visits)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Table**: số lượt đến thư viện công cộng trung bình mỗi người trong một năm, theo năm nhóm tuổi, năm 2000 và 2020. Đơn vị là lượt mỗi người mỗi năm. Dùng thì quá khứ.

## 2. Số liệu đáng chú ý
- **65 tuổi trở lên** đến nhiều nhất cả hai năm (11 và 12 lượt) và là nhóm duy nhất tăng.
- **Dưới 16 tuổi** giảm nhiều nhất, từ 9 xuống 6 lượt (giảm 3).
- **45-64 tuổi** giảm từ 7 xuống 5 lượt.
- **16-24 tuổi** thấp nhất cả hai năm (4 và 3 lượt).
- **25-44 tuổi** giảm nhẹ từ 5 xuống 4 lượt.

## 3. Gợi ý Overview
Lượt đến thư viện giảm ở mọi nhóm tuổi trừ nhóm từ 65 tuổi trở lên; nhóm lớn tuổi nhất đến nhiều nhất, còn nhóm 16-24 đến ít nhất.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "the average number of visits each person made to public libraries in one year".
- **Body 1** (3-4 câu): các nhóm giảm rõ, dưới 16 và 45-64, kèm số liệu hai năm.
- **Body 2** (3-4 câu): hai nhóm trẻ giảm nhẹ, 16-24 và 25-44, rồi đối lập với nhóm 65 trở lên tăng nhẹ.

## 5. Từ và cấu trúc nên dùng
- visits per person, fell from ... to ..., a decline of, remained the most frequent, the only group to.
- Đối chiếu: whereas, in contrast.

## 6. Lưu ý
- Không chép lại cả 10 số liệu; chọn những so sánh rõ nhất.
- Nhắc đơn vị "visits" và hai mốc năm.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002001';

-- PT-WRITING-07 Task 2 (agree / disagree)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Agree / Disagree**: trường học nên dành **nhiều thời gian hơn cho kỹ năng thực tế** (nấu ăn, quản lý tiền) và **ít hơn cho các môn học thuật**. Cần nói rõ bạn đồng ý với việc đổi tỷ trọng đó đến mức nào.

## 2. Hướng triển khai
- **Ủng hộ**: kỹ năng sống chuẩn bị cho cuộc sống người lớn, học sinh hứng thú hơn, nhiều người ra trường không biết quản lý chi tiêu.
- **Phản đối**: các môn học thuật là nền cho đại học và nghề nghiệp, kỹ năng thực tế có thể học ở nhà, thời gian ở trường có hạn.
- Lập trường gợi ý: đồng ý một phần, nên tăng kỹ năng thực tế nhưng không cắt giảm các môn cốt lõi.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu quan điểm.
- **Body 1**: lợi ích của kỹ năng thực tế kèm ví dụ.
- **Body 2**: vì sao các môn học thuật vẫn cần giữ.
- **Conclusion**: nhắc lại quan điểm cân bằng.

## 4. Từ vựng gợi ý
- life skills, practical subjects, academic subjects, financial literacy, curriculum, vocational training, well-rounded education.

## 5. Lưu ý
- Đề nói "less time on academic subjects": hãy bàn về việc giảm đó, đừng chỉ bàn lợi ích của kỹ năng thực tế.
- Mỗi thân bài một ý chính có ví dụ.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002002';

-- PT-WRITING-08 Task 1 (line graph: rainfall in two cities)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Line graph** so sánh lượng mưa trung bình mỗi tháng (đơn vị milimét) của **Lisbon** và **Dublin** trong một năm. Đồ thị là trung bình theo tháng, không gắn với năm cụ thể nên dùng thì hiện tại đơn.

## 2. Số liệu đáng chú ý
- **Dublin** khá ổn định, luôn trong khoảng 50 đến 85 mm: thấp nhất 50 mm vào tháng 4, cao nhất 85 mm vào tháng 11.
- **Lisbon** thay đổi theo mùa rất mạnh: 100 mm vào tháng 1, giảm xuống chỉ 5 mm vào tháng 7, rồi tăng lên 110 mm vào tháng 12.
- Chênh lệch giữa tháng khô nhất và ẩm nhất: Lisbon khoảng 105 mm, Dublin khoảng 35 mm.
- Mùa đông Lisbon mưa nhiều hơn mức cao nhất của Dublin; mùa hè thì ngược lại.

## 3. Gợi ý Overview
Dublin có lượng mưa tương đối đều quanh năm, còn Lisbon biến động mạnh theo mùa: mưa nhiều vào mùa đông và gần như không mưa vào mùa hè.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "the average rainfall, in millimetres, in Lisbon and Dublin in each month".
- **Body 1** (3-4 câu): Dublin, giữ nguyên khoảng 50 đến 85 mm, nêu tháng thấp nhất và cao nhất.
- **Body 2** (3-4 câu): Lisbon, từ 100 mm xuống 5 mm rồi lên 110 mm, so sánh với Dublin.

## 5. Từ và cấu trúc nên dùng
- fluctuates, remains relatively stable, seasonal pattern, drops sharply to, climbs back to, reaches a low / a peak.
- Đối chiếu: whereas, in contrast, by comparison.

## 6. Lưu ý
- Ghi đơn vị mm và nêu tháng khi đưa số liệu.
- Không đoán số liệu của tháng không được nêu trong đề.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002003';

-- PT-WRITING-08 Task 2 (discuss both views and give your opinion)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Discuss both views and give your own opinion**: chính phủ nên tài trợ **bảo tàng và nhà hát**, hay dùng khoản tiền đó cho **y tế và giáo dục**. Cần trình bày cả hai quan điểm và nêu ý kiến riêng.

## 2. Hướng triển khai
- **Ủng hộ tài trợ nghệ thuật**: bảo tồn di sản và văn hóa, thu hút du lịch, tạo cơ hội tiếp cận văn hóa cho mọi người, tốt cho sức khỏe tinh thần.
- **Ủng hộ y tế và giáo dục**: là nhu cầu thiết yếu, liên quan đến sự sống và tương lai, giúp nền kinh tế phát triển.
- **Ý kiến riêng gợi ý**: ưu tiên y tế và giáo dục trước, nhưng vẫn dành một khoản vừa phải cho nghệ thuật.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase hai quan điểm và nêu ý kiến.
- **Body 1**: lý do nên tài trợ bảo tàng và nhà hát.
- **Body 2**: lý do nên ưu tiên y tế và giáo dục, rồi ý kiến riêng của bạn.
- **Conclusion**: khẳng định ý kiến và lý do chính.

## 4. Từ vựng gợi ý
- public funding, cultural heritage, arts venues, essential services, healthcare system, government budget, priorities, tourism revenue.

## 5. Lưu ý
- Đừng quên ý kiến riêng; hãy viết rõ "In my opinion" hoặc "I believe".
- Giữ ví dụ cụ thể, ví dụ một bảo tàng thu hút khách du lịch hoặc một bệnh viện thiếu kinh phí.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002004';

-- PT-WRITING-09 Task 1 (process diagram: recycled paper)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Process diagram**: quy trình làm giấy tái chế gồm 7 giai đoạn, từ thu gom giấy thải đến cuộn và cắt giấy thành phẩm. Dùng **thì hiện tại đơn dạng bị động** và các từ chỉ thứ tự. Không có số liệu.

## 2. Các giai đoạn cần nắm
- Giấy thải được thu gom và phân loại.
- Trộn với nước trong bể lớn và nghiền thành bột giấy (pulp).
- Bột giấy đi qua các tấm lọc để loại ghim bấm và nhựa.
- Mực được rửa sạch bằng hóa chất.
- Bột sạch được ép giữa các con lăn thành tờ giấy ướt.
- Tờ giấy được sấy bằng các trụ nóng.
- Giấy được cuộn lại và cắt theo kích cỡ.

## 3. Gợi ý Overview
Quy trình có bảy giai đoạn: ba giai đoạn đầu biến giấy thải thành bột giấy sạch, các giai đoạn còn lại tạo ra tờ giấy rồi sấy, cuộn và cắt thành sản phẩm.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "the stages in making recycled paper".
- **Overview**: số giai đoạn, điểm đầu và điểm cuối.
- **Body 1**: giai đoạn 1 đến 4, thu gom đến khử mực.
- **Body 2**: giai đoạn 5 đến 7, tạo tờ, sấy và cắt.

## 5. Từ và cấu trúc nên dùng
- Thứ tự: first, then, next, after this, once ... has been removed, finally.
- Bị động: is collected, is mixed with, is broken down into, is washed out, is pressed, is dried.
- Mục đích: in order to remove, so that.

## 6. Lưu ý
- Giữ đúng tên các vật: pulp, screens, rollers, heated cylinders.
- Không thêm chi tiết ngoài đề, không nêu ý kiến.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002005';

-- PT-WRITING-09 Task 2 (advantages outweigh disadvantages)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Advantages and disadvantages**: học đại học hoàn toàn trực tuyến, và hỏi lợi ích có lớn hơn bất lợi hay không. Từ khóa là **entirely online**: không có lớp học trực tiếp.

## 2. Hướng triển khai
- **Lợi ích**: học ở bất cứ đâu, giờ giấc linh hoạt, tiết kiệm tiền đi lại và chỗ ở, tiếp cận trường ở nước khác, người đi làm vẫn học được.
- **Bất lợi**: thiếu giao tiếp trực tiếp và cảm giác cô lập, cần tự giác cao, khó với các môn thực hành, phụ thuộc đường truyền và thiết bị.
- Lập trường gợi ý: lợi ích nhiều hơn với người đi làm hoặc ở xa, nhưng không thay thế được hoàn toàn các ngành cần thực hành.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu rõ bên nào nhiều hơn.
- **Body 1**: các lợi ích chính kèm ví dụ.
- **Body 2**: các bất lợi chính, rồi so sánh với lợi ích.
- **Conclusion**: nhắc lại quan điểm.

## 4. Từ vựng gợi ý
- distance learning, flexible schedule, campus facilities, self-discipline, face-to-face teaching, hands-on training, internet connection, isolation.

## 5. Lưu ý
- Trả lời trực tiếp "outweigh" ngay trong Introduction.
- Ví dụ về một ngành cụ thể (y khoa cần thực hành, kinh doanh học được từ xa) sẽ thuyết phục hơn nói chung chung.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002006';

-- PT-WRITING-10 Task 1 (two maps: seaside town)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Map** (hai bản đồ): một thị trấn ven biển năm 1990 và hiện tại. Dùng thì quá khứ cho 1990, hiện tại hoàn thành hoặc hiện tại đơn cho "today", cùng thể bị động. Đề cho sẵn vị trí của từng khu vực.

## 2. Các thay đổi cần nêu
- **Phía đông**: cảng cá thành bến du thuyền có quán cà phê; chợ cá trên đường ra cảng đã đóng cửa.
- **Phía nam**: bãi đỗ xe cạnh bãi biển được thay bằng đường đi dạo ven biển và một quán cà phê bãi biển.
- **Phía bắc**: khách sạn nhỏ lớn lên thành khu nghỉ dưỡng; có thêm đường tránh chạy quanh phía bắc thị trấn.
- **Phía tây**: đất nông nghiệp thành khu nhà ở.

## 3. Gợi ý Overview
Thị trấn đổi từ nghề cá và nông nghiệp sang du lịch, nghỉ dưỡng và nhà ở: khu vực ven biển và cảng có thêm các tiện ích giải trí.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "a seaside town in 1990 and today".
- **Body 1** (3-4 câu): vùng ven biển, phía đông và phía nam: bến du thuyền, chợ cá đóng cửa, đường đi dạo thay bãi đỗ xe.
- **Body 2** (3-4 câu): phía bắc và phía tây: khách sạn thành khu nghỉ dưỡng, đường tránh mới, nhà ở thay đất nông nghiệp.

## 5. Từ và cấu trúc nên dùng
- Thay đổi: was converted into, has been replaced by, has expanded into, has closed, has been built.
- Vị trí: in the east, to the south of, along the coast, around the north.

## 6. Lưu ý
- Dùng đúng phương hướng như đề cho.
- Không giải thích lý do thị trấn thay đổi.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002007';

-- PT-WRITING-10 Task 2 (causes and solutions)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Causes and solutions** với hai câu hỏi: nguyên nhân khiến số trẻ em thừa cân tăng và cách giải quyết. Hãy trả lời đủ cả hai phần và nối giải pháp với nguyên nhân.

## 2. Hướng triển khai
- **Nguyên nhân**: ăn nhiều đồ ăn nhanh, đồ ngọt và nước có đường, ít vận động, dành nhiều thời gian trước màn hình, cha mẹ bận nên ít nấu ăn tại nhà, quảng cáo nhắm vào trẻ em.
- **Giải pháp**: bữa ăn lành mạnh và giờ thể dục ở trường, hạn chế quảng cáo đồ ăn không tốt cho trẻ, đánh thuế đồ uống có đường, giáo dục cha mẹ về dinh dưỡng, tạo sân chơi an toàn.
- Chọn hai nguyên nhân cho Body 1 và hai giải pháp tương ứng cho Body 2.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase hiện tượng và nói bài sẽ bàn nguyên nhân rồi giải pháp.
- **Body 1**: các nguyên nhân chính.
- **Body 2**: các giải pháp, nói rõ giải pháp nào xử lý nguyên nhân nào.
- **Conclusion**: tóm tắt ngắn.

## 4. Từ vựng gợi ý
- childhood obesity, sedentary lifestyle, processed food, sugary drinks, physical activity, balanced diet, advertising, school meals.

## 5. Lưu ý
- Tránh liệt kê quá nhiều ý; hai ý phát triển đầy đủ tốt hơn bốn ý sơ sài.
- Một ví dụ thực tế (một nước áp thuế đường) giúp bài thuyết phục hơn.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002008';

-- PT-WRITING-11 Task 1 (bar chart: renewable electricity)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Bar chart** có hai mốc (2000 và 2020): phần trăm điện năng quốc gia sản xuất từ nguồn tái tạo ở bốn nước. Dùng thì quá khứ và so sánh cả giữa các nước lẫn giữa hai năm.

## 2. Số liệu đáng chú ý
- **Canada** cao nhất cả hai năm (60% và 68%) nhưng tăng chậm nhất, chỉ 8 điểm phần trăm.
- **Đan Mạch** tăng mạnh nhất, từ 17% lên 62% (tăng 45 điểm phần trăm), và vượt Tây Ban Nha.
- **Tây Ban Nha** tăng từ 20% lên 44%, từ thứ hai xuống thứ ba.
- **Úc** thấp nhất cả hai năm (8% và 29%), nhưng cũng tăng hơn gấp ba.

## 3. Gợi ý Overview
Tỷ lệ điện tái tạo tăng ở cả bốn nước; Canada luôn đứng đầu, còn Đan Mạch có mức tăng lớn nhất và gần bắt kịp Canada.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "the percentage of national electricity produced from renewable sources in four countries".
- **Body 1** (3-4 câu): Canada và Đan Mạch, hai nước dẫn đầu năm 2020 (68% và 62%).
- **Body 2** (3-4 câu): Tây Ban Nha và Úc, kèm mức tăng và thứ hạng.

## 5. Từ và cấu trúc nên dùng
- the highest proportion, increased dramatically, overtook, more than tripled, a modest rise, remained in the lead.
- Đối chiếu: whereas, while, in contrast.

## 6. Lưu ý
- Chú ý hai thứ vừa so sánh: thứ hạng giữa các nước và mức tăng giữa hai năm.
- Ghi rõ đơn vị % và hai năm.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002009';

-- PT-WRITING-11 Task 2 (reasons and positive or negative)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Two-part question**: (1) lý do nhiều người làm việc nhiều giờ và có ít thời gian rảnh, (2) đây là xu hướng tích cực hay tiêu cực. Cần trả lời cả hai và kết luận rõ ràng cho câu thứ hai.

## 2. Hướng triển khai
- **Lý do**: chi phí sinh hoạt tăng nên phải làm thêm giờ, cạnh tranh và lo mất việc, công nghệ khiến công việc theo ta về nhà (email, tin nhắn), văn hóa công ty coi trọng làm việc nhiều, mong muốn thăng tiến.
- **Đánh giá**: chủ yếu tiêu cực vì căng thẳng và kiệt sức, ít thời gian cho gia đình và sức khỏe, năng suất giảm khi quá tải. Mặt tích cực chỉ là thu nhập cao hơn và cơ hội nghề nghiệp.
- Lập trường gợi ý: nhìn chung tiêu cực, kèm một ý thừa nhận lợi ích.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase hiện tượng và nói bạn sẽ bàn lý do rồi đánh giá.
- **Body 1**: hai lý do chính kèm giải thích.
- **Body 2**: đánh giá tích cực hay tiêu cực, kèm hệ quả cụ thể.
- **Conclusion**: tóm tắt lý do và quan điểm.

## 4. Từ vựng gợi ý
- long working hours, cost of living, job insecurity, work-life balance, burnout, overtime, productivity, always connected.

## 5. Lưu ý
- Câu hỏi hai cần câu trả lời trực tiếp ("it is a negative development").
- Giữ ví dụ cụ thể về một ngành hoặc một nước nổi tiếng với giờ làm dài.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002010';

-- PT-WRITING-12 Task 1 (two pie charts: household waste)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Pie charts** (hai biểu đồ tròn): so sánh cách xử lý rác thải sinh hoạt của một thành phố năm 2000 và 2020. Mỗi biểu đồ cộng lại 100%, bốn phương pháp: chôn lấp, tái chế, đốt và ủ phân. Dùng thì quá khứ.

## 2. Số liệu đáng chú ý
- **Chôn lấp** giảm mạnh nhất, từ 70% xuống 28% (giảm 42 điểm phần trăm): từ phương pháp chiếm phần lớn thành thứ hai.
- **Tái chế** tăng từ 15% lên 42% và trở thành phương pháp phổ biến nhất năm 2020.
- **Ủ phân** tăng từ 5% lên 18%, gấp hơn ba lần.
- **Đốt** gần như không đổi: 10% rồi 12%.

## 3. Gợi ý Overview
Thành phố chuyển từ chủ yếu chôn lấp sang tái chế là chính; ủ phân tăng, còn việc đốt gần như giữ nguyên.

## 4. Cách chia đoạn
- **Introduction**: paraphrase "how the household waste of one city was handled in 2000 and in 2020".
- **Body 1** (3-4 câu): chôn lấp và tái chế, hai phương pháp thay đổi nhiều nhất và đổi chỗ cho nhau.
- **Body 2** (3-4 câu): ủ phân và đốt, nói tăng nhiều ở ủ phân so với gần như không đổi ở đốt.

## 5. Từ và cấu trúc nên dùng
- accounted for, the dominant method, fell dramatically, overtook, more than tripled, remained broadly unchanged.
- Đối chiếu: whereas, in contrast.

## 6. Lưu ý
- Nêu rõ hai năm và đơn vị phần trăm.
- Chú ý sự đổi ngôi: chôn lấp từ đứng đầu xuống thứ hai, tái chế từ thứ hai lên đứng đầu.
- Viết tối thiểu 150 từ trong khoảng 20 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002011';

-- PT-WRITING-12 Task 2 (agree / disagree)
UPDATE question_versions SET guide = '## 1. Giải thích đề
Đề thuộc dạng **Agree / Disagree**: robot và tự động hóa sẽ **thay thế hầu hết công việc**, khiến nhiều người thất nghiệp. Chú ý "most jobs": đây là nhận định mạnh, bạn có thể phản biện mức độ này.

## 2. Hướng triển khai
- **Đồng ý**: công việc lặp lại trong nhà máy, kho bãi, văn phòng đang được máy móc và AI làm thay, doanh nghiệp muốn giảm chi phí và sai sót.
- **Không đồng ý hoàn toàn**: tự động hóa tạo ra nghề mới (lập trình, bảo trì), con người vẫn cần cho sáng tạo, đồng cảm và ra quyết định, lịch sử cho thấy cách mạng công nghiệp thay đổi nghề nghiệp chứ không xóa hết.
- Lập trường gợi ý: đồng ý một phần, tự động hóa làm mất nhiều việc nhưng không phải hầu hết, nên cần đào tạo lại lao động.

## 3. Bố cục 4 đoạn
- **Introduction**: paraphrase đề và nêu quan điểm.
- **Body 1**: các công việc dễ bị thay thế và lý do.
- **Body 2**: vì sao không phải hầu hết, kèm ví dụ nghề mới hoặc kỹ năng của con người.
- **Conclusion**: nhắc lại quan điểm.

## 4. Từ vựng gợi ý
- automation, artificial intelligence, repetitive tasks, job losses, unemployment, retraining, human skills, workforce.

## 5. Lưu ý
- Phân biệt "thay thế một số công việc" và "thay thế hầu hết công việc" để lập luận chặt hơn.
- Dùng ví dụ cụ thể như robot trong nhà máy ô tô hoặc quầy tự thanh toán.
- Viết tối thiểu 250 từ trong khoảng 40 phút.'
WHERE id = '2d000000-0000-4000-8000-040000002012';
