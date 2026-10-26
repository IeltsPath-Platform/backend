-- Demo content authored by AI; requires review by an IELTS examiner before production.
-- Enriches 48 more words that already exist in the V4 catalogue (8 per topic): CEFR, topic, register, US IPA,
-- collocations, a synonym, a common mistake and two examples. No new words are created.
-- frequency_rank stays NULL (no licensed COCA source). academic_word_list is set only for words in Coxhead (2000).
-- Rows are skipped when the sense is already enriched, so re-running or admin edits are never overwritten.

CREATE TEMP TABLE catalog_more (
    lemma text, cefr text, topic_slug text, register text, ipa_us text, awl boolean, synonym text,
    coll1 text, coll1_vi text, coll2 text, coll2_vi text,
    wrong text, correct text, explanation text,
    ex1 text, ex1_vi text, ex2 text, ex2_vi text
) ON COMMIT DROP;

INSERT INTO catalog_more VALUES
-- Education
('tuition','B2','education','formal','/tuˈɪʃn/',false,'fees','tuition fees','học phí','private tuition','dạy kèm riêng','tuition fees is','tuition fees are','Tuition fees is plural here and takes a plural verb.','Tuition fees rose again this year.','Học phí lại tăng trong năm nay.','She pays for private tuition in mathematics.','Cô ấy trả tiền học kèm môn toán.'),
('scholarship','B2','education','formal','/ˈskɑːlərʃɪp/',false,'grant','win a scholarship','giành học bổng','scholarship holder','người nhận học bổng','take a scholarship','win a scholarship','Win or receive is the natural verb with scholarship.','He won a scholarship to study abroad.','Anh ấy giành học bổng đi du học.','The scholarship covers all living costs.','Học bổng chi trả toàn bộ chi phí sinh hoạt.'),
('literacy','C1','education','academic','/ˈlɪtərəsi/',false,'reading ability','adult literacy','xóa mù chữ cho người lớn','literacy rate','tỷ lệ biết chữ','literacy are','literacy is','Literacy is uncountable and takes a singular verb.','The literacy rate has risen steadily.','Tỷ lệ biết chữ đã tăng đều đặn.','Digital literacy is now a key skill.','Kỹ năng số hiện là một kỹ năng then chốt.'),
('lecture','B1','education','general','/ˈlektʃər/',false,'talk','attend a lecture','tham dự bài giảng','guest lecture','buổi giảng của khách mời','make a lecture','give a lecture','The verb that collocates with lecture is give.','The lecture lasted ninety minutes.','Bài giảng kéo dài chín mươi phút.','Students take notes during the lecture.','Sinh viên ghi chép trong lúc nghe giảng.'),
('assignment','B1','education','general','/əˈsaɪnmənt/',true,'task','hand in an assignment','nộp bài tập','written assignment','bài tập viết','give in an assignment','hand in an assignment','Hand in means to submit work to a teacher.','The assignment is due on Friday.','Bài tập phải nộp vào thứ Sáu.','Each assignment is marked within a week.','Mỗi bài tập được chấm trong vòng một tuần.'),
('deadline','B1','education','general','/ˈdedlaɪn/',false,'time limit','meet a deadline','kịp thời hạn','tight deadline','thời hạn gấp','pass the deadline','miss the deadline','Use miss, not pass, for failing to meet a deadline.','The deadline for applications is in May.','Hạn nộp hồ sơ là vào tháng Năm.','They worked overnight to meet the deadline.','Họ làm việc suốt đêm để kịp thời hạn.'),
('syllabus','C1','education','academic','/ˈsɪləbəs/',false,'course outline','follow the syllabus','theo đề cương môn học','cover the syllabus','học hết đề cương','syllabuses are','syllabuses or syllabi are','Both syllabuses and syllabi are accepted plurals.','The syllabus lists all reading materials.','Đề cương liệt kê toàn bộ tài liệu đọc.','The teacher explained the syllabus on the first day.','Giáo viên giải thích đề cương vào ngày đầu.'),
('plagiarism','C1','education','academic','/ˈpleɪdʒərɪzəm/',false,'copying','commit plagiarism','đạo văn','detect plagiarism','phát hiện đạo văn','a plagiarism','plagiarism','Plagiarism is uncountable; do not use a before it.','Plagiarism can lead to a failed course.','Đạo văn có thể khiến trượt môn học.','The software checks essays for plagiarism.','Phần mềm kiểm tra đạo văn trong bài luận.'),
-- Environment
('climate','B1','environment','general','/ˈklaɪmət/',false,'weather pattern','climate change','biến đổi khí hậu','tropical climate','khí hậu nhiệt đới','climate are','climate is','Climate is singular in this meaning.','Vietnam has a tropical climate.','Việt Nam có khí hậu nhiệt đới.','Climate change threatens coastal cities.','Biến đổi khí hậu đe dọa các thành phố ven biển.'),
('emission','B2','environment','academic','/ɪˈmɪʃn/',false,'discharge','reduce emissions','giảm phát thải','carbon emissions','phát thải carbon','emission of cars','emissions from cars','Use emissions from with a source of the gas.','Factories must cut their emissions.','Các nhà máy phải cắt giảm phát thải.','Transport produces a large share of emissions.','Giao thông tạo ra một phần lớn lượng phát thải.'),
('habitat','B2','environment','academic','/ˈhæbɪtæt/',false,'natural home','natural habitat','môi trường sống tự nhiên','destroy a habitat','phá hủy môi trường sống','habitat of tigers are','the habitat of tigers is','The subject habitat is singular.','Forests are the habitat of many species.','Rừng là môi trường sống của nhiều loài.','Roads can break up wildlife habitats.','Đường sá có thể chia cắt môi trường sống của động vật hoang dã.'),
('deforestation','C1','environment','academic','/diːˌfɔːrɪˈsteɪʃn/',false,'forest clearance','combat deforestation','chống nạn phá rừng','rapid deforestation','nạn phá rừng nhanh chóng','a deforestation','deforestation','Deforestation is uncountable.','Deforestation destroys animal habitats.','Phá rừng hủy hoại môi trường sống của động vật.','Farming is a major cause of deforestation.','Nông nghiệp là nguyên nhân lớn của nạn phá rừng.'),
('drought','B2','environment','general','/draʊt/',false,'dry spell','severe drought','hạn hán nghiêm trọng','drought conditions','tình trạng hạn hán','a heavy drought','a severe drought','Severe or serious collocates with drought; heavy is used for rain.','The region suffered a long drought.','Khu vực chịu một đợt hạn hán dài.','Drought reduced the rice harvest.','Hạn hán làm giảm vụ thu hoạch lúa.'),
('recycle','A2','environment','general','/ˌriːˈsaɪkl/',false,'reuse','recycle paper','tái chế giấy','recycle waste','tái chế rác thải','recycle again the bottles','recycle the bottles','Recycle already means to process for reuse, so again is redundant.','We recycle glass and plastic at home.','Chúng tôi tái chế thủy tinh và nhựa ở nhà.','Many cities encourage people to recycle.','Nhiều thành phố khuyến khích người dân tái chế.'),
('landfill','C1','environment','technical','/ˈlændfɪl/',false,'waste dump','landfill site','bãi chôn lấp','send to landfill','đưa đi chôn lấp','landfills site','landfill site','Landfill is used as a noun modifier in singular form.','Most household waste goes to landfill.','Phần lớn rác thải sinh hoạt đi vào bãi chôn lấp.','The landfill site is nearly full.','Bãi chôn lấp gần đầy.'),
('erosion','C1','environment','technical','/ɪˈroʊʒn/',false,'wearing away','soil erosion','xói mòn đất','coastal erosion','xói lở bờ biển','erosions of soil','erosion of soil','Erosion is normally uncountable in this meaning.','Heavy rain causes soil erosion.','Mưa lớn gây xói mòn đất.','Trees help prevent erosion on hillsides.','Cây cối giúp ngăn xói mòn trên sườn đồi.'),
-- Technology
('innovation','B2','technology','formal','/ˌɪnəˈveɪʃn/',false,'new idea','technological innovation','đổi mới công nghệ','encourage innovation','khuyến khích đổi mới','innovation are','innovation is','Innovation is uncountable in general use.','Innovation drives economic growth.','Đổi mới thúc đẩy tăng trưởng kinh tế.','The firm is known for product innovation.','Công ty nổi tiếng về đổi mới sản phẩm.'),
('network','B1','technology','general','/ˈnetwɜːrk/',true,'system','computer network','mạng máy tính','social network','mạng xã hội','a network are','a network is','A singular noun takes a singular verb.','The school network was down this morning.','Mạng của trường bị sập sáng nay.','She built a network of contacts.','Cô ấy xây dựng một mạng lưới quan hệ.'),
('digital','B1','technology','general','/ˈdɪdʒɪtl/',false,'electronic','digital technology','công nghệ số','digital skills','kỹ năng số','digitals devices','digital devices','Adjectives in English do not take a plural form.','Digital skills are useful in most jobs.','Kỹ năng số hữu ích trong hầu hết công việc.','The library has a large digital collection.','Thư viện có bộ sưu tập số lớn.'),
('privacy','B2','technology','formal','/ˈpraɪvəsi/',false,'confidentiality','protect privacy','bảo vệ quyền riêng tư','invasion of privacy','xâm phạm quyền riêng tư','a privacy','privacy','Privacy is uncountable.','Many users worry about online privacy.','Nhiều người dùng lo lắng về quyền riêng tư trực tuyến.','The law protects personal privacy.','Luật bảo vệ quyền riêng tư cá nhân.'),
('database','B2','technology','technical','/ˈdeɪtəbeɪs/',false,'data store','update a database','cập nhật cơ sở dữ liệu','customer database','cơ sở dữ liệu khách hàng','a database are','a database is','A singular noun takes a singular verb.','The database stores millions of records.','Cơ sở dữ liệu lưu hàng triệu bản ghi.','Staff can search the database by name.','Nhân viên có thể tìm trong cơ sở dữ liệu theo tên.'),
('download','A2','technology','general','/ˈdaʊnloʊd/',false,'save','download a file','tải xuống một tệp','free download','bản tải miễn phí','download the file in internet','download the file from the internet','A file is downloaded from a source, and internet takes the.','You can download the app for free.','Bạn có thể tải ứng dụng miễn phí.','The download took only a few seconds.','Việc tải xuống chỉ mất vài giây.'),
('automation','C1','technology','academic','/ˌɔːtəˈmeɪʃn/',false,'mechanisation','industrial automation','tự động hóa công nghiệp','job automation','tự động hóa công việc','automations are','automation is','Automation is uncountable in general use.','Automation may replace routine jobs.','Tự động hóa có thể thay thế các công việc lặp đi lặp lại.','The factory relies heavily on automation.','Nhà máy dựa nhiều vào tự động hóa.'),
('encryption','C1','technology','technical','/ɪnˈkrɪpʃn/',false,'coding','data encryption','mã hóa dữ liệu','strong encryption','mã hóa mạnh','an encryption are','encryption is','Encryption is uncountable in general use.','Encryption protects messages from strangers.','Mã hóa bảo vệ tin nhắn khỏi người lạ.','The bank uses strong encryption.','Ngân hàng dùng mã hóa mạnh.'),
-- Health
('obesity','B2','health','formal','/oʊˈbiːsəti/',false,'overweight','childhood obesity','béo phì ở trẻ em','obesity rate','tỷ lệ béo phì','a obesity','obesity','Obesity is uncountable; do not use an article.','Obesity increases the risk of heart disease.','Béo phì làm tăng nguy cơ bệnh tim.','Obesity rates have doubled in thirty years.','Tỷ lệ béo phì đã tăng gấp đôi trong ba mươi năm.'),
('nutrition','B2','health','formal','/nuˈtrɪʃn/',false,'nourishment','good nutrition','dinh dưỡng tốt','nutrition label','nhãn dinh dưỡng','nutritions are','nutrition is','Nutrition is uncountable.','Good nutrition helps children grow.','Dinh dưỡng tốt giúp trẻ em phát triển.','The label shows nutrition information.','Nhãn hiển thị thông tin dinh dưỡng.'),
('symptom','B2','health','formal','/ˈsɪmptəm/',false,'sign','show symptoms','có triệu chứng','early symptom','triệu chứng sớm','symptom are','symptoms are','Use the plural form when referring to more than one.','Fever is a common symptom of flu.','Sốt là triệu chứng phổ biến của cúm.','Patients reported mild symptoms.','Bệnh nhân báo cáo triệu chứng nhẹ.'),
('vaccine','B2','health','formal','/vækˈsiːn/',false,'immunisation','develop a vaccine','phát triển vắc xin','flu vaccine','vắc xin cúm','take a vaccine','receive a vaccine','Patients receive or get a vaccine.','The vaccine protects against measles.','Vắc xin bảo vệ khỏi bệnh sởi.','Children receive the vaccine at school.','Trẻ em được tiêm vắc xin ở trường.'),
('prescription','B2','health','formal','/prɪˈskrɪpʃn/',false,'medical order','on prescription','theo đơn thuốc','repeat prescription','đơn thuốc lặp lại','write a prescription to antibiotics','write a prescription for antibiotics','A prescription is written for a medicine, not to it.','The drug is only available on prescription.','Thuốc này chỉ bán theo đơn.','The doctor wrote a prescription for antibiotics.','Bác sĩ kê đơn thuốc kháng sinh.'),
('hygiene','B2','health','formal','/ˈhaɪdʒiːn/',false,'cleanliness','personal hygiene','vệ sinh cá nhân','hygiene standards','tiêu chuẩn vệ sinh','a hygiene','good hygiene','Hygiene is uncountable.','Good hygiene prevents many infections.','Vệ sinh tốt ngăn nhiều bệnh nhiễm trùng.','Restaurants must follow strict hygiene rules.','Nhà hàng phải tuân thủ quy định vệ sinh nghiêm ngặt.'),
('anxiety','B2','health','general','/æŋˈzaɪəti/',false,'worry','suffer from anxiety','bị lo âu','anxiety disorder','rối loạn lo âu','many anxiety','much anxiety','Anxiety is uncountable in this meaning.','Exams can cause anxiety in students.','Kỳ thi có thể gây lo âu cho học sinh.','She sought help for her anxiety.','Cô ấy tìm sự giúp đỡ cho chứng lo âu.'),
('chronic','C1','health','technical','/ˈkrɑːnɪk/',false,'long-term','chronic illness','bệnh mãn tính','chronic pain','đau mãn tính','a chronic cold that lasts one day','an acute cold that lasts one day','Chronic means long-lasting; use acute for a short, sudden illness.','Chronic diseases need long-term care.','Bệnh mãn tính cần chăm sóc lâu dài.','He lives with chronic back pain.','Anh ấy sống với chứng đau lưng mãn tính.'),
-- Work and business
('income','B1','work-business','formal','/ˈɪnkʌm/',true,'earnings','average income','thu nhập trung bình','low income','thu nhập thấp','a high income are','a high income is','A singular noun takes a singular verb.','Average income varies between regions.','Thu nhập trung bình khác nhau giữa các vùng.','Rent takes a third of her income.','Tiền thuê nhà chiếm một phần ba thu nhập của cô ấy.'),
('wage','B1','work-business','general','/weɪdʒ/',false,'pay','minimum wage','mức lương tối thiểu','hourly wage','lương theo giờ','my wage is paid yearly','my salary is paid yearly','Salary is the usual word for fixed monthly or yearly pay; wage is often hourly or weekly.','The minimum wage will rise next year.','Mức lương tối thiểu sẽ tăng vào năm tới.','Workers demanded a fair wage.','Công nhân đòi mức lương công bằng.'),
('profit','B1','work-business','general','/ˈprɑːfɪt/',false,'gain','make a profit','kiếm lợi nhuận','profit margin','biên lợi nhuận','do a profit','make a profit','Make is the usual verb with profit.','The company made a large profit.','Công ty kiếm được lợi nhuận lớn.','Profit fell after costs increased.','Lợi nhuận giảm sau khi chi phí tăng.'),
('revenue','C1','work-business','formal','/ˈrevənuː/',false,'turnover','annual revenue','doanh thu hằng năm','generate revenue','tạo doanh thu','The revenue was 5 million after costs.','The profit was 5 million after costs.','Revenue is income before costs; profit is what remains after costs.','Online sales boosted annual revenue.','Bán hàng trực tuyến làm tăng doanh thu hằng năm.','Tourism generates revenue for the region.','Du lịch tạo doanh thu cho khu vực.'),
('unemployment','B2','work-business','formal','/ˌʌnɪmˈplɔɪmənt/',false,'joblessness','high unemployment','thất nghiệp cao','unemployment rate','tỷ lệ thất nghiệp','unemployments are','unemployment is','Unemployment is uncountable.','Youth unemployment remains a concern.','Thất nghiệp ở người trẻ vẫn là mối lo.','The unemployment rate fell to five percent.','Tỷ lệ thất nghiệp giảm xuống năm phần trăm.'),
('promotion','B2','work-business','general','/prəˈmoʊʃn/',false,'advancement','get a promotion','được thăng chức','sales promotion','khuyến mại','take a promotion','get a promotion','Get or receive a promotion is the natural phrase.','She got a promotion after two years.','Cô ấy được thăng chức sau hai năm.','The shop is running a special promotion.','Cửa hàng đang có chương trình khuyến mại đặc biệt.'),
('colleague','B1','work-business','general','/ˈkɑːliːɡ/',false,'co-worker','work colleague','đồng nghiệp','helpful colleague','đồng nghiệp hay giúp đỡ','colleague of mine are','a colleague of mine is','The subject a colleague is singular.','My colleague shared the report with me.','Đồng nghiệp của tôi chia sẻ báo cáo với tôi.','Colleagues often have lunch together.','Đồng nghiệp thường ăn trưa cùng nhau.'),
('productivity','C1','work-business','formal','/ˌproʊdʌkˈtɪvəti/',false,'efficiency','boost productivity','tăng năng suất','labour productivity','năng suất lao động','productivities are','productivity is','Productivity is uncountable in general use.','Flexible hours can boost productivity.','Giờ làm linh hoạt có thể tăng năng suất.','Productivity grew by two percent last year.','Năng suất tăng hai phần trăm năm ngoái.'),
-- Society
('poverty','B2','society','formal','/ˈpɑːvərti/',false,'hardship','live in poverty','sống trong nghèo đói','reduce poverty','giảm nghèo','a poverty','poverty','Poverty is uncountable.','Poverty affects children most severely.','Nghèo đói ảnh hưởng nặng nhất đến trẻ em.','The programme aims to reduce poverty.','Chương trình nhằm giảm nghèo.'),
('inequality','C1','society','academic','/ˌɪnɪˈkwɑːləti/',false,'disparity','income inequality','bất bình đẳng thu nhập','reduce inequality','giảm bất bình đẳng','inequality between rich and poor are','inequality between rich and poor is','The subject inequality is singular.','Inequality has grown in many countries.','Bất bình đẳng gia tăng ở nhiều quốc gia.','Education can reduce inequality.','Giáo dục có thể giảm bất bình đẳng.'),
('population','B1','society','general','/ˌpɑːpjuˈleɪʃn/',false,'inhabitants','growing population','dân số gia tăng','population density','mật độ dân số','the population are','the population is','Population takes a singular verb when it means the total number.','The population of the city is two million.','Dân số thành phố là hai triệu người.','An ageing population needs more care services.','Dân số già hóa cần thêm dịch vụ chăm sóc.'),
('tradition','B1','society','general','/trəˈdɪʃn/',false,'custom','family tradition','truyền thống gia đình','follow a tradition','theo một truyền thống','a tradition are','a tradition is','A singular noun takes a singular verb.','Tet is an important Vietnamese tradition.','Tết là một truyền thống quan trọng của Việt Nam.','Many young people still respect tradition.','Nhiều bạn trẻ vẫn tôn trọng truyền thống.'),
('crime','B1','society','general','/kraɪm/',false,'offence','commit a crime','phạm tội','crime rate','tỷ lệ tội phạm','do a crime','commit a crime','Commit is the verb used with crime.','Crime fell in the city centre.','Tội phạm giảm ở trung tâm thành phố.','The police are working to prevent crime.','Cảnh sát đang nỗ lực phòng chống tội phạm.'),
('diversity','B2','society','formal','/daɪˈvɜːrsəti/',true,'variety','cultural diversity','đa dạng văn hóa','promote diversity','thúc đẩy đa dạng','diversities are','diversity is','Diversity is uncountable in general use.','The school celebrates cultural diversity.','Trường tôn vinh sự đa dạng văn hóa.','Diversity in the workplace improves ideas.','Sự đa dạng ở nơi làm việc cải thiện ý tưởng.'),
('volunteer','B1','society','general','/ˌvɑːlənˈtɪr/',false,'helper','work as a volunteer','làm tình nguyện viên','volunteer work','công việc tình nguyện','volunteer in the charity work','volunteer for the charity','Volunteer for an organisation, not in it.','She volunteers at a local shelter.','Cô ấy làm tình nguyện tại một mái ấm địa phương.','The event needs more volunteers.','Sự kiện cần thêm tình nguyện viên.'),
('heritage','B2','society','formal','/ˈherɪtɪdʒ/',false,'legacy','cultural heritage','di sản văn hóa','heritage site','di tích di sản','a heritage are','heritage is','Heritage is uncountable in this meaning.','Hoi An is a world heritage site.','Hội An là một di sản thế giới.','The festival celebrates local heritage.','Lễ hội tôn vinh di sản địa phương.');

