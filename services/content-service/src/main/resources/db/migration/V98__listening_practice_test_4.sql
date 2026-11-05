-- Listening Practice Test 4 (PT-LISTENING-04): a full-length Listening test, four parts of 10 questions.
--
-- Part 1 completes a booking form, Part 2 mixes multiple choice with matching rooms to activities, Part 3 mixes multiple choice
-- with matching tasks to people, and Part 4 completes lecture notes. The script of each part is the transcript (text_content) of
-- its recording asset, and every answer is spoken in it. The recordings are generated with edge-tts voices and uploaded to the
-- public "media" bucket under listening/practice/ separately; this migration does not upload them. The test has no cover photos yet.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-020000003001', 'AUDIO', 'Receptionist: Good morning, Old Town Walking Tours. How can I help you?
Customer: Hello. I would like to book a place on one of your evening tours, please. A friend told me about the Ghost Trail.
Receptionist: I am afraid the Ghost Trail has finished for the summer. But we have a new tour, called the Lantern Trail. Everyone carries a lantern and hears stories about the old town.
Customer: That sounds even better. Yes, I would like to book that one.
Receptionist: Lovely. We run it on Fridays and Saturdays. Which day would you like?
Customer: Could I come on Friday the twelfth of June?
Receptionist: Let me check. I am sorry, Friday the twelfth is fully booked, but there are still places on Saturday the thirteenth.
Customer: Saturday the thirteenth is fine.
Receptionist: Good. The tour starts outside the library in Market Square. People often go to the town hall by mistake, but that is on the other side of the square.
Customer: Outside the library, got it. What time does it start?
Receptionist: Usually at half past seven, but in June we start earlier because the evenings are so long, so it will be half past six.
Customer: Half past six. That is perfect.
Receptionist: And how many people will be in your group?
Customer: There were going to be four of us, but my sister has just sent me a message, and she wants to bring her husband. So five altogether.
Receptionist: Five people. The price is twelve pounds fifty per person.
Customer: Twelve pounds fifty. Do you offer any discounts?
Receptionist: Yes. Groups of six or more get fifteen per cent off. I am afraid five is one person short.
Customer: Never mind. How long does the tour take?
Receptionist: About ninety minutes. It ends at a pub, where you can buy a drink if you like, but that is not included in the price.
Customer: Is there anything we should bring?
Receptionist: Please wear comfortable shoes. We provide the lanterns, but some of the alleys are very dark, so I would also bring a torch if you have one.
Customer: A torch. OK.
Receptionist: Can I take a contact number?
Customer: Yes, it is my mobile. Oh seven seven oh oh, nine five eight, four two one.
Receptionist: Nine five eight, four two one. Thank you. You are all booked, and we will see you on Saturday.
Customer: Thank you very much. Goodbye.', 'listening/practice/pt-listening-04-s1.mp3', 214, 'VALID'),
    ('2d000000-0000-4000-8000-020000003002', 'AUDIO', 'Manager: Good evening, everyone, and welcome to Riverside Sports Centre. My name is Karen, and I manage the centre. I would like to tell you a little about what is on offer, and how to make the most of your membership.
Manager: First, some news. We have made quite a few changes recently. The roof of the main hall was repaired two years ago, and the swimming pool, which many of you know, has always been popular. But the biggest change is our new climbing wall in Hall B, which opened in March. It has already attracted a lot of new members, and it is suitable for complete beginners.
Manager: Now, a few practical points. If you come by car, parking is free for the first two hours. After that, there is a charge of one pound for each extra hour, so please remember to ask at reception for a ticket if you are staying longer.
Manager: You will find the centre is busiest on weekday evenings, between six and eight, when many people come straight from work. Weekend afternoons are surprisingly quiet, so if you prefer more space, that is the best time to come.
Manager: Booking a court is easy. For the same day, you can simply ask at reception. But if you want a court for any later date, you will need to use our app. We stopped taking telephone bookings last year.
Manager: On the ground floor you will find the cafe. It is open to everybody, not just members, and it serves hot meals at lunchtime only. It closes at four o clock. I should mention that there is no discount for members, I am afraid.
Manager: Let me now tell you where things happen. As you come in, Studio 1 is on your left. That is where we run our yoga sessions. Next to it, Studio 2 is used for dance classes, mostly on weekday evenings. Straight ahead is Hall A, the big hall with the sprung floor, which is used for badminton. Hall B, as I said, is where you will find the climbing wall. And down the corridor, in Room 5, there are three table tennis tables, which you can use at any time without booking. Boxing takes place in the gym downstairs, which I will show you in a moment.
Manager: Right, if there are no questions, let us start the tour.', 'listening/practice/pt-listening-04-s2.mp3', 198, 'VALID'),
    ('2d000000-0000-4000-8000-020000003003', 'AUDIO', 'Dr Evans: Come in, both of you. Maya, Daniel, how is the river project going?
Maya: Well, we have chosen our topic, but we wanted to ask you about how to collect the data.
Daniel: Yes. We are going to count plastic bottles and other rubbish along the riverbank, but we are not sure our method is right.
Dr Evans: Tell me your plan.
Maya: At the moment we plan to survey five sites along the river, over two weeks.
Dr Evans: Five sites in two weeks is quite ambitious, given that you both have other classes. I would reduce it to three. It is better to do a small survey well than a large one badly.
Daniel: That makes sense. Three sites, then.
Dr Evans: When can you go?
Daniel: I work in a restaurant on weekday evenings, and Maya has lectures in the mornings, so I think Saturday morning would suit us best.
Maya: Or Sunday afternoon?
Daniel: The river is full of kayakers on Sunday afternoons, so it would be hard to count properly.
Maya: True. Saturday morning, then.
Dr Evans: Good. One more thing: avoid going the day after heavy rain, because the water level will be high and many items will be washed away.
Daniel: Another problem is that some items sink, so we will not be able to count them. Will that spoil our results?
Dr Evans: Not necessarily. You should not try to dive for them! But you must mention it in your report, in the limitations section, so that readers understand.
Maya: OK, we will include that.
Dr Evans: What will you do first?
Maya: We thought of starting with the literature review.
Dr Evans: I would test your method at the nearest site first, this weekend, before you do anything else. A trial often shows problems you did not expect.
Daniel: A pilot study, right. We can do that.
Dr Evans: Now, let us sort out who does what. You will need permission to work on the riverbank, because part of it belongs to the council. I know the officer there, so I will email him myself.
Maya: Thank you. That is a big help.
Daniel: Shall I design the recording sheet?
Maya: Actually, I did a similar one last term, so I will do it. You could collect the measuring equipment from the equipment store in the department, if that is all right, Dr Evans?
Dr Evans: Yes, Daniel, ask for the key at the office.
Daniel: Right. And who will write up the methods?
Maya: I will write the methodology section, since I am already doing the recording sheet.
Dr Evans: That is fine. Send me a first draft when you have finished, and I will check it before you go any further.', 'listening/practice/pt-listening-04-s3.mp3', 226, 'VALID'),
    ('2d000000-0000-4000-8000-020000003004', 'AUDIO', 'Lecturer: Good morning. Today is about colour and consumer behaviour, and in particular how companies use colour to influence what we buy.
Lecturer: Let me begin with first impressions. Research suggests that when people see a product for the first time, they make up their minds about it very quickly, in fact within ninety seconds. And what is surprising is how much of that judgement depends on colour alone. Depending on the product, as much as sixty per cent of the decision can be based on colour.
Lecturer: So what do different colours communicate? Let us look at four of the most common. Red is the colour of energy, and shops often use it in sales, because it creates a feeling of urgency. Customers believe the offer will end soon, so they decide faster. Blue, on the other hand, is associated with trust, which is why so many banks and insurance companies choose it for their logos. Green is linked to nature, and so it is widely used on organic products, including food and cosmetics. Finally, yellow. Yellow is very good at catching the eye, but designers must be careful: a large area of yellow can make customers feel anxious, so it is usually used only in small amounts.
Lecturer: Of course, colour does not mean the same thing everywhere. This is an important point for companies that sell abroad. In China, for example, red is a symbol of luck, and it is common at weddings and new year celebrations. In many Western countries, white suggests purity, but in parts of Asia, white is traditionally worn at funerals, so a product with white packaging may not be welcome as a gift.
Lecturer: Now I would like to describe one experiment. Researchers served the same coffee to a group of volunteers in cups of different colours. The volunteers did not know that the coffee was identical. When asked to describe the taste, most of them said that the coffee in the brown cup tasted the strongest, while the coffee in the yellow cup was described as weak and thin. So the colour of the cup changed the taste.
Lecturer: To sum up, then, colour is a powerful tool, but it should be used with care. My advice to designers is always to test their choice of colour on a small sample of customers before they launch a product. Next week, we will look at typography.', 'listening/practice/pt-listening-04-s4.mp3', 183, 'VALID')
ON CONFLICT (id) DO UPDATE SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference, duration_seconds = EXCLUDED.duration_seconds;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000003001', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003003', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003004', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003005', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003006', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003007', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003008', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003009', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003010', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003011', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003012', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003013', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003014', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003015', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003016', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003017', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003018', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003019', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003020', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003021', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003022', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003023', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003024', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003025', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003026', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003027', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003028', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003029', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003030', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003031', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003032', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003033', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003039', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000003040', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000003001', '2d000000-0000-4000-8000-030000003001', 1, 'Name of tour: the ______ Trail', NULL, '{"type":"FILL","accepted":["lantern"]}'::jsonb, 1, 'The new tour is called the Lantern Trail; the Ghost Trail has finished.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003002', '2d000000-0000-4000-8000-030000003002', 1, 'Date: ______ June', NULL, '{"type":"FILL","accepted":["13","13th","thirteenth"]}'::jsonb, 1, 'The twelfth is full, so the booking is for Saturday the thirteenth.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003003', '2d000000-0000-4000-8000-030000003003', 1, 'Meeting place: outside the ______ in Market Square', NULL, '{"type":"FILL","accepted":["library"]}'::jsonb, 1, 'The tour starts outside the library, not the town hall.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003004', '2d000000-0000-4000-8000-030000003004', 1, 'Start time: ______ p.m.', NULL, '{"type":"FILL","accepted":["6.30","6:30","6.30pm","18:30","half past six"]}'::jsonb, 1, 'It is usually half past seven, but in June it starts at half past six.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003005', '2d000000-0000-4000-8000-030000003005', 1, 'Number of people: ______', NULL, '{"type":"FILL","accepted":["5","five"]}'::jsonb, 1, 'Four people became five when the sister decided to bring her husband.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003006', '2d000000-0000-4000-8000-030000003006', 1, 'Cost per person: £ ______', NULL, '{"type":"FILL","accepted":["12.50","12.5","twelve pounds fifty","twelve fifty"]}'::jsonb, 1, 'The price is twelve pounds fifty per person.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003007', '2d000000-0000-4000-8000-030000003007', 1, 'Group discount: ______ % for six or more people', NULL, '{"type":"FILL","accepted":["15","fifteen"]}'::jsonb, 1, 'Groups of six or more get fifteen per cent off.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003008', '2d000000-0000-4000-8000-030000003008', 1, 'Length of tour: ______ minutes', NULL, '{"type":"FILL","accepted":["90","ninety"]}'::jsonb, 1, 'The tour takes about ninety minutes.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003009', '2d000000-0000-4000-8000-030000003009', 1, 'Bring: comfortable shoes and a ______', NULL, '{"type":"FILL","accepted":["torch","flashlight"]}'::jsonb, 1, 'Some alleys are dark, so the receptionist suggests bringing a torch.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003010', '2d000000-0000-4000-8000-030000003010', 1, 'Mobile number: 07700 ______', NULL, '{"type":"FILL","accepted":["958421","958 421","nine five eight four two one"]}'::jsonb, 1, 'The customer gives the last digits as nine five eight, four two one.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003011', '2d000000-0000-4000-8000-030000003011', 1, 'What is the newest facility at the centre?', '[{"content":"The swimming pool","optionKey":"A","sortOrder":1},{"content":"The main hall roof","optionKey":"B","sortOrder":2},{"content":"The climbing wall","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'The climbing wall in Hall B opened in March.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003012', '2d000000-0000-4000-8000-030000003012', 1, 'How long can members park their cars for free?', '[{"content":"All day","optionKey":"A","sortOrder":1},{"content":"For two hours","optionKey":"B","sortOrder":2},{"content":"Only while they are in a class","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Parking is free for the first two hours.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003013', '2d000000-0000-4000-8000-030000003013', 1, 'When is the centre busiest?', '[{"content":"On weekday mornings","optionKey":"A","sortOrder":1},{"content":"On weekday evenings","optionKey":"B","sortOrder":2},{"content":"On weekend afternoons","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'It is busiest on weekday evenings, between six and eight.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003014', '2d000000-0000-4000-8000-030000003014', 1, 'To book a court for next week, members must use', '[{"content":"reception.","optionKey":"A","sortOrder":1},{"content":"the telephone.","optionKey":"B","sortOrder":2},{"content":"the app.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Reception is only for the same day; later dates need the app.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003015', '2d000000-0000-4000-8000-030000003015', 1, 'What does the manager say about the cafe?', '[{"content":"It stays open late.","optionKey":"A","sortOrder":1},{"content":"It is open to the public.","optionKey":"B","sortOrder":2},{"content":"It gives discounts to members.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The cafe is open to everybody, not just members.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003016', '2d000000-0000-4000-8000-030000003016', 1, 'Studio 1', '[{"content":"Boxing","optionKey":"A","sortOrder":1},{"content":"Dance classes","optionKey":"B","sortOrder":2},{"content":"Badminton","optionKey":"C","sortOrder":3},{"content":"Table tennis","optionKey":"D","sortOrder":4},{"content":"Yoga","optionKey":"E","sortOrder":5},{"content":"Climbing","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'Studio 1 is where the yoga sessions are run.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003017', '2d000000-0000-4000-8000-030000003017', 1, 'Studio 2', '[{"content":"Boxing","optionKey":"A","sortOrder":1},{"content":"Dance classes","optionKey":"B","sortOrder":2},{"content":"Badminton","optionKey":"C","sortOrder":3},{"content":"Table tennis","optionKey":"D","sortOrder":4},{"content":"Yoga","optionKey":"E","sortOrder":5},{"content":"Climbing","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Studio 2 is used for dance classes.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003018', '2d000000-0000-4000-8000-030000003018', 1, 'Hall A', '[{"content":"Boxing","optionKey":"A","sortOrder":1},{"content":"Dance classes","optionKey":"B","sortOrder":2},{"content":"Badminton","optionKey":"C","sortOrder":3},{"content":"Table tennis","optionKey":"D","sortOrder":4},{"content":"Yoga","optionKey":"E","sortOrder":5},{"content":"Climbing","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Hall A, the big hall with the sprung floor, is used for badminton.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003019', '2d000000-0000-4000-8000-030000003019', 1, 'Hall B', '[{"content":"Boxing","optionKey":"A","sortOrder":1},{"content":"Dance classes","optionKey":"B","sortOrder":2},{"content":"Badminton","optionKey":"C","sortOrder":3},{"content":"Table tennis","optionKey":"D","sortOrder":4},{"content":"Yoga","optionKey":"E","sortOrder":5},{"content":"Climbing","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"F"}'::jsonb, 1, 'Hall B has the climbing wall.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003020', '2d000000-0000-4000-8000-030000003020', 1, 'Room 5', '[{"content":"Boxing","optionKey":"A","sortOrder":1},{"content":"Dance classes","optionKey":"B","sortOrder":2},{"content":"Badminton","optionKey":"C","sortOrder":3},{"content":"Table tennis","optionKey":"D","sortOrder":4},{"content":"Yoga","optionKey":"E","sortOrder":5},{"content":"Climbing","optionKey":"F","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Room 5 has three table tennis tables. Boxing is in the gym downstairs.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003021', '2d000000-0000-4000-8000-030000003021', 1, 'Why have Maya and Daniel asked to see Dr Evans?', '[{"content":"To change their project topic","optionKey":"A","sortOrder":1},{"content":"To ask for more time","optionKey":"B","sortOrder":2},{"content":"To get advice about collecting data","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'They want to ask how to collect the data.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003022', '2d000000-0000-4000-8000-030000003022', 1, 'What does Dr Evans say about their plan to survey five sites?', '[{"content":"It is too ambitious.","optionKey":"A","sortOrder":1},{"content":"It is too limited.","optionKey":"B","sortOrder":2},{"content":"It should be repeated.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'He says five sites in two weeks is quite ambitious and suggests three.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003023', '2d000000-0000-4000-8000-030000003023', 1, 'When will the students collect their data?', '[{"content":"On weekday evenings","optionKey":"A","sortOrder":1},{"content":"On Saturday mornings","optionKey":"B","sortOrder":2},{"content":"On Sunday afternoons","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Daniel works on weekday evenings and Sunday afternoons are full of kayakers, so they choose Saturday morning.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003024', '2d000000-0000-4000-8000-030000003024', 1, 'What does Dr Evans advise about items that sink?', '[{"content":"Try to collect them.","optionKey":"A","sortOrder":1},{"content":"Mention the problem in the report.","optionKey":"B","sortOrder":2},{"content":"Count them later.","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'He says they must mention it in the limitations section.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003025', '2d000000-0000-4000-8000-030000003025', 1, 'What does Dr Evans suggest they do first?', '[{"content":"Write the literature review","optionKey":"A","sortOrder":1},{"content":"Test their method at one site","optionKey":"B","sortOrder":2},{"content":"Order the equipment","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'He suggests testing the method at the nearest site this weekend.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003026', '2d000000-0000-4000-8000-030000003026', 1, 'Get permission from the council', '[{"content":"Maya","optionKey":"A","sortOrder":1},{"content":"Daniel","optionKey":"B","sortOrder":2},{"content":"Dr Evans","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Dr Evans knows the officer and will email him himself.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003027', '2d000000-0000-4000-8000-030000003027', 1, 'Design the recording sheet', '[{"content":"Maya","optionKey":"A","sortOrder":1},{"content":"Daniel","optionKey":"B","sortOrder":2},{"content":"Dr Evans","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Daniel offers, but Maya says she will do it.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003028', '2d000000-0000-4000-8000-030000003028', 1, 'Collect the measuring equipment', '[{"content":"Maya","optionKey":"A","sortOrder":1},{"content":"Daniel","optionKey":"B","sortOrder":2},{"content":"Dr Evans","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Maya suggests Daniel collects it from the equipment store.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003029', '2d000000-0000-4000-8000-030000003029', 1, 'Write the methodology section', '[{"content":"Maya","optionKey":"A","sortOrder":1},{"content":"Daniel","optionKey":"B","sortOrder":2},{"content":"Dr Evans","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Maya says she will write the methodology section.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003030', '2d000000-0000-4000-8000-030000003030', 1, 'Check the first draft', '[{"content":"Maya","optionKey":"A","sortOrder":1},{"content":"Daniel","optionKey":"B","sortOrder":2},{"content":"Dr Evans","optionKey":"C","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Dr Evans will check the first draft.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003031', '2d000000-0000-4000-8000-030000003031', 1, 'People judge a new product within ______ seconds.', NULL, '{"type":"FILL","accepted":["90","ninety"]}'::jsonb, 1, 'People decide within ninety seconds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003032', '2d000000-0000-4000-8000-030000003032', 1, 'Up to ______ per cent of the judgement can depend on colour.', NULL, '{"type":"FILL","accepted":["60","sixty"]}'::jsonb, 1, 'As much as sixty per cent can be based on colour.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003033', '2d000000-0000-4000-8000-030000003033', 1, 'Red: used in sales because it creates a feeling of ______.', NULL, '{"type":"FILL","accepted":["urgency"]}'::jsonb, 1, 'Red creates a feeling of urgency.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003034', '2d000000-0000-4000-8000-030000003034', 1, 'Blue: linked with ______, so often chosen by banks.', NULL, '{"type":"FILL","accepted":["trust"]}'::jsonb, 1, 'Blue is associated with trust.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003035', '2d000000-0000-4000-8000-030000003035', 1, 'Green: used on ______ products such as food and cosmetics.', NULL, '{"type":"FILL","accepted":["organic"]}'::jsonb, 1, 'Green is widely used on organic products.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003036', '2d000000-0000-4000-8000-030000003036', 1, 'Yellow: a large area can make customers feel ______.', NULL, '{"type":"FILL","accepted":["anxious"]}'::jsonb, 1, 'A large area of yellow can make customers feel anxious.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003037', '2d000000-0000-4000-8000-030000003037', 1, 'In China, red is a symbol of ______.', NULL, '{"type":"FILL","accepted":["luck"]}'::jsonb, 1, 'In China, red is a symbol of luck.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003038', '2d000000-0000-4000-8000-030000003038', 1, 'In parts of Asia, white is traditionally worn at ______.', NULL, '{"type":"FILL","accepted":["funerals","funeral"]}'::jsonb, 1, 'White is traditionally worn at funerals in parts of Asia.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003039', '2d000000-0000-4000-8000-030000003039', 1, 'In the coffee experiment, the ______ cup made the coffee taste strongest.', NULL, '{"type":"FILL","accepted":["brown"]}'::jsonb, 1, 'The coffee in the brown cup tasted the strongest.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000003040', '2d000000-0000-4000-8000-030000003040', 1, 'Designers should test colours on a small ______ of customers.', NULL, '{"type":"FILL","accepted":["sample"]}'::jsonb, 1, 'The lecturer advises testing on a small sample of customers.', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = published.version_id::uuid FROM (VALUES
    ('2d000000-0000-4000-8000-030000003001', '2d000000-0000-4000-8000-040000003001'),
    ('2d000000-0000-4000-8000-030000003002', '2d000000-0000-4000-8000-040000003002'),
    ('2d000000-0000-4000-8000-030000003003', '2d000000-0000-4000-8000-040000003003'),
    ('2d000000-0000-4000-8000-030000003004', '2d000000-0000-4000-8000-040000003004'),
    ('2d000000-0000-4000-8000-030000003005', '2d000000-0000-4000-8000-040000003005'),
    ('2d000000-0000-4000-8000-030000003006', '2d000000-0000-4000-8000-040000003006'),
    ('2d000000-0000-4000-8000-030000003007', '2d000000-0000-4000-8000-040000003007'),
    ('2d000000-0000-4000-8000-030000003008', '2d000000-0000-4000-8000-040000003008'),
    ('2d000000-0000-4000-8000-030000003009', '2d000000-0000-4000-8000-040000003009'),
    ('2d000000-0000-4000-8000-030000003010', '2d000000-0000-4000-8000-040000003010'),
    ('2d000000-0000-4000-8000-030000003011', '2d000000-0000-4000-8000-040000003011'),
    ('2d000000-0000-4000-8000-030000003012', '2d000000-0000-4000-8000-040000003012'),
    ('2d000000-0000-4000-8000-030000003013', '2d000000-0000-4000-8000-040000003013'),
    ('2d000000-0000-4000-8000-030000003014', '2d000000-0000-4000-8000-040000003014'),
    ('2d000000-0000-4000-8000-030000003015', '2d000000-0000-4000-8000-040000003015'),
    ('2d000000-0000-4000-8000-030000003016', '2d000000-0000-4000-8000-040000003016'),
    ('2d000000-0000-4000-8000-030000003017', '2d000000-0000-4000-8000-040000003017'),
    ('2d000000-0000-4000-8000-030000003018', '2d000000-0000-4000-8000-040000003018'),
    ('2d000000-0000-4000-8000-030000003019', '2d000000-0000-4000-8000-040000003019'),
    ('2d000000-0000-4000-8000-030000003020', '2d000000-0000-4000-8000-040000003020'),
    ('2d000000-0000-4000-8000-030000003021', '2d000000-0000-4000-8000-040000003021'),
    ('2d000000-0000-4000-8000-030000003022', '2d000000-0000-4000-8000-040000003022'),
    ('2d000000-0000-4000-8000-030000003023', '2d000000-0000-4000-8000-040000003023'),
    ('2d000000-0000-4000-8000-030000003024', '2d000000-0000-4000-8000-040000003024'),
    ('2d000000-0000-4000-8000-030000003025', '2d000000-0000-4000-8000-040000003025'),
    ('2d000000-0000-4000-8000-030000003026', '2d000000-0000-4000-8000-040000003026'),
    ('2d000000-0000-4000-8000-030000003027', '2d000000-0000-4000-8000-040000003027'),
    ('2d000000-0000-4000-8000-030000003028', '2d000000-0000-4000-8000-040000003028'),
    ('2d000000-0000-4000-8000-030000003029', '2d000000-0000-4000-8000-040000003029'),
    ('2d000000-0000-4000-8000-030000003030', '2d000000-0000-4000-8000-040000003030'),
    ('2d000000-0000-4000-8000-030000003031', '2d000000-0000-4000-8000-040000003031'),
    ('2d000000-0000-4000-8000-030000003032', '2d000000-0000-4000-8000-040000003032'),
    ('2d000000-0000-4000-8000-030000003033', '2d000000-0000-4000-8000-040000003033'),
    ('2d000000-0000-4000-8000-030000003034', '2d000000-0000-4000-8000-040000003034'),
    ('2d000000-0000-4000-8000-030000003035', '2d000000-0000-4000-8000-040000003035'),
    ('2d000000-0000-4000-8000-030000003036', '2d000000-0000-4000-8000-040000003036'),
    ('2d000000-0000-4000-8000-030000003037', '2d000000-0000-4000-8000-040000003037'),
    ('2d000000-0000-4000-8000-030000003038', '2d000000-0000-4000-8000-040000003038'),
    ('2d000000-0000-4000-8000-030000003039', '2d000000-0000-4000-8000-040000003039'),
    ('2d000000-0000-4000-8000-030000003040', '2d000000-0000-4000-8000-040000003040')
) AS published(question_id, version_id) WHERE questions.id = published.question_id::uuid;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000003001', 'PT-LISTENING-04', 'Listening Practice Test 4', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000003001', '2d000000-0000-4000-8000-050000003001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000003001' WHERE id = '2d000000-0000-4000-8000-050000003001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) VALUES
    ('2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-060000003001', 'Section 1: Booking a walking tour', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-10 as you listen.', 1),
    ('2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-060000003001', 'Section 2: Welcome to a sports centre', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 11-20 as you listen.', 2),
    ('2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-060000003001', 'Section 3: Planning a river survey', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 21-30 as you listen.', 3),
    ('2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-060000003001', 'Section 4: Colour and consumer behaviour', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 31-40 as you listen.', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000003001', '2d000000-0000-4000-8000-070000003001', 1, 'GAP_FILLING', 'Questions 1-10. Complete the booking form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000003002', '2d000000-0000-4000-8000-070000003002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 11-15. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000003003', '2d000000-0000-4000-8000-070000003002', 2, 'MATCHING', 'Questions 16-20. Which activity takes place in each room? Choose the correct letter, A-F.'),
    ('2d000000-0000-4000-8000-090000003004', '2d000000-0000-4000-8000-070000003003', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 21-25. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000003005', '2d000000-0000-4000-8000-070000003003', 2, 'MATCHING', 'Questions 26-30. Who will do each task? Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000003006', '2d000000-0000-4000-8000-070000003004', 1, 'GAP_FILLING', 'Questions 31-40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000003001', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003001', 1, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003002', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003002', 2, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003003', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003003', 3, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003004', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003004', 4, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003005', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003005', 5, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003006', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003006', 6, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003007', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003007', 7, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003008', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003008', 8, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003009', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003009', 9, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003010', '2d000000-0000-4000-8000-070000003001', '2d000000-0000-4000-8000-040000003010', 10, 1.00, '2d000000-0000-4000-8000-090000003001'),
    ('2d000000-0000-4000-8000-080000003011', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003011', 1, 1.00, '2d000000-0000-4000-8000-090000003002'),
    ('2d000000-0000-4000-8000-080000003012', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003012', 2, 1.00, '2d000000-0000-4000-8000-090000003002'),
    ('2d000000-0000-4000-8000-080000003013', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003013', 3, 1.00, '2d000000-0000-4000-8000-090000003002'),
    ('2d000000-0000-4000-8000-080000003014', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003014', 4, 1.00, '2d000000-0000-4000-8000-090000003002'),
    ('2d000000-0000-4000-8000-080000003015', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003015', 5, 1.00, '2d000000-0000-4000-8000-090000003002'),
    ('2d000000-0000-4000-8000-080000003016', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003016', 6, 1.00, '2d000000-0000-4000-8000-090000003003'),
    ('2d000000-0000-4000-8000-080000003017', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003017', 7, 1.00, '2d000000-0000-4000-8000-090000003003'),
    ('2d000000-0000-4000-8000-080000003018', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003018', 8, 1.00, '2d000000-0000-4000-8000-090000003003'),
    ('2d000000-0000-4000-8000-080000003019', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003019', 9, 1.00, '2d000000-0000-4000-8000-090000003003'),
    ('2d000000-0000-4000-8000-080000003020', '2d000000-0000-4000-8000-070000003002', '2d000000-0000-4000-8000-040000003020', 10, 1.00, '2d000000-0000-4000-8000-090000003003'),
    ('2d000000-0000-4000-8000-080000003021', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003021', 1, 1.00, '2d000000-0000-4000-8000-090000003004'),
    ('2d000000-0000-4000-8000-080000003022', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003022', 2, 1.00, '2d000000-0000-4000-8000-090000003004'),
    ('2d000000-0000-4000-8000-080000003023', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003023', 3, 1.00, '2d000000-0000-4000-8000-090000003004'),
    ('2d000000-0000-4000-8000-080000003024', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003024', 4, 1.00, '2d000000-0000-4000-8000-090000003004'),
    ('2d000000-0000-4000-8000-080000003025', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003025', 5, 1.00, '2d000000-0000-4000-8000-090000003004'),
    ('2d000000-0000-4000-8000-080000003026', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003026', 6, 1.00, '2d000000-0000-4000-8000-090000003005'),
    ('2d000000-0000-4000-8000-080000003027', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003027', 7, 1.00, '2d000000-0000-4000-8000-090000003005'),
    ('2d000000-0000-4000-8000-080000003028', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003028', 8, 1.00, '2d000000-0000-4000-8000-090000003005'),
    ('2d000000-0000-4000-8000-080000003029', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003029', 9, 1.00, '2d000000-0000-4000-8000-090000003005'),
    ('2d000000-0000-4000-8000-080000003030', '2d000000-0000-4000-8000-070000003003', '2d000000-0000-4000-8000-040000003030', 10, 1.00, '2d000000-0000-4000-8000-090000003005'),
    ('2d000000-0000-4000-8000-080000003031', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003031', 1, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003032', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003032', 2, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003033', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003033', 3, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003034', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003034', 4, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003035', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003035', 5, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003036', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003036', 6, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003037', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003037', 7, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003038', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003038', 8, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003039', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003039', 9, 1.00, '2d000000-0000-4000-8000-090000003006'),
    ('2d000000-0000-4000-8000-080000003040', '2d000000-0000-4000-8000-070000003004', '2d000000-0000-4000-8000-040000003040', 10, 1.00, '2d000000-0000-4000-8000-090000003006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000003001', '2d000000-0000-4000-8000-020000003001', '2d000000-0000-4000-8000-070000003001', 0),
    ('2d000000-0000-4000-8000-0a0000003002', '2d000000-0000-4000-8000-020000003002', '2d000000-0000-4000-8000-070000003002', 0),
    ('2d000000-0000-4000-8000-0a0000003003', '2d000000-0000-4000-8000-020000003003', '2d000000-0000-4000-8000-070000003003', 0),
    ('2d000000-0000-4000-8000-0a0000003004', '2d000000-0000-4000-8000-020000003004', '2d000000-0000-4000-8000-070000003004', 0)
ON CONFLICT (id) DO NOTHING;
