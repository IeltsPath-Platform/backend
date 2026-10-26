-- Demo content authored by AI; requires review by an IELTS examiner before production.
INSERT INTO vocabulary_topics (id, slug, name, name_vi, sort_order) VALUES
('c1000000-0000-4000-8000-000000000001','education','Education','Giáo dục',1),
('c1000000-0000-4000-8000-000000000002','environment','Environment','Môi trường',2),
('c1000000-0000-4000-8000-000000000003','technology','Technology','Công nghệ',3),
('c1000000-0000-4000-8000-000000000004','health','Health','Sức khỏe',4),
('c1000000-0000-4000-8000-000000000005','work-business','Work & Business','Công việc và kinh doanh',5),
('c1000000-0000-4000-8000-000000000006','society','Society','Xã hội',6)
ON CONFLICT (slug) DO NOTHING;

UPDATE vocabulary_items SET
    ipa_uk = COALESCE(ipa_uk, ipa),
    academic_word_list = academic_word_list OR normalized_lemma IN ('research','method','data')
WHERE normalized_lemma IN ('research','method','data');

WITH seed(lemma, cefr, topic_slug, register, usage_note, translation) AS (VALUES
('pupil','A1','education','general','Pupil is commonly used for a child who attends a school.','Học sinh cần nộp bài trước hạn.'),
('curriculum','B2','education','academic','A curriculum is the full set of subjects taught by a school or programme.','Chương trình học được cập nhật mỗi năm.'),
('research','B2','education','academic','Research is usually uncountable when referring to the activity.','Nghiên cứu cho thấy kết quả đáng khích lệ.'),
('pollution','B1','environment','academic','Use pollution as an uncountable noun.','Ô nhiễm không khí ảnh hưởng đến sức khỏe.'),
('renewable','B2','environment','formal','Often used before energy, source, or resource.','Năng lượng tái tạo có thể giảm phát thải.'),
('sustainable','B2','environment','formal','Commonly used with development, growth, or practices.','Thành phố cần tìm giải pháp phát triển bền vững.'),
('device','A2','technology','general','Use a device for a piece of equipment designed for a particular purpose.','Thiết bị này giúp người học luyện phát âm.'),
('algorithm','C1','technology','technical','An algorithm is a set of rules used by a computer to solve a problem.','Thuật toán sắp xếp kết quả theo mức độ liên quan.'),
('software','B1','technology','technical','Software is generally uncountable; use a piece of software for one product.','Phần mềm mới hỗ trợ học từ vựng.'),
('diet','A2','health','general','A diet is the usual food someone eats; it does not always mean weight loss.','Chế độ ăn cân bằng cung cấp đủ năng lượng.'),
('stress','B1','health','general','Stress is usually uncountable when referring to mental pressure.','Căng thẳng kéo dài có thể ảnh hưởng đến giấc ngủ.'),
('salary','B1','work-business','formal','Salary is commonly used for regular fixed pay, often monthly or yearly.','Mức lương phụ thuộc vào kinh nghiệm.'),
('community','B1','society','general','Community can refer to people living in one place or sharing an interest.','Cộng đồng địa phương tổ chức một sự kiện.'),
('policy','B2','society','formal','Policy is a set of principles or plans adopted by an organisation or government.','Chính sách mới khuyến khích giao thông công cộng.'),
('method','B1','society','academic','Method is a countable noun; specify a method or methods.','Phương pháp này giúp tiết kiệm thời gian.'),
('dissertation','C2','education','academic','A dissertation is a long piece of original research written for an academic degree.','Luận văn trình bày quá trình nghiên cứu độc lập.')
)
UPDATE vocabulary_senses s SET cefr_level = seed.cefr, topic_id = t.id, register = seed.register,
    usage_note = seed.usage_note
FROM seed JOIN vocabulary_topics t ON t.slug = seed.topic_slug
JOIN vocabulary_items i ON i.normalized_lemma = seed.lemma
WHERE s.vocabulary_item_id = i.id AND s.status = 'ACTIVE'
  AND s.id = (SELECT s2.id FROM vocabulary_senses s2 WHERE s2.vocabulary_item_id = i.id AND s2.status = 'ACTIVE' ORDER BY s2.sort_order, s2.id LIMIT 1);