-- Resolve each word to its first active sense; skip senses that were already enriched.
CREATE TEMP TABLE catalog_more_target ON COMMIT DROP AS
SELECT m.*, i.id AS item_id, s.id AS sense_id
FROM catalog_more m
JOIN vocabulary_items i ON i.normalized_lemma = m.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id = i.id AND s.status = 'ACTIVE'
WHERE s.id = (SELECT sx.id FROM vocabulary_senses sx
              WHERE sx.vocabulary_item_id = i.id AND sx.status = 'ACTIVE' ORDER BY sx.sort_order, sx.id LIMIT 1)
  AND s.cefr_level IS NULL;

UPDATE vocabulary_items i SET
    ipa_uk = COALESCE(i.ipa_uk, i.ipa),
    ipa_us = COALESCE(i.ipa_us, t.ipa_us),
    academic_word_list = i.academic_word_list OR t.awl
FROM catalog_more_target t WHERE i.id = t.item_id;

UPDATE vocabulary_senses s SET
    cefr_level = t.cefr, topic_id = tp.id, register = t.register,
    example_translation = COALESCE(s.example_translation, t.ex1_vi)
FROM catalog_more_target t JOIN vocabulary_topics tp ON tp.slug = t.topic_slug
WHERE s.id = t.sense_id;

