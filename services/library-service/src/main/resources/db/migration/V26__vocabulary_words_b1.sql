-- Demo content; requires review by an IELTS examiner before production.
-- B1 words for choosing game words by level and topic. Reuses a word that exists without a meaning (pronunciation-only
-- rows from video subtitles). A word that already has a meaning keeps it, and only gets a level and topic if it has none.

CREATE TEMP TABLE new_words (lemma text, pos text, topic_slug text, meaning text, definition text, example text) ON COMMIT DROP;
INSERT INTO new_words VALUES
('knowledge','NOUN','education','kiến thức','what you know from learning','Reading improves your general knowledge.'),
('scholar','NOUN','education','học giả','a person who studies a subject deeply','He is a respected scholar of history.'),
('instruction','NOUN','education','sự hướng dẫn','teaching or directions','Read the instructions carefully.'),
('recycling','NOUN','environment','việc tái chế','using old materials again','Recycling reduces rubbish.'),
('environment','NOUN','environment','môi trường','the natural world around us','We must protect the environment.'),
('solar','ADJECTIVE','environment','thuộc mặt trời','from the sun','Solar panels make clean energy.'),
('coast','NOUN','environment','bờ biển','land next to the sea','We drove along the coast.'),
('volcano','NOUN','environment','núi lửa','a mountain that can erupt','The volcano erupted last year.'),
('earthquake','NOUN','environment','động đất','a sudden shaking of the ground','The earthquake damaged many homes.'),
('jungle','NOUN','environment','rừng rậm','a thick tropical forest','Tigers live in the jungle.'),
('smoke','NOUN','environment','khói','grey gas from something burning','Smoke rose from the factory.'),
('planet','NOUN','environment','hành tinh','a large body moving round a star','Mars is a red planet.'),
('dust','NOUN','environment','bụi','very small pieces of dry dirt','Dust covered the old road.'),
('oxygen','NOUN','environment','khí oxy','a gas that we need to breathe','Trees produce oxygen.'),
('valley','NOUN','environment','thung lũng','low land between hills','A river runs through the valley.'),
('harvest','NOUN','environment','vụ thu hoạch','the time when crops are collected','This year''s harvest was good.'),
('hardware','NOUN','technology','phần cứng','the physical parts of a computer','We need to replace the hardware.'),
('gadget','NOUN','technology','thiết bị nhỏ','a small clever device','He loves buying new gadgets.'),
('satellite','NOUN','technology','vệ tinh','a machine that orbits the earth','The signal comes from a satellite.'),
('signal','NOUN','technology','tín hiệu','a sound or wave carrying information','There is no phone signal here.'),
('invention','NOUN','technology','phát minh','something newly created','The phone was a great invention.'),
('browser','NOUN','technology','trình duyệt','a program for viewing websites','Open the link in your browser.'),
('account','NOUN','technology','tài khoản','a personal record on a website','Create an account to continue.'),
('technology','NOUN','technology','công nghệ','the use of science in practical ways','Technology changes quickly.'),
('electronic','ADJECTIVE','technology','điện tử','using electric parts','We sell electronic goods.'),
('wireless','ADJECTIVE','technology','không dây','working without cables','I use a wireless mouse.'),
('update','VERB','technology','cập nhật','to make something newer','Please update the app.'),
('allergy','NOUN','health','dị ứng','a bad reaction to something','She has a nut allergy.'),
('virus','NOUN','health','vi-rút','a tiny thing that causes disease','The virus spreads quickly.'),
('bacteria','NOUN','health','vi khuẩn','tiny living things that can cause illness','Wash your hands to kill bacteria.'),
('ache','NOUN','health','cơn đau âm ỉ','a continuous pain','I have an ache in my back.'),
('wound','NOUN','health','vết thương','a cut in the skin','Clean the wound with water.'),
('protein','NOUN','health','chất đạm','a nutrient found in meat and beans','Eggs are rich in protein.'),
('clinic','NOUN','health','phòng khám','a small medical centre','She works at a local clinic.'),
('checkup','NOUN','health','buổi khám tổng quát','a general medical examination','I go for a checkup every year.'),
('reservation','NOUN','daily-life','việc đặt chỗ','an arrangement to keep a table or room','I made a reservation for two.'),
('appliance','NOUN','daily-life','thiết bị gia dụng','a machine used at home','The kitchen has modern appliances.'),
('grocery','NOUN','daily-life','cửa hàng tạp hóa','a shop selling food','I stopped at the grocery on my way home.'),
('housework','NOUN','daily-life','việc nhà','cleaning and other home jobs','We share the housework.'),
('hobby','NOUN','daily-life','sở thích','an activity you do for fun','My hobby is taking photos.'),
('routine','NOUN','daily-life','thói quen hằng ngày','the things you usually do','Exercise is part of my routine.'),
('traffic','NOUN','daily-life','giao thông','vehicles on the road','The traffic is bad this morning.'),
('apartment','NOUN','daily-life','căn hộ','rooms to live in within a building','We rent a small apartment.'),
('rent','VERB','daily-life','thuê','to pay to use something','They rent a flat near the park.'),
('souvenir','NOUN','daily-life','quà lưu niệm','something bought to remember a trip','I bought a souvenir for my sister.'),
('luxury','NOUN','daily-life','sự xa xỉ','great comfort at a high cost','They live in luxury.'),
('transport','NOUN','daily-life','phương tiện đi lại','ways of travelling','The city has good transport.'),
('delay','NOUN','daily-life','sự chậm trễ','a time when something is late','There is a delay of two hours.'),
('leisure','NOUN','daily-life','thời gian rảnh rỗi','time when you are not working','I read in my leisure time.'),
('webcam','NOUN','technology','camera máy tính','a camera connected to a computer','Turn on your webcam for the call.'),
('keyword','NOUN','technology','từ khóa','a word used to search','Type a keyword to find the file.'),
('organ','NOUN','health','cơ quan nội tạng','a body part with a function','The heart is an important organ.'),
('bandage','NOUN','health','băng gạc','a strip of cloth for a wound','Put a bandage on the cut.'),
('pulse','NOUN','health','nhịp mạch','the beat of your heart','The nurse checked my pulse.'),
('skeleton','NOUN','health','bộ xương','the bones of the body','The human skeleton has many bones.');

UPDATE vocabulary_senses s SET cefr_level = 'B1', topic_id = t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE s.vocabulary_item_id = i.id AND s.cefr_level IS NULL AND s.topic_id IS NULL;

INSERT INTO vocabulary_items (lemma, normalized_lemma)
SELECT n.lemma, n.lemma FROM new_words n
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_items i WHERE i.normalized_lemma = n.lemma);

INSERT INTO vocabulary_senses (vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning, example_sentence, cefr_level, topic_id)
SELECT DISTINCT ON (n.lemma) i.id, n.pos, n.definition, n.meaning, n.example, 'B1', t.id
FROM new_words n
JOIN vocabulary_items i ON i.normalized_lemma = n.lemma
JOIN vocabulary_topics t ON t.slug = n.topic_slug
WHERE NOT EXISTS (SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id = i.id)
ORDER BY n.lemma, i.id;
