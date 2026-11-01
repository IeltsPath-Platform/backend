-- Demo content; requires review by an IELTS examiner before production.
-- C1 words for choosing game words by level and topic. Reuses a word that exists without a meaning (pronunciation-only
-- rows from video subtitles). A word that already has a meaning keeps it, and only gets a level and topic if it has none.

CREATE TEMP TABLE new_words (lemma text, pos text, topic_slug text, meaning text, definition text, example text) ON COMMIT DROP;
INSERT INTO new_words VALUES
('pedagogy','NOUN','education','phương pháp sư phạm','the method of teaching','Modern pedagogy values discussion.'),
('scholarly','ADJECTIVE','education','mang tính học thuật','relating to serious study','She published a scholarly article.'),
('literate','ADJECTIVE','education','biết đọc biết viết','able to read and write','Most adults here are literate.'),
('rote','NOUN','education','sự học vẹt','learning by repeating','Children learned the poem by rote.'),
('tenure','NOUN','education','biên chế giảng dạy','a permanent post at a university','She was granted tenure last year.'),
('methodology','NOUN','education','phương pháp luận','the methods used in research','The study explains its methodology.'),
('aptitude','NOUN','education','năng khiếu','a natural ability','He has an aptitude for languages.'),
('postgraduate','NOUN','education','nghiên cứu sinh sau đại học','a student with a first degree','Postgraduates may teach some classes.'),
('enrolment','NOUN','education','sự ghi danh','the act of joining a course','Enrolment opens in August.'),
('remedial','ADJECTIVE','education','mang tính phụ đạo','meant to improve weak skills','He takes remedial maths classes.'),
('discourse','NOUN','education','diễn ngôn, bài luận bàn','serious discussion of a subject','Academic discourse can be difficult.'),
('bibliography','NOUN','education','thư mục tham khảo','a list of sources','Add a bibliography to your essay.'),
('cognition','NOUN','education','nhận thức','the mental process of knowing','Sleep supports cognition.'),
('ecological','ADJECTIVE','environment','thuộc sinh thái','relating to living things and their environment','The oil spill caused ecological damage.'),
('reforestation','NOUN','environment','sự tái trồng rừng','planting trees on cleared land','Reforestation helps wildlife return.'),
('contamination','NOUN','environment','sự nhiễm bẩn','making something impure','Water contamination is a serious risk.'),
('depletion','NOUN','environment','sự cạn kiệt','a large reduction in supply','Ozone depletion worries scientists.'),
('deplete','VERB','environment','làm cạn kiệt','to use up a supply','Overfishing depletes the oceans.'),
('ozone','NOUN','environment','khí ozone','a gas in the upper atmosphere','The ozone layer blocks harmful rays.'),
('sewage','NOUN','environment','nước thải','waste water from homes','Sewage was pumped into the river.'),
('toxic','ADJECTIVE','environment','độc hại','poisonous','The factory released toxic waste.'),
('aquifer','NOUN','environment','tầng ngậm nước','underground rock holding water','The aquifer supplies the whole region.'),
('mitigate','VERB','environment','giảm nhẹ','to make less severe','Forests help mitigate climate change.'),
('wetland','NOUN','environment','vùng đất ngập nước','an area of wet land','Birds nest in the wetland.'),
('overfishing','NOUN','environment','đánh bắt quá mức','catching too many fish','Overfishing has reduced stocks.'),
('blockchain','NOUN','technology','chuỗi khối','a shared digital record of transactions','Blockchain records each payment.'),
('semiconductor','NOUN','technology','chất bán dẫn','a material used in chips','There is a semiconductor shortage.'),
('biometric','ADJECTIVE','technology','thuộc sinh trắc học','using body features for identity','The phone has biometric security.'),
('nanotechnology','NOUN','technology','công nghệ nano','technology at a tiny scale','Nanotechnology is used in medicine.'),
('firewall','NOUN','technology','tường lửa','a system that blocks network attacks','The firewall stopped the attack.'),
('prototype','NOUN','technology','nguyên mẫu','a first model of a product','Engineers tested the prototype.'),
('scalable','ADJECTIVE','technology','có thể mở rộng','able to grow easily','The system is highly scalable.'),
('telecommunications','NOUN','technology','viễn thông','sending information over distance','He works in telecommunications.'),
('autonomous','ADJECTIVE','technology','tự hành','able to work alone','Autonomous cars are being tested.'),
('compatibility','NOUN','technology','tính tương thích','ability to work together','Check the compatibility of the software.'),
('latency','NOUN','technology','độ trễ','the delay in data transfer','Low latency matters in online games.'),
('disruptive','ADJECTIVE','technology','mang tính đột phá','causing major change','It was a disruptive new technology.'),
('cardiovascular','ADJECTIVE','health','thuộc tim mạch','of the heart and blood vessels','Running improves cardiovascular health.'),
('chronically','ADVERB','health','một cách mãn tính','for a long time','He is chronically ill.'),
('pathogen','NOUN','health','mầm bệnh','an organism causing disease','The pathogen spreads through water.'),
('dementia','NOUN','health','chứng mất trí','a serious decline in memory','Dementia affects many old people.'),
('metabolism','NOUN','health','sự trao đổi chất','the body''s use of food','Exercise speeds up your metabolism.'),
('placebo','NOUN','health','giả dược','a fake treatment used in tests','Half the group took a placebo.'),
('remission','NOUN','health','sự thuyên giảm','a period when an illness improves','Her cancer is in remission.'),
('anaesthetic','NOUN','health','thuốc gây mê','a drug that stops pain','The dentist gave me an anaesthetic.'),
('pandemic','NOUN','health','đại dịch','a disease spread worldwide','The pandemic closed many schools.'),
('preventive','ADJECTIVE','health','mang tính phòng ngừa','meant to stop illness','Preventive care saves money.'),
('transplant','NOUN','health','cấy ghép','moving an organ to another body','He needs a heart transplant.'),
('monopoly','NOUN','work-business','độc quyền','one company controlling a market','The firm has a monopoly on gas.'),
('acquisition','NOUN','work-business','sự thâu tóm','buying another company','The acquisition cost millions.'),
('outsource','VERB','work-business','thuê ngoài','to pay another firm to do work','They outsource customer support.'),
('remuneration','NOUN','work-business','thù lao','payment for work','The post offers good remuneration.'),
('stakeholder','NOUN','work-business','bên liên quan','a person with an interest in a business','Stakeholders were asked for views.'),
('bankruptcy','NOUN','work-business','sự phá sản','being legally unable to pay debts','The firm faced bankruptcy.'),
('dividend','NOUN','work-business','cổ tức','profit paid to shareholders','The dividend rose this year.'),
('entrepreneurial','ADJECTIVE','work-business','có tính kinh doanh','willing to start businesses','She has an entrepreneurial spirit.'),
('liability','NOUN','work-business','trách nhiệm pháp lý','legal responsibility','The firm accepted liability.'),
('logistics','NOUN','work-business','hậu cần','organising the transport of goods','He manages logistics for a retailer.'),
('downsize','VERB','work-business','cắt giảm quy mô','to reduce staff','The company had to downsize.'),
('turnover','NOUN','work-business','doanh số','the total sales of a firm','Annual turnover reached one million.'),
('incentive','NOUN','work-business','sự khuyến khích','something that motivates','Bonuses are an incentive to work hard.'),
('austerity','NOUN','work-business','thắt lưng buộc bụng','strict limits on spending','Austerity cut public services.'),
('bureaucracy','NOUN','society','bộ máy quan liêu','complex official systems','Bureaucracy slows every decision.'),
('sovereignty','NOUN','society','chủ quyền','a nation''s power to rule itself','The treaty respects national sovereignty.'),
('referendum','NOUN','society','cuộc trưng cầu dân ý','a public vote on one issue','They held a referendum on the law.'),
('marginalised','ADJECTIVE','society','bị gạt ra bên lề','treated as unimportant','The plan helps marginalised groups.'),
('activism','NOUN','society','chủ nghĩa hoạt động xã hội','action for social change','Student activism grew rapidly.'),
('censorship','NOUN','society','sự kiểm duyệt','banning parts of the media','The press opposed censorship.'),
('frugal','ADJECTIVE','daily-life','tiết kiệm','careful with money','They live a frugal life.'),
('culinary','ADJECTIVE','daily-life','thuộc ẩm thực','relating to cooking','She has great culinary skills.'),
('ambience','NOUN','daily-life','không khí của nơi chốn','the mood of a place','The cafe has a relaxed ambience.'),
('clutter','NOUN','daily-life','đồ đạc bừa bộn','an untidy collection of things','I must clear this clutter.'),
('upkeep','NOUN','daily-life','sự bảo dưỡng','the cost of keeping something in order','The upkeep of an old house is high.'),
('renovate','VERB','daily-life','cải tạo','to repair and improve a building','They renovated the kitchen.'),
('memento','NOUN','daily-life','vật kỷ niệm','an object kept as a reminder','This shell is a memento of our trip.'),
('wanderlust','NOUN','daily-life','niềm đam mê xê dịch','a strong desire to travel','Her wanderlust took her to Asia.'),
('gastronomy','NOUN','daily-life','nghệ thuật ẩm thực','the art of good food','Lyon is famous for its gastronomy.'),
('indulge','VERB','daily-life','nuông chiều bản thân','to allow yourself a pleasure','I indulged in a slice of cake.'),
('decor','NOUN','daily-life','cách trang trí','the style of a room','We like the decor of this hotel.'),
('housekeeping','NOUN','daily-life','công việc quản gia','looking after a home or hotel','Call housekeeping for more towels.'),
('pastime','NOUN','daily-life','thú tiêu khiển','an activity done for enjoyment','Fishing is his favourite pastime.'),
('thrifty','ADJECTIVE','daily-life','tằn tiện','using money carefully','My thrifty aunt never wastes food.'),
('amenity','NOUN','daily-life','tiện nghi','a useful feature of a place','The flat has every amenity.'),
('courteous','ADJECTIVE','daily-life','lịch sự','polite and respectful','The staff were very courteous.'),
('commuting','NOUN','daily-life','việc đi lại làm việc','travelling to work regularly','Commuting takes two hours a day.'),
('frazzled','ADJECTIVE','daily-life','kiệt sức','very tired and upset','I felt frazzled after the long day.'),
('snooze','VERB','daily-life','chợp mắt','to sleep lightly for a short time','He snoozed on the sofa.'),
('dwelling','NOUN','daily-life','nơi ở','a place where someone lives','The old dwelling has no electricity.'),
('reservoir','NOUN','environment','hồ chứa nước','a large store of water','The reservoir supplies the city.'),
('geothermal','ADJECTIVE','environment','thuộc địa nhiệt','using heat from inside the earth','Iceland uses geothermal power.'),
('neurological','ADJECTIVE','health','thuộc thần kinh','relating to the nerves','She has a neurological disorder.'),
('outpatient','NOUN','health','bệnh nhân ngoại trú','a patient who does not stay in hospital','He was treated as an outpatient.'),
('biopsy','NOUN','health','sinh thiết','taking tissue for testing','The biopsy showed no cancer.'),
('philanthropy','NOUN','society','hoạt động từ thiện','giving money to help others','Her philanthropy funded new schools.'),
('coalition','NOUN','society','liên minh','a union of political groups','A coalition won the election.');

UPDATE vocabulary_senses s SET cefr_level = 'C1', topic_id = t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE s.vocabulary_item_id = i.id AND s.cefr_level IS NULL AND s.topic_id IS NULL;

INSERT INTO vocabulary_items (lemma, normalized_lemma)
SELECT n.lemma, n.lemma FROM new_words n
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_items i WHERE i.normalized_lemma = n.lemma);

INSERT INTO vocabulary_senses (vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning, example_sentence, cefr_level, topic_id)
SELECT DISTINCT ON (n.lemma) i.id, n.pos, n.definition, n.meaning, n.example, 'C1', t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id = i.id)
ORDER BY n.lemma, i.id;