WITH details(lemma, collocation, meaning, example, synonym, antonym, wrong, correct, explanation, sentence, translation, n) AS (VALUES
('pupil','school pupil','học sinh','The pupil joined an after-school science club.','learner',NULL,'He is pupil.','He is a pupil.','A singular countable noun needs an article.','The pupil joined a science club.','Học sinh tham gia câu lạc bộ khoa học.',1),
('curriculum','school curriculum','chương trình học ở trường','The school curriculum includes science.','syllabus',NULL,'curriculum are','the curriculum is','Curriculum is singular; curricula is the plural form.','The curriculum includes practical projects.','Chương trình học bao gồm các dự án thực hành.',1),
('research','conduct research','tiến hành nghiên cứu','They conduct research on language learning.','study',NULL,'a research','some research','Research is normally uncountable in this meaning.','The research was published last year.','Nghiên cứu được công bố vào năm ngoái.',1),
('pollution','air pollution','ô nhiễm không khí','Air pollution is a serious issue.','contamination',NULL,'many pollution','much pollution','Pollution is uncountable in general use.','Air pollution can harm children.','Ô nhiễm không khí có thể gây hại cho trẻ em.',1),
('renewable','renewable energy','năng lượng tái tạo','The country invests in renewable energy.','replenishable',NULL,'renewable energies','renewable energy','Energy is uncountable in this general phrase.','Renewable energy can reduce reliance on coal.','Năng lượng tái tạo có thể giảm phụ thuộc vào than.',1),
('sustainable','sustainable development','phát triển bền vững','The plan supports sustainable development.','viable',NULL,'sustain development','sustainable development','Sustainable is the adjective form.','Sustainable transport benefits urban residents.','Giao thông bền vững có lợi cho cư dân đô thị.',1),
('device','mobile device','thiết bị di động','A mobile device can run the app.','gadget',NULL,'many device','many devices','Use the plural after many.','The device stores new vocabulary.','Thiết bị lưu từ vựng mới.',1),
('algorithm','search algorithm','thuật toán tìm kiếm','The search algorithm ranks useful pages.','procedure',NULL,'an algorithm are','an algorithm is','Algorithm is singular and takes a singular verb.','An algorithm can process large data sets.','Thuật toán có thể xử lý tập dữ liệu lớn.',1),
('software','educational software','phần mềm giáo dục','The school uses educational software.','program',NULL,'a software','some software','Software is uncountable; say a software package if needed.','The software supports pronunciation practice.','Phần mềm hỗ trợ luyện phát âm.',1),
('diet','balanced diet','chế độ ăn cân bằng','A balanced diet provides essential nutrients.','food plan',NULL,'a healthy diet are','a healthy diet is','Diet is singular and takes a singular verb.','A varied diet can support good health.','Chế độ ăn đa dạng có thể hỗ trợ sức khỏe.',1),
('stress','reduce stress','giảm căng thẳng','Short breaks may reduce stress.','pressure',NULL,'many stress','much stress','Stress is uncountable in this meaning.','Good sleep helps people manage stress.','Ngủ đủ giúp mọi người kiểm soát căng thẳng.',1),
('salary','annual salary','lương hằng năm','The role offers a competitive annual salary.','pay',NULL,'earn a salary of 20,000 dollars per month','earn a monthly salary of 20,000 dollars','Use monthly or annual consistently with the amount.','Her salary increased after promotion.','Mức lương của cô tăng sau khi được thăng chức.',1),
('community','local community','cộng đồng địa phương','The local community protects the park.','society',NULL,'people in community','people in the community','Use the definite article when referring to a specific community.','The community organised a clean-up day.','Cộng đồng tổ chức ngày dọn vệ sinh.',1),
('policy','government policy','chính sách của chính phủ','The government policy supports public transport.','strategy',NULL,'a policy which encourage','a policy which encourages','A singular policy takes a singular verb.','The policy aims to improve access to education.','Chính sách nhằm cải thiện khả năng tiếp cận giáo dục.',1),
('method','research method','phương pháp nghiên cứu','The researchers used a new research method.','approach',NULL,'a effective method','an effective method','Use an before a vowel sound.','This method produced consistent results.','Phương pháp này tạo ra kết quả nhất quán.',1),
('dissertation','doctoral dissertation','luận văn tiến sĩ','She submitted her doctoral dissertation in May.','thesis',NULL,'write a dissertation about the topic','write a dissertation on the topic','Write a dissertation on a subject or topic.','The dissertation examines language learning.','Luận văn phân tích việc học ngôn ngữ.',1)
)
INSERT INTO vocabulary_collocations(sense_id, phrase, meaning_vi, example, sort_order)
SELECT s.id, d.collocation, d.meaning, d.example, 1 FROM details d JOIN vocabulary_items i ON i.normalized_lemma=d.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);