INSERT INTO vocabulary_collocations(sense_id, phrase, meaning_vi, sort_order)
SELECT sense_id, coll1, coll1_vi, 1 FROM catalog_more_target
UNION ALL SELECT sense_id, coll2, coll2_vi, 2 FROM catalog_more_target;

INSERT INTO vocabulary_relations(sense_id, related_lemma, kind, sort_order)
SELECT sense_id, synonym, 'SYNONYM', 1 FROM catalog_more_target;

INSERT INTO vocabulary_common_mistakes(sense_id, wrong_usage, correct_usage, explanation, sort_order)
SELECT sense_id, wrong, correct, explanation, 1 FROM catalog_more_target;

INSERT INTO vocabulary_examples(sense_id, sentence, translation_vi, sort_order)
SELECT sense_id, ex1, ex1_vi, 1 FROM catalog_more_target
UNION ALL SELECT sense_id, ex2, ex2_vi, 2 FROM catalog_more_target;

-- Add the newly enriched senses to the existing topic and band sets (after the current last position).
INSERT INTO vocabulary_set_items(set_id, sense_id, sort_order)
SELECT st.id, t.sense_id,
       COALESCE((SELECT MAX(x.sort_order) FROM vocabulary_set_items x WHERE x.set_id = st.id), 0)
           + row_number() OVER (PARTITION BY st.id ORDER BY t.lemma)
FROM catalog_more_target t
JOIN vocabulary_topics tp ON tp.slug = t.topic_slug
JOIN vocabulary_sets st ON (st.kind = 'TOPIC' AND st.topic_id = tp.id) OR (st.kind = 'BAND' AND st.cefr_level = t.cefr)
ON CONFLICT DO NOTHING;
