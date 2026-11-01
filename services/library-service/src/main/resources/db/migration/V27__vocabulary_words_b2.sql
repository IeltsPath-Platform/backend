-- Demo content; requires review by an IELTS examiner before production.
-- B2 words for choosing game words by level and topic. Reuses a word that exists without a meaning (pronunciation-only
-- rows from video subtitles). A word that already has a meaning keeps it, and only gets a level and topic if it has none.

CREATE TEMP TABLE new_words (lemma text, pos text, topic_slug text, meaning text, definition text, example text) ON COMMIT DROP;
INSERT INTO new_words VALUES
('thesis','NOUN','education','luận văn','a long piece of university writing','She is writing her thesis on trade.'),
('faculty','NOUN','education','khoa','a department of a university','He teaches in the science faculty.'),
('diploma','NOUN','education','văn bằng','a certificate from a course','She earned a diploma in nursing.'),
('semester','NOUN','education','học kỳ','half of a school year','The semester ends in June.'),
('intellectual','ADJECTIVE','education','thuộc trí tuệ','relating to the mind','Reading is an intellectual activity.'),
('literature','NOUN','education','văn học','books and writing as art','She studies English literature.'),
('professor','NOUN','education','giáo sư','a senior university teacher','The professor gave a long lecture.'),
('pesticide','NOUN','environment','thuốc trừ sâu','a chemical that kills insects','Farmers use less pesticide now.'),
('pollutant','NOUN','environment','chất gây ô nhiễm','a substance that pollutes','Cars release many pollutants.'),
('coastal','ADJECTIVE','environment','ven biển','near the coast','Coastal towns may flood.'),
('landslide','NOUN','environment','sạt lở đất','a fall of earth down a hill','A landslide blocked the road.'),
('organic','ADJECTIVE','environment','hữu cơ','grown without chemicals','We buy organic vegetables.'),
('bandwidth','NOUN','technology','băng thông','the amount of data a connection carries','Video calls need more bandwidth.'),
('drone','NOUN','technology','máy bay không người lái','a small aircraft with no pilot','A drone filmed the match.'),
('processor','NOUN','technology','bộ vi xử lý','the main chip in a computer','This laptop has a fast processor.'),
('sensor','NOUN','technology','cảm biến','a device that detects changes','The sensor turns on the light.'),
('storage','NOUN','technology','bộ nhớ lưu trữ','space for keeping data','My phone is out of storage.'),
('malware','NOUN','technology','phần mềm độc hại','software that damages computers','The email contained malware.'),
('spreadsheet','NOUN','technology','bảng tính','a table of data on a computer','She keeps the budget in a spreadsheet.'),
('electric','ADJECTIVE','technology','chạy bằng điện','powered by electricity','Electric cars are getting cheaper.'),
('programmer','NOUN','technology','lập trình viên','a person who writes software','He works as a programmer.'),
('microchip','NOUN','technology','vi mạch','a tiny electronic circuit','Each card has a microchip.'),
('gigabyte','NOUN','technology','gigabyte','a unit of computer storage','The file is one gigabyte.'),
('antibiotic','NOUN','health','thuốc kháng sinh','medicine that kills bacteria','The doctor gave me an antibiotic.'),
('contagious','ADJECTIVE','health','dễ lây lan','spreading from person to person','The illness is highly contagious.'),
('insomnia','NOUN','health','chứng mất ngủ','being unable to sleep','Stress can cause insomnia.'),
('fatigue','NOUN','health','sự mệt mỏi','extreme tiredness','Long hours cause fatigue.'),
('digestion','NOUN','health','sự tiêu hóa','the process of breaking down food','Fibre helps digestion.'),
('investor','NOUN','work-business','nhà đầu tư','a person who puts money into a business','Investors want a quick return.'),
('negotiate','VERB','work-business','đàm phán','to discuss to reach agreement','They negotiated a better price.'),
('shareholder','NOUN','work-business','cổ đông','an owner of company shares','Shareholders voted on the plan.'),
('resign','VERB','work-business','từ chức','to leave a job officially','He resigned after two years.'),
('overtime','NOUN','work-business','giờ làm thêm','extra hours at work','I worked overtime this week.'),
('pension','NOUN','work-business','lương hưu','money paid after you retire','He lives on a small pension.'),
('internship','NOUN','work-business','kỳ thực tập','a short period of work for experience','She did an internship at a bank.'),
('merger','NOUN','work-business','sự sáp nhập','two companies joining','The merger created a giant firm.'),
('commuter','NOUN','daily-life','người đi làm xa','a person who travels to work','Commuters fill the morning trains.'),
('mortgage','NOUN','daily-life','khoản vay mua nhà','a loan to buy a home','They pay a large mortgage.'),
('stroll','NOUN','daily-life','cuộc đi dạo','a slow relaxed walk','We went for a stroll by the lake.'),
('errand','NOUN','daily-life','việc vặt','a short trip to do a task','I have some errands to run.'),
('bargain','NOUN','daily-life','món hời','something cheap for its value','This coat was a real bargain.'),
('utensil','NOUN','daily-life','dụng cụ nhà bếp','a tool used in the kitchen','Wash the kitchen utensils.'),
('cutlery','NOUN','daily-life','dao nĩa','knives, forks and spoons','Put the cutlery on the table.'),
('tenant','NOUN','daily-life','người thuê nhà','a person who rents a home','The tenant pays rent monthly.'),
('landlord','NOUN','daily-life','chủ nhà cho thuê','a person who rents out property','The landlord fixed the heating.'),
('appetite','NOUN','daily-life','sự thèm ăn','the desire to eat','I have lost my appetite.'),
('wardrobe','NOUN','daily-life','tủ quần áo','a large cupboard for clothes','My coat is in the wardrobe.'),
('ingredient','NOUN','daily-life','nguyên liệu','one food in a dish','Check each ingredient on the label.'),
('chore','NOUN','daily-life','việc vặt trong nhà','a routine household task','Washing up is my least favourite chore.'),
('hospitality','NOUN','daily-life','lòng hiếu khách','a friendly welcome to guests','We enjoyed their hospitality.'),
('sightseeing','NOUN','daily-life','việc tham quan','visiting interesting places','We went sightseeing in Rome.'),
('detour','NOUN','daily-life','đường vòng','a longer way round','We took a detour to avoid traffic.'),
('vegetarian','NOUN','daily-life','người ăn chay','a person who eats no meat','My sister is a vegetarian.'),
('greenhouse','NOUN','environment','nhà kính','a glass building for plants','Tomatoes grow well in a greenhouse.'),
('compost','NOUN','environment','phân hữu cơ','decayed plants used for soil','We make compost from kitchen waste.'),
('router','NOUN','technology','bộ định tuyến','a device that connects to the internet','Restart the router if the wifi fails.'),
('server','NOUN','technology','máy chủ','a computer that provides data','The server went down last night.'),
('username','NOUN','technology','tên đăng nhập','a name used to log in','Enter your username and password.'),
('touchscreen','NOUN','technology','màn hình cảm ứng','a screen you control by touching','The car has a touchscreen.'),
('robotic','ADJECTIVE','technology','thuộc robot','using robots','The factory uses robotic arms.'),
('inflammation','NOUN','health','sự viêm','swelling and pain in the body','The drug reduces inflammation.'),
('paramedic','NOUN','health','nhân viên cấp cứu','a person trained in emergency care','Paramedics arrived within minutes.'),
('dosage','NOUN','health','liều lượng','the amount of medicine to take','Follow the dosage on the box.'),
('migraine','NOUN','health','chứng đau nửa đầu','a very bad headache','She gets a migraine when stressed.'),
('recruit','VERB','work-business','tuyển dụng','to find new staff','We recruit graduates each year.'),
('franchise','NOUN','work-business','nhượng quyền thương mại','a right to run a branded shop','He bought a coffee franchise.'),
('wholesale','NOUN','work-business','bán buôn','selling in large amounts','They buy goods at wholesale.');

UPDATE vocabulary_senses s SET cefr_level = 'B2', topic_id = t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE s.vocabulary_item_id = i.id AND s.cefr_level IS NULL AND s.topic_id IS NULL;

INSERT INTO vocabulary_items (lemma, normalized_lemma)
SELECT n.lemma, n.lemma FROM new_words n
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_items i WHERE i.normalized_lemma = n.lemma);

INSERT INTO vocabulary_senses (vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning, example_sentence, cefr_level, topic_id)
SELECT DISTINCT ON (n.lemma) i.id, n.pos, n.definition, n.meaning, n.example, 'B2', t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id = i.id)
ORDER BY n.lemma, i.id;