WITH details(lemma, collocation, meaning, example, synonym, wrong, correct, explanation, sentence, translation) AS (VALUES
('pupil','school pupil','học sinh','The school pupil borrowed a book.','learner','He is pupil.','He is a pupil.','A singular countable noun needs an article.','Pupils learn from one another.','Học sinh học hỏi lẫn nhau.'),
('curriculum','national curriculum','chương trình học quốc gia','The national curriculum sets standards.','syllabus','curriculum are','the curriculum is','Curriculum is singular; curricula is plural.','Schools review the curriculum regularly.','Các trường thường xuyên rà soát chương trình học.'),
('research','research finding','phát hiện nghiên cứu','The research finding was surprising.','investigation','a research','some research','Research is normally uncountable.','Researchers shared their findings.','Các nhà nghiên cứu chia sẻ phát hiện.'),
('pollution','noise pollution','ô nhiễm tiếng ồn','Noise pollution affects residents.','contamination','many pollution','much pollution','Pollution is uncountable.','Local action can reduce pollution.','Hành động địa phương có thể giảm ô nhiễm.'),
('renewable','renewable source','nguồn tái tạo','Wind is a renewable source of energy.','replenishable','renewable energies','renewable energy','Energy is uncountable in general use.','The region has several renewable sources.','Khu vực có một số nguồn tái tạo.'),
('sustainable','sustainable growth','tăng trưởng bền vững','The policy encourages sustainable growth.','viable','sustain growth','sustainable growth','Use the adjective sustainable before a noun.','The project promotes sustainable farming.','Dự án thúc đẩy canh tác bền vững.'),
('device','digital device','thiết bị kỹ thuật số','A digital device can connect to the network.','gadget','many device','many devices','Use plural after many.','Digital devices are part of daily life.','Thiết bị số là một phần của cuộc sống hằng ngày.'),
('algorithm','sorting algorithm','thuật toán sắp xếp','A sorting algorithm orders the results.','procedure','an algorithm are','an algorithm is','A singular noun takes a singular verb.','The system uses an efficient algorithm.','Hệ thống dùng một thuật toán hiệu quả.'),
('software','open-source software','phần mềm mã nguồn mở','They released open-source software.','program','a software','some software','Software is uncountable.','Software can support independent learning.','Phần mềm có thể hỗ trợ việc tự học.'),
('diet','balanced diet','chế độ ăn cân bằng','A balanced diet includes vegetables.','nutrition plan','a healthy diet are','a healthy diet is','Diet is singular in this sentence.','Diet affects long-term health.','Chế độ ăn ảnh hưởng sức khỏe lâu dài.'),
('stress','work-related stress','căng thẳng liên quan công việc','Work-related stress can affect sleep.','pressure','many stress','much stress','Stress is uncountable in this meaning.','People use different ways to relieve stress.','Mọi người dùng nhiều cách để giảm căng thẳng.'),
('salary','starting salary','mức lương khởi điểm','The starting salary is listed in the offer.','pay','earn monthly salary','earn a monthly salary','Use an article or possessive before salary.','The salary depends on the role.','Mức lương phụ thuộc vào vị trí.'),
('community','community centre','trung tâm cộng đồng','The community centre offers classes.','society','people in community','people in the community','Use the before a specific community.','Community groups share useful information.','Các nhóm cộng đồng chia sẻ thông tin hữu ích.'),
('policy','public policy','chính sách công','Public policy can shape urban planning.','strategy','a policy which encourage','a policy which encourages','A singular subject takes a singular verb.','The policy was discussed by residents.','Chính sách được cư dân thảo luận.'),
('method','teaching method','phương pháp giảng dạy','The teacher tried a new teaching method.','approach','a effective method','an effective method','Use an before a vowel sound.','Several methods were compared.','Một số phương pháp đã được so sánh.'),
('dissertation','doctoral dissertation','luận văn tiến sĩ','The doctoral dissertation was carefully reviewed.','thesis','write a dissertation about the topic','write a dissertation on the topic','Write a dissertation on a subject or topic.','The dissertation was reviewed by a committee.','Luận văn được hội đồng đánh giá.')
)
INSERT INTO vocabulary_collocations(sense_id, phrase, meaning_vi, example, sort_order)
SELECT s.id,d.collocation,d.meaning,d.example,2 FROM details d JOIN vocabulary_items i ON i.normalized_lemma=d.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);

