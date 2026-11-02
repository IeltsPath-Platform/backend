-- Listening Practice Test 5 (PT-LISTENING-05): a full-length Listening test, four parts of 10 questions.
--
-- Part 1 completes a cooking-class booking, Part 2 is a botanic garden talk with multiple choice and matching places to features, Part 3 is a tutorial with multiple choice and matching tasks to people, and Part 4 completes lecture notes on the history of the bicycle.
-- The script of each part is the transcript (text_content) of its recording asset, and every answer is spoken in it. The
-- recordings are generated with edge-tts voices and uploaded to the public "media" bucket under listening/practice/ and the
-- cover photos under practice/covers/ separately; this migration does not upload them.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-020000004001', 'AUDIO', 'Receptionist: Good morning, Greenfield Kitchen School. How can I help you?
Customer: Hello. I saw your advert for the evening cooking classes, and I would like to book a place, please.
Receptionist: Of course. Can I take your surname first?
Customer: It''s Hartley. That''s H, A, R, T, L, E, Y.
Receptionist: Thank you, Mr Hartley. Which class were you thinking of? This autumn we have Thai cooking, bread making and Italian cooking.
Customer: I think I would like the Thai one. Oh, is that the class on Thursdays?
Receptionist: Yes, but I''m afraid the Thai class is already full. The Italian class still has some places left.
Customer: Then Italian cooking, please. My wife loves pasta, so it''s a good choice anyway.
Receptionist: Lovely. The next Italian class is on the nineteenth of October.
Customer: The nineteenth. I thought it was the eighteenth.
Receptionist: No, the eighteenth is a Monday. The class is on Tuesday the nineteenth.
Customer: Great. And what time does it start?
Receptionist: It usually starts at six, but in October we begin earlier, at half past five, so that everyone can eat the meal together afterwards.
Customer: Half past five is fine. How much is it?
Receptionist: It''s forty-five pounds per person for the evening, and that includes all the ingredients.
Customer: That''s reasonable. How many people are there in each class?
Receptionist: We keep the groups small. There is a maximum of twelve students, so everybody has space at the cooker.
Customer: Good. Do I need to bring anything?
Receptionist: Just an apron. We provide the knives, the pans and everything else.
Customer: An apron, fine. One more thing. I have an allergy. I can''t eat peanuts. Is that a problem?
Receptionist: Not at all. Please tell the teacher when you arrive and we will change the recipe. We take allergies very seriously.
Customer: Thanks. And where can I park?
Receptionist: Please don''t use the street, because it''s very busy. There''s a small car park behind the bakery, opposite our entrance.
Customer: The bakery, I see. Thank you.
Receptionist: Could I have a contact number, please?
Customer: Yes, it''s oh seven nine one one, then three two six, seven zero five.
Receptionist: Thank you. We will send you a confirmation by text message. See you on the nineteenth, Mr Hartley.
Customer: Thank you very much. Goodbye.', 'listening/practice/pt-listening-05-s1.mp3', 214, 'VALID'),
    ('2d000000-0000-4000-8000-020000004002', 'AUDIO', 'Guide: Good morning, everyone, and welcome to Westbrook Botanic Garden. My name is Helen, and I will be your guide today. Before we set off, let me tell you a little about the garden and how to make the most of your visit.
Guide: Westbrook began as a private family estate. The Westbrook family collected plants from all over the world, and in the nineteen thirties they gave the whole estate to the city, so that everybody could enjoy it. It has been open to the public ever since.
Guide: A few practical points. Normally an adult ticket costs twelve pounds, but on Wednesdays after three o''clock, entry is free for everyone. Local residents also pay half price on any other day if they show proof of address. If you are thinking of coming back, a yearly pass is much better value.
Guide: You came in through the main gate, and I hope it was not too crowded. In the mornings, many school groups arrive at that gate, and it can be very busy until about eleven. If you visit again, the side entrance by the lake is much quieter. There is a small sign pointing to it from the bus stop.
Guide: Now, what should you not miss? Many people come to see our oak tree, which is more than three hundred years old, but I think our real star is the giant water lily in the glasshouse pond. Its leaves grow to more than two metres across, and they are strong enough to hold the weight of a small child. It flowers for only two nights each summer, and we hold a special evening event to see it.
Guide: Please remember that dogs are not allowed in the garden, except for guide dogs. This is to protect the birds that nest on the ground. You are welcome to take photographs, and you may use a flash, but please do not pick any flowers or fruit.
Guide: Now let me describe the different areas, so that you can decide where to spend your time. Near the entrance is the Rose Garden. It is at its best in June, and there are free guided talks there every hour from ten o''clock, so ask at the information desk if you would like to join one.
Guide: Just behind it is the Water Garden. Families with children usually love this area, because there is a children''s trail that ends at the pond, where you can look for frogs and dragonflies. Pick up a trail leaflet and a pencil at the gate.
Guide: Next comes the Herb Garden. It is a quiet place with old stone statues, which came from the original family house, and each one stands beside the herb it is named after. Many visitors are surprised to find them there.
Guide: The Glasshouse is the large building on your right. It is warm and humid inside, because it holds our collection of tropical plants, including bananas, orchids and the giant water lily I mentioned. Do take off your coat before you go in.
Guide: And finally, the Woodland Walk, which leads from the lake up to the hill. It takes about forty minutes, and halfway along there are picnic tables, so it is the best place to eat your lunch. The cafe near the main gate is open all day if you would rather buy something hot.
Guide: That is all from me. If you have any questions, please ask. Otherwise, enjoy your visit.', 'listening/practice/pt-listening-05-s2.mp3', 228, 'VALID'),
    ('2d000000-0000-4000-8000-020000004003', 'AUDIO', 'Dr Morgan: Hello, Chloe, hello, Ben. Please sit down. You wanted to talk about your joint research project. Have you decided on a topic?
Chloe: We have, Dr Morgan. We would like to study cycling in our city, and why so few commuters cycle to work, even though the new cycle lanes are open.
Dr Morgan: That sounds promising. Originally you were going to look at public buses, weren''t you?
Ben: Yes, but there is already so much research on buses. We thought cycling would be more original, and we are both keen cyclists ourselves.
Dr Morgan: Good. Personal interest keeps you going when the work gets difficult. How do you plan to collect your data?
Chloe: First, a questionnaire. We want to ask about two hundred commuters, outside the main railway station, in the morning rush hour.
Dr Morgan: Two hundred is ambitious for two people in one term. I would suggest one hundred and fifty. The important thing is that the sample is varied, so try to talk to people of different ages.
Ben: That makes sense. We could also count the number of bicycles that pass a few points on the cycle lanes, at different times of day.
Dr Morgan: Yes, but a count only gives you numbers, not reasons. I would add a few short interviews with people who do cycle, to find out what encourages them.
Chloe: We hadn''t thought of interviews. Would they take a long time to analyse?
Dr Morgan: They can, so keep them short, about ten minutes each, and record them so that you can listen again later. Ask permission first, of course.
Ben: What about the literature review? Should we do that together?
Dr Morgan: I think you should divide it. Chloe, you were strong on this in your last assignment, so perhaps you could take responsibility for the literature review.
Chloe: I am happy to do that. I have already found several useful articles about cycling and health.
Ben: Then I will design the questionnaire, since I took the statistics course last year, and I will also analyse the results.
Dr Morgan: That is a sensible division. And who will carry out the interviews?
Chloe: I think we should do those together, so the interviewee is not nervous. And then I will write the final report, because Ben is not very fond of writing.
Ben: That is true. I will make the poster for the department''s research day instead.
Dr Morgan: Excellent. Now, there is one more thing. The ethics committee must approve every project that involves people, so you need to submit the form by the end of next week. I will check it before you send it.
Chloe: We will bring a draft on Monday, then.
Dr Morgan: Good. And remember, in your report, be honest about the limits of a small survey. That is more important than perfect results.', 'listening/practice/pt-listening-05-s3.mp3', 227, 'VALID'),
    ('2d000000-0000-4000-8000-020000004004', 'AUDIO', 'Lecturer: Good afternoon, everyone. Today I would like to talk about the history of the bicycle, which is one of the most successful inventions ever made.
Lecturer: The story really begins in eighteen seventeen, when a German inventor named Karl von Drais built a machine that he called the running machine. It had two wheels and a seat, and the whole frame was made of wood. There were no pedals at all. The rider sat on it and pushed along the ground with his feet.
Lecturer: It was nearly fifty years before anyone added pedals. In the eighteen sixties, a French company fixed pedals directly to the front wheel. This machine was called the velocipede, but people soon gave it another name, the boneshaker, because the wooden wheels, with iron round the outside, made the ride extremely uncomfortable on rough roads.
Lecturer: In the eighteen seventies, engineers realised that a larger front wheel meant a higher speed with every turn of the pedals. So they made the front wheel bigger and bigger, until it was sometimes one point five metres high. This was the penny-farthing. It was fast, but it was also dangerous, because the rider sat so high above the ground that a fall could cause serious injuries.
Lecturer: The big change came in eighteen eighty-five. An English engineer called John Kemp Starley produced a bicycle with two wheels of equal size and, most importantly, a chain that connected the pedals to the rear wheel. He called it the safety bicycle, and it is the ancestor of the bicycles we ride today. The name of his company was Rover, which later became famous for cars.
Lecturer: Three years later, in eighteen eighty-eight, a Scottish vet named John Dunlop invented the pneumatic tyre, which was filled with air. He had made it for his young son''s tricycle, but it soon made every bicycle far more comfortable, and also faster.
Lecturer: These improvements led to a real cycling boom in the eighteen nineties. Bicycles became cheaper, and ordinary families could buy them for the first time. Historians often point out that the bicycle gave women new freedom. They could travel alone, without a chaperone, and many people believe it also changed the way women dressed, because long skirts were impractical on a bicycle.
Lecturer: In the twentieth century, bicycles gained gears, brakes that worked in the rain, and lighter frames made of steel and later aluminium. Today, there are around one billion bicycles in the world, and in many cities, governments are again encouraging people to cycle.
Lecturer: Next week we will look at the invention of the motorcycle, and how it grew out of the bicycle.', 'listening/practice/pt-listening-05-s4.mp3', 204, 'VALID')
ON CONFLICT (id) DO UPDATE SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference, duration_seconds = EXCLUDED.duration_seconds;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000004001', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004003', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004004', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004005', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004006', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004007', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004008', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004009', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004010', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004011', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004012', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004013', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004014', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004015', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004016', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004017', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004018', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004019', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004020', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004021', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004022', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004023', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004024', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004025', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004026', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004027', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004028', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004029', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004030', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004031', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004032', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004033', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004039', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000004040', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000004001', '2d000000-0000-4000-8000-030000004001', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["hartley"]}'::jsonb, 1, 'The customer spells his surname as H, A, R, T, L, E, Y.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004002', '2d000000-0000-4000-8000-030000004002', 1, 'Class chosen: ______ cooking', NULL, '{"type":"FILL","accepted":["italian"]}'::jsonb, 1, 'The Thai class is full, so he books the Italian class.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004003', '2d000000-0000-4000-8000-030000004003', 1, 'Date: ______ October', NULL, '{"type":"FILL","accepted":["19","19th","nineteen","nineteenth"]}'::jsonb, 1, 'The eighteenth is a Monday; the class is on Tuesday the nineteenth.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004004', '2d000000-0000-4000-8000-030000004004', 1, 'Start time: ______ p.m.', NULL, '{"type":"FILL","accepted":["5.30","5:30","5.30pm","17:30","half past five"]}'::jsonb, 1, 'It usually starts at six, but in October it starts at half past five.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004005', '2d000000-0000-4000-8000-030000004005', 1, 'Cost per person: £ ______', NULL, '{"type":"FILL","accepted":["45","forty-five","forty five"]}'::jsonb, 1, 'The price is forty-five pounds, including ingredients.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004006', '2d000000-0000-4000-8000-030000004006', 1, 'Maximum number of students: ______', NULL, '{"type":"FILL","accepted":["12","twelve"]}'::jsonb, 1, 'Groups are limited to a maximum of twelve students.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004007', '2d000000-0000-4000-8000-030000004007', 1, 'Bring: an ______', NULL, '{"type":"FILL","accepted":["apron"]}'::jsonb, 1, 'Everything is provided except an apron.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004008', '2d000000-0000-4000-8000-030000004008', 1, 'Customer cannot eat: ______', NULL, '{"type":"FILL","accepted":["peanuts","nuts","peanut"]}'::jsonb, 1, 'He is allergic to peanuts.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004009', '2d000000-0000-4000-8000-030000004009', 1, 'Car park is behind the ______', NULL, '{"type":"FILL","accepted":["bakery"]}'::jsonb, 1, 'The small car park is behind the bakery, opposite the entrance.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004010', '2d000000-0000-4000-8000-030000004010', 1, 'Phone number: 07911 ______', NULL, '{"type":"FILL","accepted":["326705","326 705","three two six seven zero five"]}'::jsonb, 1, 'The customer gives the last digits as three two six, seven zero five.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004011', '2d000000-0000-4000-8000-030000004011', 1, 'Westbrook Botanic Garden used to be', '[{"content":"a private family estate.","optionKey":"A","sortOrder":1},{"content":"the garden of a hospital.","optionKey":"B","sortOrder":2},{"content":"a university research site.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It began as a private family estate that was later given to the city.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004012', '2d000000-0000-4000-8000-030000004012', 1, 'When can visitors enter the garden free of charge?', '[{"content":"On the first Sunday of every month","optionKey":"A","sortOrder":1},{"content":"On Wednesdays after three o''clock","optionKey":"B","sortOrder":2},{"content":"Every day, if they live nearby","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Entry is free on Wednesdays after three; local residents only get half price.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004013', '2d000000-0000-4000-8000-030000004013', 1, 'Why does the guide recommend the side entrance?', '[{"content":"The main gate is being repaired.","optionKey":"A","sortOrder":1},{"content":"School groups crowd the main gate in the morning.","optionKey":"B","sortOrder":2},{"content":"It is closer to the cafe.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Many school groups arrive at the main gate until about eleven.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004014', '2d000000-0000-4000-8000-030000004014', 1, 'What does the guide say about the giant water lily?', '[{"content":"It flowers only for two nights a year.","optionKey":"A","sortOrder":1},{"content":"It is the oldest plant in the garden.","optionKey":"B","sortOrder":2},{"content":"It can only be seen by booking in advance.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It flowers for only two nights each summer.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004015', '2d000000-0000-4000-8000-030000004015', 1, 'Visitors are asked not to', '[{"content":"use a flash when they take photographs.","optionKey":"A","sortOrder":1},{"content":"bring their dogs into the garden.","optionKey":"B","sortOrder":2},{"content":"walk on the grass.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Dogs are not allowed, except guide dogs. Flash is permitted.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004016', '2d000000-0000-4000-8000-030000004016', 1, 'Rose Garden', '[{"content":"Picnic tables","optionKey":"A","sortOrder":1},{"content":"Tropical plants","optionKey":"B","sortOrder":2},{"content":"Free guided talks","optionKey":"C","sortOrder":3},{"content":"A children''s trail","optionKey":"D","sortOrder":4},{"content":"A cafe","optionKey":"E","sortOrder":5},{"content":"Old stone statues","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'There are free guided talks every hour in the Rose Garden.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004017', '2d000000-0000-4000-8000-030000004017', 1, 'Water Garden', '[{"content":"Picnic tables","optionKey":"A","sortOrder":1},{"content":"Tropical plants","optionKey":"B","sortOrder":2},{"content":"Free guided talks","optionKey":"C","sortOrder":3},{"content":"A children''s trail","optionKey":"D","sortOrder":4},{"content":"A cafe","optionKey":"E","sortOrder":5},{"content":"Old stone statues","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'The Water Garden has a children''s trail that ends at the pond.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004018', '2d000000-0000-4000-8000-030000004018', 1, 'Herb Garden', '[{"content":"Picnic tables","optionKey":"A","sortOrder":1},{"content":"Tropical plants","optionKey":"B","sortOrder":2},{"content":"Free guided talks","optionKey":"C","sortOrder":3},{"content":"A children''s trail","optionKey":"D","sortOrder":4},{"content":"A cafe","optionKey":"E","sortOrder":5},{"content":"Old stone statues","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"F"}'::jsonb, 1, 'The Herb Garden has old stone statues from the family house.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004019', '2d000000-0000-4000-8000-030000004019', 1, 'Glasshouse', '[{"content":"Picnic tables","optionKey":"A","sortOrder":1},{"content":"Tropical plants","optionKey":"B","sortOrder":2},{"content":"Free guided talks","optionKey":"C","sortOrder":3},{"content":"A children''s trail","optionKey":"D","sortOrder":4},{"content":"A cafe","optionKey":"E","sortOrder":5},{"content":"Old stone statues","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The Glasshouse holds the collection of tropical plants.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004020', '2d000000-0000-4000-8000-030000004020', 1, 'Woodland Walk', '[{"content":"Picnic tables","optionKey":"A","sortOrder":1},{"content":"Tropical plants","optionKey":"B","sortOrder":2},{"content":"Free guided talks","optionKey":"C","sortOrder":3},{"content":"A children''s trail","optionKey":"D","sortOrder":4},{"content":"A cafe","optionKey":"E","sortOrder":5},{"content":"Old stone statues","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'There are picnic tables halfway along the Woodland Walk. The cafe is near the main gate.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004021', '2d000000-0000-4000-8000-030000004021', 1, 'Why did the students change their topic from public buses?', '[{"content":"There is already a lot of research on buses.","optionKey":"A","sortOrder":1},{"content":"Their tutor did not approve of it.","optionKey":"B","sortOrder":2},{"content":"They could not find enough data.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They said there is already so much research on buses.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004022', '2d000000-0000-4000-8000-030000004022', 1, 'How many commuters does Dr Morgan suggest they question?', '[{"content":"Fifty","optionKey":"A","sortOrder":1},{"content":"One hundred and fifty","optionKey":"B","sortOrder":2},{"content":"Two hundred","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'He thinks two hundred is too many and suggests one hundred and fifty.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004023', '2d000000-0000-4000-8000-030000004023', 1, 'What does Dr Morgan say is most important about the sample?', '[{"content":"It should be as large as possible.","optionKey":"A","sortOrder":1},{"content":"It should be collected at the weekend.","optionKey":"B","sortOrder":2},{"content":"It should include people of different ages.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'The sample needs to be varied, with people of different ages.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004024', '2d000000-0000-4000-8000-030000004024', 1, 'Why does Dr Morgan recommend interviews?', '[{"content":"They explain the reasons behind the numbers.","optionKey":"A","sortOrder":1},{"content":"They are quicker than a questionnaire.","optionKey":"B","sortOrder":2},{"content":"They are easier to analyse.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'A count gives numbers, not reasons; interviews show what encourages cyclists.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004025', '2d000000-0000-4000-8000-030000004025', 1, 'Before they begin, the students must', '[{"content":"buy recording equipment.","optionKey":"A","sortOrder":1},{"content":"get approval from the ethics committee.","optionKey":"B","sortOrder":2},{"content":"book a room for the interviews.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The ethics committee must approve the project, and the form is due next week.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004026', '2d000000-0000-4000-8000-030000004026', 1, 'Literature review', '[{"content":"Chloe","optionKey":"A","sortOrder":1},{"content":"Ben","optionKey":"B","sortOrder":2},{"content":"Both students","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Dr Morgan suggests Chloe takes the literature review.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004027', '2d000000-0000-4000-8000-030000004027', 1, 'Designing the questionnaire', '[{"content":"Chloe","optionKey":"A","sortOrder":1},{"content":"Ben","optionKey":"B","sortOrder":2},{"content":"Both students","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Ben will design the questionnaire.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004028', '2d000000-0000-4000-8000-030000004028', 1, 'Analysing the results', '[{"content":"Chloe","optionKey":"A","sortOrder":1},{"content":"Ben","optionKey":"B","sortOrder":2},{"content":"Both students","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Ben will also analyse the results.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004029', '2d000000-0000-4000-8000-030000004029', 1, 'Carrying out the interviews', '[{"content":"Chloe","optionKey":"A","sortOrder":1},{"content":"Ben","optionKey":"B","sortOrder":2},{"content":"Both students","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They will do the interviews together.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004030', '2d000000-0000-4000-8000-030000004030', 1, 'Writing the final report', '[{"content":"Chloe","optionKey":"A","sortOrder":1},{"content":"Ben","optionKey":"B","sortOrder":2},{"content":"Both students","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Chloe will write the final report; Ben makes the poster.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004031', '2d000000-0000-4000-8000-030000004031', 1, 'First machine built by von Drais in ______', NULL, '{"type":"FILL","accepted":["1817","eighteen seventeen"]}'::jsonb, 1, 'Karl von Drais built his running machine in eighteen seventeen.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004032', '2d000000-0000-4000-8000-030000004032', 1, 'Its frame was made of ______', NULL, '{"type":"FILL","accepted":["wood","wooden"]}'::jsonb, 1, 'The whole frame was made of wood.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004033', '2d000000-0000-4000-8000-030000004033', 1, 'It had no ______', NULL, '{"type":"FILL","accepted":["pedals","pedal"]}'::jsonb, 1, 'The rider pushed along the ground with his feet; there were no pedals.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004034', '2d000000-0000-4000-8000-030000004034', 1, 'In the 1860s pedals were fixed to the ______ wheel', NULL, '{"type":"FILL","accepted":["front"]}'::jsonb, 1, 'A French company fixed pedals directly to the front wheel.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004035', '2d000000-0000-4000-8000-030000004035', 1, 'Nickname of this machine: the ______', NULL, '{"type":"FILL","accepted":["boneshaker"]}'::jsonb, 1, 'People called the velocipede the boneshaker because it was so uncomfortable.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004036', '2d000000-0000-4000-8000-030000004036', 1, 'The penny-farthing''s front wheel could be ______ metres high', NULL, '{"type":"FILL","accepted":["1.5","one point five"]}'::jsonb, 1, 'The front wheel was sometimes one point five metres high.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004037', '2d000000-0000-4000-8000-030000004037', 1, 'The safety bicycle had a ______ connecting the pedals to the rear wheel', NULL, '{"type":"FILL","accepted":["chain"]}'::jsonb, 1, 'Starley''s safety bicycle used a chain to drive the rear wheel.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004038', '2d000000-0000-4000-8000-030000004038', 1, 'The safety bicycle was built by John Kemp ______', NULL, '{"type":"FILL","accepted":["starley"]}'::jsonb, 1, 'The engineer was John Kemp Starley.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004039', '2d000000-0000-4000-8000-030000004039', 1, 'John Dunlop invented the pneumatic ______', NULL, '{"type":"FILL","accepted":["tyre","tire"]}'::jsonb, 1, 'Dunlop invented the pneumatic tyre in eighteen eighty-eight.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000004040', '2d000000-0000-4000-8000-030000004040', 1, 'A cycling boom took place in the ______', NULL, '{"type":"FILL","accepted":["1890s","eighteen nineties"]}'::jsonb, 1, 'The boom came in the eighteen nineties; the bicycle also gave women new freedom.', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = published.version_id::uuid FROM (VALUES
    ('2d000000-0000-4000-8000-030000004001', '2d000000-0000-4000-8000-040000004001'),
    ('2d000000-0000-4000-8000-030000004002', '2d000000-0000-4000-8000-040000004002'),
    ('2d000000-0000-4000-8000-030000004003', '2d000000-0000-4000-8000-040000004003'),
    ('2d000000-0000-4000-8000-030000004004', '2d000000-0000-4000-8000-040000004004'),
    ('2d000000-0000-4000-8000-030000004005', '2d000000-0000-4000-8000-040000004005'),
    ('2d000000-0000-4000-8000-030000004006', '2d000000-0000-4000-8000-040000004006'),
    ('2d000000-0000-4000-8000-030000004007', '2d000000-0000-4000-8000-040000004007'),
    ('2d000000-0000-4000-8000-030000004008', '2d000000-0000-4000-8000-040000004008'),
    ('2d000000-0000-4000-8000-030000004009', '2d000000-0000-4000-8000-040000004009'),
    ('2d000000-0000-4000-8000-030000004010', '2d000000-0000-4000-8000-040000004010'),
    ('2d000000-0000-4000-8000-030000004011', '2d000000-0000-4000-8000-040000004011'),
    ('2d000000-0000-4000-8000-030000004012', '2d000000-0000-4000-8000-040000004012'),
    ('2d000000-0000-4000-8000-030000004013', '2d000000-0000-4000-8000-040000004013'),
    ('2d000000-0000-4000-8000-030000004014', '2d000000-0000-4000-8000-040000004014'),
    ('2d000000-0000-4000-8000-030000004015', '2d000000-0000-4000-8000-040000004015'),
    ('2d000000-0000-4000-8000-030000004016', '2d000000-0000-4000-8000-040000004016'),
    ('2d000000-0000-4000-8000-030000004017', '2d000000-0000-4000-8000-040000004017'),
    ('2d000000-0000-4000-8000-030000004018', '2d000000-0000-4000-8000-040000004018'),
    ('2d000000-0000-4000-8000-030000004019', '2d000000-0000-4000-8000-040000004019'),
    ('2d000000-0000-4000-8000-030000004020', '2d000000-0000-4000-8000-040000004020'),
    ('2d000000-0000-4000-8000-030000004021', '2d000000-0000-4000-8000-040000004021'),
    ('2d000000-0000-4000-8000-030000004022', '2d000000-0000-4000-8000-040000004022'),
    ('2d000000-0000-4000-8000-030000004023', '2d000000-0000-4000-8000-040000004023'),
    ('2d000000-0000-4000-8000-030000004024', '2d000000-0000-4000-8000-040000004024'),
    ('2d000000-0000-4000-8000-030000004025', '2d000000-0000-4000-8000-040000004025'),
    ('2d000000-0000-4000-8000-030000004026', '2d000000-0000-4000-8000-040000004026'),
    ('2d000000-0000-4000-8000-030000004027', '2d000000-0000-4000-8000-040000004027'),
    ('2d000000-0000-4000-8000-030000004028', '2d000000-0000-4000-8000-040000004028'),
    ('2d000000-0000-4000-8000-030000004029', '2d000000-0000-4000-8000-040000004029'),
    ('2d000000-0000-4000-8000-030000004030', '2d000000-0000-4000-8000-040000004030'),
    ('2d000000-0000-4000-8000-030000004031', '2d000000-0000-4000-8000-040000004031'),
    ('2d000000-0000-4000-8000-030000004032', '2d000000-0000-4000-8000-040000004032'),
    ('2d000000-0000-4000-8000-030000004033', '2d000000-0000-4000-8000-040000004033'),
    ('2d000000-0000-4000-8000-030000004034', '2d000000-0000-4000-8000-040000004034'),
    ('2d000000-0000-4000-8000-030000004035', '2d000000-0000-4000-8000-040000004035'),
    ('2d000000-0000-4000-8000-030000004036', '2d000000-0000-4000-8000-040000004036'),
    ('2d000000-0000-4000-8000-030000004037', '2d000000-0000-4000-8000-040000004037'),
    ('2d000000-0000-4000-8000-030000004038', '2d000000-0000-4000-8000-040000004038'),
    ('2d000000-0000-4000-8000-030000004039', '2d000000-0000-4000-8000-040000004039'),
    ('2d000000-0000-4000-8000-030000004040', '2d000000-0000-4000-8000-040000004040')
) AS published(question_id, version_id) WHERE questions.id = published.question_id::uuid;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000004001', 'PT-LISTENING-05', 'Listening Practice Test 5', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000004001', '2d000000-0000-4000-8000-050000004001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000004001' WHERE id = '2d000000-0000-4000-8000-050000004001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) VALUES
    ('2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-060000004001', 'Section 1: Booking a cooking class', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-10 as you listen.', 1),
    ('2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-060000004001', 'Section 2: A visit to a botanic garden', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 11-20 as you listen.', 2),
    ('2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-060000004001', 'Section 3: Choosing a research topic', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 21-30 as you listen.', 3),
    ('2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-060000004001', 'Section 4: The history of the bicycle', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 31-40 as you listen.', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000004001', '2d000000-0000-4000-8000-070000004001', 1, 'GAP_FILLING', 'Questions 1-10. Complete the booking form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000004002', '2d000000-0000-4000-8000-070000004002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 11-15. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000004003', '2d000000-0000-4000-8000-070000004002', 2, 'MATCHING', 'Questions 16-20. What can visitors find in each area of the garden? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000004004', '2d000000-0000-4000-8000-070000004003', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 21-25. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000004005', '2d000000-0000-4000-8000-070000004003', 2, 'MATCHING', 'Questions 26-30. Who will be responsible for each stage of the project? Choose the correct letter, A-C.'),
    ('2d000000-0000-4000-8000-090000004006', '2d000000-0000-4000-8000-070000004004', 1, 'GAP_FILLING', 'Questions 31-40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000004001', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004001', 1, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004002', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004002', 2, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004003', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004003', 3, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004004', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004004', 4, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004005', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004005', 5, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004006', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004006', 6, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004007', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004007', 7, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004008', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004008', 8, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004009', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004009', 9, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004010', '2d000000-0000-4000-8000-070000004001', '2d000000-0000-4000-8000-040000004010', 10, 1.00, '2d000000-0000-4000-8000-090000004001'),
    ('2d000000-0000-4000-8000-080000004011', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004011', 1, 1.00, '2d000000-0000-4000-8000-090000004002'),
    ('2d000000-0000-4000-8000-080000004012', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004012', 2, 1.00, '2d000000-0000-4000-8000-090000004002'),
    ('2d000000-0000-4000-8000-080000004013', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004013', 3, 1.00, '2d000000-0000-4000-8000-090000004002'),
    ('2d000000-0000-4000-8000-080000004014', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004014', 4, 1.00, '2d000000-0000-4000-8000-090000004002'),
    ('2d000000-0000-4000-8000-080000004015', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004015', 5, 1.00, '2d000000-0000-4000-8000-090000004002'),
    ('2d000000-0000-4000-8000-080000004016', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004016', 6, 1.00, '2d000000-0000-4000-8000-090000004003'),
    ('2d000000-0000-4000-8000-080000004017', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004017', 7, 1.00, '2d000000-0000-4000-8000-090000004003'),
    ('2d000000-0000-4000-8000-080000004018', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004018', 8, 1.00, '2d000000-0000-4000-8000-090000004003'),
    ('2d000000-0000-4000-8000-080000004019', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004019', 9, 1.00, '2d000000-0000-4000-8000-090000004003'),
    ('2d000000-0000-4000-8000-080000004020', '2d000000-0000-4000-8000-070000004002', '2d000000-0000-4000-8000-040000004020', 10, 1.00, '2d000000-0000-4000-8000-090000004003'),
    ('2d000000-0000-4000-8000-080000004021', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004021', 1, 1.00, '2d000000-0000-4000-8000-090000004004'),
    ('2d000000-0000-4000-8000-080000004022', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004022', 2, 1.00, '2d000000-0000-4000-8000-090000004004'),
    ('2d000000-0000-4000-8000-080000004023', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004023', 3, 1.00, '2d000000-0000-4000-8000-090000004004'),
    ('2d000000-0000-4000-8000-080000004024', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004024', 4, 1.00, '2d000000-0000-4000-8000-090000004004'),
    ('2d000000-0000-4000-8000-080000004025', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004025', 5, 1.00, '2d000000-0000-4000-8000-090000004004'),
    ('2d000000-0000-4000-8000-080000004026', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004026', 6, 1.00, '2d000000-0000-4000-8000-090000004005'),
    ('2d000000-0000-4000-8000-080000004027', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004027', 7, 1.00, '2d000000-0000-4000-8000-090000004005'),
    ('2d000000-0000-4000-8000-080000004028', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004028', 8, 1.00, '2d000000-0000-4000-8000-090000004005'),
    ('2d000000-0000-4000-8000-080000004029', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004029', 9, 1.00, '2d000000-0000-4000-8000-090000004005'),
    ('2d000000-0000-4000-8000-080000004030', '2d000000-0000-4000-8000-070000004003', '2d000000-0000-4000-8000-040000004030', 10, 1.00, '2d000000-0000-4000-8000-090000004005'),
    ('2d000000-0000-4000-8000-080000004031', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004031', 1, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004032', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004032', 2, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004033', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004033', 3, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004034', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004034', 4, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004035', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004035', 5, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004036', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004036', 6, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004037', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004037', 7, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004038', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004038', 8, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004039', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004039', 9, 1.00, '2d000000-0000-4000-8000-090000004006'),
    ('2d000000-0000-4000-8000-080000004040', '2d000000-0000-4000-8000-070000004004', '2d000000-0000-4000-8000-040000004040', 10, 1.00, '2d000000-0000-4000-8000-090000004006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000004001', '2d000000-0000-4000-8000-020000004001', '2d000000-0000-4000-8000-070000004001', 0),
    ('2d000000-0000-4000-8000-0a0000004002', '2d000000-0000-4000-8000-020000004002', '2d000000-0000-4000-8000-070000004002', 0),
    ('2d000000-0000-4000-8000-0a0000004003', '2d000000-0000-4000-8000-020000004003', '2d000000-0000-4000-8000-070000004003', 0),
    ('2d000000-0000-4000-8000-0a0000004004', '2d000000-0000-4000-8000-020000004004', '2d000000-0000-4000-8000-070000004004', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000004001', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000004001.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000004002', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000004002.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000004003', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000004003.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000004004', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000004004.webp', NULL, 'VALID')
ON CONFLICT (id) DO UPDATE SET media_reference = EXCLUDED.media_reference, validation_status = EXCLUDED.validation_status;

UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000004001' WHERE id = '2d000000-0000-4000-8000-070000004001';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000004002' WHERE id = '2d000000-0000-4000-8000-070000004002';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000004003' WHERE id = '2d000000-0000-4000-8000-070000004003';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000004004' WHERE id = '2d000000-0000-4000-8000-070000004004';

-- Listening Practice Test 6 (PT-LISTENING-06): a full-length Listening test, four parts of 10 questions.
--
-- Part 1 completes a holiday-cottage booking, Part 2 is an animal rescue open-day talk with multiple choice and matching volunteer roles to details, Part 3 is a tutorial on a sleep experiment with multiple choice and matching stages to problems, and Part 4 completes lecture notes on coral reefs.
-- The script of each part is the transcript (text_content) of its recording asset, and every answer is spoken in it. The
-- recordings are generated with edge-tts voices and uploaded to the public "media" bucket under listening/practice/ and the
-- cover photos under practice/covers/ separately; this migration does not upload them.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-020000005001', 'AUDIO', 'Agent: Lakeland Holiday Cottages, good afternoon. How can I help you?
Customer: Hello. I am looking for a cottage for a week in August, for me, my wife and our two children. I have seen one called Heron Cottage on your website.
Agent: Yes, Heron Cottage is one of our most popular. Where exactly are you hoping to stay?
Customer: Somewhere quiet, ideally close to a lake. Is it near Ambleside?
Agent: Not quite. It is a few miles from Ambleside, near the village of Thornby. The lake is a ten-minute walk from the front door.
Customer: That sounds perfect. Is it free from the fourth of August?
Agent: I''m afraid not. A family is staying until the thirteenth. The cottage is free from Saturday the fourteenth of August.
Customer: The fourteenth is fine. We would like to stay for seven nights.
Agent: Seven nights, that is one week, so you leave on Saturday the twenty-first. Now, how many bedrooms do you need?
Customer: Two would be enough, I think. Oh, but my mother may come for a few days, so perhaps three.
Agent: Heron Cottage has three bedrooms, so there will be plenty of room. Two doubles and a room with bunk beds for the children.
Customer: Wonderful. What is the price?
Agent: In August the price is four hundred and eighty pounds for the week. It is cheaper in September, but you said August.
Customer: Yes, because of school holidays. Do you need a deposit?
Agent: Yes, a deposit of twenty per cent, which you pay when you book. You pay the rest six weeks before you arrive.
Customer: Fine. We also have a dog. Do you accept pets?
Agent: We do, but only one small dog, and it must not be left alone in the cottage. Is that all right?
Customer: Yes, ours is very small. And is there a shop nearby? We will need to buy food when we arrive.
Agent: There is a small shop in the village, but it is rather expensive. The nearest supermarket is in Penrith, about twenty minutes away by car.
Customer: Penrith, I see. Thank you. Can I book now?
Agent: Certainly. I will send you an email with the details. Your booking reference is H, C, four seven one nine. Please keep it safe.
Customer: Four seven one nine. Thank you very much for your help.
Agent: You''re welcome. Enjoy your holiday.', 'listening/practice/pt-listening-06-s1.mp3', 204, 'VALID'),
    ('2d000000-0000-4000-8000-020000005002', 'AUDIO', 'Coordinator: Hello, everyone, and thank you for coming to our open day at Pennine Animal Rescue. My name is Karen, and I coordinate our volunteers. I would like to tell you about the centre, and how you could help.
Coordinator: The centre was founded about thirty years ago by a group of neighbours who were worried about stray dogs in the area. They started in a garden shed, and today we have kennels, a cattery and a small farm for larger animals. We are not run by the council, and we are not a vet''s practice. We depend on donations.
Coordinator: At the moment, we are looking after about one hundred and twenty animals. That sounds like a lot, but we have to say no to many animals, because we simply do not have the space. A new building would help, and that is what our fundraising this year is for.
Coordinator: The time we need help most is the summer holidays. Many families go away, and sadly some people leave their pets behind, so we receive more animals in July and August than in the rest of the year. If you can give a few hours in those months, it would make a real difference.
Coordinator: So what could you do? Let me explain the five roles we are recruiting for. First, dog walking. We need people who can walk dogs at weekends only, on Saturdays and Sundays, when we have the most visitors and the staff are very busy.
Coordinator: Second, cat care. You would feed the cats, clean their rooms and play with them, and we provide a half-day training session before you start. You do not need any experience.
Coordinator: Third, reception. You would answer the phone, welcome visitors and keep the records. For this role, you need to be comfortable with computers, because all our records are on the database.
Coordinator: Fourth, fundraising. This mostly means running our stall at the Saturday market. The stall opens at eight, so you would need to be there by seven to help us set it up. It is an early start, but people are very friendly.
Coordinator: And finally, transport. Volunteers take animals to and from the vet, so you need your own car. We pay for your petrol. Please note that kennel cleaning, which is not on the list today, is open only to people over eighteen.
Coordinator: If you decide to sign up today, you will be given a discount voucher for the pet shop in the town centre, and that is our way of saying thank you. There are forms on the table by the door. Thank you for listening, and enjoy the rest of the open day.', 'listening/practice/pt-listening-06-s2.mp3', 186, 'VALID'),
    ('2d000000-0000-4000-8000-020000005003', 'AUDIO', 'Dr Singh: Come in, Emily, Jack. I have read the draft of your report on the sleep experiment. Overall, it is a good start. Tell me, how did the experiment go?
Emily: Mostly well, Dr Singh. We wanted to find out whether students remember more words after a full night''s sleep than after staying awake.
Jack: The first problem was recruiting volunteers. We planned to have forty, but only twenty-two students agreed, so the groups were quite small.
Dr Singh: That is a common difficulty. With small groups, you must be careful when you interpret the results. Did you explain this in the report?
Emily: Yes, in the discussion section. Next came the sleep monitoring. We lent each volunteer a wristband to measure how long they slept, but the equipment failed for several people, so we had to leave their data out.
Dr Singh: That is unfortunate. Next time, test the equipment on yourselves the week before.
Jack: Good idea. The memory test also caused trouble. Some of the volunteers did not understand the instructions, and they wrote down the words in the wrong order, so the scores were not reliable.
Emily: I think we should have tried the test on a few friends first, to see if the instructions were clear.
Dr Singh: Yes, a pilot study would have helped. Now, what did you find in the end?
Jack: Surprisingly, the group that stayed awake remembered slightly more words than the group that slept. We expected the opposite, so we double-checked, but the figures were right.
Dr Singh: That is interesting, and unexpected results are often the most valuable, provided you report them honestly. What about the data entry?
Emily: That took far too long. We typed in the scores by hand, over two whole days, and it was boring work.
Dr Singh: Next time, use a spreadsheet and enter the data straight away, as you collect it. And the statistical analysis?
Jack: That was fine in the end. The software worked well once we had learned how to use it.
Dr Singh: Good. Now, for the final version, I would like you to add a section on what you would change, and to shorten the introduction a little. The deadline is the first of December. If you send me a draft by the twentieth of November, I will read it and give you comments.
Emily: That is helpful. We will do that. Thank you, Dr Singh.', 'listening/practice/pt-listening-06-s3.mp3', 197, 'VALID'),
    ('2d000000-0000-4000-8000-020000005004', 'AUDIO', 'Lecturer: Good morning, everyone. Today''s lecture is about coral reefs, which are among the most valuable and the most threatened habitats on our planet.
Lecturer: Let me start with a surprising fact. Coral reefs cover less than one per cent of the ocean floor, yet they are home to about twenty-five per cent of all marine species. That is why they are sometimes called the rainforests of the sea.
Lecturer: Many people think corals are plants or rocks, but a reef is actually built by tiny animals called polyps. A polyp is only a few millimetres across, but millions of them together produce a hard skeleton, and over thousands of years, those skeletons form the reef.
Lecturer: Inside the tissue of each polyp live microscopic algae. The algae use sunlight to make food, and they share it with the polyp. In return, the polyp gives them a safe home. The algae also give the coral its colour, which is why a healthy reef looks so bright.
Lecturer: This partnership is fragile. When the water becomes too warm, the polyps push the algae out. The coral loses its colour and turns white, and this is what we call bleaching. It does not kill the coral immediately, but if the water stays warm for long, the coral starves. A rise of just one degree Celsius, lasting for several weeks, can be enough to cause bleaching.
Lecturer: The largest reef on Earth is the Great Barrier Reef, off the coast of Australia. It stretches for about two thousand three hundred kilometres, and it can even be seen from space. Unfortunately, it has suffered several serious bleaching events in recent years.
Lecturer: Warming water is not the only threat. Pollution from farms and towns, and overfishing, also damage reefs. In some areas, an animal called the crown-of-thorns starfish has multiplied and eaten large areas of coral.
Lecturer: Why should we care? First, reefs protect the coast. They absorb the energy of waves before they reach the land, and so they protect homes and farms from storms. Second, more than five hundred million people depend on reefs for food and income, through fishing and tourism.
Lecturer: The good news is that reefs can recover when we protect them. In my next lecture, we will look at marine parks and what they have achieved.', 'listening/practice/pt-listening-06-s4.mp3', 188, 'VALID')
ON CONFLICT (id) DO UPDATE SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference, duration_seconds = EXCLUDED.duration_seconds;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000005001', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005003', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005004', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005005', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005006', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005007', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005008', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005009', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005010', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005011', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005012', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005013', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005014', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005015', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005016', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005017', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005018', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005019', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005020', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005021', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005022', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005023', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005024', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005025', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005026', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005027', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005028', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005029', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005030', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005031', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005032', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005033', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005039', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000005040', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000005001', '2d000000-0000-4000-8000-030000005001', 1, 'Name of cottage: ______ Cottage', NULL, '{"type":"FILL","accepted":["heron"]}'::jsonb, 1, 'The customer has seen Heron Cottage on the website.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005002', '2d000000-0000-4000-8000-030000005002', 1, 'Near the village of ______', NULL, '{"type":"FILL","accepted":["thornby"]}'::jsonb, 1, 'It is near the village of Thornby, a few miles from Ambleside.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005003', '2d000000-0000-4000-8000-030000005003', 1, 'Arrival date: ______ August', NULL, '{"type":"FILL","accepted":["14","14th","fourteen","fourteenth"]}'::jsonb, 1, 'It is not free from the fourth; it is free from Saturday the fourteenth.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005004', '2d000000-0000-4000-8000-030000005004', 1, 'Number of nights: ______', NULL, '{"type":"FILL","accepted":["7","seven"]}'::jsonb, 1, 'They want to stay for seven nights.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005005', '2d000000-0000-4000-8000-030000005005', 1, 'Number of bedrooms: ______', NULL, '{"type":"FILL","accepted":["3","three"]}'::jsonb, 1, 'The cottage has three bedrooms, which suits a possible visit by his mother.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005006', '2d000000-0000-4000-8000-030000005006', 1, 'Price for the week: £ ______', NULL, '{"type":"FILL","accepted":["480","four hundred and eighty","four hundred eighty"]}'::jsonb, 1, 'In August the price is four hundred and eighty pounds a week.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005007', '2d000000-0000-4000-8000-030000005007', 1, 'Deposit: ______ %', NULL, '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'A deposit of twenty per cent is paid when booking.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005008', '2d000000-0000-4000-8000-030000005008', 1, 'Pets: one small ______ only', NULL, '{"type":"FILL","accepted":["dog"]}'::jsonb, 1, 'Only one small dog is allowed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005009', '2d000000-0000-4000-8000-030000005009', 1, 'Nearest supermarket: in ______', NULL, '{"type":"FILL","accepted":["penrith"]}'::jsonb, 1, 'The nearest supermarket is in Penrith.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005010', '2d000000-0000-4000-8000-030000005010', 1, 'Booking reference: HC ______', NULL, '{"type":"FILL","accepted":["4719","four seven one nine"]}'::jsonb, 1, 'The reference is H, C, four seven one nine.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005011', '2d000000-0000-4000-8000-030000005011', 1, 'Who started Pennine Animal Rescue?', '[{"content":"A group of neighbours","optionKey":"A","sortOrder":1},{"content":"A local vet","optionKey":"B","sortOrder":2},{"content":"The town council","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It was founded by a group of neighbours worried about stray dogs.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005012', '2d000000-0000-4000-8000-030000005012', 1, 'How many animals does the centre look after now?', '[{"content":"About sixty","optionKey":"A","sortOrder":1},{"content":"About one hundred and twenty","optionKey":"B","sortOrder":2},{"content":"About three hundred","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The coordinator says about one hundred and twenty.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005013', '2d000000-0000-4000-8000-030000005013', 1, 'Why does the centre have to refuse many animals?', '[{"content":"There are not enough staff.","optionKey":"A","sortOrder":1},{"content":"There is not enough space.","optionKey":"B","sortOrder":2},{"content":"They are not allowed to keep certain animals.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They do not have the space; fundraising is for a new building.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005014', '2d000000-0000-4000-8000-030000005014', 1, 'When does the centre most need extra help?', '[{"content":"During the winter","optionKey":"A","sortOrder":1},{"content":"At weekends","optionKey":"B","sortOrder":2},{"content":"In the summer holidays","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'More animals arrive in July and August than the rest of the year.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005015', '2d000000-0000-4000-8000-030000005015', 1, 'What will people who sign up today receive?', '[{"content":"A free T-shirt","optionKey":"A","sortOrder":1},{"content":"A discount voucher for a pet shop","optionKey":"B","sortOrder":2},{"content":"A free ticket to the farm","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They receive a discount voucher for the pet shop in the town centre.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005016', '2d000000-0000-4000-8000-030000005016', 1, 'Dog walking', '[{"content":"Early starts","optionKey":"A","sortOrder":1},{"content":"Weekend shifts only","optionKey":"B","sortOrder":2},{"content":"Training provided","optionKey":"C","sortOrder":3},{"content":"Over 18s only","optionKey":"D","sortOrder":4},{"content":"Own car needed","optionKey":"E","sortOrder":5},{"content":"Computer skills needed","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Dog walkers are needed at weekends only.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005017', '2d000000-0000-4000-8000-030000005017', 1, 'Cat care', '[{"content":"Early starts","optionKey":"A","sortOrder":1},{"content":"Weekend shifts only","optionKey":"B","sortOrder":2},{"content":"Training provided","optionKey":"C","sortOrder":3},{"content":"Over 18s only","optionKey":"D","sortOrder":4},{"content":"Own car needed","optionKey":"E","sortOrder":5},{"content":"Computer skills needed","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'A half-day training session is provided.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005018', '2d000000-0000-4000-8000-030000005018', 1, 'Reception', '[{"content":"Early starts","optionKey":"A","sortOrder":1},{"content":"Weekend shifts only","optionKey":"B","sortOrder":2},{"content":"Training provided","optionKey":"C","sortOrder":3},{"content":"Over 18s only","optionKey":"D","sortOrder":4},{"content":"Own car needed","optionKey":"E","sortOrder":5},{"content":"Computer skills needed","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"F"}'::jsonb, 1, 'Receptionists must be comfortable with computers.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005019', '2d000000-0000-4000-8000-030000005019', 1, 'Fundraising', '[{"content":"Early starts","optionKey":"A","sortOrder":1},{"content":"Weekend shifts only","optionKey":"B","sortOrder":2},{"content":"Training provided","optionKey":"C","sortOrder":3},{"content":"Over 18s only","optionKey":"D","sortOrder":4},{"content":"Own car needed","optionKey":"E","sortOrder":5},{"content":"Computer skills needed","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The market stall needs helpers by seven, an early start.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005020', '2d000000-0000-4000-8000-030000005020', 1, 'Transport', '[{"content":"Early starts","optionKey":"A","sortOrder":1},{"content":"Weekend shifts only","optionKey":"B","sortOrder":2},{"content":"Training provided","optionKey":"C","sortOrder":3},{"content":"Over 18s only","optionKey":"D","sortOrder":4},{"content":"Own car needed","optionKey":"E","sortOrder":5},{"content":"Computer skills needed","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'Volunteers use their own car, and the centre pays for petrol.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005021', '2d000000-0000-4000-8000-030000005021', 1, 'What did the students want to find out?', '[{"content":"Whether sleep helps students remember words","optionKey":"A","sortOrder":1},{"content":"How many hours students sleep","optionKey":"B","sortOrder":2},{"content":"Whether students sleep better with a wristband","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They wanted to see if students remember more words after a full night''s sleep.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005022', '2d000000-0000-4000-8000-030000005022', 1, 'How many students took part in the experiment?', '[{"content":"Forty","optionKey":"A","sortOrder":1},{"content":"Twenty-two","optionKey":"B","sortOrder":2},{"content":"Twelve","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They hoped for forty, but only twenty-two agreed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005023', '2d000000-0000-4000-8000-030000005023', 1, 'What does Dr Singh advise about the equipment?', '[{"content":"Buy better wristbands","optionKey":"A","sortOrder":1},{"content":"Use fewer volunteers","optionKey":"B","sortOrder":2},{"content":"Test it before the experiment","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'He says to test the equipment on themselves the week before.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005024', '2d000000-0000-4000-8000-030000005024', 1, 'What was surprising about the results?', '[{"content":"The group that slept scored lower.","optionKey":"A","sortOrder":1},{"content":"Nobody remembered more than ten words.","optionKey":"B","sortOrder":2},{"content":"The scores were exactly the same.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The group that stayed awake remembered slightly more words.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005025', '2d000000-0000-4000-8000-030000005025', 1, 'What must the students do by the twentieth of November?', '[{"content":"Hand in the final report","optionKey":"A","sortOrder":1},{"content":"Send Dr Singh a draft","optionKey":"B","sortOrder":2},{"content":"Collect more data","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They should send a draft by the twentieth; the final deadline is the first of December.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005026', '2d000000-0000-4000-8000-030000005026', 1, 'Recruiting volunteers', '[{"content":"Too few participants","optionKey":"A","sortOrder":1},{"content":"Equipment failed","optionKey":"B","sortOrder":2},{"content":"Instructions were unclear","optionKey":"C","sortOrder":3},{"content":"Results were unexpected","optionKey":"D","sortOrder":4},{"content":"It took too long","optionKey":"E","sortOrder":5},{"content":"Software did not work","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Only twenty-two students agreed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005027', '2d000000-0000-4000-8000-030000005027', 1, 'Monitoring sleep', '[{"content":"Too few participants","optionKey":"A","sortOrder":1},{"content":"Equipment failed","optionKey":"B","sortOrder":2},{"content":"Instructions were unclear","optionKey":"C","sortOrder":3},{"content":"Results were unexpected","optionKey":"D","sortOrder":4},{"content":"It took too long","optionKey":"E","sortOrder":5},{"content":"Software did not work","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The wristbands failed for several volunteers.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005028', '2d000000-0000-4000-8000-030000005028', 1, 'The memory test', '[{"content":"Too few participants","optionKey":"A","sortOrder":1},{"content":"Equipment failed","optionKey":"B","sortOrder":2},{"content":"Instructions were unclear","optionKey":"C","sortOrder":3},{"content":"Results were unexpected","optionKey":"D","sortOrder":4},{"content":"It took too long","optionKey":"E","sortOrder":5},{"content":"Software did not work","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Some volunteers did not understand the instructions.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005029', '2d000000-0000-4000-8000-030000005029', 1, 'Entering the data', '[{"content":"Too few participants","optionKey":"A","sortOrder":1},{"content":"Equipment failed","optionKey":"B","sortOrder":2},{"content":"Instructions were unclear","optionKey":"C","sortOrder":3},{"content":"Results were unexpected","optionKey":"D","sortOrder":4},{"content":"It took too long","optionKey":"E","sortOrder":5},{"content":"Software did not work","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'Typing in the scores took two whole days.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005030', '2d000000-0000-4000-8000-030000005030', 1, 'Analysing the results', '[{"content":"Too few participants","optionKey":"A","sortOrder":1},{"content":"Equipment failed","optionKey":"B","sortOrder":2},{"content":"Instructions were unclear","optionKey":"C","sortOrder":3},{"content":"Results were unexpected","optionKey":"D","sortOrder":4},{"content":"It took too long","optionKey":"E","sortOrder":5},{"content":"Software did not work","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'The software worked well; the results themselves were not what they expected.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005031', '2d000000-0000-4000-8000-030000005031', 1, 'Reefs cover less than ______ % of the ocean floor', NULL, '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'Reefs cover less than one per cent of the ocean floor.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005032', '2d000000-0000-4000-8000-030000005032', 1, 'They are home to about ______ % of marine species', NULL, '{"type":"FILL","accepted":["25","twenty-five","twenty five"]}'::jsonb, 1, 'About twenty-five per cent of marine species live on reefs.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005033', '2d000000-0000-4000-8000-030000005033', 1, 'A reef is built by tiny animals called ______', NULL, '{"type":"FILL","accepted":["polyps","polyp"]}'::jsonb, 1, 'The reef is built by tiny animals called polyps.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005034', '2d000000-0000-4000-8000-030000005034', 1, 'Algae in the polyps give the coral its ______', NULL, '{"type":"FILL","accepted":["colour","color"]}'::jsonb, 1, 'The algae also give the coral its colour.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005035', '2d000000-0000-4000-8000-030000005035', 1, 'Bleaching happens when the water is too ______', NULL, '{"type":"FILL","accepted":["warm","hot"]}'::jsonb, 1, 'When the water becomes too warm, the polyps push the algae out.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005036', '2d000000-0000-4000-8000-030000005036', 1, 'A rise of only ______ degree Celsius can cause bleaching', NULL, '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'A rise of just one degree for several weeks can be enough.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005037', '2d000000-0000-4000-8000-030000005037', 1, 'The Great Barrier Reef is about ______ kilometres long', NULL, '{"type":"FILL","accepted":["2300","2,300","two thousand three hundred"]}'::jsonb, 1, 'It stretches for about two thousand three hundred kilometres.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005038', '2d000000-0000-4000-8000-030000005038', 1, 'The crown-of-thorns ______ eats coral', NULL, '{"type":"FILL","accepted":["starfish"]}'::jsonb, 1, 'The crown-of-thorns starfish has eaten large areas of coral.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005039', '2d000000-0000-4000-8000-030000005039', 1, 'Reefs protect the coast from ______', NULL, '{"type":"FILL","accepted":["waves","storms","wave"]}'::jsonb, 1, 'They absorb the energy of waves and protect land from storms.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000005040', '2d000000-0000-4000-8000-030000005040', 1, 'More than ______ million people depend on reefs', NULL, '{"type":"FILL","accepted":["500","five hundred"]}'::jsonb, 1, 'More than five hundred million people depend on reefs for food and income.', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = published.version_id::uuid FROM (VALUES
    ('2d000000-0000-4000-8000-030000005001', '2d000000-0000-4000-8000-040000005001'),
    ('2d000000-0000-4000-8000-030000005002', '2d000000-0000-4000-8000-040000005002'),
    ('2d000000-0000-4000-8000-030000005003', '2d000000-0000-4000-8000-040000005003'),
    ('2d000000-0000-4000-8000-030000005004', '2d000000-0000-4000-8000-040000005004'),
    ('2d000000-0000-4000-8000-030000005005', '2d000000-0000-4000-8000-040000005005'),
    ('2d000000-0000-4000-8000-030000005006', '2d000000-0000-4000-8000-040000005006'),
    ('2d000000-0000-4000-8000-030000005007', '2d000000-0000-4000-8000-040000005007'),
    ('2d000000-0000-4000-8000-030000005008', '2d000000-0000-4000-8000-040000005008'),
    ('2d000000-0000-4000-8000-030000005009', '2d000000-0000-4000-8000-040000005009'),
    ('2d000000-0000-4000-8000-030000005010', '2d000000-0000-4000-8000-040000005010'),
    ('2d000000-0000-4000-8000-030000005011', '2d000000-0000-4000-8000-040000005011'),
    ('2d000000-0000-4000-8000-030000005012', '2d000000-0000-4000-8000-040000005012'),
    ('2d000000-0000-4000-8000-030000005013', '2d000000-0000-4000-8000-040000005013'),
    ('2d000000-0000-4000-8000-030000005014', '2d000000-0000-4000-8000-040000005014'),
    ('2d000000-0000-4000-8000-030000005015', '2d000000-0000-4000-8000-040000005015'),
    ('2d000000-0000-4000-8000-030000005016', '2d000000-0000-4000-8000-040000005016'),
    ('2d000000-0000-4000-8000-030000005017', '2d000000-0000-4000-8000-040000005017'),
    ('2d000000-0000-4000-8000-030000005018', '2d000000-0000-4000-8000-040000005018'),
    ('2d000000-0000-4000-8000-030000005019', '2d000000-0000-4000-8000-040000005019'),
    ('2d000000-0000-4000-8000-030000005020', '2d000000-0000-4000-8000-040000005020'),
    ('2d000000-0000-4000-8000-030000005021', '2d000000-0000-4000-8000-040000005021'),
    ('2d000000-0000-4000-8000-030000005022', '2d000000-0000-4000-8000-040000005022'),
    ('2d000000-0000-4000-8000-030000005023', '2d000000-0000-4000-8000-040000005023'),
    ('2d000000-0000-4000-8000-030000005024', '2d000000-0000-4000-8000-040000005024'),
    ('2d000000-0000-4000-8000-030000005025', '2d000000-0000-4000-8000-040000005025'),
    ('2d000000-0000-4000-8000-030000005026', '2d000000-0000-4000-8000-040000005026'),
    ('2d000000-0000-4000-8000-030000005027', '2d000000-0000-4000-8000-040000005027'),
    ('2d000000-0000-4000-8000-030000005028', '2d000000-0000-4000-8000-040000005028'),
    ('2d000000-0000-4000-8000-030000005029', '2d000000-0000-4000-8000-040000005029'),
    ('2d000000-0000-4000-8000-030000005030', '2d000000-0000-4000-8000-040000005030'),
    ('2d000000-0000-4000-8000-030000005031', '2d000000-0000-4000-8000-040000005031'),
    ('2d000000-0000-4000-8000-030000005032', '2d000000-0000-4000-8000-040000005032'),
    ('2d000000-0000-4000-8000-030000005033', '2d000000-0000-4000-8000-040000005033'),
    ('2d000000-0000-4000-8000-030000005034', '2d000000-0000-4000-8000-040000005034'),
    ('2d000000-0000-4000-8000-030000005035', '2d000000-0000-4000-8000-040000005035'),
    ('2d000000-0000-4000-8000-030000005036', '2d000000-0000-4000-8000-040000005036'),
    ('2d000000-0000-4000-8000-030000005037', '2d000000-0000-4000-8000-040000005037'),
    ('2d000000-0000-4000-8000-030000005038', '2d000000-0000-4000-8000-040000005038'),
    ('2d000000-0000-4000-8000-030000005039', '2d000000-0000-4000-8000-040000005039'),
    ('2d000000-0000-4000-8000-030000005040', '2d000000-0000-4000-8000-040000005040')
) AS published(question_id, version_id) WHERE questions.id = published.question_id::uuid;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000005001', 'PT-LISTENING-06', 'Listening Practice Test 6', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000005001', '2d000000-0000-4000-8000-050000005001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000005001' WHERE id = '2d000000-0000-4000-8000-050000005001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) VALUES
    ('2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-060000005001', 'Section 1: Renting a holiday cottage', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-10 as you listen.', 1),
    ('2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-060000005001', 'Section 2: An open day at an animal rescue centre', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 11-20 as you listen.', 2),
    ('2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-060000005001', 'Section 3: Reviewing a sleep experiment', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 21-30 as you listen.', 3),
    ('2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-060000005001', 'Section 4: Coral reefs', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 31-40 as you listen.', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000005001', '2d000000-0000-4000-8000-070000005001', 1, 'GAP_FILLING', 'Questions 1-10. Complete the booking form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000005002', '2d000000-0000-4000-8000-070000005002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 11-15. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000005003', '2d000000-0000-4000-8000-070000005002', 2, 'MATCHING', 'Questions 16-20. What is needed for each volunteer role? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000005004', '2d000000-0000-4000-8000-070000005003', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 21-25. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000005005', '2d000000-0000-4000-8000-070000005003', 2, 'MATCHING', 'Questions 26-30. What problem did the students have at each stage of the experiment? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000005006', '2d000000-0000-4000-8000-070000005004', 1, 'GAP_FILLING', 'Questions 31-40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000005001', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005001', 1, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005002', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005002', 2, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005003', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005003', 3, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005004', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005004', 4, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005005', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005005', 5, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005006', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005006', 6, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005007', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005007', 7, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005008', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005008', 8, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005009', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005009', 9, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005010', '2d000000-0000-4000-8000-070000005001', '2d000000-0000-4000-8000-040000005010', 10, 1.00, '2d000000-0000-4000-8000-090000005001'),
    ('2d000000-0000-4000-8000-080000005011', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005011', 1, 1.00, '2d000000-0000-4000-8000-090000005002'),
    ('2d000000-0000-4000-8000-080000005012', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005012', 2, 1.00, '2d000000-0000-4000-8000-090000005002'),
    ('2d000000-0000-4000-8000-080000005013', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005013', 3, 1.00, '2d000000-0000-4000-8000-090000005002'),
    ('2d000000-0000-4000-8000-080000005014', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005014', 4, 1.00, '2d000000-0000-4000-8000-090000005002'),
    ('2d000000-0000-4000-8000-080000005015', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005015', 5, 1.00, '2d000000-0000-4000-8000-090000005002'),
    ('2d000000-0000-4000-8000-080000005016', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005016', 6, 1.00, '2d000000-0000-4000-8000-090000005003'),
    ('2d000000-0000-4000-8000-080000005017', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005017', 7, 1.00, '2d000000-0000-4000-8000-090000005003'),
    ('2d000000-0000-4000-8000-080000005018', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005018', 8, 1.00, '2d000000-0000-4000-8000-090000005003'),
    ('2d000000-0000-4000-8000-080000005019', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005019', 9, 1.00, '2d000000-0000-4000-8000-090000005003'),
    ('2d000000-0000-4000-8000-080000005020', '2d000000-0000-4000-8000-070000005002', '2d000000-0000-4000-8000-040000005020', 10, 1.00, '2d000000-0000-4000-8000-090000005003'),
    ('2d000000-0000-4000-8000-080000005021', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005021', 1, 1.00, '2d000000-0000-4000-8000-090000005004'),
    ('2d000000-0000-4000-8000-080000005022', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005022', 2, 1.00, '2d000000-0000-4000-8000-090000005004'),
    ('2d000000-0000-4000-8000-080000005023', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005023', 3, 1.00, '2d000000-0000-4000-8000-090000005004'),
    ('2d000000-0000-4000-8000-080000005024', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005024', 4, 1.00, '2d000000-0000-4000-8000-090000005004'),
    ('2d000000-0000-4000-8000-080000005025', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005025', 5, 1.00, '2d000000-0000-4000-8000-090000005004'),
    ('2d000000-0000-4000-8000-080000005026', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005026', 6, 1.00, '2d000000-0000-4000-8000-090000005005'),
    ('2d000000-0000-4000-8000-080000005027', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005027', 7, 1.00, '2d000000-0000-4000-8000-090000005005'),
    ('2d000000-0000-4000-8000-080000005028', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005028', 8, 1.00, '2d000000-0000-4000-8000-090000005005'),
    ('2d000000-0000-4000-8000-080000005029', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005029', 9, 1.00, '2d000000-0000-4000-8000-090000005005'),
    ('2d000000-0000-4000-8000-080000005030', '2d000000-0000-4000-8000-070000005003', '2d000000-0000-4000-8000-040000005030', 10, 1.00, '2d000000-0000-4000-8000-090000005005'),
    ('2d000000-0000-4000-8000-080000005031', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005031', 1, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005032', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005032', 2, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005033', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005033', 3, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005034', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005034', 4, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005035', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005035', 5, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005036', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005036', 6, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005037', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005037', 7, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005038', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005038', 8, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005039', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005039', 9, 1.00, '2d000000-0000-4000-8000-090000005006'),
    ('2d000000-0000-4000-8000-080000005040', '2d000000-0000-4000-8000-070000005004', '2d000000-0000-4000-8000-040000005040', 10, 1.00, '2d000000-0000-4000-8000-090000005006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000005001', '2d000000-0000-4000-8000-020000005001', '2d000000-0000-4000-8000-070000005001', 0),
    ('2d000000-0000-4000-8000-0a0000005002', '2d000000-0000-4000-8000-020000005002', '2d000000-0000-4000-8000-070000005002', 0),
    ('2d000000-0000-4000-8000-0a0000005003', '2d000000-0000-4000-8000-020000005003', '2d000000-0000-4000-8000-070000005003', 0),
    ('2d000000-0000-4000-8000-0a0000005004', '2d000000-0000-4000-8000-020000005004', '2d000000-0000-4000-8000-070000005004', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000005001', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000005001.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000005002', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000005002.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000005003', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000005003.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000005004', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000005004.webp', NULL, 'VALID')
ON CONFLICT (id) DO UPDATE SET media_reference = EXCLUDED.media_reference, validation_status = EXCLUDED.validation_status;

UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000005001' WHERE id = '2d000000-0000-4000-8000-070000005001';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000005002' WHERE id = '2d000000-0000-4000-8000-070000005002';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000005003' WHERE id = '2d000000-0000-4000-8000-070000005003';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000005004' WHERE id = '2d000000-0000-4000-8000-070000005004';

-- Listening Practice Test 7 (PT-LISTENING-07): a full-length Listening test, four parts of 10 questions.
--
-- Part 1 completes a language-course enquiry, Part 2 is a community theatre talk with multiple choice and matching rooms to features, Part 3 is a tutorial on a presentation about food waste with multiple choice and matching topics to visual aids, and Part 4 completes lecture notes on glaciers.
-- The script of each part is the transcript (text_content) of its recording asset, and every answer is spoken in it. The
-- recordings are generated with edge-tts voices and uploaded to the public "media" bucket under listening/practice/ and the
-- cover photos under practice/covers/ separately; this migration does not upload them.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-020000006001', 'AUDIO', 'Administrator: Good morning, Riverside Language School. How can I help?
Student: Hello. I would like some information about your evening courses, please. I am thinking of learning a new language.
Administrator: Of course. Let me take a few details first. What is your surname?
Student: It''s Delgado. That''s D, E, L, G, A, D, O.
Administrator: Thank you, Ms Delgado. And which language are you interested in?
Student: I was going to say Korean, but my company has an office in Tokyo, so I have decided on Japanese.
Administrator: Japanese is very popular at the moment. Have you studied it before?
Student: No, I have never studied it, so I need the beginner level.
Administrator: Then you want our beginner course. The next one starts on the ninth of September.
Student: The ninth. Is that a Monday?
Administrator: It is a Tuesday. The classes are on Tuesdays and Thursdays, so you would come twice a week.
Student: Thursdays are fine for me. What time are the lessons?
Administrator: There is an afternoon group at five, but most working people prefer the evening group, which starts at seven and finishes at nine.
Student: Seven o''clock is perfect, as I finish work at six. How long does the course last?
Administrator: It runs for ten weeks. Some students then continue to the next level in the winter.
Student: And how much does it cost?
Administrator: The fee is two hundred and sixty pounds, and that includes the registration. The textbook is separate, and it costs twenty-two pounds. You can buy it at the school office.
Student: That is fine. Where will the class be?
Administrator: In Room fourteen, on the second floor. The lift is next to the reception desk.
Student: Thank you. How do I register?
Administrator: You can register online, or you can come to the school with your student card, if you have one. What is the number on it, so that I can save you a place?
Student: It''s seven seven three zero.
Administrator: Thank you. Your place is saved for three days. We look forward to seeing you, Ms Delgado.', 'listening/practice/pt-listening-07-s1.mp3', 184, 'VALID'),
    ('2d000000-0000-4000-8000-020000006002', 'AUDIO', 'Manager: Welcome, everyone, and thank you for coming along this evening. I am Paul, the manager of the Elm Street Community Theatre, and I would like to tell you a little about us before I show you around.
Manager: The building itself has an interesting history. Many people assume it was a church, because of the tall windows, but it was actually a cinema, which opened in nineteen thirty-four and closed in the eighties. The community bought it in nineteen ninety-eight, and we have been putting on plays ever since.
Manager: Every year we stage six productions, roughly one every two months. We also host visiting companies and a children''s festival in the summer. The theatre seats two hundred and twenty people, and we are almost always full for our big shows.
Manager: Our most successful production last year was the musical in November. It sold out for ten nights, and we raised enough money to replace the lighting. The comedy in the spring also did well, but not nearly as well as the musical. The pantomime at Christmas is always popular with families.
Manager: Everything here is done by volunteers, and we are always looking for new people, not only actors but also people to help with costumes, lighting and tickets. If you would like to take part, you do not need to book an audition. Just come to our open session on Saturday mornings, and we will find something for you.
Manager: Now, let me describe the building so that you know where everything is. As you come in, you are in the foyer. We use it for free exhibitions by local artists, and the pictures change every month, so do take a look.
Manager: On the left is the box office. You can collect tickets there that you have booked online or by telephone, and it is open from ten o''clock each day.
Manager: Through the double doors is the main hall, where all the performances take place. Upstairs there is a balcony, and at the back of the balcony we have a quiet area with glass windows, where parents with babies can watch the show without disturbing other people.
Manager: Behind the stage you will find the studio, which is a smaller room that we use for rehearsals. Our children''s workshops are held in the main hall on Saturdays, not in the studio.
Manager: And last of all, the wardrobe, down the corridor on the right. We have hundreds of costumes there, and members of the public can hire them for parties for a small fee. It is very popular at Halloween.
Manager: Right, if you would like to follow me, I will take you backstage, and then we can have a cup of tea.', 'listening/practice/pt-listening-07-s2.mp3', 193, 'VALID'),
    ('2d000000-0000-4000-8000-020000006003', 'AUDIO', 'Professor Collins: Good morning, Mia, Omar. Your presentation on food waste is next Thursday, so I thought we should look at your plan together. Where would you like to start?
Mia: We have chosen to focus on supermarkets. We read that about a third of all food produced in the world is thrown away, and we wanted to find out what shops can do about it.
Professor Collins: A third is the figure that is usually quoted. Be careful to say where it comes from, because some reports give a lower number.
Omar: We will name the report and the year. We are using the one published by the United Nations.
Professor Collins: Good. How long will the presentation last?
Mia: We have been given fifteen minutes, with five minutes for questions at the end.
Professor Collins: Then do not try to include too much. I would suggest no more than ten slides. Now, tell me about the structure.
Omar: We begin with the causes of food waste. Mia wants to show a chart with the amount of food thrown away at each stage, from the farm to the home.
Mia: Yes, a chart is clearer than a table of numbers. Next, we compare examples from other countries. I thought a map would show clearly which countries waste the most and the least.
Professor Collins: A map is a good choice for that. And then?
Omar: After that, we describe what supermarkets are doing. We have found a case study of a French chain that donates unsold food to charities, because a law in France requires it.
Professor Collins: Excellent. A real example is more convincing than a general statement.
Mia: Then there is customer behaviour. We want to show a short video of shoppers choosing fruit, and why they leave the vegetables that look strange. It is only thirty seconds, but it is quite funny.
Professor Collins: That should get the audience''s attention. Do check that the video file works on the university computers.
Omar: We will test it tomorrow. And at the end, our recommendations. We have a quotation from a famous chef, which says that nothing should be thrown away if it can be cooked.
Professor Collins: A quotation is a nice way to finish. I do have one concern, though. You must not read from your slides. Practise until you can speak from short notes.
Mia: We have a rehearsal on Tuesday, with some of our classmates as the audience.
Professor Collins: Very sensible. Come and see me again after the rehearsal if you need any help. Good luck, both of you.', 'listening/practice/pt-listening-07-s3.mp3', 203, 'VALID'),
    ('2d000000-0000-4000-8000-020000006004', 'AUDIO', 'Lecturer: Good afternoon, everyone. Today we will look at glaciers, and how these huge rivers of ice have shaped much of the landscape we see around us.
Lecturer: First, some basic facts. At present, glaciers cover about ten per cent of the land surface of the Earth, and most of that ice is in Antarctica and Greenland. A glacier begins when snow falls year after year in a cold place and does not melt. The weight of new snow presses the older snow underneath into solid ice.
Lecturer: Although the ice looks solid, a glacier is never still. Under its own weight, it moves slowly downhill. Most glaciers move only a few metres a day, but some, in Greenland, can move much faster than that.
Lecturer: Now, how does a glacier change the land? The most important process is erosion. As the ice moves, it picks up stones and rocks from the ground, and these become frozen into the bottom of the glacier. They then act like sandpaper, and scratch and grind the rock beneath, so that the valley floor becomes deeper and wider.
Lecturer: The result is easy to recognise. A valley made by a river has a shape like the letter V, but a valley carved by a glacier has the shape of the letter U, with steep sides and a wide, flat bottom. If you have visited the mountains in Switzerland or Scotland, you have probably seen many of them.
Lecturer: At the head of a glacier, the ice often cuts a bowl-shaped hollow in the mountain. Geographers call it a cirque. When the ice melts, the cirque often holds a small, round lake.
Lecturer: Glaciers also carry material over very long distances. Sometimes, huge boulders are left in places where the rock is completely different, because the ice moved them far from home. These are known as erratics, and they are a good clue that a glacier was once there.
Lecturer: In some coastal areas, the glacial valleys are so deep that, when the ice melts, the valley fills with water from the sea. These long, narrow inlets are called fjords. Norway is the most famous country for them.
Lecturer: Sadly, most of the world''s glaciers are now shrinking, because of rising temperatures. This matters not only for the landscape, but also for the millions of people who depend on glacier water for drinking and farming. We will discuss this in more detail next week.', 'listening/practice/pt-listening-07-s4.mp3', 191, 'VALID')
ON CONFLICT (id) DO UPDATE SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference, duration_seconds = EXCLUDED.duration_seconds;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000006001', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006003', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006004', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006005', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006006', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006007', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006008', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006009', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006010', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006011', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006012', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006013', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006014', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006015', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006016', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006017', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006018', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006019', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006020', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006021', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006022', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006023', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006024', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006025', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006026', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006027', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006028', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006029', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006030', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006031', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006032', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006033', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006039', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000006040', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000006001', '2d000000-0000-4000-8000-030000006001', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["delgado"]}'::jsonb, 1, 'The student spells her surname D, E, L, G, A, D, O.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006002', '2d000000-0000-4000-8000-030000006002', 1, 'Language: ______', NULL, '{"type":"FILL","accepted":["japanese"]}'::jsonb, 1, 'She first says Korean, but decides on Japanese because of the Tokyo office.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006003', '2d000000-0000-4000-8000-030000006003', 1, 'Level: ______', NULL, '{"type":"FILL","accepted":["beginner","beginners"]}'::jsonb, 1, 'She has never studied the language, so she needs the beginner level.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006004', '2d000000-0000-4000-8000-030000006004', 1, 'Course starts: ______ September', NULL, '{"type":"FILL","accepted":["9","9th","nine","ninth"]}'::jsonb, 1, 'The next beginner course starts on the ninth of September.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006005', '2d000000-0000-4000-8000-030000006005', 1, 'Classes on Tuesdays and ______', NULL, '{"type":"FILL","accepted":["thursday","thursdays"]}'::jsonb, 1, 'The classes are on Tuesdays and Thursdays.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006006', '2d000000-0000-4000-8000-030000006006', 1, 'Evening class starts at ______ p.m.', NULL, '{"type":"FILL","accepted":["7","7.00","7pm","seven","seven o''clock","19:00"]}'::jsonb, 1, 'The evening group starts at seven; the afternoon group is at five.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006007', '2d000000-0000-4000-8000-030000006007', 1, 'Length of course: ______ weeks', NULL, '{"type":"FILL","accepted":["10","ten"]}'::jsonb, 1, 'The course runs for ten weeks.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006008', '2d000000-0000-4000-8000-030000006008', 1, 'Course fee: £ ______', NULL, '{"type":"FILL","accepted":["260","two hundred and sixty","two hundred sixty"]}'::jsonb, 1, 'The fee is two hundred and sixty pounds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006009', '2d000000-0000-4000-8000-030000006009', 1, 'Textbook: £ ______', NULL, '{"type":"FILL","accepted":["22","twenty-two","twenty two"]}'::jsonb, 1, 'The textbook costs twenty-two pounds and is bought separately.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006010', '2d000000-0000-4000-8000-030000006010', 1, 'Student card number: ______', NULL, '{"type":"FILL","accepted":["7730","seven seven three zero"]}'::jsonb, 1, 'The student gives her card number as seven seven three zero.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006011', '2d000000-0000-4000-8000-030000006011', 1, 'The theatre building was originally', '[{"content":"a church.","optionKey":"A","sortOrder":1},{"content":"a cinema.","optionKey":"B","sortOrder":2},{"content":"a school.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'It looks like a church but was a cinema.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006012', '2d000000-0000-4000-8000-030000006012', 1, 'How many productions does the theatre stage each year?', '[{"content":"Four","optionKey":"A","sortOrder":1},{"content":"Six","optionKey":"B","sortOrder":2},{"content":"Twelve","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'It stages six productions a year, one every two months.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006013', '2d000000-0000-4000-8000-030000006013', 1, 'How many seats does the theatre have?', '[{"content":"One hundred and twenty","optionKey":"A","sortOrder":1},{"content":"Two hundred and twenty","optionKey":"B","sortOrder":2},{"content":"Three hundred and fifty","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The theatre seats two hundred and twenty people.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006014', '2d000000-0000-4000-8000-030000006014', 1, 'Which production was the most successful last year?', '[{"content":"The pantomime","optionKey":"A","sortOrder":1},{"content":"The comedy","optionKey":"B","sortOrder":2},{"content":"The musical","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'The musical sold out for ten nights.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006015', '2d000000-0000-4000-8000-030000006015', 1, 'How can new volunteers get involved?', '[{"content":"By booking an audition","optionKey":"A","sortOrder":1},{"content":"By emailing the director","optionKey":"B","sortOrder":2},{"content":"By coming to the Saturday open session","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They only need to come to the open session on Saturday mornings.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006016', '2d000000-0000-4000-8000-030000006016', 1, 'Studio', '[{"content":"Used for rehearsals","optionKey":"A","sortOrder":1},{"content":"Costumes can be hired","optionKey":"B","sortOrder":2},{"content":"Tickets are collected","optionKey":"C","sortOrder":3},{"content":"Free art exhibitions","optionKey":"D","sortOrder":4},{"content":"Quiet area for parents with babies","optionKey":"E","sortOrder":5},{"content":"Workshops for children","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The studio is a smaller room used for rehearsals.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006017', '2d000000-0000-4000-8000-030000006017', 1, 'Wardrobe', '[{"content":"Used for rehearsals","optionKey":"A","sortOrder":1},{"content":"Costumes can be hired","optionKey":"B","sortOrder":2},{"content":"Tickets are collected","optionKey":"C","sortOrder":3},{"content":"Free art exhibitions","optionKey":"D","sortOrder":4},{"content":"Quiet area for parents with babies","optionKey":"E","sortOrder":5},{"content":"Workshops for children","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Members of the public can hire costumes from the wardrobe.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006018', '2d000000-0000-4000-8000-030000006018', 1, 'Box office', '[{"content":"Used for rehearsals","optionKey":"A","sortOrder":1},{"content":"Costumes can be hired","optionKey":"B","sortOrder":2},{"content":"Tickets are collected","optionKey":"C","sortOrder":3},{"content":"Free art exhibitions","optionKey":"D","sortOrder":4},{"content":"Quiet area for parents with babies","optionKey":"E","sortOrder":5},{"content":"Workshops for children","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Booked tickets are collected at the box office.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006019', '2d000000-0000-4000-8000-030000006019', 1, 'Foyer', '[{"content":"Used for rehearsals","optionKey":"A","sortOrder":1},{"content":"Costumes can be hired","optionKey":"B","sortOrder":2},{"content":"Tickets are collected","optionKey":"C","sortOrder":3},{"content":"Free art exhibitions","optionKey":"D","sortOrder":4},{"content":"Quiet area for parents with babies","optionKey":"E","sortOrder":5},{"content":"Workshops for children","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'The foyer hosts free exhibitions by local artists.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006020', '2d000000-0000-4000-8000-030000006020', 1, 'Balcony', '[{"content":"Used for rehearsals","optionKey":"A","sortOrder":1},{"content":"Costumes can be hired","optionKey":"B","sortOrder":2},{"content":"Tickets are collected","optionKey":"C","sortOrder":3},{"content":"Free art exhibitions","optionKey":"D","sortOrder":4},{"content":"Quiet area for parents with babies","optionKey":"E","sortOrder":5},{"content":"Workshops for children","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'At the back of the balcony is a quiet area for parents with babies. The children''s workshops are in the main hall.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006021', '2d000000-0000-4000-8000-030000006021', 1, 'What do the students want to find out in their presentation?', '[{"content":"How supermarkets can reduce food waste","optionKey":"A","sortOrder":1},{"content":"Why food is so expensive","optionKey":"B","sortOrder":2},{"content":"How farmers can grow more food","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They focus on what shops can do about food waste.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006022', '2d000000-0000-4000-8000-030000006022', 1, 'What does Professor Collins say about the figure of one third?', '[{"content":"It is too high.","optionKey":"A","sortOrder":1},{"content":"Its source should be named.","optionKey":"B","sortOrder":2},{"content":"It should not be used.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'He asks them to say where the figure comes from, because some reports give a lower number.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006023', '2d000000-0000-4000-8000-030000006023', 1, 'How many minutes will the presentation itself last?', '[{"content":"Five","optionKey":"A","sortOrder":1},{"content":"Ten","optionKey":"B","sortOrder":2},{"content":"Fifteen","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They have fifteen minutes, plus five for questions.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006024', '2d000000-0000-4000-8000-030000006024', 1, 'What is Professor Collins'' advice about the slides?', '[{"content":"Use no more than ten.","optionKey":"A","sortOrder":1},{"content":"Use as many pictures as possible.","optionKey":"B","sortOrder":2},{"content":"Read the text aloud.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'He suggests no more than ten slides and warns against reading from them.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006025', '2d000000-0000-4000-8000-030000006025', 1, 'What will the students do on Tuesday?', '[{"content":"Meet the professor","optionKey":"A","sortOrder":1},{"content":"Hand in their slides","optionKey":"B","sortOrder":2},{"content":"Rehearse in front of classmates","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They have a rehearsal on Tuesday, with classmates as the audience.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006026', '2d000000-0000-4000-8000-030000006026', 1, 'Causes of food waste', '[{"content":"A chart","optionKey":"A","sortOrder":1},{"content":"A short video","optionKey":"B","sortOrder":2},{"content":"A case study","optionKey":"C","sortOrder":3},{"content":"A quotation","optionKey":"D","sortOrder":4},{"content":"A photograph","optionKey":"E","sortOrder":5},{"content":"A map","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'A chart will show the food thrown away at each stage.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006027', '2d000000-0000-4000-8000-030000006027', 1, 'Examples from other countries', '[{"content":"A chart","optionKey":"A","sortOrder":1},{"content":"A short video","optionKey":"B","sortOrder":2},{"content":"A case study","optionKey":"C","sortOrder":3},{"content":"A quotation","optionKey":"D","sortOrder":4},{"content":"A photograph","optionKey":"E","sortOrder":5},{"content":"A map","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"F"}'::jsonb, 1, 'A map will show which countries waste the most and the least.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006028', '2d000000-0000-4000-8000-030000006028', 1, 'What supermarkets are doing', '[{"content":"A chart","optionKey":"A","sortOrder":1},{"content":"A short video","optionKey":"B","sortOrder":2},{"content":"A case study","optionKey":"C","sortOrder":3},{"content":"A quotation","optionKey":"D","sortOrder":4},{"content":"A photograph","optionKey":"E","sortOrder":5},{"content":"A map","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They will present a case study of a French supermarket chain.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006029', '2d000000-0000-4000-8000-030000006029', 1, 'Customer behaviour', '[{"content":"A chart","optionKey":"A","sortOrder":1},{"content":"A short video","optionKey":"B","sortOrder":2},{"content":"A case study","optionKey":"C","sortOrder":3},{"content":"A quotation","optionKey":"D","sortOrder":4},{"content":"A photograph","optionKey":"E","sortOrder":5},{"content":"A map","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'A thirty-second video will show shoppers choosing fruit.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006030', '2d000000-0000-4000-8000-030000006030', 1, 'Recommendations', '[{"content":"A chart","optionKey":"A","sortOrder":1},{"content":"A short video","optionKey":"B","sortOrder":2},{"content":"A case study","optionKey":"C","sortOrder":3},{"content":"A quotation","optionKey":"D","sortOrder":4},{"content":"A photograph","optionKey":"E","sortOrder":5},{"content":"A map","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'They will finish with a quotation from a famous chef.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006031', '2d000000-0000-4000-8000-030000006031', 1, 'Glaciers cover about ______ % of the Earth''s land', NULL, '{"type":"FILL","accepted":["10","ten"]}'::jsonb, 1, 'Glaciers cover about ten per cent of the land surface.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006032', '2d000000-0000-4000-8000-030000006032', 1, 'The weight of new snow turns older snow into ______', NULL, '{"type":"FILL","accepted":["ice"]}'::jsonb, 1, 'The weight presses the older snow into solid ice.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006033', '2d000000-0000-4000-8000-030000006033', 1, 'A glacier moves downhill because of its own ______', NULL, '{"type":"FILL","accepted":["weight"]}'::jsonb, 1, 'Under its own weight it moves slowly downhill.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006034', '2d000000-0000-4000-8000-030000006034', 1, 'Most glaciers move a few ______ a day', NULL, '{"type":"FILL","accepted":["metres","meters","metre","meter"]}'::jsonb, 1, 'Most glaciers move only a few metres a day.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006035', '2d000000-0000-4000-8000-030000006035', 1, 'Rocks frozen in the ice act like ______', NULL, '{"type":"FILL","accepted":["sandpaper"]}'::jsonb, 1, 'The stones in the ice act like sandpaper on the rock below.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006036', '2d000000-0000-4000-8000-030000006036', 1, 'A glacial valley has the shape of the letter ______', NULL, '{"type":"FILL","accepted":["u"]}'::jsonb, 1, 'A glacial valley is U-shaped, a river valley V-shaped.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006037', '2d000000-0000-4000-8000-030000006037', 1, 'A bowl-shaped hollow cut by the ice is a ______', NULL, '{"type":"FILL","accepted":["cirque"]}'::jsonb, 1, 'The bowl-shaped hollow at the head of a glacier is called a cirque.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006038', '2d000000-0000-4000-8000-030000006038', 1, 'Boulders moved far from home are called ______', NULL, '{"type":"FILL","accepted":["erratics","erratic"]}'::jsonb, 1, 'They are known as erratics.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006039', '2d000000-0000-4000-8000-030000006039', 1, 'Fjords are glacial valleys filled by water from the ______', NULL, '{"type":"FILL","accepted":["sea"]}'::jsonb, 1, 'When the ice melts, the deep valley fills with water from the sea.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000006040', '2d000000-0000-4000-8000-030000006040', 1, 'Glaciers are shrinking because of rising ______', NULL, '{"type":"FILL","accepted":["temperatures","temperature"]}'::jsonb, 1, 'Most glaciers are shrinking because of rising temperatures.', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = published.version_id::uuid FROM (VALUES
    ('2d000000-0000-4000-8000-030000006001', '2d000000-0000-4000-8000-040000006001'),
    ('2d000000-0000-4000-8000-030000006002', '2d000000-0000-4000-8000-040000006002'),
    ('2d000000-0000-4000-8000-030000006003', '2d000000-0000-4000-8000-040000006003'),
    ('2d000000-0000-4000-8000-030000006004', '2d000000-0000-4000-8000-040000006004'),
    ('2d000000-0000-4000-8000-030000006005', '2d000000-0000-4000-8000-040000006005'),
    ('2d000000-0000-4000-8000-030000006006', '2d000000-0000-4000-8000-040000006006'),
    ('2d000000-0000-4000-8000-030000006007', '2d000000-0000-4000-8000-040000006007'),
    ('2d000000-0000-4000-8000-030000006008', '2d000000-0000-4000-8000-040000006008'),
    ('2d000000-0000-4000-8000-030000006009', '2d000000-0000-4000-8000-040000006009'),
    ('2d000000-0000-4000-8000-030000006010', '2d000000-0000-4000-8000-040000006010'),
    ('2d000000-0000-4000-8000-030000006011', '2d000000-0000-4000-8000-040000006011'),
    ('2d000000-0000-4000-8000-030000006012', '2d000000-0000-4000-8000-040000006012'),
    ('2d000000-0000-4000-8000-030000006013', '2d000000-0000-4000-8000-040000006013'),
    ('2d000000-0000-4000-8000-030000006014', '2d000000-0000-4000-8000-040000006014'),
    ('2d000000-0000-4000-8000-030000006015', '2d000000-0000-4000-8000-040000006015'),
    ('2d000000-0000-4000-8000-030000006016', '2d000000-0000-4000-8000-040000006016'),
    ('2d000000-0000-4000-8000-030000006017', '2d000000-0000-4000-8000-040000006017'),
    ('2d000000-0000-4000-8000-030000006018', '2d000000-0000-4000-8000-040000006018'),
    ('2d000000-0000-4000-8000-030000006019', '2d000000-0000-4000-8000-040000006019'),
    ('2d000000-0000-4000-8000-030000006020', '2d000000-0000-4000-8000-040000006020'),
    ('2d000000-0000-4000-8000-030000006021', '2d000000-0000-4000-8000-040000006021'),
    ('2d000000-0000-4000-8000-030000006022', '2d000000-0000-4000-8000-040000006022'),
    ('2d000000-0000-4000-8000-030000006023', '2d000000-0000-4000-8000-040000006023'),
    ('2d000000-0000-4000-8000-030000006024', '2d000000-0000-4000-8000-040000006024'),
    ('2d000000-0000-4000-8000-030000006025', '2d000000-0000-4000-8000-040000006025'),
    ('2d000000-0000-4000-8000-030000006026', '2d000000-0000-4000-8000-040000006026'),
    ('2d000000-0000-4000-8000-030000006027', '2d000000-0000-4000-8000-040000006027'),
    ('2d000000-0000-4000-8000-030000006028', '2d000000-0000-4000-8000-040000006028'),
    ('2d000000-0000-4000-8000-030000006029', '2d000000-0000-4000-8000-040000006029'),
    ('2d000000-0000-4000-8000-030000006030', '2d000000-0000-4000-8000-040000006030'),
    ('2d000000-0000-4000-8000-030000006031', '2d000000-0000-4000-8000-040000006031'),
    ('2d000000-0000-4000-8000-030000006032', '2d000000-0000-4000-8000-040000006032'),
    ('2d000000-0000-4000-8000-030000006033', '2d000000-0000-4000-8000-040000006033'),
    ('2d000000-0000-4000-8000-030000006034', '2d000000-0000-4000-8000-040000006034'),
    ('2d000000-0000-4000-8000-030000006035', '2d000000-0000-4000-8000-040000006035'),
    ('2d000000-0000-4000-8000-030000006036', '2d000000-0000-4000-8000-040000006036'),
    ('2d000000-0000-4000-8000-030000006037', '2d000000-0000-4000-8000-040000006037'),
    ('2d000000-0000-4000-8000-030000006038', '2d000000-0000-4000-8000-040000006038'),
    ('2d000000-0000-4000-8000-030000006039', '2d000000-0000-4000-8000-040000006039'),
    ('2d000000-0000-4000-8000-030000006040', '2d000000-0000-4000-8000-040000006040')
) AS published(question_id, version_id) WHERE questions.id = published.question_id::uuid;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000006001', 'PT-LISTENING-07', 'Listening Practice Test 7', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000006001', '2d000000-0000-4000-8000-050000006001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000006001' WHERE id = '2d000000-0000-4000-8000-050000006001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) VALUES
    ('2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-060000006001', 'Section 1: Enquiring about a language course', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-10 as you listen.', 1),
    ('2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-060000006001', 'Section 2: A community theatre', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 11-20 as you listen.', 2),
    ('2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-060000006001', 'Section 3: A presentation on food waste', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 21-30 as you listen.', 3),
    ('2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-060000006001', 'Section 4: How glaciers shape the landscape', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 31-40 as you listen.', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000006001', '2d000000-0000-4000-8000-070000006001', 1, 'GAP_FILLING', 'Questions 1-10. Complete the enquiry form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000006002', '2d000000-0000-4000-8000-070000006002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 11-15. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000006003', '2d000000-0000-4000-8000-070000006002', 2, 'MATCHING', 'Questions 16-20. What is found in each part of the theatre? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000006004', '2d000000-0000-4000-8000-070000006003', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 21-25. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000006005', '2d000000-0000-4000-8000-070000006003', 2, 'MATCHING', 'Questions 26-30. Which visual aid will the students use for each part of the presentation? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000006006', '2d000000-0000-4000-8000-070000006004', 1, 'GAP_FILLING', 'Questions 31-40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000006001', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006001', 1, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006002', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006002', 2, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006003', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006003', 3, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006004', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006004', 4, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006005', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006005', 5, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006006', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006006', 6, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006007', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006007', 7, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006008', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006008', 8, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006009', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006009', 9, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006010', '2d000000-0000-4000-8000-070000006001', '2d000000-0000-4000-8000-040000006010', 10, 1.00, '2d000000-0000-4000-8000-090000006001'),
    ('2d000000-0000-4000-8000-080000006011', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006011', 1, 1.00, '2d000000-0000-4000-8000-090000006002'),
    ('2d000000-0000-4000-8000-080000006012', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006012', 2, 1.00, '2d000000-0000-4000-8000-090000006002'),
    ('2d000000-0000-4000-8000-080000006013', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006013', 3, 1.00, '2d000000-0000-4000-8000-090000006002'),
    ('2d000000-0000-4000-8000-080000006014', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006014', 4, 1.00, '2d000000-0000-4000-8000-090000006002'),
    ('2d000000-0000-4000-8000-080000006015', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006015', 5, 1.00, '2d000000-0000-4000-8000-090000006002'),
    ('2d000000-0000-4000-8000-080000006016', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006016', 6, 1.00, '2d000000-0000-4000-8000-090000006003'),
    ('2d000000-0000-4000-8000-080000006017', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006017', 7, 1.00, '2d000000-0000-4000-8000-090000006003'),
    ('2d000000-0000-4000-8000-080000006018', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006018', 8, 1.00, '2d000000-0000-4000-8000-090000006003'),
    ('2d000000-0000-4000-8000-080000006019', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006019', 9, 1.00, '2d000000-0000-4000-8000-090000006003'),
    ('2d000000-0000-4000-8000-080000006020', '2d000000-0000-4000-8000-070000006002', '2d000000-0000-4000-8000-040000006020', 10, 1.00, '2d000000-0000-4000-8000-090000006003'),
    ('2d000000-0000-4000-8000-080000006021', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006021', 1, 1.00, '2d000000-0000-4000-8000-090000006004'),
    ('2d000000-0000-4000-8000-080000006022', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006022', 2, 1.00, '2d000000-0000-4000-8000-090000006004'),
    ('2d000000-0000-4000-8000-080000006023', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006023', 3, 1.00, '2d000000-0000-4000-8000-090000006004'),
    ('2d000000-0000-4000-8000-080000006024', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006024', 4, 1.00, '2d000000-0000-4000-8000-090000006004'),
    ('2d000000-0000-4000-8000-080000006025', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006025', 5, 1.00, '2d000000-0000-4000-8000-090000006004'),
    ('2d000000-0000-4000-8000-080000006026', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006026', 6, 1.00, '2d000000-0000-4000-8000-090000006005'),
    ('2d000000-0000-4000-8000-080000006027', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006027', 7, 1.00, '2d000000-0000-4000-8000-090000006005'),
    ('2d000000-0000-4000-8000-080000006028', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006028', 8, 1.00, '2d000000-0000-4000-8000-090000006005'),
    ('2d000000-0000-4000-8000-080000006029', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006029', 9, 1.00, '2d000000-0000-4000-8000-090000006005'),
    ('2d000000-0000-4000-8000-080000006030', '2d000000-0000-4000-8000-070000006003', '2d000000-0000-4000-8000-040000006030', 10, 1.00, '2d000000-0000-4000-8000-090000006005'),
    ('2d000000-0000-4000-8000-080000006031', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006031', 1, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006032', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006032', 2, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006033', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006033', 3, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006034', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006034', 4, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006035', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006035', 5, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006036', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006036', 6, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006037', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006037', 7, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006038', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006038', 8, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006039', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006039', 9, 1.00, '2d000000-0000-4000-8000-090000006006'),
    ('2d000000-0000-4000-8000-080000006040', '2d000000-0000-4000-8000-070000006004', '2d000000-0000-4000-8000-040000006040', 10, 1.00, '2d000000-0000-4000-8000-090000006006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000006001', '2d000000-0000-4000-8000-020000006001', '2d000000-0000-4000-8000-070000006001', 0),
    ('2d000000-0000-4000-8000-0a0000006002', '2d000000-0000-4000-8000-020000006002', '2d000000-0000-4000-8000-070000006002', 0),
    ('2d000000-0000-4000-8000-0a0000006003', '2d000000-0000-4000-8000-020000006003', '2d000000-0000-4000-8000-070000006003', 0),
    ('2d000000-0000-4000-8000-0a0000006004', '2d000000-0000-4000-8000-020000006004', '2d000000-0000-4000-8000-070000006004', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000006001', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000006001.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000006002', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000006002.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000006003', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000006003.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000006004', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000006004.webp', NULL, 'VALID')
ON CONFLICT (id) DO UPDATE SET media_reference = EXCLUDED.media_reference, validation_status = EXCLUDED.validation_status;

UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000006001' WHERE id = '2d000000-0000-4000-8000-070000006001';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000006002' WHERE id = '2d000000-0000-4000-8000-070000006002';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000006003' WHERE id = '2d000000-0000-4000-8000-070000006003';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000006004' WHERE id = '2d000000-0000-4000-8000-070000006004';

-- Listening Practice Test 8 (PT-LISTENING-08): a full-length Listening test, four parts of 10 questions.
--
-- Part 1 completes a lost-property report, Part 2 is a local history museum tour with multiple choice and matching galleries to exhibits, Part 3 is a tutorial on a geography field trip with multiple choice and matching activities to days, and Part 4 completes lecture notes on the history of money.
-- The script of each part is the transcript (text_content) of its recording asset, and every answer is spoken in it. The
-- recordings are generated with edge-tts voices and uploaded to the public "media" bucket under listening/practice/ and the
-- cover photos under practice/covers/ separately; this migration does not upload them.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-020000007001', 'AUDIO', 'Clerk: Good afternoon, Central Station lost property. How can I help you?
Passenger: Hello. I think I left my bag on a train yesterday, and I would like to report it, please.
Clerk: I am sorry to hear that. Let me take some details. What is your surname?
Passenger: It''s Okoye. That''s O, K, O, Y, E.
Clerk: Thank you, Mr Okoye. Which train were you on?
Passenger: I got on at Lincoln, no, sorry, I mean at Leeds, and I travelled down to London.
Clerk: From Leeds to London. And what time did the train leave?
Passenger: At quarter past ten in the morning.
Clerk: Was that today, or was it yesterday?
Passenger: It was yesterday. I thought it was Thursday, but today is Saturday, so it was Friday.
Clerk: Friday, thank you. Can you describe the bag?
Passenger: It''s a backpack. I would say it is blue, but actually it''s more of a dark green colour. It has a lot of pockets.
Clerk: Dark green, then. What was inside it?
Passenger: My laptop, a book, and my glasses, which I need for driving, so it is quite urgent.
Clerk: I understand. Do you remember where you left it?
Passenger: On the luggage rack in coach six, near the door, I think.
Clerk: Coach six. Is there anything on the bag that would help us to recognise it?
Passenger: Yes, there is a red ribbon on the zip. My daughter tied it there, so I could find it easily at the airport.
Clerk: That is very useful. If we find it, you will need to collect it from this office in person. There is a charge of five pounds for each item, but nothing if you collect it within twenty-four hours.
Passenger: Five pounds is fine. I hope it is found today.
Clerk: We will contact you as soon as we can. Could I have a telephone number?
Passenger: It''s oh seven four zero zero, then two nine one, eight six five.
Clerk: Thank you, Mr Okoye. We will call you this afternoon.', 'listening/practice/pt-listening-08-s1.mp3', 179, 'VALID'),
    ('2d000000-0000-4000-8000-020000007002', 'AUDIO', 'Guide: Good morning, everybody, and welcome to Harbourtown Museum. My name is Susan, and I am going to take you around the galleries this morning. First, a little about the museum itself.
Guide: The museum opened in nineteen seventy-five, so it has been welcoming visitors for almost fifty years. The building is older than that. It used to be the town''s market hall, and you can still see the original iron columns in the entrance.
Guide: A few practical points. Audio guides are available at the desk for three pounds, and they are available in six languages. If you are in a group, you can borrow a set of headphones free of charge. Photography is welcome in most of the museum, with or without a flash, but it is not permitted in Gallery 4, because some of the old photographs there are very delicate.
Guide: The museum closes at five o''clock, but the gift shop closes half an hour earlier, at half past four, so if you would like to buy something, please plan to go there first. There is also a small cafe on the first floor.
Guide: Now, the galleries. We start in Gallery 1, on the ground floor. Its main attraction is a large model of the town as it was in eighteen hundred. Visitors, especially children, love to find their own street on it.
Guide: Gallery 2 is about the earliest history of the town. Here you can see objects found by archaeologists, and the highlight is a collection of Roman coins, which was discovered by a farmer in a field nearby only ten years ago.
Guide: In Gallery 3, we look at everyday life in the nineteenth century. There is a classroom from a Victorian school, with old wooden desks that you are allowed to sit at, and a teacher''s desk with a cane. It is always very popular with school visits.
Guide: Gallery 4 is rather more serious. It covers the two world wars, and what life was like on the home front. The main exhibition is a series of black and white photographs, taken by a local photographer, which show the town during the war.
Guide: Last of all, please step outside into the courtyard, where we keep our biggest object, a steam engine that used to pull trains on the local railway line. On the first Sunday of each month, the engine is started, and it gives rides to visitors. The costume collection, by the way, is in the basement, but it is closed for repairs until next spring.
Guide: Right, if there are no questions, let us begin in Gallery 1. Please follow me.', 'listening/practice/pt-listening-08-s2.mp3', 191, 'VALID'),
    ('2d000000-0000-4000-8000-020000007003', 'AUDIO', 'Dr Brooks: Hello, Nina, hello, Sam. You asked to see me about next month''s field trip. What would you like to know?
Nina: Mainly about the schedule, Dr Brooks. We know that the trip is to the coast, but we are not sure what exactly we are studying.
Dr Brooks: This year we are studying the sand dunes at Whitesands Nature Reserve. In particular, we will look at how the plants change as you move away from the beach. The aim is to understand how the dunes become stable over time.
Sam: How will we get there? Last year''s group went by train, didn''t they?
Dr Brooks: They did, but it was very slow, with two changes. This year the university has booked a minibus, so we will travel together and also carry all the equipment.
Nina: And where will we stay?
Dr Brooks: In the youth hostel in the village, about a mile from the reserve. You will share rooms of four. Camping was an option, but the weather at that time of year is too unreliable.
Sam: What should we bring? Do we need to buy any equipment?
Dr Brooks: The department provides the measuring tapes, the frames for counting plants, and the soil kits. What you must bring yourselves is waterproof boots, because the ground in the dunes is often wet. Please do not bring laptops, as there is no electricity in the hut where we work.
Nina: Could you go through the programme for the week?
Dr Brooks: Of course. On Monday, we travel to the reserve, and in the afternoon, I will give you a short introduction on site. On Tuesday, we do the dune survey, which means counting the plants along a line from the beach to the inland edge.
Sam: And Wednesday?
Dr Brooks: On Wednesday, you will collect soil samples from each zone of the dunes. It is the hardest day physically, so make sure that you eat a good breakfast.
Nina: What about the rangers? I read that they have worked on the reserve for twenty years.
Dr Brooks: Yes, they will be with us on Thursday. You will interview them about how the dunes have changed, and how they are managed. Prepare some questions in advance.
Sam: And Friday?
Dr Brooks: On Friday, we pack up in the morning and return in the afternoon. You will analyse your data back at the university in the following week.
Nina: How will we be assessed?
Dr Brooks: Each of you will write an individual report of about three thousand words. There is no group presentation this year, and there is no exam for this module.
Sam: That is clear. Thank you very much, Dr Brooks.', 'listening/practice/pt-listening-08-s3.mp3', 209, 'VALID'),
    ('2d000000-0000-4000-8000-020000007004', 'AUDIO', 'Lecturer: Good morning, everyone. Today I would like to give you a short history of money, because we cannot understand modern banks unless we know where money came from.
Lecturer: Before there was any money, people simply used barter. That means exchanging one thing directly for another, for example a sack of grain for a pair of shoes. Barter works in a small village, but it has a serious weakness. You need to find someone who has what you want, and who also wants what you have.
Lecturer: The first real coins appeared around six hundred BC, in the kingdom of Lydia, which was in the west of modern Turkey. They were made of electrum, a natural mixture of gold and silver. Each coin carried a stamp, which guaranteed its weight and value, so merchants did not need to weigh the metal every time.
Lecturer: Coins were also developed independently in China. The Chinese made theirs of bronze, and many had a hole in the centre, so that they could be tied together on a string. A thousand coins on one string was a common unit.
Lecturer: China also gave the world its first paper money, around the tenth century. Merchants who carried heavy strings of coins over long distances found that paper notes were much lighter, and much safer. Later, the government took control of the system and began to print the notes itself.
Lecturer: In Europe, paper money came much later. The first banknotes in Europe were issued by a bank in Sweden, in the year sixteen sixty-one. Within a few decades, many other countries had followed the example.
Lecturer: The next big step was the plastic card. The first credit card was introduced in nineteen fifty in the United States, and it was used at only a few restaurants. Today, billions of cards are in use around the world, and in many countries, people hardly ever use cash at all.
Lecturer: So the history of money is the history of making trade easier, safer and faster. In next week''s lecture, we will consider where payment by phone and digital currencies are likely to take us.', 'listening/practice/pt-listening-08-s4.mp3', 170, 'VALID')
ON CONFLICT (id) DO UPDATE SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference, duration_seconds = EXCLUDED.duration_seconds;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000007001', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007003', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007004', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007005', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007006', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007007', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007008', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007009', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007010', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007011', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007012', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007013', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007014', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007015', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007016', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007017', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007018', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007019', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007020', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007021', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007022', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007023', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007024', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007025', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007026', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007027', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007028', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007029', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007030', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007031', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007032', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007033', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007039', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000007040', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000007001', '2d000000-0000-4000-8000-030000007001', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["okoye"]}'::jsonb, 1, 'The passenger spells his surname O, K, O, Y, E.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007002', '2d000000-0000-4000-8000-030000007002', 1, 'Train from ______ to London', NULL, '{"type":"FILL","accepted":["leeds"]}'::jsonb, 1, 'He first says Lincoln, then corrects himself: Leeds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007003', '2d000000-0000-4000-8000-030000007003', 1, 'Time of train: ______ a.m.', NULL, '{"type":"FILL","accepted":["10.15","10:15","10.15am","quarter past ten"]}'::jsonb, 1, 'The train left at quarter past ten in the morning.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007004', '2d000000-0000-4000-8000-030000007004', 1, 'Day the bag was lost: ______', NULL, '{"type":"FILL","accepted":["friday"]}'::jsonb, 1, 'He thinks Thursday, but today is Saturday, so it was Friday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007005', '2d000000-0000-4000-8000-030000007005', 1, 'Colour of bag: ______', NULL, '{"type":"FILL","accepted":["green","dark green"]}'::jsonb, 1, 'He says blue, then explains it is a dark green colour.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007006', '2d000000-0000-4000-8000-030000007006', 1, 'Contents: laptop, book and ______', NULL, '{"type":"FILL","accepted":["glasses"]}'::jsonb, 1, 'The bag holds a laptop, a book and his glasses.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007007', '2d000000-0000-4000-8000-030000007007', 1, 'Left in coach number ______', NULL, '{"type":"FILL","accepted":["6","six"]}'::jsonb, 1, 'He left it on the luggage rack in coach six.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007008', '2d000000-0000-4000-8000-030000007008', 1, 'Special feature: a red ______ on the zip', NULL, '{"type":"FILL","accepted":["ribbon"]}'::jsonb, 1, 'His daughter tied a red ribbon on the zip.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007009', '2d000000-0000-4000-8000-030000007009', 1, 'Charge for collecting: £ ______', NULL, '{"type":"FILL","accepted":["5","five"]}'::jsonb, 1, 'There is a charge of five pounds, unless the bag is collected within twenty-four hours.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007010', '2d000000-0000-4000-8000-030000007010', 1, 'Telephone: 07400 ______', NULL, '{"type":"FILL","accepted":["291865","291 865","two nine one eight six five"]}'::jsonb, 1, 'The passenger gives the last digits as two nine one, eight six five.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007011', '2d000000-0000-4000-8000-030000007011', 1, 'When did the museum open?', '[{"content":"In 1952","optionKey":"A","sortOrder":1},{"content":"In 1975","optionKey":"B","sortOrder":2},{"content":"In 1998","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The museum opened in nineteen seventy-five.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007012', '2d000000-0000-4000-8000-030000007012', 1, 'The museum building used to be', '[{"content":"a market hall.","optionKey":"A","sortOrder":1},{"content":"a railway station.","optionKey":"B","sortOrder":2},{"content":"the town hall.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It used to be the town''s market hall.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007013', '2d000000-0000-4000-8000-030000007013', 1, 'How much does an audio guide cost?', '[{"content":"It is free.","optionKey":"A","sortOrder":1},{"content":"Three pounds","optionKey":"B","sortOrder":2},{"content":"Six pounds","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Audio guides cost three pounds; group headphones are free.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007014', '2d000000-0000-4000-8000-030000007014', 1, 'Where is photography not permitted?', '[{"content":"In the entrance","optionKey":"A","sortOrder":1},{"content":"In the courtyard","optionKey":"B","sortOrder":2},{"content":"In Gallery 4","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'The photographs in Gallery 4 are delicate, so no photography is allowed there.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007015', '2d000000-0000-4000-8000-030000007015', 1, 'At what time does the gift shop close?', '[{"content":"At four o''clock","optionKey":"A","sortOrder":1},{"content":"At half past four","optionKey":"B","sortOrder":2},{"content":"At five o''clock","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The museum closes at five, and the shop half an hour earlier.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007016', '2d000000-0000-4000-8000-030000007016', 1, 'Gallery 1', '[{"content":"Roman coins","optionKey":"A","sortOrder":1},{"content":"A steam engine","optionKey":"B","sortOrder":2},{"content":"Old school desks","optionKey":"C","sortOrder":3},{"content":"Wartime photographs","optionKey":"D","sortOrder":4},{"content":"A model of the town","optionKey":"E","sortOrder":5},{"content":"Costumes","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'Gallery 1 has a large model of the town.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007017', '2d000000-0000-4000-8000-030000007017', 1, 'Gallery 2', '[{"content":"Roman coins","optionKey":"A","sortOrder":1},{"content":"A steam engine","optionKey":"B","sortOrder":2},{"content":"Old school desks","optionKey":"C","sortOrder":3},{"content":"Wartime photographs","optionKey":"D","sortOrder":4},{"content":"A model of the town","optionKey":"E","sortOrder":5},{"content":"Costumes","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The highlight of Gallery 2 is a collection of Roman coins.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007018', '2d000000-0000-4000-8000-030000007018', 1, 'Gallery 3', '[{"content":"Roman coins","optionKey":"A","sortOrder":1},{"content":"A steam engine","optionKey":"B","sortOrder":2},{"content":"Old school desks","optionKey":"C","sortOrder":3},{"content":"Wartime photographs","optionKey":"D","sortOrder":4},{"content":"A model of the town","optionKey":"E","sortOrder":5},{"content":"Costumes","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Gallery 3 has a Victorian classroom with old wooden desks.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007019', '2d000000-0000-4000-8000-030000007019', 1, 'Gallery 4', '[{"content":"Roman coins","optionKey":"A","sortOrder":1},{"content":"A steam engine","optionKey":"B","sortOrder":2},{"content":"Old school desks","optionKey":"C","sortOrder":3},{"content":"Wartime photographs","optionKey":"D","sortOrder":4},{"content":"A model of the town","optionKey":"E","sortOrder":5},{"content":"Costumes","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Gallery 4 shows black and white photographs of the town during the war.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007020', '2d000000-0000-4000-8000-030000007020', 1, 'The courtyard', '[{"content":"Roman coins","optionKey":"A","sortOrder":1},{"content":"A steam engine","optionKey":"B","sortOrder":2},{"content":"Old school desks","optionKey":"C","sortOrder":3},{"content":"Wartime photographs","optionKey":"D","sortOrder":4},{"content":"A model of the town","optionKey":"E","sortOrder":5},{"content":"Costumes","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The steam engine is in the courtyard. The costumes are in the basement, which is closed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007021', '2d000000-0000-4000-8000-030000007021', 1, 'What will the students study on the trip?', '[{"content":"The plants of the sand dunes","optionKey":"A","sortOrder":1},{"content":"The speed of a river","optionKey":"B","sortOrder":2},{"content":"The birds of the coast","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They will look at how the plants change as you move away from the beach.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007022', '2d000000-0000-4000-8000-030000007022', 1, 'How will the group travel to the reserve?', '[{"content":"By minibus","optionKey":"A","sortOrder":1},{"content":"By train","optionKey":"B","sortOrder":2},{"content":"By bicycle","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The university has booked a minibus.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007023', '2d000000-0000-4000-8000-030000007023', 1, 'Where will the students stay?', '[{"content":"In a hostel","optionKey":"A","sortOrder":1},{"content":"In tents","optionKey":"B","sortOrder":2},{"content":"In a village hall","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They will stay in the youth hostel in the village; camping is too unreliable.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007024', '2d000000-0000-4000-8000-030000007024', 1, 'What must the students bring themselves?', '[{"content":"Measuring tapes","optionKey":"A","sortOrder":1},{"content":"Waterproof boots","optionKey":"B","sortOrder":2},{"content":"Laptop computers","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The department provides the equipment; students must bring waterproof boots.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007025', '2d000000-0000-4000-8000-030000007025', 1, 'How will the module be assessed?', '[{"content":"By a group presentation","optionKey":"A","sortOrder":1},{"content":"By an exam","optionKey":"B","sortOrder":2},{"content":"By an individual report","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Each student writes an individual report of about three thousand words.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007026', '2d000000-0000-4000-8000-030000007026', 1, 'Travel to the reserve', '[{"content":"Monday","optionKey":"A","sortOrder":1},{"content":"Tuesday","optionKey":"B","sortOrder":2},{"content":"Wednesday","optionKey":"C","sortOrder":3},{"content":"Thursday","optionKey":"D","sortOrder":4},{"content":"Friday","optionKey":"E","sortOrder":5}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They travel to the reserve on Monday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007027', '2d000000-0000-4000-8000-030000007027', 1, 'The dune survey', '[{"content":"Monday","optionKey":"A","sortOrder":1},{"content":"Tuesday","optionKey":"B","sortOrder":2},{"content":"Wednesday","optionKey":"C","sortOrder":3},{"content":"Thursday","optionKey":"D","sortOrder":4},{"content":"Friday","optionKey":"E","sortOrder":5}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The survey, counting plants along a line, is on Tuesday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007028', '2d000000-0000-4000-8000-030000007028', 1, 'Collecting soil samples', '[{"content":"Monday","optionKey":"A","sortOrder":1},{"content":"Tuesday","optionKey":"B","sortOrder":2},{"content":"Wednesday","optionKey":"C","sortOrder":3},{"content":"Thursday","optionKey":"D","sortOrder":4},{"content":"Friday","optionKey":"E","sortOrder":5}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Soil samples are collected on Wednesday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007029', '2d000000-0000-4000-8000-030000007029', 1, 'Interviews with rangers', '[{"content":"Monday","optionKey":"A","sortOrder":1},{"content":"Tuesday","optionKey":"B","sortOrder":2},{"content":"Wednesday","optionKey":"C","sortOrder":3},{"content":"Thursday","optionKey":"D","sortOrder":4},{"content":"Friday","optionKey":"E","sortOrder":5}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'The rangers join the group on Thursday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007030', '2d000000-0000-4000-8000-030000007030', 1, 'Return journey', '[{"content":"Monday","optionKey":"A","sortOrder":1},{"content":"Tuesday","optionKey":"B","sortOrder":2},{"content":"Wednesday","optionKey":"C","sortOrder":3},{"content":"Thursday","optionKey":"D","sortOrder":4},{"content":"Friday","optionKey":"E","sortOrder":5}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'They pack up and return on Friday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007031', '2d000000-0000-4000-8000-030000007031', 1, 'Before money, people used ______', NULL, '{"type":"FILL","accepted":["barter"]}'::jsonb, 1, 'People exchanged things directly, which is called barter.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007032', '2d000000-0000-4000-8000-030000007032', 1, 'A weakness of barter: you must find someone who also wants what you ______', NULL, '{"type":"FILL","accepted":["have"]}'::jsonb, 1, 'You need someone who has what you want and who wants what you have.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007033', '2d000000-0000-4000-8000-030000007033', 1, 'The first coins appeared around ______ BC', NULL, '{"type":"FILL","accepted":["600","six hundred"]}'::jsonb, 1, 'The first coins appeared around six hundred BC.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007034', '2d000000-0000-4000-8000-030000007034', 1, 'They appeared in the kingdom of ______', NULL, '{"type":"FILL","accepted":["lydia"]}'::jsonb, 1, 'The kingdom of Lydia was in the west of modern Turkey.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007035', '2d000000-0000-4000-8000-030000007035', 1, 'Electrum is a mixture of gold and ______', NULL, '{"type":"FILL","accepted":["silver"]}'::jsonb, 1, 'Electrum is a natural mixture of gold and silver.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007036', '2d000000-0000-4000-8000-030000007036', 1, 'Chinese coins were made of ______', NULL, '{"type":"FILL","accepted":["bronze"]}'::jsonb, 1, 'The Chinese made their coins of bronze.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007037', '2d000000-0000-4000-8000-030000007037', 1, 'The first paper money was used in ______', NULL, '{"type":"FILL","accepted":["china"]}'::jsonb, 1, 'China also gave the world its first paper money.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007038', '2d000000-0000-4000-8000-030000007038', 1, 'Paper notes were much ______ to carry than coins', NULL, '{"type":"FILL","accepted":["lighter"]}'::jsonb, 1, 'Merchants found that paper notes were much lighter and safer.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007039', '2d000000-0000-4000-8000-030000007039', 1, 'The first European banknotes were issued in ______ in 1661', NULL, '{"type":"FILL","accepted":["sweden"]}'::jsonb, 1, 'A bank in Sweden issued the first banknotes in Europe.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000007040', '2d000000-0000-4000-8000-030000007040', 1, 'The first ______ card was introduced in 1950', NULL, '{"type":"FILL","accepted":["credit"]}'::jsonb, 1, 'The first credit card was introduced in nineteen fifty.', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = published.version_id::uuid FROM (VALUES
    ('2d000000-0000-4000-8000-030000007001', '2d000000-0000-4000-8000-040000007001'),
    ('2d000000-0000-4000-8000-030000007002', '2d000000-0000-4000-8000-040000007002'),
    ('2d000000-0000-4000-8000-030000007003', '2d000000-0000-4000-8000-040000007003'),
    ('2d000000-0000-4000-8000-030000007004', '2d000000-0000-4000-8000-040000007004'),
    ('2d000000-0000-4000-8000-030000007005', '2d000000-0000-4000-8000-040000007005'),
    ('2d000000-0000-4000-8000-030000007006', '2d000000-0000-4000-8000-040000007006'),
    ('2d000000-0000-4000-8000-030000007007', '2d000000-0000-4000-8000-040000007007'),
    ('2d000000-0000-4000-8000-030000007008', '2d000000-0000-4000-8000-040000007008'),
    ('2d000000-0000-4000-8000-030000007009', '2d000000-0000-4000-8000-040000007009'),
    ('2d000000-0000-4000-8000-030000007010', '2d000000-0000-4000-8000-040000007010'),
    ('2d000000-0000-4000-8000-030000007011', '2d000000-0000-4000-8000-040000007011'),
    ('2d000000-0000-4000-8000-030000007012', '2d000000-0000-4000-8000-040000007012'),
    ('2d000000-0000-4000-8000-030000007013', '2d000000-0000-4000-8000-040000007013'),
    ('2d000000-0000-4000-8000-030000007014', '2d000000-0000-4000-8000-040000007014'),
    ('2d000000-0000-4000-8000-030000007015', '2d000000-0000-4000-8000-040000007015'),
    ('2d000000-0000-4000-8000-030000007016', '2d000000-0000-4000-8000-040000007016'),
    ('2d000000-0000-4000-8000-030000007017', '2d000000-0000-4000-8000-040000007017'),
    ('2d000000-0000-4000-8000-030000007018', '2d000000-0000-4000-8000-040000007018'),
    ('2d000000-0000-4000-8000-030000007019', '2d000000-0000-4000-8000-040000007019'),
    ('2d000000-0000-4000-8000-030000007020', '2d000000-0000-4000-8000-040000007020'),
    ('2d000000-0000-4000-8000-030000007021', '2d000000-0000-4000-8000-040000007021'),
    ('2d000000-0000-4000-8000-030000007022', '2d000000-0000-4000-8000-040000007022'),
    ('2d000000-0000-4000-8000-030000007023', '2d000000-0000-4000-8000-040000007023'),
    ('2d000000-0000-4000-8000-030000007024', '2d000000-0000-4000-8000-040000007024'),
    ('2d000000-0000-4000-8000-030000007025', '2d000000-0000-4000-8000-040000007025'),
    ('2d000000-0000-4000-8000-030000007026', '2d000000-0000-4000-8000-040000007026'),
    ('2d000000-0000-4000-8000-030000007027', '2d000000-0000-4000-8000-040000007027'),
    ('2d000000-0000-4000-8000-030000007028', '2d000000-0000-4000-8000-040000007028'),
    ('2d000000-0000-4000-8000-030000007029', '2d000000-0000-4000-8000-040000007029'),
    ('2d000000-0000-4000-8000-030000007030', '2d000000-0000-4000-8000-040000007030'),
    ('2d000000-0000-4000-8000-030000007031', '2d000000-0000-4000-8000-040000007031'),
    ('2d000000-0000-4000-8000-030000007032', '2d000000-0000-4000-8000-040000007032'),
    ('2d000000-0000-4000-8000-030000007033', '2d000000-0000-4000-8000-040000007033'),
    ('2d000000-0000-4000-8000-030000007034', '2d000000-0000-4000-8000-040000007034'),
    ('2d000000-0000-4000-8000-030000007035', '2d000000-0000-4000-8000-040000007035'),
    ('2d000000-0000-4000-8000-030000007036', '2d000000-0000-4000-8000-040000007036'),
    ('2d000000-0000-4000-8000-030000007037', '2d000000-0000-4000-8000-040000007037'),
    ('2d000000-0000-4000-8000-030000007038', '2d000000-0000-4000-8000-040000007038'),
    ('2d000000-0000-4000-8000-030000007039', '2d000000-0000-4000-8000-040000007039'),
    ('2d000000-0000-4000-8000-030000007040', '2d000000-0000-4000-8000-040000007040')
) AS published(question_id, version_id) WHERE questions.id = published.question_id::uuid;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000007001', 'PT-LISTENING-08', 'Listening Practice Test 8', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000007001', '2d000000-0000-4000-8000-050000007001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000007001' WHERE id = '2d000000-0000-4000-8000-050000007001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) VALUES
    ('2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-060000007001', 'Section 1: Reporting lost property', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-10 as you listen.', 1),
    ('2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-060000007001', 'Section 2: A local history museum', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 11-20 as you listen.', 2),
    ('2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-060000007001', 'Section 3: Planning a geography field trip', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 21-30 as you listen.', 3),
    ('2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-060000007001', 'Section 4: The history of money', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 31-40 as you listen.', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000007001', '2d000000-0000-4000-8000-070000007001', 1, 'GAP_FILLING', 'Questions 1-10. Complete the lost property form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000007002', '2d000000-0000-4000-8000-070000007002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 11-15. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000007003', '2d000000-0000-4000-8000-070000007002', 2, 'MATCHING', 'Questions 16-20. What can visitors see in each part of the museum? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000007004', '2d000000-0000-4000-8000-070000007003', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 21-25. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000007005', '2d000000-0000-4000-8000-070000007003', 2, 'MATCHING', 'Questions 26-30. Which day will each activity take place? Choose the correct letter, A-E.'),
    ('2d000000-0000-4000-8000-090000007006', '2d000000-0000-4000-8000-070000007004', 1, 'GAP_FILLING', 'Questions 31-40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000007001', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007001', 1, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007002', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007002', 2, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007003', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007003', 3, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007004', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007004', 4, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007005', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007005', 5, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007006', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007006', 6, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007007', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007007', 7, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007008', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007008', 8, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007009', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007009', 9, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007010', '2d000000-0000-4000-8000-070000007001', '2d000000-0000-4000-8000-040000007010', 10, 1.00, '2d000000-0000-4000-8000-090000007001'),
    ('2d000000-0000-4000-8000-080000007011', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007011', 1, 1.00, '2d000000-0000-4000-8000-090000007002'),
    ('2d000000-0000-4000-8000-080000007012', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007012', 2, 1.00, '2d000000-0000-4000-8000-090000007002'),
    ('2d000000-0000-4000-8000-080000007013', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007013', 3, 1.00, '2d000000-0000-4000-8000-090000007002'),
    ('2d000000-0000-4000-8000-080000007014', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007014', 4, 1.00, '2d000000-0000-4000-8000-090000007002'),
    ('2d000000-0000-4000-8000-080000007015', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007015', 5, 1.00, '2d000000-0000-4000-8000-090000007002'),
    ('2d000000-0000-4000-8000-080000007016', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007016', 6, 1.00, '2d000000-0000-4000-8000-090000007003'),
    ('2d000000-0000-4000-8000-080000007017', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007017', 7, 1.00, '2d000000-0000-4000-8000-090000007003'),
    ('2d000000-0000-4000-8000-080000007018', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007018', 8, 1.00, '2d000000-0000-4000-8000-090000007003'),
    ('2d000000-0000-4000-8000-080000007019', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007019', 9, 1.00, '2d000000-0000-4000-8000-090000007003'),
    ('2d000000-0000-4000-8000-080000007020', '2d000000-0000-4000-8000-070000007002', '2d000000-0000-4000-8000-040000007020', 10, 1.00, '2d000000-0000-4000-8000-090000007003'),
    ('2d000000-0000-4000-8000-080000007021', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007021', 1, 1.00, '2d000000-0000-4000-8000-090000007004'),
    ('2d000000-0000-4000-8000-080000007022', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007022', 2, 1.00, '2d000000-0000-4000-8000-090000007004'),
    ('2d000000-0000-4000-8000-080000007023', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007023', 3, 1.00, '2d000000-0000-4000-8000-090000007004'),
    ('2d000000-0000-4000-8000-080000007024', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007024', 4, 1.00, '2d000000-0000-4000-8000-090000007004'),
    ('2d000000-0000-4000-8000-080000007025', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007025', 5, 1.00, '2d000000-0000-4000-8000-090000007004'),
    ('2d000000-0000-4000-8000-080000007026', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007026', 6, 1.00, '2d000000-0000-4000-8000-090000007005'),
    ('2d000000-0000-4000-8000-080000007027', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007027', 7, 1.00, '2d000000-0000-4000-8000-090000007005'),
    ('2d000000-0000-4000-8000-080000007028', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007028', 8, 1.00, '2d000000-0000-4000-8000-090000007005'),
    ('2d000000-0000-4000-8000-080000007029', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007029', 9, 1.00, '2d000000-0000-4000-8000-090000007005'),
    ('2d000000-0000-4000-8000-080000007030', '2d000000-0000-4000-8000-070000007003', '2d000000-0000-4000-8000-040000007030', 10, 1.00, '2d000000-0000-4000-8000-090000007005'),
    ('2d000000-0000-4000-8000-080000007031', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007031', 1, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007032', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007032', 2, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007033', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007033', 3, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007034', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007034', 4, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007035', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007035', 5, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007036', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007036', 6, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007037', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007037', 7, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007038', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007038', 8, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007039', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007039', 9, 1.00, '2d000000-0000-4000-8000-090000007006'),
    ('2d000000-0000-4000-8000-080000007040', '2d000000-0000-4000-8000-070000007004', '2d000000-0000-4000-8000-040000007040', 10, 1.00, '2d000000-0000-4000-8000-090000007006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000007001', '2d000000-0000-4000-8000-020000007001', '2d000000-0000-4000-8000-070000007001', 0),
    ('2d000000-0000-4000-8000-0a0000007002', '2d000000-0000-4000-8000-020000007002', '2d000000-0000-4000-8000-070000007002', 0),
    ('2d000000-0000-4000-8000-0a0000007003', '2d000000-0000-4000-8000-020000007003', '2d000000-0000-4000-8000-070000007003', 0),
    ('2d000000-0000-4000-8000-0a0000007004', '2d000000-0000-4000-8000-020000007004', '2d000000-0000-4000-8000-070000007004', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000007001', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000007001.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000007002', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000007002.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000007003', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000007003.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000007004', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000007004.webp', NULL, 'VALID')
ON CONFLICT (id) DO UPDATE SET media_reference = EXCLUDED.media_reference, validation_status = EXCLUDED.validation_status;

UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000007001' WHERE id = '2d000000-0000-4000-8000-070000007001';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000007002' WHERE id = '2d000000-0000-4000-8000-070000007002';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000007003' WHERE id = '2d000000-0000-4000-8000-070000007003';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000007004' WHERE id = '2d000000-0000-4000-8000-070000007004';