CREATE TEMP TABLE catalog_demo_detail ON COMMIT DROP AS SELECT * FROM (VALUES
('pupil','learner','He is pupil.','He is a pupil.','A singular countable noun needs an article.','The pupil asked a thoughtful question.','Học sinh đặt một câu hỏi sâu sắc.'),
('curriculum','syllabus','curriculum are','the curriculum is','Curriculum is singular.','A balanced curriculum includes creative subjects.','Chương trình học cân bằng có các môn sáng tạo.'),
('research','investigation','a research','some research','Research is normally uncountable.','The research compares two teaching methods.','Nghiên cứu so sánh hai phương pháp giảng dạy.'),
('pollution','contamination','many pollution','much pollution','Pollution is uncountable.','Pollution levels fell after the policy changed.','Mức ô nhiễm giảm sau khi chính sách thay đổi.'),
('renewable','replenishable','renewable energies','renewable energy','Energy is uncountable in this general phrase.','Renewable energy is becoming more affordable.','Năng lượng tái tạo ngày càng có giá hợp lý.'),
('sustainable','viable','sustain development','sustainable development','Use the adjective sustainable.','Sustainable choices can protect natural resources.','Lựa chọn bền vững có thể bảo vệ tài nguyên thiên nhiên.'),
('device','gadget','many device','many devices','Use plural after many.','Each device has a clear purpose.','Mỗi thiết bị có một mục đích rõ ràng.'),
('algorithm','procedure','an algorithm are','an algorithm is','A singular noun takes a singular verb.','The algorithm identifies repeated patterns.','Thuật toán nhận diện các mẫu lặp lại.'),
('software','program','a software','some software','Software is uncountable.','The software was updated yesterday.','Phần mềm đã được cập nhật hôm qua.'),
('diet','nutrition plan','a healthy diet are','a healthy diet is','Diet is singular here.','A healthy diet includes a variety of foods.','Chế độ ăn lành mạnh gồm nhiều loại thực phẩm.'),
('stress','pressure','many stress','much stress','Stress is uncountable in this meaning.','The new schedule reduced stress.','Lịch trình mới đã giảm căng thẳng.'),
('salary','pay','earn monthly salary','earn a monthly salary','Use an article or possessive.','The salary is paid monthly.','Lương được trả hằng tháng.'),
('community','society','people in community','people in the community','Use the for a specific community.','The community welcomed new residents.','Cộng đồng chào đón cư dân mới.'),
('policy','strategy','a policy which encourage','a policy which encourages','A singular subject takes singular verb.','The policy supports public libraries.','Chính sách hỗ trợ thư viện công cộng.'),
('method','approach','a effective method','an effective method','Use an before a vowel sound.','The method is easy to reproduce.','Phương pháp này dễ tái hiện.'),
('dissertation','thesis','write a dissertation about the topic','write a dissertation on the topic','Write a dissertation on a subject or topic.','The dissertation was reviewed by a committee.','Luận văn được hội đồng đánh giá.')
) AS d(lemma, synonym, mistake, correct, explanation, sentence, translation);
INSERT INTO vocabulary_relations(sense_id,related_lemma,kind,sort_order)
SELECT s.id,d.synonym,'SYNONYM',1 FROM catalog_demo_detail d JOIN vocabulary_items i ON i.normalized_lemma=d.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);
INSERT INTO vocabulary_common_mistakes(sense_id,wrong_usage,correct_usage,explanation,sort_order)
SELECT s.id,d.mistake,d.correct,d.explanation,1 FROM catalog_demo_detail d JOIN vocabulary_items i ON i.normalized_lemma=d.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);
INSERT INTO vocabulary_examples(sense_id,sentence,translation_vi,sort_order)
SELECT s.id,d.sentence,d.translation,1 FROM catalog_demo_detail d JOIN vocabulary_items i ON i.normalized_lemma=d.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);
WITH second_examples(lemma,sentence,translation) AS (VALUES
('pupil','The pupil borrowed a book from the library.','Học sinh mượn một cuốn sách từ thư viện.'),
('curriculum','The school revised its curriculum last year.','Trường đã sửa đổi chương trình học vào năm ngoái.'),
('research','Further research is needed before a conclusion can be drawn.','Cần nghiên cứu thêm trước khi có thể đưa ra kết luận.'),
('pollution','Local residents campaigned against water pollution.','Cư dân địa phương vận động chống ô nhiễm nguồn nước.'),
('renewable','The island plans to generate more electricity from renewable sources.','Hòn đảo dự định tạo thêm điện từ các nguồn tái tạo.'),
('sustainable','The company is adopting more sustainable packaging.','Công ty đang áp dụng bao bì bền vững hơn.'),
('device','The device can measure temperature and humidity.','Thiết bị có thể đo nhiệt độ và độ ẩm.'),
('algorithm','The algorithm sorts results by relevance.','Thuật toán sắp xếp kết quả theo mức độ liên quan.'),
('software','The software allows students to practise at their own pace.','Phần mềm cho phép học sinh luyện tập theo tốc độ của mình.'),
('diet','A varied diet can provide a range of nutrients.','Chế độ ăn đa dạng có thể cung cấp nhiều loại chất dinh dưỡng.'),
('stress','Regular breaks can help people manage stress.','Nghỉ giải lao thường xuyên có thể giúp mọi người kiểm soát căng thẳng.'),
('community','The community worked together to restore the park.','Cộng đồng cùng nhau khôi phục công viên.'),
('policy','The policy encourages people to use public transport.','Chính sách khuyến khích mọi người sử dụng phương tiện công cộng.'),
('salary','The advertised salary depends on the candidate’s experience.','Mức lương được quảng cáo phụ thuộc vào kinh nghiệm của ứng viên.'),
('method','The method can be repeated by other researchers.','Các nhà nghiên cứu khác có thể lặp lại phương pháp này.'),
('dissertation','The dissertation examines language learning.','Luận văn phân tích việc học ngôn ngữ.')
)
INSERT INTO vocabulary_examples(sense_id,sentence,translation_vi,sort_order)
SELECT s.id,e.sentence,e.translation,2 FROM second_examples e JOIN vocabulary_items i ON i.normalized_lemma=e.lemma
JOIN vocabulary_senses s ON s.vocabulary_item_id=i.id AND s.status='ACTIVE'
WHERE s.id=(SELECT sx.id FROM vocabulary_senses sx WHERE sx.vocabulary_item_id=i.id AND sx.status='ACTIVE' ORDER BY sx.sort_order,sx.id LIMIT 1);

INSERT INTO vocabulary_sets(id,title,description,kind,cefr_level,sort_order) VALUES
('c2000000-0000-4000-8000-000000000001','A1 Foundation','Core beginner vocabulary','BAND','A1',1),
('c2000000-0000-4000-8000-000000000002','A2 Elementary','Everyday elementary vocabulary','BAND','A2',2),
('c2000000-0000-4000-8000-000000000003','B1 Intermediate','Intermediate vocabulary for familiar topics','BAND','B1',3),
('c2000000-0000-4000-8000-000000000004','B2 Upper Intermediate','Academic and topic vocabulary','BAND','B2',4),
('c2000000-0000-4000-8000-000000000005','C1 Advanced','Advanced vocabulary','BAND','C1',5),
('c2000000-0000-4000-8000-000000000006','C2 Proficiency','Proficiency-level vocabulary','BAND','C2',6);
INSERT INTO vocabulary_sets(id,title,description,kind,topic_id,sort_order)
SELECT ('c2000000-0000-4000-8000-' || LPAD((100 + row_number() OVER (ORDER BY sort_order))::text,12,'0'))::uuid,
       name || ' Vocabulary','Curated vocabulary for ' || name,'TOPIC',id,sort_order
FROM vocabulary_topics ORDER BY sort_order;

INSERT INTO vocabulary_set_items(set_id,sense_id,sort_order)
SELECT st.id,s.id,row_number() OVER (PARTITION BY st.id ORDER BY i.normalized_lemma,s.sort_order,s.id)
FROM vocabulary_sets st JOIN vocabulary_topics t ON t.id=st.topic_id
JOIN vocabulary_senses s ON s.topic_id=t.id AND s.status='ACTIVE'
JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE st.kind='TOPIC';
INSERT INTO vocabulary_set_items(set_id,sense_id,sort_order)
SELECT st.id,s.id,row_number() OVER (PARTITION BY st.id ORDER BY i.normalized_lemma,s.sort_order,s.id)
FROM vocabulary_sets st JOIN vocabulary_senses s ON s.cefr_level=st.cefr_level AND s.status='ACTIVE'
JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE st.kind='BAND';
